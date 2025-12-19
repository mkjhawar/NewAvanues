package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanues.avamagic.ui.core.display.ChipComponent

@OptIn(ExperimentalForeignApi::class)
class IOSChipRenderer {
    fun render(component: ChipComponent): UIView {
        val width = 100.0
        val height = 32.0

        val chipView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, width, height)
            backgroundColor = if (component.selected)
                UIColor.systemBlueColor.withAlphaComponent(0.2)
            else
                UIColor.systemGray6Color
            layer.cornerRadius = height / 2
            layer.masksToBounds = true
            layer.borderWidth = if (component.selected) 2.0 else 1.0
            layer.borderColor = if (component.selected)
                UIColor.systemBlueColor.CGColor
            else
                UIColor.systemGray4Color.CGColor
        }

        var xOffset = 8.0

        // Icon
        component.icon?.let { iconName ->
            val iconImageView = UIImageView().apply {
                frame = CGRectMake(xOffset, 8.0, 16.0, 16.0)
                image = UIImage.systemImageNamed(iconName)
                tintColor = if (component.selected) UIColor.systemBlueColor else UIColor.labelColor
            }
            chipView.addSubview(iconImageView)
            xOffset += 20.0
        }

        // Label
        val label = UILabel().apply {
            frame = CGRectMake(xOffset, 0.0, width - xOffset - 32.0, height)
            text = component.label
            textColor = if (component.selected) UIColor.systemBlueColor else UIColor.labelColor
            font = UIFont.systemFontOfSize(14.0)
        }
        chipView.addSubview(label)

        // Delete button
        if (component.deletable) {
            val deleteButton = UIButton.buttonWithType(UIButtonTypeSystem).apply {
                frame = CGRectMake(width - 28.0, 8.0, 16.0, 16.0)
                setImage(UIImage.systemImageNamed("xmark.circle.fill"), UIControlStateNormal)
                tintColor = UIColor.systemGrayColor
            }
            chipView.addSubview(deleteButton)
        }

        // Accessibility
        chipView.isAccessibilityElement = true
        chipView.accessibilityLabel = component.label
        chipView.accessibilityTraits = UIAccessibilityTraitButton
        if (component.selected) {
            chipView.accessibilityTraits = chipView.accessibilityTraits or UIAccessibilityTraitSelected
        }

        return chipView
    }
}
