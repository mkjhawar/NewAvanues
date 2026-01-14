package com.augmentalis.avaelements.components.navigation

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

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
    override val type: String = "Breadcrumb",
    val items: List<BreadcrumbItem>,
    val separator: String = "/",
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    init {
        require(items.isNotEmpty()) { "Breadcrumb must have at least one item" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Individual breadcrumb item
 */
data class BreadcrumbItem(
    val label: String,
    val href: String? = null,
    val onClick: (() -> Unit)? = null
)
