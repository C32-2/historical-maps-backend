package com.vb.maps.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class MapResponseDto(
    val id: String,
    val slug: String,
    val description: String,
    val pmtilesPath: String,
    val previewPath: String,
    val createdAt: String,
    val updatedAt: String,
)