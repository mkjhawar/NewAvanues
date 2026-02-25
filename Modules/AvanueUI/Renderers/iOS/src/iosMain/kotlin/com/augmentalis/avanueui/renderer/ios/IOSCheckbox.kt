package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanueui.ui.core.form.CheckboxComponent

/**
 * iOS Renderer for Checkbox Component
 *
 * Renders AvanueUI Checkbox as native UISwitch or custom checkbox view.
 * iOS doesn't have native checkbox, so we use UIButton with checkmark.
 *
 * @author Manoj Jhawar
 * @since 2025-11-19
 */
@OptIn(ExperimentalForeignApi::class)
class IOSCheckboxRenderer {

    /**
     * Render Checkbox component as UIButton with checkmark
     */
    fun render(component: CheckboxComponent): UIView {
        return UIView().apply {
            // Container view
            frame = CGRectMake(0.0, 0.0, 300.0, 44.0)

            // Checkbox button (square with checkmark)
            val checkbox = UIButton.buttonWithType(UIButtonTypeCustom).apply {
                frame = CGRectMake(0.0, 0.0, 24.0, 24.0)
                layer.borderWidth = 2.0
                layer.borderColor = UIColor.systemBlueColor.CGColor
                layer.cornerRadius = 4.0

                // Set checkmark image if checked
                if (component.checked) {
                    setImage(createCheckmarkImage(), forState = UIControlStateNormal)
                    backgroundColor = UIColor.systemBlueColor
                } else {
                    setImage(null, forState = UIControlStateNormal)
                    backgroundColor = UIColor.clearColor
                }

                // Handle indeterminate state
                if (component.indeterminate) {
                    setImage(createDashImage(), forState = UIControlStateNormal)
                    backgroundColor = UIColor.systemGrayColor
                }

                enabled = component.enabled
            }

            // Label
            val label = UILabel().apply {
                frame = CGRectMake(34.0, 0.0, 266.0, 44.0)
                text = component.label
                font = UIFont.systemFontOfSize(when (component.size) {
                    ComponentSize.SM -> 14.0
                    ComponentSize.MD -> 16.0
                    ComponentSize.LG -> 18.0
                    else -> 16.0
                })
                textColor = if (component.enabled) {
                    UIColor.labelColor
                } else {
                    UIColor.secondaryLabelColor
                }
            }

            addSubview(checkbox)
            addSubview(label)

            // Apply style
            applyStyle(component)
        }
    }

    /**
     * Create checkmark image for checked state
     */
    private fun createCheckmarkImage(): UIImage {
        // Create a simple checkmark path
        val size = CGSizeMake(20.0, 20.0)
        UIGraphicsBeginImageContextWithOptions(size, false, 0.0)

        val context = UIGraphicsGetCurrentContext()
        context?.let {
            UIColor.whiteColor.setStroke()
            it.setLineWidth(2.0)

            // Draw checkmark
            it.moveToPoint(CGPointMake(5.0, 10.0))
            it.addLineToPoint(CGPointMake(8.0, 13.0))
            it.addLineToPoint(CGPointMake(15.0, 6.0))
            it.strokePath()
        }

        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image ?: UIImage()
    }

    /**
     * Create dash image for indeterminate state
     */
    private fun createDashImage(): UIImage {
        val size = CGSizeMake(20.0, 20.0)
        UIGraphicsBeginImageContextWithOptions(size, false, 0.0)

        val context = UIGraphicsGetCurrentContext()
        context?.let {
            UIColor.whiteColor.setStroke()
            it.setLineWidth(2.0)

            // Draw horizontal dash
            it.moveToPoint(CGPointMake(5.0, 10.0))
            it.addLineToPoint(CGPointMake(15.0, 10.0))
            it.strokePath()
        }

        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return image ?: UIImage()
    }

    /**
     * Apply component style
     */
    private fun UIView.applyStyle(component: CheckboxComponent) {
        component.style?.let { style ->
            style.backgroundColor?.let { color ->
                backgroundColor = parseColor(color)
            }

            style.cornerRadius?.let { radius ->
                layer.cornerRadius = radius.toDouble()
                layer.masksToBounds = true
            }
        }
    }

    /**
     * Parse hex color string to UIColor
     */
    private fun parseColor(hex: String): UIColor {
        val cleanHex = hex.removePrefix("#")
        val rgb = cleanHex.toLongOrNull(16) ?: 0x000000

        val red = ((rgb shr 16) and 0xFF) / 255.0
        val green = ((rgb shr 8) and 0xFF) / 255.0
        val blue = (rgb and 0xFF) / 255.0

        return UIColor(red = red, green = green, blue = blue, alpha = 1.0)
    }
}
