package de.hpi.etranslation

import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.format.Jackson.auto

// not sure why this isn't automatically done by http4k-format-jackson
inline fun <reified T : Any> Response.body(body: T): Response =
    this.with(Body.auto<T>().toLens() of body)
