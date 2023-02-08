
import com.github.jk1.license.render.JsonReportRenderer
import de.gesellix.gradle.docker.tasks.DockerRmTask
import de.gesellix.gradle.docker.tasks.DockerRunTask
import de.gesellix.gradle.docker.tasks.DockerStopTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.de.gesellix.docker)
    id("de.hpi.plugin.cef-api")

    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.org.jetbrains.kotlin.kapt) apply false

    alias(libs.plugins.jk1.dependencylicensereport)
}

tasks.register<DockerRunTask>("postgresRun") {
    imageName.set("postgres")
    containerName.set("postgres")
    env.addAll(
        "POSTGRES_USER=user",
        "POSTGRES_PASSWORD=pass",
        "POSTGRES_DB=etranslation",
    )
    ports.addAll("5432:5432")
}

val postgresStop = tasks.register<DockerStopTask>("postgresStop") {
    containerId.set("postgres")
}

tasks.register<DockerRmTask>("postgresRm") {
    dependsOn(postgresStop)
    containerId.set("postgres")
}

cef {
    credentials = providers.environmentVariable("CEF_CREDENTIALS")
}

licenseReport {
    // explore the different names with
    // $ jq -r '.dependencies[] | .moduleLicense | select(. != null)' index.json | sort | uniq
    // on the json report.
    configurations = arrayOf("runtimeClasspath", "compileClasspath")
    renderers = arrayOf(JsonReportRenderer())
}
