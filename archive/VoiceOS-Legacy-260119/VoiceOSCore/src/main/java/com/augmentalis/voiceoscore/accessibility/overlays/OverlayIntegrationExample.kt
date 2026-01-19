/**
 * OverlayIntegrationExample.kt - Integration examples for voice feedback overlays
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.overlays

import android.accessibilityservice.AccessibilityService
import android.graphics.Point
import android.graphics.Rect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
import com.augmentalis.voiceos.speech.confidence.ConfidenceResult
import com.augmentalis.voiceos.speech.confidence.ScoringMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * EXAMPLE: Integration with VoiceAccessibilityService
 *
 * Add this to your VoiceAccessibilityService class:
 */
class OverlayIntegrationExample {

    companion object {
        /**
         * EXAMPLE 1: Initialize OverlayManager in AccessibilityService
         *
         * In VoiceAccessibilityService.onCreate():
         */
        fun exampleInitialization(service: AccessibilityService) {
            // Initialize the overlay manager singleton
            val overlayManager = OverlayManager.getInstance(service)

            // Note: OverlayManager automatically handles overlay lifecycle
            // No additional initialization needed
        }

        /**
         * EXAMPLE 2: Show confidence during speech recognition
         *
         * Call this when you receive recognition results with confidence scores:
         */
        fun exampleConfidenceDisplay(service: AccessibilityService) {
            val overlayManager = OverlayManager.getInstance(service)

            // Create a confidence result (normally from your speech engine)
            val result = ConfidenceResult(
                text = "open settings",
                confidence = 0.92f,  // 92% confidence
                level = ConfidenceLevel.HIGH,
                alternates = emptyList(),
                scoringMethod = ScoringMethod.VOSK_ACOUSTIC
            )

            // Show the confidence overlay
            overlayManager.showConfidence(result)

            // Update confidence in real-time as recognition progresses
            CoroutineScope(Dispatchers.Main).launch {
                delay(500)
                overlayManager.updateConfidence(
                    result.copy(
                        confidence = 0.95f,
                        text = "open settings app"
                    )
                )
            }

            // Hide after command execution
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                overlayManager.hideConfidence()
            }
        }

        /**
         * EXAMPLE 3: Show numbered selection for disambiguation
         *
         * Call this when multiple items match a voice command:
         */
        fun exampleNumberedSelection(service: AccessibilityService) {
            val overlayManager = OverlayManager.getInstance(service)

            // Create selectable items (from UI elements, search results, etc.)
            val items = listOf(
                SelectableItem(
                    number = 1,
                    label = "Settings",
                    bounds = Rect(100, 200, 300, 250),
                    action = { /* Open Settings app */ }
                ),
                SelectableItem(
                    number = 2,
                    label = "System Settings",
                    bounds = Rect(100, 300, 300, 350),
                    action = { /* Open System Settings */ }
                ),
                SelectableItem(
                    number = 3,
                    label = "Network Settings",
                    bounds = Rect(100, 400, 300, 450),
                    action = { /* Open Network Settings */ }
                )
            )

            // Show numbered selection overlay
            overlayManager.showNumberedSelection(items)

            // User says "select 2" - you handle the voice input and call:
            val success = overlayManager.selectNumberedItem(2)
            if (success) {
                overlayManager.hideNumberedSelection()
            }
        }

        /**
         * EXAMPLE 4: Show command status during processing
         *
         * Call this to show command execution progress:
         */
        fun exampleCommandStatus(service: AccessibilityService) {
            val overlayManager = OverlayManager.getInstance(service)

            // Show listening state
            overlayManager.showListening("Listening...")

            // Simulate command processing flow
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)

                // Show processing state
                overlayManager.showProcessing("open camera")

                delay(500)

                // Show executing state
                overlayManager.showExecuting("open camera")

                delay(500)

                // Show success
                overlayManager.showSuccess("open camera", "Camera opened")

                // Auto-dismiss after 2 seconds
                overlayManager.dismissAfterDelay(2000)
            }
        }

        /**
         * EXAMPLE 5: Show error state
         */
        fun exampleErrorHandling(service: AccessibilityService) {
            val overlayManager = OverlayManager.getInstance(service)

            overlayManager.showError(
                command = "unknown command xyz",
                error = "Command not recognized"
            )

            // Auto-dismiss after showing error
            CoroutineScope(Dispatchers.Main).launch {
                delay(3000)
                overlayManager.hideCommandStatus()
            }
        }

        /**
         * EXAMPLE 6: Show context menu for available commands
         *
         * Call this when user says "show menu" or "what can I say":
         */
        fun exampleContextMenu(service: AccessibilityService) {
            val overlayManager = OverlayManager.getInstance(service)

            // Create menu items with voice-selectable numbers
            val menuItems = listOf(
                MenuItem(
                    id = "scroll_down",
                    label = "Scroll Down",
                    icon = Icons.Default.ArrowDownward,
                    number = 1,
                    action = { /* Perform scroll down */ }
                ),
                MenuItem(
                    id = "scroll_up",
                    label = "Scroll Up",
                    icon = Icons.Default.ArrowUpward,
                    number = 2,
                    action = { /* Perform scroll up */ }
                ),
                MenuItem(
                    id = "go_back",
                    label = "Go Back",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    number = 3,
                    action = { /* Perform back navigation */ }
                ),
                MenuItem(
                    id = "go_home",
                    label = "Go Home",
                    icon = Icons.Default.Home,
                    number = 4,
                    action = { /* Go to home screen */ }
                )
            )

            // Show menu at center of screen
            overlayManager.showContextMenu(
                items = menuItems,
                title = "Available Commands"
            )

            // Or show at specific position (e.g., cursor location)
            overlayManager.showContextMenuAt(
                items = menuItems,
                position = Point(500, 300),
                title = "Voice Commands"
            )

            // User says "select 3" or just "three"
            overlayManager.selectContextMenuByNumber(3)
            overlayManager.hideContextMenu()
        }

        /**
         * EXAMPLE 7: Complete workflow - voice command to execution
         *
         * This demonstrates the full flow of a voice command:
         */
        fun exampleCompleteWorkflow(service: AccessibilityService) {
            val overlayManager = OverlayManager.getInstance(service)

            CoroutineScope(Dispatchers.Main).launch {
                // 1. Show listening state
                overlayManager.showListening()

                delay(1000)

                // 2. User speaks - show what was heard
                overlayManager.showProcessing("open settings")

                // 3. Show confidence while processing
                val confidenceResult = ConfidenceResult(
                    text = "open settings",
                    confidence = 0.88f,
                    level = ConfidenceLevel.HIGH,
                    alternates = emptyList(),
                    scoringMethod = ScoringMethod.VOSK_ACOUSTIC
                )
                overlayManager.showConfidence(confidenceResult)

                delay(500)

                // 4. If multiple matches, show numbered selection
                val matches = listOf(
                    SelectableItem(1, "Settings", Rect(0, 0, 100, 100), {}),
                    SelectableItem(2, "System Settings", Rect(0, 100, 100, 200), {})
                )

                if (matches.size > 1) {
                    overlayManager.hideCommandStatus()
                    overlayManager.showNumberedSelection(matches)

                    // Wait for user to say number...
                    delay(2000)
                    overlayManager.selectNumberedItem(1)
                    overlayManager.hideNumberedSelection()
                }

                // 5. Show executing state
                overlayManager.showExecuting("open settings")

                delay(500)

                // 6. Show success
                overlayManager.showSuccess("open settings", "Settings opened")

                // 7. Clean up
                delay(2000)
                overlayManager.hideConfidence()
                overlayManager.hideCommandStatus()
            }
        }

        /**
         * EXAMPLE 8: Cleanup in onDestroy()
         *
         * Add this to VoiceAccessibilityService.onDestroy():
         */
        fun exampleCleanup(service: AccessibilityService) {
            val overlayManager = OverlayManager.getInstance(service)
            overlayManager.dispose()
        }

        /**
         * EXAMPLE 9: Check overlay state
         */
        fun exampleCheckState(service: AccessibilityService) {
            val overlayManager = OverlayManager.getInstance(service)

            // Check if any overlay is visible
            if (overlayManager.isAnyVisible()) {
                // Hide all overlays
                overlayManager.hideAll()
            }

            // Check specific overlay
            if (overlayManager.isOverlayVisible("confidence")) {
                overlayManager.hideConfidence()
            }

            // Get list of active overlays
            val activeOverlays = overlayManager.getActiveOverlays()
            println("Active overlays: $activeOverlays")
        }
    }
}

/**
 * INTEGRATION NOTES:
 *
 * 1. PERMISSIONS:
 *    - TYPE_ACCESSIBILITY_OVERLAY requires AccessibilityService to be active
 *    - No additional SYSTEM_ALERT_WINDOW permission needed
 *    - All overlays work automatically when service is enabled
 *
 * 2. THREADING:
 *    - All overlay methods are main-thread safe
 *    - Can be called from background threads
 *    - State updates are synchronized
 *
 * 3. PERFORMANCE:
 *    - Overlays use lazy initialization
 *    - ComposeView is reused when possible
 *    - Minimal memory footprint when hidden
 *
 * 4. LIFECYCLE:
 *    - OverlayManager handles all lifecycle automatically
 *    - Overlays survive configuration changes
 *    - Call dispose() in onDestroy() to clean up
 *
 * 5. POSITIONING:
 *    - ConfidenceOverlay: Top-right corner (16dp margins)
 *    - CommandStatusOverlay: Top-center (80dp from top)
 *    - NumberedSelectionOverlay: Full screen with positioned badges
 *    - ContextMenuOverlay: Center or at specified point
 *
 * 6. MATERIAL DESIGN 3:
 *    - All overlays follow Material 3 design principles
 *    - Consistent color scheme across overlays
 *    - Smooth animations and transitions
 *    - Accessible contrast ratios
 *
 * 7. VOICE INTEGRATION:
 *    - NumberedSelectionOverlay: "select [number]"
 *    - ContextMenuOverlay: "select [number]" or command labels
 *    - CommandStatusOverlay: Shows current recognition state
 *    - ConfidenceOverlay: Real-time confidence feedback
 */
