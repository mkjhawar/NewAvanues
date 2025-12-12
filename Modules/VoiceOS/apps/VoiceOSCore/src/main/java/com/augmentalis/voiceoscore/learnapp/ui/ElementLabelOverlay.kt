/**
 * ElementLabelOverlay.kt - Interactive element highlighting with tap-to-rename
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-12
 *
 * Provides visual highlighting of actionable elements with color-coded labels
 * and tap-to-rename functionality for user-provided synonyms.
 *
 * Features:
 * - Highlight all actionable elements on screen
 * - Color-coded labels (green=semantic, yellow=fallback, blue=user-defined)
 * - Tap element to show rename dialog
 * - Save user-provided labels as synonyms
 *
 * Uses XML-based layout to avoid Compose lifecycle issues in AccessibilityService.
 */
package com.augmentalis.voiceoscore.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.augmentalis.database.dto.ElementCommandDTO
import com.augmentalis.database.repositories.IElementCommandRepository
import com.augmentalis.voiceoscore.R
import com.augmentalis.voiceoscore.utils.MaterialThemeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Data class representing a highlighted element
 *
 * @param bounds Element bounds on screen
 * @param uuid Element unique identifier
 * @param label Current voice command label
 * @param labelType Type of label (semantic, fallback, user-defined)
 * @param elementType Type of element (button, tab, etc.)
 * @param packageName App package name
 */
data class HighlightedElement(
    val bounds: Rect,
    val uuid: String,
    val label: String,
    val labelType: LabelType,
    val elementType: String,
    val packageName: String
)

/**
 * Types of labels for color coding
 */
enum class LabelType {
    SEMANTIC,      // Green - from text/contentDescription
    FALLBACK,      // Yellow - position-based or generated
    USER_DEFINED,  // Blue - user-provided synonym
    NONE           // Red - no label (should be rare)
}

/**
 * ElementLabelOverlay - Manages element highlighting and rename dialog
 *
 * Shows interactive overlay highlighting actionable elements with color-coded
 * labels. Users can tap elements to rename them.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val overlay = ElementLabelOverlay(
 *     context = accessibilityService,
 *     elementCommandRepository = repository
 * )
 *
 * // When exploring screen, show elements
 * overlay.showElements(elements)
 *
 * // User taps element → rename dialog shown automatically
 *
 * // Hide when done
 * overlay.hide()
 * ```
 *
 * @param context AccessibilityService context
 * @param elementCommandRepository Repository for saving user commands
 */
class ElementLabelOverlay(
    private val context: AccessibilityService,
    private val elementCommandRepository: IElementCommandRepository
) {
    companion object {
        private const val TAG = "ElementLabelOverlay"

        // Colors for label types
        val COLOR_SEMANTIC = Color.parseColor("#4CAF50")      // Green
        val COLOR_FALLBACK = Color.parseColor("#FFC107")      // Yellow/Amber
        val COLOR_USER_DEFINED = Color.parseColor("#2196F3")  // Blue
        val COLOR_NONE = Color.parseColor("#F44336")          // Red

        // Label background colors (semi-transparent)
        val BG_SEMANTIC = Color.parseColor("#CC4CAF50")
        val BG_FALLBACK = Color.parseColor("#CCFFC107")
        val BG_USER_DEFINED = Color.parseColor("#CC2196F3")
        val BG_NONE = Color.parseColor("#CCF44336")
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var overlayView: ElementHighlightView? = null
    private var isShowing = false
    private var currentElements: List<HighlightedElement> = emptyList()
    private var currentPackageName: String = ""

    /**
     * Show overlay with highlighted elements
     *
     * @param elements List of elements to highlight
     */
    fun showElements(elements: List<HighlightedElement>) {
        mainHandler.post {
            try {
                currentElements = elements
                if (elements.isNotEmpty()) {
                    currentPackageName = elements.first().packageName
                }

                if (isShowing) {
                    // Update existing view
                    overlayView?.setElements(elements)
                    overlayView?.invalidate()
                    return@post
                }

                // Create and show overlay
                createOverlayView(elements)
                val params = createLayoutParams()
                windowManager.addView(overlayView, params)
                isShowing = true

                Log.i(TAG, "Showing ${elements.size} highlighted elements")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay", e)
            }
        }
    }

    /**
     * Hide the overlay
     */
    fun hide() {
        mainHandler.post {
            try {
                if (!isShowing) return@post

                overlayView?.let { view ->
                    windowManager.removeView(view)
                }
                overlayView = null
                isShowing = false
                currentElements = emptyList()

                Log.i(TAG, "Element label overlay hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide overlay", e)
            }
        }
    }

    /**
     * Check if overlay is currently visible
     */
    fun isVisible(): Boolean = isShowing

    /**
     * Dispose and clean up resources
     */
    fun dispose() {
        hide()
    }

    // ========== Private Methods ==========

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView(elements: List<HighlightedElement>) {
        overlayView = ElementHighlightView(context).apply {
            setElements(elements)
            setOnElementTappedListener { element ->
                showRenameDialog(element)
            }
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    /**
     * Show rename dialog for element
     */
    private fun showRenameDialog(element: HighlightedElement) {
        mainHandler.post {
            try {
                // Create dialog using MaterialThemeHelper for theming
                // Reuse existing manual label dialog layout
                val dialogView = MaterialThemeHelper.inflateOverlay(
                    context,
                    R.layout.learnapp_manual_label_dialog
                )

                val titleText = dialogView.findViewById<TextView>(R.id.text_dialog_title)
                val messageText = dialogView.findViewById<TextView>(R.id.text_dialog_message)
                val previewType = dialogView.findViewById<TextView>(R.id.text_preview_type)
                val previewDetails = dialogView.findViewById<TextView>(R.id.text_preview_details)
                val previewBounds = dialogView.findViewById<TextView>(R.id.text_preview_bounds)
                val inputLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.input_layout_label)
                val inputField = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.input_label)
                val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_cancel)
                val saveButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_save)

                // Hide elements not needed for rename
                dialogView.findViewById<View>(R.id.text_quick_select)?.visibility = View.GONE
                dialogView.findViewById<View>(R.id.chip_group_suggestions)?.visibility = View.GONE

                titleText?.text = "Rename Element"
                messageText?.text = "Enter a custom voice command for this element."
                previewType?.text = element.elementType
                previewDetails?.text = "Current label: ${element.label}"
                previewBounds?.text = "Position: (${element.bounds.centerX()}, ${element.bounds.centerY()})"
                inputLayout?.hint = "Voice command"
                inputField?.setText(element.label)
                inputField?.selectAll()

                val dialog = AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
                    .setView(dialogView)
                    .create()

                cancelButton?.setOnClickListener { dialog.dismiss() }
                saveButton?.setOnClickListener {
                    val newLabel = inputField?.text?.toString()?.trim()
                    if (!newLabel.isNullOrBlank()) {
                        saveUserLabel(element, newLabel)
                    }
                    dialog.dismiss()
                }

                // Show dialog with system alert window type
                dialog.window?.setType(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    }
                )

                dialog.show()
                Log.i(TAG, "Showing rename dialog for: ${element.label}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to show rename dialog", e)
            }
        }
    }

    /**
     * Save user-provided label as synonym
     */
    private fun saveUserLabel(element: HighlightedElement, userLabel: String) {
        scope.launch {
            try {
                val command = ElementCommandDTO(
                    id = 0,
                    elementUuid = element.uuid,
                    commandPhrase = userLabel.lowercase(),
                    confidence = 1.0,
                    createdAt = System.currentTimeMillis(),
                    createdBy = "user",
                    isSynonym = true,
                    appId = element.packageName
                )

                val id = elementCommandRepository.insert(command)
                Log.i(TAG, "Saved user label: '$userLabel' for element ${element.uuid} (id=$id)")

                // Update the element in current list
                val updatedElements = currentElements.map { e ->
                    if (e.uuid == element.uuid) {
                        e.copy(label = userLabel, labelType = LabelType.USER_DEFINED)
                    } else {
                        e
                    }
                }
                currentElements = updatedElements

                // Refresh overlay
                mainHandler.post {
                    overlayView?.setElements(updatedElements)
                    overlayView?.invalidate()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save user label", e)
            }
        }
    }
}

/**
 * Custom view for drawing element highlights
 */
@SuppressLint("ViewConstructor")
class ElementHighlightView(
    context: Context
) : View(context) {

    private var elements: List<HighlightedElement> = emptyList()
    private var onElementTapped: ((HighlightedElement) -> Unit)? = null

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val labelBgPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setElements(elements: List<HighlightedElement>) {
        this.elements = elements
    }

    fun setOnElementTappedListener(listener: (HighlightedElement) -> Unit) {
        onElementTapped = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (element in elements) {
            val color = when (element.labelType) {
                LabelType.SEMANTIC -> ElementLabelOverlay.COLOR_SEMANTIC
                LabelType.FALLBACK -> ElementLabelOverlay.COLOR_FALLBACK
                LabelType.USER_DEFINED -> ElementLabelOverlay.COLOR_USER_DEFINED
                LabelType.NONE -> ElementLabelOverlay.COLOR_NONE
            }

            val bgColor = when (element.labelType) {
                LabelType.SEMANTIC -> ElementLabelOverlay.BG_SEMANTIC
                LabelType.FALLBACK -> ElementLabelOverlay.BG_FALLBACK
                LabelType.USER_DEFINED -> ElementLabelOverlay.BG_USER_DEFINED
                LabelType.NONE -> ElementLabelOverlay.BG_NONE
            }

            // Draw border around element
            borderPaint.color = color
            val rectF = RectF(element.bounds)
            canvas.drawRoundRect(rectF, 8f, 8f, borderPaint)

            // Draw semi-transparent fill
            fillPaint.color = Color.argb(30, Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawRoundRect(rectF, 8f, 8f, fillPaint)

            // Draw label background
            val labelText = element.label.take(20) + if (element.label.length > 20) "..." else ""
            val textWidth = textPaint.measureText(labelText)
            val labelBounds = RectF(
                element.bounds.centerX() - textWidth / 2 - 8,
                element.bounds.top - 40f,
                element.bounds.centerX() + textWidth / 2 + 8,
                element.bounds.top - 4f
            )

            labelBgPaint.color = bgColor
            canvas.drawRoundRect(labelBounds, 4f, 4f, labelBgPaint)

            // Draw label text
            canvas.drawText(
                labelText,
                element.bounds.centerX().toFloat(),
                element.bounds.top - 14f,
                textPaint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x.toInt()
            val y = event.y.toInt()

            // Find tapped element
            val tappedElement = elements.find { element ->
                element.bounds.contains(x, y)
            }

            tappedElement?.let { element ->
                onElementTapped?.invoke(element)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
