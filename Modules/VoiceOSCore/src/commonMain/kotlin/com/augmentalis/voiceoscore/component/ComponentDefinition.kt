/**
 * ComponentDefinition.kt - Data classes for YAML component structure
 *
 * Provides comprehensive data models for parsing YAML-defined UI components.
 * Supports the complete AVAUI YAML format used by VoiceOSCoreNG overlays.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 *
 * YAML Component Structure:
 * - component: Name, type, platform, description
 * - theme: Inherited theme and token overrides
 * - layout: Widget tree with props, children, conditions
 * - data: Input properties and computed values
 * - functions: Custom logic functions
 * - states/animations: UI state machine definitions
 * - accessibility: A11y configurations
 */
package com.augmentalis.voiceoscore

// ═══════════════════════════════════════════════════════════════════════════════
// CORE COMPONENT DEFINITION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Root definition for a YAML-defined UI component.
 *
 * Represents the complete parsed structure of an AVAUI YAML file.
 *
 * Example YAML:
 * ```yaml
 * component:
 *   name: ElementOverlay
 *   type: overlay
 *   platform: all
 * layout:
 *   type: stack
 *   children: [...]
 * ```
 */
data class ComponentDefinition(
    /** Component metadata */
    val component: ComponentMetadata,

    /** Theme configuration with token overrides */
    val theme: ThemeConfig? = null,

    /** Root layout widget tree */
    val layout: LayoutDefinition,

    /** Data bindings and inputs */
    val data: Map<String, DataBinding> = emptyMap(),

    /** Custom functions defined in YAML */
    val functions: Map<String, FunctionDefinition> = emptyMap(),

    /** State definitions for state machine */
    val states: List<StateDefinition> = emptyList(),

    /** Animation definitions */
    val animations: Map<String, AnimationDefinition> = emptyMap(),

    /** Accessibility configuration */
    val accessibility: AccessibilityConfig? = null,

    /** Mode configurations (lite/dev) */
    val modes: Map<String, ModeConfig> = emptyMap()
)

/**
 * Component metadata from YAML 'component:' section.
 */
data class ComponentMetadata(
    /** Unique name for the component */
    val name: String,

    /** Component type: overlay, screen, widget */
    val type: ComponentType,

    /** Target platforms: all, android, ios, desktop */
    val platform: String = "all",

    /** Human-readable description */
    val description: String = ""
)

/**
 * Types of components that can be defined.
 */
enum class ComponentType {
    /** Full-screen or floating overlay */
    OVERLAY,

    /** Complete screen/page */
    SCREEN,

    /** Reusable widget/component */
    WIDGET,

    /** Dialog or modal */
    DIALOG
}

// ═══════════════════════════════════════════════════════════════════════════════
// THEME CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Theme configuration from YAML 'theme:' section.
 *
 * Allows components to inherit from a base theme and override specific tokens.
 */
data class ThemeConfig(
    /** Base theme to inherit from */
    val inherit: String = "VoiceOSCoreNGTheme",

    /** Token overrides (name -> value) */
    val tokens: Map<String, String> = emptyMap()
)

// ═══════════════════════════════════════════════════════════════════════════════
// LAYOUT & WIDGET DEFINITIONS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Layout definition representing a widget tree structure.
 *
 * Can represent a single widget or a container with children.
 */
data class LayoutDefinition(
    /** Layout type: stack, column, row, box */
    val type: LayoutType,

    /** Unique identifier for this layout node */
    val id: String = "",

    /** Layout-level properties */
    val props: WidgetProps = WidgetProps(),

    /** Child widgets */
    val children: List<WidgetDefinition> = emptyList(),

    /** Template for dynamic list rendering */
    val template: TemplateDefinition? = null
)

/**
 * Supported layout container types.
 */
enum class LayoutType {
    /** Layered/overlapping children (Z-stack) */
    STACK,

    /** Vertical arrangement */
    COLUMN,

    /** Horizontal arrangement */
    ROW,

    /** Flexible box layout */
    BOX,

    /** Absolute positioning */
    ABSOLUTE
}

/**
 * Definition for a single widget in the tree.
 */
data class WidgetDefinition(
    /** Widget type: Container, Text, Icon, Badge, etc. */
    val widget: WidgetType,

    /** Unique identifier */
    val id: String = "",

    /** Conditional rendering expression */
    val condition: String? = null,

    /** Widget properties */
    val props: WidgetProps = WidgetProps(),

    /** Child widgets (for containers) */
    val children: List<WidgetDefinition> = emptyList(),

    /** Position for POSITIONED widgets */
    val position: PositionDefinition? = null,

    /** Accessibility attributes */
    val accessibility: WidgetAccessibility? = null
)

/**
 * Supported widget types matching AVAUI YAML spec.
 */
enum class WidgetType {
    // Layout containers
    CONTAINER,
    COLUMN,
    ROW,
    BOX,
    STACK,

    // Basic widgets
    TEXT,
    ICON,
    IMAGE,

    // Interactive widgets
    BUTTON,
    BADGE,

    // Progress indicators
    PROGRESS_BAR,

    // Custom/composite widgets
    INSTRUCTION_BAR,
    BADGE_LAYER,
    NUMBERED_BADGE,
    TOOLTIP,
    STATUS_CARD,
    STATUS_INDICATOR,
    PULSE_RING,
    CONFIDENCE_BAR,

    // Spacer
    SPACER,

    // Unknown/extensible
    CUSTOM;

    companion object {
        /**
         * Parse widget type from YAML string.
         */
        fun fromString(value: String): WidgetType {
            return when (value.uppercase().replace("-", "_").replace(" ", "_")) {
                "CONTAINER" -> CONTAINER
                "COLUMN" -> COLUMN
                "ROW" -> ROW
                "BOX" -> BOX
                "STACK" -> STACK
                "TEXT" -> TEXT
                "ICON" -> ICON
                "IMAGE" -> IMAGE
                "BUTTON" -> BUTTON
                "BADGE" -> BADGE
                "PROGRESSBAR", "PROGRESS_BAR" -> PROGRESS_BAR
                "INSTRUCTIONBAR", "INSTRUCTION_BAR" -> INSTRUCTION_BAR
                "BADGELAYER", "BADGE_LAYER" -> BADGE_LAYER
                "NUMBEREDBADGE", "NUMBERED_BADGE" -> NUMBERED_BADGE
                "TOOLTIP" -> TOOLTIP
                "STATUSCARD", "STATUS_CARD" -> STATUS_CARD
                "STATUSINDICATOR", "STATUS_INDICATOR" -> STATUS_INDICATOR
                "PULSERING", "PULSE_RING" -> PULSE_RING
                "CONFIDENCEBAR", "CONFIDENCE_BAR" -> CONFIDENCE_BAR
                "SPACER" -> SPACER
                else -> CUSTOM
            }
        }
    }
}

/**
 * Widget properties from YAML 'props:' section.
 *
 * Contains all possible properties - platform renderers use relevant ones.
 */
data class WidgetProps(
    // ═════ Layout Properties ═════
    val width: DimensionValue? = null,
    val height: DimensionValue? = null,
    val minWidth: DimensionValue? = null,
    val maxWidth: DimensionValue? = null,
    val minHeight: DimensionValue? = null,
    val maxHeight: DimensionValue? = null,

    /** Fill available width */
    val fillMaxWidth: Boolean = false,

    /** Fill available height */
    val fillMaxHeight: Boolean = false,

    /** Fill both dimensions */
    val fillMaxSize: Boolean = false,

    /** Weight for flex layouts */
    val weight: Float? = null,

    // ═════ Spacing Properties ═════
    /** Padding (all sides, or individual via PaddingValue) */
    val padding: PaddingValue? = null,

    /** Margin (all sides, or individual) */
    val margin: PaddingValue? = null,

    /** Spacing between children (for Column/Row) */
    val spacing: String? = null,

    // ═════ Visual Properties ═════
    /** Background color (theme token or hex) */
    val background: String? = null,

    /** Corner radius */
    val cornerRadius: String? = null,

    /** Elevation/shadow */
    val elevation: String? = null,

    /** Shadow color */
    val shadowColor: String? = null,

    /** Shadow radius */
    val shadowRadius: String? = null,

    /** Shadow Y offset */
    val shadowOffsetY: String? = null,

    /** Border width */
    val borderWidth: String? = null,

    /** Border color */
    val borderColor: String? = null,

    /** Shape type */
    val shape: String? = null,

    /** Clip content to bounds */
    val clipToBounds: Boolean? = null,

    /** Opacity (0.0 to 1.0) */
    val opacity: Float? = null,

    // ═════ Alignment Properties ═════
    /** Content alignment */
    val alignment: String? = null,

    /** Horizontal alignment */
    val horizontalAlignment: String? = null,

    /** Vertical alignment */
    val verticalAlignment: String? = null,

    // ═════ Text Properties ═════
    /** Text content (can be binding expression) */
    val text: String? = null,

    /** Text color */
    val color: String? = null,

    /** Font size */
    val fontSize: String? = null,

    /** Font weight */
    val fontWeight: String? = null,

    /** Text alignment */
    val textAlign: String? = null,

    /** Maximum lines */
    val maxLines: Int? = null,

    /** Text style preset (e.g., "instruction", "caption") */
    val style: String? = null,

    // ═════ Icon Properties ═════
    /** Icon name/resource */
    val icon: String? = null,

    /** Icon size */
    val size: String? = null,

    /** Animated icon */
    val animated: Boolean? = null,

    // ═════ Progress Properties ═════
    /** Progress value (0.0 to 1.0) */
    val progress: String? = null,

    /** Progress background color */
    val backgroundColor: String? = null,

    /** Progress fill color */
    val progressColor: String? = null,

    // ═════ State Properties ═════
    /** Component state (for stateful widgets) */
    val state: String? = null,

    /** Number value (for badges) */
    val number: String? = null,

    /** Label text */
    val label: String? = null,

    /** Show label flag */
    val showLabel: Boolean? = null,

    /** Value (generic) */
    val value: String? = null,

    // ═════ Animation Properties ═════
    /** Animation duration */
    val duration: Int? = null,

    /** Repeat mode */
    val repeat: String? = null,

    // ═════ Raw Properties ═════
    /** Additional properties not covered above */
    val extra: Map<String, Any> = emptyMap()
)

/**
 * Dimension value that can be absolute (dp), percentage, or token reference.
 */
data class DimensionValue(
    val value: String
) {
    val isToken: Boolean get() = value.startsWith("\${")
    val isPercentage: Boolean get() = value.endsWith("%")
    val numericValue: Float?
        get() = value.replace("%", "").toFloatOrNull()
}

/**
 * Padding/margin value supporting uniform or per-side values.
 */
data class PaddingValue(
    val all: String? = null,
    val top: String? = null,
    val bottom: String? = null,
    val left: String? = null,
    val right: String? = null,
    val horizontal: String? = null,
    val vertical: String? = null
) {
    companion object {
        /** Parse from YAML value (string or map) */
        fun fromYaml(value: Any?): PaddingValue? {
            return when (value) {
                null -> null
                is String -> PaddingValue(all = value)
                is Number -> PaddingValue(all = value.toString())
                is Map<*, *> -> PaddingValue(
                    all = value["all"]?.toString(),
                    top = value["top"]?.toString(),
                    bottom = value["bottom"]?.toString(),
                    left = value["left"]?.toString(),
                    right = value["right"]?.toString(),
                    horizontal = value["horizontal"]?.toString(),
                    vertical = value["vertical"]?.toString()
                )
                else -> null
            }
        }
    }
}

/**
 * Position definition for absolute positioning.
 */
data class PositionDefinition(
    val x: String? = null,
    val y: String? = null,
    val offsetX: String? = null,
    val offsetY: String? = null
)

/**
 * Widget-level accessibility attributes.
 */
data class WidgetAccessibility(
    /** ARIA role */
    val role: String? = null,

    /** Content description for screen readers */
    val contentDescription: String? = null,

    /** Live region mode (polite, assertive, off) */
    val liveRegion: String? = null,

    /** Whether element is enabled */
    val enabled: String? = null,

    /** Minimum touch target size */
    val minTouchTarget: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// TEMPLATE (FOR LOOPS)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Template definition for dynamic list rendering.
 *
 * Used for forEach loops in YAML:
 * ```yaml
 * template:
 *   forEach: ${items}
 *   as: item
 *   render: [...]
 * ```
 */
data class TemplateDefinition(
    /** Collection expression to iterate */
    val forEach: String,

    /** Variable name for current item */
    val `as`: String,

    /** Widget(s) to render per item */
    val render: List<WidgetDefinition>
)

// ═══════════════════════════════════════════════════════════════════════════════
// DATA BINDINGS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Data binding definition from YAML 'data:' section.
 */
data class DataBinding(
    /** Kotlin/Swift type name */
    val type: String,

    /** Whether this binding is required */
    val required: Boolean = false,

    /** Default value */
    val default: Any? = null,

    /** Enum values (if applicable) */
    val enum: List<String>? = null,

    /** Human-readable description */
    val description: String = "",

    /** Whether this is a computed value */
    val computed: String? = null,

    /** Minimum value (for numbers) */
    val min: Number? = null,

    /** Maximum value (for numbers) */
    val max: Number? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Function definition from YAML 'functions:' section.
 */
data class FunctionDefinition(
    /** Parameter names */
    val params: List<String>,

    /** Return type */
    val returns: String,

    /** Function logic (pseudo-code) */
    val logic: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// STATES & ANIMATIONS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * State definition for component state machine.
 */
data class StateDefinition(
    /** State name */
    val name: String,

    /** Description of this state */
    val description: String = "",

    /** Properties in this state */
    val props: Map<String, Any> = emptyMap()
)

/**
 * Animation definition from YAML 'animations:' section.
 */
data class AnimationDefinition(
    /** Duration in milliseconds */
    val duration: String,

    /** Easing function */
    val easing: String? = null,

    /** Repeat mode (infinite, once) */
    val repeat: String? = null,

    /** Stagger delay for list animations */
    val staggerDelay: Int? = null,

    /** Animated properties */
    val properties: Map<String, AnimationProperty> = emptyMap()
)

/**
 * Single animated property definition.
 */
data class AnimationProperty(
    /** Starting value */
    val from: Any? = null,

    /** Ending value */
    val to: Any? = null,

    /** Keyframes for complex animations */
    val keyframes: List<Any>? = null,

    /** Whether to use CSS transition */
    val transition: Boolean? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// ACCESSIBILITY
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Component-level accessibility configuration.
 */
data class AccessibilityConfig(
    /** ARIA role for the component */
    val role: String? = null,

    /** Live region mode */
    val liveRegion: String? = null,

    /** Default content description */
    val contentDescription: String? = null,

    /** Announcement templates for various events */
    val announcements: Map<String, String> = emptyMap(),

    /** Focus navigation configuration */
    val navigation: AccessibilityNavigation? = null
)

/**
 * Accessibility navigation configuration.
 */
data class AccessibilityNavigation(
    /** Whether component supports focus */
    val supportsFocus: Boolean = true,

    /** Whether focus is trapped within component */
    val trapFocus: Boolean = false,

    /** Focus order (sequential, visual) */
    val focusOrder: String = "sequential",

    /** Focus indicator width */
    val focusIndicatorWidth: String? = null,

    /** Focus indicator color */
    val focusIndicatorColor: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// MODE CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Mode configuration for lite/dev variants.
 */
data class ModeConfig(
    /** Maximum items (for lists) */
    val maxItems: Int? = null,

    /** Show instructions flag */
    val showInstructions: Boolean? = null,

    /** Badge style override */
    val badgeStyle: String? = null,

    /** Animation settings */
    val animations: AnimationModeConfig? = null,

    /** Show debug info */
    val enableDebugInfo: Boolean? = null,

    /** Show confidence */
    val showConfidence: Boolean? = null,

    /** Auto-dismiss delay */
    val autoDismissDelay: Int? = null,

    /** Show timestamp */
    val showTimestamp: Boolean? = null
)

/**
 * Animation settings within mode config.
 */
data class AnimationModeConfig(
    val enabled: Boolean = true
)
