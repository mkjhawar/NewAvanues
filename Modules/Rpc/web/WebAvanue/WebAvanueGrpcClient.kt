/**
 * WebAvanueGrpcClient.kt - Main gRPC-Web client for WebAvanue
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * High-level client for WebAvanue browser automation with connection management,
 * automatic reconnection, and HTTP/2 or HTTP/1.1 fallback support.
 */
package com.augmentalis.rpc.web.webavanue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Connection state for the gRPC-Web client
 */
enum class ClientConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

/**
 * Client configuration
 */
data class WebAvanueClientConfig(
    val host: String = "localhost",
    val port: Int = 50055,
    val useTls: Boolean = false,
    val autoReconnect: Boolean = true,
    val reconnectDelayMs: Long = 2000,
    val maxReconnectAttempts: Int = 5,
    val connectionTimeout: Long = 10000,
    val requestTimeout: Long = 30000,
    val enableLogging: Boolean = false
)

/**
 * Connection listener interface
 */
interface WebAvanueConnectionListener {
    fun onConnected()
    fun onDisconnected(reason: String?)
    fun onReconnecting(attempt: Int, maxAttempts: Int)
    fun onFailed(error: Throwable)
}

/**
 * WebAvanue gRPC-Web Client
 *
 * Main entry point for browser-based communication with WebAvanue service.
 *
 * Features:
 * - gRPC-Web transport for browser compatibility
 * - HTTP/2 support where available, HTTP/1.1 fallback
 * - Automatic connection management and reconnection
 * - Tab management APIs
 * - Navigation APIs
 * - Page interaction and element finding
 * - Download management
 * - Real-time page event streaming
 *
 * Example usage:
 * ```kotlin
 * val client = WebAvanueGrpcClient.create()
 * client.connect()
 *
 * // Get all tabs
 * val tabs = client.getTabs()
 *
 * // Navigate to URL
 * client.navigate(tabId, "https://example.com")
 *
 * // Find and click element
 * val elements = client.findElementByCss(tabId, "button.submit")
 * client.click(tabId, elements.first())
 *
 * // Stream page events
 * client.streamPageEvents(tabId).collect { event ->
 *     println("Event: ${event.eventType}")
 * }
 * ```
 */
class WebAvanueGrpcClient private constructor(
    private val config: WebAvanueClientConfig
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow(ClientConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ClientConnectionState> = _connectionState.asStateFlow()

    private var transport: GrpcWebTransport? = null
    private var serviceClient: WebAvanueServiceClient? = null
    private var reconnectAttempt = 0
    private val listeners = mutableListOf<WebAvanueConnectionListener>()

    /**
     * Whether the client is currently connected
     */
    val isConnected: Boolean
        get() = _connectionState.value == ClientConnectionState.CONNECTED

    /**
     * Add a connection listener
     */
    fun addConnectionListener(listener: WebAvanueConnectionListener) {
        listeners.add(listener)
    }

    /**
     * Remove a connection listener
     */
    fun removeConnectionListener(listener: WebAvanueConnectionListener) {
        listeners.remove(listener)
    }

    /**
     * Connect to the WebAvanue service
     */
    suspend fun connect(): Result<Unit> {
        if (_connectionState.value == ClientConnectionState.CONNECTED) {
            return Result.success(Unit)
        }

        _connectionState.value = ClientConnectionState.CONNECTING
        log("Connecting to ${config.host}:${config.port}")

        return try {
            transport = GrpcWebTransport(
                GrpcWebConfig(
                    host = config.host,
                    port = config.port,
                    useTls = config.useTls,
                    timeout = config.requestTimeout
                )
            )
            serviceClient = WebAvanueServiceClient(transport!!)

            // Verify connection with a simple call
            val result = serviceClient!!.getTabs()
            if (result.isSuccess) {
                _connectionState.value = ClientConnectionState.CONNECTED
                reconnectAttempt = 0
                listeners.forEach { it.onConnected() }
                log("Connected successfully")
                Result.success(Unit)
            } else {
                handleConnectionFailure(result.exceptionOrNull() ?: Exception("Connection failed"))
            }
        } catch (e: Throwable) {
            handleConnectionFailure(e)
        }
    }

    private suspend fun handleConnectionFailure(error: Throwable): Result<Unit> {
        log("Connection failed: ${error.message}")

        if (config.autoReconnect && reconnectAttempt < config.maxReconnectAttempts) {
            _connectionState.value = ClientConnectionState.RECONNECTING
            reconnectAttempt++
            listeners.forEach { it.onReconnecting(reconnectAttempt, config.maxReconnectAttempts) }

            log("Reconnecting attempt $reconnectAttempt/${config.maxReconnectAttempts}")
            delay(config.reconnectDelayMs * reconnectAttempt)
            return connect()
        }

        _connectionState.value = ClientConnectionState.FAILED
        listeners.forEach { it.onFailed(error) }
        return Result.failure(error)
    }

    /**
     * Disconnect from the service
     */
    fun disconnect() {
        log("Disconnecting")
        transport?.close()
        transport = null
        serviceClient = null
        _connectionState.value = ClientConnectionState.DISCONNECTED
        listeners.forEach { it.onDisconnected("Client disconnected") }
    }

    /**
     * Close the client and release resources
     */
    fun close() {
        disconnect()
        scope.cancel()
        listeners.clear()
    }

    // ========================================================================
    // Tab Management
    // ========================================================================

    /**
     * Get all open tabs
     */
    suspend fun getTabs(): Result<TabsResponse> {
        return withClient { it.getTabs() }
    }

    /**
     * Get the active tab
     */
    suspend fun getActiveTab(): Result<TabInfo?> {
        return getTabs().map { response ->
            response.tabs.find { it.tabId == response.activeTabId }
        }
    }

    /**
     * Create a new tab
     */
    suspend fun createTab(url: String = "", active: Boolean = true): Result<TabInfo> {
        return withClient { it.createTab(url, active) }
    }

    /**
     * Close a tab
     */
    suspend fun closeTab(tabId: String): Result<WebActionResponse> {
        return withClient { it.closeTab(tabId) }
    }

    /**
     * Switch to a tab
     */
    suspend fun switchTab(tabId: String): Result<WebActionResponse> {
        return withClient { it.switchTab(tabId) }
    }

    // ========================================================================
    // Navigation
    // ========================================================================

    /**
     * Navigate to a URL
     */
    suspend fun navigate(tabId: String, url: String): Result<WebActionResponse> {
        return withClient { it.navigate(tabId, url) }
    }

    /**
     * Navigate the active tab to a URL
     */
    suspend fun navigateActiveTab(url: String): Result<WebActionResponse> {
        val activeTab = getActiveTab().getOrNull()
            ?: return Result.failure(IllegalStateException("No active tab"))
        return navigate(activeTab.tabId, url)
    }

    /**
     * Go back in history
     */
    suspend fun goBack(tabId: String): Result<WebActionResponse> {
        return withClient { it.goBack(tabId) }
    }

    /**
     * Go forward in history
     */
    suspend fun goForward(tabId: String): Result<WebActionResponse> {
        return withClient { it.goForward(tabId) }
    }

    /**
     * Reload the page
     */
    suspend fun reload(tabId: String): Result<WebActionResponse> {
        return withClient { it.reload(tabId) }
    }

    // ========================================================================
    // Page Interaction
    // ========================================================================

    /**
     * Execute a page action
     */
    suspend fun executeAction(
        tabId: String,
        action: String,
        params: Map<String, String> = emptyMap()
    ): Result<WebActionResponse> {
        return withClient { it.executeAction(tabId, action, params) }
    }

    /**
     * Find elements by selector
     */
    suspend fun findElement(
        tabId: String,
        selector: ElementSelector
    ): Result<FindElementResponse> {
        return withClient { it.findElement(tabId, selector) }
    }

    /**
     * Find element by CSS selector
     */
    suspend fun findElementByCss(tabId: String, css: String): Result<FindElementResponse> {
        return withClient { it.findElementByCss(tabId, css) }
    }

    /**
     * Find element by XPath
     */
    suspend fun findElementByXPath(tabId: String, xpath: String): Result<FindElementResponse> {
        return withClient { it.findElementByXPath(tabId, xpath) }
    }

    /**
     * Find element by text content
     */
    suspend fun findElementByText(tabId: String, text: String): Result<FindElementResponse> {
        return withClient { it.findElementByText(tabId, text) }
    }

    /**
     * Find element by VUID
     */
    suspend fun findElementByVuid(tabId: String, vuid: String): Result<FindElementResponse> {
        return withClient { it.findElementByVuid(tabId, vuid) }
    }

    /**
     * Get page content
     */
    suspend fun getPageContent(
        tabId: String,
        includeHidden: Boolean = false
    ): Result<PageContent> {
        return withClient { it.getPageContent(tabId, includeHidden) }
    }

    /**
     * Click on an element
     */
    suspend fun click(tabId: String, selector: String): Result<WebActionResponse> {
        return withClient { it.click(tabId, selector) }
    }

    /**
     * Click on a page element
     */
    suspend fun click(tabId: String, element: PageElement): Result<WebActionResponse> {
        val selector = element.attributes["id"]?.let { "#$it" }
            ?: element.attributes["class"]?.let { ".${it.split(" ").first()}" }
            ?: "[data-element-id='${element.elementId}']"
        return click(tabId, selector)
    }

    /**
     * Type text into an element
     */
    suspend fun type(
        tabId: String,
        selector: String,
        text: String,
        clear: Boolean = false
    ): Result<WebActionResponse> {
        return withClient { it.type(tabId, selector, text, clear) }
    }

    /**
     * Scroll the page
     */
    suspend fun scroll(
        tabId: String,
        direction: ScrollDirection = ScrollDirection.DOWN,
        amount: Int = 300
    ): Result<WebActionResponse> {
        return withClient { it.scroll(tabId, direction, amount) }
    }

    /**
     * Select an option from a dropdown
     */
    suspend fun select(
        tabId: String,
        selector: String,
        value: String
    ): Result<WebActionResponse> {
        return withClient { it.select(tabId, selector, value) }
    }

    // ========================================================================
    // Downloads
    // ========================================================================

    /**
     * Start a download
     */
    suspend fun startDownload(
        url: String,
        filename: String = "",
        destination: String = ""
    ): Result<DownloadStatus> {
        return withClient { it.startDownload(url, filename, destination) }
    }

    /**
     * Get download status
     */
    suspend fun getDownloadStatus(downloadId: String): Result<DownloadStatus> {
        return withClient { it.getDownloadStatus(downloadId) }
    }

    // ========================================================================
    // Event Streaming
    // ========================================================================

    /**
     * Stream page events
     */
    fun streamPageEvents(
        tabId: String,
        eventTypes: List<String> = listOf("load", "click", "scroll", "input")
    ): Flow<PageEvent> {
        val client = serviceClient
            ?: throw IllegalStateException("Client not connected")
        return client.streamPageEvents(tabId, eventTypes)
    }

    // ========================================================================
    // Internal Helpers
    // ========================================================================

    private suspend fun <T> withClient(
        block: suspend (WebAvanueServiceClient) -> Result<T>
    ): Result<T> {
        val client = serviceClient
        if (client == null || !isConnected) {
            // Attempt to reconnect
            val connectResult = connect()
            if (connectResult.isFailure) {
                return Result.failure(
                    connectResult.exceptionOrNull()
                        ?: IllegalStateException("Not connected")
                )
            }
        }

        return try {
            block(serviceClient!!)
        } catch (e: Throwable) {
            // Check if we need to reconnect
            if (e is GrpcWebException && e.code == GrpcStatusCode.UNAVAILABLE) {
                _connectionState.value = ClientConnectionState.DISCONNECTED
                if (config.autoReconnect) {
                    scope.launch { connect() }
                }
            }
            Result.failure(e)
        }
    }

    private fun log(message: String) {
        if (config.enableLogging) {
            console.log("[WebAvanueClient] $message")
        }
    }

    companion object {
        /**
         * Create a new WebAvanue client with default configuration
         */
        fun create(
            host: String = "localhost",
            port: Int = 50055
        ): WebAvanueGrpcClient {
            return WebAvanueGrpcClient(
                WebAvanueClientConfig(host = host, port = port)
            )
        }

        /**
         * Create a new WebAvanue client with custom configuration
         */
        fun create(config: WebAvanueClientConfig): WebAvanueGrpcClient {
            return WebAvanueGrpcClient(config)
        }

        /**
         * Default service port
         */
        const val DEFAULT_PORT = 50055
    }
}

/**
 * DSL for building WebAvanue client configuration
 */
class WebAvanueClientConfigBuilder {
    var host: String = "localhost"
    var port: Int = 50055
    var useTls: Boolean = false
    var autoReconnect: Boolean = true
    var reconnectDelayMs: Long = 2000
    var maxReconnectAttempts: Int = 5
    var connectionTimeout: Long = 10000
    var requestTimeout: Long = 30000
    var enableLogging: Boolean = false

    fun build(): WebAvanueClientConfig = WebAvanueClientConfig(
        host = host,
        port = port,
        useTls = useTls,
        autoReconnect = autoReconnect,
        reconnectDelayMs = reconnectDelayMs,
        maxReconnectAttempts = maxReconnectAttempts,
        connectionTimeout = connectionTimeout,
        requestTimeout = requestTimeout,
        enableLogging = enableLogging
    )
}

/**
 * Create WebAvanue client with DSL
 */
fun webAvanueClient(block: WebAvanueClientConfigBuilder.() -> Unit): WebAvanueGrpcClient {
    val builder = WebAvanueClientConfigBuilder()
    builder.block()
    return WebAvanueGrpcClient.create(builder.build())
}

/**
 * Extension to use WebAvanue client in a scoped manner
 */
suspend fun <T> WebAvanueGrpcClient.use(block: suspend (WebAvanueGrpcClient) -> T): T {
    return try {
        connect()
        block(this)
    } finally {
        close()
    }
}

/**
 * External console for logging
 */
external object console {
    fun log(message: String)
    fun error(message: String)
    fun warn(message: String)
}
