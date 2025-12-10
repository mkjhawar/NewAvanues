/**
 * VisualDesignerExamples.kt - Examples showing complete visual design system
 * 
 * Shows how VoiceUI handles spatial positioning, theming, drag-and-drop design,
 * and logic binding with visual mockups and code examples.
 */

package com.augmentalis.voiceui.designer

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceui.api.AIContext
import com.augmentalis.voiceui.api.AccessibilityContext

/**
 * EXAMPLE 1: Login Screen with Complete Visual Specification
 * 
 * Shows spatial positioning, theming, and visual hierarchy
 */

@Composable
fun LoginScreenVisualExample() {
    // Complete visual specification with positioning and theming
    VoiceScreen(
        name = "login_screen",
        spatialMode = SpatialMode.FLAT_2D
    ) {
        // Welcome text - positioned at top with custom styling
        text(
            text = "Welcome Back",
            position = SpatialPosition(
                x = 0f, y = -200f, z = 0f,
                anchor = AnchorPoint.TOP_CENTER
            ),
            styling = ElementStyling(
                fontSize = 32f,
                fontWeight = FontWeight.BOLD,
                foregroundColor = Color(0xFF007AFF),
                margin = EdgeInsets(bottom = 40f)
            ),
            aiContext = AIContext(
                purpose = "welcome user and establish brand trust",
                accessibility = AccessibilityContext(
                    screenReaderText = "Welcome back to VoiceOS",
                    voiceCommandPriority = 2
                )
            )
        )
        
        // Email input - positioned center-left with Material styling
        input(
            label = "Email Address",
            position = SpatialPosition(
                x = 0f, y = -50f, z = 0f,
                width = 300f, height = 56f,
                anchor = AnchorPoint.CENTER
            ),
            styling = ElementStyling(
                backgroundColor = Color.White,
                borderColor = Color(0xFFE0E0E0),
                borderWidth = 1f,
                borderRadius = 8f,
                padding = EdgeInsets(16f),
                focusStyle = ElementStyling(
                    borderColor = Color(0xFF007AFF),
                    borderWidth = 2f
                )
            ),
            interactions = InteractionSet(
                onFocus = { focused ->
                    if (focused) announceVoice("Email field focused. You can type or say 'dictate email'")
                }
            ),
            voiceCommands = VoiceCommandSet(
                primary = "email field",
                alternatives = listOf("enter email", "email address", "type email"),
                aiGenerated = true
            ),
            logicBinding = LogicBinding(
                functionName = "validateEmail",
                validationRules = listOf(
                    ValidationRule(ValidationType.REQUIRED, true, "Email is required"),
                    ValidationRule(ValidationType.EMAIL, true, "Please enter a valid email")
                )
            )
        )
        
        // Password input - positioned below email
        password(
            position = SpatialPosition(
                x = 0f, y = 20f, z = 0f,
                width = 300f, height = 56f,
                anchor = AnchorPoint.CENTER,
                relativeTo = "email_input_uuid"
            ),
            styling = ElementStyling(
                backgroundColor = Color.White,
                borderColor = Color(0xFFE0E0E0),
                borderWidth = 1f,
                borderRadius = 8f,
                padding = EdgeInsets(16f)
            ),
            voiceCommands = VoiceCommandSet(
                primary = "password field",
                alternatives = listOf("enter password", "password", "secret"),
                aiGenerated = true
            ),
            logicBinding = LogicBinding(
                functionName = "validatePassword",
                validationRules = listOf(
                    ValidationRule(ValidationType.REQUIRED, true, "Password is required"),
                    ValidationRule(ValidationType.MIN_LENGTH, 8, "Password must be at least 8 characters")
                )
            )
        )
        
        // Login button - elevated with Material styling
        button(
            text = "Sign In",
            position = SpatialPosition(
                x = 0f, y = 100f, z = 10f,  // Elevated above other elements
                width = 300f, height = 56f,
                anchor = AnchorPoint.CENTER,
                depth = DepthLayer.ELEVATED
            ),
            styling = ElementStyling(
                backgroundColor = Color(0xFF007AFF),
                foregroundColor = Color.White,
                borderRadius = 8f,
                shadow = ShadowStyle(
                    offsetY = 4f,
                    blurRadius = 8f,
                    spreadRadius = 0f
                ),
                hoverStyle = ElementStyling(
                    backgroundColor = Color(0xFF005BB5),
                    shadow = ShadowStyle(
                        offsetY = 6f,
                        blurRadius = 12f
                    )
                )
            ),
            interactions = InteractionSet(
                onClick = { performLogin() },
                onHover = { hovered ->
                    if (hovered) announceVoice("Sign in button")
                }
            ),
            voiceCommands = VoiceCommandSet(
                primary = "sign in",
                alternatives = listOf("login", "log in", "sign me in", "authenticate"),
                localizations = mapOf(
                    "es" to listOf("iniciar sesión", "entrar", "acceder"),
                    "fr" to listOf("se connecter", "connexion", "entrer")
                )
            ),
            gestures = GestureSet(
                doubleTap = GestureAction({ quickLogin() }, FeedbackType.HAPTIC)
            ),
            logicBinding = LogicBinding(
                functionName = "performLogin",
                parameters = mapOf(
                    "email" to "{{email_input_value}}",
                    "password" to "{{password_input_value}}"
                ),
                conditionalLogic = ConditionalLogic(
                    condition = "email.isValid && password.isValid",
                    trueAction = { submitLogin() },
                    falseAction = { showValidationErrors() }
                )
            ),
            animation = AnimationProps(
                enterAnimation = AnimationType.SCALE_UP,
                hoverAnimation = AnimationType.BOUNCE,
                duration = 200
            )
        )
        
        // Forgot password link - positioned at bottom
        link(
            text = "Forgot Password?",
            position = SpatialPosition(
                x = 0f, y = 180f, z = 0f,
                anchor = AnchorPoint.CENTER
            ),
            styling = ElementStyling(
                foregroundColor = Color(0xFF007AFF),
                fontSize = 14f
            ),
            voiceCommands = VoiceCommandSet(
                primary = "forgot password",
                alternatives = listOf("reset password", "password help", "can't remember password")
            ),
            logicBinding = LogicBinding(
                functionName = "navigateToPasswordReset"
            )
        )
    }
}

/**
 * EXAMPLE 2: 3D Spatial Shopping Interface
 * 
 * Shows how elements exist in 3D space with depth layers
 */

@Composable
fun Shopping3DExample() {
    VoiceScreen(
        name = "shopping_3d",
        spatialMode = SpatialMode.SPATIAL_3D
    ) {
        // Background product grid - far depth
        productGrid(
            position = SpatialPosition(
                x = 0f, y = 0f, z = -100f,  // Far background
                depth = DepthLayer.BACKGROUND
            ),
            styling = ElementStyling(
                opacity = 0.6f,
                blur = 2f
            )
        )
        
        // Featured product card - elevated in foreground
        card(
            content = "iPhone 15 Pro - $999",
            position = SpatialPosition(
                x = 0f, y = 0f, z = 50f,  // Forward in space
                width = 320f, height = 400f,
                depth = DepthLayer.ELEVATED,
                rotationY = -15f  // Slight 3D rotation
            ),
            styling = ElementStyling(
                backgroundColor = Color.White,
                borderRadius = 16f,
                shadow = ShadowStyle(
                    offsetY = 8f,
                    blurRadius = 24f,
                    spreadRadius = 4f
                ),
                gradient = GradientStyle(
                    colors = listOf(Color.White, Color(0xFFF8F8F8)),
                    direction = GradientDirection.VERTICAL
                )
            ),
            voiceCommands = VoiceCommandSet(
                primary = "featured product",
                alternatives = listOf("iPhone", "main product", "center card")
            ),
            gestures = GestureSet(
                pinchZoom = GestureAction({ zoomProduct() }),
                rotation = GestureAction({ rotateProduct() })
            )
        )
        
        // Floating action buttons - highest depth
        fab(
            icon = "shopping_cart",
            position = SpatialPosition(
                x = 250f, y = -200f, z = 100f,  // Floating in space
                depth = DepthLayer.FLOATING
            ),
            styling = ElementStyling(
                backgroundColor = Color(0xFF00BCD4),
                shadow = ShadowStyle(
                    offsetY = 12f,
                    blurRadius = 20f
                )
            ),
            voiceCommands = VoiceCommandSet(
                primary = "shopping cart",
                alternatives = listOf("cart", "my cart", "view cart")
            ),
            animation = AnimationProps(
                enterAnimation = AnimationType.ELASTIC,
                hoverAnimation = AnimationType.BOUNCE
            )
        )
        
        // HUD overlay - always on top
        hudOverlay(
            content = "3 items in cart • $2,497 total",
            position = SpatialPosition(
                x = 0f, y = -300f, z = 200f,  // Always visible
                depth = DepthLayer.HUD
            ),
            styling = ElementStyling(
                backgroundColor = Color(0x88000000),
                foregroundColor = Color.White,
                borderRadius = 20f
            )
        )
    }
}

/**
 * EXAMPLE 3: AR Spatial Interface
 * 
 * Shows world-locked elements in AR space
 */

@Composable
fun ARSpatialExample() {
    VoiceScreen(
        name = "ar_home_control",
        spatialMode = SpatialMode.AR_WORLD_LOCKED
    ) {
        // Light switch - locked to physical wall
        switch(
            label = "Living Room Lights",
            position = SpatialPosition(
                worldPosition = WorldPosition(
                    latitude = 37.7749,
                    longitude = -122.4194,
                    worldX = 2.5f,  // 2.5 meters to the right
                    worldY = 1.2f,  // 1.2 meters up (light switch height)
                    worldZ = -3.0f  // 3 meters forward
                ),
                isWorldLocked = true,
                depth = DepthLayer.SPATIAL_NEAR
            ),
            styling = ElementStyling(
                backgroundColor = Color(0x88FFFFFF),
                borderColor = Color(0xFF00FF88),
                borderWidth = 2f,
                borderRadius = 12f,
                shadow = ShadowStyle(
                    offsetY = 0f,
                    blurRadius = 8f,
                    spreadRadius = 2f
                )
            ),
            voiceCommands = VoiceCommandSet(
                primary = "living room lights",
                alternatives = listOf("main lights", "room lights", "toggle lights")
            ),
            gestures = GestureSet(
                tap = GestureAction({ toggleLights() }, FeedbackType.HAPTIC)
            ),
            logicBinding = LogicBinding(
                functionName = "toggleSmartLight",
                parameters = mapOf("deviceId" to "living_room_main")
            )
        )
        
        // Thermostat - locked to different wall
        thermostat(
            currentTemp = 72,
            position = SpatialPosition(
                worldPosition = WorldPosition(
                    worldX = -1.8f,  // Left wall
                    worldY = 1.5f,   // Eye level
                    worldZ = -2.0f
                ),
                isWorldLocked = true,
                depth = DepthLayer.SPATIAL_NEAR
            ),
            styling = ElementStyling(
                backgroundColor = Color(0x99000000),
                foregroundColor = Color(0xFF00CCFF),
                borderRadius = 20f
            ),
            voiceCommands = VoiceCommandSet(
                primary = "thermostat",
                alternatives = listOf("temperature", "climate control", "AC")
            )
        )
        
        // Floating menu - follows user
        floatingMenu(
            position = SpatialPosition(
                x = -200f, y = 0f, z = 100f,  // Always in peripheral vision
                depth = DepthLayer.SPATIAL_NEAR
            ),
            styling = ElementStyling(
                backgroundColor = Color(0xAA000000),
                borderRadius = 16f
            ),
            voiceCommands = VoiceCommandSet(
                primary = "menu",
                alternatives = listOf("options", "controls", "settings")
            )
        )
    }
}

/**
 * EXAMPLE 4: Visual Designer Interface
 * 
 * Shows drag-and-drop design interface with live preview
 */

@Composable
fun DesignerInterfaceExample() {
    var selectedElement by remember { mutableStateOf<VoiceUIElement?>(null) }
    var designerMode by remember { mutableStateOf(DesignerMode.DESIGN) }
    
    VoiceUIDesigner(
        elements = listOf(
            // Header element
            VoiceUIElement(
                type = ElementType.HEADING,
                name = "App Title",
                position = SpatialPosition(x = 0f, y = -200f, z = 0f),
                styling = ElementStyling(
                    fontSize = 24f,
                    fontWeight = FontWeight.BOLD
                ),
                voiceCommands = VoiceCommandSet(primary = "app title"),
                logicBinding = LogicBinding(
                    functionName = "updateTitle",
                    parameters = mapOf("text" to "{{title_value}}")
                )
            ),
            
            // Button element  
            VoiceUIElement(
                type = ElementType.BUTTON,
                name = "Submit Button",
                position = SpatialPosition(x = 0f, y = 100f, z = 10f),
                styling = ElementStyling(
                    backgroundColor = Color(0xFF2196F3),
                    foregroundColor = Color.White,
                    borderRadius = 8f
                ),
                voiceCommands = VoiceCommandSet(
                    primary = "submit",
                    alternatives = listOf("send", "confirm", "ok")
                ),
                logicBinding = LogicBinding(
                    functionName = "handleSubmit",
                    validationRules = listOf(
                        ValidationRule(ValidationType.REQUIRED, true, "Form must be valid")
                    )
                )
            )
        ),
        selectedElement = selectedElement,
        onElementSelected = { selectedElement = it },
        onElementMoved = { uuid, newPosition ->
            // Update element position
            updateElementPosition(uuid, newPosition)
        },
        onElementStyled = { uuid, newStyling ->
            // Update element styling
            updateElementStyling(uuid, newStyling)
        },
        onLogicBound = { uuid, logicBinding ->
            // Bind element to business logic
            bindElementLogic(uuid, logicBinding)
        }
    )
    
    // Designer sidebar with tools
    DesignerSidebar(
        mode = designerMode,
        selectedElement = selectedElement,
        onModeChange = { designerMode = it },
        onElementUpdate = { element ->
            // Update element properties
            updateElement(element)
        }
    )
    
    // Live preview modes
    PreviewControls(
        onPreview2D = { switchTo2DPreview() },
        onPreview3D = { switchTo3DPreview() },
        onPreviewAR = { switchToARPreview() },
        onPreviewVoice = { testVoiceCommands() }
    )
}

/**
 * EXAMPLE 5: Theme Comparison Visual
 * 
 * Shows the same button in different themes
 */

@Composable
fun ThemeComparisonExample() {
    // Theme comparison now uses CustomTheme instances directly
    // This example shows different button styles
    val buttonStyles = listOf(
        "Material", "iOS", "macOS", "Windows", 
        "3D", "AR", "Flat", "Dark", "HighContrast"
    )
    
    buttonStyles.forEachIndexed { index, style ->
        VoiceButton(
            text = "Login",
            position = SpatialPosition(
                x = (index % 3 - 1) * 200f,  // 3 columns
                y = (index / 3) * 100f,      // Multiple rows
                z = 0f
            ),
            styleName = style,
            voiceCommands = VoiceCommandSet(
                primary = "login ${style.lowercase()}"
            )
        )
    }
}

// Supporting enums and classes
enum class SpatialMode {
    FLAT_2D,           // Traditional 2D interface
    LAYERED_2_5D,      // 2D with depth layers
    SPATIAL_3D,        // Full 3D positioning
    AR_WORLD_LOCKED,   // Locked to real world positions
    VR_IMMERSIVE       // Full VR environment
}

enum class DesignerMode {
    DESIGN,            // Drag and drop design
    PREVIEW,           // Live preview
    CODE,              // Generated code view
    VOICE_TEST,        // Voice command testing
    LOGIC_BIND         // Logic binding editor
}

// Visual ASCII representation of the login screen with spatial info
/**
 * LOGIN SCREEN VISUAL LAYOUT (with spatial coordinates):
 * 
 * ┌─────────────────────────────────────────────────────┐
 * │ ●●●       VoiceOS Login        ●●● z=200 (HUD)     │
 * ├─────────────────────────────────────────────────────┤
 * │                                                     │
 * │            Welcome Back           y=-200, z=0       │ ← Title
 * │         (iOS Cupertino theme)                       │
 * │                                                     │
 * │  ┌─────────────────────────────┐  y=-50, z=0       │
 * │  │ Email Address            🎤 │                    │ ← Input
 * │  └─────────────────────────────┘                   │
 * │                                                     │
 * │  ┌─────────────────────────────┐  y=20, z=0        │
 * │  │ Password               ●●●● │                    │ ← Password
 * │  └─────────────────────────────┘                   │
 * │                                                     │
 * │  ┌─────────── SIGN IN ─────────┐  y=100, z=10      │ ← Elevated
 * │  │     [Tap/Voice/Gesture]     │  (shadow, 3D)     │   button
 * │  └─────────────────────────────┘                   │
 * │                                                     │
 * │         Forgot Password?         y=180, z=0        │ ← Link
 * │                                                     │
 * ├─────────────────────────────────────────────────────┤
 * │ 🎙️ "email field", "sign in", "forgot password"    │ ← Voice UI
 * │ ✋ Double-tap button for quick login               │ ← Gestures  
 * │ 👁️ AR mode: elements float in space              │ ← Spatial
 * └─────────────────────────────────────────────────────┘
 * 
 * SPATIAL PROPERTIES:
 * - All elements have x,y,z coordinates
 * - Button at z=10 (elevated with shadow)
 * - HUD at z=200 (always on top)
 * - Theme: iOS Cupertino (rounded, blue accents)
 * - Voice commands auto-generated + custom
 * - Gestures: double-tap, swipe, pinch
 * - Logic: validation, conditional flow
 * - UUID: each element tracked in UUIDManager
 */

// Placeholder implementations for supporting functions
private fun announceVoice(text: String) { /* TTS announcement */ }
private fun performLogin() { /* Login logic */ }
private fun quickLogin() { /* Biometric/saved login */ }
private fun zoomProduct() { /* 3D zoom */ }
private fun rotateProduct() { /* 3D rotation */ }
private fun toggleLights() { /* Smart home control */ }
private fun updateElementPosition(uuid: String, position: SpatialPosition) { /* Designer */ }
private fun updateElementStyling(uuid: String, styling: ElementStyling) { /* Designer */ }
private fun bindElementLogic(uuid: String, binding: LogicBinding) { /* Designer */ }
private fun updateElement(element: VoiceUIElement) { /* Designer */ }
private fun switchTo2DPreview() { /* Preview mode */ }
private fun switchTo3DPreview() { /* Preview mode */ }
private fun switchToARPreview() { /* Preview mode */ }
private fun testVoiceCommands() { /* Voice testing */ }
private fun submitLogin() { /* Form submission */ }
private fun showValidationErrors() { /* Error display */ }

// Placeholder composables for missing components
@Composable private fun productGrid(position: SpatialPosition, styling: ElementStyling) {}
@Composable private fun card(content: String, position: SpatialPosition, styling: ElementStyling, voiceCommands: VoiceCommandSet, gestures: GestureSet) {}
@Composable private fun fab(icon: String, position: SpatialPosition, styling: ElementStyling, voiceCommands: VoiceCommandSet, animation: AnimationProps) {}
@Composable private fun hudOverlay(content: String, position: SpatialPosition, styling: ElementStyling) {}
@Composable private fun switch(label: String, position: SpatialPosition, styling: ElementStyling, voiceCommands: VoiceCommandSet, gestures: GestureSet, logicBinding: LogicBinding) {}
@Composable private fun thermostat(currentTemp: Int, position: SpatialPosition, styling: ElementStyling, voiceCommands: VoiceCommandSet) {}
@Composable private fun floatingMenu(position: SpatialPosition, styling: ElementStyling, voiceCommands: VoiceCommandSet) {}
@Composable private fun DesignerSidebar(mode: DesignerMode, selectedElement: VoiceUIElement?, onModeChange: (DesignerMode) -> Unit, onElementUpdate: (VoiceUIElement) -> Unit) {}
@Composable private fun PreviewControls(onPreview2D: () -> Unit, onPreview3D: () -> Unit, onPreviewAR: () -> Unit, onPreviewVoice: () -> Unit) {}
@Composable private fun VoiceButton(text: String, position: SpatialPosition, styleName: String, voiceCommands: VoiceCommandSet) {}
@Composable private fun VoiceScreen(name: String, spatialMode: SpatialMode, content: @Composable () -> Unit) {}
@Composable private fun text(text: String, position: SpatialPosition, styling: ElementStyling, aiContext: AIContext) {}
@Composable private fun input(label: String, position: SpatialPosition, styling: ElementStyling, interactions: InteractionSet, voiceCommands: VoiceCommandSet, logicBinding: LogicBinding) {}
@Composable private fun password(position: SpatialPosition, styling: ElementStyling, voiceCommands: VoiceCommandSet, logicBinding: LogicBinding) {}
@Composable private fun button(text: String, position: SpatialPosition, styling: ElementStyling, interactions: InteractionSet, voiceCommands: VoiceCommandSet, gestures: GestureSet, logicBinding: LogicBinding, animation: AnimationProps) {}
@Composable private fun link(text: String, position: SpatialPosition, styling: ElementStyling, voiceCommands: VoiceCommandSet, logicBinding: LogicBinding) {}