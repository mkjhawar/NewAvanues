package com.augmentalis.avaelements.components.phase3.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import kotlinx.serialization.Transient

data class DataGrid(
    override val type: String = "DataGrid",
    override val id: String? = null,
    val columns: List<DataGridColumn>,
    val rows: List<DataGridRow>,
    val pageSize: Int = 10,
    val currentPage: Int = 1,
    val sortBy: String? = null,
    val sortOrder: SortOrder = SortOrder.Ascending,
    val selectable: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    @Transient val onSort: ((String, SortOrder) -> Unit)? = null,
    @Transient val onPageChange: ((Int) -> Unit)? = null,
    @Transient val onSelectionChange: ((Set<String>) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class DataGridColumn(
    val id: String,
    val label: String,
    val sortable: Boolean = true,
    val width: Size? = null,
    val align: TextAlign = TextAlign.Start
)

data class DataGridRow(
    val id: String,
    val cells: Map<String, Any>
)

enum class SortOrder {
    Ascending,
    Descending
}

enum class TextAlign {
    Start,
    Center,
    End
}
