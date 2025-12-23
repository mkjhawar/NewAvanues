/**
 * FeatureGateManagerTest.kt - Unit tests for feature gating
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * TDD tests for subscription-based feature gating.
 * Tests all combinations of subscription status and developer override.
 */

package com.augmentalis.voiceoscore.learnapp.subscription

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FeatureGateManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor
    private lateinit var mockSubscriptionProvider: ISubscriptionProvider
    private lateinit var featureGateManager: FeatureGateManager

    @Before
    fun setup() {
        // Mock Android context and SharedPreferences
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        prefsEditor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns prefsEditor
        every { prefsEditor.putBoolean(any(), any()) } returns prefsEditor
        every { prefsEditor.apply() } just Runs

        // Mock subscription provider
        mockSubscriptionProvider = mockk()

        featureGateManager = FeatureGateManager(context, mockSubscriptionProvider)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== JIT Mode Tests ====================

    @Test
    fun `JIT mode is always allowed`() = runBlocking {
        // JIT should always be free, regardless of subscription
        every { prefs.getBoolean("dev_override_enabled", true) } returns false

        val result = featureGateManager.canUseMode(LearningMode.JIT)

        assertIs<FeatureGateResult.Allowed>(result)
    }

    @Test
    fun `JIT mode allowed even with developer override disabled`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false

        val result = featureGateManager.canUseMode(LearningMode.JIT)

        assertIs<FeatureGateResult.Allowed>(result)
    }

    // ==================== Developer Override Tests ====================

    @Test
    fun `developer override is enabled by default`() {
        every { prefs.getBoolean("dev_override_enabled", true) } returns true

        val isEnabled = featureGateManager.isDeveloperOverrideEnabled()

        assertTrue(isEnabled)
    }

    @Test
    fun `developer override allows all modes`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns true

        // All modes should be allowed with developer override
        val jitResult = featureGateManager.canUseMode(LearningMode.JIT)
        val liteResult = featureGateManager.canUseMode(LearningMode.LITE)
        val proResult = featureGateManager.canUseMode(LearningMode.PRO)

        assertIs<FeatureGateResult.Allowed>(jitResult)
        assertIs<FeatureGateResult.Allowed>(liteResult)
        assertIs<FeatureGateResult.Allowed>(proResult)
    }

    @Test
    fun `can disable developer override`() {
        every { prefs.getBoolean("dev_override_enabled", true) } returns true
        featureGateManager.setDeveloperOverride(false)

        verify { prefsEditor.putBoolean("dev_override_enabled", false) }
        verify { prefsEditor.apply() }
    }

    // ==================== LearnAppLite Tests ====================

    @Test
    fun `Lite mode blocked without subscription`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.LITE) } returns false
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.LITE) } returns false

        val result = featureGateManager.canUseMode(LearningMode.LITE)

        assertIs<FeatureGateResult.Blocked>(result)
        assertEquals(SubscriptionTier.LITE, result.tier)
        assertEquals("$2.99/month", result.monthlyPrice)
    }

    @Test
    fun `Lite mode allowed with active subscription`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.LITE) } returns true
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.LITE) } returns false

        val result = featureGateManager.canUseMode(LearningMode.LITE)

        assertIs<FeatureGateResult.Allowed>(result)
    }

    @Test
    fun `Lite mode allowed with permanent license`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.LITE) } returns false
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.LITE) } returns true

        val result = featureGateManager.canUseMode(LearningMode.LITE)

        assertIs<FeatureGateResult.Allowed>(result)
    }

    // ==================== LearnAppPro Tests ====================

    @Test
    fun `Pro mode blocked without subscription`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.PRO) } returns false
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.PRO) } returns false

        val result = featureGateManager.canUseMode(LearningMode.PRO)

        assertIs<FeatureGateResult.Blocked>(result)
        assertEquals(SubscriptionTier.PRO, result.tier)
        assertEquals("$9.99/month", result.monthlyPrice)
    }

    @Test
    fun `Pro mode allowed with active subscription`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.PRO) } returns true
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.PRO) } returns false

        val result = featureGateManager.canUseMode(LearningMode.PRO)

        assertIs<FeatureGateResult.Allowed>(result)
    }

    @Test
    fun `Pro mode allowed with permanent license`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.PRO) } returns false
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.PRO) } returns true

        val result = featureGateManager.canUseMode(LearningMode.PRO)

        assertIs<FeatureGateResult.Allowed>(result)
    }

    // ==================== Subscription Expiry Tests ====================

    @Test
    fun `Lite subscription expiry logged correctly`() = runBlocking {
        // Just verify it doesn't crash - logging is verified via Logcat in instrumentation tests
        featureGateManager.onSubscriptionExpired(SubscriptionTier.LITE)
    }

    @Test
    fun `Pro subscription expiry checked for Lite fallback`() = runBlocking {
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.LITE) } returns true
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.LITE) } returns false

        featureGateManager.onSubscriptionExpired(SubscriptionTier.PRO)

        // Should check if Lite is still available
        coVerify { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.LITE) }
    }

    // ==================== Highest Accessible Mode Tests ====================

    @Test
    fun `highest accessible mode is PRO with Pro subscription`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.PRO) } returns true
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.PRO) } returns false

        val mode = featureGateManager.getHighestAccessibleMode()

        assertEquals(LearningMode.PRO, mode)
    }

    @Test
    fun `highest accessible mode is LITE with Lite subscription only`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.PRO) } returns false
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.PRO) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.LITE) } returns true
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.LITE) } returns false

        val mode = featureGateManager.getHighestAccessibleMode()

        assertEquals(LearningMode.LITE, mode)
    }

    @Test
    fun `highest accessible mode is JIT with no subscription`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(any()) } returns false
        coEvery { mockSubscriptionProvider.hasPermanentLicense(any()) } returns false

        val mode = featureGateManager.getHighestAccessibleMode()

        assertEquals(LearningMode.JIT, mode)
    }

    @Test
    fun `highest accessible mode is PRO with developer override`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns true

        val mode = featureGateManager.getHighestAccessibleMode()

        assertEquals(LearningMode.PRO, mode)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `developer override overrides lack of subscription`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns true
        coEvery { mockSubscriptionProvider.hasActiveSubscription(any()) } returns false
        coEvery { mockSubscriptionProvider.hasPermanentLicense(any()) } returns false

        val liteResult = featureGateManager.canUseMode(LearningMode.LITE)
        val proResult = featureGateManager.canUseMode(LearningMode.PRO)

        assertIs<FeatureGateResult.Allowed>(liteResult)
        assertIs<FeatureGateResult.Allowed>(proResult)
    }

    @Test
    fun `both subscription and permanent license is allowed`() = runBlocking {
        every { prefs.getBoolean("dev_override_enabled", true) } returns false
        coEvery { mockSubscriptionProvider.hasActiveSubscription(SubscriptionTier.PRO) } returns true
        coEvery { mockSubscriptionProvider.hasPermanentLicense(SubscriptionTier.PRO) } returns true

        val result = featureGateManager.canUseMode(LearningMode.PRO)

        assertIs<FeatureGateResult.Allowed>(result)
    }
}
