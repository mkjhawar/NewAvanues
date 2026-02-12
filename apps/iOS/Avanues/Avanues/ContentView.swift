import SwiftUI

/// Root navigation view for the Avanues iOS app.
///
/// Routes between app modes:
/// - Browser: Main voice-controlled web browser
/// - Settings: Theme, voice, sync configuration
/// - About: Credits and version info
struct ContentView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        NavigationStack {
            Group {
                switch appState.currentMode {
                case .hub, .browser:
                    BrowserPlaceholderView()
                case .settings:
                    SettingsPlaceholderView()
                case .about:
                    AboutView()
                case .vosSyncManagement:
                    VosSyncPlaceholderView()
                }
            }
            .toolbar {
                ToolbarItem(placement: .bottomBar) {
                    HStack {
                        Button(action: { appState.currentMode = .browser }) {
                            Label("Browser", systemImage: "globe")
                        }
                        .tint(appState.currentMode == .browser ? .accentColor : .secondary)

                        Spacer()

                        Button(action: { appState.currentMode = .settings }) {
                            Label("Settings", systemImage: "gear")
                        }
                        .tint(appState.currentMode == .settings ? .accentColor : .secondary)

                        Spacer()

                        Button(action: { appState.currentMode = .about }) {
                            Label("About", systemImage: "info.circle")
                        }
                        .tint(appState.currentMode == .about ? .accentColor : .secondary)
                    }
                }
            }
        }
    }
}

// MARK: - Placeholder Views (replaced in Phase 1-6)

/// Placeholder for the browser view (Phase 1).
struct BrowserPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "globe")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("Avanues Browser")
                .font(.title)
            Text("Voice-controlled web browsing")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text("Phase 1: WKWebView + DOMScraperBridge coming next")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .navigationTitle("Avanues")
    }
}

/// Placeholder for settings view (Phase 6).
struct SettingsPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "gear")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("Settings")
                .font(.title)
            Text("Theme, voice locale, sync configuration")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .navigationTitle("Settings")
    }
}

/// Placeholder for VOS sync management (Phase 6).
struct VosSyncPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "arrow.triangle.2.circlepath")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("VOS Sync")
                .font(.title)
        }
        .navigationTitle("VOS Sync")
    }
}

/// About screen with credits.
struct AboutView: View {
    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "waveform.and.mic")
                .font(.system(size: 64))
                .foregroundStyle(.blue)

            Text("Avanues")
                .font(.largeTitle.bold())

            Text("Voice-Controlled Web Browser")
                .font(.headline)
                .foregroundStyle(.secondary)

            Text("Version 1.0.0")
                .font(.subheadline)
                .foregroundStyle(.tertiary)

            Divider()

            VStack(spacing: 8) {
                Text("VoiceOS\u{00AE} Avanues EcoSystem")
                    .font(.footnote)
                Text("Designed and Created in California with Love.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            Spacer()
        }
        .padding(.top, 60)
        .navigationTitle("About")
    }
}

#Preview {
    ContentView()
        .environmentObject(AppState())
}
