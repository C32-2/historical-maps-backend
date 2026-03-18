package com.vb.maps.api

import com.vb.maps.api.dto.CreateMapRequest
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentDisposition
import io.ktor.http.content.PartData
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.RoutingCall
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.IOException

data class ParsedCreateMapRequest(
    val request: CreateMapRequest,
    val fileContent: ByteReadChannel,
)

sealed interface CreateMapMultipartParseResult {
    data class Success(val value: ParsedCreateMapRequest) : CreateMapMultipartParseResult
    data class Failure(val status: HttpStatusCode, val message: String) : CreateMapMultipartParseResult
}

class CreateMapMultipartParser {
    suspend fun parse(call: RoutingCall): CreateMapMultipartParseResult {
        return try {
            val formData = readFormData(call)
            formData.toParseResult()
        } catch (exception: IOException) {
            if (exception.message?.contains("Limit of", ignoreCase = true) == true) {
                failure(HttpStatusCode.PayloadTooLarge, "Uploaded file is too large. Maximum allowed size is 500 MB.")
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
                is PartData.FormItem -> {
                    when (part.name) {
                        "slug" -> formData.slug = part.value.trim()
                        "title" -> formData.title = part.value.trim()
                        "description" -> formData.description = part.value.trim().ifBlank { null }
                    }
                }

                is PartData.FileItem -> {
                    if (part.name == "pmtiles" || part.name == "file") {
                        formData.fileBytes = if (part.isPmtilesFile()) {
                            part.provider().readAllBytes()
                        } else {
                            ByteArray(0)
                        }
                    }
                }

                else -> Unit
            }

            part.dispose.invoke()
            part = multipart.readPart()
        }
        return formData
    }

    private fun CreateMapFormData.toParseResult(): CreateMapMultipartParseResult {
        val requestSlug = slug?.takeUnless(String::isBlank)
            ?: return failure(HttpStatusCode.BadRequest, "Missing slug or title")
        val requestTitle = title?.takeUnless(String::isBlank)
            ?: return failure(HttpStatusCode.BadRequest, "Missing slug or title")
        val uploadedFileBytes = fileBytes
            ?: return failure(HttpStatusCode.BadRequest, "Missing pmtiles file")

        if (uploadedFileBytes.isEmpty()) {
            return failure(HttpStatusCode.BadRequest, "Uploaded file must be a .pmtiles file")
        }

        return CreateMapMultipartParseResult.Success(
            ParsedCreateMapRequest(
                request = CreateMapRequest(
                    slug = requestSlug,
                    title = requestTitle,
                    description = description,
                ),
                fileContent = ByteReadChannel(uploadedFileBytes),
            )
        )
    }
}

private const val MAX_MULTIPART_SIZE_BYTES = 500L * 1024 * 1024

private class CreateMapFormData {
    var slug: String? = null
    var title: String? = null
    var description: String? = null
    var fileBytes: ByteArray? = null
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

private fun ByteReadChannel.readAllBytes(): ByteArray = toInputStream().use { it.readAllBytes() }
