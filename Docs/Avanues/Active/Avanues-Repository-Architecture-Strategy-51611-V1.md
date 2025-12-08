# Avanues Repository Architecture Strategy
**Date**: October 28, 2025
**Status**: STRATEGIC DECISION DOCUMENT
**Author**: Claude Code Analysis

---

## EXECUTIVE SUMMARY

This document analyzes repository architecture options for the Avanues ecosystem and provides a clear recommendation based on industry best practices, team workflow, and App Store requirements.

**RECOMMENDATION**: **Hybrid Monorepo** (one repo, multiple independently deployable apps)

**Reasoning**:
- Shared code (AvaUI, AvaCode) used by all apps
- Independent App Store releases
- Simplified CI/CD
- Single source of truth for versioning

---

## 1. REPOSITORY ARCHITECTURE OPTIONS

### Option 1: Pure Monorepo (RECOMMENDED ✅)

**Structure**:
```
avanues/  (single git repository)
├── .github/
│   └── workflows/
│       ├── build-core.yml
│       ├── build-ai-app.yml
│       ├── build-browser-app.yml
│       └── build-notes-app.yml
├── shared/                           # Shared infrastructure
│   ├── avaui/                      # AvaUI runtime (KMP library)
│   ├── avacode/                    # AvaCode generator (KMP library)
│   ├── voiceos/                      # Voice command system
│   └── capability-sdk/               # Capability system SDK
├── apps/
│   ├── core/                         # Avanues Core App
│   │   ├── android/
│   │   ├── ios/
│   │   ├── web/
│   │   └── build.gradle.kts
│   ├── ai/                           # AI App (separate APK/IPA)
│   │   ├── android/
│   │   ├── ios/
│   │   ├── capabilities.voiceapp
│   │   └── build.gradle.kts
│   ├── browser/                      # Browser App
│   │   ├── android/
│   │   ├── ios/
│   │   └── build.gradle.kts
│   └── notes/                        # Notes App
│       ├── android/
│       ├── ios/
│       └── build.gradle.kts
├── manifests/                        # Manifest registry
│   ├── registry.json
│   └── schemas/
├── docs/
├── examples/
├── settings.gradle.kts               # Declares all modules
└── build.gradle.kts                  # Root build config
```

**Gradle Configuration**:
```kotlin
// settings.gradle.kts
rootProject.name = "Avanues"

// Shared libraries (KMP)
include(":shared:avaui")
include(":shared:avacode")
include(":shared:voiceos")
include(":shared:capability-sdk")

// Apps (independently deployable)
include(":apps:core")
include(":apps:ai")
include(":apps:browser")
include(":apps:notes")
```

**Pros**:
- ✅ **Single source of truth**: All code in one place
- ✅ **Atomic commits**: Change shared library + apps in one commit
- ✅ **Simplified dependency management**: Shared libraries always in sync
- ✅ **Easy refactoring**: Refactor AvaUI, update all apps instantly
- ✅ **Single CI/CD**: One pipeline, multiple deployments
- ✅ **Code sharing**: Zero duplication
- ✅ **Version control**: Single version tag for entire ecosystem

**Cons**:
- ⚠️ Large repository size (~500MB-1GB)
- ⚠️ Clone time (~30-60 seconds)
- ⚠️ Need branch protection rules per app

**Examples in Industry**:
- Google (entire Android OS in one repo)
- Meta (React, React Native, Metro in one repo)
- Microsoft (Windows components in one repo)

---

### Option 2: Multi-Repo

**Structure**:
```
avanues-core/          (separate git repo)
avanues-shared/        (separate git repo - published to Maven)
avanues-ai/            (separate git repo)
avanues-browser/       (separate git repo)
avanues-notes/         (separate git repo)
avanues-manifests/     (separate git repo)
```

**Dependencies**:
```kotlin
// In avanues-ai/build.gradle.kts
dependencies {
    implementation("com.avanues:avaui:1.2.3")  // From Maven
    implementation("com.avanues:avacode:1.2.3")
    implementation("com.avanues:capability-sdk:1.2.3")
}
```

**Pros**:
- ✅ Smaller individual repos
- ✅ Independent access control per repo
- ✅ Easier to open-source individual apps
- ✅ Clearer ownership boundaries

**Cons**:
- ❌ **Dependency hell**: Coordinating versions across repos
- ❌ **Breaking changes**: Update AvaUI → must update 4+ repos
- ❌ **Complex CI/CD**: Multiple pipelines, manual coordination
- ❌ **Slow iteration**: Publish shared lib → wait → update apps
- ❌ **Version drift**: Apps using different AvaUI versions
- ❌ **Hard to refactor**: Cross-repo changes require multiple PRs

**Examples in Industry**:
- AWS SDKs (separate repos per language)
- Kubernetes ecosystem (separate repos per component)

---

### Option 3: Hybrid (Monorepo + External Repos)

**Structure**:
```
avanues/                        (main monorepo)
├── shared/
├── apps/core/
├── apps/ai/
├── apps/browser/
└── apps/notes/

avanues-community-apps/         (separate repo)
└── third-party apps by other developers
```

**Pros**:
- ✅ Best of both worlds
- ✅ Core ecosystem in one repo
- ✅ Third-party apps in separate repos

**Cons**:
- ⚠️ Requires publishing shared libraries to Maven
- ⚠️ Two-tier development experience

---

## 2. DECISION MATRIX

| Criteria | Pure Monorepo | Multi-Repo | Hybrid | Weight |
|----------|---------------|------------|--------|--------|
| **Development Speed** | ⭐⭐⭐⭐⭐ Fast | ⭐⭐ Slow | ⭐⭐⭐⭐ Fast | HIGH |
| **Refactoring Ease** | ⭐⭐⭐⭐⭐ Easy | ⭐ Hard | ⭐⭐⭐⭐ Easy | HIGH |
| **Version Management** | ⭐⭐⭐⭐⭐ Simple | ⭐⭐ Complex | ⭐⭐⭐ Moderate | HIGH |
| **CI/CD Complexity** | ⭐⭐⭐⭐ Simple | ⭐⭐ Complex | ⭐⭐⭐ Moderate | MED |
| **Clone Time** | ⭐⭐⭐ 30-60s | ⭐⭐⭐⭐⭐ 5-10s | ⭐⭐⭐⭐ 15-30s | LOW |
| **Third-party Contrib** | ⭐⭐⭐ Moderate | ⭐⭐⭐⭐⭐ Easy | ⭐⭐⭐⭐ Easy | MED |
| **Access Control** | ⭐⭐⭐ CODEOWNERS | ⭐⭐⭐⭐⭐ Per repo | ⭐⭐⭐⭐ Mixed | LOW |

**Weighted Score**:
- **Pure Monorepo**: 4.6/5 ⭐⭐⭐⭐⭐
- **Multi-Repo**: 2.4/5 ⭐⭐
- **Hybrid**: 3.8/5 ⭐⭐⭐⭐

**Winner**: **Pure Monorepo** ✅

---

## 3. RECOMMENDED STRUCTURE: PURE MONOREPO

### 3.1 Directory Layout

```
avanues/
├── .github/
│   ├── workflows/
│   │   ├── ci-shared-libraries.yml          # Build/test shared libs
│   │   ├── ci-core-app.yml                  # Build Avanues Core
│   │   ├── ci-ai-app.yml                    # Build AI App
│   │   ├── ci-browser-app.yml               # Build Browser App
│   │   ├── ci-notes-app.yml                 # Build Notes App
│   │   ├── cd-core-app.yml                  # Deploy Core to stores
│   │   ├── cd-ai-app.yml                    # Deploy AI to stores
│   │   └── release.yml                      # Create release tags
│   ├── CODEOWNERS                           # Define ownership
│   └── dependabot.yml
│
├── shared/                                   # Shared KMP libraries
│   ├── avaui/                             # UI runtime
│   │   ├── src/
│   │   │   ├── commonMain/
│   │   │   ├── androidMain/
│   │   │   ├── iosMain/
│   │   │   └── jvmMain/
│   │   ├── build.gradle.kts
│   │   └── README.md
│   ├── avacode/                           # Code generator
│   │   ├── src/commonMain/
│   │   ├── build.gradle.kts
│   │   └── README.md
│   ├── voiceos/                             # Voice command system
│   │   ├── src/commonMain/
│   │   └── build.gradle.kts
│   ├── capability-sdk/                      # NEW: Capability system
│   │   ├── src/commonMain/
│   │   │   ├── CapabilityDescriptor.kt
│   │   │   ├── IPCBridge.kt
│   │   │   ├── ManifestParser.kt
│   │   │   └── CapabilityRegistry.kt
│   │   └── build.gradle.kts
│   ├── component-libraries/                 # UI components
│   │   ├── ColorPicker/
│   │   ├── TextField/
│   │   ├── Checkbox/
│   │   ├── ListView/
│   │   ├── Database/
│   │   └── Dialog/
│   └── testing-utils/                       # Shared test utilities
│       └── build.gradle.kts
│
├── apps/
│   ├── core/                                # Avanues Core App
│   │   ├── android/
│   │   │   ├── src/main/
│   │   │   │   ├── AndroidManifest.xml
│   │   │   │   └── kotlin/
│   │   │   │       └── com/augmentalis/avanues/
│   │   │   │           ├── MainActivity.kt
│   │   │   │           ├── CapabilityDiscoveryEngine.kt
│   │   │   │           └── MicroAppRuntime.kt
│   │   │   └── build.gradle.kts
│   │   ├── ios/
│   │   │   ├── Avanues/
│   │   │   │   ├── ContentView.swift
│   │   │   │   ├── CapabilityScanner.swift
│   │   │   │   └── MicroAppRunner.swift
│   │   │   └── Avanues.xcodeproj
│   │   ├── web/
│   │   │   ├── src/
│   │   │   └── package.json
│   │   ├── shared/                          # Shared business logic
│   │   │   └── src/commonMain/
│   │   └── README.md
│   │
│   ├── ai/                                  # AI App (50MB)
│   │   ├── android/
│   │   │   ├── src/main/
│   │   │   │   ├── AndroidManifest.xml
│   │   │   │   ├── assets/
│   │   │   │   │   └── capabilities.voiceapp
│   │   │   │   └── kotlin/
│   │   │   │       └── com/avanues/ai/
│   │   │   │           ├── AICapabilityService.kt
│   │   │   │           ├── SentimentAnalyzer.kt
│   │   │   │           ├── EntityExtractor.kt
│   │   │   │           └── ChatEngine.kt
│   │   │   └── build.gradle.kts
│   │   ├── ios/
│   │   │   └── AvanuesAI/
│   │   ├── models/                          # ML models
│   │   │   └── sentiment-model.tflite
│   │   └── README.md
│   │
│   ├── browser/                             # Browser App (40MB)
│   │   ├── android/
│   │   │   ├── src/main/
│   │   │   │   ├── assets/capabilities.voiceapp
│   │   │   │   └── kotlin/
│   │   │   │       └── com/avanues/browser/
│   │   │   │           ├── BrowserCapabilityService.kt
│   │   │   │           ├── WebRenderer.kt
│   │   │   │           └── SearchEngine.kt
│   │   │   └── build.gradle.kts
│   │   ├── ios/
│   │   └── README.md
│   │
│   └── notes/                               # Notes App (20MB)
│       ├── android/
│       │   ├── src/main/
│       │   │   ├── assets/capabilities.voiceapp
│       │   │   └── kotlin/
│       │   │       └── com/avanues/notes/
│       │   │           ├── NotesCapabilityService.kt
│       │   │           ├── NotesRepository.kt
│       │   │           └── MarkdownRenderer.kt
│       │   └── build.gradle.kts
│       ├── ios/
│       └── README.md
│
├── manifests/                               # Manifest registry
│   ├── registry.json                        # Index of all apps
│   ├── schemas/
│   │   └── capability-manifest.schema.json
│   └── apps/
│       ├── ai-1.0.0.voiceapp
│       ├── browser-1.0.0.voiceapp
│       └── notes-1.0.0.voiceapp
│
├── docs/
│   ├── Active/                              # Current session docs
│   ├── architecture/
│   ├── developer-guides/
│   └── api-reference/
│
├── examples/
│   ├── micro-apps/                          # Example .vos files
│   │   ├── smart-note-taker.vos
│   │   ├── sentiment-analyzer.vos
│   │   └── web-clipper.vos
│   └── capability-apps/                     # Third-party app template
│       └── template/
│
├── scripts/
│   ├── build-all-apps.sh
│   ├── deploy-to-testflight.sh
│   ├── generate-manifests.sh
│   └── update-registry.sh
│
├── .gitignore
├── settings.gradle.kts                      # Gradle multi-project config
├── build.gradle.kts                         # Root build config
├── gradle.properties
└── README.md
```

### 3.2 Gradle Configuration

**settings.gradle.kts**:
```kotlin
rootProject.name = "Avanues"

// ==========================================
// SHARED LIBRARIES (KMP)
// ==========================================
include(":shared:avaui")
include(":shared:avacode")
include(":shared:voiceos")
include(":shared:capability-sdk")

// Component libraries
include(":shared:component-libraries:ColorPicker")
include(":shared:component-libraries:TextField")
include(":shared:component-libraries:Checkbox")
include(":shared:component-libraries:ListView")
include(":shared:component-libraries:Database")
include(":shared:component-libraries:Dialog")

// Testing utilities
include(":shared:testing-utils")

// ==========================================
// APPLICATIONS (Independently deployable)
// ==========================================

// Avanues Core App
include(":apps:core:android")
include(":apps:core:shared")

// AI App
include(":apps:ai:android")

// Browser App
include(":apps:browser:android")

// Notes App
include(":apps:notes:android")

// Optional: iOS modules (if using KMP for iOS)
include(":apps:core:ios")
include(":apps:ai:ios")
```

**Root build.gradle.kts**:
```kotlin
plugins {
    kotlin("multiplatform") version "1.9.20" apply false
    kotlin("android") version "1.9.20" apply false
    id("com.android.application") version "8.1.0" apply false
    id("com.android.library") version "8.1.0" apply false
}

subprojects {
    repositories {
        google()
        mavenCentral()
    }

    // Common configuration for all modules
    afterEvaluate {
        if (plugins.hasPlugin("com.android.library") ||
            plugins.hasPlugin("com.android.application")) {
            extensions.configure<com.android.build.gradle.BaseExtension> {
                compileSdkVersion(34)

                defaultConfig {
                    minSdk = 26
                    targetSdk = 34
                }
            }
        }
    }
}

// Task to build all apps
tasks.register("buildAllApps") {
    dependsOn(
        ":apps:core:android:assembleRelease",
        ":apps:ai:android:assembleRelease",
        ":apps:browser:android:assembleRelease",
        ":apps:notes:android:assembleRelease"
    )
}
```

---

## 4. WHAT STAYS IN CORE VS SEPARATE APPS

### 4.1 Avanues Core App (~30MB)

**MUST Include**:
- ✅ AvaUI runtime
- ✅ AvaCode parser + generators
- ✅ VoiceOS command system
- ✅ Capability discovery engine (NEW)
- ✅ Capability registry (NEW)
- ✅ IPC bridge (NEW)
- ✅ Micro-app runtime (NEW)
- ✅ Micro-app editor UI
- ✅ Manifest registry sync
- ✅ Basic components (Text, Button, Container)

**Should NOT Include**:
- ❌ AI/ML models
- ❌ Browser rendering engine
- ❌ Note storage/sync
- ❌ Advanced components (if not core)

**Package Name**: `com.augmentalis.avanues`

### 4.2 AI App (~50MB)

**Includes**:
- ✅ Capability service (IPC endpoint)
- ✅ Sentiment analysis models
- ✅ Entity extraction models
- ✅ LLM interface (API calls or local model)
- ✅ capabilities.voiceapp manifest
- ✅ Dependency: capability-sdk

**Does NOT Include**:
- ❌ AvaUI (links to shared library)
- ❌ AvaCode (not needed)

**Package Name**: `com.avanues.ai`

### 4.3 Browser App (~40MB)

**Includes**:
- ✅ Capability service
- ✅ WebView/rendering engine
- ✅ Search functionality
- ✅ capabilities.voiceapp manifest
- ✅ Dependency: capability-sdk

**Package Name**: `com.avanues.browser`

### 4.4 Notes App (~20MB)

**Includes**:
- ✅ Capability service
- ✅ Note storage (Room/SQLite)
- ✅ Markdown renderer
- ✅ capabilities.voiceapp manifest
- ✅ Dependency: capability-sdk

**Package Name**: `com.avanues.notes`

---

## 5. BRANCHING STRATEGY

### 5.1 Main Branches

```
main                    # Production-ready code
├── develop             # Integration branch
├── release/v1.0.0      # Release preparation
└── hotfix/core-crash   # Emergency fixes
```

### 5.2 Feature Branches

```
feature/capability-discovery    # New feature in core
feature/ai-sentiment-v2         # New feature in AI app
feature/browser-bookmarks       # New feature in Browser app
```

### 5.3 Branch Protection Rules

**main**:
- ✅ Require PR reviews (2 approvals)
- ✅ Require status checks (all CI must pass)
- ✅ No direct pushes

**develop**:
- ✅ Require PR reviews (1 approval)
- ✅ Require status checks
- ✅ No direct pushes

**Feature branches**:
- No restrictions (developer freedom)

### 5.4 Release Process

1. **Create release branch** from `develop`:
   ```bash
   git checkout develop
   git pull
   git checkout -b release/v1.2.0
   ```

2. **Version bump** all apps:
   ```bash
   # Update version codes in each app's build.gradle.kts
   ./scripts/bump-version.sh 1.2.0
   ```

3. **Build all apps**:
   ```bash
   ./gradlew buildAllApps
   ```

4. **Test on devices**:
   - Core app on Android/iOS
   - AI app on Android/iOS
   - Browser app on Android/iOS
   - Notes app on Android/iOS

5. **Merge to main**:
   ```bash
   git checkout main
   git merge release/v1.2.0
   git tag -a v1.2.0 -m "Release 1.2.0"
   git push origin main --tags
   ```

6. **Deploy**:
   - CI/CD automatically deploys tagged releases
   - Core app → App Store + Play Store
   - AI app → App Store + Play Store
   - Browser app → App Store + Play Store
   - Notes app → App Store + Play Store

---

## 6. CI/CD CONFIGURATION

### 6.1 GitHub Actions Workflow

**.github/workflows/ci-core-app.yml**:
```yaml
name: CI - Core App

on:
  push:
    branches: [main, develop]
    paths:
      - 'apps/core/**'
      - 'shared/**'
  pull_request:
    branches: [main, develop]
    paths:
      - 'apps/core/**'
      - 'shared/**'

jobs:
  build-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build Core App (Android)
        run: ./gradlew :apps:core:android:assembleDebug

      - name: Run tests
        run: ./gradlew :apps:core:android:testDebugUnitTest

      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: core-app-debug.apk
          path: apps/core/android/build/outputs/apk/debug/

  build-ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build Core App (iOS)
        run: |
          cd apps/core/ios
          xcodebuild -scheme Avanues -configuration Debug
```

**.github/workflows/cd-core-app.yml**:
```yaml
name: CD - Deploy Core App

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build Release APK
        run: ./gradlew :apps:core:android:assembleRelease

      - name: Sign APK
        uses: r0adkll/sign-android-release@v1
        with:
          releaseDirectory: apps/core/android/build/outputs/apk/release
          signingKeyBase64: ${{ secrets.SIGNING_KEY }}
          alias: ${{ secrets.ALIAS }}
          keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}

      - name: Upload to Play Store
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.SERVICE_ACCOUNT_JSON }}
          packageName: com.augmentalis.avanues
          releaseFiles: apps/core/android/build/outputs/apk/release/*.apk
          track: production

  deploy-ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build and upload to TestFlight
        run: |
          cd apps/core/ios
          fastlane beta
```

---

## 7. DEPENDENCY MANAGEMENT

### 7.1 Version Catalog

**gradle/libs.versions.toml**:
```toml
[versions]
kotlin = "1.9.20"
compose = "1.5.0"
coroutines = "1.7.3"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
compose-ui = { module = "androidx.compose.ui:ui", version.ref = "compose" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
android-application = { id = "com.android.application", version = "8.1.0" }
```

### 7.2 Internal Dependencies

**apps/core/android/build.gradle.kts**:
```kotlin
dependencies {
    // Shared libraries (from same repo)
    implementation(project(":shared:avaui"))
    implementation(project(":shared:avacode"))
    implementation(project(":shared:voiceos"))
    implementation(project(":shared:capability-sdk"))

    // Component libraries
    implementation(project(":shared:component-libraries:TextField"))
    implementation(project(":shared:component-libraries:Checkbox"))
}
```

**apps/ai/android/build.gradle.kts**:
```kotlin
dependencies {
    // Only need capability SDK (not full AvaUI/AvaCode)
    implementation(project(":shared:capability-sdk"))

    // AI-specific libraries
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
}
```

---

## 8. TEAM WORKFLOW

### 8.1 CODEOWNERS

**.github/CODEOWNERS**:
```
# Global owners
* @augmentalis-team

# Shared libraries
/shared/avaui/ @avaui-team
/shared/avacode/ @avacode-team
/shared/capability-sdk/ @platform-team

# Apps
/apps/core/ @core-app-team
/apps/ai/ @ai-team
/apps/browser/ @browser-team
/apps/notes/ @notes-team

# Docs
/docs/ @docs-team
```

### 8.2 Development Workflow

**Developer working on AI App**:
```bash
# 1. Clone repo (one time)
git clone https://github.com/augmentalis/avanues.git
cd avanues

# 2. Create feature branch
git checkout develop
git pull
git checkout -b feature/ai-entity-extraction-v2

# 3. Make changes (only in apps/ai/)
cd apps/ai
# ... edit files ...

# 4. Build and test
cd ../../
./gradlew :apps:ai:android:assembleDebug
./gradlew :apps:ai:android:testDebugUnitTest

# 5. Commit and push
git add apps/ai/
git commit -m "feat(ai): improve entity extraction accuracy"
git push origin feature/ai-entity-extraction-v2

# 6. Create PR
# (GitHub UI or gh cli)
```

**Developer working on shared library**:
```bash
# Same process, but changes affect multiple apps
git checkout -b feature/avaui-new-component

# Edit shared library
cd shared/avaui/
# ... add new component ...

# Test with all apps
./gradlew :apps:core:android:assembleDebug
./gradlew :apps:ai:android:assembleDebug
./gradlew :apps:browser:android:assembleDebug
./gradlew :apps:notes:android:assembleDebug

# All apps must build successfully!
```

---

## 9. ADVANTAGES OF THIS APPROACH

### 9.1 For Development

✅ **Single clone**: `git clone` once, work on any app
✅ **Instant updates**: Change AvaUI, all apps use new version immediately
✅ **Atomic refactoring**: Refactor + update all usages in one commit
✅ **Single CI/CD**: One pipeline configuration
✅ **Easy testing**: Test interactions between apps locally

### 9.2 For App Store

✅ **Independent releases**: Core 1.2, AI 1.3, Browser 1.1 (different versions OK)
✅ **Independent review**: Each app reviewed separately
✅ **Independent updates**: Update AI app without updating Core
✅ **Separate binaries**: 4 separate APK/IPA files

### 9.3 For Users

✅ **Download only what they need**: Core (30MB) + AI (50MB) = 80MB total
✅ **Optional apps**: Browser and Notes only if needed
✅ **Smaller updates**: AI update (50MB) doesn't require Core update

---

## 10. MIGRATION PLAN

### Phase 1: Restructure Current Repo (Week 1)

```bash
# Current structure
avanues/
├── runtime/libraries/AvaUI/
├── runtime/libraries/AvaCode/
└── runtime/libraries/ColorPicker/

# New structure
avanues/
├── shared/avaui/                 # Move runtime/libraries/AvaUI → here
├── shared/avacode/               # Move runtime/libraries/AvaCode → here
├── shared/component-libraries/
│   └── ColorPicker/                # Move runtime/libraries/ColorPicker → here
└── apps/
    └── core/                       # Create new
```

**Migration Script**:
```bash
#!/bin/bash
# scripts/migrate-to-monorepo.sh

echo "Migrating to monorepo structure..."

# Create directories
mkdir -p shared apps/core apps/ai apps/browser apps/notes manifests

# Move shared libraries
git mv runtime/libraries/AvaUI shared/avaui
git mv runtime/libraries/AvaCode shared/avacode

# Move component libraries
mkdir -p shared/component-libraries
git mv runtime/libraries/ColorPicker shared/component-libraries/
git mv runtime/libraries/TextField shared/component-libraries/
# ... etc

# Update settings.gradle.kts
# (manual edit)

# Commit
git add .
git commit -m "refactor: migrate to monorepo structure"
```

### Phase 2: Create App Modules (Week 2)

- [ ] Create `apps/core/android/`
- [ ] Create `apps/ai/android/`
- [ ] Create `apps/browser/android/`
- [ ] Create `apps/notes/android/`
- [ ] Update `settings.gradle.kts`
- [ ] Configure build.gradle.kts for each app

### Phase 3: Set Up CI/CD (Week 3)

- [ ] Create GitHub Actions workflows
- [ ] Configure signing keys
- [ ] Test build pipelines
- [ ] Set up Play Store deployment

---

## 11. FINAL RECOMMENDATION

**Use Pure Monorepo** ✅

**Reasoning**:
1. **Shared code dominates** - AvaUI, AvaCode used by all apps
2. **Fast iteration** - Change shared lib, all apps updated instantly
3. **Proven at scale** - Google, Meta, Microsoft use monorepos
4. **Independent releases** - Still deploy apps separately to stores
5. **Team efficiency** - Single checkout, single CI/CD

**NOT Multi-Repo because**:
- Publishing shared libraries to Maven adds overhead
- Version coordination nightmare
- Slower development cycle
- Harder to refactor

---

## 12. SUMMARY

| Question | Answer |
|----------|--------|
| **What structure?** | Pure monorepo (one repo, multiple apps) |
| **What stays in core?** | AvaUI, AvaCode, VoiceOS, capability system |
| **What moves to separate apps?** | AI models, browser engine, note storage |
| **Branches?** | No per-app branches - feature branches + main/develop |
| **Independent releases?** | Yes - each app has own version + deployment |
| **Clone time?** | ~30-60s (acceptable for 500MB-1GB repo) |
| **CI/CD?** | One repo, multiple workflows (per app) |

**Next Step**: Proceed with IdeaCode 3.0 refactoring based on this structure!

---

**End of Document**
