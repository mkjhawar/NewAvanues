package com.augmentalis.avaelements.renderers.android.performance

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Scrolling Performance Optimizer
 *
 * Optimizes ListView.builder, GridView.builder, and PageView for:
 * - 100K+ items with smooth 60 FPS scrolling
 * - Predictive prefetching
 * - Memory pooling for list items
 * - Reduced layout passes
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Predictive prefetching for LazyColumn/LazyRow
 */
class PredictivePrefetcher(
    private val state: LazyListState,
    private val prefetchDistance: Int = 3
) {
    private var lastFirstVisibleIndex = 0
    private var scrollDirection = ScrollDirection.NONE

    enum class ScrollDirection {
        NONE, UP, DOWN
    }

    /**
     * Update scroll direction and trigger prefetch
     */
    fun onScroll() {
        val currentFirst = state.firstVisibleItemIndex

        scrollDirection = when {
            currentFirst > lastFirstVisibleIndex -> ScrollDirection.DOWN
            currentFirst < lastFirstVisibleIndex -> ScrollDirection.UP
            else -> ScrollDirection.NONE
        }

        lastFirstVisibleIndex = currentFirst
    }

    /**
     * Get indices to prefetch based on scroll direction
     */
    fun getPrefetchIndices(): List<Int> {
        val layoutInfo = state.layoutInfo
        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

        return when (scrollDirection) {
            ScrollDirection.DOWN -> {
                // Prefetch items ahead
                (lastVisible + 1..lastVisible + prefetchDistance).toList()
            }
            ScrollDirection.UP -> {
                // Prefetch items behind
                val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                (maxOf(0, firstVisible - prefetchDistance) until firstVisible).toList()
            }
            ScrollDirection.NONE -> emptyList()
        }
    }
}

/**
 * Grid scrolling optimization
 */
class GridScrollOptimizer(
    private val state: LazyGridState,
    private val columnCount: Int
) {
    /**
     * Optimize grid calculations by caching span calculations
     */
    private val spanCache = mutableMapOf<Int, Int>()

    fun getSpanForIndex(index: Int): Int {
        return spanCache.getOrPut(index) {
            // Calculate optimal span based on content type
            1 // Default span, can be customized
        }
    }

    /**
     * Calculate visible row range for efficient rendering
     */
    fun getVisibleRowRange(): IntRange {
        val layoutInfo = state.layoutInfo
        val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

        val firstRow = firstVisible / columnCount
        val lastRow = lastVisible / columnCount

        return firstRow..lastRow
    }

    /**
     * Estimate total height for large grids
     */
    fun estimateTotalHeight(
        itemCount: Int,
        averageItemHeight: Dp
    ): Dp {
        val rowCount = (itemCount + columnCount - 1) / columnCount
        return averageItemHeight * rowCount
    }
}

/**
 * PageView optimization for smooth page transitions
 */
class PageViewOptimizer(
    private val state: PagerState
) {
    private var lastPage = 0
    private val pageCache = mutableMapOf<Int, Any?>()

    /**
     * Reduce page switching jank by preloading adjacent pages
     */
    fun getPreloadPages(): List<Int> {
        val currentPage = state.currentPage
        return listOf(
            maxOf(0, currentPage - 1),
            currentPage,
            minOf(state.pageCount - 1, currentPage + 1)
        )
    }

    /**
     * Cache page content to avoid re-rendering
     */
    fun <T> getCachedPage(index: Int, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return pageCache.getOrPut(index, factory) as T
    }

    /**
     * Clear cache for pages outside visible range
     */
    fun clearDistantPages() {
        val currentPage = state.currentPage
        val keysToRemove = pageCache.keys.filter { index ->
            kotlin.math.abs(index - currentPage) > 2
        }
        keysToRemove.forEach { pageCache.remove(it) }
    }

    /**
     * Detect swipe velocity to adjust animation
     */
    fun getSwipeVelocity(): Float {
        // Track page change rate
        val currentPage = state.currentPage
        val velocity = (currentPage - lastPage).toFloat()
        lastPage = currentPage
        return velocity
    }
}

/**
 * Memory pooling for list items
 */
class ListItemMemoryPool<T : Any>(
    private val maxPoolSize: Int = 30,
    private val factory: () -> T,
    private val reset: (T) -> Unit
) {
    private val pool = ArrayDeque<T>(maxPoolSize)
    private var activeCount = 0

    /**
     * Acquire item from pool or create new
     */
    fun acquire(): T {
        activeCount++
        return if (pool.isNotEmpty()) {
            pool.removeFirst()
        } else {
            factory()
        }
    }

    /**
     * Release item back to pool
     */
    fun release(item: T) {
        activeCount--
        if (pool.size < maxPoolSize) {
            reset(item)
            pool.addLast(item)
        }
    }

    /**
     * Get pool statistics
     */
    fun getStats(): PoolStats {
        return PoolStats(
            poolSize = pool.size,
            activeCount = activeCount,
            totalAllocated = pool.size + activeCount
        )
    }

    data class PoolStats(
        val poolSize: Int,
        val activeCount: Int,
        val totalAllocated: Int
    ) {
        val hitRate: Float
            get() = if (totalAllocated > 0) poolSize.toFloat() / totalAllocated else 0f
    }
}

/**
 * Scroll performance metrics tracking
 */
class ScrollPerformanceMetrics {
    private var frameCount = 0
    private var droppedFrames = 0
    private var lastScrollOffset = 0f
    private var scrollDistanceTotal = 0f
    private val frameTimes = mutableListOf<Long>()

    /**
     * Record frame during scrolling
     */
    fun recordFrame(
        scrollOffset: Float,
        frameTimeNs: Long
    ) {
        frameCount++
        frameTimes.add(frameTimeNs)

        // Track scroll distance
        scrollDistanceTotal += kotlin.math.abs(scrollOffset - lastScrollOffset)
        lastScrollOffset = scrollOffset

        // Detect dropped frames (>16.67ms = 60 FPS)
        if (frameTimeNs > 16_670_000) {
            droppedFrames++
        }

        // Keep only last 120 frames (2 seconds at 60 FPS)
        if (frameTimes.size > 120) {
            frameTimes.removeAt(0)
        }
    }

    /**
     * Get current FPS
     */
    fun getCurrentFps(): Float {
        if (frameTimes.size < 2) return 0f

        val averageFrameTime = frameTimes.average()
        return if (averageFrameTime > 0) {
            1_000_000_000f / averageFrameTime.toFloat()
        } else 0f
    }

    /**
     * Check if meeting 60 FPS target
     */
    fun isTargetFps(): Boolean = getCurrentFps() >= 58f

    /**
     * Get dropped frame percentage
     */
    fun getDroppedFrameRate(): Float {
        return if (frameCount > 0) {
            (droppedFrames.toFloat() / frameCount) * 100f
        } else 0f
    }

    /**
     * Reset metrics
     */
    fun reset() {
        frameCount = 0
        droppedFrames = 0
        lastScrollOffset = 0f
        scrollDistanceTotal = 0f
        frameTimes.clear()
    }

    /**
     * Get comprehensive report
     */
    fun getReport(): ScrollReport {
        return ScrollReport(
            fps = getCurrentFps(),
            droppedFrameRate = getDroppedFrameRate(),
            totalFrames = frameCount,
            scrollDistance = scrollDistanceTotal,
            isTargetFps = isTargetFps()
        )
    }

    data class ScrollReport(
        val fps: Float,
        val droppedFrameRate: Float,
        val totalFrames: Int,
        val scrollDistance: Float,
        val isTargetFps: Boolean
    )
}

/**
 * Composable helper for list performance optimization
 */
@Composable
fun rememberListPerformanceOptimizer(
    state: LazyListState,
    itemCount: Int
): ListPerformanceState {
    val tier = PerformanceOptimizer.detectPerformanceTier()
    val config = remember(tier) {
        PerformanceOptimizer.ScrollConfig.forTier(tier)
    }

    val prefetcher = remember(state) {
        PredictivePrefetcher(state, config.prefetchDistance)
    }

    val metrics = remember { ScrollPerformanceMetrics() }

    // Monitor scroll performance
    LaunchedEffect(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) {
        prefetcher.onScroll()
        metrics.recordFrame(
            state.firstVisibleItemScrollOffset.toFloat(),
            System.nanoTime()
        )
    }

    return remember(state, itemCount) {
        ListPerformanceState(
            prefetchIndices = prefetcher.getPrefetchIndices(),
            config = config,
            metrics = metrics,
            estimatedMemoryMb = MemoryEstimator.estimateListMemory(itemCount)
        )
    }
}

/**
 * Performance state for lists
 */
data class ListPerformanceState(
    val prefetchIndices: List<Int>,
    val config: PerformanceOptimizer.ScrollConfig,
    val metrics: ScrollPerformanceMetrics,
    val estimatedMemoryMb: Float
) {
    val isMemoryOptimal: Boolean
        get() = estimatedMemoryMb < 100f

    val isPerformanceOptimal: Boolean
        get() = metrics.isTargetFps() && isMemoryOptimal
}

/**
 * Item extent calculator for fixed-height optimization
 */
class ItemExtentCalculator {
    private val measuredSizes = mutableMapOf<Int, Float>()
    private var averageSize: Float? = null

    /**
     * Record measured item size
     */
    fun recordSize(index: Int, size: Float) {
        measuredSizes[index] = size

        // Recalculate average every 10 items
        if (measuredSizes.size % 10 == 0) {
            averageSize = measuredSizes.values.average().toFloat()
        }
    }

    /**
     * Get estimated size for unmeasured item
     */
    fun getEstimatedSize(): Float {
        return averageSize ?: 0f
    }

    /**
     * Check if items are uniform size
     */
    fun isUniformSize(threshold: Float = 0.1f): Boolean {
        if (measuredSizes.size < 5) return false

        val avg = averageSize ?: return false
        val variance = measuredSizes.values.map { size ->
            kotlin.math.abs(size - avg) / avg
        }.average()

        return variance < threshold
    }
}

/**
 * Scroll position restoration for configuration changes
 */
class ScrollPositionRestorer {
    private var savedPosition: SavedPosition? = null

    data class SavedPosition(
        val index: Int,
        val offset: Int
    )

    /**
     * Save current scroll position
     */
    fun savePosition(state: LazyListState) {
        savedPosition = SavedPosition(
            index = state.firstVisibleItemIndex,
            offset = state.firstVisibleItemScrollOffset
        )
    }

    /**
     * Restore scroll position
     */
    suspend fun restorePosition(state: LazyListState) {
        savedPosition?.let { pos ->
            state.scrollToItem(pos.index, pos.offset)
        }
    }

    /**
     * Clear saved position
     */
    fun clear() {
        savedPosition = null
    }
}
