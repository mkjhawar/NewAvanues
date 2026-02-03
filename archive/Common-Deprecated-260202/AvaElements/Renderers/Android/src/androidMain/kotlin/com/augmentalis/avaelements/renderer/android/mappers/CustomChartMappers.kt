package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avaelements.flutter.material.charts.*
import kotlin.math.*

/**
 * Android Compose mappers for custom Flutter chart components (Canvas-based)
 *
 * This file contains renderer functions for custom charts that use Canvas rendering:
 * - Gauge
 * - Sparkline
 * - RadarChart
 * - ScatterChart
 * - Heatmap
 * - TreeMap
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render Gauge component using Canvas
 */
@Composable
fun GaugeMapper(component: Gauge) {
    if (!component.isValid()) {
        Text(
            text = "Invalid gauge data",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val animatedValue by animateFloatAsState(
        targetValue = component.value,
        animationSpec = tween(
            durationMillis = if (component.animated) component.animationDuration else 0,
            easing = EaseOutCubic
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(component.size.dp)
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = component.thickness.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Draw background arc
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = component.startAngle,
                sweepAngle = component.sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
            )

            // Draw segments or single arc
            if (component.segments.isNotEmpty()) {
                component.segments.forEach { segment ->
                    val segmentStart = component.startAngle +
                        (component.sweepAngle * (segment.start - component.min) / (component.max - component.min))
                    val segmentSweep =
                        (component.sweepAngle * (segment.end - segment.start) / (component.max - component.min))

                    drawArc(
                        color = parseColor(segment.color),
                        startAngle = segmentStart,
                        sweepAngle = segmentSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                    )
                }
            }

            // Draw value indicator
            val valueSweep = component.getValueSweepAngle()
            val valueColor = component.getValueColor()?.let { parseColor(it) }
                ?: Color(0xFF2196F3)

            drawArc(
                color = valueColor,
                startAngle = component.startAngle,
                sweepAngle = valueSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth + 4.dp.toPx(), cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
            )
        }

        // Center text
        if (component.showValue) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", animatedValue),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                component.unit?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                component.label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Render Sparkline component using Canvas
 */
@Composable
fun SparklineMapper(component: Sparkline) {
    if (!component.isValid()) return

    val (minValue, maxValue) = component.getRange()
    val color = parseColor(component.color)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            contentDescription = component.getAccessibilityDescription()
        }
    ) {
        Canvas(
            modifier = Modifier
                .width(component.width.dp)
                .height(component.height.dp)
        ) {
            if (component.data.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val stepX = width / (component.data.size - 1)

            // Create path for line
            val path = Path()
            val points = component.data.mapIndexed { index, value ->
                val x = index * stepX
                val normalizedValue = if (maxValue > minValue) {
                    (value - minValue) / (maxValue - minValue)
                } else 0.5f
                val y = height - (normalizedValue * height)
                Offset(x, y)
            }

            // Draw area fill if enabled
            if (component.showArea) {
                val areaPath = Path().apply {
                    moveTo(points.first().x, height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, height)
                    close()
                }
                drawPath(
                    path = areaPath,
                    color = color.copy(alpha = component.areaOpacity)
                )
            }

            // Draw line
            path.moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point ->
                path.lineTo(point.x, point.y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = component.lineWidth.dp.toPx())
            )

            // Draw points if enabled
            if (component.showPoints) {
                points.forEach { point ->
                    drawCircle(
                        color = color,
                        radius = component.pointSize.dp.toPx(),
                        center = point
                    )
                }
            }

            // Highlight min/max
            if (component.highlightMin || component.highlightMax) {
                val (min, minIndex) = component.getMin() ?: return@Canvas
                val (max, maxIndex) = component.getMax() ?: return@Canvas

                if (component.highlightMin) {
                    drawCircle(
                        color = Color(0xFFF44336),
                        radius = component.pointSize.dp.toPx() * 1.5f,
                        center = points[minIndex]
                    )
                }

                if (component.highlightMax) {
                    drawCircle(
                        color = Color(0xFF4CAF50),
                        radius = component.pointSize.dp.toPx() * 1.5f,
                        center = points[maxIndex]
                    )
                }
            }
        }

        // Trend indicator
        if (component.showTrend) {
            Spacer(modifier = Modifier.width(8.dp))
            val trend = component.getTrend()
            val trendIcon = when (trend) {
                Sparkline.Trend.UP -> Icons.Default.TrendingUp
                Sparkline.Trend.DOWN -> Icons.Default.TrendingDown
                Sparkline.Trend.FLAT -> Icons.Default.TrendingFlat
            }
            val trendColor = when (trend) {
                Sparkline.Trend.UP -> Color(0xFF4CAF50)
                Sparkline.Trend.DOWN -> Color(0xFFF44336)
                Sparkline.Trend.FLAT -> Color.Gray
            }
            Icon(
                imageVector = trendIcon,
                contentDescription = trend.name,
                tint = trendColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Render RadarChart component using Canvas
 */
@Composable
fun RadarChartMapper(component: RadarChart) {
    if (!component.isValid()) {
        Text(
            text = "Invalid radar chart data",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            component.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Radar Chart
            Canvas(
                modifier = Modifier
                    .size(component.size.dp)
                    .padding(24.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = min(size.width, size.height) / 2
                val axisCount = component.axes.size
                val angleStep = 360f / axisCount

                // Draw grid levels
                if (component.showGrid) {
                    for (level in 1..component.gridLevels) {
                        val levelRadius = radius * (level.toFloat() / component.gridLevels)
                        val levelPath = Path()

                        for (i in 0 until axisCount) {
                            val angle = Math.toRadians((component.getAxisAngle(i)).toDouble())
                            val x = center.x + (levelRadius * cos(angle)).toFloat()
                            val y = center.y + (levelRadius * sin(angle)).toFloat()

                            if (i == 0) {
                                levelPath.moveTo(x, y)
                            } else {
                                levelPath.lineTo(x, y)
                            }
                        }
                        levelPath.close()

                        drawPath(
                            path = levelPath,
                            color = Color.LightGray.copy(alpha = 0.3f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                // Draw axes
                for (i in 0 until axisCount) {
                    val angle = Math.toRadians((component.getAxisAngle(i)).toDouble())
                    val endX = center.x + (radius * cos(angle)).toFloat()
                    val endY = center.y + (radius * sin(angle)).toFloat()

                    drawLine(
                        color = Color.LightGray,
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw data series
                component.series.forEachIndexed { seriesIndex, series ->
                    val seriesColor = parseColor(series.getEffectiveColor("#2196F3"))
                        .copy(alpha = 0.7f)
                    val dataPath = Path()

                    for (i in series.values.indices) {
                        val normalizedValue = (series.values[i] / component.maxValue).coerceIn(0f, 1f)
                        val angle = Math.toRadians((component.getAxisAngle(i)).toDouble())
                        val x = center.x + (radius * normalizedValue * cos(angle)).toFloat()
                        val y = center.y + (radius * normalizedValue * sin(angle)).toFloat()

                        if (i == 0) {
                            dataPath.moveTo(x, y)
                        } else {
                            dataPath.lineTo(x, y)
                        }
                    }
                    dataPath.close()

                    // Fill
                    drawPath(
                        path = dataPath,
                        color = seriesColor.copy(alpha = component.fillOpacity)
                    )

                    // Stroke
                    drawPath(
                        path = dataPath,
                        color = seriesColor,
                        style = Stroke(width = component.strokeWidth.dp.toPx())
                    )
                }
            }

            // Axis labels
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                component.axes.take(5).forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Legend
            if (component.showLegend && component.series.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    component.series.forEach { series ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        parseColor(series.getEffectiveColor("#2196F3")),
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = series.label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render ScatterChart component using Canvas
 */
@Composable
fun ScatterChartMapper(component: ScatterChart) {
    if (!component.isValid()) {
        Text(
            text = "Invalid scatter chart data",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val (minX, maxX) = component.getXRange()
    val (minY, maxY) = component.getYRange()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(component.height?.dp ?: ScatterChart.DEFAULT_HEIGHT.dp)
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            component.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                val chartWidth = size.width - 40.dp.toPx()
                val chartHeight = size.height - 40.dp.toPx()

                // Draw grid if enabled
                if (component.showGrid) {
                    // Vertical lines
                    for (i in 0..4) {
                        val x = 20.dp.toPx() + (chartWidth / 4) * i
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(x, 20.dp.toPx()),
                            end = Offset(x, size.height - 20.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    // Horizontal lines
                    for (i in 0..4) {
                        val y = 20.dp.toPx() + (chartHeight / 4) * i
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(20.dp.toPx(), y),
                            end = Offset(size.width - 20.dp.toPx(), y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // Draw points
                component.series.forEachIndexed { seriesIndex, series ->
                    val seriesColor = parseColor(series.getEffectiveColor("#2196F3"))

                    series.points.forEach { point ->
                        val normalizedX = if (maxX > minX) {
                            (point.x - minX) / (maxX - minX)
                        } else 0.5f
                        val normalizedY = if (maxY > minY) {
                            (point.y - minY) / (maxY - minY)
                        } else 0.5f

                        val x = 20.dp.toPx() + normalizedX * chartWidth
                        val y = size.height - (20.dp.toPx() + normalizedY * chartHeight)
                        val radius = component.pointSize.dp.toPx() * point.size

                        drawCircle(
                            color = seriesColor.copy(alpha = 0.7f),
                            radius = radius,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = seriesColor,
                            radius = radius,
                            center = Offset(x, y),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Legend
            if (component.showLegend && component.series.size > 1) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    component.series.forEach { series ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        parseColor(series.getEffectiveColor("#2196F3")),
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = series.label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
