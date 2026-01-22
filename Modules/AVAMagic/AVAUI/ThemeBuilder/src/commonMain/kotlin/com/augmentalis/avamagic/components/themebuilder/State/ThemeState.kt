package com.augmentalis.avamagic.components.themebuilder.State

import com.augmentalis.avamagic.core.Theme
import com.augmentalis.avamagic.core.Themes
import com.augmentalis.avamagic.core.ColorScheme
import com.augmentalis.avamagic.core.Typography
import com.augmentalis.avamagic.core.Shapes
import com.augmentalis.avamagic.core.SpacingScale
import com.augmentalis.avamagic.core.ElevationScale
import com.augmentalis.avamagic.core.MaterialSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preview mode for the theme builder
 */
enum class PreviewMode {
    /** Single component preview */
    COMPONENT,

    /** Full screen preview with all components */
    FULL_SCREEN,

    /** Mobile device preview (375x667) */
    MOBILE,

    /** Tablet device preview (768x1024) */
    TABLET,

    /** Desktop preview (1920x1080) */
    DESKTOP
}

/**
 * Component state for preview
 */
enum class ComponentState {
    DEFAULT,
    HOVER,
    PRESSED,
    FOCUSED,
    DISABLED,
    ERROR
}

/**
 * Screen size configuration for previews
 */
data class ScreenSize(
    val name: String,
    val width: Int,
    val height: Int,
    val density: Float = 1.0f
) {
    companion object {
        val MOBILE = ScreenSize("Mobile", 375, 667, 2.0f)
        val TABLET = ScreenSize("Tablet", 768, 1024, 2.0f)
        val DESKTOP = ScreenSize("Desktop", 1920, 1080, 1.0f)
        val VISION_PRO = ScreenSize("Vision Pro", 3840, 2160, 1.0f)
    }
}

/**
 * Main state for the Theme Builder editor
 */
data class ThemeBuilderState(
    /** Current theme being edited */
    val currentTheme: Theme = Themes.Material3Light,

    /** Currently selected component for preview (null = all components) */
    val selectedComponent: String? = null,

    /** Currently selected component state */
    val selectedComponentState: ComponentState = ComponentState.DEFAULT,

    /** Currently selected property in the property inspector */
    val selectedProperty: String? = null,

    /** Preview mode */
    val previewMode: PreviewMode = PreviewMode.FULL_SCREEN,

    /** Screen size for preview */
    val screenSize: ScreenSize = ScreenSize.DESKTOP,

    /** Whether the theme has unsaved changes */
    val isDirty: Boolean = false,

    /** Dark mode toggle for preview */
    val isDarkMode: Boolean = false,

    /** Show grid overlay in preview */
    val showGrid: Boolean = false,

    /** Show spacing guides in preview */
    val showSpacingGuides: Boolean = false,

    /** Auto-save enabled */
    val autoSaveEnabled: Boolean = true,

    /** Last saved timestamp */
    val lastSaved: Long? = null
)

/**
 * History entry for undo/redo
 */
data class HistoryEntry(
    val theme: Theme,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String
)

/**
 * Theme Builder state manager with undo/redo support
 */
class ThemeBuilderStateManager {
    private val _state = MutableStateFlow(ThemeBuilderState())
    val state: StateFlow<ThemeBuilderState> = _state.asStateFlow()

    // Undo/Redo history
    private val history = mutableListOf<HistoryEntry>()
    private var historyIndex = -1
    private val maxHistorySize = 50

    // Current state value
    val currentState: ThemeBuilderState
        get() = _state.value

    /**
     * Update the current theme
     */
    fun updateTheme(theme: Theme, description: String = "Theme updated") {
        addToHistory(theme, description)
        _state.value = _state.value.copy(
            currentTheme = theme,
            isDirty = true
        )
    }

    /**
     * Update color scheme
     */
    fun updateColorScheme(colorScheme: ColorScheme) {
        val newTheme = currentState.currentTheme.copy(colorScheme = colorScheme)
        updateTheme(newTheme, "Color scheme updated")
    }

    /**
     * Update typography
     */
    fun updateTypography(typography: Typography) {
        val newTheme = currentState.currentTheme.copy(typography = typography)
        updateTheme(newTheme, "Typography updated")
    }

    /**
     * Update shapes
     */
    fun updateShapes(shapes: Shapes) {
        val newTheme = currentState.currentTheme.copy(shapes = shapes)
        updateTheme(newTheme, "Shapes updated")
    }

    /**
     * Update spacing
     */
    fun updateSpacing(spacing: SpacingScale) {
        val newTheme = currentState.currentTheme.copy(spacing = spacing)
        updateTheme(newTheme, "Spacing updated")
    }

    /**
     * Update elevation
     */
    fun updateElevation(elevation: ElevationScale) {
        val newTheme = currentState.currentTheme.copy(elevation = elevation)
        updateTheme(newTheme, "Elevation updated")
    }

    /**
     * Update material system
     */
    fun updateMaterial(material: MaterialSystem?) {
        val newTheme = currentState.currentTheme.copy(material = material)
        updateTheme(newTheme, "Material effects updated")
    }

    /**
     * Select a component for preview
     */
    fun selectComponent(componentName: String?) {
        _state.value = _state.value.copy(selectedComponent = componentName)
    }

    /**
     * Select component state
     */
    fun selectComponentState(componentState: ComponentState) {
        _state.value = _state.value.copy(selectedComponentState = componentState)
    }

    /**
     * Select a property in the inspector
     */
    fun selectProperty(propertyName: String?) {
        _state.value = _state.value.copy(selectedProperty = propertyName)
    }

    /**
     * Set preview mode
     */
    fun setPreviewMode(mode: PreviewMode) {
        _state.value = _state.value.copy(previewMode = mode)
    }

    /**
     * Set screen size
     */
    fun setScreenSize(size: ScreenSize) {
        _state.value = _state.value.copy(screenSize = size)
    }

    /**
     * Toggle dark mode
     */
    fun toggleDarkMode() {
        _state.value = _state.value.copy(isDarkMode = !currentState.isDarkMode)
    }

    /**
     * Toggle grid overlay
     */
    fun toggleGrid() {
        _state.value = _state.value.copy(showGrid = !currentState.showGrid)
    }

    /**
     * Toggle spacing guides
     */
    fun toggleSpacingGuides() {
        _state.value = _state.value.copy(showSpacingGuides = !currentState.showSpacingGuides)
    }

    /**
     * Load a predefined theme
     */
    fun loadTheme(theme: Theme) {
        addToHistory(theme, "Theme loaded: ${theme.name}")
        _state.value = _state.value.copy(
            currentTheme = theme,
            isDirty = false
        )
    }

    /**
     * Save the current theme
     */
    fun save() {
        _state.value = _state.value.copy(
            isDirty = false,
            lastSaved = System.currentTimeMillis()
        )
    }

    /**
     * Reset to default state
     */
    fun reset() {
        _state.value = ThemeBuilderState()
        history.clear()
        historyIndex = -1
    }

    // ==================== Undo/Redo ====================

    private fun addToHistory(theme: Theme, description: String) {
        // Remove any history after current index (when undoing then making new changes)
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }

        // Add new entry
        history.add(HistoryEntry(theme, description = description))
        historyIndex++

        // Limit history size
        if (history.size > maxHistorySize) {
            history.removeAt(0)
            historyIndex--
        }
    }

    /**
     * Undo the last change
     */
    fun undo(): Boolean {
        if (!canUndo()) return false

        historyIndex--
        val entry = history[historyIndex]
        _state.value = _state.value.copy(
            currentTheme = entry.theme,
            isDirty = true
        )
        return true
    }

    /**
     * Redo the last undone change
     */
    fun redo(): Boolean {
        if (!canRedo()) return false

        historyIndex++
        val entry = history[historyIndex]
        _state.value = _state.value.copy(
            currentTheme = entry.theme,
            isDirty = true
        )
        return true
    }

    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = historyIndex > 0

    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = historyIndex < history.size - 1

    /**
     * Get history for display
     */
    fun getHistory(): List<HistoryEntry> = history.toList()
}
