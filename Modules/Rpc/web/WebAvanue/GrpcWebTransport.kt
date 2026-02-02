/**
 * GrpcWebTransport.kt - gRPC-Web transport implementation for browser
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Provides gRPC-Web transport for browser compatibility with HTTP/1.1 fallback.
 */
package com.augmentalis.rpc.web.webavanue

import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.js.Promise

/**
 * Transport configuration for gRPC-Web
 */
data class GrpcWebConfig(
    val host: String,
    val port: Int = 50055,
    val useTls: Boolean = false,
    val timeout: Long = 30000,
    val enableRetry: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000
) {
    val baseUrl: String
        get() = "${if (useTls) "https" else "http"}://$host:$port"
}

/**
 * gRPC-Web transport state
 */
enum class TransportState {
    IDLE,
    CONNECTING,
    READY,
    ERROR,
    CLOSED
}

/**
 * gRPC-Web frame types
 */
private object GrpcWebFrameType {
    const val DATA: Byte = 0x00
    const val TRAILER: Byte = 0x80.toByte()
}

/**
 * gRPC-Web transport implementation for browser environments
 *
 * Supports:
 * - gRPC-Web text format (base64 encoded)
 * - gRPC-Web binary format
 * - HTTP/1.1 and HTTP/2 (where available)
 * - Automatic retry with exponential backoff
 */
class GrpcWebTransport(
    private val config: GrpcWebConfig
) {
    private var state: TransportState = TransportState.IDLE
    private var lastError: Throwable? = null

    val currentState: TransportState
        get() = state

    /**
     * Make a unary RPC call
     */
    suspend fun <Req, Resp> unaryCall(
        serviceName: String,
        methodName: String,
        request: Req,
        serializer: MessageSerializer<Req>,
        deserializer: MessageDeserializer<Resp>
    ): Result<Resp> {
        val path = "/$serviceName/$methodName"
        val requestBytes = serializer.serialize(request)
        val framedRequest = frameMessage(requestBytes)

        return executeWithRetry {
            state = TransportState.CONNECTING

            val response = fetch(
                "${config.baseUrl}$path",
                createRequestInit(framedRequest)
            ).await()

            if (!response.ok) {
                state = TransportState.ERROR
                throw GrpcWebException(
                    code = response.status.toInt(),
                    message = "HTTP error: ${response.statusText}"
                )
            }

            state = TransportState.READY
            val responseBuffer = response.arrayBuffer().await()
            val responseBytes = Uint8Array(responseBuffer)
            val unframedResponse = unframeMessage(responseBytes)

            deserializer.deserialize(unframedResponse)
        }
    }

    /**
     * Make a server streaming RPC call
     */
    fun <Req, Resp> serverStreamingCall(
        serviceName: String,
        methodName: String,
        request: Req,
        serializer: MessageSerializer<Req>,
        deserializer: MessageDeserializer<Resp>
    ): Flow<Resp> = flow {
        val path = "/$serviceName/$methodName"
        val requestBytes = serializer.serialize(request)
        val framedRequest = frameMessage(requestBytes)

        state = TransportState.CONNECTING

        val response = fetch(
            "${config.baseUrl}$path",
            createRequestInit(framedRequest, streaming = true)
        ).await()

        if (!response.ok) {
            state = TransportState.ERROR
            throw GrpcWebException(
                code = response.status.toInt(),
                message = "HTTP error: ${response.statusText}"
            )
        }

        state = TransportState.READY

        val reader = response.body?.getReader()
            ?: throw GrpcWebException(
                code = GrpcStatusCode.INTERNAL,
                message = "No response body"
            )

        var buffer = Uint8Array(0)

        try {
            while (true) {
                val result = readChunk(reader).await()
                if (result.done) break

                result.value?.let { chunk ->
                    buffer = concatArrays(buffer, chunk)

                    while (buffer.length >= 5) {
                        val frameLength = readFrameLength(buffer)
                        val totalLength = 5 + frameLength

                        if (buffer.length < totalLength) break

                        val frameData = extractFrame(buffer, 5, frameLength)
                        buffer = sliceArray(buffer, totalLength)

                        val frameType = buffer[0]
                        if (frameType == GrpcWebFrameType.DATA) {
                            emit(deserializer.deserialize(frameData))
                        }
                    }
                }
            }
        } finally {
            reader.cancel()
            state = TransportState.IDLE
        }
    }

    /**
     * Close the transport
     */
    fun close() {
        state = TransportState.CLOSED
    }

    /**
     * Frame a message with gRPC-Web framing
     */
    private fun frameMessage(data: ByteArray): Uint8Array {
        val framedLength = 5 + data.size
        val framed = Uint8Array(framedLength)

        // Compression flag (0 = uncompressed)
        framed[0] = 0

        // Message length (4 bytes, big-endian)
        framed[1] = ((data.size shr 24) and 0xFF).toByte()
        framed[2] = ((data.size shr 16) and 0xFF).toByte()
        framed[3] = ((data.size shr 8) and 0xFF).toByte()
        framed[4] = (data.size and 0xFF).toByte()

        // Message data
        for (i in data.indices) {
            framed[5 + i] = data[i]
        }

        return framed
    }

    /**
     * Unframe a gRPC-Web response
     */
    private fun unframeMessage(data: Uint8Array): ByteArray {
        if (data.length < 5) {
            throw GrpcWebException(
                code = GrpcStatusCode.INTERNAL,
                message = "Response too short"
            )
        }

        val messageLength = readFrameLength(data)
        if (data.length < 5 + messageLength) {
            throw GrpcWebException(
                code = GrpcStatusCode.INTERNAL,
                message = "Incomplete response"
            )
        }

        return extractFrame(data, 5, messageLength)
    }

    private fun readFrameLength(data: Uint8Array): Int {
        return ((data[1].toInt() and 0xFF) shl 24) or
                ((data[2].toInt() and 0xFF) shl 16) or
                ((data[3].toInt() and 0xFF) shl 8) or
                (data[4].toInt() and 0xFF)
    }

    private fun extractFrame(data: Uint8Array, offset: Int, length: Int): ByteArray {
        val result = ByteArray(length)
        for (i in 0 until length) {
            result[i] = data[offset + i]
        }
        return result
    }

    private fun sliceArray(data: Uint8Array, start: Int): Uint8Array {
        val result = Uint8Array(data.length - start)
        for (i in start until data.length) {
            result[i - start] = data[i]
        }
        return result
    }

    private fun concatArrays(a: Uint8Array, b: Uint8Array): Uint8Array {
        val result = Uint8Array(a.length + b.length)
        for (i in 0 until a.length) {
            result[i] = a[i]
        }
        for (i in 0 until b.length) {
            result[a.length + i] = b[i]
        }
        return result
    }

    private fun createRequestInit(body: Uint8Array, streaming: Boolean = false): RequestInit {
        val headers = Headers()
        headers.append("Content-Type", "application/grpc-web+proto")
        headers.append("X-Grpc-Web", "1")
        headers.append("X-User-Agent", "grpc-web-kotlin/1.0")

        if (streaming) {
            headers.append("Accept", "application/grpc-web+proto")
        }

        return js("({})").unsafeCast<RequestInit>().apply {
            method = "POST"
            this.headers = headers
            this.body = body
            mode = org.w3c.fetch.RequestMode.CORS
            credentials = org.w3c.fetch.RequestCredentials.SAME_ORIGIN
        }
    }

    private suspend fun <T> executeWithRetry(block: suspend () -> T): Result<T> {
        var lastException: Throwable? = null
        var delay = config.retryDelayMs

        repeat(if (config.enableRetry) config.maxRetries else 1) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Throwable) {
                lastException = e
                lastError = e

                if (!config.enableRetry || attempt == config.maxRetries - 1) {
                    return Result.failure(e)
                }

                // Exponential backoff
                kotlinx.coroutines.delay(delay)
                delay *= 2
            }
        }

        return Result.failure(lastException ?: GrpcWebException(
            code = GrpcStatusCode.UNKNOWN,
            message = "Unknown error"
        ))
    }

    companion object {
        /**
         * Create transport with default WebAvanue configuration
         */
        fun createDefault(host: String = "localhost"): GrpcWebTransport {
            return GrpcWebTransport(
                GrpcWebConfig(
                    host = host,
                    port = 50055
                )
            )
        }
    }
}

/**
 * gRPC status codes
 */
object GrpcStatusCode {
    const val OK = 0
    const val CANCELLED = 1
    const val UNKNOWN = 2
    const val INVALID_ARGUMENT = 3
    const val DEADLINE_EXCEEDED = 4
    const val NOT_FOUND = 5
    const val ALREADY_EXISTS = 6
    const val PERMISSION_DENIED = 7
    const val RESOURCE_EXHAUSTED = 8
    const val FAILED_PRECONDITION = 9
    const val ABORTED = 10
    const val OUT_OF_RANGE = 11
    const val UNIMPLEMENTED = 12
    const val INTERNAL = 13
    const val UNAVAILABLE = 14
    const val DATA_LOSS = 15
    const val UNAUTHENTICATED = 16
}

/**
 * gRPC-Web exception
 */
class GrpcWebException(
    val code: Int,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

/**
 * Message serializer interface
 */
interface MessageSerializer<T> {
    fun serialize(message: T): ByteArray
}

/**
 * Message deserializer interface
 */
interface MessageDeserializer<T> {
    fun deserialize(data: ByteArray): T
}

/**
 * Streaming read result
 */
external interface ReadResult {
    val done: Boolean
    val value: Uint8Array?
}

/**
 * External fetch function declaration
 */
external fun fetch(url: String, init: RequestInit): Promise<Response>

/**
 * Read chunk from reader
 */
private fun readChunk(reader: dynamic): Promise<ReadResult> {
    return reader.read().unsafeCast<Promise<ReadResult>>()
}
