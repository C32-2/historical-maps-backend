package com.vb.plugins

import com.vb.maps.api.mapRoutes
import com.vb.maps.application.MapService
import com.vb.maps.domain.MapRepository
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(mapRepository: MapRepository) {
    val mapService = MapService(mapRepository)
    routing {
        mapRoutes(mapService)
    }
}
