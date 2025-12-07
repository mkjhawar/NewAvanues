/**
 * VoiceUIProvider.kt - Content provider for VoiceUI data access
 * Provides structured access to themes, gestures, windows, etc.
 */

package com.augmentalis.voiceui.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.augmentalis.voiceui.VoiceUIModule
import org.json.JSONArray
import org.json.JSONObject

/**
 * Content Provider for VoiceUI data
 * Direct access to module data - no abstraction (VOS4)
 */
class VoiceUIProvider : ContentProvider() {
    
    companion object {
        private const val TAG = "VoiceUIProvider"
        private const val AUTHORITY = "com.augmentalis.voiceui.provider"
        
        // URI codes
        private const val THEMES = 1
        private const val THEMES_ID = 2
        private const val GESTURES = 3
        private const val GESTURES_ID = 4
        private const val WINDOWS = 5
        private const val WINDOWS_ID = 6
        private const val NOTIFICATIONS = 7
        private const val NOTIFICATIONS_ID = 8
        private const val COMMANDS = 9
        private const val COMMANDS_ID = 10
        private const val SETTINGS = 11
        private const val SETTINGS_ID = 12
        
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "themes", THEMES)
            addURI(AUTHORITY, "themes/*", THEMES_ID)
            addURI(AUTHORITY, "gestures", GESTURES)
            addURI(AUTHORITY, "gestures/*", GESTURES_ID)
            addURI(AUTHORITY, "windows", WINDOWS)
            addURI(AUTHORITY, "windows/*", WINDOWS_ID)
            addURI(AUTHORITY, "notifications", NOTIFICATIONS)
            addURI(AUTHORITY, "notifications/*", NOTIFICATIONS_ID)
            addURI(AUTHORITY, "commands", COMMANDS)
            addURI(AUTHORITY, "commands/*", COMMANDS_ID)
            addURI(AUTHORITY, "settings", SETTINGS)
            addURI(AUTHORITY, "settings/*", SETTINGS_ID)
        }
        
        // Base content URI
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")
        
        // Individual URIs
        val THEMES_URI: Uri = Uri.withAppendedPath(CONTENT_URI, "themes")
        val GESTURES_URI: Uri = Uri.withAppendedPath(CONTENT_URI, "gestures")
        val WINDOWS_URI: Uri = Uri.withAppendedPath(CONTENT_URI, "windows")
        val NOTIFICATIONS_URI: Uri = Uri.withAppendedPath(CONTENT_URI, "notifications")
        val COMMANDS_URI: Uri = Uri.withAppendedPath(CONTENT_URI, "commands")
        val SETTINGS_URI: Uri = Uri.withAppendedPath(CONTENT_URI, "settings")
    }
    
    private lateinit var voiceUI: VoiceUIModule
    
    override fun onCreate(): Boolean {
        context?.let {
            voiceUI = VoiceUIModule.getInstance(it)
            return true
        }
        return false
    }
    
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "Query: $uri")
        
        return when (uriMatcher.match(uri)) {
            THEMES -> queryThemes(projection)
            THEMES_ID -> queryTheme(uri.lastPathSegment!!, projection)
            GESTURES -> queryGestures(projection)
            WINDOWS -> queryWindows(projection, selection, selectionArgs)
            NOTIFICATIONS -> queryNotifications(projection)
            COMMANDS -> queryCommands(projection)
            SETTINGS -> querySettings(projection)
            else -> null
        }
    }
    
    private fun queryThemes(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf("name", "description", "primary_color", "is_active"))
        
        val themes = voiceUI.themeEngine.getAvailableThemes()
        val currentTheme = voiceUI.themeEngine.getCurrentTheme()
        
        themes.forEach { theme ->
            cursor.addRow(arrayOf(
                theme.name,
                theme.description,
                theme.primaryColor,
                theme.name == currentTheme
            ))
        }
        
        return cursor
    }
    
    private fun queryTheme(themeName: String, projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf("name", "config"))
        
        val theme = voiceUI.themeEngine.getTheme(themeName)
        theme?.let {
            cursor.addRow(arrayOf(
                it.name,
                it.toJson()
            ))
        }
        
        return cursor
    }
    
    private fun queryGestures(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf("type", "enabled", "sensitivity"))
        
        val gestures = voiceUI.gestureManager.getGestureConfigs()
        gestures.forEach { gesture ->
            cursor.addRow(arrayOf(
                gesture.type,
                gesture.enabled,
                gesture.sensitivity
            ))
        }
        
        return cursor
    }
    
    private fun queryWindows(projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf("window_id", "title", "x", "y", "z", "width", "height", "visible"))
        
        val windows = voiceUI.windowManager.getActiveWindows()
        
        // Apply selection filter if provided
        val filteredWindows = if (selection == "visible = ?") {
            val visibleFilter = selectionArgs?.firstOrNull() == "1"
            windows.filter { it.visible == visibleFilter }
        } else {
            windows
        }
        
        filteredWindows.forEach { window ->
            cursor.addRow(arrayOf(
                window.id,
                window.title,
                window.x,
                window.y,
                window.z,
                window.width,
                window.height,
                window.visible
            ))
        }
        
        return cursor
    }
    
    private fun queryNotifications(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf("notification_id", "title", "message", "timestamp"))
        
        val notifications = voiceUI.notificationSystem.getActiveNotifications()
        notifications.forEach { notification ->
            cursor.addRow(arrayOf(
                notification.id,
                notification.title,
                notification.message,
                notification.timestamp
            ))
        }
        
        return cursor
    }
    
    private fun queryCommands(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf("command", "action", "language"))
        
        val commands = voiceUI.voiceCommandSystem.getRegisteredCommands()
        commands.forEach { command ->
            cursor.addRow(arrayOf(
                command.text,
                command.action,
                command.language
            ))
        }
        
        return cursor
    }
    
    private fun querySettings(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf("key", "value", "type"))
        
        // Add module settings
        cursor.addRow(arrayOf("theme", voiceUI.themeEngine.getCurrentTheme(), "string"))
        cursor.addRow(arrayOf("gesture_enabled", voiceUI.gestureManager.isEnabled(), "boolean"))
        cursor.addRow(arrayOf("hud_visible", voiceUI.hudSystem.isVisible(), "boolean"))
        cursor.addRow(arrayOf("voice_enabled", voiceUI.voiceCommandSystem.isEnabled(), "boolean"))
        cursor.addRow(arrayOf("module_version", VoiceUIModule.MODULE_VERSION, "string"))
        
        return cursor
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.d(TAG, "Insert: $uri")
        values ?: return null
        
        return when (uriMatcher.match(uri)) {
            THEMES -> insertTheme(values)
            COMMANDS -> insertCommand(values)
            else -> null
        }
    }
    
    private fun insertTheme(values: ContentValues): Uri? {
        val themeName = values.getAsString("name") ?: return null
        val themeConfig = values.getAsString("config") ?: return null
        
        voiceUI.themeEngine.registerCustomTheme(themeName, themeConfig)
        return Uri.withAppendedPath(THEMES_URI, themeName)
    }
    
    private fun insertCommand(values: ContentValues): Uri? {
        val command = values.getAsString("command") ?: return null
        val action = values.getAsString("action") ?: return null
        val language = values.getAsString("language") ?: "en-US"
        
        voiceUI.voiceCommandSystem.registerCommand(command, action, language)
        return Uri.withAppendedPath(COMMANDS_URI, command)
    }
    
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "Update: $uri")
        values ?: return 0
        
        return when (uriMatcher.match(uri)) {
            THEMES_ID -> updateTheme(uri.lastPathSegment!!, values)
            WINDOWS_ID -> updateWindow(uri.lastPathSegment!!, values)
            SETTINGS_ID -> updateSetting(uri.lastPathSegment!!, values)
            else -> 0
        }
    }
    
    private fun updateTheme(themeName: String, values: ContentValues): Int {
        if (values.containsKey("active") && values.getAsBoolean("active")) {
            voiceUI.themeEngine.setTheme(themeName)
            return 1
        }
        return 0
    }
    
    private fun updateWindow(windowId: String, values: ContentValues): Int {
        var updated = 0
        
        if (values.containsKey("x") || values.containsKey("y") || values.containsKey("z")) {
            val x = values.getAsFloat("x") ?: 0f
            val y = values.getAsFloat("y") ?: 0f
            val z = values.getAsFloat("z") ?: -2f
            voiceUI.windowManager.moveWindow(windowId, x, y, z)
            updated++
        }
        
        if (values.containsKey("width") || values.containsKey("height")) {
            val width = values.getAsInteger("width") ?: 800
            val height = values.getAsInteger("height") ?: 600
            voiceUI.windowManager.resizeWindow(windowId, width, height)
            updated++
        }
        
        if (values.containsKey("visible")) {
            val visible = values.getAsBoolean("visible")
            if (visible) {
                voiceUI.windowManager.showWindow(windowId)
            } else {
                voiceUI.windowManager.hideWindow(windowId)
            }
            updated++
        }
        
        return updated
    }
    
    private fun updateSetting(key: String, values: ContentValues): Int {
        when (key) {
            "theme" -> {
                values.getAsString("value")?.let {
                    voiceUI.themeEngine.setTheme(it)
                    return 1
                }
            }
            "gesture_enabled" -> {
                values.getAsBoolean("value")?.let {
                    voiceUI.gestureManager.setEnabled(it)
                    return 1
                }
            }
            "hud_visible" -> {
                values.getAsBoolean("value")?.let {
                    voiceUI.hudSystem.toggleVisibility(it, 300)  // Default fade duration
                    return 1
                }
            }
            "voice_enabled" -> {
                values.getAsBoolean("value")?.let {
                    voiceUI.voiceCommandSystem.setEnabled(it)
                    return 1
                }
            }
        }
        return 0
    }
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "Delete: $uri")
        
        return when (uriMatcher.match(uri)) {
            WINDOWS_ID -> deleteWindow(uri.lastPathSegment!!)
            NOTIFICATIONS_ID -> deleteNotification(uri.lastPathSegment!!)
            else -> 0
        }
    }
    
    private fun deleteWindow(windowId: String): Int {
        voiceUI.windowManager.destroyWindow(windowId)
        return 1
    }
    
    private fun deleteNotification(notificationId: String): Int {
        voiceUI.notificationSystem.clear(notificationId)
        return 1
    }
    
    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            THEMES -> "vnd.android.cursor.dir/vnd.voiceui.theme"
            THEMES_ID -> "vnd.android.cursor.item/vnd.voiceui.theme"
            GESTURES -> "vnd.android.cursor.dir/vnd.voiceui.gesture"
            WINDOWS -> "vnd.android.cursor.dir/vnd.voiceui.window"
            WINDOWS_ID -> "vnd.android.cursor.item/vnd.voiceui.window"
            NOTIFICATIONS -> "vnd.android.cursor.dir/vnd.voiceui.notification"
            COMMANDS -> "vnd.android.cursor.dir/vnd.voiceui.command"
            SETTINGS -> "vnd.android.cursor.dir/vnd.voiceui.setting"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}