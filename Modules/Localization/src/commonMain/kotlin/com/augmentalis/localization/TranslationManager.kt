/**
 * TranslationManager.kt - Platform-agnostic translation manager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-22
 * Migrated to KMP: 2026-01-28
 */
package com.augmentalis.localization

/**
 * Translation manager for localized strings.
 *
 * Manages translation dictionaries for multiple languages and provides
 * string formatting with placeholders.
 */
class TranslationManager {
    private val translations = mutableMapOf<String, Map<String, String>>()

    init {
        loadDefaultTranslations()
    }

    /**
     * Load translations for a specific language
     */
    fun loadTranslations(languageCode: String) {
        when (languageCode) {
            "es" -> loadSpanishTranslations()
            "fr" -> loadFrenchTranslations()
            "de" -> loadGermanTranslations()
            "ja" -> loadJapaneseTranslations()
            "zh" -> loadChineseTranslations()
            else -> loadDefaultTranslations()
        }
    }

    /**
     * Translate a key to the specified language
     *
     * @param key Translation key
     * @param languageCode Target language code
     * @param args Format arguments for placeholders
     * @return Translated string or the key if not found
     */
    fun translate(key: String, languageCode: String, vararg args: Any): String {
        val langTranslations = translations[languageCode] ?: translations["en"] ?: emptyMap()
        val template = langTranslations[key] ?: key

        return if (args.isEmpty()) {
            template
        } else {
            formatString(template, args)
        }
    }

    /**
     * Get all translation keys for a language
     */
    fun getKeys(languageCode: String): Set<String> {
        return translations[languageCode]?.keys ?: translations["en"]?.keys ?: emptySet()
    }

    /**
     * Check if a translation exists for a key
     */
    fun hasTranslation(key: String, languageCode: String): Boolean {
        return translations[languageCode]?.containsKey(key) == true ||
               translations["en"]?.containsKey(key) == true
    }

    /**
     * Simple format string implementation for KMP compatibility
     */
    private fun formatString(template: String, args: Array<out Any>): String {
        var result = template
        args.forEachIndexed { index, arg ->
            // Replace %s, %d, etc. with the argument
            result = result.replaceFirst(Regex("%[sd]"), arg.toString())
        }
        return result
    }

    private fun loadDefaultTranslations() {
        translations["en"] = mapOf(
            // Voice commands
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
            // Status messages
            "msg.listening" to "Listening...",
            "msg.processing" to "Processing...",
            "msg.error" to "Error: %s",
            "msg.ready" to "VoiceOS Ready",
            // HUD notifications
            "hud.notification.incoming_call" to "Incoming call from %s",
            "hud.notification.message" to "New message from %s",
            "hud.notification.battery_low" to "Battery low: %d%%",
            "hud.notification.app_opened" to "%s opened",
            "hud.notification.voice_command" to "Command: %s",
            "hud.notification.gaze_target" to "Looking at: %s",
            // HUD modes
            "hud.mode.standard" to "Standard Mode",
            "hud.mode.meeting" to "Meeting Mode",
            "hud.mode.driving" to "Driving Mode",
            "hud.mode.workshop" to "Workshop Mode",
            "hud.mode.accessibility" to "Accessibility Mode",
            "hud.mode.gaming" to "Gaming Mode",
            "hud.mode.entertainment" to "Entertainment Mode",
            // HUD status
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
