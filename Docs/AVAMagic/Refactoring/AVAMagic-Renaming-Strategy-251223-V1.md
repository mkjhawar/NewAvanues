# AVAMagic - Renaming Strategy: MagicUI/IdeaMagic → AVAui, MagicCode → AVAcode

**Module:** AVAMagic
**Topic:** Complete Renaming Strategy
**Date:** 2025-12-23
**Version:** 1.0

---

## Overview

This document outlines the comprehensive renaming strategy to rebrand:
- **MagicUI** → **AVAui**
- **IdeaMagic** → **AVAui**
- **MagicCode** → **AVAcode**

---

## Scope Analysis

### Total Impact
- **100+ occurrences** across **37 files**
- **15 package declarations** to update
- **Multiple namespaces** affected

### Affected Areas

| Area | Old Name | New Name | Count |
|------|----------|----------|-------|
| Package names | `com.augmentalis.ideamagic.*` | `com.augmentalis.avaui.*` | ~15 |
| Package names | `com.augmentalis.magicui.*` | `com.augmentalis.avaui.*` | ~5 |
| Class references | `MagicUI*` | `AVAui*` | ~20 |
| Deep links | `magicui://` | `avaui://` | ~5 |
| Schemas | `https://magicui.dev/schemas/*` | `https://avaui.dev/schemas/*` | ~2 |
| Documentation | Various | Various | ~50 |

---

## Renaming Map

### Package Names

| Old Package | New Package |
|-------------|-------------|
| `com.augmentalis.ideamagic.ui` | `com.augmentalis.avaui.ui` |
| `com.augmentalis.ideamagic.components` | `com.augmentalis.avaui.components` |
| `com.augmentalis.magicui` | `com.augmentalis.avaui.runtime` |
| `com.augmentalis.ideamagic.designsystem` | `com.augmentalis.avaui.designsystem` |

### Class/Type Names

| Old Name | New Name | Type |
|----------|----------|------|
| `MagicUIRuntime` | `AVAuiRuntime` | Class |
| `MagicUIParser` | `AVAuiParser` | Class |
| `MagicUIThemeDTO` | `AVAuiThemeDTO` | Class |
| `IdeaMagic*` | `AVAui*` | Prefix |
| `MagicCode*` | `AVAcode*` | Prefix |

### URLs and Schemas

| Old | New |
|-----|-----|
| `magicui://theme?data=` | `avaui://theme?data=` |
| `https://magicui.dev/schemas/theme-v1.json` | `https://avaui.dev/schemas/theme-v1.json` |

### Test Files

| Old Name | New Name |
|----------|----------|
| `MagicButtonTest.kt` | `AVAuiButtonTest.kt` |
| `MagicCardTest.kt` | `AVAuiCardTest.kt` |
| `MagicCheckboxTest.kt` | `AVAuiCheckboxTest.kt` |
| `MagicChipTest.kt` | `AVAuiChipTest.kt` |
| `MagicDividerTest.kt` | `AVAuiDividerTest.kt` |
| `MagicImageTest.kt` | `AVAuiImageTest.kt` |
| `MagicListItemTest.kt` | `AVAuiListItemTest.kt` |
| `MagicTextFieldTest.kt` | `AVAuiTextFieldTest.kt` |
| `MagicTextTest.kt` | `AVAuiTextTest.kt` |
| `MagicColorPickerTest.kt` | `AVAuiColorPickerTest.kt` |
| `MagicIconPickerTest.kt` | `AVAuiIconPickerTest.kt` |

---

## Backwards Compatibility Strategy

### Type Aliases

Add type aliases in a deprecation file for all renamed types:

```kotlin
// File: Components/Core/src/commonMain/kotlin/com/augmentalis/avaui/compat/Deprecated.kt

package com.augmentalis.avaui.compat

import com.augmentalis.avaui.ui.thememanager.io.parsers.AVAuiParser
import com.augmentalis.avaui.runtime.AVAuiRuntime

/**
 * Backwards compatibility type aliases
 * @deprecated Use AVAui* instead of MagicUI*
 */
@Deprecated(
    message = "MagicUI has been renamed to AVAui. Use AVAuiParser instead.",
    replaceWith = ReplaceWith("AVAuiParser", "com.augmentalis.avaui.ui.thememanager.io.parsers.AVAuiParser"),
    level = DeprecationLevel.WARNING
)
typealias MagicUIParser = AVAuiParser

@Deprecated(
    message = "MagicUI has been renamed to AVAui. Use AVAuiRuntime instead.",
    replaceWith = ReplaceWith("AVAuiRuntime", "com.augmentalis.avaui.runtime.AVAuiRuntime"),
    level = DeprecationLevel.WARNING
)
typealias MagicUIRuntime = AVAuiRuntime

@Deprecated(
    message = "IdeaMagic has been renamed to AVAui.",
    level = DeprecationLevel.WARNING
)
typealias IdeaMagicTheme = com.augmentalis.avanues.avamagic.components.core.Theme

// ... (continue for all renamed types)
```

### Package Aliases (build.gradle.kts)

```kotlin
// Not directly supported, but we can add documentation
```

### Migration Guide

Create a migration guide document at `/Docs/AVAMagic-Migration-Guide-251223-V1.md`

---

## Execution Plan

### Phase 1: Preparation (30 min)
1. ✅ Create this strategy document
2. Create migration guide
3. Create backwards compatibility file template
4. Backup current state (git branch)

### Phase 2: Package Renaming (60 min)
1. Rename package declarations in all .kt files
2. Update import statements
3. Move directories to match new package structure (if needed)

### Phase 3: Class/Type Renaming (45 min)
1. Rename all class definitions
2. Rename all type references in code
3. Rename all test files
4. Update comments and KDoc

### Phase 4: Documentation & Config (30 min)
1. Update all markdown documentation
2. Update schema URLs
3. Update deep link schemes
4. Update build files
5. Update README files

### Phase 5: Backwards Compatibility (30 min)
1. Create deprecation file with type aliases
2. Add migration notes in key files
3. Update CHANGELOG

### Phase 6: Verification (30 min)
1. Run grep to verify no old references remain
2. Test compilation (Kotlin)
3. Test TypeScript compilation (Web Renderer)
4. Run tests
5. Verify documentation links

**Total Estimated Time: 3.5 hours**

---

## Automated Search & Replace Strategy

### Search Patterns (regex)

| Pattern | Replacement | Context |
|---------|-------------|---------|
| `package com\.augmentalis\.ideamagic` | `package com.augmentalis.avaui` | Package declarations |
| `package com\.augmentalis\.magicui` | `package com.augmentalis.avaui.runtime` | Package declarations |
| `import com\.augmentalis\.ideamagic` | `import com.augmentalis.avaui` | Import statements |
| `import com\.augmentalis\.magicui` | `import com.augmentalis.avaui.runtime` | Import statements |
| `MagicUI([A-Z]\w*)` | `AVAui$1` | Class/type names |
| `IdeaMagic([A-Z]\w*)` | `AVAui$1` | Class/type names |
| `MagicCode([A-Z]\w*)` | `AVAcode$1` | Class/type names |
| `magicui://` | `avaui://` | Deep links |
| `https://magicui\.dev` | `https://avaui.dev` | Schema URLs |
| `"MagicUI"` | `"AVAui"` | String literals |

### Files to Update (by category)

#### Kotlin Source Files (15 files)
- `UI/ThemeManager/src/commonMain/kotlin/com/augmentalis/ideamagic/ui/thememanager/io/ThemeIO.kt`
- `UI/ThemeManager/src/commonMain/kotlin/com/augmentalis/ideamagic/ui/thememanager/io/parsers/MagicUIParser.kt`
- `UI/ThemeManager/src/commonMain/kotlin/com/augmentalis/ideamagic/ui/thememanager/io/parsers/W3CTokenParser.kt`
- All test files in `Components/Foundation/tests/Magic*.kt`
- All test files in `Components/Core/tests/Magic*.kt`

#### TypeScript/JavaScript Files (7 files)
- `Renderers/WebRenderer/src/index.ts`
- `Renderers/WebRenderer/src/theme/ThemeConverter.ts`
- `Renderers/WebRenderer/src/types/index.ts`
- `Renderers/WebRenderer/src/components/*.tsx`
- `Renderers/WebRenderer/package.json`

#### Documentation Files (10+ files)
- `Docs/AVAMagic-Analysis-ThemeCreator-251223-V1.md`
- `.claude/CLAUDE.md`
- `Core/Responsive/README.md`
- Test HTML files

#### Configuration Files
- Build files (if any references)
- Package.json files

---

## Risk Mitigation

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Broken imports in dependent modules | High | Type aliases + gradual migration |
| External API references | Medium | Keep old URLs with redirects |
| Documentation out of sync | Low | Automated search & replace |
| Test failures | Medium | Run full test suite after changes |

### Rollback Plan

1. All changes done on feature branch: `feature/rename-magicui-to-avaui`
2. Can revert with `git reset --hard` if issues arise
3. Type aliases ensure old code still compiles with warnings

---

## Verification Checklist

After completion:

- [ ] No `MagicUI` references in .kt files (except deprecation file)
- [ ] No `IdeaMagic` references in .kt files (except deprecation file)
- [ ] No `MagicCode` references in .kt files (except deprecation file)
- [ ] All package names updated
- [ ] All imports updated
- [ ] All class names updated
- [ ] All documentation updated
- [ ] Schema URLs updated
- [ ] Deep link schemes updated
- [ ] Type aliases file created
- [ ] Migration guide created
- [ ] All tests pass
- [ ] Kotlin code compiles
- [ ] TypeScript code compiles
- [ ] No broken links in documentation

---

## Post-Rename Tasks

1. Update external documentation (if any)
2. Notify team of name change
3. Update any deployment scripts
4. Update any CI/CD pipelines
5. Create GitHub release notes
6. Update project README

---

**Status:** Ready for execution
**Approved:** Pending user confirmation
