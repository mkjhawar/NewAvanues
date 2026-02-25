/**
 * LocalizationModule.kt - Direct implementation localization manager
 * 
 * Author: Manoj Jhawar
 * Created: 2025-08-22
 */

package com.augmentalis.voiceoscore.managers.localizationmanager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * Localization Module - Direct implementation, manages languages and translations
 */
open class LocalizationModule(private val context: Context) {
    companion object {
        private const val TAG = "LocalizationModule"
        const val MODULE_ID = "localization"
        const val MODULE_VERSION = "1.0.0"
        
        // Supported languages
        val VOSK_LANGUAGES = setOf("en", "es", "fr", "de", "ru", "zh", "ja", "ko")
        val VIVOKA_LANGUAGES = mapOf(
            "en" to "English",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "it" to "Italian",
            "pt" to "Portuguese",
            "ru" to "Russian",
            "zh" to "Chinese",
            "ja" to "Japanese",
            "ko" to "Korean",
            "ar" to "Arabic",
            "nl" to "Dutch",
            "pl" to "Polish",
            "tr" to "Turkish",
            "hi" to "Hindi",
            "th" to "Thai",
            "cs" to "Czech",
            "da" to "Danish",
            "fi" to "Finnish",
            "el" to "Greek",
            "he" to "Hebrew",
            "hu" to "Hungarian",
            "no" to "Norwegian",
            "sv" to "Swedish",
            "uk" to "Ukrainian",
            "bg" to "Bulgarian",
            "hr" to "Croatian",
            "ro" to "Romanian",
            "sk" to "Slovak",
            "sl" to "Slovenian",
            "et" to "Estonian",
            "lv" to "Latvian",
            "lt" to "Lithuanian",
            "is" to "Icelandic",
            "ga" to "Irish",
            "mt" to "Maltese",
            "sq" to "Albanian",
            "mk" to "Macedonian",
            "sr" to "Serbian",
            "bs" to "Bosnian",
            "cy" to "Welsh"
        )
        
        @Volatile
        private var instance: LocalizationModule? = null
        
        fun getInstance(context: Context): LocalizationModule {
            return instance ?: synchronized(this) {
                instance ?: LocalizationModule(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private var isReady = false
    private var currentLanguage = "en"
    private lateinit var languageManager: LanguageManager
    private val translationManager = TranslationManager()
    
    private val _languageState = MutableStateFlow(currentLanguage)
    val languageState: StateFlow<String> = _languageState.asStateFlow()
    
    // Direct properties - no interface
    val name: String = "Localization"
    val version: String = MODULE_VERSION
    val description: String = "Multi-language support with 42+ languages"
    
    fun getDependencies(): List<String> = emptyList()
    
    fun initialize(): Boolean {
        if (isReady) return true
        
        return try {
            // Initialize language manager
            languageManager = LanguageManager(context)
            
            // Load saved language preference
            currentLanguage = languageManager.getSavedLanguage()
            _languageState.value = currentLanguage
            
            // Load translations for current language
            translationManager.loadTranslations(currentLanguage)
            
            isReady = true
            Log.d(TAG, "Localization module initialized with language: $currentLanguage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize localization module", e)
            false
        }
    }
    
    fun shutdown() {
        isReady = false
        instance = null
        Log.d(TAG, "Localization module shutdown")
    }
    
    fun isReady(): Boolean = isReady
    
    // Localization specific methods
    
    fun getCurrentLanguage(): String = currentLanguage
    
    fun getSupportedLanguages(): Set<String> = VIVOKA_LANGUAGES.keys
    
    fun isLanguageSupported(languageCode: String): Boolean {
        return languageCode in getSupportedLanguages()
    }
    
    fun setLanguage(languageCode: String): Boolean {
        if (!isLanguageSupported(languageCode)) {
            Log.e(TAG, "Unsupported language: $languageCode")
            return false
        }
        
        val oldLanguage = currentLanguage
        currentLanguage = languageCode
        _languageState.value = languageCode
        
        // Save preference
        languageManager.saveLanguage(languageCode)
        
        // Load new translations
        translationManager.loadTranslations(languageCode)
        
        Log.d(TAG, "Language changed from $oldLanguage to $languageCode")
        return true
    }
    
    fun translate(key: String, vararg args: Any): String {
        return translationManager.translate(key, currentLanguage, *args)
    }
    
    fun getLanguageDisplayName(languageCode: String): String {
        return VIVOKA_LANGUAGES[languageCode] ?: languageCode
    }
    
    fun isVoskSupported(languageCode: String): Boolean {
        return languageCode in VOSK_LANGUAGES
    }
    
    fun isVivokaSupported(languageCode: String): Boolean {
        return languageCode in VIVOKA_LANGUAGES.keys
    }
}

/**
 * Language preference manager
 */
open class LanguageManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "voiceos_localization"
        private const val KEY_LANGUAGE = "current_language"
        private const val DEFAULT_LANGUAGE = "en"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun getSavedLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    fun saveLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
}

/**
 * Translation manager for localized strings
 */
open class TranslationManager {
    private val translations = mutableMapOf<String, Map<String, String>>()
    
    init {
        // Load default English translations
        loadDefaultTranslations()
    }
    
    fun loadTranslations(languageCode: String) {
        // In a real implementation, this would load from files or resources
        // For now, we'll use hardcoded translations for key commands
        when (languageCode) {
            "es" -> loadSpanishTranslations()
            "fr" -> loadFrenchTranslations()
            "de" -> loadGermanTranslations()
            "ja" -> loadJapaneseTranslations()
            "zh" -> loadChineseTranslations()
            else -> loadDefaultTranslations()
        }
    }
    
    fun translate(key: String, languageCode: String, vararg args: Any): String {
        val langTranslations = translations[languageCode] ?: translations["en"] ?: emptyMap()
        val template = langTranslations[key] ?: key
        
        // Simple string formatting
        return if (args.isEmpty()) {
            template
        } else {
            String.format(template, *args)
        }
    }
    
    private fun loadDefaultTranslations() {
        translations["en"] = mapOf(
            "cmd.back" to "back",
            "cmd.home" to "home",
            "cmd.click" to "click",
            "cmd.scroll_up" to "scroll up",
            "cmd.scroll_down" to "scroll down",
            "cmd.open_app" to "open %s",
            "cmd.volume_up" to "volume up",
            "cmd.volume_down" to "volume down",
            "cmd.start_dictation" to "start dictation",
            "cmd.stop_dictation" to "stop dictation",
            "msg.listening" to "Listening...",
            "msg.processing" to "Processing...",
            "msg.error" to "Error: %s",
            "msg.ready" to "VoiceOS Ready",
            // HUD-specific translations
            "hud.notification.incoming_call" to "Incoming call from %s",
            "hud.notification.message" to "New message from %s",
            "hud.notification.battery_low" to "Battery low: %d%%",
            "hud.notification.app_opened" to "%s opened",
            "hud.notification.voice_command" to "Command: %s",
            "hud.notification.gaze_target" to "Looking at: %s",
            "hud.mode.standard" to "Standard Mode",
            "hud.mode.meeting" to "Meeting Mode",
            "hud.mode.driving" to "Driving Mode",
            "hud.mode.workshop" to "Workshop Mode",
            "hud.mode.accessibility" to "Accessibility Mode",
            "hud.mode.gaming" to "Gaming Mode",
            "hud.mode.entertainment" to "Entertainment Mode",
            "hud.status.connected" to "Connected",
            "hud.status.disconnected" to "Disconnected",
            "hud.status.loading" to "Loading...",
            "hud.status.ready" to "HUD Ready"
        )
    }
    
    private fun loadSpanishTranslations() {
        translations["es"] = mapOf(
            "cmd.back" to "atrás",
            "cmd.home" to "inicio",
            "cmd.click" to "clic",
            "cmd.scroll_up" to "desplazar arriba",
            "cmd.scroll_down" to "desplazar abajo",
            "cmd.open_app" to "abrir %s",
            "cmd.volume_up" to "subir volumen",
            "cmd.volume_down" to "bajar volumen",
            "cmd.start_dictation" to "iniciar dictado",
            "cmd.stop_dictation" to "detener dictado",
            "msg.listening" to "Escuchando...",
            "msg.processing" to "Procesando...",
            "msg.error" to "Error: %s",
            "msg.ready" to "VoiceOS Listo",
            // HUD-specific translations
            "hud.notification.incoming_call" to "Llamada entrante de %s",
            "hud.notification.message" to "Nuevo mensaje de %s",
            "hud.notification.battery_low" to "Batería baja: %d%%",
            "hud.notification.app_opened" to "%s abierto",
            "hud.notification.voice_command" to "Comando: %s",
            "hud.notification.gaze_target" to "Mirando a: %s",
            "hud.mode.standard" to "Modo Estándar",
            "hud.mode.meeting" to "Modo Reunión",
            "hud.mode.driving" to "Modo Conducción",
            "hud.mode.workshop" to "Modo Taller",
            "hud.mode.accessibility" to "Modo Accesibilidad",
            "hud.mode.gaming" to "Modo Juegos",
            "hud.mode.entertainment" to "Modo Entretenimiento",
            "hud.status.connected" to "Conectado",
            "hud.status.disconnected" to "Desconectado",
            "hud.status.loading" to "Cargando...",
            "hud.status.ready" to "HUD Listo"
        )
    }
    
    private fun loadFrenchTranslations() {
        translations["fr"] = mapOf(
            "cmd.back" to "retour",
            "cmd.home" to "accueil",
            "cmd.click" to "cliquer",
            "cmd.scroll_up" to "faire défiler vers le haut",
            "cmd.scroll_down" to "faire défiler vers le bas",
            "cmd.open_app" to "ouvrir %s",
            "cmd.volume_up" to "augmenter le volume",
            "cmd.volume_down" to "diminuer le volume",
            "cmd.start_dictation" to "commencer la dictée",
            "cmd.stop_dictation" to "arrêter la dictée",
            "msg.listening" to "Écoute...",
            "msg.processing" to "Traitement...",
            "msg.error" to "Erreur: %s",
            "msg.ready" to "VoiceOS Prêt"
        )
    }
    
    private fun loadGermanTranslations() {
        translations["de"] = mapOf(
            "cmd.back" to "zurück",
            "cmd.home" to "startseite",
            "cmd.click" to "klicken",
            "cmd.scroll_up" to "nach oben scrollen",
            "cmd.scroll_down" to "nach unten scrollen",
            "cmd.open_app" to "%s öffnen",
            "cmd.volume_up" to "lauter",
            "cmd.volume_down" to "leiser",
            "cmd.start_dictation" to "diktat starten",
            "cmd.stop_dictation" to "diktat beenden",
            "msg.listening" to "Höre zu...",
            "msg.processing" to "Verarbeite...",
            "msg.error" to "Fehler: %s",
            "msg.ready" to "VoiceOS Bereit"
        )
    }
    
    private fun loadJapaneseTranslations() {
        translations["ja"] = mapOf(
            "cmd.back" to "戻る",
            "cmd.home" to "ホーム",
            "cmd.click" to "クリック",
            "cmd.scroll_up" to "上にスクロール",
            "cmd.scroll_down" to "下にスクロール",
            "cmd.open_app" to "%sを開く",
            "cmd.volume_up" to "音量を上げる",
            "cmd.volume_down" to "音量を下げる",
            "cmd.start_dictation" to "音声入力開始",
            "cmd.stop_dictation" to "音声入力終了",
            "msg.listening" to "聞いています...",
            "msg.processing" to "処理中...",
            "msg.error" to "エラー: %s",
            "msg.ready" to "VoiceOS 準備完了"
        )
    }
    
    private fun loadChineseTranslations() {
        translations["zh"] = mapOf(
            "cmd.back" to "返回",
            "cmd.home" to "主页",
            "cmd.click" to "点击",
            "cmd.scroll_up" to "向上滚动",
            "cmd.scroll_down" to "向下滚动",
            "cmd.open_app" to "打开%s",
            "cmd.volume_up" to "增加音量",
            "cmd.volume_down" to "降低音量",
            "cmd.start_dictation" to "开始听写",
            "cmd.stop_dictation" to "停止听写",
            "msg.listening" to "正在聆听...",
            "msg.processing" to "处理中...",
            "msg.error" to "错误：%s",
            "msg.ready" to "VoiceOS 就绪"
        )
    }
}