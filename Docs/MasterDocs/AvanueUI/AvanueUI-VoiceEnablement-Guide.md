# AvanueUI Voice Enablement Guide

## AI Context Brief

This document explains how VoiceOS makes ANY Android app voice-controllable and
provides framework-specific integration examples. This is the practical companion
to Developer Manual Chapter 94 (4-Tier Voice Enablement Architecture).

**Audience:** AI assistants, developers building voice-enabled Android apps,
third-party developers integrating with VoiceOS.

---

## 1. How VoiceOS Sees Your App

VoiceOS uses Android's AccessibilityService to observe the UI of ANY running app.
Every interactive element on screen becomes a potential voice command target.

**What VoiceOS reads from each element:**

| Property | Source | Used For |
|----------|--------|----------|
| `text` | AccessibilityNodeInfo.getText() | Primary voice label |
| `contentDescription` | AccessibilityNodeInfo.getContentDescription() | Secondary label + Voice hint |
| `resourceId` | AccessibilityNodeInfo.getViewIdResourceName() | AVID hash + BoundsResolver anchor |
| `className` | AccessibilityNodeInfo.getClassName() | Type code (BTN, TXT, INP) |
| `isClickable` | AccessibilityNodeInfo.isClickable() | Actionability filter |
| `isScrollable` | AccessibilityNodeInfo.isScrollable() | Actionability filter |
| `bounds` | AccessibilityNodeInfo.getBoundsInScreen() | Live click targeting |

**Label priority:** `text` > `contentDescription` > `resourceId` (stripped, humanized)

If an element has no text, no contentDescription, and no resourceId, VoiceOS skips it.

---

## 2. Your Architectural Choices (Summary)

### Choice 1: 4-Tier Hybrid Voice Enablement

**What:** Four independent tiers of voice enablement coexist simultaneously.

**Why it works:** Each tier handles a different level of app integration, from full
SDK control to zero cooperation. They never conflict because priority resolution
is deterministic:

```
Tier 1 (Static commands)     → Highest priority, always checked first
Tier 4 (Voice profiles)      → Pre-verified, high confidence
Tier 2 (Developer hints)     → Explicit phrases from contentDescription
Tier 3 (Auto-scraping)       → Fallback, works on anything
```

A single voice command flows through all 4 tiers in order. The first match wins.
If Tier 1 has "go back" as a static command, it executes system back — even if
Tier 3 also found a "Back" button on screen. No duplication, no conflict.

### Choice 2: `(Voice: ...)` Convention (Tier 2)

**What:** Third-party developers embed explicit voice phrases in contentDescription.

**Why it works:**
- **Zero dependency** — no SDK, no library import, no build changes
- **Framework-agnostic** — works in Compose, Flutter, RN, Unity, native XML
- **Backwards-compatible** — screen readers (TalkBack) read the full string naturally
- **Discoverable** — VoiceOS normalizer Layer 1 extracts it via regex
- **Entry point** — opens door to AvanueAI developer ecosystem tools

The convention leverages the one universal property ALL Android UI frameworks
expose: `contentDescription` on `AccessibilityNodeInfo`. Every framework maps
its accessibility labels to this Android system property.

### Choice 3: Device-Independent AVID

**What:** Element identifiers are hash-based, NOT position-based.

**Why it works:**
- AVID = `hash(className + resourceId + text + contentDescription)`
- Same button → same AVID on Pixel 7, Galaxy Fold, tablet, portrait, landscape
- `BoundsResolver` finds elements by IDENTITY in the live tree, uses LIVE bounds
- Profiles (.VOS) are portable across all devices without modification

### Choice 4: 3-Layer Hybrid Disambiguation

**What:** Hierarchy path + semantic zone + parent hash resolve AVID collisions.

**Why it works:**
- **Hierarchy path** (`h=RV[0]/LL[2]`): structural position is app-version-specific,
  not device-specific. Works because the view tree is built by app code.
- **Semantic zone** (`z=content`): uses percentage-based screen regions (top 15% = header),
  not pixel coordinates. Device-independent.
- **Parent hash** (`p=c7d8e9f0`): includes parent context in the hash, making
  otherwise-identical elements distinguishable.

Combined, they handle the worst case (Flutter/Unity with no resourceId and
duplicate labels) while being lightweight for the common case (unique resourceId).

### Choice 5: No Bounds in .VOS Profiles

**What:** Voice profiles store AVID + phrase + screen, NOT pixel coordinates.

**Why it works:**
- Profiles created on Pixel 7 work on Galaxy Fold without modification
- Rotation doesn't invalidate the profile
- `BoundsResolver` always uses LIVE bounds from the accessibility tree
- The AVID is the stable anchor; bounds are resolved at execution time

---

## 3. Framework-Specific Examples

### 3.1 Android (Jetpack Compose)

**How VoiceOS sees Compose elements:**
Compose generates `AccessibilityNodeInfo` via the semantics system. `Modifier.clickable`
sets `isClickable = true`. `contentDescription` maps to the node's content description.

**Without Voice hint (Tier 3 auto-scraping):**
```kotlin
@Composable
fun MusicPlayer() {
    IconButton(onClick = { toggleShuffle() }) {
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = "Shuffle"  // VoiceOS generates: "click Shuffle"
        )
    }

    IconButton(onClick = { playPause() }) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play"  // VoiceOS generates: "click Play"
        )
    }

    IconButton(onClick = { skipNext() }) {
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next track"  // VoiceOS generates: "click Next track"
        )
    }
}
```

User says: "click Shuffle" → VoiceOS finds element with label "Shuffle" → dispatches tap.

**With Voice hint (Tier 2 developer convention):**
```kotlin
@Composable
fun MusicPlayer() {
    IconButton(onClick = { toggleShuffle() }) {
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = "Shuffle (Voice: shuffle play)"
            // VoiceOS extracts: "shuffle play"
            // User can say: "shuffle play" instead of "click Shuffle"
        )
    }

    IconButton(onClick = { playPause() }) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play (Voice: play music)"
            // VoiceOS extracts: "play music"
        )
    }

    IconButton(onClick = { skipNext() }) {
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next Track (Voice: next track)"
            // VoiceOS extracts: "next track"
        )
    }
}
```

User says: "shuffle play" → VoiceOS matches extracted phrase → dispatches tap.

**AVID generated for each:**
```
Shuffle → BTN:a1b2c3d4 (hash of "IconButton" + "" + "" + "Shuffle (Voice: shuffle play)")
Play    → BTN:e5f6a7b8 (hash of "IconButton" + "" + "" + "Play (Voice: play music)")
Next    → BTN:c9d0e1f2 (hash of "IconButton" + "" + "" + "Next Track (Voice: next track)")
```

**Key Compose rules:**
- Always use `IconButton` instead of `Box + Modifier.clickable` for buttons
- `IconButton` automatically exposes `role = Button` in the accessibility tree
- `Box + clickable` exposes `isClickable = true` but not the button role
- Both work for VoiceOS, but `IconButton` provides better TalkBack support

### 3.2 Flutter

**How VoiceOS sees Flutter elements:**
Flutter generates a virtual accessibility tree via its semantics system.
Flutter elements typically have NO `resourceId` (unlike native Android).
The `Semantics` widget's `label` maps to `contentDescription`.

**Without Voice hint (Tier 3 auto-scraping):**
```dart
class MusicPlayer extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Semantics(
          label: 'Shuffle',  // VoiceOS generates: "click Shuffle"
          child: IconButton(
            icon: Icon(Icons.shuffle),
            onPressed: () => toggleShuffle(),
          ),
        ),
        Semantics(
          label: 'Play',  // VoiceOS generates: "click Play"
          child: IconButton(
            icon: Icon(Icons.play_arrow),
            onPressed: () => playPause(),
          ),
        ),
        Semantics(
          label: 'Next track',  // VoiceOS generates: "click Next track"
          child: IconButton(
            icon: Icon(Icons.skip_next),
            onPressed: () => skipNext(),
          ),
        ),
      ],
    );
  }
}
```

**With Voice hint (Tier 2 developer convention):**
```dart
class MusicPlayer extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Semantics(
          label: 'Shuffle (Voice: shuffle play)',
          // VoiceOS extracts: "shuffle play"
          child: IconButton(
            icon: Icon(Icons.shuffle),
            onPressed: () => toggleShuffle(),
          ),
        ),
        Semantics(
          label: 'Play (Voice: play music)',
          // VoiceOS extracts: "play music"
          child: IconButton(
            icon: Icon(Icons.play_arrow),
            onPressed: () => playPause(),
          ),
        ),
        Semantics(
          label: 'Next track (Voice: next track)',
          // VoiceOS extracts: "next track"
          child: IconButton(
            icon: Icon(Icons.skip_next),
            onPressed: () => skipNext(),
          ),
        ),
      ],
    );
  }
}
```

**Flutter-specific considerations:**
- Flutter elements have NO `resourceId` → AVID hash based on className + contentDescription only
- If two buttons have the same label → AVID collision → DIS disambiguation needed
- Flutter generates virtual class names like `SemanticsNode` → type code detection uses action flags
- Flutter's `ExcludeSemantics` widget HIDES elements from VoiceOS (and TalkBack)
- Solution: Always wrap interactive widgets with `Semantics(label: ...)` for VoiceOS discoverability

**AVID for Flutter elements (no resourceId):**
```
Shuffle → BTN:f1e2d3c4 (hash of "SemanticsNode" + "" + "" + "Shuffle (Voice: shuffle play)")
Play    → BTN:b5a6c7d8 (hash of "SemanticsNode" + "" + "" + "Play (Voice: play music)")
```

### 3.3 React Native

**How VoiceOS sees React Native elements:**
React Native maps JavaScript accessibility props to Android's `AccessibilityNodeInfo`.
`accessibilityLabel` → `contentDescription`. `accessibilityRole` → helps type detection.
Some RN elements get `resourceId` from `testID` prop (if configured).

**Without Voice hint (Tier 3 auto-scraping):**
```jsx
function MusicPlayer() {
  return (
    <View style={styles.row}>
      <TouchableOpacity
        onPress={toggleShuffle}
        accessibilityLabel="Shuffle"
        accessibilityRole="button"
        // VoiceOS generates: "click Shuffle"
      >
        <ShuffleIcon />
      </TouchableOpacity>

      <TouchableOpacity
        onPress={playPause}
        accessibilityLabel="Play"
        accessibilityRole="button"
        // VoiceOS generates: "click Play"
      >
        <PlayIcon />
      </TouchableOpacity>

      <TouchableOpacity
        onPress={skipNext}
        accessibilityLabel="Next track"
        accessibilityRole="button"
        // VoiceOS generates: "click Next track"
      >
        <SkipNextIcon />
      </TouchableOpacity>
    </View>
  );
}
```

**With Voice hint (Tier 2 developer convention):**
```jsx
function MusicPlayer() {
  return (
    <View style={styles.row}>
      <TouchableOpacity
        onPress={toggleShuffle}
        accessibilityLabel="Shuffle (Voice: shuffle play)"
        accessibilityRole="button"
        testID="shuffle_btn"  // Becomes resourceId → stronger AVID
        // VoiceOS extracts: "shuffle play"
      >
        <ShuffleIcon />
      </TouchableOpacity>

      <TouchableOpacity
        onPress={playPause}
        accessibilityLabel="Play (Voice: play music)"
        accessibilityRole="button"
        testID="play_btn"
        // VoiceOS extracts: "play music"
      >
        <PlayIcon />
      </TouchableOpacity>

      <TouchableOpacity
        onPress={skipNext}
        accessibilityLabel="Next track (Voice: next track)"
        accessibilityRole="button"
        testID="next_btn"
        // VoiceOS extracts: "next track"
      >
        <SkipNextIcon />
      </TouchableOpacity>
    </View>
  );
}
```

**React Native-specific considerations:**
- `testID` maps to `resourceId` on Android → use it for stronger AVID hashes
- Without `testID`, AVID is based on className + accessibilityLabel (weaker)
- `accessibilityRole="button"` helps VoiceOS assign correct type code (BTN)
- Pressable, TouchableOpacity, TouchableHighlight all set `isClickable = true`
- RN's `importantForAccessibility="no"` HIDES elements from VoiceOS

**AVID with testID:**
```
Shuffle → BTN:x1y2z3w4 (hash of "ReactButton" + "shuffle_btn" + "" + "Shuffle (Voice: ...)")
```
ResourceId-based hash is more stable across app updates than label-only hash.

### 3.4 Unity

**How VoiceOS sees Unity elements:**
Unity does NOT natively support Android accessibility. Most Unity apps are invisible
to VoiceOS's accessibility tree scanning. Integration requires one of:

**Option A: Unity Accessibility Plugin (Google)**
Google's [Android Accessibility for Unity](https://github.com/nicedoc/unity-accessibility-plugin)
plugin creates virtual accessibility nodes from Unity UI elements.

```csharp
using UnityEngine;
using UnityEngine.UI;

public class MusicPlayer : MonoBehaviour
{
    [SerializeField] private Button shuffleButton;
    [SerializeField] private Button playButton;
    [SerializeField] private Button nextButton;

    void Start()
    {
        // Option A: Google's accessibility plugin
        // Sets contentDescription on the generated AccessibilityNodeInfo

        shuffleButton.GetComponent<AccessibleButton>()
            .SetContentDescription("Shuffle (Voice: shuffle play)");

        playButton.GetComponent<AccessibleButton>()
            .SetContentDescription("Play (Voice: play music)");

        nextButton.GetComponent<AccessibleButton>()
            .SetContentDescription("Next track (Voice: next track)");
    }
}
```

**Option B: Android Native Overlay**
For Unity games, create a transparent Android native overlay with accessibility nodes
that map to Unity UI positions:

```csharp
// Unity C# side
public class VoiceOSBridge : MonoBehaviour
{
    // Send UI element positions to Android native layer
    void UpdateAccessibilityNodes()
    {
        AndroidJavaObject bridge = new AndroidJavaObject(
            "com.myapp.VoiceOSAccessibilityBridge"
        );

        bridge.Call("registerElement",
            "shuffle_btn",                           // resourceId
            "Shuffle (Voice: shuffle play)",          // contentDescription
            shuffleButton.transform.position.x,       // screen position
            shuffleButton.transform.position.y,
            shuffleButton.GetComponent<RectTransform>().rect.width,
            shuffleButton.GetComponent<RectTransform>().rect.height
        );
    }
}
```

```java
// Android Java side
public class VoiceOSAccessibilityBridge extends AccessibilityNodeProvider {
    // Creates virtual AccessibilityNodeInfo objects that VoiceOS can discover
    @Override
    public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
        AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
        node.setContentDescription(elements.get(virtualViewId).contentDescription);
        node.setClickable(true);
        node.setBoundsInScreen(elements.get(virtualViewId).bounds);
        return node;
    }
}
```

**Unity-specific considerations:**
- Unity is the HARDEST framework for VoiceOS integration (no native accessibility)
- Without an accessibility plugin, Unity apps are invisible to Tier 3
- The `(Voice: ...)` convention works through either Option A or B above
- For games, Tier 4 voice profiles are the most practical approach:
  manual creation of .VOS files that map voice commands to screen coordinates
  (game UIs are often static layouts with known positions)
- Unity's `testID` equivalent is setting the `name` property on GameObjects

---

## 4. How Each Tier Handles Each Framework

### Tier 3 (Auto-Scraping) — No developer cooperation needed

| Framework | Visibility | Label Source | AVID Quality | Notes |
|-----------|-----------|--------------|-------------|-------|
| Jetpack Compose | Full | text/contentDescription | High (resourceId available) | Best native support |
| Android XML | Full | text/contentDescription | High (resourceId from layout) | Traditional, well-supported |
| Flutter | Full | Semantics.label | Medium (no resourceId) | Needs DIS for duplicates |
| React Native | Full | accessibilityLabel | Medium-High (testID = resourceId) | Use testID for better AVID |
| Unity (no plugin) | NONE | N/A | N/A | Invisible without accessibility bridge |
| Unity (with plugin) | Partial | AccessibleButton.contentDescription | Medium | Depends on plugin coverage |

### Tier 2 (Developer Convention) — Developer adds `(Voice: ...)`

| Framework | Integration Point | Effort | Impact |
|-----------|------------------|--------|--------|
| Jetpack Compose | `contentDescription = "Label (Voice: phrase)"` | 1 line per element | Full voice control |
| Android XML | `android:contentDescription="Label (Voice: phrase)"` | 1 attribute per element | Full voice control |
| Flutter | `Semantics(label: 'Label (Voice: phrase)')` | 1 line per widget | Full voice control |
| React Native | `accessibilityLabel="Label (Voice: phrase)"` | 1 prop per component | Full voice control |
| Unity | `SetContentDescription("Label (Voice: phrase)")` | Plugin required first | Full voice control |

### Tier 4 (Voice Profiles) — Scanned or community-provided

| Framework | Scan Quality | Profile Portability | Disambiguation |
|-----------|-------------|-------------------|----------------|
| Jetpack Compose | Excellent | Fully portable (resourceId in AVID) | Rarely needed |
| Android XML | Excellent | Fully portable (resourceId in AVID) | Rarely needed |
| Flutter | Good | Portable (hierarchy path for DIS) | Sometimes needed |
| React Native | Good-Excellent | Portable (testID in AVID if set) | Sometimes needed |
| Unity | Poor-Good | Depends on accessibility bridge | Often needed |

---

## 5. The (Voice: ...) Pattern — Complete Specification

### Format
```
contentDescription = "Human-Readable Label (Voice: voice command phrase)"
```

### Regex (used by CommandGenerator Layer 1)
```
\(Voice:\s*(.+?)\)\s*$
```

### Rules
1. `(Voice: ...)` MUST be at the END of the string
2. Parentheses and "Voice:" are REQUIRED (case-sensitive "V")
3. The phrase inside is extracted as-is, whitespace-trimmed
4. The label BEFORE the pattern is what screen readers (TalkBack) announce
5. If absent, VoiceOS auto-derives the phrase from the label (Tier 3)
6. Multiple words are supported: `(Voice: open browser settings)`
7. Special characters in the phrase should be avoided (keep to alphanumeric + spaces)

### What DOESN'T work
```
// WRONG: (Voice:) must be at the end
contentDescription = "(Voice: go back) Back Button"

// WRONG: Missing "Voice:" keyword
contentDescription = "Back (go back)"

// WRONG: Case mismatch (lowercase "voice")
contentDescription = "Back (voice: go back)"

// WRONG: Square brackets instead of parentheses
contentDescription = "Back [Voice: go back]"
```

### What DOES work
```
// Standard usage
contentDescription = "Back (Voice: go back)"                    → "go back"

// Multi-word phrase
contentDescription = "Settings (Voice: open browser settings)"  → "open browser settings"

// Long display label
contentDescription = "Navigate to previous page (Voice: go back)" → "go back"

// Trailing whitespace OK
contentDescription = "Back (Voice: go back)  "                  → "go back"
```

---

## 6. AVID in AvanueUI Components

### How AvanueUI Components Generate AVIDs

AvanueUI unified components (`AvanueButton`, `AvanueCard`, `AvanueIconButton`, etc.)
are Compose components. They automatically participate in the accessibility tree
through standard Compose semantics.

**AVID is NOT set by AvanueUI components.** It's generated by `ElementFingerprint`
when VoiceOS scrapes the accessibility tree. The component just needs to be
properly accessible:

```kotlin
// AvanueButton — automatically accessible via Compose Button semantics
AvanueButton(
    onClick = { /* ... */ },
    text = "Submit"  // This becomes the voice label: "click Submit"
)

// AvanueIconButton — wrap with contentDescription for voice hint
AvanueIconButton(
    onClick = { /* ... */ }
) {
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Refresh (Voice: refresh page)"
    )
}
```

### Voice-Enabling AvanueUI Screens

When building screens with AvanueUI components, ensure:

1. **Every interactive element has text or contentDescription**
   - Buttons with text: automatically voice-enabled via text
   - Icon-only buttons: MUST have contentDescription

2. **Use `(Voice: ...)` for non-obvious commands**
   - "Refresh" icon → user might say "reload" → add `(Voice: refresh page)`
   - "X" close icon → user might say "close" → add `(Voice: close)`

3. **Use `IconButton` not `Box + clickable`**
   - `IconButton` exposes `role = Button` in accessibility tree
   - `Box + clickable` exposes `isClickable` but may lack proper role

4. **Avoid hiding elements from accessibility**
   - Don't use `Modifier.clearAndSetSemantics {}` on interactive elements
   - Don't wrap interactive elements in `Box` with `semantics { invisibleToUser() }`

---

## 7. Testing Voice Enablement

### Manual Testing
1. Enable TalkBack on device
2. Navigate to your screen
3. Tap each element — TalkBack should announce the label
4. If TalkBack can read it, VoiceOS can voice-enable it

### Programmatic Testing (Compose)
```kotlin
@Test
fun shuffleButton_hasVoiceHint() {
    composeTestRule.setContent { MusicPlayer() }
    composeTestRule
        .onNodeWithContentDescription("Shuffle (Voice: shuffle play)")
        .assertExists()
        .assertIsEnabled()
}
```

### Accessibility Scanner
Use Android's Accessibility Scanner app to verify:
- All interactive elements have labels
- No duplicate labels on the same screen (or DIS will be needed)
- Touch targets are at least 48dp

---

*AvanueUI Voice Enablement Guide*
*Author: VOS4 Development Team | Created: 2026-02-11*
*Related: Developer Manual Chapter 94 (4-Tier Architecture), Chapter 93 (Pipeline)*
