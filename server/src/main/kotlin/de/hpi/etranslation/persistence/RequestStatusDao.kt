package de.hpi.etranslation.persistence

import org.jdbi.v3.json.Json
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.transaction.Transactional
import java.sql.Timestamp
import java.util.UUID

interface RequestStatusDao : Transactional<RequestStatusDao> {
    @SqlQuery(
        """
        SELECT
            p.id partId,
            p.created_at,
            p.send_failure_count,
            p.sent_at,
            r.translated_text,
            r.extras
        FROM request_parts p
        LEFT JOIN responses r
        ON p.id = r.part_id
        WHERE p.request_id = ?
    """,
    )
    fun getRequestStatusByRequestId(requestId: UUID): List<RequestStatusEntity>
}

class RequestStatusEntity(
    val partId: UUID,
    val createdAt: Timestamp,
    val sendFailureCount: Int,
    val sentAt: Timestamp?,
    val translatedText: ByteArray?,
    @get:Json
    @param:Json
    val extras: Map<String, String>?,
)
