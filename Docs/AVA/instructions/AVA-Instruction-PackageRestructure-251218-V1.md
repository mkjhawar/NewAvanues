# AVA Package Structure Cleanup - AI Instructions

## Document Metadata
- **Type**: AI Implementation Instructions
- **Created**: 2025-12-18
- **Version**: 1.0
- **Scope**: Modules/AVA/* package path restructuring
- **Estimated Files**: 100-200 files across 8 modules

---

## 1. Problem Statement

The AVA Android app modules have redundant and deeply nested package structures that violate DRY principles and create unnecessarily long import paths.

### Current Pattern (Problematic)
```
Module Path:     Modules/AVA/Chat/
Source Path:     src/main/kotlin/
Package Path:    com/augmentalis/ava/features/chat/ui/state/
Full Path:       Modules/AVA/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/ui/state/

Import Example:  import com.augmentalis.ava.features.chat.ui.state.StatusIndicatorState
```

### Issues Identified
1. **"features" is redundant** - Modules/ folder already organizes by feature
2. **Module name repeated** - "Chat" appears in module path AND package path
3. **Deep nesting** - ui/state/components creates 4+ levels
4. **Long imports** - 7+ package segments for simple classes

---

## 2. Target State

### New Pattern (Simplified)
```
Module Path:     Modules/AVA/Chat/
Source Path:     src/main/kotlin/
Package Path:    com/augmentalis/chat/state/
Full Path:       Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/state/

Import Example:  import com.augmentalis.chat.state.StatusIndicatorState
```

### Naming Convention Rules

| Module | Current Package Root | New Package Root |
|--------|---------------------|------------------|
| Chat | `com.augmentalis.ava.features.chat` | `com.augmentalis.chat` |
| RAG | `com.augmentalis.ava.features.rag` | `com.augmentalis.rag` |
| LLM | `com.augmentalis.ava.features.llm` | `com.augmentalis.llm` |
| Teach | `com.augmentalis.ava.features.teach` | `com.augmentalis.teach` |
| Actions | `com.augmentalis.ava.features.actions` | `com.augmentalis.actions` |
| WakeWord | `com.augmentalis.ava.features.wakeword` | `com.augmentalis.wakeword` |
| memory | `com.augmentalis.ava.features.memory` | `com.augmentalis.memory` |
| core/Data | `com.augmentalis.ava.core.data` | `com.augmentalis.ava.core.data` (UNCHANGED) |
| core/Domain | `com.augmentalis.ava.core.domain` | `com.augmentalis.ava.core.domain` (UNCHANGED) |
| core/Theme | `com.augmentalis.ava.core.theme` | `com.augmentalis.ava.core.theme` (UNCHANGED) |

### Sub-package Flattening Rules

| Current | New | Rationale |
|---------|-----|-----------|
| `chat.ui.state` | `chat.state` | "ui" implied, state is the concern |
| `chat.ui.components` | `chat.components` | Direct child |
| `chat.ui.dialogs` | `chat.dialogs` | Direct child |
| `chat.coordinator` | `chat.coordinator` | Keep as-is (already flat) |
| `rag.data.handlers` | `rag.handlers` | Remove "data" redundancy |
| `rag.data.clustering` | `rag.clustering` | Remove "data" redundancy |
| `nlu.embeddings` | `nlu.embeddings` | Keep as-is |
| `nlu.inference` | `nlu.inference` | Keep as-is |

---

## 3. Modules In Scope

### Priority 1: Feature Modules (Change Required)
```
Modules/AVA/Chat/           -> com.augmentalis.chat.*
Modules/AVA/RAG/            -> com.augmentalis.rag.*
Modules/AVA/LLM/            -> com.augmentalis.llm.*
Modules/AVA/Teach/          -> com.augmentalis.teach.*
Modules/AVA/Actions/        -> com.augmentalis.actions.*
Modules/AVA/WakeWord/       -> com.augmentalis.wakeword.*
Modules/AVA/memory/         -> com.augmentalis.memory.*
```

### Priority 2: Core Modules (NO CHANGE)
```
Modules/AVA/core/Data/      -> com.augmentalis.ava.core.data.* (KEEP)
Modules/AVA/core/Domain/    -> com.augmentalis.ava.core.domain.* (KEEP)
Modules/AVA/core/Theme/     -> com.augmentalis.ava.core.theme.* (KEEP)
```

### Priority 3: Shared Modules (Evaluate)
```
Modules/Shared/NLU/         -> com.augmentalis.nlu.* (currently com.augmentalis.ava.features.nlu)
Modules/Shared/Platform/    -> com.augmentalis.platform.*
```

---

## 4. Step-by-Step Implementation

### Phase 1: Analysis & Planning (Read-Only)

```
STEP 1.1: Generate file inventory
- Run: find Modules/AVA -name "*.kt" -type f | wc -l
- Expected: 150-250 files

STEP 1.2: Map current package structure
- For each module, list unique package paths
- Command per module:
  grep -rh "^package " Modules/AVA/Chat/src --include="*.kt" | sort -u

STEP 1.3: Identify cross-module imports
- Find all import statements referencing com.augmentalis.ava.features.*
- Command:
  grep -rh "^import com.augmentalis.ava.features" Modules/AVA --include="*.kt" | sort | uniq -c | sort -rn

STEP 1.4: Create migration mapping file
- Output: JSON mapping of old -> new package names
- Store at: Docs/AVA/migrations/package-mapping.json
```

### Phase 2: Directory Structure Changes

```
STEP 2.1: Create new directory structure (DO NOT DELETE OLD YET)
For each module:
  1. Create new package directories under src/main/kotlin/com/augmentalis/{module}/
  2. Mirror the flattened structure from Section 2

STEP 2.2: Example for Chat module
OLD: src/main/kotlin/com/augmentalis/ava/features/chat/ui/state/
NEW: src/main/kotlin/com/augmentalis/chat/state/

Commands:
mkdir -p Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/state
mkdir -p Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/components
mkdir -p Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/coordinator
mkdir -p Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/dialogs
mkdir -p Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/tts
```

### Phase 3: File Migration (Per Module)

```
STEP 3.1: Process one module at a time (start with smallest)
Order: memory -> WakeWord -> Actions -> Teach -> LLM -> RAG -> Chat

STEP 3.2: For each Kotlin file:
  a. Read file content
  b. Update package declaration (first line after comments/imports)
     OLD: package com.augmentalis.ava.features.chat.ui.state
     NEW: package com.augmentalis.chat.state

  c. Update internal imports within same module
     OLD: import com.augmentalis.ava.features.chat.ui.components.MessageBubble
     NEW: import com.augmentalis.chat.components.MessageBubble

  d. Move file to new location
  e. Delete old file

STEP 3.3: Update imports in OTHER modules that reference this module
  - Search all Modules/AVA for imports of the moved package
  - Update those import statements
```

### Phase 4: Build Configuration Updates

```
STEP 4.1: Update build.gradle.kts namespace declarations
For each module's build.gradle.kts:

OLD:
android {
    namespace = "com.augmentalis.ava.features.chat"
}

NEW:
android {
    namespace = "com.augmentalis.chat"
}

STEP 4.2: Update Hilt module annotations if present
- Search for @Module and @InstallIn annotations
- Package paths in component definitions may need updating

STEP 4.3: Update AndroidManifest.xml if it references old packages
- Check for explicit activity/service declarations
```

### Phase 5: App Module Updates

```
STEP 5.1: Update android/apps/ava/app imports
- This module imports from ALL feature modules
- Expected: 50-100 import statement changes
- Files to check:
  - MainActivity.kt
  - Navigation files
  - DI modules (Hilt)
  - ViewModels that aggregate features

STEP 5.2: Update settings.gradle.kts if module paths change
- Module include statements should remain unchanged
- Only internal package paths change
```

### Phase 6: Test Updates

```
STEP 6.1: Update test file packages
- Test files mirror main source structure
- Path: src/test/kotlin/com/augmentalis/{module}/*
- Path: src/androidTest/kotlin/com/augmentalis/{module}/*

STEP 6.2: Update test imports
- Tests import from main sources
- All import statements need updating
```

### Phase 7: Validation

```
STEP 7.1: Compile check (NO TESTS)
./gradlew compileDebugKotlin --no-daemon 2>&1 | grep -E "error:|Error:"
- Expected: 0 errors

STEP 7.2: Full build
./gradlew :app:assembleDebug --no-daemon
- Expected: BUILD SUCCESSFUL

STEP 7.3: Run unit tests
./gradlew testDebugUnitTest --no-daemon
- Compare pass/fail count to baseline

STEP 7.4: Verify no orphaned files
find Modules/AVA -path "*/com/augmentalis/ava/features/*" -name "*.kt"
- Expected: 0 results (all migrated)
```

### Phase 8: Cleanup

```
STEP 8.1: Remove empty old directories
find Modules/AVA -type d -empty -delete

STEP 8.2: Verify git status
git status --short | wc -l
- Should show renamed files (R) not deleted+added (D, A)

STEP 8.3: Create atomic commit
git add -A
git commit -m "refactor(packages): simplify AVA module package structure

- Remove redundant 'features' package segment
- Remove redundant 'ava' from feature module packages
- Flatten ui/data sub-packages where appropriate
- No functional changes, imports only

Modules affected: Chat, RAG, LLM, Teach, Actions, WakeWord, memory"
```

---

## 5. Package Mapping Reference

### Complete Old -> New Mapping

```json
{
  "mappings": [
    {
      "old": "com.augmentalis.ava.features.chat",
      "new": "com.augmentalis.chat",
      "subpackages": {
        "ui.state": "state",
        "ui.components": "components",
        "ui.dialogs": "dialogs",
        "ui.settings": "settings",
        "ui": "",
        "coordinator": "coordinator",
        "tts": "tts",
        "domain": "domain"
      }
    },
    {
      "old": "com.augmentalis.ava.features.rag",
      "new": "com.augmentalis.rag",
      "subpackages": {
        "data.handlers": "handlers",
        "data.clustering": "clustering",
        "data": "data",
        "domain": "domain",
        "embeddings": "embeddings",
        "parser": "parser",
        "search": "search",
        "cache": "cache",
        "ui": "ui"
      }
    },
    {
      "old": "com.augmentalis.ava.features.llm",
      "new": "com.augmentalis.llm",
      "subpackages": {
        "api": "api",
        "domain": "domain"
      }
    },
    {
      "old": "com.augmentalis.ava.features.teach",
      "new": "com.augmentalis.teach",
      "subpackages": {}
    },
    {
      "old": "com.augmentalis.ava.features.actions",
      "new": "com.augmentalis.actions",
      "subpackages": {
        "executor": "executor",
        "registry": "registry"
      }
    },
    {
      "old": "com.augmentalis.ava.features.wakeword",
      "new": "com.augmentalis.wakeword",
      "subpackages": {}
    },
    {
      "old": "com.augmentalis.ava.features.memory",
      "new": "com.augmentalis.memory",
      "subpackages": {}
    },
    {
      "old": "com.augmentalis.ava.features.nlu",
      "new": "com.augmentalis.nlu",
      "subpackages": {
        "embeddings": "embeddings",
        "inference": "inference",
        "locale": "locale"
      }
    }
  ],
  "excluded": [
    "com.augmentalis.ava.core.data",
    "com.augmentalis.ava.core.domain",
    "com.augmentalis.ava.core.theme",
    "com.augmentalis.ava.core.common"
  ]
}
```

---

## 6. Regex Patterns for Bulk Updates

### Pattern 1: Package Declaration
```regex
Find:    ^package com\.augmentalis\.ava\.features\.(\w+)\.?(.*)$
Replace: package com.augmentalis.$1.$2
```

### Pattern 2: Import Statements
```regex
Find:    ^import com\.augmentalis\.ava\.features\.(\w+)\.(.*)$
Replace: import com.augmentalis.$1.$2
```

### Pattern 3: Flatten ui.* subpackages
```regex
Find:    \.ui\.(state|components|dialogs|settings)
Replace: .$1
```

### Pattern 4: Flatten data.* subpackages (RAG only)
```regex
Find:    \.data\.(handlers|clustering)
Replace: .$1
```

---

## 7. Risk Mitigation

### High Risk Areas
1. **Hilt DI modules** - Component paths are compile-time resolved
2. **Navigation routes** - If package names are in route strings
3. **Reflection usage** - Any Class.forName() calls
4. **ProGuard/R8 rules** - Keep rules reference packages

### Mitigation Steps
```
BEFORE STARTING:
1. Create git branch: feature/package-restructure
2. Run full test suite, record baseline
3. Export APK, record size
4. Document any reflection usage

IF BUILD FAILS:
1. Check error message for specific class
2. Grep for old package name in ALL files (including .xml, .gradle)
3. Common miss: AndroidManifest.xml, proguard-rules.pro

ROLLBACK PLAN:
git checkout main -- Modules/
git checkout main -- android/apps/ava/
```

---

## 8. Success Criteria

| Metric | Before | After | Validation |
|--------|--------|-------|------------|
| Build | SUCCESS | SUCCESS | `./gradlew assembleDebug` |
| Unit Tests | X passed | X passed | Same count |
| Import depth | 7 segments | 4 segments | Grep average |
| Package with "features" | ~200 | 0 | `grep -r "features" --include="*.kt"` |
| APK size | X MB | X MB | Within 1% |

---

## 9. Execution Notes for AI

### DO
- Process one module completely before moving to next
- Verify build after each module migration
- Keep detailed log of changes made
- Preserve all comments and formatting in files
- Use atomic commits per module

### DO NOT
- Change any logic or implementation code
- Modify core/* packages
- Delete files before confirming new location works
- Make changes to files outside Modules/AVA and android/apps/ava
- Combine multiple modules in one commit

### STOP CONDITIONS
- Build fails after module migration -> Fix before continuing
- More than 10 compile errors -> Review mapping, may have edge case
- Test count decreases -> Tests may have package-path dependencies

---

## 10. Estimated Effort

| Phase | Files | Estimated Changes |
|-------|-------|-------------------|
| Analysis | 0 | Read-only |
| Chat module | ~40 | ~400 import lines |
| RAG module | ~35 | ~350 import lines |
| LLM module | ~15 | ~150 import lines |
| Other modules | ~30 | ~200 import lines |
| App module | ~25 | ~250 import lines |
| Tests | ~20 | ~200 import lines |
| **Total** | **~165** | **~1550 changes** |

---

## Appendix A: File Inventory Command

```bash
# Generate complete file list with current packages
find Modules/AVA -name "*.kt" -type f -exec grep -l "^package" {} \; | while read f; do
  pkg=$(grep "^package" "$f" | head -1 | sed 's/package //')
  echo "$f|$pkg"
done > /tmp/ava-package-inventory.csv
```

## Appendix B: Verification Script

```bash
#!/bin/bash
# verify-package-migration.sh

echo "=== Checking for old package references ==="
OLD_REFS=$(grep -r "com.augmentalis.ava.features" Modules/AVA --include="*.kt" | wc -l)
echo "Old package references remaining: $OLD_REFS"

echo "=== Checking for empty directories ==="
EMPTY=$(find Modules/AVA -type d -empty | wc -l)
echo "Empty directories: $EMPTY"

echo "=== Build verification ==="
./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -5

echo "=== Package depth analysis ==="
grep -rh "^package" Modules/AVA --include="*.kt" | \
  awk -F'.' '{print NF}' | sort | uniq -c | sort -rn
```

---

**End of Instructions**
