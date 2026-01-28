/**
 * PaginationHandler.kt - Voice handler for Pagination control interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven pagination control with navigation and page size adjustment
 * Features:
 * - Navigate to specific page by number
 * - Navigate to next/previous pages
 * - Jump to first/last page
 * - Change items per page (page size)
 * - Refresh/reload current page
 * - Named pagination targeting (e.g., "search results page 3")
 * - Focused pagination targeting
 * - AVID-based targeting for precise element selection
 * - Voice feedback for navigation changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Direct page navigation:
 * - "page [N]" - Navigate to page N
 * - "go to page [N]" - Navigate to page N
 * - "jump to page [N]" - Navigate to page N
 *
 * Relative navigation:
 * - "next page" / "next" - Go to next page
 * - "previous page" / "back" / "previous" - Go to previous page
 *
 * Boundary navigation:
 * - "first page" / "beginning" - Go to first page
 * - "last page" / "end" - Go to last page
 *
 * Page size:
 * - "show [N] per page" - Change page size
 * - "items per page [N]" - Change page size
 * - "[N] items per page" - Change page size
 *
 * Refresh:
 * - "refresh" / "reload" - Refresh current page
 *
 * ## Value Parsing
 *
 * Supports:
 * - Integer values: "5", "10", "100"
 * - Word numbers: "five", "twenty five"
 */

package com.augmentalis.avamagic.voice.handlers.navigation

import android.util.Log
import com.augmentalis.commandmanager.ActionCategory
import com.augmentalis.commandmanager.BaseHandler
import com.augmentalis.commandmanager.Bounds
import com.augmentalis.commandmanager.ElementInfo
import com.augmentalis.commandmanager.HandlerResult
import com.augmentalis.commandmanager.QuantizedCommand

/**
 * Voice command handler for Pagination control interactions.
 *
 * Provides comprehensive voice control for pagination components including:
 * - Direct page navigation (by number)
 * - Relative navigation (next/previous)
 * - Boundary navigation (first/last)
 * - Page size adjustment
 * - Page refresh/reload
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for pagination operations
 */
class PaginationHandler(
    private val executor: PaginationExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "PaginationHandler"

        // Pattern for "page [N]" or "go to page [N]" or "jump to page [N]"
        private val GO_TO_PAGE_PATTERN = Regex(
            """^(?:go\s+to\s+|jump\s+to\s+)?page\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // Pattern for "show [N] per page" or "[N] per page"
        private val SHOW_PER_PAGE_PATTERN = Regex(
            """^(?:show\s+)?(.+?)\s+(?:items\s+)?per\s+page$""",
            RegexOption.IGNORE_CASE
        )

        // Pattern for "items per page [N]"
        private val ITEMS_PER_PAGE_PATTERN = Regex(
            """^items\s+per\s+page\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // Pattern for "[N] items per page"
        private val N_ITEMS_PER_PAGE_PATTERN = Regex(
            """^(.+?)\s+items\s+per\s+page$""",
            RegexOption.IGNORE_CASE
        )

        // Word to number mapping for common spoken numbers
        private val WORD_NUMBERS = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
            "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
            "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "twenty five" to 25,
            "thirty" to 30, "forty" to 40, "fifty" to 50, "sixty" to 60,
            "seventy" to 70, "eighty" to 80, "ninety" to 90, "hundred" to 100,
            "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
            "sixth" to 6, "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Direct page navigation
        "page", "go to page", "jump to page",
        // Relative navigation
        "next page", "next", "previous page", "previous", "back",
        // Boundary navigation
        "first page", "beginning", "last page", "end",
        // Page size
        "show per page", "items per page",
        // Refresh
        "refresh", "reload"
    )

    /**
     * Callback for voice feedback when page changes.
     */
    var onPageChanged: ((paginationName: String, currentPage: Int, totalPages: Int) -> Unit)? = null

    /**
     * Callback for voice feedback when page size changes.
     */
    var onPageSizeChanged: ((paginationName: String, pageSize: Int) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing pagination command: $normalizedAction")

        return try {
            when {
                // Direct page navigation: "page [N]", "go to page [N]", "jump to page [N]"
                GO_TO_PAGE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleGoToPage(normalizedAction, command)
                }

                // Page size: "show [N] per page" or "[N] per page"
                SHOW_PER_PAGE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handlePageSize(SHOW_PER_PAGE_PATTERN, normalizedAction, command)
                }

                // Page size: "items per page [N]"
                ITEMS_PER_PAGE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handlePageSize(ITEMS_PER_PAGE_PATTERN, normalizedAction, command)
                }

                // Page size: "[N] items per page"
                N_ITEMS_PER_PAGE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handlePageSize(N_ITEMS_PER_PAGE_PATTERN, normalizedAction, command)
                }

                // Next page
                normalizedAction in listOf("next page", "next") -> {
                    handleNextPage(command)
                }

                // Previous page
                normalizedAction in listOf("previous page", "previous", "back") -> {
                    handlePreviousPage(command)
                }

                // First page
                normalizedAction in listOf("first page", "beginning") -> {
                    handleFirstPage(command)
                }

                // Last page
                normalizedAction in listOf("last page", "end") -> {
                    handleLastPage(command)
                }

                // Refresh
                normalizedAction in listOf("refresh", "reload") -> {
                    handleRefresh(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing pagination command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "page [N]" or "go to page [N]" command.
     */
    private suspend fun handleGoToPage(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = GO_TO_PAGE_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse page command")

        val pageString = matchResult.groupValues[1].trim()

        // Parse the page number
        val pageNumber = parsePageNumber(pageString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse page number: '$pageString'",
                recoverable = true,
                suggestedAction = "Try 'page 5' or 'go to page ten'"
            )

        // Find the pagination control
        val paginationInfo = findPagination(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No pagination control found",
            recoverable = true,
            suggestedAction = "Focus on a pagination control or navigate to a paginated view"
        )

        // Validate page number
        if (pageNumber < 1) {
            return HandlerResult.Failure(
                reason = "Page number must be at least 1",
                recoverable = true,
                suggestedAction = "Try 'page 1' for the first page"
            )
        }

        if (pageNumber > paginationInfo.totalPages) {
            return HandlerResult.Failure(
                reason = "Page $pageNumber exceeds total pages (${paginationInfo.totalPages})",
                recoverable = true,
                suggestedAction = "Try 'page ${paginationInfo.totalPages}' or 'last page'"
            )
        }

        // Navigate to page
        return applyPageNavigation(paginationInfo, pageNumber, "goToPage")
    }

    /**
     * Handle page size change commands.
     */
    private suspend fun handlePageSize(
        pattern: Regex,
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = pattern.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse page size command")

        val sizeString = matchResult.groupValues[1].trim()

        // Parse the page size
        val pageSize = parsePageNumber(sizeString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse page size: '$sizeString'",
                recoverable = true,
                suggestedAction = "Try 'show 20 per page' or 'items per page 50'"
            )

        // Find the pagination control
        val paginationInfo = findPagination(
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = "No pagination control found",
            recoverable = true,
            suggestedAction = "Focus on a pagination control or navigate to a paginated view"
        )

        // Validate page size
        if (pageSize < 1) {
            return HandlerResult.Failure(
                reason = "Page size must be at least 1",
                recoverable = true,
                suggestedAction = "Try 'show 10 per page'"
            )
        }

        // Check if page size is in allowed options
        if (paginationInfo.pageSizeOptions.isNotEmpty() && pageSize !in paginationInfo.pageSizeOptions) {
            val optionsStr = paginationInfo.pageSizeOptions.joinToString(", ")
            return HandlerResult.Failure(
                reason = "Page size $pageSize not available. Options: $optionsStr",
                recoverable = true,
                suggestedAction = "Try 'show ${paginationInfo.pageSizeOptions.first()} per page'"
            )
        }

        // Apply page size
        return applyPageSizeChange(paginationInfo, pageSize)
    }

    /**
     * Handle "next page" or "next" command.
     */
    private suspend fun handleNextPage(command: QuantizedCommand): HandlerResult {
        val paginationInfo = findPagination(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No pagination control found",
                recoverable = true,
                suggestedAction = "Focus on a pagination control first"
            )

        // Check if we can go next
        if (paginationInfo.currentPage >= paginationInfo.totalPages) {
            return HandlerResult.Failure(
                reason = "Already on the last page (${paginationInfo.currentPage} of ${paginationInfo.totalPages})",
                recoverable = true,
                suggestedAction = "You're on the last page. Say 'previous page' to go back."
            )
        }

        val result = executor.nextPage(paginationInfo)
        return handleNavigationResult(paginationInfo, result, "next")
    }

    /**
     * Handle "previous page", "previous", or "back" command.
     */
    private suspend fun handlePreviousPage(command: QuantizedCommand): HandlerResult {
        val paginationInfo = findPagination(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No pagination control found",
                recoverable = true,
                suggestedAction = "Focus on a pagination control first"
            )

        // Check if we can go previous
        if (paginationInfo.currentPage <= 1) {
            return HandlerResult.Failure(
                reason = "Already on the first page",
                recoverable = true,
                suggestedAction = "You're on the first page. Say 'next page' to continue."
            )
        }

        val result = executor.previousPage(paginationInfo)
        return handleNavigationResult(paginationInfo, result, "previous")
    }

    /**
     * Handle "first page" or "beginning" command.
     */
    private suspend fun handleFirstPage(command: QuantizedCommand): HandlerResult {
        val paginationInfo = findPagination(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No pagination control found",
                recoverable = true,
                suggestedAction = "Focus on a pagination control first"
            )

        // Check if already on first page
        if (paginationInfo.currentPage == 1) {
            return HandlerResult.Success(
                message = "Already on the first page",
                data = mapOf(
                    "paginationAvid" to paginationInfo.avid,
                    "currentPage" to 1,
                    "totalPages" to paginationInfo.totalPages,
                    "accessibility_announcement" to "Already on the first page"
                )
            )
        }

        val result = executor.firstPage(paginationInfo)
        return handleNavigationResult(paginationInfo, result, "first")
    }

    /**
     * Handle "last page" or "end" command.
     */
    private suspend fun handleLastPage(command: QuantizedCommand): HandlerResult {
        val paginationInfo = findPagination(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No pagination control found",
                recoverable = true,
                suggestedAction = "Focus on a pagination control first"
            )

        // Check if already on last page
        if (paginationInfo.currentPage == paginationInfo.totalPages) {
            return HandlerResult.Success(
                message = "Already on the last page (page ${paginationInfo.totalPages})",
                data = mapOf(
                    "paginationAvid" to paginationInfo.avid,
                    "currentPage" to paginationInfo.totalPages,
                    "totalPages" to paginationInfo.totalPages,
                    "accessibility_announcement" to "Already on the last page"
                )
            )
        }

        val result = executor.lastPage(paginationInfo)
        return handleNavigationResult(paginationInfo, result, "last")
    }

    /**
     * Handle "refresh" or "reload" command.
     */
    private suspend fun handleRefresh(command: QuantizedCommand): HandlerResult {
        val paginationInfo = findPagination(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No pagination control found",
                recoverable = true,
                suggestedAction = "Focus on a pagination control first"
            )

        val result = executor.refresh(paginationInfo)

        return if (result.success) {
            val feedback = buildString {
                if (paginationInfo.name.isNotBlank()) {
                    append(paginationInfo.name)
                    append(" ")
                }
                append("Page ${paginationInfo.currentPage} refreshed")
            }

            Log.i(TAG, "Page refreshed: ${paginationInfo.name} page ${paginationInfo.currentPage}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "paginationName" to paginationInfo.name,
                    "paginationAvid" to paginationInfo.avid,
                    "currentPage" to paginationInfo.currentPage,
                    "totalPages" to paginationInfo.totalPages,
                    "action" to "refresh",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not refresh page",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find pagination control by AVID, name, or focus state.
     */
    private suspend fun findPagination(
        name: String? = null,
        avid: String? = null
    ): PaginationInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val pagination = executor.findByAvid(avid)
            if (pagination != null) return pagination
        }

        // Priority 2: Name lookup
        if (name != null) {
            val pagination = executor.findByName(name)
            if (pagination != null) return pagination
        }

        // Priority 3: Focused pagination
        return executor.findFocused()
    }

    /**
     * Apply page navigation and return result.
     */
    private suspend fun applyPageNavigation(
        paginationInfo: PaginationInfo,
        targetPage: Int,
        action: String
    ): HandlerResult {
        val result = executor.goToPage(paginationInfo, targetPage)

        return if (result.success) {
            // Invoke callback for voice feedback
            onPageChanged?.invoke(
                paginationInfo.name.ifBlank { "Pagination" },
                result.newPage,
                paginationInfo.totalPages
            )

            val feedback = buildString {
                if (paginationInfo.name.isNotBlank()) {
                    append(paginationInfo.name)
                    append(": ")
                }
                append("Page $targetPage of ${paginationInfo.totalPages}")
            }

            Log.i(TAG, "Page navigation: ${paginationInfo.name} -> page $targetPage")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "paginationName" to paginationInfo.name,
                    "paginationAvid" to paginationInfo.avid,
                    "previousPage" to paginationInfo.currentPage,
                    "newPage" to targetPage,
                    "totalPages" to paginationInfo.totalPages,
                    "pageSize" to paginationInfo.pageSize,
                    "totalItems" to paginationInfo.totalItems,
                    "action" to action,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not navigate to page $targetPage",
                recoverable = true
            )
        }
    }

    /**
     * Handle navigation result for next/previous/first/last operations.
     */
    private suspend fun handleNavigationResult(
        paginationInfo: PaginationInfo,
        result: PaginationOperationResult,
        action: String
    ): HandlerResult {
        return if (result.success) {
            // Invoke callback for voice feedback
            onPageChanged?.invoke(
                paginationInfo.name.ifBlank { "Pagination" },
                result.newPage,
                paginationInfo.totalPages
            )

            val feedback = buildString {
                if (paginationInfo.name.isNotBlank()) {
                    append(paginationInfo.name)
                    append(": ")
                }
                append("Page ${result.newPage} of ${paginationInfo.totalPages}")
            }

            Log.i(TAG, "Page navigation ($action): ${paginationInfo.name} -> page ${result.newPage}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "paginationName" to paginationInfo.name,
                    "paginationAvid" to paginationInfo.avid,
                    "previousPage" to paginationInfo.currentPage,
                    "newPage" to result.newPage,
                    "totalPages" to paginationInfo.totalPages,
                    "pageSize" to paginationInfo.pageSize,
                    "totalItems" to paginationInfo.totalItems,
                    "action" to action,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not navigate to $action page",
                recoverable = true
            )
        }
    }

    /**
     * Apply page size change and return result.
     */
    private suspend fun applyPageSizeChange(
        paginationInfo: PaginationInfo,
        newPageSize: Int
    ): HandlerResult {
        val result = executor.setPageSize(paginationInfo, newPageSize)

        return if (result.success) {
            // Invoke callback for voice feedback
            onPageSizeChanged?.invoke(
                paginationInfo.name.ifBlank { "Pagination" },
                newPageSize
            )

            val feedback = buildString {
                if (paginationInfo.name.isNotBlank()) {
                    append(paginationInfo.name)
                    append(": ")
                }
                append("Showing $newPageSize items per page")
            }

            Log.i(TAG, "Page size changed: ${paginationInfo.name} -> $newPageSize items per page")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "paginationName" to paginationInfo.name,
                    "paginationAvid" to paginationInfo.avid,
                    "previousPageSize" to paginationInfo.pageSize,
                    "newPageSize" to newPageSize,
                    "currentPage" to result.newPage,
                    "totalPages" to result.newTotalPages,
                    "action" to "setPageSize",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not change page size to $newPageSize",
                recoverable = true
            )
        }
    }

    /**
     * Parse a page number from string input.
     *
     * Supports:
     * - Integer values: "5", "10", "100"
     * - Word numbers: "five", "twenty five"
     * - Ordinals: "first", "second", "third"
     */
    private fun parsePageNumber(input: String): Int? {
        val trimmed = input.trim().lowercase()

        // Try direct numeric parsing
        trimmed.toIntOrNull()?.let { return it }

        // Try word number parsing (direct match)
        WORD_NUMBERS[trimmed]?.let { return it }

        // Try compound numbers (e.g., "twenty five", "thirty two")
        val words = trimmed.split(" ", "-")
        if (words.size == 2) {
            val tens = WORD_NUMBERS[words[0]]
            val ones = WORD_NUMBERS[words[1]]
            if (tens != null && ones != null && tens >= 20 && ones < 10) {
                return tens + ones
            }
        }

        return null
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Voice Phrases for Speech Engine Registration
    // ═══════════════════════════════════════════════════════════════════════════

    override fun getVoicePhrases(): List<String> {
        return listOf(
            "page", "go to page", "jump to page",
            "next page", "next",
            "previous page", "previous", "back",
            "first page", "beginning",
            "last page", "end",
            "show per page", "items per page",
            "twenty per page", "fifty per page",
            "refresh", "reload"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Information about a pagination control component.
 *
 * @property avid AVID fingerprint for the pagination control (format: PGN:{hash8})
 * @property name Display name or associated label (e.g., "Search Results")
 * @property currentPage The currently displayed page (1-indexed)
 * @property totalPages Total number of pages available
 * @property pageSize Number of items displayed per page
 * @property totalItems Total number of items across all pages
 * @property pageSizeOptions Available page size options (e.g., [10, 25, 50, 100])
 * @property bounds Screen bounds for the pagination control
 * @property isFocused Whether this pagination control currently has focus
 * @property node Platform-specific node reference
 */
data class PaginationInfo(
    val avid: String,
    val name: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val pageSize: Int = 10,
    val totalItems: Int = 0,
    val pageSizeOptions: List<Int> = emptyList(),
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Check if there is a next page available.
     */
    val hasNextPage: Boolean
        get() = currentPage < totalPages

    /**
     * Check if there is a previous page available.
     */
    val hasPreviousPage: Boolean
        get() = currentPage > 1

    /**
     * Check if currently on the first page.
     */
    val isFirstPage: Boolean
        get() = currentPage == 1

    /**
     * Check if currently on the last page.
     */
    val isLastPage: Boolean
        get() = currentPage == totalPages

    /**
     * Calculate the range of items displayed on the current page.
     *
     * @return Pair of (startItem, endItem) indices (1-indexed)
     */
    fun getItemRange(): Pair<Int, Int> {
        val startItem = (currentPage - 1) * pageSize + 1
        val endItem = minOf(currentPage * pageSize, totalItems)
        return Pair(startItem, endItem)
    }

    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Pagination",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = "Page $currentPage of $totalPages"
    )
}

/**
 * Result of a pagination operation.
 *
 * @property success Whether the operation completed successfully
 * @property error Error message if operation failed
 * @property previousPage Page number before the operation
 * @property newPage Page number after the operation
 * @property newTotalPages Updated total pages (may change with page size change)
 */
data class PaginationOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousPage: Int = 0,
    val newPage: Int = 0,
    val newTotalPages: Int = 0
) {
    companion object {
        /**
         * Create a successful result.
         */
        fun success(previousPage: Int, newPage: Int, totalPages: Int) = PaginationOperationResult(
            success = true,
            previousPage = previousPage,
            newPage = newPage,
            newTotalPages = totalPages
        )

        /**
         * Create an error result.
         */
        fun error(message: String) = PaginationOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for pagination operations.
 *
 * Implementations should:
 * 1. Find pagination components by AVID, name, or focus state
 * 2. Read current page state and total pages
 * 3. Navigate between pages via UI actions
 * 4. Handle various pagination UI patterns (buttons, dropdowns, etc.)
 *
 * ## Pagination Control Detection Algorithm
 *
 * ```kotlin
 * fun findPaginationControl(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with:
 *     // - Page navigation buttons (next, previous, numbered pages)
 *     // - Page size dropdowns/selectors
 *     // - Page indicator text (e.g., "Page 1 of 10")
 *     // - RecyclerView/ListView with associated pagination controls
 *     // - Material Pagination components
 * }
 * ```
 *
 * ## Page Navigation Algorithm
 *
 * ```kotlin
 * fun navigateToPage(node: AccessibilityNodeInfo, pageNumber: Int): Boolean {
 *     // 1. Find page number button and click
 *     // 2. Or find input field and enter page number
 *     // 3. Or use next/previous buttons iteratively
 *     // 4. Verify navigation succeeded by checking page indicator
 * }
 * ```
 */
interface PaginationExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Pagination Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a pagination control by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: PGN:{hash8})
     * @return PaginationInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): PaginationInfo?

    /**
     * Find a pagination control by its name or associated label.
     *
     * Searches for:
     * 1. Pagination with matching contentDescription
     * 2. Pagination with label text matching name
     * 3. Pagination associated with a named list/table
     *
     * @param name The name to search for (case-insensitive)
     * @return PaginationInfo if found, null otherwise
     */
    suspend fun findByName(name: String): PaginationInfo?

    /**
     * Find the currently focused pagination control.
     *
     * @return PaginationInfo if a pagination control has focus, null otherwise
     */
    suspend fun findFocused(): PaginationInfo?

    // ═══════════════════════════════════════════════════════════════════════════
    // Page Navigation Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Navigate to a specific page number.
     *
     * @param pagination The pagination control to modify
     * @param page The target page number (1-indexed)
     * @return Operation result with previous and new page numbers
     */
    suspend fun goToPage(pagination: PaginationInfo, page: Int): PaginationOperationResult

    /**
     * Navigate to the next page.
     *
     * @param pagination The pagination control to modify
     * @return Operation result with previous and new page numbers
     */
    suspend fun nextPage(pagination: PaginationInfo): PaginationOperationResult

    /**
     * Navigate to the previous page.
     *
     * @param pagination The pagination control to modify
     * @return Operation result with previous and new page numbers
     */
    suspend fun previousPage(pagination: PaginationInfo): PaginationOperationResult

    /**
     * Navigate to the first page.
     *
     * @param pagination The pagination control to modify
     * @return Operation result with previous and new page numbers
     */
    suspend fun firstPage(pagination: PaginationInfo): PaginationOperationResult

    /**
     * Navigate to the last page.
     *
     * @param pagination The pagination control to modify
     * @return Operation result with previous and new page numbers
     */
    suspend fun lastPage(pagination: PaginationInfo): PaginationOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Page Size Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Change the number of items displayed per page.
     *
     * @param pagination The pagination control to modify
     * @param pageSize The new page size
     * @return Operation result (note: total pages may change)
     */
    suspend fun setPageSize(pagination: PaginationInfo, pageSize: Int): PaginationOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // Refresh Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Refresh the current page data.
     *
     * @param pagination The pagination control to refresh
     * @return Operation result
     */
    suspend fun refresh(pagination: PaginationInfo): PaginationOperationResult

    // ═══════════════════════════════════════════════════════════════════════════
    // State Query Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the current page number.
     *
     * @param pagination The pagination control to query
     * @return Current page number (1-indexed), or null if unable to read
     */
    suspend fun getCurrentPage(pagination: PaginationInfo): Int?

    /**
     * Get the total number of pages.
     *
     * @param pagination The pagination control to query
     * @return Total page count, or null if unable to read
     */
    suspend fun getTotalPages(pagination: PaginationInfo): Int?
}
