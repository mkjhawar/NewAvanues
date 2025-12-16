/**
 * CommandTestingPanel.kt - Real-time command testing interface
 *
 * Features:
 * - Voice input simulation (text or actual voice)
 * - Command matching visualization
 * - Conflict detection display
 * - Success/failure feedback
 */

package com.augmentalis.commandmanager.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augmentalis.commandmanager.registry.ConflictInfo
import com.augmentalis.commandmanager.registry.ConflictType
import com.augmentalis.commandmanager.registry.VoiceCommand

/**
 * Command testing panel screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandTestingPanel(
    viewModel: CommandEditorViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    var testPhrase by remember { mutableStateOf("") }
    var testResults by remember { mutableStateOf<List<VoiceCommand>>(emptyList()) }
    var selectedCommand by remember { mutableStateOf<VoiceCommand?>(null) }
    var conflicts by remember { mutableStateOf<List<ConflictInfo>>(emptyList()) }
    var testHistory by remember { mutableStateOf<List<TestHistoryEntry>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Command Testing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Input section
            TestInputSection(
                testPhrase = testPhrase,
                onPhraseChange = { testPhrase = it },
                onTest = {
                    testResults = viewModel.testCommand(testPhrase)

                    // Add to history
                    testHistory = listOf(
                        TestHistoryEntry(
                            phrase = testPhrase,
                            matchCount = testResults.size,
                            timestamp = System.currentTimeMillis()
                        )
                    ) + testHistory
                },
                onClear = {
                    testPhrase = ""
                    testResults = emptyList()
                    selectedCommand = null
                    conflicts = emptyList()
                }
            )

            HorizontalDivider()

            // Results section
            if (testResults.isNotEmpty()) {
                TestResultsSection(
                    results = testResults,
                    selectedCommand = selectedCommand,
                    onSelectCommand = { command ->
                        selectedCommand = command
                        conflicts = viewModel.detectConflicts(command)
                    }
                )
            } else if (testPhrase.isNotEmpty()) {
                NoMatchesMessage()
            }

            // Conflict detection
            if (conflicts.isNotEmpty()) {
                HorizontalDivider()
                ConflictDetectionSection(conflicts = conflicts)
            }

            // Test history
            if (testHistory.isNotEmpty()) {
                HorizontalDivider()
                TestHistorySection(
                    history = testHistory,
                    onRetest = { entry ->
                        testPhrase = entry.phrase
                        testResults = viewModel.testCommand(entry.phrase)
                    }
                )
            }
        }
    }
}

/**
 * Input section for testing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestInputSection(
    testPhrase: String,
    onPhraseChange: (String) -> Unit,
    onTest: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Test Voice Command",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Enter a phrase to see which commands match",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = testPhrase,
                    onValueChange = onPhraseChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g., open settings") },
                    leadingIcon = { Icon(Icons.Default.Mic, "Voice input") },
                    trailingIcon = {
                        if (testPhrase.isNotEmpty()) {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true
                )

                Button(
                    onClick = onTest,
                    enabled = testPhrase.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Test")
                }
            }

            // Quick test buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onPhraseChange("go back") },
                    label = { Text("go back") }
                )
                AssistChip(
                    onClick = { onPhraseChange("volume up") },
                    label = { Text("volume up") }
                )
                AssistChip(
                    onClick = { onPhraseChange("open app") },
                    label = { Text("open app") }
                )
            }
        }
    }
}

/**
 * Test results section
 */
@Composable
private fun TestResultsSection(
    results: List<VoiceCommand>,
    selectedCommand: VoiceCommand?,
    onSelectCommand: (VoiceCommand) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Matching Commands (${results.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Commands are sorted by priority (highest first)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results) { command ->
                CommandMatchCard(
                    command = command,
                    rank = results.indexOf(command) + 1,
                    isSelected = selectedCommand == command,
                    onClick = { onSelectCommand(command) }
                )
            }
        }
    }
}

/**
 * Command match card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandMatchCard(
    command: VoiceCommand,
    rank: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (rank) {
                    1 -> MaterialTheme.colorScheme.primary
                    2 -> MaterialTheme.colorScheme.secondary
                    3 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = "#$rank",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.phrases.firstOrNull() ?: command.id,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "ID: ${command.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityBadge(priority = command.priority)
                    AssistChip(
                        onClick = { },
                        label = { Text(command.actionType.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Priority badge
 */
@Composable
private fun PriorityBadge(priority: Int) {
    val color = when {
        priority >= 80 -> Color(0xFF4CAF50) // Green
        priority >= 50 -> Color(0xFFFFC107) // Amber
        else -> Color(0xFFF44336) // Red
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color
    ) {
        Text(
            text = "P$priority",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Conflict detection section
 */
@Composable
private fun ConflictDetectionSection(conflicts: List<ConflictInfo>) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Conflicts Detected (${conflicts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }

        conflicts.forEach { conflict ->
            ConflictCard(conflict = conflict)
        }
    }
}

/**
 * Conflict card
 */
@Composable
private fun ConflictCard(conflict: ConflictInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Phrase: \"${conflict.phrase}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Conflicts with: ${conflict.conflictingCommandName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Type: ${conflict.conflictType.name.replace("_", " ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Priority: ${conflict.priority}", style = MaterialTheme.typography.labelSmall)
                Text("Namespace: ${conflict.namespace}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * No matches message
 */
@Composable
private fun NoMatchesMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No matching commands found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Try a different phrase or create a new command",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Test history section
 */
@Composable
private fun TestHistorySection(
    history: List<TestHistoryEntry>,
    onRetest: (TestHistoryEntry) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Test History",
            style = MaterialTheme.typography.titleSmall
        )

        history.take(5).forEach { entry ->
            TestHistoryItem(entry = entry, onRetest = { onRetest(entry) })
        }
    }
}

/**
 * Test history item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestHistoryItem(
    entry: TestHistoryEntry,
    onRetest: () -> Unit
) {
    Card(
        onClick = onRetest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${entry.phrase}\"",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${entry.matchCount} matches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.Refresh, "Retest")
        }
    }
}

/**
 * Test history entry data class
 */
data class TestHistoryEntry(
    val phrase: String,
    val matchCount: Int,
    val timestamp: Long
)
