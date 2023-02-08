package de.hpi.etranslation.controller

import de.hpi.etranslation.Controller
import de.hpi.etranslation.kv
import de.hpi.etranslation.logger
import de.hpi.etranslation.repository.TranslationRequestPartRepository
import de.hpi.etranslation.usecase.EncryptDecryptTranslationRequestPartUseCase
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Path
import org.http4k.lens.uuid
import org.http4k.routing.bind
import org.http4k.routing.routes
import javax.inject.Inject

class DebugController @Inject constructor(
    private val translationRequestPartRepository: TranslationRequestPartRepository,
    private val encryptDecryptTranslationRequestPartUseCase: EncryptDecryptTranslationRequestPartUseCase,
) : Controller {

    private fun getPart(req: Request): Response {
        val content = Path.uuid()
            .of("partId")
            .extract(req)
            .let(translationRequestPartRepository::get)
            ?.let(encryptDecryptTranslationRequestPartUseCase::decrypt)
            ?.content

        logger.info(content, "debug" kv true)

        // no info leak!
        return Response(Status.OK)
    }

    override fun routes() = "/debug" bind routes(
        "/{partId}" bind Method.GET to this::getPart,
    )
}
