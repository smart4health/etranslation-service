package de.hpi.etranslation

import org.http4k.routing.RoutingHttpHandler

interface Controller {
    fun routes(): RoutingHttpHandler
}
