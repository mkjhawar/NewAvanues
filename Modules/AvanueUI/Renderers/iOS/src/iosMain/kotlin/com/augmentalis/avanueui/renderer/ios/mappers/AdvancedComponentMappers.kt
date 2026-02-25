package com.augmentalis.avanueui.renderer.ios.mappers

import com.augmentalis.avanueui.core.Theme
import com.augmentalis.avanueui.dsl.*
import com.augmentalis.avanueui.ui.core.form.*
import com.augmentalis.avanueui.ui.core.display.*
import com.augmentalis.avanueui.ui.core.layout.*
import com.augmentalis.avanueui.ui.core.feedback.*
import com.augmentalis.avanueui.ui.core.data.TableComponent
import com.augmentalis.avanueui.core.Orientation
import com.augmentalis.avanueui.core.Severity
import com.augmentalis.avanueui.core.Position
import com.augmentalis.avanueui.renderer.ios.bridge.*

/**
 * Advanced Component Mappers for iOS (SwiftUI)
 *
 * Maps advanced components to reach parity with Android:
 * - Button variants (5)
 * - Layout components (6)
 * - Feedback components (2)
 * - Display components (1)
 */

// ======================
// Button Variants
// ======================

object SegmentedButtonMapper {
    fun map(
        component: SegmentedButtonComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Picker,
            id = component.id,
            properties = mapOf(
                "selection" to component.selectedIndices.firstOrNull(),
                "pickerStyle" to "SegmentedPickerStyle()"
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.segments.mapIndexed { index, segment ->
                SwiftUIView(
                    type = ViewType.Text,
                    properties = mapOf(
                        "text" to segment.label,
                        "tag" to index
                    )
                )
            }
        )
    }
}

object TextButtonMapper {
    fun map(component: TextButtonComponent, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        if (component.icon != null && component.iconPosition == IconPosition.Start) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to convertToSFSymbol(component.icon)
                    )
                )
            )
        }

        children.add(
            SwiftUIView(
                type = ViewType.Text,
                properties = mapOf("text" to component.label)
            )
        )

        if (component.icon != null && component.iconPosition == IconPosition.End) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to convertToSFSymbol(component.icon)
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Button,
            id = component.id,
            properties = mapOf(
                "action" to "textButtonTapped",
                "buttonStyle" to "PlainButtonStyle()"
            ),
            modifiers = listOf(
                SwiftUIModifier.ForegroundColor(SwiftUIColor(0.2f, 0.4f, 0.8f, 1f))
            ) + ModifierConverter.convert(component.modifiers),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 8),
                    children = children
                )
            )
        )
    }

    private fun convertToSFSymbol(icon: String?): String {
        return icon?.lowercase() ?: "circle"
    }
}

object OutlinedButtonMapper {
    fun map(component: OutlinedButtonComponent, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        if (component.icon != null && component.iconPosition == IconPosition.Start) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to convertToSFSymbol(component.icon)
                    )
                )
            )
        }

        children.add(
            SwiftUIView(
                type = ViewType.Text,
                properties = mapOf("text" to component.label)
            )
        )

        if (component.icon != null && component.iconPosition == IconPosition.End) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to convertToSFSymbol(component.icon)
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Button,
            id = component.id,
            properties = mapOf(
                "action" to "outlinedButtonTapped"
            ),
            modifiers = listOf(
                SwiftUIModifier.Padding(horizontal = 16f, vertical = 8f),
                SwiftUIModifier.ForegroundColor(SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)),
                SwiftUIModifier.Overlay(
                    shape = "RoundedRectangle(cornerRadius: 8)",
                    stroke = "Color.accentColor",
                    lineWidth = 1f
                )
            ) + ModifierConverter.convert(component.modifiers),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 8),
                    children = children
                )
            )
        )
    }

    private fun convertToSFSymbol(icon: String?): String {
        return icon?.lowercase() ?: "circle"
    }
}

object FilledButtonMapper {
    fun map(component: FilledButtonComponent, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        if (component.icon != null && component.iconPosition == IconPosition.Start) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to convertToSFSymbol(component.icon)
                    )
                )
            )
        }

        children.add(
            SwiftUIView(
                type = ViewType.Text,
                properties = mapOf("text" to component.label)
            )
        )

        if (component.icon != null && component.iconPosition == IconPosition.End) {
            children.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to convertToSFSymbol(component.icon)
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Button,
            id = component.id,
            properties = mapOf(
                "action" to "filledButtonTapped"
            ),
            modifiers = listOf(
                SwiftUIModifier.Padding(horizontal = 16f, vertical = 8f),
                SwiftUIModifier.Background(SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)),
                SwiftUIModifier.ForegroundColor(SwiftUIColor(1f, 1f, 1f, 1f)),
                SwiftUIModifier.CornerRadius(8f)
            ) + ModifierConverter.convert(component.modifiers),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 8),
                    children = children
                )
            )
        )
    }

    private fun convertToSFSymbol(icon: String?): String {
        return icon?.lowercase() ?: "circle"
    }
}

object IconButtonMapper {
    fun map(component: IconButtonComponent, theme: Theme?): SwiftUIView {
        val backgroundColor = when (component.variant) {
            IconButtonVariant.FILLED -> SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)
            IconButtonVariant.FILLED_TONAL -> SwiftUIColor(0.9f, 0.9f, 0.95f, 1f)
            else -> null
        }

        val foregroundColor = when (component.variant) {
            IconButtonVariant.FILLED -> SwiftUIColor(1f, 1f, 1f, 1f)
            else -> SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)
        }

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(SwiftUIModifier.Padding(12f))
        backgroundColor?.let { modifiers.add(SwiftUIModifier.Background(it)) }
        modifiers.add(SwiftUIModifier.ForegroundColor(foregroundColor))

        if (component.variant == IconButtonVariant.OUTLINED) {
            modifiers.add(
                SwiftUIModifier.Overlay(
                    shape = "Circle()",
                    stroke = "Color.accentColor",
                    lineWidth = 1f
                )
            )
        }

        if (component.variant == IconButtonVariant.FILLED || component.variant == IconButtonVariant.FILLED_TONAL) {
            modifiers.add(SwiftUIModifier.ClipShape("Circle()"))
        }

        modifiers.addAll(ModifierConverter.convert(component.modifiers))

        return SwiftUIView(
            type = ViewType.Button,
            id = component.id,
            properties = mapOf(
                "action" to "iconButtonTapped"
            ),
            modifiers = modifiers,
            children = listOf(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to convertToSFSymbol(component.icon)
                    )
                )
            )
        )
    }

    private fun convertToSFSymbol(icon: String?): String {
        return when (icon?.lowercase()) {
            "add", "plus" -> "plus"
            "remove", "minus" -> "minus"
            "close", "cancel" -> "xmark"
            "check", "done" -> "checkmark"
            "edit" -> "pencil"
            "delete", "trash" -> "trash"
            "share" -> "square.and.arrow.up"
            "favorite", "heart" -> "heart.fill"
            "star" -> "star.fill"
            "settings" -> "gearshape.fill"
            else -> icon ?: "circle"
        }
    }
}

// ======================
// Advanced Layout
// ======================

object ScaffoldMapper {
    fun map(
        component: ScaffoldComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Top bar
        component.topBar?.let {
            children.add(renderChild(it))
        }

        // Content
        children.add(
            SwiftUIView(
                type = ViewType.VStack,
                properties = mapOf(
                    "alignment" to ".leading"
                ),
                children = listOf(renderChild(component.content))
            )
        )

        // Bottom bar
        component.bottomBar?.let {
            children.add(renderChild(it))
        }

        // FAB
        component.floatingActionButton?.let { fab ->
            children.add(
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf(
                        "alignment" to ".trailing"
                    ),
                    children = listOf(renderChild(fab))
                )
            )
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "spacing" to 0
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = children
        )
    }
}

object LazyColumnMapper {
    fun map(
        component: LazyColumnComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.List,
            id = component.id,
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.items.map { item ->
                renderChild(item)
            }
        )
    }
}

object LazyRowMapper {
    fun map(
        component: LazyRowComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.ScrollView,
            id = component.id,
            properties = mapOf(
                "axes" to ".horizontal",
                "showsIndicators" to false
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOf(
                SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf(
                        "spacing" to 8
                    ),
                    children = component.items.map { item ->
                        renderChild(item)
                    }
                )
            )
        )
    }
}

object BoxMapper {
    fun map(
        component: BoxComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.ZStack,
            id = component.id,
            properties = mapOf(
                "alignment" to convertAlignment(component.contentAlignment)
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.children.map { child ->
                renderChild(child)
            }
        )
    }

    private fun convertAlignment(alignment: String?): String {
        return when (alignment?.lowercase()) {
            "center" -> ".center"
            "topleft", "topstart" -> ".topLeading"
            "topright", "topend" -> ".topTrailing"
            "bottomleft", "bottomstart" -> ".bottomLeading"
            "bottomright", "bottomend" -> ".bottomTrailing"
            "top" -> ".top"
            "bottom" -> ".bottom"
            "left", "start" -> ".leading"
            "right", "end" -> ".trailing"
            else -> ".center"
        }
    }
}

object SurfaceMapper {
    fun map(
        component: SurfaceComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading"
            ),
            modifiers = listOf(
                SwiftUIModifier.Background(SwiftUIColor(1f, 1f, 1f, 1f)),
                SwiftUIModifier.CornerRadius(component.shape?.cornerRadius ?: 0f),
                SwiftUIModifier.Shadow(
                    color = SwiftUIColor(0f, 0f, 0f, 0.1f),
                    radius = (component.elevation * 2).toFloat(),
                    x = 0f,
                    y = component.elevation.toFloat()
                )
            ) + ModifierConverter.convert(component.modifiers),
            children = component.children.map { child ->
                renderChild(child)
            }
        )
    }
}

object ListTileMapper {
    fun map(
        component: ListTileComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Leading
        component.leading?.let {
            children.add(renderChild(it))
        }

        // Text content
        children.add(
            SwiftUIView(
                type = ViewType.VStack,
                properties = mapOf(
                    "alignment" to ".leading",
                    "spacing" to 2
                ),
                children = listOfNotNull(
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf(
                            "text" to component.title,
                            "font" to ".body"
                        )
                    ),
                    component.subtitle?.let {
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf(
                                "text" to it,
                                "font" to ".caption",
                                "foregroundColor" to "Color.secondary"
                            )
                        )
                    }
                )
            )
        )

        // Spacer
        children.add(SwiftUIView(type = ViewType.Spacer))

        // Trailing
        component.trailing?.let {
            children.add(renderChild(it))
        }

        return SwiftUIView(
            type = ViewType.HStack,
            id = component.id,
            properties = mapOf(
                "spacing" to 12
            ),
            modifiers = listOf(
                SwiftUIModifier.Padding(vertical = 8f)
            ) + ModifierConverter.convert(component.modifiers),
            children = children
        )
    }
}

// ======================
// Advanced Feedback
// ======================

object BottomSheetMapper {
    fun map(
        component: BottomSheetComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Sheet,
            id = component.id,
            properties = mapOf(
                "isPresented" to component.isVisible,
                "detents" to listOf(".medium()", ".large()")
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOf(
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf(
                        "alignment" to ".leading"
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Padding(16f)
                    ),
                    children = listOf(renderChild(component.content))
                )
            )
        )
    }
}

object LoadingDialogMapper {
    fun map(component: LoadingDialogComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.ZStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".center"
            ),
            modifiers = listOf(
                SwiftUIModifier.Frame(maxWidth = Float.MAX_VALUE, maxHeight = Float.MAX_VALUE),
                SwiftUIModifier.Background(SwiftUIColor(0f, 0f, 0f, 0.4f))
            ),
            children = listOf(
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf(
                        "spacing" to 16
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Padding(24f),
                        SwiftUIModifier.Background(SwiftUIColor(1f, 1f, 1f, 1f)),
                        SwiftUIModifier.CornerRadius(12f)
                    ),
                    children = listOfNotNull(
                        SwiftUIView(
                            type = ViewType.ProgressView,
                            properties = mapOf(
                                "style" to "CircularProgressViewStyle()"
                            )
                        ),
                        component.message?.let {
                            SwiftUIView(
                                type = ViewType.Text,
                                properties = mapOf(
                                    "text" to it,
                                    "font" to ".body"
                                )
                            )
                        }
                    )
                )
            )
        )
    }
}

// ======================
// Advanced Display
// ======================

object CircularProgressMapper {
    fun map(component: CircularProgressComponent, theme: Theme?): SwiftUIView {
        return if (component.value != null) {
            // Determinate progress
            SwiftUIView(
                type = ViewType.ProgressView,
                id = component.id,
                properties = mapOf(
                    "value" to component.value,
                    "total" to 1.0,
                    "style" to "CircularProgressViewStyle()"
                ),
                modifiers = ModifierConverter.convert(component.modifiers)
            )
        } else {
            // Indeterminate progress
            SwiftUIView(
                type = ViewType.ProgressView,
                id = component.id,
                properties = mapOf(
                    "style" to "CircularProgressViewStyle()"
                ),
                modifiers = ModifierConverter.convert(component.modifiers)
            )
        }
    }
}

// ======================
// Navigation
// ======================

object TabBarMapper {
    fun map(
        component: TabBarComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.TabView,
            id = component.id,
            properties = mapOf(
                "selection" to component.selectedIndex
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.tabs.mapIndexed { index, tab ->
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf(
                        "tag" to index,
                        "tabItem" to mapOf(
                            "label" to tab.label,
                            "systemImage" to (tab.icon ?: "circle")
                        )
                    ),
                    children = listOf(renderChild(tab.content))
                )
            }
        )
    }
}

// ======================
// Parity Components
// ======================

object DialogMapper {
    fun map(
        component: DialogComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val buttons = mutableListOf<SwiftUIView>()

        component.dismissButton?.let { text ->
            buttons.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "action" to "dismiss",
                        "role" to ".cancel"
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf("text" to text)
                        )
                    )
                )
            )
        }

        component.confirmButton?.let { text ->
            buttons.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "action" to "confirm"
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf("text" to text)
                        )
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.Alert,
            id = component.id,
            properties = mapOf(
                "isPresented" to component.isVisible,
                "title" to (component.title ?: "")
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                component.content?.let { renderChild(it) }
            ) + buttons
        )
    }
}

object NavigationDrawerMapper {
    fun map(
        component: NavigationDrawerComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.NavigationSplitView,
            id = component.id,
            properties = mapOf(
                "columnVisibility" to if (component.isOpen) ".all" else ".detailOnly"
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOf(
                // Sidebar
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf(
                        "alignment" to ".leading"
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Frame(width = 280f)
                    ),
                    children = listOf(renderChild(component.drawerContent))
                ),
                // Content
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf(
                        "alignment" to ".leading"
                    ),
                    children = listOf(renderChild(component.content))
                )
            )
        )
    }
}

object NavigationRailMapper {
    fun map(
        component: NavigationRailComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.HStack,
            id = component.id,
            properties = mapOf(
                "spacing" to 0
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOf(
                // Rail
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf(
                        "spacing" to 8
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Frame(width = 72f),
                        SwiftUIModifier.Background(SwiftUIColor(0.96f, 0.96f, 0.96f, 1f)),
                        SwiftUIModifier.Padding(vertical = 8f)
                    ),
                    children = component.items.mapIndexed { index, item ->
                        SwiftUIView(
                            type = ViewType.Button,
                            properties = mapOf(
                                "action" to "railItemTapped_$index"
                            ),
                            children = listOf(
                                SwiftUIView(
                                    type = ViewType.VStack,
                                    properties = mapOf(
                                        "spacing" to 4
                                    ),
                                    children = listOf(
                                        SwiftUIView(
                                            type = ViewType.Image,
                                            properties = mapOf(
                                                "systemName" to (item.icon ?: "circle")
                                            )
                                        ),
                                        SwiftUIView(
                                            type = ViewType.Text,
                                            properties = mapOf(
                                                "text" to item.label,
                                                "font" to ".caption2"
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    }
                ),
                // Content
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf(
                        "alignment" to ".leading"
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Frame(maxWidth = Float.MAX_VALUE, maxHeight = Float.MAX_VALUE)
                    ),
                    children = listOf(renderChild(component.content))
                )
            )
        )
    }
}

object ColorPickerMapper {
    fun map(component: ColorPickerComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.ColorPicker,
            id = component.id,
            properties = mapOf(
                "selection" to component.selectedColor,
                "supportsOpacity" to component.supportsOpacity,
                "label" to (component.label ?: "")
            ),
            modifiers = ModifierConverter.convert(component.modifiers)
        )
    }
}

object GridMapper {
    fun map(
        component: GridComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.LazyVGrid,
            id = component.id,
            properties = mapOf(
                "columns" to component.columns,
                "spacing" to component.spacing
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.children.map { child ->
                renderChild(child)
            }
        )
    }
}

object StackMapper {
    fun map(
        component: StackComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.ZStack,
            id = component.id,
            properties = mapOf(
                "alignment" to convertAlignment(component.alignment)
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.children.map { child ->
                renderChild(child)
            }
        )
    }

    private fun convertAlignment(alignment: String?): String {
        return when (alignment?.lowercase()) {
            "center" -> ".center"
            "topleft", "topstart" -> ".topLeading"
            "topright", "topend" -> ".topTrailing"
            "bottomleft", "bottomstart" -> ".bottomLeading"
            "bottomright", "bottomend" -> ".bottomTrailing"
            else -> ".center"
        }
    }
}

object PaginationMapper {
    fun map(component: PaginationComponent, theme: Theme?): SwiftUIView {
        val pageButtons = mutableListOf<SwiftUIView>()

        // Previous button
        pageButtons.add(
            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf(
                    "action" to "previousPage",
                    "disabled" to (component.currentPage <= 1)
                ),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf(
                            "systemName" to "chevron.left"
                        )
                    )
                )
            )
        )

        // Page numbers
        val startPage = maxOf(1, component.currentPage - 2)
        val endPage = minOf(component.totalPages, component.currentPage + 2)

        for (page in startPage..endPage) {
            pageButtons.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "action" to "goToPage_$page"
                    ),
                    modifiers = if (page == component.currentPage) {
                        listOf(
                            SwiftUIModifier.Background(SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)),
                            SwiftUIModifier.ForegroundColor(SwiftUIColor(1f, 1f, 1f, 1f)),
                            SwiftUIModifier.CornerRadius(4f)
                        )
                    } else {
                        emptyList()
                    },
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf(
                                "text" to page.toString()
                            )
                        )
                    )
                )
            )
        }

        // Next button
        pageButtons.add(
            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf(
                    "action" to "nextPage",
                    "disabled" to (component.currentPage >= component.totalPages)
                ),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf(
                            "systemName" to "chevron.right"
                        )
                    )
                )
            )
        )

        return SwiftUIView(
            type = ViewType.HStack,
            id = component.id,
            properties = mapOf(
                "spacing" to 4
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = pageButtons
        )
    }
}

object TooltipMapper {
    fun map(
        component: TooltipComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "help" to component.message
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                component.child?.let { renderChild(it) }
            )
        )
    }
}

object SkeletonMapper {
    fun map(component: SkeletonComponent, theme: Theme?): SwiftUIView {
        val shape = when (component.variant) {
            "circular" -> "Circle()"
            "rounded" -> "RoundedRectangle(cornerRadius: 8)"
            else -> "Rectangle()"
        }

        return SwiftUIView(
            type = ViewType.Rectangle,
            id = component.id,
            properties = mapOf(
                "clipShape" to shape
            ),
            modifiers = listOf(
                SwiftUIModifier.Frame(
                    width = component.width?.toFloat() ?: 200f,
                    height = component.height?.toFloat() ?: 16f
                ),
                SwiftUIModifier.Background(SwiftUIColor(0.9f, 0.9f, 0.9f, 1f))
            ) + ModifierConverter.convert(component.modifiers)
        )
    }
}

object SpinnerMapper {
    fun map(component: SpinnerComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.ProgressView,
            id = component.id,
            properties = mapOf(
                "style" to "CircularProgressViewStyle()"
            ),
            modifiers = ModifierConverter.convert(component.modifiers)
        )
    }
}

object BottomAppBarMapper {
    fun map(
        component: BottomAppBarComponent,
        theme: Theme?,
        renderChild: (Any) -> SwiftUIView
    ): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Leading actions
        component.leadingActions?.forEach { action ->
            children.add(renderChild(action))
        }

        // Spacer
        children.add(SwiftUIView(type = ViewType.Spacer))

        // Trailing actions
        component.trailingActions?.forEach { action ->
            children.add(renderChild(action))
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".bottom"
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOf(
                SwiftUIView(type = ViewType.Spacer),
                SwiftUIView(
                    type = ViewType.ZStack,
                    children = listOf(
                        // Background bar
                        SwiftUIView(
                            type = ViewType.HStack,
                            properties = mapOf(
                                "spacing" to 16
                            ),
                            modifiers = listOf(
                                SwiftUIModifier.Frame(maxWidth = Float.MAX_VALUE, height = 80f),
                                SwiftUIModifier.Background(SwiftUIColor(0.96f, 0.96f, 0.96f, 1f)),
                                SwiftUIModifier.Padding(horizontal = 16f)
                            ),
                            children = children
                        ),
                        // FAB overlay
                        component.floatingActionButton?.let { fab ->
                            SwiftUIView(
                                type = ViewType.VStack,
                                properties = mapOf(
                                    "alignment" to when (component.fabPosition) {
                                        "start" -> ".leading"
                                        "end" -> ".trailing"
                                        else -> ".center"
                                    }
                                ),
                                modifiers = listOf(
                                    SwiftUIModifier.Offset(y = -28f)
                                ),
                                children = listOf(renderChild(fab))
                            )
                        }
                    ).filterNotNull()
                )
            )
        )
    }
}

object MultiSelectMapper {
    fun map(component: MultiSelectComponent, theme: Theme?): SwiftUIView {
        val selectedItems = component.options
            .filter { it.first in component.selectedValues }
            .map { it.second }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 8
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                // Label
                component.label?.let {
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf(
                            "text" to it,
                            "font" to ".caption"
                        )
                    )
                },
                // Menu picker
                SwiftUIView(
                    type = ViewType.Custom("Menu"),
                    properties = mapOf(
                        "label" to if (selectedItems.isEmpty()) component.placeholder else "${selectedItems.size} selected",
                        "options" to component.options.map { it.second },
                        "selectedValues" to component.selectedValues.toList(),
                        "maxSelections" to component.maxSelections,
                        "searchable" to component.searchable,
                        "enabled" to component.enabled
                    )
                )
            )
        )
    }
}

object DateRangePickerMapper {
    fun map(component: DateRangePickerComponent, theme: Theme?): SwiftUIView {
        val displayText = when {
            component.startDate != null && component.endDate != null ->
                "${component.startDate} - ${component.endDate}"
            component.startDate != null -> "From: ${component.startDate}"
            component.endDate != null -> "To: ${component.endDate}"
            else -> "Select date range"
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 8
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                // Label
                component.label?.let {
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf(
                            "text" to it,
                            "font" to ".caption"
                        )
                    )
                },
                // Date range display with button
                SwiftUIView(
                    type = ViewType.HStack,
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf("text" to displayText)
                        ),
                        SwiftUIView(type = ViewType.Spacer),
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf(
                                "systemName" to "calendar",
                                "action" to "showDateRangePicker"
                            )
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Padding(16f),
                        SwiftUIModifier.Background(SwiftUIColor(0.95f, 0.95f, 0.95f, 1f)),
                        SwiftUIModifier.CornerRadius(8f)
                    )
                )
            )
        )
    }
}

object TagInputMapper {
    fun map(component: TagInputComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 8
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                // Label
                component.label?.let {
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf(
                            "text" to it,
                            "font" to ".caption"
                        )
                    )
                },
                // Tags display
                if (component.tags.isNotEmpty()) {
                    SwiftUIView(
                        type = ViewType.Custom("FlowLayout"),
                        properties = mapOf("spacing" to 4),
                        children = component.tags.map { tag ->
                            SwiftUIView(
                                type = ViewType.HStack,
                                properties = mapOf("spacing" to 4),
                                modifiers = listOf(
                                    SwiftUIModifier.Padding(horizontal = 8f, vertical = 4f),
                                    SwiftUIModifier.Background(SwiftUIColor(0.9f, 0.9f, 0.9f, 1f)),
                                    SwiftUIModifier.CornerRadius(12f)
                                ),
                                children = listOf(
                                    SwiftUIView(
                                        type = ViewType.Text,
                                        properties = mapOf(
                                            "text" to tag,
                                            "font" to ".caption"
                                        )
                                    ),
                                    SwiftUIView(
                                        type = ViewType.Button,
                                        properties = mapOf(
                                            "action" to "removeTag_$tag",
                                            "systemImage" to "xmark.circle.fill"
                                        )
                                    )
                                )
                            )
                        }
                    )
                } else null,
                // Text field for input
                SwiftUIView(
                    type = ViewType.TextField,
                    properties = mapOf(
                        "placeholder" to component.placeholder,
                        "text" to component.inputValue,
                        "onSubmit" to "addTag",
                        "disabled" to (!component.enabled || (component.maxTags != null && component.tags.size >= component.maxTags!!))
                    )
                ),
                // Count display
                component.maxTags?.let { max ->
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf(
                            "text" to "${component.tags.size}/$max tags",
                            "font" to ".caption2",
                            "foregroundColor" to ".secondary"
                        )
                    )
                }
            )
        )
    }
}

object ToggleMapper {
    fun map(component: ToggleComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.HStack,
            id = component.id,
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOf(
                // Label and description
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf(
                        "alignment" to ".leading",
                        "spacing" to 2
                    ),
                    children = listOfNotNull(
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf("text" to component.label)
                        ),
                        component.description?.let {
                            SwiftUIView(
                                type = ViewType.Text,
                                properties = mapOf(
                                    "text" to it,
                                    "font" to ".caption",
                                    "foregroundColor" to ".secondary"
                                )
                            )
                        }
                    )
                ),
                SwiftUIView(type = ViewType.Spacer),
                // Toggle switch
                SwiftUIView(
                    type = ViewType.Toggle,
                    properties = mapOf(
                        "isOn" to component.checked,
                        "disabled" to !component.enabled
                    )
                )
            )
        )
    }
}

object ToggleButtonGroupMapper {
    fun map(component: ToggleButtonGroupComponent, theme: Theme?): SwiftUIView {
        val viewType = if (component.orientation == Orientation.Horizontal) {
            ViewType.HStack
        } else {
            ViewType.VStack
        }

        return SwiftUIView(
            type = viewType,
            id = component.id,
            properties = mapOf("spacing" to 0),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.options.mapIndexed { index, (icon, label) ->
                val isSelected = index in component.selectedIndices

                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "action" to "toggleSelection_$index",
                        "role" to if (component.multiSelect) "multiSelect" else "singleSelect"
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.Padding(horizontal = 16f, vertical = 8f),
                        SwiftUIModifier.Background(
                            if (isSelected) SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)
                            else SwiftUIColor(0.95f, 0.95f, 0.95f, 1f)
                        ),
                        SwiftUIModifier.ForegroundColor(
                            if (isSelected) SwiftUIColor(1f, 1f, 1f, 1f)
                            else SwiftUIColor(0f, 0f, 0f, 1f)
                        )
                    ),
                    children = listOfNotNull(
                        if (icon != label) {
                            SwiftUIView(
                                type = ViewType.Image,
                                properties = mapOf("systemName" to icon)
                            )
                        } else null,
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf("text" to label)
                        )
                    )
                )
            }
        )
    }
}

object StepperMapper {
    fun map(component: StepperComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Stepper,
            id = component.id,
            properties = mapOf(
                "value" to component.value,
                "range" to "${component.min}...${component.max}",
                "step" to component.step,
                "label" to (component.label ?: ""),
                "disabled" to !component.enabled
            ),
            modifiers = ModifierConverter.convert(component.modifiers)
        )
    }
}

object IconPickerMapper {
    fun map(component: IconPickerComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 8
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                component.label?.let {
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf("text" to it, "font" to ".caption")
                    )
                },
                SwiftUIView(
                    type = ViewType.Custom("IconPickerGrid"),
                    properties = mapOf(
                        "selectedIcon" to component.value,
                        "icons" to component.icons.map { it.name },
                        "columns" to component.gridColumns,
                        "showSearch" to component.showSearch,
                        "enabled" to component.enabled
                    )
                )
            )
        )
    }
}

object StatCardMapper {
    fun map(component: StatCardComponent, theme: Theme?): SwiftUIView {
        val trendColor = when (component.trend) {
            TrendDirection.Up -> SwiftUIColor(0.3f, 0.69f, 0.31f, 1f)
            TrendDirection.Down -> SwiftUIColor(0.96f, 0.26f, 0.21f, 1f)
            TrendDirection.Neutral -> SwiftUIColor(0.62f, 0.62f, 0.62f, 1f)
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf("alignment" to ".leading", "spacing" to 8),
            modifiers = listOf(
                SwiftUIModifier.Padding(16f),
                SwiftUIModifier.Background(SwiftUIColor(1f, 1f, 1f, 1f)),
                SwiftUIModifier.CornerRadius(12f)
            ) + ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                SwiftUIView(
                    type = ViewType.HStack,
                    children = listOfNotNull(
                        component.icon?.let {
                            SwiftUIView(
                                type = ViewType.Image,
                                properties = mapOf("systemName" to it)
                            )
                        },
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf("text" to component.label, "font" to ".caption")
                        )
                    )
                ),
                SwiftUIView(
                    type = ViewType.Text,
                    properties = mapOf("text" to component.value, "font" to ".title")
                ),
                if (component.hasTrend) {
                    SwiftUIView(
                        type = ViewType.HStack,
                        children = listOf(
                            SwiftUIView(
                                type = ViewType.Image,
                                properties = mapOf(
                                    "systemName" to when (component.trend) {
                                        TrendDirection.Up -> "arrow.up"
                                        TrendDirection.Down -> "arrow.down"
                                        TrendDirection.Neutral -> "minus"
                                    }
                                ),
                                modifiers = listOf(SwiftUIModifier.ForegroundColor(trendColor))
                            ),
                            SwiftUIView(
                                type = ViewType.Text,
                                properties = mapOf("text" to component.formattedTrend),
                                modifiers = listOf(SwiftUIModifier.ForegroundColor(trendColor))
                            )
                        )
                    )
                } else null,
                component.subtitle?.let {
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf("text" to it, "font" to ".caption2", "foregroundColor" to ".secondary")
                    )
                }
            )
        )
    }
}

object FABMapper {
    fun map(component: FABComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Button,
            id = component.id,
            properties = mapOf(
                "action" to "fabAction",
                "extended" to component.extended,
                "label" to (component.label ?: "")
            ),
            modifiers = listOf(
                SwiftUIModifier.Padding(16f),
                SwiftUIModifier.Background(SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)),
                SwiftUIModifier.ForegroundColor(SwiftUIColor(1f, 1f, 1f, 1f)),
                SwiftUIModifier.CornerRadius(if (component.extended) 28f else 56f)
            ) + ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to component.icon)
                ),
                if (component.extended && !component.label.isNullOrBlank()) {
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf("text" to component.label!!)
                    )
                } else null
            )
        )
    }
}

object StickyHeaderMapper {
    fun map(component: StickyHeaderComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Section,
            id = component.id,
            properties = mapOf(
                "header" to component.content,
                "position" to if (component.position == Position.TOP) "top" else "bottom"
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                component.child?.let { renderChild(it) }
            )
        )
    }
}

object MasonryGridMapper {
    fun map(component: MasonryGridComponent, theme: Theme?, renderChild: (Any) -> SwiftUIView): SwiftUIView {
        return SwiftUIView(
            type = ViewType.LazyVGrid,
            id = component.id,
            properties = mapOf(
                "columns" to component.columns,
                "spacing" to component.gap
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.children.map { renderChild(it) }
        )
    }
}

object ProgressCircleMapper {
    fun map(component: ProgressCircleComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.ProgressView,
            id = component.id,
            properties = mapOf(
                "value" to if (component.indeterminate) null else component.value,
                "total" to component.max,
                "style" to "CircularProgressViewStyle()"
            ),
            modifiers = listOf(
                SwiftUIModifier.Frame(
                    width = when (component.size) {
                        com.augmentalis.avanueui.core.ComponentSize.SM -> 24f
                        com.augmentalis.avanueui.core.ComponentSize.LG -> 56f
                        else -> 40f
                    },
                    height = when (component.size) {
                        com.augmentalis.avanueui.core.ComponentSize.SM -> 24f
                        com.augmentalis.avanueui.core.ComponentSize.LG -> 56f
                        else -> 40f
                    }
                )
            ) + ModifierConverter.convert(component.modifiers)
        )
    }
}

object BannerMapper {
    fun map(component: BannerComponent, theme: Theme?): SwiftUIView {
        val backgroundColor = when (component.severity) {
            Severity.INFO -> SwiftUIColor(0.89f, 0.95f, 0.99f, 1f)
            Severity.SUCCESS -> SwiftUIColor(0.91f, 0.96f, 0.91f, 1f)
            Severity.WARNING -> SwiftUIColor(1f, 0.97f, 0.88f, 1f)
            Severity.ERROR -> SwiftUIColor(1f, 0.92f, 0.93f, 1f)
        }

        return SwiftUIView(
            type = ViewType.HStack,
            id = component.id,
            modifiers = listOf(
                SwiftUIModifier.Padding(16f),
                SwiftUIModifier.Background(backgroundColor),
                SwiftUIModifier.Frame(maxWidth = Float.MAX_VALUE)
            ) + ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                component.icon?.let {
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to it)
                    )
                },
                SwiftUIView(
                    type = ViewType.Text,
                    properties = mapOf("text" to component.message)
                ),
                SwiftUIView(type = ViewType.Spacer),
                if (component.dismissible) {
                    SwiftUIView(
                        type = ViewType.Button,
                        properties = mapOf("action" to "dismiss", "systemImage" to "xmark")
                    )
                } else null
            )
        )
    }
}

object NotificationCenterMapper {
    fun map(component: NotificationCenterComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf("spacing" to 8),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = component.displayedNotifications.map { notification ->
                val backgroundColor = when (notification.severity) {
                    Severity.INFO -> SwiftUIColor(0.89f, 0.95f, 0.99f, 1f)
                    Severity.SUCCESS -> SwiftUIColor(0.91f, 0.96f, 0.91f, 1f)
                    Severity.WARNING -> SwiftUIColor(1f, 0.97f, 0.88f, 1f)
                    Severity.ERROR -> SwiftUIColor(1f, 0.92f, 0.93f, 1f)
                }

                SwiftUIView(
                    type = ViewType.HStack,
                    modifiers = listOf(
                        SwiftUIModifier.Padding(12f),
                        SwiftUIModifier.Background(backgroundColor),
                        SwiftUIModifier.CornerRadius(8f)
                    ),
                    children = listOfNotNull(
                        SwiftUIView(
                            type = ViewType.VStack,
                            properties = mapOf("alignment" to ".leading"),
                            children = listOfNotNull(
                                notification.title?.let {
                                    SwiftUIView(
                                        type = ViewType.Text,
                                        properties = mapOf("text" to it, "font" to ".headline")
                                    )
                                },
                                SwiftUIView(
                                    type = ViewType.Text,
                                    properties = mapOf("text" to notification.message, "font" to ".caption")
                                )
                            )
                        ),
                        SwiftUIView(type = ViewType.Spacer),
                        if (notification.dismissible) {
                            SwiftUIView(
                                type = ViewType.Button,
                                properties = mapOf(
                                    "action" to "dismiss_${notification.id}",
                                    "systemImage" to "xmark"
                                )
                            )
                        } else null
                    )
                )
            }
        )
    }
}

object TableMapper {
    fun map(component: TableComponent, theme: Theme?): SwiftUIView {
        val rows = mutableListOf<SwiftUIView>()

        // Header row
        rows.add(
            SwiftUIView(
                type = ViewType.HStack,
                modifiers = listOf(
                    SwiftUIModifier.Padding(12f),
                    SwiftUIModifier.Background(SwiftUIColor(0.95f, 0.95f, 0.95f, 1f))
                ),
                children = component.headers.map { header ->
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf("text" to header, "font" to ".headline")
                    )
                }
            )
        )

        // Data rows
        component.rows.forEachIndexed { index, row ->
            val rowModifiers = mutableListOf(SwiftUIModifier.Padding(12f))
            if (component.striped && index % 2 == 1) {
                rowModifiers.add(SwiftUIModifier.Background(SwiftUIColor(0.98f, 0.98f, 0.98f, 1f)))
            }

            rows.add(
                SwiftUIView(
                    type = ViewType.HStack,
                    modifiers = rowModifiers,
                    children = row.map { cell ->
                        SwiftUIView(
                            type = ViewType.Text,
                            properties = mapOf("text" to cell)
                        )
                    }
                )
            )
        }

        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf("spacing" to 0),
            modifiers = if (component.bordered) {
                listOf(
                    SwiftUIModifier.Overlay(
                        SwiftUIColor(0.8f, 0.8f, 0.8f, 1f),
                        1f
                    )
                )
            } else emptyList() + ModifierConverter.convert(component.modifiers),
            children = rows
        )
    }
}

// ======================
// Gap Closure Mappers (8 missing for 100% parity)
// ======================

object RadioMapper {
    fun map(component: RadioComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.HStack,
            id = component.id,
            properties = mapOf("spacing" to 8),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf(
                        "systemName" to if (component.selected) "circle.inset.filled" else "circle",
                        "action" to "radioTapped"
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.ForegroundColor(
                            if (component.selected) SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)
                            else SwiftUIColor(0.6f, 0.6f, 0.6f, 1f)
                        )
                    )
                ),
                SwiftUIView(
                    type = ViewType.Text,
                    properties = mapOf("text" to component.label)
                )
            )
        )
    }
}

object RadioGroupMapper {
    fun map(component: RadioGroupComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 12
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                component.label?.let {
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf("text" to it, "font" to ".caption")
                    )
                }
            ) + component.options.mapIndexed { index, (value, label) ->
                val isSelected = value == component.selectedValue
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf("action" to "selectRadio_$index"),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.HStack,
                            properties = mapOf("spacing" to 8),
                            children = listOf(
                                SwiftUIView(
                                    type = ViewType.Image,
                                    properties = mapOf(
                                        "systemName" to if (isSelected) "circle.inset.filled" else "circle"
                                    ),
                                    modifiers = listOf(
                                        SwiftUIModifier.ForegroundColor(
                                            if (isSelected) SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)
                                            else SwiftUIColor(0.6f, 0.6f, 0.6f, 1f)
                                        )
                                    )
                                ),
                                SwiftUIView(
                                    type = ViewType.Text,
                                    properties = mapOf("text" to label)
                                )
                            )
                        )
                    )
                )
            }
        )
    }
}

object SliderMapper {
    fun map(component: SliderComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 4
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                component.label?.let {
                    SwiftUIView(
                        type = ViewType.HStack,
                        children = listOf(
                            SwiftUIView(
                                type = ViewType.Text,
                                properties = mapOf("text" to it, "font" to ".caption")
                            ),
                            SwiftUIView(type = ViewType.Spacer),
                            SwiftUIView(
                                type = ViewType.Text,
                                properties = mapOf(
                                    "text" to component.value.toString(),
                                    "font" to ".caption"
                                )
                            )
                        )
                    )
                },
                SwiftUIView(
                    type = ViewType.Slider,
                    properties = mapOf(
                        "value" to component.value,
                        "range" to "${component.min}...${component.max}",
                        "step" to component.step,
                        "disabled" to !component.enabled
                    )
                )
            )
        )
    }
}

object ProgressBarMapper {
    fun map(component: ProgressBarComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.VStack,
            id = component.id,
            properties = mapOf(
                "alignment" to ".leading",
                "spacing" to 4
            ),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                component.label?.let {
                    SwiftUIView(
                        type = ViewType.Text,
                        properties = mapOf("text" to it, "font" to ".caption")
                    )
                },
                if (component.indeterminate) {
                    SwiftUIView(
                        type = ViewType.ProgressView,
                        properties = mapOf("style" to "LinearProgressViewStyle()")
                    )
                } else {
                    SwiftUIView(
                        type = ViewType.ProgressView,
                        properties = mapOf(
                            "value" to component.value,
                            "total" to component.max,
                            "style" to "LinearProgressViewStyle()"
                        )
                    )
                }
            )
        )
    }
}

object AvatarMapper {
    fun map(component: AvatarComponent, theme: Theme?): SwiftUIView {
        val size = when (component.size) {
            AvatarSize.SM -> 32f
            AvatarSize.MD -> 40f
            AvatarSize.LG -> 56f
            AvatarSize.XL -> 80f
        }

        val child = when {
            component.src != null -> SwiftUIView(
                type = ViewType.AsyncImage,
                properties = mapOf(
                    "url" to component.src,
                    "placeholder" to "person.circle.fill"
                )
            )
            component.initials != null -> SwiftUIView(
                type = ViewType.Text,
                properties = mapOf(
                    "text" to component.initials,
                    "font" to if (size > 40f) ".title3" else ".caption"
                )
            )
            else -> SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to "person.fill")
            )
        }

        return SwiftUIView(
            type = ViewType.ZStack,
            id = component.id,
            modifiers = listOf(
                SwiftUIModifier.Frame(width = size, height = size),
                SwiftUIModifier.Background(
                    component.backgroundColor?.let {
                        SwiftUIColor(0.9f, 0.9f, 0.9f, 1f)
                    } ?: SwiftUIColor(0.9f, 0.9f, 0.9f, 1f)
                ),
                SwiftUIModifier.ClipShape("Circle()")
            ) + ModifierConverter.convert(component.modifiers),
            children = listOf(child)
        )
    }
}

object BadgeMapper {
    fun map(component: BadgeComponent, theme: Theme?): SwiftUIView {
        val backgroundColor = when (component.color) {
            "primary" -> SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)
            "secondary" -> SwiftUIColor(0.6f, 0.6f, 0.6f, 1f)
            "success" -> SwiftUIColor(0.3f, 0.69f, 0.31f, 1f)
            "error" -> SwiftUIColor(0.96f, 0.26f, 0.21f, 1f)
            "warning" -> SwiftUIColor(1f, 0.6f, 0f, 1f)
            else -> SwiftUIColor(0.2f, 0.4f, 0.8f, 1f)
        }

        return SwiftUIView(
            type = ViewType.Text,
            id = component.id,
            properties = mapOf(
                "text" to component.content,
                "font" to ".caption2"
            ),
            modifiers = listOf(
                SwiftUIModifier.Padding(horizontal = 6f, vertical = 2f),
                SwiftUIModifier.Background(backgroundColor),
                SwiftUIModifier.ForegroundColor(SwiftUIColor(1f, 1f, 1f, 1f)),
                SwiftUIModifier.CornerRadius(if (component.variant == "dot") 50f else 4f)
            ) + ModifierConverter.convert(component.modifiers)
        )
    }
}

object RatingMapper {
    fun map(component: RatingComponent, theme: Theme?): SwiftUIView {
        val stars = (1..component.max).map { index ->
            val isFilled = index <= component.value
            val isHalf = !isFilled && index - 0.5 <= component.value && component.allowHalf

            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf(
                    "action" to "setRating_$index",
                    "disabled" to component.readOnly
                ),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf(
                            "systemName" to when {
                                isFilled -> "star.fill"
                                isHalf -> "star.leadinghalf.filled"
                                else -> "star"
                            }
                        ),
                        modifiers = listOf(
                            SwiftUIModifier.ForegroundColor(
                                if (isFilled || isHalf) SwiftUIColor(1f, 0.8f, 0f, 1f)
                                else SwiftUIColor(0.8f, 0.8f, 0.8f, 1f)
                            )
                        )
                    )
                )
            )
        }

        return SwiftUIView(
            type = ViewType.HStack,
            id = component.id,
            properties = mapOf("spacing" to 4),
            modifiers = ModifierConverter.convert(component.modifiers),
            children = stars
        )
    }
}

object SearchBarMapper {
    fun map(component: SearchBarComponent, theme: Theme?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.HStack,
            id = component.id,
            properties = mapOf("spacing" to 8),
            modifiers = listOf(
                SwiftUIModifier.Padding(horizontal = 12f, vertical = 8f),
                SwiftUIModifier.Background(SwiftUIColor(0.95f, 0.95f, 0.95f, 1f)),
                SwiftUIModifier.CornerRadius(10f)
            ) + ModifierConverter.convert(component.modifiers),
            children = listOfNotNull(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to "magnifyingglass"),
                    modifiers = listOf(
                        SwiftUIModifier.ForegroundColor(SwiftUIColor(0.5f, 0.5f, 0.5f, 1f))
                    )
                ),
                SwiftUIView(
                    type = ViewType.TextField,
                    properties = mapOf(
                        "placeholder" to (component.placeholder ?: "Search"),
                        "text" to component.value,
                        "onSubmit" to "search"
                    )
                ),
                if (component.value.isNotEmpty()) {
                    SwiftUIView(
                        type = ViewType.Button,
                        properties = mapOf("action" to "clearSearch"),
                        children = listOf(
                            SwiftUIView(
                                type = ViewType.Image,
                                properties = mapOf("systemName" to "xmark.circle.fill"),
                                modifiers = listOf(
                                    SwiftUIModifier.ForegroundColor(SwiftUIColor(0.6f, 0.6f, 0.6f, 1f))
                                )
                            )
                        )
                    )
                } else null
            )
        )
    }
}

// ======================
// Component Type Stubs (to be defined in UI/Core)
// ======================

// These types should exist in com.augmentalis.avanueui.ui.core
// Adding stubs here for compilation - actual types in commonMain

data class RadioComponent(
    val id: String = "",
    val label: String = "",
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val modifiers: List<Any> = emptyList()
)

data class RadioGroupComponent(
    val id: String = "",
    val label: String? = null,
    val options: List<Pair<String, String>> = emptyList(),
    val selectedValue: String? = null,
    val enabled: Boolean = true,
    val modifiers: List<Any> = emptyList()
)

data class SliderComponent(
    val id: String = "",
    val label: String? = null,
    val value: Float = 0f,
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val enabled: Boolean = true,
    val modifiers: List<Any> = emptyList()
)

data class ProgressBarComponent(
    val id: String = "",
    val label: String? = null,
    val value: Float = 0f,
    val max: Float = 100f,
    val indeterminate: Boolean = false,
    val modifiers: List<Any> = emptyList()
)

enum class AvatarSize { SM, MD, LG, XL }

data class AvatarComponent(
    val id: String = "",
    val src: String? = null,
    val initials: String? = null,
    val size: AvatarSize = AvatarSize.MD,
    val backgroundColor: String? = null,
    val modifiers: List<Any> = emptyList()
)

data class BadgeComponent(
    val id: String = "",
    val content: String = "",
    val color: String = "primary",
    val variant: String = "standard",
    val modifiers: List<Any> = emptyList()
)

data class RatingComponent(
    val id: String = "",
    val value: Float = 0f,
    val max: Int = 5,
    val allowHalf: Boolean = false,
    val readOnly: Boolean = false,
    val modifiers: List<Any> = emptyList()
)

data class SearchBarComponent(
    val id: String = "",
    val value: String = "",
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val modifiers: List<Any> = emptyList()
)
