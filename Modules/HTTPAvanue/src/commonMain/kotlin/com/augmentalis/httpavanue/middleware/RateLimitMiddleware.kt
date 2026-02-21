package com.augmentalis.httpavanue.middleware

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.platform.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RateLimitMiddleware(private val config: RateLimitConfig = RateLimitConfig()) : Middleware {
    private val logger = LoggerFactory.getLogger("RateLimitMiddleware")
    private val ipBuckets = mutableMapOf<String, TokenBucket>()
    private val endpointBuckets = mutableMapOf<String, TokenBucket>()
    private val mutex = Mutex()
    private var requestCount = 0

    override suspend fun handle(request: HttpRequest, next: suspend (HttpRequest) -> HttpResponse): HttpResponse {
        if (++requestCount % 100 == 0) cleanupExpiredBuckets()
        // Only trust forwarded headers when request comes from a known proxy
        val remoteAddr = request.remoteAddress ?: "unknown"
        val clientIp = if (config.trustedProxies.isNotEmpty() && remoteAddr in config.trustedProxies) {
            request.headers["X-Forwarded-For"]?.split(",")?.first()?.trim()
                ?: request.headers["X-Real-IP"] ?: remoteAddr
        } else {
            remoteAddr
        }
        if (config.perIpLimit > 0 && !checkRateLimit(ipBuckets, clientIp, config.perIpLimit, config.windowMs)) {
            logger.w { "Rate limit exceeded for IP: $clientIp" }
            return createRateLimitResponse(config.perIpLimit, config.windowMs)
        }
        if (config.perEndpointLimit > 0) {
            val endpoint = "${request.method}:${request.path}"
            if (!checkRateLimit(endpointBuckets, endpoint, config.perEndpointLimit, config.windowMs)) {
                logger.w { "Rate limit exceeded for endpoint: $endpoint" }
                return createRateLimitResponse(config.perEndpointLimit, config.windowMs)
            }
        }
        val response = next(request)
        val rateLimitHeaders = mutex.withLock {
            ipBuckets[clientIp]?.let { bucket ->
                mapOf("X-RateLimit-Limit" to config.perIpLimit.toString(), "X-RateLimit-Remaining" to bucket.tokens.toInt().toString(),
                    "X-RateLimit-Reset" to ((bucket.lastRefill + config.windowMs) / 1000).toString())
            }
        }
        return if (rateLimitHeaders != null) response.copy(headers = response.headers + rateLimitHeaders) else response
    }

    private suspend fun checkRateLimit(buckets: MutableMap<String, TokenBucket>, key: String, maxRequests: Int, windowMs: Long): Boolean = mutex.withLock {
        val now = currentTimeMillis()
        val bucket = buckets.getOrPut(key) { TokenBucket(maxRequests.toDouble(), maxRequests.toDouble(), maxRequests.toDouble() / windowMs, now) }
        val refilledBucket = bucket.refilled(now)
        val (allowed, updatedBucket) = refilledBucket.tryConsume()
        buckets[key] = updatedBucket
        allowed
    }

    private fun createRateLimitResponse(limit: Int, windowMs: Long): HttpResponse {
        val retryAfter = (windowMs / 1000).toInt()
        return HttpResponse(429, "Too Many Requests",
            headers = mapOf("Content-Type" to "application/json", "X-RateLimit-Limit" to limit.toString(),
                "X-RateLimit-Remaining" to "0", "Retry-After" to retryAfter.toString()),
            body = """{"error":"Rate limit exceeded","retryAfter":$retryAfter}""".encodeToByteArray())
    }

    private suspend fun cleanupExpiredBuckets() = mutex.withLock {
        val now = currentTimeMillis(); val expirationTime = config.windowMs * 2
        ipBuckets.keys.filter { ipBuckets[it]?.let { b -> now - b.lastRefill > expirationTime } == true }.forEach { ipBuckets.remove(it) }
        endpointBuckets.keys.filter { endpointBuckets[it]?.let { b -> now - b.lastRefill > expirationTime } == true }.forEach { endpointBuckets.remove(it) }
    }
}

private data class TokenBucket(val capacity: Double, val tokens: Double, val refillRate: Double, val lastRefill: Long) {
    fun refilled(now: Long): TokenBucket { val tokensToAdd = (now - lastRefill) * refillRate; return copy(tokens = minOf(capacity, tokens + tokensToAdd), lastRefill = now) }
    fun tryConsume(): Pair<Boolean, TokenBucket> = if (tokens >= 1.0) true to copy(tokens = tokens - 1.0) else false to this
}

data class RateLimitConfig(val perIpLimit: Int = 100, val perEndpointLimit: Int = 0, val windowMs: Long = 60_000, val trustedProxies: Set<String> = emptySet())
fun rateLimitMiddleware(maxRequests: Int = 100, windowMs: Long = 60_000) = RateLimitMiddleware(RateLimitConfig(perIpLimit = maxRequests, windowMs = windowMs))
