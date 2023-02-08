package de.hpi.etranslation.filter

import de.hpi.etranslation.logger
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status

object CatchAll : Filter {
    override fun invoke(next: HttpHandler): HttpHandler {
        return { req ->
            try {
                next(req)
            } catch (t: Throwable) {
                logger.info("Exception occurred while processing request", t)
                Response(Status.INTERNAL_SERVER_ERROR)
            }
        }
    }
}
