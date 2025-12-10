# Tasks: PluginSystem Repository Synchronization

**Feature**: 003-pluginsystem-refactor
**Branch**: `003-pluginsystem-refactor`
**Generated**: 2025-10-26

---

## Overview

**Goal**: Merge VOS4's encrypted permission storage to MagicCode PluginSystem, establish MagicCode as canonical library, and update VOS4 to depend on it.

**User Stories**:
- **US1 (P1)**: Copy Encrypted Storage to MagicCode
- **US2 (P1)**: Verify Repository Equality
- **US3 (P2)**: Update VOS4 Dependencies
- **US4 (P3)**: Documentation Synchronization

**Implementation Strategy**: MVP-first incremental delivery. US1+US2 are MVP (establishes encrypted storage in MagicCode). US3 integrates VOS4. US4 adds documentation polish.

**Total Tasks**: 47 tasks across 7 phases

---

## Phase 1: Setup (3 tasks)

**Objective**: Initialize environment and verify prerequisites

**Duration**: 15 minutes

### Tasks

- [ ] T001 Verify VOS4 is on branch `003-pluginsystem-refactor` via `git branch --show-current`
- [ ] T002 Verify MagicCode repository exists at `/Volumes/M Drive/Coding/magiccode` via `ls`
- [ ] T003 Verify both projects have required dependencies (JUnit 4, AndroidX Security) in respective build.gradle.kts files

**Acceptance**: Environment ready for synchronization work

---

## Phase 2: Foundational - Baseline Verification (6 tasks)

**Objective**: Establish baseline test results before any changes

**Duration**: 30 minutes

**Why Foundational**: Must verify both projects build/test successfully before synchronization to isolate any failures

### Tasks

- [ ] T004 Run VOS4 encryption tests via `./gradlew :modules:libraries:PluginSystem:connectedAndroidTest` (expect 8 passing)
- [ ] T005 [P] Run MagicCode existing tests via `cd /Volumes/M\ Drive/Coding/magiccode && ./gradlew :runtime:plugin-system:test` (expect 282 passing)
- [ ] T006 Document baseline test counts in `/Volumes/M Drive/Coding/vos4/specs/003-pluginsystem-refactor/contracts/baseline-results.md`
- [ ] T007 [P] Quick diff check PluginManager.kt between VOS4 and MagicCode via `diff` command
- [ ] T008 [P] Quick diff check PluginLoader.kt between VOS4 and MagicCode via `diff` command
- [ ] T009 Document diff findings in `contracts/baseline-results.md` (expect minimal cosmetic differences only)

**Acceptance**:
- VOS4: 8/8 encryption tests passing
- MagicCode: 282/282 tests passing
- Diff shows minimal differences
- Baseline documented

**Blocker**: If either test suite fails, STOP and fix before proceeding

---

## Phase 3: User Story 1 (P1) - Copy Encrypted Storage to MagicCode (17 tasks)

**User Story**: As a PluginSystem library maintainer, I want MagicCode to have the same encrypted permission storage as VOS4, so that all consumers benefit from hardware-backed encryption security.

**Independent Test**: Can be fully tested by running MagicCode's 282 existing tests + 8 new encryption tests and verifying all 290 pass.

**Duration**: 1.5 hours

### Tasks

#### Preparation (2 tasks)

- [ ] T010 [US1] Create security directories in MagicCode: `mkdir -p` for commonMain/security, androidMain/security, iosMain/security, jvmMain/security, androidInstrumentedTest/security
- [ ] T011 [US1] Create copy script at `/Volumes/M Drive/Coding/vos4/specs/003-pluginsystem-refactor/contracts/copy-encryption-files.sh` using template from file-mapping.md

#### Copy Common Files (4 tasks - parallelizable)

- [ ] T012 [P] [US1] Copy PermissionStorage.kt (expect) from `vos4/.../commonMain/.../security/PermissionStorage.kt` to `magiccode/.../commonMain/.../security/PermissionStorage.kt`
- [ ] T013 [P] [US1] Copy EncryptionStatus.kt from `vos4/.../commonMain/.../security/EncryptionStatus.kt` to `magiccode/.../commonMain/.../security/EncryptionStatus.kt`
- [ ] T014 [P] [US1] Copy MigrationResult.kt from `vos4/.../commonMain/.../security/MigrationResult.kt` to `magiccode/.../commonMain/.../security/MigrationResult.kt`
- [ ] T015 [P] [US1] Copy Exceptions.kt from `vos4/.../commonMain/.../security/Exceptions.kt` to `magiccode/.../commonMain/.../security/Exceptions.kt`

#### Copy Android Files (3 tasks - parallelizable)

- [ ] T016 [P] [US1] Copy PermissionStorage.kt (actual) from `vos4/.../androidMain/.../security/PermissionStorage.kt` to `magiccode/.../androidMain/.../security/PermissionStorage.kt` (410 lines)
- [ ] T017 [P] [US1] Copy KeyManager.kt from `vos4/.../androidMain/.../security/KeyManager.kt` to `magiccode/.../androidMain/.../security/KeyManager.kt` (202 lines)
- [ ] T018 [P] [US1] Copy EncryptedStorageFactory.kt from `vos4/.../androidMain/.../security/EncryptedStorageFactory.kt` to `magiccode/.../androidMain/.../security/EncryptedStorageFactory.kt` (282 lines)

#### Update PluginLogger (1 task)

- [ ] T019 [US1] Add security() method to PluginLogger in `magiccode/.../commonMain/.../core/PluginLogger.kt` (interface, ConsolePluginLogger impl, PluginLog object)

#### Create Stubs (2 tasks - parallelizable)

- [ ] T020 [P] [US1] Create iOS stub at `magiccode/.../iosMain/.../security/PermissionStorage.kt` (throw UnsupportedOperationException with helpful message)
- [ ] T021 [P] [US1] Create JVM stub at `magiccode/.../jvmMain/.../security/PermissionStorage.kt` (throw UnsupportedOperationException with helpful message)

#### Build Configuration (2 tasks)

- [ ] T022 [US1] Update MagicCode build.gradle.kts at `magiccode/runtime/plugin-system/build.gradle.kts` - add androidx.security:security-crypto:1.1.0-alpha06 to androidMain dependencies
- [ ] T023 [US1] Add androidInstrumentedTest source set to MagicCode build.gradle.kts (if not exists) with test dependencies (androidx.test, coroutines-test)

#### Verification (3 tasks)

- [ ] T024 [US1] Verify MagicCode compiles via `cd /Volumes/M\ Drive/Coding/magiccode && ./gradlew :runtime:plugin-system:compileKotlinAndroid` (expect 0 errors)
- [ ] T025 [US1] Copy test files: PermissionStorageEncryptionTest.kt and PermissionStoragePerformanceTest.kt (if exists) from VOS4 to MagicCode androidInstrumentedTest
- [ ] T026 [US1] Run all 290 MagicCode tests via `./gradlew :runtime:plugin-system:test :runtime:plugin-system:connectedAndroidTest` (expect 282 + 8 = 290 passing)

**US1 Acceptance Criteria** (from spec.md):
- [x] All 7 files copied with correct package structure (`com.augmentalis.magiccode.plugins.security`)
- [x] PluginLogger.kt updated with security() method
- [x] iOS/JVM stubs created with helpful error messages
- [x] build.gradle.kts updated with correct dependency
- [x] MagicCode compiles successfully (0 errors)
- [x] All 290 tests pass (282 existing + 8 new)

**US1 Independent Test**: Run `./gradlew check` in MagicCode - all 290 tests pass, no regressions

---

## Phase 4: User Story 2 (P1) - Verify Repository Equality (6 tasks)

**User Story**: As a developer maintaining two PluginSystem repositories, I want to verify VOS4 and MagicCode are functionally equivalent, so that I can confidently switch VOS4 to depend on MagicCode.

**Independent Test**: Can be fully tested by running a diff comparison script and verifying all core files match (excluding platform-specific differences).

**Duration**: 30 minutes

**Dependency**: Requires US1 complete (encryption files copied to MagicCode)

### Tasks

- [ ] T027 [P] [US2] Create diff script at `/Volumes/M Drive/Coding/vos4/specs/003-pluginsystem-refactor/contracts/verify-equality.sh` to compare core API files
- [ ] T028 [P] [US2] Compare PluginManager.kt between VOS4 and MagicCode via diff script
- [ ] T029 [P] [US2] Compare PluginLoader.kt between VOS4 and MagicCode via diff script
- [ ] T030 [P] [US2] Compare PermissionStorage.kt (expect class) between VOS4 and MagicCode via diff script
- [ ] T031 [US2] Generate equality report at `contracts/equality-report.md` documenting matches and expected platform differences
- [ ] T032 [US2] Manual verification of API signatures - confirm public methods match across repositories

**US2 Acceptance Criteria** (from spec.md):
- [x] Diff script created and executable
- [x] Equality report shows 100% match for core APIs
- [x] Expected platform differences documented
- [x] Manual verification confirms API compatibility

**US2 Independent Test**: Run verify-equality.sh script - report shows 100% core API match

**Dependency Note**: US2 depends on US1 (need encryption files in MagicCode to compare)

---

## Phase 5: User Story 3 (P2) - Update VOS4 Dependencies (8 tasks)

**User Story**: As a VOS4 developer, I want VOS4 to consume PluginSystem from MagicCode via Gradle, so that VOS4 uses the canonical library instead of a duplicate.

**Independent Test**: Can be fully tested by removing VOS4's local PluginSystem module, adding MagicCode dependency, and verifying VOS4 builds successfully.

**Duration**: 1 hour

**Dependency**: Requires US1 complete (MagicCode PluginSystem fully functional)

### Tasks

#### Configure Composite Build (2 tasks)

- [ ] T033 [US3] Add composite build to VOS4 settings.gradle.kts at `/Volumes/M Drive/Coding/vos4/settings.gradle.kts` - includeBuild MagicCode with dependencySubstitution
- [ ] T034 [US3] Update VOS4 PluginSystem build.gradle.kts at `vos4/modules/libraries/PluginSystem/build.gradle.kts` - replace local implementation with `implementation("com.augmentalis.magiccode:plugin-system")`

#### Archive Local Module (1 task)

- [ ] T035 [US3] Archive VOS4's local PluginSystem module to `vos4/modules/libraries/_archived/PluginSystem-$(date +%Y%m%d)` via `mkdir -p` and `mv` commands

#### Build & Test (3 tasks)

- [ ] T036 [US3] Build VOS4 with MagicCode dependency via `cd /Volumes/M\ Drive/Coding/vos4 && ./gradlew :app:assembleDebug` (expect successful build)
- [ ] T037 [US3] Run VOS4 tests via `./gradlew :app:test` (expect all tests passing including 8 encryption tests)
- [ ] T038 [US3] Runtime verification - launch VOS4 app via emulator and verify PluginSystem functionality works

#### Fallback Strategy (2 tasks - conditional)

- [ ] T039 [US3] IF composite build fails: Implement fallback Maven local publish strategy in `magiccode/runtime/plugin-system/build.gradle.kts` (add maven-publish plugin)
- [ ] T040 [US3] IF using Maven fallback: Update VOS4 to use mavenLocal() repository and versioned dependency

**US3 Acceptance Criteria** (from spec.md):
- [x] Gradle dependency strategy implemented (composite build or Maven fallback)
- [x] VOS4 builds successfully with MagicCode dependency
- [x] VOS4's local PluginSystem module archived
- [x] All VOS4 tests pass (8 encryption tests + existing tests)
- [x] Runtime verification: VOS4 app launches and PluginSystem works

**US3 Independent Test**: Build VOS4 from clean state with MagicCode dependency - app launches, all tests pass

**Dependency Note**: US3 depends on US1 (need functional MagicCode PluginSystem)

---

## Phase 6: User Story 4 (P3) - Documentation Synchronization (6 tasks)

**User Story**: As a plugin developer, I want MagicCode documentation to include encrypted storage guides, so that I understand how to use encrypted permission storage.

**Independent Test**: Can be fully tested by reading documentation and successfully implementing encrypted permission storage in a test plugin.

**Duration**: 1 hour

**Dependency**: Can be done in parallel with US3 (documentation independent of integration)

### Tasks

- [ ] T041 [P] [US4] Copy VOS4 EVALUATION-REPORT-Phase1-3.md to `magiccode/runtime/plugin-system/docs/ENCRYPTION-IMPLEMENTATION-REPORT.md` (rename for clarity)
- [ ] T042 [P] [US4] Update PLUGIN_DEVELOPER_GUIDE.md at `magiccode/runtime/plugin-system/docs/PLUGIN_DEVELOPER_GUIDE.md` - add "Encrypted Permission Storage" section with 2+ usage examples
- [ ] T043 [P] [US4] Update ARCHITECTURE.md at `magiccode/runtime/plugin-system/docs/ARCHITECTURE.md` - add encryption architecture diagram (KeyManager → EncryptedStorageFactory → PermissionStorage)
- [ ] T044 [P] [US4] Update TESTING_GUIDE.md at `magiccode/runtime/plugin-system/docs/TESTING_GUIDE.md` - add encryption testing examples (unit tests, performance benchmarks)
- [ ] T045 [P] [US4] Update README.md at `magiccode/runtime/plugin-system/README.md` - mention encrypted permission storage feature
- [ ] T046 [US4] Review all documentation updates for consistency with MagicCode's existing style and completeness

**US4 Acceptance Criteria** (from spec.md):
- [x] PLUGIN_DEVELOPER_GUIDE.md updated with 2+ usage examples
- [x] ARCHITECTURE.md updated with encryption architecture diagram
- [x] TESTING_GUIDE.md updated with encryption testing section
- [x] README.md mentions encrypted permission storage
- [x] All documentation follows MagicCode's existing style

**US4 Independent Test**: Follow documentation to implement encrypted storage in test plugin - implementation works correctly

**Dependency Note**: US4 depends on US1 (need encryption implementation to document). Can run in parallel with US3.

---

## Phase 7: Polish & Completion (1 task)

**Objective**: Final verification and project completion

**Duration**: 30 minutes

### Tasks

- [ ] T047 Create COMPLETION-REPORT.md at `/Volumes/M Drive/Coding/vos4/specs/003-pluginsystem-refactor/COMPLETION-REPORT.md` documenting: tasks completed, test results (290 MagicCode, VOS4 passing), performance validation (<5ms latency), success criteria met, commits created

**Acceptance**: Synchronization complete, both repositories functional, documentation updated

---

## Dependencies & Execution Order

### Story Dependencies

```
Phase 1 (Setup) → Phase 2 (Foundational)
                        ↓
                  Phase 3 (US1 - P1)
                        ↓
                  ┌─────┴─────┐
                  ↓           ↓
         Phase 4 (US2)   Phase 6 (US4)
                  ↓           ↓
         Phase 5 (US3)        │
                  └─────┬─────┘
                        ↓
                  Phase 7 (Polish)
```

**Critical Path**: Phase 1 → Phase 2 → Phase 3 (US1) → Phase 5 (US3) → Phase 7

**Parallel Opportunities**:
- US2 and US4 can partially overlap with US1 completion
- US4 can run fully in parallel with US3 (documentation independent of integration)

### Task Parallelization Within Phases

**Phase 2 (Foundational)**: T004-T005 (test runs), T007-T008 (diff checks) can run in parallel

**Phase 3 (US1)**:
- T012-T015 (common files) - all parallelizable
- T016-T018 (Android files) - all parallelizable
- T020-T021 (stubs) - parallelizable

**Phase 4 (US2)**: T027-T030 (diff comparisons) - all parallelizable

**Phase 6 (US4)**: T041-T045 (documentation updates) - all parallelizable

---

## MVP Scope

**Minimum Viable Product**: US1 + US2 (Phases 1-4)

**MVP Delivers**:
- ✅ MagicCode has encrypted permission storage (7 files + 8 tests)
- ✅ All 290 tests pass (282 + 8)
- ✅ Repository equality verified
- ✅ MagicCode is functional as standalone library

**MVP Duration**: 2.5 hours

**Post-MVP**: US3 integrates VOS4, US4 adds documentation polish (1.5 hours additional)

---

## Task Summary

**Total Tasks**: 47
- Phase 1 (Setup): 3 tasks
- Phase 2 (Foundational): 6 tasks
- Phase 3 (US1 - P1): 17 tasks
- Phase 4 (US2 - P1): 6 tasks
- Phase 5 (US3 - P2): 8 tasks
- Phase 6 (US4 - P3): 6 tasks
- Phase 7 (Polish): 1 task

**Parallelizable Tasks**: 22 tasks (47% of total)

**Independent Test Criteria**:
- US1: Run MagicCode tests (290 passing)
- US2: Run diff script (100% core API match)
- US3: Build VOS4 with MagicCode dependency (app launches, tests pass)
- US4: Follow docs to implement encryption (implementation works)

**Success Criteria Mapping** (from spec.md):
- SC-001: MagicCode compiles → T024 (US1)
- SC-002: All 290 tests pass → T026 (US1)
- SC-003: File diff shows equality → T031 (US2)
- SC-004: VOS4 builds with MagicCode → T036 (US3)
- SC-005: Performance <5ms → T047 (verification in completion report)
- SC-006: Documentation updated → T046 (US4)
- SC-007: Zero regressions → T026 (US1 - 282 tests still pass)
- SC-008: Completed in <4 hours → All tasks (estimated 4 hours total)

---

## Format Validation

**Checklist Format**: ✅ ALL 47 tasks follow required format:
- Checkbox: `- [ ]` present
- Task ID: T001-T047 sequential
- [P] marker: 22 parallelizable tasks marked
- [Story] label: 37 tasks have US1-US4 labels (setup/foundational/polish excluded)
- Description: Clear action with file paths

**Organization**: ✅ Tasks organized by user story (Phases 3-6 map to US1-US4)

**Dependencies**: ✅ Dependency graph provided showing story completion order

**Independent Testing**: ✅ Each user story has independent test criteria

---

**Ready for Execution**: Tasks can be executed sequentially or with parallelization as marked. Use `/idea.implement` to begin implementation.
