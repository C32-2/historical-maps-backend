package com.vb.infrastructure.config

import io.ktor.server.config.ApplicationConfig

fun ApplicationConfig.adminToken(): String =
    stringSetting(
        path = "adminToken",
        envName = "ADMIN_TOKEN",
    )
