# NewAvanues - Monorepo KMP Structure Guide

**Repository:** NewAvanues
**Date:** 2025-12-23
**Version:** 1.1
**Status:** Active Standard
**Scope:** All modules and platform apps

---

## Overview

NewAvanues is a Kotlin Multiplatform (KMP) monorepo containing:
- **Shared KMP modules** in `Modules/` directory
- **Platform-specific applications** at root level
- **Common libraries** for cross-platform code sharing
- **Unified documentation** in `Docs/` directory
- **Unified build system** using Gradle composite builds

---

## Root Directory Structure

```
NewAvanues/
├── android/                    # Android platform apps
│   ├── AVA/
│   ├── VoiceOS/
│   └── Cockpit/
│
├── ios/                        # iOS platform apps (Swift + SwiftUI)
│   ├── AVA/
│   ├── VoiceOS/
│   └── Cockpit/
│
├── web/                        # Web platform apps (Tauri + React)
│   ├── WebAvanue/
│   └── Cockpit/
│
├── desktop/                    # Desktop platform apps (Tauri)
│   └── Cockpit/
│
├── Modules/                    # Shared KMP modules
│   ├── AVA/
│   ├── AVAMagic/
│   ├── VoiceOS/
│   ├── NLU/
│   └── Cockpit/
│
├── Common/                     # Shared KMP libraries
│   ├── Core/
│   ├── Data/
│   └── Utils/
│
├── Docs/                       # UNIVERSAL documentation (all modules)
│   ├── AVAMagic/
│   ├── VoiceOS/
│   ├── AVA/
│   ├── NLU/
│   ├── Architecture/
│   ├── Guides/
│   └── Standards/
│
├── .ideacode/                  # IDEACODE configuration
│   └── registries/
│
├── .claude/                    # Claude Code configuration
│   └── CLAUDE.md
│
├── settings.gradle.kts         # Gradle composite build settings
├── gradle.properties
└── version-catalogs/
```

---

## Platform Apps vs KMP Modules

### Platform Apps (android/, ios/, web/, desktop/)

**Purpose:** Platform-specific UI and runtime implementation

**Structure:**
```
android/
└── VoiceOS/
    ├── app/
    │   ├── src/
    │   │   └── main/
    │   │       ├── kotlin/com/augmentalis/voiceos/
    │   │       │   ├── MainActivity.kt
    │   │       │   ├── VoiceOSApp.kt
    │   │       │   └── ui/
    │   │       ├── AndroidManifest.xml
    │   │       └── res/
    │   └── build.gradle.kts
    └── settings.gradle.kts
```

**Package Naming:** `com.augmentalis.{app}`
- Example: `com.augmentalis.voiceos`, `com.augmentalis.ava`

**Dependencies:** Import from `Modules/` via Gradle
```kotlin
// android/VoiceOS/app/build.gradle.kts
dependencies {
    implementation(project(":Modules:VoiceOS:core"))
    implementation(project(":Modules:AVAMagic:MagicUI"))
}
```

### KMP Modules (Modules/)

**Purpose:** Cross-platform business logic and UI components

**Structure:**
```
Modules/AVAMagic/
├── .claude/
│   └── CLAUDE.md               # Module-specific instructions
│
├── Core/                       # Core utilities
│   ├── src/
│   │   ├── commonMain/kotlin/com/augmentalis/avamagic/core/
│   │   ├── androidMain/kotlin/com/augmentalis/avamagic/core/
│   │   ├── iosMain/kotlin/com/augmentalis/avamagic/core/
│   │   ├── jsMain/kotlin/com/augmentalis/avamagic/core/
│   │   └── jvmMain/kotlin/com/augmentalis/avamagic/core/
│   ├── build.gradle.kts
│   └── README.md
│
├── MagicUI/                    # Product: UI Framework
│   ├── Theme/
│   │   └── src/
│   │       ├── commonMain/kotlin/com/augmentalis/magicui/theme/
│   │       ├── androidMain/kotlin/com/augmentalis/magicui/theme/
│   │       └── iosMain/kotlin/com/augmentalis/magicui/theme/
│   │
│   ├── Components/
│   │   ├── Foundation/
│   │   │   └── src/
│   │   │       ├── commonMain/kotlin/com/augmentalis/magicui/components/foundation/
│   │   │       ├── androidMain/
│   │   │       └── iosMain/
│   │   │
│   │   └── Phase2/
│   │
│   └── Renderers/
│       ├── Android/
│       │   └── src/androidMain/kotlin/com/augmentalis/magicui/renderers/android/
│       ├── iOS/
│       │   └── src/iosMain/kotlin/com/augmentalis/magicui/renderers/ios/
│       └── Web/
│           └── src/jsMain/kotlin/com/augmentalis/magicui/renderers/web/
│
├── MagicCode/                  # Product: Code Generation
│   ├── Parser/
│   │   └── src/commonMain/kotlin/com/augmentalis/magiccode/parser/
│   ├── Generator/
│   │   └── src/commonMain/kotlin/com/augmentalis/magiccode/generator/
│   └── Templates/
│       └── src/commonMain/kotlin/com/augmentalis/magiccode/templates/
│
└── MagicTools/                 # Product: Development Tools
    └── ThemeCreator/
        ├── src/                # Tauri frontend
        └── src-tauri/          # Tauri backend
```

**Package Naming:** `com.augmentalis.{module}.{product}.{feature}`
- Module-level: `com.augmentalis.avamagic.*`
- Product-level: `com.augmentalis.magicui.*`, `com.augmentalis.magiccode.*`

**Documentation:** All module docs go to `/Docs/{Module}/`

---

## KMP Source Set Structure

### Source Set Types

| Source Set | Purpose | Example Path |
|------------|---------|--------------|
| **commonMain** | Shared cross-platform code | `src/commonMain/kotlin/` |
| **androidMain** | Android-specific implementation | `src/androidMain/kotlin/` |
| **iosMain** | iOS-specific implementation (Kotlin/Native) | `src/iosMain/kotlin/` |
| **jsMain** | JavaScript/Web implementation | `src/jsMain/kotlin/` |
| **jvmMain** | JVM/Desktop implementation | `src/jvmMain/kotlin/` |
| **commonTest** | Shared tests | `src/commonTest/kotlin/` |
| **androidTest** | Android instrumented tests | `src/androidTest/kotlin/` |

### Typical Module Structure

```
Module/Feature/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/{module}/{feature}/
│   │   ├── Models.kt           # Data models (cross-platform)
│   │   ├── Repository.kt       # Repository interfaces
│   │   ├── UseCase.kt          # Business logic
│   │   └── ViewModel.kt        # Shared view model
│   │
│   ├── androidMain/kotlin/com/augmentalis/{module}/{feature}/
│   │   ├── AndroidRepository.kt    # Android SQLDelight implementation
│   │   └── AndroidSpecifics.kt     # Platform APIs
│   │
│   ├── iosMain/kotlin/com/augmentalis/{module}/{feature}/
│   │   ├── IosRepository.kt        # iOS SQLDelight implementation
│   │   └── IosSpecifics.kt         # Platform APIs
│   │
│   ├── commonTest/kotlin/
│   │   └── UseCaseTest.kt
│   │
│   └── androidTest/kotlin/
│       └── AndroidRepositoryTest.kt
│
└── build.gradle.kts
```

### Build Configuration Example

```kotlin
// Modules/AVAMagic/MagicUI/Theme/build.gradle.kts

kotlin {
    // Platform targets
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        browser()
    }
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":Modules:AVAMagic:Core"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.compose.runtime)
                implementation(libs.androidx.compose.material3)
            }
        }

        val iosMain by getting {
            dependencies {
                // iOS-specific dependencies
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
```

---

## Documentation Structure (UNIVERSAL)

### Root-Level Organization (Docs/)

```
Docs/
├── AVAMagic/                       # AVAMagic module documentation
│   ├── Analysis/
│   │   ├── AVAMagic-Analysis-ThemeCreator-251223-V1.md
│   │   └── AVAMagic-Analysis-Spatial-Materials-251223-V1.md
│   │
│   ├── Plans/
│   │   ├── AVAMagic-Plan-Theme-System-251223-V1.md
│   │   └── AVAMagic-Plan-Code-Generator-251223-V1.md
│   │
│   ├── Specifications/
│   │   ├── AVAMagic-Spec-MagicUI-Theme-251223-V1.md
│   │   └── AVAMagic-Spec-MagicCode-Parser-251223-V1.md
│   │
│   ├── Refactoring/
│   │   ├── AVAMagic-Refactoring-Map-251223-V1.md
│   │   ├── AVAMagic-Refactoring-Checkpoint-251223-V1.md
│   │   └── AI-Refactoring-Instructions-251223-V1.md
│   │
│   ├── Standards/
│   │   └── AVAMagic-Folder-Naming-Standards-251223-V1.md
│   │
│   └── README.md
│
├── VoiceOS/                        # VoiceOS module documentation
│   ├── Analysis/
│   ├── Plans/
│   ├── Specifications/
│   ├── Issues/
│   ├── Manuals/
│   └── README.md
│
├── AVA/                            # AVA module documentation
│   ├── Analysis/
│   ├── Plans/
│   └── README.md
│
├── NLU/                            # NLU module documentation
│   ├── Analysis/
│   └── README.md
│
├── Architecture/                   # Monorepo-wide architecture
│   ├── NewAvanues-Architecture-Overview-251223-V1.md
│   └── NewAvanues-KMP-Strategy-251223-V1.md
│
├── Guides/                         # Development guides
│   ├── NewAvanues-Developer-Onboarding-251223-V1.md
│   ├── NewAvanues-KMP-Best-Practices-251223-V1.md
│   └── NewAvanues-Testing-Strategy-251223-V1.md
│
├── Standards/                      # Monorepo-wide standards
│   ├── NewAvanues-Naming-Standards-251223-V1.md
│   ├── NewAvanues-Code-Quality-Standards-251223-V1.md
│   └── NewAvanues-Monorepo-KMP-Structure-Guide-251223-V1.md (this file)
│
├── Cross-Module/                   # Cross-module documentation
│   └── CROSS-MODULE-DEPENDENCIES.md
│
└── Project-Info/                   # Project metadata
    ├── PROJECT-REGISTRY.json
    └── VERSION-INFO.json
```

### Documentation Categories

| Category | Location | Purpose | Example |
|----------|----------|---------|---------|
| **Analysis** | `Docs/{Module}/Analysis/` | Technical research and findings | Theme creator analysis |
| **Plans** | `Docs/{Module}/Plans/` | Implementation planning | Feature implementation plans |
| **Specifications** | `Docs/{Module}/Specifications/` | Feature specifications | API specs, UI specs |
| **Refactoring** | `Docs/{Module}/Refactoring/` | Refactoring documentation | Refactoring maps, checkpoints |
| **Standards** | `Docs/{Module}/Standards/` | Module-specific standards | Naming conventions |
| **Issues** | `Docs/{Module}/Issues/` | Issue documentation | Bug reports, investigations |
| **Manuals** | `Docs/{Module}/Manuals/` | User/developer manuals | How-to guides |
| **Architecture** | `Docs/Architecture/` | System architecture | Architecture decisions |
| **Guides** | `Docs/Guides/` | Development guides | Setup, workflows |
| **Standards** | `Docs/Standards/` | Monorepo standards | Universal conventions |

### Documentation Naming Convention

**Format:** `{Module}-{Type}-{Description}-{YYMMDD}-V{#}.md`

**Examples:**
- `AVAMagic-Analysis-ThemeCreator-251223-V1.md`
- `VoiceOS-Spec-AccessibilityService-251223-V1.md`
- `NewAvanues-Guide-KMP-Setup-251223-V1.md`

**Type Options:**
- Analysis
- Plan
- Spec
- Guide
- Standard
- Refactoring
- Issue
- Manual

### Module README Structure

Each module documentation folder should have a README:

```markdown
# AVAMagic - Documentation

**Module:** AVAMagic
**Products:** MagicUI (UI framework), MagicCode (code generation), MagicTools (dev tools)

## Quick Links

- [Analysis](Analysis/) - Technical research and findings
- [Plans](Plans/) - Implementation plans
- [Specifications](Specifications/) - Feature specifications
- [Refactoring](Refactoring/) - Refactoring documentation
- [Standards](Standards/) - Module-specific standards

## Key Documents

- [Theme Creator Analysis](Analysis/AVAMagic-Analysis-ThemeCreator-251223-V1.md)
- [Folder Naming Standards](Standards/AVAMagic-Folder-Naming-Standards-251223-V1.md)
- [Refactoring Map](Refactoring/AVAMagic-Refactoring-Map-251223-V1.md)

## Related Documentation

- [NewAvanues Architecture](../Architecture/)
- [KMP Structure Guide](../Standards/NewAvanues-Monorepo-KMP-Structure-Guide-251223-V1.md)
```

---

## Package Naming Standards

### Universal Namespace

**Base:** `com.augmentalis.{module}.{product}.{feature}`

### KMP Module Packages

| Module | Product | Example Package |
|--------|---------|-----------------|
| AVAMagic | MagicUI | `com.augmentalis.magicui.theme` |
| AVAMagic | MagicCode | `com.augmentalis.magiccode.generator` |
| AVAMagic | Core | `com.augmentalis.avamagic.core` |
| VoiceOS | Core | `com.augmentalis.voiceos.core` |
| VoiceOS | Accessibility | `com.augmentalis.voiceos.accessibility` |
| AVA | Core | `com.augmentalis.ava.core` |
| NLU | Engine | `com.augmentalis.nlu.engine` |

### Platform App Packages

| Platform | App | Example Package |
|----------|-----|-----------------|
| Android | VoiceOS | `com.augmentalis.voiceos` |
| Android | AVA | `com.augmentalis.ava` |
| iOS | VoiceOS | `com.augmentalis.voiceos` (Swift) |
| Web | WebAvanue | TypeScript modules, not packages |

### Rules

1. **No domain mixing:** Never use `net.ideahq.*` (use `com.augmentalis.*`)
2. **No typos:** Never use `com.augmentalis.avanues.*` (remove extra 's')
3. **Module isolation:** `com.augmentalis.voiceos.*` only in VoiceOS module
4. **Product clarity:** Use product names in packages (`magicui`, `magiccode`)
5. **Shared code:** Common libraries use `com.augmentalis.common.*`

---

## Folder Types: Modules vs Organizational Folders

### Understanding the Three Types

| Type | Has `build.gradle.kts`? | Has `src/`? | Purpose | Example |
|------|------------------------|-------------|---------|---------|
| **Gradle Module** | ✅ Yes | ✅ Yes | Contains actual code | `Core/`, `Theme/`, `Parser/` |
| **Product Folder** | ❌ No | ❌ No | Groups modules by product | `MagicUI/`, `MagicCode/` |
| **Organizational Folder** | ❌ No | ❌ No | Groups similar modules | `Components/`, `Renderers/` |

### How to Identify Each Type

**Gradle Module:**
```
Theme/
├── src/
│   ├── commonMain/kotlin/
│   └── androidMain/kotlin/
└── build.gradle.kts        # HAS build file
```
**Gradle path:** `:Modules:AVAMagic:MagicUI:Theme`

**Product/Organizational Folder:**
```
MagicUI/
├── Theme/                  # Module
├── Components/             # Folder
└── Renderers/              # Folder
```
**Gradle path:** None (it's just a folder, not a module)

### When to Use Each

| Use | When | Don't Use | Why |
|-----|------|-----------|-----|
| **Gradle Module** | You need to write code | - | Every piece of code goes in a module |
| **Product Folder** | Grouping by product name | For single products | `MagicUI/` groups UI-related modules |
| **Organizational Folder** | Many similar modules | For 1-2 modules | `Components/` when you have 5+ component modules |
| **Flat Structure** | 10 or fewer modules | - | Simplest - no nesting |

### Anti-Patterns (Don't Do This)

❌ **Generic folders like `/libraries`, `/core`, `/features`**
```
Modules/VoiceOS/
├── libraries/              # ❌ Everything is already a library!
│   ├── LearnAppCore/
│   └── JITLearning/
└── core/                   # ❌ What makes it "core"? Be specific
    └── database/
```

✅ **Flat structure instead:**
```
Modules/VoiceOS/
├── database/               # ✅ Clear purpose
├── LearnAppCore/           # ✅ Clear purpose
└── JITLearning/            # ✅ Clear purpose
```

❌ **Confusing folders with modules**
```
core/                       # Is this a module or folder? Unclear!
├── src/                    # Oh, it's a module
└── build.gradle.kts
```

✅ **Make it obvious:**
```
Core/                       # Capital = likely a module
├── src/
└── build.gradle.kts

# OR (if it's a folder)
database/                   # Lowercase, clearly a module
├── src/
└── build.gradle.kts
```

### Special Case: `/apps` Folder

The **only** organizational folder you should consistently use:

```
Modules/VoiceOS/
├── database/               # KMP library module
├── models/                 # KMP library module
└── apps/                   # Folder containing platform apps
    └── VoiceOSCore/        # Android app (NOT a KMP library)
```

**Why?** Platform apps are fundamentally different from KMP libraries:
- Apps have `AndroidManifest.xml` or `Info.plist`
- Apps are end products, not libraries
- Apps consume libraries, they're not consumed

---

## Module Organization Patterns

### Pattern 1: Product-Based (AVAMagic)

```
Modules/AVAMagic/
├── Core/                   # Gradle module: shared utilities (has src/ and build.gradle.kts)
├── MagicUI/                # Product folder: UI framework
│   ├── Theme/              # Gradle module
│   ├── Components/         # Folder containing component modules
│   │   ├── Foundation/     # Gradle module
│   │   └── Phase2/         # Gradle module
│   └── Renderers/          # Folder containing renderer modules
│       ├── Android/        # Gradle module
│       ├── iOS/            # Gradle module
│       └── Web/            # Gradle module
├── MagicCode/              # Product folder: Code generation
│   ├── Parser/             # Gradle module
│   ├── Generator/          # Gradle module
│   └── Templates/          # Gradle module
└── MagicTools/             # Product folder: Development tools
    └── ThemeCreator/       # Gradle module (Tauri app)
```

**Documentation:** All in `Docs/AVAMagic/`

**Key Principle:**
- **Gradle modules** have `src/` and `build.gradle.kts` (e.g., `Core/`, `Theme/`, `Parser/`)
- **Product folders** group related modules by product name (e.g., `MagicUI/`, `MagicCode/`)
- **Organizational folders** group similar modules (e.g., `Components/`, `Renderers/`)

**Use when:** Module contains multiple distinct products

### Pattern 2: Feature-Based (VoiceOS)

```
Modules/VoiceOS/
├── database/               # Database module
├── models/                 # Models module
├── repositories/           # Repositories module
├── LearnAppCore/           # Supporting module
├── JITLearning/            # Supporting module
├── accessibility/          # Feature module
├── commands/               # Feature module
├── learning/               # Feature module
└── apps/                   # Platform-specific apps ONLY (not KMP libraries)
    └── VoiceOSCore/        # Android accessibility service
```

**Documentation:** All in `Docs/VoiceOS/`

**Key Principle:** Everything under `Modules/VoiceOS/` is a Gradle module/library (flat structure). Only use folders for true structural separation:
- `apps/` - **ONLY** for platform-specific apps (Android/iOS apps that consume the libraries)
- Do **NOT** use: `core/`, `libraries/`, `features/` folders - these are redundant

**Use when:** Module is a single cohesive system with multiple feature modules

### Pattern 3: Library Collection (Common)

```
Common/
├── Core/                   # Core utilities
│   └── src/commonMain/kotlin/com/augmentalis/common/core/
├── Data/                   # Data utilities
│   └── src/commonMain/kotlin/com/augmentalis/common/data/
└── Utils/                  # Utility functions
    └── src/commonMain/kotlin/com/augmentalis/common/utils/
```

**Documentation:** Minimal, mostly README files

**Use when:** Collection of independent utility libraries

---

## Build System Organization

### Root settings.gradle.kts

```kotlin
// Include platform apps
include(":android:VoiceOS:app")
include(":android:AVA:app")
include(":web:WebAvanue")

// Include KMP modules
include(":Modules:AVAMagic:Core")
include(":Modules:AVAMagic:MagicUI:Theme")
include(":Modules:AVAMagic:MagicUI:Components:Foundation")
include(":Modules:AVAMagic:MagicCode:Generator")

include(":Modules:VoiceOS:core")
include(":Modules:VoiceOS:apps:VoiceOSCore")

include(":Common:Core")
include(":Common:Data")
```

### Version Catalogs

```
version-catalogs/
├── libs.versions.toml      # Main version catalog
└── build-logic.toml        # Build logic versions
```

**libs.versions.toml:**
```toml
[versions]
kotlin = "1.9.22"
compose = "1.5.11"
kmp = "1.9.22"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version = "1.7.3" }
compose-runtime = { module = "androidx.compose.runtime:runtime", version.ref = "compose" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kmp" }
android-application = { id = "com.android.application", version = "8.2.0" }
```

---

## Cross-Platform Code Sharing

### expect/actual Pattern

Use for platform-specific implementations:

**commonMain:**
```kotlin
// Modules/AVAMagic/Core/src/commonMain/kotlin/Platform.kt
package com.augmentalis.avamagic.core

expect class Platform() {
    val name: String
}
```

**androidMain:**
```kotlin
// Modules/AVAMagic/Core/src/androidMain/kotlin/Platform.kt
package com.augmentalis.avamagic.core

actual class Platform {
    actual val name: String = "Android"
}
```

**iosMain:**
```kotlin
// Modules/AVAMagic/Core/src/iosMain/kotlin/Platform.kt
package com.augmentalis.avamagic.core

actual class Platform {
    actual val name: String = "iOS"
}
```

### Shared Interfaces, Platform Implementations

**commonMain:**
```kotlin
interface ThemeRepository {
    suspend fun getTheme(id: String): Theme
    suspend fun saveTheme(theme: Theme)
}
```

**androidMain:**
```kotlin
class AndroidThemeRepository(
    private val database: Database
) : ThemeRepository {
    override suspend fun getTheme(id: String): Theme {
        // SQLDelight Android implementation
    }
}
```

**iosMain:**
```kotlin
class IosThemeRepository(
    private val database: Database
) : ThemeRepository {
    override suspend fun getTheme(id: String): Theme {
        // SQLDelight iOS implementation
    }
}
```

---

## Database Organization (SQLDelight)

### Structure

```
Modules/{Module}/core/database/
├── src/
│   ├── commonMain/
│   │   ├── kotlin/com/augmentalis/{module}/database/
│   │   │   ├── repositories/
│   │   │   │   ├── IThemeRepository.kt
│   │   │   │   └── impl/
│   │   │   │       └── SQLDelightThemeRepository.kt
│   │   │   └── Database.kt
│   │   │
│   │   └── sqldelight/com/augmentalis/{module}/
│   │       └── database/
│   │           └── Theme.sq
│   │
│   ├── androidMain/kotlin/
│   │   └── AndroidDatabaseDriver.kt
│   │
│   └── iosMain/kotlin/
│       └── IosDatabaseDriver.kt
│
└── build.gradle.kts
```

**SQLDelight Configuration:**
```kotlin
sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.augmentalis.avamagic.database")
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}
```

---

## Testing Structure

### Test Organization

```
Module/Feature/
└── src/
    ├── commonTest/kotlin/          # Shared unit tests
    │   ├── UseCaseTest.kt
    │   └── RepositoryTest.kt
    │
    ├── androidUnitTest/kotlin/     # Android unit tests
    │   └── AndroidSpecificTest.kt
    │
    ├── androidInstrumentedTest/    # Android instrumented tests
    │   └── DatabaseTest.kt
    │
    └── iosTest/kotlin/             # iOS tests
        └── IosSpecificTest.kt
```

### Test Naming

| Test Type | Location | Example |
|-----------|----------|---------|
| Unit (shared) | `commonTest/` | `ThemeParserTest.kt` |
| Unit (Android) | `androidUnitTest/` | `AndroidThemeRepositoryTest.kt` |
| Instrumented | `androidInstrumentedTest/` | `DatabaseMigrationTest.kt` |
| iOS | `iosTest/` | `IosThemeRepositoryTest.kt` |

---

## Documentation Migration

### Current State (AVAMagic)

Documentation currently exists at:
- `Modules/AVAMagic/Docs/` ❌ (needs migration)

### Target State

All documentation moves to:
- `Docs/AVAMagic/` ✅

### Migration Plan

```bash
# Create target directory
mkdir -p Docs/AVAMagic

# Move all documentation
mv Modules/AVAMagic/Docs/* Docs/AVAMagic/

# Remove old directory
rmdir Modules/AVAMagic/Docs
```

### Files to Migrate

| Current Location | New Location |
|------------------|--------------|
| `Modules/AVAMagic/Docs/AVAMagic-Analysis-ThemeCreator-251223-V1.md` | `Docs/AVAMagic/Analysis/AVAMagic-Analysis-ThemeCreator-251223-V1.md` |
| `Modules/AVAMagic/Docs/AVAMagic-Folder-Naming-Standards-251223-V1.md` | `Docs/AVAMagic/Standards/AVAMagic-Folder-Naming-Standards-251223-V1.md` |
| `Modules/AVAMagic/Docs/AI-Refactoring-Instructions-251223-V1.md` | `Docs/AVAMagic/Refactoring/AI-Refactoring-Instructions-251223-V1.md` |
| `Modules/AVAMagic/Docs/AVAMagic-Refactoring-Map-251223-V1.md` | `Docs/AVAMagic/Refactoring/AVAMagic-Refactoring-Map-251223-V1.md` |
| `Modules/AVAMagic/Docs/AVAMagic-Refactoring-Checkpoint-251223-V1.md` | `Docs/AVAMagic/Refactoring/AVAMagic-Refactoring-Checkpoint-251223-V1.md` |
| `Modules/AVAMagic/Docs/AVAMagic-Renaming-Strategy-251223-V1.md` | `Docs/AVAMagic/Refactoring/AVAMagic-Renaming-Strategy-251223-V1.md` |

---

## Migration Checklist

When refactoring to this structure:

### Phase 1: Audit
- [ ] Map current directories to new structure
- [ ] Identify package naming inconsistencies
- [ ] List all files to move (with counts)
- [ ] Document breaking changes

### Phase 2: Plan
- [ ] Create refactoring map document
- [ ] Get approval for structure changes
- [ ] Create git branch for refactoring
- [ ] Update documentation

### Phase 3: Execute
- [ ] Create new directory structure
- [ ] Migrate documentation to universal Docs/
- [ ] Move code files to new locations
- [ ] Update package declarations
- [ ] Update import statements
- [ ] Update build files (settings.gradle.kts, build.gradle.kts)

### Phase 4: Verify
- [ ] All builds succeed
- [ ] All tests pass
- [ ] No old package names remain
- [ ] Documentation updated
- [ ] Backwards compatibility maintained

### Phase 5: Cleanup
- [ ] Remove empty directories
- [ ] Create deprecation aliases
- [ ] Update README files
- [ ] Document completion

---

## Reference Implementation: AVAMagic

AVAMagic serves as the reference implementation for this structure:

**Key Features:**
- ✅ Product-based organization (MagicUI, MagicCode, MagicTools)
- ✅ Clear package naming (`com.augmentalis.magicui.*`)
- ✅ KMP source sets (commonMain, androidMain, iosMain)
- ✅ Universal documentation (`Docs/AVAMagic/`)
- ✅ Explicit folder names (not generic `ui/`, `tools/`)

**Apply this pattern** to other modules:
- VoiceOS → Feature-based (flat modules + `apps/` for Android app)
- WebAvanue → Web-specific with Tauri structure
- NLU → Library collection pattern (flat modules)

---

## Tools and Commands

### Useful Gradle Commands

```bash
# List all projects
./gradlew projects

# Build specific module
./gradlew :Modules:AVAMagic:MagicUI:Theme:build

# Run tests
./gradlew :Modules:AVAMagic:test

# Android build
./gradlew :android:VoiceOS:app:assembleDebug

# Check dependencies
./gradlew :Modules:AVAMagic:Core:dependencies
```

### Verification Scripts

```bash
# Find old package names
grep -r "net.ideahq" --include="*.kt"
grep -r "com.augmentalis.avanues" --include="*.kt"

# Find misplaced packages (VoiceOS code in AVAMagic)
grep -r "com.augmentalis.voiceos" Modules/AVAMagic/ --include="*.kt"

# Count files by source set
find . -path "*/commonMain/*" -name "*.kt" | wc -l
find . -path "*/androidMain/*" -name "*.kt" | wc -l

# Verify documentation is in correct location
find Modules/ -type d -name "Docs" # Should return nothing
find Docs/ -type d -mindepth 1 -maxdepth 1 # Should show module folders
```

---

## Related Documentation

| Document | Location | Purpose |
|----------|----------|---------|
| AVAMagic Folder Standards | `Docs/AVAMagic/Standards/` | Module-specific standards |
| AVAMagic Refactoring Map | `Docs/AVAMagic/Refactoring/` | AVAMagic refactoring plan |
| Cross-Module Dependencies | `Docs/Cross-Module/` | Inter-module dependencies |
| Developer Onboarding | `Docs/Guides/` | Setup and development guide |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.1 | 2025-12-23 | Removed `/libraries` pattern, clarified folder types, added anti-patterns |
| 1.0 | 2025-12-23 | Initial KMP-aware structure guide with universal Docs/ |

---

**Status:** Active Standard
**Scope:** NewAvanues monorepo - all modules and platform apps
