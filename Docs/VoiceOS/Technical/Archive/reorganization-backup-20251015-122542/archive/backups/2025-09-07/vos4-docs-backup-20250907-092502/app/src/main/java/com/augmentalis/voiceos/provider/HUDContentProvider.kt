/**
 * HUDContentProvider.kt
 * Path: app/src/main/java/com/augmentalis/voiceos/provider/HUDContentProvider.kt
 * 
 * Created: 2025-01-23
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: System-wide HUD ContentProvider for data sharing between apps
 * Enables secure data exchange and HUD element management
 */

package com.augmentalis.voiceos.provider

import com.augmentalis.voiceui.hud.HUDSystem

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.augmentalis.voiceui.hud.HUDRenderer
import org.json.JSONArray
import org.json.JSONObject

/**
 * ContentProvider for HUD system data and operations
 * Allows apps to query HUD state and submit elements
 */
class HUDContentProvider : ContentProvider() {
    
    companion object {
        private const val TAG = "HUDContentProvider"
        
        // Authority for the content provider (system-wide)
        const val AUTHORITY = "com.augmentalis.voiceos.hud.provider"
        
        // Base URI
        val BASE_URI: Uri = Uri.parse("content://$AUTHORITY")
        
        // URI paths
        const val PATH_HUD_ELEMENTS = "elements"
        const val PATH_HUD_STATUS = "status"
        const val PATH_HUD_CONFIG = "config"
        const val PATH_NOTIFICATIONS = "notifications"
        const val PATH_VOICE_COMMANDS = "voice_commands"
        const val PATH_DATA_VISUALIZATIONS = "visualizations"
        const val PATH_GAZE_TARGETS = "gaze_targets"
        const val PATH_ACCESSIBILITY = "accessibility"
        
        // URI codes
        private const val CODE_HUD_ELEMENTS = 1
        private const val CODE_HUD_ELEMENT_ID = 2
        private const val CODE_HUD_STATUS = 3
        private const val CODE_HUD_CONFIG = 4
        private const val CODE_NOTIFICATIONS = 5
        private const val CODE_VOICE_COMMANDS = 6
        private const val CODE_DATA_VISUALIZATIONS = 7
        private const val CODE_GAZE_TARGETS = 8
        private const val CODE_ACCESSIBILITY = 9
        
        // Content URIs
        val CONTENT_URI_ELEMENTS: Uri = Uri.withAppendedPath(BASE_URI, PATH_HUD_ELEMENTS)
        val CONTENT_URI_STATUS: Uri = Uri.withAppendedPath(BASE_URI, PATH_HUD_STATUS)
        val CONTENT_URI_CONFIG: Uri = Uri.withAppendedPath(BASE_URI, PATH_HUD_CONFIG)
        val CONTENT_URI_NOTIFICATIONS: Uri = Uri.withAppendedPath(BASE_URI, PATH_NOTIFICATIONS)
        val CONTENT_URI_VOICE_COMMANDS: Uri = Uri.withAppendedPath(BASE_URI, PATH_VOICE_COMMANDS)
        val CONTENT_URI_VISUALIZATIONS: Uri = Uri.withAppendedPath(BASE_URI, PATH_DATA_VISUALIZATIONS)
        val CONTENT_URI_GAZE_TARGETS: Uri = Uri.withAppendedPath(BASE_URI, PATH_GAZE_TARGETS)
        val CONTENT_URI_ACCESSIBILITY: Uri = Uri.withAppendedPath(BASE_URI, PATH_ACCESSIBILITY)
        
        // MIME types
        const val TYPE_HUD_ELEMENTS = "vnd.android.cursor.dir/vnd.augmentalis.hud.elements"
        const val TYPE_HUD_ELEMENT = "vnd.android.cursor.item/vnd.augmentalis.hud.element"
        const val TYPE_HUD_STATUS = "vnd.android.cursor.item/vnd.augmentalis.hud.status"
        const val TYPE_HUD_CONFIG = "vnd.android.cursor.item/vnd.augmentalis.hud.config"
        
        // Column names
        const val COLUMN_ID = "_id"
        const val COLUMN_ELEMENT_ID = "element_id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_POSITION_X = "position_x"
        const val COLUMN_POSITION_Y = "position_y"
        const val COLUMN_POSITION_Z = "position_z"
        const val COLUMN_DATA = "data"
        const val COLUMN_SCALE = "scale"
        const val COLUMN_VISIBLE = "visible"
        const val COLUMN_PRIORITY = "priority"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_STATUS = "status"
        const val COLUMN_FPS = "fps"
        const val COLUMN_MODE = "mode"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_CONFIDENCE = "confidence"
        const val COLUMN_CATEGORY = "category"
        
        // Permission constants (system-wide)
        const val PERMISSION_READ = "com.augmentalis.voiceos.permission.READ_HUD"
        const val PERMISSION_WRITE = "com.augmentalis.voiceos.permission.WRITE_HUD"
    }
    
    private lateinit var uriMatcher: UriMatcher
    private var hudSystem: HUDSystem? = null
    private var hudRenderer: HUDRenderer? = null
    
    // Temporary storage for HUD elements
    private val hudElements = mutableMapOf<String, HUDElementData>()
    
    override fun onCreate(): Boolean {
        Log.d(TAG, "HUD ContentProvider created")
        
        // Initialize URI matcher
        uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_HUD_ELEMENTS, CODE_HUD_ELEMENTS)
            addURI(AUTHORITY, "$PATH_HUD_ELEMENTS/#", CODE_HUD_ELEMENT_ID)
            addURI(AUTHORITY, PATH_HUD_STATUS, CODE_HUD_STATUS)
            addURI(AUTHORITY, PATH_HUD_CONFIG, CODE_HUD_CONFIG)
            addURI(AUTHORITY, PATH_NOTIFICATIONS, CODE_NOTIFICATIONS)
            addURI(AUTHORITY, PATH_VOICE_COMMANDS, CODE_VOICE_COMMANDS)
            addURI(AUTHORITY, PATH_DATA_VISUALIZATIONS, CODE_DATA_VISUALIZATIONS)
            addURI(AUTHORITY, PATH_GAZE_TARGETS, CODE_GAZE_TARGETS)
            addURI(AUTHORITY, PATH_ACCESSIBILITY, CODE_ACCESSIBILITY)
        }
        
        return true
    }
    
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "Query: $uri")
        
        // Check read permission
        if (!hasReadPermission()) {
            Log.e(TAG, "Read permission denied")
            return null
        }
        
        return when (uriMatcher.match(uri)) {
            CODE_HUD_ELEMENTS -> queryHUDElements(projection)
            CODE_HUD_ELEMENT_ID -> queryHUDElement(uri.lastPathSegment, projection)
            CODE_HUD_STATUS -> queryHUDStatus(projection)
            CODE_HUD_CONFIG -> queryHUDConfig(projection)
            CODE_NOTIFICATIONS -> queryNotifications(projection)
            CODE_VOICE_COMMANDS -> queryVoiceCommands(projection)
            CODE_DATA_VISUALIZATIONS -> queryDataVisualizations(projection)
            CODE_GAZE_TARGETS -> queryGazeTargets(projection)
            CODE_ACCESSIBILITY -> queryAccessibilitySettings(projection)
            else -> null
        }
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.d(TAG, "Insert: $uri")
        
        // Check write permission
        if (!hasWritePermission()) {
            Log.e(TAG, "Write permission denied")
            return null
        }
        
        values ?: return null
        
        return when (uriMatcher.match(uri)) {
            CODE_HUD_ELEMENTS -> insertHUDElement(values)
            CODE_NOTIFICATIONS -> insertNotification(values)
            CODE_VOICE_COMMANDS -> insertVoiceCommand(values)
            CODE_DATA_VISUALIZATIONS -> insertDataVisualization(values)
            else -> null
        }
    }
    
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        Log.d(TAG, "Update: $uri")
        
        // Check write permission
        if (!hasWritePermission()) {
            Log.e(TAG, "Write permission denied")
            return 0
        }
        
        values ?: return 0
        
        return when (uriMatcher.match(uri)) {
            CODE_HUD_ELEMENT_ID -> updateHUDElement(uri.lastPathSegment, values)
            CODE_HUD_CONFIG -> updateHUDConfig(values)
            CODE_ACCESSIBILITY -> updateAccessibilitySettings(values)
            else -> 0
        }
    }
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "Delete: $uri")
        
        // Check write permission
        if (!hasWritePermission()) {
            Log.e(TAG, "Write permission denied")
            return 0
        }
        
        return when (uriMatcher.match(uri)) {
            CODE_HUD_ELEMENT_ID -> deleteHUDElement(uri.lastPathSegment)
            CODE_HUD_ELEMENTS -> deleteAllHUDElements()
            else -> 0
        }
    }
    
    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_HUD_ELEMENTS -> TYPE_HUD_ELEMENTS
            CODE_HUD_ELEMENT_ID -> TYPE_HUD_ELEMENT
            CODE_HUD_STATUS -> TYPE_HUD_STATUS
            CODE_HUD_CONFIG -> TYPE_HUD_CONFIG
            else -> null
        }
    }
    
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        Log.d(TAG, "Call method: $method")
        
        return when (method) {
            "showHUD" -> {
                hudSystem?.setVisible(true)
                Bundle().apply { putBoolean("success", true) }
            }
            "hideHUD" -> {
                hudSystem?.setVisible(false)
                Bundle().apply { putBoolean("success", true) }
            }
            "toggleHUD" -> {
                hudSystem?.toggleVisibility()
                Bundle().apply { putBoolean("success", true) }
            }
            "setHUDMode" -> {
                @Suppress("UNUSED_VARIABLE")
                val mode = arg ?: "STANDARD"
                // Set HUD mode via renderer
                Bundle().apply { putBoolean("success", true) }
            }
            "enableGazeTracking" -> {
                // Enable gaze tracking
                Bundle().apply { putBoolean("success", true) }
            }
            "disableGazeTracking" -> {
                // Disable gaze tracking
                Bundle().apply { putBoolean("success", true) }
            }
            "getCurrentFPS" -> {
                val fps = hudSystem?.getCurrentFPS() ?: 0f
                Bundle().apply { putFloat("fps", fps) }
            }
            else -> super.call(method, arg, extras)
        }
    }
    
    // Query methods
    private fun queryHUDElements(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf(
            COLUMN_ID, COLUMN_ELEMENT_ID, COLUMN_TYPE, 
            COLUMN_POSITION_X, COLUMN_POSITION_Y, COLUMN_POSITION_Z,
            COLUMN_DATA, COLUMN_SCALE, COLUMN_VISIBLE, COLUMN_PRIORITY
        ))
        
        hudElements.values.forEachIndexed { index, element ->
            cursor.addRow(arrayOf(
                index,
                element.id,
                element.type,
                element.positionX,
                element.positionY,
                element.positionZ,
                element.data,
                element.scale,
                element.visible,
                element.priority
            ))
        }
        
        return cursor
    }
    
    private fun queryHUDElement(elementId: String?, projection: Array<out String>?): Cursor? {
        elementId ?: return null
        val element = hudElements[elementId] ?: return null
        
        val cursor = MatrixCursor(projection ?: arrayOf(
            COLUMN_ID, COLUMN_ELEMENT_ID, COLUMN_TYPE,
            COLUMN_POSITION_X, COLUMN_POSITION_Y, COLUMN_POSITION_Z,
            COLUMN_DATA, COLUMN_SCALE, COLUMN_VISIBLE, COLUMN_PRIORITY
        ))
        
        cursor.addRow(arrayOf(
            0,
            element.id,
            element.type,
            element.positionX,
            element.positionY,
            element.positionZ,
            element.data,
            element.scale,
            element.visible,
            element.priority
        ))
        
        return cursor
    }
    
    private fun queryHUDStatus(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf(
            COLUMN_STATUS, COLUMN_FPS, COLUMN_MODE
        ))
        
        val isVisible = hudSystem?.isVisible() ?: false
        val fps = hudSystem?.getCurrentFPS() ?: 0f
        
        cursor.addRow(arrayOf(
            if (isVisible) "visible" else "hidden",
            fps,
            "STANDARD"
        ))
        
        return cursor
    }
    
    private fun queryHUDConfig(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf(
            "render_mode", "target_fps", "vibrancy_enabled", "glass_morphism_enabled"
        ))
        
        cursor.addRow(arrayOf(
            "SPATIAL_AR",
            90,
            true,
            true
        ))
        
        return cursor
    }
    
    private fun queryNotifications(projection: Array<out String>?): Cursor {
        // Return recent notifications
        return MatrixCursor(projection ?: arrayOf(
            COLUMN_ID, COLUMN_MESSAGE, COLUMN_PRIORITY, COLUMN_TIMESTAMP
        ))
    }
    
    private fun queryVoiceCommands(projection: Array<out String>?): Cursor {
        // Return available voice commands
        return MatrixCursor(projection ?: arrayOf(
            COLUMN_ID, COLUMN_MESSAGE, COLUMN_CONFIDENCE, COLUMN_CATEGORY
        ))
    }
    
    private fun queryDataVisualizations(projection: Array<out String>?): Cursor {
        // Return active data visualizations
        return MatrixCursor(projection ?: arrayOf(
            COLUMN_ID, COLUMN_TYPE, COLUMN_DATA
        ))
    }
    
    private fun queryGazeTargets(projection: Array<out String>?): Cursor {
        // Return current gaze targets
        return MatrixCursor(projection ?: arrayOf(
            COLUMN_ID, COLUMN_POSITION_X, COLUMN_POSITION_Y, COLUMN_CONFIDENCE
        ))
    }
    
    private fun queryAccessibilitySettings(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf(
            "high_contrast", "text_scale", "voice_speed", "live_transcription"
        ))
        
        cursor.addRow(arrayOf(
            false,
            1.0f,
            1.0f,
            false
        ))
        
        return cursor
    }
    
    // Insert methods
    private fun insertHUDElement(values: ContentValues): Uri? {
        val elementId = values.getAsString(COLUMN_ELEMENT_ID) ?: return null
        val element = HUDElementData(
            id = elementId,
            type = values.getAsString(COLUMN_TYPE) ?: "NOTIFICATION",
            positionX = values.getAsFloat(COLUMN_POSITION_X) ?: 0f,
            positionY = values.getAsFloat(COLUMN_POSITION_Y) ?: 0f,
            positionZ = values.getAsFloat(COLUMN_POSITION_Z) ?: -2f,
            data = values.getAsString(COLUMN_DATA) ?: "{}",
            scale = values.getAsFloat(COLUMN_SCALE) ?: 1f,
            visible = values.getAsBoolean(COLUMN_VISIBLE) ?: true,
            priority = values.getAsInteger(COLUMN_PRIORITY) ?: 0
        )
        
        hudElements[elementId] = element
        
        // Notify HUD system
        notifyHUDSystem(element)
        
        return Uri.withAppendedPath(CONTENT_URI_ELEMENTS, elementId)
    }
    
    private fun insertNotification(values: ContentValues): Uri? {
        val message = values.getAsString(COLUMN_MESSAGE) ?: return null
        @Suppress("UNUSED_VARIABLE")
        val duration = values.getAsInteger("duration") ?: 3000
        @Suppress("UNUSED_VARIABLE")
        val position = values.getAsString("position") ?: "CENTER"
        @Suppress("UNUSED_VARIABLE")
        val priority = values.getAsString(COLUMN_PRIORITY) ?: "NORMAL"
        
        // Show notification via HUD system (simplified implementation)
        hudSystem?.showNotification(message)
        
        return Uri.withAppendedPath(CONTENT_URI_NOTIFICATIONS, System.currentTimeMillis().toString())
    }
    
    private fun insertVoiceCommand(@Suppress("UNUSED_PARAMETER") values: ContentValues): Uri? {
        // Handle voice command insertion
        return null
    }
    
    private fun insertDataVisualization(@Suppress("UNUSED_PARAMETER") values: ContentValues): Uri? {
        // Handle data visualization insertion
        return null
    }
    
    // Update methods
    private fun updateHUDElement(elementId: String?, values: ContentValues): Int {
        elementId ?: return 0
        val element = hudElements[elementId] ?: return 0
        
        // Update element properties
        values.getAsFloat(COLUMN_POSITION_X)?.let { element.positionX = it }
        values.getAsFloat(COLUMN_POSITION_Y)?.let { element.positionY = it }
        values.getAsFloat(COLUMN_POSITION_Z)?.let { element.positionZ = it }
        values.getAsString(COLUMN_DATA)?.let { element.data = it }
        values.getAsFloat(COLUMN_SCALE)?.let { element.scale = it }
        values.getAsBoolean(COLUMN_VISIBLE)?.let { element.visible = it }
        values.getAsInteger(COLUMN_PRIORITY)?.let { element.priority = it }
        
        // Notify HUD system of update
        notifyHUDSystem(element)
        
        return 1
    }
    
    private fun updateHUDConfig(@Suppress("UNUSED_PARAMETER") values: ContentValues): Int {
        // Update HUD configuration
        return 1
    }
    
    private fun updateAccessibilitySettings(@Suppress("UNUSED_PARAMETER") values: ContentValues): Int {
        // Update accessibility settings
        return 1
    }
    
    // Delete methods
    private fun deleteHUDElement(elementId: String?): Int {
        elementId ?: return 0
        return if (hudElements.remove(elementId) != null) {
            hudSystem?.removeElement(elementId)
            1
        } else {
            0
        }
    }
    
    private fun deleteAllHUDElements(): Int {
        val count = hudElements.size
        hudElements.clear()
        // Clear all HUD elements
        return count
    }
    
    // Helper methods
    private fun hasReadPermission(): Boolean {
        return context?.checkCallingOrSelfPermission(PERMISSION_READ) == 
               android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasWritePermission(): Boolean {
        return context?.checkCallingOrSelfPermission(PERMISSION_WRITE) == 
               android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    private fun notifyHUDSystem(element: HUDElementData) {
        // Notify HUD system about element changes
        Log.d(TAG, "Notifying HUD system about element: ${element.id}")
    }
    
    // Data class for HUD elements
    data class HUDElementData(
        val id: String,
        val type: String,
        var positionX: Float,
        var positionY: Float,
        var positionZ: Float,
        var data: String,
        var scale: Float,
        var visible: Boolean,
        var priority: Int
    )
}