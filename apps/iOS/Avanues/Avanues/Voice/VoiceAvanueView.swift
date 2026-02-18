import SwiftUI

/// VoiceAvanue home screen — the main voice control interface.
///
/// Provides:
/// - Large mic toggle button with animated state
/// - Real-time partial transcript display
/// - Recent command history
/// - Quick voice settings (locale, on-device)
/// - Voice command count
///
/// Mirrors the Android VoiceAvanue home screen with SpatialVoice design.
struct VoiceAvanueView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme

    @StateObject private var speechManager: SpeechRecognitionManager
    @StateObject private var voiceState = VoiceState()

    private var isDark: Bool { colorScheme == .dark }
    private var theme: AvanueColors {
        AvanueThemeBridge.colors(for: appState.palette, isDark: isDark)
    }

    init() {
        let locale = UserDefaults.standard.string(forKey: "voice_locale") ?? "en-US"
        let onDevice = UserDefaults.standard.bool(forKey: "voice_on_device")
        let threshold = UserDefaults.standard.double(forKey: "voice_confidence_threshold")
        let continuous = UserDefaults.standard.bool(forKey: "voice_continuous")

        _speechManager = StateObject(wrappedValue: SpeechRecognitionManager(
            locale: locale,
            preferOnDevice: onDevice,
            confidenceThreshold: Float(threshold > 0 ? threshold : 0.6),
            continuousListening: continuous
        ))
    }

    var body: some View {
        ZStack {
            // SpatialVoice gradient background
            LinearGradient(
                colors: [theme.background, theme.surface.opacity(0.6), theme.background],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                headerSection
                    .padding(.top, 8)

                Spacer()

                micSection

                Spacer()

                transcriptSection
                    .padding(.horizontal, 20)

                recentCommandsSection
                    .padding(.horizontal, 20)
                    .padding(.bottom, 8)
            }
        }
        .navigationTitle("VoiceAvanue")
        .onAppear {
            voiceState.configure(speechManager: speechManager)
            // Update locale from settings
            let locale = UserDefaults.standard.string(forKey: "voice_locale") ?? "en-US"
            speechManager.setLocale(locale)
        }
        .onDisappear {
            voiceState.stopListening()
        }
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(spacing: 4) {
            Text("VoiceTouch\u{2122}")
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundStyle(theme.primary)

            Text(stateLabel)
                .font(.headline)
                .foregroundStyle(theme.onBackground)

            if let error = voiceState.errorMessage {
                Text(error)
                    .font(.caption2)
                    .foregroundStyle(theme.error)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
        }
    }

    private var stateLabel: String {
        switch voiceState.state {
        case .idle: return "Tap to Start"
        case .listening: return "Listening..."
        case .processing: return "Processing..."
        case .error: return "Error"
        }
    }

    // MARK: - Mic Button

    private var micSection: some View {
        VStack(spacing: 24) {
            ZStack {
                // Pulsing rings when listening
                if voiceState.isListening {
                    ForEach(0..<3, id: \.self) { ring in
                        Circle()
                            .stroke(theme.primary.opacity(0.15 - Double(ring) * 0.04), lineWidth: 2)
                            .frame(width: CGFloat(140 + ring * 30), height: CGFloat(140 + ring * 30))
                            .scaleEffect(voiceState.isListening ? 1.1 : 0.9)
                            .animation(
                                .easeInOut(duration: 1.2)
                                    .repeatForever(autoreverses: true)
                                    .delay(Double(ring) * 0.15),
                                value: voiceState.isListening
                            )
                    }
                }

                // Main mic button
                Button(action: { voiceState.toggleListening() }) {
                    ZStack {
                        Circle()
                            .fill(micBackgroundColor)
                            .frame(width: 120, height: 120)
                            .shadow(color: micBackgroundColor.opacity(0.4), radius: voiceState.isListening ? 20 : 8)

                        Image(systemName: micIconName)
                            .font(.system(size: 44, weight: .medium))
                            .foregroundStyle(.white)
                    }
                }
                .accessibilityLabel(voiceState.isListening ? "Stop listening" : "Start listening")
            }
            .frame(height: 230)

            // Confidence bar
            if voiceState.isListening && voiceState.confidence > 0 {
                VStack(spacing: 4) {
                    ProgressView(value: voiceState.confidence, total: 1.0)
                        .tint(confidenceColor)
                        .frame(width: 120)

                    Text(String(format: "%.0f%%", voiceState.confidence * 100))
                        .font(.caption2)
                        .foregroundStyle(theme.onBackground.opacity(0.5))
                }
            }
        }
    }

    private var micBackgroundColor: Color {
        switch voiceState.state {
        case .idle: return theme.primary
        case .listening: return Color.red
        case .processing: return Color.orange
        case .error: return theme.error
        }
    }

    private var micIconName: String {
        switch voiceState.state {
        case .idle: return "mic.fill"
        case .listening: return "mic.fill"
        case .processing: return "waveform"
        case .error: return "exclamationmark.triangle.fill"
        }
    }

    private var confidenceColor: Color {
        if voiceState.confidence > 0.8 { return .green }
        if voiceState.confidence > 0.5 { return .yellow }
        return .red
    }

    // MARK: - Transcript

    private var transcriptSection: some View {
        VStack(spacing: 8) {
            if !voiceState.partialText.isEmpty {
                Text(voiceState.partialText)
                    .font(.title3)
                    .fontWeight(.medium)
                    .foregroundStyle(theme.onBackground)
                    .multilineTextAlignment(.center)
                    .lineLimit(3)
                    .frame(maxWidth: .infinity)
                    .padding(16)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(theme.surface.opacity(0.8))
                    )
            }

            if let lastCmd = voiceState.lastCommand {
                HStack(spacing: 6) {
                    Image(systemName: voiceState.lastCommandSuccess ? "checkmark.circle.fill" : "xmark.circle.fill")
                        .foregroundStyle(voiceState.lastCommandSuccess ? .green : .red)
                        .font(.caption)

                    Text(lastCmd)
                        .font(.caption)
                        .foregroundStyle(theme.onBackground.opacity(0.7))
                }
            }
        }
        .frame(minHeight: 80)
    }

    // MARK: - Recent Commands

    private var recentCommandsSection: some View {
        VStack(spacing: 8) {
            if !voiceState.recentCommands.isEmpty {
                HStack {
                    Text("Recent Commands")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(theme.primary)
                    Spacer()
                    Button("Clear") {
                        voiceState.recentCommands.removeAll()
                    }
                    .font(.caption2)
                    .foregroundStyle(theme.onBackground.opacity(0.5))
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(voiceState.recentCommands.prefix(10), id: \.timestamp) { cmd in
                            HStack(spacing: 4) {
                                Circle()
                                    .fill(cmd.success ? Color.green : Color.red)
                                    .frame(width: 6, height: 6)
                                Text(cmd.text)
                                    .font(.caption2)
                                    .lineLimit(1)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(
                                Capsule()
                                    .fill(theme.surfaceVariant.opacity(0.8))
                            )
                        }
                    }
                }
                .frame(height: 36)
            }

            // Quick info bar
            HStack(spacing: 16) {
                Label(
                    UserDefaults.standard.string(forKey: "voice_locale") ?? "en-US",
                    systemImage: "globe"
                )
                .font(.caption2)
                .foregroundStyle(theme.onBackground.opacity(0.5))

                if speechManager.supportsOnDevice {
                    Label("On-Device", systemImage: "iphone")
                        .font(.caption2)
                        .foregroundStyle(theme.onBackground.opacity(0.5))
                }

                Spacer()
            }
            .padding(.top, 4)
        }
    }
}

#Preview {
    NavigationStack {
        VoiceAvanueView()
            .environmentObject(AppState())
    }
    .preferredColorScheme(.dark)
}
