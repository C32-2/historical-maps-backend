package com.vb

import com.vb.maps.application.MapStorage
import com.vb.maps.data.db.ExposedMapRepository
import com.vb.maps.data.storage.LocalMapStorage
import com.vb.maps.domain.MapRepository
import com.vb.infrastructure.config.toStorageSettings
import com.vb.plugins.DatabasePlugin
import com.vb.plugins.InMemoryUploadRateLimiter
import com.vb.plugins.UploadRateLimiter
import com.vb.plugins.configureRouting
import com.vb.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.application.install

fun Application.module(
    mapRepository: MapRepository = ExposedMapRepository(),
    mapStorage: MapStorage = LocalMapStorage(environment.config.toStorageSettings().baseDir),
    uploadRateLimiter: UploadRateLimiter = InMemoryUploadRateLimiter(),
) {
    configureSerialization()
    install(DatabasePlugin)
    configureRouting(mapRepository, mapStorage, uploadRateLimiter)
}
