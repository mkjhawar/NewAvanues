# VoiceOS Build Migration - Root Cause Analysis

**Document Type:** Living Documentation - Root Cause Analysis
**Project:** VoiceOS
**Created:** 2025-12-16
**Status:** Active Investigation
**Severity:** CRITICAL - Blocking all builds

---

## Executive Summary

VoiceOS migration from standalone repository (`/voiceos`) to monorepo (`/NewAvanues`) has resulted in systematic build configuration corruption affecting **6 of 10 core KMP modules**. All affected modules exhibit identical corruption patterns caused by incomplete iOS target commenting, resulting in orphaned code blocks that break Kotlin DSL parsing.

**Impact:**
- 60% of core modules unbuildable
- Android apps cannot resolve dependencies
- Complete build system failure
- Estimated recovery time: 2-4 hours for manual fix, 30 minutes for automated fix

---

## Tree of Thought Analysis

### Phase 1: Hypothesis Generation

```
ROOT CAUSE HYPOTHESES
├── A. Configuration Corruption
│   ├── A1. iOS target commenting incomplete ⭐ HIGH PROBABILITY
│   ├── A2. Automated find/replace error
│   └── A3. Manual editing mistakes
├── B. Migration Artifacts
│   ├── B1. Git merge conflicts unresolved
│   ├── B2. Settings.gradle.kts misconfiguration
│   └── B3. Version catalog missing/broken
└── C. Gradle System Issues
    ├── C1. KMP plugin version mismatch
    ├── C2. Repository mode too strict (FAIL_ON_PROJECT_REPOS)
    └── C3. AGP/KGP incompatibility
```

### Phase 2: Evidence Collection

#### Hypothesis A1: iOS Target Commenting Incomplete ⭐ CONFIRMED

**Evidence:**
1. **Pattern Analysis** - All 6 affected files show identical corruption:
   ```kotlin
   // ORIGINAL (commit 18cfa4a7):
   val iosMain by creating {
       dependsOn(commonMain)
   }

   // CORRUPTED (current):
   //          val iosMain by creating {
   //              dependsOn(commonMain)
   //          }
   //  ← INCOMPLETE COMMENT
               dependsOn(commonMain)  ← ORPHANED CODE
           }  ← ORPHANED BRACE
   ```

2. **Affected Files** (6/10 core modules):
   - `voiceos-logging/build.gradle.kts` - 3 orphaned statements
   - `constants/build.gradle.kts` - 3 orphaned statements
   - `json-utils/build.gradle.kts` - 3 orphaned statements
   - `exceptions/build.gradle.kts` - 3 orphaned statements
   - `text-utils/build.gradle.kts` - 3 orphaned statements
   - `validation/build.gradle.kts` - 3 orphaned statements

3. **Unaffected Files** (4/10 core modules):
   - `result/build.gradle.kts` - Different corruption (partial comment)
   - `command-models/build.gradle.kts` - Clean
   - `hash/build.gradle.kts` - Different corruption
   - `database/build.gradle.kts` - Unknown (file not readable)

4. **Git History Evidence:**
   ```
   Last good state: 18cfa4a7 "fix: Restore complete VoiceOS modules"
   Corruption introduced: Between 18cfa4a7 and current HEAD
   Commits between: 20+ commits (WebAvanue, NLU work)
   ```

#### Hypothesis A2: Automated Find/Replace Error ⭐ LIKELY CAUSE

**Evidence:**
1. **Identical Pattern** - All 6 files have EXACT same corruption pattern
2. **Incomplete Regex** - Commenting left orphaned code blocks
3. **No Manual Variation** - Zero human-introduced variations

**Probable Command:**
```bash
# Suspected (incorrect) sed/regex that caused this:
sed -i '' 's/val ios.*by creating/\/\/          val ios.*by creating/' build.gradle.kts
# Only commented the declaration, not the body/closing brace
```

#### Hypothesis B2: Settings.gradle.kts Misconfiguration

**Evidence:**
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)  ← STRICT MODE
    repositories {
        google()
        mavenCentral()
    }
}
```

**Analysis:** Not the root cause, but amplifies impact by preventing fallback resolution.

#### Hypothesis C1: KMP Plugin Version Mismatch

**Evidence:**
```kotlin
// build.gradle.kts (root)
kotlin("multiplatform") version "1.9.25"  ← Latest
id("com.google.devtools.ksp") version "1.9.25-1.0.20"  ← Compatible
```

**Analysis:** Versions are aligned, not the issue.

### Phase 3: Root Cause Determination

**PRIMARY CAUSE:** Incomplete automated commenting of iOS target configurations (Hypothesis A1 + A2)

**MECHANISM:**
1. Someone attempted to disable iOS targets across all KMP modules
2. Used automated find/replace (likely sed or IDE mass edit)
3. Regex pattern only matched declaration lines, not entire blocks
4. Result: Commented declarations but left body/closing braces
5. Orphaned code blocks break Kotlin DSL parsing

**SECONDARY CAUSE:** No build validation after mass edit
- No CI/CD check ran after the change
- Manual build test skipped
- Error propagated to 6 modules before detection

---

## Configuration Diff Analysis

### Before (Commit 18cfa4a7) - WORKING
```kotlin
kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    // iOS targets - COMPLETE BLOCK
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "voiceos-logging"
        }
    }

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val commonMain by getting { }
        val androidMain by getting { dependsOn(commonMain) }

        val iosMain by creating {           ← DECLARATION
            dependsOn(commonMain)            ← BODY
        }                                    ← CLOSING BRACE

        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val jvmMain by getting { dependsOn(commonMain) }
    }
}
```

### After (Current) - BROKEN
```kotlin
kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    // iOS targets - INCOMPLETE COMMENT
//      listOf(                              ← Commented correctly
//          iosX64(),
//          iosArm64(),
//          iosSimulatorArm64()
//      ).forEach {
//          it.binaries.framework {
//              baseName = "voiceos-logging"
//          }
    }                                         ← ORPHANED BRACE

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val commonMain by getting { }
        val androidMain by getting { dependsOn(commonMain) }

//          val iosMain by creating {         ← DECLARATION COMMENTED
//              dependsOn(commonMain)          ← BODY COMMENTED
//          }                                  ← CLOSING BRACE COMMENTED
//                                             ← BUT EXTRA LINE BELOW:
            dependsOn(commonMain)              ← ORPHANED CODE ❌
        }                                      ← ORPHANED BRACE ❌

//          val iosX64Main by getting {        ← Same pattern
//              dependsOn(iosMain)
//          }
//
            dependsOn(iosMain)                 ← ORPHANED CODE ❌
        }                                      ← ORPHANED BRACE ❌

        // Pattern repeats for iosArm64Main, iosSimulatorArm64Main...

        val jvmMain by getting { dependsOn(commonMain) }
    }
}
```

### Error Output
```
e: file:///...constants/build.gradle.kts:79:9: Unexpected symbol
e: file:///...constants/build.gradle.kts:85:13: Unexpected symbol
e: file:///...constants/build.gradle.kts:85:22: Unexpected symbol
```

**Translation:** Kotlin DSL parser encounters:
- Line 79: `dependsOn(commonMain)` - but no variable declaration above it
- Line 85: `dependsOn(iosMain)` - but `iosMain` doesn't exist (commented out)
- Closing braces `}` with no matching opening context

---

## Impact Assessment

### Build System Impact
| Component | Status | Details |
|-----------|--------|---------|
| Core modules (6/10) | ❌ BROKEN | Parse errors, cannot compile |
| Core modules (4/10) | ⚠️ UNKNOWN | Different corruption patterns |
| Manager modules | ⚠️ UNKNOWN | Not tested yet |
| Library modules | ⚠️ UNKNOWN | Not tested yet |
| App modules | ❌ BLOCKED | Cannot resolve dependencies |

### Dependency Chain Impact
```
Apps (VoiceOSCore, LearnApp, etc.)
  ↓ depend on
Managers (CommandManager, etc.)
  ↓ depend on
Libraries (LearnAppCore, etc.)
  ↓ depend on
Core Modules ← 60% BROKEN
```

**Result:** Entire VoiceOS build system is blocked.

### Development Impact
- **Estimated Downtime:** Since corruption (unknown exact time)
- **Developer Productivity:** 0% (cannot build)
- **Testing:** Blocked (cannot generate APKs)
- **Deployment:** Blocked (no artifacts)

---

## Solution Strategy (Ranked by Risk/Complexity)

### Option 1: Automated Fix via Script ⭐ RECOMMENDED
**Approach:** Restore clean configurations from commit 18cfa4a7, then properly comment iOS targets

**Pros:**
- Guaranteed consistency across all files
- Atomic operation (all files fixed simultaneously)
- Repeatable if corruption recurs
- Fast execution (< 5 minutes)
- Can validate syntax before committing

**Cons:**
- Requires script development (15-30 min)
- Overwrites any manual fixes attempted
- Must test on sample file first

**Risk:** LOW
**Complexity:** LOW
**Time:** 30-45 minutes (including script dev + testing)

**Implementation:**
```bash
#!/bin/bash
# Fix VoiceOS KMP module corrupted iOS configurations

MODULES=(
  "voiceos-logging"
  "constants"
  "json-utils"
  "exceptions"
  "text-utils"
  "validation"
)

for module in "${MODULES[@]}"; do
  echo "Fixing $module..."
  git show 18cfa4a7:Modules/VoiceOS/core/$module/build.gradle.kts > \
    Modules/VoiceOS/core/$module/build.gradle.kts.clean

  # Properly comment iOS targets
  sed -i '' '/^[[:space:]]*\/\/ iOS targets$/,/^[[:space:]]*}$/s/^/\/\/ /' \
    Modules/VoiceOS/core/$module/build.gradle.kts.clean

  # Comment iOS sourceSets
  sed -i '' '/^[[:space:]]*val iosMain by creating/,/^[[:space:]]*}$/s/^/\/\/ /' \
    Modules/VoiceOS/core/$module/build.gradle.kts.clean

  mv Modules/VoiceOS/core/$module/build.gradle.kts.clean \
    Modules/VoiceOS/core/$module/build.gradle.kts
done

# Validate
./gradlew :Modules:VoiceOS:core:voiceos-logging:tasks --all
```

---

### Option 2: Full Restore from 18cfa4a7 + Remove JVM Targets
**Approach:** Restore working configs, then remove JVM targets (Android-only)

**Pros:**
- Eliminates iOS/JVM complexity entirely
- Simplifies KMP configuration
- Faster builds (fewer targets)
- Aligns with VoiceOS being Android-first

**Cons:**
- Loses multi-platform capability permanently
- May break future iOS port plans
- Requires re-review of all modules
- Changes architecture decision

**Risk:** MEDIUM (architectural change)
**Complexity:** MEDIUM
**Time:** 2-3 hours (review + test all modules)

**Implementation:**
```kotlin
// Simplified configuration (Android-only)
kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    sourceSets {
        val commonMain by getting { }
        val androidMain by getting { dependsOn(commonMain) }
    }
}
```

---

### Option 3: Manual Fix Per Module
**Approach:** Manually edit each of 6 files to remove orphaned code

**Pros:**
- Full control over each edit
- Can customize per-module if needed
- No automation risk

**Cons:**
- Error-prone (human mistakes)
- Time-consuming (6 files × 15 min = 90 min)
- Hard to verify consistency
- Tedious and boring

**Risk:** MEDIUM (human error)
**Complexity:** LOW
**Time:** 90-120 minutes

---

### Option 4: Nuclear - Re-import from Original VoiceOS Repo
**Approach:** Re-do entire migration from scratch

**Pros:**
- Guaranteed clean slate
- Catches other migration issues
- Proper migration documentation

**Cons:**
- Loses all post-migration work (20+ commits)
- Extremely time-consuming (4-8 hours)
- High risk of introducing new issues
- Must re-do import path fixes

**Risk:** HIGH
**Complexity:** HIGH
**Time:** 4-8 hours

---

### Option 5: Hybrid - Restore + Validate + Automated Disable
**Approach:** Combine Option 1 script with build validation loop

**Pros:**
- All benefits of Option 1
- Plus: Automated syntax validation
- Plus: Build test before commit
- Plus: Can catch other corruption patterns

**Cons:**
- Slightly more complex script
- Takes longer (build validation adds 5-10 min)

**Risk:** VERY LOW ⭐ SAFEST
**Complexity:** MEDIUM
**Time:** 45-60 minutes

**Implementation:**
```bash
#!/bin/bash
set -e  # Exit on any error

MODULES=(...)  # Same as Option 1

for module in "${MODULES[@]}"; do
  echo "Fixing $module..."
  # Restore clean
  git show 18cfa4a7:Modules/VoiceOS/core/$module/build.gradle.kts > \
    Modules/VoiceOS/core/$module/build.gradle.kts

  # Validate syntax
  if ! ./gradlew :Modules:VoiceOS:core:$module:tasks --dry-run 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    echo "ERROR: $module still broken after restore!"
    exit 1
  fi
done

# Full build test
./gradlew :Modules:VoiceOS:core:voiceos-logging:build
./gradlew :Modules:VoiceOS:core:constants:build

echo "All modules fixed and validated!"
```

---

## Recommendation Matrix

| Option | Risk | Time | Reliability | Future-Proof | Score |
|--------|------|------|-------------|--------------|-------|
| 1. Automated Fix | LOW | 30m | HIGH | MEDIUM | ⭐⭐⭐⭐ |
| 5. Hybrid (Restore + Validate) | VERY LOW | 60m | VERY HIGH | MEDIUM | ⭐⭐⭐⭐⭐ |
| 2. Android-Only | MEDIUM | 2-3h | HIGH | LOW | ⭐⭐⭐ |
| 3. Manual Fix | MEDIUM | 90m | MEDIUM | MEDIUM | ⭐⭐ |
| 4. Nuclear Re-import | HIGH | 4-8h | MEDIUM | HIGH | ⭐ |

**AI Recommendation:** **Option 5 (Hybrid)** - Restore from 18cfa4a7 with automated validation

**Rationale:**
1. Lowest risk (automated + validated)
2. Reasonable time investment (< 1 hour)
3. Preserves multi-platform capability for future
4. Provides reusable script for similar issues
5. Build validation prevents commit of broken state

---

## Prevention Strategy

### Immediate Actions (Before Fix)
1. ✅ Document current state (this analysis)
2. ⏳ Create git branch for fix isolation
3. ⏳ Notify team of build freeze

### During Fix
1. Run fix script on isolated branch
2. Validate each module builds independently
3. Test Android app integration
4. Run full build suite
5. Compare .apk sizes (sanity check)

### Post-Fix Validation
```bash
# Build all core modules
for module in voiceos-logging constants json-utils exceptions text-utils validation; do
  ./gradlew :Modules:VoiceOS:core:$module:build || exit 1
done

# Build Android app
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug || exit 1

# Verify APK
ls -lh android/voiceos/VoiceOSCore/build/outputs/apk/debug/
```

### Long-Term Prevention

#### 1. Pre-Commit Hooks
```bash
#!/bin/bash
# .git/hooks/pre-commit

# Check for orphaned dependsOn statements
if git diff --cached --name-only | grep -q "build.gradle.kts"; then
  for file in $(git diff --cached --name-only | grep "build.gradle.kts"); do
    if grep -q "^[[:space:]]*dependsOn(" "$file" && \
       ! grep -B2 "^[[:space:]]*dependsOn(" "$file" | grep -q "val.*by"; then
      echo "ERROR: Orphaned dependsOn() found in $file"
      echo "This indicates corrupted KMP configuration"
      exit 1
    fi
  done
fi
```

#### 2. CI/CD Build Validation
```yaml
# .github/workflows/build-check.yml
name: Build Validation
on: [push, pull_request]
jobs:
  validate-kmp-modules:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Validate all KMP modules
        run: |
          ./gradlew :Modules:VoiceOS:core:voiceos-logging:tasks
          ./gradlew :Modules:VoiceOS:core:constants:tasks
          # ... repeat for all core modules
```

#### 3. Automated Syntax Checker
```bash
#!/bin/bash
# scripts/check-kmp-syntax.sh

find Modules/VoiceOS -name "build.gradle.kts" -print0 | while IFS= read -r -d '' file; do
  echo "Checking $file..."

  # Check for orphaned dependsOn
  if grep -q "^[[:space:]]*dependsOn(" "$file"; then
    orphaned=$(grep -n "^[[:space:]]*dependsOn(" "$file" | \
      while read line; do
        linenum=$(echo "$line" | cut -d: -f1)
        context=$(sed -n "$((linenum-2)),$((linenum))p" "$file")
        if ! echo "$context" | grep -q "val.*by"; then
          echo "Line $linenum: orphaned dependsOn"
        fi
      done)

    if [ -n "$orphaned" ]; then
      echo "ERROR in $file:"
      echo "$orphaned"
      exit 1
    fi
  fi

  # Check for orphaned closing braces after comments
  # (Advanced pattern matching - TBD)
done

echo "All KMP configurations valid!"
```

#### 4. Mass Edit Guidelines
**RULE:** Never use automated find/replace on `build.gradle.kts` without:
1. Testing on ONE file first
2. Running `./gradlew tasks` on that file
3. Committing to separate branch
4. Peer review before merge

**Safe Mass Edit Process:**
```bash
# CORRECT approach to commenting iOS targets:
git checkout -b fix/disable-ios-targets

# Test on ONE file first
cp Modules/VoiceOS/core/voiceos-logging/build.gradle.kts backup.kts
# ... edit manually ...
./gradlew :Modules:VoiceOS:core:voiceos-logging:tasks
# If successful, proceed

# Then automate for remaining files
for module in constants json-utils ...; do
  # Apply same edit
  ./gradlew :Modules:VoiceOS:core:$module:tasks || exit 1
done
```

#### 5. Living Documentation
- Document KMP configuration standards in `/Docs/VoiceOS/Standards/`
- Include "correct" and "incorrect" examples
- Reference in onboarding docs

#### 6. Code Review Checklist
When reviewing PRs that touch `build.gradle.kts`:
- [ ] All commented blocks have complete opening/closing braces
- [ ] No orphaned `dependsOn()` statements
- [ ] `./gradlew tasks` runs successfully on affected modules
- [ ] Android app still builds

---

## Technical Debt Assessment

### Created By This Issue
| Debt Item | Severity | Effort to Fix |
|-----------|----------|---------------|
| Corrupted KMP configs (6 files) | CRITICAL | 1-2 hours |
| Disabled iOS targets | MEDIUM | 4-8 hours (if needed) |
| Missing build validation | HIGH | 2-4 hours (CI setup) |
| No pre-commit hooks | MEDIUM | 1-2 hours |

### Pre-Existing (Exposed By This Issue)
| Debt Item | Severity | Evidence |
|-----------|----------|----------|
| No automated testing of builds | HIGH | Issue undetected for 20+ commits |
| Manual migration process | MEDIUM | Multiple corruption patterns |
| Inconsistent KMP target strategy | LOW | Some modules have iOS, some don't |
| Missing version catalog | LOW | Kotlin versions in root build.gradle.kts |

---

## Lessons Learned

### What Went Wrong
1. **Automated edits without validation** - Mass commenting broke DSL syntax
2. **No build check after mass edit** - Error propagated unnoticed
3. **No pre-commit hooks** - Invalid configs committed
4. **No CI/CD** - 20+ commits passed without build validation

### What Went Right
1. **Git history preserved** - Can restore from 18cfa4a7
2. **Modular structure** - Corruption isolated to specific modules
3. **Consistent patterns** - Makes automated fix feasible

### Process Improvements
1. **NEVER mass-edit build files** without per-file validation
2. **ALWAYS test builds** before committing configuration changes
3. **IMPLEMENT pre-commit hooks** for syntax validation
4. **REQUIRE CI/CD** before merging to main branches
5. **DOCUMENT KMP standards** to prevent future variations

---

## Next Steps

### Immediate (Today)
1. [ ] Create fix branch: `fix/voiceos-kmp-corruption`
2. [ ] Implement Option 5 (Hybrid fix script)
3. [ ] Validate all 6 modules build successfully
4. [ ] Test Android app integration
5. [ ] Commit with detailed message
6. [ ] Create PR with this analysis attached

### Short-Term (This Week)
1. [ ] Add pre-commit hook for KMP syntax validation
2. [ ] Set up GitHub Actions build validation
3. [ ] Document KMP configuration standards
4. [ ] Audit remaining modules (managers, libraries, apps)

### Long-Term (This Month)
1. [ ] Decide: Keep iOS targets commented vs remove entirely
2. [ ] Implement version catalog (gradle/libs.versions.toml)
3. [ ] Create migration playbook for future module moves
4. [ ] Train team on safe mass-editing practices

---

## Appendix A: Affected Files (Full List)

### Core Modules - Confirmed Corruption
1. `Modules/VoiceOS/core/voiceos-logging/build.gradle.kts` - 3 orphaned dependsOn
2. `Modules/VoiceOS/core/constants/build.gradle.kts` - 3 orphaned dependsOn
3. `Modules/VoiceOS/core/json-utils/build.gradle.kts` - 3 orphaned dependsOn
4. `Modules/VoiceOS/core/exceptions/build.gradle.kts` - 3 orphaned dependsOn
5. `Modules/VoiceOS/core/text-utils/build.gradle.kts` - 3 orphaned dependsOn
6. `Modules/VoiceOS/core/validation/build.gradle.kts` - 3 orphaned dependsOn

### Core Modules - Different Corruption Pattern
7. `Modules/VoiceOS/core/result/build.gradle.kts` - Partial commenting, mixed pattern
8. `Modules/VoiceOS/core/hash/build.gradle.kts` - Similar to result

### Core Modules - Status Unknown
9. `Modules/VoiceOS/core/command-models/build.gradle.kts` - Needs verification
10. `Modules/VoiceOS/core/database/build.gradle.kts` - File read error

### Other Modules - Not Yet Audited
- `Modules/VoiceOS/managers/*` (5 modules)
- `Modules/VoiceOS/libraries/*` (9 modules)
- `Modules/VoiceOS/apps/*` (6 modules)

**Total Build Files:** 19
**Audited:** 10
**Confirmed Broken:** 6
**Suspected Broken:** 2
**Unknown:** 11

---

## Appendix B: Git Timeline

```
18cfa4a7 (Dec 15) fix: Restore complete VoiceOS modules from VoiceOS-Development
  ↓ WORKING STATE ✅
  ↓
  ↓ [20+ commits - WebAvanue, NLU, Cockpit work]
  ↓ [iOS targets commented - CORRUPTION INTRODUCED ❌]
  ↓
HEAD (Dec 16) BROKEN STATE ❌
```

**Key Commits Between:**
- 6ec149f2 - fix: disable MCP server auto-start
- 2760bde8 - feat(voiceos): add InstalledAppsManager and update VoiceOSService
- ce321708 - refactor(CommandManager): move deprecated NLU integration files
- bf66554c - chore(nlu): remove NLU source files

**Suspected Corruption Commit:** Unknown - needs `git bisect` to identify

---

## Appendix C: Example Fix Script (Complete)

```bash
#!/bin/bash
# fix-voiceos-kmp-corruption.sh
# Restores clean KMP configurations from commit 18cfa4a7

set -e  # Exit on any error

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
RESTORE_COMMIT="18cfa4a7"
MODULES=(
  "voiceos-logging"
  "constants"
  "json-utils"
  "exceptions"
  "text-utils"
  "validation"
)

echo -e "${YELLOW}VoiceOS KMP Configuration Fix${NC}"
echo "Restoring from commit: $RESTORE_COMMIT"
echo "Modules to fix: ${#MODULES[@]}"
echo ""

# Verify we're in correct directory
if [ ! -d "Modules/VoiceOS/core" ]; then
  echo -e "${RED}ERROR: Must run from NewAvanues repository root${NC}"
  exit 1
fi

# Create backup branch
echo -e "${YELLOW}Creating backup branch...${NC}"
BACKUP_BRANCH="backup/before-kmp-fix-$(date +%Y%m%d-%H%M%S)"
git branch "$BACKUP_BRANCH"
echo -e "${GREEN}Backup created: $BACKUP_BRANCH${NC}"
echo ""

# Process each module
SUCCESS=0
FAILED=0

for module in "${MODULES[@]}"; do
  MODULE_PATH="Modules/VoiceOS/core/$module"
  BUILD_FILE="$MODULE_PATH/build.gradle.kts"

  echo -e "${YELLOW}Processing $module...${NC}"

  # Restore from git
  if git show "$RESTORE_COMMIT:$BUILD_FILE" > "$BUILD_FILE.restored" 2>/dev/null; then
    mv "$BUILD_FILE.restored" "$BUILD_FILE"
    echo "  ✓ Restored from $RESTORE_COMMIT"
  else
    echo -e "  ${RED}✗ Failed to restore from git${NC}"
    FAILED=$((FAILED + 1))
    continue
  fi

  # Validate syntax (dry-run tasks)
  if ./gradlew ":$MODULE_PATH:tasks" --dry-run &>/dev/null; then
    echo -e "  ${GREEN}✓ Syntax validated${NC}"
    SUCCESS=$((SUCCESS + 1))
  else
    echo -e "  ${RED}✗ Syntax validation failed${NC}"
    FAILED=$((FAILED + 1))
  fi

  echo ""
done

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}Success: $SUCCESS modules${NC}"
if [ $FAILED -gt 0 ]; then
  echo -e "${RED}Failed: $FAILED modules${NC}"
fi
echo ""

# Final validation
if [ $FAILED -eq 0 ]; then
  echo -e "${YELLOW}Running full build test...${NC}"
  if ./gradlew :Modules:VoiceOS:core:voiceos-logging:build; then
    echo -e "${GREEN}✓ Build test PASSED${NC}"
    echo ""
    echo "Fix completed successfully!"
    echo "To commit: git add -A && git commit -m 'fix(voiceos): restore KMP configurations from $RESTORE_COMMIT'"
  else
    echo -e "${RED}✗ Build test FAILED${NC}"
    echo "Please investigate manually"
  fi
else
  echo -e "${RED}Some modules failed. Please fix manually.${NC}"
  echo "Backup branch: $BACKUP_BRANCH"
fi
```

---

**Document Status:** Complete
**Approval Required:** Yes (before implementing fix)
**Estimated Reading Time:** 15-20 minutes
**Next Review:** After fix implementation

