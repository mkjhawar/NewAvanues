/**
 * TableHandler.kt
 *
 * Created: 2026-01-27 00:00 PST
 * Last Modified: 2026-01-28 00:00 PST
 * Author: VOS4 Development Team
 * Version: 2.0.0
 *
 * Purpose: Voice command handler for DataGrid/Table navigation and manipulation
 * Features: Row navigation, cell selection, sorting, filtering, expand/collapse
 * Location: VoiceIntegration module handlers
 *
 * Changelog:
 * - v2.0.0 (2026-01-28): Migrated to BaseHandler architecture with executor pattern
 * - v1.0.0 (2026-01-27): Initial implementation with table navigation commands
 */

package com.augmentalis.avamagic.voice.handlers.display

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

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
 * - Delegates platform-specific operations to TableExecutor
 * - Implements BaseHandler for VoiceOS integration
 * - Stateless command processing
 */
class TableHandler(
    private val executor: TableExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "TableHandler"
        private val Log = LoggerFactory.getLogger(TAG)

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

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
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

    /**
     * Execute table command
     */
    override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
        val commandText = command.phrase.lowercase().trim()
        Log.d { "Processing table command: '$commandText'" }

        return try {
            when {
                // Row selection with number
                commandText.matches(Regex("(select|go to) row .*")) -> {
                    val rowNumber = extractNumber(commandText)
                    if (rowNumber != null) {
                        handleResult(executor.selectRow(rowNumber), "Selected row $rowNumber")
                    } else {
                        HandlerResult.failure("Could not parse row number", recoverable = true)
                    }
                }

                // Row navigation
                commandText.contains("next row") -> {
                    handleResult(executor.navigateToNextRow(), "Navigated to next row")
                }
                commandText.contains("previous row") -> {
                    handleResult(executor.navigateToPreviousRow(), "Navigated to previous row")
                }
                commandText.contains("first row") -> {
                    handleResult(executor.navigateToFirstRow(), "Navigated to first row")
                }
                commandText.contains("last row") -> {
                    handleResult(executor.navigateToLastRow(), "Navigated to last row")
                }

                // Sorting
                isSortCommand(commandText) -> {
                    processSortCommand(commandText)
                }

                // Filtering
                isFilterCommand(commandText) -> {
                    processFilterCommand(commandText)
                }

                // Expand/collapse
                commandText.contains("expand all") -> {
                    handleResult(executor.expandAllRows(), "Expanded all rows")
                }
                commandText.contains("collapse all") -> {
                    handleResult(executor.collapseAllRows(), "Collapsed all rows")
                }
                commandText.contains("expand row") -> {
                    handleResult(executor.expandCurrentRow(), "Expanded current row")
                }
                commandText.contains("collapse row") -> {
                    handleResult(executor.collapseCurrentRow(), "Collapsed current row")
                }
                commandText.contains("toggle row") -> {
                    handleResult(executor.toggleCurrentRow(), "Toggled current row")
                }

                // Cell selection
                isCellSelectionCommand(commandText) -> {
                    processCellSelectionCommand(commandText)
                }

                else -> {
                    Log.d { "Unrecognized table command: $commandText" }
                    HandlerResult.notHandled()
                }
            }
        } catch (e: Exception) {
            Log.e({ "Error processing command: $commandText" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    /**
     * Convert executor result to HandlerResult
     */
    private fun handleResult(result: TableResult, successMessage: String): HandlerResult {
        return when (result) {
            is TableResult.Success -> HandlerResult.Success(
                message = result.message ?: successMessage,
                data = result.data ?: emptyMap()
            )
            is TableResult.Error -> HandlerResult.failure(
                reason = result.message,
                recoverable = result.recoverable
            )
            is TableResult.NotFound -> HandlerResult.failure(
                reason = result.message,
                recoverable = true
            )
            is TableResult.NoAccessibility -> HandlerResult.failure(
                reason = "Accessibility service not available",
                recoverable = false
            )
        }
    }

    /**
     * Process sort command
     */
    private suspend fun processSortCommand(command: String): HandlerResult {
        val ascending = !command.contains("descending")
        val columnName = extractColumnName(command, listOf("sort by", "sort"))

        if (columnName.isNullOrBlank()) {
            return HandlerResult.failure("Could not extract column name from sort command", recoverable = true)
        }

        Log.d { "Sorting by column '$columnName' ${if (ascending) "ascending" else "descending"}" }
        return handleResult(
            executor.sortByColumn(columnName, ascending),
            "Sorted by $columnName ${if (ascending) "ascending" else "descending"}"
        )
    }

    /**
     * Process filter command
     */
    private suspend fun processFilterCommand(command: String): HandlerResult {
        return when {
            command.contains("clear filter") || command.contains("remove filter") -> {
                handleResult(executor.clearFilters(), "Filters cleared")
            }
            else -> {
                val (columnName, filterValue) = extractFilterParameters(command)
                if (columnName != null && filterValue != null) {
                    handleResult(
                        executor.filterColumn(columnName, filterValue),
                        "Filtered $columnName by $filterValue"
                    )
                } else {
                    HandlerResult.failure("Could not extract filter parameters from command", recoverable = true)
                }
            }
        }
    }

    /**
     * Process cell selection command
     */
    private suspend fun processCellSelectionCommand(command: String): HandlerResult {
        val numbers = extractMultipleNumbers(command)
        return if (numbers.size >= 2) {
            handleResult(
                executor.selectCell(numbers[0], numbers[1]),
                "Selected cell at row ${numbers[0]}, column ${numbers[1]}"
            )
        } else {
            HandlerResult.failure("Could not extract row and column from cell selection command", recoverable = true)
        }
    }

    // ============================================================================
    // Helper Methods - Command Parsing
    // ============================================================================

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
}

/**
 * Result sealed class for table operations
 */
sealed class TableResult {
    data class Success(
        val message: String? = null,
        val data: Map<String, Any>? = null
    ) : TableResult()

    data class Error(
        val message: String,
        val recoverable: Boolean = true
    ) : TableResult()

    data class NotFound(
        val message: String = "Table component not found"
    ) : TableResult()

    data object NoAccessibility : TableResult()
}

/**
 * Executor interface for table operations
 * Platform-specific implementations handle accessibility interactions
 */
interface TableExecutor {
    /**
     * Select a specific row by number (1-indexed)
     */
    suspend fun selectRow(rowNumber: Int): TableResult

    /**
     * Navigate to the next row
     */
    suspend fun navigateToNextRow(): TableResult

    /**
     * Navigate to the previous row
     */
    suspend fun navigateToPreviousRow(): TableResult

    /**
     * Navigate to the first row
     */
    suspend fun navigateToFirstRow(): TableResult

    /**
     * Navigate to the last row
     */
    suspend fun navigateToLastRow(): TableResult

    /**
     * Sort table by column name
     */
    suspend fun sortByColumn(columnName: String, ascending: Boolean): TableResult

    /**
     * Filter column by value
     */
    suspend fun filterColumn(columnName: String, value: String): TableResult

    /**
     * Clear all filters
     */
    suspend fun clearFilters(): TableResult

    /**
     * Expand current row
     */
    suspend fun expandCurrentRow(): TableResult

    /**
     * Collapse current row
     */
    suspend fun collapseCurrentRow(): TableResult

    /**
     * Toggle current row expansion
     */
    suspend fun toggleCurrentRow(): TableResult

    /**
     * Expand all rows
     */
    suspend fun expandAllRows(): TableResult

    /**
     * Collapse all rows
     */
    suspend fun collapseAllRows(): TableResult

    /**
     * Select specific cell by row and column (1-indexed)
     */
    suspend fun selectCell(row: Int, column: Int): TableResult

    /**
     * Get current table state
     */
    fun getState(): TableState
}

/**
 * Table state information
 */
data class TableState(
    val currentRowIndex: Int,
    val currentColumnIndex: Int,
    val hasAccessibilityService: Boolean
)
