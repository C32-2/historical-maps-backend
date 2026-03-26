package com.vb.infrastructure.config

import io.ktor.server.config.ApplicationConfig
import java.io.File

data class StorageSettings(
    val baseDir: File,
)

fun ApplicationConfig.toStorageSettings(): StorageSettings {
    return StorageSettings(
        baseDir = File(
            stringSetting(
                path = "storage.baseDir",
                envName = "BASE_MAP_DIRECTORY",
            )
        ),
    )
}
