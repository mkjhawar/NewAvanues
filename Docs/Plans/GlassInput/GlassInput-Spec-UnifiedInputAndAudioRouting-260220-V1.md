# Glass Input Spec: Unified Input & Audio Source Routing

**Module**: GlassInput (cross-cutting: DeviceManager, VoiceOSCore, SpeechRecognition, RemoteCast)
**Type**: Spec
**Date**: 2026-02-20
**Version**: 1.0
**Author**: Manoj Jhawar
**Status**: Draft — awaiting review

---

## 1. Problem Statement

VoiceOS has strong smart glass **detection** (15+ device types) and HUD **rendering** (8 modes, spatial layout, gaze tracking), but lacks three critical input capabilities:

1. **Audio input routing** — Speech recognition is hardcoded to the device's built-in microphone. Cannot select Bluetooth mic, wired headset mic, glasses' built-in mic, or USB mic.
2. **Physical button/touchpad input** — `GlassesManager` detects `hasTouchpad`/`hasVoiceInput` capabilities but has zero input handling code. Forward, back, confirm, tap, double-tap, long-press are not mapped.
3. **Wired earphone button handling** — Standard earphone buttons (play/pause, volume) are not intercepted for voice command shortcuts.

These gaps affect the core promise: hands-free, voice-first interaction across all form factors.

---

## 2. Device Matrix

### 2.1 Smart Glasses

| Device | Tier | Mic | Touchpad | Buttons | Display | Connection | SDK |
|--------|------|-----|----------|---------|---------|------------|-----|
| RealWear Navigator 500/520 | 1 | YES (boom mic) | NO | 3 buttons | 720p monocular | WiFi/USB | RealWear SDK |
| Vuzix Blade 2 | 1 | YES | YES (side) | 2 buttons | AR binocular | WiFi/BT | Vuzix SDK |
| Rokid Max/Air | 1 | NO (glasses only) | YES (controller) | 2 buttons | AR binocular | USB-C | Rokid UXR SDK |
| Epson Moverio BT-45 | 1 | YES (headset) | YES (controller) | 4 buttons | AR binocular | WiFi/BT | Moverio SDK |
| Vuzix Z100 | 2 | NO | YES (temple) | 1 button | 640x480 mono green | BLE only | Ultralite SDK |
| XREAL Air 2 | 3 | NO | NO (phone proxy) | 0 | AR binocular | USB-C DP | Android XR |
| Mentra Live | 3 | YES | YES | 1 button | Micro OLED | BLE/WiFi | TypeScript MiniApp |

### 2.2 Phone/Tablet (as Glass Host)

| When glasses have... | Phone provides... |
|----------------------|-------------------|
| No microphone (Z100, Rokid, XREAL) | Built-in mic for voice recognition |
| No touchpad (XREAL) | Phone screen as virtual touchpad |
| No app runtime (Z100) | Companion app + BLE relay |
| Bluetooth headset connected | BT mic route option (SCO) |

### 2.3 Earphone/Headset Input

| Input Type | Events | Android API |
|------------|--------|-------------|
| Single-button wired | HEADSETHOOK: press, long-press | KeyEvent.KEYCODE_HEADSETHOOK |
| Multi-button wired | Play/Pause, Vol+/-, Next/Prev | KEYCODE_MEDIA_* |
| Bluetooth headset (HFP) | Same as multi-button + voice trigger | MediaSession + AudioManager |
| USB-C headset | Same as wired | Same as wired |
| AirPods/Galaxy Buds | Custom gestures (tap, double-tap) | Via Bluetooth HID profile |

---

## 3. Audio Input Source Architecture

### 3.1 Current State

```
AudioCapture.kt → MediaRecorder.AudioSource.MIC → built-in mic only
AudioRouting.kt → manages OUTPUT routing only (speakers, BT speakers)
SpeechRecognition → reads from AudioCapture → always device mic
```

### 3.2 Proposed Architecture

```
IAudioSourceProvider (commonMain interface)
├── getAvailableSources(): List<AudioSource>
├── getActiveSource(): AudioSource
├── setPreferredSource(source: AudioSource)
├── onSourceChanged: SharedFlow<AudioSource>
│
├── AndroidAudioSourceProvider (androidMain)
│   ├── queries AudioManager.getDevices(GET_DEVICES_INPUTS)
│   ├── monitors AudioDeviceCallback for hot-plug
│   ├── integrates with AudioRouting.kt for BT SCO state
│   └── selects: BUILTIN_MIC | BLUETOOTH_SCO | WIRED_HEADSET | USB_DEVICE
│
├── DesktopAudioSourceProvider (desktopMain)
│   ├── queries javax.sound.sampled.AudioSystem.getMixerInfo()
│   └── selects available TargetDataLine sources
│
└── IOSAudioSourceProvider (iosMain)
    ├── queries AVAudioSession.availableInputs
    └── selects: builtInMic | bluetoothHFP | headsetMic | usbAudio
```

### 3.3 AudioSource Model (commonMain)

```kotlin
data class AudioSource(
    val id: String,                    // unique device identifier
    val type: AudioSourceType,         // enum: BUILTIN_MIC, BLUETOOTH, WIRED_HEADSET, USB, GLASSES_MIC
    val name: String,                  // human-readable: "Galaxy Buds Pro (Left)"
    val sampleRates: List<Int>,        // supported: [16000, 44100, 48000]
    val isAvailable: Boolean,          // currently connected and ready
    val signalQuality: Float,          // 0.0-1.0 estimated quality (BT codec-based)
    val latencyMs: Int,                // estimated input latency
    val deviceAddress: String? = null  // BT MAC or USB device path
)

enum class AudioSourceType {
    BUILTIN_MIC,       // Device's built-in microphone
    BLUETOOTH_SCO,     // Bluetooth headset/earbuds (voice profile)
    BLUETOOTH_A2DP,    // Bluetooth with advanced codec (not all support mic)
    WIRED_HEADSET,     // 3.5mm or USB-C headset with mic
    USB_DEVICE,        // USB audio interface or USB microphone
    GLASSES_MIC,       // Smart glasses with built-in microphone
    REMOTE_MIC,        // Microphone on remotely connected device (RemoteCast)
    VIRTUAL            // Software audio source (for testing)
}
```

### 3.4 Integration with Speech Recognition

```kotlin
// In SpeechConfig (commonMain)
data class SpeechConfig(
    ...existing fields...,
    val preferredAudioSource: AudioSourceType? = null,  // null = auto-select
    val autoRouteToGlasses: Boolean = true               // prefer glasses mic when connected
)

// In AndroidSpeechRecognitionService
val audioSourceProvider: IAudioSourceProvider
// When initializing WhisperEngine/VoiceEngine:
// 1. Query audioSourceProvider.getAvailableSources()
// 2. If glasses with mic connected AND autoRouteToGlasses → use GLASSES_MIC
// 3. If BT headset with SCO active → use BLUETOOTH_SCO
// 4. Else → BUILTIN_MIC
// 5. Pass selected source to WhisperAudio.initialize(audioSource)
```

### 3.5 Auto-Selection Priority

When `preferredAudioSource` is null (auto mode):

| Priority | Source | Condition |
|----------|--------|-----------|
| 1 | GLASSES_MIC | Smart glasses connected with mic capability |
| 2 | BLUETOOTH_SCO | BT headset connected with active SCO profile |
| 3 | WIRED_HEADSET | Wired headset detected with mic |
| 4 | USB_DEVICE | USB audio device detected with input |
| 5 | BUILTIN_MIC | Always available fallback |

Special case: If glasses are connected but have NO mic (Z100, Rokid, XREAL):
- Fall back to next available source (BT → wired → built-in)
- Show HUD indicator: "Voice: Phone Mic" so user knows

---

## 4. Button/Touchpad Input Architecture

### 4.1 Proposed Architecture

```
IInputDevice (commonMain interface)
├── deviceId: String
├── deviceType: InputDeviceType
├── capabilities: Set<InputCapability>
├── onInputEvent: SharedFlow<InputEvent>
│
├── InputDeviceType enum
│   ├── TOUCHPAD        // capacitive surface (Vuzix side, Epson controller)
│   ├── BUTTON_ARRAY    // discrete buttons (RealWear, Moverio controller)
│   ├── D_PAD           // directional pad (some controllers)
│   ├── EARPHONE_BUTTON // inline buttons on headset
│   ├── ROTARY_ENCODER  // scroll wheel (future)
│   └── GESTURE_SENSOR  // optical gesture (future: Mentra, hand tracking)
│
├── InputCapability enum
│   ├── TAP, DOUBLE_TAP, LONG_PRESS
│   ├── SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT
│   ├── PRESS, RELEASE
│   ├── SCROLL_UP, SCROLL_DOWN
│   └── PINCH_ZOOM (future)
```

### 4.2 InputEvent Model (commonMain)

```kotlin
sealed class InputEvent {
    abstract val deviceId: String
    abstract val timestampMs: Long

    // Button events
    data class ButtonPress(
        override val deviceId: String,
        override val timestampMs: Long,
        val button: PhysicalButton,
        val pressType: PressType
    ) : InputEvent()

    // Touchpad gestures
    data class TouchpadGesture(
        override val deviceId: String,
        override val timestampMs: Long,
        val gesture: GestureType,
        val velocity: Float = 0f       // for swipes
    ) : InputEvent()

    // Earphone button
    data class EarphoneButton(
        override val deviceId: String,
        override val timestampMs: Long,
        val pressType: PressType
    ) : InputEvent()
}

enum class PhysicalButton {
    PRIMARY,          // Main/confirm button
    SECONDARY,        // Back/cancel button
    TERTIARY,         // Menu/options button
    ACTION_1,         // Device-specific button 1
    ACTION_2,         // Device-specific button 2
    VOICE_TRIGGER     // Dedicated voice activation button
}

enum class PressType {
    PRESS,            // Single press
    DOUBLE_PRESS,     // Double press (within 300ms)
    LONG_PRESS,       // Hold >500ms
    RELEASE           // Button released
}

enum class GestureType {
    TAP, DOUBLE_TAP, LONG_PRESS,
    SWIPE_FORWARD, SWIPE_BACK,    // along temple axis
    SWIPE_UP, SWIPE_DOWN,         // perpendicular to temple
    TWO_FINGER_TAP                // if touchpad supports multi-touch
}
```

### 4.3 Default Action Mapping

| Input | Default Action | Context: HUD | Context: List |
|-------|---------------|--------------|---------------|
| PRIMARY press | Confirm/Select | Activate focused HUD element | Select item |
| SECONDARY press | Back/Cancel | Dismiss HUD panel | Go back |
| TERTIARY press | Open command bar | Show voice command list | Show options menu |
| Touchpad TAP | Confirm | Same as PRIMARY | Same as PRIMARY |
| Touchpad DOUBLE_TAP | Voice trigger | Start listening | Start listening |
| Touchpad LONG_PRESS | Context menu | Show element options | Show item options |
| Touchpad SWIPE_FORWARD | Next item | Focus next HUD element | Scroll down |
| Touchpad SWIPE_BACK | Previous item | Focus prev HUD element | Scroll up |
| Touchpad SWIPE_UP | Scroll up | Expand panel | Scroll up |
| Touchpad SWIPE_DOWN | Scroll down | Collapse panel | Scroll down |
| Earphone PRESS | Toggle mute | Mute/unmute voice | Mute/unmute voice |
| Earphone DOUBLE_PRESS | Voice trigger | Start listening | Start listening |
| Earphone LONG_PRESS | Toggle dictation | Enter dictation mode | Enter dictation mode |

### 4.4 Action Mapping is User-Configurable

```kotlin
data class InputMapping(
    val input: InputEvent,          // what the user does
    val action: InputAction,        // what happens
    val context: InputContext = InputContext.GLOBAL  // when it applies
)

enum class InputAction {
    CONFIRM, BACK, MENU,
    SCROLL_UP, SCROLL_DOWN, SCROLL_LEFT, SCROLL_RIGHT,
    VOICE_TRIGGER, VOICE_MUTE, VOICE_DICTATION,
    NEXT_ITEM, PREV_ITEM,
    CUSTOM_COMMAND   // maps to a voice command string
}

enum class InputContext {
    GLOBAL,          // always active
    HUD,             // only when HUD is visible
    LIST,            // in list/grid views
    TEXT_INPUT,       // during text editing
    MEDIA_PLAYBACK,  // during audio/video playback
    NAVIGATION       // during map/navigation
}
```

---

## 5. Per-Device SDK Integration

### 5.1 Vuzix (Blade 2 + Z100)

**Blade 2 (Tier 1):**
- Touchpad: Right temple, capacitive
- Gestures: tap, double-tap, swipe forward/back, swipe up/down
- Buttons: Back button (physical), Power button
- SDK: `com.vuzix:hud-actionmenu`, input events via standard Android KeyEvent
- Mic: Built-in, stereo

**Z100 (Tier 2):**
- Touchpad: Right temple, capacitive (limited)
- Buttons: 1 action button
- SDK: Ultralite SDK (`com.vuzix.ultralite`) — BLE protocol
- Mic: NONE — phone provides voice
- Input relay: Touchpad events come via BLE notifications from Ultralite SDK
- Frame push: Canvas API (text/shapes) or Layout API (structured)

### 5.2 RealWear Navigator

- Touchpad: NONE
- Buttons: 3 buttons (Action, Home, Back) — BUT primary interaction is VOICE
- SDK: RealWear Developer SDK — no special input APIs needed, standard Android KeyEvent
- Mic: Boom microphone (optimized for noisy environments, 94dB noise rejection)
- Special: Voice-first by design. Buttons are secondary.

### 5.3 Epson Moverio BT-45

- Touchpad: YES (on controller)
- Buttons: 4 buttons (OK, Menu, Home, Back) on controller
- SDK: Moverio SDK — standard Android input
- Mic: Built-in headset microphone
- Controller: Wired to glasses, acts as Android input device

### 5.4 Rokid Max/Air

- Touchpad: YES (on separate controller/phone app)
- Buttons: 2 buttons on controller
- SDK: Rokid UXR SDK
- Mic: NONE on glasses — phone provides voice
- Connection: USB-C DisplayPort Alt Mode (video only)

### 5.5 Earphones / Headsets

- Standard Android: `MediaSession.Callback` + `KeyEvent` dispatch
- BT headsets: HFP profile for voice, button events via `KEYCODE_MEDIA_*`
- AirPods/Galaxy Buds: Custom touch gestures → mapped to standard media keys by OS

---

## 6. Voice Recognition on Phone for Mic-less Glasses

### 6.1 Scenario

User wears Vuzix Z100 (BLE, no mic, no app runtime). Phone is in pocket.

**Current flow (broken):**
1. Phone captures voice from built-in mic ✓
2. Speech recognition runs on phone ✓
3. Command matched and executed on phone ✓
4. BUT: No feedback to Z100 (no rendering adapter)
5. AND: If user speaks quietly, phone mic in pocket may not hear

**Desired flow:**
1. Phone captures voice from built-in mic (or BT earpiece mic)
2. Speech recognition runs on phone
3. Command matched → action taken on phone
4. Visual feedback pushed to Z100 via BLE Ultralite SDK
5. Z100 touchpad events relayed back to phone for navigation

### 6.2 Audio Source Selection for This Scenario

| Glass Type | Preferred Mic | Fallback |
|------------|--------------|----------|
| RealWear (has boom mic) | Glasses mic via USB/BT audio | Phone mic |
| Vuzix Blade 2 (has mic) | Glasses mic via BT SCO | Phone mic |
| Epson BT-45 (has mic) | Headset mic via audio jack | Phone mic |
| Z100 (no mic) | BT earpiece mic → phone mic | Phone mic only |
| Rokid (no mic) | BT earpiece mic → phone mic | Phone mic only |
| XREAL (no mic) | BT earpiece mic → phone mic | Phone mic only |

**Key insight**: For mic-less glasses, recommending a BT earpiece is the best UX. The phone mic in a pocket is a degraded experience. The app should suggest pairing a BT earpiece when mic-less glasses are detected.

---

## 7. RemoteCast Integration (Bidirectional)

### 7.1 Current: One-Way (Phone → Glasses)

```
Phone (CastManager) → MJPEG/WebSocket → Glasses (CastClient) → Display
```

### 7.2 Proposed: Bidirectional

```
Phone (CastManager) → Video frames → Glasses (CastClient) → Display
                   ← Input events ←
                   ← Audio stream ← (if glasses have mic)
                   → VOCAB sync →   (voice commands for current screen)
                   → TTS audio →    (text-to-speech playback on glasses speaker)
```

### 7.3 New Wire Protocol Messages

| Direction | Type | Payload | Purpose |
|-----------|------|---------|---------|
| Phone → Glass | CAST | JPEG frames | Screen mirror |
| Phone → Glass | VOC | Command list | Vocabulary sync |
| Phone → Glass | CMD | Action string | Execute command |
| Phone → Glass | TTS | Audio PCM | Text-to-speech playback |
| Glass → Phone | INP | InputEvent | Button/touchpad events |
| Glass → Phone | AUD | Audio PCM | Mic audio stream |
| Glass → Phone | IMU | Sensor data | Head tracking |

The INP and AUD messages are NEW. They enable glasses to send input back to the phone.

---

## 8. Module Placement

| Component | Module | Source Set | Rationale |
|-----------|--------|-----------|-----------|
| IAudioSourceProvider | DeviceManager | commonMain | Abstract interface, all platforms |
| AudioSource model | DeviceManager | commonMain | Data model |
| AndroidAudioSourceProvider | DeviceManager | androidMain | Android AudioManager APIs |
| DesktopAudioSourceProvider | DeviceManager | desktopMain | javax.sound API |
| IOSAudioSourceProvider | DeviceManager | iosMain | AVAudioSession API |
| IInputDevice / InputEvent | DeviceManager | commonMain | Abstract interface + models |
| AndroidInputDispatcher | VoiceOSCore | androidMain | AccessibilityService + KeyEvent |
| VuzixInputAdapter | VoiceOSCore | androidMain | Vuzix SDK touchpad/button |
| RealWearInputAdapter | VoiceOSCore | androidMain | Standard KeyEvent (voice-first) |
| EpsonInputAdapter | VoiceOSCore | androidMain | Moverio SDK controller |
| EarphoneInputAdapter | VoiceOSCore | androidMain | MediaSession + HEADSETHOOK |
| InputMappingStore | VoiceOSCore | commonMain | User-configurable mappings |
| ButtonCommandHandler | VoiceOSCore | androidMain | Routes input→commands |
| HUDInputFeedback | VoiceOSCore | androidMain | Visual feedback on HUD |
| VuzixUltraliteAdapter | RemoteCast | androidMain | Z100 BLE rendering + input relay |

---

## 9. Dependency Graph

```
SpeechRecognition ──depends on──→ DeviceManager (IAudioSourceProvider)
VoiceOSCore ──depends on──→ DeviceManager (IInputDevice, GlassesManager)
RemoteCast ──depends on──→ DeviceManager (for glass detection)
HUDManager ──depends on──→ VoiceOSCore (InputEvent for HUD navigation)
```

No circular dependencies. DeviceManager is the foundation layer.

---

## 10. Settings UI Integration

Add to UnifiedSettingsScreen (SettingsProvider pattern):

**Audio Input Settings (priority 250, between VoiceCursor and VoiceControl):**
- Preferred audio source selector (Auto / Built-in / Bluetooth / Wired)
- Audio source indicator (current active source)
- "Auto-route to glasses mic" toggle
- "Suggest BT earpiece for mic-less glasses" toggle

**Glass Input Settings (priority 350, between VoiceControl and WebAvanue):**
- Button mapping editor (per device type)
- Touchpad sensitivity slider
- Earphone button action selector
- "Show button hints on HUD" toggle
- Device-specific settings (if Vuzix/RealWear detected)

---

## 11. Testing Strategy

| Test | Type | Setup |
|------|------|-------|
| Audio source switching | Integration | BT headset + phone |
| BT SCO mic routing | Device | BT earpiece paired |
| Wired headset detection | Device | 3.5mm headset |
| Earphone button capture | Device | Wired earphone with inline button |
| Button mapping persistence | Unit | SQLDelight / DataStore |
| Touchpad gesture detection | Device | Vuzix Blade 2 |
| Z100 BLE relay | Device | Vuzix Z100 + phone |
| HUD input feedback | Visual | Any glass with HUD active |
| Voice from pocket (degraded) | User test | Phone in pocket, no external mic |
| Auto-source priority | Unit | Mock IAudioSourceProvider |

---

## 12. Implementation Phases (Proposed)

These are PROPOSED phases. No implementation starts until this spec is reviewed and approved.

| Phase | Description | Est. Lines | Priority |
|-------|-------------|-----------|----------|
| G.1 | IAudioSourceProvider + Android impl + speech integration | ~400 | P1 |
| G.2 | IInputDevice + InputEvent models (commonMain) | ~200 | P1 |
| G.3 | Earphone button handler (most universal input) | ~150 | P1 |
| G.4 | Android button/touchpad dispatch (KeyEvent routing) | ~250 | P2 |
| G.5 | Vuzix SDK touchpad adapter | ~200 | P2 |
| G.6 | HUD input feedback (visual response) | ~150 | P2 |
| G.7 | Input mapping persistence + settings UI | ~300 | P3 |
| G.8 | Vuzix Z100 Ultralite BLE adapter | ~400 | P3 |
| G.9 | RemoteCast bidirectional (INP/AUD messages) | ~300 | P4 |
| G.10 | RealWear/Epson/Rokid specific adapters | ~200/ea | P4 |
| **Total** | | ~2,650+ | |

---

## 13. Open Questions

1. **BT mic quality**: Bluetooth SCO uses narrow-band audio (8kHz). Whisper expects 16kHz. Do we upsample, or does the audio quality degrade recognition accuracy too much? Need testing.
2. **Simultaneous mics**: Can we capture from BT mic AND device mic simultaneously for noise-cancellation fusion? Android API allows only one AudioSource per AudioRecord.
3. **Z100 frame rate**: At BLE 1Mbps, we get ~2-5 FPS JPEG. Is HUD text-only mode acceptable, or do users expect screen mirroring?
4. **Earphone button conflicts**: Android MediaSession may consume button events before our app. How do we ensure priority? AccessibilityService has `onKeyEvent()` but only for hardware keyboards.
5. **User expectations**: Should mic-less glasses auto-prompt "Pair a Bluetooth earpiece for better voice recognition"? Or is that too presumptuous?
6. **Device-specific SDKs**: Vuzix SDK (free), Moverio SDK (free), RealWear SDK (free), Rokid UXR (license?). Need to verify licensing terms for all SDKs.

---

## 14. References

- GlassAvanue spec: `docs/plans/RemoteCast/RemoteCast-Spec-GlassClientArchitecture-260219-V1.md`
- SDK research: `docs/analysis/RemoteCast/RemoteCast-Analysis-SmartGlassesSDKResearch-260219-V1.md`
- AudioRouting.kt: `Modules/DeviceManager/src/androidMain/.../audio/AudioRouting.kt`
- HUDManager.kt: `Modules/VoiceOSCore/src/androidMain/.../hudmanager/HUDManager.kt`
- GlassesManager.kt: `Modules/DeviceManager/src/androidMain/.../smartglasses/GlassesManager.kt`
- SmartGlassDetection.kt: `Modules/DeviceManager/src/androidMain/.../detection/SmartGlassDetection.kt`
