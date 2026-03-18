package com.vb.maps.application

import com.vb.maps.api.dto.CreateMapRequest
import com.vb.maps.domain.Map
import com.vb.maps.domain.MapRepository
import io.ktor.utils.io.ByteReadChannel
import java.util.UUID
import java.time.Instant

class MapService(
    private val repository: MapRepository,
    private val storage: MapStorage,
) {
    fun getMapById(id: UUID): Map? = repository.getById(id)
    fun getMapBySlug(slug: String): Map? = repository.getBySlug(slug)
    fun findByTitle(query: String): List<Map> = repository.findByTitle(query)

    suspend fun saveMap(mapDto: CreateMapRequest, fileContent: ByteReadChannel): Map {
        val map = createMap(mapDto)

        repository.addMap(map)

        return try {
            val persistedStorageKey = storage.save(map.storageKey, fileContent)
            map.copy(storageKey = persistedStorageKey)
        } catch (exception: Exception) {
            repository.deleteById(map.id)
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
}
