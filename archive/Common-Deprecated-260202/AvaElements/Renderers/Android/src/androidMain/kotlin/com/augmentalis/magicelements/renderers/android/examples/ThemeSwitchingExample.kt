package com.augmentalis.avaelements.renderers.android.examples

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.components.phase1.form.Button as MagicButton
import com.augmentalis.avaelements.components.phase1.display.Text as MagicText
import com.augmentalis.avaelements.renderers.android.ComposeRenderer

/**
 * Example demonstrating theme switching and hot reload
 *
 * This example shows how components automatically inherit theme colors
 * and how theme changes propagate to all components in real-time.
 */
@Composable
fun ThemeSwitchingExample() {
    // Observe current theme
    val currentTheme by ThemeProvider.currentTheme.collectAsState()

    // Create renderer with current theme
    val renderer = remember(currentTheme) { ComposeRenderer(currentTheme) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Title
        Text("AvaElements Theme Switching Demo")

        // Current theme name
        Text("Current Theme: ${currentTheme.name}")

        // Theme switcher button
        Button(onClick = {
            // Toggle between Material 3 Light and iOS 26 Light
            val newTheme = if (currentTheme.name == "Material 3 Light") {
                createiOS26LightTheme()
            } else {
                createMaterial3LightTheme()
            }
            ThemeProvider.setTheme(newTheme)
        }) {
            Text("Switch Theme")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Components below inherit theme automatically:")

        // AvaElements components that inherit theme
        val button = MagicButton(
            id = "example-button",
            text = "Primary Button",
            onClick = {},
            enabled = true
        )

        val text = MagicText(
            id = "example-text",
            content = "This text uses theme colors"
        )

        // Render components - they automatically use theme colors
        renderer.render(button).invoke()
        Spacer(modifier = Modifier.height(8.dp))
        renderer.render(text).invoke()
    }
}

/**
 * Create Material 3 Light theme
 */
fun createMaterial3LightTheme() = Theme(
    name = "Material 3 Light",
    platform = ThemePlatform.Material3_Expressive,
    colorScheme = ColorScheme.Material3Light,
    typography = Typography.Material3,
    shapes = Shapes.Material3,
    spacing = SpacingScale(),
    elevation = ElevationScale()
)

/**
 * Create iOS 26 Light theme
 */
fun createiOS26LightTheme() = Theme(
    name = "iOS 26 Light",
    platform = ThemePlatform.Material3_Expressive,  // Using Material3 platform for now
    colorScheme = ColorScheme.iOS26Light,
    typography = Typography.Material3,
    shapes = Shapes.Material3,
    spacing = SpacingScale(),
    elevation = ElevationScale()
)
