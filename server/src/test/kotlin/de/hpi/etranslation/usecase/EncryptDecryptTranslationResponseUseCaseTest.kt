package de.hpi.etranslation.usecase

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.utils.Key
import de.hpi.etranslation.repository.TranslationResponse
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class EncryptDecryptTranslationResponseUseCaseTest {
    private val lazySodiumJava = LazySodiumJava(SodiumJava(), StandardCharsets.UTF_8)

    private val fakeKey = Key.fromBytes(ByteArray(32) { 0 })

    private val underTest = EncryptDecryptTranslationResponseUseCase(
        lazySodiumJava = lazySodiumJava,
        key = fakeKey,
    )

    private val fakeTranslationResponse = TranslationResponse.Success(
        id = UUID.randomUUID(),
        createdAt = Instant.now(),
        translatedText = "translated",
        toLang = "toLang",
    )

    @Test
    fun `roundtrip through encryption works`() {
        val encrypted = underTest.encrypt(fakeTranslationResponse)

        val actual = underTest.decrypt(encrypted)

        assertEquals(fakeTranslationResponse, actual)
    }
}
