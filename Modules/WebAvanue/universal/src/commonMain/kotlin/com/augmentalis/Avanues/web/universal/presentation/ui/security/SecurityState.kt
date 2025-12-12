package com.augmentalis.Avanues.web.universal.presentation.ui.security

/**
 * Security state for browser address bar indicator
 *
 * Represents the security status of the current webpage:
 * - Secure: Valid HTTPS with trusted certificate
 * - Insecure: HTTP (cleartext) connection
 * - Warning: HTTPS with issues (mixed content, weak cipher)
 * - Error: Invalid certificate or SSL error
 * - Loading: Security status not yet determined
 *
 * @see SecurityIndicator for UI representation
 * @see CertificateUtils for certificate validation
 */
sealed class SecurityState {
    /**
     * Valid HTTPS connection with trusted certificate
     * - Green lock icon
     * - Certificate details available
     */
    data class Secure(
        val certificateInfo: CertificateInfo
    ) : SecurityState()

    /**
     * HTTP (cleartext) connection - no encryption
     * - Gray/neutral icon
     * - Warning message available
     */
    data class Insecure(
        val url: String,
        val reason: String = "Not secure (HTTP)"
    ) : SecurityState()

    /**
     * HTTPS with warnings (mixed content, weak algorithms)
     * - Yellow warning icon
     * - Issues list available
     */
    data class Warning(
        val certificateInfo: CertificateInfo,
        val issues: List<SecurityIssue>
    ) : SecurityState()

    /**
     * Invalid certificate or SSL error
     * - Red error icon
     * - Error details available for user decision
     */
    data class Error(
        val error: SslErrorInfo,
        val canProceed: Boolean = false
    ) : SecurityState()

    /**
     * Security status loading/unknown
     * - Loading indicator
     * - Transitional state
     */
    object Loading : SecurityState()
}

/**
 * Certificate information extracted from SSL certificate
 *
 * @property issuer Certificate issuer (CA name)
 * @property subject Certificate subject (domain)
 * @property validFrom Certificate valid from date (epoch millis)
 * @property validTo Certificate valid until date (epoch millis)
 * @property fingerprint SHA-256 fingerprint (hex string)
 */
data class CertificateInfo(
    val issuer: String,
    val subject: String,
    val validFrom: Long,
    val validTo: Long,
    val fingerprint: String
)

/**
 * Security issue detected (non-blocking)
 *
 * Examples:
 * - Mixed content (HTTP resources on HTTPS page)
 * - Weak cipher suite
 * - Certificate expiring soon
 */
data class SecurityIssue(
    val type: SecurityIssueType,
    val description: String,
    val severity: Severity
) {
    enum class SecurityIssueType {
        MIXED_CONTENT,
        WEAK_CIPHER,
        CERTIFICATE_EXPIRING_SOON,
        DEPRECATED_TLS_VERSION,
        OTHER
    }

    enum class Severity {
        LOW,
        MEDIUM,
        HIGH
    }
}

/**
 * SSL error information for user-facing dialogs
 *
 * @property errorType Type of SSL error (from Android SslError)
 * @property url URL where error occurred
 * @property certificateInfo Certificate details (if available)
 * @property primaryError Primary error causing failure
 * @property additionalErrors Additional errors detected
 */
data class SslErrorInfo(
    val errorType: SslErrorType,
    val url: String,
    val certificateInfo: CertificateInfo?,
    val primaryError: String,
    val additionalErrors: List<String> = emptyList()
)

/**
 * SSL error types mapped from Android WebView SslError
 *
 * Corresponds to android.net.http.SslError constants:
 * - EXPIRED: Certificate expired
 * - NOT_YET_VALID: Certificate not yet valid
 * - UNTRUSTED: Certificate from untrusted CA
 * - MISMATCH: Hostname mismatch
 * - DATE_INVALID: Certificate has invalid date
 * - INVALID: Generic invalid certificate
 */
enum class SslErrorType {
    EXPIRED,
    NOT_YET_VALID,
    UNTRUSTED,
    MISMATCH,
    DATE_INVALID,
    INVALID;

    /**
     * Get user-friendly description of error
     */
    fun getUserFriendlyDescription(): String = when (this) {
        EXPIRED -> "Certificate has expired"
        NOT_YET_VALID -> "Certificate is not yet valid"
        UNTRUSTED -> "Certificate is from an untrusted authority"
        MISMATCH -> "Certificate hostname does not match"
        DATE_INVALID -> "Certificate has invalid date"
        INVALID -> "Certificate is invalid"
    }

    /**
     * Get recommended user action
     */
    fun getRecommendedAction(): String = when (this) {
        EXPIRED, NOT_YET_VALID, UNTRUSTED, INVALID ->
            "Go back to safety. This connection is not private."
        MISMATCH ->
            "Go back to safety. You may be connecting to an imposter site."
        DATE_INVALID ->
            "Go back to safety. Your device's date/time may be incorrect."
    }
}

/**
 * Permission type for website permission requests
 *
 * Corresponds to Android WebView PermissionRequest.RESOURCE_* constants
 */
enum class PermissionType(val resourceString: String) {
    CAMERA("android.webkit.resource.VIDEO_CAPTURE"),
    MICROPHONE("android.webkit.resource.AUDIO_CAPTURE"),
    LOCATION("android.webkit.resource.GEOLOCATION"),
    PROTECTED_MEDIA("android.webkit.resource.PROTECTED_MEDIA_ID");

    companion object {
        fun fromResourceString(resource: String): PermissionType? = when (resource) {
            "android.webkit.resource.VIDEO_CAPTURE" -> CAMERA
            "android.webkit.resource.AUDIO_CAPTURE" -> MICROPHONE
            "android.webkit.resource.GEOLOCATION" -> LOCATION
            "android.webkit.resource.PROTECTED_MEDIA_ID" -> PROTECTED_MEDIA
            else -> null
        }
    }

    fun getUserFriendlyName(): String = when (this) {
        CAMERA -> "Camera"
        MICROPHONE -> "Microphone"
        LOCATION -> "Location"
        PROTECTED_MEDIA -> "Protected Media"
    }
}

/**
 * Permission request from website
 *
 * @property domain Website domain requesting permission
 * @property permissions List of requested permissions
 * @property requestId Unique request ID for tracking
 */
data class PermissionRequest(
    val domain: String,
    val permissions: List<PermissionType>,
    val requestId: String
)

/**
 * Permission grant status
 */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    PENDING
}

// ========== HTTP Authentication ==========

/**
 * HTTP authentication request from server
 *
 * Represents HTTP Basic or Digest authentication challenge from web server.
 * User must provide username and password to access protected resources.
 *
 * @property host Server hostname
 * @property realm Authentication realm (security scope description)
 * @property scheme Authentication scheme ("Basic" or "Digest")
 */
data class HttpAuthRequest(
    val host: String,
    val realm: String,
    val scheme: String = "Basic"
)

/**
 * HTTP authentication credentials
 *
 * @property username Username for authentication
 * @property password Password for authentication
 * @property remember Whether to remember credentials in secure storage
 */
data class HttpAuthCredentials(
    val username: String,
    val password: String,
    val remember: Boolean = false
)

