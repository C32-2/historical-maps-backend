package com.vb.maps.api.dto

data class CreateMapRequest(
    val slug: String,
    val title: String,
    val description: String?,
)