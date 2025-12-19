package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.functions.PluginTier

/**
 * Platform delegate interface for VoiceModule.
 * Platform implementations provide actual speech recognition/synthesis.
 */
interface VoiceModuleDelegate {
    /**
     * Start listening for speech.
     * @param options Recognition options (e.g., language, continuous, etc.)
     * @return RecognitionResult with transcript and confidence
     */
    suspend fun listen(options: Map<String, Any?>): Map<String, Any?>

    /**
     * Convert text to speech.
     * @param text Text to speak
     * @param options TTS options (e.g., voice, rate, pitch)
     */
    suspend fun speak(text: String, options: Map<String, Any?>)

    /**
     * Check if currently listening.
     */
    fun isListening(): Boolean

    /**
     * Stop listening/speaking.
     */
    fun stop()

    /**
     * Get list of available speech engines.
     * @return List of engine names
     */
    fun getEngines(): List<String>

    /**
     * Set active speech engine.
     * @param name Engine name
     */
    fun setEngine(name: String)

    /**
     * Enable wake word detection.
     * @param word Wake word to detect
     */
    suspend fun enableWakeWord(word: String)

    /**
     * Disable wake word detection.
     */
    fun disableWakeWord()

    /**
     * Start continuous dictation mode.
     * @return Session ID for tracking
     */
    suspend fun startDictation(): String

    /**
     * Stop dictation and return accumulated text.
     * @return Dictated text
     */
    fun stopDictation(): String
}

/**
 * VoiceModule - Wraps SpeechRecognition library for AvaCode.
 *
 * Provides speech recognition and synthesis capabilities to MEL plugins.
 *
 * Usage in MEL:
 * ```
 * # DATA tier methods
 * @voice.listen()                    # Start listening, return result
 * @voice.speak("Hello world")        # Text-to-speech
 * @voice.isListening()               # Check listening state
 * @voice.stop()                      # Stop listening/speaking
 * @voice.engines()                   # List available engines
 *
 * # LOGIC tier methods
 * @voice.setEngine("google")         # Change speech engine
 * @voice.wake.enable("hey ava")      # Enable wake word
 * @voice.wake.disable()              # Disable wake word
 * @voice.dictate.start()             # Start dictation mode
 * @voice.dictate.stop()              # Stop and return text
 * ```
 *
 * @param delegate Platform implementation (null for unsupported platforms)
 */
class VoiceModule(
    private val delegate: VoiceModuleDelegate?
) : BaseModule(
    name = "voice",
    version = "1.0.0",
    minimumTier = PluginTier.DATA
) {

    init {
        // ========== DATA Tier Methods ==========

        registerMethod(
            name = "listen",
            tier = PluginTier.DATA,
            description = "Start listening for speech and return recognition result",
            returnType = "Map<String, Any?>",
            parameters = listOf(
                MethodParameter(
                    name = "options",
                    type = "Map<String, Any?>",
                    required = false,
                    description = "Recognition options (language, continuous, maxAlternatives, etc.)"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val options = args.argOptions(0)
                delegate!!.listen(options)
            }
        )

        registerMethod(
            name = "speak",
            tier = PluginTier.DATA,
            description = "Convert text to speech",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "text",
                    type = "String",
                    required = true,
                    description = "Text to speak"
                ),
                MethodParameter(
                    name = "options",
                    type = "Map<String, Any?>",
                    required = false,
                    description = "TTS options (voice, rate, pitch, volume)"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val text = args.argString(0, "text")
                val options = args.argOptions(1)
                delegate!!.speak(text, options)
            }
        )

        registerMethod(
            name = "isListening",
            tier = PluginTier.DATA,
            description = "Check if currently listening for speech",
            returnType = "Boolean",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.isListening()
            }
        )

        registerMethod(
            name = "stop",
            tier = PluginTier.DATA,
            description = "Stop listening or speaking",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.stop()
            }
        )

        registerMethod(
            name = "engines",
            tier = PluginTier.DATA,
            description = "List available speech engines",
            returnType = "List<String>",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getEngines()
            }
        )

        // ========== LOGIC Tier Methods ==========

        registerMethod(
            name = "setEngine",
            tier = PluginTier.LOGIC,
            description = "Change active speech engine",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "name",
                    type = "String",
                    required = true,
                    description = "Engine name"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val name = args.argString(0, "name")
                delegate!!.setEngine(name)
            }
        )

        registerMethod(
            name = "wake.enable",
            tier = PluginTier.LOGIC,
            description = "Enable wake word detection",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "word",
                    type = "String",
                    required = true,
                    description = "Wake word to detect"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val word = args.argString(0, "word")
                delegate!!.enableWakeWord(word)
            }
        )

        registerMethod(
            name = "wake.disable",
            tier = PluginTier.LOGIC,
            description = "Disable wake word detection",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.disableWakeWord()
            }
        )

        registerMethod(
            name = "dictate.start",
            tier = PluginTier.LOGIC,
            description = "Start continuous dictation mode",
            returnType = "String",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.startDictation()
            }
        )

        registerMethod(
            name = "dictate.stop",
            tier = PluginTier.LOGIC,
            description = "Stop dictation and return accumulated text",
            returnType = "String",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.stopDictation()
            }
        )
    }

    /**
     * Ensure delegate is available.
     * @throws ModuleException if delegate is null (unsupported platform)
     */
    private fun requireDelegate() {
        if (delegate == null) {
            throw ModuleException(
                name,
                "",
                "Voice module not supported on this platform"
            )
        }
    }

    override suspend fun initialize() {
        // Delegate initialization happens at platform level
    }

    override suspend fun dispose() {
        // Cleanup happens at platform level
        delegate?.stop()
        delegate?.disableWakeWord()
    }
}
