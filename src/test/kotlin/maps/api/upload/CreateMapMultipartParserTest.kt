package com.vb.maps.api.upload

import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.forms.FormBuilder
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateMapMultipartParserTest {
    private val parser = CreateMapMultipartParser()

    @Test
    fun parsesValidMultipartRequest() = withParserTestApplication {
        client.submitFormWithBinaryData(
            url = "/parse",
            formData = createValidUploadFormData("new-map", "New map"),
        ).apply {
            assertEquals("OK:new-map:New map", body<String>())
        }
    }

    @Test
    fun rejectsRequestWithMultiplePmtilesFiles() = withParserTestApplication {
        client.submitFormWithBinaryData(
            url = "/parse",
            formData = formData {
                append("slug", "new-map")
                append("title", "New map")
                appendPmtilesFile(validPmtilesBytes())
                append(
                    key = "file",
                    value = validPmtilesBytes(),
                    headers = Headers.build {
                        append("Content-Disposition", "filename=\"tiles.pmtiles\"")
                        append("Content-Type", ContentType.Application.OctetStream.toString())
                    }
                )
            }
        ).apply {
            assertEquals("Exactly one pmtiles file must be uploaded", body<String>())
        }
    }

    @Test
    fun rejectsDuplicateMultipartFields() = withParserTestApplication {
        client.submitFormWithBinaryData(
            url = "/parse",
            formData = formData {
                append("slug", "new-map")
                append("slug", "new-map-duplicate")
                append("title", "New map")
                appendPmtilesFile(validPmtilesBytes())
            }
        ).apply {
            assertEquals("Duplicate multipart fields are not allowed", body<String>())
        }
    }

    @Test
    fun rejectsRequestWithoutPmtilesFile() = withParserTestApplication {
        client.submitFormWithBinaryData(
            url = "/parse",
            formData = formData {
                append("slug", "new-map")
                append("title", "New map")
                append("description", "Uploaded map")
            }
        ).apply {
            assertEquals("Exactly one pmtiles file must be uploaded", body<String>())
        }
    }

    @Test
    fun rejectsInvalidSlugFormat() = withParserTestApplication {
        client.submitFormWithBinaryData(
            url = "/parse",
            formData = createValidUploadFormData("Invalid_Slug", "New map"),
        ).apply {
            assertEquals("Slug may contain only lowercase letters, digits, and hyphens", body<String>())
        }
    }

    @Test
    fun rejectsFieldsThatExceedAllowedLength() = withParserTestApplication {
        client.submitFormWithBinaryData(
            url = "/parse",
            formData = createValidUploadFormData(
                slug = "a".repeat(61),
                title = "Valid title",
            ),
        ).apply {
            assertEquals("One or more fields exceed the allowed length", body<String>())
        }
    }

    @Test
    fun rejectsFileWithoutPmtilesExtension() = withParserTestApplication {
        client.submitFormWithBinaryData(
            url = "/parse",
            formData = formData {
                append("slug", "new-map")
                append("title", "New map")
                append(
                    key = "pmtiles",
                    value = validPmtilesBytes(),
                    headers = Headers.build {
                        append("Content-Disposition", "filename=\"tiles.txt\"")
                        append("Content-Type", ContentType.Application.OctetStream.toString())
                    }
                )
            }
        ).apply {
            assertEquals("Uploaded file must be a .pmtiles file", body<String>())
        }
    }

    @Test
    fun rejectsRequestWithoutTitle() = withParserTestApplication {
        client.submitFormWithBinaryData(
            url = "/parse",
            formData = formData {
                append("slug", "new-map")
                appendPmtilesFile(validPmtilesBytes())
            }
        ).apply {
            assertEquals("Missing slug or title", body<String>())
        }
    }

    @Test
    fun rejectsRequestWithoutSlug() = withParserTestApplication {
        client.submitFormWithBinaryData(
            url = "/parse",
            formData = formData {
                append("title", "New map")
                appendPmtilesFile(validPmtilesBytes())
            }
        ).apply {
            assertEquals("Missing slug or title", body<String>())
        }
    }

    private fun withParserTestApplication(test: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            routing {
                post("/parse") {
                    when (val result = parser.parse(call)) {
                        is CreateMapMultipartParseResult.Failure -> {
                            call.respond(result.status, result.message)
                        }

                        is CreateMapMultipartParseResult.Success -> {
                            try {
                                call.respond("OK:${result.value.request.slug}:${result.value.request.title}")
                            } finally {
                                result.value.cleanup()
                            }
                        }
                    }
                }
            }
        }

        test()
    }
}

private fun createValidUploadFormData(slug: String, title: String) = formData {
    append("slug", slug)
    append("title", title)
    append("description", "Uploaded map")
    appendPmtilesFile(validPmtilesBytes())
}

private fun FormBuilder.appendPmtilesFile(bytes: ByteArray) {
    append(
        key = "pmtiles",
        value = bytes,
        headers = Headers.build {
            append("Content-Disposition", "filename=\"tiles.pmtiles\"")
            append("Content-Type", ContentType.Application.OctetStream.toString())
        }
    )
}

private fun validPmtilesBytes(): ByteArray {
    val bytes = ByteArray(127)
    val magic = "PMTiles".encodeToByteArray()
    magic.copyInto(bytes, destinationOffset = 0)
    bytes[7] = 3
    ByteBuffer.wrap(bytes, 8, Long.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(127L)
    return bytes
}
