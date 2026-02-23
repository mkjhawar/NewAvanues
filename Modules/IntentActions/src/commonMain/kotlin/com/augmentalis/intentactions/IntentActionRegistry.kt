package com.augmentalis.intentactions

/**
 * Registry for all available IntentActions.
 *
 * Provides lookup by intent ID and category. Used by:
 * - NLU classifier to dispatch classified intents
 * - MacroExecutor to execute intent steps in macros
 * - UI to display available intent actions
 */
object IntentActionRegistry {
    private val actions = mutableMapOf<String, IIntentAction>()

    /** Register an intent action */
    fun register(action: IIntentAction) {
        actions[action.intentId] = action
    }

    /** Register multiple intent actions */
    fun registerAll(vararg actionList: IIntentAction) {
        actionList.forEach { register(it) }
    }

    /** Find action by intent ID */
    fun findByIntent(intentId: String): IIntentAction? = actions[intentId]

    /** Get all actions in a category */
    fun getByCategory(category: IntentCategory): List<IIntentAction> =
        actions.values.filter { it.category == category }

    /** Get all registered actions */
    fun getAll(): List<IIntentAction> = actions.values.toList()

    /** Get all registered intent IDs */
    fun getAllIntentIds(): Set<String> = actions.keys.toSet()

    /** Check if an intent has a registered action */
    fun hasAction(intentId: String): Boolean = intentId in actions

    /** Execute an intent action by ID */
    suspend fun execute(
        intentId: String,
        context: PlatformContext,
        entities: ExtractedEntities
    ): IntentResult {
        val action = actions[intentId]
            ?: return IntentResult.Failed("Unknown intent: $intentId")
        return try {
            action.execute(context, entities)
        } catch (e: Exception) {
            IntentResult.Failed("Intent '$intentId' failed: ${e.message}", e)
        }
    }

    /** Clear all registered actions (for testing) */
    fun clear() {
        actions.clear()
    }
}
