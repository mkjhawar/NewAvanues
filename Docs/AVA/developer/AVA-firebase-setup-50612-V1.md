# Firebase Setup Guide for AVA

**Version:** 1.0
**Date:** 2025-12-06
**Author:** AVA AI Team

---

## Overview

This guide walks you through setting up Firebase Crashlytics for the AVA project. Firebase Crashlytics provides real-time crash reporting to help you track, prioritize, and fix stability issues.

**Privacy-First Design:** Firebase is completely optional. AVA works perfectly without Firebase, using local Timber logging for crash reports. Firebase is only activated when you explicitly configure it.

---

## Prerequisites

- Firebase account (free tier works fine)
- AVA project cloned and building successfully
- Android Studio installed
- Access to Firebase Console

---

## Step-by-Step Setup

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click **"Add project"** (or select existing project)
3. Enter project name: **"AVA AI Assistant"**
4. Google Analytics: **Optional** (recommended for analytics)
5. Click **"Create Project"**

**Note:** You can use the same Firebase project for development and production, or create separate projects for each environment.

---

### 2. Add Android App to Firebase

1. In Firebase Console, click **"Add app"** → Select **Android** icon
2. Fill in app details:
   - **Android package name:** `com.augmentalis.ava`
     ⚠️ **IMPORTANT:** Must match exactly, case-sensitive
   - **App nickname:** `AVA Android` (for identification)
   - **Debug SHA-1:** Optional (only needed for Firebase Auth)

3. Click **"Register app"**

---

### 3. Download google-services.json

1. Firebase will show a download button for `google-services.json`
2. Click **"Download google-services.json"**
3. Copy the file to: `/Volumes/M-Drive/Coding/AVA/android/ava/google-services.json`

**File location is critical:**
```
AVA/
├── android/
│   └── ava/
│       ├── google-services.json  ← Place here
│       ├── build.gradle.kts
│       └── src/
```

**Security Note:** The `google-services.json` file is already in `.gitignore`. Never commit it to Git!

---

### 4. Enable Crashlytics in Firebase Console

1. In Firebase Console, navigate to: **Build** → **Crashlytics**
2. Click **"Get started"**
3. Follow the setup wizard:
   - ✅ SDK integration already complete (conditional in build.gradle)
   - ✅ Plugins already applied
   - ⚠️ You'll need to trigger a test crash to complete setup
4. Click **"Finish setup"**

---

### 5. Build Project

The build configuration is already set up with **conditional Firebase support**:

```bash
cd /Volumes/M-Drive/Coding/AVA

# Build without google-services.json (works fine, uses local logging)
./gradlew assembleDebug

# Add google-services.json (from step 3)
# Build with Firebase (Crashlytics enabled)
./gradlew assembleDebug
```

**What happens:**
- **Without google-services.json:** Build succeeds, CrashReporter uses Timber logging
- **With google-services.json:** Build succeeds, CrashReporter uses Firebase Crashlytics

---

### 6. Test Crash Reporting

#### Method 1: Via Settings UI (Recommended)

1. Install app on device: `./gradlew installDebug`
2. Open AVA app
3. Navigate to: **Settings** → **Privacy** → **Crash Reporting**
4. Enable **"Crash Reporting"** toggle
5. Trigger a test crash (see Method 2)

#### Method 2: Test Crash Button

Add this temporary code to MainActivity or create a test button:

```kotlin
// In MainActivity.kt or SettingsScreen.kt
Button(onClick = {
    // Test non-fatal exception
    CrashReporter.recordException(
        RuntimeException("Test non-fatal exception"),
        "Testing Firebase Crashlytics integration"
    )

    // Test breadcrumbs
    CrashReporter.log("Test", "User clicked test crash button")

    // Test custom keys
    CrashReporter.setCustomKey("test_mode", true)
    CrashReporter.setCustomKey("test_timestamp", System.currentTimeMillis())
}) {
    Text("Test Crash Report (Non-Fatal)")
}

// For fatal crash test (CAUTION: This will crash the app!)
Button(onClick = {
    throw RuntimeException("Test fatal crash for Firebase setup")
}) {
    Text("Test Fatal Crash")
}
```

---

### 7. Verify in Firebase Console

1. Run the test crash (from step 6)
2. **For fatal crashes:** Restart the app (crashes upload on next launch)
3. Wait **1-2 minutes** for processing
4. Go to Firebase Console → **Crashlytics**
5. You should see:
   - Crash report with stack trace
   - Device information (manufacturer, model, OS version)
   - Custom keys (test_mode, test_timestamp)
   - Breadcrumbs (user actions)

**Troubleshooting:** If crashes don't appear after 5 minutes:
- Check logcat for "CrashReporter" logs
- Verify `CrashReporter.isEnabled()` returns `true`
- Ensure device has internet connection
- Try force-closing and reopening the app

---

## Configuration Options

### Crashlytics Settings (Firebase Console)

Navigate to: **Crashlytics** → **Settings** (gear icon)

| Setting | Recommended Value | Purpose |
|---------|-------------------|---------|
| Data retention | 90 days | Balance history vs storage costs |
| Email notifications | Enabled | Alert on new crash types |
| Velocity alerts | Enabled | Detect crash rate spikes |
| ANR tracking | Enabled | Track app freezes (Android only) |
| NDK crash reporting | Enabled | Track native library crashes |

### Debug vs Release Builds

**Debug Builds:**
- Crashlytics enabled if user opts in
- Full stack traces with line numbers
- Verbose logging

**Release Builds:**
- Crashlytics enabled if user opts in
- ProGuard mapping files uploaded automatically
- Stack traces deobfuscated in Firebase Console

---

## Privacy & Compliance

### What Data is Collected?

When crash reporting is **enabled**, Firebase Crashlytics collects:

✅ **Collected:**
- Device manufacturer and model (e.g., "Google Pixel 8")
- Android OS version (e.g., "Android 14")
- App version (e.g., "1.0.0-alpha01")
- Stack traces (code execution path)
- Custom keys (app state at crash time)
- Breadcrumbs (user actions before crash)
- Crash timestamp

❌ **NOT Collected:**
- User names, emails, or phone numbers
- Location data
- Personal messages or content
- Contacts or calendar data
- Any personally identifiable information (PII)

### User Consent

AVA implements **opt-in crash reporting**:

1. **Default State:** Disabled (privacy-first)
2. **User Control:** Settings → Privacy → Crash Reporting
3. **Runtime Toggle:** Can be enabled/disabled anytime
4. **Transparency:** Clear disclosure of collected data

### GDPR & CCPA Compliance

- ✅ Crash reporting disabled by default
- ✅ Explicit user consent required
- ✅ No PII collected
- ✅ User can disable anytime
- ✅ Data retention configurable (default: 90 days)

---

## Integration with AVA

### Application.onCreate()

```kotlin
// In AvaApplication.kt
class AvaApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber first
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize CrashReporter (disabled by default)
        val crashReportingEnabled = settingsRepository.isCrashReportingEnabled()
        CrashReporter.initialize(this, enabled = crashReportingEnabled)

        if (crashReportingEnabled) {
            // Set anonymized user ID
            val userId = UUID.randomUUID().toString()
            CrashReporter.setUserId(userId)
        }
    }
}
```

### Settings UI Integration

```kotlin
// In SettingsViewModel.kt
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val crashReportingEnabled: StateFlow<Boolean> =
        settingsRepository.crashReportingEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onCrashReportingToggled(enabled: Boolean) {
        viewModelScope.launch {
            // Save preference
            settingsRepository.setCrashReportingEnabled(enabled)

            // Update CrashReporter
            CrashReporter.setEnabled(enabled)

            if (enabled) {
                // Set anonymized user ID
                val userId = UUID.randomUUID().toString()
                CrashReporter.setUserId(userId)

                // Log app state
                CrashReporter.setCustomKey("app_version", BuildConfig.VERSION_NAME)
                CrashReporter.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            }
        }
    }
}
```

### Crash Reporting Usage

```kotlin
// In any ViewModel or Repository
try {
    // Risky operation
    processUserQuery(query)
} catch (e: Exception) {
    // Log non-fatal exception
    CrashReporter.recordException(e, "Failed to process user query")

    // Add context
    CrashReporter.setCustomKey("query_length", query.length)
    CrashReporter.setCustomKey("llm_model", currentModel)

    // Continue execution with fallback
    return fallbackResponse()
}

// Log breadcrumbs for debugging
CrashReporter.log("User", "Opened chat screen")
CrashReporter.log("Voice", "Started recording")
CrashReporter.log("LLM", "Generated response (tokens: $tokenCount)")
```

---

## Troubleshooting

### Issue: "google-services.json not found"

**Symptom:** Build fails with error about missing `google-services.json`

**Solution:** This should NOT happen with AVA's conditional configuration. If you see this error:

1. Check `android/ava/build.gradle.kts` has conditional plugin application:
   ```kotlin
   val googleServicesFile = file("google-services.json")
   if (googleServicesFile.exists()) {
       apply(plugin = "com.google.gms.google-services")
       apply(plugin = "com.google.firebase.crashlytics")
   }
   ```

2. Clean and rebuild:
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

---

### Issue: "Firebase Crashlytics SDK not available"

**Symptom:** Logs show "Firebase Crashlytics SDK not found in classpath"

**Solution:** This is **expected behavior** when building without Firebase. The app will use local Timber logging instead. No action needed unless you want Firebase integration.

To enable Firebase:
1. Add `google-services.json` to `android/ava/`
2. Rebuild project

---

### Issue: Crashes not appearing in Firebase Console

**Symptom:** Test crashes don't show up in Crashlytics dashboard

**Troubleshooting Steps:**

1. **Check if crash reporting is enabled:**
   ```kotlin
   Log.d("Debug", "CrashReporter enabled: ${CrashReporter.isEnabled()}")
   Log.d("Debug", "Firebase available: ${CrashReporter.isFirebaseAvailable()}")
   ```

2. **Wait for upload:** Crashlytics uploads after app restarts (1-2 minutes)

3. **Check internet connection:** Crashes only upload when online

4. **Check Firebase Console:**
   - Verify app is registered with correct package name
   - Check "Data freshness" timestamp
   - Look in both "Crashes" and "Non-fatals" tabs

5. **Enable debug logging:**
   ```bash
   adb shell setprop log.tag.FirebaseCrashlytics DEBUG
   adb logcat -s FirebaseCrashlytics
   ```

---

### Issue: Stack traces are obfuscated in release builds

**Symptom:** Release build crashes show obfuscated class/method names

**Solution:** Upload ProGuard mapping files to Firebase:

1. Build release APK: `./gradlew assembleRelease`
2. Mapping file is auto-generated: `android/ava/build/outputs/mapping/release/mapping.txt`
3. Firebase Crashlytics Gradle plugin auto-uploads mapping files
4. Verify in Firebase Console → Crashlytics → Settings → ProGuard/R8 mappings

**Manual upload (if needed):**
```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login
firebase login

# Upload mapping file
firebase crashlytics:symbols:upload \
  --app=YOUR_FIREBASE_APP_ID \
  android/ava/build/outputs/mapping/release/mapping.txt
```

---

### Issue: Crash reporting enabled but preference not persisting

**Symptom:** User enables crash reporting, but it's disabled after app restart

**Solution:** Ensure preference is saved in `SettingsRepository`:

```kotlin
// In SettingsRepository.kt
suspend fun setCrashReportingEnabled(enabled: Boolean) {
    dataStore.edit { prefs ->
        prefs[CRASH_REPORTING_ENABLED] = enabled
    }
}

val crashReportingEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[CRASH_REPORTING_ENABLED] ?: false
}
```

---

## Build Commands

```bash
# Clean build
./gradlew clean

# Debug build (without Firebase)
./gradlew assembleDebug

# Debug build (with Firebase, after adding google-services.json)
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Run unit tests
./gradlew test

# Generate coverage report
./gradlew jacocoTestReport
```

---

## Advanced Configuration

### Custom Crash Keys

Add app-specific metadata to crash reports:

```kotlin
// In Application.onCreate() or when state changes
CrashReporter.setCustomKey("llm_model", "qwen2.5-0.5b")
CrashReporter.setCustomKey("rag_enabled", true)
CrashReporter.setCustomKey("voice_mode", "wake_word")
CrashReporter.setCustomKey("overlay_active", false)

// Batch logging
CrashReporter.logAppState(mapOf(
    "llm_ready" to isLLMReady,
    "rag_chunks_count" to ragChunkCount,
    "battery_level" to batteryPercent
))
```

### Breadcrumb Logging

Track user journey leading to crashes:

```kotlin
// User actions
CrashReporter.log("Navigation", "Navigated to settings screen")
CrashReporter.log("User Input", "User typed query: length=${query.length}")

// App state changes
CrashReporter.log("LLM", "Started model inference")
CrashReporter.log("LLM", "Generated ${tokenCount} tokens")

// System events
CrashReporter.log("System", "Low memory warning received")
CrashReporter.log("Network", "WiFi connected")
```

### NDK Crash Reporting (Native Libraries)

AVA uses native libraries (llama.cpp, ONNX Runtime). To enable NDK crash reporting:

1. Already configured in `build.gradle.kts`:
   ```kotlin
   android {
       buildTypes {
           release {
               ndk {
                   debugSymbolLevel = "FULL"  // Upload native symbols
               }
           }
       }
   }
   ```

2. Native symbols auto-uploaded by Crashlytics Gradle plugin

3. Crashes from native code appear in Crashlytics with full stack traces

---

## Testing Checklist

Before releasing with Firebase:

- [ ] `google-services.json` added to `android/ava/`
- [ ] `google-services.json` is in `.gitignore`
- [ ] Build succeeds with and without `google-services.json`
- [ ] CrashReporter disabled by default (privacy-first)
- [ ] Settings UI has crash reporting toggle
- [ ] Test non-fatal exception appears in Firebase Console
- [ ] Test fatal crash appears in Firebase Console
- [ ] Breadcrumbs and custom keys visible in crash reports
- [ ] ProGuard rules preserve stack traces
- [ ] NDK crashes reported (if using native libs)
- [ ] Privacy policy updated to mention crash reporting
- [ ] User consent flow implemented

---

## Production Deployment Checklist

- [ ] Firebase project created (production)
- [ ] `google-services.json` for production environment
- [ ] Crashlytics enabled in Firebase Console
- [ ] Email notifications configured
- [ ] Velocity alerts configured
- [ ] ProGuard mapping files uploading correctly
- [ ] Privacy policy includes crash reporting disclosure
- [ ] User consent toggle in Settings UI
- [ ] Default state is disabled (opt-in)
- [ ] Release build tested with real crashes

---

## Resources

### Firebase Documentation
- [Firebase Crashlytics Overview](https://firebase.google.com/docs/crashlytics)
- [Crashlytics Android Setup](https://firebase.google.com/docs/crashlytics/get-started?platform=android)
- [Customize Crash Reports](https://firebase.google.com/docs/crashlytics/customize-crash-reports)
- [ProGuard Mapping Files](https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports)

### AVA Documentation
- [CrashReporter Implementation](/Volumes/M-Drive/Coding/AVA/docs/AVA-CRASH-REPORTING.md)
- [Developer Manual Chapter 73](/Volumes/M-Drive/Coding/AVA/docs/ideacode/guides/Developer-Manual-Chapter73-Production-Readiness-Security.md)

### Android Documentation
- [Android Logging](https://developer.android.com/reference/android/util/Log)
- [ProGuard Configuration](https://www.guardsquare.com/manual/configuration/usage)

---

## Support

**Questions or Issues?**
- Check [Firebase Console](https://console.firebase.google.com)
- Review [Troubleshooting](#troubleshooting) section
- Check AVA project documentation

**Privacy Concerns?**
- AVA is privacy-first by design
- Crash reporting is completely optional
- No PII is ever collected
- Users maintain full control

---

**Version:** 1.0
**Last Updated:** 2025-12-06
**Author:** AVA AI Team
**License:** Proprietary - Augmentalis Inc
