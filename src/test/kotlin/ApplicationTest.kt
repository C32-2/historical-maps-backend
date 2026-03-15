package com.vb

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testInvalidMapId() = testApplication {
        environment {
            config = MapApplicationConfig(
                "db.enabled" to "false",
                "db.jdbcUrl" to "jdbc:postgresql://localhost:5432/test",
                "db.user" to "test",
                "db.password" to "test",
                "db.driverClassName" to "org.postgresql.Driver",
                "db.flyway.enabled" to "false",
            )
        }

        application {
            module()
        }

        client.get("/maps/not-a-uuid").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }
}
