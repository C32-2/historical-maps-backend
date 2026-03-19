package com.vb.maps.application

import com.vb.maps.api.dto.CreateMapRequest
import com.vb.maps.domain.Map
import com.vb.maps.domain.MapRepository
import io.ktor.utils.io.ByteReadChannel
import java.time.Instant
import java.util.UUID

class MapService(
    private val repository: MapRepository,
    private val storage: MapStorage,
) {
    fun getMapById(id: UUID): Map? = repository.getById(id)
    fun getMapBySlug(slug: String): Map? = repository.getBySlug(slug)
    fun findByTitle(query: String): List<Map> = repository.findByTitle(query)

    suspend fun saveMap(mapDto: CreateMapRequest, fileContent: ByteReadChannel): Map {
        val draftMap = createMap(mapDto)
        var persistedStorageKey: String? = null

        return try {
            persistedStorageKey = storage.save(draftMap.storageKey, fileContent)
            val persistedMap = draftMap.copy(storageKey = persistedStorageKey)
            repository.addMap(persistedMap)
            persistedMap
        } catch (exception: Exception) {
            storage.delete(persistedStorageKey ?: draftMap.storageKey)
            throw exception
        }
    }

    private fun createMap(mapDto: CreateMapRequest): Map {
        val id = UUID.randomUUID()
        val timestamp = Instant.now()
        return Map(
            id = id,
            slug = mapDto.slug,
            description = mapDto.description,
            createdAt = timestamp,
            updatedAt = timestamp,
            title = mapDto.title,
            storageKey = "maps/$id/tiles.pmtiles"
        )
    }

    fun removeMap(id: UUID): Boolean {
        val map = repository.getById(id) ?: return false

        repository.deleteById(id)

        try {
            storage.delete(map.storageKey)
        } catch (exception: Exception) {
            runCatching { repository.addMap(map) }
                .getOrElse { rollbackException ->
                    exception.addSuppressed(rollbackException)
                }
            throw exception
        }

        return true
    }
}
