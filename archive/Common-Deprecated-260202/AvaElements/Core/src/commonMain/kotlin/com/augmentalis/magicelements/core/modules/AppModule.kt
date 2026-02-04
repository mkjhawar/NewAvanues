package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.functions.PluginTier

/**
 * Platform delegate interface for AppModule.
 * Platform implementations provide actual app integration capabilities.
 */
interface AppModuleDelegate {
    /**
     * Open an app by package name or common name.
     * @param packageOrName Package name (e.g., "com.example.app") or common name (e.g., "settings")
     * @param options Launch options (e.g., action, data, extras)
     */
    suspend fun openApp(packageOrName: String, options: Map<String, Any?>?)

    /**
     * Open system settings.
     * @param section Settings section to open (null for main settings)
     */
    suspend fun openSettings(section: String?)

    /**
     * Share content using system share sheet.
     * @param content Content to share (text, url, files, etc.)
     */
    suspend fun share(content: Map<String, Any?>)

    /**
     * Share content to a specific app.
     * @param target Target app identifier
     * @param content Content to share
     */
    suspend fun shareTo(target: String, content: Map<String, Any?>)

    /**
     * Copy text to clipboard.
     * @param text Text to copy
     */
    fun copyToClipboard(text: String)

    /**
     * Get clipboard content.
     * @return Clipboard text or null if empty
     */
    fun getClipboard(): String?

    /**
     * Check if clipboard has content.
     * @return true if clipboard has content
     */
    fun hasClipboardContent(): Boolean

    /**
     * Show a notification.
     * @param config Notification configuration (title, body, icon, priority, etc.)
     * @return Notification ID for tracking
     */
    suspend fun showNotification(config: Map<String, Any?>): String

    /**
     * Cancel a notification by ID.
     * @param id Notification ID
     */
    suspend fun cancelNotification(id: String)

    /**
     * Clear all notifications.
     */
    suspend fun clearAllNotifications()

    /**
     * Check if a permission is granted.
     * @param permission Permission identifier
     * @return true if granted
     */
    fun checkPermission(permission: String): Boolean

    /**
     * Request a permission.
     * @param permission Permission identifier
     * @return true if granted, false if denied
     */
    suspend fun requestPermission(permission: String): Boolean

    /**
     * Handle a deep link URL.
     * @param url Deep link URL
     * @return Parsed deep link data
     */
    suspend fun handleDeepLink(url: String): Map<String, Any?>
}

/**
 * AppModule - Provides cross-app integration for AvaCode.
 *
 * Enables MEL plugins to interact with other apps, system features,
 * clipboard, notifications, permissions, and deep links.
 *
 * Usage in MEL:
 * ```
 * # LOGIC tier methods
 * @app.open("com.example.app")           # Open an app
 * @app.open("settings")                  # Open settings
 * @app.openSettings("wifi")              # Open WiFi settings
 * @app.share({ text: "Hello!" })         # Share via system sheet
 * @app.share.to("twitter", { text: "Hello!" })  # Share to specific app
 * @app.notification.show({               # Show notification
 *     title: "New Message",
 *     body: "You have mail"
 * })
 * @app.notification.cancel("notif-123")  # Cancel notification
 * @app.notification.clearAll()           # Clear all notifications
 * @app.permission.request("camera")      # Request permission
 * @app.deepLink("myapp://path/to/page")  # Handle deep link
 *
 * # DATA tier methods
 * @app.clipboard.copy("Hello World")     # Copy to clipboard
 * @app.clipboard.paste()                 # Get clipboard content
 * @app.clipboard.has()                   # Check if clipboard has content
 * @app.permission.check("camera")        # Check permission status
 * ```
 *
 * @param delegate Platform implementation (null for unsupported platforms)
 */
class AppModule(
    private val delegate: AppModuleDelegate?
) : BaseModule(
    name = "app",
    version = "1.0.0",
    minimumTier = PluginTier.DATA
) {

    init {
        // ========== LOGIC Tier Methods ==========

        registerMethod(
            name = "open",
            tier = PluginTier.LOGIC,
            description = "Open an app by package name or common name",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "packageOrName",
                    type = "String",
                    required = true,
                    description = "Package name or common name (e.g., 'settings', 'browser')"
                ),
                MethodParameter(
                    name = "options",
                    type = "Map<String, Any?>",
                    required = false,
                    description = "Launch options (action, data, extras)"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val packageOrName = args.argString(0, "packageOrName")
                val options = args.argOrNull<Map<String, Any?>>(1)
                delegate!!.openApp(packageOrName, options)
            }
        )

        registerMethod(
            name = "openSettings",
            tier = PluginTier.LOGIC,
            description = "Open system settings",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "section",
                    type = "String",
                    required = false,
                    description = "Settings section (e.g., 'wifi', 'bluetooth', 'location')"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val section = args.argOrNull<String>(0)
                delegate!!.openSettings(section)
            }
        )

        registerMethod(
            name = "share",
            tier = PluginTier.LOGIC,
            description = "Share content using system share sheet",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "content",
                    type = "Map<String, Any?>",
                    required = true,
                    description = "Content to share (text, url, files, title, etc.)"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val content = args.arg<Map<String, Any?>>(0, "content")
                delegate!!.share(content)
            }
        )

        registerMethod(
            name = "share.to",
            tier = PluginTier.LOGIC,
            description = "Share content to a specific app",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "target",
                    type = "String",
                    required = true,
                    description = "Target app identifier"
                ),
                MethodParameter(
                    name = "content",
                    type = "Map<String, Any?>",
                    required = true,
                    description = "Content to share"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val target = args.argString(0, "target")
                val content = args.arg<Map<String, Any?>>(1, "content")
                delegate!!.shareTo(target, content)
            }
        )

        registerMethod(
            name = "notification.show",
            tier = PluginTier.LOGIC,
            description = "Show a notification",
            returnType = "String",
            parameters = listOf(
                MethodParameter(
                    name = "config",
                    type = "Map<String, Any?>",
                    required = true,
                    description = "Notification config (title, body, icon, priority, etc.)"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val config = args.arg<Map<String, Any?>>(0, "config")
                delegate!!.showNotification(config)
            }
        )

        registerMethod(
            name = "notification.cancel",
            tier = PluginTier.LOGIC,
            description = "Cancel a notification by ID",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "id",
                    type = "String",
                    required = true,
                    description = "Notification ID"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val id = args.argString(0, "id")
                delegate!!.cancelNotification(id)
            }
        )

        registerMethod(
            name = "notification.clearAll",
            tier = PluginTier.LOGIC,
            description = "Clear all notifications",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.clearAllNotifications()
            }
        )

        registerMethod(
            name = "permission.request",
            tier = PluginTier.LOGIC,
            description = "Request a permission",
            returnType = "Boolean",
            parameters = listOf(
                MethodParameter(
                    name = "permission",
                    type = "String",
                    required = true,
                    description = "Permission identifier (e.g., 'camera', 'microphone')"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val permission = args.argString(0, "permission")
                delegate!!.requestPermission(permission)
            }
        )

        registerMethod(
            name = "deepLink",
            tier = PluginTier.LOGIC,
            description = "Handle a deep link URL",
            returnType = "Map<String, Any?>",
            parameters = listOf(
                MethodParameter(
                    name = "url",
                    type = "String",
                    required = true,
                    description = "Deep link URL (e.g., 'myapp://path/to/page')"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val url = args.argString(0, "url")
                delegate!!.handleDeepLink(url)
            }
        )

        // ========== DATA Tier Methods ==========

        registerMethod(
            name = "clipboard.copy",
            tier = PluginTier.DATA,
            description = "Copy text to clipboard",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "text",
                    type = "String",
                    required = true,
                    description = "Text to copy"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val text = args.argString(0, "text")
                delegate!!.copyToClipboard(text)
            }
        )

        registerMethod(
            name = "clipboard.paste",
            tier = PluginTier.DATA,
            description = "Get clipboard content",
            returnType = "String?",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getClipboard()
            }
        )

        registerMethod(
            name = "clipboard.has",
            tier = PluginTier.DATA,
            description = "Check if clipboard has content",
            returnType = "Boolean",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.hasClipboardContent()
            }
        )

        registerMethod(
            name = "permission.check",
            tier = PluginTier.DATA,
            description = "Check if a permission is granted",
            returnType = "Boolean",
            parameters = listOf(
                MethodParameter(
                    name = "permission",
                    type = "String",
                    required = true,
                    description = "Permission identifier (e.g., 'camera', 'microphone')"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val permission = args.argString(0, "permission")
                delegate!!.checkPermission(permission)
            }
        )
    }

    /**
     * Ensure delegate is available.
     * @throws ModuleException if delegate is null (unsupported platform)
     */
    private fun requireDelegate() {
        if (delegate == null) {
            throw ModuleException(
                name,
                "",
                "App module not supported on this platform"
            )
        }
    }

    override suspend fun initialize() {
        // Delegate initialization happens at platform level
    }

    override suspend fun dispose() {
        // Cleanup happens at platform level
        delegate?.clearAllNotifications()
    }
}
