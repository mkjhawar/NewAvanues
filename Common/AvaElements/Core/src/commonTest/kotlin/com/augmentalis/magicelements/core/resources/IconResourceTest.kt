package com.augmentalis.magicelements.core.resources

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Tests for IconResource and related classes
 *
 * @since 3.0.0-flutter-parity
 */
class IconResourceTest {

    @Test
    fun testMaterialIconCreation() {
        val icon = IconResource.MaterialIcon("check")
        assertEquals("check", icon.name)
        assertEquals(IconResource.IconVariant.Filled, icon.variant)
    }

    @Test
    fun testMaterialIconWithVariant() {
        val icon = IconResource.MaterialIcon("settings", IconResource.IconVariant.Outlined)
        assertEquals("settings", icon.name)
        assertEquals(IconResource.IconVariant.Outlined, icon.variant)
    }

    @Test
    fun testFromFlutterIcon_Filled() {
        val icon = IconResource.fromFlutterIcon("Icons.check")
        assertTrue(icon is IconResource.MaterialIcon)
        assertEquals("check", icon.name)
        assertEquals(IconResource.IconVariant.Filled, icon.variant)
    }

    @Test
    fun testFromFlutterIcon_Outlined() {
        val icon = IconResource.fromFlutterIcon("Icons.settings_outlined")
        assertTrue(icon is IconResource.MaterialIcon)
        assertEquals("settings", icon.name)
        assertEquals(IconResource.IconVariant.Outlined, icon.variant)
    }

    @Test
    fun testFromFlutterIcon_Rounded() {
        val icon = IconResource.fromFlutterIcon("Icons.home_rounded")
        assertTrue(icon is IconResource.MaterialIcon)
        assertEquals("home", icon.name)
        assertEquals(IconResource.IconVariant.Rounded, icon.variant)
    }

    @Test
    fun testFromFlutterIcon_Sharp() {
        val icon = IconResource.fromFlutterIcon("Icons.star_sharp")
        assertTrue(icon is IconResource.MaterialIcon)
        assertEquals("star", icon.name)
        assertEquals(IconResource.IconVariant.Sharp, icon.variant)
    }

    @Test
    fun testFromString_MaterialIcon() {
        val icon = IconResource.fromString("check")
        assertTrue(icon is IconResource.MaterialIcon)
        assertEquals("check", (icon as IconResource.MaterialIcon).name)
    }

    @Test
    fun testFromString_NetworkImage() {
        val icon = IconResource.fromString("https://example.com/icon.png")
        assertTrue(icon is IconResource.NetworkImage)
        assertEquals("https://example.com/icon.png", (icon as IconResource.NetworkImage).url)
    }

    @Test
    fun testFromString_RasterImage() {
        val icon = IconResource.fromString("icon.png")
        assertTrue(icon is IconResource.RasterImage)
        assertEquals("icon.png", (icon as IconResource.RasterImage).resourceId)
    }

    @Test
    fun testFromString_Base64Image() {
        val icon = IconResource.fromString("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA")
        assertTrue(icon is IconResource.Base64Image)
        assertEquals("image/png", (icon as IconResource.Base64Image).mimeType)
    }

    @Test
    fun testNetworkImageWithPlaceholder() {
        val placeholder = IconResource.MaterialIcon("hourglass_empty")
        val icon = IconResource.NetworkImage(
            url = "https://example.com/icon.png",
            placeholder = placeholder
        )
        assertEquals("https://example.com/icon.png", icon.url)
        assertNotNull(icon.placeholder)
        assertEquals(placeholder, icon.placeholder)
    }

    @Test
    fun testNetworkImageWithErrorIcon() {
        val errorIcon = IconResource.MaterialIcon("error")
        val icon = IconResource.NetworkImage(
            url = "https://example.com/icon.png",
            errorIcon = errorIcon
        )
        assertEquals("https://example.com/icon.png", icon.url)
        assertNotNull(icon.errorIcon)
        assertEquals(errorIcon, icon.errorIcon)
    }

    @Test
    fun testIconSizePresets() {
        assertEquals(18f, IconSize.Small.dp)
        assertEquals(24f, IconSize.Standard.dp)
        assertEquals(36f, IconSize.Large.dp)
        assertEquals(48f, IconSize.ExtraLarge.dp)
    }

    @Test
    fun testIconSizeFromDp_Exact() {
        assertEquals(IconSize.Small, IconSize.fromDp(18f))
        assertEquals(IconSize.Standard, IconSize.fromDp(24f))
        assertEquals(IconSize.Large, IconSize.fromDp(36f))
        assertEquals(IconSize.ExtraLarge, IconSize.fromDp(48f))
    }

    @Test
    fun testIconSizeFromDp_Closest() {
        assertEquals(IconSize.Small, IconSize.fromDp(20f))
        assertEquals(IconSize.Standard, IconSize.fromDp(28f))
        assertEquals(IconSize.Large, IconSize.fromDp(40f))
        assertEquals(IconSize.ExtraLarge, IconSize.fromDp(50f))
    }

    @Test
    fun testVectorDrawableCreation() {
        val icon = IconResource.VectorDrawable("ic_custom_icon")
        assertEquals("ic_custom_icon", icon.resourceId)
    }

    @Test
    fun testBase64ImageCreation() {
        val icon = IconResource.Base64Image(
            data = "iVBORw0KGgoAAAANSUhEUgAAAAUA",
            mimeType = "image/png"
        )
        assertEquals("iVBORw0KGgoAAAANSUhEUgAAAAUA", icon.data)
        assertEquals("image/png", icon.mimeType)
    }
}
