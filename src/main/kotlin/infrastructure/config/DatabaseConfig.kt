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

private fun ApplicationConfig.stringSetting(path: String, envName: String? = null, default: String? = null): String {
    val envValue = envName?.let(System::getenv)?.takeIf(String::isNotBlank)
    val configuredValue = propertyOrNull(path)?.getString()?.takeIf(String::isNotBlank)

    return envValue ?: configuredValue ?: default
    ?: error("Missing required setting '$path'")
}

private fun ApplicationConfig.booleanSetting(path: String, envName: String? = null, default: Boolean): Boolean {
    val envValue = envName?.let(System::getenv)?.toBooleanStrictOrNull()
    val configuredValue = propertyOrNull(path)?.getString()?.toBooleanStrictOrNull()

    return envValue ?: configuredValue ?: default
}

fun ApplicationConfig.toDatabaseSettings(): DatabaseSettings {
    val dbConfig = config("db")
    val flywayConfig = dbConfig.config("flyway")
    val flywayLocations = flywayConfig.stringSetting(
        path = "locations",
        envName = "DB_FLYWAY_LOCATIONS",
        default = "classpath:db/migration",
    )
        .split(",")
        .map(String::trim)
        .filter(String::isNotEmpty)

    return DatabaseSettings(
        enabled = dbConfig.booleanSetting("enabled", "DB_ENABLED", default = true),
        jdbcUrl = dbConfig.stringSetting("jdbcUrl", "DB_JDBC_URL"),
        user = dbConfig.stringSetting("user", "DB_USER"),
        password = dbConfig.stringSetting("password", "DB_PASSWORD"),
        driverClassName = dbConfig.stringSetting("driverClassName", "DB_DRIVER_CLASS_NAME"),
        runMigrations = flywayConfig.booleanSetting("enabled", "DB_FLYWAY_ENABLED", default = true),
        flywayLocations = flywayLocations,
    )
}
