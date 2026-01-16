package com.augmentalis.voiceoscoreng.features

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearnAppDevToggleTest {

    @BeforeTest
    fun setup() {
        LearnAppDevToggle.reset()
    }

    @AfterTest
    fun teardown() {
        LearnAppDevToggle.reset()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `initialize sets tier and debug mode`() {
        LearnAppDevToggle.initialize(LearnAppDevToggle.Tier.DEV, isDebug = true)

        assertEquals(LearnAppDevToggle.Tier.DEV, LearnAppDevToggle.getCurrentTier())
        assertTrue(LearnAppDevToggle.isDebug())
    }

    @Test
    fun `default tier is LITE`() {
        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.getCurrentTier())
    }

    @Test
    fun `default debug mode is false`() {
        assertFalse(LearnAppDevToggle.isDebug())
    }

    // ==================== Tier Tests ====================

    @Test
    fun `setTier changes current tier`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        assertEquals(LearnAppDevToggle.Tier.DEV, LearnAppDevToggle.getCurrentTier())

        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.getCurrentTier())
    }

    @Test
    fun `tier change notifies listeners`() {
        var notifiedTier: LearnAppDevToggle.Tier? = null
        LearnAppDevToggle.addTierChangeListener { tier -> notifiedTier = tier }

        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        assertEquals(LearnAppDevToggle.Tier.DEV, notifiedTier)
    }

    @Test
    fun `tier change does not notify if same tier`() {
        var callCount = 0
        LearnAppDevToggle.addTierChangeListener { callCount++ }

        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE) // Same as default

        assertEquals(0, callCount)
    }

    @Test
    fun `remove tier change listener stops notifications`() {
        var callCount = 0
        val listener: (LearnAppDevToggle.Tier) -> Unit = { callCount++ }

        LearnAppDevToggle.addTierChangeListener(listener)
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        assertEquals(1, callCount)

        LearnAppDevToggle.removeTierChangeListener(listener)
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        assertEquals(1, callCount) // No new call
    }

    // ==================== Mode Helpers Tests ====================

    @Test
    fun `isDevMode returns true when tier is DEV`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        assertTrue(LearnAppDevToggle.isDevMode())
        assertFalse(LearnAppDevToggle.isLiteMode())
    }

    @Test
    fun `isLiteMode returns true when tier is LITE`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        assertTrue(LearnAppDevToggle.isLiteMode())
        assertFalse(LearnAppDevToggle.isDevMode())
    }

    @Test
    fun `toggle switches between LITE and DEV`() {
        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.getCurrentTier())

        LearnAppDevToggle.toggle()
        assertEquals(LearnAppDevToggle.Tier.DEV, LearnAppDevToggle.getCurrentTier())

        LearnAppDevToggle.toggle()
        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.getCurrentTier())
    }

    // ==================== Feature Availability Tests ====================

    @Test
    fun `LITE features are enabled in LITE tier`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.VUID_GENERATION))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.JIT_PROCESSING))
    }

    @Test
    fun `DEV features are disabled in LITE tier`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.EXPLORATION_MODE))
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_CLASSIFICATION))
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY))
    }

    @Test
    fun `all features are enabled in DEV tier`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.EXPLORATION_MODE))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_CLASSIFICATION))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY))
    }

    // ==================== Feature isAvailableIn Tests ====================

    @Test
    fun `LITE feature isAvailableIn LITE returns true`() {
        assertTrue(LearnAppDevToggle.Feature.ELEMENT_SCRAPING.isAvailableIn(LearnAppDevToggle.Tier.LITE))
    }

    @Test
    fun `LITE feature isAvailableIn DEV returns true`() {
        assertTrue(LearnAppDevToggle.Feature.ELEMENT_SCRAPING.isAvailableIn(LearnAppDevToggle.Tier.DEV))
    }

    @Test
    fun `DEV feature isAvailableIn LITE returns false`() {
        assertFalse(LearnAppDevToggle.Feature.EXPLORATION_MODE.isAvailableIn(LearnAppDevToggle.Tier.LITE))
    }

    @Test
    fun `DEV feature isAvailableIn DEV returns true`() {
        assertTrue(LearnAppDevToggle.Feature.EXPLORATION_MODE.isAvailableIn(LearnAppDevToggle.Tier.DEV))
    }

    // ==================== Override Tests ====================

    @Test
    fun `override can enable a disabled feature`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY))

        LearnAppDevToggle.setOverride(LearnAppDevToggle.Feature.DEBUG_OVERLAY, true)
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY))
    }

    @Test
    fun `override can disable an enabled feature`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))

        LearnAppDevToggle.setOverride(LearnAppDevToggle.Feature.ELEMENT_SCRAPING, false)
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
    }

    @Test
    fun `removeOverride restores default behavior`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        LearnAppDevToggle.setOverride(LearnAppDevToggle.Feature.ELEMENT_SCRAPING, false)
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))

        LearnAppDevToggle.removeOverride(LearnAppDevToggle.Feature.ELEMENT_SCRAPING)
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
    }

    @Test
    fun `clearOverrides removes all overrides`() {
        LearnAppDevToggle.setOverride(LearnAppDevToggle.Feature.ELEMENT_SCRAPING, false)
        LearnAppDevToggle.setOverride(LearnAppDevToggle.Feature.VUID_GENERATION, false)

        LearnAppDevToggle.clearOverrides()

        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.VUID_GENERATION))
    }

    @Test
    fun `getOverrides returns current overrides`() {
        LearnAppDevToggle.setOverride(LearnAppDevToggle.Feature.DEBUG_OVERLAY, true)
        LearnAppDevToggle.setOverride(LearnAppDevToggle.Feature.AI_CLASSIFICATION, false)

        val overrides = LearnAppDevToggle.getOverrides()

        assertEquals(2, overrides.size)
        assertTrue(overrides[LearnAppDevToggle.Feature.DEBUG_OVERLAY] == true)
        assertTrue(overrides[LearnAppDevToggle.Feature.AI_CLASSIFICATION] == false)
    }

    // ==================== Conditional Execution Tests ====================

    @Test
    fun `ifEnabled executes action when feature enabled`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        val result = LearnAppDevToggle.ifEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY) {
            "executed"
        }

        assertEquals("executed", result)
    }

    @Test
    fun `ifEnabled returns null when feature disabled`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        val result = LearnAppDevToggle.ifEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY) {
            "executed"
        }

        assertNull(result)
    }

    @Test
    fun `ifEnabledOrElse executes enabled branch when feature enabled`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        val result = LearnAppDevToggle.ifEnabledOrElse(
            LearnAppDevToggle.Feature.DEBUG_OVERLAY,
            enabled = { "enabled" },
            disabled = { "disabled" }
        )

        assertEquals("enabled", result)
    }

    @Test
    fun `ifEnabledOrElse executes disabled branch when feature disabled`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        val result = LearnAppDevToggle.ifEnabledOrElse(
            LearnAppDevToggle.Feature.DEBUG_OVERLAY,
            enabled = { "enabled" },
            disabled = { "disabled" }
        )

        assertEquals("disabled", result)
    }

    // ==================== Query Tests ====================

    @Test
    fun `getFeaturesByCategory returns correct features`() {
        val coreFeatures = LearnAppDevToggle.getFeaturesByCategory(LearnAppDevToggle.Category.CORE)

        assertTrue(coreFeatures.contains(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
        assertTrue(coreFeatures.contains(LearnAppDevToggle.Feature.VUID_GENERATION))
        assertFalse(coreFeatures.contains(LearnAppDevToggle.Feature.JIT_PROCESSING))
    }

    @Test
    fun `getFeaturesByTier returns correct features`() {
        val liteFeatures = LearnAppDevToggle.getFeaturesByTier(LearnAppDevToggle.Tier.LITE)
        val devFeatures = LearnAppDevToggle.getFeaturesByTier(LearnAppDevToggle.Tier.DEV)

        assertTrue(liteFeatures.contains(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
        assertFalse(liteFeatures.contains(LearnAppDevToggle.Feature.DEBUG_OVERLAY))

        assertTrue(devFeatures.contains(LearnAppDevToggle.Feature.DEBUG_OVERLAY))
        assertFalse(devFeatures.contains(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
    }

    @Test
    fun `getEnabledFeatures returns only enabled features`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        val enabled = LearnAppDevToggle.getEnabledFeatures()

        assertTrue(enabled.contains(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
        assertFalse(enabled.contains(LearnAppDevToggle.Feature.DEBUG_OVERLAY))
    }

    @Test
    fun `getDisabledFeatures returns only disabled features`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        val disabled = LearnAppDevToggle.getDisabledFeatures()

        assertTrue(disabled.contains(LearnAppDevToggle.Feature.DEBUG_OVERLAY))
        assertFalse(disabled.contains(LearnAppDevToggle.Feature.ELEMENT_SCRAPING))
    }

    @Test
    fun `isCategoryEnabled returns true if any feature in category enabled`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        assertTrue(LearnAppDevToggle.isCategoryEnabled(LearnAppDevToggle.Category.CORE))
        assertTrue(LearnAppDevToggle.isCategoryEnabled(LearnAppDevToggle.Category.JIT))
    }

    @Test
    fun `isCategoryEnabled returns false if no features in category enabled`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // DEV_TOOLS only has DEV tier features
        assertFalse(LearnAppDevToggle.isCategoryEnabled(LearnAppDevToggle.Category.DEV_TOOLS))
    }

    // ==================== Feature Property Tests ====================

    @Test
    fun `all features have descriptions`() {
        LearnAppDevToggle.Feature.entries.forEach { feature ->
            assertTrue(feature.description.isNotBlank(), "${feature.name} should have description")
        }
    }

    @Test
    fun `features have correct tier assignments`() {
        // Verify some LITE features
        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.Feature.ELEMENT_SCRAPING.tier)
        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.Feature.JIT_PROCESSING.tier)

        // Verify some DEV features
        assertEquals(LearnAppDevToggle.Tier.DEV, LearnAppDevToggle.Feature.EXPLORATION_MODE.tier)
        assertEquals(LearnAppDevToggle.Tier.DEV, LearnAppDevToggle.Feature.AI_CLASSIFICATION.tier)
    }

    @Test
    fun `features have correct category assignments`() {
        assertEquals(LearnAppDevToggle.Category.CORE, LearnAppDevToggle.Feature.ELEMENT_SCRAPING.category)
        assertEquals(LearnAppDevToggle.Category.JIT, LearnAppDevToggle.Feature.JIT_PROCESSING.category)
        assertEquals(LearnAppDevToggle.Category.AI, LearnAppDevToggle.Feature.AI_CLASSIFICATION.category)
        assertEquals(LearnAppDevToggle.Category.DEV_TOOLS, LearnAppDevToggle.Feature.DEBUG_OVERLAY.category)
    }

    // ==================== Reset Tests ====================

    @Test
    fun `reset restores default state`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        LearnAppDevToggle.setOverride(LearnAppDevToggle.Feature.DEBUG_OVERLAY, false)
        LearnAppDevToggle.addTierChangeListener { }

        LearnAppDevToggle.reset()

        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.getCurrentTier())
        assertFalse(LearnAppDevToggle.isDebug())
        assertTrue(LearnAppDevToggle.getOverrides().isEmpty())
    }

    // ==================== Integration Tests ====================

    @Test
    fun `upgrading from LITE to DEV enables all features`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        val initialEnabled = LearnAppDevToggle.getEnabledFeatures().size

        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        val afterUpgrade = LearnAppDevToggle.getEnabledFeatures().size

        assertTrue(afterUpgrade > initialEnabled)
        assertEquals(LearnAppDevToggle.Feature.entries.size, afterUpgrade)
    }

    @Test
    fun `downgrading from DEV to LITE disables DEV features`() {
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY))

        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING)) // LITE still works
    }
}
