/**
 * UUIDRepositoryAliasTest.kt - Unit tests for UUIDRepository alias methods
 * Path: libraries/UUIDCreator/src/test/java/com/augmentalis/uuidcreator/database/UUIDRepositoryAliasTest.kt
 *
 * Author: VOS4 Test Specialist (Claude Code)
 * Created: 2025-10-23
 *
 * Unit tests for alias CRUD operations in UUIDRepository
 */

package com.augmentalis.uuidcreator.database

import com.augmentalis.uuidcreator.database.dao.UUIDAliasDao
import com.augmentalis.uuidcreator.database.dao.UUIDAnalyticsDao
import com.augmentalis.uuidcreator.database.dao.UUIDElementDao
import com.augmentalis.uuidcreator.database.dao.UUIDHierarchyDao
import com.augmentalis.uuidcreator.database.entities.UUIDAliasEntity
import com.augmentalis.uuidcreator.database.repository.UUIDRepository
import com.augmentalis.uuidcreator.models.UUIDElement
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for repository alias operations
 *
 * Tests all alias-related methods in UUIDRepository including:
 * - Manual alias registration
 * - Alias unregistration
 * - Alias existence checks
 * - Alias-to-UUID lookups
 * - UUID-to-aliases lookups
 * - Alias index synchronization
 */
@RunWith(MockitoJUnitRunner::class)
class UUIDRepositoryAliasTest {

    @Mock
    private lateinit var mockElementDao: UUIDElementDao

    @Mock
    private lateinit var mockHierarchyDao: UUIDHierarchyDao

    @Mock
    private lateinit var mockAnalyticsDao: UUIDAnalyticsDao

    @Mock
    private lateinit var mockAliasDao: UUIDAliasDao

    private lateinit var repository: UUIDRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        repository = UUIDRepository(
            elementDao = mockElementDao,
            hierarchyDao = mockHierarchyDao,
            analyticsDao = mockAnalyticsDao,
            aliasDao = mockAliasDao,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        // Cleanup if needed
    }

    // ==================== Register Alias ====================

    @Test
    fun registerAlias_ValidAlias_ReturnsTrue() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val alias = "test_button"
        val element = createTestElement(uuid = uuid)

        // Populate cache
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        `when`(mockAliasDao.exists(alias)).thenReturn(false)

        // Act
        val result = repository.registerAlias(uuid, alias, isPrimary = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertTrue(result)
        verify(mockAliasDao).insert(argThat { aliasEntity ->
            aliasEntity.alias == alias &&
            aliasEntity.uuid == uuid &&
            !aliasEntity.isPrimary
        })
    }

    @Test
    fun registerAlias_AsPrimary_CreatesWithPrimaryFlag() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val alias = "primary_button"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        `when`(mockAliasDao.exists(alias)).thenReturn(false)

        // Act
        val result = repository.registerAlias(uuid, alias, isPrimary = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertTrue(result)
        verify(mockAliasDao).insert(argThat { aliasEntity ->
            aliasEntity.isPrimary
        })
    }

    @Test
    fun registerAlias_AliasSanitized_SanitizedVersionStored() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val alias = "Test Button Name"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        `when`(mockAliasDao.exists("test_button_name")).thenReturn(false)

        // Act
        val result = repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertTrue(result)
        verify(mockAliasDao).insert(argThat { aliasEntity ->
            aliasEntity.alias == "test_button_name"
        })
    }

    @Test
    fun registerAlias_UuidNotFound_ReturnsFalse() = runTest(testDispatcher) {
        // Arrange
        val uuid = "non-existent-uuid"
        val alias = "test_alias"

        `when`(mockAliasDao.exists(alias)).thenReturn(false)

        // Act
        val result = repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertFalse(result)
        verify(mockAliasDao, never()).insert(any())
    }

    @Test
    fun registerAlias_AliasAlreadyExists_ReturnsFalse() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val alias = "existing_alias"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        `when`(mockAliasDao.exists(alias)).thenReturn(true)

        // Act
        val result = repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertFalse(result)
        verify(mockAliasDao, never()).insert(any())
    }

    @Test
    fun registerAlias_EmptyAlias_ReturnsFalse() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val alias = ""
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        val result = repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertFalse(result)
        verify(mockAliasDao, never()).insert(any())
    }

    @Test
    fun registerAlias_InvalidCharactersOnly_ReturnsFalse() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val alias = "@#$%"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        val result = repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertFalse(result)
        verify(mockAliasDao, never()).insert(any())
    }

    @Test
    fun registerAlias_TooShortAfterSanitization_ReturnsFalse() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val alias = "ab" // Less than 3 characters
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        val result = repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertFalse(result)
        verify(mockAliasDao, never()).insert(any())
    }

    @Test
    fun registerAlias_DaoThrowsException_ReturnsFalse() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val alias = "test_alias"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        `when`(mockAliasDao.exists(alias)).thenReturn(false)
        `when`(mockAliasDao.insert(any())).thenThrow(RuntimeException("Database error"))

        // Act
        val result = repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertFalse(result)
    }

    // ==================== Unregister Alias ====================

    @Test
    fun unregisterAlias_ExistingAlias_ReturnsTrue() = runTest(testDispatcher) {
        // Arrange
        val alias = "test_button"

        // Mock cache state (alias exists in index)
        val uuid = "test-uuid-123"
        val element = createTestElement(uuid = uuid)
        repository.insert(element)

        // Manually add to index (simulating existing alias)
        repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        reset(mockAliasDao) // Reset after setup

        // Act
        val result = repository.unregisterAlias(alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertTrue(result)
        verify(mockAliasDao).deleteByAlias(alias)
    }

    @Test
    fun unregisterAlias_NonExistentAlias_ReturnsFalse() = runTest(testDispatcher) {
        // Arrange
        val alias = "non_existent_alias"

        // Act
        val result = repository.unregisterAlias(alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertFalse(result)
        verify(mockAliasDao, never()).deleteByAlias(anyString())
    }

    @Test
    fun unregisterAlias_DaoThrowsException_ReturnsFalse() = runTest(testDispatcher) {
        // Arrange
        val alias = "test_button"
        val uuid = "test-uuid-123"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        reset(mockAliasDao)
        `when`(mockAliasDao.deleteByAlias(alias)).thenThrow(RuntimeException("Database error"))

        // Act
        val result = repository.unregisterAlias(alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertFalse(result)
    }

    // ==================== Alias Exists ====================

    @Test
    fun aliasExists_ExistingAlias_ReturnsTrue() = runTest(testDispatcher) {
        // Arrange
        val alias = "test_button"
        val uuid = "test-uuid-123"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        `when`(mockAliasDao.exists(alias)).thenReturn(false)
        repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        val result = repository.aliasExists(alias)

        // Assert
        Assert.assertTrue(result)
    }

    @Test
    fun aliasExists_NonExistentAlias_ReturnsFalse() = runTest(testDispatcher) {
        // Arrange
        val alias = "non_existent_alias"

        // Act
        val result = repository.aliasExists(alias)

        // Assert
        Assert.assertFalse(result)
    }

    // ==================== Get By Alias ====================

    @Test
    fun getByAlias_ExistingAlias_ReturnsElement() = runTest(testDispatcher) {
        // Arrange
        val alias = "test_button"
        val uuid = "test-uuid-123"
        val element = createTestElement(uuid = uuid, name = "Test Button")

        repository.insert(element)
        `when`(mockAliasDao.exists(alias)).thenReturn(false)
        repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        val result = repository.getByAlias(alias)

        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(uuid, result?.uuid)
        Assert.assertEquals("Test Button", result?.name)
    }

    @Test
    fun getByAlias_NonExistentAlias_ReturnsNull() = runTest(testDispatcher) {
        // Arrange
        val alias = "non_existent_alias"

        // Act
        val result = repository.getByAlias(alias)

        // Assert
        Assert.assertNull(result)
    }

    @Test
    fun getByAlias_AliasExistsButElementDeleted_ReturnsNull() = runTest(testDispatcher) {
        // Arrange
        val alias = "test_button"
        val uuid = "test-uuid-123"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        `when`(mockAliasDao.exists(alias)).thenReturn(false)
        repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Delete element
        repository.deleteByUuid(uuid)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        val result = repository.getByAlias(alias)

        // Assert
        Assert.assertNull(result)
    }

    // ==================== Get UUID By Alias ====================

    @Test
    fun getUuidByAlias_ExistingAlias_ReturnsUuid() = runTest(testDispatcher) {
        // Arrange
        val alias = "test_button"
        val uuid = "test-uuid-123"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        `when`(mockAliasDao.exists(alias)).thenReturn(false)
        repository.registerAlias(uuid, alias)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        val result = repository.getUuidByAlias(alias)

        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(uuid, result)
    }

    @Test
    fun getUuidByAlias_NonExistentAlias_ReturnsNull() = runTest(testDispatcher) {
        // Arrange
        val alias = "non_existent_alias"

        // Act
        val result = repository.getUuidByAlias(alias)

        // Assert
        Assert.assertNull(result)
    }

    // ==================== Get Aliases By UUID ====================

    @Test
    fun getAliasesByUuid_ExistingUuid_ReturnsAliasesList() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val aliases = listOf(
            UUIDAliasEntity(1, "test_button", uuid, true, System.currentTimeMillis()),
            UUIDAliasEntity(2, "submit_btn", uuid, false, System.currentTimeMillis())
        )

        `when`(mockAliasDao.getAliasesByUuid(uuid)).thenReturn(aliases)

        // Act
        val result = repository.getAliasesByUuid(uuid)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertEquals(2, result.size)
        Assert.assertTrue(result.any { it.alias == "test_button" && it.isPrimary })
        Assert.assertTrue(result.any { it.alias == "submit_btn" && !it.isPrimary })
    }

    @Test
    fun getAliasesByUuid_NonExistentUuid_ReturnsEmptyList() = runTest(testDispatcher) {
        // Arrange
        val uuid = "non-existent-uuid"
        `when`(mockAliasDao.getAliasesByUuid(uuid)).thenReturn(emptyList())

        // Act
        val result = repository.getAliasesByUuid(uuid)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun getAliasesByUuid_MultipleAliases_ReturnedInCorrectOrder() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val time1 = System.currentTimeMillis()
        val time2 = time1 + 1000
        val aliases = listOf(
            UUIDAliasEntity(1, "primary_alias", uuid, true, time1),
            UUIDAliasEntity(2, "secondary_alias", uuid, false, time2)
        )

        `when`(mockAliasDao.getAliasesByUuid(uuid)).thenReturn(aliases)

        // Act
        val result = repository.getAliasesByUuid(uuid)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertEquals(2, result.size)
        // Primary should be first (as per DAO ordering)
        Assert.assertEquals("primary_alias", result[0].alias)
        Assert.assertTrue(result[0].isPrimary)
    }

    // ==================== Multiple Aliases for Same UUID ====================

    @Test
    fun registerAlias_MultipleAliasesForSameUuid_AllRegistered() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        `when`(mockAliasDao.exists("alias1")).thenReturn(false)
        `when`(mockAliasDao.exists("alias2")).thenReturn(false)
        `when`(mockAliasDao.exists("alias3")).thenReturn(false)

        // Act
        val result1 = repository.registerAlias(uuid, "alias1")
        val result2 = repository.registerAlias(uuid, "alias2")
        val result3 = repository.registerAlias(uuid, "alias3")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        Assert.assertTrue(result1)
        Assert.assertTrue(result2)
        Assert.assertTrue(result3)
        verify(mockAliasDao, times(3)).insert(any())
    }

    @Test
    fun getByAlias_MultipleAliases_EachReturnsCorrectElement() = runTest(testDispatcher) {
        // Arrange
        val uuid = "test-uuid-123"
        val element = createTestElement(uuid = uuid)

        repository.insert(element)
        `when`(mockAliasDao.exists("alias1")).thenReturn(false)
        `when`(mockAliasDao.exists("alias2")).thenReturn(false)
        repository.registerAlias(uuid, "alias1")
        repository.registerAlias(uuid, "alias2")
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        val result1 = repository.getByAlias("alias1")
        val result2 = repository.getByAlias("alias2")

        // Assert
        Assert.assertNotNull(result1)
        Assert.assertNotNull(result2)
        Assert.assertEquals(uuid, result1?.uuid)
        Assert.assertEquals(uuid, result2?.uuid)
    }

    // ==================== Helper Methods ====================

    private fun createTestElement(
        uuid: String = "test-uuid-${System.nanoTime()}",
        name: String = "Test Element",
        type: String = "button"
    ) = UUIDElement(
        uuid = uuid,
        name = name,
        type = type,
        isEnabled = true
    )
}
