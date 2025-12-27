# Complete Migration Guide - MainAvanues Monorepo

**Version:** 2.0
**Date:** 2025-11-24
**Updated:** Added git history preservation with filter-repo
**Purpose:** Step-by-step guide for migrating projects with full git history

---

## 📋 Overview

This guide covers the complete migration process for bringing projects into MainAvanues monorepo while:
- ✅ Preserving full git history
- ✅ Maintaining contributor attribution
- ✅ Following clean naming principles
- ✅ Enabling git blame across migration
- ✅ Allowing safe deletion of old repos after testing

---

## 🎯 Prerequisites

### Tools Required
```bash
# 1. Git (2.24+)
git --version

# 2. Git Filter-Repo
brew install git-filter-repo
# or: pip install git-filter-repo

# 3. Verify installation
git filter-repo --version
```

### Knowledge Required
- Git basics (commit, merge, remote)
- Project structure understanding
- Gradle/build system (for KMP projects)

---

## 📊 Migration Phases

### Phase 1: Analysis & Planning (30-60 min)
- Analyze project structure
- Map old → new paths
- Identify dependencies
- Design clean structure

### Phase 2: Git History Verification (30 min)
- Verify git copy detection is working
- Test git log --follow
- Test git blame -C -C -C
- Configure git blame settings

### Phase 3: Monorepo Integration (1-2 hours)
- Merge filtered history
- Update build files
- Fix imports/paths
- Update documentation

### Phase 4: Build & Test (2-4 hours)
- Configure Gradle modules
- Compile all targets
- Run tests
- Test on emulators
- Log errors

### Phase 5: Verification & Cleanup (1 week)
- Verify git history
- Test all functionality
- Team validation
- Archive/delete old repo

**Total Time:** 5-8 hours active work + 1 week testing

---

## 🔍 Phase 1: Analysis & Planning

### Step 1.1: Analyze Source Project

```bash
cd /path/to/source/project

echo "=== Project Analysis ==="
echo "Project: $(basename $(pwd))"
echo "Kotlin files: $(find . -name "*.kt" -not -path "*/build/*" | wc -l)"
echo "Git commits: $(git rev-list --count HEAD)"
echo "Contributors: $(git shortlog -sn | wc -l)"
echo "Last commit: $(git log -1 --format='%h - %s')"
```

### Step 1.2: Map Directory Structure

**Document current structure:**
```
Source Project Structure:
  app/                    → Android app module
  universal/              → KMP shared code
  BrowserCoreData/        → Data layer module
  Android/                → Android platform code
  iOS/                    → iOS platform code
  Desktop/                → Desktop platform code
  docs/                   → Documentation
  .ideacode-v2/features/  → IDEACODE features
```

**Design target structure (apply clean naming principles):**
```
Target Monorepo Structure:
  android/apps/webavanue/                      ← app/
  common/libs/webavanue/universal/             ← universal/
  common/libs/webavanue/coredata/              ← BrowserCoreData/
  common/libs/webview/android/                 ← Android/
  common/libs/webview/ios/                     ← iOS/
  common/libs/webview/desktop/                 ← Desktop/
  docs/android/apps/webavanue/                 ← docs/ (app-specific)
  docs/common/libs/webavanue/                  ← docs/ (library)
  docs/common/libs/webavanue/ideacode/features/ ← .ideacode-v2/features/
```

**Rules Applied:**
- ✅ No type prefixes (feature-, data-)
- ✅ Parent/child for "part of" (webavanue/coredata)
- ✅ Platform grouping (webview/android, /ios, /desktop)
- ✅ No scope redundancy (webavanue not under browser/)

### Step 1.3: Create Path Mapping File

Create `migration-paths.txt`:
```
app/ → android/apps/webavanue/
universal/ → common/libs/webavanue/universal/
BrowserCoreData/ → common/libs/webavanue/coredata/
Android/ → common/libs/webview/android/
iOS/ → common/libs/webview/ios/
Desktop/ → common/libs/webview/desktop/
docs/ → docs/common/libs/webavanue/
.ideacode-v2/features/ → docs/common/libs/webavanue/ideacode/features/
README.md → docs/common/libs/webavanue/README.md
build.gradle.kts → common/libs/webavanue/build.gradle.kts
```

---

## 🔧 Phase 2: Git History Preparation

### Step 2.1: Create Backup Branch

For in-repo migrations (module already in monorepo), create a backup branch before verification:

```bash
cd /Volumes/M-Drive/Coding/MainAvanues

# Create backup branch
git branch backup-webavanue-before-history-verification

# Tag the pre-migration state
git tag -a "pre-monorepo-migration" -m "Before MainAvanues monorepo migration - $(date +%Y-%m-%d)"
```

### Step 2.2: Verify Git Copy Detection

Git's copy detection can track file history across migrations without rewriting history. Test with `-C` flags:

```bash
# Test git log with --follow
git log --oneline --follow -- android/apps/webavanue/src/main/.../MainActivity.kt

# Test git blame with copy detection (-C -C -C)
git blame -C -C -C android/apps/webavanue/src/main/.../MainActivity.kt | head -20
```

**What to look for:**
- `git log --follow` should show commits from before and after migration
- `git blame -C -C -C` should show original file paths in the blame output
- Commit hashes should match the original commits

### Step 2.3: Configure Git for Copy Detection

Enable copy detection permanently in the repository:

```bash
cd /Volumes/M-Drive/Coding/MainAvanues

# Enable copy detection for blame
git config blame.detectCopies true
git config blame.detectCopiesHarder true

# Verify configuration
git config --list | grep blame
```

**Result:**
```
blame.detectcopies=true
blame.detectcopiesharder=true
```

### Step 2.4: Test Multiple Files

Verify history tracking works across all migrated modules:

```bash
# Test Android app file
git log --oneline --follow -- android/apps/webavanue/src/main/kotlin/MainActivity.kt | head -10
git blame -C -C -C android/apps/webavanue/src/main/kotlin/MainActivity.kt | head -10

# Test Universal module file
git log --oneline --follow -- common/libs/webavanue/universal/domain/model/Tab.kt | head -10
git blame -C -C -C common/libs/webavanue/universal/domain/model/Tab.kt | head -10

# Test CoreData module file
git log --oneline --follow -- common/libs/webavanue/coredata/src/commonMain/kotlin/.../BrowserSettings.kt | head -10
git blame -C -C -C common/libs/webavanue/coredata/src/commonMain/kotlin/.../BrowserSettings.kt | head -10
```

### Step 2.5: Document Verification Results

Create a verification report:

```bash
# Document the verification in docs/develop/
# See: docs/develop/webavanue/WebAvanue-Git-History-Verification-*.md
```

**Expected Content:**
- Path mappings (old → new)
- Git log --follow test results
- Git blame -C -C -C test results
- Usage guidelines for developers
- IDE configuration tips

---

## 🔀 Phase 3: Monorepo Integration

### Step 3.1: Backup Monorepo

```bash
cd /Volumes/M-Drive/Coding/MainAvanues

# Create backup
timestamp=$(date +%Y%m%d-%H%M%S)
mkdir -p .migration-backups
cp -r .git .migration-backups/git-backup-$timestamp
```

### Step 3.2: Add Filtered Repo as Remote

```bash
cd /Volumes/M-Drive/Coding/MainAvanues

# Add temporary remote
git remote add project-history /tmp/project-filtered

# Fetch history
git fetch project-history

# Verify remote
git remote -v
git log project-history/main --oneline | head -5
```

### Step 3.3: Merge with History Preservation

```bash
# Create merge commit
git merge --allow-unrelated-histories project-history/main \
  -m "Migrate ProjectName with full git history

Migrated from: /path/to/original/project
Migration date: $(date)
Last original commit: $(cd /tmp/project-filtered && git rev-parse HEAD)

Paths rewritten:
- app/ → android/apps/projectname/
- universal/ → common/libs/projectname/universal/
- (see migration-paths.txt for complete mapping)

All git history, contributors, and blame information preserved."

# Remove temporary remote
git remote remove project-history

# Clean up temp directory
rm -rf /tmp/project-filtered
```

### Step 3.4: Verify Merge

```bash
# Verify new files appear
git log --name-status --oneline -10

# Verify history includes old commits
git log android/apps/webavanue/src/Main.kt --oneline

# Verify blame works
git blame android/apps/webavanue/src/Main.kt | head -5

# Verify contributors preserved
git shortlog -sn | grep "Original Author"
```

---

## 🔨 Phase 4: Build & Test Configuration

### Step 4.1: Update settings.gradle.kts

```kotlin
// Remove old includeBuild if exists
// includeBuild("Modules/WebAvanue")  ← DELETE

// Add new modules
include(":android:apps:webavanue")
include(":common:libs:webavanue:universal")
include(":common:libs:webavanue:coredata")
include(":common:libs:webview:android")
include(":common:libs:webview:ios")
include(":common:libs:webview:desktop")

// Set project directories
project(":android:apps:webavanue").projectDir =
    file("android/apps/webavanue")
project(":common:libs:webavanue:universal").projectDir =
    file("common/libs/webavanue/universal")
project(":common:libs:webavanue:coredata").projectDir =
    file("common/libs/webavanue/coredata")
// ... etc
```

### Step 4.2: Create Module Build Files

**android/apps/webavanue/build.gradle.kts:**
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
}

android {
    namespace = "com.augmentalis.webavanue"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.augmentalis.webavanue"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":common:libs:webavanue:universal"))
    implementation(project(":common:libs:webavanue:coredata"))
    implementation(project(":common:libs:webview:android"))

    // Other dependencies...
}
```

**common/libs/webavanue/universal/build.gradle.kts:**
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":common:libs:webavanue:coredata"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(project(":common:libs:webview:android"))
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(project(":common:libs:webview:ios"))
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(project(":common:libs:webview:desktop"))
            }
        }
    }
}

android {
    namespace = "com.augmentalis.webavanue.universal"
    compileSdk = 34
}
```

### Step 4.3: Update Import Paths

**Before:**
```kotlin
import com.augmentalis.Avanues.web.app.MainActivity
import com.augmentalis.webavanue.BrowserApp
```

**After (if package structure changed):**
```kotlin
import com.augmentalis.webavanue.MainActivity
import com.augmentalis.webavanue.app.BrowserApp
```

**Use find/replace:**
```bash
# Find all import statements
grep -r "import com.augmentalis" android/apps/webavanue/src/

# Replace in bulk (be careful!)
find android/apps/webavanue/src -name "*.kt" -exec sed -i '' \
  's/com\.augmentalis\.Avanues\.web/com.augmentalis.webavanue/g' {} +
```

### Step 4.4: Sync and Build

```bash
# Sync Gradle
./gradlew --refresh-dependencies

# Build specific module
./gradlew :android:apps:webavanue:assembleDebug

# Build all
./gradlew build
```

**Log errors to:**
```bash
mkdir -p docs/develop/webavanue
./gradlew :android:apps:webavanue:assembleDebug 2>&1 | \
  tee docs/develop/webavanue/WebAvanue-Build-Error-$(date +%Y%m%d%H%M).md
```

### Step 4.5: Fix Build Errors

**Common issues:**

1. **Missing dependencies:**
```kotlin
// Add to build.gradle.kts
implementation("androidx.core:core-ktx:1.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

2. **Wrong package names:**
```kotlin
// Update AndroidManifest.xml
<manifest package="com.augmentalis.webavanue">
```

3. **Module not found:**
```kotlin
// Check settings.gradle.kts includes module
include(":common:libs:webavanue:coredata")
```

4. **Version conflicts:**
```kotlin
// Align versions in root build.gradle.kts
ext {
    set("kotlin_version", "2.1.0")
    set("compose_version", "1.7.1")
}
```

### Step 4.6: Run Tests

```bash
# Unit tests
./gradlew :android:apps:webavanue:testDebugUnitTest

# Instrumented tests (emulator required)
./gradlew :android:apps:webavanue:connectedDebugAndroidTest

# All tests
./gradlew test

# Log test results
./gradlew test 2>&1 | \
  tee docs/develop/webavanue/WebAvanue-Test-Results-$(date +%Y%m%d%H%M).md
```

### Step 4.7: Test on Emulator

```bash
# Start emulator
emulator -avd Pixel_6_API_34 &

# Install app
./gradlew :android:apps:webavanue:installDebug

# Run app
adb shell am start -n com.augmentalis.webavanue/.MainActivity

# Check logs
adb logcat | grep "WebAvanue"
```

**If errors occur:**
```bash
# Capture logcat
adb logcat > docs/develop/webavanue/WebAvanue-Logcat-Error-$(date +%Y%m%d%H%M).md

# Capture stack trace
adb logcat *:E > docs/develop/webavanue/WebAvanue-Errors-$(date +%Y%m%d%H%M).md
```

---

## ✅ Phase 5: Verification & Cleanup

### Step 5.1: Verify Git History

```bash
# 1. Check total commits
git rev-list --count HEAD

# 2. Verify old commits visible
git log --all --grep="WebAvanue" --oneline

# 3. Test git blame
git blame android/apps/webavanue/src/main/kotlin/MainActivity.kt

# 4. Verify contributors
git shortlog -sn | head -10

# 5. Test bisect (find when bug introduced)
git bisect start
git bisect bad HEAD
git bisect good <old-commit-hash>
# ... bisect should work across migration boundary
```

### Step 5.2: Create Migration Summary

```bash
# Document migration
cat > docs/common/libs/webavanue/MIGRATION.md << 'EOF'
# WebAvanue Migration Summary

**Migrated:** 2025-11-24
**From:** /Volumes/M-Drive/Coding/Modules/WebAvanue
**Method:** Git filter-repo with full history preservation

## Git History
- **Total commits:** 123
- **Contributors:** 3
- **First commit:** 2024-11-16 abc123d
- **Last commit:** 2025-11-24 xyz789e
- **Tag:** pre-monorepo-migration

## Files Migrated
- Kotlin files: 121
- Documentation: 49
- IDEACODE features: 12

## Structure Changes
| Old Path | New Path |
|----------|----------|
| app/ | android/apps/webavanue/ |
| universal/ | common/libs/webavanue/universal/ |
| BrowserCoreData/ | common/libs/webavanue/coredata/ |

## Git Commands Work
- ✅ git log (full history)
- ✅ git blame (author attribution)
- ✅ git bisect (bug finding)
- ✅ IDE git integration

## Original Repo
**Status:** Archived
**Location:** /archive/deprecated-repos/WebAvanue-deprecated-2025-11-24
**Can be deleted after:** 2025-12-08 (2 weeks testing)
EOF
```

### Step 5.3: Team Validation (1-2 weeks)

**Checklist:**
- [ ] All features work correctly
- [ ] No regressions found
- [ ] Tests pass on CI/CD
- [ ] Team members can build locally
- [ ] Git history accessible to all
- [ ] Documentation updated
- [ ] No performance degradation

### Step 5.4: Archive Old Repo

**After successful testing (2+ weeks):**

```bash
# Move to archive
mkdir -p /archive/deprecated-repos
mv /Volumes/M-Drive/Coding/Modules/WebAvanue \
   /archive/deprecated-repos/WebAvanue-deprecated-$(date +%Y-%m-%d)

# Or delete entirely (only if very confident)
# rm -rf /Volumes/M-Drive/Coding/Modules/WebAvanue

# Update MainAvanues settings.gradle.kts
# Remove any references to old path
# Commit
git add settings.gradle.kts
git commit -m "Remove reference to archived WebAvanue module"
```

### Step 5.5: Update Documentation

```bash
# Update monorepo docs
# - MONOREPO-STRUCTURE.md
# - README.md
# - ARCHITECTURE.md

# Add to migration registry
echo "WebAvanue,2025-11-24,121 files,Full history preserved" >> \
  docs/migration-analysis/MIGRATION-REGISTRY.csv
```

---

## 📊 Migration Checklist

Use this checklist for each project:

### Pre-Migration
- [ ] Project analyzed (file counts, git history)
- [ ] Path mapping documented
- [ ] Clean structure designed (no redundant naming)
- [ ] Team notified
- [ ] Original repo tagged (pre-monorepo-migration)
- [ ] Backup created

### Git History Preservation
- [ ] git-filter-repo installed
- [ ] Cloned to temp location
- [ ] Paths filtered and rewritten
- [ ] Filtered repo verified
- [ ] Added as remote to monorepo
- [ ] Merged with --allow-unrelated-histories
- [ ] History verified (log, blame work)

### Build Configuration
- [ ] settings.gradle.kts updated
- [ ] Module build files created
- [ ] Dependencies configured
- [ ] Import paths updated
- [ ] Gradle sync successful
- [ ] Project compiles

### Testing
- [ ] Unit tests pass
- [ ] Instrumented tests pass
- [ ] App runs on emulator
- [ ] No regressions found
- [ ] Errors logged (if any)

### Documentation
- [ ] MIGRATION.md created
- [ ] Build errors documented
- [ ] Team docs updated
- [ ] Registry updated

### Verification (1-2 weeks)
- [ ] Team validation complete
- [ ] All features tested
- [ ] Git commands work
- [ ] No issues reported

### Cleanup
- [ ] Old repo archived/deleted
- [ ] References removed from code
- [ ] Final commit made

---

## 🚨 Troubleshooting

### Git Filter-Repo Issues

**Problem:** "fatal: not a git repository"
```bash
# Solution: Ensure you're in cloned repo
cd /tmp/project-filtered
git status  # Should work
```

**Problem:** "Cannot run filter-repo on non-fresh clone"
```bash
# Solution: Use --force flag
git filter-repo --force --path-rename old:new
```

**Problem:** Paths not rewritten
```bash
# Solution: Check path syntax (no trailing slashes)
git filter-repo --path-rename app/:android/apps/name/
# NOT: app → android/apps/name (missing trailing /)
```

### Merge Conflicts

**Problem:** CONFLICT (content) in file.kt
```bash
# Solution: Resolve manually
git status  # See conflicted files
# Edit files, keep HEAD or merged version
git add .
git commit -m "Resolve merge conflicts"
```

### Build Errors

**Problem:** Module not found
```bash
# Check settings.gradle.kts includes module
include(":android:apps:webavanue")

# Check projectDir is correct
project(":android:apps:webavanue").projectDir = file("android/apps/webavanue")

# Sync gradle
./gradlew --refresh-dependencies
```

**Problem:** Package does not exist
```bash
# Update import statements
find . -name "*.kt" -exec grep -l "old.package" {} +
# Use IDE refactor or sed to replace
```

### Test Failures

**Problem:** Tests can't find resources
```bash
# Check resources copied to correct location
# Update build.gradle.kts sourceSets if needed
android {
    sourceSets {
        main {
            res.srcDirs("src/main/res")
        }
    }
}
```

---

## 📚 Related Documents

- [MIGRATION-LESSONS-LEARNED.md](./MIGRATION-LESSONS-LEARNED.md) - Key principles
- [MONOREPO-STRUCTURE.md](../MONOREPO-STRUCTURE.md) - Target structure
- [foldernaming.md](/Volumes/M-Drive/Coding/ideacode/updateideas/foldernaming.md) - Naming guidelines

---

## 🎯 Success Criteria

Migration is successful when:
- ✅ All files migrated to correct locations
- ✅ Full git history preserved and accessible
- ✅ Git blame shows original authors
- ✅ Project compiles without errors
- ✅ All tests pass
- ✅ App runs on emulator
- ✅ Team can build locally
- ✅ No regressions found
- ✅ Documentation complete
- ✅ Old repo can be safely deleted

---

**Last Updated:** 2025-11-24
**Version:** 2.0
**Author:** IDEACODE Framework
**Status:** Production Ready
