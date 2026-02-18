import Foundation
import Speech
import AVFoundation

/// Native iOS speech recognition engine using SFSpeechRecognizer + AVAudioEngine.
///
/// Provides streaming partial + final results, multi-locale support,
/// on-device recognition preference (iOS 16+), and continuous listening mode.
/// Bridges to VoiceState for SwiftUI observation.
///
/// Mirrors the KMP IosSpeechRecognitionService but implemented directly in Swift
/// for native async/await integration and better error handling.
@MainActor
final class SpeechRecognitionManager: ObservableObject {

    // MARK: - Published State

    @Published private(set) var isAuthorized: Bool = false
    @Published private(set) var authorizationStatus: SFSpeechRecognizerAuthorizationStatus = .notDetermined
    @Published private(set) var supportsOnDevice: Bool = false

    // MARK: - Configuration

    private var locale: Locale
    private var preferOnDevice: Bool
    private var confidenceThreshold: Float
    private var continuousListening: Bool

    // MARK: - Speech Components

    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()

    // MARK: - Callbacks

    var onPartialResult: ((String, Float) -> Void)?
    var onFinalResult: ((String, Float, [String]) -> Void)?
    var onError: ((Error) -> Void)?
    var onStateChange: ((Bool) -> Void)?

    // MARK: - Initialization

    init(
        locale: String = "en-US",
        preferOnDevice: Bool = true,
        confidenceThreshold: Float = 0.6,
        continuousListening: Bool = false
    ) {
        self.locale = Locale(identifier: locale)
        self.preferOnDevice = preferOnDevice
        self.confidenceThreshold = confidenceThreshold
        self.continuousListening = continuousListening

        setupRecognizer()
    }

    // MARK: - Setup

    private func setupRecognizer() {
        speechRecognizer = SFSpeechRecognizer(locale: locale)
        supportsOnDevice = speechRecognizer?.supportsOnDeviceRecognition ?? false
    }

    /// Request speech recognition + microphone authorization.
    func requestAuthorization() async -> Bool {
        // Request speech recognition permission
        let speechStatus = await withCheckedContinuation { (cont: CheckedContinuation<SFSpeechRecognizerAuthorizationStatus, Never>) in
            SFSpeechRecognizer.requestAuthorization { status in
                cont.resume(returning: status)
            }
        }
        authorizationStatus = speechStatus

        // Request microphone permission
        let micGranted: Bool
        if #available(iOS 17.0, *) {
            micGranted = await AVAudioApplication.requestRecordPermission()
        } else {
            micGranted = await withCheckedContinuation { cont in
                AVAudioSession.sharedInstance().requestRecordPermission { granted in
                    cont.resume(returning: granted)
                }
            }
        }

        isAuthorized = speechStatus == .authorized && micGranted
        return isAuthorized
    }

    // MARK: - Locale

    /// Update the recognition locale (e.g., "en-US", "es-ES").
    func setLocale(_ localeId: String) {
        locale = Locale(identifier: localeId)
        setupRecognizer()
    }

    /// Update configuration from settings.
    func updateConfig(
        preferOnDevice: Bool? = nil,
        confidenceThreshold: Float? = nil,
        continuousListening: Bool? = nil
    ) {
        if let v = preferOnDevice { self.preferOnDevice = v }
        if let v = confidenceThreshold { self.confidenceThreshold = v }
        if let v = continuousListening { self.continuousListening = v }
    }

    // MARK: - Start / Stop

    /// Start listening for speech input.
    func startListening() throws {
        guard isAuthorized else {
            throw SpeechError.notAuthorized
        }
        guard let recognizer = speechRecognizer, recognizer.isAvailable else {
            throw SpeechError.recognizerUnavailable
        }

        // Cancel any existing task
        stopListening()

        // Configure audio session
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

        // Create recognition request
        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true

        // Prefer on-device if available and configured
        if preferOnDevice && recognizer.supportsOnDeviceRecognition {
            request.requiresOnDeviceRecognition = true
        }

        recognitionRequest = request

        // Install audio tap
        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { [weak self] buffer, _ in
            self?.recognitionRequest?.append(buffer)
        }

        // Start audio engine
        audioEngine.prepare()
        try audioEngine.start()

        // Start recognition task
        recognitionTask = recognizer.recognitionTask(with: request) { [weak self] result, error in
            guard let self = self else { return }

            if let error = error {
                Task { @MainActor in
                    self.handleRecognitionError(error)
                }
                return
            }

            guard let result = result else { return }

            let text = result.bestTranscription.formattedString
            let segments = result.bestTranscription.segments
            let confidence = segments.last?.confidence ?? 0.0

            // Collect alternatives
            let alternatives = result.transcriptions
                .dropFirst()
                .map { $0.formattedString }

            Task { @MainActor in
                if result.isFinal {
                    self.onFinalResult?(text, confidence, Array(alternatives))
                    // Auto-restart if continuous listening
                    if self.continuousListening {
                        self.restartListening()
                    } else {
                        self.stopListening()
                    }
                } else {
                    self.onPartialResult?(text, confidence)
                }
            }
        }

        onStateChange?(true)
    }

    /// Stop listening and clean up resources.
    func stopListening() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionRequest?.endAudio()
        recognitionRequest = nil
        recognitionTask?.cancel()
        recognitionTask = nil

        onStateChange?(false)
    }

    // MARK: - Internal

    private func handleRecognitionError(_ error: Error) {
        let nsError = error as NSError

        // Ignore cancellation errors (normal during stop)
        if nsError.domain == "kAFAssistantErrorDomain" && nsError.code == 216 {
            return // "Request was canceled"
        }

        onError?(error)
        stopListening()

        // Auto-restart on recoverable errors if continuous
        if continuousListening {
            restartListening()
        }
    }

    private func restartListening() {
        // Small delay before restart to avoid rapid cycles
        Task {
            try? await Task.sleep(nanoseconds: 500_000_000) // 0.5s
            do {
                try startListening()
            } catch {
                onError?(error)
            }
        }
    }

    /// Release all resources.
    func release() {
        stopListening()
        speechRecognizer = nil
    }

    // MARK: - Error Types

    enum SpeechError: LocalizedError {
        case notAuthorized
        case recognizerUnavailable

        var errorDescription: String? {
            switch self {
            case .notAuthorized:
                return "Speech recognition or microphone permission not granted"
            case .recognizerUnavailable:
                return "Speech recognizer unavailable for this locale"
            }
        }
    }
}
