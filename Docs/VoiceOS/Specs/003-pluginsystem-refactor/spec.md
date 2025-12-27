# Feature Specification: PluginSystem Repository Synchronization

**Feature Branch**: `003-pluginsystem-refactor`
**Created**: 2025-10-26
**Status**: Draft
**Input**: User request: "Merge VOS4 encrypted storage to MagicCode PluginSystem to properly reconcile and ensure both repositories are equal with regard to the pluginsystems"

## Background

**Problem**: VOS4 PluginSystem and MagicCode PluginSystem have **DIVERGED significantly**:

| Aspect | MagicCode | VOS4 | Status |
|--------|-----------|------|--------|
| **Location** | `/Coding/magiccode/runtime/plugin-system/` | `/Coding/vos4/modules/libraries/PluginSystem/` | Different |
| **File Count** | 97 Kotlin files | 105 Kotlin files | VOS4 has 8 more |
| **Encrypted Storage** | ❌ NO | ✅ YES (just added) | **VOS4 AHEAD** |
| **Test Count** | 282 tests (per docs) | 8 tests (just created) | MagicCode ahead |
| **KMP Structure** | ✅ Full iOS/JVM/Android | ⚠️ Android-focused | MagicCode better |
| **Documentation** | ✅ Extensive (5 MD files) | ❌ Minimal | MagicCode better |

**Goal**: Establish **MagicCode as the canonical library**, VOS4 as the consumer, with full repository equality.

**Reference**: `/Volumes/M Drive/Coding/vos4/docs/Active/PluginSystem-Divergence-Analysis-251026-1620.md`

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Copy Encrypted Storage to MagicCode (Priority: P1)

**As a** PluginSystem library maintainer
**I want** MagicCode to have the same encrypted permission storage as VOS4
**So that** all consumers benefit from hardware-backed encryption security

**Why this priority**: CRITICAL SECURITY FEATURE - encrypted storage protects permission grants from ADB extraction and tampering

**Independent Test**: Can be fully tested by running MagicCode's 282 existing tests + 8 new encryption tests and verifying all pass

**Acceptance Scenarios**:

1. **Given** VOS4 has 7 encryption files (KeyManager, EncryptedStorageFactory, etc.), **When** files are copied to MagicCode with correct KMP structure, **Then** MagicCode compiles successfully with no errors
2. **Given** VOS4 has 8 encryption tests, **When** tests are copied to MagicCode, **Then** all 8 tests pass on Android target
3. **Given** MagicCode has 282 existing tests, **When** encrypted storage is added, **Then** all 282 existing tests still pass (no regressions)
4. **Given** MagicCode supports iOS/JVM/Android, **When** encrypted storage is added, **Then** expect/actual pattern works correctly on all platforms (iOS/JVM get stub implementations)

---

### User Story 2 - Verify Repository Equality (Priority: P1)

**As a** developer maintaining two PluginSystem repositories
**I want** to verify VOS4 and MagicCode are functionally equivalent
**So that** I can confidently switch VOS4 to depend on MagicCode

**Why this priority**: PREVENTS INTEGRATION FAILURES - must verify equality before changing dependencies

**Independent Test**: Can be fully tested by running a diff comparison script and verifying all core files match (excluding platform-specific differences)

**Acceptance Scenarios**:

1. **Given** both repositories have PluginSystem modules, **When** file-by-file comparison is performed, **Then** all core API files match exactly (PluginManager, PluginLoader, etc.)
2. **Given** both repositories have permission storage, **When** PermissionStorage.kt is compared, **Then** interfaces and implementations match (excluding platform stubs)
3. **Given** both repositories have tests, **When** test coverage is compared, **Then** MagicCode has all VOS4 tests + its existing 282 tests (290 total minimum)
4. **Given** both repositories have build configurations, **When** dependencies are compared, **Then** androidx.security:security-crypto present in both

---

### User Story 3 - Update VOS4 to Depend on MagicCode (Priority: P2)

**As a** VOS4 developer
**I want** VOS4 to consume PluginSystem from MagicCode via Gradle
**So that** VOS4 uses the canonical library instead of a duplicate

**Why this priority**: ARCHITECTURAL CORRECTNESS - establishes proper library-consumer relationship, but can be done after equality verified

**Independent Test**: Can be fully tested by removing VOS4's local PluginSystem module, adding MagicCode dependency, and verifying VOS4 builds successfully

**Acceptance Scenarios**:

1. **Given** MagicCode PluginSystem is complete and tested, **When** VOS4's build.gradle.kts is modified to depend on MagicCode, **Then** VOS4 compiles successfully
2. **Given** VOS4 used to have local PluginSystem module, **When** module is removed and replaced with MagicCode dependency, **Then** all VOS4 integration tests pass
3. **Given** VOS4 has 8 encrypted storage tests, **When** tests are run against MagicCode dependency, **Then** all 8 tests still pass
4. **Given** VOS4 depends on MagicCode, **When** MagicCode updates encrypted storage, **Then** VOS4 automatically gets updates via version bump

---

### User Story 4 - Documentation Synchronization (Priority: P3)

**As a** plugin developer
**I want** MagicCode documentation to include encrypted storage guides
**So that** I understand how to use encrypted permission storage

**Why this priority**: DEVELOPER EXPERIENCE - important but not blocking integration

**Independent Test**: Can be fully tested by reading documentation and successfully implementing encrypted permission storage in a test plugin

**Acceptance Scenarios**:

1. **Given** MagicCode has PLUGIN_DEVELOPER_GUIDE.md, **When** encryption section is added, **Then** guide explains how to use PermissionStorage with encryption
2. **Given** MagicCode has ARCHITECTURE.md, **When** encryption section is added, **Then** document explains KeyManager, EncryptedStorageFactory architecture
3. **Given** MagicCode has TESTING_GUIDE.md, **When** encryption testing section is added, **Then** guide shows how to write tests for encrypted storage
4. **Given** developer reads updated documentation, **When** they implement encrypted permission storage, **Then** implementation works correctly on first try

---

### Edge Cases

- **What happens when** MagicCode has conflicting file names with VOS4's encryption files?
  - **Resolution**: Verify no naming conflicts exist before copy (EncryptedStorageFactory.kt, KeyManager.kt are unique to VOS4)

- **How does system handle** iOS/JVM platforms that don't have EncryptedSharedPreferences?
  - **Resolution**: Provide stub implementations that log warnings (encryption not available on non-Android platforms)

- **What happens when** VOS4's PluginSystem has bug fixes MagicCode doesn't?
  - **Resolution**: Perform line-by-line diff of all shared files, merge any VOS4-specific fixes to MagicCode first

- **How does system handle** MagicCode's 282 tests conflicting with VOS4's 8 tests?
  - **Resolution**: Merge test suites (no conflicts expected - VOS4 tests are for new encryption feature)

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST copy all 7 encryption files from VOS4 to MagicCode with correct package structure
- **FR-002**: System MUST copy all 8 encryption tests from VOS4 to MagicCode
- **FR-003**: System MUST update MagicCode build.gradle.kts to include androidx.security:security-crypto dependency
- **FR-004**: System MUST update MagicCode PluginLogger to include security() method for audit logging
- **FR-005**: System MUST verify all 290 tests pass (282 existing + 8 new) in MagicCode after merge
- **FR-006**: System MUST perform file-by-file diff to verify core API files match between repositories
- **FR-007**: System MUST update backup_rules.xml in MagicCode to exclude encrypted data from backups
- **FR-008**: System MUST provide iOS/JVM stub implementations for PermissionStorage (encryption not available)
- **FR-009**: System MUST update MagicCode documentation to include encryption architecture and usage
- **FR-010**: VOS4 MUST be able to depend on MagicCode PluginSystem via Gradle after synchronization
- **FR-011**: System MUST preserve all MagicCode's existing functionality (no regressions)
- **FR-012**: System MUST follow Precompaction Protocol at 90% context usage during synchronization

### Key Entities *(include if feature involves data)*

- **Encryption Files (7 entities)**:
  - KeyManager.kt - Hardware-backed key management
  - EncryptedStorageFactory.kt - EncryptedSharedPreferences wrapper
  - PermissionStorage.kt (expect) - Common API
  - PermissionStorage.kt (actual Android) - Encrypted implementation
  - EncryptionStatus.kt - Diagnostic data class
  - MigrationResult.kt - Migration result sealed class
  - Exceptions.kt - EncryptionException, MigrationException

- **Test Files (8 entities)**:
  - PermissionStorageEncryptionTest.kt - 5 encryption unit tests
  - PermissionStoragePerformanceTest.kt - 3 performance benchmarks
  - Integration tests (if created)

- **Documentation Files (4 entities)**:
  - PLUGIN_DEVELOPER_GUIDE.md - User guide for encryption
  - ARCHITECTURE.md - Technical architecture
  - TESTING_GUIDE.md - Testing strategies
  - README.md - Quick start

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: MagicCode compiles successfully with encrypted storage added (0 compilation errors)
- **SC-002**: All 290 tests pass in MagicCode (282 existing + 8 new encryption tests) with 80%+ coverage maintained
- **SC-003**: File diff shows 100% equality for all core PluginSystem API files between repositories
- **SC-004**: VOS4 successfully builds with MagicCode dependency (removing local PluginSystem module)
- **SC-005**: Encrypted storage performance meets requirements (<5ms latency per operation)
- **SC-006**: MagicCode documentation updated with 3 new sections covering encrypted storage (developer guide, architecture, testing)
- **SC-007**: Zero regressions in MagicCode's existing 282 tests after merge
- **SC-008**: Synchronization completed in <4 hours total (2 hours copy, 1 hour testing, 1 hour documentation)

---

## Implementation Strategy

### Phase 1: Pre-Merge Verification (30 min)
1. Run VOS4 encrypted storage tests - verify all 8 pass
2. Run MagicCode existing tests - verify all 282 pass (baseline)
3. Perform file-by-file diff of shared files - identify any VOS4 bug fixes to merge first

### Phase 2: Copy Encryption Stack to MagicCode (1 hour)
1. Copy 7 encryption files to correct KMP structure (androidMain, commonMain)
2. Update MagicCode build.gradle.kts with security dependency
3. Update PluginLogger.kt with security() method
4. Add backup_rules.xml to MagicCode Android target
5. Create iOS/JVM stub implementations for PermissionStorage

### Phase 3: Copy Tests to MagicCode (30 min)
1. Copy 8 encryption tests to androidInstrumentedTest
2. Update test build configuration if needed
3. Run all 290 tests - verify all pass

### Phase 4: Verify Repository Equality (30 min)
1. Run automated diff script comparing core files
2. Manually verify PluginManager, PluginLoader, PermissionStorage match
3. Document any remaining differences (expected: platform-specific code only)

### Phase 5: Update VOS4 Dependencies (1 hour)
1. Publish MagicCode PluginSystem to local Maven or project dependency
2. Update VOS4 build.gradle.kts to depend on MagicCode
3. Remove VOS4's local PluginSystem module (or archive it)
4. Run VOS4 tests - verify all pass with MagicCode dependency

### Phase 6: Documentation (1 hour)
1. Update MagicCode PLUGIN_DEVELOPER_GUIDE.md with encryption section
2. Update ARCHITECTURE.md with encryption architecture
3. Update TESTING_GUIDE.md with encryption testing examples
4. Create migration guide for existing plugins

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| MagicCode tests fail after merge | High | Run baseline tests first, merge incrementally, rollback if failures |
| VOS4 has undocumented bug fixes | Medium | Perform thorough diff before merge, manual code review |
| iOS/JVM stub implementations insufficient | Low | Provide clear "not supported" errors, document Android-only feature |
| Documentation outdated or incorrect | Low | Review against actual implementation, test examples |

---

## Definition of Done

- [ ] All 7 encryption files copied to MagicCode with correct structure
- [ ] All 8 encryption tests copied to MagicCode
- [ ] All 290 MagicCode tests pass (282 + 8)
- [ ] File diff shows core API equality verified
- [ ] VOS4 builds successfully with MagicCode dependency
- [ ] MagicCode documentation updated with 3 new sections
- [ ] Precompaction Protocol followed if context reaches 90%
- [ ] Git commit created documenting synchronization
- [ ] Both repositories tested and verified equal

---

**Next Steps**: Run `/idea.plan` to create implementation plan, then `/idea.tasks` to generate task breakdown.
