/**
 * VoiceOSMessages.kt - RPC message types for VoiceOS service (KMP)
 *
 * Defines all request/response types for voice and accessibility operations.
 * Uses AVU 2.1 format for efficient cross-platform communication.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore.rpc.messages

import kotlinx.serialization.Serializable

// ============================================================================
// Voice Command Messages
// ============================================================================

@Serializable
data class VoiceCommandRequest(
    val requestId: String,
    val commandText: String,
    val context: Map<String, String> = emptyMap(),
    val language: String = "en-US"
)

@Serializable
data class VoiceCommandResponse(
    val requestId: String,
    val success: Boolean,
    val action: String? = null,
    val result: String? = null,
    val confidence: Float = 0f,
    val error: String? = null
)

// ============================================================================
// Accessibility Action Messages
// ============================================================================

@Serializable
data class AccessibilityActionRequest(
    val requestId: String,
    val actionType: AccessibilityActionType,
    val targetAvid: String? = null,
    val params: Map<String, String> = emptyMap()
)

@Serializable
enum class AccessibilityActionType {
    CLICK, LONG_CLICK, DOUBLE_CLICK,
    SCROLL_UP, SCROLL_DOWN, SCROLL_LEFT, SCROLL_RIGHT,
    FOCUS, CLEAR_FOCUS,
    SET_TEXT, GET_TEXT, CLEAR_TEXT,
    EXPAND, COLLAPSE,
    SELECT, DESELECT,
    COPY, PASTE, CUT,
    DISMISS, BACK, HOME, RECENTS,
    CUSTOM
}

@Serializable
data class AccessibilityActionResponse(
    val requestId: String,
    val success: Boolean,
    val resultText: String? = null,
    val error: String? = null
)

// ============================================================================
// Screen Scraping Messages
// ============================================================================

@Serializable
data class ScrapeScreenRequest(
    val requestId: String,
    val includeInvisible: Boolean = false,
    val maxDepth: Int = 10
)

@Serializable
data class ScrapeScreenResponse(
    val requestId: String,
    val success: Boolean,
    val packageName: String = "",
    val activityName: String = "",
    val elements: List<ScreenElement> = emptyList(),
    val error: String? = null
)

@Serializable
data class ScreenElement(
    val avid: String,
    val className: String,
    val text: String = "",
    val contentDescription: String = "",
    val bounds: ElementBounds,
    val isClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEditable: Boolean = false,
    val isVisible: Boolean = true,
    val children: List<ScreenElement> = emptyList()
)

@Serializable
data class ElementBounds(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

// ============================================================================
// Voice Recognition Messages
// ============================================================================

@Serializable
data class StartRecognitionRequest(
    val requestId: String,
    val language: String = "en-US",
    val continuous: Boolean = false,
    val interimResults: Boolean = true
)

@Serializable
data class StopRecognitionRequest(
    val requestId: String
)

@Serializable
data class RecognitionResult(
    val requestId: String,
    val transcript: String,
    val confidence: Float,
    val isFinal: Boolean,
    val alternatives: List<String> = emptyList()
)

// ============================================================================
// App Learning Messages
// ============================================================================

@Serializable
data class LearnAppRequest(
    val requestId: String,
    val packageName: String? = null // null = current foreground app
)

@Serializable
data class LearnedAppInfo(
    val packageName: String,
    val appName: String,
    val screenCount: Int,
    val elementCount: Int,
    val commandCount: Int,
    val learnedAt: Long
)

@Serializable
data class GetLearnedAppsRequest(
    val requestId: String
)

@Serializable
data class LearnedAppsResponse(
    val requestId: String,
    val apps: List<LearnedAppInfo>
)

@Serializable
data class GetCommandsRequest(
    val requestId: String,
    val packageName: String
)

@Serializable
data class AppCommandsResponse(
    val requestId: String,
    val packageName: String,
    val commands: List<LearnedCommand>
)

@Serializable
data class LearnedCommand(
    val phrase: String,
    val actionType: String,
    val targetAvid: String,
    val confidence: Float
)

// ============================================================================
// Service Status Messages
// ============================================================================

@Serializable
data class ServiceStatusRequest(
    val requestId: String
)

@Serializable
data class ServiceStatus(
    val requestId: String,
    val isReady: Boolean,
    val isAccessibilityEnabled: Boolean,
    val isVoiceRecognitionActive: Boolean,
    val currentLanguage: String,
    val version: String,
    val capabilities: List<String>
)

// ============================================================================
// Dynamic Command Registration
// ============================================================================

@Serializable
data class RegisterCommandRequest(
    val requestId: String,
    val phrase: String,
    val actionType: String,
    val actionParams: Map<String, String> = emptyMap(),
    val appPackage: String? = null // null = global command
)

@Serializable
data class UnregisterCommandRequest(
    val requestId: String,
    val phrase: String,
    val appPackage: String? = null
)

// ============================================================================
// Event Streaming
// ============================================================================

@Serializable
sealed class VoiceOSEvent {
    abstract val timestamp: Long

    @Serializable
    data class RecognitionStarted(
        override val timestamp: Long,
        val language: String
    ) : VoiceOSEvent()

    @Serializable
    data class RecognitionResult(
        override val timestamp: Long,
        val transcript: String,
        val confidence: Float,
        val isFinal: Boolean
    ) : VoiceOSEvent()

    @Serializable
    data class RecognitionStopped(
        override val timestamp: Long,
        val reason: String
    ) : VoiceOSEvent()

    @Serializable
    data class CommandExecuted(
        override val timestamp: Long,
        val command: String,
        val success: Boolean,
        val result: String?
    ) : VoiceOSEvent()

    @Serializable
    data class ScreenChanged(
        override val timestamp: Long,
        val packageName: String,
        val activityName: String
    ) : VoiceOSEvent()

    @Serializable
    data class AccessibilityStateChanged(
        override val timestamp: Long,
        val isEnabled: Boolean
    ) : VoiceOSEvent()
}

// ============================================================================
// Generic Response
// ============================================================================

@Serializable
data class VoiceOSResponse(
    val requestId: String,
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)
