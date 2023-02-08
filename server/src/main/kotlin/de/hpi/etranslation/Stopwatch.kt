package de.hpi.etranslation

import kotlin.time.Duration.Companion.milliseconds

object Stopwatch {
    fun start() = System.currentTimeMillis().let(::Started)

    class Started(private val startTimeMillis: Long) {
        /**
         * Uses kotlin.time for easier formatting
         */
        fun elapsed() =
            System.currentTimeMillis()
                .minus(startTimeMillis)
                .milliseconds
    }
}
