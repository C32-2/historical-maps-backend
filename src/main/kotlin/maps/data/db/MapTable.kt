package com.vb.maps.data.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object MapTable : Table("map_catalog") {
    val id = uuid("id")
    val slug = varchar("slug", 60).uniqueIndex()
    val description = varchar("description", 250).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val title = varchar("title", 100).uniqueIndex()
    val storageKey = varchar("storage_key", 60)

    override val primaryKey = PrimaryKey(id)
}
