/**
 * VivokaLanguageRepository.kt - Language model configuration for Vivoka VSDK
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 */
package com.augmentalis.speechrecognition.vivoka.model

import com.google.gson.Gson

object VivokaLanguageRepository {
    const val LANGUAGE_CODE_JAPANESE = "ja" // DONE
    const val LANGUAGE_CODE_SPANISH = "es"// DONE
    const val LANGUAGE_CODE_CHINESE_SICHUAN = "zh-CN-SC" // DONE
    const val LANGUAGE_CODE_HINDI = "hi"// DONE
    const val LANGUAGE_CODE_PORTUGUESE = "pt"// DONE
    const val LANGUAGE_CODE_ARABIC_SAUDI = "ar-SA" // DONE (Only Supported Command)
    const val LANGUAGE_CODE_ARABIC_PERSIAN = "ar-APG" // DONE (Only Supported Command)
    const val LANGUAGE_CODE_DANISH = "da" // DONE (Only Supported Command)
    const val LANGUAGE_CODE_DUTCH = "nl"// DONE
    const val LANGUAGE_CODE_BULGARIAN = "bg" // DONE (Only Supported Command)
    const val LANGUAGE_CODE_CZECH = "cs"// DONE
    const val LANGUAGE_CODE_RUSSIAN = "ru"// DONE
    const val LANGUAGE_CODE_POLISH = "pl" // DONE (Only Supported Command)
    const val LANGUAGE_CODE_ITALIAN = "it"// DONE
    const val LANGUAGE_CODE_GERMAN = "de"// DONE
    const val LANGUAGE_CODE_FRENCH = "fr"// DONE
    const val LANGUAGE_CODE_KOREAN = "ko"// DONE
    const val LANGUAGE_CODE_INDONESIAN = "in"// DONE (Only Supported Command)
    const val LANGUAGE_CODE_CANTONESE_CHINA = "zh-CN-yue"// DONE
    const val LANGUAGE_CODE_CANTONESE_HONG_KONG = "zh-HK-yue"// DONE
    const val LANGUAGE_CODE_FINNISH = "fi"// DONE
    const val LANGUAGE_CODE_GREEK = "el"// DONE
    const val LANGUAGE_CODE_HEBREW = "he"// DONE
    const val LANGUAGE_CODE_HUNGARIAN = "hu"// DONE
    const val LANGUAGE_CODE_MANDARIN_CHINA = "zh-CN-cmn"// DONE
    const val LANGUAGE_CODE_MANDARIN_TAIWAN = "zh-TW-cmn"// DONE
    const val LANGUAGE_CODE_NORWEGIAN = "no"// DONE
    const val LANGUAGE_CODE_SLOVAK = "sk"// DONE
    const val LANGUAGE_CODE_SWEDISH = "sv"// DONE
    const val LANGUAGE_CODE_THAI = "th"// DONE
    const val LANGUAGE_CODE_TURKISH = "tr"// DONE
    const val LANGUAGE_CODE_ENGLISH_USA = "en" // DONE
    const val LANGUAGE_CODE_ENGLISH_INDIA = "en-IN" // DONE
    const val LANGUAGE_CODE_ENGLISH_AUSTRALIA = "en-AU" // DONE
    const val LANGUAGE_CODE_ENGLISH_CHINA = "en-CN" // DONE
    const val LANGUAGE_CODE_ENGLISH_JAPAN = "en-JP" // DONE
    const val LANGUAGE_CODE_ENGLISH_MALAYSIA = "en-MY" // DONE
    const val LANGUAGE_CODE_ENGLISH_SOUTH_KOREA = "en-KR" // DONE
    const val LANGUAGE_CODE_ENGLISH_UNITED_KINGDOM = "en-GB" // DONE
    const val LANGUAGE_CODE_FRENCH_CANADA = "fr-CA" // DONE
    const val LANGUAGE_CODE_PORTUGUESE_BRAZIL = "pt-BR" // DONE
    const val LANGUAGE_CODE_SPANISH_SPAIN = "es-SP" // DONE


    const val LANGUAGE_CODE_ENGLISH_BCP_TAG = "en-US"
    private const val LANGUAGE_CODE_JAPANESE_BCP_TAG = "ja-JP"
    private const val LANGUAGE_CODE_SPANISH_BCP_TAG = "es-ES"
    private const val LANGUAGE_CODE_CHINESE_BCP_TAG = "zh-CN"
    private const val LANGUAGE_CODE_HINDI_BCP_TAG = "hi-IN"
    private const val LANGUAGE_CODE_PORTUGUESE_BCP_TAG = "pt-PT"
    private const val LANGUAGE_CODE_ARABIC_BCP_TAG = "ar-SA"
    private const val LANGUAGE_CODE_DANISH_BCP_TAG = "da-DK"
    private const val LANGUAGE_CODE_DUTCH_BCP_TAG = "nl-NL"
    private const val LANGUAGE_CODE_BULGARIAN_BCP_TAG = "bg-BG"
    private const val LANGUAGE_CODE_CZECH_BCP_TAG = "cs-CZ"
    private const val LANGUAGE_CODE_RUSSIAN_BCP_TAG = "ru-RU"
    private const val LANGUAGE_CODE_POLISH_BCP_TAG = "pl-PL"
    private const val LANGUAGE_CODE_ITALIAN_BCP_TAG = "it-IT"
    private const val LANGUAGE_CODE_GERMAN_BCP_TAG = "de-DE"
    private const val LANGUAGE_CODE_FRENCH_BCP_TAG = "fr-FR"
    private const val LANGUAGE_CODE_KOREAN_BCP_TAG = "ko-KR"
    private const val LANGUAGE_CODE_INDONESIAN_BCP_TAG = "id-ID"

    val DYNAMIC_RESOURCE = arrayListOf(
        LANGUAGE_CODE_SPANISH,
        LANGUAGE_CODE_FRENCH,
        LANGUAGE_CODE_HINDI,
        LANGUAGE_CODE_ITALIAN,
        LANGUAGE_CODE_JAPANESE,
        LANGUAGE_CODE_PORTUGUESE,
        LANGUAGE_CODE_RUSSIAN,
        LANGUAGE_CODE_KOREAN,
        LANGUAGE_CODE_GERMAN,
        LANGUAGE_CODE_ARABIC_SAUDI,
        LANGUAGE_CODE_ARABIC_PERSIAN,
        LANGUAGE_CODE_DANISH,
        LANGUAGE_CODE_DUTCH,
        LANGUAGE_CODE_CZECH,
        LANGUAGE_CODE_BULGARIAN,
        LANGUAGE_CODE_POLISH,
        LANGUAGE_CODE_INDONESIAN,
        LANGUAGE_CODE_CHINESE_SICHUAN,
        LANGUAGE_CODE_CANTONESE_CHINA,
        LANGUAGE_CODE_CANTONESE_HONG_KONG,
        LANGUAGE_CODE_FINNISH,
        LANGUAGE_CODE_GREEK,
        LANGUAGE_CODE_HEBREW,
        LANGUAGE_CODE_HUNGARIAN,
        LANGUAGE_CODE_MANDARIN_CHINA,
        LANGUAGE_CODE_MANDARIN_TAIWAN,
        LANGUAGE_CODE_NORWEGIAN,
        LANGUAGE_CODE_SLOVAK,
        LANGUAGE_CODE_SWEDISH,
        LANGUAGE_CODE_THAI,
        LANGUAGE_CODE_TURKISH,
        LANGUAGE_CODE_ENGLISH_INDIA,
        LANGUAGE_CODE_ENGLISH_AUSTRALIA,
        LANGUAGE_CODE_ENGLISH_CHINA,
        LANGUAGE_CODE_ENGLISH_JAPAN,
        LANGUAGE_CODE_ENGLISH_MALAYSIA,
        LANGUAGE_CODE_ENGLISH_SOUTH_KOREA,
        LANGUAGE_CODE_ENGLISH_UNITED_KINGDOM,
        LANGUAGE_CODE_FRENCH_CANADA,
        LANGUAGE_CODE_PORTUGUESE_BRAZIL,
        LANGUAGE_CODE_SPANISH_SPAIN
    )

    fun isLanguageDownloaded(langCode: String, data: String, gson: Gson = Gson()): Boolean {

        if (!DYNAMIC_RESOURCE.contains(langCode)) return true

        if (langCode.isBlank() || data.isBlank()) return false

        return try {
            val downloadedFile = gson.fromJson(data, DownloadFiles::class.java)
            downloadedFile?.files?.contains(langCode) == true
        } catch (e: Exception) {
            android.util.Log.e("LanguageCheck", "Error parsing JSON: ${e.message}", e)
            false
        }
    }

    fun getDownloadLanguageString(langCode: String, data: String?): String {
        val gson = Gson()

        // If data is null or blank, create a new DownloadFiles object
        if (data.isNullOrBlank()) {
            val downloadFiles = DownloadFiles(mutableSetOf())
            if (langCode.isNotBlank()) {
                downloadFiles.files.add(langCode)
            }
            return gson.toJson(downloadFiles)
        }

        // If langCode is blank, return the existing data
        if (langCode.isBlank()) return data

        // Try to parse the existing data and update it
        return try {
            val downloadedFile = gson.fromJson(data, DownloadFiles::class.java)
            if (!downloadedFile.files.contains(langCode)) {
                downloadedFile.files.add(langCode)
            }
            gson.toJson(downloadedFile)
        } catch (e: Exception) {
            android.util.Log.e("LanguageString", "Error parsing JSON: ${e.message}", e)
            data
        }
    }

    fun getBcpLangTag(code: String): String {
        return when (code) {
            LANGUAGE_CODE_JAPANESE -> LANGUAGE_CODE_JAPANESE_BCP_TAG
            LANGUAGE_CODE_SPANISH -> LANGUAGE_CODE_SPANISH_BCP_TAG
            LANGUAGE_CODE_CHINESE_SICHUAN -> LANGUAGE_CODE_CHINESE_BCP_TAG
            LANGUAGE_CODE_HINDI -> LANGUAGE_CODE_HINDI_BCP_TAG
            LANGUAGE_CODE_PORTUGUESE -> LANGUAGE_CODE_PORTUGUESE_BCP_TAG
            LANGUAGE_CODE_ARABIC_SAUDI -> LANGUAGE_CODE_ARABIC_BCP_TAG
            LANGUAGE_CODE_DANISH -> LANGUAGE_CODE_DANISH_BCP_TAG
            LANGUAGE_CODE_DUTCH -> LANGUAGE_CODE_DUTCH_BCP_TAG
            LANGUAGE_CODE_BULGARIAN -> LANGUAGE_CODE_BULGARIAN_BCP_TAG
            LANGUAGE_CODE_CZECH -> LANGUAGE_CODE_CZECH_BCP_TAG
            LANGUAGE_CODE_RUSSIAN -> LANGUAGE_CODE_RUSSIAN_BCP_TAG
            LANGUAGE_CODE_POLISH -> LANGUAGE_CODE_POLISH_BCP_TAG
            LANGUAGE_CODE_ITALIAN -> LANGUAGE_CODE_ITALIAN_BCP_TAG
            LANGUAGE_CODE_GERMAN -> LANGUAGE_CODE_GERMAN_BCP_TAG
            LANGUAGE_CODE_FRENCH -> LANGUAGE_CODE_FRENCH_BCP_TAG
            LANGUAGE_CODE_KOREAN -> LANGUAGE_CODE_KOREAN_BCP_TAG
            LANGUAGE_CODE_INDONESIAN -> LANGUAGE_CODE_INDONESIAN_BCP_TAG
            else -> LANGUAGE_CODE_ENGLISH_BCP_TAG
        }
    }

    private const val ASR_US = "asreng-US"
    private const val ASR_ENG_IN = "asreng-IN"
    private const val ASR_ENG_AU = "asreng-AU"
    private const val ASR_ENG_CN = "asreng-CN"
    private const val ASR_ENG_JP = "asreng-JP"
    private const val ASR_ENG_MY = "asreng-MY"
    private const val ASR_ENG_KR = "asreng-KR"
    private const val ASR_ENG_GB = "asreng-GB"
    private const val ASR_IN = "asrhin-IN"
    private const val ASR_IT = "asrita-IT"
    private const val ASR_JP = "asrjpn-JP"
    private const val ASR_RU = "asrrus-RU"
    private const val ASR_KR = "asrkor-KR"
    private const val ASR_DE = "asrdeu-DE"
    private const val ASR_SA = "asrarb-SA"
    private const val ASR_APG = "asrafb-APG"
    private const val ASR_DK = "asrdan-DK"
    private const val ASR_NL = "asrnld-NL"
    private const val ASR_CZ = "asrces-CZ"
    private const val ASR_BG = "asrbul-BG"
    private const val ASR_PL = "asrpol-PL"
    private const val ASR_ID = "asrind-ID"
    private const val ASR_CN_SC = "asrzho-CN-SC"
    private const val ASR_PT = "asrpor-PT"
    private const val ASR_PT_BR = "asrpor-BR"
    private const val ASR_ES = "asrspa-MX"
    private const val ASR_ES_SP = "asrspa-ES"
    private const val ASR_FR = "asrfra-FR"
    private const val ASR_FR_CA = "asrfra-CA"
    private const val ASR_ZH_CN = "asryue-CN"
    private const val ASR_ZH_HK = "asryue-HK"
    private const val ASR_FI = "asrfin-FI"
    private const val ASR_EL = "esrell-GR"
    private const val ASR_HE = "esrheb-IL"
    private const val ASR_HU = "asrhun-HU"
    private const val ASR_ZH_MNC_CN = "asrcmn-CN"
    private const val ASR_ZH_MNC_TW = "asrcmn-TW"
    private const val ASR_NO = "asrnor-NO"
    private const val ASR_SK = "asrslk-SK"
    private const val ASR_SV = "asrswe-SE"
    private const val ASR_TH = "asrtha-TH"
    private const val ASR_TR = "asrtur-TR"

    private const val FREE_SPEECH_ENGLISH = "FreeSpeech"
    private const val FREE_SPEECH_ENGLISH_INDIA_DICTATION = "english-india-dictation"
    private const val FREE_SPEECH_ENGLISH_CHINA_DICTATION = "english-china-dictation"
    private const val FREE_SPEECH_SPANISH = "spanish-dictation"
    private const val FREE_SPEECH_SPANISH_SPAIN_DICTATION = "spanish-spain-dictation"
    private const val FREE_SPEECH_FRENCH = "french-dictation"
    private const val FREE_SPEECH_FRENCH_CANADA_DICTATION = "french-canada-dictation"
    private const val FREE_SPEECH_HINDI = "hindi-dictation"
    private const val FREE_SPEECH_ITALIAN = "italian-dictation"
    private const val FREE_SPEECH_JAPANESE = "japanese-dictation"
    private const val FREE_SPEECH_PORTUGUESE = "portuguese-dictation"
    private const val FREE_SPEECH_PORTUGUESE_BRAZIL_DICTATION = "portuguese-brazil-dictation"
    private const val FREE_SPEECH_RUSSIAN = "russian-dictation"
    private const val FREE_SPEECH_KOREAN = "korean-dictation"
    private const val FREE_SPEECH_GERMAN = "german-dictation"
    private const val FREE_SPEECH_DUTCH = "dutch-dictation"
    private const val FREE_SPEECH_CZECH = "czech-dictation"
    private const val FREE_SPEECH_CHINESE_CHINA_SICHUAN_DICTATION = "chinese-china-sichuan-dictation"
    private const val FREE_SPEECH_CANTONESE_HONG_KONG_DICTATION = "cantonese-hong-kong-dictation"
    private const val FREE_SPEECH_CANTONESE_CHINA_DICTATION = "cantonese-china-dictation"
    private const val FREE_SPEECH_HUNGARIAN = "hungarian-dictation"
    private const val FREE_SPEECH_MANDARIN_TAIWAN_DICTATION = "mandarin-taiwan-dictation"
    private const val FREE_SPEECH_MANDARIN_CHINA_DICTATION = "mandarin-china-dictation"

    private const val FREE_SPEECH_THAI = "thai-dictation"

    private val languageConfigMap = mapOf(
        LANGUAGE_CODE_HINDI to LanguageModelConfig(ASR_IN, FREE_SPEECH_HINDI),
        LANGUAGE_CODE_ITALIAN to LanguageModelConfig(ASR_IT, FREE_SPEECH_ITALIAN),
        LANGUAGE_CODE_JAPANESE to LanguageModelConfig(ASR_JP, FREE_SPEECH_JAPANESE),
        LANGUAGE_CODE_PORTUGUESE to LanguageModelConfig(ASR_PT, FREE_SPEECH_PORTUGUESE),
        LANGUAGE_CODE_RUSSIAN to LanguageModelConfig(ASR_RU, FREE_SPEECH_RUSSIAN),
        LANGUAGE_CODE_KOREAN to LanguageModelConfig(ASR_KR, FREE_SPEECH_KOREAN),
        LANGUAGE_CODE_GERMAN to LanguageModelConfig(ASR_DE, FREE_SPEECH_GERMAN),
        LANGUAGE_CODE_DUTCH to LanguageModelConfig(ASR_NL, FREE_SPEECH_DUTCH),
        LANGUAGE_CODE_CZECH to LanguageModelConfig(ASR_CZ, FREE_SPEECH_CZECH),
        LANGUAGE_CODE_CHINESE_SICHUAN to LanguageModelConfig(ASR_CN_SC, FREE_SPEECH_CHINESE_CHINA_SICHUAN_DICTATION),
        LANGUAGE_CODE_SPANISH to LanguageModelConfig(ASR_ES, FREE_SPEECH_SPANISH),
        LANGUAGE_CODE_FRENCH to LanguageModelConfig(ASR_FR, FREE_SPEECH_FRENCH),
        LANGUAGE_CODE_CANTONESE_CHINA to LanguageModelConfig(ASR_ZH_CN, FREE_SPEECH_CANTONESE_CHINA_DICTATION),
        LANGUAGE_CODE_CANTONESE_HONG_KONG to LanguageModelConfig(ASR_ZH_HK, FREE_SPEECH_CANTONESE_HONG_KONG_DICTATION),
        LANGUAGE_CODE_HUNGARIAN to LanguageModelConfig(ASR_HU, FREE_SPEECH_HUNGARIAN),
        LANGUAGE_CODE_MANDARIN_CHINA to LanguageModelConfig(ASR_ZH_MNC_CN, FREE_SPEECH_MANDARIN_CHINA_DICTATION),
        LANGUAGE_CODE_MANDARIN_TAIWAN to LanguageModelConfig(ASR_ZH_MNC_TW, FREE_SPEECH_MANDARIN_TAIWAN_DICTATION),
        LANGUAGE_CODE_THAI to LanguageModelConfig(ASR_TH, FREE_SPEECH_THAI),
        LANGUAGE_CODE_ENGLISH_INDIA to LanguageModelConfig(ASR_ENG_IN, FREE_SPEECH_ENGLISH_INDIA_DICTATION),
        LANGUAGE_CODE_ENGLISH_CHINA to LanguageModelConfig(ASR_ENG_CN, FREE_SPEECH_ENGLISH_CHINA_DICTATION),
        LANGUAGE_CODE_FRENCH_CANADA to LanguageModelConfig(ASR_FR_CA, FREE_SPEECH_FRENCH_CANADA_DICTATION),
        LANGUAGE_CODE_PORTUGUESE_BRAZIL to LanguageModelConfig(ASR_PT_BR, FREE_SPEECH_PORTUGUESE_BRAZIL_DICTATION),
        LANGUAGE_CODE_SPANISH_SPAIN to LanguageModelConfig(ASR_ES_SP, FREE_SPEECH_SPANISH_SPAIN_DICTATION),

        // Languages with ASR but no specific dictation model fall back to English dictation
        LANGUAGE_CODE_ARABIC_SAUDI to LanguageModelConfig(ASR_SA, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_ARABIC_PERSIAN to LanguageModelConfig(ASR_APG, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_DANISH to LanguageModelConfig(ASR_DK, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_BULGARIAN to LanguageModelConfig(ASR_BG, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_POLISH to LanguageModelConfig(ASR_PL, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_INDONESIAN to LanguageModelConfig(ASR_ID, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_FINNISH to LanguageModelConfig(ASR_FI, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_GREEK to LanguageModelConfig(ASR_EL, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_HEBREW to LanguageModelConfig(ASR_HE, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_NORWEGIAN to LanguageModelConfig(ASR_NO, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_SLOVAK to LanguageModelConfig(ASR_SK, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_SWEDISH to LanguageModelConfig(ASR_SV, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_TURKISH to LanguageModelConfig(ASR_TR, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_ENGLISH_AUSTRALIA to LanguageModelConfig(ASR_ENG_AU, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_ENGLISH_JAPAN to LanguageModelConfig(ASR_ENG_JP, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_ENGLISH_MALAYSIA to LanguageModelConfig(ASR_ENG_MY, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_ENGLISH_SOUTH_KOREA to LanguageModelConfig(ASR_ENG_KR, FREE_SPEECH_ENGLISH),
        LANGUAGE_CODE_ENGLISH_UNITED_KINGDOM to LanguageModelConfig(ASR_ENG_GB, FREE_SPEECH_ENGLISH)


    )

    private val defaultLanguageConfig = LanguageModelConfig(ASR_US, FREE_SPEECH_ENGLISH)

    /**
     * Retrieves the ASR model identifier for the selected language.
     */
    fun getAsr(selectedLanguageCode: String): String {
        // Look up the config in the map, or use the default if not found.
        return languageConfigMap[selectedLanguageCode]?.asrModel ?: defaultLanguageConfig.asrModel
    }

    /**
     * Retrieves the ASR model identifier wrapped in a list.
     */
    fun getModelAsr(selectedLanguageCode: String): List<String> {
        // This function can now be simplified to just call the other.
        return listOf(getAsr(selectedLanguageCode))
    }

    /**
     * Retrieves the dictation language model based on the selected language.
     */
    fun getDictationLanguage(selectedLanguageCode: String): List<String> {
        // Look up the config, or use the default if not found.
        val model = languageConfigMap[selectedLanguageCode]?.dictationModel ?: defaultLanguageConfig.dictationModel
        return listOf(model)
    }
}

data class DownloadFiles(val files: MutableSet<String>)

data class LanguageModelConfig(
    val asrModel: String,
    val dictationModel: String
)
