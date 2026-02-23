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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.augmentalis.cockpit.model.PanelRole
import kotlinx.coroutines.launch

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
 * Workflow sidebar with step navigation and multi-pane support.
 *
 * Adapts to device form factor AND content composition:
 *
 * **2-panel mode** (no AUXILIARY frames):
 * - Tablet/Desktop: 30/70 horizontal split — steps left, content right
 * - Phone: Bottom sheet overlay — content fills screen, steps in sheet
 *
 * **3-panel mode** (when any frame has [PanelRole.AUXILIARY]):
 * - Tablet/Desktop: 20/60/20 horizontal split — steps, content, auxiliary
 * - Phone: Tab navigation with swipe — Steps | Content | Auxiliary
 *
 * Each panel gets its own [FrameWindow] with working traffic light controls.
 * The 3-panel mode activates automatically when any frame has AUXILIARY role.
 *
 * @param frames All frames in workflow order
 * @param selectedFrameId Currently active frame
 * @param displayProfile Device profile for adaptive layout
 * @param onStepSelected Callback when user taps a step (passes frame ID)
 * @param onFrameClose Callback when user closes a frame via traffic lights
 * @param onFrameMinimize Callback when user minimizes a frame
 * @param onFrameMaximize Callback when user maximizes/restores a frame
 * @param onStepRenamed Callback when step title is edited (frameId, newTitle)
 * @param onStepReordered Callback when step is moved up/down (frameId, delta: -1 or +1)
 * @param onStepDeleted Callback when step is removed (frameId)
 * @param frameContent Composable lambda that renders the active frame's content
 */
@Composable
fun WorkflowSidebar(
    frames: List<CockpitFrame>,
    selectedFrameId: String?,
    displayProfile: DisplayProfile,
    onStepSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit = {},
    onFrameMinimize: (String) -> Unit = {},
    onFrameMaximize: (String) -> Unit = {},
    onStepRenamed: (String, String) -> Unit = { _, _ -> },
    onStepReordered: (String, Int) -> Unit = { _, _ -> },
    onStepDeleted: (String) -> Unit = {},
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeFrame = frames.firstOrNull { it.id == selectedFrameId } ?: frames.firstOrNull()
    val activeIndex = frames.indexOfFirst { it.id == selectedFrameId }.coerceAtLeast(0)
    val hasAuxiliary = frames.any { it.panelRole == PanelRole.AUXILIARY }
    val auxiliaryFrame = frames.firstOrNull { it.panelRole == PanelRole.AUXILIARY }

    if (displayProfile == DisplayProfile.PHONE) {
        if (hasAuxiliary && auxiliaryFrame != null) {
            WorkflowPhoneTabLayout(
                frames = frames,
                activeFrame = activeFrame,
                auxiliaryFrame = auxiliaryFrame,
                activeIndex = activeIndex,
                onStepSelected = onStepSelected,
                onFrameClose = onFrameClose,
                onFrameMinimize = onFrameMinimize,
                onFrameMaximize = onFrameMaximize,
                onStepRenamed = onStepRenamed,
                onStepReordered = onStepReordered,
                onStepDeleted = onStepDeleted,
                frameContent = frameContent,
                modifier = modifier
            )
        } else {
            WorkflowPhoneLayout(
                frames = frames,
                activeFrame = activeFrame,
                activeIndex = activeIndex,
                onStepSelected = onStepSelected,
                onFrameClose = onFrameClose,
                onFrameMinimize = onFrameMinimize,
                onFrameMaximize = onFrameMaximize,
                onStepRenamed = onStepRenamed,
                onStepReordered = onStepReordered,
                onStepDeleted = onStepDeleted,
                frameContent = frameContent,
                modifier = modifier
            )
        }
    } else {
        if (hasAuxiliary && auxiliaryFrame != null) {
            WorkflowTriPanelLayout(
                frames = frames,
                activeFrame = activeFrame,
                auxiliaryFrame = auxiliaryFrame,
                activeIndex = activeIndex,
                onStepSelected = onStepSelected,
                onFrameClose = onFrameClose,
                onFrameMinimize = onFrameMinimize,
                onFrameMaximize = onFrameMaximize,
                onStepRenamed = onStepRenamed,
                onStepReordered = onStepReordered,
                onStepDeleted = onStepDeleted,
                frameContent = frameContent,
                modifier = modifier
            )
        } else {
            WorkflowTabletLayout(
                frames = frames,
                activeFrame = activeFrame,
                activeIndex = activeIndex,
                onStepSelected = onStepSelected,
                onFrameClose = onFrameClose,
                onFrameMinimize = onFrameMinimize,
                onFrameMaximize = onFrameMaximize,
                onStepRenamed = onStepRenamed,
                onStepReordered = onStepReordered,
                onStepDeleted = onStepDeleted,
                frameContent = frameContent,
                modifier = modifier
            )
        }
    }
}

// ── Tablet/Desktop Layouts ───────────────────────────────────────────

/**
 * Tablet/Desktop: 30/70 horizontal split (2-panel, no auxiliary).
 * Left panel = scrollable step list, right panel = active frame content.
 */
@Composable
private fun WorkflowTabletLayout(
    frames: List<CockpitFrame>,
    activeFrame: CockpitFrame?,
    activeIndex: Int,
    onStepSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    onStepRenamed: (String, String) -> Unit = { _, _ -> },
    onStepReordered: (String, Int) -> Unit = { _, _ -> },
    onStepDeleted: (String) -> Unit = {},
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
            onStepRenamed = onStepRenamed,
            onStepReordered = onStepReordered,
            onStepDeleted = onStepDeleted,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .background(colors.surface.copy(alpha = 0.5f))
                .padding(8.dp)
        )

        // Divider
        VerticalDivider()

        // Active frame content (70%)
        if (activeFrame != null) {
            FrameWindow(
                frame = activeFrame,
                isSelected = true,
                isDraggable = false,
                isResizable = false,
                onSelect = {},
                onClose = { onFrameClose(activeFrame.id) },
                onMinimize = { onFrameMinimize(activeFrame.id) },
                onMaximize = { onFrameMaximize(activeFrame.id) },
                stepNumber = activeIndex + 1,
                modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)
            ) {
                frameContent(activeFrame)
            }
        }
    }
}

/**
 * Tablet/Desktop: 20/60/20 horizontal split (3-panel with auxiliary).
 *
 * ```
 * ┌──────────┬────────────────────────┬──────────┐
 * │  Steps   │    Main Content        │ Auxiliary │
 * │  (20%)   │    (60%)               │  (20%)   │
 * │  1. ●    │    [Pictures]          │  Video   │
 * │  2. ○    │    [Instructions]      │  Call    │
 * │  3. ○    │                        │  Notes   │
 * └──────────┴────────────────────────┴──────────┘
 * ```
 *
 * Each of the 3 panels has its own FrameWindow with traffic lights.
 */
@Composable
private fun WorkflowTriPanelLayout(
    frames: List<CockpitFrame>,
    activeFrame: CockpitFrame?,
    auxiliaryFrame: CockpitFrame,
    activeIndex: Int,
    onStepSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    onStepRenamed: (String, String) -> Unit = { _, _ -> },
    onStepReordered: (String, Int) -> Unit = { _, _ -> },
    onStepDeleted: (String) -> Unit = {},
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Row(modifier = modifier.fillMaxSize()) {
        // Left panel — Steps (20%)
        StepListPanel(
            frames = frames.filter { it.panelRole != PanelRole.AUXILIARY },
            activeIndex = activeIndex,
            onStepSelected = onStepSelected,
            onStepRenamed = onStepRenamed,
            onStepReordered = onStepReordered,
            onStepDeleted = onStepDeleted,
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.2f)
                .background(colors.surface.copy(alpha = 0.5f))
                .padding(8.dp)
        )

        VerticalDivider()

        // Center panel — Main Content (60%)
        if (activeFrame != null) {
            FrameWindow(
                frame = activeFrame,
                isSelected = true,
                isDraggable = false,
                isResizable = false,
                onSelect = {},
                onClose = { onFrameClose(activeFrame.id) },
                onMinimize = { onFrameMinimize(activeFrame.id) },
                onMaximize = { onFrameMaximize(activeFrame.id) },
                stepNumber = activeIndex + 1,
                modifier = Modifier.weight(0.6f).fillMaxHeight().padding(4.dp)
            ) {
                frameContent(activeFrame)
            }
        }

        VerticalDivider()

        // Right panel — Auxiliary (20%)
        FrameWindow(
            frame = auxiliaryFrame,
            isSelected = false,
            isDraggable = false,
            isResizable = false,
            onSelect = {},
            onClose = { onFrameClose(auxiliaryFrame.id) },
            onMinimize = { onFrameMinimize(auxiliaryFrame.id) },
            onMaximize = { onFrameMaximize(auxiliaryFrame.id) },
            modifier = Modifier.weight(0.2f).fillMaxHeight().padding(4.dp)
        ) {
            frameContent(auxiliaryFrame)
        }
    }
}

// ── Phone Layouts ────────────────────────────────────────────────────

/**
 * Phone: Bottom sheet with step list, main area shows active frame.
 * Used when no AUXILIARY frames exist (2-panel equivalent).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkflowPhoneLayout(
    frames: List<CockpitFrame>,
    activeFrame: CockpitFrame?,
    activeIndex: Int,
    onStepSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    onStepRenamed: (String, String) -> Unit = { _, _ -> },
    onStepReordered: (String, Int) -> Unit = { _, _ -> },
    onStepDeleted: (String) -> Unit = {},
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
                onStepRenamed = onStepRenamed,
                onStepReordered = onStepReordered,
                onStepDeleted = onStepDeleted,
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
                onClose = { onFrameClose(activeFrame.id) },
                onMinimize = { onFrameMinimize(activeFrame.id) },
                onMaximize = { onFrameMaximize(activeFrame.id) },
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
 * Phone: Tab navigation with HorizontalPager (3-panel equivalent).
 *
 * Three tabs at the top: Steps | Content | Auxiliary.
 * User can swipe between pages or tap tabs.
 * Used when AUXILIARY frames exist and the display is phone-sized.
 */
@Composable
private fun WorkflowPhoneTabLayout(
    frames: List<CockpitFrame>,
    activeFrame: CockpitFrame?,
    auxiliaryFrame: CockpitFrame,
    activeIndex: Int,
    onStepSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMinimize: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    onStepRenamed: (String, String) -> Unit = { _, _ -> },
    onStepReordered: (String, Int) -> Unit = { _, _ -> },
    onStepDeleted: (String) -> Unit = {},
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 1, // Start on Content tab
        pageCount = { 3 }
    )
    val tabTitles = listOf("Steps", "Content", "Auxiliary")

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = colors.surface.copy(alpha = 0.9f),
            contentColor = colors.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (pagerState.currentPage == index)
                                FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = colors.primary,
                    unselectedContentColor = colors.textPrimary.copy(alpha = 0.5f)
                )
            }
        }

        // Pager pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            when (page) {
                // Page 0: Steps
                0 -> StepListPanel(
                    frames = frames.filter { it.panelRole != PanelRole.AUXILIARY },
                    activeIndex = activeIndex,
                    onStepSelected = { id ->
                        onStepSelected(id)
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    onStepRenamed = onStepRenamed,
                    onStepReordered = onStepReordered,
                    onStepDeleted = onStepDeleted,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
                // Page 1: Content
                1 -> {
                    if (activeFrame != null) {
                        FrameWindow(
                            frame = activeFrame,
                            isSelected = true,
                            isDraggable = false,
                            isResizable = false,
                            onSelect = {},
                            onClose = { onFrameClose(activeFrame.id) },
                            onMinimize = { onFrameMinimize(activeFrame.id) },
                            onMaximize = { onFrameMaximize(activeFrame.id) },
                            stepNumber = activeIndex + 1,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        ) {
                            frameContent(activeFrame)
                        }
                    }
                }
                // Page 2: Auxiliary
                2 -> {
                    FrameWindow(
                        frame = auxiliaryFrame,
                        isSelected = false,
                        isDraggable = false,
                        isResizable = false,
                        onSelect = {},
                        onClose = { onFrameClose(auxiliaryFrame.id) },
                        onMinimize = { onFrameMinimize(auxiliaryFrame.id) },
                        onMaximize = { onFrameMaximize(auxiliaryFrame.id) },
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    ) {
                        frameContent(auxiliaryFrame)
                    }
                }
            }
        }

        // Step indicator dots at bottom
        StepIndicatorDots(
            totalSteps = frames.filter { it.panelRole != PanelRole.AUXILIARY }.size,
            activeIndex = activeIndex,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ── Shared Components ────────────────────────────────────────────────

/**
 * Thin vertical divider line between panels.
 */
@Composable
private fun VerticalDivider(modifier: Modifier = Modifier) {
    val colors = AvanueTheme.colors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(colors.border.copy(alpha = 0.3f))
    )
}

/**
 * Scrollable list of workflow steps with state indicators and CRUD controls.
 */
@Composable
private fun StepListPanel(
    frames: List<CockpitFrame>,
    activeIndex: Int,
    onStepSelected: (String) -> Unit,
    onStepRenamed: (String, String) -> Unit = { _, _ -> },
    onStepReordered: (String, Int) -> Unit = { _, _ -> },
    onStepDeleted: (String) -> Unit = {},
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
                    isFirst = index == 0,
                    isLast = index == frames.lastIndex,
                    onClick = { onStepSelected(frame.id) },
                    onRename = { newTitle -> onStepRenamed(frame.id, newTitle) },
                    onMoveUp = { onStepReordered(frame.id, -1) },
                    onMoveDown = { onStepReordered(frame.id, 1) },
                    onDelete = { onStepDeleted(frame.id) }
                )
            }
        }
    }
}

/**
 * Individual step row with number badge, title, state indicator, and CRUD controls.
 *
 * Tap the row to select. When the active step is selected, an edit row appears
 * with rename field, reorder arrows, and delete button.
 */
@Composable
private fun StepRow(
    stepNumber: Int,
    title: String,
    state: StepState,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onRename: (String) -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val colors = AvanueTheme.colors
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(title) { mutableStateOf(title) }

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

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (state == StepState.ACTIVE) {
                        Modifier.border(
                            1.dp,
                            colors.primary.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
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

            // Step title or edit field
            if (isEditing) {
                BasicTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            colors.surfaceInput.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            } else {
                Text(
                    text = title,
                    color = colors.textPrimary.copy(alpha = textAlpha),
                    fontSize = 13.sp,
                    fontWeight = if (state == StepState.ACTIVE) FontWeight.Medium
                    else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.width(6.dp))

            // Edit/save toggle
            if (state == StepState.ACTIVE) {
                if (isEditing) {
                    // Save + cancel
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Voice: click Save",
                        tint = colors.success,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                onRename(editText)
                                isEditing = false
                            }
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Voice: click Cancel",
                        tint = colors.textTertiary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                editText = title
                                isEditing = false
                            }
                    )
                } else {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Voice: click Edit",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { isEditing = true }
                    )
                }
            } else {
                // State icon (non-active steps)
                Icon(
                    imageVector = when (state) {
                        StepState.COMPLETED -> Icons.Default.CheckCircle
                        else -> Icons.Default.RadioButtonUnchecked
                    },
                    contentDescription = state.name,
                    tint = badgeColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Action row (visible only for active step)
        if (state == StepState.ACTIVE && !isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 34.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Move up
                if (!isFirst) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Voice: click Move Up",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(colors.surface.copy(alpha = 0.5f))
                            .clickable(onClick = onMoveUp)
                            .padding(2.dp)
                    )
                }

                // Move down
                if (!isLast) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Voice: click Move Down",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(colors.surface.copy(alpha = 0.5f))
                            .clickable(onClick = onMoveDown)
                            .padding(2.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                // Delete
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Voice: click Delete Step",
                    tint = colors.error.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.5f))
                        .clickable(onClick = onDelete)
                        .padding(2.dp)
                )
            }
        }
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
