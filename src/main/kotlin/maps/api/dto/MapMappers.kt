package com.vb.maps.api.dto

import com.vb.maps.domain.Map

fun Map.toResponseDto(): MapResponseDto {
    return MapResponseDto(
        id = id.toString(),
        slug = slug,
        description = description,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        title = title,
        storageKey = storageKey,
    )
}
