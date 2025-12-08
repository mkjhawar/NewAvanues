package com.augmentalis.avanues.avamagic.components.themebuilder.UI

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.CornerRadius
import com.augmentalis.avanues.avamagic.components.themebuilder.State.ThemeBuilderStateManager
import com.augmentalis.avanues.avamagic.components.themebuilder.State.ComponentState
import com.augmentalis.avanues.avamagic.components.themebuilder.State.PreviewMode
import com.augmentalis.avanues.avamagic.components.themebuilder.State.ScreenSize

/**
 * Preview canvas that renders components with the current theme
 * This is a conceptual implementation - actual rendering will be platform-specific
 */
class PreviewCanvas(
    private val stateManager: ThemeBuilderStateManager
) {
    /**
     * Component preview data
     */
    data class ComponentPreview(
        val name: String,
        val displayName: String,
        val category: String,
        val description: String,
        val renderFunction: (Theme, ComponentState) -> ComponentPreviewData
    )

    /**
     * Rendered component data for display
     */
    data class ComponentPreviewData(
        val component: String,
        val state: ComponentState,
        val properties: Map<String, Any>
    )

    /**
     * All available components for preview
     */
    val availableComponents = listOf(
        // Foundation Components
        ComponentPreview(
            name = "Button",
            displayName = "Button",
            category = "Foundation",
            description = "Primary action button"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Button",
                state = state,
                properties = mapOf(
                    "text" to "Click Me",
                    "backgroundColor" to when (state) {
                        ComponentState.DEFAULT -> theme.colorScheme.primary
                        ComponentState.HOVER -> theme.colorScheme.primaryContainer
                        ComponentState.PRESSED -> theme.colorScheme.primaryContainer
                        ComponentState.DISABLED -> theme.colorScheme.surfaceVariant
                        else -> theme.colorScheme.primary
                    },
                    "textColor" to when (state) {
                        ComponentState.DISABLED -> theme.colorScheme.onSurfaceVariant
                        else -> theme.colorScheme.onPrimary
                    },
                    "cornerRadius" to theme.shapes.medium,
                    "typography" to theme.typography.labelLarge,
                    "padding" to Spacing.symmetric(
                        vertical = theme.spacing.sm,
                        horizontal = theme.spacing.md
                    )
                )
            )
        },

        ComponentPreview(
            name = "Text",
            displayName = "Text",
            category = "Foundation",
            description = "Text display component"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Text",
                state = state,
                properties = mapOf(
                    "text" to "Sample Text",
                    "color" to theme.colorScheme.onSurface,
                    "typography" to theme.typography.bodyLarge
                )
            )
        },

        ComponentPreview(
            name = "TextField",
            displayName = "Text Field",
            category = "Foundation",
            description = "Text input field"
        ) { theme, state ->
            ComponentPreviewData(
                component = "TextField",
                state = state,
                properties = mapOf(
                    "placeholder" to "Enter text...",
                    "value" to if (state == ComponentState.DEFAULT) "" else "Sample input",
                    "backgroundColor" to theme.colorScheme.surfaceVariant,
                    "textColor" to theme.colorScheme.onSurface,
                    "borderColor" to when (state) {
                        ComponentState.FOCUSED -> theme.colorScheme.primary
                        ComponentState.ERROR -> theme.colorScheme.error
                        else -> theme.colorScheme.outline
                    },
                    "cornerRadius" to theme.shapes.small,
                    "typography" to theme.typography.bodyMedium,
                    "padding" to Spacing.all(theme.spacing.sm)
                )
            )
        },

        ComponentPreview(
            name = "Checkbox",
            displayName = "Checkbox",
            category = "Foundation",
            description = "Checkbox for boolean selection"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Checkbox",
                state = state,
                properties = mapOf(
                    "checked" to (state == ComponentState.PRESSED),
                    "color" to theme.colorScheme.primary,
                    "size" to 24.0f,
                    "cornerRadius" to theme.shapes.extraSmall
                )
            )
        },

        ComponentPreview(
            name = "Switch",
            displayName = "Switch",
            category = "Foundation",
            description = "Toggle switch"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Switch",
                state = state,
                properties = mapOf(
                    "checked" to (state == ComponentState.PRESSED),
                    "activeColor" to theme.colorScheme.primary,
                    "inactiveColor" to theme.colorScheme.surfaceVariant,
                    "thumbColor" to Color.White
                )
            )
        },

        ComponentPreview(
            name = "Icon",
            displayName = "Icon",
            category = "Foundation",
            description = "Icon display"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Icon",
                state = state,
                properties = mapOf(
                    "name" to "home",
                    "size" to 24.0f,
                    "color" to theme.colorScheme.onSurface
                )
            )
        },

        // Layout Components
        ComponentPreview(
            name = "Card",
            displayName = "Card",
            category = "Layout",
            description = "Card container with elevation"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Card",
                state = state,
                properties = mapOf(
                    "backgroundColor" to theme.colorScheme.surface,
                    "cornerRadius" to theme.shapes.medium,
                    "elevation" to theme.elevation.level2,
                    "padding" to Spacing.all(theme.spacing.md)
                )
            )
        },

        ComponentPreview(
            name = "Column",
            displayName = "Column Layout",
            category = "Layout",
            description = "Vertical layout container"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Column",
                state = state,
                properties = mapOf(
                    "spacing" to theme.spacing.md,
                    "padding" to Spacing.all(theme.spacing.sm)
                )
            )
        },

        ComponentPreview(
            name = "Row",
            displayName = "Row Layout",
            category = "Layout",
            description = "Horizontal layout container"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Row",
                state = state,
                properties = mapOf(
                    "spacing" to theme.spacing.md,
                    "padding" to Spacing.all(theme.spacing.sm)
                )
            )
        },

        ComponentPreview(
            name = "Container",
            displayName = "Container",
            category = "Layout",
            description = "Generic container"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Container",
                state = state,
                properties = mapOf(
                    "backgroundColor" to theme.colorScheme.surface,
                    "padding" to Spacing.all(theme.spacing.md)
                )
            )
        },

        ComponentPreview(
            name = "ScrollView",
            displayName = "Scroll View",
            category = "Layout",
            description = "Scrollable container"
        ) { theme, state ->
            ComponentPreviewData(
                component = "ScrollView",
                state = state,
                properties = mapOf(
                    "backgroundColor" to theme.colorScheme.background
                )
            )
        },

        // Advanced Components (placeholders for future)
        ComponentPreview(
            name = "Dialog",
            displayName = "Dialog",
            category = "Feedback",
            description = "Modal dialog"
        ) { theme, state ->
            ComponentPreviewData(
                component = "Dialog",
                state = state,
                properties = mapOf(
                    "backgroundColor" to theme.colorScheme.surface,
                    "cornerRadius" to theme.shapes.large,
                    "elevation" to theme.elevation.level5
                )
            )
        },

        ComponentPreview(
            name = "ListView",
            displayName = "List View",
            category = "Data Display",
            description = "Scrollable list"
        ) { theme, state ->
            ComponentPreviewData(
                component = "ListView",
                state = state,
                properties = mapOf(
                    "itemBackgroundColor" to theme.colorScheme.surface,
                    "selectedItemColor" to theme.colorScheme.primaryContainer,
                    "dividerColor" to theme.colorScheme.outlineVariant
                )
            )
        }
    )

    /**
     * Get components by category
     */
    fun getComponentsByCategory(): Map<String, List<ComponentPreview>> {
        return availableComponents.groupBy { it.category }
    }

    /**
     * Get a specific component preview
     */
    fun getComponent(name: String): ComponentPreview? {
        return availableComponents.find { it.name == name }
    }

    /**
     * Render a component with current theme and state
     */
    fun renderComponent(
        componentName: String,
        componentState: ComponentState = ComponentState.DEFAULT
    ): ComponentPreviewData? {
        val component = getComponent(componentName) ?: return null
        val theme = stateManager.currentState.currentTheme
        return component.renderFunction(theme, componentState)
    }

    /**
     * Render all components for full preview
     */
    fun renderAllComponents(): List<ComponentPreviewData> {
        val theme = stateManager.currentState.currentTheme
        val state = stateManager.currentState.selectedComponentState

        return availableComponents.map { component ->
            component.renderFunction(theme, state)
        }
    }

    /**
     * Generate a preview scene (simulated UI layout)
     */
    fun generatePreviewScene(scene: PreviewScene): SceneData {
        val theme = stateManager.currentState.currentTheme

        return when (scene) {
            PreviewScene.LOGIN -> generateLoginScene(theme)
            PreviewScene.SETTINGS -> generateSettingsScene(theme)
            PreviewScene.DASHBOARD -> generateDashboardScene(theme)
            PreviewScene.COMPONENT_GALLERY -> generateComponentGallery(theme)
        }
    }

    private fun generateLoginScene(theme: Theme): SceneData {
        return SceneData(
            name = "Login Screen",
            description = "A typical login screen with email and password fields",
            components = listOf(
                ComponentInstance("Text", "Welcome Back", mapOf("style" to "displayLarge")),
                ComponentInstance("TextField", "Email", mapOf("type" to "email")),
                ComponentInstance("TextField", "Password", mapOf("type" to "password")),
                ComponentInstance("Button", "Sign In", mapOf("variant" to "primary")),
                ComponentInstance("Button", "Create Account", mapOf("variant" to "outlined"))
            )
        )
    }

    private fun generateSettingsScene(theme: Theme): SceneData {
        return SceneData(
            name = "Settings Screen",
            description = "Settings with switches and options",
            components = listOf(
                ComponentInstance("Text", "Settings", mapOf("style" to "headlineLarge")),
                ComponentInstance("Card", "Notifications", mapOf()),
                ComponentInstance("Switch", "Push Notifications", mapOf("checked" to true)),
                ComponentInstance("Switch", "Email Notifications", mapOf("checked" to false)),
                ComponentInstance("Card", "Appearance", mapOf()),
                ComponentInstance("Switch", "Dark Mode", mapOf("checked" to false))
            )
        )
    }

    private fun generateDashboardScene(theme: Theme): SceneData {
        return SceneData(
            name = "Dashboard",
            description = "Overview dashboard with cards and statistics",
            components = listOf(
                ComponentInstance("Text", "Dashboard", mapOf("style" to "displayMedium")),
                ComponentInstance("Row", "Stats", mapOf("children" to listOf(
                    ComponentInstance("Card", "Users", mapOf()),
                    ComponentInstance("Card", "Revenue", mapOf()),
                    ComponentInstance("Card", "Growth", mapOf())
                ))),
                ComponentInstance("Card", "Recent Activity", mapOf()),
                ComponentInstance("ListView", "Activity List", mapOf())
            )
        )
    }

    private fun generateComponentGallery(theme: Theme): SceneData {
        val components = availableComponents.map { preview ->
            ComponentInstance(preview.name, preview.displayName, mapOf())
        }

        return SceneData(
            name = "Component Gallery",
            description = "All available components",
            components = components
        )
    }
}

/**
 * Preview scene types
 */
enum class PreviewScene {
    LOGIN,
    SETTINGS,
    DASHBOARD,
    COMPONENT_GALLERY
}

/**
 * Scene data for preview
 */
data class SceneData(
    val name: String,
    val description: String,
    val components: List<ComponentInstance>
)

/**
 * Component instance in a scene
 */
data class ComponentInstance(
    val type: String,
    val label: String,
    val properties: Map<String, Any>
)

/**
 * Preview grid overlay
 */
class PreviewGrid(
    val columns: Int = 12,
    val gutter: Float = 16f,
    val color: Color = Color(255, 0, 255, 0.2f) // Magenta with transparency
)

/**
 * Preview spacing guide
 */
class SpacingGuide(
    val theme: Theme
) {
    fun getGuideLines(): List<GuideLine> {
        return listOf(
            GuideLine("XS", theme.spacing.xs, Color(0, 255, 0, 0.3f)),
            GuideLine("SM", theme.spacing.sm, Color(0, 255, 255, 0.3f)),
            GuideLine("MD", theme.spacing.md, Color(255, 255, 0, 0.3f)),
            GuideLine("LG", theme.spacing.lg, Color(255, 128, 0, 0.3f)),
            GuideLine("XL", theme.spacing.xl, Color(255, 0, 0, 0.3f))
        )
    }
}

/**
 * Guide line for spacing visualization
 */
data class GuideLine(
    val label: String,
    val spacing: Float,
    val color: Color
)
