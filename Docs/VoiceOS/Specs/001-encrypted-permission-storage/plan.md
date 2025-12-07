# Implementation Plan: Encrypted Permission Storage

**Branch**: `001-encrypted-permission-storage` | **Date**: 2025-10-26 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-encrypted-permission-storage/spec.md`

**Note**: This template is filled in by the `/idea.plan` command. See `.ideacode/templates/commands/plan.md` for the execution workflow.

## Summary

Implement hardware-backed encryption for PluginSystem permission grants using AndroidX Security library (EncryptedSharedPreferences) to protect sensitive permission data from unauthorized access via ADB, rooted devices, or backup exploitation. The implementation replaces plain-text SharedPreferences storage with AES256-GCM encrypted storage backed by Android Keystore (hardware TEE/TrustZone when available), includes automatic migration from existing plain-text permissions, and maintains <5ms latency for permission operations.

**Primary Requirements:**
- FR-001: Hardware-backed AES256-GCM encryption for all permission grants
- FR-004: Automatic migration from plain-text to encrypted storage
- FR-008: <5ms latency overhead for permission operations
- NFR-006: 80%+ test coverage for encryption paths

**Technical Approach:**
- Use androidx.security:security-crypto library (industry-standard, maintained by Google)
- MasterKey with AES256_GCM scheme stored in Android Keystore
- EncryptedSharedPreferences with AES256_SIV key encryption + AES256_GCM value encryption
- Idempotent migration on PermissionStorage initialization
- Comprehensive unit tests mocking hardware keystore scenarios

## Technical Context

**Language/Version**: Kotlin 1.9.25 + Java 17
**Primary Dependencies**:
- androidx.security:security-crypto:1.1.0-alpha06 (EncryptedSharedPreferences)
- Hilt 2.51.1 (dependency injection)
- Kotlin Coroutines + Flow (async operations)

**Storage**: EncryptedSharedPreferences (AES256-GCM) backed by Android Keystore
**Testing**: JUnit 4, MockK, Robolectric (for Android context mocking)
**Target Platform**: Android API 28+ (Android 9.0+, hardware keystore mandatory)
**Project Type**: Mobile (Android library module - PluginSystem)
**Performance Goals**: <5ms encryption/decryption latency per permission operation
**Constraints**:
- Hardware keystore required (TEE/TrustZone on API 28+)
- Encrypted data survives app restart but NOT device migration
- Keys cannot be exported or backed up (device-bound)
- Direct Boot mode requires special handling (pre-unlock state)

**Scale/Scope**: PluginSystem module only (~100 files, 30KB LOC), affecting 1 file primarily (PermissionStorage.kt)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Performance-First Architecture ✅ PASS

**Compliance:**
- Encryption latency target: <5ms per operation (well within <100ms command processing requirement)
- AndroidX Security library uses hardware-accelerated AES (TEE/TrustZone)
- No additional memory footprint beyond SharedPreferences baseline (~few KB for keys)
- Performance benchmarks included in test suite (NFR-006)

**Evidence:**
- SC-002: Benchmark tests comparing encrypted vs plain-text storage for 100 operations
- AES-GCM hardware acceleration on Android 9+ reduces overhead to <2ms typically
- Caching strategies prevent repeated decryption of same permissions

**Verdict:** PASS - Performance within budgets, benchmarks enforce constraint

---

### Principle II: Direct Implementation (No Interfaces) ✅ PASS

**Compliance:**
- Using concrete `EncryptedSharedPreferences` class directly (no interface abstraction)
- `PermissionStorage` remains concrete class (no `IPermissionStorage` interface)
- MasterKey and encryption schemes are concrete implementations from AndroidX library
- Zero custom abstraction layers added

**Evidence:**
- Current PermissionStorage.kt uses `SharedPreferences` interface (Android framework standard, acceptable)
- Replacement uses `EncryptedSharedPreferences` class (subclass of SharedPreferences, still concrete)
- No new interfaces created for encryption logic

**Justification for SharedPreferences interface:**
- Android framework interface (not project-created abstraction)
- Required for Android Context.getSharedPreferences() API
- Industry standard pattern (exception per Constitution II: "proven necessary")
- Zero performance impact (JIT optimizes interface calls)

**Verdict:** PASS - Direct implementation maintained, framework interfaces acceptable

---

### Principle III: Privacy & Accessibility First ✅ PASS

**Compliance:**
- Hardware-backed encryption protects sensitive permission data (ACCESSIBILITY_SERVICES grants)
- Keys stored in Keystore (cannot be extracted even with root access)
- Automatic migration preserves user permissions (no re-granting required)
- Encryption transparent to accessibility features (permissions remain functional)

**Evidence:**
- FR-002: Encryption keys exclusively in Android Keystore (hardware TEE/TrustZone)
- SC-001: Permission data unreadable via ADB without device unlock
- FR-004: Backward compatibility via automatic migration (no user friction)

**Accessibility Impact:**
- Zero impact on voice command flow (permission checks remain synchronous)
- No UI changes required (encryption is internal implementation detail)
- Error handling ensures permissions remain queryable even if encryption fails

**Verdict:** PASS - Enhances privacy without compromising accessibility

---

### Principle IV: Modular Independence ✅ PASS

**Compliance:**
- Changes contained entirely within PluginSystem module
- Zero cross-module dependencies added (AndroidX Security is external library, not VOS4 module)
- PermissionStorage API remains unchanged (public methods identical)
- Module builds and tests independently

**Evidence:**
- Modified files: 1 file (`PermissionStorage.kt`)
- New dependencies: 1 (androidx.security:security-crypto) - external, not internal module
- API compatibility: 100% (drop-in replacement, transparent encryption)
- Namespace: `com.augmentalis.magiccode.plugins.security` (compliant)

**Module Boundaries:**
- PluginSystem → AndroidX Security library (external, acceptable)
- No new dependencies on other VOS4 modules
- PermissionStorage consumers (PluginManager, etc.) require zero changes

**Verdict:** PASS - Module independence preserved, external library acceptable

---

### Principle V: Quality Through Enforcement ✅ PASS

**Compliance:**
- NFR-006: Unit tests with >90% code coverage for encryption paths (exceeds 80% requirement)
- @vos4-test-specialist will automatically enforce test quality (IDE Loop Defend phase)
- @vos4-documentation-specialist will update PluginSystem docs (already comprehensive)
- Zero compiler warnings enforced

**Testing Plan:**
- Unit tests: PermissionStorageEncryptionTest.kt (15+ test cases)
  - Encryption/decryption round-trip
  - Migration from plain-text to encrypted
  - Keystore failure scenarios
  - Concurrent access safety
  - Corruption detection
  - Performance benchmarks
- Integration tests: PluginManagerEncryptionIntegrationTest.kt
  - End-to-end permission grant with encryption
  - App restart persistence
  - Device reboot persistence (Robolectric limitation: mock only)

**Quality Gates:**
1. ✅ All tests passing (blocks merge if failed)
2. ✅ 90%+ coverage on PermissionStorage.kt
3. ✅ Performance benchmarks within <5ms budget
4. ✅ Zero compiler warnings
5. ✅ @vos4-test-specialist approval

**Verdict:** PASS - Quality gates comprehensive and enforceable

---

### Constitution Summary

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Performance-First | ✅ PASS | <5ms latency, hardware-accelerated AES |
| II. Direct Implementation | ✅ PASS | Concrete classes, framework interfaces acceptable |
| III. Privacy & Accessibility | ✅ PASS | Hardware encryption, zero accessibility impact |
| IV. Modular Independence | ✅ PASS | PluginSystem only, external library acceptable |
| V. Quality Through Enforcement | ✅ PASS | 90%+ coverage, test-specialist enforces |

**Overall Verdict:** ✅ ALL PRINCIPLES SATISFIED - Proceed to Phase 0 Research

---

## Project Structure

### Documentation (this feature)

```text
specs/001-encrypted-permission-storage/
├── spec.md              # Feature specification (COMPLETE)
├── plan.md              # This file (IN PROGRESS - /idea.plan output)
├── research.md          # Phase 0 output (NEXT)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output (usage guide)
├── contracts/           # Phase 1 output (API contracts)
│   └── PermissionStorage.kt  # Interface contract (even though we use concrete class, document API)
├── checklists/          # Requirements checklist (auto-generated)
│   └── requirements.md
└── tasks.md             # Phase 2 output (/idea.tasks command - NOT /idea.plan)
```

### Source Code (repository root)

```text
modules/libraries/PluginSystem/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/magiccode/plugins/
│   │   └── security/
│   │       └── PermissionStorage.kt        # MODIFY (add encryption)
│   │
│   ├── androidMain/kotlin/com/augmentalis/magiccode/plugins/
│   │   └── security/
│   │       ├── PermissionStorage.kt        # MODIFY (Android-specific encryption impl)
│   │       └── EncryptionMigrator.kt       # NEW (migration logic)
│   │
│   └── androidTest/kotlin/com/augmentalis/magiccode/plugins/
│       └── security/
│           ├── PermissionStorageEncryptionTest.kt       # NEW (unit tests)
│           ├── EncryptionMigratorTest.kt                # NEW (migration tests)
│           └── PermissionStoragePerformanceTest.kt      # NEW (benchmarks)
│
└── build.gradle.kts     # MODIFY (add androidx.security dependency)

docs/modules/PluginSystem/
├── Developer-Manual-PluginSystem-251026-1146.md         # UPDATE (add encryption section)
└── Whats-Missing-PluginSystem-251026-1146.md            # UPDATE (remove FR-043 TODO)
```

**Structure Decision:**
Android-specific implementation since EncryptedSharedPreferences is Android-only. The commonMain definition will use `expect/actual` pattern for KMP compatibility, with iOS/JVM returning NotImplementedError (already established pattern in PluginSystem for platform-specific features).

**Files Modified:** 2 files (PermissionStorage.kt common + android, build.gradle.kts)
**Files Created:** 4 files (EncryptionMigrator.kt, 3 test files)
**Documentation Updated:** 2 files (Developer Manual, What's Missing)

---

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**No violations detected** - All constitution principles satisfied without exceptions.

The `SharedPreferences` interface is an Android framework interface (not project-created abstraction), which is acceptable per Constitution Principle II exception clause: "When multi-implementation scenarios are proven necessary, interfaces can be added." Android framework requires this interface for Context.getSharedPreferences() API.

---

## Phase 0: Research & Investigation

**Status:** Pending execution by `/idea.plan` command

**Research Questions to Resolve:**

1. **AndroidX Security Library Best Practices**
   - Research: Official documentation for androidx.security:security-crypto
   - Decision needed: Optimal MasterKey configuration (AES256_GCM vs other schemes)
   - Decision needed: StrongBox usage on Android 9+ (hardware security module)
   - Decision needed: Handling of keystore generation failures

2. **Migration Strategy Patterns**
   - Research: Idempotent migration patterns for SharedPreferences
   - Decision needed: Atomic migration approach (all-or-nothing vs incremental)
   - Decision needed: Rollback strategy if migration fails mid-process
   - Decision needed: Detecting migration completion (flag storage mechanism)

3. **Performance Characteristics**
   - Research: Benchmark data for EncryptedSharedPreferences vs plain SharedPreferences
   - Decision needed: Acceptable latency overhead for VOS4 use case
   - Decision needed: Caching strategy for frequently accessed permissions
   - Decision needed: Background thread requirements for encryption operations

4. **Error Handling & Edge Cases**
   - Research: Keystore deletion scenarios (user clears credentials)
   - Research: Direct Boot mode behavior (pre-unlock encryption access)
   - Research: Backup/restore implications for encrypted data
   - Decision needed: Fail-secure vs fail-open approach for encryption failures

5. **Testing Strategies**
   - Research: Mocking Android Keystore in unit tests (Robolectric limitations)
   - Research: Hardware keystore availability detection in tests
   - Decision needed: Test pyramid allocation (unit vs integration vs E2E)
   - Decision needed: Performance benchmark methodology

**Output:** `research.md` with findings and decisions for each question above

---

## Phase 1: Data Model & API Contracts

**Status:** Pending (awaits Phase 0 completion)

**Data Model Entities:**

1. **Permission Grant** (already exists, format changes)
   - Current format: `{pluginId}.{permission} → {status}|{timestamp}`
   - New format: ENCRYPTED(`{pluginId}.{permission}` → `{status}|{timestamp}|{grantedBy}`)
   - Fields: pluginId, permission, status, timestamp, grantedBy (optional)

2. **Encryption Key** (new, stored in Android Keystore)
   - Key alias: `_plugin_permissions_master_key_`
   - Key scheme: MasterKey.KeyScheme.AES256_GCM
   - Key storage: Android Keystore (hardware TEE/TrustZone)
   - Attributes: device-bound, non-exportable, requires unlock

3. **Migration State** (new, tracks migration completion)
   - Storage: Encrypted SharedPreferences key `_migration_state_`
   - Fields: migrationCompleted (Boolean), migratedCount (Int), failedCount (Int), timestamp (Long)
   - Purpose: Idempotency (prevent re-migration on restart)

**API Contracts:**

```kotlin
// PermissionStorage API (unchanged - transparent encryption)
class PermissionStorage(private val context: Context) {
    // Existing methods remain identical (API compatibility)
    fun savePermission(pluginId: String, permission: Permission, status: GrantStatus)
    fun getPermission(pluginId: String, permission: Permission): GrantStatus?
    fun getAllPermissions(pluginId: String): Map<Permission, GrantStatus>
    fun revokePermission(pluginId: String, permission: Permission)
    fun clearAllPermissions(pluginId: String)

    // New methods for encryption management
    suspend fun migrateToEncrypted(): MigrationResult
    fun isEncrypted(): Boolean
    fun getEncryptionStatus(): EncryptionStatus
}

// New supporting types
sealed class MigrationResult {
    data class Success(val migratedCount: Int) : MigrationResult()
    data class Failure(val reason: String, val failedCount: Int) : MigrationResult()
}

data class EncryptionStatus(
    val isEncrypted: Boolean,
    val isHardwareBacked: Boolean,
    val migrationCompleted: Boolean,
    val keyAlias: String
)
```

**Output Files:**
- `data-model.md`: Entity definitions with relationships
- `contracts/PermissionStorage.kt`: API contract documentation
- `quickstart.md`: Usage guide for encrypted permissions

---

## Phase 2: Implementation Tasks

**Status:** Pending (generated by `/idea.tasks` command, NOT /idea.plan)

**Note:** Task breakdown will be created by running `/idea.tasks` after this plan is complete.

**Expected Task Categories:**
1. **Setup Tasks** (add dependencies, update gradle)
2. **Core Implementation** (modify PermissionStorage.kt for encryption)
3. **Migration Logic** (create EncryptionMigrator.kt)
4. **Testing Tasks** (unit tests, integration tests, benchmarks)
5. **Documentation Tasks** (update Developer Manual, What's Missing)
6. **Integration Tasks** (verify PluginManager compatibility)

**Estimated Effort:** 2-3 hours (matches P1 priority from gap analysis)

---

## Phase 3: Testing Strategy

**Test Pyramid:**
- 70% Unit Tests (~15 tests)
- 25% Integration Tests (~5 tests)
- 5% End-to-End Tests (~2 tests)

**Unit Test Coverage (PermissionStorageEncryptionTest.kt):**

1. `testEncryptionRoundTrip()` - Save encrypted, read decrypted matches original
2. `testMultiplePermissionsEncrypted()` - Batch operations with encryption
3. `testMigrationFromPlainToEncrypted()` - Automatic migration on init
4. `testMigrationIdempotency()` - Re-running migration doesn't duplicate
5. `testMigrationWithCorruptedData()` - Handles corrupted plain-text gracefully
6. `testEncryptionFailureFallback()` - Handles keystore failures gracefully
7. `testHardwareKeystoreDetection()` - Verifies hardware-backed keys
8. `testConcurrentPermissionGrants()` - Thread-safety of encryption
9. `testCorruptedEncryptedDataDetection()` - GCM authentication tag failure
10. `testKeyDeletion Recovery()` - Generates new key if keystore cleared
11. `testPerformanceBenchmark()` - <5ms latency verification (SC-002)
12. `testDirectBootMode()` - Pre-unlock behavior
13. `testBackupExclusion()` - Keys not included in backup
14. `testPermissionPersistenceAcrossRestart()` - App restart survival
15. `testZeroPlainTextAfterMigration()` - Plain-text file deleted (SC-006)

**Integration Test Coverage (PluginManagerEncryptionIntegrationTest.kt):**

1. `testEndToEndPermissionGrantWithEncryption()` - Full flow from PluginManager
2. `testPermissionQueryAfterAppRestart()` - Persistence verification
3. `testMultiplePluginsEncryptedSeparately()` - Plugin isolation
4. `testPermissionRevocationWithEncryption()` - Revoke encrypted permission
5. `testMigrationDuringActivePermissionCheck()` - Race condition handling

**Performance Benchmarks (PermissionStoragePerformanceTest.kt):**

1. `benchmarkEncryptionLatency()` - Measure 100 grant operations
2. `benchmarkDecryptionLatency()` - Measure 100 query operations
3. `compareEncryptedVsPlainPerformance()` - Verify <5ms overhead (SC-002)

**Acceptance Tests (Manual):**

1. SC-001: ADB unreadability verification (manual device test)
2. SC-004: Hardware keystore verification (check isInsideSecureHardware())
3. SC-008: Device reboot persistence (requires physical device, not emulator)
4. SC-010: Backup extraction test (Android backup, verify no plain-text)

---

## Phase 4: Deployment & Rollout

**Deployment Strategy:**

1. **Feature Flag** (optional): `ENCRYPTED_PERMISSIONS_ENABLED`
   - Allows gradual rollout to users
   - Defaults to `true` (encryption always enabled)
   - Can disable via build config for debugging

2. **Migration Timeline:**
   - First app launch after upgrade: Automatic migration triggered
   - Migration runs on background thread (non-blocking)
   - Migration result logged to PluginLog with tag "PermissionStorage"

3. **Rollback Plan:**
   - If encryption fails, fall back to plain-text with warning log
   - User notified via PluginManager error callback
   - Critical permissions still functional (fail-open for accessibility)

4. **Monitoring:**
   - Log migration success rate (SC-003 target: 100%)
   - Log encryption failures (investigate hardware keystore issues)
   - Monitor performance metrics (SC-002 target: <5ms)
   - Track ADB readability in security audits (SC-001)

**Success Metrics (from spec.md):**
- SC-001: Permission data unreadable via ADB (manual verification)
- SC-002: <5ms latency overhead (automated benchmark)
- SC-003: 100% migration success rate (automated test)
- SC-004: Hardware keystore usage (automated check)
- SC-005: Corruption detection (automated test)
- SC-006: Zero plain-text data post-migration (automated test)
- SC-007: Security event logging (automated test)
- SC-008: App restart persistence (automated test)
- SC-009: Concurrent grant safety (automated stress test)
- SC-010: Backup security (manual extraction test)

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Keystore generation fails on some devices | Low | High | Fallback to software keystore, warn user |
| Migration corrupts existing permissions | Low | Critical | Idempotent migration, backup plain-text before deletion |
| Performance exceeds 5ms budget | Medium | Medium | Hardware AES acceleration, caching, async operations |
| Tests fail on CI (no hardware keystore) | High | Low | Mock keystore in tests, Robolectric configuration |
| Direct Boot mode breaks permission queries | Medium | Medium | Cache permissions, return safe defaults pre-unlock |
| Backup/restore exposes encrypted data | Low | High | Verify android:allowBackup excludes keys |

---

## Next Steps

1. ✅ **Specification Complete** (`spec.md`) - DONE
2. ✅ **Implementation Plan Complete** (`plan.md`) - DONE (this file)
3. ⏭️ **Phase 0: Research** - Execute research questions, create `research.md`
4. ⏭️ **Phase 1: Design** - Create `data-model.md`, `contracts/`, `quickstart.md`
5. ⏭️ **Task Generation** - Run `/idea.tasks` to create `tasks.md`
6. ⏭️ **Implementation** - Run `/idea.implement` with IDE Loop (Implement → Defend → Evaluate → Commit)

**Current Status:** Plan complete, ready for Phase 0 research execution.

**Command to Continue:** This plan.md file is complete. The `/idea.plan` command will now execute Phase 0 (research) and Phase 1 (design) automatically.
