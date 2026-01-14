# Protocol: Modular Architecture v1.0

**Version:** 1.0
**Status:** Active
**Effective Date:** 2025-11-15
**Framework Version:** 8.4

---

## 🚨 MANDATORY: No Monolithic Code

**CRITICAL - AI MUST ENFORCE - NOT OPTIONAL:**

ALL code MUST be modular and library-driven. Monolithic implementations are PROHIBITED.

---

## Purpose

**Problem:** Monolithic code creates:
- ❌ Code duplication across 6 projects (AVA, AVAConnect, Avanues, VoiceOS, NewAvanue, IDEACODE)
- ❌ Inconsistent implementations of same functionality
- ❌ Maintenance nightmare (fix in one place, breaks in 5 others)
- ❌ Unable to share improvements across projects
- ❌ Larger app sizes (duplicated libraries)

**Solution:** Modular, library-driven architecture with master repository integration

---

## Architecture Principles

### Principle 1: Module-First Mindset

**MANDATORY:** Before writing ANY code, ask: "Should this be a shared module?"

**Decision Tree:**
```
Writing new functionality?
    │
    ├─ Will this EVER be used in >1 project?
    │   ├─ YES → Create shared library ✅
    │   └─ MAYBE → Create shared library ✅
    │
    ├─ Is this business logic or utility?
    │   ├─ Business logic → Create shared library ✅
    │   └─ Utility → Create shared library ✅
    │
    └─ Is this UI component?
        ├─ Reusable UI → Create shared library ✅
        └─ App-specific UI → Inline in app (exception)
```

**Rule:** If unsure, CREATE A LIBRARY. Easier to inline later than extract later.

### Principle 2: Shared Library Repository

**MANDATORY:** All shared code MUST reside in master repository for cross-project access.

**Structure:**
```
/Volumes/M-Drive/Coding/
├── ideacode/                    # Master framework
│   └── libraries/               # ⭐ Shared libraries root
│       ├── core/                # Core utilities
│       ├── ui/                  # Reusable UI components
│       ├── data/                # Data layer modules
│       ├── network/             # Network/API modules
│       ├── security/            # Security/auth modules
│       ├── voice/               # Voice-specific modules
│       └── platform/            # Platform abstractions
│
├── ava/                         # Project: AVA
│   └── app/libs/                # Symlinks to ideacode/libraries/*
│
├── avaconnect/                  # Project: AVAConnect
│   └── app/libs/                # Symlinks to ideacode/libraries/*
│
├── Avanues/                     # Project: Avanues
│   └── app/libs/                # Symlinks to ideacode/libraries/*
│
└── ... (all projects)
```

### Principle 3: Kotlin Multiplatform (KMP) by Default

**MANDATORY:** All shared libraries MUST be Kotlin Multiplatform unless platform-specific.

**Why KMP:**
- ✅ Single codebase for Android, iOS, Web, Desktop
- ✅ Compile-time safety across platforms
- ✅ Native performance (no runtime overhead)
- ✅ Share business logic, keep UI platform-specific

**Structure:**
```kotlin
// libraries/core/auth/
src/
  commonMain/          # Shared code (100% of business logic)
    kotlin/
      com.ideacode.auth/
        AuthManager.kt
        TokenStore.kt

  androidMain/         # Android-specific (if needed)
    kotlin/
      com.ideacode.auth/
        AndroidKeyStore.kt

  iosMain/             # iOS-specific (if needed)
    kotlin/
      com.ideacode.auth/
        IOSKeychain.kt
```

### Principle 4: API-First Design

**MANDATORY:** All modules MUST expose clean, documented APIs.

**Requirements:**
1. ✅ Public API in `src/commonMain/kotlin/{package}/api/`
2. ✅ Internal implementation in `src/commonMain/kotlin/{package}/internal/`
3. ✅ KDoc documentation for all public APIs
4. ✅ Semantic versioning (MAJOR.MINOR.PATCH)

**Example:**
```kotlin
// Public API (src/commonMain/kotlin/com/ideacode/auth/api/AuthManager.kt)
package com.ideacode.auth.api

/**
 * Manages user authentication across all Avanues apps.
 *
 * @see [Authentication Guide](docs/authentication.md)
 */
interface AuthManager {
    /**
     * Authenticates user with OAuth2 PKCE flow.
     *
     * @param provider OAuth provider (Google, GitHub, etc.)
     * @return [AuthResult] with access token or error
     * @throws AuthException if authentication fails
     */
    suspend fun authenticate(provider: OAuthProvider): AuthResult
}

// Internal implementation (src/commonMain/kotlin/com/ideacode/auth/internal/AuthManagerImpl.kt)
package com.ideacode.auth.internal

internal class AuthManagerImpl : AuthManager {
    // Implementation details hidden from consumers
}
```

---

## Module Categories

### Category 1: Core Utilities

**Purpose:** Foundation libraries used by all projects

**Examples:**
- `libraries/core/logging` - Structured logging with analytics
- `libraries/core/storage` - Key-value storage abstraction
- `libraries/core/networking` - HTTP client with retry/caching
- `libraries/core/serialization` - JSON/protobuf serialization
- `libraries/core/crypto` - Encryption/hashing utilities
- `libraries/core/datetime` - Date/time utilities
- `libraries/core/validation` - Input validation

**Characteristics:**
- ✅ Zero dependencies on other modules (foundational)
- ✅ 100% test coverage (critical infrastructure)
- ✅ Semantic versioning (breaking changes = MAJOR bump)

### Category 2: UI Components

**Purpose:** Reusable UI components for Android apps

**Examples:**
- `libraries/ui/design-system` - Material Design 3 components
- `libraries/ui/theme-utils` - **Optional** Theme utilities (color manipulation, contrast checking, Avanues detection)
- `libraries/ui/charts` - Data visualization
- `libraries/ui/voice-input` - Voice recognition UI
- `libraries/ui/animations` - Shared animations
- `libraries/ui/accessibility` - A11y helpers

**Characteristics:**
- ✅ Jetpack Compose for Android
- ✅ Preview functions for all components
- ✅ Theme-aware (supports dark mode)
- ✅ Accessibility tested

**🚨 CRITICAL: Per-Project UI Theming (v8.4)**

**IMPORTANT:** Theming is **PER-PROJECT**, NOT a shared library. Each app has its own theme module with unique branding.

ALL applications MUST include their own theming module inside the app code that:
1. **Abstraction Layer:** Interface-based theme definitions (AppColors, AppTypography, AppComponents)
2. **Multiple Implementations:**
   - AVAMagic theme (Avanues ecosystem branding - **PROJECT-SPECIFIC COLORS**)
   - Material Design 3 theme (standalone/Play Store distribution - **PROJECT-SPECIFIC**)
3. **Auto-Detection:** Runtime detection of Avanues ecosystem presence
4. **User Override:** Settings to override auto-detected theme
5. **Build Variants:** Product flavors for different distributions (ideamagic, material, standalone)

**Per-Project Module Structure:**
```
{project}/app/src/main/kotlin/com/{company}/{app}/ui/theme/
├── AppTheme.kt                  # Main theme provider (PROJECT-SPECIFIC)
├── ThemeConfig.kt               # Theme selection (PROJECT-SPECIFIC)
│
├── abstraction/                 # Theme abstraction layer
│   ├── AppColors.kt             # Color interface
│   ├── AppTypography.kt         # Typography interface
│   ├── AppShapes.kt             # Shapes interface
│   └── AppComponents.kt         # Component interface
│
├── ideamagic/                   # AVAMagic (PROJECT-SPECIFIC BRANDING)
│   ├── AVAMagicTheme.kt        # AVA blue / Avanues purple / etc.
│   ├── AVAMagicColors.kt       # Custom accent colors per app
│   ├── AVAMagicTypography.kt
│   └── AVAMagicComponents.kt
│
└── material/                    # Material Design (PROJECT-SPECIFIC)
    ├── MaterialTheme.kt
    ├── MaterialColors.kt        # Dynamic colors per app
    ├── MaterialTypography.kt
    └── MaterialComponents.kt
```

**Example: Different Projects, Different Branding:**
```kotlin
// AVA: Blue branding
class AVAMagicColors : AppColors {
    override val primary = Color(0xFF2E5BDA)  // AVA Blue
}

// Avanues: Purple branding
class AVAMagicColors : AppColors {
    override val primary = Color(0xFF5A52E0)  // Avanues Purple
}

// AVAConnect: Teal branding
class AVAMagicColors : AppColors {
    override val primary = Color(0xFF00ACC1)  // AVAConnect Teal
}
```

**What CAN Be Shared (Optional):**
Only theme **utilities** can be shared in `libraries/ui/theme-utils/`:
- Color manipulation functions (lighten, darken, alpha)
- Contrast checking (accessibility)
- Avanues ecosystem detection logic
- Theme preference storage utilities

**Why Per-Project:**
- ✅ Each app has unique branding (AVA blue vs Avanues purple)
- ✅ App-specific customization needs
- ✅ Different Material You dynamic color seeds
- ✅ Independent version control
- ✅ No coupling between projects

**Read full protocol:** `protocols/Protocol-UI-Theming-Architecture-v1.0.md`

### Category 3: Data Layer

**Purpose:** Data access, caching, sync

**Examples:**
- `libraries/data/repository` - Repository pattern implementation
- `libraries/data/cache` - Multi-level caching (memory/disk/cloud)
- `libraries/data/sync` - Offline-first sync engine
- `libraries/data/migration` - Database migration utilities

**Characteristics:**
- ✅ Reactive (Flow-based)
- ✅ Offline-first
- ✅ Conflict resolution for sync

### Category 4: Network/API

**Purpose:** Backend communication

**Examples:**
- `libraries/network/api-client` - RESTful API client
- `libraries/network/graphql` - GraphQL client
- `libraries/network/websocket` - Real-time WebSocket
- `libraries/network/grpc` - gRPC client (if needed)

**Characteristics:**
- ✅ Auto-retry with exponential backoff
- ✅ Request/response interceptors
- ✅ Mock support for testing

### Category 5: Security/Auth

**Purpose:** Authentication, authorization, encryption

**Examples:**
- `libraries/security/auth` - OAuth2/JWT authentication
- `libraries/security/biometric` - Fingerprint/Face ID
- `libraries/security/keystore` - Secure key storage
- `libraries/security/encryption` - AES-256 encryption

**Characteristics:**
- ✅ OWASP Top 10 compliant
- ✅ Security audit required before v1.0
- ✅ Penetration tested

### Category 6: Voice

**Purpose:** Voice-specific functionality (Avanues core)

**Examples:**
- `libraries/voice/recognition` - Speech-to-text
- `libraries/voice/synthesis` - Text-to-speech
- `libraries/voice/dsl-parser` - VoiceOS DSL (.vos) parser
- `libraries/voice/commands` - Voice command registry
- `libraries/voice/context` - Voice context management

**Characteristics:**
- ✅ Low latency (<100ms response)
- ✅ Offline mode support
- ✅ Multi-language support

### Category 7: Platform Abstractions

**Purpose:** Hide platform differences

**Examples:**
- `libraries/platform/filesystem` - File I/O abstraction
- `libraries/platform/permissions` - Permission handling
- `libraries/platform/sensors` - Sensor access (GPS, accelerometer)
- `libraries/platform/notifications` - Push notifications

**Characteristics:**
- ✅ KMP with expect/actual declarations
- ✅ Graceful degradation if platform lacks feature

---

## Module Structure (Template)

### Standard Library Structure

```
libraries/{category}/{module-name}/
├── README.md                    # Module documentation
├── build.gradle.kts             # Build configuration
├── gradle.properties            # Version and metadata
│
├── src/
│   ├── commonMain/
│   │   ├── kotlin/
│   │   │   └── com/ideacode/{module}/
│   │   │       ├── api/         # Public API
│   │   │       │   └── {Module}Manager.kt
│   │   │       ├── internal/    # Internal implementation
│   │   │       │   └── {Module}ManagerImpl.kt
│   │   │       └── models/      # Data models
│   │   │           └── {Model}.kt
│   │   └── resources/           # Shared resources
│   │
│   ├── androidMain/             # Android-specific
│   │   ├── kotlin/
│   │   └── AndroidManifest.xml
│   │
│   ├── iosMain/                 # iOS-specific
│   │   └── kotlin/
│   │
│   ├── commonTest/              # Shared tests
│   │   └── kotlin/
│   │       └── {Module}Test.kt
│   │
│   ├── androidTest/             # Android tests
│   │   └── kotlin/
│   │
│   └── iosTest/                 # iOS tests
│       └── kotlin/
│
├── docs/
│   ├── architecture.md          # Architecture overview
│   ├── api-reference.md         # API documentation
│   ├── migration-guide.md       # Version migration
│   └── examples.md              # Usage examples
│
└── CHANGELOG.md                 # Version history
```

### Build Configuration Template

**`build.gradle.kts`:**
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}

group = "com.ideacode.{category}"
version = "1.0.0"

kotlin {
    android()
    ios()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Minimal dependencies
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
            }
        }
    }
}

android {
    namespace = "com.ideacode.{category}.{module}"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
}
```

---

## Integration Strategy

### Step 1: Create Shared Library

```bash
# Create library structure
cd /Volumes/M-Drive/Coding/ideacode
mkdir -p libraries/{category}/{module-name}/src/commonMain/kotlin/com/ideacode/{module}/api

# Use MCP tool
ideacode_execute_code({
  code: `
    import { createLibrary } from '/wrappers/project/create-library.js'

    await createLibrary({
      category: 'core',
      name: 'auth',
      description: 'OAuth2 authentication with PKCE flow',
      kmp: true,
      platforms: ['android', 'ios']
    })
  `
})
```

### Step 2: Publish to Master Repository

```bash
# Build library
cd libraries/core/auth
./gradlew build

# Publish to local Maven
./gradlew publishToMavenLocal

# Or publish to private Maven repository
./gradlew publish
```

### Step 3: Consume in Projects

**`settings.gradle.kts` (Project-level):**
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal() // For local development
        maven {
            url = uri("/Volumes/M-Drive/Coding/ideacode/libraries/maven")
        }
    }
}
```

**`build.gradle.kts` (App-level):**
```kotlin
dependencies {
    implementation("com.ideacode.core:auth:1.0.0")
    implementation("com.ideacode.ui:design-system:2.1.0")
    implementation("com.ideacode.voice:recognition:1.5.0")
}
```

### Step 4: Symlink for Development

**For active development (avoid republishing):**
```bash
# In AVA project
cd /Volumes/M-Drive/Coding/ava
mkdir -p app/libs
ln -s /Volumes/M-Drive/Coding/ideacode/libraries app/libs/ideacode

# Use composite build in settings.gradle.kts
includeBuild("../ideacode/libraries/core/auth")
```

---

## Anti-Patterns (PROHIBITED)

### ❌ Anti-Pattern 1: Copy-Paste Code

**WRONG:**
```kotlin
// In AVA project
class AuthManager {
    fun authenticate() { /* implementation */ }
}

// In AVAConnect project (DUPLICATE!)
class AuthManager {
    fun authenticate() { /* implementation */ }
}
```

**CORRECT:**
```kotlin
// In ideacode/libraries/core/auth
class AuthManager {
    fun authenticate() { /* implementation */ }
}

// In AVA and AVAConnect
dependencies {
    implementation("com.ideacode.core:auth:1.0.0")
}
```

### ❌ Anti-Pattern 2: God Module

**WRONG:**
```kotlin
// Single module doing everything
library/core/utils/
  - AuthManager.kt (auth)
  - HttpClient.kt (network)
  - JsonParser.kt (serialization)
  - Logger.kt (logging)
  - CacheManager.kt (caching)
  - ... (100+ classes)
```

**CORRECT:**
```kotlin
// Separate focused modules
libraries/core/auth/       # Only authentication
libraries/core/network/    # Only networking
libraries/core/logging/    # Only logging
libraries/core/cache/      # Only caching
```

### ❌ Anti-Pattern 3: Tight Coupling

**WRONG:**
```kotlin
class AuthManager(
    private val avaDatabase: AvaDatabase  // Coupled to AVA!
)
```

**CORRECT:**
```kotlin
interface AuthStorage {
    suspend fun saveToken(token: String)
}

class AuthManager(
    private val storage: AuthStorage  // Abstraction
)

// AVA provides implementation
class AvaAuthStorage : AuthStorage {
    override suspend fun saveToken(token: String) {
        avaDatabase.insert(token)
    }
}
```

### ❌ Anti-Pattern 4: Incomplete Abstractions

**WRONG:**
```kotlin
// Android-only (not KMP)
class AuthManager(
    private val context: Context  // Android-specific!
)
```

**CORRECT:**
```kotlin
// KMP with expect/actual
expect class PlatformAuthManager

class AuthManager(
    private val platform: PlatformAuthManager  // Platform-agnostic
)

// Android implementation
actual class PlatformAuthManager(
    private val context: Context
)

// iOS implementation
actual class PlatformAuthManager(
    private val viewController: UIViewController
)
```

---

## Quality Gates

**MANDATORY checks before publishing library:**

### Gate 1: API Review
- [ ] Public API is minimal and cohesive
- [ ] No Android/iOS-specific types in common API
- [ ] All public functions have KDoc
- [ ] Breaking changes justified and documented

### Gate 2: Testing
- [ ] 90%+ test coverage for public API
- [ ] Unit tests for all platforms (commonTest, androidTest, iosTest)
- [ ] Integration tests with real dependencies
- [ ] Performance tests (if applicable)

### Gate 3: Documentation
- [ ] README.md with quick start
- [ ] API reference (generated or manual)
- [ ] Migration guide (for breaking changes)
- [ ] Usage examples

### Gate 4: Dependencies
- [ ] Minimal dependencies (only what's essential)
- [ ] No transitive dependency conflicts
- [ ] All dependencies use stable versions
- [ ] License compatibility checked

### Gate 5: Versioning
- [ ] Semantic versioning enforced
- [ ] CHANGELOG.md updated
- [ ] Git tag created (v1.0.0)
- [ ] Release notes written

---

## Governance

### Library Ownership

**Each library MUST have:**
1. **Owner** - Primary maintainer (1 person)
2. **Approvers** - Can approve PRs (2-3 people)
3. **CODEOWNERS** - Enforced via GitHub

**Example `.github/CODEOWNERS`:**
```
/libraries/core/auth/         @manoj
/libraries/ui/design-system/  @manoj @designer
/libraries/voice/recognition/ @manoj @voice-team
```

### Versioning Policy

**Semantic Versioning (MAJOR.MINOR.PATCH):**
- **MAJOR:** Breaking API changes (requires migration guide)
- **MINOR:** New features (backward compatible)
- **PATCH:** Bug fixes (backward compatible)

**Examples:**
- `1.0.0 → 1.0.1` - Fixed token refresh bug ✅
- `1.0.1 → 1.1.0` - Added biometric auth ✅
- `1.1.0 → 2.0.0` - Changed `authenticate()` signature ⚠️ BREAKING

### Deprecation Policy

**Before removing public API:**
1. Mark as `@Deprecated` with migration path
2. Keep for at least 2 MINOR versions
3. Provide automated migration tool (if possible)
4. Announce in CHANGELOG and release notes

**Example:**
```kotlin
@Deprecated(
    message = "Use authenticate(provider) instead",
    replaceWith = ReplaceWith("authenticate(OAuthProvider.GOOGLE)"),
    level = DeprecationLevel.WARNING  // v1.5.0
)
fun authenticateWithGoogle() {
    authenticate(OAuthProvider.GOOGLE)
}

// Remove in v2.0.0 (after v1.5.0 → v1.6.0 → v1.7.0)
```

---

## MCP Integration

### Tool: Create Modular Library

**Signature:**
```typescript
export async function createLibrary(params: {
  category: string;           // core, ui, data, network, security, voice, platform
  name: string;               // Module name (e.g., "auth")
  description: string;        // Short description
  kmp: boolean;               // Kotlin Multiplatform?
  platforms: string[];        // ["android", "ios", "web", "desktop"]
  dependencies?: string[];    // External dependencies
}): Promise<LibraryResult>
```

**Usage:**
```typescript
await ideacode_execute_code({
  code: `
    import { createLibrary } from '/wrappers/project/create-library.js'

    const result = await createLibrary({
      category: 'core',
      name: 'auth',
      description: 'OAuth2 authentication with PKCE flow',
      kmp: true,
      platforms: ['android', 'ios'],
      dependencies: ['kotlinx-coroutines-core', 'ktor-client']
    })

    return result
  `
})
```

**Output:**
```json
{
  "success": true,
  "library_path": "/Volumes/M-Drive/Coding/ideacode/libraries/core/auth",
  "files_created": [
    "build.gradle.kts",
    "README.md",
    "src/commonMain/kotlin/com/ideacode/auth/api/AuthManager.kt",
    "src/commonTest/kotlin/com/ideacode/auth/AuthManagerTest.kt"
  ],
  "next_steps": [
    "Implement AuthManager interface",
    "Add platform-specific implementations",
    "Write tests",
    "Publish to Maven"
  ]
}
```

### Tool: Detect Code Duplication

**Signature:**
```typescript
export async function detectDuplication(params: {
  projects: string[];         // Paths to projects to scan
  threshold: number;          // Similarity threshold (0.0-1.0)
  minLines: number;           // Minimum lines to consider
}): Promise<DuplicationResult>
```

**Usage:**
```typescript
await ideacode_execute_code({
  code: `
    import { detectDuplication } from '/wrappers/quality/detect-duplication.js'

    const result = await detectDuplication({
      projects: [
        '/Volumes/M-Drive/Coding/ava',
        '/Volumes/M-Drive/Coding/avaconnect',
        '/Volumes/M-Drive/Coding/voiceavanue'
      ],
      threshold: 0.85,  // 85% similar
      minLines: 10
    })

    return result.candidates.slice(0, 10)  // Top 10 candidates
  `
})
```

**Output:**
```json
{
  "success": true,
  "duplications_found": 47,
  "candidates": [
    {
      "similarity": 0.95,
      "files": [
        "/Volumes/M-Drive/Coding/ava/app/src/main/kotlin/AuthManager.kt",
        "/Volumes/M-Drive/Coding/avaconnect/app/src/main/kotlin/AuthManager.kt"
      ],
      "lines": 120,
      "recommendation": "Extract to libraries/core/auth"
    }
  ]
}
```

---

## Migration Checklist

**For existing projects (AVA, AVAConnect, etc.):**

- [ ] **Step 1:** Detect code duplication across all 6 projects
- [ ] **Step 2:** Identify top 10 candidates for extraction
- [ ] **Step 3:** Create shared libraries in `ideacode/libraries/`
- [ ] **Step 4:** Extract duplicated code to libraries
- [ ] **Step 5:** Publish libraries to Maven (local or remote)
- [ ] **Step 6:** Update projects to use libraries
- [ ] **Step 7:** Remove duplicated code from projects
- [ ] **Step 8:** Run all tests (ensure nothing broken)
- [ ] **Step 9:** Update documentation
- [ ] **Step 10:** Commit changes with migration notes

---

## Examples

### Example 1: Authentication Library

**Before (Monolithic):**
```
AVA/app/src/main/kotlin/AuthManager.kt (500 lines)
AVAConnect/app/src/main/kotlin/AuthManager.kt (500 lines, copy-paste)
Avanues/app/src/main/kotlin/AuthManager.kt (500 lines, copy-paste)
```

**After (Modular):**
```
ideacode/libraries/core/auth/
  src/commonMain/kotlin/com/ideacode/auth/
    api/AuthManager.kt (100 lines, interface)
    internal/AuthManagerImpl.kt (400 lines, implementation)

AVA/app/build.gradle.kts:
  implementation("com.ideacode.core:auth:1.0.0")

AVAConnect/app/build.gradle.kts:
  implementation("com.ideacode.core:auth:1.0.0")
```

**Benefits:**
- **85% code reduction** (500×3 = 1500 lines → 500 lines)
- **Single source of truth** (bug fixes apply to all)
- **Easier testing** (test once, use everywhere)

### Example 2: Voice DSL Parser

**Before (Monolithic):**
```
Avanues/app/src/main/kotlin/dsl/Parser.kt (800 lines)
VoiceOS/app/src/main/kotlin/dsl/Parser.kt (800 lines, slight differences)
```

**After (Modular):**
```
ideacode/libraries/voice/dsl-parser/
  src/commonMain/kotlin/com/ideacode/voice/dsl/
    api/DslParser.kt
    internal/Lexer.kt
    internal/Parser.kt
    internal/Validator.kt

Avanues/app/build.gradle.kts:
  implementation("com.ideacode.voice:dsl-parser:2.0.0")
```

**Benefits:**
- **Consistency** (both apps use same DSL syntax)
- **Testability** (comprehensive test suite in library)
- **Extensibility** (new DSL features available to both)

---

## References

- **Protocol-Git-Branch-Hierarchy-v1.0.md** - Branch organization
- **Protocol-Zero-Tolerance-Pre-Code.md** - Quality gates
- **Protocol-File-Organization-v2.0.md** - File structure
- **Kotlin Multiplatform Docs:** https://kotlinlang.org/docs/multiplatform.html

---

## Changelog

### v1.0 (2025-11-15)
- Initial protocol creation
- Defined 7 module categories
- Added library structure template
- Created quality gates and governance
- Added MCP integration (createLibrary, detectDuplication)
- Added migration checklist

---

**Author:** Manoj Jhawar
**Email:** manoj@ideahq.net
**License:** Proprietary

---

**IDEACODE v8.4** - Modular, library-driven architecture for scalable development
