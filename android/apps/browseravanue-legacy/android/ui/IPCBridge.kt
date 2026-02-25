package com.augmentalis.browseravanue.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * IPC Bridge for BrowserAvanue ↔ Avanues communication
 *
 * Architecture:
 * - Event-based IPC using SharedFlow
 * - Bidirectional communication
 * - Type-safe message passing
 * - No direct dependencies between modules
 *
 * Communication Channels:
 * 1. Browser → VoiceOS: Voice commands, status updates
 * 2. VoiceOS → Browser: Commands, queries, actions
 * 3. Browser → Other Apps: Share URL, open links
 * 4. Other Apps → Browser: Open URL requests
 *
 * Message Flow:
 * ```
 * BrowserAvanue
 *     ↓ (send message)
 * IPCBridge.sendToVoiceOS()
 *     ↓
 * IDEAMagic IPC Bus
 *     ↓
 * Avanues Core
 *     ↓ (handle & respond)
 * IDEAMagic IPC Bus
 *     ↓
 * IPCBridge.messagesFromVoiceOS
 *     ↓
 * BrowserAvanue (collect)
 * ```
 *
 * Integration Points:
 * - IDEAMagic IPC Bus (future)
 * - VoiceOS Command System
 * - Android Intents (fallback)
 *
 * Features:
 * - Send voice commands to VoiceOS
 * - Receive commands from VoiceOS
 * - Share URLs with other apps
 * - Handle incoming URL intents
 * - Status broadcasting
 *
 * Usage:
 * ```
 * // Send to VoiceOS
 * ipcBridge.sendToVoiceOS(
 *     IPCMessage.VoiceCommand("open google.com")
 * )
 *
 * // Receive from VoiceOS
 * ipcBridge.messagesFromVoiceOS.collect { message ->
 *     when (message) {
 *         is IPCMessage.OpenUrl -> openUrl(message.url)
 *         is IPCMessage.NewTab -> createNewTab()
 *     }
 * }
 * ```
 */
class IPCBridge {

    // Messages from VoiceOS to Browser
    private val _messagesFromVoiceOS = MutableSharedFlow<IPCMessage>(replay = 0)
    val messagesFromVoiceOS: SharedFlow<IPCMessage> = _messagesFromVoiceOS.asSharedFlow()

    // Messages from other apps to Browser
    private val _messagesFromApps = MutableSharedFlow<IPCMessage>(replay = 0)
    val messagesFromApps: SharedFlow<IPCMessage> = _messagesFromApps.asSharedFlow()

    // Status updates from Browser
    private val _browserStatus = MutableSharedFlow<BrowserStatus>(replay = 1)
    val browserStatus: SharedFlow<BrowserStatus> = _browserStatus.asSharedFlow()

    /**
     * Send message to VoiceOS
     *
     * @param message IPC message
     */
    suspend fun sendToVoiceOS(message: IPCMessage) {
        // TODO: Route through IDEAMagic IPC Bus
        // For now, log or handle locally
    }

    /**
     * Send message to other apps
     *
     * @param message IPC message
     */
    suspend fun sendToApps(message: IPCMessage) {
        // TODO: Route through IDEAMagic IPC Bus or Android Intents
    }

    /**
     * Receive message from VoiceOS
     *
     * Called by VoiceOS to send commands to browser
     *
     * @param message IPC message
     */
    suspend fun receiveFromVoiceOS(message: IPCMessage) {
        _messagesFromVoiceOS.emit(message)
    }

    /**
     * Receive message from other apps
     *
     * Called when other apps send intents to browser
     *
     * @param message IPC message
     */
    suspend fun receiveFromApps(message: IPCMessage) {
        _messagesFromApps.emit(message)
    }

    /**
     * Broadcast browser status
     *
     * @param status Current browser status
     */
    suspend fun broadcastStatus(status: BrowserStatus) {
        _browserStatus.emit(status)
    }

    /**
     * Register with IDEAMagic IPC Bus
     *
     * Called during app initialization
     */
    fun registerWithIPCBus(): Boolean {
        // TODO: Register with IDEAMagic IPC Bus
        // AvanueIPCBus.registerModule("browseravanue", this)
        return true
    }

    /**
     * Unregister from IDEAMagic IPC Bus
     *
     * Called during app shutdown
     */
    fun unregisterFromIPCBus() {
        // TODO: Unregister from IDEAMagic IPC Bus
        // AvanueIPCBus.unregisterModule("browseravanue")
    }
}

/**
 * IPC message types
 */
sealed class IPCMessage {

    // ==========================================
    // VoiceOS → Browser Commands
    // ==========================================

    /**
     * Open URL in browser
     */
    data class OpenUrl(val url: String, val newTab: Boolean = false) : IPCMessage()

    /**
     * Create new tab
     */
    data class NewTab(val url: String? = null, val incognito: Boolean = false) : IPCMessage()

    /**
     * Close current tab
     */
    data object CloseTab : IPCMessage()

    /**
     * Go back
     */
    data object GoBack : IPCMessage()

    /**
     * Go forward
     */
    data object GoForward : IPCMessage()

    /**
     * Refresh page
     */
    data object Refresh : IPCMessage()

    /**
     * Search query
     */
    data class Search(val query: String) : IPCMessage()

    /**
     * Navigate to internal page
     */
    data class NavigateToInternal(val page: String) : IPCMessage()

    /**
     * Toggle setting
     */
    data class ToggleSetting(val setting: String) : IPCMessage()

    // ==========================================
    // Browser → VoiceOS Messages
    // ==========================================

    /**
     * Voice command from browser
     */
    data class VoiceCommand(val command: String) : IPCMessage()

    /**
     * Page loaded notification
     */
    data class PageLoaded(val url: String, val title: String) : IPCMessage()

    /**
     * Tab created notification
     */
    data class TabCreated(val tabId: String, val url: String) : IPCMessage()

    /**
     * Tab closed notification
     */
    data class TabClosed(val tabId: String) : IPCMessage()

    /**
     * Download started notification
     */
    data class DownloadStarted(val url: String, val filename: String) : IPCMessage()

    // ==========================================
    // App → Browser Messages
    // ==========================================

    /**
     * Share URL with browser
     */
    data class ShareUrl(val url: String, val sourceApp: String) : IPCMessage()

    /**
     * Open link from other app
     */
    data class OpenLink(val url: String) : IPCMessage()

    // ==========================================
    // Browser → App Messages
    // ==========================================

    /**
     * Share URL from browser
     */
    data class ShareFromBrowser(val url: String, val title: String) : IPCMessage()

    /**
     * Copy URL to clipboard
     */
    data class CopyUrl(val url: String) : IPCMessage()
}

/**
 * Browser status for broadcasting
 */
data class BrowserStatus(
    val isActive: Boolean,
    val currentUrl: String?,
    val currentTitle: String?,
    val tabCount: Int,
    val isLoading: Boolean,
    val loadProgress: Int
)

/**
 * IPC message handler interface
 *
 * Implement this in your ViewModel or Activity to handle IPC messages
 */
interface IPCMessageHandler {
    suspend fun handleMessage(message: IPCMessage)
}

/**
 * Extension function to collect IPC messages
 *
 * Usage:
 * ```
 * lifecycleScope.launch {
 *     ipcBridge.collectMessages { message ->
 *         handleMessage(message)
 *     }
 * }
 * ```
 */
suspend fun IPCBridge.collectMessages(handler: suspend (IPCMessage) -> Unit) {
    messagesFromVoiceOS.collect { message ->
        handler(message)
    }
}

/**
 * Extension function to handle common IPC patterns
 */
suspend fun IPCBridge.handleOpenUrl(url: String, onOpenUrl: suspend (String) -> Unit) {
    messagesFromVoiceOS.collect { message ->
        when (message) {
            is IPCMessage.OpenUrl -> onOpenUrl(message.url)
            else -> { /* Ignore */ }
        }
    }
}

/**
 * Migration notes for IDEAMagic IPC:
 *
 * When IDEAMagic IPC Bus is ready:
 * 1. Replace SharedFlow with IDEAMagic message bus
 * 2. Use IDEAMagic serialization for messages
 * 3. Implement proper module registration
 * 4. Add security/permission checks
 *
 * Current implementation uses in-memory SharedFlow for testing.
 * Will be replaced with IDEAMagic IPC when available.
 *
 * Example migration:
 * ```
 * // Before (SharedFlow)
 * suspend fun sendToVoiceOS(message: IPCMessage) {
 *     _messagesFromVoiceOS.emit(message)
 * }
 *
 * // After (IDEAMagic)
 * suspend fun sendToVoiceOS(message: IPCMessage) {
 *     AvanueIPCBus.send(
 *         target = "voiceos",
 *         message = message.serialize()
 *     )
 * }
 * ```
 */
