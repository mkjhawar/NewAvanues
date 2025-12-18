package com.augmentalis.actions.entities

/**
 * Base interface for entity extraction from user utterances.
 *
 * Entity extractors parse natural language to extract structured data
 * (entities) that action handlers need to execute intents.
 *
 * Examples:
 * - "search for cats" → query: "cats"
 * - "call John at 555-1234" → recipient: "John", phone: "555-1234"
 * - "set alarm for 7 AM" → time: "07:00"
 *
 * Design Principles:
 * - One extractor per entity type
 * - Stateless and reusable
 * - Returns null for missing/invalid entities
 * - Handles variations and synonyms
 * - Fast (<50ms typical)
 *
 * Future Enhancement:
 * - Integrate with NLU entity recognition
 * - Use NER (Named Entity Recognition) models
 * - Support multi-language extraction
 */
interface EntityExtractor<T> {
    /**
     * Extract entity from user utterance.
     *
     * @param utterance The user's natural language input
     * @return Extracted entity or null if not found/invalid
     */
    fun extract(utterance: String): T?
}

/**
 * Extracts web search queries from utterances.
 *
 * Patterns:
 * - "search for X" → "X"
 * - "google Y" → "Y"
 * - "look up Z" → "Z"
 * - "what is A" → "A"
 */
object QueryEntityExtractor : EntityExtractor<String> {
    private val patterns = listOf(
        Regex("search for (.+)", RegexOption.IGNORE_CASE),
        Regex("search (.+)", RegexOption.IGNORE_CASE),
        Regex("google (.+)", RegexOption.IGNORE_CASE),
        Regex("look up (.+)", RegexOption.IGNORE_CASE),
        Regex("find (.+)", RegexOption.IGNORE_CASE),
        Regex("what is (.+)", RegexOption.IGNORE_CASE),
        Regex("who is (.+)", RegexOption.IGNORE_CASE),
        Regex("how to (.+)", RegexOption.IGNORE_CASE),
        Regex("how do i (.+)", RegexOption.IGNORE_CASE),
        Regex("when is (.+)", RegexOption.IGNORE_CASE),
        Regex("where is (.+)", RegexOption.IGNORE_CASE),
        Regex("why (.+)", RegexOption.IGNORE_CASE)
    )

    override fun extract(utterance: String): String? {
        patterns.forEach { pattern ->
            pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                return match.trim()
            }
        }
        return null
    }
}

/**
 * Extracts URLs from utterances.
 *
 * Patterns:
 * - "go to website.com" → "https://website.com"
 * - "open youtube.com" → "https://youtube.com"
 * - "navigate to example.org" → "https://example.org"
 */
object URLEntityExtractor : EntityExtractor<String> {
    private val patterns = listOf(
        Regex("go to ([\\w.-]+\\.[a-z]{2,})", RegexOption.IGNORE_CASE),
        Regex("open ([\\w.-]+\\.[a-z]{2,})", RegexOption.IGNORE_CASE),
        Regex("navigate to ([\\w.-]+\\.[a-z]{2,})", RegexOption.IGNORE_CASE),
        Regex("visit ([\\w.-]+\\.[a-z]{2,})", RegexOption.IGNORE_CASE),
        Regex("browse to ([\\w.-]+\\.[a-z]{2,})", RegexOption.IGNORE_CASE),
        // Direct URL pattern (http://... or https://...)
        Regex("(https?://[\\w.-]+\\.[a-z]{2,}(?:/\\S*)?)", RegexOption.IGNORE_CASE)
    )

    override fun extract(utterance: String): String? {
        patterns.forEach { pattern ->
            pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                val url = match.trim()
                // Add https:// if not present
                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else {
                    "https://$url"
                }
            }
        }
        return null
    }
}

/**
 * Extracts phone numbers from utterances.
 *
 * Patterns:
 * - "call 555-1234" → "555-1234"
 * - "phone 555 1234" → "5551234"
 * - "dial 1-800-FLOWERS" → "1-800-FLOWERS"
 */
object PhoneNumberEntityExtractor : EntityExtractor<String> {
    private val patterns = listOf(
        // US phone numbers: 555-1234, (555) 123-4567, 555.123.4567
        Regex("call ([\\d-().\\s]+)", RegexOption.IGNORE_CASE),
        Regex("phone ([\\d-().\\s]+)", RegexOption.IGNORE_CASE),
        Regex("dial ([\\d-().\\s]+)", RegexOption.IGNORE_CASE),
        // Alphanumeric: 1-800-FLOWERS
        Regex("call ([\\d-]+[A-Z]+[\\d-]*)", RegexOption.IGNORE_CASE),
        Regex("dial ([\\d-]+[A-Z]+[\\d-]*)", RegexOption.IGNORE_CASE)
    )

    override fun extract(utterance: String): String? {
        patterns.forEach { pattern ->
            pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                return match.trim()
            }
        }
        return null
    }
}

/**
 * Data class for recipient extraction (name + optional phone/email).
 */
data class Recipient(
    val name: String?,
    val phoneNumber: String? = null,
    val email: String? = null
)

/**
 * Extracts recipient information from utterances.
 *
 * Patterns:
 * - "text mom" → Recipient(name="mom")
 * - "call John at 555-1234" → Recipient(name="John", phone="555-1234")
 * - "email alice@example.com" → Recipient(email="alice@example.com")
 */
object RecipientEntityExtractor : EntityExtractor<Recipient> {
    private val namePatterns = listOf(
        Regex("(?:text|call|email|message|send to) ([\\w\\s]+?)(?:at|about|saying|$)", RegexOption.IGNORE_CASE),
        Regex("(?:text|call|email) ([\\w\\s]+)", RegexOption.IGNORE_CASE)
    )

    private val emailPattern = Regex("([\\w.-]+@[\\w.-]+\\.[a-z]{2,})", RegexOption.IGNORE_CASE)

    override fun extract(utterance: String): Recipient? {
        // Try to extract email first
        val email = emailPattern.find(utterance)?.groupValues?.getOrNull(1)
        if (email != null) {
            return Recipient(name = null, email = email)
        }

        // Try to extract phone number
        val phoneNumber = PhoneNumberEntityExtractor.extract(utterance)

        // Extract name
        namePatterns.forEach { pattern ->
            pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                val name = match.trim()
                if (name.isNotEmpty()) {
                    return Recipient(name = name, phoneNumber = phoneNumber)
                }
            }
        }

        return null
    }
}

/**
 * Extracts message content from utterances.
 *
 * Patterns:
 * - "text mom saying hello" → "hello"
 * - "send message that I'm running late" → "I'm running late"
 * - "text dad I'll be there soon" → "I'll be there soon"
 */
object MessageEntityExtractor : EntityExtractor<String> {
    private val patterns = listOf(
        Regex("(?:saying|that) (.+)", RegexOption.IGNORE_CASE),
        Regex("(?:message|text) (?:[\\w\\s]+?) (.+)", RegexOption.IGNORE_CASE)
    )

    override fun extract(utterance: String): String? {
        patterns.forEach { pattern ->
            pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                val message = match.trim()
                if (message.isNotEmpty()) {
                    return message
                }
            }
        }
        return null
    }
}
