package com.vb.infrastructure.config

import io.ktor.server.config.ApplicationConfig
import java.io.File

data class StorageSettings(
    val baseDir: File,
)

fun ApplicationConfig.toStorageSettings(): StorageSettings {
    val storageConfig = config("storage")

    return StorageSettings(
        baseDir = File(
            storageConfig.stringSetting(
                path = "baseDir",
                envName = "BASE_MAP_DIRECTORY",
                default = "/app/data",
            )
        ),
    )
}
