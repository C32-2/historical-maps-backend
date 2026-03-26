package com.vb

import com.vb.maps.application.MapStorage
import com.vb.maps.data.db.ExposedMapRepository
import com.vb.maps.data.storage.LocalMapStorage
import com.vb.maps.domain.MapRepository
import com.vb.infrastructure.config.adminToken
import com.vb.infrastructure.config.toStorageSettings
import com.vb.plugins.DatabasePlugin
import com.vb.plugins.InMemoryUploadRateLimiter
import com.vb.plugins.UploadRateLimiter
import com.vb.plugins.configureMonitoring
import com.vb.plugins.configureRouting
import com.vb.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val host = System.getenv("HOST")?.takeIf(String::isNotBlank) ?: "0.0.0.0"
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    embeddedServer(
        factory = Netty,
        host = host,
        port = port,
        module = Application::module,
    ).start(wait = true)
}

fun Application.module(
    mapRepository: MapRepository = ExposedMapRepository(),
    mapStorage: MapStorage = LocalMapStorage(environment.config.toStorageSettings().baseDir),
    uploadRateLimiter: UploadRateLimiter = InMemoryUploadRateLimiter(),
    adminToken: String = environment.config.adminToken(),
) {
    configureMonitoring()
    configureSerialization()
    install(DatabasePlugin)
    configureRouting(mapRepository, mapStorage, uploadRateLimiter, adminToken)
}
