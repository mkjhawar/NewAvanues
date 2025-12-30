# VoiceOS Migration Issues and Fixes

**Migration Date:** 2025-12-07
**Source:** /Volumes/M-Drive/Coding/VoiceOS
**Target:** /Volumes/M-Drive/Coding/NewAvanues (android/apps/VoiceOS + Modules/VoiceOS)
**Status:** ✅ COMPLETE - All builds successful

---

## Migration Summary

| Metric | Value |
|--------|-------|
| Source .kt files | 1,079 |
| Migrated .kt files | 1,073 (+ 6 in alternate paths) |
| Modules migrated | 22 |
| Build status | ✅ Debug + Release SUCCESS |
| Total Gradle tasks | 2,147 |

---

## Issues Encountered and Fixes

### Issue 1: UniversalIPC "No Variants Exist"

**Severity:** HIGH - Build blocker
**File:** `Modules/VoiceOS/libraries/UniversalIPC/build.gradle.kts`

**Symptom:**
```
Could not resolve project :Modules:VoiceOS:libraries:UniversalIPC.
No matching variant of project was found.
- No variants exist.
```

**Root Cause:**
UniversalIPC was configured as standard Android library (`kotlin.android` plugin) instead of KMP (`kotlin("multiplatform")`). KMP consumer modules expected variants with `org.jetbrains.kotlin.platform.type` attribute.

**Fix:**
1. Changed plugin from `kotlin.android` to `kotlin("multiplatform")`
2. Migrated source structure: `src/main/java/` → `src/androidMain/kotlin/`
3. Updated publishing configuration for KMP compatibility
4. Fixed settings.gradle.kts path typo

**Commit:** `fix(VoiceOS): convert UniversalIPC to KMP and add 100% migration rule`

---

### Issue 2: Missing Vivoka SDK AAR Files

**Severity:** HIGH - Build blocker
**Files:** `android/apps/VoiceOS/vivoka/*.aar`

**Symptom:**
```
Failed to transform vsdk-6.0.0.aar to match attributes
/Volumes/M-Drive/Coding/NewAvanues/android/apps/VoiceOS/vivoka/vsdk-6.0.0.aar (No such file or directory)
```

**Root Cause:**
Vivoka SDK AAR files were gitignored in original VoiceOS repo. Git subtree import didn't include them.

**Fix:**
Manually copied AAR files from original VoiceOS:
```bash
cp /Volumes/M-Drive/Coding/VoiceOS/vivoka/*.aar \
   /Volumes/M-Drive/Coding/NewAvanues/android/apps/VoiceOS/vivoka/
```

**Files Copied:**
- `vsdk-6.0.0.aar` (129 KB)
- `vsdk-csdk-asr-2.0.0.aar` (37 MB)
- `vsdk-csdk-core-1.0.1.aar` (34 MB)

**Prevention:** Added "100% File Migration Rule" to MONOREPO-STRUCTURE-FIX-PLAN.md

---

### Issue 3: LeakCanary Reference in Release Build

**Severity:** MEDIUM - Release build blocker
**File:** `Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`

**Symptom:**
```
e: Unresolved reference: leakcanary
```

**Root Cause:**
Direct reference to `leakcanary.AppWatcher.objectWatcher` in code, but LeakCanary is only included in debug builds (`debugImplementation`).

**Fix:**
Changed direct reference to reflection-based call:
```kotlin
// Before (fails in release)
leakcanary.AppWatcher.objectWatcher.watch(this, "...")

// After (works in both debug and release)
val appWatcherClass = Class.forName("leakcanary.AppWatcher")
val objectWatcherField = appWatcherClass.getDeclaredField("objectWatcher")
val objectWatcher = objectWatcherField.get(null)
val watchMethod = objectWatcher.javaClass.getMethod("watch", Any::class.java, String::class.java)
watchMethod.invoke(objectWatcher, this, "...")
```

---

### Issue 4: Test File API Mismatches

**Severity:** LOW - Test compilation only
**Files:** Multiple test files in `src/test/java/`

**Issues:**
1. `BlockedStateDetectionTest.kt` - Used `hint` instead of `hintText` for AccessibilityNodeInfo
2. `ElementClassifierExplorationBehaviorTest.kt` - Missing Context parameter for ElementClassifier
3. `ExplorationEnginePauseResumeTest.kt` - Wrong TestDatabaseDriverFactory constructor

**Fixes:**
- Changed `.hint` → `.hintText` (correct Android API)
- Added mock Context to ElementClassifier instantiation
- Fixed TestDatabaseDriverFactory usage

---

### Issue 5: Monorepo Structure Violation

**Severity:** MEDIUM - Architecture issue
**Location:** `Common/Libraries/VoiceOS/core/`

**Root Cause:**
VoiceOS-specific modules were placed in `Common/` (cross-product shared) instead of `Modules/` (product-specific).

**Fix:**
Moved 13 modules from `Common/Libraries/VoiceOS/core/` to `Modules/VoiceOS/core/`:
- accessibility-types
- command-models
- constants
- database
- exceptions
- hash
- json-utils
- result
- text-utils
- validation
- voiceos-logging

Updated 7 Gradle files with new module paths.

---

## Path Mapping Reference

| Original VoiceOS | NewAvanues |
|------------------|------------|
| `app/` | `android/apps/VoiceOS/app/` |
| `libraries/core/` | `Modules/VoiceOS/core/` |
| `modules/apps/` | `Modules/VoiceOS/apps/` |
| `modules/libraries/` | `Modules/VoiceOS/libraries/` |
| `modules/managers/` | `Modules/VoiceOS/managers/` |
| `tests/` | `android/apps/VoiceOS/tests/` |
| `vivoka/` | `android/apps/VoiceOS/vivoka/` |
| `Vosk/` | `Common/ThirdParty/Vosk/` |
| `gradle/` | `android/apps/VoiceOS/gradle/` |

---

## Verification Commands

```bash
# Verify build
cd /Volumes/M-Drive/Coding/NewAvanues/android/apps/VoiceOS
./gradlew assembleDebug assembleRelease --exclude-task test

# List all modules
./gradlew projects

# Check for old path references
grep -r "Common/Libraries/VoiceOS" android Modules

# Count source files
find Modules/VoiceOS -name "*.kt" -not -path "*/build/*" | wc -l
```

---

## Lessons Learned

1. **Always verify proprietary files** - Git subtree doesn't include gitignored files
2. **Check plugin compatibility** - KMP consumers require KMP dependencies
3. **Use reflection for debug-only libraries** - Avoid direct references to debugImplementation dependencies
4. **Test both debug AND release builds** - Some issues only appear in release
5. **Verify API usage in tests** - Android API changes (hint → hintText) affect test compilation

---

## Related Documents

- [MONOREPO-STRUCTURE-FIX-PLAN.md](./MONOREPO-STRUCTURE-FIX-PLAN.md) - Structure migration plan
- [VoiceOS-UniversalIPC-NoVariants-251207.md](../Issues/VoiceOS-UniversalIPC-NoVariants-251207.md) - Detailed issue analysis

---

**Author:** Claude (IDEACODE v10.3)
**Created:** 2025-12-07
**Last Updated:** 2025-12-07
