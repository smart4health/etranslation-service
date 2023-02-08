@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("de.hpi.plugin.kotlin.convention")
    id("org.gradle.application")
    alias(libs.plugins.com.google.cloud.tools.jib)
    alias(libs.plugins.com.github.johnrengelman.shadow)
    alias(libs.plugins.org.jetbrains.kotlin.kapt)
}

group = "de.hpi.etranslation"
version = "0.1.0-SNAPSHOT"

application {
    mainClass.set("de.hpi.etranslation.MainKt")
}

jib.to {
    image = "registry.gitlab.hpi.de/smart4health/etranslation-service"
    tags = setOf("latest", project.version.toString())
}

dependencies {
    implementation(projects.fhirLens)

    implementation(libs.http4k.core)
    implementation(libs.http4k.client.okhttp)
    implementation(libs.http4k.cloudnative)
    implementation(libs.http4k.security.digest)
    implementation(libs.http4k.server.jetty)
    implementation(libs.http4k.format.jackson)
    implementation(libs.http4k.format.jackson.xml)

    implementation(libs.kotlin.result)

    // mainly programming against slf4j
    implementation(libs.slf4j.api)
    // implements the slf4j interfaces
    runtimeOnly(libs.logback.classic)
    // used by logback to encode into json.  Also provides StructuredArguments markers
    // since it doesn't yet seem to work with slf4j 2.x structured arguments
    implementation(libs.logstash.logback.encoder)
    // bridges liquibase's logger and slf4j
    runtimeOnly(libs.liquibase.slf4j)

    implementation(libs.hapi.fhir.base)
    implementation(libs.hapi.fhir.structures.r4)

    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

    implementation(libs.liquibase.core)
    implementation(libs.jdbi.core)
    implementation(libs.jdbi.jackson2)
    implementation(libs.jdbi.kotlin)
    implementation(libs.jdbi.kotlin.sqlobject)
    implementation(libs.jdbi.postgres)
    implementation(libs.hikari)
    runtimeOnly(libs.postgres)
    runtimeOnly(libs.snakeyaml)

    implementation(libs.lazysodium.java)
    runtimeOnly(libs.jna)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
}
