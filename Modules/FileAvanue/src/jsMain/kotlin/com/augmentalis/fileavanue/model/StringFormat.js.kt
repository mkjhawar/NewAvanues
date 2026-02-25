package com.augmentalis.fileavanue.model

internal actual fun String.Companion.format(format: String, vararg args: Any?): String {
    var result = format
    var argIndex = 0
    // Simple %s, %d, %.Nf replacement for JS (no java.lang.String.format)
    val regex = Regex("%[\\d.]*[sdfSDF]")
    result = regex.replace(result) { match ->
        if (argIndex < args.size) {
            val arg = args[argIndex++]
            val fmt = match.value
            when {
                fmt.endsWith("f") || fmt.endsWith("F") -> {
                    // Extract precision from format like "%.1f" or "%.2f"
                    val precisionMatch = Regex("\\.(\\d+)").find(fmt)
                    val precision = precisionMatch?.groupValues?.get(1)?.toIntOrNull() ?: 2
                    val num = (arg as? Number)?.toDouble() ?: 0.0
                    num.asDynamic().toFixed(precision) as String
                }
                else -> arg.toString()
            }
        } else match.value
    }
    return result
}
