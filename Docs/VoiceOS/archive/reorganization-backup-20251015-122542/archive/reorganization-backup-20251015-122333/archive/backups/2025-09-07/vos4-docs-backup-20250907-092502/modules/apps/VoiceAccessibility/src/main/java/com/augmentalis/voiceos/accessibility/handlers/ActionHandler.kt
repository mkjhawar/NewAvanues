/**
 * ActionHandler.kt - Interface for action handlers
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-26
 * 
 * ============================================================================
 * VOS4 INTERFACE EXCEPTION - APPROVED AND JUSTIFIED
 * ============================================================================
 * 
 * This interface is an APPROVED EXCEPTION to VOS4's direct implementation rule.
 * 
 * JUSTIFICATION (via COT/ROT/TOT Analysis):
 * 1. MULTIPLE IMPLEMENTATIONS: 6 different handler types require polymorphic dispatch
 *    - SystemHandler, AppHandler, DeviceHandler, InputHandler, NavigationHandler, UIHandler
 * 
 * 2. TYPE SAFETY: Collection storage and iteration requires common type
 *    - ActionCoordinator manages handlers in a Map<ActionCategory, ActionHandler>
 *    - Without interface: Complex casting and type checking needed
 * 
 * 3. EXTENSIBILITY: New handler types can be added without modifying core
 *    - Follows Open/Closed Principle
 *    - Plugin-like architecture for handlers
 * 
 * 4. PERFORMANCE: Interface overhead minimal compared to benefits
 *    - Virtual dispatch cost negligible for command execution
 *    - Cleaner code outweighs microsecond overhead
 * 
 * 5. SIMILAR TO APPROVED PATTERNS:
 *    - SpeechRecognition uses IRecognitionEngine for 6 engines
 *    - Same pattern, same justification
 * 
 * ALTERNATIVES CONSIDERED AND REJECTED:
 * - Sealed classes: Still abstraction, more complex
 * - Direct functions: No polymorphism, violates DRY
 * - Type checking: Error-prone, violates type safety
 * 
 * APPROVAL: This interface exception follows VOS4 Interface Exception Process
 * ============================================================================
 */
package com.augmentalis.voiceos.accessibility.handlers

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

/**
 * Action categories for organization and metrics
 * Direct enum - no abstraction
 */
enum class ActionCategory {
    SYSTEM,      // System-level actions (back, home, settings)
    APP,         // Application launch and control
    DEVICE,      // Device control (volume, brightness, etc)
    INPUT,       // Text input and keyboard control
    NAVIGATION,  // UI navigation and scrolling
    UI,          // UI element interaction
    GESTURE,     // Gesture-based interactions (pinch, drag, swipe)
    GAZE,        // Gaze tracking and eye-based interactions
    CUSTOM       // Custom/plugin actions
}