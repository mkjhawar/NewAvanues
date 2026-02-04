@file:Suppress("unused")

package com.augmentalis.avaelements.renderer.android.mappers

/**
 * Material Mappers - Barrel/Index File
 *
 * This file serves as a central re-export hub for all Material component mappers,
 * providing backward compatibility for existing code that imports from MaterialMappers.
 *
 * The mappers have been refactored into category-specific files for better organization:
 *
 * - **ChipMappers.kt** - FilterChipMapper, ActionChipMapper, ChoiceChipMapper, InputChipMapper
 * - **ListMappers.kt** - ExpansionTileMapper, CheckboxListTileMapper, SwitchListTileMapper, RadioListTileMapper
 * - **CardMappers.kt** - PricingCardMapper, FeatureCardMapper, TestimonialCardMapper, ProductCardMapper,
 *                        ArticleCardMapper, ImageCardMapper, HoverCardMapper, ExpandableCardMapper, MetricCardMapper
 * - **NavigationMappers.kt** - MenuMapper, SidebarMapper, NavLinkMapper, ProgressStepperMapper,
 *                              MenuBarMapper, SubMenuMapper, VerticalTabsMapper
 * - **FeedbackMappers.kt** - PopupMapper, CalloutMapper, DisclosureMapper, InfoPanelMapper,
 *                           ErrorPanelMapper, WarningPanelMapper, SuccessPanelMapper, FullPageLoadingMapper,
 *                           AnimatedCheckMapper, AnimatedErrorMapper, AnimatedSuccessMapper, AnimatedWarningMapper,
 *                           RefreshIndicatorMapper, LoadingOverlayMapper, SkeletonTextMapper, SkeletonCircleMapper,
 *                           ProgressCircleMapper, HoverCardMapper (for feedback hover cards)
 * - **DisplayMappers.kt** - PopoverMapper, ErrorStateMapper, NoDataMapper, ImageCarouselMapper,
 *                          LazyImageMapper, ImageGalleryMapper, LightboxMapper
 * - **InputMappers.kt** - PhoneInputMapper, UrlInputMapper, ComboBoxMapper, PinInputMapper,
 *                        OTPInputMapper, MaskInputMapper, RichTextEditorMapper, MarkdownEditorMapper,
 *                        CodeEditorMapper, FormSectionMapper, MultiSelectMapper
 * - **DataMappers.kt** - DataListMapper, DescriptionListMapper, StatGroupMapper, StatMapper,
 *                       KPIMapper, LeaderboardMapper, RankingMapper, VirtualScrollMapper, InfiniteScrollMapper
 *
 * @since 3.0.0-flutter-parity
 * @since 3.3.0-refactored - Split into category-specific files for SOLID compliance
 */

// ============================================================================
// RE-EXPORTS FROM CHIP MAPPERS
// ============================================================================
// All chip-related mappers are in ChipMappers.kt
// Functions: FilterChipMapper, ActionChipMapper, ChoiceChipMapper, InputChipMapper

// ============================================================================
// RE-EXPORTS FROM LIST MAPPERS
// ============================================================================
// All list tile mappers are in ListMappers.kt
// Functions: ExpansionTileMapper, CheckboxListTileMapper, SwitchListTileMapper, RadioListTileMapper

// ============================================================================
// RE-EXPORTS FROM CARD MAPPERS
// ============================================================================
// All card mappers are in CardMappers.kt
// Functions: PricingCardMapper, FeatureCardMapper, TestimonialCardMapper, ProductCardMapper,
//            ArticleCardMapper, ImageCardMapper, HoverCardMapper, ExpandableCardMapper, MetricCardMapper

// ============================================================================
// RE-EXPORTS FROM NAVIGATION MAPPERS
// ============================================================================
// All navigation mappers are in NavigationMappers.kt
// Functions: MenuMapper, SidebarMapper, NavLinkMapper, ProgressStepperMapper,
//            MenuBarMapper, SubMenuMapper, VerticalTabsMapper

// ============================================================================
// RE-EXPORTS FROM FEEDBACK MAPPERS
// ============================================================================
// All feedback/loading mappers are in FeedbackMappers.kt
// Functions: PopupMapper, CalloutMapper, DisclosureMapper, InfoPanelMapper,
//            ErrorPanelMapper, WarningPanelMapper, SuccessPanelMapper, FullPageLoadingMapper,
//            AnimatedCheckMapper, AnimatedErrorMapper, AnimatedSuccessMapper, AnimatedWarningMapper

// ============================================================================
// RE-EXPORTS FROM DISPLAY MAPPERS
// ============================================================================
// All display mappers are in DisplayMappers.kt
// Functions: PopoverMapper, ErrorStateMapper, NoDataMapper, ImageCarouselMapper,
//            LazyImageMapper, ImageGalleryMapper, LightboxMapper

// ============================================================================
// RE-EXPORTS FROM INPUT MAPPERS
// ============================================================================
// All input mappers are in InputMappers.kt
// Functions: PhoneInputMapper, UrlInputMapper, ComboBoxMapper, PinInputMapper,
//            OTPInputMapper, MaskInputMapper, RichTextEditorMapper, MarkdownEditorMapper,
//            CodeEditorMapper, FormSectionMapper, MultiSelectMapper

// ============================================================================
// RE-EXPORTS FROM DATA MAPPERS
// ============================================================================
// All data display mappers are in DataMappers.kt
// Functions: DataListMapper, DescriptionListMapper, StatGroupMapper, StatMapper,
//            KPIMapper, LeaderboardMapper, RankingMapper, VirtualScrollMapper, InfiniteScrollMapper

// ============================================================================
// BACKWARD COMPATIBILITY NOTICE
// ============================================================================
/**
 * For backward compatibility, you can import all mappers from this package:
 *
 * ```kotlin
 * import com.augmentalis.avaelements.renderer.android.mappers.*
 * ```
 *
 * This will import all mapper functions from all category files.
 *
 * Alternatively, you can import from specific category files:
 *
 * ```kotlin
 * // Import only chip mappers
 * import com.augmentalis.avaelements.renderer.android.mappers.FilterChipMapper
 * import com.augmentalis.avaelements.renderer.android.mappers.ActionChipMapper
 *
 * // Import only navigation mappers
 * import com.augmentalis.avaelements.renderer.android.mappers.MenuMapper
 * import com.augmentalis.avaelements.renderer.android.mappers.SidebarMapper
 * ```
 */

// ============================================================================
// MAPPER FUNCTION INDEX
// ============================================================================
/**
 * Complete index of all available mapper functions by category:
 *
 * ## Chips (ChipMappers.kt)
 * - `FilterChipMapper(component: FilterChip)` - Filterable chip with selection state
 * - `ActionChipMapper(component: ActionChip)` - Actionable assist chip
 * - `ChoiceChipMapper(component: ChoiceChip)` - Single-selection choice chip
 * - `InputChipMapper(component: InputChip)` - Input chip with delete action
 *
 * ## Lists (ListMappers.kt)
 * - `ExpansionTileMapper(component: ExpansionTile)` - Expandable list tile
 * - `CheckboxListTileMapper(component: CheckboxListTile)` - List tile with checkbox
 * - `SwitchListTileMapper(component: SwitchListTile)` - List tile with switch
 * - `RadioListTileMapper(component: RadioListTile)` - List tile with radio button
 *
 * ## Cards (CardMappers.kt)
 * - `PricingCardMapper(component: PricingCard)` - Pricing tier card
 * - `FeatureCardMapper(component: FeatureCard)` - Feature highlight card
 * - `TestimonialCardMapper(component: TestimonialCard)` - Customer testimonial card
 * - `ProductCardMapper(component: ProductCard)` - E-commerce product card
 * - `ArticleCardMapper(component: ArticleCard)` - Blog/article card
 * - `ImageCardMapper(component: ImageCard)` - Image with overlay card
 * - `HoverCardMapper(component: HoverCard)` - Hover information card
 * - `ExpandableCardMapper(component: ExpandableCard)` - Expandable content card
 * - `MetricCardMapper(component: MetricCard)` - Metric display card
 *
 * ## Navigation (NavigationMappers.kt)
 * - `MenuMapper(component: Menu)` - Menu with nested items
 * - `SidebarMapper(component: Sidebar)` - Navigation sidebar
 * - `NavLinkMapper(component: NavLink)` - Navigation link item
 * - `ProgressStepperMapper(component: ProgressStepper)` - Step progress indicator
 * - `MenuBarMapper(component: MenuBar)` - Top menu bar
 * - `SubMenuMapper(component: SubMenu)` - Nested submenu
 * - `VerticalTabsMapper(component: VerticalTabs)` - Vertical tab navigation
 *
 * ## Feedback (FeedbackMappers.kt)
 * - `PopupMapper(component: Popup)` - Floating popup
 * - `CalloutMapper(component: Callout)` - Banner callout
 * - `DisclosureMapper(component: Disclosure)` - Expandable disclosure
 * - `InfoPanelMapper(component: InfoPanel)` - Information panel (blue)
 * - `ErrorPanelMapper(component: ErrorPanel)` - Error panel (red)
 * - `WarningPanelMapper(component: WarningPanel)` - Warning panel (amber)
 * - `SuccessPanelMapper(component: SuccessPanel)` - Success panel (green)
 * - `FullPageLoadingMapper(component: FullPageLoading)` - Full-page loading overlay
 * - `AnimatedCheckMapper(component: AnimatedCheck)` - Animated checkmark
 * - `AnimatedErrorMapper(component: AnimatedError)` - Animated error icon
 * - `AnimatedSuccessMapper(component: AnimatedSuccess)` - Animated success icon
 * - `AnimatedWarningMapper(component: AnimatedWarning)` - Animated warning icon
 *
 * ## Display (DisplayMappers.kt)
 * - `PopoverMapper(component: Popover)` - Contextual popover
 * - `ErrorStateMapper(component: ErrorState)` - Error state display
 * - `NoDataMapper(component: NoData)` - Empty/no data state
 * - `ImageCarouselMapper(component: ImageCarousel)` - Image carousel/slider
 * - `LazyImageMapper(component: LazyImage)` - Lazy-loaded image
 * - `ImageGalleryMapper(component: ImageGallery)` - Image gallery grid
 * - `LightboxMapper(component: Lightbox)` - Fullscreen lightbox
 *
 * ## Input (InputMappers.kt)
 * - `PhoneInputMapper(component: PhoneInput)` - Phone number input with country code
 * - `UrlInputMapper(component: UrlInput)` - URL input with validation
 * - `ComboBoxMapper(component: ComboBox)` - Searchable combo box
 * - `PinInputMapper(component: PinInput)` - PIN code input
 * - `OTPInputMapper(component: OTPInput)` - OTP verification input
 * - `MaskInputMapper(component: MaskInput)` - Masked text input
 * - `RichTextEditorMapper(component: RichTextEditor)` - Rich text editor
 * - `MarkdownEditorMapper(component: MarkdownEditor)` - Markdown editor
 * - `CodeEditorMapper(component: CodeEditor)` - Code editor with line numbers
 * - `FormSectionMapper(component: FormSection)` - Collapsible form section
 * - `MultiSelectMapper(component: MultiSelect)` - Multi-selection dropdown
 *
 * ## Data (DataMappers.kt)
 * - `DataListMapper(component: DataList)` - Structured data list
 * - `DescriptionListMapper(component: DescriptionList)` - Description list
 * - `StatGroupMapper(component: StatGroup)` - Statistics group
 * - `StatMapper(component: Stat)` - Single statistic display
 * - `KPIMapper(component: KPI)` - KPI indicator
 * - `LeaderboardMapper(component: Leaderboard)` - Leaderboard display
 * - `RankingMapper(component: Ranking)` - Ranking badge
 * - `VirtualScrollMapper(component: VirtualScroll)` - Virtual scrolling list
 * - `InfiniteScrollMapper(component: InfiniteScroll)` - Infinite scroll list
 */
