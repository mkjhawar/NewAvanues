package com.augmentalis.voiceoscoreng.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD Tests for VUIDGenerator
 *
 * VUID = Voice Unique Identifier (compact format replacing UUID)
 * Format: {pkgHash6}-{typeCode}{hash8}
 * Example: a3f2e1-b917cc9dc (16 chars total)
 */
class VUIDGeneratorTest {

    // ==================== generate() Tests ====================

    @Test
    fun `generate creates VUID with valid format of 16 chars`() {
        val vuid = VUIDGenerator.generate(
            packageName = "com.example.app",
            typeCode = VUIDTypeCode.BUTTON,
            elementHash = "testElement123"
        )

        assertEquals(16, vuid.length, "VUID should be exactly 16 characters")
    }

    @Test
    fun `generate creates VUID with correct structure pkgHash-typeCodeHash`() {
        val vuid = VUIDGenerator.generate(
            packageName = "com.example.app",
            typeCode = VUIDTypeCode.BUTTON,
            elementHash = "testElement"
        )

        // Format: {pkgHash6}-{typeCode1}{hash8}
        assertTrue(vuid.contains("-"), "VUID should contain a hyphen separator")

        val parts = vuid.split("-")
        assertEquals(2, parts.size, "VUID should have exactly 2 parts separated by hyphen")
        assertEquals(6, parts[0].length, "Package hash should be 6 characters")
        assertEquals(9, parts[1].length, "Type code + element hash should be 9 characters (1+8)")
    }

    @Test
    fun `generate includes correct type code in VUID`() {
        val buttonVuid = VUIDGenerator.generate("com.app", VUIDTypeCode.BUTTON, "elem1")
        val inputVuid = VUIDGenerator.generate("com.app", VUIDTypeCode.INPUT, "elem2")
        val textVuid = VUIDGenerator.generate("com.app", VUIDTypeCode.TEXT, "elem3")

        // Type code is the first character after the hyphen
        assertEquals('b', buttonVuid.split("-")[1][0], "Button type code should be 'b'")
        assertEquals('i', inputVuid.split("-")[1][0], "Input type code should be 'i'")
        assertEquals('t', textVuid.split("-")[1][0], "Text type code should be 't'")
    }

    @Test
    fun `generate produces consistent output for same inputs`() {
        val vuid1 = VUIDGenerator.generate("com.example.app", VUIDTypeCode.BUTTON, "element123")
        val vuid2 = VUIDGenerator.generate("com.example.app", VUIDTypeCode.BUTTON, "element123")

        assertEquals(vuid1, vuid2, "Same inputs should produce same VUID")
    }

    @Test
    fun `generate produces different output for different element hashes`() {
        val vuid1 = VUIDGenerator.generate("com.example.app", VUIDTypeCode.BUTTON, "element1")
        val vuid2 = VUIDGenerator.generate("com.example.app", VUIDTypeCode.BUTTON, "element2")

        assertTrue(vuid1 != vuid2, "Different element hashes should produce different VUIDs")
    }

    @Test
    fun `generate produces different output for different packages`() {
        val vuid1 = VUIDGenerator.generate("com.app1", VUIDTypeCode.BUTTON, "element")
        val vuid2 = VUIDGenerator.generate("com.app2", VUIDTypeCode.BUTTON, "element")

        assertTrue(vuid1 != vuid2, "Different packages should produce different VUIDs")
    }

    @Test
    fun `generate only uses lowercase hexadecimal characters`() {
        val vuid = VUIDGenerator.generate("com.example.app", VUIDTypeCode.BUTTON, "element")

        val validChars = "0123456789abcdef-"
        assertTrue(
            vuid.all { it in validChars },
            "VUID should only contain lowercase hex chars and hyphen: $vuid"
        )
    }

    // ==================== generatePackageHash() Tests ====================

    @Test
    fun `generatePackageHash returns exactly 6 characters`() {
        val hash = VUIDGenerator.generatePackageHash("com.example.app")

        assertEquals(6, hash.length, "Package hash should be exactly 6 characters")
    }

    @Test
    fun `generatePackageHash produces consistent output`() {
        val hash1 = VUIDGenerator.generatePackageHash("com.example.app")
        val hash2 = VUIDGenerator.generatePackageHash("com.example.app")

        assertEquals(hash1, hash2, "Same package name should produce same hash")
    }

    @Test
    fun `generatePackageHash produces different output for different packages`() {
        val hash1 = VUIDGenerator.generatePackageHash("com.example.app1")
        val hash2 = VUIDGenerator.generatePackageHash("com.example.app2")

        assertTrue(hash1 != hash2, "Different packages should produce different hashes")
    }

    @Test
    fun `generatePackageHash only uses lowercase hexadecimal characters`() {
        val hash = VUIDGenerator.generatePackageHash("com.example.app")

        val validChars = "0123456789abcdef"
        assertTrue(
            hash.all { it in validChars },
            "Package hash should only contain lowercase hex chars: $hash"
        )
    }

    @Test
    fun `generatePackageHash handles empty string`() {
        val hash = VUIDGenerator.generatePackageHash("")

        assertEquals(6, hash.length, "Empty string should still produce 6 char hash")
    }

    // ==================== isValidVUID() Tests ====================

    @Test
    fun `isValidVUID returns true for valid VUID format`() {
        val validVuid = "a3f2e1-b917cc9dc"

        assertTrue(VUIDGenerator.isValidVUID(validVuid), "Should accept valid VUID")
    }

    @Test
    fun `isValidVUID returns true for all type codes`() {
        val typeCodes = listOf('b', 'i', 's', 't', 'e', 'c', 'l', 'm', 'd', 'g')

        typeCodes.forEach { typeCode ->
            val vuid = "a3f2e1-${typeCode}917cc9dc"  // 8 chars after type code
            assertTrue(
                VUIDGenerator.isValidVUID(vuid),
                "Should accept VUID with type code '$typeCode': $vuid"
            )
        }
    }

    @Test
    fun `isValidVUID rejects VUID with wrong length`() {
        assertFalse(VUIDGenerator.isValidVUID("a3f2e1-b917cc9"), "Too short")
        assertFalse(VUIDGenerator.isValidVUID("a3f2e1-b917cc9dca"), "Too long")
        assertFalse(VUIDGenerator.isValidVUID("a3f2e-b917cc9dc"), "Package hash too short")
    }

    @Test
    fun `isValidVUID rejects VUID without hyphen`() {
        assertFalse(VUIDGenerator.isValidVUID("a3f2e1b917cc9dc"), "Missing hyphen")
    }

    @Test
    fun `isValidVUID rejects VUID with invalid type code`() {
        assertFalse(VUIDGenerator.isValidVUID("a3f2e1-x917cc9dc"), "Invalid type code 'x'")
        assertFalse(VUIDGenerator.isValidVUID("a3f2e1-z917cc9dc"), "Invalid type code 'z'")
    }

    @Test
    fun `isValidVUID rejects VUID with uppercase characters`() {
        assertFalse(VUIDGenerator.isValidVUID("A3F2E1-b917cc9dc"), "Uppercase in package hash")
        assertFalse(VUIDGenerator.isValidVUID("a3f2e1-b917CC9DC"), "Uppercase in element hash")
    }

    @Test
    fun `isValidVUID rejects VUID with non-hex characters`() {
        assertFalse(VUIDGenerator.isValidVUID("a3f2e1-bgggcc9dc"), "Non-hex chars in element hash")
        assertFalse(VUIDGenerator.isValidVUID("gggggg-b917cc9dc"), "Non-hex chars in package hash")
    }

    @Test
    fun `isValidVUID rejects empty string`() {
        assertFalse(VUIDGenerator.isValidVUID(""), "Empty string")
    }

    @Test
    fun `isValidVUID rejects null-like strings`() {
        assertFalse(VUIDGenerator.isValidVUID("null"), "null string")
        assertFalse(VUIDGenerator.isValidVUID("undefined"), "undefined string")
    }

    // ==================== parseVUID() Tests ====================

    @Test
    fun `parseVUID extracts components from valid VUID`() {
        val vuid = "a3f2e1-b917cc9dc"
        val components = VUIDGenerator.parseVUID(vuid)

        assertNotNull(components, "Should parse valid VUID")
        assertEquals("a3f2e1", components.packageHash, "Package hash should match")
        assertEquals(VUIDTypeCode.BUTTON, components.typeCode, "Type code should be BUTTON")
        assertEquals("917cc9dc", components.elementHash, "Element hash should match")
    }

    @Test
    fun `parseVUID correctly identifies all type codes`() {
        val testCases = mapOf(
            "a3f2e1-b917cc9dc" to VUIDTypeCode.BUTTON,
            "a3f2e1-i917cc9dc" to VUIDTypeCode.INPUT,
            "a3f2e1-s917cc9dc" to VUIDTypeCode.SCROLL,
            "a3f2e1-t917cc9dc" to VUIDTypeCode.TEXT,
            "a3f2e1-e917cc9dc" to VUIDTypeCode.ELEMENT,
            "a3f2e1-c917cc9dc" to VUIDTypeCode.CARD,
            "a3f2e1-l917cc9dc" to VUIDTypeCode.LAYOUT,
            "a3f2e1-m917cc9dc" to VUIDTypeCode.MENU,
            "a3f2e1-d917cc9dc" to VUIDTypeCode.DIALOG,
            "a3f2e1-g917cc9dc" to VUIDTypeCode.IMAGE
        )

        testCases.forEach { (vuid, expectedTypeCode) ->
            val components = VUIDGenerator.parseVUID(vuid)
            assertNotNull(components, "Should parse VUID: $vuid")
            assertEquals(
                expectedTypeCode,
                components.typeCode,
                "Type code for $vuid should be $expectedTypeCode"
            )
        }
    }

    @Test
    fun `parseVUID returns null for invalid VUID`() {
        assertNull(VUIDGenerator.parseVUID("invalid"), "Should return null for invalid VUID")
        assertNull(VUIDGenerator.parseVUID("a3f2e1-x917cc9dc"), "Should return null for invalid type code")
        assertNull(VUIDGenerator.parseVUID(""), "Should return null for empty string")
    }

    @Test
    fun `parseVUID roundtrip with generate`() {
        val originalVuid = VUIDGenerator.generate("com.example.app", VUIDTypeCode.BUTTON, "element123")
        val components = VUIDGenerator.parseVUID(originalVuid)

        assertNotNull(components, "Should parse generated VUID")
        assertEquals(VUIDTypeCode.BUTTON, components.typeCode, "Type code should match")

        // Verify the parsed components can reconstruct a valid VUID structure
        assertTrue(VUIDGenerator.isValidVUID(originalVuid), "Generated VUID should be valid")
    }

    // ==================== getTypeCode() Tests ====================

    @Test
    fun `getTypeCode maps Button class name to BUTTON code`() {
        assertEquals(VUIDTypeCode.BUTTON, VUIDGenerator.getTypeCode("Button"))
        assertEquals(VUIDTypeCode.BUTTON, VUIDGenerator.getTypeCode("AppCompatButton"))
        assertEquals(VUIDTypeCode.BUTTON, VUIDGenerator.getTypeCode("MaterialButton"))
        assertEquals(VUIDTypeCode.BUTTON, VUIDGenerator.getTypeCode("ImageButton"))
        assertEquals(VUIDTypeCode.BUTTON, VUIDGenerator.getTypeCode("FloatingActionButton"))
    }

    @Test
    fun `getTypeCode maps EditText and input classes to INPUT code`() {
        assertEquals(VUIDTypeCode.INPUT, VUIDGenerator.getTypeCode("EditText"))
        assertEquals(VUIDTypeCode.INPUT, VUIDGenerator.getTypeCode("TextInputEditText"))
        assertEquals(VUIDTypeCode.INPUT, VUIDGenerator.getTypeCode("AutoCompleteTextView"))
        assertEquals(VUIDTypeCode.INPUT, VUIDGenerator.getTypeCode("SearchView"))
        assertEquals(VUIDTypeCode.INPUT, VUIDGenerator.getTypeCode("TextField"))
    }

    @Test
    fun `getTypeCode maps scroll classes to SCROLL code`() {
        assertEquals(VUIDTypeCode.SCROLL, VUIDGenerator.getTypeCode("ScrollView"))
        assertEquals(VUIDTypeCode.SCROLL, VUIDGenerator.getTypeCode("HorizontalScrollView"))
        assertEquals(VUIDTypeCode.SCROLL, VUIDGenerator.getTypeCode("NestedScrollView"))
        assertEquals(VUIDTypeCode.SCROLL, VUIDGenerator.getTypeCode("RecyclerView"))
        assertEquals(VUIDTypeCode.SCROLL, VUIDGenerator.getTypeCode("ListView"))
        assertEquals(VUIDTypeCode.SCROLL, VUIDGenerator.getTypeCode("LazyColumn"))
        assertEquals(VUIDTypeCode.SCROLL, VUIDGenerator.getTypeCode("LazyRow"))
    }

    @Test
    fun `getTypeCode maps text classes to TEXT code`() {
        assertEquals(VUIDTypeCode.TEXT, VUIDGenerator.getTypeCode("TextView"))
        assertEquals(VUIDTypeCode.TEXT, VUIDGenerator.getTypeCode("AppCompatTextView"))
        assertEquals(VUIDTypeCode.TEXT, VUIDGenerator.getTypeCode("MaterialTextView"))
        assertEquals(VUIDTypeCode.TEXT, VUIDGenerator.getTypeCode("Text"))
    }

    @Test
    fun `getTypeCode maps card classes to CARD code`() {
        assertEquals(VUIDTypeCode.CARD, VUIDGenerator.getTypeCode("CardView"))
        assertEquals(VUIDTypeCode.CARD, VUIDGenerator.getTypeCode("MaterialCardView"))
        assertEquals(VUIDTypeCode.CARD, VUIDGenerator.getTypeCode("Card"))
    }

    @Test
    fun `getTypeCode maps layout classes to LAYOUT code`() {
        assertEquals(VUIDTypeCode.LAYOUT, VUIDGenerator.getTypeCode("LinearLayout"))
        assertEquals(VUIDTypeCode.LAYOUT, VUIDGenerator.getTypeCode("RelativeLayout"))
        assertEquals(VUIDTypeCode.LAYOUT, VUIDGenerator.getTypeCode("FrameLayout"))
        assertEquals(VUIDTypeCode.LAYOUT, VUIDGenerator.getTypeCode("ConstraintLayout"))
        assertEquals(VUIDTypeCode.LAYOUT, VUIDGenerator.getTypeCode("CoordinatorLayout"))
        assertEquals(VUIDTypeCode.LAYOUT, VUIDGenerator.getTypeCode("Row"))
        assertEquals(VUIDTypeCode.LAYOUT, VUIDGenerator.getTypeCode("Column"))
        assertEquals(VUIDTypeCode.LAYOUT, VUIDGenerator.getTypeCode("Box"))
    }

    @Test
    fun `getTypeCode maps menu classes to MENU code`() {
        assertEquals(VUIDTypeCode.MENU, VUIDGenerator.getTypeCode("Menu"))
        assertEquals(VUIDTypeCode.MENU, VUIDGenerator.getTypeCode("PopupMenu"))
        assertEquals(VUIDTypeCode.MENU, VUIDGenerator.getTypeCode("ContextMenu"))
        assertEquals(VUIDTypeCode.MENU, VUIDGenerator.getTypeCode("DropdownMenu"))
        assertEquals(VUIDTypeCode.MENU, VUIDGenerator.getTypeCode("NavigationMenu"))
    }

    @Test
    fun `getTypeCode maps dialog classes to DIALOG code`() {
        assertEquals(VUIDTypeCode.DIALOG, VUIDGenerator.getTypeCode("Dialog"))
        assertEquals(VUIDTypeCode.DIALOG, VUIDGenerator.getTypeCode("AlertDialog"))
        assertEquals(VUIDTypeCode.DIALOG, VUIDGenerator.getTypeCode("BottomSheetDialog"))
        assertEquals(VUIDTypeCode.DIALOG, VUIDGenerator.getTypeCode("DialogFragment"))
    }

    @Test
    fun `getTypeCode maps image classes to IMAGE code`() {
        assertEquals(VUIDTypeCode.IMAGE, VUIDGenerator.getTypeCode("ImageView"))
        assertEquals(VUIDTypeCode.IMAGE, VUIDGenerator.getTypeCode("AppCompatImageView"))
        assertEquals(VUIDTypeCode.IMAGE, VUIDGenerator.getTypeCode("Image"))
        assertEquals(VUIDTypeCode.IMAGE, VUIDGenerator.getTypeCode("Icon"))
    }

    @Test
    fun `getTypeCode returns ELEMENT for unknown class names`() {
        assertEquals(VUIDTypeCode.ELEMENT, VUIDGenerator.getTypeCode("CustomWidget"))
        assertEquals(VUIDTypeCode.ELEMENT, VUIDGenerator.getTypeCode("UnknownView"))
        assertEquals(VUIDTypeCode.ELEMENT, VUIDGenerator.getTypeCode("MyCustomComponent"))
    }

    @Test
    fun `getTypeCode handles case insensitivity`() {
        assertEquals(VUIDTypeCode.BUTTON, VUIDGenerator.getTypeCode("button"))
        assertEquals(VUIDTypeCode.BUTTON, VUIDGenerator.getTypeCode("BUTTON"))
        assertEquals(VUIDTypeCode.TEXT, VUIDGenerator.getTypeCode("textview"))
        assertEquals(VUIDTypeCode.TEXT, VUIDGenerator.getTypeCode("TEXTVIEW"))
    }

    @Test
    fun `getTypeCode handles empty and blank strings`() {
        assertEquals(VUIDTypeCode.ELEMENT, VUIDGenerator.getTypeCode(""))
        assertEquals(VUIDTypeCode.ELEMENT, VUIDGenerator.getTypeCode("   "))
    }

    // ==================== VUIDTypeCode Enum Tests ====================

    @Test
    fun `VUIDTypeCode enum has correct codes`() {
        assertEquals('b', VUIDTypeCode.BUTTON.code)
        assertEquals('i', VUIDTypeCode.INPUT.code)
        assertEquals('s', VUIDTypeCode.SCROLL.code)
        assertEquals('t', VUIDTypeCode.TEXT.code)
        assertEquals('e', VUIDTypeCode.ELEMENT.code)
        assertEquals('c', VUIDTypeCode.CARD.code)
        assertEquals('l', VUIDTypeCode.LAYOUT.code)
        assertEquals('m', VUIDTypeCode.MENU.code)
        assertEquals('d', VUIDTypeCode.DIALOG.code)
        assertEquals('g', VUIDTypeCode.IMAGE.code)
    }

    @Test
    fun `VUIDTypeCode fromCode returns correct enum value`() {
        assertEquals(VUIDTypeCode.BUTTON, VUIDTypeCode.fromCode('b'))
        assertEquals(VUIDTypeCode.INPUT, VUIDTypeCode.fromCode('i'))
        assertEquals(VUIDTypeCode.SCROLL, VUIDTypeCode.fromCode('s'))
        assertEquals(VUIDTypeCode.TEXT, VUIDTypeCode.fromCode('t'))
        assertEquals(VUIDTypeCode.ELEMENT, VUIDTypeCode.fromCode('e'))
        assertEquals(VUIDTypeCode.CARD, VUIDTypeCode.fromCode('c'))
        assertEquals(VUIDTypeCode.LAYOUT, VUIDTypeCode.fromCode('l'))
        assertEquals(VUIDTypeCode.MENU, VUIDTypeCode.fromCode('m'))
        assertEquals(VUIDTypeCode.DIALOG, VUIDTypeCode.fromCode('d'))
        assertEquals(VUIDTypeCode.IMAGE, VUIDTypeCode.fromCode('g'))
    }

    @Test
    fun `VUIDTypeCode fromCode returns null for unknown codes`() {
        assertNull(VUIDTypeCode.fromCode('x'))
        assertNull(VUIDTypeCode.fromCode('y'))  // z is now LIST
        assertNull(VUIDTypeCode.fromCode('1'))
    }

    // ==================== VUIDComponents Data Class Tests ====================

    @Test
    fun `VUIDComponents holds correct values`() {
        val components = VUIDComponents(
            packageHash = "a3f2e1",
            typeCode = VUIDTypeCode.BUTTON,
            elementHash = "917cc9dc"
        )

        assertEquals("a3f2e1", components.packageHash)
        assertEquals(VUIDTypeCode.BUTTON, components.typeCode)
        assertEquals("917cc9dc", components.elementHash)
    }

    @Test
    fun `VUIDComponents toVUID reconstructs valid VUID string`() {
        val components = VUIDComponents(
            packageHash = "a3f2e1",
            typeCode = VUIDTypeCode.BUTTON,
            elementHash = "917cc9dc"
        )

        assertEquals("a3f2e1-b917cc9dc", components.toVUID())
    }

    // ==================== Simple Format Tests ====================

    @Test
    fun `generateSimple creates valid simple format VUID`() {
        val vuid = VUIDGenerator.generateSimple(VUIDModule.AVA, VUIDTypeCode.ELEMENT)

        assertTrue(VUIDGenerator.isSimpleFormat(vuid))
        assertTrue(vuid.startsWith("ava:elm:"))
    }

    @Test
    fun `isSimpleFormat validates simple format correctly`() {
        assertTrue(VUIDGenerator.isSimpleFormat("ava:msg:a7f3e2c1"))
        assertTrue(VUIDGenerator.isSimpleFormat("vos:btn:12345678"))
        assertTrue(VUIDGenerator.isSimpleFormat("web:tab:abcdef00"))

        assertFalse(VUIDGenerator.isSimpleFormat("a3f2e1-b917cc9dc")) // Compact
        assertFalse(VUIDGenerator.isSimpleFormat("invalid"))
        assertFalse(VUIDGenerator.isSimpleFormat(""))
    }
}
