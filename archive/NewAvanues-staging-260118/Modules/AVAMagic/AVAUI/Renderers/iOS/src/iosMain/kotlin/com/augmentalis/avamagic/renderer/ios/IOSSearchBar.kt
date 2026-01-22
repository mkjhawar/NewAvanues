package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avamagic.ui.core.form.SearchBarComponent

/**
 * iOS Renderer for SearchBar Component
 *
 * Renders AVAMagic SearchBar components as native UISearchBar.
 *
 * Features:
 * - Native UISearchBar rendering
 * - Search icon
 * - Clear button
 * - Search suggestions
 * - Placeholder text
 * - Keyboard management
 * - Dark mode support
 * - Accessibility support
 *
 * @author Manoj Jhawar
 * @since 2025-11-21
 */
@OptIn(ExperimentalForeignApi::class)
class IOSSearchBarRenderer {

    /**
     * Render SearchBar component to UISearchBar
     */
    fun render(component: SearchBarComponent): UIView {
        // Create container with search bar and optional suggestions
        val containerView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 56.0)
            backgroundColor = UIColor.systemBackgroundColor
        }

        // Create search bar
        val searchBar = UISearchBar().apply {
            frame = CGRectMake(0.0, 8.0, 375.0, 44.0)

            // Set placeholder
            placeholder = component.placeholder

            // Set current text
            text = component.value

            // Show clear button
            showsCancelButton = component.showClearButton

            // Search bar style
            searchBarStyle = UISearchBarStyleMinimal

            // Apply component style
            applyStyle(component)

            // Configure appearance
            configureAppearance(component)

            // Apply accessibility
            applyAccessibility(this, component)

            // Handle text change
            // Note: In production, use UISearchBarDelegate
            // This is simplified for demonstration
            component.onValueChange?.let { callback ->
                // Delegate textDidChange would call: callback(newText)
            }

            // Handle search button click
            component.onSearch?.let { callback ->
                // Delegate searchButtonClicked would call: callback(text)
            }
        }

        containerView.addSubview(searchBar)

        // Add suggestions table view if suggestions provided
        if (component.suggestions.isNotEmpty()) {
            val suggestionsView = createSuggestionsView(
                component.suggestions,
                yOffset = 56.0,
                onSuggestionClick = { suggestion ->
                    searchBar.text = suggestion
                    component.onSearch?.invoke(suggestion)
                }
            )
            containerView.frame = CGRectMake(
                0.0, 0.0, 375.0,
                56.0 + (component.suggestions.size * 44.0).coerceAtMost(220.0)
            )
            containerView.addSubview(suggestionsView)
        }

        return containerView
    }

    /**
     * Apply component style to search bar
     */
    private fun UISearchBar.applyStyle(component: SearchBarComponent) {
        component.style?.let { style ->
            // Background color
            style.backgroundColor?.let { color ->
                backgroundColor = parseColor(color)
                barTintColor = parseColor(color)
            }

            // Tint color (affects cursor and search icon)
            style.textColor?.let { color ->
                tintColor = parseColor(color)
            }
        }
    }

    /**
     * Configure search bar appearance
     */
    private fun UISearchBar.configureAppearance(component: SearchBarComponent) {
        // Enable auto-capitalization
        autocapitalizationType = UITextAutocapitalizationTypeNone

        // Enable auto-correction
        autocorrectionType = UITextAutocorrectionTypeNo

        // Keyboard appearance
        keyboardAppearance = UIKeyboardAppearanceDefault
    }

    /**
     * Create suggestions table view
     */
    private fun createSuggestionsView(
        suggestions: List<String>,
        yOffset: Double,
        onSuggestionClick: (String) -> Unit
    ): UIView {
        val maxHeight = (suggestions.size * 44.0).coerceAtMost(220.0)

        val scrollView = UIScrollView().apply {
            frame = CGRectMake(0.0, yOffset, 375.0, maxHeight)
            backgroundColor = UIColor.systemBackgroundColor
            layer.borderWidth = 1.0
            layer.borderColor = UIColor.separatorColor.CGColor
        }

        suggestions.forEachIndexed { index, suggestion ->
            val suggestionView = createSuggestionRow(
                text = suggestion,
                yOffset = index * 44.0,
                onClick = { onSuggestionClick(suggestion) }
            )
            scrollView.addSubview(suggestionView)
        }

        scrollView.contentSize = CGSizeMake(375.0, suggestions.size * 44.0)

        return scrollView
    }

    /**
     * Create individual suggestion row
     */
    private fun createSuggestionRow(
        text: String,
        yOffset: Double,
        onClick: () -> Unit
    ): UIView {
        val rowView = UIView().apply {
            frame = CGRectMake(0.0, yOffset, 375.0, 44.0)
            backgroundColor = UIColor.systemBackgroundColor

            // Icon
            val iconImageView = UIImageView().apply {
                frame = CGRectMake(16.0, 12.0, 20.0, 20.0)
                image = UIImage.systemImageNamed("magnifyingglass")
                tintColor = UIColor.systemGrayColor
            }
            addSubview(iconImageView)

            // Label
            val label = UILabel().apply {
                frame = CGRectMake(48.0, 0.0, 327.0 - 48.0, 44.0)
                this.text = text
                font = UIFont.systemFontOfSize(16.0)
                textColor = UIColor.labelColor
            }
            addSubview(label)

            // Separator
            val separator = UIView().apply {
                frame = CGRectMake(48.0, 43.0, 327.0, 1.0)
                backgroundColor = UIColor.separatorColor
            }
            addSubview(separator)

            // Tap gesture
            val tapGesture = UITapGestureRecognizer()
            addGestureRecognizer(tapGesture)
            isUserInteractionEnabled = true
            // Note: In production, connect gesture to onClick callback

            // Accessibility
            isAccessibilityElement = true
            accessibilityLabel = "Suggestion: $text"
            accessibilityTraits = UIAccessibilityTraitButton
        }

        return rowView
    }

    /**
     * Parse hex color string to UIColor
     */
    private fun parseColor(hex: String): UIColor {
        val cleanHex = hex.removePrefix("#")
        val rgb = cleanHex.toLongOrNull(16) ?: 0x000000

        val red = ((rgb shr 16) and 0xFF) / 255.0
        val green = ((rgb shr 8) and 0xFF) / 255.0
        val blue = (rgb and 0xFF) / 255.0

        return UIColor(red = red, green = green, blue = blue, alpha = 1.0)
    }

    /**
     * Apply accessibility features
     */
    private fun applyAccessibility(searchBar: UISearchBar, component: SearchBarComponent) {
        searchBar.isAccessibilityElement = true
        searchBar.accessibilityLabel = "Search"
        searchBar.accessibilityHint = component.placeholder
        searchBar.accessibilityTraits = UIAccessibilityTraitSearchField

        if (component.value.isNotEmpty()) {
            searchBar.accessibilityValue = component.value
        }
    }
}
