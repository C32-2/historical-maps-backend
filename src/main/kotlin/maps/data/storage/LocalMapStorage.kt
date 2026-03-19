package com.vb.maps.data.storage

import com.vb.maps.application.MapStorage
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import java.io.File
import java.nio.file.Files

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

        val targetFile = File(baseDir, storagePath.toString())
        if (!targetFile.exists()) {
            return
        }

        if (!targetFile.delete()) {
            throw kotlinx.io.IOException("Failed to delete file: ${targetFile.absolutePath}")
        }

        deleteEmptyParentDirectories(targetFile.parentFile)
    }

    private fun deleteEmptyParentDirectories(directory: File?) {
        var current = directory ?: return
        val normalizedBaseDir = baseDir.toPath().toAbsolutePath().normalize()

        while (current.exists()) {
            val currentPath = current.toPath().toAbsolutePath().normalize()
            if (currentPath == normalizedBaseDir || !currentPath.startsWith(normalizedBaseDir)) {
                return
            }

            val isEmpty = Files.list(currentPath).use { it.findAny().isEmpty }
            if (!isEmpty) {
                return
            }

            if (!current.delete()) {
                throw kotlinx.io.IOException("Failed to delete directory: ${current.absolutePath}")
            }

            current = current.parentFile ?: return
        }
    }
}
