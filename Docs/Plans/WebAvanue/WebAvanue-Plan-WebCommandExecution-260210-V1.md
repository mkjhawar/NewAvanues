# Web Voice Command Execution Pipeline — Implementation Plan

**Module**: WebAvanue + VoiceOSCore
**Branch**: `VoiceOSCore-KotlinUpdate`
**Date**: 2026-02-10
**Status**: IMPLEMENTED (all phases complete, all modules compile)

## Context

The web voice command pipeline was ~85% complete. DOM scraping, command generation, speech grammar registration, and voice recognition all worked. The critical gap: **when the speech engine recognized a web phrase, nothing executed**. `BrowserVoiceOSCallback.executeCommand()` was a stub returning `true`. Web phrases were only in the speech grammar — NOT registered as `QuantizedCommand`s in the `ActionCoordinator`'s `CommandRegistry`, so the routing system couldn't find them.

**Goal**: Complete the pipeline so every actionable web element can be voice-controlled, with full gesture support.

---

## Implementation Summary

### NEW Files (5):
| File | Module | Purpose |
|------|--------|---------|
| `IWebCommandExecutor.kt` | VoiceOSCore/commonMain/interfaces/ | KMP interface + WebAction/WebActionType/WebActionResult types |
| `WebCommandHandler.kt` | VoiceOSCore/commonMain/handler/ | IHandler routing web commands to executor |
| `WebCommandExecutorImpl.kt` | WebAvanue/commonMain | Implements IWebCommandExecutor, builds JS from DOMScraperBridge |
| `IJavaScriptExecutor.kt` | WebAvanue/commonMain | Platform-abstract JS evaluation interface |
| `AndroidJavaScriptExecutor.kt` | WebAvanue/androidMain | Android WebView JS evaluation with 3s timeout |

### MODIFIED Files (7):
| File | Changes |
|------|---------|
| `DOMScraperBridge.kt` | Added 25+ selector-based action scripts (element, page nav, form, gesture, clipboard) |
| `BrowserVoiceOSCallback.kt` | Completed executeCommand() stub; added getWebCommandsAsQuantized(); added JS executor setter; exposed active instance |
| `ActionCoordinator.kt` | Added "hover"/"grab"/"drag"/"rotate" to verbs; mapped all new CommandActionTypes |
| `CommandActionType.kt` | Added 18 new enum values (PAGE_BACK, SWIPE_*, GRAB, ROTATE, DRAG, DOUBLE_CLICK, HOVER, etc.) |
| `StaticCommandRegistry.kt` | Expanded browserCommands from 1 to 21 commands |
| `VoiceAvanueAccessibilityService.kt` | WebCommandHandler creation/registration; dual-path web command registration; executor wiring |
| `WebViewContainer.android.kt` | AndroidJavaScriptExecutor wiring; active instance lifecycle management |
| `WebAvanue/build.gradle.kts` | Added VoiceOSCore as dependency |

### Architecture

```
Speech Engine → ActionCoordinator → WebCommandHandler → IWebCommandExecutor
                                                              ↓
                                                    WebCommandExecutorImpl
                                                              ↓
                                                    DOMScraperBridge.*Script()
                                                              ↓
                                                    IJavaScriptExecutor
                                                              ↓
                                                    AndroidJavaScriptExecutor
                                                              ↓
                                                    WebView.evaluateJavascript()
```

### Dual-Path Registration
1. **Speech Grammar**: Web phrases → `voiceOSCore.updateWebCommands(phrases)` → speech recognition
2. **Command Registry**: Web commands → `getWebCommandsAsQuantized()` → `updateDynamicCommands()` → ActionCoordinator routing

### Key Design Decisions
- **Inline gesture scripts** instead of external gestures.js — simpler, no page-load dependency
- **CallbackJsExecutorProxy** pattern — WebCommandExecutorImpl delegates to BrowserVoiceOSCallback's JS executor
- **ActionCategory.BROWSER** (priority 11) — between UTILITY and CUSTOM in handler chain
- **WebAvanue depends on VoiceOSCore** (not circular — VoiceOSCore has no WebAvanue dependency)

### Build Verification
```
VoiceOSCore:  BUILD SUCCESSFUL (52s)
WebAvanue:    BUILD SUCCESSFUL (26s)
apps/avanues: BUILD SUCCESSFUL (14s)
```

---

## Supported Voice Commands

### Element Commands (from DOM scraping)
- "click [element]", "tap [element]"
- "focus [element]"
- "type [text]" (into focused input)
- "scroll to [element]"
- "toggle [checkbox]"
- "select [option]"
- "double tap [element]"
- "long press [element]"
- "hover [element]"

### Browser Navigation (static)
- "go back", "go forward", "refresh page"
- "scroll down", "scroll up", "page down", "page up"
- "go to top", "go to bottom"

### Form Navigation (static)
- "next field", "previous field"
- "submit form"

### Gestures (static)
- "swipe left/right/up/down"
- "grab [element]", "release"
- "rotate left/right"
- "zoom in", "zoom out"

### Clipboard (static)
- "select all", "copy", "cut", "paste"
