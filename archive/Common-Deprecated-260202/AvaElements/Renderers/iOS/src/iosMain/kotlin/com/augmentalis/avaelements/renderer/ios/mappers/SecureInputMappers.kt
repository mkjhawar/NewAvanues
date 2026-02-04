package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.input.*

/**
 * iOS SwiftUI Mappers for Flutter Material Parity - Secure Input Components
 *
 * This file maps specialized secure input components (PIN, OTP, Masked) to iOS SwiftUI
 * bridge representations. These components provide specialized input handling for
 * sensitive data entry with automatic formatting and validation.
 *
 * Architecture:
 * Flutter Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented:
 * - PinInput: PIN code entry with individual digit boxes
 * - OTPInput: One-time password verification input
 * - MaskInput: Masked text input with custom formatting patterns
 *
 * @since 3.0.0-flutter-parity
 */

// ============================================
// PIN INPUT
// ============================================

/**
 * Maps PinInput to custom SwiftUI PIN entry view
 *
 * SwiftUI implementation uses HStack of individual TextFields:
 * - Each digit in its own box with focus management
 * - Automatic advancement to next field on entry
 * - Backspace moves to previous field
 * - Optional masking with bullet characters
 * - Material Design 3 styling with rounded corners
 * - Error state with red borders
 *
 * Visual parity with Flutter's pin_code_fields package
 */
object PinInputMapper {
    fun map(
        component: PinInput,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add label if present
        val labelView = component.label?.let { label ->
            SwiftUIView.text(
                content = label,
                modifiers = listOf(
                    SwiftUIModifier.fontSize(14f),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                )
            )
        }

        // Apply theme colors
        val primaryColor = theme?.colorScheme?.primary?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.primary

        val errorColor = theme?.colorScheme?.error?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.system("systemRed")

        // Determine border color based on error state
        val borderColor = if (component.errorText != null) {
            errorColor
        } else {
            SwiftUIColor.system("systemGray4")
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Build properties map
        val properties = mutableMapOf<String, Any>(
            "value" to component.value,
            "length" to component.length,
            "masked" to component.masked,
            "enabled" to component.enabled,
            "borderColor" to borderColor,
            "focusColor" to primaryColor,
            "onValueChange" to "callback"
        )

        // Add onComplete callback if present
        if (component.onComplete != null) {
            properties["onComplete"] = "callback"
        }

        // Add error text if present
        component.errorText?.let { errorText ->
            properties["errorText"] = errorText
        }

        // Add accessibility description
        properties["contentDescription"] = component.getAccessibilityDescription()

        return SwiftUIView(
            type = ViewType.Custom("PinInput"),
            properties = properties,
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// OTP INPUT
// ============================================

/**
 * Maps OTPInput to custom SwiftUI OTP verification view
 *
 * Similar to PinInput but with OTP-specific features:
 * - Support for alphanumeric characters (not just digits)
 * - Paste support for complete OTP codes
 * - Auto-submit on completion (optional)
 * - Timer integration for resend functionality
 * - Material Design 3 styling
 * - Error state handling
 *
 * Visual parity with Flutter's otp_text_field package
 */
object OTPInputMapper {
    fun map(
        component: OTPInput,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add label if present
        val labelView = component.label?.let { label ->
            SwiftUIView.text(
                content = label,
                modifiers = listOf(
                    SwiftUIModifier.fontSize(14f),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.secondary)
                )
            )
        }

        // Apply theme colors
        val primaryColor = theme?.colorScheme?.primary?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.primary

        val errorColor = theme?.colorScheme?.error?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.system("systemRed")

        // Determine border color based on error state
        val borderColor = if (component.errorText != null) {
            errorColor
        } else {
            SwiftUIColor.system("systemGray4")
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Build properties map
        val properties = mutableMapOf<String, Any>(
            "value" to component.value,
            "length" to component.length,
            "enabled" to component.enabled,
            "autoSubmit" to component.autoSubmit,
            "borderColor" to borderColor,
            "focusColor" to primaryColor,
            "onValueChange" to "callback"
        )

        // Add onComplete callback if present
        if (component.onComplete != null) {
            properties["onComplete"] = "callback"
        }

        // Add error text if present
        component.errorText?.let { errorText ->
            properties["errorText"] = errorText
        }

        // Add accessibility description
        properties["contentDescription"] = component.getAccessibilityDescription()

        return SwiftUIView(
            type = ViewType.Custom("OTPInput"),
            properties = properties,
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// MASK INPUT
// ============================================

/**
 * Maps MaskInput to custom SwiftUI masked text field
 *
 * SwiftUI implementation with custom text formatting:
 * - Real-time formatting as user types
 * - Mask pattern interpretation (# = digit, A = letter, X = any)
 * - Visual separator preservation (dashes, spaces, etc.)
 * - Cursor positioning management
 * - Material Design 3 TextField styling
 * - Common mask patterns support (credit card, phone, SSN, etc.)
 *
 * Visual parity with Flutter's mask_text_input_formatter
 */
object MaskInputMapper {
    fun map(
        component: MaskInput,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Apply theme colors
        val primaryColor = theme?.colorScheme?.primary?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.primary

        val errorColor = theme?.colorScheme?.error?.let {
            ModifierConverter.convertColor(it)
        } ?: SwiftUIColor.system("systemRed")

        // Determine border color based on error state
        val borderColor = if (component.errorText != null) {
            errorColor
        } else {
            SwiftUIColor.system("systemGray4")
        }

        // Add standard TextField styling
        modifiers.add(SwiftUIModifier.textFieldStyle("roundedBorder"))

        // Apply theme accent color
        theme?.colorScheme?.primary?.let {
            modifiers.add(SwiftUIModifier.accentColor(ModifierConverter.convertColor(it)))
        }

        // Add disabled state if needed
        if (!component.enabled) {
            modifiers.add(SwiftUIModifier.disabled(true))
            modifiers.add(SwiftUIModifier.opacity(0.5f))
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Build properties map
        val properties = mutableMapOf<String, Any>(
            "value" to component.value,
            "mask" to component.mask,
            "enabled" to component.enabled,
            "required" to component.required,
            "borderColor" to borderColor,
            "focusColor" to primaryColor,
            "onValueChange" to "callback"
        )

        // Add label if present
        component.label?.let { label ->
            properties["label"] = label
        }

        // Add placeholder if present
        component.placeholder?.let { placeholder ->
            properties["placeholder"] = placeholder
        }

        // Add error text if present
        component.errorText?.let { errorText ->
            properties["errorText"] = errorText
        }

        // Add accessibility description
        properties["contentDescription"] = component.getAccessibilityDescription()

        return SwiftUIView(
            type = ViewType.Custom("MaskInput"),
            properties = properties,
            modifiers = modifiers,
            id = component.id
        )
    }
}
