package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.magicui.ui.core.display.TooltipComponent

@OptIn(ExperimentalForeignApi::class)
class IOSTooltipRenderer {
    fun render(component: TooltipComponent): UIView {
        val tooltipView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, 200.0, 40.0)
            backgroundColor = UIColor.darkGrayColor.withAlphaComponent(0.9)
            layer.cornerRadius = 8.0
            layer.masksToBounds = false
            layer.shadowColor = UIColor.blackColor.CGColor
            layer.shadowOpacity = 0.3f
            layer.shadowOffset = CGSizeMake(0.0, 2.0)
            layer.shadowRadius = 4.0

            // Label
            val label = UILabel().apply {
                frame = CGRectMake(8.0, 0.0, 184.0, 40.0)
                text = component.text
                textColor = UIColor.whiteColor
                font = UIFont.systemFontOfSize(12.0)
                textAlignment = NSTextAlignmentCenter
                numberOfLines = 2
            }
            addSubview(label)

            // Accessibility
            isAccessibilityElement = true
            accessibilityLabel = "Tooltip: ${component.text}"
            accessibilityTraits = UIAccessibilityTraitStaticText

            // Initially hidden
            alpha = 0.0
        }

        return tooltipView
    }

    fun show(tooltipView: UIView) {
        UIView.animateWithDuration(0.2) {
            tooltipView.alpha = 1.0
        }
    }

    fun hide(tooltipView: UIView) {
        UIView.animateWithDuration(0.2) {
            tooltipView.alpha = 0.0
        }
    }
}
