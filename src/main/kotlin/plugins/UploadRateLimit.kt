package com.vb.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private data class UploadRateLimitState(
    val windowStartedAtMillis: Long,
    val requestCount: AtomicInteger,
)

private val uploadRateLimitClock: Clock = Clock.systemUTC()
private val uploadRateLimitBuckets = ConcurrentHashMap<String, UploadRateLimitState>()

private const val UPLOAD_RATE_LIMIT_MAX_REQUESTS = 5
private val UPLOAD_RATE_LIMIT_WINDOW: Duration = Duration.ofMinutes(10)

object UploadRateLimiter {
    fun isAllowed(call: ApplicationCall): Boolean {
        val now = uploadRateLimitClock.millis()
        val clientIp = call.request.headers["X-Forwarded-For"]
            ?.substringBefore(',')
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: call.request.origin.remoteHost

        val state = uploadRateLimitBuckets.compute(clientIp) { _, existing ->
            if (existing == null || now - existing.windowStartedAtMillis >= UPLOAD_RATE_LIMIT_WINDOW.toMillis()) {
                UploadRateLimitState(
                    windowStartedAtMillis = now,
                    requestCount = AtomicInteger(1),
                )
            } else {
                existing.requestCount.incrementAndGet()
                existing
            }
        } ?: return false

        return state.requestCount.get() <= UPLOAD_RATE_LIMIT_MAX_REQUESTS
    }
}

fun resetUploadRateLimitBucketsForTests() {
    uploadRateLimitBuckets.clear()
}
