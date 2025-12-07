/**
 * ContextManager.kt - Context-aware command execution management
 * Manage command execution context and provide context-aware capabilities
 */

package com.augmentalis.commandmanager.context

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.models.*
import com.augmentalis.commandmanager.definitions.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Context manager for command execution
 * Provides context-aware command processing and execution
 */
class ContextManager(
    private val androidContext: Context
) {
    
    companion object {
        private const val TAG = "ContextManager"
    }
    
    // Current context state
    private val _currentContext = MutableStateFlow<CommandContext?>(null)
    val currentContext: Flow<CommandContext?> = _currentContext.asStateFlow()
    
    // Context providers
    private val contextProviders = mutableListOf<ContextProvider>()
    
    // Context rules
    private val contextRules = mutableListOf<ContextRule>()
    
    // Context cache
    private var lastContextUpdate = 0L
    private var contextCacheTimeout = 1000L // 1 second
    
    /**
     * Initialize the context manager
     */
    suspend fun initialize() {
        // Register built-in context providers
        registerBuiltInProviders()
        
        // Register built-in context rules
        registerBuiltInRules()
        
        android.util.Log.i(TAG, "Context manager initialized with ${contextProviders.size} providers and ${contextRules.size} rules")
    }
    
    /**
     * Update current context from accessibility service
     */
    fun updateContext(accessibilityService: AccessibilityService?) {
        val currentTime = System.currentTimeMillis()
        
        // Check cache timeout
        if (currentTime - lastContextUpdate < contextCacheTimeout) {
            return
        }
        
        try {
            val context = buildContext(accessibilityService)
            _currentContext.value = context
            lastContextUpdate = currentTime
            
            android.util.Log.v(TAG, "Context updated: ${context?.packageName}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to update context", e)
        }
    }
    
    /**
     * Get contextual commands for current state
     */
    fun getContextualCommands(
        allCommands: List<CommandDefinition>,
        context: CommandContext? = _currentContext.value
    ): List<CommandDefinition> {
        if (context == null) {
            return allCommands.filter { it.requiredContext.isEmpty() }
        }
        
        return allCommands.filter { command ->
            isCommandAvailableInContext(command, context)
        }.sortedBy { command ->
            // Sort by relevance score
            calculateRelevanceScore(command, context)
        }.reversed()
    }
    
    /**
     * Enhance command with context information
     */
    fun enhanceCommandWithContext(
        command: Command,
        accessibilityService: AccessibilityService?
    ): Command {
        val enhancedContext = enhanceContext(command.context, accessibilityService)
        
        return command.copy(
            context = enhancedContext,
            parameters = command.parameters + extractContextParameters(enhancedContext)
        )
    }
    
    /**
     * Check if command is available in current context
     */
    fun isCommandAvailableInContext(command: CommandDefinition, context: CommandContext): Boolean {
        // If command has no context requirements, it's always available
        if (command.requiredContext.isEmpty()) {
            return true
        }
        
        // Check each required context
        for (requirement in command.requiredContext) {
            if (!checkContextRequirement(requirement, context)) {
                return false
            }
        }
        
        // Apply custom context rules
        for (rule in contextRules) {
            if (!rule.evaluate(command, context)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Get context suggestions for command
     */
    fun getContextSuggestions(command: CommandDefinition): List<ContextSuggestion> {
        val suggestions = mutableListOf<ContextSuggestion>()
        
        for (requirement in command.requiredContext) {
            when {
                requirement == "text_input" -> {
                    suggestions.add(ContextSuggestion(
                        type = SuggestionType.FOCUS_ELEMENT,
                        message = "Focus on a text input field",
                        action = "Find and focus a text field to use this command"
                    ))
                }
                requirement.startsWith("package:") -> {
                    val packageName = requirement.removePrefix("package:")
                    suggestions.add(ContextSuggestion(
                        type = SuggestionType.OPEN_APP,
                        message = "Open app: $packageName",
                        action = "This command requires $packageName to be active"
                    ))
                }
                requirement.startsWith("activity:") -> {
                    val activityName = requirement.removePrefix("activity:")
                    suggestions.add(ContextSuggestion(
                        type = SuggestionType.NAVIGATE_TO,
                        message = "Navigate to: $activityName",
                        action = "This command requires being in $activityName"
                    ))
                }
                else -> {
                    suggestions.add(ContextSuggestion(
                        type = SuggestionType.GENERAL,
                        message = "Context requirement: $requirement",
                        action = "Ensure the required context is available"
                    ))
                }
            }
        }
        
        return suggestions
    }
    
    /**
     * Register context provider
     */
    fun registerContextProvider(provider: ContextProvider) {
        contextProviders.add(provider)
        android.util.Log.d(TAG, "Registered context provider: ${provider.name}")
    }
    
    /**
     * Register context rule
     */
    fun registerContextRule(rule: ContextRule) {
        contextRules.add(rule)
        android.util.Log.d(TAG, "Registered context rule: ${rule.name}")
    }
    
    /**
     * Set context cache timeout
     */
    fun setContextCacheTimeout(timeoutMs: Long) {
        contextCacheTimeout = timeoutMs
        android.util.Log.d(TAG, "Context cache timeout set to ${timeoutMs}ms")
    }
    
    // Private methods
    
    /**
     * Build command context from accessibility service
     */
    private fun buildContext(accessibilityService: AccessibilityService?): CommandContext? {
        if (accessibilityService == null) {
            return null
        }
        
        val customData = mutableMapOf<String, Any>()
        customData["androidContext"] = androidContext
        customData["accessibilityService"] = accessibilityService
        
        // Get current app info
        val (packageName, activityName) = getCurrentAppInfo()
        
        // Get focused element info
        val focusedElement = getFocusedElementInfo(accessibilityService)
        
        // Get screen content summary
        val screenContent = getScreenContentSummary(accessibilityService)
        
        // Enhance with context providers
        for (provider in contextProviders) {
            try {
                val providerData = provider.provideContext(accessibilityService)
                customData.putAll(providerData)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Context provider ${provider.name} failed", e)
            }
        }
        
        return CommandContext(
            packageName = packageName,
            activityName = activityName,
            screenContent = screenContent,
            focusedElement = focusedElement,
            customData = customData
        )
    }
    
    /**
     * Get current app information
     */
    private fun getCurrentAppInfo(): Pair<String?, String?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            getCurrentAppInfoModern()
        } else {
            getCurrentAppInfoLegacy()
        }
    }
    
    /**
     * Get current app info using modern UsageStatsManager API (Android 5.1+)
     */
    private fun getCurrentAppInfoModern(): Pair<String?, String?> {
        return try {
            val usageStatsManager = androidContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 1000 * 60 // Last minute
            
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                beginTime,
                endTime
            )
            
            val currentApp = usageStats.maxByOrNull { it.lastTimeUsed }
            if (currentApp != null) {
                // Note: UsageStats doesn't provide activity name, only package
                Pair(currentApp.packageName, null)
            } else {
                // Fallback to legacy method if no usage stats available
                getCurrentAppInfoLegacy()
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to get current app info via UsageStats, falling back", e)
            getCurrentAppInfoLegacy()
        }
    }
    
    /**
     * Get current app info using legacy getRunningTasks API (deprecated but still works)
     */
    private fun getCurrentAppInfoLegacy(): Pair<String?, String?> {
        return try {
            val activityManager = androidContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val runningTasks = activityManager.getRunningTasks(1)
            
            if (runningTasks.isNotEmpty()) {
                val topTask = runningTasks[0]
                val packageName = topTask.topActivity?.packageName
                val activityName = topTask.topActivity?.className
                Pair(packageName, activityName)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to get current app info", e)
            Pair(null, null)
        }
    }
    
    /**
     * Get focused element information
     */
    private fun getFocusedElementInfo(accessibilityService: AccessibilityService): String? {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow
            val focusedNode = findFocusedNode(rootNode)
            
            focusedNode?.let { node ->
                buildString {
                    append(node.className)
                    node.text?.let { text ->
                        append(":$text")
                    }
                    if (node.isEditable) append(":editable")
                    if (node.isClickable) append(":clickable")
                    if (node.isScrollable) append(":scrollable")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to get focused element info", e)
            null
        }
    }
    
    /**
     * Get screen content summary
     */
    private fun getScreenContentSummary(accessibilityService: AccessibilityService): String? {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow
            val contentElements = mutableListOf<String>()
            
            collectContentElements(rootNode, contentElements, 0, 3) // Max depth 3
            
            if (contentElements.isNotEmpty()) {
                contentElements.take(10).joinToString(", ") // Max 10 elements
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to get screen content summary", e)
            null
        }
    }
    
    /**
     * Find focused accessibility node
     */
    private fun findFocusedNode(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        
        if (rootNode.isFocused) return rootNode
        
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            child?.let {
                val focused = findFocusedNode(it)
                if (focused != null) return focused
            }
        }
        
        return null
    }
    
    /**
     * Collect content elements for screen summary
     */
    private fun collectContentElements(
        node: AccessibilityNodeInfo?,
        elements: MutableList<String>,
        depth: Int,
        maxDepth: Int
    ) {
        if (node == null || depth > maxDepth || elements.size >= 10) return
        
        // Add text content
        node.text?.toString()?.let { text ->
            if (text.isNotBlank() && text.length < 50) {
                elements.add(text.trim())
            }
        }
        
        // Add content description
        node.contentDescription?.toString()?.let { desc ->
            if (desc.isNotBlank() && desc.length < 50 && desc !in elements) {
                elements.add(desc.trim())
            }
        }
        
        // Recurse to children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            collectContentElements(child, elements, depth + 1, maxDepth)
        }
    }
    
    /**
     * Check context requirement
     */
    private fun checkContextRequirement(requirement: String, context: CommandContext): Boolean {
        return when {
            requirement == "text_input" -> {
                context.focusedElement?.contains("editable") == true
            }
            requirement.startsWith("package:") -> {
                val requiredPackage = requirement.removePrefix("package:")
                context.packageName == requiredPackage
            }
            requirement.startsWith("activity:") -> {
                val requiredActivity = requirement.removePrefix("activity:")
                context.activityName == requiredActivity
            }
            requirement == "browser" -> {
                context.packageName?.contains("browser") == true ||
                context.packageName?.contains("chrome") == true
            }
            requirement == "media" -> {
                context.packageName?.contains("music") == true ||
                context.packageName?.contains("video") == true ||
                context.packageName?.contains("media") == true
            }
            requirement == "scrollable" -> {
                context.focusedElement?.contains("scrollable") == true ||
                context.customData["hasScrollableContent"] == true
            }
            else -> true // Unknown requirements default to true
        }
    }
    
    /**
     * Calculate command relevance score for context
     */
    private fun calculateRelevanceScore(command: CommandDefinition, context: CommandContext): Float {
        var score = 0f
        
        // Base score for category relevance
        score += when (command.category) {
            "INPUT" -> if (context.focusedElement?.contains("editable") == true) 2f else 0f
            "NAVIGATION" -> 1f
            "MEDIA" -> if (context.packageName?.contains("music") == true || 
                                       context.packageName?.contains("video") == true) 2f else 0f
            "APP_CONTROL" -> 1f
            "SYSTEM" -> 0.5f
            else -> 0f
        }
        
        // Score for context requirements match
        score += command.requiredContext.size * 0.5f
        
        // Score for command specificity
        score += if (command.parameters.isNotEmpty()) 0.3f else 0f
        
        return score
    }
    
    /**
     * Enhance context with additional information
     */
    private fun enhanceContext(
        originalContext: CommandContext?,
        accessibilityService: AccessibilityService?
    ): CommandContext? {
        if (originalContext == null || accessibilityService == null) {
            return originalContext
        }
        
        val enhancedData = originalContext.customData.toMutableMap()
        
        // Add real-time accessibility info
        enhancedData["accessibilityService"] = accessibilityService
        enhancedData["rootNode"] = accessibilityService.rootInActiveWindow
        enhancedData["timestamp"] = System.currentTimeMillis()
        
        return originalContext.copy(customData = enhancedData)
    }
    
    /**
     * Extract context parameters for command
     */
    private fun extractContextParameters(context: CommandContext?): Map<String, Any> {
        if (context == null) return emptyMap()
        
        val parameters = mutableMapOf<String, Any>()
        
        // Extract common parameters from context
        context.packageName?.let { parameters["currentPackage"] = it }
        context.activityName?.let { parameters["currentActivity"] = it }
        context.focusedElement?.let { parameters["focusedElement"] = it }
        
        return parameters
    }
    
    /**
     * Register built-in context providers
     */
    private fun registerBuiltInProviders() {
        registerContextProvider(AppContextProvider())
        registerContextProvider(UIContextProvider())
        registerContextProvider(SystemContextProvider())
    }
    
    /**
     * Register built-in context rules
     */
    private fun registerBuiltInRules() {
        registerContextRule(TextInputAvailabilityRule())
        registerContextRule(AppSpecificCommandRule())
        registerContextRule(UIElementAvailabilityRule())
    }
}

/**
 * Context provider interface
 */
interface ContextProvider {
    val name: String
    fun provideContext(accessibilityService: AccessibilityService): Map<String, Any>
}

/**
 * Context rule interface
 */
interface ContextRule {
    val name: String
    fun evaluate(command: CommandDefinition, context: CommandContext): Boolean
}

/**
 * Context suggestion
 */
data class ContextSuggestion(
    val type: SuggestionType,
    val message: String,
    val action: String
)

/**
 * Suggestion types
 */
enum class SuggestionType {
    FOCUS_ELEMENT,
    OPEN_APP,
    NAVIGATE_TO,
    GENERAL
}

// Built-in context providers

class AppContextProvider : ContextProvider {
    override val name = "AppContextProvider"
    
    override fun provideContext(accessibilityService: AccessibilityService): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        try {
            val rootNode = accessibilityService.rootInActiveWindow
            rootNode?.let { node ->
                data["appTitle"] = node.packageName ?: "unknown"
                data["windowType"] = if (node.isScrollable) "scrollable" else "static"
            }
        } catch (e: Exception) {
            // Ignore errors
        }
        
        return data
    }
}

class UIContextProvider : ContextProvider {
    override val name = "UIContextProvider"
    
    override fun provideContext(accessibilityService: AccessibilityService): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        
        try {
            val rootNode = accessibilityService.rootInActiveWindow
            rootNode?.let { node ->
                data["hasScrollableContent"] = hasScrollableContent(node)
                data["hasEditableFields"] = hasEditableFields(node)
                data["hasClickableElements"] = hasClickableElements(node)
                data["elementCount"] = countElements(node)
            }
        } catch (e: Exception) {
            // Ignore errors
        }
        
        return data
    }
    
    private fun hasScrollableContent(node: AccessibilityNodeInfo): Boolean {
        if (node.isScrollable) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && hasScrollableContent(child)) return true
        }
        return false
    }
    
    private fun hasEditableFields(node: AccessibilityNodeInfo): Boolean {
        if (node.isEditable) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && hasEditableFields(child)) return true
        }
        return false
    }
    
    private fun hasClickableElements(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && hasClickableElements(child)) return true
        }
        return false
    }
    
    private fun countElements(node: AccessibilityNodeInfo): Int {
        var count = 1
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) count += countElements(child)
        }
        return count
    }
}

class SystemContextProvider : ContextProvider {
    override val name = "SystemContextProvider"
    
    override fun provideContext(accessibilityService: AccessibilityService): Map<String, Any> {
        return mapOf(
            "systemVersion" to android.os.Build.VERSION.SDK_INT,
            "timestamp" to System.currentTimeMillis()
        )
    }
}

// Built-in context rules

class TextInputAvailabilityRule : ContextRule {
    override val name = "TextInputAvailabilityRule"
    
    override fun evaluate(command: CommandDefinition, context: CommandContext): Boolean {
        // If command requires text input, check if text field is available
        if (command.requiredContext.contains("text_input")) {
            return context.focusedElement?.contains("editable") == true ||
                   context.customData["hasEditableFields"] == true
        }
        return true
    }
}

class AppSpecificCommandRule : ContextRule {
    override val name = "AppSpecificCommandRule"
    
    override fun evaluate(command: CommandDefinition, context: CommandContext): Boolean {
        // Check app-specific command requirements
        for (requirement in command.requiredContext) {
            if (requirement.startsWith("package:")) {
                val requiredPackage = requirement.removePrefix("package:")
                if (context.packageName != requiredPackage) return false
            }
        }
        return true
    }
}

class UIElementAvailabilityRule : ContextRule {
    override val name = "UIElementAvailabilityRule"
    
    override fun evaluate(command: CommandDefinition, context: CommandContext): Boolean {
        // Check if required UI elements are available
        when (command.category) {
            "INPUT" -> {
                if (command.id.contains("scroll") || command.id.contains("swipe")) {
                    return context.customData["hasScrollableContent"] == true
                }
                if (command.id.contains("click")) {
                    return context.customData["hasClickableElements"] == true
                }
            }
            else -> { /* No specific requirements */ }
        }
        return true
    }
}