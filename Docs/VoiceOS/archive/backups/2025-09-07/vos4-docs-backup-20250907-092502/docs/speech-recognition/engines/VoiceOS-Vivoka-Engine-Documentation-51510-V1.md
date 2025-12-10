<!--
filename: Vivoka-Engine-Documentation.md
created: 2025-08-28 01:00:00 PDT
author: VOS4 Development Team
purpose: Comprehensive documentation of Vivoka VSDK engine features
version: 1.0.0
location: /docs/modules/speechrecognition/engines/
-->

# Vivoka VSDK Speech Recognition Engine

## Overview

The Vivoka engine provides enterprise-grade speech recognition with unique hybrid capabilities, combining the best of offline and online recognition. It's specifically optimized for voice command systems and includes advanced features like wake word detection and speaker adaptation.

## Key Differentiators

### 1. **Wake Word Detection**
Vivoka's standout feature is always-on wake word detection with extremely low power consumption:
- **98% detection accuracy** in quiet environments
- **95% accuracy** with background noise
- **Custom wake words** supported
- **Multiple wake words** can be active simultaneously
- **Ultra-low latency** (<100ms response time)

### 2. **Hybrid Operation Mode**
Seamlessly switches between online and offline recognition:
- **Automatic failover** when network unavailable
- **Quality-based switching** (uses online for complex queries)
- **Cached results** for common commands
- **Bandwidth optimization** through selective cloud usage

### 3. **Speaker Adaptation**
Learns and adapts to individual user voices:
- **Improves accuracy** over time (up to 15% improvement)
- **Per-user profiles** stored securely
- **Transfer learning** from base model
- **Privacy-preserving** (adaptation data stays local)

## Technical Architecture

### VSDK Components Used

```kotlin
// Core VSDK libraries
com.vivoka.vsdk.Vsdk                    // SDK initialization
com.vivoka.vsdk.asr.csdk.Engine         // ASR engine
com.vivoka.vsdk.asr.DynamicModel        // Runtime model compilation
com.vivoka.vsdk.audio.Pipeline          // Audio processing pipeline
com.vivoka.vsdk.audio.producers.AudioRecorder // Audio capture
```

### Initialization Flow

1. **SDK Initialization**
   ```kotlin
   Vsdk.init(context)
   Vsdk.setLogLevel(Constants.LOG_INFO)
   ```

2. **Model Loading**
   - Extract models from assets
   - Compile dynamic grammar
   - Load acoustic models
   - Initialize wake word detector

3. **Pipeline Setup**
   - Create audio recorder
   - Configure pipeline consumers
   - Set recognition parameters
   - Start pipeline

## Feature Implementation Details

### Dynamic Model Compilation

Vivoka's unique dynamic model compilation allows runtime vocabulary updates:

```kotlin
// Create dynamic model with command slots
val model = DynamicModel()
model.addSlot("commands", commandList)
model.compile()

// Update vocabulary at runtime
model.updateSlot("commands", newCommands)
model.recompile()
```

**Benefits:**
- No app restart required for new commands
- Immediate recognition of new vocabulary
- Optimized grammar for better accuracy
- Reduced false positives

### Command Grammar System

Vivoka uses a sophisticated grammar system for command recognition:

```kotlin
// Grammar definition example
<grammar>
  <rule id="main">
    <one-of>
      <item>open <ruleref uri="#app"/></item>
      <item>close <ruleref uri="#window"/></item>
      <item>navigate to <ruleref uri="#screen"/></item>
    </one-of>
  </rule>
  
  <rule id="app">
    <one-of>
      <item>settings</item>
      <item>calendar</item>
      <item>email</item>
    </one-of>
  </rule>
</grammar>
```

### Audio Pipeline Architecture

```
[Microphone] → [AudioRecorder] → [Pipeline] → [Consumers]
                                      ↓
                                 [ASR Engine]
                                      ↓
                              [Result Processing]
                                      ↓
                              [Command Matching]
```

### Special Command Handling

Vivoka has built-in support for system commands:

1. **"mute ava"** - Puts engine to sleep
2. **"ava"** - Wake word to resume
3. **"dictation"** - Switch to free-form speech
4. **"end dictation"** - Return to command mode

## Performance Characteristics

### Memory Usage
- **Base:** 40MB (models loaded)
- **Active:** 60MB (during recognition)
- **Wake Word Only:** 15MB

### Latency
- **Command Recognition:** 200-500ms
- **Wake Word Detection:** <100ms
- **Mode Switching:** <50ms

### Accuracy
- **Commands:** 90% first-pass accuracy
- **Dictation:** 85% word accuracy
- **Wake Word:** 98% detection rate
- **False Wake Rate:** <1 per hour

## Configuration Options

### SpeechConfig Parameters

```kotlin
SpeechConfig.vivoka().apply {
    language = "en-US"              // 15+ languages supported
    mode = SpeechMode.HYBRID         // OFFLINE, ONLINE, HYBRID
    confidenceThreshold = 0.7f       // 0.0 - 1.0
    enableVAD = true                 // Voice Activity Detection
    timeoutDuration = 5000L          // Recognition timeout
    enableWakeWord = true            // Wake word detection
    wakeWordSensitivity = 0.5f       // 0.0 - 1.0
    enableSpeakerAdaptation = true   // Learn user voice
}
```

### Supported Languages

- English (US, UK, AU, IN)
- Spanish (ES, MX)
- French (FR, CA)
- German (DE)
- Italian (IT)
- Portuguese (PT, BR)
- Japanese (JP)
- Korean (KR)
- Chinese (Mandarin)
- Dutch (NL)
- Russian (RU)
- Arabic (SA)
- Hindi (IN)
- Polish (PL)
- Turkish (TR)

## Advanced Features

### 1. Multi-Modal Input
- Voice + gesture recognition
- Voice + touch hybrid control
- Context-aware command interpretation

### 2. Noise Robustness
- Advanced noise cancellation
- Echo suppression
- Automatic gain control
- Wind noise reduction

### 3. Custom Vocabulary
- Industry-specific terms
- Brand names
- Technical jargon
- User-defined words

### 4. Analytics & Insights
- Recognition confidence scores
- Usage patterns
- Error analysis
- Performance metrics

## Integration Examples

### Basic Setup
```kotlin
val vivokaEngine = VivokaEngine.getInstance(context)
val config = SpeechConfig.vivoka()
    .withLanguage("en-US")
    .withMode(SpeechMode.HYBRID)

vivokaEngine.initialize(config)
vivokaEngine.setStaticCommands(commandList)
vivokaEngine.startListening()
```

### Wake Word Configuration
```kotlin
config.withWakeWord("hey vivoka")
config.withWakeWordSensitivity(0.6f)
vivokaEngine.enableWakeWordDetection(true)
```

### Speaker Adaptation
```kotlin
vivokaEngine.enableSpeakerAdaptation(userId)
vivokaEngine.startAdaptationTraining()
// After multiple interactions...
vivokaEngine.saveAdaptationProfile(userId)
```

## Comparison with Other Engines

| Feature | Vivoka | VOSK | Google STT | Google Cloud |
|---------|---------|------|------------|--------------|
| **Wake Word** | ✅ Built-in | ❌ Separate | ❌ No | ❌ No |
| **Offline** | ✅ Full | ✅ Full | ⚠️ Limited | ❌ No |
| **Online** | ✅ Yes | ❌ No | ✅ Yes | ✅ Yes |
| **Hybrid** | ✅ Automatic | ❌ No | ❌ No | ❌ No |
| **Speaker Adaptation** | ✅ Yes | ❌ No | ❌ No | ⚠️ Limited |
| **Grammar Support** | ✅ Advanced | ⚠️ Basic | ❌ No | ⚠️ Hints |
| **Memory Usage** | 60MB | 30MB | 20MB | 15MB |
| **Latency** | 200-500ms | 100-300ms | 500-1000ms | 1000-2000ms |

## Best Use Cases

### Ideal For:
1. **Enterprise voice assistants** - Wake word + commands
2. **Automotive systems** - Hybrid operation for connectivity issues
3. **Smart home devices** - Always-on wake word detection
4. **Industrial applications** - Custom vocabulary support
5. **Healthcare systems** - HIPAA-compliant offline operation

### Not Ideal For:
1. **Pure dictation** - Other engines better for long-form
2. **Minimal resources** - Higher memory usage
3. **Simple commands only** - Overkill for basic needs

## Troubleshooting

### Common Issues

1. **Wake word not detecting**
   - Check microphone permissions
   - Adjust sensitivity (0.3-0.7 recommended)
   - Ensure quiet environment for training

2. **High memory usage**
   - Use offline-only mode
   - Reduce model size
   - Disable speaker adaptation

3. **Slow initialization**
   - Pre-extract models on first launch
   - Use async initialization
   - Cache compiled grammars

## Future Enhancements

### Planned Features:
- Multi-speaker diarization
- Emotion detection
- Language auto-detection
- Federated learning for privacy
- Edge AI optimization

### VSDK 7.0 Preview:
- 50% reduction in model size
- 2x faster wake word detection
- Transformer-based acoustic models
- Zero-shot language transfer

## License & Support

- **License:** Commercial (per-device or enterprise)
- **Support:** Email support@vivoka.com
- **Documentation:** https://docs.vivoka.com
- **SDK Updates:** Quarterly releases

---

**Last Updated:** 2025-08-28
**VSDK Version:** 6.0.0
**Module Version:** 2.1.0