# VoiceOS Production-Ready Restoration Plan

**Created:** 2025-11-27
**Status:** Ready for Swarm Execution
**Current State:** Compiling (60% functional)
**Target State:** Production-Ready (100% functional)
**Estimated Effort:** 22-32 hours (MVP) | 40-60 hours (Production)
**Swarm Mode:** REQUIRED (6 specialist agents)

---

## Executive Summary

Complete the VoiceOS restoration from YOLO migration to production-ready state. Current build compiles successfully but has ~40% features disabled (LearnApp, Scraping, VoiceCommandProcessor). This plan restores all functionality using SQLDelight database migration and parallel swarm execution.

**What's Done:**
- ✅ App compiles and builds APK
- ✅ Core VoiceOSService structure intact
- ✅ CommandManager functional
- ✅ 13 Handler classes operational
- ✅ SQLDelight infrastructure ready

**What's Missing:**
- ⚠️ LearnApp automatic app learning (~60 files)
- ⚠️ Scraping infrastructure (~30 files)
- ⚠️ VoiceCommandProcessor integration
- ⚠️ Test suite (27 tests need migration)

---

## Phase 1: LearnApp SQLDelight Migration (8-10 hours)

**Agent:** LearnApp Migration Specialist
**Priority:** HIGH (blocking for app learning features)
**Dependencies:** SQLDelight core infrastructure (ready)

### Tasks

#### 1.1: Database Adapter Layer (2-3 hours)
- Create `LearnAppDatabaseAdapter.kt` - Room-compatible API using SQLDelight
- Implement `LearnAppDao` interface with SQLDelight queries
- Create DTOs: `LearnedAppDTO`, `ExplorationSessionDTO`, `ScreenStateDTO`, `NavigationEdgeDTO`
- Add SQL schema files for LearnApp tables

**Files to Create:**
```
modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/database/
├── LearnAppDatabaseAdapter.kt          (NEW)
├── LearnAppDaoAdapter.kt               (NEW)
└── dao/
    └── LearnAppDao.kt                  (RESTORE + MIGRATE)
```

#### 1.2: Repository Layer (2-3 hours)
- Restore `LearnAppRepository.kt` (currently .disabled)
- Replace all Room `@Transaction` with SQLDelight transactions
- Update 18+ database calls to use new repository pattern
- Implement caching layer for performance

**Files to Restore + Migrate:**
```
modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/database/repository/
└── LearnAppRepository.kt               (RESTORE from .disabled, MIGRATE)
```

#### 1.3: Integration Layer (2-3 hours)
- Restore `LearnAppIntegration.kt` (currently .disabled)
- Restore `ExplorationEngine.kt` (currently .disabled)
- Update all database references to use adapters
- Remove Room imports, add SQLDelight

**Files to Restore + Migrate:**
```
modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/
├── integration/LearnAppIntegration.kt  (RESTORE, MIGRATE)
└── exploration/ExplorationEngine.kt    (RESTORE, MIGRATE)
```

#### 1.4: Supporting Classes (1-2 hours)
- Restore 10+ supporting classes from .disabled state
- Update imports and database references
- Ensure compilation

**Files to Restore:**
```
learnapp/detection/AppLaunchDetector.kt
learnapp/detection/ExpandableControlDetector.kt
learnapp/detection/LauncherDetector.kt
learnapp/elements/ElementClassifier.kt
learnapp/fingerprinting/ScreenFingerprinter.kt
learnapp/navigation/NavigationGraph.kt
learnapp/ui/ConsentDialog.kt
+ 5 more files
```

### Quality Gates
- [ ] All 15+ LearnApp files compile
- [ ] Database adapter tests pass (create 5 tests)
- [ ] Integration with VoiceOSService successful
- [ ] No Room dependencies remain
- [ ] App learning workflow functional (manual test)

---

## Phase 2: Scraping Infrastructure Migration (6-8 hours)

**Agent:** Scraping Migration Specialist
**Priority:** HIGH (blocking for voice command execution)
**Dependencies:** SQLDelight core infrastructure (ready)

### Tasks

#### 2.1: Entity Layer (1-2 hours)
- Restore `ScrapedElementEntity.kt` (replace stub with full implementation)
- Create `ScrapedAppEntity.kt`, `ScrapedHierarchyEntity.kt`
- Create `GeneratedCommandEntity.kt`, `ScreenContextEntity.kt`
- Add 5+ entity DTOs

**Files to Create/Update:**
```
modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/entities/
├── ScrapedElementEntity.kt             (UPDATE stub → full)
├── ScrapedAppEntity.kt                 (NEW)
├── ScrapedHierarchyEntity.kt           (NEW)
├── GeneratedCommandEntity.kt           (NEW)
└── ScreenContextEntity.kt              (NEW)
```

#### 2.2: DAO Layer (2-3 hours)
- Create SQLDelight DAO implementations
- Add queries for element lookup by hash
- Add queries for command generation
- Implement usage tracking

**Files to Create:**
```
modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/dao/
├── ScrapedElementDaoImpl.kt            (NEW)
├── GeneratedCommandDaoImpl.kt          (NEW)
└── ScreenContextDaoImpl.kt             (NEW)
```

#### 2.3: Core Integration (2-3 hours)
- Restore `AccessibilityScrapingIntegration.kt` (currently .disabled)
- Restore `VoiceCommandProcessor.kt` (currently .disabled)
- Restore `CommandGenerator.kt` (currently .disabled)
- Update all database calls to SQLDelight

**Files to Restore + Migrate:**
```
modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/
├── AccessibilityScrapingIntegration.kt (RESTORE, MIGRATE)
├── VoiceCommandProcessor.kt            (RESTORE, MIGRATE)
└── CommandGenerator.kt                 (RESTORE, MIGRATE)
```

#### 2.4: Supporting Infrastructure (1 hour)
- Restore helper classes
- Update imports
- Ensure compilation

**Files to Restore:**
```
scraping/ElementHasher.kt              (RESTORE)
scraping/AppHashCalculator.kt          (RESTORE)
scraping/ScreenContextInferenceHelper.kt (RESTORE)
scraping/SemanticInferenceHelper.kt    (RESTORE)
```

### Quality Gates
- [ ] All 9 core scraping files compile
- [ ] DAO implementation tests pass (create 8 tests)
- [ ] VoiceCommandProcessor integration successful
- [ ] Command matching functional (manual test)
- [ ] No Room dependencies remain

---

## Phase 3: VoiceOSService Integration (3-4 hours)

**Agent:** Service Integration Specialist
**Priority:** HIGH (connects all components)
**Dependencies:** Phase 1 & 2 complete

### Tasks

#### 3.1: Uncomment Integration Points (1 hour)
- Restore `scrapingIntegration` variable and initialization
- Restore `learnAppIntegration` variable and initialization
- Restore `voiceCommandProcessor` variable and initialization
- Remove all `// Disabled` comments (~30 lines)

**Files to Update:**
```
modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/
└── VoiceOSService.kt                   (UPDATE - uncomment ~30 lines)
```

**Lines to Restore:**
- Line 215: `learnAppIntegration` variable
- Line 222: `scrapingIntegration` variable
- Line 225: `voiceCommandProcessor` variable
- Lines 585-606: Integration initialization
- Lines 665-688: Event forwarding
- Lines 918-936: LearnApp initialization
- Lines 1233-1245: Command execution (Tier 2)
- Lines 1332-1349: Command execution fallback
- Lines 1452-1465: Cleanup

#### 3.2: Update Initialization Flow (1 hour)
- Verify database initialization order
- Test integration initialization
- Add error handling for migration failures
- Update logging

#### 3.3: Test Integration (1-2 hours)
- Manual test: App launch detection
- Manual test: Element scraping
- Manual test: Voice command execution
- Manual test: App learning workflow
- Verify all integrations load correctly

### Quality Gates
- [ ] VoiceOSService compiles with integrations
- [ ] All integrations initialize successfully
- [ ] Event forwarding works (test with accessibility events)
- [ ] No crashes on service start
- [ ] Logging confirms all components active

---

## Phase 4: Test Suite Migration (4-6 hours)

**Agent:** Test Migration Specialist
**Priority:** MEDIUM (quality assurance)
**Dependencies:** Phase 1, 2, 3 complete

### Tasks

#### 4.1: Infrastructure Tests (1 hour)
- `RepositoryTransactionTest.kt` - Verify SQLDelight transactions
- `RepositoryQueryTest.kt` - Verify query implementations
- `DatabaseMigrationTest.kt` - Verify schema migrations
- Create test database factory

**Test Count:** 19 tests (7 + 12)

#### 4.2: Accessibility Tests (1 hour)
- Move 15 files from `src/test/java.disabled/` to `src/test/java/`
- Update imports (most have ZERO database dependencies!)
- Run and verify

**Files to Move:**
```
modules/apps/VoiceOSCore/src/test/java.disabled/ → src/test/java/
├── accessibility/handlers/DragHandlerTest.kt
├── accessibility/handlers/GazeHandlerTest.kt
├── accessibility/handlers/GestureHandlerTest.kt
├── accessibility/overlays/ConfidenceOverlayTest.kt
├── accessibility/tree/AccessibilityTreeProcessorTest.kt
└── + 10 more files
```

**Test Count:** 51 tests (from lifecycle tests)

#### 4.3: Database-Dependent Tests (2-3 hours)
- Rewrite tests that used Room to use SQLDelight
- Update mock database creation
- Update transaction syntax
- Update query assertions

**Files Needing Rewrite:**
```
LearnAppRepositoryTest.kt              (8 tests - REWRITE)
ScrapingDatabaseTest.kt                (12 tests - REWRITE)
CommandGeneratorTest.kt                (7 tests - REWRITE)
```

**Test Count:** 27 tests requiring migration

#### 4.4: Integration Tests (1 hour)
- End-to-end LearnApp workflow test
- End-to-end Scraping workflow test
- VoiceOSService lifecycle test
- Create baseline performance tests

**Test Count:** 4 new integration tests

### Quality Gates
- [ ] All 97 tests pass (19 + 51 + 27)
- [ ] Test coverage ≥90% on migrated code
- [ ] Integration tests cover critical paths
- [ ] Performance baseline established
- [ ] No flaky tests

---

## Phase 5: Production Hardening (4-6 hours)

**Agent:** Production Hardening Specialist
**Priority:** HIGH (production readiness)
**Dependencies:** Phase 1-4 complete

### Tasks

#### 5.1: Error Handling (1-2 hours)
- Add try-catch to all database operations
- Add fallback behaviors for migration failures
- Add telemetry for error tracking
- Test error scenarios

#### 5.2: Performance Optimization (1-2 hours)
- Add database query caching
- Optimize transaction batching
- Add indices to SQLDelight schema
- Profile and optimize hot paths
- Target: <100ms for 95th percentile queries

#### 5.3: Migration Safety (1 hour)
- Add database version checks
- Add data migration scripts (Room → SQLDelight)
- Add rollback capability
- Test migration on production database snapshot

#### 5.4: Documentation (1 hour)
- Update architecture docs
- Document database schema
- Create migration guide
- Update README with new features

### Quality Gates
- [ ] All error scenarios handled gracefully
- [ ] Performance targets met (<100ms p95)
- [ ] Migration tested on production data
- [ ] Documentation complete
- [ ] Code review passed

---

## Phase 6: Final Validation (2-3 hours)

**Agent:** Integration Testing & Orchestration
**Priority:** CRITICAL (release gate)
**Dependencies:** All phases complete

### Tasks

#### 6.1: Full System Test (1 hour)
- Clean install on test device
- Test all voice commands
- Test app learning workflow
- Test scraping on 5 popular apps
- Verify no crashes over 30-minute session

#### 6.2: Performance Validation (30 min)
- Run performance test suite
- Verify database query performance
- Check memory usage (target: <100MB)
- Check battery impact

#### 6.3: Regression Testing (30 min)
- Run full test suite (all 97 tests)
- Verify no new bugs introduced
- Check lint/warnings (target: 0)

#### 6.4: Release Preparation (30 min)
- Create changelog
- Tag release candidate
- Create APK for distribution
- Update version numbers

### Quality Gates
- [ ] All tests pass (97/97)
- [ ] No crashes in 30-min session
- [ ] Performance targets met
- [ ] Zero critical/high bugs
- [ ] APK size acceptable (<50MB)
- [ ] Release notes complete

---

## Swarm Execution Strategy

### Agent Assignments

**Agent 1: LearnApp Migration Specialist**
- Responsibility: Phase 1 (LearnApp SQLDelight migration)
- Duration: 8-10 hours
- Deliverables: 15+ files migrated, 5 tests passing
- Dependencies: SQLDelight core (ready)

**Agent 2: Scraping Migration Specialist**
- Responsibility: Phase 2 (Scraping SQLDelight migration)
- Duration: 6-8 hours
- Deliverables: 9 core files migrated, 8 tests passing
- Dependencies: SQLDelight core (ready)

**Agent 3: Service Integration Specialist**
- Responsibility: Phase 3 (VoiceOSService integration)
- Duration: 3-4 hours
- Deliverables: All integrations restored, service functional
- Dependencies: Agent 1 & 2 complete

**Agent 4: Test Migration Specialist**
- Responsibility: Phase 4 (Test suite migration)
- Duration: 4-6 hours
- Deliverables: 97 tests passing
- Dependencies: Agent 1, 2, 3 complete

**Agent 5: Production Hardening Specialist**
- Responsibility: Phase 5 (Error handling, performance)
- Duration: 4-6 hours
- Deliverables: Production-ready code
- Dependencies: Agent 1-4 complete

**Agent 6: Orchestrator & Integration Testing**
- Responsibility: Phase 6 (Coordination, final validation)
- Duration: 2-3 hours + ongoing coordination
- Deliverables: Release-ready APK
- Dependencies: All agents complete

### Parallelization Plan

```
Timeline (Optimized with Swarm):

Hour 0-10:  [Agent 1: LearnApp] ┃ [Agent 2: Scraping]
            ↓                     ↓
Hour 10-14: [Agent 3: Service Integration]
            ↓
Hour 14-20: [Agent 4: Tests] ┃ [Agent 5: Hardening]
            ↓                 ↓
Hour 20-23: [Agent 6: Final Validation]

Total: ~23 hours (vs 40+ hours sequential)
Time Savings: 43%
```

### Coordination Points

**Checkpoint 1 (Hour 10):** Agents 1 & 2 complete
- Verify: All files compile
- Verify: Integration interfaces compatible
- Decision: Proceed to Agent 3

**Checkpoint 2 (Hour 14):** Agent 3 complete
- Verify: Service integrations working
- Verify: No crashes
- Decision: Proceed to Agents 4 & 5

**Checkpoint 3 (Hour 20):** Agents 4 & 5 complete
- Verify: All tests pass
- Verify: Performance targets met
- Decision: Proceed to Agent 6

**Final Gate (Hour 23):** Agent 6 complete
- Verify: Release criteria met
- Decision: Ship or iterate

---

## Risk Register

### Risk 1: Database Schema Incompatibility
**Probability:** Medium | **Impact:** High
- **Mitigation:** Test migrations on production data snapshots
- **Contingency:** Rollback scripts ready, phased rollout

### Risk 2: Performance Regression
**Probability:** Low | **Impact:** High
- **Mitigation:** Continuous performance testing during migration
- **Contingency:** Query optimization, caching layer

### Risk 3: Integration Failures
**Probability:** Medium | **Impact:** Medium
- **Mitigation:** Incremental integration with tests at each step
- **Contingency:** Feature flags to disable problematic integrations

### Risk 4: Test Migration Complexity
**Probability:** Low | **Impact:** Low
- **Mitigation:** Most tests have zero DB dependencies (easy)
- **Contingency:** Defer complex tests to post-MVP

---

## Success Criteria

### MVP (Minimum Viable Product)
- [ ] App compiles and runs
- [ ] LearnApp workflow functional
- [ ] Scraping infrastructure operational
- [ ] Voice command execution works
- [ ] No crashes in basic usage
- [ ] 70+ tests passing

### Production-Ready
- [ ] All 97 tests pass
- [ ] Performance targets met
- [ ] Error handling complete
- [ ] Documentation updated
- [ ] Code review passed
- [ ] 30-minute crash-free session

### Full Feature Parity
- [ ] 100% features from pre-YOLO state
- [ ] All integrations active
- [ ] Migration scripts tested
- [ ] Production deployment successful

---

## Rollback Plan

If critical issues discovered:

1. **Immediate:** Disable problematic feature via flag
2. **Short-term:** Revert to commit `476384f4` (pre-restoration)
3. **Long-term:** Fix issues, re-deploy with fixes

**Rollback Triggers:**
- Crash rate >1%
- Performance regression >20%
- Data corruption detected
- Critical bug in production

---

## Next Steps

1. ✅ Review this plan
2. ⚡ Execute: `/swarm restore` → Deploy 6 agents
3. 🎯 Monitor: Agent checkpoints at Hours 10, 14, 20
4. 🚀 Validate: Final testing at Hour 23
5. 📦 Ship: Release candidate ready

---

**Estimated Completion:** 23 hours (swarm mode)
**Timeline:** 3 working days (distributed)
**Confidence:** HIGH (detailed breakdown, proven architecture)
**Status:** READY FOR EXECUTION
