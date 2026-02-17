# Safari Web Extension for Voice Control — Feasibility Analysis

**Document:** iOS-Analysis-SafariWebExtension-260212-V1.md
**Date:** 2026-02-12
**Version:** V1
**Status:** RESEARCH COMPLETE

---

## Executive Summary

Safari Web Extensions on iOS **CAN** add voice control to Safari pages via content script injection + native messaging to the containing app for microphone access. However, strict memory limits (6 MB), non-persistent background scripts, and no direct mic access make this a **lightweight companion** to the in-app WKWebView browser, not a replacement.

**Recommendation:** Ship BOTH — in-app browser for power features + Safari extension for casual users.

---

## 1. Architecture

### Components
```
Native iOS App (Container) — has mic permission, SFSpeechRecognizer
    |
    +-- SafariWebExtensionHandler.swift — bridge (NSExtensionRequestHandling)
    |
    +-- Safari Extension
        |-- manifest.json — permissions, content scripts
        |-- background.js — native messaging bridge (NON-PERSISTENT on iOS)
        |-- content.js — DOM scraping, voice overlay injection
        |-- popup.html/js — settings UI (accessed via AA icon)
```

### Communication Flow
```
User taps mic button (content.js)
  -> browser.runtime.sendMessage() -> background.js
  -> browser.runtime.sendNativeMessage() -> SafariWebExtensionHandler.swift
  -> SFSpeechRecognizer starts listening
  -> Transcription sent back through chain
  -> content.js executes command on page DOM
```

### Key Constraint
Background scripts are **non-persistent** on iOS — Safari unloads them after 30-45 seconds idle. Design must be stateless.

---

## 2. Voice/Audio Capabilities

### Direct Microphone Access: NO
Safari extensions cannot directly access the microphone. No WebExtensions API for audio capture.

### Workaround: Native App Bridge
The containing iOS app CAN access the microphone via SFSpeechRecognizer. Messages flow:
- Content script -> background.js -> native messaging -> Swift app -> mic -> transcription -> back

### Web Speech API in Safari
- Supported but **unreliable** on iOS
- Only works after user interaction (click handler)
- Known issues with word skipping
- **Verdict**: Native SFSpeechRecognizer via bridge is far better

### Latency
- Native messaging round-trip: ~50-200ms
- Background script wake-up (if unloaded): +1-2 seconds
- Total worst case: ~2.5 seconds (background was cold)
- Best case: ~100-200ms (background still warm)

---

## 3. DOM Capabilities

### Content Script Injection: FULL
Content scripts inject into ANY webpage the user grants permission for:
```json
{
  "content_scripts": [{
    "matches": ["<all_urls>"],
    "js": ["content.js"],
    "run_at": "document_end"
  }]
}
```

### What Content Scripts CAN Do
- Add floating overlays (voice buttons, numbered labels)
- Read all DOM elements (links, buttons, inputs)
- Click elements, fill forms, scroll pages
- Execute JavaScript in page context (via postMessage bridge)
- Style injection (CSS)

### CSP Restrictions
- Content script CODE is not restricted by page CSP
- DOM-injected SCRIPTS may be blocked by strict CSP
- Workaround: Use browser.tabs.executeScript() from background, or window.postMessage

### DOMScraperBridge.js Reuse
The same JavaScript that scrapes DOM on Android WebView works in Safari content scripts. This is **direct code reuse**.

---

## 4. User Experience

### Enabling the Extension
1. Download app from App Store
2. Settings -> Safari -> Extensions -> Toggle ON
3. Grant website permissions (per-site, one day, or always)
4. In Safari: tap AA icon -> Manage Extensions

### Voice UI Options

| Approach | Visibility | Best For |
|----------|-----------|----------|
| Content script floating button | Always visible on page | Primary voice activation |
| Popup (AA menu) | On demand | Settings, configuration |

### Activation
User MUST tap the mic button — extensions cannot auto-activate voice recognition. No always-listening capability.

---

## 5. App Store Review

### Category: Utilities > Accessibility

### Required
- Privacy policy with mic/speech disclosure
- `NSMicrophoneUsageDescription` + `NSSpeechRecognitionUsageDescription` in Info.plist
- Settings/help UI in the extension
- Clear accessibility purpose in review notes

### Risks
- **LOW**: Legitimate accessibility use case
- **MEDIUM**: Requesting `<all_urls>` permission (justify in review notes)
- Request specific patterns if possible, fall back to `<all_urls>` if needed

---

## 6. Limitations

### Hard Limits
| Limitation | Impact |
|------------|--------|
| 6 MB memory limit (total extension) | Cannot bundle AI models, heavy JS |
| Non-persistent background scripts | Cold start latency, stateless design |
| No direct mic access | Must relay through native app |
| No persistent connection to app | Message-based only |
| User must tap to activate voice | No always-listening |
| Cannot modify Safari chrome | No custom address bar, tab bar |

### vs Chrome Extensions
- No Chrome extension equivalent on iOS (Safari only)
- ~70% WebExtensions API compatibility
- App Store distribution required ($99/year)
- Native messaging restricted to containing app only

---

## 7. In-App WKWebView vs Safari Extension

| Feature | In-App WKWebView | Safari Extension |
|---------|------------------|-----------------|
| Full voice control | YES | YES (with latency) |
| Direct mic access | YES | NO (native bridge) |
| AI/NLU integration | YES (full) | NO (6 MB limit) |
| RAG/semantic search | YES | NO |
| User's default browser | NO | YES |
| Safari bookmarks/tabs | NO | YES |
| Always-listening | NO (iOS limit) | NO |
| Latency | Low (~50ms) | Medium (~200ms-2.5s) |
| Code reuse (DOMScraperBridge) | Identical JS | Identical JS |
| App Store risk | Low | Low-Medium |

### Recommendation: Ship Both

```
Single iOS App Container
|-- In-App WKWebView Browser (power users, full AI features)
|-- Safari Web Extension (casual users, works in Safari)
+-- Shared Voice Engine (SFSpeechRecognizer, KMP commands)
```

**In-app browser**: Advanced features (RAG, semantic commands, NLU, on-device AI)
**Safari extension**: Lightweight voice navigation (click, scroll, search, basic commands)

Users choose their preference. Extension is the gateway, in-app browser is the power tool.

---

## 8. Implementation Estimate

| Component | Effort |
|-----------|--------|
| Extension manifest + project setup | 1 day |
| content.js (DOMScraperBridge port + voice overlay) | 2 days |
| background.js (native messaging bridge) | 1 day |
| SafariWebExtensionHandler.swift | 1 day |
| Voice recognition integration | 2 days |
| popup.html (settings/help UI) | 1 day |
| Testing + App Store submission | 2 days |
| **TOTAL** | **~10 working days** |

This can run in parallel with the main in-app browser development.
