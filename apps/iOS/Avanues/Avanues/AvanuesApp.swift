import SwiftUI
import AvanuesShared

/// Main entry point for the Avanues iOS app.
///
/// Avanues is a voice-controlled web browser powered by VoiceOSCore.
/// This app provides:
/// - In-app WKWebView browser with voice command overlays
/// - Safari Web Extension for voice control in Safari
/// - NLU-powered semantic voice commands
/// - Multi-locale support (en, es, fr, de, hi)
@main
struct AvanuesApp: App {

    init() {
        // Initialize Koin DI from KMP shared module
        KoinHelper.shared.doStartKoin()
    }

    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .preferredColorScheme(appState.appearanceMode.colorScheme)
        }
    }
}
