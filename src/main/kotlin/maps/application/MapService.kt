package com.vb.maps.application

import com.vb.maps.data.MapRepository
import com.vb.maps.domain.Map
import java.util.UUID

class MapService(
    private val repository: MapRepository
) {
    fun getMapById(id: UUID): Map? = repository.getById(id)
    fun getMapBySlug(slug: String): Map? = repository.getBySlug(slug)
    fun findByTitle(query: String): List<Map> = repository.findByTitle(query)
}
