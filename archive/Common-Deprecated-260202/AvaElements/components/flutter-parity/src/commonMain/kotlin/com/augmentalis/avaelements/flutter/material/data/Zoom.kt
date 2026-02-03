package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Zoom component - Flutter Material parity
 *
 * Zoom and pan controls for images and content, enabling detailed inspection.
 * Commonly used in image galleries, maps, diagrams, and detailed content viewing.
 *
 * **Flutter Equivalent:** InteractiveViewer
 * **Material Design 3:** Custom implementation
 *
 * ## Features
 * - Pinch-to-zoom gesture support
 * - Pan/drag support
 * - Zoom controls (buttons)
 * - Min/max zoom limits
 * - Double-tap to zoom
 * - Reset to original size
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Zoom(
 *     imageUrl = "detailed-diagram.png",
 *     initialScale = 1.0f,
 *     minScale = 0.5f,
 *     maxScale = 4.0f,
 *     showControls = true,
 *     enableDoubleTap = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property imageUrl URL or path to image to zoom
 * @property initialScale Initial zoom scale (1.0 = 100%)
 * @property minScale Minimum zoom scale
 * @property maxScale Maximum zoom scale
 * @property showControls Whether to show zoom in/out buttons
 * @property enablePan Whether panning is enabled
 * @property enableDoubleTap Whether double-tap to zoom is enabled
 * @property controlsPosition Position of zoom controls
 * @property contentDescription Accessibility description for TalkBack
 * @property onScaleChanged Callback when scale changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Zoom(
    override val type: String = "Zoom",
    override val id: String? = null,
    val imageUrl: String,
    val initialScale: Float = 1.0f,
    val minScale: Float = 0.5f,
    val maxScale: Float = 4.0f,
    val showControls: Boolean = true,
    val enablePan: Boolean = true,
    val enableDoubleTap: Boolean = true,
    val controlsPosition: ControlsPosition = ControlsPosition.BottomRight,
    val contentDescription: String? = null,
    @Transient
    val onScaleChanged: ((Float) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Position of zoom controls
     */
    enum class ControlsPosition {
        /** Top left corner */
        TopLeft,

        /** Top right corner */
        TopRight,

        /** Bottom left corner */
        BottomLeft,

        /** Bottom right corner */
        BottomRight,

        /** Bottom center */
        BottomCenter
    }

    /**
     * Validate scale value is within bounds
     */
    fun isScaleValid(scale: Float): Boolean {
        return scale in minScale..maxScale
    }

    /**
     * Get clamped scale value
     */
    fun clampScale(scale: Float): Float {
        return scale.coerceIn(minScale, maxScale)
    }

    companion object {
        /**
         * Create a simple zoom component with default settings
         */
        fun simple(imageUrl: String) = Zoom(
            imageUrl = imageUrl
        )

        /**
         * Create a zoom component without controls (gesture-only)
         */
        fun gestureOnly(
            imageUrl: String,
            maxScale: Float = 3.0f
        ) = Zoom(
            imageUrl = imageUrl,
            maxScale = maxScale,
            showControls = false
        )

        /**
         * Create a zoom component for detailed inspection (high zoom)
         */
        fun detailed(
            imageUrl: String,
            maxScale: Float = 8.0f
        ) = Zoom(
            imageUrl = imageUrl,
            maxScale = maxScale
        )
    }
}
