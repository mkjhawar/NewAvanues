package com.augmentalis.avaelements.renderer.android.charts

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.avaelements.flutter.material.charts.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for chart components
 *
 * Tests cover:
 * - Component rendering
 * - Data validation
 * - Accessibility
 * - Animations
 * - Click handlers
 * - Performance (large datasets)
 *
 * Target: 90%+ test coverage (60+ tests)
 *
 * @since 3.0.0-flutter-parity
 */
@RunWith(AndroidJUnit4::class)
class ChartComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===================
    // LineChart Tests (6)
    // ===================

    @Test
    fun lineChart_rendersCorrectly() {
        val component = LineChart(
            series = listOf(
                LineChart.ChartSeries(
                    label = "Series 1",
                    data = listOf(
                        ChartPoint(0f, 10f),
                        ChartPoint(1f, 20f),
                        ChartPoint(2f, 15f)
                    ),
                    color = "#2196F3"
                )
            ),
            title = "Test Line Chart"
        )

        composeTestRule.setContent {
            LineChartMapper(component)
        }

        // Verify title is rendered
        composeTestRule.onNodeWithText("Test Line Chart").assertIsDisplayed()
    }

    @Test
    fun lineChart_displaysMultipleSeries() {
        val component = LineChart(
            series = listOf(
                LineChart.ChartSeries("Series 1", listOf(ChartPoint(0f, 10f))),
                LineChart.ChartSeries("Series 2", listOf(ChartPoint(0f, 20f)))
            ),
            showLegend = true
        )

        composeTestRule.setContent {
            LineChartMapper(component)
        }

        // Verify legend items
        composeTestRule.onNodeWithText("Series 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Series 2").assertIsDisplayed()
    }

    @Test
    fun lineChart_validatesData() {
        val validChart = LineChart(
            series = listOf(
                LineChart.ChartSeries("Test", listOf(ChartPoint(0f, 10f)))
            )
        )
        assertTrue(validChart.isValid())

        val invalidChart = LineChart(series = emptyList())
        assertTrue(!invalidChart.isValid())
    }

    @Test
    fun lineChart_calculatesYRange() {
        val component = LineChart(
            series = listOf(
                LineChart.ChartSeries(
                    "Test",
                    listOf(
                        ChartPoint(0f, 10f),
                        ChartPoint(1f, 50f),
                        ChartPoint(2f, 30f)
                    )
                )
            )
        )

        val (min, max) = component.getYRange()
        assertEquals(10f, min)
        assertEquals(50f, max)
    }

    @Test
    fun lineChart_hasAccessibilityDescription() {
        val component = LineChart(
            series = listOf(
                LineChart.ChartSeries("Test", listOf(ChartPoint(0f, 10f)))
            ),
            title = "Test Chart"
        )

        composeTestRule.setContent {
            LineChartMapper(component)
        }

        // Verify accessibility description exists
        val description = component.getAccessibilityDescription()
        assertTrue(description.contains("Line chart"))
    }

    @Test
    fun lineChart_handlesAnimation() {
        val component = LineChart(
            series = listOf(
                LineChart.ChartSeries("Test", listOf(ChartPoint(0f, 10f)))
            ),
            animated = true,
            animationDuration = 500
        )

        composeTestRule.setContent {
            LineChartMapper(component)
        }

        // Chart should render (animation is internal to Vico)
        composeTestRule.waitForIdle()
    }

    // ===================
    // BarChart Tests (6)
    // ===================

    @Test
    fun barChart_rendersCorrectly() {
        val component = BarChart(
            data = listOf(
                BarChart.BarGroup(
                    label = "Q1",
                    bars = listOf(BarChart.Bar(value = 100f, label = "Sales"))
                )
            ),
            title = "Test Bar Chart"
        )

        composeTestRule.setContent {
            BarChartMapper(component)
        }

        composeTestRule.onNodeWithText("Test Bar Chart").assertIsDisplayed()
    }

    @Test
    fun barChart_supportsGroupedMode() {
        val component = BarChart(
            data = listOf(
                BarChart.BarGroup(
                    "Q1",
                    listOf(
                        BarChart.Bar(100f, "Revenue"),
                        BarChart.Bar(80f, "Costs")
                    )
                )
            ),
            mode = BarChart.BarMode.Grouped
        )

        assertTrue(component.isValid())
        assertEquals(BarChart.BarMode.Grouped, component.mode)
    }

    @Test
    fun barChart_supportsStackedMode() {
        val component = BarChart(
            data = listOf(
                BarChart.BarGroup(
                    "Q1",
                    listOf(
                        BarChart.Bar(100f, "Revenue"),
                        BarChart.Bar(80f, "Costs")
                    )
                )
            ),
            mode = BarChart.BarMode.Stacked
        )

        val group = component.data.first()
        assertEquals(180f, group.getTotalValue())
    }

    @Test
    fun barChart_calculatesValueRange() {
        val component = BarChart(
            data = listOf(
                BarChart.BarGroup("Q1", listOf(BarChart.Bar(100f))),
                BarChart.BarGroup("Q2", listOf(BarChart.Bar(150f))),
                BarChart.BarGroup("Q3", listOf(BarChart.Bar(75f)))
            ),
            mode = BarChart.BarMode.Grouped
        )

        val (min, max) = component.getValueRange()
        assertEquals(0f, min)
        assertEquals(150f, max)
    }

    @Test
    fun barChart_hasAccessibilityDescription() {
        val component = BarChart(
            data = listOf(
                BarChart.BarGroup("Q1", listOf(BarChart.Bar(100f)))
            ),
            title = "Quarterly Sales"
        )

        val description = component.getAccessibilityDescription()
        assertTrue(description.contains("bar chart"))
        assertTrue(description.contains("Quarterly Sales"))
    }

    @Test
    fun barChart_supportsHorizontalOrientation() {
        val component = BarChart(
            data = listOf(
                BarChart.BarGroup("Q1", listOf(BarChart.Bar(100f)))
            ),
            orientation = BarChart.Orientation.Horizontal
        )

        assertEquals(BarChart.Orientation.Horizontal, component.orientation)
    }

    // ===================
    // PieChart Tests (6)
    // ===================

    @Test
    fun pieChart_rendersCorrectly() {
        val component = PieChart(
            slices = listOf(
                PieChart.Slice(30f, "Category A", "#2196F3"),
                PieChart.Slice(45f, "Category B", "#4CAF50"),
                PieChart.Slice(25f, "Category C", "#FF9800")
            ),
            title = "Test Pie Chart"
        )

        composeTestRule.setContent {
            PieChartMapper(component)
        }

        composeTestRule.onNodeWithText("Test Pie Chart").assertIsDisplayed()
    }

    @Test
    fun pieChart_calculatesPercentages() {
        val component = PieChart(
            slices = listOf(
                PieChart.Slice(30f, "A"),
                PieChart.Slice(70f, "B")
            )
        )

        val percentages = component.getPercentages()
        assertEquals(30f, percentages[0])
        assertEquals(70f, percentages[1])
    }

    @Test
    fun pieChart_calculatesSweepAngles() {
        val component = PieChart(
            slices = listOf(
                PieChart.Slice(50f, "A"),
                PieChart.Slice(50f, "B")
            )
        )

        val angles = component.getSweepAngles()
        assertEquals(180f, angles[0])
        assertEquals(180f, angles[1])
    }

    @Test
    fun pieChart_supportsDonutMode() {
        val component = PieChart(
            slices = listOf(PieChart.Slice(100f, "Test")),
            donutMode = true,
            donutRatio = 0.6f
        )

        assertTrue(component.donutMode)
        assertEquals(0.6f, component.donutRatio)
    }

    @Test
    fun pieChart_displaysLegend() {
        val component = PieChart(
            slices = listOf(
                PieChart.Slice(30f, "Category A"),
                PieChart.Slice(70f, "Category B")
            ),
            showLegend = true
        )

        composeTestRule.setContent {
            PieChartMapper(component)
        }

        composeTestRule.onNodeWithText("Category A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category B").assertIsDisplayed()
    }

    @Test
    fun pieChart_hasAccessibilityDescription() {
        val component = PieChart(
            slices = listOf(
                PieChart.Slice(60f, "Apples"),
                PieChart.Slice(40f, "Oranges")
            ),
            title = "Fruit Distribution"
        )

        val description = component.getAccessibilityDescription()
        assertTrue(description.contains("chart"))
        assertTrue(description.contains("Apples"))
        assertTrue(description.contains("60.0%"))
    }

    // ===================
    // Gauge Tests (6)
    // ===================

    @Test
    fun gauge_rendersCorrectly() {
        val component = Gauge(
            value = 75f,
            min = 0f,
            max = 100f,
            label = "CPU Usage",
            unit = "%"
        )

        composeTestRule.setContent {
            GaugeMapper(component)
        }

        composeTestRule.onNodeWithText("CPU Usage").assertIsDisplayed()
    }

    @Test
    fun gauge_calculatesNormalizedValue() {
        val component = Gauge(
            value = 50f,
            min = 0f,
            max = 100f
        )

        assertEquals(0.5f, component.getNormalizedValue())
    }

    @Test
    fun gauge_calculatesSweepAngle() {
        val component = Gauge(
            value = 50f,
            min = 0f,
            max = 100f,
            sweepAngle = 270f
        )

        assertEquals(135f, component.getValueSweepAngle())
    }

    @Test
    fun gauge_supportsSegments() {
        val component = Gauge(
            value = 75f,
            min = 0f,
            max = 100f,
            segments = listOf(
                Gauge.GaugeSegment(0f, 60f, "#4CAF50", "Normal"),
                Gauge.GaugeSegment(60f, 80f, "#FF9800", "Warning"),
                Gauge.GaugeSegment(80f, 100f, "#F44336", "Critical")
            )
        )

        assertEquals("#FF9800", component.getValueColor())
    }

    @Test
    fun gauge_formatsValue() {
        val component = Gauge(
            value = 75.5f,
            unit = "%",
            valueFormat = "%.1f"
        )

        assertEquals("75.5%", component.getFormattedValue())
    }

    @Test
    fun gauge_hasAccessibilityDescription() {
        val component = Gauge(
            value = 75f,
            min = 0f,
            max = 100f,
            label = "Battery",
            unit = "%"
        )

        val description = component.getAccessibilityDescription()
        assertTrue(description.contains("Battery"))
        assertTrue(description.contains("75"))
    }

    // ===================
    // Sparkline Tests (6)
    // ===================

    @Test
    fun sparkline_rendersCorrectly() {
        val component = Sparkline(
            data = listOf(10f, 15f, 12f, 18f, 20f, 17f, 22f),
            color = "#4CAF50"
        )

        composeTestRule.setContent {
            SparklineMapper(component)
        }

        // Sparkline should render without errors
        composeTestRule.waitForIdle()
    }

    @Test
    fun sparkline_calculatesTrend() {
        val upTrend = Sparkline(data = listOf(10f, 15f, 20f))
        assertEquals(Sparkline.Trend.UP, upTrend.getTrend())

        val downTrend = Sparkline(data = listOf(20f, 15f, 10f))
        assertEquals(Sparkline.Trend.DOWN, downTrend.getTrend())

        val flatTrend = Sparkline(data = listOf(10f, 10f, 10f))
        assertEquals(Sparkline.Trend.FLAT, flatTrend.getTrend())
    }

    @Test
    fun sparkline_calculatesPercentageChange() {
        val component = Sparkline(data = listOf(100f, 150f))
        assertEquals(50f, component.getPercentageChange())
    }

    @Test
    fun sparkline_findsMinMax() {
        val component = Sparkline(data = listOf(10f, 50f, 30f, 70f, 20f))

        val (min, minIndex) = component.getMin()!!
        assertEquals(10f, min)
        assertEquals(0, minIndex)

        val (max, maxIndex) = component.getMax()!!
        assertEquals(70f, max)
        assertEquals(3, maxIndex)
    }

    @Test
    fun sparkline_supportsAreaFill() {
        val component = Sparkline(
            data = listOf(10f, 20f, 15f),
            showArea = true,
            areaOpacity = 0.3f
        )

        assertTrue(component.showArea)
        assertEquals(0.3f, component.areaOpacity)
    }

    @Test
    fun sparkline_hasAccessibilityDescription() {
        val component = Sparkline(
            data = listOf(10f, 15f, 12f, 18f, 20f),
            showTrend = true
        )

        val description = component.getAccessibilityDescription()
        assertTrue(description.contains("Sparkline"))
        assertTrue(description.contains("trending"))
    }

    // ===================
    // Kanban Tests (6)
    // ===================

    @Test
    fun kanban_rendersCorrectly() {
        val component = Kanban(
            title = "Sprint Board",
            columns = listOf(
                Kanban.KanbanColumnData(
                    id = "todo",
                    title = "To Do",
                    cards = listOf(
                        Kanban.KanbanCardData(
                            id = "card1",
                            title = "Task 1",
                            priority = Kanban.Priority.High
                        )
                    )
                )
            )
        )

        composeTestRule.setContent {
            KanbanMapper(component)
        }

        composeTestRule.onNodeWithText("Sprint Board").assertIsDisplayed()
        composeTestRule.onNodeWithText("To Do").assertIsDisplayed()
    }

    @Test
    fun kanban_validatesuniqueIds() {
        val validKanban = Kanban(
            columns = listOf(
                Kanban.KanbanColumnData(
                    id = "col1",
                    title = "Column 1",
                    cards = listOf(
                        Kanban.KanbanCardData(id = "card1", title = "Task 1")
                    )
                )
            )
        )
        assertTrue(validKanban.isValid())

        val invalidKanban = Kanban(
            columns = listOf(
                Kanban.KanbanColumnData(
                    id = "col1",
                    title = "Column 1",
                    cards = listOf(
                        Kanban.KanbanCardData(id = "card1", title = "Task 1"),
                        Kanban.KanbanCardData(id = "card1", title = "Task 2") // Duplicate ID
                    )
                )
            )
        )
        assertTrue(!invalidKanban.isValid())
    }

    @Test
    fun kanban_calculatesTotalCards() {
        val component = Kanban(
            columns = listOf(
                Kanban.KanbanColumnData(
                    id = "col1",
                    title = "Column 1",
                    cards = listOf(
                        Kanban.KanbanCardData(id = "card1", title = "Task 1"),
                        Kanban.KanbanCardData(id = "card2", title = "Task 2")
                    )
                ),
                Kanban.KanbanColumnData(
                    id = "col2",
                    title = "Column 2",
                    cards = listOf(
                        Kanban.KanbanCardData(id = "card3", title = "Task 3")
                    )
                )
            )
        )

        assertEquals(3, component.getTotalCards())
    }

    @Test
    fun kanban_checksColumnCapacity() {
        val column = Kanban.KanbanColumnData(
            id = "col1",
            title = "In Progress",
            cards = listOf(
                Kanban.KanbanCardData(id = "card1", title = "Task 1"),
                Kanban.KanbanCardData(id = "card2", title = "Task 2")
            ),
            maxCards = 2
        )

        assertTrue(column.isAtCapacity())
    }

    @Test
    fun kanban_displaysPriorityColors() {
        val card = Kanban.KanbanCardData(
            id = "card1",
            title = "High Priority Task",
            priority = Kanban.Priority.High
        )

        assertEquals("#FF9800", card.getPriorityColor())
    }

    @Test
    fun kanban_hasAccessibilityDescription() {
        val component = Kanban(
            title = "Project Board",
            columns = listOf(
                Kanban.KanbanColumnData(
                    id = "col1",
                    title = "To Do",
                    cards = listOf(
                        Kanban.KanbanCardData(id = "card1", title = "Task 1")
                    )
                )
            )
        )

        val description = component.getAccessibilityDescription()
        assertTrue(description.contains("Kanban board"))
        assertTrue(description.contains("Project Board"))
    }

    // ===================
    // Performance Tests (6)
    // ===================

    @Test
    fun lineChart_handlesLargeDataset() {
        val largeData = (0..1000).map { ChartPoint(it.toFloat(), (it * 2).toFloat()) }
        val component = LineChart(
            series = listOf(
                LineChart.ChartSeries("Large Dataset", largeData)
            )
        )

        assertTrue(component.isValid())
        assertEquals(1001, component.series.first().data.size)
    }

    @Test
    fun barChart_handlesMultipleGroups() {
        val manyGroups = (0..50).map {
            BarChart.BarGroup(
                label = "Group $it",
                bars = listOf(BarChart.Bar(value = it.toFloat() * 10))
            )
        }

        val component = BarChart(data = manyGroups)
        assertTrue(component.isValid())
        assertEquals(51, component.data.size)
    }

    @Test
    fun pieChart_handlesManySlices() {
        val manySlices = (0..10).map {
            PieChart.Slice(value = 10f, label = "Slice $it")
        }

        val component = PieChart(slices = manySlices)
        assertTrue(component.isValid())
        assertEquals(110f, component.getTotalValue())
    }

    @Test
    fun radarChart_handlesMultipleAxes() {
        val axes = (0..8).map { "Axis $it" }
        val values = (0..8).map { it.toFloat() * 10 }

        val component = RadarChart(
            axes = axes,
            series = listOf(
                RadarChart.RadarSeries("Test", values)
            )
        )

        assertTrue(component.isValid())
    }

    @Test
    fun heatmap_handlesLargeMatrix() {
        val largeMatrix = (0..20).map { row ->
            (0..20).map { col ->
                (row + col).toFloat()
            }
        }

        val component = Heatmap(data = largeMatrix)
        assertTrue(component.isValid())
        assertEquals(21, component.data.size)
    }

    @Test
    fun kanban_handlesManyCards() {
        val manyCards = (0..100).map {
            Kanban.KanbanCardData(
                id = "card$it",
                title = "Task $it",
                priority = Kanban.Priority.Medium
            )
        }

        val component = Kanban(
            columns = listOf(
                Kanban.KanbanColumnData(
                    id = "col1",
                    title = "Backlog",
                    cards = manyCards
                )
            )
        )

        assertEquals(101, component.getTotalCards())
    }

    // ===================
    // Edge Case Tests (6)
    // ===================

    @Test
    fun lineChart_handlesEmptyData() {
        val component = LineChart(series = emptyList())
        assertTrue(!component.isValid())
    }

    @Test
    fun barChart_handlesSingleBar() {
        val component = BarChart(
            data = listOf(
                BarChart.BarGroup("Q1", listOf(BarChart.Bar(100f)))
            )
        )

        assertTrue(component.isValid())
        val (min, max) = component.getValueRange()
        assertEquals(0f, min)
        assertEquals(100f, max)
    }

    @Test
    fun pieChart_handlesZeroValues() {
        val component = PieChart(
            slices = listOf(
                PieChart.Slice(0f, "A"),
                PieChart.Slice(100f, "B")
            )
        )

        // Zero values should be handled gracefully
        val percentages = component.getPercentages()
        assertEquals(0f, percentages[0])
        assertEquals(100f, percentages[1])
    }

    @Test
    fun gauge_handlesMinEqualsMax() {
        val component = Gauge(
            value = 50f,
            min = 50f,
            max = 50f
        )

        // Should handle edge case without crashing
        val normalized = component.getNormalizedValue()
        assertTrue(normalized in 0f..1f)
    }

    @Test
    fun sparkline_handlesTwoDataPoints() {
        val component = Sparkline(data = listOf(10f, 20f))
        assertTrue(component.isValid())

        val trend = component.getTrend()
        assertEquals(Sparkline.Trend.UP, trend)
    }

    @Test
    fun kanban_handlesEmptyColumns() {
        val component = Kanban(
            columns = listOf(
                Kanban.KanbanColumnData(
                    id = "col1",
                    title = "Empty Column",
                    cards = emptyList()
                )
            )
        )

        assertTrue(component.isValid())
        assertEquals(0, component.getTotalCards())
    }
}
