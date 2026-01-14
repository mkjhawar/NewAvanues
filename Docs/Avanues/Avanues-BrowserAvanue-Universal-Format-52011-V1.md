# BrowserAvanue - Universal Format v2.0 Implementation

**Last Updated:** 2025-11-20
**Status:** Active
**Format Version:** 2.0

---

## Overview

BrowserAvanue is the voice-first web browser for the Avanues ecosystem. It uses **Universal Format v2.0** (.awb files) to define all browser voice commands in a human-readable, IPC-ready format.

### Key Features

1. **Voice-First Navigation:** Navigate the web using natural voice commands
2. **Tab Management:** Voice-controlled tab operations
3. **Universal Format:** Same file format as AVA, VoiceOS, AvaConnect
4. **Cross-Project Integration:** Commands can be referenced from other Avanues apps
5. **IPC-Ready:** Direct mapping to browser command handlers

---

## Architecture

### Command Flow

```
User Voice Input
      ↓
Speech Recognition (STT)
      ↓
AVA NLU Engine
      ↓
Intent Classification
      ↓
BrowserAvanue Command Handler
      ↓
Browser Action Execution
```

### File Structure

```
browser-commands/
├── browseravanue-commands.awb  # All 20 browser commands
└── README.md                    # Format documentation
```

---

## Universal Format v2.0 Structure

### File Header

```
# Avanues Universal Format v1.0
# Type: AWB - Browser Commands
# Extension: .awb
# Project: BrowserAvanue (Ava Web Browser)
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: browseravanue
metadata:
  file: browseravanue-commands.awb
  category: browser
  name: Browser Commands
  description: Voice commands for web browser control
  priority: 1
  count: 20
---
```

### Command Entries

Format: `CODE:command_id:voice_example`

```
# NAVIGATION
URL:navigate:go to url
URL:navigate:navigate to
URL:navigate:open website

BCK:back:go back
BCK:back:navigate back
BCK:back:previous page

FWD:forward:go forward
FWD:forward:navigate forward
FWD:forward:next page

RLD:reload:reload page
RLD:reload:refresh page
RLD:reload:reload
```

### Global Synonyms

```
---
synonyms:
  go: [navigate, open, visit]
  search: [find, look for, query]
  close: [exit, quit, shut]
  open: [show, display, view]
```

---

## Command Categories (20 Commands)

### 1. Navigation Commands (4)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| URL | navigate | Navigate to URL | "go to google.com", "open wikipedia" |
| BCK | back | Navigate back | "go back", "previous page" |
| FWD | forward | Navigate forward | "go forward", "next page" |
| RLD | reload | Reload page | "reload", "refresh page" |

### 2. Tab Management (3)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| TAB | new_tab | Open new tab | "new tab", "open new tab" |
| CLS | close_tab | Close tab | "close tab", "close current tab" |
| SWT | switch_tab | Switch tab | "switch tab", "next tab" |

### 3. Zoom Control (2)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| ZIN | zoom_in | Zoom in | "zoom in", "enlarge", "bigger" |
| ZOT | zoom_out | Zoom out | "zoom out", "shrink", "smaller" |

### 4. Search (1)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| SRC | search | Web search | "search for", "find", "look for" |

### 5. Bookmarks (1)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| BMK | bookmark | Bookmark page | "bookmark this", "save bookmark" |

### 6. History (1)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| HST | history | Show history | "show history", "browsing history" |

### 7. Downloads (1)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| DWN | downloads | Show downloads | "show downloads", "download manager" |

### 8. Settings (1)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| CFG | settings | Open settings | "open settings", "browser settings" |

### 9. Incognito Mode (1)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| INC | incognito | Private browsing | "incognito mode", "private browsing" |

### 10. Voice Search (1)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| VCM | voice_search | Voice search | "voice search", "search by voice" |

### 11. Page Actions (2)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| PRT | print | Print page | "print page", "print this" |
| SHR | share | Share page | "share page", "share this" |

### 12. Reader Mode (1)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| RDR | reader_mode | Reader mode | "reader mode", "reading mode" |

### 13. Fullscreen (1)

| Code | Command ID | Purpose | Examples |
|------|------------|---------|----------|
| FSC | fullscreen | Fullscreen | "fullscreen", "full screen mode" |

---

## Implementation

### Kotlin Command Handler (Example)

```kotlin
package com.augmentalis.browseravanue.commands

import com.augmentalis.avamagic.ipc.UniversalFileParser
import com.augmentalis.avamagic.ipc.FileType

class BrowserCommandHandler {

    private val commands: Map<String, BrowserCommand>

    init {
        // Load commands from .awb file
        val awbContent = loadAssetFile("browser-commands/browseravanue-commands.awb")
        val parsed = UniversalFileParser.parse(awbContent, FileType.AWB)
        commands = parseCommands(parsed)
    }

    fun handleCommand(code: String, params: List<String>) {
        when (code) {
            "URL" -> navigateToUrl(params.firstOrNull() ?: "")
            "BCK" -> navigateBack()
            "FWD" -> navigateForward()
            "RLD" -> reloadPage()
            "TAB" -> openNewTab()
            "CLS" -> closeTab()
            "SWT" -> switchTab()
            "ZIN" -> zoomIn()
            "ZOT" -> zoomOut()
            "SRC" -> search(params.joinToString(" "))
            "BMK" -> bookmarkPage()
            "HST" -> showHistory()
            "DWN" -> showDownloads()
            "CFG" -> openSettings()
            "INC" -> openIncognito()
            "VCM" -> voiceSearch()
            "PRT" -> printPage()
            "SHR" -> sharePage()
            "RDR" -> readerMode()
            "FSC" -> fullscreen()
            else -> Log.w("BrowserCommand", "Unknown command: $code")
        }
    }

    private fun navigateToUrl(url: String) {
        // WebView navigation implementation
    }

    private fun navigateBack() {
        if (webView.canGoBack()) webView.goBack()
    }

    private fun navigateForward() {
        if (webView.canGoForward()) webView.goForward()
    }

    private fun reloadPage() {
        webView.reload()
    }

    // ... other command implementations
}
```

### Integration with AVA

BrowserAvanue commands can be triggered from AVA:

```kotlin
// AVA recognizes "open google"
// Routes to BrowserAvanue via IPC

val message = "URL:session123:google.com"
sendToBrowserAvanue(message)
```

---

## Voice Command Examples

### Navigation

```
User: "Open google.com"
→ URL:nav1:google.com
→ Browser navigates to google.com

User: "Go back"
→ BCK:nav1
→ Browser navigates to previous page

User: "Reload page"
→ RLD:nav1
→ Browser reloads current page
```

### Tab Management

```
User: "New tab"
→ TAB:tab1
→ Browser opens new tab

User: "Close tab"
→ CLS:tab1
→ Browser closes current tab

User: "Switch tab"
→ SWT:tab1
→ Browser switches to next tab
```

### Search

```
User: "Search for pizza near me"
→ SRC:search1:pizza near me
→ Browser searches for "pizza near me"
```

### Zoom

```
User: "Zoom in"
→ ZIN:zoom1
→ Browser increases page zoom

User: "Zoom out"
→ ZOT:zoom1
→ Browser decreases page zoom
```

### Bookmarks & History

```
User: "Bookmark this"
→ BMK:bookmark1
→ Browser saves current page to bookmarks

User: "Show history"
→ HST:history1
→ Browser displays browsing history
```

---

## Cross-Project Usage

### From AVA

```kotlin
// AVA Intent: open_browser
// Delegates to BrowserAvanue with URL command

val intent = Intent(ACTION_BROWSER_COMMAND).apply {
    putExtra("code", "URL")
    putExtra("url", "wikipedia.org")
}
sendBroadcast(intent)
```

### From VoiceOS

```kotlin
// VoiceOS system command: browser_search
// Routes to BrowserAvanue

val command = "SRC:search1:android development"
browserCommandHandler.handleCommand("SRC", listOf("android development"))
```

### From AvaConnect

```kotlin
// Remote device sends browser command via IPC

// Received: URL:remote123:github.com
val parts = message.split(":")
if (parts[0] == "URL") {
    browserCommandHandler.handleCommand("URL", listOf(parts[2]))
}
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `parse AWB file successfully`() {
    val content = loadTestResource("browseravanue-commands.awb")
    val parsed = UniversalFileParser.parse(content, FileType.AWB)

    assertEquals("avu-1.0", parsed.schema)
    assertEquals("browseravanue", parsed.project)
    assertEquals(20, parsed.metadata["count"])
}

@Test
fun `handle URL command`() {
    val handler = BrowserCommandHandler()
    handler.handleCommand("URL", listOf("google.com"))

    verify(webView).loadUrl("google.com")
}

@Test
fun `handle navigation commands`() {
    handler.handleCommand("BCK", emptyList())
    verify(webView).goBack()

    handler.handleCommand("FWD", emptyList())
    verify(webView).goForward()

    handler.handleCommand("RLD", emptyList())
    verify(webView).reload()
}
```

---

## References

- **Universal Format Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-FILE-FORMAT-FINAL.md`
- **Universal IPC Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-IPC-SPEC.md`
- **Master Guide:** `/Volumes/M-Drive/Coding/Avanues/docs/Universal-Format-v2.0-Master-Guide.md`
- **UniversalFileParser:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/IPC/UniversalIPC/`
- **Commands File:** `/Volumes/M-Drive/Coding/Avanues/browser-commands/browseravanue-commands.awb`
- **AVA Integration:** `/Volumes/M-Drive/Coding/AVA/docs/Developer-Manual-Chapter37-Universal-Format-v2.0.md`

---

**Status:** ✅ Production Ready
**Format:** Universal v2.0 (.awb)
**Total Commands:** 20
**Categories:** 13
