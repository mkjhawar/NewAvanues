package com.augmentalis.avaelements.renderer.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.augmentalis.magicui.components.core.Component
import com.augmentalis.magicui.components.core.Renderer
import com.augmentalis.magicui.components.core.Theme
import com.augmentalis.magicui.ui.core.layout.*
import com.augmentalis.magicui.ui.core.form.*
import com.augmentalis.magicui.ui.core.display.*
import com.augmentalis.magicui.ui.core.feedback.*
import com.augmentalis.magicui.ui.core.navigation.*
import com.augmentalis.magicui.ui.core.data.*
import com.augmentalis.avaelements.renderer.android.mappers.*
import com.augmentalis.avaelements.renderer.android.mappers.input.*
import com.augmentalis.avaelements.renderer.android.mappers.layout.*
import com.augmentalis.avaelements.renderer.android.mappers.display.*
import com.augmentalis.avaelements.renderer.android.mappers.navigation.*
import com.augmentalis.avaelements.renderer.android.mappers.feedback.*
import com.augmentalis.avaelements.renderer.android.mappers.data.*

/**
 * ComposeRenderer - Converts AvaElements components to Jetpack Compose UI
 *
 * This renderer maps the AvaElements component tree to native Jetpack Compose
 * functions, supporting all modifiers, theming, and state management.
 */
class ComposeRenderer : Renderer {
    override val platform = Renderer.Platform.Android

    private val themeConverter = ThemeConverter()
    private var currentTheme: Theme? = null

    // Component mappers - Foundation
    private val columnMapper = ColumnMapper()
    private val rowMapper = RowMapper()
    private val containerMapper = ContainerMapper()
    private val scrollViewMapper = ScrollViewMapper()
    private val cardMapper = CardMapper()
    private val textMapper = TextMapper()
    private val buttonMapper = ButtonMapper()
    private val textFieldMapper = TextFieldMapper()
    private val checkboxMapper = CheckboxMapper()
    private val switchMapper = SwitchMapper()
    private val iconMapper = IconMapper()
    private val imageMapper = ImageMapper()

    // Component mappers - Navigation
    private val appBarMapper = AppBarMapper()
    private val bottomNavMapper = BottomNavMapper()

    // Component mappers - Feedback
    private val toastMapper = ToastMapper()
    private val snackbarMapper = SnackbarMapper()
    private val progressBarMapper = ProgressBarMapper()
    private val modalMapper = ModalMapper()
    private val confirmMapper = ConfirmMapper()
    private val contextMenuMapper = ContextMenuMapper()

    // Component mappers - Input
    private val sliderMapper = SliderMapper()
    private val rangeSliderMapper = RangeSliderMapper()
    private val datePickerMapper = DatePickerMapper()
    private val timePickerMapper = TimePickerMapper()
    private val dropdownMapper = DropdownMapper()
    private val radioGroupMapper = RadioGroupMapper()
    private val autocompleteMapper = AutocompleteMapper()
    private val fileUploadMapper = FileUploadMapper()
    private val searchBarMapper = SearchBarMapper()
    private val ratingMapper = RatingMapper()

    // Component mappers - Advanced Layout
    private val scaffoldMapper = ScaffoldMapper()
    private val lazyColumnMapper = LazyColumnMapper()
    private val lazyRowMapper = LazyRowMapper()
    private val spacerMapper = SpacerMapper()
    private val boxMapper = BoxMapper()
    private val surfaceMapper = SurfaceMapper()

    // Component mappers - Advanced Display
    private val listTileMapper = ListTileMapper()
    private val tabBarMapper = TabBarMapper()
    private val circularProgressMapper = CircularProgressMapper()

    // Component mappers - Advanced Navigation
    private val navigationDrawerMapper = NavigationDrawerMapper()
    private val navigationRailMapper = NavigationRailMapper()
    private val bottomAppBarMapper = BottomAppBarMapper()

    // Component mappers - Advanced Input
    private val segmentedButtonMapper = SegmentedButtonMapper()
    private val textButtonMapper = TextButtonMapper()
    private val outlinedButtonMapper = OutlinedButtonMapper()
    private val filledButtonMapper = FilledButtonMapper()
    private val iconButtonMapper = IconButtonMapper()

    // Component mappers - Advanced Feedback
    private val bottomSheetMapper = BottomSheetMapper()
    private val loadingDialogMapper = LoadingDialogMapper()

    // Component mappers - Data Components
    private val accordionMapper = AccordionMapper()
    private val carouselMapper = CarouselMapper()
    private val timelineMapper = TimelineMapper()
    private val dataGridMapper = DataGridMapper()
    private val dataTableMapper = DataTableMapper()
    private val listComponentMapper = ListComponentMapper()
    private val treeViewMapper = TreeViewMapper()
    private val chipComponentMapper = ChipComponentMapper()
    private val paperMapper = PaperMapper()
    private val emptyStateMapper = EmptyStateMapper()

    override fun render(component: Component): Any {
        return when (component) {
            // Foundation components
            is ColumnComponent -> columnMapper.map(component, this)
            is RowComponent -> rowMapper.map(component, this)
            is ContainerComponent -> containerMapper.map(component, this)
            is ScrollViewComponent -> scrollViewMapper.map(component, this)
            is CardComponent -> cardMapper.map(component, this)
            is TextComponent -> textMapper.map(component, this)
            is ButtonComponent -> buttonMapper.map(component, this)
            is TextFieldComponent -> textFieldMapper.map(component, this)
            is CheckboxComponent -> checkboxMapper.map(component, this)
            is SwitchComponent -> switchMapper.map(component, this)
            is IconComponent -> iconMapper.map(component, this)
            is ImageComponent -> imageMapper.map(component, this)

            // Navigation components
            // is AppBarComponent -> appBarMapper.map(component, this)  // TODO: AppBarComponent not defined
            is BottomNavComponent -> bottomNavMapper.map(component, this)

            // Feedback components
            is ToastComponent -> toastMapper.map(component, this)
            is SnackbarComponent -> snackbarMapper.map(component, this)
            is ProgressBarComponent -> progressBarMapper.map(component, this)
            is Modal -> modalMapper.map(component, this)
            is Confirm -> confirmMapper.map(component, this)
            is ContextMenu -> contextMenuMapper.map(component, this)

            // Input components
            is SliderComponent -> sliderMapper.map(component, this)
            is RangeSliderComponent -> rangeSliderMapper.map(component, this)
            is DatePickerComponent -> datePickerMapper.map(component, this)
            is TimePickerComponent -> timePickerMapper.map(component, this)
            is DropdownComponent -> dropdownMapper.map(component, this)
            is RadioGroupComponent -> radioGroupMapper.map(component, this)
            is AutocompleteComponent -> autocompleteMapper.map(component, this)
            is FileUploadComponent -> fileUploadMapper.map(component, this)
            is SearchBarComponent -> searchBarMapper.map(component, this)
            is RatingComponent -> ratingMapper.map(component, this)

            // Advanced Layout components
            is ScaffoldComponent -> scaffoldMapper.map(component, this)
            is LazyColumnComponent -> lazyColumnMapper.map(component, this)
            is LazyRowComponent -> lazyRowMapper.map(component, this)
            is SpacerComponent -> spacerMapper.map(component, this)
            is BoxComponent -> boxMapper.map(component, this)
            is SurfaceComponent -> surfaceMapper.map(component, this)

            // Advanced Display components
            is ListTileComponent -> listTileMapper.map(component, this)
            is TabBarComponent -> tabBarMapper.map(component, this)
            is CircularProgressComponent -> circularProgressMapper.map(component, this)

            // Advanced Navigation components
            is NavigationDrawerComponent -> navigationDrawerMapper.map(component, this)
            is NavigationRailComponent -> navigationRailMapper.map(component, this)
            is BottomAppBarComponent -> bottomAppBarMapper.map(component, this)

            // Advanced Input components
            is SegmentedButtonComponent -> segmentedButtonMapper.map(component, this)
            is TextButtonComponent -> textButtonMapper.map(component, this)
            is OutlinedButtonComponent -> outlinedButtonMapper.map(component, this)
            is FilledButtonComponent -> filledButtonMapper.map(component, this)
            is IconButtonComponent -> iconButtonMapper.map(component, this)

            // Advanced Feedback components
            is BottomSheetComponent -> bottomSheetMapper.map(component, this)
            is LoadingDialogComponent -> loadingDialogMapper.map(component, this)

            // Data components
            is AccordionComponent -> accordionMapper.map(component, this)
            is CarouselComponent -> carouselMapper.map(component, this)
            is TimelineComponent -> timelineMapper.map(component, this)
            is DataGridComponent -> dataGridMapper.map(component, this)
            is DataTableComponent -> dataTableMapper.map(component, this)
            is ListComponent -> listComponentMapper.map(component, this)
            is TreeViewComponent -> treeViewMapper.map(component, this)
            is ChipComponent -> chipComponentMapper.map(component, this)
            is PaperComponent -> paperMapper.map(component, this)
            is EmptyStateComponent -> emptyStateMapper.map(component, this)

            else -> throw IllegalArgumentException("Unsupported component type: ${component::class.simpleName}")
        }
    }

    override fun applyTheme(theme: Theme) {
        currentTheme = theme
    }

    fun getTheme(): Theme? = currentTheme

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
 * Base mapper interface for component conversion
 */
interface ComponentMapper<T : Component> {
    fun map(component: T, renderer: ComposeRenderer): @Composable () -> Unit
}

/**
 * CompositionLocal for accessing the renderer in nested components
 */
val LocalComposeRenderer = compositionLocalOf<ComposeRenderer> {
    error("No ComposeRenderer provided")
}
