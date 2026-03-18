package com.vb.maps.domain

import java.time.Instant
import java.util.UUID

data class Map(
    val id: UUID,
    val slug: String,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val title: String,
    val storageKey: String,
)
