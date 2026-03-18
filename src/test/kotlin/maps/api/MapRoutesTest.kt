package com.vb.maps.api

import com.vb.module
import com.vb.maps.application.MapStorage
import com.vb.maps.domain.Map
import com.vb.maps.domain.MapRepository
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import io.ktor.client.request.forms.formData
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
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
        storageKey = "maps/11111111-1111-1111-1111-111111111111/tiles.pmtiles",
    )

    private val storedMaps = mutableListOf(testMap)

    private val fakeRepository = object : MapRepository {
        override fun getById(id: UUID): Map? = storedMaps.firstOrNull { it.id == id }
        override fun getBySlug(slug: String): Map? = storedMaps.firstOrNull { it.slug == slug }
        override fun findByTitle(query: String): List<Map> = storedMaps
            .filter { it.title.contains(query, ignoreCase = true) }
        override fun getAll(): List<Map> = storedMaps.toList()
        override fun addMap(map: Map) {
            storedMaps.add(map)
        }

        override fun deleteById(id: UUID) {
            storedMaps.removeIf { it.id == id }
        }
    }

    private val savedFiles = mutableMapOf<String, ByteArray>()

    private val fakeStorage = object : MapStorage {
        override suspend fun save(storageKey: String, fileContent: ByteReadChannel): String {
            savedFiles[storageKey] = fileContent.toInputStream().use { it.readAllBytes() }
            return storageKey
        }
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
            module(fakeRepository, fakeStorage)
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
            module(fakeRepository, fakeStorage)
        }

        client.get("/maps/slug/test-map").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertNotNull(body<String>().takeIf { it.contains("test-map") })
        }
    }

    @Test
    fun createsMapAndStoresPmtilesByUuidKey() = testApplication {
        storedMaps.clear()
        storedMaps.add(testMap)
        savedFiles.clear()

        environment {
            config = MapApplicationConfig(
                "db.enabled" to "false",
                "db.jdbcUrl" to "jdbc:postgresql://localhost:5432/test",
                "db.user" to "test",
                "db.password" to "test",
                "db.driverClassName" to "org.postgresql.Driver",
                "db.flyway.enabled" to "false",
                "storage.baseDir" to "build/test-storage",
            )
        }

        application {
            module(fakeRepository, fakeStorage)
        }

        client.submitFormWithBinaryData(
            url = "/maps",
            formData = formData {
                append("slug", "new-map")
                append("title", "New map")
                append("description", "Uploaded map")
                append(
                    key = "pmtiles",
                    value = "pmtiles-content".toByteArray(),
                    headers = Headers.build {
                        append("Content-Disposition", "filename=\"tiles.pmtiles\"")
                        append("Content-Type", ContentType.Application.OctetStream.toString())
                    }
                )
            }
        ).apply {
            assertEquals(HttpStatusCode.Created, status)
            val responseBody = body<String>()
            assertNotNull(responseBody.takeIf { it.contains("\"slug\":\"new-map\"") })
            assertNotNull(responseBody.takeIf { it.contains("\"storageKey\":\"maps/") })
            assertEquals(2, storedMaps.size)
            assertEquals(1, savedFiles.size)
            val storedKey = savedFiles.keys.single()
            assertNotNull(Regex("""^maps/[0-9a-f\-]{36}/tiles\.pmtiles$""").matchEntire(storedKey))
            assertEquals("pmtiles-content", savedFiles[storedKey]?.decodeToString())
        }
    }
}
