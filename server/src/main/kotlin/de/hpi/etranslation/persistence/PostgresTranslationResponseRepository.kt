package de.hpi.etranslation.persistence

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.combine
import de.hpi.etranslation.repository.ResponseWithFormat
import de.hpi.etranslation.repository.TranslationResponse
import de.hpi.etranslation.repository.TranslationResponseRepository
import org.jdbi.v3.json.Json
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.sql.Timestamp
import java.util.Optional
import java.util.UUID
import javax.inject.Inject

interface TranslationResponseDao {
    @SqlUpdate(
        "INSERT INTO responses (part_id, created_at, translated_text, translated_text_nonce, to_lang, extras) VALUES " +
            "(:partId, :createdAt, :translatedText, :translatedTextNonce, :toLang, :extras)",
    )
    fun create(@BindBean success: TranslationResponseEntity)

    @SqlQuery("SELECT * FROM responses WHERE id = ?")
    fun get(translationResponseId: UUID): Optional<TranslationResponseEntity>

    @SqlQuery(
        """
            SELECT
                p.id part_id,
                p.format,
                r.translated_text,
                r.translated_text_nonce,
                r.extras
            FROM request_parts p
            LEFT JOIN responses r
            ON p.id = r.part_id
            WHERE p.request_id = ?
        """,
    )
    fun getResponsesWithFormatByRequestId(requestId: UUID): List<ResponseWithFormatEntity>

    @SqlUpdate("DELETE FROM responses WHERE part_id = ?")
    fun delete(translationResponseId: UUID): Boolean

    @SqlUpdate(
        """
        DELETE FROM responses r
        USING request_parts p
        WHERE r.part_id = p.id AND p.request_id = ?
    """,
    )
    fun deleteByRequestId(requestId: UUID): Boolean
}

class ResponseWithFormatEntity(
    val partId: UUID,
    val format: String,
    val translatedText: ByteArray?,
    val translatedTextNonce: ByteArray?,
    @get:Json
    @param:Json
    val extras: Map<String, String>?,
)

class TranslationResponseEntity(
    val partId: UUID,
    val createdAt: Timestamp,
    val translatedText: ByteArray?,
    val translatedTextNonce: ByteArray?,
    val toLang: String?,
    @get:Json
    @param:Json
    val extras: Map<String, String>?,
)

class PostgresTranslationResponseRepository @Inject constructor(
    private val translationResponseDao: TranslationResponseDao,
) : TranslationResponseRepository {
    override fun createSuccess(translationSuccessResponse: TranslationResponse.EncryptedSuccess) {
        with(translationSuccessResponse) {
            TranslationResponseEntity(
                partId = id,
                createdAt = Timestamp.from(createdAt),
                translatedText = translatedText,
                translatedTextNonce = translatedTextNonce,
                toLang = toLang,
                extras = null,
            )
        }.let(translationResponseDao::create)
    }

    override fun createFailure(translationErrorResponse: TranslationResponse.Error) {
        with(translationErrorResponse) {
            TranslationResponseEntity(
                partId = id,
                createdAt = Timestamp.from(createdAt),
                translatedText = null,
                translatedTextNonce = null,
                toLang = null,
                extras = extras,
            )
        }.let(translationResponseDao::create)
    }

    override fun getResponse(responseId: UUID): Result<List<TranslationResponse>, IllegalStateException> {
        val entity: TranslationResponseEntity? = translationResponseDao
            .get(responseId)
            .orElse(null)

        return when {
            entity == null -> Ok(emptyList())
            entity.translatedText != null && entity.toLang != null && entity.translatedTextNonce != null && entity.extras == null -> TranslationResponse.EncryptedSuccess(
                id = entity.partId,
                createdAt = entity.createdAt.toInstant(),
                translatedText = entity.translatedText,
                translatedTextNonce = entity.translatedTextNonce,
                toLang = entity.toLang,
            ).let { listOf(it) }.let(::Ok)

            entity.translatedText == null && entity.translatedTextNonce != null && entity.toLang != null && entity.extras != null -> TranslationResponse.Error(
                id = entity.partId,
                createdAt = entity.createdAt.toInstant(),
                extras = entity.extras,
            ).let { listOf(it) }.let(::Ok)

            else -> Err(IllegalStateException("Translation response columns not mutually exclusive"))
        }
    }

    override fun getResponsesWithFormatByRequestId(
        requestId: UUID,
    ): Result<List<ResponseWithFormat>, IllegalStateException> {
        return translationResponseDao
            .getResponsesWithFormatByRequestId(requestId)
            .map { entity ->
                when {
                    entity.translatedText != null && entity.translatedTextNonce != null && entity.extras == null ->
                        ResponseWithFormat.EncryptedSuccess(
                            partId = entity.partId,
                            format = entity.format,
                            translatedText = entity.translatedText,
                            translatedTextNonce = entity.translatedTextNonce,
                        ).let(::Ok)

                    entity.translatedText == null && entity.translatedTextNonce == null && entity.extras != null ->
                        ResponseWithFormat.Error(
                            partId = entity.partId,
                            extras = entity.extras,
                        ).let(::Ok)

                    entity.translatedText == null && entity.translatedTextNonce == null && entity.extras == null ->
                        ResponseWithFormat.NoResponse(
                            partId = entity.partId,
                        ).let(::Ok)

                    else -> Err(IllegalStateException("ResponseWithFormatEntity properties not mutually exclusive"))
                }
            }
            .combine()
    }

    override fun delete(translationResponseId: UUID) {
        translationResponseDao.delete(translationResponseId)
    }

    override fun deleteByRequestId(requestId: UUID) {
        translationResponseDao.deleteByRequestId(requestId)
    }
}
