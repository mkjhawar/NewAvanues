package com.augmentalis.imageavanue.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageItem(
    val uri: String,
    val title: String = "",
    val mimeType: String = "image/*",
    val width: Int = 0,
    val height: Int = 0,
    val fileSizeBytes: Long = 0,
    val orientation: Int = 0,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val thumbnailUri: String = "",
    val exifData: Map<String, String> = emptyMap()
) {
    val aspectRatio: Float get() = if (height > 0) width.toFloat() / height else 1f
}

@Serializable
data class ImageViewerState(
    val currentImage: ImageItem? = null,
    val gallery: List<ImageItem> = emptyList(),
    val currentIndex: Int = 0,
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val filter: ImageFilter = ImageFilter.NONE,
    val showMetadata: Boolean = false
) {
    val hasImage: Boolean get() = currentImage != null
    val canGoNext: Boolean get() = currentIndex < gallery.size - 1
    val canGoPrevious: Boolean get() = currentIndex > 0
}

/**
 * Available image filters. NONE is the identity (no filter applied).
 * Platform implementations map these to ColorMatrix or RenderEffect operations.
 */
@Serializable
enum class ImageFilter {
    NONE,
    GRAYSCALE,
    SEPIA,
    BLUR,
    SHARPEN,
    HIGH_CONTRAST,
    INVERTED,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN
}
