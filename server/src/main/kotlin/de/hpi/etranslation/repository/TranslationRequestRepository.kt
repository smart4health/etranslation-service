package de.hpi.etranslation.repository

import com.github.michaelbull.result.Result
import java.time.Instant
import java.util.UUID

const val SEND_FAILURE_COUNT_UPPER_LIMIT = 5

interface TranslationRequestRepository {
    fun create(translationRequest: EncryptedTranslationRequest): Result<Unit, Throwable>

    fun get(translationRequestId: UUID): EncryptedTranslationRequest?

    fun delete(translationRequestId: UUID)

    // could be a super-interface in the future
    fun <R> transaction(block: TranslationRequestRepository.() -> R): R
}

data class TranslationRequest(
    val id: UUID,
    val createdAt: Instant,
    val original: String,
    val from: String,
    val to: String,
)

class EncryptedTranslationRequest(
    val id: UUID,
    val createdAt: Instant,
    val original: ByteArray,
    val from: String,
    val to: String,
    val originalNonce: ByteArray,
)
