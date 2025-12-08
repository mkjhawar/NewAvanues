# Metadata Quality Overlay & Manual Command Assignment - Implementation Plan

**Project:** VoiceOS
**Feature ID:** VOS-META-001
**Plan Version:** 1.0
**Created:** 2025-12-03
**Estimated Duration:** 1.5-2 weeks (with swarm) | 3-4 weeks (sequential)
**Complexity:** High

**Related Spec:** `metadata-quality-overlay-manual-commands-spec.md`

---

## Plan Overview

### Implementation Strategy
**Approach:** Phased implementation with parallel workstreams via swarm architecture
**Agents:** 5 specialists working concurrently
**Testing:** Continuous integration with 90%+ coverage target

### Phase Breakdown
| Phase | Duration | Dependencies | Deliverable |
|-------|----------|--------------|-------------|
| **Phase 1** | 2-3 days | None | Database schema + CustomCommandManager |
| **Phase 2** | 3-4 days | Phase 1 | Manual command assignment dialog + speech |
| **Phase 3** | 2-3 days | Phase 1, 2 | Post-learning overlay |
| **Phase 4** | 3-4 days | Phase 1 | Developer mode quality indicator |
| **Phase 5** | 2-3 days | Phase 1 | Accessibility audit export |
| **Integration** | 2-3 days | All phases | End-to-end testing + polish |

**Total:** 14-20 days (10-14 working days with parallel execution)

---

## Swarm Architecture

### Agent Assignment
| Agent | Primary Responsibility | Secondary Tasks |
|-------|----------------------|-----------------|
| **@vos4-android-expert** | UI components (overlay, dialog, service) | Jetpack Compose implementation |
| **@vos4-database-expert** | SQLDelight schema, migrations, repositories | Query optimization |
| **@vos4-test-specialist** | Unit + integration + UI tests | Test automation |
| **@vos4-architecture-reviewer** | Performance optimization, code review | Architecture compliance |
| **@vos4-documentation-specialist** | User guides, API docs, tutorials | Developer audit guide |

### Parallel Workstreams
```
Timeline (Days 1-14):

Week 1:
┌─────────────────────────────────────────────────┐
│ Day 1-2: Phase 1 (Database)                    │
│   @vos4-database-expert: Schema + Migrations   │
│   @vos4-android-expert: CustomCommandManager   │
│   @vos4-test-specialist: Repository tests      │
└─────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────┐
│ Day 3-6: Phase 2 + 4 (Parallel)                │
│   @vos4-android-expert: Command dialog         │
│   @vos4-android-expert: Quality overlay        │
│   @vos4-test-specialist: Dialog + overlay tests│
│   @vos4-architecture-reviewer: Performance     │
└─────────────────────────────────────────────────┘

Week 2:
┌─────────────────────────────────────────────────┐
│ Day 7-9: Phase 3 + 5 (Parallel)                │
│   @vos4-android-expert: Post-learning overlay  │
│   @vos4-database-expert: Audit export engine   │
│   @vos4-documentation-specialist: User guides  │
└─────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────┐
│ Day 10-14: Integration + Polish                │
│   All agents: E2E testing, bug fixes           │
│   @vos4-test-specialist: Full test suite       │
│   @vos4-documentation-specialist: Final docs   │
└─────────────────────────────────────────────────┘
```

---

## Phase 1: Database Schema + CustomCommandManager (Days 1-2)

### Tasks

#### TASK 1.1: Create SQLDelight Schema
**Owner:** @vos4-database-expert
**Duration:** 4 hours
**Priority:** P0 (Blocking)

**Files to Create:**
- `libraries/core/database/src/commonMain/sqldelight/com/augmentalis/database/CustomCommand.sq`

**Implementation:**
```sql
-- custom_commands table
CREATE TABLE IF NOT EXISTS custom_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    element_uuid TEXT NOT NULL,
    command_phrase TEXT NOT NULL,
    confidence REAL DEFAULT 1.0,
    created_at INTEGER NOT NULL,
    created_by TEXT DEFAULT 'user',
    is_synonym INTEGER DEFAULT 0,
    app_id TEXT NOT NULL,
    FOREIGN KEY (element_uuid) REFERENCES uuid_registry(uuid),
    UNIQUE(element_uuid, command_phrase, app_id)
);

CREATE INDEX IF NOT EXISTS idx_custom_commands_uuid
    ON custom_commands(element_uuid);
CREATE INDEX IF NOT EXISTS idx_custom_commands_phrase
    ON custom_commands(command_phrase);
CREATE INDEX IF NOT EXISTS idx_custom_commands_app
    ON custom_commands(app_id);

-- quality_metrics table
CREATE TABLE IF NOT EXISTS quality_metrics (
    element_uuid TEXT PRIMARY KEY,
    app_id TEXT NOT NULL,
    quality_score INTEGER NOT NULL,
    has_text INTEGER NOT NULL,
    has_content_desc INTEGER NOT NULL,
    has_resource_id INTEGER NOT NULL,
    command_count INTEGER DEFAULT 0,
    manual_command_count INTEGER DEFAULT 0,
    last_assessed INTEGER NOT NULL,
    FOREIGN KEY (element_uuid) REFERENCES uuid_registry(uuid)
);

CREATE INDEX IF NOT EXISTS idx_quality_metrics_app
    ON quality_metrics(app_id);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_score
    ON quality_metrics(quality_score);

-- Queries
insertCustomCommand:
INSERT INTO custom_commands (element_uuid, command_phrase, confidence, created_at, created_by, is_synonym, app_id)
VALUES (?, ?, ?, ?, ?, ?, ?);

getCommandsByUuid:
SELECT * FROM custom_commands WHERE element_uuid = ?;

getCommandsByApp:
SELECT * FROM custom_commands WHERE app_id = ?;

getCommandByPhrase:
SELECT * FROM custom_commands WHERE command_phrase = ? AND app_id = ?;

deleteCommand:
DELETE FROM custom_commands WHERE id = ?;

deleteCommandsBySynonym:
DELETE FROM custom_commands WHERE element_uuid = ? AND is_synonym = 1;

getAllCommandsForElement:
SELECT * FROM custom_commands WHERE element_uuid = ? ORDER BY is_synonym ASC, created_at ASC;

-- Quality metrics queries
insertQualityMetric:
INSERT OR REPLACE INTO quality_metrics
(element_uuid, app_id, quality_score, has_text, has_content_desc, has_resource_id, command_count, manual_command_count, last_assessed)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

getQualityMetricsByApp:
SELECT * FROM quality_metrics WHERE app_id = ?;

getQualityMetricByUuid:
SELECT * FROM quality_metrics WHERE element_uuid = ?;

getPoorQualityElements:
SELECT * FROM quality_metrics WHERE app_id = ? AND quality_score < 40 ORDER BY quality_score ASC;

getElementsWithoutCommands:
SELECT * FROM quality_metrics WHERE app_id = ? AND command_count = 0 AND quality_score < 40;
```

**Testing:**
- [ ] Schema compiles without errors
- [ ] All queries validated by SQLDelight
- [ ] Foreign key constraints work
- [ ] Indexes created successfully

---

#### TASK 1.2: Create DTOs
**Owner:** @vos4-database-expert
**Duration:** 2 hours
**Priority:** P0

**Files to Create:**
- `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/dto/CustomCommandDTO.kt`
- `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/dto/QualityMetricDTO.kt`

**Implementation:**
```kotlin
// CustomCommandDTO.kt
package com.augmentalis.database.dto

data class CustomCommandDTO(
    val id: Long = 0L,
    val elementUuid: String,
    val commandPhrase: String,
    val confidence: Double = 1.0,
    val createdAt: Long,
    val createdBy: String = "user",
    val isSynonym: Boolean = false,
    val appId: String
)

// Extension function for DB mapping
fun com.augmentalis.database.Custom_commands.toDTO(): CustomCommandDTO {
    return CustomCommandDTO(
        id = id,
        elementUuid = element_uuid,
        commandPhrase = command_phrase,
        confidence = confidence,
        createdAt = created_at,
        createdBy = created_by,
        isSynonym = is_synonym == 1L,
        appId = app_id
    )
}

// QualityMetricDTO.kt
package com.augmentalis.database.dto

data class QualityMetricDTO(
    val elementUuid: String,
    val appId: String,
    val qualityScore: Int,
    val hasText: Boolean,
    val hasContentDesc: Boolean,
    val hasResourceId: Boolean,
    val commandCount: Int = 0,
    val manualCommandCount: Int = 0,
    val lastAssessed: Long
) {
    fun toQualityLevel(): MetadataQualityLevel {
        return when {
            qualityScore >= 80 -> MetadataQualityLevel.EXCELLENT
            qualityScore >= 60 -> MetadataQualityLevel.GOOD
            qualityScore >= 40 -> MetadataQualityLevel.ACCEPTABLE
            else -> MetadataQualityLevel.POOR
        }
    }
}

enum class MetadataQualityLevel {
    EXCELLENT, // 80-100
    GOOD,      // 60-79
    ACCEPTABLE,// 40-59
    POOR       // 0-39
}

fun com.augmentalis.database.Quality_metrics.toDTO(): QualityMetricDTO {
    return QualityMetricDTO(
        elementUuid = element_uuid,
        appId = app_id,
        qualityScore = quality_score.toInt(),
        hasText = has_text == 1L,
        hasContentDesc = has_content_desc == 1L,
        hasResourceId = has_resource_id == 1L,
        commandCount = command_count.toInt(),
        manualCommandCount = manual_command_count.toInt(),
        lastAssessed = last_assessed
    )
}
```

**Testing:**
- [ ] DTOs serialize/deserialize correctly
- [ ] Extension functions map all fields
- [ ] Quality level calculation correct
- [ ] Boolean conversions work (Long ↔ Boolean)

---

#### TASK 1.3: Create Repository Interface
**Owner:** @vos4-database-expert
**Duration:** 2 hours
**Priority:** P0

**Files to Create:**
- `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/ICustomCommandRepository.kt`
- `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IQualityMetricRepository.kt`

**Implementation:**
```kotlin
// ICustomCommandRepository.kt
package com.augmentalis.database.repositories

import com.augmentalis.database.dto.CustomCommandDTO

interface ICustomCommandRepository {
    suspend fun insert(command: CustomCommandDTO): Long
    suspend fun getByUuid(elementUuid: String): List<CustomCommandDTO>
    suspend fun getByApp(appId: String): List<CustomCommandDTO>
    suspend fun getByPhrase(phrase: String, appId: String): CustomCommandDTO?
    suspend fun delete(commandId: Long)
    suspend fun deleteSynonyms(elementUuid: String)
    suspend fun getAllForElement(elementUuid: String): List<CustomCommandDTO>
    suspend fun hasPrimaryCommand(elementUuid: String): Boolean
}

// IQualityMetricRepository.kt
package com.augmentalis.database.repositories

import com.augmentalis.database.dto.QualityMetricDTO

interface IQualityMetricRepository {
    suspend fun insert(metric: QualityMetricDTO)
    suspend fun getByApp(appId: String): List<QualityMetricDTO>
    suspend fun getByUuid(elementUuid: String): QualityMetricDTO?
    suspend fun getPoorQualityElements(appId: String): List<QualityMetricDTO>
    suspend fun getElementsWithoutCommands(appId: String): List<QualityMetricDTO>
    suspend fun updateCommandCounts(elementUuid: String, commandCount: Int, manualCount: Int)
}
```

---

#### TASK 1.4: Implement Repositories
**Owner:** @vos4-database-expert
**Duration:** 4 hours
**Priority:** P0

**Files to Create:**
- `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightCustomCommandRepository.kt`
- `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightQualityMetricRepository.kt`

**Implementation:**
```kotlin
// SQLDelightCustomCommandRepository.kt
package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.CustomCommandDTO
import com.augmentalis.database.dto.toDTO
import com.augmentalis.database.repositories.ICustomCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SQLDelightCustomCommandRepository(
    private val database: VoiceOSDatabase
) : ICustomCommandRepository {

    private val queries = database.customCommandQueries

    override suspend fun insert(command: CustomCommandDTO): Long = withContext(Dispatchers.Default) {
        queries.insertCustomCommand(
            element_uuid = command.elementUuid,
            command_phrase = command.commandPhrase,
            confidence = command.confidence,
            created_at = command.createdAt,
            created_by = command.createdBy,
            is_synonym = if (command.isSynonym) 1L else 0L,
            app_id = command.appId
        )
        queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun getByUuid(elementUuid: String): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getCommandsByUuid(elementUuid)
                .executeAsList()
                .map { it.toDTO() }
        }

    override suspend fun getByApp(appId: String): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getCommandsByApp(appId)
                .executeAsList()
                .map { it.toDTO() }
        }

    override suspend fun getByPhrase(phrase: String, appId: String): CustomCommandDTO? =
        withContext(Dispatchers.Default) {
            queries.getCommandByPhrase(phrase, appId)
                .executeAsOneOrNull()
                ?.toDTO()
        }

    override suspend fun delete(commandId: Long) = withContext(Dispatchers.Default) {
        queries.deleteCommand(commandId)
    }

    override suspend fun deleteSynonyms(elementUuid: String) = withContext(Dispatchers.Default) {
        queries.deleteCommandsBySynonym(elementUuid)
    }

    override suspend fun getAllForElement(elementUuid: String): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getAllCommandsForElement(elementUuid)
                .executeAsList()
                .map { it.toDTO() }
        }

    override suspend fun hasPrimaryCommand(elementUuid: String): Boolean =
        withContext(Dispatchers.Default) {
            val commands = getByUuid(elementUuid)
            commands.any { !it.isSynonym }
        }
}

// Similar implementation for SQLDelightQualityMetricRepository
```

**Testing:**
- [ ] All CRUD operations work
- [ ] Concurrent access handled correctly
- [ ] Transactions rollback on error
- [ ] Query performance <10ms

---

#### TASK 1.5: Create CustomCommandManager
**Owner:** @vos4-android-expert
**Duration:** 6 hours
**Priority:** P0

**Files to Create:**
- `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/commands/CustomCommandManager.kt`

**Implementation:**
```kotlin
package com.augmentalis.voiceoscore.commands

import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.CustomCommandDTO
import com.augmentalis.database.dto.QualityMetricDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CustomCommandManager - Manages user-assigned voice commands
 *
 * Provides CRUD operations for custom commands and integrates with
 * CommandProcessor for voice recognition.
 */
class CustomCommandManager(
    private val databaseManager: VoiceOSDatabaseManager
) {
    companion object {
        private const val TAG = "CustomCommandManager"
        private const val MIN_COMMAND_LENGTH = 3
        private const val MAX_COMMAND_LENGTH = 50
    }

    // Cached commands for fast lookup
    private val _commandCache = MutableStateFlow<Map<String, List<CustomCommandDTO>>>(emptyMap())
    val commandCache: Flow<Map<String, List<CustomCommandDTO>>> = _commandCache.asStateFlow()

    /**
     * Add custom command for element
     *
     * @param elementUuid Element UUID from ThirdPartyUuidGenerator
     * @param commandPhrase User-spoken command (e.g., "submit button")
     * @param appId Package name
     * @param isSynonym true if adding synonym to existing command
     * @return Command ID or -1 on error
     */
    suspend fun addCommand(
        elementUuid: String,
        commandPhrase: String,
        appId: String,
        isSynonym: Boolean = false
    ): Long {
        // Validate command phrase
        val sanitized = sanitizeCommandPhrase(commandPhrase)
        if (!isValidCommandPhrase(sanitized)) {
            Log.w(TAG, "Invalid command phrase: $commandPhrase")
            return -1L
        }

        // Check for duplicates
        val existing = databaseManager.customCommands.getByPhrase(sanitized, appId)
        if (existing != null) {
            Log.w(TAG, "Command phrase already exists: $sanitized")
            return -1L
        }

        // Determine if this is a synonym
        val hasPrimary = databaseManager.customCommands.hasPrimaryCommand(elementUuid)
        val actualIsSynonym = isSynonym || hasPrimary

        // Create command
        val command = CustomCommandDTO(
            elementUuid = elementUuid,
            commandPhrase = sanitized,
            confidence = 1.0,
            createdAt = System.currentTimeMillis(),
            createdBy = "user",
            isSynonym = actualIsSynonym,
            appId = appId
        )

        val id = databaseManager.customCommands.insert(command)

        if (id > 0) {
            Log.i(TAG, "Added custom command: '$sanitized' for $elementUuid (synonym=$actualIsSynonym)")

            // Update command count in quality metrics
            updateCommandCount(elementUuid, appId)

            // Refresh cache
            refreshCache(appId)
        }

        return id
    }

    /**
     * Get all commands for element
     */
    suspend fun getCommandsForElement(elementUuid: String): List<CustomCommandDTO> {
        return databaseManager.customCommands.getAllForElement(elementUuid)
    }

    /**
     * Get all commands for app
     */
    suspend fun getCommandsForApp(appId: String): List<CustomCommandDTO> {
        return databaseManager.customCommands.getByApp(appId)
    }

    /**
     * Find element UUID by command phrase
     *
     * @param phrase User-spoken command
     * @param appId Package name
     * @return Element UUID or null if not found
     */
    suspend fun findElementByCommand(phrase: String, appId: String): String? {
        val sanitized = sanitizeCommandPhrase(phrase)
        val command = databaseManager.customCommands.getByPhrase(sanitized, appId)
        return command?.elementUuid
    }

    /**
     * Delete command
     */
    suspend fun deleteCommand(commandId: Long, appId: String) {
        databaseManager.customCommands.delete(commandId)
        refreshCache(appId)
        Log.i(TAG, "Deleted custom command: $commandId")
    }

    /**
     * Delete all synonyms for element (keeps primary command)
     */
    suspend fun deleteSynonyms(elementUuid: String, appId: String) {
        databaseManager.customCommands.deleteSynonyms(elementUuid)
        updateCommandCount(elementUuid, appId)
        refreshCache(appId)
        Log.i(TAG, "Deleted synonyms for: $elementUuid")
    }

    /**
     * Validate command phrase
     */
    private fun isValidCommandPhrase(phrase: String): Boolean {
        return phrase.length in MIN_COMMAND_LENGTH..MAX_COMMAND_LENGTH &&
               phrase.matches(Regex("[a-zA-Z0-9 ]+")) &&
               !isProfanity(phrase)
    }

    /**
     * Sanitize command phrase (lowercase, trim, collapse spaces)
     */
    private fun sanitizeCommandPhrase(phrase: String): String {
        return phrase.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Check for profanity (basic filter)
     */
    private fun isProfanity(phrase: String): Boolean {
        // TODO: Implement profanity filter
        return false
    }

    /**
     * Update command count in quality metrics
     */
    private suspend fun updateCommandCount(elementUuid: String, appId: String) {
        val commands = getCommandsForElement(elementUuid)
        val manualCount = commands.size

        // Get total commands (including auto-generated)
        // TODO: Query auto-generated commands from CommandGenerator
        val totalCount = manualCount

        databaseManager.qualityMetrics.updateCommandCounts(elementUuid, totalCount, manualCount)
    }

    /**
     * Refresh command cache for app
     */
    private suspend fun refreshCache(appId: String) {
        val commands = getCommandsForApp(appId)
        val cache = commands.groupBy { it.elementUuid }
        _commandCache.value = _commandCache.value + (appId to commands)
        Log.d(TAG, "Refreshed command cache for $appId: ${commands.size} commands")
    }

    /**
     * Preload cache for app (call when app becomes active)
     */
    suspend fun preloadCache(appId: String) {
        refreshCache(appId)
    }
}
```

**Testing:**
- [ ] Command validation works (length, regex, profanity)
- [ ] Duplicate detection works
- [ ] Synonym logic correct (first = primary, rest = synonyms)
- [ ] Cache updates on modifications
- [ ] Concurrent access handled

---

#### TASK 1.6: Database Manager Integration
**Owner:** @vos4-database-expert
**Duration:** 2 hours
**Priority:** P0

**Files to Modify:**
- `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/VoiceOSDatabaseManager.kt`

**Changes:**
```kotlin
class VoiceOSDatabaseManager(/* ... */) {
    // ... existing repositories ...

    // NEW: Custom command repositories
    val customCommands: ICustomCommandRepository by lazy {
        SQLDelightCustomCommandRepository(database)
    }

    val qualityMetrics: IQualityMetricRepository by lazy {
        SQLDelightQualityMetricRepository(database)
    }
}
```

**Testing:**
- [ ] New repositories accessible
- [ ] Lazy initialization works
- [ ] No breaking changes to existing code

---

#### TASK 1.7: Unit Tests - Database Layer
**Owner:** @vos4-test-specialist
**Duration:** 6 hours
**Priority:** P1

**Files to Create:**
- `libraries/core/database/src/commonTest/kotlin/com/augmentalis/database/repositories/CustomCommandRepositoryTest.kt`
- `libraries/core/database/src/commonTest/kotlin/com/augmentalis/database/repositories/QualityMetricRepositoryTest.kt`
- `modules/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/commands/CustomCommandManagerTest.kt`

**Test Coverage:**
- [ ] Insert command (primary + synonym)
- [ ] Get commands by UUID, app, phrase
- [ ] Delete command, delete synonyms
- [ ] Duplicate detection
- [ ] Quality metric CRUD
- [ ] Command count updates
- [ ] Cache refresh logic
- [ ] Concurrent access (race conditions)
- [ ] Performance (10ms query target)

**Minimum Coverage:** 90%

---

### Phase 1 Deliverables

**Code:**
- [ ] SQLDelight schema compiled and validated
- [ ] DTOs with extension functions
- [ ] Repository interfaces + implementations
- [ ] CustomCommandManager with full CRUD
- [ ] Database manager integration

**Tests:**
- [ ] 90%+ unit test coverage
- [ ] All edge cases covered
- [ ] Performance benchmarks met

**Documentation:**
- [ ] API documentation (KDoc)
- [ ] Database schema diagram
- [ ] Usage examples

---

## Phase 2: Manual Command Assignment Dialog (Days 3-6)

### Tasks

#### TASK 2.1: Speech Recognition Integration
**Owner:** @vos4-android-expert
**Duration:** 6 hours
**Priority:** P0

**Files to Create:**
- `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/speech/SpeechRecorder.kt`

**Implementation:**
```kotlin
package com.augmentalis.voiceoscore.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Locale

/**
 * SpeechRecorder - Voice input capture for command assignment
 */
class SpeechRecorder(private val context: Context) {
    companion object {
        private const val TAG = "SpeechRecorder"
    }

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val resultChannel = Channel<SpeechResult>(Channel.BUFFERED)

    sealed class SpeechResult {
        data class Success(val text: String, val confidence: Float) : SpeechResult()
        data class Error(val errorCode: Int, val message: String) : SpeechResult()
        object Listening : SpeechResult()
        object Ready : SpeechResult()
    }

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                resultChannel.trySend(SpeechResult.Ready)
            }

            override fun onBeginningOfSpeech() {
                resultChannel.trySend(SpeechResult.Listening)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val confidence = confidences?.getOrNull(0) ?: 0.5f
                    resultChannel.trySend(SpeechResult.Success(text, confidence))
                } else {
                    resultChannel.trySend(SpeechResult.Error(
                        SpeechRecognizer.ERROR_NO_MATCH,
                        "No speech detected"
                    ))
                }
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error"
                }
                resultChannel.trySend(SpeechResult.Error(error, message))
            }

            // Other callbacks...
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening(): Flow<SpeechResult> {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer.startListening(intent)
        return resultChannel.receiveAsFlow()
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun cancel() {
        speechRecognizer.cancel()
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}
```

**Testing:**
- [ ] Speech recognition starts/stops correctly
- [ ] Results flow emits events in order
- [ ] Error handling for all error codes
- [ ] Confidence scores captured
- [ ] Memory cleanup on destroy

---

#### TASK 2.2: Command Assignment Dialog UI
**Owner:** @vos4-android-expert
**Duration:** 8 hours
**Priority:** P0

**Files to Create:**
- `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/ui/commandassignment/CommandAssignmentDialog.kt`
- `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/ui/commandassignment/CommandAssignmentViewModel.kt`

**Implementation:** (See detailed implementation in continuation...)

**Testing:**
- [ ] Dialog opens/closes correctly
- [ ] Recording button states (idle, listening, processing)
- [ ] Visual feedback during recording
- [ ] Result validation UI
- [ ] Synonym flow works
- [ ] Error messages displayed

---

*[Plan continues for Phases 3-5 and Integration... Due to length, showing structure for remaining phases]*

---

## Phase 3: Post-Learning Overlay (Days 7-9)

### Tasks
- TASK 3.1: Overlay Service Architecture
- TASK 3.2: Element Highlighting Renderer
- TASK 3.3: Tap Detection & Routing
- TASK 3.4: Integration with ExplorationEngine
- TASK 3.5: Integration with JustInTimeLearner
- TASK 3.6: Unit + UI Tests

---

## Phase 4: Developer Mode Quality Indicator (Days 7-10)

### Tasks
- TASK 4.1: Settings Toggle Implementation
- TASK 4.2: Quality Overlay Service
- TASK 4.3: Element Border Rendering (Color-coded)
- TASK 4.4: Quality Popup View
- TASK 4.5: Performance Optimization (<16ms/frame)
- TASK 4.6: Notification Shade Toggle
- TASK 4.7: Unit + Performance Tests

---

## Phase 5: Accessibility Audit Export (Days 9-11)

### Tasks
- TASK 5.1: AccessibilityAuditor Engine
- TASK 5.2: JSON Report Generator
- TASK 5.3: Markdown Report Generator
- TASK 5.4: Share Integration (Android Share Sheet)
- TASK 5.5: Anonymization Logic
- TASK 5.6: Export UI in Settings
- TASK 5.7: Unit Tests + Sample Reports

---

## Integration & Testing (Days 12-14)

### Tasks
- TASK I.1: End-to-End Testing (User Stories)
- TASK I.2: Performance Profiling & Optimization
- TASK I.3: Accessibility Testing (TalkBack)
- TASK I.4: Real Device Testing (5+ Apps)
- TASK I.5: Code Review & Refactoring
- TASK I.6: Documentation Finalization
- TASK I.7: Beta Deployment

---

## Critical Path

```
Database (P1) → CustomCommandManager (P1) → Command Dialog (P2) → Post-Learning Overlay (P3)
                                          ↓
                                  Quality Indicator (P4)
                                          ↓
                                  Audit Export (P5)
                                          ↓
                                  Integration Testing
```

**Blocking Dependencies:**
- Phase 2, 3, 4, 5 all depend on Phase 1 completion
- Phase 3 depends on Phase 2 (dialog must work before overlay can use it)
- Integration depends on all phases

---

## Risk Mitigation

| Risk | Mitigation | Owner |
|------|------------|-------|
| Speech recognition accuracy <90% | Add keyboard fallback, confidence tuning | @vos4-android-expert |
| Overlay performance <60 FPS | Hardware acceleration, render optimization | @vos4-architecture-reviewer |
| UUID instability across app updates | Add fuzzy matching, manual re-map flow | @vos4-database-expert |
| Test coverage <90% | Automated coverage reporting, CI gates | @vos4-test-specialist |
| Documentation incomplete | Parallel doc writing, review checkpoints | @vos4-documentation-specialist |

---

## Success Metrics

### Phase Completion Criteria
- [ ] Phase 1: All database tests pass, CustomCommandManager functional
- [ ] Phase 2: Dialog records voice, saves commands, integrates with manager
- [ ] Phase 3: Overlay appears post-learning, elements highlighted, tap works
- [ ] Phase 4: Quality indicator toggles, overlay renders <16ms, popup shows details
- [ ] Phase 5: Audit exports to JSON + MD, shares via Android Share Sheet

### Overall Success
- [ ] User can assign custom command in <60 seconds
- [ ] Overlay maintains 60 FPS with 100 elements
- [ ] All 4 user stories validated via end-to-end tests
- [ ] Real device testing on 5+ apps (including RealWear Test App)
- [ ] Documentation complete (user guide + API docs + audit template)

---

## Next Steps

1. **Approve Plan** - Review and sign off on implementation approach
2. **Allocate Agents** - Assign 5 specialists to workstreams
3. **Kickoff Phase 1** - Start database schema and CustomCommandManager
4. **Daily Standups** - 15-min sync to track progress and blockers
5. **Weekly Review** - End-of-week demo and course correction

---

**Plan Status:** Ready for Execution
**Awaiting:** User approval to begin Phase 1
**Estimated Start:** Upon approval
**Estimated Completion:** 14 days from start (with swarm)
