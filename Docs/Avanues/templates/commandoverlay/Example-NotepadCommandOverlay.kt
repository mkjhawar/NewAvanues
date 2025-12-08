/**
 * EXAMPLE: Voice Command Overlay for Notepad Module
 *
 * This is a complete, working example showing how to implement
 * the command overlay pattern for a Notepad module.
 *
 * Use this as a reference when creating overlays for other modules.
 */

package com.augmentalis.avanue.notepad.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanue.notepad.core.NotepadEvent
import com.augmentalis.avanue.notepad.domain.model.Note

/**
 * Voice command overlay for Notepad
 *
 * Command hierarchy:
 * - Master: Notes, Folders, Edit, Search, Settings
 * - Notes: New, Show All, Delete, Archive, Share
 * - Folders: Create, Rename, Delete, Move Note
 * - Edit: Format, Insert, Undo/Redo
 * - Search: Quick, Advanced, Recent
 * - Settings: Preferences, Sync, Backup
 *
 * @param visible Whether overlay is visible
 * @param notes List of recent notes for "Show All" view
 * @param onEvent Event handler for notepad actions
 * @param onDismiss Callback when overlay is dismissed
 */
@Composable
fun NotepadCommandOverlay(
    visible: Boolean,
    notes: List<Note>,
    onEvent: (NotepadEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentLevel by remember { mutableStateOf("master") }
    var isListening by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            currentLevel = "master"
            isListening = false
        }
    }

    val slideOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        label = "overlay_slide"
    )

    if (slideOffset < 1f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .offset(y = (slideOffset * 1000).dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable(enabled = false) { }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                CommandHeader(
                    currentLevel = currentLevel,
                    isListening = isListening,
                    onBack = { currentLevel = getParentLevel(currentLevel) },
                    onVoiceToggle = { isListening = !isListening },
                    onDismiss = onDismiss
                )

                when (currentLevel) {
                    "master" -> MasterCommands(
                        onNavigate = { level -> currentLevel = level }
                    )
                    "notes" -> NotesCommands(
                        onNavigate = { level -> currentLevel = level },
                        onEvent = onEvent
                    )
                    "show-notes" -> NotesList(
                        notes = notes,
                        onNoteSelected = { noteId ->
                            onEvent(NotepadEvent.OpenNote(noteId))
                            onDismiss()
                        }
                    )
                    "folders" -> FoldersCommands(onEvent = onEvent)
                    "edit" -> EditCommands(onEvent = onEvent)
                    "search" -> SearchCommands(onEvent = onEvent)
                    "settings" -> SettingsCommands(onEvent = onEvent)
                }
            }
        }
    }
}

@Composable
private fun CommandHeader(
    currentLevel: String,
    isListening: Boolean,
    onBack: () -> Unit,
    onVoiceToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showBackButton = currentLevel != "master"
    val title = getLevelTitle(currentLevel)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onVoiceToggle,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isListening) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Start listening",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CommandChip(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class Command(
    val icon: String,
    val label: String,
    val action: () -> Unit
)

@Composable
private fun MasterCommands(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember {
        listOf(
            Command("📝", "Notes") { onNavigate("notes") },
            Command("📂", "Folders") { onNavigate("folders") },
            Command("✏️", "Edit") { onNavigate("edit") },
            Command("🔍", "Search") { onNavigate("search") },
            Command("⚙️", "Settings") { onNavigate("settings") }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

@Composable
private fun NotesCommands(
    onNavigate: (String) -> Unit,
    onEvent: (NotepadEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onNavigate, onEvent) {
        listOf(
            Command("➕", "New Note") { onEvent(NotepadEvent.CreateNote) },
            Command("📋", "Show All") { onNavigate("show-notes") },
            Command("❌", "Delete") { onEvent(NotepadEvent.DeleteCurrentNote) },
            Command("📦", "Archive") { onEvent(NotepadEvent.ArchiveNote) },
            Command("📤", "Share") { onEvent(NotepadEvent.ShareNote) },
            Command("📌", "Pin") { onEvent(NotepadEvent.TogglePin) }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

@Composable
private fun FoldersCommands(
    onEvent: (NotepadEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onEvent) {
        listOf(
            Command("➕", "New Folder") { onEvent(NotepadEvent.CreateFolder) },
            Command("✏️", "Rename") { onEvent(NotepadEvent.RenameFolder) },
            Command("❌", "Delete") { onEvent(NotepadEvent.DeleteFolder) },
            Command("📦", "Move Note") { onEvent(NotepadEvent.MoveNoteToFolder) }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

@Composable
private fun EditCommands(
    onEvent: (NotepadEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onEvent) {
        listOf(
            Command("🔤", "Bold") { onEvent(NotepadEvent.Format.Bold) },
            Command("🔠", "Italic") { onEvent(NotepadEvent.Format.Italic) },
            Command("📝", "List") { onEvent(NotepadEvent.InsertList) },
            Command("🔗", "Link") { onEvent(NotepadEvent.InsertLink) },
            Command("↩️", "Undo") { onEvent(NotepadEvent.Undo) },
            Command("↪️", "Redo") { onEvent(NotepadEvent.Redo) }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

@Composable
private fun SearchCommands(
    onEvent: (NotepadEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onEvent) {
        listOf(
            Command("⚡", "Quick Search") { onEvent(NotepadEvent.ShowQuickSearch) },
            Command("🔍", "Advanced") { onEvent(NotepadEvent.ShowAdvancedSearch) },
            Command("🕒", "Recent") { onEvent(NotepadEvent.ShowRecentNotes) }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

@Composable
private fun SettingsCommands(
    onEvent: (NotepadEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onEvent) {
        listOf(
            Command("🎨", "Theme") { onEvent(NotepadEvent.OpenThemeSettings) },
            Command("☁️", "Sync") { onEvent(NotepadEvent.SyncNotes) },
            Command("💾", "Backup") { onEvent(NotepadEvent.BackupNotes) },
            Command("🔐", "Privacy") { onEvent(NotepadEvent.OpenPrivacySettings) }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

/**
 * Custom view: Vertical scrollable list of notes
 * Example of non-grid layout for "Show All Notes"
 */
@Composable
private fun NotesList(
    notes: List<Note>,
    onNoteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        notes.forEach { note ->
            Card(
                onClick = { onNoteSelected(note.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Note icon
                    Text(
                        text = if (note.isPinned) "📌" else "📝",
                        fontSize = 18.sp
                    )

                    // Note info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = note.preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandGrid(
    commands: List<Command>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(commands) { command ->
            CommandChip(
                icon = command.icon,
                label = command.label,
                onClick = command.action
            )
        }
    }
}

private fun getLevelTitle(level: String): String = when (level) {
    "master" -> "Voice Commands"
    "notes" -> "Note Management"
    "show-notes" -> "All Notes"
    "folders" -> "Folder Management"
    "edit" -> "Editing"
    "search" -> "Search"
    "settings" -> "Settings"
    else -> "Commands"
}

private fun getParentLevel(level: String): String = when (level) {
    "show-notes" -> "notes"
    else -> "master"
}

// ============================================
// SAMPLE NOTEPAD EVENT SEALED CLASS
// ============================================

/*
package com.augmentalis.avanue.notepad.core

sealed class NotepadEvent {
    // Note operations
    data object CreateNote : NotepadEvent()
    data class OpenNote(val noteId: String) : NotepadEvent()
    data object DeleteCurrentNote : NotepadEvent()
    data object ArchiveNote : NotepadEvent()
    data object ShareNote : NotepadEvent()
    data object TogglePin : NotepadEvent()

    // Folder operations
    data object CreateFolder : NotepadEvent()
    data object RenameFolder : NotepadEvent()
    data object DeleteFolder : NotepadEvent()
    data object MoveNoteToFolder : NotepadEvent()

    // Edit operations
    sealed class Format : NotepadEvent() {
        data object Bold : Format()
        data object Italic : Format()
    }
    data object InsertList : NotepadEvent()
    data object InsertLink : NotepadEvent()
    data object Undo : NotepadEvent()
    data object Redo : NotepadEvent()

    // Search operations
    data object ShowQuickSearch : NotepadEvent()
    data object ShowAdvancedSearch : NotepadEvent()
    data object ShowRecentNotes : NotepadEvent()

    // Settings
    data object OpenThemeSettings : NotepadEvent()
    data object SyncNotes : NotepadEvent()
    data object BackupNotes : NotepadEvent()
    data object OpenPrivacySettings : NotepadEvent()
}
*/

// ============================================
// SAMPLE NOTE DOMAIN MODEL
// ============================================

/*
package com.augmentalis.avanue.notepad.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val preview: String,
    val isPinned: Boolean = false,
    val folderId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
*/
