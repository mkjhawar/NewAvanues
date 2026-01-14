package com.augmentalis.magicelements.core.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for FlutterIconMapping
 *
 * @since 3.0.0-flutter-parity
 */
class FlutterIconMappingTest {

    @Test
    fun testGetMaterialIconName_CommonIcons() {
        assertEquals("check", FlutterIconMapping.getMaterialIconName("Icons.check"))
        assertEquals("close", FlutterIconMapping.getMaterialIconName("Icons.close"))
        assertEquals("add", FlutterIconMapping.getMaterialIconName("Icons.add"))
        assertEquals("remove", FlutterIconMapping.getMaterialIconName("Icons.remove"))
        assertEquals("delete", FlutterIconMapping.getMaterialIconName("Icons.delete"))
        assertEquals("edit", FlutterIconMapping.getMaterialIconName("Icons.edit"))
    }

    @Test
    fun testGetMaterialIconName_Navigation() {
        assertEquals("arrow_back", FlutterIconMapping.getMaterialIconName("Icons.arrow_back"))
        assertEquals("arrow_forward", FlutterIconMapping.getMaterialIconName("Icons.arrow_forward"))
        assertEquals("menu", FlutterIconMapping.getMaterialIconName("Icons.menu"))
        assertEquals("home", FlutterIconMapping.getMaterialIconName("Icons.home"))
        assertEquals("more_vert", FlutterIconMapping.getMaterialIconName("Icons.more_vert"))
    }

    @Test
    fun testGetMaterialIconName_People() {
        assertEquals("person", FlutterIconMapping.getMaterialIconName("Icons.person"))
        assertEquals("person_add", FlutterIconMapping.getMaterialIconName("Icons.person_add"))
        assertEquals("group", FlutterIconMapping.getMaterialIconName("Icons.people"))
        assertEquals("account_circle", FlutterIconMapping.getMaterialIconName("Icons.account_circle"))
    }

    @Test
    fun testGetMaterialIconName_Settings() {
        assertEquals("settings", FlutterIconMapping.getMaterialIconName("Icons.settings"))
        assertEquals("tune", FlutterIconMapping.getMaterialIconName("Icons.tune"))
        assertEquals("build", FlutterIconMapping.getMaterialIconName("Icons.build"))
    }

    @Test
    fun testGetMaterialIconName_Media() {
        assertEquals("play_arrow", FlutterIconMapping.getMaterialIconName("Icons.play_arrow"))
        assertEquals("pause", FlutterIconMapping.getMaterialIconName("Icons.pause"))
        assertEquals("stop", FlutterIconMapping.getMaterialIconName("Icons.stop"))
        assertEquals("volume_up", FlutterIconMapping.getMaterialIconName("Icons.volume_up"))
        assertEquals("mic", FlutterIconMapping.getMaterialIconName("Icons.mic"))
    }

    @Test
    fun testGetMaterialIconName_Toggle() {
        assertEquals("check_box", FlutterIconMapping.getMaterialIconName("Icons.check_box"))
        assertEquals("radio_button_checked", FlutterIconMapping.getMaterialIconName("Icons.radio_button_checked"))
        assertEquals("star", FlutterIconMapping.getMaterialIconName("Icons.star"))
        assertEquals("favorite", FlutterIconMapping.getMaterialIconName("Icons.favorite"))
    }

    @Test
    fun testGetMaterialIconName_Status() {
        assertEquals("info", FlutterIconMapping.getMaterialIconName("Icons.info"))
        assertEquals("warning", FlutterIconMapping.getMaterialIconName("Icons.warning"))
        assertEquals("error", FlutterIconMapping.getMaterialIconName("Icons.error"))
        assertEquals("help", FlutterIconMapping.getMaterialIconName("Icons.help"))
        assertEquals("notifications", FlutterIconMapping.getMaterialIconName("Icons.notifications"))
    }

    @Test
    fun testGetMaterialIconName_Security() {
        assertEquals("lock", FlutterIconMapping.getMaterialIconName("Icons.lock"))
        assertEquals("lock_open", FlutterIconMapping.getMaterialIconName("Icons.lock_open"))
        assertEquals("visibility", FlutterIconMapping.getMaterialIconName("Icons.visibility"))
        assertEquals("visibility_off", FlutterIconMapping.getMaterialIconName("Icons.visibility_off"))
    }

    @Test
    fun testGetMaterialIconName_WithoutPrefix() {
        assertEquals("check", FlutterIconMapping.getMaterialIconName("check"))
        assertEquals("settings", FlutterIconMapping.getMaterialIconName("settings"))
    }

    @Test
    fun testGetMaterialIconName_Unmapped() {
        val result = FlutterIconMapping.getMaterialIconName("Icons.custom_unknown_icon")
        assertEquals("custom_unknown_icon", result)
    }

    @Test
    fun testIsMapped_CommonIcons() {
        assertTrue(FlutterIconMapping.isMapped("Icons.check"))
        assertTrue(FlutterIconMapping.isMapped("Icons.close"))
        assertTrue(FlutterIconMapping.isMapped("Icons.settings"))
        assertTrue(FlutterIconMapping.isMapped("Icons.person"))
    }

    @Test
    fun testIsMapped_WithoutPrefix() {
        assertTrue(FlutterIconMapping.isMapped("check"))
        assertTrue(FlutterIconMapping.isMapped("close"))
    }

    @Test
    fun testIsMapped_Unmapped() {
        assertFalse(FlutterIconMapping.isMapped("Icons.custom_unknown_icon"))
        assertFalse(FlutterIconMapping.isMapped("completely_random"))
    }

    @Test
    fun testGetAllMappedIcons_NotEmpty() {
        val allIcons = FlutterIconMapping.getAllMappedIcons()
        assertTrue(allIcons.isNotEmpty())
    }

    @Test
    fun testGetAllMappedIcons_ContainsCommonIcons() {
        val allIcons = FlutterIconMapping.getAllMappedIcons()
        assertTrue(allIcons.contains("Icons.check"))
        assertTrue(allIcons.contains("Icons.close"))
        assertTrue(allIcons.contains("Icons.settings"))
        assertTrue(allIcons.contains("Icons.person"))
    }

    @Test
    fun testGetMappingCount_OverTarget() {
        val count = FlutterIconMapping.getMappingCount()
        assertTrue(count >= 300, "Expected at least 300 icons, got $count")
    }

    @Test
    fun testContentCopyAlias() {
        assertEquals("content_copy", FlutterIconMapping.getMaterialIconName("Icons.content_copy"))
        assertEquals("content_copy", FlutterIconMapping.getMaterialIconName("Icons.copy"))
    }

    @Test
    fun testEmailAlias() {
        assertEquals("email", FlutterIconMapping.getMaterialIconName("Icons.email"))
        assertEquals("email", FlutterIconMapping.getMaterialIconName("Icons.mail"))
    }

    @Test
    fun testOutlinedVariants() {
        assertEquals("delete_outline", FlutterIconMapping.getMaterialIconName("Icons.delete_outlined"))
        assertEquals("edit_outline", FlutterIconMapping.getMaterialIconName("Icons.edit_outlined"))
        assertEquals("settings_outlined", FlutterIconMapping.getMaterialIconName("Icons.settings_outlined"))
    }

    @Test
    fun testRoundedVariants() {
        assertEquals("check", FlutterIconMapping.getMaterialIconName("Icons.check_rounded"))
        assertEquals("close", FlutterIconMapping.getMaterialIconName("Icons.close_rounded"))
        assertEquals("add", FlutterIconMapping.getMaterialIconName("Icons.add_rounded"))
    }

    @Test
    fun testTimeAndDate() {
        assertEquals("access_time", FlutterIconMapping.getMaterialIconName("Icons.access_time"))
        assertEquals("alarm", FlutterIconMapping.getMaterialIconName("Icons.alarm"))
        assertEquals("date_range", FlutterIconMapping.getMaterialIconName("Icons.date_range"))
        assertEquals("event", FlutterIconMapping.getMaterialIconName("Icons.event"))
    }

    @Test
    fun testShopping() {
        assertEquals("shopping_cart", FlutterIconMapping.getMaterialIconName("Icons.shopping_cart"))
        assertEquals("payment", FlutterIconMapping.getMaterialIconName("Icons.payment"))
        assertEquals("receipt", FlutterIconMapping.getMaterialIconName("Icons.receipt"))
    }

    @Test
    fun testPlaces() {
        assertEquals("place", FlutterIconMapping.getMaterialIconName("Icons.place"))
        assertEquals("map", FlutterIconMapping.getMaterialIconName("Icons.map"))
        assertEquals("restaurant", FlutterIconMapping.getMaterialIconName("Icons.restaurant"))
    }
}
