package com.augmentalis.avaelements.renderer.ios

import com.augmentalis.avaelements.renderer.ios.bridge.SwiftUIView
import com.augmentalis.avaelements.renderer.ios.bridge.ViewType

/**
 * iOS SwiftUI Renderer Examples
 *
 * Demonstrates how to use the SwiftUI renderer to convert AvaElements
 * components to iOS native views.
 *
 * TODO: Update these examples to use the current MagicUI component API.
 * The examples need to be rewritten to match:
 * - Phase 1 components (Text, Button, Icon, TextField, etc.)
 * - Phase 2 components (Card, Alert, etc.)
 * - Phase 3 components (Avatar, Divider, etc.)
 *
 * Current status: Placeholder until component DSL is finalized.
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Example: Simple Text View
 *
 * Creates a basic text view to demonstrate the renderer bridge.
 */
fun createSimpleTextView(text: String = "Hello, MagicUI!"): SwiftUIView {
    return SwiftUIView(
        type = ViewType.Text,
        properties = mapOf(
            "text" to text
        ),
        modifiers = emptyList()
    )
}

/**
 * Example: Basic VStack Layout
 *
 * Creates a vertical stack with example content.
 */
fun createBasicVStackView(): SwiftUIView {
    return SwiftUIView(
        type = ViewType.VStack,
        properties = mapOf(
            "spacing" to 16f,
            "alignment" to "Center"
        ),
        children = listOf(
            SwiftUIView(
                type = ViewType.Text,
                properties = mapOf("text" to "Title")
            ),
            SwiftUIView(
                type = ViewType.Text,
                properties = mapOf("text" to "Subtitle")
            )
        ),
        modifiers = emptyList()
    )
}

// TODO: Add more comprehensive examples when component API is stable:
// - createiOSLoginScreen(): Login form with TextField, Button
// - createiOSSettingsScreen(): Settings list with toggles, navigation
// - createiOSCardListScreen(): Card-based layout with images
// - createiOSNavigationScreen(): Navigation with tabs, app bar
