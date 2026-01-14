# VoiceOS Phase 3 - Folder Restructure Required

**Date:** 2025-12-06
**Issue:** Redundant folder naming violates IDEACODE principles
**Status:** ⚠️ **ACTION REQUIRED**

---

## Problem

Current structure uses redundant folder names:

```
❌ CURRENT (WRONG):
Common/Libraries/VoiceOS/core/
  ├── result/
  ├── hash/
  ├── constants/
  └── ... (11 modules)

✅ SHOULD BE:
Common/VoiceOS/
  ├── result/
  ├── hash/
  ├── constants/
  └── ... (11 modules)
```

**Violations:**
1. `Libraries/` is redundant - `Common/` already implies common/shared libraries
2. `core/` is redundant - all items under `VoiceOS/` are core libraries
3. Adds 2 unnecessary folder levels

---

## Impact

| Component | Current Path | Should Be | Files Affected |
|-----------|-------------|-----------|----------------|
| Result monad | `Common/Libraries/VoiceOS/core/result` | `Common/VoiceOS/result` | ~15 |
| Hash utils | `Common/Libraries/VoiceOS/core/hash` | `Common/VoiceOS/hash` | ~8 |
| Constants | `Common/Libraries/VoiceOS/core/constants` | `Common/VoiceOS/constants` | ~5 |
| Validation | `Common/Libraries/VoiceOS/core/validation` | `Common/VoiceOS/validation` | ~10 |
| Exceptions | `Common/Libraries/VoiceOS/core/exceptions` | `Common/VoiceOS/exceptions` | ~12 |
| Command models | `Common/Libraries/VoiceOS/core/command-models` | `Common/VoiceOS/command-models` | ~18 |
| Accessibility | `Common/Libraries/VoiceOS/core/accessibility-types` | `Common/VoiceOS/accessibility-types` | ~8 |
| Logging | `Common/Libraries/VoiceOS/core/voiceos-logging` | `Common/VoiceOS/voiceos-logging` | ~20 |
| Text utils | `Common/Libraries/VoiceOS/core/text-utils` | `Common/VoiceOS/text-utils` | ~12 |
| JSON utils | `Common/Libraries/VoiceOS/core/json-utils` | `Common/VoiceOS/json-utils` | ~10 |
| Database | `Common/Libraries/VoiceOS/core/database` | `Common/VoiceOS/database` | ~25 |
| **TOTAL** | **11 modules** | **11 modules** | **~143 files** |

---

## Required Changes

### 1. Move Folders
```bash
mv Common/Libraries/VoiceOS/core/* Common/VoiceOS/
rmdir Common/Libraries/VoiceOS/core
rmdir Common/Libraries/VoiceOS
rmdir Common/Libraries
```

### 2. Update Gradle Paths (settings.gradle.kts)
```kotlin
# OLD:
include(":Common:Libraries:VoiceOS:core:result")
project(":Common:Libraries:VoiceOS:core:result").projectDir =
  file("../../../Common/Libraries/VoiceOS/core/result")

# NEW:
include(":Common:VoiceOS:result")
project(":Common:VoiceOS:result").projectDir =
  file("../../../Common/VoiceOS/result")
```

**Files to update:** 11 `include()` + 11 `projectDir` = 22 lines

### 3. Update Module Dependencies (build.gradle.kts)
```kotlin
# OLD:
implementation(project(":Common:Libraries:VoiceOS:core:result"))

# NEW:
implementation(project(":Common:VoiceOS:result"))
```

**Files to update:**
- `android/apps/VoiceOS/app/build.gradle.kts`
- All 18 module `build.gradle.kts` files in `Modules/VoiceOS/`
- All 11 library `build.gradle.kts` files

**Total:** ~30 files

### 4. Update Import Statements (if any use paths)
Search for any hardcoded path references to `Common/Libraries/VoiceOS/core/`

---

## Migration Plan

### Step 1: Backup
```bash
git tag voiceos-phase2-backup
```

### Step 2: Move Folders
```bash
# Create new structure
mkdir -p Common/VoiceOS

# Move all modules
for module in result hash constants validation exceptions \
             command-models accessibility-types voiceos-logging \
             text-utils json-utils database; do
  mv "Common/Libraries/VoiceOS/core/$module" "Common/VoiceOS/"
done

# Remove empty folders
rmdir Common/Libraries/VoiceOS/core
rmdir Common/Libraries/VoiceOS
rmdir Common/Libraries
```

### Step 3: Update Gradle References
```bash
# Update settings.gradle.kts
sed -i.bak 's|:Common:Libraries:VoiceOS:core:|:Common:VoiceOS:|g' \
  android/apps/VoiceOS/settings.gradle.kts

sed -i.bak 's|Common/Libraries/VoiceOS/core/|Common/VoiceOS/|g' \
  android/apps/VoiceOS/settings.gradle.kts

# Update app build.gradle.kts
sed -i.bak 's|:Common:Libraries:VoiceOS:core:|:Common:VoiceOS:|g' \
  android/apps/VoiceOS/app/build.gradle.kts

# Update module build.gradle.kts files
find Modules/VoiceOS -name "build.gradle.kts" -exec \
  sed -i.bak 's|:Common:Libraries:VoiceOS:core:|:Common:VoiceOS:|g' {} \;

# Update library build.gradle.kts files
find Common/VoiceOS -name "build.gradle.kts" -exec \
  sed -i.bak 's|:Common:Libraries:VoiceOS:core:|:Common:VoiceOS:|g' {} \;
```

### Step 4: Test
```bash
cd android/apps/VoiceOS
./gradlew projects  # Verify all 33 projects recognized
./gradlew tasks     # Verify no errors
```

### Step 5: Commit
```bash
git add -A
git commit -m "refactor(voiceos): remove redundant folder levels

- Move Common/Libraries/VoiceOS/core/* → Common/VoiceOS/
- Remove redundant Libraries/ and core/ folder levels
- Update all Gradle module paths (33 modules)
- Update all build.gradle.kts dependencies (~30 files)
- Follows IDEACODE principle: no redundant folder names"
```

---

## Verification Checklist

- [ ] All 11 modules moved to `Common/VoiceOS/`
- [ ] Empty folders removed (`Libraries/`, `core/`)
- [ ] settings.gradle.kts updated (22 lines)
- [ ] app/build.gradle.kts updated
- [ ] All module build.gradle.kts updated (18 files)
- [ ] All library build.gradle.kts updated (11 files)
- [ ] `./gradlew projects` shows all 33 modules
- [ ] `./gradlew tasks` runs without errors
- [ ] No references to old paths remain

---

## Search Commands

```bash
# Find any remaining references to old path
grep -r "Libraries/VoiceOS" android/apps/VoiceOS/
grep -r "Libraries:VoiceOS" android/apps/VoiceOS/
grep -r "VoiceOS/core" Modules/
```

---

Updated: 2025-12-06 | IDEACODE v10.3
