package com.vb.maps.application

data class CreateMapCommand(
    val slug: String,
    val title: String,
    val description: String?,
)
