/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Desktop implementation of TTS Coordinator using FreeTTS or system TTS.
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import com.augmentalis.chat.tts.TTSSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop (JVM) implementation of ITTSCoordinator.
 *
 * On desktop platforms, TTS can be implemented using:
 * - FreeTTS (pure Java, cross-platform)
 * - SAPI (Windows via JNI)
 * - macOS say command via ProcessBuilder
 * - Linux espeak via ProcessBuilder
 *
 * This implementation provides a stub that can be extended
 * with actual TTS functionality as needed.
 *
 * @author Manoj Jhawar
 * @since 2025-01-16
 */
class TTSCoordinatorDesktop : ITTSCoordinator {

    // ==================== State ====================

    private val _isTTSReady = MutableStateFlow(false)
    override val isTTSReady: StateFlow<Boolean> = _isTTSReady.asStateFlow()

    private val _isTTSSpeaking = MutableStateFlow(false)
    override val isTTSSpeaking: StateFlow<Boolean> = _isTTSSpeaking.asStateFlow()

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    override val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private val _ttsSettings = MutableStateFlow(TTSSettings.DEFAULT)
    override val ttsSettings: StateFlow<TTSSettings> = _ttsSettings.asStateFlow()

    // Platform detection
    private val osName = System.getProperty("os.name").lowercase()
    private val isMacOS = osName.contains("mac")
    private val isWindows = osName.contains("win")
    private val isLinux = osName.contains("linux")

    // Current speech process (for cancellation)
    private var currentProcess: Process? = null

    init {
        // Initialize TTS based on platform
        initializeTTS()
    }

    private fun initializeTTS() {
        // Check if platform-specific TTS is available
        val available = when {
            isMacOS -> checkMacOSTTS()
            isWindows -> checkWindowsTTS()
            isLinux -> checkLinuxTTS()
            else -> false
        }
        _isTTSReady.value = available
        if (available) {
            println("[TTSCoordinatorDesktop] TTS initialized for ${getPlatformName()}")
        } else {
            println("[TTSCoordinatorDesktop] TTS not available on ${getPlatformName()}")
        }
    }

    private fun getPlatformName(): String = when {
        isMacOS -> "macOS"
        isWindows -> "Windows"
        isLinux -> "Linux"
        else -> "Unknown"
    }

    private fun checkMacOSTTS(): Boolean {
        return try {
            val process = ProcessBuilder("which", "say").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun checkWindowsTTS(): Boolean {
        // Windows SAPI is generally available on all Windows versions
        return isWindows
    }

    private fun checkLinuxTTS(): Boolean {
        return try {
            val process = ProcessBuilder("which", "espeak").start()
            if (process.waitFor() == 0) return true

            val process2 = ProcessBuilder("which", "espeak-ng").start()
            process2.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Operations ====================

    override fun speak(
        text: String,
        messageId: String?,
        onComplete: (() -> Unit)?
    ): Result<Unit> {
        if (!_isTTSReady.value) {
            return Result.Error(
                exception = IllegalStateException("TTS not ready"),
                message = "TTS is not available on this platform"
            )
        }

        if (!_ttsSettings.value.enabled) {
            return Result.Error(
                exception = IllegalStateException("TTS disabled"),
                message = "TTS is disabled in settings"
            )
        }

        return try {
            // Stop any current speech
            stop()

            _isTTSSpeaking.value = true
            _speakingMessageId.value = messageId

            // Build platform-specific command
            val command = buildSpeakCommand(text)

            // Execute speech in background thread
            Thread {
                try {
                    currentProcess = ProcessBuilder(command).start()
                    currentProcess?.waitFor()
                } catch (e: Exception) {
                    System.err.println("[TTSCoordinatorDesktop] Speech error: ${e.message}")
                } finally {
                    _isTTSSpeaking.value = false
                    _speakingMessageId.value = null
                    currentProcess = null
                    onComplete?.invoke()
                }
            }.start()

            Result.Success(Unit)
        } catch (e: Exception) {
            _isTTSSpeaking.value = false
            _speakingMessageId.value = null
            Result.Error(
                exception = e,
                message = "Failed to speak: ${e.message}"
            )
        }
    }

    private fun buildSpeakCommand(text: String): List<String> {
        val rate = _ttsSettings.value.speechRate
        val cleanText = text.replace("\"", "\\\"")

        return when {
            isMacOS -> {
                // macOS say command
                // Rate: 175 words per minute is normal, scale by speechRate
                val wpm = (175 * rate).toInt()
                listOf("say", "-r", wpm.toString(), cleanText)
            }
            isWindows -> {
                // Windows PowerShell with SAPI
                val script = """
                    Add-Type -AssemblyName System.Speech
                    ${'$'}synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
                    ${'$'}synth.Rate = ${((rate - 1) * 5).toInt().coerceIn(-10, 10)}
                    ${'$'}synth.Speak("$cleanText")
                """.trimIndent()
                listOf("powershell", "-Command", script)
            }
            isLinux -> {
                // espeak or espeak-ng
                // Speed: 175 is normal, scale by speechRate
                val speed = (175 * rate).toInt()
                listOf("espeak", "-s", speed.toString(), cleanText)
            }
            else -> listOf("echo", "TTS not supported")
        }
    }

    override fun stop() {
        currentProcess?.let { process ->
            try {
                process.destroyForcibly()
            } catch (e: Exception) {
                System.err.println("[TTSCoordinatorDesktop] Error stopping speech: ${e.message}")
            }
        }
        currentProcess = null
        _isTTSSpeaking.value = false
        _speakingMessageId.value = null
    }

    override fun isAutoSpeakEnabled(): Boolean {
        return _ttsSettings.value.autoSpeak
    }

    override fun clearSpeakingMessageId() {
        _speakingMessageId.value = null
    }

    override fun setSpeakingMessageId(messageId: String?) {
        _speakingMessageId.value = messageId
    }

    // ==================== Settings Access ====================

    override fun getSpeechRate(): Float {
        return _ttsSettings.value.speechRate
    }

    override fun getPitch(): Float {
        return _ttsSettings.value.pitch
    }

    override fun isReady(): Boolean {
        return _isTTSReady.value
    }

    // ==================== Settings Toggles ====================

    override fun toggleEnabled() {
        _ttsSettings.value = _ttsSettings.value.copy(
            enabled = !_ttsSettings.value.enabled
        )
        if (!_ttsSettings.value.enabled) {
            stop()
        }
    }

    override fun toggleAutoSpeak() {
        _ttsSettings.value = _ttsSettings.value.copy(
            autoSpeak = !_ttsSettings.value.autoSpeak
        )
    }

    /**
     * Update TTS settings.
     *
     * @param settings New TTS settings
     */
    fun updateSettings(settings: TTSSettings) {
        _ttsSettings.value = settings
    }

    companion object {
        @Volatile
        private var INSTANCE: TTSCoordinatorDesktop? = null

        /**
         * Get singleton instance of TTSCoordinatorDesktop.
         *
         * @return Singleton instance
         */
        fun getInstance(): TTSCoordinatorDesktop {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TTSCoordinatorDesktop().also {
                    INSTANCE = it
                }
            }
        }
    }
}
