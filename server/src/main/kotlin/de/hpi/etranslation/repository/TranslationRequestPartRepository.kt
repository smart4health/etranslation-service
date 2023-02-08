package de.hpi.etranslation.repository

import com.github.michaelbull.result.Result
import java.time.Instant
import java.util.UUID

interface TranslationRequestPartRepository {
    fun create(requestPart: EncryptedTranslationRequestPart): Result<Unit, Throwable>

    fun get(requestPartId: UUID): EncryptedTranslationRequestPart?

    fun getByRequestId(requestId: UUID): List<EncryptedTranslationRequestPart>

    fun delete(requestPartId: UUID)

    fun deleteByRequestId(requestId: UUID)

    fun markSent(requestPartId: UUID, sentAt: Instant): Boolean

    fun markSendFailed(requestPartId: UUID, sendFailureAt: Instant, sendFailureCount: Int)

    /**
     * Must be used inside a [transaction]
     *
     * @param sendFailureAtUpperLimit use `now - duration` to ensure failed
     *                                requests aren't immediately retried
     */
    fun getNextUnsent(
        sendFailureAtUpperLimit: Instant,
        sendFailureCountUpperLimit: Int,
    ): EncryptedTranslationRequestPart?

    // could be a super-interface in the future
    fun <R> transaction(block: TranslationRequestPartRepository.() -> R): R
}

class EncryptedTranslationRequestPart(
    val id: UUID,
    val requestId: UUID,
    val createdAt: Instant,
    val sentAt: Instant?,
    val format: String,
    val content: ByteArray,
    val nonce: ByteArray,
    val sendFailureAt: Instant?,
    val sendFailureCount: Int,
)
