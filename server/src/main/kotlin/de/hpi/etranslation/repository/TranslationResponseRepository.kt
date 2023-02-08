package de.hpi.etranslation.repository

import com.github.michaelbull.result.Result
import java.time.Instant
import java.util.UUID

interface TranslationResponseRepository {
    fun createSuccess(translationSuccessResponse: TranslationResponse.EncryptedSuccess)

    fun createFailure(translationErrorResponse: TranslationResponse.Error)

    fun getResponse(responseId: UUID): Result<List<TranslationResponse>, IllegalStateException>

    fun getResponsesWithFormatByRequestId(requestId: UUID): Result<List<ResponseWithFormat>, IllegalStateException>

    fun delete(translationResponseId: UUID)

    fun deleteByRequestId(requestId: UUID)
}

sealed class ResponseWithFormat {

    class Success(
        val partId: UUID,
        val format: String,
        val content: String,
    ) // not sealed

    class EncryptedSuccess(
        val partId: UUID,
        val format: String,
        val translatedText: ByteArray,
        val translatedTextNonce: ByteArray,
    ) : ResponseWithFormat()

    class NoResponse(
        val partId: UUID,
    ) : ResponseWithFormat()

    data class Error(
        val partId: UUID,
        val extras: Map<String, String>,
    ) : ResponseWithFormat()
}

sealed class TranslationResponse {

    data class Success(
        val id: UUID,
        val createdAt: Instant,
        val translatedText: String,
        val toLang: String,
    ) // not sealed

    class EncryptedSuccess(
        val id: UUID,
        val createdAt: Instant,
        val translatedText: ByteArray,
        val translatedTextNonce: ByteArray,
        val toLang: String,
    ) : TranslationResponse()

    data class Error(
        val id: UUID,
        val createdAt: Instant,
        val extras: Map<String, String>,
    ) : TranslationResponse()
}
