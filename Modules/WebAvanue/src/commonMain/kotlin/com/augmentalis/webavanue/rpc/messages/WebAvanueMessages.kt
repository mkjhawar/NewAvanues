/**
 * WebAvanueMessages.kt - RPC message types for WebAvanue service (KMP)
 *
 * Defines all request/response types for browser operations.
 * Platform-agnostic, serializable with kotlinx.serialization.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.webavanue.rpc.messages

import kotlinx.serialization.Serializable

// ============================================================================
// Common Types
// ============================================================================

@Serializable
data class TabInfo(
    val tabId: String,
    val url: String,
    val title: String,
    val isActive: Boolean = false,
    val isPinned: Boolean = false,
    val index: Int = 0,
    val faviconUrl: String? = null
)

@Serializable
data class PageElement(
    val elementId: String,
    val tag: String,
    val text: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val bounds: ElementBounds = ElementBounds(),
    val isVisible: Boolean = true,
    val isInteractable: Boolean = true,
    val avid: String? = null // AVID identifier if available
)

@Serializable
data class ElementBounds(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

@Serializable
data class ElementSelector(
    val css: String? = null,
    val xpath: String? = null,
    val text: String? = null,
    val avid: String? = null
)

// ============================================================================
// Tab Management Messages
// ============================================================================

@Serializable
data class GetTabsRequest(
    val requestId: String
)

@Serializable
data class GetTabsResponse(
    val requestId: String,
    val tabs: List<TabInfo>,
    val activeTabId: String?
)

@Serializable
data class CreateTabRequest(
    val requestId: String,
    val url: String = "",
    val makeActive: Boolean = true
)

@Serializable
data class CreateTabResponse(
    val requestId: String,
    val success: Boolean,
    val tab: TabInfo? = null,
    val error: String? = null
)

@Serializable
data class CloseTabRequest(
    val requestId: String,
    val tabId: String
)

@Serializable
data class SwitchTabRequest(
    val requestId: String,
    val tabId: String
)

// ============================================================================
// Navigation Messages
// ============================================================================

@Serializable
data class NavigateRequest(
    val requestId: String,
    val tabId: String,
    val url: String
)

@Serializable
data class GoBackRequest(
    val requestId: String,
    val tabId: String
)

@Serializable
data class GoForwardRequest(
    val requestId: String,
    val tabId: String
)

@Serializable
data class ReloadRequest(
    val requestId: String,
    val tabId: String,
    val hardReload: Boolean = false
)

@Serializable
data class NavigationResponse(
    val requestId: String,
    val success: Boolean,
    val url: String? = null,
    val error: String? = null
)

// ============================================================================
// Page Interaction Messages
// ============================================================================

@Serializable
data class ClickElementRequest(
    val requestId: String,
    val tabId: String,
    val selector: ElementSelector
)

@Serializable
data class TypeTextRequest(
    val requestId: String,
    val tabId: String,
    val selector: ElementSelector,
    val text: String,
    val clearFirst: Boolean = false
)

@Serializable
data class ScrollRequest(
    val requestId: String,
    val tabId: String,
    val direction: ScrollDirection,
    val amount: Int = 300
)

@Serializable
enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT, TOP, BOTTOM
}

@Serializable
data class FindElementRequest(
    val requestId: String,
    val tabId: String,
    val selector: ElementSelector,
    val includeHidden: Boolean = false
)

@Serializable
data class FindElementResponse(
    val requestId: String,
    val elements: List<PageElement>,
    val success: Boolean = true,
    val error: String? = null
)

@Serializable
data class GetPageContentRequest(
    val requestId: String,
    val tabId: String,
    val includeHtml: Boolean = false,
    val includeText: Boolean = true
)

@Serializable
data class GetPageContentResponse(
    val requestId: String,
    val url: String,
    val title: String,
    val text: String? = null,
    val html: String? = null,
    val elements: List<PageElement> = emptyList()
)

// ============================================================================
// Voice Command Messages
// ============================================================================

@Serializable
data class VoiceCommandRequest(
    val requestId: String,
    val command: String,
    val tabId: String? = null,
    val params: Map<String, String> = emptyMap()
)

@Serializable
data class VoiceCommandResponse(
    val requestId: String,
    val success: Boolean,
    val action: String? = null,
    val result: String? = null,
    val error: String? = null
)

// ============================================================================
// Generic Response
// ============================================================================

@Serializable
data class WebAvanueResponse(
    val requestId: String,
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val data: String? = null // JSON data if needed
)

// ============================================================================
// Download Messages
// ============================================================================

@Serializable
data class StartDownloadRequest(
    val requestId: String,
    val url: String,
    val filename: String? = null
)

@Serializable
data class DownloadStatusRequest(
    val requestId: String,
    val downloadId: String
)

@Serializable
data class DownloadStatus(
    val downloadId: String,
    val state: DownloadState,
    val progress: Float = 0f,
    val bytesReceived: Long = 0,
    val totalBytes: Long = 0,
    val filePath: String? = null,
    val error: String? = null
)

@Serializable
enum class DownloadState {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
}

// ============================================================================
// Event Messages (for streaming)
// ============================================================================

@Serializable
sealed class WebAvanueEvent {
    abstract val tabId: String
    abstract val timestamp: Long

    @Serializable
    data class PageLoaded(
        override val tabId: String,
        override val timestamp: Long,
        val url: String,
        val title: String
    ) : WebAvanueEvent()

    @Serializable
    data class NavigationStarted(
        override val tabId: String,
        override val timestamp: Long,
        val url: String
    ) : WebAvanueEvent()

    @Serializable
    data class TabCreated(
        override val tabId: String,
        override val timestamp: Long
    ) : WebAvanueEvent()

    @Serializable
    data class TabClosed(
        override val tabId: String,
        override val timestamp: Long
    ) : WebAvanueEvent()

    @Serializable
    data class TabActivated(
        override val tabId: String,
        override val timestamp: Long
    ) : WebAvanueEvent()

    @Serializable
    data class DownloadProgress(
        override val tabId: String,
        override val timestamp: Long,
        val downloadId: String,
        val progress: Float
    ) : WebAvanueEvent()
}
