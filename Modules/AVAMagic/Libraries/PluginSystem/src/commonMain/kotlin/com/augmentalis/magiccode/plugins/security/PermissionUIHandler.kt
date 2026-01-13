package com.augmentalis.avacode.plugins.security

import com.augmentalis.avacode.plugins.core.Permission

/**
 * Permission request data model.
 *
 * Encapsulates all information needed to request permissions from a user
 * via a permission dialog. Includes plugin identification, requested
 * permissions, and optional explanatory rationales.
 *
 * @property pluginId Unique plugin identifier (e.g., "com.example.plugin")
 * @property pluginName Human-readable plugin name displayed in UI
 * @property permissions Set of permissions being requested
 * @property rationales Map of permission to rationale text. Each rationale
 *                      explains why that specific permission is needed.
 *
 * @see PermissionUIHandler.showPermissionDialog
 */
data class PermissionRequest(
    val pluginId: String,
    val pluginName: String,
    val permissions: Set<Permission>,
    val rationales: Map<Permission, String> = emptyMap()
)

/**
 * Permission result data model.
 *
 * Represents the outcome of a permission request dialog, indicating
 * which permissions were granted, denied, or marked with "Don't ask again".
 *
 * ## Usage
 * After showing a permission dialog, this result indicates user decisions:
 * - Granted permissions can be used immediately
 * - Denied permissions cannot be used (may be requested again later)
 * - "Don't ask again" permissions should not show UI prompts in future
 *
 * @property granted Set of permissions granted by the user
 * @property denied Set of permissions denied by the user
 * @property dontAskAgain Set of permissions for which user selected "Don't ask again".
 *                        These are typically also in [denied].
 *
 * @see PermissionUIHandler.showPermissionDialog
 */
data class PermissionResult(
    val granted: Set<Permission>,
    val denied: Set<Permission>,
    val dontAskAgain: Set<Permission> = emptySet()
) {
    /**
     * Check if all requested permissions were granted.
     *
     * Useful for determining if a plugin has all required permissions
     * to proceed with initialization or a feature.
     *
     * @return `true` if no permissions were denied, `false` otherwise
     */
    val allGranted: Boolean
        get() = denied.isEmpty()

    /**
     * Check if any permissions were granted.
     *
     * Useful for determining if a plugin has at least some permissions
     * and can provide partial functionality.
     *
     * @return `true` if at least one permission was granted, `false` otherwise
     */
    val anyGranted: Boolean
        get() = granted.isNotEmpty()
}

/**
 * Platform-specific UI handler for permission dialogs.
 *
 * Defines the interface for displaying permission request dialogs to users.
 * Platform-specific implementations provide native UI components (Android dialogs,
 * iOS UIAlertController, JVM Swing dialogs, etc.) for requesting and managing
 * plugin permissions.
 *
 * ## Platform Implementations
 * Each platform must provide an implementation that:
 * - Displays native-looking permission dialogs
 * - Shows permission descriptions and rationales clearly
 * - Allows users to grant/deny individual permissions
 * - Supports "Don't ask again" option for denied permissions
 * - Follows platform UI guidelines and accessibility standards
 *
 * ## Thread Safety
 * All methods are suspending functions and may be called from any coroutine.
 * Implementations must ensure UI operations occur on the appropriate thread
 * (e.g., Android main thread, iOS main queue).
 *
 * ## Example Implementation (Pseudocode)
 * ```kotlin
 * class AndroidPermissionUIHandler(private val context: Context) : PermissionUIHandler {
 *     override suspend fun showPermissionDialog(request: PermissionRequest): PermissionResult {
 *         return suspendCancellableCoroutine { continuation ->
 *             AlertDialog.Builder(context)
 *                 .setTitle("${request.pluginName} requests permissions")
 *                 .setMessage(formatPermissions(request))
 *                 .setPositiveButton("Allow") { _, _ ->
 *                     continuation.resume(PermissionResult(
 *                         granted = request.permissions,
 *                         denied = emptySet()
 *                     ))
 *                 }
 *                 .setNegativeButton("Deny") { _, _ ->
 *                     continuation.resume(PermissionResult(
 *                         granted = emptySet(),
 *                         denied = request.permissions
 *                     ))
 *                 }
 *                 .show()
 *         }
 *     }
 * }
 * ```
 *
 * @see PermissionUIHandlerFactory
 * @see PermissionManager
 */
interface PermissionUIHandler {
    /**
     * Show permission request dialog to user.
     *
     * Displays a native dialog showing:
     * - Plugin name requesting permissions
     * - List of permissions with descriptions
     * - Rationales explaining why each permission is needed
     * - Options to grant or deny each permission
     * - Optional "Don't ask again" checkbox for denials
     *
     * ## User Experience
     * The dialog should clearly communicate what permissions are being
     * requested and why. Users should be able to:
     * - Grant all permissions at once
     * - Deny all permissions at once
     * - Grant/deny individual permissions (platform permitting)
     * - Mark permissions as "Don't ask again"
     *
     * ## Blocking Behavior
     * This method suspends until the user makes a decision. It must
     * resume with a [PermissionResult] reflecting the user's choices.
     *
     * @param request Permission request containing plugin info, permissions,
     *                and optional rationales
     *
     * @return [PermissionResult] with user's grant/deny decisions
     */
    suspend fun showPermissionDialog(request: PermissionRequest): PermissionResult

    /**
     * Show detailed rationale dialog for a specific permission.
     *
     * Displays an educational dialog explaining why a specific permission
     * is needed. This is typically shown:
     * - When a permission was previously denied and plugin requests again
     * - When user taps "Learn More" or "Why?" in permission lists
     * - Before showing the actual permission request for sensitive permissions
     *
     * ## Dialog Content
     * Should include:
     * - Permission name and description
     * - Detailed rationale explaining the use case
     * - Examples of what the plugin will do with this permission
     * - Option to grant the permission after reading explanation
     *
     * ## Return Value
     * Returns `true` if the user indicates they want to grant the permission
     * after reading the rationale (e.g., clicks "Grant" or "Allow").
     * Returns `false` if user dismisses or explicitly declines.
     *
     * @param pluginId Plugin identifier requesting the permission
     * @param pluginName Human-readable plugin name shown in dialog
     * @param permission The specific permission to explain
     * @param rationale Detailed text explaining why this permission is needed,
     *                  what it will be used for, and privacy implications
     *
     * @return `true` if user wants to grant after seeing rationale, `false` otherwise
     */
    suspend fun showRationaleDialog(
        pluginId: String,
        pluginName: String,
        permission: Permission,
        rationale: String
    ): Boolean

    /**
     * Show permission management settings dialog for a plugin.
     *
     * Displays a settings UI listing all possible permissions, showing
     * which are currently granted and which are denied. Users can toggle
     * permissions on/off to modify grants.
     *
     * ## Dialog Features
     * - List of all permissions (not just requested ones)
     * - Visual indication of granted vs denied state
     * - Toggle switches or checkboxes to modify permissions
     * - Permission descriptions
     * - Save/Cancel buttons
     *
     * ## Use Cases
     * - User navigates to plugin settings screen
     * - User wants to review/modify previously granted permissions
     * - Administrator wants to audit plugin permissions
     *
     * ## Return Value
     * Returns updated permission states if user saves changes.
     * Returns `null` if user cancels without making changes.
     *
     * @param pluginId Plugin identifier
     * @param pluginName Human-readable plugin name shown in dialog title
     * @param currentPermissions Map of all permissions to their current state
     *                           (true = granted, false = denied/not granted)
     *
     * @return Updated permission states map if saved, `null` if cancelled
     */
    suspend fun showPermissionSettings(
        pluginId: String,
        pluginName: String,
        currentPermissions: Map<Permission, Boolean>
    ): Map<Permission, Boolean>?
}

/**
 * Factory for creating platform-specific PermissionUIHandler instances.
 *
 * Provides platform-appropriate implementations of [PermissionUIHandler]
 * using the expect/actual pattern. Each platform provides its own
 * implementation that uses native UI components.
 *
 * ## Platform Requirements
 * - **Android**: Requires `Context` parameter for creating dialogs
 * - **JVM**: Optional `Component?` parameter as parent for Swing dialogs
 * - **iOS**: Optional `UIViewController?` parameter for presenting alerts
 * - **Web**: May require DOM element reference
 *
 * ## Usage Example
 * ```kotlin
 * // Platform-specific creation
 * val uiHandler = PermissionUIHandlerFactory.create()
 * val manager = PermissionManager(uiHandler, persistence)
 * ```
 *
 * @see PermissionUIHandler
 */
expect object PermissionUIHandlerFactory {
    /**
     * Create a PermissionUIHandler instance for the current platform.
     *
     * Returns a platform-specific implementation that displays native
     * permission dialogs appropriate for the runtime environment.
     *
     * @return Platform-specific PermissionUIHandler instance
     *
     * @see PermissionUIHandler
     */
    fun create(): PermissionUIHandler
}

/**
 * Permission descriptions for UI display.
 *
 * Provides user-friendly descriptions of what each permission allows.
 * Used by UI implementations to show clear explanations of permission
 * capabilities in dialogs and settings screens.
 *
 * ## Usage
 * ```kotlin
 * val description = PermissionDescriptions.getDescription(Permission.CAMERA)
 * // Returns: "Access device camera to capture photos and videos"
 *
 * val shortName = PermissionDescriptions.getShortName(Permission.STORAGE_READ)
 * // Returns: "Read Storage"
 * ```
 *
 * @see PermissionUIHandler
 */
object PermissionDescriptions {
    private val descriptions = mapOf(
        Permission.CAMERA to "Access device camera to capture photos and videos",
        Permission.LOCATION to "Access device location for location-based features",
        Permission.STORAGE_READ to "Read files and data from device storage",
        Permission.STORAGE_WRITE to "Write files and data to device storage",
        Permission.NETWORK to "Access network to communicate with external services",
        Permission.MICROPHONE to "Access microphone to record audio",
        Permission.CONTACTS to "Access contacts to read or modify contact information",
        Permission.CALENDAR to "Access calendar to read or modify events",
        Permission.BLUETOOTH to "Access Bluetooth to connect with nearby devices",
        Permission.SENSORS to "Access device sensors (accelerometer, gyroscope, etc.)",
        Permission.ACCESSIBILITY_SERVICES to "Access accessibility services to assist users with disabilities",
        Permission.PAYMENTS to "Process payments and access payment methods"
    )

    /**
     * Get a user-friendly description for a permission.
     *
     * Returns a complete sentence explaining what capabilities the
     * permission grants. Suitable for display in permission request
     * dialogs and settings screens.
     *
     * @param permission The permission to describe
     * @return Human-readable description explaining permission capabilities
     */
    fun getDescription(permission: Permission): String {
        return descriptions[permission] ?: "Access to ${permission.name.lowercase().replace('_', ' ')}"
    }

    /**
     * Get a short display name for a permission.
     *
     * Returns a concise name suitable for display in permission lists,
     * checkboxes, or table headers where space is limited.
     *
     * @param permission The permission
     * @return Short, capitalized display name (e.g., "Camera", "Read Storage")
     */
    fun getShortName(permission: Permission): String {
        return when (permission) {
            Permission.STORAGE_READ -> "Read Storage"
            Permission.STORAGE_WRITE -> "Write Storage"
            Permission.ACCESSIBILITY_SERVICES -> "Accessibility"
            else -> permission.name.lowercase().split('_')
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }
}
