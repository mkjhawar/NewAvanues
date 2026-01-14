# iOS Chart Foundation - Quick Reference

**Agent:** ios-chart-000 | **Status:** ✅ COMPLETE

---

## Import

```swift
// All chart helpers are in the Charts module
import ChartHelpers
import ChartColors
import ChartAccessibility
```

---

## ChartHelpers - Most Common Functions

### 1. Parse Color from Hex
```swift
let blue = ChartHelpers.parseColor("#2196F3")
let redWithAlpha = ChartHelpers.parseColor("#FF2196F3")
let shorthand = ChartHelpers.parseColor("#F00") // expands to #FF0000
```

### 2. Calculate Chart Bounds
```swift
let data = [
    ChartDataPoint(x: 0, y: 100),
    ChartDataPoint(x: 1, y: 150),
    ChartDataPoint(x: 2, y: 125)
]
let bounds = ChartHelpers.calculateChartBounds(data: data)
// bounds.minX = 0, maxX = 2, minY = 100, maxY = 150
```

### 3. Transform to Screen Coordinates
```swift
let screenPoint = ChartHelpers.transformToScreenCoordinates(
    dataPoint: CGPoint(x: 1, y: 125),
    dataBounds: bounds,
    screenSize: CGSize(width: 400, height: 300)
)
```

### 4. Format Values
```swift
ChartHelpers.formatValue(1234.56)    // "1.2K"
ChartHelpers.formatValue(1234567)    // "1.2M"
ChartHelpers.formatValue(123.456)    // "123.5"
```

### 5. Create Animation
```swift
let config = ChartHelpers.createAnimationConfiguration(duration: 500)
// config.duration = 0.5 seconds
// config.animation = .easeOut(duration: 0.5)
```

---

## ChartColors - Most Common Functions

### 1. Get Color for Series
```swift
let color = ChartColors.colorForSeries(index: 2, colorScheme: .light)
// Returns 3rd color from palette (wraps around)
```

### 2. Check WCAG AA Compliance
```swift
let passes = ChartColors.meetsWCAG_AA(
    foreground: .blue,
    background: .white
)
// Returns true if contrast ratio >= 4.5:1
```

### 3. Calculate Contrast Ratio
```swift
let ratio = ChartColors.calculateContrastRatio(
    foreground: .black,
    background: .white
)
// Returns 21.0 (maximum contrast)
```

### 4. Create Area Gradient
```swift
let gradient = ChartColors.createAreaGradient(
    color: .blue,
    opacity: 0.3
)
// Returns LinearGradient fading from 0.3 opacity to 0
```

### 5. Adjust Colors
```swift
let darkBlue = ChartColors.darken(.blue, by: 0.2)    // 20% darker
let lightBlue = ChartColors.lighten(.blue, by: 0.2)  // 20% lighter
let muted = ChartColors.adjustSaturation(.blue, to: 0.5) // 50% saturation
```

---

## ChartAccessibility - Most Common Functions

### 1. Generate Chart Label
```swift
let label = ChartAccessibility.generateChartLabel(
    title: "Revenue Chart",
    seriesCount: 2,
    dataPointCount: 24,
    chartType: "line"
)
// "Revenue Chart. Line chart with 2 series and 24 data points"
```

### 2. Generate Data Point Value
```swift
let value = ChartAccessibility.generateDataPointValue(
    x: 1.0,
    y: 150.0,
    xLabel: "Q1",
    yLabel: "Revenue"
)
// "Q1: Revenue 150.0"
```

### 3. Generate Summary
```swift
let summary = ChartAccessibility.generateSummaryDescription(
    values: [10, 20, 30, 40],
    label: "Revenue"
)
// "Revenue: Minimum 10.0, Maximum 40.0, Average 25.0"
```

### 4. Generate Trend
```swift
let trend = ChartAccessibility.generateTrendDescription(
    values: [10, 20, 30, 40]
)
// "Increasing trend, up 300.0 percent"
```

### 5. SwiftUI Integration
```swift
ChartView()
    .chartAccessibility(
        label: "Revenue Chart",
        value: "12 data points",
        hint: "Tap to view details",
        traits: [.isImage]
    )
```

---

## Complete Integration Example

```swift
import SwiftUI

struct SimpleLineChart: View {
    let data: [ChartDataPoint]
    let title: String
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        let bounds = ChartHelpers.calculateChartBounds(data: data)

        Canvas { context, size in
            // Get series color
            let color = ChartColors.colorForSeries(
                index: 0,
                colorScheme: colorScheme
            )

            // Draw line through points
            var path = Path()
            for (index, point) in data.enumerated() {
                let screenPoint = ChartHelpers.transformToScreenCoordinates(
                    dataPoint: CGPoint(x: point.x, y: point.y),
                    dataBounds: bounds,
                    screenSize: size
                )

                if index == 0 {
                    path.move(to: screenPoint)
                } else {
                    path.addLine(to: screenPoint)
                }
            }

            context.stroke(path, with: .color(color), lineWidth: 2)

            // Draw points
            for point in data {
                let screenPoint = ChartHelpers.transformToScreenCoordinates(
                    dataPoint: CGPoint(x: point.x, y: point.y),
                    dataBounds: bounds,
                    screenSize: size
                )

                context.fill(
                    Path(ellipseIn: CGRect(
                        x: screenPoint.x - 3,
                        y: screenPoint.y - 3,
                        width: 6,
                        height: 6
                    )),
                    with: .color(color)
                )
            }
        }
        .frame(height: 300)
        .chartAccessibility(
            label: ChartAccessibility.generateChartLabel(
                title: title,
                seriesCount: 1,
                dataPointCount: data.count,
                chartType: "line"
            ),
            value: ChartAccessibility.generateSummaryDescription(
                values: data.map { $0.y },
                label: "Values"
            ),
            hint: ChartAccessibility.generateChartHint(isInteractive: false),
            traits: ChartAccessibility.traitsForChart()
        )
    }
}

// Usage
SimpleLineChart(
    data: [
        ChartDataPoint(x: 0, y: 100),
        ChartDataPoint(x: 1, y: 150),
        ChartDataPoint(x: 2, y: 125),
        ChartDataPoint(x: 3, y: 175)
    ],
    title: "Sample Chart"
)
```

---

## Color Palette Reference

### Light Mode (Default)
```swift
ChartColors.defaultPalette[0] // Blue    #2196F3
ChartColors.defaultPalette[1] // Red     #F44336
ChartColors.defaultPalette[2] // Green   #4CAF50
ChartColors.defaultPalette[3] // Orange  #FF9800
ChartColors.defaultPalette[4] // Purple  #9C27B0
ChartColors.defaultPalette[5] // Yellow  #FFEB3B
ChartColors.defaultPalette[6] // Teal    #009688
ChartColors.defaultPalette[7] // Pink    #FF5252
ChartColors.defaultPalette[8] // BG      #607D8B
ChartColors.defaultPalette[9] // Brown   #795548
```

### Dark Mode
```swift
ChartColors.darkModePalette[0] // Light Blue    #64B5F6
ChartColors.darkModePalette[1] // Light Red     #EF5350
ChartColors.darkModePalette[2] // Light Green   #81C784
// ... (higher luminance versions)
```

---

## WCAG Compliance Quick Check

```swift
// Check if colors meet WCAG AA
let result = ChartColors.meetsWCAG_AA(
    foreground: myColor,
    background: backgroundColor
)

if !result {
    // Adjust color
    myColor = ChartColors.darken(myColor, by: 0.3)
    // Or use default palette (already compliant)
    myColor = ChartColors.defaultPalette[0]
}
```

---

## Performance Tips

1. **Cache Bounds:** Calculate once, reuse
   ```swift
   let bounds = ChartHelpers.calculateChartBounds(data: data)
   // Use bounds for all transformations
   ```

2. **Reuse Colors:** Don't parse repeatedly
   ```swift
   let color = ChartHelpers.parseColor("#2196F3")
   // Store and reuse
   ```

3. **Batch Transformations:** Loop efficiently
   ```swift
   let screenPoints = data.map { point in
       ChartHelpers.transformToScreenCoordinates(
           dataPoint: CGPoint(x: point.x, y: point.y),
           dataBounds: bounds,
           screenSize: size
       )
   }
   ```

---

## Common Patterns

### Pattern 1: Multi-Series Chart
```swift
let seriesColors = (0..<seriesCount).map { index in
    ChartColors.colorForSeries(index: index, colorScheme: colorScheme)
}
```

### Pattern 2: Interactive Point
```swift
.onTapGesture {
    let announcement = ChartAccessibility.generateSelectionAnnouncement(
        elementDescription: ChartAccessibility.generateDataPointValue(
            x: point.x,
            y: point.y,
            xLabel: point.label,
            yLabel: nil
        )
    )
    // Post accessibility announcement
    UIAccessibility.post(notification: .announcement, argument: announcement)
}
```

### Pattern 3: Animated Chart
```swift
@State private var animationProgress: Double = 0

let config = ChartHelpers.createAnimationConfiguration(duration: 500)

// In body
.onAppear {
    if let animation = config.animation {
        withAnimation(animation) {
            animationProgress = 1.0
        }
    }
}
```

---

## Testing Quick Reference

### Test Color Parsing
```swift
let color = ChartHelpers.parseColor("#2196F3")
// Verify RGB values within tolerance
```

### Test WCAG Compliance
```swift
let ratio = ChartColors.calculateContrastRatio(
    foreground: .white,
    background: .black
)
XCTAssertGreaterThan(ratio, 4.5) // WCAG AA
```

### Test Accessibility Labels
```swift
let label = ChartAccessibility.generateChartLabel(
    title: "Test",
    seriesCount: 1,
    dataPointCount: 10,
    chartType: "line"
)
XCTAssertTrue(label.contains("Test"))
XCTAssertTrue(label.contains("line chart"))
```

---

## Error Handling

All functions are defensive and handle edge cases:

```swift
// Empty data → zero bounds
ChartHelpers.calculateChartBounds(data: [])
// Returns: ChartBounds(minX: 0, maxX: 0, minY: 0, maxY: 0)

// Invalid hex → default blue
ChartHelpers.parseColor("invalid")
// Returns: Color(#2196F3)

// Zero-size screen → zero point
ChartHelpers.transformToScreenCoordinates(
    dataPoint: point,
    dataBounds: bounds,
    screenSize: .zero
)
// Returns: CGPoint(x: 0, y: 0)
```

---

## Resources

**Full Documentation:** See `README.md` in Charts/ directory
**Tests:** See `Tests/Charts/ChartHelpersTests.swift` for examples
**Android Reference:** `Android/src/androidMain/.../charts/`
**Shared Models:** `components/flutter-parity/.../charts/`

---

**Status:** ✅ Foundation Complete - Ready for Chart Implementations

**Last Updated:** 2025-11-25
