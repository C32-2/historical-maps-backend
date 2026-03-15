package com.vb.maps.data

import org.jetbrains.exposed.sql.Table

object MapTable : Table("map") {
    val id = uuid("id")
    val slug = text("slug").uniqueIndex()
    val description = text("description").nullable()
    val pmtilesPath = text("pmtiles_path")
    val previewPath = text("preview_path")
    val createdAt = text("created_at")
    val updatedAt = text("updated_at")

    override val primaryKey = PrimaryKey(id)
}