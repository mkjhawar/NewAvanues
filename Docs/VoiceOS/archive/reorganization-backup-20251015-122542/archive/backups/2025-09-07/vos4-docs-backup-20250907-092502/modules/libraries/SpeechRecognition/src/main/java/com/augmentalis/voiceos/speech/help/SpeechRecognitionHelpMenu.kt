/**
 * SpeechRecognitionHelpMenu.kt
 * Path: /libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/help/SpeechRecognitionHelpMenu.kt
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Comprehensive help menu for Speech Recognition commands and settings
 * Module: SpeechRecognition
 * 
 * Changelog:
 * - v1.0.0 (2025-09-05): Initial creation with comprehensive speech commands help
 */

package com.augmentalis.voiceos.speech.help

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import java.util.*

/**
 * Speech Recognition Help Menu - Shows available speech commands and settings
 */
class SpeechRecognitionHelpMenu(private val context: Context) {
    
    private var helpDialog: AlertDialog? = null
    
    /**
     * Get all speech recognition command categories
     */
    fun getSpeechCommands(): List<SpeechCommandGroup> {
        return listOf(
            // Basic Voice Commands
            getSpeechControlCommands(),
            getDictationCommands(),
            getLanguageCommands(),
            getSettingsCommands(),
            getAdvancedCommands(),
            getSystemCommands(),
            getTroubleshootingCommands()
        )
    }
    
    /**
     * Speech control commands
     */
    private fun getSpeechControlCommands(): SpeechCommandGroup {
        return SpeechCommandGroup(
            title = "Speech Control",
            description = "Basic speech recognition control commands",
            commands = listOf(
                SpeechCommand(
                    "Start listening", 
                    "start listening", 
                    "begin voice", 
                    "Activate speech recognition"
                ),
                SpeechCommand(
                    "Stop listening", 
                    "stop listening", 
                    "pause voice", 
                    "Pause speech recognition"
                ),
                SpeechCommand(
                    "Resume listening", 
                    "resume", 
                    "continue voice", 
                    "Resume speech recognition"
                ),
                SpeechCommand(
                    "Cancel command", 
                    "cancel", 
                    "never mind", 
                    "Cancel current voice command"
                ),
                SpeechCommand(
                    "Repeat last", 
                    "repeat", 
                    "say again", 
                    "Repeat last recognized command"
                ),
                SpeechCommand(
                    "Clear buffer", 
                    "clear", 
                    "reset", 
                    "Clear speech recognition buffer"
                )
            )
        )
    }
    
    /**
     * Dictation commands
     */
    private fun getDictationCommands(): SpeechCommandGroup {
        return SpeechCommandGroup(
            title = "Dictation Commands",
            description = "Text input and editing via voice",
            commands = listOf(
                SpeechCommand(
                    "Start dictation", 
                    "start dictation", 
                    "begin typing", 
                    "Start voice-to-text input"
                ),
                SpeechCommand(
                    "Stop dictation", 
                    "stop dictation", 
                    "end typing", 
                    "Stop voice-to-text input"
                ),
                SpeechCommand(
                    "New line", 
                    "new line", 
                    "press enter", 
                    "Insert line break"
                ),
                SpeechCommand(
                    "New paragraph", 
                    "new paragraph", 
                    "paragraph break", 
                    "Insert paragraph break"
                ),
                SpeechCommand(
                    "Delete word", 
                    "delete word", 
                    "scratch that", 
                    "Delete last spoken word"
                ),
                SpeechCommand(
                    "Delete line", 
                    "delete line", 
                    "clear line", 
                    "Delete current line"
                ),
                SpeechCommand(
                    "Capitalize", 
                    "cap", 
                    "capital", 
                    "Capitalize next word"
                ),
                SpeechCommand(
                    "All caps", 
                    "all caps", 
                    "all capital", 
                    "Type next word in all caps"
                ),
                SpeechCommand(
                    "No caps", 
                    "no caps", 
                    "lowercase", 
                    "Type next word in lowercase"
                ),
                SpeechCommand(
                    "Add period", 
                    "period", 
                    "full stop", 
                    "Insert period"
                ),
                SpeechCommand(
                    "Add comma", 
                    "comma", 
                    null, 
                    "Insert comma"
                ),
                SpeechCommand(
                    "Question mark", 
                    "question mark", 
                    null, 
                    "Insert question mark"
                ),
                SpeechCommand(
                    "Exclamation", 
                    "exclamation mark", 
                    "exclamation point", 
                    "Insert exclamation mark"
                ),
                SpeechCommand(
                    "Add quotes", 
                    "quote", 
                    "quotation marks", 
                    "Insert quotation marks"
                ),
                SpeechCommand(
                    "Space", 
                    "space", 
                    null, 
                    "Insert space"
                ),
                SpeechCommand(
                    "Tab", 
                    "tab", 
                    null, 
                    "Insert tab character"
                )
            )
        )
    }
    
    /**
     * Language switching commands
     */
    private fun getLanguageCommands(): SpeechCommandGroup {
        return SpeechCommandGroup(
            title = "Language Control",
            description = "Switch between different speech recognition languages",
            commands = listOf(
                SpeechCommand(
                    "Switch to English", 
                    "switch to english", 
                    "english mode", 
                    "Change recognition language to English"
                ),
                SpeechCommand(
                    "Switch to Spanish", 
                    "switch to spanish", 
                    "spanish mode", 
                    "Change recognition language to Spanish"
                ),
                SpeechCommand(
                    "Switch to French", 
                    "switch to french", 
                    "french mode", 
                    "Change recognition language to French"
                ),
                SpeechCommand(
                    "Switch to German", 
                    "switch to german", 
                    "german mode", 
                    "Change recognition language to German"
                ),
                SpeechCommand(
                    "Switch to Chinese", 
                    "switch to chinese", 
                    "chinese mode", 
                    "Change recognition language to Chinese"
                ),
                SpeechCommand(
                    "Switch to Japanese", 
                    "switch to japanese", 
                    "japanese mode", 
                    "Change recognition language to Japanese"
                ),
                SpeechCommand(
                    "Auto detect language", 
                    "auto detect", 
                    "automatic language", 
                    "Enable automatic language detection"
                ),
                SpeechCommand(
                    "Show current language", 
                    "what language", 
                    "current language", 
                    "Display current recognition language"
                ),
                SpeechCommand(
                    "List languages", 
                    "list languages", 
                    "available languages", 
                    "Show all available languages"
                )
            )
        )
    }
    
    /**
     * Settings and configuration commands
     */
    private fun getSettingsCommands(): SpeechCommandGroup {
        return SpeechCommandGroup(
            title = "Settings & Configuration",
            description = "Adjust speech recognition settings and behavior",
            commands = listOf(
                SpeechCommand(
                    "Increase confidence", 
                    "higher confidence", 
                    "more accurate", 
                    "Increase recognition confidence threshold"
                ),
                SpeechCommand(
                    "Decrease confidence", 
                    "lower confidence", 
                    "less strict", 
                    "Decrease recognition confidence threshold"
                ),
                SpeechCommand(
                    "Set timeout short", 
                    "short timeout", 
                    "quick timeout", 
                    "Set short speech timeout (2 seconds)"
                ),
                SpeechCommand(
                    "Set timeout medium", 
                    "medium timeout", 
                    "normal timeout", 
                    "Set medium speech timeout (5 seconds)"
                ),
                SpeechCommand(
                    "Set timeout long", 
                    "long timeout", 
                    "extended timeout", 
                    "Set long speech timeout (10 seconds)"
                ),
                SpeechCommand(
                    "Enable noise filtering", 
                    "filter noise", 
                    "noise reduction on", 
                    "Enable background noise filtering"
                ),
                SpeechCommand(
                    "Disable noise filtering", 
                    "no filter", 
                    "noise reduction off", 
                    "Disable background noise filtering"
                ),
                SpeechCommand(
                    "Calibrate microphone", 
                    "calibrate mic", 
                    "microphone setup", 
                    "Calibrate microphone sensitivity"
                ),
                SpeechCommand(
                    "Show recognition status", 
                    "status", 
                    "show stats", 
                    "Display recognition statistics"
                ),
                SpeechCommand(
                    "Reset to defaults", 
                    "reset settings", 
                    "default settings", 
                    "Reset all settings to default values"
                )
            )
        )
    }
    
    /**
     * Advanced speech commands
     */
    private fun getAdvancedCommands(): SpeechCommandGroup {
        return SpeechCommandGroup(
            title = "Advanced Features",
            description = "Advanced speech recognition features and customization",
            commands = listOf(
                SpeechCommand(
                    "Enable continuous mode", 
                    "continuous listening", 
                    "always listen", 
                    "Keep speech recognition always active"
                ),
                SpeechCommand(
                    "Disable continuous mode", 
                    "push to talk", 
                    "manual activation", 
                    "Require manual activation for speech"
                ),
                SpeechCommand(
                    "Train voice model", 
                    "train voice", 
                    "voice training", 
                    "Start voice model training"
                ),
                SpeechCommand(
                    "Create custom command", 
                    "new command", 
                    "add command", 
                    "Create custom voice command"
                ),
                SpeechCommand(
                    "Delete custom command", 
                    "remove command", 
                    "delete command", 
                    "Delete custom voice command"
                ),
                SpeechCommand(
                    "List custom commands", 
                    "my commands", 
                    "custom commands", 
                    "Show all custom commands"
                ),
                SpeechCommand(
                    "Enable wake word", 
                    "wake word on", 
                    "hotword detection", 
                    "Enable wake word detection"
                ),
                SpeechCommand(
                    "Disable wake word", 
                    "wake word off", 
                    "no hotword", 
                    "Disable wake word detection"
                ),
                SpeechCommand(
                    "Set wake word", 
                    "change wake word", 
                    "new wake word", 
                    "Configure custom wake word"
                )
            )
        )
    }
    
    /**
     * System integration commands
     */
    private fun getSystemCommands(): SpeechCommandGroup {
        return SpeechCommandGroup(
            title = "System Integration",
            description = "Commands for system-wide speech recognition integration",
            commands = listOf(
                SpeechCommand(
                    "Enable everywhere", 
                    "global speech", 
                    "system wide", 
                    "Enable speech recognition system-wide"
                ),
                SpeechCommand(
                    "Disable everywhere", 
                    "disable global", 
                    "app only", 
                    "Disable system-wide speech recognition"
                ),
                SpeechCommand(
                    "Check permissions", 
                    "permissions status", 
                    "access rights", 
                    "Check required permissions status"
                ),
                SpeechCommand(
                    "Request permissions", 
                    "get permissions", 
                    "grant access", 
                    "Request missing permissions"
                ),
                SpeechCommand(
                    "Show engine info", 
                    "engine status", 
                    "speech engine", 
                    "Display speech engine information"
                ),
                SpeechCommand(
                    "Switch engine", 
                    "change engine", 
                    "different engine", 
                    "Switch speech recognition engine"
                ),
                SpeechCommand(
                    "Test microphone", 
                    "mic test", 
                    "audio test", 
                    "Test microphone functionality"
                ),
                SpeechCommand(
                    "Show help", 
                    "help", 
                    "voice help", 
                    "Show this help menu"
                )
            )
        )
    }
    
    /**
     * Troubleshooting commands
     */
    private fun getTroubleshootingCommands(): SpeechCommandGroup {
        return SpeechCommandGroup(
            title = "Troubleshooting",
            description = "Commands to diagnose and fix speech recognition issues",
            commands = listOf(
                SpeechCommand(
                    "Run diagnostics", 
                    "diagnose", 
                    "run tests", 
                    "Run speech recognition diagnostics"
                ),
                SpeechCommand(
                    "Clear cache", 
                    "clear cache", 
                    "reset cache", 
                    "Clear speech recognition cache"
                ),
                SpeechCommand(
                    "Restart engine", 
                    "restart speech", 
                    "reload engine", 
                    "Restart speech recognition engine"
                ),
                SpeechCommand(
                    "Check network", 
                    "network status", 
                    "internet check", 
                    "Check network connectivity for cloud features"
                ),
                SpeechCommand(
                    "Show logs", 
                    "debug logs", 
                    "error logs", 
                    "Display speech recognition logs"
                ),
                SpeechCommand(
                    "Report issue", 
                    "report bug", 
                    "send feedback", 
                    "Report speech recognition issue"
                ),
                SpeechCommand(
                    "Force offline mode", 
                    "offline only", 
                    "no network", 
                    "Force offline speech recognition"
                ),
                SpeechCommand(
                    "Force online mode", 
                    "online only", 
                    "cloud mode", 
                    "Force online speech recognition"
                )
            )
        )
    }
    
    /**
     * Show the comprehensive help menu
     */
    fun showHelpMenu(onDismiss: (() -> Unit)? = null) {
        if (helpDialog?.isShowing == true) {
            return
        }
        
        val builder = AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog)
        val view = createHelpView()
        
        builder.setView(view)
        builder.setTitle("Speech Recognition Commands")
        builder.setPositiveButton("Close") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setNeutralButton("Quick Reference") { _, _ ->
            showQuickReference()
        }
        
        helpDialog = builder.create()
        helpDialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#FAFAFA")))
            setGravity(Gravity.CENTER)
            
            // Make dialog 95% of screen width and 80% height
            val params = attributes
            params.width = (context.resources.displayMetrics.widthPixels * 0.95).toInt()
            params.height = (context.resources.displayMetrics.heightPixels * 0.8).toInt()
            attributes = params
        }
        
        helpDialog?.setOnDismissListener {
            helpDialog = null
            onDismiss?.invoke()
        }
        
        helpDialog?.show()
    }
    
    /**
     * Show quick reference with most common commands
     */
    fun showQuickReference() {
        val quickCommands = listOf(
            SpeechCommand("Start listening", "start listening", null, "Activate speech recognition"),
            SpeechCommand("Stop listening", "stop listening", null, "Pause speech recognition"),
            SpeechCommand("Start dictation", "start dictation", null, "Begin voice typing"),
            SpeechCommand("Cancel", "cancel", null, "Cancel current command"),
            SpeechCommand("Switch to English", "switch to english", null, "Change to English"),
            SpeechCommand("Show help", "help", null, "Show help menu")
        )
        
        val group = SpeechCommandGroup("Quick Reference", "Most commonly used commands", quickCommands)
        showCommandGroup(group)
    }
    
    /**
     * Show commands by category
     */
    fun showCategory(categoryIndex: Int) {
        val categories = getSpeechCommands()
        if (categoryIndex in categories.indices) {
            showCommandGroup(categories[categoryIndex])
        }
    }
    
    /**
     * Hide the help menu
     */
    fun hideHelpMenu() {
        helpDialog?.dismiss()
        helpDialog = null
    }
    
    /**
     * Check if help menu is showing
     */
    fun isShowing(): Boolean = helpDialog?.isShowing == true
    
    /**
     * Create the main help view with tabs/categories
     */
    private fun createHelpView(): View {
        val scrollView = ScrollView(context)
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // Add description
        val descriptionText = TextView(context).apply {
            text = "Voice commands for speech recognition control. Speak clearly and wait for confirmation."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(descriptionText)
        
        // Add RecyclerView for categories
        val recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 1)
            adapter = SpeechHelpCommandAdapter(getSpeechCommands())
            setPadding(0, 8, 0, 8)
        }
        
        rootLayout.addView(recyclerView)
        scrollView.addView(rootLayout)
        return scrollView
    }
    
    /**
     * Show a specific command group in a dialog
     */
    private fun showCommandGroup(group: SpeechCommandGroup) {
        val builder = AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog)
        builder.setTitle(group.title)
        
        val scrollView = ScrollView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
        }
        
        // Add description
        if (group.description.isNotEmpty()) {
            val descText = TextView(context).apply {
                text = group.description
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 0, 0, 16)
            }
            layout.addView(descText)
        }
        
        // Add commands
        group.commands.forEach { cmd ->
            val commandView = createCommandItemView(cmd)
            layout.addView(commandView)
        }
        
        scrollView.addView(layout)
        builder.setView(scrollView)
        builder.setPositiveButton("Close", null)
        
        val dialog = builder.create()
        dialog.window?.apply {
            val params = attributes
            params.width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
            params.height = (context.resources.displayMetrics.heightPixels * 0.7).toInt()
            attributes = params
        }
        dialog.show()
    }
    
    /**
     * Create a view for individual command item
     */
    private fun createCommandItemView(command: SpeechCommand): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 16)
        }
        
        // Command name
        val nameText = TextView(context).apply {
            text = command.description
            textSize = 16f
            setTextColor(Color.BLACK)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        layout.addView(nameText)
        
        // Primary command
        val primaryText = TextView(context).apply {
            text = "Say: \"${command.primary}\""
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            setPadding(0, 4, 0, 0)
        }
        layout.addView(primaryText)
        
        // Alternate command if available
        command.alternate?.let { alt ->
            val altText = TextView(context).apply {
                text = "Or: \"$alt\""
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 2, 0, 0)
            }
            layout.addView(altText)
        }
        
        return layout
    }
}

/**
 * Speech command group data class
 */
data class SpeechCommandGroup(
    val title: String,
    val description: String,
    val commands: List<SpeechCommand>
)

/**
 * Speech command data class
 */
data class SpeechCommand(
    val description: String,
    val primary: String,
    val alternate: String?,
    val tooltip: String
)

/**
 * RecyclerView adapter for speech help commands
 */
class SpeechHelpCommandAdapter(
    private val commandGroups: List<SpeechCommandGroup>
) : RecyclerView.Adapter<SpeechHelpCommandAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(android.R.id.text1)
        val descriptionText: TextView = itemView.findViewById(android.R.id.text2)
        val commandsText: TextView = itemView.findViewById(android.R.id.background)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundResource(android.R.drawable.list_selector_background)
            
            addView(TextView(context).apply {
                id = android.R.id.text1
                textSize = 18f
                setTextColor(Color.BLACK)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            
            addView(TextView(context).apply {
                id = android.R.id.text2
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 4, 0, 8)
            })
            
            addView(TextView(context).apply {
                id = android.R.id.background
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                setPadding(0, 0, 0, 4)
            })
        }
        
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = commandGroups[position]
        holder.titleText.text = group.title
        holder.descriptionText.text = group.description
        
        val previewCommands = group.commands.take(3).joinToString(" • ") { 
            "\"${it.primary}\"" 
        }
        val commandCount = group.commands.size
        holder.commandsText.text = "$previewCommands ${if (commandCount > 3) "• +${commandCount - 3} more" else ""}"
        
        holder.itemView.setOnClickListener {
            showFullGroup(holder.itemView.context, group)
        }
    }
    
    override fun getItemCount() = commandGroups.size
    
    private fun showFullGroup(context: Context, group: SpeechCommandGroup) {
        val helpMenu = SpeechRecognitionHelpMenu(context)
        helpMenu.showCategory(commandGroups.indexOf(group))
    }
}