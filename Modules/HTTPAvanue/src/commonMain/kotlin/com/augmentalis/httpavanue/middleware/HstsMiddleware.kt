package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse

/**
 * HTTP Strict Transport Security middleware (RFC 6797).
 * Adds the Strict-Transport-Security header to all responses.
 */
data class HstsConfig(
    val maxAge: Long = 31_536_000, // 1 year
    val includeSubdomains: Boolean = true,
    val preload: Boolean = false,
)

fun hstsMiddleware(config: HstsConfig = HstsConfig()) = middleware { request, next ->
    val response = next(request)
    val value = buildString {
        append("max-age=${config.maxAge}")
        if (config.includeSubdomains) append("; includeSubDomains")
        if (config.preload) append("; preload")
    }
    response.copy(
        headers = response.headers + ("Strict-Transport-Security" to value),
    )
}
