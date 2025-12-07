# VoiceOsLogging

Timber-based logging framework for VOS4 with file, remote, and Crashlytics support.

## Overview

VoiceOsLogging replaces the custom `VoiceOsLogger` module with industry-standard **Timber** plus three custom Trees for advanced logging capabilities:

1. **FileLoggingTree** - Daily rotating file logs with export
2. **RemoteLoggingTree** - HTTP endpoint logging with batching
3. **CrashlyticsTree** - Firebase Crashlytics integration

**Lines Saved:** ~650 (987 custom logger - 335 Timber Trees)

## Quick Start

### Basic Setup

```kotlin
// Application onCreate()
class VoiceOsApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Debug builds: Log everything to Logcat
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Release builds: File + Crashlytics only
        if (!BuildConfig.DEBUG) {
            Timber.plant(FileLoggingTree(this, retentionDays = 7))
            Timber.plant(CrashlyticsTree())
        }
    }
}
```

### Logging

```kotlin
// Standard Timber API
Timber.d("Debug message")
Timber.i("Info message")
Timber.w("Warning message")
Timber.e(exception, "Error occurred")

// Tagged logging
Timber.tag("MyFeature").i("Feature initialized")

// Formatted messages
Timber.d("User %s logged in at %d", userId, timestamp)
```

## Trees

**Note:** CrashlyticsTree is optional and requires Firebase setup. See "Firebase Setup" section below.

### FileLoggingTree

Logs to daily rotating files with automatic cleanup.

**Features:**
- Daily rotation (one file per day)
- Configurable retention (default: 7 days)
- Thread-safe file I/O
- Export to external storage
- Log level filtering

**Setup:**
```kotlin
val fileTree = FileLoggingTree(
    context = this,
    retentionDays = 7,
    minLogLevel = Log.DEBUG
)
Timber.plant(fileTree)
```

**Advanced Usage:**
```kotlin
// Export logs to external storage
lifecycleScope.launch {
    val exportDir = getExternalFilesDir("exported_logs")
    val count = fileTree.exportLogs(exportDir!!)
    Toast.makeText(this, "Exported $count files", Toast.LENGTH_SHORT).show()
}

// Get log files
val logFiles = fileTree.getLogFiles() // Sorted newest first

// Get total log size
val sizeBytes = fileTree.getTotalLogSize()

// Manual cleanup
fileTree.cleanupOldLogs()

// Clear all logs
fileTree.clearAllLogs()
```

**File Format:**
```
2025-10-23 16:42:00.123 D/MyTag: Debug message
2025-10-23 16:42:01.456 E/MyTag: Error message
java.lang.Exception: Stack trace here
    at com.example.MyClass.method(MyClass.kt:42)
```

**File Location:**
- Internal storage: `/data/data/com.augmentalis.voiceos/files/logs/`
- File naming: `voiceos_20251023.log`

### RemoteLoggingTree

Sends logs to HTTP endpoint with batching and retry.

**Features:**
- Batched transmission (default: 50 logs)
- Auto-retry with exponential backoff (1s, 2s, 4s)
- Offline queue (sends when back online)
- Auto-flush timer (default: 1 minute)
- Log level filtering (default: WARN and ERROR only)

**Setup:**
```kotlin
val remoteTree = RemoteLoggingTree(
    endpoint = "https://api.example.com/logs",
    apiKey = "your-api-key",
    batchSize = 50,
    flushIntervalMs = 60_000, // 1 minute
    minLogLevel = Log.WARN
)
Timber.plant(remoteTree)
```

**Advanced Usage:**
```kotlin
// Manual flush (sends immediately)
remoteTree.flush()

// Shutdown gracefully (sends remaining logs)
remoteTree.shutdown()

// Check queue size
val pending = remoteTree.getQueueSize()

// Clear queue (discard unsent logs)
remoteTree.clearQueue()
```

**JSON Payload:**
```json
{
  "logs": [
    {
      "timestamp": 1729720920123,
      "level": "ERROR",
      "tag": "MyTag",
      "message": "Error occurred",
      "stackTrace": "java.lang.Exception: ..."
    }
  ]
}
```

**HTTP Headers:**
```
POST /logs HTTP/1.1
Content-Type: application/json
Authorization: Bearer your-api-key
```

### CrashlyticsTree

Forwards logs to Firebase Crashlytics for crash reporting.

**Features:**
- Automatic crash breadcrumbs (last 64KB of logs)
- Non-fatal exception logging
- Custom metadata (user ID, key-value pairs)
- Log level filtering (default: WARN and ERROR only)
- Privacy-friendly (no debug logs)

**Setup:**
```kotlin
val crashlyticsTree = CrashlyticsTree(
    minLogLevel = Log.WARN,
    enableNonFatalExceptions = true
)
Timber.plant(crashlyticsTree)
```

**Advanced Usage:**
```kotlin
// Set user ID for crash reports
crashlyticsTree.setUserId("user123")

// Add custom metadata
crashlyticsTree.setCustomKey("feature_enabled", true)
crashlyticsTree.setCustomKey("api_version", 2)
crashlyticsTree.setCustomKey("last_sync", System.currentTimeMillis())

// Control data collection
crashlyticsTree.setCrashlyticsCollectionEnabled(userConsent)

// Force send crashes
crashlyticsTree.sendUnsentReports()

// Delete unsent crashes (user opt-out)
crashlyticsTree.deleteUnsentReports()

// Check if previous execution crashed
if (crashlyticsTree.didCrashOnPreviousExecution()) {
    // Show recovery UI
}
```

## Migration from VoiceOsLogger

### Old Code
```kotlin
VoiceOsLogger.d(TAG, "Debug message")
VoiceOsLogger.e(TAG, "Error message", exception)
VoiceOsLogger.initialize(context)
```

### New Code
```kotlin
Timber.d("Debug message")
Timber.e(exception, "Error message")
// Initialize in Application.onCreate() once
```

### Key Differences

| Feature | VoiceOsLogger | VoiceOsLogging (Timber) |
|---------|--------------|------------------------|
| **API** | Custom static methods | Standard Timber API |
| **Tags** | Required parameter | Optional via `.tag()` |
| **File Logging** | Built-in | FileLoggingTree |
| **Remote Logging** | Custom HTTP client | RemoteLoggingTree |
| **Crashlytics** | Manual integration | CrashlyticsTree |
| **Lines of Code** | 987 | 335 (65% reduction) |
| **Dependencies** | Custom | Industry-standard |
| **Maintenance** | High | Low (Timber maintained) |

## Recommended Configurations

### Development (Debug Builds)
```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
```
- Logs everything to Logcat
- No file/remote logging (fast iteration)

### Production (Release Builds)
```kotlin
if (!BuildConfig.DEBUG) {
    Timber.plant(FileLoggingTree(this, retentionDays = 7))
    Timber.plant(CrashlyticsTree())
}
```
- File logs for user support
- Crashlytics for production monitoring
- No debug logs (privacy + performance)

### Beta Builds
```kotlin
Timber.plant(Timber.DebugTree())
Timber.plant(FileLoggingTree(this, retentionDays = 14))
Timber.plant(RemoteLoggingTree(endpoint, apiKey))
Timber.plant(CrashlyticsTree())
```
- Full debugging capability
- Extended retention for issue investigation
- Remote logging for beta feedback

### Privacy-First
```kotlin
val crashTree = CrashlyticsTree(minLogLevel = Log.ERROR)
crashTree.setCrashlyticsCollectionEnabled(userConsent)
Timber.plant(crashTree)
```
- Only errors logged
- User consent required
- No file/remote logging

## Performance

| Operation | Time (avg) |
|-----------|-----------|
| Timber.d() | <0.1ms |
| FileLoggingTree write | <1ms (async) |
| RemoteLoggingTree queue | <0.1ms (async send) |
| CrashlyticsTree log | <0.5ms |

**Overhead:** Negligible (<0.5ms per log call)

## Testing

All three trees are testable:

```kotlin
@Test
fun testFileLogging() = runTest {
    val tree = FileLoggingTree(context, retentionDays = 1)
    Timber.plant(tree)

    Timber.d("Test message")

    val logFiles = tree.getLogFiles()
    assertEquals(1, logFiles.size)
    assertTrue(logFiles[0].readText().contains("Test message"))
}
```

## Dependencies

```kotlin
// Timber (core)
implementation("com.jakewharton.timber:timber:5.0.1")

// Firebase Crashlytics (for CrashlyticsTree)
implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
implementation("com.google.firebase:firebase-crashlytics-ktx")

// Coroutines (for async I/O)
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## ADR Reference

This implementation follows:
- **ADR-002**: No Interfaces by Default (direct Timber.Tree implementations)
- **ADR-003**: Performance-First (async I/O, batching, minimal overhead)

## Firebase Setup (Optional)

CrashlyticsTree requires Firebase Crashlytics. To enable:

1. **Add Firebase to your project:**
   - Follow: https://firebase.google.com/docs/android/setup
   - Download `google-services.json` to `app/` directory

2. **Enable Crashlytics in root build.gradle.kts:**
   ```kotlin
   plugins {
       id("com.google.gms.google-services") version "4.4.2" apply false
       id("com.google.firebase.crashlytics") version "3.0.2" apply false
   }
   ```

3. **Enable in app/build.gradle.kts:**
   ```kotlin
   plugins {
       id("com.google.gms.google-services")
       id("com.google.firebase.crashlytics")
   }
   ```

4. **Uncomment Firebase dependencies in VoiceOsLogging/build.gradle.kts:**
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
   implementation("com.google.firebase:firebase-crashlytics-ktx")
   ```

5. **Rename CrashlyticsTree.kt.optional to CrashlyticsTree.kt**

6. **Rebuild project**

If you don't need Crashlytics, use FileLoggingTree and RemoteLoggingTree only.

## License

Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
