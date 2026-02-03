package com.augmentalis.avaelements.flutter.material.layout

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * AspectRatio component - Flutter Material parity
 *
 * A container that maintains a specific aspect ratio for its child content.
 * Automatically calculates height based on width and aspect ratio.
 *
 * **Web Equivalent:** CSS `aspect-ratio` property, `AspectRatio` (MUI)
 * **Material Design 3:** https://m3.material.io/foundations/layout/understanding-layout/overview
 *
 * ## Features
 * - Maintains fixed aspect ratio
 * - Fills available width
 * - Auto-calculates height
 * - Child content positioned inside
 * - Common aspect ratios predefined (16:9, 4:3, 1:1, etc.)
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * AspectRatio(
 *     ratio = AspectRatio.Ratio.SixteenByNine,
 *     child = Image(url = "video-thumbnail.jpg")
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property ratio Aspect ratio (width / height)
 * @property child Child component to render inside
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-layout-components
 */
data class AspectRatio(
    override val type: String = "AspectRatio",
    override val id: String? = null,
    val ratio: Ratio = Ratio.SixteenByNine,
    val child: Component? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Common aspect ratios
     */
    sealed class Ratio(val value: Float) {
        /** 16:9 aspect ratio (1.778) - Standard widescreen */
        object SixteenByNine : Ratio(16f / 9f)

        /** 4:3 aspect ratio (1.333) - Standard TV */
        object FourByThree : Ratio(4f / 3f)

        /** 1:1 aspect ratio (1.0) - Square */
        object Square : Ratio(1f)

        /** 3:2 aspect ratio (1.5) - Photography */
        object ThreeByTwo : Ratio(3f / 2f)

        /** 21:9 aspect ratio (2.333) - Ultrawide */
        object TwentyOneByNine : Ratio(21f / 9f)

        /** 9:16 aspect ratio (0.5625) - Vertical video */
        object NineBysSixteen : Ratio(9f / 16f)

        /** Custom aspect ratio */
        data class Custom(val customRatio: Float) : Ratio(customRatio) {
            init {
                require(customRatio > 0f) { "Aspect ratio must be positive" }
            }
        }

        companion object {
            /**
             * Create aspect ratio from width and height
             */
            fun fromDimensions(width: Float, height: Float): Ratio {
                require(width > 0f && height > 0f) { "Dimensions must be positive" }
                return Custom(width / height)
            }
        }
    }

    /**
     * Get numeric aspect ratio value
     */
    fun getRatioValue(): Float {
        return ratio.value
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Aspect ratio container"
        val ratioText = when (ratio) {
            is Ratio.SixteenByNine -> "16:9"
            is Ratio.FourByThree -> "4:3"
            is Ratio.Square -> "square"
            is Ratio.ThreeByTwo -> "3:2"
            is Ratio.TwentyOneByNine -> "21:9"
            is Ratio.NineBysSixteen -> "9:16"
            is Ratio.Custom -> {
                val rounded = (ratio.value * 100).toInt() / 100.0
                rounded.toString()
            }
        }
        return "$base, $ratioText aspect ratio"
    }

    /**
     * Validate aspect ratio
     */
    fun isRatioValid(): Boolean {
        return ratio.value > 0f
    }

    companion object {
        /**
         * Create 16:9 aspect ratio container
         */
        fun widescreen(
            child: Component? = null
        ) = AspectRatio(
            ratio = Ratio.SixteenByNine,
            child = child
        )

        /**
         * Create square aspect ratio container
         */
        fun square(
            child: Component? = null
        ) = AspectRatio(
            ratio = Ratio.Square,
            child = child
        )

        /**
         * Create 4:3 aspect ratio container
         */
        fun standard(
            child: Component? = null
        ) = AspectRatio(
            ratio = Ratio.FourByThree,
            child = child
        )

        /**
         * Create custom aspect ratio container
         */
        fun custom(
            ratio: Float,
            child: Component? = null
        ) = AspectRatio(
            ratio = Ratio.Custom(ratio),
            child = child
        )

        /**
         * Create from width and height dimensions
         */
        fun fromDimensions(
            width: Float,
            height: Float,
            child: Component? = null
        ) = AspectRatio(
            ratio = Ratio.fromDimensions(width, height),
            child = child
        )
    }
}
