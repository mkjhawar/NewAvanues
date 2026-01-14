package com.augmentalis.webavanue.ui.screen.security

import android.net.http.SslCertificate
import android.net.http.SslError
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*

/**
 * Certificate utilities for Android WebView
 *
 * Extracts certificate details from Android SslCertificate and SslError objects
 * for display in security dialogs.
 *
 * SECURITY NOTE: This class handles certificate validation UI only.
 * Actual certificate validation is performed by Android WebView.
 *
 * @see SecurityState for state management
 * @see SslErrorDialog for UI presentation
 */
object CertificateUtils {

    /**
     * Extract certificate information from SslCertificate
     *
     * Parses issuer, subject, validity dates, and computes SHA-256 fingerprint.
     *
     * @param certificate Android SslCertificate from WebView
     * @return CertificateInfo with parsed details, or null if parsing fails
     */
    fun extractCertificateInfo(certificate: SslCertificate?): CertificateInfo? {
        if (certificate == null) return null

        return try {
            val issuedBy = certificate.issuedBy
            val issuedTo = certificate.issuedTo

            // Extract issuer (CA) name
            val issuer = buildString {
                issuedBy.cName?.let { append("CN=$it") }
                issuedBy.oName?.let {
                    if (isNotEmpty()) append(", ")
                    append("O=$it")
                }
                issuedBy.uName?.let {
                    if (isNotEmpty()) append(", ")
                    append("OU=$it")
                }
            }.ifEmpty { "Unknown Issuer" }

            // Extract subject (domain) name
            val subject = buildString {
                issuedTo.cName?.let { append("CN=$it") }
                issuedTo.oName?.let {
                    if (isNotEmpty()) append(", ")
                    append("O=$it")
                }
            }.ifEmpty { "Unknown Subject" }

            // Extract validity dates
            val validFrom = certificate.validNotBeforeDate?.time ?: 0L
            val validTo = certificate.validNotAfterDate?.time ?: 0L

            // Compute SHA-256 fingerprint (best effort)
            val fingerprint = computeFingerprint(certificate) ?: "Unavailable"

            CertificateInfo(
                issuer = issuer,
                subject = subject,
                validFrom = validFrom,
                validTo = validTo,
                fingerprint = fingerprint
            )
        } catch (e: Exception) {
            // Log error but don't crash - return null to indicate parsing failure
            println("CertificateUtils: Failed to extract certificate info: ${e.message}")
            null
        }
    }

    /**
     * Convert SslError to SslErrorInfo for UI display
     *
     * Maps Android SslError constants to our SslErrorType enum and extracts
     * certificate details for user review.
     *
     * @param error Android SslError from WebView
     * @return SslErrorInfo with error details and certificate info
     */
    fun convertSslError(error: SslError): SslErrorInfo {
        val errorType = when (error.primaryError) {
            SslError.SSL_EXPIRED -> SslErrorType.EXPIRED
            SslError.SSL_NOTYETVALID -> SslErrorType.NOT_YET_VALID
            SslError.SSL_UNTRUSTED -> SslErrorType.UNTRUSTED
            SslError.SSL_IDMISMATCH -> SslErrorType.MISMATCH
            SslError.SSL_DATE_INVALID -> SslErrorType.DATE_INVALID
            SslError.SSL_INVALID -> SslErrorType.INVALID
            else -> SslErrorType.INVALID
        }

        val certificateInfo = extractCertificateInfo(error.certificate)

        // Build list of all errors detected (SslError can have multiple)
        val additionalErrors = mutableListOf<String>()
        if (error.hasError(SslError.SSL_EXPIRED)) {
            additionalErrors.add("Certificate expired")
        }
        if (error.hasError(SslError.SSL_NOTYETVALID)) {
            additionalErrors.add("Certificate not yet valid")
        }
        if (error.hasError(SslError.SSL_UNTRUSTED)) {
            additionalErrors.add("Untrusted certificate authority")
        }
        if (error.hasError(SslError.SSL_IDMISMATCH)) {
            additionalErrors.add("Hostname mismatch")
        }
        if (error.hasError(SslError.SSL_DATE_INVALID)) {
            additionalErrors.add("Invalid date")
        }

        // Remove the primary error from additional errors (avoid duplication)
        val primaryErrorDescription = errorType.getUserFriendlyDescription()
        additionalErrors.removeAll { it.equals(primaryErrorDescription, ignoreCase = true) }

        return SslErrorInfo(
            errorType = errorType,
            url = error.url ?: "Unknown URL",
            certificateInfo = certificateInfo,
            primaryError = primaryErrorDescription,
            additionalErrors = additionalErrors
        )
    }

    /**
     * Compute SHA-256 fingerprint of certificate
     *
     * Best effort attempt - may return null if X509Certificate is unavailable
     * (Android doesn't always expose it via SslCertificate API).
     *
     * @param certificate Android SslCertificate
     * @return Hex-encoded SHA-256 fingerprint, or null if unavailable
     */
    private fun computeFingerprint(certificate: SslCertificate): String? {
        return try {
            // Try to get X509Certificate via reflection (not always available)
            val x509Field = certificate.javaClass.getDeclaredField("mX509Certificate")
            x509Field.isAccessible = true
            val x509Cert = x509Field.get(certificate) as? X509Certificate

            x509Cert?.let {
                val digest = MessageDigest.getInstance("SHA-256")
                val fingerprint = digest.digest(it.encoded)
                fingerprint.joinToString(":") { byte ->
                    "%02X".format(byte)
                }
            }
        } catch (e: Exception) {
            // Reflection failed or X509Certificate not available
            // This is acceptable - fingerprint is nice-to-have, not critical
            null
        }
    }

    /**
     * Format certificate validity period for display
     *
     * Example: "Valid from Jan 15, 2024 to Jan 15, 2025"
     *
     * @param certificateInfo Certificate info with validity dates
     * @return Formatted validity string
     */
    fun formatValidityPeriod(certificateInfo: CertificateInfo): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val validFrom = dateFormat.format(Date(certificateInfo.validFrom))
        val validTo = dateFormat.format(Date(certificateInfo.validTo))
        return "Valid from $validFrom to $validTo"
    }

    /**
     * Check if certificate is currently valid (date-wise)
     *
     * @param certificateInfo Certificate info with validity dates
     * @return true if current date is within validity period
     */
    fun isCertificateValidNow(certificateInfo: CertificateInfo): Boolean {
        val now = System.currentTimeMillis()
        return now >= certificateInfo.validFrom && now <= certificateInfo.validTo
    }

    /**
     * Check if certificate is expiring soon (within 30 days)
     *
     * @param certificateInfo Certificate info with validity dates
     * @return true if certificate expires within 30 days
     */
    fun isCertificateExpiringSoon(certificateInfo: CertificateInfo): Boolean {
        val now = System.currentTimeMillis()
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        return certificateInfo.validTo - now < thirtyDaysInMillis && certificateInfo.validTo > now
    }
}
