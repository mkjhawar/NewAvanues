package com.augmentalis.avaelements.core.api

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.Theme

/**
 * Platform-specific renderer interface
 * Renders components to native UI elements
 */
interface Renderer {
    /**
     * Current theme for this renderer
     */
    val theme: Theme
    
    /**
     * Render a component to platform-specific representation
     * Components automatically inherit theme unless overridden by style
     */
    fun render(component: Component): Any
}
