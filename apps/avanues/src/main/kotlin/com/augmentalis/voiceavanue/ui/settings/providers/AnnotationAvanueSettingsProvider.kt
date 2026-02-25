/**
 * AnnotationAvanueSettingsProvider.kt - DrawAvanue annotation tool settings
 *
 * Provides default drawing tool, color, stroke width, and bezier smoothing
 * tension configuration. State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.augmentalis.avanueui.components.settings.SettingsDropdownRow
import com.augmentalis.avanueui.components.settings.SettingsGroupCard
import com.augmentalis.avanueui.components.settings.SettingsSliderRow
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.foundation.settings.models.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

class AnnotationAvanueSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    companion object {
        val TOOL_OPTIONS = listOf("Pen", "Highlighter", "Eraser")

        val COLOR_OPTIONS = listOf(
            "#FFFFFF" to "White",
            "#FF0000" to "Red",
            "#00FF00" to "Green",
            "#0000FF" to "Blue",
            "#FFFF00" to "Yellow"
        )

        fun colorLabel(hex: String): String =
            COLOR_OPTIONS.find { it.first == hex }?.second ?: hex
    }

    override val moduleId = "annotationavanue"
    override val displayName = "DrawAvanue"
    override val iconName = "Draw"
    override val sortOrder = 750

    override val sections = listOf(
        SettingsSection(id = "drawing", title = "Drawing")
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "annotation_default_tool",
            displayName = "Default Tool",
            sectionId = "drawing",
            keywords = listOf("tool", "pen", "highlighter", "eraser", "brush", "draw")
        ),
        SearchableSettingEntry(
            key = "annotation_default_color",
            displayName = "Default Color",
            sectionId = "drawing",
            keywords = listOf("color", "colour", "white", "red", "green", "blue", "yellow")
        ),
        SearchableSettingEntry(
            key = "annotation_stroke_width",
            displayName = "Stroke Width",
            sectionId = "drawing",
            keywords = listOf("stroke", "width", "thickness", "line", "size")
        ),
        SearchableSettingEntry(
            key = "annotation_tension",
            displayName = "Smoothing Tension",
            sectionId = "drawing",
            keywords = listOf("smoothing", "tension", "bezier", "curve", "smooth")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Default Tool",
                subtitle = "Drawing tool selected on launch",
                icon = Icons.Default.Brush,
                selected = settings.annotationDefaultTool,
                options = TOOL_OPTIONS,
                optionLabel = { it },
                onSelected = { scope.launch { repository.updateAnnotationDefaultTool(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: select Default Tool" }
            )

            SettingsDropdownRow(
                title = "Default Color",
                subtitle = "Stroke color used when opening the canvas",
                icon = Icons.Default.ColorLens,
                selected = settings.annotationDefaultColor,
                options = COLOR_OPTIONS.map { it.first },
                optionLabel = { hex -> colorLabel(hex) },
                onSelected = { scope.launch { repository.updateAnnotationDefaultColor(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: select Default Color" }
            )

            SettingsSliderRow(
                title = "Stroke Width",
                subtitle = "Line thickness in density-independent pixels",
                icon = Icons.Default.LineWeight,
                value = settings.annotationStrokeWidth.toFloat(),
                valueRange = 1f..20f,
                steps = 18,
                valueLabel = "${settings.annotationStrokeWidth}dp",
                onValueChange = { scope.launch { repository.updateAnnotationStrokeWidth(it.toInt()) } },
                modifier = Modifier.semantics { contentDescription = "Voice: adjust Stroke Width" }
            )

            SettingsSliderRow(
                title = "Smoothing Tension",
                subtitle = "Catmull-Rom bezier curve tension (0 = sharp, 1 = smooth)",
                icon = Icons.Default.Tune,
                value = settings.annotationTension,
                valueRange = 0f..1f,
                steps = 9,
                valueLabel = String.format("%.1f", settings.annotationTension),
                onValueChange = { scope.launch { repository.updateAnnotationTension(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: adjust Smoothing Tension" }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.Draw
}
