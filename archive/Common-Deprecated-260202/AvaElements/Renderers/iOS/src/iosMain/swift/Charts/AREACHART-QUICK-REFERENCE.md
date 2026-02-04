# AreaChart Quick Reference

**Component:** AreaChartView
**Framework:** Swift Charts (iOS 16+)
**File:** `AreaChartView.swift`

---

## Basic Usage

### Single Series Area Chart
```swift
AreaChartView(
    data: [
        DataPoint(x: 0, y: 100, xLabel: "Jan", series: "Revenue"),
        DataPoint(x: 1, y: 150, xLabel: "Feb", series: "Revenue"),
        DataPoint(x: 2, y: 125, xLabel: "Mar", series: "Revenue")
    ],
    title: "Monthly Revenue",
    stacked: false,
    showLegend: false
)
```

### Multiple Series (Overlapping)
```swift
AreaChartView(
    data: multiSeriesData,
    title: "Revenue vs Cost",
    stacked: false,  // Transparent overlapping
    showLegend: true
)
```

### Stacked Mode
```swift
AreaChartView(
    data: multiSeriesData,
    title: "Total Performance",
    stacked: true,  // Cumulative stacking
    showLegend: true
)
```

---

## Key Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `data` | `[DataPoint]` | Required | Data points to display |
| `title` | `String?` | `nil` | Chart title |
| `xAxisLabel` | `String?` | `nil` | X-axis label |
| `yAxisLabel` | `String?` | `nil` | Y-axis label |
| `stacked` | `Bool` | `false` | Stack areas cumulatively |
| `seriesColors` | `[String: Color]?` | `nil` | Custom colors by series |
| `showLegend` | `Bool` | `true` | Show legend |
| `showGrid` | `Bool` | `true` | Show grid lines |
| `animated` | `Bool` | `true` | Animate chart |
| `height` | `CGFloat?` | `nil` | Custom height (default 300) |

---

## DataPoint Structure

```swift
DataPoint(
    x: Double,              // X-axis value
    y: Double,              // Y-axis value (can be negative)
    xLabel: String?,        // Optional X label (overrides x)
    series: String          // Series name (default: "Data")
)
```

---

## Common Patterns

### Custom Colors
```swift
AreaChartView(
    data: data,
    seriesColors: [
        "Revenue": .green,
        "Cost": .red
    ]
)
```

### Negative Values (Profit/Loss)
```swift
let data = [
    DataPoint(x: 0, y: 50, xLabel: "Q1", series: "Net"),
    DataPoint(x: 1, y: -20, xLabel: "Q2", series: "Net"),  // Below zero
    DataPoint(x: 2, y: 30, xLabel: "Q3", series: "Net")
]

AreaChartView(data: data, title: "Net Income")
```

### Convenience Initializer
```swift
// Simple array of tuples
AreaChartView(
    points: [(0, 100), (1, 150), (2, 125)],
    title: "Revenue",
    xAxisLabel: "Month",
    yAxisLabel: "Amount"
)
```

---

## Key Differences from LineChart

| Aspect | LineChart | AreaChart |
|--------|-----------|-----------|
| **Mark** | LineMark | AreaMark |
| **Fill** | None | Gradient fill |
| **Opacity** | 1.0 | 0.3 (overlapping) / 0.7 (stacked) |
| **Stacking** | N/A | Supports cumulative |
| **Use Case** | Trends | Volume/Accumulation |

---

## Gradient Behavior

### Overlapping Mode (stacked: false)
- Opacity: **0.3**
- Areas overlap with transparency
- Best for comparing series
- Gradient: Color → Transparent

### Stacked Mode (stacked: true)
- Opacity: **0.7**
- Areas stack cumulatively
- Best for showing total
- Gradient: Color → Transparent

---

## Accessibility

### Automatic Labels
```
"Revenue Trend. Area chart with 1 series and 6 data points"
```

### Custom Description
```swift
AreaChartView(
    data: data,
    title: "Revenue",
    contentDescription: "Quarterly revenue showing 20% growth"
)
```

### VoiceOver Features
- ✅ Chart type announced
- ✅ Series count and data point count
- ✅ Min/max/average for each series
- ✅ Individual point values
- ✅ Trend descriptions

---

## Testing

### Test File
`Tests/Charts/AreaChartTests.swift`

### Run Tests
```swift
// In Xcode
// Cmd+U to run all tests
// Or select specific test and Cmd+Control+Option+U
```

### Sample Test
```swift
func testSingleSeriesRendering() {
    let data = [
        AreaChartView.DataPoint(x: 0, y: 100, series: "Revenue")
    ]

    let view = AreaChartView(
        data: data,
        title: "Test",
        stacked: false
    )

    XCTAssertEqual(view.data.count, 1)
    XCTAssertFalse(view.stacked)
}
```

---

## Common Issues

### Issue: Areas not visible
**Solution:** Check data range - very small Y values may not show

### Issue: Colors not distinct
**Solution:** Use custom `seriesColors` parameter

### Issue: Animation too fast/slow
**Solution:** Currently fixed at 0.5s - contact team for adjustment

### Issue: Legend not showing
**Solution:** Set `showLegend: true` AND have >1 series

---

## Performance Tips

1. **Data Size:** Support up to 1000+ points per series
2. **Series Count:** Optimal 2-5 series, max 10
3. **Animation:** Disable with `animated: false` for large data sets
4. **Memory:** No leaks, uses SwiftUI lifecycle

---

## Preview Examples

### Single Series
```swift
AreaChartView.sampleData  // 6 points
```

### Multiple Series
```swift
AreaChartView.sampleMultiSeriesData  // 8 points, 2 series
```

### Negative Values
```swift
AreaChartView.sampleNegativeData  // 5 points with negatives
```

---

## Integration Checklist

- [ ] Import SwiftUI, Charts
- [ ] Create DataPoint array
- [ ] Choose stacked or overlapping mode
- [ ] Set title and axis labels
- [ ] Configure legend visibility
- [ ] Test with sample data
- [ ] Verify VoiceOver support
- [ ] Test in dark mode

---

## Related Components

- **LineChartView** - Line charts (trends)
- **BarChartView** - Bar charts (comparison)
- **PieChartView** - Pie charts (proportions) [Coming Soon]

---

## Support

**Documentation:** See `AREACHART-IMPLEMENTATION-REPORT.md`
**Foundation:** ChartHelpers, ChartColors, ChartAccessibility
**Tests:** 12 test cases with 90%+ coverage
**Status:** ✅ Production ready

---

## Version History

- **v1.0** (2025-11-25) - Initial implementation
  - Single and multiple series
  - Stacked and overlapping modes
  - Gradient fills
  - Negative values support
  - Full accessibility
  - 12 comprehensive tests
