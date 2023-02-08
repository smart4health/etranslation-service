package de.hpi.etranslation.persistence

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import de.hpi.etranslation.repository.EncryptedTranslationRequestPart
import de.hpi.etranslation.repository.TranslationRequestPartRepository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class PostgresTranslationRequestPartRepository @Inject constructor(
    private val translationRequestPartDao: TranslationRequestPartDao,
) : TranslationRequestPartRepository {
    override fun create(requestPart: EncryptedTranslationRequestPart): Result<Unit, Throwable> {
        requestPart
            .asTranslationRequestPartEntity()
            .let(translationRequestPartDao::insert)

        return Ok(Unit)
    }

    override fun get(requestPartId: UUID): EncryptedTranslationRequestPart? {
        return translationRequestPartDao
            .get(requestPartId)
            .orElse(null)
            ?.asEncryptedTranslationRequestPart()
    }

    override fun getByRequestId(requestId: UUID): List<EncryptedTranslationRequestPart> {
        return translationRequestPartDao
            .getByRequestId(requestId)
            .map(TranslationRequestPartEntity::asEncryptedTranslationRequestPart)
    }

    override fun delete(requestPartId: UUID) {
        translationRequestPartDao.delete(requestPartId)
    }

    override fun deleteByRequestId(requestId: UUID) {
        translationRequestPartDao.deleteByRequestId(requestId)
    }

    override fun markSent(requestPartId: UUID, sentAt: Instant): Boolean {
        return translationRequestPartDao.markSent(
            id = requestPartId,
            sentAt = sentAt.let(Timestamp::from),
        )
    }

    override fun markSendFailed(requestPartId: UUID, sendFailureAt: Instant, sendFailureCount: Int) {
        translationRequestPartDao.markSendFailed(
            id = requestPartId,
            sendFailureAt = sendFailureAt.let(Timestamp::from),
            sendFailureCount = sendFailureCount,
        )
    }

    override fun getNextUnsent(
        sendFailureAtUpperLimit: Instant,
        sendFailureCountUpperLimit: Int,
    ): EncryptedTranslationRequestPart? {
        if (!translationRequestPartDao.handle.isInTransaction)
            error("getNextUnsent must be called in a transaction")

        return translationRequestPartDao
            .getNextUnsent(
                sendFailureAtUpperLimit = sendFailureAtUpperLimit.let(Timestamp::from),
                sendFailureCountUpperLimit = sendFailureCountUpperLimit,
            )
            .orElse(null)
            ?.asEncryptedTranslationRequestPart()
    }

    override fun <R> transaction(block: TranslationRequestPartRepository.() -> R): R {
        return translationRequestPartDao.inTransaction<R, Exception> { txn ->
            PostgresTranslationRequestPartRepository(txn).run(block)
        }
    }
}

private fun TranslationRequestPartEntity.asEncryptedTranslationRequestPart() = EncryptedTranslationRequestPart(
    id = id,
    requestId = requestId,
    createdAt = createdAt.toInstant(),
    sentAt = sentAt?.toInstant(),
    format = format,
    content = content,
    nonce = nonce,
    sendFailureAt = sendFailureAt?.toInstant(),
    sendFailureCount = sendFailureCount,
)

private fun EncryptedTranslationRequestPart.asTranslationRequestPartEntity() = TranslationRequestPartEntity(
    id = id,
    requestId = requestId,
    createdAt = createdAt.let(Timestamp::from),
    sentAt = sentAt?.let(Timestamp::from),
    format = format,
    content = content,
    nonce = nonce,
    sendFailureAt = sendFailureAt?.let(Timestamp::from),
    sendFailureCount = sendFailureCount,
)
