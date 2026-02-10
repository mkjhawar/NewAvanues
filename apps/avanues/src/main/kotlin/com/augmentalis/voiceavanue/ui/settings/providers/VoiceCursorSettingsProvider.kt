/**
 * VoiceCursorSettingsProvider.kt - Cursor behavior and appearance settings
 *
 * Two sections:
 * - Behavior: dwell click, delay, smoothing
 * - Appearance: cursor size, speed, coordinates, accent color
 *
 * State persisted via AvanuesSettingsRepository (DataStore).
 * Color overrides integrate with AvanueModuleAccents.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.augmentalis.avanueui.components.settings.SettingsColorRow
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsSliderRow
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.avanueui.theme.AvanueModuleAccents
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.ModuleAccent
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.voiceavanue.data.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

class VoiceCursorSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    override val moduleId = "voicecursor"
    override val displayName = "VoiceCursor"
    override val iconName = "TouchApp"
    override val sortOrder = 200

    override val sections = listOf(
        SettingsSection(id = "cursor", title = "Behavior"),
        SettingsSection(id = "appearance", title = "Appearance", sortOrder = 1)
    )

    override val searchableEntries = listOf(
        // Behavior section
        SearchableSettingEntry(
            key = "cursor_enabled",
            displayName = "Cursor Overlay",
            sectionId = "cursor",
            keywords = listOf("cursor", "overlay", "enable", "disable", "on", "off", "show", "hide")
        ),
        SearchableSettingEntry(
            key = "dwell_click_enabled",
            displayName = "Dwell Click",
            sectionId = "cursor",
            keywords = listOf("auto-click", "dwell", "tap", "cursor click")
        ),
        SearchableSettingEntry(
            key = "dwell_click_delay",
            displayName = "Dwell Click Delay",
            sectionId = "cursor",
            keywords = listOf("delay", "timing", "milliseconds", "dwell time")
        ),
        SearchableSettingEntry(
            key = "cursor_smoothing",
            displayName = "Cursor Smoothing",
            sectionId = "cursor",
            keywords = listOf("smoothing", "jitter", "stabilize", "cursor")
        ),
        // Appearance section
        SearchableSettingEntry(
            key = "cursor_size",
            displayName = "Cursor Size",
            sectionId = "appearance",
            keywords = listOf("size", "big", "small", "scale", "radius")
        ),
        SearchableSettingEntry(
            key = "cursor_speed",
            displayName = "Cursor Speed",
            sectionId = "appearance",
            keywords = listOf("speed", "sensitivity", "fast", "slow", "movement")
        ),
        SearchableSettingEntry(
            key = "show_coordinates",
            displayName = "Show Coordinates",
            sectionId = "appearance",
            keywords = listOf("coordinates", "position", "x", "y", "debug")
        ),
        SearchableSettingEntry(
            key = "cursor_color",
            displayName = "Cursor Color",
            sectionId = "appearance",
            keywords = listOf("color", "accent", "theme", "customise", "appearance")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        when (sectionId) {
            "cursor" -> BehaviorSection()
            "appearance" -> AppearanceSection()
        }
    }

    @Composable
    private fun BehaviorSection() {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsSwitchRow(
                title = "Cursor Overlay",
                subtitle = "Show gaze/head cursor on screen",
                icon = Icons.Default.TouchApp,
                checked = settings.cursorEnabled,
                onCheckedChange = { scope.launch { repository.updateCursorEnabled(it) } }
            )

            SettingsSwitchRow(
                title = "Dwell Click",
                subtitle = "Auto-click when cursor stays still",
                icon = Icons.Default.TouchApp,
                checked = settings.dwellClickEnabled,
                onCheckedChange = { scope.launch { repository.updateDwellClickEnabled(it) } }
            )

            SettingsSliderRow(
                title = "Dwell Click Delay",
                subtitle = "${settings.dwellClickDelayMs.toInt()} ms",
                icon = Icons.Default.Timer,
                value = settings.dwellClickDelayMs,
                valueRange = 500f..3000f,
                onValueChange = { scope.launch { repository.updateDwellClickDelay(it) } }
            )

            SettingsSwitchRow(
                title = "Cursor Smoothing",
                subtitle = "Reduce cursor jitter",
                icon = Icons.Default.Tune,
                checked = settings.cursorSmoothing,
                onCheckedChange = { scope.launch { repository.updateCursorSmoothing(it) } }
            )
        }
    }

    @Composable
    private fun AppearanceSection() {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()
        val themeAccent = AvanueTheme.moduleAccent("voicecursor")

        // Resolve display color: custom override or theme accent
        val useThemeColor = settings.cursorAccentOverride == null
        val currentColor = if (useThemeColor) {
            themeAccent.accent
        } else {
            Color(settings.cursorAccentOverride!!.toInt())
        }

        SettingsGroupCard {
            SettingsSliderRow(
                title = "Cursor Size",
                icon = Icons.Default.ZoomOutMap,
                value = settings.cursorSize.toFloat(),
                valueRange = 8f..64f,
                valueLabel = "${settings.cursorSize}",
                onValueChange = { scope.launch { repository.updateCursorSize(it.toInt()) } }
            )

            SettingsSliderRow(
                title = "Cursor Speed",
                icon = Icons.Default.Speed,
                value = settings.cursorSpeed.toFloat(),
                valueRange = 1f..15f,
                steps = 13,
                valueLabel = "${settings.cursorSpeed}",
                onValueChange = { scope.launch { repository.updateCursorSpeed(it.toInt()) } }
            )

            SettingsSwitchRow(
                title = "Show Coordinates",
                subtitle = "Display cursor position on screen",
                icon = Icons.Default.Visibility,
                checked = settings.showCoordinates,
                onCheckedChange = { scope.launch { repository.updateShowCoordinates(it) } }
            )

            SettingsColorRow(
                title = "Cursor Color",
                subtitle = "Accent color for cursor and dwell ring",
                icon = Icons.Default.Palette,
                color = currentColor,
                useThemeColor = useThemeColor,
                onColorSelected = { color ->
                    scope.launch {
                        val argb = color.toArgbLong()
                        repository.updateCursorAccentOverride(argb)
                        AvanueModuleAccents.setOverride(
                            "voicecursor",
                            ModuleAccent(
                                accent = color,
                                onAccent = Color.White,
                                accentMuted = color.copy(alpha = 0.6f),
                                isCustom = true
                            )
                        )
                    }
                },
                onUseTheme = {
                    scope.launch {
                        repository.updateCursorAccentOverride(null)
                        AvanueModuleAccents.clearOverride("voicecursor")
                    }
                }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.TouchApp
}

/** Convert Compose Color to ARGB Long for DataStore persistence */
private fun Color.toArgbLong(): Long {
    val a = (alpha * 255 + 0.5f).toInt() and 0xFF
    val r = (red * 255 + 0.5f).toInt() and 0xFF
    val g = (green * 255 + 0.5f).toInt() and 0xFF
    val b = (blue * 255 + 0.5f).toInt() and 0xFF
    return ((a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
}
