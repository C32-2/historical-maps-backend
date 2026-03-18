package com.vb.maps.data.storage

import com.vb.maps.application.MapStorage
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

class LocalMapStorage(
    private val baseDir: File
) : MapStorage {
    override suspend fun save(storageKey: String, fileContent: ByteReadChannel): String {
        require(storageKey.isNotBlank()) { "storageKey is empty" }
        require(!storageKey.startsWith("/") && !storageKey.startsWith("\\")) { "storageKey must be relative" }

        val normalizedStorageKey = storageKey.replace('\\', '/')
        val storagePath = java.nio.file.Paths.get(normalizedStorageKey).normalize()
        require(!storagePath.startsWith("..")) { "storageKey must stay within baseDir" }

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw kotlinx.io.IOException("Failed to create base directory: ${baseDir.absolutePath}")
        }

        val targetFile = File(baseDir, storagePath.toString())
        val parent = targetFile.parentFile

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw kotlinx.io.IOException("Failed to create directory: ${parent.absolutePath}")
        }

        fileContent.copyAndClose(targetFile.writeChannel())

        return normalizedStorageKey
    }

    override fun delete(storageKey: String) {
        val normalizedStorageKey = storageKey.replace('\\', '/')
        val storagePath = java.nio.file.Paths.get(normalizedStorageKey).normalize()
        require(!storagePath.startsWith("..")) { "storageKey must stay within baseDir" }

        File(baseDir, storagePath.toString()).delete()
    }
}
