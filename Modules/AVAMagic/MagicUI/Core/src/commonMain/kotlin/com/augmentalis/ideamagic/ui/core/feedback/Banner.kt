package com.augmentalis.magicui.ui.core.feedback
import com.augmentalis.magicui.components.core.*
import com.augmentalis.magicui.components.core.Position
import com.augmentalis.magicui.components.core.Severity
data class BannerComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val message: String, val severity: Severity = Severity.INFO, val position: Position = Position.TOP, val dismissible: Boolean = true, val actions: List<String> = emptyList(), val icon: String? = null) : Component {
    init { require(message.isNotBlank()) { "Banner message cannot be blank" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    companion object { fun info(message: String) = BannerComponent(message = message, severity = Severity.INFO); fun warning(message: String) = BannerComponent(message = message, severity = Severity.WARNING); fun error(message: String) = BannerComponent(message = message, severity = Severity.ERROR) }
}
