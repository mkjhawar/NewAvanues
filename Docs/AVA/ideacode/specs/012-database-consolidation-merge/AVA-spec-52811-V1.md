# Database Consolidation for Multi-Repo Merge - Feature Specification

**Feature ID:** 012
**Feature Name:** Cross-Repository Database Consolidation (VoiceOS/AVA/Avanues Merge)
**Version:** 1.0.0
**Created:** 2025-11-28
**Author:** Database Architecture Team
**Platforms:** Android (VoiceOS, AVA, WebAvanue), Backend (shared infrastructure), Desktop (future KMP expansion)
**Complexity:** Tier 3 (Complex - Multi-repository, cross-platform, data migration)
**Estimated Effort:** 6 weeks (4 phases, 24 task-days)
**Based On:** Commit 11810966 - DATABASE-CONSOLIDATION-ANALYSIS.md

---

## Executive Summary

Consolidate 63 database schema files across 4 repositories (VoiceOS, AVA, WebAvanue, MainAvanues) into a unified modular database architecture to support monorepo merge. This consolidation will eliminate duplicate functionality, enable cross-module features, share ML/AI infrastructure, and reduce maintenance burden by 30-40%.

**Key Features:**
- Unified settings/preferences system (3 separate → 1 modular)
- Unified activity history tracking (voice commands, web browsing, chat)
- Shared ML/AI embedding cache (10-20x VoiceOS performance improvement)
- Unified error logging across all modules
- Extended plugin system (voice, browser, chat plugins)

**Platforms:** Android (primary), KMP (Kotlin Multiplatform for shared logic), Backend (supporting infrastructure)

**Code Sharing Strategy:** Shared database schema via SQLDelight KMP (100% schema sharing, platform-specific repositories)

**Performance Impact:**
- VoiceOS initialization: 4-5s → 0.2-0.5s (10-20x faster via AVA's embedding cache)
- AVA initialization: 0.2s (maintained, no regression)
- Database size reduction: 15-20%
- APK size reduction: 20-30 MB (shared ML models)
- Table reduction: 55 → 42 tables (24% fewer tables to maintain)

---

## Clarifications

### Session 2025-11-28

**Clarifications gathered via /clarify command to resolve critical ambiguities:**

- **Q1: Data Retention Policy for activity_history** → **A:** Rolling 7-day/28-day consolidation strategy. Every 7 days, consolidate to top 50 commands + top 50 websites (ranked by frequency). Every 28 days, perform second-level consolidation across 4 weekly snapshots. This provides compact history with automatic deduplication while preserving user favorites.

- **Q2: Migration Failure Recovery Procedure** → **A:** Automatic rollback (transaction-based). If migration fails at any point (checksum verification fails, device crashes, storage full), automatically rollback to legacy tables with zero user intervention. Safest approach for zero-data-loss guarantee (NFR-003).

- **Q3: Migration Strategy (Dual-Write Period)** → **A:** Sequential module migration - migrate one module at a time, test all databases, confirm working, then move to next module. NO dual-write period needed. Order: VoiceOS → test → AVA → test → WebAvanue → test. Eliminates dual-write complexity, safer rollback (only one module at risk), clear testing checkpoints.

- **Q4: Privacy/GDPR Compliance** → **A:** Full GDPR compliance from day 1. Explicit user consent for tracking, easy data export (JSON format), one-click delete all history, privacy policy link. Meets EU regulations for personal data (browsing history, chat messages, voice commands).

---

## Problem Statement

### Current State

**4 Separate Repositories with Fragmented Database Architecture:**

1. **VoiceOS** (38 tables, 2 databases):
   - voiceos.db: Scraping, LearnApp, plugins, UUID management, learning systems
   - command_database: Voice commands, usage tracking, command history

2. **AVA** (11 tables, 1 database):
   - ava_database.db: Chat conversations, messages, NLU, embeddings, training data
   - **Proven optimizations:** 95% embedding cache hit rate, 0.2s initialization, FTS4 search

3. **WebAvanue** (6 tables, 1 database):
   - BrowserDatabase: Tabs, favorites, history, settings

4. **MainAvanues** (6 duplicate files):
   - Migration backups only (no unique tables)

**All repositories use SQLDelight 2.0.1 with INSERT OR REPLACE pattern (100% compatible).**

### Pain Points

#### 1. Duplicate Functionality (15-20% waste)

**Settings/Preferences (3 separate systems):**
- VoiceOS: `settings`, `user_preference`, `context_preference`, `analytics_settings`, `retention_settings`
- WebAvanue: `browser_settings` (45+ columns, monolithic)
- AVA: Android SharedPreferences (not in database)

**Problem:** Changes to settings structure require updates in 3 places, inconsistent UX, no cross-module settings sync.

**History/Usage Tracking (3 separate systems):**
- VoiceOS: `command_history`, `command_usage`, `user_interaction`, `usage_statistic`
- WebAvanue: `history_entry` (browsing history)
- AVA: `conversation`, `message` (chat history)

**Problem:** Cannot correlate user activity across modules (e.g., "user searched web, then asked AVA question"), no unified timeline, separate retention policies.

**Error Reporting (only 1 system):**
- VoiceOS: `error_report` table
- WebAvanue: Errors lost (no persistence)
- AVA: Errors lost (no persistence)

**Problem:** 66% of errors not tracked, no cross-module error correlation, debugging difficult.

#### 2. Missed ML/AI Sharing Opportunity

**VoiceOS:**
- Slow initialization: 4-5s (no embedding cache)
- Separate learning tables: `gesture_learning`, `language_model`, `recognition_learning`
- No quantized embedding support
- No FTS4 full-text search

**AVA:**
- Fast initialization: 0.2s (95% embedding cache hit rate)
- Proven optimizations: BLOB embeddings, dual model support (MobileBERT-384, mALBERT-768)
- FTS4 search: 50-100x faster than LIKE queries
- Quantized embeddings: 60% space savings

**Problem:** VoiceOS could be 10-20x faster by using AVA's proven architecture, but they don't share infrastructure.

#### 3. Isolated Plugin Systems

**VoiceOS:** Complete plugin system (plugins, plugin_dependencies, plugin_permissions, system_checkpoint)
**WebAvanue:** No plugin support (hardcoded features)
**AVA:** No plugin support (hardcoded features)

**Problem:** Cannot extend browser or chat with plugins, no unified plugin marketplace, duplicate plugin code if added to other modules.

#### 4. Maintenance Burden

- **63 schema files** to maintain (55 unique tables)
- **4 separate migration pipelines** (VoiceOS, AVA, WebAvanue, MainAvanues)
- **3 separate testing strategies** (no shared test infrastructure)
- **Inconsistent patterns** (though all use INSERT OR REPLACE, field names differ)

**Problem:** Every schema change requires 4 separate PRs, testing cycles, and migration scripts.

### Desired State

**Single Unified Database: `augmentalis.db`**

**Modular Architecture:**
- Core System (shared): `app_settings`, `activity_history`, `error_log`, `plugins`
- ML/AI System (shared): `ml_embedding_cache`, `ml_training_example`, `ml_model_metadata`
- VoiceOS Module: 24 tables (voice_* prefix)
- Browser Module: 6 tables (browser_* prefix)
- Chat Module: 4 tables (chat_* prefix)

**Benefits:**
- ✅ 24% table reduction (55 → 42 tables)
- ✅ Single migration pipeline (4 → 1)
- ✅ Cross-module features enabled (voice commands in browser, browser bookmarks in chat)
- ✅ VoiceOS 10-20x faster initialization (shared embedding cache)
- ✅ 20-30 MB APK savings (shared ML models)
- ✅ Unified error tracking (all modules)
- ✅ Consistent UX (shared settings patterns)

---

## Requirements

### Functional Requirements

#### FR-001: Unified Settings System

**Description:** Create modular `app_settings` table to replace 3 separate settings systems.

**Current Systems to Replace:**
- VoiceOS: `settings`, `user_preference`, `context_preference`, `analytics_settings`, `retention_settings`
- WebAvanue: `browser_settings` (45+ columns)
- AVA: Android SharedPreferences

**Schema:**
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

CREATE INDEX idx_settings_module ON app_settings(module);
CREATE INDEX idx_settings_category ON app_settings(category);
```

**Tech Stack:**
- SQLDelight 2.0.1 (KMP schema)
- ColumnAdapter for type conversions
- Repository pattern (per-module repositories)

**Implementation Notes:**
- Module isolation via `module` field (no cross-module pollution)
- Type safety via `type` field (validate at repository layer)
- Default values for backward compatibility
- Sync-friendly (key-value pairs, no complex objects)

**Migration Strategy:**
- Phase 1: Create `app_settings` table
- Phase 2: Migrate VoiceOS settings → app_settings (module='voice')
- Phase 3: Migrate WebAvanue settings → app_settings (module='browser')
- Phase 4: Migrate AVA SharedPreferences → app_settings (module='chat')
- Phase 5: Dual-write period (1-2 releases)
- Phase 6: Remove legacy tables

**Success Criteria:**
- [ ] All 3 modules read from `app_settings`
- [ ] Settings queries 3x faster (1 query vs 3 separate)
- [ ] Zero data loss (checksum verification)
- [ ] Backward compatibility (dual-read fallback)

**Acceptance Tests:**
- [ ] Change voice setting, verify read in VoiceOS
- [ ] Change browser setting, verify read in WebAvanue
- [ ] Change chat setting, verify read in AVA
- [ ] Cross-module settings don't interfere (voice settings invisible to browser)
- [ ] Type validation works (reject string for boolean field)

---

#### FR-002: Unified Activity History Tracking

**Description:** Create modular `activity_history` table to replace 3 separate history systems and enable cross-module analytics.

**Current Systems to Replace:**
- VoiceOS: `command_history`, `command_usage`, `user_interaction`, `usage_statistic`
- WebAvanue: `history_entry` (browsing history)
- AVA: `conversation`, `message` (chat history)

**Schema:**
```sql
CREATE TABLE activity_history (
    id TEXT PRIMARY KEY NOT NULL,
    module TEXT NOT NULL,             -- 'voice', 'browser', 'chat'
    activity_type TEXT NOT NULL,      -- 'command', 'web_visit', 'chat_message'
    timestamp INTEGER NOT NULL,

    -- Common fields
    title TEXT,
    content TEXT,
    metadata TEXT,                    -- JSON for module-specific data

    -- Analytics
    confidence REAL,
    success INTEGER NOT NULL DEFAULT 1,
    execution_time_ms INTEGER,
    usage_count INTEGER NOT NULL DEFAULT 1,

    -- Context
    context_id TEXT,
    user_id TEXT,
    device_id TEXT,

    -- Privacy
    is_incognito INTEGER NOT NULL DEFAULT 0,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX idx_activity_module ON activity_history(module);
CREATE INDEX idx_activity_type ON activity_history(activity_type);
CREATE INDEX idx_activity_timestamp ON activity_history(timestamp DESC);
CREATE INDEX idx_activity_context ON activity_history(context_id);
```

**Tech Stack:**
- SQLDelight 2.0.1 (KMP schema)
- JSON metadata field (module-specific data)
- Repository pattern (per-module repositories)

**Module-Specific Metadata Examples:**

**Voice Command:**
```json
{
  "originalText": "open browser",
  "processedCommand": "com.augmentalis.browser.LAUNCH",
  "language": "en-US",
  "engineUsed": "GEMINI_NANO",
  "elementId": "button_launch_browser"
}
```

**Browser Visit:**
```json
{
  "url": "https://example.com",
  "favicon": "base64...",
  "visit_duration": 45000,
  "referrer": "https://google.com",
  "search_terms": "kotlin multiplatform"
}
```

**Chat Message:**
```json
{
  "conversationId": "conv-123",
  "role": "user",
  "modelUsed": "gemini-2.0-flash",
  "tokens": 150,
  "ragContext": ["doc1", "doc2"]
}
```

**Implementation Notes:**
- Unified timeline (cross-module activity correlation)
- **Retention Policy (Clarification Q1):** Rolling 7-day/28-day consolidation strategy
  - Every 7 days: Consolidate to top 50 commands + top 50 websites (ranked by frequency/recency)
  - Every 28 days: Second-level consolidation across 4 weekly snapshots
  - Benefits: Compact history, automatic deduplication, preserves favorites, space-efficient
  - Storage savings: ~95% reduction vs keeping all raw data
- Global search + module filter
- Privacy controls (incognito mode, clear all history, GDPR compliance)

**Cross-Module Features Enabled:**
- "User searched web for 'kotlin multiplatform', then asked AVA 'what is KMP?'" (correlation)
- Unified timeline UI (Activity Feed showing all user actions)
- Smart suggestions ("You searched this last week")

**Migration Strategy (Clarification Q3 - Sequential, No Dual-Write):**
- Phase 1: Create `activity_history` table (empty, ready)
- Phase 2: Migrate VoiceOS → activity_history (module='voice', type='command')
  - Test all VoiceOS databases, confirm working
  - Remove VoiceOS legacy tables (command_history, user_interaction, usage_statistic)
- Phase 3: Migrate AVA → activity_history (module='chat', type='chat_message')
  - Test all AVA databases, confirm working
  - Remove AVA legacy tables (conversation, message)
- Phase 4: Migrate WebAvanue → activity_history (module='browser', type='web_visit')
  - Test all WebAvanue databases, confirm working
  - Remove WebAvanue legacy tables (history_entry)
- **No dual-write period:** Each module fully migrates and validates before next module starts
- **Automatic rollback (Q2):** If any module fails, auto-rollback to its legacy tables

**Success Criteria:**
- [ ] All 3 modules write to `activity_history`
- [ ] Cross-module timeline query works (e.g., "all activity from today")
- [ ] Module filtering works (e.g., "only browser history")
- [ ] Zero data loss (checksum verification)
- [ ] Retention policies work per module

**Acceptance Tests:**
- [ ] Execute voice command, verify in activity_history (module='voice')
- [ ] Visit webpage, verify in activity_history (module='browser')
- [ ] Send chat message, verify in activity_history (module='chat')
- [ ] Query timeline across all modules (sorted by timestamp)
- [ ] Privacy mode works (incognito browsing not saved)
- [ ] Retention policy deletes old records per module settings

---

#### FR-003: Shared ML/AI Embedding Cache (CRITICAL PERFORMANCE)

**Description:** Create shared `ml_embedding_cache`, `ml_training_example`, and `ml_model_metadata` tables to enable VoiceOS to use AVA's proven 95% cache hit rate architecture.

**Current Systems:**
- **AVA:** `intent_embedding` (95% cache hit, 0.2s init), `embedding_metadata`, `intent_example`, `train_example`, `train_example_fts`
- **VoiceOS:** `language_model`, `gesture_learning`, `recognition_learning` (no cache, 4-5s init)

**Problem:** VoiceOS is 20-25x slower than AVA for same embedding operations.

**Proposed Shared Schema:**

**Embedding Cache (from AVA's proven design):**
```sql
CREATE TABLE ml_embedding_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    module TEXT NOT NULL,                    -- 'voice', 'chat', 'browser'

    -- Embedding identity
    item_id TEXT NOT NULL,                   -- Intent ID, gesture ID, command ID
    item_type TEXT NOT NULL,                 -- 'intent', 'gesture', 'command', 'webpage'
    locale TEXT NOT NULL,

    -- Embedding data (AVA's BLOB optimization)
    embedding_vector BLOB NOT NULL,          -- Quantized INT8 embeddings
    embedding_dimension INTEGER NOT NULL,    -- 384 or 768
    model_version TEXT NOT NULL,
    normalization_type TEXT NOT NULL DEFAULT 'l2',

    -- Metadata
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    example_count INTEGER NOT NULL DEFAULT 1,
    source TEXT NOT NULL,                    -- 'AVA_FILE', 'VOICEOS_SCRAPE', 'USER_TRAINED'

    UNIQUE(module, item_id, locale)
);

CREATE UNIQUE INDEX idx_ml_embedding_module_item_locale
ON ml_embedding_cache(module, item_id, locale);

CREATE INDEX idx_ml_embedding_model_version
ON ml_embedding_cache(model_version);
```

**Training Examples (unified):**
```sql
CREATE TABLE ml_training_example (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    module TEXT NOT NULL,

    -- Training data
    input_text TEXT NOT NULL,
    target_label TEXT NOT NULL,              -- Intent, command, gesture name
    locale TEXT NOT NULL,

    -- Metadata
    source TEXT NOT NULL,                    -- 'USER', 'SYNTHETIC', 'SCRAPED', 'CORRECTED'
    confidence REAL,
    hash TEXT NOT NULL,                      -- Deduplication (AVA pattern)

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    UNIQUE(hash)
);

-- FTS4 for fast search (AVA's 50-100x optimization)
CREATE VIRTUAL TABLE ml_training_example_fts USING fts4(
    content='ml_training_example',
    input_text,
    target_label,
    locale
);

-- Triggers (AVA's DELETE + INSERT pattern for FTS4)
CREATE TRIGGER ml_training_fts_insert AFTER INSERT ON ml_training_example
BEGIN
    INSERT INTO ml_training_example_fts(rowid, input_text, target_label, locale)
    VALUES (new.id, new.input_text, new.target_label, new.locale);
END;

CREATE TRIGGER ml_training_fts_update AFTER UPDATE ON ml_training_example
BEGIN
    DELETE FROM ml_training_example_fts WHERE rowid = old.id;
    INSERT INTO ml_training_example_fts(rowid, input_text, target_label, locale)
    VALUES (new.id, new.input_text, new.target_label, new.locale);
END;

CREATE TRIGGER ml_training_fts_delete AFTER DELETE ON ml_training_example
BEGIN
    DELETE FROM ml_training_example_fts WHERE rowid = old.id;
END;
```

**Model Metadata:**
```sql
CREATE TABLE ml_model_metadata (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    module TEXT NOT NULL,
    model_type TEXT NOT NULL,                -- 'EMBEDDING', 'LLM', 'TTS', 'STT'

    -- Model info
    model_id TEXT NOT NULL,
    model_version TEXT NOT NULL,
    model_dimension INTEGER,
    quantization TEXT,                       -- 'INT8', 'FP16', 'FP32'

    -- Performance (track cache effectiveness)
    avg_inference_ms REAL,
    cache_hit_rate REAL,

    -- Lifecycle
    loaded_at INTEGER,
    last_used_at INTEGER,
    usage_count INTEGER NOT NULL DEFAULT 0,

    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,

    UNIQUE(module, model_type, model_id)
);
```

**Tech Stack:**
- SQLDelight 2.0.1 (KMP schema)
- BLOB ColumnAdapter (ByteArray ↔ BLOB)
- FTS4 triggers (DELETE + INSERT pattern)
- Repository pattern (shared ML repository)

**Expected Performance Gains:**
- **VoiceOS initialization:** 4-5s → 0.2-0.5s (10-20x faster)
- **AVA initialization:** 0.2s (maintained, no regression)
- **Cache hit rate:** 95% (proven in AVA)
- **FTS search:** 250ms → 5ms (50x faster)

**Implementation Notes:**
- Migrate AVA embeddings first (validate no regression)
- Then migrate VoiceOS (measure 10-20x speedup)
- Shared model downloads (20-30 MB APK savings)
- Invalidate cache on model version change

**Migration Strategy:**
- Phase 1: Create ml_* tables
- Phase 2: Migrate AVA intent_embedding → ml_embedding_cache (module='chat')
- Phase 3: Verify AVA 95% cache hit rate maintained
- Phase 4: Migrate VoiceOS language_model → ml_training_example (module='voice')
- Phase 5: Update VoiceOS to use embedding cache
- Phase 6: Benchmark VoiceOS speedup (expect 10-20x)
- Phase 7: Remove legacy tables

**Success Criteria:**
- [ ] AVA maintains 95% cache hit rate (0.2s init)
- [ ] VoiceOS achieves 10-20x faster init (0.2-0.5s)
- [ ] FTS search works (5ms latency)
- [ ] Shared models reduce APK size by 20-30 MB
- [ ] Cache invalidation works on model update

**Acceptance Tests:**
- [ ] AVA intent recognition still 0.2s (no regression)
- [ ] VoiceOS command recognition now 0.2-0.5s (from 4-5s)
- [ ] FTS search returns results in <10ms
- [ ] Cache hit rate metrics tracked in ml_model_metadata
- [ ] Model version mismatch invalidates cache

---

#### FR-004: Unified Error Logging System

**Description:** Create modular `error_log` table to capture errors from all modules (currently only VoiceOS tracks errors).

**Current State:**
- VoiceOS: `error_report` table ✅
- WebAvanue: Errors lost ❌
- AVA: Errors lost ❌

**Problem:** 66% of errors not tracked, no cross-module error correlation, debugging difficult.

**Schema:**
```sql
CREATE TABLE error_log (
    id TEXT PRIMARY KEY NOT NULL,
    module TEXT NOT NULL,                    -- 'voice', 'browser', 'chat', 'system'
    severity TEXT NOT NULL,                  -- 'DEBUG', 'INFO', 'WARNING', 'ERROR', 'FATAL'

    -- Error details
    error_type TEXT NOT NULL,                -- Exception class name
    error_message TEXT NOT NULL,
    stack_trace TEXT,

    -- Context
    timestamp INTEGER NOT NULL,
    user_id TEXT,
    device_id TEXT,
    app_version TEXT NOT NULL,
    os_version TEXT NOT NULL,

    -- Reproduction
    activity_id TEXT,                        -- Link to activity_history
    steps_to_reproduce TEXT,

    -- Analytics
    first_occurred INTEGER NOT NULL,
    occurrence_count INTEGER NOT NULL DEFAULT 1,
    last_occurred INTEGER NOT NULL,

    -- Status
    is_resolved INTEGER NOT NULL DEFAULT 0,
    resolution_notes TEXT
);

CREATE INDEX idx_error_module ON error_log(module);
CREATE INDEX idx_error_severity ON error_log(severity);
CREATE INDEX idx_error_timestamp ON error_log(timestamp DESC);
CREATE INDEX idx_error_resolved ON error_log(is_resolved);
```

**Tech Stack:**
- SQLDelight 2.0.1 (KMP schema)
- Foreign key to activity_history (optional context)
- Deduplication via error hash (occurrence_count)

**Implementation Notes:**
- All uncaught exceptions → error_log
- Correlation with activity_history (what user was doing)
- Deduplication (same error increments occurrence_count)
- Privacy controls (opt-out per module via app_settings)

**Migration Strategy:**
- Phase 1: Create error_log table
- Phase 2: Migrate VoiceOS error_report → error_log (module='voice')
- Phase 3: Add error logging to WebAvanue (module='browser')
- Phase 4: Add error logging to AVA (module='chat')
- Phase 5: Keep VoiceOS error_report for 1 release
- Phase 6: Remove error_report table

**Success Criteria:**
- [ ] All 3 modules write to error_log
- [ ] Errors correlated with activity_history
- [ ] Deduplication works (occurrence_count increments)
- [ ] Zero data loss from VoiceOS migration

**Acceptance Tests:**
- [ ] Throw exception in VoiceOS, verify in error_log
- [ ] Throw exception in WebAvanue, verify in error_log
- [ ] Throw exception in AVA, verify in error_log
- [ ] Same error twice increments occurrence_count
- [ ] Stack trace captured correctly
- [ ] Activity context linked (if available)

---

#### FR-005: Extended Plugin System (All Modules)

**Description:** Extend VoiceOS plugin system to support browser extensions and chat plugins.

**Current State:**
- VoiceOS: Complete plugin system ✅ (`plugins`, `plugin_dependencies`, `plugin_permissions`, `system_checkpoint`)
- WebAvanue: No plugins ❌
- AVA: No plugins ❌

**Opportunity:** Enable browser extensions (ad blockers, password managers) and chat plugins (custom AI actions, tools) using existing VoiceOS infrastructure.

**Schema Changes (additive only):**
```sql
-- Add module field to existing tables (backward compatible)
ALTER TABLE plugins ADD COLUMN module TEXT NOT NULL DEFAULT 'voice';
ALTER TABLE plugin_permissions ADD COLUMN module TEXT NOT NULL DEFAULT 'voice';

-- Now supports:
-- module='voice' - VoiceOS plugins (existing)
-- module='browser' - WebAvanue extensions (new)
-- module='chat' - AVA actions/tools (new)
-- module='system' - Global plugins (new)
```

**Tech Stack:**
- SQLDelight 2.0.1 (schema update)
- Module field (isolate plugins by module)
- Plugin APIs (per-module interfaces)

**Implementation Notes:**
- Keep VoiceOS plugin system exactly as-is (zero risk)
- Add module='browser' support (WebAvanue extensions)
- Add module='chat' support (AVA AI actions)
- Cross-module plugins possible (e.g., voice command to open bookmarked page)

**Migration Strategy:**
- Phase 1: Add module field to plugins, plugin_permissions (default 'voice')
- Phase 2: Create plugin APIs for browser module
- Phase 3: Create plugin APIs for chat module
- Phase 4: Test cross-module plugin (voice → browser)

**Success Criteria:**
- [ ] VoiceOS plugins still work (no regression)
- [ ] Browser extensions installable via plugin system
- [ ] Chat plugins installable via plugin system
- [ ] Cross-module plugins work (voice command to browser)

**Acceptance Tests:**
- [ ] Install VoiceOS plugin, verify works (backward compat)
- [ ] Install browser extension (ad blocker), verify blocks ads
- [ ] Install chat plugin (custom AI action), verify executes
- [ ] Install cross-module plugin (voice → browser), verify works

---

### Non-Functional Requirements

#### NFR-001: Performance

**Targets:**
- VoiceOS initialization: **≤ 0.5s** (from 4-5s) - 10x minimum improvement
- AVA initialization: **≤ 0.2s** (maintain current) - zero regression
- Settings query: **≤ 50ms** (from 150ms) - 3x improvement
- History query: **≤ 100ms** (any module, any time range)
- Error log query: **≤ 50ms** (for debugging)
- Embedding cache hit rate: **≥ 95%** (AVA proven rate)
- FTS search latency: **≤ 10ms** (from 250ms) - 25x improvement

**Measurement:**
- Benchmark before/after migration (all phases)
- Continuous monitoring (cache hit rate, query latency)
- Regression tests (AVA must maintain 0.2s init)

#### NFR-002: Security & Privacy

**Requirements:**
- Module isolation (voice settings invisible to browser)
- Privacy controls (incognito mode, clear history per module)
- Secure error logging (no PII in stack traces)
- Plugin permissions (module-scoped)
- **GDPR Compliance (Clarification Q4):** Full compliance for personal data handling

**Implementation:**
- CHECK constraints on module field (validate at database level)
- Repository pattern (module-specific repositories filter by module)
- PII scrubbing (error logs)
- **GDPR Compliance (Q4):**
  - **Explicit Consent:** User consent dialog on first launch for activity tracking
  - **Data Export:** One-click export all history to JSON file (GDPR Article 20 - right to data portability)
  - **Right to Erasure:** One-click "Delete All History" button (GDPR Article 17 - right to be forgotten)
  - **Privacy Policy:** Link to privacy policy explaining what data is collected and why
  - **Transparency:** Clear indication in settings which modules are tracking activity
  - **Granular Controls:** Per-module consent (can disable browser history but keep voice history)
  - **Incognito Mode:** Activity not saved when incognito enabled (privacy-by-design)

#### NFR-003: Data Integrity

**Requirements:**
- Zero data loss during migration (all phases)
- Checksum verification (before/after)
- **Automatic rollback on failure (Clarification Q2):** Transaction-based migration with automatic rollback
- **Sequential module migration (Clarification Q3):** One module at a time, no dual-write complexity

**Implementation:**
- INSERT OR REPLACE pattern (VOS4 proven, zero data loss)
- Checksum verification script (pre/post migration)
- **Automatic Rollback (Q2):** If migration fails at any point:
  - Checksum verification fails → automatic rollback
  - Device crashes → resume or rollback on restart
  - Storage full → rollback and notify user
  - Database corruption → rollback to last known good state
  - Zero user intervention required
- **Sequential Migration (Q3):** Migrate VoiceOS → test → AVA → test → WebAvanue → test
  - NO dual-write period (eliminates complexity)
  - Each module fully validated before next module starts
  - Clear rollback boundary (only one module at risk)

#### NFR-004: Backward Compatibility

**Requirements (Updated per Clarification Q3):**
- **Sequential migration:** Each module migrates fully before next module starts (no dual-write/dual-read)
- Legacy tables removed immediately after successful module migration
- No breaking API changes (repository interfaces unchanged)
- Automatic rollback on failure (reverts to legacy tables for failed module only)

**Implementation:**
- Migration per module (VoiceOS → AVA → WebAvanue)
- Each module tested and validated before removal of legacy tables
- Phased rollout possible (release Module 1 migration, then Module 2 in next release)
- Version detection (app startup checks which modules have migrated)
- Rollback flag per module (if Module 2 fails, Module 1 stays migrated)

#### NFR-005: Accessibility

**Requirements:**
- Database operations accessible from all modules
- Module-scoped queries (no cross-contamination)
- Clear error messages (migration failures)

**Implementation:**
- Repository pattern (per-module repositories)
- Module field validation (database-level)
- Migration status UI (show progress to user)

### Success Criteria

#### Phase 1: Core Consolidation (Week 1-2) - Sequential Migration

- [ ] `app_settings` table created and tested
- [ ] `activity_history` table created and tested
- [ ] `error_log` table created and tested
- [ ] **VoiceOS Module Migration:**
  - [ ] VoiceOS settings → app_settings (module='voice')
  - [ ] VoiceOS history → activity_history (module='voice')
  - [ ] VoiceOS errors → error_log (module='voice')
  - [ ] Test VoiceOS databases, confirm working
  - [ ] Remove VoiceOS legacy tables
  - [ ] Automatic rollback ready if failure occurs
- [ ] **AVA Module Migration:**
  - [ ] AVA SharedPreferences → app_settings (module='chat')
  - [ ] AVA conversation/message → activity_history (module='chat')
  - [ ] AVA errors → error_log (module='chat')
  - [ ] Test AVA databases, confirm working (0.2s init maintained)
  - [ ] Remove AVA legacy tables
- [ ] **WebAvanue Module Migration:**
  - [ ] WebAvanue settings → app_settings (module='browser')
  - [ ] WebAvanue history → activity_history (module='browser')
  - [ ] WebAvanue errors → error_log (module='browser')
  - [ ] Test WebAvanue databases, confirm working
  - [ ] Remove WebAvanue legacy tables
- [ ] Settings queries 3x faster (verified)
- [ ] Zero data loss (checksum verification passed for all modules)
- [ ] GDPR consent dialog implemented

#### Phase 2: ML/AI Consolidation (Week 3-4)

- [ ] `ml_embedding_cache` created and tested
- [ ] `ml_training_example` + FTS created and tested
- [ ] `ml_model_metadata` created and tested
- [ ] AVA embeddings migrated (module='chat')
- [ ] AVA maintains 95% cache hit rate (verified)
- [ ] AVA initialization still 0.2s (no regression)
- [ ] VoiceOS training data migrated (module='voice')
- [ ] VoiceOS uses embedding cache
- [ ] VoiceOS initialization 10-20x faster (0.2-0.5s verified)
- [ ] FTS search 50x faster (≤10ms verified)

#### Phase 3: Plugin Extension (Week 5)

- [ ] Module field added to plugin tables
- [ ] VoiceOS plugins still work (no regression)
- [ ] Browser plugin API created
- [ ] Chat plugin API created
- [ ] Test browser extension installed successfully
- [ ] Test chat plugin installed successfully

#### Phase 4: Validation & Optimization (Week 6)

- [ ] All modules fully migrated and validated (no legacy tables remaining)
- [ ] **Rolling Consolidation (Q1):** 7-day/28-day consolidation working
  - [ ] Top 50 commands per week consolidated
  - [ ] Top 50 websites per week consolidated
  - [ ] Monthly consolidation across 4 weeks working
  - [ ] Storage savings verified (~95% reduction vs raw data)
- [ ] **GDPR Compliance (Q4):** All privacy features working
  - [ ] Consent dialog on first launch
  - [ ] One-click data export (JSON)
  - [ ] One-click delete all history
  - [ ] Privacy policy link present
  - [ ] Per-module consent controls working
- [ ] Database size reduced by 15-20% (verified)
- [ ] Performance targets met (VoiceOS ≤0.5s, AVA ≤0.2s)
- [ ] Production monitoring enabled (cache hit rate, query latency)

---

## Platform-Specific Details

### Android (Primary Platform)

**Tech Stack:**
- **Language:** Kotlin 1.9+
- **Database:** SQLDelight 2.0.1 (KMP)
- **Driver:** AndroidSqliteDriver
- **Architecture:** Repository pattern (per-module repositories)
- **DI:** Hilt (dependency injection for repositories)
- **Testing:** JUnit + Robolectric (database tests)

**Key Components:**
- **Unified Database:** `augmentalis.db` (single SQLite file)
- **Module Repositories:**
  - `AppSettingsRepository` (shared, module-filtered)
  - `ActivityHistoryRepository` (shared, module-filtered)
  - `ErrorLogRepository` (shared, module-filtered)
  - `MLEmbeddingCacheRepository` (shared, module-filtered)
  - `PluginRepository` (shared, module-filtered)

**Integration Points:**
- VoiceOS → reads from `app_settings` (module='voice')
- WebAvanue → reads from `app_settings` (module='browser')
- AVA → reads from `app_settings` (module='chat')
- All modules → write to `activity_history` (timeline UI)
- All modules → write to `error_log` (crash reporting)

**Platform-Specific:**
- AndroidSqliteDriver (native SQLite)
- EncryptedSharedPreferences for sensitive settings (optional, per-module)
- WorkManager for background migration (large datasets)

**Testing Strategy:**
- Unit tests (repository pattern, mocked database)
- Integration tests (Robolectric, real SQLite database)
- Migration tests (checksum verification)
- Performance tests (benchmark before/after)

---

### Shared/KMP (Kotlin Multiplatform)

**Tech Stack:**
- **Language:** Kotlin 1.9+
- **Database:** SQLDelight 2.0.1 (commonMain schema)
- **Targets:** android, iosArm64, iosSimulatorArm64 (future: desktop)
- **Code Sharing:** 100% schema, 80% repository logic

**Shared Code (in commonMain):**
- All .sq schema files (app_settings, activity_history, error_log, ml_*)
- Repository interfaces (IAppSettingsRepository, etc.)
- Domain models (Settings, Activity, Error, Embedding, etc.)
- Query wrappers (type-safe query builders)

**Platform-Specific (in androidMain, iosMain):**
- Database drivers (AndroidSqliteDriver, NativeSqliteDriver)
- Platform storage paths (/data/data/... vs Documents/)
- Encryption (Android Keystore vs iOS Keychain)

**Benefits:**
- **100% schema sharing** (single source of truth)
- **80% repository logic sharing** (query logic identical)
- **Type-safe queries** (compile-time verification)
- **Easier testing** (shared test code)

**Implementation Notes:**
- Schema in commonMain (all platforms use same schema)
- Drivers in platform-specific source sets
- Repository implementations in platform-specific (platform-optimized)

---

### Backend (Supporting Infrastructure - Future)

**Tech Stack:**
- **Language:** Kotlin (align with Android/KMP)
- **Framework:** Ktor (KMP-compatible)
- **Database:** PostgreSQL (for cloud sync)
- **Cache:** Redis (for distributed cache)

**Key Components:**
- **Sync API:** REST/GraphQL API for syncing settings, history, errors
- **Cloud ML:** Shared embedding cache (distributed)
- **Analytics:** Cross-module usage analytics

**Integration Points:**
- Android → sync app_settings to cloud (multi-device)
- Android → sync activity_history to cloud (timeline across devices)
- Android → upload error_log to cloud (crash analytics)

**Platform-Specific:**
- PostgreSQL schema (mapping from SQLite to PostgreSQL)
- Redis cache (embedding cache distributed)
- JWT auth (secure sync)

**Testing Strategy:**
- API tests (REST/GraphQL endpoints)
- Database migration tests (SQLite → PostgreSQL)
- Load tests (concurrent sync requests)

**Note:** Backend implementation deferred to Q2 2025 (after Android consolidation complete).

---

## User Stories

### Story 1: VoiceOS User Benefits from AVA's Speed

**As a** VoiceOS user
**I want** fast voice command recognition (<1s)
**So that** I can use voice commands without waiting

**Platforms:** Android (VoiceOS module)

**Acceptance Criteria:**
- [ ] VoiceOS initialization ≤ 0.5s (from 4-5s)
- [ ] Voice command recognition ≤ 0.2s (95% cache hit rate)
- [ ] No accuracy regression (same recognition quality)
- [ ] No user-facing changes (transparent performance improvement)

**Technical Implementation:**
- VoiceOS uses shared `ml_embedding_cache` (AVA's proven design)
- Pre-computed embeddings loaded from BLOB (60% smaller)
- Cache hit rate tracked in `ml_model_metadata`

---

### Story 2: Unified Activity Timeline

**As a** user
**I want** to see all my activity in one timeline
**So that** I can track what I did across voice, browser, and chat

**Platforms:** Android (all modules: VoiceOS, WebAvanue, AVA)

**Acceptance Criteria:**
- [ ] Timeline shows voice commands, web visits, chat messages (sorted by time)
- [ ] Filter by module (show only browser history)
- [ ] Search across all activity (unified search)
- [ ] Privacy controls (incognito mode excluded)
- [ ] Retention policy works per module

**Technical Implementation:**
- All modules write to `activity_history` table
- Timeline UI queries activity_history (sorted by timestamp DESC)
- Module filter (WHERE module = ?)
- Full-text search (FTS4 on title + content)

---

### Story 3: Browser Extensions via Plugin System

**As a** WebAvanue user
**I want** to install browser extensions (ad blocker, password manager)
**So that** I can customize my browsing experience

**Platforms:** Android (WebAvanue module)

**Acceptance Criteria:**
- [ ] Install browser extension from plugin marketplace
- [ ] Extension permissions requested (WebAvanue-scoped)
- [ ] Extension runs (ad blocker blocks ads)
- [ ] Extension can be disabled/enabled
- [ ] Extension uninstalls cleanly

**Technical Implementation:**
- WebAvanue uses `plugins` table (module='browser')
- Plugin API defined for browser (intercept requests, modify DOM)
- Plugin permissions (module='browser' scoped)

---

### Story 4: Chat AI Plugins

**As an** AVA user
**I want** to install custom AI actions (e.g., "summarize PDF")
**So that** I can extend AVA's capabilities

**Platforms:** Android (AVA module)

**Acceptance Criteria:**
- [ ] Install chat plugin from plugin marketplace
- [ ] Plugin registers new AI action ("summarize PDF")
- [ ] Plugin executes when user invokes action
- [ ] Plugin permissions requested (AVA-scoped)
- [ ] Plugin can be disabled/uninstalled

**Technical Implementation:**
- AVA uses `plugins` table (module='chat')
- Plugin API defined for chat (register actions, execute tools)
- Plugin permissions (module='chat' scoped)

---

### Story 5: Cross-Module Error Correlation

**As a** developer
**I want** to see errors across all modules in one dashboard
**So that** I can debug cross-module issues

**Platforms:** Android (all modules), Backend (analytics dashboard)

**Acceptance Criteria:**
- [ ] Error dashboard shows errors from voice, browser, chat
- [ ] Filter by module, severity, time range
- [ ] Errors correlated with activity_history (what user was doing)
- [ ] Deduplication works (same error shows occurrence count)
- [ ] Stack traces captured correctly

**Technical Implementation:**
- All modules write to `error_log` table
- Error dashboard queries error_log (filtered, sorted)
- Foreign key to activity_history (optional context)

---

## Technical Constraints

### Database Technology

**Required:**
- SQLDelight 2.0.1 (all repositories already on this version)
- INSERT OR REPLACE pattern (VOS4 proven, zero data loss)
- SQLite 3.38+ dialect (Android 8.0+ API 26)

**Constraints:**
- Must maintain VoiceOS 38 tables (module-prefixed)
- Must maintain AVA 11 tables (module-prefixed)
- Must maintain WebAvanue 6 tables (module-prefixed)
- Cannot break existing VoiceOS plugin system

### Android Platform

**Minimum Requirements:**
- Android API 26+ (Android 8.0)
- Kotlin 1.9+
- SQLDelight 2.0.1
- AndroidSqliteDriver

**Database Constraints:**
- Max database size: 2 GB (SQLite limit)
- Max table count: No practical limit (using 42 tables)
- Max index count: No practical limit (using ~60 indices)

### Kotlin Multiplatform

**Shared Code (commonMain):**
- All .sq schema files (100% sharing)
- Repository interfaces (100% sharing)
- Domain models (100% sharing)

**Platform-Specific (androidMain, iosMain):**
- Database drivers (AndroidSqliteDriver, NativeSqliteDriver)
- Storage paths (platform-specific)
- Encryption (platform-specific)

### Performance Constraints

**Must Maintain:**
- AVA initialization ≤ 0.2s (no regression)
- AVA cache hit rate ≥ 95% (no regression)

**Must Achieve:**
- VoiceOS initialization ≤ 0.5s (10x improvement from 4-5s)
- Settings query ≤ 50ms (3x improvement)
- FTS search ≤ 10ms (25x improvement)

---

## Dependencies

### Cross-Platform Dependencies

**Database Migration Sequence:**
1. Backend: N/A (no backend in Phase 1-4)
2. Shared/KMP: Schema created first (in commonMain)
3. Android: Migration scripts (after schema ready)

**Critical Path:**
- `app_settings` must be ready before any module can migrate settings
- `ml_embedding_cache` must be ready before AVA/VoiceOS can migrate
- AVA migration must complete (and verify no regression) before VoiceOS migrates

**Timing:**
- Phase 1 (settings, history, errors) can run in parallel
- Phase 2 (ML/AI) depends on Phase 1 completion (shared infrastructure)
- Phase 3 (plugins) depends on Phase 1 (uses app_settings for plugin config)
- Phase 4 (cleanup) depends on all phases (1-2 releases in production)

### External Dependencies

**None (Internal Only):**
- No third-party services
- No external APIs
- No cloud dependencies (Phase 1-4 Android-only)

**Future (Deferred to Q2 2025):**
- Backend sync API (PostgreSQL, Redis)
- Cloud analytics (error logging, usage analytics)

### Repository Dependencies

**VoiceOS:**
- VoiceOS codebase (read-only, understand existing plugin system)
- VoiceOS database schema (38 .sq files)

**AVA:**
- AVA codebase (read-only, understand embedding cache)
- AVA database schema (11 .sq files)

**WebAvanue:**
- WebAvanue codebase (read-only, understand browser settings)
- WebAvanue database schema (6 .sq files)

### Development Dependencies

**Required Tools:**
- Kotlin 1.9+
- Gradle 8.0+
- SQLDelight Gradle Plugin 2.0.1
- Android Studio (database inspection)

**Testing Tools:**
- JUnit 5 (unit tests)
- Robolectric (Android database tests)
- Checksum verification script (data integrity)

---

## Out of Scope

### Not Included in Phases 1-4

**Backend Sync (Deferred to Q2 2025):**
- Cloud sync of settings/history/errors
- Multi-device sync
- PostgreSQL migration
- Redis distributed cache

**iOS Implementation (Deferred to Q3 2025):**
- NativeSqliteDriver (iOS)
- iOS-specific repositories
- iOS app integration

**Desktop Implementation (Deferred to Q4 2025):**
- Desktop SQLite driver (JVM)
- Desktop app integration

**Web Implementation (Out of Scope):**
- WebAvanue is Android browser (not web app)
- No web version planned

### Future Enhancements (Post-Consolidation)

**Advanced Analytics:**
- Cross-module user journey analytics
- Predictive suggestions (ML-powered)
- Usage heatmaps

**Advanced Plugins:**
- Plugin marketplace (install from cloud)
- Cross-module plugins (voice → browser → chat)
- Plugin sandboxing (security)

**Advanced ML/AI:**
- Federated learning (train on-device)
- Model fine-tuning (user-specific)
- Multi-model support (>2 models)

---

## Swarm Activation Assessment

### Swarm Mode: **RECOMMENDED**

**Reasoning:**

#### Multiple Platforms (3):
- Android (VoiceOS, AVA, WebAvanue) - primary platform
- KMP (shared schema) - cross-platform support
- Backend (future) - cloud sync infrastructure

#### High Task Count:
- Estimated subtasks: **32** (threshold: 15+)
  - Phase 1: 8 tasks (settings, history, errors)
  - Phase 2: 12 tasks (ML/AI consolidation)
  - Phase 3: 6 tasks (plugin extension)
  - Phase 4: 6 tasks (cleanup)

#### Multiple Domains:
- **Database Architecture** (schema design, migration)
- **Android Development** (repositories, DI, UI)
- **ML/AI Infrastructure** (embeddings, FTS, cache optimization)
- **Data Migration** (zero data loss, checksum verification)
- **Performance Optimization** (10-20x speedup target)

### Recommended Swarm Agents

#### Agent 1: Database Architect
**Domain:** Schema design, migration strategy
**Responsibilities:**
- Design unified schemas (app_settings, activity_history, error_log, ml_*)
- CREATE TABLE statements with indices, constraints
- Migration sequence planning (Phase 1 → 2 → 3 → 4)
- Checksum verification scripts

**Why Needed:** Complex schema consolidation (55 → 42 tables), must maintain INSERT OR REPLACE pattern, must preserve AVA performance optimizations.

#### Agent 2: Android Repository Developer
**Domain:** Repository pattern, dependency injection
**Responsibilities:**
- Create shared repositories (AppSettingsRepository, ActivityHistoryRepository, etc.)
- Module-filtered queries (WHERE module = ?)
- Hilt dependency injection setup
- Type-safe query wrappers

**Why Needed:** 5 new shared repositories, must maintain module isolation, must integrate with VoiceOS/AVA/WebAvanue existing code.

#### Agent 3: ML/AI Specialist
**Domain:** Embedding cache, FTS optimization, performance
**Responsibilities:**
- Migrate AVA embeddings → ml_embedding_cache (maintain 95% hit rate)
- Migrate VoiceOS learning data → ml_training_example
- FTS4 triggers (DELETE + INSERT pattern)
- Performance benchmarking (verify 10-20x speedup)

**Why Needed:** Critical performance requirement (VoiceOS 10-20x faster), must maintain AVA 0.2s init, complex BLOB/FTS optimizations.

#### Agent 4: Data Migration Engineer
**Domain:** Data integrity, zero data loss, dual-write
**Responsibilities:**
- Migration scripts (Phase 1, 2, 3)
- Checksum verification (before/after)
- Dual-write implementation (1-2 release safety)
- Rollback plan (keep legacy tables)

**Why Needed:** Zero data loss requirement (critical), 55 tables to migrate, phased rollout (4 phases), backward compatibility.

#### Agent 5: Integration & Testing Lead
**Domain:** Cross-module integration, testing, validation
**Responsibilities:**
- Integration tests (VoiceOS + AVA + WebAvanue)
- Performance tests (before/after benchmarks)
- Regression tests (AVA must stay 0.2s)
- Migration validation (all success criteria)

**Why Needed:** Cross-module dependencies, 32 success criteria to validate, performance SLAs (≤0.5s VoiceOS, ≤0.2s AVA).

### Swarm Coordination

**Scrum Master Agent:**
- Coordinates 5 specialist agents
- Validates no conflicts between phases
- Ensures all agents approve final solution
- Tracks success criteria (32 total)

**Agent Dependencies:**
1. Database Architect → designs schemas (all agents depend on this)
2. Android Repository Developer → implements repositories (depends on schemas)
3. ML/AI Specialist → migrates embeddings (depends on schemas + repositories)
4. Data Migration Engineer → runs migrations (depends on all above)
5. Integration & Testing Lead → validates everything (final gatekeeper)

**Parallel Work:**
- Phase 1: Agents 1, 2, 4 work in parallel (settings, history, errors)
- Phase 2: Agents 1, 2, 3, 4 work in parallel (ML/AI consolidation)
- Phase 3: Agents 2, 4 work in parallel (plugin extension)
- Phase 4: Agent 5 validates, Agent 4 cleans up

### Estimated Time Savings

**Without Swarm (Sequential):**
- Phase 1: 2 weeks (settings + history + errors, sequential)
- Phase 2: 2 weeks (ML/AI, sequential AVA then VoiceOS)
- Phase 3: 1 week (plugin extension)
- Phase 4: 1 week (cleanup)
- **Total: 6 weeks**

**With Swarm (Parallel):**
- Phase 1: 1 week (3 agents in parallel)
- Phase 2: 1.5 weeks (4 agents in parallel, ML/AI complex)
- Phase 3: 0.5 weeks (2 agents in parallel)
- Phase 4: 0.5 weeks (validation + cleanup)
- **Total: 3.5 weeks**

**Time Savings: 40% (6 weeks → 3.5 weeks)**

### Swarm Mode Benefits

✅ **Parallel implementation** across 5 domains
✅ **Domain expertise** per agent (database, Android, ML/AI, migration, testing)
✅ **Coordinated testing** (cross-module integration)
✅ **Risk mitigation** (5 agents review each other's work)
✅ **Time savings: 40%** (3.5 weeks vs 6 weeks)

---

**Last Updated:** 2025-11-28
**Status:** Ready for Planning (/plan)
**Next Command:** /plan docs/ideacode/specs/012-database-consolidation-merge/spec.md
