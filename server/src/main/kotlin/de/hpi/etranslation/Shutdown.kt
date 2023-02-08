package de.hpi.etranslation

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private val isActive = AtomicBoolean(true)

fun Runtime.onShutdown(block: () -> Unit) {
    addShutdownHook(
        thread(start = false, name = "ShutdownHook") {
            if (isActive.compareAndSet(true, false)) {
                block()
            }
        },
    )
}
