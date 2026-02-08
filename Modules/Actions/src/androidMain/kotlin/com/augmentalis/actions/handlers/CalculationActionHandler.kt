/**
 * Calculation Action Handler
 *
 * Intent handler for mathematical calculations.
 * Implements IntentActionHandler to integrate with AVA's action system.
 *
 * Created: 2025-12-03
 * Author: Manoj Jhawar
 */

package com.augmentalis.actions.handlers

import android.content.Context
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.IntentActionHandler

/**
 * Handler for perform_calculation intent
 *
 * Executes mathematical operations and returns formatted results.
 *
 * Usage:
 * ```kotlin
 * // Register handler
 * IntentActionHandlerRegistry.register(CalculationActionHandler())
 *
 * // Handler will be automatically invoked for "perform_calculation" intent
 * ```
 */
class CalculationActionHandler : IntentActionHandler {

    override val intent: String = "perform_calculation"

    private var calculationAction: CalculationAction? = null

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        // Lazy initialize CalculationAction
        if (calculationAction == null) {
            calculationAction = CalculationAction(context)
        }

        // Execute calculation
        val result = calculationAction!!.execute(utterance)

        // Convert to ActionResult format
        return if (result.success) {
            ActionResult.Success(
                message = result.message,
                data = result.data
            )
        } else {
            ActionResult.Failure(
                message = result.message
            )
        }
    }
}
