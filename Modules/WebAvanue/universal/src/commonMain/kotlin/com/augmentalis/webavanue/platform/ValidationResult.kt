package com.augmentalis.webavanue.platform

/**
 * Result of download path validation
 *
 * Contains validation status, error messages, and storage information.
 * Used to provide feedback to users about their selected download paths.
 *
 * ## Usage
 * ```kotlin
 * val result = pathValidator.validate(path)
 * if (result.isValid) {
 *     // Proceed with download
 *     if (result.isLowSpace) {
 *         showWarning("Low storage space")
 *     }
 * } else {
 *     showError(result.errorMessage ?: "Invalid path")
 * }
 * ```
 *
 * @property isValid Whether the path is valid and writable
 * @property errorMessage Human-readable error message (null if valid)
 * @property availableSpaceMB Free space available in megabytes
 * @property isLowSpace Whether available space is below 100MB threshold
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val availableSpaceMB: Long = 0,
    val isLowSpace: Boolean = false
) {
    companion object {
        /**
         * Low space threshold in megabytes
         */
        const val LOW_SPACE_THRESHOLD_MB = 100L

        /**
         * Create a successful validation result
         *
         * @param availableSpaceMB Free space in megabytes
         * @return ValidationResult with isValid=true
         */
        fun success(availableSpaceMB: Long): ValidationResult {
            return ValidationResult(
                isValid = true,
                availableSpaceMB = availableSpaceMB,
                isLowSpace = availableSpaceMB < LOW_SPACE_THRESHOLD_MB
            )
        }

        /**
         * Create a failed validation result
         *
         * @param errorMessage Reason for validation failure
         * @return ValidationResult with isValid=false
         */
        fun error(errorMessage: String): ValidationResult {
            return ValidationResult(
                isValid = false,
                errorMessage = errorMessage
            )
        }

        /**
         * Create validation result for non-existent path
         *
         * @return ValidationResult with standard error message
         */
        fun pathNotFound(): ValidationResult {
            return error("Path no longer exists. Please select a new location.")
        }

        /**
         * Create validation result for non-writable path
         *
         * @return ValidationResult with standard error message
         */
        fun notWritable(): ValidationResult {
            return error("Cannot write to this location. Please choose a different folder.")
        }

        /**
         * Create validation result for invalid URI
         *
         * @return ValidationResult with standard error message
         */
        fun invalidPath(): ValidationResult {
            return error("Invalid path format. Please select a valid location.")
        }

        /**
         * Create validation result for insufficient space
         *
         * @param availableSpaceMB Current available space
         * @return ValidationResult with standard error message
         */
        fun insufficientSpace(availableSpaceMB: Long): ValidationResult {
            return ValidationResult(
                isValid = false,
                errorMessage = "Insufficient storage space (${availableSpaceMB}MB available)",
                availableSpaceMB = availableSpaceMB,
                isLowSpace = true
            )
        }
    }

    /**
     * Get user-friendly status message
     *
     * @return Status message or null if valid with sufficient space
     */
    fun getStatusMessage(): String? {
        return when {
            !isValid -> errorMessage
            isLowSpace -> "Warning: Low storage space (${availableSpaceMB}MB free)"
            else -> null
        }
    }

    /**
     * Check if validation passed with warnings
     *
     * @return true if valid but has low space warning
     */
    fun hasWarning(): Boolean {
        return isValid && isLowSpace
    }
}
