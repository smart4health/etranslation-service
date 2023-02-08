package de.hpi.etranslation.fhirlens

import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal infix fun String.kv(v: Any?): StructuredArgument =
    StructuredArguments.kv(this, v)

internal val Any.logger: Logger
    get() = LoggerFactory.getLogger(this::class.java)

internal fun logger(name: String): Logger = LoggerFactory.getLogger(name)
