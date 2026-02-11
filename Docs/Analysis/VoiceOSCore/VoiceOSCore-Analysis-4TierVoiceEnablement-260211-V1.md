# VoiceOSCore-Analysis-4TierVoiceEnablement-260211-V1

## Problem Statement
Voice commands need to work across ALL Android apps regardless of framework (Compose, Flutter, React Native, Unity, native XML). Our solution must handle:
- Our own apps (full control)
- Third-party apps with developer cooperation
- Third-party apps with zero cooperation
- Enhanced profiles for popular apps

## Architecture: 4-Tier Voice Enablement

```
Tier 1: OUR APPS ────────────────── Full AVID + Static Commands + (Voice: ...)
Tier 2: DEVELOPER CONVENTION ────── (Voice: phrase) in contentDescription
Tier 3: AUTOMATIC SCRAPING ──────── Accessibility tree → CommandGenerator
Tier 4: VOICE PROFILES ─────────── .VOS files (scanned / community / curated)
```

### Tier 1: Our Apps (Full Control)
- Every interactive element has an AVID (Avanue Voice ID)
- Static commands in `StaticCommandRegistry` (go back, scroll down, etc.)
- `(Voice: ...)` in contentDescription for explicit phrase mapping
- Full pipeline: scraping + command generation + execution
- **Priority: Highest** — AVID matches override all other tiers

### Tier 2: Developer Convention (Opt-In, Zero Dependency)
- Third-party developers add `(Voice: phrase)` to contentDescription
- Works in ANY Android framework — it's just a string in accessibility label
- No SDK dependency, no build changes, no library import
- `CommandGenerator.normalizeRealWearMlScript()` Layer 1 extracts the phrase
- **AvanueAI IDE Tools**: Auto-add `(Voice: ...)` to elements
- **Developer Manual**: Spec for the convention
- **Example**:
  ```kotlin
  // Compose
  contentDescription = "Play (Voice: play music)"
  // Flutter
  Semantics(label: "Play (Voice: play music)")
  // React Native
  accessibilityLabel="Play (Voice: play music)"
  // Unity
  accessibilityDescription = "Play (Voice: play music)"
  ```

### Tier 3: Automatic Scraping (Zero Integration)
- `AndroidScreenExtractor` traverses accessibility tree every screen change
- `AccessibilityNodeAdapter` maps `AccessibilityNodeInfo` → `ElementInfo`
- `CommandGenerator.deriveLabel()` picks best label from text/contentDescription/resourceId
- Auto-generates `QuantizedCommand` with phrase = label
- User says "click [label]" to activate
- **Works for ANY Android app out of the box**
- **Priority**: Lowest — supplemental to Tier 1/2/4

### Tier 4: Voice Profiles (.VOS Files)
- Scanning utility traverses app accessibility tree
- Assigns AVIDs to stable elements using `ElementFingerprint`
- `PersistenceDecisionEngine` filters volatile elements
- Exports compact .VOS voice profile per app
- Profiles can be: auto-generated, developer-authored, community-shared
- Loaded at runtime to supplement Tier 3 auto-scraping
- Higher confidence than auto-scraped commands

## Priority Resolution

When a user speaks a command, the `ActionCoordinator` resolves in priority order:

```
1. Static Commands (Tier 1)           ← "go back", "scroll down"
   ↓ no match
2. Voice Profile Commands (Tier 4)    ← loaded .VOS with stable AVIDs
   ↓ no match
3. Developer Hints (Tier 2)           ← (Voice: ...) extracted phrases
   ↓ no match
4. Auto-Scraped Commands (Tier 3)     ← dynamic from current screen
   ↓ no match
5. NLU fallback                       ← fuzzy matching / intent resolution
```

Tiers 2-4 all flow through the same `CommandGenerator` → `QuantizedCommand` pipeline.
The difference is confidence and persistence:
- Tier 2: Medium confidence (developer-specified phrase, no AVID stability data)
- Tier 3: Low confidence (auto-derived label, ephemeral)
- Tier 4: High confidence (scanned AVID, persistence-verified, stable)

## .VOS Voice Profile Format (AVU Compact)

### File Extension
`.vos` — VoiceOS Seed file

### Format
AVU compact wire protocol with YAML header:

```
---
schema: avu-vos-1.0
version: 1.0.0
locale: en-US
app: com.spotify.music
app_version: 8.9.x
source: scan
generated: 2026-02-11
element_count: 42
metadata:
  display_name: Spotify
  stability_score: 0.87
  screens_covered: 6
---
CAT:home:Home Screen:Main navigation and playback controls
CAT:search:Search:Find music and podcasts
CAT:library:Library:Your saved music and playlists
ELM:BTN:a3f2e1c9:shuffle play:CLICK:home:0.95
ELM:BTN:b4e3d2a1:play:CLICK:home:0.92
ELM:BTN:c5d4e3f2:search:CLICK:home:0.98
ELM:TXT:d6e5f4a3:liked songs:CLICK:library:0.90
ELM:INP:e7f6a5b4:search bar:TYPE:search:0.99
DIS:BTN:b4e3d2a1:h=LL[0]/FL[1]:z=content:p=c7d8e9f0
---
SYN:shuffle play:[shuffle,play random,random play]
SYN:play:[resume,start,start playing]
SYN:search:[find,look for,search for]
```

**Note:** No absolute bounds in ELM. AVID hash is device-independent (based on
className + resourceId + text + contentDescription). At runtime, BoundsResolver
finds the element in the LIVE accessibility tree and uses LIVE bounds. This makes
profiles fully portable across devices, orientations, and DPI configurations.

### Wire Protocol Codes for .VOS

| Code | Format | Description |
|------|--------|-------------|
| `CAT` | `CAT:id:name:description` | Screen/category definition |
| `ELM` | `ELM:typeCode:hash:phrase:action:screen:confidence` | Element voice command (no bounds) |
| `DIS` | `DIS:typeCode:hash:h=path:z=zone:p=parentHash` | Disambiguation (when AVID not unique) |
| `SYN` | `SYN:phrase:[alt1,alt2,...]` | Synonym/alternate phrases |
| `ACT` | `ACT:phrase:actionType:targetAvid` | Action mapping override |
| `IGN` | `IGN:typeCode:hash:reason` | Explicitly ignored element |

### ELM Field Breakdown

```
ELM:BTN:a3f2e1c9:shuffle play:CLICK:home:0.95
     │   │        │             │     │    │
     │   │        │             │     │    └── confidence (0.0-1.0)
     │   │        │             │     └── screen/category ID
     │   │        │             └── action type (CLICK, TYPE, SCROLL, LONG_PRESS)
     │   │        └── voice phrase (the command label)
     │   └── element hash (8-char hex, device-independent)
     └── type code (BTN, TXT, INP, CHK, IMG, SCR, etc.)
```

### Type Codes

| Code | Element Type |
|------|-------------|
| `BTN` | Button, IconButton, FAB |
| `TXT` | TextView, clickable text |
| `INP` | EditText, TextField, SearchBar |
| `CHK` | Checkbox, Switch, Toggle |
| `IMG` | ImageButton, clickable Image |
| `SCR` | Scrollable container |
| `TAB` | Tab, TabItem |
| `MNU` | Menu item, dropdown |
| `LNK` | Link, clickable URL |
| `LST` | List item (in RecyclerView, etc.) |

### DIS (Disambiguation) — Hybrid 3-Layer Strategy

When multiple elements share the same AVID hash (common in Flutter, React Native,
Unity where elements lack Android resourceId), the `DIS` code provides device-independent
disambiguation using three complementary methods:

```
DIS:BTN:b4e3d2a1:h=LL[0]/FL[1]:z=content:p=c7d8e9f0
     │   │        │              │         │
     │   │        │              │         └── p= parent-extended hash
     │   │        │              └── z= semantic zone
     │   │        └── h= hierarchy path
     │   └── element hash (same as ELM reference)
     └── type code
```

**Layer 1: Hierarchy Path (`h=`)**
Structural position in the view tree. Device-independent because the view hierarchy
is determined by app code, not screen size.
- Format: `ParentAbbrev[childIndex]/ParentAbbrev[childIndex]/...`
- Example: `h=RV[0]/LL[2]/FL[1]` = RecyclerView child 0 → LinearLayout child 2 → FrameLayout child 1
- Abbreviations: RV=RecyclerView, LL=LinearLayout, FL=FrameLayout, CL=ConstraintLayout, RL=RelativeLayout, CV=ComposeView, VW=View

**Layer 2: Semantic Zone (`z=`)**
Spatial region detected by heuristics. Robust to layout changes because zones
are relative, not absolute.
- Values: `header` (top 15%), `nav` (bottom 10%), `content` (middle), `sidebar` (left/right 20%), `overlay` (floating/dialog)
- Detection: Based on element bounds relative to screen dimensions (percentage-based)
- Fallback: `content` if zone can't be determined

**Layer 3: Parent-Extended Hash (`p=`)**
Hash that includes parent context for uniqueness.
- Computed as: `hash(parentClassName + parentResourceId + childIndex + siblingCount)`
- 8-char hex, same format as AVID hash
- Different from AVID because it includes positional context within the parent

**Resolution Order (at runtime):**
```
1. AVID exact match (resourceId-based)         ← 80% of native Android elements
   ↓ ambiguous (multiple matches)
2. Filter by hierarchy path (h=)               ← structural position
   ↓ still ambiguous
3. Filter by parent-extended hash (p=)         ← parent context
   ↓ still ambiguous
4. Filter by semantic zone (z=)                ← spatial heuristic
   ↓ still ambiguous
5. Confidence score tiebreaker                 ← highest confidence wins
```

**When DIS is needed:**
- `DIS` lines are only emitted when the scanner detects AVID collisions (2+ elements with same hash)
- For unique AVIDs (most elements), no `DIS` line needed
- Keeps .VOS files compact — disambiguation is the exception, not the rule

### Rotation & Display Size Handling

**Why bounds are NOT in the profile:**
- Absolute bounds (pixel coordinates) are device-specific
- Screen rotation changes ALL bounds
- Different DPI/resolution = different pixel positions
- Bounds make profiles non-portable

**How targeting works WITHOUT bounds:**
1. Profile provides AVID + phrase + screen
2. At runtime, BoundsResolver finds element in LIVE accessibility tree:
   - Layer 1: `findAccessibilityNodeInfosByViewId(resourceId)` — identity-based
   - Layer 2: Full tree search matching text/contentDescription/className
   - Layer 3: Disambiguation via hierarchy path / semantic zone
3. LIVE bounds from the found node are used for gesture dispatch
4. Works on ANY device, ANY orientation, ANY DPI

**What triggers re-resolution:**
- Screen rotation → ScreenCacheManager detects new hash → full re-scrape
- App resize (foldable) → same detection → re-scrape
- Scroll → delta compensation in BoundsResolver Layer 2
- Navigation → new screen → full re-scrape

## Scanning Utility Design

### Concept: "VoiceOS App Trainer"

A built-in mode in the Avanues app that lets users train voice commands for installed apps.

### How It Works

```
User opens "App Trainer" in Avanues settings
        ↓
Selects an installed app (e.g., Spotify)
        ↓
App Trainer launches the target app
        ↓
AccessibilityService monitors + scrapes each screen
        ↓
User navigates through key screens (home, search, player, etc.)
        ↓
PersistenceDecisionEngine decides what's stable
        ↓
After training, exports .VOS voice profile
        ↓
Profile loaded automatically when target app is foregrounded
```

### What Already Exists

| Component | Location | Ready? |
|-----------|----------|--------|
| `AndroidScreenExtractor` | VoiceOSCore/androidMain | Yes — scrapes any app |
| `AccessibilityNodeAdapter` | VoiceOSCore/androidMain | Yes — maps nodes to ElementInfo |
| `CommandGenerator` | VoiceOSCore/commonMain | Yes — generates QuantizedCommands |
| `PersistenceDecisionEngine` | VoiceOSCore/commonMain | Yes — 4-layer persistence decision |
| `ScreenCacheManager` | VoiceOSCore/androidMain | Yes — detects screen changes |
| `ElementFingerprint` | VoiceOSCore/commonMain | Yes — generates stable AVIDs |
| `ScrapedElement` table | Database module | Yes — stores elements |
| `GeneratedCommand` table | Database module | Yes — stores commands |
| AVU wire protocol | AvuCodec module | Yes — encode/decode |

### What Needs To Be Built

| Component | Purpose | Effort |
|-----------|---------|--------|
| `VosProfileExporter` | Export persisted commands to .VOS format | Small |
| `VosProfileLoader` | Load .VOS file → inject into command registry | Small |
| `AppTrainerMode` | UI for guided app scanning | Medium |
| `StabilityTracker` | Track element stability across sessions | Small (extends PersistenceDecisionEngine) |
| `ProfileSharingService` | Upload/download community profiles | Large (future) |

### Passive Learning (Always Active)

The existing `PersistenceDecisionEngine` + `ScreenCacheManager` already passively learn stable elements as the user navigates. The only missing piece is EXPORTING this accumulated knowledge as a .VOS file.

```
User uses Spotify daily
        ↓
AccessibilityService scrapes each screen (already happening)
        ↓
PersistenceDecisionEngine persists stable elements (already happening)
        ↓
After N sessions, confidence scores stabilize (already happening)
        ↓
VosProfileExporter.export("com.spotify.music") ← NEW: export as .VOS
        ↓
.VOS file saved to app-specific directory
```

## Developer SDK: `(Voice: ...)` Convention Spec

### Specification

Any Android app can add explicit voice command phrases by embedding `(Voice: phrase)` at the END of `contentDescription`:

```
contentDescription = "Display Label (Voice: voice command phrase)"
```

### Rules
1. Pattern must be at the END of the string
2. Parentheses and "Voice:" prefix are required
3. The phrase inside is extracted as-is (trimmed)
4. Display label before the pattern is used for standard accessibility
5. If the pattern is absent, VoiceOS auto-derives from the label

### Framework Examples

**Jetpack Compose:**
```kotlin
Icon(
    imageVector = Icons.Default.Shuffle,
    contentDescription = "Shuffle (Voice: shuffle play)"
)
```

**Flutter:**
```dart
Semantics(
  label: 'Shuffle (Voice: shuffle play)',
  child: IconButton(icon: Icon(Icons.shuffle), onPressed: _shuffle),
)
```

**React Native:**
```jsx
<TouchableOpacity accessibilityLabel="Shuffle (Voice: shuffle play)">
  <ShuffleIcon />
</TouchableOpacity>
```

**Android XML:**
```xml
<ImageButton
    android:contentDescription="Shuffle (Voice: shuffle play)"
    android:src="@drawable/ic_shuffle" />
```

**Unity (via Android Accessibility bridge):**
```csharp
gameObject.GetComponent<AccessibilityNode>()
    .SetContentDescription("Shuffle (Voice: shuffle play)");
```

### Benefits for Developers
- Zero dependency — no SDK, no library, no build changes
- Works with ANY accessibility testing tool
- Backwards-compatible — standard screen readers ignore the pattern
- Discoverable — VoiceOS normalizer Layer 1 extracts it automatically

## Conflict Analysis

### Do the 4 tiers conflict? NO.

| Scenario | Resolution |
|----------|-----------|
| Static command + scraped element with same phrase | Static wins (higher priority in ActionCoordinator) |
| Voice profile + auto-scraped same element | Profile wins (higher confidence) |
| Developer hint + auto-derived same element | Hint wins (Layer 1 normalizer extracts explicit phrase) |
| Multiple elements with same label | Numbers overlay differentiates ("click 1", "click 2") |
| App update changes element positions | AVID hash detects change, triggers re-scan |

### Data Flow (No Duplication)

```
Screen change detected
        ↓
ScreenCacheManager checks if screen is cached
        ↓ (cache miss)
AndroidScreenExtractor scrapes accessibility tree
        ↓
For each element:
  1. Check if AVID exists in loaded .VOS profile (Tier 4)
     → Yes: use profile command (high confidence)
     → No: continue
  2. CommandGenerator.deriveLabel()
     a. Layer 1: Check for (Voice: ...) hint (Tier 2)
     b. Layer 2: Delimiter parsing with guard (Tier 3)
  3. Generate QuantizedCommand
        ↓
ActionCoordinator.updateDynamicCommandsBySource(source, commands)
        ↓
Commands available for voice matching
```

## Next Steps

### Immediate (This Session)
- [x] Two-layer normalizer in CommandGenerator (Layer 1: Voice hint, Layer 2: delimiter + guard)
- [x] AddressBar.kt button cleanup (IconButton migration + voice hints)
- [ ] Write architecture doc (this document)

### Near-Term (Next Sessions)
- [ ] `VosProfileExporter` — export persisted commands as .VOS
- [ ] `VosProfileLoader` — load .VOS into command registry
- [ ] Developer Manual chapter for `(Voice: ...)` convention spec
- [ ] Developer Manual chapter for .VOS voice profile format

### Medium-Term
- [ ] App Trainer mode UI in Avanues settings
- [ ] StabilityTracker for element confidence over sessions
- [ ] Auto-export after N sessions of passive learning

### Long-Term
- [ ] AvanueAI IDE plugin (Android Studio / VS Code)
- [ ] Community profile sharing (cloud sync)
- [ ] Pre-built profiles for top 100 Android apps
