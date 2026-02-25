package com.augmentalis.fileavanue.model

internal actual fun String.Companion.format(format: String, vararg args: Any?): String {
    var result = format
    var argIndex = 0
    val regex = Regex("%[\\d.]*[sdfSDF]")
    result = regex.replace(result) { match ->
        if (argIndex < args.size) {
            val arg = args[argIndex++]
            val fmt = match.value
            when {
                fmt.endsWith("f") || fmt.endsWith("F") -> {
                    val precisionMatch = Regex("\\.(\\d+)").find(fmt)
                    val precision = precisionMatch?.groupValues?.get(1)?.toIntOrNull() ?: 2
                    val num = (arg as? Number)?.toDouble() ?: 0.0
                    // Kotlin/Native has basic Double.toString() — round manually
                    val factor = Math.pow(10.0, precision.toDouble())
                    val rounded = Math.round(num * factor) / factor
                    rounded.toString()
                }
                else -> arg.toString()
            }
        } else match.value
    }
    return result
}

private object Math {
    fun pow(base: Double, exp: Double): Double {
        var result = 1.0
        repeat(exp.toInt()) { result *= base }
        return result
    }
    fun round(value: Double): Long = if (value >= 0) (value + 0.5).toLong() else (value - 0.5).toLong()
}
