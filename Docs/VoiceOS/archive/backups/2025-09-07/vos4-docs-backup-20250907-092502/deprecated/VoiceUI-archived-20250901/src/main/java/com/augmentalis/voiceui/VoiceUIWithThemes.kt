/**
 * VoiceUIWithThemes.kt - Main VoiceUI integration with Android theme system
 * 
 * Connects the Android theme system to the main VoiceUI module, providing
 * seamless theme integration for developers.
 */

package com.augmentalis.voiceui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceui.android.*
// import com.augmentalis.voiceui.universalui.AdaptiveVoiceUI // Temporarily disabled
import com.augmentalis.voiceui.theming.*
import com.augmentalis.voiceui.api.*
import kotlinx.coroutines.launch

/**
 * ENHANCED VOICEUI WITH THEME SUPPORT
 * 
 * This is the main entry point for VoiceUI apps on Android with full
 * theme support including Material You, manufacturer themes, and custom themes.
 */
@Composable
fun VoiceUIWithThemes(
    // Theme selection
    selectedTheme: AndroidTheme? = null,
    enableThemeSelector: Boolean = false,
    enableDynamicColors: Boolean = true,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    
    // VoiceUI settings
    name: String,
    enableNativeTheming: Boolean = true,
    enableSpatialAdaptation: Boolean = false,
    enableSeeThroughOptimization: Boolean = false,
    
    // Callbacks
    onThemeChanged: (AndroidTheme) -> Unit = {},
    onCreateCustomTheme: () -> Unit = {},
    
    // Content
    content: @Composable VoiceUIScope.() -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize systems
    val themeSystem = remember { AndroidThemeSystem(context) }
    val themePersistence = remember { ThemePersistence(context) }
    val dynamicColorExtractor = remember { DynamicColorExtractor(context) }
    val fontManager = remember { FontManager(context) }
    
    // Theme state
    var currentTheme by remember { mutableStateOf(selectedTheme) }
    var showThemeSelector by remember { mutableStateOf(enableThemeSelector) }
    val dynamicColorScheme by dynamicColorExtractor.colorScheme.collectAsState()
    
    // Load saved theme on first run
    LaunchedEffect(Unit) {
        if (currentTheme == null) {
            val savedThemeId = themePersistence.getCurrentTheme()
            if (savedThemeId != null) {
                // Find theme by ID
                val allThemes = themeSystem.getAllAvailableThemes()
                for (category in allThemes.values) {
                    val theme = category.find { it.id == savedThemeId }
                    if (theme != null) {
                        currentTheme = theme
                        break
                    }
                }
            }
            
            // Fallback to Material 3 with dynamic colors
            if (currentTheme == null) {
                currentTheme = createDefaultTheme(enableDynamicColors, dynamicColorScheme)
            }
        }
    }
    
    // Create integrated theme
    val integratedTheme = currentTheme?.let { theme ->
        if (enableDynamicColors && dynamicColorScheme?.isDynamic == true) {
            // Blend custom theme with dynamic colors
            blendWithDynamicColors(theme, dynamicColorScheme!!)
        } else {
            theme
        }
    }
    
    // Apply theme and render
    if (integratedTheme != null) {
        MaterialTheme(
            colorScheme = integratedTheme.theme.colors.toComposeColorScheme(),
            typography = integratedTheme.theme.typography.toComposeTypography()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main VoiceUI content - Simplified version
                // TODO: Re-enable AdaptiveVoiceUI once compilation issues are resolved
                Column(modifier = Modifier.fillMaxSize()) {
                    val scope = VoiceUIScopeImpl(
                        theme = integratedTheme.theme,
                        fontManager = fontManager,
                        onShowThemeSelector = { showThemeSelector = true }
                    )
                    scope.content()
                }
                
                // Theme selector overlay
                if (showThemeSelector) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ) {
                        AndroidThemeSelector(
                            onThemeSelected = { theme ->
                                currentTheme = theme
                                showThemeSelector = false
                                onThemeChanged(theme)
                                
                                // Save selection
                                coroutineScope.launch {
                                    themePersistence.saveCurrentTheme(theme)
                                }
                            },
                            onCreateCustomTheme = {
                                showThemeSelector = false
                                onCreateCustomTheme()
                            },
                            currentTheme = currentTheme,
                            showPreview = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple VoiceUI with automatic theme
 */
@Composable
fun SimpleVoiceUI(
    name: String,
    enableDynamicColors: Boolean = true,
    content: @Composable VoiceUIScope.() -> Unit
) {
    VoiceUIWithThemes(
        name = name,
        enableDynamicColors = enableDynamicColors,
        content = content
    )
}

/**
 * VoiceUI with theme selector button
 */
@Composable
fun VoiceUIWithThemeButton(
    name: String,
    buttonText: String = "Change Theme",
    content: @Composable VoiceUIScope.() -> Unit
) {
    var showSelector by remember { mutableStateOf(false) }
    
    VoiceUIWithThemes(
        name = name,
        enableThemeSelector = showSelector,
        content = {
            Column {
                // Theme button
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showSelector = true }) {
                        Text(buttonText)
                    }
                }
                
                // User content
                content()
            }
        }
    )
}

/**
 * VoiceUI Scope implementation
 */
private class VoiceUIScopeImpl(
    private val theme: CustomTheme,
    private val fontManager: FontManager,
    private val onShowThemeSelector: () -> Unit
) : VoiceUIScope {
    
    @Composable
    override fun button(
        text: String,
        voiceCommand: String?,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = theme.colors.primary,
                contentColor = theme.colors.onPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
    
    @Composable
    override fun text(
        text: String,
        style: TextStyle?
    ) {
        Text(
            text = text,
            color = theme.colors.onBackground,
            style = style ?: MaterialTheme.typography.bodyLarge
        )
    }
    
    @Composable
    override fun card(
        modifier: Modifier,
        content: @Composable () -> Unit
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = theme.colors.surface,
                contentColor = theme.colors.onSurface
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.padding(theme.spacing.md.dp)) {
                content()
            }
        }
    }
    
    @Composable
    override fun input(
        value: String,
        onValueChange: (String) -> Unit,
        label: String?,
        voiceInput: Boolean
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label?.let { { Text(it) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.colors.primary,
                focusedLabelColor = theme.colors.primary
            ),
            shape = RoundedCornerShape(4.dp)
        )
    }
    
    fun showThemeSelector() {
        onShowThemeSelector()
    }
}

/**
 * VoiceUI Scope interface
 */
interface VoiceUIScope {
    @Composable
    fun button(
        text: String,
        voiceCommand: String?,
        onClick: () -> Unit
    )
    
    @Composable
    fun text(
        text: String,
        style: androidx.compose.ui.text.TextStyle?
    )
    
    @Composable
    fun card(
        modifier: Modifier,
        content: @Composable () -> Unit
    )
    
    @Composable
    fun input(
        value: String,
        onValueChange: (String) -> Unit,
        label: String?,
        voiceInput: Boolean
    )
}

/**
 * Helper functions
 */
private fun createDefaultTheme(
    enableDynamicColors: Boolean,
    dynamicColorScheme: ExtractedColorScheme?
): AndroidTheme {
    return if (enableDynamicColors && dynamicColorScheme != null) {
        AndroidTheme(
            id = "material3_dynamic_auto",
            name = "Material You (Auto)",
            description = "Automatically generated from dynamic colors",
            theme = dynamicColorScheme.toCustomTheme("Material You Dynamic")
        )
    } else {
        AndroidTheme(
            id = "material3_default",
            name = "Material 3",
            description = "Default Material 3 theme",
            theme = CustomThemeBuilder()
                .name("Material 3")
                .colors {
                    primary(androidx.compose.ui.graphics.Color(0xFF6750A4))
                    secondary(androidx.compose.ui.graphics.Color(0xFF625B71))
                    background(androidx.compose.ui.graphics.Color(0xFFFFFBFE))
                    surface(androidx.compose.ui.graphics.Color(0xFFFFFBFE))
                }
                .build()
        )
    }
}

private fun blendWithDynamicColors(
    theme: AndroidTheme,
    dynamicColors: ExtractedColorScheme
): AndroidTheme {
    // Create new theme that blends custom theme with dynamic colors
    val blendedTheme = CustomThemeBuilder()
        .name("${theme.name} (Dynamic)")
        .colors {
            // Use dynamic colors for primary system colors
            primary(dynamicColors.primary)
            secondary(dynamicColors.secondary)
            background(dynamicColors.background)
            surface(dynamicColors.surface)
            
            // Keep original theme's custom colors
            val originalCustomColors = theme.theme.colors.getAllCustomColors()
            originalCustomColors.forEach { (name, color) ->
                custom(name, color)
            }
        }
        .typography {
            // Keep original typography
            h1(theme.theme.typography.h1.fontSize.value)
            h2(theme.theme.typography.h2.fontSize.value)
            h3(theme.theme.typography.h3.fontSize.value)
            body1(theme.theme.typography.body1.fontSize.value)
            body2(theme.theme.typography.body2.fontSize.value)
        }
        .build()
    
    return theme.copy(
        name = "${theme.name} (Dynamic)",
        theme = blendedTheme
    )
}

// Extension functions
private fun com.augmentalis.voiceui.theming.CustomColorScheme.toComposeColorScheme(): ColorScheme {
    return lightColorScheme(
        primary = this.primary,
        secondary = this.secondary,
        background = this.background,
        surface = this.surface,
        error = this.error,
        onPrimary = this.onPrimary,
        onSecondary = this.onSecondary,
        onBackground = this.onBackground,
        onSurface = this.onSurface,
        onError = this.onError
    )
}

private fun com.augmentalis.voiceui.theming.CustomTypographyTheme.toComposeTypography(): Typography {
    return Typography(
        displayLarge = androidx.compose.ui.text.TextStyle(
            fontSize = this.h1.fontSize,
            fontWeight = this.h1.fontWeight
        ),
        displayMedium = androidx.compose.ui.text.TextStyle(
            fontSize = this.h2.fontSize,
            fontWeight = this.h2.fontWeight
        ),
        headlineLarge = androidx.compose.ui.text.TextStyle(
            fontSize = this.h3.fontSize,
            fontWeight = this.h3.fontWeight
        ),
        bodyLarge = androidx.compose.ui.text.TextStyle(
            fontSize = this.body1.fontSize,
            fontWeight = this.body1.fontWeight
        ),
        bodyMedium = androidx.compose.ui.text.TextStyle(
            fontSize = this.body2.fontSize,
            fontWeight = this.body2.fontWeight
        )
    )
}