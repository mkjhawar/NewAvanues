/**
 * ThemeIntegrationPipeline.kt - Integrates custom themes with adaptive rendering
 * 
 * This pipeline ensures custom themes work seamlessly with the Universal Adaptation
 * system, maintaining brand identity while feeling native on every device.
 */

package com.augmentalis.voiceui.theming

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.augmentalis.voiceui.designer.FontWeight as DesignerFontWeight
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceui.universalui.*
import com.augmentalis.voiceui.designer.*
import kotlinx.coroutines.flow.*
import android.util.Log

/**
 * THEME INTEGRATION PIPELINE
 * 
 * Merges custom themes with native platform conventions to create
 * the perfect blend of brand identity and native feel.
 */
class ThemeIntegrationPipeline {
    
    companion object {
        private const val TAG = "ThemeIntegration"
        
        val instance by lazy { ThemeIntegrationPipeline() }
    }
    
    /**
     * Apply custom theme while maintaining native feel
     */
    fun applyThemeWithNativeAdaptation(
        customTheme: CustomTheme?,
        elements: List<VoiceUIElement>,
        device: DeviceProfile,
        nativeThemingEnabled: Boolean = true
    ): ThemedElements {
        
        Log.d(TAG, "Applying theme: ${customTheme?.name} to ${elements.size} elements")
        
        // Step 1: Detect native platform conventions
        val nativePlatform = if (nativeThemingEnabled) {
            detectNativePlatform(device)
        } else {
            NativePlatform.NONE
        }
        
        // Step 2: Create integrated theme
        val integratedTheme = createIntegratedTheme(
            customTheme = customTheme,
            nativePlatform = nativePlatform,
            device = device
        )
        
        // Step 3: Apply theme to elements
        val themedElements = elements.map { element ->
            applyIntegratedTheme(element, integratedTheme, device)
        }
        
        // Step 4: Optimize for device capabilities
        val optimizedElements = optimizeForDevice(themedElements, device)
        
        return ThemedElements(
            elements = optimizedElements,
            theme = integratedTheme,
            device = device
        )
    }
    
    /**
     * Detect native platform for proper integration
     */
    private fun detectNativePlatform(device: DeviceProfile): NativePlatform {
        return when (device.type) {
            DeviceType.PHONE, DeviceType.TABLET -> when {
                device.os.contains("Android", ignoreCase = true) -> {
                    when {
                        device.osVersion >= 31 -> NativePlatform.MATERIAL_3
                        device.osVersion >= 21 -> NativePlatform.MATERIAL_2
                        else -> NativePlatform.MATERIAL_1
                    }
                }
                device.os.contains("iOS", ignoreCase = true) -> NativePlatform.IOS_CUPERTINO
                else -> NativePlatform.GENERIC
            }
            
            DeviceType.SMART_GLASSES -> NativePlatform.AR_NATIVE
            DeviceType.VR_DEVICE -> NativePlatform.VR_SPATIAL
            DeviceType.TV -> NativePlatform.TV_LEANBACK
            DeviceType.WATCH -> NativePlatform.WEAR_OS
            DeviceType.CAR -> NativePlatform.AUTOMOTIVE
            DeviceType.DESKTOP -> when {
                device.os.contains("Windows") -> NativePlatform.WINDOWS_FLUENT
                device.os.contains("macOS") -> NativePlatform.MACOS_AQUA
                device.os.contains("Linux") -> NativePlatform.LINUX_GTK
                else -> NativePlatform.GENERIC
            }
            
            else -> NativePlatform.GENERIC
        }
    }
    
    /**
     * Create integrated theme combining custom + native
     */
    private fun createIntegratedTheme(
        customTheme: CustomTheme?,
        nativePlatform: NativePlatform,
        device: DeviceProfile
    ): IntegratedTheme {
        
        // Start with native platform base
        val nativeBase = getNativeThemeBase(nativePlatform, device)
        
        // Apply custom theme on top if provided
        return if (customTheme != null) {
            IntegratedTheme(
                // Custom colors override native
                colors = mergeColors(nativeBase.colors, customTheme.colors),
                
                // Custom typography with native fallbacks
                typography = mergeTypography(nativeBase.typography, customTheme.typography),
                
                // Blend spacing for consistency
                spacing = mergeSpacing(nativeBase.spacing, customTheme.spacing),
                
                // Use native shapes unless custom specified
                shapes = customTheme.shapes ?: nativeBase.shapes,
                
                // Respect platform animation conventions
                animations = mergeAnimations(nativeBase.animations, customTheme.animations),
                
                // Custom shadows with platform defaults
                shadows = customTheme.shadows ?: nativeBase.shadows,
                
                // Keep native components
                components = nativeBase.components,
                
                // Apply device-specific overrides
                deviceOverrides = customTheme.deviceOverrides[device.type]
            )
        } else {
            // No custom theme - use pure native
            IntegratedTheme.fromNative(nativeBase)
        }
    }
    
    /**
     * Apply integrated theme to element
     */
    private fun applyIntegratedTheme(
        element: VoiceUIElement,
        theme: IntegratedTheme,
        device: DeviceProfile
    ): VoiceUIElement {
        
        // Apply base theme
        var themedElement = element.copy(
            styling = ElementStyling(
                backgroundColor = theme.colors.getColorForElement(element.type, "background"),
                foregroundColor = theme.colors.getColorForElement(element.type, "foreground"),
                borderColor = theme.colors.getColorForElement(element.type, "border"),
                fontSize = theme.typography.getSizeForElement(element.type),
                fontWeight = theme.typography.getWeightForElement(element.type),
                fontFamily = theme.typography.getFontForElement(element.type),
                padding = EdgeInsets(theme.spacing.getPaddingForElement(element.type)),
                margin = EdgeInsets(theme.spacing.getMarginForElement(element.type)),
                borderRadius = when(element.type) {
                    ElementType.BUTTON, ElementType.CARD -> (theme.shapes.medium as? ShapeStyle.Rounded)?.radius ?: 8f
                    ElementType.DIALOG -> (theme.shapes.large as? ShapeStyle.Rounded)?.radius ?: 16f
                    else -> (theme.shapes.small as? ShapeStyle.Rounded)?.radius ?: 4f
                },
                shadow = ShadowStyle()
            ),
            animation = AnimationProps(
                duration = theme.animations.getDurationForElement(element.type),
                easing = theme.animations.getEasingForElement(element.type),
                enterAnimation = AnimationType.FADE_IN,
                exitAnimation = AnimationType.FADE_OUT
            )
        )
        
        // Apply device-specific overrides
        theme.deviceOverrides?.let { override ->
            themedElement = applyDeviceOverride(themedElement, override)
        }
        
        // Apply native component styling
        themedElement = applyNativeComponentStyle(themedElement, theme.components, device)
        
        return themedElement
    }
    
    /**
     * Get native theme base for platform
     */
    private fun getNativeThemeBase(
        platform: NativePlatform,
        device: DeviceProfile
    ): NativeThemeBase {
        
        return when (platform) {
            NativePlatform.MATERIAL_3 -> Material3ThemeBase(device)
            NativePlatform.MATERIAL_2 -> Material2ThemeBase(device)
            NativePlatform.IOS_CUPERTINO -> CupertinoThemeBase(device)
            NativePlatform.WINDOWS_FLUENT -> FluentThemeBase(device)
            NativePlatform.MACOS_AQUA -> AquaThemeBase(device)
            NativePlatform.AR_NATIVE -> ARThemeBase(device)
            NativePlatform.VR_SPATIAL -> VRThemeBase(device)
            NativePlatform.TV_LEANBACK -> TVThemeBase(device)
            else -> GenericThemeBase(device)
        }
    }
    
    /**
     * Merge color schemes intelligently
     */
    private fun mergeColors(
        native: NativeColorScheme,
        custom: CustomColorScheme
    ): IntegratedColorScheme {
        
        return IntegratedColorScheme(
            // Use custom primary/secondary if defined
            primary = custom.primary ?: native.primary,
            secondary = custom.secondary ?: native.secondary,
            
            // Background and surface from custom or native
            background = custom.background ?: native.background,
            surface = custom.surface ?: native.surface,
            
            // Error color (important to keep consistent)
            error = custom.error ?: native.error,
            
            // Text colors
            onPrimary = custom.onPrimary ?: native.onPrimary,
            onSecondary = custom.onSecondary ?: native.onSecondary,
            onBackground = custom.onBackground ?: native.onBackground,
            onSurface = custom.onSurface ?: native.onSurface,
            onError = custom.onError ?: native.onError,
            
            // Native semantic colors (for platform consistency)
            nativeSemanticColors = native.semanticColors,
            
            // All custom colors
            customColors = custom.getAllCustomColors()
        )
    }
    
    /**
     * Merge typography while respecting platform conventions
     */
    private fun mergeTypography(
        native: NativeTypography,
        custom: CustomTypographyTheme
    ): IntegratedTypography {
        
        return IntegratedTypography(
            // Headers - use custom if defined
            h1 = custom.h1 ?: native.h1,
            h2 = custom.h2 ?: native.h2,
            h3 = custom.h3 ?: native.h3,
            
            // Body text
            body1 = custom.body1 ?: native.body1,
            body2 = custom.body2 ?: native.body2,
            
            // Special text
            button = custom.getCustom("button") ?: native.button,
            caption = custom.caption ?: native.caption,
            
            // Platform-specific text styles
            nativeStyles = native.platformStyles,
            
            // Custom text styles
            customStyles = custom.getAllCustomStyles()
        )
    }
    
    /**
     * Apply native component styling
     */
    private fun applyNativeComponentStyle(
        element: VoiceUIElement,
        components: NativeComponents,
        device: DeviceProfile
    ): VoiceUIElement {
        
        return when (element.type) {
            ElementType.BUTTON -> components.styleButton(element, device)
            ElementType.TEXT_FIELD -> components.styleTextField(element, device)
            ElementType.CARD -> components.styleCard(element, device)
            ElementType.LIST -> components.styleListItem(element, device)
            ElementType.NAVIGATION_BAR -> components.styleNavigationBar(element, device)
            ElementType.TAB_BAR -> components.styleTabBar(element, device)
            ElementType.DIALOG -> components.styleDialog(element, device)
            ElementType.MENU -> components.styleMenu(element, device)
            else -> element
        }
    }
    
    /**
     * Optimize themed elements for device
     */
    private fun optimizeForDevice(
        elements: List<VoiceUIElement>,
        device: DeviceProfile
    ): List<VoiceUIElement> {
        
        return when (device.type) {
            DeviceType.PHONE -> optimizeForPhone(elements, device)
            DeviceType.TABLET -> optimizeForTablet(elements, device)
            DeviceType.SMART_GLASSES -> optimizeForSmartGlasses(elements, device)
            DeviceType.TV -> optimizeForTV(elements, device)
            DeviceType.WATCH -> optimizeForWatch(elements, device)
            DeviceType.VR_DEVICE -> optimizeForVR(elements, device)
            else -> elements
        }
    }
    
    private fun optimizeForPhone(
        elements: List<VoiceUIElement>,
        device: DeviceProfile
    ): List<VoiceUIElement> {
        
        return elements.map { element ->
            element.copy(
                styling = element.styling.copy(
                    // Optimize font sizes for mobile
                    fontSize = when {
                        element.type == ElementType.HEADING -> minOf(element.styling.fontSize, 32f)
                        element.type == ElementType.TEXT -> minOf(element.styling.fontSize, 16f)
                        else -> element.styling.fontSize
                    }
                )
            )
        }
    }
    
    private fun optimizeForSmartGlasses(
        elements: List<VoiceUIElement>,
        device: DeviceProfile
    ): List<VoiceUIElement> {
        
        return elements.map { element ->
            element.copy(
                styling = element.styling.copy(
                    // High contrast for AR visibility
                    backgroundColor = element.styling.backgroundColor.copy(alpha = 0.8f),
                    foregroundColor = Color.White,
                    
                    // Larger text for readability
                    fontSize = element.styling.fontSize * 1.2f,
                    fontWeight = DesignerFontWeight.BOLD
                ),
                
                // Enable eye tracking
                interactions = element.interactions
            )
        }
    }
    
    private fun optimizeForTV(
        elements: List<VoiceUIElement>,
        device: DeviceProfile
    ): List<VoiceUIElement> {
        
        return elements.map { element ->
            element.copy(
                styling = element.styling.copy(
                    // 10-foot UI sizing
                    fontSize = element.styling.fontSize * 1.5f,
                    padding = EdgeInsets(
                        top = element.styling.padding.top * 2f,
                        right = element.styling.padding.right * 2f,
                        bottom = element.styling.padding.bottom * 2f,
                        left = element.styling.padding.left * 2f
                    ),
                    margin = EdgeInsets(
                        top = element.styling.margin.top * 2f,
                        right = element.styling.margin.right * 2f,
                        bottom = element.styling.margin.bottom * 2f,
                        left = element.styling.margin.left * 2f
                    ),
                    
                    // Clear focus indicators (using border properties)
                    borderWidth = 4f,
                    borderColor = Color(0xFF2196F3)
                ),
                
                // D-pad navigation - keeping default interactions
                interactions = element.interactions
            )
        }
    }
    
    private fun optimizeForWatch(
        elements: List<VoiceUIElement>,
        device: DeviceProfile
    ): List<VoiceUIElement> {
        
        return elements.map { element ->
            element.copy(
                styling = element.styling.copy(
                    // Compact sizing for small screen
                    fontSize = minOf(element.styling.fontSize, 14f),
                    padding = EdgeInsets(
                        top = element.styling.padding.top * 0.5f,
                        right = element.styling.padding.right * 0.5f,
                        bottom = element.styling.padding.bottom * 0.5f,
                        left = element.styling.padding.left * 0.5f
                    ),
                    margin = EdgeInsets(
                        top = element.styling.margin.top * 0.5f,
                        right = element.styling.margin.right * 0.5f,
                        bottom = element.styling.margin.bottom * 0.5f,
                        left = element.styling.margin.left * 0.5f
                    )
                ),
                
                // Crown navigation - keeping default interactions
                interactions = element.interactions
            )
        }
    }
    
    private fun optimizeForVR(
        elements: List<VoiceUIElement>,
        device: DeviceProfile
    ): List<VoiceUIElement> {
        
        return elements.map { element ->
            element.copy(
                // 3D positioning
                position = element.position.copy(
                    z = element.position.z.coerceIn(0.5f, 5.0f) // Comfortable viewing distance
                ),
                
                styling = element.styling.copy(
                    // Readable in VR
                    fontSize = maxOf(element.styling.fontSize, 18f),
                    
                    // Depth cues
                    shadow = ShadowStyle(0f, 4f, 8f, 2f)
                ),
                
                // VR interactions - keeping default interactions
                interactions = element.interactions
            )
        }
    }
    
    private fun optimizeForTablet(
        elements: List<VoiceUIElement>,
        device: DeviceProfile
    ): List<VoiceUIElement> {
        // Tablet-specific optimizations
        return elements
    }
    
    private fun applyDeviceOverride(
        element: VoiceUIElement,
        override: ThemeOverride
    ): VoiceUIElement {
        return element.copy(
            styling = element.styling.copy(
                backgroundColor = override.colors["background"] ?: element.styling.backgroundColor,
                foregroundColor = override.colors["foreground"] ?: element.styling.foregroundColor,
                fontSize = override.typography["fontSize"]?.fontSize?.value ?: element.styling.fontSize
            )
        )
    }
}

/**
 * Data classes for theme integration
 */
data class ThemedElements(
    val elements: List<VoiceUIElement>,
    val theme: IntegratedTheme,
    val device: DeviceProfile
)

data class IntegratedTheme(
    val colors: IntegratedColorScheme,
    val typography: IntegratedTypography,
    val spacing: IntegratedSpacing,
    val shapes: CustomShapeTheme,
    val animations: IntegratedAnimations,
    val shadows: CustomShadowTheme,
    val components: NativeComponents,
    val deviceOverrides: ThemeOverride?
) {
    companion object {
        fun fromNative(native: NativeThemeBase): IntegratedTheme {
            return IntegratedTheme(
                colors = IntegratedColorScheme.fromNative(native.colors),
                typography = IntegratedTypography.fromNative(native.typography),
                spacing = IntegratedSpacing.fromNative(native.spacing),
                shapes = native.shapes,
                animations = IntegratedAnimations.fromNative(native.animations),
                shadows = native.shadows,
                components = native.components,
                deviceOverrides = null
            )
        }
    }
}

data class IntegratedColorScheme(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val error: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onError: Color,
    val nativeSemanticColors: Map<String, Color>,
    val customColors: Map<String, Color>
) {
    fun getColorForElement(type: ElementType, colorType: String): Color {
        return when (colorType) {
            "background" -> background
            "foreground" -> onBackground
            "border" -> surface
            else -> primary
        }
    }
    
    companion object {
        fun fromNative(native: NativeColorScheme): IntegratedColorScheme {
            return IntegratedColorScheme(
                primary = native.primary,
                secondary = native.secondary,
                background = native.background,
                surface = native.surface,
                error = native.error,
                onPrimary = native.onPrimary,
                onSecondary = native.onSecondary,
                onBackground = native.onBackground,
                onSurface = native.onSurface,
                onError = native.onError,
                nativeSemanticColors = native.semanticColors,
                customColors = emptyMap()
            )
        }
    }
}

data class IntegratedTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val button: TextStyle,
    val caption: TextStyle,
    val nativeStyles: Map<String, TextStyle>,
    val customStyles: Map<String, TextStyle>
) {
    fun getSizeForElement(type: ElementType): Float {
        return when (type) {
            ElementType.HEADING -> h1.fontSize.value
            ElementType.TEXT -> body1.fontSize.value
            ElementType.BUTTON -> button.fontSize.value
            ElementType.CAPTION -> caption.fontSize.value
            else -> body1.fontSize.value
        }
    }
    
    fun getWeightForElement(type: ElementType): DesignerFontWeight {
        return when (type) {
            ElementType.HEADING -> DesignerFontWeight.BOLD
            ElementType.BUTTON -> DesignerFontWeight.NORMAL
            else -> DesignerFontWeight.NORMAL
        }
    }
    
    fun getFontForElement(type: ElementType): String {
        return "default"
    }
    
    companion object {
        fun fromNative(native: NativeTypography): IntegratedTypography {
            return IntegratedTypography(
                h1 = native.h1,
                h2 = native.h2,
                h3 = native.h3,
                body1 = native.body1,
                body2 = native.body2,
                button = native.button,
                caption = native.caption,
                nativeStyles = native.platformStyles,
                customStyles = emptyMap()
            )
        }
    }
}

data class IntegratedSpacing(
    val xs: Float,
    val sm: Float,
    val md: Float,
    val lg: Float,
    val xl: Float,
    val customSpacing: Map<String, Float>
) {
    fun getPaddingForElement(type: ElementType): Float {
        return when (type) {
            ElementType.BUTTON -> md
            ElementType.CARD -> lg
            else -> sm
        }
    }
    
    fun getMarginForElement(type: ElementType): Float {
        return when (type) {
            ElementType.CARD -> md
            else -> sm
        }
    }
    
    companion object {
        fun fromNative(native: NativeSpacing): IntegratedSpacing {
            return IntegratedSpacing(
                xs = native.xs,
                sm = native.sm,
                md = native.md,
                lg = native.lg,
                xl = native.xl,
                customSpacing = emptyMap()
            )
        }
    }
}

data class IntegratedAnimations(
    val fast: AnimationStyle,
    val normal: AnimationStyle,
    val slow: AnimationStyle,
    val customAnimations: Map<String, AnimationStyle>
) {
    fun getDurationForElement(type: ElementType): Long {
        return when (type) {
            ElementType.BUTTON -> fast.duration
            ElementType.DIALOG -> normal.duration
            else -> normal.duration
        }
    }
    
    fun getEasingForElement(type: ElementType): EasingType {
        return normal.easing
    }
    
    fun getDelayForElement(type: ElementType): Long {
        return 0L
    }
    
    companion object {
        fun fromNative(native: NativeAnimations): IntegratedAnimations {
            return IntegratedAnimations(
                fast = native.fast,
                normal = native.normal,
                slow = native.slow,
                customAnimations = emptyMap()
            )
        }
    }
}

// Native platform enum
enum class NativePlatform {
    MATERIAL_3,
    MATERIAL_2,
    MATERIAL_1,
    IOS_CUPERTINO,
    WINDOWS_FLUENT,
    MACOS_AQUA,
    LINUX_GTK,
    AR_NATIVE,
    VR_SPATIAL,
    TV_LEANBACK,
    WEAR_OS,
    AUTOMOTIVE,
    GENERIC,
    NONE
}

// Abstract native theme bases
abstract class NativeThemeBase(val device: DeviceProfile) {
    abstract val colors: NativeColorScheme
    abstract val typography: NativeTypography
    abstract val spacing: NativeSpacing
    abstract val shapes: CustomShapeTheme
    abstract val animations: NativeAnimations
    abstract val shadows: CustomShadowTheme
    abstract val components: NativeComponents
}

// Platform-specific implementations
class Material3ThemeBase(device: DeviceProfile) : NativeThemeBase(device) {
    override val colors = NativeColorScheme.material3(device)
    override val typography = NativeTypography.material3()
    override val spacing = NativeSpacing.material3()
    override val shapes = createMaterial3ShapeTheme()
    override val animations = NativeAnimations.material3()
    override val shadows = createMaterial3ShadowTheme()
    override val components = MaterialComponents()
}

// Additional helper classes
data class NativeColorScheme(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val error: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onError: Color,
    val semanticColors: Map<String, Color>
) {
    companion object {
        fun material3(device: DeviceProfile): NativeColorScheme {
            return NativeColorScheme(
                primary = Color(0xFF6200EE),
                secondary = Color(0xFF03DAC6),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFFFFBFE),
                error = Color(0xFFB00020),
                onPrimary = Color.White,
                onSecondary = Color.Black,
                onBackground = Color.Black,
                onSurface = Color.Black,
                onError = Color.White,
                semanticColors = mapOf(
                    "primaryContainer" to Color(0xFFEADDFF),
                    "onPrimaryContainer" to Color(0xFF21005D)
                )
            )
        }
    }
}

// Placeholder classes for missing types
class Material2ThemeBase(device: DeviceProfile) : NativeThemeBase(device) {
    override val colors = NativeColorScheme.material3(device)
    override val typography = NativeTypography.material3()
    override val spacing = NativeSpacing.material3()
    override val shapes = createMaterial3ShapeTheme()
    override val animations = NativeAnimations.material3()
    override val shadows = createMaterial3ShadowTheme()
    override val components = MaterialComponents()
}

class CupertinoThemeBase(device: DeviceProfile) : NativeThemeBase(device) {
    override val colors = NativeColorScheme.material3(device)
    override val typography = NativeTypography.material3()
    override val spacing = NativeSpacing.material3()
    override val shapes = createMaterial3ShapeTheme()
    override val animations = NativeAnimations.material3()
    override val shadows = createMaterial3ShadowTheme()
    override val components = MaterialComponents()
}

class FluentThemeBase(device: DeviceProfile) : NativeThemeBase(device) {
    override val colors = NativeColorScheme.material3(device)
    override val typography = NativeTypography.material3()
    override val spacing = NativeSpacing.material3()
    override val shapes = createMaterial3ShapeTheme()
    override val animations = NativeAnimations.material3()
    override val shadows = createMaterial3ShadowTheme()
    override val components = MaterialComponents()
}

class AquaThemeBase(device: DeviceProfile) : NativeThemeBase(device) {
    override val colors = NativeColorScheme.material3(device)
    override val typography = NativeTypography.material3()
    override val spacing = NativeSpacing.material3()
    override val shapes = createMaterial3ShapeTheme()
    override val animations = NativeAnimations.material3()
    override val shadows = createMaterial3ShadowTheme()
    override val components = MaterialComponents()
}

class ARThemeBase(device: DeviceProfile) : NativeThemeBase(device) {
    override val colors = NativeColorScheme.material3(device)
    override val typography = NativeTypography.material3()
    override val spacing = NativeSpacing.material3()
    override val shapes = createMaterial3ShapeTheme()
    override val animations = NativeAnimations.material3()
    override val shadows = createMaterial3ShadowTheme()
    override val components = MaterialComponents()
}

class VRThemeBase(device: DeviceProfile) : NativeThemeBase(device) {
    override val colors = NativeColorScheme.material3(device)
    override val typography = NativeTypography.material3()
    override val spacing = NativeSpacing.material3()
    override val shapes = createMaterial3ShapeTheme()
    override val animations = NativeAnimations.material3()
    override val shadows = createMaterial3ShadowTheme()
    override val components = MaterialComponents()
}

class TVThemeBase(device: DeviceProfile) : NativeThemeBase(device) {
    override val colors = NativeColorScheme.material3(device)
    override val typography = NativeTypography.material3()
    override val spacing = NativeSpacing.material3()
    override val shapes = createMaterial3ShapeTheme()
    override val animations = NativeAnimations.material3()
    override val shadows = createMaterial3ShadowTheme()
    override val components = MaterialComponents()
}

class GenericThemeBase(device: DeviceProfile) : NativeThemeBase(device) {
    override val colors = NativeColorScheme.material3(device)
    override val typography = NativeTypography.material3()
    override val spacing = NativeSpacing.material3()
    override val shapes = createMaterial3ShapeTheme()
    override val animations = NativeAnimations.material3()
    override val shadows = createMaterial3ShadowTheme()
    override val components = MaterialComponents()
}

// Component styling
abstract class NativeComponents {
    abstract fun styleButton(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement
    abstract fun styleTextField(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement
    abstract fun styleCard(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement
    abstract fun styleListItem(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement
    abstract fun styleNavigationBar(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement
    abstract fun styleTabBar(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement
    abstract fun styleDialog(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement
    abstract fun styleMenu(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement
}

class MaterialComponents : NativeComponents() {
    override fun styleButton(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement {
        return element
    }
    
    override fun styleTextField(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement {
        return element
    }
    
    override fun styleCard(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement {
        return element
    }
    
    override fun styleListItem(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement {
        return element
    }
    
    override fun styleNavigationBar(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement {
        return element
    }
    
    override fun styleTabBar(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement {
        return element
    }
    
    override fun styleDialog(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement {
        return element
    }
    
    override fun styleMenu(element: VoiceUIElement, device: DeviceProfile): VoiceUIElement {
        return element
    }
}

// Additional helper data classes
data class NativeTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val button: TextStyle,
    val caption: TextStyle,
    val platformStyles: Map<String, TextStyle>
) {
    companion object {
        fun material3(): NativeTypography {
            return NativeTypography(
                h1 = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Light, fontFamily = FontFamily.Default),
                h2 = TextStyle(fontSize = 60.sp, fontWeight = FontWeight.Light, fontFamily = FontFamily.Default),
                h3 = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Default),
                body1 = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Default),
                body2 = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Default),
                button = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Default),
                caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Default),
                platformStyles = emptyMap()
            )
        }
    }
}

data class NativeSpacing(
    val xs: Float,
    val sm: Float,
    val md: Float,
    val lg: Float,
    val xl: Float
) {
    companion object {
        fun material3(): NativeSpacing {
            return NativeSpacing(
                xs = 4f,
                sm = 8f,
                md = 16f,
                lg = 24f,
                xl = 32f
            )
        }
    }
}

data class NativeAnimations(
    val fast: AnimationStyle,
    val normal: AnimationStyle,
    val slow: AnimationStyle
) {
    companion object {
        fun material3(): NativeAnimations {
            return NativeAnimations(
                fast = AnimationStyle(150, EasingType.EASE_IN_OUT),
                normal = AnimationStyle(300, EasingType.EASE_IN_OUT),
                slow = AnimationStyle(500, EasingType.EASE_IN_OUT)
            )
        }
    }
}

// Factory functions instead of extension functions on non-existent Companion
private fun createMaterial3ShapeTheme(): CustomShapeTheme {
    return CustomShapeTheme(
        mapOf(
            "small" to ShapeStyle.Rounded(4f),
            "medium" to ShapeStyle.Rounded(12f),
            "large" to ShapeStyle.Rounded(20f)
        )
    )
}

private fun createMaterial3ShadowTheme(): CustomShadowTheme {
    return CustomShadowTheme(
        mapOf(
            "none" to ShadowStyle(0f, 0f, 0f, 0f),
            "small" to ShadowStyle(0f, 2f, 4f, 0f),
            "medium" to ShadowStyle(0f, 4f, 8f, 0f),
            "large" to ShadowStyle(0f, 8f, 16f, 2f)
        )
    )
}

internal fun CustomColorScheme.getAllCustomColors(): Map<String, Color> {
    return mapOf() // Return custom colors map
}

private fun CustomTypographyTheme.getAllCustomStyles(): Map<String, TextStyle> {
    return mapOf() // Return custom styles map
}

private fun mergeSpacing(native: NativeSpacing, custom: CustomSpacingTheme): IntegratedSpacing {
    return IntegratedSpacing(
        xs = custom.xs,
        sm = custom.sm,
        md = custom.md,
        lg = custom.lg,
        xl = custom.xl,
        customSpacing = emptyMap()
    )
}

private fun mergeAnimations(native: NativeAnimations, custom: CustomAnimationTheme): IntegratedAnimations {
    return IntegratedAnimations(
        fast = custom.fast,
        normal = custom.normal,
        slow = custom.slow,
        customAnimations = emptyMap()
    )
}
