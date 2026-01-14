# Clean Slate VUID/VoiceOSCoreNG Rebuild Plan

## Overview
Remove legacy migration code, consolidate duplicate files, and establish proper KMP architecture with platform-specific implementations.

## Current State Problems

| Issue | Location | Action |
|-------|----------|--------|
| VuidMigrator | Common/VUID/migration/ | DELETE - no legacy data |
| Duplicate VUIDGenerator | Common/VUID + VoiceOSCoreNG | CONSOLIDATE to VoiceOSCoreNG |
| Android-only source sets | VoiceOSCoreNG/src/ | ADD iosMain, desktopMain |
| No platform hashing | VUIDGenerator uses kotlin.random | ADD expect/actual for crypto |
| IDEACODE config conflict | .ideacode/config.idc | FIX merge conflict |

---

## Step 1: Remove VuidMigrator

**Files to DELETE:**
```
Common/VUID/src/commonMain/kotlin/com/augmentalis/vuid/migration/VuidFormat.kt
Common/VUID/src/commonMain/kotlin/com/augmentalis/vuid/migration/VuidMigrator.kt
Common/VUID/src/commonTest/kotlin/com/augmentalis/vuid/migration/VuidMigratorTest.kt
```

**Rationale:** Clean slate = no legacy data to migrate

---

## Step 2: Consolidate VUIDGenerator

**Decision:** Keep VoiceOSCoreNG version, deprecate Common/VUID version

**VoiceOSCoreNG VUIDGenerator location:**
```
Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt
```

**Common/VUID options:**
- Option A: DELETE Common/VUID module entirely
- Option B: Keep as thin wrapper that delegates to VoiceOSCoreNG

**Recommended:** Option B - maintains backward compatibility for other modules

---

## Step 3: Add KMP Source Sets

**Current structure:**
```
Modules/VoiceOSCoreNG/src/
├── androidMain/
├── androidUnitTest/
├── androidInstrumentedTest/
├── commonMain/
└── commonTest/
```

**Target structure:**
```
Modules/VoiceOSCoreNG/src/
├── commonMain/           # Shared logic
├── commonTest/           # Shared tests
├── androidMain/          # Android implementations
├── androidUnitTest/
├── androidInstrumentedTest/
├── iosMain/              # iOS implementations (NEW)
├── iosTest/              # iOS tests (NEW)
├── desktopMain/          # Desktop implementations (NEW)
└── desktopTest/          # Desktop tests (NEW)
```

---

## Step 4: Create expect/actual for Platform-Specific Code

### 4.1 Platform Hash Generation

**commonMain (expect):**
```kotlin
// Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../platform/PlatformCrypto.kt
expect object PlatformCrypto {
    fun sha256(input: String): ByteArray
    fun randomBytes(count: Int): ByteArray
}
```

**androidMain (actual):**
```kotlin
// Uses java.security.MessageDigest, java.security.SecureRandom
actual object PlatformCrypto {
    actual fun sha256(input: String): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    }
    actual fun randomBytes(count: Int): ByteArray {
        return ByteArray(count).also { SecureRandom().nextBytes(it) }
    }
}
```

**iosMain (actual):**
```kotlin
// Uses CommonCrypto via cinterop
actual object PlatformCrypto {
    actual fun sha256(input: String): ByteArray {
        // CryptoKit or CC_SHA256 via cinterop
    }
}
```

**desktopMain (actual):**
```kotlin
// Uses java.security (same as Android for JVM)
actual object PlatformCrypto {
    // Same as Android implementation
}
```

### 4.2 Update VUIDGenerator to Use PlatformCrypto

```kotlin
object VUIDGenerator {
    private fun generateHash(length: Int = 8): String {
        val bytes = PlatformCrypto.randomBytes(length / 2 + 1)
        return bytes.toHexString().take(length)
    }

    private fun hashPackage(packageName: String): String {
        return PlatformCrypto.sha256(packageName)
            .take(3)
            .toHexString()
    }
}
```

---

## Step 5: Fix IDEACODE Config

**File:** `.ideacode/config.idc`

**Action:** Resolve merge conflict, keep latest version

---

## Step 6: Update IDEACODE Registries

**Files to update:**
- `.ideacode/Registries/Modules.registry.json` - Add VoiceOSCoreNG module
- `.ideacode/Registries/FOLDER-REGISTRY.md` - Document new structure

**VoiceOSCoreNG module entry:**
```json
{
  "name": "VoiceOSCoreNG",
  "path": "Modules/VoiceOSCoreNG",
  "type": "library",
  "platforms": ["android", "ios", "desktop"],
  "sourcesets": {
    "commonMain": "Shared logic, AVU format, VUID generation",
    "androidMain": "Android accessibility integration",
    "iosMain": "iOS accessibility integration",
    "desktopMain": "Desktop accessibility (Cockpit)"
  }
}
```

---

## Step 7: Test and Verify

**Test commands:**
```bash
# VoiceOSCoreNG tests
./gradlew :Modules:VoiceOSCoreNG:test

# VUID tests (if keeping Common/VUID)
./gradlew :Common:VUID:test

# Full build verification
./gradlew assembleDebug
```

---

## Implementation Order

| Step | Priority | Estimate |
|------|----------|----------|
| 1. Remove VuidMigrator | High | 5 min |
| 2. Consolidate VUIDGenerator | High | 15 min |
| 3. Add KMP source sets | Medium | 10 min |
| 4. Create expect/actual | Medium | 30 min |
| 5. Fix IDEACODE config | Low | 5 min |
| 6. Update registries | Low | 10 min |
| 7. Test and verify | High | 10 min |

**Total:** ~85 minutes

---

## Files Summary

### DELETE (3 files)
- `Common/VUID/src/commonMain/kotlin/com/augmentalis/vuid/migration/VuidFormat.kt`
- `Common/VUID/src/commonMain/kotlin/com/augmentalis/vuid/migration/VuidMigrator.kt`
- `Common/VUID/src/commonTest/kotlin/com/augmentalis/vuid/migration/VuidMigratorTest.kt`

### CREATE (6 files)
- `Modules/VoiceOSCoreNG/src/commonMain/.../platform/PlatformCrypto.kt` (expect)
- `Modules/VoiceOSCoreNG/src/androidMain/.../platform/PlatformCrypto.kt` (actual)
- `Modules/VoiceOSCoreNG/src/iosMain/.../platform/PlatformCrypto.kt` (actual)
- `Modules/VoiceOSCoreNG/src/desktopMain/.../platform/PlatformCrypto.kt` (actual)
- `Modules/VoiceOSCoreNG/src/iosMain/.../functions/Platform.kt` (actual for time)
- `Modules/VoiceOSCoreNG/src/desktopMain/.../functions/Platform.kt` (actual for time)

### MODIFY (3 files)
- `Modules/VoiceOSCoreNG/src/commonMain/.../common/VUIDGenerator.kt`
- `.ideacode/config.idc`
- `.ideacode/Registries/Modules.registry.json`

---

**Author:** Claude
**Date:** 2025-12-31
**Version:** 1.0
