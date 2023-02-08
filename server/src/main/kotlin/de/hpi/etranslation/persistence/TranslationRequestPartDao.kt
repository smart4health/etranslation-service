package de.hpi.etranslation.persistence

import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transactional
import java.sql.Timestamp
import java.util.Optional
import java.util.UUID

interface TranslationRequestPartDao : Transactional<TranslationRequestPartDao> {
    @SqlUpdate(
        """
        INSERT INTO request_parts (
            id,
            request_id,
            created_at,
            sent_at,
            format,
            content,
            nonce,
            send_failure_at,
            send_failure_count
        ) VALUES (
            :id,
            :requestId,
            :createdAt,
            :sentAt,
            :format,
            :content,
            :nonce,
            :sendFailureAt,
            :sendFailureCount
        )
        """,
    )
    fun insert(@BindBean translationRequestPartEntity: TranslationRequestPartEntity)

    @SqlQuery("SELECT * FROM request_parts WHERE id = ?")
    fun get(id: UUID): Optional<TranslationRequestPartEntity>

    @SqlQuery("SELECT * FROM request_parts WHERE request_id = ?")
    fun getByRequestId(id: UUID): List<TranslationRequestPartEntity>

    @SqlUpdate("DELETE FROM request_parts WHERE id = ?")
    fun delete(id: UUID): Boolean

    @SqlUpdate("DELETE FROM request_parts WHERE request_id = ?")
    fun deleteByRequestId(requestId: UUID): Boolean

    @SqlUpdate(
        """
        UPDATE request_parts 
        SET 
            sent_at = :sentAt,
            send_failure_at = NULL,
            send_failure_count = 0 
        WHERE id = :id
        """,
    )
    fun markSent(
        id: UUID,
        sentAt: Timestamp,
    ): Boolean

    @SqlUpdate(
        """
        UPDATE request_parts
        SET
            sent_at = NULL,
            send_failure_at = :sendFailureAt,
            send_failure_count = :sendFailureCount
        WHERE id = :id
        """,
    )
    fun markSendFailed(
        id: UUID,
        sendFailureAt: Timestamp,
        sendFailureCount: Int,
    )

    @SqlQuery(
        """
        SELECT
            *
        FROM request_parts
        WHERE sent_at IS NULL
        AND (send_failure_at IS NULL OR send_failure_at < :sendFailureAtUpperLimit)
        AND send_failure_count < :sendFailureCountUpperLimit
        ORDER BY created_at ASC
        LIMIT 1
        FOR UPDATE
        SKIP LOCKED
        """,
    )
    fun getNextUnsent(
        sendFailureAtUpperLimit: Timestamp,
        sendFailureCountUpperLimit: Int,
    ): Optional<TranslationRequestPartEntity>
}

class TranslationRequestPartEntity(
    val id: UUID,
    val requestId: UUID,
    val createdAt: Timestamp,
    val sentAt: Timestamp?,
    val format: String,
    val content: ByteArray,
    val nonce: ByteArray,
    val sendFailureAt: Timestamp?,
    val sendFailureCount: Int,
)
