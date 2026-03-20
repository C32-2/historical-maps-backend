package com.vb.maps.api.upload

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

internal object PmtilesArchiveValidator {
    private const val PMTILES_HEADER_SIZE_BYTES = 127
    private const val PMTILES_VERSION = 3
    private const val PMTILES_ROOT_DIRECTORY_WINDOW_BYTES = 16_384L
    private val PMTILES_MAGIC = "PMTiles".encodeToByteArray()

    fun isValid(upload: TemporaryUpload): Boolean {
        val path = upload.path
        if (!Files.isRegularFile(path)) {
            return false
        }

        if (Files.size(path) < PMTILES_HEADER_SIZE_BYTES) {
            return false
        }

        Files.newInputStream(path).use { input ->
            val header = ByteArray(PMTILES_HEADER_SIZE_BYTES)
            val bytesRead = input.readNBytes(header, 0, header.size)
            if (bytesRead != PMTILES_HEADER_SIZE_BYTES) {
                return false
            }

            if (!header.copyOfRange(0, PMTILES_MAGIC.size).contentEquals(PMTILES_MAGIC)) {
                return false
            }

            if (header[PMTILES_MAGIC.size].toInt() != PMTILES_VERSION) {
                return false
            }

            val rootDirectoryOffset = ByteBuffer.wrap(header, 8, Long.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .long

            return rootDirectoryOffset in PMTILES_HEADER_SIZE_BYTES.toLong()..PMTILES_ROOT_DIRECTORY_WINDOW_BYTES
        }
    }
}
