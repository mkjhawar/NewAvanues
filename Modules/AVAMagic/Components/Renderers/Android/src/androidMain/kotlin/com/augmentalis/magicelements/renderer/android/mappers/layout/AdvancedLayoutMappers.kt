package com.augmentalis.avaelements.renderer.android.mappers.layout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.augmentalis.avanues.avamagic.ui.core.layout.ScaffoldComponent
import com.augmentalis.avanues.avamagic.ui.core.layout.FabPosition
import com.augmentalis.avanues.avamagic.ui.core.layout.LazyColumnComponent
import com.augmentalis.avanues.avamagic.ui.core.layout.LazyRowComponent
import com.augmentalis.avanues.avamagic.ui.core.layout.SpacerComponent
import com.augmentalis.avanues.avamagic.ui.core.layout.BoxComponent
import com.augmentalis.avanues.avamagic.ui.core.layout.SurfaceComponent
import com.augmentalis.avanues.avamagic.ui.core.layout.DrawerComponent
import com.augmentalis.avanues.avamagic.ui.core.layout.Shape
import com.augmentalis.avanues.avamagic.ui.core.layout.ContentAlignment
import com.augmentalis.avanues.avamagic.ui.core.layout.VerticalArrangement
import com.augmentalis.avanues.avamagic.ui.core.layout.HorizontalArrangement
import com.augmentalis.avanues.avamagic.ui.core.layout.HorizontalAlignment
import com.augmentalis.avanues.avamagic.ui.core.layout.VerticalAlignment
import com.augmentalis.avanues.avamagic.ui.core.layout.DividerComponent
import com.augmentalis.avanues.avamagic.ui.core.navigation.TabsComponent
import com.augmentalis.avaelements.renderer.android.ComponentMapper
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.ModifierConverter
import com.augmentalis.avaelements.renderer.android.toComposeColor

class ScaffoldMapper : ComponentMapper<ScaffoldComponent> {
    private val modifierConverter = ModifierConverter()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun map(component: ScaffoldComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Scaffold(
                modifier = modifierConverter.convert(component.modifiers),
                topBar = {
                    component.topBar?.let {
                        val composable = renderer.render(it) as @Composable () -> Unit
                        composable()
                    }
                },
                bottomBar = {
                    component.bottomBar?.let {
                        val composable = renderer.render(it) as @Composable () -> Unit
                        composable()
                    }
                },
                floatingActionButton = {
                    component.floatingActionButton?.let {
                        val composable = renderer.render(it) as @Composable () -> Unit
                        composable()
                    }
                },
                floatingActionButtonPosition = when (component.floatingActionButtonPosition) {
                    FabPosition.START -> androidx.compose.material3.FabPosition.Start
                    FabPosition.CENTER -> androidx.compose.material3.FabPosition.Center
                    FabPosition.END -> androidx.compose.material3.FabPosition.End
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    val contentComposable = renderer.render(component.content) as @Composable () -> Unit
                    contentComposable()
                }
            }
        }
    }
}

class LazyColumnMapper : ComponentMapper<LazyColumnComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: LazyColumnComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            LazyColumn(
                modifier = modifierConverter.convert(component.modifiers),
                reverseLayout = component.reverseLayout,
                verticalArrangement = when (component.verticalArrangement) {
                    VerticalArrangement.Top -> Arrangement.Top
                    VerticalArrangement.Bottom -> Arrangement.Bottom
                    VerticalArrangement.Center -> Arrangement.Center
                    VerticalArrangement.SpaceBetween -> Arrangement.SpaceBetween
                    VerticalArrangement.SpaceAround -> Arrangement.SpaceAround
                    VerticalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
                },
                horizontalAlignment = when (component.horizontalAlignment) {
                    HorizontalAlignment.Start -> Alignment.Start
                    HorizontalAlignment.Center -> Alignment.CenterHorizontally
                    HorizontalAlignment.End -> Alignment.End
                }
            ) {
                items(component.items) { item ->
                    val composable = renderer.render(item) as @Composable () -> Unit
                    composable()
                }
            }
        }
    }
}

class LazyRowMapper : ComponentMapper<LazyRowComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: LazyRowComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            LazyRow(
                modifier = modifierConverter.convert(component.modifiers),
                reverseLayout = component.reverseLayout,
                horizontalArrangement = when (component.horizontalArrangement) {
                    HorizontalArrangement.Start -> Arrangement.Start
                    HorizontalArrangement.End -> Arrangement.End
                    HorizontalArrangement.Center -> Arrangement.Center
                    HorizontalArrangement.SpaceBetween -> Arrangement.SpaceBetween
                    HorizontalArrangement.SpaceAround -> Arrangement.SpaceAround
                    HorizontalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
                },
                verticalAlignment = when (component.verticalAlignment) {
                    VerticalAlignment.Top -> Alignment.Top
                    VerticalAlignment.Center -> Alignment.CenterVertically
                    VerticalAlignment.Bottom -> Alignment.Bottom
                }
            ) {
                items(component.items) { item ->
                    val composable = renderer.render(item) as @Composable () -> Unit
                    composable()
                }
            }
        }
    }
}

class SpacerMapper : ComponentMapper<SpacerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: SpacerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val width = component.width
            val height = component.height
            val modifier = when {
                width != null && height != null ->
                    Modifier.size(width.dp, height.dp)
                width != null ->
                    Modifier.width(width.dp)
                height != null ->
                    Modifier.height(height.dp)
                else ->
                    Modifier
            }
            Spacer(modifier = modifierConverter.convert(component.modifiers).then(modifier))
        }
    }
}

class BoxMapper : ComponentMapper<BoxComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: BoxComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Box(
                modifier = modifierConverter.convert(component.modifiers),
                contentAlignment = when (component.contentAlignment) {
                    ContentAlignment.TopStart -> Alignment.TopStart
                    ContentAlignment.TopCenter -> Alignment.TopCenter
                    ContentAlignment.TopEnd -> Alignment.TopEnd
                    ContentAlignment.CenterStart -> Alignment.CenterStart
                    ContentAlignment.Center -> Alignment.Center
                    ContentAlignment.CenterEnd -> Alignment.CenterEnd
                    ContentAlignment.BottomStart -> Alignment.BottomStart
                    ContentAlignment.BottomCenter -> Alignment.BottomCenter
                    ContentAlignment.BottomEnd -> Alignment.BottomEnd
                }
            ) {
                component.children.forEach { child ->
                    val composable = renderer.render(child) as @Composable () -> Unit
                    composable()
                }
            }
        }
    }
}

class SurfaceMapper : ComponentMapper<SurfaceComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: SurfaceComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            Surface(
                modifier = modifierConverter.convert(component.modifiers),
                shape = when (component.shape) {
                    Shape.Rectangle -> RoundedCornerShape(0.dp)
                    Shape.RoundedSmall -> RoundedCornerShape(4.dp)
                    Shape.RoundedMedium -> RoundedCornerShape(8.dp)
                    Shape.RoundedLarge -> RoundedCornerShape(16.dp)
                    Shape.Circle -> CircleShape
                },
                color = component.color?.toComposeColor() ?: MaterialTheme.colorScheme.surface,
                contentColor = component.contentColor?.toComposeColor() ?: MaterialTheme.colorScheme.onSurface,
                tonalElevation = component.tonalElevation.dp,
                shadowElevation = component.shadowElevation.dp
            ) {
                component.child?.let {
                    val composable = renderer.render(it) as @Composable () -> Unit
                    composable()
                }
            }
        }
    }
}

class DrawerMapper : ComponentMapper<DrawerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: DrawerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            val drawerState = rememberDrawerState(
                initialValue = if (component.isOpen) DrawerValue.Open else DrawerValue.Closed
            )
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = component.gesturesEnabled,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = modifierConverter.convert(component.modifiers)
                    ) {
                        component.drawerContent?.let {
                            val composable = renderer.render(it) as @Composable () -> Unit
                            composable()
                        }
                    }
                }
            ) {
                component.content?.let {
                    val composable = renderer.render(it) as @Composable () -> Unit
                    composable()
                }
            }

            // Handle state changes
            if (component.isOpen && drawerState.isClosed) {
                scope.launch { drawerState.open() }
            } else if (!component.isOpen && drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }
        }
    }
}

class DividerMapper : ComponentMapper<DividerComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: DividerComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            if (component.orientation == "vertical") {
                VerticalDivider(
                    modifier = modifierConverter.convert(component.modifiers)
                        .then(if (component.thickness != null) Modifier.width(component.thickness!!.dp) else Modifier),
                    thickness = (component.thickness ?: 1f).dp,
                    color = component.color?.toComposeColor() ?: MaterialTheme.colorScheme.outlineVariant
                )
            } else {
                HorizontalDivider(
                    modifier = modifierConverter.convert(component.modifiers),
                    thickness = (component.thickness ?: 1f).dp,
                    color = component.color?.toComposeColor() ?: MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

class TabsMapper : ComponentMapper<TabsComponent> {
    private val modifierConverter = ModifierConverter()

    override fun map(component: TabsComponent, renderer: ComposeRenderer): @Composable () -> Unit {
        return {
            var selectedIndex by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(component.selectedIndex)
            }

            Column(modifier = modifierConverter.convert(component.modifiers)) {
                if (component.scrollable) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        edgePadding = 0.dp
                    ) {
                        component.tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedIndex == index,
                                onClick = {
                                    selectedIndex = index
                                    component.onTabSelected?.invoke(index)
                                },
                                text = { Text(tab.label) },
                                icon = tab.icon?.let {
                                    {
                                        val iconComposable = renderer.render(it) as @Composable () -> Unit
                                        iconComposable()
                                    }
                                },
                                enabled = tab.enabled
                            )
                        }
                    }
                } else {
                    TabRow(selectedTabIndex = selectedIndex) {
                        component.tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedIndex == index,
                                onClick = {
                                    selectedIndex = index
                                    component.onTabSelected?.invoke(index)
                                },
                                text = { Text(tab.label) },
                                icon = tab.icon?.let {
                                    {
                                        val iconComposable = renderer.render(it) as @Composable () -> Unit
                                        iconComposable()
                                    }
                                },
                                enabled = tab.enabled
                            )
                        }
                    }
                }

                // Render the content of the selected tab
                component.tabs.getOrNull(selectedIndex)?.content?.let { content ->
                    val contentComposable = renderer.render(content) as @Composable () -> Unit
                    contentComposable()
                }
            }
        }
    }
}
