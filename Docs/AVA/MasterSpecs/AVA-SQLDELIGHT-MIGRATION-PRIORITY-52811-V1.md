# AVA SQLDelight Migration - Priority Plan

**Created:** 2025-11-28
**Status:** Prioritization Analysis
**Purpose:** Determine if AVA should migrate to SQLDelight before implementing Q2 (MagicUI/MagicCode)
**Decision Required:** YES/NO to migrate before Q2

---

## Executive Summary

**RECOMMENDATION: MIGRATE AVA TO SQLDELIGHT BEFORE Q2** ✅

**Why:**
1. Q2 (MagicUI/MagicCode) will generate SQLDelight code for new apps
2. Having mixed database technologies (Room in AVA, SQLDelight in generated apps) creates complexity
3. Code generation system should use the same patterns as the host application
4. Migration is well-planned and already completed in other repos (VoiceOS, AvaConnect)
5. Minimal disruption: 3-5 days estimated, well-documented migration path

---

## Decision Matrix

| Factor | Migrate First | Migrate Later | Weight | Score |
|--------|---------------|---------------|--------|-------|
| **Technical Consistency** | ✅ Unified tech stack | ❌ Mixed Room/SQLDelight | HIGH | +3 |
| **Q2 Implementation Complexity** | ✅ Generate same tech as AVA | ❌ Generate different tech | HIGH | +3 |
| **Code Reusability** | ✅ Share patterns between AVA/generated | ❌ Maintain two patterns | MEDIUM | +2 |
| **Cross-platform Support** | ✅ AVA can run on iOS/Desktop | ❌ AVA stays Android-only | HIGH | +3 |
| **Migration Risk** | ⚠️ 3-5 days upfront cost | ✅ No immediate disruption | MEDIUM | -1 |
| **Documentation** | ✅ Well-planned migration | ✅ Already documented | LOW | 0 |
| **Team Knowledge** | ✅ VoiceOS/AvaConnect already migrated | ❌ Split knowledge | MEDIUM | +2 |
| **Future Maintenance** | ✅ One database tech | ❌ Two database techs | HIGH | +3 |

**Total Score: +15** (Strong recommendation to migrate first)

---

## Current State Analysis

### AVA Database Usage

#### Core/Data Module
**Location:** `Universal/AVA/Core/Data/`

**Entities (Room):**
```
ConversationEntity          - Chat conversations
MessageEntity              - Chat messages
IntentExampleEntity        - Training examples
IntentEmbeddingEntity      - Cached embeddings (CRITICAL for performance)
SemanticIntentOntologyEntity - Intent ontology
TrainExampleEntity         - Learning data
DecisionEntity             - Decision logs
DecisionActionEntity       - Action tracking
EmbeddingMetadata          - Embedding metadata
```

**Database Size:** ~50-200 MB per user (primarily embeddings)

**Critical Features:**
- ✅ Intent embedding cache (95% faster initialization - MUST PRESERVE)
- ✅ Conversation history
- ✅ Training data
- ✅ Decision logging

#### Features/RAG Module
**Location:** `Universal/AVA/Features/RAG/src/androidMain/kotlin/.../room/`

**Entities (Room):**
```
DocumentEntity             - Ingested documents
ChunkEntity               - Document chunks
ChunkEmbeddingEntity      - Chunk embeddings (LARGE - quantized INT8)
DocumentMetadata          - Document metadata
```

**Database Size:** ~100 MB - 10 GB (depends on ingested content)

**Critical Features:**
- ✅ Quantized embedding storage (75% space savings)
- ✅ Full-text search (FTS4)
- ✅ Semantic search
- ✅ Hybrid search (BM25 + Vector)

---

## Migration Impact Analysis

### 1. Q2 Implementation Impact

#### Without Migration (Room in AVA, SQLDelight in Generated Apps)

**Complexity:**
```kotlin
// AVA uses Room
@Entity(tableName = "tasks")
data class TaskEntity(...)

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)
}

// Generated apps use SQLDelight
// File: Task.sq
CREATE TABLE task (...);
INSERT OR REPLACE INTO task VALUES (...);

// Result: Developers must know TWO patterns
// Result: Cannot reuse AVA patterns in generated code
// Result: Code generator must understand BOTH technologies
```

**Code Generator Complexity:**
- ❌ Must maintain two code generation paths
- ❌ Cannot learn from AVA's database patterns
- ❌ Cannot use AVA's database utilities in generated apps
- ❌ Testing requires both Room and SQLDelight knowledge

**Documentation Burden:**
- ❌ "AVA uses Room but generates SQLDelight apps"
- ❌ Confusing for developers
- ❌ Higher learning curve

#### With Migration (SQLDelight Everywhere)

**Simplicity:**
```kotlin
// AVA uses SQLDelight
-- File: Task.sq
CREATE TABLE task (...);
INSERT OR REPLACE INTO task VALUES (...);

// Generated apps use SQLDelight
-- File: Task.sq (SAME PATTERN)
CREATE TABLE task (...);
INSERT OR REPLACE INTO task VALUES (...);

// Result: ONE pattern to learn
// Result: Reuse AVA patterns in generated code
// Result: Code generator is simpler
```

**Code Generator Simplicity:**
- ✅ Single code generation path
- ✅ Learn from AVA's database patterns
- ✅ Reuse AVA's database utilities
- ✅ Testing uses one technology

**Documentation Clarity:**
- ✅ "Everything uses SQLDelight"
- ✅ Clear and consistent
- ✅ Lower learning curve

### 2. Cross-Platform Benefits

**After Migration:**
- ✅ AVA can run on iOS
- ✅ AVA can run on Desktop (macOS, Windows, Linux)
- ✅ AVA can run on Web (WASM - experimental)
- ✅ Generated apps automatically support all platforms

**Use Cases Unlocked:**
- AVA on iOS for iPhone/iPad users
- AVA on macOS as desktop assistant
- AVA on Windows for enterprise deployments
- Unified cross-platform app generation

### 3. Performance Considerations

**Critical Optimization to Preserve:**

From `docs/Developer-Manual-Chapter52-RAG-System-Architecture.md`:
```
Phase 4 Performance Optimizations:
- Intent embedding cache: 95% faster initialization (4.2s → 0.2s)
- Query result caching: 30-50% faster searches
- Quantized embeddings: 75% storage reduction
```

**SQLDelight Compatibility:**
- ✅ Can store quantized ByteArray embeddings
- ✅ Can use BLOB for binary data
- ✅ Can implement caching queries
- ✅ Can use FTS (Full-Text Search) extensions

**Migration Risk:** LOW - All performance features transferable

---

## Migration Effort Estimate

### Based on VoiceOS Completion (ADR-010)

**VoiceOS Migration Stats:**
- 30 tables migrated
- INSERT OR REPLACE pattern applied
- 3 days actual completion time
- Zero data loss
- Zero feature regressions

**AVA Migration Estimate:**

| Module | Tables | Complexity | Est. Time |
|--------|--------|------------|-----------|
| Core/Data | 9 entities | Medium | 1.5 days |
| Features/RAG | 4 entities | High (quantization, FTS) | 2 days |
| Testing | All modules | Medium | 1 day |
| Documentation | All modules | Low | 0.5 days |
| **TOTAL** | **13 entities** | **Medium-High** | **5 days** |

**Confidence:** HIGH (based on VoiceOS success)

---

## Migration Timeline

### Week 1: Core/Data Module (2 days)

**Day 1:**
- [ ] Setup SQLDelight plugin and dependencies
- [ ] Create .sq schema files for all 9 entities
- [ ] Generate Kotlin code
- [ ] Create platform drivers (Android, iOS, Desktop)

**Day 2:**
- [ ] Migrate repositories to use SQLDelight queries
- [ ] Migrate type converters (Map, FloatList → BLOB)
- [ ] Update dependency injection (Hilt)
- [ ] Run unit tests

### Week 1: Features/RAG Module (2 days)

**Day 3:**
- [ ] Create .sq schema files for 4 entities
- [ ] Implement quantized embedding storage (ByteArray)
- [ ] Implement FTS (Full-Text Search) tables
- [ ] Generate Kotlin code

**Day 4:**
- [ ] Migrate RAGRepository to SQLDelight
- [ ] Preserve caching optimizations
- [ ] Preserve quantization logic
- [ ] Update embeddings storage/retrieval

### Week 2: Testing & Documentation (1 day)

**Day 5:**
- [ ] Run full test suite
- [ ] Verify performance metrics (95% cache hit rate)
- [ ] Update documentation
- [ ] Create migration guide for users

---

## Risk Mitigation

### Risk 1: Data Loss During Migration

**Mitigation:**
- ✅ Create database backup before migration
- ✅ Write migration script to export Room → SQLDelight
- ✅ Verify data integrity with checksums
- ✅ Test on development database first

### Risk 2: Performance Regression

**Mitigation:**
- ✅ Benchmark before migration (record baseline)
- ✅ Benchmark after migration (verify no regression)
- ✅ Monitor intent cache hit rate (must stay 95%+)
- ✅ Monitor RAG search latency (must stay <100ms)

### Risk 3: Breaking Changes for Users

**Mitigation:**
- ✅ Implement automatic migration on app update
- ✅ Preserve user data (conversations, documents)
- ✅ Provide rollback mechanism (export data)
- ✅ Test on beta channel first

### Risk 4: Development Velocity Slowdown

**Mitigation:**
- ✅ Feature freeze during migration (5 days)
- ✅ Use separate branch (`feature/sqldelight-migration`)
- ✅ Merge only after all tests pass
- ✅ VoiceOS team available for consultation

---

## Success Criteria

### Must-Have (Blockers)

- [ ] All 13 entities migrated to SQLDelight
- [ ] Zero data loss (100% user data preserved)
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Intent cache hit rate ≥ 95%
- [ ] RAG search latency ≤ 100ms
- [ ] Zero performance regressions

### Nice-to-Have (Enhancements)

- [ ] iOS build works
- [ ] Desktop build works
- [ ] Cross-platform tests pass
- [ ] Documentation updated
- [ ] Migration guide published

---

## Recommendation: MIGRATE FIRST

### Pros of Migrating Before Q2

1. ✅ **Technical Consistency** - One database technology everywhere
2. ✅ **Simpler Q2 Implementation** - Generate same tech as AVA
3. ✅ **Code Reusability** - Share patterns between AVA and generated apps
4. ✅ **Cross-Platform AVA** - Unlock iOS/Desktop deployments
5. ✅ **Future-Proof** - SQLDelight is the strategic choice
6. ✅ **Lower Maintenance** - One tech stack to maintain
7. ✅ **Team Knowledge** - Leverage VoiceOS/AvaConnect experience
8. ✅ **Well-Planned** - Migration path already documented

### Cons of Migrating Before Q2

1. ⚠️ **Upfront Cost** - 5 days before Q2 can start
2. ⚠️ **Migration Risk** - Could introduce bugs (mitigated by testing)
3. ⚠️ **Feature Freeze** - No new features during migration

### Pros of Postponing Migration

1. ✅ **No Upfront Cost** - Start Q2 immediately
2. ✅ **No Migration Risk** - Avoid potential bugs

### Cons of Postponing Migration

1. ❌ **Technical Debt** - Mixed technologies
2. ❌ **Higher Complexity** - Maintain two database systems
3. ❌ **Duplicate Code** - Two code generation paths
4. ❌ **Confusing Docs** - "AVA uses Room, generated apps use SQLDelight"
5. ❌ **Limited Reusability** - Cannot share patterns
6. ❌ **Future Migration** - Will need to migrate eventually anyway

---

## Decision

**RECOMMENDATION: MIGRATE AVA TO SQLDELIGHT BEFORE IMPLEMENTING Q2** ✅

**Reasoning:**
- 5-day upfront cost is worth the long-term benefits
- VoiceOS migration success proves low risk
- Technical consistency is critical for Q2
- Cross-platform AVA unlocks major value
- Inevitable migration anyway - better now than later

**Next Steps:**
1. Review this document with stakeholders
2. Get approval for 5-day feature freeze
3. Create migration branch
4. Begin migration (follow timeline above)
5. Verify success criteria
6. Merge to main
7. Begin Q2 implementation with SQLDelight

---

**Decision Authority:** Engineering Lead
**Review Required:** Yes
**Timeline Impact:** +5 days before Q2 starts
**Risk Level:** LOW (mitigated by VoiceOS success)
**Strategic Value:** HIGH (cross-platform, consistency, maintainability)

---

**See Also:**
- `ROOM-TO-SQLDELIGHT-MIGRATION-PLAN.md` - Original migration plan
- `PHASE-1-AVA-CORE-DATA-MIGRATION.md` - Detailed migration steps
- `VoiceOS/docs/planning/architecture/decisions/ADR-010-Room-SQLDelight-Migration-Completion-251128-0349.md` - VoiceOS success story
- `AVA-RAG-ROOM-TO-SQLDELIGHT-MIGRATION-GUIDE.md` - Detailed migration guide (next document)
