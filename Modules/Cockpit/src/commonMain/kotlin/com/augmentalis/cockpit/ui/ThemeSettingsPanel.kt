/**
 * ThemeSettingsPanel.kt - Inline overlay panel for theme customization
 *
 * Appears as an overlay within the Cockpit Dashboard when the user taps
 * the settings gear icon. Provides three sections:
 * 1. Preset picker (horizontal scrollable cards)
 * 2. Manual overrides (palette, material, appearance dropdowns)
 * 3. Background scene selector
 *
 * All styling uses AvanueTheme.colors (MANDATORY RULE #3).
 * All interactive elements have AVID voice identifiers.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.components.settings.SettingsDropdownRow
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsSectionHeader
import com.augmentalis.avanueui.theme.AppearanceMode
import com.augmentalis.avanueui.theme.AvanueColorPalette
import com.augmentalis.avanueui.theme.AvanueModuleAccents
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode
import com.augmentalis.avanueui.theme.ModuleAccent
import com.augmentalis.avanueui.theme.ThemePreset
import com.augmentalis.avanueui.theme.ThemePresetRegistry
import com.augmentalis.cockpit.model.DashboardModule
import com.augmentalis.cockpit.model.DashboardModuleRegistry

/**
 * Inline overlay panel for theme customization within the Cockpit Dashboard.
 *
 * Renders a semi-transparent scrim behind a bottom-aligned panel containing:
 * - Section 1: Preset picker (horizontal scrollable row of preset cards)
 * - Section 2: Manual overrides (palette, material mode, appearance dropdowns)
 * - Section 3: Background scene selector
 *
 * @param currentPalette Active color palette
 * @param currentMaterial Active material mode
 * @param currentAppearance Active appearance mode
 * @param currentPresetId ID of the active preset, or null if using manual overrides
 * @param currentBackgroundScene Active background scene
 * @param onPaletteChanged Called when palette selection changes (clears active preset)
 * @param onMaterialChanged Called when material mode changes (clears active preset)
 * @param onAppearanceChanged Called when appearance changes (clears active preset)
 * @param onPresetApplied Called when a preset card is tapped
 * @param onBackgroundSceneChanged Called when background scene changes
 * @param onDismiss Called when the panel should close (scrim tap or close button)
 * @param modifier Modifier applied to the outermost container
 */
@Composable
fun ThemeSettingsPanel(
    currentPalette: AvanueColorPalette,
    currentMaterial: MaterialMode,
    currentAppearance: AppearanceMode,
    currentPresetId: String?,
    currentBackgroundScene: BackgroundScene,
    onPaletteChanged: (AvanueColorPalette) -> Unit,
    onMaterialChanged: (MaterialMode) -> Unit,
    onAppearanceChanged: (AppearanceMode) -> Unit,
    onPresetApplied: (ThemePreset) -> Unit,
    onBackgroundSceneChanged: (BackgroundScene) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim layer — semi-transparent backdrop, tap to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .semantics { contentDescription = "Voice: click Dismiss" }
        )

        // Panel — bottom-aligned with rounded top corners
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(colors.surface)
                .padding(bottom = 16.dp)
        ) {
            // Header row: title + close button
            PanelHeader(onDismiss = onDismiss)

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp)
            ) {
                // Section 1: Preset Picker
                PresetPickerSection(
                    currentPresetId = currentPresetId,
                    onPresetApplied = onPresetApplied
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Section 2: Manual Overrides
                ManualOverridesSection(
                    currentPalette = currentPalette,
                    currentMaterial = currentMaterial,
                    currentAppearance = currentAppearance,
                    onPaletteChanged = onPaletteChanged,
                    onMaterialChanged = onMaterialChanged,
                    onAppearanceChanged = onAppearanceChanged
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Section 3: Background Scene
                BackgroundSceneSection(
                    currentScene = currentBackgroundScene,
                    onSceneChanged = onBackgroundSceneChanged
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Section 4: Module Accent Colors
                ModuleAccentsSection()
            }
        }
    }
}

// ── Panel Header ─────────────────────────────────────────────────────────────

@Composable
private fun PanelHeader(onDismiss: () -> Unit) {
    val colors = AvanueTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Theme Settings",
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.semantics {
                contentDescription = "Voice: click Close Theme Settings"
            }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = colors.textPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Section 1: Preset Picker ─────────────────────────────────────────────────

@Composable
private fun PresetPickerSection(
    currentPresetId: String?,
    onPresetApplied: (ThemePreset) -> Unit
) {
    SettingsSectionHeader(title = "PRESETS")

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(ThemePresetRegistry.ALL, key = { it.id }) { preset ->
            PresetCard(
                preset = preset,
                isActive = preset.id == currentPresetId,
                onClick = { onPresetApplied(preset) }
            )
        }
    }
}

/**
 * Individual preset card shown in the horizontal picker row.
 *
 * Displays the preset's [displayName], [materialMode] label, and [description].
 * Active preset is indicated by a primary-colored border.
 */
@Composable
private fun PresetCard(
    preset: ThemePreset,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    val borderModifier = if (isActive) {
        Modifier.border(
            width = 2.dp,
            color = colors.primary,
            shape = RoundedCornerShape(12.dp)
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = colors.border.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
        )
    }

    AvanueCard(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .then(borderModifier)
            .semantics {
                contentDescription = "Voice: click ${preset.displayName} Preset"
            },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isActive) colors.primary.copy(alpha = 0.1f)
                    else colors.surface.copy(alpha = 0.7f)
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: preset name + material icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = materialModeIcon(preset.materialMode),
                    contentDescription = null,
                    tint = if (isActive) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = preset.displayName,
                    color = if (isActive) colors.primary else colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Middle: material mode label
            Text(
                text = preset.materialMode.displayName,
                color = colors.textSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )

            // Bottom: description
            Text(
                text = preset.description,
                color = colors.textSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}

/**
 * Returns an icon vector corresponding to the given [MaterialMode].
 * Uses AutoAwesome for Glass (sparkle = frosted), Tune for others.
 */
private fun materialModeIcon(mode: MaterialMode) = when (mode) {
    MaterialMode.Glass -> Icons.Default.AutoAwesome
    MaterialMode.Water -> Icons.Default.AutoAwesome
    MaterialMode.Cupertino -> Icons.Default.Tune
    MaterialMode.MountainView -> Icons.Default.Tune
}

// ── Section 2: Manual Overrides ──────────────────────────────────────────────

@Composable
private fun ManualOverridesSection(
    currentPalette: AvanueColorPalette,
    currentMaterial: MaterialMode,
    currentAppearance: AppearanceMode,
    onPaletteChanged: (AvanueColorPalette) -> Unit,
    onMaterialChanged: (MaterialMode) -> Unit,
    onAppearanceChanged: (AppearanceMode) -> Unit
) {
    SettingsSectionHeader(title = "MANUAL OVERRIDES")

    SettingsGroupCard {
        // Palette dropdown with colored indicator
        SettingsDropdownRow(
            title = "Color Palette",
            subtitle = "Primary color family",
            icon = Icons.Default.Palette,
            selected = currentPalette,
            options = AvanueColorPalette.entries.toList(),
            optionLabel = { it.displayName },
            onSelected = onPaletteChanged,
            modifier = Modifier.semantics {
                contentDescription = "Voice: click Color Palette"
            }
        )

        // Material Mode dropdown
        SettingsDropdownRow(
            title = "Material Style",
            subtitle = "Surface rendering mode",
            icon = Icons.Default.AutoAwesome,
            selected = currentMaterial,
            options = MaterialMode.entries.toList(),
            optionLabel = { it.displayName },
            onSelected = onMaterialChanged,
            modifier = Modifier.semantics {
                contentDescription = "Voice: click Material Style"
            }
        )

        // Appearance dropdown
        SettingsDropdownRow(
            title = "Appearance",
            subtitle = "Light, dark, or automatic",
            icon = Icons.Default.Tune,
            selected = currentAppearance,
            options = AppearanceMode.entries.toList(),
            optionLabel = { it.displayName },
            onSelected = onAppearanceChanged,
            modifier = Modifier.semantics {
                contentDescription = "Voice: click Appearance"
            }
        )
    }
}

// ── Section 3: Background Scene ──────────────────────────────────────────────

@Composable
private fun BackgroundSceneSection(
    currentScene: BackgroundScene,
    onSceneChanged: (BackgroundScene) -> Unit
) {
    SettingsSectionHeader(title = "BACKGROUND")

    SettingsGroupCard {
        SettingsDropdownRow(
            title = "Background Scene",
            subtitle = "Backdrop animation behind content",
            icon = Icons.Default.Wallpaper,
            selected = currentScene,
            options = BackgroundScene.entries.toList(),
            optionLabel = { backgroundSceneLabel(it) },
            onSelected = onSceneChanged,
            modifier = Modifier.semantics {
                contentDescription = "Voice: click Background Scene"
            }
        )
    }
}

/**
 * Human-readable label for each [BackgroundScene] value.
 */
private fun backgroundSceneLabel(scene: BackgroundScene): String = when (scene) {
    BackgroundScene.GRADIENT -> "Gradient"
    BackgroundScene.STARFIELD -> "Starfield"
    BackgroundScene.SCANLINE_GRID -> "Scanline Grid"
    BackgroundScene.TRANSPARENT -> "Transparent"
}

// ── Section 4: Module Accent Colors ─────────────────────────────────────────

/**
 * Predefined accent color palette for module customization.
 * Material Design 500-level colors covering the full hue range.
 */
private val ACCENT_PALETTE: List<Color> = listOf(
    Color(0xFFE53935), // Red
    Color(0xFFEC407A), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF7E57C2), // Deep Purple
    Color(0xFF3F51B5), // Indigo
    Color(0xFF2196F3), // Blue
    Color(0xFF00BCD4), // Cyan
    Color(0xFF009688), // Teal
    Color(0xFF4CAF50), // Green
    Color(0xFF8BC34A), // Light Green
    Color(0xFFFFC107), // Amber
    Color(0xFFFF9800), // Orange
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF795548), // Brown
)

/**
 * Module accent color customization section.
 *
 * Allows per-module accent color overrides. Shows a horizontal scrollable
 * row of module chips — selecting one reveals a color palette grid below.
 * Choosing a color calls [AvanueModuleAccents.setOverride]; "Reset" clears
 * the override to revert to the theme's global accent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleAccentsSection() {
    val colors = AvanueTheme.colors
    var selectedModuleId by remember { mutableStateOf<String?>(null) }

    SettingsSectionHeader(title = "MODULE ACCENTS")

    SettingsGroupCard {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Module chip row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    DashboardModuleRegistry.allModules,
                    key = { it.id }
                ) { module ->
                    ModuleAccentChip(
                        module = module,
                        isSelected = module.id == selectedModuleId,
                        onClick = {
                            selectedModuleId = if (selectedModuleId == module.id) null else module.id
                        }
                    )
                }
            }

            // Expanded color palette for selected module
            if (selectedModuleId != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = DashboardModuleRegistry.findById(selectedModuleId!!)?.displayName ?: "",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color dots in a wrapping flow layout
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentAccent = AvanueModuleAccents.get(selectedModuleId!!)

                    ACCENT_PALETTE.forEach { paletteColor ->
                        AccentColorDot(
                            color = paletteColor,
                            isSelected = currentAccent.isCustom && currentAccent.accent == paletteColor,
                            onClick = {
                                AvanueModuleAccents.setOverride(
                                    selectedModuleId!!,
                                    ModuleAccent(
                                        accent = paletteColor,
                                        onAccent = Color.White,
                                        accentMuted = paletteColor.copy(alpha = 0.6f),
                                        isCustom = true
                                    )
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reset button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            AvanueModuleAccents.clearOverride(selectedModuleId!!)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .semantics {
                            contentDescription = "Voice: click Reset Accent"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Reset to theme default",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Compact chip showing a module's name and current accent color swatch.
 * Tapping selects/deselects the module for accent editing.
 */
@Composable
private fun ModuleAccentChip(
    module: DashboardModule,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    val accent = AvanueModuleAccents.get(module.id)
    val chipShape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(chipShape)
            .background(
                if (isSelected) accent.accent.copy(alpha = 0.15f) else colors.surface,
                chipShape
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) accent.accent else colors.border.copy(alpha = 0.3f),
                shape = chipShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
            .semantics {
                contentDescription = "Voice: click ${module.displayName} Accent"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Color swatch dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accent.accent)
        )
        Text(
            text = module.displayName,
            color = if (isSelected) accent.accent else colors.textPrimary.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Individual color dot in the accent palette grid.
 * Selected state shown with a primary-colored ring.
 */
@Composable
private fun AccentColorDot(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val ringColor = AvanueTheme.colors.textPrimary

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelected) Modifier.border(2.dp, ringColor, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .semantics {
                contentDescription = "Voice: click Accent Color"
            }
    )
}
