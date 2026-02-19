package com.augmentalis.httpavanue.server

import com.augmentalis.httpavanue.http.HttpMethod
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.PayloadTooLargeException
import okio.Buffer
import okio.BufferedSource

/**
 * HTTP/1.1 request parser — NanoHTTPD-inspired, rewritten for Kotlin + Okio
 */
internal object HttpParser {
    private const val CR = '\r'.code.toByte()
    private const val LF = '\n'.code.toByte()
    private const val MAX_HEADER_SIZE = 8192
    private const val MAX_REQUEST_LINE_SIZE = 2048

    suspend fun parse(source: BufferedSource, maxBodySize: Long = 10 * 1024 * 1024): HttpRequest {
        val requestLine = source.readUtf8Line(MAX_REQUEST_LINE_SIZE)
            ?: throw HttpParseException("Empty request line")
        val (method, uri, version) = parseRequestLine(requestLine)

        val headers = mutableMapOf<String, String>()
        var totalHeaderSize = requestLine.length
        while (true) {
            val line = source.readUtf8Line(MAX_HEADER_SIZE)
                ?: throw HttpParseException("Unexpected end of headers")
            totalHeaderSize += line.length
            if (totalHeaderSize > MAX_HEADER_SIZE) throw HttpParseException("Headers too large")
            if (line.isEmpty()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) throw HttpParseException("Invalid header: $line")
            headers[line.substring(0, colonIndex).trim()] = line.substring(colonIndex + 1).trim()
        }

        val body = parseBody(source, headers, maxBodySize)
        val queryParams = parseQueryParams(uri)

        return HttpRequest(method = method, uri = uri, version = version,
            headers = headers, body = body, queryParams = queryParams)
    }

    private fun parseRequestLine(line: String): Triple<HttpMethod, String, String> {
        val parts = line.split(' ')
        if (parts.size != 3) throw HttpParseException("Invalid request line: $line")
        val method = try { HttpMethod.from(parts[0]) }
        catch (e: IllegalArgumentException) { throw HttpParseException("Invalid HTTP method: ${parts[0]}") }
        return Triple(method, parts[1], parts[2])
    }

    private suspend fun parseBody(source: BufferedSource, headers: Map<String, String>, maxBodySize: Long): ByteArray? {
        val transferEncoding = headers["Transfer-Encoding"]?.lowercase()
        if (transferEncoding == "chunked") return parseChunkedBody(source, maxBodySize)
        val contentLength = headers["Content-Length"]?.toLongOrNull() ?: return null
        if (contentLength == 0L) return null
        if (contentLength > maxBodySize) throw PayloadTooLargeException(
            "Request body size ($contentLength bytes) exceeds maximum allowed ($maxBodySize bytes)")
        if (contentLength > Int.MAX_VALUE) throw HttpParseException("Content-Length too large: $contentLength")
        return source.readByteArray(contentLength)
    }

    private suspend fun parseChunkedBody(source: BufferedSource, maxBodySize: Long): ByteArray {
        val buffer = Buffer()
        var totalSize = 0L
        while (true) {
            val chunkSizeLine = source.readUtf8Line(MAX_REQUEST_LINE_SIZE)
                ?: throw HttpParseException("Unexpected end of chunked body")
            val chunkSizeHex = chunkSizeLine.substringBefore(';').trim()
            val chunkSize = try { chunkSizeHex.toLong(16) }
            catch (e: NumberFormatException) { throw HttpParseException("Invalid chunk size: $chunkSizeHex") }
            if (chunkSize < 0) throw HttpParseException("Negative chunk size: $chunkSize")
            if (chunkSize == 0L) {
                while (true) {
                    val trailerLine = source.readUtf8Line(MAX_HEADER_SIZE)
                        ?: throw HttpParseException("Unexpected end of trailer")
                    if (trailerLine.isEmpty()) break
                }
                break
            }
            totalSize += chunkSize
            if (totalSize > maxBodySize) throw PayloadTooLargeException(
                "Chunked body size ($totalSize bytes) exceeds maximum allowed ($maxBodySize bytes)")
            buffer.write(source.readByteArray(chunkSize))
            val trailingLine = source.readUtf8Line(MAX_REQUEST_LINE_SIZE)
            if (trailingLine == null || trailingLine.isNotEmpty())
                throw HttpParseException("Missing CRLF after chunk data")
        }
        return buffer.readByteArray()
    }

    private fun parseQueryParams(uri: String): Map<String, List<String>> {
        val queryStart = uri.indexOf('?')
        if (queryStart == -1) return emptyMap()
        val query = uri.substring(queryStart + 1)
        val params = mutableMapOf<String, MutableList<String>>()
        query.split('&').forEach { param ->
            if (param.isBlank()) return@forEach
            val eqIndex = param.indexOf('=')
            val (name, value) = if (eqIndex == -1) param to "" else param.substring(0, eqIndex) to param.substring(eqIndex + 1)
            params.getOrPut(name) { mutableListOf() }.add(value)
        }
        return params
    }

    private fun BufferedSource.readUtf8Line(maxLength: Int): String? {
        val buffer = Buffer()
        var length = 0L
        while (length < maxLength) {
            val b = readByte()
            length++
            if (b == CR) {
                if (request(1) && peek().readByte() == LF) { skip(1); length++ }
                return buffer.readUtf8()
            } else if (b == LF) {
                return buffer.readUtf8()
            } else {
                buffer.writeByte(b.toInt())
            }
        }
        throw HttpParseException("Line too long (> $maxLength bytes)")
    }
}

internal open class HttpParseException(message: String) : Exception(message)
