package com.augmentalis.voiceos.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.augmentalis.voiceos.app.R

/**
 * Custom icons for VoiceOS extracted from Material Icons Extended.
 *
 * These icons are provided as vector drawables to avoid including the full
 * material-icons-extended library (~15MB) in the APK.
 *
 * Usage:
 * ```
 * Icon(VoiceOSIcons.science(), contentDescription = "Science")
 * ```
 *
 * To add more icons:
 * 1. Get the vector drawable from Material Icons (or AvaMagic repository)
 * 2. Add it to res/drawable/ic_<name>.xml
 * 3. Add a composable function here following the pattern below
 */
object VoiceOSIcons {

    @Composable
    fun fileOpen(): ImageVector = ImageVector.vectorResource(R.drawable.ic_file_open)

    @Composable
    fun code(): ImageVector = ImageVector.vectorResource(R.drawable.ic_code)

    @Composable
    fun science(): ImageVector = ImageVector.vectorResource(R.drawable.ic_science)

    @Composable
    fun sync(): ImageVector = ImageVector.vectorResource(R.drawable.ic_sync)

    @Composable
    fun deleteSweep(): ImageVector = ImageVector.vectorResource(R.drawable.ic_delete_sweep)

    @Composable
    fun accessibility(): ImageVector = ImageVector.vectorResource(R.drawable.ic_accessibility)

    @Composable
    fun scanner(): ImageVector = ImageVector.vectorResource(R.drawable.ic_scanner)

    @Composable
    fun layers(): ImageVector = ImageVector.vectorResource(R.drawable.ic_layers)

    @Composable
    fun checklist(): ImageVector = ImageVector.vectorResource(R.drawable.ic_checklist)

    @Composable
    fun touchApp(): ImageVector = ImageVector.vectorResource(R.drawable.ic_touch_app)

    @Composable
    fun playCircle(): ImageVector = ImageVector.vectorResource(R.drawable.ic_play_circle)

    @Composable
    fun navigation(): ImageVector = ImageVector.vectorResource(R.drawable.ic_navigation)

    @Composable
    fun apps(): ImageVector = ImageVector.vectorResource(R.drawable.ic_apps)

    @Composable
    fun keyboard(): ImageVector = ImageVector.vectorResource(R.drawable.ic_keyboard)

    @Composable
    fun helpOutline(): ImageVector = ImageVector.vectorResource(R.drawable.ic_help_outline)

    // Icons that were expected to be in material-icons-core but aren't
    @Composable
    fun stop(): ImageVector = ImageVector.vectorResource(R.drawable.ic_stop)

    @Composable
    fun error(): ImageVector = ImageVector.vectorResource(R.drawable.ic_error)

    @Composable
    fun help(): ImageVector = ImageVector.vectorResource(R.drawable.ic_help)

    @Composable
    fun cancel(): ImageVector = ImageVector.vectorResource(R.drawable.ic_cancel)

    @Composable
    fun mic(): ImageVector = ImageVector.vectorResource(R.drawable.ic_mic)

    @Composable
    fun expandLess(): ImageVector = ImageVector.vectorResource(R.drawable.ic_expand_less)

    @Composable
    fun expandMore(): ImageVector = ImageVector.vectorResource(R.drawable.ic_expand_more)
}
