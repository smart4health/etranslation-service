import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class KotlinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.jvm")
        }

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain {
                languageVersion.convention(JavaLanguageVersion.of(17))
            }
        }

        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }
    }
}
