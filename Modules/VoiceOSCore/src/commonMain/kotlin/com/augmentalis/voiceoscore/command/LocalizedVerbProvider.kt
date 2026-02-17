/**
 * LocalizedVerbProvider.kt - Locale-aware action verb registry
 *
 * Provides localized verb phrases for dynamic command verb extraction.
 * When a user says "pulsar 4" (Spanish for "click 4"), this registry
 * maps "pulsar" to canonical "click" so handler routing works.
 *
 * Populated at runtime from StaticCommandRegistry after VOS file load.
 * Falls back to built-in English verbs before VOS data is available.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.command

import com.augmentalis.voiceoscore.CommandActionType
import kotlin.concurrent.Volatile

/**
 * A localized verb phrase with its canonical English equivalent.
 *
 * @property localizedPhrase The verb as spoken in the user's language (e.g., "pulsar")
 * @property canonicalVerb The English canonical form for handler routing (e.g., "click")
 * @property actionType The CommandActionType this verb maps to (e.g., CLICK)
 */
data class LocalizedVerb(
    val localizedPhrase: String,
    val canonicalVerb: String,
    val actionType: CommandActionType
)

/**
 * Registry of localized action verbs for voice command verb extraction.
 *
 * ## How It Works
 *
 * 1. At startup, built-in English verbs are available immediately
 * 2. After VOS files load, [updateVerbs] is called with locale-specific verbs
 *    extracted from acc_click/acc_long_click command entries
 * 3. [ActionCoordinator] calls [getActionVerbs] for verb extraction
 * 4. When a localized verb is found, [canonicalVerbFor] normalizes it
 *    to English for handler routing
 *
 * ## Verb Command Mapping
 *
 * | VOS Command ID | Canonical Verb | CommandActionType |
 * |----------------|---------------|-------------------|
 * | acc_click      | click         | CLICK             |
 * | acc_long_click | long press    | LONG_CLICK        |
 *
 * ## Example Flow
 * ```
 * User (es-ES): "pulsar 4"
 * getActionVerbs() → includes "pulsar"
 * extractVerbAndTarget("pulsar 4") → verb="pulsar", target="4"
 * canonicalVerbFor("pulsar") → "click"
 * Handler receives: "click 4" → taps element 4
 * ```
 */
object LocalizedVerbProvider {

    /**
     * Maps VOS command IDs to their canonical verb + action type.
     * These are the commands whose phrases represent action verbs
     * rather than standalone commands.
     */
    val VERB_COMMAND_MAP: Map<String, Pair<String, CommandActionType>> = mapOf(
        "acc_click" to Pair("click", CommandActionType.CLICK),
        "acc_long_click" to Pair("long press", CommandActionType.LONG_CLICK)
    )

    /**
     * Built-in English action verbs.
     * These are available immediately before VOS files load.
     * Matches the original hardcoded list in ActionCoordinator.
     */
    private val builtInVerbs: List<LocalizedVerb> = listOf(
        // Click variants
        LocalizedVerb("click", "click", CommandActionType.CLICK),
        LocalizedVerb("tap", "click", CommandActionType.CLICK),
        LocalizedVerb("press", "click", CommandActionType.CLICK),
        LocalizedVerb("select", "click", CommandActionType.CLICK),
        LocalizedVerb("choose", "click", CommandActionType.CLICK),
        LocalizedVerb("pick", "click", CommandActionType.CLICK),
        // Long press variants
        LocalizedVerb("long click", "long press", CommandActionType.LONG_CLICK),
        LocalizedVerb("long press", "long press", CommandActionType.LONG_CLICK),
        LocalizedVerb("hold", "long press", CommandActionType.LONG_CLICK),
        // Double tap
        LocalizedVerb("double tap", "double tap", CommandActionType.DOUBLE_CLICK),
        LocalizedVerb("double click", "double tap", CommandActionType.DOUBLE_CLICK),
        // Scroll (as verb prefix: "scroll the list")
        LocalizedVerb("scroll", "scroll", CommandActionType.SCROLL),
        LocalizedVerb("swipe", "scroll", CommandActionType.SCROLL),
        // Other action verbs
        LocalizedVerb("focus", "focus", CommandActionType.FOCUS),
        LocalizedVerb("type", "type", CommandActionType.TYPE),
        LocalizedVerb("hover", "hover", CommandActionType.HOVER),
        LocalizedVerb("grab", "grab", CommandActionType.GRAB),
        LocalizedVerb("drag", "drag", CommandActionType.DRAG),
        LocalizedVerb("rotate", "rotate", CommandActionType.ROTATE)
    )

    /**
     * Current locale-specific verbs from VOS files.
     * Set to null before VOS load; built-in verbs used as fallback.
     */
    @Volatile
    private var _localeVerbs: List<LocalizedVerb>? = null

    /**
     * Update the locale-specific verb list.
     *
     * Called from CommandManager after StaticCommandRegistry.initialize()
     * when VOS commands for the active locale are loaded.
     *
     * The provided list is MERGED with built-in verbs. Built-in English
     * verbs are always present as fallback (supports bilingual users
     * who mix English verbs with locale commands).
     *
     * @param verbs Localized verbs extracted from VOS verb-type commands
     */
    fun updateVerbs(verbs: List<LocalizedVerb>) {
        _localeVerbs = verbs
    }

    /**
     * Clear locale-specific verbs (e.g., on locale change before new load).
     */
    fun clearLocaleVerbs() {
        _localeVerbs = null
    }

    /**
     * Get all known action verbs as phrase strings.
     *
     * Returns built-in English verbs PLUS any locale-specific verbs.
     * Sorted by length descending for longest-match-first extraction
     * (important for multi-word verbs like "long press" vs "press").
     *
     * @return Sorted list of verb phrase strings
     */
    fun getActionVerbs(): List<String> {
        val allVerbs = buildSet {
            addAll(builtInVerbs.map { it.localizedPhrase })
            _localeVerbs?.let { localeVerbs ->
                addAll(localeVerbs.map { it.localizedPhrase })
            }
        }
        return allVerbs.sortedByDescending { it.length }
    }

    /**
     * Map a localized verb to its canonical English equivalent.
     *
     * Used by ActionCoordinator to normalize verb phrases for handler routing.
     * E.g., "pulsar" → "click", "klicken" → "click"
     *
     * @param localizedVerb The verb as spoken by the user
     * @return Canonical English verb, or the input itself if already canonical or unknown
     */
    fun canonicalVerbFor(localizedVerb: String): String {
        val normalized = localizedVerb.lowercase().trim()

        // Check locale-specific verbs first (more specific)
        _localeVerbs?.find { it.localizedPhrase.lowercase() == normalized }?.let {
            return it.canonicalVerb
        }

        // Check built-in verbs
        builtInVerbs.find { it.localizedPhrase.lowercase() == normalized }?.let {
            return it.canonicalVerb
        }

        // Unknown verb — return as-is (best effort)
        return normalized
    }

    /**
     * Get the CommandActionType for a localized verb.
     *
     * @param localizedVerb The verb as spoken by the user
     * @return CommandActionType or null if verb not recognized
     */
    fun actionTypeForVerb(localizedVerb: String): CommandActionType? {
        val normalized = localizedVerb.lowercase().trim()

        _localeVerbs?.find { it.localizedPhrase.lowercase() == normalized }?.let {
            return it.actionType
        }

        builtInVerbs.find { it.localizedPhrase.lowercase() == normalized }?.let {
            return it.actionType
        }

        return null
    }

    /**
     * Get all verbs with full metadata (for debugging/UI).
     */
    fun allVerbs(): List<LocalizedVerb> {
        val result = builtInVerbs.toMutableList()
        _localeVerbs?.let { localeVerbs ->
            // Add locale verbs that aren't already covered by built-ins
            val builtInPhrases = builtInVerbs.map { it.localizedPhrase.lowercase() }.toSet()
            localeVerbs.filter { it.localizedPhrase.lowercase() !in builtInPhrases }
                .forEach { result.add(it) }
        }
        return result
    }

    /**
     * Whether locale-specific verbs have been loaded.
     */
    fun isLocaleLoaded(): Boolean = _localeVerbs != null

    /**
     * Get verb count for diagnostics.
     */
    val verbCount: Int get() = getActionVerbs().size
}
