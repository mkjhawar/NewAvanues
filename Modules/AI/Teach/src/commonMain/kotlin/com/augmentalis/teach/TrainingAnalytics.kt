package com.augmentalis.teach
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import kotlin.math.roundToInt
import kotlinx.datetime.Clock

/**
 * Phase 1.1: Training Analytics
 *
 * Calculates statistics and metrics for training examples.
 * Provides insights into dataset quality, coverage, and improvement trends.
 */

/**
 * Overall training analytics
 */
data class TrainingAnalytics(
    val totalExamples: Int,
    val totalIntents: Int,
    val totalLocales: Int,
    val examplesPerIntent: Map<String, Int>,
    val examplesPerLocale: Map<String, Int>,
    val sourceDistribution: Map<TrainExampleSource, Int>,
    val averageExamplesPerIntent: Double,
    val coverageMetrics: CoverageMetrics,
    val qualityMetrics: QualityMetrics,
    val trendMetrics: TrendMetrics
)

/**
 * Coverage metrics
 */
data class CoverageMetrics(
    val wellCoveredIntents: Int,        // >= 10 examples
    val adequatelyCoveredIntents: Int,   // 5-9 examples
    val poorlyCoveredIntents: Int,       // 1-4 examples
    val coverageScore: Double            // 0-100 score
)

/**
 * Quality metrics
 */
data class QualityMetrics(
    val averageUsageCount: Double,
    val mostUsedIntents: List<Pair<String, Int>>,  // Top 5 intents by usage
    val leastUsedIntents: List<Pair<String, Int>>,  // Bottom 5 intents by usage
    val unusedExamplesCount: Int,       // Examples with usage_count = 0
    val unusedExamplesPercentage: Double
)

/**
 * Trend metrics (time-based analysis)
 */
data class TrendMetrics(
    val examplesAddedLast7Days: Int,
    val examplesAddedLast30Days: Int,
    val growthRate: Double,              // Examples per day (last 30 days)
    val sourcesTrend: Map<TrainExampleSource, Int>  // Sources in last 30 days
)

/**
 * Intent distribution for visualization
 */
data class IntentDistribution(
    val intent: String,
    val count: Int,
    val percentage: Double,
    val usageCount: Int,
    val averageUsage: Double
)

/**
 * Analytics calculator
 */
class TrainingAnalyticsCalculator {

    /**
     * Calculate comprehensive analytics from training examples
     */
    fun calculateAnalytics(examples: List<TrainExample>): TrainingAnalytics {
        if (examples.isEmpty()) {
            return emptyAnalytics()
        }

        val examplesPerIntent = examples.groupBy { it.intent }
            .mapValues { it.value.size }

        val examplesPerLocale = examples.groupBy { it.locale }
            .mapValues { it.value.size }

        val sourceDistribution = examples.groupBy { it.source }
            .mapValues { it.value.size }

        val coverageMetrics = calculateCoverageMetrics(examplesPerIntent)
        val qualityMetrics = calculateQualityMetrics(examples, examplesPerIntent)
        val trendMetrics = calculateTrendMetrics(examples)

        return TrainingAnalytics(
            totalExamples = examples.size,
            totalIntents = examplesPerIntent.keys.size,
            totalLocales = examplesPerLocale.keys.size,
            examplesPerIntent = examplesPerIntent,
            examplesPerLocale = examplesPerLocale,
            sourceDistribution = sourceDistribution,
            averageExamplesPerIntent = examples.size.toDouble() / examplesPerIntent.keys.size,
            coverageMetrics = coverageMetrics,
            qualityMetrics = qualityMetrics,
            trendMetrics = trendMetrics
        )
    }

    /**
     * Calculate intent distribution for pie chart visualization
     */
    fun calculateIntentDistribution(examples: List<TrainExample>): List<IntentDistribution> {
        if (examples.isEmpty()) return emptyList()

        val total = examples.size
        val intentGroups = examples.groupBy { it.intent }

        return intentGroups.map { (intent, examplesForIntent) ->
            val count = examplesForIntent.size
            val percentage = (count.toDouble() / total) * 100
            val totalUsage = examplesForIntent.sumOf { it.usageCount }
            val averageUsage = totalUsage.toDouble() / count

            IntentDistribution(
                intent = intent,
                count = count,
                percentage = percentage,
                usageCount = totalUsage,
                averageUsage = averageUsage
            )
        }.sortedByDescending { it.count }
    }

    /**
     * Calculate coverage metrics
     */
    private fun calculateCoverageMetrics(examplesPerIntent: Map<String, Int>): CoverageMetrics {
        val wellCovered = examplesPerIntent.count { it.value >= 10 }
        val adequatelyCovered = examplesPerIntent.count { it.value in 5..9 }
        val poorlyCovered = examplesPerIntent.count { it.value in 1..4 }

        // Coverage score: weighted average based on coverage categories
        // Well covered: 100 points, Adequately: 60 points, Poorly: 20 points
        val totalIntents = examplesPerIntent.size
        val coverageScore = if (totalIntents > 0) {
            ((wellCovered * 100.0 + adequatelyCovered * 60.0 + poorlyCovered * 20.0) / totalIntents)
        } else {
            0.0
        }

        return CoverageMetrics(
            wellCoveredIntents = wellCovered,
            adequatelyCoveredIntents = adequatelyCovered,
            poorlyCoveredIntents = poorlyCovered,
            coverageScore = coverageScore
        )
    }

    /**
     * Calculate quality metrics
     */
    private fun calculateQualityMetrics(
        examples: List<TrainExample>,
        examplesPerIntent: Map<String, Int>
    ): QualityMetrics {
        // Calculate usage per intent
        val usagePerIntent = examples.groupBy { it.intent }
            .mapValues { (_, examplesForIntent) ->
                examplesForIntent.sumOf { it.usageCount }
            }

        val mostUsed = usagePerIntent.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        val leastUsed = usagePerIntent.entries
            .sortedBy { it.value }
            .take(5)
            .map { it.key to it.value }

        val unusedCount = examples.count { it.usageCount == 0 }
        val unusedPercentage = if (examples.isNotEmpty()) {
            (unusedCount.toDouble() / examples.size) * 100
        } else {
            0.0
        }

        val averageUsage = if (examples.isNotEmpty()) {
            examples.sumOf { it.usageCount }.toDouble() / examples.size
        } else {
            0.0
        }

        return QualityMetrics(
            averageUsageCount = averageUsage,
            mostUsedIntents = mostUsed,
            leastUsedIntents = leastUsed,
            unusedExamplesCount = unusedCount,
            unusedExamplesPercentage = unusedPercentage
        )
    }

    /**
     * Calculate trend metrics
     */
    private fun calculateTrendMetrics(examples: List<TrainExample>): TrendMetrics {
        val now = Clock.System.now().toEpochMilliseconds()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)

        val last7Days = examples.count { it.createdAt >= sevenDaysAgo }
        val last30Days = examples.count { it.createdAt >= thirtyDaysAgo }

        val growthRate = last30Days.toDouble() / 30.0

        val sourcesTrend = examples
            .filter { it.createdAt >= thirtyDaysAgo }
            .groupBy { it.source }
            .mapValues { it.value.size }

        return TrendMetrics(
            examplesAddedLast7Days = last7Days,
            examplesAddedLast30Days = last30Days,
            growthRate = growthRate,
            sourcesTrend = sourcesTrend
        )
    }

    /**
     * Empty analytics for no data
     */
    private fun emptyAnalytics(): TrainingAnalytics {
        return TrainingAnalytics(
            totalExamples = 0,
            totalIntents = 0,
            totalLocales = 0,
            examplesPerIntent = emptyMap(),
            examplesPerLocale = emptyMap(),
            sourceDistribution = emptyMap(),
            averageExamplesPerIntent = 0.0,
            coverageMetrics = CoverageMetrics(0, 0, 0, 0.0),
            qualityMetrics = QualityMetrics(0.0, emptyList(), emptyList(), 0, 0.0),
            trendMetrics = TrendMetrics(0, 0, 0.0, emptyMap())
        )
    }
}

/**
 * Extension functions for formatting
 */
fun Double.toPercentageString(): String = "${(this * 100).roundToInt()}%"
fun Double.toOneDecimal(): String = "%.1f".format(this)
fun Double.toTwoDecimals(): String = "%.2f".format(this)

/**
 * Get coverage status color
 */
fun getCoverageColor(count: Int): CoverageStatus {
    return when {
        count >= 10 -> CoverageStatus.GOOD
        count >= 5 -> CoverageStatus.ADEQUATE
        else -> CoverageStatus.POOR
    }
}

enum class CoverageStatus {
    GOOD,
    ADEQUATE,
    POOR
}
