package de.hpi.etranslation.controller

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.merge
import de.hpi.etranslation.Config
import de.hpi.etranslation.Controller
import de.hpi.etranslation.body
import de.hpi.etranslation.logger
import de.hpi.etranslation.usecase.CreateTranslationRequestUseCase
import de.hpi.etranslation.usecase.DeleteTranslationRequestUseCase
import de.hpi.etranslation.usecase.GetTranslatedDocumentUseCase
import de.hpi.etranslation.usecase.GetTranslationStatusUseCase
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.lens.uuid
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.UUID
import javax.inject.Inject
import com.github.michaelbull.result.runCatching as catch

class DocumentController @Inject constructor(
    private val config: Config,
    private val createTranslationRequestUseCase: CreateTranslationRequestUseCase,
    private val getTranslationStatusUseCase: GetTranslationStatusUseCase,
    private val deleteTranslationRequestUseCase: DeleteTranslationRequestUseCase,
    private val getTranslatedDocumentUseCase: GetTranslatedDocumentUseCase,
) : Controller {

    private fun createTranslationRequest(req: Request): Response {
        val from = Query.string().required("from").extract(req)
        val to = Query.string().required("to").extract(req)
        val body = req.bodyString()

        return binding {
            val translationRequestId = createTranslationRequestUseCase(
                body,
                from,
                to,
            ).mapError { err ->
                when (err) {
                    is CreateTranslationRequestUseCase.Error.Db -> {
                        logger.info("Failed to create translation request", err.t)
                        Response(Status.INTERNAL_SERVER_ERROR)
                    }
                    is CreateTranslationRequestUseCase.Error.Extraction -> {
                        logger.info("Failed to process FHIR", err.e)
                        Response(Status.BAD_REQUEST).body("Failed to process FHIR")
                    }
                }
            }.bind()

            val uri = Uri.of(config.basePath)
                .extend(Uri.of("queue"))
                .extend(Uri.of(translationRequestId.toString()))

            Response(Status.ACCEPTED)
                .with(Header.LOCATION of uri)
                .body(translationRequestId.toString())
        }.merge()
    }

    private fun getDocument(req: Request): Response {
        // get translation request, but 404 if not TRANSLATED
        val documentId = Path.uuid().of("documentId").extract(req)

        return getTranslatedDocumentUseCase(documentId)
            .mapBoth(
                success = {
                    Response(Status.OK)
                        .with(Header.CONTENT_TYPE of ContentType("application/fhir+json"))
                        .body(it)
                },
                failure = { Response(Status.NOT_FOUND) },
            )
    }

    private fun deleteDocument(req: Request): Response {
        Path.uuid()
            .of("documentId")
            .extract(req)
            .let(deleteTranslationRequestUseCase::invoke)

        return Response(Status.OK)
    }

    private fun getDocumentStatus(req: Request): Response {
        // get translation request, map to status
        val documentId = Path.uuid().of("documentId").extract(req)

        return getTranslationStatusUseCase(documentId)
            ?.let {
                when (it.status) {
                    GetTranslationStatusUseCase.TranslationStatus.Status.TRANSLATED -> {
                        val uri = Uri.of(config.basePath)
                            .extend(Uri.of("documents"))
                            .extend(Uri.of(documentId.toString()))

                        Response(Status.SEE_OTHER)
                            .with(Header.LOCATION of uri)
                    }
                    else -> Response(Status.OK).body(it)
                }
            } ?: Response(Status.NOT_FOUND)
    }

    private fun getBulkDocumentStatus(req: Request): Response {
        val ids = Query.string().required("ids").extract(req).split(",")

        val statuses = ids
            .map { it to catch { UUID.fromString(it) } }
            .associate { (rawId, uuid) ->
                rawId to uuid.get()?.let(getTranslationStatusUseCase::invoke)
            }

        return Response(Status.OK).body(statuses)
    }

    override fun routes(): RoutingHttpHandler = routes(
        "/documents" bind Method.POST to this::createTranslationRequest,
        "/documents/{documentId}" bind routes(
            Method.GET to this::getDocument,
            Method.DELETE to this::deleteDocument,
        ),
        "/queue/{documentId}" bind Method.GET to this::getDocumentStatus,
        "/queue" bind Method.GET to this::getBulkDocumentStatus,
    )
}
