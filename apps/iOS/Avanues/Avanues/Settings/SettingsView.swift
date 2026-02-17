import SwiftUI

/// Settings screen mirroring Android UnifiedSettingsScreen.
///
/// Organized in 4 sections: Theme, Voice, Browser, Developer.
/// Uses SpatialVoice design language with AvanueUI theme bridge.
struct SettingsView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme

    // MARK: - Voice Settings
    @AppStorage("voice_locale") private var voiceLocale: String = "en-US"
    @AppStorage("voice_confidence_threshold") private var confidenceThreshold: Double = 0.6
    @AppStorage("voice_on_device") private var preferOnDevice: Bool = true
    @AppStorage("voice_continuous") private var continuousListening: Bool = false

    // MARK: - Browser Settings
    @AppStorage("search_engine") private var searchEngine: String = "Google"
    @AppStorage("browser_content_blocking") private var contentBlocking: Bool = true
    @AppStorage("browser_javascript") private var javascriptEnabled: Bool = true
    @AppStorage("browser_desktop_mode") private var desktopMode: Bool = false

    // MARK: - Developer Settings
    @AppStorage("dev_element_overlay") private var showElementOverlay: Bool = false
    @State private var devTapCount: Int = 0
    @State private var showDevSettings: Bool = false

    private var isDark: Bool { colorScheme == .dark }
    private var theme: AvanueColors {
        AvanueThemeBridge.colors(for: appState.palette, isDark: isDark)
    }

    var body: some View {
        ZStack {
            // SpatialVoice gradient background
            LinearGradient(
                colors: [
                    theme.background,
                    theme.surface.opacity(0.6),
                    theme.background
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    themeSection
                    voiceSection
                    browserSection

                    if showDevSettings {
                        developerSection
                    }

                    versionSection

                    Spacer(minLength: 40)
                    footerSection
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
            }
        }
        .navigationTitle("Settings")
    }

    // MARK: - Theme Section

    private var themeSection: some View {
        VStack(spacing: 16) {
            SectionHeader(title: "Theme", icon: "paintbrush.fill", theme: theme)

            VStack(spacing: 12) {
                // Color Palette
                VStack(alignment: .leading, spacing: 8) {
                    Text("Color Palette")
                        .font(.subheadline)
                        .foregroundStyle(theme.onSurface)

                    Picker("Color Palette", selection: $appState.palette) {
                        ForEach(ColorPalette.allCases, id: \.self) { palette in
                            HStack {
                                Circle()
                                    .fill(palettePreviewColor(for: palette))
                                    .frame(width: 20, height: 20)
                                Text(paletteLabel(for: palette))
                            }
                            .tag(palette)
                        }
                    }
                    .pickerStyle(.menu)
                    .tint(theme.primary)
                }

                Divider().background(theme.outline.opacity(0.3))

                // Material Style
                VStack(alignment: .leading, spacing: 8) {
                    Text("Material Style")
                        .font(.subheadline)
                        .foregroundStyle(theme.onSurface)

                    Picker("Material Style", selection: $appState.materialStyle) {
                        ForEach(MaterialStyle.allCases, id: \.self) { style in
                            Text(style.rawValue).tag(style)
                        }
                    }
                    .pickerStyle(.segmented)
                    .tint(theme.primary)
                }

                Divider().background(theme.outline.opacity(0.3))

                // Appearance
                VStack(alignment: .leading, spacing: 8) {
                    Text("Appearance")
                        .font(.subheadline)
                        .foregroundStyle(theme.onSurface)

                    Picker("Appearance", selection: $appState.appearanceMode) {
                        ForEach(AppearanceMode.allCases, id: \.self) { mode in
                            Text(mode.rawValue).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)
                    .tint(theme.primary)
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(theme.surface.opacity(0.8))
            )
        }
    }

    // MARK: - Voice Section

    private var voiceSection: some View {
        VStack(spacing: 16) {
            SectionHeader(title: "Voice", icon: "waveform", theme: theme)

            VStack(spacing: 12) {
                // Voice Language
                VStack(alignment: .leading, spacing: 8) {
                    Text("Voice Language")
                        .font(.subheadline)
                        .foregroundStyle(theme.onSurface)

                    Picker("Voice Language", selection: $voiceLocale) {
                        Text("English (US)").tag("en-US")
                        Text("Spanish (ES)").tag("es-ES")
                        Text("French (FR)").tag("fr-FR")
                        Text("German (DE)").tag("de-DE")
                        Text("Hindi (IN)").tag("hi-IN")
                    }
                    .pickerStyle(.menu)
                    .tint(theme.primary)
                }

                Divider().background(theme.outline.opacity(0.3))

                // Confidence Threshold
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("Confidence Threshold")
                            .font(.subheadline)
                            .foregroundStyle(theme.onSurface)
                        Spacer()
                        Text(String(format: "%.1f", confidenceThreshold))
                            .font(.caption)
                            .foregroundStyle(theme.primary)
                            .fontWeight(.medium)
                    }

                    Slider(value: $confidenceThreshold, in: 0.3...0.9, step: 0.1)
                        .tint(theme.primary)
                }

                Divider().background(theme.outline.opacity(0.3))

                // On-Device Recognition
                Toggle(isOn: $preferOnDevice) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("On-Device Recognition")
                            .font(.subheadline)
                            .foregroundStyle(theme.onSurface)
                        Text("Prefer local processing when available")
                            .font(.caption)
                            .foregroundStyle(theme.onSurface.opacity(0.6))
                    }
                }
                .tint(theme.primary)

                Divider().background(theme.outline.opacity(0.3))

                // Continuous Listening
                Toggle(isOn: $continuousListening) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Continuous Listening")
                            .font(.subheadline)
                            .foregroundStyle(theme.onSurface)
                        Text("Auto-restart after pause")
                            .font(.caption)
                            .foregroundStyle(theme.onSurface.opacity(0.6))
                    }
                }
                .tint(theme.primary)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(theme.surface.opacity(0.8))
            )
        }
    }

    // MARK: - Browser Section

    private var browserSection: some View {
        VStack(spacing: 16) {
            SectionHeader(title: "Browser", icon: "globe", theme: theme)

            VStack(spacing: 12) {
                // Search Engine
                VStack(alignment: .leading, spacing: 8) {
                    Text("Search Engine")
                        .font(.subheadline)
                        .foregroundStyle(theme.onSurface)

                    Picker("Search Engine", selection: $searchEngine) {
                        Text("Google").tag("Google")
                        Text("DuckDuckGo").tag("DuckDuckGo")
                        Text("Bing").tag("Bing")
                        Text("Brave").tag("Brave")
                    }
                    .pickerStyle(.menu)
                    .tint(theme.primary)
                }

                Divider().background(theme.outline.opacity(0.3))

                // Content Blocking
                Toggle(isOn: $contentBlocking) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Content Blocking")
                            .font(.subheadline)
                            .foregroundStyle(theme.onSurface)
                        Text("Block ads and trackers")
                            .font(.caption)
                            .foregroundStyle(theme.onSurface.opacity(0.6))
                    }
                }
                .tint(theme.primary)

                Divider().background(theme.outline.opacity(0.3))

                // JavaScript Enabled
                Toggle(isOn: $javascriptEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("JavaScript Enabled")
                            .font(.subheadline)
                            .foregroundStyle(theme.onSurface)
                        Text("Required for most modern websites")
                            .font(.caption)
                            .foregroundStyle(theme.onSurface.opacity(0.6))
                    }
                }
                .tint(theme.primary)

                Divider().background(theme.outline.opacity(0.3))

                // Desktop Mode
                Toggle(isOn: $desktopMode) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Desktop Mode")
                            .font(.subheadline)
                            .foregroundStyle(theme.onSurface)
                        Text("Request desktop site version")
                            .font(.caption)
                            .foregroundStyle(theme.onSurface.opacity(0.6))
                    }
                }
                .tint(theme.primary)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(theme.surface.opacity(0.8))
            )
        }
    }

    // MARK: - Developer Section

    private var developerSection: some View {
        VStack(spacing: 16) {
            SectionHeader(title: "Developer", icon: "hammer.fill", theme: theme)

            VStack(spacing: 12) {
                // Element Overlay
                Toggle(isOn: $showElementOverlay) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Show Element Overlay")
                            .font(.subheadline)
                            .foregroundStyle(theme.onSurface)
                        Text("Display interactive element borders")
                            .font(.caption)
                            .foregroundStyle(theme.onSurface.opacity(0.6))
                    }
                }
                .tint(theme.primary)

                Divider().background(theme.outline.opacity(0.3))

                // VOS Sync (placeholder)
                NavigationLink(destination: PlaceholderView(mode: .vosSyncManagement)) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("VOS Sync")
                                .font(.subheadline)
                                .foregroundStyle(theme.onSurface)
                            Text("SFTP-based command synchronization")
                                .font(.caption)
                                .foregroundStyle(theme.onSurface.opacity(0.6))
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(theme.onSurface.opacity(0.5))
                    }
                }

                Divider().background(theme.outline.opacity(0.3))

                // Developer Console (placeholder)
                NavigationLink(destination: PlaceholderView(mode: .developerConsole)) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Developer Console")
                                .font(.subheadline)
                                .foregroundStyle(theme.onSurface)
                            Text("Debug logs and diagnostics")
                                .font(.caption)
                                .foregroundStyle(theme.onSurface.opacity(0.6))
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(theme.onSurface.opacity(0.5))
                    }
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(theme.surface.opacity(0.8))
            )
        }
    }

    // MARK: - Version Section

    private var versionSection: some View {
        Button(action: handleVersionTap) {
            HStack {
                Text("Version")
                    .font(.subheadline)
                    .foregroundStyle(theme.onSurface)
                Spacer()
                Text("1.0.0")
                    .font(.subheadline)
                    .foregroundStyle(theme.onSurface.opacity(0.6))
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(theme.surface.opacity(0.8))
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Footer

    private var footerSection: some View {
        VStack(spacing: 4) {
            Text("VoiceOS® Avanues EcoSystem")
                .font(.caption2)
                .foregroundStyle(theme.onBackground.opacity(0.4))
            Text("Designed and Created in California with Love.")
                .font(.caption2)
                .foregroundStyle(theme.onBackground.opacity(0.3))
        }
        .padding(.bottom, 20)
    }

    // MARK: - Helpers

    private func paletteLabel(for palette: ColorPalette) -> String {
        switch palette {
        case .hydra: return "Sapphire"
        case .sol: return "Gold"
        case .luna: return "Silver"
        case .terra: return "Green"
        }
    }

    private func palettePreviewColor(for palette: ColorPalette) -> Color {
        let colors = AvanueThemeBridge.colors(for: palette, isDark: isDark)
        return colors.primary
    }

    private func handleVersionTap() {
        devTapCount += 1

        if devTapCount >= 7 {
            showDevSettings = true
            devTapCount = 0
        }

        // Reset counter after 2 seconds if not completed
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            if devTapCount < 7 {
                devTapCount = 0
            }
        }
    }
}

// MARK: - Section Header

struct SectionHeader: View {
    let title: String
    let icon: String
    let theme: AvanueColors

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(theme.primary)

            Text(title.uppercased())
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundStyle(theme.primary)

            Spacer()
        }
    }
}

#Preview("Light Mode") {
    NavigationStack {
        SettingsView()
            .environmentObject(AppState())
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NavigationStack {
        SettingsView()
            .environmentObject(AppState())
    }
    .preferredColorScheme(.dark)
}
