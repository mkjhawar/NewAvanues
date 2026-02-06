package com.augmentalis.avamagic.ui.core.form

import com.augmentalis.avamagic.components.core.*

/**
 * SearchBar Component
 *
 * A search input component with live search and suggestions.
 *
 * Features:
 * - Real-time search input
 * - Search suggestions/autocomplete
 * - Recent searches history
 * - Clear button
 * - Search icon
 * - Debounced search events
 * - Keyboard shortcuts
 *
 * Platform mappings:
 * - Android: SearchView
 * - iOS: UISearchBar
 * - Web: Input with search type
 *
 * Usage:
 * ```kotlin
 * SearchBar(
 *     value = "query",
 *     placeholder = "Search...",
 *     showClearButton = true,
 *     suggestions = listOf("suggestion1", "suggestion2"),
 *     onValueChange = { query -> println("Searching: $query") },
 *     onSearch = { query -> performSearch(query) }
 * )
 * ```
 */
data class SearchBarComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val value: String = "",
    val placeholder: String = "Search...",
    val showClearButton: Boolean = true,
    val suggestions: List<String> = emptyList(),
    val onValueChange: ((String) -> Unit)? = null,
    val onSearch: ((String) -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}
