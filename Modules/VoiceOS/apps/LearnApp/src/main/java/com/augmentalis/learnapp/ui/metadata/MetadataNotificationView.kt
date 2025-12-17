/**
 * MetadataNotificationView.kt - Custom notification view
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/metadata/MetadataNotificationView.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Custom view for metadata notification display
 */

package com.augmentalis.learnapp.ui.metadata

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.augmentalis.learnapp.R
import com.augmentalis.learnapp.metadata.MetadataNotificationItem
import com.augmentalis.learnapp.metadata.MetadataSuggestion
import com.google.android.material.button.MaterialButton

/**
 * Metadata Notification View
 *
 * Custom view displaying insufficient metadata notification.
 * Material Design 3 styling with responsive layout.
 *
 * ## Features
 *
 * - Clear element information display
 * - Suggestion chips for quick labeling
 * - Action buttons: Skip, Skip All, Provide Label
 * - Queue size indicator
 * - Dismissible by close button
 *
 * @since 1.0.0
 */
class MetadataNotificationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * View references
     */
    private val textTitle: TextView
    private val textMessage: TextView
    private val textElementType: TextView
    private val textElementDetails: TextView
    private val textQueueInfo: TextView
    private val recyclerSuggestions: RecyclerView
    private val buttonSkip: MaterialButton
    private val buttonSkipAll: MaterialButton
    private val buttonProvideLabel: MaterialButton
    private val buttonClose: ImageButton

    /**
     * Current notification item
     */
    private var currentItem: MetadataNotificationItem? = null

    /**
     * Suggestions adapter
     */
    private val suggestionsAdapter = SuggestionChipAdapter()

    /**
     * Click listeners
     */
    private var onSkipClickListener: (() -> Unit)? = null
    private var onSkipAllClickListener: (() -> Unit)? = null
    private var onProvideLabelClickListener: (() -> Unit)? = null
    private var onCloseClickListener: (() -> Unit)? = null

    init {
        // Inflate layout
        val view = LayoutInflater.from(context).inflate(
            R.layout.insufficient_metadata_notification,
            this,
            true
        )

        // Get view references
        textTitle = view.findViewById(R.id.text_title)
        textMessage = view.findViewById(R.id.text_message)
        textElementType = view.findViewById(R.id.text_element_type)
        textElementDetails = view.findViewById(R.id.text_element_details)
        textQueueInfo = view.findViewById(R.id.text_queue_info)
        recyclerSuggestions = view.findViewById(R.id.recycler_suggestions)
        buttonSkip = view.findViewById(R.id.button_skip)
        buttonSkipAll = view.findViewById(R.id.button_skip_all)
        buttonProvideLabel = view.findViewById(R.id.button_provide_label)
        buttonClose = view.findViewById(R.id.button_close)

        // Setup suggestions RecyclerView
        recyclerSuggestions.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = suggestionsAdapter
        }

        // Setup click listeners
        buttonSkip.setOnClickListener { onSkipClickListener?.invoke() }
        buttonSkipAll.setOnClickListener { onSkipAllClickListener?.invoke() }
        buttonProvideLabel.setOnClickListener { onProvideLabelClickListener?.invoke() }
        buttonClose.setOnClickListener { onCloseClickListener?.invoke() }

        // Suggestion chip click listener
        suggestionsAdapter.setOnSuggestionClickListener { suggestion ->
            // Treat suggestion click as providing label
            onProvideLabelClickListener?.invoke()
        }
    }

    /**
     * Set notification item
     *
     * @param item Notification item to display
     */
    fun setNotificationItem(item: MetadataNotificationItem) {
        currentItem = item

        // Update element type
        val elementType = item.element.extractElementType()
        textElementType.text = elementType.replaceFirstChar { it.uppercase() }

        // Update element details
        val details = buildString {
            if (item.element.text.isNotBlank()) {
                append("Text: \"${item.element.text}\"")
            } else if (item.element.contentDescription.isNotBlank()) {
                append("Description: \"${item.element.contentDescription}\"")
            } else if (item.element.resourceId.isNotBlank()) {
                append("ID: ${item.element.resourceId.substringAfterLast('/')}")
            } else {
                append(context.getString(R.string.element_no_identifiers))
            }
        }
        textElementDetails.text = details

        // Update suggestions
        suggestionsAdapter.setSuggestions(item.suggestions)
    }

    /**
     * Set queue size
     *
     * @param size Number of items in queue
     */
    fun setQueueSize(size: Int) {
        if (size > 0) {
            textQueueInfo.visibility = View.VISIBLE
            textQueueInfo.text = context.getString(R.string.queue_info, size)
        } else {
            textQueueInfo.visibility = View.GONE
        }
    }

    /**
     * Set skip click listener
     *
     * @param listener Click listener
     */
    fun setOnSkipClickListener(listener: () -> Unit) {
        onSkipClickListener = listener
    }

    /**
     * Set skip all click listener
     *
     * @param listener Click listener
     */
    fun setOnSkipAllClickListener(listener: () -> Unit) {
        onSkipAllClickListener = listener
    }

    /**
     * Set provide label click listener
     *
     * @param listener Click listener
     */
    fun setOnProvideLabelClickListener(listener: () -> Unit) {
        onProvideLabelClickListener = listener
    }

    /**
     * Set close click listener
     *
     * @param listener Click listener
     */
    fun setOnCloseClickListener(listener: () -> Unit) {
        onCloseClickListener = listener
    }

    /**
     * Get current notification item
     *
     * @return Current item
     */
    fun getCurrentItem(): MetadataNotificationItem? {
        return currentItem
    }
}

/**
 * Suggestion Chip Adapter
 *
 * RecyclerView adapter for suggestion chips.
 */
private class SuggestionChipAdapter : RecyclerView.Adapter<SuggestionChipAdapter.ViewHolder>() {

    /**
     * Suggestions list
     */
    private var suggestions = emptyList<MetadataSuggestion>()

    /**
     * Click listener
     */
    private var onSuggestionClickListener: ((String) -> Unit)? = null

    /**
     * Set suggestions
     *
     * @param newSuggestions Suggestions to display
     */
    fun setSuggestions(newSuggestions: List<MetadataSuggestion>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }

    /**
     * Set suggestion click listener
     *
     * @param listener Click listener
     */
    fun setOnSuggestionClickListener(listener: (String) -> Unit) {
        onSuggestionClickListener = listener
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.metadata_suggestion_item,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(suggestions[position], onSuggestionClickListener)
    }

    override fun getItemCount(): Int = suggestions.size

    /**
     * ViewHolder
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chip: com.google.android.material.chip.Chip = itemView.findViewById(R.id.chip_suggestion)

        fun bind(suggestion: MetadataSuggestion, clickListener: ((String) -> Unit)?) {
            chip.text = suggestion.text
            chip.setOnClickListener {
                clickListener?.invoke(suggestion.text)
            }
        }
    }
}
