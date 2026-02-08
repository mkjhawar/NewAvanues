package com.augmentalis.avanueui.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Component dimension tokens. Static and universal across all themes.
 * Includes standard UI, spatial/AR, and voice-specific sizes.
 */
object SizeTokens {
    // Icons
    val iconSm: Dp = 16.dp
    val iconMd: Dp = 24.dp
    val iconLg: Dp = 32.dp
    val iconXl: Dp = 48.dp

    // Buttons
    val buttonHeightSm: Dp = 32.dp
    val buttonHeightMd: Dp = 40.dp
    val buttonHeightLg: Dp = 48.dp
    val buttonHeightXl: Dp = 56.dp
    val buttonMinWidth: Dp = 88.dp

    // Touch targets
    val minTouchTarget: Dp = 48.dp
    val minTouchTargetSpatial: Dp = 60.dp

    // TextFields
    val textFieldHeight: Dp = 56.dp
    val textFieldHeightSm: Dp = 40.dp
    val textFieldHeightCompact: Dp = 36.dp

    // App bars
    val appBarHeight: Dp = 56.dp
    val appBarHeightCompact: Dp = 48.dp
    val bottomNavHeight: Dp = 56.dp

    // Chat
    val chatBubbleMaxWidth: Dp = 320.dp

    // Command bar
    val commandBarHeight: Dp = 64.dp
    val commandBarItemSize: Dp = 48.dp
    val commandBarItemGap: Dp = 8.dp
    val commandBarIconSize: Dp = 24.dp
    val commandBarCornerRadius: Dp = 32.dp

    // Voice button
    val voiceButtonSize: Dp = 56.dp
    val voiceButtonSizeCompact: Dp = 48.dp

    // Drawer
    val drawerItemHeight: Dp = 48.dp
    val drawerItemIconSize: Dp = 24.dp
    val drawerItemPadding: Dp = 12.dp
    val drawerItemGap: Dp = 8.dp
    val drawerGridItemSize: Dp = 64.dp
    val drawerGridGap: Dp = 8.dp

    fun resolve(id: String): Dp? = when (id) {
        "size.iconSm" -> iconSm
        "size.iconMd" -> iconMd
        "size.iconLg" -> iconLg
        "size.iconXl" -> iconXl
        "size.buttonHeightSm" -> buttonHeightSm
        "size.buttonHeightMd" -> buttonHeightMd
        "size.buttonHeightLg" -> buttonHeightLg
        "size.buttonHeightXl" -> buttonHeightXl
        "size.buttonMinWidth" -> buttonMinWidth
        "size.minTouchTarget" -> minTouchTarget
        "size.minTouchTargetSpatial" -> minTouchTargetSpatial
        "size.textFieldHeight" -> textFieldHeight
        "size.textFieldHeightSm" -> textFieldHeightSm
        "size.appBarHeight" -> appBarHeight
        "size.bottomNavHeight" -> bottomNavHeight
        "size.chatBubbleMaxWidth" -> chatBubbleMaxWidth
        "size.voiceButtonSize" -> voiceButtonSize
        else -> null
    }
}

/**
 * AR/VR Spatial UI sizes.
 */
object SpatialSizeTokens {
    const val hudDistance: Float = 0.5f
    const val interactiveDistance: Float = 1.0f
    const val primaryDistance: Float = 1.5f
    const val secondaryDistance: Float = 2.0f
    const val ambientDistance: Float = 3.0f

    val spatialTouchTarget: Dp = 60.dp

    const val maxVoiceOptions: Int = 3
    const val commandHierarchyLevels: Int = 2
}
