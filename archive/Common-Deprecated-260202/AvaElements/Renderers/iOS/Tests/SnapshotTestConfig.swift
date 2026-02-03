import Foundation
import SwiftUI
import XCTest
#if canImport(SnapshotTesting)
import SnapshotTesting
#endif

/**
 * Shared snapshot testing configuration for all visual tests.
 *
 * Provides centralized configuration for:
 * - Device matrix (4 devices)
 * - Theme modes (light/dark)
 * - Precision settings
 * - Snapshot storage paths
 *
 * @since 1.0.0 (iOS Visual Testing Framework)
 */
enum SnapshotTestConfig {

    // MARK: - Precision Settings

    /**
     * Default pixel precision for snapshot comparisons.
     * Allows 0.1% pixel difference for anti-aliasing and rendering variations.
     */
    static let defaultPrecision: Float = 0.999

    /**
     * Strict precision for critical UI elements.
     * Requires 99.9% pixel match.
     */
    static let strictPrecision: Float = 0.999

    /**
     * Relaxed precision for dynamic content (images, animations).
     * Allows 1% pixel difference.
     */
    static let relaxedPrecision: Float = 0.99

    // MARK: - Snapshot Strategies

    /**
     * Creates a snapshot strategy for light mode.
     */
    static func lightMode(
        device: ViewImageConfig = DeviceConfigurations.iPhone14,
        precision: Float = defaultPrecision
    ) -> Snapshotting<some View, UIImage> {
        return .image(
            precision: precision,
            perceptualPrecision: precision,
            layout: .device(config: device),
            traits: UITraitCollection(userInterfaceStyle: .light)
        )
    }

    /**
     * Creates a snapshot strategy for dark mode.
     */
    static func darkMode(
        device: ViewImageConfig = DeviceConfigurations.iPhone14,
        precision: Float = defaultPrecision
    ) -> Snapshotting<some View, UIImage> {
        return .image(
            precision: precision,
            perceptualPrecision: precision,
            layout: .device(config: device),
            traits: UITraitCollection(userInterfaceStyle: .dark)
        )
    }

    /**
     * Creates a snapshot strategy for accessibility mode (large text).
     */
    static func accessibility(
        device: ViewImageConfig = DeviceConfigurations.iPhone14,
        precision: Float = relaxedPrecision
    ) -> Snapshotting<some View, UIImage> {
        return .image(
            precision: precision,
            perceptualPrecision: precision,
            layout: .device(config: DeviceConfigurations.accessibility(device)),
            traits: UITraitCollection(preferredContentSizeCategory: .accessibilityExtraExtraExtraLarge)
        )
    }

    /**
     * Creates a snapshot strategy for high contrast mode.
     */
    static func highContrast(
        device: ViewImageConfig = DeviceConfigurations.iPhone14,
        precision: Float = defaultPrecision
    ) -> Snapshotting<some View, UIImage> {
        return .image(
            precision: precision,
            perceptualPrecision: precision,
            layout: .device(config: DeviceConfigurations.highContrast(device)),
            traits: UITraitCollection(accessibilityContrast: .high)
        )
    }

    /**
     * Creates a snapshot strategy for landscape orientation.
     */
    static func landscape(
        device: ViewImageConfig = DeviceConfigurations.iPhone14,
        precision: Float = defaultPrecision
    ) -> Snapshotting<some View, UIImage> {
        return .image(
            precision: precision,
            perceptualPrecision: precision,
            layout: .device(config: DeviceConfigurations.landscape(device))
        )
    }

    // MARK: - Multi-Device Testing

    /**
     * Tests a view across all devices in the device matrix.
     */
    static func assertSnapshotAllDevices<V: View>(
        _ view: V,
        name: String,
        file: StaticString = #file,
        testName: String = #function,
        line: UInt = #line
    ) {
        for device in DeviceConfigurations.allDevices {
            let deviceName = DeviceConfigurations.name(for: device)

            // Light mode
            assertSnapshot(
                matching: view,
                as: .image(
                    precision: defaultPrecision,
                    perceptualPrecision: defaultPrecision,
                    layout: .device(config: device),
                    traits: UITraitCollection(userInterfaceStyle: .light)
                ),
                named: "\(name)_\(deviceName)_light",
                file: file,
                testName: testName,
                line: line
            )

            // Dark mode
            assertSnapshot(
                matching: view,
                as: .image(
                    precision: defaultPrecision,
                    perceptualPrecision: defaultPrecision,
                    layout: .device(config: device),
                    traits: UITraitCollection(userInterfaceStyle: .dark)
                ),
                named: "\(name)_\(deviceName)_dark",
                file: file,
                testName: testName,
                line: line
            )
        }
    }

    /**
     * Tests a view in both light and dark modes on a single device.
     */
    static func assertSnapshotLightDark<V: View>(
        _ view: V,
        name: String,
        device: ViewImageConfig = DeviceConfigurations.iPhone14,
        file: StaticString = #file,
        testName: String = #function,
        line: UInt = #line
    ) {
        let deviceName = DeviceConfigurations.name(for: device)

        // Light mode
        assertSnapshot(
            matching: view,
            as: lightMode(device: device),
            named: "\(name)_\(deviceName)_light",
            file: file,
            testName: testName,
            line: line
        )

        // Dark mode
        assertSnapshot(
            matching: view,
            as: darkMode(device: device),
            named: "\(name)_\(deviceName)_dark",
            file: file,
            testName: testName,
            line: line
        )
    }

    /**
     * Tests a view in all accessibility modes.
     */
    static func assertSnapshotAccessibility<V: View>(
        _ view: V,
        name: String,
        device: ViewImageConfig = DeviceConfigurations.iPhone14,
        file: StaticString = #file,
        testName: String = #function,
        line: UInt = #line
    ) {
        let deviceName = DeviceConfigurations.name(for: device)

        // Standard
        assertSnapshot(
            matching: view,
            as: lightMode(device: device),
            named: "\(name)_\(deviceName)_standard",
            file: file,
            testName: testName,
            line: line
        )

        // Large text
        assertSnapshot(
            matching: view,
            as: accessibility(device: device),
            named: "\(name)_\(deviceName)_largeText",
            file: file,
            testName: testName,
            line: line
        )

        // High contrast
        assertSnapshot(
            matching: view,
            as: highContrast(device: device),
            named: "\(name)_\(deviceName)_highContrast",
            file: file,
            testName: testName,
            line: line
        )
    }
}

// MARK: - XCTestCase Extension

extension XCTestCase {

    /**
     * Records all snapshot reference images.
     * Set this to true when first creating tests or updating UI.
     */
    var isRecording: Bool {
        return false // Change to true to record new snapshots
    }

    /**
     * Configure snapshot testing for the test case.
     * Call this in setUp().
     */
    func configureSnapshotTesting() {
        #if canImport(SnapshotTesting)
        SnapshotTesting.isRecording = isRecording
        #endif
    }
}
