/**
 * QuantizedContext.kt - Compact representation of app UI context
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Provides a token-efficient representation of learned app structure
 * for LLM/NLU consumption. Optimized for voice command matching and
 * action prediction.
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

/**
 * Quantized Context
 *
 * Compact, NLU-optimized representation of an app's UI structure.
 * Contains:
 * - App metadata
 * - Screen summaries with actionable elements
 * - Navigation graph (simplified)
 * - Element vocabulary for command matching
 */
data class QuantizedContext(
    /** Package name of the app */
    val packageName: String,

    /** Human-readable app name */
    val appName: String,

    /** Version code when app was learned */
    val versionCode: Long,

    /** Version name when app was learned */
    val versionName: String,

    /** Timestamp when context was generated */
    val generatedAt: Long,

    /** Quantized screens in the app */
    val screens: List<QuantizedScreen>,

    /** Navigation edges between screens */
    val navigation: List<QuantizedNavigation>,

    /** Global vocabulary - common element labels across screens */
    val vocabulary: Set<String>,

    /** App-specific commands discovered during learning */
    val knownCommands: List<QuantizedCommand>
) {
    /**
     * Find screen by hash
     */
    fun findScreen(screenHash: String): QuantizedScreen? {
        return screens.find { it.screenHash == screenHash }
    }

    /**
     * Find screens containing a specific element label
     */
    fun findScreensWithElement(elementLabel: String): List<QuantizedScreen> {
        val normalizedLabel = elementLabel.lowercase()
        return screens.filter { screen ->
            screen.elements.any { it.label.lowercase().contains(normalizedLabel) }
        }
    }

    /**
     * Get navigation options from a screen
     */
    fun getNavigationFrom(screenHash: String): List<QuantizedNavigation> {
        return navigation.filter { it.fromScreenHash == screenHash }
    }

    /**
     * Get all actionable elements across all screens
     */
    fun getAllActionableElements(): List<QuantizedElement> {
        return screens.flatMap { it.elements }
    }

    /**
     * Generate summary statistics
     */
    fun getStats(): QuantizedStats {
        return QuantizedStats(
            totalScreens = screens.size,
            totalElements = screens.sumOf { it.elements.size },
            totalNavigations = navigation.size,
            vocabularySize = vocabulary.size,
            knownCommandsCount = knownCommands.size
        )
    }
}

/**
 * Quantized Screen
 *
 * Compact representation of a single screen.
 */
data class QuantizedScreen(
    /** Unique hash identifying this screen state */
    val screenHash: String,

    /** Activity name (if known) */
    val activityName: String?,

    /** Screen title or inferred name */
    val screenTitle: String,

    /** Actionable elements on this screen */
    val elements: List<QuantizedElement>,

    /** Screen tags for categorization */
    val tags: Set<String> = emptySet()
) {
    /**
     * Find element by label (fuzzy match)
     */
    fun findElement(label: String): QuantizedElement? {
        val normalizedLabel = label.lowercase()
        return elements.find { it.label.lowercase() == normalizedLabel }
            ?: elements.find { it.label.lowercase().contains(normalizedLabel) }
    }

    /**
     * Get elements by type
     */
    fun getElementsByType(type: QuantizedElementType): List<QuantizedElement> {
        return elements.filter { it.type == type }
    }
}

/**
 * Quantized Element
 *
 * Compact representation of an actionable UI element.
 */
data class QuantizedElement(
    /** Unique identifier (VUID) */
    val uuid: String,

    /** Human-readable label */
    val label: String,

    /** Element type */
    val type: QuantizedElementType,

    /** Alternative labels/synonyms for voice matching */
    val aliases: Set<String> = emptySet(),

    /** Confidence score (0.0-1.0) */
    val confidence: Float = 1.0f,

    /** Element position hints */
    val positionHint: String? = null
) {
    /**
     * Check if this element matches a voice command
     */
    fun matchesCommand(command: String): Boolean {
        val normalizedCommand = command.lowercase()
        return label.lowercase().contains(normalizedCommand) ||
               aliases.any { it.lowercase().contains(normalizedCommand) }
    }
}

/**
 * Quantized Element Type
 *
 * Simplified element categories for NLU.
 */
enum class QuantizedElementType {
    /** Button or clickable element */
    BUTTON,

    /** Text input field */
    INPUT,

    /** Navigation element (link, tab, menu item) */
    NAVIGATION,

    /** Toggle or switch */
    TOGGLE,

    /** Selectable item (list item, checkbox, radio) */
    SELECTABLE,

    /** Scrollable container */
    SCROLLABLE,

    /** Other actionable element */
    OTHER
}

/**
 * Quantized Navigation
 *
 * Represents a navigation edge between screens.
 */
data class QuantizedNavigation(
    /** Source screen hash */
    val fromScreenHash: String,

    /** Element that triggers navigation */
    val triggerElementUuid: String,

    /** Human-readable trigger label */
    val triggerLabel: String,

    /** Destination screen hash */
    val toScreenHash: String,

    /** Navigation type hint */
    val navigationType: NavigationType = NavigationType.PUSH
)

/**
 * Navigation Type
 */
enum class NavigationType {
    /** Pushes new screen onto stack */
    PUSH,

    /** Replaces current screen */
    REPLACE,

    /** Pops to previous screen */
    POP,

    /** Opens dialog or modal */
    DIALOG,

    /** Opens external app */
    EXTERNAL
}

/**
 * Quantized Command
 *
 * Represents a discovered app-specific voice command.
 */
data class QuantizedCommand(
    /** Primary command phrase */
    val phrase: String,

    /** Alternative phrases */
    val alternatives: Set<String> = emptySet(),

    /** Target element UUID */
    val targetElementUuid: String?,

    /** Target screen hash (if navigational) */
    val targetScreenHash: String?,

    /** Action type */
    val actionType: CommandActionType,

    /** Usage frequency (learning score) */
    val frequency: Int = 0
)

/**
 * Command Action Type
 */
enum class CommandActionType {
    /** Click/tap action */
    CLICK,

    /** Text input */
    INPUT,

    /** Navigation */
    NAVIGATE,

    /** Toggle/switch */
    TOGGLE,

    /** Scroll action */
    SCROLL,

    /** System action (back, home, etc.) */
    SYSTEM
}

/**
 * Quantized Stats
 *
 * Summary statistics for a quantized context.
 */
data class QuantizedStats(
    val totalScreens: Int,
    val totalElements: Int,
    val totalNavigations: Int,
    val vocabularySize: Int,
    val knownCommandsCount: Int
)
