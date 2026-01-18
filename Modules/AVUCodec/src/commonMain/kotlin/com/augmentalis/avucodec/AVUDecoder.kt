package com.augmentalis.avucodec

/**
 * AVU Decoder - Parses Avanues Universal IPC Protocol messages.
 *
 * Companion to [AVUEncoder] for decoding incoming AVU IPC Protocol messages.
 *
 * @author Augmentalis Engineering
 * @since 1.0.0
 */
object AVUDecoder {

    private const val DELIMITER = ':'

    /**
     * Parsed IPC message result.
     */
    data class ParsedMessage(
        val code: String,
        val id: String,
        val params: List<String>
    ) {
        /**
         * Get parameter at index, or null if not present.
         */
        fun paramOrNull(index: Int): String? = params.getOrNull(index)

        /**
         * Get parameter at index, unescaped.
         */
        fun param(index: Int): String = AVUEncoder.unescape(params[index])

        /**
         * Check if this is a specific message type.
         */
        fun isCode(expected: String): Boolean = code == expected
    }

    /**
     * Parse an IPC message string.
     *
     * @param message Raw IPC message (e.g., "VCM:cmd123:SCROLL_TOP")
     * @return Parsed message or null if invalid
     */
    fun parse(message: String): ParsedMessage? {
        if (message.length < 4) return null

        val parts = message.split(DELIMITER)
        if (parts.size < 2) return null

        val code = parts[0]
        if (code.length != 3 || !code.all { it.isUpperCase() }) return null

        val id = AVUEncoder.unescape(parts[1])
        val params = if (parts.size > 2) {
            parts.subList(2, parts.size).map { AVUEncoder.unescape(it) }
        } else {
            emptyList()
        }

        return ParsedMessage(code, id, params)
    }

    /**
     * Parse voice command message.
     *
     * @return Triple of (commandId, action, params) or null if not a voice command
     */
    fun parseVoiceCommand(message: String): Triple<String, String, Map<String, String>>? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_VOICE_COMMAND)) return null

        val action = parsed.paramOrNull(0) ?: return null
        val params = mutableMapOf<String, String>()

        // Parse key=value params
        for (i in 1 until parsed.params.size) {
            val param = parsed.params[i]
            val eqIndex = param.indexOf('=')
            if (eqIndex > 0) {
                val key = param.substring(0, eqIndex)
                val value = param.substring(eqIndex + 1)
                params[key] = value
            }
        }

        return Triple(parsed.id, action, params)
    }

    /**
     * Parse error message.
     *
     * @return Triple of (requestId, errorCode, errorMessage) or null if not an error
     */
    fun parseError(message: String): Triple<String, Int, String>? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_ERROR)) return null

        val errorCode = parsed.paramOrNull(0)?.toIntOrNull() ?: 0
        val errorMessage = parsed.paramOrNull(1) ?: "Unknown error"

        return Triple(parsed.id, errorCode, errorMessage)
    }

    /**
     * Parse handshake message.
     *
     * @return Triple of (sessionId, appId, version) or null if not a handshake
     */
    fun parseHandshake(message: String): Triple<String, String, String>? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_HANDSHAKE)) return null

        val appId = parsed.paramOrNull(0) ?: return null
        val version = parsed.paramOrNull(1) ?: "1.0.0"

        return Triple(parsed.id, appId, version)
    }

    /**
     * Parse AI query message.
     *
     * @return Triple of (queryId, query, context) or null if not an AI query
     */
    fun parseAIQuery(message: String): Triple<String, String, String?>? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_AI_QUERY)) return null

        val query = parsed.paramOrNull(0) ?: return null
        val context = parsed.paramOrNull(1)

        return Triple(parsed.id, query, context)
    }

    /**
     * Parse speech-to-text message.
     *
     * @return Quadruple of (sessionId, transcript, confidence, isFinal) or null
     */
    fun parseSpeechToText(message: String): SpeechToTextResult? {
        val parsed = parse(message) ?: return null
        if (!parsed.isCode(AVUEncoder.CODE_SPEECH_TO_TEXT)) return null

        val transcript = parsed.paramOrNull(0) ?: return null
        val confidence = parsed.paramOrNull(1)?.toFloatOrNull() ?: 0f
        val isFinal = parsed.paramOrNull(2)?.toBooleanStrictOrNull() ?: true

        return SpeechToTextResult(parsed.id, transcript, confidence, isFinal)
    }

    /**
     * Speech-to-text parse result.
     */
    data class SpeechToTextResult(
        val sessionId: String,
        val transcript: String,
        val confidence: Float,
        val isFinal: Boolean
    )
}
