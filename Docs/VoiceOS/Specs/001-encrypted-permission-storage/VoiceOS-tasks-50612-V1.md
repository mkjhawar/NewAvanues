# Tasks: Encrypted Permission Storage

**Input**: Design documents from `/specs/001-encrypted-permission-storage/`
**Prerequisites**: plan.md (complete), spec.md (complete), research.md (complete), data-model.md (complete), contracts/ (complete)

**Tests**: Tests are MANDATORY per NFR-006 (>90% code coverage). @vos4-test-specialist will enforce test quality during IDE Loop Defend phase.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

This is an Android library module within VOS4 monorepo:
- **Source**: `modules/libraries/PluginSystem/src/{commonMain,androidMain}/kotlin/com/augmentalis/magiccode/plugins/`
- **Tests**: `modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/`
- **Docs**: `docs/modules/PluginSystem/`
- **Build**: `modules/libraries/PluginSystem/build.gradle.kts`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependency setup for encrypted storage

- [X] T001 Add androidx.security:security-crypto:1.1.0-alpha06 dependency in modules/libraries/PluginSystem/build.gradle.kts
- [X] T002 [P] Configure backup exclusion rules in app/src/main/res/xml/backup_rules.xml
- [X] T003 [P] Update AndroidManifest.xml to reference backup_rules.xml (android:fullBackupContent attribute)
- [X] T004 [P] Verify Kotlin coroutines dependency (org.jetbrains.kotlinx:kotlinx-coroutines-android) in build.gradle.kts
- [X] T005 [P] Verify MockK dependency (io.mockk:mockk) in build.gradle.kts for test mocking

**Checkpoint**: ✅ Dependencies configured, backup exclusion rules ready

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 Create MasterKey utility for encryption key generation in modules/libraries/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/KeyManager.kt
- [ ] T007 Create EncryptedSharedPreferences factory wrapper in modules/libraries/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptedStorageFactory.kt
- [ ] T008 [P] Define MigrationResult sealed class in modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/MigrationResult.kt
- [ ] T009 [P] Define EncryptionStatus data class in modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionStatus.kt
- [ ] T010 [P] Define EncryptionException and Migration Exception in modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/Exceptions.kt
- [ ] T011 Add security audit logging tags to PluginLog in modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/core/PluginLog.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Hardware-Backed Permission Encryption (Priority: P1) 🎯 MVP

**Goal**: Encrypt all new plugin permission grants using hardware-backed AES256-GCM encryption

**Independent Test**:
1. Grant a permission to "com.example.plugin"
2. Verify with `adb shell cat /data/data/com.augmentalis.vos4/shared_prefs/*.xml` that data is encrypted
3. Verify encryption keys are in Android Keystore (not accessible via ADB)
4. Verify app can read and use encrypted permissions correctly

### Tests for User Story 1 ✅ MANDATORY

> **NOTE: Write these tests FIRST (TDD), ensure they FAIL before implementation**

- [ ] T012 [P] [US1] Unit test: testEncryptionRoundTrip() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T013 [P] [US1] Unit test: testMultiplePermissionsEncrypted() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T014 [P] [US1] Unit test: testHardwareKeystoreDetection() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T015 [P] [US1] Unit test: testCorruptedEncryptedDataDetection() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T016 [P] [US1] Unit test: testConcurrentPermissionGrants() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T017 [P] [US1] Performance benchmark: testPerformanceBenchmark() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStoragePerformanceTest.kt
- [ ] T018 [P] [US1] Performance benchmark: benchmarkEncryptionLatency() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStoragePerformanceTest.kt
- [ ] T019 [P] [US1] Performance benchmark: benchmarkDecryptionLatency() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStoragePerformanceTest.kt
- [ ] T020 [P] [US1] Integration test: testEndToEndPermissionGrantWithEncryption() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/integration/PluginManagerEncryptionIntegrationTest.kt

**Verification Checkpoint**: All tests MUST fail at this point (RED phase of TDD)

### Implementation for User Story 1

- [ ] T021 [US1] Modify PermissionStorage expect class in modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt to add encryption status methods
- [ ] T022 [US1] Implement PermissionStorage actual class with EncryptedSharedPreferences in modules/libraries/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt
- [ ] T023 [US1] Replace plain SharedPreferences initialization with EncryptedSharedPreferences using MasterKey in PermissionStorage.kt:line 14
- [ ] T024 [US1] Add StrongBox keystore detection and fallback logic in PermissionStorage initialization
- [ ] T025 [US1] Implement savePermission() method to encrypt permission grants transparently
- [ ] T026 [US1] Implement getPermission() method to decrypt permission grants transparently
- [ ] T027 [US1] Implement getAllPermissions() method to decrypt all permissions for a plugin
- [ ] T028 [US1] Implement revokePermission() method with encrypted storage
- [ ] T029 [US1] Implement clearAllPermissions() method with encrypted storage
- [ ] T030 [US1] Add GCM authentication tag verification on decrypt operations
- [ ] T031 [US1] Implement isEncrypted() method to check encryption status
- [ ] T032 [US1] Implement getEncryptionStatus() method to return detailed encryption info
- [ ] T033 [US1] Add security audit logging for encryption key generation in PermissionStorage
- [ ] T034 [US1] Add security audit logging for all permission grant/query operations
- [ ] T035 [US1] Add KDoc documentation explaining security implications of each method

**Verification Checkpoint**: Run tests - all US1 tests should now PASS (GREEN phase of TDD)

**Acceptance Checkpoint**:
- SC-001: Verify permission data unreadable via ADB (manual test on device)
- SC-002: Verify encryption adds <5ms latency (T017-T019 benchmarks)
- SC-004: Verify hardware keystore usage (T014 test)
- SC-007: Verify security event logging (T033-T034 implementation)
- SC-009: Verify concurrent access safety (T016 test)

---

## Phase 4: User Story 2 - Seamless Migration from Plain Storage (Priority: P2)

**Goal**: Automatically migrate existing plain-text permissions to encrypted storage on first launch

**Independent Test**:
1. Create mock plain-text permissions in old SharedPreferences format
2. Initialize PermissionStorage
3. Verify all permissions migrated to encrypted storage
4. Verify old plain-text file deleted
5. Verify no permissions lost

### Tests for User Story 2 ✅ MANDATORY

- [ ] T036 [P] [US2] Unit test: testMigrationFromPlainToEncrypted() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionMigratorTest.kt
- [ ] T037 [P] [US2] Unit test: testMigrationIdempotency() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionMigratorTest.kt
- [ ] T038 [P] [US2] Unit test: testMigrationWithCorruptedData() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionMigratorTest.kt
- [ ] T039 [P] [US2] Unit test: testZeroPlainTextAfterMigration() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionMigratorTest.kt
- [ ] T040 [P] [US2] Unit test: testPermissionPersistenceAcrossRestart() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T041 [P] [US2] Integration test: testPermissionQueryAfterAppRestart() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/integration/PluginManagerEncryptionIntegrationTest.kt
- [ ] T042 [P] [US2] Integration test: testMigrationDuringActivePermissionCheck() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/integration/PluginManagerEncryptionIntegrationTest.kt

**Verification Checkpoint**: All tests MUST fail at this point (RED phase of TDD)

### Implementation for User Story 2

- [ ] T043 [US2] Create EncryptionMigrator class in modules/libraries/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/security/EncryptionMigrator.kt
- [ ] T044 [US2] Implement detectPlainTextPermissions() method to scan for old SharedPreferences file
- [ ] T045 [US2] Implement readPlainTextPermissions() method to read all plain-text permissions
- [ ] T046 [US2] Implement migratePermissions() method with atomic all-or-nothing semantics
- [ ] T047 [US2] Implement saveMigrationState() method to track migration completion in encrypted storage
- [ ] T048 [US2] Implement isMigrationComplete() method to check if migration already ran (idempotency)
- [ ] T049 [US2] Add PermissionStorage.migrateToEncrypted() suspend method wrapping EncryptionMigrator
- [ ] T050 [US2] Implement automatic migration trigger on PermissionStorage initialization
- [ ] T051 [US2] Add mutex synchronization to prevent concurrent migration attempts
- [ ] T052 [US2] Implement deletePlainTextFile() method to remove old file after successful migration
- [ ] T053 [US2] Add migration result logging (success/failure counts) to security audit log
- [ ] T054 [US2] Add error handling for corrupted plain-text data (skip and log)
- [ ] T055 [US2] Add rollback mechanism if migration fails mid-process (restore from backup)
- [ ] T056 [US2] Add KDoc documentation explaining migration process and idempotency

**Verification Checkpoint**: Run tests - all US2 tests should now PASS (GREEN phase of TDD)

**Acceptance Checkpoint**:
- SC-003: Verify 100% migration success rate (T036 test with 50 mock permissions)
- SC-006: Verify zero plain-text data after migration (T039 test)
- SC-008: Verify app restart persistence (T040-T041 tests)
- NFR-007: Verify migration idempotency (T037 test)

---

## Phase 5: User Story 3 - Graceful Encryption Failure Handling (Priority: P3)

**Goal**: Fall back to software encryption if hardware keystore unavailable, fail secure on complete encryption failure

**Independent Test**:
1. Mock hardware keystore failure
2. Grant a permission
3. Verify system falls back to software encryption
4. Verify permissions remain functional
5. Verify user notified of degraded security

### Tests for User Story 3 ✅ MANDATORY

- [ ] T057 [P] [US3] Unit test: testEncryptionFailureFallback() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T058 [P] [US3] Unit test: testSoftwareKeystoreFallback() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T059 [P] [US3] Unit test: testKeyDeletionRecovery() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T060 [P] [US3] Unit test: testCompleteEncryptionFailureFailSecure() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt

**Verification Checkpoint**: All tests MUST fail at this point (RED phase of TDD)

### Implementation for User Story 3

- [ ] T061 [US3] Implement hardware keystore availability detection in KeyManager.kt
- [ ] T062 [US3] Implement software keystore fallback in MasterKey creation (setRequestStrongBoxBacked(false))
- [ ] T063 [US3] Add try-catch wrapper around MasterKey.Builder with fallback chain
- [ ] T064 [US3] Implement key invalidation detection (KeyPermanentlyInvalidatedException handling)
- [ ] T065 [US3] Implement automatic key regeneration when keystore cleared
- [ ] T066 [US3] Add fail-secure logic: deny permission grant if encryption completely unavailable
- [ ] T067 [US3] Add warning logs when software keystore is used (degraded security)
- [ ] T068 [US3] Add error logs when encryption fails completely
- [ ] T069 [US3] Update getEncryptionStatus() to include isHardwareBacked flag
- [ ] T070 [US3] Add KDoc warnings about security implications of software keystore

**Verification Checkpoint**: Run tests - all US3 tests should now PASS (GREEN phase of TDD)

**Acceptance Checkpoint**:
- FR-006: Verify fail-secure behavior (T060 test)
- Verify software fallback functional (T058 test)
- Verify key regeneration on invalidation (T059 test)

---

## Phase 6: User Story 4 - Backup and Restore Compatibility (Priority: P3)

**Goal**: Ensure encrypted permissions remain secure in backups and fail gracefully on restore to different device

**Independent Test**:
1. Grant several permissions
2. Perform Android backup
3. Verify backup does not contain decryptable permission data
4. Restore backup on same device → permissions work
5. Restore on different device → graceful failure

### Tests for User Story 4 ✅ MANDATORY

- [ ] T071 [P] [US4] Unit test: testBackupExclusion() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T072 [P] [US4] Unit test: testDirectBootMode() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorageEncryptionTest.kt
- [ ] T073 [P] [US4] Integration test: testBackupRestoreSameDevice() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/integration/BackupRestoreIntegrationTest.kt
- [ ] T074 [P] [US4] Integration test: testBackupRestoreDifferentDevice() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/integration/BackupRestoreIntegrationTest.kt

**Verification Checkpoint**: All tests MUST fail at this point (RED phase of TDD)

### Implementation for User Story 4

- [ ] T075 [US4] Verify backup_rules.xml excludes plugin_permissions_encrypted.xml (already done in T002)
- [ ] T076 [US4] Add manifest validation that android:allowBackup references backup_rules.xml
- [ ] T077 [US4] Implement Direct Boot mode detection in PermissionStorage
- [ ] T078 [US4] Implement ACTION_USER_UNLOCKED broadcast receiver to reload permissions after unlock
- [ ] T079 [US4] Add in-memory permission cache for Direct Boot mode pre-unlock access
- [ ] T080 [US4] Return empty permissions or cached data when device locked (Direct Boot)
- [ ] T081 [US4] Add graceful decryption failure handling for device-migration scenario
- [ ] T082 [US4] Add user notification when permissions cannot be restored (different device)
- [ ] T083 [US4] Add KDoc explaining backup/restore behavior and device-binding

**Verification Checkpoint**: Run tests - all US4 tests should now PASS (GREEN phase of TDD)

**Acceptance Checkpoint**:
- SC-010: Verify backup does not leak plain-text data (T071 test + manual extraction)
- FR-011: Verify Direct Boot mode support (T072, T077-T080 implementation)
- Verify restore on different device fails gracefully (T074 test)

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories, documentation, and final validation

### Documentation Updates

- [ ] T084 [P] Update Developer-Manual-PluginSystem-251026-1146.md with encryption section in docs/modules/PluginSystem/
- [ ] T085 [P] Add "Encrypted Permission Storage" chapter to Developer Manual explaining implementation
- [ ] T086 [P] Add security considerations section to Developer Manual
- [ ] T087 [P] Add troubleshooting guide for encryption issues to Developer Manual
- [ ] T088 [P] Update Whats-Missing-PluginSystem-251026-1146.md to remove TODO #43 (encrypted storage) in docs/modules/PluginSystem/
- [ ] T089 [P] Add migration guide for existing users to Developer Manual

### Code Quality & Refactoring

- [ ] T090 [P] Code review: Verify all methods have comprehensive KDoc with security warnings
- [ ] T091 [P] Code review: Verify zero compiler warnings in PermissionStorage module
- [ ] T092 [P] Run ktlint formatter on all modified Kotlin files
- [ ] T093 [P] Verify Hilt dependency injection configuration for PermissionStorage singleton
- [ ] T094 Performance profiling: Verify <5ms latency on physical device (not emulator)

### Integration Verification

- [ ] T095 Integration test: testMultiplePluginsEncryptedSeparately() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/integration/PluginManagerEncryptionIntegrationTest.kt
- [ ] T096 Integration test: testPermissionRevocationWithEncryption() in modules/libraries/PluginSystem/src/androidTest/kotlin/com/augmentalis/magiccode/plugins/integration/PluginManagerEncryptionIntegrationTest.kt
- [ ] T097 Verify PluginManager compatibility: No changes required to PluginManager consumers
- [ ] T098 Verify PluginRegistry integration: Permission checks work with encrypted storage

### Manual Acceptance Testing

- [ ] T099 Manual test: SC-001 - ADB unreadability verification on physical device
- [ ] T100 Manual test: SC-004 - Hardware keystore verification (check MasterKey.isInsideSecureHardware())
- [ ] T101 Manual test: SC-008 - Device reboot persistence test on physical device
- [ ] T102 Manual test: SC-010 - Android backup extraction and plain-text search

### Quickstart Validation

- [ ] T103 [P] Run through quickstart.md examples to verify accuracy
- [ ] T104 [P] Test all code snippets in quickstart.md compile and run correctly
- [ ] T105 [P] Verify troubleshooting section in quickstart.md covers common errors

### Final Verification

- [ ] T106 Run full test suite: Verify >90% code coverage on PermissionStorage.kt
- [ ] T107 Run performance benchmark suite: Verify all latency targets met
- [ ] T108 Verify all 10 Success Criteria (SC-001 through SC-010) pass
- [ ] T109 Verify all 13 Functional Requirements (FR-001 through FR-013) implemented
- [ ] T110 Verify all 7 Non-Functional Requirements (NFR-001 through NFR-007) met

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion (T001-T005) - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion (T006-T011)
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order: US1 (P1) → US2 (P2) → US3 (P3) → US4 (P3)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories ✅ INDEPENDENT
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - No dependencies on US1 (but integrates with US1 implementation) ✅ INDEPENDENT
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - No dependencies on US1/US2 (error handling is orthogonal) ✅ INDEPENDENT
- **User Story 4 (P3)**: Can start after Foundational (Phase 2) - No dependencies on US1/US2/US3 (backup is orthogonal) ✅ INDEPENDENT

### Within Each User Story (TDD Flow)

1. **Write tests FIRST** (RED phase) - tests MUST fail
2. **Implement code** (GREEN phase) - make tests pass
3. **Refactor** (REFACTOR phase) - clean up code
4. **Verify acceptance criteria** - story complete

**Strict Order Within Story:**
- Tests before implementation (TDD)
- Models/utilities before services
- Services before integration
- Core implementation before edge cases
- Story verified complete before moving to next priority

### Parallel Opportunities

**Phase 1 (Setup):**
- T002, T003, T004, T005 can run in parallel

**Phase 2 (Foundational):**
- T008, T009, T010 can run in parallel (different files)

**Phase 3 (US1 Tests):**
- T012-T020 can all run in parallel (different test files/methods)

**Phase 3 (US1 Implementation):**
- T033, T034, T035 can run in parallel with other tasks (logging and docs)

**Phase 4 (US2 Tests):**
- T036-T042 can all run in parallel

**Phase 5 (US3 Tests):**
- T057-T060 can all run in parallel

**Phase 6 (US4 Tests):**
- T071-T074 can all run in parallel

**Phase 7 (Polish):**
- All documentation tasks (T084-T089) can run in parallel
- All code quality tasks (T090-T094) can run in parallel
- All quickstart validation tasks (T103-T105) can run in parallel

**Cross-Story Parallelism:**
Once Foundational phase completes, multiple developers can work on different user stories simultaneously:
- Developer A: User Story 1 (T012-T035)
- Developer B: User Story 2 (T036-T056)
- Developer C: User Story 3 (T057-T070)
- Developer D: User Story 4 (T071-T083)

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all US1 tests in parallel (TDD RED phase):
Task: "Unit test: testEncryptionRoundTrip() in PermissionStorageEncryptionTest.kt"
Task: "Unit test: testMultiplePermissionsEncrypted() in PermissionStorageEncryptionTest.kt"
Task: "Unit test: testHardwareKeystoreDetection() in PermissionStorageEncryptionTest.kt"
Task: "Unit test: testCorruptedEncryptedDataDetection() in PermissionStorageEncryptionTest.kt"
Task: "Unit test: testConcurrentPermissionGrants() in PermissionStorageEncryptionTest.kt"
Task: "Performance benchmark: testPerformanceBenchmark() in PermissionStoragePerformanceTest.kt"
Task: "Performance benchmark: benchmarkEncryptionLatency() in PermissionStoragePerformanceTest.kt"
Task: "Performance benchmark: benchmarkDecryptionLatency() in PermissionStoragePerformanceTest.kt"
Task: "Integration test: testEndToEndPermissionGrantWithEncryption() in PluginManagerEncryptionIntegrationTest.kt"

# All tests should FAIL (RED) - ready for implementation
```

## Parallel Example: User Story 2 Tests

```bash
# Launch all US2 tests in parallel (TDD RED phase):
Task: "Unit test: testMigrationFromPlainToEncrypted() in EncryptionMigratorTest.kt"
Task: "Unit test: testMigrationIdempotency() in EncryptionMigratorTest.kt"
Task: "Unit test: testMigrationWithCorruptedData() in EncryptionMigratorTest.kt"
Task: "Unit test: testZeroPlainTextAfterMigration() in EncryptionMigratorTest.kt"
Task: "Unit test: testPermissionPersistenceAcrossRestart() in PermissionStorageEncryptionTest.kt"
Task: "Integration test: testPermissionQueryAfterAppRestart() in PluginManagerEncryptionIntegrationTest.kt"
Task: "Integration test: testMigrationDuringActivePermissionCheck() in PluginManagerEncryptionIntegrationTest.kt"

# All tests should FAIL (RED) - ready for implementation
```

---

## Implementation Strategy

### MVP First (User Story 1 Only) 🎯 RECOMMENDED

**Fastest path to deliverable value:**

1. ✅ Complete Phase 1: Setup (T001-T005) - ~15 minutes
2. ✅ Complete Phase 2: Foundational (T006-T011) - ~30 minutes
3. ✅ Complete Phase 3: User Story 1 (T012-T035) - ~1-1.5 hours
4. **STOP and VALIDATE**: Test US1 independently - ~15 minutes
5. **Deploy/Demo**: Hardware-backed encryption working! 🎉

**Total MVP Time: ~2-2.5 hours**

**MVP Value:**
- All NEW permission grants are encrypted (FR-001 ✅)
- Hardware-backed security (FR-002 ✅)
- <5ms latency (FR-008 ✅)
- 90%+ test coverage (NFR-006 ✅)
- Ready for production use with new installations

**What's NOT in MVP:**
- Migration from old plain-text (US2) - can be added later
- Error handling edge cases (US3) - can be added later
- Backup/restore optimization (US4) - can be added later

### Incremental Delivery (Recommended for Production)

**Add user stories incrementally, each independently tested:**

1. ✅ Foundation (T001-T011) → ~45 minutes
2. ✅ US1 (T012-T035) → Test independently → Deploy/Demo → ~1-1.5 hours ✅ MVP!
3. ✅ US2 (T036-T056) → Test independently → Deploy/Demo → ~1 hour ✅ Migration added!
4. ✅ US3 (T057-T070) → Test independently → Deploy/Demo → ~30 minutes ✅ Robust error handling!
5. ✅ US4 (T071-T083) → Test independently → Deploy/Demo → ~30 minutes ✅ Complete backup security!
6. ✅ Polish (T084-T110) → Final validation → Production ready → ~1 hour ✅ DONE!

**Total Time: ~5-6 hours** (matches plan.md estimate of 2-3 hours for US1 MVP + 3 hours for remaining stories)

**Each increment adds value without breaking previous stories**

### Parallel Team Strategy

**If multiple developers available:**

1. **Together**: Complete Setup + Foundational (T001-T011) - ~45 minutes
2. **Split** once Foundational is done:
   - **Developer A**: User Story 1 (T012-T035) - Core encryption
   - **Developer B**: User Story 2 (T036-T056) - Migration
   - **Developer C**: User Story 3 (T057-T070) - Error handling
   - **Developer D**: User Story 4 (T071-T083) - Backup/restore
3. **Merge**: Each story tested independently, then integrated
4. **Together**: Polish phase (T084-T110) - Final validation

**Parallel Time: ~2-3 hours total** (with 4 developers)

---

## Task Statistics

**Total Tasks**: 110 tasks
**Task Breakdown by Phase:**
- Phase 1 (Setup): 5 tasks
- Phase 2 (Foundational): 6 tasks
- Phase 3 (US1 - P1): 24 tasks (9 tests + 15 implementation)
- Phase 4 (US2 - P2): 21 tasks (7 tests + 14 implementation)
- Phase 5 (US3 - P3): 14 tasks (4 tests + 10 implementation)
- Phase 6 (US4 - P3): 13 tasks (4 tests + 9 implementation)
- Phase 7 (Polish): 27 tasks (docs, quality, validation)

**Parallel Opportunities**: 45 tasks marked [P] can run in parallel

**Independent Test Criteria**:
- ✅ US1: Grant permission → verify ADB unreadable → verify app can read → PASS
- ✅ US2: Create mock plain-text → migrate → verify encrypted → verify old file deleted → PASS
- ✅ US3: Mock keystore failure → verify fallback → verify functional → PASS
- ✅ US4: Grant permissions → backup → restore → verify security → PASS

**MVP Scope (US1 Only)**: 35 tasks (Phase 1 + Phase 2 + Phase 3)
**MVP Estimated Time**: 2-2.5 hours

---

## Notes

- **[P] tasks** = different files, no dependencies, can run in parallel
- **[Story] label** maps task to specific user story for traceability (US1, US2, US3, US4)
- **TDD Mandatory**: Write tests FIRST, verify FAIL, then implement
- **Each user story** should be independently completable and testable
- **Stop at any checkpoint** to validate story independently before proceeding
- **Commit frequently**: After each task or logical group of related tasks
- **Quality gates enforced**: @vos4-test-specialist blocks if tests fail or coverage <90%
- **Constitution compliance**: All 5 VOS4 principles verified as PASS in plan.md

---

## Success Criteria Mapping

| Success Criterion | Verified By Task(s) |
|-------------------|---------------------|
| SC-001: ADB unreadable | T099 (manual test) |
| SC-002: <5ms latency | T017-T019, T094, T107 |
| SC-003: 100% migration | T036 (50 mock permissions) |
| SC-004: Hardware keystore | T014, T100 (manual test) |
| SC-005: Corruption detection | T015, T030 |
| SC-006: Zero plain-text post-migration | T039, T052 |
| SC-007: Security event logging | T033, T034, T051, T053 |
| SC-008: App restart persistence | T040, T041, T101 (manual) |
| SC-009: Concurrent grant safety | T016, T050 |
| SC-010: Backup security | T071, T102 (manual test) |

---

**Ready to implement!** Run `/idea.implement` to start execution with IDE Loop (Implement → Defend → Evaluate → Commit).
