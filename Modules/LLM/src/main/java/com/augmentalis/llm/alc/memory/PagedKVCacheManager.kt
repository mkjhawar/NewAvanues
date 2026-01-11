/**
 * Paged KV Cache Manager
 *
 * Single Responsibility: Manage KV cache with paging for extended context
 *
 * Implements paged attention mechanism similar to vLLM's PagedAttention:
 * - Divides KV cache into fixed-size pages (typically 16 tokens)
 * - Enables efficient memory management for long conversations
 * - Supports cache eviction with LRU policy
 * - Allows dynamic memory allocation without fragmentation
 *
 * Benefits over simple KV cache:
 * - Up to 24x more memory efficient for long contexts
 * - No need to pre-allocate maximum context length
 * - Smooth handling of variable-length sequences
 * - Graceful degradation when memory is constrained
 *
 * Created: 2025-12-03
 * Author: AVA AI Team
 */

package com.augmentalis.llm.alc.memory

import com.augmentalis.llm.alc.interfaces.IMemoryManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Page metadata for KV cache
 *
 * @param pageId Unique identifier for this page
 * @param sequenceId Which sequence/conversation this belongs to
 * @param tokenStart Starting token index in the sequence
 * @param tokenCount Number of tokens stored in this page
 * @param lastAccessTime Timestamp of last access (for LRU eviction)
 */
data class CachePage(
    val pageId: Int,
    val sequenceId: Long,
    val tokenStart: Int,
    var tokenCount: Int,
    var lastAccessTime: Long = System.currentTimeMillis()
) {
    var keyData: ByteBuffer? = null
    var valueData: ByteBuffer? = null

    val isFull: Boolean get() = tokenCount >= PAGE_SIZE

    companion object {
        const val PAGE_SIZE = 16  // Tokens per page (matches vLLM default)
    }

    fun sizeBytes(): Long {
        val keySize = keyData?.capacity()?.toLong() ?: 0L
        val valSize = valueData?.capacity()?.toLong() ?: 0L
        return keySize + valSize
    }
}

/**
 * Paged KV Cache Manager implementation
 *
 * Uses paged attention similar to vLLM for efficient memory management.
 * Each page holds 16 tokens worth of K/V tensors.
 *
 * @param memoryBudgetBytes Maximum memory for KV cache (default 512 MB)
 * @param numLayers Number of transformer layers
 * @param numHeads Number of attention heads
 * @param headDim Dimension of each attention head
 * @param dtype Data type size (2 for FP16, 4 for FP32)
 */
class PagedKVCacheManager(
    private val memoryBudgetBytes: Long = 512 * 1024 * 1024, // 512 MB default
    private val numLayers: Int = 32,
    private val numHeads: Int = 32,
    private val headDim: Int = 128,
    private val dtype: Int = 2  // FP16
) : IMemoryManager {

    companion object {
        private const val TAG = "PagedKVCache"
        private const val PAGE_SIZE = CachePage.PAGE_SIZE
    }

    // Page pool and allocation tracking
    private val pageIdCounter = AtomicInteger(0)
    private val allocatedPages = mutableMapOf<Int, CachePage>()
    private val freePages = ConcurrentLinkedDeque<CachePage>()

    // Sequence to pages mapping
    private val sequencePages = mutableMapOf<Long, MutableList<CachePage>>()

    // Memory tracking
    private val currentUsage = AtomicLong(0)
    private val mutex = Mutex()

    // Statistics
    private var cacheHits = 0L
    private var cacheMisses = 0L
    private var pagesAllocated = 0
    private var pagesEvicted = 0

    /**
     * Calculate bytes per page
     * K and V each have shape: [numLayers, numHeads, pageSize, headDim]
     */
    private val bytesPerPage: Long = run {
        val tensorElements = numLayers.toLong() * numHeads * PAGE_SIZE * headDim
        val kvPairSize = tensorElements * dtype * 2  // Both K and V
        kvPairSize
    }

    /**
     * Maximum pages that can fit in budget
     */
    private val maxPages: Int = (memoryBudgetBytes / bytesPerPage).toInt()

    init {
        Timber.i("$TAG: Initialized with budget=${memoryBudgetBytes / 1024 / 1024}MB, " +
                 "bytesPerPage=${bytesPerPage / 1024}KB, maxPages=$maxPages")
    }

    /**
     * Allocate pages for a new sequence
     *
     * @param sequenceId Unique ID for this conversation/sequence
     * @param numTokens Number of tokens to allocate space for
     * @return List of allocated pages, or null if insufficient memory
     */
    suspend fun allocatePages(sequenceId: Long, numTokens: Int): List<CachePage>? = mutex.withLock {
        val pagesNeeded = (numTokens + PAGE_SIZE - 1) / PAGE_SIZE

        if (!canAllocate(pagesNeeded)) {
            Timber.w("$TAG: Cannot allocate $pagesNeeded pages, attempting eviction")
            evictLRU(pagesNeeded)

            if (!canAllocate(pagesNeeded)) {
                Timber.e("$TAG: Still cannot allocate after eviction")
                return@withLock null
            }
        }

        val pages = mutableListOf<CachePage>()
        var remainingTokens = numTokens
        var currentTokenStart = 0

        repeat(pagesNeeded) {
            val page = getFreePage() ?: createNewPage()
            page.apply {
                // Re-assign to new sequence
                sequencePages.getOrPut(sequenceId) { mutableListOf() }.add(this)
                tokenCount = minOf(PAGE_SIZE, remainingTokens)
                lastAccessTime = System.currentTimeMillis()
            }

            // Allocate buffers if not already allocated
            if (page.keyData == null) {
                val pageBytes = bytesPerPage.toInt() / 2  // Half for K, half for V
                page.keyData = ByteBuffer.allocateDirect(pageBytes)
                page.valueData = ByteBuffer.allocateDirect(pageBytes)
                currentUsage.addAndGet(bytesPerPage)
            }

            pages.add(page)
            remainingTokens -= PAGE_SIZE
            currentTokenStart += PAGE_SIZE
        }

        pagesAllocated += pages.size
        Timber.d("$TAG: Allocated ${pages.size} pages for sequence $sequenceId")

        pages
    }

    /**
     * Get pages for an existing sequence
     */
    suspend fun getSequencePages(sequenceId: Long): List<CachePage>? = mutex.withLock {
        val pages = sequencePages[sequenceId]
        if (pages != null) {
            cacheHits++
            // Update access time for LRU
            val now = System.currentTimeMillis()
            pages.forEach { it.lastAccessTime = now }
        } else {
            cacheMisses++
        }
        pages
    }

    /**
     * Append tokens to a sequence's cache
     *
     * @param sequenceId Sequence to append to
     * @param tokenCount Number of new tokens
     * @return true if successful
     */
    suspend fun appendTokens(sequenceId: Long, tokenCount: Int): Boolean = mutex.withLock {
        val pages = sequencePages[sequenceId] ?: return@withLock false

        var tokensRemaining = tokenCount

        // Fill existing partial page first
        val lastPage = pages.lastOrNull()
        if (lastPage != null && !lastPage.isFull) {
            val spaceInPage = PAGE_SIZE - lastPage.tokenCount
            val tokensToAdd = minOf(spaceInPage, tokensRemaining)
            lastPage.tokenCount += tokensToAdd
            lastPage.lastAccessTime = System.currentTimeMillis()
            tokensRemaining -= tokensToAdd
        }

        // Allocate new pages if needed
        while (tokensRemaining > 0) {
            if (!canAllocate(1)) {
                evictLRU(1, excludeSequence = sequenceId)
                if (!canAllocate(1)) {
                    Timber.w("$TAG: Cannot extend sequence $sequenceId - out of memory")
                    return@withLock false
                }
            }

            val newPage = getFreePage() ?: createNewPage()
            newPage.apply {
                this.tokenCount = minOf(PAGE_SIZE, tokensRemaining)
                this.lastAccessTime = System.currentTimeMillis()
            }

            if (newPage.keyData == null) {
                val pageBytes = bytesPerPage.toInt() / 2
                newPage.keyData = ByteBuffer.allocateDirect(pageBytes)
                newPage.valueData = ByteBuffer.allocateDirect(pageBytes)
                currentUsage.addAndGet(bytesPerPage)
            }

            pages.add(newPage)
            allocatedPages[newPage.pageId] = newPage
            tokensRemaining -= PAGE_SIZE
            pagesAllocated++
        }

        true
    }

    /**
     * Free all pages for a sequence
     */
    suspend fun freeSequence(sequenceId: Long) = mutex.withLock {
        val pages = sequencePages.remove(sequenceId) ?: return@withLock

        pages.forEach { page ->
            allocatedPages.remove(page.pageId)
            page.tokenCount = 0
            freePages.addLast(page)  // Reuse the page
        }

        Timber.d("$TAG: Freed ${pages.size} pages from sequence $sequenceId")
    }

    /**
     * Check if we can allocate more pages
     */
    private fun canAllocate(pagesNeeded: Int): Boolean {
        val pagesInUse = allocatedPages.size
        return (pagesInUse + pagesNeeded) <= maxPages
    }

    /**
     * Get a free page from the pool
     */
    private fun getFreePage(): CachePage? {
        return freePages.pollFirst()
    }

    /**
     * Create a new page
     */
    private fun createNewPage(): CachePage {
        val pageId = pageIdCounter.incrementAndGet()
        val page = CachePage(
            pageId = pageId,
            sequenceId = -1,
            tokenStart = 0,
            tokenCount = 0
        )
        allocatedPages[pageId] = page
        return page
    }

    /**
     * Evict least recently used pages
     */
    private fun evictLRU(pagesNeeded: Int, excludeSequence: Long = -1) {
        // Find sequences to evict (sorted by oldest access time)
        val sequences = sequencePages.entries
            .filter { it.key != excludeSequence }
            .sortedBy { entry -> entry.value.minOfOrNull { it.lastAccessTime } ?: Long.MAX_VALUE }

        var pagesFreed = 0

        for ((seqId, pages) in sequences) {
            if (pagesFreed >= pagesNeeded) break

            // Free oldest pages first within this sequence
            val sortedPages = pages.sortedBy { it.lastAccessTime }
            for (page in sortedPages) {
                if (pagesFreed >= pagesNeeded) break

                pages.remove(page)
                allocatedPages.remove(page.pageId)
                page.tokenCount = 0
                freePages.addLast(page)
                pagesFreed++
                pagesEvicted++
            }

            // Remove sequence if no pages left
            if (pages.isEmpty()) {
                sequencePages.remove(seqId)
            }
        }

        Timber.d("$TAG: Evicted $pagesFreed pages via LRU")
    }

    // IMemoryManager implementation

    override fun checkMemoryAvailable(requiredBytes: Long): Boolean {
        val available = memoryBudgetBytes - currentUsage.get()
        return available >= requiredBytes
    }

    override fun getCurrentMemoryUsage(): Long = currentUsage.get()

    override fun getMemoryBudget(): Long = memoryBudgetBytes

    override suspend fun resetCache() = mutex.withLock {
        sequencePages.clear()
        allocatedPages.values.forEach { page ->
            page.tokenCount = 0
            freePages.addLast(page)
        }
        allocatedPages.clear()
        cacheMisses++
        Timber.d("$TAG: Cache reset")
    }

    override suspend fun optimizeMemory(): Long = mutex.withLock {
        val before = currentUsage.get()

        // Free unused pages
        while (freePages.isNotEmpty()) {
            val page = freePages.pollFirst() ?: break
            page.keyData = null
            page.valueData = null
        }

        System.gc()

        // Recalculate actual usage
        var actualUsage = 0L
        allocatedPages.values.forEach { page ->
            actualUsage += page.sizeBytes()
        }
        currentUsage.set(actualUsage)

        val freed = before - actualUsage
        Timber.i("$TAG: Optimization freed ${freed / 1024}KB")
        freed
    }

    override fun getCacheStats(): Map<String, Any> {
        val totalTokens = sequencePages.values.sumOf { pages ->
            pages.sumOf { it.tokenCount }
        }

        return mapOf(
            "cache_hits" to cacheHits,
            "cache_misses" to cacheMisses,
            "cache_hit_rate" to if (cacheHits + cacheMisses > 0) {
                cacheHits.toFloat() / (cacheHits + cacheMisses)
            } else 0f,
            "current_usage_bytes" to currentUsage.get(),
            "budget_bytes" to memoryBudgetBytes,
            "utilization_pct" to (currentUsage.get().toFloat() / memoryBudgetBytes * 100),
            "pages_allocated" to pagesAllocated,
            "pages_evicted" to pagesEvicted,
            "active_pages" to allocatedPages.size,
            "free_pages" to freePages.size,
            "max_pages" to maxPages,
            "active_sequences" to sequencePages.size,
            "total_cached_tokens" to totalTokens,
            "bytes_per_page" to bytesPerPage
        )
    }

    /**
     * Get number of tokens currently cached for a sequence
     */
    fun getSequenceTokenCount(sequenceId: Long): Int {
        return sequencePages[sequenceId]?.sumOf { it.tokenCount } ?: 0
    }

    /**
     * Get total context length that can be cached with current budget
     */
    fun getMaxContextLength(): Int {
        return maxPages * PAGE_SIZE
    }
}
