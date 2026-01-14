package com.augmentalis.avaelements.renderer.desktop

import androidx.compose.runtime.Composable
import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.api.Renderer
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.core.ThemeProvider
import com.augmentalis.avaelements.components.phase1.form.*
import com.augmentalis.avaelements.components.phase1.display.*
import com.augmentalis.avaelements.components.phase1.layout.*
import com.augmentalis.avaelements.components.phase1.navigation.*
import com.augmentalis.avaelements.components.phase1.data.*
import com.augmentalis.avaelements.renderer.desktop.mappers.*

/**
 * Compose Desktop Renderer
 *
 * Renders AvaElements components using Jetpack Compose for Desktop.
 * Optimized for larger screens, keyboard/mouse input, and desktop UX patterns.
 *
 * Platform: macOS, Windows, Linux
 * Framework: Compose for Desktop
 * Target: JVM
 */
class ComposeDesktopRenderer(override val theme: Theme = ThemeProvider.getCurrentTheme()) : Renderer {

    override fun render(component: Component): Any = @Composable {
        when (component) {
            // Phase 1 - Form
            is Checkbox -> RenderCheckbox(component, theme)
            is TextField -> RenderTextField(component, theme)
            is Button -> RenderButton(component, theme)
            is Switch -> RenderSwitch(component, theme)

            // Phase 1 - Display
            is Text -> RenderText(component, theme)
            is Image -> RenderImage(component, theme)
            is Icon -> RenderIcon(component, theme)

            // Phase 1 - Layout
            is Container -> RenderContainer(component, theme)
            is Row -> RenderRow(component, theme)
            is Column -> RenderColumn(component, theme)
            is Card -> RenderCard(component, theme)

            // Phase 1 - Navigation & Data
            is ScrollView -> RenderScrollView(component, theme)
            is com.augmentalis.avaelements.components.phase1.data.List -> RenderList(component, theme)

            else -> {
                // Unknown component - just show placeholder
            }
        }
    }
}
