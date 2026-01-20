package com.augmentalis.voiceoscoreng.service

import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.voiceoscore.CommandGenerator
import com.augmentalis.voiceoscore.CommandRegistry
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.StaticCommandRegistry
import com.augmentalis.voiceoscore.ICommandPersistence
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import kotlinx.coroutines.CoroutineScope

private const val TAG = "DynamicCommandGen"

/**
 * Generates voice commands for actionable elements.
 *
 * Single Responsibility: Orchestrate command generation and registry updates.
 * Delegates to:
 * - CommandPersistenceManager: Database persistence
 * - OverlayItemGenerator: Visual badge generation
 * - LegacyCommandGenerator: Backwards-compatible command format
 */
class DynamicCommandGenerator(
    private val commandRegistry: CommandRegistry,
    commandPersistence: ICommandPersistence,
    scrapedAppRepository: IScrapedAppRepository,
    scrapedElementRepository: IScrapedElementRepository,
    private val scope: CoroutineScope,
    private val getAppInfo: (String) -> AppVersionInfo
) {
    private val persistenceManager = CommandPersistenceManager(
        commandPersistence, scrapedAppRepository, scrapedElementRepository, scope, getAppInfo
    )

    private val avidAssignments = mutableMapOf<String, Int>()
    private var nextAvidNumber = 1
    private var currentAppPackage: String? = null

    /**
     * Generate voice commands for actionable elements using KMP CommandGenerator.
     */
    fun generateCommands(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>,
        elementLabels: Map<Int, String>,
        packageName: String,
        updateSpeechEngine: ((List<String>) -> Unit)? = null
    ): CommandGenerationResult {
        val commandResults = elements.mapNotNull { element ->
            CommandGenerator.fromElementWithPersistence(element, packageName)
        }

        val staticCommands = commandResults.filter { it.shouldPersist }
        val dynamicCommands = commandResults.filter { !it.shouldPersist }
        val allCommands = commandResults.map { it.command }
        commandRegistry.updateSync(allCommands)

        val listItems = elements.filter { it.listIndex >= 0 }
        val indexCommands = generateAndRegisterIndexCommands(listItems, packageName)
        val labelCommands = generateAndRegisterLabelCommands(listItems, packageName)
        val numericCommands = generateAndRegisterNumericCommands(listItems, packageName)

        val overlayItems = OverlayItemGenerator.generate(listItems, elements, packageName)
        OverlayStateManager.updateNumberedOverlayItems(overlayItems)

        logCommandSummary(allCommands, staticCommands, dynamicCommands, indexCommands)

        val commandPhrases = buildSpeechPhrases(
            allCommands, indexCommands, labelCommands, numericCommands
        )
        updateSpeechEngine?.invoke(commandPhrases)

        val staticQuantizedCommands = staticCommands.map { it.command }
        if (staticQuantizedCommands.isNotEmpty()) {
            persistenceManager.persistStaticCommands(
                staticQuantizedCommands, elements, packageName, dynamicCommands.size
            )
        }

        val legacyCommands = LegacyCommandGenerator.generate(elements, elementLabels, packageName)

        return CommandGenerationResult(
            legacyCommands = legacyCommands,
            allQuantizedCommands = allCommands,
            staticCommands = staticQuantizedCommands,
            dynamicCommands = dynamicCommands.map { it.command },
            indexCommands = indexCommands,
            labelCommands = labelCommands,
            numericCommands = numericCommands,
            overlayItems = overlayItems
        )
    }

    /**
     * Generate commands incrementally for scroll/content changes.
     */
    fun generateCommandsIncremental(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>,
        elementLabels: Map<Int, String>,
        packageName: String,
        existingCommands: List<QuantizedCommand>,
        updateSpeechEngine: ((List<String>) -> Unit)? = null
    ): IncrementalUpdateResult {
        if (packageName != currentAppPackage) {
            clearAvidAssignments()
            currentAppPackage = packageName
            Log.v(TAG, "App changed to $packageName - reset AVID")
        }

        val existingByHash = existingCommands.associateBy { it.metadata["elementHash"] }
        var newCount = 0
        var preservedCount = 0
        val mergedCommands = mutableListOf<QuantizedCommand>()

        val commandResults = elements.mapNotNull { element ->
            CommandGenerator.fromElementWithPersistence(element, packageName)
        }

        commandResults.forEach { result ->
            val cmd = result.command
            val hash = cmd.metadata["elementHash"]

            if (hash != null && hash in avidAssignments) {
                val existingCmd = existingByHash[hash]
                mergedCommands.add(existingCmd ?: cmd)
                preservedCount++
            } else {
                if (hash != null) avidAssignments[hash] = nextAvidNumber++
                mergedCommands.add(cmd)
                newCount++
            }
        }

        commandRegistry.updateSync(mergedCommands)

        val listItems = elements.filter { it.listIndex >= 0 }
        val indexCommands = generateAndRegisterIndexCommands(listItems, packageName)
        val labelCommands = generateAndRegisterLabelCommands(listItems, packageName)
        val numericCommands = generateAndRegisterNumericCommands(listItems, packageName)

        val overlayItems = OverlayItemGenerator.generateIncremental(
            listItems, elements, packageName, avidAssignments
        )
        OverlayStateManager.updateNumberedOverlayItemsIncremental(overlayItems)

        val allPhrases = buildSpeechPhrases(
            mergedCommands, indexCommands, labelCommands, numericCommands
        )
        updateSpeechEngine?.invoke(allPhrases)

        Log.v(TAG, "Incremental: ${mergedCommands.size} ($newCount new, $preservedCount kept)")

        return IncrementalUpdateResult(
            totalCommands = mergedCommands.size + indexCommands.size + labelCommands.size,
            newCommands = newCount,
            preservedCommands = preservedCount,
            removedCommands = existingCommands.size - preservedCount
        )
    }

    fun clearAvidAssignments() {
        avidAssignments.clear()
        nextAvidNumber = 1
        currentAppPackage = null
    }

    private fun generateAndRegisterIndexCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val commands = CommandGenerator.generateListIndexCommands(listItems, packageName)
        if (commands.isNotEmpty()) commandRegistry.addAll(commands)
        return commands
    }

    private fun generateAndRegisterLabelCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val commands = CommandGenerator.generateListLabelCommands(listItems, packageName)
        if (commands.isNotEmpty()) commandRegistry.addAll(commands)
        return commands
    }

    private fun generateAndRegisterNumericCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val commands = CommandGenerator.generateNumericCommands(listItems, packageName)
        if (commands.isNotEmpty()) commandRegistry.addAll(commands)
        return commands
    }

    private fun buildSpeechPhrases(
        allCommands: List<QuantizedCommand>,
        indexCommands: List<QuantizedCommand>,
        labelCommands: List<QuantizedCommand>,
        numericCommands: List<QuantizedCommand>
    ): List<String> {
        val staticPhrases = StaticCommandRegistry.allPhrases()
        val dynamicPhrases = allCommands.map { it.phrase } +
            indexCommands.map { it.phrase } +
            labelCommands.map { it.phrase } +
            numericCommands.map { it.phrase }
        return (staticPhrases + dynamicPhrases).distinct()
    }

    private fun logCommandSummary(
        allCommands: List<QuantizedCommand>,
        staticCommands: List<CommandGenerator.GeneratedCommandResult>,
        dynamicCommands: List<CommandGenerator.GeneratedCommandResult>,
        indexCommands: List<QuantizedCommand>
    ) {
        Log.v(TAG, "Commands: ${allCommands.size} (${staticCommands.size} static)")
        if (dynamicCommands.isNotEmpty()) {
            Log.v(TAG, "Dynamic: ${dynamicCommands.take(3).map { it.command.phrase }}")
        }
        if (indexCommands.isNotEmpty()) {
            Log.v(TAG, "Index: ${indexCommands.take(5).map { it.phrase }}")
        }
    }

    companion object {
        fun getAppInfoFromPackageManager(
            packageManager: PackageManager,
            packageName: String
        ): AppVersionInfo {
            return try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                AppVersionInfo(versionCode, packageInfo.versionName ?: "unknown")
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Package not found: $packageName")
                AppVersionInfo(0, "unknown")
            }
        }
    }
}
