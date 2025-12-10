# VoiceOS Connection Architecture

**Version:** 1.0
**Updated:** 2025-12-07

---

## Overview

VoiceOSConnection provides a singleton AIDL binding manager for AVA to communicate with VoiceOS accessibility service.

---

## Component Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                         AVA App                              │
├──────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │ VoiceOSDetector │    │        VoiceOSQueryProvider     │ │
│  │                 │    │                                 │ │
│  │ - isInstalled() │    │ - queryAppContext()             │ │
│  │ - isReady()     │    │ - queryClickableElements()      │ │
│  │ - getStatus()   │    │ - queryElementsBySelector()     │ │
│  └────────┬────────┘    └───────────────┬─────────────────┘ │
│           │                             │                    │
│           └──────────────┬──────────────┘                    │
│                          ↓                                   │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              VoiceOSConnection (Singleton)            │  │
│  │                                                       │  │
│  │  - bind() / unbind()                                  │  │
│  │  - executeCommand()                                   │  │
│  │  - scrapeCurrentScreen()                              │  │
│  │  - setCallback()                                      │  │
│  │                                                       │  │
│  │  ┌─────────────────────────────────────────────────┐ │  │
│  │  │ ServiceConnection                               │ │  │
│  │  │  - onServiceConnected()                         │ │  │
│  │  │  - onServiceDisconnected()                      │ │  │
│  │  │  - onBindingDied()                              │ │  │
│  │  └─────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────┘  │
│                          │                                   │
│                          │ AIDL Binding                      │
└──────────────────────────┼───────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────────┐
│                      VoiceOS App                             │
├──────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐  │
│  │            VoiceOSService (AccessibilityService)      │  │
│  │                                                       │  │
│  │  ┌─────────────────────────────────────────────────┐ │  │
│  │  │ VoiceOSServiceBinder : IVoiceOSService.Stub     │ │  │
│  │  │                                                 │ │  │
│  │  │  - executeCommand()                             │ │  │
│  │  │  - executeAccessibilityAction()                 │ │  │
│  │  │  - scrapeCurrentScreen()                        │ │  │
│  │  │  - getAvailableCommands()                       │ │  │
│  │  └─────────────────────────────────────────────────┘ │  │
│  │                          ↓                            │  │
│  │  ┌─────────────────────────────────────────────────┐ │  │
│  │  │ Accessibility API                               │ │  │
│  │  │  - performGlobalAction()                        │ │  │
│  │  │  - findAccessibilityNodeInfosByViewId()         │ │  │
│  │  │  - getRootInActiveWindow()                      │ │  │
│  │  └─────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## State Machine

```
     ┌──────────────┐
     │ Disconnected │←──────────────────────┐
     └──────┬───────┘                       │
            │ bind()                        │
            ↓                               │
     ┌──────────────┐                       │
     │  Connecting  │                       │
     └──────┬───────┘                       │
            │                               │
    ┌───────┼───────┐                       │
    ↓       ↓       ↓                       │
Success  Timeout  Error                     │
    │       │       │                       │
    ↓       └───────┴───────────────────────┤
┌──────────────┐                            │
│  Connected   │────── unbind() ────────────┤
└──────────────┘                            │
        │                                   │
        │ onServiceDisconnected()           │
        │ onBindingDied()                   │
        └───────────────────────────────────┘
```

---

## Connection States

| State | Description |
|-------|-------------|
| `Disconnected` | Not bound to VoiceOS service |
| `Connecting` | Bind initiated, waiting for callback |
| `Connected` | Service bound and ready |
| `Error` | Connection failed with error message |

---

## Thread Safety

| Component | Thread Safety |
|-----------|---------------|
| `instance` | `@Volatile` + `synchronized` |
| `isBound` | `AtomicBoolean` |
| `voiceOSService` | Accessed on main thread |
| `connectionState` | Updated from ServiceConnection |

---

## Lifecycle

1. **Initialization**: `getInstance(context)` creates singleton
2. **Binding**: `bind()` starts async binding with timeout
3. **Usage**: Call methods after connection confirmed
4. **Callbacks**: Optional real-time event notifications
5. **Cleanup**: `unbind()` releases service connection

---

## Auto-Reconnect

VoiceOSConnection automatically handles reconnection:
- On `executeCommand()` if disconnected → attempts `bind()`
- On `scrapeCurrentScreen()` via QueryProvider → check and reconnect

---

## Timeouts

| Operation | Timeout |
|-----------|---------|
| Bind | 5 seconds |
| Command execution | 30 seconds |

---

## Related

- [VoiceOS AIDL Integration](../features/voiceos-aidl-integration-251207.md)
