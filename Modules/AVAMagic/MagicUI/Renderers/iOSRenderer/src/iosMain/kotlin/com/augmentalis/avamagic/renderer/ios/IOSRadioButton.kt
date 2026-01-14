package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.magicui.ui.core.form.RadioButtonComponent

/**
 * iOS Renderer for RadioButton Component
 *
 * Renders AVAMagic RadioButton as custom button with circular selection.
 * iOS doesn't have native radio button, so we create custom UIButton.
 *
 * @author Manoj Jhawar
 * @since 2025-11-19
 */
@OptIn(ExperimentalForeignApi::class)
class IOSRadioButtonRenderer {

    /**
     * Render RadioButton component as UIView with circular button
     */
    fun render(component: RadioButtonComponent): UIView {
        return UIView().apply {
            frame = CGRectMake(0.0, 0.0, 300.0, 44.0)

            // Radio button (circular)
            val radioButton = UIButton.buttonWithType(UIButtonTypeCustom).apply {
                frame = CGRectMake(0.0, 10.0, 24.0, 24.0)
                layer.borderWidth = 2.0
                layer.borderColor = UIColor.systemBlueColor.CGColor
                layer.cornerRadius = 12.0 // Half of width/height for circle

                if (component.selected) {
                    // Inner filled circle for selected state
                    val innerCircle = UIView().apply {
                        frame = CGRectMake(6.0, 6.0, 12.0, 12.0)
                        backgroundColor = UIColor.systemBlueColor
                        layer.cornerRadius = 6.0
                        isUserInteractionEnabled = false
                    }
                    addSubview(innerCircle)
                    backgroundColor = UIColor.clearColor
                } else {
                    backgroundColor = UIColor.clearColor
                }

                enabled = component.enabled
                if (!component.enabled) {
                    layer.borderColor = UIColor.systemGrayColor.CGColor
                }
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

            addSubview(radioButton)
            addSubview(label)

            // Apply style
            applyStyle(component)
        }
    }

    /**
     * Render RadioGroup (multiple radio buttons)
     */
    fun renderGroup(
        options: List<RadioButtonComponent>,
        groupName: String,
        selectedValue: String?
    ): UIStackView {
        return UIStackView().apply {
            axis = UILayoutConstraintAxisVertical
            spacing = 8.0
            distribution = UIStackViewDistributionFillEqually

            options.forEach { option ->
                val radioView = render(option.copy(
                    selected = option.value == selectedValue
                ))
                addArrangedSubview(radioView)
            }
        }
    }

    /**
     * Apply component style
     */
    private fun UIView.applyStyle(component: RadioButtonComponent) {
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
