package com.augmentalis.webavanue

/**
 * Platform-specific manager for download storage permissions
 *
 * Handles Android's complex permission requirements across different API levels:
 * - **API 21-28**: Requires `WRITE_EXTERNAL_STORAGE` permission
 * - **API 29-32**: Scoped storage (no permission required for Downloads folder)
 * - **API 33+**: Enhanced scoped storage (no permission required)
 *
 * ## Permission Strategy
 * This manager implements a graceful degradation strategy:
 * 1. Check if permission is required for current API level
 * 2. If required, request permission with rationale
 * 3. On denial, offer to open app settings
 * 4. Always fallback to default Downloads folder (no permission needed)
 *
 * ## Usage
 * ```kotlin
 * val permissionManager = DownloadPermissionManager(context)
 *
 * if (permissionManager.isPermissionRequired()) {
 *     if (!permissionManager.isPermissionGranted()) {
 *         // Show rationale to user
 *         val granted = permissionManager.requestPermission()
 *         if (!granted) {
 *             // Fallback to default Downloads folder
 *             // or offer to open settings
 *             permissionManager.openPermissionSettings()
 *         }
 *     }
 * }
 *
 * // Proceed with download (permission guaranteed or not required)
 * ```
 *
 * @see com.augmentalis.webavanue.platform.DownloadFilePickerLauncher
 */
expect class DownloadPermissionManager {

    /**
     * Check if storage permission is required for current API level
     *
     * Returns true only for Android API 21-28 where `WRITE_EXTERNAL_STORAGE`
     * is needed for custom download locations.
     *
     * **API Level Behavior**:
     * - API 21-28: `true` (permission required)
     * - API 29+: `false` (scoped storage, no permission needed)
     *
     * @return true if permission is required, false otherwise
     */
    fun isPermissionRequired(): Boolean

    /**
     * Check if storage permission is currently granted
     *
     * For API levels where permission is not required (29+), this always
     * returns true since scoped storage provides implicit access.
     *
     * **API Level Behavior**:
     * - API 21-28: Checks `WRITE_EXTERNAL_STORAGE` status
     * - API 29+: Always returns `true`
     *
     * @return true if permission granted or not required, false if denied
     */
    fun isPermissionGranted(): Boolean

    /**
     * Request storage permission from user
     *
     * Shows system permission dialog if permission is required and not granted.
     * The operation is suspended until user responds.
     *
     * **Best Practice**: Show a rationale dialog before calling this method
     * to explain why the permission is needed.
     *
     * **API Level Behavior**:
     * - API 21-28: Requests `WRITE_EXTERNAL_STORAGE` permission
     * - API 29+: Immediately returns `true` (no permission needed)
     *
     * ## Rationale Dialog
     * Android requires showing a rationale if user has previously denied
     * the permission. Check `shouldShowRequestPermissionRationale()` before
     * calling this method.
     *
     * @return true if permission granted or not required, false if denied
     * @throws IllegalStateException if called from non-Activity context
     */
    suspend fun requestPermission(): Boolean

    /**
     * Open system settings for manual permission grant
     *
     * Use this when user has denied permission and checking
     * `shouldShowRequestPermissionRationale()` returns false
     * (indicating "Don't ask again" was selected).
     *
     * Opens the app's settings page where user can manually grant
     * storage permission.
     *
     * **User Flow**:
     * 1. User clicks "Grant Permission" button
     * 2. App opens Settings > Apps > [App Name] > Permissions
     * 3. User toggles "Storage" permission ON
     * 4. User returns to app (no callback, app must recheck permission)
     *
     * ## Checking Result
     * After user returns from settings, recheck permission status:
     * ```kotlin
     * permissionManager.openPermissionSettings()
     * // ... user returns to app ...
     * if (permissionManager.isPermissionGranted()) {
     *     // User granted permission
     * }
     * ```
     */
    fun openPermissionSettings()

    /**
     * Check if should show permission rationale dialog
     *
     * Android system method that indicates whether a rationale dialog
     * should be shown before requesting permission.
     *
     * **Returns `true` when**:
     * - User has previously denied the permission
     * - User has NOT selected "Don't ask again"
     *
     * **Returns `false` when**:
     * - Permission never requested before
     * - User selected "Don't ask again"
     * - Permission already granted
     *
     * ## Usage
     * ```kotlin
     * if (permissionManager.shouldShowRationale()) {
     *     // Show explanation dialog
     *     showPermissionRationaleDialog {
     *         permissionManager.requestPermission()
     *     }
     * } else {
     *     // First request or "Don't ask again" selected
     *     if (!permissionManager.isPermissionGranted()) {
     *         // Open settings instead of requesting
     *         permissionManager.openPermissionSettings()
     *     }
     * }
     * ```
     *
     * @return true if rationale should be shown, false otherwise
     */
    fun shouldShowRationale(): Boolean
}
