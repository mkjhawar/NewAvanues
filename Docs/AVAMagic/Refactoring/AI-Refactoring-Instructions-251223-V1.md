# AI Instructions: Module Refactoring to NewAvanues Standards

**Purpose:** Step-by-step instructions for AI assistants to refactor modules to follow NewAvanues standards
**Reference:** See `AVAMagic-Folder-Naming-Standards-251223-V1.md` for complete standards
**Date:** 2025-12-23
**Version:** 1.0

---

## Prerequisites

Before starting, ensure you have:
1. ✅ Read `AVAMagic-Folder-Naming-Standards-251223-V1.md`
2. ✅ Identified the module to refactor (e.g., AVAMagic, VoiceOS, AVA)
3. ✅ Confirmed current git branch or created feature branch
4. ✅ User approval to proceed

---

## Refactoring Workflow

### Step 1: Analysis Phase (15-30 min)

#### 1.1 Survey Current Structure

```bash
# List all top-level directories
ls -la /path/to/module/

# Find all Kotlin source files
find /path/to/module -name "*.kt" | head -20

# Find all package declarations
grep -r "^package " /path/to/module/src --include="*.kt" | head -20

# Count occurrences of old naming
grep -r "MagicUI\|IdeaMagic\|magicui" /path/to/module --include="*.kt" --include="*.md" --include="*.ts"
```

**Document findings:**
- Total directories
- Package patterns
- Old naming occurrences
- Estimated complexity

#### 1.2 Create Mapping Document

Create: `/path/to/module/Docs/{Module}-Refactoring-Map-{YYMMDD}-V1.md`

Template:
```markdown
# {Module} Refactoring Map

## Directory Mapping
| Old Path | New Path | Reason |
|----------|----------|--------|
| UI/ThemeManager/ | theme/ | Remove redundancy |
| ... | ... | ... |

## Package Mapping
| Old Package | New Package | Files Affected |
|-------------|-------------|----------------|
| com.augmentalis.ideamagic.ui.thememanager | com.augmentalis.avaui.theme | 15 |
| ... | ... | ... |

## Class Renaming
| Old Name | New Name | Type |
|----------|----------|------|
| MagicUIParser | AVAuiParser | Class |
| ... | ... | ... |

## Estimated Effort
- Files to modify: XX
- Directories to create/move: XX
- Tests to update: XX
- Total time: X hours
```

#### 1.3 Get User Approval

Present the mapping document and ask:
- "I've analyzed the module. Here's the refactoring map: [summary]. Proceed? (Y/n)"
- Wait for explicit approval before continuing

---

### Step 2: Preparation Phase (10-15 min)

#### 2.1 Create Backup Branch (if not done)

```bash
# Create feature branch
git checkout -b refactor/{module-name}-folder-structure

# Verify branch
git branch --show-current
```

#### 2.2 Create Backwards Compatibility File

Create: `{module}/compat/src/commonMain/kotlin/com/augmentalis/{product}/compat/Deprecated.kt`

Template:
```kotlin
@file:Suppress("DEPRECATION")

package com.augmentalis.{product}.compat

/**
 * Backwards compatibility type aliases
 *
 * These aliases maintain compatibility with old code while migration happens.
 * All aliases are marked as deprecated and will be removed in version 2.0.
 */

// Import new classes
import com.augmentalis.{product}.{feature}.{NewClass}

/**
 * @deprecated {OldClass} has been renamed to {NewClass}.
 * Use {NewClass} instead.
 */
@Deprecated(
    message = "{OldClass} has been renamed to {NewClass}. Update your imports.",
    replaceWith = ReplaceWith("{NewClass}", "com.augmentalis.{product}.{feature}.{NewClass}"),
    level = DeprecationLevel.WARNING
)
typealias {OldClass} = {NewClass}

// Repeat for all renamed classes
```

#### 2.3 Create Migration Guide

Create: `{module}/Docs/{Module}-Migration-Guide-{YYMMDD}-V1.md`

Template:
```markdown
# {Module} Migration Guide

## Overview
This guide helps migrate from old structure to new standards.

## Quick Reference
| Old | New |
|-----|-----|
| `import com.augmentalis.old.package.*` | `import com.augmentalis.new.package.*` |
| `MagicUI*` | `AVAui*` |

## Step-by-Step Migration
1. Update imports: [examples]
2. Update class references: [examples]
3. Test compilation

## Breaking Changes
- None (backwards compatibility maintained via typealiases)

## Timeline
- Deprecation warnings: Now
- Removal of old names: Version 2.0
```

---

### Step 3: Directory Restructuring (30-60 min)

#### 3.1 Create New Directory Structure

```bash
# Example for AVAMagic theme system
mkdir -p Modules/AVAMagic/theme/src/commonMain/kotlin/com/augmentalis/avaui/theme/{io,manager,tokens,data}
mkdir -p Modules/AVAMagic/theme/src/commonMain/kotlin/com/augmentalis/avaui/theme/io/{parsers,exporters,encoders,handlers}
mkdir -p Modules/AVAMagic/theme/src/commonTest/kotlin/com/augmentalis/avaui/theme
mkdir -p Modules/AVAMagic/theme/src/androidMain/kotlin/com/augmentalis/avaui/theme
mkdir -p Modules/AVAMagic/theme/src/iosMain/kotlin/com/augmentalis/avaui/theme
```

**Use TodoWrite tool to track:**
```
- [ ] Create theme/ directory structure
- [ ] Create components/ directory structure
- [ ] Create renderers/ directory structure
- [ ] Create codegen/ directory structure
```

#### 3.2 Create build.gradle.kts Files

For each new feature directory, create `build.gradle.kts`:

```kotlin
// Modules/AVAMagic/theme/build.gradle.kts

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js(IR) { browser() }
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":Modules:AVAMagic:core"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avaui.theme"
    compileSdk = 34
}
```

---

### Step 4: Package Renaming (45-90 min)

#### 4.1 Automated Package Update

For EACH Kotlin file:

1. **Read the file**
2. **Update package declaration:**
   ```kotlin
   // Old
   package com.augmentalis.ideamagic.ui.thememanager.io

   // New
   package com.augmentalis.avaui.theme.io
   ```

3. **Update import statements:**
   ```kotlin
   // Old
   import com.augmentalis.ideamagic.ui.thememanager.Theme

   // New
   import com.augmentalis.avaui.theme.Theme
   ```

4. **Write updated file to NEW location**

**Script pattern:**
```bash
# For each file
OLD_PACKAGE="com.augmentalis.ideamagic.ui.thememanager"
NEW_PACKAGE="com.augmentalis.avaui.theme"
OLD_PATH="UI/ThemeManager/src/commonMain/kotlin/com/augmentalis/ideamagic/ui/thememanager"
NEW_PATH="theme/src/commonMain/kotlin/com/augmentalis/avaui/theme"

# Read, update, write
# Use Edit tool to update package and imports
```

**Use TodoWrite to track each file:**
```
- [ ] Update ThemeIO.kt (package + imports)
- [ ] Update W3CTokenParser.kt (package + imports)
- [ ] Update AVAuiParser.kt (package + imports)
- ... (one task per file)
```

#### 4.2 Update Build Files

Update `settings.gradle.kts`:
```kotlin
// Old
include(":Modules:AVAMagic:UI:ThemeManager")

// New
include(":Modules:AVAMagic:theme")
```

Update dependency declarations across modules:
```kotlin
// Old
implementation(project(":Modules:AVAMagic:UI:ThemeManager"))

// New
implementation(project(":Modules:AVAMagic:theme"))
```

---

### Step 5: Class and Type Renaming (30-60 min)

#### 5.1 Rename Class Definitions

Search and replace patterns:

| Pattern | Replacement | Context |
|---------|-------------|---------|
| `class MagicUI([A-Z]\w*)` | `class AVAui$1` | Class definitions |
| `object MagicUI([A-Z]\w*)` | `object AVAui$1` | Object definitions |
| `interface MagicUI([A-Z]\w*)` | `interface AVAui$1` | Interface definitions |
| `data class MagicUI([A-Z]\w*)` | `data class AVAui$1` | Data class definitions |
| `enum class MagicUI([A-Z]\w*)` | `enum class AVAui$1` | Enum definitions |

**For each file:**
1. Read file
2. Apply regex replacements
3. Update filename if needed (e.g., `MagicUIParser.kt` → `AVAuiParser.kt`)
4. Write file

**Example:**
```kotlin
// Old file: MagicUIParser.kt
class MagicUIParser { ... }
data class MagicUIThemeDTO { ... }

// New file: AVAuiParser.kt
class AVAuiParser { ... }
data class AVAuiThemeDTO { ... }
```

#### 5.2 Update References

Search all files for references and update:

```bash
# Find all references
grep -r "MagicUIParser" /path/to/module --include="*.kt"

# Update each reference
# Use Edit tool
```

#### 5.3 Update String Literals and Comments

Update:
- Schema URLs: `https://magicui.dev` → `https://avaui.dev`
- Deep links: `magicui://` → `avaui://`
- Documentation strings: `"MagicUI"` → `"AVAui"`
- Comments: `// MagicUI parser` → `// AVAui parser`

---

### Step 6: Documentation Updates (20-30 min)

#### 6.1 Update Markdown Files

For each `.md` file in `Docs/`:
1. Read file
2. Replace old terminology:
   - `MagicUI` → `AVAui`
   - `IdeaMagic` → `AVAui`
   - `MagicCode` → `AVAcode`
   - `com.augmentalis.ideamagic` → `com.augmentalis.avaui`
3. Update code blocks with new package names
4. Update directory paths
5. Write updated file

#### 6.2 Update README Files

Update module README.md:
```markdown
# AVAMagic - AVAui & AVAcode

AVAui: Cross-platform UI framework
AVAcode: Code generation tools

## Structure
- `theme/` - Theme system
- `components/` - UI components
- `renderers/` - Platform renderers
- `codegen/` - Code generation

## Packages
- `com.augmentalis.avaui.*` - UI framework
- `com.augmentalis.avacode.*` - Code generation
```

#### 6.3 Update .claude/CLAUDE.md

Update module-specific instructions:
```markdown
# AVAMagic - Module Instructions

Module: AVAMagic - AVAui & AVAcode

## PURPOSE
AVAui: UI framework and design system
AVAcode: Code generation tools

## PACKAGES
- `com.augmentalis.avaui.*` - All UI code
- `com.augmentalis.avacode.*` - Code generation

...
```

---

### Step 7: Test File Updates (20-30 min)

#### 7.1 Rename Test Files

```bash
# Old
Components/Foundation/tests/MagicButtonTest.kt

# New
components/foundation/src/commonTest/kotlin/com/augmentalis/avaui/components/ButtonTest.kt
```

#### 7.2 Update Test Code

For each test file:
1. Update package declaration
2. Update imports
3. Rename test class if needed
4. Update test references to renamed classes

```kotlin
// Old
package com.augmentalis.ideamagic.components.foundation

import com.augmentalis.ideamagic.components.MagicButton
import kotlin.test.Test

class MagicButtonTest {
    @Test
    fun testButton() {
        val button = MagicButton(...)
    }
}

// New
package com.augmentalis.avaui.components

import com.augmentalis.avaui.components.Button
import kotlin.test.Test

class ButtonTest {
    @Test
    fun testButton() {
        val button = Button(...)
    }
}
```

---

### Step 8: TypeScript/JavaScript Updates (if applicable) (15-30 min)

#### 8.1 Update Package Names

```json
// Old package.json
{
  "name": "@magicui/renderer",
  "version": "1.0.0"
}

// New package.json
{
  "name": "@avaui/renderer",
  "version": "2.0.0"
}
```

#### 8.2 Update Imports

```typescript
// Old
import { MagicUITheme } from '@magicui/theme';

// New
import { AVAuiTheme } from '@avaui/theme';
```

#### 8.3 Update Type Definitions

```typescript
// Old
export interface MagicUIThemeProps { ... }

// New
export interface AVAuiThemeProps { ... }
```

---

### Step 9: Backwards Compatibility (15-20 min)

#### 9.1 Complete Deprecated.kt File

Add ALL renamed types to the deprecation file:

```kotlin
package com.augmentalis.avaui.compat

import com.augmentalis.avaui.theme.io.AVAuiParser
import com.augmentalis.avaui.theme.ThemeManager

@Deprecated(
    message = "MagicUIParser has been renamed to AVAuiParser",
    replaceWith = ReplaceWith("AVAuiParser", "com.augmentalis.avaui.theme.io.AVAuiParser"),
    level = DeprecationLevel.WARNING
)
typealias MagicUIParser = AVAuiParser

@Deprecated(
    message = "IdeaMagicThemeManager has been renamed to ThemeManager",
    replaceWith = ReplaceWith("ThemeManager", "com.augmentalis.avaui.theme.ThemeManager"),
    level = DeprecationLevel.WARNING
)
typealias IdeaMagicThemeManager = ThemeManager

// ... Continue for all renamed types
```

#### 9.2 Add Compat Module to Build

```kotlin
// settings.gradle.kts
include(":Modules:AVAMagic:compat")
```

---

### Step 10: Verification (30-45 min)

#### 10.1 Verification Checklist

Run through this checklist:

- [ ] **No old package names remain** (except in compat/Deprecated.kt)
  ```bash
  grep -r "com.augmentalis.ideamagic" --include="*.kt" --exclude-dir=compat
  grep -r "com.augmentalis.magicui" --include="*.kt" --exclude-dir=compat
  ```

- [ ] **No old class names remain** (except in compat)
  ```bash
  grep -r "class MagicUI" --include="*.kt" --exclude-dir=compat
  grep -r "interface MagicUI" --include="*.kt" --exclude-dir=compat
  ```

- [ ] **All imports resolve**
  ```bash
  ./gradlew :Modules:AVAMagic:theme:dependencies
  ```

- [ ] **Build succeeds**
  ```bash
  ./gradlew :Modules:AVAMagic:build
  ```

- [ ] **Tests pass**
  ```bash
  ./gradlew :Modules:AVAMagic:test
  ```

- [ ] **Android build succeeds**
  ```bash
  ./gradlew :Modules:AVAMagic:assembleDebug
  ```

- [ ] **iOS build succeeds** (if applicable)
  ```bash
  ./gradlew :Modules:AVAMagic:linkDebugFrameworkIosArm64
  ```

- [ ] **Documentation updated**
  - No references to old names in .md files
  - README reflects new structure
  - .claude/CLAUDE.md updated

- [ ] **Type aliases work**
  - Create test file using old names
  - Verify deprecation warnings appear
  - Verify code still compiles

#### 10.2 Create Verification Report

Create: `{module}/Docs/{Module}-Refactoring-Report-{YYMMDD}-V1.md`

```markdown
# {Module} Refactoring Report

## Summary
- Files modified: XX
- Directories created: XX
- Directories removed: XX
- Classes renamed: XX
- Packages renamed: XX

## Verification Results
- [x] Build succeeds
- [x] Tests pass (XX/XX)
- [x] No old references (except compat)
- [x] Documentation updated
- [x] Backwards compatibility verified

## Breaking Changes
- None (type aliases maintain compatibility)

## Next Steps
1. Review changes
2. Merge feature branch
3. Update dependent modules (if any)
4. Monitor for issues

## Migration Timeline
- Old names deprecated: {Date}
- Old names removal: Version 2.0 (TBD)
```

---

### Step 11: Cleanup and Finalization (10-15 min)

#### 11.1 Remove Old Directories

**ONLY after verification passes:**

```bash
# List old directories to remove
ls -la Modules/AVAMagic/UI/
ls -la Modules/AVAMagic/UI/ThemeManager/

# Remove (with git)
git rm -r Modules/AVAMagic/UI/ThemeManager/

# Commit
git add .
git commit -m "refactor: restructure AVAMagic to follow NewAvanues standards

- Rename MagicUI → AVAui, MagicCode → AVAcode
- Flatten directory structure (UI/ThemeManager → theme)
- Update packages: com.augmentalis.ideamagic → com.augmentalis.avaui
- Add backwards compatibility via type aliases
- Update all documentation

See Docs/AVAMagic-Refactoring-Report-{YYMMDD}-V1.md for details"
```

#### 11.2 Update Root Documentation

If this is the first module refactored, create monorepo-level guide:

Create: `/Modules/README.md`

```markdown
# NewAvanues Modules

All modules follow standardized structure. See individual module documentation.

## Modules
- **AVA** - AI Assistant
- **VoiceOS** - Voice-first accessibility
- **AVAMagic** - AVAui (UI framework) + AVAcode (code gen) ✅ Refactored
- **WebAvanue** - Web platform
- **Cockpit** - Management dashboard
- **NLU** - Natural language understanding

## Standards
See `Modules/AVAMagic/Docs/AVAMagic-Folder-Naming-Standards-251223-V1.md`

## Refactoring Status
- [x] AVAMagic
- [ ] VoiceOS
- [ ] AVA
- [ ] WebAvanue
- [ ] Cockpit
- [ ] NLU
```

---

## Common Pitfalls and Solutions

### Issue: Build fails after package rename

**Solution:**
1. Check `settings.gradle.kts` for old module paths
2. Check all `implementation(project(...))` references
3. Verify `namespace` in android blocks matches new package

### Issue: Tests can't find classes

**Solution:**
1. Verify test source sets have correct package structure
2. Check test dependencies include the new module
3. Ensure `commonTest` directories exist

### Issue: Circular dependencies

**Solution:**
1. Extract shared code to separate `:core` module
2. Ensure dependency graph is acyclic
3. Use dependency inversion (interfaces) if needed

### Issue: IDE doesn't recognize new packages

**Solution:**
1. File → Invalidate Caches / Restart
2. Reimport Gradle project
3. Verify source sets are configured correctly

---

## Regex Patterns Reference

For automated search & replace:

```regex
# Package declarations
Find:    ^package com\.augmentalis\.ideamagic\.ui\.thememanager
Replace: package com.augmentalis.avaui.theme

# Import statements
Find:    ^import com\.augmentalis\.ideamagic\.ui\.thememanager
Replace: import com.augmentalis.avaui.theme

# Class names
Find:    (class|interface|object|data class|enum class) MagicUI(\w+)
Replace: $1 AVAui$2

# References in code
Find:    MagicUI(\w+)
Replace: AVAui$1

# Deep links
Find:    magicui://
Replace: avaui://

# Schema URLs
Find:    https://magicui\.dev
Replace: https://avaui.dev
```

---

## Success Criteria

Refactoring is complete when:

1. ✅ All builds succeed (Android, iOS, JVM, JS)
2. ✅ All tests pass
3. ✅ No old package names exist (except compat module)
4. ✅ No old class names exist (except type aliases)
5. ✅ Documentation reflects new structure
6. ✅ Backwards compatibility verified
7. ✅ Migration guide created
8. ✅ Refactoring report completed

---

## Post-Refactoring Tasks

1. **Update dependent modules** - If other modules depend on this one, update their imports
2. **Update CI/CD** - Ensure build pipelines use new module paths
3. **Announce changes** - Notify team of new structure
4. **Monitor** - Watch for issues in first week
5. **Schedule cleanup** - Plan removal of deprecated aliases for next major version

---

## Questions to Ask User

Throughout the process, ask for confirmation at these points:

1. **Before starting:** "Ready to proceed with refactoring {Module}?"
2. **After analysis:** "I've mapped {X} files to update. Estimated {Y} hours. Proceed?"
3. **After phase 3:** "Directory structure created. Proceed with file migration?"
4. **After phase 6:** "All files updated. Proceed with verification?"
5. **Before cleanup:** "Verification passed. Remove old directories?"
6. **After completion:** "Refactoring complete. Review changes?"

---

## Template Messages

### Starting Message
```
I'll refactor {Module} to follow NewAvanues standards. This includes:
- Renaming MagicUI → AVAui, MagicCode → AVAcode
- Restructuring directories ({old} → {new})
- Updating {X} packages
- Renaming {Y} classes
- Maintaining backwards compatibility via type aliases

Estimated time: {Z} hours

Ready to proceed? (Y/n)
```

### Completion Message
```
✅ {Module} refactoring complete!

Summary:
- Files modified: {X}
- Directories restructured: {Y}
- Packages renamed: {Z}
- Classes renamed: {W}
- Tests: All passing ✓
- Build: Success ✓
- Backwards compatibility: Maintained via type aliases ✓

Next steps:
1. Review changes (git diff)
2. Test in your environment
3. Merge when ready

Full report: Docs/{Module}-Refactoring-Report-{YYMMDD}-V1.md
```

---

**Version:** 1.0
**Last Updated:** 2025-12-23
