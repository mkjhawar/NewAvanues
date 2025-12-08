/**
 * AliasSanitizationTest.kt - Unit tests for alias sanitization logic
 * Path: libraries/UUIDCreator/src/test/java/com/augmentalis/uuidcreator/database/AliasSanitizationTest.kt
 *
 * Author: VOS4 Test Specialist (Claude Code)
 * Created: 2025-10-23
 *
 * Unit tests for UUIDRepository.sanitizeAlias() method
 */

package com.augmentalis.uuidcreator.database

import com.augmentalis.uuidcreator.database.dao.UUIDAliasDao
import com.augmentalis.uuidcreator.database.dao.UUIDAnalyticsDao
import com.augmentalis.uuidcreator.database.dao.UUIDElementDao
import com.augmentalis.uuidcreator.database.dao.UUIDHierarchyDao
import com.augmentalis.uuidcreator.database.repository.UUIDRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for alias sanitization
 *
 * Tests the sanitization logic that converts human-readable names
 * into valid database alias strings.
 *
 * ## Test Coverage:
 * - Normal names with spaces
 * - Special characters
 * - Multiple spaces/underscores
 * - Leading/trailing underscores
 * - Empty strings
 * - Unicode characters
 * - Very long names (>50 chars)
 * - Very short names (<3 chars)
 */
@RunWith(MockitoJUnitRunner::class)
class AliasSanitizationTest {

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

    // ==================== Normal Cases ====================

    @Test
    fun sanitizeAlias_NormalNameWithSpaces_ReturnsLowercaseWithUnderscores() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Instagram Like Button")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "instagram_like_button" &&
            alias.uuid == element.uuid &&
            alias.isPrimary
        })
    }

    @Test
    fun sanitizeAlias_SingleWord_ReturnsLowercase() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Submit")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "submit" &&
            alias.isPrimary
        })
    }

    @Test
    fun sanitizeAlias_MixedCase_ReturnsLowercase() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "MyButtonName")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "mybuttonname" &&
            alias.isPrimary
        })
    }

    // ==================== Special Characters ====================

    @Test
    fun sanitizeAlias_SpecialCharacters_ReplacedWithUnderscores() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Hello@World#123")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "hello_world_123"
        })
    }

    @Test
    fun sanitizeAlias_HyphensAndDots_ReplacedWithUnderscores() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Menu-Item.1")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "menu_item_1"
        })
    }

    @Test
    fun sanitizeAlias_OnlySpecialCharacters_ReturnsEmpty() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "@#$%^&*()")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - No alias should be created for invalid name
        verify(mockAliasDao, never()).insert(any())
    }

    // ==================== Multiple Spaces/Underscores ====================

    @Test
    fun sanitizeAlias_MultipleSpaces_ReplacedWithSingleUnderscore() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Test   Name   Here")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "test_name_here"
        })
    }

    @Test
    fun sanitizeAlias_ConsecutiveUnderscores_Collapsed() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Test___Name___Here")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "test_name_here"
        })
    }

    @Test
    fun sanitizeAlias_MixedSpacesAndSpecialChars_CollapsedToSingleUnderscore() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Test  @#  Name")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "test_name"
        })
    }

    // ==================== Leading/Trailing Underscores ====================

    @Test
    fun sanitizeAlias_LeadingUnderscore_Removed() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "_button")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "button"
        })
    }

    @Test
    fun sanitizeAlias_TrailingUnderscore_Removed() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "button_")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "button"
        })
    }

    @Test
    fun sanitizeAlias_LeadingAndTrailingUnderscores_BothRemoved() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "__button__")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "button"
        })
    }

    @Test
    fun sanitizeAlias_LeadingSpace_ConvertedAndRemoved() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "  button")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "button"
        })
    }

    @Test
    fun sanitizeAlias_TrailingSpace_ConvertedAndRemoved() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "button  ")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "button"
        })
    }

    // ==================== Empty/Null Cases ====================

    @Test
    fun sanitizeAlias_EmptyString_NoAliasCreated() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao, never()).insert(any())
    }

    @Test
    fun sanitizeAlias_NullName_NoAliasCreated() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = null)
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao, never()).insert(any())
    }

    @Test
    fun sanitizeAlias_OnlySpaces_NoAliasCreated() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "     ")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao, never()).insert(any())
    }

    // ==================== Length Constraints ====================

    @Test
    fun sanitizeAlias_VeryLongName_TruncatedTo50Chars() = runTest(testDispatcher) {
        // Arrange
        val longName = "a".repeat(100) // 100 characters
        val element = createTestElement(name = longName)
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias.length == 50 &&
            alias.alias == "a".repeat(50)
        })
    }

    @Test
    fun sanitizeAlias_51Characters_TruncatedTo50() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "this_is_a_very_long_button_name_that_exceeds_fifty_c")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias.length == 50 &&
            alias.alias == "this_is_a_very_long_button_name_that_exceeds_fi"
        })
    }

    @Test
    fun sanitizeAlias_Exactly3Characters_Accepted() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "btn")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "btn"
        })
    }

    @Test
    fun sanitizeAlias_TwoCharacters_NoAliasCreated() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "ab")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao, never()).insert(any())
    }

    @Test
    fun sanitizeAlias_OneCharacter_NoAliasCreated() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "a")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao, never()).insert(any())
    }

    // ==================== Unicode/International Characters ====================

    @Test
    fun sanitizeAlias_UnicodeCharacters_RemovedOrReplaced() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Button™ Ñame")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            // Unicode characters should be replaced with underscores
            alias.alias == "button_ame" || alias.alias == "button_name"
        })
    }

    @Test
    fun sanitizeAlias_EmojiCharacters_Removed() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Like 👍 Button")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "like_button"
        })
    }

    // ==================== Edge Cases ====================

    @Test
    fun sanitizeAlias_NumbersOnly_Accepted() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "123456")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "123456"
        })
    }

    @Test
    fun sanitizeAlias_MixedAlphanumeric_Accepted() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "Button123")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "button123"
        })
    }

    @Test
    fun sanitizeAlias_ComplexRealWorldExample_SanitizedCorrectly() = runTest(testDispatcher) {
        // Arrange
        val element = createTestElement(name = "  Submit   Form (2024) - Main  ")
        `when`(mockAliasDao.exists(anyString())).thenReturn(false)

        // Act
        repository.insert(element)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockAliasDao).insert(argThat { alias ->
            alias.alias == "submit_form_2024_main"
        })
    }

    // ==================== Helper Methods ====================

    private fun createTestElement(
        uuid: String = "test-uuid-${System.nanoTime()}",
        name: String?,
        type: String = "button"
    ) = com.augmentalis.uuidcreator.models.UUIDElement(
        uuid = uuid,
        name = name,
        type = type,
        isEnabled = true
    )
}
