package com.augmentalis.avanueui.renderer.android.mappers.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.ui.core.navigation.NavigationDrawerComponent
import com.augmentalis.avanueui.ui.core.navigation.NavDrawerItem
import com.augmentalis.avanueui.ui.core.navigation.DrawerType
import com.augmentalis.avanueui.ui.core.navigation.NavigationRailComponent
import com.augmentalis.avanueui.ui.core.navigation.RailItem
import com.augmentalis.avanueui.ui.core.navigation.RailLabelType
import com.augmentalis.avanueui.ui.core.navigation.BottomAppBarComponent
import com.augmentalis.avanueui.renderer.android.ComponentMapper
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.IconResolver
import com.augmentalis.avanueui.renderer.android.ModifierConverter

class NavigationDrawerMapper : ComponentMapper<NavigationDrawerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: NavigationDrawerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            when (component.drawerType) {
                DrawerType.MODAL -> ModalNavigationDrawer(
                    drawerContent = {
                        ModalDrawerSheet {
                            component.header?.let {
                                val headerComposable = renderer.render(it) as @Composable () -> Unit
                                headerComposable()
                            }
                            Spacer(Modifier.height(12.dp))
                            component.items.forEachIndexed { index, item ->
                                NavigationDrawerItem(
                                    icon = { Icon(IconResolver.resolve(item.icon), contentDescription = null) },
                                    label = { Text(item.label) },
                                    selected = index == component.selectedIndex,
                                    onClick = { component.onItemSelected?.invoke(index) },
                                    badge = item.badge?.let { { Text(it) } },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    },
                    modifier = modifierConverter.convert(component.modifiers)
                ) {
                    // Content would be rendered by parent
                }
                DrawerType.DISMISSIBLE -> DismissibleNavigationDrawer(
                    drawerContent = {
                        DismissibleDrawerSheet {
                            component.header?.let {
                                val headerComposable = renderer.render(it) as @Composable () -> Unit
                                headerComposable()
                            }
                            Spacer(Modifier.height(12.dp))
                            component.items.forEachIndexed { index, item ->
                                NavigationDrawerItem(
                                    icon = { Icon(IconResolver.resolve(item.icon), contentDescription = null) },
                                    label = { Text(item.label) },
                                    selected = index == component.selectedIndex,
                                    onClick = { component.onItemSelected?.invoke(index) },
                                    badge = item.badge?.let { { Text(it) } },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    },
                    modifier = modifierConverter.convert(component.modifiers)
                ) {
                    // Content would be rendered by parent
                }
                DrawerType.PERMANENT -> PermanentNavigationDrawer(
                    drawerContent = {
                        PermanentDrawerSheet {
                            component.header?.let {
                                val headerComposable = renderer.render(it) as @Composable () -> Unit
                                headerComposable()
                            }
                            Spacer(Modifier.height(12.dp))
                            component.items.forEachIndexed { index, item ->
                                NavigationDrawerItem(
                                    icon = { Icon(IconResolver.resolve(item.icon), contentDescription = null) },
                                    label = { Text(item.label) },
                                    selected = index == component.selectedIndex,
                                    onClick = { component.onItemSelected?.invoke(index) },
                                    badge = item.badge?.let { { Text(it) } },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    },
                    modifier = modifierConverter.convert(component.modifiers)
                ) {
                    // Content would be rendered by parent
                }
            }
        }
    }
}

class NavigationRailMapper : ComponentMapper<NavigationRailComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: NavigationRailComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            NavigationRail(
                modifier = modifierConverter.convert(component.modifiers),
                header = component.header?.let {
                    {
                        val headerComposable = renderer.render(it) as @Composable () -> Unit
                        headerComposable()
                    }
                }
            ) {
                component.items.forEachIndexed { index, item ->
                    NavigationRailItem(
                        icon = { Icon(IconResolver.resolve(item.icon), contentDescription = null) },
                        label = when (component.labelType) {
                            RailLabelType.NONE -> null
                            RailLabelType.SELECTED -> if (index == component.selectedIndex) {
                                { Text(item.label) }
                            } else null
                            RailLabelType.ALL -> { { Text(item.label) } }
                        },
                        selected = index == component.selectedIndex,
                        onClick = { component.onItemSelected?.invoke(index) },
                        enabled = item.enabled
                    )
                }
            }
        }
    }
}

class BottomAppBarMapper : ComponentMapper<BottomAppBarComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: BottomAppBarComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            BottomAppBar(
                modifier = modifierConverter.convert(component.modifiers),
                actions = {
                    component.actions.forEach { action ->
                        val actionComposable = renderer.render(action) as @Composable () -> Unit
                        actionComposable()
                    }
                },
                floatingActionButton = component.floatingActionButton?.let {
                    {
                        val fabComposable = renderer.render(it) as @Composable () -> Unit
                        fabComposable()
                    }
                }
            )
        }
    }
}
