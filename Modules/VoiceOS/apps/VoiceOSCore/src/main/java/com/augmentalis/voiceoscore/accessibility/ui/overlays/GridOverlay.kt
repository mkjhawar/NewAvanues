/**
 * GridOverlay.kt - Grid navigation overlay for precise screen targeting
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.ui.overlays

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.augmentalis.voiceoscore.accessibility.ui.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * Grid overlay for screen navigation using coordinate system
 */
class GridOverlay(
    context: Context,
    private val onGridSelected: (GridCoordinate) -> Unit,
    private val onDismiss: () -> Unit
) : BaseOverlay(context, OverlayType.FULLSCREEN) {
    
    
    companion object {
        private const val TAG = "GridOverlay"
        private const val AUTO_HIDE_DELAY = 45000L
        private const val DEFAULT_ROWS = 5
        private const val DEFAULT_COLS = 4
        private val MIN_CELL_SIZE = 60.dp
    }
    
    private var _gridRows by mutableStateOf(DEFAULT_ROWS)
    private var _gridCols by mutableStateOf(DEFAULT_COLS)
    private var _isShowing by mutableStateOf(false)
    private var _selectedCell by mutableStateOf<GridCoordinate?>(null)
    private var _showLabels by mutableStateOf(true)
    
    /**
     * Grid coordinate with row and column
     */
    data class GridCoordinate(
        val row: Int,
        val col: Int,
        val x: Float,
        val y: Float
    ) {
        val rowLabel: String get() = ('A' + row).toString()
        val colLabel: String get() = (col + 1).toString()
        val coordinate: String get() = "$rowLabel$colLabel"
    }
    
    /**
     * Show grid overlay
     */
    fun showGrid(rows: Int = DEFAULT_ROWS, cols: Int = DEFAULT_COLS, showLabels: Boolean = true) {
        _gridRows = rows
        _gridCols = cols
        _showLabels = showLabels
        _isShowing = true
        _selectedCell = null
        
        if (!isVisible()) {
            show()
        }
        
        // Auto-hide after delay
        overlayScope.launch {
            delay(AUTO_HIDE_DELAY)
            hideGrid()
        }
    }
    
    /**
     * Hide grid overlay
     */
    fun hideGrid() {
        _isShowing = false
        
        overlayScope.launch {
            delay(300) // Wait for animation
            hide()
            onDismiss()
        }
    }
    
    /**
     * Select grid coordinate
     */
    fun selectGrid(coordinate: String) {
        val gridCoord = parseGridCoordinate(coordinate)
        if (gridCoord != null) {
            _selectedCell = gridCoord
            onGridSelected(gridCoord)
            
            // Show selection briefly then hide
            overlayScope.launch {
                delay(1000)
                hideGrid()
            }
        }
    }
    
    /**
     * Parse grid coordinate from string (e.g., "A3", "B5")
     */
    private fun parseGridCoordinate(coordinate: String): GridCoordinate? {
        if (coordinate.length < 2) return null
        
        val rowChar = coordinate[0].uppercaseChar()
        val colString = coordinate.substring(1)
        
        val row = rowChar - 'A'
        val col = colString.toIntOrNull()?.minus(1) ?: return null
        
        if (row < 0 || row >= _gridRows || col < 0 || col >= _gridCols) {
            return null
        }
        
        return createGridCoordinate(row, col)
    }
    
    /**
     * Create grid coordinate with screen position
     */
    private fun createGridCoordinate(row: Int, col: Int): GridCoordinate {
        // Calculate screen position for this grid cell (center of cell)
        val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
        
        val cellWidth = screenWidth / _gridCols
        val cellHeight = screenHeight / _gridRows
        
        val x = (col * cellWidth) + (cellWidth / 2)
        val y = (row * cellHeight) + (cellHeight / 2)
        
        return GridCoordinate(row, col, x, y)
    }
    
    /**
     * Cleanup resources when overlay is destroyed
     */
    override fun dispose() {
        overlayScope.cancel()
        super.dispose()
    }
    
    @Composable
    override fun OverlayContent() {
        val configuration = LocalConfiguration.current
        
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        
        val cellWidth = screenWidth / _gridCols
        val cellHeight = screenHeight / _gridRows
        
        AnimatedVisibility(
            visible = _isShowing,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Grid lines and cells
                GridContent(
                    rows = _gridRows,
                    cols = _gridCols,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    showLabels = _showLabels,
                    selectedCell = _selectedCell,
                    onCellClick = { row, col ->
                        val gridCoord = createGridCoordinate(row, col)
                        _selectedCell = gridCoord
                        onGridSelected(gridCoord)
                        
                        overlayScope.launch {
                            delay(1000)
                            hideGrid()
                        }
                    }
                )
                
                // Instructions panel
                InstructionsPanel(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    gridSize = "${_gridRows}x${_gridCols}",
                    onDismiss = ::hideGrid,
                    onToggleLabels = { _showLabels = !_showLabels }
                )
            }
        }
    }
}

@Composable
private fun GridContent(
    rows: Int,
    cols: Int,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    showLabels: Boolean,
    selectedCell: GridOverlay.GridCoordinate?,
    onCellClick: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.height(cellHeight)
            ) {
                repeat(cols) { col ->
                    GridCell(
                        modifier = Modifier
                            .width(cellWidth)
                            .fillMaxHeight(),
                        row = row,
                        col = col,
                        showLabel = showLabels,
                        isSelected = selectedCell?.row == row && selectedCell.col == col,
                        onClick = { onCellClick(row, col) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GridCell(
    modifier: Modifier = Modifier,
    row: Int,
    col: Int,
    showLabel: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val rowLabel = ('A' + row).toString()
    val colLabel = (col + 1).toString()
    val coordinate = "$rowLabel$colLabel"
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.6f else 0.1f,
        animationSpec = tween(300),
        label = "cell_selection"
    )
    
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f)
            )
            .background(
                color = if (isSelected) 
                    Color(0xFF4CAF50).copy(alpha = animatedAlpha)
                else 
                    Color.White.copy(alpha = 0.05f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Card(
                modifier = Modifier
                    .glassMorphism(
                        config = GlassMorphismConfig(
                            cornerRadius = 8.dp,
                            backgroundOpacity = if (isSelected) 0.3f else 0.15f,
                            borderOpacity = if (isSelected) 0.4f else 0.2f,
                            borderWidth = 1.dp,
                            tintColor = if (isSelected) Color(0xFF4CAF50) else Color(0xFF2196F3),
                            tintOpacity = if (isSelected) 0.4f else 0.2f
                        ),
                        depth = DepthLevel(if (isSelected) 0.8f else 0.4f)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Text(
                    text = coordinate,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InstructionsPanel(
    modifier: Modifier = Modifier,
    gridSize: String,
    onDismiss: () -> Unit,
    onToggleLabels: () -> Unit
) {
    Card(
        modifier = modifier
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.2f,
                    borderOpacity = 0.3f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF4285F4),
                    tintOpacity = 0.25f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Grid Navigation ($gridSize)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Say \"grid [coordinate]\" (e.g., \"grid A3\", \"grid B5\")",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            IconButton(
                onClick = onToggleLabels,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFF2196F3).copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = "Toggle Labels",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFFFF5722).copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hide Grid",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
        }
    }
}