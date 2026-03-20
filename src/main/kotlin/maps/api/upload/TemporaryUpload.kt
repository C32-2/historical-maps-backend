package com.vb.maps.api.upload

import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel
import java.nio.file.Files
import java.nio.file.Path

class TemporaryUpload private constructor(
    internal val path: Path,
) {
    fun openContent(): ByteReadChannel = path.toFile().readChannel()

    fun delete() {
        Files.deleteIfExists(path)
    }

    companion object {
        fun create(): TemporaryUpload = TemporaryUpload(
            path = Files.createTempFile("historical-maps-upload-", ".pmtiles")
        )
    }
}
