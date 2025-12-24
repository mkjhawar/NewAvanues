/**
 * VUIDRepositoryIntegrationTest.kt - Comprehensive UUID repository tests
 *
 * Tests all UUID element, hierarchy, analytics, and alias operations.
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.VUIDElementDTO
import com.augmentalis.database.dto.VUIDHierarchyDTO
import com.augmentalis.database.dto.VUIDAnalyticsDTO
import com.augmentalis.database.dto.VUIDAliasDTO
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class VUIDRepositoryIntegrationTest : BaseRepositoryTest() {

    // ==================== Element CRUD Tests ====================

    @Test
    fun testElementInsertAndGet() = runTest {
        val repo = databaseManager.uuids
        val element = VUIDElementDTO("elem-001", "Submit Button", "BUTTON", "Primary action", null, true, 5, now(), null, null)

        repo.insertElement(element)

        val retrieved = repo.getElementByUuid("elem-001")
        assertNotNull(retrieved)
        assertEquals("Submit Button", retrieved.name)
        assertEquals("BUTTON", retrieved.type)
        assertEquals(5, retrieved.priority)
    }

    @Test
    fun testElementUpdate() = runTest {
        val repo = databaseManager.uuids
        val element = VUIDElementDTO("elem-001", "Original", "BUTTON", null, null, true, 0, now(), null, null)

        repo.insertElement(element)
        repo.updateElement(element.copy(name = "Updated", priority = 10, isEnabled = false))

        val updated = repo.getElementByUuid("elem-001")
        assertEquals("Updated", updated?.name)
        assertEquals(10, updated?.priority)
        assertFalse(updated?.isEnabled ?: true)
    }

    @Test
    fun testElementDelete() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("elem-001", "Test", "BUTTON", null, null, true, 0, now(), null, null))
        assertEquals(1, repo.countElements())

        repo.deleteElement("elem-001")
        assertEquals(0, repo.countElements())
    }

    @Test
    fun testGetElementsByType() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("e1", "Button1", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("e2", "Button2", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("e3", "Text1", "TEXT", null, null, true, 0, now(), null, null))

        val buttons = repo.getElementsByType("BUTTON")
        assertEquals(2, buttons.size)
        assertTrue(buttons.all { it.type == "BUTTON" })
    }

    @Test
    fun testGetChildrenOfParent() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("parent", "Parent", "CONTAINER", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("child1", "Child1", "BUTTON", null, "parent", true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("child2", "Child2", "BUTTON", null, "parent", true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("orphan", "Orphan", "BUTTON", null, null, true, 0, now(), null, null))

        val children = repo.getChildrenOfParent("parent")
        assertEquals(2, children.size)
        assertTrue(children.all { it.parentUuid == "parent" })
    }

    @Test
    fun testGetEnabledElements() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("e1", "Enabled1", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("e2", "Disabled", "BUTTON", null, null, false, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("e3", "Enabled2", "BUTTON", null, null, true, 0, now(), null, null))

        val enabled = repo.getEnabledElements()
        assertEquals(2, enabled.size)
        assertTrue(enabled.all { it.isEnabled })
    }

    @Test
    fun testSearchByName() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("e1", "Submit Button", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("e2", "Cancel Button", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("e3", "Text Field", "TEXT", null, null, true, 0, now(), null, null))

        val buttons = repo.searchByName("%Button%")
        assertEquals(2, buttons.size)
    }

    // ==================== Hierarchy Tests ====================

    @Test
    fun testHierarchyInsertAndGet() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("parent", "Parent", "CONTAINER", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("child", "Child", "BUTTON", null, "parent", true, 0, now(), null, null))

        val hierarchy = VUIDHierarchyDTO(0, "parent", "child", 1, "/parent/child", 0)
        repo.insertHierarchy(hierarchy)

        val retrieved = repo.getHierarchyByParent("parent")
        assertEquals(1, retrieved.size)
        assertEquals("child", retrieved[0].childUuid)
        assertEquals(1, retrieved[0].depth)
    }

    @Test
    fun testDeleteHierarchyByParent() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("parent", "P", "CONTAINER", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("child1", "C1", "BUTTON", null, "parent", true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("child2", "C2", "BUTTON", null, "parent", true, 0, now(), null, null))

        repo.insertHierarchy(VUIDHierarchyDTO(0, "parent", "child1", 1, "/parent/child1", 0))
        repo.insertHierarchy(VUIDHierarchyDTO(0, "parent", "child2", 1, "/parent/child2", 1))

        assertEquals(2, repo.getHierarchyByParent("parent").size)

        repo.deleteHierarchyByParent("parent")

        assertEquals(0, repo.getHierarchyByParent("parent").size)
    }

    // ==================== Analytics Tests ====================

    @Test
    fun testAnalyticsInsertAndGet() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("elem-001", "Test", "BUTTON", null, null, true, 0, now(), null, null))

        val analytics = VUIDAnalyticsDTO("elem-001", 5, past(10000), now(), 150, 4, 1, "ACTIVE")
        repo.insertAnalytics(analytics)

        val retrieved = repo.getAnalyticsByUuid("elem-001")
        assertNotNull(retrieved)
        assertEquals(5, retrieved.accessCount)
        assertEquals(4, retrieved.successCount)
        assertEquals(1, retrieved.failureCount)
    }

    @Test
    fun testIncrementAccessCount() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("elem-001", "Test", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertAnalytics(VUIDAnalyticsDTO("elem-001", 5, now(), now(), 100, 0, 0, "ACTIVE"))

        repo.incrementAccessCount("elem-001", now())

        val retrieved = repo.getAnalyticsByUuid("elem-001")
        assertEquals(6, retrieved?.accessCount)
    }

    @Test
    fun testRecordExecution() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("elem-001", "Test", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertAnalytics(VUIDAnalyticsDTO("elem-001", 0, now(), now(), 0, 3, 1, "ACTIVE"))

        repo.recordExecution("elem-001", 250, true, now())

        val analytics = repo.getAnalyticsByUuid("elem-001")
        assertEquals(4, analytics?.successCount)
        assertEquals(1, analytics?.failureCount)
    }

    @Test
    fun testGetMostAccessed() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("e1", "Low", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("e2", "High", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("e3", "Mid", "BUTTON", null, null, true, 0, now(), null, null))

        repo.insertAnalytics(VUIDAnalyticsDTO("e1", 5, now(), now(), 100, 0, 0, "ACTIVE"))
        repo.insertAnalytics(VUIDAnalyticsDTO("e2", 50, now(), now(), 100, 0, 0, "ACTIVE"))
        repo.insertAnalytics(VUIDAnalyticsDTO("e3", 20, now(), now(), 100, 0, 0, "ACTIVE"))

        val mostAccessed = repo.getMostAccessed(2)
        assertEquals(2, mostAccessed.size)
        assertEquals("e2", mostAccessed[0].uuid)
    }

    // ==================== Alias Tests ====================

    @Test
    fun testAliasInsertAndGet() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("elem-001", "Test", "BUTTON", null, null, true, 0, now(), null, null))

        val alias = VUIDAliasDTO(0, "submit-btn", "elem-001", true, now())
        repo.insertAlias(alias)

        assertTrue(repo.aliasExists("submit-btn"))
        assertEquals("elem-001", repo.getUuidByAlias("submit-btn"))

        val retrieved = repo.getAliasByName("submit-btn")
        assertTrue(retrieved?.isPrimary ?: false)
    }

    @Test
    fun testGetAliasesForUuid() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("elem-001", "Test", "BUTTON", null, null, true, 0, now(), null, null))

        repo.insertAlias(VUIDAliasDTO(0, "alias1", "elem-001", true, now()))
        repo.insertAlias(VUIDAliasDTO(0, "alias2", "elem-001", false, now()))
        repo.insertAlias(VUIDAliasDTO(0, "alias3", "elem-001", false, now()))

        val aliases = repo.getAliasesForUuid("elem-001")
        assertEquals(3, aliases.size)
    }

    @Test
    fun testDeleteAliases() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("elem-001", "Test", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertAlias(VUIDAliasDTO(0, "to-delete", "elem-001", false, now()))

        assertTrue(repo.aliasExists("to-delete"))

        repo.deleteAliasByName("to-delete")

        assertFalse(repo.aliasExists("to-delete"))
    }

    // ==================== Bulk Operations Tests ====================

    @Test
    fun testDeleteAllElements() = runTest {
        val repo = databaseManager.uuids

        repo.insertElement(VUIDElementDTO("e1", "E1", "BUTTON", null, null, true, 0, now(), null, null))
        repo.insertElement(VUIDElementDTO("e2", "E2", "BUTTON", null, null, true, 0, now(), null, null))

        repo.insertHierarchy(VUIDHierarchyDTO(0, "e1", "e2", 1, "/e1/e2", 0))
        repo.insertAnalytics(VUIDAnalyticsDTO("e1", 0, now(), now(), 0, 0, 0, "ACTIVE"))
        repo.insertAlias(VUIDAliasDTO(0, "alias1", "e1", false, now()))

        assertEquals(2, repo.countElements())

        repo.deleteAllElements()

        assertEquals(0, repo.countElements())
        assertEquals(0, repo.getAllHierarchy().size)
        assertEquals(0, repo.getAllAnalytics().size)
        assertEquals(0, repo.getAllAliases().size)
    }
}
