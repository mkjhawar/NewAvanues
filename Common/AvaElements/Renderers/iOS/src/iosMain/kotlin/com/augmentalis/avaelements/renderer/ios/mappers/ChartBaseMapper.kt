package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.charts.*

/**
 * iOS SwiftUI Chart Mappers - Flutter Material Parity
 *
 * Maps cross-platform chart components to iOS SwiftUI bridge representations.
 * Uses SwiftUI Charts framework on iOS 16+ with fallback to custom views.
 *
 * Supported Chart Types:
 * - LineChart: Line charts with multiple series
 * - BarChart: Grouped/stacked bar charts
 * - PieChart: Pie/donut charts
 * - AreaChart: Area charts (filled line charts)
 * - ScatterChart: Scatter plots
 * - RadarChart: Radar/spider charts
 * - Sparkline: Minimal inline charts
 * - Gauge: Circular/linear gauges
 * - Heatmap: Grid-based heat maps
 * - TreeMap: Hierarchical rectangular charts
 *
 * @since 3.1.0-android-parity
 */

/**
 * Maps LineChart to SwiftUI Charts view
 */
object LineChartMapper {
    fun map(
        component: LineChart,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Title if present
        val chartTitle = component.title
        if (chartTitle != null) {
            children.add(
                SwiftUIView.text(
                    content = chartTitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title2),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                    )
                )
            )
        }

        // Chart content
        val chartView = SwiftUIView(
            type = ViewType.Custom("Chart"),
            id = component.id,
            properties = buildMap {
                put("chartType", "line")
                put("series", component.series.map { series ->
                    mapOf(
                        "label" to series.label,
                        "color" to series.getEffectiveColor(),
                        "strokeWidth" to (series.strokeWidth ?: component.lineWidth),
                        "fillArea" to series.fillArea,
                        "dashed" to series.dashed,
                        "data" to series.data.map { point ->
                            mapOf("x" to point.x, "y" to point.y, "label" to (point.label ?: ""))
                        }
                    )
                })
                put("showLegend", component.showLegend)
                put("showGrid", component.showGrid)
                put("showPoints", component.showPoints)
                put("pointSize", component.pointSize)
                put("animated", component.animated)
                put("animationDuration", component.animationDuration)
                component.xAxisLabel?.let { put("xAxisLabel", it) }
                component.yAxisLabel?.let { put("yAxisLabel", it) }
                component.minY?.let { put("minY", it) }
                component.maxY?.let { put("maxY", it) }
            },
            modifiers = buildList {
                val height = component.height ?: LineChart.DEFAULT_HEIGHT
                add(SwiftUIModifier.frame(
                    width = SizeValue.Infinity,
                    height = SizeValue.Fixed(height)
                ))
                addAll(ModifierConverter.convert(component.modifiers, theme))
            }
        )

        children.add(chartView)

        // Legend if enabled
        if (component.showLegend && component.series.isNotEmpty()) {
            children.add(buildLegend(component.series.map { it.label to it.getEffectiveColor() }))
        }

        return SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )
    }
}

/**
 * Maps BarChart to SwiftUI Charts view
 */
object BarChartMapper {
    fun map(
        component: BarChart,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Title if present
        val chartTitle = component.title
        if (chartTitle != null) {
            children.add(
                SwiftUIView.text(
                    content = chartTitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title2),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                    )
                )
            )
        }

        // Chart content
        val chartView = SwiftUIView(
            type = ViewType.Custom("Chart"),
            id = component.id,
            properties = buildMap {
                put("chartType", "bar")
                put("mode", component.mode.name.lowercase())
                put("orientation", component.orientation.name.lowercase())
                put("data", component.data.map { group ->
                    mapOf(
                        "label" to group.label,
                        "bars" to group.bars.map { bar ->
                            mapOf(
                                "value" to bar.value,
                                "label" to (bar.label ?: ""),
                                "color" to bar.getEffectiveColor()
                            )
                        }
                    )
                })
                put("showLegend", component.showLegend)
                put("showGrid", component.showGrid)
                put("showValues", component.showValues)
                put("barWidth", component.barWidth)
                put("groupSpacing", component.groupSpacing)
                put("barSpacing", component.barSpacing)
                put("animated", component.animated)
                put("animationDuration", component.animationDuration)
                component.xAxisLabel?.let { put("xAxisLabel", it) }
                component.yAxisLabel?.let { put("yAxisLabel", it) }
                component.minValue?.let { put("minValue", it) }
                component.maxValue?.let { put("maxValue", it) }
            },
            modifiers = buildList {
                val height = component.height ?: BarChart.DEFAULT_HEIGHT
                add(SwiftUIModifier.frame(
                    width = SizeValue.Infinity,
                    height = SizeValue.Fixed(height)
                ))
                addAll(ModifierConverter.convert(component.modifiers, theme))
            }
        )

        children.add(chartView)

        // Legend if enabled
        if (component.showLegend) {
            val legendItems = component.getBarLabels().mapIndexed { index, label ->
                val defaultColors = listOf("#2196F3", "#4CAF50", "#FF9800", "#F44336", "#9C27B0")
                label to defaultColors[index % defaultColors.size]
            }
            if (legendItems.isNotEmpty()) {
                children.add(buildLegend(legendItems))
            }
        }

        return SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )
    }
}

/**
 * Maps PieChart to SwiftUI Charts view
 */
object PieChartMapper {
    fun map(
        component: PieChart,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Title if present
        val chartTitle = component.title
        if (chartTitle != null) {
            children.add(
                SwiftUIView.text(
                    content = chartTitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title2),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                    )
                )
            )
        }

        // Chart content
        val chartView = SwiftUIView(
            type = ViewType.Custom("Chart"),
            id = component.id,
            properties = buildMap {
                put("chartType", "pie")
                put("donutMode", component.donutMode)
                put("donutRatio", component.donutRatio)
                put("showLabels", component.showLabels)
                put("showPercentages", component.showPercentages)
                put("startAngle", component.startAngle)
                put("strokeWidth", component.strokeWidth)
                put("slices", component.slices.map { slice ->
                    mapOf(
                        "value" to slice.value,
                        "label" to slice.label,
                        "color" to slice.getEffectiveColor()
                    )
                })
                put("animated", component.animated)
                put("animationDuration", component.animationDuration)
            },
            modifiers = buildList {
                add(SwiftUIModifier.frame(
                    width = SizeValue.Fixed(component.size),
                    height = SizeValue.Fixed(component.size)
                ))
                addAll(ModifierConverter.convert(component.modifiers, theme))
            }
        )

        children.add(chartView)

        // Legend if enabled
        if (component.showLegend && component.slices.isNotEmpty()) {
            children.add(buildLegend(component.slices.map { it.label to it.getEffectiveColor() }))
        }

        return SwiftUIView.vStack(
            spacing = 16f,
            alignment = HorizontalAlignment.Center,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )
    }
}

/**
 * Maps AreaChart to SwiftUI (uses LineChart with fillArea=true)
 */
object AreaChartMapper {
    fun map(
        component: AreaChart,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val chartTitle = component.title
        if (chartTitle != null) {
            children.add(
                SwiftUIView.text(
                    content = chartTitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title2),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                    )
                )
            )
        }

        val chartView = SwiftUIView(
            type = ViewType.Custom("Chart"),
            id = component.id,
            properties = buildMap {
                put("chartType", "area")
                put("series", component.series.map { series ->
                    mapOf(
                        "label" to series.label,
                        "color" to series.getEffectiveColor(),
                        "fillOpacity" to series.fillOpacity,
                        "gradient" to series.gradient,
                        "data" to series.data.map { point ->
                            mapOf("x" to point.x, "y" to point.y)
                        }
                    )
                })
                put("showLegend", component.showLegend)
                put("showGrid", component.showGrid)
                put("animated", component.animated)
                put("animationDuration", component.animationDuration)
                put("stacked", component.stacked)
            },
            modifiers = buildList {
                val height = component.height ?: AreaChart.DEFAULT_HEIGHT
                add(SwiftUIModifier.frame(
                    width = SizeValue.Infinity,
                    height = SizeValue.Fixed(height)
                ))
                addAll(ModifierConverter.convert(component.modifiers, theme))
            }
        )

        children.add(chartView)

        if (component.showLegend && component.series.isNotEmpty()) {
            children.add(buildLegend(component.series.map { it.label to it.getEffectiveColor() }))
        }

        return SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )
    }
}

/**
 * Maps ScatterChart to SwiftUI
 */
object ScatterChartMapper {
    fun map(
        component: ScatterChart,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val chartTitle = component.title
        if (chartTitle != null) {
            children.add(
                SwiftUIView.text(
                    content = chartTitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title2),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                    )
                )
            )
        }

        val chartView = SwiftUIView(
            type = ViewType.Custom("Chart"),
            id = component.id,
            properties = buildMap {
                put("chartType", "scatter")
                put("series", component.series.map { series ->
                    mapOf(
                        "label" to series.label,
                        "color" to series.getEffectiveColor(),
                        "points" to series.points.map { point ->
                            mapOf("x" to point.x, "y" to point.y, "size" to point.size)
                        }
                    )
                })
                put("showLegend", component.showLegend)
                put("showGrid", component.showGrid)
                put("showTrendLine", component.showTrendLine)
                put("pointSize", component.pointSize)
                put("animated", component.animated)
            },
            modifiers = buildList {
                val height = component.height ?: ScatterChart.DEFAULT_HEIGHT
                add(SwiftUIModifier.frame(
                    width = SizeValue.Infinity,
                    height = SizeValue.Fixed(height)
                ))
                addAll(ModifierConverter.convert(component.modifiers, theme))
            }
        )

        children.add(chartView)

        if (component.showLegend && component.series.isNotEmpty()) {
            children.add(buildLegend(component.series.map { it.label to it.getEffectiveColor() }))
        }

        return SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )
    }
}

/**
 * Maps RadarChart to SwiftUI
 */
object RadarChartMapper {
    fun map(
        component: RadarChart,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val chartTitle = component.title
        if (chartTitle != null) {
            children.add(
                SwiftUIView.text(
                    content = chartTitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title2),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                    )
                )
            )
        }

        val chartView = SwiftUIView(
            type = ViewType.Custom("Chart"),
            id = component.id,
            properties = buildMap {
                put("chartType", "radar")
                put("axes", component.axes)
                put("series", component.series.map { series ->
                    mapOf(
                        "label" to series.label,
                        "color" to series.getEffectiveColor(),
                        "values" to series.values
                    )
                })
                put("showLegend", component.showLegend)
                put("showGrid", component.showGrid)
                put("gridLevels", component.gridLevels)
                put("fillOpacity", component.fillOpacity)
                put("strokeWidth", component.strokeWidth)
                put("animated", component.animated)
            },
            modifiers = buildList {
                val size = component.size
                add(SwiftUIModifier.frame(
                    width = SizeValue.Fixed(size),
                    height = SizeValue.Fixed(size)
                ))
                addAll(ModifierConverter.convert(component.modifiers, theme))
            }
        )

        children.add(chartView)

        if (component.showLegend && component.series.isNotEmpty()) {
            children.add(buildLegend(component.series.map { it.label to it.getEffectiveColor() }))
        }

        return SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Center,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )
    }
}

/**
 * Maps Sparkline to SwiftUI (minimal inline chart)
 */
object SparklineMapper {
    fun map(
        component: Sparkline,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Custom("Sparkline"),
            id = component.id,
            properties = buildMap {
                put("data", component.data)
                put("color", component.color)
                put("lineWidth", component.lineWidth)
                put("showArea", component.showArea)
                put("areaOpacity", component.areaOpacity)
                put("showPoints", component.showPoints)
                put("pointSize", component.pointSize)
                put("highlightMin", component.highlightMin)
                put("highlightMax", component.highlightMax)
                put("showTrend", component.showTrend)
                put("animated", component.animated)
            },
            modifiers = buildList {
                add(SwiftUIModifier.frame(
                    width = SizeValue.Fixed(component.width),
                    height = SizeValue.Fixed(component.height)
                ))
                addAll(ModifierConverter.convert(component.modifiers, theme))
            }
        )
    }
}

/**
 * Maps Gauge to SwiftUI
 */
object GaugeMapper {
    fun map(
        component: Gauge,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val gaugeLabel = component.label
        if (gaugeLabel != null) {
            children.add(
                SwiftUIView.text(
                    content = gaugeLabel,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Headline),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                    )
                )
            )
        }

        val gaugeView = SwiftUIView(
            type = ViewType.Custom("Gauge"),
            id = component.id,
            properties = buildMap {
                put("value", component.value)
                put("min", component.min)
                put("max", component.max)
                put("startAngle", component.startAngle)
                put("sweepAngle", component.sweepAngle)
                put("thickness", component.thickness)
                put("showValue", component.showValue)
                component.valueFormat?.let { put("valueFormat", it) }
                component.unit?.let { put("unit", it) }
                put("segments", component.segments.map { segment ->
                    mapOf(
                        "start" to segment.start,
                        "end" to segment.end,
                        "color" to segment.color,
                        "label" to (segment.label ?: "")
                    )
                })
                put("animated", component.animated)
            },
            modifiers = buildList {
                add(SwiftUIModifier.frame(
                    width = SizeValue.Fixed(component.size),
                    height = SizeValue.Fixed(component.size)
                ))
                addAll(ModifierConverter.convert(component.modifiers, theme))
            }
        )

        children.add(gaugeView)

        return SwiftUIView.vStack(
            spacing = 8f,
            alignment = HorizontalAlignment.Center,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(12f))
        )
    }
}

/**
 * Maps Heatmap to SwiftUI
 */
object HeatmapMapper {
    fun map(
        component: Heatmap,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val chartTitle = component.title
        if (chartTitle != null) {
            children.add(
                SwiftUIView.text(
                    content = chartTitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title2),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                    )
                )
            )
        }

        val chartView = SwiftUIView(
            type = ViewType.Custom("Heatmap"),
            id = component.id,
            properties = buildMap {
                put("data", component.data)
                put("rowLabels", component.rowLabels)
                put("columnLabels", component.columnLabels)
                put("colorScheme", component.colorScheme.name)
                put("showValues", component.showValues)
                put("showGrid", component.showGrid)
                put("cellSize", component.cellSize)
                component.minValue?.let { put("minValue", it) }
                component.maxValue?.let { put("maxValue", it) }
                put("animated", component.animated)
            },
            modifiers = ModifierConverter.convert(component.modifiers, theme)
        )

        children.add(chartView)

        return SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )
    }
}

/**
 * Maps TreeMap to SwiftUI
 */
object TreeMapMapper {
    fun map(
        component: TreeMap,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        val chartTitle = component.title
        if (chartTitle != null) {
            children.add(
                SwiftUIView.text(
                    content = chartTitle,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Title2),
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.text)
                    )
                )
            )
        }

        val chartView = SwiftUIView(
            type = ViewType.Custom("TreeMap"),
            id = component.id,
            properties = buildMap {
                put("items", component.items.map { item ->
                    mapOf(
                        "label" to item.label,
                        "value" to item.value,
                        "color" to item.getEffectiveColor(),
                        "category" to (item.category ?: "")
                    )
                })
                put("showLabels", component.showLabels)
                put("showValues", component.showValues)
                put("minArea", component.minArea)
                put("animated", component.animated)
            },
            modifiers = buildList {
                add(SwiftUIModifier.frame(
                    width = SizeValue.Infinity,
                    height = SizeValue.Fixed(component.height)
                ))
                addAll(ModifierConverter.convert(component.modifiers, theme))
            }
        )

        children.add(chartView)

        return SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Leading,
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )
    }
}

// Note: KanbanMapper is defined in KanbanMappers.kt

/**
 * Helper function to build legend view
 */
private fun buildLegend(items: List<Pair<String, String>>): SwiftUIView {
    val legendItems = items.map { (label, color) ->
        SwiftUIView.hStack(
            spacing = 6f,
            alignment = VerticalAlignment.Center,
            children = listOf(
                SwiftUIView(
                    type = ViewType.Circle,
                    properties = emptyMap(),
                    modifiers = listOf(
                        SwiftUIModifier.frame(
                            width = SizeValue.Fixed(10f),
                            height = SizeValue.Fixed(10f)
                        ),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.hex(color))
                    )
                ),
                SwiftUIView.text(
                    content = label,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.secondaryText)
                    )
                )
            )
        )
    }

    return SwiftUIView.hStack(
        spacing = 16f,
        alignment = VerticalAlignment.Center,
        children = legendItems,
        modifiers = listOf(SwiftUIModifier.padding(8f, 0f, 8f, 0f))
    )
}
