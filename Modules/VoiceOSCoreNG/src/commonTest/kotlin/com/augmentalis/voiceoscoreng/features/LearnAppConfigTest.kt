package com.augmentalis.voiceoscoreng.features

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LearnAppConfigTest {

    @BeforeTest
    fun setup() {
        LearnAppConfig.reset()
        LearnAppDevToggle.reset()
    }

    @AfterTest
    fun teardown() {
        LearnAppConfig.reset()
        LearnAppDevToggle.reset()
    }

    // ==================== Default Configuration Tests ====================

    @Test
    fun `default configuration is LITE`() {
        val config = LearnAppConfig.getConfig()
        assertEquals("LearnApp Lite", config.name)
        assertEquals(LearnAppDevToggle.Tier.LITE, config.tier)
    }

    @Test
    fun `isLite returns true by default`() {
        assertTrue(LearnAppConfig.isLite())
    }

    @Test
    fun `isDev returns false by default`() {
        assertFalse(LearnAppConfig.isDev())
    }

    // ==================== Variant Configuration Tests ====================

    @Test
    fun `LITE_CONFIG has correct values`() {
        val config = LearnAppConfig.LITE_CONFIG

        assertEquals("LearnApp Lite", config.name)
        assertEquals(LearnAppDevToggle.Tier.LITE, config.tier)
        assertEquals(LearnAppConfig.ProcessingMode.IMMEDIATE, config.processingMode)
        assertEquals(50, config.maxElementsPerScan)
        assertEquals(10, config.maxAppsLearned)
        assertFalse(config.enableAI)
        assertFalse(config.enableExploration)
        assertFalse(config.enableFrameworkDetection)
        assertFalse(config.cacheEnabled)
        assertFalse(config.analyticsEnabled)
    }

    @Test
    fun `DEV_CONFIG has correct values`() {
        val config = LearnAppConfig.DEV_CONFIG

        assertEquals("LearnApp Dev", config.name)
        assertEquals(LearnAppDevToggle.Tier.DEV, config.tier)
        assertEquals(LearnAppConfig.ProcessingMode.HYBRID, config.processingMode)
        assertEquals(500, config.maxElementsPerScan)
        assertEquals(-1, config.maxAppsLearned)
        assertTrue(config.enableAI)
        assertTrue(config.enableExploration)
        assertTrue(config.enableFrameworkDetection)
        assertTrue(config.cacheEnabled)
        assertTrue(config.analyticsEnabled)
    }

    // ==================== Variant Switching Tests ====================

    @Test
    fun `setVariant to DEV updates configuration`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        val config = LearnAppConfig.getConfig()
        assertEquals("LearnApp Dev", config.name)
        assertTrue(LearnAppConfig.isDev())
        assertFalse(LearnAppConfig.isLite())
    }

    @Test
    fun `setVariant to LITE updates configuration`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)

        val config = LearnAppConfig.getConfig()
        assertEquals("LearnApp Lite", config.name)
        assertTrue(LearnAppConfig.isLite())
    }

    @Test
    fun `setVariant syncs with LearnAppDevToggle tier`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        assertEquals(LearnAppDevToggle.Tier.DEV, LearnAppDevToggle.getCurrentTier())

        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)
        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.getCurrentTier())
    }

    @Test
    fun `setVariant notifies listeners`() {
        var notifiedConfig: LearnAppConfig.VariantConfig? = null
        LearnAppConfig.addConfigChangeListener { config -> notifiedConfig = config }

        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        assertEquals("LearnApp Dev", notifiedConfig?.name)
    }

    @Test
    fun `setVariant does not notify if same variant`() {
        var callCount = 0
        LearnAppConfig.addConfigChangeListener { callCount++ }

        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE) // Same as default

        assertEquals(0, callCount)
    }

    @Test
    fun `removeConfigChangeListener stops notifications`() {
        var callCount = 0
        val listener: (LearnAppConfig.VariantConfig) -> Unit = { callCount++ }

        LearnAppConfig.addConfigChangeListener(listener)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        assertEquals(1, callCount)

        LearnAppConfig.removeConfigChangeListener(listener)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)
        assertEquals(1, callCount) // No new call
    }

    // ==================== Getter Tests (LITE) ====================

    @Test
    fun `getProcessingMode returns IMMEDIATE for LITE`() {
        assertEquals(LearnAppConfig.ProcessingMode.IMMEDIATE, LearnAppConfig.getProcessingMode())
    }

    @Test
    fun `getMaxElementsPerScan returns 50 for LITE`() {
        assertEquals(50, LearnAppConfig.getMaxElementsPerScan())
    }

    @Test
    fun `getMaxAppsLearned returns 10 for LITE`() {
        assertEquals(10, LearnAppConfig.getMaxAppsLearned())
    }

    @Test
    fun `isAIEnabled returns false for LITE`() {
        assertFalse(LearnAppConfig.isAIEnabled())
    }

    @Test
    fun `isExplorationEnabled returns false for LITE`() {
        assertFalse(LearnAppConfig.isExplorationEnabled())
    }

    @Test
    fun `isFrameworkDetectionEnabled returns false for LITE`() {
        assertFalse(LearnAppConfig.isFrameworkDetectionEnabled())
    }

    @Test
    fun `isCacheEnabled returns false for LITE`() {
        assertFalse(LearnAppConfig.isCacheEnabled())
    }

    @Test
    fun `isAnalyticsEnabled returns false for LITE`() {
        assertFalse(LearnAppConfig.isAnalyticsEnabled())
    }

    // ==================== Getter Tests (DEV) ====================

    @Test
    fun `getProcessingMode returns HYBRID for DEV`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        assertEquals(LearnAppConfig.ProcessingMode.HYBRID, LearnAppConfig.getProcessingMode())
    }

    @Test
    fun `getMaxElementsPerScan returns 500 for DEV`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        assertEquals(500, LearnAppConfig.getMaxElementsPerScan())
    }

    @Test
    fun `getMaxAppsLearned returns -1 (unlimited) for DEV`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        assertEquals(-1, LearnAppConfig.getMaxAppsLearned())
    }

    @Test
    fun `isAIEnabled returns true for DEV`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        assertTrue(LearnAppConfig.isAIEnabled())
    }

    @Test
    fun `isExplorationEnabled returns true for DEV`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        assertTrue(LearnAppConfig.isExplorationEnabled())
    }

    @Test
    fun `isFrameworkDetectionEnabled returns true for DEV`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        assertTrue(LearnAppConfig.isFrameworkDetectionEnabled())
    }

    // ==================== Summary Tests ====================

    @Test
    fun `getSummary includes variant name`() {
        val summary = LearnAppConfig.getSummary()
        assertTrue(summary.contains("LearnApp Lite"))
    }

    @Test
    fun `getSummary includes tier`() {
        val summary = LearnAppConfig.getSummary()
        assertTrue(summary.contains("LITE"))
    }

    @Test
    fun `getSummary shows Unlimited for DEV max apps`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        val summary = LearnAppConfig.getSummary()
        assertTrue(summary.contains("Unlimited"))
    }

    @Test
    fun `getSummary shows Enabled for DEV AI`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        val summary = LearnAppConfig.getSummary()
        assertTrue(summary.contains("AI: Enabled"))
    }

    @Test
    fun `getSummary shows Disabled for LITE AI`() {
        val summary = LearnAppConfig.getSummary()
        assertTrue(summary.contains("AI: Disabled"))
    }

    // ==================== Reset Tests ====================

    @Test
    fun `reset restores LITE configuration`() {
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)
        LearnAppConfig.reset()

        assertTrue(LearnAppConfig.isLite())
        assertEquals("LearnApp Lite", LearnAppConfig.getConfig().name)
    }

    // ==================== ProcessingMode Tests ====================

    @Test
    fun `ProcessingMode enum has correct values`() {
        val modes = LearnAppConfig.ProcessingMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(LearnAppConfig.ProcessingMode.IMMEDIATE))
        assertTrue(modes.contains(LearnAppConfig.ProcessingMode.BATCH))
        assertTrue(modes.contains(LearnAppConfig.ProcessingMode.HYBRID))
    }
}
