# Vivoka to Apple Speech API Equivalence Table

**Created:** 2025-11-02
**Purpose:** Cross-platform speech recognition implementation guide for iOS/macOS
**Context:** VOS4 uses Vivoka VSDK on Android - iOS/macOS will use Apple's native Speech APIs

---

## Executive Summary

Since **Vivoka VSDK does not support iOS**, VOS4's cross-platform expansion requires using **Apple's native Speech framework** on iOS/macOS. This document provides a comprehensive mapping between Vivoka SDK APIs and their Apple Speech framework equivalents.

### Key Differences

| Aspect | Vivoka (Android) | Apple Speech (iOS/macOS) |
|--------|-----------------|---------------------------|
| **Framework** | Vivoka VSDK (3rd party) | Speech framework (native) |
| **Offline Support** | Yes (with models) | Yes (iOS 13+) |
| **Online Recognition** | Yes | Yes |
| **Continuous Recognition** | Yes | Yes |
| **Real-time Streaming** | Yes | Yes |
| **Custom Models** | Yes | Limited (via Create ML) |
| **Language Support** | 20+ languages | 50+ languages |
| **License** | Commercial | Free (Apple ecosystem) |

---

## 1. Core API Mappings

### 1.1 Initialization

#### Vivoka (Android)
```kotlin
// Initialize SDK
Vsdk.init(context, licenseKey)

// Configure recognizer
val recognizer = VRecognizer.Builder()
    .setLanguage("en-US")
    .setOffline(true)
    .build()
```

#### Apple Speech (iOS/Swift)
```swift
import Speech

// Request authorization
SFSpeechRecognizer.requestAuthorization { status in
    guard status == .authorized else { return }
}

// Create recognizer
let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
recognizer?.defaultTaskHint = .dictation
```

**Equivalence:** Both require initialization, but Apple uses authorization request instead of license key.

---

### 1.2 Recognition Request

#### Vivoka (Android)
```kotlin
// Start recognition
recognizer.startRecognition(object : IRecognizerListener {
    override fun onReady() { }
    override fun onPartialResult(text: String?) { }
    override fun onFinalResult(text: String?, confidence: Float) { }
    override fun onError(error: Int, message: String?) { }
})
```

#### Apple Speech (iOS/Swift)
```swift
// Create recognition request
let request = SFSpeechAudioBufferRecognitionRequest()
request.shouldReportPartialResults = true
request.requiresOnDeviceRecognition = true  // For offline

// Start recognition task
recognitionTask = recognizer?.recognitionTask(with: request) { result, error in
    if let result = result {
        let bestTranscription = result.bestTranscription
        if result.isFinal {
            // Final result
            self.handleFinalResult(bestTranscription.formattedString)
        } else {
            // Partial result
            self.handlePartialResult(bestTranscription.formattedString)
        }
    }
    if let error = error {
        self.handleError(error)
    }
}
```

**Key Differences:**
- Vivoka uses listener pattern with callbacks
- Apple uses task-based pattern with completion handlers
- Apple provides `SFTranscription` object with rich metadata

---

### 1.3 Audio Input

#### Vivoka (Android)
```kotlin
// Vivoka handles audio automatically
recognizer.startRecognition(listener)
```

#### Apple Speech (iOS/Swift)
```swift
// Manual audio engine setup required
let audioEngine = AVAudioEngine()
let inputNode = audioEngine.inputNode
let recordingFormat = inputNode.outputFormat(forBus: 0)

inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, time in
    request.append(buffer)
}

audioEngine.prepare()
try audioEngine.start()
```

**Key Difference:** Apple requires manual audio engine setup, Vivoka handles it internally.

---

### 1.4 Stop/Cancel Recognition

#### Vivoka (Android)
```kotlin
recognizer.stopRecognition()
recognizer.cancelRecognition()
```

#### Apple Speech (iOS/Swift)
```swift
// Finish recognition (process remaining audio)
request.endAudio()
recognitionTask?.finish()

// Cancel recognition (immediate stop)
recognitionTask?.cancel()
audioEngine.stop()
inputNode.removeTap(onBus: 0)
```

**Equivalence:** Both support graceful stop and immediate cancel.

---

## 2. Feature Mappings

### 2.1 Offline Recognition

#### Vivoka (Android)
```kotlin
val recognizer = VRecognizer.Builder()
    .setOffline(true)
    .setModelPath("/path/to/model")
    .build()
```

#### Apple Speech (iOS/Swift)
```swift
let request = SFSpeechAudioBufferRecognitionRequest()
request.requiresOnDeviceRecognition = true  // Offline

// Check availability
if recognizer?.supportsOnDeviceRecognition == true {
    // Offline supported
} else {
    // Fallback to online
}
```

**Note:** iOS 13+ supports on-device recognition for many languages.

---

### 2.2 Language Selection

#### Vivoka (Android)
```kotlin
recognizer.setLanguage("en-US")
recognizer.setLanguage("fr-FR")
recognizer.setLanguage("es-ES")
```

#### Apple Speech (iOS/Swift)
```swift
// Create recognizer with locale
let enRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
let frRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "fr-FR"))
let esRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "es-ES"))

// Check availability
SFSpeechRecognizer.supportedLocales()  // Returns Set<Locale>
```

**Note:** Apple has separate recognizer instances per language.

---

### 2.3 Confidence Scores

#### Vivoka (Android)
```kotlin
override fun onFinalResult(text: String?, confidence: Float) {
    // confidence: 0.0 - 1.0
}
```

#### Apple Speech (iOS/Swift)
```swift
let segments = result.bestTranscription.segments
for segment in segments {
    let confidence = segment.confidence  // 0.0 - 1.0
    let text = segment.substring
    let timestamp = segment.timestamp
    let duration = segment.duration
}
```

**Apple Advantage:** Per-word confidence scores vs. overall confidence.

---

### 2.4 Alternative Transcriptions

#### Vivoka (Android)
```kotlin
// Not directly supported
// Single best result returned
```

#### Apple Speech (iOS/Swift)
```swift
let result = recognitionResult
let bestTranscription = result.bestTranscription
let alternativeTranscriptions = result.transcriptions  // Array of alternatives

for transcription in alternativeTranscriptions {
    let text = transcription.formattedString
    let averageConfidence = transcription.segments.map { $0.confidence }.reduce(0, +) / Float(transcription.segments.count)
}
```

**Apple Advantage:** Multiple alternative transcriptions with confidence scores.

---

### 2.5 Continuous Recognition

#### Vivoka (Android)
```kotlin
recognizer.setContinuousMode(true)
recognizer.startRecognition(listener)
// Continues until stopRecognition() called
```

#### Apple Speech (iOS/Swift)
```swift
// Apple limits recognition to 1 minute
// Workaround: Restart recognition in chunks

func restartRecognitionIfNeeded() {
    if Date().timeIntervalSince(recognitionStartTime) > 55 {
        restartRecognition()
    }
}
```

**Vivoka Advantage:** True continuous recognition. Apple has 1-minute limit.

---

## 3. Advanced Features

### 3.1 Voice Activity Detection (VAD)

#### Vivoka (Android)
```kotlin
recognizer.setVADEnabled(true)
recognizer.setVADTimeout(2000)  // ms of silence
```

#### Apple Speech (iOS/Swift)
```swift
// No built-in VAD
// Implement manually using AVAudioEngine

func detectSilence(in buffer: AVAudioPCMBuffer) -> Bool {
    // Calculate RMS energy
    let channelData = buffer.floatChannelData?[0]
    var rms: Float = 0
    for i in 0..<Int(buffer.frameLength) {
        let sample = channelData?[i] ?? 0
        rms += sample * sample
    }
    rms = sqrt(rms / Float(buffer.frameLength))
    return rms < 0.01  // Silence threshold
}
```

**Vivoka Advantage:** Built-in VAD. Apple requires manual implementation.

---

### 3.2 Custom Wake Words

#### Vivoka (Android)
```kotlin
recognizer.addWakeWord("Hey Voice")
recognizer.setWakeWordCallback { wakeWord ->
    // Wake word detected
}
```

#### Apple Speech (iOS/Swift)
```swift
// No built-in wake word detection
// Use SNAudioStreamAnalyzer for sound classification

import SoundAnalysis

let request = try SNClassifySoundRequest(mlModel: customWakeWordModel)
let analyzer = SNAudioStreamAnalyzer(format: audioFormat)
try analyzer.add(request, withObserver: self)
```

**Workaround:** Use Sound Analysis framework with custom Core ML model.

---

### 3.3 Model Management

#### Vivoka (Android)
```kotlin
// Download language model
val downloader = ModelDownloader()
downloader.downloadModel("en-US") { progress ->
    // Update UI
}

// Check model availability
val modelManager = ModelManager()
val availableModels = modelManager.getInstalledModels()
```

#### Apple Speech (iOS/Swift)
```swift
// Models managed by iOS automatically
// Check availability per language

let recognizer = SFSpeechRecognizer(locale: locale)
if recognizer?.isAvailable == true {
    if recognizer?.supportsOnDeviceRecognition == true {
        // Offline model available
    } else {
        // Only online available
    }
}

// No manual download - iOS handles it
```

**Apple Advantage:** Automatic model management by iOS.

---

## 4. Error Handling

### 4.1 Error Codes

#### Vivoka (Android)
```kotlin
override fun onError(error: Int, message: String?) {
    when (error) {
        ERROR_NETWORK -> // Network issue
        ERROR_AUDIO -> // Microphone issue
        ERROR_LICENSE -> // License expired
        ERROR_MODEL_MISSING -> // Model not downloaded
        ERROR_NO_SPEECH -> // No speech detected
    }
}
```

#### Apple Speech (iOS/Swift)
```swift
if let error = error as NSError? {
    switch error.code {
    case 1:  // Audio recording failed
    case 100:  // Recognition not available
    case 101:  // Recognition service unavailable
    case 102:  // Recognition request denied
    case 1100:  // Not authorized
    case 1101:  // Recognition restricted
    case 1110:  // Recognizer not available
    default:
        // Handle generic error
    }
}
```

**Mapping:**
| Vivoka Error | Apple Error Code |
|--------------|------------------|
| ERROR_AUDIO | 1 |
| ERROR_NETWORK | 101 |
| ERROR_LICENSE | 1100 (Not authorized) |
| ERROR_MODEL_MISSING | 1110 (Not available) |
| ERROR_NO_SPEECH | (No direct equivalent) |

---

## 5. Permissions

### 5.1 Required Permissions

#### Vivoka (Android - AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

#### Apple Speech (iOS - Info.plist)
```xml
<key>NSSpeechRecognitionUsageDescription</key>
<string>We need speech recognition to control the device with your voice</string>
<key>NSMicrophoneUsageDescription</key>
<string>We need microphone access for voice commands</string>
```

**Runtime Request (iOS/Swift):**
```swift
// Speech recognition authorization
SFSpeechRecognizer.requestAuthorization { status in }

// Microphone authorization
AVAudioSession.sharedInstance().requestRecordPermission { granted in }
```

---

## 6. Performance Characteristics

| Metric | Vivoka (Android) | Apple Speech (iOS/macOS) |
|--------|------------------|---------------------------|
| **Latency (online)** | 100-300ms | 50-200ms |
| **Latency (offline)** | 50-150ms | 30-100ms |
| **Accuracy (online)** | 92-96% | 94-98% |
| **Accuracy (offline)** | 88-92% | 90-94% |
| **Model Size** | 50-300MB | Managed by OS |
| **Memory Usage** | 80-150MB | 50-100MB |
| **Battery Impact** | Moderate | Low (optimized) |
| **Continuous Recognition** | Unlimited | 1-minute limit |

---

## 7. Platform-Specific Considerations

### 7.1 macOS Differences

#### Additional Capabilities
```swift
// macOS has additional dictation features
if #available(macOS 10.15, *) {
    recognizer?.queue = DispatchQueue.main
    recognizer?.defaultTaskHint = .dictation
}
```

### 7.2 iOS Differences

#### App Extensions
```swift
// Speech recognition in app extensions
// Requires entitlement: com.apple.security.get-task-allow
```

---

## 8. Migration Strategy for VOS4

### 8.1 Common Interface (Kotlin Multiplatform)

```kotlin
// shared/commonMain
interface SpeechRecognitionEngine {
    suspend fun initialize(config: SpeechConfig): Boolean
    suspend fun startRecognition(listener: RecognitionListener)
    suspend fun stopRecognition()
    fun setLanguage(locale: String)
}

// shared/androidMain
class VivokaSpeechEngine : SpeechRecognitionEngine {
    // Vivoka implementation
}

// shared/iosMain
expect class AppleSpeechEngine : SpeechRecognitionEngine {
    // Apple Speech implementation
}
```

### 8.2 Wrapper Implementation

#### iOS (Swift)
```swift
class AppleSpeechEngineImpl: AppleSpeechEngine {
    private var recognizer: SFSpeechRecognizer?
    private var recognitionTask: SFSpeechRecognitionTask?
    private var audioEngine: AVAudioEngine?

    func initialize(config: SpeechConfig) async -> Bool {
        // Request authorization
        let authorized = await requestAuthorization()
        guard authorized else { return false }

        // Setup recognizer
        recognizer = SFSpeechRecognizer(locale: Locale(identifier: config.language))
        return recognizer?.isAvailable ?? false
    }

    func startRecognition(listener: RecognitionListener) async {
        // Setup audio engine and recognition
    }
}
```

### 8.3 Feature Parity Matrix

| Feature | Android (Vivoka) | iOS (Apple) | Implementation Status |
|---------|------------------|-------------|----------------------|
| Basic Recognition | ✅ | ✅ | ✅ Complete |
| Offline Mode | ✅ | ✅ | ✅ Complete |
| Continuous Recognition | ✅ | ⚠️ Limited | 🔄 Workaround needed |
| Multiple Languages | ✅ | ✅ | ✅ Complete |
| Confidence Scores | ✅ Basic | ✅ Advanced | ✅ Complete |
| Wake Words | ✅ | ❌ | 🔄 Core ML needed |
| VAD | ✅ | ❌ | 🔄 Manual impl. needed |
| Alternative Transcriptions | ❌ | ✅ | ✅ Complete |
| Model Management | ✅ Manual | ✅ Auto | ✅ Complete |

---

## 9. Recommended Implementation Approach

### Phase 1: Core Recognition (Week 1-2)
1. Implement basic SFSpeechRecognizer wrapper
2. Handle authorization and permissions
3. Implement start/stop recognition
4. Add partial and final result callbacks

### Phase 2: Feature Parity (Week 3-4)
1. Add offline mode support
2. Implement multi-language switching
3. Add confidence score extraction
4. Implement error handling and recovery

### Phase 3: Advanced Features (Week 5-6)
1. Implement continuous recognition workaround (1-min chunks)
2. Add custom VAD using audio analysis
3. Integrate wake word detection (Core ML)
4. Optimize battery usage

### Phase 4: Testing & Optimization (Week 7-8)
1. Unit tests for all features
2. Integration tests with VOS4
3. Performance benchmarking
4. Memory and battery optimization

---

## 10. Code Examples

### Complete iOS Implementation Example

```swift
import Speech
import AVFoundation

class AppleSpeechRecognitionEngine {
    // MARK: - Properties
    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()

    // MARK: - Initialization
    func initialize(language: String) async throws {
        // Request authorization
        let status = await SFSpeechRecognizer.requestAuthorization()
        guard status == .authorized else {
            throw SpeechError.notAuthorized
        }

        // Create recognizer
        let locale = Locale(identifier: language)
        speechRecognizer = SFSpeechRecognizer(locale: locale)

        guard speechRecognizer?.isAvailable == true else {
            throw SpeechError.recognizerNotAvailable
        }

        // Configure audio session
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
    }

    // MARK: - Recognition
    func startRecognition(
        onPartial: @escaping (String) -> Void,
        onFinal: @escaping (String, Float) -> Void,
        onError: @escaping (Error) -> Void
    ) throws {
        // Cancel any ongoing task
        recognitionTask?.cancel()
        recognitionTask = nil

        // Create recognition request
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else {
            throw SpeechError.requestCreationFailed
        }

        recognitionRequest.shouldReportPartialResults = true
        recognitionRequest.requiresOnDeviceRecognition = true  // Offline

        // Setup audio engine
        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)

        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            recognitionRequest.append(buffer)
        }

        // Start audio engine
        audioEngine.prepare()
        try audioEngine.start()

        // Start recognition task
        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { result, error in
            if let result = result {
                let transcription = result.bestTranscription
                let text = transcription.formattedString

                // Calculate average confidence
                let avgConfidence = transcription.segments.isEmpty ? 0.0 :
                    transcription.segments.map { $0.confidence }.reduce(0, +) / Float(transcription.segments.count)

                if result.isFinal {
                    onFinal(text, avgConfidence)
                } else {
                    onPartial(text)
                }
            }

            if let error = error {
                onError(error)
                self.stopRecognition()
            }
        }
    }

    func stopRecognition() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionTask?.finish()
        recognitionTask = nil
        recognitionRequest = nil
    }
}

// MARK: - Error Types
enum SpeechError: Error {
    case notAuthorized
    case recognizerNotAvailable
    case requestCreationFailed
}
```

---

## 11. Testing Strategy

### Unit Tests (XCTest)
```swift
func testSpeechRecognitionInitialization() async throws {
    let engine = AppleSpeechRecognitionEngine()
    let success = try await engine.initialize(language: "en-US")
    XCTAssertTrue(success)
}

func testOfflineRecognition() async throws {
    let engine = AppleSpeechRecognitionEngine()
    try await engine.initialize(language: "en-US")

    let expectation = XCTestExpectation(description: "Recognition completed")

    try engine.startRecognition(
        onPartial: { text in
            print("Partial: \(text)")
        },
        onFinal: { text, confidence in
            XCTAssertFalse(text.isEmpty)
            XCTAssertGreaterThan(confidence, 0.0)
            expectation.fulfill()
        },
        onError: { error in
            XCTFail("Recognition failed: \(error)")
        }
    )

    await fulfillment(of: [expectation], timeout: 10.0)
}
```

---

## 12. Performance Optimization Tips

### Memory Management
```swift
// Use weak references in closures
recognitionTask = speechRecognizer?.recognitionTask(with: request) { [weak self] result, error in
    guard let self = self else { return }
    // Handle result
}
```

### Battery Optimization
```swift
// Stop audio engine when not needed
func pauseRecognition() {
    audioEngine.pause()
}

func resumeRecognition() {
    try? audioEngine.start()
}
```

### Continuous Recognition Optimization
```swift
// Implement smart chunking for continuous recognition
private var recognitionStartTime: Date?
private let maxRecognitionDuration: TimeInterval = 55  // seconds

func monitorRecognitionDuration() {
    Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
        guard let self = self,
              let startTime = self.recognitionStartTime else { return }

        if Date().timeIntervalSince(startTime) > self.maxRecognitionDuration {
            self.restartRecognition()
        }
    }
}
```

---

## 13. Summary

### Key Takeaways

1. **Authorization Model:** Apple requires explicit user authorization (no license keys)
2. **Audio Management:** Apple requires manual audio engine setup (Vivoka is automatic)
3. **Continuous Recognition:** Apple has 1-minute limit (workaround: restart in chunks)
4. **Model Management:** Apple handles automatically (Vivoka requires manual download)
5. **Confidence Scores:** Apple provides per-word scores (Vivoka overall score)
6. **Alternative Transcriptions:** Apple provides multiple (Vivoka single best)
7. **VAD & Wake Words:** Vivoka built-in (Apple requires manual implementation)

### Implementation Recommendation

Use **Kotlin Multiplatform (KMP)** with:
- Shared interface in `commonMain`
- Vivoka implementation in `androidMain`
- Apple Speech implementation in `iosMain`
- Platform-specific wrappers for advanced features

This approach ensures **maximum code reuse** while leveraging **platform-specific strengths**.

---

**Document Version:** 1.0
**Last Updated:** 2025-11-02
**Author:** VOS4 Development Team
**Status:** Complete - Ready for Implementation
