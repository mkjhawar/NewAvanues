package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.http.HttpStatus

/**
 * Range request middleware (RFC 7233) — supports single byte ranges.
 * Returns 206 Partial Content with Content-Range header for valid ranges,
 * 416 Range Not Satisfiable for invalid ranges.
 * Adds Accept-Ranges: bytes to all responses.
 */
fun rangeMiddleware() = middleware { request, next ->
    val response = next(request)

    // Always advertise range support
    val headers = response.headers.toMutableMap()
    headers["Accept-Ranges"] = "bytes"

    val rangeHeader = request.header("Range") ?: return@middleware response.copy(
        headers = headers.toMap(),
    )

    // Only handle ranges for 200 responses with a body
    if (response.status != HttpStatus.OK.code || response.body == null || response.body.isEmpty()) {
        return@middleware response.copy(headers = headers.toMap())
    }

    val totalSize = response.body.size.toLong()
    val range = parseRangeHeader(rangeHeader, totalSize)
    if (range == null) {
        // Invalid range format
        return@middleware HttpResponse(
            status = HttpStatus.RANGE_NOT_SATISFIABLE.code,
            statusMessage = HttpStatus.RANGE_NOT_SATISFIABLE.message,
            headers = headers + ("Content-Range" to "bytes */$totalSize"),
            body = """{"error":"Range Not Satisfiable"}""".encodeToByteArray(),
        )
    }

    val (start, end) = range
    if (start < 0 || end >= totalSize || start > end) {
        return@middleware HttpResponse(
            status = HttpStatus.RANGE_NOT_SATISFIABLE.code,
            statusMessage = HttpStatus.RANGE_NOT_SATISFIABLE.message,
            headers = headers + ("Content-Range" to "bytes */$totalSize"),
            body = """{"error":"Range Not Satisfiable"}""".encodeToByteArray(),
        )
    }

    // Validate If-Range (ETag check) if present
    val ifRange = request.header("If-Range")
    val etag = response.headers["ETag"]
    if (ifRange != null && etag != null && ifRange != etag) {
        // ETag doesn't match — return full response
        return@middleware response.copy(headers = headers.toMap())
    }

    val slicedBody = response.body.copyOfRange(start.toInt(), (end + 1).toInt())
    headers["Content-Range"] = "bytes $start-$end/$totalSize"
    headers["Content-Length"] = slicedBody.size.toString()

    HttpResponse(
        status = HttpStatus.PARTIAL_CONTENT.code,
        statusMessage = HttpStatus.PARTIAL_CONTENT.message,
        headers = headers.toMap(),
        body = slicedBody,
    )
}

/** Parse "Range: bytes=N-M" header. Returns (start, end) pair or null. */
private fun parseRangeHeader(rangeHeader: String, totalSize: Long): Pair<Long, Long>? {
    if (!rangeHeader.startsWith("bytes=", ignoreCase = true)) return null
    val rangeSpec = rangeHeader.substringAfter("bytes=").trim()

    // Only support single range (not multi-range)
    if (',' in rangeSpec) return null

    val dashIdx = rangeSpec.indexOf('-')
    if (dashIdx == -1) return null

    val startStr = rangeSpec.substring(0, dashIdx).trim()
    val endStr = rangeSpec.substring(dashIdx + 1).trim()

    return when {
        // Suffix range: bytes=-500 (last 500 bytes)
        startStr.isEmpty() -> {
            val suffix = endStr.toLongOrNull() ?: return null
            val start = maxOf(0, totalSize - suffix)
            start to (totalSize - 1)
        }
        // Open-ended: bytes=500- (from 500 to end)
        endStr.isEmpty() -> {
            val start = startStr.toLongOrNull() ?: return null
            start to (totalSize - 1)
        }
        // Full range: bytes=200-499
        else -> {
            val start = startStr.toLongOrNull() ?: return null
            val end = endStr.toLongOrNull() ?: return null
            start to end
        }
    }
}
