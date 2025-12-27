# Chapter 31: AVA & AVAConnect Integration

## Overview

AVAConnect is a Kotlin Multiplatform networking library that provides the connectivity layer between VOS4 and the AVA AI platform. It enables bidirectional communication, remote device control, WebRTC video/audio streaming, and cross-platform integration. This chapter explores AVAConnect's architecture, integration with VOS4, and real-world usage scenarios.

**Location:** `/Volumes/M-Drive/Coding/AVAConnect/`

**Key Capabilities:**
- HTTP/WebSocket server and client (100% complete)
- WebRTC peer-to-peer video/audio (Android + iOS)
- Remote UI control via AccessibilityService
- Device pairing and authentication (QR/PIN codes)
- Service discovery (mDNS/Bonjour)
- TLS/HTTPS security with certificates
- Multi-platform: Android (100%), iOS (100%), JVM (100%)
- Code sharing: 88% common code across platforms

**Project Stats:**
- **Files:** 666 Kotlin files
- **Lines of Code:** 51,000+
- **Modules:** 29 core modules
- **Version:** 0.8.5-beta (85% production-ready)
- **Architecture:** Kotlin Multiplatform (KMP)

---

## 31.1 AVAConnect Architecture

### 31.1.1 System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    AVAConnect Library                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         HTTP/WebSocket Layer                         │  │
│  │  - HttpServer (middleware pipeline)                  │  │
│  │  - HttpClient (platform abstraction)                 │  │
│  │  - WebSocketServer (RFC 6455)                        │  │
│  │  - WebSocketClient                                   │  │
│  └──────────────────────────────────────────────────────┘  │
│                        ↓                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         WebRTC Layer (Android/iOS)                   │  │
│  │  - WebRTCPeerConnection                              │  │
│  │  - Media streams (audio/video)                       │  │
│  │  - Data channels                                     │  │
│  │  - STUN/TURN servers                                 │  │
│  └──────────────────────────────────────────────────────┘  │
│                        ↓                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Remote Control Layer                         │  │
│  │  - AccessibilityService integration                  │  │
│  │  - UI scraping                                       │  │
│  │  - Action dispatch (tap, swipe, type)                │  │
│  └──────────────────────────────────────────────────────┘  │
│                        ↓                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Security Layer                               │  │
│  │  - Device pairing (QR/PIN)                           │  │
│  │  - TLS/HTTPS                                         │  │
│  │  - Certificate management                            │  │
│  │  - Authentication tokens                             │  │
│  └──────────────────────────────────────────────────────┘  │
│                        ↓                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         Discovery Layer                              │  │
│  │  - mDNS/Bonjour service discovery                    │  │
│  │  - Device registration                               │  │
│  │  - Network scanning                                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 31.1.2 Module Structure

AVAConnect consists of 29 core modules organized by functionality:

**HTTP & Networking (6 modules):**
- `http` - HTTP/1.1 protocol implementation
- `routing` - URL routing with path parameters
- `middleware` - HTTP middleware pipeline
- `client` - HTTP client implementation
- `websocket` - WebSocket protocol (RFC 6455)
- `server` - HTTP/WebSocket server orchestration

**WebRTC & Real-Time Communication (4 modules):**
- `webrtc` - WebRTC peer connections (Android + iOS)
- `signaling` - WebRTC signaling protocols
- `media` - Audio/video stream management
- `codec` - Codec negotiation and management

**Remote Control & UI Access (4 modules):**
- `actions` - Remote control action models
- `accessibility` - AccessibilityService integration
- `gesture` - Touch gesture synthesis
- `uiautomator` - UI scraping and inspection

**Security & Authentication (5 modules):**
- `security` - TLS/certificate management
- `pairing` - Device pairing (QR/PIN)
- `auth` - Authentication/authorization
- `crypto` - Cryptographic utilities
- `trust` - Device trust store

**Discovery & Transport (4 modules):**
- `discovery` - Service discovery (mDNS/Bonjour)
- `transport` - Transport layer abstractions
- `device` - Device management
- `network` - Network utilities

**Other (6 modules):**
- `platform` - Platform-specific implementations
- `sync` - Cross-device synchronization
- `storage` - Persistent storage
- `logging` - Structured logging
- `errors` - Error handling
- `models` - Data models

---

## 31.2 HTTP & WebSocket Communication

### 31.2.1 HTTP Server

AVAConnect provides a full HTTP/1.1 server with middleware pipeline:

```kotlin
// File: shared/src/commonMain/kotlin/com/augmentalis/avaconnect/server/HttpServer.kt

class HttpServer(private val config: ServerConfig) {
    private val router = Router()
    private val middlewarePipeline = mutableListOf<Middleware>()

    /**
     * Add middleware to the pipeline
     */
    fun use(middleware: Middleware) {
        middlewarePipeline.add(middleware)
    }

    /**
     * Register a route handler
     */
    fun route(method: HttpMethod, path: String, handler: RouteHandler) {
        router.register(method, path, handler)
    }

    /**
     * Start the server
     */
    suspend fun start() {
        val socket = Socket.bind(config.port)

        println("Server listening on port ${config.port}")

        while (true) {
            val connection = socket.accept()

            // Handle connection asynchronously
            launch {
                handleConnection(connection)
            }
        }
    }

    private suspend fun handleConnection(connection: Connection) {
        try {
            // Read HTTP request
            val request = readRequest(connection)

            // Apply middleware pipeline
            var response = applyMiddleware(request)

            // Route to handler if not handled by middleware
            if (response == null) {
                val handler = router.match(request.method, request.path)
                response = handler?.invoke(request) ?: HttpResponse.notFound()
            }

            // Write response
            writeResponse(connection, response)
        } catch (e: Exception) {
            val errorResponse = HttpResponse.internalServerError(e.message ?: "Unknown error")
            writeResponse(connection, errorResponse)
        } finally {
            connection.close()
        }
    }

    private suspend fun applyMiddleware(request: HttpRequest): HttpResponse? {
        var currentResponse: HttpResponse? = null

        for (middleware in middlewarePipeline) {
            val result = middleware.handle(request, currentResponse)
            if (result != null) {
                currentResponse = result
                // If middleware returns a response, stop processing
                if (middleware.isTerminal) {
                    break
                }
            }
        }

        return currentResponse
    }
}

data class ServerConfig(
    val port: Int = 8080,
    val enableTls: Boolean = false,
    val tlsCertificate: String? = null,
    val tlsPrivateKey: String? = null,
    val readTimeout: Duration = 30.seconds,
    val writeTimeout: Duration = 30.seconds,
    val maxConnections: Int = 1000
)
```

### 31.2.2 HTTP Client

Cross-platform HTTP client with platform-specific implementations:

```kotlin
// File: shared/src/commonMain/kotlin/com/augmentalis/avaconnect/client/HttpClient.kt

interface HttpClient {
    suspend fun execute(request: HttpRequest): HttpResponse
}

class RealHttpClient : HttpClient {
    override suspend fun execute(request: HttpRequest): HttpResponse {
        return withContext(Dispatchers.IO) {
            // Connect to server
            val socket = Socket.connect(request.host, request.port)

            try {
                // Write request
                socket.write(request.toByteArray())

                // Read response
                val responseData = socket.readAll()
                parseResponse(responseData)
            } finally {
                socket.close()
            }
        }
    }

    private fun parseResponse(data: ByteArray): HttpResponse {
        val responseText = data.decodeToString()
        val lines = responseText.lines()

        // Parse status line
        val statusLine = lines[0].split(" ")
        val statusCode = statusLine[1].toInt()

        // Parse headers
        val headers = mutableMapOf<String, String>()
        var bodyStartIndex = 1

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                bodyStartIndex = i + 1
                break
            }

            val (key, value) = line.split(": ", limit = 2)
            headers[key] = value
        }

        // Parse body
        val body = lines.drop(bodyStartIndex).joinToString("\n")

        return HttpResponse(
            status = HttpStatus.fromCode(statusCode),
            headers = headers,
            body = body
        )
    }
}

// Extension functions for convenience
suspend fun HttpClient.get(url: String): HttpResponse {
    return execute(HttpRequest(method = HttpMethod.GET, url = url))
}

suspend fun HttpClient.post(url: String, body: String): HttpResponse {
    return execute(
        HttpRequest(
            method = HttpMethod.POST,
            url = url,
            headers = mapOf("Content-Type" to "application/json"),
            body = body
        )
    )
}
```

### 31.2.3 WebSocket Server

WebSocket server implementation supporting RFC 6455:

```kotlin
// File: shared/src/commonMain/kotlin/com/augmentalis/avaconnect/websocket/WebSocketServer.kt

class WebSocketServer(private val config: ServerConfig) {
    private val connections = mutableListOf<WebSocketConnection>()
    private val handlers = mutableMapOf<String, WebSocketHandler>()

    /**
     * Register WebSocket route
     */
    fun route(path: String, handler: WebSocketHandler) {
        handlers[path] = handler
    }

    /**
     * Start WebSocket server
     */
    suspend fun start() {
        val socket = Socket.bind(config.port)

        println("WebSocket server listening on port ${config.port}")

        while (true) {
            val connection = socket.accept()

            launch {
                handleWebSocketUpgrade(connection)
            }
        }
    }

    private suspend fun handleWebSocketUpgrade(connection: Connection) {
        // Read HTTP upgrade request
        val request = readRequest(connection)

        // Validate WebSocket upgrade headers
        if (!isWebSocketUpgrade(request)) {
            val response = HttpResponse.badRequest("Not a WebSocket upgrade")
            writeResponse(connection, response)
            connection.close()
            return
        }

        // Perform WebSocket handshake
        val handshake = WebSocketHandshake.fromRequest(request)
        val response = handshake.createResponse()
        writeResponse(connection, response)

        // Create WebSocket connection
        val wsConnection = WebSocketConnection(connection, request.path)
        connections.add(wsConnection)

        // Get handler for this path
        val handler = handlers[request.path]

        if (handler != null) {
            // Notify handler of new connection
            handler.onConnect(wsConnection)

            // Read frames
            while (wsConnection.isOpen) {
                try {
                    val frame = readFrame(connection)

                    when (frame.opcode) {
                        WebSocketOpcode.TEXT -> {
                            val text = frame.payload.decodeToString()
                            handler.onMessage(wsConnection, text)
                        }
                        WebSocketOpcode.BINARY -> {
                            handler.onBinary(wsConnection, frame.payload)
                        }
                        WebSocketOpcode.CLOSE -> {
                            handler.onClose(wsConnection)
                            wsConnection.close()
                            break
                        }
                        WebSocketOpcode.PING -> {
                            // Send pong
                            val pong = WebSocketFrame(
                                opcode = WebSocketOpcode.PONG,
                                payload = frame.payload
                            )
                            writeFrame(connection, pong)
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    handler.onError(wsConnection, e)
                    wsConnection.close()
                    break
                }
            }
        }

        connections.remove(wsConnection)
    }

    private fun isWebSocketUpgrade(request: HttpRequest): Boolean {
        return request.headers["Upgrade"]?.equals("websocket", ignoreCase = true) == true &&
                request.headers["Connection"]?.contains("Upgrade", ignoreCase = true) == true
    }

    /**
     * Broadcast message to all connections
     */
    suspend fun broadcast(message: String) {
        connections.forEach { connection ->
            connection.send(message)
        }
    }
}

interface WebSocketHandler {
    suspend fun onConnect(connection: WebSocketConnection)
    suspend fun onMessage(connection: WebSocketConnection, message: String)
    suspend fun onBinary(connection: WebSocketConnection, data: ByteArray)
    suspend fun onClose(connection: WebSocketConnection)
    suspend fun onError(connection: WebSocketConnection, error: Exception)
}
```

---

## 31.3 WebRTC Integration

### 31.3.1 WebRTC Peer Connection

AVAConnect uses `webrtc-kmp` library for Android and iOS WebRTC support:

```kotlin
// File: shared/src/commonMain/kotlin/com/augmentalis/avaconnect/webrtc/WebRTCPeerConnection.kt

interface WebRTCPeerConnection {
    /**
     * Create an offer for establishing connection
     */
    suspend fun createOffer(): SessionDescription

    /**
     * Create an answer to a received offer
     */
    suspend fun createAnswer(): SessionDescription

    /**
     * Set local session description
     */
    suspend fun setLocalDescription(sdp: SessionDescription)

    /**
     * Set remote session description
     */
    suspend fun setRemoteDescription(sdp: SessionDescription)

    /**
     * Add ICE candidate
     */
    suspend fun addIceCandidate(candidate: IceCandidate)

    /**
     * Add media stream
     */
    fun addStream(stream: MediaStream)

    /**
     * Get connection state
     */
    fun getState(): PeerConnectionState

    /**
     * Close connection
     */
    fun close()
}

// Platform-specific implementation (Android/iOS)
// File: shared/src/androidMain/kotlin/com/augmentalis/avaconnect/webrtc/WebRTCPeerConnectionImpl.kt

class WebRTCPeerConnectionImpl(
    private val config: WebRTCConfig,
    private val signaler: WebRTCSignaler
) : WebRTCPeerConnection {

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        createPeerConnectionFactory()
    }

    private var peerConnection: com.shepeliev.webrtckmp.PeerConnection? = null
    private val iceServers = listOf(
        IceServer(urls = listOf("stun:stun.l.google.com:19302"))
    )

    init {
        createPeerConnection()
    }

    private fun createPeerConnection() {
        peerConnection = peerConnectionFactory.createPeerConnection(
            iceServers = iceServers,
            onIceCandidate = { candidate ->
                // Send ICE candidate via signaling
                signaler.sendIceCandidate(candidate)
            },
            onConnectionStateChange = { state ->
                println("Connection state: $state")
            },
            onTrack = { track ->
                println("Track received: ${track.kind}")
            }
        )
    }

    override suspend fun createOffer(): SessionDescription {
        val offer = peerConnection?.createOffer() ?: throw IllegalStateException("No peer connection")

        return SessionDescription(
            type = SessionDescriptionType.OFFER,
            sdp = offer.sdp
        )
    }

    override suspend fun createAnswer(): SessionDescription {
        val answer = peerConnection?.createAnswer() ?: throw IllegalStateException("No peer connection")

        return SessionDescription(
            type = SessionDescriptionType.ANSWER,
            sdp = answer.sdp
        )
    }

    override suspend fun setLocalDescription(sdp: SessionDescription) {
        val sessionDescription = com.shepeliev.webrtckmp.SessionDescription(
            type = when (sdp.type) {
                SessionDescriptionType.OFFER -> com.shepeliev.webrtckmp.SdpType.OFFER
                SessionDescriptionType.ANSWER -> com.shepeliev.webrtckmp.SdpType.ANSWER
            },
            sdp = sdp.sdp
        )

        peerConnection?.setLocalDescription(sessionDescription)
    }

    override suspend fun setRemoteDescription(sdp: SessionDescription) {
        val sessionDescription = com.shepeliev.webrtckmp.SessionDescription(
            type = when (sdp.type) {
                SessionDescriptionType.OFFER -> com.shepeliev.webrtckmp.SdpType.OFFER
                SessionDescriptionType.ANSWER -> com.shepeliev.webrtckmp.SdpType.ANSWER
            },
            sdp = sdp.sdp
        )

        peerConnection?.setRemoteDescription(sessionDescription)
    }

    override suspend fun addIceCandidate(candidate: IceCandidate) {
        val iceCandidate = com.shepeliev.webrtckmp.IceCandidate(
            sdpMid = candidate.sdpMid,
            sdpMLineIndex = candidate.sdpMLineIndex,
            sdp = candidate.candidate
        )

        peerConnection?.addIceCandidate(iceCandidate)
    }

    override fun addStream(stream: MediaStream) {
        stream.audioTracks.forEach { track ->
            peerConnection?.addTrack(track)
        }

        stream.videoTracks.forEach { track ->
            peerConnection?.addTrack(track)
        }
    }

    override fun getState(): PeerConnectionState {
        return when (peerConnection?.connectionState) {
            com.shepeliev.webrtckmp.PeerConnectionState.NEW -> PeerConnectionState.NEW
            com.shepeliev.webrtckmp.PeerConnectionState.CONNECTING -> PeerConnectionState.CONNECTING
            com.shepeliev.webrtckmp.PeerConnectionState.CONNECTED -> PeerConnectionState.CONNECTED
            com.shepeliev.webrtckmp.PeerConnectionState.DISCONNECTED -> PeerConnectionState.DISCONNECTED
            com.shepeliev.webrtckmp.PeerConnectionState.FAILED -> PeerConnectionState.FAILED
            com.shepeliev.webrtckmp.PeerConnectionState.CLOSED -> PeerConnectionState.CLOSED
            else -> PeerConnectionState.NEW
        }
    }

    override fun close() {
        peerConnection?.close()
        peerConnection = null
    }
}

data class SessionDescription(
    val type: SessionDescriptionType,
    val sdp: String
)

enum class SessionDescriptionType {
    OFFER, ANSWER
}

data class IceCandidate(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String
)

enum class PeerConnectionState {
    NEW, CONNECTING, CONNECTED, DISCONNECTED, FAILED, CLOSED
}
```

### 31.3.2 WebRTC Signaling

Signaling protocol for WebRTC connection establishment:

```kotlin
// File: shared/src/commonMain/kotlin/com/augmentalis/avaconnect/signaling/WebRTCSignaler.kt

interface WebRTCSignaler {
    /**
     * Send offer to remote peer
     */
    suspend fun sendOffer(peerId: String, offer: SessionDescription)

    /**
     * Send answer to remote peer
     */
    suspend fun sendAnswer(peerId: String, answer: SessionDescription)

    /**
     * Send ICE candidate to remote peer
     */
    suspend fun sendIceCandidate(candidate: IceCandidate)

    /**
     * Register listener for signaling events
     */
    fun setListener(listener: SignalingListener)
}

interface SignalingListener {
    suspend fun onOffer(peerId: String, offer: SessionDescription)
    suspend fun onAnswer(peerId: String, answer: SessionDescription)
    suspend fun onIceCandidate(peerId: String, candidate: IceCandidate)
}

class WebSocketSignaler(private val websocket: WebSocketClient) : WebRTCSignaler {
    private var listener: SignalingListener? = null

    init {
        websocket.setHandler(object : WebSocketHandler {
            override suspend fun onMessage(connection: WebSocketConnection, message: String) {
                handleSignalingMessage(message)
            }

            // ... other WebSocket handler methods
        })
    }

    override suspend fun sendOffer(peerId: String, offer: SessionDescription) {
        val message = SignalingMessage.Offer(
            peerId = peerId,
            sdp = offer.sdp
        )
        websocket.send(Json.encodeToString(message))
    }

    override suspend fun sendAnswer(peerId: String, answer: SessionDescription) {
        val message = SignalingMessage.Answer(
            peerId = peerId,
            sdp = answer.sdp
        )
        websocket.send(Json.encodeToString(message))
    }

    override suspend fun sendIceCandidate(candidate: IceCandidate) {
        val message = SignalingMessage.IceCandidate(
            candidate = candidate.candidate,
            sdpMid = candidate.sdpMid,
            sdpMLineIndex = candidate.sdpMLineIndex
        )
        websocket.send(Json.encodeToString(message))
    }

    override fun setListener(listener: SignalingListener) {
        this.listener = listener
    }

    private suspend fun handleSignalingMessage(message: String) {
        val signalingMessage = Json.decodeFromString<SignalingMessage>(message)

        when (signalingMessage) {
            is SignalingMessage.Offer -> {
                val offer = SessionDescription(
                    type = SessionDescriptionType.OFFER,
                    sdp = signalingMessage.sdp
                )
                listener?.onOffer(signalingMessage.peerId, offer)
            }
            is SignalingMessage.Answer -> {
                val answer = SessionDescription(
                    type = SessionDescriptionType.ANSWER,
                    sdp = signalingMessage.sdp
                )
                listener?.onAnswer(signalingMessage.peerId, answer)
            }
            is SignalingMessage.IceCandidate -> {
                val candidate = IceCandidate(
                    sdpMid = signalingMessage.sdpMid,
                    sdpMLineIndex = signalingMessage.sdpMLineIndex,
                    candidate = signalingMessage.candidate
                )
                listener?.onIceCandidate(signalingMessage.peerId, candidate)
            }
        }
    }
}

@Serializable
sealed class SignalingMessage {
    @Serializable
    @SerialName("offer")
    data class Offer(val peerId: String, val sdp: String) : SignalingMessage()

    @Serializable
    @SerialName("answer")
    data class Answer(val peerId: String, val sdp: String) : SignalingMessage()

    @Serializable
    @SerialName("ice-candidate")
    data class IceCandidate(
        val peerId: String,
        val candidate: String,
        val sdpMid: String,
        val sdpMLineIndex: Int
    ) : SignalingMessage()
}
```

---

## 31.4 Remote Control Integration

### 31.4.1 Accessibility-Based Control

AVAConnect integrates with Android's AccessibilityService for remote UI control:

```kotlin
// File: shared/src/androidMain/kotlin/com/augmentalis/avaconnect/accessibility/AccessibilityProvider.kt

interface AccessibilityProvider {
    /**
     * Perform action on remote device
     */
    suspend fun performAction(action: RemoteAction): ActionResult

    /**
     * Get current UI hierarchy
     */
    suspend fun getUIHierarchy(): UIHierarchy

    /**
     * Find element by criteria
     */
    suspend fun findElement(criteria: ElementCriteria): UIElement?
}

class AndroidAccessibilityProvider(
    private val accessibilityService: AccessibilityService
) : AccessibilityProvider {

    override suspend fun performAction(action: RemoteAction): ActionResult {
        return when (action) {
            is RemoteAction.Tap -> performTap(action.x, action.y)
            is RemoteAction.Swipe -> performSwipe(action.startX, action.startY, action.endX, action.endY)
            is RemoteAction.Type -> performType(action.text)
            is RemoteAction.PressKey -> performKeyPress(action.keyCode)
            is RemoteAction.ScrollTo -> performScrollTo(action.elementId)
            else -> ActionResult.Unsupported
        }
    }

    private suspend fun performTap(x: Float, y: Float): ActionResult {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        return withContext(Dispatchers.Main) {
            accessibilityService.dispatchGesture(
                gestureDescription,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        // Success
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        // Cancelled
                    }
                },
                null
            )

            ActionResult.Success
        }
    }

    private suspend fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ): ActionResult {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()

        return withContext(Dispatchers.Main) {
            accessibilityService.dispatchGesture(gestureDescription, null, null)
            ActionResult.Success
        }
    }

    private suspend fun performType(text: String): ActionResult {
        val arguments = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }

        val rootNode = accessibilityService.rootInActiveWindow
        val focusedNode = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        return if (focusedNode != null) {
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            ActionResult.Success
        } else {
            ActionResult.Failed("No focused input field")
        }
    }

    override suspend fun getUIHierarchy(): UIHierarchy {
        val rootNode = accessibilityService.rootInActiveWindow ?: return UIHierarchy.empty()

        return UIHierarchy(
            root = convertNodeToElement(rootNode)
        )
    }

    private fun convertNodeToElement(node: AccessibilityNodeInfo): UIElement {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        return UIElement(
            id = node.hashCode().toString(),
            className = node.className?.toString() ?: "",
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            bounds = ElementBounds(
                left = bounds.left,
                top = bounds.top,
                right = bounds.right,
                bottom = bounds.bottom
            ),
            clickable = node.isClickable,
            focusable = node.isFocusable,
            enabled = node.isEnabled,
            children = (0 until node.childCount).map { index ->
                convertNodeToElement(node.getChild(index))
            }
        )
    }

    override suspend fun findElement(criteria: ElementCriteria): UIElement? {
        val hierarchy = getUIHierarchy()
        return hierarchy.root.findElement(criteria)
    }
}

data class UIHierarchy(val root: UIElement) {
    companion object {
        fun empty() = UIHierarchy(UIElement.empty())
    }
}

data class UIElement(
    val id: String,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val bounds: ElementBounds,
    val clickable: Boolean,
    val focusable: Boolean,
    val enabled: Boolean,
    val children: List<UIElement>
) {
    fun findElement(criteria: ElementCriteria): UIElement? {
        if (matches(criteria)) {
            return this
        }

        for (child in children) {
            val found = child.findElement(criteria)
            if (found != null) {
                return found
            }
        }

        return null
    }

    private fun matches(criteria: ElementCriteria): Boolean {
        return when (criteria) {
            is ElementCriteria.ById -> id == criteria.id
            is ElementCriteria.ByText -> text == criteria.text
            is ElementCriteria.ByClassName -> className == criteria.className
            is ElementCriteria.ByContentDescription -> contentDescription == criteria.contentDescription
        }
    }

    companion object {
        fun empty() = UIElement(
            id = "",
            className = "",
            text = null,
            contentDescription = null,
            bounds = ElementBounds(0, 0, 0, 0),
            clickable = false,
            focusable = false,
            enabled = false,
            children = emptyList()
        )
    }
}

data class ElementBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

sealed class ElementCriteria {
    data class ById(val id: String) : ElementCriteria()
    data class ByText(val text: String) : ElementCriteria()
    data class ByClassName(val className: String) : ElementCriteria()
    data class ByContentDescription(val contentDescription: String) : ElementCriteria()
}

sealed class RemoteAction {
    data class Tap(val x: Float, val y: Float) : RemoteAction()
    data class Swipe(val startX: Float, val startY: Float, val endX: Float, val endY: Float) : RemoteAction()
    data class Type(val text: String) : RemoteAction()
    data class PressKey(val keyCode: Int) : RemoteAction()
    data class ScrollTo(val elementId: String) : RemoteAction()
}

sealed class ActionResult {
    object Success : ActionResult()
    data class Failed(val reason: String) : ActionResult()
    object Unsupported : ActionResult()
}
```

---

## 31.5 Device Pairing & Security

### 31.5.1 QR Code Pairing

```kotlin
// File: shared/src/commonMain/kotlin/com/augmentalis/avaconnect/pairing/DevicePairing.kt

class DevicePairing {
    /**
     * Generate pairing code
     */
    fun generatePairingCode(): PairingCode {
        val deviceId = UUID.randomUUID().toString()
        val secret = generateSecureRandom(32)
        val timestamp = Clock.System.now().toEpochMilliseconds()

        return PairingCode(
            deviceId = deviceId,
            secret = secret,
            timestamp = timestamp,
            expiresIn = 300_000 // 5 minutes
        )
    }

    /**
     * Generate QR code data from pairing code
     */
    fun generateQRCode(pairingCode: PairingCode): String {
        val data = mapOf(
            "deviceId" to pairingCode.deviceId,
            "secret" to pairingCode.secret.encodeBase64(),
            "timestamp" to pairingCode.timestamp,
            "version" to "1.0"
        )

        return Json.encodeToString(data)
    }

    /**
     * Pair devices using pairing code
     */
    suspend fun pairDevices(
        localDevice: Device,
        pairingCode: PairingCode
    ): PairingResult {
        // Verify pairing code is not expired
        val now = Clock.System.now().toEpochMilliseconds()
        if (now > pairingCode.timestamp + pairingCode.expiresIn) {
            return PairingResult.Expired
        }

        // Generate shared secret using ECDH
        val sharedSecret = generateSharedSecret(
            localDevice.privateKey,
            pairingCode.secret
        )

        // Store device pairing
        val pairedDevice = PairedDevice(
            deviceId = pairingCode.deviceId,
            sharedSecret = sharedSecret,
            pairedAt = now,
            trustedLevel = TrustLevel.VERIFIED
        )

        return PairingResult.Success(pairedDevice)
    }
}

data class PairingCode(
    val deviceId: String,
    val secret: ByteArray,
    val timestamp: Long,
    val expiresIn: Long
)

data class PairedDevice(
    val deviceId: String,
    val sharedSecret: ByteArray,
    val pairedAt: Long,
    val trustedLevel: TrustLevel
)

enum class TrustLevel {
    UNVERIFIED, VERIFIED, TRUSTED
}

sealed class PairingResult {
    data class Success(val device: PairedDevice) : PairingResult()
    object Expired : PairingResult()
    data class Failed(val reason: String) : PairingResult()
}
```

---

## 31.6 VOS4 Integration

### 31.6.1 Integration Architecture

```
VOS4 (Android App)
    ↓
┌───────────────────────────────────────┐
│   AVAConnect Client Integration      │
├───────────────────────────────────────┤
│                                       │
│  1. HTTP Client                       │
│     - API calls to AVA platform       │
│     - File upload/download            │
│                                       │
│  2. WebSocket Client                  │
│     - Real-time command channel       │
│     - Bidirectional messaging         │
│                                       │
│  3. WebRTC Peer                       │
│     - Video/audio streaming           │
│     - Screen sharing                  │
│                                       │
│  4. Remote Control Provider           │
│     - Accessibility actions           │
│     - UI scraping                     │
│                                       │
│  5. Device Pairing                    │
│     - QR code scanning                │
│     - PIN verification                │
│                                       │
└───────────────────────────────────────┘
    ↓
AVA Platform (Server)
```

### 31.6.2 VOS4 Client Implementation

```kotlin
// File: VOS4/modules/apps/VoiceOSCore/src/main/kotlin/com/augmentalis/voiceoscore/ava/AVAClient.kt

object AVAClient {
    private val httpClient = RealHttpClient()
    private var websocket: WebSocketClient? = null
    private var webrtcPeer: WebRTCPeerConnection? = null

    /**
     * Connect to AVA platform
     */
    suspend fun connect(serverUrl: String, deviceId: String) {
        // Establish WebSocket connection
        websocket = WebSocketClient(serverUrl)
        websocket?.connect()

        // Authenticate
        authenticate(deviceId)

        // Set up WebRTC for video/audio
        setupWebRTC()
    }

    private suspend fun authenticate(deviceId: String) {
        val authRequest = mapOf(
            "type" to "auth",
            "deviceId" to deviceId,
            "timestamp" to Clock.System.now().toEpochMilliseconds()
        )

        websocket?.send(Json.encodeToString(authRequest))
    }

    private suspend fun setupWebRTC() {
        val config = WebRTCConfig(
            iceServers = listOf(
                IceServer(urls = listOf("stun:stun.l.google.com:19302"))
            )
        )

        val signaler = WebSocketSignaler(websocket!!)
        webrtcPeer = WebRTCPeerConnectionImpl(config, signaler)

        // Set up signaling listener
        signaler.setListener(object : SignalingListener {
            override suspend fun onOffer(peerId: String, offer: SessionDescription) {
                // Received offer from AVA
                webrtcPeer?.setRemoteDescription(offer)
                val answer = webrtcPeer?.createAnswer()
                webrtcPeer?.setLocalDescription(answer!!)
                signaler.sendAnswer(peerId, answer!!)
            }

            override suspend fun onAnswer(peerId: String, answer: SessionDescription) {
                webrtcPeer?.setRemoteDescription(answer)
            }

            override suspend fun onIceCandidate(peerId: String, candidate: IceCandidate) {
                webrtcPeer?.addIceCandidate(candidate)
            }
        })
    }

    /**
     * Execute remote action via AVA
     */
    suspend fun executeAction(action: RemoteAction): ActionResult {
        val accessibilityProvider = AndroidAccessibilityProvider(
            VoiceOSAccessibilityService.instance!!
        )

        return accessibilityProvider.performAction(action)
    }

    /**
     * Get current UI state
     */
    suspend fun getUIState(): UIHierarchy {
        val accessibilityProvider = AndroidAccessibilityProvider(
            VoiceOSAccessibilityService.instance!!
        )

        return accessibilityProvider.getUIHierarchy()
    }

    /**
     * Send command to AVA
     */
    suspend fun sendCommand(command: String) {
        val message = mapOf(
            "type" to "command",
            "command" to command,
            "timestamp" to Clock.System.now().toEpochMilliseconds()
        )

        websocket?.send(Json.encodeToString(message))
    }

    /**
     * Disconnect from AVA platform
     */
    fun disconnect() {
        webrtcPeer?.close()
        websocket?.close()
    }
}
```

---

## 31.7 Complete Integration Example

### 31.7.1 VOS4 → AVA Connection Flow

```kotlin
// 1. Initialize AVA connection
suspend fun connectToAVA() {
    val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

    AVAClient.connect(
        serverUrl = "wss://ava.augmentalis.com",
        deviceId = deviceId
    )

    println("Connected to AVA platform")
}

// 2. Handle voice command via AVA
suspend fun handleVoiceCommand(command: String) {
    // Send to AVA for processing
    AVAClient.sendCommand(command)

    // Wait for response
    val response = AVAClient.waitForResponse()

    // Execute action if needed
    if (response.action != null) {
        val result = AVAClient.executeAction(response.action)
        println("Action result: $result")
    }
}

// 3. Share screen via WebRTC
suspend fun shareScreen() {
    val mediaStream = MediaStreamProvider.getScreenCaptureStream()
    AVAClient.webrtcPeer?.addStream(mediaStream)

    println("Screen sharing started")
}

// 4. Remote control from AVA
suspend fun handleRemoteControl() {
    AVAClient.setRemoteControlListener { action ->
        when (action) {
            is RemoteAction.Tap -> {
                println("Remote tap at (${action.x}, ${action.y})")
                AVAClient.executeAction(action)
            }
            is RemoteAction.Type -> {
                println("Remote type: ${action.text}")
                AVAClient.executeAction(action)
            }
            else -> {
                println("Unsupported action: $action")
            }
        }
    }
}
```

---

## 31.8 Summary

AVAConnect provides comprehensive connectivity for VOS4 with:

1. **HTTP/WebSocket**: Full client and server implementations
2. **WebRTC**: Peer-to-peer video/audio streaming (Android + iOS)
3. **Remote Control**: Accessibility-based UI control
4. **Device Pairing**: QR code and PIN-based pairing
5. **Security**: TLS/HTTPS, certificate management, authentication
6. **Service Discovery**: mDNS/Bonjour for local network discovery
7. **Multi-platform**: 88% shared code across Android, iOS, JVM
8. **Production-Ready**: 85% complete, 666 files, 51K+ LOC

**Key Modules:**
- HTTP/WebSocket: Complete server and client
- WebRTC: Video/audio with webrtc-kmp (Android/iOS only)
- Accessibility: Remote UI control via AccessibilityService
- Pairing: QR code generation and device trust
- Security: TLS, certificates, encryption

**Next Steps:**
Chapters 32-35 would cover Testing Strategy, Code Quality Standards, Build System, and Deployment - completing the comprehensive VOS4 Developer Manual.

---

**End of Developer Manual Chapters 29-31**
