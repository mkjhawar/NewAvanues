# Implementation Plan: Top 10 Codebase Problems
**Date:** 260222 | **Mode:** YOLO + SWARM + CoT + ToT | **Author:** Manoj Jhawar
**Source:** 20-agent deep review (435+ findings)
**Context:** 1M (required — multi-module SWARM implementation)

---

## Scope Decision (ToT Analysis)

### What we fix THIS SESSION (S + small-M effort):
- Problem 4: VoiceKeyboard self-assignment (S — one word)
- Problem 9: Rule 7 sweep (S — grep+sed)
- Phase 1 P0s: BootReceiver, activity-alias, HiltWorkerFactory, settings link (S each)
- Phase 2 core fixes: root.recycle(), @Volatile data races, println removal, disambiguation (S each)
- SpeechRecognition: audioQueue rebuild, download button wiring (S each)
- Database: migration system enablement (M)

### What we PLAN but DEFER (L effort, future sessions):
- Problem 1: RAG tokenizer (M — needs BERT vocab.txt, WordPiece implementation)
- Problem 3: AvanueUI 42 stubs (L — weeks of component implementation)
- Problem 5: License enforcement (M — needs server-side component design)
- Problem 6: AVID sweep 80% of UI (M — hundreds of files, sweep per module)
- Problem 7: IPC/Rpc real implementation (L — real AIDL/UDS transport)
- Problem 8: PluginSystem architecture (L — fundamental design decision)
- Problem 10: Test infrastructure (L — commonTest for 5+ critical modules)

---

## Implementation Waves

### Wave 1: One-Line Fixes (5 minutes, sequential — too fast for agents)

| # | Fix | File | Change |
|---|-----|------|--------|
| 1.1 | VoiceKeyboard self-assignment | VoiceKeyboardService.kt:140 | `currentInputConnection = currentInputConnection()` |
| 1.2 | BootReceiver exported | AndroidManifest.xml:184 | `exported="true"` |
| 1.3 | Debug mode default | DeveloperPreferences.kt:95 | `debugMode = false` |
| 1.4 | SFTP host key default | AvanuesSettingsRepository.kt:125 | `vosSftpHostKeyMode = "strict"` |
| 1.5 | ReadingHandler ttsReady race | ReadingHandler.kt:34 | `@Volatile var ttsReady` |
| 1.6 | lastScreenHash race | VoiceOSAccessibilityService.kt | `@Volatile var lastScreenHash` |
| 1.7 | HTTP/2 enablePush | Http2Settings.kt:8 | `enablePush = false` |
| 1.8 | ImageViewer debug artifact | ImageViewer.kt:174 | Remove `|| true` |
| 1.9 | Debug println hot path | CommandGenerator.kt:407,411 | Delete 2 println lines |

### Wave 2: SWARM — Module-Grouped Fixes (parallel agents)

| Agent | Module | Fixes | Effort |
|-------|--------|-------|--------|
| S1 | apps/avanues | Activity-alias, HiltWorkerFactory, accessibility settingsActivity, duplicate permissions | M |
| S2 | VoiceOSCore | root.recycle() try/finally in 2 locations, AccessibilityEvent.recycle, handler phrase prefixes | M |
| S3 | WebAvanue | Fix selectDisambiguationOption, add sendScrapeResult nonce validation | M |
| S4 | SpeechRecognition | Rebuild audioQueue per session, wire WhisperModelDownloadScreen onDownload | M |
| S5 | Rule 7 Sweep | Remove ~160 AI attributions across entire repo | S |

### Wave 3: Medium-Effort Structural (sequential, context-heavy)

| # | Fix | Module | Effort |
|---|-----|--------|--------|
| 3.1 | Enable database migrations | Database | M |
| 3.2 | Merge CursorCommandHandler into HandlerRegistry | VoiceOSCore | M |
| 3.3 | AVU escape unification | IPC + WebSocket + AVU | M |

### Deferred (Future Sessions — documented for handover)

| Problem | Why Deferred | Next Step |
|---------|-------------|-----------|
| RAG tokenizer | Needs BERT vocab.txt file + WordPiece algorithm | Source vocab.txt from HuggingFace, implement tokenizer |
| AvanueUI 42 stubs | Weeks of component implementation | Implement top 10 most-used components first |
| License enforcement | Needs server-side validation endpoint | Design API, implement LicenseClient |
| AVID sweep | ~200 files across all apps | Sweep module-by-module in dedicated sessions |
| IPC/Rpc real transport | Fundamental architecture decision | Decide AIDL vs UDS vs Binder, then implement |
| PluginSystem | .avp vs DEX fundamental mismatch | Decision: route .avp through AVU interpreter |
| Test infrastructure | commonTest for 5+ modules | Start with Foundation + AVID (pure logic, easiest) |

---

## Estimated Session Usage

| Wave | Items | Est. Context | Parallel? |
|------|-------|-------------|-----------|
| Wave 1 | 9 one-liners | ~5% | Sequential |
| Wave 2 | 5 agents | ~40% | SWARM parallel |
| Wave 3 | 3 structural | ~20% | Sequential |
| Handover + commit | — | ~5% | — |
| **Total** | **~25 fixes** | **~70%** | — |

---

Author: Manoj Jhawar | 260222
