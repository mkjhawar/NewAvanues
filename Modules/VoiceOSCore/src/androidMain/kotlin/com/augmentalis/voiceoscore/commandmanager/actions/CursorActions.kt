/**
 * CursorActions.kt
 *
 * Created: 2025-10-10 18:01 PDT
 * Version: 4.0.0
 *
 * Purpose: Voice command handlers for VoiceCursor - uses CursorController directly
 * Module: CommandManager
 *
 * This class provides voice command execution by calling CursorController directly
 * and dispatching gestures (click, long press, scroll) via AndroidGestureDispatcher.
 * The intermediate VoiceCursorAPI bridge has been removed.
 *
 * Extracted from VoiceCursor/CursorCommandHandler as part of separation of concerns refactoring.
 *
 * Changelog:
 * - v4.0.0 (2026-02-06): Remove VoiceCursorAPI bridge, use CursorController + AndroidGestureDispatcher directly
 * - v3.0.0 (2025-10-10): Complete refactor - direct VoiceCursorAPI delegation pattern
 * - v2.0.0 (2025-08-19): BaseAction pattern implementation (backed up as .backup-251010-1801)
 * - v1.0.0: Initial version
 */

package com.augmentalis.voiceoscore.commandmanager.actions

import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.voicecursor.core.CursorConfig
import com.augmentalis.voicecursor.core.CursorController
import com.augmentalis.voicecursor.core.CursorInput
import com.augmentalis.voicecursor.core.CursorOffset
import com.augmentalis.voicecursor.core.CursorType
import com.augmentalis.voicecursor.core.CursorAction
import com.augmentalis.voiceoscore.AndroidGestureDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * CursorActions - Voice command handlers for VoiceCursor
 *
 * This object provides voice command execution by calling CursorController
 * directly for cursor state/movement, and AndroidGestureDispatcher for
 * gesture actions (click, long press, scroll).
 *
 * Design:
 * - All methods are suspend functions for consistency with command processing
 * - CursorController handles position, visibility, and configuration
 * - AndroidGestureDispatcher handles gesture dispatch via AccessibilityService
 * - All methods return Boolean indicating success/failure
 * - Errors are logged and returned as false
 *
 * Initialization:
 * - Call [initialize] with a CursorController and AndroidGestureDispatcher
 *   before using any cursor commands.
 */
object CursorActions {
    private const val TAG = "CursorActions"

    private var controller: CursorController? = null
    private var gestureDispatcher: AndroidGestureDispatcher? = null

    /**
     * Initialize CursorActions with required dependencies.
     *
     * @param cursorController The CursorController instance for position/state management
     * @param dispatcher The AndroidGestureDispatcher for click/scroll gestures (nullable for
     *                   environments without AccessibilityService; gesture actions will return false)
     */
    fun initialize(cursorController: CursorController, dispatcher: AndroidGestureDispatcher? = null) {
        controller = cursorController
        gestureDispatcher = dispatcher
        Log.d(TAG, "Initialized with CursorController" +
            if (dispatcher != null) " and AndroidGestureDispatcher" else " (no gesture dispatcher)")
    }

    /**
     * Check if CursorActions has been initialized.
     */
    fun isInitialized(): Boolean = controller != null

    // ========== Movement Commands ==========

    /**
     * Move cursor in specified direction by distance
     *
     * @param direction The direction to move (UP, DOWN, LEFT, RIGHT)
     * @param distance The distance in pixels (default: 50f)
     * @return true if cursor was moved successfully, false otherwise
     */
    suspend fun moveCursor(direction: CursorDirection, distance: Float = 50f): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot move cursor - CursorController not initialized")
                    return@withContext false
                }

                val state = ctrl.state.value
                if (!state.isVisible) {
                    Log.w(TAG, "Cannot move cursor - cursor not visible")
                    return@withContext false
                }

                val currentPosition = state.position
                val newX = when (direction) {
                    CursorDirection.LEFT -> currentPosition.x - distance
                    CursorDirection.RIGHT -> currentPosition.x + distance
                    else -> currentPosition.x
                }
                val newY = when (direction) {
                    CursorDirection.UP -> currentPosition.y - distance
                    CursorDirection.DOWN -> currentPosition.y + distance
                    else -> currentPosition.y
                }

                ctrl.update(
                    CursorInput.DirectPosition(newX, newY),
                    System.currentTimeMillis()
                )

                Log.d(TAG, "Moved cursor $direction by $distance pixels to ($newX, $newY)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to move cursor $direction", e)
                false
            }
        }
    }

    // ========== Click Actions ==========

    /**
     * Perform single click at current cursor position.
     *
     * Dispatches a tap gesture via AccessibilityService at the cursor's current coordinates.
     *
     * @return true if click was performed successfully, false otherwise
     */
    suspend fun click(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot click - CursorController not initialized")
                    return@withContext false
                }
                val dispatcher = gestureDispatcher ?: run {
                    Log.w(TAG, "Cannot click - AndroidGestureDispatcher not available")
                    return@withContext false
                }

                val position = ctrl.state.value.position
                val success = dispatcher.tap(position.x, position.y)

                if (success) {
                    Log.d(TAG, "Single click performed at (${position.x}, ${position.y})")
                } else {
                    Log.w(TAG, "Single click failed at (${position.x}, ${position.y})")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform click", e)
                false
            }
        }
    }

    /**
     * Perform double click at current cursor position.
     *
     * Dispatches two rapid tap gestures via AccessibilityService at the cursor's
     * current coordinates with a brief pause between them.
     *
     * @return true if double click was performed successfully, false otherwise
     */
    suspend fun doubleClick(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot double click - CursorController not initialized")
                    return@withContext false
                }
                val dispatcher = gestureDispatcher ?: run {
                    Log.w(TAG, "Cannot double click - AndroidGestureDispatcher not available")
                    return@withContext false
                }

                val position = ctrl.state.value.position
                val firstTap = dispatcher.tap(position.x, position.y)
                if (!firstTap) {
                    Log.w(TAG, "Double click failed - first tap failed")
                    return@withContext false
                }

                // Brief pause between taps (Android ViewConfiguration double-tap timeout is ~300ms;
                // keeping interval short to register as double-tap)
                delay(40L)

                val secondTap = dispatcher.tap(position.x, position.y)
                if (secondTap) {
                    Log.d(TAG, "Double click performed at (${position.x}, ${position.y})")
                } else {
                    Log.w(TAG, "Double click partially failed - second tap failed")
                }
                secondTap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform double click", e)
                false
            }
        }
    }

    /**
     * Perform long press at current cursor position.
     *
     * Dispatches a long press gesture via AccessibilityService (500ms hold)
     * at the cursor's current coordinates.
     *
     * @return true if long press was performed successfully, false otherwise
     */
    suspend fun longPress(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot long press - CursorController not initialized")
                    return@withContext false
                }
                val dispatcher = gestureDispatcher ?: run {
                    Log.w(TAG, "Cannot long press - AndroidGestureDispatcher not available")
                    return@withContext false
                }

                val position = ctrl.state.value.position
                val success = dispatcher.longPress(position.x, position.y)

                if (success) {
                    Log.d(TAG, "Long press performed at (${position.x}, ${position.y})")
                } else {
                    Log.w(TAG, "Long press failed at (${position.x}, ${position.y})")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform long press", e)
                false
            }
        }
    }

    // ========== System Commands ==========

    /**
     * Show cursor overlay with optional configuration
     *
     * @param config The cursor configuration to use (default: CursorConfig())
     * @return true if cursor was shown successfully, false otherwise
     */
    suspend fun showCursor(config: CursorConfig = CursorConfig()): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot show cursor - CursorController not initialized")
                    return@withContext false
                }

                ctrl.updateConfig(config)
                ctrl.setVisible(true)

                Log.d(TAG, "Cursor shown")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show cursor", e)
                false
            }
        }
    }

    /**
     * Hide cursor overlay
     *
     * @return true if cursor was hidden successfully, false otherwise
     */
    suspend fun hideCursor(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot hide cursor - CursorController not initialized")
                    return@withContext false
                }

                ctrl.setVisible(false)

                Log.d(TAG, "Cursor hidden")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide cursor", e)
                false
            }
        }
    }

    /**
     * Center cursor on screen
     *
     * @return true if cursor was centered successfully, false otherwise
     */
    suspend fun centerCursor(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot center cursor - CursorController not initialized")
                    return@withContext false
                }

                ctrl.resetToCenter()

                Log.d(TAG, "Cursor centered")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to center cursor", e)
                false
            }
        }
    }

    /**
     * Toggle cursor visibility
     *
     * @return true if toggle was successful, false otherwise
     */
    suspend fun toggleCursor(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot toggle cursor - CursorController not initialized")
                    return@withContext false
                }

                val isCurrentlyVisible = ctrl.state.value.isVisible
                ctrl.setVisible(!isCurrentlyVisible)

                Log.d(TAG, "Cursor visibility toggled to: ${!isCurrentlyVisible}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle cursor", e)
                false
            }
        }
    }

    // ========== Configuration Commands ==========

    /**
     * Show cursor coordinates display
     *
     * @return true if coordinates were shown successfully, false otherwise
     */
    suspend fun showCoordinates(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot show coordinates - CursorController not initialized")
                    return@withContext false
                }

                val currentConfig = ctrl.getConfig()
                ctrl.updateConfig(currentConfig.copy(showCoordinates = true))

                Log.d(TAG, "Cursor coordinates shown")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show coordinates", e)
                false
            }
        }
    }

    /**
     * Hide cursor coordinates display
     *
     * @return true if coordinates were hidden successfully, false otherwise
     */
    suspend fun hideCoordinates(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot hide coordinates - CursorController not initialized")
                    return@withContext false
                }

                val currentConfig = ctrl.getConfig()
                ctrl.updateConfig(currentConfig.copy(showCoordinates = false))

                Log.d(TAG, "Cursor coordinates hidden")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide coordinates", e)
                false
            }
        }
    }

    /**
     * Toggle cursor coordinates display
     *
     * @return true if coordinates were toggled successfully, false otherwise
     */
    suspend fun toggleCoordinates(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot toggle coordinates - CursorController not initialized")
                    return@withContext false
                }

                val currentConfig = ctrl.getConfig()
                val newShowCoordinates = !currentConfig.showCoordinates
                ctrl.updateConfig(currentConfig.copy(showCoordinates = newShowCoordinates))

                Log.d(TAG, "Cursor coordinates toggled to: $newShowCoordinates")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle coordinates", e)
                false
            }
        }
    }

    /**
     * Set cursor type/appearance
     *
     * @param type The cursor type to set (Hand, Normal, Custom)
     * @return true if cursor type was set successfully, false otherwise
     */
    suspend fun setCursorType(type: CursorType): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot set cursor type - CursorController not initialized")
                    return@withContext false
                }

                val currentConfig = ctrl.getConfig()
                ctrl.updateConfig(currentConfig.copy(type = type))

                Log.d(TAG, "Cursor type set to: $type")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set cursor type", e)
                false
            }
        }
    }

    // ========== Advanced Commands ==========

    /**
     * Show cursor context menu
     *
     * @return true if menu was shown successfully, false otherwise
     */
    suspend fun showMenu(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot show menu - CursorController not initialized")
                    return@withContext false
                }

                // Menu is integrated into cursor view, so just ensure cursor is visible
                ctrl.setVisible(true)

                Log.d(TAG, "Cursor menu shown (integrated in cursor view)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show cursor menu", e)
                false
            }
        }
    }

    /**
     * Open cursor settings activity
     *
     * @param context The context to use for starting the activity
     * @return true if settings were opened successfully, false otherwise
     */
    suspend fun openSettings(context: Context): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent().apply {
                    setClassName(
                        context.packageName,
                        "com.augmentalis.voiceos.cursor.ui.VoiceCursorSettingsActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Log.d(TAG, "Cursor settings opened")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open cursor settings", e)
                false
            }
        }
    }

    /**
     * Calibrate cursor tracking
     * Note: Calibration is handled internally by VoiceCursor/IMU integration
     *
     * @return true if calibration was initiated successfully, false otherwise
     */
    suspend fun calibrate(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                // Calibration resets the filter and gaze state via resetToCenter
                val ctrl = controller ?: run {
                    Log.w(TAG, "Cannot calibrate - CursorController not initialized")
                    return@withContext false
                }

                ctrl.resetToCenter()
                Log.d(TAG, "Cursor calibrated (reset to center)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to calibrate cursor", e)
                false
            }
        }
    }

    // ========== Scrolling Commands ==========

    /**
     * Scroll up at current cursor position.
     *
     * Dispatches a scroll-up gesture via AccessibilityService starting from
     * the cursor's current coordinates.
     *
     * @return true if scroll was performed successfully, false otherwise
     */
    suspend fun scrollUp(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val dispatcher = gestureDispatcher ?: run {
                    Log.w(TAG, "Cannot scroll - AndroidGestureDispatcher not available")
                    return@withContext false
                }

                // If we have a cursor position, scroll from there; otherwise use screen center
                val ctrl = controller
                val bounds = if (ctrl != null) {
                    val pos = ctrl.state.value.position
                    com.augmentalis.voiceoscore.Bounds(
                        left = pos.x.toInt() - 1,
                        top = pos.y.toInt() - 1,
                        right = pos.x.toInt() + 1,
                        bottom = pos.y.toInt() + 1
                    )
                } else {
                    null
                }

                val success = dispatcher.scroll("up", bounds)
                if (success) {
                    Log.d(TAG, "Scroll up performed")
                } else {
                    Log.w(TAG, "Scroll up failed")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scroll up", e)
                false
            }
        }
    }

    /**
     * Scroll down at current cursor position.
     *
     * Dispatches a scroll-down gesture via AccessibilityService starting from
     * the cursor's current coordinates.
     *
     * @return true if scroll was performed successfully, false otherwise
     */
    suspend fun scrollDown(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val dispatcher = gestureDispatcher ?: run {
                    Log.w(TAG, "Cannot scroll - AndroidGestureDispatcher not available")
                    return@withContext false
                }

                val ctrl = controller
                val bounds = if (ctrl != null) {
                    val pos = ctrl.state.value.position
                    com.augmentalis.voiceoscore.Bounds(
                        left = pos.x.toInt() - 1,
                        top = pos.y.toInt() - 1,
                        right = pos.x.toInt() + 1,
                        bottom = pos.y.toInt() + 1
                    )
                } else {
                    null
                }

                val success = dispatcher.scroll("down", bounds)
                if (success) {
                    Log.d(TAG, "Scroll down performed")
                } else {
                    Log.w(TAG, "Scroll down failed")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scroll down", e)
                false
            }
        }
    }
}

/**
 * Cursor movement directions
 */
enum class CursorDirection {
    UP, DOWN, LEFT, RIGHT
}
