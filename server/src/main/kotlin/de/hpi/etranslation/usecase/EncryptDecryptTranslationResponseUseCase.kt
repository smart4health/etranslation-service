package de.hpi.etranslation.usecase

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import de.hpi.etranslation.repository.ResponseWithFormat
import de.hpi.etranslation.repository.TranslationResponse
import javax.inject.Inject

class EncryptDecryptTranslationResponseUseCase @Inject constructor(
    private val lazySodiumJava: LazySodiumJava,
    private val key: Key,
) {
    @Throws(SodiumException::class)
    fun encrypt(translationResponse: TranslationResponse.Success): TranslationResponse.EncryptedSuccess {
        val translatedTextNonce = lazySodiumJava.nonce(SecretBox.NONCEBYTES)
        val translatedText =
            lazySodiumJava.cryptoSecretBoxEasy(translationResponse.translatedText, translatedTextNonce, key)
                .let(lazySodiumJava::sodiumHex2Bin)

        return TranslationResponse.EncryptedSuccess(
            id = translationResponse.id,
            createdAt = translationResponse.createdAt,
            translatedText = translatedText,
            translatedTextNonce = translatedTextNonce,
            toLang = translationResponse.toLang,
        )
    }

    @Throws(SodiumException::class)
    fun decrypt(encryptedTranslationResponseSuccess: TranslationResponse.EncryptedSuccess): TranslationResponse.Success {
        val translatedTextCipherText = lazySodiumJava.sodiumBin2Hex(encryptedTranslationResponseSuccess.translatedText)
        val translatedText = lazySodiumJava.cryptoSecretBoxOpenEasy(
            translatedTextCipherText,
            encryptedTranslationResponseSuccess.translatedTextNonce,
            key,
        )

        return TranslationResponse.Success(
            id = encryptedTranslationResponseSuccess.id,
            createdAt = encryptedTranslationResponseSuccess.createdAt,
            translatedText = translatedText,
            toLang = encryptedTranslationResponseSuccess.toLang,
        )
    }

    @Throws(SodiumException::class)
    fun decrypt(
        responseWithFormat: ResponseWithFormat.EncryptedSuccess,
    ): ResponseWithFormat.Success {
        val contentCipherText = lazySodiumJava.sodiumBin2Hex(responseWithFormat.translatedText)
        val content = lazySodiumJava.cryptoSecretBoxOpenEasy(
            contentCipherText,
            responseWithFormat.translatedTextNonce,
            key,
        )

        return ResponseWithFormat.Success(
            partId = responseWithFormat.partId,
            format = responseWithFormat.format,
            content = content,
        )
    }
}
