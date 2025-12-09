# LearnApp On-Demand Command Renaming - Phase 2 Enhanced

**Document**: LearnApp-On-Demand-Command-Renaming-5081220-V2.md
**Author**: Manoj Jhawar
**Code-Reviewed-By**: Claude Code (IDEACODE v10.3)
**Created**: 2025-12-08
**Supersedes**: LearnApp-User-Command-Assignment-Enhancement (Phase 2 only)

---

## Design Philosophy

### Old Approach (Discarded)
❌ Pause exploration to ask user for command names
❌ Interrupts workflow
❌ Forces decision before user understands context

### New Approach (This Document)
✅ Never interrupt exploration - always use fallback labels
✅ Show contextual hint when screen has generated labels
✅ User renames commands naturally while using the app
✅ Rename command: "Rename Button 1 to Save"
✅ Synonyms stored, original label kept as fallback

---

## Phase 2: On-Demand Command Renaming

### User Flow

```
1. Exploration generates fallback labels
   - Button 1, Button 2, Tab 1, etc.
   - Exploration completes without interruption

2. User opens app
   - System detects screen has generated labels
   - Shows contextual hint overlay (3 seconds)
   - "You can rename buttons by saying: Rename Button 1 to Save"

3. User continues using app
   - Optional: Says "Rename Button 1 to Save"
   - System adds "Save" as synonym
   - User can now say "Save" or "Button 1"

4. Alternative: Settings UI
   - User: "Open voice command settings"
   - Choose app from list
   - Select command to edit
   - Add synonyms
```

---

## Component 1: Contextual Hint Overlay

### RenameHintOverlay.kt

```kotlin
/**
 * Contextual hint overlay for command renaming
 *
 * Shows brief hint when screen has generated fallback labels.
 * Only shows once per screen per session.
 *
 * Features:
 * - 3-second auto-dismiss
 * - Non-intrusive (top of screen)
 * - Shows only on screens with generated labels
 * - Session-based (doesn't repeat)
 */
class RenameHintOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var overlayView: View? = null
    private val shownScreens = mutableSetOf<String>()

    /**
     * Show rename hint if screen has generated labels
     *
     * @param packageName Current app package
     * @param activityName Current activity name
     * @param generatedCommands List of commands with generated labels
     */
    fun showIfNeeded(
        packageName: String,
        activityName: String,
        generatedCommands: List<GeneratedCommandDTO>
    ) {
        // Screen identifier
        val screenKey = "$packageName/$activityName"

        // Already shown this session?
        if (shownScreens.contains(screenKey)) {
            Log.d(TAG, "Hint already shown for $screenKey")
            return
        }

        // Any generated labels on this screen?
        val hasGeneratedLabels = generatedCommands.any { command ->
            isGeneratedLabel(command.commandText)
        }

        if (!hasGeneratedLabels) {
            Log.d(TAG, "No generated labels on $screenKey")
            return
        }

        // Show hint
        show(generatedCommands.first())
        shownScreens.add(screenKey)
    }

    /**
     * Check if command text is a generated label
     *
     * Patterns:
     * - "click button 1", "click tab 2"
     * - "click top button", "click bottom card"
     * - "click top left button" (Unity)
     * - "click corner top far left button" (Unreal)
     */
    private fun isGeneratedLabel(commandText: String): Boolean {
        val patterns = listOf(
            Regex("click (button|tab|card|option) \\d+"),        // Position-based
            Regex("click (top|bottom|center) .+"),                // Context-aware
            Regex("click (top|middle|bottom) (left|center|right) .+"), // Unity 3x3
            Regex("click corner .+"),                             // Unreal corners
            Regex("click (upper|lower) .+")                       // Unreal 4x4
        )

        return patterns.any { it.matches(commandText) }
    }

    private fun show(exampleCommand: GeneratedCommandDTO) {
        // Create overlay view
        overlayView = createHintView(exampleCommand)

        // Window params - top of screen, non-focusable
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100 // 100px from top
        }

        windowManager.addView(overlayView, params)

        // Auto-dismiss after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            hide()
        }, 3000)
    }

    private fun createHintView(exampleCommand: GeneratedCommandDTO): View {
        return ComposeView(context).apply {
            setContent {
                RenameHintCard(exampleCommand = exampleCommand)
            }
        }
    }

    private fun hide() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
    }

    /**
     * Reset shown screens (for testing or new session)
     */
    fun reset() {
        shownScreens.clear()
    }

    companion object {
        private const val TAG = "RenameHintOverlay"
    }
}

@Composable
fun RenameHintCard(exampleCommand: GeneratedCommandDTO) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Info",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    "Rename buttons by saying:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                // Extract button name from command
                val buttonName = extractButtonName(exampleCommand.commandText)

                Text(
                    "\"Rename $buttonName to Save\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

/**
 * Extract button name from command text
 * "click button 1" → "Button 1"
 * "click top left button" → "Top Left Button"
 */
private fun extractButtonName(commandText: String): String {
    return commandText
        .removePrefix("click ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
```

---

## Component 2: Rename Command Handler

### RenameCommandHandler.kt

```kotlin
/**
 * Handles "Rename [old name] to [new name]" voice commands
 *
 * Features:
 * - Adds new name as synonym (keeps original)
 * - Updates database immediately
 * - Provides voice feedback
 * - Handles multiple formats
 *
 * Supported formats:
 * - "Rename Button 1 to Save"
 * - "Rename Button 1 to Save Button"
 * - "Rename click button 1 to save"
 * - "Rename Tab 2 to Settings"
 */
class RenameCommandHandler(
    private val context: Context,
    private val database: VoiceOSDatabaseManager,
    private val tts: TextToSpeech
) {
    companion object {
        private const val TAG = "RenameCommandHandler"
    }

    /**
     * Process rename command
     *
     * @param voiceInput Raw voice input (e.g., "Rename Button 1 to Save")
     * @param packageName Current app package
     * @return RenameResult
     */
    suspend fun processRenameCommand(
        voiceInput: String,
        packageName: String
    ): RenameResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing rename: '$voiceInput' for $packageName")

            // Parse command
            val parsed = parseRenameCommand(voiceInput)
                ?: return@withContext RenameResult.Error("Could not understand rename command")

            Log.d(TAG, "Parsed: oldName='${parsed.oldName}', newName='${parsed.newName}'")

            // Find command by old name
            val command = findCommandByName(parsed.oldName, packageName)
                ?: return@withContext RenameResult.Error("Could not find command '${parsed.oldName}'")

            Log.d(TAG, "Found command: ${command.commandText}")

            // Add new name as synonym
            val updatedCommand = addSynonym(command, parsed.newName)

            // Update database
            database.generatedCommands.update(updatedCommand)

            Log.i(TAG, "✅ Renamed '${parsed.oldName}' → '${parsed.newName}'")

            // Voice feedback
            withContext(Dispatchers.Main) {
                tts.speak(
                    "Renamed to ${parsed.newName}. You can now say ${parsed.newName} or ${parsed.oldName}.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "rename_success"
                )
            }

            RenameResult.Success(
                oldName = parsed.oldName,
                newName = parsed.newName,
                command = updatedCommand
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing rename command", e)
            RenameResult.Error("Failed to rename: ${e.message}")
        }
    }

    /**
     * Parse rename command from voice input
     *
     * Supported patterns:
     * - "Rename [old] to [new]"
     * - "Rename [old] as [new]"
     * - "Change [old] to [new]"
     */
    private fun parseRenameCommand(voiceInput: String): ParsedRename? {
        val normalized = voiceInput.trim().lowercase()

        // Pattern 1: "rename X to Y"
        val pattern1 = Regex("rename (.+) to (.+)")
        pattern1.find(normalized)?.let { match ->
            return ParsedRename(
                oldName = match.groupValues[1].trim(),
                newName = match.groupValues[2].trim()
            )
        }

        // Pattern 2: "rename X as Y"
        val pattern2 = Regex("rename (.+) as (.+)")
        pattern2.find(normalized)?.let { match ->
            return ParsedRename(
                oldName = match.groupValues[1].trim(),
                newName = match.groupValues[2].trim()
            )
        }

        // Pattern 3: "change X to Y"
        val pattern3 = Regex("change (.+) to (.+)")
        pattern3.find(normalized)?.let { match ->
            return ParsedRename(
                oldName = match.groupValues[1].trim(),
                newName = match.groupValues[2].trim()
            )
        }

        return null
    }

    /**
     * Find command by name (fuzzy match)
     *
     * Matches:
     * - "button 1" → "click button 1"
     * - "Button 1" → "click button 1"
     * - "click button 1" → "click button 1"
     */
    private suspend fun findCommandByName(
        name: String,
        packageName: String
    ): GeneratedCommandDTO? = withContext(Dispatchers.IO) {
        val normalized = name.lowercase()

        // Get all commands for package
        val commands = database.generatedCommands.getByPackage(packageName)

        // Try exact match first
        commands.firstOrNull { cmd ->
            cmd.commandText.lowercase() == normalized
        }?.let { return@withContext it }

        // Try match without "click" prefix
        val withoutAction = normalized.removePrefix("click ").removePrefix("type ").removePrefix("scroll ")
        commands.firstOrNull { cmd ->
            cmd.commandText.lowercase().contains(withoutAction)
        }?.let { return@withContext it }

        // Try match with "click" prefix added
        val withAction = "click $normalized"
        commands.firstOrNull { cmd ->
            cmd.commandText.lowercase() == withAction
        }?.let { return@withContext it }

        null
    }

    /**
     * Add synonym to command
     *
     * Synonyms format: "save,submit,send" (comma-separated)
     * If synonyms already exist, append new synonym
     */
    private fun addSynonym(
        command: GeneratedCommandDTO,
        newName: String
    ): GeneratedCommandDTO {
        val existingSynonyms = command.synonyms
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()

        // Add new synonym
        existingSynonyms.add(newName.lowercase())

        // Add original command text as synonym (if not already)
        val originalLabel = command.commandText.removePrefix("${command.actionType} ")
        existingSynonyms.add(originalLabel.lowercase())

        return command.copy(
            synonyms = existingSynonyms.joinToString(",")
        )
    }
}

/**
 * Parsed rename command
 */
data class ParsedRename(
    val oldName: String,
    val newName: String
)

/**
 * Rename command result
 */
sealed class RenameResult {
    data class Success(
        val oldName: String,
        val newName: String,
        val command: GeneratedCommandDTO
    ) : RenameResult()

    data class Error(val message: String) : RenameResult()
}
```

---

## Component 3: Integration with VoiceCommandExecutor

### VoiceCommandExecutor.kt (Enhanced)

```kotlin
/**
 * Voice Command Executor
 *
 * Routes voice commands to appropriate handlers.
 * Enhanced with rename command detection.
 */
class VoiceCommandExecutor(
    private val context: Context,
    private val database: VoiceOSDatabaseManager,
    private val commandManager: CommandManager
) {
    private val renameHandler by lazy {
        RenameCommandHandler(
            context = context,
            database = database,
            tts = getTTS()
        )
    }

    /**
     * Execute voice command
     *
     * @param voiceInput Raw voice input
     * @param packageName Current foreground package
     */
    suspend fun execute(voiceInput: String, packageName: String): ExecutionResult {
        Log.d(TAG, "Executing: '$voiceInput' for $packageName")

        // Check if rename command
        if (isRenameCommand(voiceInput)) {
            return handleRenameCommand(voiceInput, packageName)
        }

        // Check if synonym exists
        val resolvedCommand = resolveCommandWithSynonyms(voiceInput, packageName)
        if (resolvedCommand != null) {
            return executeCommand(resolvedCommand)
        }

        // Standard command routing
        return commandManager.execute(voiceInput, packageName)
    }

    /**
     * Check if voice input is a rename command
     */
    private fun isRenameCommand(voiceInput: String): Boolean {
        val normalized = voiceInput.lowercase()
        return normalized.startsWith("rename ") ||
               normalized.startsWith("change ")
    }

    /**
     * Handle rename command
     */
    private suspend fun handleRenameCommand(
        voiceInput: String,
        packageName: String
    ): ExecutionResult {
        return when (val result = renameHandler.processRenameCommand(voiceInput, packageName)) {
            is RenameResult.Success -> {
                ExecutionResult.Success("Command renamed successfully")
            }
            is RenameResult.Error -> {
                ExecutionResult.Error(result.message)
            }
        }
    }

    /**
     * Resolve command with synonyms
     *
     * If voice input matches a synonym, return original command.
     */
    private suspend fun resolveCommandWithSynonyms(
        voiceInput: String,
        packageName: String
    ): GeneratedCommandDTO? = withContext(Dispatchers.IO) {
        val normalized = voiceInput.lowercase()

        val commands = database.generatedCommands.getByPackage(packageName)

        commands.firstOrNull { command ->
            // Check synonyms
            val synonyms = command.synonyms
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            synonyms.any { synonym ->
                normalized == synonym ||
                normalized == "${command.actionType} $synonym"
            }
        }
    }

    private suspend fun executeCommand(command: GeneratedCommandDTO): ExecutionResult {
        // Execute the actual command
        // ... implementation
        return ExecutionResult.Success("Command executed")
    }

    companion object {
        private const val TAG = "VoiceCommandExecutor"
    }
}
```

---

## Component 4: Screen Activity Detection

### ScreenActivityDetector.kt

```kotlin
/**
 * Detects screen/activity changes and triggers rename hints
 *
 * Observes accessibility events to detect when user navigates to new screen.
 * Checks if screen has generated labels and shows hint overlay if needed.
 */
class ScreenActivityDetector(
    private val context: Context,
    private val database: VoiceOSDatabaseManager,
    private val renameHintOverlay: RenameHintOverlay
) {
    private var currentScreen: String = ""

    /**
     * Called when accessibility event TYPE_WINDOW_STATE_CHANGED fires
     *
     * @param event Accessibility event
     */
    suspend fun onWindowStateChanged(event: AccessibilityEvent) = withContext(Dispatchers.IO) {
        val packageName = event.packageName?.toString() ?: return@withContext
        val className = event.className?.toString() ?: return@withContext

        val screenKey = "$packageName/$className"

        // New screen?
        if (screenKey != currentScreen) {
            Log.d(TAG, "Screen changed: $currentScreen → $screenKey")
            currentScreen = screenKey

            // Get commands for this screen
            val commands = getCommandsForScreen(packageName, className)

            // Show hint if needed
            withContext(Dispatchers.Main) {
                renameHintOverlay.showIfNeeded(packageName, className, commands)
            }
        }
    }

    /**
     * Get commands for current screen
     *
     * Filters commands by package and screen (if screen metadata available).
     */
    private suspend fun getCommandsForScreen(
        packageName: String,
        className: String
    ): List<GeneratedCommandDTO> = withContext(Dispatchers.IO) {
        // Get all commands for package
        val allCommands = database.generatedCommands.getByPackage(packageName)

        // TODO: Filter by screen if we have screen metadata in database
        // For now, return all commands for package
        allCommands
    }

    companion object {
        private const val TAG = "ScreenActivityDetector"
    }
}
```

---

## Component 5: Settings UI for Synonym Management

### CommandSynonymSettingsActivity.kt

```kotlin
/**
 * Activity for managing command synonyms
 *
 * User flow:
 * 1. Choose app from list
 * 2. View all commands for app
 * 3. Select command to edit
 * 4. Add/remove synonyms
 */
class CommandSynonymSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoiceOSTheme {
                CommandSynonymSettingsScreen()
            }
        }
    }
}

@Composable
fun CommandSynonymSettingsScreen() {
    val viewModel: CommandSynonymViewModel = viewModel()
    val apps by viewModel.installedApps.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Command Synonyms") },
                navigationIcon = {
                    if (selectedApp != null) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (selectedApp == null) {
            // App selection screen
            AppListScreen(
                apps = apps,
                onAppSelected = { viewModel.selectApp(it) },
                modifier = Modifier.padding(padding)
            )
        } else {
            // Command list screen for selected app
            CommandListScreen(
                packageName = selectedApp!!,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun AppListScreen(
    apps: List<AppInfo>,
    onAppSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(apps) { app ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onAppSelected(app.packageName) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App icon
                    Image(
                        painter = rememberImagePainter(app.icon),
                        contentDescription = app.name,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            app.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "${app.commandCount} commands",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommandListScreen(
    packageName: String,
    viewModel: CommandSynonymViewModel,
    modifier: Modifier = Modifier
) {
    val commands by viewModel.commandsForApp.collectAsState()
    var selectedCommand by remember { mutableStateOf<GeneratedCommandDTO?>(null) }

    if (selectedCommand != null) {
        // Show synonym editor dialog
        SynonymEditorDialog(
            command = selectedCommand!!,
            onDismiss = { selectedCommand = null },
            onSave = { updatedCommand ->
                viewModel.updateCommand(updatedCommand)
                selectedCommand = null
            }
        )
    }

    LazyColumn(modifier = modifier) {
        items(commands) { command ->
            CommandSynonymItem(
                command = command,
                onClick = { selectedCommand = command }
            )
        }
    }
}

@Composable
fun CommandSynonymItem(
    command: GeneratedCommandDTO,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Command name
            Text(
                command.commandText,
                style = MaterialTheme.typography.titleMedium
            )

            // Synonyms
            if (command.synonyms.isNotBlank()) {
                Spacer(Modifier.height(8.dp))

                val synonymList = command.synonyms.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    synonymList.forEach { synonym ->
                        SuggestionChip(
                            onClick = { /* No-op */ },
                            label = { Text(synonym) }
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    "No synonyms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SynonymEditorDialog(
    command: GeneratedCommandDTO,
    onDismiss: () -> Unit,
    onSave: (GeneratedCommandDTO) -> Unit
) {
    var synonymsText by remember {
        mutableStateOf(command.synonyms)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Synonyms") },
        text = {
            Column {
                Text(
                    "Command: ${command.commandText}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = synonymsText,
                    onValueChange = { synonymsText = it },
                    label = { Text("Synonyms (comma-separated)") },
                    placeholder = { Text("save, submit, send") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Enter synonyms separated by commas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(command.copy(synonyms = synonymsText))
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * ViewModel for command synonym settings
 */
class CommandSynonymViewModel(
    private val database: VoiceOSDatabaseManager,
    private val packageManager: PackageManager
) : ViewModel() {
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    private val _selectedApp = MutableStateFlow<String?>(null)
    val selectedApp = _selectedApp.asStateFlow()

    private val _commandsForApp = MutableStateFlow<List<GeneratedCommandDTO>>(emptyList())
    val commandsForApp = _commandsForApp.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { appInfo ->
                    try {
                        val name = packageManager.getApplicationLabel(appInfo).toString()
                        val commandCount = database.generatedCommands.getByPackage(appInfo.packageName).size

                        if (commandCount > 0) {
                            AppInfo(
                                packageName = appInfo.packageName,
                                name = name,
                                icon = appInfo.loadIcon(packageManager),
                                commandCount = commandCount
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedBy { it.name }

            _installedApps.value = apps
        }
    }

    fun selectApp(packageName: String) {
        _selectedApp.value = packageName
        loadCommandsForApp(packageName)
    }

    fun clearSelection() {
        _selectedApp.value = null
        _commandsForApp.value = emptyList()
    }

    private fun loadCommandsForApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val commands = database.generatedCommands.getByPackage(packageName)
                .sortedBy { it.commandText }
            _commandsForApp.value = commands
        }
    }

    fun updateCommand(command: GeneratedCommandDTO) {
        viewModelScope.launch(Dispatchers.IO) {
            database.generatedCommands.update(command)
            // Reload commands
            _selectedApp.value?.let { loadCommandsForApp(it) }
        }
    }
}

/**
 * App info for synonym settings
 */
data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable,
    val commandCount: Int
)
```

---

## Integration with VoiceOS Accessibility Service

### VoiceOSAccessibilityService.kt (Enhanced)

```kotlin
class VoiceOSAccessibilityService : AccessibilityService() {

    private lateinit var screenActivityDetector: ScreenActivityDetector
    private lateinit var renameHintOverlay: RenameHintOverlay
    private lateinit var voiceCommandExecutor: VoiceCommandExecutor

    override fun onCreate() {
        super.onCreate()

        // Initialize components
        renameHintOverlay = RenameHintOverlay(
            context = this,
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        )

        screenActivityDetector = ScreenActivityDetector(
            context = this,
            database = VoiceOSDatabaseManager.getInstance(this),
            renameHintOverlay = renameHintOverlay
        )

        voiceCommandExecutor = VoiceCommandExecutor(
            context = this,
            database = VoiceOSDatabaseManager.getInstance(this),
            commandManager = CommandManager.getInstance(this)
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Screen changed - check if we should show rename hint
                lifecycleScope.launch {
                    screenActivityDetector.onWindowStateChanged(event)
                }
            }
            // ... other event types
        }
    }

    /**
     * Called when voice command received
     * (from voice recognition service)
     */
    fun onVoiceCommandReceived(voiceInput: String) {
        lifecycleScope.launch {
            val packageName = getCurrentForegroundPackage()
            if (packageName != null) {
                voiceCommandExecutor.execute(voiceInput, packageName)
            }
        }
    }

    private fun getCurrentForegroundPackage(): String? {
        return rootInActiveWindow?.packageName?.toString()
    }

    override fun onInterrupt() {
        // Handle interrupt
    }
}
```

---

## User Experience Examples

### Example 1: First Time Using App with Generated Labels

```
[User opens DeviceInfo app]

System (overlay appears at top):
┌─────────────────────────────────────────┐
│ ℹ️ Rename buttons by saying:             │
│ "Rename Button 1 to Save"               │
└─────────────────────────────────────────┘

[Overlay auto-dismisses after 3 seconds]

User: "Button 1"
[Taps first tab]

User: "Rename Button 1 to Device Info"

System (TTS): "Renamed to Device Info. You can now say Device Info or Button 1."

User: "Device Info"
[Taps first tab using new name]

✅ Works perfectly!
```

### Example 2: Adding Synonym via Settings

```
User: "Open voice command settings"

[Shows list of apps with commands]
- DeviceInfo (117 commands)
- Microsoft Teams (45 commands)
- Chrome (28 commands)

User taps "DeviceInfo"

[Shows command list]
- click button 1
  Synonyms: device info

- click button 2
  No synonyms

- click tab 1
  Synonyms: settings

User taps "click button 2"

[Dialog appears]
Edit Synonyms
Command: click button 2

Synonyms (comma-separated):
[                            ]
Enter synonyms separated by commas

[Cancel] [Save]

User types: "system info, about device"
User taps "Save"

System: "Synonyms saved"

[Back to command list]
- click button 2
  Synonyms: system info, about device

✅ User can now say "Button 2", "System Info", or "About Device"
```

### Example 3: Multiple Synonyms for Same Command

```
User: "Rename Button 1 to Save"
System: "Renamed to Save"

[Later in settings]
User adds synonym: "Submit, Send"

Final synonyms for "click button 1":
- button 1 (original generated label)
- save (added via voice)
- submit (added via settings)
- send (added via settings)

User can now say ANY of these:
✅ "Button 1"
✅ "Save"
✅ "Submit"
✅ "Send"

All execute the same command!
```

---

## Database Schema Updates

### GeneratedCommandDTO (No changes needed!)

The existing `synonyms` field (comma-separated string) already supports this feature:

```kotlin
data class GeneratedCommandDTO(
    val id: Long,
    val elementHash: String,
    val commandText: String,      // Original: "click button 1"
    val actionType: String,        // "click"
    val confidence: Float,
    val synonyms: String,          // "save,submit,send" (NEW: populated by rename)
    val isUserApproved: Long,
    val usageCount: Long,
    val lastUsed: Long?,
    val createdAt: Long
)
```

---

## Settings & Preferences

### VoiceOS Settings UI

Add new section: **Voice Commands**

```
Settings
  └─ Voice Commands
       ├─ Manage Synonyms
       │    └─ (Opens CommandSynonymSettingsActivity)
       │
       ├─ Show Rename Hints
       │    └─ [Toggle] Enabled
       │    └─ Shows hint overlay on screens with generated labels
       │
       ├─ Hint Duration
       │    └─ [Slider] 1-10 seconds (default: 3)
       │
       └─ Reset Shown Hints
            └─ [Button] Reset
            └─ Shows hints again for all screens
```

### Developer Settings

Add to LearnAppDeveloperSettings:

```kotlin
fun isRenameHintEnabled(): Boolean {
    return prefs.getBoolean("rename_hint_enabled", true)
}

fun setRenameHintEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("rename_hint_enabled", enabled).apply()
}

fun getRenameHintDuration(): Long {
    return prefs.getLong("rename_hint_duration", 3000)
}

fun setRenameHintDuration(durationMs: Long) {
    prefs.edit().putLong("rename_hint_duration", durationMs).apply()
}
```

---

## Implementation Roadmap

### Week 1: Core Rename Handler
- [ ] Create RenameCommandHandler.kt
- [ ] Implement parseRenameCommand()
- [ ] Implement findCommandByName() (fuzzy match)
- [ ] Implement addSynonym()
- [ ] Unit tests (10 test cases)

### Week 2: Hint Overlay & Screen Detection
- [ ] Create RenameHintOverlay.kt
- [ ] Create ScreenActivityDetector.kt
- [ ] Implement showIfNeeded() logic
- [ ] Implement isGeneratedLabel() detection
- [ ] Integration with VoiceOSAccessibilityService

### Week 3: Voice Command Integration
- [ ] Enhance VoiceCommandExecutor with rename detection
- [ ] Implement synonym resolution
- [ ] Test voice flow (rename + use synonym)
- [ ] Voice feedback and TTS integration

### Week 4: Settings UI
- [ ] Create CommandSynonymSettingsActivity
- [ ] Create CommandSynonymViewModel
- [ ] Implement app list + command list UI
- [ ] Implement synonym editor dialog
- [ ] Add to VoiceOS settings

### Week 5: Polish & Testing
- [ ] End-to-end testing with DeviceInfo
- [ ] Test with multiple apps
- [ ] Performance optimization
- [ ] User documentation
- [ ] Video demo

---

## Testing Plan

### Unit Tests

**RenameCommandHandler Tests** (10 cases):
1. Parse "rename button 1 to save"
2. Parse "rename button 1 as save"
3. Parse "change button 1 to save"
4. Parse invalid format (should return null)
5. Find command by exact name
6. Find command by partial name (fuzzy)
7. Find command with "click" prefix
8. Add synonym to empty synonyms
9. Add synonym to existing synonyms
10. Update database and verify

**RenameHintOverlay Tests** (5 cases):
1. Show hint on screen with generated labels
2. Don't show hint if already shown
3. Don't show hint if no generated labels
4. Auto-dismiss after 3 seconds
5. Detect all generated label patterns

### Integration Tests

**Rename Flow**:
1. Open app with generated labels
2. Hint overlay shows
3. User says "Rename Button 1 to Save"
4. Synonym added to database
5. User says "Save"
6. Command executes

**Settings Flow**:
1. Open voice command settings
2. Select app
3. Select command
4. Add synonym via dialog
5. Save and verify
6. Use synonym via voice

### Performance Tests

**Rename Handler**:
- Parse command: < 1ms
- Find command: < 5ms (100 commands)
- Update database: < 10ms

**Hint Overlay**:
- Show overlay: < 100ms
- Auto-dismiss: 3000ms ± 50ms

---

## User Documentation

### Help Text

**Voice Commands - Renaming**

You can rename auto-generated button names (like "Button 1") to more meaningful names.

**How to Rename:**
1. Open the app
2. When you see a hint at the top of the screen, say:
   "Rename Button 1 to Save"
3. The system will remember your new name

**Using Renamed Commands:**
After renaming, you can use either name:
- Original: "Button 1" ✅
- New: "Save" ✅

**Managing Synonyms:**
1. Say "Open voice command settings"
2. Choose the app
3. Select a command
4. Add or edit synonyms

**Tips:**
- You can add multiple synonyms: "Save, Submit, Send"
- Original names always work as fallbacks
- Hints only show once per screen per session

---

## Advantages of This Approach

### 1. Non-Intrusive
✅ Never interrupts exploration
✅ User decides when to rename
✅ Hint auto-dismisses after 3 seconds

### 2. Context-Aware
✅ Shows hint only on screens with generated labels
✅ User sees button before renaming
✅ Better understanding of what button does

### 3. Flexible
✅ Rename via voice while using app
✅ Edit synonyms via settings UI
✅ Multiple synonyms per command

### 4. Backwards Compatible
✅ Original labels always work
✅ No breaking changes
✅ Synonyms are additions, not replacements

### 5. Discoverable
✅ Contextual hint teaches the feature
✅ Settings UI for advanced users
✅ Voice feedback confirms rename

---

## Comparison: Old vs New

### Old Approach (Discarded)
```
[Exploration finds unlabeled button]
System: "No label found. Assign a command?"
[Shows modal dialog - INTERRUPTS EXPLORATION]
User must decide NOW
⏸️ Exploration paused
⏸️ Disrupts workflow
⏸️ User doesn't know button context yet
```

### New Approach (This Document)
```
[Exploration completes - all buttons get fallback labels]
✅ 117/117 VUIDs created (100%)
✅ No interruptions

[User opens app]
System: [Shows brief hint at top]
"Rename buttons by saying: Rename Button 1 to Save"
[Auto-dismisses after 3 seconds]

User continues using app
User (optional): "Rename Button 1 to Save"
System: "Renamed to Save"

✅ User in control
✅ Context-aware renaming
✅ Non-intrusive
```

---

## Conclusion

This redesigned Phase 2 provides:

1. **✅ Non-intrusive exploration**: Always uses fallback labels, never pauses
2. **✅ Contextual hints**: Shows rename instruction only when needed
3. **✅ Natural voice renaming**: "Rename Button 1 to Save"
4. **✅ Synonym system**: Multiple names for same command
5. **✅ Settings UI**: Advanced synonym management
6. **✅ Backwards compatible**: Original labels always work

**Expected User Satisfaction**: Very High
- Users explore apps without interruption
- Users rename commands when they understand context
- Users have full control (voice + settings UI)
- Original names work as fallbacks

**Implementation Time**: 5 weeks
- Week 1: Rename handler
- Week 2: Hint overlay
- Week 3: Voice integration
- Week 4: Settings UI
- Week 5: Polish & testing

---

## Next Steps

1. **User Approval**: Review and approve design
2. **Week 1 Start**: Begin RenameCommandHandler implementation
3. **Parallel Work**: Design hint overlay UI mockups
4. **Testing Prep**: DeviceInfo app ready for testing

---

## References

- LearnApp-VUID-Fix-Implementation-Summary-5081220-V1.md
- LearnApp-User-Command-Assignment-Enhancement-5081220-V1.md (Phase 1 only)
- GeneratedCommandDTO schema
- VoiceOSAccessibilityService.kt

---

**End of Document**
