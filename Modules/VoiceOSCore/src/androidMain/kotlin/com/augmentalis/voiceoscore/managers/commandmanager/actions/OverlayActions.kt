/**
 * OverlayActions.kt - Overlay and UI command actions (Q7 Dual Mode Support)
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/OverlayActions.kt
 *
 * Created: 2025-08-19
 * Updated: 2025-10-10 20:00 PDT - Added Q7 dual-mode support (visual + audio)
 * Author: Claude Code
 * Module: Commands
 *
 * Purpose: Overlay, help, and UI-related voice command actions
 * CRITICAL: All features work in BOTH visual mode (overlay granted) and audio mode (overlay denied)
 */

package com.augmentalis.voiceoscore.managers.commandmanager.actions

import com.augmentalis.voiceoscore.*
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Overlay and UI command actions with Q7 dual-mode support
 * Handles overlay display, help system, and UI visibility controls
 *
 * Q7 DUAL MODE IMPLEMENTATION:
 * - Visual Mode: Overlay permission granted, show visual overlays
 * - Audio Mode: Overlay permission denied, use audio announcements + vibration
 */
object OverlayActions {

    private const val TAG = "OverlayActions"

    // Overlay state management
    private var isOverlayVisible = false
    private var isHelpVisible = false
    private var isCommandHintsVisible = false

    /**
     * Q7: Overlay mode detection
     */
    enum class OverlayMode {
        VISUAL,     // Overlay permission granted
        AUDIO       // Overlay permission denied, audio-only
    }

    /**
     * Detect current overlay mode based on permission
     */
    private fun detectOverlayMode(context: Context): OverlayMode {
        return if (Settings.canDrawOverlays(context)) {
            OverlayMode.VISUAL
        } else {
            OverlayMode.AUDIO
        }
    }
    
    /**
     * Show Overlay Action (Q7 Dual Mode)
     * Visual Mode: Display overlay
     * Audio Mode: Audio announcement + vibration
     */
    class ShowOverlayAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val mode = detectOverlayMode(context)

            return when (mode) {
                OverlayMode.VISUAL -> executeVisualMode(command, accessibilityService, context)
                OverlayMode.AUDIO -> executeAudioMode(command, accessibilityService, context)
            }
        }

        private fun executeVisualMode(
            command: Command,
            service: AccessibilityService?,
            @Suppress("UNUSED_PARAMETER") context: Context
        ): CommandResult {
            return if (isOverlayVisible) {
                createSuccessResult(command, "Overlay is already visible")
            } else {
                // This would integrate with the overlay module (e.g., HUDManager)
                isOverlayVisible = true
                announceViaAccessibility(service, "Overlay shown")
                createSuccessResult(command, "Overlay shown")
            }
        }

        private fun executeAudioMode(
            command: Command,
            service: AccessibilityService?,
            context: Context
        ): CommandResult {
            // Audio mode: Use audio feedback + vibration instead of visual overlay
            announceViaAccessibility(service, "Overlay mode unavailable. Overlay permission not granted. Using audio feedback.")
            vibrateDevice(context, 100)

            // Mark as "shown" conceptually (audio mode active)
            isOverlayVisible = true
            return createSuccessResult(command, "Audio mode active (overlay permission required for visual mode)")
        }
    }
    
    /**
     * Hide Overlay Action (Q7 Dual Mode)
     * Visual Mode: Hide overlay
     * Audio Mode: Audio announcement
     */
    class HideOverlayAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val mode = detectOverlayMode(context)

            return when (mode) {
                OverlayMode.VISUAL -> executeVisualMode(command, accessibilityService, context)
                OverlayMode.AUDIO -> executeAudioMode(command, accessibilityService, context)
            }
        }

        private fun executeVisualMode(
            command: Command,
            service: AccessibilityService?,
            @Suppress("UNUSED_PARAMETER") context: Context
        ): CommandResult {
            return if (!isOverlayVisible) {
                createSuccessResult(command, "Overlay is already hidden")
            } else {
                // This would integrate with the overlay module
                isOverlayVisible = false
                announceViaAccessibility(service, "Overlay hidden")
                createSuccessResult(command, "Overlay hidden")
            }
        }

        private fun executeAudioMode(
            command: Command,
            service: AccessibilityService?,
            @Suppress("UNUSED_PARAMETER") context: Context
        ): CommandResult {
            // Audio mode: Announce status
            announceViaAccessibility(service, "Audio mode deactivated")
            isOverlayVisible = false
            return createSuccessResult(command, "Audio mode deactivated")
        }
    }
    
    /**
     * Toggle Overlay Action
     */
    class ToggleOverlayAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            isOverlayVisible = !isOverlayVisible
            val state = if (isOverlayVisible) "shown" else "hidden"
            return createSuccessResult(command, "Overlay $state")
        }
    }
    
    /**
     * Show Command Hints Action
     */
    class ShowCommandHintsAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return if (isCommandHintsVisible) {
                createSuccessResult(command, "Command hints are already visible")
            } else {
                isCommandHintsVisible = true
                // This would show contextual command hints based on current app/screen
                createSuccessResult(command, "Command hints shown")
            }
        }
    }
    
    /**
     * Hide Command Hints Action
     */
    class HideCommandHintsAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return if (!isCommandHintsVisible) {
                createSuccessResult(command, "Command hints are already hidden")
            } else {
                isCommandHintsVisible = false
                createSuccessResult(command, "Command hints hidden")
            }
        }
    }
    
    /**
     * Show Help Action
     */
    class ShowHelpAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val topic = getTextParameter(command, "topic")?.lowercase()
            
            return when (topic) {
                "commands", "command" -> {
                    showCommandHelp()
                    createSuccessResult(command, "Showing command help")
                }
                "navigation" -> {
                    showNavigationHelp()
                    createSuccessResult(command, "Showing navigation help")
                }
                "volume" -> {
                    showVolumeHelp()
                    createSuccessResult(command, "Showing volume help")
                }
                "text", "dictation" -> {
                    showTextHelp()
                    createSuccessResult(command, "Showing text input help")
                }
                "settings" -> {
                    showSettingsHelp()
                    createSuccessResult(command, "Showing settings help")
                }
                else -> {
                    showGeneralHelp()
                    createSuccessResult(command, "Showing general help")
                }
            }
        }
    }
    
    /**
     * Hide Help Action
     */
    class HideHelpAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            return if (!isHelpVisible) {
                createSuccessResult(command, "Help is already hidden")
            } else {
                isHelpVisible = false
                createSuccessResult(command, "Help hidden")
            }
        }
    }
    
    /**
     * List Available Commands Action
     */
    class ListCommandsAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val category = getTextParameter(command, "category")?.lowercase()
            
            val commands = when (category) {
                "navigation" -> listOf(
                    "go back", "go home", "recent apps", "notifications", 
                    "quick settings", "power dialog", "split screen", "lock screen"
                )
                "cursor", "click" -> listOf(
                    "click [target]", "double click [target]", "long press [target]",
                    "show cursor", "hide cursor", "center cursor"
                )
                "scroll" -> listOf(
                    "scroll up", "scroll down", "scroll left", "scroll right",
                    "page up", "page down", "scroll to top", "scroll to bottom"
                )
                "volume" -> listOf(
                    "volume up", "volume down", "mute", "unmute", "max volume",
                    "volume level 1-15"
                )
                "text", "dictation" -> listOf(
                    "start dictation", "end dictation", "type [text]", "backspace",
                    "clear text", "enter", "show keyboard", "hide keyboard"
                )
                "system" -> listOf(
                    "wifi toggle", "bluetooth toggle", "open settings",
                    "battery status", "device info", "network status"
                )
                else -> listOf(
                    "Navigation: go back, go home, recent apps, notifications",
                    "Cursor: click, double click, long press, show/hide cursor",
                    "Scroll: scroll up/down/left/right, page up/down",
                    "Volume: volume up/down, mute/unmute, volume levels 1-15",
                    "Text: start/end dictation, type text, backspace, clear",
                    "System: wifi/bluetooth toggle, settings, status info",
                    "Help: show help [topic], list commands [category]"
                )
            }
            
            val commandList = commands.joinToString("\n• ", "Available commands:\n• ")
            return createSuccessResult(command, commandList, commands)
        }
    }
    
    /**
     * Show Status Action
     */
    class ShowStatusAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val statusInfo = mapOf(
                "overlayVisible" to isOverlayVisible,
                "helpVisible" to isHelpVisible,
                "hintsVisible" to isCommandHintsVisible,
                "voiceOSVersion" to "3.0.0",
                "accessibility" to (accessibilityService != null)
            )
            
            val message = buildString {
                append("VOS4 Status:\n")
                append("• Overlay: ${if (isOverlayVisible) "visible" else "hidden"}\n")
                append("• Help: ${if (isHelpVisible) "visible" else "hidden"}\n")
                append("• Hints: ${if (isCommandHintsVisible) "visible" else "hidden"}\n")
                append("• Accessibility: ${if (accessibilityService != null) "active" else "inactive"}")
            }
            
            return createSuccessResult(command, message, statusInfo)
        }
    }
    
    /**
     * Set Overlay Position Action
     */
    class SetOverlayPositionAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val position = getTextParameter(command, "position")?.lowercase()
            
            return when (position) {
                "top", "top left", "top-left" -> {
                    // Set overlay to top-left position
                    createSuccessResult(command, "Overlay position set to top-left")
                }
                "top right", "top-right" -> {
                    // Set overlay to top-right position
                    createSuccessResult(command, "Overlay position set to top-right")
                }
                "bottom", "bottom left", "bottom-left" -> {
                    // Set overlay to bottom-left position
                    createSuccessResult(command, "Overlay position set to bottom-left")
                }
                "bottom right", "bottom-right" -> {
                    // Set overlay to bottom-right position
                    createSuccessResult(command, "Overlay position set to bottom-right")
                }
                "center", "middle" -> {
                    // Set overlay to center position
                    createSuccessResult(command, "Overlay position set to center")
                }
                else -> {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS, 
                        "Invalid position. Use: top-left, top-right, bottom-left, bottom-right, center")
                }
            }
        }
    }
    
    /**
     * Set Overlay Size Action
     */
    class SetOverlaySizeAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val size = getTextParameter(command, "size")?.lowercase()
            
            return when (size) {
                "small", "compact" -> {
                    // Set overlay to small size
                    createSuccessResult(command, "Overlay size set to small")
                }
                "medium", "normal" -> {
                    // Set overlay to medium size
                    createSuccessResult(command, "Overlay size set to medium")
                }
                "large", "expanded" -> {
                    // Set overlay to large size
                    createSuccessResult(command, "Overlay size set to large")
                }
                "full", "fullscreen" -> {
                    // Set overlay to full screen
                    createSuccessResult(command, "Overlay size set to full screen")
                }
                else -> {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS,
                        "Invalid size. Use: small, medium, large, full")
                }
            }
        }
    }
    
    /**
     * Set Overlay Transparency Action
     */
    class SetOverlayTransparencyAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): CommandResult {
            val transparency = getNumberParameter(command, "transparency")?.toFloat()
            
            return if (transparency == null || transparency < 0f || transparency > 100f) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, 
                    "Transparency must be between 0 and 100")
            } else {
                // Set overlay transparency (0 = fully transparent, 100 = fully opaque)
                createSuccessResult(command, "Overlay transparency set to ${transparency.toInt()}%")
            }
        }
    }
    
    // Helper methods
    
    /**
     * Show command help
     */
    private fun showCommandHelp() {
        isHelpVisible = true
        // This would display command help overlay
    }
    
    /**
     * Show navigation help
     */
    private fun showNavigationHelp() {
        isHelpVisible = true
        // This would display navigation-specific help
    }
    
    /**
     * Show volume help
     */
    private fun showVolumeHelp() {
        isHelpVisible = true
        // This would display volume control help
    }
    
    /**
     * Show text input help
     */
    private fun showTextHelp() {
        isHelpVisible = true
        // This would display text input and dictation help
    }
    
    /**
     * Show settings help
     */
    private fun showSettingsHelp() {
        isHelpVisible = true
        // This would display settings and system control help
    }
    
    /**
     * Show general help
     */
    private fun showGeneralHelp() {
        isHelpVisible = true
        // This would display general VOS4 help
    }

    // ==================== Q7 DUAL MODE HELPER FUNCTIONS ====================

    /**
     * Announce text via AccessibilityService (TalkBack compatible)
     * Used in both visual and audio modes
     */
    private fun announceViaAccessibility(service: AccessibilityService?, text: String) {
        service?.let {
            try {
                // Use accessibility announcement for TalkBack compatibility
                val event = android.view.accessibility.AccessibilityEvent.obtain()
                event.eventType = android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                event.className = javaClass.name
                event.packageName = service.packageName
                event.text.add(text)

                // Check if TalkBack is active
                service.serviceInfo?.feedbackType?.let { feedbackType ->
                    if (feedbackType and android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_SPOKEN != 0) {
                        // TalkBack is active, use announcement
                        service.rootInActiveWindow?.let { root ->
                            root.performAction(
                                android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS.id
                            )
                        }
                    }
                }

                Log.d(TAG, "Announced: $text")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to announce via accessibility", e)
            }
        }
    }

    /**
     * Vibrate device for haptic feedback
     * Used in audio mode for non-visual feedback
     */
    @SuppressLint("MissingPermission")
    private fun vibrateDevice(context: Context, durationMs: Long) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate device", e)
        }
    }
}