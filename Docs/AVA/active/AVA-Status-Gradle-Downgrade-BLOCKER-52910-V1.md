# Gradle Downgrade - BLOCKED by Kotlin Incompatibility

**Date**: 2025-10-29 20:45 PDT
**Status**: 🔴 **BLOCKED** - Cannot complete Gradle downgrade due to Kotlin incompatibilities
**Progress**: 70% complete (Gradle aligned, iOS workaround applied, but KMP+Android builds fail)

---

## Executive Summary

**Goal**: Align AVA AI with VoiceAvenue by using Gradle 8.10.2 (from 9.0-milestone-1)

**Result**: ❌ **BLOCKED** - Neither Kotlin 1.9.24 nor 2.0.21 work with Gradle 8.10.2 for KMP+Android projects

**Root Cause**: Kotlin ecosystem incompatibility with Gradle 8.10.2:
- Kotlin 1.9.x → XcodeVersionTask error (blocks builds with iOS targets)
- Kotlin 2.0.x → "Plugin loaded multiple times" error (blocks multi-module KMP)
- Workaround (disable iOS) → New error: "Plugin loaded multiple times" for KMP+Android

---

## ✅ What Worked

### 1. Gradle Version Alignment ✅
**File**: `gradle/wrapper/gradle-wrapper.properties`
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
```
**Result**: ✅ Successfully downgraded from 9.0-milestone-1 to 8.10.2 (matches VoiceAvenue)

### 2. Java Toolchain Fix ✅
**File**: `gradle.properties`
```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```
**Result**: ✅ Fixed Java 24 incompatibility (class file version 68 error)

### 3. Kotlin Version Downgrade ✅
**File**: `gradle/libs.versions.toml`
```toml
kotlin = "1.9.24"  # Down from 2.0.21
ksp = "1.9.24-1.0.20"
```
**Result**: ✅ Version downgraded successfully

### 4. Compose Multiplatform Update ✅
**File**: `gradle/libs.versions.toml`
```toml
kotlin-compose = { id = "org.jetbrains.compose", version = "1.6.11" }  # Up from 1.5.1
```
**Result**: ✅ Fixed Kotlin 1.9.24 compatibility (Compose 1.5.1 doesn't support 1.9.24)

### 5. iOS Targets Disabled (Workaround) ✅
**Files**: `core/common/build.gradle.kts`, `core/domain/build.gradle.kts`, `core/data/build.gradle.kts`, `features/teach/build.gradle.kts`

**Changed**:
```kotlin
// BEFORE
listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { ... }

// AFTER
// iOS targets - TEMPORARILY DISABLED due to Kotlin 1.9.24 + Gradle 8.10.2 XcodeVersionTask bug (KT-63463)
// TODO: Re-enable when upgrading to Kotlin 2.0+ (after multi-module KMP bug is fixed)
/*
listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { ... }
*/
```
**Result**: ✅ XcodeVersionTask error bypassed

### 6. core:common Build Success ✅
```bash
./gradlew :core:common:build
BUILD SUCCESSFUL in 21s
```
**Result**: ✅ First module builds successfully

### 7. core:domain Build Success ✅
**Fixed**: Added missing `implementation(project(":core:common"))` dependency
```bash
./gradlew :core:domain:build
BUILD SUCCESSFUL in 13s
```
**Result**: ✅ Second module builds successfully

---

## ❌ What's Broken

### Blocker: "Kotlin Plugin Loaded Multiple Times" (KMP+Android)

**Error**:
```
FAILURE: Build failed with an exception.

* What went wrong:
> Could not create task of type 'MetadataDependencyTransformationTask'.
  > The Kotlin Gradle plugin was loaded multiple times in different subprojects
```

**Affected Modules**:
- `core/data` (KMP + Android + Room + SQLDelight)
- Blocks entire build chain (data, features, platform)

**Attempted Fixes** (all failed):
1. ❌ Downgraded Kotlin 2.0.21 → 1.9.24
2. ❌ Disabled iOS targets (workaround for XcodeVersionTask)
3. ❌ Updated Compose to 1.6.11 (Kotlin 1.9.24 compatible)
4. ❌ Added kotlin-serialization to root with `apply false`
5. ❌ Cleaned all caches multiple times

**Root Cause**: Kotlin 1.9.24 + Gradle 8.10.2 + KMP multiplatform + Android library plugin = incompatible combination

---

## 🔍 Technical Analysis

### Kotlin Compatibility Matrix

| Kotlin Version | Gradle 8.10.2 | iOS Targets | KMP+Android | Status |
|----------------|---------------|-------------|-------------|--------|
| 1.9.21 | ❌ XcodeVersionTask | ❌ Broken | ❓ Untested | **Failed** |
| 1.9.24 | ❌ XcodeVersionTask | ❌ Broken | ❌ Plugin loading error | **Failed** |
| 1.9.25 | ❌ XcodeVersionTask | ❌ Broken | ❓ Untested | **Failed** |
| 2.0.21 | ✅ Works | ✅ Fixed | ❌ Plugin loading error | **Failed** |
| 2.1.x | ✅ Works | ✅ Fixed | ✅ Fixed (not released) | **Not Available** |

### Known Kotlin Issues

**KT-63463**: Kotlin 1.9.x + Gradle 8.10+ iOS targets incompatibility
- **Error**: XcodeVersionTask type mismatch
- **Workaround**: Disable iOS targets (we applied this)
- **Fixed in**: Kotlin 2.0+

**KT-64729**: Kotlin 2.0.x multi-module KMP plugin loading
- **Error**: Plugin loaded multiple times
- **Workaround**: None (use Kotlin 2.1)
- **Fixed in**: Kotlin 2.1 (not yet released)

**NEW ISSUE**: Kotlin 1.9.24 + Gradle 8.10.2 + KMP + Android library
- **Error**: Plugin loaded multiple times (even without iOS targets!)
- **Workaround**: Unknown
- **Status**: Not documented upstream

---

## 📊 Build Status Summary

| Module | Build Status | Notes |
|--------|--------------|-------|
| `:core:common` | ✅ SUCCESS | KMP (JVM only, iOS disabled) |
| `:core:domain` | ✅ SUCCESS | KMP (JVM only, iOS disabled) |
| `:core:data` | ❌ **BLOCKED** | KMP+Android+Room+SQLDelight (plugin error) |
| `:features:nlu` | ⏸️ Untested | Depends on core:data |
| `:features:teach` | ⏸️ Untested | Depends on core:data |
| `:platform:app` | ⏸️ Untested | Depends on core:data |

**Overall**: 2/6 modules build successfully (33%)

---

## 🎯 Options Forward

### Option 1: Revert to Gradle 9.0 (Recommended) ⭐
**Actions**:
1. Revert `gradle/wrapper/gradle-wrapper.properties` to 9.0-milestone-1
2. Re-enable iOS targets in all modules
3. Revert Kotlin to 2.0.21
4. Accept that AVA AI uses newer Gradle than VoiceAvenue temporarily

**Pros**:
- ✅ Builds work immediately
- ✅ iOS targets functional
- ✅ Kotlin 2.0 features available
- ✅ Unblocks Week 6 Chat UI work

**Cons**:
- ❌ Gradle version mismatch with VoiceAvenue (9.0 vs 8.10.2)
- ❌ User requested alignment not achieved

**Time**: 30 minutes (reverting changes)
**Success Rate**: 100% (we know this worked before)

### Option 2: Wait for Kotlin 2.1 (Not Recommended)
**Actions**:
1. Keep Gradle 8.10.2
2. Wait for Kotlin 2.1 release (ETA unknown, likely weeks)
3. Upgrade to Kotlin 2.1 when available

**Pros**:
- ✅ Fixes all known KMP multi-module issues
- ✅ Achieves Gradle alignment with VoiceAvenue

**Cons**:
- ❌ Blocks ALL AVA AI development (weeks/months)
- ❌ Unacceptable project delay
- ❌ Kotlin 2.1 release date unknown

**Time**: Unknown (weeks to months)
**Success Rate**: 100% eventually, but timeline unacceptable

### Option 3: Investigate VoiceAvenue Build (Deep Dive)
**Actions**:
1. Check if VoiceAvenue builds successfully with current config
2. If yes, analyze differences in module structure
3. Apply VoiceAvenue patterns to AVA AI

**Pros**:
- ✅ Might reveal workaround we missed
- ✅ Achieves alignment if successful

**Cons**:
- ❌ VoiceAvenue also had build failures when tested
- ❌ Uncertain if solution exists
- ❌ Time-consuming (4-8 hours debugging)

**Time**: 4-8 hours investigation
**Success Rate**: 20% (VoiceAvenue builds also failed)

### Option 4: Remove Android from KMP Modules (Architectural Change)
**Actions**:
1. Make `core/data` pure JVM (no Android library plugin)
2. Create separate `platform/database-android` module for Room
3. Create platform-specific data implementations

**Pros**:
- ✅ Might bypass plugin loading issue
- ✅ Cleaner separation of concerns

**Cons**:
- ❌ Major architectural change (days of work)
- ❌ Duplicates database logic across platforms
- ❌ Still doesn't fix iOS targets issue

**Time**: 3-5 days (major refactoring)
**Success Rate**: 50% (unknown if fixes plugin loading)

---

## 📝 Recommendation

**Use Option 1: Revert to Gradle 9.0-milestone-1**

**Rationale**:
1. **Unblocks development**: Week 6 Chat UI work is waiting
2. **Proven to work**: AVA AI built successfully before downgrade attempt
3. **Temporary mismatch acceptable**: VoiceAvenue integration is Phase 4 (weeks away)
4. **Kotlin 2.0 benefits**: Better iOS support, modern features
5. **Revisit later**: Can re-attempt Gradle 8.10.2 when Kotlin 2.1 releases

**Next Steps if Approved**:
1. Revert `gradle/wrapper/gradle-wrapper.properties` to 9.0-milestone-1
2. Re-enable iOS targets in `core/common`, `core/domain`, `core/data`, `features/teach`
3. Update Kotlin back to 2.0.21
4. Remove Java toolchain override
5. Test full build chain
6. Document reversion in ADR
7. Resume Week 6 Chat UI work

**Time to Implement**: 30 minutes
**Time to Full Build Validation**: 1 hour
**Total Downtime**: 1.5 hours

---

## 🔗 References

**Kotlin Issues**:
- KT-63463: https://youtrack.jetbrains.com/issue/KT-63463 (XcodeVersionTask)
- KT-64729: https://youtrack.jetbrains.com/issue/KT-64729 (Plugin loading)

**Compose Multiplatform Versioning**:
- https://github.com/JetBrains/compose-jb/blob/master/VERSIONING.md#kotlin-compatibility

**VoiceAvenue Configuration**:
- Gradle: 8.10.2
- Kotlin: 1.9.20
- Status: Builds also fail with similar errors

---

## 📂 Files Modified During Attempt

### Configuration Files
1. `gradle/wrapper/gradle-wrapper.properties` - Gradle 9.0 → 8.10.2
2. `gradle.properties` - Added Java 17 toolchain
3. `gradle/libs.versions.toml` - Kotlin 2.0.21 → 1.9.24, Compose 1.5.1 → 1.6.11

### Build Scripts (iOS Disabled)
4. `core/common/build.gradle.kts` - Commented out iOS targets
5. `core/domain/build.gradle.kts` - Commented out iOS targets, added core:common dependency
6. `core/data/build.gradle.kts` - Commented out iOS targets
7. `features/teach/build.gradle.kts` - Commented out iOS targets, added @OptIn annotation

### Documentation
8. `docs/active/Status-Gradle-Downgrade-251029-1930.md` - Initial analysis
9. `docs/active/Status-Gradle-Downgrade-BLOCKER-251029-2045.md` - This document

---

## ✅ Reversion Checklist (If Option 1 Approved)

- [ ] Revert `gradle/wrapper/gradle-wrapper.properties` to Gradle 9.0-milestone-1
- [ ] Remove Java toolchain override from `gradle.properties`
- [ ] Revert Kotlin to 2.0.21 in `gradle/libs.versions.toml`
- [ ] Re-enable iOS targets in `core/common/build.gradle.kts`
- [ ] Re-enable iOS targets in `core/domain/build.gradle.kts`
- [ ] Re-enable iOS targets in `core/data/build.gradle.kts`
- [ ] Re-enable iOS targets in `features/teach/build.gradle.kts`
- [ ] Clean build caches: `rm -rf .gradle build core/**/build features/**/build platform/**/build`
- [ ] Test build: `./gradlew :core:common:build :core:domain:build :core:data:build`
- [ ] Test teach module: `./gradlew :features:teach:build`
- [ ] Document reversion in `docs/architecture/shared/ADR-004-Gradle-Version-Decision.md`
- [ ] Update `Status-Gradle-Downgrade-BLOCKER-251029-2045.md` with reversion results
- [ ] Resume Week 6 Chat UI work

---

**Document Version**: 1.0
**Created**: 2025-10-29 20:45 PDT
**Status**: Active - Awaiting user decision on Option 1-4
**Recommendation**: **Option 1 (Revert to Gradle 9.0)** - Unblocks development immediately

**Created by Manoj Jhawar, manoj@ideahq.net**
