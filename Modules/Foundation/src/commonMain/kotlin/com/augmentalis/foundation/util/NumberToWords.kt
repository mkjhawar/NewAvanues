/**
 * NumberToWords.kt - Converts numbers to spoken word equivalents
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-29
 *
 * Supports:
 * - Western numbering: thousand, million, billion, trillion
 * - Indian numbering: lakh (1,00,000), crore (1,00,00,000)
 * - Comprehensive currency support (50+ currencies)
 * - Unit suffixes (percent, degrees, bytes, etc.)
 *
 * Examples:
 *   1 -> "one"
 *   21 -> "twenty one"
 *   100 -> "one hundred"
 *   100000 -> "one lakh" (Indian) or "one hundred thousand" (Western)
 *   10000000 -> "one crore" (Indian) or "ten million" (Western)
 */
package com.augmentalis.foundation.util

/**
 * Number system convention for regional formatting
 */
enum class NumberSystem {
    /** Western: thousand, million, billion, trillion */
    WESTERN,
    /** Indian: thousand, lakh, crore */
    INDIAN,
    /** East Asian: wan (10000), yi (100000000) - uses English words */
    EAST_ASIAN
}

object NumberToWords {

    private val ones = arrayOf(
        "", "one", "two", "three", "four", "five",
        "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen",
        "sixteen", "seventeen", "eighteen", "nineteen"
    )

    private val tens = arrayOf(
        "", "", "twenty", "thirty", "forty", "fifty",
        "sixty", "seventy", "eighty", "ninety"
    )

    // Reverse mapping for parsing words back to numbers (includes all systems)
    private val wordToNumber: Map<String, Long> by lazy {
        buildMap {
            ones.forEachIndexed { index, word -> if (word.isNotEmpty()) put(word, index.toLong()) }
            tens.forEachIndexed { index, word -> if (word.isNotEmpty()) put(word, (index * 10).toLong()) }
            put("zero", 0L)
            put("hundred", 100L)
            put("thousand", 1_000L)
            // Western scale
            put("million", 1_000_000L)
            put("billion", 1_000_000_000L)
            put("trillion", 1_000_000_000_000L)
            // Indian scale
            put("lakh", 100_000L)
            put("lac", 100_000L)  // Alternative spelling
            put("lakhs", 100_000L)
            put("lacs", 100_000L)
            put("crore", 10_000_000L)
            put("crores", 10_000_000L)
            put("arab", 1_000_000_000L)  // 100 crore
            put("kharab", 100_000_000_000L)  // 100 arab
            // East Asian (English words for the concepts)
            put("wan", 10_000L)  // 万
            put("yi", 100_000_000L)  // 亿
        }
    }

    // Currency Definitions
    private val currencyMap = mapOf(
        // Americas
        "$" to "dollars",
        "US$" to "us dollars",
        "USD" to "us dollars",
        "C$" to "canadian dollars",
        "CAD" to "canadian dollars",
        "MX$" to "mexican pesos",
        "MXN" to "mexican pesos",
        "R$" to "brazilian reais",
        "BRL" to "brazilian reais",
        "ARS" to "argentine pesos",

        // Europe
        "€" to "euros",
        "EUR" to "euros",
        "£" to "pounds",
        "GBP" to "british pounds",
        "CHF" to "swiss francs",
        "SEK" to "swedish kronor",
        "NOK" to "norwegian kroner",
        "DKK" to "danish kroner",
        "PLN" to "polish zloty",
        "CZK" to "czech koruna",
        "HUF" to "hungarian forints",
        "RON" to "romanian lei",
        "RUB" to "russian rubles",
        "₽" to "russian rubles",
        "UAH" to "ukrainian hryvnia",
        "₴" to "ukrainian hryvnia",
        "TRY" to "turkish lira",
        "₺" to "turkish lira",

        // Asia - East
        "¥" to "yen",
        "JPY" to "japanese yen",
        "CN¥" to "chinese yuan",
        "CNY" to "chinese yuan",
        "元" to "yuan",
        "RMB" to "renminbi",
        "₩" to "korean won",
        "KRW" to "korean won",
        "NT$" to "taiwan dollars",
        "TWD" to "taiwan dollars",
        "HK$" to "hong kong dollars",
        "HKD" to "hong kong dollars",

        // Asia - South
        "₹" to "rupees",
        "INR" to "indian rupees",
        "Rs" to "rupees",
        "Rs." to "rupees",
        "PKR" to "pakistani rupees",
        "LKR" to "sri lankan rupees",
        "BDT" to "bangladeshi taka",
        "৳" to "taka",
        "NPR" to "nepalese rupees",

        // Asia - Southeast
        "฿" to "thai baht",
        "THB" to "thai baht",
        "SGD" to "singapore dollars",
        "S$" to "singapore dollars",
        "MYR" to "malaysian ringgit",
        "RM" to "ringgit",
        "IDR" to "indonesian rupiah",
        "Rp" to "rupiah",
        "PHP" to "philippine pesos",
        "₱" to "philippine pesos",
        "VND" to "vietnamese dong",
        "₫" to "dong",

        // Middle East
        "SAR" to "saudi riyals",
        "AED" to "uae dirhams",
        "QAR" to "qatari riyals",
        "KWD" to "kuwaiti dinars",
        "BHD" to "bahraini dinars",
        "OMR" to "omani rials",
        "ILS" to "israeli shekels",
        "₪" to "shekels",
        "EGP" to "egyptian pounds",
        "E£" to "egyptian pounds",

        // Africa
        "ZAR" to "south african rand",
        "R" to "rand",
        "NGN" to "nigerian naira",
        "₦" to "naira",
        "KES" to "kenyan shillings",
        "GHS" to "ghanaian cedis",
        "₵" to "cedis",
        "MAD" to "moroccan dirhams",

        // Oceania
        "A$" to "australian dollars",
        "AUD" to "australian dollars",
        "NZ$" to "new zealand dollars",
        "NZD" to "new zealand dollars",

        // Crypto (common ones)
        "BTC" to "bitcoin",
        "₿" to "bitcoin",
        "ETH" to "ethereum",
        "Ξ" to "ethereum",
        "USDT" to "tether",
        "USDC" to "usd coin"
    )

    /**
     * Default number system (can be changed globally)
     */
    var defaultSystem: NumberSystem = NumberSystem.WESTERN

    /**
     * Convert a number to its word representation using the default system
     * @param number The number to convert (0 to Int.MAX_VALUE)
     * @return Word representation (e.g., 101 -> "one hundred one")
     */
    fun convert(number: Int): String = convert(number.toLong(), defaultSystem)

    /**
     * Convert a number to its word representation with specified number system
     * @param number The number to convert
     * @param system The number system to use (WESTERN, INDIAN, EAST_ASIAN)
     * @return Word representation
     */
    fun convert(number: Long, system: NumberSystem = defaultSystem): String {
        if (number == 0L) return "zero"
        if (number < 0L) return "negative ${convert(-number, system)}"

        return when (system) {
            NumberSystem.WESTERN -> convertWestern(number).trim()
            NumberSystem.INDIAN -> convertIndian(number).trim()
            NumberSystem.EAST_ASIAN -> convertEastAsian(number).trim()
        }
    }

    /**
     * Convert using Western system (thousand, million, billion, trillion)
     */
    private fun convertWestern(number: Long): String {
        return when {
            number < 20 -> ones[number.toInt()]
            number < 100 -> {
                val ten = tens[(number / 10).toInt()]
                val one = ones[(number % 10).toInt()]
                if (one.isEmpty()) ten else "$ten $one"
            }
            number < 1000 -> {
                val hundreds = "${ones[(number / 100).toInt()]} hundred"
                val remainder = number % 100
                if (remainder == 0L) hundreds else "$hundreds ${convertWestern(remainder)}"
            }
            number < 1_000_000 -> {
                val thousands = "${convertWestern(number / 1000)} thousand"
                val remainder = number % 1000
                if (remainder == 0L) thousands else "$thousands ${convertWestern(remainder)}"
            }
            number < 1_000_000_000 -> {
                val millions = "${convertWestern(number / 1_000_000)} million"
                val remainder = number % 1_000_000
                if (remainder == 0L) millions else "$millions ${convertWestern(remainder)}"
            }
            number < 1_000_000_000_000 -> {
                val billions = "${convertWestern(number / 1_000_000_000)} billion"
                val remainder = number % 1_000_000_000
                if (remainder == 0L) billions else "$billions ${convertWestern(remainder)}"
            }
            else -> {
                val trillions = "${convertWestern(number / 1_000_000_000_000)} trillion"
                val remainder = number % 1_000_000_000_000
                if (remainder == 0L) trillions else "$trillions ${convertWestern(remainder)}"
            }
        }
    }

    /**
     * Convert using Indian system (thousand, lakh, crore, arab, kharab)
     */
    private fun convertIndian(number: Long): String {
        return when {
            number < 20 -> ones[number.toInt()]
            number < 100 -> {
                val ten = tens[(number / 10).toInt()]
                val one = ones[(number % 10).toInt()]
                if (one.isEmpty()) ten else "$ten $one"
            }
            number < 1000 -> {
                val hundreds = "${ones[(number / 100).toInt()]} hundred"
                val remainder = number % 100
                if (remainder == 0L) hundreds else "$hundreds ${convertIndian(remainder)}"
            }
            number < 100_000 -> {
                val thousands = "${convertIndian(number / 1000)} thousand"
                val remainder = number % 1000
                if (remainder == 0L) thousands else "$thousands ${convertIndian(remainder)}"
            }
            number < 10_000_000 -> {
                val lakhs = "${convertIndian(number / 100_000)} lakh"
                val remainder = number % 100_000
                if (remainder == 0L) lakhs else "$lakhs ${convertIndian(remainder)}"
            }
            number < 1_000_000_000 -> {
                val crores = "${convertIndian(number / 10_000_000)} crore"
                val remainder = number % 10_000_000
                if (remainder == 0L) crores else "$crores ${convertIndian(remainder)}"
            }
            number < 100_000_000_000 -> {
                val arabs = "${convertIndian(number / 1_000_000_000)} arab"
                val remainder = number % 1_000_000_000
                if (remainder == 0L) arabs else "$arabs ${convertIndian(remainder)}"
            }
            else -> {
                val kharabs = "${convertIndian(number / 100_000_000_000)} kharab"
                val remainder = number % 100_000_000_000
                if (remainder == 0L) kharabs else "$kharabs ${convertIndian(remainder)}"
            }
        }
    }

    /**
     * Convert using East Asian system (wan = 10000, yi = 100000000)
     */
    private fun convertEastAsian(number: Long): String {
        return when {
            number < 20 -> ones[number.toInt()]
            number < 100 -> {
                val ten = tens[(number / 10).toInt()]
                val one = ones[(number % 10).toInt()]
                if (one.isEmpty()) ten else "$ten $one"
            }
            number < 1000 -> {
                val hundreds = "${ones[(number / 100).toInt()]} hundred"
                val remainder = number % 100
                if (remainder == 0L) hundreds else "$hundreds ${convertEastAsian(remainder)}"
            }
            number < 10_000 -> {
                val thousands = "${convertEastAsian(number / 1000)} thousand"
                val remainder = number % 1000
                if (remainder == 0L) thousands else "$thousands ${convertEastAsian(remainder)}"
            }
            number < 100_000_000 -> {
                val wan = "${convertEastAsian(number / 10_000)} wan"
                val remainder = number % 10_000
                if (remainder == 0L) wan else "$wan ${convertEastAsian(remainder)}"
            }
            else -> {
                val yi = "${convertEastAsian(number / 100_000_000)} yi"
                val remainder = number % 100_000_000
                if (remainder == 0L) yi else "$yi ${convertEastAsian(remainder)}"
            }
        }
    }

    // Convenience methods for specific systems

    /**
     * Convert using Western system explicitly
     */
    fun convertWestern(number: Int): String = convert(number.toLong(), NumberSystem.WESTERN)

    /**
     * Convert using Indian system explicitly
     */
    fun convertIndian(number: Int): String = convert(number.toLong(), NumberSystem.INDIAN)

    /**
     * Convert using East Asian system explicitly
     */
    fun convertEastAsian(number: Int): String = convert(number.toLong(), NumberSystem.EAST_ASIAN)

    /**
     * Parse word number back to Long
     */
    fun parse(words: String): Long? {
        val normalized = words.lowercase().trim()
        if (normalized.isEmpty()) return null

        wordToNumber[normalized]?.let { return it }
        normalized.toLongOrNull()?.let { return it }

        return parseCompound(normalized)
    }

    /**
     * Parse to Int (for backward compatibility)
     */
    fun parseToInt(words: String): Int? = parse(words)?.toInt()

    private fun parseCompound(words: String): Long? {
        val tokens = words.split(" ")
            .filter { it.isNotBlank() && it != "and" }
        if (tokens.isEmpty()) return null

        var result = 0L
        var current = 0L

        for (token in tokens) {
            val value = wordToNumber[token] ?: return null

            when {
                value == 100L -> current *= 100L
                value >= 1000L -> {
                    if (current == 0L) current = 1L
                    current *= value
                    result += current
                    current = 0L
                }
                else -> current += value
            }
        }

        return result + current
    }

    /**
     * Check if a string is a valid number word
     */
    fun isNumberWord(word: String): Boolean {
        return parse(word) != null
    }

    /**
     * Format a number with the appropriate grouping for display
     */
    fun formatWithGrouping(number: Long, system: NumberSystem = defaultSystem): String {
        return when (system) {
            NumberSystem.INDIAN -> formatIndianGrouping(number)
            else -> formatWesternGrouping(number)
        }
    }

    private fun formatWesternGrouping(number: Long): String {
        return number.toString().reversed().chunked(3).joinToString(",").reversed()
    }

    private fun formatIndianGrouping(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str

        val lastThree = str.takeLast(3)
        val remaining = str.dropLast(3)
        val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
        return "$grouped,$lastThree"
    }

    // Unit suffix map
    private val unitSuffixMap = mapOf(
        "%" to "percent",
        "°" to "degrees",
        "°C" to "degrees celsius",
        "°F" to "degrees fahrenheit",
        "K" to "kelvin",
        "km" to "kilometers",
        "m" to "meters",
        "cm" to "centimeters",
        "mm" to "millimeters",
        "mi" to "miles",
        "ft" to "feet",
        "in" to "inches",
        "kg" to "kilograms",
        "g" to "grams",
        "mg" to "milligrams",
        "lb" to "pounds",
        "lbs" to "pounds",
        "oz" to "ounces",
        "L" to "liters",
        "ml" to "milliliters",
        "gal" to "gallons",
        "B" to "bytes",
        "KB" to "kilobytes",
        "MB" to "megabytes",
        "GB" to "gigabytes",
        "TB" to "terabytes",
        "Hz" to "hertz",
        "kHz" to "kilohertz",
        "MHz" to "megahertz",
        "GHz" to "gigahertz",
        "ms" to "milliseconds",
        "s" to "seconds",
        "min" to "minutes",
        "hr" to "hours",
        "mph" to "miles per hour",
        "km/h" to "kilometers per hour",
        "W" to "watts",
        "kW" to "kilowatts",
        "V" to "volts",
        "A" to "amps",
        "mAh" to "milliamp hours",
        "px" to "pixels",
        "dB" to "decibels"
    )

    private val suffixMap: Map<String, String> by lazy {
        unitSuffixMap + currencyMap
    }

    private val prefixCurrencies = setOf(
        "$", "US$", "C$", "A$", "NZ$", "HK$", "S$", "NT$", "MX$",
        "£", "€", "¥", "CN¥", "₹", "₽", "₩", "₱", "₺", "₴", "₦", "₵", "₿", "Ξ",
        "৳", "₫", "₪", "R$", "E£"
    )

    /**
     * Convert a number with suffix to its word representation
     */
    fun convertWithSuffix(
        numberWithSuffix: String,
        system: NumberSystem = defaultSystem
    ): String? {
        val trimmed = numberWithSuffix.trim().replace(",", "")
        if (trimmed.isEmpty()) return null

        for ((symbol, word) in currencyMap) {
            if (symbol in prefixCurrencies && trimmed.startsWith(symbol)) {
                val numberPart = trimmed.removePrefix(symbol).trim()
                val number = numberPart.toLongOrNull() ?: continue
                return "${convert(number, system)} $word"
            }
        }

        for ((symbol, word) in currencyMap) {
            if (trimmed.endsWith(symbol) || trimmed.endsWith(" $symbol")) {
                val numberPart = trimmed.removeSuffix(symbol).trim()
                val number = numberPart.toLongOrNull() ?: continue
                return "${convert(number, system)} $word"
            }
        }

        for ((symbol, word) in unitSuffixMap) {
            if (trimmed.endsWith(symbol)) {
                val numberPart = trimmed.removeSuffix(symbol).trim()
                val number = numberPart.toLongOrNull() ?: continue
                return "${convert(number, system)} $word"
            }
        }

        val number = trimmed.toLongOrNull() ?: return null
        return convert(number, system)
    }

    /**
     * Convert currency amount with appropriate number system
     */
    fun convertCurrency(amount: Long, currencyCode: String): String? {
        val code = currencyCode.uppercase()
        val currencyWord = currencyMap[code] ?: return null

        val system = when (code) {
            "INR", "PKR", "LKR", "BDT", "NPR" -> NumberSystem.INDIAN
            "CNY", "JPY", "KRW", "TWD", "HKD" -> NumberSystem.EAST_ASIAN
            else -> NumberSystem.WESTERN
        }

        return "${convert(amount, system)} $currencyWord"
    }

    /**
     * Get all supported suffix symbols
     */
    fun getSupportedSuffixes(): Set<String> = suffixMap.keys

    /**
     * Get all supported currency codes
     */
    fun getSupportedCurrencyCodes(): Set<String> = currencyMap.keys
}
