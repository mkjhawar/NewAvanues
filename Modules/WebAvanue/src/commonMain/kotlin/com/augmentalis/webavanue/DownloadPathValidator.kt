package com.augmentalis.webavanue

/**
 * DownloadPathValidator - Cross-platform download path validation
 *
 * ## Overview
 * Validates download paths for existence, writability, and available storage space.
 * Platform-specific implementations handle path types (file system paths, content URIs, etc.).
 *
 * ## Validation Checks
 * - **Existence**: Path exists and is accessible
 * - **Writability**: Can write files to the location
 * - **Storage Space**: Sufficient space available (warns if < 100MB)
 *
 * ## Usage Example
 * ```kotlin
 * val validator = DownloadPathValidator(context)
 * val result = validator.validate("content://com.android.externalstorage.documents/tree/...")
 *
 * if (!result.isValid) {
 *     showError(result.errorMessage)
 * } else if (result.isLowSpace) {
 *     showWarning("Low storage space: ${result.availableSpaceMB} MB available")
 * }
 * ```
 *
 * ## Platform-Specific Behavior
 * - **Android**: Uses DocumentFile for content:// URIs, StatFs for space calculation
 * - **iOS**: Uses FileManager for file:// paths
 * - **Desktop**: Uses java.io.File for standard paths
 *
 * @see DownloadValidationResult for validation outcome
 */
expect class DownloadPathValidator {
    /**
     * Validate download path
     *
     * Performs comprehensive validation checks on the provided path:
     * 1. Path exists and is accessible
     * 2. Path is writable (can create/modify files)
     * 3. Available storage space calculation
     * 4. Low space warning (< 100MB)
     *
     * **Threading**: Performs I/O operations, should be called from background thread.
     * Use `withContext(Dispatchers.IO)` when calling from UI.
     *
     * @param path Platform-specific path (content:// URI on Android, file:// on others)
     * @return DownloadValidationResult with status, error message, and space information
     */
    suspend fun validate(path: String): DownloadValidationResult
}
