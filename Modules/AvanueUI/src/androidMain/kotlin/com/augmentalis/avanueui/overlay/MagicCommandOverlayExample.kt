/**
 * MagicCommandOverlay - Usage Example
 *
 * This file demonstrates how to integrate MagicCommandOverlay
 * into your Magic app or plugin.
 *
 * Created: 2025-11-08
 * Author: Manoj Jhawar, manoj@ideahq.net
 */

package com.augmentalis.avanueui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Example: Magic Text Editor with Voice Command Overlay
 *
 * This shows how to integrate the command overlay into a simple
 * Magic app (Text Editor example).
 */
@Composable
fun MagicTextEditorExample(
    modifier: Modifier = Modifier
) {
    // ============================================
    // STATE
    // ============================================

    var showCommands by remember { mutableStateOf(false) }
    var isVoiceListening by remember { mutableStateOf(false) }
    var currentText by remember { mutableStateOf("") }

    // ============================================
    // COMMAND DEFINITIONS
    // ============================================

    val commandCategories = remember {
        listOf(
            // File Operations Category
            MagicCommandCategory(
                id = "file",
                icon = "📁",
                label = "File",
                commands = listOf(
                    MagicCommand(
                        icon = "📄",
                        label = "New",
                        action = { /* Create new document */ }
                    ),
                    MagicCommand(
                        icon = "💾",
                        label = "Save",
                        action = { /* Save document */ }
                    ),
                    MagicCommand(
                        icon = "📂",
                        label = "Open",
                        action = { /* Open document */ }
                    ),
                    MagicCommand(
                        icon = "📤",
                        label = "Export",
                        action = { /* Export document */ }
                    )
                )
            ),

            // Edit Operations Category
            MagicCommandCategory(
                id = "edit",
                icon = "✏️",
                label = "Edit",
                commands = listOf(
                    MagicCommand(
                        icon = "↩️",
                        label = "Undo",
                        action = { /* Undo last action */ }
                    ),
                    MagicCommand(
                        icon = "↪️",
                        label = "Redo",
                        action = { /* Redo action */ }
                    ),
                    MagicCommand(
                        icon = "✂️",
                        label = "Cut",
                        action = { /* Cut selection */ }
                    ),
                    MagicCommand(
                        icon = "📋",
                        label = "Copy",
                        action = { /* Copy selection */ }
                    ),
                    MagicCommand(
                        icon = "📌",
                        label = "Paste",
                        action = { /* Paste from clipboard */ }
                    ),
                    MagicCommand(
                        icon = "🔍",
                        label = "Find",
                        action = { /* Open find dialog */ }
                    )
                )
            ),

            // Format Category
            MagicCommandCategory(
                id = "format",
                icon = "🎨",
                label = "Format",
                commands = listOf(
                    MagicCommand(
                        icon = "B",
                        label = "Bold",
                        action = { /* Toggle bold */ }
                    ),
                    MagicCommand(
                        icon = "I",
                        label = "Italic",
                        action = { /* Toggle italic */ }
                    ),
                    MagicCommand(
                        icon = "U",
                        label = "Underline",
                        action = { /* Toggle underline */ }
                    ),
                    MagicCommand(
                        icon = "🔤",
                        label = "Font Size",
                        action = { /* Change font size */ }
                    ),
                    MagicCommand(
                        icon = "🎨",
                        label = "Color",
                        action = { /* Change text color */ }
                    )
                )
            ),

            // Insert Category
            MagicCommandCategory(
                id = "insert",
                icon = "➕",
                label = "Insert",
                commands = listOf(
                    MagicCommand(
                        icon = "🖼️",
                        label = "Image",
                        action = { /* Insert image */ }
                    ),
                    MagicCommand(
                        icon = "🔗",
                        label = "Link",
                        action = { /* Insert hyperlink */ }
                    ),
                    MagicCommand(
                        icon = "📊",
                        label = "Table",
                        action = { /* Insert table */ }
                    ),
                    MagicCommand(
                        icon = "📅",
                        label = "Date",
                        action = { /* Insert current date */ }
                    )
                )
            ),

            // View Category
            MagicCommandCategory(
                id = "view",
                icon = "👁️",
                label = "View",
                commands = listOf(
                    MagicCommand(
                        icon = "🔍+",
                        label = "Zoom In",
                        action = { /* Increase zoom */ }
                    ),
                    MagicCommand(
                        icon = "🔍-",
                        label = "Zoom Out",
                        action = { /* Decrease zoom */ }
                    ),
                    MagicCommand(
                        icon = "📐",
                        label = "Ruler",
                        action = { /* Toggle ruler */ }
                    ),
                    MagicCommand(
                        icon = "🌙",
                        label = "Dark Mode",
                        action = { /* Toggle dark mode */ }
                    )
                )
            ),

            // Settings Category
            MagicCommandCategory(
                id = "settings",
                icon = "⚙️",
                label = "Settings",
                commands = listOf(
                    MagicCommand(
                        icon = "🔊",
                        label = "Audio",
                        action = { /* Audio settings */ }
                    ),
                    MagicCommand(
                        icon = "🎤",
                        label = "Voice",
                        action = { /* Voice settings */ }
                    ),
                    MagicCommand(
                        icon = "📝",
                        label = "Editor",
                        action = { /* Editor preferences */ }
                    ),
                    MagicCommand(
                        icon = "🌐",
                        label = "Language",
                        action = { /* Language settings */ }
                    )
                )
            )
        )
    }

    // ============================================
    // UI
    // ============================================

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCommands = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Commands"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ============================================
            // MAIN CONTENT
            // ============================================

            // Your app content here (e.g., text editor, web view, etc.)
            // ...

            // ============================================
            // COMMAND OVERLAY
            // ============================================

            MagicCommandOverlay(
                visible = showCommands,
                commandCategories = commandCategories,
                onCommand = { command ->
                    // Handle command execution
                    println("Command executed: ${command.label}")
                    showCommands = false  // Close overlay after command
                },
                onDismiss = {
                    showCommands = false
                },
                enableVoiceButton = true,
                onVoiceToggle = { listening ->
                    isVoiceListening = listening
                    // Start/stop voice recognition here
                    if (listening) {
                        // startVoiceRecognition()
                    } else {
                        // stopVoiceRecognition()
                    }
                }
            )
        }
    }
}

/**
 * Example: Simplified Integration
 *
 * Minimal example showing just the essential integration code.
 */
@Composable
fun MinimalIntegrationExample() {
    var showCommands by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            MagicCommandCategory(
                id = "actions",
                icon = "⚡",
                label = "Actions",
                commands = listOf(
                    MagicCommand("🆕", "New") { /* action */ },
                    MagicCommand("💾", "Save") { /* action */ },
                    MagicCommand("📂", "Open") { /* action */ }
                )
            )
        )
    }

    Box {
        // Your content

        // Trigger
        FloatingActionButton(onClick = { showCommands = true }) {
            Icon(Icons.Default.Mic, "Commands")
        }

        // Overlay
        MagicCommandOverlay(
            visible = showCommands,
            commandCategories = categories,
            onCommand = { command ->
                command.action()
                showCommands = false
            },
            onDismiss = { showCommands = false }
        )
    }
}

/**
 * Example: Android Plugin Integration
 *
 * Shows how to integrate into an Android Studio plugin or IDE extension.
 */
@Composable
fun AndroidPluginExample(
    onOpenFile: (String) -> Unit,
    onRunCommand: (String) -> Unit,
    onShowSettings: () -> Unit
) {
    var showCommands by remember { mutableStateOf(false) }

    val pluginCategories = remember {
        listOf(
            MagicCommandCategory(
                id = "project",
                icon = "📦",
                label = "Project",
                commands = listOf(
                    MagicCommand("🔨", "Build") { onRunCommand("build") },
                    MagicCommand("▶️", "Run") { onRunCommand("run") },
                    MagicCommand("🧪", "Test") { onRunCommand("test") },
                    MagicCommand("🔄", "Sync") { onRunCommand("sync") }
                )
            ),
            MagicCommandCategory(
                id = "navigate",
                icon = "🧭",
                label = "Navigate",
                commands = listOf(
                    MagicCommand("📂", "Find File") { /* show file search */ },
                    MagicCommand("🔍", "Find") { /* show find dialog */ },
                    MagicCommand("📝", "Recent") { /* show recent files */ }
                )
            ),
            MagicCommandCategory(
                id = "tools",
                icon = "🛠️",
                label = "Tools",
                commands = listOf(
                    MagicCommand("⚙️", "Settings") { onShowSettings() },
                    MagicCommand("🔌", "Plugins") { /* show plugins */ },
                    MagicCommand("📊", "Profiler") { /* open profiler */ }
                )
            )
        )
    }

    Box {
        // Plugin content

        MagicCommandOverlay(
            visible = showCommands,
            commandCategories = pluginCategories,
            onCommand = { it.action(); showCommands = false },
            onDismiss = { showCommands = false }
        )
    }
}
