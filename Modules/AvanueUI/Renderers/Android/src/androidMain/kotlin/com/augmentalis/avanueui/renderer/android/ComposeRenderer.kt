package com.augmentalis.avanueui.renderer.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.augmentalis.avanueui.core.Component
import com.augmentalis.avanueui.core.Renderer
import com.augmentalis.avanueui.core.Theme
import com.augmentalis.avanueui.ui.core.layout.*
import com.augmentalis.avanueui.ui.core.form.*
import com.augmentalis.avanueui.ui.core.display.*
import com.augmentalis.avanueui.ui.core.feedback.*
import com.augmentalis.avanueui.ui.core.navigation.*
import com.augmentalis.avanueui.ui.core.data.*
import com.augmentalis.avanueui.renderer.android.extensions.*
import com.augmentalis.avanueui.dsl.AlertComponent as DslAlertComponent
import com.augmentalis.avanueui.dsl.BadgeComponent as DslBadgeComponent
import com.augmentalis.avanueui.dsl.ButtonComponent as DslButtonComponent
import com.augmentalis.avanueui.dsl.CardComponent as DslCardComponent
import com.augmentalis.avanueui.dsl.CheckboxComponent as DslCheckboxComponent
import com.augmentalis.avanueui.dsl.ColumnComponent as DslColumnComponent
import com.augmentalis.avanueui.dsl.ContainerComponent as DslContainerComponent
import com.augmentalis.avanueui.dsl.DatePickerComponent as DslDatePickerComponent
import com.augmentalis.avanueui.dsl.DialogComponent as DslDialogComponent
import com.augmentalis.avanueui.dsl.DropdownComponent as DslDropdownComponent
import com.augmentalis.avanueui.dsl.FileUploadComponent as DslFileUploadComponent
import com.augmentalis.avanueui.dsl.IconComponent as DslIconComponent
import com.augmentalis.avanueui.dsl.ImageComponent as DslImageComponent
import com.augmentalis.avanueui.dsl.ProgressBarComponent as DslProgressBarComponent
import com.augmentalis.avanueui.dsl.RadioComponent as DslRadioComponent
import com.augmentalis.avanueui.dsl.RatingComponent as DslRatingComponent
import com.augmentalis.avanueui.dsl.RowComponent as DslRowComponent
import com.augmentalis.avanueui.dsl.ScrollViewComponent as DslScrollViewComponent
import com.augmentalis.avanueui.dsl.SearchBarComponent as DslSearchBarComponent
import com.augmentalis.avanueui.dsl.SliderComponent as DslSliderComponent
import com.augmentalis.avanueui.dsl.SpinnerComponent as DslSpinnerComponent
import com.augmentalis.avanueui.dsl.SwitchComponent as DslSwitchComponent
import com.augmentalis.avanueui.dsl.TextComponent as DslTextComponent
import com.augmentalis.avanueui.dsl.TextFieldComponent as DslTextFieldComponent
import com.augmentalis.avanueui.dsl.TimePickerComponent as DslTimePickerComponent
import com.augmentalis.avanueui.dsl.ToastComponent as DslToastComponent
import com.augmentalis.avanueui.dsl.TooltipComponent as DslTooltipComponent

/**
 * ComposeRenderer - Converts AvaElements components to Jetpack Compose UI
 *
 * This renderer maps the AvaElements component tree to native Jetpack Compose
 * functions, supporting all modifiers, theming, and state management.
 */
class ComposeRenderer : Renderer {
    override val platform = Renderer.Platform.Android

    private val themeConverter = ThemeConverter()
    private val modifierConverter = ModifierConverter()
    private var currentTheme: Theme? = null

    override fun render(component: Component): Any {
        return when (component) {
            // Foundation components
            is ColumnComponent -> { { component.Render(this@ComposeRenderer) } }
            is RowComponent -> { { component.Render(this@ComposeRenderer) } }
            is ContainerComponent -> { { component.Render(this@ComposeRenderer) } }
            is ScrollViewComponent -> { { component.Render(this@ComposeRenderer) } }
            is CardComponent -> { { component.Render(this@ComposeRenderer) } }
            is TextComponent -> { { component.Render(this@ComposeRenderer) } }
            is ButtonComponent -> { { component.Render(this@ComposeRenderer) } }
            is TextFieldComponent -> { { component.Render(this@ComposeRenderer) } }
            is CheckboxComponent -> { { component.Render(this@ComposeRenderer) } }
            is SwitchComponent -> { { component.Render(this@ComposeRenderer) } }
            is IconComponent -> { { component.Render(this@ComposeRenderer) } }
            is ImageComponent -> { { component.Render(this@ComposeRenderer) } }

            // Navigation components
            is AppBarComponent -> { { component.Render(this@ComposeRenderer) } }
            is BottomNavComponent -> { { component.Render(this@ComposeRenderer) } }

            // Feedback components
            is ToastComponent -> { { component.Render(this@ComposeRenderer) } }
            is SnackbarComponent -> { { component.Render(this@ComposeRenderer) } }
            is ProgressBarComponent -> { { component.Render(this@ComposeRenderer) } }
            is Modal -> { { component.Render(this@ComposeRenderer) } }
            is Confirm -> { { component.Render(this@ComposeRenderer) } }
            is ContextMenu -> { { component.Render(this@ComposeRenderer) } }
            is DialogComponent -> { { component.Render(this@ComposeRenderer) } }

            // Input components
            is SliderComponent -> { { component.Render(this@ComposeRenderer) } }
            is RangeSliderComponent -> { { component.Render(this@ComposeRenderer) } }
            is DatePickerComponent -> { { component.Render(this@ComposeRenderer) } }
            is TimePickerComponent -> { { component.Render(this@ComposeRenderer) } }
            is DropdownComponent -> { { component.Render(this@ComposeRenderer) } }
            is RadioGroupComponent -> { { component.Render(this@ComposeRenderer) } }
            is AutocompleteComponent -> { { component.Render(this@ComposeRenderer) } }
            is FileUploadComponent -> { { component.Render(this@ComposeRenderer) } }
            is SearchBarComponent -> { { component.Render(this@ComposeRenderer) } }
            is RatingComponent -> { { component.Render(this@ComposeRenderer) } }

            // Advanced Layout components
            is ScaffoldComponent -> { { component.Render(this@ComposeRenderer) } }
            is LazyColumnComponent -> { { component.Render(this@ComposeRenderer) } }
            is LazyRowComponent -> { { component.Render(this@ComposeRenderer) } }
            is SpacerComponent -> { { component.Render(this@ComposeRenderer) } }
            is BoxComponent -> { { component.Render(this@ComposeRenderer) } }
            is SurfaceComponent -> { { component.Render(this@ComposeRenderer) } }

            // Advanced Display components
            is ListTileComponent -> { { component.Render(this@ComposeRenderer) } }
            is TabBarComponent -> { { component.Render(this@ComposeRenderer) } }
            is CircularProgressComponent -> { { component.Render(this@ComposeRenderer) } }

            // Advanced Navigation components
            is NavigationDrawerComponent -> { { component.Render(this@ComposeRenderer) } }
            is NavigationRailComponent -> { { component.Render(this@ComposeRenderer) } }
            is BottomAppBarComponent -> { { component.Render(this@ComposeRenderer) } }

            // Advanced Input components
            is SegmentedButtonComponent -> { { component.Render(this@ComposeRenderer) } }
            is TextButtonComponent -> { { component.Render(this@ComposeRenderer) } }
            is OutlinedButtonComponent -> { { component.Render(this@ComposeRenderer) } }
            is FilledButtonComponent -> { { component.Render(this@ComposeRenderer) } }
            is IconButtonComponent -> { { component.Render(this@ComposeRenderer) } }

            // Advanced Feedback components
            is BottomSheetComponent -> { { component.Render(this@ComposeRenderer) } }
            is LoadingDialogComponent -> { { component.Render(this@ComposeRenderer) } }

            // Data components
            is AccordionComponent -> { { component.Render(this@ComposeRenderer) } }
            is CarouselComponent -> { { component.Render(this@ComposeRenderer) } }
            is TimelineComponent -> { { component.Render(this@ComposeRenderer) } }
            is DataGridComponent -> { { component.Render(this@ComposeRenderer) } }
            is DataTableComponent -> { { component.Render(this@ComposeRenderer) } }
            is ListComponent -> { { component.Render(this@ComposeRenderer) } }
            is TreeViewComponent -> { { component.Render(this@ComposeRenderer) } }
            is ChipComponent -> { { component.Render(this@ComposeRenderer) } }
            is PaperComponent -> { { component.Render(this@ComposeRenderer) } }
            is EmptyStateComponent -> { { component.Render(this@ComposeRenderer) } }

            // DSL components (from AvaUI { } builder in com.augmentalis.avanueui.dsl)
            is DslButtonComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslTextComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslCardComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslTextFieldComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslImageComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslColumnComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslRowComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslContainerComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslScrollViewComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslCheckboxComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslSwitchComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslIconComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslRadioComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslSliderComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslDropdownComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslDatePickerComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslTimePickerComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslFileUploadComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslSearchBarComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslRatingComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslDialogComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslToastComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslAlertComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslProgressBarComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslSpinnerComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslBadgeComponent -> { { component.RenderDsl(this@ComposeRenderer) } }
            is DslTooltipComponent -> { { component.RenderDsl(this@ComposeRenderer) } }

            else -> throw IllegalArgumentException("Unsupported component type: ${component::class.simpleName}")
        }
    }

    override fun applyTheme(theme: Theme) {
        currentTheme = theme
    }

    fun getTheme(): Theme? = currentTheme

    /**
     * Utility method for extension functions to convert modifiers
     */
    fun convertModifiers(modifiers: List<com.augmentalis.avanueui.core.Modifier>): androidx.compose.ui.Modifier {
        return modifierConverter.convert(modifiers)
    }

    /**
     * Utility method for extension functions to get theme converter
     */
    fun getThemeConverter(): ThemeConverter = themeConverter

    /**
     * Render a component as a @Composable function
     */
    @Composable
    fun RenderComponent(component: Component) {
        val composable = render(component) as @Composable () -> Unit
        composable()
    }

    /**
     * Render component with theme applied
     */
    @Composable
    fun RenderWithTheme(component: Component, theme: Theme? = null) {
        val activeTheme = theme ?: currentTheme

        if (activeTheme != null) {
            themeConverter.WithMaterialTheme(activeTheme) {
                RenderComponent(component)
            }
        } else {
            RenderComponent(component)
        }
    }
}

/**
 * CompositionLocal for accessing the renderer in nested components
 */
val LocalComposeRenderer = compositionLocalOf<ComposeRenderer> {
    error("No ComposeRenderer provided")
}
