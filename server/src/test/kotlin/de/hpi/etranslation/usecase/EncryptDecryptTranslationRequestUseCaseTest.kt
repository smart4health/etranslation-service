package de.hpi.etranslation.usecase

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.utils.Key
import de.hpi.etranslation.repository.TranslationRequest
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class EncryptDecryptTranslationRequestUseCaseTest {
    private val lazySodiumJava = LazySodiumJava(SodiumJava(), StandardCharsets.UTF_8)

    private val fakeKey = Key.fromBytes(ByteArray(32) { 0 })

    private val underTest = EncryptDecryptTranslationRequestUseCase(
        lazySodiumJava = lazySodiumJava,
        key = fakeKey,
    )

    private val fakeTranslationRequest = TranslationRequest(
        id = UUID.randomUUID(),
        createdAt = Instant.now(),
        original = "original",
        from = "from",
        to = "to",
    )

    @Test
    fun `roundtrip through encryption works`() {
        val encrypted = underTest.encrypt(fakeTranslationRequest)

        val actual = underTest.decrypt(encrypted)

        assertEquals(fakeTranslationRequest, actual)
    }
}
