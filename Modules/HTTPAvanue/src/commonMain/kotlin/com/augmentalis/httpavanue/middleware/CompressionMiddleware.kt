package com.augmentalis.httpavanue.middleware

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse

class CompressionMiddleware(private val config: CompressionConfig = CompressionConfig()) : Middleware {
    private val logger = LoggerFactory.getLogger("CompressionMiddleware")

    override suspend fun handle(request: HttpRequest, next: suspend (HttpRequest) -> HttpResponse): HttpResponse {
        val processedRequest = if (shouldDecompressRequest(request)) decompressRequest(request) else request
        val response = next(processedRequest)
        return if (shouldCompressResponse(request, response)) compressResponse(response) else response
    }

    private fun shouldDecompressRequest(request: HttpRequest) =
        request.headers["Content-Encoding"]?.lowercase() == "gzip" && request.body?.isNotEmpty() == true

    private fun decompressRequest(request: HttpRequest): HttpRequest = try {
        request.body?.let { body ->
            val decompressed = gzipDecompress(body)
            logger.d { "Decompressed request: ${body.size} -> ${decompressed.size} bytes" }
            request.copy(body = decompressed, headers = request.headers.toMutableMap().apply {
                remove("Content-Encoding"); put("Content-Length", decompressed.size.toString())
            })
        } ?: request
    } catch (e: Exception) { logger.w { "Failed to decompress request body: ${e.message}" }; request }

    private fun shouldCompressResponse(request: HttpRequest, response: HttpResponse): Boolean {
        val acceptEncoding = request.headers["Accept-Encoding"]?.lowercase() ?: ""
        if (!acceptEncoding.contains("gzip")) return false
        if (response.headers["Content-Encoding"] != null) return false
        val body = response.body ?: return false
        if (body.isEmpty() || body.size < config.minBytes) return false
        val contentType = response.headers["Content-Type"]?.lowercase() ?: ""
        return shouldCompressContentType(contentType)
    }

    private fun shouldCompressContentType(contentType: String): Boolean {
        val skipTypes = setOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "video/", "audio/",
            "application/zip", "application/gzip", "application/x-gzip")
        if (skipTypes.any { contentType.contains(it) }) return false
        val compressTypes = setOf("text/", "application/json", "application/javascript", "application/xml")
        return compressTypes.any { contentType.contains(it) }
    }

    private fun compressResponse(response: HttpResponse): HttpResponse = try {
        response.body?.let { body ->
            val compressed = gzipCompress(body)
            logger.d { "Compressed response: ${body.size} -> ${compressed.size} bytes" }
            response.copy(body = compressed, headers = response.headers + mapOf(
                "Content-Encoding" to "gzip", "Content-Length" to compressed.size.toString(), "Vary" to "Accept-Encoding"))
        } ?: response
    } catch (e: Exception) { logger.w { "Failed to compress response: ${e.message}" }; response }
}

expect fun gzipCompress(data: ByteArray): ByteArray
expect fun gzipDecompress(data: ByteArray): ByteArray

data class CompressionConfig(val level: CompressionLevel = CompressionLevel.MEDIUM, val minBytes: Int = 1024)
enum class CompressionLevel { FAST, MEDIUM, BEST }
fun compressionMiddleware(level: CompressionLevel = CompressionLevel.MEDIUM, minBytes: Int = 1024) =
    CompressionMiddleware(CompressionConfig(level, minBytes))
