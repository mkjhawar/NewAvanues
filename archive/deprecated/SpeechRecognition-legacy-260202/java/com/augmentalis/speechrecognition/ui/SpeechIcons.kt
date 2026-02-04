package com.augmentalis.speechrecognition.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.augmentalis.speechrecognition.R

/**
 * Custom icons for SpeechRecognition module.
 * Using vector drawables instead of material-icons-extended to reduce APK size.
 */
object SpeechIcons {

    @Composable
    fun storage(): ImageVector = ImageVector.vectorResource(R.drawable.ic_storage)

    @Composable
    fun cancel(): ImageVector = ImageVector.vectorResource(R.drawable.ic_cancel)

    @Composable
    fun error(): ImageVector = ImageVector.vectorResource(R.drawable.ic_error)

    @Composable
    fun download(): ImageVector = ImageVector.vectorResource(R.drawable.ic_download)
}
