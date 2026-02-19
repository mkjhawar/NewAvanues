package com.augmentalis.httpavanue.server

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.*
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
    private val activeConnections = mutableSetOf<Job>()
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
                    activeConnections.add(connectionJob)
                    connectionJob.invokeOnCompletion { activeConnections.remove(connectionJob) }
                    if (activeConnections.size >= config.maxConnections) {
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
        serverJob?.cancelAndJoin()
        serverJob = null
        activeConnections.forEach { it.cancel() }
        activeConnections.clear()
        logger.i { "Server stopped" }
    }

    private suspend fun handleConnection(socket: Socket) {
        withContext(Dispatchers.IO) {
            var isWebSocket = false
            var websocketRequest: HttpRequest? = null
            try {
                withTimeout(config.requestTimeout) {
                    val request = HttpParser.parse(socket.source(), config.maxRequestBodySize)
                    logger.d { "${request.method} ${request.uri}" }
                    if (WebSocketHandshake.isWebSocketRequest(request)) {
                        isWebSocket = true
                        websocketRequest = request
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
                sendError(socket, HttpResponse.badRequest(e.message ?: "Bad request"))
            } catch (e: Exception) {
                logger.e({ "Error handling connection" }, e)
                sendError(socket, HttpResponse.internalError())
            } finally {
                if (!isWebSocket) socket.close()
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
