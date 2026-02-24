# Wave 2 Batch 3 — Master Analysis Entry
**Module:** WebAvanue | **Date:** 260222 | **Branch:** VoiceOS-1M-SpeechEngine

## Review Reference
Full quality report: `docs/reviews/WebAvanue-Review-QualityAnalysis-260222-V1.md`
Prior session review (260220, 50 findings): documented in agent-memory MEMORY.md

## Score: 61 / 100 | Health: YELLOW

## Finding Counts (V1 incremental — new findings only vs prior session)
| Severity | Count |
|----------|-------|
| Critical (P0) | 3 |
| High (P1) | 5 |
| Medium (P2) | 6 |
| Low | 3 |
| **Total** | **17** |

## Top Issues

### P0 — Block Shipping

| ID | File | Issue |
|----|------|-------|
| W-01 | `BrowserVoiceOSCallback.kt:764` | `selectDisambiguationOption()` indexes full command list — executes wrong command |
| W-05 | `WebAvanueVoiceOSBridge.kt:233` | `sendScrapeResult()` accepts any web-page JS call — fake command injection |
| W-07 | `SecureScriptLoader.kt:230+` | `ScriptFragments` are plain Base64, not AES-encrypted — stated security property false |

### P1 — Fix Before Feature Complete

| ID | File | Issue |
|----|------|-------|
| W-02 | `TabViewModel.kt:548` | `captureScreenshot()` never calls `onComplete` — Rule 1 stub |
| W-03 | `TabViewModel.kt:407-412` | 6 scroll methods empty stubs |
| W-04 | `TabViewModel.kt:462` | `freezePage()` empty stub |
| W-06 | `GestureMapper.kt:62-63` | GESTURE_COPY/CUT/PASTE return `await` in non-async context — SyntaxError |
| W-10 | `WebAvanueVoiceOSBridge.kt:114` | Manual JSON unescaping brittle for multi-level escape |

### P2 — Hardening

| ID | File | Issue |
|----|------|-------|
| W-08 | `DOMScraperBridge.kt:1008` | `dragScript()` String params injected raw into JS |
| W-09 | `DOMScraperBridge.kt:1089+` | 12+ gesture scripts accept untyped String numeric params |
| W-11 | `SecureScriptLoader.kt:211` | `clearCache()` calls only `System.gc()` — does not zero strings |
| W-12 | `BrowserVoiceOSCallback.kt:130+` | `System.currentTimeMillis()` in commonMain — KMP violation |
| W-13 | `BrowserVoiceOSCallback.kt:597` | `String.hashCode()` URL cache key — 32-bit collision risk |
| W-14 | `AdBlocker.kt:25-26` | Overbroad regex ad patterns cause false positives |

### Low

| ID | File | Issue |
|----|------|-------|
| W-15 | `BrowserVoiceOSCallback.kt:140+` | 20+ `println()` in production commonMain |
| W-16 | `CommandBarAutoHide.kt:41` + 7 others | `System.currentTimeMillis()` in additional commonMain files |
| W-17 | `DOMScraperBridge.kt:511` | SCRAPER_SCRIPT always executes synchronously — no async path |

## Items Confirmed Fixed vs Prior Session (260220)
- `SecureStorage.kt:useEncryption` default — FIXED to `true` (encrypted by default)
- `DOMScraperBridge.kt` clickBySelector/focusBySelector/inputTextBySelector/scrollToBySelector — all now escape via `JsStringEscaper` — XSS FIXED
- `WebAvanueVoiceOSBridge.kt` `findInPage` JS injection (L163) — FIXED (uses `JsStringEscaper.escape()`)

## Items Still Open from Prior Session (260220)
The following HIGH findings from the 260220 session are still confirmed open:
- `AndroidWebViewController.kt:evaluateJavaScript()` always returns null (callback race) — confirmed still present
- `IOSWebView.kt` (iOS target disabled, not reviewed on this branch)
- `WebAvanueJsonRpcServer.kt isRunning` non-@Volatile — confirmed still present
- `TabViewModel.kt` scroll stubs — confirmed (W-03 above)
- `BrowserVoiceOSCallback.kt` selectDisambiguationOption bug — confirmed (W-01 above)

## Architecture Notes
- Build targets: Android only (iOS + Desktop disabled via comments in build.gradle.kts)
- `DOMScraperBridge` (commonMain) — Protected per Mandatory Rule #1; only sync/escaping bugs fixable
- `BrowserVoiceOSCallback` (commonMain) — Protected per Mandatory Rule #1; sync/logic bugs fixable
- Theme: No `MaterialTheme.colorScheme.*` violations found — module uses its own parallel color system (AppColors.kt, AvaMagicColors.kt) which is a known pre-existing issue from prior session
- Rule 7: No AI attribution violations found in files reviewed
- AVID: Not reviewed in this pass; prior session found zero AVID semantics across UI components
