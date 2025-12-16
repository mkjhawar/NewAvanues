package com.augmentalis.ava.features.actions.entities

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for entity extractors.
 *
 * Tests various utterance patterns to ensure accurate entity extraction.
 */
class EntityExtractorTest {

    // ===================================================================================
    // Query Entity Extractor Tests
    // ===================================================================================

    @Test
    fun `QueryEntityExtractor - search for pattern`() {
        val testCases = mapOf(
            "search for cats" to "cats",
            "search for kotlin tutorials" to "kotlin tutorials",
            "search for pizza near me" to "pizza near me"
        )

        testCases.forEach { (utterance, expected) ->
            val result = QueryEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `QueryEntityExtractor - google pattern`() {
        val testCases = mapOf(
            "google kotlin" to "kotlin",
            "google best restaurants" to "best restaurants"
        )

        testCases.forEach { (utterance, expected) ->
            val result = QueryEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `QueryEntityExtractor - what is pattern`() {
        val testCases = mapOf(
            "what is quantum computing" to "quantum computing",
            "what is the weather" to "the weather",
            "who is albert einstein" to "albert einstein",
            "how to learn python" to "learn python"
        )

        testCases.forEach { (utterance, expected) ->
            val result = QueryEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `QueryEntityExtractor - returns null for invalid input`() {
        val testCases = listOf(
            "hello",
            "turn on wifi",
            "call mom"
        )

        testCases.forEach { utterance ->
            val result = QueryEntityExtractor.extract(utterance)
            assertThat(result).isNull()
        }
    }

    // ===================================================================================
    // URL Entity Extractor Tests
    // ===================================================================================

    @Test
    fun `URLEntityExtractor - go to pattern`() {
        val testCases = mapOf(
            "go to youtube.com" to "https://youtube.com",
            "go to google.com" to "https://google.com",
            "go to example.org" to "https://example.org"
        )

        testCases.forEach { (utterance, expected) ->
            val result = URLEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `URLEntityExtractor - open pattern`() {
        val testCases = mapOf(
            "open github.com" to "https://github.com",
            "open reddit.com" to "https://reddit.com"
        )

        testCases.forEach { (utterance, expected) ->
            val result = URLEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `URLEntityExtractor - full URL with protocol`() {
        val testCases = mapOf(
            "go to https://github.com" to "https://github.com",
            "open http://example.com" to "http://example.com"
        )

        testCases.forEach { (utterance, expected) ->
            val result = URLEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `URLEntityExtractor - returns null for invalid input`() {
        val testCases = listOf(
            "search for cats",
            "call mom",
            "turn on wifi"
        )

        testCases.forEach { utterance ->
            val result = URLEntityExtractor.extract(utterance)
            assertThat(result).isNull()
        }
    }

    // ===================================================================================
    // Phone Number Entity Extractor Tests
    // ===================================================================================

    @Test
    fun `PhoneNumberEntityExtractor - call pattern with dashes`() {
        val testCases = mapOf(
            "call 555-1234" to "555-1234",
            "dial 1-800-FLOWERS" to "1-800-FLOWERS",
            "phone 555-123-4567" to "555-123-4567"
        )

        testCases.forEach { (utterance, expected) ->
            val result = PhoneNumberEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `PhoneNumberEntityExtractor - dial pattern`() {
        val testCases = mapOf(
            "dial 555 1234" to "555 1234",
            "phone 5551234" to "5551234"
        )

        testCases.forEach { (utterance, expected) ->
            val result = PhoneNumberEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `PhoneNumberEntityExtractor - returns null for invalid input`() {
        val testCases = listOf(
            "call mom",
            "text dad",
            "search for cats"
        )

        testCases.forEach { utterance ->
            val result = PhoneNumberEntityExtractor.extract(utterance)
            assertThat(result).isNull()
        }
    }

    // ===================================================================================
    // Recipient Entity Extractor Tests
    // ===================================================================================

    @Test
    fun `RecipientEntityExtractor - text pattern with name`() {
        val testCases = mapOf(
            "text mom" to "mom",
            "text dad" to "dad",
            "text john" to "john"
        )

        testCases.forEach { (utterance, expectedName) ->
            val result = RecipientEntityExtractor.extract(utterance)
            assertThat(result).isNotNull()
            assertThat(result!!.name).isEqualTo(expectedName)
        }
    }

    @Test
    fun `RecipientEntityExtractor - call pattern with name and phone`() {
        val utterance = "call john at 555-1234"
        val result = RecipientEntityExtractor.extract(utterance)

        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("john")
        assertThat(result.phoneNumber).isEqualTo("555-1234")
    }

    @Test
    fun `RecipientEntityExtractor - email pattern`() {
        val utterance = "email alice@example.com"
        val result = RecipientEntityExtractor.extract(utterance)

        assertThat(result).isNotNull()
        assertThat(result!!.email).isEqualTo("alice@example.com")
    }

    @Test
    fun `RecipientEntityExtractor - returns null for invalid input`() {
        val testCases = listOf(
            "search for cats",
            "turn on wifi",
            "google kotlin"
        )

        testCases.forEach { utterance ->
            val result = RecipientEntityExtractor.extract(utterance)
            assertThat(result).isNull()
        }
    }

    // ===================================================================================
    // Message Entity Extractor Tests
    // ===================================================================================

    @Test
    fun `MessageEntityExtractor - saying pattern`() {
        val testCases = mapOf(
            "text mom saying hello" to "hello",
            "message dad saying I'm running late" to "I'm running late",
            "text john saying see you soon" to "see you soon"
        )

        testCases.forEach { (utterance, expected) ->
            val result = MessageEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `MessageEntityExtractor - that pattern`() {
        val testCases = mapOf(
            "send message that I'll be there" to "I'll be there",
            "text dad that I'm on my way" to "I'm on my way"
        )

        testCases.forEach { (utterance, expected) ->
            val result = MessageEntityExtractor.extract(utterance)
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `MessageEntityExtractor - returns null when no message`() {
        val testCases = listOf(
            "text mom",
            "call dad",
            "search for cats"
        )

        testCases.forEach { utterance ->
            val result = MessageEntityExtractor.extract(utterance)
            assertThat(result).isNull()
        }
    }
}
