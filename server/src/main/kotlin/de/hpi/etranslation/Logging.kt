package de.hpi.etranslation

import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.Logger
import org.slf4j.LoggerFactory

infix fun String.kv(v: Any?): StructuredArgument =
    StructuredArguments.kv(this, v)

val Any.logger: Logger
    get() = LoggerFactory.getLogger(this::class.java)

fun logger(name: String): Logger = LoggerFactory.getLogger(name)
