package de.hpi.etranslation.usecase

import de.hpi.etranslation.repository.TranslationResponse
import de.hpi.etranslation.repository.TranslationResponseRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CreateTranslationSuccessUseCase @Inject constructor(
    private val translationResponseRepository: TranslationResponseRepository,
    private val encryptDecryptTranslationResponseUseCase: EncryptDecryptTranslationResponseUseCase,
) {
    operator fun invoke(
        translationRequestId: UUID,
        translatedText: String,
        toLang: String,
    ) {
        // given a document id and base64 text
        // check there isn't already an error
        // store in translated table:
        // - doc id (comp. key)
        // - created_at
        // - translated text
        // - target language (comp. key)

        TranslationResponse.Success(
            id = translationRequestId,
            createdAt = Instant.now(),
            translatedText = translatedText,
            toLang = toLang,
        ).let(encryptDecryptTranslationResponseUseCase::encrypt)
            .let(translationResponseRepository::createSuccess)
    }
}
