package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.Bounds

/**
 * Represents a UI element that has been assigned a number for voice selection.
 *
 * @property number The assigned number (1-based)
 * @property vuid The unique voice identifier for the element
 * @property label The human-readable label for the element
 * @property bounds Optional screen bounds of the element
 */
data class NumberedElement(
    val number: Int,
    val vuid: String,
    val label: String,
    val bounds: Bounds? = null
)

/**
 * Result of attempting to select a numbered element.
 *
 * @property success Whether the selection was successful
 * @property selectedNumber The number that was parsed/selected (if parseable)
 * @property element The selected element (if found)
 * @property error Error message if selection failed
 */
data class NumberSelectionResult(
    val success: Boolean,
    val selectedNumber: Int? = null,
    val element: NumberedElement? = null,
    val error: String? = null
)

/**
 * Handles numbered element selection for voice commands.
 *
 * This class manages a collection of numbered UI elements and provides
 * methods to select elements by number through voice commands like
 * "tap 3", "click 5", "select 2", or just "3".
 *
 * Word numbers ("one" through "ten") are also supported.
 *
 * Usage:
 * ```
 * val handler = NumberHandler()
 *
 * // Assign numbers to elements (vuid to label pairs)
 * handler.assignNumbers(listOf(
 *     "btn_submit" to "Submit",
 *     "btn_cancel" to "Cancel"
 * ))
 *
 * // Handle voice commands
 * val result = handler.handleCommand("tap 1")
 * if (result.success) {
 *     println("Selected: ${result.element?.label}")
 * }
 * ```
 */
class NumberHandler {

    private val numberedElements = mutableMapOf<Int, NumberedElement>()
    private var nextNumber: Int = 1

    /**
     * Word number mappings for voice recognition.
     */
    private val wordNumbers = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
    )

    /**
     * Command prefixes recognized for number selection.
     */
    private val commandPrefixes = listOf("tap ", "click ", "select ", "number ")

    /**
     * Assigns sequential numbers to a list of elements.
     *
     * This clears any previous assignments and starts numbering from 1.
     *
     * @param elements List of pairs where first is VUID and second is label
     * @return Map of assigned numbers to their elements
     */
    fun assignNumbers(elements: List<Pair<String, String>>): Map<Int, NumberedElement> {
        clear()
        elements.forEach { (vuid, label) ->
            val number = nextNumber++
            numberedElements[number] = NumberedElement(number, vuid, label)
        }
        return numberedElements.toMap()
    }

    /**
     * Assigns sequential numbers to elements with bounds information.
     *
     * @param elements List of triples (vuid, label, bounds)
     * @return Map of assigned numbers to their elements
     */
    fun assignNumbersWithBounds(elements: List<Triple<String, String, Bounds?>>): Map<Int, NumberedElement> {
        clear()
        elements.forEach { (vuid, label, bounds) ->
            val number = nextNumber++
            numberedElements[number] = NumberedElement(number, vuid, label, bounds)
        }
        return numberedElements.toMap()
    }

    /**
     * Handles a voice command to select a numbered element.
     *
     * Recognizes commands like:
     * - "tap 3" - selects element 3
     * - "click 5" - selects element 5
     * - "select 2" - selects element 2
     * - "number 1" - selects element 1
     * - "3" - selects element 3 (direct number)
     * - "three" - selects element 3 (word number)
     *
     * @param command The voice command string
     * @return NumberSelectionResult indicating success/failure
     */
    fun handleCommand(command: String): NumberSelectionResult {
        val normalizedCommand = command.lowercase().trim()

        val number = parseNumber(normalizedCommand)
            ?: return NumberSelectionResult(
                success = false,
                error = "Could not parse number from: $command"
            )

        return selectNumber(number)
    }

    /**
     * Selects an element by its assigned number.
     *
     * @param number The number to select
     * @return NumberSelectionResult with the selected element or error
     */
    fun selectNumber(number: Int): NumberSelectionResult {
        val element = numberedElements[number]
            ?: return NumberSelectionResult(
                success = false,
                selectedNumber = number,
                error = "No element with number $number"
            )

        return NumberSelectionResult(
            success = true,
            selectedNumber = number,
            element = element
        )
    }

    /**
     * Gets an element by its assigned number.
     *
     * @param number The number to look up
     * @return The numbered element or null if not found
     */
    fun getElement(number: Int): NumberedElement? = numberedElements[number]

    /**
     * Gets all currently numbered elements.
     *
     * @return Copy of the numbered elements map
     */
    fun getAllNumberedElements(): Map<Int, NumberedElement> = numberedElements.toMap()

    /**
     * Gets the count of numbered elements.
     *
     * @return Number of elements currently assigned
     */
    fun getCount(): Int = numberedElements.size

    /**
     * Clears all numbered elements and resets numbering.
     */
    fun clear() {
        numberedElements.clear()
        nextNumber = 1
    }

    /**
     * Parses a number from a command string.
     *
     * @param command Normalized (lowercase, trimmed) command string
     * @return The parsed number or null if not parseable
     */
    private fun parseNumber(command: String): Int? {
        // Try direct number
        command.toIntOrNull()?.let { return it }

        // Try command prefixes (e.g., "tap 3", "click 5")
        for (prefix in commandPrefixes) {
            if (command.startsWith(prefix)) {
                val afterPrefix = command.substringAfter(prefix).trim()
                // Try numeric after prefix
                afterPrefix.toIntOrNull()?.let { return it }
                // Try word number after prefix
                wordNumbers[afterPrefix]?.let { return it }
            }
        }

        // Try word numbers directly
        wordNumbers[command]?.let { return it }

        return null
    }
}
