/**
 * TranslationProvider.kt - Translation data provider
 *
 * Contains all translation key-value pairs for supported languages.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.localization

/**
 * Translation provider - stores and retrieves translations
 */
object TranslationProvider {
    private val translations = mutableMapOf<String, Map<String, String>>()

    init {
        loadAllTranslations()
    }

    /**
     * Get translation for key in specified language
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
     * Simple string formatting (cross-platform compatible)
     */
    private fun formatString(template: String, args: Array<out Any>): String {
        var result = template
        args.forEachIndexed { index, arg ->
            result = result.replaceFirst("%s", arg.toString())
            result = result.replaceFirst("%d", arg.toString())
        }
        return result
    }

    private fun loadAllTranslations() {
        // English (default)
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

        // Spanish
        translations["es"] = mapOf(
            "cmd.back" to "atras",
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
            "hud.notification.battery_low" to "Bateria baja: %d%%",
            "hud.notification.app_opened" to "%s abierto",
            "hud.mode.standard" to "Modo Estandar",
            "hud.mode.meeting" to "Modo Reunion",
            "hud.mode.driving" to "Modo Conduccion",
            "hud.status.connected" to "Conectado",
            "hud.status.disconnected" to "Desconectado",
            "hud.status.ready" to "HUD Listo"
        )

        // French
        translations["fr"] = mapOf(
            "cmd.back" to "retour",
            "cmd.home" to "accueil",
            "cmd.click" to "cliquer",
            "cmd.scroll_up" to "faire defiler vers le haut",
            "cmd.scroll_down" to "faire defiler vers le bas",
            "cmd.open_app" to "ouvrir %s",
            "cmd.volume_up" to "augmenter le volume",
            "cmd.volume_down" to "diminuer le volume",
            "msg.listening" to "Ecoute...",
            "msg.processing" to "Traitement...",
            "msg.error" to "Erreur: %s",
            "msg.ready" to "VoiceOS Pret"
        )

        // German
        translations["de"] = mapOf(
            "cmd.back" to "zuruck",
            "cmd.home" to "startseite",
            "cmd.click" to "klicken",
            "cmd.scroll_up" to "nach oben scrollen",
            "cmd.scroll_down" to "nach unten scrollen",
            "cmd.open_app" to "%s offnen",
            "cmd.volume_up" to "lauter",
            "cmd.volume_down" to "leiser",
            "msg.listening" to "Hore zu...",
            "msg.processing" to "Verarbeite...",
            "msg.error" to "Fehler: %s",
            "msg.ready" to "VoiceOS Bereit"
        )

        // Japanese
        translations["ja"] = mapOf(
            "cmd.back" to "戻る",
            "cmd.home" to "ホーム",
            "cmd.click" to "クリック",
            "cmd.scroll_up" to "上にスクロール",
            "cmd.scroll_down" to "下にスクロール",
            "cmd.open_app" to "%sを開く",
            "msg.listening" to "聞いています...",
            "msg.processing" to "処理中...",
            "msg.error" to "エラー: %s",
            "msg.ready" to "VoiceOS 準備完了"
        )

        // Chinese
        translations["zh"] = mapOf(
            "cmd.back" to "返回",
            "cmd.home" to "主页",
            "cmd.click" to "点击",
            "cmd.scroll_up" to "向上滚动",
            "cmd.scroll_down" to "向下滚动",
            "cmd.open_app" to "打开%s",
            "msg.listening" to "正在聆听...",
            "msg.processing" to "处理中...",
            "msg.error" to "错误：%s",
            "msg.ready" to "VoiceOS 就绪"
        )
    }
}
