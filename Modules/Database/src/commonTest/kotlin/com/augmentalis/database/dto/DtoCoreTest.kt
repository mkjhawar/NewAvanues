package com.augmentalis.database.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for core Database DTO data classes.
 *
 * These tests verify construction, copy semantics, and equals/hashCode
 * contracts for the most-used DTOs in the scraping pipeline.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class DtoCoreTest {

    // ── ScrapedAppDTO ─────────────────────────────────────────────────────────

    @Test
    fun scrapedAppDTO_construction_and_property_access() {
        val dto = ScrapedAppDTO(
            appId = "app_001",
            packageName = "com.example.app",
            versionCode = 120L,
            versionName = "1.2.0",
            appHash = "abc123",
            firstScrapedAt = 1706300000000L,
            lastScrapedAt = 1706300500000L
        )

        assertEquals("app_001", dto.appId)
        assertEquals("com.example.app", dto.packageName)
        assertEquals(120L, dto.versionCode)
        assertEquals("1.2.0", dto.versionName)
        assertEquals(0L, dto.isFullyLearned)   // default
        assertEquals("DYNAMIC", dto.scrapingMode) // default
    }

    @Test
    fun scrapedAppDTO_equals_same_data() {
        val dto1 = ScrapedAppDTO(
            appId = "app_001", packageName = "com.app", versionCode = 1L,
            versionName = "1.0", appHash = "h1", firstScrapedAt = 100L, lastScrapedAt = 200L
        )
        val dto2 = dto1.copy()
        assertEquals(dto1, dto2)
        assertEquals(dto1.hashCode(), dto2.hashCode())
    }

    @Test
    fun scrapedAppDTO_copy_with_changed_field_is_not_equal() {
        val dto1 = ScrapedAppDTO(
            appId = "app_001", packageName = "com.app", versionCode = 1L,
            versionName = "1.0", appHash = "h1", firstScrapedAt = 100L, lastScrapedAt = 200L
        )
        val dto2 = dto1.copy(packageName = "com.other")
        assertNotEquals(dto1, dto2)
    }

    @Test
    fun scrapedAppDTO_optional_pkgHash_defaults_to_null() {
        val dto = ScrapedAppDTO(
            appId = "x", packageName = "com.x", versionCode = 1L,
            versionName = "1.0", appHash = "h", firstScrapedAt = 0L, lastScrapedAt = 0L
        )
        assertNull(dto.pkgHash)
    }

    @Test
    fun scrapedAppDTO_optional_pkgHash_set() {
        val dto = ScrapedAppDTO(
            appId = "x", packageName = "com.x", versionCode = 1L,
            versionName = "1.0", appHash = "h", firstScrapedAt = 0L, lastScrapedAt = 0L,
            pkgHash = "a3f2e1"
        )
        assertEquals("a3f2e1", dto.pkgHash)
    }

    // ── ScrapedElementDTO ─────────────────────────────────────────────────────

    @Test
    fun scrapedElementDTO_construction_and_property_access() {
        val dto = ScrapedElementDTO(
            id = 1L,
            elementHash = "elemhash123",
            appId = "app_001",
            uuid = "uuid-abc",
            className = "android.widget.Button",
            viewIdResourceName = "com.app:id/btn_submit",
            text = "Submit",
            contentDescription = "Submit button",
            bounds = """{"left":0,"top":0,"right":200,"bottom":60}""",
            isClickable = 1L,
            isLongClickable = 0L,
            isEditable = 0L,
            isScrollable = 0L,
            isCheckable = 0L,
            isFocusable = 1L,
            isEnabled = 1L,
            depth = 3L,
            indexInParent = 2L,
            scrapedAt = 1706300000000L,
            semanticRole = "button",
            inputType = null,
            visualWeight = null,
            isRequired = null,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = "screen_abc"
        )

        assertEquals(1L, dto.id)
        assertEquals("elemhash123", dto.elementHash)
        assertEquals("android.widget.Button", dto.className)
        assertEquals(1L, dto.isClickable)
        assertEquals("Submit", dto.text)
        assertNotNull(dto.screen_hash)
    }

    @Test
    fun scrapedElementDTO_equals_and_hashCode_contract() {
        val dto1 = ScrapedElementDTO(
            id = 5L, elementHash = "h", appId = "app", uuid = null, className = "View",
            viewIdResourceName = null, text = null, contentDescription = null,
            bounds = "{}", isClickable = 0L, isLongClickable = 0L, isEditable = 0L,
            isScrollable = 0L, isCheckable = 0L, isFocusable = 0L, isEnabled = 1L,
            depth = 1L, indexInParent = 0L, scrapedAt = 0L,
            semanticRole = null, inputType = null, visualWeight = null,
            isRequired = null, formGroupId = null, placeholderText = null,
            validationPattern = null, backgroundColor = null, screen_hash = null
        )
        val dto2 = dto1.copy()
        assertEquals(dto1, dto2)
        assertEquals(dto1.hashCode(), dto2.hashCode())
    }

    // ── GeneratedCommandDTO ───────────────────────────────────────────────────

    @Test
    fun generatedCommandDTO_construction_with_defaults() {
        val dto = GeneratedCommandDTO(
            id = 10L,
            elementHash = "elem_abc",
            commandText = "click submit",
            actionType = "CLICK",
            confidence = 0.95,
            synonyms = null,
            createdAt = 1706300000000L
        )

        assertEquals(10L, dto.id)
        assertEquals("CLICK", dto.actionType)
        assertEquals(0.95, dto.confidence)
        assertEquals(0L, dto.isUserApproved)    // default
        assertEquals(0L, dto.usageCount)         // default
        assertEquals("", dto.appId)              // default
        assertEquals(0L, dto.isDeprecated)       // default
    }

    @Test
    fun generatedCommandDTO_version_tracking_fields() {
        val dto = GeneratedCommandDTO(
            id = 1L, elementHash = "h", commandText = "tap ok",
            actionType = "CLICK", confidence = 0.9, synonyms = null,
            createdAt = 0L, appVersion = "8.2024.11.123", versionCode = 82024L,
            lastVerified = 1706300000000L, isDeprecated = 0L
        )
        assertEquals("8.2024.11.123", dto.appVersion)
        assertEquals(82024L, dto.versionCode)
        assertNotNull(dto.lastVerified)
        assertEquals(0L, dto.isDeprecated)
    }

    // ── PhraseSuggestionDTO ───────────────────────────────────────────────────

    @Test
    fun phraseSuggestionDTO_construction_and_equals() {
        val dto = PhraseSuggestionDTO(
            id = 1L,
            commandId = "nav_back",
            originalPhrase = "go back",
            suggestedPhrase = "navigate back",
            locale = "en-US",
            createdAt = 1706300000000L,
            status = "pending",
            source = "user"
        )
        assertEquals("nav_back", dto.commandId)
        assertEquals("en-US", dto.locale)
        assertEquals("pending", dto.status)
        assertEquals(dto, dto.copy())
    }

    // ── VoiceCommandDTO.create factory ────────────────────────────────────────

    @Test
    fun voiceCommandDTO_create_sets_id_to_zero() {
        val dto = VoiceCommandDTO.create(
            commandId = "nav_back",
            locale = "en-US",
            triggerPhrase = "go back",
            action = "NAVIGATE_BACK",
            category = "NAVIGATION"
        )
        assertEquals(0L, dto.id) // placeholder for DB auto-increment
        assertEquals("nav_back", dto.commandId)
        assertEquals("en-US", dto.locale)
        assertEquals("app", dto.domain) // default
    }

    @Test
    fun voiceCommandDTO_create_isEnabled_true_by_default() {
        val dto = VoiceCommandDTO.create(
            commandId = "test", locale = "en-US", triggerPhrase = "test",
            action = "CLICK", category = "UI"
        )
        assertEquals(1L, dto.isEnabled)
    }

    @Test
    fun voiceCommandDTO_create_isFallback_false_by_default() {
        val dto = VoiceCommandDTO.create(
            commandId = "test", locale = "en-US", triggerPhrase = "test",
            action = "CLICK", category = "UI"
        )
        assertEquals(0L, dto.isFallback)
    }

    @Test
    fun voiceCommandDTO_create_timestamps_are_positive() {
        val dto = VoiceCommandDTO.create(
            commandId = "test", locale = "en-US", triggerPhrase = "test",
            action = "CLICK", category = "UI"
        )
        assertTrue(dto.createdAt > 0L)
        assertTrue(dto.updatedAt > 0L)
        assertEquals(dto.createdAt, dto.updatedAt)
    }
}
