package com.vb

import com.vb.plugins.DatabasePlugin
import com.vb.plugins.configureRouting
import com.vb.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.application.install

fun Application.module() {
    configureSerialization()
    install(DatabasePlugin)
    configureRouting()
}
