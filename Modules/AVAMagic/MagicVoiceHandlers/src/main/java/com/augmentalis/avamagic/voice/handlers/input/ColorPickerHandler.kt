/**
 * ColorPickerHandler.kt - Voice handler for Color Picker interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven color picker control with named colors, RGB channels, and adjustments
 * Features:
 * - Set color by name (red, blue, green, etc.)
 * - Set individual RGB channels (0-255)
 * - Set alpha/opacity (0-100%)
 * - Adjust brightness (lighter/darker)
 * - Adjust saturation (more/less saturated)
 * - Set hex color values
 * - Named color picker targeting
 * - Focused color picker targeting
 * - AVID-based targeting for precise element selection
 * - Voice feedback for color changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Named colors:
 * - "set color to [color]" - Set to named color (red, blue, green, etc.)
 * - "[color]" - Shorthand color selection (e.g., "red", "blue")
 *
 * RGB channels:
 * - "set red to [value]" / "red [value]" - Set red channel (0-255)
 * - "set green to [value]" / "green [value]" - Set green channel (0-255)
 * - "set blue to [value]" / "blue [value]" - Set blue channel (0-255)
 *
 * Alpha/Opacity:
 * - "set opacity to [value]" / "alpha [value]" - Set alpha (0-100%)
 * - "set alpha to [value]" - Set alpha channel
 *
 * Brightness adjustment:
 * - "lighter" / "brighten" - Increase brightness
 * - "darker" / "darken" - Decrease brightness
 *
 * Saturation adjustment:
 * - "more saturated" / "saturate" - Increase saturation
 * - "less saturated" / "desaturate" - Decrease saturation
 *
 * Hex colors:
 * - "hex [code]" - Set hex color (e.g., "hex FF0000", "hex #00FF00")
 *
 * ## Color Parsing
 *
 * Supports:
 * - Named colors: "red", "blue", "green", "yellow", etc.
 * - RGB values: integers 0-255
 * - Percentages for alpha: "50%", "75 percent"
 * - Hex codes: "FF0000", "#FF0000", "0xFF0000"
 */

package com.augmentalis.avamagic.voice.handlers.input

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Color Picker interactions.
 *
 * Provides comprehensive voice control for color picker components including:
 * - Named color selection (red, blue, green, etc.)
 * - Individual RGB channel control
 * - Alpha/opacity adjustment
 * - Brightness and saturation modifications
 * - Hex color input
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for color picker operations
 */
class ColorPickerHandler(
    private val executor: ColorPickerExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "ColorPickerHandler"

        // Default adjustment amount for brightness/saturation
        private const val DEFAULT_BRIGHTNESS_STEP = 0.1
        private const val DEFAULT_SATURATION_STEP = 0.1

        // RGB channel bounds
        private const val RGB_MIN = 0
        private const val RGB_MAX = 255

        // Alpha bounds (0-100%)
        private const val ALPHA_MIN = 0
        private const val ALPHA_MAX = 100

        // Patterns for parsing commands
        private val SET_COLOR_TO_PATTERN = Regex(
            """^set\s+color\s+to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val SET_RED_PATTERN = Regex(
            """^(?:set\s+)?red\s+(?:to\s+)?(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        private val SET_GREEN_PATTERN = Regex(
            """^(?:set\s+)?green\s+(?:to\s+)?(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        private val SET_BLUE_PATTERN = Regex(
            """^(?:set\s+)?blue\s+(?:to\s+)?(\d+)$""",
            RegexOption.IGNORE_CASE
        )

        private val SET_ALPHA_PATTERN = Regex(
            """^(?:set\s+)?(?:alpha|opacity)\s+(?:to\s+)?(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val HEX_COLOR_PATTERN = Regex(
            """^hex\s+#?(?:0x)?([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$""",
            RegexOption.IGNORE_CASE
        )

        // Named colors mapping to hex values
        private val NAMED_COLORS = mapOf(
            // Primary colors
            "red" to "#FF0000",
            "green" to "#00FF00",
            "blue" to "#0000FF",

            // Secondary colors
            "yellow" to "#FFFF00",
            "cyan" to "#00FFFF",
            "magenta" to "#FF00FF",

            // Tertiary colors
            "orange" to "#FFA500",
            "purple" to "#800080",
            "violet" to "#EE82EE",
            "pink" to "#FFC0CB",
            "lime" to "#00FF00",
            "teal" to "#008080",
            "indigo" to "#4B0082",

            // Neutral colors
            "white" to "#FFFFFF",
            "black" to "#000000",
            "gray" to "#808080",
            "grey" to "#808080",
            "silver" to "#C0C0C0",

            // Extended palette
            "maroon" to "#800000",
            "olive" to "#808000",
            "navy" to "#000080",
            "aqua" to "#00FFFF",
            "fuchsia" to "#FF00FF",

            // Common web colors
            "coral" to "#FF7F50",
            "salmon" to "#FA8072",
            "crimson" to "#DC143C",
            "tomato" to "#FF6347",
            "gold" to "#FFD700",
            "khaki" to "#F0E68C",
            "lavender" to "#E6E6FA",
            "plum" to "#DDA0DD",
            "orchid" to "#DA70D6",
            "turquoise" to "#40E0D0",
            "skyblue" to "#87CEEB",
            "steelblue" to "#4682B4",
            "tan" to "#D2B48C",
            "chocolate" to "#D2691E",
            "sienna" to "#A0522D",
            "brown" to "#A52A2A",
            "beige" to "#F5F5DC",
            "ivory" to "#FFFFF0",
            "mint" to "#98FF98",
            "peach" to "#FFCBA4"
        )

        // Word to number mapping for common spoken numbers (0-255 for RGB)
        private val WORD_NUMBERS = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
            "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
            "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30,
            "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
            "eighty" to 80, "ninety" to 90, "hundred" to 100,
            "two hundred" to 200, "two fifty five" to 255, "two fifty" to 250
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Named color
        "set color to", "color",
        // RGB channels
        "set red to", "red",
        "set green to", "green",
        "set blue to", "blue",
        // Alpha
        "set alpha to", "alpha",
        "set opacity to", "opacity",
        // Brightness
        "lighter", "brighten",
        "darker", "darken",
        // Saturation
        "more saturated", "saturate",
        "less saturated", "desaturate",
        // Hex
        "hex"
    )

    /**
     * Callback for voice feedback when color value changes.
     */
    var onColorChanged: ((pickerName: String, newColor: ColorValue) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing color picker command: $normalizedAction")

        return try {
            when {
                // Set color by name: "set color to [color]"
                SET_COLOR_TO_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetColorByName(normalizedAction, command)
                }

                // Direct named color (single word like "red", "blue")
                NAMED_COLORS.containsKey(normalizedAction) -> {
                    handleDirectNamedColor(normalizedAction, command)
                }

                // Set red channel: "set red to [value]" or "red [value]"
                SET_RED_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetRed(normalizedAction, command)
                }

                // Set green channel: "set green to [value]" or "green [value]"
                SET_GREEN_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetGreen(normalizedAction, command)
                }

                // Set blue channel: "set blue to [value]" or "blue [value]"
                SET_BLUE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetBlue(normalizedAction, command)
                }

                // Set alpha/opacity: "set alpha to [value]" or "opacity [value]"
                SET_ALPHA_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetAlpha(normalizedAction, command)
                }

                // Set hex color: "hex [code]"
                HEX_COLOR_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetHex(normalizedAction, command)
                }

                // Brightness adjustments
                normalizedAction in listOf("lighter", "brighten") -> {
                    handleAdjustBrightness(increase = true, command = command)
                }

                normalizedAction in listOf("darker", "darken") -> {
                    handleAdjustBrightness(increase = false, command = command)
                }

                // Saturation adjustments
                normalizedAction in listOf("more saturated", "saturate") -> {
                    handleAdjustSaturation(increase = true, command = command)
                }

                normalizedAction in listOf("less saturated", "desaturate") -> {
                    handleAdjustSaturation(increase = false, command = command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing color picker command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ===============================================================================
    // Command Handlers
    // ===============================================================================

    /**
     * Handle "set color to [color]" command.
     */
    private suspend fun handleSetColorByName(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SET_COLOR_TO_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse set color command")

        val colorName = matchResult.groupValues[1].lowercase().trim()

        // Find the color picker
        val pickerInfo = findColorPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No color picker focused",
                recoverable = true,
                suggestedAction = "Focus on a color picker first"
            )

        // Look up the named color
        val hexColor = NAMED_COLORS[colorName]
            ?: return HandlerResult.Failure(
                reason = "Unknown color: '$colorName'",
                recoverable = true,
                suggestedAction = "Try 'set color to red', 'set color to blue', etc."
            )

        // Apply the color by name
        return applyColorByName(pickerInfo, colorName, hexColor)
    }

    /**
     * Handle direct named color command (single word like "red", "blue").
     */
    private suspend fun handleDirectNamedColor(
        colorName: String,
        command: QuantizedCommand
    ): HandlerResult {
        // Find the color picker
        val pickerInfo = findColorPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No color picker focused",
                recoverable = true,
                suggestedAction = "Focus on a color picker first"
            )

        // Look up the named color
        val hexColor = NAMED_COLORS[colorName]
            ?: return HandlerResult.notHandled()

        // Apply the color by name
        return applyColorByName(pickerInfo, colorName, hexColor)
    }

    /**
     * Handle "set red to [value]" or "red [value]" command.
     */
    private suspend fun handleSetRed(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SET_RED_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse red channel command")

        val valueString = matchResult.groupValues[1]
        val value = parseChannelValue(valueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse red value: '$valueString'",
                recoverable = true,
                suggestedAction = "Try 'set red to 128' or 'red 255'"
            )

        // Clamp to valid range
        val clampedValue = value.coerceIn(RGB_MIN, RGB_MAX)

        // Find the color picker
        val pickerInfo = findColorPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No color picker focused",
                recoverable = true,
                suggestedAction = "Focus on a color picker first"
            )

        // Apply the red channel
        return applyChannelChange(pickerInfo, ColorChannel.RED, clampedValue)
    }

    /**
     * Handle "set green to [value]" or "green [value]" command.
     */
    private suspend fun handleSetGreen(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SET_GREEN_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse green channel command")

        val valueString = matchResult.groupValues[1]
        val value = parseChannelValue(valueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse green value: '$valueString'",
                recoverable = true,
                suggestedAction = "Try 'set green to 128' or 'green 255'"
            )

        // Clamp to valid range
        val clampedValue = value.coerceIn(RGB_MIN, RGB_MAX)

        // Find the color picker
        val pickerInfo = findColorPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No color picker focused",
                recoverable = true,
                suggestedAction = "Focus on a color picker first"
            )

        // Apply the green channel
        return applyChannelChange(pickerInfo, ColorChannel.GREEN, clampedValue)
    }

    /**
     * Handle "set blue to [value]" or "blue [value]" command.
     */
    private suspend fun handleSetBlue(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SET_BLUE_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse blue channel command")

        val valueString = matchResult.groupValues[1]
        val value = parseChannelValue(valueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse blue value: '$valueString'",
                recoverable = true,
                suggestedAction = "Try 'set blue to 128' or 'blue 255'"
            )

        // Clamp to valid range
        val clampedValue = value.coerceIn(RGB_MIN, RGB_MAX)

        // Find the color picker
        val pickerInfo = findColorPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No color picker focused",
                recoverable = true,
                suggestedAction = "Focus on a color picker first"
            )

        // Apply the blue channel
        return applyChannelChange(pickerInfo, ColorChannel.BLUE, clampedValue)
    }

    /**
     * Handle "set alpha to [value]" or "opacity [value]" command.
     */
    private suspend fun handleSetAlpha(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SET_ALPHA_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse alpha command")

        val valueString = matchResult.groupValues[1]
        val parsedValue = parsePercentageValue(valueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse alpha value: '$valueString'",
                recoverable = true,
                suggestedAction = "Try 'set opacity to 50' or 'alpha 75%'"
            )

        // Find the color picker
        val pickerInfo = findColorPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No color picker focused",
                recoverable = true,
                suggestedAction = "Focus on a color picker first"
            )

        // Check if picker supports alpha
        if (!pickerInfo.supportsAlpha) {
            return HandlerResult.Failure(
                reason = "This color picker does not support alpha/opacity",
                recoverable = false
            )
        }

        // Clamp to valid percentage range
        val clampedValue = parsedValue.coerceIn(ALPHA_MIN, ALPHA_MAX)

        // Apply the alpha channel
        return applyAlphaChange(pickerInfo, clampedValue)
    }

    /**
     * Handle "hex [code]" command.
     */
    private suspend fun handleSetHex(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = HEX_COLOR_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse hex color command")

        val hexCode = matchResult.groupValues[1].uppercase()

        // Find the color picker
        val pickerInfo = findColorPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No color picker focused",
                recoverable = true,
                suggestedAction = "Focus on a color picker first"
            )

        // Apply the hex color
        return applyHexColor(pickerInfo, hexCode)
    }

    /**
     * Handle brightness adjustment (lighter/darker).
     */
    private suspend fun handleAdjustBrightness(
        increase: Boolean,
        command: QuantizedCommand
    ): HandlerResult {
        // Find the color picker
        val pickerInfo = findColorPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No color picker focused",
                recoverable = true,
                suggestedAction = "Focus on a color picker first"
            )

        val delta = if (increase) DEFAULT_BRIGHTNESS_STEP else -DEFAULT_BRIGHTNESS_STEP

        val result = executor.adjustBrightness(pickerInfo, delta)

        return if (result.success) {
            val directionWord = if (increase) "lighter" else "darker"

            // Invoke callback for voice feedback
            result.newColor?.let { newColor ->
                onColorChanged?.invoke(
                    pickerInfo.name.ifBlank { "Color picker" },
                    newColor
                )
            }

            val feedback = buildString {
                if (pickerInfo.name.isNotBlank()) {
                    append(pickerInfo.name)
                    append(" made ")
                } else {
                    append("Color made ")
                }
                append(directionWord)
            }

            Log.i(TAG, "Color brightness adjusted: ${pickerInfo.name} -> $directionWord")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerName" to pickerInfo.name,
                    "pickerAvid" to pickerInfo.avid,
                    "previousColor" to (pickerInfo.currentColor.hex),
                    "newColor" to (result.newColor?.hex ?: ""),
                    "adjustment" to "brightness",
                    "direction" to directionWord,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not adjust brightness",
                recoverable = true
            )
        }
    }

    /**
     * Handle saturation adjustment (more/less saturated).
     */
    private suspend fun handleAdjustSaturation(
        increase: Boolean,
        command: QuantizedCommand
    ): HandlerResult {
        // Find the color picker
        val pickerInfo = findColorPicker(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No color picker focused",
                recoverable = true,
                suggestedAction = "Focus on a color picker first"
            )

        val delta = if (increase) DEFAULT_SATURATION_STEP else -DEFAULT_SATURATION_STEP

        val result = executor.adjustSaturation(pickerInfo, delta)

        return if (result.success) {
            val directionWord = if (increase) "more saturated" else "less saturated"

            // Invoke callback for voice feedback
            result.newColor?.let { newColor ->
                onColorChanged?.invoke(
                    pickerInfo.name.ifBlank { "Color picker" },
                    newColor
                )
            }

            val feedback = buildString {
                if (pickerInfo.name.isNotBlank()) {
                    append(pickerInfo.name)
                    append(" made ")
                } else {
                    append("Color made ")
                }
                append(directionWord)
            }

            Log.i(TAG, "Color saturation adjusted: ${pickerInfo.name} -> $directionWord")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerName" to pickerInfo.name,
                    "pickerAvid" to pickerInfo.avid,
                    "previousColor" to (pickerInfo.currentColor.hex),
                    "newColor" to (result.newColor?.hex ?: ""),
                    "adjustment" to "saturation",
                    "direction" to directionWord,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not adjust saturation",
                recoverable = true
            )
        }
    }

    // ===============================================================================
    // Helper Methods
    // ===============================================================================

    /**
     * Find color picker by AVID, name, or focus state.
     */
    private suspend fun findColorPicker(
        name: String? = null,
        avid: String? = null
    ): ColorPickerInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val picker = executor.findByAvid(avid)
            if (picker != null) return picker
        }

        // Priority 2: Name lookup
        if (name != null) {
            val picker = executor.findByName(name)
            if (picker != null) return picker
        }

        // Priority 3: Focused color picker
        return executor.findFocused()
    }

    /**
     * Apply a named color to the color picker.
     */
    private suspend fun applyColorByName(
        pickerInfo: ColorPickerInfo,
        colorName: String,
        hexColor: String
    ): HandlerResult {
        val result = executor.setColorByName(pickerInfo, colorName)

        return if (result.success) {
            // Invoke callback for voice feedback
            result.newColor?.let { newColor ->
                onColorChanged?.invoke(
                    pickerInfo.name.ifBlank { "Color picker" },
                    newColor
                )
            }

            val feedback = buildString {
                if (pickerInfo.name.isNotBlank()) {
                    append(pickerInfo.name)
                    append(" set to ")
                } else {
                    append("Color set to ")
                }
                append(colorName)
            }

            Log.i(TAG, "Color set: ${pickerInfo.name} = $colorName ($hexColor)")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerName" to pickerInfo.name,
                    "pickerAvid" to pickerInfo.avid,
                    "previousColor" to (pickerInfo.currentColor.hex),
                    "newColor" to hexColor,
                    "colorName" to colorName,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set color",
                recoverable = true
            )
        }
    }

    /**
     * Apply a channel change (red, green, or blue).
     */
    private suspend fun applyChannelChange(
        pickerInfo: ColorPickerInfo,
        channel: ColorChannel,
        value: Int
    ): HandlerResult {
        val result = when (channel) {
            ColorChannel.RED -> executor.setRed(pickerInfo, value)
            ColorChannel.GREEN -> executor.setGreen(pickerInfo, value)
            ColorChannel.BLUE -> executor.setBlue(pickerInfo, value)
        }

        return if (result.success) {
            // Invoke callback for voice feedback
            result.newColor?.let { newColor ->
                onColorChanged?.invoke(
                    pickerInfo.name.ifBlank { "Color picker" },
                    newColor
                )
            }

            val channelName = channel.name.lowercase()
            val feedback = buildString {
                append(channelName.replaceFirstChar { it.uppercase() })
                append(" set to ")
                append(value)
            }

            Log.i(TAG, "Color channel set: ${pickerInfo.name}.$channelName = $value")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerName" to pickerInfo.name,
                    "pickerAvid" to pickerInfo.avid,
                    "channel" to channelName,
                    "previousValue" to when (channel) {
                        ColorChannel.RED -> pickerInfo.currentColor.red
                        ColorChannel.GREEN -> pickerInfo.currentColor.green
                        ColorChannel.BLUE -> pickerInfo.currentColor.blue
                    },
                    "newValue" to value,
                    "newColor" to (result.newColor?.hex ?: ""),
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set ${channel.name.lowercase()} channel",
                recoverable = true
            )
        }
    }

    /**
     * Apply an alpha/opacity change.
     */
    private suspend fun applyAlphaChange(
        pickerInfo: ColorPickerInfo,
        percentage: Int
    ): HandlerResult {
        val result = executor.setAlpha(pickerInfo, percentage)

        return if (result.success) {
            // Invoke callback for voice feedback
            result.newColor?.let { newColor ->
                onColorChanged?.invoke(
                    pickerInfo.name.ifBlank { "Color picker" },
                    newColor
                )
            }

            val feedback = buildString {
                append("Opacity set to ")
                append(percentage)
                append("%")
            }

            Log.i(TAG, "Color alpha set: ${pickerInfo.name}.alpha = $percentage%")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerName" to pickerInfo.name,
                    "pickerAvid" to pickerInfo.avid,
                    "previousAlpha" to pickerInfo.currentColor.alpha,
                    "newAlpha" to percentage,
                    "newColor" to (result.newColor?.hex ?: ""),
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set opacity",
                recoverable = true
            )
        }
    }

    /**
     * Apply a hex color.
     */
    private suspend fun applyHexColor(
        pickerInfo: ColorPickerInfo,
        hexCode: String
    ): HandlerResult {
        val result = executor.setHex(pickerInfo, hexCode)

        return if (result.success) {
            // Invoke callback for voice feedback
            result.newColor?.let { newColor ->
                onColorChanged?.invoke(
                    pickerInfo.name.ifBlank { "Color picker" },
                    newColor
                )
            }

            val feedback = buildString {
                if (pickerInfo.name.isNotBlank()) {
                    append(pickerInfo.name)
                    append(" set to ")
                } else {
                    append("Color set to ")
                }
                append("#$hexCode")
            }

            Log.i(TAG, "Color set via hex: ${pickerInfo.name} = #$hexCode")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "pickerName" to pickerInfo.name,
                    "pickerAvid" to pickerInfo.avid,
                    "previousColor" to (pickerInfo.currentColor.hex),
                    "newColor" to "#$hexCode",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set hex color",
                recoverable = true
            )
        }
    }

    /**
     * Parse a channel value (0-255) from string.
     */
    private fun parseChannelValue(input: String): Int? {
        val trimmed = input.trim().lowercase()

        // Try direct numeric parsing
        trimmed.toIntOrNull()?.let { return it }

        // Try word number parsing
        WORD_NUMBERS[trimmed]?.let { return it }

        return null
    }

    /**
     * Parse a percentage value (0-100) from string.
     *
     * Supports:
     * - "50" -> 50
     * - "50%" -> 50
     * - "50 percent" -> 50
     * - "fifty" -> 50
     */
    private fun parsePercentageValue(input: String): Int? {
        val trimmed = input.trim().lowercase()

        // Check for percentage suffix
        val valueStr = trimmed
            .removeSuffix("%")
            .removeSuffix("percent")
            .removeSuffix(" percent")
            .trim()

        // Try direct numeric parsing
        valueStr.toIntOrNull()?.let { return it }

        // Try word number parsing
        WORD_NUMBERS[valueStr]?.let { return it }

        return null
    }

    // ===============================================================================
    // Voice Phrases for Speech Engine Registration
    // ===============================================================================

    override fun getVoicePhrases(): List<String> {
        return listOf(
            // Named colors
            "set color to red",
            "set color to blue",
            "set color to green",
            "set color to yellow",
            "set color to orange",
            "set color to purple",
            "set color to white",
            "set color to black",
            "red", "blue", "green", "yellow",
            // RGB channels
            "set red to", "red",
            "set green to", "green",
            "set blue to", "blue",
            // Alpha
            "set opacity to", "opacity",
            "set alpha to", "alpha",
            // Brightness
            "lighter", "brighten",
            "darker", "darken",
            // Saturation
            "more saturated", "saturate",
            "less saturated", "desaturate",
            // Hex
            "hex"
        )
    }
}

// ===================================================================================
// Supporting Types
// ===================================================================================

/**
 * Color channel identifier.
 */
private enum class ColorChannel {
    RED,
    GREEN,
    BLUE
}

/**
 * Color value representation with RGB and alpha components.
 *
 * @property red Red channel value (0-255)
 * @property green Green channel value (0-255)
 * @property blue Blue channel value (0-255)
 * @property alpha Alpha/opacity value (0-100 as percentage)
 * @property hex Hex representation of the color (e.g., "#FF0000" or "#FF0000FF" with alpha)
 */
data class ColorValue(
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0,
    val alpha: Int = 100,
    val hex: String = "#000000"
) {
    companion object {
        /**
         * Create a ColorValue from hex string.
         *
         * @param hex Hex color string (e.g., "#FF0000", "FF0000", "#FF0000FF")
         * @return ColorValue instance
         */
        fun fromHex(hex: String): ColorValue {
            val cleanHex = hex.removePrefix("#").removePrefix("0x").uppercase()

            return when (cleanHex.length) {
                6 -> {
                    val r = cleanHex.substring(0, 2).toIntOrNull(16) ?: 0
                    val g = cleanHex.substring(2, 4).toIntOrNull(16) ?: 0
                    val b = cleanHex.substring(4, 6).toIntOrNull(16) ?: 0
                    ColorValue(red = r, green = g, blue = b, alpha = 100, hex = "#$cleanHex")
                }
                8 -> {
                    val r = cleanHex.substring(0, 2).toIntOrNull(16) ?: 0
                    val g = cleanHex.substring(2, 4).toIntOrNull(16) ?: 0
                    val b = cleanHex.substring(4, 6).toIntOrNull(16) ?: 0
                    val a = cleanHex.substring(6, 8).toIntOrNull(16) ?: 255
                    val alphaPercent = (a * 100) / 255
                    ColorValue(red = r, green = g, blue = b, alpha = alphaPercent, hex = "#$cleanHex")
                }
                else -> ColorValue()
            }
        }

        /**
         * Create a ColorValue from RGB components.
         *
         * @param red Red channel (0-255)
         * @param green Green channel (0-255)
         * @param blue Blue channel (0-255)
         * @param alpha Alpha as percentage (0-100), default 100
         * @return ColorValue instance
         */
        fun fromRgb(red: Int, green: Int, blue: Int, alpha: Int = 100): ColorValue {
            val r = red.coerceIn(0, 255)
            val g = green.coerceIn(0, 255)
            val b = blue.coerceIn(0, 255)
            val a = alpha.coerceIn(0, 100)

            val hex = if (a < 100) {
                val alphaHex = ((a * 255) / 100).toString(16).padStart(2, '0').uppercase()
                "#${r.toString(16).padStart(2, '0').uppercase()}" +
                    "${g.toString(16).padStart(2, '0').uppercase()}" +
                    "${b.toString(16).padStart(2, '0').uppercase()}" +
                    alphaHex
            } else {
                "#${r.toString(16).padStart(2, '0').uppercase()}" +
                    "${g.toString(16).padStart(2, '0').uppercase()}" +
                    "${b.toString(16).padStart(2, '0').uppercase()}"
            }

            return ColorValue(red = r, green = g, blue = b, alpha = a, hex = hex)
        }

        /** Predefined color: Transparent */
        val TRANSPARENT = ColorValue(0, 0, 0, 0, "#00000000")

        /** Predefined color: Black */
        val BLACK = ColorValue(0, 0, 0, 100, "#000000")

        /** Predefined color: White */
        val WHITE = ColorValue(255, 255, 255, 100, "#FFFFFF")

        /** Predefined color: Red */
        val RED = ColorValue(255, 0, 0, 100, "#FF0000")

        /** Predefined color: Green */
        val GREEN = ColorValue(0, 255, 0, 100, "#00FF00")

        /** Predefined color: Blue */
        val BLUE = ColorValue(0, 0, 255, 100, "#0000FF")
    }

    /**
     * Create a copy with modified red channel.
     */
    fun withRed(red: Int): ColorValue = fromRgb(red, green, blue, alpha)

    /**
     * Create a copy with modified green channel.
     */
    fun withGreen(green: Int): ColorValue = fromRgb(red, green, blue, alpha)

    /**
     * Create a copy with modified blue channel.
     */
    fun withBlue(blue: Int): ColorValue = fromRgb(red, green, blue, alpha)

    /**
     * Create a copy with modified alpha.
     */
    fun withAlpha(alpha: Int): ColorValue = fromRgb(red, green, blue, alpha)
}

/**
 * Information about a color picker component.
 *
 * @property avid AVID fingerprint for the color picker (format: CLR:{hash8})
 * @property name Display name or associated label
 * @property currentColor Current color value
 * @property supportsAlpha Whether this picker supports alpha/opacity
 * @property bounds Screen bounds for the color picker
 * @property isFocused Whether this color picker currently has focus
 * @property node Platform-specific node reference
 */
data class ColorPickerInfo(
    val avid: String,
    val name: String = "",
    val currentColor: ColorValue = ColorValue(),
    val supportsAlpha: Boolean = true,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "ColorPicker",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = currentColor.hex
    )
}

/**
 * Result of a color picker operation.
 *
 * @property success Whether the operation succeeded
 * @property error Error message if operation failed
 * @property previousColor The color before the operation
 * @property newColor The color after the operation
 */
data class ColorPickerOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousColor: ColorValue? = null,
    val newColor: ColorValue? = null
) {
    companion object {
        /**
         * Create a successful result.
         */
        fun success(previousColor: ColorValue, newColor: ColorValue) = ColorPickerOperationResult(
            success = true,
            previousColor = previousColor,
            newColor = newColor
        )

        /**
         * Create an error result.
         */
        fun error(message: String) = ColorPickerOperationResult(
            success = false,
            error = message
        )
    }
}

// ===================================================================================
// Platform Executor Interface
// ===================================================================================

/**
 * Platform-specific executor for color picker operations.
 *
 * Implementations should:
 * 1. Find color picker components by AVID, name, or focus state
 * 2. Read current color values from picker state
 * 3. Set colors via accessibility actions or direct manipulation
 * 4. Handle both native Android color pickers and custom implementations
 *
 * ## Color Picker Detection Algorithm
 *
 * ```kotlin
 * fun findColorPickerNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className or contentDescription indicating color picker:
 *     // - Custom views with "color" in class name
 *     // - Views with "color picker" in contentDescription
 *     // - Known color picker library classes
 *     // - Views with color-related child elements (sliders for RGB, etc.)
 * }
 * ```
 *
 * ## Color Setting Algorithm
 *
 * ```kotlin
 * fun setColor(node: AccessibilityNodeInfo, color: ColorValue): Boolean {
 *     // Strategy 1: Direct color setter via reflection/accessibility
 *     // Strategy 2: Manipulate child RGB sliders
 *     // Strategy 3: Inject color value into text input field
 *     // Strategy 4: Simulate touch on color palette at computed position
 * }
 * ```
 */
interface ColorPickerExecutor {

    // ===============================================================================
    // Color Picker Discovery
    // ===============================================================================

    /**
     * Find a color picker by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: CLR:{hash8})
     * @return ColorPickerInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): ColorPickerInfo?

    /**
     * Find a color picker by its name or associated label.
     *
     * Searches for:
     * 1. Color picker with matching contentDescription
     * 2. Color picker with label text matching name
     * 3. Color picker with associated TextView label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return ColorPickerInfo if found, null otherwise
     */
    suspend fun findByName(name: String): ColorPickerInfo?

    /**
     * Find the currently focused color picker.
     *
     * @return ColorPickerInfo if a color picker has focus, null otherwise
     */
    suspend fun findFocused(): ColorPickerInfo?

    // ===============================================================================
    // Color Operations - Full Color
    // ===============================================================================

    /**
     * Set the color picker to a specific color value.
     *
     * @param picker The color picker to modify
     * @param color The target color value
     * @return Operation result with previous and new colors
     */
    suspend fun setColor(picker: ColorPickerInfo, color: ColorValue): ColorPickerOperationResult

    /**
     * Set the color picker to a named color.
     *
     * @param picker The color picker to modify
     * @param colorName The name of the color (e.g., "red", "blue")
     * @return Operation result with previous and new colors
     */
    suspend fun setColorByName(picker: ColorPickerInfo, colorName: String): ColorPickerOperationResult

    /**
     * Set the color picker to a hex color value.
     *
     * @param picker The color picker to modify
     * @param hexCode The hex color code (e.g., "FF0000" or "FF0000FF" with alpha)
     * @return Operation result with previous and new colors
     */
    suspend fun setHex(picker: ColorPickerInfo, hexCode: String): ColorPickerOperationResult

    // ===============================================================================
    // Color Operations - Individual Channels
    // ===============================================================================

    /**
     * Set the red channel of the color picker.
     *
     * @param picker The color picker to modify
     * @param value Red channel value (0-255)
     * @return Operation result with previous and new colors
     */
    suspend fun setRed(picker: ColorPickerInfo, value: Int): ColorPickerOperationResult

    /**
     * Set the green channel of the color picker.
     *
     * @param picker The color picker to modify
     * @param value Green channel value (0-255)
     * @return Operation result with previous and new colors
     */
    suspend fun setGreen(picker: ColorPickerInfo, value: Int): ColorPickerOperationResult

    /**
     * Set the blue channel of the color picker.
     *
     * @param picker The color picker to modify
     * @param value Blue channel value (0-255)
     * @return Operation result with previous and new colors
     */
    suspend fun setBlue(picker: ColorPickerInfo, value: Int): ColorPickerOperationResult

    /**
     * Set the alpha/opacity of the color picker.
     *
     * @param picker The color picker to modify
     * @param percentage Alpha value as percentage (0-100)
     * @return Operation result with previous and new colors
     */
    suspend fun setAlpha(picker: ColorPickerInfo, percentage: Int): ColorPickerOperationResult

    // ===============================================================================
    // Color Operations - Adjustments
    // ===============================================================================

    /**
     * Adjust the brightness of the current color.
     *
     * Positive delta increases brightness, negative decreases.
     * The delta is a factor (e.g., 0.1 = 10% brighter).
     *
     * @param picker The color picker to modify
     * @param delta Brightness adjustment factor (-1.0 to 1.0)
     * @return Operation result with previous and new colors
     */
    suspend fun adjustBrightness(picker: ColorPickerInfo, delta: Double): ColorPickerOperationResult

    /**
     * Adjust the saturation of the current color.
     *
     * Positive delta increases saturation, negative decreases.
     * The delta is a factor (e.g., 0.1 = 10% more saturated).
     *
     * @param picker The color picker to modify
     * @param delta Saturation adjustment factor (-1.0 to 1.0)
     * @return Operation result with previous and new colors
     */
    suspend fun adjustSaturation(picker: ColorPickerInfo, delta: Double): ColorPickerOperationResult
}
