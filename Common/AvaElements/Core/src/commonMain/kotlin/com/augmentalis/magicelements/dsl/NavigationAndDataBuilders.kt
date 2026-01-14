package com.augmentalis.avaelements.dsl

import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.components.navigation.*
import com.augmentalis.avaelements.components.data.*

/**
 * DSL Builders for Phase 3.3 (Navigation) and 3.4 (Data Display) Components
 *
 * This file provides type-safe DSL builders for all navigation and data display components,
 * following the established AvaElements DSL pattern.
 */

// ==================== Navigation Component DSL Extensions ====================

/**
 * AppBar DSL extension for AvaUIScope
 */
fun AvaUIScope.AppBar(
    title: String,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (AppBarScope.() -> Unit)? = null
): AppBarComponent {
    val scope = AppBarScope(title, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * BottomNav DSL extension for AvaUIScope
 */
fun AvaUIScope.BottomNav(
    items: List<BottomNavItem>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (BottomNavScope.() -> Unit)? = null
): BottomNavComponent {
    val scope = BottomNavScope(items, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Tabs DSL extension for AvaUIScope
 */
fun AvaUIScope.Tabs(
    tabs: List<Tab>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (TabsScope.() -> Unit)? = null
): TabsComponent {
    val scope = TabsScope(tabs, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Drawer DSL extension for AvaUIScope
 */
fun AvaUIScope.Drawer(
    isOpen: Boolean = false,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (DrawerScope.() -> Unit)? = null
): DrawerComponent {
    val scope = DrawerScope(isOpen, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Breadcrumb DSL extension for AvaUIScope
 */
fun AvaUIScope.Breadcrumb(
    items: List<BreadcrumbItem>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (BreadcrumbScope.() -> Unit)? = null
): BreadcrumbComponent {
    val scope = BreadcrumbScope(items, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Pagination DSL extension for AvaUIScope
 */
fun AvaUIScope.Pagination(
    totalPages: Int,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (PaginationScope.() -> Unit)? = null
): PaginationComponent {
    val scope = PaginationScope(totalPages, id, style)
    builder?.invoke(scope)
    return scope.build()
}

// ==================== Data Display Component DSL Extensions ====================

/**
 * Table DSL extension for AvaUIScope
 */
fun AvaUIScope.Table(
    columns: List<TableColumn>,
    rows: List<TableRow>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (TableScope.() -> Unit)? = null
): TableComponent {
    val scope = TableScope(columns, rows, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * List DSL extension for AvaUIScope
 */
fun AvaUIScope.List(
    items: List<ListItem>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (ListScope.() -> Unit)? = null
): ListComponent {
    val scope = ListScope(items, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Accordion DSL extension for AvaUIScope
 */
fun AvaUIScope.Accordion(
    items: List<AccordionItem>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (AccordionScope.() -> Unit)? = null
): AccordionComponent {
    val scope = AccordionScope(items, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Stepper DSL extension for AvaUIScope
 */
fun AvaUIScope.Stepper(
    steps: List<Step>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (StepperScope.() -> Unit)? = null
): StepperComponent {
    val scope = StepperScope(steps, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Timeline DSL extension for AvaUIScope
 */
fun AvaUIScope.Timeline(
    items: List<TimelineItem>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (TimelineScope.() -> Unit)? = null
): TimelineComponent {
    val scope = TimelineScope(items, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * TreeView DSL extension for AvaUIScope
 */
fun AvaUIScope.TreeView(
    nodes: List<TreeNode>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (TreeViewScope.() -> Unit)? = null
): TreeViewComponent {
    val scope = TreeViewScope(nodes, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Carousel DSL extension for AvaUIScope
 */
fun AvaUIScope.Carousel(
    items: List<Component>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (CarouselScope.() -> Unit)? = null
): CarouselComponent {
    val scope = CarouselScope(items, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Avatar DSL extension for AvaUIScope
 */
fun AvaUIScope.Avatar(
    id: String? = null,
    style: ComponentStyle? = null,
    builder: AvatarScope.() -> Unit
): AvatarComponent {
    val scope = AvatarScope(id, style)
    scope.builder()
    return scope.build()
}

/**
 * Chip DSL extension for AvaUIScope
 */
fun AvaUIScope.Chip(
    label: String,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (ChipScope.() -> Unit)? = null
): ChipComponent {
    val scope = ChipScope(label, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Divider DSL extension for AvaUIScope
 */
fun AvaUIScope.Divider(
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (DividerScope.() -> Unit)? = null
): DividerComponent {
    val scope = DividerScope(id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * Paper DSL extension for AvaUIScope
 */
fun AvaUIScope.Paper(
    id: String? = null,
    style: ComponentStyle? = null,
    builder: PaperScope.() -> Unit
): PaperComponent {
    val scope = PaperScope(id, style)
    scope.builder()
    return scope.build()
}

/**
 * Skeleton DSL extension for AvaUIScope
 */
fun AvaUIScope.Skeleton(
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (SkeletonScope.() -> Unit)? = null
): SkeletonComponent {
    val scope = SkeletonScope(id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * EmptyState DSL extension for AvaUIScope
 */
fun AvaUIScope.EmptyState(
    title: String,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (EmptyStateScope.() -> Unit)? = null
): EmptyStateComponent {
    val scope = EmptyStateScope(title, id, style)
    builder?.invoke(scope)
    return scope.build()
}

/**
 * DataGrid DSL extension for AvaUIScope
 */
fun AvaUIScope.DataGrid(
    columns: List<DataGridColumn>,
    rows: List<DataGridRow>,
    id: String? = null,
    style: ComponentStyle? = null,
    builder: (DataGridScope.() -> Unit)? = null
): DataGridComponent {
    val scope = DataGridScope(columns, rows, id, style)
    builder?.invoke(scope)
    return scope.build()
}

// ==================== Navigation Component Scopes ====================

class AppBarScope(
    private val title: String,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var navigationIcon: String? = null
    var actions: List<AppBarAction> = emptyList()
    var elevation: Int = 1
    var onNavigationClick: (() -> Unit)? = null

    fun action(icon: String, label: String? = null, onClick: () -> Unit) {
        actions = actions + AppBarAction(icon, label, onClick)
    }

    internal fun build() = AppBarComponent(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        elevation = elevation,
        id = id,
        style = style,
        modifiers = modifiers,
        onNavigationClick = onNavigationClick
    )
}

class BottomNavScope(
    private val items: List<BottomNavItem>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var selectedIndex: Int = 0
    var onItemSelected: ((Int) -> Unit)? = null

    internal fun build() = BottomNavComponent(
        items = items,
        selectedIndex = selectedIndex,
        id = id,
        style = style,
        modifiers = modifiers,
        onItemSelected = onItemSelected
    )
}

class TabsScope(
    private val tabs: List<Tab>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var selectedIndex: Int = 0
    var onTabSelected: ((Int) -> Unit)? = null

    internal fun build() = TabsComponent(
        tabs = tabs,
        selectedIndex = selectedIndex,
        id = id,
        style = style,
        modifiers = modifiers,
        onTabSelected = onTabSelected
    )
}

class DrawerScope(
    private val isOpen: Boolean,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var position: DrawerPosition = DrawerPosition.Left
    var header: Component? = null
    var items: List<DrawerItem> = emptyList()
    var footer: Component? = null
    var onItemClick: ((String) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    fun item(id: String, label: String, icon: String? = null, badge: String? = null) {
        items = items + DrawerItem(id, icon, label, badge)
    }

    internal fun build() = DrawerComponent(
        isOpen = isOpen,
        position = position,
        header = header,
        items = items,
        footer = footer,
        id = id,
        style = style,
        modifiers = modifiers,
        onItemClick = onItemClick,
        onDismiss = onDismiss
    )
}

class BreadcrumbScope(
    private val items: List<BreadcrumbItem>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var separator: String = "/"

    internal fun build() = BreadcrumbComponent(
        items = items,
        separator = separator,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

class PaginationScope(
    private val totalPages: Int,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var currentPage: Int = 1
    var showFirstLast: Boolean = true
    var showPrevNext: Boolean = true
    var maxVisible: Int = 7
    var onPageChange: ((Int) -> Unit)? = null

    internal fun build() = PaginationComponent(
        currentPage = currentPage,
        totalPages = totalPages,
        showFirstLast = showFirstLast,
        showPrevNext = showPrevNext,
        maxVisible = maxVisible,
        id = id,
        style = style,
        modifiers = modifiers,
        onPageChange = onPageChange
    )
}

// ==================== Data Display Component Scopes ====================

class TableScope(
    private val columns: List<TableColumn>,
    private val rows: List<TableRow>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var sortable: Boolean = false
    var hoverable: Boolean = true
    var striped: Boolean = false
    var onRowClick: ((Int) -> Unit)? = null

    internal fun build() = TableComponent(
        columns = columns,
        rows = rows,
        sortable = sortable,
        hoverable = hoverable,
        striped = striped,
        id = id,
        style = style,
        modifiers = modifiers,
        onRowClick = onRowClick
    )
}

class ListScope(
    private val items: List<ListItem>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var selectable: Boolean = false
    var selectedIndices: Set<Int> = emptySet()
    var onItemClick: ((Int) -> Unit)? = null

    internal fun build() = ListComponent(
        items = items,
        selectable = selectable,
        selectedIndices = selectedIndices,
        id = id,
        style = style,
        modifiers = modifiers,
        onItemClick = onItemClick
    )
}

class AccordionScope(
    private val items: List<AccordionItem>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var expandedIndices: Set<Int> = emptySet()
    var allowMultiple: Boolean = false
    var onToggle: ((Int) -> Unit)? = null

    internal fun build() = AccordionComponent(
        items = items,
        expandedIndices = expandedIndices,
        allowMultiple = allowMultiple,
        id = id,
        style = style,
        modifiers = modifiers,
        onToggle = onToggle
    )
}

class StepperScope(
    private val steps: List<Step>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var currentStep: Int = 0
    var orientation: Orientation = Orientation.Horizontal
    var onStepClick: ((Int) -> Unit)? = null

    internal fun build() = StepperComponent(
        steps = steps,
        currentStep = currentStep,
        orientation = orientation,
        id = id,
        style = style,
        modifiers = modifiers,
        onStepClick = onStepClick
    )
}

class TimelineScope(
    private val items: List<TimelineItem>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var orientation: Orientation = Orientation.Vertical

    internal fun build() = TimelineComponent(
        items = items,
        orientation = orientation,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

class TreeViewScope(
    private val nodes: List<TreeNode>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var expandedIds: Set<String> = emptySet()
    var onNodeClick: ((String) -> Unit)? = null
    var onToggle: ((String) -> Unit)? = null

    internal fun build() = TreeViewComponent(
        nodes = nodes,
        expandedIds = expandedIds,
        id = id,
        style = style,
        modifiers = modifiers,
        onNodeClick = onNodeClick,
        onToggle = onToggle
    )
}

class CarouselScope(
    private val items: List<Component>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var currentIndex: Int = 0
    var autoPlay: Boolean = false
    var interval: Long = 3000
    var showIndicators: Boolean = true
    var showControls: Boolean = true
    var onSlideChange: ((Int) -> Unit)? = null

    internal fun build() = CarouselComponent(
        items = items,
        currentIndex = currentIndex,
        autoPlay = autoPlay,
        interval = interval,
        showIndicators = showIndicators,
        showControls = showControls,
        id = id,
        style = style,
        modifiers = modifiers,
        onSlideChange = onSlideChange
    )
}

class AvatarScope(
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var source: String? = null
    var text: String? = null
    var size: AvatarSize = AvatarSize.Medium
    var shape: AvatarShape = AvatarShape.Circle

    internal fun build() = AvatarComponent(
        source = source,
        text = text,
        size = size,
        shape = shape,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

class ChipScope(
    private val label: String,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var icon: String? = null
    var deletable: Boolean = false
    var selected: Boolean = false
    var onClick: (() -> Unit)? = null
    var onDelete: (() -> Unit)? = null

    internal fun build() = ChipComponent(
        label = label,
        icon = icon,
        deletable = deletable,
        selected = selected,
        id = id,
        style = style,
        modifiers = modifiers,
        onClick = onClick,
        onDelete = onDelete
    )
}

class DividerScope(
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var orientation: Orientation = Orientation.Horizontal
    var thickness: Float = 1f
    var text: String? = null

    internal fun build() = DividerComponent(
        orientation = orientation,
        thickness = thickness,
        text = text,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

class PaperScope(
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var elevation: Int = 1
    private val children = mutableListOf<Component>()

    fun Text(text: String, builder: (TextScope.() -> Unit)? = null) {
        val scope = TextScope(text)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Button(text: String, builder: (ButtonScope.() -> Unit)? = null) {
        val scope = ButtonScope(text)
        builder?.invoke(scope)
        children.add(scope.build())
    }

    fun Column(builder: ColumnScope.() -> Unit) {
        val scope = ColumnScope(null, null)
        scope.builder()
        children.add(scope.build())
    }

    fun Row(builder: RowScope.() -> Unit) {
        val scope = RowScope(null, null)
        scope.builder()
        children.add(scope.build())
    }

    internal fun build() = PaperComponent(
        elevation = elevation,
        children = children,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

class SkeletonScope(
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var variant: SkeletonVariant = SkeletonVariant.Text
    var width: Size? = null
    var height: Size? = null
    var animation: SkeletonAnimation = SkeletonAnimation.Pulse

    internal fun build() = SkeletonComponent(
        variant = variant,
        width = width,
        height = height,
        animation = animation,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

class EmptyStateScope(
    private val title: String,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var icon: String? = null
    var description: String? = null
    var action: Component? = null

    internal fun build() = EmptyStateComponent(
        icon = icon,
        title = title,
        description = description,
        action = action,
        id = id,
        style = style,
        modifiers = modifiers
    )
}

class DataGridScope(
    private val columns: List<DataGridColumn>,
    private val rows: List<DataGridRow>,
    private val id: String?,
    private val style: ComponentStyle?
) : ComponentScope() {
    var pageSize: Int = 10
    var currentPage: Int = 1
    var sortBy: String? = null
    var sortOrder: SortOrder = SortOrder.Ascending
    var selectable: Boolean = false
    var selectedIds: Set<String> = emptySet()
    var onSort: ((String, SortOrder) -> Unit)? = null
    var onPageChange: ((Int) -> Unit)? = null
    var onSelectionChange: ((Set<String>) -> Unit)? = null

    internal fun build() = DataGridComponent(
        columns = columns,
        rows = rows,
        pageSize = pageSize,
        currentPage = currentPage,
        sortBy = sortBy,
        sortOrder = sortOrder,
        selectable = selectable,
        selectedIds = selectedIds,
        id = id,
        style = style,
        modifiers = modifiers,
        onSort = onSort,
        onPageChange = onPageChange,
        onSelectionChange = onSelectionChange
    )
}

// ==================== Helper Builder Functions ====================

/**
 * Helper to create BottomNavItem instances
 */
fun bottomNavItem(icon: String, label: String, badge: String? = null) =
    BottomNavItem(icon, label, badge)

/**
 * Helper to create Tab instances
 */
fun tab(label: String, icon: String? = null, content: Component? = null) =
    Tab(label, icon, content)

/**
 * Helper to create DrawerItem instances
 */
fun drawerItem(id: String, label: String, icon: String? = null, badge: String? = null) =
    DrawerItem(id, icon, label, badge)

/**
 * Helper to create BreadcrumbItem instances
 */
fun breadcrumbItem(label: String, href: String? = null, onClick: (() -> Unit)? = null) =
    BreadcrumbItem(label, href, onClick)

/**
 * Helper to create TableColumn instances
 */
fun tableColumn(id: String, label: String, sortable: Boolean = false, width: Size? = null) =
    TableColumn(id, label, sortable, width)

/**
 * Helper to create TableRow instances
 */
fun tableRow(id: String, cells: List<TableCell>) =
    TableRow(id, cells)

/**
 * Helper to create TableCell instances
 */
fun tableCell(content: String, component: Component? = null) =
    TableCell(content, component)

/**
 * Helper to create ListItem instances
 */
fun listItem(
    id: String,
    primary: String,
    secondary: String? = null,
    icon: String? = null,
    avatar: String? = null,
    trailing: Component? = null
) = ListItem(id, primary, secondary, icon, avatar, trailing)

/**
 * Helper to create AccordionItem instances
 */
fun accordionItem(id: String, title: String, content: Component) =
    AccordionItem(id, title, content)

/**
 * Helper to create Step instances
 */
fun step(label: String, description: String? = null, status: StepStatus = StepStatus.Pending) =
    Step(label, description, status)

/**
 * Helper to create TimelineItem instances
 */
fun timelineItem(
    id: String,
    timestamp: String,
    title: String,
    description: String? = null,
    icon: String? = null,
    color: Color? = null
) = TimelineItem(id, timestamp, title, description, icon, color)

/**
 * Helper to create TreeNode instances
 */
fun treeNode(id: String, label: String, icon: String? = null, children: List<TreeNode> = emptyList()) =
    TreeNode(id, label, icon, children)

/**
 * Helper to create DataGridColumn instances
 */
fun dataGridColumn(
    id: String,
    label: String,
    sortable: Boolean = true,
    width: Size? = null,
    align: TextAlign = TextAlign.Start
) = DataGridColumn(id, label, sortable, width, align)

/**
 * Helper to create DataGridRow instances
 */
fun dataGridRow(id: String, cells: Map<String, Any>) =
    DataGridRow(id, cells)

/**
 * Helper to create AppBarAction instances
 */
fun appBarAction(icon: String, label: String? = null, onClick: () -> Unit) =
    AppBarAction(icon, label, onClick)
