/**
 * OverlayPlugin.kt - Overlay Plugin contract for VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for plugins that render accessibility overlays.
 * Overlays provide visual feedback for voice/gaze control, including
 * labels, highlights, and badges on UI elements.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.commandmanager.QuantizedElement

/**
 * Overlay Plugin contract for rendering accessibility overlays.
 *
 * Overlay plugins provide visual feedback and assistance for voice/gaze control.
 * They render labels, highlights, badges, and other visual elements that help
 * users identify and interact with UI components.
 *
 * ## Design Principles
 * - **Priority-Based Rendering**: Higher priority overlays render on top
 * - **Visibility Modes**: Control when overlays appear (always, on command, etc.)
 * - **Screen-Aware**: Overlays update when screen content changes
 * - **Interactive**: Overlays can handle user interactions
 *
 * ## Implementation Example
 * ```kotlin
 * class NumberLabelOverlay : OverlayPlugin {
 *     override val priority = 100
 *     override val visibilityMode = OverlayVisibility.ON_COMMAND
 *
 *     override fun render(context: OverlayContext): OverlayContent {
 *         val elements = context.elements.mapIndexed { index, element ->
 *             OverlayElement.Label(
 *                 id = "label_$index",
 *                 text = "${index + 1}",
 *                 targetAvid = element.avid,
 *                 position = LabelPosition.TOP_LEFT,
 *                 style = LabelStyle.DEFAULT
 *             )
 *         }
 *         return OverlayContent(elements = elements)
 *     }
 *
 *     override fun onInteraction(event: OverlayInteractionEvent): Boolean {
 *         // Handle taps on labels
 *         return event is OverlayInteractionEvent.Tap
 *     }
 *
 *     override fun onScreenChanged(context: OverlayContext) {
 *         // Clear and re-render when screen changes
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see OverlayContent
 * @see OverlayElement
 * @see OverlayVisibility
 */
interface OverlayPlugin : UniversalPlugin {

    /**
     * Rendering priority for this overlay.
     *
     * Higher values render on top of lower values. Use standard ranges:
     * - 0-99: Background overlays (grids, guides)
     * - 100-199: Standard overlays (labels, badges)
     * - 200-299: Interactive overlays (selection highlights)
     * - 300+: Critical overlays (error indicators, focus rings)
     */
    val priority: Int

    /**
     * Visibility mode controlling when the overlay appears.
     *
     * Determines the conditions under which this overlay is rendered.
     */
    val visibilityMode: OverlayVisibility

    /**
     * Render overlay content for the current screen state.
     *
     * Called when the overlay needs to be rendered or updated.
     * Implementations should return a complete [OverlayContent] describing
     * all elements to display.
     *
     * ## Performance
     * This method may be called frequently (on each screen update).
     * Implementations should be efficient and avoid heavy computations.
     * Consider caching if rendering is expensive.
     *
     * @param context Current overlay context with screen state and elements
     * @return OverlayContent describing elements to render
     */
    fun render(context: OverlayContext): OverlayContent

    /**
     * Handle user interaction with overlay elements.
     *
     * Called when the user interacts with an overlay element (tap, long press, etc.).
     * Return true if the interaction was handled, false to pass through to
     * underlying UI.
     *
     * @param event The interaction event
     * @return true if the interaction was consumed, false to pass through
     */
    fun onInteraction(event: OverlayInteractionEvent): Boolean

    /**
     * Handle screen content changes.
     *
     * Called when the underlying screen content changes significantly
     * (navigation, content update, etc.). Use this to clear caches
     * or prepare for re-rendering.
     *
     * @param context Updated overlay context with new screen state
     */
    fun onScreenChanged(context: OverlayContext)

    /**
     * Check if the overlay should be visible under current conditions.
     *
     * @param isVoiceActive Whether voice recognition is active
     * @param isGazeActive Whether gaze tracking is active
     * @param isManuallyTriggered Whether user manually requested overlay
     * @return true if overlay should be visible
     */
    fun shouldBeVisible(
        isVoiceActive: Boolean,
        isGazeActive: Boolean,
        isManuallyTriggered: Boolean
    ): Boolean {
        return when (visibilityMode) {
            OverlayVisibility.ALWAYS -> true
            OverlayVisibility.ON_COMMAND -> isVoiceActive
            OverlayVisibility.ON_GAZE -> isGazeActive
            OverlayVisibility.MANUAL -> isManuallyTriggered
            OverlayVisibility.ON_ACTIVITY -> isVoiceActive || isGazeActive
        }
    }
}

/**
 * Visibility mode for overlays.
 *
 * Determines the conditions under which an overlay is displayed.
 */
enum class OverlayVisibility {
    /** Always visible when accessibility service is active */
    ALWAYS,

    /** Visible only when voice command mode is active */
    ON_COMMAND,

    /** Visible only when gaze tracking is active */
    ON_GAZE,

    /** Visible only when manually triggered by user */
    MANUAL,

    /** Visible when any input activity is detected (voice or gaze) */
    ON_ACTIVITY
}

/**
 * Context provided to overlay plugins for rendering.
 *
 * Contains all information needed to render appropriate overlay elements
 * for the current screen state.
 *
 * @property screenContext Current screen metadata
 * @property elements List of quantized UI elements on screen
 * @property focusedAvid AVID of currently focused element (if any)
 * @property gazeTarget AVID of current gaze target (if gaze active)
 * @property themeColors Current theme colors for consistent styling
 * @property density Screen density for size calculations
 */
data class OverlayContext(
    val screenContext: ScreenContext,
    val elements: List<QuantizedElement>,
    val focusedAvid: String?,
    val gazeTarget: String?,
    val themeColors: ThemeColors,
    val density: Float
) {
    /**
     * Get the focused element if any.
     *
     * @return The focused QuantizedElement or null
     */
    fun getFocusedElement(): QuantizedElement? {
        return focusedAvid?.let { avid ->
            elements.find { it.avid == avid }
        }
    }

    /**
     * Get the gaze target element if any.
     *
     * @return The gaze target QuantizedElement or null
     */
    fun getGazeTargetElement(): QuantizedElement? {
        return gazeTarget?.let { avid ->
            elements.find { it.avid == avid }
        }
    }

    /**
     * Get actionable elements only.
     *
     * @return List of elements that have click or other actions
     */
    fun getActionableElements(): List<QuantizedElement> {
        return elements.filter { it.actions.isNotBlank() }
    }

    /**
     * Convert density-independent pixels to actual pixels.
     *
     * @param dp Density-independent pixel value
     * @return Actual pixel value for current density
     */
    fun dpToPx(dp: Float): Float = dp * density
}

/**
 * Content to be rendered by an overlay.
 *
 * Contains all elements that should be displayed by the overlay system.
 *
 * @property elements List of overlay elements to render
 * @property backgroundColor Optional semi-transparent background color
 * @property transitionDuration Animation duration in milliseconds
 */
data class OverlayContent(
    val elements: List<OverlayElement>,
    val backgroundColor: String? = null,
    val transitionDuration: Long = 150
) {
    /**
     * Check if overlay has any content to render.
     */
    fun isEmpty(): Boolean = elements.isEmpty()

    /**
     * Get elements by type.
     */
    inline fun <reified T : OverlayElement> getElementsOfType(): List<T> {
        return elements.filterIsInstance<T>()
    }

    companion object {
        /** Empty overlay content */
        val EMPTY = OverlayContent(elements = emptyList())
    }
}

/**
 * Individual overlay element.
 *
 * Sealed class representing different types of overlay elements
 * that can be rendered on screen.
 */
sealed class OverlayElement {
    /** Unique identifier for this element */
    abstract val id: String

    /** AVID of the target UI element this overlay is associated with */
    abstract val targetAvid: String?

    /**
     * Text label overlay element.
     *
     * Displays a text label near a UI element, typically used for
     * showing voice command shortcuts (numbers, names).
     *
     * @property id Unique element identifier
     * @property text Text to display
     * @property targetAvid AVID of the target element
     * @property position Position relative to target element
     * @property style Visual style for the label
     */
    data class Label(
        override val id: String,
        val text: String,
        override val targetAvid: String?,
        val position: LabelPosition = LabelPosition.TOP_LEFT,
        val style: LabelStyle = LabelStyle.DEFAULT
    ) : OverlayElement()

    /**
     * Highlight overlay element.
     *
     * Draws a highlight (border, fill) around a UI element to
     * indicate focus, selection, or gaze target.
     *
     * @property id Unique element identifier
     * @property targetAvid AVID of the element to highlight
     * @property highlightType Type of highlight to apply
     * @property color Highlight color (hex format)
     * @property strokeWidth Border stroke width in dp
     */
    data class Highlight(
        override val id: String,
        override val targetAvid: String,
        val highlightType: HighlightType = HighlightType.BORDER,
        val color: String = "#2196F3",
        val strokeWidth: Float = 2f
    ) : OverlayElement()

    /**
     * Badge overlay element.
     *
     * Displays a small badge (icon, indicator) near a UI element.
     * Used for status indicators, action hints, etc.
     *
     * @property id Unique element identifier
     * @property targetAvid AVID of the target element
     * @property badgeType Type of badge to display
     * @property content Badge content (text or icon identifier)
     * @property position Position relative to target element
     */
    data class Badge(
        override val id: String,
        override val targetAvid: String?,
        val badgeType: BadgeType,
        val content: String,
        val position: LabelPosition = LabelPosition.TOP_RIGHT
    ) : OverlayElement()

    /**
     * Tooltip overlay element.
     *
     * Displays a tooltip with additional information about an element.
     *
     * @property id Unique element identifier
     * @property targetAvid AVID of the target element
     * @property text Tooltip text content
     * @property dismissable Whether user can dismiss the tooltip
     */
    data class Tooltip(
        override val id: String,
        override val targetAvid: String?,
        val text: String,
        val dismissable: Boolean = true
    ) : OverlayElement()

    /**
     * Custom drawable overlay element.
     *
     * Allows rendering custom shapes/paths for specialized overlays.
     *
     * @property id Unique element identifier
     * @property targetAvid AVID of the target element (if associated)
     * @property drawablePath SVG path or custom path commands
     * @property bounds Absolute bounds for the drawable
     * @property fillColor Fill color (hex format, null for no fill)
     * @property strokeColor Stroke color (hex format, null for no stroke)
     */
    data class Custom(
        override val id: String,
        override val targetAvid: String?,
        val drawablePath: String,
        val bounds: OverlayBounds,
        val fillColor: String?,
        val strokeColor: String?
    ) : OverlayElement()
}

/**
 * Position for label placement relative to target element.
 */
enum class LabelPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}

/**
 * Style configuration for label overlays.
 *
 * @property backgroundColor Label background color (hex format)
 * @property textColor Label text color (hex format)
 * @property fontSize Text size in sp
 * @property cornerRadius Corner radius in dp
 * @property padding Padding in dp
 */
data class LabelStyle(
    val backgroundColor: String,
    val textColor: String,
    val fontSize: Float,
    val cornerRadius: Float,
    val padding: Float
) {
    companion object {
        val DEFAULT = LabelStyle(
            backgroundColor = "#2196F3",
            textColor = "#FFFFFF",
            fontSize = 14f,
            cornerRadius = 4f,
            padding = 4f
        )

        val DARK = LabelStyle(
            backgroundColor = "#333333",
            textColor = "#FFFFFF",
            fontSize = 14f,
            cornerRadius = 4f,
            padding = 4f
        )

        val LIGHT = LabelStyle(
            backgroundColor = "#FFFFFF",
            textColor = "#333333",
            fontSize = 14f,
            cornerRadius = 4f,
            padding = 4f
        )

        val ACCENT = LabelStyle(
            backgroundColor = "#FF5722",
            textColor = "#FFFFFF",
            fontSize = 14f,
            cornerRadius = 4f,
            padding = 4f
        )
    }
}

/**
 * Type of highlight to apply.
 */
enum class HighlightType {
    /** Border around the element */
    BORDER,

    /** Semi-transparent fill over the element */
    FILL,

    /** Both border and fill */
    BORDER_AND_FILL,

    /** Animated pulse effect */
    PULSE,

    /** Glow effect around the element */
    GLOW
}

/**
 * Type of badge to display.
 */
enum class BadgeType {
    /** Numeric badge (e.g., count) */
    NUMBER,

    /** Text badge (e.g., "NEW") */
    TEXT,

    /** Icon badge */
    ICON,

    /** Dot indicator */
    DOT,

    /** Status indicator (success, error, warning) */
    STATUS
}

/**
 * Bounds for custom overlay elements.
 *
 * @property left Left position in pixels
 * @property top Top position in pixels
 * @property right Right position in pixels
 * @property bottom Bottom position in pixels
 */
data class OverlayBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2

    companion object {
        val ZERO = OverlayBounds(0f, 0f, 0f, 0f)

        /**
         * Create bounds from string format "left,top,right,bottom".
         */
        fun fromString(bounds: String): OverlayBounds? {
            val parts = bounds.split(",").mapNotNull { it.toFloatOrNull() }
            return if (parts.size == 4) {
                OverlayBounds(parts[0], parts[1], parts[2], parts[3])
            } else null
        }
    }
}

/**
 * User interaction event on overlay elements.
 */
sealed class OverlayInteractionEvent {
    /** Element that was interacted with */
    abstract val elementId: String

    /** Target AVID associated with the element */
    abstract val targetAvid: String?

    /**
     * Tap/click on overlay element.
     */
    data class Tap(
        override val elementId: String,
        override val targetAvid: String?,
        val x: Float,
        val y: Float
    ) : OverlayInteractionEvent()

    /**
     * Long press on overlay element.
     */
    data class LongPress(
        override val elementId: String,
        override val targetAvid: String?,
        val x: Float,
        val y: Float
    ) : OverlayInteractionEvent()

    /**
     * Swipe/drag on overlay element.
     */
    data class Swipe(
        override val elementId: String,
        override val targetAvid: String?,
        val direction: SwipeDirection,
        val velocity: Float
    ) : OverlayInteractionEvent()

    /**
     * Gaze entered overlay element.
     */
    data class GazeEnter(
        override val elementId: String,
        override val targetAvid: String?,
        val dwellTime: Long
    ) : OverlayInteractionEvent()

    /**
     * Gaze left overlay element.
     */
    data class GazeExit(
        override val elementId: String,
        override val targetAvid: String?
    ) : OverlayInteractionEvent()

    /**
     * Overlay element dismissed.
     */
    data class Dismiss(
        override val elementId: String,
        override val targetAvid: String?
    ) : OverlayInteractionEvent()
}

/**
 * Swipe direction for gesture events.
 */
enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}

/**
 * Theme colors for overlay styling.
 *
 * Provides consistent color palette for overlay elements.
 *
 * @property primary Primary accent color
 * @property secondary Secondary accent color
 * @property background Background color
 * @property surface Surface/card color
 * @property error Error/warning color
 * @property onPrimary Text color on primary
 * @property onBackground Text color on background
 */
data class ThemeColors(
    val primary: String,
    val secondary: String,
    val background: String,
    val surface: String,
    val error: String,
    val onPrimary: String,
    val onBackground: String
) {
    companion object {
        val LIGHT = ThemeColors(
            primary = "#2196F3",
            secondary = "#03DAC6",
            background = "#FFFFFF",
            surface = "#F5F5F5",
            error = "#B00020",
            onPrimary = "#FFFFFF",
            onBackground = "#000000"
        )

        val DARK = ThemeColors(
            primary = "#BB86FC",
            secondary = "#03DAC6",
            background = "#121212",
            surface = "#1E1E1E",
            error = "#CF6679",
            onPrimary = "#000000",
            onBackground = "#FFFFFF"
        )
    }
}
