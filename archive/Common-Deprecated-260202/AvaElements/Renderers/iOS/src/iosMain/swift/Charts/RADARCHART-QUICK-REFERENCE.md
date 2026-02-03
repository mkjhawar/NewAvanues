# RadarChart Quick Reference

**Component:** RadarChart (Spider/Web Chart)
**Platform:** iOS (SwiftUI Canvas API)
**Status:** ✅ Production Ready

---

## Quick Start

### Basic Usage
```swift
import SwiftUI

let series = [
    RadarChartView.RadarSeries(
        label: "Player 1",
        values: [80, 90, 70, 85, 75],
        color: "#2196F3"
    )
]

RadarChartView(
    axes: ["Speed", "Power", "Defense", "Agility", "Intelligence"],
    series: series
)
```

### Multiple Series
```swift
let series = [
    RadarChartView.RadarSeries(
        label: "Player 1",
        values: [80, 90, 70, 85, 75],
        color: "#2196F3"
    ),
    RadarChartView.RadarSeries(
        label: "Player 2",
        values: [70, 85, 90, 75, 80],
        color: "#F44336"
    )
]

RadarChartView(
    axes: ["Speed", "Power", "Defense", "Agility", "Intelligence"],
    series: series,
    maxValue: 100,
    size: 350
)
```

---

## API Reference

### RadarChartView

#### Initializer
```swift
init(
    axes: [String],
    series: [RadarSeries],
    maxValue: Float = 100,
    size: Float = 300,
    showGrid: Bool = true,
    gridLevels: Int = 5,
    animated: Bool = true,
    contentDescription: String? = nil
)
```

#### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `axes` | `[String]` | Required | Axis labels (min 3) |
| `series` | `[RadarSeries]` | Required | Data series to display |
| `maxValue` | `Float` | `100` | Maximum value for scaling |
| `size` | `Float` | `300` | Chart size in points |
| `showGrid` | `Bool` | `true` | Show grid lines |
| `gridLevels` | `Int` | `5` | Number of grid levels |
| `animated` | `Bool` | `true` | Enable animations |
| `contentDescription` | `String?` | `nil` | Accessibility description |

### RadarSeries

#### Structure
```swift
struct RadarSeries {
    let label: String
    let values: [Float]
    let color: String?

    var average: Float { get }
}
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `label` | `String` | Series label for legend |
| `values` | `[Float]` | Data values (one per axis) |
| `color` | `String?` | Hex color (e.g., "#2196F3") |
| `average` | `Float` | Computed average value |

---

## Common Patterns

### Pattern 1: Character Stats
```swift
let characterStats = RadarChartView(
    axes: ["Strength", "Speed", "Intelligence", "Agility", "Defense", "Endurance"],
    values: [85, 70, 90, 75, 80, 88],
    label: "Hero"
)
```

### Pattern 2: Team Comparison
```swift
let teamComparison = RadarChartView(
    axes: ["Attack", "Defense", "Midfield", "Speed", "Teamwork"],
    series: [
        RadarChartView.RadarSeries(
            label: "Team A",
            values: [80, 90, 70, 85, 75],
            color: "#2196F3"
        ),
        RadarChartView.RadarSeries(
            label: "Team B",
            values: [70, 85, 90, 75, 80],
            color: "#F44336"
        )
    ],
    maxValue: 100,
    size: 350
)
```

### Pattern 3: Product Features
```swift
let productFeatures = RadarChartView(
    axes: ["Quality", "Price", "Design", "Performance", "Support"],
    series: [
        RadarChartView.RadarSeries(
            label: "Product A",
            values: [90, 70, 85, 80, 75],
            color: "#4CAF50"
        ),
        RadarChartView.RadarSeries(
            label: "Product B",
            values: [75, 85, 80, 90, 70],
            color: "#FF9800"
        )
    ]
)
```

### Pattern 4: Skills Assessment
```swift
let skillsAssessment = RadarChartView(
    axes: ["Communication", "Leadership", "Technical", "Creativity"],
    series: [
        RadarChartView.RadarSeries(
            label: "Self-Assessment",
            values: [80, 70, 90, 85],
            color: "#2196F3"
        ),
        RadarChartView.RadarSeries(
            label: "Manager Assessment",
            values: [85, 75, 88, 82],
            color: "#F44336"
        )
    ],
    showGrid: true,
    gridLevels: 4
)
```

---

## Configuration Options

### Grid Customization
```swift
// Show grid with 4 levels
RadarChartView(
    axes: axes,
    series: series,
    showGrid: true,
    gridLevels: 4
)

// Hide grid
RadarChartView(
    axes: axes,
    series: series,
    showGrid: false
)
```

### Size Adjustment
```swift
// Small chart
RadarChartView(axes: axes, series: series, size: 200)

// Medium chart (default)
RadarChartView(axes: axes, series: series, size: 300)

// Large chart
RadarChartView(axes: axes, series: series, size: 400)
```

### Custom Max Value
```swift
// Scale 0-10
RadarChartView(axes: axes, series: series, maxValue: 10)

// Scale 0-100 (default)
RadarChartView(axes: axes, series: series, maxValue: 100)

// Scale 0-5
RadarChartView(axes: axes, series: series, maxValue: 5)
```

### Disable Animation
```swift
RadarChartView(
    axes: axes,
    series: series,
    animated: false
)
```

---

## Styling

### Custom Colors
```swift
let series = [
    RadarChartView.RadarSeries(
        label: "Series 1",
        values: [80, 90, 70, 85, 75],
        color: "#2196F3"  // Blue
    ),
    RadarChartView.RadarSeries(
        label: "Series 2",
        values: [70, 85, 90, 75, 80],
        color: "#F44336"  // Red
    ),
    RadarChartView.RadarSeries(
        label: "Series 3",
        values: [75, 80, 85, 90, 70],
        color: "#4CAF50"  // Green
    )
]
```

### Default Colors
If `color` is `nil`, chart uses default palette:
- Series 0: Blue (#2196F3)
- Series 1: Red (#F44336)
- Series 2: Green (#4CAF50)
- Series 3: Orange (#FF9800)
- Series 4: Purple (#9C27B0)

---

## Accessibility

### Custom Description
```swift
RadarChartView(
    axes: axes,
    series: series,
    contentDescription: "Character comparison showing 5 attributes for 2 players"
)
```

### VoiceOver Labels
Chart automatically provides:
- Chart description with axis count and series count
- Series descriptions with average values
- Selection announcements

Example VoiceOver output:
```
"Radar chart with 5 axes and 2 series.
5 axes: Speed, Power, Defense, Agility, Intelligence.
Series 1 of 2: Player 1 with 5 data points, average value 80.0.
Series 2 of 2: Player 2 with 5 data points, average value 77.0."
```

---

## Validation

### Data Requirements
- **Minimum axes:** 3
- **Maximum axes (recommended):** 12
- **Value count:** Must match axis count
- **Max value:** Must be > 0

### Validation Example
```swift
// ✅ Valid
let valid = RadarChartView(
    axes: ["A", "B", "C"],
    series: [
        RadarChartView.RadarSeries(
            label: "Data",
            values: [1, 2, 3],  // 3 values for 3 axes
            color: nil
        )
    ]
)

// ❌ Invalid (too few axes)
let invalid1 = RadarChartView(
    axes: ["A", "B"],  // Only 2 axes (min 3)
    series: [...]
)

// ❌ Invalid (mismatched count)
let invalid2 = RadarChartView(
    axes: ["A", "B", "C", "D", "E"],  // 5 axes
    series: [
        RadarChartView.RadarSeries(
            label: "Data",
            values: [1, 2, 3],  // Only 3 values
            color: nil
        )
    ]
)
```

---

## Math Reference

### Polar to Cartesian Conversion
```
x = centerX + r × cos(θ)
y = centerY + r × sin(θ)
```

Where:
- `r` = radius (scaled by value)
- `θ` = angle in radians
- `centerX`, `centerY` = center of chart

### Angle Calculation
```
angleStep = 360° / axisCount
angle[i] = -90° + (i × angleStep)
```

Start at -90° (top of circle)

### Value Scaling
```
normalizedValue = value / maxValue  // 0.0 to 1.0
scaledRadius = radius × normalizedValue
```

---

## Performance Tips

### Recommended Limits
- **Axes:** 3-12 (optimal: 5-6)
- **Series:** 1-5 (optimal: 2-3)
- **Grid Levels:** 3-7 (optimal: 5)

### Animation Considerations
- Animations run at 60 FPS
- Disable for static displays: `animated: false`
- Animation duration: 800ms (fixed)

### Memory
- Minimal state: 2 @State properties
- Efficient Canvas rendering
- No image assets required

---

## Troubleshooting

### Issue: Grid not visible
**Solution:** Check `showGrid` parameter
```swift
RadarChartView(axes: axes, series: series, showGrid: true)
```

### Issue: Labels overlap
**Solution:** Reduce axis count or increase chart size
```swift
// Increase size
RadarChartView(axes: axes, series: series, size: 400)

// Or reduce axes
let axes = ["A", "B", "C", "D", "E"]  // 5 instead of 12
```

### Issue: Series not visible
**Solution:** Check value scaling and maxValue
```swift
// If values are small, adjust maxValue
RadarChartView(axes: axes, series: series, maxValue: 10)
```

### Issue: Invalid data error
**Solution:** Ensure values.count == axes.count
```swift
let axes = ["A", "B", "C"]  // 3 axes
let series = RadarChartView.RadarSeries(
    label: "Data",
    values: [1, 2, 3]  // 3 values ✅
)
```

---

## Integration with SwiftUI

### In a Form
```swift
Form {
    Section("Character Stats") {
        RadarChartView(
            axes: ["STR", "DEX", "INT", "WIS", "CON", "CHA"],
            series: [characterSeries],
            size: 250
        )
    }
}
```

### In a ScrollView
```swift
ScrollView {
    VStack(spacing: 20) {
        Text("Comparison")
            .font(.title)

        RadarChartView(
            axes: axes,
            series: series
        )

        Text("Legend")
            .font(.headline)

        // Additional content...
    }
}
```

### In a Sheet
```swift
.sheet(isPresented: $showChart) {
    NavigationView {
        RadarChartView(
            axes: axes,
            series: series
        )
        .navigationTitle("Performance")
        .navigationBarTitleDisplayMode(.inline)
    }
}
```

---

## Best Practices

### 1. Axis Count
- **3-6 axes:** Optimal readability
- **7-9 axes:** Acceptable for complex data
- **10-12 axes:** Maximum (labels may overlap)

### 2. Series Count
- **1-2 series:** Clearest comparison
- **3-4 series:** Still readable
- **5+ series:** Consider splitting into multiple charts

### 3. Color Selection
- Use high-contrast colors for multiple series
- Avoid similar hues (e.g., red + orange)
- Test in both light and dark mode

### 4. Labeling
- Keep axis labels short (1-2 words)
- Use abbreviations for long names
- Provide full names in accessibility description

### 5. Data Preparation
- Normalize values to same scale
- Handle missing data (use 0 or omit series)
- Validate counts before creating chart

---

## Examples Gallery

### Example 1: Single Series (Basic)
```swift
RadarChartView(
    axes: ["A", "B", "C", "D", "E"],
    values: [80, 90, 70, 85, 75],
    label: "Performance"
)
```

### Example 2: Dual Comparison
```swift
RadarChartView(
    axes: ["Speed", "Power", "Defense", "Agility", "Intelligence"],
    series: [
        RadarChartView.RadarSeries(label: "Before", values: [70, 65, 75, 70, 60], color: "#2196F3"),
        RadarChartView.RadarSeries(label: "After", values: [85, 80, 90, 85, 75], color: "#4CAF50")
    ]
)
```

### Example 3: Minimal Grid
```swift
RadarChartView(
    axes: ["Q1", "Q2", "Q3", "Q4"],
    series: [series],
    showGrid: true,
    gridLevels: 3
)
```

### Example 4: Large Display
```swift
RadarChartView(
    axes: sixAxes,
    series: multipleSeries,
    maxValue: 100,
    size: 450,
    gridLevels: 5
)
.frame(maxWidth: .infinity, maxHeight: .infinity)
```

---

## See Also

- **Implementation Report:** RADARCHART-IMPLEMENTATION-REPORT.md
- **Test Suite:** RadarChartTests.swift
- **Kotlin Data Class:** RadarChart.kt
- **Other Charts:** LineChart, BarChart, PieChart, etc.

---

**Last Updated:** 2025-11-25
**Version:** 1.0.0
**Status:** Production Ready ✅
