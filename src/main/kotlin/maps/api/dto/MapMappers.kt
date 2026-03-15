package com.vb.maps.api.dto

import com.vb.maps.domain.Map

fun Map.toResponseDto(): MapResponseDto {
    return MapResponseDto(
        id = id.toString(),
        slug = slug,
        description = description,
        pmtilesPath = pmtilesPath,
        previewPath = previewPath,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        title = title,
    )
}