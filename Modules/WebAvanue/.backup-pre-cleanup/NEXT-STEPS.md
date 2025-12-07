# WebAvanue - Next Steps

**Date:** 2025-11-17
**Status:** Folder reorganization complete, build issues to resolve

---

## ✅ Completed

1. **IDEACODE Folder Structure** ✅
   - Universal folder with all shared code
   - Platform-specific folders (Android, iOS, Desktop)
   - Tests organized by platform
   - Documentation (FOLDER-STRUCTURE.md)

2. **Platform Implementations** ✅
   - Android WebView (android.webkit.WebView)
   - iOS WKWebView (WKWebView)
   - Desktop JavaFX (JavaFX WebView)

3. **Test Coverage** ✅
   - 60+ comprehensive tests created
   - Domain, Repository, Use Case, and Platform tests

---

## 🚧 Current Issues

### Issue #1: SQLDelight Build Failure (BLOCKER)

**Error:**
```
Cannot find service com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
```

**Cause:** Known compatibility issue with SQLDelight 2.1.0 and certain Java/Gradle environments

**Impact:** Prevents building and testing the BrowserCoreData module

---

## 📋 Required Next Steps (Priority Order)

### Priority 1: Resolve SQLDelight Build Issue

**Option A: Switch to Room (Recommended for Android-first)**
```kotlin
// Remove SQLDelight dependency
// Add Room dependencies
dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

**Pros:**
- ✅ Better Android integration
- ✅ Mature and stable
- ✅ Excellent tooling support
- ✅ No build issues

**Cons:**
- ❌ Android-only (need separate implementations for iOS/Desktop)
- ❌ More platform-specific code

**Option B: Downgrade to SQLDelight 2.0.0**
```kotlin
// build.gradle.kts
id("app.cash.sqldelight") version "2.0.0" apply false
```

**Pros:**
- ✅ Quick fix
- ✅ Keep cross-platform database

**Cons:**
- ⚠️ Older version, may have other issues
- ⚠️ Missing newer features

**Option C: Use in-memory database for now**
```kotlin
// Create mock implementation
class InMemoryBrowserRepository : BrowserRepository {
    private val tabs = mutableListOf<Tab>()
    private val favorites = mutableListOf<Favorite>()
    // ... implement with in-memory collections
}
```

**Pros:**
- ✅ Immediate unblocking
- ✅ Tests can run
- ✅ Development can continue

**Cons:**
- ❌ No persistence
- ❌ Temporary solution only

**Recommendation:** Option C for immediate development, then Option A for production

---

### Priority 2: Update Build Configurations

**Tasks:**
1. Create separate `build.gradle.kts` for each module:
   - `Universal/build.gradle.kts`
   - `Android/build.gradle.kts`
   - `iOS/build.gradle.kts`
   - `Desktop/build.gradle.kts`

2. Update `settings.gradle.kts`:
```kotlin
include(":Universal")
include(":Android")
include(":iOS")
include(":Desktop")
include(":app")
```

3. Configure module dependencies:
```kotlin
// Android/build.gradle.kts
dependencies {
    implementation(project(":Universal"))
}
```

4. Remove deprecated `kotlinOptions` (migrate to `compilerOptions`)

**Estimated Time:** 2-3 hours

---

### Priority 3: Fix Import Statements

After reorganization, some imports may be broken. Need to:

1. Update package references in source files
2. Fix test imports
3. Update resource paths
4. Verify all cross-module references

**Command:**
```bash
# Find and replace old imports
find . -name "*.kt" -exec sed -i '' 's/BrowserCoreData/Universal/g' {} +
```

**Estimated Time:** 1-2 hours

---

### Priority 4: Run Tests

Once build issues are resolved:

1. Run all Universal tests:
```bash
./gradlew :Universal:test
```

2. Run platform-specific tests:
```bash
./gradlew :Android:testDebugUnitTest
./gradlew :iOS:iosX64Test
./gradlew :Desktop:desktopTest
```

3. Generate coverage report:
```bash
./gradlew :Universal:jacocoTestReport
```

**Target:** 90%+ coverage

**Estimated Time:** 1 hour (if no issues)

---

### Priority 5: Delete Legacy Module

Once new structure is working:

1. Backup the old module:
```bash
mv BrowserCoreData BrowserCoreData.backup
```

2. Update git:
```bash
git rm -r BrowserCoreData
git add Universal/ Android/ iOS/ Desktop/
git commit -m "refactor: migrate to IDEACODE folder structure"
```

3. Clean up build files:
```bash
./gradlew clean
```

**Estimated Time:** 30 minutes

---

### Priority 6: Create Build Scripts

Create convenience scripts for common operations:

**`scripts/build-all.sh`:**
```bash
#!/bin/bash
./gradlew :Universal:build
./gradlew :Android:build
./gradlew :iOS:build
./gradlew :Desktop:build
```

**`scripts/test-all.sh`:**
```bash
#!/bin/bash
./gradlew :Universal:test
./gradlew :Android:testDebugUnitTest
./gradlew :Desktop:desktopTest
# iOS tests require macOS
```

**`scripts/clean-all.sh`:**
```bash
#!/bin/bash
./gradlew clean
rm -rf build/
rm -rf */build/
```

**Estimated Time:** 30 minutes

---

### Priority 7: Update Documentation

1. Update main README.md with new structure
2. Create MIGRATION.md guide
3. Update CONTRIBUTING.md (if exists)
4. Add architecture diagrams
5. Document database schema

**Estimated Time:** 2-3 hours

---

### Priority 8: Set Up CI/CD

Configure automated testing and building:

**`.github/workflows/build.yml`:**
```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build Universal
        run: ./gradlew :Universal:build
      - name: Run tests
        run: ./gradlew test
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

**Estimated Time:** 2-4 hours

---

## 🎯 Quick Start (Immediate Actions)

**To get building ASAP:**

```bash
# 1. Create in-memory database implementation
touch Universal/data/repository/InMemoryBrowserRepository.kt

# 2. Temporarily disable SQLDelight in build.gradle.kts
# Comment out: id("app.cash.sqldelight")

# 3. Create basic build configs for new modules
touch Universal/build.gradle.kts
touch Android/build.gradle.kts
touch iOS/build.gradle.kts
touch Desktop/build.gradle.kts

# 4. Update settings.gradle.kts
# Add new modules

# 5. Try building
./gradlew :Universal:build --no-daemon
```

**Expected Result:** Build succeeds, tests run (with in-memory data)

---

## 📊 Estimated Timeline

| Task | Priority | Time | Dependencies |
|------|----------|------|--------------|
| Fix SQLDelight issue | P1 | 2-4h | None |
| Update build configs | P2 | 2-3h | None |
| Fix imports | P3 | 1-2h | P2 complete |
| Run tests | P4 | 1h | P1, P3 complete |
| Delete legacy module | P5 | 30min | P4 complete |
| Create scripts | P6 | 30min | P2 complete |
| Documentation | P7 | 2-3h | P5 complete |
| CI/CD setup | P8 | 2-4h | P4 complete |

**Total Estimated Time:** 12-20 hours

**Minimum Viable:** P1 + P2 + P3 = 5-9 hours

---

## ✅ Definition of Done

Project is complete when:

- [x] IDEACODE folder structure implemented
- [ ] All modules build successfully
- [ ] All tests pass (90%+ coverage)
- [ ] No deprecated APIs used
- [ ] Documentation updated
- [ ] CI/CD pipeline working
- [ ] Legacy module removed
- [ ] Git history clean

---

## 🚀 Deployment Checklist

Before releasing:

- [ ] All platforms tested (Android, iOS, Desktop)
- [ ] Performance benchmarks passed
- [ ] Security audit completed
- [ ] Database migrations tested
- [ ] Changelog updated
- [ ] Version bumped
- [ ] Git tags created
- [ ] Release notes written

---

**Last Updated:** 2025-11-17
**Status:** Ready for Priority 1 implementation
