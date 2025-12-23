/**
 * FeatureGateManager.kt - Controls access to learning modes based on subscription
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-22
 *
 * Implements feature gating with developer override for testing.
 * Follows SOLID principles:
 * - Single Responsibility: Only manages feature access
 * - Open/Closed: Extensible for new tiers
 * - Liskov Substitution: Can swap SubscriptionProvider implementations
 * - Interface Segregation: Minimal interface
 * - Dependency Inversion: Depends on abstractions (ISubscriptionProvider)
 */

package com.augmentalis.voiceoscore.learnapp.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Feature Gate Manager
 *
 * Controls access to learning modes with subscription enforcement.
 * Includes developer override for testing (default: all features unlocked).
 *
 * @property context Application context
 * @property subscriptionProvider Subscription status provider
 */
class FeatureGateManager(
    private val context: Context,
    private val subscriptionProvider: ISubscriptionProvider = DeveloperSubscriptionProvider(context)
) {
    companion object {
        private const val TAG = "FeatureGateManager"
        private const val PREFS_NAME = "voiceos_feature_gates"
        private const val KEY_DEV_OVERRIDE = "dev_override_enabled"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if mode is accessible
     *
     * @param mode Learning mode to check
     * @return Access result (allowed or blocked with reason)
     */
    suspend fun canUseMode(mode: LearningMode): FeatureGateResult {
        // Developer override - always allow all features
        if (isDeveloperOverrideEnabled()) {
            Log.d(TAG, "Developer override enabled - allowing $mode")
            return FeatureGateResult.Allowed
        }

        return when (mode) {
            LearningMode.JIT -> {
                // JIT is always free
                FeatureGateResult.Allowed
            }

            LearningMode.LITE -> {
                val hasAccess = subscriptionProvider.hasActiveSubscription(SubscriptionTier.LITE)
                    || subscriptionProvider.hasPermanentLicense(SubscriptionTier.LITE)

                if (hasAccess) {
                    FeatureGateResult.Allowed
                } else {
                    FeatureGateResult.Blocked(
                        tier = SubscriptionTier.LITE,
                        reason = "LearnAppLite requires active subscription or permanent license",
                        monthlyPrice = "$2.99/month",
                        annualPrice = "$20/year"
                    )
                }
            }

            LearningMode.PRO -> {
                val hasAccess = subscriptionProvider.hasActiveSubscription(SubscriptionTier.PRO)
                    || subscriptionProvider.hasPermanentLicense(SubscriptionTier.PRO)

                if (hasAccess) {
                    FeatureGateResult.Allowed
                } else {
                    FeatureGateResult.Blocked(
                        tier = SubscriptionTier.PRO,
                        reason = "LearnAppPro requires active subscription or permanent license",
                        monthlyPrice = "$9.99/month",
                        annualPrice = "$80/year"
                    )
                }
            }
        }
    }

    /**
     * Check if developer override is enabled
     *
     * Default: TRUE (all features unlocked for testing)
     */
    fun isDeveloperOverrideEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEV_OVERRIDE, true)  // Default TRUE
    }

    /**
     * Set developer override
     *
     * @param enabled true to unlock all features, false to enforce subscriptions
     */
    fun setDeveloperOverride(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEV_OVERRIDE, enabled).apply()
        Log.i(TAG, "Developer override ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    /**
     * Handle subscription expiry
     *
     * Called when subscription expires. Disables features immediately.
     *
     * @param tier Subscription tier that expired
     */
    suspend fun onSubscriptionExpired(tier: SubscriptionTier) {
        Log.w(TAG, "Subscription expired: $tier")

        when (tier) {
            SubscriptionTier.LITE -> {
                // Disable Lite features, fall back to JIT
                Log.i(TAG, "LearnAppLite expired - falling back to JIT mode")
                // VoiceOSService will be notified via broadcast
            }

            SubscriptionTier.PRO -> {
                // Check if user still has Lite access
                val hasLite = subscriptionProvider.hasActiveSubscription(SubscriptionTier.LITE)
                    || subscriptionProvider.hasPermanentLicense(SubscriptionTier.LITE)

                if (hasLite) {
                    Log.i(TAG, "LearnAppPro expired - falling back to LearnAppLite")
                } else {
                    Log.i(TAG, "LearnAppPro expired - falling back to JIT mode")
                }
            }
        }
    }

    /**
     * Get current highest accessible mode
     *
     * @return Highest mode user can access
     */
    suspend fun getHighestAccessibleMode(): LearningMode {
        return when {
            canUseMode(LearningMode.PRO) is FeatureGateResult.Allowed -> LearningMode.PRO
            canUseMode(LearningMode.LITE) is FeatureGateResult.Allowed -> LearningMode.LITE
            else -> LearningMode.JIT
        }
    }
}

/**
 * Learning Mode
 *
 * Three-tier progressive learning system.
 */
enum class LearningMode {
    /** JIT (Quick) - Free, passive learning */
    JIT,

    /** LearnAppLite - Mid-tier, menu/drawer deep scan */
    LITE,

    /** LearnAppPro - Premium, full exploration + export */
    PRO
}

/**
 * Feature Gate Result
 *
 * Result of feature gate check.
 */
sealed class FeatureGateResult {
    /** Access granted */
    object Allowed : FeatureGateResult()

    /** Access blocked - subscription required */
    data class Blocked(
        val tier: SubscriptionTier,
        val reason: String,
        val monthlyPrice: String,
        val annualPrice: String
    ) : FeatureGateResult()
}

/**
 * Subscription Tier
 */
enum class SubscriptionTier {
    LITE,
    PRO
}

/**
 * Subscription Provider Interface
 *
 * Abstraction for subscription checking.
 * Allows swapping between real billing and developer override.
 */
interface ISubscriptionProvider {
    suspend fun hasActiveSubscription(tier: SubscriptionTier): Boolean
    suspend fun hasPermanentLicense(tier: SubscriptionTier): Boolean
}

/**
 * Developer Subscription Provider
 *
 * Default implementation for testing.
 * Respects developer override setting.
 */
class DeveloperSubscriptionProvider(
    private val context: Context
) : ISubscriptionProvider {
    companion object {
        private const val PREFS_NAME = "voiceos_dev_subscriptions"
        private const val KEY_LITE_ACTIVE = "dev_lite_active"
        private const val KEY_PRO_ACTIVE = "dev_pro_active"
        private const val KEY_LITE_PERMANENT = "dev_lite_permanent"
        private const val KEY_PRO_PERMANENT = "dev_pro_permanent"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun hasActiveSubscription(tier: SubscriptionTier): Boolean {
        return when (tier) {
            SubscriptionTier.LITE -> prefs.getBoolean(KEY_LITE_ACTIVE, false)
            SubscriptionTier.PRO -> prefs.getBoolean(KEY_PRO_ACTIVE, false)
        }
    }

    override suspend fun hasPermanentLicense(tier: SubscriptionTier): Boolean {
        return when (tier) {
            SubscriptionTier.LITE -> prefs.getBoolean(KEY_LITE_PERMANENT, false)
            SubscriptionTier.PRO -> prefs.getBoolean(KEY_PRO_PERMANENT, false)
        }
    }

    /**
     * Set subscription status (for testing)
     */
    fun setSubscriptionStatus(tier: SubscriptionTier, active: Boolean) {
        val key = when (tier) {
            SubscriptionTier.LITE -> KEY_LITE_ACTIVE
            SubscriptionTier.PRO -> KEY_PRO_ACTIVE
        }
        prefs.edit().putBoolean(key, active).apply()
    }

    /**
     * Set permanent license (for testing)
     */
    fun setPermanentLicense(tier: SubscriptionTier, granted: Boolean) {
        val key = when (tier) {
            SubscriptionTier.LITE -> KEY_LITE_PERMANENT
            SubscriptionTier.PRO -> KEY_PRO_PERMANENT
        }
        prefs.edit().putBoolean(key, granted).apply()
    }
}
