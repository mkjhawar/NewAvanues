package com.augmentalis.imageavanue.controller

import com.augmentalis.imageavanue.model.ImageFilter
import com.augmentalis.imageavanue.model.ImageItem
import com.augmentalis.imageavanue.model.ImageViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.FileImageInputStream
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Desktop (JVM) implementation of [IImageController].
 *
 * Gallery discovery walks [galleryRoot] (defaults to ~/Pictures) using
 * [java.nio.file.Files.walk], filtering by known image extensions. File metadata
 * (dimensions, size, dates) is read via [javax.imageio.ImageIO] and
 * [java.nio.file.attribute.BasicFileAttributes].
 *
 * Sharing opens the file in the OS default viewer via [java.awt.Desktop.open].
 *
 * All state is held in a [MutableStateFlow] so Compose for Desktop recomposes
 * automatically on every mutation.
 *
 * Author: Manoj Jhawar
 */
class DesktopImageController(
    private val galleryRoot: Path = Paths.get(System.getProperty("user.home"), "Pictures")
) : IImageController {

    private val _state = MutableStateFlow(ImageViewerState())
    override val state: StateFlow<ImageViewerState> = _state.asStateFlow()

    companion object {
        /** File extensions treated as images. Lower-case, without leading dot. */
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif", "ico", "svg")
    }

    // -------------------------------------------------------------------------
    // Gallery loading
    // -------------------------------------------------------------------------

    override suspend fun loadGallery() {
        _state.update { it.copy(isLoading = true, error = null) }
        val items = withContext(Dispatchers.IO) {
            runCatching {
                if (!Files.exists(galleryRoot)) return@runCatching emptyList()
                Files.walk(galleryRoot).use { stream ->
                    stream
                        .filter { path -> path.isRegularFile() && isImageFile(path) }
                        .map { path -> buildImageItem(path) }
                        .toList()
                        .sortedByDescending { it.dateModified }
                }
            }.getOrElse { emptyList() }
        }
        _state.update { current ->
            current.copy(
                gallery = items,
                currentIndex = 0,
                currentImage = items.firstOrNull(),
                isLoading = false,
                error = if (items.isEmpty() && Files.exists(galleryRoot)) null else null
            )
        }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    override fun openImage(uri: String) {
        val gallery = _state.value.gallery
        val index = gallery.indexOfFirst { it.uri == uri }
        if (index >= 0) {
            _state.update { it.copy(currentImage = gallery[index], currentIndex = index) }
        } else {
            // URI not in gallery — create a transient ImageItem for it
            val file = File(uri)
            val item = if (file.exists()) buildImageItem(file.toPath()) else ImageItem(uri = uri, title = file.name)
            _state.update { it.copy(currentImage = item) }
        }
    }

    override fun nextImage() {
        _state.update { current ->
            if (!current.canGoNext) return@update current
            val nextIndex = current.currentIndex + 1
            current.copy(
                currentIndex = nextIndex,
                currentImage = current.gallery[nextIndex]
            )
        }
    }

    override fun previousImage() {
        _state.update { current ->
            if (!current.canGoPrevious) return@update current
            val prevIndex = current.currentIndex - 1
            current.copy(
                currentIndex = prevIndex,
                currentImage = current.gallery[prevIndex]
            )
        }
    }

    // -------------------------------------------------------------------------
    // Transformations
    // -------------------------------------------------------------------------

    override fun applyFilter(filter: ImageFilter) {
        _state.update { it.copy(filter = filter) }
    }

    override fun rotate(clockwise: Boolean) {
        _state.update { current ->
            val delta = if (clockwise) 90f else -90f
            val newRotation = (current.rotation + delta).let { r ->
                // Normalise into [0, 360)
                ((r % 360f) + 360f) % 360f
            }
            current.copy(rotation = newRotation)
        }
    }

    override fun flipHorizontal() {
        _state.update { it.copy(flipH = !it.flipH) }
    }

    override fun flipVertical() {
        _state.update { it.copy(flipV = !it.flipV) }
    }

    override fun toggleMetadata() {
        _state.update { it.copy(showMetadata = !it.showMetadata) }
    }

    override fun resetView() {
        _state.update { current ->
            current.copy(
                zoom = 1.0f,
                panX = 0f,
                panY = 0f,
                rotation = 0f,
                flipH = false,
                flipV = false,
                filter = ImageFilter.NONE
            )
        }
    }

    // -------------------------------------------------------------------------
    // Platform operations
    // -------------------------------------------------------------------------

    override suspend fun shareImage() {
        val uri = _state.value.currentImage?.uri ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(uri)
                if (file.exists() && java.awt.Desktop.isDesktopSupported()) {
                    val desktop = java.awt.Desktop.getDesktop()
                    if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                        desktop.open(file)
                    }
                }
            }
        }
    }

    override suspend fun deleteImage() {
        val current = _state.value.currentImage ?: return
        val deleted = withContext(Dispatchers.IO) {
            runCatching { File(current.uri).delete() }.getOrElse { false }
        }
        if (deleted) {
            _state.update { s ->
                val updatedGallery = s.gallery.filter { it.uri != current.uri }
                val newIndex = (s.currentIndex).coerceAtMost(updatedGallery.lastIndex.coerceAtLeast(0))
                s.copy(
                    gallery = updatedGallery,
                    currentIndex = newIndex,
                    currentImage = updatedGallery.getOrNull(newIndex)
                )
            }
        } else {
            _state.update { it.copy(error = "Failed to delete ${current.title.ifBlank { current.uri }}") }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun isImageFile(path: Path): Boolean =
        path.extension.lowercase() in IMAGE_EXTENSIONS

    /**
     * Build an [ImageItem] from a file path, reading dimensions from the image header
     * without fully decoding the pixel data.
     */
    private fun buildImageItem(path: Path): ImageItem {
        val file = path.toFile()
        val attrs = runCatching {
            Files.readAttributes(path, BasicFileAttributes::class.java)
        }.getOrNull()

        var width = 0
        var height = 0
        var mimeType = "image/*"

        runCatching {
            val suffix = path.extension.lowercase()
            val readers: Iterator<ImageReader> = ImageIO.getImageReadersBySuffix(suffix)
            if (readers.hasNext()) {
                val reader = readers.next()
                try {
                    reader.input = FileImageInputStream(file)
                    width = reader.getWidth(0)
                    height = reader.getHeight(0)
                    mimeType = reader.formatName?.let { "image/$it" } ?: "image/$suffix"
                } finally {
                    reader.dispose()
                }
            }
        }

        return ImageItem(
            uri = file.absolutePath,
            title = path.name,
            mimeType = mimeType,
            width = width,
            height = height,
            fileSizeBytes = file.length(),
            orientation = 0,
            dateAdded = attrs?.creationTime()?.toMillis() ?: file.lastModified(),
            dateModified = attrs?.lastModifiedTime()?.toMillis() ?: file.lastModified(),
            thumbnailUri = file.absolutePath
        )
    }
}
