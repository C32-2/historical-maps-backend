package com.vb.infrastructure.config

import io.ktor.server.config.ApplicationConfig

data class DatabaseSettings(
    val enabled: Boolean,
    val jdbcUrl: String,
    val user: String,
    val password: String,
    val driverClassName: String,
    val runMigrations: Boolean,
    val flywayLocations: List<String>,
)

internal fun ApplicationConfig.stringSetting(path: String, envName: String? = null, default: String? = null): String {
    val envValue = envName?.let(System::getenv)?.takeIf(String::isNotBlank)
    val configuredValue = propertyOrNull(path)?.getString()?.takeIf(String::isNotBlank)

    return envValue ?: configuredValue ?: default
    ?: error("Missing required setting '$path'")
}

internal fun ApplicationConfig.booleanSetting(path: String, envName: String? = null, default: Boolean): Boolean {
    val envValue = envName?.let(System::getenv)?.toBooleanStrictOrNull()
    val configuredValue = propertyOrNull(path)?.getString()?.toBooleanStrictOrNull()

    return envValue ?: configuredValue ?: default
}

fun ApplicationConfig.toDatabaseSettings(): DatabaseSettings {
    val flywayLocations = stringSetting(
        path = "db.flyway.locations",
        envName = "DB_FLYWAY_LOCATIONS",
        default = "classpath:db/migration",
    )
        .split(",")
        .map(String::trim)
        .filter(String::isNotEmpty)

    return DatabaseSettings(
        enabled = booleanSetting("db.enabled", "DB_ENABLED", default = true),
        jdbcUrl = stringSetting("db.jdbcUrl", "DB_JDBC_URL"),
        user = stringSetting("db.user", "DB_USER"),
        password = stringSetting("db.password", "DB_PASSWORD"),
        driverClassName = stringSetting("db.driverClassName", "DB_DRIVER_CLASS_NAME"),
        runMigrations = booleanSetting("db.flyway.enabled", "DB_FLYWAY_ENABLED", default = true),
        flywayLocations = flywayLocations,
    )
}
