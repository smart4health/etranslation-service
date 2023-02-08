package de.hpi.etranslation.usecase

import ca.uhn.fhir.context.FhirContext
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import de.hpi.etranslation.fhirlens.FhirLens
import javax.inject.Inject

class ExtractTranslatablesUseCase @Inject constructor(
    private val fhirContext: FhirContext,
    private val fhirLens: FhirLens,
) {
    operator fun invoke(
        rawResource: String,
    ): Result<List<Translatable>, Error> = binding {
        val jsonParser = fhirContext.newJsonParser()
        val resource = jsonParser.parseResource(rawResource)

        resource
            .let(fhirLens::extract)
            .mapError(Error::ExtractionFailure)
            .bind()
            .map { (format, content) ->
                Translatable(
                    content = content,
                    format = format.toString().lowercase(),
                )
            }
    }

    sealed class Error {
        data class ExtractionFailure(val extractError: FhirLens.ExtractError) : Error()
    }

    data class Translatable(
        val content: String,
        val format: String,
    )
}
