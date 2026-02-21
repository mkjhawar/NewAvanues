package com.augmentalis.httpavanue.server

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.*
import com.augmentalis.httpavanue.http2.Http2FrameCodec
import com.augmentalis.httpavanue.http2.Http2ServerHandler
import com.augmentalis.httpavanue.http2.Http2Settings
import com.augmentalis.httpavanue.middleware.Middleware
import com.augmentalis.httpavanue.middleware.MiddlewarePipeline
import com.augmentalis.httpavanue.platform.Socket
import com.augmentalis.httpavanue.platform.SocketConfig
import com.augmentalis.httpavanue.platform.SocketServer
import com.augmentalis.httpavanue.routing.Router
import com.augmentalis.httpavanue.websocket.WebSocketHandshake
import kotlinx.coroutines.*

typealias WebSocketHandler = suspend (Socket) -> Unit

data class ServerConfig(
    val port: Int = 8080,
    val host: String = "0.0.0.0",
    val maxConnections: Int = 50,
    val requestTimeout: Long = 30_000,
    val maxRequestBodySize: Long = 10 * 1024 * 1024,
    val socketConfig: SocketConfig = SocketConfig(),
    val http2Enabled: Boolean = true,
    val http2Settings: Http2Settings = Http2Settings(),
)

/**
 * HTTPAvanue HTTP Server — NanoHTTPD-inspired, pure Kotlin multiplatform
 */
class HttpServer(
    private val config: ServerConfig = ServerConfig(),
    private val router: Router = Router(),
) {
    private val logger = LoggerFactory.getLogger("HttpServer")
    private val socketServer = SocketServer(config.socketConfig)
    private var serverJob: Job? = null
    // Thread-safe connection counter — avoids ConcurrentModificationException
    // from invokeOnCompletion callbacks racing with stop() iteration.
    // Child jobs are cancelled automatically via structured concurrency when
    // serverJob is cancelled, so we only need the count for throttling.
    @kotlin.concurrent.Volatile
    private var activeConnectionCount = 0
    private var middlewarePipeline = MiddlewarePipeline.empty()
    private val websocketHandlers = mutableMapOf<String, WebSocketHandler>()

    val port: Int get() = config.port

    fun routes(block: Router.() -> Unit) { router.apply(block) }
    fun use(middleware: Middleware) { middlewarePipeline = middlewarePipeline.add(middleware) }
    fun use(vararg middlewares: Middleware) { middlewarePipeline = middlewarePipeline.addAll(*middlewares) }

    fun websocket(path: String, handler: WebSocketHandler) {
        websocketHandlers[path] = handler
        logger.d { "Registered WebSocket handler for path: $path" }
    }

    fun start() {
        if (serverJob?.isActive == true) {
            logger.w { "Server already running on port ${config.port}" }
            return
        }
        socketServer.bind(config.port)
        logger.i { "Server started on ${config.host}:${config.port}" }

        serverJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    val socket = socketServer.accept()
                    logger.d { "Accepted connection from ${socket.remoteAddress()}" }
                    val connectionJob = launch { handleConnection(socket) }
                    activeConnectionCount++
                    connectionJob.invokeOnCompletion { activeConnectionCount-- }
                    if (activeConnectionCount >= config.maxConnections) {
                        logger.w { "Max connections reached (${config.maxConnections})" }
                        delay(100)
                    }
                } catch (e: CancellationException) {
                    logger.i { "Server shutting down..." }
                    break
                } catch (e: Exception) {
                    val isShutdownException = e::class.simpleName == "SocketException" &&
                        e.message?.contains("Socket closed", ignoreCase = true) == true
                    if (!isShutdownException && socketServer.isBound()) {
                        logger.e({ "Error accepting connection" }, e)
                    } else {
                        logger.d { "Server socket closed during shutdown" }
                    }
                }
            }
        }
    }

    suspend fun stop() {
        logger.i { "Stopping server..." }
        socketServer.close()
        // cancelAndJoin cascades cancellation to all child connection jobs
        // (structured concurrency — children are launched inside serverJob's scope)
        serverJob?.cancelAndJoin()
        serverJob = null
        activeConnectionCount = 0
        logger.i { "Server stopped" }
    }

    private suspend fun handleConnection(socket: Socket) {
        withContext(Dispatchers.IO) {
            var isLongLived = false // WebSocket or HTTP/2 — don't close socket in finally
            try {
                val source = socket.source()

                // ── HTTP/2 prior knowledge detection ──
                // Peek at the first bytes to check for the 24-byte HTTP/2 connection preface.
                // Okio's request(n) fills the internal buffer without consuming; the bytes
                // remain available for the HTTP/1.1 parser if this isn't HTTP/2.
                if (config.http2Enabled) {
                    val prefaceSize = Http2FrameCodec.CONNECTION_PREFACE.size.toLong()
                    if (source.request(prefaceSize)) {
                        val peeked = source.buffer.snapshot().toByteArray()
                        if (Http2ServerHandler.isPriorKnowledgePreface(peeked)) {
                            // Consume the preface bytes so Http2Connection starts at the SETTINGS frame
                            source.skip(prefaceSize)
                            isLongLived = true
                            logger.i { "HTTP/2 prior knowledge from ${socket.remoteAddress()}" }
                            Http2ServerHandler.handlePriorKnowledge(
                                socket = socket,
                                settings = config.http2Settings,
                                requestHandler = { req ->
                                    middlewarePipeline.execute(req) { r -> router.handle(r) }
                                }
                            )
                            return@withContext
                        }
                    }
                }

                // ── HTTP/1.1 path ──
                var websocketRequest: HttpRequest? = null
                withTimeout(config.requestTimeout) {
                    val request = HttpParser.parse(source, config.maxRequestBodySize)
                    logger.d { "${request.method} ${request.uri}" }

                    // Check for WebSocket upgrade
                    if (WebSocketHandshake.isWebSocketRequest(request)) {
                        isLongLived = true
                        websocketRequest = request
                        return@withTimeout
                    }

                    // Check for HTTP/2 h2c upgrade (Upgrade: h2c header)
                    if (config.http2Enabled && Http2ServerHandler.isH2cUpgradeRequest(request)) {
                        isLongLived = true
                        logger.i { "HTTP/2 h2c upgrade from ${socket.remoteAddress()}" }
                        Http2ServerHandler.handleH2cUpgrade(
                            socket = socket,
                            request = request,
                            settings = config.http2Settings,
                            requestHandler = { req ->
                                middlewarePipeline.execute(req) { r -> router.handle(r) }
                            }
                        )
                        return@withTimeout
                    }

                    val response = middlewarePipeline.execute(request) { req -> router.handle(req) }
                    socket.sink().apply { write(response.toBytes()); flush() }
                    if (!request.isKeepAlive) socket.close()
                }
                websocketRequest?.let { request -> handleWebSocketUpgrade(socket, request) }
            } catch (e: TimeoutCancellationException) {
                logger.w { "Request timeout from ${socket.remoteAddress()}" }
                sendError(socket, HttpResponse.error(HttpStatus.GATEWAY_TIMEOUT, "Request timeout"))
            } catch (e: PayloadTooLargeException) {
                logger.w { "Payload too large from ${socket.remoteAddress()}: ${e.message}" }
                sendError(socket, HttpResponse.payloadTooLarge(e.message ?: "Payload Too Large"))
            } catch (e: HttpException) {
                logger.w { "HTTP error: ${e.message}" }
                sendError(socket, HttpResponse.error(HttpStatus.from(e.statusCode), e.message ?: "Bad request"))
            } catch (e: Exception) {
                logger.e({ "Error handling connection" }, e)
                sendError(socket, HttpResponse.internalError())
            } finally {
                if (!isLongLived) socket.close()
            }
        }
    }

    private suspend fun handleWebSocketUpgrade(socket: Socket, request: HttpRequest) {
        val handler = websocketHandlers[request.path]
        if (handler == null) {
            logger.w { "No WebSocket handler registered for path: ${request.path}" }
            sendError(socket, HttpResponse.notFound())
            return
        }
        try {
            val response = WebSocketHandshake.createHandshakeResponse(request)
            socket.sink().apply { write(response.toBytes()); flush() }
            logger.i { "WebSocket upgraded: ${request.path}" }
            socket.setReadTimeout(0)
            handler(socket)
        } catch (e: Exception) {
            logger.e({ "WebSocket upgrade failed" }, e)
            sendError(socket, HttpResponse.badRequest("WebSocket upgrade failed"))
        }
    }

    private suspend fun sendError(socket: Socket, response: HttpResponse) {
        try {
            socket.sink().apply { write(response.toBytes()); flush() }
        } catch (e: Exception) {
            logger.e({ "Error sending error response" }, e)
        }
    }

    fun isRunning() = serverJob?.isActive == true
}

fun httpServer(config: ServerConfig = ServerConfig(), block: Router.() -> Unit) =
    HttpServer(config).apply { routes(block) }
