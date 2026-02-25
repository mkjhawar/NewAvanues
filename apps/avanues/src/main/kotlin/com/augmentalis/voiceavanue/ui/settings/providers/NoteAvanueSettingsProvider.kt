/**
 * NoteAvanueSettingsProvider.kt - Note editor and format settings
 *
 * Two sections:
 * - Editor: font size, auto-save interval, spell check
 * - Format: default document format (Markdown / Rich Text)
 *
 * State persisted via AvanuesSettingsRepository (DataStore).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings.providers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Spellcheck
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
import com.augmentalis.avanueui.components.settings.SettingsSwitchRow
import com.augmentalis.foundation.settings.SearchableSettingEntry
import com.augmentalis.foundation.settings.SettingsSection
import com.augmentalis.foundation.settings.models.AvanuesSettings
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.ui.settings.ComposableSettingsProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

class NoteAvanueSettingsProvider @Inject constructor(
    private val repository: AvanuesSettingsRepository
) : ComposableSettingsProvider {

    companion object {
        /** Font size presets. */
        val FONT_SIZE_OPTIONS = listOf(
            "Small" to "Small",
            "Medium" to "Medium",
            "Large" to "Large"
        )

        /** Auto-save interval options with display labels. */
        val AUTOSAVE_OPTIONS = listOf(
            "Off" to "Off",
            "5s" to "5s",
            "15s" to "15s",
            "30s" to "30s"
        )

        /** Document format options with display labels. */
        val FORMAT_OPTIONS = listOf(
            "Markdown" to "Markdown",
            "RichText" to "Rich Text"
        )
    }

    override val moduleId = "noteavanue"
    override val displayName = "NoteAvanue"
    override val iconName = "EditNote"
    override val sortOrder = 600

    override val sections = listOf(
        SettingsSection(id = "editor", title = "Editor", sortOrder = 0),
        SettingsSection(id = "format", title = "Format", sortOrder = 1)
    )

    override val searchableEntries = listOf(
        SearchableSettingEntry(
            key = "note_font_size",
            displayName = "Font Size",
            sectionId = "editor",
            keywords = listOf("font", "text size", "small", "medium", "large", "typography")
        ),
        SearchableSettingEntry(
            key = "note_autosave",
            displayName = "Auto-Save",
            sectionId = "editor",
            keywords = listOf("auto save", "autosave", "save interval", "automatic", "backup")
        ),
        SearchableSettingEntry(
            key = "note_spellcheck",
            displayName = "Spell Check",
            sectionId = "editor",
            keywords = listOf("spell", "spelling", "grammar", "check", "autocorrect")
        ),
        SearchableSettingEntry(
            key = "note_default_format",
            displayName = "Default Format",
            sectionId = "format",
            keywords = listOf("format", "markdown", "rich text", "document type", "note format")
        )
    )

    @Composable
    override fun SectionContent(sectionId: String) {
        when (sectionId) {
            "editor" -> EditorSection()
            "format" -> FormatSection()
        }
    }

    @Composable
    private fun EditorSection() {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Font Size",
                subtitle = "Text size in the note editor",
                icon = Icons.Default.FormatSize,
                selected = settings.noteFontSize,
                options = FONT_SIZE_OPTIONS.map { it.first },
                optionLabel = { code ->
                    FONT_SIZE_OPTIONS.find { it.first == code }?.second ?: code
                },
                onSelected = { size ->
                    scope.launch { repository.updateNoteFontSize(size) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Font Size" }
            )

            SettingsDropdownRow(
                title = "Auto-Save",
                subtitle = "How often notes are automatically saved",
                icon = Icons.Default.Save,
                selected = settings.noteAutosave,
                options = AUTOSAVE_OPTIONS.map { it.first },
                optionLabel = { code ->
                    AUTOSAVE_OPTIONS.find { it.first == code }?.second ?: code
                },
                onSelected = { interval ->
                    scope.launch { repository.updateNoteAutosave(interval) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Auto-Save" }
            )

            SettingsSwitchRow(
                title = "Spell Check",
                subtitle = "Highlight misspelled words",
                icon = Icons.Default.Spellcheck,
                checked = settings.noteSpellcheck,
                onCheckedChange = { scope.launch { repository.updateNoteSpellcheck(it) } },
                modifier = Modifier.semantics { contentDescription = "Voice: toggle Spell Check" }
            )
        }
    }

    @Composable
    private fun FormatSection() {
        val settings by repository.settings.collectAsState(initial = AvanuesSettings())
        val scope = rememberCoroutineScope()

        SettingsGroupCard {
            SettingsDropdownRow(
                title = "Default Format",
                subtitle = "Format used when creating new notes",
                icon = Icons.Default.Description,
                selected = settings.noteDefaultFormat,
                options = FORMAT_OPTIONS.map { it.first },
                optionLabel = { code ->
                    FORMAT_OPTIONS.find { it.first == code }?.second ?: code
                },
                onSelected = { format ->
                    scope.launch { repository.updateNoteDefaultFormat(format) }
                },
                modifier = Modifier.semantics { contentDescription = "Voice: select Default Format" }
            )
        }
    }

    @Composable
    override fun ModuleIcon(): ImageVector = Icons.Default.EditNote
}
