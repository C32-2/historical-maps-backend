package com.vb.maps.api

import com.vb.maps.api.dto.toResponseDto
import com.vb.maps.api.upload.CreateMapMultipartError
import com.vb.maps.api.upload.CreateMapMultipartParseResult
import com.vb.maps.api.upload.CreateMapMultipartParser
import com.vb.maps.api.upload.message
import com.vb.maps.api.upload.status
import com.vb.maps.application.MapService
import com.vb.plugins.UploadRateLimiter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

private const val MAP_NOT_FOUND_MESSAGE = "Map not found"
private const val INVALID_UUID_MESSAGE = "Invalid UUID"
private const val TOO_MANY_UPLOAD_ATTEMPTS_MESSAGE = "Too many upload attempts. Try again later."
private const val INVALID_ADMIN_TOKEN_MESSAGE = "Invalid admin token"

fun Route.mapRoutes(
    mapService: MapService,
    createMapMultipartParser: CreateMapMultipartParser = CreateMapMultipartParser(),
    uploadRateLimiter: UploadRateLimiter,
    adminToken: String,
) {
    route("/maps") {
        get("/{id}") {
            val id = call.parseUuidParameter("id") ?: return@get
            val map = mapService.getMapById(id) ?: return@get call.respondMapNotFound()

            call.respond(map.toResponseDto())
        }

        delete("/{id}") {
            if (!call.requireAdminToken(adminToken)) return@delete

            val id = call.parseUuidParameter("id") ?: return@delete

            val removed = mapService.removeMap(id)
            if (!removed) {
                return@delete call.respondMapNotFound()
            }

            call.respond(HttpStatusCode.NoContent)
        }

        get("/slug/{slug}") {
            val slug = call.requiredParameter("slug") ?: return@get
            val map = mapService.getMapBySlug(slug) ?: return@get call.respondMapNotFound()

            call.respond(map.toResponseDto())
        }

        post {
            if (!call.requireAdminToken(adminToken)) return@post

            if (!uploadRateLimiter.isAllowed(call.clientIpAddress())) {
                call.respond(HttpStatusCode.TooManyRequests, TOO_MANY_UPLOAD_ATTEMPTS_MESSAGE)
                return@post
            }

            val parsedRequest = call.parseCreateMapRequest(createMapMultipartParser) ?: return@post

            try {
                val map = mapService.saveMap(parsedRequest.command, parsedRequest.openFileContent())
                call.respond(HttpStatusCode.Created, map.toResponseDto())
            } finally {
                parsedRequest.cleanup()
            }
        }

        get {
            val title = call.request.queryParameters["title"]
                ?.trim()
                ?.takeIf(String::isNotEmpty)

            val maps = if (title != null) {
                mapService.findByTitle(title)
            } else {
                mapService.getAll()
            }

            call.respond(maps.map { it.toResponseDto() })
        }
    }
}

private suspend fun RoutingCall.parseUuidParameter(name: String): UUID? {
    val rawValue = requiredParameter(name) ?: return null
    return runCatching { UUID.fromString(rawValue) }.getOrElse {
        respond(HttpStatusCode.BadRequest, INVALID_UUID_MESSAGE)
        null
    }
}

private suspend fun RoutingCall.parseCreateMapRequest(
    parser: CreateMapMultipartParser,
) = when (val result = parser.parse(this)) {
    is CreateMapMultipartParseResult.Failure -> {
        respond(result.error.status, result.error.message)
        null
    }

    is CreateMapMultipartParseResult.Success -> result.value
}

private suspend fun RoutingCall.respondMapNotFound() {
    respond(HttpStatusCode.NotFound, MAP_NOT_FOUND_MESSAGE)
}

private suspend fun ApplicationCall.requireAdminToken(expectedToken: String): Boolean {
    val actualToken = request.headers["X-Admin-Token"]
    if (actualToken != expectedToken) {
        respond(HttpStatusCode.Unauthorized, INVALID_ADMIN_TOKEN_MESSAGE)
        return false
    }
    return true
}

private fun ApplicationCall.clientIpAddress(): String =
    request.headers["X-Forwarded-For"]
        ?.substringBefore(',')
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: request.origin.remoteHost

private suspend fun RoutingCall.requiredParameter(name: String): String? =
    parameters[name] ?: run {
        respond(HttpStatusCode.BadRequest, "Missing route parameter '$name'")
        null
    }
