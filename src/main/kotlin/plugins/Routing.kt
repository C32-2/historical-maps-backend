package com.vb.plugins

import com.vb.maps.api.mapRoutes
import com.vb.maps.application.MapService
import com.vb.maps.application.MapStorage
import com.vb.maps.domain.MapRepository
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(
    mapRepository: MapRepository,
    mapStorage: MapStorage,
    uploadRateLimiter: UploadRateLimiter,
) {
    val mapService = MapService(mapRepository, mapStorage)
    routing {
        mapRoutes(
            mapService = mapService,
            uploadRateLimiter = uploadRateLimiter,
        )
    }
}
