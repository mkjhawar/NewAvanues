package com.augmentalis.avanueui.ui.core.feedback

import com.augmentalis.avanueui.core.*

/**
 * BottomSheet - Sliding panel from bottom
 */
data class BottomSheetComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val content: Component,
    val sheetContent: Component,
    val sheetState: BottomSheetState = BottomSheetState.COLLAPSED,
    val dragHandle: Boolean = true,
    val skipPartiallyExpanded: Boolean = false,
    val onDismiss: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

enum class BottomSheetState {
    COLLAPSED, PARTIALLY_EXPANDED, EXPANDED, HIDDEN
}

/**
 * LoadingDialog - Progress indicator in modal
 */
data class LoadingDialogComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val message: String? = null,
    val progress: Float? = null, // null = indeterminate
    val dismissible: Boolean = false,
    val onDismiss: (() -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}
