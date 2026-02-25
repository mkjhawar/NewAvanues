package com.augmentalis.avanueui.ui.core.data

import com.augmentalis.avanueui.core.*

/**
 * Table Component
 *
 * A simple table component for displaying data in rows and columns.
 * Provides basic table functionality with styling options for a clean, organized data presentation.
 * Simpler alternative to DataTable for basic tabular data without advanced features.
 *
 * Features:
 * - Headers (column names)
 * - Row data (simple list of list of strings)
 * - Striped rows (alternating background colors)
 * - Bordered table (cell and table borders)
 * - Hoverable rows (highlight on hover)
 * - Responsive design
 * - Accessibility support
 *
 * Platform mappings:
 * - Android: TableLayout or custom Row/Column layout
 * - iOS: UITableView with static cells or custom grid view
 * - Web: HTML table element
 *
 * Usage:
 * ```kotlin
 * // Simple table
 * TableComponent(
 *     headers = listOf("Name", "Email", "Role"),
 *     rows = listOf(
 *         listOf("John Doe", "john@example.com", "Developer"),
 *         listOf("Jane Smith", "jane@example.com", "Designer"),
 *         listOf("Bob Johnson", "bob@example.com", "Manager")
 *     )
 * )
 *
 * // Styled table with options
 * TableComponent(
 *     headers = listOf("Product", "Price", "Stock"),
 *     rows = products.map { listOf(it.name, it.price.toString(), it.stock.toString()) },
 *     striped = true,
 *     bordered = true,
 *     hoverable = true
 * )
 * ```
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
data class TableComponent(
    /**
     * Column headers for the table.
     * Each string represents a column header label.
     *
     * @see rows
     */
    val headers: List<String>,

    /**
     * Table row data.
     * Each inner list represents a row with cells matching the header count.
     * All rows should have the same number of cells as there are headers.
     *
     * @see headers
     */
    val rows: List<List<String>>,

    /**
     * Enable striped rows (alternating background colors).
     * Improves readability of data by distinguishing rows visually.
     *
     * Default: false
     */
    val striped: Boolean = false,

    /**
     * Enable borders around cells and the table.
     * When true, applies borders to all cells and the table perimeter.
     * When false, borders are minimized for a cleaner look.
     *
     * Default: false
     */
    val bordered: Boolean = false,

    /**
     * Enable row hover effect.
     * When true, hovering over a row highlights it.
     * Improves interactivity and user feedback.
     *
     * Default: false
     */
    val hoverable: Boolean = false,

    /**
     * Optional unique identifier for this component.
     * Used for tracking, testing, and state management.
     *
     * Default: null
     */
    val id: String? = null,

    /**
     * Optional styling configuration for this component.
     * Allows custom styling of the table appearance.
     *
     * Default: null
     */
    val style: Any? = null,

    /**
     * List of modifiers applied to this component.
     * Supports behaviors such as click handling, accessibility, and animations.
     *
     * Default: empty list
     */
    val modifiers: List<Any> = emptyList()
) {
    init {
        // Validation: headers must not be empty
        require(headers.isNotEmpty()) { "Table must have at least one header" }

        // Validation: all rows must have same number of cells as headers
        rows.forEach { row ->
            require(row.size == headers.size) {
                "All rows must have ${headers.size} cells to match headers, but got ${row.size}"
            }
        }
    }

    companion object {
        /**
         * Creates a simple table from a list of string pairs.
         *
         * Useful for displaying key-value data.
         *
         * @param headers The column headers
         * @param data Pairs of values for each row
         * @return A new TableComponent
         *
         * Example:
         * ```kotlin
         * val table = TableComponent.fromPairs(
         *     headers = listOf("Key", "Value"),
         *     data = listOf(
         *         "Name" to "John Doe",
         *         "Email" to "john@example.com",
         *         "Role" to "Developer"
         *     )
         * )
         * ```
         */
        fun fromPairs(
            headers: List<String>,
            data: List<Pair<String, String>>
        ): TableComponent {
            require(headers.size == 2) { "fromPairs expects exactly 2 headers" }
            val rows = data.map { (key, value) -> listOf(key, value) }
            return TableComponent(headers = headers, rows = rows)
        }

        /**
         * Creates a simple striped and bordered table.
         *
         * Applies default styling suitable for most use cases.
         *
         * @param headers The column headers
         * @param rows The table rows
         * @return A new TableComponent with striped and bordered styles
         *
         * Example:
         * ```kotlin
         * val table = TableComponent.styled(
         *     headers = listOf("Name", "Email"),
         *     rows = listOf(listOf("John", "john@example.com"))
         * )
         * ```
         */
        fun styled(
            headers: List<String>,
            rows: List<List<String>>
        ): TableComponent {
            return TableComponent(
                headers = headers,
                rows = rows,
                striped = true,
                bordered = true,
                hoverable = true
            )
        }

        /**
         * Creates a basic minimal table without styling.
         *
         * @param headers The column headers
         * @param rows The table rows
         * @return A new TableComponent with minimal styling
         *
         * Example:
         * ```kotlin
         * val table = TableComponent.minimal(
         *     headers = listOf("Name", "Email"),
         *     rows = listOf(listOf("John", "john@example.com"))
         * )
         * ```
         */
        fun minimal(
            headers: List<String>,
            rows: List<List<String>>
        ): TableComponent {
            return TableComponent(
                headers = headers,
                rows = rows,
                striped = false,
                bordered = false,
                hoverable = false
            )
        }

        /**
         * Creates an empty table with headers only.
         *
         * Useful for displaying a table structure before data is loaded.
         *
         * @param headers The column headers
         * @return A new TableComponent with no data rows
         *
         * Example:
         * ```kotlin
         * val table = TableComponent.empty(
         *     headers = listOf("Name", "Email", "Status")
         * )
         * ```
         */
        fun empty(headers: List<String>): TableComponent {
            return TableComponent(
                headers = headers,
                rows = emptyList()
            )
        }

        /**
         * Creates a table from a list of objects with a transformation function.
         *
         * Converts objects to string representations for table display.
         *
         * @param headers The column headers
         * @param data The list of objects to display
         * @param transform Function to transform each object to a list of strings
         * @return A new TableComponent
         *
         * Example:
         * ```kotlin
         * data class User(val name: String, val email: String, val role: String)
         *
         * val users = listOf(
         *     User("John Doe", "john@example.com", "Developer"),
         *     User("Jane Smith", "jane@example.com", "Designer")
         * )
         *
         * val table = TableComponent.from(
         *     headers = listOf("Name", "Email", "Role"),
         *     data = users,
         *     transform = { user -> listOf(user.name, user.email, user.role) }
         * )
         * ```
         */
        fun <T> from(
            headers: List<String>,
            data: List<T>,
            transform: (T) -> List<String>
        ): TableComponent {
            val rows = data.map(transform)
            return TableComponent(headers = headers, rows = rows)
        }
    }
}
