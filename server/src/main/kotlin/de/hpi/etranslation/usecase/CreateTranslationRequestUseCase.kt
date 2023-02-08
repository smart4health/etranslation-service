package de.hpi.etranslation.usecase

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.combine
import com.github.michaelbull.result.mapError
import de.hpi.etranslation.repository.TranslationRequest
import de.hpi.etranslation.repository.TranslationRequestPartRepository
import de.hpi.etranslation.repository.TranslationRequestRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

class CreateTranslationRequestUseCase @Inject constructor(
    private val uuidGenerator: () -> UUID,
    private val clock: Clock,
    private val extractTranslatablesUseCase: ExtractTranslatablesUseCase,
    private val translationRequestRepository: TranslationRequestRepository,
    private val translationRequestPartRepository: TranslationRequestPartRepository,
    private val encryptDecryptTranslationRequestUseCase: EncryptDecryptTranslationRequestUseCase,
    private val encryptDecryptTranslationRequestPartUseCase: EncryptDecryptTranslationRequestPartUseCase,
) {
    operator fun invoke(
        body: String,
        from: String,
        to: String,
    ): Result<UUID, Error> = binding {
        // Given string body, from, and to
        //
        // extract text to translate
        // store to translate, og, from, to in db with a document_id and status of UNTRANSLATED
        // + created at time
        //
        // now thinking ditch the status, add a nullable sent_at column
        //
        // return document id or error

        val request = TranslationRequest(
            id = uuidGenerator(),
            createdAt = clock.instant(),
            original = body,
            from = from,
            to = to,
        )

        val requestParts = extractTranslatablesUseCase(body)
            .mapError(Error::Extraction)
            .bind()
            .map { translatable ->
                TranslationRequestPart(
                    id = uuidGenerator(),
                    requestId = request.id,
                    createdAt = clock.instant(),
                    sentAt = null,
                    format = translatable.format,
                    content = translatable.content,
                    sendFailureAt = null,
                    sendFailureCount = 0,
                )
            }

        request
            .let(encryptDecryptTranslationRequestUseCase::encrypt)
            .let(translationRequestRepository::create)
            .mapError(Error::Db)
            .bind()

        requestParts
            .map { requestPart ->
                requestPart
                    .let(encryptDecryptTranslationRequestPartUseCase::encrypt)
                    .let(translationRequestPartRepository::create)
                    .mapError(Error::Db)
            }
            .combine()
            .bind()

        request.id
    }

    sealed class Error {
        data class Extraction(val e: ExtractTranslatablesUseCase.Error) : Error()

        data class Db(val t: Throwable) : Error()
    }
}
