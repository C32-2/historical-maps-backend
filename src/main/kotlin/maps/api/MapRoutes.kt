package com.vb.maps.api

import com.vb.maps.api.dto.toResponseDto
import com.vb.maps.api.upload.CreateMapMultipartParseResult
import com.vb.maps.api.upload.CreateMapMultipartParser
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

fun Route.mapRoutes(
    mapService: MapService,
    createMapMultipartParser: CreateMapMultipartParser = CreateMapMultipartParser(),
    uploadRateLimiter: UploadRateLimiter,
) {
    route("/maps") {
        get("/{id}") {
            val id = call.parseUuidParameter("id") ?: return@get
            val map = mapService.getMapById(id) ?: return@get call.respondMapNotFound()

            call.respond(map.toResponseDto())
        }

        delete("/{id}") {
            val id = call.parseUuidParameter("id") ?: return@delete

            val removed = mapService.removeMap(id)
            if (!removed) {
                return@delete call.respondMapNotFound()
            }

            call.respond(HttpStatusCode.NoContent)
        }

        get("/slug/{slug}") {
            val slug = call.parameters["slug"] ?: error("Route parameter 'slug' is required")
            val map = mapService.getMapBySlug(slug) ?: return@get call.respondMapNotFound()

            call.respond(map.toResponseDto())
        }

        post {
            if (!uploadRateLimiter.isAllowed(call.clientIpAddress())) {
                call.respond(HttpStatusCode.TooManyRequests, "Too many upload attempts. Try again later.")
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
            val title = call.request.queryParameters["title"]?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing title query parameter")

            val maps = mapService.findByTitle(title)

            call.respond(maps.map { it.toResponseDto() })
        }
    }
}

private suspend fun RoutingCall.parseUuidParameter(name: String): UUID? {
    val rawValue = parameters[name] ?: error("Route parameter '$name' is required")
    return runCatching { UUID.fromString(rawValue) }.getOrElse {
        respond(HttpStatusCode.BadRequest, "Invalid UUID")
        null
    }
}

private suspend fun RoutingCall.parseCreateMapRequest(
    parser: CreateMapMultipartParser,
) = when (val result = parser.parse(this)) {
    is CreateMapMultipartParseResult.Failure -> {
        respond(result.status, result.message)
        null
    }

    is CreateMapMultipartParseResult.Success -> result.value
}

private suspend fun RoutingCall.respondMapNotFound() {
    respond(HttpStatusCode.NotFound, "Map not found")
}

private fun ApplicationCall.clientIpAddress(): String =
    request.headers["X-Forwarded-For"]
        ?.substringBefore(',')
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: request.origin.remoteHost
