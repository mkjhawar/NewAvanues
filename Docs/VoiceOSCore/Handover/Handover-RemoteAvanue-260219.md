# Session Handover - 260219
## Current State
Repo: NewAvanues | Branch: Cockpit-Development | Mode: .yolo .tot .cot | CWD: /Volumes/M-Drive/Coding/NewAvanues

## Task In Progress
RemoteAvanue/GlassAvanue full system implementation planning + research.
User's latest request: implement wake-word into app (KMP cross-platform) with system settings for user to choose wake word or disable.

## Completed This Session
1. Ordinal command fix (non-target app overlays) — `8fc85bd6`
2. CommandMatcher 3-layer optimization (LRU cache + pre-filter + early exit) — `8fc85bd6`
3. Migrate 11 runBlocking → suspend variants — `8fc85bd6`
4. Hardcoded BOM cleanup (10 modules) — `59da63c1`
5. Wire AI ActionCoordinator dispatch (5 command types) — `8fc85bd6`
6. VideoAvanue Android UI (ExoPlayer + gallery + Cockpit wiring) — `703ee45f`
7. Fix compile errors (NOT_SUPPORTED + CommandResult sealed class) — `f482f4d5`
8. Consolidate CommandResult → ActionResult rename — `2d191f63`
9. Flatten managers/commandmanager → commandmanager — `f16d7d79`
10. Fix ActionResult naming collision → CommandExecutionResult — `47abb7d1`
11. RemoteAvanue spec + SDK research + UI mockups + full implementation plan — `92473af1` through `700f8e73`

## Next Steps (Priority Order)
1. **Wake-word implementation (KMP)** — User request: add wake-word with system setting (enable/disable, custom word)
   - Existing interface: `enableWakeWord()` in ISpeechEngine, not wired to Vivoka
   - KMP approach: `IWakeWordEngine` expect/actual (Android: Vivoka wake-word, iOS: Apple Speech, Desktop: Vosk)
   - Settings: add to Foundation.AvanuesSettings — `wakeWordEnabled: Boolean`, `wakeWord: String`
   - UI: add to UnifiedSettingsScreen via new SettingsProvider
2. **Phase 1.1: Extract HUDManager** to Modules:HUDManager (16 files, 0 callers)
3. **Phase 1.2: Extract LocalizationManager** to Modules:LocalizationManager (12 files, 0 callers)
4. **Phase 2: RemoteCast foreground service** + MediaProjection consent flow
5. **Phase 3: Protocol extension** (CMD + VOC messages, AVU wire format)
6. **GlassAvanue + GlassClient apps**

## Key Architecture Decisions Made
- GlassAvanue = phone SERVER app (intelligence on phone, glasses thin)
- GlassClient = thin glasses APK (~15-25MB without Vivoka)
- Two modes: HUD (Z100/BLE) vs Cast (WiFi/TCP)
- AVU wire format for command relay (VOS compact v3.0)
- VOCAB sync: sender pushes actionable items on screen change
- NanoHTTPD (117KB) over Ktor Server (8-15MB) for WebRTC signaling
- Dual streaming: MJPEG for glasses, WebRTC for browsers

## Key Files Created/Modified
- `docs/plans/RemoteCast/RemoteCast-Spec-GlassClientArchitecture-260219-V1.md`
- `docs/plans/RemoteCast/RemoteAvanue-Plan-FullSystemImplementation-260219-V1.md`
- `docs/analysis/RemoteCast/RemoteCast-Analysis-SmartGlassesSDKResearch-260219-V1.md`
- `demo/remoteavanue/RemoteCast-UI-Mockups.html`
- `docs/plans/VoiceOSCore/VoiceOSCore-Plan-TechDebtAndSixModule-260219-V1.md`
- `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-TechDebtCluster-260219-V1.md`

## Battery Analysis Finding
VoiceOSCore is event-driven (NOT polling). Dominant cost = continuous speech recognition.
Wake-word gating = key optimization (10%/hr → 2-3%/hr idle). enableWakeWord() interface exists but NOT wired.

## APK Size Finding
286MB debug: Vivoka = 83% (164MB models + 74MB native). AvanueUI = ~2-3MB. Our code = ~15-20MB.
Release est: ~120-150MB. GlassClient (no Vivoka): ~15-25MB.

## Quick Resume
Read this handover + `docs/plans/RemoteCast/RemoteAvanue-Plan-FullSystemImplementation-260219-V1.md` and continue with wake-word implementation.
