package com.augmentalis.webavanue.util

/**
 * Escapes a string for safe interpolation into JavaScript string literals.
 *
 * Prevents XSS/code injection when building JavaScript code that will be
 * evaluated via WebView's evaluateJavascript(). Handles all characters that
 * could break out of a JS string context.
 */
object JsStringEscaper {

    /**
     * Escapes [input] for safe use inside a JavaScript single-quoted string literal.
     *
     * Handles: backslash, single/double quotes, backticks, newlines, carriage returns,
     * tabs, null bytes, Unicode line/paragraph separators, and `</script>` tag injection.
     */
    fun escape(input: String): String {
        val sb = StringBuilder(input.length + 16)
        for (char in input) {
            when (char) {
                '\\' -> sb.append("\\\\")
                '\'' -> sb.append("\\'")
                '"' -> sb.append("\\\"")
                '`' -> sb.append("\\`")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\u0000' -> sb.append("\\0")
                '\u2028' -> sb.append("\\u2028") // Unicode line separator
                '\u2029' -> sb.append("\\u2029") // Unicode paragraph separator
                '<' -> sb.append("\\x3C")         // Prevent </script> injection
                else -> {
                    if (char.code < 0x20) {
                        // Escape other control characters
                        sb.append("\\x${char.code.toString(16).padStart(2, '0')}")
                    } else {
                        sb.append(char)
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Escapes [input] for safe use inside a CSS selector string in JavaScript.
     * Delegates to [escape] since CSS selectors inside JS strings need the same escaping.
     */
    fun escapeSelector(input: String): String = escape(input)
}
