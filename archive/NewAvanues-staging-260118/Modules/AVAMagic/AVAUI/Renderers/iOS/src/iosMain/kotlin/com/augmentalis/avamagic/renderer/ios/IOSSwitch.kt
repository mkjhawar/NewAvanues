package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avamagic.ui.core.form.SwitchComponent

/**
 * iOS Renderer for Switch Component
 *
 * Renders AVAMagic Switch as native UISwitch.
 *
 * @author Manoj Jhawar
 * @since 2025-11-19
 */
@OptIn(ExperimentalForeignApi::class)
class IOSSwitchRenderer {

    /**
     * Render Switch component as UIView with UISwitch and label
     */
    fun render(component: SwitchComponent): UIView {
        return UIView().apply {
            frame = CGRectMake(0.0, 0.0, 300.0, 44.0)

            // Label
            val label = UILabel().apply {
                frame = CGRectMake(0.0, 0.0, 240.0, 44.0)
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

            // Switch
            val switchControl = UISwitch().apply {
                frame = CGRectMake(250.0, 6.0, 51.0, 31.0)
                on = component.checked
                enabled = component.enabled

                // Custom colors if provided
                component.style?.let { style ->
                    style.accentColor?.let { color ->
                        onTintColor = parseColor(color)
                    }
                }
            }

            addSubview(label)
            addSubview(switchControl)

            // Apply style
            applyStyle(component)
        }
    }

    /**
     * Apply component style
     */
    private fun UIView.applyStyle(component: SwitchComponent) {
        component.style?.let { style ->
            style.backgroundColor?.let { color ->
                backgroundColor = parseColor(color)
            }

            style.cornerRadius?.let { radius ->
                layer.cornerRadius = radius.toDouble()
                layer.masksToBounds = true
            }

            style.padding?.let { padding ->
                frame = CGRectMake(
                    frame.origin.x + padding,
                    frame.origin.y + padding,
                    frame.size.width - (padding * 2),
                    frame.size.height - (padding * 2)
                )
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
