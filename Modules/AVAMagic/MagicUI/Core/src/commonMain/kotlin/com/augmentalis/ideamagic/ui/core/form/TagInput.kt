package com.augmentalis.magicui.ui.core.form
import com.augmentalis.magicui.components.core.*
data class TagInputComponent(override val id: String? = null, override val style: ComponentStyle? = null, override val modifiers: List<Modifier> = emptyList(), val tags: List<String> = emptyList(), val inputValue: String = "", val label: String? = null, val placeholder: String = "Add tag", val allowDuplicates: Boolean = false, val maxTags: Int? = null, val size: ComponentSize = ComponentSize.MD, val enabled: Boolean = true) : Component {
    init { if (maxTags != null) require(maxTags > 0) { "maxTags must be positive" } }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun addTag(tag: String): TagInputComponent = if (tag.isNotBlank() && (allowDuplicates || tag !in tags) && (maxTags == null || tags.size < maxTags)) copy(tags = tags + tag, inputValue = "") else this
    fun removeTag(tag: String): TagInputComponent = copy(tags = tags.filter { it != tag })
    fun updateInput(value: String): TagInputComponent = copy(inputValue = value)
    fun clear(): TagInputComponent = copy(tags = emptyList(), inputValue = "")
}
