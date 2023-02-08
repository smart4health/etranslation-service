package de.hpi.etranslation.filter

import de.hpi.etranslation.kv
import de.hpi.etranslation.logger
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import kotlin.time.Duration.Companion.milliseconds

object RequestLogger : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { req ->
        val start = System.currentTimeMillis()
        val res = next(req)
        val duration = System.currentTimeMillis()
            .minus(start)
            .milliseconds

        logger.info(
            "Request received",
            "method" kv req.method,
            "uri" kv req.uri.toString(),
            "status" kv res.status.code,
            "duration" kv duration.toIsoString(),
        )
        res
    }
}
