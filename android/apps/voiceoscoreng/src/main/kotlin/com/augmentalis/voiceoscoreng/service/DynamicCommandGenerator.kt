package com.augmentalis.voiceoscoreng.service

import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.voiceoscore.AppVersionInfo
import com.augmentalis.voiceoscore.CommandOrchestrator
import com.augmentalis.voiceoscore.CommandRegistry
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.ICommandPersistence
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.UICommandGenerator
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import kotlinx.coroutines.CoroutineScope

private const val TAG = "DynamicCommandGen"

/**
 * App-level command generation wrapper.
 *
 * Composes CommandOrchestrator (core logic) with app-specific concerns:
 * - Overlay badge generation via OverlayItemGenerator
 * - Database persistence via CommandPersistenceManager
 * - UI command generation via UICommandGenerator
 *
 * Core command generation logic is delegated to CommandOrchestrator in VoiceOSCore.
 */
class DynamicCommandGenerator(
    private val commandRegistry: CommandRegistry,
    commandPersistence: ICommandPersistence,
    scrapedAppRepository: IScrapedAppRepository,
    scrapedElementRepository: IScrapedElementRepository,
    private val scope: CoroutineScope,
    private val getAppInfo: (String) -> AppVersionInfo
) {
    // Core orchestrator for command generation
    private val orchestrator = CommandOrchestrator(commandRegistry, commandPersistence)

    // App-specific persistence manager for scraped elements
    private val persistenceManager = CommandPersistenceManager(
        commandPersistence, scrapedAppRepository, scrapedElementRepository, scope, getAppInfo
    )

    /**
     * Generate voice commands for actionable elements.
     *
     * Delegates core logic to CommandOrchestrator, then adds:
     * - Overlay item generation
     * - UI command generation
     * - Scraped element persistence
     *
     * @param screenHash Optional screen hash for element persistence. When provided,
     *                   elements are stored per-screen to preserve commands across
     *                   screen navigation within the same app.
     */
    fun generateCommands(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>,
        elementLabels: Map<Int, String>,
        packageName: String,
        screenHash: String? = null,
        updateSpeechEngine: ((List<String>) -> Unit)? = null
    ): CommandGenerationResult {
        // Core command generation
        val coreResult = orchestrator.generateCommands(
            elements = elements,
            packageName = packageName,
            updateSpeechEngine = updateSpeechEngine
        )

        // App-specific: Generate overlay items for visual badges
        val listItems = elements.filter { it.listIndex >= 0 }
        val overlayItems = OverlayItemGenerator.generate(listItems, elements, packageName)
        OverlayStateManager.updateNumberedOverlayItems(overlayItems)

        // App-specific: Generate UI commands for exploration display
        val uiCommands = UICommandGenerator.generate(elements, elementLabels, packageName)

        // App-specific: Persist scraped elements with full metadata
        if (coreResult.staticCommands.isNotEmpty()) {
            persistenceManager.persistStaticCommands(
                coreResult.staticCommands,
                elements,
                packageName,
                coreResult.dynamicCommands.size,
                screenHash
            )
        }

        Log.v(TAG, "Generated ${coreResult.allCommands.size} commands, ${overlayItems.size} overlays")

        return CommandGenerationResult(
            uiCommands = uiCommands,
            allQuantizedCommands = coreResult.allCommands,
            staticCommands = coreResult.staticCommands,
            dynamicCommands = coreResult.dynamicCommands,
            indexCommands = coreResult.indexCommands,
            labelCommands = coreResult.labelCommands,
            numericCommands = coreResult.numericCommands,
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
        // Core incremental generation
        val coreResult = orchestrator.generateCommandsIncremental(
            elements = elements,
            packageName = packageName,
            existingCommands = existingCommands,
            updateSpeechEngine = updateSpeechEngine
        )

        // App-specific: Generate overlay items with AVID assignments
        val listItems = elements.filter { it.listIndex >= 0 }
        val avidAssignments = orchestrator.getAvidAssignments()
        val overlayItems = OverlayItemGenerator.generateIncremental(
            listItems, elements, packageName, avidAssignments
        )
        OverlayStateManager.updateNumberedOverlayItemsIncremental(overlayItems)

        Log.v(TAG, "Incremental: ${coreResult.totalCommands} total, ${overlayItems.size} overlays")

        return IncrementalUpdateResult(
            totalCommands = coreResult.totalCommands,
            newCommands = coreResult.newCommands,
            preservedCommands = coreResult.preservedCommands,
            removedCommands = coreResult.removedCommands
        )
    }

    /**
     * Clear AVID assignments. Call when switching apps or resetting state.
     */
    fun clearAvidAssignments() {
        orchestrator.clearAvidAssignments()
    }

    companion object {
        /**
         * Get app version info from PackageManager.
         */
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
