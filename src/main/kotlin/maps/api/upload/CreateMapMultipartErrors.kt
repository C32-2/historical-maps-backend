package com.vb.maps.api.upload

import io.ktor.http.HttpStatusCode

sealed interface CreateMapMultipartError {
    data object PayloadTooLarge : CreateMapMultipartError
    data object MissingSlugOrTitle : CreateMapMultipartError
    data object DuplicateFields : CreateMapMultipartError
    data object InvalidFileCount : CreateMapMultipartError
    data object InvalidSlug : CreateMapMultipartError
    data object FieldTooLong : CreateMapMultipartError
    data object InvalidFileExtension : CreateMapMultipartError
    data object MissingFile : CreateMapMultipartError
    data object InvalidPmtilesArchive : CreateMapMultipartError
}

val CreateMapMultipartError.status: HttpStatusCode
    get() = when (this) {
        CreateMapMultipartError.PayloadTooLarge -> HttpStatusCode.PayloadTooLarge
        CreateMapMultipartError.MissingSlugOrTitle,
        CreateMapMultipartError.DuplicateFields,
        CreateMapMultipartError.InvalidFileCount,
        CreateMapMultipartError.InvalidSlug,
        CreateMapMultipartError.FieldTooLong,
        CreateMapMultipartError.InvalidFileExtension,
        CreateMapMultipartError.MissingFile,
        CreateMapMultipartError.InvalidPmtilesArchive,
            -> HttpStatusCode.BadRequest
    }

val CreateMapMultipartError.message: String
    get() = when (this) {
        CreateMapMultipartError.PayloadTooLarge -> "Uploaded file is too large. Maximum allowed size is 100 MB."
        CreateMapMultipartError.MissingSlugOrTitle -> "Missing slug or title"
        CreateMapMultipartError.DuplicateFields -> "Duplicate multipart fields are not allowed"
        CreateMapMultipartError.InvalidFileCount -> "Exactly one pmtiles file must be uploaded"
        CreateMapMultipartError.InvalidSlug -> "Slug may contain only lowercase letters, digits, and hyphens"
        CreateMapMultipartError.FieldTooLong -> "One or more fields exceed the allowed length"
        CreateMapMultipartError.InvalidFileExtension -> "Uploaded file must be a .pmtiles file"
        CreateMapMultipartError.MissingFile -> "Missing pmtiles file"
        CreateMapMultipartError.InvalidPmtilesArchive -> "Uploaded file is not a valid PMTiles archive"
    }
