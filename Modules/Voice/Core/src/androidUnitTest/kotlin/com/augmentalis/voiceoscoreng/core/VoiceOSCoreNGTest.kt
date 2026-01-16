package com.augmentalis.voiceoscoreng.core

import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoiceOSCoreNGTest {

    @Before
    fun setup() {
        VoiceOSCoreNG.reset()
    }

    @After
    fun teardown() {
        VoiceOSCoreNG.reset()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `getInstance returns singleton`() {
        val instance1 = VoiceOSCoreNG.getInstance()
        val instance2 = VoiceOSCoreNG.getInstance()
        assertTrue(instance1 === instance2)
    }

    @Test
    fun `initialize sets tier and debug mode`() {
        VoiceOSCoreNG.initialize(
            tier = LearnAppDevToggle.Tier.DEV,
            isDebug = true
        )

        assertTrue(VoiceOSCoreNG.isInitialized())
        assertEquals(LearnAppDevToggle.Tier.DEV, LearnAppDevToggle.getCurrentTier())
        assertTrue(LearnAppDevToggle.isDebug())
    }

    @Test
    fun `isInitialized returns false before initialization`() {
        assertFalse(VoiceOSCoreNG.isInitialized())
    }

    @Test
    fun `isInitialized returns true after initialization`() {
        VoiceOSCoreNG.initialize(
            tier = LearnAppDevToggle.Tier.LITE,
            isDebug = false
        )
        assertTrue(VoiceOSCoreNG.isInitialized())
    }

    // ==================== Mode Tests ====================

    @Test
    fun `isDevMode returns correct value`() {
        VoiceOSCoreNG.initialize(tier = LearnAppDevToggle.Tier.DEV, isDebug = false)
        assertTrue(VoiceOSCoreNG.isDevMode())

        VoiceOSCoreNG.setTier(LearnAppDevToggle.Tier.LITE)
        assertFalse(VoiceOSCoreNG.isDevMode())
    }

    @Test
    fun `isLiteMode returns correct value`() {
        VoiceOSCoreNG.initialize(tier = LearnAppDevToggle.Tier.LITE, isDebug = false)
        assertTrue(VoiceOSCoreNG.isLiteMode())

        VoiceOSCoreNG.setTier(LearnAppDevToggle.Tier.DEV)
        assertFalse(VoiceOSCoreNG.isLiteMode())
    }

    // ==================== Tier Change Tests ====================

    @Test
    fun `setTier changes current tier`() {
        VoiceOSCoreNG.initialize(tier = LearnAppDevToggle.Tier.LITE, isDebug = false)
        VoiceOSCoreNG.setTier(LearnAppDevToggle.Tier.DEV)
        assertEquals(LearnAppDevToggle.Tier.DEV, VoiceOSCoreNG.getCurrentTier())
    }

    @Test
    fun `toggle switches between tiers`() {
        VoiceOSCoreNG.initialize(tier = LearnAppDevToggle.Tier.LITE, isDebug = false)

        VoiceOSCoreNG.toggle()
        assertEquals(LearnAppDevToggle.Tier.DEV, VoiceOSCoreNG.getCurrentTier())

        VoiceOSCoreNG.toggle()
        assertEquals(LearnAppDevToggle.Tier.LITE, VoiceOSCoreNG.getCurrentTier())
    }

    // ==================== Feature Check Tests ====================

    @Test
    fun `isFeatureEnabled returns correct value for LITE tier`() {
        VoiceOSCoreNG.initialize(tier = LearnAppDevToggle.Tier.LITE, isDebug = false)

        assertTrue(VoiceOSCoreNG.isFeatureEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
        assertFalse(VoiceOSCoreNG.isFeatureEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY))
    }

    @Test
    fun `isFeatureEnabled returns correct value for DEV tier`() {
        VoiceOSCoreNG.initialize(tier = LearnAppDevToggle.Tier.DEV, isDebug = false)

        assertTrue(VoiceOSCoreNG.isFeatureEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
        assertTrue(VoiceOSCoreNG.isFeatureEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY))
    }

    // ==================== Version Info Tests ====================

    @Test
    fun `getVersion returns non-empty string`() {
        val version = VoiceOSCoreNG.getVersion()
        assertNotNull(version)
        assertTrue(version.isNotEmpty())
    }

    @Test
    fun `getVersionCode returns positive value`() {
        val versionCode = VoiceOSCoreNG.getVersionCode()
        assertTrue(versionCode > 0)
    }

    // ==================== Reset Tests ====================

    @Test
    fun `reset clears initialization state`() {
        VoiceOSCoreNG.initialize(tier = LearnAppDevToggle.Tier.DEV, isDebug = true)
        assertTrue(VoiceOSCoreNG.isInitialized())

        VoiceOSCoreNG.reset()
        assertFalse(VoiceOSCoreNG.isInitialized())
    }
}
