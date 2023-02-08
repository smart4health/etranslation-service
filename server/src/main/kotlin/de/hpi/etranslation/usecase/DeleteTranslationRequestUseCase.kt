package de.hpi.etranslation.usecase

import de.hpi.etranslation.repository.TranslationRequestPartRepository
import de.hpi.etranslation.repository.TranslationRequestRepository
import de.hpi.etranslation.repository.TranslationResponseRepository
import java.util.UUID
import javax.inject.Inject

class DeleteTranslationRequestUseCase @Inject constructor(
    private val translationRequestRepository: TranslationRequestRepository,
    private val translationRequestPartRepository: TranslationRequestPartRepository,
    private val translationResponseRepository: TranslationResponseRepository,
) {
    operator fun invoke(translationRequestId: UUID) {
        translationResponseRepository.delete(translationRequestId)
        translationRequestPartRepository.deleteByRequestId(translationRequestId)
        translationRequestRepository.delete(translationRequestId)
    }
}
