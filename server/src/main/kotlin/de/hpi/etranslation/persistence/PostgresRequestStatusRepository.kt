package de.hpi.etranslation.persistence

import de.hpi.etranslation.repository.RequestStatusRepository
import de.hpi.etranslation.repository.StatusView
import java.util.UUID
import javax.inject.Inject

class PostgresRequestStatusRepository @Inject constructor(
    private val requestStatusDao: RequestStatusDao,
) : RequestStatusRepository {
    override fun getStatusViewsByRequestId(requestId: UUID): List<StatusView> {
        return requestStatusDao
            .getRequestStatusByRequestId(requestId)
            .map(RequestStatusEntity::toStatusView)
    }
}

private fun RequestStatusEntity.toStatusView(): StatusView {
    val responseStatus = when {
        translatedText == null && extras == null -> StatusView.ResponseStatus.NotFound
        translatedText == null && extras != null -> StatusView.ResponseStatus.Error
        translatedText != null && extras == null -> StatusView.ResponseStatus.Success
        else -> StatusView.ResponseStatus.InvalidResponseState
    }

    return StatusView(
        partId = partId,
        createdAt = createdAt.toInstant(),
        sendFailureCount = sendFailureCount,
        sentAt = sentAt?.toInstant(),
        responseStatus = responseStatus,
    )
}
