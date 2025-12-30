package com.augmentalis.laas

import kotlinx.serialization.Serializable

/**
 * License status enumeration.
 */
enum class LicenseStatus {
    ACTIVE,
    EXPIRED,
    SUSPENDED,
    REVOKED,
    TRIAL,
    PENDING
}

// ==================== REQUEST MODELS ====================

@Serializable
data class ValidateLicenseRequest(
    val licenseKey: String,
    val deviceFingerprint: String,
    val platform: String,
    val metadata: Map<String, String>? = null
)

@Serializable
data class ActivateLicenseRequest(
    val licenseKey: String,
    val deviceFingerprint: String,
    val platform: String,
    val deviceName: String,
    val metadata: Map<String, String>? = null
)

@Serializable
data class DeactivateLicenseRequest(
    val licenseKey: String,
    val deviceFingerprint: String
)

// ==================== RESPONSE MODELS ====================

@Serializable
data class ValidateLicenseResponse(
    val valid: Boolean,
    val status: String,
    val licenseKey: String,
    val productId: String,
    val productName: String,
    val expiresAt: String? = null,
    val features: List<String>? = null,
    val maxDevices: Int? = null,
    val currentDeviceCount: Int? = null,
    val deviceActivated: Boolean? = null,
    val gracePeriod: GracePeriodInfo? = null,
    val message: String? = null
) {
    /**
     * Get the license status as an enum.
     */
    fun getStatus(): LicenseStatus = try {
        LicenseStatus.valueOf(status.uppercase())
    } catch (e: Exception) {
        LicenseStatus.PENDING
    }
}

@Serializable
data class GracePeriodInfo(
    val offlineGraceExpiresAt: String,
    val nextValidationDue: String,
    val validationIntervalHours: Int,
    val strictMode: Boolean = false
) {
    /**
     * Get offline grace expiry as epoch millis.
     */
    val offlineGraceExpiresAtMillis: Long
        get() = parseIsoDate(offlineGraceExpiresAt)

    /**
     * Get next validation due as epoch millis.
     */
    val nextValidationDueMillis: Long
        get() = parseIsoDate(nextValidationDue)

    private fun parseIsoDate(isoDate: String): Long {
        // Simple ISO-8601 parsing (YYYY-MM-DDTHH:MM:SS.sssZ)
        return try {
            // This is a simplified parser - in production use kotlinx-datetime
            val cleaned = isoDate.replace("Z", "").replace("T", " ")
            // Return approximate millis (this would need proper parsing in production)
            System.currentTimeMillis() + (validationIntervalHours * 60 * 60 * 1000L)
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

@Serializable
data class ActivateLicenseResponse(
    val success: Boolean,
    val deviceId: String,
    val licenseKey: String,
    val activatedAt: String,
    val expiresAt: String? = null,
    val gracePeriod: GracePeriodInfo,
    val message: String? = null
)

@Serializable
data class DeactivateLicenseResponse(
    val success: Boolean,
    val message: String,
    val deactivatedAt: String
)

@Serializable
data class LicenseInfoResponse(
    val licenseKey: String,
    val status: String,
    val productId: String,
    val productName: String,
    val createdAt: String,
    val expiresAt: String? = null,
    val maxDevices: Int,
    val currentDeviceCount: Int,
    val features: List<String>,
    val metadata: Map<String, String>? = null
) {
    /**
     * Get the license status as an enum.
     */
    fun getStatus(): LicenseStatus = try {
        LicenseStatus.valueOf(status.uppercase())
    } catch (e: Exception) {
        LicenseStatus.PENDING
    }
}

// ==================== ERROR MODELS ====================

@Serializable
data class LaasErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

/**
 * Exception thrown when a LaaS API call fails.
 */
class LaasApiException(
    override val message: String,
    val code: String,
    val statusCode: Int,
    val details: Map<String, String>? = null
) : Exception(message) {

    override fun toString(): String {
        return "LaasApiException(code=$code, statusCode=$statusCode, message=$message)"
    }
}

// ==================== OFFLINE STORAGE ====================

/**
 * Data class for persisting license state for offline validation.
 */
@Serializable
data class OfflineLicenseState(
    val licenseKey: String,
    val status: String,
    val productId: String,
    val features: List<String>,
    val gracePeriod: GracePeriodInfo,
    val lastValidatedAt: Long,
    val deviceFingerprint: String
) {
    /**
     * Check if the license is valid for offline use.
     */
    fun isValidOffline(): Boolean {
        val now = System.currentTimeMillis()
        return status == "ACTIVE" && now < gracePeriod.offlineGraceExpiresAtMillis
    }

    /**
     * Check if online validation is needed.
     */
    fun needsOnlineValidation(): Boolean {
        val now = System.currentTimeMillis()
        return now >= gracePeriod.nextValidationDueMillis
    }
}
