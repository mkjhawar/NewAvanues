package com.augmentalis.httpavanue.client

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.HttpClient
import com.augmentalis.httpavanue.http.HttpClientException
import com.augmentalis.httpavanue.http.HttpMethod
import com.augmentalis.httpavanue.platform.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.BufferedSink
import kotlin.math.min
import kotlin.math.pow

/**
 * Real HTTP client implementation with connection pooling, retries, and redirect following
 */
class RealHttpClient(
    private val config: HttpClientConfig = HttpClientConfig(),
) : HttpClient {
    private val logger = LoggerFactory.getLogger("RealHttpClient")
    private var closed = false
    private val connectionPool = ConnectionPool(config.poolConfig.maxIdleConnections, config.poolConfig.keepAliveDurationMs)

    override suspend fun request(request: ClientRequest): ClientResponse {
        if (closed) throw HttpClientException("Client is closed")
        return executeWithRetryLogic(request)
    }

    override suspend fun get(url: String, headers: Map<String, String>) =
        request(ClientRequest(HttpMethod.GET, url, headers))
    override suspend fun post(url: String, body: ByteArray?, headers: Map<String, String>) =
        request(ClientRequest(HttpMethod.POST, url, headers, body))
    override suspend fun put(url: String, body: ByteArray?, headers: Map<String, String>) =
        request(ClientRequest(HttpMethod.PUT, url, headers, body))
    override suspend fun delete(url: String, headers: Map<String, String>) =
        request(ClientRequest(HttpMethod.DELETE, url, headers))
    override fun close() { closed = true }

    private suspend fun executeWithRetryLogic(request: ClientRequest): ClientResponse {
        var attempt = 0
        var lastException: Exception? = null
        while (attempt < config.retryConfig.maxRetries) {
            try {
                attempt++
                if (attempt > 1) logger.d { "Retry attempt $attempt/${config.retryConfig.maxRetries} for ${request.url}" }
                return executeWithRedirects(request, config.maxRedirects)
            } catch (e: HttpClientException) {
                lastException = e
                if (!isRetryableError(e)) { logger.d { "Non-retryable error: ${e.message}" }; throw e }
                logger.w { "Request attempt $attempt failed: ${e.message}" }
                if (attempt < config.retryConfig.maxRetries) {
                    val backoffDelay = calculateBackoffDelay(attempt)
                    logger.d { "Retrying in ${backoffDelay}ms..." }
                    delay(backoffDelay)
                }
            }
        }
        logger.e { "All $attempt retry attempts exhausted for ${request.url}" }
        throw lastException ?: HttpClientException("Request failed after $attempt attempts")
    }

    private suspend fun executeWithRedirects(request: ClientRequest, maxRedirects: Int): ClientResponse {
        var currentRequest = request
        var redirectCount = 0
        while (true) {
            val response = executeRequest(currentRequest)
            if (response.isRedirect && redirectCount < maxRedirects) {
                val location = response.headers["Location"]
                    ?: throw HttpClientException("Redirect response missing Location header")
                val redirectUrl = resolveUrl(currentRequest.url, location)
                currentRequest = when (response.status) {
                    301, 302, 303 -> ClientRequest(HttpMethod.GET, redirectUrl, currentRequest.headers, null, currentRequest.timeout)
                    307, 308 -> currentRequest.copy(url = redirectUrl)
                    else -> return response
                }
                redirectCount++
                continue
            }
            return response
        }
    }

    private fun isRetryableError(e: HttpClientException): Boolean {
        val message = e.message?.lowercase() ?: ""
        return message.contains("failed to connect") || message.contains("timeout") ||
            message.contains("connection") || message.contains("network") || message.contains("socket")
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = (config.retryConfig.baseDelayMs * 2.0.pow(attempt - 1)).toLong()
        val cappedDelay = min(exponentialDelay, config.retryConfig.maxDelayMs)
        return cappedDelay + (cappedDelay * kotlin.random.Random.nextDouble() * 0.5).toLong()
    }

    private suspend fun executeRequest(request: ClientRequest): ClientResponse {
        val url = parseUrl(request.url)
        var socket: Socket? = null
        try {
            socket = connectionPool.get(url.host, url.port)
            if (socket == null) {
                socket = withTimeout(request.timeout) {
                    Socket.connect(url.host, url.port, SocketConfig(
                        readTimeout = request.timeout, writeTimeout = request.timeout, tls = url.tls))
                }
            }
        } catch (e: Exception) { throw HttpClientException("Failed to connect to ${url.host}:${url.port}", e) }

        return try {
            writeRequest(socket.sink(), request, url)
            val response = ResponseParser.parse(socket.source(), maxBodySize = config.maxBodySize)
            val connectionHeader = response.headers["Connection"]?.lowercase()
            if (connectionHeader != "close") connectionPool.put(socket, url.host, url.port)
            else socket.close()
            response
        } catch (e: Exception) {
            try { socket.close() } catch (_: Exception) {}
            throw HttpClientException("Request failed: ${e.message}", e)
        }
    }

    private suspend fun writeRequest(sink: BufferedSink, request: ClientRequest, url: ParsedUrl) {
        val buffer = Buffer()
        buffer.writeUtf8("${request.method.name} ${url.path} HTTP/1.1\r\n")
        val headers = buildHeaders(request, url)
        headers.forEach { (name, value) -> buffer.writeUtf8("$name: $value\r\n") }
        buffer.writeUtf8("\r\n")
        sink.write(buffer, buffer.size)
        sink.flush()
        request.body?.let { body -> sink.write(body); sink.flush() }
    }

    private fun buildHeaders(request: ClientRequest, url: ParsedUrl): Map<String, String> {
        val headers = request.headers.toMutableMap()
        if ("Host" !in headers) headers["Host"] = if (url.port == url.defaultPort) url.host else "${url.host}:${url.port}"
        if ("User-Agent" !in headers) headers["User-Agent"] = config.userAgent
        if ("Connection" !in headers) headers["Connection"] = "keep-alive"
        request.body?.let { body -> if ("Content-Length" !in headers) headers["Content-Length"] = body.size.toString() }
        return headers
    }

    private fun parseUrl(url: String): ParsedUrl {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd == -1) throw HttpClientException("Invalid URL: $url")
        val scheme = url.substring(0, schemeEnd).lowercase()
        val useTls = scheme == "https"
        val defaultPort = if (useTls) 443 else 80
        val afterScheme = url.substring(schemeEnd + 3)
        val pathStart = afterScheme.indexOf('/')
        val hostPort = if (pathStart == -1) afterScheme else afterScheme.substring(0, pathStart)
        val path = if (pathStart == -1) "/" else afterScheme.substring(pathStart)
        val (host, port) = if (':' in hostPort) {
            val parts = hostPort.split(':'); parts[0] to (parts.getOrNull(1)?.toIntOrNull() ?: defaultPort)
        } else hostPort to defaultPort
        return ParsedUrl(scheme, host, port, path, defaultPort, if (useTls) config.tlsConfig else TlsConfig.disabled())
    }

    private fun resolveUrl(baseUrl: String, location: String): String = when {
        location.startsWith("http://") || location.startsWith("https://") -> location
        location.startsWith("/") -> { val p = parseUrl(baseUrl); "${p.scheme}://${p.host}:${p.port}$location" }
        else -> { val p = parseUrl(baseUrl); val bp = p.path.substringBeforeLast('/'); "${p.scheme}://${p.host}:${p.port}$bp/$location" }
    }
}

data class HttpClientConfig(
    val maxRedirects: Int = 5,
    val maxBodySize: Long = 50 * 1024 * 1024,
    val userAgent: String = "HTTPAvanue/1.0",
    val tlsConfig: TlsConfig = TlsConfig.enabled(),
    val retryConfig: HttpRetryConfig = HttpRetryConfig(),
    val poolConfig: HttpConnectionPoolConfig = HttpConnectionPoolConfig(),
)

data class HttpRetryConfig(val maxRetries: Int = 3, val baseDelayMs: Long = 1000, val maxDelayMs: Long = 10000)
data class HttpConnectionPoolConfig(val maxIdleConnections: Int = 5, val keepAliveDurationMs: Long = 5 * 60 * 1000)

private class ConnectionPool(private val maxIdleConnections: Int, private val keepAliveDurationMs: Long) {
    private data class PooledConnection(val socket: Socket, val host: String, val port: Int, val createdAt: Long, val keepAliveDuration: Long) {
        fun isExpired() = currentTimeMillis() - createdAt > keepAliveDuration
    }
    private val connections = mutableListOf<PooledConnection>()
    private val mutex = Mutex()

    suspend fun get(host: String, port: Int): Socket? = mutex.withLock {
        connections.removeAll { it.isExpired() }
        val index = connections.indexOfFirst { it.host == host && it.port == port }
        if (index >= 0) connections.removeAt(index).socket else null
    }

    suspend fun put(socket: Socket, host: String, port: Int) = mutex.withLock {
        connections.removeAll { it.isExpired() }
        while (connections.size >= maxIdleConnections) {
            try { connections.removeAt(0).socket.close() } catch (_: Exception) {}
        }
        connections.add(PooledConnection(socket, host, port, currentTimeMillis(), keepAliveDurationMs))
    }
}

private data class ParsedUrl(val scheme: String, val host: String, val port: Int, val path: String, val defaultPort: Int, val tls: TlsConfig)
