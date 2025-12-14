package com.augmentalis.webavanue.ui.screen.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.Favorite

/**
 * FavoritesBar - Horizontal scrolling bar showing favorite/bookmarked sites
 *
 * Features:
 * - Horizontal scrolling list of favorite items
 * - Click to navigate to favorite URL
 * - Long-press to edit/delete favorite
 * - Add button to add current page
 * - Dark 3D theme matching command bar
 *
 * @param favorites List of favorites to display
 * @param onFavoriteClick Callback when favorite is clicked
 * @param onFavoriteLongPress Callback when favorite is long-pressed
 * @param onAddFavorite Callback when add button is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun FavoritesBar(
    favorites: List<Favorite>,
    onFavoriteClick: (Favorite) -> Unit,
    onFavoriteLongPress: (Favorite) -> Unit = {},
    onAddFavorite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Dark 3D theme colors matching BottomCommandBar
    val bgFavoritesBar = Color(0xFF0F3460).copy(alpha = 0.95f)
    val bgSurface = Color(0xFF16213E)

    Surface(
        color = bgFavoritesBar,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add Favorite button (always first)
            AddFavoriteButton(
                onClick = onAddFavorite,
                backgroundColor = bgSurface
            )

            // Favorite items
            favorites.forEach { favorite ->
                FavoriteItem(
                    favorite = favorite,
                    onClick = { onFavoriteClick(favorite) },
                    onLongPress = { onFavoriteLongPress(favorite) },
                    backgroundColor = bgSurface
                )
            }

            // Empty state hint
            if (favorites.isEmpty()) {
                Text(
                    text = "Add favorites to quickly access your most visited sites",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0A0A0),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

/**
 * AddFavoriteButton - Button to add current page to favorites
 */
@Composable
private fun AddFavoriteButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = backgroundColor,
            contentColor = Color(0xFFE8E8E8)
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add to Favorites",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Add",
            style = MaterialTheme.typography.labelMedium
        )
    }
}
