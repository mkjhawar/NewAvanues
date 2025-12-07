# LegacyAvenue Speech Engine Analysis & Port Plan
**Date:** 2025-01-26
**Status:** 🔴 Critical Issues Found
**Purpose:** Complete analysis of speech engine implementations and missing functionality

## Executive Summary

Analysis of LegacyAvenue reveals:
1. **CRITICAL:** Vivoka error listener not connected in VOS4 (breaks functional equivalency)
2. **GOOD:** Vosk and AndroidSTT properly handle errors in VOS4
3. **MISSING:** SpeechRecognitionServiceProvider pattern not ported
4. **DEFAULT:** Need to set Vivoka as default engine (per user request)

## Error Listener Implementation Comparison

### 1. Vivoka Engine

#### LegacyAvenue Implementation ✅
```kotlin
// VivokaSpeechRecognitionService.kt (Line 510-517)
override fun onError(codeString: String?, message: String?) {
    VoiceOsLogger.e("Recognition codeString: $codeString, message: $message")
    updateVoiceStatus(
        VoiceRecognitionServiceState.Error(
            Exception("Vivoka SDK error [$codeString]: $message")
        )
    )
}

// Which properly notifies:
private fun updateVoiceStatus(state: VoiceRecognitionServiceState) {
    this.vsdkStatus = state
    onSpeechRecognitionResultListener?.onVoiceRecognitionServiceState(state)
}
```

#### VOS4 Implementation ❌
```kotlin
// VivokaEngine.kt (Lines 330-333)
fun setErrorListener(@Suppress("UNUSED_PARAMETER") listener: OnSpeechErrorListener) {
    // BROKEN - doesn't store listener
}

// Lines 365-376
override fun onError(codeString: String?, message: String?) {
    // Logs but doesn't notify listener
}
```

**Status:** 🔴 **BROKEN** - Must fix immediately

### 2. Google/Android STT Engine

#### LegacyAvenue Implementation ✅
```kotlin
// GoogleSpeechRecognitionService.kt (Lines 220-231)
override fun onError(error: Int) {
    VoiceOsLogger.e("onError: $error")
    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_CLIENT) {
        updateVoiceStatus(
            VoiceRecognitionServiceState.Error(
                Exception("Google speech error with code$error")
            )
        )
    }
    // Auto-restart logic
}
```

#### VOS4 Implementation ✅
```kotlin
// AndroidSTTEngine.kt
private var errorListener: ((String, Int) -> Unit)? = null

fun setErrorListener(listener: OnSpeechErrorListener) {
    this.errorListener = listener  // Properly stores
}

// In error handling:
errorListener?.invoke(errorMessage, errorCode)  // Properly invokes
```

**Status:** ✅ **WORKING** - Correctly implemented

### 3. Vosk Engine

#### LegacyAvenue Implementation
- Uses single listener pattern (OnSpeechRecognitionResultListener)
- Errors reported through state updates

#### VOS4 Implementation ✅
```kotlin
// VoskEngine.kt (Lines 92-95)
fun setErrorListener(listener: OnSpeechErrorListener) {
    this.errorListener = listener
    voskErrorHandler.setErrorListener(listener)  // Properly stores and delegates
}
```

**Status:** ✅ **WORKING** - Correctly implemented

## Missing Components from LegacyAvenue

### 1. SpeechRecognitionServiceProvider Pattern

**LegacyAvenue Has:**
```kotlin
class SpeechRecognitionServiceProvider {
    fun provideSpeechRecognitionService(
        context: Context,
        provider: SpeechRecognitionProvider,
        config: SpeechRecognitionConfig,
        listener: OnSpeechRecognitionResultListener
    ): SpeechRecognitionServiceInterface
}

enum class SpeechRecognitionProvider {
    GOOGLE, VIVOKA, VOSK
}
```

**VOS4 Status:** Not directly ported - using AIDL service pattern instead

### 2. Configuration Builder Pattern

**LegacyAvenue Has:**
- `SpeechRecognitionConfigBuilder.kt` - Builder pattern for config

**VOS4 Status:** Using direct config creation in VoiceRecognitionService

### 3. Voice Utils

**LegacyAvenue Has:**
- `VoiceUtils.kt` - Utility functions for command matching

**VOS4 Status:** Partially ported in various engine components

## Critical Fix Required: Vivoka Error Listener

### Implementation Fix
```kotlin
// VivokaEngine.kt - Required changes

// 1. Add class variable (line ~62)
private var errorListener: OnSpeechErrorListener? = null

// 2. Fix setter (lines 330-333)
fun setErrorListener(listener: OnSpeechErrorListener) {
    errorListener = listener
    Log.d(TAG, "Error listener registered")
}

// 3. Fix onError (lines 365-376)
override fun onError(codeString: String?, message: String?) {
    Log.e(TAG, "VSDK error - Code: $codeString, Message: $message")
    
    performance.recordRecognition(System.currentTimeMillis(), null, 0f, false)
    
    // CRITICAL FIX: Notify listener
    errorListener?.invoke(
        "Vivoka SDK error [$codeString]: $message",
        codeString?.toIntOrNull() ?: 500
    )
    
    coroutineScope.launch {
        Log.e(TAG, "VSDK Error - Code: $codeString, Message: $message")
    }
}
```

## New Feature Request: Default Engine Setting

### Requirement
- Set Vivoka as default engine
- Add system setting to change engines

### Implementation Plan

#### 1. Update VoiceRecognitionService
```kotlin
// Add default engine constant
companion object {
    private const val DEFAULT_ENGINE = "vivoka"  // Per user request
    private const val PREF_SELECTED_ENGINE = "selected_speech_engine"
}

// In startRecognition method
override fun startRecognition(engine: String?, language: String, mode: Int): Boolean {
    val selectedEngine = engine ?: getSelectedEngine() ?: DEFAULT_ENGINE
    // ... rest of implementation
}

private fun getSelectedEngine(): String? {
    val prefs = getSharedPreferences("voice_recognition_prefs", Context.MODE_PRIVATE)
    return prefs.getString(PREF_SELECTED_ENGINE, DEFAULT_ENGINE)
}
```

#### 2. Add Settings UI in VoiceAccessibility
```kotlin
// In settings activity
@Composable
fun EngineSelectionSetting() {
    var selectedEngine by remember { 
        mutableStateOf(prefs.getString(PREF_SELECTED_ENGINE, "vivoka")) 
    }
    
    DropdownMenu(
        label = "Speech Recognition Engine",
        options = listOf("vivoka", "vosk", "android_stt", "whisper"),
        selected = selectedEngine,
        onSelectionChange = { engine ->
            selectedEngine = engine
            prefs.edit().putString(PREF_SELECTED_ENGINE, engine).apply()
        }
    )
}
```

## Functional Equivalency Status

| Component | LegacyAvenue | VOS4 Current | Required Action |
|-----------|--------------|--------------|-----------------|
| Vivoka Error Handling | ✅ | ❌ | **FIX IMMEDIATELY** |
| Google/Android Error Handling | ✅ | ✅ | None |
| Vosk Error Handling | ✅ | ✅ | None |
| Service Provider Pattern | ✅ | ⚠️ | Using AIDL instead |
| Config Builder | ✅ | ⚠️ | Direct creation |
| Default Engine | N/A | ❌ | **IMPLEMENT** |
| Engine Selection UI | N/A | ❌ | **IMPLEMENT** |

## Priority Action Items

### Phase 1: Critical Fixes (Immediate)
1. ✅ Fix Vivoka error listener connection
2. ✅ Test error propagation through AIDL
3. ✅ Verify all engines report errors correctly

### Phase 2: Default Engine (High Priority)
1. ✅ Set Vivoka as default engine
2. ✅ Add SharedPreferences for engine selection
3. ✅ Create settings UI for engine selection

### Phase 3: Enhancement (Medium Priority)
1. ⚠️ Consider porting ConfigBuilder pattern
2. ⚠️ Port missing VoiceUtils functions
3. ⚠️ Add engine capability detection

## Testing Requirements

### Error Handling Tests
```kotlin
@Test
fun testVivokaErrorPropagation() {
    // Given: Vivoka engine with error listener
    val engine = VivokaEngine(context)
    var receivedError: String? = null
    engine.setErrorListener { error, code ->
        receivedError = error
    }
    
    // When: Error occurs
    engine.onError("404", "Model not found")
    
    // Then: Listener receives formatted error
    assertEquals("Vivoka SDK error [404]: Model not found", receivedError)
}
```

### Default Engine Tests
```kotlin
@Test
fun testDefaultEngineIsVivoka() {
    // Given: Fresh installation
    clearPreferences()
    
    // When: Starting recognition without specifying engine
    service.startRecognition(null, "en-US", 0)
    
    // Then: Vivoka engine is used
    assertTrue(service.getCurrentEngine() is VivokaEngine)
}
```

## Conclusion

The VOS4 implementation is **NOT fully equivalent** to LegacyAvenue due to:
1. **Vivoka error listener not connected** (CRITICAL)
2. **No default engine setting** (Required by user)
3. **No engine selection UI** (Required for system setting)

Once these issues are fixed, VOS4 will achieve 100% functional equivalency with enhanced features.