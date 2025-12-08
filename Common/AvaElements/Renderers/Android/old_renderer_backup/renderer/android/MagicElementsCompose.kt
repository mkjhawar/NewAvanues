package com.augmentalis.magicelements.renderer.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.augmentalis.magicelements.core.Component
import com.augmentalis.magicelements.core.Theme
import com.augmentalis.magicelements.dsl.MagicUI

/**
 * MagicElementsCompose - Convenience functions for rendering MagicElements in Compose
 *
 * This file provides high-level composable functions for integrating MagicElements
 * into Android Jetpack Compose applications.
 */

/**
 * Render a complete MagicUI with its theme
 *
 * Usage:
 * ```kotlin
 * setContent {
 *     MagicUI(ui = myMagicUI)
 * }
 * ```
 */
@Composable
fun MagicUI(
    ui: MagicUI,
    renderer: ComposeRenderer = ComposeRenderer()
) {
    CompositionLocalProvider(LocalComposeRenderer provides renderer) {
        renderer.RenderWithTheme(
            component = ui.root ?: return,
            theme = ui.theme
        )
    }
}

/**
 * Render a MagicElements component with optional theme
 *
 * Usage:
 * ```kotlin
 * RenderComponent(
 *     component = myComponent,
 *     theme = Themes.Material3Light
 * )
 * ```
 */
@Composable
fun RenderComponent(
    component: Component,
    theme: Theme? = null,
    renderer: ComposeRenderer = ComposeRenderer()
) {
    CompositionLocalProvider(LocalComposeRenderer provides renderer) {
        if (theme != null) {
            renderer.RenderWithTheme(component, theme)
        } else {
            renderer.RenderComponent(component)
        }
    }
}

/**
 * Render a component tree without theme
 *
 * Usage:
 * ```kotlin
 * RenderMagicElement {
 *     Column {
 *         Text("Hello")
 *         Button("Click Me")
 *     }
 * }
 * ```
 */
@Composable
fun RenderMagicElement(
    component: Component,
    renderer: ComposeRenderer = ComposeRenderer()
) {
    CompositionLocalProvider(LocalComposeRenderer provides renderer) {
        renderer.RenderComponent(component)
    }
}

/**
 * Access the current ComposeRenderer from anywhere in the composition
 *
 * Usage:
 * ```kotlin
 * val renderer = LocalRenderer.current
 * ```
 */
val LocalRenderer = LocalComposeRenderer
