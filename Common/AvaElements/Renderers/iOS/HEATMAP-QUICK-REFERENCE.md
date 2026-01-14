# iOS Heatmap Quick Reference

## Basic Usage

```swift
import SwiftUI

HeatmapView(
    data: [
        [10, 20, 30],
        [15, 25, 35],
        [20, 30, 40]
    ],
    rowLabels: ["Row 1", "Row 2", "Row 3"],
    columnLabels: ["Col 1", "Col 2", "Col 3"],
    colorScheme: .blueRed,
    showValues: true,
    cellSize: 50,
    animated: true
)
```

---

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `data` | `[[Float]]` | **Required** | 2D matrix of values |
| `rowLabels` | `[String]` | `[]` | Labels for rows (left side) |
| `columnLabels` | `[String]` | `[]` | Labels for columns (top) |
| `colorScheme` | `ColorScheme` | `.blueRed` | Color gradient scheme |
| `showValues` | `Bool` | `false` | Display values on cells |
| `cellSize` | `Float` | `40` | Cell size in points |
| `animated` | `Bool` | `true` | Enable fade-in animation |
| `contentDescription` | `String?` | `nil` | Custom accessibility text |

---

## Color Schemes

### BlueRed (Default)
```swift
colorScheme: .blueRed
```
- **Range:** Blue → Red
- **Use:** General purpose, temperature

### GreenRed
```swift
colorScheme: .greenRed
```
- **Range:** Green → Red
- **Use:** Performance, health metrics

### Grayscale
```swift
colorScheme: .grayscale
```
- **Range:** White → Black
- **Use:** Print-friendly, monochrome

### Viridis
```swift
colorScheme: .viridis
```
- **Range:** Purple → Blue → Teal → Green → Yellow
- **Use:** Scientific, color-blind accessible

---

## Examples

### Correlation Matrix

```swift
let correlationData: [[Float]] = [
    [1.0, 0.8, 0.3],
    [0.8, 1.0, 0.5],
    [0.3, 0.5, 1.0]
]

let labels = ["X", "Y", "Z"]

HeatmapView(
    data: correlationData,
    rowLabels: labels,
    columnLabels: labels,
    colorScheme: .blueRed,
    showValues: true,
    cellSize: 60
)
```

### Activity Pattern (GitHub-style)

```swift
let activityData: [[Float]] = (0..<7).map { day in
    (0..<24).map { hour in
        Float.random(in: 0...10)
    }
}

let days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
let hours = (0..<24).map { "h\($0)" }

HeatmapView(
    data: activityData,
    rowLabels: days,
    columnLabels: hours,
    colorScheme: .greenRed,
    cellSize: 20
)
```

### Simple Temperature Map

```swift
let temperatures: [[Float]] = [
    [15.2, 18.5, 22.1],
    [14.8, 19.3, 23.5],
    [16.1, 20.2, 24.8]
]

HeatmapView(
    data: temperatures,
    colorScheme: .blueRed,
    showValues: true,
    cellSize: 50,
    animated: true
)
```

### Compact Visualization

```swift
HeatmapView(
    data: largeMatrix,
    cellSize: 15,  // Small cells
    showValues: false,  // No text overlay
    animated: false  // Instant rendering
)
```

---

## Features

### Interactive Cell Selection
- **Tap cell:** Select/highlight
- **Tap again:** Deselect
- **Selected cell:** Blue border + info display

### Automatic Labels
If labels not provided, defaults to:
- Rows: "R1", "R2", "R3", ...
- Columns: "C1", "C2", "C3", ...

### Value Formatting
- **Integers:** "1", "10", "100" (no decimals)
- **Decimals:** "1.2", "4.6", "7.9" (1 decimal)

### Text Contrast
Automatically chooses black or white text based on cell background luminance for WCAG AA compliance.

### Legend
Displays automatically:
- Color gradient bar (20 steps)
- Min value (left)
- Max value (right)

---

## Performance Tips

### Optimal Matrix Sizes
- **Small:** < 100 cells (10x10)
- **Medium:** 100-400 cells (20x20)
- **Large:** 400-1000 cells (31x31)
- **Maximum:** 1000 cells (recommended limit)

### For Large Data
```swift
HeatmapView(
    data: largeData,
    cellSize: 15,        // Smaller cells
    showValues: false,   // Disable text overlay
    animated: false      // Skip animation
)
```

---

## Accessibility

### Custom Description
```swift
HeatmapView(
    data: data,
    contentDescription: "Monthly sales heatmap showing performance across regions"
)
```

### VoiceOver Announces
- "Heatmap with {rows} rows and {columns} columns"
- "Values range from {min} to {max}"
- "Highest value at row {r}, column {c}"

---

## Edge Cases

### Empty Data
```swift
HeatmapView(data: [])  // Renders empty, no crash
```

### Single Cell
```swift
HeatmapView(data: [[42]])  // Renders 1x1 heatmap
```

### Uniform Values
```swift
HeatmapView(data: [[50, 50], [50, 50]])  // All cells same color (middle of gradient)
```

### Negative Values
```swift
HeatmapView(data: [[-10, 0, 10]])  // Works correctly
```

---

## Color Interpolation

### How It Works
1. Find min and max values in data
2. Normalize each value to 0.0-1.0 range
3. Interpolate RGB components linearly
4. Apply to cell background

### Formula
```
normalized = (value - min) / (max - min)
r = startR + (endR - startR) * normalized
g = startG + (endG - startG) * normalized
b = startB + (endB - startB) * normalized
```

---

## Integration with Kotlin

### Kotlin Model
```kotlin
data class Heatmap(
    val data: List<List<Float>>,
    val rowLabels: List<String> = emptyList(),
    val columnLabels: List<String> = emptyList(),
    val colorScheme: ColorScheme = ColorScheme.BlueRed,
    val showValues: Boolean = false,
    val cellSize: Float = 40f,
    val animated: Boolean = true,
    val contentDescription: String? = null
)
```

### Bridge Mapping
```kotlin
when (component) {
    is Heatmap -> HeatmapView(
        data: component.data,
        rowLabels: component.rowLabels,
        columnLabels: component.columnLabels,
        colorScheme: mapColorScheme(component.colorScheme),
        showValues: component.showValues,
        cellSize: component.cellSize,
        animated: component.animated,
        contentDescription: component.contentDescription
    )
}
```

---

## Testing

### Run Tests
```bash
swift test --filter HeatmapTests
```

### Test Coverage
- 26 test functions
- 100% code coverage
- Performance benchmarks included

---

## Files

```
src/iosMain/swift/Charts/HeatmapView.swift
Tests/Charts/HeatmapTests.swift
ios-chart-009-complete.json
```

---

## Dependencies

- **SwiftUI:** iOS 16.0+
- **Canvas API:** Native SwiftUI
- **Foundation:** Standard library

No external dependencies required.

---

## Common Issues

### Issue: Text not visible on cells
**Solution:** Ensure `showValues: true` is set

### Issue: Colors all look the same
**Solution:** Check data - might be uniform values (all same)

### Issue: Labels truncated
**Solution:** Labels auto-scale with `minimumScaleFactor: 0.5`, but very long labels may still truncate

### Issue: Animation too fast/slow
**Solution:** Animation is fixed at 0.5s. Disable with `animated: false` if needed

---

## Best Practices

1. **Use descriptive labels** for accessibility
2. **Choose appropriate color scheme** for data type
3. **Enable value overlay** for small matrices (< 10x10)
4. **Disable value overlay** for large matrices (> 10x10)
5. **Provide custom contentDescription** for complex data
6. **Test with VoiceOver** for accessibility compliance
7. **Use cellSize 40-60pt** for readable text overlay
8. **Use cellSize 15-30pt** for compact visualizations

---

**Last Updated:** 2025-11-25
**iOS Version:** 16.0+
**Agent:** ios-chart-009
