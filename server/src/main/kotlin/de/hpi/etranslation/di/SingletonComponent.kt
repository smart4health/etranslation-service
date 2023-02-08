package de.hpi.etranslation.di

import ca.uhn.fhir.context.FhirContext
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import dagger.Module
import dagger.Provides
import de.hpi.etranslation.Config
import de.hpi.etranslation.controller.CefController
import de.hpi.etranslation.controller.ConfigController
import de.hpi.etranslation.controller.DebugController
import de.hpi.etranslation.controller.DocumentController
import de.hpi.etranslation.controller.HealthController
import de.hpi.etranslation.fhirlens.FhirLens
import de.hpi.etranslation.kv
import de.hpi.etranslation.logger
import de.hpi.etranslation.persistence.PostgresRequestStatusRepository
import de.hpi.etranslation.persistence.PostgresTranslationRequestPartRepository
import de.hpi.etranslation.persistence.PostgresTranslationRequestRepository
import de.hpi.etranslation.persistence.PostgresTranslationResponseRepository
import de.hpi.etranslation.persistence.RequestStatusDao
import de.hpi.etranslation.persistence.TranslationRequestDao
import de.hpi.etranslation.persistence.TranslationRequestPartDao
import de.hpi.etranslation.persistence.TranslationResponseDao
import de.hpi.etranslation.repository.RequestStatusRepository
import de.hpi.etranslation.repository.TranslationRequestPartRepository
import de.hpi.etranslation.repository.TranslationRequestRepository
import de.hpi.etranslation.repository.TranslationResponseRepository
import de.hpi.etranslation.usecase.MigrateDatabaseUseCase
import de.hpi.etranslation.usecase.SendTranslationRequestBatchUseCase
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DigestAuth
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.jackson2.Jackson2Config
import org.jdbi.v3.jackson2.Jackson2Plugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.onDemand
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.inject.Named
import javax.inject.Singleton
import javax.sql.DataSource

@Component(
    modules = [
        ConfigModule::class,
        FhirModule::class,
        DatabaseModule::class,
        ClientModule::class,
        BackgroundModule::class,
        GeneratorModule::class,
        FhirLensModule::class,
    ],
)
@Singleton
interface SingletonComponent {
    val configController: ConfigController

    val documentController: DocumentController

    val cefController: CefController

    val healthController: HealthController

    val debugController: DebugController

    val migrateDatabaseUseCase: MigrateDatabaseUseCase

    val hikariDataSource: HikariDataSource

    val sendTranslationRequestBatchUseCase: SendTranslationRequestBatchUseCase

    val scheduledExecutorService: ScheduledExecutorService
}

@Module
class ConfigModule(private val config: Config) {
    @Provides
    fun provideConfig() = config
}

@Module
object FhirModule {
    @Provides
    @Singleton
    fun provideFhirContext(): FhirContext = FhirContext.forR4()
}

@Module
object DatabaseModule {
    @Provides
    fun provideHikariConfig(config: Config) = HikariConfig().apply {
        jdbcUrl = config.databaseUrl
        maximumPoolSize = 10
    }

    @Provides
    @Singleton
    fun provideHikariDataSource(hikariConfig: HikariConfig): HikariDataSource = HikariDataSource(hikariConfig)

    @Provides
    fun provideDataSource(hikariDataSource: HikariDataSource): DataSource = hikariDataSource

    @Provides
    @Singleton
    fun provideJdbi(dataSource: DataSource): Jdbi = Jdbi.create(dataSource)
        .installPlugin(Jackson2Plugin())
        .also { jdbi ->
            jdbi.getConfig(Jackson2Config::class.java).mapper = jacksonObjectMapper()
        }
        .installPlugin(PostgresPlugin())
        .installPlugin(KotlinPlugin())
        .installPlugin(KotlinSqlObjectPlugin())

    @Provides
    @Singleton
    fun provideTranslationRequestDao(jdbi: Jdbi): TranslationRequestDao = jdbi.onDemand()

    @Provides
    @Singleton
    fun provideTranslationRequestPartDao(jdbi: Jdbi): TranslationRequestPartDao = jdbi.onDemand()

    @Provides
    @Singleton
    fun provideRequestStatusDao(jdbi: Jdbi): RequestStatusDao = jdbi.onDemand()

    @Provides
    @Singleton
    fun provideTranslationResponseDao(jdbi: Jdbi): TranslationResponseDao = jdbi.onDemand()

    @Provides
    fun provideTranslationRequestRepository(
        postgresTranslationRequestRepository: PostgresTranslationRequestRepository,
    ): TranslationRequestRepository = postgresTranslationRequestRepository

    @Provides
    fun provideTranslationResponseRepository(
        postgresTranslationResponseRepository: PostgresTranslationResponseRepository,
    ): TranslationResponseRepository = postgresTranslationResponseRepository

    @Provides
    fun provideTranslationRequestPartRepository(
        postgresTranslationRequestPartRepository: PostgresTranslationRequestPartRepository,
    ): TranslationRequestPartRepository = postgresTranslationRequestPartRepository

    @Provides
    fun provideRequestStatusRepository(
        postgresRequestStatusRepository: PostgresRequestStatusRepository,
    ): RequestStatusRepository = postgresRequestStatusRepository

    @Provides
    @Singleton
    fun provideLazySodium(): LazySodiumJava = LazySodiumJava(SodiumJava(), StandardCharsets.UTF_8)

    @Provides
    @Singleton
    fun provideKey(config: Config): Key =
        config.databaseKey.use { hexBytes ->
            if (hexBytes == "hardcoded") {
                ByteArray(SecretBox.KEYBYTES) { i -> i.toByte() }
                    .let(Key::fromBytes)
                    .also { hardcodedKey ->
                        logger.warn(
                            "Using hardcoded key, do NOT run in production {}",
                            "ES_DATABASE_KEY" kv hardcodedKey.asHexString,
                        )
                    }
            } else {
                Key.fromHexString(hexBytes)
            }
        }
}

@Module
object ClientModule {
    @Provides
    @Singleton
    @Named("cefTranslationClient")
    fun provideCefTranslationClient(config: Config): HttpHandler =
        config.credentials()
            .let(ClientFilters::DigestAuth)
            .then(ClientFilters.SetBaseUriFrom(config.cefEndpoint))
            .then(OkHttp())
}

@Module
object BackgroundModule {
    /**
     * inspired by Spring
     *
     * https://github.com/spring-projects/spring-framework/blob/50973f73c70723198752d6ded3ad6af2c05b5674/spring-context/src/main/java/org/springframework/scheduling/concurrent/ScheduledExecutorFactoryBean.java#L187
     */
    @Provides
    @Singleton
    fun provideScheduledExecutorService(): ScheduledExecutorService =
        Executors.newScheduledThreadPool(1)
}

@Module
object GeneratorModule {
    @Provides
    @Singleton
    fun provideUuidGenerator(): () -> UUID = UUID::randomUUID

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
object FhirLensModule {
    @Provides
    @Singleton
    fun provideFhirLens() = FhirLens()
}
