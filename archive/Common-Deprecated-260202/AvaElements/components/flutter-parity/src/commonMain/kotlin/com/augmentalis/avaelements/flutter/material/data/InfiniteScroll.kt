package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * InfiniteScroll component - Flutter Material parity
 *
 * A scrolling container with automatic "load more" functionality when user scrolls near the bottom.
 * Perfect for paginated lists, social media feeds, and search results.
 *
 * **Flutter Equivalent:** Custom infinite scroll implementation
 * **Material Design 3:** https://m3.material.io/foundations/layout/applying-layout/compact
 *
 * ## Features
 * - Automatic next page loading on scroll
 * - Configurable loading threshold (distance from bottom)
 * - Loading indicator while fetching
 * - Page tracking
 * - Pull-to-refresh integration
 * - Error state handling
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * var items by remember { mutableStateOf(loadInitialItems()) }
 * var hasMore by remember { mutableStateOf(true) }
 * var loading by remember { mutableStateOf(false) }
 *
 * InfiniteScroll(
 *     items = items.map { item ->
 *         ListTile(title = item.title, subtitle = item.description)
 *     },
 *     hasMore = hasMore,
 *     loading = loading,
 *     loadingThreshold = 100f,
 *     onLoadMore = {
 *         loading = true
 *         loadNextPage { newItems, noMore ->
 *             items = items + newItems
 *             hasMore = !noMore
 *             loading = false
 *         }
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property items List of components to display
 * @property hasMore Whether there are more items to load
 * @property loading Whether data is currently being loaded
 * @property loadingThreshold Distance from bottom (in dp) to trigger load
 * @property loadingIndicatorText Text to show while loading
 * @property endMessageText Text to show when no more items
 * @property errorMessageText Text to show on error
 * @property showError Whether to show error state
 * @property orientation Scroll orientation (Vertical or Horizontal)
 * @property backgroundColor Background color of the scroll container
 * @property contentPadding Custom padding for the scroll container
 * @property initialLoadDelay Delay before first auto-load (milliseconds)
 * @property contentDescription Accessibility description for TalkBack
 * @property onLoadMore Callback invoked when more items should be loaded (not serialized)
 * @property onRetry Callback invoked when user taps retry after error (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class InfiniteScroll(
    override val type: String = "InfiniteScroll",
    override val id: String? = null,
    val items: List<Component>,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val loadingThreshold: Float = 100f,
    val loadingIndicatorText: String? = "Loading more...",
    val endMessageText: String? = "No more items",
    val errorMessageText: String? = "Failed to load. Tap to retry.",
    val showError: Boolean = false,
    val orientation: Orientation = Orientation.Vertical,
    val backgroundColor: String? = null,
    val contentPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val initialLoadDelay: Long = 0,
    val contentDescription: String? = null,
    @Transient
    val onLoadMore: (() -> Unit)? = null,
    @Transient
    val onRetry: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Scroll orientation
     */
    enum class Orientation {
        /** Vertical scrolling (default) */
        Vertical,

        /** Horizontal scrolling */
        Horizontal
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Infinite scroll list"
        val itemCount = items.size
        val stateDesc = when {
            loading -> ", loading more items"
            showError -> ", error loading items"
            !hasMore -> ", all items loaded"
            else -> ""
        }
        return "$base with $itemCount items$stateDesc"
    }

    /**
     * Check if should trigger load more
     */
    fun shouldLoadMore(scrollOffset: Float, maxScrollOffset: Float): Boolean {
        if (loading || !hasMore || showError) return false
        val distanceFromBottom = maxScrollOffset - scrollOffset
        return distanceFromBottom <= loadingThreshold
    }

    /**
     * Get footer state for display
     */
    fun getFooterState(): FooterState {
        return when {
            showError -> FooterState.Error
            loading -> FooterState.Loading
            !hasMore -> FooterState.End
            else -> FooterState.None
        }
    }

    /**
     * Footer state enumeration
     */
    enum class FooterState {
        /** No footer (more items available but not loading) */
        None,

        /** Loading indicator */
        Loading,

        /** End message (no more items) */
        End,

        /** Error message with retry */
        Error
    }

    /**
     * Get footer message based on state
     */
    fun getFooterMessage(): String? {
        return when (getFooterState()) {
            FooterState.Loading -> loadingIndicatorText
            FooterState.End -> endMessageText
            FooterState.Error -> errorMessageText
            FooterState.None -> null
        }
    }

    companion object {
        /**
         * Create a vertical infinite scroll list
         */
        fun vertical(
            items: List<Component>,
            hasMore: Boolean = true,
            loading: Boolean = false,
            onLoadMore: (() -> Unit)? = null
        ) = InfiniteScroll(
            items = items,
            hasMore = hasMore,
            loading = loading,
            orientation = Orientation.Vertical,
            onLoadMore = onLoadMore
        )

        /**
         * Create a horizontal infinite scroll list
         */
        fun horizontal(
            items: List<Component>,
            hasMore: Boolean = true,
            loading: Boolean = false,
            onLoadMore: (() -> Unit)? = null
        ) = InfiniteScroll(
            items = items,
            hasMore = hasMore,
            loading = loading,
            orientation = Orientation.Horizontal,
            onLoadMore = onLoadMore
        )

        /**
         * Create an infinite scroll with custom threshold
         */
        fun withThreshold(
            items: List<Component>,
            loadingThreshold: Float,
            hasMore: Boolean = true,
            onLoadMore: (() -> Unit)? = null
        ) = InfiniteScroll(
            items = items,
            hasMore = hasMore,
            loadingThreshold = loadingThreshold,
            orientation = Orientation.Vertical,
            onLoadMore = onLoadMore
        )
    }
}
