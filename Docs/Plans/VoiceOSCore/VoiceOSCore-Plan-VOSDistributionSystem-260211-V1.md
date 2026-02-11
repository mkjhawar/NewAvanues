# Plan: VOS Distribution System — Domain Split + Registry + FTP Sync + Crowd-Sourcing

**Date**: 2026-02-11
**Module**: VoiceOSCore + Database
**Branch**: VoiceOSCore-KotlinUpdate
**Status**: Phase A In Progress

## Context

Voice commands currently ship as monolithic `.VOS` files per locale (107 commands each). Three problems:
1. **Translation quality** — AI-translated app commands need native speaker crowd-sourcing, but web gesture terms (pan, orbit, pinch) are universal
2. **No sharing** — web commands scraped from websites live only in the session/local DB; users can't share them
3. **No version control** — no way to track what VOS files exist, their versions, or prevent duplicates

User requirements:
- Split `.VOS` -> `.app.vos` (62 crowd-sourceable) + `.web.vos` (45 technical)
- Generate `.web.vos` from DOM scraping, save locally + upload to FTP
- Download shared `.web.vos` files from FTP with dedup/version control
- VOS Registry (SQLite) with full provenance metadata
- Three sync modes: manual export/import, background auto-sync, on-demand cache
- Dual crowd-sourcing: GitHub PRs + in-app phrase suggestions

## Architecture Overview

```
                     +-------------------------+
                     |   FTP Server (Remote)    |
                     |  /vos/app/{locale}/      |
                     |  /vos/web/{domain}/      |
                     |  manifest.json           |
                     +-----------+-------------+
                                 | upload / download
                     +-----------v-------------+
                     |   VosSyncManager         |
                     |  - manual export/import  |
                     |  - background auto-sync  |
                     |  - on-demand cache       |
                     +-----------+-------------+
                                 |
              +------------------+------------------+
              |                  |                  |
    +---------v----+   +---------v------+   +------v-------+
    | VosExporter   |   | VosRegistry  |   | VosImporter  |
    | scrape->.vos  |   | SQLite table |   | .vos->DB     |
    +---------------+   +--------------+   +--------------+
              |                  |                  |
              +------------------+------------------+
                                 |
              +------------------v------------------+
              |  Downloads/commands/ (local)         |
              |  {locale}.app.vos                   |
              |  {domain}.web.vos                   |
              +-------------------------------------+
```

## Implementation Phases

### Phase A: VOS File Split + Exporter + Registry (THIS SESSION)

**Scope**: Split existing VOS files, build export/import infrastructure, create registry DB table.

### Phase B: FTP Sync + Download (NEXT SESSION)

**Scope**: FTP upload/download, manifest comparison, background auto-sync, on-demand cache.

### Phase C: In-App Suggestions (FUTURE SESSION)

**Scope**: Help screen phrase suggestion UI, suggestion storage, export for crowd-sourcing.

---

## Phase A: Detailed Implementation

### A1. Split VOS Files (5 locales x 2 = 10 new files)

**Python script** splits each `{locale}.VOS` -> `{locale}.app.vos` + `{locale}.web.vos`:

```
APP domain (62 cmds, 9 prefixes): nav(8), media(7), sys(12), voice(11), app(8), acc(6), text(7), input(2), appctl(1)
WEB domain (45 cmds, 2 prefixes): browser(18), gesture(27)
```

Each split file keeps identical JSON structure with domain-filtered content. Version bumped to `"2.1"`.

**Files created**: `en-US.app.vos`, `en-US.web.vos`, `es-ES.app.vos`, `es-ES.web.vos`, `fr-FR.app.vos`, `fr-FR.web.vos`, `de-DE.app.vos`, `de-DE.web.vos`, `hi-IN.app.vos`, `hi-IN.web.vos`
**Files deleted**: `en-US.VOS`, `es-ES.VOS`, `fr-FR.VOS`, `de-DE.VOS`, `hi-IN.VOS`

### A2. Update CommandLoader for Dual-File Loading

**File**: `Modules/VoiceOSCore/src/androidMain/.../loader/CommandLoader.kt`

Changes:
1. New constants: `FILE_EXTENSION_APP = ".app.vos"`, `FILE_EXTENSION_WEB = ".web.vos"`
2. `getAvailableLocales()`: Scan `.app.vos` files, intersect with `.web.vos` -> return complete locales
3. `loadLocale(locale, isFallback)`: Open + parse both `.app.vos` and `.web.vos`, merge commands, single `insertBatch()` to DB
4. Version bump: `"2.0"` -> `"2.1"` (forces reload on upgrade)
5. **No changes to**: ArrayJsonParser, DB schema, downstream consumers

### A3. VOS Registry Table (SQLDelight)

**New file**: `Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/VosFileRegistry.sq`

```sql
CREATE TABLE vos_file_registry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Identity
    fileId TEXT NOT NULL,
    fileType TEXT NOT NULL,
    fileName TEXT NOT NULL,

    -- Content
    contentHash TEXT NOT NULL,
    commandCount INTEGER NOT NULL,
    vosVersion TEXT NOT NULL,

    -- Full Provenance
    domain TEXT,
    pageTitle TEXT,
    urlPatterns TEXT,
    uploaderDeviceId TEXT,
    userAgent TEXT,
    scrapeDurationMs INTEGER,

    -- Timestamps
    scrapedAt INTEGER NOT NULL,
    uploadedAt INTEGER,
    downloadedAt INTEGER,

    -- Status
    source TEXT NOT NULL DEFAULT 'local',
    localPath TEXT,
    isActive INTEGER NOT NULL DEFAULT 1,

    -- Versioning
    version INTEGER NOT NULL DEFAULT 1,

    UNIQUE(fileId, fileType, version)
);
```

Named queries: `insert`, `getByFileId`, `getByDomain`, `getByHash`, `getActive`, `getAllByType`, `updateUploadedAt`, `updateDownloadedAt`, `deactivateOldVersions`, `deleteById`, `exists`, `count`

### A4. VOS Registry Repository

**New files**:
- `IVosFileRegistryRepository.kt` — interface
- `SQLDelightVosFileRegistryRepository.kt` — SQLDelight implementation
- `VosFileRegistryDTO.kt` — DTO data class

**Modified**: `VoiceOSDatabaseManager.kt` — add `vosFileRegistry` lazy property

### A5. VOS Exporter (Web -> .web.vos)

**New file**: `Modules/VoiceOSCore/src/androidMain/.../vos/VosFileExporter.kt`

Key methods:
- `exportCurrentWebPage(): ExportResult` — exports scraped web commands
- `exportAppCommands(locale: String): ExportResult` — exports app commands
- `saveToLocal(fileName, content): String` — saves to Downloads/commands/
- `contentHash(content): String` — SHA-256
- `registerFile(dto)` — registers in VOS registry

**Output directory**: `Downloads/commands/`

### A6. VOS Importer (.web.vos -> DB)

**New file**: `Modules/VoiceOSCore/src/androidMain/.../vos/VosFileImporter.kt`

Key methods:
- `importFromFile(filePath: String): ImportResult`
- `importFromContent(content: String, source: String): ImportResult`
- `isDuplicate(contentHash: String): Boolean`

### A7. Add `id` Field to StaticCommand

**File**: `StaticCommandRegistry.kt` — add `val id: String = ""` to `StaticCommand`
**File**: `CommandManager.kt` — wire `id` from VoiceCommandEntity

## Files Summary

### Modified (Phase A)

| # | File | Change |
|---|------|--------|
| 1 | `CommandLoader.kt` | Dual-file extensions (.app.vos + .web.vos), updated getAvailableLocales(), loadLocale() loads both, version 2.1 |
| 2 | `StaticCommandRegistry.kt` | Add `id: String` to StaticCommand data class |
| 3 | `CommandManager.kt` | Wire `id` field in populateStaticRegistryFromDb() |
| 4 | `VoiceOSDatabaseManager.kt` | Add `vosFileRegistry` lazy property |

### Created (Phase A)

| # | File | Content |
|---|------|---------|
| 5 | `VosFileRegistry.sq` | Registry table schema + named queries |
| 6 | `VosFileRegistryDTO.kt` | DTO data class + mapping extension |
| 7 | `IVosFileRegistryRepository.kt` | Repository interface |
| 8 | `SQLDelightVosFileRegistryRepository.kt` | SQLDelight implementation |
| 9 | `VosFileExporter.kt` | Web scrape -> .web.vos export, app -> .app.vos export |
| 10 | `VosFileImporter.kt` | .vos file -> DB import with dedup |
| 11 | 10 x split VOS files | 5 .app.vos + 5 .web.vos |

### Deleted (Phase A)

| # | File |
|---|------|
| 12 | 5 x old .VOS files (en-US, es-ES, fr-FR, de-DE, hi-IN) |

## Execution Order

1. Python script: Split 5 VOS -> 10 files, verify counts (62 app + 45 web each)
2. CommandLoader.kt: Dual-file loading + version 2.1
3. Build verify: VoiceOSCore compiles
4. VosFileRegistry.sq + DTO + Repository interface + impl
5. VoiceOSDatabaseManager: Add vosFileRegistry
6. Build verify: Database module compiles
7. StaticCommand `id` field + CommandManager wiring
8. VosFileExporter.kt: Export infrastructure
9. VosFileImporter.kt: Import with dedup
10. Build verify: Full app assembleDebug
11. Delete old .VOS files
12. Documentation: Update Chapter 93, create fix doc
13. Commit + push

## Verification

1. `./gradlew :Modules:Database:compileDebugKotlinAndroid` — DB module compiles with new .sq
2. `./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid` — VoiceOSCore compiles
3. `./gradlew :apps:avanues:assembleDebug` — APK builds with 10 split VOS files
4. Python: each `.app.vos` has 62 commands, each `.web.vos` has 45 commands
5. Python: all 107 command IDs present across both files per locale
6. Python: `category_map`/`action_map`/`meta_map` correctly filtered per domain
7. Old `.VOS` files deleted, no references remain
8. VosFileExporter can serialize web commands to valid VOS JSON
9. VosFileImporter can parse and load a .web.vos file, rejecting duplicates by contentHash

## Phase B Preview (Next Session): FTP Sync

- `VosSyncManager`: orchestrates upload/download/auto-sync
- `VosFtpClient`: FTP upload/download with retry
- Server-side `manifest.json`: index of all available VOS files
- Background WorkManager job for auto-sync
- On-demand download when visiting unscraped website
- Manifest comparison for dedup (contentHash matching)

## Phase C Preview (Future): In-App Suggestions

- `PhraseSuggestionDialog`: Compose AlertDialog for phrase suggestions
- `HelpScreenHandler.onCommandLongPress()` -> triggers suggestion dialog
- `AvanuesSettingsRepository`: suggestion storage + export
- `SystemSettingsProvider`: Export/Clear buttons in Developer Settings
