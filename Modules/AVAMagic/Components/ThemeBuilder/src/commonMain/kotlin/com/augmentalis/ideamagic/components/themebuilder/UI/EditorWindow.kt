package com.augmentalis.avanues.avamagic.components.themebuilder.UI

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.CornerRadius
import com.augmentalis.avanues.avamagic.components.themebuilder.State.ThemeBuilderStateManager
import com.augmentalis.avanues.avamagic.components.themebuilder.State.ThemeBuilderState
import com.augmentalis.avanues.avamagic.components.themebuilder.State.ComponentState
import com.augmentalis.avanues.avamagic.components.themebuilder.State.PreviewMode
import com.augmentalis.avanues.avamagic.components.themebuilder.State.ScreenSize
import com.augmentalis.avanues.avamagic.components.themebuilder.Engine.ThemeCompiler
import com.augmentalis.avanues.avamagic.components.themebuilder.Engine.ThemeValidator
import com.augmentalis.avanues.avamagic.components.themebuilder.Engine.ExportFormat
import kotlinx.coroutines.flow.StateFlow

/**
 * Main Theme Editor Window
 * Coordinates the three-panel layout: Component Gallery | Preview Canvas | Property Inspector
 */
class ThemeEditorWindow {
    // State management
    val stateManager = ThemeBuilderStateManager()

    // UI Components
    val previewCanvas = PreviewCanvas(stateManager)
    val propertyInspector = PropertyInspector(stateManager)
    val componentGallery = ComponentGallery(stateManager, previewCanvas)

    // Compiler and validator
    val compiler = ThemeCompiler()
    val validator = ThemeValidator()

    // State flow for reactive UI updates
    val state: StateFlow<ThemeBuilderState> = stateManager.state

    /**
     * Initialize the editor with a theme
     */
    fun initialize(theme: Theme = Themes.Material3Light) {
        stateManager.loadTheme(theme)
    }

    /**
     * Load a predefined theme
     */
    fun loadPredefinedTheme(platform: ThemePlatform) {
        val theme = when (platform) {
            ThemePlatform.Material3_Expressive -> Themes.Material3Light
            ThemePlatform.iOS26_LiquidGlass -> Themes.iOS26LiquidGlass
            ThemePlatform.Windows11_Fluent2 -> Themes.Windows11Fluent2
            ThemePlatform.visionOS2_SpatialGlass -> Themes.visionOS2SpatialGlass
            else -> Themes.Material3Light
        }
        stateManager.loadTheme(theme)
    }

    /**
     * Export current theme to specified format
     */
    fun exportTheme(format: ExportFormat): String {
        val theme = stateManager.currentState.currentTheme
        return when (format) {
            ExportFormat.DSL -> compiler.compileToDSL(theme)
            ExportFormat.YAML -> compiler.compileToYAML(theme)
            ExportFormat.JSON -> compiler.compileToJSON(theme)
            ExportFormat.CSS -> compiler.compileToCSS(theme)
            ExportFormat.ANDROID_XML -> generateAndroidXml(theme)
        }
    }

    /**
     * Validate current theme
     */
    fun validateCurrentTheme(): ThemeValidator.ValidationResult {
        return validator.validate(stateManager.currentState.currentTheme)
    }

    /**
     * Save current theme
     */
    fun saveTheme(): Boolean {
        // Validate before saving
        val validationResult = validateCurrentTheme()
        if (!validationResult.isValid) {
            return false
        }

        stateManager.save()
        return true
    }

    /**
     * Undo last change
     */
    fun undo() {
        stateManager.undo()
    }

    /**
     * Redo last undone change
     */
    fun redo() {
        stateManager.redo()
    }

    /**
     * Reset to default theme
     */
    fun reset() {
        stateManager.reset()
    }

    /**
     * Toggle dark mode preview
     */
    fun toggleDarkMode() {
        stateManager.toggleDarkMode()
    }

    /**
     * Toggle grid overlay
     */
    fun toggleGrid() {
        stateManager.toggleGrid()
    }

    /**
     * Toggle spacing guides
     */
    fun toggleSpacingGuides() {
        stateManager.toggleSpacingGuides()
    }

    /**
     * Set preview mode
     */
    fun setPreviewMode(mode: PreviewMode) {
        stateManager.setPreviewMode(mode)
    }

    /**
     * Set preview scene
     */
    fun setPreviewScene(scene: PreviewScene) {
        componentGallery.currentScene = scene
    }

    /**
     * Get editor actions
     */
    fun getActions(): EditorActions {
        return EditorActions(
            canUndo = stateManager.canUndo(),
            canRedo = stateManager.canRedo(),
            isDirty = stateManager.currentState.isDirty,
            canSave = validateCurrentTheme().isValid
        )
    }

    private fun generateAndroidXml(theme: Theme): String {
        // Simplified Android XML theme generation
        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<!-- Generated Theme: ${theme.name} -->")
            appendLine("<resources>")
            appendLine("    <style name=\"${theme.name.replace(" ", "")}\" parent=\"Theme.Material3.Light\">")

            with(theme.colorScheme) {
                appendLine("        <item name=\"colorPrimary\">${colorToAndroidHex(primary)}</item>")
                appendLine("        <item name=\"colorOnPrimary\">${colorToAndroidHex(onPrimary)}</item>")
                appendLine("        <item name=\"colorSecondary\">${colorToAndroidHex(secondary)}</item>")
                appendLine("        <item name=\"colorOnSecondary\">${colorToAndroidHex(onSecondary)}</item>")
                appendLine("        <item name=\"colorSurface\">${colorToAndroidHex(surface)}</item>")
                appendLine("        <item name=\"colorOnSurface\">${colorToAndroidHex(onSurface)}</item>")
                appendLine("        <item name=\"colorError\">${colorToAndroidHex(error)}</item>")
                appendLine("        <item name=\"colorOnError\">${colorToAndroidHex(onError)}</item>")
            }

            appendLine("    </style>")
            appendLine("</resources>")
        }
    }

    private fun colorToAndroidHex(color: Color): String {
        val a = (color.alpha * 255).toInt().toString(16).padStart(2, '0')
        val r = (color.red * 255).toInt().toString(16).padStart(2, '0')
        val g = (color.green * 255).toInt().toString(16).padStart(2, '0')
        val b = (color.blue * 255).toInt().toString(16).padStart(2, '0')
        return "#$a$r$g$b".uppercase()
    }
}

/**
 * Component Gallery - lists all available components
 */
class ComponentGallery(
    private val stateManager: ThemeBuilderStateManager,
    private val previewCanvas: PreviewCanvas
) {
    var currentScene: PreviewScene = PreviewScene.COMPONENT_GALLERY

    /**
     * Get all components grouped by category
     */
    fun getComponents(): Map<String, List<PreviewCanvas.ComponentPreview>> {
        return previewCanvas.getComponentsByCategory()
    }

    /**
     * Select a component for preview
     */
    fun selectComponent(componentName: String) {
        stateManager.selectComponent(componentName)
    }

    /**
     * Select component state
     */
    fun selectComponentState(state: ComponentState) {
        stateManager.selectComponentState(state)
    }

    /**
     * Get available preview scenes
     */
    fun getScenes(): List<SceneInfo> {
        return listOf(
            SceneInfo(
                scene = PreviewScene.LOGIN,
                name = "Login Screen",
                description = "A typical login form with email and password fields"
            ),
            SceneInfo(
                scene = PreviewScene.SETTINGS,
                name = "Settings Screen",
                description = "Settings panel with switches and options"
            ),
            SceneInfo(
                scene = PreviewScene.DASHBOARD,
                name = "Dashboard",
                description = "Overview dashboard with cards and statistics"
            ),
            SceneInfo(
                scene = PreviewScene.COMPONENT_GALLERY,
                name = "Component Gallery",
                description = "All available components in one view"
            )
        )
    }

    /**
     * Generate current scene
     */
    fun getCurrentScene(): SceneData {
        return previewCanvas.generatePreviewScene(currentScene)
    }
}

/**
 * Scene information
 */
data class SceneInfo(
    val scene: PreviewScene,
    val name: String,
    val description: String
)

/**
 * Editor action states
 */
data class EditorActions(
    val canUndo: Boolean,
    val canRedo: Boolean,
    val isDirty: Boolean,
    val canSave: Boolean
)

/**
 * Hot reload system for live preview
 */
class HotReloadManager(
    private val stateManager: ThemeBuilderStateManager
) {
    private var isEnabled = true
    private val updateCallbacks = mutableListOf<(Theme) -> Unit>()

    /**
     * Enable hot reload
     */
    fun enable() {
        isEnabled = true
    }

    /**
     * Disable hot reload
     */
    fun disable() {
        isEnabled = false
    }

    /**
     * Register a callback for theme updates
     */
    fun onThemeUpdate(callback: (Theme) -> Unit) {
        updateCallbacks.add(callback)
    }

    /**
     * Trigger hot reload
     */
    fun reload() {
        if (!isEnabled) return

        val theme = stateManager.currentState.currentTheme
        updateCallbacks.forEach { it(theme) }
    }

    /**
     * Get reload delay in milliseconds
     */
    var reloadDelay: Long = 300L // Debounce to avoid excessive reloads
}

/**
 * Auto-save manager
 */
class AutoSaveManager(
    private val stateManager: ThemeBuilderStateManager,
    private val saveCallback: (Theme) -> Unit
) {
    private var isEnabled = true
    private var saveInterval: Long = 30_000L // 30 seconds
    private var lastSaveTime: Long = 0

    /**
     * Enable auto-save
     */
    fun enable() {
        isEnabled = true
    }

    /**
     * Disable auto-save
     */
    fun disable() {
        isEnabled = false
    }

    /**
     * Check if auto-save should trigger
     */
    fun shouldSave(): Boolean {
        if (!isEnabled) return false
        val currentTime = System.currentTimeMillis()
        val isDirty = stateManager.currentState.isDirty
        return isDirty && (currentTime - lastSaveTime) >= saveInterval
    }

    /**
     * Perform auto-save
     */
    fun performAutoSave() {
        if (shouldSave()) {
            val theme = stateManager.currentState.currentTheme
            saveCallback(theme)
            lastSaveTime = System.currentTimeMillis()
            stateManager.save()
        }
    }

    /**
     * Set auto-save interval
     */
    fun setInterval(intervalMs: Long) {
        saveInterval = intervalMs
    }
}

/**
 * Theme preset manager
 */
object ThemePresets {
    /**
     * Get all available theme presets
     */
    fun getAllPresets(): List<ThemePreset> {
        return listOf(
            ThemePreset(
                name = "Material Design 3",
                description = "Google's Material Design 3 with dynamic color",
                platform = ThemePlatform.Material3_Expressive,
                theme = Themes.Material3Light
            ),
            ThemePreset(
                name = "iOS 26 Liquid Glass",
                description = "Apple's iOS 26 with translucent glass materials",
                platform = ThemePlatform.iOS26_LiquidGlass,
                theme = Themes.iOS26LiquidGlass
            ),
            ThemePreset(
                name = "Windows 11 Fluent 2",
                description = "Microsoft's Fluent Design System with mica",
                platform = ThemePlatform.Windows11_Fluent2,
                theme = Themes.Windows11Fluent2
            ),
            ThemePreset(
                name = "visionOS 2 Spatial Glass",
                description = "Apple Vision Pro spatial design with depth",
                platform = ThemePlatform.visionOS2_SpatialGlass,
                theme = Themes.visionOS2SpatialGlass
            )
        )
    }

    /**
     * Get preset by platform
     */
    fun getPreset(platform: ThemePlatform): ThemePreset? {
        return getAllPresets().find { it.platform == platform }
    }
}

/**
 * Theme preset data
 */
data class ThemePreset(
    val name: String,
    val description: String,
    val platform: ThemePlatform,
    val theme: Theme
)

/**
 * Keyboard shortcuts for the editor
 */
object KeyboardShortcuts {
    const val UNDO = "Ctrl+Z"
    const val REDO = "Ctrl+Y"
    const val SAVE = "Ctrl+S"
    const val EXPORT = "Ctrl+E"
    const val TOGGLE_DARK_MODE = "Ctrl+D"
    const val TOGGLE_GRID = "Ctrl+G"
    const val TOGGLE_SPACING = "Ctrl+Shift+G"
    const val RESET = "Ctrl+R"

    fun getShortcutMap(): Map<String, String> {
        return mapOf(
            UNDO to "Undo last change",
            REDO to "Redo last undone change",
            SAVE to "Save current theme",
            EXPORT to "Export theme",
            TOGGLE_DARK_MODE to "Toggle dark mode preview",
            TOGGLE_GRID to "Toggle grid overlay",
            TOGGLE_SPACING to "Toggle spacing guides",
            RESET to "Reset to default theme"
        )
    }
}
