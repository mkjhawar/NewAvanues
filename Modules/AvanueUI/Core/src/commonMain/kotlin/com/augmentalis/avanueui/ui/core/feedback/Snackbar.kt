package com.augmentalis.avanueui.ui.core.feedback
import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.Position
data class SnackbarComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val message: String, val actionLabel: String? = null, val duration: Long = 4000, val position: Position = Position.BOTTOM, val dismissible: Boolean = true) : Component {
    init { require(message.isNotBlank()) { "Snackbar message cannot be blank" }; require(duration > 0) { "Duration must be positive" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    companion object { fun simple(message: String) = SnackbarComponent(message = message); fun withAction(message: String, action: String) = SnackbarComponent(message = message, actionLabel = action) }
}
