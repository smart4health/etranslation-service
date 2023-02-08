package de.hpi.etranslation.controller

import de.hpi.etranslation.Controller
import de.hpi.etranslation.body
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import javax.inject.Inject

class HealthController @Inject constructor(
    private val scheduledExecutorService: ScheduledExecutorService,
) : Controller {
    @Suppress("unused")
    override fun routes(): RoutingHttpHandler = "/health" bind Method.GET to {
        val health = object {
            val scheduledExecutorServiceHealth = object {
                val isShutdown = scheduledExecutorService.isShutdown
                val queueSize =
                    (scheduledExecutorService as? ScheduledThreadPoolExecutor)
                        ?.queue
                        ?.size
            }
        }

        Response(Status.OK)
            .body(health)
    }
}
