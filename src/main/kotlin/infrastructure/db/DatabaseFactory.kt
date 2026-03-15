package com.vb.infrastructure.db

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        val url = System.getenv("DB_URL")
        val user = System.getenv("DB_USERNAME")
        val password = System.getenv("DB_PASSWORD")

        Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )
    }
}