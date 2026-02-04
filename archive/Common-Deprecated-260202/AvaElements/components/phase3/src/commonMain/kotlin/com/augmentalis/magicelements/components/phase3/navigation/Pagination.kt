package com.augmentalis.avaelements.components.phase3.navigation

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import kotlinx.serialization.Transient

data class Pagination(
    override val type: String = "Pagination",
    override val id: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int,
    val showFirstLast: Boolean = true,
    val showPrevNext: Boolean = true,
    val maxVisible: Int = 7,
    @Transient val onPageChange: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}
