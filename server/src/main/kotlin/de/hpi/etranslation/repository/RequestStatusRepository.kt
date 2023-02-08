package de.hpi.etranslation.repository

import java.time.Instant
import java.util.UUID

/**
 * Read only repository to query a view of the data relevant for status reporting
 */
interface RequestStatusRepository {
    fun getStatusViewsByRequestId(requestId: UUID): List<StatusView>
}

/**
 * Status for a single request part
 */
data class StatusView(
    val partId: UUID,
    val createdAt: Instant,
    val sendFailureCount: Int,
    val sentAt: Instant?,
    val responseStatus: ResponseStatus,
) {
    enum class ResponseStatus {
        Success,
        Error,
        NotFound,
        InvalidResponseState,
    }
}
