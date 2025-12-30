# Speech Engine Port Analysis - LegacyAvenue to VOS4
**Created:** 2025-01-29
**Purpose:** Analyze missing functionality from LegacyAvenue engines

## 🔍 Engine Analysis

### 1. Vivoka Engine ✅ COMPLETE
- **LegacyAvenue:** VivokaSpeechRecognitionService.kt (842 lines)
- **VOS4:** VivokaEngine.kt (842 lines)
- **Status:** 100% ported with continuous recognition fix
- **Key Fix:** Model reset after each recognition

### 2. Google STT Engine (Android Native) 🔄 NEEDS UPDATE
- **LegacyAvenue:** GoogleSpeechRecognitionService.kt (715 lines) - ANDROID STT
- **VOS4:** GoogleSTTEngine.kt (800 lines) - ANDROID STT
- **Status:** Has most features but missing some critical ones
- **Note:** This is Android's native SpeechRecognizer, NOT Google Cloud

#### Missing Features in VOS4 GoogleSTT:
1. **Voice Sleep/Wake System** (Ava → Voice)
   - `isAvaVoiceEnabled` flag
   - `isAvaVoiceSleeping` flag
   - Sleep/wake command handling
   - Voice timeout system (5 minutes default)
   
2. **Special Commands**
   - Mute/unmute voice commands
   - Start/stop dictation commands
   - Command should come from config, not hardcoded
   
3. **Silence Detection for Dictation**
   - `silenceCheckHandler` and `silenceCheckRunnable`
   - Auto-stop dictation after silence (configurable 1-10 seconds)
   - Already partially implemented but needs completion
   
4. **Audio Level Monitoring**
   - Full `onRmsChanged` implementation
   - Track audio levels for silence detection
   
5. **Voice Timeout System**
   - `runTimeout()` method
   - Auto-sleep after inactivity (default 5 minutes)
   - `lastExecutedCommandTime` tracking

#### Features Already Present:
- ✅ Dynamic command registration
- ✅ Similarity matching (Levenshtein)
- ✅ Language support (50+ languages)
- ✅ Partial results
- ✅ Error recovery
- ✅ Dictation mode basics

### 3. Vosk Engine 🔄 NEEDS ANALYSIS
- **LegacyAvenue:** VoskSpeechRecognitionService.kt (needs check)
- **VOS4:** VoskEngine.kt (needs analysis)
- **Status:** To be analyzed

### 4. Google Cloud Engine ❌ NOT IN LEGACY
- **LegacyAvenue:** Does not exist (they only have Android STT)
- **VOS4:** GoogleCloudEngine.kt (VOS4 original implementation)
- **Status:** No porting needed - this is a VOS4 enhancement
- **Note:** Uses actual Google Cloud Speech-to-Text API with gRPC

## 📋 Implementation Plan

### Phase 1: Google STT Port (Priority: HIGH)
**Approach:** Similar to Vivoka - full replacement with LegacyAvenue code

1. **Backup current GoogleSTTEngine.kt**
2. **Copy GoogleSpeechRecognitionService.kt content**
3. **Adapt to VOS4 structure:**
   - Remove interfaces (SpeechRecognitionServiceInterface)
   - Replace Ava with Voice in all names
   - Use VOS4 shared components (ServiceState, ResultProcessor, etc.)
   - Convert to functional types for listeners
   - Use RecognitionResult instead of custom result types

4. **Key adaptations needed:**
   - Change package to `com.augmentalis.speechrecognition.speechengines`
   - Use `SpeechConfig` instead of `SpeechRecognitionConfig`
   - Use functional types: `typealias OnResultListener = (RecognitionResult) -> Unit`
   - Remove VoiceOsLogger, use Android Log
   - Adapt error handling to VOS4 pattern

### Phase 2: Vosk Engine Port (Priority: MEDIUM)
1. Analyze VoskSpeechRecognitionService.kt
2. Compare with current VoskEngine.kt
3. Port missing functionality

### Phase 3: Verification
1. Test each engine
2. Verify continuous recognition
3. Test special commands
4. Verify voice sleep/wake

## 🎯 Success Criteria
- [ ] Google STT has 100% LegacyAvenue functionality
- [ ] Vosk has 100% LegacyAvenue functionality  
- [ ] All engines support continuous recognition
- [ ] Voice sleep/wake system working
- [ ] Special commands configurable
- [ ] Silence detection for dictation

## 📝 Notes
- Google Cloud doesn't exist in LegacyAvenue (skip)
- Focus on functional equivalency, not line-by-line match
- Maintain VOS4 architecture patterns (no interfaces)
- Test thoroughly after each port