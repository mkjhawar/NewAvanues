import Foundation

/// Navigation modes matching Android AvanueMode enum.
///
/// Each case maps to a distinct app section navigated from the Hub dashboard.
/// Mirrored from `apps/avanues/.../MainActivity.kt`.
enum AvanueMode: String, Hashable, CaseIterable, Identifiable {
    case hub = "hub"
    case browser = "browser"
    case voice = "voice_home"
    case commands = "commands"
    case settings = "settings"
    case about = "about"
    case vosSyncManagement = "vos_sync"
    case developerConsole = "developer_console"
    case developerSettings = "developer_settings"

    var id: String { rawValue }

    /// Human-readable label for display.
    var label: String {
        switch self {
        case .hub: return "Avanues"
        case .browser: return "WebAvanue"
        case .voice: return "VoiceAvanue"
        case .commands: return "Voice Commands"
        case .settings: return "Settings"
        case .about: return "About Avanues"
        case .vosSyncManagement: return "VOS Sync"
        case .developerConsole: return "Developer Console"
        case .developerSettings: return "Developer Settings"
        }
    }

    /// SF Symbol icon name for the hub dashboard.
    var icon: String {
        switch self {
        case .hub: return "square.grid.2x2"
        case .browser: return "globe"
        case .voice: return "mic.fill"
        case .commands: return "list.bullet.rectangle"
        case .settings: return "gear"
        case .about: return "info.circle"
        case .vosSyncManagement: return "arrow.triangle.2.circlepath"
        case .developerConsole: return "terminal"
        case .developerSettings: return "wrench.and.screwdriver"
        }
    }

    /// Modes displayed as icons on the Hub dashboard grid.
    static var hubItems: [AvanueMode] {
        [.browser, .voice, .commands, .vosSyncManagement, .settings, .about]
    }

    /// Whether this mode is available for navigation (vs. placeholder).
    var isAvailable: Bool {
        switch self {
        case .hub, .browser, .settings, .about:
            return true
        case .voice, .commands, .vosSyncManagement,
             .developerConsole, .developerSettings:
            return false // Placeholder until future phases
        }
    }
}
