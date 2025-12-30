/**
 * UIState.kt - UI state tracking data class
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-04
 * 
 * Data class that tracks the current UI state including screen content,
 * focused elements, and accessibility information for voice commands.
 */
package com.augmentalis.voiceos.accessibility.state

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing the current UI state
 */
@Parcelize
data class UIState(
    /**
     * Current application package name
     */
    val packageName: String? = null,
    
    /**
     * Current activity or screen name
     */
    val activityName: String? = null,
    
    /**
     * Window title or screen title
     */
    val windowTitle: String? = null,
    
    /**
     * Currently focused element information
     */
    val focusedElement: ElementInfo? = null,
    
    /**
     * List of interactive elements on screen
     */
    val interactiveElements: List<ElementInfo> = emptyList(),
    
    /**
     * Screen content text (for context)
     */
    val screenText: String? = null,
    
    /**
     * Screen orientation
     */
    val screenOrientation: ScreenOrientation = ScreenOrientation.UNKNOWN,
    
    /**
     * Screen dimensions
     */
    val screenBounds: Rect? = null,
    
    /**
     * Whether the screen is scrollable
     */
    val isScrollable: Boolean = false,
    
    /**
     * Scroll position if available (0.0 to 1.0)
     */
    val scrollPosition: Float = 0f,
    
    /**
     * Whether keyboard is visible
     */
    val isKeyboardVisible: Boolean = false,
    
    /**
     * Current input method editor info
     */
    val inputFieldInfo: InputFieldInfo? = null,
    
    /**
     * Timestamp when this state was captured
     */
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * Additional metadata
     */
    val metadata: Map<String, String> = emptyMap()
) : Parcelable {
    
    /**
     * Information about an interactive element
     */
    @Parcelize
    data class ElementInfo(
        /**
         * Unique identifier for the element
         */
        val id: String? = null,
        
        /**
         * Element text content
         */
        val text: String? = null,
        
        /**
         * Content description for accessibility
         */
        val contentDescription: String? = null,
        
        /**
         * Element class name
         */
        val className: String? = null,
        
        /**
         * Element bounds on screen
         */
        val bounds: Rect? = null,
        
        /**
         * Whether element is clickable
         */
        val isClickable: Boolean = false,
        
        /**
         * Whether element is focusable
         */
        val isFocusable: Boolean = false,
        
        /**
         * Whether element is currently focused
         */
        val isFocused: Boolean = false,
        
        /**
         * Whether element is enabled
         */
        val isEnabled: Boolean = true,
        
        /**
         * Whether element is visible to user
         */
        val isVisibleToUser: Boolean = true,
        
        /**
         * Whether element is selected (for checkboxes, radio buttons)
         */
        val isSelected: Boolean = false,
        
        /**
         * Whether element is checkable
         */
        val isCheckable: Boolean = false,
        
        /**
         * Whether element is checked (if checkable)
         */
        val isChecked: Boolean = false,
        
        /**
         * Element type for voice commands
         */
        val elementType: ElementType = ElementType.UNKNOWN,
        
        /**
         * Available actions on this element
         */
        val availableActions: List<String> = emptyList()
    ) : Parcelable {
        
        /**
         * Get display text for voice commands
         */
        fun getDisplayText(): String? {
            return when {
                !text.isNullOrBlank() -> text
                !contentDescription.isNullOrBlank() -> contentDescription
                !id.isNullOrBlank() -> id
                else -> null
            }
        }
        
        /**
         * Check if element can be interacted with
         */
        fun isInteractive(): Boolean {
            return isEnabled && isVisibleToUser && (isClickable || isFocusable)
        }
    }
    
    /**
     * Information about input fields
     */
    @Parcelize
    data class InputFieldInfo(
        /**
         * Current text in the input field
         */
        val currentText: String? = null,
        
        /**
         * Hint text shown in empty field
         */
        val hintText: String? = null,
        
        /**
         * Input type (text, number, email, etc.)
         */
        val inputType: Int = 0,
        
        /**
         * Whether field is password type
         */
        val isPassword: Boolean = false,
        
        /**
         * Whether field is multiline
         */
        val isMultiline: Boolean = false,
        
        /**
         * Cursor position in text
         */
        val cursorPosition: Int = 0,
        
        /**
         * Selected text range
         */
        val selectionStart: Int = 0,
        val selectionEnd: Int = 0
    ) : Parcelable
    
    /**
     * Screen orientation enumeration
     */
    enum class ScreenOrientation {
        PORTRAIT,
        LANDSCAPE,
        UNKNOWN
    }
    
    /**
     * Element type enumeration for voice command optimization
     */
    enum class ElementType {
        BUTTON,
        TEXT_FIELD,
        CHECKBOX,
        RADIO_BUTTON,
        SWITCH,
        SLIDER,
        PROGRESS_BAR,
        IMAGE,
        LINK,
        TAB,
        MENU_ITEM,
        LIST_ITEM,
        GRID_ITEM,
        TOOLBAR,
        NAVIGATION,
        DIALOG,
        POPUP,
        UNKNOWN
    }
    
    /**
     * Check if UI state has meaningful content
     */
    fun hasContent(): Boolean {
        return !packageName.isNullOrBlank() || 
               interactiveElements.isNotEmpty() || 
               !screenText.isNullOrBlank()
    }
    
    /**
     * Get clickable elements
     */
    fun getClickableElements(): List<ElementInfo> {
        return interactiveElements.filter { it.isClickable && it.isInteractive() }
    }
    
    /**
     * Get focusable elements
     */
    fun getFocusableElements(): List<ElementInfo> {
        return interactiveElements.filter { it.isFocusable && it.isInteractive() }
    }
    
    /**
     * Get elements of a specific type
     */
    fun getElementsByType(type: ElementType): List<ElementInfo> {
        return interactiveElements.filter { it.elementType == type }
    }
    
    /**
     * Find element by text content
     */
    fun findElementByText(text: String, ignoreCase: Boolean = true): ElementInfo? {
        return interactiveElements.find { element ->
            val displayText = element.getDisplayText()
            if (ignoreCase) {
                displayText?.equals(text, ignoreCase = true) == true
            } else {
                displayText == text
            }
        }
    }
    
    /**
     * Find elements containing text
     */
    fun findElementsContaining(text: String, ignoreCase: Boolean = true): List<ElementInfo> {
        return interactiveElements.filter { element ->
            val displayText = element.getDisplayText()
            if (ignoreCase) {
                displayText?.contains(text, ignoreCase = true) == true
            } else {
                displayText?.contains(text) == true
            }
        }
    }
    
    /**
     * Get summary for voice feedback
     */
    fun getSummary(): String {
        return buildString {
            if (!packageName.isNullOrBlank()) {
                append("App: $packageName")
            }
            
            if (!windowTitle.isNullOrBlank()) {
                if (isNotEmpty()) append(", ")
                append("Screen: $windowTitle")
            }
            
            val clickableCount = getClickableElements().size
            if (clickableCount > 0) {
                if (isNotEmpty()) append(", ")
                append("$clickableCount interactive elements")
            }
            
            focusedElement?.let { focused ->
                if (isNotEmpty()) append(", ")
                append("Focused: ${focused.getDisplayText() ?: focused.className}")
            }
            
            if (isKeyboardVisible) {
                if (isNotEmpty()) append(", ")
                append("Keyboard visible")
            }
        }
    }
    
    /**
     * Create a copy with updated timestamp
     */
    fun withCurrentTimestamp(): UIState {
        return copy(timestamp = System.currentTimeMillis())
    }
    
    /**
     * Create a copy with additional metadata
     */
    fun withMetadata(key: String, value: String): UIState {
        return copy(metadata = metadata + (key to value))
    }
    
    companion object {
        /**
         * Create empty UI state
         */
        fun empty(): UIState = UIState()
        
        /**
         * Create UI state from AccessibilityNodeInfo
         */
        fun fromAccessibilityNode(
            rootNode: AccessibilityNodeInfo?,
            packageName: String? = null,
            activityName: String? = null
        ): UIState {
            if (rootNode == null) {
                return empty()
            }
            
            val elements = mutableListOf<ElementInfo>()
            val screenTextBuilder = StringBuilder()
            
            // Extract elements recursively
            extractElements(rootNode, elements, screenTextBuilder)
            
            // Find focused element
            val focusedElement = elements.find { it.isFocused }
            
            // Determine if screen is scrollable
            val isScrollable = elements.any { element ->
                element.availableActions.any { action ->
                    action.contains("scroll", ignoreCase = true)
                }
            }
            
            return UIState(
                packageName = packageName,
                activityName = activityName,
                windowTitle = rootNode.text?.toString(),
                focusedElement = focusedElement,
                interactiveElements = elements,
                screenText = screenTextBuilder.toString().trim().takeIf { it.isNotBlank() },
                isScrollable = isScrollable
            )
        }
        
        /**
         * Recursively extract elements from AccessibilityNodeInfo
         */
        private fun extractElements(
            node: AccessibilityNodeInfo,
            elements: MutableList<ElementInfo>,
            screenTextBuilder: StringBuilder
        ) {
            // Add text to screen content
            node.text?.let { text ->
                if (text.isNotBlank()) {
                    screenTextBuilder.append(text).append(" ")
                }
            }
            
            // Create element info if interactive
            if (node.isClickable || node.isFocusable || node.isCheckable) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                
                val elementType = determineElementType(node)
                val actions = node.actionList?.map { it.toString() } ?: emptyList()
                
                val element = ElementInfo(
                    id = node.viewIdResourceName,
                    text = node.text?.toString(),
                    contentDescription = node.contentDescription?.toString(),
                    className = node.className?.toString(),
                    bounds = bounds,
                    isClickable = node.isClickable,
                    isFocusable = node.isFocusable,
                    isFocused = node.isFocused,
                    isEnabled = node.isEnabled,
                    isVisibleToUser = node.isVisibleToUser,
                    isSelected = node.isSelected,
                    isCheckable = node.isCheckable,
                    isChecked = node.isChecked,
                    elementType = elementType,
                    availableActions = actions
                )
                
                elements.add(element)
            }
            
            // Process children
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    extractElements(child, elements, screenTextBuilder)
                    // child.recycle() // Deprecated - Android handles this automatically
                }
            }
        }
        
        /**
         * Determine element type from AccessibilityNodeInfo
         */
        private fun determineElementType(node: AccessibilityNodeInfo): ElementType {
            val className = node.className?.toString()?.lowercase() ?: return ElementType.UNKNOWN
            
            return when {
                className.contains("button") -> ElementType.BUTTON
                className.contains("edittext") || className.contains("textfield") -> ElementType.TEXT_FIELD
                className.contains("checkbox") -> ElementType.CHECKBOX
                className.contains("radiobutton") -> ElementType.RADIO_BUTTON
                className.contains("switch") -> ElementType.SWITCH
                className.contains("seekbar") || className.contains("slider") -> ElementType.SLIDER
                className.contains("progressbar") -> ElementType.PROGRESS_BAR
                className.contains("imageview") || className.contains("image") -> ElementType.IMAGE
                className.contains("textview") && node.isClickable -> ElementType.LINK
                className.contains("tab") -> ElementType.TAB
                className.contains("menuitem") -> ElementType.MENU_ITEM
                className.contains("listview") || className.contains("recyclerview") -> ElementType.LIST_ITEM
                className.contains("gridview") -> ElementType.GRID_ITEM
                className.contains("toolbar") -> ElementType.TOOLBAR
                className.contains("navigation") -> ElementType.NAVIGATION
                else -> ElementType.UNKNOWN
            }
        }
    }
}