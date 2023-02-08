package de.hpi.etranslation

import com.github.michaelbull.result.Result
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.cloudnative.env.Secret
import org.http4k.core.Credentials
import org.http4k.core.Uri
import org.http4k.lens.boolean
import org.http4k.lens.int
import org.http4k.lens.secret
import org.http4k.lens.string
import org.http4k.lens.uri
import com.github.michaelbull.result.runCatching as catch

const val PREFIX = "es."

/**
 * Defaults in server.properties
 */
data class Config(
    val port: Int,
    val basePath: String,
    val languages: List<String>,
    val resourceTypes: List<String>,
    val databaseUrl: String,
    val migrateDatabase: Boolean,
    val cefEndpoint: Uri,
    val cefSuccessCallback: Uri,
    val cefErrorCallback: Uri,
    val cefUsername: String,
    val cefPassword: Secret,
    val databaseKey: Secret,
    val enableDebugController: Boolean,
    val domain: String?,
) {

    fun credentials(): Credentials = cefPassword.use { password ->
        Credentials(cefUsername, password)
    }

    companion object {
        fun from(env: Environment): Result<Config, Throwable> = catch {
            Config(
                port = EnvironmentKey.int().required("port".prefixed()).extract(env),
                basePath = EnvironmentKey.string().required("base-path".prefixed()).extract(env),
                languages = EnvironmentKey.string().multi.required("languages".prefixed()).extract(env),
                resourceTypes = EnvironmentKey.string().multi.required("resource-types".prefixed()).extract(env),
                databaseUrl = EnvironmentKey.string().required("database-url".prefixed()).extract(env),
                migrateDatabase = EnvironmentKey.boolean().required("migrate-database".prefixed()).extract(env),
                cefEndpoint = EnvironmentKey.uri().required("cef-url".prefixed()).extract(env),
                cefSuccessCallback = EnvironmentKey.uri().required("cef-success-callback".prefixed()).extract(env),
                cefErrorCallback = EnvironmentKey.uri().required("cef-error-callback".prefixed()).extract(env),
                cefUsername = EnvironmentKey.string().required("cef-username".prefixed()).extract(env),
                cefPassword = EnvironmentKey.secret().required("cef-password".prefixed()).extract(env),
                databaseKey = EnvironmentKey.secret().required("database-key".prefixed()).extract(env),
                enableDebugController = EnvironmentKey.boolean()
                    .defaulted("enable-debug-controller".prefixed(), false)
                    .extract(env),
                domain = EnvironmentKey.string().optional("domain".prefixed()).extract(env),
            )
        }
    }
}

private fun String.prefixed() = "${PREFIX}$this"
