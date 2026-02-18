package com.augmentalis.imageavanue.controller

import com.augmentalis.imageavanue.model.ImageFilter
import com.augmentalis.imageavanue.model.ImageItem
import com.augmentalis.imageavanue.model.ImageViewerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller interface for image viewing and editing.
 * Manages gallery loading, image viewing, filter application, and transformations.
 *
 * Architecture: Defined in commonMain, implemented per-platform.
 * Android uses MediaStore + Coil; Desktop uses filesystem + java.awt.
 */
interface IImageController {

    /** Observable viewer state. */
    val state: StateFlow<ImageViewerState>

    /** Load image gallery from platform storage. */
    suspend fun loadGallery()

    /** Open a specific image by URI. */
    fun openImage(uri: String)

    /** Navigate to next image in gallery. */
    fun nextImage()

    /** Navigate to previous image in gallery. */
    fun previousImage()

    /** Apply a filter to the current image. */
    fun applyFilter(filter: ImageFilter)

    /** Rotate the current image by 90 degrees in the given direction. */
    fun rotate(clockwise: Boolean)

    /** Flip the current image horizontally. */
    fun flipHorizontal()

    /** Flip the current image vertically. */
    fun flipVertical()

    /** Toggle EXIF/metadata display. */
    fun toggleMetadata()

    /** Share the current image via platform sharing. */
    suspend fun shareImage()

    /** Delete the current image (requires platform permission). */
    suspend fun deleteImage()

    /** Reset zoom, pan, rotation to defaults. */
    fun resetView()
}
