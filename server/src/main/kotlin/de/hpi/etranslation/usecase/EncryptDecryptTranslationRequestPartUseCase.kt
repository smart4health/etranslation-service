package de.hpi.etranslation.usecase

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import de.hpi.etranslation.repository.EncryptedTranslationRequestPart
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class EncryptDecryptTranslationRequestPartUseCase @Inject constructor(
    private val lazySodiumJava: LazySodiumJava,
    private val key: Key,
) {
    @Throws(SodiumException::class)
    fun encrypt(
        requestPart: TranslationRequestPart,
    ): EncryptedTranslationRequestPart {
        val nonce = lazySodiumJava.nonce(SecretBox.NONCEBYTES)
        val content = lazySodiumJava
            .cryptoSecretBoxEasy(requestPart.content, nonce, key)
            .let(lazySodiumJava::sodiumHex2Bin)

        return EncryptedTranslationRequestPart(
            id = requestPart.id,
            requestId = requestPart.requestId,
            createdAt = requestPart.createdAt,
            sentAt = requestPart.sentAt,
            format = requestPart.format,
            content = content,
            nonce = nonce,
            sendFailureAt = requestPart.sendFailureAt,
            sendFailureCount = requestPart.sendFailureCount,
        )
    }

    @Throws(SodiumException::class)
    fun decrypt(
        encryptedRequestPart: EncryptedTranslationRequestPart,
    ): TranslationRequestPart {
        val contentCipherText = lazySodiumJava.sodiumBin2Hex(encryptedRequestPart.content)
        val content = lazySodiumJava.cryptoSecretBoxOpenEasy(
            contentCipherText,
            encryptedRequestPart.nonce,
            key,
        )

        return TranslationRequestPart(
            id = encryptedRequestPart.id,
            requestId = encryptedRequestPart.requestId,
            createdAt = encryptedRequestPart.createdAt,
            sentAt = encryptedRequestPart.sentAt,
            format = encryptedRequestPart.format,
            content = content,
            sendFailureAt = encryptedRequestPart.sendFailureAt,
            sendFailureCount = encryptedRequestPart.sendFailureCount,
        )
    }
}

data class TranslationRequestPart(
    val id: UUID,
    val requestId: UUID,
    val createdAt: Instant,
    val sentAt: Instant?,
    val format: String,
    val content: String,
    val sendFailureAt: Instant?,
    val sendFailureCount: Int,
)
