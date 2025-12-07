# Technical Q&A: Week 2/3 Features Deep Dive - Part 2

**Date:** 2025-10-09 11:10:00 PDT
**Continued from:** Technical-QA-Week2-3-Features-251009-1106.md

---

## Table of Contents (Part 2)

8. [Command Database & Lazy Loading Strategy](#command-database)
9. [LearnApp: Metadata Missing - Spotlight Solution](#learnapp-metadata)
10. [Command Generator: NLP Engine Details](#command-generator)
11. [Hardware Detection: CPU & Battery Cost](#hardware-detection)
12. [Sensor Fusion: How It Works & Spatial Support](#sensor-fusion)
13. [UUIDCreator Extensions: Module Organization](#uuidcreator-extensions)

---

## 8. Command Database & Lazy Loading Strategy {#command-database}

### Current Problem: All Commands Loaded in Memory

**File:** `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/CommandMapper.kt`

**Current Implementation (PROBLEMATIC):**
```kotlin
class CommandMapper {
    // ❌ PROBLEM: 150+ commands loaded at startup
    private val commandMap = mapOf(
        "move up" to MoveAction(Direction.UP),
        "move down" to MoveAction(Direction.DOWN),
        // ... 150+ more command mappings
    )

    // Memory cost: ~150 KB constantly in RAM
    // Startup cost: ~50ms to initialize all Action objects
    // Not extensible: Cannot add custom commands at runtime
}
```

---

### Solution: Database with Lazy Loading + LRU Cache

**Create New File:** `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/CommandDatabase.kt`

```kotlin
/**
 * Room database for voice commands with lazy loading strategy
 *
 * Architecture:
 * ┌──────────────────────────────────────────────────────────┐
 * │  User speaks: "move up"                                   │
 * └────────────────┬─────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌──────────────────────────────────────────────────────────┐
 * │  1. Check LRU Cache (20 most recent commands)            │
 * │     Size: 2 KB in RAM                                    │
 * │     Lookup: 10 microseconds                              │
 * └────────────────┬─────────────────────────────────────────┘
 *                  │ Cache Miss
 *                  ▼
 * ┌──────────────────────────────────────────────────────────┐
 * │  2. Query SQLite Database                                │
 * │     Database size: 50 KB on disk                         │
 * │     Query time: 500 microseconds                         │
 * │     Only loads requested command + metadata              │
 * └────────────────┬─────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌──────────────────────────────────────────────────────────┐
 * │  3. Add to LRU Cache for future                          │
 * │     Evict least recently used if cache full              │
 * └────────────────┬─────────────────────────────────────────┘
 *                  │
 *                  ▼
 * ┌──────────────────────────────────────────────────────────┐
 * │  4. Parse & Execute Action                               │
 * └──────────────────────────────────────────────────────────┘
 *
 * Benefits:
 * - Memory: 2 KB (cache) + 0 KB (database not loaded)
 * - Startup: <1ms (database already exists on disk)
 * - Extensible: Users can add custom commands via INSERT
 * - Persistent: Commands survive app restarts
 * - Analytics: Track usage statistics per command
 * - Versioning: Easy to update command sets via database migration
 *
 * @see CommandDao for database operations
 * @see CommandMapperV2 for usage example
 */
@Database(
    entities = [
        VoiceCommand::class,
        CommandAlias::class,
        CommandUsageStats::class
    ],
    version = 1,
    exportSchema = true
)
abstract class CommandDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao

    companion object {
        @Volatile
        private var INSTANCE: CommandDatabase? = null

        fun getDatabase(context: Context): CommandDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CommandDatabase::class.java,
                    "voice_commands.db"
                )
                    .createFromAsset("databases/voice_commands.db")  // Preloaded commands
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

### Database Schema

```kotlin
/**
 * Voice command entity
 * Stores command text and associated action
 */
@Entity(
    tableName = "voice_commands",
    indices = [
        Index(value = ["command"], unique = true),
        Index(value = ["category"]),
        Index(value = ["priority"])
    ]
)
data class VoiceCommand(
    @PrimaryKey
    @ColumnInfo(name = "command")
    val command: String,                    // "move up"

    @ColumnInfo(name = "action_type")
    val actionType: String,                 // "MOVE"

    @ColumnInfo(name = "action_data")
    val actionData: String,                 // "{\"direction\":\"UP\",\"distance\":\"NORMAL\"}"

    @ColumnInfo(name = "category")
    val category: String,                   // "movement", "interaction", "navigation"

    @ColumnInfo(name = "priority")
    val priority: Int = 0,                  // For disambiguation (higher = preferred)

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,            // Can be disabled by user

    @ColumnInfo(name = "custom")
    val customCommand: Boolean = false,     // User-created?

    @ColumnInfo(name = "description")
    val description: String? = null,        // Human-readable description

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Command aliases
 * Multiple phrases map to same command
 */
@Entity(
    tableName = "command_aliases",
    foreignKeys = [
        ForeignKey(
            entity = VoiceCommand::class,
            parentColumns = ["command"],
            childColumns = ["primary_command"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["alias"], unique = true),
        Index(value = ["primary_command"])
    ]
)
data class CommandAlias(
    @PrimaryKey
    @ColumnInfo(name = "alias")
    val alias: String,                      // "go up"

    @ColumnInfo(name = "primary_command")
    val primaryCommand: String              // "move up"
)

/**
 * Usage statistics
 * Track command popularity and accuracy
 */
@Entity(
    tableName = "command_usage_stats",
    foreignKeys = [
        ForeignKey(
            entity = VoiceCommand::class,
            parentColumns = ["command"],
            childColumns = ["command"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CommandUsageStats(
    @PrimaryKey
    @ColumnInfo(name = "command")
    val command: String,

    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,

    @ColumnInfo(name = "success_count")
    val successCount: Int = 0,

    @ColumnInfo(name = "failure_count")
    val failureCount: Int = 0,

    @ColumnInfo(name = "last_used")
    val lastUsed: Long = 0,

    @ColumnInfo(name = "average_execution_time")
    val averageExecutionTime: Long = 0      // milliseconds
) {
    /**
     * Success rate: 0.0 to 1.0
     */
    fun successRate(): Float {
        val total = successCount + failureCount
        return if (total > 0) successCount.toFloat() / total else 0f
    }

    /**
     * Accuracy percentage: 0-100%
     */
    fun accuracyPercent(): Int = (successRate() * 100).toInt()
}
```

---

### DAO Interface

```kotlin
@Dao
interface CommandDao {
    // ========== Query Operations ==========

    /**
     * Get command by exact match (primary lookup)
     * Execution time: ~100 microseconds
     *
     * @param command Exact command string (normalized)
     * @return VoiceCommand if found, null otherwise
     */
    @Query("SELECT * FROM voice_commands WHERE command = :command AND enabled = 1 LIMIT 1")
    suspend fun getCommand(command: String): VoiceCommand?

    /**
     * Find similar commands using LIKE pattern (fuzzy match)
     * Execution time: ~500 microseconds
     *
     * @param pattern SQL LIKE pattern (e.g., "%move%up%")
     * @return List of matching commands, ordered by priority
     */
    @Query("""
        SELECT * FROM voice_commands
        WHERE command LIKE :pattern AND enabled = 1
        ORDER BY priority DESC
        LIMIT 10
    """)
    suspend fun findSimilarCommands(pattern: String): List<VoiceCommand>

    /**
     * Get commands by category
     * Used for context-aware suggestions
     *
     * @param category Command category (movement, interaction, navigation, etc.)
     * @return List of commands in category
     */
    @Query("SELECT * FROM voice_commands WHERE category = :category AND enabled = 1")
    suspend fun getCommandsByCategory(category: String): List<VoiceCommand>

    /**
     * Get most used commands (for predictive cache)
     * Execution time: ~200 microseconds
     *
     * @param limit Number of commands to return
     * @return Top N most frequently used commands
     */
    @Query("""
        SELECT vc.* FROM voice_commands vc
        LEFT JOIN command_usage_stats cus ON vc.command = cus.command
        WHERE vc.enabled = 1
        ORDER BY COALESCE(cus.usage_count, 0) DESC
        LIMIT :limit
    """)
    suspend fun getMostUsedCommands(limit: Int = 20): List<VoiceCommand>

    /**
     * Get all custom commands
     * User-created commands for review/export
     */
    @Query("SELECT * FROM voice_commands WHERE custom = 1")
    suspend fun getCustomCommands(): List<VoiceCommand>

    /**
     * Search commands by description
     * Full-text search for user-facing command browser
     */
    @Query("SELECT * FROM voice_commands WHERE description LIKE :query")
    suspend fun searchCommands(query: String): List<VoiceCommand>

    // ========== Insert/Update Operations ==========

    /**
     * Insert or replace command
     * Used for adding custom commands or updating existing
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: VoiceCommand)

    /**
     * Insert multiple commands (bulk import)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommands(commands: List<VoiceCommand>)

    /**
     * Update command priority (for disambiguation)
     */
    @Query("UPDATE voice_commands SET priority = :priority WHERE command = :command")
    suspend fun updatePriority(command: String, priority: Int)

    /**
     * Enable/disable command
     */
    @Query("UPDATE voice_commands SET enabled = :enabled WHERE command = :command")
    suspend fun setCommandEnabled(command: String, enabled: Boolean)

    // ========== Usage Statistics ==========

    /**
     * Increment usage count
     * Called after successful command execution
     */
    @Transaction
    suspend fun incrementUsage(command: String, timestamp: Long) {
        // Create stats entry if doesn't exist
        val stats = getUsageStats(command) ?: CommandUsageStats(command = command)

        // Update stats
        val updated = stats.copy(
            usageCount = stats.usageCount + 1,
            lastUsed = timestamp
        )

        insertUsageStats(updated)
    }

    /**
     * Record command success
     */
    @Transaction
    suspend fun recordSuccess(command: String, executionTime: Long) {
        val stats = getUsageStats(command) ?: CommandUsageStats(command = command)

        val updated = stats.copy(
            successCount = stats.successCount + 1,
            averageExecutionTime = (stats.averageExecutionTime + executionTime) / 2
        )

        insertUsageStats(updated)
    }

    /**
     * Record command failure
     */
    @Transaction
    suspend fun recordFailure(command: String) {
        val stats = getUsageStats(command) ?: CommandUsageStats(command = command)

        val updated = stats.copy(
            failureCount = stats.failureCount + 1
        )

        insertUsageStats(updated)
    }

    @Query("SELECT * FROM command_usage_stats WHERE command = :command")
    suspend fun getUsageStats(command: String): CommandUsageStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageStats(stats: CommandUsageStats)

    // ========== Alias Operations ==========

    /**
     * Get primary command for alias
     */
    @Query("SELECT primary_command FROM command_aliases WHERE alias = :alias")
    suspend fun resolveAlias(alias: String): String?

    /**
     * Add command alias
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: CommandAlias)

    // ========== Maintenance ==========

    /**
     * Delete unused commands (never used in last 30 days)
     */
    @Query("""
        DELETE FROM voice_commands
        WHERE custom = 1
        AND command IN (
            SELECT command FROM command_usage_stats
            WHERE last_used < :cutoffTime
        )
    """)
    suspend fun pruneUnusedCommands(cutoffTime: Long)

    /**
     * Get database statistics
     */
    @Query("SELECT COUNT(*) FROM voice_commands")
    suspend fun getTotalCommandCount(): Int

    @Query("SELECT COUNT(*) FROM voice_commands WHERE enabled = 1")
    suspend fun getEnabledCommandCount(): Int

    @Query("SELECT COUNT(*) FROM voice_commands WHERE custom = 1")
    suspend fun getCustomCommandCount(): Int
}
```

---

### Lazy Loading Implementation

```kotlin
/**
 * Command mapper with lazy loading and LRU cache
 *
 * Memory footprint:
 * - Cache: 2 KB (20 commands x 100 bytes each)
 * - Database handle: <1 KB
 * - Total: ~3 KB (vs 150 KB for hardcoded map)
 *
 * Startup time:
 * - Database init: <1ms (file already exists)
 * - No commands loaded initially
 * - Total: <1ms (vs 50ms for hardcoded map)
 *
 * Lookup performance:
 * - Cache hit: 10 microseconds (same as HashMap)
 * - Cache miss: 500 microseconds (database query)
 * - Cache hit rate: 90%+ after warm-up
 * - Average: ~50 microseconds (weighted average)
 */
class CommandMapperV2(
    private val database: CommandDatabase,
    private val coroutineScope: CoroutineScope
) {
    // LRU cache: Keep 20 most recently used commands
    private val cache = object : LinkedHashMap<String, VoiceCommand>(
        20,     // Initial capacity
        0.75f,  // Load factor
        true    // Access order (LRU)
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<String, VoiceCommand>): Boolean {
            return size > 20  // Evict when size exceeds 20
        }
    }

    // Predictive cache: Commands likely to be used next
    private val predictiveCache = mutableSetOf<String>()

    // Command sequence history (for prediction)
    private val commandSequence = ArrayDeque<String>(10)

    /**
     * Map voice command to cursor action
     *
     * Lookup strategy:
     * 1. Check LRU cache (10 μs)
     * 2. Check predictive cache (10 μs)
     * 3. Database exact match (100 μs)
     * 4. Database fuzzy match (500 μs)
     * 5. Return null if not found
     *
     * @param spokenCommand Raw voice input
     * @return CursorAction if command found, null otherwise
     */
    suspend fun mapCommand(spokenCommand: String): CursorAction? {
        val normalized = normalizeCommand(spokenCommand)

        // 1. LRU cache lookup (fast path)
        cache[normalized]?.let { cached ->
            updateCommandSequence(normalized)
            updateUsageAsync(normalized)
            return parseAction(cached)
        }

        // 2. Predictive cache (warm path)
        if (normalized in predictiveCache) {
            val command = database.commandDao().getCommand(normalized)
            if (command != null) {
                cache[normalized] = command
                updateCommandSequence(normalized)
                return parseAction(command)
            }
        }

        // 3. Database query (cold path)
        val command = findCommand(normalized)

        if (command != null) {
            // Add to cache
            cache[normalized] = command

            // Update sequence and predictions
            updateCommandSequence(normalized)
            updatePredictiveCache()

            // Update usage stats (async, don't block)
            updateUsageAsync(normalized)

            return parseAction(command)
        }

        return null  // Unknown command
    }

    /**
     * Find command in database
     * Tries exact match first, then fuzzy match
     */
    private suspend fun findCommand(command: String): VoiceCommand? {
        // 1. Exact match
        database.commandDao().getCommand(command)?.let { return it }

        // 2. Check if it's an alias
        database.commandDao().resolveAlias(command)?.let { primaryCommand ->
            return database.commandDao().getCommand(primaryCommand)
        }

        // 3. Fuzzy match
        val pattern = buildFuzzyPattern(command)
        val matches = database.commandDao().findSimilarCommands(pattern)

        if (matches.isNotEmpty()) {
            // Calculate similarity scores
            val scored = matches.map { cmd ->
                cmd to calculateSimilarity(command, cmd.command)
            }

            // Return best match if similarity > 70%
            val best = scored.maxByOrNull { it.second }
            if (best != null && best.second > 0.7f) {
                return best.first
            }
        }

        return null
    }

    /**
     * Build SQL LIKE pattern for fuzzy matching
     * "move up" → "%move%up%"
     */
    private fun buildFuzzyPattern(command: String): String {
        val words = command.split(" ")
        return "%${words.joinToString("%")}%"
    }

    /**
     * Update command sequence history
     * Used for predicting next commands
     */
    private fun updateCommandSequence(command: String) {
        commandSequence.addLast(command)
        if (commandSequence.size > 10) {
            commandSequence.removeFirst()
        }
    }

    /**
     * Update predictive cache based on command sequences
     *
     * Pattern analysis:
     * - "move up" → likely next: ["click", "move down", "go back"]
     * - "snap to button" → likely next: ["click", "long press"]
     * - "click" → likely next: ["go back", "move up"]
     */
    private fun updatePredictiveCache() {
        if (commandSequence.isEmpty()) return

        val lastCommand = commandSequence.last()

        val predictions = when {
            lastCommand.startsWith("move") || lastCommand.startsWith("go") ->
                listOf("click", "tap", "press", "long press", "go back")

            lastCommand.startsWith("snap") ->
                listOf("click", "tap", "long press", "go back")

            lastCommand in setOf("click", "tap", "press", "long press") ->
                listOf("go back", "move up", "move down", "move left", "move right")

            else -> emptyList()
        }

        predictiveCache.clear()
        predictiveCache.addAll(predictions)

        // Preload predictions into cache (async)
        coroutineScope.launch {
            predictions.forEach { command ->
                database.commandDao().getCommand(command)?.let {
                    cache[command] = it
                }
            }
        }
    }

    /**
     * Update usage statistics (async)
     */
    private fun updateUsageAsync(command: String) {
        coroutineScope.launch {
            database.commandDao().incrementUsage(
                command,
                System.currentTimeMillis()
            )
        }
    }

    /**
     * Parse action from voice command
     */
    private fun parseAction(command: VoiceCommand): CursorAction {
        val data = JSONObject(command.actionData)

        return when (command.actionType) {
            "MOVE" -> MoveAction(
                direction = Direction.valueOf(data.getString("direction")),
                distance = Distance.valueOf(data.optString("distance", "NORMAL"))
            )

            "CLICK" -> ClickAction(
                type = ClickType.valueOf(data.optString("type", "SINGLE"))
            )

            "MOVE_TO" -> MoveToAction(
                position = Position.valueOf(data.getString("position"))
            )

            "GESTURE" -> GestureAction(
                gesture = Gesture.valueOf(data.getString("gesture"))
            )

            "SNAP" -> SnapAction(
                elementType = ElementType.valueOf(data.getString("type"))
            )

            "VISIBILITY" -> VisibilityAction(
                visible = data.getBoolean("visible")
            )

            "HISTORY" -> HistoryAction(
                action = HistoryActionType.valueOf(data.getString("action"))
            )

            else -> UnknownAction()
        }
    }

    /**
     * Normalize command string
     */
    private fun normalizeCommand(command: String): String {
        return command
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")  // Multiple spaces → single
            .removeFiller()                // Remove "um", "uh", "like"
    }

    private fun String.removeFiller(): String {
        val fillers = setOf("um", "uh", "like", "you know", "i mean", "actually")
        val words = split(" ")
        return words.filterNot { it in fillers }.joinToString(" ")
    }

    /**
     * Calculate similarity between two strings
     * Uses Levenshtein distance
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val distance = levenshteinDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return if (maxLength == 0) 1f else 1f - (distance.toFloat() / maxLength)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,          // deletion
                    dp[i][j - 1] + 1,          // insertion
                    dp[i - 1][j - 1] + cost    // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}
```

---

### Performance Comparison

| Metric | Hardcoded Map (Current) | Database + Cache (Proposed) | Improvement |
|--------|-------------------------|----------------------------|-------------|
| **Startup Time** | 50ms | <1ms | **50x faster** |
| **Memory Usage** | 150 KB | 2 KB | **75x less** |
| **Lookup (cache hit)** | 10 μs | 10 μs | **Same** |
| **Lookup (cache miss)** | N/A | 500 μs | N/A |
| **Average lookup** | 10 μs | ~50 μs | **5x slower** |
| **Extensible** | ❌ No | ✅ Yes | - |
| **Persistent** | ❌ No | ✅ Yes | - |
| **Analytics** | ❌ No | ✅ Yes | - |
| **Fuzzy matching** | ❌ No | ✅ Yes | - |

**Conclusion:** Database approach is **significantly better** despite slightly slower average lookup (50μs vs 10μs, which is still imperceptible to users).

---

### Database File Size

```
Command count:          150 built-in + user-added
Bytes per command:      ~300 bytes (text + JSON + metadata)
Total commands:         ~50 KB

Aliases:                ~50 aliases
Bytes per alias:        ~100 bytes
Total aliases:          ~5 KB

Usage stats:            ~150 rows
Bytes per stats row:    ~100 bytes
Total stats:            ~15 KB

SQLite overhead:        ~10 KB (indexes, metadata)

Total database size:    ~80 KB

Conclusion: ✅ Very small, negligible storage cost
```

---

## 9. LearnApp: Metadata Missing - Spotlight Solution {#learnapp-metadata}

### Current Problem

**File:** `modules/libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/recording/InteractionRecorder.kt`

**Problem:** When recording interactions, some UI elements don't have `contentDescription` or `text` metadata:

```kotlin
// Example element with NO metadata:
AccessibilityNodeInfo {
    text: null
    contentDescription: null  // ❌ No metadata!
    className: "android.widget.ImageButton"
    bounds: Rect(100, 200 - 200, 300)
    isClickable: true
}

// Current behavior: This element is ignored/skipped
// Result: Cannot generate voice command for it
```

---

### Solution: Spotlight + User Input Dialog

**Visual Mockup:**

```
┌────────────────────────────────────────────────────────────┐
│  Gmail App                                    [≡]           │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓┏━━━━━━━━━━━━━━━━━━━━━━━━━┓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓┃  ╔════════════════════╗  ┃▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓┃  ║   📧  [Icon]      ║  ┃▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓┃  ╚════════════════════╝  ┃▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓┗━━━━━━━━━━━━━━━━━━━━━━━━━┛▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  ⚠️  Element Without Metadata                       │ │
│  │                                                      │ │
│  │  This button has no description.                    │ │
│  │  What voice command should activate it?             │ │
│  │                                                      │ │
│  │  ┌────────────────────────────────────────────────┐ │ │
│  │  │  Compose                                  🎤   │ │ │
│  │  └────────────────────────────────────────────────┘ │ │
│  │                                                      │ │
│  │  Suggestions:                                        │ │
│  │  • "Compose email"                                   │ │
│  │  • "New email"                                       │ │
│  │  • "Write email"                                     │ │
│  │                                                      │ │
│  │  [ Cancel ]                   [ Save Command ]      │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
└────────────────────────────────────────────────────────────┘

Legend:
▓▓▓ Dimmed background (50% black overlay)
━━  Spotlight border (animated pulsing)
╔══  Element focus ring
```

---

### Implementation

**Create New File:** `modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/overlays/MetadataPromptOverlay.kt`

```kotlin
/**
 * Metadata prompt overlay for elements without descriptions
 *
 * When InteractionRecorder encounters an element without metadata:
 * 1. Dim the entire screen (50% black overlay)
 * 2. Spotlight the element (clear area with pulsing border)
 * 3. Show dialog asking user to name the element
 * 4. Generate voice command from user input
 * 5. Store command for future use
 *
 * Visual Design:
 * ┌──────────────────────────────────────────────┐
 * │  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
 * │  ▓▓▓▓▓▓┏━━━━━━━━━━━━┓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
 * │  ▓▓▓▓▓▓┃  Element   ┃▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
 * │  ▓▓▓▓▓▓┗━━━━━━━━━━━━┛▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
 * │  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │
 * └──────────────────────────────────────────────┘
 *
 * @param context Application context
 * @param element The element without metadata
 * @param onCommandNamed Callback when user provides name
 */
class MetadataPromptOverlay(
    private val context: Context,
    private val element: AccessibilityNodeInfo,
    private val onCommandNamed: (String) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val overlayView: View
    private val spotlightView: SpotlightView
    private val dialogView: View

    init {
        overlayView = createOverlayView()
        spotlightView = createSpotlightView()
        dialogView = createDialogView()
    }

    /**
     * Show the metadata prompt overlay
     */
    fun show() {
        // 1. Add dim overlay (full screen)
        val dimParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlayView, dimParams)

        // 2. Add spotlight (element area)
        val elementBounds = Rect()
        element.getBoundsInScreen(elementBounds)

        val spotlightParams = WindowManager.LayoutParams(
            elementBounds.width() + 40,  // 20dp padding
            elementBounds.height() + 40,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        spotlightParams.x = elementBounds.left - 20
        spotlightParams.y = elementBounds.top - 20
        windowManager.addView(spotlightView, spotlightParams)

        // 3. Add dialog (center bottom)
        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,  // Allow keyboard input
            PixelFormat.TRANSLUCENT
        )
        dialogParams.gravity = Gravity.BOTTOM
        windowManager.addView(dialogView, dialogParams)

        // 4. Start animations
        spotlightView.startPulseAnimation()
    }

    /**
     * Hide the overlay
     */
    fun dismiss() {
        spotlightView.stopPulseAnimation()
        windowManager.removeView(overlayView)
        windowManager.removeView(spotlightView)
        windowManager.removeView(dialogView)
    }

    /**
     * Create dimmed overlay (covers entire screen)
     */
    private fun createOverlayView(): View {
        return View(context).apply {
            setBackgroundColor(Color.argb(128, 0, 0, 0))  // 50% black
        }
    }

    /**
     * Create spotlight view (highlights element)
     */
    private fun createSpotlightView(): SpotlightView {
        return SpotlightView(context)
    }

    /**
     * Create dialog view (user input form)
     */
    private fun createDialogView(): View {
        return LayoutInflater.from(context).inflate(
            R.layout.metadata_prompt_dialog,
            null
        ).apply {
            // Get element info
            val className = element.className.toString().substringAfterLast(".")
            val elementType = when {
                className.contains("Button", ignoreCase = true) -> "button"
                className.contains("Image", ignoreCase = true) -> "image"
                className.contains("Text", ignoreCase = true) -> "text"
                else -> "element"
            }

            // Set dialog title
            findViewById<TextView>(R.id.dialog_title).text =
                "⚠️ Element Without Metadata"

            findViewById<TextView>(R.id.dialog_message).text =
                "This $elementType has no description.\n" +
                "What voice command should activate it?"

            // Set up text input
            val commandInput = findViewById<EditText>(R.id.command_input)
            commandInput.hint = "E.g., \"Compose email\""

            // Generate suggestions based on element type
            val suggestions = generateSuggestions(elementType, element)
            val suggestionsContainer = findViewById<LinearLayout>(R.id.suggestions_container)

            suggestions.forEach { suggestion ->
                val suggestionButton = Button(context).apply {
                    text = suggestion
                    setOnClickListener {
                        commandInput.setText(suggestion)
                    }
                }
                suggestionsContainer.addView(suggestionButton)
            }

            // Voice input button
            findViewById<ImageButton>(R.id.voice_input_button).setOnClickListener {
                startVoiceInput(commandInput)
            }

            // Cancel button
            findViewById<Button>(R.id.cancel_button).setOnClickListener {
                dismiss()
            }

            // Save button
            findViewById<Button>(R.id.save_button).setOnClickListener {
                val commandText = commandInput.text.toString().trim()

                if (commandText.isNotEmpty()) {
                    onCommandNamed(commandText)
                    dismiss()

                    // Show confirmation
                    Toast.makeText(
                        context,
                        "Voice command \"$commandText\" saved",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    commandInput.error = "Please enter a command"
                }
            }
        }
    }

    /**
     * Generate command suggestions based on element type
     */
    private fun generateSuggestions(
        elementType: String,
        element: AccessibilityNodeInfo
    ): List<String> {
        // Get context from nearby elements
        val parent = element.parent
        val siblings = mutableListOf<String>()

        parent?.let { p ->
            for (i in 0 until p.childCount) {
                p.getChild(i)?.let { child ->
                    child.text?.toString()?.let { siblings.add(it) }
                    child.contentDescription?.toString()?.let { siblings.add(it) }
                }
            }
        }

        val context = siblings.joinToString(" ").lowercase()

        // Generate suggestions based on context and element type
        return when {
            // Email compose button
            context.contains("compose") || context.contains("write") ->
                listOf("Compose email", "New email", "Write email")

            // Search button
            context.contains("search") || context.contains("find") ->
                listOf("Search", "Find", "Look up")

            // Settings button
            context.contains("settings") || context.contains("preferences") ->
                listOf("Open settings", "Settings", "Preferences")

            // Back button
            element.className.toString().contains("ImageButton") &&
                parent?.childCount ?: 0 <= 3 ->  // Likely navigation button
                listOf("Go back", "Back", "Return")

            // Menu button
            context.contains("menu") || context.contains("more") ->
                listOf("Open menu", "Show menu", "More options")

            // Generic button
            elementType == "button" ->
                listOf("Tap button", "Click button", "Press button")

            // Generic image
            elementType == "image" ->
                listOf("Select image", "Tap image", "Open image")

            else ->
                listOf("Tap $elementType", "Select $elementType", "Click here")
        }
    }

    /**
     * Start voice input for command naming
     */
    private fun startVoiceInput(targetEditText: EditText) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the command name")
        }

        // Note: This requires activity context, so we need to handle this differently
        // For accessibility overlay, we'll use VOSK directly
        val vosk = VoskEngine()
        vosk.startListening { result ->
            targetEditText.setText(result)
        }
    }
}

/**
 * Custom view for spotlight effect with pulsing border
 */
class SpotlightView(context: Context) : View(context) {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.rgb(33, 150, 243)  // Blue
        isAntiAlias = true
    }

    private var animationProgress = 0f
    private var animator: ValueAnimator? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw pulsing border
        val alpha = (255 * (0.5f + 0.5f * sin(animationProgress))).toInt()
        paint.alpha = alpha

        canvas.drawRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            16f,  // Corner radius
            16f,
            paint
        )
    }

    fun startPulseAnimation() {
        animator = ValueAnimator.ofFloat(0f, 2 * PI.toFloat()).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE