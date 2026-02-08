package com.augmentalis.actions

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Category Capability Registry - Open/Closed Principle compliant
 *
 * Created as part of SOLID refactoring (P2) to extract hardcoded category sets
 * from IntentRouter. Now categories can be added/modified without changing
 * the IntentRouter class.
 *
 * Benefits:
 * - Open for extension (add new categories without modifying existing code)
 * - Closed for modification (IntentRouter doesn't need changes)
 * - Database-ready (can be backed by Room entity in future)
 * - Testable (easy to mock for testing)
 *
 * Future: Move category definitions to database for runtime configuration.
 *
 * @author Manoj Jhawar
 * @since 2025-12-05
 */
@Singleton
class CategoryCapabilityRegistry @Inject constructor() {

    companion object {
        private const val TAG = "CategoryCapabilityRegistry"
    }

    /**
     * Execution target for a category.
     */
    enum class ExecutionTarget {
        /** Execute locally in AVA (has permissions/capabilities) */
        AVA_LOCAL,
        /** Forward to VoiceOS (requires accessibility service) */
        VOICEOS,
        /** Unknown category, fall back to LLM */
        FALLBACK_LLM
    }

    /**
     * Category definition with metadata.
     */
    data class CategoryDefinition(
        val id: String,
        val displayName: String,
        val executionTarget: ExecutionTarget,
        val description: String,
        val requiresAccessibility: Boolean = false
    )

    // ==================== Category Registry ====================

    private val categories = mutableMapOf<String, CategoryDefinition>()

    init {
        registerDefaultCategories()
    }

    /**
     * Register default categories (extracted from IntentRouter).
     */
    private fun registerDefaultCategories() {
        // AVA-capable categories (execute locally)
        registerAVACategory("connectivity", "Connectivity", "WiFi, Bluetooth control")
        registerAVACategory("volume", "Volume", "Volume control")
        registerAVACategory("media", "Media", "Play, pause, skip")
        registerAVACategory("system", "System", "Settings, apps")
        registerAVACategory("navigation", "Navigation", "App navigation")
        registerAVACategory("productivity", "Productivity", "Note-taking, reminders")
        registerAVACategory("smart_home", "Smart Home", "Smart home control")
        registerAVACategory("information", "Information", "Weather, time, etc.")
        registerAVACategory("calculation", "Calculation", "Math operations")
        registerAVACategory("communication", "Communication", "Email, SMS, calls")

        // VoiceOS-only categories (require accessibility service)
        registerVoiceOSCategory("gesture", "Gestures", "Swipe gestures")
        registerVoiceOSCategory("cursor", "Cursor", "Cursor movement")
        registerVoiceOSCategory("scroll", "Scroll", "Scroll operations")
        registerVoiceOSCategory("swipe", "Swipe", "Swipe gestures")
        registerVoiceOSCategory("drag", "Drag", "Drag operations")
        registerVoiceOSCategory("keyboard", "Keyboard", "Keyboard input simulation")
        registerVoiceOSCategory("editing", "Editing", "Text editing via accessibility")
        registerVoiceOSCategory("gaze", "Gaze", "Gaze tracking")
        registerVoiceOSCategory("overlays", "Overlays", "Overlay management")
        registerVoiceOSCategory("dialog", "Dialog", "Dialog control")
        registerVoiceOSCategory("menu", "Menu", "Menu navigation")
        registerVoiceOSCategory("dictation", "Dictation", "Dictation mode")
        
        // Expanded VoiceOS Categories (Phase 3 Expanded)
        registerVoiceOSCategory("clipboard", "Clipboard", "Copy, paste, cut")
        registerVoiceOSCategory("app_interaction", "App Interaction", "Deep in-app search/actions")
        registerVoiceOSCategory("vision", "Vision", "Screen reading and analysis")
        registerVoiceOSCategory("media_casting", "Media Casting", "Cast screen or media to devices")
        
        Log.i(TAG, "Registered ${categories.size} categories")
    }

    /**
     * Register an AVA-capable category.
     */
    fun registerAVACategory(id: String, displayName: String, description: String) {
        categories[id] = CategoryDefinition(
            id = id,
            displayName = displayName,
            executionTarget = ExecutionTarget.AVA_LOCAL,
            description = description,
            requiresAccessibility = false
        )
    }

    /**
     * Register a VoiceOS-only category.
     */
    fun registerVoiceOSCategory(id: String, displayName: String, description: String) {
        categories[id] = CategoryDefinition(
            id = id,
            displayName = displayName,
            executionTarget = ExecutionTarget.VOICEOS,
            description = description,
            requiresAccessibility = true
        )
    }

    /**
     * Register a custom category.
     */
    fun registerCategory(category: CategoryDefinition) {
        categories[category.id] = category
        Log.d(TAG, "Registered category: ${category.id} -> ${category.executionTarget}")
    }

    // ==================== Query Methods ====================

    /**
     * Get execution target for a category.
     */
    fun getExecutionTarget(categoryId: String): ExecutionTarget {
        return categories[categoryId]?.executionTarget ?: ExecutionTarget.FALLBACK_LLM
    }

    /**
     * Check if category is AVA-capable.
     */
    fun isAVACapable(categoryId: String): Boolean {
        return getExecutionTarget(categoryId) == ExecutionTarget.AVA_LOCAL
    }

    /**
     * Check if category requires VoiceOS.
     */
    fun requiresVoiceOS(categoryId: String): Boolean {
        return getExecutionTarget(categoryId) == ExecutionTarget.VOICEOS
    }

    /**
     * Check if category requires accessibility service.
     */
    fun requiresAccessibility(categoryId: String): Boolean {
        return categories[categoryId]?.requiresAccessibility ?: false
    }

    /**
     * Get all AVA-capable category IDs.
     */
    fun getAVACapableCategories(): Set<String> {
        return categories.filterValues { it.executionTarget == ExecutionTarget.AVA_LOCAL }.keys
    }

    /**
     * Get all VoiceOS-only category IDs.
     */
    fun getVoiceOSCategories(): Set<String> {
        return categories.filterValues { it.executionTarget == ExecutionTarget.VOICEOS }.keys
    }

    /**
     * Get category definition by ID.
     */
    fun getCategory(id: String): CategoryDefinition? = categories[id]

    /**
     * Get all registered categories.
     */
    fun getAllCategories(): List<CategoryDefinition> = categories.values.toList()

    /**
     * Get statistics about registered categories.
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "total_categories" to categories.size,
            "ava_capable_count" to getAVACapableCategories().size,
            "voiceos_count" to getVoiceOSCategories().size
        )
    }
}
