# VOS4 Known Issues

**Last Updated:** 2025-10-24

---

## Active Bugs

### [P1] Gradle Test Execution Blocker - LearnApp Tests Won't Run

**Severity**: P1 (High Priority - Blocks Test Coverage Verification)
**Status**: Active
**Affects**: LearnApp module test execution (tests compile but won't run)
**Discovered**: 2025-10-24 (Phase 1 of LearnApp Widget Migration)
**Module**: LearnApp

**Description:**
Phase 1 tests for LearnApp widget migration compile successfully but Gradle consistently marks them as SKIPPED. Cannot execute 26 tests required to verify 80%+ coverage (mandatory per IDE Loop).

**Affected Test Files:**
1. `WidgetOverlayHelperTest.kt` - 8 tests (SKIPPED)
2. `ProgressOverlayTest.kt` - 10 tests (SKIPPED)
3. `ProgressOverlayManagerTest.kt` - 8 tests (SKIPPED)

**Impact:**
- ❌ Blocks test coverage verification for Phase 1
- ❌ Cannot verify 80%+ coverage requirement (IDE Loop MANDATORY)
- ✅ Tests compile successfully with zero errors
- ✅ Production code compiles and builds successfully
- ✅ Implementation verified functional through manual QA

**Reproduction:**
```bash
./gradlew :modules:apps:LearnApp:testDebugUnitTest
# Result: Task SKIPPED, 0 tests executed
```

**Attempted Fixes:**
1. ✅ Moved tests from `/test/kotlin/` to `/test/java/` (no change)
2. ✅ Updated JVM target from 1.8 to 11 (no change)
3. ✅ Added missing test dependencies (Robolectric, MockK) (no change)
4. ✅ Clean builds with `--rerun-tasks` (no change)
5. ✅ Debug with `--debug` and `--info` flags (no useful output)

**Current Workaround**: Manual QA on device/emulator to verify functionality. Tests remain as documentation of expected behavior.

**Next Steps:**
1. Investigate Android Studio/IntelliJ test runner configuration
2. Check for test source set configuration issues in `build.gradle.kts`
3. Try running tests from Android Studio directly (not CLI)
4. Review Gradle test task configuration
5. Consider moving to androidTest if unit test execution cannot be resolved

**Related Issues:**
- Spec: `/Volumes/M Drive/Coding/vos4/specs/001-learnapp-widget-migration/spec.md`
- Phase 1 Tasks: `/Volumes/M Drive/Coding/vos4/specs/001-learnapp-widget-migration/tasks.md`

---

### [P1] Test Infrastructure Compilation Errors (4 Errors)

**Severity**: P1 (High Priority - Blocks Test Compilation)
**Status**: Active
**Affects**: Test compilation only (NOT production code)
**Discovered**: October 2025
**Module**: Testing Utilities

**Description:**
Four compilation errors in test infrastructure utilities are blocking test compilation/execution. These are NOT in production code or test content, but in the shared test utility classes used across multiple test suites.

**Affected Files:**
1. `SideEffectComparator.kt` - Utility for comparing side effects in tests
2. `StateComparator.kt` - Utility for comparing state changes in tests
3. `TimingComparator.kt` - Utility for comparing timing in tests
4. (Fourth error location to be confirmed during investigation)

**Impact:**
- ❌ Blocks test compilation
- ❌ Prevents running test suites that depend on these utilities
- ✅ Does NOT affect production code compilation
- ✅ Does NOT block production builds
- ✅ All production modules compile successfully with zero warnings

**Reproduction:**
```bash
./gradlew test
# Error: Compilation failures in test infrastructure
```

**Workaround**: None currently. Tests cannot be run until resolved.

**Next Steps:**
1. Investigate exact compilation error messages
2. Check git history for recent changes to affected files
3. Verify test framework versions (JUnit 5, MockK compatibility)
4. Fix type mismatches or missing dependencies
5. Verify all test suites compile successfully
6. Run full test suite to ensure 80%+ coverage maintained

---

## Recently Fixed Bugs

### [FIXED] VoiceCursor Cursor Type Persistence Issue

**Severity**: P2 (Medium)
**Status**: ✅ FIXED (2025-10-19)
**Module**: VoiceCursor

**Description**: Cursor type preference was not persisting across app restarts, reverting to default cursor type instead of user's selected type.

**Root Cause**: SharedPreferences key mismatch between write and read operations.

**Fix**: Corrected SharedPreferences key constants to match across persistence layer.

---

### [FIXED] VoiceRecognition Hilt DI Configuration False Alarm

**Severity**: P3 (Low - False Alarm)
**Status**: ✅ VERIFIED CORRECT (2025-10-19)
**Module**: VoiceRecognition

**Description**: Automated analysis repeatedly flagged VoiceRecognition Hilt DI configuration as incorrect.

**Root Cause**: False alarm - automated analysis tools missed Hilt's annotation processing. Configuration was actually correct all along.

**Resolution**: Verified configuration is correct through manual review and runtime testing. No changes needed.

**Lesson Learned**: AI/automated analysis can have false positives. Always verify against git history and runtime behavior.

---

### [FIXED] LearnApp False "0% Functional" Claim

**Severity**: P3 (Low - Documentation Issue)
**Status**: ✅ VERIFIED INCORRECT (2025-10-23)
**Module**: LearnApp, Analysis Process

**Description**: Code analysis claimed LearnApp was "0% functional" and needed complete implementation.

**Root Cause**: Analysis error - LearnApp was actually fully integrated since October 8, 2025.

**Resolution**: Verified LearnApp is fully functional with ConsentDialogManager correctly implemented, threading issues resolved, and integration verified in VoiceOSCore.

---

## Severity Definitions

**P0 (Critical)**: Production blocking, crashes, data loss, security issues
**P1 (High)**: Development blocking, CI/CD blocking, performance degradation >20%
**P2 (Medium)**: Non-critical features broken, minor performance issues, UI/UX issues
**P3 (Low)**: Cosmetic issues, minor inconveniences, documentation issues, false alarms

---

**End of Known Issues**
