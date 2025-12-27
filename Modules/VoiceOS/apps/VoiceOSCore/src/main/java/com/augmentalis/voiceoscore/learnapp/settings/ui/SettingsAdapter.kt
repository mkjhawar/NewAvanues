/**
 * SettingsAdapter.kt - RecyclerView adapter for settings list
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-05
 *
 * Displays settings items with appropriate input controls based on type.
 */

package com.augmentalis.voiceoscore.learnapp.settings.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.augmentalis.voiceoscore.R

/**
 * Adapter for displaying and editing developer settings
 *
 * @param onSettingChanged Callback when a setting value changes
 */
class SettingsAdapter(
    private val onSettingChanged: (key: String, value: Any) -> Unit
) : ListAdapter<SettingItem, RecyclerView.ViewHolder>(SettingDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_NUMBER = 0
        private const val VIEW_TYPE_TOGGLE = 1
        private const val VIEW_TYPE_SLIDER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            SettingType.NUMBER_INT, SettingType.NUMBER_LONG, SettingType.NUMBER_FLOAT -> VIEW_TYPE_NUMBER
            SettingType.TOGGLE -> VIEW_TYPE_TOGGLE
            SettingType.SLIDER -> VIEW_TYPE_SLIDER
            SettingType.TEXT, SettingType.SELECT, SettingType.ACTION -> VIEW_TYPE_NUMBER // Fallback to number view
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NUMBER -> {
                val view = inflater.inflate(R.layout.item_setting_number, parent, false)
                NumberViewHolder(view, onSettingChanged)
            }
            VIEW_TYPE_TOGGLE -> {
                val view = inflater.inflate(R.layout.item_setting_toggle, parent, false)
                ToggleViewHolder(view, onSettingChanged)
            }
            VIEW_TYPE_SLIDER -> {
                val view = inflater.inflate(R.layout.item_setting_slider, parent, false)
                SliderViewHolder(view, onSettingChanged)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is NumberViewHolder -> holder.bind(item)
            is ToggleViewHolder -> holder.bind(item)
            is SliderViewHolder -> holder.bind(item)
        }
    }

    /**
     * ViewHolder for number input settings
     */
    class NumberViewHolder(
        view: View,
        private val onSettingChanged: (String, Any) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val labelText: TextView = view.findViewById(R.id.setting_label)
        private val descriptionText: TextView = view.findViewById(R.id.setting_description)
        private val valueInput: EditText = view.findViewById(R.id.setting_value_input)
        private val unitText: TextView = view.findViewById(R.id.setting_unit)

        private var currentKey: String = ""
        private var currentType: SettingType = SettingType.NUMBER_INT
        private var textWatcher: TextWatcher? = null

        fun bind(item: SettingItem) {
            currentKey = item.key
            currentType = item.type

            labelText.text = item.label
            descriptionText.text = item.description

            // Remove old watcher before setting text
            textWatcher?.let { valueInput.removeTextChangedListener(it) }

            // Set appropriate value and unit
            when (item.type) {
                SettingType.NUMBER_INT -> {
                    valueInput.setText(item.getIntValue().toString())
                    unitText.visibility = View.GONE
                }
                SettingType.NUMBER_LONG -> {
                    valueInput.setText(item.getLongValue().toString())
                    unitText.visibility = View.VISIBLE
                    unitText.text = "ms"
                }
                else -> {}
            }

            // Add new watcher
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val text = s?.toString() ?: return
                    if (text.isEmpty()) return

                    try {
                        val value: Any = when (currentType) {
                            SettingType.NUMBER_INT -> text.toInt()
                            SettingType.NUMBER_LONG -> text.toLong()
                            else -> return
                        }
                        onSettingChanged(currentKey, value)
                    } catch (e: NumberFormatException) {
                        // Invalid input, ignore
                    }
                }
            }
            valueInput.addTextChangedListener(textWatcher)
        }
    }

    /**
     * ViewHolder for toggle (boolean) settings
     */
    class ToggleViewHolder(
        view: View,
        private val onSettingChanged: (String, Any) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val labelText: TextView = view.findViewById(R.id.setting_label)
        private val descriptionText: TextView = view.findViewById(R.id.setting_description)
        private val toggleSwitch: Switch = view.findViewById(R.id.setting_toggle)

        private var currentKey: String = ""

        fun bind(item: SettingItem) {
            currentKey = item.key

            labelText.text = item.label
            descriptionText.text = item.description

            // Remove listener before setting checked state
            toggleSwitch.setOnCheckedChangeListener(null)
            toggleSwitch.isChecked = item.getBooleanValue()

            // Add listener
            toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                onSettingChanged(currentKey, isChecked)
            }
        }
    }

    /**
     * ViewHolder for slider (percentage/threshold) settings
     */
    class SliderViewHolder(
        view: View,
        private val onSettingChanged: (String, Any) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val labelText: TextView = view.findViewById(R.id.setting_label)
        private val descriptionText: TextView = view.findViewById(R.id.setting_description)
        private val valueText: TextView = view.findViewById(R.id.setting_value_text)
        private val slider: SeekBar = view.findViewById(R.id.setting_slider)

        private var currentKey: String = ""
        private var isPercentage: Boolean = false

        fun bind(item: SettingItem) {
            currentKey = item.key
            isPercentage = item.key.contains("_percent")

            labelText.text = item.label
            descriptionText.text = item.description

            // Remove listener before setting progress
            slider.setOnSeekBarChangeListener(null)

            val floatValue = item.getFloatValue()
            val progress = if (isPercentage) {
                floatValue.toInt() // Already 0-100
            } else {
                (floatValue * 100).toInt() // Convert 0.0-1.0 to 0-100
            }
            slider.progress = progress.coerceIn(0, 100)
            updateValueText(progress)

            // Add listener
            slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        updateValueText(progress)
                        val value = if (isPercentage) {
                            progress.toFloat()
                        } else {
                            progress / 100f
                        }
                        onSettingChanged(currentKey, value)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        private fun updateValueText(progress: Int) {
            valueText.text = if (isPercentage) {
                "$progress%"
            } else {
                "${progress / 100f}"
            }
        }
    }
}

/**
 * DiffUtil callback for efficient list updates
 */
class SettingDiffCallback : DiffUtil.ItemCallback<SettingItem>() {
    override fun areItemsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
        return oldItem.key == newItem.key
    }

    override fun areContentsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
        return oldItem == newItem
    }
}
