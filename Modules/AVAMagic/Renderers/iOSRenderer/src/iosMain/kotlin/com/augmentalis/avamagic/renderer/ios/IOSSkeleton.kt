package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import platform.QuartzCore.*
import kotlinx.cinterop.*
import com.augmentalis.avanues.avamagic.ui.core.display.SkeletonComponent
import com.augmentalis.avanues.avamagic.ui.core.display.SkeletonVariant

@OptIn(ExperimentalForeignApi::class)
class IOSSkeletonRenderer {
    fun render(component: SkeletonComponent): UIView {
        val width = component.width.toDouble()
        val height = component.height.toDouble()

        val skeletonView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, width, height)
            backgroundColor = component.backgroundColor?.let { parseColor(it) }
                ?: UIColor.systemGray5Color

            // Shape based on variant
            when (component.variant) {
                SkeletonVariant.CIRCULAR -> {
                    layer.cornerRadius = width / 2
                }
                SkeletonVariant.RECTANGULAR -> {
                    layer.cornerRadius = 4.0
                }
                SkeletonVariant.TEXT -> {
                    layer.cornerRadius = height / 2
                }
            }
            layer.masksToBounds = true

            // Animation
            if (component.animated) {
                addPulseAnimation()
            }

            // Accessibility
            isAccessibilityElement = true
            accessibilityLabel = component.contentDescription
            accessibilityTraits = UIAccessibilityTraitUpdatesFrequently
        }

        return skeletonView
    }

    private fun UIView.addPulseAnimation() {
        val animation = CABasicAnimation.animationWithKeyPath("opacity").apply {
            fromValue = 1.0
            toValue = 0.4
            duration = 1.0
            repeatCount = Float.POSITIVE_INFINITY
            autoreverses = true
            timingFunction = CAMediaTimingFunction.functionWithName(kCAMediaTimingFunctionEaseInEaseOut)
        }
        layer.addAnimation(animation, "pulse")
    }

    private fun parseColor(hex: String): UIColor {
        val cleanHex = hex.removePrefix("#")
        val rgb = cleanHex.toLongOrNull(16) ?: 0xE0E0E0

        val red = ((rgb shr 16) and 0xFF) / 255.0
        val green = ((rgb shr 8) and 0xFF) / 255.0
        val blue = (rgb and 0xFF) / 255.0

        return UIColor(red = red, green = green, blue = blue, alpha = 1.0)
    }
}
