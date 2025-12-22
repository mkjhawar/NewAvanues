/**
 * SettingsAdapter.kt - RecyclerView adapter for settings UI
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Adapter for displaying setting items with different types (toggle, number, action, etc.)
 */

package com.augmentalis.voiceoscore.learnapp.settings.ui

import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for settings RecyclerView.
 *
 * Supports different setting types:
 * - TOGGLE: On/off switch
 * - NUMBER_INT: Integer number input
 * - NUMBER_LONG: Long number input
 * - NUMBER_FLOAT: Float number input
 * - ACTION: Button that triggers an action
 *
 * @param onSettingChanged Callback invoked when a setting value changes
 */
class SettingsAdapter(
    private val onSettingChanged: (key: String, newValue: Any) -> Unit
) : ListAdapter<SettingItem, SettingsAdapter.SettingViewHolder>(SettingItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
        return SettingViewHolder.create(parent, onSettingChanged)
    }

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for setting items.
     * Creates appropriate UI based on setting type.
     */
    class SettingViewHolder private constructor(
        private val container: LinearLayout,
        private val onSettingChanged: (key: String, newValue: Any) -> Unit
    ) : RecyclerView.ViewHolder(container) {

        fun bind(item: SettingItem) {
            container.removeAllViews()
            container.orientation = LinearLayout.VERTICAL
            container.setPadding(48, 32, 48, 32)

            // Add label
            val labelView = TextView(container.context).apply {
                text = item.label
                textSize = 16f
                setTextColor(0xFF000000.toInt())
            }
            container.addView(labelView)

            // Add description
            val descriptionView = TextView(container.context).apply {
                text = item.description
                textSize = 12f
                setTextColor(0xFF666666.toInt())
                setPadding(0, 8, 0, 16)
            }
            container.addView(descriptionView)

            // Add control based on type
            when (item.type) {
                SettingType.TOGGLE -> {
                    val switch = Switch(container.context).apply {
                        isChecked = item.value as? Boolean ?: false
                        setOnCheckedChangeListener { _, isChecked ->
                            onSettingChanged(item.key, isChecked)
                        }
                    }
                    container.addView(switch)
                }

                SettingType.NUMBER_INT, SettingType.NUMBER_LONG, SettingType.NUMBER_FLOAT -> {
                    val editText = EditText(container.context).apply {
                        setText(item.value.toString())
                        inputType = when (item.type) {
                            SettingType.NUMBER_INT, SettingType.NUMBER_LONG -> InputType.TYPE_CLASS_NUMBER
                            SettingType.NUMBER_FLOAT -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                            else -> InputType.TYPE_CLASS_TEXT
                        }

                        // Update on focus lost
                        setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) {
                                val newValue = when (item.type) {
                                    SettingType.NUMBER_INT -> text.toString().toIntOrNull() ?: item.value
                                    SettingType.NUMBER_LONG -> text.toString().toLongOrNull() ?: item.value
                                    SettingType.NUMBER_FLOAT -> text.toString().toFloatOrNull() ?: item.value
                                    else -> text.toString()
                                }
                                onSettingChanged(item.key, newValue)
                            }
                        }
                    }
                    container.addView(editText, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ))
                }

                SettingType.ACTION -> {
                    val button = Button(container.context).apply {
                        text = item.value.toString()
                        setOnClickListener {
                            onSettingChanged(item.key, Unit)
                        }
                    }
                    container.addView(button, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.START
                    })
                }

                SettingType.TEXT -> {
                    val editText = EditText(container.context).apply {
                        setText(item.value.toString())
                        inputType = InputType.TYPE_CLASS_TEXT

                        setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) {
                                onSettingChanged(item.key, text.toString())
                            }
                        }
                    }
                    container.addView(editText)
                }

                SettingType.SELECT, SettingType.SLIDER -> {
                    // TODO: Implement SELECT and SLIDER types when needed
                    val placeholderText = TextView(container.context).apply {
                        text = "Not implemented: ${item.type}"
                        textSize = 14f
                        setTextColor(0xFFFF0000.toInt())
                    }
                    container.addView(placeholderText)
                }
            }
        }

        companion object {
            fun create(parent: ViewGroup, onSettingChanged: (String, Any) -> Unit): SettingViewHolder {
                val container = LinearLayout(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                return SettingViewHolder(container, onSettingChanged)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    class SettingItemDiffCallback : DiffUtil.ItemCallback<SettingItem>() {
        override fun areItemsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return oldItem == newItem
        }
    }
}
