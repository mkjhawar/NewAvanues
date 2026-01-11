/**
 * Localized Language Filter
 *
 * Single Responsibility: Filter available languages based on app localization
 *
 * Only shows language packs where we have:
 * - Complete UI translations (strings.xml)
 * - LLM model available
 * - NLU training data (if needed)
 *
 * This prevents users from downloading languages where the app UI
 * would still be in English, creating a poor user experience.
 *
 * Created: 2025-10-31
 * Author: AVA Team
 */

package com.augmentalis.llm.alc.language

import android.content.Context
import timber.log.Timber
import java.util.Locale

/**
 * Filters language packs to only show fully localized languages
 *
 * @param context Android context for accessing resources
 */
class LocalizedLanguageFilter(
    private val context: Context
) {
    /**
     * Filter language packs to only show localized languages
     *
     * @param allPacks All available language packs from CDN
     * @return Filtered list of packs where app has full localization
     */
    fun filterLocalizedOnly(allPacks: List<LanguagePack>): List<LanguagePack> {
        val localizedLanguages = getLocalizedLanguages()

        val filtered = allPacks.filter { pack ->
            pack.code in localizedLanguages
        }

        Timber.d("Filtered ${allPacks.size} language packs to ${filtered.size} localized languages")

        return filtered
    }

    /**
     * Check if a specific language has app localization
     *
     * @param languageCode ISO 639-1 language code (e.g., "es", "fr")
     * @return true if app is fully localized for this language
     */
    fun isLanguageLocalized(languageCode: String): Boolean {
        return languageCode in getLocalizedLanguages()
    }

    /**
     * Get list of languages with full app localization
     *
     * Checks for presence of strings.xml in res/values-{lang}/
     *
     * @return Set of ISO 639-1 language codes
     */
    private fun getLocalizedLanguages(): Set<String> {
        // Get all available locales from resources
        val availableLocales = context.resources.assets.locales

        // Extract language codes from locales
        val localizedCodes = availableLocales
            .map { Locale.forLanguageTag(it).language }
            .filter { it.isNotEmpty() }
            .toSet()

        // Add hardcoded list as fallback (update as we add localizations)
        val supportedLanguages = SUPPORTED_LANGUAGES + localizedCodes

        Timber.d("Available localized languages: ${supportedLanguages.joinToString(", ")}")

        return supportedLanguages
    }

    /**
     * Get display name for a language in current locale
     *
     * @param languageCode ISO 639-1 code
     * @return Localized display name (e.g., "Spanish" or "Español")
     */
    fun getDisplayName(languageCode: String): String {
        return try {
            val locale = Locale(languageCode)
            locale.getDisplayName(Locale.getDefault())
        } catch (e: Exception) {
            Timber.w(e, "Failed to get display name for language: $languageCode")
            languageCode.uppercase()
        }
    }

    /**
     * Get native display name for a language
     *
     * @param languageCode ISO 639-1 code
     * @return Native display name (e.g., "Español" for Spanish)
     */
    fun getNativeDisplayName(languageCode: String): String {
        return try {
            val locale = Locale(languageCode)
            locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get native display name for language: $languageCode")
            languageCode.uppercase()
        }
    }

    /**
     * Get localization completeness percentage
     *
     * @param languageCode ISO 639-1 code
     * @return Percentage of strings translated (0-100)
     */
    fun getLocalizationCompleteness(languageCode: String): Int {
        // In production, this would check actual string resources
        // For now, return 100% for supported languages, 0% for others
        return if (isLanguageLocalized(languageCode)) 100 else 0
    }

    companion object {
        /**
         * Hardcoded list of supported languages
         *
         * Update this as we add new localizations.
         * Must match languages in res/values-{lang}/strings.xml
         */
        private val SUPPORTED_LANGUAGES = setOf(
            "en",  // English (default)
            // Add more as we complete localizations:
            // "es",  // Spanish
            // "fr",  // French
            // "de",  // German
            // "ja",  // Japanese
            // "zh",  // Chinese (Simplified)
            // "pt",  // Portuguese
            // "it",  // Italian
            // "ko",  // Korean
            // "ar",  // Arabic
        )

        /**
         * Languages in development (partial localization)
         *
         * These show in UI with a "Beta" badge
         */
        private val BETA_LANGUAGES = setOf<String>(
            // Add languages with >50% but <100% translation here
        )

        /**
         * Check if a language is in beta
         */
        fun isBetaLanguage(languageCode: String): Boolean {
            return languageCode in BETA_LANGUAGES
        }
    }
}

/**
 * Localization status for a language
 */
enum class LocalizationStatus {
    COMPLETE,      // 100% translated
    BETA,          // >50% translated
    INCOMPLETE,    // <50% translated
    NOT_AVAILABLE  // No translations
}

/**
 * Language with localization metadata
 */
data class LocalizedLanguagePack(
    val pack: LanguagePack,
    val localizationStatus: LocalizationStatus,
    val completenessPercent: Int,
    val displayName: String,
    val nativeDisplayName: String,
    val isBeta: Boolean
)

/**
 * Extension function to enrich language packs with localization info
 */
fun LanguagePack.withLocalizationInfo(
    context: Context
): LocalizedLanguagePack {
    val filter = LocalizedLanguageFilter(context)
    val completeness = filter.getLocalizationCompleteness(code)

    val status = when {
        completeness == 100 -> LocalizationStatus.COMPLETE
        completeness >= 50 -> LocalizationStatus.BETA
        completeness > 0 -> LocalizationStatus.INCOMPLETE
        else -> LocalizationStatus.NOT_AVAILABLE
    }

    return LocalizedLanguagePack(
        pack = this,
        localizationStatus = status,
        completenessPercent = completeness,
        displayName = filter.getDisplayName(code),
        nativeDisplayName = filter.getNativeDisplayName(code),
        isBeta = LocalizedLanguageFilter.isBetaLanguage(code)
    )
}
