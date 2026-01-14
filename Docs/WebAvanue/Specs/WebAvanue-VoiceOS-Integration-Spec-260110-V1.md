# WebAvanue + VoiceOS Integration Specification

**Version:** 1.0
**Date:** 2026-01-10
**Status:** Draft

---

## Overview

### What is WebAvanue?
WebAvanue is Augmentalis's custom web browser built on Android WebView with enhanced features:
- Custom navigation
- Ad blocking
- Privacy features
- WebXR support
- File downloads

### What is VoiceOS?
VoiceOS is Augmentalis's voice-first accessibility system that allows users to control apps using voice commands.

### Integration Goal
Enable users to navigate and interact with ANY website using voice commands through WebAvanue.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER                                     │
│                    "Click sign in"                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPEECH RECOGNITION                            │
│              (Vivoka / Android / On-device)                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   VOICE COMMAND MATCHER                          │
│                  (VoiceCommandGenerator)                         │
│                                                                  │
│  Input: "sign in"                                                │
│  Process:                                                        │
│    1. Tokenize: ["sign", "in"]                                   │
│    2. Match against scraped elements                             │
│    3. Find: element with name "Sign in" → match!                 │
│  Output: WebVoiceCommand { selector: "#sign-in-btn", ... }       │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┴────────────────────┐
         │ Multiple matches?                        │
         ▼                                          ▼
┌─────────────────────┐                  ┌─────────────────────┐
│    SINGLE MATCH     │                  │   MULTIPLE MATCHES  │
│    Execute action   │                  │   NLU Disambiguation│
└─────────────────────┘                  └─────────────────────┘
         │                                          │
         │                                          ▼
         │                               ┌─────────────────────┐
         │                               │ "Did you mean:      │
         │                               │  1. Sign in button  │
         │                               │  2. Sign in link"   │
         │                               └─────────────────────┘
         │                                          │
         └────────────────────┬────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                 WEBAVANUE VOICE BRIDGE                           │
│              (WebAvanueVoiceOSBridge.kt)                         │
│                                                                  │
│  Takes: WebVoiceCommand                                          │
│  Executes: JavaScript in WebView                                 │
│    webView.evaluateJavascript(clickScript(selector))             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        WEBVIEW                                   │
│                                                                  │
│  JavaScript executes:                                            │
│    document.querySelector('#sign-in-btn').click()                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     WEBSITE RESPONDS                             │
│                  (Login dialog opens)                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Components Created

### 1. DOMScraperBridge.kt (commonMain)
**Purpose:** JavaScript code that scrapes all interactive elements from a webpage.

**What it captures:**
- Links (`<a>`)
- Buttons (`<button>`, `role="button"`)
- Form inputs (`<input>`, `<textarea>`, `<select>`)
- Interactive elements (clickable divs, etc.)
- ARIA labels and roles
- Bounding boxes for numbers overlay

**Output:** JSON with all elements:
```json
{
  "scrape": {
    "url": "https://yahoo.com",
    "title": "Yahoo",
    "elements": [
      {
        "id": "vos_1",
        "tag": "a",
        "type": "link",
        "name": "Sign in",
        "selector": "#header-signin-link",
        "bounds": { "left": 100, "top": 50, "width": 60, "height": 30 }
      },
      {
        "id": "vos_2",
        "tag": "input",
        "type": "input",
        "name": "Search the web",
        "selector": "#search-input",
        "bounds": { "left": 200, "top": 100, "width": 400, "height": 40 }
      }
    ]
  }
}
```

### 2. DOMElement.kt (commonMain)
**Purpose:** Data models for scraped DOM elements.

**Classes:**
- `DOMElement` - Single interactive element
- `ElementBounds` - Position/size
- `DOMScrapeResult` - Full page scrape
- `ScraperResponse` - JSON wrapper

### 3. VoiceCommandGenerator.kt (commonMain)
**Purpose:** Convert DOM elements to voice commands with flexible matching.

**Matching Logic:**
```
User says: "sign in"
↓
Tokenize: ["sign", "in"]
↓
Compare to element names:
  - "Sign in" → ["sign", "in"] → MATCH (2 words)
  - "Sign up" → ["sign", "up"] → NO MATCH (only 1 word matches)
↓
Single match → Execute
Multiple matches → Ask NLU to disambiguate
```

### 4. WebAvanueVoiceOSBridge.kt (androidMain)
**Purpose:** Android integration between WebView and VoiceOS.

**Key Methods:**
- `attach()` - Inject JavaScript interface
- `scrapeDom()` - Get all interactive elements
- `clickElement(vosId)` - Click by VoiceOS ID
- `focusElement(selector)` - Focus an input
- `inputText(selector, text)` - Type into input
- `scrollToElement(selector)` - Scroll to element

### 5. SecureScriptLoader.kt (androidMain)
**Purpose:** Protect JavaScript from reverse engineering.

**How it works:**
- JavaScript split into encrypted Base64 fragments
- Decryption key derived from app signature + device ID
- Assembled at runtime only

---

## Integration Flow

### Step 1: Page Load
```kotlin
// In WebAvanue MainActivity or WebViewFragment
val bridge = WebAvanueVoiceOSBridge(webView)
bridge.attach()

webView.webViewClient = object : WebViewClient() {
    override fun onPageFinished(view: WebView, url: String) {
        // Scrape DOM after page loads
        lifecycleScope.launch {
            val result = bridge.scrapeDom()
            result?.let {
                voiceCommandGenerator.clear()
                voiceCommandGenerator.addElements(it.elements)
            }
        }
    }
}
```

### Step 2: Voice Command Received
```kotlin
// When speech recognition returns text
fun onVoiceCommand(spokenText: String) {
    val matches = voiceCommandGenerator.findMatches(spokenText)

    when {
        matches.isEmpty() -> {
            speak("No matching element found")
        }
        matches.size == 1 -> {
            executeCommand(matches[0].command)
        }
        else -> {
            // Multiple matches - disambiguate
            val options = voiceCommandGenerator.generateDisambiguationOptions(matches)
            askUserToChoose(options)
        }
    }
}
```

### Step 3: Execute Command
```kotlin
fun executeCommand(command: WebVoiceCommand) {
    lifecycleScope.launch {
        when (command.action) {
            CommandAction.CLICK -> bridge.clickElement(command.selector)
            CommandAction.FOCUS -> bridge.focusElement(command.selector)
            CommandAction.INPUT -> {
                bridge.focusElement(command.selector)
                // Then handle voice input for text
            }
            CommandAction.SCROLL_TO -> bridge.scrollToElement(command.selector)
        }
    }
}
```

---

## Numbers Overlay for WebAvanue

### How It Works

1. **Scrape DOM** → Get all interactive elements with bounds
2. **Generate numbers** → Assign number 1, 2, 3... to each element
3. **Draw overlay** → Position number badges at element bounds
4. **Voice command** → "Click 5" → Execute click on element 5

### Implementation

```kotlin
// In WebAvanue with numbers overlay
fun showNumbersOverlay() {
    val result = bridge.scrapeDom() ?: return

    // Convert to overlay items
    val items = result.elements.mapIndexed { index, element ->
        OverlayItem(
            number = index + 1,
            bounds = Rect(
                element.bounds.left,
                element.bounds.top,
                element.bounds.right,
                element.bounds.bottom
            ),
            vosId = element.id
        )
    }

    overlayService.showNumbers(items)
}
```

---

## Comparison: Chrome vs WebAvanue

| Feature | Chrome (Accessibility API) | WebAvanue (JS Bridge) |
|---------|---------------------------|----------------------|
| Element detection | ~5-10 elements | 50-200+ elements |
| Link text | Partial (content-desc only) | Full text + ARIA |
| Form support | Basic | Full (placeholders, labels) |
| Dynamic content | Misses JS-rendered | Sees everything |
| Numbers overlay | Not working | Full support |
| Voice commands | Very limited | Complete |

---

## File Structure

```
Modules/WebAvanue/universal/src/
├── commonMain/kotlin/com/augmentalis/webavanue/voiceos/
│   ├── DOMScraperBridge.kt      # JavaScript scraper
│   ├── DOMElement.kt            # Data models
│   └── VoiceCommandGenerator.kt # Voice command matching
│
└── androidMain/kotlin/com/augmentalis/webavanue/voiceos/
    ├── WebAvanueVoiceOSBridge.kt # Android WebView integration
    └── SecureScriptLoader.kt     # JS protection
```

---

## Next Steps

1. **Integrate with WebAvanue app** - Add bridge to MainActivity
2. **Add numbers overlay** - Draw number badges on WebView
3. **Connect to VoiceOSCoreNG** - Route commands through main engine
4. **Test on real sites** - Yahoo, Google, common sites
5. **Handle edge cases** - iframes, shadow DOM, dynamic updates

---

## APK Size Estimate

| Component | Estimated Size |
|-----------|---------------|
| WebAvanue app (base) | ~3-5 MB |
| VoiceOSCoreNG library | ~500 KB |
| NLU code (no models) | ~200 KB |
| LLM code (no models) | ~150 KB |
| Voice bridge code | ~50 KB |
| **Total (code only)** | **~4-6 MB** |

Models (downloaded separately):
- Speech recognition: 50-200 MB
- NLU BERT model: 100-400 MB
- LLM (if local): 2-8 GB

---

**Author:** Claude Code
**Approved by:** [Pending]
