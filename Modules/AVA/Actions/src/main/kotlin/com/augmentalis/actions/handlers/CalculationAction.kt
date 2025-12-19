/**
 * Calculation Action Handler
 *
 * Executes mathematical calculations triggered by NLU intent classification.
 * Integrates with IntentRouter and returns formatted results.
 *
 * Created: 2025-12-03
 * Author: Manoj Jhawar
 */

package com.augmentalis.actions.handlers

import android.content.Context
import timber.log.Timber

/**
 * Result of executing a calculation action
 */
data class ActionResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any> = emptyMap()
)

/**
 * Calculation action handler
 *
 * Usage:
 * ```kotlin
 * val handler = CalculationAction(context)
 * val result = handler.execute("multiply 12 by 10", emptyMap())
 * // result.message = "12 × 10 = 120"
 * ```
 */
class CalculationAction(
    private val context: Context
) {

    companion object {
        private const val TAG = "CalculationAction"
    }

    /**
     * Execute calculation from user utterance
     *
     * @param utterance Original user input (e.g., "multiply 12 by 10")
     * @param entities Extracted entities from NLU (optional, not used currently)
     * @return ActionResult with formatted answer
     */
    fun execute(utterance: String, entities: Map<String, String> = emptyMap()): ActionResult {
        Timber.d("$TAG: Executing calculation for: $utterance")

        val calcResult = MathCalculator.calculate(utterance)

        return if (calcResult.success) {
            val message = formatSuccessMessage(calcResult)
            Timber.i("$TAG: Calculation successful: $message")

            ActionResult(
                success = true,
                message = message,
                data = mapOf(
                    "result" to (calcResult.result ?: 0.0),
                    "expression" to calcResult.expression,
                    "formatted" to calcResult.formattedResult
                )
            )
        } else {
            val message = "I couldn't calculate that. ${calcResult.error ?: "Please try rephrasing."}"
            Timber.w("$TAG: Calculation failed: ${calcResult.error}")

            ActionResult(
                success = false,
                message = message
            )
        }
    }

    /**
     * Format success message with result
     */
    private fun formatSuccessMessage(result: CalculationResult): String {
        val expr = result.expression
        val value = result.formattedResult

        return when {
            // Show expression with result
            expr.isNotEmpty() -> "$expr = $value"

            // Just show value
            else -> value
        }
    }

    /**
     * Check if intent is a calculation intent
     */
    fun isCalculationIntent(intent: String): Boolean {
        return intent == "perform_calculation" ||
               intent.contains("calculate") ||
               intent.contains("math")
    }

    /**
     * Get supported operations (for help/documentation)
     */
    fun getSupportedOperations(): List<String> {
        return listOf(
            "Basic: add, subtract, multiply, divide",
            "Advanced: square root, power, factorial",
            "Trigonometry: sine, cosine, tangent",
            "Logarithms: log, natural log",
            "Percentages: X% of Y",
            "Rounding: round, floor, ceiling",
            "Constants: pi, e"
        )
    }
}
