package com.augmentalis.webavanue.platform

/**
 * Android implementation of DownloadPathValidator
 *
 * TODO: Phase 4 - Implement full path validation with DocumentFile
 * This is a stub implementation to satisfy the compiler.
 */
actual class DownloadPathValidator {
    /**
     * Validate download path (stub implementation)
     *
     * TODO: Implement using DocumentFile for content:// URIs and StatFs for space checks
     */
    actual suspend fun validate(path: String): DownloadValidationResult {
        // Stub: Return valid result
        return DownloadValidationResult(
            isValid = true,
            errorMessage = null,
            availableSpaceMB = 1000L,
            isLowSpace = false
        )
    }
}
