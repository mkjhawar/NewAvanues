/**
 * CommandListActivity.kt - Searchable list of voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Created: 2025-12-08
 *
 * Full searchable list of voice commands for current app, grouped by screen/category.
 */

package com.augmentalis.voiceoscore.learnapp.ui.discovery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.dto.GeneratedCommandDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * Voice command grouped by screen context
 */
data class CommandGroup(
    val screenName: String,
    val commands: List<GeneratedCommandDTO>,
    val isExpanded: Boolean = true
)

/**
 * Command List ViewModel
 */
class CommandListViewModel(
    private val databaseManager: VoiceOSDatabaseManager,
    private val packageName: String
) : ViewModel() {

    private val _commandGroups = MutableStateFlow<List<CommandGroup>>(emptyList())
    val commandGroups: StateFlow<List<CommandGroup>> = _commandGroups.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadCommands()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterCommands()
    }

    fun toggleGroupExpanded(screenName: String) {
        _commandGroups.value = _commandGroups.value.map { group ->
            if (group.screenName == screenName) {
                group.copy(isExpanded = !group.isExpanded)
            } else {
                group
            }
        }
    }

    private fun loadCommands() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Get all commands for package
                val allCommands = databaseManager.generatedCommands.getAllCommands()

                // Group by screen context (using element hash as proxy for screen)
                // In real implementation, you'd have a screen_hash field
                val grouped = allCommands
                    .groupBy { extractScreenName(it) }
                    .map { (screenName, commands) ->
                        CommandGroup(
                            screenName = screenName,
                            commands = commands.sortedBy { it.commandText }
                        )
                    }
                    .sortedBy { it.screenName }

                _commandGroups.value = grouped

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load commands", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun filterCommands() {
        viewModelScope.launch {
            val query = _searchQuery.value.lowercase()
            if (query.isEmpty()) {
                loadCommands()
                return@launch
            }

            // Filter commands by search query
            val allCommands = databaseManager.generatedCommands.getAllCommands()
            val filtered = allCommands.filter { command ->
                command.commandText.lowercase().contains(query) ||
                        command.actionType.lowercase().contains(query)
            }

            val grouped = filtered
                .groupBy { extractScreenName(it) }
                .map { (screenName, commands) ->
                    CommandGroup(
                        screenName = screenName,
                        commands = commands.sortedBy { it.commandText }
                    )
                }
                .sortedBy { it.screenName }

            _commandGroups.value = grouped
        }
    }

    private fun extractScreenName(command: GeneratedCommandDTO): String {
        // Extract screen name from element hash or use default
        // Format: packageName.elementType-hash
        // For now, group by element type
        return when {
            command.commandText.contains("tab", ignoreCase = true) -> "Main Screen"
            command.commandText.contains("button", ignoreCase = true) -> "Buttons"
            command.commandText.contains("setting", ignoreCase = true) -> "Settings"
            else -> "Other Controls"
        }
    }

    companion object {
        private const val TAG = "CommandListViewModel"
    }
}

/**
 * Command List Activity
 *
 * Displays searchable list of all voice commands for target app.
 *
 * ## Launch:
 * ```kotlin
 * val intent = CommandListActivity.createIntent(context, "com.example.app")
 * startActivity(intent)
 * ```
 */
class CommandListActivity : ComponentActivity() {

    private lateinit var databaseManager: VoiceOSDatabaseManager
    private lateinit var textToSpeech: TextToSpeech
    private var ttsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get package name from intent
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            Log.e(TAG, "No package name provided")
            finish()
            return
        }

        // Initialize database
        databaseManager = VoiceOSDatabaseManager.getInstance(
            DatabaseDriverFactory(applicationContext)
        )

        // Initialize TTS
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInitialized = true
                textToSpeech.language = Locale.US
            }
        }

        setContent {
            MaterialTheme {
                CommandListScreen(
                    packageName = packageName,
                    databaseManager = databaseManager,
                    onClose = { finish() },
                    onSpeakCommand = { command -> speakCommand(command) }
                )
            }
        }
    }

    override fun onDestroy() {
        if (ttsInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    private fun speakCommand(command: String) {
        if (ttsInitialized) {
            textToSpeech.speak(command, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    companion object {
        private const val TAG = "CommandListActivity"
        private const val EXTRA_PACKAGE_NAME = "package_name"

        fun createIntent(context: Context, packageName: String): Intent {
            return Intent(context, CommandListActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandListScreen(
    packageName: String,
    databaseManager: VoiceOSDatabaseManager,
    onClose: () -> Unit,
    onSpeakCommand: (String) -> Unit
) {
    val viewModel: CommandListViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CommandListViewModel(databaseManager, packageName) as T
            }
        }
    )

    val commandGroups by viewModel.commandGroups.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Voice Commands",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = packageName.substringAfterLast("."),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    // Export commands
                    IconButton(onClick = { /* TODO: Export */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Command groups
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (commandGroups.isEmpty()) {
                EmptyState()
            } else {
                CommandGroupsList(
                    groups = commandGroups,
                    onToggleGroup = { viewModel.toggleGroupExpanded(it) },
                    onSpeakCommand = onSpeakCommand
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search commands...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun CommandGroupsList(
    groups: List<CommandGroup>,
    onToggleGroup: (String) -> Unit,
    onSpeakCommand: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groups.forEach { group ->
            item {
                CommandGroupHeader(
                    group = group,
                    onToggle = { onToggleGroup(group.screenName) }
                )
            }

            if (group.isExpanded) {
                items(group.commands) { command ->
                    CommandItem(
                        command = command,
                        onSpeak = { onSpeakCommand(command.commandText) }
                    )
                }
            }
        }

        // Help footer
        item {
            HelpFooter()
        }
    }
}

@Composable
fun CommandGroupHeader(
    group: CommandGroup,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        group.screenName.contains("Main") -> Icons.Default.Home
                        group.screenName.contains("Settings") -> Icons.Default.Settings
                        else -> Icons.Default.Apps
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        text = group.screenName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${group.commands.size} commands",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                imageVector = if (group.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (group.isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun CommandItem(
    command: GeneratedCommandDTO,
    onSpeak: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${command.commandText}\"",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Action type badge
                    Badge(
                        text = command.actionType,
                        color = when (command.actionType.lowercase()) {
                            "click" -> Color(0xFF4CAF50)
                            "type" -> Color(0xFF2196F3)
                            "scroll" -> Color(0xFFFFC107)
                            else -> Color.Gray
                        }
                    )
                    // Confidence badge
                    Badge(
                        text = "${(command.confidence * 100).toInt()}%",
                        color = when {
                            command.confidence >= 0.85 -> Color(0xFF4CAF50)
                            command.confidence >= 0.60 -> Color(0xFFFFC107)
                            else -> Color(0xFFFF5722)
                        }
                    )
                }
            }

            // Speak button
            IconButton(onClick = onSpeak) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speak command",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = "No commands found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Try learning this app first",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun HelpFooter() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💡 Tip",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Say \"Show commands on screen\" to see labels overlaid on the app",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
