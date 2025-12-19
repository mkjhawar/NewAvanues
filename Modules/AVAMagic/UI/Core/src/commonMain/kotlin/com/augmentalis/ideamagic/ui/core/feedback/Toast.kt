package com.augmentalis.avanues.avamagic.ui.core.feedback
import com.augmentalis.avanues.avamagic.components.core.*
import com.augmentalis.avanues.avamagic.components.core.Position
import com.augmentalis.avanues.avamagic.components.core.Severity
data class ToastComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val message: String, val severity: Severity = Severity.INFO, val duration: Long = 3000, val position: Position = Position.BOTTOM, val dismissible: Boolean = true, val action: String? = null) : Component {
    init { require(message.isNotBlank()) { "Toast message cannot be blank" }; require(duration > 0) { "Duration must be positive" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    companion object { fun success(message: String) = ToastComponent(message = message, severity = Severity.SUCCESS); fun error(message: String) = ToastComponent(message = message, severity = Severity.ERROR, duration = 5000); fun warning(message: String) = ToastComponent(message = message, severity = Severity.WARNING); fun info(message: String) = ToastComponent(message = message, severity = Severity.INFO) }
}
