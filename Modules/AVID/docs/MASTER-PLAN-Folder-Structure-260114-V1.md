# Master Plan: KMP Module Folder Structure Standards

**Date:** 2026-01-14 | **Version:** V1 | **Branch:** Refactor-VUID

---

## Executive Summary

Establish standardized folder structure guidelines for KMP (Kotlin Multiplatform) modules to prevent redundant nesting and ensure flat, discoverable organization. This plan updates:
1. FOLDER-REGISTRY.md with KMP module rules
2. FILE-REGISTRY.md with package naming rules
3. CLAUDE.md global instructions with enforcement rules

---

## Problem Statement

### Current Issue (AVID Module)
```
BAD - Redundant nesting:
src/commonMain/kotlin/com/augmentalis/avid/
├── AvidGenerator.kt       ← Package level
├── Platform.kt
├── TypeCode.kt
└── core/                  ← REDUNDANT subfolder
    └── AvidGenerator.kt   ← Duplicate file!
```

### Target Structure
```
GOOD - Flat structure:
src/commonMain/kotlin/com/augmentalis/avid/
├── AvidGenerator.kt       ← Single location
├── Platform.kt
├── TypeCode.kt
└── Fingerprint.kt
```

---

## KMP Module Standard Structure

### Minimal KMP Module (Single Package)
```
Modules/{ModuleName}/
├── build.gradle.kts
├── docs/
│   └── {ModuleName}-*.md
└── src/
    ├── commonMain/kotlin/com/augmentalis/{module}/
    │   ├── {MainClass}.kt
    │   └── {SupportClasses}.kt
    ├── commonTest/kotlin/com/augmentalis/{module}/
    │   └── {MainClass}Test.kt
    ├── androidMain/kotlin/com/augmentalis/{module}/
    │   └── {MainClass}Android.kt (platform-specific only)
    ├── iosMain/kotlin/com/augmentalis/{module}/
    │   └── {MainClass}Ios.kt (platform-specific only)
    └── desktopMain/kotlin/com/augmentalis/{module}/
        └── {MainClass}Desktop.kt (platform-specific only)
```

### Multi-Feature KMP Module (Feature Subfolders)
```
Modules/{ModuleName}/
├── build.gradle.kts
├── docs/
└── src/
    └── commonMain/kotlin/com/augmentalis/{module}/
        ├── {Module}.kt                    ← Main entry point
        ├── feature1/                      ← Feature subfolder (OK)
        │   ├── Feature1Manager.kt
        │   └── Feature1Config.kt
        └── feature2/
            ├── Feature2Service.kt
            └── Feature2Repository.kt
```

---

## Folder Naming Rules

### ALLOWED Subfolders (Semantic Purpose)
```
✓ /feature/      - Feature-specific code
✓ /model/        - Data models/DTOs
✓ /repository/   - Data access layer
✓ /service/      - Business logic services
✓ /handler/      - Event/action handlers
✓ /util/         - Utilities
✓ /di/           - Dependency injection
✓ /ui/           - UI components
✓ /api/          - Public API surface
```

### FORBIDDEN Subfolders (Redundant)
```
✗ /core/         - Use package root instead
✗ /impl/         - Use package root instead
✗ /internal/     - Use internal modifier instead
✗ /base/         - Use package root instead
✗ /common/       - Already in commonMain
✗ /main/         - Already in *Main
```

---

## File Naming Rules

### Kotlin Files (PascalCase)
| Type | Pattern | Example |
|------|---------|---------|
| Main Class | `{ModuleName}.kt` | `AvidGenerator.kt` |
| Data Class | `{Entity}DTO.kt` or `{Entity}.kt` | `UserDTO.kt` |
| Interface | `I{Name}.kt` or `{Name}.kt` | `IRepository.kt` |
| Enum | `{Name}.kt` | `Platform.kt` |
| Object | `{Name}.kt` | `TypeCode.kt` |
| Platform Specific | `{Name}{Platform}.kt` | `AvidGeneratorAndroid.kt` |

### Package Structure
| Level | Convention | Example |
|-------|------------|---------|
| Root | `com.augmentalis` | Always |
| Module | `com.augmentalis.{module}` | `com.augmentalis.avid` |
| Feature | `com.augmentalis.{module}.{feature}` | `com.augmentalis.avid.sync` |

---

## Files to Update

### 1. FOLDER-REGISTRY.md
Add section: "KMP Module Structure Rules"

### 2. FILE-REGISTRY.md
Add section: "Kotlin/KMP File Naming"

### 3. CLAUDE.md (Global)
Add enforcement rules for:
- No redundant /core/, /impl/, /internal/ subfolders
- Flat package structure preference
- Platform-specific file naming

---

## Implementation Tasks

### Phase 1: Update FOLDER-REGISTRY.md
- [ ] Add KMP Module Structure section
- [ ] Add ALLOWED vs FORBIDDEN subfolder rules
- [ ] Add multi-platform source set structure

### Phase 2: Update FILE-REGISTRY.md
- [ ] Add Kotlin file naming conventions
- [ ] Add package structure rules
- [ ] Add platform-specific naming rules

### Phase 3: Update CLAUDE.md (Global)
- [ ] Add "KMP MODULE RULES" section
- [ ] Add enforcement rules with CHECK before creating folders
- [ ] Add examples of correct vs incorrect structure

### Phase 4: Apply to AVID Module
- [ ] Flatten AVID module structure
- [ ] Remove redundant /core/ subfolders
- [ ] Verify build passes

---

## Enforcement Rules (for CLAUDE.md)

```markdown
## KMP MODULE RULES (MUST Follow)

### Folder Structure
- **YOU MUST:** Use flat structure at package level (no /core/, /impl/, /internal/)
- **YOU MUST:** Place main classes at package root, not in subfolders
- **ALLOWED:** Feature subfolders for logical grouping (/feature/, /model/, /service/)
- **FORBIDDEN:** Redundant subfolders (/core/, /impl/, /base/, /internal/, /common/)

### Before Creating KMP Files
1. Check if file already exists at package root
2. Never create duplicate files in subfolders
3. Use feature subfolders only for 3+ related files

### Platform-Specific Files
- Pattern: `{ClassName}{Platform}.kt`
- Example: `AvidGeneratorAndroid.kt`, `AvidGeneratorIos.kt`
- Location: Same folder structure as commonMain

### Package Names
- Root: `com.augmentalis.{module}`
- Never: `com.augmentalis.{module}.core` (redundant)
- Never: `com.augmentalis.{module}.impl` (redundant)
```

---

## Migration Checklist for AVID

| Task | Status |
|------|--------|
| Merge core/AvidGenerator.kt → AvidGenerator.kt | Pending |
| Move androidMain/core/*.kt → androidMain/ | Pending |
| Move iosMain/core/*.kt → iosMain/ | Pending |
| Move desktopMain/core/*.kt → desktopMain/ | Pending |
| Delete all core/ directories | Pending |
| Update package declarations | Pending |
| Verify build | Pending |

---

**Author:** Claude | **IDEACODE v18**
