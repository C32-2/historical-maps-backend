package com.vb.maps.api

import com.vb.maps.api.dto.toResponseDto
import com.vb.maps.application.MapService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.UUID

fun Route.mapRoutes(
    mapService: MapService,
) {
    route("/maps") {
        get("/id/{id}") {
            val rawId = call.parameters["id"] ?: error("Route parameter 'id' is required")
            val id = runCatching { UUID.fromString(rawId) }.getOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid UUID")

            val map = mapService.getMapById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Map not found")

            call.respond(map.toResponseDto())
        }

        get("/slug/{slug}") {
            val slug = call.parameters["slug"] ?: error("Route parameter 'slug' is required")
            val map = mapService.getMapBySlug(slug)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Map not found")

            call.respond(map.toResponseDto())
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
