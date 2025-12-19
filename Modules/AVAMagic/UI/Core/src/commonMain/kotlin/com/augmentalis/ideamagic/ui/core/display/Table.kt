package com.augmentalis.avanues.avamagic.ui.core.display

import com.augmentalis.avanues.avamagic.components.core.*

/**
 * Simple table for displaying tabular data.
 *
 * Table provides a straightforward way to display data in rows and columns
 * without the advanced features of DataGrid.
 *
 * ## Usage Examples
 * ```kotlin
 * // Basic table
 * val table = TableComponent(
 *     headers = listOf("Name", "Age", "City"),
 *     rows = listOf(
 *         listOf("Alice", "30", "New York"),
 *         listOf("Bob", "25", "Boston"),
 *         listOf("Charlie", "35", "Chicago")
 *     )
 * )
 *
 * // With styling
 * val table = TableComponent(
 *     headers = headers,
 *     rows = rows,
 *     striped = true,
 *     bordered = true,
 *     hoverable = true
 * )
 *
 * // Without headers
 * val table = TableComponent(
 *     rows = rows,
 *     showHeaders = false
 * )
 *
 * // Compact size
 * val table = TableComponent(
 *     headers = headers,
 *     rows = rows,
 *     size = ComponentSize.SM
 * )
 * ```
 *
 * @property headers Column header labels
 * @property rows Table rows (list of cell values)
 * @property showHeaders Whether to display headers (default true)
 * @property striped Alternating row colors (default false)
 * @property bordered Show borders (default true)
 * @property hoverable Highlight row on hover (default false)
 * @property size Table size (default MD)
 * @since 1.0.0
 */
data class TableComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val headers: List<String> = emptyList(),
    val rows: List<List<String>>,
    val showHeaders: Boolean = true,
    val striped: Boolean = false,
    val bordered: Boolean = true,
    val hoverable: Boolean = false,
    val size: ComponentSize = ComponentSize.MD
) : Component {
    init {
        require(rows.isNotEmpty()) { "Rows cannot be empty" }
        if (headers.isNotEmpty()) {
            val expectedColumns = headers.size
            rows.forEachIndexed { index, row ->
                require(row.size == expectedColumns) {
                    "Row $index has ${row.size} columns, expected $expectedColumns"
                }
            }
        }
        if (showHeaders) {
            require(headers.isNotEmpty()) { "Headers cannot be empty when showHeaders is true" }
        }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    /**
     * Number of columns in the table.
     */
    val columnCount: Int
        get() = if (headers.isNotEmpty()) headers.size else (rows.firstOrNull()?.size ?: 0)

    /**
     * Number of rows in the table.
     */
    val rowCount: Int
        get() = rows.size

    /**
     * Adds a row to the table.
     */
    fun addRow(row: List<String>): TableComponent {
        require(row.size == columnCount) {
            "Row has ${row.size} columns, expected $columnCount"
        }
        return copy(rows = rows + listOf(row))
    }

    /**
     * Removes a row at the given index.
     */
    fun removeRow(index: Int): TableComponent {
        require(index in rows.indices) { "Index $index out of bounds" }
        return copy(rows = rows.filterIndexed { i, _ -> i != index })
    }

    /**
     * Gets a specific cell value.
     */
    fun getCell(rowIndex: Int, columnIndex: Int): String {
        require(rowIndex in rows.indices) { "Row index $rowIndex out of bounds" }
        require(columnIndex in 0 until columnCount) { "Column index $columnIndex out of bounds" }
        return rows[rowIndex][columnIndex]
    }

    /**
     * Updates a specific cell value.
     */
    fun setCell(rowIndex: Int, columnIndex: Int, value: String): TableComponent {
        require(rowIndex in rows.indices) { "Row index $rowIndex out of bounds" }
        require(columnIndex in 0 until columnCount) { "Column index $columnIndex out of bounds" }
        val newRows = rows.mapIndexed { i, row ->
            if (i == rowIndex) {
                row.mapIndexed { j, cell ->
                    if (j == columnIndex) value else cell
                }
            } else {
                row
            }
        }
        return copy(rows = newRows)
    }

    /**
     * Gets an entire row.
     */
    fun getRow(index: Int): List<String> {
        require(index in rows.indices) { "Index $index out of bounds" }
        return rows[index]
    }

    /**
     * Gets an entire column.
     */
    fun getColumn(index: Int): List<String> {
        require(index in 0 until columnCount) { "Index $index out of bounds" }
        return rows.map { it[index] }
    }

    companion object {
        /**
         * Creates a simple table with minimal styling.
         */
        fun simple(headers: List<String>, rows: List<List<String>>) =
            TableComponent(
                headers = headers,
                rows = rows,
                striped = false,
                bordered = false,
                hoverable = false
            )

        /**
         * Creates a styled table with striped rows and hover effect.
         */
        fun styled(headers: List<String>, rows: List<List<String>>) =
            TableComponent(
                headers = headers,
                rows = rows,
                striped = true,
                bordered = true,
                hoverable = true
            )

        /**
         * Creates a compact table.
         */
        fun compact(headers: List<String>, rows: List<List<String>>) =
            TableComponent(
                headers = headers,
                rows = rows,
                size = ComponentSize.SM
            )

        /**
         * Creates a table without headers.
         */
        fun noHeaders(rows: List<List<String>>) =
            TableComponent(
                rows = rows,
                showHeaders = false
            )

        /**
         * Creates a CSV-style table from string data.
         */
        fun fromCSV(csvData: String, delimiter: String = ","): TableComponent {
            val lines = csvData.trim().lines().filter { it.isNotBlank() }
            require(lines.isNotEmpty()) { "CSV data cannot be empty" }

            val headers = lines.first().split(delimiter).map { it.trim() }
            val rows = lines.drop(1).map { line ->
                line.split(delimiter).map { it.trim() }
            }

            return TableComponent(headers = headers, rows = rows)
        }
    }
}
