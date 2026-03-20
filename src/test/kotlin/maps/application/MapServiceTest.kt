package com.vb.maps.application

import com.vb.maps.domain.Map
import com.vb.maps.domain.MapRepository
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import java.time.Instant
import java.util.UUID

class MapServiceTest {

    private val firstMap = Map(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        slug = "test-map",
        title = "Test map",
        description = "Description",
        createdAt = Instant.parse("2026-01-01T10:00:00Z"),
        updatedAt = Instant.parse("2026-01-02T10:00:00Z"),
        storageKey = "maps/11111111-1111-1111-1111-111111111111/tiles.pmtiles",
    )

    private val secondMap = Map(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        slug = "another-map",
        title = "Another atlas",
        description = "Description",
        createdAt = Instant.parse("2026-01-03T10:00:00Z"),
        updatedAt = Instant.parse("2026-01-04T10:00:00Z"),
        storageKey = "maps/22222222-2222-2222-2222-222222222222/tiles.pmtiles",
    )

    private val repository = object : MapRepository {
        private val maps = listOf(firstMap, secondMap)

        override fun getById(id: UUID): Map? = maps.firstOrNull { it.id == id }

        override fun getBySlug(slug: String): Map? = maps.firstOrNull { it.slug == slug }

        override fun findByTitle(query: String): List<Map> =
            maps.filter { it.title.contains(query, ignoreCase = true) }

        override fun getAll(): List<Map> = maps

        override fun addMap(map: Map) = Unit

        override fun deleteById(id: UUID) = Unit
    }

    private val storage = object : MapStorage {
        override suspend fun save(storageKey: String, fileContent: ByteReadChannel): String = storageKey
        override fun delete(storageKey: String) = Unit
    }

    private val service = MapService(repository, storage)

    @Test
    fun returnsMapById() {
        val result = service.getMapById(firstMap.id)

        assertEquals(firstMap, result)
    }

    @Test
    fun returnsNullWhenMapBySlugDoesNotExist() {
        val result = service.getMapBySlug("missing-map")

        assertNull(result)
    }

    @Test
    fun filtersMapsByTitle() {
        val result = service.findByTitle("atlas")

        assertEquals(listOf(secondMap), result)
    }

    @Test
    fun deletesStoredFileWhenRepositorySaveFails() {
        val deletedStorageKeys = mutableListOf<String>()

        val repository = object : MapRepository {
            override fun getById(id: UUID): Map? = null
            override fun getBySlug(slug: String): Map? = null
            override fun findByTitle(query: String): List<Map> = emptyList()
            override fun getAll(): List<Map> = emptyList()
            override fun addMap(map: Map) {
                throw IllegalStateException("database failed")
            }

            override fun deleteById(id: UUID) = Unit
        }

        val storage = object : MapStorage {
            override suspend fun save(storageKey: String, fileContent: ByteReadChannel): String = storageKey
            override fun delete(storageKey: String) { deletedStorageKeys.add(storageKey) }
        }

        val service = MapService(repository, storage)

        assertFailsWith<IllegalStateException> {
            kotlinx.coroutines.runBlocking {
                service.saveMap(
                    command = CreateMapCommand(
                        slug = "new-map",
                        title = "New map",
                        description = "Description",
                    ),
                    fileContent = ByteReadChannel("content"),
                )
            }
        }

        assertEquals(1, deletedStorageKeys.size)
    }

    @Test
    fun removesMapAndStoredFile() {
        val deletedIds = mutableListOf<UUID>()
        val deletedStorageKeys = mutableListOf<String>()

        val repository = object : MapRepository {
            override fun getById(id: UUID): Map? = firstMap.takeIf { it.id == id }
            override fun getBySlug(slug: String): Map? = null
            override fun findByTitle(query: String): List<Map> = emptyList()
            override fun getAll(): List<Map> = emptyList()
            override fun addMap(map: Map) = Unit
            override fun deleteById(id: UUID) {
                deletedIds.add(id)
            }
        }

        val storage = object : MapStorage {
            override suspend fun save(storageKey: String, fileContent: ByteReadChannel): String = storageKey
            override fun delete(storageKey: String) {
                deletedStorageKeys.add(storageKey)
            }
        }

        val service = MapService(repository, storage)

        val removed = service.removeMap(firstMap.id)

        assertTrue(removed)
        assertEquals(listOf(firstMap.id), deletedIds)
        assertEquals(listOf(firstMap.storageKey), deletedStorageKeys)
    }

    @Test
    fun returnsFalseWhenDeletingMissingMap() {
        val service = MapService(repository, storage)

        val removed = service.removeMap(UUID.fromString("33333333-3333-3333-3333-333333333333"))

        assertFalse(removed)
    }

    @Test
    fun restoresRepositoryRecordWhenStorageDeletionFails() {
        val deletedIds = mutableListOf<UUID>()
        val restoredMaps = mutableListOf<Map>()

        val repository = object : MapRepository {
            override fun getById(id: UUID): Map? = firstMap.takeIf { it.id == id }
            override fun getBySlug(slug: String): Map? = null
            override fun findByTitle(query: String): List<Map> = emptyList()
            override fun getAll(): List<Map> = emptyList()
            override fun addMap(map: Map) {
                restoredMaps.add(map)
            }
            override fun deleteById(id: UUID) {
                deletedIds.add(id)
            }
        }

        val storage = object : MapStorage {
            override suspend fun save(storageKey: String, fileContent: ByteReadChannel): String = storageKey
            override fun delete(storageKey: String) {
                throw IllegalStateException("storage failed")
            }
        }

        val service = MapService(repository, storage)

        assertFailsWith<IllegalStateException> {
            service.removeMap(firstMap.id)
        }

        assertEquals(listOf(firstMap.id), deletedIds)
        assertEquals(listOf(firstMap), restoredMaps)
    }
}
