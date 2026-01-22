package com.augmentalis.webavanue

/**
 * Platform-specific file picker launcher for selecting download directories
 *
 * Uses Android Storage Access Framework (SAF) on Android to provide
 * native file/folder picker UI with persistent URI permissions.
 *
 * ## Usage
 * ```kotlin
 * val launcher = DownloadFilePickerLauncher(context)
 * val selectedUri = launcher.pickDownloadDirectory()
 * if (selectedUri != null) {
 *     // User selected a directory
 *     saveDownloadPath(selectedUri)
 * } else {
 *     // User cancelled
 * }
 * ```
 *
 * ## Platform Implementations
 * - **Android**: Uses `Intent.ACTION_OPEN_DOCUMENT_TREE` with persistent permissions
 * - **iOS**: Not applicable (uses system download manager)
 * - **Web**: Not applicable (browser handles downloads)
 *
 * @see com.augmentalis.webavanue.platform.DownloadPermissionManager
 */
expect class DownloadFilePickerLauncher {
    /**
     * Launch platform-specific directory picker
     *
     * Opens the system file picker UI allowing the user to navigate and
     * select a directory for saving downloads. The operation is suspended
     * until the user makes a selection or cancels.
     *
     * **Android Behavior**:
     * - Launches Storage Access Framework picker
     * - Grants persistent read/write permissions to selected directory
     * - Returns `content://` URI that survives app restart
     * - Can start navigation at `initialUri` if provided
     *
     * @param initialUri Optional URI to start navigation at (Android only)
     * @return Selected directory URI as String, or null if cancelled
     *
     * ## Return Value Format
     * - **Android**: `content://com.android.externalstorage.documents/tree/...`
     * - **Cancelled**: `null`
     *
     * ## Threading
     * This is a suspending function that must be called from a coroutine.
     * UI will block until user completes selection.
     *
     * @throws IllegalStateException if called from non-Android platform
     */
    suspend fun pickDownloadDirectory(initialUri: String? = null): String?
}
