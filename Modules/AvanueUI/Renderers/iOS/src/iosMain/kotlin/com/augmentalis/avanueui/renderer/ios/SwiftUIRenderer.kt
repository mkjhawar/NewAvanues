package com.augmentalis.avanueui.renderer.ios

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.dsl.*
import com.augmentalis.avanueui.renderer.ios.bridge.*
import com.augmentalis.avanueui.renderer.ios.mappers.*
import com.augmentalis.avanueui.phase3.input.*
import com.augmentalis.avanueui.phase3.display.*
import com.augmentalis.avanueui.phase3.layout.*
import com.augmentalis.avanueui.phase3.navigation.*
import com.augmentalis.avanueui.phase3.feedback.*

/**
 * SwiftUI Renderer for iOS
 *
 * Renders AvaElements components to SwiftUI bridge models that can be
 * consumed by Swift code to create native SwiftUI views.
 *
 * Architecture:
 * AvaElements Component → SwiftUIRenderer → SwiftUIView (Bridge) → Swift → Native SwiftUI
 *
 * Usage (Kotlin side):
 * ```kotlin
 * val renderer = SwiftUIRenderer()
 * renderer.applyTheme(Themes.iOS26LiquidGlass)
 * val swiftView = renderer.render(component) as SwiftUIView
 * // Pass swiftView to Swift code
 * ```
 *
 * Usage (Swift side):
 * ```swift
 * let renderer = SwiftUIRenderer()
 * let swiftView = renderer.render(component: myComponent)
 * AvaElementsView(component: swiftView)
 * ```
 */
class SwiftUIRenderer : Renderer {

    override val platform: Renderer.Platform = Renderer.Platform.iOS

    private var currentTheme: Theme? = null
    private val themeConverter = iOSThemeConverter()

    /**
     * Design tokens generated from the current theme
     */
    var designTokens: iOSThemeConverter.iOSDesignTokens? = null
        private set

    /**
     * Apply a theme to the renderer
     * This generates iOS-specific design tokens
     */
    override fun applyTheme(theme: Theme) {
        currentTheme = theme
        designTokens = themeConverter.convert(theme)
    }

    /**
     * Render a component tree to SwiftUI bridge representation
     *
     * @param component The root component to render
     * @return SwiftUIView that can be consumed by Swift code
     */
    override fun render(component: Component): Any {
        return renderComponent(component)
    }

    /**
     * Internal recursive renderer
     */
    private fun renderComponent(component: Component): SwiftUIView {
        return when (component) {
            // Phase 1: Layout components
            is ColumnComponent -> ColumnMapper.map(component, currentTheme, ::renderComponent)
            is RowComponent -> RowMapper.map(component, currentTheme, ::renderComponent)
            is ContainerComponent -> ContainerMapper.map(component, currentTheme, ::renderComponent)
            is ScrollViewComponent -> ScrollViewMapper.map(component, currentTheme, ::renderComponent)
            is CardComponent -> CardMapper.map(component, currentTheme, ::renderComponent)

            // Phase 1: Basic components
            is TextComponent -> TextMapper.map(component, currentTheme)
            is ButtonComponent -> ButtonMapper.map(component, currentTheme)
            is TextFieldComponent -> TextFieldMapper.map(component, currentTheme)
            is CheckboxComponent -> CheckboxMapper.map(component, currentTheme)
            is SwitchComponent -> SwitchMapper.map(component, currentTheme)
            is IconComponent -> IconMapper.map(component, currentTheme)
            is ImageComponent -> ImageMapper.map(component, currentTheme)

            // Phase 3: Input components
            is Slider -> SliderMapper.map(component, currentTheme)
            is RangeSlider -> RangeSliderMapper.map(component, currentTheme)
            is DatePicker -> DatePickerMapper.map(component, currentTheme)
            is TimePicker -> TimePickerMapper.map(component, currentTheme)
            is RadioButton -> RadioButtonMapper.map(component, currentTheme)
            is RadioGroup -> RadioGroupMapper.map(component, currentTheme)
            is Dropdown -> DropdownMapper.map(component, currentTheme)
            is Autocomplete -> AutocompleteMapper.map(component, currentTheme)
            is FileUpload -> FileUploadMapper.map(component, currentTheme)
            is ImagePicker -> ImagePickerMapper.map(component, currentTheme)
            is Rating -> RatingMapper.map(component, currentTheme)
            is SearchBar -> SearchBarMapper.map(component, currentTheme)

            // Phase 3: Display components
            is Badge -> BadgeMapper.map(component, currentTheme)
            is Chip -> ChipMapper.map(component, currentTheme)
            is Avatar -> AvatarMapper.map(component, currentTheme)
            is Divider -> DividerMapper.map(component, currentTheme)
            is Skeleton -> SkeletonMapper.map(component, currentTheme)
            is Spinner -> SpinnerMapper.map(component, currentTheme)
            is ProgressBar -> ProgressBarMapper.map(component, currentTheme)
            is Tooltip -> TooltipMapper.map(component, currentTheme)

            // Phase 3: Layout components
            is Grid -> GridMapper.map(component, currentTheme, ::renderComponent)
            is Stack -> StackMapper.map(component, currentTheme, ::renderComponent)
            is Spacer -> SpacerMapper.map(component, currentTheme)
            is Drawer -> DrawerMapper.map(component, currentTheme, ::renderComponent)
            is Tabs -> TabsMapper.map(component, currentTheme, ::renderComponent)

            // Phase 3: Navigation components
            is AppBar -> AppBarMapper.map(component, currentTheme, ::renderComponent)
            is BottomNav -> BottomNavMapper.map(component, currentTheme)
            is Breadcrumb -> BreadcrumbMapper.map(component, currentTheme)
            is Pagination -> PaginationMapper.map(component, currentTheme)

            // Phase 3: Feedback components
            is Alert -> AlertMapper.map(component, currentTheme)
            is Snackbar -> SnackbarMapper.map(component, currentTheme)
            is Modal -> ModalMapper.map(component, currentTheme, ::renderComponent)
            is Toast -> ToastMapper.map(component, currentTheme)
            is Confirm -> ConfirmMapper.map(component, currentTheme)
            is ContextMenu -> ContextMenuMapper.map(component, currentTheme, ::renderComponent)

            else -> {
                // Fallback for unknown component types
                SwiftUIView(
                    type = ViewType.EmptyView,
                    properties = mapOf("error" to "Unknown component type: ${component::class.simpleName}")
                )
            }
        }
    }

    /**
     * Render a AvaUI tree (convenience method)
     */
    fun renderUI(ui: AvaUI): SwiftUIView? {
        // Apply theme if present
        ui.theme?.let { applyTheme(it) }

        // Render root component
        return ui.root?.let { renderComponent(it) }
    }

    /**
     * Get color from theme by semantic name
     */
    fun getThemeColor(colorName: String): SwiftUIColor? {
        return designTokens?.colors?.get(colorName)
    }

    /**
     * Get font from theme by semantic name
     */
    fun getThemeFont(fontName: String): iOSThemeConverter.FontDefinition? {
        return designTokens?.fonts?.get(fontName)
    }

    /**
     * Get shape (corner radius) from theme by size
     */
    fun getThemeShape(shapeName: String): Float? {
        return designTokens?.shapes?.get(shapeName)
    }

    /**
     * Get spacing value from theme
     */
    fun getThemeSpacing(spacingName: String): Float? {
        return designTokens?.spacing?.get(spacingName)
    }

    /**
     * Get elevation (shadow) from theme
     */
    fun getThemeElevation(elevationName: String): ShadowValue? {
        return designTokens?.elevation?.get(elevationName)
    }

    /**
     * Check if current theme uses Liquid Glass material
     */
    fun usesLiquidGlass(): Boolean {
        return currentTheme?.platform == ThemePlatform.iOS26_LiquidGlass
    }

    /**
     * Check if current theme uses spatial glass (visionOS)
     */
    fun usesSpatialGlass(): Boolean {
        return currentTheme?.platform == ThemePlatform.visionOS2_SpatialGlass
    }

    /**
     * Get material effect tokens
     */
    fun getMaterialTokens(): iOSThemeConverter.MaterialTokens? {
        return designTokens?.material
    }

    /**
     * Create a preview SwiftUIView for testing
     */
    fun createPreview(component: Component, theme: Theme = Themes.iOS26LiquidGlass): SwiftUIView {
        applyTheme(theme)
        return renderComponent(component)
    }

    companion object {
        /**
         * Create a renderer with iOS 26 Liquid Glass theme
         */
        fun withLiquidGlass(): SwiftUIRenderer {
            return SwiftUIRenderer().apply {
                applyTheme(Themes.iOS26LiquidGlass)
            }
        }

        /**
         * Create a renderer with visionOS 2 Spatial Glass theme
         */
        fun withSpatialGlass(): SwiftUIRenderer {
            return SwiftUIRenderer().apply {
                applyTheme(Themes.visionOS2SpatialGlass)
            }
        }

        /**
         * Create a renderer with Material Design 3 theme
         */
        fun withMaterial3(): SwiftUIRenderer {
            return SwiftUIRenderer().apply {
                applyTheme(Themes.Material3Light)
            }
        }
    }
}

/**
 * Extension function to render any component directly
 */
fun Component.toSwiftUI(theme: Theme? = null): SwiftUIView {
    val renderer = SwiftUIRenderer()
    theme?.let { renderer.applyTheme(it) }
    return renderer.render(this) as SwiftUIView
}

/**
 * Extension function to render AvaUI directly
 */
fun AvaUI.toSwiftUI(): SwiftUIView? {
    val renderer = SwiftUIRenderer()
    return renderer.renderUI(this)
}
