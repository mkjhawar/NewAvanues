package com.augmentalis.avanues.avamagic.ui.core.form

import com.augmentalis.avanues.avamagic.components.core.*

data class MultiSelectComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val options: List<Pair<String, String>>,
    val selectedValues: Set<String> = emptySet(),
    val label: String? = null,
    val placeholder: String = "Select items",
    val searchable: Boolean = true,
    val maxSelections: Int? = null,
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true
) : Component {
    init {
        require(options.isNotEmpty()) { "Options cannot be empty" }
        require(options.map { it.first }.distinct().size == options.size) { "Option values must be unique" }
        if (maxSelections != null) {
            require(maxSelections > 0) { "maxSelections must be positive" }
            require(selectedValues.size <= maxSelections) { "Selected values exceed maxSelections" }
        }
    }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    fun toggle(value: String): MultiSelectComponent {
        val newSelection = if (value in selectedValues) selectedValues - value 
        else if (maxSelections == null || selectedValues.size < maxSelections) selectedValues + value 
        else selectedValues
        return copy(selectedValues = newSelection)
    }
    fun selectAll(): MultiSelectComponent = copy(selectedValues = options.map { it.first }.toSet())
    fun clearAll(): MultiSelectComponent = copy(selectedValues = emptySet())
    companion object {
        fun tags(allTags: List<String>) = MultiSelectComponent(options = allTags.map { it to it }, label = "Tags")
    }
}
