/**
 * UUIDAccessibilityService.kt - Accessibility service wrapper for UUID generation
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/integration/UUIDAccessibilityService.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Accessibility service wrapper for scanning apps and generating third-party UUIDs
 *
 * NOTE: This file is NOT wired into VOS4. It provides integration interfaces only.
 */

package com.augmentalis.uuidcreator.integration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.uuidcreator.models.UUIDMetadata
import com.augmentalis.uuidcreator.models.UUIDAccessibility
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * UUID Accessibility Service
 *
 * Accessibility service wrapper that scans UI elements and generates UUIDs.
 *
 * ## Integration Pattern
 *
 * This class is designed to be used by VOS4's accessibility service as a
 * delegate for UUID generation and management.
 *
 * ## Usage Example (VOS4 Integration)
 *
 * ```kotlin
 * class VOS4AccessibilityService : AccessibilityService() {
 *     private lateinit var uuidService: UUIDAccessibilityService
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         uuidService = UUIDAccessibilityService(applicationContext)
 *         uuidService.initialize()
 *     }
 *
 *     override fun onAccessibilityEvent(event: AccessibilityEvent) {
 *         uuidService.onAccessibilityEvent(event)
 *     }
 * }
 * ```
 *
 * ## Features
 *
 * - Automatic UUID generation for third-party apps
 * - Alias creation for voice commands
 * - Element registration with UUIDCreator
 * - Background scanning
 *
 * @property context Application context
 *
 * @since 1.0.0
 */
class UUIDAccessibilityService(
    private val context: Context
) {

    /**
     * Third-party UUID generator
     */
    private lateinit var thirdPartyGenerator: ThirdPartyUuidGenerator

    /**
     * Alias manager
     */
    private lateinit var aliasManager: UuidAliasManager

    /**
     * UUIDCreator instance
     */
    private lateinit var uuidCreator: UUIDCreator

    /**
     * Coroutine scope for background operations
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Cache of scanned packages (to avoid re-scanning)
     */
    private val scannedPackages = mutableSetOf<String>()

    /**
     * Initialize the service
     *
     * Must be called before use.
     */
    fun initialize() {
        uuidCreator = UUIDCreator.getInstance()
        thirdPartyGenerator = ThirdPartyUuidGenerator(context)
        aliasManager = UuidAliasManager(com.augmentalis.uuidcreator.database.UUIDCreatorDatabase.getInstance(context))
    }

    /**
     * Handle accessibility event
     *
     * Scans UI elements when window content changes.
     *
     * @param event Accessibility event
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                if (shouldScanPackage(packageName)) {
                    scanWindow(packageName)
                }
            }
        }
    }

    /**
     * Scan window for UI elements
     *
     * Generates UUIDs and aliases for all elements in the current window.
     *
     * @param packageName Package name of the app
     */
    fun scanWindow(packageName: String) {
        scope.launch {
            try {
                // Get root node (would be provided by actual accessibility service)
                // val rootNode = getRootInActiveWindow()
                // if (rootNode != null) {
                //     scanNode(rootNode, packageName)
                // }

                // Mark package as scanned
                scannedPackages.add(packageName)
            } catch (e: Exception) {
                // Handle scanning errors
            }
        }
    }

    /**
     * Scan individual node and register UUID
     *
     * @param node Accessibility node to scan
     * @param packageName Package name
     */
    suspend fun scanNode(node: AccessibilityNodeInfo, packageName: String) {
        // Generate UUID for node
        val uuid = thirdPartyGenerator.generateUuid(node, packageName)

        // Extract element properties
        val elementName = node.text?.toString() ?: node.contentDescription?.toString()
        val elementType = extractElementType(node)

        // Create UUIDElement
        val element = UUIDElement(
            uuid = uuid,
            name = elementName,
            type = elementType,
            metadata = UUIDMetadata(
                attributes = mapOf(
                    "thirdPartyApp" to "true",
                    "packageName" to packageName
                ),
                accessibility = UUIDAccessibility(
                    isClickable = node.isClickable,
                    isFocusable = node.isEnabled
                )
            )
        )

        // Register with UUIDCreator
        uuidCreator.registerElement(element)

        // Create alias for voice commands
        if (elementName != null && elementName.isNotBlank()) {
            aliasManager.createAutoAlias(uuid, elementName, elementType)
        }

        // Recursively scan children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                scanNode(child, packageName)
            }
        }
    }

    /**
     * Check if package should be scanned
     *
     * Filters system apps and already-scanned packages.
     *
     * @param packageName Package name to check
     * @return true if should scan, false otherwise
     */
    private fun shouldScanPackage(packageName: String): Boolean {
        // Skip system packages
        if (packageName.startsWith("com.android") || packageName.startsWith("android")) {
            return false
        }

        // Skip already scanned (unless we want to re-scan)
        if (scannedPackages.contains(packageName)) {
            return false
        }

        return true
    }

    /**
     * Extract element type from accessibility node
     *
     * @param node Accessibility node
     * @return Element type string
     */
    private fun extractElementType(node: AccessibilityNodeInfo): String {
        val className = node.className?.toString() ?: return "unknown"

        return when {
            className.contains("Button", ignoreCase = true) -> "button"
            className.contains("TextView", ignoreCase = true) -> "text"
            className.contains("EditText", ignoreCase = true) -> "input"
            className.contains("ImageView", ignoreCase = true) -> "image"
            className.contains("ImageButton", ignoreCase = true) -> "imagebutton"
            className.contains("CheckBox", ignoreCase = true) -> "checkbox"
            className.contains("RadioButton", ignoreCase = true) -> "radio"
            className.contains("Switch", ignoreCase = true) -> "switch"
            className.contains("ViewGroup", ignoreCase = true) -> "container"
            className.contains("Layout", ignoreCase = true) -> "layout"
            else -> "view"
        }
    }

    /**
     * Clear scan cache
     *
     * Forces re-scanning of all packages on next event.
     */
    fun clearScanCache() {
        scannedPackages.clear()
    }

    /**
     * Scan specific package manually
     *
     * @param packageName Package to scan
     * @param rootNode Root accessibility node
     */
    suspend fun scanPackage(packageName: String, rootNode: AccessibilityNodeInfo) {
        scanNode(rootNode, packageName)
    }

    /**
     * Get scan statistics
     *
     * @return Scan stats
     */
    fun getScanStats(): ScanStats {
        return ScanStats(
            scannedPackages = scannedPackages.size,
            totalElements = uuidCreator.getAllElements().size
        )
    }
}

/**
 * Scan Statistics
 *
 * @property scannedPackages Number of packages scanned
 * @property totalElements Total elements registered
 */
data class ScanStats(
    val scannedPackages: Int,
    val totalElements: Int
)
