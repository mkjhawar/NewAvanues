/**
 * AdaptiveScope.kt - DSL scope for adaptive UI composition
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-24
 */
package com.augmentalis.voiceui.universalui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import com.augmentalis.voiceui.designer.VoiceUIElement
import com.augmentalis.voiceui.designer.ElementType

/**
 * DSL scope for building adaptive UI compositions
 * Provides direct access to UI building functions without helper methods
 */
@Stable
class AdaptiveScope {
    
    private val elements = mutableListOf<VoiceUIElement>()
    
    /**
     * Add a text element
     */
    @Composable
    fun text(content: String, modifier: Modifier = Modifier) {
        val element = VoiceUIElement(
            type = ElementType.TEXT,
            name = "text_${System.currentTimeMillis()}"
        )
        elements.add(element)
        
        // Render the text
        androidx.compose.material3.Text(
            text = content,
            modifier = modifier
        )
    }
    
    /**
     * Add a button element
     */
    @Composable
    fun button(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val element = VoiceUIElement(
            type = ElementType.BUTTON,
            name = "button_${System.currentTimeMillis()}"
        )
        elements.add(element)
        
        // Render the button
        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = modifier
        ) {
            androidx.compose.material3.Text(text)
        }
    }
    
    /**
     * Add a card element
     */
    @Composable
    fun card(
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit
    ) {
        androidx.compose.material3.Card(
            modifier = modifier
        ) {
            Column {
                content()
            }
        }
    }
    
    /**
     * Add a column layout
     */
    @Composable
    fun column(
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(modifier = modifier) {
            content()
        }
    }
    
    /**
     * Add a row layout
     */
    @Composable
    fun row(
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
    ) {
        Row(modifier = modifier) {
            content()
        }
    }
    
    /**
     * Add a spacer
     */
    @Composable
    fun spacer(modifier: Modifier = Modifier) {
        Spacer(modifier = modifier)
    }
    
    /**
     * Get all elements (direct access, no helper method)
     */
    val allElements: List<VoiceUIElement>
        get() = elements.toList()
}

// Extension to add missing content property
private var VoiceUIElement.content: String
    get() = name
    set(value) {
        // Direct property access pattern
    }
