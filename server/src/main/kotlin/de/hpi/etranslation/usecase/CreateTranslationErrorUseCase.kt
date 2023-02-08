package de.hpi.etranslation.usecase

import de.hpi.etranslation.repository.TranslationResponse
import de.hpi.etranslation.repository.TranslationResponseRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CreateTranslationErrorUseCase @Inject constructor(
    private val translationResponseRepository: TranslationResponseRepository,
) {
    operator fun invoke(
        translationRequestId: UUID,
        extras: Map<String, String>,
    ) {
        // Given document id and hashmap of extras
        // (for cef its error code and error message)
        // check it does not exist in success
        // store in table of errors with created_at

        TranslationResponse.Error(
            id = translationRequestId,
            createdAt = Instant.now(),
            extras = extras,
        ).let(translationResponseRepository::createFailure)
    }
}
