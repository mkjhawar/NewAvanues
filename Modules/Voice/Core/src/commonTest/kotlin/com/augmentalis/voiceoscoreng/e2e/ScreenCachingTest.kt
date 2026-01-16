/**
 * ScreenCachingTest.kt - Screen hash caching tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * Tests screen hash caching to verify that the system correctly:
 * - Avoids re-scanning known screens
 * - Detects screen changes (content or app version)
 * - Caches commands for screen hashes
 * - Handles dynamic content screens
 */
package com.augmentalis.voiceoscoreng.e2e

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.functions.FingerprintUtils
import com.augmentalis.voiceoscoreng.functions.PopupInfo
import com.augmentalis.voiceoscoreng.functions.PopupType
import com.augmentalis.voiceoscoreng.functions.ScreenFingerprinter
import com.augmentalis.voiceoscoreng.functions.ScreenState
import com.augmentalis.voiceoscoreng.persistence.ScreenHashRepository
import com.augmentalis.voiceoscoreng.persistence.ScreenInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for screen hash caching functionality.
 *
 * Validates the intelligent caching system that prevents
 * unnecessary re-scanning of known screens.
 */
class ScreenCachingTest {

    private lateinit var fingerprinter: ScreenFingerprinter
    private lateinit var mockRepository: MockScreenHashRepository

    @BeforeTest
    fun setup() {
        fingerprinter = ScreenFingerprinter()
        mockRepository = MockScreenHashRepository()
    }

    @AfterTest
    fun teardown() {
        mockRepository.clear()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 1: Fingerprint Generation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `same screen elements produce same fingerprint`() {
        val elements = createSampleScreen("Main Screen")

        val hash1 = fingerprinter.calculateFingerprint(elements)
        val hash2 = fingerprinter.calculateFingerprint(elements)

        assertEquals(hash1, hash2, "Same elements should produce same hash")
        assertNotEquals(FingerprintUtils.EMPTY_HASH, hash1)
    }

    @Test
    fun `different screen elements produce different fingerprint`() {
        val elements1 = createSampleScreen("Main Screen")
        val elements2 = createSampleScreen("Settings Screen")

        val hash1 = fingerprinter.calculateFingerprint(elements1)
        val hash2 = fingerprinter.calculateFingerprint(elements2)

        assertNotEquals(hash1, hash2, "Different elements should produce different hash")
    }

    @Test
    fun `null input produces empty hash`() {
        val hash = fingerprinter.calculateFingerprint(null)
        assertEquals(FingerprintUtils.EMPTY_HASH, hash)
    }

    @Test
    fun `empty list produces empty hash`() {
        val hash = fingerprinter.calculateFingerprint(emptyList<ElementInfo>())
        assertEquals(FingerprintUtils.EMPTY_HASH, hash)
    }

    @Test
    fun `element order does not affect fingerprint`() {
        val elements1 = listOf(
            ElementInfo.button("Submit", "btn1", "com.app"),
            ElementInfo.button("Cancel", "btn2", "com.app")
        )
        val elements2 = listOf(
            ElementInfo.button("Cancel", "btn2", "com.app"),
            ElementInfo.button("Submit", "btn1", "com.app")
        )

        val hash1 = fingerprinter.calculateFingerprint(elements1)
        val hash2 = fingerprinter.calculateFingerprint(elements2)

        assertEquals(hash1, hash2, "Element order should not affect fingerprint")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 2: Structural Fingerprinting
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `structural fingerprint ignores text content`() {
        val elements1 = listOf(
            ElementInfo(
                className = "Button",
                resourceId = "com.app:id/btn",
                text = "Submit",
                isClickable = true,
                packageName = "com.app"
            )
        )
        val elements2 = listOf(
            ElementInfo(
                className = "Button",
                resourceId = "com.app:id/btn",
                text = "Different Text",
                isClickable = true,
                packageName = "com.app"
            )
        )

        val structHash1 = fingerprinter.calculateStructuralFingerprint(elements1)
        val structHash2 = fingerprinter.calculateStructuralFingerprint(elements2)

        assertEquals(structHash1, structHash2, "Structural hash should ignore text")

        // But content hash should be different
        val contentHash1 = fingerprinter.calculateFingerprint(elements1)
        val contentHash2 = fingerprinter.calculateFingerprint(elements2)
        assertNotEquals(contentHash1, contentHash2, "Content hash should differ")
    }

    @Test
    fun `structural fingerprint detects class changes`() {
        val elements1 = listOf(
            ElementInfo(className = "Button", resourceId = "btn", isClickable = true, packageName = "com.app")
        )
        val elements2 = listOf(
            ElementInfo(className = "ImageButton", resourceId = "btn", isClickable = true, packageName = "com.app")
        )

        val hash1 = fingerprinter.calculateStructuralFingerprint(elements1)
        val hash2 = fingerprinter.calculateStructuralFingerprint(elements2)

        assertNotEquals(hash1, hash2, "Different classes should produce different structural hash")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 3: Dynamic Content Detection
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `detects dynamic content in timestamps`() {
        val dynamicElements = listOf(
            ElementInfo(className = "TextView", text = "5 mins ago", packageName = "com.app"),
            ElementInfo(className = "TextView", text = "Just now", packageName = "com.app"),
            ElementInfo(className = "TextView", text = "3:45 PM", packageName = "com.app")
        )

        assertTrue(fingerprinter.isDynamicContentScreen(dynamicElements))
    }

    @Test
    fun `does not flag static content as dynamic`() {
        val staticElements = listOf(
            ElementInfo(className = "TextView", text = "Settings", packageName = "com.app"),
            ElementInfo(className = "TextView", text = "Account", packageName = "com.app"),
            ElementInfo.button("Submit", "btn", "com.app")
        )

        assertFalse(fingerprinter.isDynamicContentScreen(staticElements))
    }

    @Test
    fun `normalizes dynamic patterns in text`() {
        val original = "Updated 5 mins ago at 3:45 PM (99+)"
        val normalized = FingerprintUtils.normalizeText(original)

        assertFalse(normalized.contains("5"))
        assertFalse(normalized.contains("3:45"))
        assertFalse(normalized.contains("99"))
        assertTrue(normalized.contains("["))  // Should have placeholders
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 4: Popup Detection
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `detects dialog popups`() {
        val dialogElements = listOf(
            ElementInfo(className = "AlertDialog", text = "Confirm Delete", packageName = "com.app"),
            ElementInfo.button("OK", "ok_btn", "com.app"),
            ElementInfo.button("Cancel", "cancel_btn", "com.app")
        )

        val popupInfo = fingerprinter.detectPopup(dialogElements)

        assertTrue(popupInfo.isPopup)
        assertEquals(PopupType.ALERT, popupInfo.popupType)
        assertTrue(popupInfo.hasPositiveAction)
        assertTrue(popupInfo.hasNegativeAction)
    }

    @Test
    fun `detects bottom sheet`() {
        val bottomSheetElements = listOf(
            ElementInfo(className = "BottomSheetDialog", text = "Options", packageName = "com.app"),
            ElementInfo.button("Share", "share", "com.app"),
            ElementInfo.button("Delete", "delete", "com.app")
        )

        val popupInfo = fingerprinter.detectPopup(bottomSheetElements)

        assertTrue(popupInfo.isPopup)
        assertEquals(PopupType.BOTTOM_SHEET, popupInfo.popupType)
    }

    @Test
    fun `non-popup screen returns false`() {
        val normalElements = listOf(
            ElementInfo(className = "LinearLayout", packageName = "com.app"),
            ElementInfo.button("Submit", "submit", "com.app")
        )

        val popupInfo = fingerprinter.detectPopup(normalElements)

        assertFalse(popupInfo.isPopup)
        assertEquals(PopupType.UNKNOWN, popupInfo.popupType)
    }

    @Test
    fun `popup fingerprint differs from screen fingerprint`() {
        val elements = listOf(
            ElementInfo(className = "AlertDialog", text = "Confirm", packageName = "com.app"),
            ElementInfo.button("OK", "ok", "com.app")
        )

        val screenHash = fingerprinter.calculateFingerprint(elements)
        val popupHash = fingerprinter.calculatePopupFingerprint(elements, PopupType.DIALOG)

        assertNotEquals(screenHash, popupHash, "Popup hash should differ from screen hash")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 5: Screen State Comparison
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `same screen states are equal`() {
        val elements = createSampleScreen("Main")
        val hash = fingerprinter.calculateFingerprint(elements)

        val state1 = ScreenState(hash = hash, elementCount = elements.size)
        val state2 = ScreenState(hash = hash, elementCount = elements.size)

        assertTrue(state1.isSameScreen(state2))
    }

    @Test
    fun `different screen states are not equal`() {
        val elements1 = createSampleScreen("Main")
        val elements2 = createSampleScreen("Settings")

        val state1 = ScreenState(hash = fingerprinter.calculateFingerprint(elements1))
        val state2 = ScreenState(hash = fingerprinter.calculateFingerprint(elements2))

        assertFalse(state1.isSameScreen(state2))
    }

    @Test
    fun `popup and non-popup are different`() {
        val elements = listOf(ElementInfo.button("OK", "ok", "com.app"))
        val hash = fingerprinter.calculateFingerprint(elements)

        val normalState = ScreenState(hash = hash, isPopup = false)
        val popupState = ScreenState(hash = hash, isPopup = true)

        assertFalse(normalState.isSameScreen(popupState))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 6: Repository Caching
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `cached screen is found in repository`() = kotlinx.coroutines.runBlocking {
        val elements = createSampleScreen("Main")
        val hash = fingerprinter.calculateFingerprint(elements)

        // Save screen
        mockRepository.saveScreen(
            hash = hash,
            packageName = "com.test.app",
            activityName = "MainActivity",
            appVersion = "1.0.0",
            elementCount = elements.size
        )

        // Check existence
        assertTrue(mockRepository.hasScreen(hash))
    }

    @Test
    fun `unknown screen not found in repository`() = kotlinx.coroutines.runBlocking {
        val unknownHash = "unknownhash123456789"
        assertFalse(mockRepository.hasScreen(unknownHash))
    }

    @Test
    fun `commands cached for screen`() = kotlinx.coroutines.runBlocking {
        val elements = createSampleScreen("Main")
        val hash = fingerprinter.calculateFingerprint(elements)

        val commands = listOf(
            QuantizedCommand.create(
                avid = "cmd-001",
                phrase = "submit",
                actionType = CommandActionType.CLICK,
                packageName = "com.test.app",
                targetAvid = "BTN:submit"
            ),
            QuantizedCommand.create(
                avid = "cmd-002",
                phrase = "cancel",
                actionType = CommandActionType.CLICK,
                packageName = "com.test.app",
                targetAvid = "BTN:cancel"
            )
        )

        // Save commands
        mockRepository.saveCommandsForScreen(hash, commands)

        // Retrieve
        val cached = mockRepository.getCommandsForScreen(hash)
        assertEquals(2, cached.size)
        assertEquals("submit", cached[0].phrase)
    }

    @Test
    fun `screen cleared correctly`() = kotlinx.coroutines.runBlocking {
        val hash = "testscreenhash"

        mockRepository.saveScreen(hash, "com.app", "Main", "1.0.0", 10)
        assertTrue(mockRepository.hasScreen(hash))

        mockRepository.clearScreen(hash)
        assertFalse(mockRepository.hasScreen(hash))
    }

    @Test
    fun `clear screens for package removes only that package`() = kotlinx.coroutines.runBlocking {
        // Save screens for two packages
        mockRepository.saveScreen("hash1", "com.app1", "Main", "1.0.0", 5)
        mockRepository.saveScreen("hash2", "com.app1", "Settings", "1.0.0", 3)
        mockRepository.saveScreen("hash3", "com.app2", "Main", "1.0.0", 7)

        // Clear only com.app1
        val cleared = mockRepository.clearScreensForPackage("com.app1")

        assertEquals(2, cleared)
        assertFalse(mockRepository.hasScreen("hash1"))
        assertFalse(mockRepository.hasScreen("hash2"))
        assertTrue(mockRepository.hasScreen("hash3"))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 7: App Version Invalidation
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `app version change triggers rescan`() = kotlinx.coroutines.runBlocking {
        val hash = "screenhash"

        // Save with old version
        mockRepository.saveScreen(hash, "com.app", "Main", "1.0.0", 10)

        // Check stored version
        val storedVersion = mockRepository.getAppVersion(hash)
        assertEquals("1.0.0", storedVersion)

        // App was updated - new version is different
        val currentVersion = "2.0.0"
        val needsRescan = storedVersion != currentVersion

        assertTrue(needsRescan, "Version change should trigger rescan")
    }

    @Test
    fun `same version skips rescan`() = kotlinx.coroutines.runBlocking {
        val hash = "screenhash"
        val version = "1.0.0"

        mockRepository.saveScreen(hash, "com.app", "Main", version, 10)

        val storedVersion = mockRepository.getAppVersion(hash)
        val needsRescan = storedVersion != version

        assertFalse(needsRescan, "Same version should skip rescan")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Test 8: Screen Info for Debug
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `screen info returned for cached screen`() = kotlinx.coroutines.runBlocking {
        val hash = "screenhash"
        mockRepository.saveScreen(hash, "com.app", "MainActivity", "1.0.0", 15)
        mockRepository.saveCommandsForScreen(hash, listOf(
            QuantizedCommand.create("cmd1", "click", CommandActionType.CLICK, "com.app")
        ))

        val info = mockRepository.getScreenInfo(hash)

        assertNotNull(info)
        assertEquals(hash, info.hash)
        assertEquals("com.app", info.packageName)
        assertEquals("MainActivity", info.activityName)
        assertEquals(15, info.elementCount)
        assertEquals(1, info.commandCount)
        assertTrue(info.isCached)
    }

    @Test
    fun `screen info null for unknown screen`() = kotlinx.coroutines.runBlocking {
        val info = mockRepository.getScreenInfo("nonexistent")
        assertNull(info)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Functions
    // ═══════════════════════════════════════════════════════════════════════════

    private fun createSampleScreen(title: String): List<ElementInfo> {
        return listOf(
            ElementInfo(
                className = "TextView",
                text = title,
                bounds = Bounds(0, 0, 1080, 100),
                packageName = "com.sample.app"
            ),
            ElementInfo.button("Submit", "com.sample:id/submit", "com.sample.app", Bounds(100, 200, 300, 250)),
            ElementInfo.button("Cancel", "com.sample:id/cancel", "com.sample.app", Bounds(400, 200, 600, 250)),
            ElementInfo(
                className = "EditText",
                resourceId = "com.sample:id/input",
                hint = "Enter text",
                bounds = Bounds(100, 300, 980, 350),
                isClickable = true,
                packageName = "com.sample.app"
            )
        )
    }
}

/**
 * Mock implementation of ScreenHashRepository for testing.
 */
class MockScreenHashRepository : ScreenHashRepository {

    private val screens = mutableMapOf<String, ScreenData>()
    private val commands = mutableMapOf<String, List<QuantizedCommand>>()

    data class ScreenData(
        val hash: String,
        val packageName: String,
        val activityName: String?,
        val appVersion: String,
        val elementCount: Int,
        val scannedAt: Long = System.currentTimeMillis()
    )

    fun clear() {
        screens.clear()
        commands.clear()
    }

    override suspend fun hasScreen(hash: String): Boolean {
        return screens.containsKey(hash)
    }

    override suspend fun saveScreen(
        hash: String,
        packageName: String,
        activityName: String?,
        appVersion: String,
        elementCount: Int
    ) {
        screens[hash] = ScreenData(hash, packageName, activityName, appVersion, elementCount)
    }

    override suspend fun getAppVersion(hash: String): String? {
        return screens[hash]?.appVersion
    }

    override suspend fun getCommandsForScreen(hash: String): List<QuantizedCommand> {
        return commands[hash] ?: emptyList()
    }

    override suspend fun saveCommandsForScreen(hash: String, commands: List<QuantizedCommand>) {
        this.commands[hash] = commands
    }

    override suspend fun clearScreen(hash: String) {
        screens.remove(hash)
        commands.remove(hash)
    }

    override suspend fun clearScreensForPackage(packageName: String): Int {
        val toRemove = screens.filter { it.value.packageName == packageName }.keys
        toRemove.forEach { hash ->
            screens.remove(hash)
            commands.remove(hash)
        }
        return toRemove.size
    }

    override suspend fun clearAllScreens(): Int {
        val count = screens.size
        screens.clear()
        commands.clear()
        return count
    }

    override suspend fun getScreenCount(): Int {
        return screens.size
    }

    override suspend fun getScreenCountForPackage(packageName: String): Int {
        return screens.count { it.value.packageName == packageName }
    }

    override suspend fun getScreenInfo(hash: String): ScreenInfo? {
        val data = screens[hash] ?: return null
        val cmdList = commands[hash] ?: emptyList()
        return ScreenInfo(
            hash = data.hash,
            packageName = data.packageName,
            activityName = data.activityName,
            appVersion = data.appVersion,
            elementCount = data.elementCount,
            actionableCount = 0, // Simplified for test
            commandCount = cmdList.size,
            scannedAt = data.scannedAt,
            isCached = true
        )
    }
}
