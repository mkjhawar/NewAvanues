package com.augmentalis.avamagic.renderer.android.mappers

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.augmentalis.avamagic.ui.core.navigation.AppBarComponent
import com.augmentalis.avamagic.ui.core.navigation.AppBarAction
import com.augmentalis.avamagic.renderer.android.ComponentMapper
import com.augmentalis.avamagic.renderer.android.ComposeRenderer

/**
 * AppBarMapper - Maps AppBarComponent to Material3 TopAppBar
 */
class AppBarMapper : ComponentMapper<AppBarComponent> {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: AppBarComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            TopAppBar(
                title = {
                    Text(
                        text = component.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    NavigationIcon(component.navigationIcon, component.onNavigationClick)
                },
                actions = {
                    AppBarActions(component.actions)
                }
            )
        }
    }

    @Composable
    private fun NavigationIcon(icon: String?, onClick: (() -> Unit)?) {
        if (icon != null && onClick != null) {
            IconButton(onClick = onClick) {
                val imageVector = when (icon) {
                    "back", "arrow_back" -> Icons.AutoMirrored.Filled.ArrowBack
                    "menu" -> Icons.Default.Menu
                    else -> Icons.AutoMirrored.Filled.ArrowBack
                }
                Icon(
                    imageVector = imageVector,
                    contentDescription = "Navigation"
                )
            }
        }
    }

    @Composable
    private fun RowScope.AppBarActions(actions: List<AppBarAction>) {
        actions.forEach { action ->
            IconButton(onClick = action.onClick) {
                // In a real implementation, you'd map icon strings to actual icons
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Placeholder
                    contentDescription = action.label
                )
            }
        }
    }
}

// Typealias for convenience
typealias AppBar = AppBarComponent
