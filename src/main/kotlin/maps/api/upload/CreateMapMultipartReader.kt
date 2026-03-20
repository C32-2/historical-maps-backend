package com.vb.maps.api.upload

import io.ktor.http.ContentDisposition
import io.ktor.http.content.PartData
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.RoutingCall
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose

internal class CreateMapMultipartReader {
    suspend fun read(call: RoutingCall): CreateMapMultipartPayload {
        val accumulator = CreateMapMultipartAccumulator()
        val multipart = call.receiveMultipart(formFieldLimit = MAX_MULTIPART_SIZE_BYTES)
        var part = multipart.readPart()

        while (part != null) {
            when (part) {
                is PartData.FormItem -> accumulator.consumeField(part.name, part.value)
                is PartData.FileItem -> accumulator.consumeFile(part)
                else -> Unit
            }

            part.dispose.invoke()
            part = multipart.readPart()
        }

        return accumulator.toPayload()
    }
}

private class CreateMapMultipartAccumulator {
    private val seenFields = mutableSetOf<String>()
    private var slug: String? = null
    private var title: String? = null
    private var description: String? = null
    private var uploadedFile: TemporaryUpload? = null
    private var hasInvalidFile: Boolean = false
    private var hasDuplicateFields: Boolean = false
    private var filePartsCount: Int = 0

    fun consumeField(name: String?, rawValue: String) {
        when (name) {
            SLUG_FIELD -> slug = consumeUniqueField(SLUG_FIELD, rawValue.trim())
            TITLE_FIELD -> title = consumeUniqueField(TITLE_FIELD, rawValue.trim())
            DESCRIPTION_FIELD -> {
                description = consumeUniqueField(DESCRIPTION_FIELD, rawValue.trim())
                    ?.ifBlank { null }
            }
        }
    }

    suspend fun consumeFile(part: PartData.FileItem) {
        if (part.name !in FILE_FIELDS) {
            return
        }

        filePartsCount += 1
        if (!part.hasPmtilesExtension()) {
            hasInvalidFile = true
            return
        }

        uploadedFile?.delete()
        val tempUpload = TemporaryUpload.create()
        part.provider().copyAndClose(tempUpload.path.toFile().writeChannel())
        uploadedFile = tempUpload
    }

    fun toPayload(): CreateMapMultipartPayload = CreateMapMultipartPayload(
        slug = slug,
        title = title,
        description = description,
        uploadedFile = uploadedFile,
        hasInvalidFile = hasInvalidFile,
        hasDuplicateFields = hasDuplicateFields,
        filePartsCount = filePartsCount,
    )

    private fun consumeUniqueField(fieldName: String, value: String): String? {
        if (!seenFields.add(fieldName)) {
            hasDuplicateFields = true
            return when (fieldName) {
                SLUG_FIELD -> slug
                TITLE_FIELD -> title
                DESCRIPTION_FIELD -> description
                else -> null
            }
        }

        return value
    }
}

private fun PartData.FileItem.hasPmtilesExtension(): Boolean {
    val originalName = originalFileName.orEmpty()
    return originalName.endsWith(".pmtiles", ignoreCase = true) ||
        ContentDisposition.parse(headers["Content-Disposition"].orEmpty())
            .parameter("filename")
            ?.endsWith(".pmtiles", ignoreCase = true) == true
}
