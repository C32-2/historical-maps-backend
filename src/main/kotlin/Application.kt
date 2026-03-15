package com.vb

import com.vb.infrastructure.db.DatabaseFactory
import com.vb.maps.api.mapRoutes
import com.vb.maps.application.MapService
import com.vb.maps.data.MapRepository
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.module() {
    DatabaseFactory.init(this)

    val mapRepository = MapRepository()
    val mapService = MapService(mapRepository)

    routing {
        mapRoutes(mapService)
    }
}