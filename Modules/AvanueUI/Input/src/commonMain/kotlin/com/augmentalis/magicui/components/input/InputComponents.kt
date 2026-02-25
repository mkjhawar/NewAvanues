package com.augmentalis.avanueui.input

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * MagicUI Input Components
 *
 * 12 advanced input components for complex user interactions
 */

/**
 * Slider component for range selection
 */
@Serializable
data class Slider(
    val id: String,
    val value: Float,
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val enabled: Boolean = true,
    val showValue: Boolean = true,
    val label: String? = null
)

/**
 * RangeSlider component for two-thumb range selection
 */
@Serializable
data class RangeSlider(
    val id: String,
    val valueStart: Float,
    val valueEnd: Float,
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val enabled: Boolean = true,
    val showValues: Boolean = true,
    val label: String? = null
)

/**
 * DatePicker component with calendar popup
 */
@Serializable
data class DatePicker(
    val id: String,
    val selectedDate: Date? = null,
    val minDate: Date? = null,
    val maxDate: Date? = null,
    val enabled: Boolean = true,
    val label: String? = null,
    val placeholder: String = "Select date",
    val format: DateFormat = DateFormat.ISO_8601
)

/**
 * TimePicker component for time selection
 */
@Serializable
data class TimePicker(
    val id: String,
    val selectedTime: Time? = null,
    val is24Hour: Boolean = true,
    val enabled: Boolean = true,
    val label: String? = null,
    val placeholder: String = "Select time"
)

/**
 * RadioButton component for single choice
 */
@Serializable
data class RadioButton(
    val id: String,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val label: String? = null
)

/**
 * RadioGroup component for managing radio buttons
 */
@Serializable
data class RadioGroup(
    val id: String,
    val options: List<RadioOption>,
    val selectedIndex: Int? = null,
    val enabled: Boolean = true,
    val orientation: Orientation = Orientation.Vertical
)

/**
 * Dropdown/Select menu component
 */
@Serializable
data class Dropdown(
    val id: String,
    val options: List<DropdownOption>,
    val selectedIndex: Int? = null,
    val enabled: Boolean = true,
    val label: String? = null,
    val placeholder: String = "Select option",
    val searchable: Boolean = false
)

/**
 * Autocomplete component with search and select
 */
@Serializable
data class Autocomplete(
    val id: String,
    val options: List<AutocompleteOption>,
    val selectedOption: AutocompleteOption? = null,
    val inputValue: String = "",
    val enabled: Boolean = true,
    val label: String? = null,
    val placeholder: String = "Start typing...",
    val minChars: Int = 1,
    val maxResults: Int = 10
)

/**
 * FileUpload component for file selection
 */
@Serializable
data class FileUpload(
    val id: String,
    val selectedFiles: List<FileInfo> = emptyList(),
    val enabled: Boolean = true,
    val multiple: Boolean = false,
    val accept: List<String> = emptyList(),
    val maxSize: Long? = null,
    val maxFiles: Int = 1,
    val label: String? = null,
    val buttonText: String = "Choose File"
)

/**
 * ImagePicker component for image selection
 */
@Serializable
data class ImagePicker(
    val id: String,
    val selectedImages: List<ImageInfo> = emptyList(),
    val enabled: Boolean = true,
    val multiple: Boolean = false,
    val maxImages: Int = 1,
    val maxSize: Long? = null,
    val allowCamera: Boolean = true,
    val allowGallery: Boolean = true,
    val cropEnabled: Boolean = false,
    val aspectRatio: Float? = null,
    val label: String? = null
)

/**
 * Rating component with stars
 */
@Serializable
data class Rating(
    val id: String,
    val rating: Float = 0f,
    val maxRating: Int = 5,
    val enabled: Boolean = true,
    val allowHalf: Boolean = false,
    val size: RatingSize = RatingSize.Medium,
    val showValue: Boolean = false,
    val label: String? = null
)

/**
 * SearchBar component for search input
 */
@Serializable
data class SearchBar(
    val id: String,
    val query: String = "",
    val placeholder: String = "Search...",
    val enabled: Boolean = true,
    val showClearButton: Boolean = true,
    val showSearchIcon: Boolean = true,
    val autoFocus: Boolean = false,
    val debounceMs: Long = 300
)

// Supporting data classes

@Serializable
data class RadioOption(
    val id: String,
    val label: String,
    val enabled: Boolean = true
)

@Serializable
data class DropdownOption(
    val id: String,
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true
)

@Serializable
data class AutocompleteOption(
    val id: String,
    val label: String,
    val description: String? = null,
    val icon: String? = null
)

@Serializable
data class FileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val uri: String,
    val lastModified: Long
)

@Serializable
data class ImageInfo(
    val id: String,
    val name: String,
    val size: Long,
    val uri: String,
    val width: Int,
    val height: Int
)

@Serializable
data class Date(
    val year: Int,
    val month: Int,
    val day: Int
) {
    companion object {
        fun now(): Date {
            val local = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            return Date(local.year, local.monthNumber, local.dayOfMonth)
        }
        fun fromTimestamp(timestamp: Long): Date {
            val local = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            return Date(local.year, local.monthNumber, local.dayOfMonth)
        }
    }

    fun toTimestamp(): Long = 0L

    fun format(format: DateFormat): String = when (format) {
        DateFormat.ISO_8601 -> "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        DateFormat.US -> "${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}/$year"
        DateFormat.EU -> "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
    }
}

@Serializable
data class Time(
    val hour: Int,
    val minute: Int,
    val second: Int = 0
) {
    companion object {
        fun now(): Time {
            val local = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            return Time(local.hour, local.minute, local.second)
        }
    }

    fun format(is24Hour: Boolean): String = if (is24Hour) {
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    } else {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        "${h.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} $amPm"
    }
}

@Serializable
enum class DateFormat {
    ISO_8601,
    US,
    EU
}

@Serializable
enum class Orientation {
    Horizontal,
    Vertical
}

@Serializable
enum class RatingSize {
    Small,
    Medium,
    Large
}
