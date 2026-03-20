package com.vb.plugins

import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private data class UploadRateLimitState(
    val windowStartedAtMillis: Long,
    val requestCount: AtomicInteger,
)

interface UploadRateLimiter {
    fun isAllowed(clientKey: String): Boolean
}

class InMemoryUploadRateLimiter(
    private val clock: Clock = Clock.systemUTC(),
    private val maxRequests: Int = 5,
    private val window: Duration = Duration.ofMinutes(10),
) : UploadRateLimiter {
    private val buckets = ConcurrentHashMap<String, UploadRateLimitState>()

    override fun isAllowed(clientKey: String): Boolean {
        val now = clock.millis()
        val state = buckets.compute(clientKey) { _, existing ->
            if (existing == null || now - existing.windowStartedAtMillis >= window.toMillis()) {
                UploadRateLimitState(
                    windowStartedAtMillis = now,
                    requestCount = AtomicInteger(1),
                )
            } else {
                existing.requestCount.incrementAndGet()
                existing
            }
        } ?: return false

        return state.requestCount.get() <= maxRequests
    }

    fun reset() {
        buckets.clear()
    }
}
