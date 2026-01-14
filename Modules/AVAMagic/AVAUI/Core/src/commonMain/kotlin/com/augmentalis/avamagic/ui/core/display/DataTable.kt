package com.augmentalis.avamagic.ui.core.display
import com.augmentalis.avamagic.components.core.*
data class DataTableComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val headers: List<String>, val rows: List<List<String>>, val sortable: Boolean = true, val selectable: Boolean = false, val selectedRows: Set<Int> = emptySet(), val size: ComponentSize = ComponentSize.MD) : Component {
    init { require(headers.isNotEmpty()) { "headers cannot be empty" }; require(rows.all { it.size == headers.size }) { "All rows must match header count" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun toggleRow(index: Int): DataTableComponent = if (index in selectedRows) copy(selectedRows = selectedRows - index) else copy(selectedRows = selectedRows + index)
}
