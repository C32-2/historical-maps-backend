package com.vb.infrastructure.db

import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init(application: Application) {
        val config = application.environment.config

        val jdbcUrl = config.property("db.jdbcUrl").getString()
        val user = config.property("db.user").getString()
        val password = config.property("db.password").getString()
        val driverClassName = config.property("db.driverClassName").getString()
    }
}