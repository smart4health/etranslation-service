package de.hpi.etranslation.usecase

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import javax.inject.Inject
import javax.sql.DataSource

class MigrateDatabaseUseCase @Inject constructor(
    private val dataSource: DataSource,
) {
    operator fun invoke() {
        // not too interested in keeping liquibase machinery around more than needed
        val database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(JdbcConnection(dataSource.connection))

        Liquibase("db/changelog.yaml", ClassLoaderResourceAccessor(), database)
            .update(Contexts(), LabelExpression())
    }
}
