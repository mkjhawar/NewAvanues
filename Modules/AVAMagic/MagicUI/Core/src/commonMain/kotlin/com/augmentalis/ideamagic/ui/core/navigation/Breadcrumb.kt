package com.augmentalis.magicui.ui.core.navigation

import com.augmentalis.magicui.components.core.*

/**
 * Breadcrumb Component
 *
 * A breadcrumb navigation component that shows the current location in a
 * hierarchical structure and allows navigation to parent levels.
 *
 * Features:
 * - Hierarchical navigation path
 * - Clickable items to navigate back
 * - Customizable separator
 * - Current page indication
 *
 * Platform mappings:
 * - Android: Custom horizontal layout
 * - iOS: Custom navigation path
 * - Web: Standard breadcrumb navigation
 *
 * Usage:
 * ```kotlin
 * Breadcrumb(
 *     items = listOf(
 *         BreadcrumbItem("Home", "/"),
 *         BreadcrumbItem("Products", "/products"),
 *         BreadcrumbItem("Electronics", "/products/electronics")
 *     ),
 *     separator = "/"
 * )
 * ```
 */
data class BreadcrumbComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val items: List<BreadcrumbItem>,
    val separator: String = "/"
) : Component {
    init {
        require(items.isNotEmpty()) { "Breadcrumb must have at least one item" }
    }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * Individual breadcrumb item
 */
data class BreadcrumbItem(
    val label: String,
    val href: String? = null,
    val onClick: (() -> Unit)? = null
)
