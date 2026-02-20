/**
 * CommandDefinitions.kt - Command definitions
 * Simplified version for direct implementation
 */

package com.augmentalis.voiceoscore.commandmanager.definitions

import com.augmentalis.voiceoscore.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Command definitions manager
 * Direct implementation - no unnecessary abstractions
 */
class CommandDefinitions {
    
    companion object {
        private const val TAG = "CommandDefinitions"
    }
    
    // Built-in command definitions
    private val builtInDefinitions = mutableMapOf<String, CommandDefinition>()
    
    // Custom command definitions
    private val customDefinitions = ConcurrentHashMap<String, CommandDefinition>()
    
    // Current language
    private var currentLanguage = "en"
    
    /**
     * Load all built-in command definitions
     */
    fun loadBuiltInCommands() {
        loadNavigationCommands()
        loadCursorCommands()
        loadScrollCommands()
        loadVolumeCommands()
        loadDictationCommands()
        loadSystemCommands()
        loadAppCommands()
        loadTextCommands()
        loadLearnAppCommands()

        android.util.Log.d(TAG, "Loaded ${builtInDefinitions.size} built-in command definitions")
    }
    
    /**
     * Get all command definitions
     */
    fun getAllDefinitions(): List<CommandDefinition> {
        return (builtInDefinitions.values + customDefinitions.values).toList()
    }
    
    /**
     * Get contextual commands based on context
     */
    fun getContextualCommands(context: CommandContext): List<CommandDefinition> {
        return getAllDefinitions().filter { definition ->
            definition.requiredContext.isEmpty() || 
            definition.requiredContext.any { req ->
                when (req) {
                    "text_input" -> context.viewId?.contains("EditText") == true
                    "browser" -> context.packageName?.contains("browser") == true
                    "media" -> context.packageName?.contains("media") == true
                    else -> false
                }
            }
        }
    }
    
    /**
     * Add custom command definition
     */
    fun addCustomDefinition(definition: CommandDefinition) {
        customDefinitions[definition.id] = definition
    }
    
    /**
     * Remove custom command definition
     */
    fun removeCustomDefinition(commandId: String) {
        customDefinitions.remove(commandId)
    }
    
    /**
     * Get command count
     */
    fun getCommandCount(): Int = builtInDefinitions.size + customDefinitions.size
    
    // Navigation Commands
    private fun loadNavigationCommands() {
        builtInDefinitions["nav_back"] = CommandDefinition(
            id = "nav_back",
            name = "Go Back",
            description = "Navigate back",
            category = "NAVIGATION",
            patterns = listOf("go back", "back", "navigate back", "previous", "return")
        )
        
        builtInDefinitions["nav_home"] = CommandDefinition(
            id = "nav_home",
            name = "Go Home",
            description = "Navigate to home screen",
            category = "NAVIGATION",
            patterns = listOf("go home", "home", "home screen", "main screen")
        )
        
        builtInDefinitions["nav_recent_apps"] = CommandDefinition(
            id = "nav_recent_apps",
            name = "Recent Apps",
            description = "Open recent applications",
            category = "NAVIGATION",
            patterns = listOf("recent apps", "recents", "app switcher")
        )
        
        builtInDefinitions["nav_notifications"] = CommandDefinition(
            id = "nav_notifications",
            name = "Notifications",
            description = "Open notification panel",
            category = "NAVIGATION",
            patterns = listOf("notifications", "notification panel", "open notifications")
        )
    }
    
    // Cursor Commands
    private fun loadCursorCommands() {
        builtInDefinitions["cursor_click"] = CommandDefinition(
            id = "cursor_click",
            name = "Click",
            description = "Perform click action",
            category = "INPUT",
            patterns = listOf("click", "tap", "press", "select"),
            parameters = listOf(
                CommandParameter("target", ParameterType.STRING, required = false),
                CommandParameter("x", ParameterType.NUMBER, required = false),
                CommandParameter("y", ParameterType.NUMBER, required = false)
            )
        )
        
        builtInDefinitions["cursor_double_click"] = CommandDefinition(
            id = "cursor_double_click",
            name = "Double Click",
            description = "Perform double click action",
            category = "INPUT",
            patterns = listOf("double click", "double tap", "double press"),
            parameters = listOf(
                CommandParameter("target", ParameterType.STRING, required = false),
                CommandParameter("x", ParameterType.NUMBER, required = false),
                CommandParameter("y", ParameterType.NUMBER, required = false)
            )
        )
        
        builtInDefinitions["cursor_long_press"] = CommandDefinition(
            id = "cursor_long_press",
            name = "Long Press",
            description = "Perform long press action",
            category = "INPUT",
            patterns = listOf("long press", "long click", "hold", "press and hold"),
            parameters = listOf(
                CommandParameter("target", ParameterType.STRING, required = false),
                CommandParameter("x", ParameterType.NUMBER, required = false),
                CommandParameter("y", ParameterType.NUMBER, required = false)
            )
        )
    }
    
    // Scroll Commands
    private fun loadScrollCommands() {
        builtInDefinitions["scroll_up"] = CommandDefinition(
            id = "scroll_up",
            name = "Scroll Up",
            description = "Scroll up",
            category = "INPUT",
            patterns = listOf("scroll up", "move up", "go up"),
            parameters = listOf(
                CommandParameter("distance", ParameterType.NUMBER, required = false)
            )
        )
        
        builtInDefinitions["scroll_down"] = CommandDefinition(
            id = "scroll_down",
            name = "Scroll Down",
            description = "Scroll down",
            category = "INPUT",
            patterns = listOf("scroll down", "move down", "go down"),
            parameters = listOf(
                CommandParameter("distance", ParameterType.NUMBER, required = false)
            )
        )
        
        builtInDefinitions["scroll_left"] = CommandDefinition(
            id = "scroll_left",
            name = "Scroll Left",
            description = "Scroll left",
            category = "INPUT",
            patterns = listOf("scroll left", "move left", "go left")
        )
        
        builtInDefinitions["scroll_right"] = CommandDefinition(
            id = "scroll_right",
            name = "Scroll Right",
            description = "Scroll right",
            category = "INPUT",
            patterns = listOf("scroll right", "move right", "go right")
        )
    }
    
    // Volume Commands
    private fun loadVolumeCommands() {
        builtInDefinitions["volume_up"] = CommandDefinition(
            id = "volume_up",
            name = "Volume Up",
            description = "Increase volume",
            category = "MEDIA",
            patterns = listOf("volume up", "increase volume", "louder", "turn up"),
            parameters = listOf(
                CommandParameter("steps", ParameterType.NUMBER, required = false),
                CommandParameter("stream", ParameterType.STRING, required = false)
            )
        )
        
        builtInDefinitions["volume_down"] = CommandDefinition(
            id = "volume_down",
            name = "Volume Down",
            description = "Decrease volume",
            category = "MEDIA",
            patterns = listOf("volume down", "decrease volume", "quieter", "turn down")
        )
        
        builtInDefinitions["mute"] = CommandDefinition(
            id = "mute",
            name = "Mute",
            description = "Mute audio",
            category = "MEDIA",
            patterns = listOf("mute", "silence", "turn off sound", "quiet")
        )
        
        builtInDefinitions["unmute"] = CommandDefinition(
            id = "unmute",
            name = "Unmute",
            description = "Unmute audio",
            category = "MEDIA",
            patterns = listOf("unmute", "turn on sound", "restore audio")
        )
    }
    
    // Dictation Commands
    private fun loadDictationCommands() {
        builtInDefinitions["dictation_start"] = CommandDefinition(
            id = "dictation_start",
            name = "Start Dictation",
            description = "Start dictation mode",
            category = "INPUT",
            patterns = listOf("start dictation", "begin dictation", "dictate"),
            requiredContext = setOf("text_input")
        )
        
        builtInDefinitions["dictation_end"] = CommandDefinition(
            id = "dictation_end",
            name = "End Dictation",
            description = "End dictation mode",
            category = "INPUT",
            patterns = listOf("end dictation", "stop dictation", "finish dictation")
        )
        
        builtInDefinitions["type_text"] = CommandDefinition(
            id = "type_text",
            name = "Type Text",
            description = "Type text into field",
            category = "INPUT",
            patterns = listOf("type", "write", "enter text"),
            parameters = listOf(
                CommandParameter("text", ParameterType.STRING, required = true)
            ),
            requiredContext = setOf("text_input")
        )
        
        builtInDefinitions["backspace"] = CommandDefinition(
            id = "backspace",
            name = "Backspace",
            description = "Delete character",
            category = "INPUT",
            patterns = listOf("backspace", "delete", "erase", "remove character"),
            parameters = listOf(
                CommandParameter("count", ParameterType.NUMBER, required = false, defaultValue = 1)
            )
        )
    }
    
    // System Commands
    private fun loadSystemCommands() {
        builtInDefinitions["wifi_toggle"] = CommandDefinition(
            id = "wifi_toggle",
            name = "Toggle WiFi",
            description = "Toggle WiFi on/off",
            category = "SYSTEM",
            patterns = listOf("toggle wifi", "wifi on off", "switch wifi")
        )
        
        builtInDefinitions["bluetooth_toggle"] = CommandDefinition(
            id = "bluetooth_toggle",
            name = "Toggle Bluetooth",
            description = "Toggle Bluetooth on/off",
            category = "SYSTEM",
            patterns = listOf("toggle bluetooth", "bluetooth on off", "switch bluetooth")
        )
        
        builtInDefinitions["open_settings"] = CommandDefinition(
            id = "open_settings",
            name = "Open Settings",
            description = "Open system settings",
            category = "SYSTEM",
            patterns = listOf("open settings", "settings", "system settings"),
            parameters = listOf(
                CommandParameter("category", ParameterType.STRING, required = false)
            )
        )
    }
    
    // App Commands
    private fun loadAppCommands() {
        builtInDefinitions["open_app"] = CommandDefinition(
            id = "open_app",
            name = "Open App",
            description = "Open application",
            category = "APP_CONTROL",
            patterns = listOf("open app", "launch app", "start app", "open"),
            parameters = listOf(
                CommandParameter("app", ParameterType.STRING, required = true)
            )
        )
        
        builtInDefinitions["close_app"] = CommandDefinition(
            id = "close_app",
            name = "Close App",
            description = "Close application",
            category = "APP_CONTROL",
            patterns = listOf("close app", "exit app", "quit app"),
            parameters = listOf(
                CommandParameter("app", ParameterType.STRING, required = false),
                CommandParameter("current", ParameterType.BOOLEAN, required = false, defaultValue = true)
            )
        )
    }
    
    // Text Commands
    private fun loadTextCommands() {
        builtInDefinitions["copy_text"] = CommandDefinition(
            id = "copy_text",
            name = "Copy",
            description = "Copy text",
            category = "INPUT",
            patterns = listOf("copy", "copy text", "copy selection"),
            parameters = listOf(
                CommandParameter("text", ParameterType.STRING, required = false)
            )
        )

        builtInDefinitions["paste_text"] = CommandDefinition(
            id = "paste_text",
            name = "Paste",
            description = "Paste text",
            category = "INPUT",
            patterns = listOf("paste", "paste text", "insert")
        )

        builtInDefinitions["select_all"] = CommandDefinition(
            id = "select_all",
            name = "Select All",
            description = "Select all text",
            category = "INPUT",
            patterns = listOf("select all", "select everything", "highlight all")
        )

        builtInDefinitions["cut_text"] = CommandDefinition(
            id = "cut_text",
            name = "Cut",
            description = "Cut text",
            category = "INPUT",
            patterns = listOf("cut", "cut text", "cut selection")
        )
    }

    // LearnApp Commands
    private fun loadLearnAppCommands() {
        builtInDefinitions["relearn_app"] = CommandDefinition(
            id = "relearn_app",
            name = "Relearn App",
            description = "Update AVIDs for an app without full re-exploration",
            category = "LEARNAPP",
            patterns = listOf(
                "relearn {app_name}",
                "relearn this app",
                "relearn current app"
            ),
            parameters = listOf(
                CommandParameter("app_name", ParameterType.STRING, required = false)
            )
        )
    }
}