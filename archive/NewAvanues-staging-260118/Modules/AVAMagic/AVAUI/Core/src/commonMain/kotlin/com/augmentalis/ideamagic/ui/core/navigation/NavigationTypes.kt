package com.augmentalis.avamagic.ui.core.navigation

import com.augmentalis.avamagic.components.core.*

// Type aliases for mappers - point to existing components
typealias AppBar = AppBarComponent
typealias BottomNav = BottomNavComponent
typealias Breadcrumb = BreadcrumbComponent

// AppBar variants
enum class AppBarVariant {
    SMALL,
    MEDIUM,
    LARGE,
    CENTER_ALIGNED
}

// Pagination component (not defined elsewhere)
data class Pagination(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val currentPage: Int,
    val totalPages: Int,
    val variant: PaginationVariant = PaginationVariant.STANDARD,
    val showFirstLast: Boolean = true,
    val siblingCount: Int = 1,
    val onPageChange: ((Int) -> Unit)? = null
) : Component {
    init {
        require(currentPage >= 1) { "Current page must be >= 1" }
        require(totalPages >= 1) { "Total pages must be >= 1" }
        require(currentPage <= totalPages) { "Current page cannot exceed total pages" }
    }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

enum class PaginationVariant {
    STANDARD,
    OUTLINED,
    COMPACT
}
