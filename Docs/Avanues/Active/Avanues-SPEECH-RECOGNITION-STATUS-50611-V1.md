# SpeechRecognition Module - Current Status & Testing Plan

**Date**: 2025-11-06
**Module**: android/avanues/libraries/speechrecognition
**Status**: ⚠️ **NEEDS VERIFICATION**
**Before Migration**: Must confirm working state

---

## Current State (from README.md)

### ✅ What's Working

1. **WhisperEngine** - ✅ **FULLY FUNCTIONAL**
   - Native whisper.cpp integration
   - 99+ languages supported
   - Offline recognition
   - ARMv7 and ARM64 support
   - Model management UI (Compose)

2. **AndroidSTTEngine** - ✅ **Fully functional**
   - Built-in Android speech recognition
   - Online only
   - Limited languages

3. **VoskEngine** - ✅ **Fully functional**
   - Offline recognition
   - Open-source
   - Model-based

### ⚠️ What's Disabled/Uncertain

4. **GoogleCloudEngine** - 🚫 **Temporarily disabled**
   - Falls back to Android STT
   - REST API implementation
   - Needs testing

5. **VivokaEngine** - ⚠️ **Requires external SDK**
   - Requires `vsdk-6.0.0.aar` file
   - May not be available

---

## Architecture Overview

```
SpeechRecognition Library
├── Multiple Engines (pluggable)
│   ├── WhisperEngine (OpenAI - offline, 99+ langs)
│   ├── VoskEngine (offline, open-source)
│   ├── AndroidSTTEngine (built-in Android)
│   ├── GoogleCloudEngine (REST API - disabled)
│   └── VivokaEngine (enterprise - requires SDK)
├── Common Components (72% code reuse)
│   ├── CommandCache (thread-safe)
│   ├── TimeoutManager (coroutine-based)
│   ├── ResultProcessor (confidence filtering)
│   └── ServiceState (state management)
├── Configuration (SpeechConfig)
├── Models (RecognitionResult)
└── UI Components (Whisper model download)
```

---

## Testing Status

### Available Tests

From source scan:
```
src/test/java/com/augmentalis/speechrecognition/
├── SpeechRecognitionManagerTest.kt
├── CompilationTest.kt
├── SmokeTest.kt
├── performance/
│   └── SpeechRecognitionPerformanceTest.kt
└── speechengines/
    └── AndroidSTTEngineIntegrationTest.kt
```

### Test Commands

```bash
# Unit tests
./gradlew :android:avanues:libraries:speechrecognition:test

# Integration tests
./gradlew :android:avanues:libraries:speechrecognition:connectedAndroidTest
```

---

## Verification Plan

### Phase 1: Build Verification ✅ (COMPLETE per README)

```bash
# Verify clean build
./gradlew :android:avanues:libraries:speechrecognition:clean
./gradlew :android:avanues:libraries:speechrecognition:build
```

**Expected Result**:
- ✅ 0 compilation errors
- ✅ Native libraries built (ARM64, ARMv7)
- ✅ AAR generated

**Status from README**: ✅ **Build successful (2025-08-31)**

### Phase 2: Unit Tests ⚠️ (NEEDS VERIFICATION)

```bash
# Run all unit tests
./gradlew :android:avanues:libraries:speechrecognition:test

# Run specific tests
./gradlew :android:avanues:libraries:speechrecognition:test --tests "*SmokeTest*"
./gradlew :android:avanues:libraries:speechrecognition:test --tests "*CompilationTest*"
```

**What to Check**:
- ✅ CompilationTest passes (verifies code compiles)
- ✅ SmokeTest passes (basic functionality)
- ✅ SpeechRecognitionManagerTest passes

**Action Required**: **RUN THESE TESTS NOW**

### Phase 3: Integration Tests ⚠️ (NEEDS DEVICE)

```bash
# Requires Android device/emulator connected
adb devices  # Verify device connected

# Run integration tests
./gradlew :android:avanues:libraries:speechrecognition:connectedAndroidTest

# Run specific engine tests
./gradlew :android:avanues:libraries:speechrecognition:connectedAndroidTest \
  --tests "*AndroidSTTEngineIntegrationTest*"
```

**What to Check**:
- ✅ AndroidSTTEngine works on real device
- ✅ Audio recording works (RECORD_AUDIO permission)
- ✅ Recognition returns results

**Action Required**: **RUN WITH DEVICE**

### Phase 4: Manual Testing (Critical!) ⚠️

#### Test App Needed

We need a simple test app to verify:
1. **WhisperEngine** (offline, primary engine)
2. **AndroidSTTEngine** (fallback)
3. **VoskEngine** (offline alternative)

**Option A**: Create minimal test app
```
apps/speechtest/
├── MainActivity.kt
└── Simple UI to test recognition
```

**Option B**: Use existing `avanuelaunch` or `avauidemo` app
```
Add speech recognition button to existing app
```

#### Manual Test Checklist

**WhisperEngine Test**:
```kotlin
// In test app MainActivity
val whisperEngine = WhisperEngine(context)

lifecycleScope.launch {
    // 1. Check if models exist
    val hasModel = whisperEngine.hasDownloadedModel()

    // 2. If not, download tiny model (39 MB)
    if (!hasModel) {
        // Show WhisperModelDownloadDialog
        // Download tiny model
    }

    // 3. Initialize
    val config = SpeechConfig(
        mode = SpeechMode.FREE_SPEECH,
        confidenceThreshold = 0.5f
    )
    val success = whisperEngine.initialize(config)

    // 4. Start listening
    if (success) {
        whisperEngine.startListening()
    }
}

// 5. Handle results
whisperEngine.setResultListener { result ->
    Log.d("Test", "✅ Recognized: ${result.text}")
    Log.d("Test", "✅ Confidence: ${result.confidence}")
    Log.d("Test", "✅ Language: ${result.language}")
}
```

**Test Scenarios**:
1. ✅ Say "hello world" → Should recognize
2. ✅ Say "open settings" → Should recognize
3. ✅ Test non-English (if multilingual needed)
4. ✅ Test with background noise
5. ✅ Test timeout (stop speaking for 5 seconds)
6. ✅ Test stop/restart

**Success Criteria**:
- [ ] Recognizes simple commands (>80% accuracy)
- [ ] Confidence scores are reasonable (>0.5 for clear speech)
- [ ] No crashes or freezes
- [ ] Can stop/restart without issues
- [ ] Memory usage acceptable (<100 MB)

---

## Known Issues (from README)

### 1. GoogleCloudEngine Disabled
**Status**: 🚫 Temporarily disabled, falls back to Android STT
**Impact**: Not critical (we have 3 working engines)
**Action**: Can ignore for now, or fix later

### 2. VivokaEngine Requires SDK
**Status**: ⚠️ Requires `vsdk-6.0.0.aar` file
**Impact**: Not critical if Whisper/Android STT work
**Action**: Remove if SDK not available

### 3. x86/x86_64 Support Removed
**Status**: ℹ️ Intentionally removed to reduce APK size
**Impact**: Won't work on Intel-based Android devices (rare)
**Action**: None needed (acceptable trade-off)

---

## Dependencies Status

### Required Dependencies ✅
```gradle
// Core functionality
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") ✅
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") ✅

// Whisper (native, bundled)
Native whisper.cpp libraries (built in module) ✅

// Android STT (built-in Android)
No external dependencies ✅

// VOSK
implementation("com.alphacephei:vosk-android:0.3.47") ✅
```

### Optional Dependencies ⚠️
```gradle
// Google Cloud (REST API - disabled)
implementation("com.squareup.okhttp3:okhttp:4.12.0") ⚠️
implementation("com.google.code.gson:gson:2.10.1") ⚠️
// Can remove if not fixing GoogleCloudEngine

// Vivoka (requires external SDK)
implementation(files("../../Vivoka/vsdk-6.0.0.aar")) ❌
implementation(files("../../Vivoka/vsdk-csdk-asr-2.0.0.aar")) ❌
implementation(files("../../Vivoka/vsdk-csdk-core-1.0.1.aar")) ❌
// These files may not exist - check and remove if missing
```

### Action: Verify Dependencies
```bash
# Check if Vivoka SDK files exist
ls -la /Volumes/M-Drive/Coding/Avanues/Vivoka/

# If not found, remove from build.gradle.kts
```

---

## Recommendation

### Before Plugin Migration

**MUST DO** (Critical):
1. ✅ **Run unit tests**
   ```bash
   ./gradlew :android:avanues:libraries:speechrecognition:test
   ```
   Expected: All tests pass

2. ✅ **Run integration tests** (with device)
   ```bash
   ./gradlew :android:avanues:libraries:speechrecognition:connectedAndroidTest
   ```
   Expected: AndroidSTTEngine test passes

3. ⚠️ **Manual testing** (CRITICAL!)
   - Create simple test app
   - Test WhisperEngine (primary)
   - Test AndroidSTTEngine (fallback)
   - Verify actual speech recognition works
   - Verify no crashes
   - Verify memory usage acceptable

**SHOULD DO** (Important):
4. ⚠️ **Check Vivoka SDK availability**
   - If SDK files missing, remove VivokaEngine code
   - Clean up build.gradle.kts

5. ⚠️ **Fix or remove GoogleCloudEngine**
   - Either fix the REST API implementation
   - Or remove the code entirely

**NICE TO HAVE**:
6. ℹ️ Document test results
7. ℹ️ Create performance benchmarks

---

## Test App Template

### Minimal Test Activity

```kotlin
// apps/speechtest/MainActivity.kt
class SpeechTestActivity : ComponentActivity() {
    private lateinit var whisperEngine: WhisperEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request microphone permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO
        )

        setContent {
            SpeechTestScreen()
        }
    }

    @Composable
    fun SpeechTestScreen() {
        var isListening by remember { mutableStateOf(false) }
        var lastResult by remember { mutableStateOf("") }
        var statusText by remember { mutableStateOf("Ready") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SpeechRecognition Test", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(32.dp))

            // Status
            Text(statusText, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(16.dp))

            // Last result
            if (lastResult.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Result: $lastResult",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Start/Stop button
            Button(
                onClick = {
                    if (isListening) {
                        stopListening()
                        isListening = false
                    } else {
                        startListening { result ->
                            lastResult = result.text
                            statusText = "Confidence: ${result.confidence}"
                        }
                        isListening = true
                    }
                }
            ) {
                Text(if (isListening) "Stop Listening" else "Start Listening")
            }
        }
    }

    private fun startListening(onResult: (RecognitionResult) -> Unit) {
        lifecycleScope.launch {
            whisperEngine = WhisperEngine(this@SpeechTestActivity)

            val config = SpeechConfig(
                mode = SpeechMode.FREE_SPEECH,
                confidenceThreshold = 0.5f
            )

            val success = whisperEngine.initialize(config)
            if (success) {
                whisperEngine.setResultListener(onResult)
                whisperEngine.startListening()
            }
        }
    }

    private fun stopListening() {
        whisperEngine.stopListening()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 1
    }
}
```

---

## Decision Point

### Can We Migrate to Plugin NOW?

**NO** ❌ - Not yet ready

**Reasons**:
1. ⚠️ No confirmation tests have actually run
2. ⚠️ No manual testing performed
3. ⚠️ Unknown if engines actually work
4. ⚠️ Vivoka SDK dependency unclear
5. ⚠️ GoogleCloudEngine disabled

### What's Needed FIRST

```
┌─────────────────────────────────────────────┐
│  CRITICAL PATH TO PLUGIN MIGRATION          │
├─────────────────────────────────────────────┤
│  1. Run unit tests            [REQUIRED]    │
│  2. Run integration tests     [REQUIRED]    │
│  3. Manual test (WhisperEngine) [CRITICAL!] │
│  4. Clean up Vivoka/Google    [IMPORTANT]   │
│  5. Document working state    [IMPORTANT]   │
│  ↓                                           │
│  THEN migrate to plugin                     │
└─────────────────────────────────────────────┘
```

---

## Next Steps

### Immediate Actions (Today)

1. **Run tests**:
   ```bash
   cd /Volumes/M-Drive/Coding/Avanues
   ./gradlew :android:avanues:libraries:speechrecognition:test
   ```

2. **Check results**:
   - Review test output
   - Identify any failures
   - Fix critical issues

3. **Connect device and run integration tests**:
   ```bash
   adb devices
   ./gradlew :android:avanues:libraries:speechrecognition:connectedAndroidTest
   ```

4. **Report findings**:
   - Which engines work?
   - Any crashes?
   - Test coverage?

### Short-term Actions (This Week)

5. **Create test app** (if tests pass)
   - Simple UI with "Start/Stop" button
   - Test WhisperEngine recognition
   - Test multiple phrases
   - Verify memory usage

6. **Clean up dependencies**
   - Check Vivoka SDK availability
   - Remove or fix GoogleCloudEngine
   - Update build.gradle.kts

7. **Document results**
   - "SpeechRecognition VERIFIED - Ready for Plugin Migration"
   - Or "Issues found - Need fixes before migration"

---

## Risk Assessment

### High Risk ⚠️
- **No verification testing done** → Don't know if it actually works
- **Missing SDK dependencies** → May break at runtime
- **Disabled engines** → Reduced functionality

### Medium Risk ⚠️
- **Old README date** → May not reflect current state
- **Multiple engines** → Complexity in testing all paths
- **Native libraries** → Platform-specific issues possible

### Low Risk ✅
- **Build successful** → At least compiles
- **Multiple engine options** → Fallbacks available
- **Well-documented** → Good starting point

---

## Success Criteria

### Ready for Plugin Migration When:

- [x] README exists with clear documentation
- [ ] All unit tests pass (100% pass rate)
- [ ] Integration tests pass on real device
- [ ] Manual test confirms WhisperEngine works
- [ ] No crashes during start/stop/recognize
- [ ] Memory usage acceptable (<100 MB)
- [ ] Dependencies cleaned up (no missing files)
- [ ] Documentation updated with verification date
- [ ] Confident we can recreate working state after migration

**Current Status**: **3/9 criteria met** (33%)

---

## Conclusion

**SpeechRecognition module is NOT READY for plugin migration yet.**

**Reason**: No verification that it actually works beyond compilation.

**Action Required**: **Run tests and manual verification FIRST**

**Timeline**:
- Tests: 1-2 hours
- Manual testing: 2-4 hours
- Cleanup: 1-2 hours
- **Total: 4-8 hours before migration**

**After verification**: Use `ideacode_port_module` tool to migrate to plugin architecture.

---

**Status**: ⚠️ **VERIFICATION NEEDED**
**Priority**: **HIGH**
**Blocker**: Manual testing required before plugin migration

---

**Document Version**: 1.0.0
**Author**: Claude Code (Sonnet 4.5)
**Date**: 2025-11-06
