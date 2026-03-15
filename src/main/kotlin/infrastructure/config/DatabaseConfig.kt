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

fun ApplicationConfig.toDatabaseSettings(): DatabaseSettings {
    val dbConfig = config("db")
    val flywayConfig = dbConfig.config("flyway")
    val flywayLocations = flywayConfig.propertyOrNull("locations")
        ?.getString()
        ?.split(",")
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?: listOf("classpath:db/migration")

    return DatabaseSettings(
        enabled = dbConfig.propertyOrNull("enabled")?.getString()?.toBooleanStrictOrNull() ?: true,
        jdbcUrl = dbConfig.property("jdbcUrl").getString(),
        user = dbConfig.property("user").getString(),
        password = dbConfig.property("password").getString(),
        driverClassName = dbConfig.property("driverClassName").getString(),
        runMigrations = flywayConfig.propertyOrNull("enabled")?.getString()?.toBooleanStrictOrNull() ?: true,
        flywayLocations = flywayLocations,
    )
}
