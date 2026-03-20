package com.vb.maps.api.upload

import com.vb.maps.application.CreateMapCommand
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
    data class Failure(val error: CreateMapMultipartError) : CreateMapMultipartParseResult
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
                failure(CreateMapMultipartError.PayloadTooLarge)
            } else {
                throw exception
            }
        }
    }
}

private object CreateMapMultipartPayloadValidator {
    fun validate(payload: CreateMapMultipartPayload): CreateMapMultipartParseResult {
        val requestSlug = payload.slug?.takeUnless(String::isBlank)
            ?: return failure(CreateMapMultipartError.MissingSlugOrTitle)
        val requestTitle = payload.title?.takeUnless(String::isBlank)
            ?: return failure(CreateMapMultipartError.MissingSlugOrTitle)

        if (payload.hasDuplicateFields) {
            return failure(CreateMapMultipartError.DuplicateFields)
        }

        if (payload.filePartsCount != 1) {
            return failure(CreateMapMultipartError.InvalidFileCount)
        }

        if (!requestSlug.matches(SLUG_PATTERN)) {
            return failure(CreateMapMultipartError.InvalidSlug)
        }

        if (requestSlug.length > MAX_SLUG_LENGTH || requestTitle.length > MAX_TITLE_LENGTH || (payload.description?.length ?: 0) > MAX_DESCRIPTION_LENGTH) {
            return failure(CreateMapMultipartError.FieldTooLong)
        }

        if (payload.hasInvalidFile) {
            return failure(CreateMapMultipartError.InvalidFileExtension)
        }

        val uploadedFile = payload.uploadedFile
            ?: return failure(CreateMapMultipartError.MissingFile)

        if (!PmtilesArchiveValidator.isValid(uploadedFile)) {
            payload.cleanup()
            return failure(CreateMapMultipartError.InvalidPmtilesArchive)
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

private fun failure(error: CreateMapMultipartError) =
    CreateMapMultipartParseResult.Failure(error = error)
