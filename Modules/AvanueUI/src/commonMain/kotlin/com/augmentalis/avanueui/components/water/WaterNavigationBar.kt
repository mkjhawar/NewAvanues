/**
 * WaterNavigationBar.kt - Morphing water-effect navigation bar
 *
 * Apple-inspired tab bar that shrinks/expands on scroll. Morphing behavior:
 * when [isExpanded] transitions false->true or vice versa, the bar shape and
 * size animate using animateDpAsState + shape interpolation.
 *
 * Usage:
 * ```
 * val items = listOf(
 *     WaterNavItem(Icons.Default.Home, "Home"),
 *     WaterNavItem(Icons.Default.Search, "Search"),
 *     WaterNavItem(Icons.Default.Settings, "Settings")
 * )
 * WaterNavigationBar(
 *     items = items,
 *     selectedIndex = selectedTab,
 *     onItemSelected = { selectedTab = it },
 *     isExpanded = !scrollState.isScrollInProgress
 * )
 * ```
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components.water

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.tokens.WaterTokens
import com.augmentalis.avanueui.water.WaterBorder
import com.augmentalis.avanueui.water.WaterDefaults
import com.augmentalis.avanueui.water.WaterLevel
import com.augmentalis.avanueui.water.WaterShapes
import com.augmentalis.avanueui.water.waterEffect

/**
 * Navigation item data for [WaterNavigationBar].
 */
data class WaterNavItem(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String? = null
)

/**
 * Morphing water-effect navigation bar.
 *
 * @param items Navigation items to display
 * @param selectedIndex Currently selected item index
 * @param onItemSelected Callback when an item is tapped
 * @param modifier Modifier for customization
 * @param isExpanded Whether the bar is fully expanded (false = collapsed on scroll)
 * @param waterLevel Effect intensity
 */
@Suppress("DEPRECATION") // Intentional use of WaterSurface - this is a water-specific component
@Composable
fun WaterNavigationBar(
    items: List<WaterNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    waterLevel: WaterLevel = WaterLevel.REGULAR
) {
    val morphDuration = WaterTokens.morphDuration

    // Morphing dimensions
    val barHeight: Dp by animateDpAsState(
        targetValue = if (isExpanded) 72.dp else 52.dp,
        animationSpec = tween(durationMillis = morphDuration),
        label = "waterNavBarHeight"
    )
    val barCorner: Dp by animateDpAsState(
        targetValue = if (isExpanded) 24.dp else 26.dp,
        animationSpec = tween(durationMillis = morphDuration),
        label = "waterNavBarCorner"
    )
    val horizontalPadding: Dp by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = morphDuration),
        label = "waterNavBarPadding"
    )

    val morphShape: Shape = RoundedCornerShape(barCorner)

    WaterSurface(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth()
            .height(barHeight),
        shape = morphShape,
        waterLevel = waterLevel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                WaterNavItemView(
                    item = item,
                    isSelected = index == selectedIndex,
                    isExpanded = isExpanded,
                    onClick = { onItemSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun WaterNavItemView(
    item: WaterNavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = AvanueTheme.water.highlightColor
    val unselectedColor = AvanueTheme.colors.textSecondary

    val tintColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = tween(durationMillis = 200),
        label = "waterNavItemColor"
    )

    val iconSize: Dp by animateDpAsState(
        targetValue = if (isExpanded) 24.dp else 22.dp,
        animationSpec = tween(durationMillis = WaterTokens.morphDuration),
        label = "waterNavIconSize"
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.contentDescription ?: item.label,
            modifier = Modifier.size(iconSize),
            tint = tintColor
        )
        if (isExpanded) {
            Text(
                text = item.label,
                color = tintColor,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
