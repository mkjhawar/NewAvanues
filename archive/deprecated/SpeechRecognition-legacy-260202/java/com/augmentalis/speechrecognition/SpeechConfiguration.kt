/**
 * SpeechConfiguration.kt - Vivoka-specific configuration helpers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 * Updated: 2026-01-18 - Migrated core types to KMP commonMain, kept Vivoka helpers
 *
 * This file contains Android-specific helpers for Vivoka engine configuration.
 * Core types (SpeechEngine, SpeechMode, SpeechConfig) are now in commonMain.
 */
package com.augmentalis.speechrecognition

import com.augmentalis.voiceos.speech.engines.vivoka.model.VivokaLanguageRepository

/**
 * Vivoka-specific configuration extensions and helpers.
 * These require Android-specific imports that can't go in commonMain.
 */
object VivokaConfigHelper {

    /**
     * Convert Vivoka language code to BCP-47 standard code
     */
    fun vivokaLanguageToBcp47(vivokaCode: String): String {
        return when (vivokaCode) {
            VivokaLanguageRepository.LANGUAGE_CODE_ENGLISH_USA -> LanguageCodes.ENGLISH_US
            VivokaLanguageRepository.LANGUAGE_CODE_ENGLISH_UNITED_KINGDOM -> LanguageCodes.ENGLISH_UK
            VivokaLanguageRepository.LANGUAGE_CODE_FRENCH -> LanguageCodes.FRENCH
            VivokaLanguageRepository.LANGUAGE_CODE_GERMAN -> LanguageCodes.GERMAN
            VivokaLanguageRepository.LANGUAGE_CODE_SPANISH -> LanguageCodes.SPANISH
            VivokaLanguageRepository.LANGUAGE_CODE_ITALIAN -> LanguageCodes.ITALIAN
            VivokaLanguageRepository.LANGUAGE_CODE_PORTUGUESE -> LanguageCodes.PORTUGUESE
            VivokaLanguageRepository.LANGUAGE_CODE_JAPANESE -> LanguageCodes.JAPANESE
            VivokaLanguageRepository.LANGUAGE_CODE_KOREAN -> LanguageCodes.KOREAN
            VivokaLanguageRepository.LANGUAGE_CODE_RUSSIAN -> LanguageCodes.RUSSIAN
            else -> vivokaCode
        }
    }

    /**
     * Convert BCP-47 language code to Vivoka code
     */
    fun bcp47ToVivokaLanguage(bcp47Code: String): String {
        return when (bcp47Code) {
            LanguageCodes.ENGLISH_US -> VivokaLanguageRepository.LANGUAGE_CODE_ENGLISH_USA
            LanguageCodes.ENGLISH_UK -> VivokaLanguageRepository.LANGUAGE_CODE_ENGLISH_UNITED_KINGDOM
            LanguageCodes.FRENCH -> VivokaLanguageRepository.LANGUAGE_CODE_FRENCH
            LanguageCodes.GERMAN -> VivokaLanguageRepository.LANGUAGE_CODE_GERMAN
            LanguageCodes.SPANISH -> VivokaLanguageRepository.LANGUAGE_CODE_SPANISH
            LanguageCodes.ITALIAN -> VivokaLanguageRepository.LANGUAGE_CODE_ITALIAN
            LanguageCodes.PORTUGUESE -> VivokaLanguageRepository.LANGUAGE_CODE_PORTUGUESE
            LanguageCodes.JAPANESE -> VivokaLanguageRepository.LANGUAGE_CODE_JAPANESE
            LanguageCodes.KOREAN -> VivokaLanguageRepository.LANGUAGE_CODE_KOREAN
            LanguageCodes.RUSSIAN -> VivokaLanguageRepository.LANGUAGE_CODE_RUSSIAN
            else -> bcp47Code
        }
    }

    /**
     * Convert normalized confidence (0-1) to Vivoka confidence (1000-10000)
     */
    fun normalizedToVivokaConfidence(normalized: Float): Float {
        return (normalized * 9000f + 1000f).coerceIn(1000f, 10000f)
    }

    /**
     * Convert Vivoka confidence (1000-10000) to normalized confidence (0-1)
     */
    fun vivokaToNormalizedConfidence(vivoka: Float): Float {
        return ((vivoka - 1000f) / 9000f).coerceIn(0f, 1f)
    }

    /**
     * Validate Vivoka-specific confidence threshold
     */
    fun isValidVivokaConfidence(confidence: Float): Boolean {
        return confidence in 1000f..10000f
    }

    /**
     * Create a Vivoka-compatible SpeechConfig
     */
    fun createVivokaConfig(
        language: String = VivokaLanguageRepository.LANGUAGE_CODE_ENGLISH_USA,
        vivokaConfidence: Float = 4000f,
        mode: SpeechMode = SpeechMode.DYNAMIC_COMMAND
    ): SpeechConfig {
        return SpeechConfig(
            engine = SpeechEngine.VIVOKA,
            language = vivokaLanguageToBcp47(language),
            mode = mode,
            confidenceThreshold = vivokaToNormalizedConfidence(vivokaConfidence)
        )
    }

    /**
     * Get Vivoka confidence from SpeechConfig
     */
    fun SpeechConfig.getVivokaConfidence(): Float {
        return normalizedToVivokaConfidence(this.confidenceThreshold)
    }
}

/**
 * Legacy type alias for backward compatibility
 * @deprecated Use SpeechConfig from commonMain
 */
@Deprecated("Use SpeechConfig from commonMain", ReplaceWith("SpeechConfig"))
typealias SpeechRecognitionConfig = SpeechConfig
