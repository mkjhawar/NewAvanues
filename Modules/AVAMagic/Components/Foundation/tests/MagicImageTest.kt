package net.ideahq.ideamagic.components.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MagicImage Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicImageTest {

    @Test
    fun `image creation with default values`() {
        val image = MagicImage(
            source = "image.png"
        )

        assertEquals("image.png", image.source)
        assertEquals(null, image.alt)
        assertEquals(ImageFit.CONTAIN, image.fit)
        assertEquals(null, image.width)
        assertEquals(null, image.height)
        assertEquals(null, image.onClick)
    }

    @Test
    fun `image with all properties`() {
        val image = MagicImage(
            source = "avatar.jpg",
            alt = "User Avatar",
            fit = ImageFit.COVER,
            width = 100,
            height = 100,
            onClick = {}
        )

        assertEquals("avatar.jpg", image.source)
        assertEquals("User Avatar", image.alt)
        assertEquals(ImageFit.COVER, image.fit)
        assertEquals(100, image.width)
        assertEquals(100, image.height)
        assertTrue(image.onClick != null)
    }

    @Test
    fun `image fit modes`() {
        val contain = MagicImage("img.png", fit = ImageFit.CONTAIN)
        val cover = MagicImage("img.png", fit = ImageFit.COVER)
        val fill = MagicImage("img.png", fit = ImageFit.FILL)
        val none = MagicImage("img.png", fit = ImageFit.NONE)

        assertEquals(ImageFit.CONTAIN, contain.fit)
        assertEquals(ImageFit.COVER, cover.fit)
        assertEquals(ImageFit.FILL, fill.fit)
        assertEquals(ImageFit.NONE, none.fit)
    }

    @Test
    fun `image with alt text`() {
        val image = MagicImage(
            source = "logo.png",
            alt = "Company Logo"
        )

        assertEquals("Company Logo", image.alt)
    }

    @Test
    fun `image with dimensions`() {
        val image = MagicImage(
            source = "photo.jpg",
            width = 200,
            height = 150
        )

        assertEquals(200, image.width)
        assertEquals(150, image.height)
    }

    @Test
    fun `square image`() {
        val image = MagicImage(
            source = "avatar.png",
            width = 64,
            height = 64
        )

        assertEquals(64, image.width)
        assertEquals(64, image.height)
    }

    @Test
    fun `clickable image`() {
        var clicked = false
        val image = MagicImage(
            source = "thumbnail.jpg",
            onClick = { clicked = true }
        )

        image.onClick?.invoke()
        assertTrue(clicked)
    }

    @Test
    fun `avatar use case`() {
        val image = MagicImage(
            source = "user-avatar.png",
            alt = "John Doe",
            fit = ImageFit.COVER,
            width = 48,
            height = 48
        )

        assertEquals(ImageFit.COVER, image.fit)
        assertEquals(48, image.width)
        assertEquals(48, image.height)
    }

    @Test
    fun `hero image use case`() {
        val image = MagicImage(
            source = "hero-banner.jpg",
            alt = "Welcome Banner",
            fit = ImageFit.COVER,
            width = 1200,
            height = 400
        )

        assertEquals(ImageFit.COVER, image.fit)
        assertEquals(1200, image.width)
        assertEquals(400, image.height)
    }

    @Test
    fun `thumbnail gallery use case`() {
        var selected = false
        val image = MagicImage(
            source = "thumbnail-1.jpg",
            fit = ImageFit.COVER,
            width = 120,
            height = 120,
            onClick = { selected = true }
        )

        assertTrue(image.onClick != null)
        image.onClick?.invoke()
        assertTrue(selected)
    }
}
