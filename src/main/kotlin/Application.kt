package com.vb

import com.vb.maps.data.ExposedMapRepository
import com.vb.maps.domain.MapRepository
import com.vb.plugins.DatabasePlugin
import com.vb.plugins.configureRouting
import com.vb.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.application.install

fun Application.module(mapRepository: MapRepository = ExposedMapRepository()) {
    configureSerialization()
    install(DatabasePlugin)
    configureRouting(mapRepository)
}
