package com.avanueui.data

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.ComponentStyle
import com.augmentalis.avanueui.core.Size
import com.augmentalis.avanueui.core.Color
import com.augmentalis.avanueui.core.Spacing
import com.augmentalis.avanueui.core.Border
import com.augmentalis.avanueui.core.Shadow

/**
 * DataGrid Component
 *
 * An advanced data grid component with sorting, pagination, and selection.
 *
 * Features:
 * - Column-based data display
 * - Sorting by column
 * - Pagination
 * - Row selection (single/multiple)
 * - Customizable column alignment and width
 *
 * Platform mappings:
 * - Android: RecyclerView with advanced features
 * - iOS: UITableView with sorting
 * - Web: Data table with controls
 *
 * Usage:
 * ```kotlin
 * DataGrid(
 *     columns = listOf(
 *         DataGridColumn("name", "Name", sortable = true),
 *         DataGridColumn("age", "Age", sortable = true, align = TextAlign.End)
 *     ),
 *     rows = listOf(
 *         DataGridRow("1", mapOf("name" to "John", "age" to 30)),
 *         DataGridRow("2", mapOf("name" to "Jane", "age" to 25))
 *     ),
 *     pageSize = 10,
 *     sortBy = "name",
 *     selectable = true,
 *     onSort = { column, order -> /* handle sort */ }
 * )
 * ```
 */
data class DataGridComponent(
    val type: String = "DataGrid",
    val columns: List<DataGridColumn>,
    val rows: List<DataGridRow>,
    val pageSize: Int = 10,
    val currentPage: Int = 1,
    val sortBy: String? = null,
    val sortOrder: SortOrder = SortOrder.Ascending,
    val selectable: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onSort: ((String, SortOrder) -> Unit)? = null,
    val onPageChange: ((Int) -> Unit)? = null,
    val onSelectionChange: ((Set<String>) -> Unit)? = null
) : Component {
    init {
        require(columns.isNotEmpty()) { "DataGrid must have at least one column" }
        require(pageSize > 0) { "pageSize must be greater than 0" }
        require(currentPage > 0) { "currentPage must be greater than 0" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Data grid column definition
 */
data class DataGridColumn(
    val id: String,
    val label: String,
    val sortable: Boolean = true,
    val width: Size? = null,
    val align: TextAlign = TextAlign.Start
)

/**
 * Data grid row with cell values
 */
data class DataGridRow(
    val id: String,
    val cells: Map<String, Any>
)

/**
 * Sort order for columns
 */
enum class SortOrder {
    Ascending, Descending
}

/**
 * Text alignment options
 */
enum class TextAlign {
    Start, Center, End
}
