/**
 * DynamicCommandGenerator.kt - Generates contextual commands based on UI
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-27
 * 
 * Analyzes current UI and generates available voice commands dynamically.
 * VOS4 Direct Implementation - No interfaces.
 */
package com.augmentalis.voiceos.accessibility.managers

import android.accessibilityservice.AccessibilityService as AndroidAccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Generates dynamic commands based on current UI context
 * Direct implementation following VOS4 patterns
 */
class DynamicCommandGenerator(private val service: VoiceAccessibilityService) {
    
    companion object {
        private const val TAG = "DynamicCommandGenerator"
        private const val CACHE_DURATION_MS = 5000L
        private const val MAX_DEPTH = 10
        private const val MAX_COMMANDS = 100
    }
    
    // Command cache to avoid regenerating frequently
    private val commandCache = ConcurrentHashMap<String, CachedCommands>()
    
    // Coroutine scope for async operations
    private val generatorScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob()
    )
    
    data class DynamicCommand(
        val command: String,
        val description: String,
        val confidence: Float,
        val nodeInfo: AccessibilityNodeInfo? = null
    )
    
    data class CachedCommands(
        val commands: List<DynamicCommand>,
        val timestamp: Long
    )
    
    /**
     * Initialize generator
     */
    fun initialize() {
        Log.d(TAG, "Initializing DynamicCommandGenerator")
    }
    
    /**
     * Generate commands for current UI context
     */
    fun generateCommands(useCache: Boolean = true): List<DynamicCommand> {
        val packageName = service.rootInActiveWindow?.packageName?.toString() ?: "unknown"
        
        // Check cache if enabled
        if (useCache) {
            commandCache[packageName]?.let { cached ->
                if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
                    Log.d(TAG, "Using cached commands for $packageName")
                    return cached.commands
                }
            }
        }
        
        // Generate new commands
        val commands = mutableListOf<DynamicCommand>()
        val rootNode = service.rootInActiveWindow
        
        if (rootNode != null) {
            // Add basic navigation commands
            commands.addAll(generateNavigationCommands(rootNode))
            
            // Add clickable elements
            commands.addAll(generateClickableCommands(rootNode))
            
            // Add text input commands
            commands.addAll(generateTextInputCommands(rootNode))
            
            // Add scrollable commands
            commands.addAll(generateScrollCommands(rootNode))
            
            // Add app-specific commands
            commands.addAll(generateAppSpecificCommands(packageName, rootNode))
        }
        
        // Sort by confidence and limit
        val sortedCommands = commands
            .sortedByDescending { it.confidence }
            .take(MAX_COMMANDS)
        
        // Cache results
        commandCache[packageName] = CachedCommands(sortedCommands, System.currentTimeMillis())
        
        Log.d(TAG, "Generated ${sortedCommands.size} commands for $packageName")
        return sortedCommands
    }
    
    /**
     * Generate commands asynchronously
     */
    fun generateCommandsAsync(callback: (List<DynamicCommand>) -> Unit) {
        generatorScope.launch {
            val commands = generateCommands()
            withContext(Dispatchers.Main) {
                callback(commands)
            }
        }
    }
    
    /**
     * Get command suggestions for partial input
     */
    fun getSuggestions(partialCommand: String): List<DynamicCommand> {
        val normalized = partialCommand.lowercase().trim()
        if (normalized.isEmpty()) return emptyList()
        
        val allCommands = generateCommands()
        
        return allCommands.filter { command ->
            command.command.lowercase().startsWith(normalized) ||
            command.command.lowercase().contains(normalized)
        }.take(10)
    }
    
    /**
     * Generate navigation commands
     */
    private fun generateNavigationCommands(rootNode: AccessibilityNodeInfo): List<DynamicCommand> {
        val commands = mutableListOf<DynamicCommand>()
        
        // Always available navigation
        commands.add(DynamicCommand("go back", "Navigate to previous screen", 1.0f))
        commands.add(DynamicCommand("go home", "Return to home screen", 1.0f))
        commands.add(DynamicCommand("recent apps", "Show recent applications", 0.9f))
        
        // Check if scrollable
        if (hasScrollableContent(rootNode)) {
            commands.add(DynamicCommand("scroll down", "Scroll content down", 0.95f))
            commands.add(DynamicCommand("scroll up", "Scroll content up", 0.95f))
        }
        
        return commands
    }
    
    /**
     * Generate commands for clickable elements
     */
    private fun generateClickableCommands(rootNode: AccessibilityNodeInfo): List<DynamicCommand> {
        val commands = mutableListOf<DynamicCommand>()
        val clickableNodes = findClickableNodes(rootNode)
        
        clickableNodes.forEach { node ->
            val text = node.text?.toString()
            val description = node.contentDescription?.toString()
            
            // Generate command from text or description
            val label = text ?: description
            if (!label.isNullOrBlank() && label.length < 50) {
                val normalizedLabel = label.lowercase().trim()
                commands.add(
                    DynamicCommand(
                        "click $normalizedLabel",
                        "Click on $label",
                        0.85f,
                        node
                    )
                )
                
                // Add alternative commands for common buttons
                when {
                    normalizedLabel.contains("submit") || normalizedLabel.contains("ok") -> {
                        commands.add(DynamicCommand("submit", "Submit form", 0.9f, node))
                    }
                    normalizedLabel.contains("cancel") -> {
                        commands.add(DynamicCommand("cancel", "Cancel action", 0.9f, node))
                    }
                    normalizedLabel.contains("search") -> {
                        commands.add(DynamicCommand("search", "Perform search", 0.9f, node))
                    }
                    normalizedLabel.contains("menu") -> {
                        commands.add(DynamicCommand("open menu", "Open menu", 0.9f, node))
                    }
                    normalizedLabel.contains("more") -> {
                        commands.add(DynamicCommand("show more", "Show more options", 0.85f, node))
                    }
                }
            }
        }
        
        return commands
    }
    
    /**
     * Generate text input commands
     */
    private fun generateTextInputCommands(rootNode: AccessibilityNodeInfo): List<DynamicCommand> {
        val commands = mutableListOf<DynamicCommand>()
        val editableNodes = findEditableNodes(rootNode)
        
        editableNodes.forEach { node ->
            val hint = node.hintText?.toString()
            val description = node.contentDescription?.toString()
            
            val fieldName = hint ?: description ?: "text field"
            
            commands.add(
                DynamicCommand(
                    "type in $fieldName",
                    "Enter text in $fieldName",
                    0.8f,
                    node
                )
            )
            
            // Add specific commands based on field type
            when {
                fieldName.lowercase().contains("search") -> {
                    commands.add(DynamicCommand("search for", "Search for content", 0.85f, node))
                }
                fieldName.lowercase().contains("email") -> {
                    commands.add(DynamicCommand("enter email", "Enter email address", 0.85f, node))
                }
                fieldName.lowercase().contains("password") -> {
                    commands.add(DynamicCommand("enter password", "Enter password", 0.85f, node))
                }
                fieldName.lowercase().contains("username") -> {
                    commands.add(DynamicCommand("enter username", "Enter username", 0.85f, node))
                }
            }
        }
        
        if (editableNodes.isNotEmpty()) {
            commands.add(DynamicCommand("clear text", "Clear all text", 0.75f))
            commands.add(DynamicCommand("select all", "Select all text", 0.75f))
        }
        
        return commands
    }
    
    /**
     * Generate scroll commands
     */
    private fun generateScrollCommands(rootNode: AccessibilityNodeInfo): List<DynamicCommand> {
        val commands = mutableListOf<DynamicCommand>()
        
        if (hasScrollableContent(rootNode)) {
            commands.addAll(listOf(
                DynamicCommand("scroll to top", "Scroll to beginning", 0.8f),
                DynamicCommand("scroll to bottom", "Scroll to end", 0.8f),
                DynamicCommand("page down", "Scroll one page down", 0.75f),
                DynamicCommand("page up", "Scroll one page up", 0.75f)
            ))
        }
        
        return commands
    }
    
    /**
     * Generate app-specific commands
     */
    private fun generateAppSpecificCommands(
        packageName: String,
        rootNode: AccessibilityNodeInfo
    ): List<DynamicCommand> {
        val commands = mutableListOf<DynamicCommand>()
        
        // Analyze UI structure for context-aware commands
        val hasTextFields = findNodesWithRole(rootNode, "EditText").isNotEmpty()
        @Suppress("UNUSED_VARIABLE")
        val hasButtons = findNodesWithRole(rootNode, "Button").isNotEmpty() // Reserved for future button-specific commands
        val hasScrollableContent = findScrollableNodes(rootNode).isNotEmpty()
        
        when {
            // Browser commands
            packageName.contains("chrome") || packageName.contains("browser") -> {
                commands.addAll(listOf(
                    DynamicCommand("new tab", "Open new browser tab", 0.85f),
                    DynamicCommand("close tab", "Close current tab", 0.85f),
                    DynamicCommand("refresh", "Refresh page", 0.85f),
                    DynamicCommand("go forward", "Navigate forward", 0.8f),
                    DynamicCommand("bookmarks", "Show bookmarks", 0.8f)
                ))
                
                // Add context-aware commands
                if (hasTextFields) {
                    commands.add(DynamicCommand("search", "Perform search", 0.9f))
                }
                if (hasScrollableContent) {
                    commands.add(DynamicCommand("scroll to bottom", "Scroll to bottom of page", 0.75f))
                }
            }
            
            // Messaging apps
            packageName.contains("messages") || packageName.contains("whatsapp") -> {
                commands.addAll(listOf(
                    DynamicCommand("new message", "Compose new message", 0.85f),
                    DynamicCommand("send", "Send message", 0.9f),
                    DynamicCommand("attach photo", "Attach a photo", 0.8f),
                    DynamicCommand("voice message", "Record voice message", 0.8f)
                ))
            }
            
            // Email apps
            packageName.contains("gmail") || packageName.contains("email") -> {
                commands.addAll(listOf(
                    DynamicCommand("compose", "Compose new email", 0.85f),
                    DynamicCommand("reply", "Reply to email", 0.85f),
                    DynamicCommand("forward", "Forward email", 0.8f),
                    DynamicCommand("archive", "Archive email", 0.8f),
                    DynamicCommand("mark as read", "Mark email as read", 0.75f)
                ))
            }
            
            // Maps
            packageName.contains("maps") -> {
                commands.addAll(listOf(
                    DynamicCommand("directions", "Get directions", 0.85f),
                    DynamicCommand("my location", "Show my location", 0.9f),
                    DynamicCommand("zoom in", "Zoom in on map", 0.8f),
                    DynamicCommand("zoom out", "Zoom out on map", 0.8f),
                    DynamicCommand("satellite view", "Switch to satellite view", 0.75f)
                ))
            }
            
            // YouTube
            packageName.contains("youtube") -> {
                commands.addAll(listOf(
                    DynamicCommand("play", "Play video", 0.9f),
                    DynamicCommand("pause", "Pause video", 0.9f),
                    DynamicCommand("next video", "Skip to next video", 0.85f),
                    DynamicCommand("fullscreen", "Enter fullscreen", 0.85f),
                    DynamicCommand("subscribe", "Subscribe to channel", 0.8f)
                ))
            }
        }
        
        return commands
    }
    
    /**
     * Find all clickable nodes
     */
    private fun findClickableNodes(
        node: AccessibilityNodeInfo,
        depth: Int = 0
    ): List<AccessibilityNodeInfo> {
        if (depth > MAX_DEPTH) return emptyList()
        
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()
        
        if (node.isClickable) {
            clickableNodes.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                clickableNodes.addAll(findClickableNodes(child, depth + 1))
            }
        }
        
        return clickableNodes
    }
    
    /**
     * Find all editable nodes
     */
    private fun findEditableNodes(
        node: AccessibilityNodeInfo,
        depth: Int = 0
    ): List<AccessibilityNodeInfo> {
        if (depth > MAX_DEPTH) return emptyList()
        
        val editableNodes = mutableListOf<AccessibilityNodeInfo>()
        
        if (node.isEditable) {
            editableNodes.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                editableNodes.addAll(findEditableNodes(child, depth + 1))
            }
        }
        
        return editableNodes
    }
    
    /**
     * Check if node tree has scrollable content
     */
    private fun hasScrollableContent(
        node: AccessibilityNodeInfo,
        depth: Int = 0
    ): Boolean {
        if (depth > MAX_DEPTH) return false
        
        if (node.isScrollable) return true
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                if (hasScrollableContent(child, depth + 1)) return true
            }
        }
        
        return false
    }
    
    /**
     * Check if command exists in generated commands
     */
    fun hasCommand(command: String): Boolean {
        val normalizedCommand = command.lowercase().trim()
        val commands = generateCommands()
        return commands.any { it.command.lowercase() == normalizedCommand }
    }

    /**
     * Execute a dynamic command
     */
    fun executeCommand(command: String): Boolean {
        val normalizedCommand = command.lowercase().trim()
        val commands = generateCommands()
        
        val matchingCommand = commands.find { it.command.lowercase() == normalizedCommand }
        if (matchingCommand != null) {
            // If command has associated node, perform action on it
            matchingCommand.nodeInfo?.let { node ->
                return when {
                    normalizedCommand.startsWith("click ") ||
                    normalizedCommand.startsWith("tap ") -> {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    normalizedCommand.startsWith("type in ") ||
                    normalizedCommand.startsWith("enter ") -> {
                        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    }
                    else -> node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
            
            // Handle global commands without specific nodes
            return when (normalizedCommand) {
                "go back" -> service.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_BACK)
                "go home" -> service.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_HOME)
                "recent apps" -> service.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_RECENTS)
                "scroll down" -> performScroll(forward = true)
                "scroll up" -> performScroll(forward = false)
                "scroll to top" -> performScrollToTop()
                "scroll to bottom" -> performScrollToBottom()
                else -> false
            }
        }
        
        return false
    }
    
    /**
     * Perform scroll action
     */
    private fun performScroll(forward: Boolean): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(rootNode) ?: return false
        
        return if (forward) {
            scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        } else {
            scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }
    }
    
    /**
     * Scroll to top of content
     */
    private fun performScrollToTop(): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(rootNode) ?: return false
        
        // Repeatedly scroll backward until we can't scroll anymore
        var scrolled = false
        repeat(20) { // Maximum 20 attempts to prevent infinite loop
            if (scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                scrolled = true
            } else {
                return scrolled
            }
        }
        return scrolled
    }
    
    /**
     * Scroll to bottom of content
     */
    private fun performScrollToBottom(): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(rootNode) ?: return false
        
        // Repeatedly scroll forward until we can't scroll anymore
        var scrolled = false
        repeat(20) { // Maximum 20 attempts to prevent infinite loop
            if (scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                scrolled = true
            } else {
                return scrolled
            }
        }
        return scrolled
    }
    
    /**
     * Find first scrollable node
     */
    private fun findScrollableNode(
        node: AccessibilityNodeInfo,
        depth: Int = 0
    ): AccessibilityNodeInfo? {
        if (depth > MAX_DEPTH) return null
        
        if (node.isScrollable) return node
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findScrollableNode(child, depth + 1)
                if (result != null) return result
            }
        }
        
        return null
    }

    /**
     * Find nodes with specific role/class
     */
    private fun findNodesWithRole(
        node: AccessibilityNodeInfo,
        className: String,
        depth: Int = 0
    ): List<AccessibilityNodeInfo> {
        if (depth > MAX_DEPTH) return emptyList()
        
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        
        if (node.className?.toString()?.contains(className, ignoreCase = true) == true) {
            nodes.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                nodes.addAll(findNodesWithRole(child, className, depth + 1))
            }
        }
        
        return nodes
    }
    
    /**
     * Find all scrollable nodes in the UI tree
     */
    private fun findScrollableNodes(
        node: AccessibilityNodeInfo,
        depth: Int = 0
    ): List<AccessibilityNodeInfo> {
        if (depth > MAX_DEPTH) return emptyList()
        
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        
        if (node.isScrollable) {
            nodes.add(node)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                nodes.addAll(findScrollableNodes(child, depth + 1))
            }
        }
        
        return nodes
    }

    /**
     * Clear command cache
     */
    fun clearCache() {
        commandCache.clear()
        Log.d(TAG, "Command cache cleared")
    }
    
    /**
     * Dispose generator
     */
    fun dispose() {
        Log.d(TAG, "Disposing DynamicCommandGenerator")
        clearCache()
        generatorScope.cancel()
    }
}