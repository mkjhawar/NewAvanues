/**
 * TableHandler.kt
 *
 * Created: 2026-01-27 00:00 PST
 * Last Modified: 2026-01-27 00:00 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Voice command handler for DataGrid/Table navigation and manipulation
 * Features: Row navigation, cell selection, sorting, filtering, expand/collapse
 * Location: CommandManager module
 *
 * Changelog:
 * - v1.0.0 (2026-01-27): Initial implementation with table navigation commands
 */

package com.augmentalis.avamagic.voice.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandHandler
import com.augmentalis.commandmanager.CommandRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Voice command handler for DataGrid/Table components
 *
 * Provides voice control for:
 * - Row navigation (next/previous/first/last/select specific)
 * - Cell selection
 * - Sorting by column
 * - Filtering by column value
 * - Expand/collapse for expandable rows
 *
 * Design:
 * - Command parsing and routing only (no table logic)
 * - Uses accessibility APIs to interact with table components
 * - Implements CommandHandler for CommandRegistry integration
 * - Thread-safe with managed coroutine scope
 */
class TableHandler private constructor(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "TableHandler"
        private const val MODULE_ID = "table"

        @Volatile
        private var instance: TableHandler? = null

        @Volatile
        private var accessibilityService: AccessibilityService? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): TableHandler {
            return instance ?: synchronized(this) {
                instance ?: TableHandler(context.applicationContext).also {
                    instance = it
                }
            }
        }

        /**
         * Set accessibility service reference for table operations
         */
        fun setAccessibilityService(service: AccessibilityService?) {
            accessibilityService = service
        }

        // Command prefixes
        private const val TABLE_PREFIX = "table"
        private const val GRID_PREFIX = "grid"

        // Word number mappings for voice recognition
        private val WORD_TO_NUMBER = mapOf(
            "one" to 1, "first" to 1, "1st" to 1,
            "two" to 2, "second" to 2, "2nd" to 2,
            "three" to 3, "third" to 3, "3rd" to 3,
            "four" to 4, "fourth" to 4, "4th" to 4,
            "five" to 5, "fifth" to 5, "5th" to 5,
            "six" to 6, "sixth" to 6, "6th" to 6,
            "seven" to 7, "seventh" to 7, "7th" to 7,
            "eight" to 8, "eighth" to 8, "8th" to 8,
            "nine" to 9, "ninth" to 9, "9th" to 9,
            "ten" to 10, "tenth" to 10, "10th" to 10,
            "eleven" to 11, "eleventh" to 11, "11th" to 11,
            "twelve" to 12, "twelfth" to 12, "12th" to 12
        )
    }

    // CommandHandler interface implementation
    override val moduleId: String = MODULE_ID

    override val supportedCommands: List<String> = listOf(
        // Row selection commands
        "select row [N]",
        "go to row [N]",

        // Row navigation commands
        "next row",
        "previous row",
        "first row",
        "last row",

        // Sorting commands
        "sort by [column]",
        "sort by [column] ascending",
        "sort by [column] descending",
        "sort [column] ascending",
        "sort [column] descending",

        // Filtering commands
        "filter [column] by [value]",
        "filter by [column] [value]",
        "clear filter",
        "clear filters",
        "remove filter",
        "remove filters",

        // Expand/collapse commands
        "expand row",
        "collapse row",
        "expand all",
        "collapse all",
        "toggle row",

        // Cell selection commands
        "select cell [row] [column]",
        "go to cell [row] [column]",

        // Table navigation with prefix
        "table next row",
        "table previous row",
        "table first row",
        "table last row",
        "grid next row",
        "grid previous row",
        "grid first row",
        "grid last row"
    )

    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State tracking
    private var isInitialized = false
    private var currentRowIndex = 0
    private var currentColumnIndex = 0

    init {
        initialize()
        CommandRegistry.registerHandler(moduleId, this)
    }

    /**
     * Initialize table handler
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return try {
            isInitialized = true
            Log.d(TAG, "TableHandler initialized with ${supportedCommands.size} commands")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }

    /**
     * CommandHandler interface: Check if this handler can process the command
     */
    override fun canHandle(command: String): Boolean {
        return when {
            command.startsWith(TABLE_PREFIX) -> true
            command.startsWith(GRID_PREFIX) -> true
            isTableNavigationCommand(command) -> true
            isSortCommand(command) -> true
            isFilterCommand(command) -> true
            isExpandCollapseCommand(command) -> true
            isCellSelectionCommand(command) -> true
            else -> false
        }
    }

    /**
     * CommandHandler interface: Execute the command
     */
    override suspend fun handleCommand(command: String): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized for command processing")
            return false
        }

        Log.d(TAG, "Processing table command: '$command'")

        return try {
            when {
                // Row selection with number
                command.matches(Regex("(select|go to) row .*")) -> {
                    val rowNumber = extractNumber(command)
                    if (rowNumber != null) selectRow(rowNumber) else false
                }

                // Row navigation
                command.contains("next row") -> navigateToNextRow()
                command.contains("previous row") -> navigateToPreviousRow()
                command.contains("first row") -> navigateToFirstRow()
                command.contains("last row") -> navigateToLastRow()

                // Sorting
                isSortCommand(command) -> processSortCommand(command)

                // Filtering
                isFilterCommand(command) -> processFilterCommand(command)

                // Expand/collapse
                command.contains("expand all") -> expandAllRows()
                command.contains("collapse all") -> collapseAllRows()
                command.contains("expand row") -> expandCurrentRow()
                command.contains("collapse row") -> collapseCurrentRow()
                command.contains("toggle row") -> toggleCurrentRow()

                // Cell selection
                isCellSelectionCommand(command) -> processCellSelectionCommand(command)

                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $command", e)
            false
        }
    }

    // ============================================================================
    // Row Navigation Methods
    // ============================================================================

    /**
     * Select a specific row by number
     */
    private fun selectRow(rowNumber: Int): Boolean {
        Log.d(TAG, "Selecting row $rowNumber")

        val service = accessibilityService ?: run {
            Log.w(TAG, "Accessibility service not available")
            return false
        }

        val tableNode = findTableNode(service.rootInActiveWindow) ?: run {
            Log.w(TAG, "No table/grid found on screen")
            return false
        }

        val row = findRowByIndex(tableNode, rowNumber - 1) // Convert to 0-indexed
        return if (row != null) {
            val result = performRowSelection(row)
            if (result) {
                currentRowIndex = rowNumber - 1
                Log.d(TAG, "Successfully selected row $rowNumber")
            }
            row.recycle()
            result
        } else {
            Log.w(TAG, "Row $rowNumber not found")
            false
        }.also {
            tableNode.recycle()
        }
    }

    /**
     * Navigate to the next row
     */
    private fun navigateToNextRow(): Boolean {
        Log.d(TAG, "Navigating to next row")

        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        val nextRow = findRowByIndex(tableNode, currentRowIndex + 1)
        return if (nextRow != null) {
            val result = performRowSelection(nextRow)
            if (result) {
                currentRowIndex++
                Log.d(TAG, "Moved to row ${currentRowIndex + 1}")
            }
            nextRow.recycle()
            result
        } else {
            // Try scrolling to reveal more rows
            val scrolled = scrollTableDown(tableNode)
            if (scrolled) {
                // Retry after scroll
                val retryRow = findRowByIndex(tableNode, currentRowIndex + 1)
                if (retryRow != null) {
                    val result = performRowSelection(retryRow)
                    if (result) currentRowIndex++
                    retryRow.recycle()
                    result
                } else false
            } else {
                Log.d(TAG, "Already at last row")
                false
            }
        }.also {
            tableNode.recycle()
        }
    }

    /**
     * Navigate to the previous row
     */
    private fun navigateToPreviousRow(): Boolean {
        Log.d(TAG, "Navigating to previous row")

        if (currentRowIndex <= 0) {
            Log.d(TAG, "Already at first row")
            return false
        }

        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        val prevRow = findRowByIndex(tableNode, currentRowIndex - 1)
        return if (prevRow != null) {
            val result = performRowSelection(prevRow)
            if (result) {
                currentRowIndex--
                Log.d(TAG, "Moved to row ${currentRowIndex + 1}")
            }
            prevRow.recycle()
            result
        } else {
            // Try scrolling up to reveal previous rows
            val scrolled = scrollTableUp(tableNode)
            if (scrolled) {
                val retryRow = findRowByIndex(tableNode, currentRowIndex - 1)
                if (retryRow != null) {
                    val result = performRowSelection(retryRow)
                    if (result) currentRowIndex--
                    retryRow.recycle()
                    result
                } else false
            } else false
        }.also {
            tableNode.recycle()
        }
    }

    /**
     * Navigate to the first row
     */
    private fun navigateToFirstRow(): Boolean {
        Log.d(TAG, "Navigating to first row")

        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        // Scroll to top first
        var scrollAttempts = 0
        while (scrollTableUp(tableNode) && scrollAttempts < 10) {
            scrollAttempts++
        }

        val firstRow = findRowByIndex(tableNode, 0)
        return if (firstRow != null) {
            val result = performRowSelection(firstRow)
            if (result) {
                currentRowIndex = 0
                Log.d(TAG, "Moved to first row")
            }
            firstRow.recycle()
            result
        } else false.also {
            tableNode.recycle()
        }
    }

    /**
     * Navigate to the last row
     */
    private fun navigateToLastRow(): Boolean {
        Log.d(TAG, "Navigating to last row")

        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        // Scroll to bottom first
        var scrollAttempts = 0
        while (scrollTableDown(tableNode) && scrollAttempts < 50) {
            scrollAttempts++
        }

        // Find the last visible row
        val rows = findAllRows(tableNode)
        return if (rows.isNotEmpty()) {
            val lastRow = rows.last()
            val result = performRowSelection(lastRow)
            if (result) {
                currentRowIndex = rows.size - 1
                Log.d(TAG, "Moved to last row (${currentRowIndex + 1})")
            }
            rows.forEach { it.recycle() }
            result
        } else false.also {
            tableNode.recycle()
        }
    }

    // ============================================================================
    // Sorting Methods
    // ============================================================================

    /**
     * Process sort command
     */
    private fun processSortCommand(command: String): Boolean {
        val ascending = !command.contains("descending")
        val columnName = extractColumnName(command, listOf("sort by", "sort"))

        if (columnName.isNullOrBlank()) {
            Log.w(TAG, "Could not extract column name from sort command")
            return false
        }

        Log.d(TAG, "Sorting by column '$columnName' ${if (ascending) "ascending" else "descending"}")
        return sortByColumn(columnName, ascending)
    }

    /**
     * Sort table by column name
     */
    private fun sortByColumn(columnName: String, ascending: Boolean): Boolean {
        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        // Find header row and column header
        val headerNode = findColumnHeader(tableNode, columnName)
        return if (headerNode != null) {
            // Click header to sort (most tables toggle sort on header click)
            val result = performClick(headerNode)

            // If descending and currently ascending, click again
            if (result && !ascending) {
                Thread.sleep(200) // Brief delay for UI update
                performClick(headerNode)
            }

            Log.d(TAG, "Sort by '$columnName' ${if (ascending) "ascending" else "descending"}: $result")
            headerNode.recycle()
            result
        } else {
            Log.w(TAG, "Column header '$columnName' not found")
            false
        }.also {
            tableNode.recycle()
        }
    }

    // ============================================================================
    // Filtering Methods
    // ============================================================================

    /**
     * Process filter command
     */
    private fun processFilterCommand(command: String): Boolean {
        return when {
            command.contains("clear filter") || command.contains("remove filter") -> {
                clearFilters()
            }
            else -> {
                val (columnName, filterValue) = extractFilterParameters(command)
                if (columnName != null && filterValue != null) {
                    filterColumn(columnName, filterValue)
                } else {
                    Log.w(TAG, "Could not extract filter parameters from command")
                    false
                }
            }
        }
    }

    /**
     * Filter column by value
     */
    private fun filterColumn(columnName: String, value: String): Boolean {
        Log.d(TAG, "Filtering column '$columnName' by '$value'")

        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        // Look for filter input associated with column
        val filterInput = findFilterInput(tableNode, columnName)
        return if (filterInput != null) {
            // Clear existing text and enter new filter value
            filterInput.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
            filterInput.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
            val result = filterInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            Log.d(TAG, "Filter applied to '$columnName': $result")
            filterInput.recycle()
            result
        } else {
            // Try clicking column header for filter dropdown
            val headerNode = findColumnHeader(tableNode, columnName)
            if (headerNode != null) {
                val filterButton = findFilterButton(headerNode)
                val result = if (filterButton != null) {
                    performClick(filterButton).also { filterButton.recycle() }
                } else {
                    // Long press might open filter menu
                    performLongClick(headerNode)
                }
                headerNode.recycle()
                result
            } else {
                Log.w(TAG, "Could not find filter controls for column '$columnName'")
                false
            }
        }.also {
            tableNode.recycle()
        }
    }

    /**
     * Clear all filters
     */
    private fun clearFilters(): Boolean {
        Log.d(TAG, "Clearing all filters")

        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        // Look for clear filter button
        val clearButton = findNodeByText(rootNode, "clear filter")
            ?: findNodeByText(rootNode, "clear filters")
            ?: findNodeByText(rootNode, "reset")
            ?: findNodeByText(rootNode, "clear all")

        return if (clearButton != null) {
            val result = performClick(clearButton)
            Log.d(TAG, "Filters cleared: $result")
            clearButton.recycle()
            result
        } else {
            // Try to clear individual filter inputs
            val tableNode = findTableNode(rootNode)
            if (tableNode != null) {
                val cleared = clearAllFilterInputs(tableNode)
                tableNode.recycle()
                cleared
            } else false
        }.also {
            rootNode.recycle()
        }
    }

    // ============================================================================
    // Expand/Collapse Methods
    // ============================================================================

    /**
     * Expand current row
     */
    private fun expandCurrentRow(): Boolean {
        Log.d(TAG, "Expanding current row")
        return toggleRowExpansion(expand = true)
    }

    /**
     * Collapse current row
     */
    private fun collapseCurrentRow(): Boolean {
        Log.d(TAG, "Collapsing current row")
        return toggleRowExpansion(expand = false)
    }

    /**
     * Toggle current row expansion
     */
    private fun toggleCurrentRow(): Boolean {
        Log.d(TAG, "Toggling current row expansion")
        return toggleRowExpansion(expand = null)
    }

    /**
     * Toggle row expansion state
     */
    private fun toggleRowExpansion(expand: Boolean?): Boolean {
        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        val currentRow = findRowByIndex(tableNode, currentRowIndex)
        return if (currentRow != null) {
            val expandButton = findExpandCollapseButton(currentRow)
            val result = if (expandButton != null) {
                when (expand) {
                    true -> {
                        if (!isExpanded(currentRow)) performClick(expandButton) else true
                    }
                    false -> {
                        if (isExpanded(currentRow)) performClick(expandButton) else true
                    }
                    null -> performClick(expandButton)
                }.also { expandButton.recycle() }
            } else {
                // Try expand/collapse accessibility action
                when (expand) {
                    true -> currentRow.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
                    false -> currentRow.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
                    null -> {
                        if (isExpanded(currentRow)) {
                            currentRow.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
                        } else {
                            currentRow.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
                        }
                    }
                }
            }
            currentRow.recycle()
            result
        } else false.also {
            tableNode.recycle()
        }
    }

    /**
     * Expand all rows
     */
    private fun expandAllRows(): Boolean {
        Log.d(TAG, "Expanding all rows")

        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        // Look for "expand all" button first
        val expandAllButton = findNodeByText(tableNode, "expand all")
        return if (expandAllButton != null) {
            val result = performClick(expandAllButton)
            expandAllButton.recycle()
            result
        } else {
            // Expand each row individually
            val rows = findAllRows(tableNode)
            var successCount = 0
            rows.forEach { row ->
                if (!isExpanded(row)) {
                    val expandButton = findExpandCollapseButton(row)
                    if (expandButton != null) {
                        if (performClick(expandButton)) successCount++
                        expandButton.recycle()
                    } else if (row.performAction(AccessibilityNodeInfo.ACTION_EXPAND)) {
                        successCount++
                    }
                }
                row.recycle()
            }
            Log.d(TAG, "Expanded $successCount rows")
            successCount > 0
        }.also {
            tableNode.recycle()
        }
    }

    /**
     * Collapse all rows
     */
    private fun collapseAllRows(): Boolean {
        Log.d(TAG, "Collapsing all rows")

        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        // Look for "collapse all" button first
        val collapseAllButton = findNodeByText(tableNode, "collapse all")
        return if (collapseAllButton != null) {
            val result = performClick(collapseAllButton)
            collapseAllButton.recycle()
            result
        } else {
            // Collapse each row individually
            val rows = findAllRows(tableNode)
            var successCount = 0
            rows.forEach { row ->
                if (isExpanded(row)) {
                    val collapseButton = findExpandCollapseButton(row)
                    if (collapseButton != null) {
                        if (performClick(collapseButton)) successCount++
                        collapseButton.recycle()
                    } else if (row.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)) {
                        successCount++
                    }
                }
                row.recycle()
            }
            Log.d(TAG, "Collapsed $successCount rows")
            successCount > 0
        }.also {
            tableNode.recycle()
        }
    }

    // ============================================================================
    // Cell Selection Methods
    // ============================================================================

    /**
     * Process cell selection command
     */
    private fun processCellSelectionCommand(command: String): Boolean {
        val numbers = extractMultipleNumbers(command)
        return if (numbers.size >= 2) {
            selectCell(numbers[0], numbers[1])
        } else {
            Log.w(TAG, "Could not extract row and column from cell selection command")
            false
        }
    }

    /**
     * Select specific cell by row and column
     */
    private fun selectCell(row: Int, column: Int): Boolean {
        Log.d(TAG, "Selecting cell at row $row, column $column")

        val service = accessibilityService ?: return false
        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false

        val rowNode = findRowByIndex(tableNode, row - 1) // Convert to 0-indexed
        return if (rowNode != null) {
            val cellNode = findCellByIndex(rowNode, column - 1)
            if (cellNode != null) {
                val result = performCellSelection(cellNode)
                if (result) {
                    currentRowIndex = row - 1
                    currentColumnIndex = column - 1
                    Log.d(TAG, "Successfully selected cell at row $row, column $column")
                }
                cellNode.recycle()
                result
            } else {
                Log.w(TAG, "Cell at column $column not found in row $row")
                false
            }.also {
                rowNode.recycle()
            }
        } else {
            Log.w(TAG, "Row $row not found")
            false
        }.also {
            tableNode.recycle()
        }
    }

    // ============================================================================
    // Helper Methods - Node Finding
    // ============================================================================

    /**
     * Find table or grid node in the accessibility tree
     */
    private fun findTableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null

        // Check if current node is a table/grid
        val className = root.className?.toString() ?: ""
        if (isTableClass(className)) {
            return AccessibilityNodeInfo.obtain(root)
        }

        // Check by content description for table-like elements
        val contentDesc = root.contentDescription?.toString()?.lowercase() ?: ""
        if (contentDesc.contains("table") || contentDesc.contains("grid") || contentDesc.contains("list")) {
            return AccessibilityNodeInfo.obtain(root)
        }

        // Search children recursively
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findTableNode(child)
            child.recycle()
            if (found != null) return found
        }

        return null
    }

    /**
     * Check if class name represents a table/grid component
     */
    private fun isTableClass(className: String): Boolean {
        val tablePatterns = listOf(
            "recyclerview", "listview", "gridview", "tableview",
            "datagrid", "table", "grid", "lazygrid", "lazycolumn",
            "lazyrow", "lazyverticalgrid", "horizontalgrid"
        )
        val lowerClassName = className.lowercase()
        return tablePatterns.any { lowerClassName.contains(it) }
    }

    /**
     * Find row by index
     */
    private fun findRowByIndex(tableNode: AccessibilityNodeInfo, index: Int): AccessibilityNodeInfo? {
        val rows = findAllRows(tableNode)
        return if (index in rows.indices) {
            val targetRow = AccessibilityNodeInfo.obtain(rows[index])
            rows.forEachIndexed { i, row ->
                if (i != index) row.recycle()
            }
            targetRow
        } else {
            rows.forEach { it.recycle() }
            null
        }
    }

    /**
     * Find all rows in table
     */
    private fun findAllRows(tableNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val rows = mutableListOf<AccessibilityNodeInfo>()

        for (i in 0 until tableNode.childCount) {
            val child = tableNode.getChild(i) ?: continue
            val className = child.className?.toString()?.lowercase() ?: ""

            // Skip header rows
            if (className.contains("header")) {
                child.recycle()
                continue
            }

            // Check if this is a row-like element
            if (isRowElement(child)) {
                rows.add(child)
            } else {
                child.recycle()
            }
        }

        return rows
    }

    /**
     * Check if node is a row element
     */
    private fun isRowElement(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString()?.lowercase() ?: ""
        val rowPatterns = listOf(
            "tablerow", "row", "item", "cell", "linearlayout",
            "relativelayout", "constraintlayout", "framelayout"
        )
        return rowPatterns.any { className.contains(it) } || node.childCount > 0
    }

    /**
     * Find cell by index within a row
     */
    private fun findCellByIndex(rowNode: AccessibilityNodeInfo, index: Int): AccessibilityNodeInfo? {
        if (index < 0 || index >= rowNode.childCount) return null
        return rowNode.getChild(index)
    }

    /**
     * Find column header by name
     */
    private fun findColumnHeader(tableNode: AccessibilityNodeInfo, columnName: String): AccessibilityNodeInfo? {
        val normalizedName = columnName.lowercase().trim()

        // Search for header row first
        for (i in 0 until tableNode.childCount) {
            val child = tableNode.getChild(i) ?: continue
            val className = child.className?.toString()?.lowercase() ?: ""

            if (className.contains("header") || i == 0) {
                // Search within header for column name
                val header = findNodeByText(child, normalizedName)
                if (header != null) {
                    child.recycle()
                    return header
                }
            }
            child.recycle()
        }

        // Fallback: search entire table for column header
        return findNodeByText(tableNode, normalizedName)
    }

    /**
     * Find node by text content
     */
    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val normalizedText = text.lowercase()

        // Check current node
        val nodeText = root.text?.toString()?.lowercase() ?: ""
        val contentDesc = root.contentDescription?.toString()?.lowercase() ?: ""

        if (nodeText.contains(normalizedText) || contentDesc.contains(normalizedText)) {
            return AccessibilityNodeInfo.obtain(root)
        }

        // Search children
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            child.recycle()
            if (found != null) return found
        }

        return null
    }

    /**
     * Find filter input for column
     */
    private fun findFilterInput(tableNode: AccessibilityNodeInfo, columnName: String): AccessibilityNodeInfo? {
        // Look for editable field near column header
        val header = findColumnHeader(tableNode, columnName) ?: return null

        // Check siblings and nearby nodes for input field
        val parent = header.parent
        header.recycle()

        if (parent != null) {
            for (i in 0 until parent.childCount) {
                val sibling = parent.getChild(i) ?: continue
                if (sibling.isEditable) {
                    parent.recycle()
                    return sibling
                }
                sibling.recycle()
            }
            parent.recycle()
        }

        return null
    }

    /**
     * Find filter button near header
     */
    private fun findFilterButton(headerNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for filter icon/button within or near header
        val filterKeywords = listOf("filter", "funnel", "dropdown", "menu")

        for (i in 0 until headerNode.childCount) {
            val child = headerNode.getChild(i) ?: continue
            val contentDesc = child.contentDescription?.toString()?.lowercase() ?: ""

            if (filterKeywords.any { contentDesc.contains(it) } || child.isClickable) {
                return child
            }
            child.recycle()
        }

        return null
    }

    /**
     * Find expand/collapse button in row
     */
    private fun findExpandCollapseButton(rowNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val expandKeywords = listOf("expand", "collapse", "arrow", "chevron", "toggle", "more", "details")

        for (i in 0 until rowNode.childCount) {
            val child = rowNode.getChild(i) ?: continue
            val contentDesc = child.contentDescription?.toString()?.lowercase() ?: ""
            val className = child.className?.toString()?.lowercase() ?: ""

            if (expandKeywords.any { contentDesc.contains(it) || className.contains(it) }) {
                return child
            }

            // Check if it's an image button (often used for expand/collapse)
            if (className.contains("imagebutton") || className.contains("imageview")) {
                if (child.isClickable) return child
            }

            child.recycle()
        }

        return null
    }

    /**
     * Check if row is expanded
     */
    private fun isExpanded(rowNode: AccessibilityNodeInfo): Boolean {
        // Check if collapse action is available (indicates expanded state)
        val actionList = rowNode.actionList ?: emptyList()
        if (actionList.any { it.id == AccessibilityNodeInfo.ACTION_COLLAPSE }) return true

        // Check content description for expanded indicators
        val contentDesc = rowNode.contentDescription?.toString()?.lowercase() ?: ""
        if (contentDesc.contains("expanded") || contentDesc.contains("open")) return true

        // Check for aria-expanded equivalent
        val extras = rowNode.extras
        if (extras.containsKey("expanded")) {
            return extras.getBoolean("expanded", false)
        }

        return false
    }

    /**
     * Clear all filter inputs in table
     */
    private fun clearAllFilterInputs(tableNode: AccessibilityNodeInfo): Boolean {
        var cleared = false

        fun clearEditableNodes(node: AccessibilityNodeInfo) {
            if (node.isEditable && !node.text.isNullOrEmpty()) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                }
                if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                    cleared = true
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                clearEditableNodes(child)
                child.recycle()
            }
        }

        clearEditableNodes(tableNode)
        return cleared
    }

    // ============================================================================
    // Helper Methods - Actions
    // ============================================================================

    /**
     * Perform row selection
     */
    private fun performRowSelection(row: AccessibilityNodeInfo): Boolean {
        // Try selection action first
        if (row.performAction(AccessibilityNodeInfo.ACTION_SELECT)) {
            return true
        }

        // Try focus
        if (row.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
            return true
        }

        // Try click
        if (row.isClickable && row.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        // Try accessibility focus
        return row.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
    }

    /**
     * Perform cell selection
     */
    private fun performCellSelection(cell: AccessibilityNodeInfo): Boolean {
        // Try focus first for editable cells
        if (cell.isEditable && cell.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
            return true
        }

        // Try click
        if (cell.isClickable && cell.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }

        // Try accessibility focus
        return cell.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
    }

    /**
     * Perform click action
     */
    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * Perform long click action
     */
    private fun performLongClick(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    /**
     * Scroll table down
     */
    private fun scrollTableDown(tableNode: AccessibilityNodeInfo): Boolean {
        return tableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    /**
     * Scroll table up
     */
    private fun scrollTableUp(tableNode: AccessibilityNodeInfo): Boolean {
        return tableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    // ============================================================================
    // Helper Methods - Command Parsing
    // ============================================================================

    /**
     * Check if command is table navigation
     */
    private fun isTableNavigationCommand(command: String): Boolean {
        val patterns = listOf(
            "select row", "go to row",
            "next row", "previous row",
            "first row", "last row"
        )
        return patterns.any { command.contains(it) }
    }

    /**
     * Check if command is sort command
     */
    private fun isSortCommand(command: String): Boolean {
        return command.contains("sort by") || command.matches(Regex("sort \\w+.*"))
    }

    /**
     * Check if command is filter command
     */
    private fun isFilterCommand(command: String): Boolean {
        return command.contains("filter") || command.contains("clear filter") || command.contains("remove filter")
    }

    /**
     * Check if command is expand/collapse command
     */
    private fun isExpandCollapseCommand(command: String): Boolean {
        return command.contains("expand") || command.contains("collapse") || command.contains("toggle row")
    }

    /**
     * Check if command is cell selection command
     */
    private fun isCellSelectionCommand(command: String): Boolean {
        return command.contains("select cell") || command.contains("go to cell")
    }

    /**
     * Extract number from command text
     */
    private fun extractNumber(command: String): Int? {
        // Try word to number mapping first
        WORD_TO_NUMBER.forEach { (word, number) ->
            if (command.contains(word)) return number
        }

        // Try numeric extraction
        val regex = Regex("\\d+")
        val match = regex.find(command)
        return match?.value?.toIntOrNull()
    }

    /**
     * Extract multiple numbers from command
     */
    private fun extractMultipleNumbers(command: String): List<Int> {
        val numbers = mutableListOf<Int>()

        // Extract word numbers
        val words = command.split(" ")
        words.forEach { word ->
            WORD_TO_NUMBER[word.lowercase()]?.let { numbers.add(it) }
        }

        // Extract numeric values
        val regex = Regex("\\d+")
        regex.findAll(command).forEach { match ->
            match.value.toIntOrNull()?.let { numbers.add(it) }
        }

        return numbers
    }

    /**
     * Extract column name from command
     */
    private fun extractColumnName(command: String, prefixes: List<String>): String? {
        var text = command

        // Remove prefixes
        prefixes.forEach { prefix ->
            text = text.replace(prefix, "").trim()
        }

        // Remove direction keywords
        text = text.replace("ascending", "").replace("descending", "").trim()

        return text.ifBlank { null }
    }

    /**
     * Extract filter parameters (column name and value)
     */
    private fun extractFilterParameters(command: String): Pair<String?, String?> {
        // Pattern: "filter [column] by [value]" or "filter by [column] [value]"
        val byPattern = Regex("filter(?:\\s+by)?\\s+(\\w+)\\s+(?:by\\s+)?(.+)", RegexOption.IGNORE_CASE)
        val match = byPattern.find(command)

        return if (match != null) {
            val column = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            Pair(column, value)
        } else {
            Pair(null, null)
        }
    }

    // ============================================================================
    // Lifecycle Methods
    // ============================================================================

    /**
     * Check if handler is ready
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Get current state
     */
    fun getState(): TableHandlerState {
        return TableHandlerState(
            isInitialized = isInitialized,
            currentRowIndex = currentRowIndex,
            currentColumnIndex = currentColumnIndex,
            hasAccessibilityService = accessibilityService != null
        )
    }

    /**
     * Reset row/column tracking
     */
    fun resetPosition() {
        currentRowIndex = 0
        currentColumnIndex = 0
        Log.d(TAG, "Position reset to row 0, column 0")
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        CommandRegistry.unregisterHandler(moduleId)
        handlerScope.cancel()
        accessibilityService = null
        instance = null
        Log.d(TAG, "TableHandler disposed")
    }
}

/**
 * Table handler state information
 */
data class TableHandlerState(
    val isInitialized: Boolean,
    val currentRowIndex: Int,
    val currentColumnIndex: Int,
    val hasAccessibilityService: Boolean
)
