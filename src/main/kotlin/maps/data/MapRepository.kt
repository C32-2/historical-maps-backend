package com.vb.maps.data

import com.vb.maps.domain.Map
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class MapRepository {
    fun getById(id: UUID): Map? = transaction {
        MapTable
            .selectAll()
            .andWhere { MapTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.let(::toDomain)
    }

    fun getBySlug(slug: String): Map? = transaction {
        MapTable
            .selectAll()
            .where { MapTable.slug eq slug }
            .limit(1)
            .firstOrNull()
            ?.let(::toDomain)
    }

    fun findByTitle(query: String): List<Map> = transaction {
        MapTable
            .selectAll()
            .where(MapTable.title like "%$query%")
            .map { toDomain(it) }
    }

    private fun toDomain(row: ResultRow): Map = Map(
        id = row[MapTable.id],
        slug = row[MapTable.slug],
        description = row[MapTable.description] ?: "",
        createdAt = row[MapTable.createdAt],
        updatedAt = row[MapTable.updatedAt],
        title = row[MapTable.title],
    )
}
