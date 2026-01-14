/**
 * VoiceCursorHelpMenu.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/help/VoiceCursorHelpMenu.kt
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Help menu system for VoiceCursor commands
 * Module: VoiceCursor
 */

package com.augmentalis.voiceos.cursor.help

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.augmentalis.voiceos.cursor.R

/**
 * VoiceCursor Help Menu - Shows available voice commands
 */
class VoiceCursorHelpMenu(private val context: Context) {
    
    private var helpDialog: AlertDialog? = null
    
    /**
     * Command categories for VoiceCursor
     */
    fun getCursorCommands(): List<CommandGroup> {
        return listOf(
            CommandGroup(
                title = "Cursor Movement",
                commands = listOf(
                    VoiceCommand("Move cursor up", "move up", "cursor up"),
                    VoiceCommand("Move cursor down", "move down", "cursor down"),
                    VoiceCommand("Move cursor left", "move left", "cursor left"),
                    VoiceCommand("Move cursor right", "move right", "cursor right"),
                    VoiceCommand("Center cursor", "center", "cursor center")
                )
            ),
            CommandGroup(
                title = "Tap Actions",
                commands = listOf(
                    VoiceCommand("Single tap", "tap", "click"),
                    VoiceCommand("Double tap", "double tap", "double click"),
                    VoiceCommand("Long press", "long press", "hold"),
                    VoiceCommand("Right tap", "right tap", "context menu")
                )
            ),
            CommandGroup(
                title = "Drag & Scroll",
                commands = listOf(
                    VoiceCommand("Start drag", "drag start", "begin drag"),
                    VoiceCommand("Stop drag", "drag stop", "end drag"),
                    VoiceCommand("Scroll up", "scroll up", "page up"),
                    VoiceCommand("Scroll down", "scroll down", "page down"),
                    VoiceCommand("Scroll left", "scroll left"),
                    VoiceCommand("Scroll right", "scroll right")
                )
            ),
            CommandGroup(
                title = "Cursor Settings",
                commands = listOf(
                    VoiceCommand("Show cursor", "show cursor", "cursor on"),
                    VoiceCommand("Hide cursor", "hide cursor", "cursor off"),
                    VoiceCommand("Increase speed", "faster", "speed up"),
                    VoiceCommand("Decrease speed", "slower", "speed down"),
                    VoiceCommand("Change cursor", "cursor style", "cursor shape")
                )
            ),
            CommandGroup(
                title = "Help & Menu",
                commands = listOf(
                    VoiceCommand("Show help", "help", "show commands"),
                    VoiceCommand("Hide help", "close help", "hide commands"),
                    VoiceCommand("Show menu", "cursor menu", "show menu"),
                    VoiceCommand("Show numbers", "show numbers", "number mode"),
                    VoiceCommand("Hide numbers", "hide numbers", "exit numbers")
                )
            ),
            CommandGroup(
                title = "Voice Control",
                commands = listOf(
                    VoiceCommand("Stop listening", "stop voice", "pause voice"),
                    VoiceCommand("Start listening", "start voice", "resume voice"),
                    VoiceCommand("Cancel", "cancel", "undo"),
                    VoiceCommand("Confirm", "confirm", "okay")
                )
            )
        )
    }
    
    /**
     * Show the help menu dialog
     */
    fun showHelpMenu(onDismiss: (() -> Unit)? = null) {
        if (helpDialog?.isShowing == true) {
            return
        }
        
        val builder = AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
        val inflater = LayoutInflater.from(context)
        val view = createHelpView(inflater)
        
        builder.setView(view)
        builder.setTitle("VoiceCursor Commands")
        builder.setPositiveButton("Close") { dialog, _ ->
            dialog.dismiss()
        }
        
        helpDialog = builder.create()
        helpDialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#F0F0F0")))
            setGravity(Gravity.CENTER)
            
            // Make dialog 90% of screen width
            val params = attributes
            params.width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
        }
        
        helpDialog?.setOnDismissListener {
            helpDialog = null
            onDismiss?.invoke()
        }
        
        helpDialog?.show()
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
     * Create the help view layout
     */
    @Suppress("UNUSED_PARAMETER")
    private fun createHelpView(inflater: LayoutInflater): View {
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        val recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 2) // 2 columns
            adapter = HelpCommandAdapter(getCursorCommands())
            setPadding(8, 8, 8, 8)
        }
        
        rootLayout.addView(recyclerView)
        return rootLayout
    }
    
    /**
     * Show quick command list (for specific context)
     */
    fun showQuickCommands(commands: List<VoiceCommand>, title: String = "Quick Commands") {
        val group = CommandGroup(title, commands)
        showCommandGroup(group)
    }
    
    /**
     * Show a specific command group
     */
    private fun showCommandGroup(group: CommandGroup) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(group.title)
        
        val message = group.commands.joinToString("\n") { cmd ->
            "• ${cmd.description}: \"${cmd.primary}\""
        }
        
        builder.setMessage(message)
        builder.setPositiveButton("OK", null)
        builder.show()
    }
}

/**
 * Command group data class
 */
data class CommandGroup(
    val title: String,
    val commands: List<VoiceCommand>
)

/**
 * Voice command data class
 */
data class VoiceCommand(
    val description: String,
    val primary: String,
    val alternate: String? = null
)

/**
 * RecyclerView adapter for help commands
 */
class HelpCommandAdapter(
    private val commandGroups: List<CommandGroup>
) : RecyclerView.Adapter<HelpCommandAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(android.R.id.text1)
        val commandsText: TextView = itemView.findViewById(android.R.id.text2)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            
            addView(TextView(context).apply {
                id = android.R.id.text1
                textSize = 16f
                setTextColor(Color.BLACK)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            
            addView(TextView(context).apply {
                id = android.R.id.text2
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 8, 0, 0)
            })
        }
        
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = commandGroups[position]
        holder.titleText.text = group.title
        
        val commandsText = group.commands.take(3).joinToString("\n") { cmd ->
            "\"${cmd.primary}\""
        }
        holder.commandsText.text = commandsText
        
        holder.itemView.setOnClickListener {
            // Show full command group when clicked
            showFullGroup(holder.itemView.context, group)
        }
    }
    
    override fun getItemCount() = commandGroups.size
    
    private fun showFullGroup(context: Context, group: CommandGroup) {
        AlertDialog.Builder(context)
            .setTitle(group.title)
            .setMessage(group.commands.joinToString("\n") { 
                "• ${it.description}: \"${it.primary}\"" 
            })
            .setPositiveButton("OK", null)
            .show()
    }
}