/**
 * DeviceHandler.kt - Handles device control commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP handler for device control actions (volume, brightness, screen, flashlight).
 * Part of Phase 12 handler system implementation.
 */
package com.augmentalis.voiceoscoreng.handlers

/**
 * Enum representing different device control types.
 */
enum class DeviceControl {
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    SCREEN_ON,
    SCREEN_OFF,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    FLASHLIGHT_TOGGLE
}

/**
 * Result of a device control operation.
 *
 * @param success Whether the operation succeeded
 * @param control The type of control that was executed
 * @param currentValue The current value after the operation (for volume/brightness, 0-100)
 * @param error Error message if operation failed
 */
data class DeviceControlResult(
    val success: Boolean,
    val control: DeviceControl,
    val currentValue: Int? = null,
    val error: String? = null
)

/**
 * Interface for platform-specific device control operations.
 *
 * Implementations should handle the actual hardware/system calls
 * on each platform (Android, iOS, Desktop).
 */
interface IDeviceController {
    /**
     * Set the device volume level.
     *
     * @param level Volume level (0-100)
     * @return true if successful
     */
    fun setVolume(level: Int): Boolean

    /**
     * Get the current device volume level.
     *
     * @return Volume level (0-100)
     */
    fun getVolume(): Int

    /**
     * Set the device brightness level.
     *
     * @param level Brightness level (0-100)
     * @return true if successful
     */
    fun setBrightness(level: Int): Boolean

    /**
     * Get the current device brightness level.
     *
     * @return Brightness level (0-100)
     */
    fun getBrightness(): Int

    /**
     * Set the screen state (on/off).
     *
     * @param on true to turn screen on, false to turn off
     * @return true if successful
     */
    fun setScreenState(on: Boolean): Boolean

    /**
     * Set the flashlight state (on/off).
     *
     * @param on true to turn flashlight on, false to turn off
     * @return true if successful
     */
    fun setFlashlight(on: Boolean): Boolean
}

/**
 * Handler for device control commands.
 *
 * Supports:
 * - Volume: up, down, mute, set volume N
 * - Brightness: up, down, set brightness N
 * - Screen: on, off
 * - Flashlight: on, off, toggle
 *
 * @param deviceController Optional platform-specific controller for actual hardware control
 * @param initialVolume Initial volume level (default 50)
 * @param initialBrightness Initial brightness level (default 50)
 */
class DeviceHandler(
    private val deviceController: IDeviceController? = null,
    initialVolume: Int = 50,
    initialBrightness: Int = 50
) {
    private var currentVolume: Int = initialVolume.coerceIn(0, 100)
    private var currentBrightness: Int = initialBrightness.coerceIn(0, 100)
    private var flashlightOn: Boolean = false

    companion object {
        /** Step size for volume adjustments */
        const val VOLUME_STEP = 10
        /** Step size for brightness adjustments */
        const val BRIGHTNESS_STEP = 10
    }

    /**
     * Handle a device control command string.
     *
     * @param command The command to process (e.g., "volume up", "brightness down")
     * @return Result of the operation
     */
    fun handleCommand(command: String): DeviceControlResult {
        val normalizedCommand = command.lowercase().trim()

        if (normalizedCommand.isEmpty()) {
            return DeviceControlResult(
                success = false,
                control = DeviceControl.VOLUME_UP,
                error = "Empty command"
            )
        }

        return when {
            normalizedCommand == "volume up" -> volumeUp()
            normalizedCommand == "volume down" -> volumeDown()
            normalizedCommand == "mute" -> mute()
            normalizedCommand == "brightness up" -> brightnessUp()
            normalizedCommand == "brightness down" -> brightnessDown()
            normalizedCommand == "screen on" -> screenOn()
            normalizedCommand == "screen off" -> screenOff()
            normalizedCommand == "flashlight on" || normalizedCommand == "turn on flashlight" -> flashlightOn()
            normalizedCommand == "flashlight off" || normalizedCommand == "turn off flashlight" -> flashlightOff()
            normalizedCommand == "toggle flashlight" -> toggleFlashlight()
            normalizedCommand.startsWith("set volume ") -> setVolumeTo(command)
            normalizedCommand.startsWith("set brightness ") -> setBrightnessTo(command)
            else -> DeviceControlResult(
                success = false,
                control = DeviceControl.VOLUME_UP,
                error = "Unknown device command: $command"
            )
        }
    }

    /**
     * Increase volume by VOLUME_STEP.
     *
     * @return Result with new volume level
     */
    fun volumeUp(): DeviceControlResult {
        val newVolume = (currentVolume + VOLUME_STEP).coerceIn(0, 100)
        currentVolume = newVolume
        deviceController?.setVolume(newVolume)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.VOLUME_UP,
            currentValue = newVolume
        )
    }

    /**
     * Decrease volume by VOLUME_STEP.
     *
     * @return Result with new volume level
     */
    fun volumeDown(): DeviceControlResult {
        val newVolume = (currentVolume - VOLUME_STEP).coerceIn(0, 100)
        currentVolume = newVolume
        deviceController?.setVolume(newVolume)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.VOLUME_DOWN,
            currentValue = newVolume
        )
    }

    /**
     * Mute (set volume to 0).
     *
     * @return Result with volume at 0
     */
    fun mute(): DeviceControlResult {
        currentVolume = 0
        deviceController?.setVolume(0)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.VOLUME_MUTE,
            currentValue = 0
        )
    }

    /**
     * Increase brightness by BRIGHTNESS_STEP.
     *
     * @return Result with new brightness level
     */
    fun brightnessUp(): DeviceControlResult {
        val newBrightness = (currentBrightness + BRIGHTNESS_STEP).coerceIn(0, 100)
        currentBrightness = newBrightness
        deviceController?.setBrightness(newBrightness)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.BRIGHTNESS_UP,
            currentValue = newBrightness
        )
    }

    /**
     * Decrease brightness by BRIGHTNESS_STEP.
     *
     * @return Result with new brightness level
     */
    fun brightnessDown(): DeviceControlResult {
        val newBrightness = (currentBrightness - BRIGHTNESS_STEP).coerceIn(0, 100)
        currentBrightness = newBrightness
        deviceController?.setBrightness(newBrightness)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.BRIGHTNESS_DOWN,
            currentValue = newBrightness
        )
    }

    /**
     * Turn screen on.
     *
     * @return Result indicating success
     */
    fun screenOn(): DeviceControlResult {
        deviceController?.setScreenState(true)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.SCREEN_ON
        )
    }

    /**
     * Turn screen off.
     *
     * @return Result indicating success
     */
    fun screenOff(): DeviceControlResult {
        deviceController?.setScreenState(false)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.SCREEN_OFF
        )
    }

    /**
     * Turn flashlight on.
     *
     * @return Result indicating success
     */
    fun flashlightOn(): DeviceControlResult {
        flashlightOn = true
        deviceController?.setFlashlight(true)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.FLASHLIGHT_ON
        )
    }

    /**
     * Turn flashlight off.
     *
     * @return Result indicating success
     */
    fun flashlightOff(): DeviceControlResult {
        flashlightOn = false
        deviceController?.setFlashlight(false)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.FLASHLIGHT_OFF
        )
    }

    /**
     * Toggle flashlight state.
     *
     * @return Result indicating success with new state
     */
    fun toggleFlashlight(): DeviceControlResult {
        flashlightOn = !flashlightOn
        deviceController?.setFlashlight(flashlightOn)

        return DeviceControlResult(
            success = true,
            control = DeviceControl.FLASHLIGHT_TOGGLE
        )
    }

    /**
     * Get current volume level.
     *
     * @return Volume level (0-100)
     */
    fun getCurrentVolume(): Int = currentVolume

    /**
     * Get current brightness level.
     *
     * @return Brightness level (0-100)
     */
    fun getCurrentBrightness(): Int = currentBrightness

    /**
     * Check if flashlight is on.
     *
     * @return true if flashlight is on
     */
    fun isFlashlightOn(): Boolean = flashlightOn

    // ==================== Private Helpers ====================

    /**
     * Set volume to a specific level from command string.
     * Parses "set volume N" format.
     */
    private fun setVolumeTo(command: String): DeviceControlResult {
        val valueStr = command.lowercase().trim().removePrefix("set volume ").trim()

        return try {
            val value = valueStr.toInt().coerceIn(0, 100)
            currentVolume = value
            deviceController?.setVolume(value)

            DeviceControlResult(
                success = true,
                control = DeviceControl.VOLUME_UP, // Uses VOLUME_UP for set operations
                currentValue = value
            )
        } catch (e: NumberFormatException) {
            DeviceControlResult(
                success = false,
                control = DeviceControl.VOLUME_UP,
                error = "Invalid volume value: $valueStr"
            )
        }
    }

    /**
     * Set brightness to a specific level from command string.
     * Parses "set brightness N" format.
     */
    private fun setBrightnessTo(command: String): DeviceControlResult {
        val valueStr = command.lowercase().trim().removePrefix("set brightness ").trim()

        return try {
            val value = valueStr.toInt().coerceIn(0, 100)
            currentBrightness = value
            deviceController?.setBrightness(value)

            DeviceControlResult(
                success = true,
                control = DeviceControl.BRIGHTNESS_UP, // Uses BRIGHTNESS_UP for set operations
                currentValue = value
            )
        } catch (e: NumberFormatException) {
            DeviceControlResult(
                success = false,
                control = DeviceControl.BRIGHTNESS_UP,
                error = "Invalid brightness value: $valueStr"
            )
        }
    }
}
