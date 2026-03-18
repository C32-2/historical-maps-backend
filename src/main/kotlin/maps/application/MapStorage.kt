package com.vb.maps.application

import io.ktor.utils.io.ByteReadChannel

interface MapStorage {
    suspend fun save(storageKey: String, fileContent: ByteReadChannel): String
    fun delete(storageKey: String)
}
