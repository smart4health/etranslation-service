plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradlePlugin.kotlin)
}

gradlePlugin {
    plugins {
        register("kotlinConvention") {
            id = "de.hpi.plugin.kotlin.convention"
            implementationClass = "KotlinConventionPlugin"
        }
    }
}
