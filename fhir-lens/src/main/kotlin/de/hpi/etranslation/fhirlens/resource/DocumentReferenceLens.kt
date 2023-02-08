package de.hpi.etranslation.fhirlens.resource

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import de.hpi.etranslation.fhirlens.FhirLens
import de.hpi.etranslation.fhirlens.Format
import de.hpi.etranslation.fhirlens.Lens
import de.hpi.etranslation.fhirlens.Translatables
import de.hpi.etranslation.fhirlens.encodeBase64
import de.hpi.etranslation.fhirlens.logger
import de.hpi.etranslation.fhirlens.requireDecodeBase64
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Base64BinaryType
import org.hl7.fhir.r4.model.DocumentReference
import java.security.MessageDigest

internal class DocumentReferenceLens(
    private val xmlMapper: XmlMapper,
) : Lens<DocumentReference> {
    override fun inject(
        resource: DocumentReference,
        translatables: Translatables,
    ): Result<DocumentReference, FhirLens.InjectError> = binding {
        val attachment = Attachment().apply {
            val content = translatables
                .firstOrNull { it.first == Format.PDF }
                .toResultOr { FhirLens.InjectError.MissingFormat(Format.PDF) }
                .bind()
                .second
                .let(::Base64BinaryType)

            contentType = "application/pdf"
            dataElement = content
            hashElement = content.hash()
            size = content.value.size
        }

        // Note, FHIR docs suggest multiple attachments for multiple languages
        // https://www.hl7.org/fhir/datatypes.html#Attachment
        resource.content.clear()
        resource.content.add(DocumentReference.DocumentReferenceContentComponent(attachment))

        val description = translatables
            .firstOrNull { it.first == Format.XML }
            ?.second
            ?.requireDecodeBase64()
            ?.decodeToString()
            ?.let { xmlMapper.readCatching<TranslatableDocumentReference>(it) }
            ?.mapError(FhirLens.InjectError::CorruptedFormat)
            ?.bind()
            ?.description

        if (description != null)
            resource.description = description

        resource
    }

    override fun extract(
        resource: DocumentReference,
    ): Result<Translatables, FhirLens.ExtractError> = binding {
        val attachment = resource
            .content
            .firstOrNull()
            ?.attachment
            .toResultOr { Error("No attachment on PDF").let(FhirLens.ExtractError::ResourceError) }
            .bind()

        if (attachment.contentType != "application/pdf")
            logger.info("DocumentReference.attachment[0].contentType not set, assuming pdf")

        // DocumentReference.description has 0..1 cardinality
        val xml = resource
            .description
            ?.let { description ->
                TranslatableDocumentReference(
                    description = description,
                )
            }
            ?.let(xmlMapper::writeValueAsString)
            ?.encodeToByteArray()
            ?.encodeBase64()

        listOfNotNull(
            Format.PDF to attachment.dataElement.valueAsString,
            xml?.let { Format.XML to it },
        )
    }
}

internal data class TranslatableDocumentReference(
    @JacksonXmlProperty(localName = "Description")
    val description: String,
)

private fun Base64BinaryType.hash(): Base64BinaryType =
    MessageDigest.getInstance("SHA-1")
        .digest(value)
        .let(::Base64BinaryType)
