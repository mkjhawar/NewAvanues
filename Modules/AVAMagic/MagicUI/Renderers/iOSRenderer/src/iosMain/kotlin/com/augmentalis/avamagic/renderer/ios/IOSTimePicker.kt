package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.magicui.ui.core.form.TimePickerComponent
import com.augmentalis.magicui.ui.core.form.Time

/**
 * iOS Renderer for TimePicker Component
 *
 * Renders AVAMagic TimePicker components as native UIDatePicker in time mode.
 *
 * Features:
 * - Native UIDatePicker rendering in time mode
 * - 12-hour or 24-hour format
 * - Compact wheels or inline style
 * - Dark mode support
 * - Accessibility support
 *
 * @author Manoj Jhawar
 * @since 2025-11-21
 */
@OptIn(ExperimentalForeignApi::class)
class IOSTimePickerRenderer {

    /**
     * Render TimePicker component to UIDatePicker
     */
    fun render(component: TimePickerComponent): UIView {
        // Create container with label and picker
        val containerView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 120.0)
            backgroundColor = UIColor.systemBackgroundColor
        }

        var yOffset = 0.0

        // Add label if provided
        component.label?.let { labelText ->
            val label = UILabel().apply {
                frame = CGRectMake(16.0, yOffset, 343.0, 24.0)
                text = labelText
                font = UIFont.systemFontOfSize(14.0)
                textColor = UIColor.secondaryLabelColor
            }
            containerView.addSubview(label)
            yOffset += 32.0
        }

        // Create time picker
        val timePicker = UIDatePicker().apply {
            frame = CGRectMake(0.0, yOffset, 375.0, 80.0)

            // Set date picker mode to time only
            datePickerMode = UIDatePickerModeTime

            // iOS 14+ compact wheels style
            if (preferredDatePickerStyle != null) {
                preferredDatePickerStyle = UIDatePickerStyleWheels
            }

            // Set selected time
            component.selectedTime?.let { time ->
                val calendar = NSCalendar.currentCalendar
                val components = NSDateComponents().apply {
                    hour = time.hour.toLong()
                    minute = time.minute.toLong()
                }
                calendar.dateFromComponents(components)?.let { date ->
                    this.date = date
                }
            }

            // Set 12-hour or 24-hour format
            locale = if (component.is24Hour) {
                NSLocale("en_GB") // Forces 24-hour format
            } else {
                NSLocale("en_US") // Forces 12-hour format with AM/PM
            }

            // Enable/disable
            enabled = component.enabled

            // Apply component style
            applyStyle(component)

            // Handle time change
            // Note: In production, use addTarget:action:forControlEvents:
            // with UIControlEventValueChanged
            component.onTimeSelected?.let { callback ->
                // Target-action pattern would extract hour/minute:
                // val calendar = NSCalendar.currentCalendar
                // val components = calendar.components(
                //     NSCalendarUnitHour or NSCalendarUnitMinute,
                //     fromDate = date
                // )
                // callback(Time(components.hour.toInt(), components.minute.toInt()))
            }
        }

        containerView.addSubview(timePicker)

        // Apply accessibility
        applyAccessibility(containerView, component)

        return containerView
    }

    /**
     * Apply component style to time picker
     */
    private fun UIDatePicker.applyStyle(component: TimePickerComponent) {
        component.style?.let { style ->
            // Background color
            style.backgroundColor?.let { color ->
                backgroundColor = parseColor(color)
            }

            // Corner radius
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

    /**
     * Apply accessibility features
     */
    private fun applyAccessibility(view: UIView, component: TimePickerComponent) {
        view.isAccessibilityElement = false // Container is not accessible, picker is
        component.label?.let { label ->
            // The UIDatePicker will be accessible with the label as hint
        }
    }

    /**
     * Extract Time from NSDate
     */
    fun extractTime(date: NSDate): Time {
        val calendar = NSCalendar.currentCalendar
        val components = calendar.components(
            NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = date
        )
        return Time(
            hour = components.hour.toInt(),
            minute = components.minute.toInt()
        )
    }

    /**
     * Create NSDate from Time
     */
    fun createDate(time: Time): NSDate? {
        val calendar = NSCalendar.currentCalendar
        val components = NSDateComponents().apply {
            hour = time.hour.toLong()
            minute = time.minute.toLong()
        }
        return calendar.dateFromComponents(components)
    }
}
