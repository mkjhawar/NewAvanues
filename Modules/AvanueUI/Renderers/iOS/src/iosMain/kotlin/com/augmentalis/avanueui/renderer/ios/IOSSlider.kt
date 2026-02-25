package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanueui.ui.core.form.SliderComponent

/**
 * iOS Renderer for Slider Component
 *
 * Renders AvanueUI Slider as native UISlider.
 *
 * @author Manoj Jhawar
 * @since 2025-11-19
 */
@OptIn(ExperimentalForeignApi::class)
class IOSSliderRenderer {

    /**
     * Render Slider component as UIView with UISlider and labels
     */
    fun render(component: SliderComponent): UIView {
        return UIView().apply {
            frame = CGRectMake(0.0, 0.0, 300.0, 80.0)

            // Label
            if (component.label != null) {
                val label = UILabel().apply {
                    frame = CGRectMake(0.0, 0.0, 300.0, 20.0)
                    text = component.label
                    font = UIFont.systemFontOfSize(16.0)
                    textColor = UIColor.labelColor
                }
                addSubview(label)
            }

            // Min/Max labels container
            val labelsView = UIView().apply {
                frame = CGRectMake(0.0, 25.0, 300.0, 20.0)

                // Min value label
                val minLabel = UILabel().apply {
                    frame = CGRectMake(0.0, 0.0, 50.0, 20.0)
                    text = component.min.toString()
                    font = UIFont.systemFontOfSize(12.0)
                    textColor = UIColor.secondaryLabelColor
                    textAlignment = NSTextAlignmentLeft
                }

                // Current value label (center)
                val valueLabel = UILabel().apply {
                    frame = CGRectMake(125.0, 0.0, 50.0, 20.0)
                    text = component.value.toInt().toString()
                    font = UIFont.boldSystemFontOfSize(14.0)
                    textColor = UIColor.systemBlueColor
                    textAlignment = NSTextAlignmentCenter
                }

                // Max value label
                val maxLabel = UILabel().apply {
                    frame = CGRectMake(250.0, 0.0, 50.0, 20.0)
                    text = component.max.toString()
                    font = UIFont.systemFontOfSize(12.0)
                    textColor = UIColor.secondaryLabelColor
                    textAlignment = NSTextAlignmentRight
                }

                addSubview(minLabel)
                addSubview(valueLabel)
                addSubview(maxLabel)
            }

            // Slider
            val slider = UISlider().apply {
                frame = CGRectMake(0.0, 50.0, 300.0, 30.0)
                minimumValue = component.min.toFloat()
                maximumValue = component.max.toFloat()
                value = component.value.toFloat()
                enabled = component.enabled

                // Step increments
                if (component.step > 0) {
                    // UISlider doesn't have direct step property
                    // Would need to handle in value change callback
                }

                // Custom colors
                component.style?.let { style ->
                    style.accentColor?.let { color ->
                        minimumTrackTintColor = parseColor(color)
                    }
                    style.trackColor?.let { color ->
                        maximumTrackTintColor = parseColor(color)
                    }
                    style.thumbColor?.let { color ->
                        thumbTintColor = parseColor(color)
                    }
                }

                // Show/hide value indicator
                if (!component.showValue) {
                    labelsView.subviews[1].hidden = true
                }
            }

            addSubview(labelsView)
            addSubview(slider)

            // Apply style
            applyStyle(component)
        }
    }

    /**
     * Render RangeSlider (two-thumb slider)
     */
    fun renderRange(
        min: Float,
        max: Float,
        lowerValue: Float,
        upperValue: Float,
        enabled: Boolean = true
    ): UIView {
        // iOS doesn't have native range slider
        // Would need custom implementation with two UISliders or third-party library
        return UIView().apply {
            frame = CGRectMake(0.0, 0.0, 300.0, 80.0)

            val label = UILabel().apply {
                frame = CGRectMake(0.0, 30.0, 300.0, 20.0)
                text = "Range Slider (Custom Implementation Required)"
                font = UIFont.systemFontOfSize(14.0)
                textColor = UIColor.secondaryLabelColor
                textAlignment = NSTextAlignmentCenter
            }
            addSubview(label)
        }
    }

    /**
     * Apply component style
     */
    private fun UIView.applyStyle(component: SliderComponent) {
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
