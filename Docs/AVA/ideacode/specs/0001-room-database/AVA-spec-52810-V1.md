# Feature Specification: Room Database with VOS4 Patterns

**ID:** 0001
**Created:** 2025-10-28 14:01
**Status:** In Progress
**Owner:** AI Assistant
**Phase:** Week 3-4 (Phase 1.0)

---

## Problem Statement

AVA AI needs a local database to persist:
- **Conversations:** Chat sessions with metadata
- **Messages:** Individual messages with timestamps
- **Train Examples:** Teach-Ava user training data
- **Decisions:** Decision history for learning
- **Learning:** Learned patterns from interactions
- **Memory:** Cognitive memory (Working/Episodic/Semantic/Procedural)

**Why Now:** Database is foundational - all features (NLU, LLM, RAG, Memory) depend on it.

**Current State:** Empty `core/data` module with no implementation.

**Desired State:** Production-ready Room database with VOS4-proven patterns (composite indices, hash-based lookups, cascading deletes).

---

## Acceptance Criteria

### Must Have (P0)

1. ✅ **6 Core Tables Created:**
   - [ ] `conversations` - Chat sessions
   - [ ] `messages` - Individual messages
   - [ ] `train_examples` - Teach-Ava training data
   - [ ] `decisions` - Decision history
   - [ ] `learning` - Learned patterns
   - [ ] `memory` - Cognitive memory entries

2. ✅ **Room Database Setup:**
   - [ ] Database class with version 1
   - [ ] All entities annotated with `@Entity`
   - [ ] All DAOs with suspend functions (coroutine-compatible)
   - [ ] Type converters for complex types (List, Map, custom types)

3. ✅ **VOS4 Patterns Applied:**
   - [ ] Composite indices for multi-field queries (e.g., `(conversationId, timestamp)`)
   - [ ] Hash-based unique identification where appropriate
   - [ ] Cascading deletes (delete conversation → delete messages)
   - [ ] Fallback patterns for optional relationships

4. ✅ **Repository Layer:**
   - [ ] Repository interfaces in `core.domain`
   - [ ] Repository implementations in `core.data` (delegate to DAOs)
   - [ ] Kotlin Flows for reactive queries

5. ✅ **Testing:**
   - [ ] Unit tests for all DAOs (in-memory database)
   - [ ] Repository tests
   - [ ] Migration tests (version 1 → future versions)
   - [ ] **Coverage:** 90%+ (TDD enforced)

6. ✅ **Documentation:**
   - [ ] KDoc comments on all entities, DAOs, repositories
   - [ ] Database schema diagram (Mermaid)
   - [ ] Migration strategy documented

### Nice to Have (P1)

- [ ] Database inspector support (debug builds)
- [ ] Export/import functionality (JSON)
- [ ] Database encryption (SQLCipher, Phase 1.2)
- [ ] Performance benchmarks (insert/query latency)

### Out of Scope

- ❌ **ContentProvider for VOS4** (Phase 4)
- ❌ **Supabase cloud sync** (Phase 6)
- ❌ **Full-text search** (Phase 2 with RAG)
- ❌ **Multi-user support** (Phase 6 enterprise)

---

## Success Metrics

1. **Performance:**
   - Insert 1000 messages: <500ms
   - Query conversation with 100 messages: <100ms
   - Database size with 1000 conversations: <50MB

2. **Quality:**
   - 90%+ test coverage (DAOs + repositories)
   - Zero memory leaks (LeakCanary validation)
   - All tests pass on CI

3. **Usability:**
   - Simple API for features to use
   - Clear error messages for constraint violations
   - Type-safe queries (no string SQL in business logic)

---

## Constraints

### Technical Constraints

1. **Room Version:** 2.6.1 (from libs.versions.toml)
2. **Min SDK:** Android API 24 (Android 7.0)
3. **Kotlin Coroutines:** All DAOs use suspend functions
4. **No Blocking Calls:** All database operations async

### Business Constraints

1. **Timeline:** Week 3-4 (2 weeks)
2. **Privacy:** All data local-only (no cloud in Phase 1.0)
3. **AOSP Compatible:** No Google Play Services dependencies

### Performance Constraints

1. **Memory:** Database operations <100MB peak
2. **Latency:** Query operations <100ms (95th percentile)
3. **Storage:** Efficient schema (no data duplication)

---

## Assumptions

1. **Single User:** No multi-user support needed (Phase 1.0)
2. **Single Device:** No cross-device sync needed (Phase 6)
3. **No Migration Data:** Fresh install, no existing database to migrate
4. **English First:** Localization later, schema supports it via `locale` columns
5. **Room Stability:** Room 2.6.1 is stable and production-ready

---

## Database Schema Design

### Table 1: conversations

**Purpose:** Store chat sessions

```kotlin
@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["updated_at"])
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String,              // UUID
    val title: String,                       // Generated from first message
    val created_at: Long,                    // Timestamp millis
    val updated_at: Long,                    // Last message timestamp
    val message_count: Int = 0,              // Cached count
    val is_archived: Boolean = false,
    val metadata: String? = null             // JSON for extensibility
)
```

**VOS4 Pattern:** Simple UUID primary key, timestamp indices for sorting.

### Table 2: messages

**Purpose:** Individual messages within conversations

```kotlin
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversation_id", "timestamp"]),  // Composite for pagination
        Index(value = ["role"])                           // Filter by user/assistant
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE                 // VOS4 pattern: cascade delete
        )
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,              // UUID
    val conversation_id: String,             // FK to conversations
    val role: String,                        // "user" | "assistant" | "system"
    val content: String,                     // Message text
    val timestamp: Long,                     // When sent
    val intent: String? = null,              // Classified intent (from NLU)
    val confidence: Float? = null,           // NLU confidence
    val metadata: String? = null             // JSON (model used, tokens, etc.)
)
```

**VOS4 Pattern:** Composite index (conversation_id, timestamp) for efficient pagination.

### Table 3: train_examples

**Purpose:** Teach-Ava user training data

```kotlin
@Entity(
    tableName = "train_examples",
    indices = [
        Index(value = ["intent"]),
        Index(value = ["created_at"]),
        Index(value = ["example_hash"], unique = true)   // VOS4 pattern: hash-based dedup
    ]
)
data class TrainExampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val example_hash: String,                // MD5(utterance + intent) for uniqueness
    val utterance: String,                   // User's phrase
    val intent: String,                      // Mapped intent
    val locale: String = "en-US",            // Language
    val source: String,                      // "manual" | "auto_learn" | "correction"
    val created_at: Long,                    // When added
    val usage_count: Int = 0,                // How many times matched
    val last_used: Long? = null              // Last match timestamp
)
```

**VOS4 Pattern:** Hash-based unique constraint prevents duplicate training examples.

### Table 4: decisions

**Purpose:** Store decision history for learning

```kotlin
@Entity(
    tableName = "decisions",
    indices = [
        Index(value = ["decision_type"]),
        Index(value = ["timestamp"])
    ]
)
data class DecisionEntity(
    @PrimaryKey val id: String,              // UUID
    val decision_type: String,               // "intent_classification" | "response_generation" | etc.
    val input: String,                       // What was the input?
    val output: String,                      // What did AVA decide?
    val reasoning: String? = null,           // Why? (Constitutional AI, Phase 3)
    val confidence: Float,                   // Confidence score
    val timestamp: Long,
    val was_correct: Boolean? = null,        // User feedback
    val feedback: String? = null             // User correction
)
```

**Purpose:** Learn from decisions, improve over time.

### Table 5: learning

**Purpose:** Learned patterns from interactions

```kotlin
@Entity(
    tableName = "learning",
    indices = [
        Index(value = ["pattern_type"]),
        Index(value = ["confidence"])
    ]
)
data class LearningEntity(
    @PrimaryKey val id: String,              // UUID
    val pattern_type: String,                // "frequent_intent" | "user_preference" | "workflow"
    val pattern_data: String,                // JSON representation of learned pattern
    val confidence: Float,                   // How confident (0.0-1.0)
    val evidence_count: Int,                 // How many observations support this
    val created_at: Long,
    val updated_at: Long,
    val is_active: Boolean = true            // Can be disabled
)
```

**Purpose:** Semantic memory - learned facts about user's behavior.

### Table 6: memory

**Purpose:** Cognitive memory entries (Working/Episodic/Semantic/Procedural)

```kotlin
@Entity(
    tableName = "memory",
    indices = [
        Index(value = ["memory_type"]),
        Index(value = ["importance"]),
        Index(value = ["accessed_at"])        // LRU eviction
    ]
)
data class MemoryEntity(
    @PrimaryKey val id: String,              // UUID
    val memory_type: String,                 // "working" | "episodic" | "semantic" | "procedural"
    val content: String,                     // Memory content (text or JSON)
    val importance: Float,                   // 0.0-1.0 (for consolidation)
    val emotional_valence: Float? = null,    // -1.0 to 1.0 (negative to positive)
    val created_at: Long,
    val accessed_at: Long,                   // Last access (for LRU)
    val access_count: Int = 0,
    val is_consolidated: Boolean = false     // Moved to long-term? (Phase 1.2)
)
```

**Purpose:** All 4 types of cognitive memory in one table (differentiated by type).

---

## Database Relationships

```
conversations (1) ──────< messages (N)
                         (CASCADE DELETE)

train_examples (independent)
decisions (independent)
learning (independent)
memory (independent)
```

**Rationale:** Simple relationships in Phase 1.0, more complex in later phases.

---

## Dependencies

### Module Dependencies

```
core.data (this feature)
    ↓ depends on
core.domain (repository interfaces)
    ↓ depends on
Nothing (pure Kotlin)
```

### Library Dependencies

- **Room:** `androidx.room:room-runtime:2.6.1`
- **Room KTX:** `androidx.room:room-ktx:2.6.1` (coroutines support)
- **Room Compiler:** `androidx.room:room-compiler:2.6.1` (KSP)

### External Dependencies

- **VOS4 Research:** `.ideacode/VOS4_INTEGRATION_REQUIREMENTS.md` (patterns)
- **Localization Research:** VOS4 localization analysis (42 languages)

---

## Implementation Phases

### Phase 1: Schema & Entities (Day 1)
- Create 6 entity classes
- Add indices, foreign keys
- Create type converters

**Tests:** None yet (entities are data classes)

### Phase 2: DAOs (Day 2)
- Create 6 DAO interfaces
- Implement CRUD + custom queries
- Use suspend functions

**Tests:** DAO unit tests (in-memory database)

### Phase 3: Database Class (Day 3)
- Create AVADatabase class
- Wire all DAOs
- Implement singleton pattern

**Tests:** Database initialization tests

### Phase 4: Repositories (Day 4-5)
- Create repository interfaces (domain)
- Create repository implementations (data)
- Use Kotlin Flows for reactive queries

**Tests:** Repository tests (mock DAOs)

### Phase 5: Migration & Documentation (Day 6)
- Create migration tests
- Document schema with Mermaid diagram
- KDoc all public APIs

**Tests:** Migration tests

### Phase 6: Integration & Testing (Day 7)
- Integration tests (full database flow)
- Performance benchmarks
- Code review & polish

**Tests:** Integration + benchmark tests

---

## Risk Assessment

### High Risk

1. **Room Version Compatibility:** Mitigation: Use stable 2.6.1
2. **Test Coverage:** Mitigation: TDD from Day 1 (write tests BEFORE implementation)

### Medium Risk

1. **Performance:** Mitigation: Benchmark queries, add indices as needed
2. **Schema Changes:** Mitigation: Plan for migrations from Day 1

### Low Risk

1. **Kotlin Coroutines:** Well-understood, Room has great support
2. **Cascade Deletes:** VOS4 proven pattern

---

## Rollback Plan

If critical issues found:
1. **Revert commit:** Git has clean history
2. **Disable database:** Use in-memory stub (testing only)
3. **Fix forward:** Database is isolated in `core.data`, can fix without affecting other modules

---

## References

- **Constitution:** `.ideacode/memory/principles.md` (v1.2.2, Section: Technical Stack)
- **VOS4 Database Patterns:** VOS4 research document (30+ entities, 6 databases)
- **VOS4 Localization:** 42 languages, 3-tier caching, composite indices
- **Clean Architecture:** Dependencies flow inward (domain ← data)
- **Phase 1.0 Spec:** `.ideacode/PHASE_1_0_SPECIFICATION.md` (FR-002: Room Database)
- **Project Config:** `.ideacode-v2/config.yml` (android, hybrid-tdd)

---

## Approval

- [ ] **Reviewed by:** User
- [ ] **Approved for Planning:** Yes/No
- [ ] **Estimated Time:** 2 weeks (Week 3-4)

**Next Step:** Create implementation plan (`/idea.plan`)

---

**Version:** 1.0.0
**Status:** Awaiting Approval
**Last Updated:** 2025-10-28 14:01
