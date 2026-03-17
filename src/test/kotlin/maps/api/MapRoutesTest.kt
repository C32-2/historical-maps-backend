package com.vb.maps.api

import com.vb.module
import com.vb.maps.domain.Map
import com.vb.maps.domain.MapRepository
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.time.Instant
import java.util.UUID

class MapRoutesTest {

    private val testMap = Map(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        slug = "test-map",
        title = "Test map",
        description = "Description",
        createdAt = Instant.parse("2026-01-01T10:00:00Z"),
        updatedAt = Instant.parse("2026-01-02T10:00:00Z"),
    )

    private val fakeRepository = object : MapRepository {
        override fun getById(id: UUID): Map? = testMap.takeIf { it.id == id }
        override fun getBySlug(slug: String): Map? = testMap.takeIf { it.slug == slug }
        override fun findByTitle(query: String): List<Map> = listOf(testMap)
            .filter { it.title.contains(query, ignoreCase = true) }
        override fun getAll(): List<Map> = listOf(testMap)
    }

    @Test
    fun returnsBadRequestForInvalidMapId() = testApplication {
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
            module(fakeRepository)
        }

        client.get("/maps/not-a-uuid").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun returnsMapBySlugWithoutDatabase() = testApplication {
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
            module(fakeRepository)
        }

        client.get("/maps/slug/test-map").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertNotNull(body<String>().takeIf { it.contains("test-map") })
        }
    }
}
