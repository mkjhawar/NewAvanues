# VoiceOS AIDL Integration

**Version:** 1.0
**Updated:** 2025-12-07
**Module:** `Modules/AVA/Actions`

---

## Overview

AVA communicates with VoiceOS using Android AIDL (Android Interface Definition Language) for IPC. This enables AVA to leverage VoiceOS's accessibility service for:
- Executing UI actions (taps, swipes, gestures)
- Scraping screen elements for context
- Running accessibility-based commands

---

## Architecture

```
┌─────────────────┐     AIDL Binding     ┌─────────────────────────┐
│                 │  ←─────────────────→ │                         │
│   AVA App       │                      │  VoiceOS Service        │
│                 │                      │  (AccessibilityService) │
│ VoiceOSConnection                      │                         │
│       ↓         │                      │  VoiceOSServiceBinder   │
│ IVoiceOSService │                      │         ↓               │
│ IVoiceOSCallback│                      │  Accessibility API      │
└─────────────────┘                      └─────────────────────────┘
```

---

## Key Files

| File | Purpose |
|------|---------|
| `VoiceOSConnection.kt` | AIDL service binding manager |
| `VoiceOSQueryProvider.kt` | Screen scraping via AIDL |
| `VoiceOSDetector.kt` | Installation/status detection |
| `IVoiceOSService.aidl` | Service interface definition |
| `IVoiceOSCallback.aidl` | Callback interface definition |

---

## AIDL Interface

### IVoiceOSService

```aidl
interface IVoiceOSService {
    boolean executeCommand(String command);
    boolean executeAccessibilityAction(String action, String paramsJson);
    String scrapeCurrentScreen();
    List<String> getAvailableCommands();
    String getServiceStatus();
    boolean isServiceReady();
    void registerCallback(IVoiceOSCallback callback);
    void unregisterCallback(IVoiceOSCallback callback);
}
```

### IVoiceOSCallback

```aidl
interface IVoiceOSCallback {
    void onCommandRecognized(String command, float confidence);
    void onCommandExecuted(String command, boolean success, String message);
    void onServiceStateChanged(int state, String message);
    void onScrapingComplete(String elementsJson, int elementCount);
}
```

---

## Usage

### 1. Get Connection Instance

```kotlin
val connection = VoiceOSConnection.getInstance(context)
```

### 2. Bind to Service

```kotlin
lifecycleScope.launch {
    val connected = connection.bind()
    if (connected) {
        // Ready to use
    }
}
```

### 3. Execute Commands

```kotlin
val result = connection.executeCommand(
    intent = "tap",
    category = "accessibility",
    parameters = mapOf("target" to "Settings")
)

when (result) {
    is CommandResult.Success -> Log.d(TAG, "Executed in ${result.executionTimeMs}ms")
    is CommandResult.Failure -> Log.e(TAG, "Failed: ${result.error}")
}
```

### 4. Scrape Screen

```kotlin
val screenJson = connection.scrapeCurrentScreen()
// Returns JSON with elements array
```

### 5. Register Callbacks

```kotlin
connection.setCallback(
    onCommandExecuted = { command, success, message ->
        Log.d(TAG, "Command $command: $success")
    },
    onScrapingComplete = { json, count ->
        Log.d(TAG, "Scraped $count elements")
    }
)
```

### 6. Cleanup

```kotlin
connection.unbind()
```

---

## VoiceOSQueryProvider

High-level API for screen data:

```kotlin
val provider = VoiceOSQueryProvider(context)

// Get current app
val packageName = provider.queryAppContext()

// Get clickable elements
val elements = provider.queryClickableElements()

// Query by selector
val buttons = provider.queryElementsBySelector(".Button")
val okButton = provider.queryElementsBySelector("[text='OK']")

// Suspend versions with auto-connect
val elementsAsync = provider.queryClickableElementsAsync()
```

---

## VoiceOSDetector

Check VoiceOS status:

```kotlin
val detector = VoiceOSDetector(context)

// Basic checks
val installed = detector.isVoiceOSInstalled()
val accessible = detector.isAccessibilityServiceEnabled()
val ready = detector.isVoiceOSReady()

// Detailed status
val status = detector.getStatus()
// VoiceOSStatus(installed, packageName, accessibilityEnabled, version, ready)
```

---

## Build Configuration

### build.gradle.kts (Actions module)

```kotlin
android {
    buildFeatures {
        aidl = true
    }

    sourceSets["main"].aidl.srcDirs("src/androidMain/aidl")
}
```

### File Structure

```
Modules/AVA/Actions/
├── src/
│   ├── androidMain/
│   │   ├── aidl/
│   │   │   └── com/augmentalis/voiceoscore/accessibility/
│   │   │       ├── IVoiceOSService.aidl
│   │   │       ├── IVoiceOSCallback.aidl
│   │   │       └── CommandResult.aidl
│   │   └── kotlin/
│   │       └── com/augmentalis/ava/features/actions/
│   │           └── VoiceOSConnection.kt
```

---

## Package Names

| Package | Purpose |
|---------|---------|
| `com.augmentalis.voiceoscore` | VoiceOS main package (current) |
| `com.augmentalis.voiceos` | Alternative package |
| `com.avanues.voiceos` | Legacy package |

VoiceOSDetector and VoiceOSConnection check all package variations for backwards compatibility.

---

## Error Handling

| Error | Cause | Resolution |
|-------|-------|------------|
| `VoiceOS not installed` | Package not found | Install VoiceOS app |
| `Bind timeout` | Service not responding | Enable accessibility service |
| `Permission denied` | Security exception | Check manifest permissions |
| `Service disconnected` | Process killed | Auto-reconnect on next call |

---

## Threading

- `bind()` - Main thread (suspend)
- `executeCommand()` - IO dispatcher
- `scrapeCurrentScreen()` - Main thread (sync)
- `setCallback()` - Main thread

---

## Migration from ContentProvider

Previous implementation used ContentProvider queries which didn't work because VoiceOS exposes AIDL, not ContentProvider.

| Old (ContentProvider) | New (AIDL) |
|-----------------------|------------|
| `contentResolver.query()` | `service.scrapeCurrentScreen()` |
| URI-based queries | Method calls |
| Cursor parsing | JSON parsing |
| No callbacks | Full callback support |

---

## Related

- [VoiceOS AIDL Binding](../../../../VoiceOS/Technical/modules/VoiceOSCore/aidl-binding-251207.md)
- [Accessibility Service Architecture](../../../../VoiceOS/Technical/manuals/developer/architecture/accessibility-service.md)
