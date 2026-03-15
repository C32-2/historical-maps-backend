package com.vb.infrastructure.db

import com.vb.infrastructure.config.DatabaseSettings
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init(settings: DatabaseSettings) {
        Database.connect(
            url = settings.jdbcUrl,
            driver = settings.driverClassName,
            user = settings.user,
            password = settings.password,
        )
    }

    fun migrate(settings: DatabaseSettings) {
        Flyway.configure()
            .dataSource(settings.jdbcUrl, settings.user, settings.password)
            .locations(*settings.flywayLocations.toTypedArray())
            .load()
            .migrate()
    }
}
