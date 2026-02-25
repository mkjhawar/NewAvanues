package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.http.HttpStatus

/**
 * ETag middleware — computes weak ETags via FNV-1a hash and handles
 * conditional requests with If-None-Match → 304 Not Modified.
 *
 * Pure Kotlin, no crypto dependencies — uses FNV-1a for speed.
 */
data class ETagConfig(
    val includeWeakPrefix: Boolean = true,
)

fun etagMiddleware(config: ETagConfig = ETagConfig()) = middleware { request, next ->
    val response = next(request)

    // Only add ETag to 200 responses with a body
    if (response.status != HttpStatus.OK.code || response.body == null || response.body.isEmpty()) {
        return@middleware response
    }

    val hash = fnv1aHash(response.body)
    val etag = if (config.includeWeakPrefix) "W/\"$hash\"" else "\"$hash\""

    // Check If-None-Match
    val ifNoneMatch = request.header("If-None-Match")
    if (ifNoneMatch != null && (ifNoneMatch == etag || ifNoneMatch == "*")) {
        return@middleware HttpResponse(
            status = HttpStatus.NOT_MODIFIED.code,
            statusMessage = HttpStatus.NOT_MODIFIED.message,
            headers = response.headers + ("ETag" to etag),
            body = null,
        )
    }

    response.copy(
        headers = response.headers + ("ETag" to etag),
    )
}

/** FNV-1a 32-bit hash — fast, non-cryptographic, good distribution. */
private fun fnv1aHash(data: ByteArray): String {
    var hash = 0x811c9dc5.toInt()
    for (byte in data) {
        hash = hash xor (byte.toInt() and 0xFF)
        hash = (hash * 0x01000193)
    }
    return hash.toUInt().toString(16).padStart(8, '0')
}
