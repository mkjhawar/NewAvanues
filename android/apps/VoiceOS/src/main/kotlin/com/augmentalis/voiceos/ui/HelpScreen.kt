/**
 * HelpScreen.kt - Voice command help screen UI
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-29
 *
 * Compose UI for displaying available voice commands.
 * Supports:
 * - Category-based organization (Navigation, Media, System, etc.)
 * - Expandable command lists
 * - Quick reference table
 * - Tap to execute/copy command
 * - Search functionality
 */
package com.augmentalis.voiceos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import com.augmentalis.voiceos.ui.icons.VoiceOSIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceoscore.help.HelpCategory
import com.augmentalis.voiceoscore.help.HelpCommand
import com.augmentalis.voiceoscore.help.HelpCommandDataProvider

/**
 * Help screen displaying all voice commands organized by category.
 *
 * @param onNavigateBack Callback when back is pressed
 * @param onCommandTapped Callback when a command is tapped for execution
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    onCommandTapped: ((String) -> Unit)? = null
) {
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember { HelpCommandDataProvider.getCategories() }

    // Filter categories/commands if searching
    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            categories.map { category ->
                category.copy(
                    commands = category.commands.filter { cmd ->
                        cmd.primaryPhrase.contains(searchQuery, ignoreCase = true) ||
                        cmd.variations.any { it.contains(searchQuery, ignoreCase = true) } ||
                        cmd.description.contains(searchQuery, ignoreCase = true)
                    }
                )
            }.filter { it.commands.isNotEmpty() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Commands") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search commands...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Quick tip
            item {
                QuickTipCard()
            }

            // Category cards
            items(filteredCategories) { category ->
                CategoryCard(
                    category = category,
                    isExpanded = expandedCategoryId == category.id,
                    onToggleExpand = {
                        expandedCategoryId = if (expandedCategoryId == category.id) null else category.id
                    },
                    onCommandTapped = onCommandTapped
                )
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap any command to execute it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun QuickTipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                VoiceOSIcons.helpOutline(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Say \"help\" or \"what can I say\" anytime",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "to see available voice commands",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: HelpCategory,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCommandTapped: ((String) -> Unit)?
) {
    val categoryColor = remember(category.color) {
        category.color?.let {
            try { Color(android.graphics.Color.parseColor(it)) }
            catch (e: Exception) { null }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Category header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon with color
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor ?: MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getCategoryIcon(category.id),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${category.commandCount} commands",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    if (isExpanded) VoiceOSIcons.expandLess() else VoiceOSIcons.expandMore(),
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            // Expanded command list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    category.commands.forEach { command ->
                        CommandItem(
                            command = command,
                            onTapped = { onCommandTapped?.invoke(command.primaryPhrase) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandItem(
    command: HelpCommand,
    onTapped: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTapped)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "\"${command.primaryPhrase}\"",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            if (command.variations.isNotEmpty()) {
                Text(
                    "Also: ${command.variations.take(2).joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                command.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Execute",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * Get icon for category based on ID.
 */
@Composable
private fun getCategoryIcon(categoryId: String): ImageVector {
    return when (categoryId) {
        "navigation" -> VoiceOSIcons.navigation()
        "app_control" -> VoiceOSIcons.apps()
        "ui_interaction" -> VoiceOSIcons.touchApp()
        "text_input" -> VoiceOSIcons.keyboard()
        "system" -> Icons.Default.Settings
        "media" -> VoiceOSIcons.playCircle()
        "voiceos" -> VoiceOSIcons.mic()
        else -> VoiceOSIcons.helpOutline()
    }
}

/**
 * Floating Action Button for accessing help.
 * Place this in your main screen's Scaffold.
 */
@Composable
fun HelpFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Icon(
            VoiceOSIcons.helpOutline(),
            contentDescription = "Voice Command Help"
        )
    }
}

/**
 * Copy a HelpCategory with modified commands.
 */
private fun HelpCategory.copy(commands: List<HelpCommand>): HelpCategory {
    return HelpCategory(
        id = this.id,
        title = this.title,
        iconName = this.iconName,
        commands = commands,
        color = this.color
    )
}
