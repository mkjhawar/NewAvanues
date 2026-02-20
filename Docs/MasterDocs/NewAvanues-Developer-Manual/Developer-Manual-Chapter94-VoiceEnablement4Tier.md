# Chapter 94: 4-Tier Voice Enablement Architecture

## AI Context Brief

This chapter documents the complete voice enablement system for VoiceOS. It covers how voice
commands are generated, matched, and executed across ALL Android apps — both our own and
third-party apps of any framework (Compose, Flutter, React Native, Unity, native XML).

**Key files referenced in this chapter:**
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/CommandGenerator.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/StaticCommandRegistry.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/element/ElementFingerprint.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/element/ElementInfo.kt`
- `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/AndroidScreenExtractor.kt`
- `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/AccessibilityNodeAdapter.kt`
- `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/BoundsResolver.kt`
- `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/AndroidGestureDispatcher.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/actions/ActionCoordinator.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/SynonymRegistry.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/help/HelpCommandData.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/help/HelpScreenHandler.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/handler/WebCommandHandler.kt`

**Related chapters:**
- Chapter 93: Voice Command Pipeline & Localization Architecture
- Chapter 03: VoiceOSCore Deep Dive
- Chapter 05: WebAvanue Deep Dive
- Chapters 81-87: AVU DSL Evolution

---

## 1. The 4-Tier Model

Voice enablement operates across four independent tiers that coexist without conflict.
Each tier serves a different integration level — from full control to zero cooperation.

```
┌──────────────────────────────────────────────────────────────────────┐
│                    VOICE COMMAND RESOLUTION                          │
│                                                                      │
│  User speaks: "shuffle play"                                         │
│       ↓                                                              │
│  ┌─ Tier 1: Static Commands ──────── "go back", "scroll down"  ──┐  │
│  │  Source: StaticCommandRegistry    Priority: HIGHEST            │  │
│  │  Works: Our apps only             Hardcoded phrases            │  │
│  └────────────────────────────────────────────────────────────────┘  │
│       ↓ no match                                                     │
│  ┌─ Tier 4: Voice Profiles (.VOS) ── Pre-scanned app commands ───┐  │
│  │  Source: .VOS files               Priority: HIGH               │  │
│  │  Works: Any scanned app           Stable AVIDs, verified       │  │
│  └────────────────────────────────────────────────────────────────┘  │
│       ↓ no match                                                     │
│  ┌─ Tier 2: Developer Convention ─── (Voice: phrase) hints ──────┐  │
│  │  Source: contentDescription        Priority: MEDIUM            │  │
│  │  Works: Apps using convention     Zero-dependency              │  │
│  └────────────────────────────────────────────────────────────────┘  │
│       ↓ no match                                                     │
│  ┌─ Tier 3: Auto-Scraping ───────── Accessibility tree scan ─────┐  │
│  │  Source: Live accessibility tree   Priority: NORMAL            │  │
│  │  Works: ANY Android app           Automatic, no cooperation    │  │
│  └────────────────────────────────────────────────────────────────┘  │
│       ↓ no match                                                     │
│  NLU fallback → fuzzy matching / intent resolution                   │
└──────────────────────────────────────────────────────────────────────┘
```

### 1.1 Tier 1: Our Apps (Full Control)

**Scope:** Avanues app and any app built with VoiceOS SDK.

**How it works:**
- `StaticCommandRegistry` defines ~100+ voice commands organized by category
- Each `StaticCommand` has: phrases, actionType, category, description, metadata
- `ActionCoordinator` routes to the appropriate handler based on category and source
- Handlers execute via platform-specific dispatchers (JS injection for web, AccessibilityService for Android)

**Components:**
| Component | File | Role |
|-----------|------|------|
| StaticCommandRegistry | `command/StaticCommandRegistry.kt` | Central command definitions |
| CommandActionType | `CommandModels.kt` | Enum of 70+ action types |
| CommandCategory | `CommandModels.kt` | 13 categories (NAVIGATION, MEDIA, BROWSER, WEB_GESTURE, etc.) |
| ActionCoordinator | `actions/ActionCoordinator.kt` | Routes commands to handlers |
| WebCommandHandler | `handler/WebCommandHandler.kt` | Web JS execution |
| AndroidGestureHandler | `VoiceOSCoreAndroidFactory.kt` | Native gesture dispatch |
| SystemHandler | `VoiceOSCoreAndroidFactory.kt` | Global actions (back, home) |
| AppHandler | `VoiceOSCoreAndroidFactory.kt` | App launch via Intent |
| AndroidCursorHandler | `AndroidCursorHandler.kt` | Cursor overlay service |

**Priority:** Highest. Static commands always win over dynamic commands.

**AVID integration:** Our app elements have AVIDs assigned via `ElementFingerprint.fromElementInfo()`.
The AVID hash is deterministic: `hash(packageName + className + resourceId + text + contentDescription)`.
Including `packageName` ensures cross-app uniqueness — the same button in different apps produces different AVIDs, which is critical for VOS export portability.

### 1.2 Tier 2: Developer Convention (Opt-In, Zero Dependency)

**Scope:** Any third-party app that adopts the `(Voice: ...)` convention.

**How it works:**
Any Android app developer can add explicit voice command phrases by embedding
`(Voice: phrase)` at the END of their element's `contentDescription`:

```
contentDescription = "Display Label (Voice: voice command phrase)"
```

When VoiceOS scrapes this element via the accessibility tree, `CommandGenerator.normalizeRealWearMlScript()`
Layer 1 extracts the voice phrase using regex: `\(Voice:\s*(.+?)\)\s*$`

**Rules:**
1. Pattern MUST be at the END of the string
2. Parentheses and `Voice:` prefix are REQUIRED (case-sensitive)
3. The phrase inside is extracted as-is (trimmed of whitespace)
4. Display label before the pattern is used for standard accessibility (screen readers)
5. If the pattern is absent, VoiceOS falls through to Tier 3 (auto-derive from label)

**Framework examples:**

```kotlin
// Jetpack Compose
Icon(
    imageVector = Icons.Default.Shuffle,
    contentDescription = "Shuffle (Voice: shuffle play)"
)

// Compose with LabeledNavButton (our internal component)
LabeledNavButton(
    icon = Icons.Default.Refresh,
    label = "Reload",
    voiceHint = "refresh"  // produces contentDescription = "Reload (Voice: refresh)"
)
```

```dart
// Flutter
Semantics(
  label: 'Shuffle (Voice: shuffle play)',
  child: IconButton(icon: Icon(Icons.shuffle), onPressed: _shuffle),
)
```

```jsx
// React Native
<TouchableOpacity accessibilityLabel="Shuffle (Voice: shuffle play)">
  <ShuffleIcon />
</TouchableOpacity>
```

```xml
<!-- Android XML -->
<ImageButton
    android:contentDescription="Shuffle (Voice: shuffle play)"
    android:src="@drawable/ic_shuffle" />
```

```csharp
// Unity (via Android accessibility bridge)
gameObject.GetComponent<AccessibilityNode>()
    .SetContentDescription("Shuffle (Voice: shuffle play)");
```

**Why this matters:**
- Zero dependency — no SDK, no library, no build changes
- Backwards-compatible — standard screen readers ignore the `(Voice: ...)` pattern
- Framework-agnostic — works with ANY Android UI framework
- Entry point for the AvanueAI developer ecosystem

**Implementation in CommandGenerator.kt:**
```kotlin
private val VOICE_HINT_PATTERN = Regex("\\(Voice:\\s*(.+?)\\)\\s*$")

private fun normalizeRealWearMlScript(text: String): String {
    // Layer 1: Check for explicit (Voice: ...) hint
    val voiceMatch = VOICE_HINT_PATTERN.find(text)
    if (voiceMatch != null) {
        return voiceMatch.groupValues[1].trim()
    }
    // Layer 2: Delimiter-based parsing (fallback)
    ...
}
```

### 1.3 Tier 3: Automatic Scraping (Zero Integration)

**Scope:** ANY Android app, regardless of framework or developer cooperation.

**How it works:**
1. `AccessibilityService` monitors screen changes (window state, content changes)
2. `ScreenCacheManager` detects if the screen is new/changed (signature hash)
3. `AndroidScreenExtractor` traverses the accessibility tree, extracting ALL visible actionable elements
4. `AccessibilityNodeAdapter` maps each `AccessibilityNodeInfo` → `ElementInfo`
5. `CommandGenerator.fromElementWithPersistence()` generates `QuantizedCommand` for each element
6. Commands are registered with `ActionCoordinator.updateDynamicCommandsBySource()`
7. User says "click [label]" to activate any element

**Data flow:**

```
AccessibilityService.onAccessibilityEvent()
    ↓
ScreenCacheManager.shouldProcess(rootNode)
    ↓ (cache miss = new screen)
AndroidScreenExtractor.extractElements(rootNode)
    ↓
For each AccessibilityNodeInfo:
    AccessibilityNodeAdapter.toElementInfo(node)
        ↓
    ElementInfo {
        text, contentDescription, resourceId,
        className, bounds, isClickable, isScrollable,
        isParentClickable, listIndex, containerResourceId,
        scrollOffsetX, scrollOffsetY
    }
        ↓
    CommandGenerator.fromElementWithPersistence(elementInfo, packageName, allElements)
        ↓
    deriveLabel(element):
        1. element.text (if not blank)
        2. element.contentDescription (if not blank)
        3. element.resourceId → strip prefix, replace _ with space
        4. empty string → skip element
            ↓
        normalizeRealWearMlScript(rawLabel):
            Layer 1: (Voice: ...) extraction  ← Tier 2
            Layer 2: Delimiter splitting + guard
            ↓
        SymbolNormalizer.normalize()  (& → and, # → pound, etc.)
            ↓
        final label
        ↓
    QuantizedCommand {
        phrase = label,           // "Shuffle Play"
        actionType = CLICK,       // derived from className
        targetAvid = "BTN:a3f2",  // ElementFingerprint hash
        confidence = 0.7,         // from PersistenceDecisionEngine
        metadata = { packageName, elementHash, bounds, resourceId, ... }
    }
        ↓
    PersistenceDecisionEngine.decideForElement()
        Layer 1: App category (EMAIL → don't persist messages)
        Layer 2: Container type (RecyclerView → dynamic)
        Layer 3: Content signals (text length, stability)
        Layer 4: Screen type (settings → persist, list → don't)
        ↓
    GeneratedCommandResult { command, shouldPersist, listIndex }
```

**Key properties checked for actionability:**
- `isActionable`: `isClickable || isScrollable || isLongClickable || isParentClickable`
- `hasVoiceContent`: `text.isNotBlank() || contentDescription.isNotBlank() || resourceId.isNotBlank()`

**Label derivation priority:** `text` > `contentDescription` > `resourceId` > skip

### 1.4 Tier 4: Voice Profiles (.VOS Files)

**Scope:** Any app that has been scanned or has a community-contributed profile.

**How it works:**
- A scanning utility (or passive learning) observes app elements over time
- Stable elements are identified via `PersistenceDecisionEngine`
- Element AVIDs, phrases, actions, and disambiguation data are exported to `.VOS` files
- When the target app is foregrounded, the `.VOS` profile is loaded
- Profile commands supplement Tier 3 auto-scraping with higher confidence

**Advantages over Tier 3:**
- Pre-verified element stability (confidence > 0.9)
- Synonym mappings for each element
- Disambiguation data for elements without resourceId
- Faster matching (pre-computed, no per-screen generation needed)
- Shareable across devices via cloud sync

---

## 2. AVID: Avanue Voice ID

### 2.1 What AVID Is

AVID (Avanue Voice ID) is a deterministic, device-independent identifier for UI elements.

**Format:** `{TypeCode}:{hash8}`
**Example:** `BTN:a3f2e1c9`

**Generated by:** `ElementFingerprint.fromElementInfo(element, packageName)`

**Hash inputs:** `packageName + className + resourceId + text + contentDescription`

**Critical properties:**
- AVID does NOT include bounds/position. The same button produces the same AVID on Pixel 7, Galaxy Fold, iPad-sized tablet, portrait or landscape.
- AVID DOES include `packageName`. The same "Settings" button in Gmail and Chrome produces different AVIDs, enabling VOS files to be exported and shared across devices without cross-app collisions.

### 2.2 Type Codes

| Code | Element Type | Detection |
|------|-------------|-----------|
| `BTN` | Button, IconButton, FAB | className contains "button" |
| `TXT` | TextView, clickable text | className contains "text" |
| `INP` | EditText, TextField, SearchBar | className contains "edit" or "textfield" |
| `CHK` | Checkbox, Switch, Toggle | className contains "check" or "switch" |
| `IMG` | ImageButton, clickable Image | className contains "image" |
| `SCR` | Scrollable container | isScrollable = true |
| `TAB` | Tab, TabItem | className contains "tab" |
| `MNU` | Menu item, dropdown | className contains "menu" |
| `LNK` | Link, clickable URL | className contains "link" |
| `LST` | List item | inside RecyclerView/ListView |

Type codes are inferred by `TypeCode.fromTypeName(className)` in the AVID module.

### 2.3 AVID Collision Handling

When two elements have the same AVID hash (same className + text + contentDescription,
no unique resourceId), disambiguation is needed. This is common in:
- Flutter apps (auto-generated accessibility nodes, no resourceId)
- React Native apps (limited accessibility mapping)
- Unity apps (custom accessibility bridge)
- Dynamic lists with identical item types

---

## 3. Disambiguation: DIS Protocol

### 3.1 The Problem

```
Screen has two "Play" buttons:
  - One in the header (plays current playlist)
  - One in a list item (plays a specific song)

Both generate: AVID = BTN:d4e5f6a7 (same hash)
User says: "click play" → WHICH one?
```

### 3.2 Hybrid 3-Layer Disambiguation

Three complementary methods resolve collisions:

**Layer 1: Hierarchy Path (`h=`)**
Structural position in the view tree. Device-independent because the view hierarchy
is determined by app code, not screen dimensions.

```
h=LL[0]/FL[1]/BTN

Meaning:
  LinearLayout (1st child of root)
    → FrameLayout (2nd child)
      → Button (the target)

Abbreviations:
  RV = RecyclerView
  LL = LinearLayout
  FL = FrameLayout
  CL = ConstraintLayout
  RL = RelativeLayout
  CV = ComposeView
  VW = View (generic)
  SV = ScrollView
  NV = NestedScrollView
```

**Layer 2: Semantic Zone (`z=`)**
Spatial region based on element position relative to screen dimensions.
Uses percentages, not pixels, so it's device-independent.

```
z=header     Top 15% of screen
z=nav        Bottom 10% of screen
z=content    Middle area (everything between header and nav)
z=sidebar    Left or right 20%
z=overlay    Floating/dialog (detected by window type)
```

Detection logic:
```kotlin
fun detectZone(bounds: Bounds, screenWidth: Int, screenHeight: Int): String {
    val centerY = (bounds.top + bounds.bottom) / 2f
    val relativeY = centerY / screenHeight
    return when {
        relativeY < 0.15f -> "header"
        relativeY > 0.90f -> "nav"
        else -> "content"
    }
}
```

**Layer 3: Parent-Extended Hash (`p=`)**
A hash that includes parent context for uniqueness. Different from AVID because
it incorporates positional information within the parent.

```
p = hash(parentClassName + parentResourceId + childIndex + siblingCount)
p = hash("LinearLayout" + "com.app:id/header" + "2" + "5") = "c7d8e9f0"
```

### 3.3 Resolution Order (Runtime)

```
User says "click play"
    ↓
1. Find all elements with phrase "play" in current commands
    ↓ (2 matches: BTN:d4e5f6a7 in header, BTN:d4e5f6a7 in list)
2. Check loaded .VOS profile for DIS data
    ↓
3. Filter by hierarchy path: header play = h=LL[0]/FL[1], list play = h=RV[0]/LL[3]
    ↓
4. If still ambiguous: filter by semantic zone (header vs content)
    ↓
5. If still ambiguous: filter by parent-extended hash
    ↓
6. If STILL ambiguous: numbers overlay ("play 1" or "play 2")
```

---

## 4. .VOS Voice Profile Format

### 4.1 Overview

`.VOS` (VoiceOS Seed) files use the AVU compact wire protocol with a YAML header.
They define voice commands for a specific app, portable across all Android devices.

### 4.2 File Structure

```
---                                    ← YAML header start
schema: avu-vos-1.0                    ← format version
version: 1.0.0                        ← profile version
locale: en-US                          ← language/locale
app: com.spotify.music                 ← target app package
app_version: 8.9.x                     ← compatible app version range
source: scan                           ← how profile was created
generated: 2026-02-11                  ← creation date
element_count: 42                      ← total elements
metadata:
  display_name: Spotify                ← human-readable app name
  stability_score: 0.87               ← average element stability
  screens_covered: 6                   ← number of unique screens
---                                    ← header end, body start
CAT:home:Home Screen:Main navigation and playback controls
CAT:search:Search:Find music and podcasts
CAT:library:Library:Your saved music and playlists
ELM:BTN:a3f2e1c9:shuffle play:CLICK:home:0.95
ELM:BTN:b4e3d2a1:play:CLICK:home:0.92
ELM:BTN:c5d4e3f2:search:CLICK:home:0.98
ELM:TXT:d6e5f4a3:liked songs:CLICK:library:0.90
ELM:INP:e7f6a5b4:search bar:TYPE:search:0.99
DIS:BTN:b4e3d2a1:h=LL[0]/FL[1]:z=content:p=c7d8e9f0
---                                    ← synonyms section
SYN:shuffle play:[shuffle,play random,random play]
SYN:play:[resume,start,start playing]
SYN:search:[find,look for,search for]
```

### 4.3 Wire Protocol Codes

| Code | Format | Description |
|------|--------|-------------|
| `CAT` | `CAT:id:name:description` | Screen/category definition |
| `ELM` | `ELM:typeCode:hash:phrase:action:screen:confidence` | Element voice command |
| `DIS` | `DIS:typeCode:hash:h=path:z=zone:p=parentHash` | Disambiguation metadata |
| `SYN` | `SYN:phrase:[alt1,alt2,...]` | Synonym/alternate phrases |
| `ACT` | `ACT:phrase:actionType:targetAvid` | Action mapping override |
| `IGN` | `IGN:typeCode:hash:reason` | Explicitly ignored element |

### 4.4 ELM Field Breakdown

```
ELM:BTN:a3f2e1c9:shuffle play:CLICK:home:0.95
     │   │        │             │     │    │
     │   │        │             │     │    └── confidence (0.0-1.0)
     │   │        │             │     └── screen/category ID
     │   │        │             └── action type
     │   │        └── voice phrase
     │   └── 8-char hex hash (device-independent)
     └── 3-char type code
```

**Action types:** `CLICK`, `TYPE`, `SCROLL`, `LONG_PRESS`, `DOUBLE_CLICK`, `TOGGLE`

**Confidence scale:**
| Range | Meaning |
|-------|---------|
| 0.95-1.0 | Very stable (static UI, always present) |
| 0.80-0.94 | Stable (appears on most visits) |
| 0.60-0.79 | Moderate (conditional visibility) |
| 0.40-0.59 | Low (dynamic content) |
| < 0.40 | Volatile (not suitable for profiles) |

### 4.5 DIS Field Breakdown

```
DIS:BTN:b4e3d2a1:h=LL[0]/FL[1]:z=content:p=c7d8e9f0
     │   │        │              │         │
     │   │        │              │         └── p= parent-extended hash (8-char hex)
     │   │        │              └── z= semantic zone (header/nav/content/sidebar/overlay)
     │   │        └── h= hierarchy path (abbreviated parent chain)
     │   └── element hash (references an ELM line)
     └── type code
```

DIS lines are ONLY emitted for elements with AVID collisions. For unique AVIDs,
no DIS line is needed. This keeps profiles compact.

### 4.6 No Bounds in Profiles

**Critical design decision:** .VOS profiles contain NO absolute pixel bounds.

**Why:**
- Bounds are device-specific (screen resolution, DPI)
- Rotation changes all bounds
- Foldable devices have multiple configurations
- Split-screen mode changes available area

**How targeting works without bounds:**
1. Match user speech → find `ELM` in profile → get AVID
2. `BoundsResolver` finds element in LIVE accessibility tree:
   - Layer 1: `findAccessibilityNodeInfosByViewId(resourceId)` (identity)
   - Layer 2: Full tree search by text/contentDescription/className
   - Layer 3: DIS disambiguation if needed
3. Use LIVE bounds from the found node for gesture dispatch
4. Works on ANY device, ANY orientation, ANY DPI

---

## 5. The Label Normalizer

### 5.1 Two-Layer Architecture

`CommandGenerator.normalizeRealWearMlScript()` processes raw accessibility labels
through two layers:

```
Raw label from accessibility tree
    ↓
Layer 1: (Voice: ...) extraction
    Regex: \(Voice:\s*(.+?)\)\s*$
    "Back (Voice: go back)" → "go back"
    ↓ (no match)
Layer 2: Delimiter splitting + guard
    Delimiters: [":", "|", ",", "."] (order matters)
    "Settings: Volume" → "Settings"
    "hf_btn:Go Back" → "Go Back" (hf_ → take part after delimiter)
    Guard: if result < 2 chars → return original (protects "3:45 PM")
    ↓ (no delimiter)
Return unchanged
```

### 5.2 Layer 1: Voice Hint Extraction (Tier 2)

```kotlin
private val VOICE_HINT_PATTERN = Regex("\\(Voice:\\s*(.+?)\\)\\s*$")

// Examples:
// "Back (Voice: go back)" → "go back"
// "Shuffle (Voice: shuffle play)" → "shuffle play"
// "Settings" → no match, falls through to Layer 2
```

### 5.3 Layer 2: Delimiter Splitting (Tier 3)

```kotlin
private val PARSE_DESCRIPTION_DELIMITERS = listOf(":", "|", ",", ".")

// Normal case:
// "Settings: Volume" → splits on ":" → ["Settings", " Volume"] → "Settings"
// "hf_btn:Go Back" → has "hf_" → takes parts[1] → "Go Back"

// Guard case:
// "3:45 PM" → splits on ":" → ["3", "45 PM"] → "3" is < 2 chars → return "3:45 PM"
```

The guard prevents aggressive splitting from breaking time formats, version numbers,
and similar patterns in third-party app labels.

---

## 6. Rotation & Display Size Handling

### 6.1 Why It's Not a Problem

**AVID hash** = `hash(className + resourceId + text + contentDescription)`
- NO bounds in the hash
- Same AVID on Pixel 7, Galaxy Fold, tablet, portrait, landscape

**BoundsResolver** = finds elements by IDENTITY, not position
- Layer 1: `findAccessibilityNodeInfosByViewId(resourceId)` — resourceId is universal
- Layer 2: Scroll delta compensation
- Layer 3: Full tree search by label/class
- Uses LIVE bounds from found node

**ScreenCacheManager** = detects rotation as a new screen
- New layout = new screen signature hash
- Triggers full re-scrape with fresh bounds

### 6.2 What Triggers Re-Resolution

| Event | Detection | Action |
|-------|-----------|--------|
| Screen rotation | ScreenCacheManager hash change | Full re-scrape |
| App resize (foldable) | ScreenCacheManager hash change | Full re-scrape |
| Scroll | AccessibilityEvent TYPE_VIEW_SCROLLED | Delta compensation in BoundsResolver |
| Navigation | Window state change event | Full re-scrape |
| Content change | AccessibilityEvent TYPE_WINDOW_CONTENT_CHANGED | Incremental update |

---

## 7. Scanning Utility: "VoiceOS App Trainer"

### 7.1 Concept

A built-in mode in the Avanues app that lets users train voice commands for installed apps.

### 7.2 Workflow

```
User opens "App Trainer" in Avanues settings
    ↓
Selects an installed app (e.g., Spotify)
    ↓
App Trainer launches the target app via Intent
    ↓
AccessibilityService monitors + scrapes each screen automatically
    ↓
User navigates through key screens (home, search, player, library)
    ↓
PersistenceDecisionEngine tracks element stability per screen
    ↓
User taps "Finish Training" when done
    ↓
VosProfileExporter generates .VOS file
    ↓
Profile loaded automatically when target app is foregrounded
```

### 7.3 Passive Learning (Always Active)

The system already passively learns stable elements as users navigate:

| Component | Status | Role |
|-----------|--------|------|
| `AndroidScreenExtractor` | EXISTS | Scrapes accessibility tree |
| `AccessibilityNodeAdapter` | EXISTS | Maps nodes to ElementInfo |
| `CommandGenerator` | EXISTS | Generates QuantizedCommands |
| `PersistenceDecisionEngine` | EXISTS | 4-layer persistence decision |
| `ScreenCacheManager` | EXISTS | Detects screen changes |
| `ElementFingerprint` | EXISTS | Generates stable AVIDs |
| `ScrapedElement` table | EXISTS | Stores elements in DB |
| `GeneratedCommand` table | EXISTS | Stores commands in DB |

**What needs to be built:**

| Component | Purpose | Effort |
|-----------|---------|--------|
| `VosProfileExporter` | Export persisted commands to .VOS format | Small |
| `VosProfileLoader` | Load .VOS file → inject into command registry | Small |
| `AppTrainerMode` | UI for guided app scanning | Medium |
| `StabilityTracker` | Track element confidence over sessions | Small |
| `ProfileSharingService` | Upload/download community profiles | Large (future) |

### 7.4 Profile Sources

| Source | Description | Confidence |
|--------|------------|------------|
| `scan` | App Trainer guided scan | High (0.85+) |
| `passive` | Auto-learned from usage | Medium (0.70+) |
| `developer` | Third-party dev authored | High (0.90+) |
| `community` | Shared via cloud | Varies |
| `curated` | Verified by Augmentalis team | Highest (0.95+) |

---

## 8. Existing Infrastructure Map

### 8.1 Database Tables (Already Exist)

| Table | Module | Purpose |
|-------|--------|---------|
| `ScrapedApp` | Database | App metadata (package, name, version) |
| `ScrapedElement` | Database | UI elements with AVIDs |
| `ScrapedHierarchy` | Database | View hierarchy structure |
| `ScrapedWebElement` | Database | DOM elements from WebView |
| `ScreenContext` | Database | Screen signatures and metadata |
| `GeneratedCommand` | Database | Voice commands from elements |
| `ScreenTransition` | Database | Navigation graph between screens |
| `UserInteraction` | Database | User action history |

### 8.2 Code Path: Speech → Action

```
User speaks
    ↓
STT Engine (Vivoka/Whisper/Google) → raw text
    ↓
NLU Processing → intent + entities
    ↓
SynonymRegistry.canonicalize(verb) → canonical verb
    ↓
ActionCoordinator.processCommand(quantizedCommand)
    ↓
Handler selection (priority order):
  1. WebCommandHandler (if source="web")
  2. AndroidGestureHandler (gesture commands)
  3. SystemHandler (global actions)
  4. AppHandler (app launch)
  5. AndroidCursorHandler (cursor commands)
    ↓
BoundsResolver.resolve(command) → live bounds
    ↓
AndroidGestureDispatcher.tap/click/scroll/fling/pinch/drag(bounds)
    ↓
AccessibilityService.dispatchGesture(gestureDescription)
```

### 8.3 Handler Registry

| Handler | Category | Actions |
|---------|----------|---------|
| WebCommandHandler | BROWSER, WEB_GESTURE, TEXT | JS injection via DOMScraperBridge |
| AndroidGestureHandler | All gesture-capable | tap, scroll, fling, pinch, drag, doubleTap |
| SystemHandler | SYSTEM | performGlobalAction (back, home, recents, notifications) |
| AppHandler | APP | Launch via Intent |
| AndroidCursorHandler | GAZE | CursorOverlayService start/stop/click |
| VoiceControlHandler | SYSTEM | mute/wake voice, dictation, numbers on/off/auto |
| HelpScreenHandler | ACCESSIBILITY | Show help UI |

> **CursorOverlayService coordinate system**: The cursor overlay window MUST use
> `FLAG_LAYOUT_IN_SCREEN` so its y=0 matches `dispatchGesture()` absolute screen
> coordinates. Without this flag, clicks land `statusBarHeight` pixels above the
> cursor visual position (overlay y=0 defaults to below the status bar).

---

## 9. Overlay-Aligned Numeric Command System

### 9.1 Overview

Starting with commit `2cfc0391`, overlay badge numbers are the single source of truth for all
numeric voice commands. Before this change, `OverlayItemGenerator` and `generateNumericCommands()`
maintained independent counters that could diverge when elements were filtered differently. The
result was that badge "5" might activate what VoiceOS called ordinal "2".

The fix makes badge number assignment authoritative: the overlay drives the commands, not the other way around.

### 9.2 Source Key: `overlay_numbers`

`CommandRegistry` now uses a dedicated source key `"overlay_numbers"` for badge-derived numeric commands. This keeps them separate from DOM-scraped commands (`"web"`), static VOS commands (`"web_static"`), and label-based dynamic commands so they can be cleared and rebuilt independently on each screen change.

```
CommandRegistry sources (partial list):
  "static"          — VOS seed commands loaded from DB
  "web"             — DOM-scraped web element commands
  "web_static"      — static BROWSER/WEB_GESTURE commands (browser-scoped)
  "overlay_numbers" — badge-derived numeric commands (NEW)
```

### 9.3 Three Command Forms Per Badge

For every badge number N assigned by `OverlayItemGenerator`, three voice command phrases are registered:

| Form | Example (N=3) | Pattern |
|------|--------------|---------|
| Digit | "3" | `N.toString()` |
| Word | "three" | `NumberWords.forIndex(N)` |
| Ordinal | "third" | `OrdinalWords.forIndex(N)` |

All three phrases map to the same `QuantizedCommand` (same AVID, same action). If a user says any of the three, the correct element is activated.

`NumberWords` and `OrdinalWords` cover 1-20 explicitly and fall back to `"{N}th"` for higher numbers.

### 9.4 Data Flow

```
OverlayItemGenerator.assignBadgeNumbers(elements)
    ↓
For each element with listIndex >= 0:
    overlayItem.badgeNumber = sequentialN
    ↓
CommandRegistry.update("overlay_numbers", overlayCommands)
    ↓
generateNumericCommands() reads from CommandRegistry["overlay_numbers"]
    → registers "3", "three", "third" → QuantizedCommand(avid=overlayItem.avid)
```

The overlay render and the voice grammar share the same badge number because they both read from `OverlayItem.badgeNumber` — no separate counter exists anywhere in the pipeline.

### 9.5 Overlay Mode Behaviour

`NumbersOverlayMode` controls both badge generation and visibility for ALL app types.

| Mode | Badges generated | Badges visible | Applies to |
|------|-----------------|---------------|------------|
| `OFF` | Never | Never | All apps (target + non-target) |
| `ON` | Always | Always | All apps |
| `AUTO` | Always | Always | All apps |

**Important changes:**

1. **Target app mode enforcement (260219):** `DynamicCommandGenerator.processScreen()` now checks
   `NumbersOverlayMode` BEFORE the target/non-target app split. Previously, target apps (Gmail,
   WhatsApp, etc.) bypassed the mode check and always generated overlay items — "hide numbers"
   had no effect on list-based apps.

2. **AUTO mode unification (260215):** `AUTO` shows overlay badges on ALL apps, not just target
   apps. Since badge-based commands are generated for all apps with indexable elements, showing
   the badges consistently is required for the system to be usable.

3. **Immediate re-scan on mode change (260219):** When mode changes via voice command
   ("show numbers", "hide numbers", "numbers auto"), the accessibility service invalidates the
   screen hash and triggers an immediate `refreshOverlayBadges()`. This ensures badges appear
   or disappear immediately, without waiting for the next accessibility event.

---

## 10. Adding Voice Support to New UI Components

### 10.1 For Our Apps (Tier 1 + Tier 2)

1. Use `IconButton` (not `Box + clickable`) for proper accessibility semantics
2. Set clean `contentDescription` with optional `(Voice: ...)` hint:
   ```kotlin
   Icon(
       imageVector = Icons.Default.Settings,
       contentDescription = "Settings (Voice: open settings)"
   )
   ```
3. For `LabeledNavButton`, use the `voiceHint` parameter:
   ```kotlin
   LabeledNavButton(
       icon = Icons.Default.History,
       label = "History",
       voiceHint = "open history"
   )
   ```
4. AVID is automatically assigned by `ElementFingerprint.fromElementInfo()`

### 10.2 For Third-Party Developers (Tier 2)

Tell them to add `(Voice: phrase)` to `contentDescription`. That's it.
No SDK, no dependency, no build changes. Works in any framework.

### 10.3 For Third-Party Apps Without Cooperation (Tier 3 + 4)

Nothing to do — the system works automatically via accessibility tree scraping.
To improve quality, use the App Trainer to create a .VOS profile.

---

## 11. Quick Reference: Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| AVID hash inputs | className + resourceId + text + contentDescription | Device-independent identity |
| AVID hash inputs exclude | bounds, position, DPI | Portability across devices |
| .VOS format | AVU compact wire protocol | 70% smaller than JSON, aligned with AVU ecosystem |
| .VOS bounds | NOT included | Profiles must be device-portable |
| Label normalizer | Two-layer (Voice hint + delimiter) | Tier 2 + Tier 3 compatibility |
| Delimiter guard | Skip if result < 2 chars | Protects time formats in third-party labels |
| Disambiguation | 3-layer hybrid (hierarchy + zone + parent hash) | Handles Flutter/RN/Unity apps without resourceId |
| Button implementation | IconButton over Box+clickable | Proper accessibility role semantics |

---

*Chapter 94 | 4-Tier Voice Enablement Architecture*
*Created: 2026-02-11 | Updated: 2026-02-19 (overlay-aligned numeric command system, AUTO mode badge rendering, Section 9 added)*
*Related: Chapter 93 (Voice Command Pipeline), Chapter 03 (VoiceOSCore Deep Dive)*
