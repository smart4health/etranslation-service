import de.hpi.etranslation.buildlogic.cefapi.CefEtranslateTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register

abstract class CefApiExtension {
    abstract var credentials: Provider<String>
}

class CefApiPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
        val extension = extensions.create("cef", CefApiExtension::class.java)

        tasks.register<CefEtranslateTask>("cefEtranslateTask") {
            credentials.set(extension.credentials)
        }
    }
}
