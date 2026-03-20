package com.vb.maps.support

import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun createValidUploadFormData(slug: String, title: String) = formData {
    append("slug", slug)
    append("title", title)
    append("description", "Uploaded map")
    appendPmtilesFile(validPmtilesBytes())
}

fun FormBuilder.appendPmtilesFile(bytes: ByteArray) {
    append(
        key = "pmtiles",
        value = bytes,
        headers = Headers.build {
            append("Content-Disposition", "filename=\"tiles.pmtiles\"")
            append("Content-Type", ContentType.Application.OctetStream.toString())
        }
    )
}

fun validPmtilesBytes(): ByteArray {
    val bytes = ByteArray(127)
    val magic = "PMTiles".encodeToByteArray()
    magic.copyInto(bytes, destinationOffset = 0)
    bytes[7] = 3
    ByteBuffer.wrap(bytes, 8, Long.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(127L)
    return bytes
}
