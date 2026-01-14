# Implementation Plan: VUID to AVID Migration

**Date:** 2026-01-14 | **Version:** V1 | **Branch:** Refactor-VUID

---

## Executive Summary

Complete migration from VUID (Voice Unique ID) to AVID (Avanues Voice ID) system. This involves:
1. Flattening the AVID module folder structure (remove redundant `core/` subfolder)
2. Adding VUIDGenerator-compatible API methods to AvidGenerator
3. Updating all source files that reference VUID to use AVID

---

## Chain-of-Thought Reasoning (COT)

### Step 1: Current Problem Analysis

**Compile Errors in VoiceOSAccessibilityService.kt:**
```
Unresolved reference: VUIDGenerator
Unresolved reference: VUIDTypeCode
```

**Root Cause:**
- `VUIDGenerator` and `VUIDTypeCode` classes don't exist in `com.augmentalis.voiceoscoreng.common`
- They were removed but callers weren't updated to use the new `com.augmentalis.avid.core.AvidGenerator`

### Step 2: Folder Structure Issue

**Current (Redundant):**
```
Modules/AVID/src/commonMain/kotlin/com/augmentalis/avid/
├── AvidGenerator.kt         ← Package-level (simple format)
├── Platform.kt
├── TypeCode.kt
├── Fingerprint.kt
└── core/
    └── AvidGenerator.kt     ← Duplicate in core/ (full featured)
```

**Target (Flat):**
```
Modules/AVID/src/commonMain/kotlin/com/augmentalis/avid/
├── AvidGenerator.kt         ← Merged, full-featured
├── Platform.kt
├── TypeCode.kt
└── Fingerprint.kt
```

### Step 3: API Compatibility Gap

**Old VUIDGenerator API (Used by callers):**
```kotlin
VUIDGenerator.getTypeCode(className: String): String
VUIDGenerator.generate(packageName: String, typeCode: String, hash: String): String
```

**New AvidGenerator API (Available):**
```kotlin
AvidGenerator.TypeAbbrev.fromTypeName(typeName: String): String
AvidGenerator.generateCompact(packageName: String, version: String, typeName: String, elementHash: String?): String
```

**Gap:** Signature mismatch - need compatibility methods

---

## Tree-of-Thought Analysis (TOT)

### Approach A: Add Compatibility Layer
- Add `getTypeCode()` and `generate(pkg, type, hash)` methods to AvidGenerator
- Callers can use familiar API
- **Selected** - least disruption, maintains compatibility

### Approach B: Update All Callers
- Change every call site to new API
- More invasive, higher risk
- **Rejected** - unnecessary churn

### Approach C: Create Type Alias
- `typealias VUIDGenerator = AvidGenerator`
- Simple but doesn't solve method signature issue
- **Rejected** - method signatures differ

---

## Affected Files Inventory

### Phase 1: Flatten AVID Module (6 files)

| File | Action |
|------|--------|
| `src/commonMain/kotlin/com/augmentalis/avid/core/AvidGenerator.kt` | MERGE into parent → DELETE folder |
| `src/commonMain/kotlin/com/augmentalis/avid/AvidGenerator.kt` | Merge content from core/, add compat methods |
| `src/androidMain/kotlin/com/augmentalis/avid/core/AvidGeneratorAndroid.kt` | MOVE to parent |
| `src/iosMain/kotlin/com/augmentalis/avid/core/AvidGeneratorIos.kt` | MOVE to parent |
| `src/desktopMain/kotlin/com/augmentalis/avid/core/AvidGeneratorDesktop.kt` | MOVE to parent |

### Phase 2: Add VUID Compatibility Methods

**Add to AvidGenerator.kt:**
```kotlin
// VUID Compatibility Layer
companion object {
    @Deprecated("Use TypeAbbrev.fromTypeName()", ReplaceWith("TypeAbbrev.fromTypeName(className)"))
    fun getTypeCode(className: String): String = TypeAbbrev.fromTypeName(className)

    /**
     * Generate AVID using VoiceOS-style parameters
     * Compatibility method for VUIDGenerator.generate(packageName, typeCode, hash)
     */
    fun generate(packageName: String, typeCode: String, hash: String, version: String = "1.0.0"): String {
        val reversedPkg = reversePackage(packageName)
        return "$reversedPkg:$version:$typeCode:${hash.take(8).lowercase()}"
    }
}
```

### Phase 3: Update Source Files (15 files)

#### VoiceOSCoreNG Android App (3 files)
| File | Changes |
|------|---------|
| `android/apps/voiceoscoreng/.../VoiceOSAccessibilityService.kt` | Update imports, change VUIDGenerator→AvidGenerator |
| `android/apps/voiceoscoreng/.../MainActivity.kt` | Update VUID references in UI strings |
| `android/apps/voiceoscoreng/.../AccessibilitySettingsActivity.kt` | Update VUID references |

#### VoiceOSCoreNG Module (12 files)
| File | Changes |
|------|---------|
| `Modules/VoiceOSCoreNG/src/commonMain/.../CommandRegistry.kt` | Update VUID→AVID in comments |
| `Modules/VoiceOSCoreNG/src/commonMain/.../CommandGenerator.kt` | Update imports, method calls |
| `Modules/VoiceOSCoreNG/src/commonMain/.../ElementFingerprint.kt` | Update VUID→AVID naming |
| `Modules/VoiceOSCoreNG/src/commonMain/.../TypePatternRegistry.kt` | Update VUID→AVID naming |
| `Modules/VoiceOSCoreNG/src/commonMain/.../handlers/UIHandler.kt` | Update imports |
| `Modules/VoiceOSCoreNG/src/commonMain/.../handlers/ActionCoordinator.kt` | Update imports |
| `Modules/VoiceOSCoreNG/src/commonMain/.../learnapp/JITLearner.kt` | Update VUID→AVID |
| `Modules/VoiceOSCoreNG/src/commonMain/.../jit/JitProcessor.kt` | Update VUID→AVID |
| `Modules/VoiceOSCoreNG/src/commonTest/.../CommandGeneratorTest.kt` | Update test imports |
| `Modules/VoiceOSCoreNG/src/commonTest/.../CommandRegistryTest.kt` | Update test imports |
| `Modules/VoiceOSCoreNG/src/commonTest/.../JITLearnerTest.kt` | Update test imports |
| `Modules/VoiceOSCoreNG/src/androidMain/.../AndroidUIExecutor.kt` | Update VUID→AVID |

### Phase 4: Update Gradle Dependencies (2 files)

| File | Change |
|------|--------|
| `android/apps/voiceoscoreng/build.gradle.kts` | Add `implementation(project(":Modules:AVID"))` |
| `Modules/VoiceOSCoreNG/build.gradle.kts` | Add `implementation(project(":Modules:AVID"))` if missing |

### Phase 5: Update Settings (1 file)

| File | Change |
|------|--------|
| `settings.gradle.kts` | Verify `:Modules:AVID` is included |

### Phase 6: Rename Data Classes (1 file)

| File | Change |
|------|--------|
| `VoiceOSAccessibilityService.kt` | Rename `VUIDInfo` → `AvidInfo`, `vuids` → `avids` |

---

## Implementation Phases

### Phase 1: Flatten AVID Module Structure

**Tasks:**
1. Merge `core/AvidGenerator.kt` content into parent `AvidGenerator.kt`
2. Move platform-specific files from `core/` to parent directory
3. Update package declarations from `com.augmentalis.avid.core` to `com.augmentalis.avid`
4. Delete empty `core/` directories
5. Verify build: `./gradlew :Modules:AVID:build`

### Phase 2: Add VUID Compatibility Methods

**Tasks:**
1. Add `getTypeCode(className: String)` method (wrapper for TypeAbbrev.fromTypeName)
2. Add `generate(packageName, typeCode, hash)` method for VoiceOS compatibility
3. Add deprecation annotations pointing to new methods
4. Run tests: `./gradlew :Modules:AVID:allTests`

### Phase 3: Update VoiceOSAccessibilityService.kt

**Tasks:**
1. Change imports:
   - `com.augmentalis.voiceoscoreng.common.VUIDGenerator` → `com.augmentalis.avid.AvidGenerator`
   - `com.augmentalis.voiceoscoreng.common.VUIDTypeCode` → `com.augmentalis.avid.TypeCode`
2. Update method calls:
   - `VUIDGenerator.getTypeCode(...)` → `AvidGenerator.getTypeCode(...)`
   - `VUIDGenerator.generate(...)` → `AvidGenerator.generate(...)`
3. Rename data classes:
   - `VUIDInfo` → `AvidInfo`
   - Property `vuids` → `avids`
4. Update comments from VUID → AVID terminology

### Phase 4: Update VoiceOSCoreNG Module

**Tasks:**
1. Update all VUID references in common module
2. Update all VUID references in test files
3. Update all VUID references in platform-specific files
4. Run tests: `./gradlew :Modules:VoiceOSCoreNG:allTests`

### Phase 5: Update Gradle Dependencies

**Tasks:**
1. Add AVID dependency to android app if missing
2. Add AVID dependency to VoiceOSCoreNG if missing
3. Verify full build

### Phase 6: Verification

**Tasks:**
1. Full build: `./gradlew build`
2. Run all tests: `./gradlew allTests`
3. Verify no remaining VUID references in source files
4. Commit and push to `Refactor-VUID`

---

## Task Checklist (22 Tasks)

### Phase 1: Flatten AVID Module (5 tasks)
- [ ] 1.1 Merge core/AvidGenerator.kt into parent AvidGenerator.kt
- [ ] 1.2 Move androidMain/core/*.kt to androidMain/
- [ ] 1.3 Move iosMain/core/*.kt to iosMain/
- [ ] 1.4 Move desktopMain/core/*.kt to desktopMain/
- [ ] 1.5 Delete all core/ directories and verify build

### Phase 2: Add Compatibility Methods (3 tasks)
- [ ] 2.1 Add getTypeCode(className) compatibility method
- [ ] 2.2 Add generate(packageName, typeCode, hash) compatibility method
- [ ] 2.3 Verify AVID module tests pass

### Phase 3: Update VoiceOSAccessibilityService (4 tasks)
- [ ] 3.1 Update imports to use com.augmentalis.avid
- [ ] 3.2 Update VUIDGenerator calls to AvidGenerator
- [ ] 3.3 Rename VUIDInfo → AvidInfo data class
- [ ] 3.4 Update all VUID comments/strings

### Phase 4: Update VoiceOSCoreNG Module (5 tasks)
- [ ] 4.1 Update imports in common source files
- [ ] 4.2 Update imports in test files
- [ ] 4.3 Update VUID→AVID terminology in comments
- [ ] 4.4 Update property/variable names (vuid→avid)
- [ ] 4.5 Run VoiceOSCoreNG tests

### Phase 5: Update Gradle (2 tasks)
- [ ] 5.1 Add AVID dependency to android app
- [ ] 5.2 Verify VoiceOSCoreNG has AVID dependency

### Phase 6: Verification (3 tasks)
- [ ] 6.1 Full build passes
- [ ] 6.2 All tests pass
- [ ] 6.3 Grep confirms no remaining VUID references in source

---

## Migration Mapping

| Old (VUID) | New (AVID) |
|------------|------------|
| `VUIDGenerator` | `AvidGenerator` |
| `VUIDTypeCode` | `TypeCode` |
| `VUIDTypeCode.BUTTON` | `TypeCode.BUTTON` |
| `VUIDGenerator.getTypeCode(...)` | `AvidGenerator.getTypeCode(...)` |
| `VUIDGenerator.generate(...)` | `AvidGenerator.generate(...)` |
| `vuid` (variable) | `avid` |
| `VUIDInfo` (class) | `AvidInfo` |
| `targetVuid` (property) | `targetAvid` |

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing code | HIGH | Add compatibility methods |
| Missing import updates | MEDIUM | Use grep to find all references |
| Test failures | MEDIUM | Run tests after each phase |
| Build failures | HIGH | Incremental builds per phase |

---

## Summary

| Metric | Value |
|--------|-------|
| Directories to delete | 4 (core/ in each platform) |
| Files to merge | 1 |
| Files to move | 3 |
| Source files to update | ~15 |
| Gradle files to update | 2-3 |
| Total tasks | 22 |
| Estimated complexity | MEDIUM |

---

**Author:** Claude | **IDEACODE v18**
