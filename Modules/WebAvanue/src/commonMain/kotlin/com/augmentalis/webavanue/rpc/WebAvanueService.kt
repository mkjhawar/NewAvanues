/**
 * WebAvanueService.kt - WebAvanue RPC service interface (KMP)
 *
 * Defines the service contract for browser operations.
 * Implementations are platform-specific (gRPC for Android, HTTP for web).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.webavanue.rpc

import com.augmentalis.webavanue.rpc.messages.*
import kotlinx.coroutines.flow.Flow

/**
 * WebAvanue service interface
 *
 * Provides all browser operations that can be invoked via RPC.
 * Implement this interface to handle incoming requests.
 */
interface IWebAvanueService {

    // ========================================================================
    // Tab Management
    // ========================================================================

    /**
     * Get all open tabs
     */
    suspend fun getTabs(request: GetTabsRequest): GetTabsResponse

    /**
     * Create a new tab
     */
    suspend fun createTab(request: CreateTabRequest): CreateTabResponse

    /**
     * Close a tab
     */
    suspend fun closeTab(request: CloseTabRequest): WebAvanueResponse

    /**
     * Switch to a tab
     */
    suspend fun switchTab(request: SwitchTabRequest): WebAvanueResponse

    // ========================================================================
    // Navigation
    // ========================================================================

    /**
     * Navigate to URL
     */
    suspend fun navigate(request: NavigateRequest): NavigationResponse

    /**
     * Go back in history
     */
    suspend fun goBack(request: GoBackRequest): NavigationResponse

    /**
     * Go forward in history
     */
    suspend fun goForward(request: GoForwardRequest): NavigationResponse

    /**
     * Reload page
     */
    suspend fun reload(request: ReloadRequest): NavigationResponse

    // ========================================================================
    // Page Interaction
    // ========================================================================

    /**
     * Click an element
     */
    suspend fun clickElement(request: ClickElementRequest): WebAvanueResponse

    /**
     * Type text into an element
     */
    suspend fun typeText(request: TypeTextRequest): WebAvanueResponse

    /**
     * Scroll the page
     */
    suspend fun scroll(request: ScrollRequest): WebAvanueResponse

    /**
     * Find elements on the page
     */
    suspend fun findElements(request: FindElementRequest): FindElementResponse

    /**
     * Get page content
     */
    suspend fun getPageContent(request: GetPageContentRequest): GetPageContentResponse

    // ========================================================================
    // Voice Commands
    // ========================================================================

    /**
     * Execute a voice command
     */
    suspend fun executeVoiceCommand(request: VoiceCommandRequest): VoiceCommandResponse

    // ========================================================================
    // Downloads
    // ========================================================================

    /**
     * Start a download
     */
    suspend fun startDownload(request: StartDownloadRequest): DownloadStatus

    /**
     * Get download status
     */
    suspend fun getDownloadStatus(request: DownloadStatusRequest): DownloadStatus

    // ========================================================================
    // Events (Streaming)
    // ========================================================================

    /**
     * Stream browser events
     */
    fun streamEvents(tabId: String?): Flow<WebAvanueEvent>
}

/**
 * Service delegate for platform-specific implementations
 *
 * The RPC server calls these methods to perform actual browser operations.
 * Implement this in your Android app to connect RPC to WebView operations.
 */
interface IWebAvanueServiceDelegate {

    // Tab operations - delegate to TabViewModel
    suspend fun getTabs(): List<TabInfo>
    suspend fun getActiveTabId(): String?
    suspend fun createTab(url: String, makeActive: Boolean): TabInfo?
    suspend fun closeTab(tabId: String): Boolean
    suspend fun switchTab(tabId: String): Boolean

    // Navigation - delegate to WebView
    suspend fun navigate(tabId: String, url: String): Boolean
    suspend fun goBack(tabId: String): Boolean
    suspend fun goForward(tabId: String): Boolean
    suspend fun reload(tabId: String, hardReload: Boolean): Boolean

    // Page interaction - delegate to WebView JavaScript
    suspend fun clickElement(tabId: String, selector: ElementSelector): Boolean
    suspend fun typeText(tabId: String, selector: ElementSelector, text: String, clearFirst: Boolean): Boolean
    suspend fun scroll(tabId: String, direction: ScrollDirection, amount: Int): Boolean
    suspend fun findElements(tabId: String, selector: ElementSelector, includeHidden: Boolean): List<PageElement>
    suspend fun getPageContent(tabId: String, includeHtml: Boolean, includeText: Boolean): GetPageContentResponse?

    // Voice commands
    suspend fun executeVoiceCommand(command: String, tabId: String?, params: Map<String, String>): VoiceCommandResponse

    // Downloads
    suspend fun startDownload(url: String, filename: String?): DownloadStatus
    suspend fun getDownloadStatus(downloadId: String): DownloadStatus?

    // Event stream
    fun getEventFlow(): Flow<WebAvanueEvent>
}

/**
 * RPC Server configuration
 */
data class WebAvanueServerConfig(
    val port: Int = 50055,
    val useUnixSocket: Boolean = false,
    val unixSocketPath: String? = null,
    val maxConcurrentCalls: Int = 100
)

/**
 * RPC Server interface - platform-specific
 */
expect class WebAvanueRpcServer(
    delegate: IWebAvanueServiceDelegate,
    config: WebAvanueServerConfig
) {
    fun start()
    fun stop()
    fun isRunning(): Boolean
    fun getPort(): Int
}
