# Database Consolidation - Task Breakdown

**Feature ID:** 012
**Feature Name:** Cross-Repository Database Consolidation (VoiceOS/AVA/Avanues Merge)
**Created:** 2025-11-28
**Platforms:** Shared/KMP, Android (VoiceOS, AVA, WebAvanue)
**Total Tasks:** 87 across 7 phases
**Estimated Effort:** 140 hours (3.5 weeks with swarm) | 240 hours (6 weeks sequential)
**Swarm Mode:** RECOMMENDED (5 specialist agents + Scrum Master)

---

## Task Summary

### By Platform/Module

| Platform/Module | Tasks | Hours | Agent | Phase | Parallelizable |
|-----------------|-------|-------|-------|-------|----------------|
| Shared/KMP | 9 | 18h | DB Architect | 0 | No (foundation) |
| Android (VoiceOS) | 16 | 32h | Data Migration Eng | 1A | Partial (with testing) |
| Android (AVA) | 14 | 28h | Data Migration Eng | 1B | Partial (with testing) |
| Android (WebAvanue) | 10 | 20h | Data Migration Eng | 1C | Partial (with testing) |
| ML/AI (AVA) | 12 | 24h | ML/AI Specialist | 2A | Partial (with testing) |
| ML/AI (VoiceOS) | 10 | 20h | ML/AI Specialist | 2B | Partial (with testing) |
| Plugins (All) | 8 | 16h | Android Repo Dev | 3 | Yes (with testing) |
| Validation (All) | 8 | 16h | Test Lead | 4 | Yes (parallel per module) |
| **Total** | **87** | **174h** | **5 agents** | **7 phases** | **40% parallel** |

### By Phase

| Phase | Platform/Module | Tasks | Hours | Dependencies | Parallelizable |
|-------|-----------------|-------|-------|--------------|----------------|
| 0 | Shared/KMP Foundation | 9 | 18h | None (blocking all) | No |
| 1A | Android (VoiceOS) | 16 | 32h | Phase 0 complete | Partial |
| 1B | Android (AVA) | 14 | 28h | Phase 1A complete | Partial |
| 1C | Android (WebAvanue) | 10 | 20h | Phase 1B complete | Partial |
| 2A | ML/AI (AVA) | 12 | 24h | Phase 1B complete | Partial |
| 2B | ML/AI (VoiceOS) | 10 | 20h | Phase 2A complete | Partial |
| 3 | Plugins (All) | 8 | 16h | Phase 1C complete | Yes |
| 4 | Validation (All) | 8 | 16h | Phases 2B, 3 complete | Yes |

### Parallel Execution Summary

**Sequential Execution:** 240 hours (6 weeks)
**Parallel Execution (Swarm):** 140 hours (3.5 weeks)
**Time Savings:** 100 hours (42% reduction)

---

## PHASE 0: Shared/KMP Foundation (Week 0.5 - 18 hours)

**Platform:** Shared/KMP (commonMain)
**Dependencies:** None (foundation phase - BLOCKING all others)
**Agent:** Database Architect
**Parallelizable:** No (sequential, must complete before any module migration)

---

### Task S01: Configure SQLDelight Plugin

**Description:** Add and configure SQLDelight 2.0.1 Gradle plugin in Core/Data module

**Agent:** Database Architect
**Estimated Time:** 1 hour
**Complexity:** Tier 1 (Simple)
**Dependencies:** None
**Blocks:** S02, S03, S04, S05, S06, S07, S08, S09

**Quality Gates:**
- [ ] SQLDelight plugin version 2.0.1 configured
- [ ] Database name: "AugmentalisDatabase"
- [ ] Package: "com.augmentalis.core.data.db"
- [ ] SQLite 3.38 dialect specified
- [ ] Gradle sync successful

**Files to Modify:**
- `Universal/AVA/Core/Data/build.gradle.kts`

**Implementation Steps:**
1. Add SQLDelight plugin to plugins block
2. Configure sqldelight {} block with database settings
3. Set schema output directory
4. Enable deriveSchemaFromMigrations
5. Sync Gradle project

---

### Task S02: Create app_settings Schema

**Description:** Create app_settings.sq schema file for unified settings system (module-based)

**Agent:** Database Architect
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** S01
**Blocks:** S09, V01, A01, W01

**Quality Gates:**
- [ ] Table created with 8 columns (id, module, category, key, value, type, default_value, updated_at)
- [ ] UNIQUE constraint on (module, category, key)
- [ ] 2 indices created (module, category)
- [ ] INSERT OR REPLACE query defined
- [ ] SELECT BY MODULE query defined
- [ ] Code generation successful

**Files to Create:**
- `Universal/AVA/Core/Data/src/commonMain/sqldelight/com/augmentalis/core/data/db/AppSettings.sq`

**Schema Requirements:**
```sql
CREATE TABLE app_settings (
    id TEXT PRIMARY KEY NOT NULL,
    module TEXT NOT NULL,        -- 'voice', 'browser', 'chat', 'system'
    category TEXT NOT NULL,      -- 'privacy', 'performance', 'ui', 'analytics'
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    type TEXT NOT NULL,          -- 'boolean', 'int', 'string', 'json'
    default_value TEXT,
    updated_at INTEGER NOT NULL,
    UNIQUE(module, category, key)
);
```

---

### Task S03: Create activity_history Schema

**Description:** Create activity_history.sq schema file for unified activity tracking (voice, browser, chat)

**Agent:** Database Architect
**Estimated Time:** 3 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** S01
**Blocks:** S09, V02, A02, W02

**Quality Gates:**
- [ ] Table created with 16 columns (module-based design)
- [ ] 4 indices created (module, activity_type, timestamp, context_id)
- [ ] INSERT OR REPLACE query defined
- [ ] SELECT BY MODULE query defined
- [ ] SELECT TIMELINE query defined (all modules, sorted by timestamp)
- [ ] Code generation successful

**Files to Create:**
- `Universal/AVA/Core/Data/src/commonMain/sqldelight/com/augmentalis/core/data/db/ActivityHistory.sq`

**Schema Requirements:**
- Support for 3 modules: voice, browser, chat
- Metadata field (JSON) for module-specific data
- Privacy support (is_incognito field)
- Analytics fields (confidence, success, execution_time_ms, usage_count)

---

### Task S04: Create error_log Schema

**Description:** Create error_log.sq schema file for unified error tracking (all modules)

**Agent:** Database Architect
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** S01
**Blocks:** S09, V03, A03, W03

**Quality Gates:**
- [ ] Table created with 14 columns
- [ ] 4 indices created (module, severity, timestamp, is_resolved)
- [ ] INSERT OR REPLACE query defined
- [ ] SELECT BY MODULE query defined
- [ ] SELECT UNRESOLVED query defined
- [ ] Code generation successful

**Files to Create:**
- `Universal/AVA/Core/Data/src/commonMain/sqldelight/com/augmentalis/core/data/db/ErrorLog.sq`

**Schema Requirements:**
- Support for severity levels (DEBUG, INFO, WARNING, ERROR, FATAL)
- Stack trace storage
- Activity correlation (optional FK to activity_history)
- Deduplication via occurrence_count

---

### Task S05: Create ml_embedding_cache Schema

**Description:** Create ml_embedding_cache.sq schema file for shared embedding cache (AVA proven design)

**Agent:** Database Architect
**Estimated Time:** 3 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** S01
**Blocks:** S09, ML01

**Quality Gates:**
- [ ] Table created with 11 columns
- [ ] UNIQUE constraint on (module, item_id, locale)
- [ ] 2 indices created (unique composite, model_version)
- [ ] BLOB column for embedding_vector
- [ ] INSERT OR REPLACE query defined
- [ ] SELECT BY MODULE query defined
- [ ] Code generation successful

**Files to Create:**
- `Universal/AVA/Core/Data/src/commonMain/sqldelight/com/augmentalis/core/data/db/MLEmbeddingCache.sq`

**Schema Requirements:**
- BLOB storage for embeddings (ByteArray)
- Dual model support (384 or 768 dimensions)
- Quantized embedding support (INT8)
- Model version tracking

---

### Task S06: Create ml_training_example Schema with FTS4

**Description:** Create ml_training_example.sq and ml_training_example_fts.sq for unified training data with full-text search

**Agent:** Database Architect
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** S01
**Blocks:** S09, ML02

**Quality Gates:**
- [ ] Main table created with 9 columns
- [ ] FTS4 virtual table created (content='ml_training_example')
- [ ] 3 FTS4 triggers created (insert, update, delete)
- [ ] UPDATE trigger uses DELETE + INSERT pattern (FTS4 requirement)
- [ ] UNIQUE constraint on hash (deduplication)
- [ ] FTS SEARCH query defined
- [ ] Code generation successful

**Files to Create:**
- `Universal/AVA/Core/Data/src/commonMain/sqldelight/com/augmentalis/core/data/db/MLTrainingExample.sq`
- `Universal/AVA/Core/Data/src/commonMain/sqldelight/com/augmentalis/core/data/db/MLTrainingExampleFts.sq`

**Schema Requirements:**
- FTS4 full-text search (50-100x faster than LIKE)
- Hash-based deduplication
- Module-based filtering
- 3 synchronized FTS triggers

---

### Task S07: Create ml_model_metadata Schema

**Description:** Create ml_model_metadata.sq schema file for model version tracking and cache metrics

**Agent:** Database Architect
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** S01
**Blocks:** S09, ML03

**Quality Gates:**
- [ ] Table created with 12 columns
- [ ] UNIQUE constraint on (module, model_type, model_id)
- [ ] Performance metrics fields (avg_inference_ms, cache_hit_rate)
- [ ] INSERT OR REPLACE query defined
- [ ] SELECT BY MODULE query defined
- [ ] Code generation successful

**Files to Create:**
- `Universal/AVA/Core/Data/src/commonMain/sqldelight/com/augmentalis/core/data/db/MLModelMetadata.sq`

**Schema Requirements:**
- Track cache effectiveness (cache_hit_rate)
- Model lifecycle (loaded_at, last_used_at, usage_count)
- Quantization tracking (INT8, FP16, FP32)

---

### Task S08: Create ColumnAdapters

**Description:** Create ColumnAdapters.kt for complex type conversions (List<String>, Boolean, ByteArray)

**Agent:** Database Architect
**Estimated Time:** 1 hour
**Complexity:** Tier 1 (Simple)
**Dependencies:** S01
**Blocks:** S09

**Quality Gates:**
- [ ] StringListAdapter created (List<String> ↔ String via JSON)
- [ ] BooleanAdapter created (Boolean ↔ Long)
- [ ] ByteArrayAdapter created if needed
- [ ] All adapters have encode() and decode() methods
- [ ] Unit tests created (encode/decode round-trip)

**Files to Create:**
- `Universal/AVA/Core/Data/src/commonMain/kotlin/com/augmentalis/core/data/db/ColumnAdapters.kt`

---

### Task S09: Generate SQLDelight Code and Validate

**Description:** Run code generation and validate all schemas compile successfully

**Agent:** Database Architect
**Estimated Time:** 0.5 hours
**Complexity:** Tier 1 (Simple)
**Dependencies:** S01, S02, S03, S04, S05, S06, S07, S08
**Blocks:** All Phase 1, 2, 3, 4 tasks

**Quality Gates:**
- [ ] Gradle task successful: `generateCommonMainAugmentalisDatabaseInterface`
- [ ] No compilation errors
- [ ] All query interfaces generated (AppSettingsQueries, ActivityHistoryQueries, etc.)
- [ ] Database interface generated (AugmentalisDatabase.kt)
- [ ] All indices created correctly
- [ ] All UNIQUE constraints enforced

**Command:**
```bash
./gradlew :Universal:AVA:Core:Data:generateCommonMainAugmentalisDatabaseInterface
```

**Success Criteria:**
- BUILD SUCCESSFUL
- 11 query classes generated (7 schemas + 1 FTS)
- Type-safe query methods available

---

## PHASE 1A: VoiceOS Module Migration (Week 1 - 32 hours)

**Platform:** Android (VoiceOS module)
**Dependencies:** Phase 0 complete (S09)
**Agent:** Data Migration Engineer + Test Lead
**Parallelizable:** Partial (migration tasks sequential, testing parallel)

---

### Task V01: Create VoiceOS Migration Utilities

**Description:** Create database driver, repositories, and migration utility classes for VoiceOS

**Agent:** Android Repository Developer
**Estimated Time:** 3 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** S09
**Blocks:** V02, V03, V04, V05, V06, V07

**Quality Gates:**
- [ ] AndroidDatabaseDriverFactory created (androidMain)
- [ ] AppSettingsRepository created with module filtering
- [ ] ActivityHistoryRepository created with module filtering
- [ ] ErrorLogRepository created with module filtering
- [ ] VoiceOSMigrationUtility created with checksum verification
- [ ] Hilt modules configured for DI

**Files to Create:**
- `Universal/AVA/Core/Data/src/androidMain/kotlin/com/augmentalis/core/data/db/DatabaseDriverFactory.kt`
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/repository/AppSettingsRepository.kt`
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/repository/ActivityHistoryRepository.kt`
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/repository/ErrorLogRepository.kt`
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/migration/VoiceOSMigrationUtility.kt`

---

### Task V02: Migrate VoiceOS Settings Tables

**Description:** Migrate 5 VoiceOS settings tables to app_settings (module='voice')

**Agent:** Data Migration Engineer
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** V01
**Blocks:** V07, V08

**Quality Gates:**
- [ ] 5 tables migrated (settings, user_preference, context_preference, analytics_settings, retention_settings)
- [ ] Checksum verification passed (before/after match)
- [ ] Row count verification passed
- [ ] Module field = 'voice' for all rows
- [ ] Category field correctly inferred (system, user, context, analytics, retention)
- [ ] Type field correctly inferred (boolean, int, string, json)
- [ ] Transaction-based migration (automatic rollback on failure)

**Migration Tables:**
- `settings` → `app_settings` (category='system')
- `user_preference` → `app_settings` (category='user')
- `context_preference` → `app_settings` (category='context')
- `analytics_settings` → `app_settings` (category='analytics')
- `retention_settings` → `app_settings` (category='retention')

**Testing:**
- [ ] Spot check: 10 random settings match legacy values
- [ ] VoiceOS reads settings from app_settings successfully
- [ ] VoiceOS writes new settings to app_settings successfully

---

### Task V03: Migrate VoiceOS History Tables

**Description:** Migrate 4 VoiceOS history tables to activity_history (module='voice')

**Agent:** Data Migration Engineer
**Estimated Time:** 5 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** V01
**Blocks:** V07, V08

**Quality Gates:**
- [ ] 4 tables migrated (command_history, command_usage, user_interaction, usage_statistic)
- [ ] Checksum verification passed
- [ ] Row count verification passed
- [ ] Module field = 'voice' for all rows
- [ ] Activity_type field correctly set (command, command_usage, interaction, statistic)
- [ ] Metadata field contains VoiceOS-specific data (JSON)
- [ ] Transaction-based migration

**Migration Tables:**
- `command_history` → `activity_history` (type='command')
- `command_usage` → `activity_history` (type='command_usage')
- `user_interaction` → `activity_history` (type='interaction')
- `usage_statistic` → `activity_history` (type='statistic')

**Metadata JSON Example:**
```json
{
  "originalText": "open browser",
  "processedCommand": "com.augmentalis.browser.LAUNCH",
  "language": "en-US",
  "engineUsed": "GEMINI_NANO"
}
```

**Testing:**
- [ ] Spot check: 10 random history entries match legacy values
- [ ] VoiceOS reads history from activity_history successfully
- [ ] VoiceOS writes new commands to activity_history successfully

---

### Task V04: Migrate VoiceOS Error Table

**Description:** Migrate error_report table to error_log (module='voice')

**Agent:** Data Migration Engineer
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** V01
**Blocks:** V07, V08

**Quality Gates:**
- [ ] error_report table migrated to error_log
- [ ] Checksum verification passed
- [ ] Row count verification passed
- [ ] Module field = 'voice' for all rows
- [ ] All fields mapped correctly (severity, error_type, stack_trace, etc.)
- [ ] Transaction-based migration

**Migration:**
- `error_report` → `error_log` (module='voice')

**Testing:**
- [ ] Spot check: 10 random errors match legacy values
- [ ] VoiceOS writes new errors to error_log successfully

---

### Task V05: Create VoiceOS Functional Tests

**Description:** Create functional test suite for VoiceOS migration (read/write validation)

**Agent:** Integration & Testing Lead
**Estimated Time:** 4 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** V02, V03, V04
**Blocks:** V07

**Quality Gates:**
- [ ] Test: VoiceOS reads settings from app_settings (module='voice')
- [ ] Test: VoiceOS writes new settings to app_settings
- [ ] Test: VoiceOS reads history from activity_history (module='voice')
- [ ] Test: VoiceOS writes new commands to activity_history
- [ ] Test: VoiceOS writes errors to error_log
- [ ] Test: Module filtering works (only 'voice' data visible to VoiceOS)
- [ ] All tests passing

**Files to Create:**
- `Universal/AVA/Core/Data/src/androidTest/kotlin/com/augmentalis/core/data/migration/VoiceOSMigrationTest.kt`

---

### Task V06: Create VoiceOS Performance Tests

**Description:** Create performance benchmark tests for VoiceOS (settings/history query latency)

**Agent:** Integration & Testing Lead
**Estimated Time:** 3 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** V02, V03, V04
**Blocks:** V07

**Quality Gates:**
- [ ] Benchmark: Settings query ≤ 50ms (3x faster than 150ms baseline)
- [ ] Benchmark: History query ≤ 100ms
- [ ] Benchmark: Error query ≤ 50ms
- [ ] Performance regression detection enabled
- [ ] All benchmarks passing

**Files to Create:**
- `Universal/AVA/Core/Data/src/androidTest/kotlin/com/augmentalis/core/data/migration/VoiceOSPerformanceTest.kt`

---

### Task V07: VoiceOS Testing Checkpoint (CRITICAL)

**Description:** Run all VoiceOS tests and validate migration success (BLOCKING)

**Agent:** Integration & Testing Lead
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** V02, V03, V04, V05, V06
**Blocks:** V08

**Quality Gates (MUST PASS - Automatic Rollback if Fails):**
- [ ] Functional tests: 100% passing
- [ ] Performance tests: All targets met (≤50ms settings, ≤100ms history)
- [ ] Data integrity: Checksum verification passed (all 3 migrations)
- [ ] Row count: Matches legacy tables exactly
- [ ] Spot checks: 10 random settings, 10 random history, 10 random errors match
- [ ] VoiceOS module: Fully operational with unified database

**Rollback Trigger:**
- If ANY test fails → AUTOMATIC ROLLBACK to VoiceOS legacy tables
- Migration marked as failed
- User notified (no data loss)
- Investigation required before retry

**Command:**
```bash
./gradlew :Universal:AVA:Core:Data:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.augmentalis.core.data.migration.VoiceOSMigrationTest
```

---

### Task V08: Remove VoiceOS Legacy Tables

**Description:** Drop VoiceOS legacy tables (ONLY if V07 passes all tests)

**Agent:** Data Migration Engineer
**Estimated Time:** 1 hour
**Complexity:** Tier 1 (Simple)
**Dependencies:** V07
**Blocks:** None (Phase 1A complete)

**Quality Gates:**
- [ ] V07 passed all tests (mandatory prerequisite)
- [ ] Legacy tables dropped (10 tables)
- [ ] VoiceOS still functional after drop (verification test)
- [ ] No references to legacy tables in VoiceOS code

**Tables to Drop:**
```sql
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

**Safety Check:**
- Run VoiceOS smoke test after drop
- If smoke test fails → restore from backup (automatic)

---

## PHASE 1B: AVA Module Migration (Week 1.5 - 28 hours)

**Platform:** Android (AVA module)
**Dependencies:** Phase 1A complete (V08)
**Agent:** Data Migration Engineer + Test Lead
**Parallelizable:** Partial (migration tasks sequential, testing parallel)

---

### Task A01: Migrate AVA Settings (SharedPreferences)

**Description:** Migrate AVA SharedPreferences to app_settings (module='chat')

**Agent:** Data Migration Engineer
**Estimated Time:** 3 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** V08
**Blocks:** A04, A05

**Quality Gates:**
- [ ] All SharedPreferences migrated to app_settings
- [ ] Module field = 'chat' for all rows
- [ ] Category field correctly inferred from key names
- [ ] Type field correctly inferred from value types
- [ ] No data loss (all preferences present)
- [ ] Transaction-based migration

**Migration:**
- Android SharedPreferences ("ava_prefs") → `app_settings` (module='chat')

**Testing:**
- [ ] AVA reads settings from app_settings successfully
- [ ] AVA writes new settings to app_settings successfully

---

### Task A02: Migrate AVA History Tables (Conversation/Message)

**Description:** Migrate conversation and message tables to activity_history (module='chat')

**Agent:** Data Migration Engineer
**Estimated Time:** 5 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** V08
**Blocks:** A04, A05

**Quality Gates:**
- [ ] 2 tables migrated (conversation, message)
- [ ] Checksum verification passed
- [ ] Row count verification passed
- [ ] Module field = 'chat' for all rows
- [ ] Activity_type field correctly set (conversation, chat_message)
- [ ] Metadata field contains AVA-specific data (JSON)
- [ ] Foreign key relationship preserved (conversation_id in metadata)
- [ ] Transaction-based migration

**Migration Tables:**
- `conversation` → `activity_history` (type='conversation')
- `message` → `activity_history` (type='chat_message')

**Metadata JSON Example:**
```json
{
  "conversationId": "conv-123",
  "role": "user",
  "modelUsed": "gemini-2.0-flash",
  "tokens": 150,
  "ragContext": ["doc1", "doc2"]
}
```

**Testing:**
- [ ] Spot check: 10 random conversations match legacy
- [ ] Spot check: 10 random messages match legacy
- [ ] AVA reads conversations from activity_history successfully
- [ ] AVA writes new messages to activity_history successfully

---

### Task A03: Add AVA Error Logging

**Description:** Add error interceptor to AVA for error_log table (module='chat')

**Agent:** Data Migration Engineer
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** V08
**Blocks:** A04

**Quality Gates:**
- [ ] AVAErrorInterceptor class created
- [ ] All uncaught exceptions logged to error_log
- [ ] Module field = 'chat' for all errors
- [ ] Stack traces captured correctly
- [ ] Activity correlation working (links to chat messages)

**Files to Create:**
- `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/error/AVAErrorInterceptor.kt`

**Testing:**
- [ ] Throw test exception in AVA, verify in error_log
- [ ] Verify stack trace captured
- [ ] Verify activity_id linked to chat message

---

### Task A04: Create AVA Functional Tests

**Description:** Create functional test suite for AVA migration (read/write validation)

**Agent:** Integration & Testing Lead
**Estimated Time:** 4 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** A01, A02, A03
**Blocks:** A05

**Quality Gates:**
- [ ] Test: AVA reads settings from app_settings (module='chat')
- [ ] Test: AVA writes new settings to app_settings
- [ ] Test: AVA reads conversations from activity_history (module='chat')
- [ ] Test: AVA writes new messages to activity_history
- [ ] Test: AVA writes errors to error_log
- [ ] Test: Module filtering works (only 'chat' data visible to AVA)
- [ ] All tests passing

**Files to Create:**
- `Universal/AVA/Core/Data/src/androidTest/kotlin/com/augmentalis/core/data/migration/AVAMigrationTest.kt`

---

### Task A05: Create AVA Performance Tests (CRITICAL)

**Description:** Create performance benchmark tests for AVA (CRITICAL: must maintain 0.2s init)

**Agent:** Integration & Testing Lead
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** A01, A02, A03
**Blocks:** A06

**Quality Gates (CRITICAL - Zero Regression Allowed):**
- [ ] Benchmark: AVA initialization ≤ 0.2s (MUST maintain)
- [ ] Benchmark: Chat message write ≤ 100ms
- [ ] Benchmark: Conversation load ≤ 50ms
- [ ] Benchmark: Settings query ≤ 50ms
- [ ] Performance regression detection enabled (alert if >0.2s)
- [ ] All benchmarks passing

**Files to Create:**
- `Universal/AVA/Core/Data/src/androidTest/kotlin/com/augmentalis/core/data/migration/AVAPerformanceTest.kt`

**Regression Detection:**
- If AVA init >0.2s → AUTOMATIC FAILURE
- Do NOT proceed to A06 if performance regresses

---

### Task A06: AVA Testing Checkpoint (CRITICAL - Zero Regression)

**Description:** Run all AVA tests and validate migration success with ZERO performance regression

**Agent:** Integration & Testing Lead
**Estimated Time:** 2 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** A04, A05
**Blocks:** A07

**Quality Gates (MUST PASS - Automatic Rollback if Fails):**
- [ ] Functional tests: 100% passing
- [ ] Performance tests: All targets met (CRITICAL: ≤0.2s init)
- [ ] Data integrity: Checksum verification passed (conversations, messages)
- [ ] Row count: Matches legacy tables exactly
- [ ] Spot checks: 10 random conversations, 10 random messages match
- [ ] AVA module: Fully operational with unified database
- [ ] AVA initialization: ≤0.2s (NO REGRESSION)

**Rollback Trigger (CRITICAL):**
- If AVA init >0.2s → AUTOMATIC ROLLBACK to AVA legacy tables
- If ANY data integrity test fails → AUTOMATIC ROLLBACK
- **STOP all further migration** (do NOT proceed to Phase 1C or Phase 2)
- Investigation required before retry

**Command:**
```bash
./gradlew :Universal:AVA:Core:Data:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.augmentalis.core.data.migration.AVAMigrationTest
```

---

### Task A07: Remove AVA Legacy Tables

**Description:** Drop AVA legacy tables (ONLY if A06 passes all tests with zero regression)

**Agent:** Data Migration Engineer
**Estimated Time:** 1 hour
**Complexity:** Tier 1 (Simple)
**Dependencies:** A06
**Blocks:** None (Phase 1B complete)

**Quality Gates:**
- [ ] A06 passed all tests (mandatory prerequisite)
- [ ] AVA init ≤0.2s verified (no regression)
- [ ] Legacy tables dropped (2 tables)
- [ ] AVA still functional after drop (verification test)

**Tables to Drop:**
```sql
DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS message;
```

**Safety Check:**
- Run AVA smoke test after drop
- Verify AVA init still ≤0.2s
- If smoke test fails → restore from backup (automatic)

---

## PHASE 1C: WebAvanue Module Migration (Week 2 - 20 hours)

**Platform:** Android (WebAvanue module)
**Dependencies:** Phase 1B complete (A07)
**Agent:** Data Migration Engineer + Test Lead
**Parallelizable:** Partial (migration tasks sequential, testing parallel)

---

### Task W01: Migrate WebAvanue Settings (browser_settings)

**Description:** Migrate monolithic browser_settings table (45+ columns) to app_settings (module='browser')

**Agent:** Data Migration Engineer
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** A07
**Blocks:** W04, W05

**Quality Gates:**
- [ ] browser_settings table migrated to app_settings
- [ ] 45+ columns broken into key-value pairs
- [ ] Module field = 'browser' for all rows
- [ ] Category field correctly inferred (privacy, performance, ui, etc.)
- [ ] Type field correctly inferred (boolean, int, string, json)
- [ ] No data loss (all 45+ settings present)
- [ ] Transaction-based migration

**Migration:**
- `browser_settings` (monolithic 45+ columns) → `app_settings` (key-value pairs, module='browser')

**Example Mapping:**
- home_page → app_settings (module='browser', category='ui', key='home_page', type='string')
- enable_javascript → app_settings (module='browser', category='performance', key='enable_javascript', type='boolean')

**Testing:**
- [ ] Spot check: 10 random settings match legacy
- [ ] WebAvanue reads settings from app_settings successfully
- [ ] WebAvanue writes new settings to app_settings successfully

---

### Task W02: Migrate WebAvanue History (history_entry)

**Description:** Migrate history_entry table to activity_history (module='browser')

**Agent:** Data Migration Engineer
**Estimated Time:** 3 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** A07
**Blocks:** W04, W05

**Quality Gates:**
- [ ] history_entry table migrated to activity_history
- [ ] Checksum verification passed
- [ ] Row count verification passed
- [ ] Module field = 'browser' for all rows
- [ ] Activity_type = 'web_visit' for all rows
- [ ] Metadata field contains WebAvanue-specific data (URL, favicon, duration)
- [ ] Incognito mode preserved (is_incognito field)
- [ ] Transaction-based migration

**Migration:**
- `history_entry` → `activity_history` (type='web_visit', module='browser')

**Metadata JSON Example:**
```json
{
  "url": "https://example.com",
  "favicon": "base64...",
  "visit_duration": 45000,
  "referrer": "https://google.com",
  "search_terms": "kotlin multiplatform"
}
```

**Testing:**
- [ ] Spot check: 10 random visits match legacy
- [ ] WebAvanue reads history from activity_history successfully
- [ ] WebAvanue writes new visits to activity_history successfully
- [ ] Incognito mode works (is_incognito=1, not saved)

---

### Task W03: Add WebAvanue Error Logging

**Description:** Add error interceptor to WebAvanue for error_log table (module='browser')

**Agent:** Data Migration Engineer
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** A07
**Blocks:** W04

**Quality Gates:**
- [ ] BrowserErrorInterceptor class created
- [ ] All uncaught exceptions logged to error_log
- [ ] Module field = 'browser' for all errors
- [ ] Stack traces captured correctly
- [ ] Activity correlation working (links to web visits)

**Files to Create:**
- `WebAvanue/BrowserCoreData/src/main/kotlin/com/augmentalis/webavanue/data/error/BrowserErrorInterceptor.kt`

**Testing:**
- [ ] Throw test exception in WebAvanue, verify in error_log
- [ ] Verify stack trace captured
- [ ] Verify activity_id linked to web visit

---

### Task W04: Create WebAvanue Functional Tests

**Description:** Create functional test suite for WebAvanue migration (read/write validation)

**Agent:** Integration & Testing Lead
**Estimated Time:** 3 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** W01, W02, W03
**Blocks:** W05

**Quality Gates:**
- [ ] Test: WebAvanue reads settings from app_settings (module='browser')
- [ ] Test: WebAvanue writes new settings to app_settings
- [ ] Test: WebAvanue reads history from activity_history (module='browser')
- [ ] Test: WebAvanue writes new visits to activity_history
- [ ] Test: Incognito mode works (is_incognito=1, not saved)
- [ ] Test: WebAvanue writes errors to error_log
- [ ] All tests passing

**Files to Create:**
- `Universal/AVA/Core/Data/src/androidTest/kotlin/com/augmentalis/core/data/migration/WebAvanueMigrationTest.kt`

---

### Task W05: WebAvanue Testing Checkpoint

**Description:** Run all WebAvanue tests and validate migration success

**Agent:** Integration & Testing Lead
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** W04
**Blocks:** W06

**Quality Gates (MUST PASS - Automatic Rollback if Fails):**
- [ ] Functional tests: 100% passing
- [ ] Performance tests: Settings query ≤50ms, History query ≤100ms
- [ ] Data integrity: Checksum verification passed (history_entry)
- [ ] Row count: Matches legacy table exactly
- [ ] Spot checks: 10 random settings, 10 random visits match
- [ ] WebAvanue module: Fully operational with unified database
- [ ] Incognito mode: Working correctly (not saving to history)

**Rollback Trigger:**
- If ANY test fails → AUTOMATIC ROLLBACK to WebAvanue legacy tables
- Migration marked as failed
- Investigation required before retry

---

### Task W06: Remove WebAvanue Legacy Tables

**Description:** Drop WebAvanue legacy tables (ONLY if W05 passes all tests)

**Agent:** Data Migration Engineer
**Estimated Time:** 1 hour
**Complexity:** Tier 1 (Simple)
**Dependencies:** W05
**Blocks:** None (Phase 1C complete)

**Quality Gates:**
- [ ] W05 passed all tests (mandatory prerequisite)
- [ ] Legacy tables dropped (2 tables)
- [ ] WebAvanue still functional after drop

**Tables to Drop:**
```sql
DROP TABLE IF EXISTS browser_settings;
DROP TABLE IF EXISTS history_entry;
```

---

## PHASE 2A: AVA ML/AI Migration (Week 2.5 - 24 hours)

**Platform:** Android (AVA module, ML/AI focus)
**Dependencies:** Phase 1B complete (A07)
**Agent:** ML/AI Specialist + Test Lead
**Parallelizable:** Partial (migration tasks sequential, testing parallel)

---

### Task ML01: Migrate AVA Embeddings to ml_embedding_cache

**Description:** Migrate intent_embedding and embedding_metadata tables to ml_embedding_cache (module='chat')

**Agent:** ML/AI Specialist
**Estimated Time:** 6 hours
**Complexity:** Tier 3 (Complex - CRITICAL)
**Dependencies:** A07
**Blocks:** ML04, ML05

**Quality Gates (CRITICAL - Must Preserve Embeddings Exactly):**
- [ ] intent_embedding table migrated to ml_embedding_cache
- [ ] embedding_metadata table migrated to ml_model_metadata
- [ ] BLOB checksum verification passed (embedding integrity)
- [ ] Row count verification passed
- [ ] Module field = 'chat' for all embeddings
- [ ] Item_type = 'intent' for all embeddings
- [ ] Dual model support preserved (384-dim and 768-dim)
- [ ] Model version tracking preserved
- [ ] Transaction-based migration

**Migration:**
- `intent_embedding` → `ml_embedding_cache` (module='chat', item_type='intent')
- `embedding_metadata` → `ml_model_metadata` (module='chat', model_type='EMBEDDING')

**CRITICAL:**
- Embeddings are quantized INT8 BLOBs (must preserve exactly)
- Any BLOB corruption → automatic rollback
- Checksum verification mandatory

---

### Task ML02: Migrate AVA Training Data to ml_training_example

**Description:** Migrate intent_example and train_example tables to ml_training_example with FTS4

**Agent:** ML/AI Specialist
**Estimated Time:** 5 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** A07
**Blocks:** ML04, ML05

**Quality Gates:**
- [ ] intent_example table migrated to ml_training_example
- [ ] train_example table migrated to ml_training_example
- [ ] FTS4 index populated correctly (ml_training_example_fts)
- [ ] Row count verification passed
- [ ] Module field = 'chat' for all training data
- [ ] Hash-based deduplication preserved
- [ ] FTS4 triggers working (insert, update, delete)
- [ ] Transaction-based migration

**Migration:**
- `intent_example` → `ml_training_example` (module='chat', source='AVA_FILE')
- `train_example` → `ml_training_example` (module='chat', source='USER')

**FTS4 Validation:**
- [ ] FTS search returns results in ≤10ms (50x faster than LIKE)
- [ ] FTS triggers synchronized with main table

---

### Task ML03: Update AVA to Use Shared ML Repositories

**Description:** Update AVA NLU code to use shared ML repositories instead of legacy tables

**Agent:** ML/AI Specialist
**Estimated Time:** 4 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** ML01, ML02
**Blocks:** ML04

**Quality Gates:**
- [ ] AVA IntentClassifier uses MLEmbeddingCacheRepository
- [ ] AVA NLU uses MLTrainingExampleRepository
- [ ] AVA tracks cache hits via MLModelMetadataRepository
- [ ] No references to legacy tables (intent_embedding, intent_example, train_example)

**Files to Modify:**
- `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentClassifier.kt`
- `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/NLUInitializer.kt`

---

### Task ML04: Create AVA ML/AI Performance Tests (CRITICAL)

**Description:** Create performance benchmark tests for AVA ML/AI (CRITICAL: 95% cache hit, 0.2s init)

**Agent:** Integration & Testing Lead
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex - CRITICAL)
**Dependencies:** ML01, ML02, ML03
**Blocks:** ML05

**Quality Gates (CRITICAL - Zero Regression Allowed):**
- [ ] Benchmark: AVA initialization ≤ 0.2s (MUST maintain)
- [ ] Benchmark: Cache hit rate ≥ 95% (MUST maintain)
- [ ] Benchmark: Intent recognition ≤ 0.2s (MUST maintain)
- [ ] Benchmark: FTS search ≤ 10ms (50x faster than 250ms)
- [ ] Benchmark: Cache miss latency ≤ 50ms (compute + cache)
- [ ] All benchmarks passing

**Files to Create:**
- `Universal/AVA/Core/Data/src/androidTest/kotlin/com/augmentalis/core/data/migration/AVAMLPerformanceTest.kt`

**Regression Detection:**
- If cache hit rate <95% → AUTOMATIC FAILURE
- If AVA init >0.2s → AUTOMATIC FAILURE
- Do NOT proceed to ML05 if performance regresses

---

### Task ML05: AVA ML/AI Testing Checkpoint (CRITICAL)

**Description:** Run all AVA ML/AI tests and validate migration success with ZERO regression

**Agent:** Integration & Testing Lead
**Estimated Time:** 2 hours
**Complexity:** Tier 3 (Complex - CRITICAL)
**Dependencies:** ML04
**Blocks:** ML06

**Quality Gates (MUST PASS - Automatic Rollback if Fails):**
- [ ] Performance tests: 100% passing
- [ ] Cache hit rate: ≥95% (NO REGRESSION)
- [ ] AVA initialization: ≤0.2s (NO REGRESSION)
- [ ] Intent recognition: ≤0.2s (NO REGRESSION)
- [ ] FTS search: ≤10ms (50x improvement verified)
- [ ] Data integrity: BLOB checksum verification passed
- [ ] Row count: Matches legacy tables exactly
- [ ] AVA ML/AI: Fully operational with shared cache

**Rollback Trigger (CRITICAL):**
- If cache hit rate <95% → AUTOMATIC ROLLBACK to AVA legacy ML tables
- If AVA init >0.2s → AUTOMATIC ROLLBACK
- **STOP VoiceOS ML migration** (do NOT proceed to Phase 2B)
- Investigation required before retry

**Command:**
```bash
./gradlew :Universal:AVA:Core:Data:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.augmentalis.core.data.migration.AVAMLPerformanceTest
```

---

### Task ML06: Remove AVA Legacy ML Tables

**Description:** Drop AVA legacy ML tables (ONLY if ML05 passes with zero regression)

**Agent:** ML/AI Specialist
**Estimated Time:** 1 hour
**Complexity:** Tier 1 (Simple)
**Dependencies:** ML05
**Blocks:** None (Phase 2A complete)

**Quality Gates:**
- [ ] ML05 passed all tests (mandatory prerequisite)
- [ ] Cache hit rate ≥95% verified
- [ ] AVA init ≤0.2s verified
- [ ] Legacy ML tables dropped (5 tables)

**Tables to Drop:**
```sql
DROP TABLE IF EXISTS intent_embedding;
DROP TABLE IF EXISTS embedding_metadata;
DROP TABLE IF EXISTS intent_example;
DROP TABLE IF EXISTS train_example;
DROP TABLE IF EXISTS train_example_fts;
```

---

## PHASE 2B: VoiceOS ML/AI Migration (Week 3 - 20 hours)

**Platform:** Android (VoiceOS module, ML/AI focus)
**Dependencies:** Phase 2A complete (ML06) - AVA regression-free
**Agent:** ML/AI Specialist + Test Lead
**Parallelizable:** Partial (migration tasks sequential, testing parallel)

---

### Task VML01: Migrate VoiceOS Learning Data

**Description:** Migrate 3 VoiceOS learning tables to ml_training_example (module='voice')

**Agent:** ML/AI Specialist
**Estimated Time:** 5 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** ML06
**Blocks:** VML03, VML04

**Quality Gates:**
- [ ] language_model table migrated to ml_training_example
- [ ] gesture_learning table migrated to ml_training_example
- [ ] recognition_learning table migrated to ml_training_example
- [ ] Row count verification passed
- [ ] Module field = 'voice' for all training data
- [ ] Hash-based deduplication applied
- [ ] FTS4 index populated correctly
- [ ] Transaction-based migration

**Migration:**
- `language_model` → `ml_training_example` (module='voice', source='SCRAPED')
- `gesture_learning` → `ml_training_example` (module='voice', source='USER')
- `recognition_learning` → `ml_training_example` (module='voice', source='CORRECTED')

---

### Task VML02: Enable VoiceOS Embedding Cache

**Description:** Update VoiceOS IntentRecognizer to use shared embedding cache (expect 10-20x speedup)

**Agent:** ML/AI Specialist
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** VML01
**Blocks:** VML03

**Quality Gates:**
- [ ] VoiceOS IntentRecognizer uses MLEmbeddingCacheRepository
- [ ] Cache hit logic implemented (check cache first)
- [ ] Cache miss logic implemented (compute + store)
- [ ] VoiceOS tracks cache hits via MLModelMetadataRepository
- [ ] No references to legacy learning tables

**Files to Modify:**
- `VoiceOS/features/nlu/IntentRecognizer.kt` (or equivalent)

**Expected Behavior:**
1. Check cache for utterance embedding
2. If cache hit (expected 90%+) → use cached embedding (1ms)
3. If cache miss → compute embedding, cache it, continue (50ms)

---

### Task VML03: Create VoiceOS ML/AI Performance Tests (CRITICAL - 10-20x Speedup)

**Description:** Create performance benchmark tests for VoiceOS ML/AI (CRITICAL: achieve 10-20x speedup)

**Agent:** Integration & Testing Lead
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex - CRITICAL)
**Dependencies:** VML01, VML02
**Blocks:** VML04

**Quality Gates (CRITICAL - 10x Minimum Speedup Required):**
- [ ] Benchmark: VoiceOS initialization ≤ 0.5s (from 4-5s) - **10x minimum**
- [ ] Target: 0.2-0.5s (same as AVA)
- [ ] Benchmark: Cache hit rate ≥ 90% (after warmup)
- [ ] Benchmark: Intent recognition ≤ 0.2s (same as AVA)
- [ ] Benchmark: FTS search ≤ 10ms (50x faster than 250ms)
- [ ] All benchmarks passing

**Files to Create:**
- `VoiceOS/tests/performance/VoiceOSMLPerformanceTest.kt` (or equivalent)

**Benchmarking Script:**
```kotlin
// Cold start (no cache)
val coldStartTime = measureTimeMillis { voiceOS.initialize() }
expect(coldStartTime).isLessThan(500)  // ≤0.5s

// Warm start (cache populated)
val warmStartTime = measureTimeMillis { voiceOS.initialize() }
expect(warmStartTime).isLessThan(200)  // ≤0.2s (same as AVA)

// Cache hit rate
val hitRate = modelMetadata.getCacheHitRate(module = "voice")
expect(hitRate).isGreaterThanOrEqualTo(0.90)  // ≥90%
```

---

### Task VML04: VoiceOS ML/AI Testing Checkpoint (CRITICAL - 10-20x Speedup)

**Description:** Run all VoiceOS ML/AI tests and validate 10-20x speedup achieved

**Agent:** Integration & Testing Lead
**Estimated Time:** 2 hours
**Complexity:** Tier 3 (Complex - CRITICAL)
**Dependencies:** VML03
**Blocks:** VML05

**Quality Gates (MUST PASS - Investigate if Speedup < 10x):**
- [ ] Performance tests: 100% passing
- [ ] VoiceOS initialization: ≤0.5s (10x faster than 4-5s)
- [ ] Target achieved: 0.2-0.5s (same as AVA)
- [ ] Cache hit rate: ≥90%
- [ ] Intent recognition: ≤0.2s
- [ ] FTS search: ≤10ms
- [ ] VoiceOS accuracy: No regression (same recognition quality)

**Rollback Trigger (if Speedup < 10x):**
- If VoiceOS init >0.5s but <2s → INVESTIGATE, OPTIMIZE, RETRY
- If VoiceOS init >2s → ROLLBACK to legacy learning tables
- If accuracy regresses → ROLLBACK

**Command:**
```bash
# Run VoiceOS ML performance tests
./gradlew :VoiceOS:tests:performance:VoiceOSMLPerformanceTest
```

---

### Task VML05: Remove VoiceOS Legacy ML Tables

**Description:** Drop VoiceOS legacy ML tables (ONLY if VML04 passes with ≥10x speedup)

**Agent:** ML/AI Specialist
**Estimated Time:** 1 hour
**Complexity:** Tier 1 (Simple)
**Dependencies:** VML04
**Blocks:** None (Phase 2B complete)

**Quality Gates:**
- [ ] VML04 passed all tests (mandatory prerequisite)
- [ ] VoiceOS init ≤0.5s verified (10x speedup)
- [ ] Legacy ML tables dropped (3 tables)

**Tables to Drop:**
```sql
DROP TABLE IF EXISTS language_model;
DROP TABLE IF EXISTS gesture_learning;
DROP TABLE IF EXISTS recognition_learning;
```

---

## PHASE 3: Plugin Extension (Week 3.5 - 16 hours)

**Platform:** Android (All modules - VoiceOS, WebAvanue, AVA)
**Dependencies:** Phase 1C complete (W06)
**Agent:** Android Repository Developer + Test Lead
**Parallelizable:** Yes (plugin API creation parallel, testing parallel)

---

### Task P01: Add Module Field to Plugin Tables

**Description:** Extend VoiceOS plugin tables with module field (backward compatible)

**Agent:** Android Repository Developer
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** W06
**Blocks:** P02, P03, P04

**Quality Gates:**
- [ ] Module field added to plugins table (default 'voice')
- [ ] Module field added to plugin_permissions table (default 'voice')
- [ ] Indices created (idx_plugins_module, idx_plugin_permissions_module)
- [ ] All existing VoiceOS plugins default to module='voice'
- [ ] Backward compatible (VoiceOS plugins still work)

**Migration:**
```sql
ALTER TABLE plugins ADD COLUMN module TEXT NOT NULL DEFAULT 'voice';
ALTER TABLE plugin_permissions ADD COLUMN module TEXT NOT NULL DEFAULT 'voice';
CREATE INDEX idx_plugins_module ON plugins(module);
CREATE INDEX idx_plugin_permissions_module ON plugin_permissions(module);
```

---

### Task P02: Create Browser Plugin API

**Description:** Create BrowserPlugin interface and permission system for WebAvanue

**Agent:** Android Repository Developer
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** P01
**Blocks:** P05

**Quality Gates:**
- [ ] BrowserPlugin interface created
- [ ] BrowserPermission enum created (READ_HISTORY, MODIFY_HISTORY, INTERCEPT_REQUESTS, MODIFY_DOM, ACCESS_COOKIES)
- [ ] Plugin lifecycle methods defined (onInstall, onEnable, onDisable, onUninstall)
- [ ] Browser hooks defined (onPageLoad, onRequest, onResponse)
- [ ] Example plugin created (Ad Blocker)

**Files to Create:**
- `WebAvanue/BrowserCoreData/src/main/kotlin/com/augmentalis/webavanue/data/plugin/BrowserPlugin.kt`
- `WebAvanue/BrowserCoreData/src/main/kotlin/com/augmentalis/webavanue/data/plugin/BrowserPermission.kt`
- `WebAvanue/BrowserCoreData/src/main/kotlin/com/augmentalis/webavanue/data/plugin/AdBlockerPlugin.kt` (example)

---

### Task P03: Create Chat Plugin API

**Description:** Create ChatPlugin interface and permission system for AVA

**Agent:** Android Repository Developer
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** P01
**Blocks:** P05

**Quality Gates:**
- [ ] ChatPlugin interface created
- [ ] ChatPermission enum created (READ_CONVERSATION, MODIFY_MESSAGES, REGISTER_ACTIONS, ACCESS_RAG)
- [ ] Plugin lifecycle methods defined
- [ ] Chat hooks defined (onUserMessage, onAssistantResponse, registerActions)
- [ ] AIAction data class created (name, description, parameters, execute)
- [ ] Example plugin created (PDF Summarizer)

**Files to Create:**
- `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/plugin/ChatPlugin.kt`
- `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/plugin/ChatPermission.kt`
- `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/plugin/PDFSummarizerPlugin.kt` (example)

---

### Task P04: Update Plugin Repository for Module Filtering

**Description:** Update PluginRepository to support module-based queries

**Agent:** Android Repository Developer
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** P01
**Blocks:** P05

**Quality Gates:**
- [ ] PluginRepository.getByModule(module: String) query added
- [ ] PluginPermissionsRepository.getByModule(module: String) query added
- [ ] Module filtering enforced (VoiceOS sees only 'voice' plugins)

**Files to Modify:**
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/repository/PluginRepository.kt`

---

### Task P05: Create Plugin System Tests

**Description:** Create test suite for plugin system (VoiceOS backward compat, browser, chat, cross-module)

**Agent:** Integration & Testing Lead
**Estimated Time:** 4 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** P01, P02, P03, P04
**Blocks:** None (Phase 3 complete)

**Quality Gates:**
- [ ] Test: VoiceOS plugins still work (backward compatibility)
- [ ] Test: Install browser plugin (ad blocker), verify blocks ads
- [ ] Test: Install chat plugin (PDF summarizer), verify action registers
- [ ] Test: Cross-module plugin (voice → browser), verify works
- [ ] Test: Plugin permissions enforced (module-scoped)
- [ ] All tests passing

**Files to Create:**
- `Universal/AVA/Core/Data/src/androidTest/kotlin/com/augmentalis/core/data/plugin/PluginSystemTest.kt`

**Test Scenarios:**
1. Backward Compatibility: Existing VoiceOS plugin loads and executes
2. Browser Plugin: Ad blocker plugin intercepts requests, blocks ads
3. Chat Plugin: PDF summarizer plugin registers AI action, executes on user request
4. Cross-Module: Voice command "open bookmarked page X" → browser opens page

---

## PHASE 4: Validation & Optimization (Week 4 - 16 hours)

**Platform:** Android (All modules - validation, GDPR, monitoring)
**Dependencies:** Phases 2B, 3 complete (VML05, P05)
**Agent:** Test Lead + Data Migration Engineer
**Parallelizable:** Yes (GDPR, consolidation, monitoring in parallel)

---

### Task VAL01: Implement Rolling Consolidation (7-day/28-day)

**Description:** Implement ActivityHistoryConsolidator with 7-day and 28-day consolidation logic

**Agent:** Data Migration Engineer
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** VML05, P05
**Blocks:** VAL05

**Quality Gates:**
- [ ] ActivityHistoryConsolidator class created
- [ ] 7-day consolidation logic implemented (top 50 commands + top 50 websites)
- [ ] 28-day consolidation logic implemented (merge 4 weekly snapshots)
- [ ] Ranking algorithm implemented (frequency × recency)
- [ ] WorkManager periodic job configured (7-day interval)
- [ ] Storage savings verified (~95% reduction)

**Files to Create:**
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/consolidation/ActivityHistoryConsolidator.kt`
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/consolidation/ConsolidationWorker.kt`

**7-Day Logic:**
1. Get all activity from past 7 days
2. Group by module and activity_type
3. Rank by frequency × recency
4. Keep top 50 per category
5. Delete the rest (95% reduction)

**28-Day Logic:**
1. Get 4 weekly snapshots
2. Merge and rank across all snapshots
3. Keep top 50 commands + top 50 websites
4. Delete the rest

---

### Task VAL02: Implement GDPR Compliance Features

**Description:** Implement GDPR consent, data export, and right to erasure features

**Agent:** Data Migration Engineer
**Estimated Time:** 4 hours
**Complexity:** Tier 3 (Complex)
**Dependencies:** VML05, P05
**Blocks:** VAL05

**Quality Gates:**
- [ ] GDPRConsentDialog created (first launch)
- [ ] GDPRDataExporter created (export to JSON)
- [ ] Right to erasure implemented (delete all data)
- [ ] Per-module consent controls implemented
- [ ] Privacy policy link added
- [ ] Incognito mode working (activity not saved)

**Files to Create:**
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/gdpr/GDPRConsentDialog.kt`
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/gdpr/GDPRDataExporter.kt`
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/gdpr/GDPRDataDeletion.kt`

**GDPR Features:**
1. Explicit consent: Dialog on first launch
2. Data export: One-click export to JSON (GDPR Article 20)
3. Right to erasure: One-click delete all history (GDPR Article 17)
4. Privacy policy: Link to privacy policy
5. Granular controls: Per-module consent (disable browser tracking, keep voice)
6. Incognito mode: Activity not saved when enabled

---

### Task VAL03: Create Final Performance Validation Suite

**Description:** Create comprehensive performance validation suite (all modules, all targets)

**Agent:** Integration & Testing Lead
**Estimated Time:** 3 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** VML05
**Blocks:** VAL04

**Quality Gates:**
- [ ] Test: VoiceOS initialization ≤ 0.5s (10x faster)
- [ ] Test: AVA initialization ≤ 0.2s (no regression)
- [ ] Test: Settings query ≤ 50ms (all modules)
- [ ] Test: History query ≤ 100ms (all modules)
- [ ] Test: FTS search ≤ 10ms (25x faster)
- [ ] Test: Cache hit rate ≥ 95% (AVA), ≥ 90% (VoiceOS)
- [ ] All tests passing

**Files to Create:**
- `Universal/AVA/Core/Data/src/androidTest/kotlin/com/augmentalis/core/data/validation/FinalPerformanceValidationTest.kt`

---

### Task VAL04: Setup Production Monitoring

**Description:** Create DatabaseMetricsCollector and configure Firebase Analytics for production monitoring

**Agent:** Integration & Testing Lead
**Estimated Time:** 3 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** VAL03
**Blocks:** VAL05

**Quality Gates:**
- [ ] DatabaseMetricsCollector class created
- [ ] Metrics: avgQueryLatency, cacheHitRate, databaseSize, totalSettings, totalActivities, totalErrors, totalEmbeddings
- [ ] Firebase Analytics integration configured
- [ ] Metrics logged every 24 hours
- [ ] Dashboard created (Firebase console)

**Files to Create:**
- `Universal/AVA/Core/Data/src/main/kotlin/com/augmentalis/core/data/monitoring/DatabaseMetricsCollector.kt`

**Metrics to Track:**
- Performance: avgQueryLatency, cacheHitRate
- Data: totalSettings, totalActivities, totalErrors, totalEmbeddings, databaseSize
- Health: migrationStatus, consolidationLastRun, gdprCompliance

---

### Task VAL05: Final Validation & Documentation

**Description:** Run final validation tests, create migration summary, and update documentation

**Agent:** Integration & Testing Lead
**Estimated Time:** 2 hours
**Complexity:** Tier 2 (Moderate)
**Dependencies:** VAL01, VAL02, VAL03, VAL04
**Blocks:** None (Phase 4 complete - ALL DONE!)

**Quality Gates (FINAL PROJECT APPROVAL):**
- [ ] All 87 tasks completed
- [ ] Rolling consolidation working (7-day/28-day)
- [ ] GDPR compliance validated (consent, export, delete)
- [ ] Performance targets met (VoiceOS ≤0.5s, AVA ≤0.2s)
- [ ] Production monitoring enabled
- [ ] Documentation complete (Developer Migration Guide, User Privacy Guide, Plugin Developer Guide)
- [ ] All 32 success criteria from spec met

**Documentation to Update:**
- `docs/database-migration-guide.md` - Developer migration guide
- `docs/user-privacy-guide.md` - User privacy guide
- `docs/plugin-developer-guide.md` - Plugin developer guide
- `docs/performance-optimization-guide.md` - Performance optimization guide

**Final Validation:**
```bash
# Run all tests
./gradlew :Universal:AVA:Core:Data:connectedAndroidTest

# Verify all modules working
# VoiceOS: Initialize and execute voice command
# AVA: Initialize and send chat message
# WebAvanue: Initialize and visit webpage

# Verify performance targets
# VoiceOS init ≤0.5s ✅
# AVA init ≤0.2s ✅
# Cache hit rate ≥95% (AVA), ≥90% (VoiceOS) ✅
# FTS search ≤10ms ✅
```

**Success! 🎉**

---

## Parallel Execution Plan

### Batch 1: Foundation (Sequential - BLOCKING)

**Tasks:** S01-S09 (Phase 0)
**Duration:** 18 hours (0.5 weeks)
**Agent:** Database Architect (solo)
**Parallelizable:** No (foundation - all other phases depend on this)

**Critical Path:** S01 → S02-S08 (parallel schema creation) → S09 (code generation)

---

### Batch 2: VoiceOS Migration (Partial Parallel)

**Tasks:** V01-V08 (Phase 1A)
**Duration:** 32 hours (1 week)
**Agents:** Android Repo Dev (V01), Data Migration Eng (V02-V04), Test Lead (V05-V06)
**Parallelizable:** Partial (V05-V06 parallel with V02-V04, V07 sequential, V08 final)

**Critical Path:** V01 → [V02, V03, V04, V05, V06 parallel] → V07 → V08

**Parallel Window:**
- V02 (settings migration) - 4h
- V03 (history migration) - 5h
- V04 (error migration) - 2h
- V05 (functional tests) - 4h (parallel with V02-V04)
- V06 (performance tests) - 3h (parallel with V02-V04)

**Time Savings:** 32h sequential → 20h parallel (12h saved, 38% reduction)

---

### Batch 3: AVA Migration (Partial Parallel)

**Tasks:** A01-A07 (Phase 1B)
**Duration:** 28 hours (0.7 weeks)
**Agents:** Data Migration Eng (A01-A03), Test Lead (A04-A05)
**Parallelizable:** Partial (A04-A05 parallel with A01-A03, A06 sequential, A07 final)

**Critical Path:** [A01, A02, A03, A04, A05 parallel] → A06 → A07

**Time Savings:** 28h sequential → 18h parallel (10h saved, 36% reduction)

---

### Batch 4: WebAvanue Migration (Partial Parallel)

**Tasks:** W01-W06 (Phase 1C)
**Duration:** 20 hours (0.5 weeks)
**Agents:** Data Migration Eng (W01-W03), Test Lead (W04)
**Parallelizable:** Partial (W04 parallel with W01-W03, W05 sequential, W06 final)

**Critical Path:** [W01, W02, W03, W04 parallel] → W05 → W06

**Time Savings:** 20h sequential → 13h parallel (7h saved, 35% reduction)

---

### Batch 5: AVA ML/AI Migration (Partial Parallel)

**Tasks:** ML01-ML06 (Phase 2A)
**Duration:** 24 hours (0.6 weeks)
**Agents:** ML/AI Specialist (ML01-ML03), Test Lead (ML04)
**Parallelizable:** Partial (ML04 parallel with ML01-ML03, ML05 sequential, ML06 final)

**Critical Path:** [ML01, ML02, ML03, ML04 parallel] → ML05 → ML06

**Time Savings:** 24h sequential → 16h parallel (8h saved, 33% reduction)

---

### Batch 6: VoiceOS ML/AI Migration (Partial Parallel)

**Tasks:** VML01-VML05 (Phase 2B)
**Duration:** 20 hours (0.5 weeks)
**Agents:** ML/AI Specialist (VML01-VML02), Test Lead (VML03)
**Parallelizable:** Partial (VML03 parallel with VML01-VML02, VML04 sequential, VML05 final)

**Critical Path:** [VML01, VML02, VML03 parallel] → VML04 → VML05

**Time Savings:** 20h sequential → 14h parallel (6h saved, 30% reduction)

---

### Batch 7: Plugin Extension (Fully Parallel)

**Tasks:** P01-P05 (Phase 3)
**Duration:** 16 hours (0.4 weeks)
**Agents:** Android Repo Dev (P01-P04), Test Lead (P05)
**Parallelizable:** Yes (P02-P04 parallel after P01, P05 after all)

**Critical Path:** P01 → [P02, P03, P04 parallel] → P05

**Time Savings:** 16h sequential → 10h parallel (6h saved, 38% reduction)

---

### Batch 8: Validation (Fully Parallel)

**Tasks:** VAL01-VAL05 (Phase 4)
**Duration:** 16 hours (0.4 weeks)
**Agents:** Data Migration Eng (VAL01-VAL02), Test Lead (VAL03-VAL04), Both (VAL05)
**Parallelizable:** Yes (VAL01-VAL04 parallel, VAL05 sequential)

**Critical Path:** [VAL01, VAL02, VAL03, VAL04 parallel] → VAL05

**Time Savings:** 16h sequential → 8h parallel (8h saved, 50% reduction)

---

## Total Execution Time Summary

### Sequential Execution (No Swarm)

| Phase | Duration | Cumulative |
|-------|----------|------------|
| 0: Foundation | 18h (0.5w) | 18h (0.5w) |
| 1A: VoiceOS | 32h (1w) | 50h (1.5w) |
| 1B: AVA | 28h (0.7w) | 78h (2.2w) |
| 1C: WebAvanue | 20h (0.5w) | 98h (2.7w) |
| 2A: AVA ML/AI | 24h (0.6w) | 122h (3.3w) |
| 2B: VoiceOS ML/AI | 20h (0.5w) | 142h (3.8w) |
| 3: Plugins | 16h (0.4w) | 158h (4.2w) |
| 4: Validation | 16h (0.4w) | 174h (4.6w) |
| **Total Sequential** | **174h** | **4.6 weeks** |

### Parallel Execution (With Swarm)

| Phase | Sequential | Parallel | Savings |
|-------|------------|----------|---------|
| 0: Foundation | 18h | 18h | 0h (blocking) |
| 1A: VoiceOS | 32h | 20h | 12h (38%) |
| 1B: AVA | 28h | 18h | 10h (36%) |
| 1C: WebAvanue | 20h | 13h | 7h (35%) |
| 2A: AVA ML/AI | 24h | 16h | 8h (33%) |
| 2B: VoiceOS ML/AI | 20h | 14h | 6h (30%) |
| 3: Plugins | 16h | 10h | 6h (38%) |
| 4: Validation | 16h | 8h | 8h (50%) |
| **Total Parallel** | **174h** | **117h** | **57h (33%)** |

**Final Duration:**
- **Sequential:** 174 hours = 4.6 weeks (40h/week)
- **Parallel (Swarm):** 117 hours = 3 weeks (40h/week)
- **Time Savings:** 57 hours (33% reduction)

**Note:** Original plan estimated 3.5 weeks. Actual parallel execution is 3 weeks (faster due to aggressive parallelization).

---

## Swarm Task Distribution

### Agent 1: Database Architect

**Total Tasks:** 9 (Phase 0)
**Total Hours:** 18 hours (0.5 weeks)
**Domain:** Schema design, migration validation

**Tasks:**
- S01: Configure SQLDelight plugin (1h)
- S02: Create app_settings schema (2h)
- S03: Create activity_history schema (3h)
- S04: Create error_log schema (2h)
- S05: Create ml_embedding_cache schema (3h)
- S06: Create ml_training_example schema + FTS4 (4h)
- S07: Create ml_model_metadata schema (2h)
- S08: Create ColumnAdapters (1h)
- S09: Generate SQLDelight code and validate (0.5h)

**Responsibilities:**
- CREATE TABLE statements with indices, constraints
- FTS4 triggers (DELETE + INSERT pattern)
- ColumnAdapter type conversions
- Code generation validation

---

### Agent 2: Android Repository Developer

**Total Tasks:** 13 (Phase 1A, 3)
**Total Hours:** 26 hours (0.65 weeks)
**Domain:** Repository pattern, dependency injection, plugin APIs

**Tasks:**
- V01: Create VoiceOS migration utilities (3h)
- P01: Add module field to plugin tables (2h)
- P02: Create browser plugin API (4h)
- P03: Create chat plugin API (4h)
- P04: Update plugin repository for module filtering (2h)
- (Supporting tasks for A01, W01)

**Responsibilities:**
- Repository creation (module-filtered)
- Hilt dependency injection
- Plugin APIs (browser, chat)
- Type-safe query wrappers

---

### Agent 3: Data Migration Engineer

**Total Tasks:** 24 (Phases 1A, 1B, 1C, 4)
**Total Hours:** 48 hours (1.2 weeks)
**Domain:** Data migration, integrity verification, GDPR

**Tasks:**
- V02-V04: VoiceOS migrations (settings, history, errors) - 11h
- V08: Remove VoiceOS legacy tables (1h)
- A01-A03: AVA migrations (settings, history, errors) - 10h
- A07: Remove AVA legacy tables (1h)
- W01-W03: WebAvanue migrations (settings, history, errors) - 9h
- W06: Remove WebAvanue legacy tables (1h)
- VAL01: Rolling consolidation (4h)
- VAL02: GDPR compliance (4h)

**Responsibilities:**
- Migration scripts (checksum verification)
- Transaction-based migration (automatic rollback)
- GDPR features (consent, export, delete)
- Rolling consolidation logic

---

### Agent 4: ML/AI Specialist

**Total Tasks:** 16 (Phases 2A, 2B)
**Total Hours:** 32 hours (0.8 weeks)
**Domain:** Embedding cache, FTS optimization, performance

**Tasks:**
- ML01: Migrate AVA embeddings (6h)
- ML02: Migrate AVA training data (5h)
- ML03: Update AVA to use shared ML repositories (4h)
- ML06: Remove AVA legacy ML tables (1h)
- VML01: Migrate VoiceOS learning data (5h)
- VML02: Enable VoiceOS embedding cache (4h)
- VML05: Remove VoiceOS legacy ML tables (1h)

**Responsibilities:**
- AVA embedding migration (maintain 95% cache hit)
- VoiceOS learning data migration
- Enable VoiceOS embedding cache (10-20x speedup)
- BLOB checksum verification
- FTS4 optimization

---

### Agent 5: Integration & Testing Lead

**Total Tasks:** 25 (All phases - testing)
**Total Hours:** 50 hours (1.25 weeks)
**Domain:** Cross-module integration, performance testing, validation

**Tasks:**
- V05-V07: VoiceOS testing (functional, performance, checkpoint) - 9h
- A04-A06: AVA testing (functional, performance, checkpoint) - 10h
- W04-W05: WebAvanue testing (functional, checkpoint) - 5h
- ML04-ML05: AVA ML/AI testing (performance, checkpoint) - 6h
- VML03-VML04: VoiceOS ML/AI testing (performance, checkpoint) - 6h
- P05: Plugin system tests (4h)
- VAL03-VAL05: Final validation (performance, monitoring, documentation) - 8h

**Responsibilities:**
- Functional tests (all modules)
- Performance tests (all targets)
- Testing checkpoints (CRITICAL gates)
- Regression detection (AVA 0.2s init, 95% cache hit)
- Production monitoring setup
- Final validation (all 32 success criteria)

---

### Scrum Master Agent (Coordination Only)

**Total Tasks:** 0 (coordination, no direct coding)
**Total Hours:** Continuous (async)
**Domain:** Swarm coordination, conflict resolution, progress tracking

**Responsibilities:**
- Coordinate 5 specialist agents
- Resolve dependencies (ensure Phase 0 completes before Phase 1)
- Track progress (87 tasks, 32 success criteria)
- Identify blockers (performance regressions, migration failures)
- Validate no conflicts (schema changes, API conflicts)
- Ensure all agents approve final solution
- Daily standups (async status updates)

**Coordination Strategy:**
- Phase 0: Agent 1 solo (blocking all others)
- Phases 1A-1C: Agents 2, 3, 5 parallel (migration + testing)
- Phases 2A-2B: Agents 3, 4, 5 parallel (ML migration + testing)
- Phase 3: Agents 2, 5 parallel (plugins + testing)
- Phase 4: Agents 3, 5 parallel (validation + monitoring)

---

## Critical Path

**Longest dependency chain (cannot be parallelized):**

S01 → S09 → V01 → V07 → A06 → ML05 → VML04 → VAL05

**Critical Path Duration:** 68 hours (1.7 weeks)

**Tasks in Critical Path:**
1. S01: Configure SQLDelight (1h)
2. S09: Generate SQLDelight code (0.5h) - depends on S01
3. V01: Create VoiceOS utilities (3h) - depends on S09
4. V07: VoiceOS testing checkpoint (2h) - depends on V01
5. A06: AVA testing checkpoint (2h) - depends on V07
6. ML05: AVA ML/AI testing checkpoint (2h) - depends on A06
7. VML04: VoiceOS ML/AI testing checkpoint (2h) - depends on ML05
8. VAL05: Final validation (2h) - depends on VML04

**Critical Path Time:** 14.5 hours (critical sequential gates)

**Note:** Even with perfect parallelization, minimum time = critical path time. Actual parallel time (117h) is much longer because many tasks depend on phase completion, not just individual task completion.

---

## Quality Gate Summary

### Overall Project Gates (from Spec)

- [ ] Test coverage ≥ 90% (critical paths)
- [ ] Build time ≤ 60 seconds
- [ ] All public APIs documented
- [ ] All tests passing (87 test tasks)
- [ ] No security vulnerabilities (GDPR compliance validated)
- [ ] Performance requirements met (VoiceOS ≤0.5s, AVA ≤0.2s)

### VoiceOS Gates (Phase 1A)

- [ ] Settings query ≤ 50ms (3x faster)
- [ ] History query ≤ 100ms
- [ ] Error query ≤ 50ms
- [ ] Data integrity: Checksum verified
- [ ] Zero data loss: Row count matches

### AVA Gates (Phase 1B - CRITICAL)

- [ ] AVA initialization ≤ 0.2s (NO REGRESSION)
- [ ] Chat message write ≤ 100ms
- [ ] Conversation load ≤ 50ms
- [ ] Settings query ≤ 50ms
- [ ] Data integrity: Checksum verified

### WebAvanue Gates (Phase 1C)

- [ ] Settings query ≤ 50ms
- [ ] History query ≤ 100ms
- [ ] Incognito mode working (not saved)
- [ ] Data integrity: Checksum verified

### AVA ML/AI Gates (Phase 2A - CRITICAL)

- [ ] Cache hit rate ≥ 95% (NO REGRESSION)
- [ ] AVA initialization ≤ 0.2s (NO REGRESSION)
- [ ] Intent recognition ≤ 0.2s
- [ ] FTS search ≤ 10ms (50x faster)
- [ ] BLOB integrity: Checksum verified

### VoiceOS ML/AI Gates (Phase 2B - CRITICAL SPEEDUP)

- [ ] VoiceOS initialization ≤ 0.5s (10-20x faster)
- [ ] Cache hit rate ≥ 90%
- [ ] Intent recognition ≤ 0.2s
- [ ] FTS search ≤ 10ms
- [ ] VoiceOS accuracy: No regression

### Plugin Gates (Phase 3)

- [ ] VoiceOS plugins: No regression
- [ ] Browser plugins: Installable and functional
- [ ] Chat plugins: Installable and functional
- [ ] Cross-module plugins: Working correctly

### Validation Gates (Phase 4)

- [ ] Rolling consolidation: Working (7-day/28-day)
- [ ] GDPR compliance: All features validated
- [ ] Performance: All targets met
- [ ] Production monitoring: Enabled
- [ ] Documentation: Complete

---

## Next Steps

1. **Review task breakdown** for completeness
2. **Verify parallel execution plan** (33% time savings)
3. **Confirm swarm agent assignments** (5 specialists)
4. **Run `/implement`** to execute (swarm auto-activates)

---

**Last Updated:** 2025-11-28
**Status:** Ready for Implementation
**Recommended Next Command:** `/implement docs/ideacode/specs/012-database-consolidation-merge/tasks.md`
