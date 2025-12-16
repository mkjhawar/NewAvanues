# VoiceOS Phase 3 - Version Management Enhancements
**Tasks**: UI Features (2.1-2.3) + Optimizations + Testing
**Date**: 2025-12-15
**Priority**: Production Polish & User Experience
**Estimated Time**: 4-5 days

---

## Overview

Phase 3 completes the version-aware command management system by adding:
1. **UI Features** (deferred from Phase 2): User-facing displays and controls
2. **Optimizations**: Performance improvements for large datasets
3. **Testing**: Comprehensive test coverage
4. **Documentation**: Complete API documentation

**Phase 2 Baseline**:
- ✅ Database schema v3 with version tracking
- ✅ Hash-based rescan optimization (80% skip rate)
- ✅ N+1 query optimization (97% faster)
- ✅ CleanupManager with safety checks
- ✅ PackageUpdateReceiver integration

**Phase 3 Additions**:
- UI for version info, cleanup preview, and settings
- Large database optimizations (>100k commands)
- Direct unit tests for ScreenHashCalculator
- Repository method documentation

---

## Track 1: UI Features (Tasks 2.1-2.3)

### Task 2.1: Version Info in Command List UI (4-6 hours)

#### Goal
Display app version, command version badges, and deprecation warnings in command list.

#### Files to Create
1. `CommandListUiState.kt` - UI state models
2. `CommandListScreen.kt` - Compose screen
3. `CommandVersionBadge.kt` - Reusable version badge component
4. `DeprecationWarningChip.kt` - Warning indicator component

#### Files to Modify
1. `CommandListViewModel.kt` - Add version loading logic
2. `CommandListActivity.kt` - Use new screen composable

#### Data Models

```kotlin
// CommandListUiState.kt
package com.augmentalis.voiceoscore.commands.ui

data class CommandListUiState(
    val appInfo: AppVersionInfo? = null,
    val commandGroups: List<CommandGroupUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class AppVersionInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val lastUpdated: Long,
    val totalCommands: Int,
    val deprecatedCommands: Int
) {
    val deprecationRate: Float
        get() = if (totalCommands > 0) {
            (deprecatedCommands.toFloat() / totalCommands) * 100
        } else 0f
}

data class CommandGroupUiModel(
    val packageName: String,
    val appName: String,
    val commands: List<CommandUiModel>
)

data class CommandUiModel(
    val id: Long,
    val commandText: String,
    val confidence: Double,
    val versionName: String,
    val versionCode: Long,
    val isDeprecated: Boolean,
    val isUserApproved: Boolean,
    val usageCount: Long,
    val lastUsed: Long?,
    val daysUntilDeletion: Int?  // Null if not deprecated
) {
    val confidencePercentage: Int get() = (confidence * 100).toInt()
}
```

#### ViewModel Updates

```kotlin
// CommandListViewModel.kt
class CommandListViewModel(
    private val commandRepo: IGeneratedCommandRepository,
    private val versionRepo: IAppVersionRepository,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommandListUiState())
    val uiState: StateFlow<CommandListUiState> = _uiState.asStateFlow()

    init {
        loadCommands()
    }

    private fun loadCommands() {
        viewModelScope.launch {
            try {
                _uiState.value = CommandListUiState(isLoading = true)

                // Load all commands
                val allCommands = commandRepo.getAll()

                // Group by app
                val commandsByApp = allCommands.groupBy { it.appId }

                // Load app version info for each app
                val commandGroups = commandsByApp.map { (packageName, commands) ->
                    val appName = extractAppName(packageName)
                    val appVersion = versionRepo.getVersion(packageName)

                    CommandGroupUiModel(
                        packageName = packageName,
                        appName = appName,
                        commands = commands.map { cmd ->
                            CommandUiModel(
                                id = cmd.id,
                                commandText = cmd.commandText,
                                confidence = cmd.confidence,
                                versionName = cmd.appVersion,
                                versionCode = cmd.versionCode,
                                isDeprecated = cmd.isDeprecated == 1L,
                                isUserApproved = cmd.isUserApproved == 1L,
                                usageCount = cmd.usageCount,
                                lastUsed = cmd.lastUsed,
                                daysUntilDeletion = if (cmd.isDeprecated == 1L) {
                                    calculateDaysUntilDeletion(cmd.lastVerified ?: cmd.createdAt)
                                } else null
                            )
                        }.sortedByDescending { it.usageCount }
                    )
                }.sortedByDescending { it.commands.size }

                _uiState.value = CommandListUiState(
                    commandGroups = commandGroups,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = CommandListUiState(
                    isLoading = false,
                    error = "Failed to load commands: ${e.message}"
                )
            }
        }
    }

    private fun extractAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    private fun calculateDaysUntilDeletion(lastVerified: Long): Int {
        val gracePeriodMs = 30 * 24 * 60 * 60 * 1000L // 30 days
        val deletionTimestamp = lastVerified + gracePeriodMs
        val now = System.currentTimeMillis()
        val remainingMs = deletionTimestamp - now
        return (remainingMs / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
    }
}
```

#### UI Components

```kotlin
// CommandListScreen.kt
@Composable
fun CommandListScreen(
    uiState: CommandListUiState,
    onCommandClick: (Long) -> Unit = {},
    onAppClick: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Commands") },
                colors = TopAppBarDefaults.topAppBarColors(...)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingView()
            uiState.error != null -> ErrorView(uiState.error)
            else -> CommandGroupsList(
                groups = uiState.commandGroups,
                onCommandClick = onCommandClick,
                onAppClick = onAppClick,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun CommandGroupsList(
    groups: List<CommandGroupUiModel>,
    onCommandClick: (Long) -> Unit,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        groups.forEach { group ->
            stickyHeader {
                AppHeader(
                    appName = group.appName,
                    commandCount = group.commands.size,
                    deprecatedCount = group.commands.count { it.isDeprecated },
                    onClick = { onAppClick(group.packageName) }
                )
            }

            items(group.commands) { command ->
                CommandItem(
                    command = command,
                    onClick = { onCommandClick(command.id) }
                )
            }
        }
    }
}

@Composable
private fun AppHeader(
    appName: String,
    commandCount: Int,
    deprecatedCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$commandCount commands",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (deprecatedCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "$deprecatedCount deprecated",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandItem(
    command: CommandUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (command.isDeprecated) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = command.commandText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (command.isUserApproved) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Version badge
                        CommandVersionBadge(
                            versionName = command.versionName,
                            isDeprecated = command.isDeprecated
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Confidence badge
                        ConfidenceBadge(confidence = command.confidencePercentage)

                        if (command.isUserApproved) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "User approved",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Usage count
                Text(
                    text = "${command.usageCount}×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Deprecation warning
            if (command.isDeprecated) {
                Spacer(modifier = Modifier.height(8.dp))
                DeprecationWarning(daysUntilDeletion = command.daysUntilDeletion)
            }
        }
    }
}

@Composable
private fun CommandVersionBadge(
    versionName: String,
    isDeprecated: Boolean
) {
    val backgroundColor = if (isDeprecated) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = if (isDeprecated) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = "v$versionName",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Composable
private fun ConfidenceBadge(confidence: Int) {
    val color = when {
        confidence >= 90 -> Color(0xFF4CAF50) // Green
        confidence >= 70 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = "$confidence%",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun DeprecationWarning(daysUntilDeletion: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Deprecated",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = when {
                daysUntilDeletion == null -> "Deprecated command"
                daysUntilDeletion == 0 -> "Will be deleted soon"
                daysUntilDeletion == 1 -> "Will be deleted in 1 day"
                else -> "Will be deleted in $daysUntilDeletion days"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
```

#### Success Criteria
- [ ] App version header displays at top of each group
- [ ] Version badges show on each command
- [ ] Deprecated commands show warning with days until deletion
- [ ] Different styling for deprecated vs active commands
- [ ] User-approved commands have checkmark indicator
- [ ] Confidence badges color-coded (green/orange/red)

---

### Task 2.2: Cleanup Preview Screen (Already Implemented)

**Status**: ✅ COMPLETE

**Existing Implementation**:
- CleanupPreviewActivity.kt
- CleanupPreviewViewModel.kt
- CleanupPreviewScreen.kt
- CleanupModule.kt (Hilt DI)

**Verified Components**:
- Preview state with statistics
- Safety level indicators
- Affected apps list
- Execute/Cancel buttons
- Error handling

**No work required** - proceed to Task 2.3

---

### Task 2.3: Settings Cleanup Trigger (2-3 hours)

#### Goal
Add cleanup section to Settings screen with manual trigger button.

#### Files to Create
1. `SettingsCleanupSection.kt` - Cleanup UI component

#### Files to Modify
1. `SettingsViewModel.kt` - Add cleanup tracking
2. `SettingsScreen.kt` - Add CommandManagementSection
3. `MainActivity.kt` - Wire navigation with ActivityResultLauncher

#### ViewModel Changes

```kotlin
// SettingsViewModel.kt
class SettingsViewModel(
    private val context: Context
) : ViewModel() {

    private val _lastCleanupTimestamp = MutableStateFlow<Long?>(null)
    val lastCleanupTimestamp: StateFlow<Long?> = _lastCleanupTimestamp.asStateFlow()

    private val _lastCleanupDeletedCount = MutableStateFlow(0)
    val lastCleanupDeletedCount: StateFlow<Int> = _lastCleanupDeletedCount.asStateFlow()

    init {
        loadLastCleanupInfo()
    }

    private fun loadLastCleanupInfo() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("voiceos_cleanup", Context.MODE_PRIVATE)
            _lastCleanupTimestamp.value = prefs.getLong("last_cleanup_timestamp", 0L).takeIf { it > 0 }
            _lastCleanupDeletedCount.value = prefs.getInt("last_cleanup_deleted", 0)
        }
    }

    fun updateCleanupInfo(timestamp: Long, deletedCount: Int) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("voiceos_cleanup", Context.MODE_PRIVATE)
            prefs.edit {
                putLong("last_cleanup_timestamp", timestamp)
                putInt("last_cleanup_deleted", deletedCount)
            }
            _lastCleanupTimestamp.value = timestamp
            _lastCleanupDeletedCount.value = deletedCount
        }
    }
}
```

#### UI Component

```kotlin
// SettingsCleanupSection.kt
@Composable
fun CommandManagementSection(
    lastCleanupTimestamp: Long?,
    lastCleanupDeletedCount: Int,
    onRunCleanup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Command Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Clean up deprecated commands to free storage space and improve performance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Last cleanup info
            lastCleanupTimestamp?.let { timestamp ->
                Spacer(modifier = Modifier.height(16.dp))

                val daysAgo = ((System.currentTimeMillis() - timestamp) / 86400000L).toInt()
                val lastRunText = when {
                    daysAgo == 0 -> "Today"
                    daysAgo == 1 -> "Yesterday"
                    else -> "$daysAgo days ago"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Last run: $lastRunText • Deleted $lastCleanupDeletedCount commands",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Run cleanup button
            Button(
                onClick = onRunCleanup,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Run Cleanup Now",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
```

#### Navigation Wiring

```kotlin
// MainActivity.kt (or SettingsActivity.kt)
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: SettingsViewModel

    private val cleanupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val deletedCount = result.data?.getIntExtra("deleted_count", 0) ?: 0
            val timestamp = System.currentTimeMillis()

            // UpdateViewModel
            viewModel.updateCleanupInfo(timestamp, deletedCount)

            // Show success message
            Toast.makeText(
                this,
                "Cleanup complete: $deletedCount commands deleted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setContent {
            VoiceOSTheme {
                val lastCleanupTimestamp by viewModel.lastCleanupTimestamp.collectAsState()
                val lastCleanupDeletedCount by viewModel.lastCleanupDeletedCount.collectAsState()

                SettingsScreen(
                    onRunCleanup = {
                        cleanupLauncher.launch(
                            CleanupPreviewActivity.createIntent(this)
                        )
                    },
                    lastCleanupTimestamp = lastCleanupTimestamp,
                    lastCleanupDeletedCount = lastCleanupDeletedCount
                )
            }
        }
    }
}
```

#### Success Criteria
- [ ] Settings shows "Command Management" section
- [ ] "Run Cleanup Now" button launches CleanupPreviewActivity
- [ ] Last cleanup info persists across restarts
- [ ] Settings updates after cleanup completes
- [ ] Navigation back to settings works correctly

---

## Track 2: Optimizations

### Task 3.1: Large Database Optimization (3-4 hours)

#### Goal
Optimize cleanup operations for databases with >100k commands.

#### Current Performance
- **Small DB** (1k-10k commands): <100ms cleanup
- **Medium DB** (10k-50k commands): 100-500ms cleanup
- **Large DB** (50k-100k commands): 500ms-2s cleanup
- **Very Large DB** (>100k commands): >2s cleanup ⚠️

#### Optimization Strategies

**1. Batch Deletion with Progress Callbacks**

```kotlin
// CleanupManager.kt
suspend fun executeCleanupWithProgress(
    gracePeriodDays: Int = 30,
    keepUserApproved: Boolean = true,
    batchSize: Int = 1000,
    onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
): CleanupResult = withContext(Dispatchers.Default) {
    // ... validation ...

    val preview = previewCleanup(gracePeriodDays, keepUserApproved)
    val totalToDelete = preview.commandsToDelete
    var deletedCount = 0

    // Delete in batches to avoid long transactions
    while (deletedCount < totalToDelete) {
        val batch = commandRepo.getDeprecatedCommandsForCleanup(
            packageName = "",
            olderThan = cutoffTimestamp,
            keepUserApproved = keepUserApproved,
            limit = batchSize
        )

        if (batch.isEmpty()) break

        // Delete batch
        batch.forEach { command ->
            commandRepo.deleteById(command.id)
        }

        deletedCount += batch.size
        onProgress(deletedCount, totalToDelete)

        // Yield to allow UI updates
        yield()
    }

    // ... return result ...
}
```

**2. Index Optimization**

```sql
-- GeneratedCommand.sq
-- Add composite index for cleanup queries
CREATE INDEX IF NOT EXISTS idx_gc_cleanup
ON commands_generated(isDeprecated, lastVerified, isUserApproved);

-- Add index for package-based cleanup
CREATE INDEX IF NOT EXISTS idx_gc_package_deprecated
ON commands_generated(appId, isDeprecated, lastVerified);
```

**3. Vacuum After Large Deletions**

```kotlin
// CleanupManager.kt
private suspend fun vacuumDatabase() = withContext(Dispatchers.IO) {
    try {
        // SQLite VACUUM to reclaim space
        databaseManager.database.executeRaw("VACUUM")
    } catch (e: Exception) {
        Log.w(TAG, "Failed to vacuum database: ${e.message}")
    }
}

suspend fun executeCleanup(...): CleanupResult {
    // ... existing cleanup code ...

    // Vacuum if deleted >10% of database
    if (deletedCount > totalCommands * 0.1) {
        vacuumDatabase()
    }

    // ... return result ...
}
```

#### Success Criteria
- [ ] Large DB (>100k commands) cleanup <2s
- [ ] Batch deletion with progress callbacks working
- [ ] Composite indexes improve query performance 3-5x
- [ ] VACUUM reclaims disk space after large deletions

---

### Task 3.2: ScreenHashCalculator Unit Tests (1 hour)

#### Goal
Add direct unit tests for hash calculation edge cases.

#### Test File
```kotlin
// ScreenHashCalculatorTest.kt
package com.augmentalis.voiceoscore.version

import com.augmentalis.database.dto.ScrapedElementDTO
import org.junit.Assert.*
import org.junit.Test

class ScreenHashCalculatorTest {

    @Test
    fun calculateScreenHash_emptyList_returnsEmptyString() {
        val hash = ScreenHashCalculator.calculateScreenHash(emptyList())
        assertEquals("", hash)
    }

    @Test
    fun calculateScreenHash_sameElements_produceSameHash() {
        val elements1 = createTestElements(5)
        val elements2 = createTestElements(5)

        val hash1 = ScreenHashCalculator.calculateScreenHash(elements1)
        val hash2 = ScreenHashCalculator.calculateScreenHash(elements2)

        assertEquals(hash1, hash2)
    }

    @Test
    fun calculateScreenHash_sameElementsDifferentOrder_produceSameHash() {
        val elements1 = createTestElements(5)
        val elements2 = elements1.reversed()

        val hash1 = ScreenHashCalculator.calculateScreenHash(elements1)
        val hash2 = ScreenHashCalculator.calculateScreenHash(elements2)

        assertEquals("Hash should be same regardless of element order", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_differentBounds_producesDifferentHash() {
        val element1 = createTestElement(id = "btn1", bounds = "0,0,100,50")
        val element2 = createTestElement(id = "btn1", bounds = "0,0,100,51") // 1px different

        val hash1 = ScreenHashCalculator.calculateScreenHash(listOf(element1))
        val hash2 = ScreenHashCalculator.calculateScreenHash(listOf(element2))

        assertNotEquals("Different bounds should produce different hash", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_differentText_producesSameHash() {
        val element1 = createTestElement(id = "btn1", text = "Submit")
        val element2 = createTestElement(id = "btn1", text = "Done")

        val hash1 = ScreenHashCalculator.calculateScreenHash(listOf(element1))
        val hash2 = ScreenHashCalculator.calculateScreenHash(listOf(element2))

        assertEquals("Text differences should not affect hash", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_addElement_producesDifferentHash() {
        val elements1 = createTestElements(5)
        val elements2 = elements1 + createTestElement(id = "newBtn", bounds = "200,200,300,250")

        val hash1 = ScreenHashCalculator.calculateScreenHash(elements1)
        val hash2 = ScreenHashCalculator.calculateScreenHash(elements2)

        assertNotEquals("Adding element should change hash", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_removeElement_producesDifferentHash() {
        val elements1 = createTestElements(5)
        val elements2 = elements1.dropLast(1)

        val hash1 = ScreenHashCalculator.calculateScreenHash(elements1)
        val hash2 = ScreenHashCalculator.calculateScreenHash(elements2)

        assertNotEquals("Removing element should change hash", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_returns64CharHex() {
        val elements = createTestElements(10)
        val hash = ScreenHashCalculator.calculateScreenHash(elements)

        assertEquals("SHA-256 hash should be 64 hex characters", 64, hash.length)
        assertTrue("Hash should contain only hex characters", hash.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun calculateScreenHash_collisionProbability_isNegligible() {
        // Generate 10,000 different screen configurations
        val hashes = (1..10000).map { count ->
            ScreenHashCalculator.calculateScreenHash(createTestElements(count % 100 + 1))
        }.toSet()

        // All hashes should be unique (no collisions)
        assertEquals("No collisions expected for 10k screens", 10000, hashes.size)
    }

    // Helper functions
    private fun createTestElements(count: Int): List<ScrapedElementDTO> {
        return (1..count).map { i ->
            createTestElement(
                id = "element$i",
                className = "Button",
                bounds = "${i * 10},${i * 10},${i * 10 + 100},${i * 10 + 50}"
            )
        }
    }

    private fun createTestElement(
        id: String = "testElement",
        className: String = "Button",
        bounds: String = "0,0,100,50",
        text: String = "Test"
    ): ScrapedElementDTO {
        return ScrapedElementDTO(
            id = 0L,
            elementHash = id,
            appId = "com.test.app",
            uuid = "uuid-$id",
            className = className,
            viewIdResourceName = id,
            text = text,
            contentDescription = null,
            bounds = bounds,
            isClickable = 1L,
            isLongClickable = 0L,
            isEditable = 0L,
            isScrollable = 0L,
            isCheckable = 0L,
            isFocusable = 1L,
            isEnabled = 1L,
            depth = 2L,
            indexInParent = 0L,
            scrapedAt = System.currentTimeMillis(),
            semanticRole = null,
            inputType = null,
            visualWeight = null,
            isRequired = null,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = null
        )
    }
}
```

#### Success Criteria
- [ ] 9 test cases all passing
- [ ] Hash stability verified (same input → same output)
- [ ] Order independence verified
- [ ] Structural changes detected (bounds, add/remove)
- [ ] Non-structural changes ignored (text)
- [ ] No collisions in 10k test cases

---

### Task 3.3: Repository Method Documentation (15 minutes)

#### Goal
Document unused repository methods to prevent confusion.

#### Files to Modify
- `IGeneratedCommandRepository.kt`

#### Documentation Updates

```kotlin
/**
 * Get active commands by version string instead of version code (Task 1.3).
 *
 * **STATUS:** Implemented for Phase 3, not yet used in production.
 * **PLANNED USE:** Memory optimization for JIT learning to filter commands
 * at database level instead of loading all and filtering in Kotlin.
 *
 * **Performance:** Reduces memory usage by 60-80% for large apps by
 * filtering deprecated/wrong-version commands at DB level.
 *
 * **See:** VoiceOS-Plan-VersionManagement-P3-Enhancements-5141217-V1.md
 *
 * @param packageName App package name
 * @param appVersion App version string (e.g., "8.2024.11.123")
 * @param limit Maximum number of commands to return (default: 1000)
 * @return List of active commands for the specified version, sorted by usage
 */
suspend fun getActiveCommandsByVersion(
    packageName: String,
    appVersion: String,
    limit: Int = 1000
): List<GeneratedCommandDTO>

/**
 * Update command version information after verification.
 *
 * **STATUS:** Implemented but primarily used in tests.
 * **PRODUCTION USE:** Reserved for manual version corrections or
 * administrative tools. Not part of normal command lifecycle.
 *
 * **Use Cases:**
 * - Bulk version updates after database migration
 * - Manual correction of misattributed commands
 * - Testing version-aware logic
 *
 * @param id Command ID
 * @param versionCode New version code
 * @param appVersion New version string (e.g., "8.2024.11.123")
 * @param lastVerified Timestamp of verification
 * @param isDeprecated Whether command is deprecated (0=active, 1=deprecated)
 */
suspend fun updateCommandVersion(
    id: Long,
    versionCode: Long,
    appVersion: String,
    lastVerified: Long,
    isDeprecated: Long
)
```

#### Success Criteria
- [ ] Documentation added to both methods
- [ ] Status clearly indicated
- [ ] Planned use cases documented
- [ ] Reference to Phase 3 plan included

---

## Implementation Timeline

### Day 1: UI Foundation (6-8 hours)
**Morning (4 hours):**
- Create CommandListUiState.kt with data models
- Create CommandListViewModel.kt with business logic
- Write unit tests for ViewModel

**Afternoon (3-4 hours):**
- Create CommandListScreen.kt with Compose UI
- Create reusable components (badges, warnings)
- Manual UI testing

### Day 2: Settings Integration (4-5 hours)
**Morning (2-3 hours):**
- Update SettingsViewModel with cleanup tracking
- Create SettingsCleanupSection component
- Wire navigation in MainActivity

**Afternoon (2 hours):**
- End-to-end testing of Settings → Cleanup flow
- Verify cleanup result propagation
- Fix any navigation issues

### Day 3: Optimizations (4-5 hours)
**Morning (3-4 hours):**
- Implement batch deletion with progress
- Add composite indexes to schema
- Add VACUUM after large deletions

**Afternoon (1 hour):**
- Performance testing with large datasets
- Benchmark improvements

### Day 4: Testing & Documentation (3-4 hours)
**Morning (2 hours):**
- Write ScreenHashCalculator unit tests (9 test cases)
- Run tests and fix any failures

**Afternoon (1-2 hours):**
- Document repository methods
- Update Phase 3 action items with completion status
- Final code review

### Day 5: Polish & Validation (2-3 hours)
**Morning (2 hours):**
- UI polish (colors, spacing, accessibility)
- Fix any UI bugs from testing
- Verify all success criteria met

**Afternoon (1 hour):**
- Create Phase 3 completion report
- Update documentation
- Final testing pass

---

## Testing Strategy

### Unit Tests
- **ViewModel Tests**: CommandListViewModelTest (6 tests)
- **Hash Tests**: ScreenHashCalculatorTest (9 tests)

### Integration Tests
- **UI Flow**: Settings → Cleanup → Result propagation
- **Large DB**: Cleanup with 100k+ commands
- **Batch Progress**: Progress callbacks during cleanup

### UI Tests
- **Command List**: Version display, deprecation warnings
- **Settings**: Cleanup section, last run info
- **Cleanup Preview**: Navigation, result handling

---

## Success Metrics

### Performance
- [ ] Large DB cleanup <2s (>100k commands)
- [ ] Batch deletion with progress working
- [ ] Index optimization 3-5x faster queries
- [ ] VACUUM reduces DB size by 20-40% after large deletions

### UI/UX
- [ ] Version info visible in command list
- [ ] Deprecation warnings clear and actionable
- [ ] Settings cleanup section intuitive
- [ ] Cleanup flow seamless (Settings → Preview → Execute → Back)

### Code Quality
- [ ] 15 new unit tests (6 ViewModel + 9 Hash)
- [ ] Test coverage >90% for new code
- [ ] All documentation complete
- [ ] No compilation warnings

### User Value
- [ ] Users can see command versions at a glance
- [ ] Deprecated commands clearly marked
- [ ] One-tap cleanup from settings
- [ ] Last cleanup info persisted and displayed

---

## Rollback Plan

### If UI Tasks Fail
- Revert to headless operation (cleanup via background jobs only)
- No impact on core functionality

### If Optimizations Fail
- Fall back to original cleanup implementation
- Performance degraded but functional

### If Tests Fail
- Fix tests incrementally
- Block merge until all tests pass

---

## Dependencies

**Required**:
- Phase 2 complete (database schema v3, CleanupManager, etc.)
- Hilt DI configured
- Compose UI dependencies

**Optional**:
- Performance monitoring tools (for benchmarking)
- Large test database (for optimization validation)

---

## Key Files Reference

### Task 2.1 (Command List UI)
```
CommandListUiState.kt         - Create (data models)
CommandListViewModel.kt       - Modify (add version loading)
CommandListScreen.kt          - Create (Compose UI)
CommandVersionBadge.kt        - Create (reusable component)
DeprecationWarningChip.kt     - Create (reusable component)
CommandListActivity.kt        - Modify (use new screen)
```

### Task 2.3 (Settings Integration)
```
SettingsCleanupSection.kt     - Create (UI component)
SettingsViewModel.kt          - Modify (add cleanup tracking)
SettingsScreen.kt             - Modify (add section)
MainActivity.kt               - Modify (wire navigation)
```

### Task 3.1 (Optimizations)
```
CleanupManager.kt             - Modify (batch deletion, VACUUM)
GeneratedCommand.sq           - Modify (add indexes)
VoiceOSDatabaseManager.kt     - Modify (VACUUM support)
```

### Task 3.2 (Tests)
```
ScreenHashCalculatorTest.kt   - Create (9 test cases)
CommandListViewModelTest.kt   - Create (6 test cases)
```

### Task 3.3 (Documentation)
```
IGeneratedCommandRepository.kt - Modify (add docs)
```

---

**Plan Status**: READY FOR IMPLEMENTATION
**Next Step**: Begin Task 2.1 (Command List UI) - Create data models

---

**Version**: V1
**Date**: 2025-12-15
**Author**: VOS4 Development Team
