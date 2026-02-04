# App Consolidation Framework Plan

**Date**: 2026-02-04
**Branch**: 040226-1-module-consolidation
**Status**: Planning

---

## Executive Summary

Consolidate common code, utilities, resources, and assets across VoiceOSCore, WebAvanue, and AVA apps into a shared framework to reduce duplication and improve maintainability.

## Recent Work Analysis (Last 96 Hours)

### WebAvanue Commits

| Commit | Description | Consolidation Relevance |
|--------|-------------|------------------------|
| `cfe164e7` | StateFlow utilities (BaseViewModel, UiState, etc.) | **HIGH** - Reusable across all apps |
| `fd3fdf20` | Repository pattern split | **MEDIUM** - Pattern can be shared |
| `3af965b8` | WebSocket sync module | **HIGH** - Standalone KMP module created |
| `e299d543` | VoiceCursor KMP + AVU 2.1 RPC | **HIGH** - New shared modules |

### Existing Shared Utilities

```
WebAvanue/util/
├── BaseViewModel.kt        # ViewModel base class
├── ListState.kt           # List manipulation helpers
├── SearchState.kt         # Search state management
├── UiState.kt             # Loading/error/success state
└── ViewModelState.kt      # StateFlow wrapper

VoiceOSCore/util/
└── NumberToWords.kt       # Number-to-text conversion

AVA/core/Data/util/
├── HashHelper.kt          # Hash generation (KMP)
├── TimeHelper.kt          # Time formatting (KMP)
└── AvidHelper.kt          # AVID utilities
```

---

## Consolidation Plan

### Phase 1: Create Shared Foundation Module

**New Module**: `Modules/Shared/Foundation`

```
Modules/Shared/Foundation/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/com/augmentalis/shared/
    │   ├── util/
    │   │   ├── HashHelper.kt         # From AVA/core/Data
    │   │   ├── TimeHelper.kt         # From AVA/core/Data
    │   │   └── NumberToWords.kt      # From VoiceOSCore
    │   ├── state/
    │   │   ├── UiState.kt            # From WebAvanue
    │   │   ├── ListState.kt          # From WebAvanue
    │   │   ├── SearchState.kt        # From WebAvanue
    │   │   └── ViewModelState.kt     # From WebAvanue
    │   └── viewmodel/
    │       └── BaseViewModel.kt      # From WebAvanue
    ├── androidMain/
    └── iosMain/
```

### Phase 2: Consolidate Common Resources

**New Module**: `Modules/Shared/Resources`

```
Modules/Shared/Resources/
├── src/
│   ├── commonMain/
│   │   └── resources/
│   │       ├── strings/              # Shared strings
│   │       │   ├── common.xml
│   │       │   └── errors.xml
│   │       └── values/
│   │           └── colors.xml        # Shared colors
│   └── androidMain/
│       └── res/
│           ├── drawable/             # Shared icons
│           │   ├── ic_mic.xml
│           │   ├── ic_settings.xml
│           │   └── ic_error.xml
│           └── values/
│               └── themes.xml        # Base theme
```

### Phase 3: Consolidate Common Assets

**Location**: `Modules/Shared/Assets`

```
Modules/Shared/Assets/
└── src/
    └── commonMain/
        └── assets/
            ├── filters/              # From VoiceOSCore
            │   ├── en-US/
            │   ├── de-DE/
            │   ├── es-ES/
            │   └── fr-FR/
            ├── commands/             # Shared commands
            │   └── common.vos
            ├── intents/              # Shared intents
            │   └── common.aai
            └── categories/           # App categories
                └── known-apps.acd
```

### Phase 4: Create Shared Theme Module

**New Module**: `Modules/Shared/Theme`

```
Modules/Shared/Theme/
└── src/
    └── commonMain/kotlin/com/augmentalis/shared/theme/
        ├── AvanuesColors.kt          # Color definitions
        ├── AvanuesTypography.kt      # Typography
        ├── AvanuesShapes.kt          # Shape definitions
        └── AvanuesTheme.kt           # Theme composition
```

---

## Dependency Structure

### Before (Current)

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  VoiceOS    │     │  WebAvanue  │     │    AVA      │
│    Core     │     │             │     │             │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ util/       │     │ util/       │     │ util/       │
│ themes/     │     │ themes/     │     │ themes/     │
│ assets/     │     │ assets/     │     │ assets/     │
└─────────────┘     └─────────────┘     └─────────────┘
   (duplicated)        (duplicated)        (duplicated)
```

### After (Consolidated)

```
                    ┌─────────────────┐
                    │ Shared/         │
                    │ Foundation      │
                    │ Resources       │
                    │ Theme           │
                    │ Assets          │
                    └────────┬────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
    ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
    │  VoiceOS    │   │  WebAvanue  │   │    AVA      │
    │    Core     │   │             │   │             │
    └─────────────┘   └─────────────┘   └─────────────┘
```

---

## Implementation Tasks

### Task 1: Create Shared Foundation Module
```kotlin
// settings.gradle.kts
include(":Modules:Shared:Foundation")
```

**Files to move**:
- `WebAvanue/util/*.kt` → `Shared/Foundation/state/`
- `AVA/core/Data/util/*.kt` → `Shared/Foundation/util/`
- `VoiceOSCore/util/NumberToWords.kt` → `Shared/Foundation/util/`

### Task 2: Update Module Dependencies
```kotlin
// VoiceOSCore/build.gradle.kts
commonMain {
    dependencies {
        api(project(":Modules:Shared:Foundation"))
    }
}

// WebAvanue/build.gradle.kts
commonMain {
    dependencies {
        api(project(":Modules:Shared:Foundation"))
    }
}
```

### Task 3: Create Type Aliases for Backward Compatibility
```kotlin
// WebAvanue/util/Deprecated.kt
@Deprecated("Use com.augmentalis.shared.state.UiState")
typealias UiState<T> = com.augmentalis.shared.state.UiState<T>
```

### Task 4: Consolidate Assets
Move filter files:
```
VoiceOSCore/assets/filters/* → Shared/Assets/filters/
```

### Task 5: Update Imports Across Codebase
```kotlin
// Before
import com.augmentalis.webavanue.util.UiState

// After
import com.augmentalis.shared.state.UiState
```

---

## Existing KMP Modules to Leverage

| Module | Purpose | Reuse Status |
|--------|---------|--------------|
| `AVUCodec` | AVU format encoding/decoding | ✓ Already shared |
| `AVID` | Element identifier generation | ✓ Already shared |
| `Localization` | Multi-language support | ✓ Already shared |
| `Rpc` | gRPC communication | ✓ Already shared |
| `WebSocket` | Real-time sync | ✓ New standalone module |
| `VoiceCursor` | Cursor control | ✓ New KMP module |

---

## Migration Strategy

### Step 1: Non-Breaking Changes First
1. Create `Shared/Foundation` module
2. Copy (don't move) utilities to shared module
3. Add shared module as dependency

### Step 2: Gradual Migration
1. Update new code to use shared imports
2. Add deprecation warnings to old locations
3. Migrate existing code file-by-file

### Step 3: Cleanup
1. Remove deprecated type aliases after migration
2. Delete original files from individual modules
3. Update documentation

---

## Estimated Impact

| Metric | Before | After | Reduction |
|--------|--------|-------|-----------|
| Duplicate utility files | ~15 | 0 | 100% |
| Duplicate theme code | ~3 modules | 1 module | 67% |
| Asset duplication | ~4 copies | 1 copy | 75% |
| Total LOC saved | - | ~2,000+ | - |

---

## Priority Order

1. **HIGH**: Shared Foundation (utilities, state management)
2. **HIGH**: Shared Theme (consistent look)
3. **MEDIUM**: Shared Resources (icons, strings)
4. **LOW**: Shared Assets (can reference from VoiceOSCore for now)

---

## Next Steps

1. Create `Modules/Shared/Foundation` directory structure
2. Move utility classes with proper package names
3. Update `settings.gradle.kts`
4. Add dependencies to consuming modules
5. Test build
6. Commit and document

---

*Created by Claude Code Assistant*
