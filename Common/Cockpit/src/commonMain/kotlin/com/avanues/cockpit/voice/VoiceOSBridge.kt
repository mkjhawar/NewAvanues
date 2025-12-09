package com.avanues.cockpit.voice

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.core.workspace.Workspace

/**
 * Voice OS Bridge Interface
 *
 * Defines the contract for voice integration with VoiceOS accessibility service.
 * Cockpit uses this interface to communicate with VoiceOS for voice-first interactions.
 *
 * **Communication Flow:**
 * - Cockpit → VoiceOS: Request voice input, announce actions, request accessibility info
 * - VoiceOS → Cockpit: Send voice commands, accessibility events, gaze targets
 *
 * **Platform Implementations:**
 * - Android: VoiceOS accessibility service integration
 * - iOS: VoiceOver + Siri integration
 * - Desktop: System voice commands + TTS
 *
 * **Voice Commands:**
 * - Window Management: "Open Gmail", "Close browser", "Focus calculator"
 * - Layout Control: "Linear mode", "Arc mode", "Grid mode"
 * - Navigation: "Next window", "Previous workspace"
 * - Manipulation: "Move email left", "Make browser bigger", "Pin this window"
 * - Content: "Read this window", "Scroll down", "Click sign in"
 *
 * **Usage:**
 * ```kotlin
 * val bridge = VoiceOSBridge()
 * bridge.announceAction("Opened Gmail window")
 * val command = bridge.requestVoiceInput()
 * bridge.onVoiceCommand(command)
 * ```
 */
interface VoiceOSBridge {

    // ==================== Cockpit → VoiceOS ====================

    /**
     * Requests voice input from the user
     *
     * Activates voice recognition and waits for user command.
     *
     * @param prompt Optional prompt to display ("Say a command...")
     * @return Voice command result
     */
    suspend fun requestVoiceInput(prompt: String? = null): VoiceCommandResult

    /**
     * Announces an action to the user
     *
     * Uses text-to-speech to inform user of an action.
     * Spatial audio position based on window location.
     *
     * Examples:
     * - "Opened Gmail window"
     * - "Switched to linear mode"
     * - "Moved window to the left"
     *
     * @param message Message to announce
     * @param position Optional 3D position for spatial audio
     */
    suspend fun announceAction(message: String, position: Vector3D? = null)

    /**
     * Requests accessibility information for a window
     *
     * Gets window content structure from VoiceOS accessibility service.
     * Used for "Read this window", "What's in this window?" commands.
     *
     * @param windowId Window identifier
     * @return Accessibility node tree
     */
    suspend fun requestAccessibilityInfo(windowId: String): AccessibilityNode?

    /**
     * Announces workspace state change
     *
     * Voice examples:
     * - "Switched to work setup workspace"
     * - "Loaded media center with 3 windows"
     *
     * @param workspace Current workspace
     */
    suspend fun announceWorkspace(workspace: Workspace) {
        announceAction(workspace.toVoiceDescription())
    }

    /**
     * Announces window state change
     *
     * Voice examples:
     * - "Gmail window at left front"
     * - "Browser zoomed to 150%, scrolled down"
     *
     * @param window Window that changed
     */
    suspend fun announceWindow(window: AppWindow) {
        announceAction(window.toVoiceAnnouncement(), window.position)
    }

    /**
     * Announces layout change
     *
     * Voice examples:
     * - "Switched to linear mode"
     * - "Now in arc layout with 5 windows"
     *
     * @param layoutDescription Layout description
     */
    suspend fun announceLayout(layoutDescription: String) {
        announceAction(layoutDescription)
    }

    // ==================== VoiceOS → Cockpit ====================

    /**
     * Called when VoiceOS recognizes a voice command
     *
     * Main entry point for voice command handling.
     * Cockpit parses the command and executes appropriate action.
     *
     * @param command Voice command from VoiceOS
     */
    fun onVoiceCommand(command: VoiceCommand)

    /**
     * Called when VoiceOS detects an accessibility event
     *
     * Examples:
     * - Window content changed
     * - Focus changed
     * - Window opened/closed by another app
     *
     * @param event Accessibility event
     */
    fun onAccessibilityEvent(event: AccessibilityEvent)

    /**
     * Called when VoiceOS detects gaze target
     *
     * Eye tracking integration for AR glasses.
     * Determines which window user is looking at.
     *
     * @param target Gaze target information
     */
    fun onGazeTarget(target: GazeTarget)

    // ==================== Spatial Audio ====================

    /**
     * Updates spatial audio listener position
     *
     * Called when user's head/camera position changes.
     * Used to calculate correct 3D audio for window announcements.
     *
     * @param position Listener position in 3D space
     * @param rotation Listener rotation (quaternion or euler angles)
     */
    fun updateListenerPosition(position: Vector3D, rotation: Vector3D)

    /**
     * Plays spatial audio feedback at a specific location
     *
     * Examples:
     * - Window open sound at window position
     * - Error beep at error location
     * - Click sound at interaction point
     *
     * @param soundId Sound identifier
     * @param position 3D position for sound
     * @param volume Volume level (0.0 to 1.0)
     */
    suspend fun playSpatialAudio(soundId: String, position: Vector3D, volume: Float = 1.0f)
}

/**
 * Voice Command Result
 *
 * Represents the result of voice recognition.
 *
 * @property success Whether recognition was successful
 * @property text Recognized text
 * @property confidence Confidence score (0.0 to 1.0)
 * @property alternatives Alternative interpretations
 */
data class VoiceCommandResult(
    val success: Boolean,
    val text: String = "",
    val confidence: Float = 0f,
    val alternatives: List<String> = emptyList()
)

/**
 * Voice Command
 *
 * Parsed voice command from VoiceOS.
 *
 * @property rawText Original voice input
 * @property intent Parsed intent (OPEN_WINDOW, CLOSE_WINDOW, SWITCH_LAYOUT, etc.)
 * @property parameters Command parameters (window name, layout ID, etc.)
 * @property confidence Recognition confidence (0.0 to 1.0)
 */
data class VoiceCommand(
    val rawText: String,
    val intent: VoiceIntent,
    val parameters: Map<String, String> = emptyMap(),
    val confidence: Float = 1.0f
)

/**
 * Voice Intent
 *
 * Categorizes what the user wants to do.
 */
enum class VoiceIntent {
    // Window Management
    OPEN_WINDOW,      // "Open Gmail"
    CLOSE_WINDOW,     // "Close browser"
    FOCUS_WINDOW,     // "Focus calculator"
    SHOW_WINDOW,      // "Show email"
    HIDE_WINDOW,      // "Hide music player"

    // Window Manipulation
    MOVE_WINDOW,      // "Move email left"
    RESIZE_WINDOW,    // "Make browser bigger"
    PIN_WINDOW,       // "Pin this window"
    UNPIN_WINDOW,     // "Unpin window"

    // Layout Control
    SWITCH_LAYOUT,    // "Linear mode"
    MOVE_WORKSPACE,   // "Move workspace forward"

    // Workspace Management
    LOAD_WORKSPACE,   // "Load work setup"
    SAVE_WORKSPACE,   // "Save workspace as evening"
    NEXT_WORKSPACE,   // "Next workspace"
    PREV_WORKSPACE,   // "Previous workspace"

    // Window Content
    READ_WINDOW,      // "Read this window"
    SCROLL_WINDOW,    // "Scroll down"
    ZOOM_WINDOW,      // "Zoom in"
    CLICK_ELEMENT,    // "Click sign in"

    // Queries
    QUERY_WORKSPACE,  // "What workspace am I in?"
    QUERY_LAYOUT,     // "What layout am I using?"
    QUERY_WINDOWS,    // "What windows are open?"
    QUERY_CAPACITY,   // "Can I add another window?"

    // Unknown
    UNKNOWN           // Unrecognized command
}

/**
 * Accessibility Node
 *
 * Represents a node in the accessibility tree.
 * Used for reading window contents and identifying interactive elements.
 *
 * @property id Node identifier
 * @property className Class name (Button, EditText, etc.)
 * @property text Visible text
 * @property contentDescription Accessibility description
 * @property isClickable Whether node is clickable
 * @property isFocusable Whether node is focusable
 * @property children Child nodes
 */
data class AccessibilityNode(
    val id: String,
    val className: String,
    val text: String? = null,
    val contentDescription: String? = null,
    val isClickable: Boolean = false,
    val isFocusable: Boolean = false,
    val children: List<AccessibilityNode> = emptyList()
)

/**
 * Accessibility Event
 *
 * Represents an accessibility event from VoiceOS.
 *
 * @property type Event type (WINDOW_CONTENT_CHANGED, FOCUS_CHANGED, etc.)
 * @property windowId Affected window ID
 * @property node Accessibility node that triggered event
 * @property timestamp Event timestamp
 */
data class AccessibilityEvent(
    val type: AccessibilityEventType,
    val windowId: String,
    val node: AccessibilityNode? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Accessibility Event Type
 */
enum class AccessibilityEventType {
    WINDOW_CONTENT_CHANGED,
    WINDOW_STATE_CHANGED,
    FOCUS_CHANGED,
    CLICK,
    SCROLL,
    TEXT_CHANGED
}

/**
 * Gaze Target
 *
 * Represents where the user is looking (eye tracking).
 * Used to determine which window has gaze focus.
 *
 * @property rayOrigin Gaze ray origin (eye position)
 * @property rayDirection Gaze ray direction (normalized)
 * @property targetWindowId Window that gaze intersects (if any)
 * @property confidence Tracking confidence (0.0 to 1.0)
 */
data class GazeTarget(
    val rayOrigin: Vector3D,
    val rayDirection: Vector3D,
    val targetWindowId: String? = null,
    val confidence: Float = 1.0f
)

/**
 * Platform-specific VoiceOSBridge implementation
 */
expect class VoiceOSBridgeImpl() : VoiceOSBridge
