plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.http4k.core)
    implementation(libs.http4k.client.okhttp)
    implementation(libs.http4k.format.jackson)
    implementation(libs.http4k.security.digest)
}

gradlePlugin {
    plugins {
        register("cefApi") {
            id = "de.hpi.plugin.cef-api"
            implementationClass = "CefApiPlugin"
        }
    }
}
