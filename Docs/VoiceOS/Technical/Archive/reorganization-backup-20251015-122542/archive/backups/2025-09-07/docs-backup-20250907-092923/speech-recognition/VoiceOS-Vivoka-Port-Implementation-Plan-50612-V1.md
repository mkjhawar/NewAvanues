# Vivoka Engine Port Implementation Plan
**Created:** 2025-01-28
**Status:** IN PROGRESS
**Source:** LegacyAvenue 100% working implementation
**Target:** VOS4 VivokaEngine with shared components

## 🎯 Objective
Port 100% functional LegacyAvenue VivokaSpeechRecognitionService to VOS4 VivokaEngine structure, maintaining all functionality while integrating with VOS4 shared components.

## 📊 Component Mapping

### Naming Conversions
- `isAvaVoiceEnabled` → `isVoiceEnabled`
- `isAvaVoiceSleeping` → `isVoiceSleeping`
- AvaVoice references → Voice references

### Source to Target Mapping

| LegacyAvenue Component | VOS4 Target | Status |
|------------------------|-------------|---------|
| RecognizerMode enum | VivokaEngine inner enum | ⏳ Pending |
| State flags | Class properties | ⏳ Pending |
| compileModels() | Port as-is | ⏳ Pending |
| processRecognitionResult() | Enhance ResultProcessor | ⏳ Pending |
| onResult() with reset | Override with reset logic | ⏳ Pending |
| updateVoice() | Port adapted | ⏳ Pending |
| Timeout system | Integrate with TimeoutManager | ⏳ Pending |
| Silence detection | Port Handler-based | ⏳ Pending |
| Special commands | Port command checking | ⏳ Pending |
| LanguageUtils | Create helper methods | ⏳ Pending |

## 🔄 Implementation Steps

### Step 1: Core State Management
**Components:**
- RecognizerMode enum (COMMAND, FREE_SPEECH_START, FREE_SPEECH_RUNNING, STOP_FREE_SPEECH)
- State flags: isVoiceEnabled, isVoiceSleeping, isDictationActive, isListening
- recognizerMode property
- lastExecutedCommandTime tracking

**Verification:** 
- All states defined
- Proper initialization
- Thread-safe access

### Step 2: Model Management System
**Components:**
- compileModels(commands: List<String>)
- Dynamic model compilation
- Model path tracking (currentModelPath)
- Language-based model resolution

**Verification:**
- Models compile successfully
- Dynamic commands register
- Model switching works

### Step 3: Recognition Flow with Continuous Mode
**Components:**
- onResult() implementation
- processRecognitionResult() logic
- Model reset after results
- Mode transition handling

**Verification:**
- Continuous recognition works
- Results delivered correctly
- No single-recognition bug

### Step 4: Voice Timeout System
**Components:**
- runTimeout() method
- cancelTimeout() method
- Voice sleep mode
- Timeout configuration

**Verification:**
- Timeouts trigger correctly
- Sleep mode activates
- Wake commands work

### Step 5: Silence Detection System
**Components:**
- silenceCheckHandler
- silenceCheckRunnable
- Dictation timeout
- Auto-stop on silence

**Verification:**
- Silence detected correctly
- Dictation stops on timeout
- Handler lifecycle correct

### Step 6: Special Command Processing
**Components:**
- checkMuteCommand()
- checkUnmuteCommand()
- Start/stop dictation commands
- Command confidence checking

**Verification:**
- Commands recognized
- State changes trigger
- Confidence thresholds work

### Step 7: Integration & Testing
**Components:**
- Integration with TimeoutManager
- Integration with ResultProcessor
- Integration with CommandCache
- Full system testing

**Verification:**
- All features work
- No regressions
- Performance acceptable

## 🔧 Technical Details

### Dependencies to Handle
1. **LanguageUtils** - Create local helper methods
2. **FirebaseRemoteConfig** - Use VOS4 configuration
3. **PreferencesUtils** - Map to SpeechConfig
4. **VoiceOsLogger** - Use Android Log

### Files to Modify
- `/libraries/SpeechRecognition/src/main/java/com/augmentalis/speechrecognition/speechengines/VivokaEngine.kt`

### Files to Reference
- `/Volumes/M Drive/Coding/Warp/LegacyAvenue/voiceos/src/main/java/com/augmentalis/voiceos/speech/VivokaSpeechRecognitionService.kt`

## 📈 Progress Tracking
See: `/docs/modules/speechrecognition/Vivoka-Port-Checklist.md`