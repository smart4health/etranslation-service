package de.hpi.etranslation.controller

import de.hpi.etranslation.Controller
import de.hpi.etranslation.usecase.CreateTranslationErrorUseCase
import de.hpi.etranslation.usecase.CreateTranslationSuccessUseCase
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.FormField
import org.http4k.lens.Query
import org.http4k.lens.Validator
import org.http4k.lens.string
import org.http4k.lens.uuid
import org.http4k.lens.webForm
import org.http4k.routing.bind
import org.http4k.routing.routes
import javax.inject.Inject

class CefController @Inject constructor(
    private val createTranslationSuccessUseCase: CreateTranslationSuccessUseCase,
    private val createTranslationErrorUseCase: CreateTranslationErrorUseCase,
) : Controller {
    private fun success(req: Request): Response {
        val translationRequestId = Query.uuid().required("external-reference").extract(req)
        val toLang = Query.string().required("target-language").extract(req)
        val translatedText = req.bodyString()

        createTranslationSuccessUseCase(
            translationRequestId = translationRequestId,
            translatedText = translatedText,
            toLang = toLang,
        )

        return Response(Status.OK)
    }

    private fun error(req: Request): Response {
        val webForm = Body.webForm(Validator.Ignore).toLens().extract(req)

        val translationRequestId = FormField.uuid().required("external-reference").extract(webForm)
        // the spec says '_' but the rest of the fields say '-'
        val defaultErrorCodeLens = FormField.string().defaulted("error-code", "")
        val errorCode = FormField.string().defaulted("error_code", defaultErrorCodeLens).extract(webForm)
        val errorMessage = FormField.string().defaulted("error-message", "").extract(webForm)

        createTranslationErrorUseCase(
            translationRequestId,
            hashMapOf(
                "error-code" to errorCode,
                "error-message" to errorMessage,
            ),
        )

        return Response(Status.OK)
    }

    override fun routes() = "/cef" bind routes(
        "/success-callback" bind Method.POST to this::success,
        "/error-callback" bind Method.POST to this::error,
    )
}
