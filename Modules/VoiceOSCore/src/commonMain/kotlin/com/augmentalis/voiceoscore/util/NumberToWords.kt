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
package com.augmentalis.voiceoscore.util

/**
 * Number system convention for regional formatting
 */
enum class NumberSystem {
    /** Western: thousand, million, billion, trillion */
    WESTERN,
    /** Indian: thousand, lakh, crore */
    INDIAN,
    /** East Asian: wan (万, 10000), yi (亿, 100000000) - uses English words */
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

    // ═══════════════════════════════════════════════════════════════════════
    // Currency Definitions
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Comprehensive currency map with symbols and spoken names
     * Includes major world currencies organized by region
     */
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
     *
     * Indian numbering:
     * - 1,000 = one thousand
     * - 1,00,000 = one lakh (100 thousand)
     * - 1,00,00,000 = one crore (10 million)
     * - 1,00,00,00,000 = one arab (1 billion)
     * - 1,00,00,00,00,000 = one kharab (100 billion)
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
                // Thousands: 1,000 to 99,999
                val thousands = "${convertIndian(number / 1000)} thousand"
                val remainder = number % 1000
                if (remainder == 0L) thousands else "$thousands ${convertIndian(remainder)}"
            }
            number < 10_000_000 -> {
                // Lakhs: 1,00,000 to 99,99,999
                val lakhs = "${convertIndian(number / 100_000)} lakh"
                val remainder = number % 100_000
                if (remainder == 0L) lakhs else "$lakhs ${convertIndian(remainder)}"
            }
            number < 1_000_000_000 -> {
                // Crores: 1,00,00,000 to 99,99,99,999
                val crores = "${convertIndian(number / 10_000_000)} crore"
                val remainder = number % 10_000_000
                if (remainder == 0L) crores else "$crores ${convertIndian(remainder)}"
            }
            number < 100_000_000_000 -> {
                // Arabs: 1,00,00,00,000 to 99,99,99,99,999
                val arabs = "${convertIndian(number / 1_000_000_000)} arab"
                val remainder = number % 1_000_000_000
                if (remainder == 0L) arabs else "$arabs ${convertIndian(remainder)}"
            }
            else -> {
                // Kharabs: 1,00,00,00,00,000 and above
                val kharabs = "${convertIndian(number / 100_000_000_000)} kharab"
                val remainder = number % 100_000_000_000
                if (remainder == 0L) kharabs else "$kharabs ${convertIndian(remainder)}"
            }
        }
    }

    /**
     * Convert using East Asian system (wan = 10000, yi = 100000000)
     * Uses English words for the concepts
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
                // Wan (万): 10,000 to 99,999,999
                val wan = "${convertEastAsian(number / 10_000)} wan"
                val remainder = number % 10_000
                if (remainder == 0L) wan else "$wan ${convertEastAsian(remainder)}"
            }
            else -> {
                // Yi (亿): 100,000,000 and above
                val yi = "${convertEastAsian(number / 100_000_000)} yi"
                val remainder = number % 100_000_000
                if (remainder == 0L) yi else "$yi ${convertEastAsian(remainder)}"
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Convenience methods for specific systems
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Convert using Western system explicitly
     */
    fun convertWestern(number: Int): String = convert(number.toLong(), NumberSystem.WESTERN)

    /**
     * Convert using Indian system explicitly
     *
     * Examples:
     *   100000 -> "one lakh"
     *   1500000 -> "fifteen lakh"
     *   10000000 -> "one crore"
     *   25000000 -> "two crore fifty lakh"
     */
    fun convertIndian(number: Int): String = convert(number.toLong(), NumberSystem.INDIAN)

    /**
     * Convert using East Asian system explicitly
     */
    fun convertEastAsian(number: Int): String = convert(number.toLong(), NumberSystem.EAST_ASIAN)

    /**
     * Parse word number back to Long
     * Handles Western, Indian, and East Asian number words
     *
     * @param words Word representation (e.g., "one hundred one", "five lakh", "two crore")
     * @return Long value or null if not parseable
     */
    fun parse(words: String): Long? {
        val normalized = words.lowercase().trim()
        if (normalized.isEmpty()) return null

        // Direct single word lookup
        wordToNumber[normalized]?.let { return it }

        // Try parsing as digit
        normalized.toLongOrNull()?.let { return it }

        // Parse compound numbers (handles all systems)
        return parseCompound(normalized)
    }

    /**
     * Parse to Int (for backward compatibility)
     */
    fun parseToInt(words: String): Int? = parse(words)?.toInt()

    private fun parseCompound(words: String): Long? {
        // Filter out "and" which is commonly used in spoken numbers
        // e.g., "one thousand two hundred and fifty three"
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
                    // Handle multipliers (thousand, lakh, crore, million, billion, etc.)
                    if (current == 0L) current = 1L  // Handle "one lakh" vs just "lakh"
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
     *
     * @param number The number to format
     * @param system The number system for grouping
     * @return Formatted string with appropriate separators
     */
    fun formatWithGrouping(number: Long, system: NumberSystem = defaultSystem): String {
        return when (system) {
            NumberSystem.INDIAN -> formatIndianGrouping(number)
            else -> formatWesternGrouping(number)
        }
    }

    private fun formatWesternGrouping(number: Long): String {
        // Standard: 1,234,567,890
        return number.toString().reversed().chunked(3).joinToString(",").reversed()
    }

    private fun formatIndianGrouping(number: Long): String {
        // Indian: 1,23,45,67,890 (first group of 3, then groups of 2)
        val str = number.toString()
        if (str.length <= 3) return str

        val lastThree = str.takeLast(3)
        val remaining = str.dropLast(3)
        val grouped = remaining.reversed().chunked(2).joinToString(",").reversed()
        return "$grouped,$lastThree"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Suffix Handling (percent, units, etc.)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Unit suffixes and their spoken equivalents (non-currency)
     */
    private val unitSuffixMap = mapOf(
        // Percentage
        "%" to "percent",

        // Temperature
        "°" to "degrees",
        "°C" to "degrees celsius",
        "°F" to "degrees fahrenheit",
        "K" to "kelvin",

        // Length - Metric
        "km" to "kilometers",
        "m" to "meters",
        "cm" to "centimeters",
        "mm" to "millimeters",
        "μm" to "micrometers",
        "nm" to "nanometers",

        // Length - Imperial
        "mi" to "miles",
        "yd" to "yards",
        "ft" to "feet",
        "in" to "inches",

        // Weight - Metric
        "kg" to "kilograms",
        "g" to "grams",
        "mg" to "milligrams",
        "μg" to "micrograms",
        "t" to "tonnes",

        // Weight - Imperial
        "lb" to "pounds",
        "lbs" to "pounds",
        "oz" to "ounces",
        "st" to "stone",

        // Volume - Metric
        "L" to "liters",
        "l" to "liters",
        "ml" to "milliliters",
        "mL" to "milliliters",
        "cl" to "centiliters",

        // Volume - Imperial
        "gal" to "gallons",
        "qt" to "quarts",
        "pt" to "pints",
        "fl oz" to "fluid ounces",

        // Data Storage
        "B" to "bytes",
        "KB" to "kilobytes",
        "MB" to "megabytes",
        "GB" to "gigabytes",
        "TB" to "terabytes",
        "PB" to "petabytes",
        "KiB" to "kibibytes",
        "MiB" to "mebibytes",
        "GiB" to "gibibytes",
        "TiB" to "tebibytes",

        // Frequency
        "Hz" to "hertz",
        "kHz" to "kilohertz",
        "MHz" to "megahertz",
        "GHz" to "gigahertz",

        // Time
        "ms" to "milliseconds",
        "μs" to "microseconds",
        "ns" to "nanoseconds",
        "s" to "seconds",
        "sec" to "seconds",
        "min" to "minutes",
        "hr" to "hours",
        "hrs" to "hours",

        // Speed
        "mph" to "miles per hour",
        "km/h" to "kilometers per hour",
        "kmph" to "kilometers per hour",
        "kph" to "kilometers per hour",
        "m/s" to "meters per second",
        "fps" to "frames per second",
        "kbps" to "kilobits per second",
        "mbps" to "megabits per second",
        "Mbps" to "megabits per second",
        "Gbps" to "gigabits per second",

        // Power & Energy
        "W" to "watts",
        "kW" to "kilowatts",
        "MW" to "megawatts",
        "Wh" to "watt hours",
        "kWh" to "kilowatt hours",
        "V" to "volts",
        "mV" to "millivolts",
        "A" to "amps",
        "mA" to "milliamps",
        "mAh" to "milliamp hours",
        "Ah" to "amp hours",

        // Display & Graphics
        "px" to "pixels",
        "pt" to "points",
        "dp" to "density independent pixels",
        "sp" to "scale independent pixels",
        "dpi" to "dots per inch",
        "ppi" to "pixels per inch",

        // Audio
        "dB" to "decibels",
        "dBA" to "decibels a weighted",

        // Pressure
        "Pa" to "pascals",
        "kPa" to "kilopascals",
        "psi" to "pounds per square inch",
        "bar" to "bar",
        "atm" to "atmospheres",

        // Angle
        "deg" to "degrees",
        "rad" to "radians",

        // Area
        "m²" to "square meters",
        "km²" to "square kilometers",
        "ft²" to "square feet",
        "mi²" to "square miles",
        "ac" to "acres",
        "ha" to "hectares"
    )

    /**
     * Combined suffix map including currencies and units
     * Currencies are checked with special prefix handling
     */
    private val suffixMap: Map<String, String> by lazy {
        unitSuffixMap + currencyMap
    }

    /**
     * Currency symbols that appear as prefixes (before the number)
     */
    private val prefixCurrencies = setOf(
        "$", "US$", "C$", "A$", "NZ$", "HK$", "S$", "NT$", "MX$",
        "£", "€", "¥", "CN¥", "₹", "₽", "₩", "₱", "₺", "₴", "₦", "₵", "₿", "Ξ",
        "৳", "₫", "₪", "R$", "E£"
    )

    /**
     * Convert a number with suffix to its word representation.
     *
     * Examples:
     *   "143%" -> "one hundred forty three percent"
     *   "$50" -> "fifty dollars"
     *   "₹5,00,000" -> "five lakh rupees" (with Indian system)
     *   "25°C" -> "twenty five degrees celsius"
     *   "100GB" -> "one hundred gigabytes"
     *
     * @param numberWithSuffix String containing number and optional suffix
     * @param system Number system to use for conversion
     * @return Word representation or null if not parseable
     */
    fun convertWithSuffix(
        numberWithSuffix: String,
        system: NumberSystem = defaultSystem
    ): String? {
        val trimmed = numberWithSuffix.trim().replace(",", "")  // Remove grouping separators
        if (trimmed.isEmpty()) return null

        // Check for prefix currencies (e.g., $50, ₹500, €100)
        for ((symbol, word) in currencyMap) {
            if (symbol in prefixCurrencies && trimmed.startsWith(symbol)) {
                val numberPart = trimmed.removePrefix(symbol).trim()
                val number = numberPart.toLongOrNull() ?: continue
                return "${convert(number, system)} $word"
            }
        }

        // Check for suffix currencies and codes (e.g., 50 USD, 100 INR)
        for ((symbol, word) in currencyMap) {
            if (trimmed.endsWith(symbol) || trimmed.endsWith(" $symbol")) {
                val numberPart = trimmed.removeSuffix(symbol).trim()
                val number = numberPart.toLongOrNull() ?: continue
                return "${convert(number, system)} $word"
            }
        }

        // Check for unit suffixes (e.g., 143%, 25°C, 100GB)
        for ((symbol, word) in unitSuffixMap) {
            if (trimmed.endsWith(symbol)) {
                val numberPart = trimmed.removeSuffix(symbol).trim()
                val number = numberPart.toLongOrNull() ?: continue
                return "${convert(number, system)} $word"
            }
        }

        // No suffix found, try plain number
        val number = trimmed.toLongOrNull() ?: return null
        return convert(number, system)
    }

    /**
     * Parse a number with suffix from words back to the original format.
     *
     * Examples:
     *   "one hundred forty three percent" -> "143%"
     *   "fifty dollars" -> "$50"
     *   "five lakh rupees" -> "₹500000"
     *   "two crore rupees" -> "₹20000000"
     *   "twenty five degrees celsius" -> "25°C"
     *
     * @param words Word representation with suffix
     * @param outputSystem Number system for formatting output
     * @return Original format string or null if not parseable
     */
    fun parseWithSuffix(words: String, outputSystem: NumberSystem = defaultSystem): String? {
        val normalized = words.lowercase().trim()
        if (normalized.isEmpty()) return null

        // Check for currency words
        for ((symbol, word) in currencyMap) {
            if (normalized.endsWith(word) || normalized.endsWith("${word}s")) {
                val wordToRemove = if (normalized.endsWith("${word}s")) "${word}s" else word
                val numberWords = normalized.removeSuffix(wordToRemove).trim()
                val number = parse(numberWords) ?: continue

                // Format based on currency type
                return if (symbol in prefixCurrencies) {
                    "$symbol${formatWithGrouping(number, outputSystem)}"
                } else {
                    "${formatWithGrouping(number, outputSystem)} $symbol"
                }
            }
        }

        // Check for unit suffix words
        for ((symbol, word) in unitSuffixMap) {
            if (normalized.endsWith(word)) {
                val numberWords = normalized.removeSuffix(word).trim()
                val number = parse(numberWords) ?: continue
                return "$number$symbol"
            }
        }

        // No suffix found, try plain number
        val number = parse(normalized) ?: return null
        return number.toString()
    }

    /**
     * Convert currency amount with appropriate number system
     *
     * Examples:
     *   convertCurrency(500000, "INR") -> "five lakh indian rupees"
     *   convertCurrency(500000, "USD") -> "five hundred thousand us dollars"
     *
     * @param amount The amount
     * @param currencyCode ISO currency code (e.g., "INR", "USD", "EUR")
     * @return Word representation with appropriate number system
     */
    fun convertCurrency(amount: Long, currencyCode: String): String? {
        val code = currencyCode.uppercase()
        val currencyWord = currencyMap[code] ?: return null

        // Determine appropriate number system based on currency
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
     * Get all supported suffix words
     */
    fun getSupportedSuffixWords(): Set<String> = suffixMap.values.toSet()

    /**
     * Get all supported currency codes
     */
    fun getSupportedCurrencyCodes(): Set<String> = currencyMap.keys

    /**
     * Get currency word for a symbol or code
     */
    fun getCurrencyWord(symbolOrCode: String): String? = currencyMap[symbolOrCode]

    /**
     * Check if a currency uses prefix notation (symbol before number)
     */
    fun isCurrencyPrefix(symbol: String): Boolean = symbol in prefixCurrencies
}
