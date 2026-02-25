/**
 * MultilingualSupport - Language-aware text processing for command matching
 *
 * Handles:
 * - Unicode normalization (NFKC)
 * - Diacritics removal (optional)
 * - Character equivalency classes (Arabic, Cyrillic, etc.)
 * - Language detection
 * - Locale-specific synonym expansion
 *
 * Created: 2026-01-17
 */

package com.augmentalis.nlu.matching

import com.augmentalis.nlu.NluThresholds

/**
 * Supported languages/locales for command matching.
 */
enum class SupportedLocale(
    val code: String,
    val script: Script,
    val rtl: Boolean = false
) {
    // Latin-based
    ENGLISH("en", Script.LATIN),
    SPANISH("es", Script.LATIN),
    FRENCH("fr", Script.LATIN),
    GERMAN("de", Script.LATIN),
    PORTUGUESE("pt", Script.LATIN),
    ITALIAN("it", Script.LATIN),
    DUTCH("nl", Script.LATIN),
    POLISH("pl", Script.LATIN),
    VIETNAMESE("vi", Script.LATIN),

    // CJK
    CHINESE_SIMPLIFIED("zh-CN", Script.CJK),
    CHINESE_TRADITIONAL("zh-TW", Script.CJK),
    JAPANESE("ja", Script.CJK),
    KOREAN("ko", Script.CJK),

    // Arabic/RTL
    ARABIC("ar", Script.ARABIC, rtl = true),
    HEBREW("he", Script.HEBREW, rtl = true),
    PERSIAN("fa", Script.ARABIC, rtl = true),
    URDU("ur", Script.ARABIC, rtl = true),

    // Indic
    HINDI("hi", Script.DEVANAGARI),
    TAMIL("ta", Script.TAMIL),
    TELUGU("te", Script.TELUGU),
    BENGALI("bn", Script.BENGALI),

    // Cyrillic
    RUSSIAN("ru", Script.CYRILLIC),
    UKRAINIAN("uk", Script.CYRILLIC),

    // Other
    GREEK("el", Script.GREEK),
    THAI("th", Script.THAI),
    TURKISH("tr", Script.LATIN);

    companion object {
        fun fromCode(code: String): SupportedLocale? {
            return entries.find { it.code.equals(code, ignoreCase = true) }
        }
    }
}

/**
 * Script types for processing decisions.
 */
enum class Script {
    LATIN,
    CJK,
    ARABIC,
    HEBREW,
    DEVANAGARI,
    TAMIL,
    TELUGU,
    BENGALI,
    CYRILLIC,
    GREEK,
    THAI
}

/**
 * Multilingual text normalizer.
 *
 * Provides language-aware normalization for consistent matching.
 */
class MultilingualNormalizer(
    private val config: NormalizationConfig = NormalizationConfig()
) {

    /**
     * Normalize text for matching.
     *
     * @param text Input text
     * @param locale Target locale (null = auto-detect or default)
     * @return Normalized text
     */
    fun normalize(text: String, locale: SupportedLocale? = null): String {
        if (text.isBlank()) return ""

        var result = text

        // Step 1: Unicode normalization (NFKC for compatibility)
        if (config.unicodeNormalize) {
            result = unicodeNormalize(result)
        }

        // Step 2: Lowercase (language-aware)
        if (config.lowercase) {
            result = lowercaseLocaleAware(result, locale)
        }

        // Step 3: Trim and collapse whitespace
        result = result.trim().replace(Regex("\\s+"), " ")

        // Step 4: Remove diacritics (optional, locale-dependent)
        if (config.removeDiacritics && shouldRemoveDiacritics(locale)) {
            result = removeDiacritics(result)
        }

        // Step 5: Apply character equivalencies (for Arabic, Cyrillic, etc.)
        if (config.applyEquivalencies) {
            result = applyCharacterEquivalencies(result, locale)
        }

        return result
    }

    /**
     * Normalize for fuzzy matching (more aggressive normalization).
     */
    fun normalizeForFuzzy(text: String, locale: SupportedLocale? = null): String {
        var result = normalize(text, locale)

        // Remove punctuation for fuzzy matching
        if (config.removePunctuation) {
            result = result.replace(Regex("[\\p{Punct}]"), "")
        }

        return result
    }

    /**
     * Unicode NFKC normalization.
     * Platform-specific implementation.
     */
    private fun unicodeNormalize(text: String): String {
        return normalizeUnicode(text)
    }

    /**
     * Locale-aware lowercase conversion.
     * Handles Turkish İ/i, German ß, etc.
     */
    private fun lowercaseLocaleAware(text: String, locale: SupportedLocale?): String {
        return when (locale) {
            SupportedLocale.TURKISH -> text.lowercase().replace('İ', 'i').replace('I', 'ı')
            else -> text.lowercase()
        }
    }

    /**
     * Determine if diacritics should be removed for locale.
     * Some languages (Vietnamese, Thai) rely heavily on diacritics for meaning.
     */
    private fun shouldRemoveDiacritics(locale: SupportedLocale?): Boolean {
        return when (locale) {
            SupportedLocale.VIETNAMESE,
            SupportedLocale.THAI,
            SupportedLocale.HINDI,
            SupportedLocale.TAMIL,
            SupportedLocale.TELUGU,
            SupportedLocale.BENGALI,
            SupportedLocale.ARABIC,
            SupportedLocale.HEBREW,
            SupportedLocale.PERSIAN,
            SupportedLocale.URDU -> false
            else -> true
        }
    }

    /**
     * Remove diacritical marks (accents).
     * NFD decomposition + strip combining marks.
     */
    private fun removeDiacritics(text: String): String {
        return stripDiacritics(text)
    }

    /**
     * Apply character equivalency classes.
     * Handles variations in Arabic, Cyrillic, etc.
     */
    private fun applyCharacterEquivalencies(text: String, locale: SupportedLocale?): String {
        val script = locale?.script ?: detectScript(text)

        return when (script) {
            Script.ARABIC -> applyArabicEquivalencies(text)
            Script.CYRILLIC -> applyCyrillicEquivalencies(text)
            else -> text
        }
    }

    /**
     * Arabic character equivalencies.
     * Handle alif variations, hamza positions, etc.
     */
    private fun applyArabicEquivalencies(text: String): String {
        return text
            // Alif variations → base alif
            .replace('أ', 'ا')  // Alif with hamza above
            .replace('إ', 'ا')  // Alif with hamza below
            .replace('آ', 'ا')  // Alif with madda
            .replace('ٱ', 'ا')  // Alif wasla
            // Taa marbuta → haa
            .replace('ة', 'ه')
            // Alif maqsura → yaa
            .replace('ى', 'ي')
    }

    /**
     * Cyrillic character equivalencies.
     */
    private fun applyCyrillicEquivalencies(text: String): String {
        return text
            // Е/Ё equivalence (Russian)
            .replace('ё', 'е')
            .replace('Ё', 'Е')
    }

    /**
     * Detect script from text content.
     */
    private fun detectScript(text: String): Script {
        for (char in text) {
            return when {
                char in '\u0600'..'\u06FF' -> Script.ARABIC
                char in '\u0400'..'\u04FF' -> Script.CYRILLIC
                char in '\u4E00'..'\u9FFF' -> Script.CJK
                char in '\u3040'..'\u30FF' -> Script.CJK // Japanese
                char in '\uAC00'..'\uD7AF' -> Script.CJK // Korean
                char in '\u0900'..'\u097F' -> Script.DEVANAGARI
                char in '\u0B80'..'\u0BFF' -> Script.TAMIL
                char in '\u0C00'..'\u0C7F' -> Script.TELUGU
                char in '\u0980'..'\u09FF' -> Script.BENGALI
                char in '\u0370'..'\u03FF' -> Script.GREEK
                char in '\u0E00'..'\u0E7F' -> Script.THAI
                char in '\u0590'..'\u05FF' -> Script.HEBREW
                char.isLetter() -> Script.LATIN
                else -> continue
            }
        }
        return Script.LATIN
    }
}

/**
 * Configuration for text normalization.
 */
data class NormalizationConfig(
    val unicodeNormalize: Boolean = true,
    val lowercase: Boolean = true,
    val removeDiacritics: Boolean = true,
    val removePunctuation: Boolean = false,
    val applyEquivalencies: Boolean = true
)

/**
 * Locale-aware synonym provider.
 *
 * Manages synonyms per language for voice command matching.
 */
class LocalizedSynonymProvider {

    // Locale -> (synonym -> canonical)
    private val synonymsByLocale = mutableMapOf<SupportedLocale, MutableMap<String, String>>()

    // Fallback synonyms (used when locale not found)
    private val fallbackSynonyms = mutableMapOf<String, String>()

    /**
     * Set synonyms for a specific locale.
     */
    fun setSynonyms(locale: SupportedLocale, synonyms: Map<String, String>) {
        synonymsByLocale[locale] = synonyms.toMutableMap()
    }

    /**
     * Add a synonym for a specific locale.
     */
    fun addSynonym(locale: SupportedLocale, synonym: String, canonical: String) {
        synonymsByLocale.getOrPut(locale) { mutableMapOf() }[synonym.lowercase()] = canonical.lowercase()
    }

    /**
     * Set fallback synonyms (used when locale-specific not found).
     */
    fun setFallbackSynonyms(synonyms: Map<String, String>) {
        fallbackSynonyms.clear()
        synonyms.forEach { (k, v) -> fallbackSynonyms[k.lowercase()] = v.lowercase() }
    }

    /**
     * Expand synonyms in text for the given locale.
     */
    fun expand(text: String, locale: SupportedLocale): String {
        val synonyms = synonymsByLocale[locale] ?: fallbackSynonyms

        val words = text.split(Regex("\\s+"))
        val expanded = words.map { word ->
            synonyms[word.lowercase()] ?: word
        }
        return expanded.joinToString(" ")
    }

    /**
     * Get canonical form of a word.
     */
    fun getCanonical(word: String, locale: SupportedLocale): String {
        val synonyms = synonymsByLocale[locale] ?: fallbackSynonyms
        return synonyms[word.lowercase()] ?: word
    }

    /**
     * Load default synonyms for common voice actions.
     */
    fun loadDefaults() {
        // English defaults
        setSynonyms(SupportedLocale.ENGLISH, mapOf(
            "tap" to "click",
            "press" to "click",
            "hit" to "click",
            "touch" to "click",
            "select" to "click",
            "choose" to "click",
            "open" to "launch",
            "start" to "launch",
            "run" to "launch",
            "close" to "exit",
            "quit" to "exit",
            "shut" to "exit",
            "go back" to "back",
            "return" to "back",
            "previous" to "back",
            "forward" to "next",
            "ahead" to "next",
            "search for" to "search",
            "look for" to "search",
            "find" to "search"
        ))

        // Spanish defaults
        setSynonyms(SupportedLocale.SPANISH, mapOf(
            "tocar" to "click",
            "pulsar" to "click",
            "presionar" to "click",
            "abrir" to "launch",
            "iniciar" to "launch",
            "cerrar" to "exit",
            "salir" to "exit",
            "atrás" to "back",
            "volver" to "back",
            "buscar" to "search"
        ))

        // French defaults
        setSynonyms(SupportedLocale.FRENCH, mapOf(
            "appuyer" to "click",
            "toucher" to "click",
            "ouvrir" to "launch",
            "lancer" to "launch",
            "fermer" to "exit",
            "quitter" to "exit",
            "retour" to "back",
            "revenir" to "back",
            "chercher" to "search",
            "rechercher" to "search"
        ))

        // German defaults
        setSynonyms(SupportedLocale.GERMAN, mapOf(
            "drücken" to "click",
            "tippen" to "click",
            "öffnen" to "launch",
            "starten" to "launch",
            "schließen" to "exit",
            "beenden" to "exit",
            "zurück" to "back",
            "suchen" to "search"
        ))

        // Arabic defaults
        setSynonyms(SupportedLocale.ARABIC, mapOf(
            "اضغط" to "click",     // press
            "انقر" to "click",     // click
            "افتح" to "launch",    // open
            "شغل" to "launch",     // run
            "أغلق" to "exit",      // close
            "خروج" to "exit",      // exit
            "رجوع" to "back",      // back
            "عودة" to "back",      // return
            "ابحث" to "search"     // search
        ))

        // Hindi defaults
        setSynonyms(SupportedLocale.HINDI, mapOf(
            "दबाएं" to "click",     // press
            "टैप करें" to "click", // tap
            "खोलें" to "launch",   // open
            "शुरू करें" to "launch", // start
            "बंद करें" to "exit",   // close
            "वापस" to "back",      // back
            "खोजें" to "search"    // search
        ))

        // Chinese defaults
        setSynonyms(SupportedLocale.CHINESE_SIMPLIFIED, mapOf(
            "点击" to "click",      // click
            "按" to "click",        // press
            "打开" to "launch",     // open
            "启动" to "launch",     // start
            "关闭" to "exit",       // close
            "退出" to "exit",       // exit
            "返回" to "back",       // back
            "搜索" to "search"      // search
        ))

        // Japanese defaults
        setSynonyms(SupportedLocale.JAPANESE, mapOf(
            "タップ" to "click",     // tap
            "クリック" to "click",   // click
            "押す" to "click",       // press
            "開く" to "launch",      // open
            "起動" to "launch",      // start
            "閉じる" to "exit",      // close
            "終了" to "exit",        // exit
            "戻る" to "back",        // back
            "検索" to "search"       // search
        ))

        // Set English as fallback
        setFallbackSynonyms(synonymsByLocale[SupportedLocale.ENGLISH] ?: emptyMap())
    }
}

/**
 * Language detector for input text.
 */
class LanguageDetector {

    /**
     * Detect the most likely language of input text.
     *
     * Simple heuristic-based detection. For production, consider using
     * a proper language detection library.
     *
     * @param text Input text
     * @return Detected locale or null if uncertain
     */
    fun detect(text: String): SupportedLocale? {
        if (text.isBlank()) return null

        // Count characters by script
        var latinCount = 0
        var arabicCount = 0
        var cjkCount = 0
        var cyrillicCount = 0
        var devanagariCount = 0

        for (char in text) {
            when {
                char in '\u0600'..'\u06FF' -> arabicCount++
                char in '\u4E00'..'\u9FFF' || char in '\u3040'..'\u30FF' || char in '\uAC00'..'\uD7AF' -> cjkCount++
                char in '\u0400'..'\u04FF' -> cyrillicCount++
                char in '\u0900'..'\u097F' -> devanagariCount++
                char.isLetter() -> latinCount++
            }
        }

        val total = latinCount + arabicCount + cjkCount + cyrillicCount + devanagariCount
        if (total == 0) return null

        // Return dominant script's default locale
        return when {
            arabicCount.toFloat() / total > NluThresholds.SCRIPT_MAJORITY_THRESHOLD -> SupportedLocale.ARABIC
            cjkCount.toFloat() / total > NluThresholds.SCRIPT_MAJORITY_THRESHOLD -> SupportedLocale.CHINESE_SIMPLIFIED
            cyrillicCount.toFloat() / total > NluThresholds.SCRIPT_MAJORITY_THRESHOLD -> SupportedLocale.RUSSIAN
            devanagariCount.toFloat() / total > NluThresholds.SCRIPT_MAJORITY_THRESHOLD -> SupportedLocale.HINDI
            latinCount.toFloat() / total > NluThresholds.SCRIPT_MAJORITY_THRESHOLD -> SupportedLocale.ENGLISH
            else -> null
        }
    }
}

// =============================================================================
// Platform-specific functions (expect declarations)
// =============================================================================

/**
 * Unicode NFKC normalization.
 * Platform implementations in androidMain, iosMain, desktopMain, jsMain.
 */
internal expect fun normalizeUnicode(text: String): String

/**
 * Strip diacritical marks from text.
 * Uses Unicode NFD decomposition + combining mark removal.
 */
internal expect fun stripDiacritics(text: String): String
