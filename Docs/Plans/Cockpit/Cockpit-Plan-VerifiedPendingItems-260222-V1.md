# Cockpit-Plan-VerifiedPendingItems-260222-V1

## Context

The Cockpit module (`Modules/Cockpit/`) is a KMP multi-window workspace with SpatialVoice design language. Multiple prior sessions (260217-260220) built the core: 17 content types, 13 layouts, spatial head-tracking, VOS commands, and ContentRenderer.

Prior deep review documents listed 5 "critical", 7 "high", and 10 "medium" severity issues. **Source code verification revealed most had already been fixed.** This plan captures only the ACTUAL remaining gaps confirmed by reading the current source.

## Already Fixed (DO NOT RE-DO)

- Dispatchers.Main → Uses `Dispatchers.Default` (CockpitViewModel.kt:39)
- SpatialViewportController data race → Uses `@Volatile` (SpatialViewportController.kt:58)
- CockpitViewModel.createSession → Already `suspend` (CockpitViewModel.kt:98)
- CockpitViewModel.dispose() → Already exists (CockpitViewModel.kt:351)
- LayoutMode.SPATIAL_DICE → Already in `SPATIAL_CAPABLE` set
- ICockpitRepository → All 24 methods present including PinnedFrame/CrossFrameLink/TimelineEvent
- DesktopCockpitRepository → Exists in desktopMain
- 11 of 12 ecosystem issues from 260218 review → Fixed
- Phase 3 renderers (Form, Widget, Terminal, Map, AiSummary) → All already implemented as full composables

## Changes Made

### Phase 1: ContentAccent Exhaustive Mappings (commonMain)

**File**: `model/ContentAccent.kt`
**Change**: Added 8 explicit mappings to `forContentType()`:
- `form` → INFO (structured data, informational)
- `signature` → WARNING (action-required, amber)
- `voice` → SECONDARY (media, purple)
- `map` → SUCCESS (location/navigation, green)
- `whiteboard` → PRIMARY (creative, sapphire)
- `terminal` → TERTIARY (neutral)
- `ai_summary` → INFO (informational blue)
- `widget` → TERTIARY (neutral)

Retained `else -> TERTIARY` as safety net (Kotlin can't enforce exhaustiveness on String `when`).

### Phase 1: CommandBar addFrameOptions() (commonMain)

**File**: `ui/CommandBar.kt`
**Change**: Added 6 missing frame types to the "Add Frame" menu:
- Voice, Form, Map, Terminal, Widget, AI Summary (Beta)

Total: 17 options (was 11), organized by priority tier (P0/P1/P2/Killer Features).

### Phase 2: Signature Serialization Fix (androidMain, CRITICAL)

**File**: `content/ContentRenderer.kt`
**Bug**: `strokes.toString()` produced `[Stroke@3a4b5c]` — Java object identity strings.
**Fix**: Replaced with `AnnotationSerializer.strokesToJson(strokes)` for proper JSON round-trip.
**Import added**: `com.augmentalis.annotationavanue.controller.AnnotationSerializer`

### Phase 4: RowLayout Minimize/Maximize (Decision: Keep As-Is)

**File**: `ui/LayoutEngine.kt` (line 663-665)
**Decision**: Empty lambdas are correct — RowLayout's equal-width design makes minimize/maximize semantically wrong. No code change needed.

## Phase 3: Already Implemented (Verified)

All content renderers were found to be already implemented:
- **FormContent.kt**: Checkbox, text, number fields with progress bar, add item button
- **WidgetContent.kt**: All 8 WidgetType values — Clock/Timer/Stopwatch functional, sensors with graceful fallback
- **TerminalContent.kt**: Monospace, line numbers, auto-scroll, color coding, SelectionContainer, horizontal scroll
- **AiSummaryContent.kt**: Type selector, source frames, generate button, auto-refresh indicator
- **MapContentRenderer**: OpenStreetMap embed via WebView with bounding box calculation
- **VoiceNoteRenderer**: Recording state, duration display, transcription

## Files Modified

| File | Source Set | Changes |
|------|-----------|---------|
| `model/ContentAccent.kt` | commonMain | 8 explicit accent mappings added |
| `ui/CommandBar.kt` | commonMain | 6 missing frame types in addFrameOptions() |
| `content/ContentRenderer.kt` | androidMain | Signature serialization fix + import |

## Verification

1. ContentAccent: All 17 typeIds return an explicit accent
2. CommandBar: addFrameOptions() lists all 17 user-addable types
3. Signature: Uses AnnotationSerializer.strokesToJson() for proper JSON round-trip
4. Build: Gradle compile verification needed
