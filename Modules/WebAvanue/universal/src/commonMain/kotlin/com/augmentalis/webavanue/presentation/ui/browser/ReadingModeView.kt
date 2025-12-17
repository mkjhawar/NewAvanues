package com.augmentalis.webavanue.ui.screen.browser

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.webavanue.util.ReadingModeArticle
import com.augmentalis.webavanue.domain.model.BrowserSettings

/**
 * ReadingModeView - Clean article reading interface
 *
 * Features:
 * - Distraction-free reading UI
 * - Customizable typography (font size, line height, font family)
 * - Multiple themes (light, dark, sepia)
 * - Article metadata display (title, author, date, site)
 * - Scrollable content with optimized readability
 * - Quick settings toolbar
 *
 * Design Principles:
 * - Maximum readability (optimal line length, spacing)
 * - Minimal distractions (clean UI, no clutter)
 * - Accessibility (high contrast, resizable text)
 * - Performance (efficient recomposition, lazy loading)
 *
 * @param article Extracted article content
 * @param theme Reading mode theme (light, dark, sepia)
 * @param fontSize Font size multiplier (0.75 - 2.0)
 * @param fontFamily Font family to use
 * @param lineHeight Line height multiplier (1.0 - 2.0)
 * @param onClose Callback to exit reading mode
 * @param onThemeChange Callback when theme changes
 * @param onFontSizeChange Callback when font size changes
 * @param onFontFamilyChange Callback when font family changes
 * @param onLineHeightChange Callback when line height changes
 * @param modifier Modifier for customization
 */
@Composable
fun ReadingModeView(
    article: ReadingModeArticle,
    theme: ReadingModeTheme = ReadingModeTheme.LIGHT,
    fontSize: Float = 1.0f,
    fontFamily: ReadingModeFontFamily = ReadingModeFontFamily.SYSTEM,
    lineHeight: Float = 1.5f,
    onClose: () -> Unit,
    onThemeChange: (ReadingModeTheme) -> Unit = {},
    onFontSizeChange: (Float) -> Unit = {},
    onFontFamilyChange: (ReadingModeFontFamily) -> Unit = {},
    onLineHeightChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Settings panel state
    var showSettings by remember { mutableStateOf(false) }

    // Theme colors
    val colors = remember(theme) { getThemeColors(theme) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Main content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Reading Mode",
                        tint = colors.iconTint
                    )
                }

                // Quick controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Font size decrease
                    IconButton(
                        onClick = {
                            val newSize = (fontSize - 0.1f).coerceIn(0.75f, 2.0f)
                            onFontSizeChange(newSize)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextDecrease,
                            contentDescription = "Decrease Font Size",
                            tint = colors.iconTint
                        )
                    }

                    // Font size increase
                    IconButton(
                        onClick = {
                            val newSize = (fontSize + 0.1f).coerceIn(0.75f, 2.0f)
                            onFontSizeChange(newSize)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextIncrease,
                            contentDescription = "Increase Font Size",
                            tint = colors.iconTint
                        )
                    }

                    // Theme toggle (light/dark/sepia)
                    IconButton(
                        onClick = {
                            val nextTheme = when (theme) {
                                ReadingModeTheme.LIGHT -> ReadingModeTheme.DARK
                                ReadingModeTheme.DARK -> ReadingModeTheme.SEPIA
                                ReadingModeTheme.SEPIA -> ReadingModeTheme.LIGHT
                            }
                            onThemeChange(nextTheme)
                        }
                    ) {
                        Icon(
                            imageVector = when (theme) {
                                ReadingModeTheme.LIGHT -> Icons.Default.DarkMode
                                ReadingModeTheme.DARK -> Icons.Default.LightMode
                                ReadingModeTheme.SEPIA -> Icons.Default.DarkMode
                            },
                            contentDescription = "Change Theme",
                            tint = colors.iconTint
                        )
                    }

                    // Settings button
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Reading Mode Settings",
                            tint = colors.iconTint
                        )
                    }
                }
            }

            // Article metadata
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Title
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = (32 * fontSize).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (40 * fontSize * lineHeight).sp,
                        fontFamily = fontFamily.toComposeFontFamily()
                    ),
                    color = colors.primaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Metadata row (author, date, site)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (article.author.isNotBlank()) {
                        Text(
                            text = article.author,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (14 * fontSize).sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = fontFamily.toComposeFontFamily()
                            ),
                            color = colors.secondaryText
                        )

                        if (article.publishDate.isNotBlank() || article.siteName.isNotBlank()) {
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    if (article.publishDate.isNotBlank()) {
                        Text(
                            text = article.publishDate,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (14 * fontSize).sp,
                                fontFamily = fontFamily.toComposeFontFamily()
                            ),
                            color = colors.secondaryText
                        )

                        if (article.siteName.isNotBlank()) {
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.secondaryText,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    if (article.siteName.isNotBlank()) {
                        Text(
                            text = article.siteName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (14 * fontSize).sp,
                                fontFamily = fontFamily.toComposeFontFamily()
                            ),
                            color = colors.secondaryText
                        )
                    }
                }

                // Word count and reading time estimate
                if (article.wordCount > 0) {
                    val readingTimeMinutes = (article.wordCount / 250).coerceAtLeast(1) // Avg 250 words/min
                    Text(
                        text = "${article.wordCount} words • ${readingTimeMinutes} min read",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = (12 * fontSize).sp,
                            fontFamily = fontFamily.toComposeFontFamily()
                        ),
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Featured image (if available)
            if (article.featuredImage.isNotBlank()) {
                // Note: Image loading would require platform-specific implementation
                // For now, we'll show a placeholder with the URL
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(colors.imagePlaceholder)
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Featured Image\n${article.featuredImage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.tertiaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Article content
            // Note: For HTML rendering, we would need a platform-specific WebView or HTML parser
            // For now, display the plain text content with basic formatting
            Text(
                text = article.textContent,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (18 * fontSize).sp,
                    lineHeight = (28 * fontSize * lineHeight).sp,
                    fontFamily = fontFamily.toComposeFontFamily()
                ),
                color = colors.primaryText,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }

        // Settings panel overlay
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReadingModeSettingsPanel(
                theme = theme,
                fontSize = fontSize,
                fontFamily = fontFamily,
                lineHeight = lineHeight,
                onThemeChange = onThemeChange,
                onFontSizeChange = onFontSizeChange,
                onFontFamilyChange = onFontFamilyChange,
                onLineHeightChange = onLineHeightChange,
                onDismiss = { showSettings = false },
                colors = colors
            )
        }
    }
}

/**
 * ReadingModeSettingsPanel - Settings panel for customizing reading mode
 *
 * @param theme Current theme
 * @param fontSize Current font size
 * @param fontFamily Current font family
 * @param lineHeight Current line height
 * @param onThemeChange Callback when theme changes
 * @param onFontSizeChange Callback when font size changes
 * @param onFontFamilyChange Callback when font family changes
 * @param onLineHeightChange Callback when line height changes
 * @param onDismiss Callback to dismiss panel
 * @param colors Theme colors
 */
@Composable
private fun ReadingModeSettingsPanel(
    theme: ReadingModeTheme,
    fontSize: Float,
    fontFamily: ReadingModeFontFamily,
    lineHeight: Float,
    onThemeChange: (ReadingModeTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (ReadingModeFontFamily) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    colors: ReadingModeColors
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reading Mode Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primaryText
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = colors.iconTint
                    )
                }
            }

            Divider(color = colors.divider)

            // Theme selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadingModeTheme.values().forEach { themeOption ->
                        FilterChip(
                            selected = theme == themeOption,
                            onClick = { onThemeChange(themeOption) },
                            label = { Text(themeOption.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Font size slider
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primaryText
                    )
                    Text(
                        text = "${(fontSize * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )
                }
                Slider(
                    value = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 0.75f..2.0f,
                    steps = 24
                )
            }

            // Line height slider
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Line Height",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primaryText
                    )
                    Text(
                        text = "${String.format("%.1f", lineHeight)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText
                    )
                }
                Slider(
                    value = lineHeight,
                    onValueChange = onLineHeightChange,
                    valueRange = 1.0f..2.0f,
                    steps = 9
                )
            }

            // Font family selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Font Family",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryText
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadingModeFontFamily.values().forEach { family ->
                        FilterChip(
                            selected = fontFamily == family,
                            onClick = { onFontFamilyChange(family) },
                            label = { Text(family.displayName) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reading mode theme options
 */
enum class ReadingModeTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SEPIA("Sepia")
}

/**
 * Reading mode font family options
 */
enum class ReadingModeFontFamily(val displayName: String) {
    SYSTEM("System"),
    SERIF("Serif"),
    SANS_SERIF("Sans Serif"),
    MONOSPACE("Monospace")
}

/**
 * Convert ReadingModeFontFamily to Compose FontFamily
 */
private fun ReadingModeFontFamily.toComposeFontFamily(): FontFamily {
    return when (this) {
        ReadingModeFontFamily.SYSTEM -> FontFamily.Default
        ReadingModeFontFamily.SERIF -> FontFamily.Serif
        ReadingModeFontFamily.SANS_SERIF -> FontFamily.SansSerif
        ReadingModeFontFamily.MONOSPACE -> FontFamily.Monospace
    }
}

/**
 * Theme colors for reading mode
 */
data class ReadingModeColors(
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val iconTint: Color,
    val surfaceVariant: Color,
    val divider: Color,
    val imagePlaceholder: Color
)

/**
 * Get colors for reading mode theme
 */
private fun getThemeColors(theme: ReadingModeTheme): ReadingModeColors {
    return when (theme) {
        ReadingModeTheme.LIGHT -> ReadingModeColors(
            background = Color.White,
            primaryText = Color(0xFF1A1A1A),
            secondaryText = Color(0xFF666666),
            tertiaryText = Color(0xFF999999),
            iconTint = Color(0xFF333333),
            surfaceVariant = Color(0xFFF5F5F5),
            divider = Color(0xFFE0E0E0),
            imagePlaceholder = Color(0xFFEEEEEE)
        )
        ReadingModeTheme.DARK -> ReadingModeColors(
            background = Color(0xFF1A1A1A),
            primaryText = Color(0xFFE0E0E0),
            secondaryText = Color(0xFFAAAAAA),
            tertiaryText = Color(0xFF777777),
            iconTint = Color(0xFFCCCCCC),
            surfaceVariant = Color(0xFF2A2A2A),
            divider = Color(0xFF3A3A3A),
            imagePlaceholder = Color(0xFF333333)
        )
        ReadingModeTheme.SEPIA -> ReadingModeColors(
            background = Color(0xFFF4ECD8),
            primaryText = Color(0xFF3D3226),
            secondaryText = Color(0xFF6B5D4F),
            tertiaryText = Color(0xFF9B8B7A),
            iconTint = Color(0xFF5D4E3F),
            surfaceVariant = Color(0xFFE8DCC5),
            divider = Color(0xFFD4C7B1),
            imagePlaceholder = Color(0xFFE0D4BE)
        )
    }
}
