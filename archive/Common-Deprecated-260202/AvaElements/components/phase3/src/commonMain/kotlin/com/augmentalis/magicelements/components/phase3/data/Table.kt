package com.augmentalis.avaelements.components.phase3.data

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import kotlinx.serialization.Transient

data class Table(
    override val type: String = "Table",
    override val id: String? = null,
    val columns: List<TableColumn>,
    val rows: List<TableRow>,
    val sortable: Boolean = false,
    val hoverable: Boolean = true,
    val striped: Boolean = false,
    @Transient val onRowClick: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

data class TableColumn(
    val id: String,
    val label: String,
    val sortable: Boolean = false,
    val width: Size? = null
)

data class TableRow(
    val id: String,
    val cells: List<TableCell>
)

data class TableCell(
    val content: String,
    val component: Component? = null
)
