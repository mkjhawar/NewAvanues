# Protocol: Master Repository Integration v1.0

**Version:** 1.0
**Status:** Active
**Effective Date:** 2025-11-15
**Framework Version:** 8.4

---

## 🚨 MANDATORY: All Shared Code Lives in Master Repo

**CRITICAL - AI MUST ENFORCE - NOT OPTIONAL:**

ALL shared libraries, modules, and reusable code MUST reside in the IDEACODE master repository at `/Volumes/M-Drive/Coding/ideacode/libraries/`. Individual projects (AVA, AVAConnect, etc.) MUST NOT contain duplicated code.

---

## Purpose

**Problem:** Without master repository integration:
- ❌ Code duplicated across 6 projects (1500+ lines → 500 lines)
- ❌ Bug fixes require updating 6 repositories
- ❌ Inconsistent implementations (auth works in AVA, broken in AVAConnect)
- ❌ Wasted development time (rebuild same feature 6 times)
- ❌ Larger app sizes (each app bundles same libraries)

**Solution:** Single source of truth for all shared code in master repository

---

## Architecture Overview

### Repository Structure

```
/Volumes/M-Drive/Coding/
│
├── ideacode/                           # ⭐ MASTER REPOSITORY
│   ├── libraries/                      # All shared libraries
│   │   ├── core/                       # Core utilities
│   │   │   ├── auth/                   # OAuth2 authentication
│   │   │   ├── logging/                # Structured logging
│   │   │   ├── networking/             # HTTP client
│   │   │   ├── storage/                # Key-value storage
│   │   │   └── serialization/          # JSON/Protobuf
│   │   │
│   │   ├── ui/                         # UI components
│   │   │   ├── design-system/          # Material Design 3
│   │   │   ├── charts/                 # Data visualization
│   │   │   └── animations/             # Shared animations
│   │   │
│   │   ├── data/                       # Data layer
│   │   │   ├── repository/             # Repository pattern
│   │   │   ├── cache/                  # Multi-level caching
│   │   │   └── sync/                   # Offline-first sync
│   │   │
│   │   ├── network/                    # Network layer
│   │   │   ├── api-client/             # REST API client
│   │   │   ├── graphql/                # GraphQL client
│   │   │   └── websocket/              # WebSocket client
│   │   │
│   │   ├── security/                   # Security layer
│   │   │   ├── auth/                   # Authentication
│   │   │   ├── biometric/              # Biometric auth
│   │   │   └── encryption/             # Encryption utils
│   │   │
│   │   ├── voice/                      # Voice layer (Avanues-specific)
│   │   │   ├── recognition/            # Speech-to-text
│   │   │   ├── synthesis/              # Text-to-speech
│   │   │   ├── dsl-parser/             # .vos parser
│   │   │   └── commands/               # Voice command registry
│   │   │
│   │   └── platform/                   # Platform abstractions
│   │       ├── filesystem/             # File I/O
│   │       ├── permissions/            # Permission handling
│   │       └── notifications/          # Push notifications
│   │
│   ├── .ideacode/                      # IDEACODE framework config
│   ├── ideacode-mcp/                   # MCP server
│   ├── protocols/                      # Development protocols
│   ├── programming-standards/          # Coding standards
│   └── PROJECT-REGISTRY.json           # All projects registry
│
├── ava/                                # PROJECT: AVA
│   ├── app/
│   │   ├── build.gradle.kts            # Uses ideacode/libraries/*
│   │   └── src/main/kotlin/            # App-specific code ONLY
│   └── .ideacode/                      # Project config
│
├── avaconnect/                         # PROJECT: AVAConnect
│   ├── app/
│   │   ├── build.gradle.kts            # Uses ideacode/libraries/*
│   │   └── src/main/kotlin/            # App-specific code ONLY
│   └── .ideacode/                      # Project config
│
├── Avanues/                            # PROJECT: Avanues
│   ├── app/
│   │   ├── build.gradle.kts            # Uses ideacode/libraries/*
│   │   └── src/main/kotlin/            # App-specific code ONLY
│   └── .ideacode/                      # Project config
│
├── voiceos/                            # PROJECT: VoiceOS
│   ├── app/
│   │   ├── build.gradle.kts            # Uses ideacode/libraries/*
│   │   └── src/main/kotlin/            # App-specific code ONLY
│   └── .ideacode/                      # Project config
│
├── newavanue/                          # PROJECT: NewAvanue
│   ├── app/
│   │   ├── build.gradle.kts            # Uses ideacode/libraries/*
│   │   └── src/main/kotlin/            # App-specific code ONLY
│   └── .ideacode/                      # Project config
│
└── browseravanue/                      # PROJECT: BrowserAvanue (deprecated → merged into NewAvanue)
    └── README.md                       # Migration notice
```

### Dependency Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   MASTER REPOSITORY                         │
│              /ideacode/libraries/                           │
│                                                             │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐      │
│  │  core/  │  │   ui/   │  │  data/  │  │ voice/  │      │
│  │  auth   │  │ design  │  │  cache  │  │  dsl    │      │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘      │
│       ▲            ▲            ▲            ▲             │
└───────┼────────────┼────────────┼────────────┼─────────────┘
        │            │            │            │
        │ dependency │ dependency │ dependency │
        │            │            │            │
   ┌────┴──────┬─────┴──────┬─────┴──────┬────┴──────┐
   │           │            │            │           │
   ▼           ▼            ▼            ▼           ▼
┌─────┐   ┌─────┐      ┌─────┐      ┌─────┐   ┌─────┐
│ AVA │   │AVA  │      │Voice│      │Voice│   │ New │
│     │   │Con  │      │Avan │      │ OS  │   │Avan │
└─────┘   └─────┘      └─────┘      └─────┘   └─────┘

All 5 projects depend on ideacode/libraries/*
Changes in libraries auto-propagate to all projects
```

---

## Integration Methods

### Method 1: Maven Publishing (Recommended for Production)

**Best for:** Stable releases, CI/CD pipelines, team collaboration

**Setup:**

1. **Configure Maven publishing in library `build.gradle.kts`:**

```kotlin
// ideacode/libraries/core/auth/build.gradle.kts

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}

group = "com.ideacode.core"
version = "1.0.0"

publishing {
    repositories {
        maven {
            name = "IdeacodeLocal"
            url = uri("/Volumes/M-Drive/Coding/ideacode/libraries/maven")
        }
        // Optional: Remote Maven repository
        maven {
            name = "IdeacodeRemote"
            url = uri("https://maven.ideahq.net/releases")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }

    publications {
        create<MavenPublication>("release") {
            from(components["release"])
            artifactId = "auth"
            version = "1.0.0"
        }
    }
}
```

2. **Publish library:**

```bash
cd /Volumes/M-Drive/Coding/ideacode/libraries/core/auth
./gradlew publishToMavenLocal
# OR
./gradlew publish  # To remote
```

3. **Consume in project:**

```kotlin
// ava/app/build.gradle.kts

repositories {
    mavenLocal()  // For local development
    maven {
        url = uri("/Volumes/M-Drive/Coding/ideacode/libraries/maven")
    }
}

dependencies {
    implementation("com.ideacode.core:auth:1.0.0")
    implementation("com.ideacode.ui:design-system:2.1.0")
    implementation("com.ideacode.voice:dsl-parser:1.5.0")
}
```

**Pros:**
- ✅ Versioned dependencies (explicit compatibility)
- ✅ Works with CI/CD pipelines
- ✅ Cacheable (faster builds)
- ✅ Team-friendly (published artifacts)

**Cons:**
- ❌ Requires republishing after changes
- ❌ Slower iteration during development

---

### Method 2: Composite Builds (Recommended for Development)

**Best for:** Active development, rapid iteration, local testing

**Setup:**

1. **Include library in project `settings.gradle.kts`:**

```kotlin
// ava/settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AVA"
include(":app")

// ⭐ Include libraries from master repository
includeBuild("/Volumes/M-Drive/Coding/ideacode/libraries/core/auth")
includeBuild("/Volumes/M-Drive/Coding/ideacode/libraries/ui/design-system")
includeBuild("/Volumes/M-Drive/Coding/ideacode/libraries/voice/dsl-parser")
```

2. **Use in project:**

```kotlin
// ava/app/build.gradle.kts

dependencies {
    // No version needed - uses source directly
    implementation("com.ideacode.core:auth")
    implementation("com.ideacode.ui:design-system")
    implementation("com.ideacode.voice:dsl-parser")
}
```

**Pros:**
- ✅ Instant changes (no republishing)
- ✅ Source-level debugging
- ✅ Fast iteration
- ✅ Automatic recompilation

**Cons:**
- ❌ Slower builds (recompiles libraries)
- ❌ Requires local access to master repo
- ❌ Not suitable for CI/CD

---

### Method 3: Git Submodules (NOT Recommended)

**Why NOT recommended:**
- ❌ Complex to maintain
- ❌ Easy to forget committing submodule changes
- ❌ Nested .git directories confusing
- ❌ Merge conflicts difficult

**If you must use submodules:**

```bash
cd /Volumes/M-Drive/Coding/ava
git submodule add /Volumes/M-Drive/Coding/ideacode/libraries/core/auth app/libs/auth
git submodule update --init --recursive
```

---

## Workflow: From Development to Production

### Phase 1: Active Development (Composite Builds)

```bash
# Developer working on auth library AND AVA app simultaneously

# 1. Work on library
cd /Volumes/M-Drive/Coding/ideacode/libraries/core/auth
# Edit AuthManager.kt

# 2. Test in AVA (uses composite build)
cd /Volumes/M-Drive/Coding/ava
./gradlew :app:assembleDebug
# Library auto-rebuilds with changes ✅

# 3. Commit library changes
cd /Volumes/M-Drive/Coding/ideacode/libraries/core/auth
git add .
git commit -m "feat(auth): add biometric authentication"
git push
```

### Phase 2: Pre-Release Testing (Maven Local)

```bash
# Publish to local Maven for testing

cd /Volumes/M-Drive/Coding/ideacode/libraries/core/auth
./gradlew publishToMavenLocal

# Update project to use Maven version
cd /Volumes/M-Drive/Coding/ava
# Edit settings.gradle.kts: Remove includeBuild()
# Edit build.gradle.kts: Add version number

./gradlew :app:assembleRelease
# Test with versioned dependency ✅
```

### Phase 3: Production Release (Maven Remote)

```bash
# Publish to remote Maven repository

cd /Volumes/M-Drive/Coding/ideacode/libraries/core/auth

# 1. Update version in build.gradle.kts
version = "1.1.0"  # Bump version

# 2. Update CHANGELOG.md
echo "## [1.1.0] - 2025-11-15
- Added biometric authentication
- Fixed token refresh bug" >> CHANGELOG.md

# 3. Create Git tag
git tag v1.1.0
git push origin v1.1.0

# 4. Publish to remote Maven
./gradlew publish

# 5. Update all projects to new version
cd /Volumes/M-Drive/Coding/ava
# Edit build.gradle.kts: implementation("com.ideacode.core:auth:1.1.0")

cd /Volumes/M-Drive/Coding/avaconnect
# Edit build.gradle.kts: implementation("com.ideacode.core:auth:1.1.0")

# ... (repeat for all 5 projects)
```

---

## Automated Synchronization

### MCP Tool: Sync Library Versions

**Purpose:** Automatically update all projects to latest library versions

**Signature:**
```typescript
export async function syncLibraryVersions(params: {
  libraryName: string;        // e.g., "com.ideacode.core:auth"
  version: string;            // e.g., "1.1.0"
  projects?: string[];        // Optional: specific projects
  dryRun?: boolean;           // Preview changes
}): Promise<SyncResult>
```

**Usage:**
```typescript
await ideacode_execute_code({
  code: `
    import { syncLibraryVersions } from '/wrappers/project/sync-library-versions.js'

    const result = await syncLibraryVersions({
      libraryName: "com.ideacode.core:auth",
      version: "1.1.0",
      // Auto-detects all projects from PROJECT-REGISTRY.json
      dryRun: false
    })

    return result
  `
})
```

**Output:**
```json
{
  "success": true,
  "updated_projects": 5,
  "changes": [
    {
      "project": "AVA",
      "file": "/Volumes/M-Drive/Coding/ava/app/build.gradle.kts",
      "old_version": "1.0.0",
      "new_version": "1.1.0"
    },
    {
      "project": "AVAConnect",
      "file": "/Volumes/M-Drive/Coding/avaconnect/app/build.gradle.kts",
      "old_version": "1.0.0",
      "new_version": "1.1.0"
    }
  ],
  "next_steps": [
    "Run ./gradlew clean build in each project",
    "Test all apps with new library version",
    "Commit changes"
  ]
}
```

### MCP Tool: Detect Library Usage

**Purpose:** Find all projects using a specific library

**Signature:**
```typescript
export async function detectLibraryUsage(params: {
  libraryName: string;        // e.g., "auth"
  scope?: string;             // "all" | "active" | "deprecated"
}): Promise<UsageResult>
```

**Usage:**
```typescript
await ideacode_execute_code({
  code: `
    import { detectLibraryUsage } from '/wrappers/project/detect-library-usage.js'

    const result = await detectLibraryUsage({
      libraryName: "auth",
      scope: "active"
    })

    return result
  `
})
```

**Output:**
```json
{
  "success": true,
  "library": "com.ideacode.core:auth",
  "usage": [
    {
      "project": "AVA",
      "version": "1.0.0",
      "file": "/Volumes/M-Drive/Coding/ava/app/build.gradle.kts",
      "line": 45
    },
    {
      "project": "AVAConnect",
      "version": "1.0.0",
      "file": "/Volumes/M-Drive/Coding/avaconnect/app/build.gradle.kts",
      "line": 42
    }
  ],
  "total_projects": 5,
  "versions_in_use": ["1.0.0", "0.9.0"]
}
```

---

## Version Management Strategy

### Versioning Policy

**Semantic Versioning (MAJOR.MINOR.PATCH):**
- **MAJOR (1.0.0 → 2.0.0):** Breaking changes (API signature changes)
- **MINOR (1.0.0 → 1.1.0):** New features (backward compatible)
- **PATCH (1.0.0 → 1.0.1):** Bug fixes (backward compatible)

**Examples:**
```kotlin
// PATCH: Bug fix (1.0.0 → 1.0.1)
- fun authenticate(): AuthResult  // Before
+ fun authenticate(): AuthResult  // After (fixed token refresh bug)

// MINOR: New feature (1.0.1 → 1.1.0)
+ fun authenticateWithBiometric(): AuthResult  // Added new function

// MAJOR: Breaking change (1.1.0 → 2.0.0)
- fun authenticate(): AuthResult                    // Before
+ suspend fun authenticate(): AuthResult           // After (now suspend!)
```

### Version Compatibility Matrix

| Library Version | Min Project Version | Max Project Version | Status |
|-----------------|---------------------|---------------------|--------|
| auth:2.0.0 | 8.4 | latest | ✅ Current |
| auth:1.1.0 | 8.0 | 8.3 | ⚠️ Deprecated |
| auth:1.0.0 | 7.0 | 7.9 | ❌ Unsupported |

**Deprecation Timeline:**
1. **v2.0.0 released** → v1.x marked as deprecated
2. **+3 months** → v1.x receives critical bug fixes only
3. **+6 months** → v1.x fully unsupported (no updates)

---

## Migration from Monolithic to Modular

### Step-by-Step Migration Process

**Scenario:** Extract `AuthManager` from AVA, AVAConnect, Avanues into shared library

#### Step 1: Identify Duplication

```bash
# Use MCP tool to detect code duplication
await ideacode_execute_code({
  code: `
    import { detectDuplication } from '/wrappers/quality/detect-duplication.js'

    const result = await detectDuplication({
      projects: [
        '/Volumes/M-Drive/Coding/ava',
        '/Volumes/M-Drive/Coding/avaconnect',
        '/Volumes/M-Drive/Coding/Avanues'
      ],
      threshold: 0.85,
      minLines: 50
    })

    return result.candidates
      .filter(c => c.files.some(f => f.includes('AuthManager')))
  `
})
```

**Output:**
```json
{
  "candidates": [
    {
      "similarity": 0.95,
      "files": [
        "/Volumes/M-Drive/Coding/ava/app/src/main/kotlin/com/ava/auth/AuthManager.kt",
        "/Volumes/M-Drive/Coding/avaconnect/app/src/main/kotlin/com/avaconnect/auth/AuthManager.kt",
        "/Volumes/M-Drive/Coding/Avanues/app/src/main/kotlin/com/avanues/auth/AuthManager.kt"
      ],
      "lines": 450,
      "recommendation": "Extract to libraries/core/auth"
    }
  ]
}
```

#### Step 2: Create Shared Library

```bash
cd /Volumes/M-Drive/Coding/ideacode

# Use MCP tool to create library scaffold
await ideacode_execute_code({
  code: `
    import { createLibrary } from '/wrappers/project/create-library.js'

    const result = await createLibrary({
      category: 'core',
      name: 'auth',
      description: 'OAuth2 authentication with PKCE flow and biometric support',
      kmp: true,
      platforms: ['android', 'ios']
    })

    return result
  `
})
```

#### Step 3: Extract Code

```bash
# Copy AuthManager from AVA (choose most complete version)
cp /Volumes/M-Drive/Coding/ava/app/src/main/kotlin/com/ava/auth/AuthManager.kt \
   /Volumes/M-Drive/Coding/ideacode/libraries/core/auth/src/commonMain/kotlin/com/ideacode/auth/api/AuthManager.kt

# Refactor to use common package
# Change: package com.ava.auth → package com.ideacode.auth.api
```

#### Step 4: Make Platform-Agnostic (KMP)

```kotlin
// Before: Android-specific
class AuthManager(
    private val context: Context  // ❌ Android-specific!
) {
    fun authenticate() {
        val intent = Intent(context, LoginActivity::class.java)
        context.startActivity(intent)
    }
}

// After: KMP with expect/actual
// commonMain/kotlin/com/ideacode/auth/api/AuthManager.kt
expect class PlatformAuthContext

interface AuthManager {
    suspend fun authenticate(provider: OAuthProvider): AuthResult
}

// androidMain/kotlin/com/ideacode/auth/internal/AndroidAuthManager.kt
actual class PlatformAuthContext(val context: Context)

class AndroidAuthManager(
    private val platformContext: PlatformAuthContext
) : AuthManager {
    override suspend fun authenticate(provider: OAuthProvider): AuthResult {
        val intent = Intent(platformContext.context, LoginActivity::class.java)
        platformContext.context.startActivity(intent)
        // ...
    }
}

// iosMain/kotlin/com/ideacode/auth/internal/IOSAuthManager.kt
actual class PlatformAuthContext(val viewController: UIViewController)

class IOSAuthManager(
    private val platformContext: PlatformAuthContext
) : AuthManager {
    override suspend fun authenticate(provider: OAuthProvider): AuthResult {
        // iOS-specific implementation
    }
}
```

#### Step 5: Publish Library

```bash
cd /Volumes/M-Drive/Coding/ideacode/libraries/core/auth

# 1. Write tests
# 2. Update README.md
# 3. Update CHANGELOG.md

# 4. Publish to local Maven
./gradlew publishToMavenLocal

# 5. Tag version
git add .
git commit -m "feat(auth): initial release of shared authentication library"
git tag v1.0.0
git push origin v1.0.0
```

#### Step 6: Update Projects

```kotlin
// AVA: app/build.gradle.kts
dependencies {
    // Remove old implementation
-   // AuthManager code was inline

    // Add library dependency
+   implementation("com.ideacode.core:auth:1.0.0")
}
```

```kotlin
// AVA: app/src/main/kotlin/com/ava/MainActivity.kt
- import com.ava.auth.AuthManager
+ import com.ideacode.auth.api.AuthManager
+ import com.ideacode.auth.internal.AndroidAuthManager
+ import com.ideacode.auth.internal.PlatformAuthContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

-       val authManager = AuthManager(this)
+       val authManager = AndroidAuthManager(PlatformAuthContext(this))

        // Rest of code unchanged ✅
    }
}
```

#### Step 7: Delete Duplicated Code

```bash
# Delete old AuthManager from AVA
rm /Volumes/M-Drive/Coding/ava/app/src/main/kotlin/com/ava/auth/AuthManager.kt

# Delete old AuthManager from AVAConnect
rm /Volumes/M-Drive/Coding/avaconnect/app/src/main/kotlin/com/avaconnect/auth/AuthManager.kt

# Delete old AuthManager from Avanues
rm /Volumes/M-Drive/Coding/Avanues/app/src/main/kotlin/com/avanues/auth/AuthManager.kt
```

#### Step 8: Test All Projects

```bash
# Test AVA
cd /Volumes/M-Drive/Coding/ava
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug

# Test AVAConnect
cd /Volumes/M-Drive/Coding/avaconnect
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug

# Test Avanues
cd /Volumes/M-Drive/Coding/Avanues
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

#### Step 9: Commit Changes

```bash
cd /Volumes/M-Drive/Coding/ava
git add .
git commit -m "refactor: migrate to shared auth library (ideacode/core/auth:1.0.0)

- Removed inline AuthManager implementation (450 lines)
- Added dependency on com.ideacode.core:auth:1.0.0
- Updated imports to use shared library

Benefits:
- Code reduction: 450 lines → 5 lines import
- Single source of truth for authentication
- Shared bug fixes and features across all apps"

git push

# Repeat for AVAConnect and Avanues
```

---

## Quality Gates

### Before Publishing Library

- [ ] **API Review:** Public API is minimal, cohesive, documented
- [ ] **Testing:** 90%+ coverage, all platforms tested
- [ ] **Documentation:** README, API reference, examples
- [ ] **Version:** Semantic versioning, CHANGELOG updated
- [ ] **Dependencies:** Minimal, stable versions only
- [ ] **Security:** OWASP Top 10 check (if security-related)

### Before Consuming Library

- [ ] **Version Check:** Using latest stable version
- [ ] **Compatibility:** Library version compatible with project
- [ ] **Dependencies:** No transitive dependency conflicts
- [ ] **Testing:** Integration tests pass
- [ ] **Documentation:** Usage examples reviewed

---

## Troubleshooting

### Problem: Version Conflict

**Error:**
```
Dependency resolution failed:
  - com.ideacode.core:auth:1.0.0 (required by app)
  - com.ideacode.core:auth:2.0.0 (required by design-system)
```

**Solution:**
```kotlin
// Force specific version
dependencies {
    implementation("com.ideacode.core:auth:2.0.0") {
        force = true
    }
}
```

### Problem: Library Not Found

**Error:**
```
Could not find com.ideacode.core:auth:1.0.0
```

**Solution:**
```bash
# 1. Check Maven repository exists
ls -la /Volumes/M-Drive/Coding/ideacode/libraries/maven/

# 2. Republish library
cd /Volumes/M-Drive/Coding/ideacode/libraries/core/auth
./gradlew publishToMavenLocal

# 3. Verify in project settings.gradle.kts
repositories {
    mavenLocal()
    maven { url = uri("/Volumes/M-Drive/Coding/ideacode/libraries/maven") }
}
```

### Problem: Composite Build Not Working

**Error:**
```
Included build '/path/to/library' does not exist
```

**Solution:**
```kotlin
// settings.gradle.kts - Use absolute path
includeBuild("/Volumes/M-Drive/Coding/ideacode/libraries/core/auth")
```

---

## References

- **Protocol-Modular-Architecture-v1.0.md** - Modular design principles
- **Protocol-Git-Branch-Hierarchy-v1.0.md** - Branch organization
- **Kotlin Multiplatform:** https://kotlinlang.org/docs/multiplatform.html
- **Gradle Composite Builds:** https://docs.gradle.org/current/userguide/composite_builds.html

---

## Changelog

### v1.0 (2025-11-15)
- Initial protocol creation
- Defined master repository structure
- Added 3 integration methods (Maven, Composite, Submodules)
- Created workflow from development to production
- Added automated synchronization tools
- Created migration checklist

---

**Author:** Manoj Jhawar
**Email:** manoj@ideahq.net
**License:** Proprietary

---

**IDEACODE v8.4** - Master repository integration for shared libraries across all projects
