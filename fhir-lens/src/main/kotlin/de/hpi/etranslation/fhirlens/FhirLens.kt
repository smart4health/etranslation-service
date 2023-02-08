package de.hpi.etranslation.fhirlens

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import de.hpi.etranslation.fhirlens.resource.DocumentReferenceLens
import de.hpi.etranslation.fhirlens.resource.QuestionnaireResponseLens
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.QuestionnaireResponse
import java.lang.IllegalArgumentException
import java.util.Base64
import kotlin.jvm.Throws

/**
 * The fhir lens injects and extracts translatables
 * from Fhir Resources
 */
class FhirLens : Lens<IBaseResource> {

    private val xmlMapper = XmlMapper.builder()
        .addModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build(),
        )
        .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
        .build()

    /**
     * Injects the [IBaseResource] with the given [Translatables]
     *
     * > Note: This is a mutating api, and the [IBaseResource] is returned as a courtesy
     */
    override fun inject(
        resource: IBaseResource,
        translatables: Translatables,
    ): Result<IBaseResource, InjectError> {
        return when (resource) {
            is DocumentReference -> DocumentReferenceLens(xmlMapper).inject(resource, translatables)
            is QuestionnaireResponse -> QuestionnaireResponseLens(xmlMapper).inject(resource, translatables)
            else -> resource.fhirType().let(InjectError::UnknownResourceType).let(::Err)
        }
    }

    /**
     * Extracts translatables from a given [IBaseResource].
     *
     * @return a non-empty [Translatables] if Ok, [ExtractError] otherwise
     */
    override fun extract(
        resource: IBaseResource,
    ): Result<Translatables, ExtractError> {
        return when (resource) {
            is DocumentReference -> DocumentReferenceLens(xmlMapper).extract(resource)
            is QuestionnaireResponse -> QuestionnaireResponseLens(xmlMapper).extract(resource)
            else -> resource.fhirType().let(ExtractError::UnknownResourceType).let(::Err)
        }
    }

    sealed class InjectError {
        data class UnknownResourceType(val resourceType: String) : InjectError()

        data class MissingFormat(val format: Format) : InjectError()

        data class CorruptedFormat(val t: Throwable) : InjectError()
    }

    sealed class ExtractError {
        data class UnknownResourceType(val resourceType: String) : ExtractError()

        data class ResourceError(val t: Throwable) : ExtractError()
    }
}

internal interface Lens<T> {
    fun inject(resource: T, translatables: Translatables): Result<T, FhirLens.InjectError>

    fun extract(resource: T): Result<Translatables, FhirLens.ExtractError>
}

enum class Format {
    PDF,
    XML,
}

fun String.asFormat() = try {
    Format.valueOf(uppercase())
} catch (t: Throwable) {
    null
}

/**
 * Generally preceeded by [encodeToByteArray]
 */
fun ByteArray.encodeBase64(): String = Base64
    .getEncoder()
    .encodeToString(this)

/**
 * Generally followed by [decodeToString]
 */
@Throws(IllegalArgumentException::class)
fun String.requireDecodeBase64(): ByteArray {
    return Base64.getDecoder().decode(this)
}

/**
 * Each document can produce multiple request parts with their own formats
 *
 * The string must be base64 encoded
 */
typealias Translatables = List<Pair<Format, String>>
