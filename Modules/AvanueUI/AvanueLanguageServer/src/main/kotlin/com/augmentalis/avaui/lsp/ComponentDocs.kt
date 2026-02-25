package com.augmentalis.avaui.lsp

/**
 * Shared component documentation for completions and hover.
 * Single source of truth for AvanueUI component metadata.
 */
object ComponentDocs {

    fun getDocumentation(componentName: String): String {
        return when (componentName) {
            "Button" -> "Interactive button component with click handling"
            "TextField" -> "Text input field with validation and change handling"
            "Card" -> "Container with elevation and rounded corners"
            "Text" -> "Display text with customizable styling"
            "Image" -> "Display images from URLs or assets"
            "Column" -> "Vertical layout container"
            "Row" -> "Horizontal layout container"
            "Container" -> "Generic container for grouping components"
            "Checkbox" -> "Checkbox input for boolean values"
            "Switch" -> "Toggle switch for boolean values"
            else -> "AvanueUI component"
        }
    }

    fun getHoverDocumentation(componentName: String): String {
        return when (componentName) {
            "Button" -> """
                ### Button Component
                Interactive button for user actions

                **Properties:**
                - `text`: Button label text
                - `icon`: Optional icon
                - `onClick`: Click event handler
                - `enabled`: Enable/disable state
                - `avid`: AvanueUI Voice IDentifier

                **Example:**
                ```yaml
                Button:
                  avid: submit-btn
                  text: Submit Form
                  onClick: handleSubmit
                ```
            """.trimIndent()
            "TextField" -> """
                ### TextField Component
                Text input field with validation

                **Properties:**
                - `placeholder`: Placeholder text
                - `value`: Current value
                - `onChange`: Change event handler
                - `avid`: AvanueUI Voice IDentifier
                - `validation`: Validation rules

                **Example:**
                ```yaml
                TextField:
                  avid: email-input
                  placeholder: Enter email
                  onChange: handleEmailChange
                ```
            """.trimIndent()
            "Card" -> """
                ### Card Component
                Container with elevation and rounded corners

                **Properties:**
                - `elevation`: Shadow depth
                - `children`: Child components
                - `backgroundColor`: Card background color
                - `padding`: Inner padding

                **Example:**
                ```yaml
                Card:
                  avid: profile-card
                  elevation: 4
                  children:
                    - Text: ...
                ```
            """.trimIndent()
            else -> """
                ### $componentName Component
                AvanueUI component for building user interfaces

                **Common Properties:**
                - `avid`: AvanueUI Voice IDentifier
                - `visible`: Visibility state
                - `enabled`: Enabled state
            """.trimIndent()
        }
    }
}
