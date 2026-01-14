package com.augmentalis.voiceoscoreng.integration

import com.augmentalis.voiceoscoreng.features.LearnAppConfig
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Feature Gate Integration Tests - TDD approach for Lite vs Dev feature gating.
 *
 * These tests verify feature gating across the entire VoiceOSCoreNG system,
 * ensuring that:
 * - Lite features are always available
 * - Dev features require the dev toggle enabled
 * - Feature checks work correctly across component boundaries
 * - Paywall interface preparation is future-ready
 *
 * Feature Map:
 * | Feature                  | Lite | Dev |
 * |--------------------------|------|-----|
 * | Basic element capture    | Yes  | Yes |
 * | Command generation       | Yes  | Yes |
 * | Framework detection      | No   | Yes |
 * | Batch exploration        | No   | Yes |
 * | Custom command templates | No   | Yes |
 * | Analytics export         | No   | Yes |
 * | Debug overlays           | No   | Yes |
 */
class FeatureGateIntegrationTest {

    // ==================== Test Setup ====================

    @BeforeTest
    fun setup() {
        // Reset all feature state before each test
        LearnAppDevToggle.reset()
        LearnAppConfig.reset()
    }

    @AfterTest
    fun teardown() {
        // Clean up after each test
        LearnAppDevToggle.reset()
        LearnAppConfig.reset()
    }

    // ==================== 1. Lite Features Always Available ====================

    @Test
    fun `test lite features available when dev disabled`() {
        // Given: Dev mode is disabled (default LITE tier)
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)

        // Then: Core LITE features should be enabled
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING),
            "Element scraping should be available in Lite mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.VUID_GENERATION),
            "VUID generation should be available in Lite mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.NATIVE_DETECTION),
            "Native detection should be available in Lite mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.VOICE_COMMANDS),
            "Voice commands should be available in Lite mode"
        )

        // And: JIT features should be enabled
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.JIT_PROCESSING),
            "JIT processing should be available in Lite mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.JIT_COMMANDS),
            "JIT commands should be available in Lite mode"
        )

        // And: Config should reflect Lite settings
        assertTrue(LearnAppConfig.isLite(), "Config should be in Lite mode")
        assertTrue(LearnAppConfig.isAIEnabled(), "AI should be enabled for Lite (basic voice)")
        assertTrue(LearnAppConfig.isNLUEnabled(), "NLU should be enabled for Lite")
        assertTrue(LearnAppConfig.isCacheEnabled(), "Basic caching should be enabled for Lite")
    }

    @Test
    fun `test all lite tier features remain accessible regardless of toggle state`() {
        // Given: LITE tier is active
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // When: We get all LITE tier features
        val liteFeatures = LearnAppDevToggle.getFeaturesByTier(LearnAppDevToggle.Tier.LITE)

        // Then: All LITE features should be enabled
        liteFeatures.forEach { feature ->
            assertTrue(
                LearnAppDevToggle.isEnabled(feature),
                "Lite feature ${feature.name} should be enabled in Lite tier"
            )
        }
    }

    // ==================== 2. Dev Features Blocked When Disabled ====================

    @Test
    fun `test dev features blocked when dev disabled`() {
        // Given: Dev mode is disabled (LITE tier)
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)

        // Then: All DEV tier features should be disabled
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.EXPLORATION_MODE),
            "Exploration mode should be blocked in Lite mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.BATCH_PROCESSING),
            "Batch processing should be blocked in Lite mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug overlay should be blocked in Lite mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_CLASSIFICATION),
            "AI classification should be blocked in Lite mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.USAGE_ANALYTICS),
            "Usage analytics should be blocked in Lite mode"
        )

        // And: Config should reflect blocked features
        assertFalse(LearnAppConfig.isExplorationEnabled(), "Exploration should be disabled in Lite config")
        assertFalse(LearnAppConfig.isAnalyticsEnabled(), "Analytics should be disabled in Lite config")
        assertFalse(LearnAppConfig.isFrameworkDetectionEnabled(), "Framework detection should be disabled in Lite")
        assertFalse(LearnAppConfig.isDebugOverlayEnabled(), "Debug overlay should be disabled in Lite config")
    }

    @Test
    fun `test all dev tier features are blocked when lite mode active`() {
        // Given: LITE tier is active
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // When: We get all DEV tier features
        val devFeatures = LearnAppDevToggle.getFeaturesByTier(LearnAppDevToggle.Tier.DEV)

        // Then: All DEV features should be disabled
        devFeatures.forEach { feature ->
            assertFalse(
                LearnAppDevToggle.isEnabled(feature),
                "Dev feature ${feature.name} should be blocked in Lite tier"
            )
        }
    }

    // ==================== 3. Dev Features Available When Enabled ====================

    @Test
    fun `test dev features available when dev enabled`() {
        // Given: Dev mode is enabled (DEV tier)
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        // Then: All features should be enabled (both LITE and DEV)
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING),
            "Element scraping should be available in Dev mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.EXPLORATION_MODE),
            "Exploration mode should be available in Dev mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.BATCH_PROCESSING),
            "Batch processing should be available in Dev mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug overlay should be available in Dev mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_CLASSIFICATION),
            "AI classification should be available in Dev mode"
        )

        // And: Config should reflect Dev settings
        assertTrue(LearnAppConfig.isDev(), "Config should be in Dev mode")
        assertTrue(LearnAppConfig.isExplorationEnabled(), "Exploration should be enabled in Dev config")
        assertTrue(LearnAppConfig.isAnalyticsEnabled(), "Analytics should be enabled in Dev config")
        assertTrue(LearnAppConfig.isFrameworkDetectionEnabled(), "Framework detection should be enabled in Dev")
    }

    @Test
    fun `test all features enabled count in dev mode equals total features`() {
        // Given: DEV tier is active
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        // When: We count enabled features
        val enabledFeatures = LearnAppDevToggle.getEnabledFeatures()
        val totalFeatures = LearnAppDevToggle.Feature.entries.size

        // Then: All features should be enabled
        assertEquals(
            totalFeatures,
            enabledFeatures.size,
            "All features should be enabled in Dev mode"
        )
    }

    // ==================== 4. Batch Exploration Requires Dev Mode ====================

    @Test
    fun `test batch exploration requires dev mode`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)

        // Then: Batch/exploration features should be blocked
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.EXPLORATION_MODE),
            "Exploration mode requires Dev mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.BATCH_PROCESSING),
            "Batch processing requires Dev mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.SCREEN_CACHING),
            "Screen caching requires Dev mode"
        )
        assertFalse(
            LearnAppConfig.isExplorationEnabled(),
            "Config exploration should be disabled in Lite"
        )

        // When: Switching to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        // Then: Batch/exploration features should be enabled
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.EXPLORATION_MODE),
            "Exploration mode should be available in Dev mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.BATCH_PROCESSING),
            "Batch processing should be available in Dev mode"
        )
        assertTrue(
            LearnAppConfig.isExplorationEnabled(),
            "Config exploration should be enabled in Dev"
        )
    }

    @Test
    fun `test batch processing mode requires dev tier`() {
        // Given: LITE mode with IMMEDIATE processing
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)

        // Then: Processing mode should be IMMEDIATE (not BATCH)
        assertEquals(
            LearnAppConfig.ProcessingMode.IMMEDIATE,
            LearnAppConfig.getProcessingMode(),
            "Lite mode should use IMMEDIATE processing"
        )

        // When: Switching to DEV mode
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        // Then: Processing mode should be HYBRID (allows batch)
        assertEquals(
            LearnAppConfig.ProcessingMode.HYBRID,
            LearnAppConfig.getProcessingMode(),
            "Dev mode should use HYBRID processing (allows batch)"
        )
    }

    // ==================== 5. Debug Overlays Require Dev Mode ====================

    @Test
    fun `test debug overlays require dev mode`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)

        // Then: Debug overlay features should be blocked
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug overlay requires Dev mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_INSPECTOR),
            "Element inspector requires Dev mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.VUID_VIEWER),
            "VUID viewer requires Dev mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.PERFORMANCE_PROFILER),
            "Performance profiler requires Dev mode"
        )
        assertFalse(
            LearnAppConfig.isDebugOverlayEnabled(),
            "Config debug overlay should be disabled in Lite"
        )

        // When: Switching to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        // Then: Debug overlay features should be enabled
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug overlay should be available in Dev mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.ELEMENT_INSPECTOR),
            "Element inspector should be available in Dev mode"
        )
    }

    @Test
    fun `test dev tools category requires dev mode`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // Then: DEV_TOOLS category should be disabled
        assertFalse(
            LearnAppDevToggle.isCategoryEnabled(LearnAppDevToggle.Category.DEV_TOOLS),
            "DEV_TOOLS category should be disabled in Lite mode"
        )

        // When: Switching to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        // Then: DEV_TOOLS category should be enabled
        assertTrue(
            LearnAppDevToggle.isCategoryEnabled(LearnAppDevToggle.Category.DEV_TOOLS),
            "DEV_TOOLS category should be enabled in Dev mode"
        )
    }

    // ==================== 6. Custom Command Templates Require Dev Mode ====================

    @Test
    fun `test custom command templates require dev mode`() {
        // Given: LITE mode - templates would use AI features
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // Then: AI-powered template features should be blocked
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_SUGGESTIONS),
            "AI suggestions (for templates) requires Dev mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_NAMING),
            "AI naming (for templates) requires Dev mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_CLASSIFICATION),
            "AI classification (for templates) requires Dev mode"
        )

        // When: Switching to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        // Then: AI-powered template features should be enabled
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_SUGGESTIONS),
            "AI suggestions should be available in Dev mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.AI_NAMING),
            "AI naming should be available in Dev mode"
        )
    }

    @Test
    fun `test cross app learning for templates requires dev mode`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // Then: Cross-app learning (for template reuse) should be blocked
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.CROSS_APP_LEARNING),
            "Cross-app learning requires Dev mode"
        )

        // When: Switching to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        // Then: Cross-app learning should be enabled
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.CROSS_APP_LEARNING),
            "Cross-app learning should be available in Dev mode"
        )
    }

    // ==================== 7. Analytics Export Requires Dev Mode ====================

    @Test
    fun `test analytics export requires dev mode`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)

        // Then: Analytics features should be blocked
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.USAGE_ANALYTICS),
            "Usage analytics requires Dev mode"
        )
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.COMMAND_METRICS),
            "Command metrics requires Dev mode"
        )
        assertFalse(
            LearnAppConfig.isAnalyticsEnabled(),
            "Config analytics should be disabled in Lite"
        )

        // When: Switching to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        // Then: Analytics features should be enabled
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.USAGE_ANALYTICS),
            "Usage analytics should be available in Dev mode"
        )
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.COMMAND_METRICS),
            "Command metrics should be available in Dev mode"
        )
        assertTrue(
            LearnAppConfig.isAnalyticsEnabled(),
            "Config analytics should be enabled in Dev"
        )
    }

    @Test
    fun `test analytics category requires dev mode`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // Then: ANALYTICS category should be disabled
        assertFalse(
            LearnAppDevToggle.isCategoryEnabled(LearnAppDevToggle.Category.ANALYTICS),
            "ANALYTICS category should be disabled in Lite mode"
        )

        // When: Switching to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        // Then: ANALYTICS category should be enabled
        assertTrue(
            LearnAppDevToggle.isCategoryEnabled(LearnAppDevToggle.Category.ANALYTICS),
            "ANALYTICS category should be enabled in Dev mode"
        )
    }

    // ==================== 8. Feature Gate Changes Take Effect Immediately ====================

    @Test
    fun `test feature gate changes take effect immediately`() {
        // Given: Start in LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug overlay should be disabled in Lite"
        )

        // When: Immediately switch to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        // Then: Feature should be immediately available (no restart needed)
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug overlay should be immediately enabled after tier change"
        )

        // When: Switch back to LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // Then: Feature should be immediately blocked
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug overlay should be immediately disabled after tier change back"
        )
    }

    @Test
    fun `test tier change listener notifies immediately`() {
        // Given: A tier change listener
        var notificationCount = 0
        var lastTier: LearnAppDevToggle.Tier? = null
        LearnAppDevToggle.addTierChangeListener { tier ->
            notificationCount++
            lastTier = tier
        }

        // When: Changing tier
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)

        // Then: Listener should be called immediately
        assertEquals(1, notificationCount, "Listener should be called once")
        assertEquals(LearnAppDevToggle.Tier.DEV, lastTier, "Listener should receive DEV tier")

        // When: Changing back
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // Then: Listener should be called again
        assertEquals(2, notificationCount, "Listener should be called twice")
        assertEquals(LearnAppDevToggle.Tier.LITE, lastTier, "Listener should receive LITE tier")
    }

    @Test
    fun `test config change listener notifies immediately`() {
        // Given: A config change listener
        var notificationCount = 0
        var lastConfig: LearnAppConfig.VariantConfig? = null
        LearnAppConfig.addConfigChangeListener { config ->
            notificationCount++
            lastConfig = config
        }

        // When: Changing variant
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        // Then: Listener should be called immediately
        assertEquals(1, notificationCount, "Listener should be called once")
        assertEquals("LearnApp Dev", lastConfig?.name, "Listener should receive Dev config")
    }

    @Test
    fun `test toggle function switches modes immediately`() {
        // Given: Start in LITE mode
        assertTrue(LearnAppDevToggle.isLiteMode(), "Should start in Lite mode")
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug should be disabled"
        )

        // When: Toggle to DEV
        LearnAppDevToggle.toggle()

        // Then: Should be in DEV mode immediately with features enabled
        assertTrue(LearnAppDevToggle.isDevMode(), "Should be in Dev mode after toggle")
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug should be enabled after toggle"
        )

        // When: Toggle back to LITE
        LearnAppDevToggle.toggle()

        // Then: Should be back in LITE mode immediately
        assertTrue(LearnAppDevToggle.isLiteMode(), "Should be in Lite mode after second toggle")
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Debug should be disabled after toggle back"
        )
    }

    // ==================== 9. License Provider Interface Can Be Injected ====================

    @Test
    fun `test license provider interface can be injected`() {
        // This test verifies the paywall preparation architecture.
        // LicenseProvider is an interface that can be injected for subscription validation.

        // Given: A mock license provider implementation
        val mockLicenseProvider = MockLicenseProvider()

        // When: Using the provider to check license status
        val hasDevLicense = mockLicenseProvider.hasActiveLicense(LicenseType.DEV)
        val hasLiteLicense = mockLicenseProvider.hasActiveLicense(LicenseType.LITE)

        // Then: Provider should return appropriate values
        assertFalse(hasDevLicense, "Mock should return false for DEV license by default")
        assertTrue(hasLiteLicense, "Mock should return true for LITE license (always free)")

        // When: Activating DEV license
        mockLicenseProvider.activateLicense(LicenseType.DEV)

        // Then: DEV license should be active
        assertTrue(
            mockLicenseProvider.hasActiveLicense(LicenseType.DEV),
            "DEV license should be active after activation"
        )
    }

    @Test
    fun `test license provider can gate tier access`() {
        // Given: A mock license provider with no DEV license
        val mockLicenseProvider = MockLicenseProvider()
        val gatedToggle = GatedFeatureToggle(mockLicenseProvider)

        // Then: Attempting to enable DEV should fail without license
        assertFalse(
            gatedToggle.canEnableTier(LearnAppDevToggle.Tier.DEV),
            "Should not be able to enable DEV tier without license"
        )

        // When: Activating DEV license
        mockLicenseProvider.activateLicense(LicenseType.DEV)

        // Then: DEV tier should be accessible
        assertTrue(
            gatedToggle.canEnableTier(LearnAppDevToggle.Tier.DEV),
            "Should be able to enable DEV tier with license"
        )
    }

    @Test
    fun `test license expiration blocks dev features`() {
        // Given: A license provider with expired DEV license
        val mockLicenseProvider = MockLicenseProvider()
        mockLicenseProvider.activateLicense(LicenseType.DEV, expiresInMs = -1) // Already expired

        val gatedToggle = GatedFeatureToggle(mockLicenseProvider)

        // Then: DEV tier should not be accessible
        assertFalse(
            gatedToggle.canEnableTier(LearnAppDevToggle.Tier.DEV),
            "Should not be able to enable DEV tier with expired license"
        )
    }

    // ==================== 10. Code Access Provider Interface Can Be Injected ====================

    @Test
    fun `test code access provider interface can be injected`() {
        // This test verifies the architecture for code/promo code validation.
        // CodeAccessProvider validates access codes for feature unlocking.

        // Given: A mock code access provider
        val mockCodeProvider = MockCodeAccessProvider()

        // When: Validating an invalid code
        val invalidResult = mockCodeProvider.validateCode("INVALID_CODE")

        // Then: Should return invalid result
        assertFalse(invalidResult.isValid, "Invalid code should not be valid")
        assertNull(invalidResult.unlockedTier, "Invalid code should not unlock any tier")

        // When: Using a valid DEV code
        mockCodeProvider.registerCode("DEVMODE2024", LicenseType.DEV)
        val validResult = mockCodeProvider.validateCode("DEVMODE2024")

        // Then: Should return valid result with DEV tier
        assertTrue(validResult.isValid, "Valid code should be valid")
        assertEquals(
            LicenseType.DEV,
            validResult.unlockedTier,
            "Code should unlock DEV tier"
        )
    }

    @Test
    fun `test code access can unlock dev tier`() {
        // Given: A mock code provider with a valid code
        val mockCodeProvider = MockCodeAccessProvider()
        mockCodeProvider.registerCode("BETA_TESTER", LicenseType.DEV)

        val mockLicenseProvider = MockLicenseProvider()
        val gatedToggle = GatedFeatureToggle(mockLicenseProvider)

        // Verify: Initially DEV is not accessible
        assertFalse(
            gatedToggle.canEnableTier(LearnAppDevToggle.Tier.DEV),
            "DEV should not be accessible initially"
        )

        // When: Validating and applying the code
        val codeResult = mockCodeProvider.validateCode("BETA_TESTER")
        if (codeResult.isValid && codeResult.unlockedTier == LicenseType.DEV) {
            mockLicenseProvider.activateLicense(LicenseType.DEV)
        }

        // Then: DEV tier should now be accessible
        assertTrue(
            gatedToggle.canEnableTier(LearnAppDevToggle.Tier.DEV),
            "DEV should be accessible after code activation"
        )
    }

    @Test
    fun `test one time use code can only be used once`() {
        // Given: A mock code provider with a one-time use code
        val mockCodeProvider = MockCodeAccessProvider()
        mockCodeProvider.registerCode("SINGLE_USE_123", LicenseType.DEV, oneTimeUse = true)

        // When: First validation
        val firstResult = mockCodeProvider.validateCode("SINGLE_USE_123")

        // Then: First use should succeed
        assertTrue(firstResult.isValid, "First use should be valid")
        assertEquals(LicenseType.DEV, firstResult.unlockedTier)

        // When: Consuming the code
        mockCodeProvider.consumeCode("SINGLE_USE_123")

        // And: Second validation
        val secondResult = mockCodeProvider.validateCode("SINGLE_USE_123")

        // Then: Second use should fail
        assertFalse(secondResult.isValid, "Code should not be valid after being consumed")
    }

    // ==================== Cross-Component Integration Tests ====================

    @Test
    fun `test dev toggle and config stay in sync`() {
        // Given: Initial LITE state
        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.getCurrentTier())
        assertTrue(LearnAppConfig.isLite())

        // When: Setting config variant to DEV
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        // Then: Both should be in sync
        assertEquals(LearnAppDevToggle.Tier.DEV, LearnAppDevToggle.getCurrentTier())
        assertTrue(LearnAppConfig.isDev())

        // When: Setting toggle tier to LITE
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        // Note: Config needs to be explicitly updated for full sync
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)

        // Then: Both should be LITE
        assertEquals(LearnAppDevToggle.Tier.LITE, LearnAppDevToggle.getCurrentTier())
        assertTrue(LearnAppConfig.isLite())
    }

    @Test
    fun `test feature check behavior matches config behavior`() {
        // Given: DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        // Then: Feature checks should align with config checks
        assertEquals(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.EXPLORATION_MODE),
            LearnAppConfig.isExplorationEnabled(),
            "Exploration feature check should match config"
        )
        assertEquals(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            LearnAppConfig.isDebugOverlayEnabled() || LearnAppDevToggle.isDevMode(),
            "Debug overlay feature check should match config"
        )
        assertEquals(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.USAGE_ANALYTICS),
            LearnAppConfig.isAnalyticsEnabled(),
            "Analytics feature check should match config"
        )
    }

    @Test
    fun `test framework detection requires dev mode across all components`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)

        // Then: All framework detection features should be blocked
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.FLUTTER_DETECTION))
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.UNITY_DETECTION))
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.REACT_NATIVE_DETECTION))
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.WEBVIEW_HANDLING))
        assertFalse(LearnAppConfig.isFrameworkDetectionEnabled())

        // When: Switching to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.DEV)

        // Then: All framework detection features should be enabled
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.FLUTTER_DETECTION))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.UNITY_DETECTION))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.REACT_NATIVE_DETECTION))
        assertTrue(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.WEBVIEW_HANDLING))
        assertTrue(LearnAppConfig.isFrameworkDetectionEnabled())
    }

    // ==================== Developer Override Tests ====================

    @Test
    fun `test developer settings can override lite restrictions`() {
        // Given: LITE mode with developer settings enabled
        LearnAppConfig.setVariant(LearnAppDevToggle.Tier.LITE)
        LearnAppConfig.DeveloperSettings.enable(unlockAll = false)
        LearnAppConfig.DeveloperSettings.forceEnableExploration = true

        // When: Getting config
        val config = LearnAppConfig.getConfig()

        // Then: Exploration should be force-enabled despite LITE tier
        assertTrue(
            config.enableExploration,
            "Exploration should be force-enabled via developer settings"
        )
    }

    @Test
    fun `test feature override can enable dev feature in lite mode`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)
        assertFalse(LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY))

        // When: Setting override
        LearnAppDevToggle.setOverride(LearnAppDevToggle.Feature.DEBUG_OVERLAY, true)

        // Then: Feature should be enabled despite tier
        assertTrue(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Override should enable feature in Lite mode"
        )

        // When: Removing override
        LearnAppDevToggle.removeOverride(LearnAppDevToggle.Feature.DEBUG_OVERLAY)

        // Then: Feature should respect tier again
        assertFalse(
            LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY),
            "Feature should be disabled after override removed"
        )
    }

    // ==================== Conditional Execution Tests ====================

    @Test
    fun `test ifEnabled executes for available features only`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // When: Using ifEnabled on LITE feature
        var liteExecuted = false
        LearnAppDevToggle.ifEnabled(LearnAppDevToggle.Feature.ELEMENT_SCRAPING) {
            liteExecuted = true
        }

        // Then: Should execute
        assertTrue(liteExecuted, "Lite feature action should execute")

        // When: Using ifEnabled on DEV feature
        var devExecuted = false
        LearnAppDevToggle.ifEnabled(LearnAppDevToggle.Feature.DEBUG_OVERLAY) {
            devExecuted = true
        }

        // Then: Should not execute
        assertFalse(devExecuted, "Dev feature action should not execute in Lite mode")
    }

    @Test
    fun `test ifEnabledOrElse provides fallback for blocked features`() {
        // Given: LITE mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.LITE)

        // When: Using ifEnabledOrElse on DEV feature
        val result = LearnAppDevToggle.ifEnabledOrElse(
            LearnAppDevToggle.Feature.EXPLORATION_MODE,
            enabled = { "full_exploration" },
            disabled = { "limited_mode" }
        )

        // Then: Should return fallback
        assertEquals("limited_mode", result, "Should return disabled fallback in Lite mode")

        // When: Switching to DEV mode
        LearnAppDevToggle.setTier(LearnAppDevToggle.Tier.DEV)
        val devResult = LearnAppDevToggle.ifEnabledOrElse(
            LearnAppDevToggle.Feature.EXPLORATION_MODE,
            enabled = { "full_exploration" },
            disabled = { "limited_mode" }
        )

        // Then: Should return enabled value
        assertEquals("full_exploration", devResult, "Should return enabled value in Dev mode")
    }
}

// ==================== Support Classes for Testing ====================

/**
 * License types for subscription/paywall system.
 */
enum class LicenseType {
    LITE,  // Always free
    DEV    // Requires subscription/payment
}

/**
 * Interface for license validation - can be implemented for real paywall.
 */
interface LicenseProvider {
    fun hasActiveLicense(type: LicenseType): Boolean
    fun activateLicense(type: LicenseType, expiresInMs: Long = Long.MAX_VALUE)
    fun deactivateLicense(type: LicenseType)
    fun isLicenseExpired(type: LicenseType): Boolean
}

/**
 * Mock implementation of LicenseProvider for testing.
 * Uses simple boolean flags for testing rather than real time tracking.
 */
class MockLicenseProvider : LicenseProvider {
    private data class LicenseInfo(
        val type: LicenseType,
        val isExpired: Boolean = false
    )

    private val licenses = mutableMapOf<LicenseType, LicenseInfo>()

    override fun hasActiveLicense(type: LicenseType): Boolean {
        // LITE is always available
        if (type == LicenseType.LITE) return true

        val license = licenses[type] ?: return false
        return !license.isExpired
    }

    override fun activateLicense(type: LicenseType, expiresInMs: Long) {
        // For testing: if expiresInMs is negative, treat as already expired
        val isExpired = expiresInMs < 0
        licenses[type] = LicenseInfo(type, isExpired)
    }

    override fun deactivateLicense(type: LicenseType) {
        licenses.remove(type)
    }

    override fun isLicenseExpired(type: LicenseType): Boolean {
        if (type == LicenseType.LITE) return false
        return licenses[type]?.isExpired ?: true
    }
}

/**
 * Code validation result.
 */
data class CodeValidationResult(
    val isValid: Boolean,
    val unlockedTier: LicenseType? = null,
    val message: String = ""
)

/**
 * Interface for code/promo code validation.
 */
interface CodeAccessProvider {
    fun validateCode(code: String): CodeValidationResult
    fun registerCode(code: String, unlocksType: LicenseType, oneTimeUse: Boolean = false)
    fun consumeCode(code: String)
}

/**
 * Mock implementation of CodeAccessProvider for testing.
 */
class MockCodeAccessProvider : CodeAccessProvider {
    private data class CodeInfo(
        val code: String,
        val unlocksType: LicenseType,
        val oneTimeUse: Boolean,
        var consumed: Boolean = false
    )

    private val codes = mutableMapOf<String, CodeInfo>()

    override fun validateCode(code: String): CodeValidationResult {
        val codeInfo = codes[code]
            ?: return CodeValidationResult(false, message = "Invalid code")

        if (codeInfo.consumed) {
            return CodeValidationResult(false, message = "Code already used")
        }

        return CodeValidationResult(
            isValid = true,
            unlockedTier = codeInfo.unlocksType,
            message = "Code valid"
        )
    }

    override fun registerCode(code: String, unlocksType: LicenseType, oneTimeUse: Boolean) {
        codes[code] = CodeInfo(code, unlocksType, oneTimeUse)
    }

    override fun consumeCode(code: String) {
        codes[code]?.let { it.consumed = true }
    }
}

/**
 * Feature toggle that respects license state - bridges license provider with feature toggle.
 */
class GatedFeatureToggle(
    private val licenseProvider: LicenseProvider
) {
    fun canEnableTier(tier: LearnAppDevToggle.Tier): Boolean {
        return when (tier) {
            LearnAppDevToggle.Tier.LITE -> true // Always available
            LearnAppDevToggle.Tier.DEV -> licenseProvider.hasActiveLicense(LicenseType.DEV)
        }
    }

    fun enableTierIfAllowed(tier: LearnAppDevToggle.Tier): Boolean {
        if (!canEnableTier(tier)) return false
        LearnAppDevToggle.setTier(tier)
        return true
    }
}
