package de.hpi.etranslation.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import de.hpi.etranslation.fhirlens.FhirLens
import de.hpi.etranslation.repository.EncryptedTranslationRequest
import de.hpi.etranslation.repository.EncryptedTranslationRequestPart
import de.hpi.etranslation.repository.TranslationRequestPartRepository
import de.hpi.etranslation.repository.TranslationRequestRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateTranslationRequestUseCaseTest {

    private val fakeEncryptedTranslationRequest = EncryptedTranslationRequest(
        id = UUID.randomUUID(),
        createdAt = Instant.now(),
        original = byteArrayOf(),
        from = "from",
        to = "to",
        originalNonce = byteArrayOf(),
    )

    private val fakeEncryptedTranslationRequestPart = EncryptedTranslationRequestPart(
        id = UUID.randomUUID(),
        requestId = fakeEncryptedTranslationRequest.id,
        createdAt = Instant.now(),
        sentAt = null,
        format = "string",
        content = byteArrayOf(),
        nonce = byteArrayOf(),
        sendFailureAt = null,
        sendFailureCount = 0,
    )

    private val mockExtractTranslatablesUseCase = mockk<ExtractTranslatablesUseCase>()

    private val mockTranslationRequestRepository = mockk<TranslationRequestRepository> {
        every { create(any()) } returns Ok(Unit)
    }

    private val mockTranslationRequestPartRepository = mockk<TranslationRequestPartRepository> {
        every { create(any()) } returns Ok(Unit)
    }

    private val mockEncryptDecryptTranslationRequestUseCase = mockk<EncryptDecryptTranslationRequestUseCase> {
        every { encrypt(any()) } returns fakeEncryptedTranslationRequest
    }

    private val mockEncryptDecryptTranslationRequestPartUseCase = mockk<EncryptDecryptTranslationRequestPartUseCase> {
        every { encrypt(any()) } returns fakeEncryptedTranslationRequestPart
    }

    private val underTest = CreateTranslationRequestUseCase(
        uuidGenerator = { fakeEncryptedTranslationRequest.id },
        clock = Clock.systemDefaultZone(),
        extractTranslatablesUseCase = mockExtractTranslatablesUseCase,
        translationRequestRepository = mockTranslationRequestRepository,
        translationRequestPartRepository = mockTranslationRequestPartRepository,
        encryptDecryptTranslationRequestUseCase = mockEncryptDecryptTranslationRequestUseCase,
        encryptDecryptTranslationRequestPartUseCase = mockEncryptDecryptTranslationRequestPartUseCase,
    )

    @Test
    fun `use case returns new id when successful`() {
        every { mockExtractTranslatablesUseCase.invoke(any()) } returns Ok(
            listOf(
                ExtractTranslatablesUseCase.Translatable(
                    content = "content",
                    format = "string",
                ),
            ),
        )

        val actual = underTest.invoke(
            body = "body",
            from = "from",
            to = "to",
        )

        assertEquals(actual.get(), fakeEncryptedTranslationRequest.id)
    }

    @Test
    fun `use case stores request in repository`() {
        every {
            mockExtractTranslatablesUseCase.invoke(any())
        } returns Ok(
            listOf(
                ExtractTranslatablesUseCase.Translatable(
                    content = "content",
                    format = "string",
                ),
            ),
        )

        underTest.invoke(
            body = "body",
            from = "from",
            to = "to",
        )

        verify(exactly = 1) {
            mockTranslationRequestRepository.create(fakeEncryptedTranslationRequest)
        }
    }

    @Test
    fun `use case returns error if it fails to extract a translatable`() {
        every {
            mockExtractTranslatablesUseCase.invoke(any())
        } returns Err(ExtractTranslatablesUseCase.Error.ExtractionFailure(FhirLens.ExtractError.ResourceError(mockk())))

        val actual = underTest.invoke(
            body = "body",
            from = "from",
            to = "to",
        )

        assertTrue { actual.getError() is CreateTranslationRequestUseCase.Error.Extraction }
    }
}
