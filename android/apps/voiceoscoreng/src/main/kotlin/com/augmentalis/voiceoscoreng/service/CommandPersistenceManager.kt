package com.augmentalis.voiceoscoreng.service

import android.util.Log
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.HashUtils
import com.augmentalis.voiceoscore.AppVersionInfo
import com.augmentalis.voiceoscore.ICommandPersistence
import com.augmentalis.database.dto.ScrapedAppDTO
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "CommandPersistence"

/**
 * Manages persistence of static commands to SQLDelight database.
 * Extracted from DynamicCommandGenerator for SOLID compliance.
 * Single Responsibility: Command persistence to database.
 */
class CommandPersistenceManager(
    private val commandPersistence: ICommandPersistence,
    private val scrapedAppRepository: IScrapedAppRepository,
    private val scrapedElementRepository: IScrapedElementRepository,
    private val scope: CoroutineScope,
    private val getAppInfo: (String) -> AppVersionInfo
) {

    /**
     * Persist static commands to SQLDelight database.
     *
     * FIX (2026-01-19): Resolve FOREIGN KEY constraint failure (code 787)
     * Root cause: Commands were being inserted referencing elementHashes that
     * didn't exist in scraped_element table.
     *
     * FIX (2026-01-22): Additional FK constraint fix
     * Root cause #2: Commands with empty/null elementHash were passing validation
     * but failing FK constraint (no element with hash "").
     *
     * Solution:
     * - Pre-filter commands to only those with valid elementHash
     * - Track which elements were SUCCESSFULLY inserted (not just attempted)
     * - Verify element existence before command insert
     * - Only insert commands for elements that actually exist in DB
     */
    fun persistStaticCommands(
        staticQuantizedCommands: List<QuantizedCommand>,
        elements: List<ElementInfo>,
        packageName: String,
        dynamicCount: Int
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()

                // Pre-filter: Only commands with valid non-empty elementHash
                val validHashCommands = staticQuantizedCommands.filter { cmd ->
                    val hash = cmd.metadata["elementHash"]
                    !hash.isNullOrBlank()
                }

                if (validHashCommands.isEmpty()) {
                    Log.w(TAG, "No commands with valid elementHash to persist (filtered ${staticQuantizedCommands.size})")
                    return@launch
                }

                val filteredOut = staticQuantizedCommands.size - validHashCommands.size
                if (filteredOut > 0) {
                    Log.w(TAG, "Pre-filtered $filteredOut commands with missing/empty elementHash")
                }

                // Step 1: Ensure scraped_app exists (FK parent)
                insertScrapedApp(packageName, elements.size, validHashCommands.size, currentTime)

                // Step 2: Insert scraped_elements (FK parent for commands)
                val confirmedHashes = insertScrapedElements(
                    validHashCommands, elements, packageName, currentTime
                )

                // Step 3: Insert ONLY commands whose elements exist in DB
                insertValidCommands(
                    validHashCommands, confirmedHashes, dynamicCount
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist commands to database", e)
            }
        }
    }

    private suspend fun insertScrapedApp(
        packageName: String,
        elementCount: Int,
        commandCount: Int,
        currentTime: Long
    ) {
        val appInfo = getAppInfo(packageName)
        val appHash = HashUtils.calculateHash(packageName + appInfo.versionCode).take(8)
        val scrapedApp = ScrapedAppDTO(
            appId = packageName,
            packageName = packageName,
            versionCode = appInfo.versionCode,
            versionName = appInfo.versionName,
            appHash = appHash,
            isFullyLearned = 0,
            learnCompletedAt = null,
            scrapingMode = "DYNAMIC",
            scrapeCount = 1,
            elementCount = elementCount.toLong(),
            commandCount = commandCount.toLong(),
            firstScrapedAt = currentTime,
            lastScrapedAt = currentTime
        )
        scrapedAppRepository.insert(scrapedApp)
        Log.v(TAG, "Inserted scraped_app for $packageName")
    }

    private suspend fun insertScrapedElements(
        commands: List<QuantizedCommand>,
        elements: List<ElementInfo>,
        packageName: String,
        currentTime: Long
    ): Set<String> {
        val confirmedHashes = mutableSetOf<String>()
        var insertedCount = 0
        var alreadyExistedCount = 0

        commands.forEach { cmd ->
            val elementHash = cmd.metadata["elementHash"] ?: return@forEach
            if (elementHash in confirmedHashes) return@forEach

            val element = findMatchingElement(cmd, elements)
            val scrapedElement = createScrapedElementDTO(
                elementHash, packageName, element, cmd, currentTime
            )

            try {
                scrapedElementRepository.insert(scrapedElement)
                confirmedHashes.add(elementHash)
                insertedCount++
            } catch (e: Exception) {
                val existing = scrapedElementRepository.getByHash(elementHash)
                if (existing != null) {
                    confirmedHashes.add(elementHash)
                    alreadyExistedCount++
                } else {
                    Log.w(TAG, "Element $elementHash insert failed: ${e.message}")
                }
            }
        }
        Log.v(TAG, "Inserted $insertedCount elements ($alreadyExistedCount pre-existed)")
        return confirmedHashes
    }

    private fun findMatchingElement(
        cmd: QuantizedCommand,
        elements: List<ElementInfo>
    ): ElementInfo? {
        return elements.find { el ->
            val cmdClassName = cmd.metadata["className"] ?: ""
            val cmdResourceId = cmd.metadata["resourceId"] ?: ""
            el.className == cmdClassName && el.resourceId == cmdResourceId
        } ?: elements.firstOrNull { el ->
            val cmdLabel = cmd.metadata["label"] ?: ""
            el.text == cmdLabel || el.contentDescription == cmdLabel
        }
    }

    private fun createScrapedElementDTO(
        elementHash: String,
        packageName: String,
        element: ElementInfo?,
        cmd: QuantizedCommand,
        currentTime: Long
    ): ScrapedElementDTO {
        val bounds = element?.let {
            "${it.bounds.left},${it.bounds.top},${it.bounds.right},${it.bounds.bottom}"
        } ?: "0,0,0,0"

        return ScrapedElementDTO(
            id = 0,
            elementHash = elementHash,
            appId = packageName,
            uuid = null,
            className = element?.className ?: cmd.metadata["className"] ?: "",
            viewIdResourceName = element?.resourceId?.ifBlank { null }
                ?: cmd.metadata["resourceId"]?.ifBlank { null },
            text = element?.text?.ifBlank { null },
            contentDescription = element?.contentDescription?.ifBlank { null },
            bounds = bounds,
            isClickable = if (element?.isClickable == true) 1L else 0L,
            isLongClickable = if (element?.isLongClickable == true) 1L else 0L,
            isEditable = 0L,
            isScrollable = if (element?.isScrollable == true) 1L else 0L,
            isCheckable = 0L,
            isFocusable = 0L,
            isEnabled = if (element?.isEnabled != false) 1L else 0L,
            depth = 0L,
            indexInParent = 0L,
            scrapedAt = currentTime,
            semanticRole = null,
            inputType = null,
            visualWeight = null,
            isRequired = null,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = null
        )
    }

    private suspend fun insertValidCommands(
        commands: List<QuantizedCommand>,
        confirmedHashes: Set<String>,
        dynamicCount: Int
    ) {
        val validCommands = commands.filter { cmd ->
            val hash = cmd.metadata["elementHash"]
            hash != null && hash in confirmedHashes
        }

        val skippedCount = commands.size - validCommands.size
        if (skippedCount > 0) {
            Log.w(TAG, "Skipping $skippedCount commands with missing FK references")
        }

        if (validCommands.isNotEmpty()) {
            commandPersistence.insertBatch(validCommands)
            Log.v(TAG, "Persisted ${validCommands.size} commands (skipped $dynamicCount dynamic)")
        }
    }
}
