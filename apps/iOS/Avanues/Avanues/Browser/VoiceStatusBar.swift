import SwiftUI

/// Voice feedback UI bar for the browser.
///
/// Shows:
/// - Mic listening/processing/idle state indicator
/// - Available voice command count
/// - Last executed command result
/// - Confidence indicator
///
/// Positioned at the bottom of the BrowserView, above the toolbar.
struct VoiceStatusBar: View {
    @ObservedObject var voiceState: VoiceState
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 10) {
            // Mic indicator
            micIndicator

            // Status text
            VStack(alignment: .leading, spacing: 2) {
                if let lastCommand = voiceState.lastCommand {
                    Text(lastCommand)
                        .font(.caption)
                        .foregroundStyle(voiceState.lastCommandSuccess ? .green : .red)
                        .lineLimit(1)
                } else if voiceState.isListening {
                    Text(voiceState.partialText.isEmpty ? "Listening..." : voiceState.partialText)
                        .font(.caption)
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                }

                Text("\(voiceState.availableCommandCount) voice commands")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            // Confidence indicator (when listening)
            if voiceState.isListening && voiceState.confidence > 0 {
                confidenceIndicator
            }

            // Mic toggle button
            Button(action: { voiceState.toggleListening() }) {
                Image(systemName: voiceState.isListening ? "mic.fill" : "mic.slash")
                    .font(.system(size: 18))
                    .foregroundStyle(voiceState.isListening ? .red : .secondary)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(.ultraThinMaterial)
    }

    private var micIndicator: some View {
        ZStack {
            Circle()
                .fill(micColor.opacity(0.2))
                .frame(width: 28, height: 28)

            if voiceState.isListening {
                Circle()
                    .fill(micColor.opacity(0.4))
                    .frame(width: 28, height: 28)
                    .scaleEffect(voiceState.isProcessing ? 1.3 : 1.0)
                    .animation(.easeInOut(duration: 0.6).repeatForever(autoreverses: true), value: voiceState.isProcessing)
            }

            Image(systemName: micIconName)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(micColor)
        }
    }

    private var confidenceIndicator: some View {
        HStack(spacing: 2) {
            ForEach(0..<3) { level in
                RoundedRectangle(cornerRadius: 1)
                    .fill(level < confidenceLevel ? Color.green : Color.gray.opacity(0.3))
                    .frame(width: 3, height: CGFloat(8 + level * 4))
            }
        }
    }

    private var micColor: Color {
        switch voiceState.state {
        case .idle: return .secondary
        case .listening: return .green
        case .processing: return .orange
        case .error: return .red
        }
    }

    private var micIconName: String {
        switch voiceState.state {
        case .idle: return "mic.slash.fill"
        case .listening: return "mic.fill"
        case .processing: return "waveform"
        case .error: return "exclamationmark.triangle.fill"
        }
    }

    private var confidenceLevel: Int {
        if voiceState.confidence > 0.8 { return 3 }
        if voiceState.confidence > 0.5 { return 2 }
        if voiceState.confidence > 0.2 { return 1 }
        return 0
    }
}

// MARK: - Voice State

/// Observable state for the voice recognition system.
///
/// Bridges between the KMP SpeechRecognition module and SwiftUI.
/// Phase 4 wires this to the real AppleSpeechEngine.
@MainActor
final class VoiceState: ObservableObject {
    enum State {
        case idle, listening, processing, error
    }

    @Published var state: State = .idle
    @Published var partialText: String = ""
    @Published var lastCommand: String? = nil
    @Published var lastCommandSuccess: Bool = false
    @Published var confidence: Double = 0.0
    @Published var availableCommandCount: Int = 0
    @Published var errorMessage: String? = nil

    var isListening: Bool { state == .listening }
    var isProcessing: Bool { state == .processing }

    /// Toggle listening on/off.
    func toggleListening() {
        switch state {
        case .idle, .error:
            startListening()
        case .listening, .processing:
            stopListening()
        }
    }

    /// Start speech recognition.
    /// Phase 4 replaces this with real SFSpeechRecognizer integration.
    func startListening() {
        state = .listening
        partialText = ""
        lastCommand = nil
        errorMessage = nil
    }

    /// Stop speech recognition.
    func stopListening() {
        state = .idle
        partialText = ""
    }

    /// Called when a command is executed.
    func onCommandExecuted(text: String, success: Bool) {
        lastCommand = text
        lastCommandSuccess = success

        // Clear after 3 seconds
        Task {
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            if lastCommand == text {
                lastCommand = nil
            }
        }
    }
}
