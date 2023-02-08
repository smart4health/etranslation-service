package de.hpi.etranslation.usecase

import de.hpi.etranslation.repository.RequestStatusRepository
import de.hpi.etranslation.repository.StatusView
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class GetTranslationStatusUseCase @Inject constructor(
    private val requestStatusRepository: RequestStatusRepository,
) {
    operator fun invoke(translationRequestId: UUID): TranslationStatus? {
        val statuses = requestStatusRepository.getStatusViewsByRequestId(translationRequestId)

        val status = when {
            statuses.any { it.sentAt == null && it.sendFailureCount > 0 } ->
                TranslationStatus.Status.SEND_ERROR

            statuses.any { it.sentAt == null } ->
                TranslationStatus.Status.UNTRANSLATED

            statuses.all { it.responseStatus == StatusView.ResponseStatus.Success } ->
                TranslationStatus.Status.TRANSLATED

            statuses.any {
                it.responseStatus == StatusView.ResponseStatus.InvalidResponseState ||
                    it.responseStatus == StatusView.ResponseStatus.Error
            } -> TranslationStatus.Status.TRANSLATION_ERROR

            statuses.all {
                it.sentAt != null && it.responseStatus == StatusView.ResponseStatus.NotFound
            } -> TranslationStatus.Status.SENT

            else -> TranslationStatus.Status.UNTRANSLATED
        }

        val at = statuses.maxOfOrNull {
            it.sentAt ?: it.createdAt
        } ?: return null

        return TranslationStatus(status, at)
    }

    data class TranslationStatus(
        val status: Status,
        val at: Instant,
    ) {
        enum class Status {
            UNTRANSLATED,
            SENT,
            TRANSLATED,
            TRANSLATION_ERROR,
            SEND_ERROR,
        }
    }
}
