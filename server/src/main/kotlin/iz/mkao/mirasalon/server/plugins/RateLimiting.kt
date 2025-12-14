package iz.mkao.mirasalon.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond
import iz.mkao.mirasalon.core.network.model.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

/**
 * Rate limiter using token bucket.
 * For distributed setups, replace ConcurrentHashMap with Redis Lua script.
 * Limits are configurable via environment variables.
 */
fun Application.configureRateLimiting() {
    val generalLimit = System.getenv("RATE_LIMIT_GENERAL")?.toIntOrNull() ?: 500
    val authLimit = System.getenv("RATE_LIMIT_AUTH")?.toIntOrNull() ?: 50
    val refillPeriodMs = 60_000L

    val generalBuckets = ConcurrentHashMap<String, TokenBucket>()
    val authBuckets = ConcurrentHashMap<String, TokenBucket>()

    val authPatterns = listOf(
        Regex("""(?:/v\d+)?/api/auth/login"""),
        Regex("""(?:/v\d+)?/api/auth/register""")
    )

    val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    cleanupScope.launch {
        while (true) {
            delay((refillPeriodMs * 5).milliseconds)
            generalBuckets.entries.removeIf { it.value.isIdle(refillPeriodMs * 5) }
            authBuckets.entries.removeIf { it.value.isIdle(refillPeriodMs * 5) }
        }
    }
    monitor.subscribe(ApplicationStopped) {
        cleanupScope.cancel()
    }

    intercept(ApplicationCallPipeline.Call) {
        val call = this.call
        val ip = call.request.headers["X-Forwarded-For"]
            ?.split(",")?.firstOrNull()?.trim()
            ?: call.request.local.remoteAddress

        val path = call.request.path()
        val isAuth = authPatterns.any { it.containsMatchIn(path) }

        val buckets = if (isAuth) authBuckets else generalBuckets
        val maxTokens = if (isAuth) authLimit else generalLimit

        val bucket = buckets.computeIfAbsent(ip) {
            TokenBucket(maxTokens = maxTokens, refillPeriodMs = refillPeriodMs)
        }

        call.response.header("X-RateLimit-Limit", maxTokens.toString())
        call.response.header("X-RateLimit-Remaining", bucket.remainingTokens().toString())

        if (!bucket.tryConsume()) {
            val retryAfter = bucket.secondsUntilRefill()
            call.response.header("Retry-After", retryAfter.toString())
            call.respond(
                HttpStatusCode.TooManyRequests,
                ApiResponse<Unit>(
                    success = false,
                    error = "Rate limit exceeded. Try again in ${retryAfter}s."
                )
            )
            finish()
        }
    }
}

/**
 * Token-bucket implementation with improved refill logic.
 * Still uses synchronized, but reduces lock contention by refilling on a schedule.
 */
private class TokenBucket(
    private val maxTokens: Int,
    private val refillPeriodMs: Long
) {
    private val tokens = AtomicInteger(maxTokens)
    @Volatile
    private var lastRefillTime = System.currentTimeMillis()

    fun tryConsume(): Boolean {
        refillIfNeeded()
        while (true) {
            val current = tokens.get()
            if (current <= 0) return false
            if (tokens.compareAndSet(current, current - 1)) return true
        }
    }

    private fun refillIfNeeded() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefillTime
        if (elapsed >= refillPeriodMs) {
            synchronized(this) {
                val elapsedSync = now - lastRefillTime
                if (elapsedSync >= refillPeriodMs) {
                    val periods = elapsedSync / refillPeriodMs
                    val tokensToAdd = (periods * maxTokens).coerceAtMost(maxTokens.toLong()).toInt()
                    val current = tokens.get()
                    val newVal = (current + tokensToAdd).coerceAtMost(maxTokens)
                    tokens.set(newVal)
                    lastRefillTime += periods * refillPeriodMs
                }
            }
        }
    }

    fun remainingTokens(): Int = tokens.get().coerceAtLeast(0)

    fun isIdle(idleMs: Long): Boolean =
        System.currentTimeMillis() - lastRefillTime > idleMs

    fun secondsUntilRefill(): Long {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefillTime
        return ((refillPeriodMs - elapsed).coerceAtLeast(0)) / 1000
    }
}
