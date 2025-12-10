/**
 * UnifiedIntent - Core intent model for shared NLU
 *
 * Represents an intent parsed from AVU .aai files.
 * Used by both VoiceOS and AVA for hybrid classification.
 *
 * Created: 2025-12-07
 */

package com.augmentalis.shared.nlu.model

/**
 * Unified intent model parsed from AVU .aai files.
 *
 * Contains all data needed for pattern matching, fuzzy matching,
 * and semantic similarity classification.
 *
 * @property id Unique intent identifier (e.g., "nav_back")
 * @property canonicalPhrase Primary phrase for this intent (e.g., "go back")
 * @property patterns Exact patterns for matching (from PAT: entries)
 * @property synonyms Alternative phrases (from SYN: entries)
 * @property embedding Pre-computed BERT embedding (384-dim, from EMB: entries)
 * @property category Intent category (nav, media, system, etc.)
 * @property actionId Action to execute (from INT: or ACT: entries)
 * @property priority Higher priority intents match first
 * @property locale Locale code (e.g., "en-US")
 * @property source Where intent came from (core, voiceos, ava, user)
 */
data class UnifiedIntent(
    val id: String,
    val canonicalPhrase: String,
    val patterns: List<String>,
    val synonyms: List<String>,
    val embedding: FloatArray?,
    val category: String,
    val actionId: String,
    val priority: Int,
    val locale: String,
    val source: String
) {
    /**
     * Get all matchable phrases (patterns + synonyms + canonical)
     */
    val allPhrases: List<String>
        get() = listOf(canonicalPhrase) + patterns + synonyms

    /**
     * Check if this intent has a pre-computed embedding
     */
    val hasEmbedding: Boolean
        get() = embedding != null && embedding.isNotEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UnifiedIntent
        return id == other.id && locale == other.locale
    }

    override fun hashCode(): Int {
        return 31 * id.hashCode() + locale.hashCode()
    }

    override fun toString(): String {
        return "UnifiedIntent(id='$id', canonical='$canonicalPhrase', category='$category', locale='$locale')"
    }
}

/**
 * Intent match result with confidence score
 */
data class IntentMatch(
    val intent: UnifiedIntent,
    val score: Float,
    val matchedPhrase: String? = null,
    val method: MatchMethod = MatchMethod.UNKNOWN
) : Comparable<IntentMatch> {
    override fun compareTo(other: IntentMatch): Int {
        return other.score.compareTo(score) // Descending by score
    }
}

/**
 * How the intent was matched
 */
enum class MatchMethod {
    EXACT,      // Exact pattern match
    FUZZY,      // Levenshtein fuzzy match
    SEMANTIC,   // Embedding cosine similarity
    HYBRID,     // Combined fuzzy + semantic
    UNKNOWN
}

/**
 * Intent category constants
 */
object IntentCategory {
    const val NAVIGATION = "navigation"
    const val MEDIA = "media"
    const val SYSTEM = "system"
    const val EDITING = "editing"
    const val BROWSER = "browser"
    const val OVERLAY = "overlay"
    const val INTERACTION = "interaction"
    const val APP = "app"
    const val CUSTOM = "custom"
}

/**
 * Intent source constants
 */
object IntentSource {
    const val CORE = "core"
    const val VOICEOS = "voiceos"
    const val AVA = "ava"
    const val USER = "user"
}
