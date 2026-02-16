# VoiceOSCore-Plan-VOSCompactFormat-260216-V1

## Status: IMPLEMENTED (260216)

## Summary

Migrate VOS seed files from verbose JSON v2.1 to compact v3.0 format. Eliminates 25 KB of duplicated maps, reduces total size from ~126 KB to ~55 KB, and simplifies the parser by compiling locale-independent maps as constants.

## Problem

Current v2.1 format has three inefficiencies:

1. **Map duplication**: `category_map` (~0.5 KB) and `action_map` (~2 KB) are **identical** across all 10 files. That's 25 KB of pure redundancy — the mappings are locale-independent.
2. **Meta_map partially empty**: `meta_map` is `{}` in app.vos files but has 22 entries in web.vos files (gesture direction/scale/factor metadata). Both get duplicated across all locale variants.
3. **JSON verbosity**: Brackets, quotes, commas account for ~30% of the commands section.

### File Inventory Before (10 files, 5 locales x 2 domains)

| Locale | App | Web | Combined |
|--------|-----|-----|----------|
| en-US | 11.4 KB (62 cmds) | 12.5 KB (62 cmds) | 23.9 KB |
| es-ES | 12.7 KB | 13.3 KB | 26.0 KB |
| fr-FR | 12.3 KB | 13.2 KB | 25.5 KB |
| de-DE | 12.7 KB | 13.2 KB | 25.9 KB |
| hi-IN | 13.2 KB | 14.5 KB | 27.7 KB |
| **Total** | | | **~126 KB** |

---

## Solution: VOS v3.0 Compact Format

### Design Principles

1. **Maps compiled as constants** — `CATEGORY_MAP` (10), `ACTION_MAP` (124), and `META_MAP` (22) are baked into VosParser as Kotlin maps. No per-file duplication.
2. **Pipe-delimited commands** — Replace JSON arrays with `|`-delimited lines.
3. **Comma-delimited synonyms** — Synonyms joined by `,` within their field.
4. **UTF-8 text file** — Human-readable, git-diffable, easy to edit.
5. **Backward compatibility** — VosParser auto-detects v2.1 JSON vs v3.0 compact by first character (`{` = JSON, `#` or `VOS:` = compact).

### v3.0 File Format

```
# VOS v3.0 — en-US app commands
# Copyright (c) 2026 Manoj Jhawar, Aman Jhawar — Intelligent Devices LLC
VOS:3.0:en-US:en-US:app

nav_back|go back|navigate back,back,previous screen|Navigate to previous screen
nav_home|go home|home,navigate home,open home|Go to home screen
media_play|play music|play,resume|Play/resume media
voice_cursor_show|show cursor|cursor on,enable cursor|Show the voice cursor overlay
```

### Header Line

```
VOS:{version}:{locale}:{fallback}:{domain}
```

- `version`: `3.0`
- `locale`: BCP-47 code (e.g., `en-US`, `hi-IN`)
- `fallback`: Fallback locale (always `en-US`)
- `domain`: `app` or `web`

### Command Line

```
{action_id}|{primary_phrase}|{synonyms_csv}|{description}
```

- All 4 fields are mandatory
- Synonyms separated by `,`
- No quotes, no brackets, no trailing commas
- Pipe `|` chosen because it doesn't appear in natural language phrases

---

## Implementation Results

### Phase 1: Compiled Maps (VosParser) — DONE

**File:** `VosParser.kt` (commonMain KMP)

Three compiled companion maps:
- `CATEGORY_MAP`: 10 entries (nav→NAVIGATION, media→MEDIA, etc.)
- `ACTION_MAP`: 124 entries (61 app + 63 web action_id→CommandActionType)
- `META_MAP`: 22 entries (gesture direction/scale/factor metadata)

### Phase 2: Compact Parser (VosParser) — DONE

Added `parseCompact()` to VosParser:
- Skips comment lines (`#`) and empty lines
- Parses header: `VOS:version:locale:fallback:domain`
- Splits command lines by `|`, resolves category/action/meta from compiled maps
- Returns `VosParseResult.Success` with `List<VosParsedCommand>`

### Phase 3: Auto-Detection — DONE

`VosParser.parse()` entry point:
- `{` → `parseJson()` (v2.1 JSON)
- `#` or `VOS:` → `parseCompact()` (v3.0)
- Else → `VosParseResult.Error`

### Phase 4: Convert VOS Files — DONE

Python converter script converted all 10 files:

| File | Commands | Before | After | Savings |
|------|----------|--------|-------|---------|
| en-US.app.vos | 62 | 11,384 | 4,366 | 62% |
| en-US.web.vos | 62 | 12,527 | 4,540 | 64% |
| es-ES.app.vos | 62 | 12,686 | 5,619 | 56% |
| es-ES.web.vos | 62 | 13,293 | 5,793 | 56% |
| fr-FR.app.vos | 62 | 12,309 | 5,484 | 55% |
| fr-FR.web.vos | 62 | 13,188 | 5,710 | 57% |
| de-DE.app.vos | 62 | 12,662 | 5,683 | 55% |
| de-DE.web.vos | 62 | 13,184 | 5,717 | 57% |
| hi-IN.app.vos | 62 | 13,177 | 5,848 | 56% |
| hi-IN.web.vos | 62 | 14,537 | 6,343 | 56% |
| **TOTAL** | **620** | **128,947** | **55,103** | **57%** |

Unicode preservation verified for hi-IN (Hindi/Devanagari text).

### Phase 5: Update CommandLoader + VosFileImporter — DONE

**CommandLoader.kt:**
- Replaced `ArrayJsonParser.parseCommandsJson()` with `VosParser.parse()`
- Added `VosParsedCommand.toEntity()` mapping
- Bumped `requiredVersion` from `"2.1"` to `"3.0"` (forces DB reload on first launch)

**VosFileImporter.kt:**
- Replaced `ArrayJsonParser` with `VosParser.parse()` (auto-detects format)
- Updated `detectFileType()` and `detectFileId()` to use parsed domain from VosParseResult
- Added `VosParsedCommand.toEntity()` mapping

### Phase 6: VOS Exporter — DEFERRED

Exporter still writes v2.1 JSON (web-scraped files). Will be updated when compact export is needed for crowd-sourcing workflow.

---

## Files Modified

| # | File | Change | Commit |
|---|------|--------|--------|
| 1 | `VosParser.kt` | Compiled maps, parseCompact(), auto-detection | `322653a7` |
| 2 | 10x `.vos` files | JSON v2.1 → compact v3.0 | `322653a7` |
| 3 | `CommandLoader.kt` | VosParser, version 3.0, toEntity() | `16db3040` |
| 4 | `VosFileImporter.kt` | VosParser, simplified detect methods | `16db3040` |
| 5 | ~~`ArrayJsonParser.kt`~~ | **DELETED** — zero callers after VosParser migration | `16db3040` |
| 6 | ~~`UnifiedJSONParser.kt`~~ | **DELETED** — parsed non-existent file, zero callers | `16db3040` |
| 7 | ~~`VOSCommandIngestion.kt`~~ | **DELETED** — replaced by CommandLoader, zero callers | `16db3040` |

## Verification

- `compileDebugKotlinAndroid` BUILD SUCCESSFUL for VosParser, CommandLoader, VosFileImporter
- All 10 VOS files parse correctly (620 commands total)
- Backward compatible: v2.1 JSON files still parseable via auto-detection
- Version bump 2.1→3.0 triggers automatic DB reload on app upgrade
