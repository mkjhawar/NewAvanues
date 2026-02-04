package com.augmentalis.avaelements.common.color

/**
 * Color Accessibility Operations
 *
 * Focused object responsible for WCAG compliance and
 * accessibility-related color calculations.
 *
 * SOLID Principle: Single Responsibility
 * This object handles only accessibility concerns,
 * separated from manipulation and color theory operations.
 */
object ColorAccessibility {

    /**
     * Get the best contrasting foreground color (black or white)
     * for the given background color.
     *
     * Uses WCAG luminance calculation to determine which
     * foreground provides better contrast.
     *
     * @param background The background color
     * @return Black or White, whichever provides better contrast
     */
    fun contrastingForeground(background: UniversalColor): UniversalColor {
        return if (background.luminance > 0.5f) {
            UniversalColor.Black
        } else {
            UniversalColor.White
        }
    }

    /**
     * Calculate the WCAG contrast ratio between two colors
     *
     * The contrast ratio ranges from 1:1 (no contrast) to 21:1 (maximum contrast).
     * WCAG requirements:
     * - AA normal text: 4.5:1
     * - AA large text: 3:1
     * - AAA normal text: 7:1
     * - AAA large text: 4.5:1
     *
     * @param color1 The first color (typically foreground)
     * @param color2 The second color (typically background)
     * @return Contrast ratio as a float (1.0 to 21.0)
     */
    fun contrastRatio(color1: UniversalColor, color2: UniversalColor): Float {
        val l1 = color1.luminance + 0.05f
        val l2 = color2.luminance + 0.05f
        return if (l1 > l2) l1 / l2 else l2 / l1
    }

    /**
     * Check if the color pair meets WCAG AA standard
     *
     * WCAG AA requires:
     * - 4.5:1 contrast ratio for normal text
     * - 3:1 contrast ratio for large text (18pt+ or 14pt+ bold)
     *
     * This method checks for normal text (4.5:1).
     *
     * @param foreground The foreground/text color
     * @param background The background color
     * @return true if contrast ratio >= 4.5:1
     */
    fun meetsWcagAA(foreground: UniversalColor, background: UniversalColor): Boolean {
        return contrastRatio(foreground, background) >= 4.5f
    }

    /**
     * Check if the color pair meets WCAG AAA standard
     *
     * WCAG AAA requires:
     * - 7:1 contrast ratio for normal text
     * - 4.5:1 contrast ratio for large text (18pt+ or 14pt+ bold)
     *
     * This method checks for normal text (7:1).
     *
     * @param foreground The foreground/text color
     * @param background The background color
     * @return true if contrast ratio >= 7:1
     */
    fun meetsWcagAAA(foreground: UniversalColor, background: UniversalColor): Boolean {
        return contrastRatio(foreground, background) >= 7f
    }
}
