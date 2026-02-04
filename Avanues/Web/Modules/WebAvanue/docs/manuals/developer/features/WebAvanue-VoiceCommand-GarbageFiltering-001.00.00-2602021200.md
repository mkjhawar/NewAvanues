# WebAvanue: Voice Command Garbage Filtering & Icon Support

**Version:** 1.0.0
**Last Updated:** 2026-02-02
**Status:** Production Ready
**Module:** WebAvanue Voice Commands

---

## Table of Contents

1. [Overview](#overview)
2. [Garbage Text Filtering](#garbage-text-filtering)
3. [Icon Command Support](#icon-command-support)
4. [JavaScript DOM Scraper Updates](#javascript-dom-scraper-updates)
5. [Localization](#localization)
6. [API Reference](#api-reference)
7. [Integration Guide](#integration-guide)

---

## Overview

This document describes voice command improvements in WebAvanue for filtering garbage text and supporting icon element commands. These features mirror the VoiceOSCore implementation for consistency across native Android and WebView contexts.

### Key Features

1. **Garbage Text Filtering** - Removes invalid voice command candidates from DOM elements
2. **Icon Command Support** - Single-word commands for toolbar icons and action buttons
3. **JS Injection Updates** - Garbage filtering in the DOM scraper bridge for third-party browsers
4. **Localization** - Multi-language support for 6 locales

### Files Modified

| File | Purpose |
|------|---------|
| `VoiceCommandGenerator.kt` | Kotlin voice command generation |
| `DOMScraperBridge.kt` | JavaScript injection for DOM scraping |
| `BrowserVoiceOSCallback.kt` | Voice command callback handling |

---

## Garbage Text Filtering

### VoiceCommandGenerator (Kotlin)

Located in: `Modules/WebAvanue/src/commonMain/kotlin/com/augmentalis/webavanue/VoiceCommandGenerator.kt`

#### Localized Repetitive Words

```kotlin
private val LOCALIZED_REPETITIVE_WORDS = mapOf(
    "en" to setOf("comma", "dot", "dash", "space", "tab", "enter",
                  "null", "undefined", "nan", "true", "false"),
    "de" to setOf("komma", "punkt", "strich", "leerzeichen", ...),
    "es" to setOf("coma", "punto", "guion", "espacio", ...),
    "fr" to setOf("virgule", "point", "tiret", "espace", ...),
    "zh" to setOf("逗号", "句号", "空格", ...),
    "ja" to setOf("コンマ", "ピリオド", "スペース", ...)
)
```

#### Pattern Detection

```kotlin
private val GARBAGE_PATTERNS = listOf(
    // CSS classes: btn-primary-flex
    Regex("^[a-z]+(-[a-z]+){2,}$", RegexOption.IGNORE_CASE),
    // Base64/hash strings
    Regex("^[A-Za-z0-9+/=]{20,}$"),
    // Hex strings
    Regex("^(0x)?[a-f0-9]{8,}$", RegexOption.IGNORE_CASE),
    // Punctuation only
    Regex("^[\\s\\p{Punct}]+$"),
    // UUIDs
    Regex("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"),
    // Package names
    Regex("^[a-z]+\\.[a-z]+\\.[a-z]+", RegexOption.IGNORE_CASE),
    // Object toString
    Regex("^\\[?object\\s*\\w*\\]?$|^\\w+@[a-f0-9]+$", RegexOption.IGNORE_CASE)
)
```

#### API

```kotlin
/**
 * Check if text is garbage that should not be a voice command.
 */
fun isGarbageText(text: String, locale: String = "en"): Boolean

/**
 * Clean label text by removing garbage content.
 */
fun cleanLabel(text: String, locale: String = "en"): String?
```

---

## Icon Command Support

### Detection Logic

Icons are detected based on:
- Element type (`button`, `a` with role="button")
- Small dimensions (< 150x150 pixels)
- Has `aria-label` or `title` but no visible text
- Class name contains icon-related terms

```kotlin
private val LOCALIZED_NAVIGATION_ICONS = mapOf(
    "en" to setOf(
        "menu", "more", "options", "settings", "back", "forward",
        "home", "close", "refresh", "search", "filter", "sort",
        "add", "edit", "delete", "save", "share", "download",
        "call", "meet", "video", "camera", "mic", "mute",
        "play", "pause", "stop", "star", "favorite", "bookmark",
        "help", "info"
    ),
    "de" to setOf("menü", "mehr", "optionen", "einstellungen", ...),
    "es" to setOf("menú", "más", "opciones", "ajustes", ...),
    "fr" to setOf("menu", "plus", "options", "paramètres", ...)
)
```

### Minimum Word Requirements

```kotlin
companion object {
    const val MIN_WORDS_FOR_MATCH = 2      // Standard elements
    const val MIN_WORDS_FOR_ICON = 1       // Icon elements
}

fun addElements(elements: List<DOMElement>) {
    elements.forEach { element ->
        val command = createCommand(element)
        if (command != null) {
            val minWords = if (isIconElement(element))
                MIN_WORDS_FOR_ICON
            else
                MIN_WORDS_FOR_MATCH

            if (command.words.size >= minWords) {
                commands.add(command)
                indexCommand(command)
            }
        }
    }
}
```

---

## JavaScript DOM Scraper Updates

### DOMScraperBridge (JavaScript Injection)

Located in: `Modules/WebAvanue/src/commonMain/kotlin/com/augmentalis/webavanue/DOMScraperBridge.kt`

The JavaScript code injected into WebViews now includes garbage filtering:

#### Repetitive Words (Multi-Language)

```javascript
const REPETITIVE_WORDS = new Set([
    // English
    'comma', 'dot', 'dash', 'space', 'tab', 'enter',
    'null', 'undefined', 'nan', 'true', 'false',
    // German
    'komma', 'punkt', 'strich', 'leerzeichen',
    // Spanish
    'coma', 'punto', 'guion', 'espacio',
    // French
    'virgule', 'tiret', 'espace', 'entrer'
]);
```

#### Garbage Detection Function

```javascript
function isGarbageText(text) {
    if (!text) return true;
    const trimmed = text.trim();
    if (trimmed.length <= 1) return true;

    // Exact match check
    if (GARBAGE_EXACT.has(trimmed.toLowerCase())) return true;

    // Pattern checks
    if (/^[a-z]+(-[a-z]+){2,}$/i.test(trimmed)) return true;  // CSS classes
    if (/^[A-Za-z0-9+\/=]{20,}$/.test(trimmed)) return true;  // Base64
    if (/^(0x)?[a-f0-9]{8,}$/i.test(trimmed)) return true;    // Hex
    if (/^[\s\p{P}]+$/u.test(trimmed)) return true;           // Punctuation

    // Repetitive word detection
    const words = trimmed.toLowerCase().split(/\s+/);
    if (words.length >= 2) {
        let repetitiveCount = 0;
        words.forEach(w => {
            if (REPETITIVE_WORDS.has(w)) repetitiveCount++;
        });
        if (repetitiveCount > words.length / 2) return true;
    }

    return false;
}
```

#### Clean Label Function

```javascript
function cleanLabel(text) {
    if (isGarbageText(text)) return null;

    let cleaned = text.trim()
        .replace(/[\r\n\t]+/g, ' ')
        .replace(/\s{2,}/g, ' ');

    // Remove leading/trailing punctuation
    cleaned = cleaned.replace(/^[\p{P}\p{S}]+|[\p{P}\p{S}]+$/gu, '').trim();

    return cleaned.length > 0 ? cleaned : null;
}
```

#### Updated getAccessibleName

```javascript
function getAccessibleName(element) {
    // Priority: aria-label > aria-labelledby > title > text content

    const ariaLabel = element.getAttribute('aria-label');
    if (ariaLabel) {
        const cleaned = cleanLabel(ariaLabel);
        if (cleaned) return cleaned;
    }

    const labelledBy = element.getAttribute('aria-labelledby');
    if (labelledBy) {
        const labelEl = document.getElementById(labelledBy);
        if (labelEl) {
            const cleaned = cleanLabel(labelEl.textContent);
            if (cleaned) return cleaned;
        }
    }

    const title = element.getAttribute('title');
    if (title) {
        const cleaned = cleanLabel(title);
        if (cleaned) return cleaned;
    }

    // Text content (cleaned)
    const textContent = element.textContent;
    if (textContent) {
        const cleaned = cleanLabel(textContent);
        if (cleaned) return cleaned;
    }

    return null;
}
```

---

## Localization

### Supported Locales

| Code | Language | Kotlin Support | JavaScript Support |
|------|----------|----------------|-------------------|
| `en` | English | Full | Full |
| `de` | German | Full | Partial (common words) |
| `es` | Spanish | Full | Partial (common words) |
| `fr` | French | Full | Partial (common words) |
| `zh` | Chinese | Partial | Not included |
| `ja` | Japanese | Partial | Not included |

### Locale Detection

```kotlin
// In BrowserVoiceOSCallback
private fun getCurrentLocale(): String {
    return java.util.Locale.getDefault().language
}
```

### Adding New Locales

1. **Kotlin (VoiceCommandGenerator.kt)**:
```kotlin
// Add to LOCALIZED_REPETITIVE_WORDS
"it" to setOf("virgola", "punto", "trattino", ...)

// Add to LOCALIZED_NAVIGATION_ICONS
"it" to setOf("menu", "altro", "opzioni", ...)
```

2. **JavaScript (DOMScraperBridge.kt)**:
```javascript
// Add to REPETITIVE_WORDS
'virgola', 'punto', 'trattino', 'spazio',  // Italian
```

---

## API Reference

### VoiceCommandGenerator

```kotlin
class VoiceCommandGenerator {
    companion object {
        const val MIN_WORDS_FOR_MATCH = 2
        const val MIN_WORDS_FOR_ICON = 1
    }

    /**
     * Check if text is garbage.
     */
    fun isGarbageText(text: String, locale: String = "en"): Boolean

    /**
     * Clean label text.
     */
    fun cleanLabel(text: String, locale: String = "en"): String?

    /**
     * Check if element is an icon.
     */
    fun isIconElement(element: DOMElement, locale: String = "en"): Boolean

    /**
     * Add DOM elements and generate commands.
     * Automatically handles garbage filtering and icon detection.
     */
    fun addElements(elements: List<DOMElement>)

    /**
     * Find matching commands for spoken phrase.
     */
    fun findMatches(spokenPhrase: String): List<MatchResult>

    /**
     * Get all available commands.
     */
    fun getAllCommands(): List<WebVoiceCommand>
}
```

### DOMScraperBridge

```kotlin
object DOMScraperBridge {
    /**
     * Get JavaScript code for DOM scraping.
     * Includes garbage filtering and icon detection.
     */
    fun getScrapeScript(): String

    /**
     * Parse scrape result from JavaScript.
     */
    fun parseScrapeResult(json: String): DOMScrapeResult?
}
```

---

## Integration Guide

### Using in BrowserVoiceOSCallback

```kotlin
class BrowserVoiceOSCallback : VoiceOSWebCallback {

    private val commandGenerator = VoiceCommandGenerator()

    override fun onDOMScraped(result: DOMScrapeResult) {
        // Clear previous commands
        commandGenerator.clear()

        // Add elements - garbage filtering is automatic
        commandGenerator.addElements(result.elements)

        val count = commandGenerator.getCommandCount()
        println("Generated $count voice commands")
    }

    fun findMatches(spokenPhrase: String): List<MatchResult> {
        return commandGenerator.findMatches(spokenPhrase)
    }
}
```

### Injecting Scraper Script

```kotlin
fun scrapeDOM(webView: WebView) {
    val script = DOMScraperBridge.getScrapeScript()

    webView.evaluateJavascript(script) { result ->
        val scrapeResult = DOMScraperBridge.parseScrapeResult(result)
        if (scrapeResult != null) {
            callback.onDOMScraped(scrapeResult)
        }
    }
}
```

---

## Testing

### Test Cases

1. **Garbage Filtering**
   - "comma comma comma" → Filtered
   - "btn-primary-active" → Filtered
   - "Click Here" → Not filtered

2. **Icon Detection**
   - `<button aria-label="Menu">` with 48x48 size → Icon: true
   - `<button>Submit Form</button>` with 200x50 size → Icon: false

3. **Localization**
   - German: "Komma Komma" → Filtered
   - French: "menu" as icon → Recognized

### Manual Testing

1. Load a web page with known garbage elements
2. Verify overlays don't appear for garbage text
3. Verify icon buttons get single-word commands
4. Test with different device locales

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02-02 | Initial release with garbage filtering and icon commands |

---

**Author:** Augmentalis Engineering
**Module Owner:** WebAvanue Team
**License:** Proprietary - Augmentalis Inc.
