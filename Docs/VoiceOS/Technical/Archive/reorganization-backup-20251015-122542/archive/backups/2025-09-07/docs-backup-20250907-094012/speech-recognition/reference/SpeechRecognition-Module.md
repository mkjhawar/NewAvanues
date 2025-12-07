
<!--
filename: SpeechRecognition-Module.md
created: 2025-01-23 20:45:00 PST
author: VOS4 Development Team
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Technical documentation for SpeechRecognition module
last-modified: 2025-01-23 20:45:00 PST
version: 2.0.0
-->

# SpeechRecognition Module

## Overview
Multi-engine speech-to-text application supporting 6 different recognition engines with unified configuration and zero adapter layers.

## Status: ✅ Complete (100%)

## Architecture
- **Namespace**: `com.ai.speechrecognition`
- **Type**: Standalone Application
- **Database**: ObjectBox for configuration and history
- **Engines**: 6 supported engines with direct implementation

## Supported Engines

### Local Engines
1. **Vosk** - Offline speech recognition
   - **Models**: Multiple language models
   - **Performance**: <30MB memory usage
   - **Accuracy**: High for offline processing

2. **Vivoka** - Hybrid local/cloud processing
   - **SDK**: vsdk-6.0.0.aar integration
   - **Features**: Wake word detection, continuous recognition
   - **Performance**: <60MB memory usage

### Cloud Engines
3. **Android STT** - Built-in Android speech recognition
   - **Integration**: Native Android SpeechRecognizer API
   - **Connectivity**: Requires internet connection
   - **Accuracy**: High with Google's cloud processing

4. **Google Cloud Speech** - Advanced cloud recognition
   - **API**: REST API integration
   - **Features**: Advanced language models, punctuation
   - **Latency**: ~200-500ms depending on connection

5. **Azure Speech** - Microsoft cloud recognition
   - **SDK**: Azure Cognitive Services
   - **Features**: Custom models, speaker recognition
   - **Languages**: 85+ languages supported

6. **AWS Transcribe** - Amazon cloud recognition
   - **Integration**: AWS SDK for Android
   - **Features**: Real-time streaming, custom vocabularies
   - **Accuracy**: Industry-leading for specific domains

## Key Components

### Core Classes
- **SpeechModule** - Main module coordinator
- **EngineSelector** - Engine switching logic
- **ConfigurationManager** - Unified configuration
- **ResultProcessor** - Recognition result handling
- **WakeWordDetector** - Wake word processing

### Configuration
```kotlin
data class SpeechConfig(
    val primaryEngine: EngineType,
    val fallbackEngine: EngineType,
    val language: String = "en-US",
    val enableWakeWord: Boolean = true,
    val wakeWords: List<String> = listOf("hey ava", "ava"),
    val confidenceThreshold: Float = 0.7f,
    val maxRecordingTime: Long = 30000L,
    val enableProfanityFilter: Boolean = false
)
```

### Engine Integration Pattern
```kotlin
// Direct implementation - no interfaces
class VoskEngine(private val config: SpeechConfig) {
    fun initialize(): Boolean
    fun startListening(): Boolean
    fun stopListening(): Boolean
    fun processAudio(audioData: ByteArray): RecognitionResult
}
```

## Performance Metrics
- **Initialization**: <500ms
- **Engine switching**: <100ms
- **Recognition latency**: <200ms (local), <500ms (cloud)
- **Memory usage**: 30-60MB depending on engine
- **Battery impact**: <1% per hour continuous use

## API Reference

### Main APIs
```kotlin
class SpeechModule(context: Context) {
    // Core functionality
    fun initialize(): Boolean
    fun startListening(): Boolean
    fun stopListening(): Boolean
    fun switchEngine(engine: EngineType): Boolean
    
    // Configuration
    fun updateConfig(config: SpeechConfig)
    fun getCurrentEngine(): EngineType
    fun isListening(): Boolean
    
    // Results
    fun setResultCallback(callback: (RecognitionResult) -> Unit)
    fun getLastResult(): RecognitionResult?
}
```

### Recognition Result
```kotlin
data class RecognitionResult(
    val text: String,
    val confidence: Float,
    val engine: EngineType,
    val timestamp: Long,
    val isPartial: Boolean = false,
    val alternatives: List<String> = emptyList()
)
```

## Configuration Files
- **Location**: `/apps/SpeechRecognition/src/main/assets/`
- **Main Config**: `speech_config.json`
- **Engine Configs**: Individual engine configuration files
- **Models**: Vosk model files (downloaded dynamically)

## Integration Points
- **DeviceMGR**: Audio input/output management
- **CoreMGR**: Module lifecycle management
- **DataMGR**: Configuration and history storage
- **CommandsMGR**: Recognition result forwarding

## Wake Word Detection
```kotlin
class WakeWordDetector {
    // Vosk-based wake word detection
    fun initialize(wakeWords: List<String>): Boolean
    fun startDetection(): Boolean
    fun stopDetection(): Boolean
    fun updateWakeWords(wakeWords: List<String>)
}
```

## Testing
- **Unit Tests**: All engine implementations
- **Integration Tests**: Cross-engine switching
- **Performance Tests**: Memory and battery usage
- **Accuracy Tests**: Recognition quality validation

## Known Limitations
1. **Model Downloads**: Vosk models require initial download
2. **Network Dependency**: Cloud engines require internet
3. **Language Support**: Varies by engine
4. **Device Compatibility**: Some engines require specific Android versions

## Learning & Correction System

### Current Learning Capabilities

#### VoskEngine - Advanced Learning (✅ Implemented)
- **Learned Commands Cache**: Persistent JSON storage (`VoiceOsLearnedCommands.json`)
- **Vocabulary Cache**: Language-specific pre-testing (`static_commands_{language}.json`)
- **Four-Tier Processing**: Exact match → Learned commands → Grammar-constrained → Similarity matching
- **Auto-Learning**: Successful similarity matches automatically cached for future fast lookup
- **File Persistence**: Automatic save/load with performance metrics

#### GoogleCloudEngine - Enhanced Learning (✅ Implemented)
- **Learned Commands Cache**: Cloud-optimized storage (`GoogleCloudLearnedCommands.json`)
- **Enhanced Phrase Hints**: Dynamic context boosting with multi-tier prioritization
- **Vocabulary Testing**: Advanced heuristics for phrase optimization
- **Cross-Session Learning**: Persistent learned mappings across app restarts

#### AndroidSTTEngine - Basic Learning (⚠️ Limited)
- **Similarity Matching**: Levenshtein distance-based command matching
- **Session-Only Learning**: No persistence between app sessions
- **Hard-Coded Corrections**: Basic text corrections (e.g., "mike" → "mic")

### Learning System Architecture
```kotlin
// Unified learning interface across all engines
interface SpeechLearningSystem {
    // User correction capabilities
    suspend fun addUserCorrection(recognized: String, intended: String)
    suspend fun getUserCorrections(): List<CorrectionMapping>
    
    // Synonym management
    suspend fun addSynonym(command: String, synonym: String)
    suspend fun getSynonyms(command: String): List<String>
    
    // Learning analytics
    suspend fun getLearningStats(): LearningStatistics
    suspend fun getAccuracyTrend(): List<AccuracyDataPoint>
}
```

### Planned Learning Enhancements
- [ ] **User Correction Interface**: UI overlay for manual command corrections
- [ ] **Cross-Engine Learning**: Unified learning repository shared across engines  
- [ ] **Advanced Synonym Management**: Context-aware synonym resolution
- [ ] **Context-Aware Learning**: Usage pattern analysis and temporal context learning
- [ ] **Learning Analytics**: Recognition accuracy trending and learning effectiveness metrics
- [ ] **Adaptive Thresholds**: Dynamic confidence adjustment based on user feedback
- [ ] **A/B Testing Framework**: Learning algorithm optimization

### Learning Data Structures
```kotlin
@Entity
data class LearnedCommandEntity(
    val recognizedText: String,
    val matchedCommand: String,
    val confidence: Float,
    val usageCount: Int,
    val learningSource: LearningSource, // SIMILARITY_MATCHING, USER_CORRECTION, etc.
    val languageCode: String,
    val createdTimestamp: Long
)

data class LearningStatistics(
    val totalLearnedCommands: Int,
    val averageConfidence: Float,
    val successfulCorrections: Int,
    val learningAccuracy: Float
)
```

## Future Enhancements
- [ ] Custom model training support
- [ ] Multi-language detection
- [ ] Speaker identification
- [ ] Real-time translation integration
- [ ] Advanced noise cancellation
- [ ] Neural network-based command prediction
- [ ] Voice biometric authentication
- [ ] Contextual command suggestions

## Dependencies
```gradle
// Main dependencies
implementation 'io.objectbox:objectbox-android:3.4.1'
implementation files('libs/vsdk-6.0.0.aar')
implementation 'com.microsoft.cognitiveservices.speech:client-sdk:1.24.2'
implementation 'com.google.cloud:google-cloud-speech:4.0.0'
implementation 'com.amazonaws:aws-android-sdk-transcribe:2.73.0'
```

## Troubleshooting

### Common Issues
1. **Engine fails to initialize**
   - Check network connectivity for cloud engines
   - Verify model files for local engines
   - Validate API keys for cloud services

2. **Poor recognition accuracy**
   - Check microphone permissions
   - Adjust confidence threshold
   - Switch to different engine
   - Verify language configuration

3. **High battery usage**
   - Disable unnecessary engines
   - Reduce wake word sensitivity
   - Optimize recording parameters

### Debug Commands
```bash
# Check module status
adb logcat | grep "SpeechRecognition"

# Test engine switching
adb shell am broadcast -a com.ai.speechrecognition.SWITCH_ENGINE --es engine "VOSK"

# Verify configuration
adb shell cat /data/data/com.augmentalis.voiceos/files/speech_config.json
```

---

*Module Status: ✅ Complete*  
*Last Updated: 2025-08-22*  
*Compilation: ✅ Clean build*  
*Testing: ✅ All engines functional*
