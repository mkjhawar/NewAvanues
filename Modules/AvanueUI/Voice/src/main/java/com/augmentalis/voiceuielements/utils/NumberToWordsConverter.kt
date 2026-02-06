/**
 * NumberToWordsConverter.kt - Multi-language number to words conversion
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-04
 * 
 * Converts numeric values to spoken words across supported languages.
 * Used for voice command disambiguation with Vosk and other providers.
 */
package com.augmentalis.voiceuielements.utils

import java.util.*

/**
 * Multi-language number to words converter
 * Supports numbers 1-999 across major languages
 */
object NumberToWordsConverter {
    
    private val englishOnes = arrayOf(
        "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", 
        "seventeen", "eighteen", "nineteen"
    )
    
    private val englishTens = arrayOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    )
    
    private val spanishOnes = arrayOf(
        "", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
        "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", 
        "diecisiete", "dieciocho", "diecinueve"
    )
    
    private val spanishTens = arrayOf(
        "", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"
    )
    
    private val frenchOnes = arrayOf(
        "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
        "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", 
        "dix-sept", "dix-huit", "dix-neuf"
    )
    
    private val frenchTens = arrayOf(
        "", "", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante-dix", "quatre-vingt", "quatre-vingt-dix"
    )
    
    private val germanOnes = arrayOf(
        "", "eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun",
        "zehn", "elf", "zwölf", "dreizehn", "vierzehn", "fünfzehn", "sechzehn", 
        "siebzehn", "achtzehn", "neunzehn"
    )
    
    private val germanTens = arrayOf(
        "", "", "zwanzig", "dreißig", "vierzig", "fünfzig", "sechzig", "siebzig", "achtzig", "neunzig"
    )
    
    private val italianOnes = arrayOf(
        "", "uno", "due", "tre", "quattro", "cinque", "sei", "sette", "otto", "nove",
        "dieci", "undici", "dodici", "tredici", "quattordici", "quindici", "sedici", 
        "diciassette", "diciotto", "diciannove"
    )
    
    private val italianTens = arrayOf(
        "", "", "venti", "trenta", "quaranta", "cinquanta", "sessanta", "settanta", "ottanta", "novanta"
    )
    
    private val portugueseOnes = arrayOf(
        "", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove",
        "dez", "onze", "doze", "treze", "quatorze", "quinze", "dezesseis", 
        "dezessete", "dezoito", "dezenove"
    )
    
    private val portugueseTens = arrayOf(
        "", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa"
    )
    
    private val russianOnes = arrayOf(
        "", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять",
        "десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать", "шестнадцать", 
        "семнадцать", "восемнадцать", "девятнадцать"
    )
    
    private val russianTens = arrayOf(
        "", "", "двадцать", "тридцать", "сорок", "пятьдесят", "шестьдесят", "семьдесят", "восемьдесят", "девяносто"
    )
    
    private val chineseOnes = arrayOf(
        "", "一", "二", "三", "四", "五", "六", "七", "八", "九",
        "十", "十一", "十二", "十三", "十四", "十五", "十六", 
        "十七", "十八", "十九"
    )
    
    private val chineseTens = arrayOf(
        "", "", "二十", "三十", "四十", "五十", "六十", "七十", "八十", "九十"
    )
    
    private val japaneseOnes = arrayOf(
        "", "一", "二", "三", "四", "五", "六", "七", "八", "九",
        "十", "十一", "十二", "十三", "十四", "十五", "十六", 
        "十七", "十八", "十九"
    )
    
    private val japaneseTens = arrayOf(
        "", "", "二十", "三十", "四十", "五十", "六十", "七十", "八十", "九十"
    )
    
    /**
     * Convert number to words based on locale
     */
    fun numberToWords(number: Int, locale: Locale = Locale.getDefault()): String {
        if (number < 0 || number > 999) return number.toString()
        if (number == 0) return getZeroWord(locale)
        
        return when (locale.language.lowercase()) {
            "en" -> convertEnglish(number)
            "es" -> convertSpanish(number)
            "fr" -> convertFrench(number)
            "de" -> convertGerman(number)
            "it" -> convertItalian(number)
            "pt" -> convertPortuguese(number)
            "ru" -> convertRussian(number)
            "zh" -> convertChinese(number)
            "ja" -> convertJapanese(number)
            else -> convertEnglish(number) // Default to English
        }
    }
    
    /**
     * Convert number to words with language code
     */
    fun numberToWords(number: Int, languageCode: String): String {
        val locale = Locale.Builder().setLanguage(languageCode).build()
        return numberToWords(number, locale)
    }
    
    /**
     * Generate selection command variations for different languages
     */
    fun generateSelectionCommands(number: Int, locale: Locale = Locale.getDefault()): List<String> {
        val numberWord = numberToWords(number, locale)
        val numberString = number.toString()
        
        return when (locale.language.lowercase()) {
            "en" -> listOf(
                "select $numberString",
                "select $numberWord", 
                "tap $numberString",
                "tap $numberWord",
                "click $numberString",
                "click $numberWord",
                "choose $numberString",
                "choose $numberWord",
                numberString,
                numberWord
            )
            "es" -> listOf(
                "seleccionar $numberString",
                "seleccionar $numberWord",
                "tocar $numberString", 
                "tocar $numberWord",
                "elegir $numberString",
                "elegir $numberWord",
                numberString,
                numberWord
            )
            "fr" -> listOf(
                "sélectionner $numberString",
                "sélectionner $numberWord",
                "appuyer $numberString",
                "appuyer $numberWord", 
                "choisir $numberString",
                "choisir $numberWord",
                numberString,
                numberWord
            )
            "de" -> listOf(
                "auswählen $numberString",
                "auswählen $numberWord",
                "tippen $numberString",
                "tippen $numberWord",
                "wählen $numberString", 
                "wählen $numberWord",
                numberString,
                numberWord
            )
            else -> generateSelectionCommands(number, Locale.ENGLISH)
        }
    }
    
    private fun getZeroWord(locale: Locale): String {
        return when (locale.language.lowercase()) {
            "en" -> "zero"
            "es" -> "cero"
            "fr" -> "zéro"
            "de" -> "null"
            "it" -> "zero"
            "pt" -> "zero"
            "ru" -> "ноль"
            "zh" -> "零"
            "ja" -> "零"
            else -> "zero"
        }
    }
    
    private fun convertEnglish(number: Int): String {
        if (number < 20) return englishOnes[number]
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            return "${englishTens[tens]}${if (ones != 0) "-${englishOnes[ones]}" else ""}"
        }
        
        val hundreds = number / 100
        val remainder = number % 100
        var result = "${englishOnes[hundreds]} hundred"
        if (remainder != 0) {
            result += " ${convertEnglish(remainder)}"
        }
        return result
    }
    
    private fun convertSpanish(number: Int): String {
        if (number < 20) return spanishOnes[number]
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            return "${spanishTens[tens]}${if (ones != 0) " y ${spanishOnes[ones]}" else ""}"
        }
        
        val hundreds = number / 100
        val remainder = number % 100
        var result = "${spanishOnes[hundreds]}cientos"
        if (remainder != 0) {
            result += " ${convertSpanish(remainder)}"
        }
        return result
    }
    
    private fun convertFrench(number: Int): String {
        if (number < 20) return frenchOnes[number]
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            return "${frenchTens[tens]}${if (ones != 0) "-${frenchOnes[ones]}" else ""}"
        }
        
        val hundreds = number / 100
        val remainder = number % 100
        var result = "${frenchOnes[hundreds]} cent"
        if (remainder != 0) {
            result += " ${convertFrench(remainder)}"
        }
        return result
    }
    
    private fun convertGerman(number: Int): String {
        if (number < 20) return germanOnes[number]
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            return "${if (ones != 0) "${germanOnes[ones]}und" else ""}${germanTens[tens]}"
        }
        
        val hundreds = number / 100
        val remainder = number % 100
        var result = "${germanOnes[hundreds]}hundert"
        if (remainder != 0) {
            result += convertGerman(remainder)
        }
        return result
    }
    
    private fun convertItalian(number: Int): String {
        if (number < 20) return italianOnes[number]
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            return "${italianTens[tens]}${if (ones != 0) italianOnes[ones] else ""}"
        }
        
        val hundreds = number / 100
        val remainder = number % 100
        var result = "${italianOnes[hundreds]}cento"
        if (remainder != 0) {
            result += convertItalian(remainder)
        }
        return result
    }
    
    private fun convertPortuguese(number: Int): String {
        if (number < 20) return portugueseOnes[number]
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            return "${portugueseTens[tens]}${if (ones != 0) " e ${portugueseOnes[ones]}" else ""}"
        }
        
        val hundreds = number / 100
        val remainder = number % 100
        var result = "${portugueseOnes[hundreds]}centos"
        if (remainder != 0) {
            result += " ${convertPortuguese(remainder)}"
        }
        return result
    }
    
    private fun convertRussian(number: Int): String {
        if (number < 20) return russianOnes[number]
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            return "${russianTens[tens]}${if (ones != 0) " ${russianOnes[ones]}" else ""}"
        }
        
        val hundreds = number / 100
        val remainder = number % 100
        var result = "${russianOnes[hundreds]} сто"
        if (remainder != 0) {
            result += " ${convertRussian(remainder)}"
        }
        return result
    }
    
    private fun convertChinese(number: Int): String {
        if (number < 20) return chineseOnes[number]
        if (number < 100) {
            val tens = number / 10
            val ones = number % 10
            return "${chineseTens[tens]}${if (ones != 0) chineseOnes[ones] else ""}"
        }
        
        val hundreds = number / 100
        val remainder = number % 100
        var result = "${chineseOnes[hundreds]}百"
        if (remainder != 0) {
            result += convertChinese(remainder)
        }
        return result
    }
    
    private fun convertJapanese(number: Int): String {
        if (number < 20) return japaneseOnes[number]
        if (number < 100) {
            val tens = number / 10
            val ones = number % 100 - (tens * 10)
            return "${japaneseTens[tens]}${if (ones != 0) japaneseOnes[ones] else ""}"
        }
        
        val hundreds = number / 100
        val remainder = number % 100
        var result = "${japaneseOnes[hundreds]}百"
        if (remainder != 0) {
            result += convertJapanese(remainder)
        }
        return result
    }
}