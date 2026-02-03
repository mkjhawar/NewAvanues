package com.augmentalis.avaelements.core.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.core.ThemeProvider

/**
 * Android Compose integration for ThemeProvider
 */
@Composable
fun currentTheme(): Theme {
    val theme by ThemeProvider.currentTheme.collectAsState()
    return theme
}

@Composable
fun ProvideTheme(
    theme: Theme = ThemeProvider.getCurrentTheme(),
    content: @Composable () -> Unit
) {
    ThemeProvider.setTheme(theme)
    content()
}
