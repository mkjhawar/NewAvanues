package com.augmentalis.httpavanue.client

import com.augmentalis.httpavanue.http.HttpClientException
import com.augmentalis.httpavanue.io.AvanueBuffer
import com.augmentalis.httpavanue.io.AvanueSource
import com.augmentalis.httpavanue.platform.currentTimeMillis

/**
 * HTTP response parser for client-side response reading
 */
internal object ResponseParser {
    private const val MAX_STATUS_LINE_SIZE = 2048
    private const val MAX_HEADER_SIZE = 8192
    private const val CR = '\r'.code.toByte()
    private const val LF = '\n'.code.toByte()

    suspend fun parse(source: AvanueSource, maxBodySize: Long = 50 * 1024 * 1024): ClientResponse {
        val startTime = currentTimeMillis()
        val statusLine = source.readUtf8Line(MAX_STATUS_LINE_SIZE)
            ?: throw HttpClientException("Empty status line")
        val (status, _) = parseStatusLine(statusLine)

        val headers = mutableMapOf<String, String>()
        var totalHeaderSize = statusLine.length
        while (true) {
            val line = source.readUtf8Line(MAX_HEADER_SIZE)
                ?: throw HttpClientException("Unexpected end of headers")
            totalHeaderSize += line.length
            if (totalHeaderSize > MAX_HEADER_SIZE) throw HttpClientException("Headers too large")
            if (line.isEmpty()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) throw HttpClientException("Invalid header: $line")
            headers[line.substring(0, colonIndex).trim()] = line.substring(colonIndex + 1).trim()
        }

        val body = parseBody(source, headers, maxBodySize)
        val duration = currentTimeMillis() - startTime

        return ClientResponse(status = status, headers = headers, body = body,
            requestTime = startTime, requestDuration = duration)
    }

    private fun parseStatusLine(line: String): Pair<Int, String> {
        val parts = line.split(' ', limit = 3)
        if (parts.size < 2) throw HttpClientException("Invalid status line: $line")
        val statusCode = parts[1].toIntOrNull() ?: throw HttpClientException("Invalid status code: ${parts[1]}")
        return statusCode to (if (parts.size >= 3) parts[2] else "")
    }

    private suspend fun parseBody(source: AvanueSource, headers: Map<String, String>, maxBodySize: Long): ByteArray? {
        val transferEncoding = headers["Transfer-Encoding"]?.lowercase()
        if (transferEncoding == "chunked") return parseChunkedBody(source, maxBodySize)
        val contentLength = headers["Content-Length"]?.toLongOrNull() ?: return null
        if (contentLength == 0L) return null
        if (contentLength > maxBodySize) throw HttpClientException(
            "Response body size ($contentLength bytes) exceeds maximum allowed ($maxBodySize bytes)")
        if (contentLength > Int.MAX_VALUE) throw HttpClientException("Content-Length too large: $contentLength")
        return source.readByteArray(contentLength)
    }

    private suspend fun parseChunkedBody(source: AvanueSource, maxBodySize: Long): ByteArray {
        val buffer = AvanueBuffer()
        var totalSize = 0L
        while (true) {
            val chunkSizeLine = source.readUtf8Line(MAX_STATUS_LINE_SIZE)
                ?: throw HttpClientException("Unexpected end of chunked body")
            val chunkSizeHex = chunkSizeLine.substringBefore(';').trim()
            val chunkSize = try { chunkSizeHex.toLong(16) }
            catch (e: NumberFormatException) { throw HttpClientException("Invalid chunk size: $chunkSizeHex") }
            if (chunkSize < 0) throw HttpClientException("Negative chunk size: $chunkSize")
            if (chunkSize == 0L) {
                while (true) {
                    val trailerLine = source.readUtf8Line(MAX_HEADER_SIZE)
                        ?: throw HttpClientException("Unexpected end of trailer")
                    if (trailerLine.isEmpty()) break
                }
                break
            }
            totalSize += chunkSize
            if (totalSize > maxBodySize) throw HttpClientException(
                "Chunked body size ($totalSize bytes) exceeds maximum allowed ($maxBodySize bytes)")
            buffer.write(source.readByteArray(chunkSize))
            val trailingLine = source.readUtf8Line(MAX_STATUS_LINE_SIZE)
            if (trailingLine == null || trailingLine.isNotEmpty())
                throw HttpClientException("Missing CRLF after chunk data")
        }
        return buffer.toByteArray()
    }

    private fun AvanueSource.readUtf8Line(maxLength: Int): String? {
        val buffer = AvanueBuffer()
        var length = 0L
        while (length < maxLength) {
            if (!request(1)) return null
            val b = readByte()
            length++
            if (b == CR) {
                if (request(1) && peek().readByte() == LF) { skip(1); length++ }
                return buffer.readUtf8()
            } else if (b == LF) return buffer.readUtf8()
            else buffer.writeByte(b.toInt())
        }
        throw HttpClientException("Line too long (> $maxLength bytes)")
    }
}
