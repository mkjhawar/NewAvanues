package com.augmentalis.avaelements.renderers.android.mappers
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.components.phase3.navigation.*

// ============================================
// APP BAR
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderAppBar(c: AppBar, theme: Theme) = TopAppBar(
    title = { Text(text = c.title) },
    navigationIcon = {
        if (c.showBack) {
            IconButton(onClick = c.onBackClick ?: {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    }
)

// ============================================
// BOTTOM NAVIGATION
// ============================================
@Composable
fun RenderBottomNav(c: BottomNav, theme: Theme) {
    NavigationBar(
        containerColor = theme.colorScheme.surface.toCompose(),
        contentColor = theme.colorScheme.onSurface.toCompose()
    ) {
        c.items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == c.selectedIndex,
                onClick = { c.onItemClick?.invoke(index) },
                icon = {
                    // Icon placeholder - in real usage would use actual icons
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = item
                    )
                },
                label = { Text(text = item) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = theme.colorScheme.onSecondaryContainer.toCompose(),
                    selectedTextColor = theme.colorScheme.onSurface.toCompose(),
                    indicatorColor = theme.colorScheme.secondaryContainer.toCompose(),
                    unselectedIconColor = theme.colorScheme.onSurfaceVariant.toCompose(),
                    unselectedTextColor = theme.colorScheme.onSurfaceVariant.toCompose()
                )
            )
        }
    }
}

// ============================================
// BREADCRUMB
// ============================================
@Composable
fun RenderBreadcrumb(c: Breadcrumb, theme: Theme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        c.items.forEachIndexed { index, item ->
            Text(
                text = item,
                modifier = Modifier.clickable {
                    if (index < c.items.lastIndex) {
                        c.onItemClick?.invoke(index)
                    }
                }
            )

            if (index < c.items.lastIndex) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "separator",
                    tint = theme.colorScheme.onSurfaceVariant.toCompose(),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

// ============================================
// PAGINATION
// ============================================
@Composable
fun RenderPagination(c: Pagination, theme: Theme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        // Previous button
        IconButton(
            onClick = {
                if (c.currentPage > 1) {
                    c.onPageChange?.invoke(c.currentPage - 1)
                }
            },
            enabled = c.currentPage > 1
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous page"
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Page numbers (show current and nearby pages)
        val startPage = maxOf(1, c.currentPage - 2)
        val endPage = minOf(c.totalPages, c.currentPage + 2)

        for (page in startPage..endPage) {
            FilledTonalButton(
                onClick = { c.onPageChange?.invoke(page) },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(text = page.toString())
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Next button
        IconButton(
            onClick = {
                if (c.currentPage < c.totalPages) {
                    c.onPageChange?.invoke(c.currentPage + 1)
                }
            },
            enabled = c.currentPage < c.totalPages
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next page"
            )
        }
    }
}
