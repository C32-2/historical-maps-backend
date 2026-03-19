package com.vb.maps.api

import com.vb.maps.api.dto.toResponseDto
import com.vb.maps.application.MapService
import com.vb.plugins.UploadRateLimiter
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.mapRoutes(
    mapService: MapService,
    createMapMultipartParser: CreateMapMultipartParser = CreateMapMultipartParser(),
) {
    route("/maps") {
        get("/{id}") {
            val rawId = call.parameters["id"] ?: error("Route parameter 'id' is required")
            val id = runCatching { UUID.fromString(rawId) }.getOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid UUID")

            val map = mapService.getMapById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Map not found")

            call.respond(map.toResponseDto())
        }

        delete("/{id}") {
            val rawId = call.parameters["id"] ?: error("Route parameter 'id' is required")
            val id = runCatching { UUID.fromString(rawId) }.getOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid UUID")

            val removed = mapService.removeMap(id)
            if (!removed) {
                return@delete call.respond(HttpStatusCode.NotFound, "Map not found")
            }

            call.respond(HttpStatusCode.NoContent)
        }

        get("/slug/{slug}") {
            val slug = call.parameters["slug"] ?: error("Route parameter 'slug' is required")
            val map = mapService.getMapBySlug(slug)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Map not found")

            call.respond(map.toResponseDto())
        }

        post {
            if (!UploadRateLimiter.isAllowed(call)) {
                call.respond(HttpStatusCode.TooManyRequests, "Too many upload attempts. Try again later.")
                return@post
            }

            val parsedRequest = when (val result = createMapMultipartParser.parse(call)) {
                is CreateMapMultipartParseResult.Failure -> {
                    call.respond(result.status, result.message)
                    return@post
                }

                is CreateMapMultipartParseResult.Success -> result.value
            }

            try {
                val map = mapService.saveMap(parsedRequest.request, parsedRequest.openFileContent())
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
