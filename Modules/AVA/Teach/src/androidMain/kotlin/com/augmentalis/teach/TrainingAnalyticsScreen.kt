package com.augmentalis.teach
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Phase 1.1: Training Analytics Screen
 *
 * Displays comprehensive analytics dashboard with:
 * - Total statistics
 * - Intent distribution pie chart
 * - Coverage metrics
 * - Quality metrics
 * - Trend analysis
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingAnalyticsScreen(
    analytics: TrainingAnalytics,
    intentDistribution: List<IntentDistribution>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Section
            item {
                OverviewCard(analytics)
            }

            // Intent Distribution Pie Chart
            item {
                IntentDistributionCard(intentDistribution)
            }

            // Coverage Metrics
            item {
                CoverageMetricsCard(analytics.coverageMetrics, analytics.examplesPerIntent)
            }

            // Quality Metrics
            item {
                QualityMetricsCard(analytics.qualityMetrics)
            }

            // Trend Metrics
            item {
                TrendMetricsCard(analytics.trendMetrics)
            }

            // Source Distribution
            item {
                SourceDistributionCard(analytics.sourceDistribution, analytics.totalExamples)
            }
        }
    }
}

@Composable
private fun OverviewCard(analytics: TrainingAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total Examples", analytics.totalExamples.toString())
                StatItem("Intents", analytics.totalIntents.toString())
                StatItem("Locales", analytics.totalLocales.toString())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    "Avg per Intent",
                    analytics.averageExamplesPerIntent.toOneDecimal()
                )
                StatItem(
                    "Coverage Score",
                    "${analytics.coverageMetrics.coverageScore.toInt()}/100"
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IntentDistributionCard(distribution: List<IntentDistribution>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Intent Distribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Divider()

            if (distribution.isNotEmpty()) {
                // Pie Chart
                PieChart(
                    data = distribution.take(10), // Show top 10
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                distribution.take(10).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .padding(2.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = getIntentColor(distribution.indexOf(item))
                                    )
                                }
                            }
                            Text(
                                item.intent,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            "${item.count} (${item.percentage.toOneDecimal()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PieChart(
    data: List<IntentDistribution>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = min(canvasWidth, canvasHeight) / 2.5f
        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

        var startAngle = -90f // Start from top
        val total = data.sumOf { it.count }

        data.forEachIndexed { index, item ->
            val sweepAngle = (item.count.toFloat() / total) * 360f

            drawArc(
                color = getIntentColor(index),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            startAngle += sweepAngle
        }

        // Draw white circle in center for donut effect
        drawCircle(
            color = Color.White,
            radius = radius * 0.6f,
            center = center
        )
    }
}

@Composable
private fun CoverageMetricsCard(
    coverage: CoverageMetrics,
    examplesPerIntent: Map<String, Int>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Coverage Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CoverageItem(
                    label = "Well Covered",
                    count = coverage.wellCoveredIntents,
                    color = Color(0xFF4CAF50),
                    description = "≥10 examples"
                )
                CoverageItem(
                    label = "Adequate",
                    count = coverage.adequatelyCoveredIntents,
                    color = Color(0xFFFFC107),
                    description = "5-9 examples"
                )
                CoverageItem(
                    label = "Poor",
                    count = coverage.poorlyCoveredIntents,
                    color = Color(0xFFF44336),
                    description = "1-4 examples"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Coverage Score Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Overall Coverage Score")
                    Text(
                        "${coverage.coverageScore.toInt()}/100",
                        fontWeight = FontWeight.Bold,
                        color = getCoverageScoreColor(coverage.coverageScore)
                    )
                }
                LinearProgressIndicator(
                    progress = (coverage.coverageScore / 100).toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    color = getCoverageScoreColor(coverage.coverageScore),
                )
            }
        }
    }
}

@Composable
private fun CoverageItem(
    label: String,
    count: Int,
    color: Color,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QualityMetricsCard(quality: QualityMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Quality Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Divider()

            // Usage Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Avg Usage", quality.averageUsageCount.toOneDecimal())
                StatItem(
                    "Unused",
                    "${quality.unusedExamplesCount} (${quality.unusedExamplesPercentage.toInt()}%)"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Most Used Intents
            Text(
                "Most Used Intents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            quality.mostUsedIntents.forEach { (intent, usage) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(intent, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        usage.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Least Used Intents
            Text(
                "Least Used Intents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            quality.leastUsedIntents.forEach { (intent, usage) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(intent, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        usage.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (usage == 0) Color(0xFFF44336)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendMetricsCard(trends: TrendMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Growth Trends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Last 7 Days", trends.examplesAddedLast7Days.toString())
                StatItem("Last 30 Days", trends.examplesAddedLast30Days.toString())
                StatItem("Per Day", trends.growthRate.toOneDecimal())
            }

            if (trends.sourcesTrend.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Recent Sources (30 days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                trends.sourcesTrend.forEach { (source, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(source.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            count.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceDistributionCard(
    sourceDistribution: Map<TrainExampleSource, Int>,
    totalExamples: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Source Distribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Divider()

            sourceDistribution.forEach { (source, count) ->
                val percentage = if (totalExamples > 0) {
                    (count.toDouble() / totalExamples) * 100
                } else 0.0

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(source.name)
                        Text("$count (${percentage.toInt()}%)")
                    }
                    LinearProgressIndicator(
                        progress = (count.toFloat() / totalExamples),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * Get color for intent based on index
 */
private fun getIntentColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFFC107), // Amber
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF9800), // Orange
        Color(0xFF607D8B), // Blue Grey
        Color(0xFFE91E63), // Pink
        Color(0xFF3F51B5), // Indigo
    )
    return colors[index % colors.size]
}

/**
 * Get color for coverage score
 */
private fun getCoverageScoreColor(score: Double): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}
