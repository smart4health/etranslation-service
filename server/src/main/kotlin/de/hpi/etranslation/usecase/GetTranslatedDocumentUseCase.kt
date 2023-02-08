package de.hpi.etranslation.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.combine
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.toResultOr
import de.hpi.etranslation.fhirlens.asFormat
import de.hpi.etranslation.kv
import de.hpi.etranslation.logger
import de.hpi.etranslation.repository.ResponseWithFormat
import de.hpi.etranslation.repository.TranslationRequestRepository
import de.hpi.etranslation.repository.TranslationResponseRepository
import java.util.UUID
import javax.inject.Inject

class GetTranslatedDocumentUseCase @Inject constructor(
    private val translationResponseRepository: TranslationResponseRepository,
    private val translationRequestRepository: TranslationRequestRepository,
    private val injectTranslatableUseCase: InjectTranslatableUseCase,
    private val encryptDecryptTranslationRequestUseCase: EncryptDecryptTranslationRequestUseCase,
    private val encryptDecryptTranslationResponseUseCase: EncryptDecryptTranslationResponseUseCase,
) {
    operator fun invoke(translationRequestId: UUID) = binding {
        val request = translationRequestRepository.get(translationRequestId)
            .toResultOr { Error.NoRequest }
            .bind()
            .let(encryptDecryptTranslationRequestUseCase::decrypt)

        val (successResponses, noResponses) = translationResponseRepository
            .getResponsesWithFormatByRequestId(translationRequestId)
            .mapError(Error::InvalidResponseState)
            .bind()
            .let {
                if (it.isEmpty()) Err(Error.NoRequestParts) else Ok(it)
            }
            .bind()
            .map {
                when (it) {
                    is ResponseWithFormat.EncryptedSuccess -> Ok(Ok(it))
                    is ResponseWithFormat.Error -> Err(it)
                    is ResponseWithFormat.NoResponse -> Ok(Err(it))
                }
            }
            .combine()
            .mapError { Error.ErrorResponse(it.extras) }
            .bind()
            .partition()

        if (noResponses.isNotEmpty())
            Error.Incomplete(
                complete = successResponses.size,
                incomplete = noResponses.size,
            ).let(::Err).bind<String>()

        val translatables = successResponses
            .map(encryptDecryptTranslationResponseUseCase::decrypt)
            .map { responseWithFormat ->
                responseWithFormat
                    .format
                    .asFormat()
                    .toResultOr { Error.InvalidFormat(responseWithFormat.format) }
                    .map { it to responseWithFormat.content }
            }
            .combine()
            .bind()

        injectTranslatableUseCase(request.original, translatables, request.to)
            .mapError(Error::InjectionError)
            .bind()
    }.onFailure { error ->
        logger.info("Failed to get document", "error" kv error)
    }

    sealed class Error {
        /**
         * original request not found
         */
        object NoRequest : Error()

        /**
         * Response found in database is malformed.  Should never happen
         */
        data class InvalidResponseState(
            val illegalStateException: IllegalStateException,
        ) : Error()

        /**
         * If there is a request but no request parts.  Should never happen
         */
        object NoRequestParts : Error()

        /**
         * A request part has a response that errored
         */
        data class ErrorResponse(val extras: Map<String, String>) : Error()

        /**
         * Not all responses were received yet
         */
        data class Incomplete(
            val complete: Int,
            val incomplete: Int,
        ) : Error()

        /**
         * A request part has a malformed format. Should never happen
         */
        data class InvalidFormat(
            val format: String,
        ) : Error()

        /**
         * Failed to inject the translatables
         */
        data class InjectionError(val e: InjectTranslatableUseCase.Error) : Error()
    }
}
