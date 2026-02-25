/**
 * TypeCodeTest.kt - Unit tests for TypeCode constants and fromTypeName()
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypeCodeTest {

    // Collect every constant declared on TypeCode via reflection-free explicit list.
    // This list mirrors the object body exactly to catch future additions.
    private val allCodes = listOf(
        TypeCode.BUTTON, TypeCode.INPUT, TypeCode.TEXT, TypeCode.IMAGE, TypeCode.SCROLL,
        TypeCode.CARD, TypeCode.LIST, TypeCode.ITEM, TypeCode.MENU, TypeCode.DIALOG,
        TypeCode.CHECKBOX, TypeCode.SWITCH, TypeCode.SLIDER, TypeCode.TAB, TypeCode.LAYOUT,
        TypeCode.MESSAGE, TypeCode.CONVERSATION, TypeCode.DOCUMENT, TypeCode.CHUNK,
        TypeCode.MEMORY, TypeCode.DECISION, TypeCode.LEARNING, TypeCode.INTENT,
        TypeCode.CLUSTER, TypeCode.BOOKMARK, TypeCode.ANNOTATION, TypeCode.FILTER,
        TypeCode.UTTERANCE, TypeCode.FAVORITE, TypeCode.DOWNLOAD, TypeCode.HISTORY,
        TypeCode.SESSION, TypeCode.GROUP, TypeCode.REQUEST, TypeCode.WINDOW,
        TypeCode.STREAM, TypeCode.PRESET, TypeCode.DEVICE, TypeCode.SYNC,
        TypeCode.ELEMENT, TypeCode.COMMAND, TypeCode.SCREEN, TypeCode.APP
    )

    @Test
    fun allTypeCodes_areExactlyThreeCharacters() {
        allCodes.forEach { code ->
            assertEquals(3, code.length, "TypeCode '$code' must be exactly 3 characters")
        }
    }

    @Test
    fun allTypeCodes_areUppercase() {
        allCodes.forEach { code ->
            assertEquals(code, code.uppercase(), "TypeCode '$code' must be uppercase")
        }
    }

    @Test
    fun allTypeCodes_areUnique() {
        val unique = allCodes.toSet()
        assertEquals(allCodes.size, unique.size, "TypeCode list contains duplicates: ${allCodes.groupBy { it }.filter { it.value.size > 1 }.keys}")
    }

    @Test
    fun fromTypeName_knownTypesResolveToCodes() {
        assertEquals(TypeCode.BUTTON, TypeCode.fromTypeName("button"))
        assertEquals(TypeCode.BUTTON, TypeCode.fromTypeName("ImageButton"))
        assertEquals(TypeCode.INPUT, TypeCode.fromTypeName("edittext"))
        assertEquals(TypeCode.MESSAGE, TypeCode.fromTypeName("chatmessage"))
        assertEquals(TypeCode.DIALOG, TypeCode.fromTypeName("alertdialog"))
        assertEquals(TypeCode.COMMAND, TypeCode.fromTypeName("command"))
    }

    @Test
    fun fromTypeName_isCaseInsensitive() {
        assertEquals(TypeCode.fromTypeName("BUTTON"), TypeCode.fromTypeName("button"))
        assertEquals(TypeCode.fromTypeName("Slider"), TypeCode.fromTypeName("seekbar"))
    }

    @Test
    fun fromTypeName_unknownInputFallsBackToElement() {
        assertEquals(TypeCode.ELEMENT, TypeCode.fromTypeName("totally_unknown_widget"))
        assertEquals(TypeCode.ELEMENT, TypeCode.fromTypeName(""))
    }

    @Test
    fun allCodeValues_containOnlyAlphaChars() {
        allCodes.forEach { code ->
            assertTrue(code.all { it.isLetter() }, "TypeCode '$code' must contain only letters")
        }
    }
}
