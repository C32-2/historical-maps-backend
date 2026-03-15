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
            val rawId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")

            val id = runCatching { UUID.fromString(rawId) }.getOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid UUID")

            val map = mapService.getMapById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Map not found")

            call.respond(map.toResponseDto())
        }

        get("/slug/{slug}") {
            val slug = call.parameters["slug"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing slug")

            val map = mapService.getMapBySlug(slug)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Map not found")

            call.respond(map.toResponseDto())
        }
    }
}