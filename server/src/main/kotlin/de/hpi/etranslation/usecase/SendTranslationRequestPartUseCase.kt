package de.hpi.etranslation.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.binding
import de.hpi.etranslation.Config
import de.hpi.etranslation.kv
import de.hpi.etranslation.logger
import de.hpi.etranslation.repository.TranslationRequest
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class SendTranslationRequestPartUseCase @Inject constructor(
    @Named("cefTranslationClient")
    private val cefClient: Provider<@JvmSuppressWildcards HttpHandler>,
    private val config: Config,
) {
    private val domain: CefDomain

    init {
        domain = config
            .domain
            ?.let(CefDomain::fromString)
            .also {
                if (it == null)
                    logger.warn("domain is null, defaulting to PUBHEALTH")
            }
            ?: CefDomain.PUBHEALTH
    }

    operator fun invoke(request: TranslationRequest, part: TranslationRequestPart) = binding {
        if (part.sentAt != null)
            Error.AlreadySent.let(::Err).bind<Unit>()

        val cefBody = CefTranslationRequestBody(
            callerInformation = CefTranslationRequestBody.CallerInformation(
                application = config.cefUsername,
            ),
            documentToTranslateBase64 = CefTranslationRequestBody.DocumentToTranslateBase64(
                content = part.content,
                format = part.format,
            ),
            sourceLanguage = request.from,
            targetLanguages = listOf(request.to),
            domain = domain.toString(),
            errorCallback = config.cefErrorCallback.toString(),
            externalReference = part.id.toString(),
            destinations = CefTranslationRequestBody.Destinations(
                httpDestinations = listOf(config.cefSuccessCallback.toString()),
            ),
        )

        val res = Request(Method.POST, "")
            .with(Body.auto<CefTranslationRequestBody>().toLens() of cefBody)
            .let(cefClient.get())

        if (!res.status.successful) {
            logger.info("Failed to send request", "response" kv res)
            Err(Error.SendFailed).bind<Unit>()
        }
    }

    sealed class Error {
        object AlreadySent : Error()

        object SendFailed : Error()
    }

    /**
     * Errata:
     *   - docs say destinations.httpDestinations is a String but it is List<String> (can be found elsewhere actually)
     *   - docs have Drupal example where there is a requesterCallback and an email Destination, implying requesterCallback
     *     works for documents.  It might, but the request fails validation without destinations, so what's the point
     *
     * Not clearly documented:
     *   - httpDestinations receives a POST with request-id/target-language/external-reference as Query params,
     *     and the body as the content (base64'd), no content  type
     *
     * XML in, XML out works quite well
     */
    private data class CefTranslationRequestBody(
        val callerInformation: CallerInformation,
        val documentToTranslateBase64: DocumentToTranslateBase64,
        // Two-letter uppercase country code (ISO 639-1)
        val sourceLanguage: String,
        val targetLanguages: List<String>,
        val domain: String,
        val errorCallback: String,
        val externalReference: String,
        val destinations: Destinations,
    ) {
        data class CallerInformation(
            val application: String, // the application id, aka username?
        )

        data class DocumentToTranslateBase64(
            val content: String,
            // odt, ods,odp,odg, ott, ots, otp, otg, rtf, doc, docx, xls, xlsx, ppt, ppts, pdf, txt, htm, html, xhtml, xml, xlf, xliff, sdlxliff, tmx, rdf
            val format: String,
        )

        data class Destinations(
            val httpDestinations: List<String>,
        )
    }

    enum class CefDomain(
        private val domain: String,
    ) {
        GENERAL("GEN"),
        PUBHEALTH("PUBHEALTH"),
        ;

        override fun toString(): String {
            return domain
        }

        companion object {
            fun fromString(s: String): CefDomain? =
                CefDomain
                    .values()
                    .firstOrNull { it.domain == s }
        }
    }
}
