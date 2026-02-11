# VoiceOSCore-Fix-WebAvanueTopBarVoice-260211-V1

## Problem
WebAvanue top bar buttons were not voice-enabled. Voice commands were not working for toolbar buttons (Back, Forward, Refresh, History, Settings, Bookmark).

## Root Cause
Three issues identified:

### 1. Label Parsing Breaks on `(Voice: ...)` Pattern
**File:** `CommandGenerator.kt` — `normalizeRealWearMlScript()`

Landscape toolbar buttons used contentDescription like `"Back (Voice: go back)"`. The normalizer's delimiter list `[":", "|", ",", "."]` found `:` inside the `(Voice: ...)` pattern and split the string:
- Input: `"Back (Voice: go back)"`
- Split on `:` → `["Back (Voice", " go back)"]`
- No `"hf_"` → takes `parts[0]` = `"Back (Voice"` (broken label)

### 2. Non-Standard Button Implementations
**File:** `AddressBar.kt`

History, Settings, and Bookmark buttons used `Box` + `Modifier.clickable` or `Surface` + `Modifier.clickable` instead of `IconButton`. While `Modifier.clickable` does set `isClickable = true` on `AccessibilityNodeInfo`, `IconButton` is the standard Compose pattern that guarantees proper minimum touch target, role semantics, and ripple indication.

### 3. Portrait vs Landscape Inconsistency
Portrait mode used `LabeledNavButton` with simple labels ("Back", "Fwd") without voice hints. Landscape mode had the broken `(Voice: ...)` pattern. No consistency between orientations.

## Fix Applied

### Two-Layer Label Normalizer (Universal)
**File:** `CommandGenerator.kt`

Added a two-layer approach to `normalizeRealWearMlScript()`:

**Layer 1: Universal `(Voice: ...)` Convention**
- Regex `\(Voice:\s*(.+?)\)\s*$` extracts explicit voice phrase
- Framework-agnostic: works for Compose, Flutter, React Native, Unity
- Example: `"Back (Voice: go back)"` → `"go back"`
- Any app developer can embed this in contentDescription

**Layer 2: Delimiter Splitting (Existing, Hardened)**
- Falls back to existing delimiter-based parsing for RealWear ML scripts
- Added guard: if split result is < 2 chars (e.g. time "3:45" → "3"), returns original text
- Protects third-party app labels that naturally contain delimiters

### Button Implementation Cleanup
**File:** `AddressBar.kt`

- Replaced all `Box` + `clickable` and `Surface` + `clickable` buttons with `IconButton`
- Added `(Voice: ...)` hints to all buttons in both portrait and landscape modes
- Added `voiceHint` parameter to `LabeledNavButton` for clean separation of visible label vs voice phrase

### Voice Hints Added

| Button | contentDescription | Voice Phrase |
|--------|-------------------|-------------|
| Back | Back (Voice: go back) | go back |
| Forward | Forward (Voice: go forward) | go forward |
| Refresh | Refresh (Voice: refresh) | refresh |
| History | History (Voice: open history) | open history |
| Settings | Settings (Voice: open settings) | open settings |
| Bookmark | Add bookmark (Voice: add bookmark) | add bookmark |
| Command bar | Show command bar (Voice: show command bar) | show command bar |
| Mic | Microphone (Voice: start listening) | start listening |
| Read | Read (Voice: reading mode) | reading mode |
| Voice | Voice (Voice: show command bar) | show command bar |

## Third-Party App Compatibility

The `(Voice: ...)` convention is designed as a **universal standard** for Android accessibility:

1. **Our apps**: Explicitly embed `(Voice: phrase)` in contentDescription
2. **Third-party apps with accessibility**: Labels like "Settings", "Back" work via auto-generated commands
3. **Third-party apps adopting convention**: Can embed `(Voice: ...)` in any framework
4. **RealWear apps**: Existing `hf_` prefix handling preserved in Layer 2

For third-party apps WITHOUT the convention, the system automatically:
1. Scrapes accessibility tree → `AndroidScreenExtractor`
2. Extracts labels → `AccessibilityNodeAdapter` → `CommandGenerator.deriveLabel()`
3. Generates commands → "click [label]" format
4. Matches user speech → matches against generated commands
5. Executes → `BoundsResolver` finds element → `AndroidGestureDispatcher` dispatches gesture

## Files Modified

| # | File | Change |
|---|------|--------|
| 1 | `CommandGenerator.kt` | Two-layer normalizer: Layer 1 (Voice: ...) + Layer 2 (delimiter, hardened) |
| 2 | `AddressBar.kt` | All buttons → IconButton, voice hints in both orientations |

## Verification
- BUILD SUCCESSFUL (compileDebugKotlinAndroid, both VoiceOSCore and WebAvanue)
- `normalizeRealWearMlScript("Back (Voice: go back)")` → `"go back"` (Layer 1)
- `normalizeRealWearMlScript("hf_btn:Go Back")` → `"Go Back"` (Layer 2, hf_ path)
- `normalizeRealWearMlScript("Settings")` → `"Settings"` (no delimiter, unchanged)
- `normalizeRealWearMlScript("3:45 PM")` → `"3:45 PM"` (guard: "3" < 2 chars → original)
- `normalizeRealWearMlScript("Volume: 50%")` → `"Volume"` (Layer 2, acceptable)
