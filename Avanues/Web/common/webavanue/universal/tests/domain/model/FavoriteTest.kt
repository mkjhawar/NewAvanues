package com.augmentalis.webavanue.domain.model

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FavoriteTest {

    @Test
    fun `create favorite with default values`() {
        val url = "https://www.example.com"
        val title = "Example Site"
        val favorite = Favorite.create(url, title)

        assertEquals(url, favorite.url)
        assertEquals(title, favorite.title)
        assertNotNull(favorite.id)
        assertNotNull(favorite.createdAt)
        assertNotNull(favorite.lastModifiedAt)
        assertEquals(0, favorite.visitCount)
        assertEquals(0, favorite.position)
        assertTrue(favorite.tags.isEmpty())
    }

    @Test
    fun `create favorite in folder`() {
        val folderId = "folder_123"
        val favorite = Favorite.create(
            url = "https://www.example.com",
            title = "Example",
            folderId = folderId
        )

        assertEquals(folderId, favorite.folderId)
    }

    @Test
    fun `favorite can have tags`() {
        val tags = listOf("news", "technology", "favorite")
        val favorite = Favorite(
            id = "fav_123",
            url = "https://www.tech.com",
            title = "Tech News",
            tags = tags,
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now()
        )

        assertEquals(3, favorite.tags.size)
        assertTrue(favorite.tags.contains("technology"))
    }

    @Test
    fun `favorite can have description`() {
        val description = "My favorite tech news site"
        val favorite = Favorite(
            id = "fav_123",
            url = "https://www.tech.com",
            title = "Tech News",
            description = description,
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now()
        )

        assertEquals(description, favorite.description)
    }

    @Test
    fun `favorite id is unique`() {
        val fav1 = Favorite.create("https://www.example.com", "Example")
        val fav2 = Favorite.create("https://www.example.com", "Example")

        assertTrue(fav1.id != fav2.id)
    }

    @Test
    fun `root folder id constant`() {
        assertEquals("root", Favorite.ROOT_FOLDER_ID)
    }

    @Test
    fun `max title length constant`() {
        assertEquals(255, Favorite.MAX_TITLE_LENGTH)
    }

    @Test
    fun `create favorite folder`() {
        val folderName = "News Sites"
        val folder = FavoriteFolder.create(folderName)

        assertEquals(folderName, folder.name)
        assertNotNull(folder.id)
        assertNotNull(folder.createdAt)
        assertEquals(0, folder.position)
    }

    @Test
    fun `create nested folder`() {
        val parentFolder = FavoriteFolder.create("Parent")
        val childFolder = FavoriteFolder.create("Child", parentId = parentFolder.id)

        assertEquals(parentFolder.id, childFolder.parentId)
    }

    @Test
    fun `folder can have custom color and icon`() {
        val folder = FavoriteFolder(
            id = "folder_123",
            name = "Important",
            color = "#FF0000",
            icon = "star",
            createdAt = Clock.System.now(),
            position = 0
        )

        assertEquals("#FF0000", folder.color)
        assertEquals("star", folder.icon)
    }
}