package com.vb.maps.api

import com.vb.maps.api.dto.CreateMapRequest
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentDisposition
import io.ktor.http.content.PartData
import io.ktor.util.cio.readChannel
import io.ktor.util.cio.writeChannel
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.RoutingCall
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path

data class ParsedCreateMapRequest(
    val request: CreateMapRequest,
    private val uploadedFile: Path,
) {
    fun openFileContent(): ByteReadChannel = uploadedFile.toFile().readChannel()
    fun cleanup() {
        Files.deleteIfExists(uploadedFile)
    }
}

sealed interface CreateMapMultipartParseResult {
    data class Success(val value: ParsedCreateMapRequest) : CreateMapMultipartParseResult
    data class Failure(val status: HttpStatusCode, val message: String) : CreateMapMultipartParseResult
}

class CreateMapMultipartParser {
    suspend fun parse(call: RoutingCall): CreateMapMultipartParseResult {
        var formData: CreateMapFormData? = null

        return try {
            formData = readFormData(call)
            CreateMapFormValidator.validate(formData).also { result ->
                if (result is CreateMapMultipartParseResult.Failure) {
                    formData.cleanupUpload()
                }
            }
        } catch (exception: IOException) {
            formData?.cleanupUpload()
            if (exception.message?.contains("Limit of", ignoreCase = true) == true) {
                failure(HttpStatusCode.PayloadTooLarge, "Uploaded file is too large. Maximum allowed size is 100 MB.")
            } else {
                throw exception
            }
        }
    }

    private suspend fun readFormData(call: RoutingCall): CreateMapFormData {
        val formData = CreateMapFormData()
        val multipart = call.receiveMultipart(formFieldLimit = MAX_MULTIPART_SIZE_BYTES)
        var part = multipart.readPart()

        while (part != null) {
            when (part) {
                is PartData.FormItem -> formData.consume(part)
                is PartData.FileItem -> formData.consume(part)

                else -> Unit
            }

            part.dispose.invoke()
            part = multipart.readPart()
        }
        return formData
    }

}

private const val MAX_MULTIPART_SIZE_BYTES = 100L * 1024 * 1024
private const val PMTILES_HEADER_SIZE_BYTES = 127
private const val PMTILES_VERSION = 3
private const val PMTILES_ROOT_DIRECTORY_WINDOW_BYTES = 16_384L
private const val MAX_SLUG_LENGTH = 60
private const val MAX_TITLE_LENGTH = 100
private const val MAX_DESCRIPTION_LENGTH = 250
private val PMTILES_MAGIC = "PMTiles".encodeToByteArray()
private val SLUG_PATTERN = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
private const val SLUG_FIELD = "slug"
private const val TITLE_FIELD = "title"
private const val DESCRIPTION_FIELD = "description"
private val FILE_FIELDS = setOf("pmtiles", "file")

private class CreateMapFormData {
    var slug: String? = null
    var title: String? = null
    var description: String? = null
    var uploadedFile: Path? = null
    var hasInvalidFile: Boolean = false
    var hasDuplicateFields: Boolean = false
    var filePartsCount: Int = 0
    var hasSeenSlug: Boolean = false
    var hasSeenTitle: Boolean = false
    var hasSeenDescription: Boolean = false

    suspend fun consume(part: PartData.FormItem) {
        when (part.name) {
            SLUG_FIELD -> {
                if (hasSeenSlug) {
                    hasDuplicateFields = true
                } else {
                    hasSeenSlug = true
                    slug = part.value.trim()
                }
            }

            TITLE_FIELD -> {
                if (hasSeenTitle) {
                    hasDuplicateFields = true
                } else {
                    hasSeenTitle = true
                    title = part.value.trim()
                }
            }

            DESCRIPTION_FIELD -> {
                if (hasSeenDescription) {
                    hasDuplicateFields = true
                } else {
                    hasSeenDescription = true
                    description = part.value.trim().ifBlank { null }
                }
            }
        }
    }

    suspend fun consume(part: PartData.FileItem) {
        if (part.name !in FILE_FIELDS) {
            return
        }

        filePartsCount += 1
        if (!part.isPmtilesFile()) {
            hasInvalidFile = true
            return
        }

        cleanupUpload()
        uploadedFile = createTempUploadFile()
        part.provider().copyAndClose(uploadedFile!!.toFile().writeChannel())
    }

    fun cleanupUpload() {
        uploadedFile?.let(Files::deleteIfExists)
        uploadedFile = null
    }
}

private object CreateMapFormValidator {
    fun validate(formData: CreateMapFormData): CreateMapMultipartParseResult {
        val requestSlug = formData.slug?.takeUnless(String::isBlank)
            ?: return failure(HttpStatusCode.BadRequest, "Missing slug or title")
        val requestTitle = formData.title?.takeUnless(String::isBlank)
            ?: return failure(HttpStatusCode.BadRequest, "Missing slug or title")

        if (formData.hasDuplicateFields) {
            return failure(HttpStatusCode.BadRequest, "Duplicate multipart fields are not allowed")
        }

        if (formData.filePartsCount != 1) {
            return failure(HttpStatusCode.BadRequest, "Exactly one pmtiles file must be uploaded")
        }

        if (!requestSlug.matches(SLUG_PATTERN)) {
            return failure(HttpStatusCode.BadRequest, "Slug may contain only lowercase letters, digits, and hyphens")
        }

        if (requestSlug.length > MAX_SLUG_LENGTH || requestTitle.length > MAX_TITLE_LENGTH || (formData.description?.length ?: 0) > MAX_DESCRIPTION_LENGTH) {
            return failure(HttpStatusCode.BadRequest, "One or more fields exceed the allowed length")
        }

        if (formData.hasInvalidFile) {
            return failure(HttpStatusCode.BadRequest, "Uploaded file must be a .pmtiles file")
        }

        val uploadedFile = formData.uploadedFile
            ?: return failure(HttpStatusCode.BadRequest, "Missing pmtiles file")

        if (!PmtilesArchiveValidator.isValid(uploadedFile)) {
            formData.cleanupUpload()
            return failure(HttpStatusCode.BadRequest, "Uploaded file is not a valid PMTiles archive")
        }

        return CreateMapMultipartParseResult.Success(
            ParsedCreateMapRequest(
                request = CreateMapRequest(
                    slug = requestSlug,
                    title = requestTitle,
                    description = formData.description,
                ),
                uploadedFile = uploadedFile,
            )
        )
    }
}

private fun PartData.FileItem.isPmtilesFile(): Boolean {
    val originalName = originalFileName.orEmpty()
    return originalName.endsWith(".pmtiles", ignoreCase = true) ||
        ContentDisposition.parse(headers["Content-Disposition"].orEmpty())
            .parameter("filename")
            ?.endsWith(".pmtiles", ignoreCase = true) == true
}

private fun failure(status: HttpStatusCode, message: String) =
    CreateMapMultipartParseResult.Failure(status = status, message = message)

private fun createTempUploadFile(): Path = Files.createTempFile("historical-maps-upload-", ".pmtiles")

private object PmtilesArchiveValidator {
    fun isValid(path: Path): Boolean {
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
