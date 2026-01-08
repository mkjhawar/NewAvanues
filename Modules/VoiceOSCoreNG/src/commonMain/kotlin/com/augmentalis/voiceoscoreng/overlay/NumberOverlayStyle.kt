/**
 * NumberOverlayStyle.kt - Configuration for number badge styling
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * Provides styling configuration for number overlay badges displayed on UI elements
 * for voice control navigation. This is a simplified styling API focused on badge
 * appearance and behavior configuration.
 */
package com.augmentalis.voiceoscoreng.overlay

/**
 * Style configuration for number overlay badges.
 *
 * Provides customizable styling for circular badges that display element numbers
 * for voice control navigation. Supports various visual configurations and
 * behavioral settings.
 *
 * @property badgeSize Size of the badge in density-independent pixels (dp)
 * @property badgeColor Background color of the badge in 0xAARRGGBB format
 * @property textColor Color of the number text in 0xAARRGGBB format
 * @property fontSize Size of the number text in scaled pixels (sp)
 * @property borderWidth Width of the badge border in dp
 * @property borderColor Color of the badge border in 0xAARRGGBB format
 * @property cornerRadius Corner radius for rounded badge shapes in dp
 * @property offsetX Horizontal offset from anchor position in dp
 * @property offsetY Vertical offset from anchor position in dp
 * @property showOnClickable Whether to show badges on clickable elements
 * @property showOnScrollable Whether to show badges on scrollable elements
 * @property maxNumber Maximum number to display before showing "N+" format
 */
data class NumberOverlayStyle(
    val badgeSize: Int = 24,
    val badgeColor: Long = 0xFF2196F3L,  // Blue
    val textColor: Long = 0xFFFFFFFFL,   // White
    val fontSize: Int = 12,
    val borderWidth: Int = 1,
    val borderColor: Long = 0xFFFFFFFFL, // White
    val cornerRadius: Int = 12,          // Circular for default
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val showOnClickable: Boolean = true,
    val showOnScrollable: Boolean = true,
    val maxNumber: Int = 99              // Numbers > 99 show "99+"
) {
    companion object {
        /**
         * Default style with standard Material blue badge and white text.
         */
        val DEFAULT = NumberOverlayStyle()

        /**
         * Large style with increased badge size and font for better visibility.
         */
        val LARGE = NumberOverlayStyle(
            badgeSize = 32,
            fontSize = 16,
            cornerRadius = 16
        )

        /**
         * Minimal style with smaller size and no border for subtle appearance.
         */
        val MINIMAL = NumberOverlayStyle(
            badgeSize = 20,
            fontSize = 10,
            borderWidth = 0,
            cornerRadius = 10
        )

        /**
         * High contrast style with black background and yellow text/border
         * for maximum visibility in accessibility scenarios.
         */
        val HIGH_CONTRAST = NumberOverlayStyle(
            badgeColor = 0xFF000000L,  // Black
            textColor = 0xFFFFFF00L,   // Yellow
            borderColor = 0xFFFFFF00L, // Yellow
            borderWidth = 2
        )
    }

    /**
     * Formats a number for display in the badge.
     *
     * Numbers greater than [maxNumber] are displayed as "maxNumber+"
     * (e.g., "99+" when maxNumber is 99).
     *
     * @param number The number to format
     * @return Formatted string representation of the number
     */
    fun formatNumber(number: Int): String {
        return if (number > maxNumber) "$maxNumber+" else number.toString()
    }
}
