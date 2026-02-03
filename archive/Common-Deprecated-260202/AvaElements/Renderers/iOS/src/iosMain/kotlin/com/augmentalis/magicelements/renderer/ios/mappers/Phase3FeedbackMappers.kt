package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.components.phase3.feedback.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Phase 3 Feedback Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Phase 3 feedback components to SwiftUI equivalents:
 * - Alert, Snackbar
 * - Modal, Toast
 * - Confirm, ContextMenu
 */

// ============================================
// ALERT
// ============================================

/**
 * Maps Alert component to SwiftUI Alert dialog
 */
object AlertMapper {
    fun map(component: Alert, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        val props = mutableMapOf<String, Any>(
            "message" to component.message,
            "severity" to component.severity
        )
        if (component.onClose != null) {
            props["dismissButton"] = mapOf("label" to "OK", "callback" to "onClose")
        }

        return SwiftUIView(
            type = ViewType.Alert,
            properties = props,
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// SNACKBAR
// ============================================

/**
 * Maps Snackbar component to SwiftUI toast-style notification
 */
object SnackbarMapper {
    fun map(component: Snackbar, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Position at bottom
        modifiers.add(SwiftUIModifier.custom("position(bottom)"))

        // Apply theme colors
        theme?.colorScheme?.inverseSurface?.let {
            modifiers.add(SwiftUIModifier.background(ModifierConverter.convertColor(it)))
        }
        theme?.colorScheme?.inverseOnSurface?.let {
            modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(it)))
        }

        modifiers.add(SwiftUIModifier.cornerRadius(4f))
        modifiers.add(SwiftUIModifier.padding(16f))

        // Auto-dismiss animation
        modifiers.add(SwiftUIModifier.custom("animation(.easeInOut)"))

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        val props = mutableMapOf<String, Any>(
            "message" to component.message,
            "duration" to component.duration
        )
        if (component.onDismiss != null) {
            props["onDismiss"] = "callback"
        }

        return SwiftUIView(
            type = ViewType.HStack,
            properties = props,
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// MODAL
// ============================================

/**
 * Maps Modal component to SwiftUI sheet presentation
 */
object ModalMapper {
    fun map(component: Modal, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        val props = mutableMapOf<String, Any>(
            "isPresented" to component.open,
            "title" to (component.title ?: "")
        )
        if (component.onClose != null) {
            props["onClose"] = "callback"
        }

        return SwiftUIView(
            type = ViewType.Sheet,
            properties = props,
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// TOAST
// ============================================

/**
 * Maps Toast component to SwiftUI temporary overlay notification
 */
object ToastMapper {
    fun map(component: Toast, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Position at top or bottom based on component.position
        val position = when (component.position) {
            "bottom" -> "bottom"
            "center" -> "center"
            else -> "top"
        }
        modifiers.add(SwiftUIModifier.custom("position($position)"))

        // Background styling
        theme?.colorScheme?.inverseSurface?.let {
            modifiers.add(SwiftUIModifier.background(ModifierConverter.convertColor(it)))
        }

        theme?.colorScheme?.inverseOnSurface?.let {
            modifiers.add(SwiftUIModifier.foregroundColor(ModifierConverter.convertColor(it)))
        }

        modifiers.add(SwiftUIModifier.cornerRadius(8f))
        modifiers.add(SwiftUIModifier.padding(12f))
        modifiers.add(SwiftUIModifier.shadow(radius = 4f))

        // Animation
        modifiers.add(SwiftUIModifier.custom("animation(.spring())"))

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView.text(
            content = component.message,
            modifiers = modifiers
        ).copy(id = component.id)
    }
}

// ============================================
// CONFIRM
// ============================================

/**
 * Maps Confirm component to SwiftUI confirmation dialog
 */
object ConfirmMapper {
    fun map(component: Confirm, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView(
            type = ViewType.Alert,
            properties = mapOf(
                "message" to component.message,
                "confirmButton" to mapOf(
                    "label" to component.confirmText,
                    "callback" to "onConfirm"
                ),
                "cancelButton" to mapOf(
                    "label" to component.cancelText,
                    "callback" to "onCancel",
                    "role" to "cancel"
                )
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}

// ============================================
// CONTEXT MENU
// ============================================

/**
 * Maps ContextMenu component to SwiftUI contextMenu modifier
 */
object ContextMenuMapper {
    fun map(component: ContextMenu, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Items are simple strings in Phase3 ContextMenu
        val menuItems = component.items.mapIndexed { index, label ->
            mapOf(
                "label" to label,
                "index" to index,
                "action" to "callback"
            )
        }

        return SwiftUIView(
            type = ViewType.Menu,
            properties = mapOf(
                "items" to menuItems
            ),
            modifiers = modifiers,
            id = component.id
        )
    }
}
