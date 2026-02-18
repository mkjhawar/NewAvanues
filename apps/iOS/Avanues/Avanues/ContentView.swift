import SwiftUI

/// Root navigation view for the Avanues iOS app.
///
/// Routes between app modes using NavigationStack.
/// Hub is the default entry point; sub-apps are reached via the grid dashboard.
struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme

    private var isDark: Bool { colorScheme == .dark }
    private var theme: AvanueColors {
        AvanueThemeBridge.colors(for: appState.palette, isDark: isDark)
    }

    var body: some View {
        NavigationStack {
            Group {
                switch appState.currentMode {
                case .hub:
                    HubView()
                case .browser:
                    BrowserView()
                case .voice:
                    VoiceAvanueView()
                case .commands:
                    VoiceCommandsView()
                case .settings:
                    SettingsView()
                case .about:
                    AboutView()
                case .vosSyncManagement:
                    PlaceholderView(mode: .vosSyncManagement)
                case .developerConsole:
                    PlaceholderView(mode: .developerConsole)
                case .developerSettings:
                    PlaceholderView(mode: .developerSettings)
                }
            }
            .toolbar {
                // Back to hub button (shown when not on hub)
                if appState.currentMode != .hub {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(action: { appState.currentMode = .hub }) {
                            Label("Hub", systemImage: "square.grid.2x2")
                        }
                        .tint(theme.primary)
                    }
                }

                ToolbarItem(placement: .bottomBar) {
                    HStack {
                        Button(action: { appState.currentMode = .hub }) {
                            Label("Hub", systemImage: "square.grid.2x2")
                        }
                        .tint(appState.currentMode == .hub ? theme.primary : theme.onSurface.opacity(0.5))

                        Spacer()

                        Button(action: { appState.currentMode = .browser }) {
                            Label("Browser", systemImage: "globe")
                        }
                        .tint(appState.currentMode == .browser ? theme.primary : theme.onSurface.opacity(0.5))

                        Spacer()

                        Button(action: { appState.currentMode = .settings }) {
                            Label("Settings", systemImage: "gear")
                        }
                        .tint(appState.currentMode == .settings ? theme.primary : theme.onSurface.opacity(0.5))

                        Spacer()

                        Button(action: { appState.currentMode = .about }) {
                            Label("About", systemImage: "info.circle")
                        }
                        .tint(appState.currentMode == .about ? theme.primary : theme.onSurface.opacity(0.5))
                    }
                }
            }
        }
    }
}

// MARK: - Placeholder Views

/// Generic placeholder for modes not yet implemented.
struct PlaceholderView: View {
    let mode: AvanueMode

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: mode.icon)
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text(mode.label)
                .font(.title)
            Text("Coming soon")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .navigationTitle(mode.label)
    }
}

/// Placeholder for the browser view (replaced in Phase 3).
struct BrowserPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "globe")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("WebAvanue Browser")
                .font(.title)
            Text("Voice-controlled web browsing")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text("WKWebView + DOMScraperBridge — Phase 3")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .navigationTitle("WebAvanue")
    }
}

/// Placeholder for settings view (replaced in Phase 6).
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

/// About screen with credits — themed with AvanueUI colors.
struct AboutView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme

    private var theme: AvanueColors {
        AvanueThemeBridge.colors(for: appState.palette, isDark: colorScheme == .dark)
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [theme.background, theme.surface.opacity(0.6), theme.background],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "waveform.and.mic")
                    .font(.system(size: 64))
                    .foregroundStyle(theme.primary)

                Text("Avanues")
                    .font(.largeTitle.bold())
                    .foregroundStyle(theme.onBackground)

                Text("Voice-Controlled Web Browser")
                    .font(.headline)
                    .foregroundStyle(theme.onBackground.opacity(0.7))

                Text("Version 1.0.0")
                    .font(.subheadline)
                    .foregroundStyle(theme.onBackground.opacity(0.4))

                Divider()
                    .overlay(theme.outline.opacity(0.3))

                VStack(spacing: 8) {
                    Text("VoiceOS\u{00AE} Avanues EcoSystem")
                        .font(.footnote)
                        .foregroundStyle(theme.onBackground.opacity(0.5))
                    Text("Designed and Created in California with Love.")
                        .font(.footnote)
                        .foregroundStyle(theme.onBackground.opacity(0.4))
                }

                Spacer()
            }
            .padding(.top, 60)
        }
        .navigationTitle("About")
    }
}

#Preview {
    ContentView()
        .environmentObject(AppState())
        .preferredColorScheme(.dark)
}
