/**
 * AVUQuantizerIntegrationUnitTest.kt - Unit tests for AVUQuantizerIntegration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-26
 *
 * Tests the classifyElementType() method which classifies Android UI elements
 * into semantic ElementType categories for LLM consumption.
 */

package com.augmentalis.voiceoscore.learnapp.ai.quantized

import android.content.Context
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AVUQuantizerIntegration.classifyElementType()
 *
 * Tests the element type classification logic that converts Android view class names
 * to semantic ElementType enum values for NLU/LLM processing.
 */
class AVUQuantizerIntegrationUnitTest : BaseVoiceOSTest() {

    private lateinit var integration: AVUQuantizerIntegration
    private lateinit var mockContext: Context

    @Before
    override fun setUp() {
        super.setUp()
        mockContext = mockk(relaxed = true)
        integration = AVUQuantizerIntegration(context = mockContext)
    }

    /**
     * Helper to invoke private classifyElementType method via reflection
     */
    private fun classifyElementType(className: String?): ElementType {
        val method = AVUQuantizerIntegration::class.java.getDeclaredMethod(
            "classifyElementType",
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(integration, className) as ElementType
    }

    // ============================================
    // Null Input Tests
    // ============================================

    @Test
    fun `classifyElementType returns OTHER for null className`() {
        val result = classifyElementType(null)
        assertEquals(ElementType.OTHER, result)
    }

    // ============================================
    // Button Classification Tests
    // ============================================

    @Test
    fun `classifyElementType returns BUTTON for android_widget_Button`() {
        val result = classifyElementType("android.widget.Button")
        assertEquals(ElementType.BUTTON, result)
    }

    @Test
    fun `classifyElementType returns BUTTON for android_widget_ImageButton`() {
        val result = classifyElementType("android.widget.ImageButton")
        assertEquals(ElementType.BUTTON, result)
    }

    @Test
    fun `classifyElementType returns BUTTON for AppCompatButton`() {
        val result = classifyElementType("androidx.appcompat.widget.AppCompatButton")
        assertEquals(ElementType.BUTTON, result)
    }

    @Test
    fun `classifyElementType returns BUTTON for MaterialButton`() {
        val result = classifyElementType("com.google.android.material.button.MaterialButton")
        assertEquals(ElementType.BUTTON, result)
    }

    @Test
    fun `classifyElementType is case insensitive for Button`() {
        assertEquals(ElementType.BUTTON, classifyElementType("button"))
        assertEquals(ElementType.BUTTON, classifyElementType("BUTTON"))
        assertEquals(ElementType.BUTTON, classifyElementType("BuTtOn"))
    }

    // ============================================
    // Text Field Classification Tests
    // ============================================

    @Test
    fun `classifyElementType returns TEXT_FIELD for android_widget_EditText`() {
        val result = classifyElementType("android.widget.EditText")
        assertEquals(ElementType.TEXT_FIELD, result)
    }

    @Test
    fun `classifyElementType returns TEXT_FIELD for TextInputEditText`() {
        val result = classifyElementType("com.google.android.material.textfield.TextInputEditText")
        assertEquals(ElementType.TEXT_FIELD, result)
    }

    @Test
    fun `classifyElementType returns TEXT_FIELD for AutoCompleteTextView`() {
        val result = classifyElementType("android.widget.AutoCompleteTextView")
        assertEquals(ElementType.TEXT_FIELD, result)
    }

    @Test
    fun `classifyElementType returns TEXT_FIELD for MultiAutoCompleteTextView`() {
        val result = classifyElementType("android.widget.MultiAutoCompleteTextView")
        assertEquals(ElementType.TEXT_FIELD, result)
    }

    @Test
    fun `classifyElementType returns TEXT_FIELD for AppCompatEditText`() {
        val result = classifyElementType("androidx.appcompat.widget.AppCompatEditText")
        assertEquals(ElementType.TEXT_FIELD, result)
    }

    // ============================================
    // Checkbox Classification Tests
    // ============================================

    @Test
    fun `classifyElementType returns CHECKBOX for android_widget_CheckBox`() {
        val result = classifyElementType("android.widget.CheckBox")
        assertEquals(ElementType.CHECKBOX, result)
    }

    @Test
    fun `classifyElementType returns CHECKBOX for AppCompatCheckBox`() {
        val result = classifyElementType("androidx.appcompat.widget.AppCompatCheckBox")
        assertEquals(ElementType.CHECKBOX, result)
    }

    @Test
    fun `classifyElementType returns CHECKBOX for MaterialCheckBox`() {
        val result = classifyElementType("com.google.android.material.checkbox.MaterialCheckBox")
        assertEquals(ElementType.CHECKBOX, result)
    }

    // ============================================
    // Switch Classification Tests
    // ============================================

    @Test
    fun `classifyElementType returns SWITCH for android_widget_Switch`() {
        val result = classifyElementType("android.widget.Switch")
        assertEquals(ElementType.SWITCH, result)
    }

    @Test
    fun `classifyElementType returns SWITCH for SwitchCompat`() {
        val result = classifyElementType("androidx.appcompat.widget.SwitchCompat")
        assertEquals(ElementType.SWITCH, result)
    }

    @Test
    fun `classifyElementType returns SWITCH for SwitchMaterial`() {
        val result = classifyElementType("com.google.android.material.switchmaterial.SwitchMaterial")
        assertEquals(ElementType.SWITCH, result)
    }

    @Test
    fun `classifyElementType returns BUTTON for ToggleButton`() {
        // Note: ToggleButton contains "Button" in its name, so it's classified as BUTTON
        // by the current implementation which checks "Button" before "Toggle".
        // This is actually reasonable since ToggleButton acts as a button with two states.
        val result = classifyElementType("android.widget.ToggleButton")
        assertEquals(ElementType.BUTTON, result)
    }

    @Test
    fun `classifyElementType returns SWITCH for CompoundButton with Toggle`() {
        val result = classifyElementType("custom.ToggleSwitch")
        assertEquals(ElementType.SWITCH, result)
    }

    // ============================================
    // Dropdown Classification Tests
    // ============================================

    @Test
    fun `classifyElementType returns DROPDOWN for android_widget_Spinner`() {
        val result = classifyElementType("android.widget.Spinner")
        assertEquals(ElementType.DROPDOWN, result)
    }

    @Test
    fun `classifyElementType returns DROPDOWN for AppCompatSpinner`() {
        val result = classifyElementType("androidx.appcompat.widget.AppCompatSpinner")
        assertEquals(ElementType.DROPDOWN, result)
    }

    @Test
    fun `classifyElementType returns DROPDOWN for DropDown widget`() {
        val result = classifyElementType("com.custom.DropDownMenu")
        assertEquals(ElementType.DROPDOWN, result)
    }

    // ============================================
    // Tab Classification Tests
    // ============================================

    @Test
    fun `classifyElementType returns TAB for TabHost`() {
        val result = classifyElementType("android.widget.TabHost")
        assertEquals(ElementType.TAB, result)
    }

    @Test
    fun `classifyElementType returns TAB for TabLayout`() {
        val result = classifyElementType("com.google.android.material.tabs.TabLayout")
        assertEquals(ElementType.TAB, result)
    }

    @Test
    fun `classifyElementType returns TAB for TabItem`() {
        val result = classifyElementType("com.google.android.material.tabs.TabItem")
        assertEquals(ElementType.TAB, result)
    }

    @Test
    fun `classifyElementType returns TAB for TabView`() {
        val result = classifyElementType("custom.TabView")
        assertEquals(ElementType.TAB, result)
    }

    // ============================================
    // Other/Unknown Classification Tests
    // ============================================

    @Test
    fun `classifyElementType returns OTHER for TextView`() {
        val result = classifyElementType("android.widget.TextView")
        assertEquals(ElementType.OTHER, result)
    }

    @Test
    fun `classifyElementType returns OTHER for ImageView`() {
        val result = classifyElementType("android.widget.ImageView")
        assertEquals(ElementType.OTHER, result)
    }

    @Test
    fun `classifyElementType returns OTHER for LinearLayout`() {
        val result = classifyElementType("android.widget.LinearLayout")
        assertEquals(ElementType.OTHER, result)
    }

    @Test
    fun `classifyElementType returns OTHER for RecyclerView`() {
        val result = classifyElementType("androidx.recyclerview.widget.RecyclerView")
        assertEquals(ElementType.OTHER, result)
    }

    @Test
    fun `classifyElementType returns OTHER for custom widget without known pattern`() {
        val result = classifyElementType("com.custom.MyCustomWidget")
        assertEquals(ElementType.OTHER, result)
    }

    @Test
    fun `classifyElementType returns OTHER for empty string`() {
        val result = classifyElementType("")
        assertEquals(ElementType.OTHER, result)
    }

    // ============================================
    // Edge Cases
    // ============================================

    @Test
    fun `classifyElementType handles compound names correctly`() {
        // ImageButton should match Button first due to order in when clause
        val result = classifyElementType("android.widget.ImageButton")
        assertEquals(ElementType.BUTTON, result)
    }

    @Test
    fun `classifyElementType handles obfuscated class names`() {
        // Obfuscated names without known patterns should return OTHER
        val result = classifyElementType("a.b.c")
        assertEquals(ElementType.OTHER, result)
    }

    @Test
    fun `classifyElementType handles class names with numbers`() {
        val result = classifyElementType("com.custom.Button2")
        assertEquals(ElementType.BUTTON, result)
    }

    @Test
    fun `classifyElementType handles inner class names`() {
        val result = classifyElementType("com.custom.MyView\$Button")
        assertEquals(ElementType.BUTTON, result)
    }
}
