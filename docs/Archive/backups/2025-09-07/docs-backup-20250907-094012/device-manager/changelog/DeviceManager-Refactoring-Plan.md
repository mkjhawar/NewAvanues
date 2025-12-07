# DeviceManager Refactoring & Enhancement Plan

## Executive Summary
This document outlines the refactoring plan for DeviceManager's AccessibilityManager (to be renamed) and Audio modules, based on analysis and user requirements.

## Phase 1: Immediate Refactoring (No Feature Changes)

### 1.1 Rename AccessibilityManager
**Current**: `AccessibilityManager` (too generic, conflicts with Android class name)
**New Name**: `AccessibilityOrchestrator`
**Rationale**: Clearly indicates it orchestrates multiple accessibility services rather than being an AccessibilityService

### 1.2 SOLID Compliance Refactoring
**Current Issue**: Single class handles TTS, Translation, Preferences, State Management (SRP violation)

**Proposed Structure**:
```
accessibility/
├── AccessibilityOrchestrator.kt  // Main coordinator
├── tts/
│   └── TTSManager.kt             // Text-to-speech only
├── translation/
│   └── TranslationManager.kt     // Translation services only
└── preferences/
    └── AccessibilityPreferencesManager.kt  // UI preferences only
```

**Implementation Priority**: LOW - Current monolithic design works, split only if maintenance becomes issue

### 1.3 Audio Focus Consolidation
**Action**: Route all audio focus requests through AudioService
**Files to Update**:
- `VolumeActions.kt` - Remove direct AudioManager calls
- `AudioService.kt` - Add centralized focus management

### 1.4 Bluetooth Audio Routing Consolidation
**Action**: Route all Bluetooth audio through AudioRouting
**Files to Update**:
- `BluetoothHandler.kt` - Use AudioRouting instead of direct calls
- Remove duplicate Bluetooth SCO management

## Phase 2: High Priority Enhancements

### 2.1 Voice Activity Detection (VAD) - Pre-Wake Word
**Note from User**: "isn't this already part of speechrecognition?"
**Clarification**: This is DIFFERENT - happens BEFORE wake word detection

**Implementation**:
```kotlin
// New file: audio/VoiceActivityDetector.kt
class VoiceActivityDetector {
    // Detect human voice vs background noise
    // Saves 30-40% battery by not processing non-voice audio
    // Reduces false wake word triggers
}
```

**Benefits**:
- Battery savings: Only process audio when voice detected
- Fewer false positives: Ignore TV/music
- Works before SpeechRecognition VAD

### 2.2 Enhanced Echo Cancellation
**User Question**: "How do you plan to do this?"
**Answer**: Enhance Android's basic AcousticEchoCanceler

**Implementation Approach**: Option C (Balanced)
```kotlin
// Enhance existing Android APIs
class EnhancedEchoCanceller {
    private val androidAEC = AcousticEchoCanceler.create(sessionId)
    private val doubleTalkDetector = DoubleTalkDetector()
    
    fun process(input: ShortArray): ShortArray {
        // 1. Use Android AEC as base
        // 2. Add double-talk detection
        // 3. Prevent filter corruption
        return processedAudio
    }
}
```

**Why Not WebRTC?**: 
- WebRTC adds 5MB to app size
- We only need echo cancellation, not full WebRTC stack
- Android APIs + enhancements sufficient for our needs

### 2.3 Audio Latency Optimization
**Current**: ~120-150ms
**Target**: <80ms (per user requirement)

**Step-by-Step Plan**:
1. **Measure Current Latency** (Week 1)
   - Add timestamp logging at each stage
   - Identify bottlenecks

2. **Optimize Buffers** (Week 2)
   ```kotlin
   // Reduce buffer sizes carefully
   const val VOICE_BUFFER_SIZE = 256  // From 1024
   const val VOICE_SAMPLE_RATE = 16000  // From 44100
   ```

3. **Implement Fast Audio Path** (Week 3)
   ```kotlin
   AudioTrack.Builder()
       .setPerformanceMode(PERFORMANCE_MODE_LOW_LATENCY)
   ```

4. **Test & Tune** (Week 4)
   - Monitor for audio glitches
   - Find optimal buffer/latency balance

## Phase 3: Medium Priority Enhancements

### 3.1 Smart Audio Device Switching
**User Confusion**: "why would we switch devices?"
**Clarification**: Switching AUDIO devices (headphones/speaker), not phones

**Implementation**:
```kotlin
class SmartAudioDeviceSwitcher {
    // Automatically switch when:
    // - Headphones plugged in → Switch from speaker
    // - Bluetooth connects → Switch from wired
    // - Hearing aid detected → Highest priority
}
```

**User Control**: Add setting to disable auto-switching if users prefer manual control

### 3.2 Audio Session Management
**User Comment**: "I agree but don't understand the scope"

**Simplified Scope - 3 Core Sessions**:
1. **Voice Session** - Optimized for commands (16kHz, mono, low latency)
2. **Media Session** - Optimized for music/video (48kHz, stereo, quality)
3. **Call Session** - Optimized for phone calls (8kHz, echo cancellation)

**API Design**:
```kotlin
// Simple for developers
audioService.useVoiceMode()  // Auto-configures everything
audioService.useMediaMode()  // Different settings
audioService.useCallMode()   // Phone optimized
```

## Phase 4: Deferred/Cancelled Items

### 4.1 SRP Refactoring of AccessibilityOrchestrator
**Status**: DEFERRED
**Reason**: Current monolithic design works well, no immediate benefit to splitting

### 4.2 Full WebRTC Integration
**Status**: CANCELLED
**Reason**: Too heavy (5MB), overkill for our needs

### 4.3 Custom Echo Cancellation from Scratch
**Status**: CANCELLED
**Reason**: Too complex, Android APIs + enhancements sufficient

## Implementation Timeline

### Sprint 1 (Immediate - 1-2 days)
#### AccessibilityManager & Audio
- [ ] Rename AccessibilityManager → AccessibilityOrchestrator
- [ ] Consolidate audio focus in AudioService
- [ ] Route Bluetooth audio through AudioRouting

#### Network Module Cleanup
- [ ] DELETE deprecated NetworkManager.kt (1000+ lines)
- [ ] Add DeviceCapabilities parameter to NfcManager
- [ ] Add DeviceCapabilities parameter to CellularManager
- [ ] Add DeviceCapabilities parameter to UsbNetworkManager
- [ ] Remove detectDeviceCapabilities() from BluetoothManager
- [ ] Remove hasSystemFeature() calls from WiFiManager

### Sprint 2 (2 weeks)
#### Audio Enhancements
- [ ] Implement VAD for pre-wake-word filtering
- [ ] Measure current audio latency
- [ ] Design enhanced echo cancellation

#### Network Consolidation
- [ ] Create NetworkStateMonitor for shared state tracking
- [ ] Create NetworkPermissionManager for centralized permissions
- [ ] Create NetworkBroadcastManager for shared broadcasts

### Sprint 3 (2 weeks)
#### Audio Optimization
- [ ] Implement enhanced echo cancellation
- [ ] Optimize audio buffers for latency
- [ ] Test latency improvements

#### Network Enhancements
- [ ] Implement UnifiedNetworkState
- [ ] Add SmartNetworkSelector
- [ ] Begin NetworkHandoffManager implementation

### Sprint 4 (2 weeks)
#### Audio Features
- [ ] Implement audio session management
- [ ] Add smart device switching
- [ ] Performance testing

#### Network Features
- [ ] Complete NetworkHandoffManager
- [ ] Consider renaming managers to avoid Android conflicts
- [ ] Integration testing across all network types

## Success Metrics

1. **Audio Latency**: <80ms from voice input to command execution
2. **Battery Usage**: 30% reduction with VAD
3. **Echo Cancellation**: No echo in speakerphone mode
4. **Code Quality**: No direct AudioManager calls outside AudioService

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Buffer underruns from small buffers | Start conservative, reduce gradually |
| Echo cancellation degrades audio | Keep original Android AEC as fallback |
| VAD misses quiet voices | Adjustable sensitivity threshold |
| Auto-switching annoys users | Setting to disable |

## Testing Strategy

### Unit Tests
```kotlin
@Test fun testVADDetectsVoice()
@Test fun testEchoCancellationDoubleTalk()
@Test fun testAudioLatencyUnder80ms()
@Test fun testSessionSwitching()
```

### Integration Tests
- Test with real Bluetooth devices
- Test with various Android versions
- Test with hearing aids (Android P+)

### Performance Tests
- Measure latency with systrace
- Monitor battery usage
- Check memory consumption

## Documentation Updates Required

1. Update `AccessibilityOrchestrator` class documentation
2. Create audio session usage guide
3. Document VAD configuration options
4. Update integration guide for other modules

## Dependencies

- No external libraries (using enhanced Android APIs)
- Minimum API 21 (Android 5.0) for basic features
- API 28+ (Android P) for hearing aid support
- API 31+ (Android S) for advanced audio routing

## Approval Required For

1. ✅ Renaming AccessibilityManager
2. ⏸️ Implementing VAD (battery impact)
3. ⏸️ Changing buffer sizes (stability risk)
4. ⏸️ Auto device switching (UX change)

---
**Document Version**: 1.0
**Date**: 2025-01-29
**Status**: Awaiting Approval
**Author**: Development Team