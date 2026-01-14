/**
 * ActionHandler.kt - Interface for action handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-01-26
 */
package com.augmentalis.voiceoscore.accessibility.handlers

/**
 * Interface for action handlers
 * 
 * VOS4 Exception: Interface justified for 6 handler implementations
 * See file header for complete justification
 */
interface ActionHandler {
    
    /**
     * Execute an action
     * 
     * @param category The category of action (for logging/metrics)
     * @param action The action to execute
     * @param params Optional parameters for the action
     * @return true if the action was handled successfully
     */
    fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any> = emptyMap()
    ): Boolean
    
    /**
     * Check if this handler can handle the given action
     * 
     * @param action The action to check
     * @return true if this handler can process the action
     */
    fun canHandle(action: String): Boolean
    
    /**
     * Get supported actions for this handler
     * Used for command discovery and help
     * 
     * @return List of supported action patterns
     */
    fun getSupportedActions(): List<String>
    
    /**
     * Initialize the handler
     * Called once during service startup
     */
    fun initialize() {
        // Default empty implementation
        // Handlers override if needed
    }
    
    /**
     * Dispose resources
     * Called when service is destroyed
     */
    fun dispose() {
        // Default empty implementation
        // Handlers override if needed
    }
}

// Note: ActionCategory is defined in ActionCategory.kt