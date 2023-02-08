package de.hpi.etranslation.usecase

import ca.uhn.fhir.context.FhirContext
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import de.hpi.etranslation.fhirlens.FhirLens
import de.hpi.etranslation.fhirlens.Format
import de.hpi.etranslation.fhirlens.Translatables
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.QuestionnaireResponse
import javax.inject.Inject

class InjectTranslatableUseCase @Inject constructor(
    private val fhirContext: FhirContext,
    private val fhirLens: FhirLens,
) {
    operator fun invoke(
        rawResource: String,
        translatables: Translatables,
        toLang: String,
    ): Result<String, Error> = binding {
        val jsonParser = fhirContext.newJsonParser()

        val resource = jsonParser.parseResource(rawResource)

        fhirLens.inject(resource, translatables)
            .mapError(Error::InjectionFailure)
            .bind()
            .let(jsonParser::encodeResourceToString)
    }

    sealed class Error {
        data class UnknownResource(val className: String) : Error()

        data class InjectionFailure(val injectError: FhirLens.InjectError) : Error()
    }
}

fun IBaseResource.toFormat() = when (this) {
    is DocumentReference -> Format.PDF
    is QuestionnaireResponse -> Format.XML
    else -> null
}
