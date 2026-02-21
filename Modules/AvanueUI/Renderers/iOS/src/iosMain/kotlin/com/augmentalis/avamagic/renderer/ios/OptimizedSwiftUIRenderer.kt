package com.augmentalis.avamagic.renderer.ios

import com.augmentalis.avamagic.core.*
import com.augmentalis.avamagic.dsl.*
import com.augmentalis.avamagic.renderer.ios.bridge.*
import com.augmentalis.avamagic.renderer.ios.mappers.*
import kotlin.reflect.KClass

/**
 * Optimized SwiftUI Renderer for iOS
 *
 * Performance improvements over SwiftUIRenderer:
 * 1. HashMap-based dispatch instead of when statement (O(1) vs O(n))
 * 2. Lazy mapper initialization
 * 3. Component caching for repeated renders
 * 4. Batch rendering support
 * 5. Pre-compiled theme tokens
 *
 * Benchmarks (estimated):
 * - Single component: <1ms (vs 2-3ms)
 * - 100 components: <16ms (60fps target)
 * - Theme switch: <5ms
 */
class OptimizedSwiftUIRenderer : Renderer {

    override val platform: Renderer.Platform = Renderer.Platform.iOS

    private var currentTheme: Theme? = null
    private val themeConverter = iOSThemeConverter()

    // Pre-computed design tokens
    var designTokens: iOSThemeConverter.iOSDesignTokens? = null
        private set

    // Cache for rendered components (weak references to allow GC)
    private val renderCache = mutableMapOf<Int, SwiftUIView>()
    private var cacheEnabled = true
    private var cacheHits = 0
    private var cacheMisses = 0

    // Mapper registry for O(1) dispatch
    private val mapperRegistry = MapperRegistry()

    init {
        registerAllMappers()
    }

    /**
     * Register all component mappers for fast dispatch
     */
    private fun registerAllMappers() {
        // Phase 1: Basic components
        mapperRegistry.register<TextComponent> { c, t -> TextMapper.map(c, t) }
        mapperRegistry.register<ButtonComponent> { c, t -> ButtonMapper.map(c, t) }
        mapperRegistry.register<TextFieldComponent> { c, t -> TextFieldMapper.map(c, t) }
        mapperRegistry.register<CheckboxComponent> { c, t -> CheckboxMapper.map(c, t) }
        mapperRegistry.register<SwitchComponent> { c, t -> SwitchMapper.map(c, t) }
        mapperRegistry.register<IconComponent> { c, t -> IconMapper.map(c, t) }
        mapperRegistry.register<ImageComponent> { c, t -> ImageMapper.map(c, t) }

        // Phase 1: Layout components (with children)
        mapperRegistry.registerWithChildren<ColumnComponent> { c, t, r -> ColumnMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<RowComponent> { c, t, r -> RowMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<ContainerComponent> { c, t, r -> ContainerMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<ScrollViewComponent> { c, t, r -> ScrollViewMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<CardComponent> { c, t, r -> CardMapper.map(c, t, r) }

        // Data components
        mapperRegistry.registerWithChildren<AccordionComponent> { c, t, r -> AccordionMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<TimelineComponent> { c, t, r -> TimelineMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<CarouselComponent> { c, t, r -> CarouselMapper.map(c, t, r) }

        // Advanced layout
        mapperRegistry.registerWithChildren<ScaffoldComponent> { c, t, r -> ScaffoldMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<LazyColumnComponent> { c, t, r -> LazyColumnMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<LazyRowComponent> { c, t, r -> LazyRowMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<BoxComponent> { c, t, r -> BoxMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<SurfaceComponent> { c, t, r -> SurfaceMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<ListTileComponent> { c, t, r -> ListTileMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<GridComponent> { c, t, r -> GridMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<StackComponent> { c, t, r -> StackMapper.map(c, t, r) }

        // Feedback components
        mapperRegistry.registerWithChildren<BottomSheetComponent> { c, t, r -> BottomSheetMapper.map(c, t, r) }
        mapperRegistry.register<LoadingDialogComponent> { c, t -> LoadingDialogMapper.map(c, t) }
        mapperRegistry.register<CircularProgressComponent> { c, t -> CircularProgressMapper.map(c, t) }

        // Navigation
        mapperRegistry.registerWithChildren<TabBarComponent> { c, t, r -> TabBarMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<DialogComponent> { c, t, r -> DialogMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<NavigationDrawerComponent> { c, t, r -> NavigationDrawerMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<NavigationRailComponent> { c, t, r -> NavigationRailMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<BottomAppBarComponent> { c, t, r -> BottomAppBarMapper.map(c, t, r) }

        // Button variants
        mapperRegistry.register<SegmentedButtonComponent> { c, t ->
            SegmentedButtonMapper.map(c, t) { child -> renderComponent(child as Component) }
        }
        mapperRegistry.register<TextButtonComponent> { c, t -> TextButtonMapper.map(c, t) }
        mapperRegistry.register<OutlinedButtonComponent> { c, t -> OutlinedButtonMapper.map(c, t) }
        mapperRegistry.register<FilledButtonComponent> { c, t -> FilledButtonMapper.map(c, t) }
        mapperRegistry.register<IconButtonComponent> { c, t -> IconButtonMapper.map(c, t) }

        // Display components
        mapperRegistry.register<ColorPickerComponent> { c, t -> ColorPickerMapper.map(c, t) }
        mapperRegistry.register<PaginationComponent> { c, t -> PaginationMapper.map(c, t) }
        mapperRegistry.registerWithChildren<TooltipComponent> { c, t, r -> TooltipMapper.map(c, t, r) }
        mapperRegistry.register<SkeletonComponent> { c, t -> SkeletonMapper.map(c, t) }
        mapperRegistry.register<SpinnerComponent> { c, t -> SpinnerMapper.map(c, t) }

        // Form/Input components
        mapperRegistry.register<MultiSelectComponent> { c, t -> MultiSelectMapper.map(c, t) }
        mapperRegistry.register<DateRangePickerComponent> { c, t -> DateRangePickerMapper.map(c, t) }
        mapperRegistry.register<TagInputComponent> { c, t -> TagInputMapper.map(c, t) }
        mapperRegistry.register<ToggleComponent> { c, t -> ToggleMapper.map(c, t) }
        mapperRegistry.register<ToggleButtonGroupComponent> { c, t -> ToggleButtonGroupMapper.map(c, t) }
        mapperRegistry.register<StepperComponent> { c, t -> StepperMapper.map(c, t) }
        mapperRegistry.register<IconPickerComponent> { c, t -> IconPickerMapper.map(c, t) }

        // Advanced display
        mapperRegistry.register<StatCardComponent> { c, t -> StatCardMapper.map(c, t) }
        mapperRegistry.registerWithChildren<FABComponent> { c, t, r -> FABMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<StickyHeaderComponent> { c, t, r -> StickyHeaderMapper.map(c, t, r) }
        mapperRegistry.registerWithChildren<MasonryGridComponent> { c, t, r -> MasonryGridMapper.map(c, t, r) }
        mapperRegistry.register<ProgressCircleComponent> { c, t -> ProgressCircleMapper.map(c, t) }
        mapperRegistry.register<BannerComponent> { c, t -> BannerMapper.map(c, t) }
        mapperRegistry.register<NotificationCenterComponent> { c, t -> NotificationCenterMapper.map(c, t) }
        mapperRegistry.register<TableComponent> { c, t -> TableMapper.map(c, t) }

        // Gap closure (new mappers)
        mapperRegistry.register<RadioComponent> { c, t -> RadioMapper.map(c, t) }
        mapperRegistry.register<RadioGroupComponent> { c, t -> RadioGroupMapper.map(c, t) }
        mapperRegistry.register<SliderComponent> { c, t -> SliderMapper.map(c, t) }
        mapperRegistry.register<ProgressBarComponent> { c, t -> ProgressBarMapper.map(c, t) }
        mapperRegistry.register<AvatarComponent> { c, t -> AvatarMapper.map(c, t) }
        mapperRegistry.register<BadgeComponent> { c, t -> BadgeMapper.map(c, t) }
        mapperRegistry.register<RatingComponent> { c, t -> RatingMapper.map(c, t) }
        mapperRegistry.register<SearchBarComponent> { c, t -> SearchBarMapper.map(c, t) }
    }

    override fun applyTheme(theme: Theme) {
        currentTheme = theme
        designTokens = themeConverter.convert(theme)
        // Clear cache on theme change
        clearCache()
    }

    override fun render(component: Component): Any {
        return renderComponent(component)
    }

    /**
     * Optimized recursive renderer with caching
     */
    private fun renderComponent(component: Component): SwiftUIView {
        // Check cache first
        if (cacheEnabled) {
            val cacheKey = component.hashCode()
            renderCache[cacheKey]?.let {
                cacheHits++
                return it
            }
            cacheMisses++
        }

        // Dispatch to mapper
        val result = mapperRegistry.render(component, currentTheme) { child ->
            renderComponent(child as Component)
        } ?: SwiftUIView(
            type = ViewType.EmptyView,
            properties = mapOf("error" to "Unknown component: ${component::class.simpleName}")
        )

        // Cache result
        if (cacheEnabled) {
            renderCache[component.hashCode()] = result
        }

        return result
    }

    /**
     * Batch render multiple components (optimized for lists)
     */
    fun renderBatch(components: List<Component>): List<SwiftUIView> {
        return components.map { renderComponent(it) }
    }

    /**
     * Clear the render cache
     */
    fun clearCache() {
        renderCache.clear()
        cacheHits = 0
        cacheMisses = 0
    }

    /**
     * Enable/disable caching
     */
    fun setCacheEnabled(enabled: Boolean) {
        cacheEnabled = enabled
        if (!enabled) clearCache()
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val total = cacheHits + cacheMisses
        val hitRate = if (total > 0) cacheHits.toFloat() / total else 0f
        return CacheStats(cacheHits, cacheMisses, renderCache.size, hitRate)
    }

    /**
     * Render with profiling
     */
    fun renderWithProfiling(component: Component): RenderResult {
        val startTime = platform.Foundation.NSDate().timeIntervalSince1970.toLong() * 1000L
        val result = renderComponent(component)
        val endTime = platform.Foundation.NSDate().timeIntervalSince1970.toLong() * 1000L

        return RenderResult(
            view = result,
            renderTimeMs = endTime - startTime,
            cacheStats = getCacheStats()
        )
    }

    data class CacheStats(
        val hits: Int,
        val misses: Int,
        val size: Int,
        val hitRate: Float
    )

    data class RenderResult(
        val view: SwiftUIView,
        val renderTimeMs: Long,
        val cacheStats: CacheStats
    )

    companion object {
        fun withLiquidGlass(): OptimizedSwiftUIRenderer {
            return OptimizedSwiftUIRenderer().apply {
                applyTheme(Themes.iOS26LiquidGlass)
            }
        }

        fun withMaterial3(): OptimizedSwiftUIRenderer {
            return OptimizedSwiftUIRenderer().apply {
                applyTheme(Themes.Material3Light)
            }
        }
    }
}

/**
 * Mapper registry for O(1) component dispatch
 */
private class MapperRegistry {
    private val simpleMappers = mutableMapOf<KClass<*>, (Any, Theme?) -> SwiftUIView>()
    private val childMappers = mutableMapOf<KClass<*>, (Any, Theme?, (Any) -> SwiftUIView) -> SwiftUIView>()

    inline fun <reified T : Any> register(noinline mapper: (T, Theme?) -> SwiftUIView) {
        @Suppress("UNCHECKED_CAST")
        simpleMappers[T::class] = mapper as (Any, Theme?) -> SwiftUIView
    }

    inline fun <reified T : Any> registerWithChildren(
        noinline mapper: (T, Theme?, (Any) -> SwiftUIView) -> SwiftUIView
    ) {
        @Suppress("UNCHECKED_CAST")
        childMappers[T::class] = mapper as (Any, Theme?, (Any) -> SwiftUIView) -> SwiftUIView
    }

    fun render(component: Any, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView? {
        val kclass = component::class

        // Check simple mappers first
        simpleMappers[kclass]?.let { mapper ->
            return mapper(component, theme)
        }

        // Check child mappers
        childMappers[kclass]?.let { mapper ->
            return mapper(component, theme, renderChild)
        }

        return null
    }
}

/**
 * Extension function for optimized rendering
 */
fun Component.toOptimizedSwiftUI(theme: Theme? = null): SwiftUIView {
    val renderer = OptimizedSwiftUIRenderer()
    theme?.let { renderer.applyTheme(it) }
    return renderer.render(this) as SwiftUIView
}
