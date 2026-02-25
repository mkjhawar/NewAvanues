package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanueui.ui.core.display.BadgeComponent

@OptIn(ExperimentalForeignApi::class)
class IOSBadgeRenderer {
    fun render(component: BadgeComponent): UIView {
        val isDot = component.dot
        val size = if (isDot) 8.0 else 20.0
        val width = if (isDot) size else (component.content.length * 8 + 12).coerceAtLeast(20.0)

        return UIView().apply {
            frame = CGRectMake(0.0, 0.0, width, size)
            backgroundColor = parseColor(component.color)
            layer.cornerRadius = size / 2
            layer.masksToBounds = true

            if (!isDot) {
                val label = UILabel().apply {
                    frame = bounds
                    text = component.content
                    textColor = UIColor.whiteColor
                    font = UIFont.boldSystemFontOfSize(11.0)
                    textAlignment = NSTextAlignmentCenter
                }
                addSubview(label)
            }

            // Accessibility
            isAccessibilityElement = true
            accessibilityLabel = if (isDot) "Notification" else component.content
            accessibilityTraits = UIAccessibilityTraitStaticText
        }
    }

    private fun parseColor(color: com.augmentalis.avanueui.core.Color): UIColor {
        return when (color) {
            com.augmentalis.avanueui.core.Color.Blue -> UIColor.systemBlueColor
            com.augmentalis.avanueui.core.Color.Green -> UIColor.systemGreenColor
            com.augmentalis.avanueui.core.Color.Red -> UIColor.systemRedColor
            com.augmentalis.avanueui.core.Color.Orange -> UIColor.systemOrangeColor
            com.augmentalis.avanueui.core.Color.Purple -> UIColor.systemPurpleColor
            com.augmentalis.avanueui.core.Color.Gray -> UIColor.systemGrayColor
            com.augmentalis.avanueui.core.Color.White -> UIColor.whiteColor
        }
    }
}
