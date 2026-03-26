package com.vb.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            this@configureMonitoring.environment.log.error(
                "Unhandled exception for ${call.request.httpMethod.value} ${call.request.path()}",
                cause,
            )
            call.respond(HttpStatusCode.InternalServerError, "Internal server error")
        }
    }
}
