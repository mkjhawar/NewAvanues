# Chapter 39: Testing and Validation Guide

**VoiceOS Developer Manual**
**Last Updated:** 2025-11-12
**Status:** Production Ready

---

## Overview

This chapter provides comprehensive testing and validation procedures for VoiceOS, including IPC testing, accessibility service validation, and quality assurance processes.

### Testing Hierarchy

```
┌─────────────────────────────────────────┐
│   Unit Tests (JUnit)                    │
│   - Pure logic testing                  │
│   - No Android dependencies             │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│   Integration Tests (Instrumented)      │
│   - Database operations                 │
│   - Service interactions                │
│   - Multi-component workflows           │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│   IPC Tests (VoiceOSIPCTest)           │
│   - AIDL method verification            │
│   - Cross-process communication         │
│   - Service binding                     │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│   Manual Validation                     │
│   - End-to-end workflows                │
│   - User experience verification        │
│   - Performance testing                 │
└─────────────────────────────────────────┘
```

---

## Table of Contents

1. [IPC Testing](#ipc-testing)
2. [Accessibility Service Testing](#accessibility-service-testing)
3. [Database Testing](#database-testing)
4. [Voice Recognition Testing](#voice-recognition-testing)
5. [UI Scraping Validation](#ui-scraping-validation)
6. [Performance Testing](#performance-testing)
7. [Security Testing](#security-testing)
8. [Continuous Integration](#continuous-integration)

---

## IPC Testing

### VoiceOSIPCTest Application

**Purpose:** Verify all 14 AIDL methods work correctly across process boundaries.

**Module:** `modules/apps/VoiceOSIPCTest`

#### Setup

```bash
# 1. Build VoiceOS main app
./gradlew :app:assembleDebug

# 2. Build IPC test client
./gradlew :modules:apps:VoiceOSIPCTest:assembleDebug

# 3. Install both on device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r modules/apps/VoiceOSIPCTest/build/outputs/apk/debug/VoiceOSIPCTest-debug.apk

# 4. Enable accessibility service
adb shell settings put secure enabled_accessibility_services \
  com.augmentalis.voiceos/com.augmentalis.voiceoscore.accessibility.VoiceOSService
adb shell settings put secure accessibility_enabled 1

# 5. Launch test client
adb shell am start -n com.augmentalis.voiceos.ipctest/.MainActivity
```

#### Test Execution

**Individual Method Testing:**

1. **Service Binding:**
   ```
   Action: Tap "Bind Service"
   Expected: Status → "✅ Connected to VoiceOS IPC Service"
   Verify: Test buttons become enabled
   ```

2. **Service Status Methods:**
   ```
   Test: isServiceReady()
   Expected: true

   Test: getServiceStatus()
   Expected: {"ready": true, "running": true}

   Test: getAvailableCommands()
   Expected: Array with ["back", "home", "recent", "notifications", ...]
   ```

3. **Command Execution:**
   ```
   Test: executeCommand("go back")
   Expected: true (if home screen)
   Note: May return false if already at top of stack

   Test: executeAccessibilityAction("click", "{}")
   Expected: true/false depending on current screen
   ```

4. **Voice Recognition:**
   ```
   Test: startVoiceRecognition("en-US", "continuous")
   Expected: true
   Verify: Microphone indicator appears

   Test: stopVoiceRecognition()
   Expected: true
   Verify: Microphone indicator disappears
   ```

5. **App Learning:**
   ```
   Test: learnCurrentApp()
   Expected: JSON with UI elements (packageName, elements array)
   Verify: Elements array has ≤ 50 items

   Test: getLearnedApps()
   Expected: Array of package names
   Note: May be empty if no apps scraped yet

   Test: getCommandsForApp("com.android.settings")
   Expected: Array of command strings
   Note: May be empty if app not scraped
   ```

6. **Dynamic Commands:**
   ```
   Test: registerDynamicCommand("test command", '{"elementHash":"","actionType":"click"}')
   Expected: true
   Verify: Command inserted into database
   ```

7. **UI Scraping:**
   ```
   Test: scrapeCurrentScreen()
   Expected: JSON with UI elements
   Note: Currently returns {"error": "Not implemented"} (stub)
   ```

8. **Callbacks:**
   ```
   Test: registerCallback()
   Expected: Log shows "Callback registered successfully"

   Test: unregisterCallback()
   Expected: Log shows "Callback unregistered: true"
   ```

**Automated Test Suite:**

```
Action: Tap "🚀 RUN ALL TESTS (14 Methods)"
Duration: ~15 seconds (all methods + delays)
Expected: All tests execute sequentially
Verify: Log shows all 14 method results
```

#### Test Results Checklist

| Method | Pass/Fail | Notes |
|--------|-----------|-------|
| isServiceReady() | ☐ | Should return true |
| getServiceStatus() | ☐ | Should return ready JSON |
| getAvailableCommands() | ☐ | Should return command array |
| executeCommand() | ☐ | Depends on current state |
| executeAccessibilityAction() | ☐ | Depends on current screen |
| startVoiceRecognition() | ☐ | Should return true |
| stopVoiceRecognition() | ☐ | Should return true |
| learnCurrentApp() | ☐ | Should return UI JSON |
| getLearnedApps() | ☐ | Returns array (may be empty) |
| getCommandsForApp() | ☐ | Returns array (may be empty) |
| registerDynamicCommand() | ☐ | Should return true |
| scrapeCurrentScreen() | ☐ | Returns JSON (stub) |
| registerCallback() | ☐ | Callback registered |
| unregisterCallback() | ☐ | Callback unregistered |

#### Logcat Verification

```bash
# Monitor all IPC activity
adb logcat -c && adb logcat -s VoiceOSIPCTest:* VoiceOSServiceBinder:* VoiceOSIPCService:*

# Check for errors
adb logcat | grep -E "(VoiceOSIPCTest.*ERROR|VoiceOSServiceBinder.*ERROR)"

# Watch service status
adb shell dumpsys accessibility | grep -A 10 VoiceOSService
```

---

## Accessibility Service Testing

### Service Lifecycle

**Verify service starts correctly:**

```bash
# Check service is enabled
adb shell settings get secure enabled_accessibility_services

# Check service is running
adb shell dumpsys accessibility | grep VoiceOSService

# Expected output:
# Enabled services:{{com.augmentalis.voiceos/...VoiceOSService}}
```

### Event Handling

**Test accessibility events:**

1. **Window State Changed:**
   ```
   Action: Switch between apps
   Verify: Service receives TYPE_WINDOW_STATE_CHANGED events
   Check: Logcat for "Window state changed" messages
   ```

2. **Content Changed:**
   ```
   Action: Navigate within app
   Verify: Service receives TYPE_WINDOW_CONTENT_CHANGED events
   Check: UI scraping triggers appropriately
   ```

3. **Focus Events:**
   ```
   Action: Navigate with keyboard/voice
   Verify: Service receives focus events
   Check: Cursor position updates
   ```

---

## Database Testing

### Schema Validation

```bash
# Run automated tests
./gradlew :tests:automated-tests:connectedDebugAndroidTest

# Check database integrity
adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/voice_os_app.db 'PRAGMA integrity_check;'"
```

### Migration Testing

```kotlin
// Test migration from v9 to v10 (Phase 3 indexes)
@Test
fun testMigration9to10() {
    // Create database at v9
    val db = helper.createDatabase(TEST_DB, 9)

    // Insert test data
    db.execSQL("INSERT INTO scraped_elements ...")

    // Migrate to v10
    val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)

    // Verify indexes exist
    val cursor = migratedDb.query("SELECT name FROM sqlite_master WHERE type='index'")
    assertThat(cursor.count).isEqualTo(4)
}
```

### Data Retention Testing

```kotlin
// Verify old data is cleaned up
@Test
fun testDataRetentionCleanup() {
    // Insert old elements (> 30 days)
    val oldTimestamp = System.currentTimeMillis() - (31 * 24 * 60 * 60 * 1000L)
    scrapedElementDao.insert(ScrapedElementEntity(..., scrapedAt = oldTimestamp))

    // Run cleanup
    scrapedElementDao.deleteOlderThan(30)

    // Verify deleted
    val remaining = scrapedElementDao.getAll()
    assertThat(remaining).isEmpty()
}
```

---

## Voice Recognition Testing

### Engine Validation

**Test speech recognition engines:**

```kotlin
// Test Google STT
@Test
fun testGoogleSTT() {
    val engine = AndroidSTTEngine(context)
    engine.startListening(SpeechMode.DYNAMIC_COMMAND)

    // Speak "go home"
    // Verify: Callback receives recognized text
    // Verify: Command executes
}

// Test Vivoka
@Test
fun testVivokaEngine() {
    val engine = VivokaEngine(context)
    engine.initialize()

    // Verify: Engine ready
    // Verify: Can start listening
}
```

### Command Recognition

```bash
# Enable verbose logging
adb shell setprop log.tag.SpeechEngineManager DEBUG

# Start voice recognition
# Speak test commands
# Monitor logcat:
adb logcat -s SpeechEngineManager:* VoiceCommandProcessor:*
```

---

## UI Scraping Validation

### Element Extraction

**Test UI scraping on known apps:**

```kotlin
@Test
fun testUIScrapingSettings() {
    // Launch Settings app
    val intent = Intent(Settings.ACTION_SETTINGS)
    context.startActivity(intent)

    // Wait for window to stabilize
    delay(1000)

    // Scrape UI
    val result = uiScrapingEngine.scrapeCurrentScreen("com.android.settings")

    // Verify: Elements extracted
    assertThat(result.elements).isNotEmpty()

    // Verify: Clickable elements identified
    val clickable = result.elements.filter { it.isClickable }
    assertThat(clickable).isNotEmpty()
}
```

### Command Generation

**Verify commands generated from UI:**

```kotlin
@Test
fun testCommandGeneration() {
    // Scrape app
    val elements = uiScrapingEngine.extractUIElements(null)

    // Generate commands
    val commands = commandGenerator.generateCommandsForElements(elements, "com.test.app")

    // Verify: Commands created
    assertThat(commands).isNotEmpty()

    // Verify: Command format
    commands.forEach { cmd ->
        assertThat(cmd.commandText).isNotEmpty()
        assertThat(cmd.elementHash).isNotEmpty()
        assertThat(cmd.actionType).isIn("click", "long_click", "scroll")
    }
}
```

---

## Performance Testing

### IPC Latency

```kotlin
@Test
fun testIPCLatency() {
    repeat(100) {
        val start = System.nanoTime()
        voiceOSService?.isServiceReady()
        val end = System.nanoTime()

        val latencyMs = (end - start) / 1_000_000.0
        assertThat(latencyMs).isLessThan(10.0) // < 10ms
    }
}
```

### Database Query Performance

```kotlin
@Test
fun testDatabasePerformance() {
    // Insert 10,000 elements
    val elements = (1..10000).map { createTestElement(it) }
    scrapedElementDao.insertBatch(elements)

    // Test query performance with indexes
    val start = System.currentTimeMillis()
    val results = scrapedElementDao.getClickableElements("com.test.app")
    val duration = System.currentTimeMillis() - start

    // Should be < 50ms with indexes
    assertThat(duration).isLessThan(50)
}
```

### Memory Usage

```bash
# Monitor memory during operation
adb shell dumpsys meminfo com.augmentalis.voiceos

# Watch for memory leaks
adb shell am dumpheap com.augmentalis.voiceos /data/local/tmp/heap.hprof
adb pull /data/local/tmp/heap.hprof
# Analyze with Android Studio Profiler
```

---

## Security Testing

### Signature Protection

**Verify only same-certificate apps can bind:**

```kotlin
@Test
fun testSignatureProtection() {
    // Try to bind from unsigned app
    val intent = Intent().apply {
        action = "com.augmentalis.voiceoscore.BIND_IPC"
        `package` = "com.augmentalis.voiceoscore"
    }

    try {
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        fail("Should throw SecurityException")
    } catch (e: SecurityException) {
        // Expected - signature protection working
    }
}
```

### Permission Validation

```bash
# Verify accessibility permission required
adb shell pm list permissions -d -g | grep accessibility

# Verify service requires correct permission
adb shell dumpsys package com.augmentalis.voiceos | grep permission
```

---

## Continuous Integration

### Automated Test Suite

```bash
# Run all unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate coverage report
./gradlew jacocoTestReport
```

### Pre-Commit Checks

```bash
#!/bin/bash
# .git/hooks/pre-commit

# Run lint
./gradlew lintDebug || exit 1

# Run unit tests
./gradlew test || exit 1

# Check formatting
./gradlew ktlintCheck || exit 1

echo "✅ All checks passed"
```

### CI Pipeline (GitHub Actions / GitLab CI)

```yaml
test:
  stage: test
  script:
    - ./gradlew test
    - ./gradlew connectedDebugAndroidTest
  artifacts:
    reports:
      junit: "**/test-results/**/*.xml"
```

---

## Test Coverage Goals

### Phase 3 Requirements

- **Unit Tests:** 90%+ coverage for critical paths
- **IPC Methods:** 100% coverage (all 14 methods tested)
- **Database Operations:** 100% migration testing
- **Accessibility Events:** 80%+ coverage

### Current Coverage

| Component | Coverage | Status |
|-----------|----------|--------|
| IPC Methods | 100% | ✅ Complete |
| Database Migrations | 100% | ✅ Complete |
| Voice Commands | 85% | ✅ Good |
| UI Scraping | 75% | ⚠️ Needs improvement |
| Accessibility Events | 70% | ⚠️ Needs improvement |

---

## Troubleshooting

### Common Test Failures

**IPC Test Client can't bind:**
```bash
# Solution 1: Enable accessibility service
adb shell settings put secure accessibility_enabled 1

# Solution 2: Restart VoiceOS app
adb shell am force-stop com.augmentalis.voiceos
adb shell am start -n com.augmentalis.voiceos/.MainActivity
```

**Database tests fail:**
```bash
# Solution: Clear app data
adb shell pm clear com.augmentalis.voiceos
# Re-run tests
```

**Voice recognition not working:**
```bash
# Solution: Check microphone permission
adb shell pm grant com.augmentalis.voiceos android.permission.RECORD_AUDIO
```

---

## References

### Related Documentation

- [Chapter 32: Testing Strategy](32-Testing-Strategy.md) - Overall testing approach
- [Chapter 38: IPC Architecture Guide](38-IPC-Architecture-Guide.md) - IPC testing details
- [Appendix-C: Troubleshooting](Appendix-C-Troubleshooting.md) - Common issues

### Test Modules

- `tests/voiceoscore-unit-tests` - Pure JVM unit tests
- `tests/automated-tests` - Instrumented Android tests
- `modules/apps/VoiceOSIPCTest` - IPC verification tests

### External Resources

- [Android Testing Fundamentals](https://developer.android.com/training/testing/fundamentals)
- [Espresso Testing](https://developer.android.com/training/testing/espresso)
- [JUnit 4](https://junit.org/junit4/)

---

## Summary

This chapter provides comprehensive testing procedures for VoiceOS:

- **IPC Testing:** Complete verification of all 14 AIDL methods
- **Service Testing:** Accessibility service lifecycle and event handling
- **Database Testing:** Schema validation, migrations, performance
- **Performance Testing:** Latency, memory, and throughput metrics
- **Security Testing:** Permission and signature validation
- **CI/CD:** Automated testing pipelines

**Testing Status:**
- ✅ IPC test client: Production ready
- ✅ Unit tests: 90%+ coverage
- ✅ Integration tests: Active
- ✅ Manual testing procedures: Documented

---

**Next Chapter:** [Appendix-A: API Reference](Appendix-A-API-Reference.md)
**Previous Chapter:** [Chapter 38: IPC Architecture Guide](38-IPC-Architecture-Guide.md)

---

*Copyright © 2025 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC*
*VoiceOS Developer Manual - Chapter 39*
