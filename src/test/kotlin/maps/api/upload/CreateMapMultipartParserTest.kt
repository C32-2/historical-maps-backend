package com.vb.maps.api.upload

import com.vb.maps.support.appendPmtilesFile
import com.vb.maps.support.createValidUploadFormData
import com.vb.maps.support.validPmtilesBytes
import com.vb.maps.api.upload.message
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
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
                            call.respond(result.error.message)
                        }

                        is CreateMapMultipartParseResult.Success -> {
                            try {
                                call.respond("OK:${result.value.command.slug}:${result.value.command.title}")
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
