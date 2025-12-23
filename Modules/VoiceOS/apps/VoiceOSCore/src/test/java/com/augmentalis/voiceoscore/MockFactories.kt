/**
 * MockFactories.kt - Mock object factories for VoiceOS tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Database Test Coverage Agent - Sprint 1
 * Created: 2025-12-23
 *
 * Provides factory methods for creating mock objects used in tests.
 */

package com.augmentalis.voiceoscore

import android.content.Context
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.*
import com.augmentalis.database.repositories.*
import com.augmentalis.voiceoscore.learnapp.models.AppEntity
import com.augmentalis.voiceoscore.scraping.entities.*
import io.mockk.every
import io.mockk.mockk

/**
 * Factory object for creating mock instances used in tests.
 *
 * ## Provided Mocks:
 * - VoiceOSDatabaseManager with all repositories
 * - Android Context
 * - DTOs (ScrapedAppDTO, ScrapedElementDTO, etc.)
 * - Entities (AppEntity, ScrapedElementEntity, etc.)
 *
 * ## Usage:
 * ```kotlin
 * @Test
 * fun `my test`() {
 *     val mockDatabase = MockFactories.createMockDatabase()
 *     val mockContext = MockFactories.createMockContext()
 *     // Use mocks in test
 * }
 * ```
 */
object MockFactories {

    /**
     * Create a fully mocked VoiceOSDatabaseManager.
     *
     * All repositories are mocked with relaxed = true, allowing
     * any method calls without explicit stubbing.
     *
     * @return Mocked database manager
     */
    fun createMockDatabase(): VoiceOSDatabaseManager {
        return mockk(relaxed = true) {
            every { scrapedApps } returns mockk(relaxed = true)
            every { scrapedElements } returns mockk(relaxed = true)
            every { generatedCommands } returns mockk(relaxed = true)
            every { screenContexts } returns mockk(relaxed = true)
            every { scrapedHierarchies } returns mockk(relaxed = true)
            every { elementRelationships } returns mockk(relaxed = true)
            every { screenTransitions } returns mockk(relaxed = true)
            every { userInteractions } returns mockk(relaxed = true)
            every { elementStateHistory } returns mockk(relaxed = true)
            every { transaction(any()) } answers {
                val block = firstArg<() -> Unit>()
                block()
            }
        }
    }

    /**
     * Create a mocked Android Context.
     *
     * @param packageName Package name to return (default: test package)
     * @return Mocked context
     */
    fun createMockContext(packageName: String = "com.augmentalis.voiceoscore.test"): Context {
        return mockk(relaxed = true) {
            every { this@mockk.packageName } returns packageName
            every { applicationContext } returns this@mockk
        }
    }

    /**
     * Create a sample ScrapedAppDTO for testing.
     *
     * @param packageName Package name (default: "com.example.test")
     * @param appId App ID (default: packageName)
     * @param isFullyLearned Whether app is fully learned (default: false)
     * @return Sample DTO
     */
    fun createScrapedAppDTO(
        packageName: String = "com.example.test",
        appId: String = packageName,
        isFullyLearned: Boolean = false
    ): ScrapedAppDTO {
        val timestamp = System.currentTimeMillis()
        return ScrapedAppDTO(
            appId = appId,
            packageName = packageName,
            versionCode = 1L,
            versionName = "1.0.0",
            appHash = packageName.hashCode().toString(),
            isFullyLearned = if (isFullyLearned) 1L else 0L,
            learnCompletedAt = if (isFullyLearned) timestamp else null,
            scrapingMode = "STATIC",
            scrapeCount = 0L,
            elementCount = 0L,
            commandCount = 0L,
            firstScrapedAt = timestamp,
            lastScrapedAt = timestamp
        )
    }

    /**
     * Create a sample AppEntity for testing.
     *
     * @param packageName Package name (default: "com.example.test")
     * @param appName App name (default: "Test App")
     * @param isFullyLearned Whether app is fully learned (default: false)
     * @return Sample entity
     */
    fun createAppEntity(
        packageName: String = "com.example.test",
        appName: String = "Test App",
        isFullyLearned: Boolean = false
    ): AppEntity {
        val timestamp = System.currentTimeMillis()
        return AppEntity(
            appId = packageName,
            packageName = packageName,
            appName = appName,
            icon = null,
            isSystemApp = false,
            versionCode = 1L,
            versionName = "1.0.0",
            installTime = timestamp,
            updateTime = timestamp,
            isFullyLearned = isFullyLearned,
            exploredElementCount = 0,
            scrapedElementCount = 0,
            totalScreens = 0,
            lastExplored = null,
            lastScraped = timestamp,
            learnAppEnabled = true,
            dynamicScrapingEnabled = false,
            maxScrapeDepth = 5
        )
    }

    /**
     * Create a sample ScrapedElementDTO for testing.
     *
     * @param elementHash Element hash (default: random)
     * @param appId App ID (default: "com.example.test")
     * @param text Element text (default: "Button")
     * @return Sample DTO
     */
    fun createScrapedElementDTO(
        elementHash: String = "element_${System.currentTimeMillis()}",
        appId: String = "com.example.test",
        text: String? = "Button"
    ): ScrapedElementDTO {
        return ScrapedElementDTO(
            id = null,
            elementHash = elementHash,
            appId = appId,
            uuid = "uuid_$elementHash",
            className = "android.widget.Button",
            viewIdResourceName = "btn_test",
            text = text,
            contentDescription = null,
            bounds = "[0,0][100,50]",
            isClickable = 1L,
            isLongClickable = 0L,
            isEditable = 0L,
            isScrollable = 0L,
            isCheckable = 0L,
            isFocusable = 1L,
            isEnabled = 1L,
            depth = 3L,
            indexInParent = 0L,
            scrapedAt = System.currentTimeMillis(),
            semanticRole = "button",
            inputType = null,
            visualWeight = 1.0,
            isRequired = 0L,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = null
        )
    }

    /**
     * Create a sample ScrapedElementEntity for testing.
     *
     * @param elementHash Element hash (default: random)
     * @param appId App ID (default: "com.example.test")
     * @param text Element text (default: "Button")
     * @return Sample entity
     */
    fun createScrapedElementEntity(
        elementHash: String = "element_${System.currentTimeMillis()}",
        appId: String = "com.example.test",
        text: String? = "Button"
    ): ScrapedElementEntity {
        return ScrapedElementEntity(
            id = null,
            elementHash = elementHash,
            appId = appId,
            uuid = "uuid_$elementHash",
            className = "android.widget.Button",
            viewIdResourceName = "btn_test",
            text = text,
            contentDescription = null,
            bounds = "[0,0][100,50]",
            isClickable = 1L,
            isLongClickable = 0L,
            isEditable = 0L,
            isScrollable = 0L,
            isCheckable = 0L,
            isFocusable = 1L,
            isEnabled = 1L,
            depth = 3L,
            indexInParent = 0L,
            scrapedAt = System.currentTimeMillis(),
            semanticRole = "button",
            inputType = null,
            visualWeight = 1.0,
            isRequired = 0L,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null
        )
    }

    /**
     * Create a sample GeneratedCommandDTO for testing.
     *
     * @param elementHash Element hash
     * @param commandText Command text (default: "click button")
     * @param appId App ID (default: "com.example.test")
     * @return Sample DTO
     */
    fun createGeneratedCommandDTO(
        elementHash: String,
        commandText: String = "click button",
        appId: String = "com.example.test"
    ): GeneratedCommandDTO {
        return GeneratedCommandDTO(
            id = null,
            elementHash = elementHash,
            commandText = commandText,
            actionType = "CLICK",
            confidence = 0.9,
            synonyms = null,
            isUserApproved = 0L,
            usageCount = 0L,
            lastUsed = null,
            createdAt = System.currentTimeMillis(),
            appId = appId,
            appVersion = "1.0.0",
            versionCode = 1L,
            lastVerified = null,
            isDeprecated = 0L
        )
    }

    /**
     * Create a sample ScreenContextDTO for testing.
     *
     * @param screenHash Screen hash (default: random)
     * @param packageName Package name (default: "com.example.test")
     * @return Sample DTO
     */
    fun createScreenContextDTO(
        screenHash: String = "screen_${System.currentTimeMillis()}",
        packageName: String = "com.example.test"
    ): ScreenContextDTO {
        return ScreenContextDTO(
            id = null,
            screenHash = screenHash,
            appId = packageName,
            packageName = packageName,
            activityName = "MainActivity",
            windowTitle = "Test App",
            screenType = "FORM",
            formContext = null,
            navigationLevel = 1L,
            primaryAction = null,
            elementCount = 5L,
            hasBackButton = 1L,
            firstScraped = System.currentTimeMillis(),
            lastScraped = System.currentTimeMillis(),
            visitCount = 1L
        )
    }

    /**
     * Create a list of sample ScrapedElementDTOs for bulk testing.
     *
     * @param count Number of elements to create
     * @param appId App ID for all elements
     * @return List of sample DTOs
     */
    fun createScrapedElementDTOList(
        count: Int,
        appId: String = "com.example.test"
    ): List<ScrapedElementDTO> {
        return (1..count).map { i ->
            createScrapedElementDTO(
                elementHash = "element_$i",
                appId = appId,
                text = "Element $i"
            )
        }
    }

    /**
     * Create a list of sample ScrapedElementEntities for bulk testing.
     *
     * @param count Number of elements to create
     * @param appId App ID for all elements
     * @return List of sample entities
     */
    fun createScrapedElementEntityList(
        count: Int,
        appId: String = "com.example.test"
    ): List<ScrapedElementEntity> {
        return (1..count).map { i ->
            createScrapedElementEntity(
                elementHash = "element_$i",
                appId = appId,
                text = "Element $i"
            )
        }
    }
}
