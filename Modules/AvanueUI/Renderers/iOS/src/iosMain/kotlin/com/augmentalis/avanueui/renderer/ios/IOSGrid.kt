package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*

/**
 * iOS Renderer for Grid Component
 *
 * Renders a grid layout using UICollectionView.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSGridRenderer {
    fun render(
        columns: Int = 2,
        spacing: Double = 8.0,
        itemCount: Int = 6
    ): UICollectionView {
        // Create flow layout
        val layout = UICollectionViewFlowLayout().apply {
            val screenWidth = UIScreen.mainScreen.bounds.useContents { size.width }
            val totalSpacing = (columns + 1) * spacing
            val itemWidth = (screenWidth - totalSpacing) / columns

            itemSize = CGSizeMake(itemWidth, itemWidth)
            minimumInteritemSpacing = spacing
            minimumLineSpacing = spacing
            sectionInset = UIEdgeInsetsMake(spacing, spacing, spacing, spacing)
        }

        // Create collection view
        return UICollectionView(
            frame = CGRectMake(0.0, 0.0, 375.0, 600.0),
            collectionViewLayout = layout
        ).apply {
            backgroundColor = UIColor.systemBackgroundColor

            // Note: In production, register cell classes and implement
            // UICollectionViewDataSource and UICollectionViewDelegate

            // Accessibility
            isAccessibilityElement = false // Individual cells are accessible
        }
    }
}
