package com.augmentalis.avaelements.phase3

import com.augmentalis.avaelements.core.*

/**
 * Phase 3 Input Components - Common Interface
 *
 * 12 advanced input components for complex user interactions
 */

/**
 * Slider component for range selection
 */
data class Slider(
    val id: String,
    val value: Float,
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val enabled: Boolean = true,
    val showValue: Boolean = true,
    val onValueChange: ((Float) -> Unit)? = null,
    val label: String? = null
) : Component

/**
 * RangeSlider component for two-thumb range selection
 */
data class RangeSlider(
    val id: String,
    val valueStart: Float,
    val valueEnd: Float,
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val enabled: Boolean = true,
    val showValues: Boolean = true,
    val onValuesChange: ((Float, Float) -> Unit)? = null,
    val label: String? = null
) : Component

/**
 * DatePicker component with calendar popup
 */
data class DatePicker(
    val id: String,
    val selectedDate: Date? = null,
    val minDate: Date? = null,
    val maxDate: Date? = null,
    val enabled: Boolean = true,
    val label: String? = null,
    val placeholder: String = "Select date",
    val format: DateFormat = DateFormat.ISO_8601,
    val onDateSelected: ((Date) -> Unit)? = null
) : Component

/**
 * TimePicker component for time selection
 */
data class TimePicker(
    val id: String,
    val selectedTime: Time? = null,
    val is24Hour: Boolean = true,
    val enabled: Boolean = true,
    val label: String? = null,
    val placeholder: String = "Select time",
    val onTimeSelected: ((Time) -> Unit)? = null
) : Component

/**
 * RadioButton component for single choice
 */
data class RadioButton(
    val id: String,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val label: String? = null,
    val onSelected: (() -> Unit)? = null
) : Component

/**
 * RadioGroup component for managing radio buttons
 */
data class RadioGroup(
    val id: String,
    val options: List<RadioOption>,
    val selectedIndex: Int? = null,
    val enabled: Boolean = true,
    val orientation: Orientation = Orientation.Vertical,
    val onSelectionChanged: ((Int) -> Unit)? = null
) : Component

/**
 * Dropdown/Select menu component
 */
data class Dropdown(
    val id: String,
    val options: List<DropdownOption>,
    val selectedIndex: Int? = null,
    val enabled: Boolean = true,
    val label: String? = null,
    val placeholder: String = "Select option",
    val searchable: Boolean = false,
    val onSelectionChanged: ((Int) -> Unit)? = null
) : Component

/**
 * Autocomplete component with search and select
 */
data class Autocomplete(
    val id: String,
    val options: List<AutocompleteOption>,
    val selectedOption: AutocompleteOption? = null,
    val inputValue: String = "",
    val enabled: Boolean = true,
    val label: String? = null,
    val placeholder: String = "Start typing...",
    val minChars: Int = 1,
    val maxResults: Int = 10,
    val onInputChanged: ((String) -> Unit)? = null,
    val onOptionSelected: ((AutocompleteOption) -> Unit)? = null,
    val filterFunction: ((String, List<AutocompleteOption>) -> List<AutocompleteOption>)? = null
) : Component

/**
 * FileUpload component for file selection
 */
data class FileUpload(
    val id: String,
    val selectedFiles: List<FileInfo> = emptyList(),
    val enabled: Boolean = true,
    val multiple: Boolean = false,
    val accept: List<String> = emptyList(), // MIME types or extensions
    val maxSize: Long? = null, // bytes
    val maxFiles: Int = 1,
    val label: String? = null,
    val buttonText: String = "Choose File",
    val onFilesSelected: ((List<FileInfo>) -> Unit)? = null
) : Component

/**
 * ImagePicker component for image selection
 */
data class ImagePicker(
    val id: String,
    val selectedImages: List<ImageInfo> = emptyList(),
    val enabled: Boolean = true,
    val multiple: Boolean = false,
    val maxImages: Int = 1,
    val maxSize: Long? = null, // bytes
    val allowCamera: Boolean = true,
    val allowGallery: Boolean = true,
    val cropEnabled: Boolean = false,
    val aspectRatio: Float? = null, // width/height for cropping
    val label: String? = null,
    val onImagesSelected: ((List<ImageInfo>) -> Unit)? = null
) : Component

/**
 * Rating component with stars
 */
data class Rating(
    val id: String,
    val rating: Float = 0f,
    val maxRating: Int = 5,
    val enabled: Boolean = true,
    val allowHalf: Boolean = false,
    val size: RatingSize = RatingSize.Medium,
    val showValue: Boolean = false,
    val label: String? = null,
    val onRatingChanged: ((Float) -> Unit)? = null
) : Component

/**
 * SearchBar component for search input
 */
data class SearchBar(
    val id: String,
    val query: String = "",
    val placeholder: String = "Search...",
    val enabled: Boolean = true,
    val showClearButton: Boolean = true,
    val showSearchIcon: Boolean = true,
    val autoFocus: Boolean = false,
    val debounceMs: Long = 300,
    val onQueryChanged: ((String) -> Unit)? = null,
    val onSearch: ((String) -> Unit)? = null,
    val onClear: (() -> Unit)? = null
) : Component

// Supporting data classes

/**
 * Radio option
 */
data class RadioOption(
    val id: String,
    val label: String,
    val enabled: Boolean = true
)

/**
 * Dropdown option
 */
data class DropdownOption(
    val id: String,
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true
)

/**
 * Autocomplete option
 */
data class AutocompleteOption(
    val id: String,
    val label: String,
    val description: String? = null,
    val icon: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * File information
 */
data class FileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val uri: String,
    val lastModified: Long
)

/**
 * Image information
 */
data class ImageInfo(
    val id: String,
    val name: String,
    val size: Long,
    val uri: String,
    val width: Int,
    val height: Int,
    val thumbnail: ByteArray? = null
)

/**
 * Date representation
 */
data class Date(
    val year: Int,
    val month: Int, // 1-12
    val day: Int    // 1-31
) {
    companion object {
        fun now(): Date {
            // Platform-specific implementation
            return Date(2025, 10, 30)
        }

        fun fromTimestamp(timestamp: Long): Date {
            // Platform-specific implementation
            return Date(2025, 10, 30)
        }
    }

    fun toTimestamp(): Long {
        // Platform-specific implementation
        return 0L
    }

    fun format(format: DateFormat): String {
        return when (format) {
            DateFormat.ISO_8601 -> "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            DateFormat.US -> "${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}/$year"
            DateFormat.EU -> "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
        }
    }
}

/**
 * Time representation
 */
data class Time(
    val hour: Int,   // 0-23
    val minute: Int, // 0-59
    val second: Int = 0  // 0-59
) {
    companion object {
        fun now(): Time {
            // Platform-specific implementation
            return Time(12, 0, 0)
        }
    }

    fun format(is24Hour: Boolean): String {
        return if (is24Hour) {
            "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        } else {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val amPm = if (hour < 12) "AM" else "PM"
            "${h.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} $amPm"
        }
    }
}

/**
 * Date format options
 */
enum class DateFormat {
    ISO_8601,  // YYYY-MM-DD
    US,        // MM/DD/YYYY
    EU         // DD/MM/YYYY
}

/**
 * Orientation for radio groups
 */
enum class Orientation {
    Horizontal,
    Vertical
}

/**
 * Rating size
 */
enum class RatingSize {
    Small,
    Medium,
    Large
}
