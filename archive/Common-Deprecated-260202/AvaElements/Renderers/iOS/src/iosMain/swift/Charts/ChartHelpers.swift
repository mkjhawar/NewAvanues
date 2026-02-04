import SwiftUI
import Foundation

/// Chart utility functions for iOS renderer
///
/// Provides shared utilities used across all chart components:
/// - Color parsing from hex strings
/// - Data point calculations
/// - Coordinate transformations
/// - Animation helpers
///
/// **iOS Version:** 16.0+
/// **Thread Safety:** All functions are thread-safe and can be called from any thread
///
/// ## Usage Example
/// ```swift
/// // Parse color
/// let color = ChartHelpers.parseColor("#2196F3")
///
/// // Calculate bounds
/// let bounds = ChartHelpers.calculateChartBounds(data: dataPoints)
///
/// // Transform coordinates
/// let screenPoint = ChartHelpers.transformToScreenCoordinates(
///     dataPoint: point,
///     dataBounds: bounds,
///     screenSize: size
/// )
/// ```
public enum ChartHelpers {

    // MARK: - Color Parsing

    /// Parse hex color string to SwiftUI Color
    ///
    /// Supports formats:
    /// - 6-digit hex: `#RRGGBB` or `RRGGBB`
    /// - 8-digit hex with alpha: `#AARRGGBB` or `AARRGGBB`
    /// - 3-digit hex: `#RGB` (expands to `#RRGGBB`)
    ///
    /// **Default:** Returns Material Blue (#2196F3) for invalid input
    ///
    /// - Parameter colorString: Hex color string
    /// - Returns: SwiftUI Color object
    ///
    /// ## Examples
    /// ```swift
    /// parseColor("#2196F3")      // Valid 6-digit
    /// parseColor("#FF2196F3")    // Valid 8-digit with alpha
    /// parseColor("2196F3")       // Valid without #
    /// parseColor("#F00")         // Valid 3-digit (expands to #FF0000)
    /// parseColor("invalid")      // Returns default blue
    /// ```
    public static func parseColor(_ colorString: String) -> Color {
        // Handle empty string
        guard !colorString.isEmpty else {
            return defaultBlue
        }

        // Remove # prefix if present
        let cleanColor = colorString.hasPrefix("#") ? String(colorString.dropFirst()) : colorString

        // Handle different hex formats
        let hexString: String
        switch cleanColor.count {
        case 3:
            // Expand RGB to RRGGBB
            hexString = cleanColor.map { "\($0)\($0)" }.joined()
        case 6:
            // Standard RRGGBB format
            hexString = cleanColor
        case 8:
            // AARRGGBB format
            hexString = cleanColor
        default:
            // Invalid format, return default
            return defaultBlue
        }

        // Parse hex string
        guard let hexValue = UInt64(hexString, radix: 16) else {
            return defaultBlue
        }

        // Extract color components
        let red, green, blue, alpha: Double
        if hexString.count == 8 {
            // AARRGGBB
            alpha = Double((hexValue & 0xFF000000) >> 24) / 255.0
            red = Double((hexValue & 0x00FF0000) >> 16) / 255.0
            green = Double((hexValue & 0x0000FF00) >> 8) / 255.0
            blue = Double(hexValue & 0x000000FF) / 255.0
        } else {
            // RRGGBB (default alpha to 1.0)
            alpha = 1.0
            red = Double((hexValue & 0xFF0000) >> 16) / 255.0
            green = Double((hexValue & 0x00FF00) >> 8) / 255.0
            blue = Double(hexValue & 0x0000FF) / 255.0
        }

        return Color(red: red, green: green, blue: blue, opacity: alpha)
    }

    /// Default Material Blue color (#2196F3)
    private static let defaultBlue = Color(red: 0x21 / 255.0, green: 0x96 / 255.0, blue: 0xF3 / 255.0)

    // MARK: - Data Point Calculations

    /// Calculate chart bounds from data points
    ///
    /// Computes min/max values for both X and Y axes.
    /// Returns zero bounds for empty data.
    ///
    /// - Parameter data: Array of chart data points
    /// - Returns: ChartBounds with min/max for X and Y
    ///
    /// ## Example
    /// ```swift
    /// let data = [
    ///     ChartDataPoint(x: 0, y: 100),
    ///     ChartDataPoint(x: 1, y: 150)
    /// ]
    /// let bounds = calculateChartBounds(data: data)
    /// // bounds.minX = 0, maxX = 1, minY = 100, maxY = 150
    /// ```
    public static func calculateChartBounds(data: [ChartDataPoint]) -> ChartBounds {
        guard !data.isEmpty else {
            return ChartBounds(minX: 0, maxX: 0, minY: 0, maxY: 0)
        }

        let xValues = data.map { $0.x }
        let yValues = data.map { $0.y }

        return ChartBounds(
            minX: xValues.min() ?? 0,
            maxX: xValues.max() ?? 0,
            minY: yValues.min() ?? 0,
            maxY: yValues.max() ?? 0
        )
    }

    // MARK: - Coordinate Transformations

    /// Transform data coordinates to screen coordinates
    ///
    /// Converts chart data point to pixel coordinates for rendering.
    /// Y-axis is inverted (0 at top, max at bottom) to match screen coordinates.
    ///
    /// **Edge Cases:**
    /// - Zero-width bounds: Returns 0 for X
    /// - Zero-height bounds: Returns 0 for Y
    /// - Zero-size screen: Returns (0, 0)
    ///
    /// - Parameters:
    ///   - dataPoint: Data point in chart coordinates
    ///   - dataBounds: Min/max bounds of chart data
    ///   - screenSize: Size of rendering area in pixels
    /// - Returns: Screen coordinates as CGPoint
    ///
    /// ## Example
    /// ```swift
    /// let dataPoint = CGPoint(x: 5, y: 50)
    /// let bounds = ChartBounds(minX: 0, maxX: 10, minY: 0, maxY: 100)
    /// let screenSize = CGSize(width: 400, height: 300)
    ///
    /// let screenPoint = transformToScreenCoordinates(
    ///     dataPoint: dataPoint,
    ///     dataBounds: bounds,
    ///     screenSize: screenSize
    /// )
    /// // screenPoint = (200, 150) - center of screen
    /// ```
    public static func transformToScreenCoordinates(
        dataPoint: CGPoint,
        dataBounds: ChartBounds,
        screenSize: CGSize
    ) -> CGPoint {
        // Handle zero-size screen
        guard screenSize.width > 0 && screenSize.height > 0 else {
            return .zero
        }

        // Calculate data range
        let dataWidth = dataBounds.maxX - dataBounds.minX
        let dataHeight = dataBounds.maxY - dataBounds.minY

        // Handle zero-width data range
        let screenX: CGFloat
        if dataWidth > 0 {
            let normalizedX = (dataPoint.x - dataBounds.minX) / dataWidth
            screenX = normalizedX * screenSize.width
        } else {
            screenX = 0
        }

        // Handle zero-height data range
        let screenY: CGFloat
        if dataHeight > 0 {
            let normalizedY = (dataPoint.y - dataBounds.minY) / dataHeight
            // Invert Y axis (0 at top, max at bottom)
            screenY = (1.0 - normalizedY) * screenSize.height
        } else {
            screenY = 0
        }

        return CGPoint(x: screenX, y: screenY)
    }

    // MARK: - Animation Helpers

    /// Create animation configuration for charts
    ///
    /// Provides consistent animation settings across all chart types.
    /// Uses ease-out cubic timing for smooth, natural motion.
    ///
    /// **Duration = 0:** Animation is disabled (nil animation)
    ///
    /// - Parameter duration: Animation duration in milliseconds
    /// - Returns: AnimationConfiguration with duration and animation
    ///
    /// ## Example
    /// ```swift
    /// let config = createAnimationConfiguration(duration: 500)
    /// // config.duration = 0.5 seconds
    /// // config.animation = .easeOut
    ///
    /// // Disable animation
    /// let noAnimation = createAnimationConfiguration(duration: 0)
    /// // noAnimation.animation = nil
    /// ```
    public static func createAnimationConfiguration(duration: Int) -> AnimationConfiguration {
        let durationInSeconds = Double(duration) / 1000.0

        if duration > 0 {
            return AnimationConfiguration(
                duration: durationInSeconds,
                animation: .easeOut(duration: durationInSeconds)
            )
        } else {
            return AnimationConfiguration(
                duration: 0,
                animation: nil
            )
        }
    }

    // MARK: - Value Formatting

    /// Format number for display in charts
    ///
    /// Formats numbers with appropriate precision and units.
    ///
    /// **Formatting Rules:**
    /// - < 1000: Show 1 decimal place
    /// - >= 1000: Show as K (thousands)
    /// - >= 1,000,000: Show as M (millions)
    ///
    /// - Parameter value: Number to format
    /// - Returns: Formatted string
    ///
    /// ## Examples
    /// ```swift
    /// formatValue(123.456)     // "123.5"
    /// formatValue(1234.56)     // "1.2K"
    /// formatValue(1234567)     // "1.2M"
    /// ```
    public static func formatValue(_ value: Double) -> String {
        let absValue = abs(value)
        let sign = value < 0 ? "-" : ""

        if absValue >= 1_000_000 {
            return String(format: "%@%.1fM", sign, absValue / 1_000_000)
        } else if absValue >= 1_000 {
            return String(format: "%@%.1fK", sign, absValue / 1_000)
        } else {
            return String(format: "%@%.1f", sign, absValue)
        }
    }

    // MARK: - Interpolation

    /// Linear interpolation between two values
    ///
    /// - Parameters:
    ///   - start: Start value
    ///   - end: End value
    ///   - fraction: Interpolation fraction (0.0 to 1.0)
    /// - Returns: Interpolated value
    ///
    /// ## Example
    /// ```swift
    /// lerp(start: 0, end: 100, fraction: 0.5)  // 50.0
    /// lerp(start: 0, end: 100, fraction: 0.25) // 25.0
    /// ```
    public static func lerp(start: Double, end: Double, fraction: Double) -> Double {
        return start + (end - start) * fraction
    }

    // MARK: - Grid Calculations

    /// Calculate grid line positions for axis
    ///
    /// Generates evenly-spaced grid lines with nice round numbers.
    ///
    /// - Parameters:
    ///   - min: Minimum value
    ///   - max: Maximum value
    ///   - targetCount: Target number of grid lines (approximate)
    /// - Returns: Array of grid line values
    ///
    /// ## Example
    /// ```swift
    /// calculateGridLines(min: 0, max: 100, targetCount: 5)
    /// // Returns: [0, 25, 50, 75, 100]
    /// ```
    public static func calculateGridLines(
        min: Double,
        max: Double,
        targetCount: Int
    ) -> [Double] {
        guard max > min, targetCount > 0 else {
            return []
        }

        let range = max - min
        let roughStep = range / Double(targetCount - 1)

        // Round step to nice number
        let magnitude = pow(10, floor(log10(roughStep)))
        let normalizedStep = roughStep / magnitude
        let niceStep: Double
        if normalizedStep < 1.5 {
            niceStep = 1
        } else if normalizedStep < 3 {
            niceStep = 2
        } else if normalizedStep < 7 {
            niceStep = 5
        } else {
            niceStep = 10
        }
        let step = niceStep * magnitude

        // Generate grid lines
        var gridLines: [Double] = []
        var currentValue = floor(min / step) * step
        while currentValue <= max {
            if currentValue >= min {
                gridLines.append(currentValue)
            }
            currentValue += step
        }

        return gridLines
    }
}

// MARK: - Supporting Types

/// Chart data point
public struct ChartDataPoint {
    public let x: Double
    public let y: Double

    public init(x: Double, y: Double) {
        self.x = x
        self.y = y
    }
}

/// Chart bounds (min/max for X and Y axes)
public struct ChartBounds {
    public let minX: Double
    public let maxX: Double
    public let minY: Double
    public let maxY: Double

    public init(minX: Double, maxX: Double, minY: Double, maxY: Double) {
        self.minX = minX
        self.maxX = maxX
        self.minY = minY
        self.maxY = maxY
    }

    /// Width of data range
    public var width: Double {
        maxX - minX
    }

    /// Height of data range
    public var height: Double {
        maxY - minY
    }
}

/// Animation configuration for charts
public struct AnimationConfiguration {
    public let duration: Double
    public let animation: Animation?

    public init(duration: Double, animation: Animation?) {
        self.duration = duration
        self.animation = animation
    }
}
