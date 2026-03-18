package com.vb.maps.domain

import java.util.UUID

interface MapRepository {
    fun getById(id: UUID): Map?
    fun getBySlug(slug: String): Map?
    fun findByTitle(query: String): List<Map>
    fun getAll(): List<Map>
    fun addMap(map: Map)
    fun deleteById(id: UUID)
}
