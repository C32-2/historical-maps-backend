package com.vb.maps.api

import com.vb.module
import com.vb.maps.application.MapStorage
import com.vb.maps.domain.Map
import com.vb.maps.domain.MapRepository
import com.vb.plugins.resetUploadRateLimitBucketsForTests
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.time.Instant
import java.util.UUID

class MapRoutesTest {

    @BeforeTest
    fun resetState() {
        resetUploadRateLimitBucketsForTests()
        storedMaps.clear()
        storedMaps.add(testMap)
        savedFiles.clear()
    }

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

        override fun delete(storageKey: String) = Unit
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
                    value = validPmtilesBytes(),
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
            assertEquals(2, storedMaps.size)
            assertEquals(1, savedFiles.size)
            val storedKey = savedFiles.keys.single()
            assertNotNull(Regex("""^maps/[0-9a-f\-]{36}/tiles\.pmtiles$""").matchEntire(storedKey))
            assertEquals(validPmtilesBytes().toList(), savedFiles[storedKey]?.toList())
        }
    }

    @Test
    fun rejectsFileWithPmtilesExtensionButInvalidContent() = testApplication {
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
                append(
                    key = "pmtiles",
                    value = "not-a-real-pmtiles".toByteArray(),
                    headers = Headers.build {
                        append("Content-Disposition", "filename=\"tiles.pmtiles\"")
                        append("Content-Type", ContentType.Application.OctetStream.toString())
                    }
                )
            }
        ).apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("Uploaded file is not a valid PMTiles archive", body<String>())
            assertEquals(1, storedMaps.size)
            assertEquals(0, savedFiles.size)
        }
    }

    @Test
    fun rejectsRequestWithMultiplePmtilesFiles() = testApplication {
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
                append(
                    key = "pmtiles",
                    value = validPmtilesBytes(),
                    headers = Headers.build {
                        append("Content-Disposition", "filename=\"tiles.pmtiles\"")
                        append("Content-Type", ContentType.Application.OctetStream.toString())
                    }
                )
                append(
                    key = "file",
                    value = validPmtilesBytes(),
                    headers = Headers.build {
                        append("Content-Disposition", "filename=\"tiles.pmtiles\"")
                        append("Content-Type", ContentType.Application.OctetStream.toString())
                    }
                )
            }
        ).apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("Exactly one pmtiles file must be uploaded", body<String>())
            assertEquals(1, storedMaps.size)
            assertEquals(0, savedFiles.size)
        }
    }

    @Test
    fun rateLimitsRepeatedUploadsFromSameClientIp() = testApplication {
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

        repeat(5) { index ->
            client.submitFormWithBinaryData(
                url = "/maps",
                formData = createValidUploadFormData("new-map-$index", "New map $index"),
                block = {
                    headers.append("X-Forwarded-For", "203.0.113.10")
                }
            ).apply {
                assertEquals(HttpStatusCode.Created, status)
            }
        }

        client.submitFormWithBinaryData(
            url = "/maps",
            formData = createValidUploadFormData("new-map-over-limit", "New map over limit"),
            block = {
                headers.append("X-Forwarded-For", "203.0.113.10")
            }
        ).apply {
            assertEquals(HttpStatusCode.TooManyRequests, status)
            assertEquals("Too many upload attempts. Try again later.", body<String>())
        }
    }
}

private fun createValidUploadFormData(slug: String, title: String) = formData {
    append("slug", slug)
    append("title", title)
    append("description", "Uploaded map")
    append(
        key = "pmtiles",
        value = validPmtilesBytes(),
        headers = Headers.build {
            append("Content-Disposition", "filename=\"tiles.pmtiles\"")
            append("Content-Type", ContentType.Application.OctetStream.toString())
        }
    )
}

private fun validPmtilesBytes(): ByteArray {
    val bytes = ByteArray(127)
    val magic = "PMTiles".encodeToByteArray()
    magic.copyInto(bytes, destinationOffset = 0)
    bytes[7] = 3
    ByteBuffer.wrap(bytes, 8, Long.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(127L)
    return bytes
}
