package com.augmentalis.magiccode.plugins.security

import com.augmentalis.magiccode.plugins.core.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Component
import java.awt.GraphicsEnvironment
import javax.swing.JCheckBox
import javax.swing.JOptionPane
import javax.swing.JPanel
import java.awt.GridLayout

/**
 * JVM implementation of PermissionUIHandler.
 *
 * Uses Swing dialogs for GUI mode, falls back to console prompts in headless mode.
 *
 * TODO: Full implementation requires:
 * - Better Swing UI with custom components
 * - JavaFX alternative for modern UIs
 * - Integration with desktop notification systems
 * - Native OS permission dialogs (via JNA/JNI)
 */
class JvmPermissionUIHandler(
    private val parentComponent: Component? = null
) : PermissionUIHandler {
    companion object {
        private const val TAG = "PermissionUIHandler"
    }

    private val isHeadless: Boolean = GraphicsEnvironment.isHeadless()

    /**
     * Show permission request dialog.
     *
     * Uses Swing JOptionPane in GUI mode, console prompt in headless mode.
     *
     * TODO: Improve Swing UI with:
     * - Custom dialog with checkboxes for individual permissions
     * - Icons and better visual hierarchy
     * - Remember choice checkbox
     * - JavaFX alternative for better UX
     */
    override suspend fun showPermissionDialog(request: PermissionRequest): PermissionResult {
        return withContext(Dispatchers.IO) {
            if (isHeadless) {
                showConsolePermissionDialog(request)
            } else {
                showSwingPermissionDialog(request)
            }
        }
    }

    /**
     * Show Swing-based permission dialog.
     */
    private fun showSwingPermissionDialog(request: PermissionRequest): PermissionResult {
        // Build message with permission list
        val message = buildString {
            append("${request.pluginName} is requesting the following permissions:\n\n")
            request.permissions.forEachIndexed { index, permission ->
                if (index > 0) append("\n")
                append("• ${PermissionDescriptions.getShortName(permission)}\n")

                val rationale = request.rationales[permission]
                if (rationale != null) {
                    append("  $rationale\n")
                } else {
                    append("  ${PermissionDescriptions.getDescription(permission)}\n")
                }
            }
        }

        val options = arrayOf("Allow All", "Deny All", "Choose Individually")
        val result = JOptionPane.showOptionDialog(
            parentComponent,
            message,
            "Permission Request",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )

        return when (result) {
            0 -> PermissionResult(granted = request.permissions, denied = emptySet())
            1 -> PermissionResult(granted = emptySet(), denied = request.permissions)
            2 -> showIndividualPermissionDialog(request)
            else -> PermissionResult(granted = emptySet(), denied = request.permissions)
        }
    }

    /**
     * Show dialog for choosing individual permissions.
     *
     * Creates checkboxes for each permission.
     */
    private fun showIndividualPermissionDialog(request: PermissionRequest): PermissionResult {
        val panel = JPanel(GridLayout(0, 1))
        val checkboxes = mutableMapOf<Permission, JCheckBox>()

        request.permissions.forEach { permission ->
            val text = buildString {
                append(PermissionDescriptions.getShortName(permission))
                val rationale = request.rationales[permission]
                if (rationale != null) {
                    append(" - $rationale")
                }
            }
            val checkbox = JCheckBox(text, true) // Default to checked
            checkboxes[permission] = checkbox
            panel.add(checkbox)
        }

        val result = JOptionPane.showConfirmDialog(
            parentComponent,
            panel,
            "Choose Permissions for ${request.pluginName}",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        if (result != JOptionPane.OK_OPTION) {
            return PermissionResult(granted = emptySet(), denied = request.permissions)
        }

        val granted = mutableSetOf<Permission>()
        val denied = mutableSetOf<Permission>()

        checkboxes.forEach { (permission, checkbox) ->
            if (checkbox.isSelected) {
                granted.add(permission)
            } else {
                denied.add(permission)
            }
        }

        return PermissionResult(granted = granted, denied = denied)
    }

    /**
     * Show rationale dialog for a specific permission.
     *
     * TODO: Improve with icons and better formatting
     */
    override suspend fun showRationaleDialog(
        pluginId: String,
        pluginName: String,
        permission: Permission,
        rationale: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            if (isHeadless) {
                showConsoleRationaleDialog(pluginName, permission, rationale)
            } else {
                showSwingRationaleDialog(pluginName, permission, rationale)
            }
        }
    }

    /**
     * Show Swing-based rationale dialog.
     */
    private fun showSwingRationaleDialog(
        pluginName: String,
        permission: Permission,
        rationale: String
    ): Boolean {
        val message = buildString {
            append("$pluginName needs access to ${PermissionDescriptions.getShortName(permission)}.\n\n")
            append("Reason: $rationale\n\n")
            append("${PermissionDescriptions.getDescription(permission)}")
        }

        val result = JOptionPane.showConfirmDialog(
            parentComponent,
            message,
            "Permission Required",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        )

        return result == JOptionPane.YES_OPTION
    }

    /**
     * Show permission settings dialog.
     *
     * TODO: Implement with:
     * - Table view of permissions
     * - Toggle buttons for each permission
     * - Filter and search capabilities
     */
    override suspend fun showPermissionSettings(
        pluginId: String,
        pluginName: String,
        currentPermissions: Map<Permission, Boolean>
    ): Map<Permission, Boolean>? {
        return withContext(Dispatchers.IO) {
            if (isHeadless) {
                // In headless mode, can't modify settings
                null
            } else {
                showSwingPermissionSettings(pluginName, currentPermissions)
            }
        }
    }

    /**
     * Show Swing-based permission settings dialog.
     */
    private fun showSwingPermissionSettings(
        pluginName: String,
        currentPermissions: Map<Permission, Boolean>
    ): Map<Permission, Boolean>? {
        val panel = JPanel(GridLayout(0, 1))
        val checkboxes = mutableMapOf<Permission, JCheckBox>()

        currentPermissions.forEach { (permission, granted) ->
            val checkbox = JCheckBox(PermissionDescriptions.getShortName(permission), granted)
            checkboxes[permission] = checkbox
            panel.add(checkbox)
        }

        val result = JOptionPane.showConfirmDialog(
            parentComponent,
            panel,
            "Permissions for $pluginName",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        if (result != JOptionPane.OK_OPTION) {
            return null
        }

        return checkboxes.mapValues { (_, checkbox) -> checkbox.isSelected }
    }

    /**
     * Console-based permission dialog (headless mode).
     */
    private fun showConsolePermissionDialog(request: PermissionRequest): PermissionResult {
        println("\n=== Permission Request ===")
        println("Plugin: ${request.pluginName} (${request.pluginId})")
        println("\nRequested Permissions:")
        request.permissions.forEachIndexed { index, permission ->
            println("  ${index + 1}. ${PermissionDescriptions.getShortName(permission)}")
            val rationale = request.rationales[permission]
            if (rationale != null) {
                println("     Reason: $rationale")
            } else {
                println("     ${PermissionDescriptions.getDescription(permission)}")
            }
        }

        print("\nGrant all permissions? (yes/no/choose): ")
        val response = readlnOrNull()?.trim()?.lowercase()

        return when (response) {
            "yes", "y" -> {
                println("✓ All permissions granted")
                PermissionResult(granted = request.permissions, denied = emptySet())
            }
            "choose", "c" -> {
                showConsoleIndividualPermissionDialog(request)
            }
            else -> {
                println("✗ All permissions denied")
                PermissionResult(granted = emptySet(), denied = request.permissions)
            }
        }
    }

    /**
     * Console-based individual permission dialog.
     */
    private fun showConsoleIndividualPermissionDialog(request: PermissionRequest): PermissionResult {
        val granted = mutableSetOf<Permission>()
        val denied = mutableSetOf<Permission>()

        println("\nChoose permissions individually:")
        request.permissions.forEach { permission ->
            print("  Grant ${PermissionDescriptions.getShortName(permission)}? (y/n): ")
            val response = readlnOrNull()?.trim()?.lowercase()
            if (response == "y" || response == "yes") {
                granted.add(permission)
                println("    ✓ Granted")
            } else {
                denied.add(permission)
                println("    ✗ Denied")
            }
        }

        return PermissionResult(granted = granted, denied = denied)
    }

    /**
     * Console-based rationale dialog.
     */
    private fun showConsoleRationaleDialog(
        pluginName: String,
        permission: Permission,
        rationale: String
    ): Boolean {
        println("\n=== Permission Required ===")
        println("Plugin: $pluginName")
        println("Permission: ${PermissionDescriptions.getShortName(permission)}")
        println("\nReason: $rationale")
        println("\n${PermissionDescriptions.getDescription(permission)}")

        print("\nGrant this permission? (yes/no): ")
        val response = readlnOrNull()?.trim()?.lowercase()

        return response == "yes" || response == "y"
    }
}

/**
 * Factory for creating JVM PermissionUIHandler instances.
 */
actual object PermissionUIHandlerFactory {
    private var parentComponentProvider: (() -> Component?)? = null

    /**
     * Set the parent component provider for creating PermissionUIHandler instances.
     * This is optional - if not set, dialogs will be created without a parent.
     *
     * @param provider Lambda that provides Component? for Swing dialog parent
     */
    fun setParentComponentProvider(provider: () -> Component?) {
        parentComponentProvider = provider
    }

    /**
     * Create a PermissionUIHandler instance.
     * Uses the registered parent component provider if available.
     */
    actual fun create(): PermissionUIHandler {
        val parentComponent = parentComponentProvider?.invoke()
        return JvmPermissionUIHandler(parentComponent)
    }

    /**
     * Create a PermissionUIHandler with a specific parent component.
     *
     * @param parentComponent Optional Swing Component to use as dialog parent
     */
    fun createWithParent(parentComponent: Component? = null): PermissionUIHandler {
        return JvmPermissionUIHandler(parentComponent)
    }
}
