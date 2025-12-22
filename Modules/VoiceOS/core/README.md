# VoiceOS Core Libraries (KMP)

**Kotlin Multiplatform libraries extracted from VoiceOSCore for maximum reusability**

## Overview

These libraries provide pure Kotlin utilities that work across Android, iOS, JVM, and JavaScript platforms. All libraries are published to Maven Local for easy integration across VoiceOS projects.

## Available Libraries (5)

### 1. voiceos-result (Type-Safe Error Handling)
**Package:** `com.augmentalis.voiceos.result`
**Version:** 1.0.0
**Size:** ~150 LOC

**Purpose:** Result monad pattern for type-safe error handling without exceptions

**Features:**
- `VoiceOSResult<T, E>` sealed class (Success, Failure)
- Functional transformations: `map()`, `flatMap()`, `mapError()`
- Railway-oriented programming pattern
- Zero runtime overhead

**Usage:**
```kotlin
dependencies {
    implementation("com.augmentalis.voiceos:result:1.0.0")
}

// Example
fun fetchUser(id: String): VoiceOSResult<User, DatabaseError> {
    return database.query(id)
        .map { row -> User(row.name, row.email) }
        .mapError { e -> DatabaseError.QueryFailed(e) }
}
```

**Test Coverage:** 25+ test cases
**Platforms:** Android, iOS, JVM, JS

---

### 2. voiceos-hash (SHA-256 Hashing)
**Package:** `com.augmentalis.voiceos.hash`
**Version:** 1.0.0
**Size:** ~250 LOC

**Purpose:** Pure Kotlin SHA-256 hashing for content deduplication and integrity

**Features:**
- Platform-agnostic SHA-256 implementation
- Android: Uses MessageDigest (optimized)
- iOS: Pure Kotlin implementation (180 LOC)
- JVM: Uses MessageDigest
- Hex string conversion utilities

**Usage:**
```kotlin
dependencies {
    implementation("com.augmentalis.voiceos:hash:1.0.0")
}

// Example
val content = "Hello, World!"
val hash = HashUtils.sha256(content)
// Returns: "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
```

**Test Coverage:** 18+ test cases (cross-platform validation)
**Platforms:** Android, iOS, JVM, JS

---

### 3. voiceos-constants (Centralized Configuration)
**Package:** `com.augmentalis.voiceos.constants`
**Version:** 1.0.0
**Size:** ~370 LOC

**Purpose:** Centralized configuration constants to eliminate magic numbers

**Features:**
- 18 configuration categories:
  - TreeTraversal, Timing, Cache, Database, Performance
  - RateLimit, CircuitBreaker, Logging, UI, Security
  - Network, VoiceRecognition, Validation, Storage
  - Testing, Accessibility, Metrics, Overlays, Animation, Battery
- Compile-time constants (zero runtime cost)
- Well-documented with rationale

**Usage:**
```kotlin
dependencies {
    implementation("com.augmentalis.voiceos:constants:1.0.0")
}

// Example
import com.augmentalis.voiceos.constants.VoiceOSConstants

val maxDepth = VoiceOSConstants.TreeTraversal.MAX_DEPTH // 50
val cacheSize = VoiceOSConstants.Cache.DEFAULT_CACHE_SIZE // 100
val timeout = VoiceOSConstants.Network.HTTP_TIMEOUT_MS // 30000L
```

**Test Coverage:** 30+ test cases (value validation, consistency checks)
**Platforms:** Android, iOS, JVM, JS

---

### 4. voiceos-validation (Input Sanitization)
**Package:** `com.augmentalis.voiceos.validation`
**Version:** 1.0.0
**Size:** ~130 LOC

**Purpose:** SQL wildcard escaping for secure database queries

**Features:**
- LIKE pattern escaping (prevents SQL injection via wildcards)
- Escape dangerous characters: `%`, `_`, `\`
- Utility methods:
  - `escapeLikePattern()` - Core escaping
  - `wrapWithWildcards()` - Partial matching: `%text%`
  - `prefixWithWildcard()` - Suffix matching: `%text`
  - `suffixWithWildcard()` - Prefix matching: `text%`
  - `containsWildcards()` - Detection utility

**Usage:**
```kotlin
dependencies {
    implementation("com.augmentalis.voiceos:validation:1.0.0")
}

// Example
import com.augmentalis.voiceos.validation.SqlEscapeUtils

val userInput = "50% off"
val safePattern = SqlEscapeUtils.wrapWithWildcards(userInput)
// Returns: "%50\\% off%"

// In DAO:
@Query("SELECT * FROM products WHERE name LIKE :pattern ESCAPE '\\'")
fun searchProducts(pattern: String): List<Product>

// Usage:
dao.searchProducts(safePattern) // Safe from SQL injection
```

**Test Coverage:** 42+ test cases (edge cases, security scenarios)
**Platforms:** Android, iOS, JVM, JS

---

### 5. voiceos-exceptions (Exception Hierarchy)
**Package:** `com.augmentalis.voiceos.exceptions`
**Version:** 1.0.0
**Size:** ~366 LOC

**Purpose:** Structured exception hierarchy for consistent error handling

**Features:**
- Base `VoiceOSException` with:
  - `getFullMessage()` - Formatted with error codes
  - `isCausedBy<T>()` - Deep cause chain inspection
- 6 sealed exception hierarchies:
  1. **DatabaseException** (5 types): Backup, Restore, Integrity, Migration, Transaction
  2. **SecurityException** (5 types): Encryption, Decryption, Signature, Unauthorized, Keystore
  3. **CommandException** (4 types): Execution, Parsing, RateLimit, CircuitBreaker
  4. **ScrapingException** (3 types): Element, Hierarchy, Cache
  5. **PrivacyException** (2 types): Consent, Retention
  6. **AccessibilityException** (3 types): Service, Node, Action
- Structured error codes (e.g., "DB_BACKUP_FAILED", "SECURITY_UNAUTHORIZED")
- Context-aware toString() for debugging

**Usage:**
```kotlin
dependencies {
    implementation("com.augmentalis.voiceos:exceptions:1.0.0")
}

// Example
import com.augmentalis.voiceos.exceptions.*

fun performDatabaseBackup() {
    try {
        // Backup logic
    } catch (e: Exception) {
        throw DatabaseException.BackupException(
            "Backup failed for database v5",
            cause = e
        )
    }
}

// Usage:
try {
    performDatabaseBackup()
} catch (e: DatabaseException.BackupException) {
    logger.error(e.getFullMessage()) // "[DB_BACKUP_FAILED] Backup failed..."
}
```

**Test Coverage:** 60+ test cases (all exception types, edge cases)
**Platforms:** Android, iOS, JVM, JS

---

## Integration Guide

### Step 1: Add Maven Local Repository
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()  // Required for locally published KMP libraries
        google()
        mavenCentral()
    }
}
```

### Step 2: Add Dependencies
```kotlin
// build.gradle.kts
dependencies {
    // Add only the libraries you need
    implementation("com.augmentalis.voiceos:result:1.0.0")
    implementation("com.augmentalis.voiceos:hash:1.0.0")
    implementation("com.augmentalis.voiceos:constants:1.0.0")
    implementation("com.augmentalis.voiceos:validation:1.0.0")
    implementation("com.augmentalis.voiceos:exceptions:1.0.0")
}
```

### Step 3: Import and Use
```kotlin
// In your Kotlin code
import com.augmentalis.voiceos.result.VoiceOSResult
import com.augmentalis.voiceos.hash.HashUtils
import com.augmentalis.voiceos.constants.VoiceOSConstants
import com.augmentalis.voiceos.validation.SqlEscapeUtils
import com.augmentalis.voiceos.exceptions.*

// Use as needed...
```

---

## Architecture

### Directory Structure
```
libraries/core/
├── result/          # Type-safe error handling
│   ├── src/
│   │   ├── commonMain/kotlin/
│   │   ├── commonTest/kotlin/
│   │   ├── androidMain/kotlin/
│   │   ├── iosMain/kotlin/
│   │   └── jvmMain/kotlin/
│   └── build.gradle.kts
├── hash/            # SHA-256 hashing
├── constants/       # Configuration constants
├── validation/      # Input sanitization
├── exceptions/      # Exception hierarchy
└── README.md        # This file
```

### Build Configuration Pattern
All libraries follow the same KMP build pattern:

```kotlin
// build.gradle.kts (standard pattern)
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "library-name" }
    }

    jvm {
        compilations.all { kotlinOptions.jvmTarget = "17" }
    }
}
```

---

## Publishing

### Publishing to Maven Local (Development)
```bash
# Publish all libraries
./gradlew publishToMavenLocal

# Publish specific library
./gradlew :libraries:core:result:publishToMavenLocal
```

### Publishing to Maven Central (Production)
```bash
# Configure in ~/.gradle/gradle.properties:
# signing.keyId=YOUR_KEY_ID
# signing.password=YOUR_PASSWORD
# signing.secretKeyRingFile=/path/to/secring.gpg
# ossrhUsername=YOUR_USERNAME
# ossrhPassword=YOUR_PASSWORD

# Publish
./gradlew publish
```

---

## Migration from VoiceOSCore

If you're migrating code from VoiceOSCore to use these libraries:

### Before (VoiceOSCore internal)
```kotlin
// Old imports
import com.augmentalis.voiceoscore.utils.HashUtils
import com.augmentalis.voiceoscore.utils.VoiceOSConstants
import com.augmentalis.voiceoscore.utils.SqlEscapeUtils
import com.augmentalis.voiceoscore.exceptions.*

// Usage (same)
val hash = HashUtils.sha256("content")
```

### After (KMP library)
```kotlin
// New imports (cleaner namespace)
import com.augmentalis.voiceos.hash.HashUtils
import com.augmentalis.voiceos.constants.VoiceOSConstants
import com.augmentalis.voiceos.validation.SqlEscapeUtils
import com.augmentalis.voiceos.exceptions.*

// Usage (unchanged)
val hash = HashUtils.sha256("content")
```

**Benefits:**
- ✅ Reusable across all VoiceOS projects (AVA, AVAConnect, Avanues)
- ✅ Works on iOS (not just Android)
- ✅ Cleaner namespace separation
- ✅ Independent versioning
- ✅ Smaller app size (only include what you need)

---

## Testing

Each library includes comprehensive test coverage:

```bash
# Run all tests
./gradlew test

# Run specific library tests
./gradlew :libraries:core:result:test
./gradlew :libraries:core:hash:test
./gradlew :libraries:core:constants:test
./gradlew :libraries:core:validation:test
./gradlew :libraries:core:exceptions:test
```

**Test Results:**
- result: 25+ test cases ✅
- hash: 18+ test cases ✅
- constants: 30+ test cases ✅
- validation: 42+ test cases ✅
- exceptions: 60+ test cases ✅

**Total: 175+ test cases across all libraries**

---

## Roadmap

### Completed (Phase 1-5)
- ✅ Phase 1: voiceos-result (Type-safe error handling)
- ✅ Phase 2: voiceos-hash (SHA-256 hashing)
- ✅ Phase 3: voiceos-constants (Configuration values)
- ✅ Phase 4: voiceos-validation (SQL escaping)
- ✅ Phase 5: voiceos-exceptions (Exception hierarchy)

### Planned (Future)
- 🔄 Phase 6: voiceos-command-models (Command data structures)
- 🔄 Phase 7: voiceos-accessibility-types (Accessibility types)
- 🔄 Phase 8: voiceos-text-utils (String utilities)
- 🔄 Phase 9: voiceos-logging (Structured logging)
- 🔄 Phase 10: voiceos-security (Encryption/signing)

---

## Contributing

### Adding a New Library

1. **Create structure:**
   ```bash
   mkdir -p libraries/core/{name}/src/{commonMain,commonTest,androidMain,iosMain,jvmMain}/kotlin/com/augmentalis/voiceos/{name}
   ```

2. **Copy build.gradle.kts** from existing library and update:
   - `baseName` in iOS framework
   - `namespace` in android block
   - `name.set()` in publishing block

3. **Add to settings.gradle.kts:**
   ```kotlin
   include(":libraries:core:{name}")  // {Description}
   ```

4. **Extract code** from VoiceOSCore:
   - Move to `commonMain/kotlin/`
   - Update package to `com.augmentalis.voiceos.{name}`
   - Remove Android-specific dependencies

5. **Write tests** in `commonTest/kotlin/`

6. **Publish:**
   ```bash
   ./gradlew :libraries:core:{name}:publishToMavenLocal
   ```

7. **Update VoiceOSCore:**
   - Add dependency in `build.gradle.kts`
   - Update imports
   - Remove old file
   - Verify build

### Quality Standards

- ✅ 90%+ test coverage
- ✅ Zero Android dependencies in commonMain
- ✅ Well-documented public APIs
- ✅ Comprehensive KDoc comments
- ✅ Cross-platform validation tests

---

## License

**Copyright (C)** Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
**License:** Proprietary
**Author:** Manoj Jhawar <manoj@ideahq.net>

---

## Support

**Issues:** Report at VoiceOS project repository
**Documentation:** See individual library source files for detailed API docs
**Migration Help:** Contact development team

---

**Last Updated:** 2025-11-17
**Total Libraries:** 5
**Total LOC Extracted:** ~1,400
**Test Coverage:** 175+ test cases
