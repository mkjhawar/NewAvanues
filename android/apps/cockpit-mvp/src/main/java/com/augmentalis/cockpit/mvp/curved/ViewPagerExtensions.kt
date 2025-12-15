package com.augmentalis.cockpit.mvp.curved

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

/**
 * Configure ViewPager2 for curved workspace preview effect
 *
 * Shows center window at 70% width with curved previews on both sides.
 * Side windows scale down and curve away for depth effect.
 *
 * Based on reference implementation from Task_Cockpit module.
 */
fun ViewPager2.setPreviewBothSide() {
    // Show 3 pages at once (center + left/right previews)
    offscreenPageLimit = 3

    // Center window width in percentage (70% of screen)
    val centerItemSize = 0.7f

    // Scale factor for side windows (each side window is 1/2 size of previous)
    val decrement = 2.0f

    /**
     * Calculate translation X for curved arc layout
     */
    fun getTranslationX(position: Float): Float {
        fun getTranslationXInt(position: Int): Float {
            if (position == 0) return 0f

            var translationX = -width * position.toFloat()
            for (reversePosition in position downTo 0) {
                val pageSize = width * centerItemSize * (decrement.pow(reversePosition))
                translationX += pageSize * if (reversePosition == 0 || reversePosition == position) 0.5f else 1f
            }
            return translationX
        }

        val previousTranslationX = getTranslationXInt(floor(abs(position)).toInt())
        val nextTranslationX = getTranslationXInt(ceil(abs(position)).toInt())
        var translationX = previousTranslationX + ((nextTranslationX - previousTranslationX) * (abs(position) % 1))
        if (position < 0f) translationX = 0 - translationX
        return translationX
    }

    post {
        val pageTransformer = ViewPager2.PageTransformer { page: View, position: Float ->
            // Scale windows based on position (REFERENCE FORMULA: side windows larger)
            // This creates depth illusion - closer windows appear larger
            val scale = centerItemSize * (decrement.pow(abs(position))) * 0.98f
            page.scaleX = scale
            page.scaleY = scale

            // Position windows in curved arc
            page.translationX = getTranslationX(position)

            // CRITICAL: Apply curve transformation to ALL pages (reference code behavior)
            // Left window (position < 0): Squeeze right edge
            // Right window (position > 0): Squeeze left edge
            // Center (position ≈ 0): No squeeze (both sides at 1f)
            page.getCurvedImage()?.setHeightPercentage(
                leftHeight = if (position < 0) 1f else (1f / decrement),
                rightHeight = if (position < 0) (1f / decrement) else 1f
            )

            // Optional fade effect for depth (uncomment if desired)
            // page.alpha = 0.4f + (0.6f * (1f - abs(position).coerceAtMost(1f)))
        }
        setPageTransformer(pageTransformer)
    }
}

/**
 * Get CurvedImage view from page hierarchy
 */
private fun View.getCurvedImage(): CurvedImage? {
    if (this !is ViewGroup) return null
    if (this.childCount <= 0) return null

    // Search through view hierarchy for CurvedImage
    this.children.forEach { child ->
        if (child is CurvedImage) return child
        if (child is ViewGroup) {
            child.getCurvedImage()?.let { return it }
        }
    }
    return null
}

/**
 * Get content view (FrameLayout containing live WebView)
 */
private fun View.getContentView(): View? {
    if (this !is ViewGroup) return null
    if (this.childCount <= 0) return null

    // First child of container should be the content FrameLayout
    // (see WindowViewPagerAdapter.onCreateViewHolder)
    return if (this.childCount >= 1) this.getChildAt(0) else null
}
