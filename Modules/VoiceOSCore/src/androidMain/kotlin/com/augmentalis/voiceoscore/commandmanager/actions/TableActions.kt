/**
 * TableActions.kt - Table/DataGrid command actions
 * Path: modules/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/TableActions.kt
 *
 * Created: 2026-01-27
 * Author: VOS4 Development Team
 * Module: CommandManager
 *
 * Purpose: Table and DataGrid-related voice command actions
 * Features: Row navigation, cell selection, sorting, filtering, expand/collapse
 */

package com.augmentalis.voiceoscore.commandmanager.actions

import com.augmentalis.voiceoscore.*
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Table and DataGrid command actions
 * Handles table navigation, sorting, filtering, and row manipulation
 */
object TableActions {

    private const val TAG = "TableActions"

    /**
     * Select Row Action
     */
    class SelectRowAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val rowNumber = getNumberParameter(command, "row")?.toInt()
                ?: return createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Row number required")

            return if (selectRow(accessibilityService, rowNumber)) {
                createSuccessResult(command, "Selected row $rowNumber")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to select row $rowNumber")
            }
        }
    }

    /**
     * Next Row Action
     */
    class NextRowAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return if (navigateRow(accessibilityService, NavigationDirection.NEXT)) {
                createSuccessResult(command, "Moved to next row")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to move to next row")
            }
        }
    }

    /**
     * Previous Row Action
     */
    class PreviousRowAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return if (navigateRow(accessibilityService, NavigationDirection.PREVIOUS)) {
                createSuccessResult(command, "Moved to previous row")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to move to previous row")
            }
        }
    }

    /**
     * First Row Action
     */
    class FirstRowAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return if (navigateRow(accessibilityService, NavigationDirection.FIRST)) {
                createSuccessResult(command, "Moved to first row")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to move to first row")
            }
        }
    }

    /**
     * Last Row Action
     */
    class LastRowAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return if (navigateRow(accessibilityService, NavigationDirection.LAST)) {
                createSuccessResult(command, "Moved to last row")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to move to last row")
            }
        }
    }

    /**
     * Sort By Column Action
     */
    class SortByColumnAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val columnName = getTextParameter(command, "column")
                ?: return createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Column name required")
            val ascending = getBooleanParameter(command, "ascending") ?: true

            val direction = if (ascending) "ascending" else "descending"
            return if (sortByColumn(accessibilityService, columnName, ascending)) {
                createSuccessResult(command, "Sorted by $columnName $direction")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to sort by $columnName")
            }
        }
    }

    /**
     * Filter Column Action
     */
    class FilterColumnAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val columnName = getTextParameter(command, "column")
                ?: return createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Column name required")
            val filterValue = getTextParameter(command, "value")
                ?: return createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Filter value required")

            return if (filterColumn(accessibilityService, columnName, filterValue)) {
                createSuccessResult(command, "Filtered $columnName by '$filterValue'")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to filter $columnName")
            }
        }
    }

    /**
     * Clear Filters Action
     */
    class ClearFiltersAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return if (clearFilters(accessibilityService)) {
                createSuccessResult(command, "Filters cleared")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to clear filters")
            }
        }
    }

    /**
     * Expand Row Action
     */
    class ExpandRowAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return if (toggleRowExpansion(accessibilityService, expand = true)) {
                createSuccessResult(command, "Row expanded")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to expand row")
            }
        }
    }

    /**
     * Collapse Row Action
     */
    class CollapseRowAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return if (toggleRowExpansion(accessibilityService, expand = false)) {
                createSuccessResult(command, "Row collapsed")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to collapse row")
            }
        }
    }

    /**
     * Select Cell Action
     */
    class SelectCellAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val row = getNumberParameter(command, "row")?.toInt()
                ?: return createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Row number required")
            val column = getNumberParameter(command, "column")?.toInt()
                ?: return createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "Column number required")

            return if (selectCell(accessibilityService, row, column)) {
                createSuccessResult(command, "Selected cell at row $row, column $column")
            } else {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to select cell")
            }
        }
    }

    // ============================================================================
    // Navigation Direction
    // ============================================================================

    enum class NavigationDirection {
        NEXT, PREVIOUS, FIRST, LAST
    }

    // ============================================================================
    // Implementation Methods
    // ============================================================================

    /**
     * Select a specific row by number
     */
    private fun selectRow(service: AccessibilityService?, rowNumber: Int): Boolean {
        if (service == null) return false

        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false
        val row = findRowByIndex(tableNode, rowNumber - 1)

        return if (row != null) {
            performRowSelection(row)
        } else false
    }

    /**
     * Navigate to a row based on direction
     */
    private fun navigateRow(service: AccessibilityService?, direction: NavigationDirection): Boolean {
        if (service == null) return false

        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false
        val focusedRow = findFocusedRow(tableNode)

        val result = when (direction) {
            NavigationDirection.NEXT -> navigateToAdjacentRow(tableNode, focusedRow, forward = true)
            NavigationDirection.PREVIOUS -> navigateToAdjacentRow(tableNode, focusedRow, forward = false)
            NavigationDirection.FIRST -> navigateToExtremeRow(tableNode, first = true)
            NavigationDirection.LAST -> navigateToExtremeRow(tableNode, first = false)
        }

        return result
    }

    /**
     * Sort table by column
     */
    private fun sortByColumn(service: AccessibilityService?, columnName: String, ascending: Boolean): Boolean {
        if (service == null) return false

        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false
        val headerNode = findColumnHeader(tableNode, columnName)

        return if (headerNode != null) {
            var result = headerNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            // Click again for descending sort
            if (result && !ascending) {
                Thread.sleep(200)
                result = headerNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            result
        } else false
    }

    /**
     * Filter column by value
     */
    private fun filterColumn(service: AccessibilityService?, columnName: String, value: String): Boolean {
        if (service == null) return false

        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false
        val filterInput = findFilterInput(tableNode, columnName)

        return if (filterInput != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
            val result = filterInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            result
        } else false
    }

    /**
     * Clear all filters
     */
    private fun clearFilters(service: AccessibilityService?): Boolean {
        if (service == null) return false

        val rootNode = service.rootInActiveWindow ?: return false

        // Look for clear filter button
        val clearButton = findNodeByText(rootNode, "clear filter")
            ?: findNodeByText(rootNode, "clear filters")
            ?: findNodeByText(rootNode, "reset")

        return if (clearButton != null) {
            val result = clearButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            result
        } else {
            // Clear individual filter inputs
            val tableNode = findTableNode(rootNode)
            val result = if (tableNode != null) {
                clearAllFilterInputs(tableNode)
            } else false
            result
        }
    }

    /**
     * Toggle row expansion
     */
    private fun toggleRowExpansion(service: AccessibilityService?, expand: Boolean): Boolean {
        if (service == null) return false

        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false
        val focusedRow = findFocusedRow(tableNode)

        return if (focusedRow != null) {
            val action = if (expand) {
                AccessibilityNodeInfo.ACTION_EXPAND
            } else {
                AccessibilityNodeInfo.ACTION_COLLAPSE
            }
            val result = focusedRow.performAction(action)
            result
        } else false
    }

    /**
     * Select specific cell
     */
    private fun selectCell(service: AccessibilityService?, row: Int, column: Int): Boolean {
        if (service == null) return false

        val tableNode = findTableNode(service.rootInActiveWindow) ?: return false
        val rowNode = findRowByIndex(tableNode, row - 1)

        return if (rowNode != null) {
            val cellNode = findCellByIndex(rowNode, column - 1)
            val result = if (cellNode != null) {
                val selected = cellNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    || cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                selected
            } else false
            result
        } else false
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private fun findTableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null

        val className = root.className?.toString()?.lowercase() ?: ""
        val tablePatterns = listOf(
            "recyclerview", "listview", "gridview", "tableview",
            "datagrid", "table", "grid", "lazygrid"
        )

        if (tablePatterns.any { className.contains(it) }) {
            return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findTableNode(child)
            if (found != null) return found
        }

        return null
    }

    private fun findRowByIndex(tableNode: AccessibilityNodeInfo, index: Int): AccessibilityNodeInfo? {
        if (index < 0 || index >= tableNode.childCount) return null
        return tableNode.getChild(index)
    }

    private fun findCellByIndex(rowNode: AccessibilityNodeInfo, index: Int): AccessibilityNodeInfo? {
        if (index < 0 || index >= rowNode.childCount) return null
        return rowNode.getChild(index)
    }

    private fun findFocusedRow(tableNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (i in 0 until tableNode.childCount) {
            val child = tableNode.getChild(i) ?: continue
            if (child.isFocused || child.isAccessibilityFocused || child.isSelected) {
                return child
            }
        }
        return tableNode.getChild(0) // Default to first row
    }

    private fun findColumnHeader(tableNode: AccessibilityNodeInfo, columnName: String): AccessibilityNodeInfo? {
        return findNodeByText(tableNode, columnName.lowercase())
    }

    private fun findFilterInput(tableNode: AccessibilityNodeInfo, columnName: String): AccessibilityNodeInfo? {
        val header = findColumnHeader(tableNode, columnName) ?: return null
        val parent = header.parent

        if (parent != null) {
            for (i in 0 until parent.childCount) {
                val sibling = parent.getChild(i) ?: continue
                if (sibling.isEditable) {
                    return sibling
                }
            }
        }
        return null
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = root.text?.toString()?.lowercase() ?: ""
        val contentDesc = root.contentDescription?.toString()?.lowercase() ?: ""

        if (nodeText.contains(text) || contentDesc.contains(text)) {
            return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) return found
        }

        return null
    }

    private fun performRowSelection(row: AccessibilityNodeInfo): Boolean {
        return row.performAction(AccessibilityNodeInfo.ACTION_SELECT)
            || row.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            || row.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            || row.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
    }

    private fun navigateToAdjacentRow(
        tableNode: AccessibilityNodeInfo,
        currentRow: AccessibilityNodeInfo?,
        forward: Boolean
    ): Boolean {
        if (currentRow == null) {
            val firstRow = tableNode.getChild(0) ?: return false
            val result = performRowSelection(firstRow)
            return result
        }

        val currentIndex = findRowIndex(tableNode, currentRow)
        if (currentIndex < 0) return false

        val targetIndex = if (forward) currentIndex + 1 else currentIndex - 1
        if (targetIndex < 0 || targetIndex >= tableNode.childCount) {
            // Try scrolling
            val scrollAction = if (forward) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            return tableNode.performAction(scrollAction)
        }

        val targetRow = tableNode.getChild(targetIndex) ?: return false
        val result = performRowSelection(targetRow)
        return result
    }

    private fun navigateToExtremeRow(tableNode: AccessibilityNodeInfo, first: Boolean): Boolean {
        // Scroll to extreme first
        val scrollAction = if (first) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        var scrollAttempts = 0
        while (tableNode.performAction(scrollAction) && scrollAttempts < 50) {
            scrollAttempts++
        }

        val targetIndex = if (first) 0 else tableNode.childCount - 1
        val targetRow = tableNode.getChild(targetIndex) ?: return false
        val result = performRowSelection(targetRow)
        return result
    }

    private fun findRowIndex(tableNode: AccessibilityNodeInfo, row: AccessibilityNodeInfo): Int {
        for (i in 0 until tableNode.childCount) {
            val child = tableNode.getChild(i) ?: continue
            val matches = child.hashCode() == row.hashCode()
            if (matches) return i
        }
        return -1
    }

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
            }
        }

        clearEditableNodes(tableNode)
        return cleared
    }
}
