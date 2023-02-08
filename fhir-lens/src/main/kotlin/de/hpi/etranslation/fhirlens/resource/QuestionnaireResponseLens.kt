package de.hpi.etranslation.fhirlens.resource

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import de.hpi.etranslation.fhirlens.FhirLens
import de.hpi.etranslation.fhirlens.Format
import de.hpi.etranslation.fhirlens.Lens
import de.hpi.etranslation.fhirlens.Translatables
import de.hpi.etranslation.fhirlens.encodeBase64
import de.hpi.etranslation.fhirlens.requireDecodeBase64
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType

internal data class TranslatableQuestionnaireResponse(
    @JacksonXmlProperty(localName = "Question")
    val questions: List<Question>,
) {
    data class Question(
        @JacksonXmlProperty(localName = "Text")
        val text: String,
        @JacksonXmlProperty(localName = "Answer")
        val answers: List<String?>,
    )
}

internal class QuestionnaireResponseLens constructor(
    private val xmlMapper: XmlMapper,
) : Lens<QuestionnaireResponse> {
    override fun inject(
        resource: QuestionnaireResponse,
        translatables: Translatables,
    ): Result<QuestionnaireResponse, FhirLens.InjectError> = binding {
        val translatable = translatables
            .firstOrNull { it.first == Format.XML }
            .toResultOr { FhirLens.InjectError.MissingFormat(Format.XML) }
            .bind()
            .second
            .requireDecodeBase64()
            .decodeToString()
            .let { xmlMapper.readCatching<TranslatableQuestionnaireResponse>(it) }
            .mapError(FhirLens.InjectError::CorruptedFormat)
            .bind()

        resource.item.zip(translatable.questions).forEach { (item, question) ->
            item.text = question.text
            item.answer.zip(question.answers).forEach { (answer, value) ->
                if (value != null)
                    answer.value = StringType(value)
            }
        }

        resource
    }

    override fun extract(resource: QuestionnaireResponse): Result<Translatables, FhirLens.ExtractError> = binding {
        val questions = resource.item.map { item ->
            TranslatableQuestionnaireResponse.Question(
                text = item.text,
                answers = item.answer.map { answer ->
                    if (answer.hasValueStringType())
                        answer.valueStringType.toString()
                    else
                        null
                },
            )
        }

        val xml = TranslatableQuestionnaireResponse(questions)
            .let(xmlMapper::writeValueAsString)
            .encodeToByteArray()
            .encodeBase64()

        listOf(Format.XML to xml)
    }
}

internal inline fun <reified T> XmlMapper.readCatching(
    s: String,
): Result<T, Throwable> = runCatching {
    readValue(s)
}
