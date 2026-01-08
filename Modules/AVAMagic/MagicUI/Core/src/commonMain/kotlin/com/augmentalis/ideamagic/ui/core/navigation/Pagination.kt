package com.augmentalis.magicui.ui.core.navigation

import com.augmentalis.magicui.components.core.*

/**
 * Pagination Component
 *
 * A pagination component for navigating through multiple pages of content.
 *
 * Features:
 * - Current page display
 * - Next/previous navigation
 * - Jump to first/last page
 * - Configurable number of visible page numbers
 * - Ellipsis for large page counts
 *
 * Platform mappings:
 * - Android: Custom pagination control
 * - iOS: Custom pagination control
 * - Web: Standard pagination
 *
 * Usage:
 * ```kotlin
 * Pagination(
 *     currentPage = 3,
 *     totalPages = 10,
 *     showFirstLast = true,
 *     showPrevNext = true,
 *     maxVisible = 7,
 *     onPageChange = { page -> /* load page */ }
 * )
 * ```
 */
data class PaginationComponent(
    val currentPage: Int = 1,
    val totalPages: Int,
    val showFirstLast: Boolean = true,
    val showPrevNext: Boolean = true,
    val maxVisible: Int = 7,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList(),
    val onPageChange: ((Int) -> Unit)? = null
) {
    init {
        require(totalPages > 0) { "totalPages must be greater than 0" }
        require(currentPage in 1..totalPages) { "currentPage must be between 1 and totalPages" }
        require(maxVisible > 0) { "maxVisible must be greater than 0" }
    }

}
