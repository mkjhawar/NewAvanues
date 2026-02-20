package com.augmentalis.httpavanue.http

/**
 * Immutable HTTP response representation.
 * Not serializable — this is a server-internal pipeline type.
 * For client-side serializable request/response types, see ClientModels.kt.
 */
data class HttpResponse(
    val status: Int,
    val statusMessage: String,
    val version: String = "HTTP/1.1",
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
) {
    val contentLength: Long by lazy { headers["Content-Length"]?.toLongOrNull() ?: body?.size?.toLong() ?: 0L }
    val contentType: String? by lazy { headers["Content-Type"] }

    fun header(name: String) = headers[name]

    fun withHeader(name: String, value: String) = copy(headers = headers + (name to value))

    fun toBytes(): ByteArray = buildString {
        append("$version $status $statusMessage\r\n")
        headers.forEach { (name, value) -> append("$name: $value\r\n") }
        append("\r\n")
    }.encodeToByteArray() + (body ?: byteArrayOf())

    fun toChunked(chunkSize: Int = 8192): ByteArray {
        val responseHeaders = headers.toMutableMap()
        responseHeaders["Transfer-Encoding"] = "chunked"
        responseHeaders.remove("Content-Length")

        val headerBytes = buildString {
            append("$version $status $statusMessage\r\n")
            responseHeaders.forEach { (name, value) -> append("$name: $value\r\n") }
            append("\r\n")
        }.encodeToByteArray()

        if (body == null || body.isEmpty()) {
            return headerBytes + "0\r\n\r\n".encodeToByteArray()
        }

        val chunkedBody = buildList<ByteArray> {
            var offset = 0
            while (offset < body.size) {
                val currentChunkSize = minOf(chunkSize, body.size - offset)
                add("${currentChunkSize.toString(16)}\r\n".encodeToByteArray())
                add(body.copyOfRange(offset, offset + currentChunkSize))
                add("\r\n".encodeToByteArray())
                offset += currentChunkSize
            }
            add("0\r\n\r\n".encodeToByteArray())
        }

        return headerBytes + chunkedBody.reduce { acc, bytes -> acc + bytes }
    }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is HttpResponse -> false
        else -> status == other.status && statusMessage == other.statusMessage &&
            version == other.version && headers == other.headers &&
            body?.contentEquals(other.body) != false
    }

    override fun hashCode(): Int {
        var result = status
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        fun ok(body: ByteArray? = null, contentType: String = "text/plain") = HttpResponse(
            status = HttpStatus.OK.code, statusMessage = HttpStatus.OK.message,
            headers = mapOf("Content-Type" to contentType) +
                (body?.let { mapOf("Content-Length" to it.size.toString()) } ?: emptyMap()),
            body = body,
        )

        fun ok(body: String, contentType: String = "text/plain; charset=UTF-8") =
            ok(body.encodeToByteArray(), contentType)

        fun json(body: String) = ok(body, "application/json; charset=UTF-8")

        fun notFound(message: String = "Not Found") = HttpResponse(
            status = HttpStatus.NOT_FOUND.code, statusMessage = HttpStatus.NOT_FOUND.message,
            headers = mapOf("Content-Type" to "text/plain"), body = message.encodeToByteArray(),
        )

        fun error(status: HttpStatus, message: String? = null) = HttpResponse(
            status = status.code, statusMessage = status.message,
            headers = mapOf("Content-Type" to "text/plain"),
            body = (message ?: status.message).encodeToByteArray(),
        )

        fun badRequest(message: String = "Bad Request") = error(HttpStatus.BAD_REQUEST, message)
        fun payloadTooLarge(message: String = "Payload Too Large") = error(HttpStatus.PAYLOAD_TOO_LARGE, message)
        fun internalError(message: String = "Internal Server Error") = error(HttpStatus.INTERNAL_SERVER_ERROR, message)
    }
}
