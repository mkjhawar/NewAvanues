# VoiceOSCore-Plan-VOSCompactFormat-260216-V1

## Summary

Migrate VOS seed files from verbose JSON v2.1 to compact v3.0 format. Eliminates 25 KB of duplicated maps, reduces total size from ~138 KB to ~50 KB, and simplifies the parser by compiling locale-independent maps as constants.

## Problem

Current v2.1 format has three inefficiencies:

1. **Map duplication**: `category_map` (~0.5 KB) and `action_map` (~2 KB) are **identical** across all 10 files. That's 25 KB of pure redundancy — the mappings are locale-independent.
2. **Empty fields**: `meta_map` is `{}` in every file. Occupies space, adds parsing overhead.
3. **JSON verbosity**: Brackets, quotes, commas account for ~30% of the commands section.

### Current v2.1 Structure (per file)

```json
{
  "version": "2.1",
  "locale": "en-US",
  "fallback": "en-US",
  "domain": "app",
  "category_map": { "nav": "NAVIGATION", ... },    // IDENTICAL x10
  "action_map": { "nav_back": "BACK", ... },        // IDENTICAL x10
  "meta_map": {},                                     // EMPTY x10
  "commands": [
    ["nav_back", "go back", ["navigate back", "back"], "Navigate to previous screen"],
    ...
  ]
}
```

### File Inventory (10 files, 5 locales x 2 domains)

| Locale | App | Web | Combined |
|--------|-----|-----|----------|
| en-US | 11.4 KB (62 cmds) | 12.5 KB (45 cmds) | 23.9 KB |
| es-ES | 12.7 KB | 13.3 KB | 26.0 KB |
| fr-FR | 12.3 KB | 13.2 KB | 25.5 KB |
| de-DE | 12.7 KB | 13.2 KB | 25.9 KB |
| hi-IN | 13.2 KB | 14.5 KB | 27.7 KB |
| **Total** | | | **~138 KB** |

---

## Solution: VOS v3.0 Compact Format

### Design Principles

1. **Maps compiled as constants** — `category_map` and `action_map` are baked into VosParser as Kotlin maps. No per-file duplication.
2. **Pipe-delimited commands** — Replace JSON arrays with `|`-delimited lines.
3. **Comma-delimited synonyms** — Synonyms joined by `,` within their field.
4. **Optional metadata** — 5th field only present when metadata exists.
5. **UTF-8 text file** — Human-readable, git-diffable, easy to edit.
6. **Backward compatibility** — VosParser auto-detects v2.1 JSON vs v3.0 compact by first character (`{` = JSON, `#` or `VOS:` = compact).

### v3.0 File Format

```
# VOS v3.0 — en-US app commands
# Copyright (c) 2026 Manoj Jhawar, Aman Jhawar — Intelligent Devices LLC
VOS:3.0:en-US:en-US:app

# Format: action_id|primary_phrase|synonym1,synonym2,...|description
# Lines starting with # are comments. Empty lines ignored.

nav_back|go back|navigate back,back,previous screen|Navigate to previous screen
nav_home|go home|home screen,main screen,home|Return to home screen
nav_recent|recent apps|recent,app switcher,multitask|Open recent apps
media_play|play|play music,resume,unpause|Play or resume media
media_pause|pause|pause music,stop playing|Pause current media
voice_cursor_show|show cursor|cursor on,display cursor|Show voice cursor overlay
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
{action_id}|{primary_phrase}|{synonyms_csv}|{description}[|{metadata_json}]
```

- Fields 1-4 are mandatory
- Field 5 (metadata) is optional, only present when non-empty
- Synonyms separated by `,`
- No quotes, no brackets, no trailing commas
- Pipe `|` chosen because it doesn't appear in natural language phrases

### Size Estimates

| Component | v2.1 (per file) | v3.0 (per file) | Savings |
|-----------|-----------------|-----------------|---------|
| Maps (category + action) | ~2.5 KB | 0 KB (compiled) | 100% |
| meta_map | ~15 bytes | 0 bytes | 100% |
| JSON wrapper | ~100 bytes | ~80 bytes (header) | 20% |
| Per command | ~120 bytes avg | ~75 bytes avg | 37% |
| **Total per file** | ~13 KB avg | ~5.5 KB avg | ~58% |
| **Total (10 files)** | ~138 KB | ~55 KB | ~60% |

---

## Implementation Plan

### Phase 1: Compile Maps as Constants (VosParser)

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/loader/VosParser.kt`

Add two companion object maps:

```kotlin
companion object {
    /** Locale-independent category map. Shared across all VOS files. */
    val CATEGORY_MAP: Map<String, String> = mapOf(
        "nav" to "NAVIGATION",
        "media" to "MEDIA",
        "sys" to "SYSTEM",
        "voice" to "VOICE_CONTROL",
        "acc" to "ACCESSIBILITY",
        "text" to "TEXT",
        "input" to "INPUT",
        "appctl" to "APP_CONTROL",
        "browser" to "BROWSER",
        "gesture" to "WEB_GESTURE"
    )

    /** Locale-independent action map. Maps action_id prefix to CommandActionType. */
    val ACTION_MAP: Map<String, String> = mapOf(
        "nav_back" to "BACK",
        "nav_home" to "HOME",
        "nav_recent" to "RECENT_APPS",
        // ... all 61+ entries from current action_map
    )
}
```

### Phase 2: Add v3.0 Parser to VosParser

Add `parseCompact(text: String, isFallback: Boolean): VosParseResult` method:

1. Skip comment lines (`#`) and empty lines
2. Parse header line: `VOS:version:locale:fallback:domain`
3. For each command line, split by `|`:
   - `parts[0]` = action_id
   - `parts[1]` = primary phrase
   - `parts[2]` = synonyms (split by `,`, trim each)
   - `parts[3]` = description
   - `parts[4]` = metadata JSON (optional)
4. Resolve category from action_id prefix via `CATEGORY_MAP`
5. Resolve action type via `ACTION_MAP`
6. Return `VosParseResult.Success`

### Phase 3: Auto-Detection in VosParser

Modify `parse()` entry point:

```kotlin
fun parse(content: String, isFallback: Boolean = false): VosParseResult {
    val trimmed = content.trimStart()
    return when {
        trimmed.startsWith("{") -> parseJson(trimmed, isFallback)   // v2.1
        trimmed.startsWith("#") || trimmed.startsWith("VOS:") -> parseCompact(trimmed, isFallback)  // v3.0
        else -> VosParseResult.Error("Unknown VOS format")
    }
}
```

### Phase 4: Convert VOS Files

Script to convert all 10 files from v2.1 JSON to v3.0 compact:

1. Read each `.vos` file
2. Parse with existing JSON parser
3. Write compact format with header + commands
4. Verify round-trip: parse compact → compare command count and content

### Phase 5: Update CommandLoader

**File:** `Modules/VoiceOSCore/src/androidMain/kotlin/.../loader/CommandLoader.kt`

- Update `CURRENT_VOS_VERSION` from `"2.1"` to `"3.0"`
- Use `VosParser.parse()` (auto-detecting) instead of `ArrayJsonParser`
- Remove `ArrayJsonParser` dependency (already `@Deprecated`)

### Phase 6: Update VOS Exporter

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../distribution/VosExporter.kt` (if exists)

- Add compact format export option
- Default to v3.0 for new exports
- Keep v2.1 JSON export as compatibility option

---

## Migration Strategy

### Backward Compatibility

- VosParser auto-detects format by first character
- Old v2.1 JSON files continue to work indefinitely
- No forced migration — new files use v3.0, old files parsed as v2.1
- Version bump to "3.0" in `CommandLoader.CURRENT_VOS_VERSION` triggers DB reload

### Rollback

- Keep v2.1 files in git history
- VosParser handles both formats — revert file format without code changes

---

## Files Modified

| # | File | Change |
|---|------|--------|
| 1 | `VosParser.kt` | Add compiled maps, parseCompact(), auto-detection |
| 2 | `CommandLoader.kt` | Use VosParser, bump version to 3.0 |
| 3 | `ArrayJsonParser.kt` | No change (already deprecated, kept for reference) |
| 4 | 10x `.vos` files | Convert from JSON v2.1 to compact v3.0 |

## Dependencies

- `kotlinx.serialization.json` — already in VosParser for v2.1 fallback
- No new module dependencies

## Verification

1. `./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid`
2. Round-trip test: parse compact → count commands → compare to v2.1 counts (62 app + 45 web per locale)
3. Device test: force reload commands, verify all 107 commands per locale available
4. Check synonym preservation: each command's synonym list matches v2.1 original

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| Pipe in synonym text | Audit all synonyms — no pipes in natural language phrases |
| Comma in synonym | Individual synonyms never contain commas (verified across 5 locales) |
| Encoding issues (hi-IN Devanagari) | UTF-8 throughout, no encoding change from v2.1 |
| SFTP sync compatibility | VosFileRegistry tracks version; compact files get new contentHash |
