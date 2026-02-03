package com.augmentalis.avaelements.renderer.ios

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.dsl.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.renderer.ios.mappers.*
import com.augmentalis.magicelements.renderer.ios.mappers.*
// Explicit imports for Flutter mappers
import com.augmentalis.magicelements.renderer.ios.mappers.FilterChipMapper
import com.augmentalis.magicelements.renderer.ios.mappers.InputChipMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ActionChipMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ChoiceChipMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ExpansionTileMapper
import com.augmentalis.magicelements.renderer.ios.mappers.CheckboxListTileMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SwitchListTileMapper
import com.augmentalis.magicelements.renderer.ios.mappers.FilledButtonMapper
import com.augmentalis.magicelements.renderer.ios.mappers.PricingCardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.FeatureCardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.TestimonialCardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ProductCardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ArticleCardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ImageCardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.HoverCardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ExpandableCardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ErrorStateMapper
import com.augmentalis.magicelements.renderer.ios.mappers.NoDataMapper
import com.augmentalis.magicelements.renderer.ios.mappers.LazyImageMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ImageGalleryMapper
import com.augmentalis.magicelements.renderer.ios.mappers.LightboxMapper
import com.augmentalis.magicelements.renderer.ios.mappers.PopupMapper
import com.augmentalis.magicelements.renderer.ios.mappers.CalloutMapper
import com.augmentalis.magicelements.renderer.ios.mappers.DisclosureMapper
import com.augmentalis.magicelements.renderer.ios.mappers.InfoPanelMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ErrorPanelMapper
import com.augmentalis.magicelements.renderer.ios.mappers.WarningPanelMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SuccessPanelMapper
import com.augmentalis.magicelements.renderer.ios.mappers.FullPageLoadingMapper
import com.augmentalis.magicelements.renderer.ios.mappers.MenuMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SidebarMapper
import com.augmentalis.magicelements.renderer.ios.mappers.NavLinkMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ProgressStepperMapper
import com.augmentalis.magicelements.renderer.ios.mappers.MenuBarMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SubMenuMapper
import com.augmentalis.magicelements.renderer.ios.mappers.VerticalTabsMapper
import com.augmentalis.magicelements.renderer.ios.mappers.PopupMenuButtonMapper
// New Flutter Parity mappers
import com.augmentalis.magicelements.renderer.ios.mappers.DataListMapper
import com.augmentalis.magicelements.renderer.ios.mappers.DescriptionListMapper
import com.augmentalis.magicelements.renderer.ios.mappers.StatGroupMapper
import com.augmentalis.magicelements.renderer.ios.mappers.StatMapper
import com.augmentalis.magicelements.renderer.ios.mappers.KPIMapper
import com.augmentalis.magicelements.renderer.ios.mappers.MetricCardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.LeaderboardMapper
import com.augmentalis.magicelements.renderer.ios.mappers.RankingMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ZoomMapper
import com.augmentalis.magicelements.renderer.ios.mappers.VirtualScrollMapper
import com.augmentalis.magicelements.renderer.ios.mappers.InfiniteScrollMapper
import com.augmentalis.magicelements.renderer.ios.mappers.QRCodeMapper
import com.augmentalis.magicelements.renderer.ios.mappers.RichTextMapper
import com.augmentalis.magicelements.renderer.ios.mappers.PinInputMapper
import com.augmentalis.magicelements.renderer.ios.mappers.OTPInputMapper
import com.augmentalis.magicelements.renderer.ios.mappers.MaskInputMapper
import com.augmentalis.magicelements.renderer.ios.mappers.RichTextEditorMapper
import com.augmentalis.magicelements.renderer.ios.mappers.MarkdownEditorMapper
import com.augmentalis.magicelements.renderer.ios.mappers.CodeEditorMapper
import com.augmentalis.magicelements.renderer.ios.mappers.FormSectionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.MultiSelectMapper
import com.augmentalis.magicelements.renderer.ios.mappers.CalendarMapper
import com.augmentalis.magicelements.renderer.ios.mappers.DateCalendarMapper
import com.augmentalis.magicelements.renderer.ios.mappers.MonthCalendarMapper
import com.augmentalis.magicelements.renderer.ios.mappers.WeekCalendarMapper
import com.augmentalis.magicelements.renderer.ios.mappers.EventCalendarMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ListViewBuilderMapper
import com.augmentalis.magicelements.renderer.ios.mappers.GridViewBuilderMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ListViewSeparatedMapper
import com.augmentalis.magicelements.renderer.ios.mappers.PageViewMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ReorderableListViewMapper
import com.augmentalis.magicelements.renderer.ios.mappers.CustomScrollViewMapper
import com.augmentalis.magicelements.renderer.ios.mappers.IndexedStackMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedContainerMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedOpacityMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedPositionedMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedDefaultTextStyleMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedPaddingMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedSizeMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedAlignMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedScaleMapper
import com.augmentalis.magicelements.renderer.ios.mappers.FadeTransitionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SlideTransitionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.HeroMapper
import com.augmentalis.magicelements.renderer.ios.mappers.ScaleTransitionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.RotationTransitionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.PositionedTransitionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SizeTransitionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedCrossFadeMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedSwitcherMapper
import com.augmentalis.magicelements.renderer.ios.mappers.DecoratedBoxTransitionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AlignTransitionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SliverListMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SliverGridMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SliverFixedExtentListMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SliverAppBarMapper
import com.augmentalis.magicelements.renderer.ios.mappers.FadeInImageMapper
import com.augmentalis.magicelements.renderer.ios.mappers.CircleAvatarMapper
import com.augmentalis.magicelements.renderer.ios.mappers.SelectableTextMapper
import com.augmentalis.magicelements.renderer.ios.mappers.VerticalDividerMapper
import com.augmentalis.magicelements.renderer.ios.mappers.EndDrawerMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedListMapper
import com.augmentalis.magicelements.renderer.ios.mappers.AnimatedModalBarrierMapper
import com.augmentalis.magicelements.renderer.ios.mappers.DefaultTextStyleTransitionMapper
import com.augmentalis.magicelements.renderer.ios.mappers.RelativePositionedTransitionMapper
import com.augmentalis.avaelements.components.phase3.input.*
import com.augmentalis.avaelements.components.phase3.display.*
import com.augmentalis.avaelements.components.phase3.layout.*
import com.augmentalis.avaelements.components.phase3.navigation.*
import com.augmentalis.avaelements.components.phase3.feedback.*
import com.augmentalis.avaelements.components.phase3.data.*
import com.augmentalis.avaelements.components.phase3.data.ListComponent
import com.augmentalis.avaelements.flutter.material.chips.*
import com.augmentalis.avaelements.flutter.material.lists.*
import com.augmentalis.avaelements.flutter.material.advanced.*
import com.augmentalis.avaelements.flutter.material.cards.*
import com.augmentalis.avaelements.flutter.material.display.*
import com.augmentalis.avaelements.flutter.material.display.Lightbox
import com.augmentalis.avaelements.flutter.material.feedback.*
import com.augmentalis.avaelements.flutter.material.navigation.*
import com.augmentalis.avaelements.flutter.material.layout.MasonryGrid
import com.augmentalis.avaelements.flutter.material.layout.AspectRatio as FlutterAspectRatio
import com.augmentalis.avaelements.flutter.material.data.*
import com.augmentalis.avaelements.flutter.material.input.*
import com.augmentalis.avaelements.flutter.layout.*
import com.augmentalis.avaelements.flutter.layout.scrolling.*
import com.augmentalis.avaelements.flutter.animation.*
import com.augmentalis.avaelements.flutter.animation.transitions.*
import com.augmentalis.avaelements.flutter.material.advanced.IndexedStack
import com.augmentalis.avaelements.flutter.material.advanced.VerticalDivider
import com.augmentalis.avaelements.flutter.material.advanced.RichText

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
            is Divider -> Phase3DividerMapper.map(component, currentTheme)
            is Skeleton -> SkeletonMapper.map(component, currentTheme)
            is Spinner -> SpinnerMapper.map(component, currentTheme)
            is ProgressBar -> ProgressBarMapper.map(component, currentTheme)
            is Tooltip -> TooltipMapper.map(component, currentTheme)

            // Phase 3: Layout components
            is Grid -> GridMapper.map(component, currentTheme)
            is Stack -> StackMapper.map(component, currentTheme)
            is Spacer -> Phase3SpacerMapper.map(component, currentTheme)
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
            is Modal -> ModalMapper.map(component, currentTheme)
            is Toast -> ToastMapper.map(component, currentTheme)
            is Confirm -> ConfirmMapper.map(component, currentTheme)
            is ContextMenu -> ContextMenuMapper.map(component, currentTheme)

            // Phase 3: Data components
            is Table -> TableMapper.map(component, currentTheme, ::renderComponent)
            is ListComponent -> ListMapper.map(component, currentTheme, ::renderComponent)
            is Accordion -> AccordionMapper.map(component, currentTheme, ::renderComponent)
            is Stepper -> StepperMapper.map(component, currentTheme)
            is Timeline -> TimelineMapper.map(component, currentTheme, ::renderComponent)
            is TreeView -> TreeViewMapper.map(component, currentTheme, ::renderComponent)
            is Carousel -> CarouselMapper.map(component, currentTheme, ::renderComponent)
            is Paper -> PaperMapper.map(component, currentTheme, ::renderComponent)
            is EmptyState -> EmptyStateMapper.map(component, currentTheme, ::renderComponent)
            is DataGrid -> DataGridMapper.map(component, currentTheme, ::renderComponent)

            // Flutter Parity: Chips
            is FilterChip -> FilterChipMapper.map(component, currentTheme, ::renderComponent)
            is InputChip -> InputChipMapper.map(component, currentTheme, ::renderComponent)
            is ActionChip -> ActionChipMapper.map(component, currentTheme, ::renderComponent)
            is ChoiceChip -> ChoiceChipMapper.map(component, currentTheme, ::renderComponent)

            // Flutter Parity: Lists
            is ExpansionTile -> ExpansionTileMapper.map(component, currentTheme, ::renderComponent)
            is CheckboxListTile -> CheckboxListTileMapper.map(component, currentTheme, ::renderComponent)
            is SwitchListTile -> SwitchListTileMapper.map(component, currentTheme, ::renderComponent)
            is com.augmentalis.avaelements.flutter.material.data.RadioListTile -> RadioListTileMapper.map(component, currentTheme, ::renderComponent)

            // Flutter Parity: Buttons
            is FilledButton -> FilledButtonMapper.map(component, currentTheme, ::renderComponent)
            is CloseButton -> CloseButtonMapper.map(component, currentTheme, ::renderComponent)
            is LoadingButton -> LoadingButtonMapper.map(component, currentTheme, ::renderComponent)

            // Flutter Parity: Cards
            is PricingCard -> PricingCardMapper.map(component, currentTheme, ::renderComponent)
            is FeatureCard -> FeatureCardMapper.map(component, currentTheme, ::renderComponent)
            is TestimonialCard -> TestimonialCardMapper.map(component, currentTheme, ::renderComponent)
            is ProductCard -> ProductCardMapper.map(component, currentTheme, ::renderComponent)
            is ArticleCard -> ArticleCardMapper.map(component, currentTheme, ::renderComponent)
            is ImageCard -> ImageCardMapper.map(component, currentTheme, ::renderComponent)
            is com.augmentalis.avaelements.flutter.material.feedback.HoverCard -> HoverCardMapper.map(component, currentTheme, ::renderComponent)
            is ExpandableCard -> ExpandableCardMapper.map(component, currentTheme, ::renderComponent)

            // Flutter Parity: Display
            is AvatarGroup -> AvatarGroupMapper.map(component, currentTheme, ::renderComponent)
            is SkeletonText -> SkeletonTextMapper.map(component, currentTheme)
            is SkeletonCircle -> SkeletonCircleMapper.map(component, currentTheme)
            is ProgressCircle -> ProgressCircleMapper.map(component, currentTheme)
            is LoadingOverlay -> LoadingOverlayMapper.map(component, currentTheme)
            is Popover -> PopoverMapper.map(component, currentTheme)
            is ErrorState -> ErrorStateMapper.map(component, currentTheme)
            is NoData -> NoDataMapper.map(component, currentTheme)
            is ImageCarousel -> ImageCarouselMapper.map(component, currentTheme)
            is com.augmentalis.avaelements.flutter.material.display.LazyImage -> LazyImageMapper.map(component, currentTheme)
            is com.augmentalis.avaelements.flutter.material.display.ImageGallery -> ImageGalleryMapper.map(component, currentTheme)
            is Lightbox -> LightboxMapper.map(component, currentTheme)

            // Flutter Parity: Feedback
            is Popup -> PopupMapper.map(component, currentTheme)
            is Callout -> CalloutMapper.map(component, currentTheme)
            is Disclosure -> DisclosureMapper.map(component, currentTheme)
            is InfoPanel -> InfoPanelMapper.map(component, currentTheme)
            is ErrorPanel -> ErrorPanelMapper.map(component, currentTheme)
            is WarningPanel -> WarningPanelMapper.map(component, currentTheme)
            is SuccessPanel -> SuccessPanelMapper.map(component, currentTheme)
            is FullPageLoading -> FullPageLoadingMapper.map(component, currentTheme)
            is AnimatedCheck -> AnimatedCheckMapper.map(component, currentTheme)
            is AnimatedError -> AnimatedErrorMapper.map(component, currentTheme)

            // Flutter Parity: Navigation
            is Menu -> MenuMapper.map(component, currentTheme, ::renderComponent)
            is Sidebar -> SidebarMapper.map(component, currentTheme, ::renderComponent)
            is NavLink -> NavLinkMapper.map(component, currentTheme, ::renderComponent)
            is ProgressStepper -> ProgressStepperMapper.map(component, currentTheme)
            is MenuBar -> MenuBarMapper.map(component, currentTheme)
            is SubMenu -> SubMenuMapper.map(component, currentTheme)
            is VerticalTabs -> VerticalTabsMapper.map(component, currentTheme, ::renderComponent)
            is MasonryGrid -> MasonryGridMapper.map(component, currentTheme, ::renderComponent)
            is FlutterAspectRatio -> AspectRatioMapper.map(component, currentTheme, ::renderComponent)

            // Flutter Parity: Data
            is DataList -> DataListMapper.map(component, currentTheme)
            is DescriptionList -> DescriptionListMapper.map(component, currentTheme)
            is StatGroup -> StatGroupMapper.map(component, currentTheme)
            is Stat -> StatMapper.map(component, currentTheme)
            is KPI -> KPIMapper.map(component, currentTheme)
            is MetricCard -> MetricCardMapper.map(component, currentTheme)
            is Leaderboard -> LeaderboardMapper.map(component, currentTheme)
            is Ranking -> RankingMapper.map(component, currentTheme)
            is com.augmentalis.avaelements.flutter.material.data.Zoom -> ZoomMapper.map(component, currentTheme)
            is VirtualScroll -> VirtualScrollMapper.map(component, currentTheme, ::renderComponent)
            is InfiniteScroll -> InfiniteScrollMapper.map(component, currentTheme, ::renderComponent)
            is com.augmentalis.avaelements.flutter.material.data.QRCode -> QRCodeMapper.map(component, currentTheme)
            is RichText -> RichTextMapper.map(component, currentTheme)

            // Flutter Parity: Input
            // Note: PhoneInput, UrlInput, ComboBox are in Phase3InputMappers
            is PinInput -> PinInputMapper.map(component, currentTheme)
            is OTPInput -> OTPInputMapper.map(component, currentTheme)
            is MaskInput -> MaskInputMapper.map(component, currentTheme)
            is RichTextEditor -> RichTextEditorMapper.map(component, currentTheme)
            is MarkdownEditor -> MarkdownEditorMapper.map(component, currentTheme)
            is CodeEditor -> CodeEditorMapper.map(component, currentTheme)
            is FormSection -> FormSectionMapper.map(component, currentTheme, ::renderComponent)
            is MultiSelect -> MultiSelectMapper.map(component, currentTheme)

            // Flutter Parity: Calendar
            is com.augmentalis.avaelements.flutter.material.data.Calendar -> CalendarMapper.map(component, currentTheme)
            is DateCalendar -> DateCalendarMapper.map(component, currentTheme)
            is MonthCalendar -> MonthCalendarMapper.map(component, currentTheme)
            is WeekCalendar -> WeekCalendarMapper.map(component, currentTheme)
            is EventCalendar -> EventCalendarMapper.map(component, currentTheme)

            // Flutter Parity: Components that DO extend Component
            is IndexedStack -> IndexedStackMapper.map(component, currentTheme, ::renderComponent)
            is EndDrawer -> EndDrawerMapper.map(component, currentTheme, ::renderAny)

            // Flutter Parity: Advanced (legacy - extend Component)
            is PopupMenuButton -> PopupMenuButtonMapper.map(component, currentTheme, ::renderComponent)
            is RichText -> RichTextMapper.map(component, currentTheme)
            is SelectableText -> SelectableTextMapper.map(component, currentTheme)
            is RefreshIndicator -> RefreshIndicatorMapper.map(component, currentTheme, ::renderComponent)

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
     * Helper to render Any type children (for Flutter layout components)
     */
    private fun renderAny(child: Any): SwiftUIView {
        return when (child) {
            is Component -> renderComponent(child)
            is SwiftUIView -> child
            // Flutter Parity: Layout components (don't extend Component)
            is AlignComponent -> AlignMapper.map(child, currentTheme, ::renderAny)
            is CenterComponent -> CenterMapper.map(child, currentTheme, ::renderAny)
            is ConstrainedBoxComponent -> ConstrainedBoxMapper.map(child, currentTheme, ::renderAny)
            is SizedBoxComponent -> SizedBoxMapper.map(child, currentTheme, ::renderAny)
            is ExpandedComponent -> ExpandedMapper.map(child, currentTheme, ::renderAny)
            is FlexibleComponent -> FlexibleMapper.map(child, currentTheme, ::renderAny)
            is FlexComponent -> FlexMapper.map(child, currentTheme, ::renderAny)
            is PaddingComponent -> PaddingMapper.map(child, currentTheme, ::renderAny)
            is FittedBoxComponent -> FittedBoxMapper.map(child, currentTheme, ::renderAny)
            is WrapComponent -> WrapMapper.map(child, currentTheme, ::renderAny)

            // Flutter Parity: Scrolling (don't extend Component)
            is ListViewBuilderComponent -> ListViewBuilderMapper.map(child, currentTheme, ::renderAny)
            is GridViewBuilderComponent -> GridViewBuilderMapper.map(child, currentTheme, ::renderAny)
            is ListViewSeparatedComponent -> ListViewSeparatedMapper.map(child, currentTheme, ::renderAny)
            is PageViewComponent -> PageViewMapper.map(child, currentTheme, ::renderAny)
            is ReorderableListViewComponent -> ReorderableListViewMapper.map(child, currentTheme, ::renderAny)
            is CustomScrollViewComponent -> CustomScrollViewMapper.map(child, currentTheme, ::renderAny)

            // Flutter Parity: Animation (don't extend Component)
            is AnimatedContainer -> AnimatedContainerMapper.map(child, currentTheme, ::renderAny)
            is AnimatedOpacity -> AnimatedOpacityMapper.map(child, currentTheme, ::renderAny)
            is AnimatedPositioned -> AnimatedPositionedMapper.map(child, currentTheme, ::renderAny)
            is AnimatedDefaultTextStyle -> AnimatedDefaultTextStyleMapper.map(child, currentTheme, ::renderAny)
            is AnimatedPadding -> AnimatedPaddingMapper.map(child, currentTheme, ::renderAny)
            is AnimatedSize -> AnimatedSizeMapper.map(child, currentTheme, ::renderAny)
            is AnimatedAlign -> AnimatedAlignMapper.map(child, currentTheme, ::renderAny)
            is AnimatedScale -> AnimatedScaleMapper.map(child, currentTheme, ::renderAny)

            // Flutter Parity: Transitions (don't extend Component)
            is FadeTransition -> FadeTransitionMapper.map(child, currentTheme, ::renderAny)
            is SlideTransition -> SlideTransitionMapper.map(child, currentTheme, ::renderAny)
            is Hero -> HeroMapper.map(child, currentTheme, ::renderAny)
            is ScaleTransition -> ScaleTransitionMapper.map(child, currentTheme, ::renderAny)
            is RotationTransition -> RotationTransitionMapper.map(child, currentTheme, ::renderAny)
            is PositionedTransition -> PositionedTransitionMapper.map(child, currentTheme, ::renderAny)
            is SizeTransition -> SizeTransitionMapper.map(child, currentTheme, ::renderAny)
            is AnimatedCrossFade -> AnimatedCrossFadeMapper.map(child, currentTheme, ::renderAny)
            is AnimatedSwitcher -> AnimatedSwitcherMapper.map(child, currentTheme, ::renderAny)
            is DecoratedBoxTransition -> DecoratedBoxTransitionMapper.map(child, currentTheme, ::renderAny)
            is AlignTransition -> AlignTransitionMapper.map(child, currentTheme, ::renderAny)

            // Flutter Parity: Slivers (don't extend Component)
            is SliverList -> SliverListMapper.map(child, currentTheme, ::renderAny)
            is SliverGrid -> SliverGridMapper.map(child, currentTheme, ::renderAny)
            is SliverFixedExtentList -> SliverFixedExtentListMapper.map(child, currentTheme, ::renderAny)
            is SliverAppBar -> SliverAppBarMapper.map(child, currentTheme, ::renderAny)

            // Flutter Parity: Other (don't extend Component)
            is FadeInImage -> FadeInImageMapper.map(child, currentTheme)
            is CircleAvatar -> CircleAvatarMapper.map(child, currentTheme)
            is com.augmentalis.avaelements.flutter.material.advanced.SelectableText -> SelectableTextMapper.map(child, currentTheme)
            is VerticalDivider -> VerticalDividerMapper.map(child, currentTheme)
            is AnimatedList -> AnimatedListMapper.map(child, currentTheme, ::renderAny)
            is AnimatedModalBarrier -> AnimatedModalBarrierMapper.map(child, currentTheme)
            is DefaultTextStyleTransition -> DefaultTextStyleTransitionMapper.map(child, currentTheme, ::renderAny)
            is RelativePositionedTransition -> RelativePositionedTransitionMapper.map(child, currentTheme, ::renderAny)

            else -> SwiftUIView(
                type = ViewType.Text,
                properties = mapOf("text" to child.toString())
            )
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
    fun getThemeElevation(elevationName: String): ShadowValueWithColor? {
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
