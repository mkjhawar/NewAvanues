package com.augmentalis.avaelements.renderer.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.augmentalis.avanues.avamagic.components.core.Component
import com.augmentalis.avanues.avamagic.components.core.Theme
import com.augmentalis.avanues.avamagic.components.dsl.AvaUI

/**
 * AvaElementsCompose - Convenience functions for rendering AvaElements in Compose
 *
 * This file provides high-level composable functions for integrating AvaElements
 * into Android Jetpack Compose applications.
 */

/**
 * Render a complete AvaUI with its theme
 *
 * Usage:
 * ```kotlin
 * setContent {
 *     AvaUI(ui = myAvaUI)
 * }
 * ```
 */
@Composable
fun AvaUI(
    ui: AvaUI,
    renderer: ComposeRenderer = ComposeRenderer()
) {
    val root = ui.root ?: return
    CompositionLocalProvider(LocalComposeRenderer provides renderer) {
        renderer.RenderWithTheme(
            component = root,
            theme = ui.theme
        )
    }
}

/**
 * Render a AvaElements component with optional theme
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
