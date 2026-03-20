package com.vb.maps.api.upload

import com.vb.maps.application.CreateMapCommand
import io.ktor.http.HttpStatusCode
import io.ktor.util.cio.readChannel
import io.ktor.server.routing.RoutingCall
import io.ktor.utils.io.ByteReadChannel
import java.io.IOException

data class ParsedCreateMapRequest(
    val command: CreateMapCommand,
    private val uploadedFile: TemporaryUpload,
) {
    fun openFileContent(): ByteReadChannel = uploadedFile.openContent()
    fun cleanup() = uploadedFile.delete()
}

sealed interface CreateMapMultipartParseResult {
    data class Success(val value: ParsedCreateMapRequest) : CreateMapMultipartParseResult
    data class Failure(val status: HttpStatusCode, val message: String) : CreateMapMultipartParseResult
}

class CreateMapMultipartParser {
    private val reader = CreateMapMultipartReader()

    suspend fun parse(call: RoutingCall): CreateMapMultipartParseResult {
        var payload: CreateMapMultipartPayload? = null

        return try {
            payload = reader.read(call)
            CreateMapMultipartPayloadValidator.validate(payload).also { result ->
                if (result is CreateMapMultipartParseResult.Failure) {
                    payload.cleanup()
                }
            }
        } catch (exception: IOException) {
            payload?.cleanup()
            if (exception.message?.contains("Limit of", ignoreCase = true) == true) {
                failure(HttpStatusCode.PayloadTooLarge, "Uploaded file is too large. Maximum allowed size is 100 MB.")
            } else {
                throw exception
            }
        }
    }
}

private object CreateMapMultipartPayloadValidator {
    fun validate(payload: CreateMapMultipartPayload): CreateMapMultipartParseResult {
        val requestSlug = payload.slug?.takeUnless(String::isBlank)
            ?: return failure(HttpStatusCode.BadRequest, "Missing slug or title")
        val requestTitle = payload.title?.takeUnless(String::isBlank)
            ?: return failure(HttpStatusCode.BadRequest, "Missing slug or title")

        if (payload.hasDuplicateFields) {
            return failure(HttpStatusCode.BadRequest, "Duplicate multipart fields are not allowed")
        }

        if (payload.filePartsCount != 1) {
            return failure(HttpStatusCode.BadRequest, "Exactly one pmtiles file must be uploaded")
        }

        if (!requestSlug.matches(SLUG_PATTERN)) {
            return failure(HttpStatusCode.BadRequest, "Slug may contain only lowercase letters, digits, and hyphens")
        }

        if (requestSlug.length > MAX_SLUG_LENGTH || requestTitle.length > MAX_TITLE_LENGTH || (payload.description?.length ?: 0) > MAX_DESCRIPTION_LENGTH) {
            return failure(HttpStatusCode.BadRequest, "One or more fields exceed the allowed length")
        }

        if (payload.hasInvalidFile) {
            return failure(HttpStatusCode.BadRequest, "Uploaded file must be a .pmtiles file")
        }

        val uploadedFile = payload.uploadedFile
            ?: return failure(HttpStatusCode.BadRequest, "Missing pmtiles file")

        if (!PmtilesArchiveValidator.isValid(uploadedFile)) {
            payload.cleanup()
            return failure(HttpStatusCode.BadRequest, "Uploaded file is not a valid PMTiles archive")
        }

        return CreateMapMultipartParseResult.Success(
            ParsedCreateMapRequest(
                command = CreateMapCommand(
                    slug = requestSlug,
                    title = requestTitle,
                    description = payload.description,
                ),
                uploadedFile = uploadedFile,
            )
        )
    }
}

private fun failure(status: HttpStatusCode, message: String) =
    CreateMapMultipartParseResult.Failure(status = status, message = message)
