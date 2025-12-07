/**
 * AndroidLanguage.kt - Language mapping and management for AndroidSTTEngine
 * 
 * Extracted from AndroidSTTEngine as part of SOLID refactoring
 * Handles all language-related functionality including:
 * - BCP-47 language code mapping
 * - Language validation
 * - Locale management
 * - Language feature support
 * 
 * © Augmentalis Inc, Intelligent Devices LLC, Manoj Jhawar, Aman Jhawar
 */
package com.augmentalis.voiceos.speech.engines.android

import android.util.Log
import java.util.Locale

/**
 * Manages language mapping and validation for AndroidSTTEngine.
 * Provides BCP-47 compliant language codes and regional variants.
 */
class AndroidLanguage {
    
    companion object {
        private const val TAG = "AndroidLanguage"
        
        // Comprehensive language mapping for Android Speech Recognition
        // Maps VOS4 language codes to Android-compatible BCP-47 tags
        private val LANGUAGE_MAP = mapOf(
            // English variants
            "en-US" to "en-US",
            "en-GB" to "en-GB", 
            "en-AU" to "en-AU",
            "en-CA" to "en-CA",
            "en-IN" to "en-IN",
            
            // French variants
            "fr-FR" to "fr-FR",
            "fr-CA" to "fr-CA",
            
            // German
            "de-DE" to "de-DE",
            
            // Spanish variants
            "es-ES" to "es-ES",
            "es-MX" to "es-MX",
            "es-AR" to "es-AR",
            
            // Italian
            "it-IT" to "it-IT",
            
            // Asian languages
            "ja-JP" to "ja-JP",
            "ko-KR" to "ko-KR",
            "zh-CN" to "zh-CN",
            "zh-TW" to "zh-TW",
            "zh-HK" to "zh-HK",
            
            // Portuguese variants
            "pt-BR" to "pt-BR",
            "pt-PT" to "pt-PT",
            
            // Other European languages
            "ru-RU" to "ru-RU",
            "nl-NL" to "nl-NL",
            "pl-PL" to "pl-PL",
            "sv-SE" to "sv-SE",
            "da-DK" to "da-DK",
            "no-NO" to "nb-NO", // Norwegian Bokmål
            "fi-FI" to "fi-FI",
            "tr-TR" to "tr-TR",
            "el-GR" to "el-GR",
            "he-IL" to "iw-IL", // Hebrew uses legacy code
            "hu-HU" to "hu-HU",
            "cs-CZ" to "cs-CZ",
            "sk-SK" to "sk-SK",
            "ro-RO" to "ro-RO",
            "uk-UA" to "uk-UA",
            "bg-BG" to "bg-BG",
            "hr-HR" to "hr-HR",
            "sr-RS" to "sr-RS",
            "sl-SI" to "sl-SI",
            "lt-LT" to "lt-LT",
            "lv-LV" to "lv-LV",
            "et-EE" to "et-EE",
            
            // Indian languages
            "hi-IN" to "hi-IN",
            "bn-IN" to "bn-IN",
            "ta-IN" to "ta-IN",
            "te-IN" to "te-IN",
            "ml-IN" to "ml-IN",
            "kn-IN" to "kn-IN",
            "gu-IN" to "gu-IN",
            "mr-IN" to "mr-IN",
            
            // Southeast Asian
            "vi-VN" to "vi-VN",
            "id-ID" to "id-ID",
            "ms-MY" to "ms-MY",
            "th-TH" to "th-TH",
            
            // Arabic (using generic region code)
            "ar-SA" to "ar-001"
        )
        
        // Default fallback language
        private const val DEFAULT_LANGUAGE = "en-US"
    }
    
    // Current language settings
    private var currentLanguage: String = DEFAULT_LANGUAGE
    private var currentBcpTag: String = DEFAULT_LANGUAGE
    private var currentLocale: Locale = Locale.US
    
    /**
     * Set the current language and map to Android BCP-47 tag
     */
    fun setLanguage(languageCode: String): Boolean {
        val mappedLanguage = LANGUAGE_MAP[languageCode]
        
        if (mappedLanguage != null) {
            currentLanguage = languageCode
            currentBcpTag = mappedLanguage
            currentLocale = parseLocale(mappedLanguage)
            
            Log.d(TAG, "Language set: $languageCode -> $mappedLanguage")
            return true
        } else {
            Log.w(TAG, "Unsupported language: $languageCode, using default: $DEFAULT_LANGUAGE")
            // Fall back to input language if no mapping exists
            currentLanguage = languageCode
            currentBcpTag = languageCode
            currentLocale = parseLocale(languageCode)
            return false
        }
    }
    
    /**
     * Parse BCP-47 language tag into Locale
     */
    private fun parseLocale(bcpTag: String): Locale {
        return try {
            val parts = bcpTag.split("-")
            when (parts.size) {
                1 -> Locale(parts[0])
                2 -> Locale(parts[0], parts[1])
                else -> Locale(parts[0], parts[1], parts.drop(2).joinToString("-"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse locale for $bcpTag: ${e.message}")
            Locale.US
        }
    }
    
    /**
     * Get current language code (VOS4 format)
     */
    fun getCurrentLanguage(): String = currentLanguage
    
    /**
     * Get current BCP-47 tag for Android
     */
    fun getCurrentBcpTag(): String = currentBcpTag
    
    /**
     * Get current locale object
     */
    fun getCurrentLocale(): Locale = currentLocale
    
    /**
     * Check if a language is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return LANGUAGE_MAP.containsKey(languageCode)
    }
    
    /**
     * Get all supported languages
     */
    fun getSupportedLanguages(): List<String> = LANGUAGE_MAP.keys.toList()
    
    /**
     * Get mapping for a specific language
     */
    fun getLanguageMapping(languageCode: String): String? {
        return LANGUAGE_MAP[languageCode]
    }
    
    /**
     * Get language display name in current locale
     */
    fun getLanguageDisplayName(languageCode: String = currentLanguage): String {
        return try {
            val locale = parseLocale(LANGUAGE_MAP[languageCode] ?: languageCode)
            locale.getDisplayName(currentLocale)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get display name for $languageCode: ${e.message}")
            languageCode
        }
    }
    
    /**
     * Get language statistics
     */
    fun getLanguageStats(): LanguageStats {
        val totalSupported = LANGUAGE_MAP.size
        val europeanCount = LANGUAGE_MAP.keys.count { key ->
            key.endsWith("-DE") || key.endsWith("-FR") || key.endsWith("-ES") ||
            key.endsWith("-IT") || key.endsWith("-NL") || key.endsWith("-PL") ||
            key.startsWith("en-") && key != "en-US"
        }
        val asianCount = LANGUAGE_MAP.keys.count { key ->
            key.startsWith("zh-") || key.startsWith("ja-") || key.startsWith("ko-") ||
            key.endsWith("-IN") || key.endsWith("-VN") || key.endsWith("-ID") ||
            key.endsWith("-MY") || key.endsWith("-TH")
        }
        
        return LanguageStats(
            totalSupported = totalSupported,
            europeanLanguages = europeanCount,
            asianLanguages = asianCount,
            currentLanguage = currentLanguage,
            currentBcpTag = currentBcpTag
        )
    }
    
    /**
     * Reset to default language
     */
    fun reset() {
        setLanguage(DEFAULT_LANGUAGE)
        Log.d(TAG, "Language reset to default: $DEFAULT_LANGUAGE")
    }
    
    /**
     * Data class for language statistics
     */
    data class LanguageStats(
        val totalSupported: Int,
        val europeanLanguages: Int,
        val asianLanguages: Int,
        val currentLanguage: String,
        val currentBcpTag: String
    )
}