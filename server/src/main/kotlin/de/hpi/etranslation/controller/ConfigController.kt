package de.hpi.etranslation.controller

import de.hpi.etranslation.Config
import de.hpi.etranslation.Controller
import de.hpi.etranslation.body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.routing.bind
import org.http4k.routing.routes
import javax.inject.Inject

class ConfigController @Inject constructor(
    private val config: Config,
) : Controller {
    override fun routes() = routes(
        "/configuration" bind Method.GET to {
            Response(Status.OK)
                .body(
                    ConfigurationResponseBody(
                        languages = config.languages,
                        resourceTypes = config.resourceTypes,
                    ),
                )
        },
        "/openapi.yaml" bind Method.GET to {
            Response(Status.OK)
                .with(Header.CONTENT_TYPE of ContentType.APPLICATION_YAML)
                .body(this::class.java.classLoader.getResourceAsStream("openapi.yaml")!!)
        },
    )

    private data class ConfigurationResponseBody(
        val languages: List<String>,
        val resourceTypes: List<String>,
    )
}
