package com.augmentalis.avamagic.avaui.data

import com.augmentalis.avamagic.components.core.*
import com.augmentalis.avamagic.components.core.ComponentStyle
import com.augmentalis.avamagic.components.core.Size
import com.augmentalis.avamagic.components.core.Color
import com.augmentalis.avamagic.components.core.Spacing
import com.augmentalis.avamagic.components.core.Border
import com.augmentalis.avamagic.components.core.Shadow

/**
 * Table Component
 *
 * A table component for displaying structured data in rows and columns.
 *
 * Features:
 * - Column headers with optional sorting
 * - Customizable column widths
 * - Row click handlers
 * - Striped and hoverable rows
 *
 * Platform mappings:
 * - Android: RecyclerView with grid layout
 * - iOS: UITableView with custom cells
 * - Web: HTML table or grid
 *
 * Usage:
 * ```kotlin
 * Table(
 *     columns = listOf(
 *         TableColumn("name", "Name", sortable = true),
 *         TableColumn("email", "Email"),
 *         TableColumn("role", "Role")
 *     ),
 *     rows = listOf(
 *         TableRow("1", listOf(
 *             TableCell("John Doe"),
 *             TableCell("john@example.com"),
 *             TableCell("Admin")
 *         ))
 *     ),
 *     sortable = true,
 *     hoverable = true
 * )
 * ```
 */
data class TableComponent(
    val type: String = "Table",
    val columns: List<TableColumn>,
    val rows: List<TableRow>,
    val sortable: Boolean = false,
    val hoverable: Boolean = true,
    val striped: Boolean = false,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onRowClick: ((Int) -> Unit)? = null
) : Component {
    init {
        require(columns.isNotEmpty()) { "Table must have at least one column" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Table column definition
 */
data class TableColumn(
    val id: String,
    val label: String,
    val sortable: Boolean = false,
    val width: Size? = null
)

/**
 * Table row containing cells
 */
data class TableRow(
    val id: String,
    val cells: List<TableCell>
)

/**
 * Individual table cell
 */
data class TableCell(
    val content: String,
    val component: Component? = null
)
