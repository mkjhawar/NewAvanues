# Implementation Plan: Migrate to AVU/IDC Compact Format

**Date:** 2026-01-15 | **Version:** V1
**Author:** Claude | **Module:** AvaMagic

---

## Overview

**Objective:** Migrate AvaMagic theme/config loaders from standard YAML/JSON to compact AVU/IDC format.

**Platforms:** KMP (Android, iOS, Desktop, Web)
**Swarm Recommended:** No (single module, <15 tasks)
**Estimated:** 12 tasks

---

## Chain of Thought Reasoning

### Why Compact Format?

1. **Token Efficiency:** AVU/IDC format is ~60-70% more compact than YAML/JSON
2. **Parsing Speed:** Line-based format is faster to parse (no nested structure traversal)
3. **Consistency:** Aligns with VoiceOS `.vos` and IDEACODE `.idc` formats
4. **AI-Friendly:** Compact format uses fewer tokens for LLM processing

### Format Design for Themes

**Current YAML (verbose - 287 chars):**
```yaml
name: "Dark Theme"
palette:
  primary: "#007AFF"
  secondary: "#5AC8FA"
  background: "#000000"
typography:
  h1:
    size: 28
    weight: bold
```

**Proposed IDC (compact - 142 chars, 51% reduction):**
```
# AvaUI Theme Format v1.0
# Type: THM
---
schema: thm-1.0
---
THM:Dark Theme:1.0.0
PAL:primary:#007AFF
PAL:secondary:#5AC8FA
PAL:background:#000000
TYP:h1:28:bold:system
TYP:body:16:regular:system
SPC:xs:4:sm:8:md:16:lg:24:xl:32
EFX:shadow:true:blur:8:elevation:4
```

### Record Type Prefixes

| Prefix | Purpose | Format |
|--------|---------|--------|
| `THM:` | Theme metadata | `THM:name:version` |
| `PAL:` | Palette color | `PAL:key:#hexcolor` |
| `TYP:` | Typography | `TYP:style:size:weight:family` |
| `SPC:` | Spacing | `SPC:xs:val:sm:val:md:val:lg:val:xl:val` |
| `EFX:` | Effects | `EFX:type:value:type:value...` |

---

## Files to Migrate

| Current File | Action | New File |
|--------------|--------|----------|
| `YamlThemeLoader.kt` | Replace | `IdcThemeLoader.kt` |
| `JsonThemeLoader.kt` | Deprecate | Keep for compatibility |
| `LayoutLoader.kt` | Extend | Add AVU format support |
| - | Create | `IdcThemeSerializer.kt` |
| - | Create | `AvuLayoutLoader.kt` |

---

## Phases

### Phase 1: Core IDC Theme Format (4 tasks)

1. **Create IDC format parser** (`IdcThemeParser.kt`)
   - Parse line-based format
   - Handle record type prefixes
   - Validate schema version

2. **Create IDC theme loader** (`IdcThemeLoader.kt`)
   - Load `.thm` files
   - Convert to `ThemeConfig` model
   - Support defaults for missing fields

3. **Create IDC theme serializer** (`IdcThemeSerializer.kt`)
   - Serialize `ThemeConfig` to IDC format
   - Generate `.thm` file output
   - Include header with schema version

4. **Update theme loader interface**
   - Add format detection for `.thm` files
   - Deprecate YAML loader (keep JSON for compatibility)

### Phase 2: AVU Layout Format (4 tasks)

5. **Design AVU layout format**
   - Define component type prefixes: `COL:`, `ROW:`, `TXT:`, `BTN:`, etc.
   - Design property encoding
   - Document format specification

6. **Create AVU layout parser** (`AvuLayoutParser.kt`)
   - Parse component definitions
   - Handle nesting via indentation or parent reference

7. **Create AVU layout loader** (`AvuLayoutLoader.kt`)
   - Implement `LayoutLoader` interface
   - Load `.avu` layout files
   - Convert to `ComponentModel` list

8. **Update layout loader interface**
   - Add `LayoutFormat.AVU` enum value
   - Update auto-detection for AVU format

### Phase 3: Integration & Migration (4 tasks)

9. **Update PluginLoader for new formats**
   - Support `.thm` theme files in plugins
   - Support `.avu` layout files in plugins

10. **Create migration utility**
    - Convert existing YAML themes to IDC
    - Convert existing JSON themes to IDC
    - Batch migration script

11. **Update documentation**
    - Format specification docs
    - Migration guide
    - Examples

12. **Add tests**
    - Unit tests for parsers
    - Round-trip serialization tests
    - Edge case handling

---

## Task List (TodoWrite Format)

```
1. [pending] Create IdcThemeParser for line-based format parsing
2. [pending] Create IdcThemeLoader to load .thm files
3. [pending] Create IdcThemeSerializer to save themes as IDC
4. [pending] Update ThemeLoader interface for format detection
5. [pending] Design AVU layout format specification
6. [pending] Create AvuLayoutParser for component parsing
7. [pending] Create AvuLayoutLoader implementing LayoutLoader
8. [pending] Add LayoutFormat.AVU to format enum
9. [pending] Update PluginLoader for .thm and .avu support
10. [pending] Create YAML/JSON to IDC migration utility
11. [pending] Update format documentation
12. [pending] Add unit tests for new parsers
```

---

## Time Estimates

| Mode | Time |
|------|------|
| Sequential | 8-10 hours |
| Parallel (not recommended) | N/A |

---

## Dependencies

- `kotlinx.serialization` (existing)
- No new external dependencies required

---

## Success Criteria

- [ ] All theme files can be loaded from IDC format
- [ ] Theme serialization produces valid IDC output
- [ ] Layout files can be loaded from AVU format
- [ ] Round-trip (load → save → load) produces identical results
- [ ] 50%+ reduction in file size vs YAML
- [ ] All existing tests pass
- [ ] Migration utility successfully converts sample files
