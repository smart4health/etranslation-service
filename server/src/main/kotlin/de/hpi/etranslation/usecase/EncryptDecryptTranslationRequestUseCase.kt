package de.hpi.etranslation.usecase

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import de.hpi.etranslation.repository.EncryptedTranslationRequest
import de.hpi.etranslation.repository.TranslationRequest
import javax.inject.Inject

class EncryptDecryptTranslationRequestUseCase @Inject constructor(
    private val lazySodiumJava: LazySodiumJava,
    private val key: Key,
) {
    @Throws(SodiumException::class)
    fun encrypt(translationRequest: TranslationRequest): EncryptedTranslationRequest {
        val originalNonce = lazySodiumJava.nonce(SecretBox.NONCEBYTES)
        val original = lazySodiumJava.cryptoSecretBoxEasy(translationRequest.original, originalNonce, key)
            .let(lazySodiumJava::sodiumHex2Bin)

        return EncryptedTranslationRequest(
            id = translationRequest.id,
            createdAt = translationRequest.createdAt,
            original = original,
            from = translationRequest.from,
            to = translationRequest.to,
            originalNonce = originalNonce,
        )
    }

    @Throws(SodiumException::class)
    fun decrypt(encryptedTranslationRequest: EncryptedTranslationRequest): TranslationRequest {
        val originalCipherText = lazySodiumJava.sodiumBin2Hex(encryptedTranslationRequest.original)
        val original = lazySodiumJava.cryptoSecretBoxOpenEasy(
            originalCipherText,
            encryptedTranslationRequest.originalNonce,
            key,
        )

        return TranslationRequest(
            id = encryptedTranslationRequest.id,
            createdAt = encryptedTranslationRequest.createdAt,
            original = original,
            from = encryptedTranslationRequest.from,
            to = encryptedTranslationRequest.to,
        )
    }
}
