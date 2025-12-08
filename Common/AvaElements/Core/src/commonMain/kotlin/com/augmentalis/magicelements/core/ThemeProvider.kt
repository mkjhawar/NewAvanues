package com.augmentalis.avaelements.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global theme provider with hot reload support
 */
object ThemeProvider {
    private val defaultTheme = Theme(
        name = "Material 3 Light",
        platform = ThemePlatform.Material3_Expressive,
        colorScheme = ColorScheme.Material3Light,
        typography = Typography.Material3,
        shapes = Shapes.Material3,
        spacing = SpacingScale(),
        elevation = ElevationScale()
    )
    
    private val _currentTheme = MutableStateFlow(defaultTheme)
    val currentTheme: StateFlow<Theme> = _currentTheme.asStateFlow()
    
    fun setTheme(theme: Theme) {
        _currentTheme.value = theme
    }
    
    fun getCurrentTheme(): Theme = _currentTheme.value
}
