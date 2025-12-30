import Foundation
import SwiftUI
#if canImport(SnapshotTesting)
import SnapshotTesting
#endif

/**
 * Device configurations for iOS visual testing across the device matrix.
 *
 * Provides 4 standard device configurations:
 * - iPhone SE (Small phone)
 * - iPhone 14 (Standard phone)
 * - iPhone 14 Pro Max (Large phone)
 * - iPad Pro 12.9" (Tablet)
 *
 * @since 1.0.0 (iOS Visual Testing Framework)
 */
enum DeviceConfigurations {

    // MARK: - iPhone Configurations

    /**
     * iPhone SE (3rd generation) - Compact phone configuration.
     *
     * Resolution: 375x667 points (@2x = 750x1334)
     * Screen Size: 4.7"
     * Use case: Small phones, compact layouts
     */
    static let iPhoneSE = ViewImageConfig.iPhoneSe

    /**
     * iPhone 14 - Standard iPhone configuration.
     *
     * Resolution: 390x844 points (@3x = 1170x2532)
     * Screen Size: 6.1"
     * Use case: Most common iPhone size
     */
    static let iPhone14 = ViewImageConfig.iPhone13

    /**
     * iPhone 14 Pro Max - Large iPhone configuration.
     *
     * Resolution: 430x932 points (@3x = 1290x2796)
     * Screen Size: 6.7"
     * Use case: Large phones, max content display
     */
    static let iPhone14ProMax = ViewImageConfig.iPhone13ProMax

    // MARK: - iPad Configurations

    /**
     * iPad Pro 12.9" - Large screen tablet configuration.
     *
     * Resolution: 1024x1366 points (@2x = 2048x2732)
     * Screen Size: 12.9"
     * Use case: Tablet layouts, landscape orientation
     */
    static let iPadPro12_9 = ViewImageConfig.iPadPro12_9

    // MARK: - Device Matrix

    /**
     * All device configurations for matrix testing.
     */
    static let allDevices: [ViewImageConfig] = [
        iPhoneSE,
        iPhone14,
        iPhone14ProMax,
        iPadPro12_9
    ]

    /**
     * Device names for labeling screenshots.
     */
    static let deviceNames: [ViewImageConfig: String] = [
        iPhoneSE: "iPhoneSE",
        iPhone14: "iPhone14",
        iPhone14ProMax: "iPhone14ProMax",
        iPadPro12_9: "iPadPro12_9"
    ]

    /**
     * Gets a friendly name for a device configuration.
     */
    static func name(for config: ViewImageConfig) -> String {
        return deviceNames[config] ?? "UnknownDevice"
    }

    // MARK: - Orientation Variants

    /**
     * Landscape variant of a device configuration.
     */
    static func landscape(_ config: ViewImageConfig) -> ViewImageConfig {
        return ViewImageConfig(
            safeArea: config.safeArea,
            size: CGSize(width: config.size!.height, height: config.size!.width),
            traits: UITraitCollection(traitsFrom: [
                config.traits,
                UITraitCollection(horizontalSizeClass: .regular)
            ])
        )
    }

    // MARK: - Accessibility Variants

    /**
     * Accessibility configuration with large text (200% scale).
     */
    static func accessibility(_ config: ViewImageConfig) -> ViewImageConfig {
        return ViewImageConfig(
            safeArea: config.safeArea,
            size: config.size,
            traits: UITraitCollection(traitsFrom: [
                config.traits,
                UITraitCollection(preferredContentSizeCategory: .accessibilityExtraExtraExtraLarge)
            ])
        )
    }

    /**
     * Dark mode variant of a device configuration.
     */
    static func darkMode(_ config: ViewImageConfig) -> ViewImageConfig {
        return ViewImageConfig(
            safeArea: config.safeArea,
            size: config.size,
            traits: UITraitCollection(traitsFrom: [
                config.traits,
                UITraitCollection(userInterfaceStyle: .dark)
            ])
        )
    }

    /**
     * High contrast mode variant.
     */
    static func highContrast(_ config: ViewImageConfig) -> ViewImageConfig {
        return ViewImageConfig(
            safeArea: config.safeArea,
            size: config.size,
            traits: UITraitCollection(traitsFrom: [
                config.traits,
                UITraitCollection(accessibilityContrast: .high)
            ])
        )
    }
}

// MARK: - Snapshot Testing Helpers

extension ViewImageConfig: Hashable {
    public func hash(into hasher: inout Hasher) {
        hasher.combine(size?.width ?? 0)
        hasher.combine(size?.height ?? 0)
        hasher.combine(safeArea.top)
        hasher.combine(safeArea.bottom)
    }

    public static func == (lhs: ViewImageConfig, rhs: ViewImageConfig) -> Bool {
        return lhs.size == rhs.size &&
               lhs.safeArea == rhs.safeArea
    }
}
