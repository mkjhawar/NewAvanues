/**
 * VoiceUIDesigner.kt - Complete Visual UI Design System
 * 
 * Comprehensive UI system with visual editor, drag-and-drop, theming,
 * spatial positioning, and logic binding capabilities.
 */

package com.augmentalis.voiceui.designer

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.uuidmanager.UUIDManager
import com.augmentalis.uuidmanager.models.UUIDElement
import com.augmentalis.uuidmanager.models.UUIDPosition
import com.augmentalis.voiceui.api.AIContext
import com.augmentalis.voiceui.theming.CustomTheme

/**
 * Complete VoiceUI Design System Components
 * Research-based comprehensive UI system for 2025
 */

// ================================
// CORE UI SYSTEM COMPONENTS
// ================================

/**
 * Visual Element with complete styling and positioning
 */
data class VoiceUIElement(
    // Core properties
    val uuid: String = UUIDManager.generate(),
    val type: ElementType,
    val name: String,
    
    // Visual properties
    val position: SpatialPosition = SpatialPosition(),
    val styling: ElementStyling = ElementStyling(),
    val theme: CustomTheme? = null,  // Direct use of CustomTheme, null = use default
    
    // Interaction properties  
    val interactions: InteractionSet = InteractionSet(),
    val voiceCommands: VoiceCommandSet = VoiceCommandSet(),
    val gestures: GestureSet = GestureSet(),
    
    // Logic binding
    val logicBinding: LogicBinding? = null,
    val dataBinding: DataBinding? = null,
    
    // AI context
    val aiContext: AIContext? = null,
    
    // Design system properties
    val accessibility: AccessibilityProps = AccessibilityProps(),
    val responsive: ResponsiveProps = ResponsiveProps(),
    val animation: AnimationProps = AnimationProps()
)

/**
 * All supported UI element types (research-based)
 */
enum class ElementType {
    // Input Controls
    BUTTON, ICON_BUTTON, FAB, CHIP_BUTTON,
    TEXT_FIELD, PASSWORD_FIELD, SEARCH_FIELD, TEXT_AREA,
    CHECKBOX, RADIO_BUTTON, SWITCH, TOGGLE,
    SLIDER, RANGE_SLIDER, STEPPER,
    DROPDOWN, SELECT, COMBOBOX, AUTOCOMPLETE,
    DATE_PICKER, TIME_PICKER, COLOR_PICKER, FILE_PICKER,
    
    // Navigation
    TAB_BAR, NAVIGATION_BAR, BREADCRUMB, PAGINATION,
    SIDEBAR, DRAWER, BOTTOM_NAV, TOP_BAR,
    MENU, CONTEXT_MENU, ACTION_SHEET,
    
    // Layout
    CONTAINER, CARD, PANEL, SECTION,
    GRID, FLEX_BOX, STACK, ROW, COLUMN,
    SPACER, DIVIDER, SEPARATOR,
    SCROLL_VIEW, LIST, VIRTUAL_LIST,
    
    // Content Display
    TEXT, HEADING, LABEL, CAPTION,
    IMAGE, ICON, AVATAR, THUMBNAIL,
    VIDEO, AUDIO, MEDIA_PLAYER,
    CHART, GRAPH, DATA_VIZ,
    CODE_BLOCK, SYNTAX_HIGHLIGHTER,
    
    // Feedback & Status
    ALERT, TOAST, SNACKBAR, BANNER,
    PROGRESS_BAR, PROGRESS_CIRCLE, LOADING_SPINNER,
    BADGE, STATUS_DOT, INDICATOR,
    TOOLTIP, POPOVER, MODAL, DIALOG,
    
    // Advanced
    MAP, CALENDAR, TABLE, DATA_GRID,
    TREE_VIEW, ACCORDION, COLLAPSIBLE,
    CAROUSEL, IMAGE_GALLERY, SLIDESHOW,
    RICH_TEXT_EDITOR, WYSIWYG,
    
    // VoiceUI Specific
    VOICE_ACTIVATOR, SPATIAL_WINDOW, AR_OVERLAY,
    GESTURE_ZONE, VOICE_FEEDBACK, HUD_ELEMENT
}

/**
 * Spatial positioning in 3D space
 */
data class SpatialPosition(
    // 2D position
    val x: Float = 0f,
    val y: Float = 0f,
    
    // 3D depth
    val z: Float = 0f,           // Forward/backward in space
    val depth: DepthLayer = DepthLayer.FLAT,
    
    // Size
    val width: Float = 100f,
    val height: Float = 50f,
    
    // 3D rotation
    val rotationX: Float = 0f,   // Pitch
    val rotationY: Float = 0f,   // Yaw  
    val rotationZ: Float = 0f,   // Roll
    
    // Anchoring
    val anchor: AnchorPoint = AnchorPoint.CENTER,
    val relativeTo: String? = null,  // UUID of parent element
    
    // AR/VR specific
    val worldPosition: WorldPosition? = null,
    val isWorldLocked: Boolean = false
)

/**
 * Depth layers for spatial UI
 */
enum class DepthLayer(val zIndex: Float) {
    BACKGROUND(-100f),
    FLAT(0f),              // Default 2D layer
    ELEVATED(10f),         // Cards, buttons
    FLOATING(20f),         // Tooltips, dropdowns
    MODAL(30f),           // Dialogs, overlays
    HUD(40f),             // Always on top UI
    SPATIAL_NEAR(100f),   // Close AR objects
    SPATIAL_FAR(200f)     // Distant AR objects
}

/**
 * Anchor points for positioning
 */
enum class AnchorPoint {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

/**
 * World position for AR/VR
 */
data class WorldPosition(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val worldX: Float = 0f,
    val worldY: Float = 0f,
    val worldZ: Float = 0f
)

/**
 * Complete styling system
 */
data class ElementStyling(
    // Colors
    val backgroundColor: Color = Color.Transparent,
    val foregroundColor: Color = Color.Black,
    val borderColor: Color = Color.Gray,
    val shadowColor: Color = Color.Black.copy(alpha = 0.3f),
    
    // Typography
    val fontSize: Float = 16f,
    val fontWeight: FontWeight = FontWeight.NORMAL,
    val fontFamily: String = "system",
    val textAlign: TextAlign = TextAlign.START,
    
    // Layout
    val padding: EdgeInsets = EdgeInsets(),
    val margin: EdgeInsets = EdgeInsets(),
    val borderWidth: Float = 0f,
    val borderRadius: Float = 0f,
    
    // Visual effects
    val shadow: ShadowStyle = ShadowStyle(),
    val blur: Float = 0f,
    val opacity: Float = 1f,
    val gradient: GradientStyle? = null,
    
    // States
    val hoverStyle: ElementStyling? = null,
    val focusStyle: ElementStyling? = null,
    val activeStyle: ElementStyling? = null,
    val disabledStyle: ElementStyling? = null
)

data class EdgeInsets(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
) {
    constructor(all: Float) : this(all, all, all, all)
    constructor(vertical: Float, horizontal: Float) : this(vertical, horizontal, vertical, horizontal)
}

data class ShadowStyle(
    val offsetX: Float = 0f,
    val offsetY: Float = 2f,
    val blurRadius: Float = 4f,
    val spreadRadius: Float = 0f
)

data class GradientStyle(
    val colors: List<Color>,
    val direction: GradientDirection = GradientDirection.VERTICAL
)

enum class GradientDirection { HORIZONTAL, VERTICAL, DIAGONAL, RADIAL }
enum class FontWeight { LIGHT, NORMAL, BOLD, EXTRA_BOLD }
enum class TextAlign { START, CENTER, END, JUSTIFY }

// UITheme removed - using CustomTheme directly for zero overhead
// Pre-built themes are now created as CustomTheme instances

// Theme data classes
abstract class ColorScheme {
    abstract val primary: Color
    abstract val secondary: Color
    abstract val background: Color
    abstract val surface: Color
    abstract val error: Color
    abstract val onPrimary: Color
    abstract val onSecondary: Color
    abstract val onBackground: Color
    abstract val onSurface: Color
    abstract val onError: Color
}

abstract class TypographyTheme {
    abstract val h1: VoiceUITextStyle
    abstract val h2: VoiceUITextStyle
    abstract val h3: VoiceUITextStyle
    abstract val body1: VoiceUITextStyle
    abstract val body2: VoiceUITextStyle
    abstract val caption: VoiceUITextStyle
}

abstract class SpacingTheme {
    abstract val xs: Float
    abstract val sm: Float
    abstract val md: Float
    abstract val lg: Float
    abstract val xl: Float
}

data class VoiceUITextStyle(
    val fontSize: Float,
    val fontWeight: FontWeight,
    val fontFamily: String
)

/**
 * Interaction system
 */
data class InteractionSet(
    val onClick: (() -> Unit)? = null,
    val onDoubleClick: (() -> Unit)? = null,
    val onLongPress: (() -> Unit)? = null,
    val onHover: ((Boolean) -> Unit)? = null,
    val onFocus: ((Boolean) -> Unit)? = null,
    val onValueChange: ((Any) -> Unit)? = null,
    val onDragStart: ((Offset) -> Unit)? = null,
    val onDragEnd: ((Offset) -> Unit)? = null
)

/**
 * Voice command system
 */
data class VoiceCommandSet(
    val primary: String? = null,
    val alternatives: List<String> = emptyList(),
    val localizations: Map<String, List<String>> = emptyMap(), // Language code -> commands
    val contextualCommands: Map<String, String> = emptyMap(),  // Context -> command
    val aiGenerated: Boolean = true
)

/**
 * Gesture system
 */
data class GestureSet(
    val tap: GestureAction? = null,
    val doubleTap: GestureAction? = null,
    val longPress: GestureAction? = null,
    val swipeUp: GestureAction? = null,
    val swipeDown: GestureAction? = null,
    val swipeLeft: GestureAction? = null,
    val swipeRight: GestureAction? = null,
    val pinchZoom: GestureAction? = null,
    val rotation: GestureAction? = null,
    val drag: GestureAction? = null
)

data class GestureAction(
    val action: () -> Unit,
    val feedback: FeedbackType = FeedbackType.HAPTIC
)

enum class FeedbackType { NONE, HAPTIC, SOUND, VISUAL, VOICE }

/**
 * Logic binding system - connects UI to business logic
 */
data class LogicBinding(
    val functionName: String,
    val parameters: Map<String, Any> = emptyMap(),
    val returnValueHandler: ((Any) -> Unit)? = null,
    val errorHandler: ((Exception) -> Unit)? = null,
    val validationRules: List<ValidationRule> = emptyList(),
    val conditionalLogic: ConditionalLogic? = null
)

data class ValidationRule(
    val type: ValidationType,
    val value: Any,
    val errorMessage: String
)

enum class ValidationType {
    REQUIRED, MIN_LENGTH, MAX_LENGTH, PATTERN, NUMERIC, EMAIL, URL, CUSTOM
}

data class ConditionalLogic(
    val condition: String,  // JavaScript-like expression
    val trueAction: () -> Unit,
    val falseAction: (() -> Unit)? = null
)

/**
 * Data binding system - connects UI to data sources
 */
data class DataBinding(
    val dataSource: String,      // Path to data source
    val bindingType: BindingType,
    val transformer: ((Any) -> Any)? = null,
    val formatter: ((Any) -> String)? = null,
    val validator: ((Any) -> Boolean)? = null
)

enum class BindingType {
    ONE_WAY,        // Data -> UI
    TWO_WAY,        // Data <-> UI  
    ONE_TIME        // Data -> UI (once)
}

/**
 * Accessibility properties
 */
data class AccessibilityProps(
    val contentDescription: String? = null,
    val semanticRole: SemanticRole = SemanticRole.GENERIC,
    val isImportantForAccessibility: Boolean = true,
    val focusable: Boolean = true,
    val screenReaderText: String? = null,
    val voicePriority: Int = 5,  // 1-10, 10 = highest
    val keyboardNavigation: KeyboardNavigation = KeyboardNavigation()
)

enum class SemanticRole {
    GENERIC, BUTTON, TEXT, IMAGE, LIST, LIST_ITEM, HEADING, LINK, INPUT
}

data class KeyboardNavigation(
    val tabIndex: Int = 0,
    val tabStop: Boolean = true,
    val arrowKeyNavigation: Boolean = false
)

/**
 * Responsive design properties
 */
data class ResponsiveProps(
    val breakpoints: Map<ScreenSize, ElementStyling> = emptyMap(),
    val hiddenOn: List<ScreenSize> = emptyList(),
    val priorityOn: Map<ScreenSize, Int> = emptyMap()
)

enum class ScreenSize { PHONE, TABLET, DESKTOP, TV, WATCH, AR_GLASSES }

/**
 * Animation properties  
 */
data class AnimationProps(
    val enterAnimation: AnimationType = AnimationType.NONE,
    val exitAnimation: AnimationType = AnimationType.NONE,
    val hoverAnimation: AnimationType = AnimationType.NONE,
    val duration: Long = 300,
    val easing: EasingType = EasingType.EASE_IN_OUT
)

enum class AnimationType {
    NONE, FADE_IN, FADE_OUT, SLIDE_UP, SLIDE_DOWN, SLIDE_LEFT, SLIDE_RIGHT,
    SCALE_UP, SCALE_DOWN, ROTATE, BOUNCE, ELASTIC, SPRING
}

// ================================
// VISUAL DESIGNER SYSTEM  
// ================================

/**
 * Visual drag-and-drop designer
 */
@Composable
fun VoiceUIDesigner(
    elements: List<VoiceUIElement> = emptyList(),
    selectedElement: VoiceUIElement? = null,
    onElementSelected: (VoiceUIElement) -> Unit = {},
    onElementMoved: (String, SpatialPosition) -> Unit = { _, _ -> },
    onElementStyled: (String, ElementStyling) -> Unit = { _, _ -> },
    onLogicBound: (String, LogicBinding) -> Unit = { _, _ -> }
) {
    // Main designer interface
    DesignerWorkspace(
        elements = elements,
        selectedElement = selectedElement,
        onElementSelected = onElementSelected,
        onElementMoved = onElementMoved,
        onElementStyled = onElementStyled,
        onLogicBound = onLogicBound
    )
}

@Composable
private fun DesignerWorkspace(
    elements: List<VoiceUIElement>,
    selectedElement: VoiceUIElement?,
    onElementSelected: (VoiceUIElement) -> Unit,
    onElementMoved: (String, SpatialPosition) -> Unit,
    onElementStyled: (String, ElementStyling) -> Unit,
    onLogicBound: (String, LogicBinding) -> Unit
) {
    // Implementation of visual designer workspace
    // This would be a complex UI with:
    // - Canvas for drag/drop
    // - Component palette
    // - Property inspector
    // - Logic binding editor
    // - Theme selector
    // - Preview modes (2D, 3D, AR)
}

// ================================
// CONCRETE THEME IMPLEMENTATIONS
// ================================

class MaterialColors : ColorScheme() {
    override val primary = Color(0xFF6200EE)
    override val secondary = Color(0xFF03DAC6)
    override val background = Color(0xFFFFFBFE)
    override val surface = Color(0xFFFFFBFE)
    override val error = Color(0xFFB00020)
    override val onPrimary = Color(0xFFFFFFFF)
    override val onSecondary = Color(0xFF000000)
    override val onBackground = Color(0xFF1C1B1F)
    override val onSurface = Color(0xFF1C1B1F)
    override val onError = Color(0xFFFFFFFF)
}

class CupertinoColors : ColorScheme() {
    override val primary = Color(0xFF007AFF)
    override val secondary = Color(0xFF5AC8FA)
    override val background = Color(0xFFF2F2F7)
    override val surface = Color(0xFFFFFFFF)
    override val error = Color(0xFFFF3B30)
    override val onPrimary = Color(0xFFFFFFFF)
    override val onSecondary = Color(0xFFFFFFFF)
    override val onBackground = Color(0xFF000000)
    override val onSurface = Color(0xFF000000)
    override val onError = Color(0xFFFFFFFF)
}

class VoiceUI3DColors : ColorScheme() {
    override val primary = Color(0xFF00BCD4)
    override val secondary = Color(0xFF9C27B0)
    override val background = Color(0xFF263238)
    override val surface = Color(0xFF37474F)
    override val error = Color(0xFFF44336)
    override val onPrimary = Color(0xFFFFFFFF)
    override val onSecondary = Color(0xFFFFFFFF)
    override val onBackground = Color(0xFFFFFFFF)
    override val onSurface = Color(0xFFFFFFFF)
    override val onError = Color(0xFFFFFFFF)
}

// Additional theme implementations...
class AquaColors : ColorScheme() {
    override val primary = Color(0xFF0066CC)
    override val secondary = Color(0xFF999999)
    override val background = Color(0xFFF0F0F0)
    override val surface = Color(0xFFFFFFFF)
    override val error = Color(0xFFCC0000)
    override val onPrimary = Color(0xFFFFFFFF)
    override val onSecondary = Color(0xFF000000)
    override val onBackground = Color(0xFF000000)
    override val onSurface = Color(0xFF000000)
    override val onError = Color(0xFFFFFFFF)
}

class FluentColors : ColorScheme() {
    override val primary = Color(0xFF0078D4)
    override val secondary = Color(0xFF00BCF2)
    override val background = Color(0xFFF3F2F1)
    override val surface = Color(0xFFFFFFFF)
    override val error = Color(0xFFD13438)
    override val onPrimary = Color(0xFFFFFFFF)
    override val onSecondary = Color(0xFFFFFFFF)
    override val onBackground = Color(0xFF323130)
    override val onSurface = Color(0xFF323130)
    override val onError = Color(0xFFFFFFFF)
}

class ARColors : ColorScheme() {
    override val primary = Color(0xFF00FF88)
    override val secondary = Color(0xFF00CCFF)
    override val background = Color.Transparent
    override val surface = Color(0x88000000)
    override val error = Color(0xFFFF0040)
    override val onPrimary = Color(0xFF000000)
    override val onSecondary = Color(0xFF000000)
    override val onBackground = Color(0xFFFFFFFF)
    override val onSurface = Color(0xFFFFFFFF)
    override val onError = Color(0xFFFFFFFF)
}

class FlatColors : ColorScheme() {
    override val primary = Color(0xFF3498DB)
    override val secondary = Color(0xFF2ECC71)
    override val background = Color(0xFFECF0F1)
    override val surface = Color(0xFFFFFFFF)
    override val error = Color(0xFFE74C3C)
    override val onPrimary = Color(0xFFFFFFFF)
    override val onSecondary = Color(0xFFFFFFFF)
    override val onBackground = Color(0xFF2C3E50)
    override val onSurface = Color(0xFF2C3E50)
    override val onError = Color(0xFFFFFFFF)
}

class DarkColors : ColorScheme() {
    override val primary = Color(0xFFBB86FC)
    override val secondary = Color(0xFF03DAC6)
    override val background = Color(0xFF121212)
    override val surface = Color(0xFF1E1E1E)
    override val error = Color(0xFFCF6679)
    override val onPrimary = Color(0xFF000000)
    override val onSecondary = Color(0xFF000000)
    override val onBackground = Color(0xFFFFFFFF)
    override val onSurface = Color(0xFFFFFFFF)
    override val onError = Color(0xFF000000)
}

class HighContrastColors : ColorScheme() {
    override val primary = Color(0xFF000000)
    override val secondary = Color(0xFF000000)
    override val background = Color(0xFFFFFFFF)
    override val surface = Color(0xFFFFFFFF)
    override val error = Color(0xFF000000)
    override val onPrimary = Color(0xFFFFFFFF)
    override val onSecondary = Color(0xFFFFFFFF)
    override val onBackground = Color(0xFF000000)
    override val onSurface = Color(0xFF000000)
    override val onError = Color(0xFFFFFFFF)
}

class CustomColors : ColorScheme() {
    override val primary = Color(0xFF6200EE)
    override val secondary = Color(0xFF03DAC6)
    override val background = Color(0xFFFFFBFE)
    override val surface = Color(0xFFFFFBFE)
    override val error = Color(0xFFB00020)
    override val onPrimary = Color(0xFFFFFFFF)
    override val onSecondary = Color(0xFF000000)
    override val onBackground = Color(0xFF1C1B1F)
    override val onSurface = Color(0xFF1C1B1F)
    override val onError = Color(0xFFFFFFFF)
}

// Typography implementations
class MaterialTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(96f, FontWeight.LIGHT, "Roboto")
    override val h2 = VoiceUITextStyle(60f, FontWeight.LIGHT, "Roboto")
    override val h3 = VoiceUITextStyle(48f, FontWeight.NORMAL, "Roboto")
    override val body1 = VoiceUITextStyle(16f, FontWeight.NORMAL, "Roboto")
    override val body2 = VoiceUITextStyle(14f, FontWeight.NORMAL, "Roboto")
    override val caption = VoiceUITextStyle(12f, FontWeight.NORMAL, "Roboto")
}

class CupertinoTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(96f, FontWeight.LIGHT, "SF Pro")
    override val h2 = VoiceUITextStyle(60f, FontWeight.LIGHT, "SF Pro")
    override val h3 = VoiceUITextStyle(48f, FontWeight.NORMAL, "SF Pro")
    override val body1 = VoiceUITextStyle(17f, FontWeight.NORMAL, "SF Pro")
    override val body2 = VoiceUITextStyle(15f, FontWeight.NORMAL, "SF Pro")
    override val caption = VoiceUITextStyle(13f, FontWeight.NORMAL, "SF Pro")
}

class VoiceUI3DTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(96f, FontWeight.BOLD, "Orbitron")
    override val h2 = VoiceUITextStyle(60f, FontWeight.BOLD, "Orbitron")
    override val h3 = VoiceUITextStyle(48f, FontWeight.NORMAL, "Orbitron")
    override val body1 = VoiceUITextStyle(16f, FontWeight.NORMAL, "Exo 2")
    override val body2 = VoiceUITextStyle(14f, FontWeight.NORMAL, "Exo 2")
    override val caption = VoiceUITextStyle(12f, FontWeight.NORMAL, "Exo 2")
}

// Additional typography implementations...
class AquaTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(96f, FontWeight.LIGHT, "Helvetica Neue")
    override val h2 = VoiceUITextStyle(60f, FontWeight.LIGHT, "Helvetica Neue")
    override val h3 = VoiceUITextStyle(48f, FontWeight.NORMAL, "Helvetica Neue")
    override val body1 = VoiceUITextStyle(14f, FontWeight.NORMAL, "Helvetica Neue")
    override val body2 = VoiceUITextStyle(12f, FontWeight.NORMAL, "Helvetica Neue")
    override val caption = VoiceUITextStyle(10f, FontWeight.NORMAL, "Helvetica Neue")
}

class FluentTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(96f, FontWeight.LIGHT, "Segoe UI")
    override val h2 = VoiceUITextStyle(60f, FontWeight.LIGHT, "Segoe UI")
    override val h3 = VoiceUITextStyle(48f, FontWeight.NORMAL, "Segoe UI")
    override val body1 = VoiceUITextStyle(14f, FontWeight.NORMAL, "Segoe UI")
    override val body2 = VoiceUITextStyle(12f, FontWeight.NORMAL, "Segoe UI")
    override val caption = VoiceUITextStyle(10f, FontWeight.NORMAL, "Segoe UI")
}

class ARTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(72f, FontWeight.BOLD, "Noto Sans")
    override val h2 = VoiceUITextStyle(48f, FontWeight.BOLD, "Noto Sans")
    override val h3 = VoiceUITextStyle(36f, FontWeight.NORMAL, "Noto Sans")
    override val body1 = VoiceUITextStyle(18f, FontWeight.NORMAL, "Noto Sans")
    override val body2 = VoiceUITextStyle(16f, FontWeight.NORMAL, "Noto Sans")
    override val caption = VoiceUITextStyle(14f, FontWeight.NORMAL, "Noto Sans")
}

class FlatTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(96f, FontWeight.LIGHT, "Lato")
    override val h2 = VoiceUITextStyle(60f, FontWeight.LIGHT, "Lato")
    override val h3 = VoiceUITextStyle(48f, FontWeight.NORMAL, "Lato")
    override val body1 = VoiceUITextStyle(16f, FontWeight.NORMAL, "Lato")
    override val body2 = VoiceUITextStyle(14f, FontWeight.NORMAL, "Lato")
    override val caption = VoiceUITextStyle(12f, FontWeight.NORMAL, "Lato")
}

class DarkTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(96f, FontWeight.LIGHT, "Roboto")
    override val h2 = VoiceUITextStyle(60f, FontWeight.LIGHT, "Roboto")
    override val h3 = VoiceUITextStyle(48f, FontWeight.NORMAL, "Roboto")
    override val body1 = VoiceUITextStyle(16f, FontWeight.NORMAL, "Roboto")
    override val body2 = VoiceUITextStyle(14f, FontWeight.NORMAL, "Roboto")
    override val caption = VoiceUITextStyle(12f, FontWeight.NORMAL, "Roboto")
}

class HighContrastTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(96f, FontWeight.BOLD, "Arial")
    override val h2 = VoiceUITextStyle(60f, FontWeight.BOLD, "Arial")
    override val h3 = VoiceUITextStyle(48f, FontWeight.BOLD, "Arial")
    override val body1 = VoiceUITextStyle(18f, FontWeight.BOLD, "Arial")
    override val body2 = VoiceUITextStyle(16f, FontWeight.BOLD, "Arial")
    override val caption = VoiceUITextStyle(14f, FontWeight.BOLD, "Arial")
}

class CustomTypography : TypographyTheme() {
    override val h1 = VoiceUITextStyle(96f, FontWeight.LIGHT, "Inter")
    override val h2 = VoiceUITextStyle(60f, FontWeight.LIGHT, "Inter")
    override val h3 = VoiceUITextStyle(48f, FontWeight.NORMAL, "Inter")
    override val body1 = VoiceUITextStyle(16f, FontWeight.NORMAL, "Inter")
    override val body2 = VoiceUITextStyle(14f, FontWeight.NORMAL, "Inter")
    override val caption = VoiceUITextStyle(12f, FontWeight.NORMAL, "Inter")
}

// Spacing implementations
class MaterialSpacing : SpacingTheme() {
    override val xs = 4f
    override val sm = 8f
    override val md = 16f
    override val lg = 24f
    override val xl = 32f
}

class CupertinoSpacing : SpacingTheme() {
    override val xs = 4f
    override val sm = 8f
    override val md = 16f
    override val lg = 20f
    override val xl = 32f
}

class VoiceUI3DSpacing : SpacingTheme() {
    override val xs = 8f
    override val sm = 16f
    override val md = 24f
    override val lg = 32f
    override val xl = 48f
}

class AquaSpacing : SpacingTheme() {
    override val xs = 3f
    override val sm = 6f
    override val md = 12f
    override val lg = 18f
    override val xl = 24f
}

class FluentSpacing : SpacingTheme() {
    override val xs = 4f
    override val sm = 8f
    override val md = 12f
    override val lg = 20f
    override val xl = 32f
}

class ARSpacing : SpacingTheme() {
    override val xs = 12f
    override val sm = 24f
    override val md = 48f
    override val lg = 72f
    override val xl = 96f
}

class FlatSpacing : SpacingTheme() {
    override val xs = 5f
    override val sm = 10f
    override val md = 20f
    override val lg = 30f
    override val xl = 40f
}

class DarkSpacing : SpacingTheme() {
    override val xs = 4f
    override val sm = 8f
    override val md = 16f
    override val lg = 24f
    override val xl = 32f
}

class HighContrastSpacing : SpacingTheme() {
    override val xs = 8f
    override val sm = 16f
    override val md = 24f
    override val lg = 32f
    override val xl = 48f
}

class CustomSpacing : SpacingTheme() {
    override val xs = 4f
    override val sm = 8f
    override val md = 16f
    override val lg = 24f
    override val xl = 32f
}