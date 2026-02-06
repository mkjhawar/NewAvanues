package com.avanues.avu.dsl.migration

/**
 * Backward compatibility for AVU v1 wire protocol messages.
 *
 * Parses raw `CODE:field1:field2` format into structured [V1Message] objects
 * that can be dispatched through the interpreter pipeline or converted to
 * AVU DSL text for migration preview.
 *
 * ## V1 Format
 * ```
 * VCM:cmd1:SCROLL_TOP:speed=fast
 * AAC:act1:CLICK:btn_submit
 * CHT:msg1:Hello World
 * ```
 *
 * ## Known Field Mappings
 * Common codes have predefined field name mappings (VCM, AAC, SCR, CHT, TTS).
 * Unknown codes use positional field names (`field_0`, `field_1`, ...).
 */
object AvuV1Compat {

    /**
     * Parse a v1 wire protocol message.
     */
    fun parseV1Message(message: String): V1ParseResult {
        if (message.isBlank()) {
            return V1ParseResult.Error("Empty message")
        }

        val parts = message.split(":")
        if (parts.isEmpty() || parts[0].length != 3) {
            return V1ParseResult.Error("Invalid v1 format: missing 3-letter code")
        }

        val code = parts[0]
        if (!code.all { it.isUpperCase() }) {
            return V1ParseResult.Error("Invalid code: must be uppercase: $code")
        }

        val fields = if (parts.size > 1) parts.subList(1, parts.size) else emptyList()

        return V1ParseResult.Success(V1Message(code = code, fields = fields))
    }

    /**
     * Convert a v1 message to dispatch arguments.
     * Maps positional fields to named arguments based on code conventions.
     */
    fun toDispatchArguments(message: V1Message): Map<String, Any?> {
        val args = mutableMapOf<String, Any?>()
        args["_code"] = message.code

        // Always include positional field names as fallback
        message.fields.forEachIndexed { index, field ->
            args["field_$index"] = field
        }

        // Apply known field name mappings for common codes
        when (message.code) {
            "VCM" -> mapVcmFields(message.fields, args)
            "AAC" -> mapAacFields(message.fields, args)
            "SCR" -> mapScrFields(message.fields, args)
            "CHT" -> mapChtFields(message.fields, args)
            "TTS" -> mapTtsFields(message.fields, args)
        }

        return args
    }

    /**
     * Generate AVU DSL text from a v1 message (for migration preview).
     */
    fun toAvuDslText(message: V1Message): String {
        val args = toDispatchArguments(message)
        val namedArgs = args.entries
            .filter { !it.key.startsWith("_") && !it.key.startsWith("field_") }
            .joinToString(", ") { (k, v) -> "$k: \"$v\"" }

        return if (namedArgs.isNotEmpty()) {
            "${message.code}($namedArgs)"
        } else {
            val positionalArgs = message.fields.joinToString(", ") { "\"$it\"" }
            "${message.code}($positionalArgs)"
        }
    }

    /**
     * Check if a string looks like a v1 wire protocol message.
     */
    fun isV1Format(text: String): Boolean {
        if (text.length < 3) return false
        val code = text.take(3)
        return code.all { it.isUpperCase() } && (text.length == 3 || text[3] == ':')
    }

    // =========================================================================
    // FIELD NAME MAPPINGS FOR COMMON CODES
    // =========================================================================

    private fun mapVcmFields(fields: List<String>, args: MutableMap<String, Any?>) {
        if (fields.size >= 1) args["id"] = fields[0]
        if (fields.size >= 2) args["action"] = fields[1]
        if (fields.size >= 3) args["params"] = fields[2]
    }

    private fun mapAacFields(fields: List<String>, args: MutableMap<String, Any?>) {
        if (fields.size >= 1) args["id"] = fields[0]
        if (fields.size >= 2) args["actionType"] = fields[1]
        if (fields.size >= 3) args["target"] = fields[2]
        if (fields.size >= 4) args["params"] = fields[3]
    }

    private fun mapScrFields(fields: List<String>, args: MutableMap<String, Any?>) {
        if (fields.size >= 1) args["msgId"] = fields[0]
        if (fields.size >= 2) args["entityType"] = fields[1]
        if (fields.size >= 3) args["entityId"] = fields[2]
        if (fields.size >= 4) args["version"] = fields[3]
        if (fields.size >= 5) args["data"] = fields[4]
    }

    private fun mapChtFields(fields: List<String>, args: MutableMap<String, Any?>) {
        if (fields.size >= 1) args["messageId"] = fields[0]
        if (fields.size >= 2) args["text"] = fields[1]
    }

    private fun mapTtsFields(fields: List<String>, args: MutableMap<String, Any?>) {
        if (fields.size >= 1) args["text"] = fields[0]
        if (fields.size >= 2) args["locale"] = fields[1]
    }
}

/**
 * Parsed v1 wire protocol message.
 */
data class V1Message(
    val code: String,
    val fields: List<String>
)

/**
 * Result of parsing a v1 message.
 */
sealed class V1ParseResult {
    data class Success(val message: V1Message) : V1ParseResult()
    data class Error(val reason: String) : V1ParseResult()

    val isSuccess: Boolean get() = this is Success
    fun messageOrNull(): V1Message? = (this as? Success)?.message
}
