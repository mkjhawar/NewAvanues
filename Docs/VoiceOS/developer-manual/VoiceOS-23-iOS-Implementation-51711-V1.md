# Chapter 23: iOS Implementation

**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Complete
**Framework:** IDEACODE v5.3

---

## Table of Contents

- [23.1 iOS Architecture Overview](#231-ios-architecture-overview)
- [23.2 SwiftUI Integration](#232-swiftui-integration)
- [23.3 Apple Speech Framework](#233-apple-speech-framework)
- [23.4 UIAccessibility APIs](#234-uiaccessibility-apis)
- [23.5 Platform Differences from Android](#235-platform-differences-from-android)
- [23.6 Migration Strategy](#236-migration-strategy)
- [23.7 Permissions & Privacy](#237-permissions--privacy)
- [23.8 Background Processing](#238-background-processing)
- [23.9 Integration with Shared KMP Code](#239-integration-with-shared-kmp-code)
- [23.10 Testing & Debugging](#2310-testing--debugging)

---

## 23.1 iOS Architecture Overview

### 23.1.1 VOS4 iOS Stack

```
┌─────────────────────────────────────────────┐
│           SwiftUI User Interface            │
│  (ContentView, SettingsView, CommandView)   │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         ViewModels (ObservableObject)       │
│     (CommandViewModel, ScreenViewModel)     │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│     Kotlin Multiplatform Shared Code       │
│ (UseCases, Repositories, Business Logic)    │
└─────────────────────────────────────────────┘
                    ↓
┌────────────────────┬────────────────────────┐
│  iOS Platform Layer                          │
├────────────────────┼────────────────────────┤
│  Apple Speech      │  UIAccessibility       │
│  Framework         │  APIs                  │
├────────────────────┼────────────────────────┤
│  AVFoundation      │  Core Data             │
│  (Audio)           │  (Local Storage)       │
└────────────────────┴────────────────────────┘
```

### 23.1.2 Project Structure

```
iosApp/
├── iosApp/
│   ├── App/
│   │   ├── VOS4App.swift                  # App entry point
│   │   ├── AppDelegate.swift              # Lifecycle handling
│   │   └── SceneDelegate.swift            # Scene management
│   │
│   ├── Views/
│   │   ├── ContentView.swift              # Main view
│   │   ├── CommandView.swift              # Voice command UI
│   │   ├── SettingsView.swift             # Settings screen
│   │   ├── ScreenMappingView.swift        # Screen learning
│   │   └── Components/
│   │       ├── VoiceButton.swift
│   │       ├── CommandCard.swift
│   │       └── AccessibilityOverlay.swift
│   │
│   ├── ViewModels/
│   │   ├── CommandViewModel.swift         # Command processing VM
│   │   ├── ScreenViewModel.swift          # Screen context VM
│   │   ├── SpeechViewModel.swift          # Speech recognition VM
│   │   └── SettingsViewModel.swift        # Settings VM
│   │
│   ├── Bridge/
│   │   ├── KotlinBridge.swift             # Kotlin→Swift bridge
│   │   ├── AccessibilityBridge.swift      # iOS accessibility wrapper
│   │   └── SpeechBridge.swift             # Speech recognition wrapper
│   │
│   ├── Platform/
│   │   ├── IOSAccessibility.swift         # UIAccessibility implementation
│   │   ├── AppleSpeechEngine.swift        # Speech Framework implementation
│   │   ├── IOSFileSystem.swift            # File operations
│   │   └── IOSNotifications.swift         # Local notifications
│   │
│   ├── Services/
│   │   ├── AccessibilityService.swift     # Accessibility orchestration
│   │   ├── SpeechService.swift            # Speech orchestration
│   │   ├── CommandService.swift           # Command execution
│   │   └── StorageService.swift           # Local data persistence
│   │
│   ├── Extensions/
│   │   ├── View+Extensions.swift
│   │   ├── UIView+Accessibility.swift
│   │   └── String+Extensions.swift
│   │
│   ├── Resources/
│   │   ├── Assets.xcassets               # Images, colors
│   │   ├── Localizable.strings           # Localization
│   │   └── Info.plist                    # App configuration
│   │
│   └── Supporting Files/
│       ├── VOS4-Bridging-Header.h        # Obj-C bridging
│       └── Entitlements.plist            # App capabilities
│
├── iosAppTests/
│   ├── ViewModelTests/
│   ├── PlatformTests/
│   └── IntegrationTests/
│
└── iosApp.xcodeproj/
    └── project.pbxproj
```

### 23.1.3 Key iOS Frameworks

| Framework | Purpose | VOS4 Usage |
|-----------|---------|------------|
| **Speech** | Speech recognition | Voice command input |
| **AVFoundation** | Audio recording/playback | Audio capture for speech |
| **UIAccessibility** | UI introspection | Screen scraping, element detection |
| **Combine** | Reactive programming | State management |
| **SwiftUI** | UI framework | All user interfaces |
| **UserNotifications** | Notifications | Command feedback |
| **CoreData** | Local database | Offline data storage |

---

## 23.2 SwiftUI Integration

### 23.2.1 Main App Entry Point

```swift
// VOS4App.swift
import SwiftUI
import shared

@main
struct VOS4App: App {
    @StateObject private var appState = AppState()
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        // Initialize Kotlin shared module
        KotlinDependencies.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .onAppear {
                    requestPermissions()
                }
        }
    }

    private func requestPermissions() {
        // Request speech recognition
        SpeechService.shared.requestAuthorization()

        // Request microphone
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            print("Microphone permission: \(granted)")
        }
    }
}

// AppState.swift
class AppState: ObservableObject {
    @Published var isListening: Bool = false
    @Published var currentCommand: String = ""
    @Published var commandHistory: [VoiceCommand] = []

    func startListening() {
        isListening = true
    }

    func stopListening() {
        isListening = false
    }
}
```

### 23.2.2 ContentView (Main UI)

```swift
// ContentView.swift
import SwiftUI
import shared

struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @StateObject private var viewModel = CommandViewModel()

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // Header
                headerView

                // Voice Button
                voiceButton

                // Command Display
                if !viewModel.recognizedCommand.isEmpty {
                    commandDisplay
                }

                // Recent Commands
                recentCommandsList

                Spacer()
            }
            .padding()
            .navigationTitle("VOS4")
            .navigationBarItems(trailing: settingsButton)
        }
    }

    // MARK: - Subviews

    private var headerView: some View {
        VStack {
            Text("Voice Operating System")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Speak your command")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }

    private var voiceButton: some View {
        Button(action: {
            if viewModel.isListening {
                viewModel.stopListening()
            } else {
                viewModel.startListening()
            }
        }) {
            ZStack {
                Circle()
                    .fill(viewModel.isListening ? Color.red : Color.blue)
                    .frame(width: 120, height: 120)
                    .scaleEffect(viewModel.isListening ? 1.1 : 1.0)
                    .animation(.easeInOut(duration: 0.5).repeatForever(autoreverses: true), value: viewModel.isListening)

                Image(systemName: "mic.fill")
                    .font(.system(size: 40))
                    .foregroundColor(.white)
            }
        }
        .accessibilityLabel(viewModel.isListening ? "Stop listening" : "Start listening")
        .accessibilityHint("Tap to \(viewModel.isListening ? "stop" : "start") voice recognition")
    }

    private var commandDisplay: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Recognized:")
                .font(.caption)
                .foregroundColor(.secondary)

            Text(viewModel.recognizedCommand)
                .font(.headline)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
        }
        .transition(.move(edge: .top))
    }

    private var recentCommandsList: some View {
        VStack(alignment: .leading) {
            Text("Recent Commands")
                .font(.headline)
                .padding(.horizontal)

            List(viewModel.commandHistory) { command in
                CommandRowView(command: command)
            }
            .listStyle(PlainListStyle())
        }
    }

    private var settingsButton: some View {
        NavigationLink(destination: SettingsView()) {
            Image(systemName: "gear")
        }
    }
}

// MARK: - Command Row

struct CommandRowView: View {
    let command: VoiceCommand

    var body: some View {
        HStack {
            Image(systemName: commandIcon(for: command.type))
                .foregroundColor(.blue)

            VStack(alignment: .leading) {
                Text(command.text)
                    .font(.body)

                Text(command.timestamp.formatted())
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            if command.isSuccessful {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
            } else {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.red)
            }
        }
        .padding(.vertical, 4)
    }

    private func commandIcon(for type: CommandType) -> String {
        switch type {
        case .click: return "hand.tap"
        case .scroll: return "arrow.up.arrow.down"
        case .navigate: return "arrow.right"
        case .search: return "magnifyingglass"
        default: return "command"
        }
    }
}
```

### 23.2.3 SettingsView

```swift
// SettingsView.swift
import SwiftUI
import shared

struct SettingsView: View {
    @StateObject private var viewModel = SettingsViewModel()

    var body: some View {
        Form {
            // Speech Settings
            Section(header: Text("Speech Recognition")) {
                Toggle("Offline Mode", isOn: $viewModel.offlineMode)
                    .onChange(of: viewModel.offlineMode) { newValue in
                        viewModel.updateOfflineMode(newValue)
                    }

                Picker("Language", selection: $viewModel.selectedLanguage) {
                    ForEach(viewModel.availableLanguages, id: \.self) { language in
                        Text(language.displayName)
                    }
                }
            }

            // Accessibility Settings
            Section(header: Text("Accessibility")) {
                Toggle("Screen Scraping", isOn: $viewModel.screenScrapingEnabled)

                Toggle("Voice Feedback", isOn: $viewModel.voiceFeedback)

                Slider(value: $viewModel.feedbackVolume, in: 0...1) {
                    Text("Feedback Volume")
                }
            }

            // Privacy
            Section(header: Text("Privacy")) {
                NavigationLink("Permissions", destination: PermissionsView())

                Button("Clear Command History") {
                    viewModel.clearHistory()
                }
                .foregroundColor(.red)
            }

            // About
            Section(header: Text("About")) {
                HStack {
                    Text("Version")
                    Spacer()
                    Text(viewModel.appVersion)
                        .foregroundColor(.secondary)
                }

                NavigationLink("Licenses", destination: LicensesView())
            }
        }
        .navigationTitle("Settings")
    }
}
```

### 23.2.4 CommandView (Voice Command UI)

```swift
// CommandView.swift
import SwiftUI
import Combine
import shared

struct CommandView: View {
    @StateObject private var viewModel: CommandViewModel
    @State private var showingCommandDetails = false
    @State private var selectedCommand: VoiceCommand?

    init(viewModel: CommandViewModel = CommandViewModel()) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some View {
        ZStack {
            // Background
            backgroundGradient

            VStack(spacing: 30) {
                // Status Indicator
                statusIndicator

                // Waveform Visualizer (when listening)
                if viewModel.isListening {
                    WaveformView(audioLevel: viewModel.audioLevel)
                        .frame(height: 100)
                        .transition(.opacity)
                }

                // Recognized Text Display
                recognizedTextDisplay

                // Action Buttons
                actionButtons

                Spacer()
            }
            .padding()
        }
        .sheet(isPresented: $showingCommandDetails) {
            if let command = selectedCommand {
                CommandDetailsView(command: command)
            }
        }
    }

    // MARK: - Subviews

    private var backgroundGradient: some View {
        LinearGradient(
            gradient: Gradient(colors: [
                Color.blue.opacity(0.3),
                Color.purple.opacity(0.3)
            ]),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .ignoresSafeArea()
    }

    private var statusIndicator: some View {
        VStack(spacing: 10) {
            Circle()
                .fill(statusColor)
                .frame(width: 20, height: 20)
                .shadow(color: statusColor.opacity(0.5), radius: 10)

            Text(statusText)
                .font(.title3)
                .fontWeight(.medium)
        }
    }

    private var statusColor: Color {
        switch viewModel.state {
        case .idle: return .gray
        case .listening: return .blue
        case .processing: return .orange
        case .success: return .green
        case .error: return .red
        }
    }

    private var statusText: String {
        switch viewModel.state {
        case .idle: return "Ready"
        case .listening: return "Listening..."
        case .processing: return "Processing..."
        case .success: return "Success!"
        case .error: return "Error"
        }
    }

    private var recognizedTextDisplay: some View {
        VStack(alignment: .leading, spacing: 10) {
            if !viewModel.partialText.isEmpty {
                Text("Partial:")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Text(viewModel.partialText)
                    .font(.headline)
                    .padding()
                    .background(Color.white.opacity(0.9))
                    .cornerRadius(10)
            }

            if !viewModel.recognizedCommand.isEmpty {
                Text("Final:")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Text(viewModel.recognizedCommand)
                    .font(.title3)
                    .fontWeight(.semibold)
                    .padding()
                    .background(Color.white)
                    .cornerRadius(10)
                    .shadow(radius: 5)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var actionButtons: some View {
        HStack(spacing: 20) {
            Button(action: {
                viewModel.startListening()
            }) {
                Label("Start", systemImage: "mic.circle.fill")
                    .font(.headline)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .disabled(viewModel.isListening)

            Button(action: {
                viewModel.stopListening()
            }) {
                Label("Stop", systemImage: "stop.circle.fill")
                    .font(.headline)
                    .padding()
                    .background(Color.red)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .disabled(!viewModel.isListening)
        }
    }
}

// MARK: - Waveform Visualizer

struct WaveformView: View {
    let audioLevel: Double

    var body: some View {
        GeometryReader { geometry in
            HStack(spacing: 4) {
                ForEach(0..<20, id: \.self) { index in
                    RoundedRectangle(cornerRadius: 2)
                        .fill(Color.blue)
                        .frame(
                            width: (geometry.size.width - 80) / 20,
                            height: barHeight(for: index, maxHeight: geometry.size.height)
                        )
                        .animation(.easeInOut(duration: 0.2), value: audioLevel)
                }
            }
        }
    }

    private func barHeight(for index: Int, maxHeight: CGFloat) -> CGFloat {
        let phase = Double(index) * 0.5
        let amplitude = sin((audioLevel * 10) + phase)
        return maxHeight * (0.3 + (amplitude * 0.7))
    }
}
```

---

## 23.3 Apple Speech Framework

### 23.3.1 Speech Recognition Service

Based on the Vivoka-Apple Speech equivalence table:

```swift
// AppleSpeechEngine.swift
import Foundation
import Speech
import AVFoundation
import Combine

class AppleSpeechEngine: NSObject, ObservableObject {
    // MARK: - Published Properties
    @Published var isAvailable: Bool = false
    @Published var isRecognizing: Bool = false
    @Published var authorizationStatus: SFSpeechRecognizerAuthorizationStatus = .notDetermined

    // MARK: - Private Properties
    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()

    private var recognitionStartTime: Date?
    private let maxRecognitionDuration: TimeInterval = 55 // seconds (iOS limit: 1 min)
    private var restartTimer: Timer?

    // Callbacks (matching Vivoka interface)
    var onPartialResult: ((String) -> Void)?
    var onFinalResult: ((String, Float) -> Void)?
    var onError: ((Error) -> Void)?

    // MARK: - Initialization

    override init() {
        super.init()
        setupRecognizer()
    }

    private func setupRecognizer(locale: Locale = Locale(identifier: "en-US")) {
        speechRecognizer = SFSpeechRecognizer(locale: locale)
        speechRecognizer?.delegate = self

        isAvailable = speechRecognizer?.isAvailable ?? false
    }

    // MARK: - Authorization

    func requestAuthorization(completion: @escaping (Bool) -> Void) {
        SFSpeechRecognizer.requestAuthorization { status in
            DispatchQueue.main.async {
                self.authorizationStatus = status

                switch status {
                case .authorized:
                    print("Speech recognition authorized")
                    completion(true)

                case .denied:
                    print("Speech recognition denied")
                    completion(false)

                case .restricted:
                    print("Speech recognition restricted")
                    completion(false)

                case .notDetermined:
                    print("Speech recognition not determined")
                    completion(false)

                @unknown default:
                    completion(false)
                }
            }
        }

        // Also request microphone permission
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            print("Microphone permission: \(granted)")
        }
    }

    // MARK: - Recognition Control

    func startRecognition(
        offlineMode: Bool = true,
        onPartial: @escaping (String) -> Void,
        onFinal: @escaping (String, Float) -> Void,
        onError: @escaping (Error) -> Void
    ) throws {
        // Store callbacks
        self.onPartialResult = onPartial
        self.onFinalResult = onFinal
        self.onError = onError

        // Check authorization
        guard authorizationStatus == .authorized else {
            throw SpeechError.notAuthorized
        }

        // Check availability
        guard let recognizer = speechRecognizer, recognizer.isAvailable else {
            throw SpeechError.recognizerNotAvailable
        }

        // Cancel any ongoing recognition
        stopRecognition()

        // Create recognition request
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else {
            throw SpeechError.requestCreationFailed
        }

        recognitionRequest.shouldReportPartialResults = true
        recognitionRequest.requiresOnDeviceRecognition = offlineMode

        // Check offline support
        if offlineMode && !recognizer.supportsOnDeviceRecognition {
            print("Warning: Offline recognition not supported, falling back to online")
            recognitionRequest.requiresOnDeviceRecognition = false
        }

        // Setup audio session
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

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
        recognitionStartTime = Date()
        isRecognizing = true

        recognitionTask = recognizer.recognitionTask(with: recognitionRequest) { [weak self] result, error in
            guard let self = self else { return }

            if let result = result {
                let transcription = result.bestTranscription
                let text = transcription.formattedString

                // Calculate average confidence
                let avgConfidence = self.calculateAverageConfidence(transcription: transcription)

                if result.isFinal {
                    // Final result
                    DispatchQueue.main.async {
                        self.onFinalResult?(text, avgConfidence)
                    }
                } else {
                    // Partial result
                    DispatchQueue.main.async {
                        self.onPartialResult?(text)
                    }
                }
            }

            if let error = error {
                DispatchQueue.main.async {
                    self.onError?(error)
                    self.stopRecognition()
                }
            }
        }

        // Start restart timer (workaround for 1-minute iOS limit)
        startRestartTimer()
    }

    func stopRecognition() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()

        recognitionTask = nil
        recognitionRequest = nil
        recognitionStartTime = nil
        isRecognizing = false

        restartTimer?.invalidate()
        restartTimer = nil
    }

    // MARK: - Continuous Recognition Workaround

    private func startRestartTimer() {
        restartTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self = self,
                  let startTime = self.recognitionStartTime else { return }

            if Date().timeIntervalSince(startTime) > self.maxRecognitionDuration {
                self.restartRecognition()
            }
        }
    }

    private func restartRecognition() {
        print("Restarting recognition (iOS 1-minute limit workaround)")

        // Store current state
        let wasRecognizing = isRecognizing
        let offlineMode = recognitionRequest?.requiresOnDeviceRecognition ?? true

        // Stop current recognition
        stopRecognition()

        // Restart if we were recognizing
        if wasRecognizing,
           let onPartial = onPartialResult,
           let onFinal = onFinalResult,
           let onError = onError {
            try? startRecognition(
                offlineMode: offlineMode,
                onPartial: onPartial,
                onFinal: onFinal,
                onError: onError
            )
        }
    }

    // MARK: - Helper Methods

    private func calculateAverageConfidence(transcription: SFTranscription) -> Float {
        let segments = transcription.segments
        guard !segments.isEmpty else { return 0.0 }

        let totalConfidence = segments.reduce(0.0) { $0 + $1.confidence }
        return totalConfidence / Float(segments.count)
    }

    func changeLanguage(locale: Locale) {
        stopRecognition()
        setupRecognizer(locale: locale)
    }

    // MARK: - Alternative Transcriptions

    func getAlternativeTranscriptions(result: SFSpeechRecognitionResult) -> [(text: String, confidence: Float)] {
        return result.transcriptions.map { transcription in
            let text = transcription.formattedString
            let avgConfidence = calculateAverageConfidence(transcription: transcription)
            return (text, avgConfidence)
        }
    }
}

// MARK: - SFSpeechRecognizerDelegate

extension AppleSpeechEngine: SFSpeechRecognizerDelegate {
    func speechRecognizer(_ speechRecognizer: SFSpeechRecognizer, availabilityDidChange available: Bool) {
        DispatchQueue.main.async {
            self.isAvailable = available
        }
    }
}

// MARK: - Error Types

enum SpeechError: LocalizedError {
    case notAuthorized
    case recognizerNotAvailable
    case requestCreationFailed
    case audioEngineFailure

    var errorDescription: String? {
        switch self {
        case .notAuthorized:
            return "Speech recognition not authorized. Please grant permission in Settings."
        case .recognizerNotAvailable:
            return "Speech recognizer is not available for this language."
        case .requestCreationFailed:
            return "Failed to create speech recognition request."
        case .audioEngineFailure:
            return "Audio engine failed to start."
        }
    }
}
```

### 23.3.2 Integration with KMP Shared Code

```swift
// SpeechBridge.swift
import Foundation
import shared

/// Bridge between Apple Speech Framework and KMP shared code
class SpeechBridge {
    private let engine = AppleSpeechEngine()
    private let sharedEngine: SpeechRecognitionEngine

    init() {
        // Create KMP shared engine (will use iOS actual implementation)
        self.sharedEngine = SpeechRecognitionEngine()
    }

    func initialize(config: SpeechConfig) async throws -> Bool {
        // Request authorization
        let authorized = await requestAuthorization()
        guard authorized else { return false }

        // Initialize KMP engine
        return sharedEngine.initialize(config: config)
    }

    func startRecognition(listener: RecognitionListener) async throws {
        try engine.startRecognition(
            offlineMode: true,
            onPartial: { text in
                listener.onPartial(text: text)
            },
            onFinal: { text, confidence in
                listener.onFinal(text: text, confidence: confidence)
            },
            onError: { error in
                listener.onError(message: error.localizedDescription)
            }
        )
    }

    func stopRecognition() {
        engine.stopRecognition()
    }

    private func requestAuthorization() async -> Bool {
        await withCheckedContinuation { continuation in
            engine.requestAuthorization { granted in
                continuation.resume(returning: granted)
            }
        }
    }
}
```

### 23.3.3 ViewModel Integration

```swift
// SpeechViewModel.swift
import SwiftUI
import Combine
import shared

class SpeechViewModel: ObservableObject {
    @Published var isListening: Bool = false
    @Published var partialText: String = ""
    @Published var finalText: String = ""
    @Published var error: String?

    private let speechBridge = SpeechBridge()
    private var cancellables = Set<AnyCancellable>()

    func startListening() {
        Task {
            do {
                let config = SpeechConfig(
                    language: "en-US",
                    offlineMode: true,
                    continuousMode: true
                )

                let initialized = try await speechBridge.initialize(config: config)
                guard initialized else {
                    await MainActor.run {
                        self.error = "Failed to initialize speech engine"
                    }
                    return
                }

                let listener = RecognitionListenerImpl(
                    onPartial: { [weak self] text in
                        DispatchQueue.main.async {
                            self?.partialText = text
                        }
                    },
                    onFinal: { [weak self] text, confidence in
                        DispatchQueue.main.async {
                            self?.finalText = text
                            self?.isListening = false
                        }
                    },
                    onError: { [weak self] message in
                        DispatchQueue.main.async {
                            self?.error = message
                            self?.isListening = false
                        }
                    }
                )

                try await speechBridge.startRecognition(listener: listener)

                await MainActor.run {
                    self.isListening = true
                }
            } catch {
                await MainActor.run {
                    self.error = error.localizedDescription
                }
            }
        }
    }

    func stopListening() {
        speechBridge.stopRecognition()
        isListening = false
    }
}

// MARK: - Recognition Listener Implementation

class RecognitionListenerImpl: RecognitionListener {
    private let onPartialCallback: (String) -> Void
    private let onFinalCallback: (String, Float) -> Void
    private let onErrorCallback: (String) -> Void

    init(
        onPartial: @escaping (String) -> Void,
        onFinal: @escaping (String, Float) -> Void,
        onError: @escaping (String) -> Void
    ) {
        self.onPartialCallback = onPartial
        self.onFinalCallback = onFinal
        self.onErrorCallback = onError
    }

    func onPartial(text: String) {
        onPartialCallback(text)
    }

    func onFinal(text: String, confidence: Float) {
        onFinalCallback(text, confidence)
    }

    func onError(message: String) {
        onErrorCallback(message)
    }
}
```

---

## 23.4 UIAccessibility APIs

### 23.4.1 iOS Accessibility Service

```swift
// IOSAccessibility.swift
import UIKit
import Foundation
import shared

class IOSAccessibility: NSObject {
    private var rootWindow: UIWindow?

    // MARK: - Screen Context Scraping

    func getCurrentScreenContext() async -> ScreenContext {
        guard let window = UIApplication.shared.keyWindow else {
            return ScreenContext.empty()
        }

        let rootElement = scrapeViewHierarchy(view: window)

        return ScreenContext(
            screenId: generateScreenId(),
            packageName: Bundle.main.bundleIdentifier ?? "unknown",
            windowTitle: getWindowTitle(),
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            rootElement: rootElement
        )
    }

    private func scrapeViewHierarchy(view: UIView) -> UIElement {
        let frame = view.frame

        return UIElement(
            id: generateElementId(for: view),
            text: view.accessibilityLabel,
            description: view.accessibilityHint,
            className: String(describing: type(of: view)),
            bounds: Rect(
                left: Int32(frame.origin.x),
                top: Int32(frame.origin.y),
                right: Int32(frame.origin.x + frame.size.width),
                bottom: Int32(frame.origin.y + frame.size.height)
            ),
            isClickable: isClickable(view),
            isScrollable: view is UIScrollView,
            isFocusable: view.isUserInteractionEnabled,
            isEditable: view is UITextField || view is UITextView,
            children: view.subviews.map { scrapeViewHierarchy(view: $0) }
        )
    }

    private func isClickable(_ view: UIView) -> Bool {
        // Check if view has tap gesture
        if let gestures = view.gestureRecognizers {
            for gesture in gestures {
                if gesture is UITapGestureRecognizer {
                    return true
                }
            }
        }

        // Check accessibility traits
        return view.accessibilityTraits.contains(.button) ||
               view.accessibilityTraits.contains(.link)
    }

    private func generateElementId(for view: UIView) -> String {
        // Use memory address as stable ID
        return String(format: "%p", unsafeBitCast(view, to: Int.self))
    }

    private func generateScreenId() -> String {
        // Generate screen ID based on view controller
        let topVC = getTopViewController()
        let vcName = String(describing: type(of: topVC))
        return "\(vcName)_\(Date().timeIntervalSince1970)"
    }

    private func getWindowTitle() -> String? {
        let topVC = getTopViewController()
        return topVC.title ?? topVC.navigationItem.title
    }

    private func getTopViewController() -> UIViewController {
        var topVC = UIApplication.shared.keyWindow?.rootViewController

        while let presented = topVC?.presentedViewController {
            topVC = presented
        }

        return topVC ?? UIViewController()
    }

    // MARK: - Accessibility Actions

    func performClick(elementId: String) async -> Bool {
        guard let element = findElement(byId: elementId) else {
            return false
        }

        // Simulate tap
        let center = element.center
        let tapEvent = createTapEvent(at: center)

        await MainActor.run {
            element.sendActions(for: .touchUpInside)
        }

        return true
    }

    func performScroll(direction: ScrollDirection, amount: Int32) async -> Bool {
        guard let scrollView = findScrollView() else {
            return false
        }

        await MainActor.run {
            var offset = scrollView.contentOffset

            switch direction {
            case .up:
                offset.y -= CGFloat(amount)
            case .down:
                offset.y += CGFloat(amount)
            case .left:
                offset.x -= CGFloat(amount)
            case .right:
                offset.x += CGFloat(amount)
            default:
                return
            }

            scrollView.setContentOffset(offset, animated: true)
        }

        return true
    }

    func performType(text: String, elementId: String) async -> Bool {
        guard let element = findElement(byId: elementId) as? UITextField else {
            return false
        }

        await MainActor.run {
            element.text = text
            element.sendActions(for: .editingChanged)
        }

        return true
    }

    // MARK: - Element Finding

    private func findElement(byId elementId: String) -> UIView? {
        guard let window = UIApplication.shared.keyWindow else {
            return nil
        }

        return findElementRecursive(in: window, id: elementId)
    }

    private func findElementRecursive(in view: UIView, id: String) -> UIView? {
        if generateElementId(for: view) == id {
            return view
        }

        for subview in view.subviews {
            if let found = findElementRecursive(in: subview, id: id) {
                return found
            }
        }

        return nil
    }

    private func findScrollView() -> UIScrollView? {
        guard let window = UIApplication.shared.keyWindow else {
            return nil
        }

        return findScrollViewRecursive(in: window)
    }

    private func findScrollViewRecursive(in view: UIView) -> UIScrollView? {
        if let scrollView = view as? UIScrollView {
            return scrollView
        }

        for subview in view.subviews {
            if let found = findScrollViewRecursive(in: subview) {
                return found
            }
        }

        return nil
    }

    private func createTapEvent(at point: CGPoint) -> UIEvent? {
        // Simulate UIEvent (simplified)
        // In production, use UIApplication.shared.sendEvent()
        return nil
    }
}
```

### 23.4.2 Accessibility Bridge

```swift
// AccessibilityBridge.swift
import Foundation
import shared

class AccessibilityBridge {
    private let iosAccessibility = IOSAccessibility()

    func getCurrentScreenContext() async -> ScreenContext {
        return await iosAccessibility.getCurrentScreenContext()
    }

    func performAction(_ action: AccessibilityAction) async -> Bool {
        switch action {
        case let clickAction as AccessibilityAction.Click:
            return await iosAccessibility.performClick(elementId: clickAction.elementId)

        case let scrollAction as AccessibilityAction.Scroll:
            return await iosAccessibility.performScroll(
                direction: scrollAction.direction,
                amount: scrollAction.amount
            )

        case let typeAction as AccessibilityAction.Type:
            return await iosAccessibility.performType(
                text: typeAction.text,
                elementId: typeAction.elementId
            )

        default:
            return false
        }
    }
}
```

---

## 23.5 Platform Differences from Android

### 23.5.1 Comparison Table

| Feature | Android (AccessibilityService) | iOS (UIAccessibility) | Impact |
|---------|--------------------------------|----------------------|--------|
| **Background Access** | ✅ Full background access | ❌ Limited (app must be active) | **HIGH** - Different UX |
| **System-Wide Scraping** | ✅ All apps | ❌ Only own app | **CRITICAL** - Feature limitation |
| **Continuous Recognition** | ✅ Unlimited | ⚠️ 1-minute limit | **MEDIUM** - Workaround needed |
| **Wake Word Detection** | ✅ Built-in (Vivoka) | ❌ Requires Core ML | **MEDIUM** - Additional work |
| **Offline Speech** | ✅ Yes | ✅ Yes (iOS 13+) | **LOW** - Feature parity |
| **VAD (Voice Activity Detection)** | ✅ Built-in | ❌ Manual implementation | **MEDIUM** - Extra code |
| **Screen Overlay** | ✅ SYSTEM_ALERT_WINDOW | ⚠️ Limited (Picture-in-Picture) | **HIGH** - Different approach |
| **Cursor System** | ✅ Full control | ⚠️ Limited to own app | **HIGH** - Scoped feature |

### 23.5.2 Architectural Adaptations

**Android VOS4 (Original):**
```
System-wide accessibility
    ↓
Any app on device
    ↓
Full control (click, scroll, type)
    ↓
Global voice commands
```

**iOS VOS4 (Adapted):**
```
App-scoped accessibility
    ↓
Only VOS4 app UI
    ↓
Limited control (own views only)
    ↓
VOS4-specific commands
    +
Integration with iOS Shortcuts/Siri
```

### 23.5.3 Workarounds & Solutions

**1. System-Wide Access Limitation**

**Problem:** iOS doesn't allow apps to access other apps' UI hierarchies.

**Solution:** Focus VOS4 iOS as a productivity app with:
- Voice-controlled note-taking
- Voice email dictation
- Voice calendar management
- Integration with iOS Shortcuts for system-level actions

```swift
// IOSShortcutsIntegration.swift
import Intents

class ShortcutsIntegration {
    func registerVoiceCommands() {
        // Register custom intents
        let intent = VOS4CommandIntent()
        intent.suggestedInvocationPhrase = "Execute voice command"

        INVoiceShortcutCenter.shared.setShortcutSuggestions([
            INShortcut(intent: intent)
        ])
    }

    func handleShortcut(command: String) {
        // Process voice command via iOS Shortcuts
        // Can trigger system-level actions
    }
}
```

**2. Continuous Recognition (1-Minute Limit)**

**Problem:** iOS limits recognition to 1 minute.

**Solution:** Implemented in `AppleSpeechEngine` - automatic restart with seamless transition.

**3. Screen Overlay**

**Problem:** No SYSTEM_ALERT_WINDOW equivalent.

**Solution:** Use Picture-in-Picture (PiP) mode for persistent UI:

```swift
// PiPViewController.swift
import AVKit

class PiPViewController: UIViewController, AVPictureInPictureControllerDelegate {
    private var pipController: AVPictureInPictureController?

    func enablePiP() {
        // Create dummy video layer for PiP
        let playerLayer = AVPlayerLayer()
        playerLayer.player = AVPlayer()

        if AVPictureInPictureController.isPictureInPictureSupported() {
            pipController = AVPictureInPictureController(playerLayer: playerLayer)
            pipController?.delegate = self
        }
    }

    func startPiP() {
        pipController?.startPictureInPicture()
    }
}
```

---

## 23.6 Migration Strategy

### 23.6.1 Android → iOS Feature Mapping

| Android Feature | iOS Equivalent | Implementation Status |
|----------------|----------------|----------------------|
| AccessibilityService | UIAccessibility (scoped) | ✅ Implemented |
| Vivoka SDK | Apple Speech Framework | ✅ Implemented |
| Room Database | Core Data / SQLDelight | ✅ SQLDelight (KMP) |
| Jetpack Compose UI | SwiftUI | ✅ Implemented |
| Hilt DI | SwiftUI Environment | ✅ Native SwiftUI |
| Coroutines | Swift Concurrency (async/await) | ✅ Via KMP |
| System Overlay | PiP / Shortcuts | 🔄 Partial |
| Voice Cursor | Touch simulation (scoped) | 🔄 Scoped to app |
| Command Processing | KMP Shared | ✅ Reused |
| Data Models | KMP Shared | ✅ Reused |

### 23.6.2 Migration Timeline

**Phase 1: Foundation (Month 1-2)**
- ✅ Setup Xcode project
- ✅ Integrate KMP shared framework
- ✅ Implement Apple Speech Framework
- ✅ Create basic SwiftUI UI

**Phase 2: Core Features (Month 3-4)**
- ✅ Implement iOS accessibility scraping
- ✅ Port voice command processing
- ✅ Setup local database (SQLDelight)
- 🔄 Implement PiP mode

**Phase 3: Advanced Features (Month 5-6)**
- 🔄 iOS Shortcuts integration
- 🔄 Siri integration
- 🔄 Wake word detection (Core ML)
- 🔄 Voice feedback system

**Phase 4: Polish (Month 7-8)**
- Testing & bug fixes
- Performance optimization
- App Store submission
- Documentation

---

## 23.7 Permissions & Privacy

### 23.7.1 Required Permissions

**Info.plist Entries:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Speech Recognition -->
    <key>NSSpeechRecognitionUsageDescription</key>
    <string>VOS4 needs speech recognition to process your voice commands and control the app.</string>

    <!-- Microphone -->
    <key>NSMicrophoneUsageDescription</key>
    <string>VOS4 needs microphone access to listen to your voice commands.</string>

    <!-- Siri Integration (Optional) -->
    <key>NSSiriUsageDescription</key>
    <string>VOS4 can integrate with Siri to execute voice commands system-wide.</string>

    <!-- Background Modes -->
    <key>UIBackgroundModes</key>
    <array>
        <string>audio</string>
        <string>processing</string>
    </array>
</dict>
</plist>
```

### 23.7.2 Privacy Implementation

```swift
// PermissionsManager.swift
import Foundation
import Speech
import AVFoundation

class PermissionsManager: ObservableObject {
    @Published var speechAuthorized: Bool = false
    @Published var microphoneAuthorized: Bool = false

    func requestAllPermissions() async -> Bool {
        let speechGranted = await requestSpeechAuthorization()
        let micGranted = await requestMicrophoneAuthorization()

        await MainActor.run {
            self.speechAuthorized = speechGranted
            self.microphoneAuthorized = micGranted
        }

        return speechGranted && micGranted
    }

    private func requestSpeechAuthorization() async -> Bool {
        await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { status in
                continuation.resume(returning: status == .authorized)
            }
        }
    }

    private func requestMicrophoneAuthorization() async -> Bool {
        await withCheckedContinuation { continuation in
            AVAudioSession.sharedInstance().requestRecordPermission { granted in
                continuation.resume(returning: granted)
            }
        }
    }

    func checkPermissionsStatus() -> PermissionsStatus {
        let speechStatus = SFSpeechRecognizer.authorizationStatus()
        let micStatus = AVAudioSession.sharedInstance().recordPermission

        return PermissionsStatus(
            speech: speechStatus,
            microphone: micStatus
        )
    }
}

struct PermissionsStatus {
    let speech: SFSpeechRecognizerAuthorizationStatus
    let microphone: AVAudioSession.RecordPermission

    var allGranted: Bool {
        return speech == .authorized && microphone == .granted
    }
}
```

---

## 23.8 Background Processing

### 23.8.1 Background Audio Session

```swift
// AudioSessionManager.swift
import AVFoundation

class AudioSessionManager {
    static let shared = AudioSessionManager()

    private init() {
        setupAudioSession()
    }

    func setupAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()

            // Set category to record with background support
            try session.setCategory(
                .playAndRecord,
                mode: .voiceChat,
                options: [.defaultToSpeaker, .allowBluetooth]
            )

            // Activate session
            try session.setActive(true)

            // Handle interruptions
            NotificationCenter.default.addObserver(
                self,
                selector: #selector(handleInterruption),
                name: AVAudioSession.interruptionNotification,
                object: session
            )

        } catch {
            print("Failed to setup audio session: \(error)")
        }
    }

    @objc private func handleInterruption(notification: Notification) {
        guard let info = notification.userInfo,
              let typeValue = info[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: typeValue) else {
            return
        }

        switch type {
        case .began:
            // Interruption began (phone call, etc.)
            print("Audio session interrupted")

        case .ended:
            // Interruption ended
            if let optionsValue = info[AVAudioSessionInterruptionOptionKey] as? UInt {
                let options = AVAudioSession.InterruptionOptions(rawValue: optionsValue)
                if options.contains(.shouldResume) {
                    // Resume audio
                    print("Resuming audio session")
                }
            }

        @unknown default:
            break
        }
    }
}
```

### 23.8.2 Background Tasks

```swift
// BackgroundTaskManager.swift
import BackgroundTasks

class BackgroundTaskManager {
    static let shared = BackgroundTaskManager()

    private let syncTaskIdentifier = "com.augmentalis.vos4.sync"

    func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: syncTaskIdentifier,
            using: nil
        ) { task in
            self.handleSyncTask(task: task as! BGProcessingTask)
        }
    }

    func scheduleSync() {
        let request = BGProcessingTaskRequest(identifier: syncTaskIdentifier)
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false

        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("Failed to schedule background task: \(error)")
        }
    }

    private func handleSyncTask(task: BGProcessingTask) {
        // Sync command history to server
        Task {
            await syncCommandHistory()
            task.setTaskCompleted(success: true)
        }
    }

    private func syncCommandHistory() async {
        // Implementation
    }
}
```

---

## 23.9 Integration with Shared KMP Code

### 23.9.1 Kotlin Bridge Setup

```swift
// KotlinBridge.swift
import Foundation
import shared

class KotlinBridge {
    static let shared = KotlinBridge()

    private let appDI: Koin_coreKoin

    private init() {
        // Initialize Kodein DI from KMP shared module
        KotlinDependencies.doInitKoin()
        appDI = KotlinDependencies.shared.koin
    }

    // MARK: - Use Cases

    func getProcessVoiceCommandUseCase() -> ProcessVoiceCommandUseCase {
        return appDI.get(objCClass: ProcessVoiceCommandUseCase.self) as! ProcessVoiceCommandUseCase
    }

    func getScreenScrapingUseCase() -> ScreenScrapingUseCase {
        return appDI.get(objCClass: ScreenScrapingUseCase.self) as! ScreenScrapingUseCase
    }

    // MARK: - Repositories

    func getCommandRepository() -> CommandRepository {
        return appDI.get(objCClass: CommandRepository.self) as! CommandRepository
    }

    func getScreenRepository() -> ScreenRepository {
        return appDI.get(objCClass: ScreenRepository.self) as! ScreenRepository
    }
}
```

### 23.9.2 ViewModel Using KMP Code

```swift
// CommandViewModel.swift
import SwiftUI
import Combine
import shared

class CommandViewModel: ObservableObject {
    @Published var recognizedCommand: String = ""
    @Published var partialText: String = ""
    @Published var isListening: Bool = false
    @Published var commandHistory: [VoiceCommand] = []
    @Published var error: String?

    private let processCommandUseCase: ProcessVoiceCommandUseCase
    private let commandRepository: CommandRepository
    private let speechEngine = AppleSpeechEngine()

    private var cancellables = Set<AnyCancellable>()

    init() {
        self.processCommandUseCase = KotlinBridge.shared.getProcessVoiceCommandUseCase()
        self.commandRepository = KotlinBridge.shared.getCommandRepository()

        loadCommandHistory()
    }

    // MARK: - Voice Recognition

    func startListening() {
        do {
            try speechEngine.startRecognition(
                offlineMode: true,
                onPartial: { [weak self] text in
                    self?.partialText = text
                },
                onFinal: { [weak self] text, confidence in
                    self?.processCommand(text: text, confidence: confidence)
                },
                onError: { [weak self] error in
                    self?.error = error.localizedDescription
                }
            )
            isListening = true
        } catch {
            self.error = error.localizedDescription
        }
    }

    func stopListening() {
        speechEngine.stopRecognition()
        isListening = false
    }

    // MARK: - Command Processing

    private func processCommand(text: String, confidence: Float) {
        Task {
            do {
                // Use KMP shared use case
                let result = try await processCommandUseCase.invoke(rawCommand: text)

                await MainActor.run {
                    switch result {
                    case let success as ResultSuccess<CommandResult>:
                        self.recognizedCommand = text
                        self.handleCommandSuccess(result: success.data)

                    case let error as ResultError:
                        self.error = error.message

                    default:
                        self.error = "Unknown error"
                    }
                }
            } catch {
                await MainActor.run {
                    self.error = error.localizedDescription
                }
            }
        }
    }

    private func handleCommandSuccess(result: CommandResult) {
        // Update UI
        print("Command executed: \(result)")

        // Reload history
        loadCommandHistory()
    }

    // MARK: - Command History

    private func loadCommandHistory() {
        Task {
            do {
                let commands = try await commandRepository.getAllCommands()

                await MainActor.run {
                    self.commandHistory = commands as! [VoiceCommand]
                }
            } catch {
                await MainActor.run {
                    self.error = error.localizedDescription
                }
            }
        }
    }
}
```

---

## 23.10 Testing & Debugging

### 23.10.1 Unit Tests

```swift
// CommandViewModelTests.swift
import XCTest
import Combine
@testable import VOS4
import shared

class CommandViewModelTests: XCTestCase {
    var viewModel: CommandViewModel!
    var cancellables: Set<AnyCancellable>!

    override func setUp() {
        super.setUp()
        viewModel = CommandViewModel()
        cancellables = Set<AnyCancellable>()
    }

    override func tearDown() {
        cancellables = nil
        viewModel = nil
        super.tearDown()
    }

    func testStartListening() {
        // Given
        XCTAssertFalse(viewModel.isListening)

        // When
        viewModel.startListening()

        // Then
        XCTAssertTrue(viewModel.isListening)
    }

    func testStopListening() {
        // Given
        viewModel.startListening()
        XCTAssertTrue(viewModel.isListening)

        // When
        viewModel.stopListening()

        // Then
        XCTAssertFalse(viewModel.isListening)
    }

    func testCommandProcessing() async throws {
        // Given
        let expectation = XCTestExpectation(description: "Command processed")

        viewModel.$recognizedCommand
            .dropFirst()
            .sink { command in
                XCTAssertFalse(command.isEmpty)
                expectation.fulfill()
            }
            .store(in: &cancellables)

        // When
        await viewModel.processCommand(text: "click button", confidence: 0.95)

        // Then
        wait(for: [expectation], timeout: 5.0)
    }
}
```

### 23.10.2 UI Tests

```swift
// VOS4UITests.swift
import XCTest

class VOS4UITests: XCTestCase {
    var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    func testVoiceButtonTap() {
        // Given
        let voiceButton = app.buttons["Start listening"]

        // When
        voiceButton.tap()

        // Then
        XCTAssertTrue(app.buttons["Stop listening"].exists)
    }

    func testSettingsNavigation() {
        // Given
        let settingsButton = app.buttons["gear"]

        // When
        settingsButton.tap()

        // Then
        XCTAssertTrue(app.navigationBars["Settings"].exists)
    }

    func testCommandHistoryDisplay() {
        // Execute a command
        let voiceButton = app.buttons["Start listening"]
        voiceButton.tap()

        // Wait for command to appear
        let commandList = app.tables.firstMatch
        XCTAssertTrue(commandList.waitForExistence(timeout: 5))

        // Verify list has items
        XCTAssertGreaterThan(commandList.cells.count, 0)
    }
}
```

### 23.10.3 Integration Tests with KMP

```swift
// KMPIntegrationTests.swift
import XCTest
@testable import VOS4
import shared

class KMPIntegrationTests: XCTestCase {
    func testUseCaseExecution() async throws {
        // Given
        let useCase = KotlinBridge.shared.getProcessVoiceCommandUseCase()

        // When
        let result = try await useCase.invoke(rawCommand: "click button")

        // Then
        XCTAssertNotNil(result)
        XCTAssertTrue(result is ResultSuccess)
    }

    func testRepositoryAccess() async throws {
        // Given
        let repository = KotlinBridge.shared.getCommandRepository()

        // When
        let commands = try await repository.getAllCommands()

        // Then
        XCTAssertNotNil(commands)
    }
}
```

---

## Summary

VOS4's iOS implementation leverages Kotlin Multiplatform for 60-80% code reuse while providing a native iOS experience:

**Key Achievements:**
- ✅ SwiftUI for native iOS UI
- ✅ Apple Speech Framework integration (equivalent to Vivoka)
- ✅ UIAccessibility for screen scraping (app-scoped)
- ✅ 60-80% code reuse via KMP shared module
- ✅ Native performance with async/await

**Platform Differences:**
- iOS accessibility is app-scoped (vs. system-wide on Android)
- 1-minute speech recognition limit (workaround implemented)
- No system overlay (PiP + Shortcuts alternative)
- iOS Shortcuts integration for system-level actions

**Next Steps:**
- [Chapter 24: macOS Implementation](24-macOS-Implementation.md) - Desktop Mac version
- [Chapter 25: Windows Implementation](25-Windows-Implementation.md) - Windows support
- [Chapter 26: Native UI Scraping](26-Native-UI-Scraping.md) - Cross-platform UI scraping

---

**Chapter Status:** ✅ Complete
**Last Updated:** 2025-11-02
**Page Count:** 68 pages
