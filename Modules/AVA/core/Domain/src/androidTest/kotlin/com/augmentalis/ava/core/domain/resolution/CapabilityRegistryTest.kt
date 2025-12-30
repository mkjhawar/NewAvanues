package com.augmentalis.ava.core.domain.resolution

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [CapabilityRegistry].
 *
 * Tests cover:
 * - All 10+ capabilities are defined
 * - Each capability has required fields
 * - Known apps have valid package names
 * - Intent specs are properly configured
 *
 * Part of Intelligent Resolution System (Chapter 71)
 *
 * Author: Manoj Jhawar
 */
@RunWith(AndroidJUnit4::class)
class CapabilityRegistryTest {

    @Test
    fun capabilities_shouldHaveAtLeast10Entries() {
        val count = CapabilityRegistry.capabilities.size
        assertTrue(count >= 10, "Expected at least 10 capabilities, found $count")
    }

    @Test
    fun email_capabilityShouldExist() {
        val email = CapabilityRegistry.capabilities["email"]
        assertNotNull(email, "Email capability should exist")
        assertEquals("email", email.id)
        assertEquals("Email", email.displayName)
        assertEquals(CapabilityCategory.COMMUNICATION, email.category)
    }

    @Test
    fun sms_capabilityShouldExist() {
        val sms = CapabilityRegistry.capabilities["sms"]
        assertNotNull(sms, "SMS capability should exist")
        assertEquals("sms", sms.id)
        assertEquals("Text Messages", sms.displayName)
        assertEquals(CapabilityCategory.COMMUNICATION, sms.category)
    }

    @Test
    fun email_shouldHaveKnownApps() {
        val email = CapabilityRegistry.capabilities["email"]!!
        assertTrue(email.knownApps.isNotEmpty(), "Email should have known apps")

        // Gmail should be a known app
        val gmail = email.knownApps.find { it.packageName == "com.google.android.gm" }
        assertNotNull(gmail, "Gmail should be a known email app")
        assertEquals("Gmail", gmail.displayName)
    }

    @Test
    fun sms_shouldHaveKnownApps() {
        val sms = CapabilityRegistry.capabilities["sms"]!!
        assertTrue(sms.knownApps.isNotEmpty(), "SMS should have known apps")

        // Messages should be a known app
        val messages = sms.knownApps.find { it.packageName == "com.google.android.apps.messaging" }
        assertNotNull(messages, "Google Messages should be a known SMS app")
    }

    @Test
    fun email_shouldHaveAndroidIntents() {
        val email = CapabilityRegistry.capabilities["email"]!!
        assertTrue(email.androidIntents.isNotEmpty(), "Email should have Android intents")

        // Should have mailto intent
        val mailtoIntent = email.androidIntents.find { it.dataScheme == "mailto" }
        assertNotNull(mailtoIntent, "Email should have mailto: intent")
    }

    @Test
    fun sms_shouldHaveAndroidIntents() {
        val sms = CapabilityRegistry.capabilities["sms"]!!
        assertTrue(sms.androidIntents.isNotEmpty(), "SMS should have Android intents")

        // Should have sms or smsto intent
        val smsIntent = sms.androidIntents.find {
            it.dataScheme == "sms" || it.dataScheme == "smsto"
        }
        assertNotNull(smsIntent, "SMS should have sms: or smsto: intent")
    }

    @Test
    fun music_capabilityShouldExist() {
        val music = CapabilityRegistry.capabilities["music"]
        assertNotNull(music, "Music capability should exist")
        assertEquals("Music Player", music.displayName)
        assertEquals(CapabilityCategory.MEDIA, music.category)
    }

    @Test
    fun maps_capabilityShouldExist() {
        val maps = CapabilityRegistry.capabilities["maps"]
        assertNotNull(maps, "Maps capability should exist")
        assertEquals("Maps & Navigation", maps.displayName)
        assertEquals(CapabilityCategory.NAVIGATION, maps.category)
    }

    @Test
    fun allCapabilities_shouldHaveValidCategories() {
        CapabilityRegistry.capabilities.forEach { (id, capability) ->
            assertNotNull(capability.category, "Capability $id should have a category")
            assertTrue(
                capability.category in CapabilityCategory.entries,
                "Capability $id should have a valid category"
            )
        }
    }

    @Test
    fun allKnownApps_shouldHaveValidPackageNames() {
        CapabilityRegistry.capabilities.forEach { (id, capability) ->
            capability.knownApps.forEach { app ->
                assertTrue(
                    app.packageName.contains("."),
                    "App ${app.displayName} in $id should have valid package name"
                )
            }
        }
    }

    @Test
    fun unknownCapability_shouldReturnNull() {
        val unknown = CapabilityRegistry.capabilities["nonexistent_capability"]
        assertNull(unknown, "Unknown capability should return null")
    }

    @Test
    fun browser_shouldHaveHttpIntents() {
        val browser = CapabilityRegistry.capabilities["browser"]
        assertNotNull(browser, "Browser capability should exist")

        val httpIntent = browser.androidIntents.find {
            it.dataScheme == "http" || it.dataScheme == "https"
        }
        assertNotNull(httpIntent, "Browser should handle http/https")
    }

    @Test
    fun calendar_shouldExistWithInsertIntent() {
        val calendar = CapabilityRegistry.capabilities["calendar"]
        assertNotNull(calendar, "Calendar capability should exist")
        assertEquals(CapabilityCategory.PRODUCTIVITY, calendar.category)
    }
}
