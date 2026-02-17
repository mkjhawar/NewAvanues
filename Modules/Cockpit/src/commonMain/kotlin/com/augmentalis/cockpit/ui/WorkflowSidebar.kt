package com.augmentalis.cockpit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.display.DisplayProfile
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.CockpitFrame

/**
 * Step state for visual indicators in the workflow sidebar.
 */
enum class StepState {
    /** Step not yet reached */
    PENDING,
    /** Currently active step */
    ACTIVE,
    /** Step completed */
    COMPLETED
}

/**
 * Workflow sidebar with step navigation.
 *
 * Adapts to device form factor:
 * - **Tablet/Desktop**: 30/70 horizontal split — step list on left, active frame on right
 * - **Phone**: Bottom sheet overlay — active frame fills screen, step list in sheet
 *
 * Each step shows a number badge, title, and state indicator (pending/active/completed).
 * Tapping a step switches to that frame.
 *
 * @param frames All frames in workflow order
 * @param selectedFrameId Currently active frame
 * @param displayProfile Device profile for adaptive layout
 * @param onStepSelected Callback when user taps a step (passes frame ID)
 * @param frameContent Composable lambda that renders the active frame's content
 */
@Composable
fun WorkflowSidebar(
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    displayProfile: DisplayProfile,
    onStepSelected: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeFrame = frames.firstOrNull { it.id == selectedFrameId } ?: frames.firstOrNull()
    val activeIndex = frames.indexOfFirst { it.id == selectedFrameId }.coerceAtLeast(0)

    if (displayProfile == DisplayProfile.PHONE) {
        WorkflowPhoneLayout(
            frames = frames,
            activeFrame = activeFrame,
            activeIndex = activeIndex,
            onStepSelected = onStepSelected,
            frameContent = frameContent,
            modifier = modifier
        )
    } else {
        WorkflowTabletLayout(
            frames = frames,
            activeFrame = activeFrame,
            activeIndex = activeIndex,
            onStepSelected = onStepSelected,
            frameContent = frameContent,
            modifier = modifier
        )
    }
}

/**
 * Tablet/Desktop: 30/70 horizontal split.
 * Left panel = scrollable step list, right panel = active frame content.
 */
@Composable
private fun WorkflowTabletLayout(
    frames: List<CockpitFrame>,
    activeFrame: CockpitFrame?,
    activeIndex: Int,
    onStepSelected: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Row(modifier = modifier.fillMaxSize()) {
        // Step list panel (30%)
        StepListPanel(
            frames = frames,
            activeIndex = activeIndex,
            onStepSelected = onStepSelected,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .background(colors.surface.copy(alpha = 0.5f))
                .padding(8.dp)
        )

        // Divider
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(colors.border.copy(alpha = 0.3f))
        )

        // Active frame content (70%)
        if (activeFrame != null) {
            FrameWindow(
                frame = activeFrame,
                isSelected = true,
                isDraggable = false,
                isResizable = false,
                onSelect = {},
                onClose = {},
                onMinimize = {},
                onMaximize = {},
                stepNumber = activeIndex + 1,
                modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)
            ) {
                frameContent(activeFrame)
            }
        }
    }
}

/**
 * Phone: Bottom sheet with step list, main area shows active frame.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowPhoneLayout(
    frames: List<CockpitFrame>,
    activeFrame: CockpitFrame?,
    activeIndex: Int,
    onStepSelected: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 56.dp,
        sheetContainerColor = colors.surface.copy(alpha = 0.95f),
        containerColor = colors.background,
        sheetContent = {
            // Collapsed: step indicator dots
            StepIndicatorDots(
                totalSteps = frames.size,
                activeIndex = activeIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Expanded: full step list
            StepListPanel(
                frames = frames,
                activeIndex = activeIndex,
                onStepSelected = onStepSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(8.dp)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        // Main content area
        if (activeFrame != null) {
            FrameWindow(
                frame = activeFrame,
                isSelected = true,
                isDraggable = false,
                isResizable = false,
                onSelect = {},
                onClose = {},
                onMinimize = {},
                onMaximize = {},
                stepNumber = activeIndex + 1,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(4.dp)
            ) {
                frameContent(activeFrame)
            }
        }
    }
}

/**
 * Scrollable list of workflow steps with state indicators.
 */
@Composable
private fun StepListPanel(
    frames: List<CockpitFrame>,
    activeIndex: Int,
    onStepSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Column(modifier = modifier) {
        Text(
            text = "Steps",
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(frames) { index, frame ->
                val state = when {
                    index < activeIndex -> StepState.COMPLETED
                    index == activeIndex -> StepState.ACTIVE
                    else -> StepState.PENDING
                }

                StepRow(
                    stepNumber = index + 1,
                    title = frame.title,
                    state = state,
                    onClick = { onStepSelected(frame.id) }
                )
            }
        }
    }
}

/**
 * Individual step row with number badge, title, and state indicator.
 */
@Composable
private fun StepRow(
    stepNumber: Int,
    title: String,
    state: StepState,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors

    val badgeColor by animateColorAsState(
        targetValue = when (state) {
            StepState.COMPLETED -> colors.success
            StepState.ACTIVE -> colors.primary
            StepState.PENDING -> colors.border
        },
        label = "stepBadge"
    )

    val textAlpha = when (state) {
        StepState.ACTIVE -> 1f
        StepState.COMPLETED -> 0.7f
        StepState.PENDING -> 0.4f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (state == StepState.ACTIVE) {
                    Modifier.border(1.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                } else Modifier
            )
            .background(
                if (state == StepState.ACTIVE) colors.primary.copy(alpha = 0.1f)
                else colors.surface.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number badge
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(badgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                color = colors.onPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(10.dp))

        // Step title
        Text(
            text = title,
            color = colors.textPrimary.copy(alpha = textAlpha),
            fontSize = 13.sp,
            fontWeight = if (state == StepState.ACTIVE) FontWeight.Medium else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(6.dp))

        // State icon
        Icon(
            imageVector = when (state) {
                StepState.COMPLETED -> Icons.Default.CheckCircle
                StepState.ACTIVE -> Icons.Default.RadioButtonUnchecked
                StepState.PENDING -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = state.name,
            tint = badgeColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Compact step indicator dots for the phone bottom sheet collapsed state.
 */
@Composable
private fun StepIndicatorDots(
    totalSteps: Int,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Drag handle
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.border.copy(alpha = 0.4f))
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step ${activeIndex + 1} of $totalSteps",
                color = colors.textPrimary.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.width(8.dp))

            // Dot indicators (max 10 visible)
            val visibleCount = totalSteps.coerceAtMost(10)
            repeat(visibleCount) { i ->
                val dotColor by animateColorAsState(
                    targetValue = when {
                        i < activeIndex -> colors.success
                        i == activeIndex -> colors.primary
                        else -> colors.border.copy(alpha = 0.3f)
                    },
                    label = "dot$i"
                )
                Box(
                    modifier = Modifier
                        .size(if (i == activeIndex) 8.dp else 6.dp)
                        .background(dotColor, CircleShape)
                )
            }

            if (totalSteps > 10) {
                Text(
                    text = "+${totalSteps - 10}",
                    color = colors.textPrimary.copy(alpha = 0.3f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
