/**
 * ManualLabelDialog.kt - Manual label input dialog
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/metadata/ManualLabelDialog.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Dialog for manual label input with element preview
 */

package com.augmentalis.voiceoscore.learnapp.ui.metadata

import android.app.Dialog
import android.content.Context
<<<<<<< HEAD
=======
import android.view.LayoutInflater
>>>>>>> AVA-Development
import android.view.Window
import android.widget.TextView
import com.augmentalis.voiceoscore.R
import com.augmentalis.voiceoscore.learnapp.metadata.MetadataNotificationItem
<<<<<<< HEAD
import com.augmentalis.voiceoscore.utils.MaterialThemeHelper
=======
>>>>>>> AVA-Development
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Manual Label Dialog
 *
 * Dialog for user to provide manual label for element.
 * Material Design 3 dialog with element preview and suggestions.
 *
 * ## Features
 *
 * - Element preview (type, details, bounds)
 * - Text input with validation
 * - Quick select suggestion chips
 * - Character counter (max 50)
 * - Save and cancel actions
 *
 * ## Usage Example
 *
 * ```kotlin
 * val dialog = ManualLabelDialog(context)
 * dialog.showDialog(
 *     item = notificationItem,
 *     onSave = { label ->
 *         saveManualLabel(item, label)
 *     },
 *     onCancel = {
 *         // User cancelled
 *     }
 * )
 * ```
 *
 * @property context Application context
 *
 * @since 1.0.0
 */
class ManualLabelDialog(
    private val context: Context
) {
    /**
     * Current dialog instance
     */
    private var currentDialog: Dialog? = null

    /**
     * Show dialog for notification item
     *
     * @param item Notification item
     * @param onSave Callback when user saves label
     * @param onCancel Callback when user cancels
     */
    fun showDialog(
        item: MetadataNotificationItem,
        onSave: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        // Dismiss existing dialog if any
        dismissDialog()

<<<<<<< HEAD
        // Get themed context for Material components (Dialog, Chip, MaterialButton, etc.)
        val themedContext = MaterialThemeHelper.getThemedContext(context)

        // Create dialog with themed context
        val dialog = Dialog(themedContext).apply {
=======
        // Create dialog
        val dialog = Dialog(context).apply {
>>>>>>> AVA-Development
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
        }

<<<<<<< HEAD
        // Inflate layout with themed context
        val view = MaterialThemeHelper.inflateOverlay(context, R.layout.learnapp_manual_label_dialog)
=======
        // Inflate layout
        val view = LayoutInflater.from(context).inflate(
            R.layout.learnapp_manual_label_dialog,
            null
        )
>>>>>>> AVA-Development

        // Get view references
        val textPreviewType: TextView = view.findViewById(R.id.text_preview_type)
        val textPreviewDetails: TextView = view.findViewById(R.id.text_preview_details)
        val textPreviewBounds: TextView = view.findViewById(R.id.text_preview_bounds)
        val inputLayoutLabel: TextInputLayout = view.findViewById(R.id.input_layout_label)
        val inputLabel: TextInputEditText = view.findViewById(R.id.input_label)
        val chipGroupSuggestions: ChipGroup = view.findViewById(R.id.chip_group_suggestions)
        val buttonCancel: MaterialButton = view.findViewById(R.id.button_cancel)
        val buttonSave: MaterialButton = view.findViewById(R.id.button_save)

        // Populate element preview
        val element = item.element
        textPreviewType.text = element.extractElementType().replaceFirstChar { it.uppercase() }

        val details = buildString {
            if (element.text.isNotBlank()) {
                append("Text: \"${element.text}\"")
            } else if (element.contentDescription.isNotBlank()) {
                append("Description: \"${element.contentDescription}\"")
            } else if (element.resourceId.isNotBlank()) {
                append("ID: ${element.resourceId.substringAfterLast('/')}")
            } else {
                append(context.getString(R.string.learnapp_element_no_identifiers))
            }
        }
        textPreviewDetails.text = details

        val bounds = element.bounds
        textPreviewBounds.text = context.getString(
            R.string.learnapp_element_bounds_format,
            bounds.left,
            bounds.top,
            bounds.width(),
            bounds.height()
        )

<<<<<<< HEAD
        // Populate suggestion chips (use themed context for Material Chip)
        item.suggestions.forEach { suggestion ->
            val chip = Chip(themedContext).apply {
=======
        // Populate suggestion chips
        item.suggestions.forEach { suggestion ->
            val chip = Chip(context).apply {
>>>>>>> AVA-Development
                text = suggestion.text
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        inputLabel.setText(suggestion.text)
                        // Uncheck other chips
                        chipGroupSuggestions.children.forEach { child ->
                            if (child is Chip && child != this) {
                                child.isChecked = false
                            }
                        }
                    }
                }
            }
            chipGroupSuggestions.addView(chip)
        }

        // Setup button listeners
        buttonCancel.setOnClickListener {
            onCancel()
            dialog.dismiss()
        }

        buttonSave.setOnClickListener {
            val label = inputLabel.text?.toString()?.trim() ?: ""

            // Validate input
            if (label.isBlank()) {
                inputLayoutLabel.error = "Label cannot be empty"
                return@setOnClickListener
            }

            if (label.length < 2) {
                inputLayoutLabel.error = "Label too short (minimum 2 characters)"
                return@setOnClickListener
            }

            // Clear error and save
            inputLayoutLabel.error = null
            onSave(label)
            dialog.dismiss()
        }

        // Setup cancel on back press
        dialog.setOnCancelListener {
            onCancel()
        }

        // Show dialog
        dialog.setContentView(view)
        dialog.show()

        // Adjust dialog width
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        currentDialog = dialog
    }

    /**
     * Dismiss dialog
     */
    fun dismissDialog() {
        currentDialog?.dismiss()
        currentDialog = null
    }

    /**
     * Check if dialog is showing
     *
     * @return true if showing
     */
    fun isDialogShowing(): Boolean {
        return currentDialog?.isShowing == true
    }
}

/**
 * Extension function to iterate ChipGroup children
 */
private val ChipGroup.children: Sequence<android.view.View>
    get() = (0 until childCount).asSequence().map { getChildAt(it) }
