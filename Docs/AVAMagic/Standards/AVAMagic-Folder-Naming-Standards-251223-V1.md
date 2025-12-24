# Folder and Naming Standards - NewAvanues Monorepo

**Scope:** All modules in NewAvanues monorepo
**Date:** 2025-12-23
**Version:** 1.0

---

## Executive Summary

This document establishes unified folder structure and naming conventions for the entire NewAvanues monorepo, supporting Kotlin Multiplatform (KMP) projects across Android, iOS, Web, and Desktop.

**Key Principles:**
1. **No Redundancy** - Avoid repeating names in paths and packages
2. **Flat Where Possible** - Minimize nesting depth
3. **KMP Standard** - Follow Kotlin Multiplatform conventions
4. **Semantic Clarity** - Names should be self-documenting
5. **Platform Agnostic** - Core structure works across all platforms

---

## Monorepo Structure

```
NewAvanues/
├── Modules/
│   ├── AVA/              # AI Assistant platform
│   ├── VoiceOS/          # Voice-first accessibility
│   ├── AVAMagic/         # AVAui (UI system) + AVAcode (code gen)
│   ├── WebAvanue/        # Web platform (Tauri)
│   ├── Cockpit/          # Management dashboard
│   └── NLU/              # Natural language understanding
│
├── Common/               # Shared KMP libraries
└── Docs/                 # Monorepo-level documentation
```

---

## Module Structure Standard

### Top-Level Organization

Every module MUST follow this structure:

```
Modules/{ModuleName}/
├── .claude/              # Claude Code configuration
├── .ideacode/            # IDEACODE registries
├── Docs/                 # Module documentation
├── core/                 # Core utilities (required)
├── {feature}/            # Feature-specific directories
├── apps/                 # Application entry points (if applicable)
├── build.gradle.kts      # Root build file
└── settings.gradle.kts   # Module settings
```

### Feature Organization Patterns

Choose ONE of these patterns based on module complexity:

#### Pattern A: Flat Feature Structure (Simple modules)
**When:** Module has < 10 features, minimal complexity

```
Modules/{ModuleName}/
├── core/                 # Core utilities, models, constants
├── data/                 # Data layer (repositories, sources)
├── domain/               # Business logic (use cases, entities)
├── ui/                   # UI layer (if applicable)
└── apps/                 # Applications
```

**Example (AVA):**
```
Modules/AVA/
├── core/                 # Core AVA utilities
├── llm/                  # LLM integration
├── memory/               # Memory management
├── chat/                 # Chat functionality
├── voice/                # Voice processing
├── actions/              # Action system
└── apps/                 # AVA applications
```

#### Pattern B: Categorized Structure (Complex modules)
**When:** Module has 10+ features, needs organization

```
Modules/{ModuleName}/
├── core/                 # Core utilities
│   ├── {utility-name}/   # Individual utilities
│   └── ...
├── features/             # Feature implementations
│   ├── {feature-name}/
│   └── ...
├── libraries/            # Reusable libraries
│   ├── {library-name}/
│   └── ...
├── managers/             # Manager components (if needed)
│   ├── {manager-name}/
│   └── ...
└── apps/                 # Applications
```

**Example (VoiceOS - current structure, GOOD):**
```
Modules/VoiceOS/
├── core/                 # Core utilities
│   ├── database/
│   ├── logging/
│   ├── validation/
│   └── ...
├── libraries/            # Reusable libs
│   ├── PluginSystem/
│   ├── UniversalIPC/
│   └── ...
├── managers/             # Managers
│   ├── CommandManager/
│   ├── HUDManager/
│   └── ...
└── apps/                 # Applications
    ├── VoiceOSCore/
    └── LearnApp/
```

---

## AVAMagic Proposed Structure

### Current (Redundant, Too Nested)
```
Modules/AVAMagic/
├── UI/
│   ├── ThemeManager/src/commonMain/kotlin/com/augmentalis/ideamagic/ui/thememanager/
│   ├── DesignSystem/src/commonMain/kotlin/com/augmentalis/ideamagic/designsystem/
│   └── Core/
├── Components/
├── Renderers/
├── CodeGen/
└── ...
```

**Problems:**
- `UI/ThemeManager` - redundant (UI → ThemeManager)
- `ideamagic/ui/thememanager` - repeats path in package
- Too many top-level directories (18+)
- Inconsistent categorization

### Proposed (Clean, Semantic)
```
Modules/AVAMagic/
├── .claude/
├── .ideacode/
├── Docs/
│
├── core/                             # Core utilities
│   ├── responsive/                   # Responsive utilities
│   └── ipc/                          # IPC utilities
│
├── theme/                            # Theme system (was UI/ThemeManager + UI/DesignSystem)
│   ├── src/commonMain/kotlin/com/augmentalis/avaui/theme/
│   │   ├── io/                       # Import/export
│   │   ├── tokens/                   # Design tokens
│   │   ├── manager/                  # Theme management
│   │   └── data/                     # Theme data models
│   └── build.gradle.kts
│
├── components/                       # UI components (was Components/)
│   ├── foundation/                   # Foundation components
│   ├── phase2/                       # Phase 2 components
│   ├── phase3/                       # Phase 3 components
│   └── builder/                      # Theme builder UI
│
├── renderers/                        # Platform renderers (keep as is)
│   ├── android/
│   ├── ios/
│   ├── web/
│   └── desktop/
│
├── codegen/                          # Code generation (becomes AVAcode)
│   ├── parser/
│   ├── generator/
│   └── templates/
│
├── tools/                            # Development tools
│   └── themecreator/                 # Theme Creator app
│
└── apps/                             # Sample applications
    └── showcase/
```

---

## Package Naming Convention

### Standard Pattern

```
com.augmentalis.{product}.{feature}[.{subfeature}]
```

### Rules

| Rule | Description | Example |
|------|-------------|---------|
| **No Module Name** | Don't repeat module name in package | ❌ `com.augmentalis.avamagic.avaui` <br> ✅ `com.augmentalis.avaui` |
| **Product = Brand** | Use customer-facing name | `avaui`, `voiceos`, `ava` (not `avamagic`, `voiceos-accessibility`) |
| **Short Features** | Keep feature names concise | ✅ `theme`, `components` <br> ❌ `thememanagement`, `uicomponents` |
| **No Redundancy** | Don't repeat path in package | ❌ `com.augmentalis.avaui.ui.theme` <br> ✅ `com.augmentalis.avaui.theme` |
| **Lowercase** | All lowercase, no camelCase | ✅ `com.augmentalis.avaui.theme.io` <br> ❌ `com.augmentalis.avaUi.ThemeIO` |

### Package Examples

| Module | Feature | Package |
|--------|---------|---------|
| AVAMagic | Theme management | `com.augmentalis.avaui.theme` |
| AVAMagic | Theme I/O | `com.augmentalis.avaui.theme.io` |
| AVAMagic | Components | `com.augmentalis.avaui.components` |
| AVAMagic | Renderers | `com.augmentalis.avaui.renderers.android` |
| AVAMagic | Code generation | `com.augmentalis.avacode.parser` |
| VoiceOS | Database | `com.augmentalis.voiceos.database` |
| VoiceOS | Command Manager | `com.augmentalis.voiceos.commands` |
| AVA | LLM | `com.augmentalis.ava.llm` |
| AVA | Memory | `com.augmentalis.ava.memory` |

### Package-to-Path Mapping

**Kotlin Multiplatform:**
```
Directory: theme/src/commonMain/kotlin/com/augmentalis/avaui/theme/io/
Package:   com.augmentalis.avaui.theme.io
```

**TypeScript/JavaScript:**
```
Directory: renderers/web/src/theme/
Import:    @avaui/theme or ./theme
```

---

## KMP Source Set Structure

### Standard Layout (Gradle KMP)

```
{feature}/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/com/augmentalis/{product}/{feature}/
    ├── commonTest/kotlin/com/augmentalis/{product}/{feature}/
    ├── androidMain/kotlin/com/augmentalis/{product}/{feature}/
    ├── androidUnitTest/kotlin/com/augmentalis/{product}/{feature}/
    ├── androidInstrumentedTest/kotlin/com/augmentalis/{product}/{feature}/
    ├── iosMain/kotlin/com/augmentalis/{product}/{feature}/
    ├── iosTest/kotlin/com/augmentalis/{product}/{feature}/
    ├── jsMain/kotlin/com/augmentalis/{product}/{feature}/
    ├── jsTest/kotlin/com/augmentalis/{product}/{feature}/
    ├── jvmMain/kotlin/com/augmentalis/{product}/{feature}/
    └── jvmTest/kotlin/com/augmentalis/{product}/{feature}/
```

### Source Set Naming

| Source Set | Purpose | Required |
|------------|---------|----------|
| `commonMain` | Shared code across all platforms | ✅ Yes |
| `commonTest` | Shared tests | ✅ Yes |
| `androidMain` | Android-specific code | If targeting Android |
| `androidUnitTest` | Android unit tests (JVM) | If targeting Android |
| `androidInstrumentedTest` | Android instrumented tests (device) | If targeting Android |
| `iosMain` | iOS-specific code | If targeting iOS |
| `iosTest` | iOS tests | If targeting iOS |
| `jsMain` | Web/JS-specific code | If targeting Web |
| `jvmMain` | Desktop/Server-specific code | If targeting JVM |

---

## Naming Conventions

### File Naming

| Type | Convention | Example |
|------|------------|---------|
| **Kotlin Class** | PascalCase | `ThemeIOManager.kt` |
| **Kotlin Interface** | PascalCase, often ends in `-er` or describes capability | `ThemeImporter.kt`, `Serializable.kt` |
| **Kotlin Object** | PascalCase | `Themes.kt`, `Constants.kt` |
| **Kotlin Extension** | Feature + Extensions | `ColorExtensions.kt`, `ThemeExtensions.kt` |
| **TypeScript Component** | PascalCase | `ThemeEditor.tsx`, `ColorPicker.tsx` |
| **TypeScript Utility** | camelCase | `themeConverter.ts`, `validators.ts` |
| **Test File** | Class + Test | `ThemeIOManagerTest.kt` |
| **Documentation** | `{Module}-{Type}-{Topic}-{YYMMDD}-V#.md` | `AVAMagic-Analysis-ThemeCreator-251223-V1.md` |

### Directory Naming

| Type | Convention | Example |
|------|------------|---------|
| **Feature** | lowercase, singular or plural based on content | `theme/`, `components/`, `renderer/` |
| **Multi-word** | lowercase with dash (if needed) | `theme-builder/`, `design-tokens/` |
| **Avoid** | camelCase, PascalCase in directories | ❌ `ThemeManager/`, `themeManager/` |

### Class Naming

| Type | Pattern | Example |
|------|---------|---------|
| **Manager** | `{Feature}Manager` | `ThemeManager`, `CommandManager` |
| **Repository** | `{Entity}Repository` | `ThemeRepository`, `UserRepository` |
| **Service** | `{Feature}Service` | `ThemeService`, `AuthService` |
| **ViewModel** | `{Screen}ViewModel` | `ThemeEditorViewModel` |
| **Adapter** | `{Source}To{Target}Adapter` | `FigmaToThemeAdapter` |
| **Parser** | `{Format}Parser` | `W3CTokenParser`, `JsonParser` |
| **Serializer** | `{Format}Serializer` | `JsonSerializer`, `XmlSerializer` |
| **Validator** | `{Feature}Validator` | `ThemeValidator`, `ContrastValidator` |
| **Util** | `{Feature}Utils` or `{Feature}Helper` | `ColorUtils`, `StringHelper` |

---

## Brand Naming

### Official Names

| Old Name | New Name | Usage |
|----------|----------|-------|
| MagicUI | **AVAui** | UI framework and design system |
| IdeaMagic | **AVAui** | (same as above, consolidated) |
| MagicCode | **AVAcode** | Code generation tools |
| AVAMagic | **AVAMagic** | Module name (keep as container) |

### Package Prefixes

| Product | Package Prefix | Example |
|---------|----------------|---------|
| AVAui | `com.augmentalis.avaui` | `com.augmentalis.avaui.theme` |
| AVAcode | `com.augmentalis.avacode` | `com.augmentalis.avacode.parser` |
| VoiceOS | `com.augmentalis.voiceos` | `com.augmentalis.voiceos.commands` |
| AVA | `com.augmentalis.ava` | `com.augmentalis.ava.llm` |

### URL Schemes

| Product | Scheme | Example |
|---------|--------|---------|
| AVAui | `avaui://` | `avaui://theme?data=...` |
| VoiceOS | `voiceos://` | `voiceos://command?action=...` |
| AVA | `ava://` | `ava://chat?query=...` |

---

## Documentation Structure

### Per-Module Documentation

```
Modules/{ModuleName}/
└── Docs/
    ├── README.md                           # Module overview
    ├── {Module}-Analysis-{Topic}-{Date}-V#.md
    ├── {Module}-Plan-{Topic}-{Date}-V#.md
    ├── {Module}-Manual-{Topic}-{Date}-V#.md
    └── Architecture/
        ├── {Module}-Architecture-{Date}-V#.md
        └── diagrams/
```

### File Naming Pattern

```
{Module}-{Type}-{Topic}-{YYMMDD}-V#.md
```

**Components:**
- `{Module}`: Module name (e.g., AVAMagic, VoiceOS)
- `{Type}`: Document type (Analysis, Plan, Manual, Architecture, Issue, Spec)
- `{Topic}`: Brief description (e.g., ThemeCreator, CommandSystem)
- `{YYMMDD}`: Date (2-digit year, month, day)
- `V#`: Version number

**Examples:**
- `AVAMagic-Analysis-ThemeCreator-251223-V1.md`
- `VoiceOS-Plan-LearnApp-251220-V2.md`
- `AVA-Architecture-MemorySystem-251215-V1.md`

---

## Build File Organization

### Module-Level build.gradle.kts

```kotlin
// Modules/{ModuleName}/build.gradle.kts

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    // Target declarations
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
                // Common dependencies
            }
        }
        val androidMain by getting {
            dependencies {
                // Android dependencies
            }
        }
        // ... other source sets
    }
}
```

### Feature-Level build.gradle.kts

```kotlin
// Modules/{ModuleName}/{feature}/build.gradle.kts

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    // Inherit targets from module level or declare specific targets
    sourceSets {
        commonMain {
            dependencies {
                // Feature dependencies
                implementation(project(":Modules:{ModuleName}:core"))
            }
        }
    }
}
```

---

## Migration Checklist

When refactoring a module to follow these standards:

### Phase 1: Planning
- [ ] Review current structure
- [ ] Map old → new structure
- [ ] Identify all package renames
- [ ] Create migration plan document

### Phase 2: Directory Structure
- [ ] Create new directory structure
- [ ] Move files to new locations
- [ ] Update build.gradle.kts files
- [ ] Update settings.gradle.kts

### Phase 3: Package Renaming
- [ ] Update package declarations in all .kt files
- [ ] Update import statements
- [ ] Update fully qualified names in documentation

### Phase 4: Name Standardization
- [ ] Rename classes to follow conventions
- [ ] Rename files to match classes
- [ ] Update references in code

### Phase 5: Documentation
- [ ] Update README files
- [ ] Update module documentation
- [ ] Update architecture diagrams
- [ ] Update CLAUDE.md

### Phase 6: Backwards Compatibility
- [ ] Create type aliases for deprecated names
- [ ] Add deprecation warnings
- [ ] Create migration guide

### Phase 7: Verification
- [ ] Verify all imports resolve
- [ ] Run all tests
- [ ] Check build succeeds for all platforms
- [ ] Verify no broken references

---

## Common Patterns by Feature Type

### Theme System
```
theme/
├── src/commonMain/kotlin/com/augmentalis/avaui/theme/
│   ├── Theme.kt                      # Core theme models
│   ├── manager/
│   │   └── ThemeManager.kt           # Theme management
│   ├── io/
│   │   ├── ThemeIO.kt                # Import/export interfaces
│   │   ├── parsers/
│   │   │   ├── W3CTokenParser.kt
│   │   │   └── AVAuiParser.kt
│   │   └── exporters/
│   │       └── JsonExporter.kt
│   ├── tokens/
│   │   ├── ColorTokens.kt            # Design tokens
│   │   ├── TypographyTokens.kt
│   │   └── SpacingTokens.kt
│   └── data/                         # Theme data files
└── build.gradle.kts
```

### Component Library
```
components/
├── foundation/                        # Basic components
│   ├── src/commonMain/kotlin/com/augmentalis/avaui/components/
│   │   ├── Button.kt
│   │   ├── Text.kt
│   │   └── Card.kt
│   └── build.gradle.kts
├── advanced/                          # Complex components
└── builder/                           # Component builder tools
```

### Platform Renderer
```
renderers/
├── android/
│   └── src/androidMain/kotlin/com/augmentalis/avaui/renderers/android/
├── ios/
│   └── src/iosMain/kotlin/com/augmentalis/avaui/renderers/ios/
├── web/
│   └── src/jsMain/kotlin/com/augmentalis/avaui/renderers/web/
└── desktop/
    └── src/jvmMain/kotlin/com/augmentalis/avaui/renderers/desktop/
```

---

## Anti-Patterns to Avoid

| ❌ Anti-Pattern | ✅ Correct Pattern | Reason |
|----------------|-------------------|--------|
| `UI/ThemeManager/` | `theme/` | Redundant nesting |
| `com.augmentalis.ideamagic.ui.thememanager` | `com.augmentalis.avaui.theme` | Too long, redundant |
| `MagicUIParser` | `AVAuiParser` | Old branding |
| `magicui://` | `avaui://` | Old branding |
| `ThemeManager/src/main/kotlin/` | `theme/src/commonMain/kotlin/` | Not KMP standard |
| `utils/StringUtil.kt` | `core/text/StringUtils.kt` | Vague organization |
| `com.company.module.module.feature` | `com.company.product.feature` | Redundant module name |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-23 | Initial standards document |

---

**Status:** Active Standard
**Scope:** All NewAvanues modules
