# AVA Crash Reporting Architecture

**Purpose:** Privacy-first crash reporting and diagnostics for AVA AI
**Status:** Production - Local Logging Active
**Classification:** Internal Development Reference
**Last Updated:** 2025-12-01

---

## Table of Contents

1. [Overview](#overview)
2. [Design Philosophy](#design-philosophy)
3. [Privacy-First Architecture](#privacy-first-architecture)
4. [Current Implementation](#current-implementation)
5. [Technical Architecture](#technical-architecture)
6. [Usage Guidelines](#usage-guidelines)
7. [Future Options](#future-options)
8. [Testing Procedures](#testing-procedures)
9. [FAQ](#faq)
10. [Related Documentation](#related-documentation)

---

## Overview

AVA implements a **privacy-first crash reporting system** that keeps all diagnostic data local to the user's device by default. This document explains the architectural decisions, implementation details, and optional future enhancements.

### Key Characteristics

| Aspect | Implementation |
|--------|----------------|
| **Default Behavior** | Local logging only (Timber) |
| **Data Transmission** | None (no network requests) |
| **User Control** | Complete data sovereignty |
| **Privacy Compliance** | GDPR/CCPA compliant by default |
| **External Services** | Intentionally disabled |
| **Opt-In Mechanism** | Available but disabled |

**This is a deliberate design choice, not a missing feature.**

---

## Design Philosophy

### Why Privacy-First?

AVA's crash reporting design is guided by the following principles:

#### 1. User Privacy is Non-Negotiable

```
User data sovereignty > Developer convenience
```

- Users own their data, including crash reports
- No data should leave the device without explicit consent
- Privacy should be the default, not an option

#### 2. Transparency Over Convenience

```
Clear user understanding > Automatic data collection
```

- Users should know exactly what data is collected
- Users should know exactly where data goes
- No hidden data transmission

#### 3. Compliance by Design

```
Privacy regulation compliance > Feature richness
```

- GDPR compliant by default
- CCPA compliant by default
- No cookies, no tracking, no telemetry without consent

#### 4. Local-First Development

```
On-device diagnostics > Cloud-based analytics
```

- Developers can debug using local logs
- Users can share crash reports manually if they choose
- No dependency on third-party crash reporting services

---

## Privacy-First Architecture

### Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        AVA Application                       │
│                                                              │
│  ┌──────────────┐                                           │
│  │ App Crashes  │                                           │
│  │ or Exception │                                           │
│  └──────┬───────┘                                           │
│         │                                                    │
│         ▼                                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          CrashReporter.recordException()             │  │
│  │                                                       │  │
│  │  Privacy Check: Is crash reporting enabled?          │  │
│  └──────┬───────────────────────────────────────────────┘  │
│         │                                                    │
│         ├─── YES (Opt-In) ────┐                             │
│         │                      │                             │
│         │                      ▼                             │
│         │            ┌──────────────────┐                   │
│         │            │ Firebase         │ (Future)          │
│         │            │ Crashlytics      │ (Disabled)        │
│         │            │ (Commented Out)  │                   │
│         │            └──────────────────┘                   │
│         │                                                    │
│         └─── NO (Default) ───┐                              │
│                               │                              │
│                               ▼                              │
│                      ┌─────────────────┐                    │
│                      │ Timber Logging  │                    │
│                      │ (Local Only)    │                    │
│                      └────────┬────────┘                    │
│                               │                              │
│                               ▼                              │
│                      ┌─────────────────┐                    │
│                      │ Android Logcat  │                    │
│                      │ (Device Only)   │                    │
│                      └─────────────────┘                    │
│                                                              │
│  NO DATA LEAVES DEVICE (Default Configuration)              │
└─────────────────────────────────────────────────────────────┘
```

### Privacy Guarantees

| Guarantee | Implementation |
|-----------|----------------|
| **No Network Calls** | All Firebase code is commented out |
| **No PII Collection** | User IDs are only set if enabled |
| **No Automatic Opt-In** | `enabled = false` by default |
| **Runtime Control** | Users can toggle in Settings |
| **Local Storage** | Logs via Timber stay in logcat |
| **No Third-Party Services** | No Firebase dependencies active |

---

## Current Implementation

### Default Behavior (Privacy Mode)

**What Happens by Default:**

```kotlin
// In Application.onCreate() or MainActivity.onCreate()
CrashReporter.initialize(context, enabled = false)  // DEFAULT: disabled

// When an exception occurs
try {
    // Some operation that might fail
} catch (e: Exception) {
    CrashReporter.recordException(e)
    // ✅ Logged to Timber (local logcat)
    // ❌ NOT sent to Firebase
    // ❌ NO network request made
}
```

**Log Output (Local Only):**

```
D/CrashReporter: [Crash] User initiated voice command
E/CrashReporter: Non-fatal exception recorded
    java.lang.NullPointerException: Attempt to invoke...
    at com.augmentalis.ava.voice.VoiceProcessor.process(...)
    ...
```

### Where Logs Are Stored

| Platform | Location | Accessible By |
|----------|----------|---------------|
| **Android Device** | Logcat buffer | Developer via ADB |
| **Android Studio** | Logcat window | Developer during debug |
| **User Device** | System logs (temporary) | User via log capture apps |

**Important:** Logs are NOT persisted to disk, NOT uploaded to cloud, NOT shared with anyone.

---

## Technical Architecture

### Component Structure

```
Universal/AVA/Core/Common/src/androidMain/kotlin/
└── com/augmentalis/ava/crashreporting/
    └── CrashReporter.kt         # Main crash reporting facade
```

### CrashReporter API

#### Initialization

```kotlin
/**
 * Initialize crash reporter
 *
 * @param context Android application context
 * @param enabled Whether crash reporting is enabled (default: false)
 */
fun initialize(context: Context, enabled: Boolean = false)
```

**Usage:**

```kotlin
class AvaApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Privacy-first: disabled by default
        CrashReporter.initialize(this, enabled = false)
    }
}
```

#### Runtime Control

```kotlin
/**
 * Enable or disable crash reporting at runtime
 *
 * Connected to Settings > Privacy > Crash Reporting toggle
 */
fun setEnabled(enabled: Boolean)
```

**Usage:**

```kotlin
// In Settings screen
fun onCrashReportingToggle(enabled: Boolean) {
    CrashReporter.setEnabled(enabled)
    // Updates internal state
    // If enabled, future crashes would go to Firebase (when implemented)
}
```

#### Logging Breadcrumbs

```kotlin
/**
 * Log a message for debugging context
 * Only logged if crash reporting is enabled
 */
fun log(message: String)
fun log(tag: String, message: String)
```

**Usage:**

```kotlin
CrashReporter.log("User started voice recording")
CrashReporter.log("VoiceProcessor", "Processing audio chunk 5/10")
```

#### Recording Exceptions

```kotlin
/**
 * Record a non-fatal exception
 * These are caught exceptions that don't crash the app
 */
fun recordException(throwable: Throwable)
fun recordException(throwable: Throwable, message: String)
```

**Usage:**

```kotlin
try {
    processVoiceCommand(audio)
} catch (e: IOException) {
    CrashReporter.recordException(e, "Failed to process voice command")
    // App continues, but exception is logged
}
```

#### Setting Context

```kotlin
/**
 * Set custom key-value pairs for crash context
 * Helps with debugging by providing additional context
 */
fun setCustomKey(key: String, value: String)
fun setCustomKey(key: String, value: Boolean)
fun setCustomKey(key: String, value: Int)

/**
 * Set user identifier (only if enabled)
 */
fun setUserId(userId: String)
```

**Usage:**

```kotlin
CrashReporter.setCustomKey("nlu_model", "mALBERT-v2.0")
CrashReporter.setCustomKey("device_model", Build.MODEL)
CrashReporter.setCustomKey("voice_enabled", true)
```

### State Management

```kotlin
private var isEnabled: Boolean = false      // Privacy default: OFF
private var isInitialized: Boolean = false  // Initialization state
```

| State | Meaning | Behavior |
|-------|---------|----------|
| `isInitialized = false` | CrashReporter not initialized | All methods are no-ops |
| `isEnabled = false` | Privacy mode (default) | Logs to Timber only |
| `isEnabled = true` | User opted in | Would send to Firebase (when implemented) |

---

## Usage Guidelines

### When to Use CrashReporter

#### ✅ DO Use For:

1. **Non-Fatal Exceptions**
   ```kotlin
   try {
       loadModel()
   } catch (e: IOException) {
       CrashReporter.recordException(e, "Model loading failed")
       // Show user-friendly error
   }
   ```

2. **Debugging Context (Breadcrumbs)**
   ```kotlin
   CrashReporter.log("Starting NLU inference")
   val result = runInference()
   CrashReporter.log("NLU inference completed: ${result.intent}")
   ```

3. **State Information**
   ```kotlin
   CrashReporter.setCustomKey("llm_model", currentModel.name)
   CrashReporter.setCustomKey("rag_enabled", ragEnabled)
   ```

#### ❌ DON'T Use For:

1. **Fatal Crashes** - Android handles these automatically
2. **Expected Errors** - Use regular logging (Timber.d/i/w/e)
3. **Sensitive Data** - Never log passwords, tokens, PII
4. **High-Frequency Events** - Will spam logs, use sampling

### Best Practices

#### 1. Initialize Early

```kotlin
class AvaApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize before any other code that might crash
        CrashReporter.initialize(this, enabled = false)

        // Rest of initialization
        initializeNLU()
        initializeLLM()
    }
}
```

#### 2. Set Context Before Operations

```kotlin
fun processVoiceCommand(audio: ByteArray) {
    CrashReporter.setCustomKey("audio_length", audio.size)
    CrashReporter.setCustomKey("nlu_model", nluModel.version)

    try {
        val intent = nlu.classify(audio)
        executeIntent(intent)
    } catch (e: Exception) {
        CrashReporter.recordException(e, "Voice command processing failed")
    }
}
```

#### 3. Never Log Sensitive Data

```kotlin
// ❌ WRONG: Logging sensitive data
CrashReporter.setCustomKey("user_password", password)
CrashReporter.log("API key: $apiKey")

// ✅ CORRECT: Log non-sensitive metadata
CrashReporter.setCustomKey("user_id_hash", userId.hashCode().toString())
CrashReporter.log("API call initiated")
```

#### 4. Use Descriptive Messages

```kotlin
// ❌ WRONG: Vague message
CrashReporter.recordException(e, "Error")

// ✅ CORRECT: Descriptive context
CrashReporter.recordException(e, "Failed to load NLU model from assets/models/nlu/")
```

---

## Future Options

### Firebase Crashlytics Integration (Optional)

Firebase Crashlytics is **prepared but intentionally disabled**. Here's how to enable it if users opt-in:

#### Step 1: Add Configuration File

```bash
# Add Firebase config to standalone app
cp google-services.json apps/ava-standalone/
```

#### Step 2: Update Build Configuration

**File:** `apps/ava-standalone/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")  // ADD THIS
}

dependencies {
    // Add Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Add Crashlytics
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ndk")
}
```

#### Step 3: Uncomment Firebase Code

**File:** `CrashReporter.kt`

Find and uncomment all Firebase code blocks:

```kotlin
// Before (commented out)
// FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)

// After (uncommented)
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
```

#### Step 4: Enable in Settings

```kotlin
// User toggles Settings > Privacy > Crash Reporting
CrashReporter.setEnabled(true)
```

### Alternative Options

If Firebase is not desired, consider these alternatives:

| Service | Privacy Level | Pros | Cons |
|---------|--------------|------|------|
| **Local Only (Current)** | Maximum | No external dependencies | Manual log collection |
| **Firebase Crashlytics** | Medium | Full-featured, reliable | Google dependency |
| **Sentry** | Medium | Open source option | Additional dependency |
| **Bugsnag** | Medium | Good Android support | Commercial service |
| **Manual Email Reports** | High | User controlled | Manual process |

**Recommendation:** Stick with local-only unless users explicitly request cloud reporting.

---

## Testing Procedures

### Test 1: Verify Default Privacy Mode

**Objective:** Confirm no network calls are made

```kotlin
@Test
fun `crash reporter does not make network calls by default`() {
    // Initialize with default (disabled)
    CrashReporter.initialize(context, enabled = false)

    // Simulate crash
    val exception = RuntimeException("Test crash")
    CrashReporter.recordException(exception)

    // Verify no network calls
    // Check with network monitor (e.g., OkHttp Interceptor)
    assertThat(networkCalls).isEmpty()
}
```

### Test 2: Verify Timber Logging

**Objective:** Confirm logs appear in logcat

```kotlin
@Test
fun `crash reporter logs to Timber when disabled`() {
    // Initialize with disabled
    CrashReporter.initialize(context, enabled = false)

    // Record exception
    val exception = RuntimeException("Test exception")
    CrashReporter.recordException(exception)

    // Verify Timber received log
    assertThat(timberLogs).contains("Non-fatal exception recorded")
}
```

### Test 3: Verify Runtime Toggle

**Objective:** Confirm setEnabled() updates state

```kotlin
@Test
fun `crash reporter respects runtime toggle`() {
    CrashReporter.initialize(context, enabled = false)
    assertThat(CrashReporter.isEnabled()).isFalse()

    CrashReporter.setEnabled(true)
    assertThat(CrashReporter.isEnabled()).isTrue()

    CrashReporter.setEnabled(false)
    assertThat(CrashReporter.isEnabled()).isFalse()
}
```

### Test 4: Verify Initialization Guard

**Objective:** Confirm methods are no-ops before initialization

```kotlin
@Test
fun `crash reporter is no-op before initialization`() {
    // DO NOT call initialize()

    // These should not crash
    CrashReporter.recordException(RuntimeException("Test"))
    CrashReporter.log("Test message")
    CrashReporter.setCustomKey("key", "value")

    // Verify warning logged
    assertThat(timberLogs).contains("CrashReporter not initialized")
}
```

### Manual Testing

**Test Scenario 1: Device Log Verification**

1. Install AVA on device
2. Enable USB debugging
3. Connect to ADB: `adb logcat -s CrashReporter`
4. Trigger an exception in AVA
5. Verify log appears in logcat
6. **Verify NO network traffic using Charles Proxy or similar**

**Test Scenario 2: Privacy Settings Integration**

1. Open AVA Settings
2. Navigate to Privacy > Crash Reporting
3. Verify toggle is OFF by default
4. Toggle ON
5. Verify CrashReporter.isEnabled() returns true
6. Toggle OFF
7. Verify CrashReporter.isEnabled() returns false

---

## FAQ

### Q: Why not use Firebase Crashlytics by default?

**A:** Privacy is a core value of AVA. We believe users should have complete control over their data. Crash reports often contain sensitive information about device state, app usage, and potentially user data. By keeping crash reports local by default, we ensure users maintain sovereignty over their diagnostic data.

### Q: Isn't this inconvenient for developers?

**A:** Development is about trade-offs. While cloud-based crash reporting is convenient, user privacy is more important. Developers can still access crash logs via ADB/logcat during development. For production issues, we can implement opt-in mechanisms that clearly explain what data is collected.

### Q: What if users want to help improve AVA by sharing crashes?

**A:** Users can opt-in to crash reporting via Settings > Privacy > Crash Reporting. When enabled (and when Firebase is configured), crashes will be sent to Firebase Crashlytics. This is an explicit, informed choice by the user.

### Q: How do we debug production crashes without automatic reporting?

**A:** Several approaches:
1. **Local logs:** Users experiencing crashes can capture logs using `adb logcat` or log capture apps
2. **Manual reporting:** Users can share logs via email or support channels
3. **Opt-in analytics:** Offer users the option to enable crash reporting with clear explanation
4. **Beta testing:** Use opt-in crash reporting in beta builds

### Q: Is this approach compliant with app store requirements?

**A:** Yes. Both Google Play and iOS App Store allow apps without crash reporting. Many privacy-focused apps use local-only logging. However, having the infrastructure ready (even if disabled) makes it easier to enable if requirements change.

### Q: What about GDPR/CCPA compliance?

**A:** This approach is **maximally compliant** with privacy regulations:
- **GDPR:** No personal data is processed without consent
- **CCPA:** No personal information is sold or shared
- **No cookies/tracking:** Crash reporting doesn't track users
- **Data minimization:** Only logs what's necessary, locally
- **Right to be forgotten:** Easy - data never leaves device

### Q: Can we use analytics without crash reporting?

**A:** Yes, but apply the same privacy-first principles:
- Make analytics opt-in, not opt-out
- Clearly explain what data is collected
- Allow users to view collected data
- Provide easy opt-out mechanism
- Consider privacy-focused analytics (e.g., self-hosted Matomo)

### Q: How do we handle crashes in production then?

**A:** Current approach:
1. **Prevent crashes:** Extensive testing, error handling, defensive programming
2. **Graceful degradation:** Catch exceptions, log locally, continue operation
3. **User feedback:** Provide easy ways for users to report issues
4. **Beta testing:** Opt-in crash reporting in beta channel
5. **Monitoring:** Use app-level health checks (local only)

---

## Related Documentation

### AVA Documentation

| Document | Purpose | Location |
|----------|---------|----------|
| **AVA-TECHNOLOGY-STACK.md** | Technology overview | `/docs/AVA-TECHNOLOGY-STACK.md` |
| **Developer-Manual-Complete.md** | Full developer guide | `/docs/Developer-Manual-Complete.md` |
| **ARCHITECTURE.md** | System architecture | `/docs/ARCHITECTURE.md` |

### Code References

| Component | Location |
|-----------|----------|
| **CrashReporter** | `Universal/AVA/Core/Common/src/androidMain/kotlin/com/augmentalis/ava/crashreporting/CrashReporter.kt` |
| **Timber Initialization** | `apps/ava-standalone/src/main/java/com/augmentalis/ava/AvaApplication.kt` |

### External Resources

| Resource | URL |
|----------|-----|
| **Firebase Crashlytics Docs** | https://firebase.google.com/docs/crashlytics |
| **Timber Logging** | https://github.com/JakeWharton/timber |
| **GDPR Compliance** | https://gdpr.eu/ |
| **CCPA Overview** | https://oag.ca.gov/privacy/ccpa |

---

## Change Log

| Date | Change | Updated By |
|------|--------|------------|
| 2025-12-01 | Initial crash reporting documentation created | AVA AI Team |
| 2025-12-01 | Added privacy-first architecture details | AVA AI Team |
| 2025-12-01 | Documented local-only implementation | AVA AI Team |
| 2025-12-01 | Added Firebase Crashlytics opt-in instructions | AVA AI Team |
| 2025-12-01 | Added testing procedures and FAQ | AVA AI Team |

---

## Summary

AVA's crash reporting system is designed with **privacy as the foundation**, not an afterthought. By keeping crash data local by default and providing clear opt-in mechanisms for cloud reporting, we respect user sovereignty while maintaining the ability to diagnose issues when needed.

**Key Takeaways:**

1. ✅ **Privacy-first by design** - No data leaves device by default
2. ✅ **User control** - Clear opt-in mechanism via Settings
3. ✅ **Compliance ready** - GDPR/CCPA compliant by default
4. ✅ **Developer friendly** - Local logs accessible via ADB
5. ✅ **Future ready** - Firebase integration prepared but disabled
6. ✅ **Transparent** - Clear documentation of what data goes where

**This is not a missing feature - this is a deliberate design choice that prioritizes user privacy.**

---

**Classification:** INTERNAL DEVELOPMENT REFERENCE
**Distribution:** Development Team Only
**Authority:** DEFINITIVE - Privacy-First Crash Reporting
**Last Updated:** 2025-12-01
**Version:** 1.0
