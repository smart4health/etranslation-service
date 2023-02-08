package de.hpi.etranslation.usecase

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import de.hpi.etranslation.kv
import de.hpi.etranslation.logger
import de.hpi.etranslation.repository.SEND_FAILURE_COUNT_UPPER_LIMIT
import de.hpi.etranslation.repository.TranslationRequestPartRepository
import de.hpi.etranslation.repository.TranslationRequestRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends as many requests as possible in 10 seconds
 *
 * Easy enough to write, later versions could definitely
 * increase throughput by being more dynamic and not being
 * polled by a background executor
 */
@Singleton
class SendTranslationRequestBatchUseCase @Inject constructor(
    private val translationRequestRepository: TranslationRequestRepository,
    private val translationRequestPartRepository: TranslationRequestPartRepository,
    private val encryptDecryptTranslationRequestUseCase: EncryptDecryptTranslationRequestUseCase,
    private val encryptDecryptTranslationRequestPartUseCase: EncryptDecryptTranslationRequestPartUseCase,
    private val sendTranslationRequestPartUseCase: SendTranslationRequestPartUseCase,
) {
    operator fun invoke() {
        val start = Instant.now()

        while (Duration.between(start, Instant.now()) < Duration.ofSeconds(10)) {
            val didProcessRow = translationRequestPartRepository.transaction {
                val part = getNextUnsent(
                    sendFailureAtUpperLimit = Instant.now().minus(Duration.ofSeconds(30)),
                    sendFailureCountUpperLimit = SEND_FAILURE_COUNT_UPPER_LIMIT,
                )
                    ?.let(encryptDecryptTranslationRequestPartUseCase::decrypt)
                    ?: run {
                        return@transaction false
                    }

                val request = translationRequestRepository.get(part.requestId)
                    ?.let(encryptDecryptTranslationRequestUseCase::decrypt)
                    ?: run {
                        return@transaction false
                    }

                this@SendTranslationRequestBatchUseCase.logger.info("Sending request {}", "partId" kv part.id)

                sendTranslationRequestPartUseCase(request, part)
                    .onFailure { error ->
                        when (error) {
                            SendTranslationRequestPartUseCase.Error.AlreadySent -> {
                                this@SendTranslationRequestBatchUseCase.logger.warn(
                                    "getNextUnsent() returned a request that was already sent {}",
                                    "requestId" kv part.id,
                                )
                            }

                            SendTranslationRequestPartUseCase.Error.SendFailed -> {
                                markSendFailed(part.id, Instant.now(), part.sendFailureCount + 1)
                            }
                        }
                    }
                    .onSuccess {
                        markSent(part.id, Instant.now())
                    }

                true
            }

            if (!didProcessRow)
                break
        }
    }
}
