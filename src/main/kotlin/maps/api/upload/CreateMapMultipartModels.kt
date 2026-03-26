package com.vb.maps.api.upload

internal const val MAX_SLUG_LENGTH = 60
internal const val MAX_TITLE_LENGTH = 100
internal const val MAX_DESCRIPTION_LENGTH = 250
internal val SLUG_PATTERN = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
internal const val SLUG_FIELD = "slug"
internal const val TITLE_FIELD = "title"
internal const val DESCRIPTION_FIELD = "description"
internal val FILE_FIELDS = setOf("pmtiles", "file")

internal data class CreateMapMultipartPayload(
    val slug: String?,
    val title: String?,
    val description: String?,
    val uploadedFile: TemporaryUpload?,
    val hasInvalidFile: Boolean,
    val hasDuplicateFields: Boolean,
    val filePartsCount: Int,
) {
    fun cleanup() {
        uploadedFile?.delete()
    }
}
