import SwiftUI

/// Appearance mode matching AvanueUI v5.1 AppearanceMode enum.
enum AppearanceMode: String, CaseIterable {
    case light = "Light"
    case dark = "Dark"
    case auto = "Auto"

    var colorScheme: ColorScheme? {
        switch self {
        case .light: return .light
        case .dark: return .dark
        case .auto: return nil // Follow system
        }
    }
}

/// Color palette matching AvanueUI v5.1 AvanueColorPalette enum.
enum ColorPalette: String, CaseIterable {
    case hydra = "HYDRA"   // Royal sapphire (default)
    case sol = "SOL"       // Sun/gold
    case luna = "LUNA"     // Moon/silver
    case terra = "TERRA"   // Earth/green
}

/// Material style matching AvanueUI v5.1 MaterialMode enum.
enum MaterialStyle: String, CaseIterable {
    case water = "Water"           // Default
    case glass = "Glass"
    case cupertino = "Cupertino"
    case mountainView = "MountainView"
}

/// App-wide observable state.
///
/// Manages theme preferences, voice locale, and navigation mode.
/// Persists to UserDefaults (mirrors DataStore keys from Android).
@MainActor
class AppState: ObservableObject {
    // MARK: - Theme (v5.1 three-axis system)
    @AppStorage("theme_palette") var palette: ColorPalette = .hydra
    @AppStorage("theme_style") var materialStyle: MaterialStyle = .water
    @AppStorage("theme_appearance") var appearanceMode: AppearanceMode = .auto

    // MARK: - Voice
    @AppStorage("voice_locale") var voiceLocale: String = "en-US"

    // MARK: - Navigation
    @Published var currentMode: AvanueMode = .hub
}
