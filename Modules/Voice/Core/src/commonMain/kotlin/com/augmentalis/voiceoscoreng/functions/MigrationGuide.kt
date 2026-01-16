/**
 * MigrationGuide.kt - Complete migration documentation for VoiceOSCoreNG
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * This file serves as the authoritative migration guide for transitioning from
 * the old VoiceOS modules (LearnAppCore, JITLearning, learnapp) to the new
 * unified VoiceOSCoreNG module.
 *
 * ## PHASE 11: Migration Bridge Adapters
 *
 * The migration bridges enable gradual transition from legacy modules to
 * VoiceOSCoreNG without requiring immediate full rewrites of consuming code.
 *
 * @see LearnAppCoreAdapter
 * @see JITLearningAdapter
 * @see TypeAliases
 */
package com.augmentalis.voiceoscoreng.functions

/**
 * # VoiceOSCoreNG Migration Guide
 *
 * This guide documents the complete migration path from legacy VoiceOS modules
 * to the new unified VoiceOSCoreNG module.
 *
 * ## Overview
 *
 * ### Old Modules Being Replaced:
 *
 * | Old Module | Package | Status |
 * |------------|---------|--------|
 * | LearnAppCore | `com.augmentalis.learnappcore` | Deprecated |
 * | JITLearning | `com.augmentalis.jitlearning` | Deprecated |
 * | VoiceOSCore/learnapp | `com.augmentalis.voiceoscore.learnapp` | Deprecated |
 * | UUIDCreator | `com.augmentalis.uuidcreator` | Deprecated |
 *
 * ### New Unified Module:
 *
 * | New Module | Package |
 * |------------|---------|
 * | VoiceOSCoreNG | `com.augmentalis.voiceoscoreng` |
 *
 * ## Type Mappings
 *
 * ### ElementInfo
 *
 * | Old | New |
 * |-----|-----|
 * | `com.augmentalis.learnappcore.models.ElementInfo` | `com.augmentalis.voiceoscoreng.common.ElementInfo` |
 * | `android.graphics.Rect` (bounds) | `com.augmentalis.voiceoscoreng.common.Bounds` |
 * | `AccessibilityNodeInfo` reference | Not included (platform-independent) |
 *
 * ### UUID/VUID
 *
 * | Old | New |
 * |-----|-----|
 * | `java.util.UUID` (36 chars) | `VUID` String (16 chars) |
 * | `ThirdPartyUuidGenerator` | `VUIDGenerator` object |
 * | `UUIDCreator` | `VUIDGenerator` object |
 * | `UuidAliasManager` | Built into VoiceOSCoreNG |
 *
 * ### JIT Learning
 *
 * | Old | New |
 * |-----|-----|
 * | `JitElementCapture` | `ElementParser` |
 * | `JITLearningService` | Platform-specific service + `ExplorationBridge` |
 * | `JITLearnerProvider` | `ExplorationBridge` APIs |
 * | `JitCapturedElement` | `ElementInfo` |
 *
 * ### Exploration
 *
 * | Old | New |
 * |-----|-----|
 * | `ExplorationEngine` | `ExplorationBridge` |
 * | `ExplorationStrategy` | `ExplorationOptions` |
 * | `ExplorationStats` | `ExplorationProgressResult` |
 * | `ExplorationState` enum | String state (IDLE, RUNNING, etc.) |
 * | `ExplorationDebugCallback` | `Flow<ExplorationEvent>` |
 *
 * ## Migration Steps
 *
 * ### Step 1: Add VoiceOSCoreNG Dependency
 *
 * ```kotlin
 * // build.gradle.kts
 * dependencies {
 *     implementation(project(":Modules:VoiceOSCoreNG"))
 * }
 * ```
 *
 * ### Step 2: Use Migration Imports (Quick Migration)
 *
 * For quick migration with minimal code changes:
 *
 * ```kotlin
 * // Add this import
 * import com.augmentalis.voiceoscoreng.functions.*
 *
 * // Old code continues to work with type aliases:
 * val element: LearnAppElementInfo = ...  // Alias to new ElementInfo
 * val bounds: ElementBounds = ...          // Alias to new Bounds
 * ```
 *
 * ### Step 3: Migrate ElementInfo Usage
 *
 * **Old Code:**
 * ```kotlin
 * val element = ElementInfo(
 *     className = "android.widget.Button",
 *     text = "Submit",
 *     bounds = Rect(10, 20, 110, 70),
 *     node = accessibilityNode  // Platform-specific
 * )
 * ```
 *
 * **New Code:**
 * ```kotlin
 * val element = ElementInfo(
 *     className = "android.widget.Button",
 *     text = "Submit",
 *     bounds = Bounds(10, 20, 110, 70),
 *     packageName = "com.example.app"  // Required for VUID
 * )
 * ```
 *
 * **Using Adapter:**
 * ```kotlin
 * val legacyElement = LegacyElementInfo(...)
 * val newElement = LearnAppCoreAdapter.convertElementInfo(legacyElement)
 * ```
 *
 * ### Step 4: Migrate UUID to VUID
 *
 * **Old Code:**
 * ```kotlin
 * val uuid = thirdPartyGenerator.generateUuid(node, packageName)
 * // Result: "com.example.app.v1.0.0.button-a7f3e2c1d4b5" (40+ chars)
 * ```
 *
 * **New Code:**
 * ```kotlin
 * val vuid = VUIDGenerator.generate(
 *     packageName = "com.example.app",
 *     typeCode = VUIDTypeCode.BUTTON,
 *     elementHash = elementHash
 * )
 * // Result: "a3f2e1-b917cc9dc" (16 chars)
 * ```
 *
 * **Migrating Existing UUIDs:**
 * ```kotlin
 * val legacyUuid = "com.example.app.v1.0.0.button-a7f3e2c1d4b5"
 * val vuid = LearnAppCoreAdapter.migrateUuidToVuid(legacyUuid)
 * // Result: "a3f2e1-b917cc9dc"
 * ```
 *
 * ### Step 5: Migrate JIT Learning
 *
 * **Old Code:**
 * ```kotlin
 * val capturer = JitElementCapture(service, dbManager, uuidGenerator)
 * val elements = capturer.captureScreenElements(packageName)
 * capturer.persistElements(packageName, elements, screenHash)
 * ```
 *
 * **New Code:**
 * ```kotlin
 * // On Android, use platform-specific element extraction
 * // Then convert to ElementInfo:
 * val elements: List<ElementInfo> = ... // From platform code
 *
 * // Use repository for persistence:
 * val repository = RepositoryProvider.getElementRepository()
 * repository.saveElements(packageName, screenHash, elements)
 * ```
 *
 * **Using Adapter:**
 * ```kotlin
 * val adapter = JITLearningAdapter()
 * adapter.pauseLearning()
 * val isPaused = adapter.isLearningPaused()
 * adapter.resumeLearning()
 * ```
 *
 * ### Step 6: Migrate Exploration Engine
 *
 * **Old Code:**
 * ```kotlin
 * val engine = ExplorationEngine(
 *     context, accessibilityService, uuidCreator,
 *     thirdPartyGenerator, aliasManager, repository, databaseManager
 * )
 * engine.startExploration("com.example.app")
 * engine.explorationState.collect { state ->
 *     when (state) {
 *         is ExplorationState.Running -> { ... }
 *         is ExplorationState.Completed -> { ... }
 *     }
 * }
 * ```
 *
 * **New Code:**
 * ```kotlin
 * val bridge = ExplorationBridge(
 *     executor = platformExecutor,  // Platform-specific
 *     repository = repository
 * )
 * bridge.startExploration("com.example.app", ExplorationOptions())
 * bridge.progress.collect { progress ->
 *     when (progress.state) {
 *         "RUNNING" -> { ... }
 *         "COMPLETED" -> { ... }
 *     }
 * }
 * ```
 *
 * ## Deprecation Timeline
 *
 * | Phase | Date | Action |
 * |-------|------|--------|
 * | 11 | 2026-01 | Migration bridges available |
 * | 12 | 2026-02 | Deprecation warnings enabled |
 * | 13 | 2026-03 | Legacy modules marked for removal |
 * | 14 | 2026-04 | Legacy modules removed |
 *
 * ## Common Migration Patterns
 *
 * ### Pattern 1: Gradual Type Migration
 *
 * Use type aliases initially, then gradually replace:
 *
 * ```kotlin
 * // Phase 1: Use alias
 * import com.augmentalis.voiceoscoreng.functions.LearnAppElementInfo
 * val element: LearnAppElementInfo = ...
 *
 * // Phase 2: Replace alias with direct type
 * import com.augmentalis.voiceoscoreng.common.ElementInfo
 * val element: ElementInfo = ...
 * ```
 *
 * ### Pattern 2: Adapter-Based Migration
 *
 * Use adapters for complex conversions:
 *
 * ```kotlin
 * // Convert legacy data
 * val legacyElements: List<LegacyElementInfo> = loadFromLegacyStorage()
 * val newElements = legacyElements.map { legacy ->
 *     LearnAppCoreAdapter.convertElementInfo(legacy)
 * }
 *
 * // Convert back if needed for interop
 * val backToLegacy = newElements.map { element ->
 *     LearnAppCoreAdapter.toLegacyElementInfo(element)
 * }
 * ```
 *
 * ### Pattern 3: UUID Migration Script
 *
 * For migrating stored UUIDs in database:
 *
 * ```kotlin
 * suspend fun migrateStoredUuids(database: Database) {
 *     val elements = database.getAllElements()
 *     elements.forEach { element ->
 *         if (LearnAppCoreAdapter.isLegacyUuid(element.uuid)) {
 *             val vuid = LearnAppCoreAdapter.migrateUuidToVuid(element.uuid)
 *             if (vuid != null) {
 *                 database.updateElementVuid(element.id, vuid)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Testing Migration
 *
 * ### Verify Conversions
 *
 * ```kotlin
 * @Test
 * fun `legacy element converts correctly`() {
 *     val legacy = LegacyElementInfo(
 *         className = "Button",
 *         text = "Submit",
 *         isClickable = true
 *     )
 *     val converted = LearnAppCoreAdapter.convertElementInfo(legacy)
 *     assertEquals("Button", converted.className)
 *     assertEquals("Submit", converted.text)
 *     assertTrue(converted.isClickable)
 * }
 * ```
 *
 * ### Verify UUID Migration
 *
 * ```kotlin
 * @Test
 * fun `legacy UUID migrates to VUID`() {
 *     val legacyUuid = "com.app.button-abc123def456"
 *     val vuid = LearnAppCoreAdapter.migrateUuidToVuid(legacyUuid)
 *     assertNotNull(vuid)
 *     assertTrue(VUIDGenerator.isValidVUID(vuid!!))
 * }
 * ```
 *
 * ## Troubleshooting
 *
 * ### Issue: Type Mismatch
 *
 * **Problem:** Compiler error about incompatible types.
 *
 * **Solution:** Use explicit conversion:
 * ```kotlin
 * val newElement = LearnAppCoreAdapter.convertElementInfo(legacyElement)
 * ```
 *
 * ### Issue: Missing Package Name
 *
 * **Problem:** New ElementInfo requires packageName but old didn't.
 *
 * **Solution:** Add package name when converting:
 * ```kotlin
 * val newElement = LearnAppCoreAdapter.convertElementInfo(legacy)
 *     .copy(packageName = currentPackageName)
 * ```
 *
 * ### Issue: UUID Migration Returns Null
 *
 * **Problem:** migrateUuidToVuid returns null.
 *
 * **Solution:** Check if UUID format is recognized:
 * ```kotlin
 * if (!LearnAppCoreAdapter.isLegacyUuid(uuid)) {
 *     // UUID format not recognized, generate new VUID
 *     val vuid = VUIDGenerator.generate(...)
 * }
 * ```
 *
 * @see LearnAppCoreAdapter
 * @see JITLearningAdapter
 * @see TypeAliases
 */
object MigrationGuide {

    /**
     * Current migration phase.
     */
    const val CURRENT_PHASE = 11

    /**
     * Migration guide version.
     */
    const val VERSION = "1.0.0"

    /**
     * Check if migration is required for a UUID.
     * Note: No legacy data exists, so migration is never required.
     *
     * @param identifier The identifier to check
     * @return Always false - no legacy migration needed
     */
    fun requiresMigration(identifier: String): Boolean {
        // No legacy data exists - migration is never required
        return false
    }

    /**
     * Perform automatic migration of an identifier.
     *
     * @param identifier The identifier to migrate
     * @return Migrated identifier, or original if already migrated
     */
    fun migrateIdentifier(identifier: String): String {
        return if (requiresMigration(identifier)) {
            LearnAppCoreAdapter.migrateUuidToVuid(identifier) ?: identifier
        } else {
            identifier
        }
    }

    /**
     * Get migration summary for logging/debugging.
     */
    fun getMigrationSummary(): String {
        return """
            |VoiceOSCoreNG Migration Guide
            |============================
            |Phase: $CURRENT_PHASE
            |Version: $VERSION
            |
            |Type Mappings:
            |  LearnAppElementInfo -> ElementInfo
            |  ElementBounds -> Bounds
            |  UUID -> VUID
            |  JITElementCapture -> ElementParser
            |  ExplorationEngine -> ExplorationBridge
            |
            |Adapters:
            |  LearnAppCoreAdapter - for LearnAppCore migration
            |  JITLearningAdapter - for JITLearning migration
            |
            |Status: Active - Use adapters for gradual migration
        """.trimMargin()
    }
}
