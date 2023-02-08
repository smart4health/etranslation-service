package de.hpi.etranslation

import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

fun ScheduledExecutorService.every(
    period: Duration,
    initialDelay: Duration = Duration.ZERO,
    command: Runnable,
) {
    scheduleAtFixedRate({
        try {
            command.run()
        } catch (t: Throwable) {
            logger.warn("Failed to execute command", t)
        }
    }, initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS)
}

fun ScheduledExecutorService.awaitTermination(timeout: Duration) {
    try {
        awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)
    } catch (t: Throwable) {
        logger.warn("Exception while awaiting termination", t)
    }
}
