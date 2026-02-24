# SpeechRecognition — Wake Word & PhonemeASR TinyML Plan

**Module**: `Modules/SpeechRecognition/`, `Modules/VoiceOSCore/`, `Modules/Voice/WakeWord/`
**Branch**: `VoiceOS-1M-SpeechEngine`
**Created**: 2026-02-24
**Version**: V1

---

## 1. System 1: Vivoka Grammar-Based Wake Word (IMPLEMENTED)

### Architecture

Vivoka VSDK has no dedicated wake-word API. Instead, we use **grammar-based recognition with a restricted single-phrase grammar**:

```
enableWakeWord("hey ava", sensitivity=0.5)
  → compile grammar with ONLY "hey ava"
  → start continuous listening (low-power, tiny grammar)
  → on detection (confidence >= sensitivity):
      emit WakeWordEvent
      → recompile with FULL command grammar
      → listen for command (4s window, resets on each command)
      → timeout → recompile back to wake-word grammar
```

This reuses the proven mute/unmute pattern from `VivokaEngine.handleMuteCommand()`.

### Settings Persistence

| Field | DataStore Key | Type | Default |
|-------|--------------|------|---------|
| `wakeWordEnabled` | `wake_word_enabled` | Boolean | false |
| `wakeWordKeyword` | `wake_word_keyword` | String | "HEY_AVA" |
| `wakeWordSensitivity` | `wake_word_sensitivity` | Float | 0.5 |

Settings stored in `AvanuesSettings` (Foundation commonMain), persisted via `AvanuesSettingsRepository` (DataStore).

### UI (VoiceControlSettingsProvider)

- **Toggle**: Wake Word on/off
- **Dropdown**: Wake Phrase selector (Hey AVA / OK AVA / Computer)
- **Slider**: Sensitivity (10%-90%, 8 steps)

Phrase/sensitivity controls only visible when wake word is enabled.

### Engine Flow (VivokaAndroidEngine)

1. `enableWakeWord(phrase, sensitivity)` → compiles restricted grammar
2. `handleRecognitionResult()` intercepts results in wake-word mode
3. On match → `transitionToCommandMode()` → emits `WakeWordEvent`, recompiles full grammar
4. 4s timeout → `returnToWakeWordMode()` → recompiles restricted grammar
5. `updateCommands()` caches commands but skips grammar push while in wake-word mode
6. `disableWakeWord()` → restores full command grammar

### Lifecycle Wiring

Settings changes flow reactively from DataStore to the engine:

```
DataStore (AvanuesSettingsRepository)
  → Flow<AvanuesSettings>
    → VoiceAvanueAccessibilityService.collectLatest
      → VoiceOSCore.updateWakeWordSettings(enabled, wakePhrase, sensitivity)
        → (engine as? IWakeWordCapable)
          → enableWakeWord() / disableWakeWord()
```

`VoiceOSCore.updateWakeWordSettings()` uses `as? IWakeWordCapable` runtime type check — any engine implementing the interface (Vivoka now, PhonemeASR future) works without code changes.

Keyword mapping in accessibility service: `"HEY_AVA"` → `"hey ava"`, `"OK_AVA"` → `"ok ava"`, `"COMPUTER"` → `"computer"`.

`StubVivokaEngine` updated to match the new `enableWakeWord(wakeWord, sensitivity)` signature (returns `Result.failure(UnsupportedOperationException)`).

### Files Modified

| File | Changes |
|------|---------|
| `Modules/Foundation/.../AvanuesSettings.kt` | +3 wake word fields, +2 companion defaults |
| `Modules/Foundation/.../SettingsKeys.kt` | +3 wake word key constants |
| `apps/avanues/.../AvanuesSettingsRepository.kt` | +3 DataStore keys, read/write, 3 update methods |
| `apps/avanues/.../VoiceControlSettingsProvider.kt` | Full wake word settings group (toggle+dropdown+slider) |
| `Modules/VoiceOSCore/.../IVivokaEngine.kt` | `enableWakeWord()` + sensitivity param |
| `Modules/VoiceOSCore/.../VivokaAndroidEngine.kt` | Wake word state, grammar switching, mode transitions |
| `Modules/VoiceOSCore/.../VoiceOSCore.kt` | +`updateWakeWordSettings()` method with `IWakeWordCapable` runtime cast |
| `Modules/VoiceOSCore/.../StubVivokaEngine.kt` | Fixed `enableWakeWord()` signature (added sensitivity param) |
| `apps/avanues/.../VoiceAvanueAccessibilityService.kt` | Wired wake word settings observation in DataStore collection loop |

---

## 2. System 2: PhonemeASR TinyML Architecture (PLAN ONLY)

### ToT Analysis

| Branch | Approach | Size | Accuracy | Platform | Verdict |
|--------|----------|------|----------|----------|---------|
| **A** | TinyML Tiny Transducer | 6-20MB | Good (18% WER) | All (ONNX) | **SELECTED (immediate)** |
| B | Custom Kaldi/Vosk | 20-50MB | Good | Android+Desktop only | Rejected — no iOS/JS |
| C | Wav2Vec2-XLSR | 300MB-1.2GB | Best | All (ONNX) | Rejected — too large |
| D | Allosaurus | 100-150MB | High | Needs optimization | Rejected — not edge-optimized |
| **E** | Hybrid TinyML + Bigram LM | ~35MB | 4-6% WER | All (ONNX + Kotlin) | **Long-term target** |

### Unified Architecture

```
Audio (16kHz mono) → TinyML Phoneme Extractor (ONNX, ~20MB)
                            ↓
                    IPA Phoneme Sequence
                            ↓
         ┌──────────────────┼──────────────────┐
         ▼                  ▼                  ▼
   WAKE WORD           COMMAND             DICTATION
   Pattern match       Phoneme→Command     Lexicon+Bigram
   DTW/edit dist       dictionary lookup   beam search
   (<1MB)              (~0.5MB)            (~15MB dict+LM)
```

### Coexistence Architecture

```
IWakeWordDetector (commonMain interface)
    ├── VivokaWakeWordAdapter (Android only, PRODUCTION)
    │   └── Wraps VivokaAndroidEngine.enableWakeWord()
    │       Uses VSDK grammar recognition
    │
    └── PhonemeWakeWordDetector (All platforms, FUTURE)
        └── TinyML ONNX phoneme extractor + pattern matching
```

Runtime selection: Android with Vivoka → default Vivoka. Otherwise → PhonemeASR.
Developer override available in settings for testing.

### Phase Roadmap

| Phase | Scope | Effort | Dependencies |
|-------|-------|--------|-------------|
| A | Universal Phoneme Extractor (ONNX) | 12h | Source/train TinyML model |
| B | Wake Word Pattern Matching (DTW) | 8h | Phase A |
| C | Command Phoneme Dictionary | 12h | Phase B |
| D | Dictionary-Based Dictation | 16h | Phase C |
| E | Cross-Platform Audio (iOS/Desktop) | 8h | Phase B |

### Key PhonemeASR Advantages

| Feature | Porcupine (rejected) | PhonemeASR |
|---------|---------------------|------------|
| Size | 200MB+ | 20MB |
| Custom wake words | Requires paid training | Runtime text-to-phoneme |
| Licensing | API key + paid tiers | Open-source |
| Platform | Android only | All KMP targets via ONNX |
| Beyond wake-word | Wake word only | Commands + dictation |

---

## 3. Verification Checklist

### System 1 (Implemented)

- [ ] Foundation compiles: `./gradlew :Modules:Foundation:compileKotlinAndroid`
- [ ] SR Android compiles: `./gradlew :Modules:SpeechRecognition:compileDebugKotlinAndroid`
- [ ] App compiles: `./gradlew :apps:avanues:assembleDebug`
- [ ] Settings persist across app kill/restart
- [ ] Vivoka compiles 1-item grammar without crash
- [ ] Say "Hey AVA" → WakeWordEvent emitted
- [ ] After detection → full grammar available for 4s
- [ ] After timeout → returns to wake-word grammar

### System 2 (Future)

- [ ] ONNX model loads on all target platforms
- [ ] Phoneme accuracy: known audio → expected ARPAbet
- [ ] Detection rate >90% per keyword (quiet environment)
- [ ] False positive rate <5% over 1hr continuous listening
- [ ] Latency <300ms from utterance end to detection
- [ ] Custom wake word: text→phoneme→detection works
