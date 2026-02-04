/**
 * WebAvanueServiceClient.kt - gRPC-Web service client for WebAvanue
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Typed service client generated from webavanue.proto definitions.
 */
package com.augmentalis.rpc.web.webavanue

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Service name constant
 */
private const val SERVICE_NAME = "com.augmentalis.rpc.webavanue.WebAvanueService"

// ============================================================================
// Message Types (matching webavanue.proto)
// ============================================================================

@Serializable
data class TabInfo(
    val tabId: String = "",
    val url: String = "",
    val title: String = "",
    val active: Boolean = false,
    val pinned: Boolean = false,
    val index: Int = 0
)

@Serializable
data class Rect(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

@Serializable
data class PageElement(
    val elementId: String = "",
    val tag: String = "",
    val text: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val bounds: Rect = Rect(),
    val visible: Boolean = false,
    val interactable: Boolean = false
)

@Serializable
data class ElementSelector(
    val css: String = "",
    val xpath: String = "",
    val text: String = "",
    val vuid: String = ""
)

@Serializable
data class WebActionResponse(
    val requestId: String = "",
    val success: Boolean = false,
    val message: String = "",
    val resultJson: String = ""
)

@Serializable
data class PageContent(
    val requestId: String = "",
    val url: String = "",
    val title: String = "",
    val html: String = "",
    val text: String = "",
    val elements: List<PageElement> = emptyList()
)

@Serializable
data class DownloadStatus(
    val requestId: String = "",
    val downloadId: String = "",
    val state: String = "",
    val progress: Float = 0f,
    val bytesReceived: Long = 0,
    val totalBytes: Long = 0,
    val filePath: String = ""
)

@Serializable
data class PageEvent(
    val tabId: String = "",
    val eventType: String = "",
    val targetSelector: String = "",
    val data: Map<String, String> = emptyMap(),
    val timestamp: Long = 0
)

// Request types
@Serializable
data class GetTabsRequest(val requestId: String = "")

@Serializable
data class TabsResponse(
    val tabs: List<TabInfo> = emptyList(),
    val activeTabId: String = ""
)

@Serializable
data class CreateTabRequest(
    val requestId: String = "",
    val url: String = "",
    val active: Boolean = true
)

@Serializable
data class CloseTabRequest(
    val requestId: String = "",
    val tabId: String = ""
)

@Serializable
data class SwitchTabRequest(
    val requestId: String = "",
    val tabId: String = ""
)

@Serializable
data class NavigateRequest(
    val requestId: String = "",
    val tabId: String = "",
    val url: String = ""
)

@Serializable
data class GoBackRequest(
    val requestId: String = "",
    val tabId: String = ""
)

@Serializable
data class GoForwardRequest(
    val requestId: String = "",
    val tabId: String = ""
)

@Serializable
data class ReloadRequest(
    val requestId: String = "",
    val tabId: String = ""
)

@Serializable
data class PageActionRequest(
    val requestId: String = "",
    val tabId: String = "",
    val action: String = "",
    val params: Map<String, String> = emptyMap()
)

@Serializable
data class FindElementRequest(
    val requestId: String = "",
    val tabId: String = "",
    val selector: ElementSelector = ElementSelector()
)

@Serializable
data class FindElementResponse(
    val requestId: String = "",
    val elements: List<PageElement> = emptyList()
)

@Serializable
data class GetPageContentRequest(
    val requestId: String = "",
    val tabId: String = "",
    val includeHidden: Boolean = false
)

@Serializable
data class DownloadRequest(
    val requestId: String = "",
    val url: String = "",
    val filename: String = "",
    val destination: String = ""
)

@Serializable
data class GetDownloadStatusRequest(
    val requestId: String = "",
    val downloadId: String = ""
)

@Serializable
data class StreamPageEventsRequest(
    val requestId: String = "",
    val tabId: String = "",
    val eventTypes: List<String> = emptyList()
)

// ============================================================================
// JSON Serializers (for gRPC-Web JSON mode)
// ============================================================================

/**
 * JSON-based message serialization for gRPC-Web
 */
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

/**
 * Generic JSON serializer
 */
inline fun <reified T> createJsonSerializer(): MessageSerializer<T> {
    return object : MessageSerializer<T> {
        override fun serialize(message: T): ByteArray {
            return json.encodeToString(message).encodeToByteArray()
        }
    }
}

/**
 * Generic JSON deserializer
 */
inline fun <reified T> createJsonDeserializer(): MessageDeserializer<T> {
    return object : MessageDeserializer<T> {
        override fun deserialize(data: ByteArray): T {
            return json.decodeFromString(data.decodeToString())
        }
    }
}

// ============================================================================
// Service Client
// ============================================================================

/**
 * WebAvanue gRPC-Web service client
 *
 * Provides typed access to all WebAvanue service methods including:
 * - Tab management (create, close, switch tabs)
 * - Navigation (navigate, back, forward, reload)
 * - Page interaction (execute actions, find elements, get content)
 * - Downloads (start, status)
 * - Event streaming (page events)
 */
class WebAvanueServiceClient(
    private val transport: GrpcWebTransport
) {
    private var requestCounter: Long = 0

    private fun nextRequestId(): String = "req-${++requestCounter}-${currentTimeMillis()}"

    // ========================================================================
    // Tab Management
    // ========================================================================

    /**
     * Get all open tabs
     */
    suspend fun getTabs(): Result<TabsResponse> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "GetTabs",
            request = GetTabsRequest(requestId = nextRequestId()),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    /**
     * Create a new tab
     */
    suspend fun createTab(url: String = "", active: Boolean = true): Result<TabInfo> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "CreateTab",
            request = CreateTabRequest(
                requestId = nextRequestId(),
                url = url,
                active = active
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    /**
     * Close a tab
     */
    suspend fun closeTab(tabId: String): Result<WebActionResponse> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "CloseTab",
            request = CloseTabRequest(
                requestId = nextRequestId(),
                tabId = tabId
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    /**
     * Switch to a tab
     */
    suspend fun switchTab(tabId: String): Result<WebActionResponse> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "SwitchTab",
            request = SwitchTabRequest(
                requestId = nextRequestId(),
                tabId = tabId
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    // ========================================================================
    // Navigation
    // ========================================================================

    /**
     * Navigate to a URL
     */
    suspend fun navigate(tabId: String, url: String): Result<WebActionResponse> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "Navigate",
            request = NavigateRequest(
                requestId = nextRequestId(),
                tabId = tabId,
                url = url
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    /**
     * Go back in history
     */
    suspend fun goBack(tabId: String): Result<WebActionResponse> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "GoBack",
            request = GoBackRequest(
                requestId = nextRequestId(),
                tabId = tabId
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    /**
     * Go forward in history
     */
    suspend fun goForward(tabId: String): Result<WebActionResponse> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "GoForward",
            request = GoForwardRequest(
                requestId = nextRequestId(),
                tabId = tabId
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    /**
     * Reload the page
     */
    suspend fun reload(tabId: String): Result<WebActionResponse> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "Reload",
            request = ReloadRequest(
                requestId = nextRequestId(),
                tabId = tabId
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    // ========================================================================
    // Page Interaction
    // ========================================================================

    /**
     * Execute a page action (scroll, click, type, select)
     */
    suspend fun executeAction(
        tabId: String,
        action: String,
        params: Map<String, String> = emptyMap()
    ): Result<WebActionResponse> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "ExecuteAction",
            request = PageActionRequest(
                requestId = nextRequestId(),
                tabId = tabId,
                action = action,
                params = params
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    /**
     * Find elements matching a selector
     */
    suspend fun findElement(
        tabId: String,
        selector: ElementSelector
    ): Result<FindElementResponse> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "FindElement",
            request = FindElementRequest(
                requestId = nextRequestId(),
                tabId = tabId,
                selector = selector
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    /**
     * Find element by CSS selector
     */
    suspend fun findElementByCss(tabId: String, css: String): Result<FindElementResponse> {
        return findElement(tabId, ElementSelector(css = css))
    }

    /**
     * Find element by XPath
     */
    suspend fun findElementByXPath(tabId: String, xpath: String): Result<FindElementResponse> {
        return findElement(tabId, ElementSelector(xpath = xpath))
    }

    /**
     * Find element by text content
     */
    suspend fun findElementByText(tabId: String, text: String): Result<FindElementResponse> {
        return findElement(tabId, ElementSelector(text = text))
    }

    /**
     * Find element by VUID
     */
    suspend fun findElementByVuid(tabId: String, vuid: String): Result<FindElementResponse> {
        return findElement(tabId, ElementSelector(vuid = vuid))
    }

    /**
     * Get page content
     */
    suspend fun getPageContent(
        tabId: String,
        includeHidden: Boolean = false
    ): Result<PageContent> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "GetPageContent",
            request = GetPageContentRequest(
                requestId = nextRequestId(),
                tabId = tabId,
                includeHidden = includeHidden
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
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
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "StartDownload",
            request = DownloadRequest(
                requestId = nextRequestId(),
                url = url,
                filename = filename,
                destination = destination
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    /**
     * Get download status
     */
    suspend fun getDownloadStatus(downloadId: String): Result<DownloadStatus> {
        return transport.unaryCall(
            serviceName = SERVICE_NAME,
            methodName = "GetDownloadStatus",
            request = GetDownloadStatusRequest(
                requestId = nextRequestId(),
                downloadId = downloadId
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
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
        return transport.serverStreamingCall(
            serviceName = SERVICE_NAME,
            methodName = "StreamPageEvents",
            request = StreamPageEventsRequest(
                requestId = nextRequestId(),
                tabId = tabId,
                eventTypes = eventTypes
            ),
            serializer = createJsonSerializer(),
            deserializer = createJsonDeserializer()
        )
    }

    // ========================================================================
    // Convenience Methods
    // ========================================================================

    /**
     * Click on an element
     */
    suspend fun click(tabId: String, selector: String): Result<WebActionResponse> {
        return executeAction(tabId, "click", mapOf("selector" to selector))
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
        return executeAction(
            tabId,
            "type",
            mapOf(
                "selector" to selector,
                "text" to text,
                "clear" to clear.toString()
            )
        )
    }

    /**
     * Scroll the page
     */
    suspend fun scroll(
        tabId: String,
        direction: ScrollDirection = ScrollDirection.DOWN,
        amount: Int = 300
    ): Result<WebActionResponse> {
        return executeAction(
            tabId,
            "scroll",
            mapOf(
                "direction" to direction.name.lowercase(),
                "amount" to amount.toString()
            )
        )
    }

    /**
     * Select an option from a dropdown
     */
    suspend fun select(
        tabId: String,
        selector: String,
        value: String
    ): Result<WebActionResponse> {
        return executeAction(
            tabId,
            "select",
            mapOf(
                "selector" to selector,
                "value" to value
            )
        )
    }

    companion object {
        /**
         * Create client with default configuration
         */
        fun create(host: String = "localhost", port: Int = 50055): WebAvanueServiceClient {
            val transport = GrpcWebTransport(
                GrpcWebConfig(host = host, port = port)
            )
            return WebAvanueServiceClient(transport)
        }
    }
}

/**
 * Scroll directions
 */
enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

/**
 * Platform-specific time function
 */
private fun currentTimeMillis(): Long {
    return js("Date.now()").unsafeCast<Double>().toLong()
}
