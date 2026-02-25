package com.augmentalis.avanueui.ui.core.display

import com.augmentalis.avanueui.core.*

/**
 * Advanced data grid with sorting, filtering, and pagination.
 *
 * DataGrid provides a feature-rich tabular data display with sorting,
 * filtering, pagination, column resizing, and row selection.
 *
 * ## Usage Examples
 * ```kotlin
 * // Basic data grid
 * val grid = DataGridComponent(
 *     columns = listOf(
 *         DataGridColumn("id", "ID"),
 *         DataGridColumn("name", "Name"),
 *         DataGridColumn("email", "Email")
 *     ),
 *     rows = listOf(
 *         mapOf("id" to "1", "name" to "Alice", "email" to "alice@example.com"),
 *         mapOf("id" to "2", "name" to "Bob", "email" to "bob@example.com")
 *     )
 * )
 *
 * // With sorting
 * val grid = DataGridComponent(
 *     columns = columns,
 *     rows = rows,
 *     sortable = true,
 *     sortedBy = "name",
 *     sortAscending = true
 * )
 *
 * // With pagination
 * val grid = DataGridComponent(
 *     columns = columns,
 *     rows = allRows,
 *     paginated = true,
 *     pageSize = 10,
 *     currentPage = 1
 * )
 *
 * // With row selection
 * val grid = DataGridComponent(
 *     columns = columns,
 *     rows = rows,
 *     selectable = true,
 *     selectedRowIndices = setOf(0, 2)
 * )
 * ```
 *
 * @property columns Column definitions
 * @property rows Data rows (maps of column key to value)
 * @property sortable Whether columns can be sorted (default true)
 * @property sortedBy Column key currently sorted by
 * @property sortAscending Sort direction (default true)
 * @property filterable Whether columns can be filtered (default true)
 * @property filters Current filter values by column key
 * @property paginated Whether to show pagination (default false)
 * @property pageSize Rows per page (default 10)
 * @property currentPage Current page number (1-indexed, default 1)
 * @property selectable Whether rows can be selected (default false)
 * @property selectedRowIndices Set of selected row indices
 * @property resizable Whether columns can be resized (default true)
 * @property size Grid size (default MD)
 * @since 1.0.0
 */
data class DataGridComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val columns: List<DataGridColumn>,
    val rows: List<Map<String, Any>>,
    val sortable: Boolean = true,
    val sortedBy: String? = null,
    val sortAscending: Boolean = true,
    val filterable: Boolean = true,
    val filters: Map<String, String> = emptyMap(),
    val paginated: Boolean = false,
    val pageSize: Int = 10,
    val currentPage: Int = 1,
    val selectable: Boolean = false,
    val selectedRowIndices: Set<Int> = emptySet(),
    val resizable: Boolean = true,
    val size: ComponentSize = ComponentSize.MD
) : Component {
    init {
        require(columns.isNotEmpty()) { "Columns cannot be empty" }
        require(columns.map { it.key }.distinct().size == columns.size) {
            "Column keys must be unique"
        }
        if (sortedBy != null) {
            require(columns.any { it.key == sortedBy }) {
                "sortedBy '$sortedBy' must match a column key"
            }
        }
        require(pageSize > 0) { "pageSize must be positive (got $pageSize)" }
        require(currentPage > 0) { "currentPage must be positive (got $currentPage)" }
        require(selectedRowIndices.all { it in rows.indices }) {
            "All selectedRowIndices must be valid row indices"
        }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    /**
     * Returns filtered rows based on current filters.
     */
    val filteredRows: List<Map<String, Any>>
        get() = if (filters.isEmpty()) {
            rows
        } else {
            rows.filter { row ->
                filters.all { (key, filterValue) ->
                    val cellValue = row[key]?.toString() ?: ""
                    cellValue.contains(filterValue, ignoreCase = true)
                }
            }
        }

    /**
     * Returns sorted rows based on current sort settings.
     */
    val sortedRows: List<Map<String, Any>>
        get() {
            val filtered = filteredRows
            return if (sortedBy == null) {
                filtered
            } else {
                val sorted = filtered.sortedBy { row ->
                    row[sortedBy]?.toString() ?: ""
                }
                if (sortAscending) sorted else sorted.reversed()
            }
        }

    /**
     * Returns current page rows based on pagination settings.
     */
    val currentPageRows: List<Map<String, Any>>
        get() {
            val sorted = sortedRows
            return if (paginated) {
                val startIndex = (currentPage - 1) * pageSize
                sorted.drop(startIndex).take(pageSize)
            } else {
                sorted
            }
        }

    /**
     * Total number of pages.
     */
    val totalPages: Int
        get() = if (paginated) {
            (sortedRows.size + pageSize - 1) / pageSize
        } else {
            1
        }

    /**
     * Sorts by the given column key.
     */
    fun sortBy(columnKey: String): DataGridComponent {
        require(columns.any { it.key == columnKey }) {
            "Column key '$columnKey' not found"
        }
        val ascending = if (sortedBy == columnKey) !sortAscending else true
        return copy(sortedBy = columnKey, sortAscending = ascending)
    }

    /**
     * Adds or updates a filter for the given column.
     */
    fun setFilter(columnKey: String, filterValue: String): DataGridComponent {
        require(columns.any { it.key == columnKey }) {
            "Column key '$columnKey' not found"
        }
        return copy(filters = filters + (columnKey to filterValue))
    }

    /**
     * Removes filter for the given column.
     */
    fun clearFilter(columnKey: String): DataGridComponent {
        return copy(filters = filters - columnKey)
    }

    /**
     * Clears all filters.
     */
    fun clearAllFilters(): DataGridComponent {
        return copy(filters = emptyMap())
    }

    /**
     * Navigates to the given page.
     */
    fun goToPage(page: Int): DataGridComponent {
        require(page in 1..totalPages) { "Page $page out of range 1..$totalPages" }
        return copy(currentPage = page)
    }

    /**
     * Toggles row selection at the given index.
     */
    fun toggleRowSelection(index: Int): DataGridComponent {
        require(index in rows.indices) { "Index $index out of bounds" }
        val newSelection = if (index in selectedRowIndices) {
            selectedRowIndices - index
        } else {
            selectedRowIndices + index
        }
        return copy(selectedRowIndices = newSelection)
    }

    /**
     * Selects all rows.
     */
    fun selectAll(): DataGridComponent {
        return copy(selectedRowIndices = rows.indices.toSet())
    }

    /**
     * Clears all row selections.
     */
    fun clearSelection(): DataGridComponent {
        return copy(selectedRowIndices = emptySet())
    }

    companion object {
        /**
         * Creates a simple data grid without advanced features.
         */
        fun simple(columns: List<DataGridColumn>, rows: List<Map<String, Any>>) =
            DataGridComponent(
                columns = columns,
                rows = rows,
                sortable = false,
                filterable = false,
                paginated = false,
                selectable = false,
                resizable = false
            )

        /**
         * Creates a paginated data grid.
         */
        fun paginated(
            columns: List<DataGridColumn>,
            rows: List<Map<String, Any>>,
            pageSize: Int = 10
        ) = DataGridComponent(
            columns = columns,
            rows = rows,
            paginated = true,
            pageSize = pageSize
        )
    }
}

/**
 * Column definition for DataGrid.
 *
 * @property key Unique column identifier (data key)
 * @property label Display label for column header
 * @property width Column width (optional, auto-size if null)
 * @property sortable Whether this column can be sorted (default true)
 * @property filterable Whether this column can be filtered (default true)
 * @property resizable Whether this column can be resized (default true)
 */
data class DataGridColumn(
    val key: String,
    val label: String,
    val width: Float? = null,
    val sortable: Boolean = true,
    val filterable: Boolean = true,
    val resizable: Boolean = true
) {
    init {
        require(key.isNotBlank()) { "Column key cannot be blank" }
        require(label.isNotBlank()) { "Column label cannot be blank" }
        if (width != null) {
            require(width > 0) { "Width must be positive (got $width)" }
        }
    }

    companion object {
        /**
         * Creates a fixed-width column.
         */
        fun fixed(key: String, label: String, width: Float) =
            DataGridColumn(key, label, width, resizable = false)

        /**
         * Creates a read-only column (no sorting/filtering).
         */
        fun readOnly(key: String, label: String) =
            DataGridColumn(key, label, sortable = false, filterable = false, resizable = false)
    }
}
