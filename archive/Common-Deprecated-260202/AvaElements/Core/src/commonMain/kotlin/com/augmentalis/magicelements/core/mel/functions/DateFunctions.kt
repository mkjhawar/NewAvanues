package com.augmentalis.magicelements.core.mel.functions

import kotlinx.datetime.*

/**
 * Date/Time functions for MEL (Tier 1 - Apple-safe).
 *
 * Uses kotlinx-datetime for KMP compatibility.
 * All dates are represented as ISO-8601 strings or Instant objects.
 */
object DateFunctions {
    fun register() {
        val tier = PluginTier.DATA

        // Current date/time
        FunctionRegistry.register("date", "now", tier) { args ->
            requireArgs("date.now", args, 0)
            Clock.System.now()
        }

        FunctionRegistry.register("date", "nowMillis", tier) { args ->
            requireArgs("date.nowMillis", args, 0)
            Clock.System.now().toEpochMilliseconds()
        }

        FunctionRegistry.register("date", "today", tier) { args ->
            requireArgs("date.today", args, 0)
            Clock.System.todayIn(TimeZone.currentSystemDefault())
        }

        // Parsing
        FunctionRegistry.register("date", "parse", tier) { args ->
            requireArgs("date.parse", args, 1)
            val str = TypeCoercion.toString(args[0])
            try {
                Instant.parse(str)
            } catch (e: Exception) {
                throw MELFunctionException("Invalid date format: $str. Expected ISO-8601 format.", e)
            }
        }

        FunctionRegistry.register("date", "fromMillis", tier) { args ->
            requireArgs("date.fromMillis", args, 1)
            val millis = TypeCoercion.toNumber(args[0]).toLong()
            Instant.fromEpochMilliseconds(millis)
        }

        // Formatting
        FunctionRegistry.register("date", "format", tier) { args ->
            when (args.size) {
                1 -> {
                    // format(instant) - ISO-8601 format
                    val instant = toInstant(args[0])
                    instant.toString()
                }
                2 -> {
                    // format(instant, pattern) - custom format
                    val instant = toInstant(args[0])
                    val pattern = TypeCoercion.toString(args[1])
                    formatInstant(instant, pattern)
                }
                else -> throw MELArgumentCountException("date.format", 1, args.size)
            }
        }

        FunctionRegistry.register("date", "toMillis", tier) { args ->
            requireArgs("date.toMillis", args, 1)
            val instant = toInstant(args[0])
            instant.toEpochMilliseconds()
        }

        // Arithmetic
        FunctionRegistry.register("date", "addDays", tier) { args ->
            requireArgs("date.addDays", args, 2)
            val instant = toInstant(args[0])
            val days = TypeCoercion.toNumber(args[1]).toInt()
            instant.plus(days, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        }

        FunctionRegistry.register("date", "addHours", tier) { args ->
            requireArgs("date.addHours", args, 2)
            val instant = toInstant(args[0])
            val hours = TypeCoercion.toNumber(args[1]).toInt()
            instant.plus(hours, DateTimeUnit.HOUR, TimeZone.currentSystemDefault())
        }

        FunctionRegistry.register("date", "addMinutes", tier) { args ->
            requireArgs("date.addMinutes", args, 2)
            val instant = toInstant(args[0])
            val minutes = TypeCoercion.toNumber(args[1]).toInt()
            instant.plus(minutes, DateTimeUnit.MINUTE, TimeZone.currentSystemDefault())
        }

        FunctionRegistry.register("date", "addSeconds", tier) { args ->
            requireArgs("date.addSeconds", args, 2)
            val instant = toInstant(args[0])
            val seconds = TypeCoercion.toNumber(args[1]).toInt()
            instant.plus(seconds, DateTimeUnit.SECOND, TimeZone.currentSystemDefault())
        }

        FunctionRegistry.register("date", "addMonths", tier) { args ->
            requireArgs("date.addMonths", args, 2)
            val instant = toInstant(args[0])
            val months = TypeCoercion.toNumber(args[1]).toInt()
            instant.plus(months, DateTimeUnit.MONTH, TimeZone.currentSystemDefault())
        }

        FunctionRegistry.register("date", "addYears", tier) { args ->
            requireArgs("date.addYears", args, 2)
            val instant = toInstant(args[0])
            val years = TypeCoercion.toNumber(args[1]).toInt()
            instant.plus(years, DateTimeUnit.YEAR, TimeZone.currentSystemDefault())
        }

        // Difference
        FunctionRegistry.register("date", "diff", tier) { args ->
            when (args.size) {
                2 -> {
                    // diff(date1, date2) - returns difference in milliseconds
                    val instant1 = toInstant(args[0])
                    val instant2 = toInstant(args[1])
                    instant1.toEpochMilliseconds() - instant2.toEpochMilliseconds()
                }
                3 -> {
                    // diff(date1, date2, unit) - returns difference in specified unit
                    val instant1 = toInstant(args[0])
                    val instant2 = toInstant(args[1])
                    val unit = TypeCoercion.toString(args[2]).lowercase()
                    val diffMillis = instant1.toEpochMilliseconds() - instant2.toEpochMilliseconds()

                    when (unit) {
                        "milliseconds", "ms" -> diffMillis
                        "seconds", "s" -> diffMillis / 1000
                        "minutes", "m" -> diffMillis / (1000 * 60)
                        "hours", "h" -> diffMillis / (1000 * 60 * 60)
                        "days", "d" -> diffMillis / (1000 * 60 * 60 * 24)
                        else -> throw IllegalArgumentException("Unknown time unit: $unit")
                    }
                }
                else -> throw MELArgumentCountException("date.diff", 2, args.size)
            }
        }

        // Components
        FunctionRegistry.register("date", "year", tier) { args ->
            requireArgs("date.year", args, 1)
            val instant = toInstant(args[0])
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.year
        }

        FunctionRegistry.register("date", "month", tier) { args ->
            requireArgs("date.month", args, 1)
            val instant = toInstant(args[0])
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.monthNumber
        }

        FunctionRegistry.register("date", "day", tier) { args ->
            requireArgs("date.day", args, 1)
            val instant = toInstant(args[0])
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.dayOfMonth
        }

        FunctionRegistry.register("date", "hour", tier) { args ->
            requireArgs("date.hour", args, 1)
            val instant = toInstant(args[0])
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.hour
        }

        FunctionRegistry.register("date", "minute", tier) { args ->
            requireArgs("date.minute", args, 1)
            val instant = toInstant(args[0])
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.minute
        }

        FunctionRegistry.register("date", "second", tier) { args ->
            requireArgs("date.second", args, 1)
            val instant = toInstant(args[0])
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.second
        }

        FunctionRegistry.register("date", "dayOfWeek", tier) { args ->
            requireArgs("date.dayOfWeek", args, 1)
            val instant = toInstant(args[0])
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.dayOfWeek.value  // 1-7 (Monday-Sunday)
        }

        FunctionRegistry.register("date", "dayOfYear", tier) { args ->
            requireArgs("date.dayOfYear", args, 1)
            val instant = toInstant(args[0])
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.dayOfYear
        }

        // Comparison
        FunctionRegistry.register("date", "isBefore", tier) { args ->
            requireArgs("date.isBefore", args, 2)
            val instant1 = toInstant(args[0])
            val instant2 = toInstant(args[1])
            instant1 < instant2
        }

        FunctionRegistry.register("date", "isAfter", tier) { args ->
            requireArgs("date.isAfter", args, 2)
            val instant1 = toInstant(args[0])
            val instant2 = toInstant(args[1])
            instant1 > instant2
        }

        FunctionRegistry.register("date", "isSame", tier) { args ->
            requireArgs("date.isSame", args, 2)
            val instant1 = toInstant(args[0])
            val instant2 = toInstant(args[1])
            instant1 == instant2
        }

        // Utilities
        FunctionRegistry.register("date", "startOfDay", tier) { args ->
            requireArgs("date.startOfDay", args, 1)
            val instant = toInstant(args[0])
            val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            LocalDateTime(date, LocalTime(0, 0, 0))
                .toInstant(TimeZone.currentSystemDefault())
        }

        FunctionRegistry.register("date", "endOfDay", tier) { args ->
            requireArgs("date.endOfDay", args, 1)
            val instant = toInstant(args[0])
            val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            LocalDateTime(date, LocalTime(23, 59, 59, 999_999_999))
                .toInstant(TimeZone.currentSystemDefault())
        }
    }

    /**
     * Convert argument to Instant.
     * Supports: Instant, Long (millis), String (ISO-8601)
     */
    private fun toInstant(value: Any): Instant {
        return when (value) {
            is Instant -> value
            is Number -> Instant.fromEpochMilliseconds(value.toLong())
            is String -> Instant.parse(value)
            else -> throw MELArgumentTypeException("date function", 0, "Instant/Long/String", value)
        }
    }

    /**
     * Format an Instant using a simple pattern.
     * Supported patterns:
     * - "iso" or "ISO-8601": Full ISO-8601 format
     * - "date": YYYY-MM-DD
     * - "time": HH:MM:SS
     * - "datetime": YYYY-MM-DD HH:MM:SS
     * - Custom patterns with placeholders: YYYY, MM, DD, HH, mm, ss
     */
    private fun formatInstant(instant: Instant, pattern: String): String {
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        return when (pattern.lowercase()) {
            "iso", "iso-8601" -> instant.toString()
            "date" -> {
                val year = localDateTime.year.toString().padStart(4, '0')
                val month = localDateTime.monthNumber.toString().padStart(2, '0')
                val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
                "$year-$month-$day"
            }
            "time" -> {
                val hour = localDateTime.hour.toString().padStart(2, '0')
                val minute = localDateTime.minute.toString().padStart(2, '0')
                val second = localDateTime.second.toString().padStart(2, '0')
                "$hour:$minute:$second"
            }
            "datetime" -> {
                val year = localDateTime.year.toString().padStart(4, '0')
                val month = localDateTime.monthNumber.toString().padStart(2, '0')
                val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
                val hour = localDateTime.hour.toString().padStart(2, '0')
                val minute = localDateTime.minute.toString().padStart(2, '0')
                val second = localDateTime.second.toString().padStart(2, '0')
                "$year-$month-$day $hour:$minute:$second"
            }
            else -> {
                // Custom pattern with placeholders
                pattern
                    .replace("YYYY", localDateTime.year.toString().padStart(4, '0'))
                    .replace("MM", localDateTime.monthNumber.toString().padStart(2, '0'))
                    .replace("DD", localDateTime.dayOfMonth.toString().padStart(2, '0'))
                    .replace("HH", localDateTime.hour.toString().padStart(2, '0'))
                    .replace("mm", localDateTime.minute.toString().padStart(2, '0'))
                    .replace("ss", localDateTime.second.toString().padStart(2, '0'))
            }
        }
    }

    private fun requireArgs(name: String, args: List<Any>, expected: Int) {
        if (args.size != expected) {
            throw MELArgumentCountException(name, expected, args.size)
        }
    }
}
