package com.vb.maps.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object MapTable : Table("map_catalog") {
    val id = uuid("id")
    val slug = text("slug").uniqueIndex()
    val description = text("description").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val title = text("title")

    override val primaryKey = PrimaryKey(id)
}
