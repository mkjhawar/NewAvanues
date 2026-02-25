package com.augmentalis.avanueui.renderer.android.mappers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.augmentalis.avanueui.ui.core.navigation.BottomNavComponent
import com.augmentalis.avanueui.ui.core.navigation.BottomNavItem
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer

/**
 * BottomNavMapper - Maps BottomNavComponent to Material3 NavigationBar
 */
class BottomNavMapper : ComponentMapper<BottomNavComponent> {

    override fun map(component: BottomNavComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            NavigationBar {
                component.items.forEachIndexed { index, item ->
                    val isSelected = index == component.selectedIndex

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { component.onItemSelected?.invoke(index) },
                        icon = {
                            val badgeText = item.badge
                            if (badgeText != null) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text(badgeText) }
                                    }
                                ) {
                                    Icon(
                                        imageVector = getIconForName(item.icon),
                                        contentDescription = item.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = getIconForName(item.icon),
                                    contentDescription = item.label
                                )
                            }
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    }

    private fun getIconForName(name: String) = when (name.lowercase()) {
        "home" -> Icons.Default.Home
        "search" -> Icons.Default.Search
        "profile", "person" -> Icons.Default.Person
        "settings" -> Icons.Default.Settings
        "favorite", "favourites" -> Icons.Default.Favorite
        else -> Icons.Default.Home
    }
}

// Typealias for convenience
typealias BottomNav = BottomNavComponent
