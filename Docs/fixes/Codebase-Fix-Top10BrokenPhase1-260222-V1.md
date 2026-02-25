# Codebase Fix — Top 10 Broken Problems Phase 1
**Date:** 260222 | **Branch:** VoiceOS-1M-SpeechEngine | **Author:** Manoj Jhawar
**Mode:** YOLO + SWARM (5 parallel agents + sequential one-liners)
**Source:** 20-agent deep review (435+ findings)

---

## Summary

Phase 1 implementation of the Top 10 Codebase Problems fix plan. Executed in two waves:
- **Wave 1:** 9 one-line fixes applied sequentially (5 minutes)
- **Wave 2:** 5 SWARM agents dispatched in parallel for module-grouped medium-effort fixes

**Total changes: 369 files, 177 insertions, 470 deletions**

---

## Wave 1: One-Line Fixes (9/9 complete)

| # | Fix | File | Change | Problem # |
|---|-----|------|--------|-----------|
| 1 | VoiceKeyboard self-assignment | `VoiceKeyboardService.kt:140` | `currentInputConnection = currentInputConnection()` — added missing parentheses to call the superclass method | 4 |
| 2 | BootReceiver exported | `AndroidManifest.xml:184` | `exported="false"` → `exported="true"` — system broadcasts require exported receivers | P1-P0 |
| 3 | Debug mode default | `DeveloperPreferences.kt:95` | `?: true` → `?: false` — debug mode off by default in production | P1-P0 |
| 4 | SFTP host key mode | `AvanuesSettingsRepository.kt:125` | `?: "no"` → `?: "strict"` — enables SSH host key verification (prevents MITM) | P1-P1 |
| 5 | ReadingHandler data race | `ReadingHandler.kt:35` | Added `@Volatile` to `ttsReady` — fixes JVM visibility across TTS callback thread | P2 |
| 6 | lastScreenHash data race | `VoiceOSAccessibilityService.kt:56` | Added `@Volatile` to `lastScreenHash` — fixes visibility on Dispatchers.Default | P2 |
| 7 | HTTP/2 enablePush | `Http2Settings.kt:8` | `enablePush = true` → `enablePush = false` — RFC 7540 compliance (server never pushes) | P2 |
| 8 | ImageViewer debug artifact | `ImageViewer.kt:174` | Removed `|| true` from nav bar visibility condition | P2 |
| 9 | CommandGenerator hot-path I/O | `CommandGenerator.kt:407,411` | Removed 2 `println()` calls in list label generation loop | P2 |

---

## Wave 2: SWARM Agent Fixes (5 agents)

### Agent S1: apps/avanues P0 Fixes
**Files:** AndroidManifest.xml, VoiceAvanueApplication.kt, accessibility_service_config.xml
**Changes:**
- Added `<activity-alias>` for VoiceAvanue and WebAvanue dual launcher icons
- Configured `HiltWorkerFactory` in Application class (implements `Configuration.Provider`)
- Fixed `android:settingsActivity` to correct package path
- Removed duplicate permission declarations (WRITE_SETTINGS, BLUETOOTH, BLUETOOTH_ADMIN, CHANGE_WIFI_STATE)

### Agent S2: VoiceOSCore Pipeline Fixes
**Files:** VoiceOSAccessibilityService.kt, CockpitCommandHandler.kt, ImageCommandHandler.kt
**Changes:**
- Added `try/finally { root.recycle() }` in `handleScreenChange()` — fixes ANI memory leak
- Added `try/finally { root.recycle() }` in `refreshScreen()` — same fix
- Added `try/finally { event.recycle() }` in `refreshScreen()` — fixes AccessibilityEvent pool leak
- All recycle calls guarded by `Build.VERSION.SDK_INT < UPSIDE_DOWN_CAKE` (API 34+ auto-recycles)
- Prefixed Cockpit commands: "scroll up" → "frame scroll up", "zoom in" → "frame zoom in" (4 changes)
- Prefixed Image commands: "rotate left" → "image rotate left" (2 changes)

### Agent S3: WebAvanue Critical Fixes
**Files:** BrowserVoiceOSCallback.kt, WebAvanueVoiceOSBridge.kt
**Changes:**
- Fixed `selectDisambiguationOption()` to index the filtered matches list instead of getAllCommands()
- Added `lastDisambiguationMatches` field to store the filtered list between processSpokenPhrase and selection
- Added nonce validation to `sendScrapeResult()` — generates UUID nonce at bridge attach, validates on receive

### Agent S4: SpeechRecognition Fixes
**Files:** GoogleCloudStreamingClient.kt, WhisperModelDownloadScreen.kt
**Changes:**
- Added `audioQueue = Channel(Channel.UNLIMITED)` at start of each streaming session — fixes permanent channel closure on reconnect after 4:50m
- Wired `onDownload` callback to trigger `WhisperModelManager.downloadModel()` via ViewModel

### Agent S5: Rule 7 Sweep
**Files:** ~350 files across entire repository
**Changes:**
- Removed "Author: VOS4 Development Team" from ~131 VoiceOSCore files + other modules
- Removed "author: Claude Code" from 3 Voice/WakeWord files
- Removed "Author: AI Assistant" from 1 VoiceDataManager file
- Removed "Code-Reviewed-By: CCA" from 4 VoiceOSCore overlay files
- Removed "/ Claude AI" suffix from 12+ AI/Chat files
- Cleaned all other scattered Rule 7 violations

---

## Problems Addressed

| Problem # | Description | Status |
|-----------|-------------|--------|
| 4 | VoiceKeyboard entirely non-functional | FIXED (one word) |
| 9 | 160+ Rule 7 violations | FIXED (~350 files cleaned) |
| — | Phase 1 P0s (Boot, aliases, Hilt, settings link) | FIXED (4 P0s resolved) |
| — | Core pipeline bugs (memory leak, data races, phrase collisions) | FIXED (6 bugs resolved) |
| — | WebAvanue disambiguation + injection | FIXED (2 bugs resolved) |
| — | SpeechRecognition audioQueue + download button | FIXED (2 bugs resolved) |
| — | HTTP/2 RFC compliance | FIXED (enablePush) |

## Problems Deferred (Future Sessions)

| Problem # | Description | Why Deferred |
|-----------|-------------|-------------|
| 1 | RAG embeddings garbage | Needs BERT vocab.txt + WordPiece implementation |
| 2 | No database migration system | Medium effort — needs migration files |
| 3 | AvanueUI 90% stubs | Large effort — weeks of implementation |
| 5 | License enforcement stub | Needs server-side design |
| 6 | 80% UI has no AVID | Module-by-module sweep needed |
| 7 | IPC/Rpc simulation | Large effort — real transport |
| 8 | PluginSystem architecture | Fundamental design decision |
| 10 | 85% modules have zero tests | Large effort — commonTest infrastructure |

---

## Files Modified Summary

**Total: 369 files changed, 177 insertions, 470 deletions**

Key files by module:
- `Modules/VoiceKeyboard/` — 1 file (self-assignment fix)
- `Modules/VoiceOSCore/` — 4 files (memory leak, data races, phrase prefixes, println) + ~131 Rule 7
- `Modules/WebAvanue/` — 2 files (disambiguation, nonce validation)
- `Modules/SpeechRecognition/` — 2 files (audioQueue, download button)
- `Modules/HTTPAvanue/` — 1 file (enablePush)
- `Modules/ImageAvanue/` — 1 file (debug artifact)
- `Apps/avanues/` — 4 files (manifest, application, settings, config)
- ~350 files — Rule 7 attribution removal

---

Author: Manoj Jhawar | 260222
