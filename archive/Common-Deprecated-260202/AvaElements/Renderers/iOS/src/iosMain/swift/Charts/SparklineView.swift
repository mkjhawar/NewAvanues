import SwiftUI
import Foundation

/// Sparkline component for iOS using SwiftUI Canvas API
///
/// A compact, minimal line chart designed for inline display in tables, cards, and dashboards.
/// Perfect for showing quick trends without axes, labels, or other visual chrome.
///
/// **Technology:** SwiftUI Canvas API (iOS 15+)
/// **Performance:** Hardware-accelerated rendering, supports 100+ data points
/// **Accessibility:** WCAG 2.1 Level AA compliant, full VoiceOver support with trend descriptions
///
/// ## Features
/// - Very compact (typically 100x30 points)
/// - No axes or labels - minimal visual weight
/// - Optional dots at data points
/// - Optional fill area under line
/// - Automatic scaling to fit bounds
/// - Smooth animations
/// - Trend description for VoiceOver
/// - Light/dark mode support
///
/// ## Usage Example
/// ```swift
/// // Simple sparkline
/// SparklineView(
///     data: [10, 15, 12, 18, 20, 17, 22],
///     color: .blue,
///     width: 100,
///     height: 30
/// )
///
/// // With dots and area fill
/// SparklineView(
///     data: [10, 15, 12, 18, 20, 17, 22],
///     showDots: true,
///     showArea: true,
///     color: .green
/// )
/// ```
///
/// @available iOS 15.0+
@available(iOS 15.0, *)
public struct SparklineView: View {

    // MARK: - Properties

    /// Data points to display
    let data: [Float]

    /// Width of the sparkline in points
    let width: CGFloat

    /// Height of the sparkline in points
    let height: CGFloat

    /// Line width in points
    let lineWidth: CGFloat

    /// Whether to show dots at data points
    let showDots: Bool

    /// Whether to show filled area under line
    let showArea: Bool

    /// Line and dot color
    let color: Color

    /// Whether to animate the sparkline
    let animated: Bool

    /// Content description for accessibility
    let contentDescription: String?

    /// Current color scheme (light/dark)
    @Environment(\.colorScheme) private var colorScheme

    /// Animation progress state
    @State private var animationProgress: Double = 0.0

    // MARK: - Initialization

    /// Initialize SparklineView
    ///
    /// - Parameters:
    ///   - data: Array of data values
    ///   - width: Width in points (default: 100)
    ///   - height: Height in points (default: 30)
    ///   - lineWidth: Line width in points (default: 2)
    ///   - showDots: Whether to show dots at data points (default: false)
    ///   - showArea: Whether to show filled area (default: false)
    ///   - color: Line color (default: blue)
    ///   - animated: Whether to animate (default: true)
    ///   - contentDescription: Accessibility description (auto-generated if nil)
    public init(
        data: [Float],
        width: CGFloat = 100,
        height: CGFloat = 30,
        lineWidth: CGFloat = 2,
        showDots: Bool = false,
        showArea: Bool = false,
        color: Color? = nil,
        animated: Bool = true,
        contentDescription: String? = nil
    ) {
        self.data = data
        self.width = width
        self.height = height
        self.lineWidth = lineWidth
        self.showDots = showDots
        self.showArea = showArea
        self.color = color ?? .blue
        self.animated = animated
        self.contentDescription = contentDescription
    }

    // MARK: - Body

    public var body: some View {
        Canvas { context, size in
            // Calculate data bounds
            guard let minValue = data.min(),
                  let maxValue = data.max(),
                  data.count >= 2 else {
                // Draw empty state or single point
                if data.count == 1 {
                    drawSinglePoint(context: context, size: size)
                }
                return
            }

            let valueRange = maxValue - minValue
            let adjustedRange = valueRange == 0 ? 1 : valueRange // Prevent division by zero

            // Build path for line
            var path = Path()
            let points = calculatePoints(size: size, minValue: minValue, range: adjustedRange)

            if !points.isEmpty {
                // Start path
                path.move(to: points[0])

                // Draw line through points
                for point in points.dropFirst() {
                    path.addLine(to: point)
                }

                // Draw area fill if enabled
                if showArea {
                    drawArea(context: context, path: path, points: points, size: size)
                }

                // Draw line
                context.stroke(
                    path,
                    with: .color(color),
                    style: StrokeStyle(
                        lineWidth: lineWidth,
                        lineCap: .round,
                        lineJoin: .round
                    )
                )

                // Draw dots if enabled
                if showDots {
                    drawDots(context: context, points: points)
                }
            }
        }
        .frame(width: width, height: height)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityValue(accessibilityValue)
        .accessibilityAddTraits([.isStaticText, .isImage])
        .onAppear {
            if animated {
                withAnimation(.easeOut(duration: 0.3)) {
                    animationProgress = 1.0
                }
            } else {
                animationProgress = 1.0
            }
        }
    }

    // MARK: - Drawing Helpers

    /// Calculate screen points from data values
    private func calculatePoints(size: CGSize, minValue: Float, range: Float) -> [CGPoint] {
        guard data.count >= 2 else { return [] }

        let stepX = size.width / CGFloat(data.count - 1)
        let animatedDataCount = animated ? Int(Double(data.count) * animationProgress) : data.count
        let pointsToShow = max(2, animatedDataCount) // Always show at least 2 points for a line

        return data.prefix(pointsToShow).enumerated().map { index, value in
            let x = CGFloat(index) * stepX
            let normalizedY = CGFloat((value - minValue) / range)
            let y = size.height - (normalizedY * size.height) // Invert Y for screen coordinates
            return CGPoint(x: x, y: y)
        }
    }

    /// Draw filled area under the line
    private func drawArea(context: GraphicsContext, path: Path, points: [CGPoint], size: CGSize) {
        guard !points.isEmpty else { return }

        var areaPath = path

        // Complete the area path to bottom corners
        areaPath.addLine(to: CGPoint(x: points.last!.x, y: size.height))
        areaPath.addLine(to: CGPoint(x: points.first!.x, y: size.height))
        areaPath.closeSubpath()

        // Fill with semi-transparent color
        context.fill(areaPath, with: .color(color.opacity(0.2)))
    }

    /// Draw dots at data points
    private func drawDots(context: GraphicsContext, points: [CGPoint]) {
        let dotRadius: CGFloat = lineWidth * 1.5

        for point in points {
            let dotPath = Path(ellipseIn: CGRect(
                x: point.x - dotRadius,
                y: point.y - dotRadius,
                width: dotRadius * 2,
                height: dotRadius * 2
            ))

            // Fill dot with color
            context.fill(dotPath, with: .color(color))

            // Draw white/dark border for contrast
            let borderColor = colorScheme == .dark ? Color.black : Color.white
            context.stroke(
                dotPath,
                with: .color(borderColor),
                style: StrokeStyle(lineWidth: 1)
            )
        }
    }

    /// Draw single data point
    private func drawSinglePoint(context: GraphicsContext, size: CGSize) {
        let center = CGPoint(x: size.width / 2, y: size.height / 2)
        let dotRadius: CGFloat = lineWidth * 1.5

        let dotPath = Path(ellipseIn: CGRect(
            x: center.x - dotRadius,
            y: center.y - dotRadius,
            width: dotRadius * 2,
            height: dotRadius * 2
        ))

        context.fill(dotPath, with: .color(color))
    }

    // MARK: - Computed Properties

    /// Trend direction
    private var trend: Trend {
        guard data.count >= 2 else { return .flat }

        let first = data.first!
        let last = data.last!
        let threshold: Float = 0.01

        if last > first * (1 + threshold) {
            return .up
        } else if last < first * (1 - threshold) {
            return .down
        } else {
            return .flat
        }
    }

    /// Percentage change from first to last value
    private var percentageChange: Float {
        guard data.count >= 2 else { return 0 }

        let first = data.first!
        guard first != 0 else { return 0 }

        let last = data.last!
        return ((last - first) / first) * 100
    }

    // MARK: - Accessibility

    /// Accessibility label for sparkline
    private var accessibilityLabel: String {
        if let contentDescription = contentDescription {
            return contentDescription
        }

        return "Sparkline chart"
    }

    /// Accessibility value with trend description
    private var accessibilityValue: String {
        guard !data.isEmpty else {
            return "No data"
        }

        if data.count == 1 {
            let value = data[0]
            return String(format: "Single value: %.1f", value)
        }

        // Generate trend description
        let trendText: String
        switch trend {
        case .up:
            trendText = "trending up"
        case .down:
            trendText = "trending down"
        case .flat:
            trendText = "stable"
        }

        let changeText: String
        if abs(percentageChange) > 0.1 {
            changeText = String(format: " by %.1f%%", abs(percentageChange))
        } else {
            changeText = ""
        }

        // Add range information
        let minValue = data.min() ?? 0
        let maxValue = data.max() ?? 0

        return String(
            format: "%d data points, %@%@. Range: %.1f to %.1f",
            data.count,
            trendText,
            changeText,
            minValue,
            maxValue
        )
    }

    // MARK: - Supporting Types

    /// Trend direction enum
    private enum Trend {
        case up
        case down
        case flat
    }
}

// MARK: - Convenience Initializers

@available(iOS 15.0, *)
extension SparklineView {
    /// Create sparkline from Kotlin component
    ///
    /// Maps Kotlin Sparkline data model to Swift view.
    ///
    /// - Parameter sparkline: Kotlin Sparkline component
    public init(fromKotlin sparkline: Any) {
        // This is a placeholder for Kotlin interop
        // In production, this would use actual Kotlin interop to extract:
        // - data array
        // - dimensions
        // - styling options
        // - animation settings

        // For now, initialize with empty data
        self.init(
            data: [],
            width: 100,
            height: 30,
            lineWidth: 2,
            showDots: false,
            showArea: false,
            animated: true
        )
    }
}

// MARK: - Preview Support

@available(iOS 15.0, *)
extension SparklineView {
    /// Sample data for previews - upward trend
    static var sampleDataUpward: [Float] {
        [10, 12, 11, 15, 18, 17, 20, 22]
    }

    /// Sample data for previews - downward trend
    static var sampleDataDownward: [Float] {
        [22, 20, 21, 18, 15, 16, 12, 10]
    }

    /// Sample data for previews - volatile
    static var sampleDataVolatile: [Float] {
        [10, 15, 8, 18, 12, 20, 10, 17]
    }

    /// Sample data for previews - flat
    static var sampleDataFlat: [Float] {
        [15, 15.2, 14.8, 15.1, 14.9, 15.0, 15.2, 15.1]
    }
}

// MARK: - Previews

@available(iOS 15.0, *)
struct SparklineView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            // Simple line
            VStack(alignment: .leading, spacing: 8) {
                Text("Simple Line").font(.caption).foregroundColor(.secondary)
                SparklineView(
                    data: SparklineView.sampleDataUpward,
                    color: .blue
                )
            }
            .padding()
            .previewDisplayName("Simple Line")

            // With dots
            VStack(alignment: .leading, spacing: 8) {
                Text("With Dots").font(.caption).foregroundColor(.secondary)
                SparklineView(
                    data: SparklineView.sampleDataUpward,
                    showDots: true,
                    color: .green
                )
            }
            .padding()
            .previewDisplayName("With Dots")

            // With area fill
            VStack(alignment: .leading, spacing: 8) {
                Text("With Area Fill").font(.caption).foregroundColor(.secondary)
                SparklineView(
                    data: SparklineView.sampleDataUpward,
                    showArea: true,
                    color: .purple
                )
            }
            .padding()
            .previewDisplayName("With Area Fill")

            // Downward trend
            VStack(alignment: .leading, spacing: 8) {
                Text("Downward Trend").font(.caption).foregroundColor(.secondary)
                SparklineView(
                    data: SparklineView.sampleDataDownward,
                    showArea: true,
                    color: .red
                )
            }
            .padding()
            .previewDisplayName("Downward Trend")

            // Volatile data
            VStack(alignment: .leading, spacing: 8) {
                Text("Volatile Data").font(.caption).foregroundColor(.secondary)
                SparklineView(
                    data: SparklineView.sampleDataVolatile,
                    showDots: true,
                    showArea: true,
                    color: .orange
                )
            }
            .padding()
            .previewDisplayName("Volatile Data")

            // Flat trend
            VStack(alignment: .leading, spacing: 8) {
                Text("Flat Trend").font(.caption).foregroundColor(.secondary)
                SparklineView(
                    data: SparklineView.sampleDataFlat,
                    color: .gray
                )
            }
            .padding()
            .previewDisplayName("Flat Trend")

            // Dark mode
            VStack(alignment: .leading, spacing: 8) {
                Text("Dark Mode").font(.caption).foregroundColor(.secondary)
                SparklineView(
                    data: SparklineView.sampleDataUpward,
                    showArea: true,
                    color: .cyan
                )
            }
            .padding()
            .preferredColorScheme(.dark)
            .previewDisplayName("Dark Mode")

            // Compact size (typical usage)
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    VStack(alignment: .leading) {
                        Text("Revenue").font(.caption).foregroundColor(.secondary)
                        Text("$42.5K").font(.title2).fontWeight(.semibold)
                    }
                    Spacer()
                    SparklineView(
                        data: SparklineView.sampleDataUpward,
                        width: 80,
                        height: 30,
                        showArea: true,
                        color: .green
                    )
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)

                HStack {
                    VStack(alignment: .leading) {
                        Text("Users").font(.caption).foregroundColor(.secondary)
                        Text("1,234").font(.title2).fontWeight(.semibold)
                    }
                    Spacer()
                    SparklineView(
                        data: SparklineView.sampleDataDownward,
                        width: 80,
                        height: 30,
                        showArea: true,
                        color: .red
                    )
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
            }
            .padding()
            .previewDisplayName("In Card (Typical Usage)")

            // Edge case: Single data point
            VStack(alignment: .leading, spacing: 8) {
                Text("Single Point").font(.caption).foregroundColor(.secondary)
                SparklineView(
                    data: [15],
                    color: .blue
                )
            }
            .padding()
            .previewDisplayName("Single Point")

            // Edge case: Two points
            VStack(alignment: .leading, spacing: 8) {
                Text("Two Points").font(.caption).foregroundColor(.secondary)
                SparklineView(
                    data: [10, 20],
                    showDots: true,
                    color: .blue
                )
            }
            .padding()
            .previewDisplayName("Two Points")
        }
    }
}
