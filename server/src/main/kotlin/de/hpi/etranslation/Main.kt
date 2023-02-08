package de.hpi.etranslation

import com.github.michaelbull.result.getOrThrow
import de.hpi.etranslation.di.ConfigModule
import de.hpi.etranslation.di.DaggerSingletonComponent
import de.hpi.etranslation.filter.CatchAll
import de.hpi.etranslation.filter.RequestLogger
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.lens.Failure
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.time.Duration
import kotlin.time.DurationUnit

fun main() {
    val startStopwatch = Stopwatch.start()

    val config = Environment.ENV
        .overrides(Environment.fromResource("server.properties"))
        .let(Config::from)
        .getOrThrow()

    val component = DaggerSingletonComponent.builder()
        .configModule(ConfigModule(config))
        .build()

    if (config.migrateDatabase)
        component.migrateDatabaseUseCase()

    component.scheduledExecutorService.every(
        period = Duration.ofSeconds(15),
        initialDelay = Duration.ofSeconds(15),
        command = component.sendTranslationRequestBatchUseCase::invoke,
    )

    val middleware = RequestLogger
        .then(CatchAll)
        .then(
            ServerFilters.CatchLensFailure {
                Response(Status.BAD_REQUEST)
                    .body(it.failures.map(Failure::toString))
            },
        )

    val controllers = listOfNotNull(
        component.configController,
        component.documentController,
        component.cefController,
        component.healthController,
        component.debugController.takeIf { config.enableDebugController },
    )

    val router = config.basePath bind routes(*controllers.map(Controller::routes).toTypedArray())

    val server = middleware.then(router)
        .asServer(Jetty(port = config.port))
        .start()

    val startupDuration = startStopwatch.elapsed()

    logger("main").info(
        "Server started in ${startupDuration.toString(DurationUnit.SECONDS, decimals = 3)}",
        "startupDurationMillis" kv startupDuration.inWholeMilliseconds,
    )

    Runtime.getRuntime().onShutdown {
        val shutdownStopwatch = Stopwatch.start()

        component.scheduledExecutorService.shutdown()

        try {
            server.stop()
        } catch (t: Throwable) {
            // timeout exception most likely
            logger("shutdown").warn("Exception while shutting down", t)
        }
        component.hikariDataSource.close()
        component.scheduledExecutorService.awaitTermination(Duration.ofSeconds(5))

        val shutdownDuration = shutdownStopwatch.elapsed()

        logger("main").info(
            "Server stopped in ${shutdownDuration.toString(DurationUnit.SECONDS, decimals = 3)}",
            "shutdownDurationMillis" kv shutdownDuration.inWholeMilliseconds,
        )
    }
}
