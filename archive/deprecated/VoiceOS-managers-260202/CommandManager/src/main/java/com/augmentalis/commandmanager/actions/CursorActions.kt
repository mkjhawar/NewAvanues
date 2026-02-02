/**
 * CursorActions.kt
 *
 * Created: 2025-10-10 18:01 PDT
 * Author: VOS4 Development Team
 * Version: 3.0.0
 *
 * Purpose: Voice command handlers for VoiceCursor - delegates to VoiceCursorAPI
 * Module: CommandManager
 *
 * This class provides voice command execution by delegating to VoiceCursorAPI.
 * All command logic is centralized here, keeping VoiceCursor focused on cursor mechanics.
 *
 * Extracted from VoiceCursor/CursorCommandHandler as part of separation of concerns refactoring.
 *
 * Changelog:
 * - v3.0.0 (2025-10-10): Complete refactor - direct VoiceCursorAPI delegation pattern
 * - v2.0.0 (2025-08-19): BaseAction pattern implementation (backed up as .backup-251010-1801)
 * - v1.0.0: Initial version
 */

package com.augmentalis.commandmanager.actions

import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.voiceos.cursor.VoiceCursorAPI
import com.augmentalis.voiceos.cursor.core.CursorConfig
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.core.CursorType
import com.augmentalis.voiceos.cursor.view.CursorAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CursorActions - Voice command handlers for VoiceCursor
 *
 * This object provides voice command execution by delegating to VoiceCursorAPI.
 * All cursor operations go through the public API, maintaining clean separation.
 *
 * Design:
 * - All methods are suspend functions for consistency with command processing
 * - Pure delegation pattern - no business logic here
 * - All methods return Boolean indicating success/failure
 * - Errors are logged and returned as false
 */
object CursorActions {
    private const val TAG = "CursorActions"

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
                val currentPosition = VoiceCursorAPI.getCurrentPosition() ?: run {
                    Log.w(TAG, "Cannot move cursor - cursor not visible or not initialized")
                    return@withContext false
                }

                val newPosition = when (direction) {
                    CursorDirection.UP -> currentPosition.copy(y = currentPosition.y - distance)
                    CursorDirection.DOWN -> currentPosition.copy(y = currentPosition.y + distance)
                    CursorDirection.LEFT -> currentPosition.copy(x = currentPosition.x - distance)
                    CursorDirection.RIGHT -> currentPosition.copy(x = currentPosition.x + distance)
                }

                // Use VoiceCursorAPI.moveTo() to move cursor to new position
                val success = VoiceCursorAPI.moveTo(newPosition, animate = true)
                if (success) {
                    Log.d(TAG, "Moved cursor $direction by $distance pixels to (${newPosition.x}, ${newPosition.y})")
                } else {
                    Log.w(TAG, "Failed to move cursor $direction")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to move cursor $direction", e)
                false
            }
        }
    }

    // ========== Click Actions ==========

    /**
     * Perform single click at current cursor position
     *
     * @return true if click was performed successfully, false otherwise
     */
    suspend fun click(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val success = VoiceCursorAPI.click()
                if (success) {
                    Log.d(TAG, "Single click performed")
                } else {
                    Log.w(TAG, "Single click failed")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform click", e)
                false
            }
        }
    }

    /**
     * Perform double click at current cursor position
     *
     * @return true if double click was performed successfully, false otherwise
     */
    suspend fun doubleClick(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val success = VoiceCursorAPI.doubleClick()
                if (success) {
                    Log.d(TAG, "Double click performed")
                } else {
                    Log.w(TAG, "Double click failed")
                }
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to perform double click", e)
                false
            }
        }
    }

    /**
     * Perform long press at current cursor position
     *
     * @return true if long press was performed successfully, false otherwise
     */
    suspend fun longPress(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val success = VoiceCursorAPI.longPress()
                if (success) {
                    Log.d(TAG, "Long press performed")
                } else {
                    Log.w(TAG, "Long press failed")
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
                val success = VoiceCursorAPI.showCursor(config)
                if (success) {
                    Log.d(TAG, "Cursor shown")
                } else {
                    Log.w(TAG, "Failed to show cursor")
                }
                success
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
                val success = VoiceCursorAPI.hideCursor()
                if (success) {
                    Log.d(TAG, "Cursor hidden")
                } else {
                    Log.w(TAG, "Failed to hide cursor")
                }
                success
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
                val success = VoiceCursorAPI.centerCursor()
                if (success) {
                    Log.d(TAG, "Cursor centered")
                } else {
                    Log.w(TAG, "Failed to center cursor")
                }
                success
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
                val success = VoiceCursorAPI.toggleCursor()
                if (success) {
                    Log.d(TAG, "Cursor visibility toggled")
                } else {
                    Log.w(TAG, "Failed to toggle cursor")
                }
                success
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
                // Get current config, update showCoordinates, apply
                val currentConfig = getCurrentConfig()
                val newConfig = currentConfig.copy(showCoordinates = true)
                val success = VoiceCursorAPI.updateConfiguration(newConfig)
                if (success) {
                    Log.d(TAG, "Cursor coordinates shown")
                } else {
                    Log.w(TAG, "Failed to show coordinates")
                }
                success
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
                val currentConfig = getCurrentConfig()
                val newConfig = currentConfig.copy(showCoordinates = false)
                val success = VoiceCursorAPI.updateConfiguration(newConfig)
                if (success) {
                    Log.d(TAG, "Cursor coordinates hidden")
                } else {
                    Log.w(TAG, "Failed to hide coordinates")
                }
                success
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
                val currentConfig = getCurrentConfig()
                val newConfig = currentConfig.copy(showCoordinates = !currentConfig.showCoordinates)
                val success = VoiceCursorAPI.updateConfiguration(newConfig)
                if (success) {
                    Log.d(TAG, "Cursor coordinates toggled to: ${newConfig.showCoordinates}")
                } else {
                    Log.w(TAG, "Failed to toggle coordinates")
                }
                success
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
                val currentConfig = getCurrentConfig()
                val newConfig = currentConfig.copy(type = type)
                val success = VoiceCursorAPI.updateConfiguration(newConfig)
                if (success) {
                    Log.d(TAG, "Cursor type set to: $type")
                } else {
                    Log.w(TAG, "Failed to set cursor type")
                }
                success
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
                // Menu is integrated into cursor view, so just ensure cursor is visible
                val success = VoiceCursorAPI.showCursor()
                if (success) {
                    Log.d(TAG, "Cursor menu shown (integrated in cursor view)")
                } else {
                    Log.w(TAG, "Failed to show cursor menu")
                }
                success
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
                // Calibration is internal to VoiceCursor - no public API method yet
                // For now, log the request
                Log.d(TAG, "Cursor calibration requested (requires VoiceCursor internal access)")
                // TODO: Add calibration method to VoiceCursorAPI
                false // Return false until API method is added
            } catch (e: Exception) {
                Log.e(TAG, "Failed to calibrate cursor", e)
                false
            }
        }
    }

    // ========== Scrolling Commands ==========

    /**
     * Scroll up at current cursor position
     *
     * @return true if scroll was performed successfully, false otherwise
     */
    suspend fun scrollUp(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val success = VoiceCursorAPI.scrollUp()
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
     * Scroll down at current cursor position
     *
     * @return true if scroll was performed successfully, false otherwise
     */
    suspend fun scrollDown(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val success = VoiceCursorAPI.scrollDown()
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

    // ========== Helper Methods ==========

    /**
     * Get current cursor configuration
     * Returns default config if cursor is not initialized
     */
    private fun getCurrentConfig(): CursorConfig {
        // VoiceCursorAPI doesn't expose getCurrentConfig()
        // For now, return default config
        // TODO: Add VoiceCursorAPI.getCurrentConfig() method
        return CursorConfig()
    }
}

/**
 * Cursor movement directions
 */
enum class CursorDirection {
    UP, DOWN, LEFT, RIGHT
}
