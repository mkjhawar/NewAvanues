# Implementation Plan: Room Database with VOS4 Patterns

**Spec ID:** 0001
**Created:** 2025-10-28 14:01
**Estimated Time:** 2 weeks (10 working days, ~80 hours)
**Methodology:** Hybrid TDD (90% coverage for domain/data layers)

---

## Phase Breakdown

### Phase 1: Project Setup & Dependencies (Day 1 - 4 hours)

**Goal:** Configure Gradle, add dependencies, verify build

**Tasks:**
1. Create `core/data/build.gradle.kts` with Room dependencies
2. Create `core/domain/build.gradle.kts` (no dependencies)
3. Configure KSP for Room annotation processing
4. Verify builds successfully

**Tests Required:**
- None (configuration only)

**Success Criteria:**
- [x] Gradle sync successful
- [x] No dependency conflicts
- [x] KSP configured correctly

**Estimated Time:** 4 hours
**Dependencies:** None

---

### Phase 2: Domain Layer - Repository Interfaces (Day 1-2 - 6 hours)

**Goal:** Define repository contracts in pure Kotlin (core.domain)

**Tasks:**
1. Create `ConversationRepository` interface
2. Create `MessageRepository` interface
3. Create `TrainExampleRepository` interface
4. Create `DecisionRepository` interface
5. Create `LearningRepository` interface
6. Create `MemoryRepository` interface
7. Define common result types (Success/Error)

**Tests Required:**
- None yet (interfaces have no logic)
- Will test implementations in Phase 4

**Success Criteria:**
- [x] 6 repository interfaces created
- [x] All methods use suspend functions
- [x] Return types use Kotlin Flow for streams
- [x] KDoc comments on all methods

**Estimated Time:** 6 hours
**Dependencies:** None

**Specialists Needed:** Domain modeling specialist

---

### Phase 3: Data Layer - Entity Classes (Day 2-3 - 8 hours)

**Goal:** Create Room entities with VOS4 patterns

**Tasks:**
1. Create `ConversationEntity` with indices
2. Create `MessageEntity` with composite index + FK
3. Create `TrainExampleEntity` with hash-based unique constraint
4. Create `DecisionEntity`
5. Create `LearningEntity`
6. Create `MemoryEntity` with importance index
7. Create type converters (List→JSON, custom types)
8. **DEFEND:** Write entity tests (validation, JSON conversion)

**Tests Required:**
- ✅ Type converter tests (to/from JSON)
- ✅ Entity validation tests
- ✅ Verify indices are defined correctly

**Success Criteria:**
- [x] All 6 entities created with `@Entity` annotation
- [x] All indices defined per spec
- [x] Foreign key cascade delete configured (messages)
- [x] Type converters tested
- [x] **Coverage:** 90%+ for type converters

**Estimated Time:** 8 hours (4 hours implement + 4 hours tests)
**Dependencies:** None

**Specialists Needed:** Test specialist (Defend phase)

---

### Phase 4: Data Layer - DAO Interfaces (Day 3-4 - 10 hours)

**Goal:** Create DAOs with CRUD + custom queries

**Tasks:**

**4.1 ConversationDao:**
- Insert, update, delete
- Get by ID, get all (Flow)
- Get recent (limit, order by updated_at DESC)
- Search by title

**4.2 MessageDao:**
- Insert, update, delete
- Get by conversation ID (paginated, composite index)
- Get by role (user/assistant)
- Count messages in conversation

**4.3 TrainExampleDao:**
- Insert (with hash check)
- Get by intent
- Get by locale
- Update usage count
- Find duplicate (by hash)

**4.4 DecisionDao:**
- Insert, get by type
- Get recent decisions
- Update with feedback

**4.5 LearningDao:**
- Insert, update confidence
- Get by pattern type
- Get active patterns only

**4.6 MemoryDao:**
- Insert, update access time
- Get by type
- Get by importance (threshold)
- LRU eviction query

**Tests Required:**
- ✅ **DEFEND:** Unit tests for EVERY DAO method
- ✅ Use in-memory Room database
- ✅ Test edge cases (empty results, large datasets)
- ✅ Test cascade deletes (conversation → messages)

**Success Criteria:**
- [x] All 6 DAOs created
- [x] All methods use suspend functions
- [x] Custom queries optimized with indices
- [x] **Coverage:** 95%+ (DAO tests are critical)
- [x] All DAO tests pass

**Estimated Time:** 10 hours (5 hours implement + 5 hours tests)
**Dependencies:** Phase 3 (entities)

**Specialists Needed:**
- Database specialist (SQL optimization)
- Test specialist (comprehensive DAO tests)

---

### Phase 5: Database Class & Singleton (Day 5 - 4 hours)

**Goal:** Wire all DAOs into single database instance

**Tasks:**
1. Create `AVADatabase` abstract class
2. Annotate with `@Database(entities = [...], version = 1)`
3. Define abstract DAO methods
4. Implement singleton with double-checked locking
5. Add database builder configuration
6. **DEFEND:** Test database initialization

**Tests Required:**
- ✅ Database creates successfully
- ✅ Singleton pattern works (same instance)
- ✅ All DAOs accessible
- ✅ Database migration test (version 1)

**Success Criteria:**
- [x] Database class compiles
- [x] All 6 DAOs wired
- [x] Singleton thread-safe
- [x] **Coverage:** 85%+

**Estimated Time:** 4 hours (2 hours implement + 2 hours tests)
**Dependencies:** Phase 4 (DAOs)

---

### Phase 6: Repository Implementations (Day 5-7 - 12 hours)

**Goal:** Implement repository interfaces, delegate to DAOs

**Tasks:**

**6.1 Create Repository Implementations:**
- `RoomConversationRepository`
- `RoomMessageRepository`
- `RoomTrainExampleRepository`
- `RoomDecisionRepository`
- `RoomLearningRepository`
- `RoomMemoryRepository`

**6.2 Features:**
- Convert DAO results to domain models
- Handle errors gracefully (try-catch)
- Use Kotlin Flows for reactive streams
- Apply business logic (e.g., auto-generate conversation titles)

**6.3 DEFEND - Repository Tests:**
- Mock DAOs with MockK
- Test all repository methods
- Test error handling
- Test Flow emissions

**Tests Required:**
- ✅ Mock DAO responses
- ✅ Verify domain model mapping
- ✅ Test error cases (database errors)
- ✅ Test Flow behavior (collect, map, filter)

**Success Criteria:**
- [x] All 6 repositories implemented
- [x] Clean API for features to use
- [x] Error handling robust
- [x] **Coverage:** 90%+
- [x] All repository tests pass

**Estimated Time:** 12 hours (6 hours implement + 6 hours tests)
**Dependencies:** Phase 5 (database), Phase 2 (interfaces)

**Specialists Needed:**
- Repository pattern specialist
- Test specialist (MockK expert)

---

### Phase 7: Migration Strategy (Day 8 - 4 hours)

**Goal:** Prepare for future schema changes

**Tasks:**
1. Document migration process
2. Create migration test helper
3. Test version 1 → version 2 (dummy migration)
4. Document rollback strategy

**Tests Required:**
- ✅ Migration test (version 1 creates all tables)
- ✅ Migration validation (no data loss)

**Success Criteria:**
- [x] Migration tests pass
- [x] Documentation complete
- [x] Ready for future schema changes

**Estimated Time:** 4 hours
**Dependencies:** Phase 5 (database)

---

### Phase 8: Integration Tests (Day 8-9 - 8 hours)

**Goal:** Test full database flow end-to-end

**Tasks:**
1. Create integration test suite
2. Test: Create conversation → Add messages → Query
3. Test: Add train example → Query by intent
4. Test: Cascade delete (conversation → messages)
5. Test: Hash-based deduplication (train examples)
6. **Performance benchmarks:**
   - Insert 1000 messages (<500ms)
   - Query conversation with 100 messages (<100ms)
   - Database size with 1000 conversations (<50MB)

**Tests Required:**
- ✅ End-to-end workflow tests
- ✅ Performance benchmarks
- ✅ Cascade delete validation
- ✅ Concurrent access tests

**Success Criteria:**
- [x] All integration tests pass
- [x] Performance budgets met
- [x] No memory leaks (LeakCanary)
- [x] **Coverage:** Overall 90%+

**Estimated Time:** 8 hours
**Dependencies:** Phase 6 (repositories)

**Specialists Needed:** Integration test specialist

---

### Phase 9: Documentation & Diagrams (Day 9 - 4 hours)

**Goal:** Complete documentation for developers

**Tasks:**
1. Create database schema diagram (Mermaid)
2. Write KDoc for all public APIs
3. Create usage examples
4. Document VOS4 patterns applied
5. Update module READMEs

**Tests Required:**
- None (documentation only)

**Success Criteria:**
- [x] Schema diagram clear and accurate
- [x] All public methods documented
- [x] Examples runnable
- [x] VOS4 patterns explained

**Estimated Time:** 4 hours
**Dependencies:** Phase 8 (all features complete)

---

### Phase 10: Code Review & Polish (Day 10 - 4 hours)

**Goal:** Final review, polish, prepare for merge

**Tasks:**
1. **CODEBASE REVIEW:**
   - Scan for TODOs, empty implementations
   - Check for deprecated patterns
   - Verify consistent naming
   - Assess technical debt
2. Run full test suite
3. Check test coverage report
4. Performance profiling
5. Git cleanup (squash commits if needed)
6. Create PR description

**Tests Required:**
- ✅ All tests pass
- ✅ Coverage ≥90%

**Success Criteria:**
- [x] No critical issues found
- [x] All quality gates pass
- [x] Ready for production use
- [x] Documentation complete

**Estimated Time:** 4 hours
**Dependencies:** Phase 9 (documentation)

**Specialists Needed:** Code review specialist

---

## Summary Timeline

| Phase | Days | Hours | Description |
|-------|------|-------|-------------|
| 1 | 1 | 4h | Setup & Dependencies |
| 2 | 1-2 | 6h | Repository Interfaces (domain) |
| 3 | 2-3 | 8h | Entity Classes + Tests (TDD) |
| 4 | 3-4 | 10h | DAO Interfaces + Tests (TDD) |
| 5 | 5 | 4h | Database Class + Tests |
| 6 | 5-7 | 12h | Repository Implementations + Tests (TDD) |
| 7 | 8 | 4h | Migration Strategy |
| 8 | 8-9 | 8h | Integration Tests |
| 9 | 9 | 4h | Documentation & Diagrams |
| 10 | 10 | 4h | Code Review & Polish |
| **Total** | **10 days** | **68h** | **Week 3-4** |

**Buffer:** 12 hours for unexpected issues (total 80h = 2 weeks)

---

## IDE Loop Integration

Each phase follows: **IMPLEMENT → DEFEND → EVALUATE → CODEBASE REVIEW**

**Defend Phase (MANDATORY):**
- Phases 3, 4, 6: Write tests BEFORE marking complete
- Target: 90%+ coverage for core.data
- Cannot proceed to next phase without tests passing

**Evaluate Phase:**
- Verify acceptance criteria met
- Check performance budgets
- Stakeholder approval (if needed)

**Codebase Review:**
- Scan for issues, TODOs
- Assess options for improvements
- Log technical debt if found

---

## Risk Mitigation

### Risk 1: Room Compilation Errors
**Likelihood:** Medium
**Impact:** High
**Mitigation:** Use stable Room 2.6.1, follow official docs
**Contingency:** If KSP fails, try KAPT (slower but more stable)

### Risk 2: Test Coverage Below 90%
**Likelihood:** Low (TDD enforced)
**Impact:** High (quality gate)
**Mitigation:** Write tests DURING implementation, not after
**Contingency:** Add tests before merging (block PR)

### Risk 3: Performance Issues
**Likelihood:** Low (VOS4 patterns proven)
**Impact:** Medium
**Mitigation:** Benchmark in Phase 8, optimize queries
**Contingency:** Add indices, refactor schema if needed

### Risk 4: Schema Redesign Needed
**Likelihood:** Low (well-specified)
**Impact:** High
**Mitigation:** Review schema in Phase 10 before finalization
**Contingency:** Create migration, update entities

---

## Rollback Plan

**If critical issues during implementation:**

1. **Phase 1-5:** Revert commits, no impact on other modules
2. **Phase 6-8:** Use in-memory stub repositories for testing features
3. **Phase 9-10:** Fix forward, database isolated in core.data

**Rollback triggers:**
- Test coverage <80%
- Performance budgets not met
- Critical bugs found in integration tests
- Cannot resolve within buffer time (12 hours)

---

## Dependencies Summary

**External:**
- Room 2.6.1
- KSP 1.9.21-1.0.15
- Kotlin Coroutines

**Internal:**
- core.domain (repository interfaces)
- core.common (Result types, extensions)

**VOS4 Research:**
- Database patterns document
- Localization patterns document

---

## Approval

- [ ] **Plan Reviewed by:** User
- [ ] **Approved for Implementation:** Yes/No
- [ ] **Start Date:** TBD
- [ ] **Target Completion:** Week 3-4 end (2 weeks)

**Next Step:** Create task breakdown (`/idea.tasks`) or begin implementation (`/idea.implement`)

---

**Version:** 1.0.0
**Status:** Awaiting Approval
**Last Updated:** 2025-10-28 14:01
