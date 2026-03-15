package com.vb.plugins

import com.vb.infrastructure.config.toDatabaseSettings
import com.vb.infrastructure.db.DatabaseFactory
import io.ktor.server.application.createApplicationPlugin

val DatabasePlugin = createApplicationPlugin("DatabasePlugin") {
    val settings = application.environment.config.toDatabaseSettings()

    if (!settings.enabled) {
        return@createApplicationPlugin
    }

    DatabaseFactory.init(settings)

    if (settings.runMigrations) {
        DatabaseFactory.migrate(settings)
    }
}
