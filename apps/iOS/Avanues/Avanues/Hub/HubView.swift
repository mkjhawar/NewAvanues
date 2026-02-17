import SwiftUI

/// Hub dashboard — the main entry point for the Avanues iOS app.
///
/// Displays a grid of app icons (WebAvanue, VoiceAvanue, etc.)
/// with SpatialVoice design language aesthetics.
/// Mirrors the Android HubDashboardScreen composable.
struct HubView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var isDark: Bool { colorScheme == .dark }
    private var theme: AvanueColors {
        AvanueThemeBridge.colors(for: appState.palette, isDark: isDark)
    }

    private var columns: [GridItem] {
        let count = sizeClass == .regular ? 3 : 2
        return Array(repeating: GridItem(.flexible(), spacing: 20), count: count)
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
                VStack(spacing: 32) {
                    headerSection
                    appGrid
                    Spacer(minLength: 40)
                    footerSection
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
            }
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(spacing: 8) {
            Image(systemName: "waveform.and.mic")
                .font(.system(size: 44, weight: .light))
                .foregroundStyle(theme.primary)

            Text("Avanues")
                .font(.largeTitle.bold())
                .foregroundStyle(theme.onBackground)

            Text("Voice-Controlled EcoSystem")
                .font(.subheadline)
                .foregroundStyle(theme.onBackground.opacity(0.6))

            Text("v1.0.0")
                .font(.caption2)
                .foregroundStyle(theme.onBackground.opacity(0.3))
        }
        .padding(.top, 12)
    }

    // MARK: - App Grid

    private var appGrid: some View {
        LazyVGrid(columns: columns, spacing: 20) {
            ForEach(AvanueMode.hubItems) { mode in
                HubAppIcon(
                    mode: mode,
                    theme: theme,
                    onTap: {
                        if mode.isAvailable {
                            appState.currentMode = mode
                        }
                    }
                )
            }
        }
    }

    // MARK: - Footer

    private var footerSection: some View {
        VStack(spacing: 4) {
            Text("VoiceOS\u{00AE} Avanues EcoSystem")
                .font(.caption2)
                .foregroundStyle(theme.onBackground.opacity(0.4))
            Text("Designed and Created in California with Love.")
                .font(.caption2)
                .foregroundStyle(theme.onBackground.opacity(0.3))
        }
        .padding(.bottom, 20)
    }
}

// MARK: - Hub App Icon

/// A single app icon tile for the hub dashboard grid.
struct HubAppIcon: View {
    let mode: AvanueMode
    let theme: AvanueColors
    let onTap: () -> Void

    @State private var isPressed = false

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 20)
                        .fill(theme.surfaceVariant.opacity(mode.isAvailable ? 1.0 : 0.5))
                        .frame(width: 80, height: 80)

                    Image(systemName: mode.icon)
                        .font(.system(size: 32, weight: .medium))
                        .foregroundStyle(
                            mode.isAvailable ? theme.primary : theme.onSurface.opacity(0.3)
                        )
                }

                Text(mode.label)
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundStyle(
                        mode.isAvailable ? theme.onBackground : theme.onBackground.opacity(0.4)
                    )
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
        .scaleEffect(isPressed ? 0.95 : 1.0)
        .animation(.easeInOut(duration: 0.15), value: isPressed)
        .opacity(mode.isAvailable ? 1.0 : 0.6)
        .accessibilityLabel(mode.label)
        .accessibilityHint(mode.isAvailable ? "Opens \(mode.label)" : "Coming soon")
    }
}

#Preview {
    NavigationStack {
        HubView()
            .environmentObject(AppState())
    }
    .preferredColorScheme(.dark)
}
