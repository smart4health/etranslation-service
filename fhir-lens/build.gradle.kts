@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("de.hpi.plugin.kotlin.convention")
}

dependencies {
    implementation(libs.hapi.fhir.base)
    implementation(libs.hapi.fhir.structures.r4)

    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.module.kotlin)

    api(libs.kotlin.result)

    implementation(libs.slf4j.api)
    implementation(libs.logstash.logback.encoder)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.logback.classic)
}
