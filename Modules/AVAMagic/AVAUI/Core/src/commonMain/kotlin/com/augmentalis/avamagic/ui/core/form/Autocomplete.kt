package com.augmentalis.avamagic.ui.core.form

import com.augmentalis.avamagic.components.core.*

/**
 * Autocomplete text input with suggestions.
 *
 * Autocomplete provides text input with real-time suggestions based on
 * user input. Commonly used for search, address entry, and tag selection.
 *
 * @property value Current input value
 * @property suggestions List of suggestion strings
 * @property label Input label
 * @property placeholder Placeholder text
 * @property minChars Minimum characters before showing suggestions (default 1)
 * @property maxSuggestions Maximum suggestions to display (default 5)
 * @property caseSensitive Whether filtering is case-sensitive (default false)
 * @property size Input size (default MD)
 * @property enabled Whether user can interact (default true)
 * @since 1.0.0
 */
data class AutocompleteComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val value: String = "",
    val suggestions: List<String> = emptyList(),
    val label: String? = null,
    val placeholder: String = "",
    val minChars: Int = 1,
    val maxSuggestions: Int = 5,
    val caseSensitive: Boolean = false,
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true
) : Component {
    init {
        require(minChars >= 0) { "minChars must be non-negative (got $minChars)" }
        require(maxSuggestions > 0) { "maxSuggestions must be positive (got $maxSuggestions)" }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    fun updateValue(newValue: String): AutocompleteComponent =
        copy(value = newValue)

    val filteredSuggestions: List<String>
        get() {
            if (value.length < minChars) return emptyList()
            val filtered = suggestions.filter { suggestion ->
                if (caseSensitive) {
                    suggestion.contains(value)
                } else {
                    suggestion.contains(value, ignoreCase = true)
                }
            }
            return filtered.take(maxSuggestions)
        }

    val showSuggestions: Boolean
        get() = value.length >= minChars && filteredSuggestions.isNotEmpty()

    fun selectSuggestion(suggestion: String): AutocompleteComponent =
        copy(value = suggestion)

    fun clear(): AutocompleteComponent =
        copy(value = "")

    companion object {
        fun email(domains: List<String> = listOf("gmail.com", "yahoo.com", "outlook.com")) =
            AutocompleteComponent(
                label = "Email",
                placeholder = "Enter email address",
                suggestions = domains.map { "@$it" }
            )

        fun search(placeholder: String = "Search...") =
            AutocompleteComponent(
                placeholder = placeholder,
                minChars = 2
            )
    }
}
