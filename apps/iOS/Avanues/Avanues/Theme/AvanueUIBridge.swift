import SwiftUI

/// Maps AvanueUI v5.1 design tokens to SwiftUI colors.
///
/// Mirrors the KMP AvanueColorScheme interface with all 4 palettes
/// (HYDRA, SOL, LUNA, TERRA) in both light and dark variants.
/// Source of truth: `Modules/AvanueUI/src/commonMain/.../theme/*.kt`
struct AvanueColors {
    let primary: Color
    let onPrimary: Color
    let secondary: Color
    let onSecondary: Color
    let surface: Color
    let onSurface: Color
    let background: Color
    let onBackground: Color
    let primaryContainer: Color
    let onPrimaryContainer: Color
    let error: Color
    let onError: Color
    let outline: Color
    let surfaceVariant: Color
}

/// Resolves the correct AvanueColors for a palette + appearance combination.
enum AvanueThemeBridge {

    static func colors(
        for palette: ColorPalette,
        isDark: Bool
    ) -> AvanueColors {
        switch palette {
        case .hydra: return isDark ? hydraDark : hydraLight
        case .sol: return isDark ? solDark : solLight
        case .luna: return isDark ? lunaDark : lunaLight
        case .terra: return isDark ? terraDark : terraLight
        }
    }

    // MARK: - HYDRA (Royal Sapphire) — Default

    static let hydraDark = AvanueColors(
        primary: Color(hex: 0x1E40AF),
        onPrimary: .white,
        secondary: Color(hex: 0x8B5CF6),
        onSecondary: .white,
        surface: Color(hex: 0x0F172A),
        onSurface: Color(hex: 0xF1F5F9),
        background: Color(hex: 0x020617),
        onBackground: Color(hex: 0xF1F5F9),
        primaryContainer: Color(hex: 0x0C1A3D),
        onPrimaryContainer: Color(hex: 0xBFDBFE),
        error: Color(hex: 0xEF4444),
        onError: .white,
        outline: Color(hex: 0x334155),
        surfaceVariant: Color(hex: 0x1E293B)
    )

    static let hydraLight = AvanueColors(
        primary: Color(hex: 0x1E40AF),
        onPrimary: .white,
        secondary: Color(hex: 0x7C3AED),
        onSecondary: .white,
        surface: .white,
        onSurface: Color(hex: 0x0F172A),
        background: Color(hex: 0xF8FAFC),
        onBackground: Color(hex: 0x0F172A),
        primaryContainer: Color(hex: 0xDBEAFE),
        onPrimaryContainer: Color(hex: 0x1E3A8A),
        error: Color(hex: 0xDC2626),
        onError: .white,
        outline: Color(hex: 0xCBD5E1),
        surfaceVariant: Color(hex: 0xF1F5F9)
    )

    // MARK: - SOL (Amber Gold)

    static let solDark = AvanueColors(
        primary: Color(hex: 0xD97706),
        onPrimary: .white,
        secondary: Color(hex: 0xEF4444),
        onSecondary: .white,
        surface: Color(hex: 0x2D1B0E),
        onSurface: Color(hex: 0xF5E6D3),
        background: Color(hex: 0x1A0F05),
        onBackground: Color(hex: 0xF5E6D3),
        primaryContainer: Color(hex: 0x4A2C0A),
        onPrimaryContainer: Color(hex: 0xFDE68A),
        error: Color(hex: 0xEF4444),
        onError: .white,
        outline: Color(hex: 0x78350F),
        surfaceVariant: Color(hex: 0x3D2610)
    )

    static let solLight = AvanueColors(
        primary: Color(hex: 0xB45309),
        onPrimary: .white,
        secondary: Color(hex: 0xDC2626),
        onSecondary: .white,
        surface: .white,
        onSurface: Color(hex: 0x1C1917),
        background: Color(hex: 0xFFFBF0),
        onBackground: Color(hex: 0x1C1917),
        primaryContainer: Color(hex: 0xFEF3C7),
        onPrimaryContainer: Color(hex: 0x78350F),
        error: Color(hex: 0xDC2626),
        onError: .white,
        outline: Color(hex: 0xD6D3D1),
        surfaceVariant: Color(hex: 0xFEFCE8)
    )

    // MARK: - LUNA (Moonlit Silver)

    static let lunaDark = AvanueColors(
        primary: Color(hex: 0x818CF8),
        onPrimary: .black,
        secondary: Color(hex: 0x7C3AED),
        onSecondary: .white,
        surface: Color(hex: 0x1A1D2E),
        onSurface: Color(hex: 0xE2E8F0),
        background: Color(hex: 0x0C0F1A),
        onBackground: Color(hex: 0xE2E8F0),
        primaryContainer: Color(hex: 0x1E1B4B),
        onPrimaryContainer: Color(hex: 0xC7D2FE),
        error: Color(hex: 0xEF4444),
        onError: .white,
        outline: Color(hex: 0x4C1D95),
        surfaceVariant: Color(hex: 0x252840)
    )

    static let lunaLight = AvanueColors(
        primary: Color(hex: 0x6366F1),
        onPrimary: .white,
        secondary: Color(hex: 0x7C3AED),
        onSecondary: .white,
        surface: .white,
        onSurface: Color(hex: 0x1E1B4B),
        background: Color(hex: 0xF5F3FF),
        onBackground: Color(hex: 0x1E1B4B),
        primaryContainer: Color(hex: 0xE0E7FF),
        onPrimaryContainer: Color(hex: 0x312E81),
        error: Color(hex: 0xDC2626),
        onError: .white,
        outline: Color(hex: 0xC4B5FD),
        surfaceVariant: Color(hex: 0xEDE9FE)
    )

    // MARK: - TERRA (Forest Green)

    static let terraDark = AvanueColors(
        primary: Color(hex: 0x2D7D46),
        onPrimary: .white,
        secondary: Color(hex: 0xD97706),
        onSecondary: .white,
        surface: Color(hex: 0x1A2B1C),
        onSurface: Color(hex: 0xE8F0E4),
        background: Color(hex: 0x0F1A10),
        onBackground: Color(hex: 0xE8F0E4),
        primaryContainer: Color(hex: 0x0F3D1E),
        onPrimaryContainer: Color(hex: 0xBBF7D0),
        error: Color(hex: 0xEF4444),
        onError: .white,
        outline: Color(hex: 0x14532D),
        surfaceVariant: Color(hex: 0x233826)
    )

    static let terraLight = AvanueColors(
        primary: Color(hex: 0x15803D),
        onPrimary: .white,
        secondary: Color(hex: 0xB45309),
        onSecondary: .white,
        surface: .white,
        onSurface: Color(hex: 0x14532D),
        background: Color(hex: 0xF0FDF4),
        onBackground: Color(hex: 0x14532D),
        primaryContainer: Color(hex: 0xDCFCE7),
        onPrimaryContainer: Color(hex: 0x14532D),
        error: Color(hex: 0xDC2626),
        onError: .white,
        outline: Color(hex: 0x86EFAC),
        surfaceVariant: Color(hex: 0xECFDF5)
    )
}

// MARK: - Color Hex Initializer

extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: alpha
        )
    }
}

// MARK: - Environment Key

private struct AvanueColorsKey: EnvironmentKey {
    static let defaultValue: AvanueColors = AvanueThemeBridge.hydraDark
}

extension EnvironmentValues {
    var avanueColors: AvanueColors {
        get { self[AvanueColorsKey.self] }
        set { self[AvanueColorsKey.self] = newValue }
    }
}

extension View {
    /// Injects AvanueUI theme colors into the SwiftUI environment.
    func avanueTheme(palette: ColorPalette, isDark: Bool) -> some View {
        environment(\.avanueColors, AvanueThemeBridge.colors(for: palette, isDark: isDark))
    }
}
