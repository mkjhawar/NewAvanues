package com.augmentalis.voiceoscoreng.features

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeatureGateTest {

    @BeforeTest
    fun setup() {
        FeatureGate.reset()
    }

    @AfterTest
    fun teardown() {
        FeatureGate.reset()
    }

    // ==================== Initialization Tests ====================

    @Test
    fun `initialize sets tier and debug mode`() {
        FeatureGate.initialize(FeatureGate.Tier.DEV, isDebug = true)

        assertEquals(FeatureGate.Tier.DEV, FeatureGate.getCurrentTier())
        assertTrue(FeatureGate.isDebug())
    }

    @Test
    fun `default tier is LITE`() {
        assertEquals(FeatureGate.Tier.LITE, FeatureGate.getCurrentTier())
    }

    @Test
    fun `default debug mode is false`() {
        assertFalse(FeatureGate.isDebug())
    }

    // ==================== Tier Tests ====================

    @Test
    fun `setTier changes current tier`() {
        FeatureGate.setTier(FeatureGate.Tier.DEV)
        assertEquals(FeatureGate.Tier.DEV, FeatureGate.getCurrentTier())

        FeatureGate.setTier(FeatureGate.Tier.LITE)
        assertEquals(FeatureGate.Tier.LITE, FeatureGate.getCurrentTier())
    }

    @Test
    fun `tier change notifies listeners`() {
        var notifiedTier: FeatureGate.Tier? = null
        FeatureGate.addTierChangeListener { tier -> notifiedTier = tier }

        FeatureGate.setTier(FeatureGate.Tier.DEV)

        assertEquals(FeatureGate.Tier.DEV, notifiedTier)
    }

    @Test
    fun `tier change does not notify if same tier`() {
        var callCount = 0
        FeatureGate.addTierChangeListener { callCount++ }

        FeatureGate.setTier(FeatureGate.Tier.LITE) // Same as default

        assertEquals(0, callCount)
    }

    @Test
    fun `remove tier change listener stops notifications`() {
        var callCount = 0
        val listener: (FeatureGate.Tier) -> Unit = { callCount++ }

        FeatureGate.addTierChangeListener(listener)
        FeatureGate.setTier(FeatureGate.Tier.DEV)
        assertEquals(1, callCount)

        FeatureGate.removeTierChangeListener(listener)
        FeatureGate.setTier(FeatureGate.Tier.LITE)
        assertEquals(1, callCount) // No new call
    }

    // ==================== Feature Availability Tests ====================

    @Test
    fun `LITE features are enabled in LITE tier`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)

        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.ELEMENT_SCRAPING))
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.VUID_GENERATION))
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.JIT_PROCESSING))
    }

    @Test
    fun `DEV features are disabled in LITE tier`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)

        assertFalse(FeatureGate.isEnabled(FeatureGate.Feature.EXPLORATION_MODE))
        assertFalse(FeatureGate.isEnabled(FeatureGate.Feature.AI_CLASSIFICATION))
        assertFalse(FeatureGate.isEnabled(FeatureGate.Feature.DEBUG_OVERLAY))
    }

    @Test
    fun `all features are enabled in DEV tier`() {
        FeatureGate.setTier(FeatureGate.Tier.DEV)

        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.ELEMENT_SCRAPING))
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.EXPLORATION_MODE))
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.AI_CLASSIFICATION))
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.DEBUG_OVERLAY))
    }

    // ==================== Feature isAvailableIn Tests ====================

    @Test
    fun `LITE feature isAvailableIn LITE returns true`() {
        assertTrue(FeatureGate.Feature.ELEMENT_SCRAPING.isAvailableIn(FeatureGate.Tier.LITE))
    }

    @Test
    fun `LITE feature isAvailableIn DEV returns true`() {
        assertTrue(FeatureGate.Feature.ELEMENT_SCRAPING.isAvailableIn(FeatureGate.Tier.DEV))
    }

    @Test
    fun `DEV feature isAvailableIn LITE returns false`() {
        assertFalse(FeatureGate.Feature.EXPLORATION_MODE.isAvailableIn(FeatureGate.Tier.LITE))
    }

    @Test
    fun `DEV feature isAvailableIn DEV returns true`() {
        assertTrue(FeatureGate.Feature.EXPLORATION_MODE.isAvailableIn(FeatureGate.Tier.DEV))
    }

    // ==================== Override Tests ====================

    @Test
    fun `override can enable a disabled feature`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)
        assertFalse(FeatureGate.isEnabled(FeatureGate.Feature.DEBUG_OVERLAY))

        FeatureGate.setOverride(FeatureGate.Feature.DEBUG_OVERLAY, true)
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.DEBUG_OVERLAY))
    }

    @Test
    fun `override can disable an enabled feature`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.ELEMENT_SCRAPING))

        FeatureGate.setOverride(FeatureGate.Feature.ELEMENT_SCRAPING, false)
        assertFalse(FeatureGate.isEnabled(FeatureGate.Feature.ELEMENT_SCRAPING))
    }

    @Test
    fun `removeOverride restores default behavior`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)
        FeatureGate.setOverride(FeatureGate.Feature.ELEMENT_SCRAPING, false)
        assertFalse(FeatureGate.isEnabled(FeatureGate.Feature.ELEMENT_SCRAPING))

        FeatureGate.removeOverride(FeatureGate.Feature.ELEMENT_SCRAPING)
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.ELEMENT_SCRAPING))
    }

    @Test
    fun `clearOverrides removes all overrides`() {
        FeatureGate.setOverride(FeatureGate.Feature.ELEMENT_SCRAPING, false)
        FeatureGate.setOverride(FeatureGate.Feature.VUID_GENERATION, false)

        FeatureGate.clearOverrides()

        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.ELEMENT_SCRAPING))
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.VUID_GENERATION))
    }

    @Test
    fun `getOverrides returns current overrides`() {
        FeatureGate.setOverride(FeatureGate.Feature.DEBUG_OVERLAY, true)
        FeatureGate.setOverride(FeatureGate.Feature.AI_CLASSIFICATION, false)

        val overrides = FeatureGate.getOverrides()

        assertEquals(2, overrides.size)
        assertTrue(overrides[FeatureGate.Feature.DEBUG_OVERLAY] == true)
        assertTrue(overrides[FeatureGate.Feature.AI_CLASSIFICATION] == false)
    }

    // ==================== Conditional Execution Tests ====================

    @Test
    fun `ifEnabled executes action when feature enabled`() {
        FeatureGate.setTier(FeatureGate.Tier.DEV)

        val result = FeatureGate.ifEnabled(FeatureGate.Feature.DEBUG_OVERLAY) {
            "executed"
        }

        assertEquals("executed", result)
    }

    @Test
    fun `ifEnabled returns null when feature disabled`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)

        val result = FeatureGate.ifEnabled(FeatureGate.Feature.DEBUG_OVERLAY) {
            "executed"
        }

        assertNull(result)
    }

    @Test
    fun `ifEnabledOrElse executes enabled branch when feature enabled`() {
        FeatureGate.setTier(FeatureGate.Tier.DEV)

        val result = FeatureGate.ifEnabledOrElse(
            FeatureGate.Feature.DEBUG_OVERLAY,
            enabled = { "enabled" },
            disabled = { "disabled" }
        )

        assertEquals("enabled", result)
    }

    @Test
    fun `ifEnabledOrElse executes disabled branch when feature disabled`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)

        val result = FeatureGate.ifEnabledOrElse(
            FeatureGate.Feature.DEBUG_OVERLAY,
            enabled = { "enabled" },
            disabled = { "disabled" }
        )

        assertEquals("disabled", result)
    }

    // ==================== Query Tests ====================

    @Test
    fun `getFeaturesByCategory returns correct features`() {
        val coreFeatures = FeatureGate.getFeaturesByCategory(FeatureGate.Category.CORE)

        assertTrue(coreFeatures.contains(FeatureGate.Feature.ELEMENT_SCRAPING))
        assertTrue(coreFeatures.contains(FeatureGate.Feature.VUID_GENERATION))
        assertFalse(coreFeatures.contains(FeatureGate.Feature.JIT_PROCESSING))
    }

    @Test
    fun `getFeaturesByTier returns correct features`() {
        val liteFeatures = FeatureGate.getFeaturesByTier(FeatureGate.Tier.LITE)
        val devFeatures = FeatureGate.getFeaturesByTier(FeatureGate.Tier.DEV)

        assertTrue(liteFeatures.contains(FeatureGate.Feature.ELEMENT_SCRAPING))
        assertFalse(liteFeatures.contains(FeatureGate.Feature.DEBUG_OVERLAY))

        assertTrue(devFeatures.contains(FeatureGate.Feature.DEBUG_OVERLAY))
        assertFalse(devFeatures.contains(FeatureGate.Feature.ELEMENT_SCRAPING))
    }

    @Test
    fun `getEnabledFeatures returns only enabled features`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)

        val enabled = FeatureGate.getEnabledFeatures()

        assertTrue(enabled.contains(FeatureGate.Feature.ELEMENT_SCRAPING))
        assertFalse(enabled.contains(FeatureGate.Feature.DEBUG_OVERLAY))
    }

    @Test
    fun `getDisabledFeatures returns only disabled features`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)

        val disabled = FeatureGate.getDisabledFeatures()

        assertTrue(disabled.contains(FeatureGate.Feature.DEBUG_OVERLAY))
        assertFalse(disabled.contains(FeatureGate.Feature.ELEMENT_SCRAPING))
    }

    @Test
    fun `isCategoryEnabled returns true if any feature in category enabled`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)

        assertTrue(FeatureGate.isCategoryEnabled(FeatureGate.Category.CORE))
        assertTrue(FeatureGate.isCategoryEnabled(FeatureGate.Category.JIT))
    }

    @Test
    fun `isCategoryEnabled returns false if no features in category enabled`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)

        // DEV_TOOLS only has DEV tier features
        assertFalse(FeatureGate.isCategoryEnabled(FeatureGate.Category.DEV_TOOLS))
    }

    // ==================== Feature Property Tests ====================

    @Test
    fun `all features have descriptions`() {
        FeatureGate.Feature.entries.forEach { feature ->
            assertTrue(feature.description.isNotBlank(), "${feature.name} should have description")
        }
    }

    @Test
    fun `features have correct tier assignments`() {
        // Verify some LITE features
        assertEquals(FeatureGate.Tier.LITE, FeatureGate.Feature.ELEMENT_SCRAPING.tier)
        assertEquals(FeatureGate.Tier.LITE, FeatureGate.Feature.JIT_PROCESSING.tier)

        // Verify some DEV features
        assertEquals(FeatureGate.Tier.DEV, FeatureGate.Feature.EXPLORATION_MODE.tier)
        assertEquals(FeatureGate.Tier.DEV, FeatureGate.Feature.AI_CLASSIFICATION.tier)
    }

    @Test
    fun `features have correct category assignments`() {
        assertEquals(FeatureGate.Category.CORE, FeatureGate.Feature.ELEMENT_SCRAPING.category)
        assertEquals(FeatureGate.Category.JIT, FeatureGate.Feature.JIT_PROCESSING.category)
        assertEquals(FeatureGate.Category.AI, FeatureGate.Feature.AI_CLASSIFICATION.category)
        assertEquals(FeatureGate.Category.DEV_TOOLS, FeatureGate.Feature.DEBUG_OVERLAY.category)
    }

    // ==================== Reset Tests ====================

    @Test
    fun `reset restores default state`() {
        FeatureGate.setTier(FeatureGate.Tier.DEV)
        FeatureGate.setOverride(FeatureGate.Feature.DEBUG_OVERLAY, false)
        FeatureGate.addTierChangeListener { }

        FeatureGate.reset()

        assertEquals(FeatureGate.Tier.LITE, FeatureGate.getCurrentTier())
        assertFalse(FeatureGate.isDebug())
        assertTrue(FeatureGate.getOverrides().isEmpty())
    }

    // ==================== Integration Tests ====================

    @Test
    fun `upgrading from LITE to DEV enables all features`() {
        FeatureGate.setTier(FeatureGate.Tier.LITE)
        val initialEnabled = FeatureGate.getEnabledFeatures().size

        FeatureGate.setTier(FeatureGate.Tier.DEV)
        val afterUpgrade = FeatureGate.getEnabledFeatures().size

        assertTrue(afterUpgrade > initialEnabled)
        assertEquals(FeatureGate.Feature.entries.size, afterUpgrade)
    }

    @Test
    fun `downgrading from DEV to LITE disables DEV features`() {
        FeatureGate.setTier(FeatureGate.Tier.DEV)
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.DEBUG_OVERLAY))

        FeatureGate.setTier(FeatureGate.Tier.LITE)
        assertFalse(FeatureGate.isEnabled(FeatureGate.Feature.DEBUG_OVERLAY))
        assertTrue(FeatureGate.isEnabled(FeatureGate.Feature.ELEMENT_SCRAPING)) // LITE still works
    }
}
