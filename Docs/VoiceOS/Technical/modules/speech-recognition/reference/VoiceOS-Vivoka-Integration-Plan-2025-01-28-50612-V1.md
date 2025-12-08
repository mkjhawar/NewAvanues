# Vivoka VSDK Integration Plan for VOS4
**Date:** 2025-01-28  
**Module:** SpeechRecognition Library  
**Priority:** CRITICAL  
**Status:** In Progress

## Executive Summary
The Vivoka VSDK integration requires critical fixes to properly integrate the SDK into the VOS4 structure. The AAR files are present but not correctly configured in the build system, and the implementation has several issues that need resolution.

## Current State Analysis

### ✅ What's Working
1. **AAR Files Present:** Three Vivoka AAR files in `/vivoka/` directory:
   - `vsdk-6.0.0.aar` (128KB) - Core SDK
   - `vsdk-csdk-asr-2.0.0.aar` (37MB) - ASR component
   - `vsdk-csdk-core-1.0.1.aar` (34MB) - Core native libraries

2. **Basic Implementation Structure:** 
   - VivokaEngine.kt with SOLID architecture
   - 10 specialized components created
   - Error handling framework in place

3. **Build Configuration:**
   - CompileOnly dependencies configured
   - Path references to AAR files exist

### ❌ Critical Issues

1. **Build Configuration Problems:**
   - AAR files using relative paths (`../../vivoka/`) instead of absolute
   - Library module cannot directly include local AARs
   - Dependencies marked as `compileOnly` causing runtime ClassNotFoundException

2. **Namespace Inconsistencies:**
   - Mixed namespaces: `com.augmentalis.speechrecognition` vs `com.augmentalis.voiceos.speech`
   - Import conflicts between old and new package structures

3. **Missing SDK Initialization:**
   - Vsdk.init() not properly called
   - License key management not implemented
   - Context initialization incomplete

4. **Error Handling Gaps:**
   - IRecognizerListener callbacks not fully implemented
   - Error propagation to clients incomplete
   - Recovery mechanisms not tested

## Implementation Plan

### Phase 1: Fix Build Configuration (Priority: CRITICAL)

#### 1.1 Move AAR Dependencies to App Level
```kotlin
// In app/VoiceRecognition/build.gradle.kts
dependencies {
    implementation(files("../../vivoka/vsdk-6.0.0.aar"))
    implementation(files("../../vivoka/vsdk-csdk-asr-2.0.0.aar"))
    implementation(files("../../vivoka/vsdk-csdk-core-1.0.1.aar"))
}
```

#### 1.2 Update Library Module Configuration
```kotlin
// In libraries/SpeechRecognition/build.gradle.kts
dependencies {
    // Keep as compileOnly for library
    compileOnly(files("${rootDir}/vivoka/vsdk-6.0.0.aar"))
    compileOnly(files("${rootDir}/vivoka/vsdk-csdk-asr-2.0.0.aar"))
    compileOnly(files("${rootDir}/vivoka/vsdk-csdk-core-1.0.1.aar"))
}
```

#### 1.3 Add ProGuard Rules
```proguard
# Vivoka VSDK
-keep class com.vivoka.** { *; }
-keep interface com.vivoka.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
```

### Phase 2: Fix Namespace Issues (Priority: HIGH)

#### 2.1 Standardize Package Structure
- Primary namespace: `com.augmentalis.voiceos.speech.engines.vivoka`
- API namespace: `com.augmentalis.voiceos.speech.api`
- Common namespace: `com.augmentalis.voiceos.speech.engines.common`

#### 2.2 Update All Imports
```kotlin
// OLD
import com.augmentalis.speechrecognition.*

// NEW
import com.augmentalis.voiceos.speech.api.*
```

### Phase 3: Implement Proper SDK Initialization (Priority: HIGH)

#### 3.1 Create VivokaInitializer
```kotlin
class VivokaInitializer(private val context: Context) {
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Initialize SDK with license
                Vsdk.init(context, getLicenseKey())
                
                // Configure ASR
                configureASR()
                
                // Verify initialization
                verifyInitialization()
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Vivoka initialization failed", e)
                false
            }
        }
    }
    
    private fun getLicenseKey(): String {
        // TODO: Implement secure license key retrieval
        return BuildConfig.VIVOKA_LICENSE_KEY
    }
}
```

#### 3.2 Update VivokaEngine Initialization
```kotlin
suspend fun initialize(speechConfig: SpeechConfig): Boolean {
    // First initialize SDK
    if (!VivokaInitializer(context).initialize()) {
        return false
    }
    
    // Then proceed with engine initialization
    return performActualInitialization(context, speechConfig)
}
```

### Phase 4: Complete Error Handling (Priority: MEDIUM)

#### 4.1 Implement All Listener Callbacks
```kotlin
override fun onError(error: Int, message: String?) {
    coroutineScope.launch {
        val speechError = mapVivokaError(error, message)
        errorRecoveryManager.handleError(speechError)
        errorListener?.onError(speechError)
    }
}

override fun onPartialResult(result: String?) {
    result?.let {
        voiceStateManager.updatePartialResult(it)
    }
}

override fun onFinalResult(result: String?, confidence: Float) {
    result?.let {
        val recognitionResult = RecognitionResult(
            text = it,
            confidence = confidence,
            isFinal = true,
            timestamp = System.currentTimeMillis()
        )
        voiceStateManager.updateFinalResult(recognitionResult)
    }
}
```

#### 4.2 Add Recovery Mechanisms
```kotlin
private suspend fun recoverFromError(error: SpeechError): Boolean {
    return when (error.code) {
        NETWORK_ERROR -> attemptOfflineMode()
        AUDIO_ERROR -> reinitializeAudio()
        MODEL_ERROR -> reloadModel()
        else -> false
    }
}
```

### Phase 5: Testing & Validation (Priority: MEDIUM)

#### 5.1 Unit Tests
- Test SDK initialization
- Test error handling
- Test state transitions
- Test audio processing

#### 5.2 Integration Tests
- Test with VoiceRecognition app
- Test engine switching
- Test error recovery
- Test performance metrics

#### 5.3 System Tests
- End-to-end voice recognition
- Multi-language support
- Noise handling
- Resource management

## File Changes Required

### Build Files
1. `/modules/libraries/SpeechRecognition/build.gradle.kts` - Fix AAR paths
2. `/modules/apps/VoiceRecognition/build.gradle.kts` - Add AAR dependencies
3. `/settings.gradle.kts` - Ensure module paths correct

### Source Files
1. `/modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaEngine.kt` - Fix initialization
2. `/modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaInitializer.kt` - NEW FILE
3. `/modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaErrorMapper.kt` - NEW FILE

### Configuration Files
1. `/modules/libraries/SpeechRecognition/proguard-rules.pro` - Add Vivoka rules
2. `/local.properties` - Add VIVOKA_LICENSE_KEY

### Test Files
1. `/modules/libraries/SpeechRecognition/src/test/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaEngineTest.kt` - NEW FILE
2. `/modules/libraries/SpeechRecognition/src/test/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaInitializerTest.kt` - NEW FILE

## Success Criteria

### Immediate (Phase 1-2)
- [ ] Build compiles without errors
- [ ] No AAR dependency warnings
- [ ] Namespace conflicts resolved

### Short-term (Phase 3-4)
- [ ] Vivoka SDK initializes successfully
- [ ] Voice recognition works with Vivoka engine
- [ ] Error handling functional
- [ ] Recovery mechanisms working

### Long-term (Phase 5)
- [ ] All tests passing
- [ ] Performance metrics acceptable
- [ ] Documentation complete
- [ ] Integration stable

## Risk Mitigation

### Technical Risks
1. **AAR Compatibility:** Test on multiple Android versions
2. **Native Library Conflicts:** Use separate processes if needed
3. **Memory Management:** Implement proper cleanup

### Business Risks
1. **License Management:** Secure key storage required
2. **SDK Updates:** Version compatibility checks
3. **Support:** Establish Vivoka support channel

## Timeline

| Phase | Duration | Start | End | Status |
|-------|----------|-------|-----|--------|
| Phase 1: Build Config | 2 hours | 2025-01-28 | 2025-01-28 | Pending |
| Phase 2: Namespace Fix | 1 hour | 2025-01-28 | 2025-01-28 | Pending |
| Phase 3: SDK Init | 3 hours | 2025-01-28 | 2025-01-28 | Pending |
| Phase 4: Error Handling | 2 hours | 2025-01-28 | 2025-01-28 | Pending |
| Phase 5: Testing | 4 hours | 2025-01-29 | 2025-01-29 | Pending |

## Dependencies

### External
- Vivoka VSDK 6.0.0
- Android SDK 28+
- Kotlin 1.9.24+

### Internal
- VoiceDataManager module
- DeviceManager module
- VoiceRecognition app

## Notes

1. **License Key:** Must be obtained from Vivoka and stored securely
2. **Performance:** Monitor memory usage with large models
3. **Compatibility:** Test on ARM64 and ARMv7 architectures
4. **Documentation:** Update all engine documentation after implementation

---
**Last Updated:** 2025-01-28  
**Author:** VOS4 Development Team  
**Review Status:** Pending Implementation