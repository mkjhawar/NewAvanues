package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse

/**
 * Multipart form-data parser — extracts parts from multipart/form-data requests.
 *
 * Closes NanoHTTPD gap #1: multipart file upload support.
 * Parses Content-Disposition and per-part Content-Type headers.
 */
data class MultipartPart(
    val name: String,
    val filename: String? = null,
    val contentType: String? = null,
    val data: ByteArray,
) {
    /** Interpret part data as UTF-8 text (for text fields). */
    fun asText(): String = data.decodeToString()

    override fun equals(other: Any?) = other is MultipartPart &&
        name == other.name && filename == other.filename &&
        contentType == other.contentType && data.contentEquals(other.data)
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (filename?.hashCode() ?: 0)
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Parse multipart/form-data from an HTTP request body.
 * Returns null if the request is not multipart.
 */
fun HttpRequest.multipartParts(): List<MultipartPart>? {
    val ct = contentType ?: return null
    if (!ct.contains("multipart/form-data", ignoreCase = true)) return null
    val boundary = extractBoundary(ct) ?: return null
    val body = this.body ?: return null
    return parseMultipart(body, boundary)
}

/**
 * Middleware that auto-parses multipart requests and stores the part count marker
 * in request context. Parts are re-parsed on retrieval since the context map is String-typed.
 *
 * Note: HttpRequest.context is Map<String, String>, so we store a marker and
 * re-parse from the body on retrieval. For zero-copy access, use multipartParts() directly.
 */
fun multipartMiddleware() = middleware { request, next ->
    val parts = request.multipartParts()
    if (parts != null) {
        val ctx = request.context.toMutableMap()
        ctx["multipart_parsed"] = "true"
        ctx["multipart_count"] = parts.size.toString()
        next(request.copy(context = ctx))
    } else {
        next(request)
    }
}

/**
 * Retrieve multipart parts from request.
 * If multipartMiddleware was applied (context contains "multipart_parsed"),
 * this re-parses the body. For performance-sensitive paths, call multipartParts() directly
 * and cache the result in your handler.
 */
fun HttpRequest.parsedMultipartParts(): List<MultipartPart>? {
    if (context["multipart_parsed"] != "true") return null
    return multipartParts()
}

private fun extractBoundary(contentType: String): String? {
    val boundaryParam = contentType.split(';')
        .map { it.trim() }
        .firstOrNull { it.startsWith("boundary=", ignoreCase = true) }
        ?: return null
    return boundaryParam.substringAfter('=').trim().removeSurrounding("\"")
}

private fun parseMultipart(body: ByteArray, boundary: String): List<MultipartPart> {
    val boundaryBytes = "--$boundary".encodeToByteArray()
    val endBoundaryBytes = "--$boundary--".encodeToByteArray()
    val bodyString = body.decodeToString() // Safe for boundary/header scanning
    val parts = mutableListOf<MultipartPart>()

    // Split on boundary markers
    val sections = bodyString.split("--$boundary")
    for (section in sections) {
        val trimmed = section.trimStart('\r', '\n')
        if (trimmed.isEmpty() || trimmed.startsWith("--")) continue // preamble or epilogue

        // Split headers from body at double CRLF
        val headerEnd = trimmed.indexOf("\r\n\r\n")
        if (headerEnd == -1) continue
        val headerBlock = trimmed.substring(0, headerEnd)
        val partBody = trimmed.substring(headerEnd + 4).removeSuffix("\r\n")

        // Parse part headers
        val headers = mutableMapOf<String, String>()
        headerBlock.split("\r\n").forEach { line ->
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                headers[line.substring(0, colonIdx).trim().lowercase()] =
                    line.substring(colonIdx + 1).trim()
            }
        }

        // Extract name and filename from Content-Disposition
        val disposition = headers["content-disposition"] ?: continue
        val name = extractDispositionParam(disposition, "name") ?: continue
        val filename = extractDispositionParam(disposition, "filename")
        val partContentType = headers["content-type"]

        parts.add(MultipartPart(
            name = name,
            filename = filename,
            contentType = partContentType,
            data = partBody.encodeToByteArray(),
        ))
    }
    return parts
}

private fun extractDispositionParam(disposition: String, param: String): String? {
    val pattern = Regex("""$param\s*=\s*"?([^";]+)"?""", RegexOption.IGNORE_CASE)
    return pattern.find(disposition)?.groupValues?.get(1)
}
