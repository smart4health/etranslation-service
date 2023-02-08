package de.hpi.etranslation.persistence

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import de.hpi.etranslation.repository.EncryptedTranslationRequest
import de.hpi.etranslation.repository.TranslationRequestRepository
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.transaction.Transactional
import java.sql.Timestamp
import java.util.Optional
import java.util.UUID
import javax.inject.Inject

interface TranslationRequestDao : Transactional<TranslationRequestDao> {
    @SqlUpdate(
        """
        INSERT INTO requests (
            id,
            created_at,
            from_lang,
            to_lang,
            original_nonce,
            original
        ) VALUES (
            :id, 
            :createdAt,
            :fromLang,
            :toLang,
            :originalNonce, 
            :original
        )
    """,
    )
    fun insert(@BindBean translationRequestEntity: TranslationRequestEntity)

    @SqlQuery("SELECT * FROM requests WHERE id = ?")
    fun get(id: UUID): Optional<TranslationRequestEntity>

    @SqlUpdate("DELETE FROM requests WHERE id = ?")
    fun delete(id: UUID): Boolean
}

class TranslationRequestEntity(
    val id: UUID,
    val createdAt: Timestamp,
    val fromLang: String,
    val toLang: String,
    val originalNonce: ByteArray,
    val original: ByteArray,
) {
    fun asEncryptedTranslationRequest() = EncryptedTranslationRequest(
        id = id,
        createdAt = createdAt.toInstant(),
        from = fromLang,
        to = toLang,
        originalNonce = originalNonce,
        original = original,
    )

    companion object {
        fun from(translationRequest: EncryptedTranslationRequest) = with(translationRequest) {
            TranslationRequestEntity(
                id = id,
                createdAt = createdAt.let(Timestamp::from),
                fromLang = from,
                toLang = to,
                originalNonce = originalNonce,
                original = original,
            )
        }
    }
}

class PostgresTranslationRequestRepository @Inject constructor(
    private val translationRequestDao: TranslationRequestDao,
) : TranslationRequestRepository {
    override fun create(translationRequest: EncryptedTranslationRequest): Result<Unit, Throwable> {
        translationRequest.let(TranslationRequestEntity.Companion::from).let(translationRequestDao::insert)

        return Ok(Unit)
    }

    override fun get(translationRequestId: UUID): EncryptedTranslationRequest? {
        return translationRequestDao.get(translationRequestId).orElse(null)?.asEncryptedTranslationRequest()
    }

    override fun delete(translationRequestId: UUID) {
        translationRequestDao.delete(translationRequestId)
    }

    /**
     * Run a lambda with a new TranslationRequestRepository
     * wrapping a transactional dao
     */
    override fun <R> transaction(block: TranslationRequestRepository.() -> R): R {
        return translationRequestDao.inTransaction<R, Exception> { txn ->
            PostgresTranslationRequestRepository(txn).run(block)
        }
    }
}
