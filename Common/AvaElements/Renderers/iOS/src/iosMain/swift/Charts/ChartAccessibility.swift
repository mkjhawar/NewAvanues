import SwiftUI
import Foundation

/// Chart accessibility utilities for VoiceOver support
///
/// Provides comprehensive accessibility support for chart components:
/// - Descriptive accessibility labels
/// - Accessibility values for data points
/// - VoiceOver hints for interactions
/// - Accessible navigation through chart data
///
/// **Standards:**
/// - WCAG 2.1 Level AA compliance
/// - iOS VoiceOver best practices
/// - Human Interface Guidelines for Charts
///
/// **iOS Version:** 16.0+
///
/// ## Usage Example
/// ```swift
/// // Generate chart accessibility label
/// let label = ChartAccessibility.generateChartLabel(
///     title: "Revenue Chart",
///     seriesCount: 2,
///     dataPointCount: 12,
///     chartType: "line"
/// )
///
/// // Generate data point accessibility value
/// let value = ChartAccessibility.generateDataPointValue(
///     x: 1.0,
///     y: 150.0,
///     xLabel: "Q1",
///     yLabel: "Revenue"
/// )
/// ```
public enum ChartAccessibility {

    // MARK: - Chart Labels

    /// Generate accessibility label for chart
    ///
    /// Creates a comprehensive description that VoiceOver reads when chart is focused.
    ///
    /// **Format:** "[Title]. [Type] chart with [N] series and [M] total data points"
    ///
    /// - Parameters:
    ///   - title: Chart title (optional)
    ///   - seriesCount: Number of data series
    ///   - dataPointCount: Total number of data points
    ///   - chartType: Type of chart (line, bar, pie, etc.)
    /// - Returns: Accessibility label string
    ///
    /// ## Examples
    /// ```swift
    /// generateChartLabel(title: "Revenue", seriesCount: 1, dataPointCount: 12, chartType: "line")
    /// // "Revenue. Line chart with 1 series and 12 data points"
    ///
    /// generateChartLabel(title: nil, seriesCount: 2, dataPointCount: 24, chartType: "bar")
    /// // "Bar chart with 2 series and 24 data points"
    /// ```
    public static func generateChartLabel(
        title: String?,
        seriesCount: Int,
        dataPointCount: Int,
        chartType: String
    ) -> String {
        var components: [String] = []

        // Add title if present
        if let title = title, !title.isEmpty {
            components.append(title)
        }

        // Add chart type and data summary
        let seriesText = seriesCount == 1 ? "1 series" : "\(seriesCount) series"
        let pointsText = dataPointCount == 1 ? "1 data point" : "\(dataPointCount) data points"

        components.append("\(chartType.capitalized) chart with \(seriesText) and \(pointsText)")

        return components.joined(separator: ". ")
    }

    /// Generate accessibility label for chart with custom description
    ///
    /// - Parameters:
    ///   - title: Chart title
    ///   - description: Custom description
    /// - Returns: Accessibility label
    public static func generateChartLabel(title: String?, description: String) -> String {
        if let title = title, !title.isEmpty {
            return "\(title). \(description)"
        }
        return description
    }

    // MARK: - Data Point Values

    /// Generate accessibility value for data point
    ///
    /// Creates a readable description of a data point's coordinates and values.
    ///
    /// **Format:** "[X Label]: [Y Label] [Y Value]"
    ///
    /// - Parameters:
    ///   - x: X-axis value
    ///   - y: Y-axis value
    ///   - xLabel: X-axis label (e.g., "January", "Q1")
    ///   - yLabel: Y-axis label (e.g., "Revenue", "Temperature")
    /// - Returns: Accessibility value string
    ///
    /// ## Examples
    /// ```swift
    /// generateDataPointValue(x: 1, y: 150, xLabel: "Q1", yLabel: "Revenue")
    /// // "Q1: Revenue 150.0"
    ///
    /// generateDataPointValue(x: 3, y: 75.5, xLabel: "March", yLabel: "Temperature")
    /// // "March: Temperature 75.5 degrees"
    /// ```
    public static func generateDataPointValue(
        x: Double,
        y: Double,
        xLabel: String?,
        yLabel: String?
    ) -> String {
        var components: [String] = []

        // Add X-axis information
        if let xLabel = xLabel, !xLabel.isEmpty {
            components.append(xLabel)
        } else {
            components.append("X: \(formatValue(x))")
        }

        // Add Y-axis information
        if let yLabel = yLabel, !yLabel.isEmpty {
            components.append("\(yLabel) \(formatValue(y))")
        } else {
            components.append("Y: \(formatValue(y))")
        }

        return components.joined(separator: ": ")
    }

    /// Generate accessibility value for bar
    ///
    /// - Parameters:
    ///   - label: Bar label
    ///   - value: Bar value
    ///   - unit: Unit of measurement (optional)
    /// - Returns: Accessibility value string
    ///
    /// ## Example
    /// ```swift
    /// generateBarValue(label: "Q1", value: 150, unit: "dollars")
    /// // "Q1: 150.0 dollars"
    /// ```
    public static func generateBarValue(label: String, value: Double, unit: String? = nil) -> String {
        var text = "\(label): \(formatValue(value))"
        if let unit = unit {
            text += " \(unit)"
        }
        return text
    }

    /// Generate accessibility value for pie slice
    ///
    /// - Parameters:
    ///   - label: Slice label
    ///   - value: Slice value
    ///   - percentage: Percentage of total
    /// - Returns: Accessibility value string
    ///
    /// ## Example
    /// ```swift
    /// generatePieSliceValue(label: "Sales", value: 150, percentage: 45.5)
    /// // "Sales: 150.0, 45.5 percent of total"
    /// ```
    public static func generatePieSliceValue(
        label: String,
        value: Double,
        percentage: Double
    ) -> String {
        return "\(label): \(formatValue(value)), \(formatValue(percentage)) percent of total"
    }

    // MARK: - Interaction Hints

    /// Generate accessibility hint for interactive chart
    ///
    /// Provides guidance on how to interact with the chart.
    ///
    /// - Parameter isInteractive: Whether chart supports interaction
    /// - Returns: Accessibility hint string
    ///
    /// ## Examples
    /// ```swift
    /// generateChartHint(isInteractive: true)
    /// // "Tap to view details for individual data points"
    ///
    /// generateChartHint(isInteractive: false)
    /// // ""
    /// ```
    public static func generateChartHint(isInteractive: Bool) -> String {
        if isInteractive {
            return "Tap to view details for individual data points"
        }
        return ""
    }

    /// Generate accessibility hint for data point
    ///
    /// - Returns: Hint for data point interaction
    public static func generateDataPointHint() -> String {
        return "Double tap to select"
    }

    /// Generate accessibility hint for legend item
    ///
    /// - Returns: Hint for legend interaction
    public static func generateLegendItemHint() -> String {
        return "Double tap to toggle series visibility"
    }

    // MARK: - Range Descriptions

    /// Generate accessibility description for value range
    ///
    /// - Parameters:
    ///   - min: Minimum value
    ///   - max: Maximum value
    ///   - label: Axis label (optional)
    /// - Returns: Range description string
    ///
    /// ## Example
    /// ```swift
    /// generateRangeDescription(min: 0, max: 100, label: "Revenue")
    /// // "Revenue ranges from 0.0 to 100.0"
    /// ```
    public static func generateRangeDescription(
        min: Double,
        max: Double,
        label: String?
    ) -> String {
        let labelText = label ?? "Values"
        return "\(labelText) ranges from \(formatValue(min)) to \(formatValue(max))"
    }

    /// Generate accessibility description for axis
    ///
    /// - Parameters:
    ///   - axis: Axis name (X or Y)
    ///   - label: Axis label
    ///   - min: Minimum value
    ///   - max: Maximum value
    /// - Returns: Axis description
    ///
    /// ## Example
    /// ```swift
    /// generateAxisDescription(axis: "Y", label: "Revenue", min: 0, max: 500)
    /// // "Y-axis: Revenue, from 0.0 to 500.0"
    /// ```
    public static func generateAxisDescription(
        axis: String,
        label: String?,
        min: Double,
        max: Double
    ) -> String {
        var text = "\(axis)-axis"
        if let label = label, !label.isEmpty {
            text += ": \(label)"
        }
        text += ", from \(formatValue(min)) to \(formatValue(max))"
        return text
    }

    // MARK: - Summary Descriptions

    /// Generate summary description of chart data
    ///
    /// Provides a quick overview of key statistics.
    ///
    /// - Parameters:
    ///   - values: Array of values
    ///   - label: Data label
    /// - Returns: Summary string
    ///
    /// ## Example
    /// ```swift
    /// generateSummaryDescription(values: [10, 20, 30, 40], label: "Revenue")
    /// // "Revenue: Minimum 10.0, Maximum 40.0, Average 25.0"
    /// ```
    public static func generateSummaryDescription(
        values: [Double],
        label: String?
    ) -> String {
        guard !values.isEmpty else {
            return "No data"
        }

        let min = values.min() ?? 0
        let max = values.max() ?? 0
        let avg = values.reduce(0, +) / Double(values.count)

        let labelText = label.map { "\($0): " } ?? ""
        return "\(labelText)Minimum \(formatValue(min)), Maximum \(formatValue(max)), Average \(formatValue(avg))"
    }

    /// Generate trend description
    ///
    /// Describes the overall trend of the data.
    ///
    /// - Parameter values: Array of values in chronological order
    /// - Returns: Trend description
    ///
    /// ## Examples
    /// ```swift
    /// generateTrendDescription(values: [10, 20, 30, 40])
    /// // "Increasing trend"
    ///
    /// generateTrendDescription(values: [40, 30, 20, 10])
    /// // "Decreasing trend"
    ///
    /// generateTrendDescription(values: [10, 20, 15, 25, 20])
    /// // "Fluctuating trend"
    /// ```
    public static func generateTrendDescription(values: [Double]) -> String {
        guard values.count >= 2 else {
            return "Insufficient data for trend"
        }

        let first = values.first!
        let last = values.last!
        let diff = last - first
        let percentChange = (diff / first) * 100.0

        if abs(percentChange) < 5.0 {
            return "Stable trend"
        } else if diff > 0 {
            return "Increasing trend, up \(formatValue(percentChange)) percent"
        } else {
            return "Decreasing trend, down \(formatValue(abs(percentChange))) percent"
        }
    }

    // MARK: - Series Descriptions

    /// Generate accessibility description for series
    ///
    /// - Parameters:
    ///   - label: Series label
    ///   - pointCount: Number of points in series
    ///   - index: Series index
    ///   - total: Total number of series
    /// - Returns: Series description
    ///
    /// ## Example
    /// ```swift
    /// generateSeriesDescription(label: "Revenue", pointCount: 12, index: 0, total: 2)
    /// // "Series 1 of 2: Revenue with 12 data points"
    /// ```
    public static func generateSeriesDescription(
        label: String,
        pointCount: Int,
        index: Int,
        total: Int
    ) -> String {
        let seriesNumber = index + 1
        let pointsText = pointCount == 1 ? "1 data point" : "\(pointCount) data points"
        return "Series \(seriesNumber) of \(total): \(label) with \(pointsText)"
    }

    // MARK: - Navigation Announcements

    /// Generate announcement for navigating to data point
    ///
    /// Used with `AccessibilityAnnouncement` for dynamic updates.
    ///
    /// - Parameters:
    ///   - index: Point index
    ///   - total: Total points
    ///   - value: Point value description
    /// - Returns: Navigation announcement
    ///
    /// ## Example
    /// ```swift
    /// generateNavigationAnnouncement(index: 3, total: 12, value: "Q1: Revenue 150.0")
    /// // "Point 4 of 12. Q1: Revenue 150.0"
    /// ```
    public static func generateNavigationAnnouncement(
        index: Int,
        total: Int,
        value: String
    ) -> String {
        let pointNumber = index + 1
        return "Point \(pointNumber) of \(total). \(value)"
    }

    /// Generate announcement for selecting element
    ///
    /// - Parameter elementDescription: Description of selected element
    /// - Returns: Selection announcement
    public static func generateSelectionAnnouncement(elementDescription: String) -> String {
        return "Selected: \(elementDescription)"
    }

    // MARK: - Private Helpers

    /// Format value for accessibility
    ///
    /// Rounds to 1 decimal place for readability.
    ///
    /// - Parameter value: Value to format
    /// - Returns: Formatted string
    private static func formatValue(_ value: Double) -> String {
        // Round to 1 decimal place
        let rounded = (value * 10).rounded() / 10

        // Check if we can display as integer
        if rounded.truncatingRemainder(dividingBy: 1.0) == 0 {
            return String(format: "%.0f", rounded)
        } else {
            return String(format: "%.1f", rounded)
        }
    }

    // MARK: - Trait Helpers

    /// Get accessibility traits for chart element
    ///
    /// - Parameters:
    ///   - isInteractive: Whether element is interactive
    ///   - isSelected: Whether element is selected
    /// - Returns: Accessibility traits
    public static func traitsForChartElement(
        isInteractive: Bool,
        isSelected: Bool
    ) -> AccessibilityTraits {
        var traits: AccessibilityTraits = []

        if isInteractive {
            traits.insert(.isButton)
        } else {
            traits.insert(.isStaticText)
        }

        if isSelected {
            traits.insert(.isSelected)
        }

        return traits
    }

    /// Get accessibility traits for chart container
    ///
    /// - Returns: Accessibility traits for chart
    public static func traitsForChart() -> AccessibilityTraits {
        return [.isImage, .allowsDirectInteraction]
    }

    // MARK: - Sorting Descriptions

    /// Generate description of data sorting
    ///
    /// - Parameter sortOrder: Sort order (ascending/descending/none)
    /// - Returns: Sort description
    public static func generateSortDescription(sortOrder: String) -> String {
        switch sortOrder.lowercased() {
        case "ascending":
            return "Data sorted in ascending order"
        case "descending":
            return "Data sorted in descending order"
        default:
            return "Data in original order"
        }
    }

    // MARK: - Error States

    /// Generate accessibility label for error state
    ///
    /// - Parameter error: Error message
    /// - Returns: Error accessibility label
    public static func generateErrorLabel(error: String) -> String {
        return "Chart error: \(error)"
    }

    /// Generate accessibility label for loading state
    ///
    /// - Returns: Loading accessibility label
    public static func generateLoadingLabel() -> String {
        return "Chart loading"
    }

    /// Generate accessibility label for empty state
    ///
    /// - Returns: Empty state accessibility label
    public static func generateEmptyStateLabel() -> String {
        return "No chart data available"
    }

    // MARK: - Comparison Descriptions

    /// Generate comparison between two values
    ///
    /// - Parameters:
    ///   - value1: First value
    ///   - value2: Second value
    ///   - label1: First value label
    ///   - label2: Second value label
    /// - Returns: Comparison description
    ///
    /// ## Example
    /// ```swift
    /// generateComparisonDescription(value1: 100, value2: 150, label1: "Q1", label2: "Q2")
    /// // "Q2 is 50.0 percent higher than Q1"
    /// ```
    public static func generateComparisonDescription(
        value1: Double,
        value2: Double,
        label1: String,
        label2: String
    ) -> String {
        let diff = value2 - value1
        let percentChange = abs((diff / value1) * 100.0)

        if diff > 0 {
            return "\(label2) is \(formatValue(percentChange)) percent higher than \(label1)"
        } else if diff < 0 {
            return "\(label2) is \(formatValue(percentChange)) percent lower than \(label1)"
        } else {
            return "\(label2) equals \(label1)"
        }
    }
}

// MARK: - SwiftUI Extensions

extension View {
    /// Add chart accessibility modifiers
    ///
    /// Convenience method to add all necessary accessibility attributes to a chart view.
    ///
    /// - Parameters:
    ///   - label: Accessibility label
    ///   - value: Accessibility value (optional)
    ///   - hint: Accessibility hint (optional)
    ///   - traits: Accessibility traits
    /// - Returns: Modified view with accessibility
    ///
    /// ## Example
    /// ```swift
    /// ChartView()
    ///     .chartAccessibility(
    ///         label: "Revenue Chart",
    ///         value: "12 data points",
    ///         hint: "Tap to view details",
    ///         traits: [.isImage]
    ///     )
    /// ```
    public func chartAccessibility(
        label: String,
        value: String? = nil,
        hint: String? = nil,
        traits: AccessibilityTraits = []
    ) -> some View {
        var view = self
            .accessibilityLabel(label)
            .accessibilityAddTraits(traits)

        if let value = value {
            view = view.accessibilityValue(value)
        }

        if let hint = hint {
            view = view.accessibilityHint(hint)
        }

        return view
    }
}
