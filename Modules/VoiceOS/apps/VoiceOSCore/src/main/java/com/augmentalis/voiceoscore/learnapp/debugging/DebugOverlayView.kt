/**
 * DebugOverlayView.kt - Scrollable debug overlay for exploration tracking
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * A floating overlay window that displays a scrollable list of ALL items
 * scanned during exploration with:
 * - Click status (clicked/not clicked/blocked)
 * - Source screen information
 * - VUID and element details
 * - Summary statistics
 *
 * Features:
 * - Scrollable RecyclerView-based list
 * - Filter buttons (All/Clicked/Blocked/By Screen)
 * - Collapsible header with summary stats
 * - Semi-transparent background
 * - Draggable/resizable
 */
package com.augmentalis.voiceoscore.learnapp.debugging

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug overlay view showing scrollable list of exploration items
 */
@SuppressLint("ViewConstructor")
class DebugOverlayView(
    context: Context,
    private val tracker: ExplorationItemTracker
) : FrameLayout(context) {

    companion object {
        private const val TAG = "DebugOverlayView"
    }

    // Main thread handler for thread-safe UI updates
    private val mainHandler = Handler(Looper.getMainLooper())

    // UI Components
    private var headerView: LinearLayout? = null
    private var summaryText: TextView? = null
    private var filterButtons: LinearLayout? = null
    private var contentScrollView: ScrollView? = null
    private var itemsContainer: LinearLayout? = null
    private var collapseButton: ImageButton? = null
    private var closeButton: ImageButton? = null

    // State
    private var isCollapsed = false
    private var currentFilter = OverlayDisplayMode.ALL_ITEMS
    private var currentScreenFilter: String? = null

    // Sizing - Optimized for RealWear Navigator 500 (854x480, ~480dp width)
    private val overlayWidth = dpToPx(240)   // ~50% of screen width
    private val collapsedHeight = dpToPx(36)
    private val expandedHeight = dpToPx(280) // Fits in 480dp height with room for widget

    // Move callback
    var onMoveRequested: ((Float, Float) -> Unit)? = null
    var onCloseRequested: (() -> Unit)? = null

    init {
        setupView()
        refreshItems()

        // Listen for tracker changes
        tracker.setOnItemsChangedListener { _, summary ->
            post { refreshItems() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView() {
        // Main container with dark background
        setBackgroundColor(0xE0212121.toInt())
        setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))

        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(overlayWidth, expandedHeight)
        }

        // Header with title and controls
        headerView = createHeader()
        mainLayout.addView(headerView)

        // Summary stats
        summaryText = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)  // Smaller for RealWear
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            setBackgroundColor(0x40000000)
        }
        mainLayout.addView(summaryText)

        // Filter buttons
        filterButtons = createFilterButtons()
        mainLayout.addView(filterButtons)

        // Scrollable content
        contentScrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f  // Take remaining space
            )
            isVerticalScrollBarEnabled = true
        }

        itemsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        contentScrollView?.addView(itemsContainer)
        mainLayout.addView(contentScrollView)

        addView(mainLayout)

        // Make header draggable
        setupDragging()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createHeader(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1565C0.toInt())  // Blue header
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(36)
            )

            // Title
            val title = TextView(context).apply {
                text = "DEBUG"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)  // Smaller for RealWear
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(title)

            // Save Report button
            val saveText = TextView(context).apply {
                text = "💾"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dpToPx(6), 0, dpToPx(6), 0)
                setOnClickListener { saveReport() }
                contentDescription = "Save Report"
            }
            addView(saveText)

            // Collapse button
            collapseButton = ImageButton(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                setColorFilter(Color.WHITE)
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                contentDescription = "Collapse"
                // Using text as icon placeholder (would use drawable in real app)
                setOnClickListener { toggleCollapse() }
            }
            val collapseText = TextView(context).apply {
                text = "▼"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(dpToPx(6), 0, dpToPx(6), 0)
                setOnClickListener { toggleCollapse() }
            }
            addView(collapseText)

            // Close button
            val closeText = TextView(context).apply {
                text = "✕"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(dpToPx(8), 0, dpToPx(4), 0)
                setOnClickListener { onCloseRequested?.invoke() }
            }
            addView(closeText)
        }
    }

    private fun createFilterButtons(): LinearLayout {
        val container = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
        }

        val buttonsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }

        // Filter buttons - includes Un-clicked to see what hasn't been clicked yet
        val filters = listOf(
            "All" to OverlayDisplayMode.ALL_ITEMS,
            "Screens" to OverlayDisplayMode.BY_SCREEN,
            "Clicked" to OverlayDisplayMode.CLICKED_ONLY,
            "Un-clicked" to OverlayDisplayMode.UNCLICKED_ONLY,
            "Blocked" to OverlayDisplayMode.BLOCKED_ONLY,
            "Stats" to OverlayDisplayMode.SUMMARY
        )

        filters.forEach { (label, mode) ->
            val btn = TextView(context).apply {
                text = label
                setTextColor(if (mode == currentFilter) Color.WHITE else 0xB0FFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)  // Smaller for RealWear
                setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
                setBackgroundColor(if (mode == currentFilter) 0x40FFFFFF else 0x20FFFFFF)
                setOnClickListener {
                    currentFilter = mode
                    currentScreenFilter = null
                    refreshItems()
                    updateFilterButtons(buttonsLayout)
                }
            }
            buttonsLayout.addView(btn)
        }

        container.addView(buttonsLayout)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(container)
        }
    }

    private fun updateFilterButtons(container: LinearLayout) {
        val filters = listOf(
            OverlayDisplayMode.ALL_ITEMS,
            OverlayDisplayMode.BY_SCREEN,
            OverlayDisplayMode.CLICKED_ONLY,
            OverlayDisplayMode.UNCLICKED_ONLY,
            OverlayDisplayMode.BLOCKED_ONLY,
            OverlayDisplayMode.SUMMARY
        )

        for (i in 0 until container.childCount) {
            val btn = container.getChildAt(i) as? TextView ?: continue
            val mode = filters.getOrNull(i) ?: continue
            btn.setTextColor(if (mode == currentFilter) Color.WHITE else 0xB0FFFFFF.toInt())
            btn.setBackgroundColor(if (mode == currentFilter) 0x40FFFFFF else 0x20FFFFFF)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragging() {
        var startX = 0f
        var startY = 0f

        headerView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY
                    onMoveRequested?.invoke(dx, dy)
                    startX = event.rawX
                    startY = event.rawY
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleCollapse() {
        isCollapsed = !isCollapsed

        if (isCollapsed) {
            summaryText?.visibility = View.GONE
            filterButtons?.visibility = View.GONE
            contentScrollView?.visibility = View.GONE
            layoutParams = layoutParams.apply {
                height = collapsedHeight
            }
        } else {
            summaryText?.visibility = View.VISIBLE
            filterButtons?.visibility = View.VISIBLE
            contentScrollView?.visibility = View.VISIBLE
            layoutParams = layoutParams.apply {
                height = expandedHeight
            }
        }

        requestLayout()
    }

    /**
     * Refresh displayed items based on current filter
     * Thread-safe: Always runs on main thread
     */
    fun refreshItems() {
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { refreshItemsInternal() }
        } else {
            refreshItemsInternal()
        }
    }

    /**
     * Internal refresh - must be called on main thread
     */
    private fun refreshItemsInternal() {
        try {
            // Update summary
            val summary = tracker.getSummary()
            summaryText?.text = "📊 ${summary.clickedItems}/${summary.totalItems - summary.blockedItems} clicked | " +
                    "${summary.blockedItems} blocked | ${summary.totalScreens} screens | ${summary.completionPercent}%"

            // Clear and rebuild items
            itemsContainer?.removeAllViews()

            when (currentFilter) {
                OverlayDisplayMode.ALL_ITEMS -> showAllItems()
                OverlayDisplayMode.BY_SCREEN -> showByScreen()
                OverlayDisplayMode.CLICKED_ONLY -> showFilteredItems(ItemStatus.CLICKED)
                OverlayDisplayMode.UNCLICKED_ONLY -> showFilteredItems(ItemStatus.DISCOVERED)
                OverlayDisplayMode.BLOCKED_ONLY -> showFilteredItems(ItemStatus.BLOCKED)
                OverlayDisplayMode.SUMMARY -> showSummary()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing items", e)
        }
    }

    private fun showAllItems() {
        val items = tracker.getAllItems()
        if (items.isEmpty()) {
            addEmptyMessage("No items scanned yet")
            return
        }

        items.forEach { item ->
            addItemRow(item)
        }
    }

    private fun showByScreen() {
        val grouped = tracker.getItemsGroupedByScreen()
        if (grouped.isEmpty()) {
            addEmptyMessage("No screens discovered yet")
            return
        }

        grouped.forEach { (screen, items) ->
            // Screen header
            addScreenHeader(screen)

            // Items under screen
            items.forEach { item ->
                addItemRow(item, indent = true)
            }
        }
    }

    private fun showFilteredItems(status: ItemStatus) {
        val items = tracker.getItemsByStatus(status)
        if (items.isEmpty()) {
            val statusName = when (status) {
                ItemStatus.CLICKED -> "clicked"
                ItemStatus.BLOCKED -> "blocked"
                ItemStatus.DISCOVERED -> "un-clicked"
                else -> "matching"
            }
            addEmptyMessage("No $statusName items")
            return
        }

        // Add count header
        val headerText = when (status) {
            ItemStatus.CLICKED -> "✅ ${items.size} Clicked Items"
            ItemStatus.BLOCKED -> "🚫 ${items.size} Blocked Items"
            ItemStatus.DISCOVERED -> "⚪ ${items.size} Un-clicked Items"
            else -> "${items.size} Items"
        }
        addSectionHeader(headerText)

        items.forEach { item ->
            addItemRow(item, showScreen = true)
        }
    }

    private fun showSummary() {
        val summary = tracker.getSummary()
        val screens = tracker.getAllScreens()

        // Stats table
        addStatRow("Total Items", summary.totalItems.toString())
        addStatRow("Clicked", summary.clickedItems.toString(), 0xFF4CAF50.toInt())
        addStatRow("Blocked", summary.blockedItems.toString(), 0xFFF44336.toInt())
        addStatRow("Not Clicked", summary.discoveredItems.toString(), 0xFF9E9E9E.toInt())
        addStatRow("Screens", summary.totalScreens.toString())
        addStatRow("Completion", "${summary.completionPercent}%")

        // Screen breakdown
        if (screens.isNotEmpty()) {
            addSectionHeader("Screen Breakdown")
            screens.forEach { screen ->
                addStatRow(
                    screen.shortName(),
                    "${screen.clickedCount}/${screen.itemCount - screen.blockedCount} (${screen.completionPercent()}%)"
                )
            }
        }
    }

    private fun addItemRow(item: ExplorationItem, indent: Boolean = false, showScreen: Boolean = false) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                if (indent) dpToPx(16) else dpToPx(4),
                dpToPx(4),
                dpToPx(4),
                dpToPx(4)
            )
            setBackgroundColor(0x10FFFFFF)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(1), 0, 0)
            }
        }

        // Status emoji
        val statusView = TextView(context).apply {
            text = item.statusEmoji()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)  // Smaller for RealWear
            setPadding(dpToPx(2), 0, dpToPx(2), 0)
        }
        row.addView(statusView)

        // Item details
        val detailsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Name
        val nameView = TextView(context).apply {
            text = item.shortName()
            setTextColor(item.statusColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)  // Smaller for RealWear
            maxLines = 1
        }
        detailsLayout.addView(nameView)

        // VUID and class
        val infoText = buildString {
            item.vuid?.let { append(it.take(6)) } ?: append("-")  // Shorter VUID
            append(" • ")
            append(item.className.take(12))  // Truncate class name
            if (showScreen) {
                append(" • ")
                append(item.screenName.substringAfterLast('.').take(10))
            }
        }
        val infoView = TextView(context).apply {
            text = infoText
            setTextColor(0x80FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)  // Smaller for RealWear
            maxLines = 1
        }
        detailsLayout.addView(infoView)

        // Block reason if blocked
        if (item.status == ItemStatus.BLOCKED && item.blockReason != null) {
            val reasonView = TextView(context).apply {
                text = "⚠ ${item.blockReason}"
                setTextColor(0xFFFFAB91.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            }
            detailsLayout.addView(reasonView)
        }

        row.addView(detailsLayout)
        itemsContainer?.addView(row)
    }

    private fun addScreenHeader(screen: ExplorationScreen) {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0x30FFFFFF)
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(4), 0, 0)
            }
        }

        // Screen icon and name
        val nameView = TextView(context).apply {
            text = "📱 ${screen.shortName()}"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(nameView)

        // Stats
        val statsView = TextView(context).apply {
            text = "${screen.clickedCount}/${screen.itemCount - screen.blockedCount} • ${screen.completionPercent()}%"
            setTextColor(0xB0FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        header.addView(statsView)

        itemsContainer?.addView(header)
    }

    private fun addStatRow(label: String, value: String, valueColor: Int = Color.WHITE) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val labelView = TextView(context).apply {
            text = label
            setTextColor(0xB0FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(labelView)

        val valueView = TextView(context).apply {
            text = value
            setTextColor(valueColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(null, Typeface.BOLD)
        }
        row.addView(valueView)

        itemsContainer?.addView(row)
    }

    private fun addSectionHeader(title: String) {
        val header = TextView(context).apply {
            text = title
            setTextColor(0xFF90CAF9.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(null, Typeface.BOLD)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(4))
        }
        itemsContainer?.addView(header)
    }

    private fun addEmptyMessage(message: String) {
        val empty = TextView(context).apply {
            text = message
            setTextColor(0x80FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32))
        }
        itemsContainer?.addView(empty)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * Save exploration report to file
     * Creates a markdown report in the app's external files directory
     */
    private fun saveReport() {
        try {
            // Generate markdown report from tracker
            val report = tracker.exportToMarkdown()

            // Create timestamp for filename
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val filename = "exploration-report-$timestamp.md"

            // Save to app-specific external files directory (no permission needed)
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir == null) {
                Log.e(TAG, "External files directory not available")
                showToast("Error: Storage not available")
                return
            }

            // Create reports subdirectory
            val reportsDir = File(externalDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val file = File(reportsDir, filename)
            file.writeText(report)

            Log.i(TAG, "📄 Report saved: ${file.absolutePath}")
            showToast("Report saved: $filename")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save report", e)
            showToast("Failed to save: ${e.message}")
        }
    }

    /**
     * Show toast message - thread-safe
     */
    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Cleanup
     */
    fun dispose() {
        tracker.setOnItemsChangedListener(null)
    }
}
