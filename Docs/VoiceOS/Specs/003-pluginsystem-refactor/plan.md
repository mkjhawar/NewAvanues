# Implementation Plan: PluginSystem Repository Synchronization

**Branch**: `003-pluginsystem-refactor` | **Date**: 2025-10-26 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-pluginsystem-refactor/spec.md`

## Summary

**Primary Requirement**: Merge VOS4's encrypted permission storage (7 files + 8 tests) to MagicCode PluginSystem to establish MagicCode as canonical library and VOS4 as consumer.

**Technical Approach**:
1. Copy encryption stack from VOS4 → MagicCode with correct KMP structure
2. Verify repository equality via automated file diff
3. Update VOS4 to depend on MagicCode PluginSystem
4. Synchronize documentation with encryption architecture

**Complexity**: Tier 3 (cross-repository synchronization, 4+ hours, architectural changes)

---

## Technical Context

**Language/Version**: Kotlin 1.9.25 + Java 17
**Primary Dependencies**:
- androidx.security:security-crypto:1.1.0-alpha06 (encryption)
- Kotlin Coroutines 1.7.3
- Room database (existing)
- JUnit 4 + MockK + Robolectric (testing)

**Storage**: EncryptedSharedPreferences (AES256-SIV/AES256-GCM) on Android, stubs on iOS/JVM

**Testing**:
- MagicCode: JUnit 5 + MockK (282 existing tests)
- VOS4: JUnit 4 + MockK + Robolectric (8 new encryption tests)
- Target: 290 total tests with 80%+ coverage

**Target Platform**:
- MagicCode: Kotlin Multiplatform (iOS 15+, Android API 29+, JVM 17)
- VOS4: Android API 29-34 only

**Project Type**: Library (MagicCode) + Consumer (VOS4) architecture

**Performance Goals**:
- Encryption/decryption latency: <5ms per operation (SC-002 from VOS4 spec)
- All 290 tests pass in <10s total
- No build time regression (MagicCode currently ~2min clean build)

**Constraints**:
- MUST maintain backward compatibility with MagicCode's 282 existing tests (zero regressions)
- MUST support KMP expect/actual pattern (Android implementation, iOS/JVM stubs)
- MUST preserve MagicCode's existing documentation structure
- MUST follow VOS4 Direct Implementation principle (no new interfaces unless strategic)

**Scale/Scope**:
- 2 repositories affected (MagicCode, VOS4)
- 7 files copied (KeyManager, EncryptedStorageFactory, PermissionStorage + 4 support classes)
- 8 tests copied (5 unit tests, 3 performance benchmarks)
- 4 documentation files updated (PLUGIN_DEVELOPER_GUIDE, ARCHITECTURE, TESTING_GUIDE, README)
- 105 existing Kotlin files in VOS4 PluginSystem
- 97 existing Kotlin files in MagicCode PluginSystem

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ I. Performance-First Architecture

**Status**: COMPLIANT

- Encryption latency <5ms meets <100ms command processing budget
- Hardware-backed keystore (StrongBox/TEE) provides best available performance
- Direct EncryptedSharedPreferences API (no abstraction layers)
- Performance tests included in VOS4 spec (PermissionStoragePerformanceTest.kt)

### ✅ II. Direct Implementation (No Interfaces)

**Status**: COMPLIANT

- No new interfaces introduced
- Uses concrete EncryptedSharedPreferences class directly
- KeyManager is object singleton (no interface)
- EncryptedStorageFactory is object with static methods (no interface)
- expect/actual pattern for KMP (NOT interfaces - this is platform-specific code)

### ✅ III. Privacy & Accessibility First

**Status**: COMPLIANT - **STRENGTHENS CONSTITUTION**

- Hardware-backed encryption protects permission grants from ADB extraction
- On-device encryption (no cloud dependency)
- Backup exclusion rules prevent encrypted data backup without keys (backup_rules.xml)
- Security audit logging for permission grants/revocations (FR-009 from VOS4 spec)
- This feature **enhances** privacy protection required by constitution

### ✅ IV. Modular Independence

**Status**: COMPLIANT

- PluginSystem remains self-contained module
- No new cross-module dependencies introduced
- Encryption files contained within PluginSystem/security/ package
- `com.augmentalis.magiccode.plugins.security` namespace (MANDATORY namespace compliance)
- Both repositories maintain independent versioning

### ✅ V. Quality Through Enforcement

**Status**: COMPLIANT

- 8 new encryption tests added (TDD approach - tests written first)
- All 290 tests (282 + 8) must pass before merge (BLOCKS on failure)
- @vos4-test-specialist auto-invoked during VOS4 implementation (completed)
- @vos4-documentation-specialist auto-invoked during VOS4 implementation (completed)
- IDE Loop followed during VOS4 implementation (Implement → Defend → Evaluate → Commit)

**Constitution Check Result**: ✅ **ALL GATES PASSED** - No violations, no exceptions needed

---

## Project Structure

### Documentation (this feature)

```text
specs/003-pluginsystem-refactor/
├── spec.md                    # Feature specification (COMPLETE)
├── plan.md                    # This file (IN PROGRESS)
├── research.md                # Phase 0 output (PENDING)
├── data-model.md              # Phase 1 output (PENDING)
├── quickstart.md              # Phase 1 output (PENDING)
├── contracts/                 # Phase 1 output (PENDING)
│   └── file-mapping.md       # VOS4 → MagicCode file mapping
└── tasks.md                   # Phase 2 output (/idea.tasks - NOT created by /idea.plan)
```

### Source Code (repository root)

**Two repositories affected:**

```text
# Repository 1: MagicCode (canonical library)
/Volumes/M Drive/Coding/magiccode/runtime/plugin-system/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/magiccode/plugins/
│   │   ├── core/
│   │   │   ├── PluginManager.kt          # Existing (97 files total)
│   │   │   ├── PluginLoader.kt
│   │   │   └── PluginLogger.kt           # UPDATE: Add security() method
│   │   └── security/                      # NEW DIRECTORY
│   │       ├── PermissionStorage.kt       # NEW: expect class
│   │       ├── EncryptionStatus.kt        # NEW: data class
│   │       ├── MigrationResult.kt         # NEW: sealed class
│   │       └── Exceptions.kt              # NEW: exception classes
│   │
│   ├── androidMain/kotlin/com/augmentalis/magiccode/plugins/security/
│   │   ├── PermissionStorage.kt           # NEW: actual implementation
│   │   ├── KeyManager.kt                  # NEW: key management
│   │   └── EncryptedStorageFactory.kt     # NEW: storage factory
│   │
│   ├── iosMain/kotlin/com/augmentalis/magiccode/plugins/security/
│   │   └── PermissionStorage.kt           # NEW: stub implementation
│   │
│   ├── jvmMain/kotlin/com/augmentalis/magiccode/plugins/security/
│   │   └── PermissionStorage.kt           # NEW: stub implementation
│   │
│   ├── commonTest/kotlin/                 # Existing 282 tests
│   │
│   └── androidInstrumentedTest/kotlin/com/augmentalis/magiccode/plugins/security/
│       ├── PermissionStorageEncryptionTest.kt      # NEW: 5 unit tests
│       └── PermissionStoragePerformanceTest.kt     # NEW: 3 benchmarks
│
├── docs/
│   ├── ARCHITECTURE.md                    # UPDATE: Add encryption section
│   ├── PLUGIN_DEVELOPER_GUIDE.md          # UPDATE: Add encryption usage
│   ├── TESTING_GUIDE.md                   # UPDATE: Add encryption testing
│   └── README.md                          # UPDATE: Mention encryption feature
│
└── build.gradle.kts                       # UPDATE: Add androidx.security dependency

# Repository 2: VOS4 (consumer application)
/Volumes/M Drive/Coding/vos4/modules/libraries/PluginSystem/
├── src/                                   # DELETE: Remove after migration
│   └── [105 Kotlin files]                 # To be replaced by MagicCode dependency
│
└── build.gradle.kts                       # UPDATE: Depend on MagicCode library
```

**Structure Decision**: Two-repository architecture with clear library-consumer relationship:
- **MagicCode** = Canonical source of truth (library)
- **VOS4** = Consumer application (depends on MagicCode via Gradle)

This follows VOS4 Modular Independence principle while enabling code reuse and centralized maintenance.

---

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Result**: No violations - this section is empty. All constitution gates passed without exceptions.

---

## Phase 0: Research & Unknowns

### Research Tasks

**R001: File-by-File Diff Analysis (NEEDS CLARIFICATION)**
- **Question**: Are there VOS4-specific bug fixes in core PluginSystem files (not encryption-related)?
- **Why**: Must merge any VOS4 improvements to MagicCode before synchronization
- **Method**: Line-by-line diff of shared files (PluginManager, PluginLoader, etc.)
- **Output**: List of files with differences + decision to merge or keep separate

**R002: MagicCode Test Framework Compatibility (NEEDS CLARIFICATION)**
- **Question**: Are VOS4's JUnit 4 tests compatible with MagicCode's JUnit 5 suite?
- **Why**: VOS4 uses JUnit 4 (AndroidJUnit4), MagicCode uses JUnit 5 for commonTest
- **Method**: Review MagicCode's test configuration, determine if JUnit 4 allowed for androidInstrumentedTest
- **Output**: Test migration strategy (keep JUnit 4 for Android tests, or convert to JUnit 5)

**R003: iOS/JVM Stub Implementation Strategy (NEEDS CLARIFICATION)**
- **Question**: What should iOS/JVM PermissionStorage stubs do (throw exception, log warning, no-op)?
- **Why**: Encryption not available on non-Android platforms
- **Method**: Review MagicCode's existing KMP patterns for platform-specific features
- **Output**: Stub implementation pattern for iOS/JVM (expect class methods)

**R004: Gradle Dependency Management (NEEDS CLARIFICATION)**
- **Question**: How should VOS4 depend on MagicCode (local project, Maven local, composite build)?
- **Why**: Need to establish dependency path for VOS4 → MagicCode integration
- **Method**: Review existing VOS4 build.gradle.kts for external dependencies
- **Output**: Gradle dependency configuration strategy

**R005: Documentation Merge Strategy (NEEDS CLARIFICATION)**
- **Question**: Should VOS4's EVALUATION-REPORT-Phase1-3.md be copied to MagicCode docs?
- **Why**: Contains comprehensive encryption implementation details and test results
- **Method**: Review MagicCode's documentation structure and decide where to integrate
- **Output**: Documentation integration plan

### Best Practices Research

**BP001: EncryptedSharedPreferences Best Practices**
- **Source**: Android Security Library documentation, AndroidX Security guides
- **Focus**: Key rotation, backup exclusion, error handling patterns
- **Output**: Validate VOS4 implementation follows Android best practices

**BP002: Kotlin Multiplatform expect/actual Patterns**
- **Source**: Kotlin KMP documentation, existing MagicCode patterns
- **Focus**: Platform-specific encryption, stub implementations for unsupported platforms
- **Output**: Validate expect/actual usage matches KMP conventions

**BP003: Test Migration Strategies (JUnit 4 → JUnit 5)**
- **Source**: JUnit documentation, MagicCode existing test patterns
- **Focus**: Annotation mapping, assertion library compatibility
- **Output**: Decision matrix for test framework compatibility

**BP004: Gradle Composite Builds for Local Dependencies**
- **Source**: Gradle documentation, multi-repo project patterns
- **Focus**: includeBuild vs local Maven publish
- **Output**: Recommended dependency management approach

---

## Phase 1: Design & Contracts

### Data Model

**Existing Entities** (no new data models - copying existing):

```kotlin
// Already implemented in VOS4, copying to MagicCode

data class EncryptionStatus(
    val isEncrypted: Boolean,
    val isHardwareBacked: Boolean,
    val encryptionScheme: String,
    val keyAlias: String,
    val migrationCompleted: Boolean,
    val migratedPermissionCount: Int
)

sealed class MigrationResult {
    data class Success(val migratedCount: Int, val timestamp: Long) : MigrationResult()
    data class Failure(val reason: String, val failedCount: Int, val exception: Throwable? = null) : MigrationResult()
    data class AlreadyMigrated(val count: Int, val timestamp: Long) : MigrationResult()
}

class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
class MigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

**Validation Rules**:
- EncryptionStatus.keyAlias must match KeyManager.MASTER_KEY_ALIAS
- MigrationResult.Success.migratedCount must be >= 0
- EncryptionException should wrap underlying crypto exceptions with context

**No new data models** - this is a synchronization task copying proven implementations.

### API Contracts

**Contract 1: File Mapping (VOS4 → MagicCode)**

| VOS4 Source Path | MagicCode Target Path | Notes |
|-----------------|----------------------|-------|
| `src/commonMain/.../security/PermissionStorage.kt` | `src/commonMain/.../security/PermissionStorage.kt` | expect class (API definition) |
| `src/commonMain/.../security/EncryptionStatus.kt` | `src/commonMain/.../security/EncryptionStatus.kt` | data class (diagnostics) |
| `src/commonMain/.../security/MigrationResult.kt` | `src/commonMain/.../security/MigrationResult.kt` | sealed class (migration result) |
| `src/commonMain/.../security/Exceptions.kt` | `src/commonMain/.../security/Exceptions.kt` | exception classes |
| `src/androidMain/.../security/PermissionStorage.kt` | `src/androidMain/.../security/PermissionStorage.kt` | actual implementation (410 lines) |
| `src/androidMain/.../security/KeyManager.kt` | `src/androidMain/.../security/KeyManager.kt` | object singleton (202 lines) |
| `src/androidMain/.../security/EncryptedStorageFactory.kt` | `src/androidMain/.../security/EncryptedStorageFactory.kt` | object factory (282 lines) |
| `src/androidInstrumentedTest/.../PermissionStorageEncryptionTest.kt` | `src/androidInstrumentedTest/.../PermissionStorageEncryptionTest.kt` | 5 unit tests (325 lines) |
| `src/androidInstrumentedTest/.../PermissionStoragePerformanceTest.kt` | `src/androidInstrumentedTest/.../PermissionStoragePerformanceTest.kt` | 3 benchmarks (pending) |
| `src/commonMain/.../core/PluginLogger.kt` | `src/commonMain/.../core/PluginLogger.kt` | ADD security() method |

**Contract 2: Build Configuration Changes**

**MagicCode build.gradle.kts:**
```kotlin
// Add to androidMain dependencies
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}

// Add androidInstrumentedTest source set (if not exists)
val androidInstrumentedTest by getting {
    dependencies {
        implementation("androidx.test:core:1.5.0")
        implementation("androidx.test.ext:junit:1.1.5")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    }
}
```

**VOS4 build.gradle.kts (after synchronization):**
```kotlin
dependencies {
    // Replace local PluginSystem module with MagicCode dependency
    implementation("com.augmentalis.magiccode:plugin-system:1.2.0") // Version TBD
    // OR: Use composite build
    // (configured in settings.gradle.kts)
}
```

**Contract 3: iOS/JVM Stub Interface**

```kotlin
// src/iosMain/kotlin/.../security/PermissionStorage.kt
actual class PermissionStorage {
    actual companion object {
        actual fun create(context: Any): PermissionStorage {
            throw UnsupportedOperationException(
                "Encrypted permission storage not available on iOS. " +
                "Use Android platform for encrypted storage."
            )
        }
    }

    // All methods throw UnsupportedOperationException with helpful message
}
```

---

## Phase 2: Implementation Phases

### Phase 2.1: Pre-Merge Verification (30 min)

**Objective**: Establish baseline and identify any VOS4-specific changes to merge first

**Tasks**:
1. Run VOS4 encrypted storage tests → verify all 8 pass (baseline)
2. Run MagicCode existing tests → verify all 282 pass (baseline)
3. Perform file-by-file diff of shared files (research.md findings)
4. Document any VOS4 bug fixes to merge before encryption stack

**Acceptance**:
- [ ] VOS4 tests: 8/8 passing
- [ ] MagicCode tests: 282/282 passing
- [ ] Diff report created with actionable findings
- [ ] Decision: merge VOS4 fixes first, or proceed directly to encryption copy

---

### Phase 2.2: Copy Encryption Stack to MagicCode (1.5 hours)

**Objective**: Copy all 7 encryption files + PluginLogger update to MagicCode with correct KMP structure

**Tasks**:
1. Create `src/commonMain/.../security/` directory in MagicCode
2. Copy 4 common files (PermissionStorage expect, EncryptionStatus, MigrationResult, Exceptions)
3. Create `src/androidMain/.../security/` directory
4. Copy 3 Android files (PermissionStorage actual, KeyManager, EncryptedStorageFactory)
5. Update `src/commonMain/.../core/PluginLogger.kt` with security() method
6. Create `src/iosMain/.../security/PermissionStorage.kt` stub (throw UnsupportedOperationException)
7. Create `src/jvmMain/.../security/PermissionStorage.kt` stub (throw UnsupportedOperationException)
8. Update MagicCode `build.gradle.kts` with androidx.security dependency
9. Create `app/src/main/res/xml/backup_rules.xml` (Android target)
10. Verify compilation: `./gradlew :plugin-system:compileKotlinAndroid`

**Acceptance**:
- [ ] All 7 files copied with correct package structure (`com.augmentalis.magiccode.plugins.security`)
- [ ] PluginLogger.kt updated with security() method
- [ ] iOS/JVM stubs created with helpful error messages
- [ ] build.gradle.kts updated with correct dependency
- [ ] backup_rules.xml created (exclude plugin_permissions_encrypted.xml)
- [ ] MagicCode compiles successfully (0 errors)

---

### Phase 2.3: Copy Tests to MagicCode (45 min)

**Objective**: Copy 8 encryption tests and verify all 290 tests pass

**Tasks**:
1. Create `src/androidInstrumentedTest/.../security/` directory
2. Copy PermissionStorageEncryptionTest.kt (5 tests, 325 lines)
3. Copy PermissionStoragePerformanceTest.kt (3 benchmarks, pending in VOS4)
4. Update MagicCode `build.gradle.kts` with androidInstrumentedTest dependencies (if needed)
5. Run instrumented tests: `./gradlew :plugin-system:connectedAndroidTest`
6. Run all existing tests: `./gradlew :plugin-system:test`
7. Verify coverage: 290 tests total (282 existing + 8 new)

**Acceptance**:
- [ ] 8 encryption tests copied to correct directory
- [ ] Test configuration updated (JUnit 4 allowed for Android instrumented tests)
- [ ] All 290 tests pass (282 + 8)
- [ ] No test regressions (all existing tests still pass)
- [ ] Test execution time <10s total

---

### Phase 2.4: Verify Repository Equality (30 min)

**Objective**: Automated verification that core PluginSystem APIs match between repositories

**Tasks**:
1. Create diff script in `specs/003-pluginsystem-refactor/contracts/verify-equality.sh`
2. Compare core API files:
   - PluginManager.kt
   - PluginLoader.kt
   - PermissionStorage.kt (expect class)
   - All public interfaces
3. Generate equality report in `contracts/equality-report.md`
4. Document expected differences (platform-specific code, test frameworks)
5. Manual verification of API signatures

**Acceptance**:
- [ ] Diff script created and executable
- [ ] Equality report shows 100% match for core APIs
- [ ] Expected platform differences documented
- [ ] Manual verification confirms API compatibility
- [ ] Report saved to specs/003-pluginsystem-refactor/contracts/

---

### Phase 2.5: Update VOS4 Dependencies (1 hour)

**Objective**: Replace VOS4's local PluginSystem module with MagicCode dependency

**Tasks**:
1. Decide Gradle dependency strategy (research.md findings)
   - Option A: Composite build (includeBuild in settings.gradle.kts)
   - Option B: Local Maven publish (publishToMavenLocal)
   - Option C: Direct project dependency
2. Implement chosen strategy in VOS4 build.gradle.kts
3. Remove/archive VOS4's local PluginSystem module
4. Run VOS4 build: `./gradlew :app:assembleDebug`
5. Run VOS4 tests: `./gradlew :app:test`
6. Verify all 8 encryption tests still pass with MagicCode dependency

**Acceptance**:
- [ ] Gradle dependency strategy implemented
- [ ] VOS4 builds successfully with MagicCode dependency
- [ ] VOS4's local PluginSystem module removed (or moved to archive/)
- [ ] All VOS4 tests pass (8 encryption tests + existing tests)
- [ ] Runtime verification: VOS4 app launches and PluginSystem works

---

### Phase 2.6: Documentation Synchronization (1 hour)

**Objective**: Update MagicCode documentation with encryption architecture and usage guides

**Tasks**:
1. Update `PLUGIN_DEVELOPER_GUIDE.md`:
   - Add "Encrypted Permission Storage" section (usage examples)
   - Add code examples for PermissionStorage API
   - Add migration guide for existing plugins
2. Update `ARCHITECTURE.md`:
   - Add encryption architecture diagram (KeyManager → EncryptedStorageFactory → PermissionStorage)
   - Add hardware keystore fallback logic diagram (StrongBox → TEE → Software)
   - Add security considerations section
3. Update `TESTING_GUIDE.md`:
   - Add encryption testing examples (unit tests, performance benchmarks)
   - Add mock/stub strategies for PermissionStorage in tests
4. Update `README.md`:
   - Add encryption feature to feature list
   - Add security dependency to installation instructions

**Acceptance**:
- [ ] PLUGIN_DEVELOPER_GUIDE.md updated with 2+ usage examples
- [ ] ARCHITECTURE.md updated with 2 diagrams (architecture + fallback logic)
- [ ] TESTING_GUIDE.md updated with encryption testing section
- [ ] README.md mentions encrypted permission storage
- [ ] All documentation follows MagicCode's existing style

---

### Phase 2.7: Final Verification & Commit (30 min)

**Objective**: Verify synchronization complete and commit changes to both repositories

**Tasks**:
1. Run full test suite in MagicCode: `./gradlew check`
2. Run full test suite in VOS4: `./gradlew check`
3. Verify performance benchmarks meet requirements (<5ms latency)
4. Create commit in MagicCode:
   - `feat(PluginSystem): Add hardware-backed encrypted permission storage`
   - Stage docs → code → tests separately
5. Create commit in VOS4:
   - `refactor(PluginSystem): Migrate to MagicCode library dependency`
   - Document removal of local module
6. Update specs/003-pluginsystem-refactor/COMPLETION-REPORT.md

**Acceptance**:
- [ ] All 290 MagicCode tests pass
- [ ] All VOS4 tests pass with MagicCode dependency
- [ ] Performance benchmarks meet <5ms requirement
- [ ] MagicCode commit created (follows conventional commits)
- [ ] VOS4 commit created (documents migration)
- [ ] COMPLETION-REPORT.md created with summary

---

## Success Criteria Mapping

**From spec.md:**

- **SC-001**: MagicCode compiles successfully → Phase 2.2 acceptance
- **SC-002**: All 290 tests pass → Phase 2.3 acceptance
- **SC-003**: File diff shows 100% equality → Phase 2.4 acceptance
- **SC-004**: VOS4 builds with MagicCode dependency → Phase 2.5 acceptance
- **SC-005**: Performance meets <5ms latency → Phase 2.7 acceptance (benchmarks)
- **SC-006**: Documentation updated (3 sections) → Phase 2.6 acceptance
- **SC-007**: Zero regressions → Phase 2.3 acceptance (282 tests still pass)
- **SC-008**: Completed in <4 hours → Total time budget across all phases

---

## Risk Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| MagicCode tests fail after merge | Medium | High | Run baseline tests first (Phase 2.1), merge incrementally, rollback if failures |
| JUnit 4/5 incompatibility | Low | Medium | Keep JUnit 4 for androidInstrumentedTest (Android standard), separate from commonTest |
| VOS4 has undocumented bug fixes | Medium | Medium | Thorough diff in Phase 2.1, merge any fixes before encryption copy |
| iOS/JVM stub insufficient | Low | Low | Clear error messages, document Android-only in README |
| Gradle dependency issues | Medium | High | Test all 3 strategies (composite, Maven, direct), choose simplest working approach |

---

## Definition of Done

- [ ] All Phase 2.1-2.7 acceptance criteria met
- [ ] Constitution Check re-verified (still passing)
- [ ] All 290 MagicCode tests pass (282 + 8)
- [ ] All VOS4 tests pass with MagicCode dependency
- [ ] File diff shows core API equality
- [ ] Documentation updated (4 files: PLUGIN_DEVELOPER_GUIDE, ARCHITECTURE, TESTING_GUIDE, README)
- [ ] Commits created in both repositories (conventional commit format)
- [ ] COMPLETION-REPORT.md created with synchronization summary
- [ ] Precompaction Protocol followed if context reached 90%

---

**Next Step**: Run `/idea.tasks` to generate task breakdown from this implementation plan.

**Note**: This plan is ready for task generation. All NEEDS CLARIFICATION items will be resolved in Phase 0 (research.md) before implementation begins.
