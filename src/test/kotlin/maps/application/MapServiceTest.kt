package com.vb.maps.application

import com.vb.maps.domain.Map
import com.vb.maps.domain.MapRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
    )

    private val secondMap = Map(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        slug = "another-map",
        title = "Another atlas",
        description = "Description",
        createdAt = Instant.parse("2026-01-03T10:00:00Z"),
        updatedAt = Instant.parse("2026-01-04T10:00:00Z"),
    )

    private val repository = object : MapRepository {
        private val maps = listOf(firstMap, secondMap)

        override fun getById(id: UUID): Map? = maps.firstOrNull { it.id == id }

        override fun getBySlug(slug: String): Map? = maps.firstOrNull { it.slug == slug }

        override fun findByTitle(query: String): List<Map> =
            maps.filter { it.title.contains(query, ignoreCase = true) }

        override fun getAll(): List<Map> = maps
    }

    private val service = MapService(repository)

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
}
