package com.augmentalis.avaelements.renderers.android.mappers
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.components.phase3.layout.*

// ============================================
// GRID
// ============================================
@Composable
fun RenderGrid(c: Grid, theme: Theme) {
    // Grid is a container component - in actual usage would contain children
    // Using a placeholder implementation
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(c.gap.dp)
    ) {
        Text(
            text = "Grid: ${c.columns} columns, ${c.gap}dp gap",
            color = theme.colorScheme.onSurface.toCompose()
        )
    }
}

// ============================================
// STACK (Z-axis layering)
// ============================================
@Composable
fun RenderStack(c: com.augmentalis.avaelements.components.phase3.layout.Stack, theme: Theme) {
    val alignment = when (c.alignment) {
        "topStart" -> Alignment.TopStart
        "topCenter" -> Alignment.TopCenter
        "topEnd" -> Alignment.TopEnd
        "centerStart" -> Alignment.CenterStart
        "center" -> Alignment.Center
        "centerEnd" -> Alignment.CenterEnd
        "bottomStart" -> Alignment.BottomStart
        "bottomCenter" -> Alignment.BottomCenter
        "bottomEnd" -> Alignment.BottomEnd
        else -> Alignment.Center
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        // Stack is a container - in actual usage would contain layered children
        Text(
            text = "Stack (${c.alignment})",
            color = theme.colorScheme.onSurface.toCompose()
        )
    }
}

// ============================================
// SPACER
// ============================================
@Composable
fun RenderSpacer(c: Spacer, theme: Theme) {
    androidx.compose.foundation.layout.Spacer(
        modifier = Modifier.size(c.size.dp)
    )
}

// ============================================
// DRAWER
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderDrawer(c: Drawer, theme: Theme) {
    val drawerState = rememberDrawerState(
        initialValue = if (c.open) DrawerValue.Open else DrawerValue.Closed
    )

    LaunchedEffect(c.open) {
        if (c.open && drawerState.isClosed) {
            drawerState.open()
        } else if (!c.open && drawerState.isOpen) {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Drawer Content",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.colorScheme.onSurface.toCompose()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // In actual usage, drawer content would be passed as children
                }
            }
        },
        content = {
            // Main content would go here
            Text(
                text = "Main Content",
                modifier = Modifier.padding(16.dp)
            )
        }
    )
}

// ============================================
// TABS
// ============================================
@Composable
fun RenderTabs(c: Tabs, theme: Theme) {
    TabRow(
        selectedTabIndex = c.selectedIndex,
        containerColor = c.style?.backgroundColor?.toCompose()
            ?: theme.colorScheme.surface.toCompose(),
        contentColor = theme.colorScheme.onSurface.toCompose(),
        indicator = { tabPositions ->
            if (c.selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = theme.colorScheme.primary.toCompose()
                )
            }
        }
    ) {
        c.tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == c.selectedIndex,
                onClick = { c.onTabChange?.invoke(index) },
                text = { Text(text = tab) },
                selectedContentColor = theme.colorScheme.primary.toCompose(),
                unselectedContentColor = theme.colorScheme.onSurfaceVariant.toCompose()
            )
        }
    }
}
