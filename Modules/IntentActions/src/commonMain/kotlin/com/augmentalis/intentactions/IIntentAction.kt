package com.augmentalis.intentactions

/**
 * Interface for intent-based actions that interact with the external world.
 *
 * IntentActions handle commands that require Activity context to launch external apps,
 * send communications, navigate maps, set alarms, etc. They are distinct from VoiceOSCore
 * IHandler actions which control what's on screen (gestures, system controls, module commands).
 *
 * Architecture:
 * - VoiceOSCore (IHandler) = control what's ON screen
 * - IntentActions (IIntentAction) = interact WITH the world
 * - Macros can compose steps from both systems
 *
 * @see IntentActionRegistry
 * @see IntentResult
 */
interface IIntentAction {
    /** Unique intent identifier (e.g., "send_email", "navigate_map") */
    val intentId: String

    /** Category for grouping and filtering */
    val category: IntentCategory

    /** Entity types required for execution (NLU must extract these before dispatch) */
    val requiredEntities: List<EntityType>

    /**
     * Execute this intent action.
     *
     * @param context Platform-specific context (Activity on Android)
     * @param entities Extracted entities from the user's utterance
     * @return Result indicating success, failure, or need for more information
     */
    suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult
}

/** Platform-specific context wrapper. Implemented as Activity on Android. */
expect class PlatformContext
