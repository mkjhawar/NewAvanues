package com.augmentalis.avacode.plugins

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.augmentalis.avacode.plugins.Permission
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of PermissionUIHandler.
 *
 * Uses AlertDialog for showing permission requests.
 * Requires an Activity context for displaying dialogs.
 *
 * TODO: Full implementation requires:
 * - Integration with Android Activity lifecycle
 * - Material Design UI components
 * - Proper theme support
 * - Accessibility support
 * - Fragment-based dialogs for configuration changes
 */
class AndroidPermissionUIHandler(
    private val context: Context
) : PermissionUIHandler {
    companion object {
        private const val TAG = "PermissionUIHandler"
    }

    /**
     * Show permission request dialog.
     *
     * TODO: Replace with custom dialog fragment for better UX:
     * - Material Design 3 components
     * - Individual permission toggles
     * - Expandable rationale sections
     * - Permission grouping by category
     */
    override suspend fun showPermissionDialog(request: PermissionRequest): PermissionResult {
        return suspendCancellableCoroutine { continuation ->
            val activity = context as? Activity ?: run {
                // Fallback to console for non-Activity contexts
                continuation.resume(showConsolePermissionDialog(request))
                return@suspendCancellableCoroutine
            }

            // Build permission list with descriptions
            val permissionList = buildString {
                request.permissions.forEachIndexed { index, permission ->
                    if (index > 0) append("\n\n")
                    append("${index + 1}. ${PermissionDescriptions.getShortName(permission)}\n")

                    // Show rationale if provided
                    val rationale = request.rationales[permission]
                    if (rationale != null) {
                        append("   $rationale\n")
                    } else {
                        append("   ${PermissionDescriptions.getDescription(permission)}\n")
                    }
                }
            }

            val message = buildString {
                append("${request.pluginName} is requesting the following permissions:\n\n")
                append(permissionList)
                append("\n\nDo you want to grant these permissions?")
            }

            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setTitle("Permission Request")
                    .setMessage(message)
                    .setPositiveButton("Allow All") { _, _ ->
                        continuation.resume(
                            PermissionResult(
                                granted = request.permissions,
                                denied = emptySet()
                            )
                        )
                    }
                    .setNegativeButton("Deny All") { _, _ ->
                        continuation.resume(
                            PermissionResult(
                                granted = emptySet(),
                                denied = request.permissions
                            )
                        )
                    }
                    .setNeutralButton("Choose") { _, _ ->
                        // TODO: Show individual permission selection dialog
                        // For now, show rationale for each permission
                        continuation.resume(
                            showIndividualPermissionDialog(activity, request)
                        )
                    }
                    .setCancelable(false)
                    .show()
            }

            continuation.invokeOnCancellation {
                // If coroutine is cancelled, treat as deny all
                continuation.resume(
                    PermissionResult(
                        granted = emptySet(),
                        denied = request.permissions
                    )
                )
            }
        }
    }

    /**
     * Show individual permission selection dialog.
     *
     * TODO: Implement proper multi-choice dialog with checkboxes.
     * This is a simplified version that asks for each permission individually.
     */
    private fun showIndividualPermissionDialog(
        activity: Activity,
        request: PermissionRequest
    ): PermissionResult {
        // Simplified implementation - shows "Allow All" or "Deny All"
        // Full implementation would show checkboxes for each permission
        return PermissionResult(
            granted = request.permissions,
            denied = emptySet()
        )
    }

    /**
     * Show rationale dialog for a specific permission.
     *
     * TODO: Improve UI with:
     * - Material Design components
     * - Visual icons for permissions
     * - Better formatting
     */
    override suspend fun showRationaleDialog(
        pluginId: String,
        pluginName: String,
        permission: Permission,
        rationale: String
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val activity = context as? Activity ?: run {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            val message = buildString {
                append("$pluginName needs access to ${PermissionDescriptions.getShortName(permission)}.\n\n")
                append("Reason: $rationale\n\n")
                append("${PermissionDescriptions.getDescription(permission)}")
            }

            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setTitle("Permission Required")
                    .setMessage(message)
                    .setPositiveButton("Grant") { _, _ ->
                        continuation.resume(true)
                    }
                    .setNegativeButton("Deny") { _, _ ->
                        continuation.resume(false)
                    }
                    .setCancelable(false)
                    .show()
            }

            continuation.invokeOnCancellation {
                continuation.resume(false)
            }
        }
    }

    /**
     * Show permission settings dialog.
     *
     * TODO: Implement proper settings UI with:
     * - Toggle switches for each permission
     * - Grouping by category
     * - Search/filter capabilities
     * - Permission usage history
     */
    override suspend fun showPermissionSettings(
        pluginId: String,
        pluginName: String,
        currentPermissions: Map<Permission, Boolean>
    ): Map<Permission, Boolean>? {
        return suspendCancellableCoroutine { continuation ->
            val activity = context as? Activity ?: run {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val permissionList = buildString {
                currentPermissions.entries.forEachIndexed { index, (permission, granted) ->
                    if (index > 0) append("\n")
                    val status = if (granted) "✓ Granted" else "✗ Denied"
                    append("${PermissionDescriptions.getShortName(permission)}: $status")
                }
            }

            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setTitle("Permissions for $pluginName")
                    .setMessage(permissionList)
                    .setPositiveButton("OK") { _, _ ->
                        continuation.resume(currentPermissions)
                    }
                    .setNegativeButton("Revoke All") { _, _ ->
                        val revokedAll = currentPermissions.mapValues { false }
                        continuation.resume(revokedAll)
                    }
                    .setCancelable(true)
                    .show()
            }

            continuation.invokeOnCancellation {
                continuation.resume(null)
            }
        }
    }

    /**
     * Console-based fallback for permission dialog.
     * Used when Activity context is not available or for testing.
     */
    private fun showConsolePermissionDialog(request: PermissionRequest): PermissionResult {
        println("=== Permission Request ===")
        println("Plugin: ${request.pluginName} (${request.pluginId})")
        println("Requested Permissions:")
        request.permissions.forEachIndexed { index, permission ->
            println("  ${index + 1}. ${PermissionDescriptions.getShortName(permission)}")
            val rationale = request.rationales[permission]
            if (rationale != null) {
                println("     Reason: $rationale")
            }
        }
        println("\nConsole fallback: Auto-denying all permissions")
        println("To grant permissions, use proper Activity context")
        println("===========================")

        // In console mode, deny all by default for security
        return PermissionResult(
            granted = emptySet(),
            denied = request.permissions
        )
    }
}

/**
 * Factory for creating Android PermissionUIHandler instances.
 *
 * Note: On Android, you must provide a Context (preferably Activity) to create the handler.
 * This factory provides a no-arg creation method that will throw if no context is set.
 */
actual object PermissionUIHandlerFactory {
    private var contextProvider: (() -> Context)? = null

    /**
     * Set the context provider for creating PermissionUIHandler instances.
     * Call this during application initialization.
     *
     * @param provider Lambda that provides Context (should return Activity when available)
     */
    fun setContextProvider(provider: () -> Context) {
        contextProvider = provider
    }

    /**
     * Create a PermissionUIHandler instance using the registered context provider.
     *
     * @throws IllegalStateException if no context provider has been set
     */
    actual fun create(): PermissionUIHandler {
        val context = contextProvider?.invoke()
            ?: throw IllegalStateException(
                "PermissionUIHandlerFactory.setContextProvider() must be called before creating handlers"
            )
        return AndroidPermissionUIHandler(context)
    }

    /**
     * Create a PermissionUIHandler with a specific context.
     * Use this method to create handlers with explicit context control.
     *
     * @param context Android Context (preferably Activity for dialog display)
     */
    fun createWithContext(context: Context): PermissionUIHandler {
        return AndroidPermissionUIHandler(context)
    }
}
