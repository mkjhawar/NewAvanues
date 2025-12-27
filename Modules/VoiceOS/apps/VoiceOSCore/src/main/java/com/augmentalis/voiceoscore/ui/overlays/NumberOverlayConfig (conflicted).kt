/**
 * NumberOverlayConfig.kt - Configuration classes for number overlays
 *
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.ui.overlays

import android.graphics.Color
import android.graphics.Typeface

/**
 * Anchor point for number badge positioning
 */
enum class AnchorPoint {
    TOP_RIGHT,
    TOP_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
}

/**
 * Voice state for elements
 */
enum class ElementVoiceState {
    ENABLED_WITH_NAME,
    ENABLED_NO_NAME,
    DISABLED
}

/**
 * Number overlay style configuration
 */
data class NumberOverlayStyle(
    val circleRadius: Float = 12f,
    val offsetX: Float = 4f,
    val offsetY: Float = 4f,
    val hasNameColor: Int = Color.parseColor("#4CAF50"),
    val noNameColor: Int = Color.parseColor("#FFC107"),
    val disabledColor: Int = Color.parseColor("#9E9E9E"),
    val textColor: Int = Color.WHITE,
    val textSize: Float = 14f,
    val strokeWidth: Float = 2f,
    val strokeColor: Int = Color.WHITE,
    val numberColor: Int = Color.WHITE,
    val numberSize: Float = 14f,
    val fontWeight: Typeface = Typeface.DEFAULT_BOLD,
    val dropShadow: Boolean = true,
    val shadowColor: Int = Color.BLACK,
    val shadowRadius: Float = 4f,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 2f,
    val cacheTextBounds: Boolean = true,
    val anchorPoint: AnchorPoint = AnchorPoint.TOP_RIGHT
) {
    companion object {
        /**
         * Create standard overlay style
         */
        fun standard(): NumberOverlayStyle = NumberOverlayStyle()

        /**
         * Create compact overlay style
         */
        fun compact(): NumberOverlayStyle = NumberOverlayStyle(
            circleRadius = 10f,
            offsetX = 2f,
            offsetY = 2f,
            textSize = 12f
        )

        /**
         * Create large overlay style
         */
        fun large(): NumberOverlayStyle = NumberOverlayStyle(
            circleRadius = 16f,
            offsetX = 6f,
            offsetY = 6f,
            textSize = 16f
        )
    }
}

/**
 * Render configuration
 */
data class RenderConfig(
    val showDisabled: Boolean = true,
    val showOnlyWithNames: Boolean = false,
    val maxOverlays: Int = 99,
    val fadeInDuration: Long = 200,
    val fadeOutDuration: Long = 150,
    val styleVariant: String = "standard",
    val hardwareAcceleration: Boolean = true,
    val hideOnWindowFocusLoss: Boolean = true,
    val enabled: Boolean = true,
    val paintPooling: Boolean = true,
    val cacheTextBounds: Boolean = true,
    val partialInvalidation: Boolean = true,
    val maxOverlaysPerFrame: Int = 20,
    val targetFrameTimeMs: Long = 16
)

/**
 * Number overlay configuration
 */
data class NumberOverlayConfig(
    val style: NumberOverlayStyle = NumberOverlayStyle.standard(),
    val renderConfig: RenderConfig = RenderConfig(),
    val anchorPoint: AnchorPoint = AnchorPoint.TOP_RIGHT
)
