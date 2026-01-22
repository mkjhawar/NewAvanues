# Wake Word Detection Module

**Feature:** Wake Word Detection for AVA AI
**Version:** 1.0.0
**Status:** Phase 1.2 - Voice Integration
**Created:** 2025-11-22

---

## Overview

The Wake Word Detection module provides hands-free activation for AVA AI using the "Hey AVA" or "OK AVA" wake words. It uses the Porcupine wake word engine from Picovoice for accurate, on-device detection with minimal battery impact.

### Key Features

- **Porcupine Integration**: Industry-leading wake word engine
- **On-Device Processing**: Privacy-first, no cloud dependency
- **Low Power**: Optimized for battery efficiency
- **Background Service**: Continuous listening via foreground service
- **Battery Optimization**: Auto-pause when screen is off or battery is low
- **Configurable**: Multiple keywords, adjustable sensitivity
- **Material 3 UI**: Settings integrated with AVA design system

---

## Architecture

```
WakeWord Module
├── detector/
│   └── WakeWordDetector.kt       # Core Porcupine integration
├── service/
│   └── WakeWordService.kt        # Foreground service for background listening
├── settings/
│   ├── WakeWordSettingsRepository.kt  # DataStore persistence
│   └── WakeWordViewModel.kt      # State management
├── di/
│   └── WakeWordModule.kt         # Hilt DI configuration
└── WakeWordModels.kt             # Domain models
```

---

## Quick Start

### 1. Add Porcupine API Key

The module requires a Porcupine access key. You can get a free key at https://picovoice.ai/platform/porcupine/

**Option A: Environment Variable (Development)**

```bash
export AVA_PORCUPINE_API_KEY="your_access_key_here"
```

**Option B: Encrypted Storage (Production)**

Add to ApiKeyManager (future enhancement):
```kotlin
apiKeyManager.saveApiKey(ProviderType.PORCUPINE, "your_access_key_here")
```

### 2. Request Permissions

Add to your Activity/Fragment:

```kotlin
// Request RECORD_AUDIO permission
if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.RECORD_AUDIO),
        REQUEST_CODE_AUDIO
    )
}

// For Android 13+, also request POST_NOTIFICATIONS
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        REQUEST_CODE_NOTIFICATION
    )
}
```

### 3. Start Wake Word Service

```kotlin
import com.augmentalis.ava.features.wakeword.WakeWordSettings
import com.augmentalis.ava.features.wakeword.service.WakeWordService

// Configure settings
val settings = WakeWordSettings(
    enabled = true,
    keyword = WakeWordKeyword.HEY_AVA,
    sensitivity = 0.5f,
    backgroundListening = true,
    batteryOptimization = true
)

// Start service
WakeWordService.start(context, settings)
```

### 4. Listen for Wake Word Detections

Register a broadcast receiver in your Activity:

```kotlin
private val wakeWordReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val keyword = intent?.getStringExtra("keyword")
        Timber.i("Wake word detected: $keyword")

        // Start voice input
        startVoiceInput()
    }
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Register receiver
    val filter = IntentFilter("com.augmentalis.ava.WAKE_WORD_DETECTED")
    registerReceiver(wakeWordReceiver, filter)
}

override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(wakeWordReceiver)
}
```

---

## Usage with ViewModel

For UI-driven wake word management:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val wakeWordViewModel: WakeWordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize detector
        wakeWordViewModel.initialize { keyword ->
            Timber.i("Wake word detected: ${keyword.displayName}")
            startVoiceInput()
        }

        // Observe state
        lifecycleScope.launch {
            wakeWordViewModel.state.collect { state ->
                Timber.d("Wake word state: $state")
            }
        }

        // Observe events
        lifecycleScope.launch {
            wakeWordViewModel.events.collect { event ->
                when (event) {
                    is WakeWordEvent.Detected -> {
                        Timber.i("Detected: ${event.keyword}")
                    }
                    is WakeWordEvent.Error -> {
                        Timber.e("Error: ${event.message}")
                    }
                    else -> {}
                }
            }
        }

        // Start detection
        wakeWordViewModel.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeWordViewModel.stop()
    }
}
```

---

## Configuration

### Wake Word Keywords

| Keyword | Display Name | Type | Description |
|---------|--------------|------|-------------|
| `HEY_AVA` | "Hey AVA" | Custom | Preferred wake word (requires custom model) |
| `OK_AVA` | "OK AVA" | Custom | Alternative wake word (requires custom model) |
| `JARVIS` | "Jarvis" | Built-in | Fallback for testing |
| `ALEXA` | "Alexa" | Built-in | Fallback for testing |
| `COMPUTER` | "Computer" | Built-in | Fallback for testing |

**Note:** Custom "Hey AVA" and "OK AVA" models are not included. The module falls back to built-in Porcupine keywords (Jarvis, Alexa, Computer) for testing.

### Sensitivity

- **Range:** 0.0 - 1.0
- **Default:** 0.5 (balanced)
- **Low (0.2):** Fewer false positives, more false negatives (stricter)
- **High (0.8):** More false positives, fewer false negatives (more lenient)

### Battery Optimization

When enabled, the detector:
- Pauses when screen is off
- Pauses when battery is below 15%
- Resumes when screen is on and battery is sufficient

---

## Testing

### Unit Tests

Run tests:

```bash
./gradlew :Universal:AVA:Features:WakeWord:test
```

Test files:
- `WakeWordDetectorTest.kt` - Detector logic
- `WakeWordViewModelTest.kt` - ViewModel state management

### Manual Testing

1. **Verify API Key**: Ensure `AVA_PORCUPINE_API_KEY` is set
2. **Grant Permissions**: Allow microphone access
3. **Start Service**: Trigger wake word detection
4. **Speak Wake Word**: Say "Jarvis" (or configured keyword)
5. **Verify Detection**: Check logs for detection event

### Device Testing Checklist

- [ ] Wake word detection accuracy (>90% in quiet environment)
- [ ] False positive rate (<5% in noisy environment)
- [ ] Battery drain (<50 mAh/hour when listening)
- [ ] Service survives screen off/on
- [ ] Service survives low battery
- [ ] Notification appears when listening
- [ ] Detection triggers voice input

---

## Battery Impact Analysis

### Estimated Battery Consumption

| Scenario | Battery Drain | Notes |
|----------|--------------|-------|
| Active Listening (Screen On) | ~40 mAh/hour | Continuous microphone access |
| Active Listening (Screen Off) | N/A | Paused by battery optimization |
| Service Overhead | ~5 mAh/hour | Foreground service only |

**Total Daily Impact:** ~480 mAh (12 hours screen-on time)
**Percentage of 5000 mAh Battery:** ~9.6%

### Optimization Strategies

1. **Screen-Off Pause**: Saves ~70% battery by pausing when screen is off
2. **Low Battery Pause**: Prevents further drain below 15% battery
3. **Efficient Porcupine Engine**: Optimized C++ implementation
4. **On-Device Processing**: No network overhead

---

## Detection Accuracy Metrics

### Expected Performance

| Environment | Accuracy | False Positive Rate |
|-------------|----------|---------------------|
| Quiet (< 40 dB) | 95%+ | < 2% |
| Normal (40-60 dB) | 90%+ | < 5% |
| Noisy (60-80 dB) | 80%+ | < 10% |
| Very Noisy (> 80 dB) | 60%+ | < 20% |

### Factors Affecting Accuracy

- **Distance from Microphone**: Best within 1-3 meters
- **Accent/Pronunciation**: Porcupine supports multiple accents
- **Background Noise**: Music, TV, conversations reduce accuracy
- **Microphone Quality**: Device-dependent

---

## Troubleshooting

### Issue: "Porcupine access key not found"

**Solution:**
```bash
# Set environment variable
export AVA_PORCUPINE_API_KEY="your_key_here"

# Or add to ~/.zshrc or ~/.bashrc
echo 'export AVA_PORCUPINE_API_KEY="your_key_here"' >> ~/.zshrc
```

### Issue: Wake word not detected

**Checklist:**
1. Verify microphone permission is granted
2. Check Porcupine API key is valid
3. Ensure service is running (check notification)
4. Verify keyword pronunciation matches built-in model
5. Check logs for detection errors

### Issue: High battery drain

**Solutions:**
1. Enable battery optimization in settings
2. Lower sensitivity (reduces false positives)
3. Use headset/external microphone (better signal-to-noise)
4. Disable background listening when not needed

### Issue: Service stops after a while

**Possible Causes:**
- Android battery optimization killing service
- Out of memory
- Porcupine library error

**Solutions:**
1. Disable battery optimization for AVA in Android settings
2. Check logcat for errors
3. Restart service

---

## Future Enhancements

### Phase 1.3
- [ ] Custom "Hey AVA" wake word model training
- [ ] Voice activity detection (VAD) pre-filtering
- [ ] Multi-language support
- [ ] User-specific wake word personalization

### Phase 2.0
- [ ] Multiple wake words simultaneously
- [ ] Context-aware sensitivity adjustment
- [ ] Wake word analytics dashboard
- [ ] Cloud-based model updates

---

## API Reference

### WakeWordDetector

```kotlin
class WakeWordDetector(
    context: Context,
    apiKeyManager: ApiKeyManager
)

suspend fun initialize(
    settings: WakeWordSettings,
    onDetected: (WakeWordKeyword) -> Unit
): Result<Unit>

suspend fun start(): Result<Unit>
suspend fun stop(): Result<Unit>
suspend fun pause(reason: String)
suspend fun resume()
suspend fun cleanup()

fun isListening(): Boolean

val state: StateFlow<WakeWordState>
val detectionCount: StateFlow<Int>
```

### WakeWordViewModel

```kotlin
@HiltViewModel
class WakeWordViewModel(
    detector: WakeWordDetector,
    settingsRepository: WakeWordSettingsRepository
)

val settings: StateFlow<WakeWordSettings>
val state: StateFlow<WakeWordState>
val detectionCount: StateFlow<Int>
val events: SharedFlow<WakeWordEvent>
val stats: StateFlow<WakeWordStats>
val errorMessage: StateFlow<String?>

fun initialize(onDetected: (WakeWordKeyword) -> Unit)
fun start()
fun stop()
fun pause(reason: String)
fun resume()
fun updateSettings(settings: WakeWordSettings)
fun setEnabled(enabled: Boolean)
fun setKeyword(keyword: WakeWordKeyword)
fun setSensitivity(sensitivity: Float)
fun setBatteryOptimization(enabled: Boolean)
fun resetStats()
fun markFalsePositive()
fun isListening(): Boolean
fun clearError()
```

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Porcupine | 3.0.2 | Wake word detection engine |
| Hilt | 2.48 | Dependency injection |
| Coroutines | 1.7.3 | Async operations |
| DataStore | 1.0.0 | Settings persistence |

---

## License

© 2025 Augmentalis Inc, Intelligent Devices LLC
Proprietary - All Rights Reserved

---

## Author

**Manoj Jhawar**
Email: manoj@ideahq.net
AVA AI - Phase 1.2

---

**Last Updated:** 2025-11-22
