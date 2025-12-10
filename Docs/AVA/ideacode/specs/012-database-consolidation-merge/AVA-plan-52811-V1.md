# Implementation Plan: Database Consolidation for Multi-Repo Merge

**Feature ID:** 012
**Feature Name:** Cross-Repository Database Consolidation (VoiceOS/AVA/Avanues Merge)
**Spec Version:** 1.0.0
**Plan Created:** 2025-11-28
**Estimated Duration:** 3.5 weeks (with swarm mode) | 6 weeks (sequential)
**Complexity:** Tier 3 (Complex - Multi-repository, cross-platform, data migration)

---

## Executive Summary

This plan details the implementation strategy for consolidating 63 database schema files across 4 repositories (VoiceOS, AVA, WebAvanue, MainAvanues) into a unified modular database architecture. The consolidation will be executed in **4 sequential phases** over 3.5 weeks using **swarm mode** (5 specialist agents) to achieve 40% time savings.

**Key Strategy Decisions (from Clarifications):**
- ✅ **Sequential Migration** (NO dual-write period) - migrate one module at a time, test, confirm, move to next
- ✅ **Automatic Rollback** (transaction-based) - zero user intervention if migration fails
- ✅ **Rolling Consolidation** (7-day/28-day) - compact history with automatic deduplication
- ✅ **GDPR Compliance** (from day 1) - explicit consent, data export, right to erasure

**Expected Outcomes:**
- VoiceOS initialization: **4-5s → 0.2-0.5s** (10-20x faster)
- AVA initialization: **0.2s maintained** (zero regression)
- Database tables: **55 → 42** (24% reduction)
- APK size: **-20-30 MB** (shared ML models)
- Storage savings: **~95%** (rolling consolidation vs raw data)

---

## Phase Dependencies & Platform Ordering

### Critical Path Analysis

```
┌─────────────────────────────────────────────────────────────┐
│ PHASE 0: Foundation (Shared/KMP) - Week 0.5                │
│ Create all schemas in commonMain                           │
│ ├── app_settings.sq                                         │
│ ├── activity_history.sq                                     │
│ ├── error_log.sq                                            │
│ ├── ml_embedding_cache.sq                                   │
│ ├── ml_training_example.sq + FTS                            │
│ └── ml_model_metadata.sq                                    │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 1A: VoiceOS Module Migration - Week 1                │
│ Sequential: VoiceOS → test → confirm                        │
│ ├── Settings migration (5 tables → app_settings)            │
│ ├── History migration (4 tables → activity_history)         │
│ ├── Error migration (1 table → error_log)                   │
│ ├── TESTING CHECKPOINT: All VoiceOS DBs working            │
│ └── Remove VoiceOS legacy tables                            │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 1B: AVA Module Migration - Week 1.5                  │
│ Sequential: AVA → test → confirm                            │
│ ├── Settings migration (SharedPreferences → app_settings)   │
│ ├── History migration (conversation/message → activity)     │
│ ├── Error migration (add error_log)                         │
│ ├── TESTING CHECKPOINT: AVA 0.2s init maintained           │
│ └── Remove AVA legacy tables                                │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 1C: WebAvanue Module Migration - Week 2              │
│ Sequential: WebAvanue → test → confirm                      │
│ ├── Settings migration (browser_settings → app_settings)    │
│ ├── History migration (history_entry → activity_history)    │
│ ├── Error migration (add error_log)                         │
│ ├── TESTING CHECKPOINT: WebAvanue working                  │
│ └── Remove WebAvanue legacy tables                          │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 2A: AVA ML/AI Migration - Week 2.5                   │
│ Migrate AVA first (validate no regression)                  │
│ ├── intent_embedding → ml_embedding_cache (module='chat')   │
│ ├── intent_example → ml_training_example                    │
│ ├── train_example → ml_training_example                     │
│ ├── CRITICAL CHECKPOINT: 95% cache hit rate maintained     │
│ └── CRITICAL CHECKPOINT: 0.2s init maintained              │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 2B: VoiceOS ML/AI Migration - Week 3                 │
│ Migrate VoiceOS (expect 10-20x speedup)                     │
│ ├── language_model → ml_training_example (module='voice')   │
│ ├── gesture_learning → ml_training_example                  │
│ ├── recognition_learning → ml_training_example              │
│ ├── Enable VoiceOS embedding cache                          │
│ ├── CRITICAL CHECKPOINT: 10-20x speedup achieved           │
│ └── Remove VoiceOS legacy ML tables                         │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 3: Plugin Extension - Week 3.5                       │
│ Extend VoiceOS plugin system to all modules                 │
│ ├── Add module field to plugins, plugin_permissions         │
│ ├── Create browser plugin API (module='browser')            │
│ ├── Create chat plugin API (module='chat')                  │
│ ├── TESTING CHECKPOINT: VoiceOS plugins still work         │
│ └── TESTING CHECKPOINT: Cross-module plugins work          │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ PHASE 4: Validation & Optimization - Week 4 (final 0.5w)   │
│ Final validation and cleanup                                │
│ ├── Rolling consolidation implementation (7-day/28-day)     │
│ ├── GDPR compliance validation (all privacy features)       │
│ ├── Performance validation (all targets met)                │
│ ├── Production monitoring setup                             │
│ └── Documentation and training                              │
└─────────────────────────────────────────────────────────────┘
```

### Platform Execution Order

**1. Backend: NOT IN SCOPE** (Phases 1-4)
- Deferred to Q2 2025
- No backend work needed for Android consolidation

**2. Shared/KMP: FIRST** (Phase 0)
- All schema files created in commonMain
- 100% schema sharing across future platforms
- Type-safe query interfaces generated
- Column adapters for complex types

**3. Android: SEQUENTIAL BY MODULE** (Phases 1-3)
- VoiceOS migrated first → test → confirm → remove legacy
- AVA migrated second → test → confirm → remove legacy
- WebAvanue migrated third → test → confirm → remove legacy
- **NO dual-write period** (per clarification Q3)

**Why This Order:**
1. **Shared/KMP first** → provides foundation (schemas) for all modules
2. **VoiceOS first** → largest codebase (38 tables), most complex, validates migration strategy
3. **AVA second** → critical performance validation (must maintain 0.2s init), ML/AI expertise
4. **WebAvanue third** → smallest codebase (6 tables), least risk, final validation

---

## Platform-Specific Implementation Breakdown

### Platform 1: Shared/KMP (Foundation)

**Phase 0: Schema Setup (Week 0.5)**

**Deliverables:**
- 11 .sq schema files in `commonMain/sqldelight/com/augmentalis/core/data/db/`:
  - `AppSettings.sq` - Unified settings (module-based)
  - `ActivityHistory.sq` - Unified history (module-based)
  - `ErrorLog.sq` - Unified error tracking
  - `MLEmbeddingCache.sq` - Shared embedding cache
  - `MLTrainingExample.sq` - Unified training data
  - `MLTrainingExampleFts.sq` - FTS4 for fast search
  - `MLModelMetadata.sq` - Model version tracking
  - `Plugins.sq` - Extended with module field
  - `PluginDependencies.sq` - Unchanged
  - `PluginPermissions.sq` - Extended with module field
  - `SystemCheckpoint.sq` - Unchanged

**Tech Stack:**
- SQLDelight 2.0.1 plugin configuration
- SQLite 3.38 dialect
- CommonMain source set (Kotlin Multiplatform)

**Implementation Steps:**

1. **Configure SQLDelight in Core/Data module** (1 hour)
   ```kotlin
   // build.gradle.kts
   plugins {
       id("app.cash.sqldelight") version "2.0.1"
   }

   sqldelight {
       databases {
           create("AugmentalisDatabase") {
               packageName.set("com.augmentalis.core.data.db")
               dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
               schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
               deriveSchemaFromMigrations.set(true)
           }
       }
   }
   ```

2. **Create schema files** (1 day)
   - Copy from specification (FR-001 through FR-005)
   - Add indices, constraints, triggers (FTS4)
   - Add SQLDelight queries (insert, update, delete, select)

3. **Create ColumnAdapters** (2 hours)
   ```kotlin
   // ColumnAdapters.kt (commonMain)
   object StringListAdapter : ColumnAdapter<List<String>, String>
   object BooleanAdapter : ColumnAdapter<Boolean, Long>
   object ByteArrayAdapter : ColumnAdapter<ByteArray, ByteArray>
   ```

4. **Generate code and validate** (2 hours)
   ```bash
   ./gradlew :Core:Data:generateCommonMainAugmentalisDatabaseInterface
   ```
   - Verify no compilation errors
   - Verify all queries type-safe

**Success Criteria:**
- [ ] All 11 .sq files created in commonMain
- [ ] Code generation successful (no errors)
- [ ] All queries type-safe (compile-time validation)
- [ ] ColumnAdapters created for complex types

**Testing:**
- Unit tests for ColumnAdapters (encode/decode)
- Schema validation tests (tables exist, indices correct)

**Risks:**
- Low risk (read-only schema creation, no data yet)

---

### Platform 2: Android (Primary Implementation)

#### PHASE 1A: VoiceOS Module Migration (Week 1)

**Sequential Migration Strategy (NO Dual-Write):**

**Step 1: Pre-Migration Setup** (Day 1)

1. **Create Android database driver**
   ```kotlin
   // DatabaseDriverFactory.kt (androidMain)
   class AndroidDatabaseDriverFactory(private val context: Context) {
       fun createDriver(): SqlDriver {
           return AndroidSqliteDriver(
               schema = AugmentalisDatabase.Schema,
               context = context,
               name = "augmentalis.db"
           )
       }
   }
   ```

2. **Create shared repositories (empty, ready for migration)**
   - `AppSettingsRepository` (module filter support)
   - `ActivityHistoryRepository` (module filter support)
   - `ErrorLogRepository` (module filter support)

3. **Create migration utility**
   ```kotlin
   class VoiceOSMigrationUtility(
       private val oldDb: VoiceOSDatabase,  // Legacy
       private val newDb: AugmentalisDatabase  // New unified
   ) {
       suspend fun migrateSettings(): MigrationResult
       suspend fun migrateHistory(): MigrationResult
       suspend fun migrateErrors(): MigrationResult
       suspend fun calculateChecksum(): String
   }
   ```

**Step 2: VoiceOS Settings Migration** (Day 2)

**Tables to Migrate:**
- `settings` → `app_settings` (module='voice', category='system')
- `user_preference` → `app_settings` (module='voice', category='user')
- `context_preference` → `app_settings` (module='voice', category='context')
- `analytics_settings` → `app_settings` (module='voice', category='analytics')
- `retention_settings` → `app_settings` (module='voice', category='retention')

**Migration Logic:**
```kotlin
suspend fun migrateSettings(): MigrationResult {
    return withTransaction {
        // 1. Calculate checksum of legacy data
        val oldChecksum = calculateChecksum(oldDb.settingsQueries.selectAll())

        // 2. Migrate settings table
        oldDb.settingsQueries.selectAll().forEach { setting ->
            newDb.appSettingsQueries.insertOrReplace(
                id = UUID.randomUUID().toString(),
                module = "voice",
                category = "system",
                key = setting.key,
                value = setting.value,
                type = setting.type,
                default_value = setting.default_value,
                updated_at = System.currentTimeMillis()
            )
        }

        // 3. Verify checksum
        val newChecksum = calculateChecksum(
            newDb.appSettingsQueries.selectByModule("voice")
        )

        if (oldChecksum != newChecksum) {
            // AUTOMATIC ROLLBACK (Clarification Q2)
            throw MigrationException("Checksum mismatch")
        }

        MigrationResult.Success(rowCount = count)
    }
}
```

**Step 3: VoiceOS History Migration** (Day 3)

**Tables to Migrate:**
- `command_history` → `activity_history` (module='voice', type='command')
- `command_usage` → `activity_history` (module='voice', type='command_usage')
- `user_interaction` → `activity_history` (module='voice', type='interaction')
- `usage_statistic` → `activity_history` (module='voice', type='statistic')

**Migration Logic:**
```kotlin
suspend fun migrateHistory(): MigrationResult {
    return withTransaction {
        // 1. Migrate command_history
        oldDb.commandHistoryQueries.selectAll().forEach { cmd ->
            newDb.activityHistoryQueries.insertOrReplace(
                id = cmd.id,
                module = "voice",
                activity_type = "command",
                timestamp = cmd.timestamp,
                title = cmd.command_text,
                content = cmd.processed_text,
                metadata = buildMetadataJson(cmd),  // JSON for VoiceOS-specific
                confidence = cmd.confidence,
                success = cmd.success,
                execution_time_ms = cmd.execution_time,
                usage_count = 1,
                context_id = cmd.context_id,
                user_id = null,
                device_id = getDeviceId(),
                is_incognito = 0,
                created_at = cmd.created_at,
                updated_at = System.currentTimeMillis()
            )
        }

        // 2. Similar for command_usage, user_interaction, usage_statistic
        // ... (repeat pattern)

        MigrationResult.Success()
    }
}
```

**Step 4: VoiceOS Error Migration** (Day 3)

**Tables to Migrate:**
- `error_report` → `error_log` (module='voice')

**Migration Logic:**
```kotlin
suspend fun migrateErrors(): MigrationResult {
    return withTransaction {
        oldDb.errorReportQueries.selectAll().forEach { error ->
            newDb.errorLogQueries.insertOrReplace(
                id = error.id,
                module = "voice",
                severity = error.severity,
                error_type = error.error_type,
                error_message = error.message,
                stack_trace = error.stack_trace,
                timestamp = error.timestamp,
                user_id = null,
                device_id = getDeviceId(),
                app_version = error.app_version,
                os_version = error.os_version,
                activity_id = null,
                steps_to_reproduce = error.steps,
                first_occurred = error.first_occurred,
                occurrence_count = error.count,
                last_occurred = error.last_occurred,
                is_resolved = error.is_resolved,
                resolution_notes = error.notes
            )
        }

        MigrationResult.Success()
    }
}
```

**Step 5: VoiceOS Testing Checkpoint** (Day 4)

**CRITICAL TESTING (must pass before proceeding):**

1. **Functional Tests:**
   - [ ] VoiceOS reads settings from `app_settings` (module='voice')
   - [ ] VoiceOS writes new settings to `app_settings`
   - [ ] VoiceOS reads history from `activity_history` (module='voice')
   - [ ] VoiceOS writes new commands to `activity_history`
   - [ ] VoiceOS writes errors to `error_log`

2. **Data Integrity Tests:**
   - [ ] Checksum verification passed (all tables)
   - [ ] Row count matches (legacy vs new)
   - [ ] Spot check: 10 random settings match
   - [ ] Spot check: 10 random history entries match
   - [ ] Spot check: 10 random errors match

3. **Performance Tests:**
   - [ ] Settings query ≤ 50ms (benchmark)
   - [ ] History query ≤ 100ms (benchmark)
   - [ ] Error query ≤ 50ms (benchmark)

**If ANY test fails:**
- **AUTOMATIC ROLLBACK** (Clarification Q2)
- VoiceOS reverts to legacy tables
- Migration marked as failed
- User notified (no data loss)
- Investigate and retry

**Step 6: Remove VoiceOS Legacy Tables** (Day 4, only if all tests pass)

```sql
-- Drop legacy tables (ONLY after successful migration + testing)
DROP TABLE IF EXISTS settings;
DROP TABLE IF EXISTS user_preference;
DROP TABLE IF EXISTS context_preference;
DROP TABLE IF EXISTS analytics_settings;
DROP TABLE IF EXISTS retention_settings;
DROP TABLE IF EXISTS command_history;
DROP TABLE IF EXISTS command_usage;
DROP TABLE IF EXISTS user_interaction;
DROP TABLE IF EXISTS usage_statistic;
DROP TABLE IF EXISTS error_report;
```

**Success Criteria:**
- [ ] VoiceOS fully migrated to unified database
- [ ] All tests passing (functional, integrity, performance)
- [ ] Legacy tables removed
- [ ] Zero data loss (checksum verified)

---

#### PHASE 1B: AVA Module Migration (Week 1.5)

**Sequential Migration Strategy (AVA after VoiceOS confirmed working):**

**Step 1: AVA Settings Migration** (Day 5)

**Source:** Android SharedPreferences (not in database)

**Migration Logic:**
```kotlin
suspend fun migrateAVASettings(): MigrationResult {
    val sharedPrefs = context.getSharedPreferences("ava_prefs", Context.MODE_PRIVATE)

    return withTransaction {
        // Migrate all SharedPreferences to app_settings
        sharedPrefs.all.forEach { (key, value) ->
            newDb.appSettingsQueries.insertOrReplace(
                id = UUID.randomUUID().toString(),
                module = "chat",
                category = inferCategory(key),  // 'ui', 'performance', etc.
                key = key,
                value = value.toString(),
                type = inferType(value),  // 'boolean', 'int', 'string'
                default_value = null,
                updated_at = System.currentTimeMillis()
            )
        }

        MigrationResult.Success()
    }
}
```

**Step 2: AVA History Migration** (Day 6)

**Tables to Migrate:**
- `conversation` → `activity_history` (module='chat', type='conversation')
- `message` → `activity_history` (module='chat', type='chat_message')

**Migration Logic:**
```kotlin
suspend fun migrateAVAHistory(): MigrationResult {
    return withTransaction {
        // 1. Migrate conversations
        oldDb.conversationQueries.selectAll().forEach { conv ->
            newDb.activityHistoryQueries.insertOrReplace(
                id = conv.id,
                module = "chat",
                activity_type = "conversation",
                timestamp = conv.created_at,
                title = conv.title,
                content = conv.summary ?: "",
                metadata = buildConversationMetadata(conv),
                confidence = null,
                success = 1,
                execution_time_ms = null,
                usage_count = conv.message_count,
                context_id = conv.id,
                user_id = null,
                device_id = getDeviceId(),
                is_incognito = 0,
                created_at = conv.created_at,
                updated_at = conv.updated_at
            )
        }

        // 2. Migrate messages
        oldDb.messageQueries.selectAll().forEach { msg ->
            newDb.activityHistoryQueries.insertOrReplace(
                id = msg.id,
                module = "chat",
                activity_type = "chat_message",
                timestamp = msg.timestamp,
                title = msg.role,  // 'user' or 'assistant'
                content = msg.content,
                metadata = buildMessageMetadata(msg),
                confidence = msg.confidence,
                success = if (msg.error == null) 1 else 0,
                execution_time_ms = msg.inference_time,
                usage_count = 1,
                context_id = msg.conversation_id,
                user_id = null,
                device_id = getDeviceId(),
                is_incognito = 0,
                created_at = msg.timestamp,
                updated_at = msg.timestamp
            )
        }

        MigrationResult.Success()
    }
}
```

**Step 3: AVA Error Migration** (Day 6)

AVA doesn't have existing error table, so just enable error logging:

```kotlin
// Add error interceptor to AVA
class AVAErrorInterceptor(private val errorLogRepository: ErrorLogRepository) {
    suspend fun logError(error: Throwable, context: String) {
        errorLogRepository.insert(
            module = "chat",
            severity = "ERROR",
            errorType = error::class.simpleName ?: "Unknown",
            errorMessage = error.message ?: "",
            stackTrace = error.stackTraceToString(),
            activityId = getCurrentActivityId()
        )
    }
}
```

**Step 4: AVA Testing Checkpoint (CRITICAL)** (Day 7)

**CRITICAL PERFORMANCE VALIDATION (must maintain 0.2s init):**

1. **Performance Tests (REGRESSION DETECTION):**
   - [ ] AVA initialization ≤ 0.2s (CRITICAL - must not regress)
   - [ ] Chat message send ≤ 100ms (write to activity_history)
   - [ ] Conversation load ≤ 50ms (read from activity_history)

2. **Functional Tests:**
   - [ ] AVA reads settings from `app_settings` (module='chat')
   - [ ] AVA writes new settings to `app_settings`
   - [ ] AVA reads conversations from `activity_history`
   - [ ] AVA writes new messages to `activity_history`
   - [ ] AVA writes errors to `error_log`

3. **Data Integrity Tests:**
   - [ ] All conversations migrated (count matches)
   - [ ] All messages migrated (count matches)
   - [ ] Spot check: 10 random conversations match
   - [ ] Spot check: 10 random messages match

**If performance regresses (>0.2s init):**
- **AUTOMATIC ROLLBACK**
- AVA reverts to legacy tables (conversation, message)
- Investigate performance issue
- Retry after optimization

**Step 5: Remove AVA Legacy Tables** (Day 7, only if all tests pass)

```sql
DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS message;
```

**Success Criteria:**
- [ ] AVA fully migrated to unified database
- [ ] AVA initialization ≤ 0.2s (NO REGRESSION)
- [ ] All tests passing
- [ ] Zero data loss

---

#### PHASE 1C: WebAvanue Module Migration (Week 2)

**Sequential Migration Strategy (WebAvanue after AVA confirmed working):**

**Step 1: WebAvanue Settings Migration** (Day 8)

**Tables to Migrate:**
- `browser_settings` (45+ columns) → `app_settings` (module='browser')

**Migration Logic:**
```kotlin
suspend fun migrateWebAvanueSettings(): MigrationResult {
    return withTransaction {
        oldDb.browserSettingsQueries.selectAll().forEach { settings ->
            // Break monolithic settings into key-value pairs
            val settingsMap = mapOf(
                "home_page" to settings.home_page,
                "search_engine" to settings.search_engine,
                "enable_javascript" to settings.enable_javascript,
                "enable_cookies" to settings.enable_cookies,
                "enable_popups" to settings.enable_popups,
                // ... 40+ more fields
            )

            settingsMap.forEach { (key, value) ->
                newDb.appSettingsQueries.insertOrReplace(
                    id = UUID.randomUUID().toString(),
                    module = "browser",
                    category = inferCategory(key),
                    key = key,
                    value = value.toString(),
                    type = inferType(value),
                    default_value = getDefault(key),
                    updated_at = System.currentTimeMillis()
                )
            }
        }

        MigrationResult.Success()
    }
}
```

**Step 2: WebAvanue History Migration** (Day 9)

**Tables to Migrate:**
- `history_entry` → `activity_history` (module='browser', type='web_visit')

**Migration Logic:**
```kotlin
suspend fun migrateWebAvanueHistory(): MigrationResult {
    return withTransaction {
        oldDb.historyEntryQueries.selectAll().forEach { entry ->
            newDb.activityHistoryQueries.insertOrReplace(
                id = entry.id,
                module = "browser",
                activity_type = "web_visit",
                timestamp = entry.visit_time,
                title = entry.title,
                content = entry.url,
                metadata = buildWebMetadata(entry),  // URL, favicon, duration
                confidence = null,
                success = 1,
                execution_time_ms = entry.visit_duration?.toInt(),
                usage_count = entry.visit_count,
                context_id = null,
                user_id = null,
                device_id = getDeviceId(),
                is_incognito = if (entry.is_incognito) 1 else 0,
                created_at = entry.visit_time,
                updated_at = entry.visit_time
            )
        }

        MigrationResult.Success()
    }
}
```

**Step 3: WebAvanue Error Migration** (Day 9)

WebAvanue doesn't have existing error table, so just enable error logging:

```kotlin
// Add error interceptor to WebAvanue
class BrowserErrorInterceptor(private val errorLogRepository: ErrorLogRepository) {
    suspend fun logError(error: Throwable, url: String?) {
        errorLogRepository.insert(
            module = "browser",
            severity = "ERROR",
            errorType = error::class.simpleName ?: "Unknown",
            errorMessage = error.message ?: "",
            stackTrace = error.stackTraceToString(),
            activityId = findActivityByUrl(url)
        )
    }
}
```

**Step 4: WebAvanue Testing Checkpoint** (Day 10)

**Functional Tests:**
- [ ] WebAvanue reads settings from `app_settings` (module='browser')
- [ ] WebAvanue writes new settings to `app_settings`
- [ ] WebAvanue reads history from `activity_history` (module='browser')
- [ ] WebAvanue writes new visits to `activity_history`
- [ ] Incognito mode works (is_incognito=1, not saved)

**Data Integrity Tests:**
- [ ] All history entries migrated (count matches)
- [ ] Spot check: 10 random visits match
- [ ] Spot check: 10 random settings match

**Performance Tests:**
- [ ] History query ≤ 100ms
- [ ] Settings query ≤ 50ms

**Step 5: Remove WebAvanue Legacy Tables** (Day 10, only if all tests pass)

```sql
DROP TABLE IF EXISTS browser_settings;
DROP TABLE IF EXISTS history_entry;
```

**Success Criteria:**
- [ ] WebAvanue fully migrated to unified database
- [ ] All tests passing
- [ ] Zero data loss

---

#### PHASE 2A: AVA ML/AI Migration (Week 2.5)

**Critical: AVA First (validate no regression before VoiceOS)**

**Step 1: Create ML/AI Repositories** (Day 11)

```kotlin
class MLEmbeddingCacheRepository(
    private val database: AugmentalisDatabase,
    private val module: String  // 'chat' for AVA, 'voice' for VoiceOS
) {
    suspend fun getCachedEmbedding(itemId: String, locale: String): ByteArray?
    suspend fun cacheEmbedding(itemId: String, locale: String, vector: ByteArray)
    suspend fun getCacheHitRate(): Double
}

class MLTrainingExampleRepository(
    private val database: AugmentalisDatabase,
    private val module: String
) {
    suspend fun addTrainingExample(inputText: String, targetLabel: String)
    suspend fun searchByFTS(query: String): List<TrainingExample>
}

class MLModelMetadataRepository(
    private val database: AugmentalisDatabase
) {
    suspend fun trackCacheHitRate(module: String, modelType: String, hitRate: Double)
}
```

**Step 2: Migrate AVA Embeddings** (Day 12-13)

**Tables to Migrate:**
- `intent_embedding` → `ml_embedding_cache` (module='chat')
- `embedding_metadata` → `ml_model_metadata` (module='chat')

**Migration Logic:**
```kotlin
suspend fun migrateAVAEmbeddings(): MigrationResult {
    return withTransaction {
        // 1. Calculate checksum (CRITICAL - must preserve embeddings exactly)
        val oldChecksum = calculateEmbeddingChecksum(
            oldDb.intentEmbeddingQueries.selectAll()
        )

        // 2. Migrate embeddings
        oldDb.intentEmbeddingQueries.selectAll().forEach { embedding ->
            newDb.mlEmbeddingCacheQueries.insertOrReplace(
                id = null,  // Auto-increment
                module = "chat",
                item_id = embedding.intent_id,
                item_type = "intent",
                locale = embedding.locale,
                embedding_vector = embedding.embedding_vector,  // BLOB
                embedding_dimension = embedding.embedding_dimension,
                model_version = embedding.model_version,
                normalization_type = embedding.normalization_type,
                created_at = embedding.created_at,
                updated_at = embedding.updated_at,
                example_count = embedding.example_count,
                source = embedding.source
            )
        }

        // 3. Verify checksum (CRITICAL)
        val newChecksum = calculateEmbeddingChecksum(
            newDb.mlEmbeddingCacheQueries.selectByModule("chat")
        )

        if (oldChecksum != newChecksum) {
            throw MigrationException("Embedding checksum mismatch - ROLLBACK")
        }

        // 4. Migrate metadata
        oldDb.embeddingMetadataQueries.selectAll().forEach { meta ->
            newDb.mlModelMetadataQueries.insertOrReplace(
                id = null,
                module = "chat",
                model_type = "EMBEDDING",
                model_id = meta.model_id,
                model_version = meta.model_version,
                model_dimension = meta.dimension,
                quantization = meta.quantization,
                avg_inference_ms = meta.avg_inference_ms,
                cache_hit_rate = meta.cache_hit_rate,
                loaded_at = meta.loaded_at,
                last_used_at = meta.last_used_at,
                usage_count = meta.usage_count,
                created_at = meta.created_at,
                updated_at = meta.updated_at
            )
        }

        MigrationResult.Success()
    }
}
```

**Step 3: Migrate AVA Training Data** (Day 14)

**Tables to Migrate:**
- `intent_example` → `ml_training_example` (module='chat')
- `train_example` → `ml_training_example` (module='chat')

**Migration Logic:**
```kotlin
suspend fun migrateAVATrainingData(): MigrationResult {
    return withTransaction {
        // 1. Migrate intent_example
        oldDb.intentExampleQueries.selectAll().forEach { example ->
            newDb.mlTrainingExampleQueries.insertOrReplace(
                id = null,
                module = "chat",
                input_text = example.utterance,
                target_label = example.intent,
                locale = example.locale,
                source = example.source,
                confidence = example.confidence,
                hash = example.hash,
                created_at = example.created_at,
                updated_at = example.updated_at
            )
        }

        // 2. Migrate train_example
        oldDb.trainExampleQueries.selectAll().forEach { example ->
            newDb.mlTrainingExampleQueries.insertOrReplace(
                id = null,
                module = "chat",
                input_text = example.utterance,
                target_label = example.intent,
                locale = example.locale,
                source = "USER",
                confidence = null,
                hash = example.hash,
                created_at = example.created_at,
                updated_at = example.updated_at
            )
        }

        MigrationResult.Success()
    }
}
```

**Step 4: AVA ML/AI Testing Checkpoint (CRITICAL)** (Day 15)

**CRITICAL PERFORMANCE VALIDATION:**

1. **Embedding Cache Performance:**
   - [ ] Cache hit rate ≥ 95% (CRITICAL - must maintain)
   - [ ] AVA initialization ≤ 0.2s (CRITICAL - must maintain)
   - [ ] Intent recognition ≤ 0.2s (CRITICAL - must maintain)

2. **FTS Search Performance:**
   - [ ] FTS search ≤ 10ms (50x faster than LIKE)
   - [ ] Training example search works correctly

3. **Data Integrity:**
   - [ ] All embeddings migrated (count matches)
   - [ ] Embedding checksums match (BLOB integrity)
   - [ ] All training examples migrated
   - [ ] FTS index populated correctly

**If ANY performance metric fails:**
- **AUTOMATIC ROLLBACK**
- AVA reverts to legacy ML tables
- Investigate performance regression
- DO NOT proceed to VoiceOS migration

**Step 5: Remove AVA Legacy ML Tables** (Day 15, only if all tests pass)

```sql
DROP TABLE IF EXISTS intent_embedding;
DROP TABLE IF EXISTS embedding_metadata;
DROP TABLE IF EXISTS intent_example;
DROP TABLE IF EXISTS train_example;
DROP TABLE IF EXISTS train_example_fts;
```

**Success Criteria:**
- [ ] AVA maintains 95% cache hit rate
- [ ] AVA maintains 0.2s initialization
- [ ] All tests passing
- [ ] Zero data loss

---

#### PHASE 2B: VoiceOS ML/AI Migration (Week 3)

**Goal: Achieve 10-20x Speedup (4-5s → 0.2-0.5s)**

**Step 1: Migrate VoiceOS Learning Data** (Day 16-17)

**Tables to Migrate:**
- `language_model` → `ml_training_example` (module='voice')
- `gesture_learning` → `ml_training_example` (module='voice')
- `recognition_learning` → `ml_training_example` (module='voice')

**Migration Logic:**
```kotlin
suspend fun migrateVoiceOSLearningData(): MigrationResult {
    return withTransaction {
        // 1. Migrate language_model
        oldDb.languageModelQueries.selectAll().forEach { model ->
            newDb.mlTrainingExampleQueries.insertOrReplace(
                id = null,
                module = "voice",
                input_text = model.utterance,
                target_label = model.intent,
                locale = model.locale,
                source = "SCRAPED",
                confidence = model.confidence,
                hash = generateHash(model),
                created_at = model.created_at,
                updated_at = model.updated_at
            )
        }

        // 2. Migrate gesture_learning
        oldDb.gestureLearningQueries.selectAll().forEach { gesture ->
            newDb.mlTrainingExampleQueries.insertOrReplace(
                id = null,
                module = "voice",
                input_text = gesture.gesture_name,
                target_label = gesture.action,
                locale = "en-US",
                source = "USER",
                confidence = gesture.accuracy,
                hash = generateHash(gesture),
                created_at = gesture.created_at,
                updated_at = gesture.updated_at
            )
        }

        // 3. Migrate recognition_learning
        oldDb.recognitionLearningQueries.selectAll().forEach { recog ->
            newDb.mlTrainingExampleQueries.insertOrReplace(
                id = null,
                module = "voice",
                input_text = recog.recognized_text,
                target_label = recog.corrected_text,
                locale = recog.locale,
                source = "CORRECTED",
                confidence = recog.confidence,
                hash = generateHash(recog),
                created_at = recog.created_at,
                updated_at = recog.updated_at
            )
        }

        MigrationResult.Success()
    }
}
```

**Step 2: Enable VoiceOS Embedding Cache** (Day 18)

**Implementation:**
```kotlin
class VoiceOSIntentRecognizer(
    private val embeddingCache: MLEmbeddingCacheRepository,  // NEW - shared cache
    private val trainingData: MLTrainingExampleRepository,   // NEW - shared FTS
    private val modelMetadata: MLModelMetadataRepository     // NEW - track metrics
) {
    suspend fun recognize(utterance: String, locale: String): Intent {
        // 1. Try cache first (95% hit rate expected)
        val cachedEmbedding = embeddingCache.getCachedEmbedding(
            itemId = utterance,
            locale = locale
        )

        if (cachedEmbedding != null) {
            // Cache hit - use cached embedding
            modelMetadata.recordCacheHit(module = "voice")
            return findBestMatch(cachedEmbedding)
        }

        // 2. Cache miss - compute embedding
        modelMetadata.recordCacheMiss(module = "voice")
        val embedding = computeEmbedding(utterance)

        // 3. Cache for next time
        embeddingCache.cacheEmbedding(
            itemId = utterance,
            locale = locale,
            vector = embedding
        )

        return findBestMatch(embedding)
    }
}
```

**Step 3: VoiceOS ML/AI Testing Checkpoint (CRITICAL SPEEDUP)** (Day 19)

**CRITICAL PERFORMANCE VALIDATION (10-20x speedup):**

1. **Initialization Performance (CRITICAL):**
   - [ ] VoiceOS initialization ≤ 0.5s (from 4-5s) - **10x minimum**
   - [ ] Target: 0.2-0.5s (same as AVA)

2. **Cache Performance:**
   - [ ] Cache hit rate ≥ 90% (after warmup)
   - [ ] Cache hit latency ≤ 1ms (BLOB read)
   - [ ] Cache miss latency ≤ 50ms (compute + cache)

3. **FTS Search Performance:**
   - [ ] Training data search ≤ 10ms (FTS4)
   - [ ] Previously: 250ms (LIKE queries)

4. **Data Integrity:**
   - [ ] All training data migrated (count matches)
   - [ ] FTS index populated correctly
   - [ ] VoiceOS accuracy maintained (no regression)

**Benchmarking Script:**
```kotlin
suspend fun benchmarkVoiceOSSpeedup() {
    // 1. Cold start (no cache)
    val coldStartTime = measureTimeMillis {
        voiceOS.initialize()
    }
    println("Cold start: ${coldStartTime}ms (expect ≤500ms)")

    // 2. Warm start (cache populated)
    val warmStartTime = measureTimeMillis {
        voiceOS.initialize()
    }
    println("Warm start: ${warmStartTime}ms (expect ≤200ms)")

    // 3. Cache hit rate
    val hitRate = modelMetadata.getCacheHitRate(module = "voice")
    println("Cache hit rate: ${hitRate * 100}% (expect ≥90%)")

    // 4. Intent recognition latency
    val recognitionTime = measureTimeMillis {
        voiceOS.recognize("open browser", "en-US")
    }
    println("Recognition: ${recognitionTime}ms (expect ≤200ms)")
}
```

**Expected Results:**
- ✅ VoiceOS initialization: **4-5s → 0.2-0.5s** (10-20x faster)
- ✅ Cache hit rate: **≥90%** (after warmup)
- ✅ Intent recognition: **≤200ms** (same as AVA)

**If speedup < 10x:**
- Investigate cache effectiveness
- Check BLOB deserialization performance
- Verify FTS4 indices used
- Retry after optimization

**Step 4: Remove VoiceOS Legacy ML Tables** (Day 19, only if speedup achieved)

```sql
DROP TABLE IF EXISTS language_model;
DROP TABLE IF EXISTS gesture_learning;
DROP TABLE IF EXISTS recognition_learning;
```

**Success Criteria:**
- [ ] VoiceOS initialization ≤ 0.5s (10x faster)
- [ ] Cache hit rate ≥ 90%
- [ ] All tests passing
- [ ] Zero data loss

---

#### PHASE 3: Plugin Extension (Week 3.5)

**Goal: Extend VoiceOS plugin system to browser and chat modules**

**Step 1: Add Module Field to Plugin Tables** (Day 20)

**Schema Migration:**
```sql
-- Add module field (default 'voice' for backward compatibility)
ALTER TABLE plugins ADD COLUMN module TEXT NOT NULL DEFAULT 'voice';
ALTER TABLE plugin_permissions ADD COLUMN module TEXT NOT NULL DEFAULT 'voice';

-- Create indices
CREATE INDEX idx_plugins_module ON plugins(module);
CREATE INDEX idx_plugin_permissions_module ON plugin_permissions(module);
```

**Migration Script:**
```kotlin
suspend fun extendPluginSystem(): MigrationResult {
    return withTransaction {
        // 1. Verify all existing plugins default to 'voice'
        val voicePlugins = database.pluginsQueries.selectByModule("voice").executeAsList()

        if (voicePlugins.isEmpty()) {
            throw MigrationException("No voice plugins found - migration failed")
        }

        // 2. All existing plugins should now have module='voice'
        MigrationResult.Success()
    }
}
```

**Step 2: Create Browser Plugin API** (Day 21)

**Interface:**
```kotlin
interface BrowserPlugin {
    val id: String
    val name: String
    val version: String
    val permissions: List<BrowserPermission>

    // Lifecycle
    suspend fun onInstall()
    suspend fun onEnable()
    suspend fun onDisable()
    suspend fun onUninstall()

    // Browser hooks
    suspend fun onPageLoad(url: String, html: String): String?  // Modify HTML
    suspend fun onRequest(request: HttpRequest): HttpRequest?  // Intercept requests
    suspend fun onResponse(response: HttpResponse): HttpResponse?  // Modify responses
}

enum class BrowserPermission {
    READ_HISTORY,
    MODIFY_HISTORY,
    INTERCEPT_REQUESTS,
    MODIFY_DOM,
    ACCESS_COOKIES
}
```

**Example Plugin (Ad Blocker):**
```kotlin
class AdBlockerPlugin : BrowserPlugin {
    override val id = "com.augmentalis.browser.adblocker"
    override val name = "Ad Blocker"
    override val permissions = listOf(
        BrowserPermission.INTERCEPT_REQUESTS
    )

    override suspend fun onRequest(request: HttpRequest): HttpRequest? {
        // Block known ad domains
        if (isAdDomain(request.url)) {
            return null  // Block request
        }
        return request  // Allow request
    }
}
```

**Step 3: Create Chat Plugin API** (Day 22)

**Interface:**
```kotlin
interface ChatPlugin {
    val id: String
    val name: String
    val version: String
    val permissions: List<ChatPermission>

    // Lifecycle
    suspend fun onInstall()
    suspend fun onEnable()
    suspend fun onDisable()
    suspend fun onUninstall()

    // Chat hooks
    suspend fun onUserMessage(message: String): String?  // Preprocess user input
    suspend fun onAssistantResponse(response: String): String?  // Postprocess AI response
    suspend fun registerActions(): List<AIAction>  // Register custom AI actions
}

data class AIAction(
    val name: String,  // "summarize_pdf"
    val description: String,  // "Summarize a PDF file"
    val parameters: List<ActionParameter>,
    val execute: suspend (parameters: Map<String, Any>) -> String
)

enum class ChatPermission {
    READ_CONVERSATION,
    MODIFY_MESSAGES,
    REGISTER_ACTIONS,
    ACCESS_RAG
}
```

**Example Plugin (PDF Summarizer):**
```kotlin
class PDFSummarizerPlugin : ChatPlugin {
    override val id = "com.augmentalis.chat.pdfsummarizer"
    override val name = "PDF Summarizer"
    override val permissions = listOf(
        ChatPermission.REGISTER_ACTIONS
    )

    override suspend fun registerActions(): List<AIAction> {
        return listOf(
            AIAction(
                name = "summarize_pdf",
                description = "Summarize a PDF file",
                parameters = listOf(
                    ActionParameter("file_path", "string", required = true)
                ),
                execute = { params ->
                    val filePath = params["file_path"] as String
                    val pdfText = extractPDFText(filePath)
                    val summary = summarizeText(pdfText)
                    summary
                }
            )
        )
    }
}
```

**Step 4: Plugin System Testing** (Day 23)

**Tests:**

1. **Backward Compatibility (VoiceOS):**
   - [ ] All existing VoiceOS plugins still work
   - [ ] VoiceOS plugin permissions unchanged
   - [ ] VoiceOS plugin lifecycle unchanged

2. **Browser Plugin Tests:**
   - [ ] Install browser ad blocker plugin
   - [ ] Plugin intercepts requests correctly
   - [ ] Plugin blocks ads
   - [ ] Plugin can be disabled/enabled
   - [ ] Plugin uninstalls cleanly

3. **Chat Plugin Tests:**
   - [ ] Install PDF summarizer plugin
   - [ ] Plugin registers AI action
   - [ ] User can invoke "summarize_pdf" action
   - [ ] Plugin executes correctly
   - [ ] Plugin can be disabled/uninstalled

4. **Cross-Module Plugin Tests:**
   - [ ] Install cross-module plugin (voice → browser)
   - [ ] Voice command "open bookmarked page X"
   - [ ] Plugin executes across modules

**Success Criteria:**
- [ ] VoiceOS plugins work (no regression)
- [ ] Browser plugins installable and functional
- [ ] Chat plugins installable and functional
- [ ] Cross-module plugins work

---

#### PHASE 4: Validation & Optimization (Week 4 - final 0.5 weeks)

**Goal: Final validation, GDPR compliance, rolling consolidation, production monitoring**

**Step 1: Rolling Consolidation Implementation** (Day 24)

**7-Day Consolidation (Clarification Q1):**

```kotlin
class ActivityHistoryConsolidator(
    private val activityRepository: ActivityHistoryRepository
) {
    suspend fun consolidateWeekly() {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)

        // 1. Get all activity from past 7 days
        val allActivity = activityRepository.getActivitySince(sevenDaysAgo)

        // 2. Group by module and activity_type
        val grouped = allActivity.groupBy { it.module to it.activity_type }

        grouped.forEach { (moduleType, activities) ->
            val (module, type) = moduleType

            // 3. Rank by frequency and recency
            val ranked = activities.sortedWith(
                compareByDescending<Activity> { it.usage_count }
                    .thenByDescending { it.timestamp }
            )

            // 4. Keep top 50
            val top50 = ranked.take(50)

            // 5. Delete the rest (95% reduction)
            val toDelete = ranked.drop(50)
            activityRepository.deleteMany(toDelete.map { it.id })

            // 6. Log consolidation
            logger.info("Consolidated $module/$type: kept ${top50.size}, deleted ${toDelete.size}")
        }

        // 7. Save weekly snapshot metadata
        saveWeeklySnapshot(sevenDaysAgo, allActivity.size, top50TotalSize)
    }
}
```

**28-Day Consolidation (Clarification Q1):**

```kotlin
suspend fun consolidateMonthly() {
    val twentyEightDaysAgo = System.currentTimeMillis() - (28 * 24 * 60 * 60 * 1000)

    // 1. Get all weekly snapshots from past 4 weeks
    val weeklySnapshots = getWeeklySnapshots(lastFourWeeks = true)

    // 2. Merge snapshots and rank
    val allItems = weeklySnapshots.flatMap { it.items }
    val ranked = allItems.groupBy { it.item_id }
        .mapValues { (_, items) -> items.sumOf { it.usage_count } }
        .toList()
        .sortedByDescending { it.second }

    // 3. Keep top 50 commands + top 50 websites
    val top50Commands = ranked.filter { isCommand(it.first) }.take(50)
    val top50Websites = ranked.filter { isWebsite(it.first) }.take(50)

    // 4. Delete the rest
    val toKeep = (top50Commands + top50Websites).map { it.first }.toSet()
    activityRepository.deleteOlderThan(twentyEightDaysAgo, keepIds = toKeep)

    // 5. Log monthly consolidation
    logger.info("Monthly consolidation: kept ${toKeep.size}, deleted ${allItems.size - toKeep.size}")
}
```

**Scheduled Consolidation:**
```kotlin
// WorkManager periodic job (Android)
class ConsolidationWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        try {
            // Weekly consolidation (every 7 days)
            if (shouldRunWeeklyConsolidation()) {
                consolidator.consolidateWeekly()
            }

            // Monthly consolidation (every 28 days)
            if (shouldRunMonthlyConsolidation()) {
                consolidator.consolidateMonthly()
            }

            return Result.success()
        } catch (e: Exception) {
            logger.error("Consolidation failed", e)
            return Result.retry()
        }
    }
}

// Schedule weekly
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "weekly_consolidation",
    ExistingPeriodicWorkPolicy.KEEP,
    PeriodicWorkRequestBuilder<ConsolidationWorker>(7, TimeUnit.DAYS).build()
)
```

**Step 2: GDPR Compliance Implementation** (Day 25)

**Consent Dialog (Clarification Q4):**

```kotlin
class GDPRConsentDialog(private val context: Context) {
    fun show(onResult: (ConsentResult) -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Activity Tracking Consent")
            .setMessage("""
                We'd like to track your activity to improve your experience:

                • Voice commands (for personalization)
                • Browsing history (for suggestions)
                • Chat messages (for context)

                You can disable tracking per module in Settings.
                You can export or delete your data anytime.

                Read our Privacy Policy for details.
            """.trimIndent())
            .setPositiveButton("Accept") { _, _ ->
                onResult(ConsentResult.Accepted)
            }
            .setNegativeButton("Decline") { _, _ ->
                onResult(ConsentResult.Declined)
            }
            .setNeutralButton("Privacy Policy") { _, _ ->
                openPrivacyPolicy()
            }
            .show()
    }
}
```

**Data Export (Clarification Q4):**

```kotlin
class GDPRDataExporter(
    private val activityRepository: ActivityHistoryRepository,
    private val settingsRepository: AppSettingsRepository,
    private val errorRepository: ErrorLogRepository
) {
    suspend fun exportAllData(): File {
        val exportData = mapOf(
            "activity_history" to activityRepository.getAll(),
            "settings" to settingsRepository.getAll(),
            "errors" to errorRepository.getAll(),
            "export_date" to System.currentTimeMillis(),
            "user_id" to getUserId()
        )

        // JSON export
        val json = Json.encodeToString(exportData)

        // Save to Downloads
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "ava_data_export_${System.currentTimeMillis()}.json"
        )
        file.writeText(json)

        return file
    }
}
```

**Right to Erasure (Clarification Q4):**

```kotlin
suspend fun deleteAllUserData() {
    withTransaction {
        // 1. Delete all activity history
        activityRepository.deleteAll()

        // 2. Reset settings to defaults
        settingsRepository.resetToDefaults()

        // 3. Delete all errors
        errorRepository.deleteAll()

        // 4. Delete ML/AI data
        mlEmbeddingCacheRepository.deleteUserEmbeddings()
        mlTrainingExampleRepository.deleteUserExamples()

        // 5. Log deletion (for compliance audit)
        logger.info("All user data deleted per GDPR Article 17")
    }
}
```

**Per-Module Consent (Clarification Q4):**

```kotlin
data class ConsentSettings(
    val voiceTrackingEnabled: Boolean,
    val browserTrackingEnabled: Boolean,
    val chatTrackingEnabled: Boolean
)

suspend fun saveActivityIfConsented(activity: Activity) {
    val consent = settingsRepository.getConsentSettings()

    val shouldSave = when (activity.module) {
        "voice" -> consent.voiceTrackingEnabled
        "browser" -> consent.browserTrackingEnabled
        "chat" -> consent.chatTrackingEnabled
        else -> false
    }

    if (shouldSave && !activity.is_incognito) {
        activityRepository.insert(activity)
    }
}
```

**Step 3: Performance Validation** (Day 26)

**Automated Performance Test Suite:**

```kotlin
class PerformanceValidationSuite {
    @Test
    fun `VoiceOS initialization within 500ms`() {
        val time = measureTimeMillis {
            voiceOS.initialize()
        }
        assertThat(time).isLessThan(500)  // 10x faster than 4-5s
    }

    @Test
    fun `AVA initialization within 200ms`() {
        val time = measureTimeMillis {
            ava.initialize()
        }
        assertThat(time).isLessThan(200)  // No regression
    }

    @Test
    fun `Settings query within 50ms`() {
        val time = measureTimeMillis {
            settingsRepository.getByModule("voice")
        }
        assertThat(time).isLessThan(50)  // 3x faster
    }

    @Test
    fun `History query within 100ms`() {
        val time = measureTimeMillis {
            activityRepository.getByModule("browser")
        }
        assertThat(time).isLessThan(100)
    }

    @Test
    fun `FTS search within 10ms`() {
        val time = measureTimeMillis {
            trainingRepository.searchByFTS("open browser")
        }
        assertThat(time).isLessThan(10)  // 25x faster
    }

    @Test
    fun `Embedding cache hit rate at least 95%`() {
        val hitRate = mlModelMetadata.getCacheHitRate(module = "chat")
        assertThat(hitRate).isGreaterThanOrEqualTo(0.95)
    }
}
```

**Step 4: Production Monitoring Setup** (Day 27)

**Metrics to Track:**

```kotlin
class DatabaseMetricsCollector(
    private val database: AugmentalisDatabase
) {
    fun collectMetrics(): DatabaseMetrics {
        return DatabaseMetrics(
            // Performance
            avgQueryLatency = measureAverageQueryLatency(),
            cacheHitRate = mlModelMetadata.getOverallCacheHitRate(),
            databaseSize = getDatabaseSizeBytes(),

            // Data
            totalSettings = settingsQueries.count().executeAsOne(),
            totalActivities = activityQueries.count().executeAsOne(),
            totalErrors = errorQueries.count().executeAsOne(),
            totalEmbeddings = embeddingQueries.count().executeAsOne(),

            // Health
            migrationStatus = getMigrationStatus(),
            consolidationLastRun = getLastConsolidationTimestamp(),
            gdrpCompliance = checkGDPRCompliance()
        )
    }
}

// Log metrics to Firebase Analytics or local dashboard
analyticsLogger.logEvent("database_metrics", metrics.toMap())
```

**Step 5: Documentation and Training** (Day 28 - final half day)

**Documentation Deliverables:**

1. **Developer Migration Guide** (`docs/database-migration-guide.md`)
   - Sequential migration strategy
   - Rollback procedures
   - Testing checkpoints
   - Performance benchmarks

2. **User Privacy Guide** (`docs/user-privacy-guide.md`)
   - GDPR compliance features
   - Data export instructions
   - Consent management
   - Right to erasure

3. **Plugin Developer Guide** (`docs/plugin-developer-guide.md`)
   - Browser plugin API
   - Chat plugin API
   - Cross-module plugins
   - Permission system

4. **Performance Optimization Guide** (`docs/performance-optimization-guide.md`)
   - Embedding cache architecture
   - FTS4 optimization
   - Rolling consolidation
   - Monitoring and metrics

**Success Criteria:**
- [ ] Rolling consolidation working (7-day/28-day)
- [ ] GDPR compliance validated (consent, export, delete)
- [ ] Performance targets met (VoiceOS ≤0.5s, AVA ≤0.2s)
- [ ] Production monitoring enabled
- [ ] Documentation complete

---

## Swarm Mode Configuration

### Swarm: ACTIVATED

**Agents:** 5 specialists + 1 Scrum Master

**Time Savings:** 40% (6 weeks sequential → 3.5 weeks parallel)

---

### Agent Assignments

#### Agent 1: Database Architect (DB-ARCH)

**Domain:** Schema design, migration strategy, data integrity

**Primary Responsibilities:**
- Phase 0: Design all schemas (11 .sq files)
- Phase 0: CREATE TABLE statements, indices, constraints
- Phase 0: FTS4 triggers (DELETE + INSERT pattern)
- Phase 1-2: Checksum verification scripts
- Phase 1-2: Migration sequence validation
- Phase 4: Rolling consolidation SQL queries

**Deliverables:**
- [ ] 11 .sq schema files (app_settings, activity_history, error_log, ml_*)
- [ ] 60+ indices across all tables
- [ ] 3 FTS4 trigger sets (insert, update, delete)
- [ ] Checksum verification utility
- [ ] Migration validation scripts

**Dependencies:**
- None (first agent to start)

**Timeline:**
- Phase 0: 0.5 weeks (schema creation)
- Phase 1-2: 1 week (migration validation support)
- Phase 4: 0.5 weeks (rolling consolidation queries)

---

#### Agent 2: Android Repository Developer (ANDROID-REPO)

**Domain:** Repository pattern, dependency injection, type-safe queries

**Primary Responsibilities:**
- Phase 0: Create database driver (AndroidSqliteDriver)
- Phase 1: Create shared repositories (5 repositories)
- Phase 1-2: Module-filtered query wrappers
- Phase 1-2: Hilt dependency injection setup
- Phase 3: Plugin repository updates (module field)

**Deliverables:**
- [ ] DatabaseDriverFactory (androidMain)
- [ ] AppSettingsRepository (module-filtered)
- [ ] ActivityHistoryRepository (module-filtered)
- [ ] ErrorLogRepository (module-filtered)
- [ ] MLEmbeddingCacheRepository (shared cache)
- [ ] MLTrainingExampleRepository (FTS search)
- [ ] Hilt modules (DI configuration)

**Dependencies:**
- Depends on Agent 1 (schemas must exist first)

**Timeline:**
- Phase 0: 0.5 weeks (driver creation)
- Phase 1: 1.5 weeks (repository creation)
- Phase 2: 0.5 weeks (ML repositories)
- Phase 3: 0.5 weeks (plugin updates)

---

#### Agent 3: ML/AI Specialist (ML-AI)

**Domain:** Embedding cache, FTS optimization, performance benchmarking

**Primary Responsibilities:**
- Phase 2A: Migrate AVA embeddings (maintain 95% hit rate)
- Phase 2A: Verify AVA performance (0.2s init)
- Phase 2B: Migrate VoiceOS learning data
- Phase 2B: Enable VoiceOS embedding cache
- Phase 2B: Benchmark VoiceOS speedup (10-20x)
- Phase 2: FTS4 optimization (ensure 5-10ms search)

**Deliverables:**
- [ ] AVA embedding migration script (zero regression)
- [ ] VoiceOS learning data migration script
- [ ] VoiceOS embedding cache integration
- [ ] Performance benchmarking suite
- [ ] FTS4 optimization validation
- [ ] Cache hit rate tracking

**Dependencies:**
- Depends on Agent 1 (ml_* schemas)
- Depends on Agent 2 (ML repositories)

**Timeline:**
- Phase 2A: 1 week (AVA migration + validation)
- Phase 2B: 0.5 weeks (VoiceOS migration + speedup)

---

#### Agent 4: Data Migration Engineer (DATA-MIGRATE)

**Domain:** Data migration, integrity verification, rollback procedures

**Primary Responsibilities:**
- Phase 1A: VoiceOS settings/history/error migration
- Phase 1B: AVA settings/history/error migration
- Phase 1C: WebAvanue settings/history/error migration
- Phase 1-2: Checksum verification (all phases)
- Phase 1-2: Automatic rollback implementation
- Phase 2: ML/AI data migration support

**Deliverables:**
- [ ] VoiceOS migration scripts (settings, history, errors)
- [ ] AVA migration scripts (settings, history, errors)
- [ ] WebAvanue migration scripts (settings, history, errors)
- [ ] Checksum verification utility
- [ ] Automatic rollback mechanism
- [ ] Migration status tracking

**Dependencies:**
- Depends on Agent 1 (schemas)
- Depends on Agent 2 (repositories)

**Timeline:**
- Phase 1A: 1 week (VoiceOS migration)
- Phase 1B: 0.5 weeks (AVA migration)
- Phase 1C: 0.5 weeks (WebAvanue migration)
- Phase 2: 0.5 weeks (ML migration support)

---

#### Agent 5: Integration & Testing Lead (TEST-LEAD)

**Domain:** Cross-module integration, performance testing, validation

**Primary Responsibilities:**
- Phase 1-3: Create testing checkpoints (all phases)
- Phase 1-3: Functional tests (all modules)
- Phase 1-3: Performance tests (benchmarking)
- Phase 2: Regression tests (AVA must stay 0.2s)
- Phase 4: Production monitoring setup
- Phase 4: Final validation (all success criteria)

**Deliverables:**
- [ ] VoiceOS testing checkpoint (Day 4)
- [ ] AVA testing checkpoint (Day 7, 15)
- [ ] WebAvanue testing checkpoint (Day 10)
- [ ] ML/AI performance tests (Phase 2)
- [ ] Plugin system tests (Phase 3)
- [ ] Production monitoring dashboard (Phase 4)
- [ ] Final validation report (Phase 4)

**Dependencies:**
- Depends on all agents (validates their work)

**Timeline:**
- Phase 1: 1 week (testing VoiceOS, AVA, WebAvanue)
- Phase 2: 1 week (testing ML/AI migration)
- Phase 3: 0.5 weeks (testing plugins)
- Phase 4: 0.5 weeks (final validation)

---

#### Scrum Master Agent (SCRUM-MASTER)

**Domain:** Coordination, conflict resolution, progress tracking

**Primary Responsibilities:**
- Coordinate 5 specialist agents
- Resolve dependencies (ensure Agent 1 completes before 2-4 start)
- Track progress (32 success criteria)
- Identify blockers (performance regressions, migration failures)
- Validate no conflicts (schema changes, repository interfaces)
- Ensure all agents approve final solution
- Daily standups (async status updates)

**Deliverables:**
- [ ] Daily progress reports (agent status)
- [ ] Dependency tracking (critical path)
- [ ] Risk mitigation (identify blockers early)
- [ ] Conflict resolution (schema/API conflicts)
- [ ] Final approval (all agents sign off)

**Dependencies:**
- Coordinates all agents

**Timeline:**
- All phases (continuous coordination)

---

### Swarm Coordination Strategy

**Phase 0 (Week 0.5): Schema Setup**
- Agent 1 (DB-ARCH) → works solo (creates all schemas)
- Agents 2-5 → blocked, waiting for schemas
- **Bottleneck:** Agent 1 must complete before others start

**Phase 1A (Week 1): VoiceOS Migration**
- Agent 1 → validates migration queries
- Agent 2 → creates repositories (parallel with Agent 4)
- Agent 4 → writes migration scripts (parallel with Agent 2)
- Agent 5 → writes test suite (parallel with 2, 4)
- **Parallel work:** 3 agents (2, 4, 5)

**Phase 1B (Week 1.5): AVA Migration**
- Agent 1 → validates ML schema queries
- Agent 2 → updates repositories (parallel with Agent 4)
- Agent 4 → writes AVA migration scripts (parallel with Agent 2)
- Agent 5 → writes AVA test suite (parallel with 2, 4)
- **Parallel work:** 3 agents (2, 4, 5)

**Phase 1C (Week 2): WebAvanue Migration**
- Agent 2 → updates repositories (parallel with Agent 4)
- Agent 4 → writes WebAvanue migration scripts (parallel with Agent 2)
- Agent 5 → writes WebAvanue test suite (parallel with 2, 4)
- **Parallel work:** 3 agents (2, 4, 5)

**Phase 2A (Week 2.5): AVA ML/AI Migration**
- Agent 1 → validates FTS4 triggers
- Agent 2 → creates ML repositories (parallel with Agent 3)
- Agent 3 → migrates AVA embeddings (parallel with Agent 2)
- Agent 4 → supports data migration (parallel with 3)
- Agent 5 → writes ML performance tests (parallel with 2, 3, 4)
- **Parallel work:** 4 agents (2, 3, 4, 5)

**Phase 2B (Week 3): VoiceOS ML/AI Migration**
- Agent 3 → migrates VoiceOS learning data (leads this phase)
- Agent 3 → enables VoiceOS embedding cache
- Agent 4 → supports data migration (parallel with 3)
- Agent 5 → benchmarks VoiceOS speedup (parallel with 3, 4)
- **Parallel work:** 3 agents (3, 4, 5)

**Phase 3 (Week 3.5): Plugin Extension**
- Agent 1 → validates plugin schema changes
- Agent 2 → updates plugin repositories (parallel with Agent 4)
- Agent 4 → writes plugin migration scripts (parallel with Agent 2)
- Agent 5 → writes plugin test suite (parallel with 2, 4)
- **Parallel work:** 3 agents (2, 4, 5)

**Phase 4 (Week 4 - final 0.5 weeks): Validation**
- Agent 1 → creates rolling consolidation queries
- Agent 4 → implements GDPR features (parallel with Agent 1)
- Agent 5 → runs final validation suite (validates all agents)
- Agent 5 → sets up production monitoring
- **Parallel work:** 3 agents (1, 4, 5)

**Scrum Master:** Coordinates all phases, daily async standups

---

## Technical Decisions & Rationales

### Decision 1: Sequential Migration (NO Dual-Write)

**Clarification:** Q3 - User directed: "migrate one app at a time, test all databases concerned and once it is confirmed that all is well, then move to the next module, no need to double write"

**Rationale:**
- **Simplicity:** Eliminates dual-write complexity (no write-to-both-databases logic)
- **Safety:** Only one module at risk at any time (clear rollback boundary)
- **Testing:** Clear testing checkpoint between modules
- **Performance:** No write amplification (dual-write doubles write latency)

**Implementation:**
- VoiceOS → test → confirm → remove legacy → AVA → test → confirm → remove legacy → WebAvanue
- Each module fully validated before next module starts
- Automatic rollback reverts only the failed module (others stay migrated)

**Trade-offs:**
- **Slower rollout:** Cannot release all modules simultaneously
- **Phased release:** May require 2-3 releases (1 module per release)
- **Acceptable:** User confirmed this is preferred over dual-write complexity

---

### Decision 2: Automatic Rollback (Transaction-Based)

**Clarification:** Q2 - User selected: "a" (Automatic rollback)

**Rationale:**
- **Zero user intervention:** If migration fails, automatically revert to legacy tables
- **Data integrity:** Transaction-based migration ensures atomic success/failure
- **Safety:** Checksum verification detects silent data corruption

**Implementation:**
```kotlin
suspend fun migrateWithAutoRollback(
    migrate: suspend () -> MigrationResult
): MigrationResult {
    return withTransaction {
        try {
            // 1. Run migration
            val result = migrate()

            // 2. Verify checksum
            if (!result.checksumMatches) {
                throw MigrationException("Checksum mismatch")
            }

            // 3. Commit transaction
            result
        } catch (e: Exception) {
            // AUTOMATIC ROLLBACK (transaction reverts)
            logger.error("Migration failed, rolling back", e)
            MigrationResult.RolledBack(error = e)
        }
    }
}
```

**Trade-offs:**
- **Automatic only:** User cannot manually approve rollback (fully automated)
- **Acceptable:** User confirmed automatic is preferred (zero intervention)

---

### Decision 3: Rolling Consolidation (7-day/28-day)

**Clarification:** Q1 - User specified: "every 7 days consolidate to the top 50 commands and 50 websites... do this for 4 weeks, then on the 28th day consolidate again"

**Rationale:**
- **Smart summarization:** Keeps favorites (top 50 by frequency/recency)
- **Space efficiency:** 95% storage reduction vs keeping all raw data
- **User-centric:** Preserves user's most important activities
- **Deduplication:** Automatically removes duplicates during consolidation

**Implementation:**
- Weekly: Top 50 commands + top 50 websites (ranked by frequency × recency)
- Monthly: Second-level consolidation across 4 weekly snapshots
- WorkManager periodic job (Android background task)

**Trade-offs:**
- **Data loss:** Deletes bottom 95% of data (older, infrequent items)
- **Acceptable:** User confirmed this is desired behavior (compact history)

---

### Decision 4: GDPR Compliance from Day 1

**Clarification:** Q4 - User selected: "option a" (Full GDPR compliance)

**Rationale:**
- **Legal compliance:** EU regulations require explicit consent, data export, right to erasure
- **User trust:** Privacy-first approach builds user confidence
- **Future-proof:** Avoids costly privacy retrofitting later

**Implementation:**
- **Explicit consent:** Consent dialog on first launch (per-module granular consent)
- **Data export:** One-click export to JSON (GDPR Article 20)
- **Right to erasure:** One-click delete all history (GDPR Article 17)
- **Privacy policy:** Link to privacy policy (transparency)
- **Incognito mode:** Activity not saved (privacy-by-design)

**Trade-offs:**
- **Upfront work:** GDPR implementation in Phase 4 (not deferred)
- **Acceptable:** User confirmed full compliance is required from day 1

---

### Decision 5: VoiceOS First, Then AVA, Then WebAvanue

**Rationale:**
- **VoiceOS first:** Largest codebase (38 tables), most complex, validates migration strategy
- **AVA second:** Critical performance validation (must maintain 0.2s init), ML/AI expertise needed
- **WebAvanue third:** Smallest codebase (6 tables), least risk, final validation

**Alternative Considered:** AVA first (smallest risk)
**Rejected Because:** AVA ML/AI migration is more complex than VoiceOS settings/history, better to validate simpler patterns first

---

### Decision 6: AVA ML/AI Before VoiceOS ML/AI

**Rationale:**
- **Validate no regression:** AVA must maintain 95% cache hit rate and 0.2s init
- **Proven architecture:** AVA's embedding cache is proven, VoiceOS will adopt it
- **Risk mitigation:** If AVA regresses, don't proceed to VoiceOS

**Critical Path:**
- AVA embeddings migrated → verify 95% cache hit → verify 0.2s init → THEN migrate VoiceOS
- If AVA fails: STOP, rollback, investigate, do NOT touch VoiceOS

---

## Quality Gates & Success Criteria

### Phase 0: Schema Setup

**Must Pass:**
- [ ] All 11 .sq files created in commonMain
- [ ] Code generation successful (no compilation errors)
- [ ] All queries type-safe (compile-time validation)
- [ ] ColumnAdapters created and tested

**Performance:**
- N/A (no data yet)

**Rollback:**
- Low risk (schema creation only, no migration)

---

### Phase 1A: VoiceOS Migration

**Must Pass:**
- [ ] VoiceOS reads settings from `app_settings` (module='voice')
- [ ] VoiceOS reads history from `activity_history` (module='voice')
- [ ] VoiceOS writes errors to `error_log` (module='voice')
- [ ] Checksum verification passed (settings, history, errors)
- [ ] Row counts match (legacy vs new)

**Performance:**
- [ ] Settings query ≤ 50ms (3x faster than 150ms)
- [ ] History query ≤ 100ms

**Rollback:**
- If ANY test fails → automatic rollback to VoiceOS legacy tables

---

### Phase 1B: AVA Migration

**Must Pass (CRITICAL - Zero Regression):**
- [ ] AVA initialization ≤ 0.2s (MUST maintain)
- [ ] AVA reads settings from `app_settings` (module='chat')
- [ ] AVA reads conversations from `activity_history` (module='chat')
- [ ] Checksum verification passed

**Performance:**
- [ ] AVA init ≤ 0.2s (NO REGRESSION)
- [ ] Chat message write ≤ 100ms
- [ ] Conversation load ≤ 50ms

**Rollback:**
- If performance regresses (>0.2s) → automatic rollback to AVA legacy tables
- STOP all further migration, investigate regression

---

### Phase 1C: WebAvanue Migration

**Must Pass:**
- [ ] WebAvanue reads settings from `app_settings` (module='browser')
- [ ] WebAvanue reads history from `activity_history` (module='browser')
- [ ] Incognito mode works (is_incognito=1, not saved)
- [ ] Checksum verification passed

**Performance:**
- [ ] History query ≤ 100ms
- [ ] Settings query ≤ 50ms

**Rollback:**
- If ANY test fails → automatic rollback to WebAvanue legacy tables

---

### Phase 2A: AVA ML/AI Migration

**Must Pass (CRITICAL - Zero Regression):**
- [ ] AVA embedding cache hit rate ≥ 95% (MUST maintain)
- [ ] AVA initialization ≤ 0.2s (MUST maintain)
- [ ] Intent recognition ≤ 0.2s (MUST maintain)
- [ ] FTS search ≤ 10ms (50x faster than 250ms)
- [ ] Embedding checksum verification passed (BLOB integrity)

**Performance:**
- [ ] Cache hit rate ≥ 95% (CRITICAL)
- [ ] AVA init ≤ 0.2s (CRITICAL)
- [ ] Intent recognition ≤ 0.2s (CRITICAL)
- [ ] FTS search ≤ 10ms (CRITICAL)

**Rollback:**
- If ANY performance metric fails → automatic rollback to AVA legacy ML tables
- STOP VoiceOS ML migration, investigate regression

---

### Phase 2B: VoiceOS ML/AI Migration

**Must Pass (CRITICAL - 10-20x Speedup):**
- [ ] VoiceOS initialization ≤ 0.5s (from 4-5s) - **10x minimum**
- [ ] Target: 0.2-0.5s (same as AVA)
- [ ] Cache hit rate ≥ 90% (after warmup)
- [ ] FTS search ≤ 10ms
- [ ] VoiceOS accuracy maintained (no regression)

**Performance:**
- [ ] VoiceOS init ≤ 0.5s (CRITICAL - 10x speedup)
- [ ] Cache hit rate ≥ 90% (CRITICAL)
- [ ] Intent recognition ≤ 0.2s (same as AVA)

**Rollback:**
- If speedup < 10x → investigate, optimize, retry
- If accuracy regresses → rollback to legacy ML tables

---

### Phase 3: Plugin Extension

**Must Pass:**
- [ ] VoiceOS plugins still work (no regression)
- [ ] Browser plugin installable and functional
- [ ] Chat plugin installable and functional
- [ ] Cross-module plugin works

**Performance:**
- [ ] Plugin load time ≤ 100ms
- [ ] Plugin execution overhead ≤ 10ms

**Rollback:**
- Low risk (additive only, backward compatible)

---

### Phase 4: Validation & Optimization

**Must Pass:**
- [ ] Rolling consolidation working (7-day/28-day)
- [ ] GDPR consent dialog implemented
- [ ] Data export working (JSON)
- [ ] Right to erasure working (delete all)
- [ ] Production monitoring enabled
- [ ] All performance targets met (VoiceOS ≤0.5s, AVA ≤0.2s)

**Performance:**
- [ ] VoiceOS init ≤ 0.5s (final verification)
- [ ] AVA init ≤ 0.2s (final verification)
- [ ] Settings query ≤ 50ms
- [ ] History query ≤ 100ms
- [ ] FTS search ≤ 10ms
- [ ] Cache hit rate ≥ 95% (AVA), ≥90% (VoiceOS)

**Final Approval:**
- All 5 agents must approve
- Scrum Master validates all success criteria met
- Production monitoring shows stable metrics

---

## Risk Assessment & Mitigation

### Risk 1: AVA Performance Regression

**Severity:** CRITICAL (showstopper)

**Probability:** Medium (complex ML/AI migration)

**Impact:** AVA initialization >0.2s, cache hit rate <95%

**Mitigation:**
- **Preventive:** Agent 3 (ML-AI) validates AVA migration BEFORE VoiceOS
- **Detective:** Automated performance tests (cache hit rate, init time)
- **Corrective:** Automatic rollback to AVA legacy ML tables
- **Escalation:** STOP all migration, investigate regression

**Contingency:**
- If regression detected: STOP, rollback, optimize, retry
- Do NOT proceed to VoiceOS ML migration until AVA validated

---

### Risk 2: VoiceOS Speedup < 10x

**Severity:** HIGH (performance goal not met)

**Probability:** Low (AVA proves 95% cache hit rate achievable)

**Impact:** VoiceOS still 2-4s init (better than 4-5s, but not 10x)

**Mitigation:**
- **Preventive:** Agent 3 (ML-AI) validates cache architecture before VoiceOS migration
- **Detective:** Benchmark VoiceOS init time (cold start, warm start)
- **Corrective:** Investigate cache effectiveness, FTS optimization
- **Escalation:** Retry after optimization

**Contingency:**
- If speedup 5-9x: Acceptable (still significant improvement), proceed
- If speedup < 5x: STOP, investigate, optimize, retry

---

### Risk 3: Data Loss During Migration

**Severity:** CRITICAL (zero data loss requirement)

**Probability:** Very Low (checksum verification, automatic rollback)

**Impact:** User data lost during migration

**Mitigation:**
- **Preventive:** Checksum verification (before/after migration)
- **Detective:** Row count validation, spot checks
- **Corrective:** Automatic rollback on checksum mismatch
- **Escalation:** User notified, migration retried

**Contingency:**
- If checksum fails: AUTOMATIC ROLLBACK (no user intervention)
- If corruption detected: Restore from backup (WorkManager backup job)

---

### Risk 4: Migration Failure (Device Crash, Storage Full)

**Severity:** HIGH (disrupts user experience)

**Probability:** Low (transaction-based migration)

**Impact:** Migration incomplete, database in inconsistent state

**Mitigation:**
- **Preventive:** Transaction-based migration (atomic success/failure)
- **Detective:** Version detection on app restart
- **Corrective:** Automatic rollback to legacy tables
- **Escalation:** User notified, retry on next launch

**Contingency:**
- If crash during migration: Detect on restart → rollback → retry
- If storage full: Rollback → notify user → retry after cleanup

---

### Risk 5: Plugin System Breaking Changes

**Severity:** MEDIUM (VoiceOS plugins may break)

**Probability:** Very Low (additive only, backward compatible)

**Impact:** Existing VoiceOS plugins stop working

**Mitigation:**
- **Preventive:** Add module field with default 'voice' (backward compatible)
- **Detective:** Automated plugin compatibility tests
- **Corrective:** Rollback plugin schema changes
- **Escalation:** Fix plugin code, redeploy

**Contingency:**
- If plugins break: Rollback to legacy plugin tables
- If one plugin breaks: Disable that plugin, keep others working

---

## Testing Strategy

### Unit Tests

**Coverage:** 90%+ on critical paths

**Focus Areas:**
- Repository pattern (module filtering)
- ColumnAdapters (encode/decode)
- Migration utilities (checksum verification)
- GDPR features (consent, export, delete)

**Tools:**
- JUnit 5
- MockK (Kotlin mocking)
- Turbine (Flow testing)

---

### Integration Tests

**Coverage:** All module integrations

**Focus Areas:**
- VoiceOS + unified database
- AVA + unified database
- WebAvanue + unified database
- Cross-module queries (timeline UI)

**Tools:**
- Robolectric (Android database tests)
- Real SQLite database (not mocked)

---

### Performance Tests

**Coverage:** All performance targets

**Focus Areas:**
- VoiceOS initialization ≤ 0.5s
- AVA initialization ≤ 0.2s
- Settings query ≤ 50ms
- History query ≤ 100ms
- FTS search ≤ 10ms
- Cache hit rate ≥ 95% (AVA), ≥90% (VoiceOS)

**Tools:**
- Android Profiler
- Benchmark library (Jetpack)
- Custom timing utilities

---

### Migration Tests

**Coverage:** All migration scripts

**Focus Areas:**
- VoiceOS migration (settings, history, errors)
- AVA migration (settings, history, errors, ML)
- WebAvanue migration (settings, history, errors)
- Checksum verification
- Automatic rollback

**Tools:**
- Custom migration test suite
- Checksum utilities
- Transaction verification

---

### End-to-End Tests

**Coverage:** Complete user workflows

**Focus Areas:**
- User onboarding (GDPR consent)
- Voice command → activity history → timeline
- Browser visit → activity history → timeline
- Chat message → activity history → timeline
- Plugin install → enable → execute → uninstall
- Data export (JSON)
- Delete all data (right to erasure)

**Tools:**
- Espresso (UI tests)
- UIAutomator (cross-app tests)

---

## Deployment Strategy

### Phased Rollout

**Release 1 (VoiceOS Module):**
- VoiceOS migrated to unified database
- Legacy tables removed (settings, history, errors)
- Beta testing (1 week)
- Production release (10% → 50% → 100%)

**Release 2 (AVA Module):**
- AVA migrated to unified database
- AVA ML/AI migrated (maintain 0.2s init)
- Legacy tables removed
- Beta testing (1 week)
- Production release (10% → 50% → 100%)

**Release 3 (WebAvanue Module + Plugins):**
- WebAvanue migrated to unified database
- Plugin system extended (browser, chat)
- Legacy tables removed
- Beta testing (1 week)
- Production release (10% → 50% → 100%)

**Release 4 (GDPR + Optimization):**
- Rolling consolidation enabled
- GDPR features enabled (consent, export, delete)
- Production monitoring enabled
- Full release (100%)

---

### Rollback Plan

**Per-Module Rollback:**
- If Release 1 fails: Rollback VoiceOS to legacy tables
- If Release 2 fails: Rollback AVA to legacy tables (VoiceOS stays migrated)
- If Release 3 fails: Rollback WebAvanue to legacy tables (VoiceOS + AVA stay migrated)

**Version Detection:**
```kotlin
fun detectMigrationStatus(): MigrationStatus {
    return when {
        hasUnifiedDatabase() && voiceOSMigrated() -> MigrationStatus.VOICEOS_COMPLETE
        hasUnifiedDatabase() && avaMigrated() -> MigrationStatus.AVA_COMPLETE
        hasUnifiedDatabase() && webavanueMigrated() -> MigrationStatus.WEBAVANUE_COMPLETE
        else -> MigrationStatus.NOT_STARTED
    }
}
```

**Automatic Rollback:**
- Transaction-based migration (atomic)
- If migration fails → automatic rollback to legacy tables
- User notified (toast message)
- Migration retried on next launch

---

## Monitoring & Observability

### Key Metrics

**Performance Metrics:**
- VoiceOS initialization time (target: ≤0.5s)
- AVA initialization time (target: ≤0.2s)
- Settings query latency (target: ≤50ms)
- History query latency (target: ≤100ms)
- FTS search latency (target: ≤10ms)
- Cache hit rate (target: ≥95% AVA, ≥90% VoiceOS)

**Data Metrics:**
- Total settings (all modules)
- Total activities (all modules)
- Total errors (all modules)
- Total embeddings (all modules)
- Database size (MB)
- Storage savings (rolling consolidation)

**Health Metrics:**
- Migration status (per module)
- Migration failures (count, reasons)
- Rollback events (count, modules)
- GDPR consent rate (%)
- Data export requests (count)
- Data deletion requests (count)

**Logging:**
- Migration events (start, success, failure, rollback)
- Performance benchmarks (init time, query latency)
- Error tracking (migration errors, database errors)
- User actions (consent, export, delete)

**Dashboards:**
- Firebase Analytics (production metrics)
- Local database metrics (debug builds)
- Crash reporting (Firebase Crashlytics)

---

## Documentation Deliverables

### Developer Documentation

1. **Database Migration Guide** (`docs/database-migration-guide.md`)
   - Sequential migration strategy
   - Module-by-module migration steps
   - Checksum verification procedures
   - Automatic rollback procedures
   - Testing checkpoints

2. **Repository Pattern Guide** (`docs/repository-pattern-guide.md`)
   - Shared repository architecture
   - Module filtering patterns
   - Hilt dependency injection
   - Type-safe query wrappers

3. **ML/AI Integration Guide** (`docs/ml-ai-integration-guide.md`)
   - Embedding cache architecture
   - FTS4 optimization
   - Performance benchmarking
   - Cache hit rate tracking

4. **Plugin Developer Guide** (`docs/plugin-developer-guide.md`)
   - Browser plugin API
   - Chat plugin API
   - Cross-module plugins
   - Permission system
   - Plugin lifecycle

---

### User Documentation

1. **Privacy Guide** (`docs/user-privacy-guide.md`)
   - GDPR compliance features
   - Consent management
   - Data export instructions
   - Right to erasure instructions
   - Per-module consent controls

2. **Activity Timeline Guide** (`docs/activity-timeline-guide.md`)
   - Unified timeline UI
   - Module filtering
   - Search functionality
   - Retention policy explanation

3. **Plugin Marketplace Guide** (`docs/plugin-marketplace-guide.md`)
   - How to install plugins
   - Browser extensions (ad blockers, password managers)
   - Chat plugins (custom AI actions)
   - Cross-module plugins

---

## Success Metrics (Final Validation)

### Phase 1 Success (Core Consolidation)

- [ ] All 3 modules migrated (VoiceOS, AVA, WebAvanue)
- [ ] Settings queries 3x faster (≤50ms vs 150ms)
- [ ] Zero data loss (checksum verification passed)
- [ ] Sequential migration completed (no dual-write)
- [ ] Automatic rollback tested (works correctly)

### Phase 2 Success (ML/AI Consolidation)

- [ ] AVA maintains 95% cache hit rate
- [ ] AVA maintains 0.2s initialization (no regression)
- [ ] VoiceOS achieves 10-20x speedup (4-5s → 0.2-0.5s)
- [ ] FTS search 50x faster (250ms → 5ms)
- [ ] Shared embedding cache working (AVA + VoiceOS)

### Phase 3 Success (Plugin Extension)

- [ ] VoiceOS plugins still work (no regression)
- [ ] Browser plugins installable and functional
- [ ] Chat plugins installable and functional
- [ ] Cross-module plugins work

### Phase 4 Success (Validation & Optimization)

- [ ] Rolling consolidation working (7-day/28-day)
- [ ] GDPR compliance validated (consent, export, delete)
- [ ] Performance targets met (VoiceOS ≤0.5s, AVA ≤0.2s)
- [ ] Production monitoring enabled
- [ ] Documentation complete
- [ ] All 32 success criteria met

---

## Timeline Summary

**Phase 0: Schema Setup** - Week 0.5 (3.5 days)
- Agent 1 creates all schemas
- Agents 2-5 prepare tools

**Phase 1A: VoiceOS Migration** - Week 1 (5 days)
- Agents 2, 4, 5 work in parallel
- Testing checkpoint (Day 4)

**Phase 1B: AVA Migration** - Week 1.5 (3.5 days)
- Agents 2, 4, 5 work in parallel
- CRITICAL testing checkpoint (Day 7)

**Phase 1C: WebAvanue Migration** - Week 2 (3 days)
- Agents 2, 4, 5 work in parallel
- Testing checkpoint (Day 10)

**Phase 2A: AVA ML/AI Migration** - Week 2.5 (5 days)
- Agents 2, 3, 4, 5 work in parallel
- CRITICAL testing checkpoint (Day 15)

**Phase 2B: VoiceOS ML/AI Migration** - Week 3 (4 days)
- Agents 3, 4, 5 work in parallel
- CRITICAL speedup validation (Day 19)

**Phase 3: Plugin Extension** - Week 3.5 (4 days)
- Agents 2, 4, 5 work in parallel
- Testing checkpoint (Day 23)

**Phase 4: Validation & Optimization** - Week 4 (4 days, final 0.5 weeks)
- Agents 1, 4, 5 work in parallel
- Final validation (Day 28)

**Total Duration:** 3.5 weeks (with swarm mode) vs 6 weeks (sequential)

**Time Savings:** 40%

---

## Workflow Chaining

**Current Position:** You have completed /specify (specification) and /clarify (ambiguity resolution).

**Next Steps:**

1. **Option 1: Continue to /tasks** (Detailed Task Breakdown)
   - Command: `/tasks docs/ideacode/specs/012-database-consolidation-merge/plan.md`
   - Creates detailed task list for implementation
   - Breaks down each phase into granular tasks
   - Assigns tasks to swarm agents

2. **Option 2: Continue to /implement** (Direct to Coding)
   - Command: `/implement docs/ideacode/specs/012-database-consolidation-merge/plan.md`
   - Activates swarm mode (5 agents)
   - Starts coding Phase 0 (schema setup)
   - Automatic progression through all phases

3. **Option 3: Manual Review**
   - Review this plan document
   - Provide feedback or adjustments
   - Continue when ready

**Recommended:** Option 1 (/tasks) for detailed task breakdown before coding.

---

**Plan Status:** ✅ COMPLETE
**Ready for:** /tasks (task breakdown) or /implement (direct to code)
**Estimated Completion:** 3.5 weeks (with swarm mode)
**Success Probability:** HIGH (proven patterns, sequential migration, automatic rollback)

---

**Last Updated:** 2025-11-28
**Plan Version:** 1.0.0
**Specification:** docs/ideacode/specs/012-database-consolidation-merge/spec.md
