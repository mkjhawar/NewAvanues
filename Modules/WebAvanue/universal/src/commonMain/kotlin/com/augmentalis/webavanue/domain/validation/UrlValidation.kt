package com.augmentalis.webavanue.domain.validation

import com.augmentalis.webavanue.domain.errors.TabError

/**
 * URL validation utility for tab creation and navigation.
 *
 * Validates URLs before creating tabs or navigating, providing
 * specific error types with user-friendly messages.
 *
 * Validation rules:
 * - Must not be empty/blank (unless creating blank tab)
 * - Must be valid URL format or search query
 * - Special internal URLs (avanues://) are allowed
 * - about: URLs are allowed
 * - File URLs require validation
 *
 * @see TabError.InvalidUrl for error types
 */
object UrlValidation {

    /**
     * URL validation result
     */
    sealed class UrlValidationResult {
        /**
         * URL is valid and can be used
         */
        data class Valid(val normalizedUrl: String) : UrlValidationResult()

        /**
         * URL is invalid with specific error
         */
        data class Invalid(val error: TabError.InvalidUrl) : UrlValidationResult()
    }

    /**
     * Validate a URL for tab creation or navigation.
     *
     * Returns normalized URL if valid, or specific error if invalid.
     *
     * @param url URL to validate
     * @param allowBlank Whether blank URLs are allowed (for new blank tabs)
     * @return UrlValidationResult with normalized URL or error
     */
    fun validate(url: String, allowBlank: Boolean = false): UrlValidationResult {
        val trimmed = url.trim()

        // Empty/blank check
        if (trimmed.isEmpty() || trimmed.isBlank()) {
            return if (allowBlank) {
                UrlValidationResult.Valid("about:blank")
            } else {
                UrlValidationResult.Invalid(
                    TabError.InvalidUrl(
                        url = url,
                        reason = "URL cannot be empty"
                    )
                )
            }
        }

        // Special internal URLs (avanues://...)
        if (trimmed.startsWith("avanues://", ignoreCase = true)) {
            return if (isValidInternalUrl(trimmed)) {
                UrlValidationResult.Valid(trimmed)
            } else {
                UrlValidationResult.Invalid(
                    TabError.InvalidUrl(
                        url = url,
                        reason = "Invalid internal URL format"
                    )
                )
            }
        }

        // about: URLs
        if (trimmed.startsWith("about:", ignoreCase = true)) {
            return UrlValidationResult.Valid(trimmed)
        }

        // file: URLs (require special validation)
        if (trimmed.startsWith("file://", ignoreCase = true)) {
            return validateFileUrl(trimmed)
        }

        // data: URLs (inline data)
        if (trimmed.startsWith("data:", ignoreCase = true)) {
            return UrlValidationResult.Valid(trimmed)
        }

        // HTTP(S) URLs
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return validateHttpUrl(trimmed)
        }

        // No scheme - could be domain or search query
        return validateDomainOrSearch(trimmed)
    }

    /**
     * Validate internal avanues:// URLs
     */
    private fun isValidInternalUrl(url: String): Boolean {
        // Valid patterns:
        // - avanues://newtab?mode=...
        // - avanues://settings
        // - avanues://history
        // - avanues://favorites
        val validPatterns = listOf(
            "avanues://newtab",
            "avanues://settings",
            "avanues://history",
            "avanues://favorites",
            "avanues://downloads"
        )

        return validPatterns.any { url.startsWith(it, ignoreCase = true) }
    }

    /**
     * Validate file:// URLs
     */
    private fun validateFileUrl(url: String): UrlValidationResult {
        // File URLs must have path after file://
        if (url.length <= 7 || url.substring(7).isBlank()) {
            return UrlValidationResult.Invalid(
                TabError.InvalidUrl(
                    url = url,
                    reason = "File URL must specify a path"
                )
            )
        }

        // Additional security: warn about file access
        // In production, you might want to check permissions here
        return UrlValidationResult.Valid(url)
    }

    /**
     * Validate HTTP(S) URLs
     */
    private fun validateHttpUrl(url: String): UrlValidationResult {
        // Extract domain part
        val schemeEnd = url.indexOf("://") + 3
        val pathStart = url.indexOf('/', schemeEnd)
        val domain = if (pathStart > 0) {
            url.substring(schemeEnd, pathStart)
        } else {
            url.substring(schemeEnd)
        }

        // Domain must not be empty
        if (domain.isBlank()) {
            return UrlValidationResult.Invalid(
                TabError.InvalidUrl(
                    url = url,
                    reason = "Missing domain name"
                )
            )
        }

        // Domain validation: must contain valid characters
        if (!isValidDomain(domain)) {
            return UrlValidationResult.Invalid(
                TabError.InvalidUrl(
                    url = url,
                    reason = "Invalid domain name"
                )
            )
        }

        return UrlValidationResult.Valid(url)
    }

    /**
     * Validate domain-like strings or treat as search queries
     */
    private fun validateDomainOrSearch(input: String): UrlValidationResult {
        // If contains dot and no spaces, treat as domain
        if (input.contains('.') && !input.contains(' ')) {
            // Check if it looks like a valid domain
            if (isValidDomain(input)) {
                // Add https:// scheme
                return UrlValidationResult.Valid("https://$input")
            } else {
                return UrlValidationResult.Invalid(
                    TabError.InvalidUrl(
                        url = input,
                        reason = "Invalid domain format"
                    )
                )
            }
        }

        // Otherwise, treat as search query (always valid)
        // Search engine will be applied by TabViewModel
        return UrlValidationResult.Valid(input)
    }

    /**
     * Check if string is a valid domain name
     */
    private fun isValidDomain(domain: String): Boolean {
        // Remove port if present
        val domainWithoutPort = domain.split(':').firstOrNull() ?: return false

        // Basic domain validation
        if (domainWithoutPort.isEmpty() || domainWithoutPort.length > 253) {
            return false
        }

        // Domain parts (labels) separated by dots
        val labels = domainWithoutPort.split('.')

        // Must have at least 2 labels (e.g., google.com)
        if (labels.size < 2) {
            return false
        }

        // Each label must be valid
        return labels.all { label ->
            label.isNotEmpty() &&
                    label.length <= 63 &&
                    label.matches(Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\$"))
        }
    }

    /**
     * Check if URL requires network connectivity
     */
    fun requiresNetwork(url: String): Boolean {
        val normalized = url.trim().lowercase()
        return normalized.startsWith("http://") ||
                normalized.startsWith("https://") ||
                (!normalized.startsWith("about:") &&
                        !normalized.startsWith("file://") &&
                        !normalized.startsWith("data:"))
    }

    /**
     * Extract domain from URL for display purposes
     *
     * @param url URL to extract domain from
     * @return Domain name or null if not applicable
     */
    fun extractDomain(url: String): String? {
        val trimmed = url.trim()

        // Handle HTTP(S) URLs
        val schemeIndex = trimmed.indexOf("://")
        if (schemeIndex > 0) {
            val afterScheme = trimmed.substring(schemeIndex + 3)
            val pathIndex = afterScheme.indexOf('/')
            return if (pathIndex > 0) {
                afterScheme.substring(0, pathIndex).split(':').first()
            } else {
                afterScheme.split(':').first()
            }
        }

        // Handle domain-like strings
        if (trimmed.contains('.') && !trimmed.contains(' ')) {
            return trimmed.split('/').first().split(':').first()
        }

        return null
    }
}
