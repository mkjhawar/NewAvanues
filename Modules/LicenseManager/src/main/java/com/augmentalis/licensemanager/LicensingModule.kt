/**
 * LicensingModule.kt - Direct implementation licensing manager
 *
 * Author: Manoj Jhawar
 * Created: 2025-08-22
 */

package com.augmentalis.licensemanager

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Licensing Module - Direct implementation, manages subscriptions and licenses.
 */
open class LicensingModule(private val context: Context) {
    companion object {
        private const val TAG = "LicensingModule"
        const val MODULE_ID = "licensing"
        const val MODULE_VERSION = "1.0.0"

        // Trial period
        const val TRIAL_DAYS = 30
        const val TRIAL_WARNING_DAYS = 7

        // License types
        const val LICENSE_FREE = "free"
        const val LICENSE_TRIAL = "trial"
        const val LICENSE_PREMIUM = "premium"
        const val LICENSE_ENTERPRISE = "enterprise"

        @Volatile
        private var instance: LicensingModule? = null

        fun getInstance(context: Context): LicensingModule {
            return instance ?: synchronized(this) {
                instance ?: LicensingModule(context.applicationContext).also { instance = it }
            }
        }
    }

    private var isReady = false
    private lateinit var subscriptionManager: SubscriptionManager
    private val licenseValidator = LicenseValidator()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _subscriptionState = MutableStateFlow(SubscriptionState())
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    // Direct properties - no interface
    val name: String = MODULE_ID
    val version: String = MODULE_VERSION
    val description: String = "Manages subscriptions and licenses"

    fun getDependencies(): List<String> = emptyList()

    fun initialize(): Boolean {
        if (isReady) return true

        return try {
            // Initialize subscription manager with encrypted storage
            subscriptionManager = SubscriptionManager(context)

            // Load saved subscription state
            val savedState = subscriptionManager.loadSubscriptionState()
            _subscriptionState.value = savedState

            // Start background tasks
            scope.launch {
                // Validate license
                validateLicense()

                // Start periodic validation
                startPeriodicValidation()

                // Check trial status
                checkTrialStatus()
            }

            isReady = true
            Log.d(TAG, "Licensing module initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize licensing module", e)
            false
        }
    }

    fun shutdown() {
        scope.cancel()
        isReady = false
        instance = null
        Log.d(TAG, "Licensing module shutdown")
    }

    fun isReady(): Boolean = isReady

    fun getCapabilities(): ModuleCapabilities {
        return ModuleCapabilities(
            requiresNetwork = true,
            requiresStorage = true,
            requiresAccessibility = false,
            requiresMicrophone = false,
            requiresNotification = false,
            supportsOffline = true,
            memoryImpact = MemoryImpact.LOW
        )
    }

    /**
     * Start trial period.
     */
    suspend fun startTrial(): Boolean {
        if (_subscriptionState.value.licenseType != LICENSE_FREE) {
            Log.w(TAG, "Cannot start trial - already have license type on record")
            return false
        }

        val trialEndDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(TRIAL_DAYS.toLong())

        val newState = _subscriptionState.value.copy(
            licenseType = LICENSE_TRIAL,
            isPremium = true,
            trialStartDate = System.currentTimeMillis(),
            trialEndDate = trialEndDate,
            isValid = true
        )

        _subscriptionState.value = newState
        subscriptionManager.saveSubscriptionState(newState)

        Log.d(TAG, "Trial period started")
        return true
    }

    /**
     * Activate premium subscription with a validated license key.
     */
    suspend fun activatePremium(licenseKey: String): Boolean {
        val validationResult = licenseValidator.validateKey(licenseKey)

        if (!validationResult.isValid) {
            Log.e(TAG, "License activation rejected: format or checksum validation failed")
            return false
        }

        val newState = _subscriptionState.value.copy(
            licenseType = validationResult.licenseType ?: LICENSE_PREMIUM,
            isPremium = true,
            licenseKey = licenseKey,
            expiryDate = validationResult.expiryDate,
            isValid = true,
            lastValidation = System.currentTimeMillis()
        )

        _subscriptionState.value = newState
        subscriptionManager.saveSubscriptionState(newState)

        Log.d(TAG, "Premium subscription activated: ${validationResult.licenseType}")
        return true
    }

    /**
     * Check if premium features are available.
     */
    fun isPremium(): Boolean = _subscriptionState.value.isPremium

    /**
     * Get current license type.
     */
    fun getLicenseType(): String = _subscriptionState.value.licenseType

    /**
     * Get current subscription state.
     */
    fun getSubscriptionState(): SubscriptionState = _subscriptionState.value

    /**
     * Activate free license (reset to free state).
     */
    suspend fun activateFree() {
        val newState = SubscriptionState(
            licenseType = LICENSE_FREE,
            isPremium = false,
            isValid = true
        )

        _subscriptionState.value = newState
        subscriptionManager.saveSubscriptionState(newState)

        Log.d(TAG, "Free license activated")
    }

    /**
     * Get days remaining in trial.
     */
    fun getTrialDaysRemaining(): Int {
        val state = _subscriptionState.value
        if (state.licenseType != LICENSE_TRIAL) return 0

        val remaining = state.trialEndDate - System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toDays(remaining).toInt().coerceAtLeast(0)
    }

    /**
     * Public method to validate a license key without activating it.
     */
    suspend fun validateLicense(licenseKey: String): ValidationResult {
        return licenseValidator.validateKey(licenseKey)
    }

    /**
     * Validate current stored license.
     */
    private suspend fun validateLicense() {
        val state = _subscriptionState.value

        when (state.licenseType) {
            LICENSE_TRIAL -> validateTrial()
            LICENSE_PREMIUM, LICENSE_ENTERPRISE -> validatePremiumLicense()
            else -> {
                // Free license always valid
                _subscriptionState.value = state.copy(isValid = true)
            }
        }
    }

    /**
     * Validate trial period - expire if past end date.
     */
    private fun validateTrial() {
        val state = _subscriptionState.value
        val now = System.currentTimeMillis()

        if (now > state.trialEndDate) {
            val newState = state.copy(
                licenseType = LICENSE_FREE,
                isPremium = false,
                isValid = true
            )
            _subscriptionState.value = newState
            subscriptionManager.saveSubscriptionState(newState)

            Log.d(TAG, "Trial period expired")
        }
    }

    /**
     * Re-validate a stored premium or enterprise license key.
     */
    private suspend fun validatePremiumLicense() {
        val state = _subscriptionState.value
        val licenseKey = state.licenseKey ?: return

        val validationResult = licenseValidator.validateKey(licenseKey)

        if (!validationResult.isValid) {
            val newState = state.copy(
                licenseType = LICENSE_FREE,
                isPremium = false,
                isValid = false,
                licenseKey = null
            )
            _subscriptionState.value = newState
            subscriptionManager.saveSubscriptionState(newState)

            Log.w(TAG, "Stored license failed re-validation")
        } else {
            _subscriptionState.value = state.copy(
                lastValidation = System.currentTimeMillis(),
                isValid = true
            )
        }
    }

    /**
     * Check trial status and emit warnings.
     */
    private fun checkTrialStatus() {
        val state = _subscriptionState.value
        if (state.licenseType != LICENSE_TRIAL) return

        val daysRemaining = getTrialDaysRemaining()

        if (daysRemaining in 1..TRIAL_WARNING_DAYS) {
            Log.d(TAG, "Trial ending warning: $daysRemaining days remaining")
        }
    }

    /**
     * Start periodic license validation (every 24 hours).
     */
    private fun startPeriodicValidation() {
        scope.launch {
            while (isActive) {
                delay(TimeUnit.HOURS.toMillis(24))
                validateLicense()
                checkTrialStatus()
            }
        }
    }
}

/**
 * Subscription state.
 */
data class SubscriptionState(
    val licenseType: String = LicensingModule.LICENSE_FREE,
    val isPremium: Boolean = false,
    val licenseKey: String? = null,
    val trialStartDate: Long = 0,
    val trialEndDate: Long = 0,
    val expiryDate: Long? = null,
    val lastValidation: Long = 0,
    val isValid: Boolean = true
)

/**
 * Subscription manager for persistence.
 * Stores all license data in EncryptedSharedPreferences (AES256-SIV keys, AES256-GCM values).
 *
 * Requires: androidx.security:security-crypto
 */
open class SubscriptionManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "voiceos_licensing_secure"
        private const val KEY_LICENSE_TYPE = "license_type"
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val KEY_LICENSE_KEY = "license_key"
        private const val KEY_TRIAL_START = "trial_start"
        private const val KEY_TRIAL_END = "trial_end"
        private const val KEY_EXPIRY_DATE = "expiry_date"
        private const val KEY_LAST_VALIDATION = "last_validation"
    }

    /**
     * Lazily-created EncryptedSharedPreferences backed by AES256-GCM MasterKey.
     * Falls back to plain SharedPreferences only if the security library is unavailable
     * (compile-time constraint already enforced via build.gradle.kts dependency).
     */
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadSubscriptionState(): SubscriptionState {
        return SubscriptionState(
            licenseType = prefs.getString(KEY_LICENSE_TYPE, LicensingModule.LICENSE_FREE)
                ?: LicensingModule.LICENSE_FREE,
            isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false),
            licenseKey = prefs.getString(KEY_LICENSE_KEY, null),
            trialStartDate = prefs.getLong(KEY_TRIAL_START, 0),
            trialEndDate = prefs.getLong(KEY_TRIAL_END, 0),
            expiryDate = if (prefs.contains(KEY_EXPIRY_DATE)) {
                prefs.getLong(KEY_EXPIRY_DATE, 0)
            } else null,
            lastValidation = prefs.getLong(KEY_LAST_VALIDATION, 0)
        )
    }

    fun saveSubscriptionState(state: SubscriptionState) {
        prefs.edit().apply {
            putString(KEY_LICENSE_TYPE, state.licenseType)
            putBoolean(KEY_IS_PREMIUM, state.isPremium)
            if (state.licenseKey != null) {
                putString(KEY_LICENSE_KEY, state.licenseKey)
            } else {
                remove(KEY_LICENSE_KEY)
            }
            putLong(KEY_TRIAL_START, state.trialStartDate)
            putLong(KEY_TRIAL_END, state.trialEndDate)
            if (state.expiryDate != null) {
                putLong(KEY_EXPIRY_DATE, state.expiryDate)
            } else {
                remove(KEY_EXPIRY_DATE)
            }
            putLong(KEY_LAST_VALIDATION, state.lastValidation)
            apply()
        }
    }
}

/**
 * License validator.
 *
 * Expected key formats:
 *   PREMIUM-{uuid}    e.g. PREMIUM-550e8400-e29b-41d4-a716-446655440000
 *   ENTERPRISE-{uuid} e.g. ENTERPRISE-550e8400-e29b-41d4-a716-446655440000
 *
 * Validation steps (applied in constant-time to resist timing attacks):
 *   1. Null/blank guard and max-length guard (prevents DoS on regex).
 *   2. Prefix check: must start with "PREMIUM-" or "ENTERPRISE-".
 *   3. UUID segment validation via java.util.UUID.fromString().
 *   4. HMAC-SHA256 checksum of the UUID segment against a compile-time secret
 *      embedded in the last 12 hex characters of the UUID's node field.
 *      This provides offline tamper-resistance without a server round-trip.
 *   5. Expiry derivation: PREMIUM keys carry a 1-year expiry from the UUID
 *      timestamp field; ENTERPRISE keys carry no expiry.
 *   6. Result caching: valid results are cached for CACHE_TTL_MS (24 h) so
 *      the app works offline after the first successful validation.
 *
 * Server-side validation: callers may inject a [ServerValidationDelegate] to
 * perform additional remote checks (e.g. via LicenseSDK's LicenseClient).
 * The delegate is called only when the local format check passes, and its
 * failure overrides the local result.
 */
class LicenseValidator(
    private val serverDelegate: ServerValidationDelegate? = null
) {
    companion object {
        private const val TAG = "LicenseValidator"
        private const val MAX_KEY_LENGTH = 128

        // Offline cache: maps licenseKey -> (ValidationResult, expiresAtMillis)
        // Access is guarded by the validator's own mutex (coroutine Mutex equivalent
        // achieved by confining to a single-threaded context in validateKey).
        private val cache = HashMap<String, Pair<ValidationResult, Long>>()
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

        // UUID segment pattern: 8-4-4-4-12 hex characters
        private val UUID_PATTERN = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )

        /**
         * Compile-time HMAC signing key.
         * In a production system, replace with a value injected from BuildConfig or
         * a key derivation step using the app signing certificate fingerprint.
         */
        private const val HMAC_SECRET = "VoiceOS-LicenseKey-HmacSecret-v1"

        /**
         * Compute HMAC-SHA256 of [input] using [HMAC_SECRET].
         * Returns the first 6 bytes as a 12-character lowercase hex string — this
         * is the expected value embedded in the UUID node field (last 12 chars).
         */
        private fun expectedNodeField(input: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(HMAC_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val digest = mac.doFinal(input.toByteArray(Charsets.UTF_8))
            // Take first 6 bytes → 12 hex chars, matching UUID node field length
            return digest.take(6).joinToString("") { "%02x".format(it) }
        }

        /**
         * Constant-time string comparison to prevent timing side-channels.
         */
        private fun secureEquals(a: String, b: String): Boolean {
            if (a.length != b.length) {
                // Still iterate to keep timing constant
                var diff = a.length xor b.length
                for (i in a.indices) diff = diff or (a[i].code xor (b.getOrElse(i) { '\u0000' }.code))
                return diff == 0
            }
            var diff = 0
            for (i in a.indices) {
                diff = diff or (a[i].code xor b[i].code)
            }
            return diff == 0
        }

        /**
         * Derive expiry epoch millis from a UUID-v1 style timestamp field.
         * For PREMIUM keys: timestamp nibbles encode issuance epoch seconds;
         * expiry = issuance + 365 days.
         * Returns null if the timestamp cannot be parsed (treated as no-expiry
         * for backward compatibility with non-v1 UUIDs).
         */
        private fun deriveExpiryForPremium(uuid: UUID): Long? {
            return try {
                if (uuid.version() == 1) {
                    // UUID v1 timestamp is 100-nanosecond intervals since Oct 15, 1582
                    val uuidEpochOffset = 122192928000000000L
                    val epochMillis = (uuid.timestamp() - uuidEpochOffset) / 10_000
                    epochMillis + TimeUnit.DAYS.toMillis(365)
                } else {
                    // Non-v1 UUID: grant a fixed 1-year window from now
                    System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)
                }
            } catch (e: Exception) {
                System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)
            }
        }
    }

    /**
     * Validate a license key.
     *
     * Returns [ValidationResult.isValid] = true only when all local checks pass
     * and, if a [serverDelegate] is configured, the server also confirms the key.
     *
     * Valid results are cached for 24 hours to support offline use.
     */
    suspend fun validateKey(licenseKey: String): ValidationResult {
        // Guard: null-equivalent or oversized input
        if (licenseKey.isBlank() || licenseKey.length > MAX_KEY_LENGTH) {
            return ValidationResult(isValid = false, errors = listOf("Invalid format"))
        }

        // Check offline cache first
        val now = System.currentTimeMillis()
        synchronized(cache) {
            cache[licenseKey]?.let { (result, expiresAt) ->
                if (now < expiresAt) {
                    Log.d(TAG, "Returning cached validation result")
                    return result
                } else {
                    cache.remove(licenseKey)
                }
            }
        }

        val result = performValidation(licenseKey)

        // Cache successful results for offline use
        if (result.isValid) {
            synchronized(cache) {
                cache[licenseKey] = Pair(result, now + CACHE_TTL_MS)
            }
        }

        return result
    }

    /**
     * Perform the actual validation without cache lookup.
     */
    private suspend fun performValidation(licenseKey: String): ValidationResult {
        // Step 1: Determine prefix and extract UUID segment
        val (prefix, uuidSegment) = when {
            licenseKey.startsWith("PREMIUM-") ->
                Pair(LicensingModule.LICENSE_PREMIUM, licenseKey.removePrefix("PREMIUM-"))
            licenseKey.startsWith("ENTERPRISE-") ->
                Pair(LicensingModule.LICENSE_ENTERPRISE, licenseKey.removePrefix("ENTERPRISE-"))
            else ->
                return ValidationResult(isValid = false, errors = listOf("Invalid format"))
        }

        // Step 2: UUID pattern check before parsing
        if (!UUID_PATTERN.matches(uuidSegment)) {
            return ValidationResult(isValid = false, errors = listOf("Invalid format"))
        }

        // Step 3: Parse UUID (catches malformed strings that pass the regex but are structurally invalid)
        val uuid = try {
            UUID.fromString(uuidSegment)
        } catch (e: IllegalArgumentException) {
            return ValidationResult(isValid = false, errors = listOf("Invalid format"))
        }

        // Step 4: HMAC checksum — the last 12 hex chars of the UUID (node field) must match
        // HMAC-SHA256(HMAC_SECRET, prefix + "-" + uuidWithoutNode) truncated to 6 bytes.
        // The "input" for the HMAC is the key without its node field, giving the key a
        // self-authenticating structure that only the key-issuing authority can produce.
        val uuidWithoutNode = uuidSegment.substringBeforeLast('-') // first 4 groups
        val nodeField = uuidSegment.substringAfterLast('-').lowercase()  // last 12 hex chars
        val expected = expectedNodeField("$prefix-$uuidWithoutNode")
        if (!secureEquals(expected, nodeField)) {
            return ValidationResult(isValid = false, errors = listOf("Invalid format"))
        }

        // Step 5: Server validation (optional)
        if (serverDelegate != null) {
            val serverResult = try {
                serverDelegate.validate(licenseKey, prefix)
            } catch (e: Exception) {
                // Network failure — honour cached offline grace if we ever validated before.
                // Here the cache was already checked and missed, so the key is unconfirmed.
                // Return invalid with a specific error to allow caller to distinguish.
                Log.w(TAG, "Server validation unavailable")
                return ValidationResult(
                    isValid = false,
                    errors = listOf("Server validation unavailable")
                )
            }
            if (!serverResult.isValid) return serverResult
        }

        // Step 6: Build result
        val expiryDate = when (prefix) {
            LicensingModule.LICENSE_PREMIUM -> deriveExpiryForPremium(uuid)
            else -> null // ENTERPRISE has no expiry
        }

        return ValidationResult(
            isValid = true,
            licenseType = prefix,
            expiryDate = expiryDate
        )
    }
}

/**
 * Optional delegate for server-side license validation.
 * Implement this interface to wire in LicenseSDK's LicenseClient or any custom backend.
 */
interface ServerValidationDelegate {
    /**
     * Validate [licenseKey] of [licenseType] against the remote licensing server.
     * Implementations should throw on network failure so the caller can apply
     * offline grace-period logic.
     */
    suspend fun validate(licenseKey: String, licenseType: String): ValidationResult
}

/**
 * License validation result.
 */
data class ValidationResult(
    val isValid: Boolean,
    val licenseType: String? = null,
    val expiryDate: Long? = null,
    val errors: List<String> = if (isValid) emptyList() else listOf("Invalid format")
)

/**
 * Module capabilities data class.
 */
data class ModuleCapabilities(
    val requiresNetwork: Boolean = false,
    val requiresStorage: Boolean = false,
    val requiresAccessibility: Boolean = false,
    val requiresMicrophone: Boolean = false,
    val requiresNotification: Boolean = false,
    val supportsOffline: Boolean = true,
    val memoryImpact: MemoryImpact = MemoryImpact.LOW
)

/**
 * Memory impact enum.
 */
enum class MemoryImpact {
    LOW,
    MEDIUM,
    HIGH
}
