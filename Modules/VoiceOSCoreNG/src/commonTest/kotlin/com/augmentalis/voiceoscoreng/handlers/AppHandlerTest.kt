/**
 * AppHandlerTest.kt - TDD tests for AppHandler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Tests for app launching and management handler using TDD methodology.
 * Phase 12 of the VoiceOSCoreNG handler system.
 */
package com.augmentalis.voiceoscoreng.handlers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * TDD tests for AppHandler class.
 *
 * AppHandler manages app launching and registration for voice commands.
 * It provides app lookup by display name, package name, or alias.
 */
class AppHandlerTest {

    // ==================== Mock IAppLauncher ====================

    /**
     * Mock app launcher for testing.
     * Tracks launch calls and simulates success/failure.
     */
    class MockAppLauncher(
        private val shouldSucceed: Boolean = true,
        private val installedApps: List<AppInfo> = emptyList()
    ) : IAppLauncher {
        val launchedPackages = mutableListOf<String>()

        override fun launchApp(packageName: String): Boolean {
            launchedPackages.add(packageName)
            return shouldSucceed
        }

        override fun getInstalledApps(): List<AppInfo> = installedApps
    }

    // ==================== Default Registration Tests ====================

    @Test
    fun `default constructor should register common apps`() {
        val handler = AppHandler()
        val appCount = handler.getAppCount()
        assertTrue(appCount >= 7, "Should have at least 7 common apps registered")
    }

    @Test
    fun `should have Google Maps registered by default`() {
        val handler = AppHandler()
        val mapsApp = handler.findApp("google maps")
        assertNotNull(mapsApp, "Google Maps should be registered")
        assertEquals("com.google.android.apps.maps", mapsApp.packageName)
    }

    @Test
    fun `should have YouTube registered by default`() {
        val handler = AppHandler()
        val youtubeApp = handler.findApp("youtube")
        assertNotNull(youtubeApp, "YouTube should be registered")
        assertEquals("com.google.android.youtube", youtubeApp.packageName)
    }

    @Test
    fun `should have Chrome registered by default`() {
        val handler = AppHandler()
        val chromeApp = handler.findApp("chrome")
        assertNotNull(chromeApp, "Chrome should be registered")
        assertEquals("com.android.chrome", chromeApp.packageName)
    }

    @Test
    fun `should have Settings registered by default`() {
        val handler = AppHandler()
        val settingsApp = handler.findApp("settings")
        assertNotNull(settingsApp, "Settings should be registered")
        assertEquals("com.android.settings", settingsApp.packageName)
    }

    @Test
    fun `should have Phone registered by default`() {
        val handler = AppHandler()
        val phoneApp = handler.findApp("phone")
        assertNotNull(phoneApp, "Phone should be registered")
        assertEquals("com.android.dialer", phoneApp.packageName)
    }

    @Test
    fun `should have Messages registered by default`() {
        val handler = AppHandler()
        val messagesApp = handler.findApp("messages")
        assertNotNull(messagesApp, "Messages should be registered")
        assertEquals("com.android.mms", messagesApp.packageName)
    }

    @Test
    fun `should have Camera registered by default`() {
        val handler = AppHandler()
        val cameraApp = handler.findApp("camera")
        assertNotNull(cameraApp, "Camera should be registered")
        assertEquals("com.android.camera", cameraApp.packageName)
    }

    // ==================== handleCommand Tests ====================

    @Test
    fun `handleCommand with open prefix should find app`() {
        val mockLauncher = MockAppLauncher()
        val handler = AppHandler(mockLauncher)

        val result = handler.handleCommand("open maps")

        assertTrue(result.success, "Should succeed")
        assertNotNull(result.app, "Should return app info")
        assertEquals("com.google.android.apps.maps", result.app?.packageName)
    }

    @Test
    fun `handleCommand with launch prefix should find app`() {
        val mockLauncher = MockAppLauncher()
        val handler = AppHandler(mockLauncher)

        val result = handler.handleCommand("launch youtube")

        assertTrue(result.success, "Should succeed")
        assertNotNull(result.app, "Should return app info")
        assertEquals("com.google.android.youtube", result.app?.packageName)
    }

    @Test
    fun `handleCommand with start prefix should find app`() {
        val mockLauncher = MockAppLauncher()
        val handler = AppHandler(mockLauncher)

        val result = handler.handleCommand("start camera")

        assertTrue(result.success, "Should succeed")
        assertNotNull(result.app, "Should return app info")
        assertEquals("com.android.camera", result.app?.packageName)
    }

    @Test
    fun `handleCommand should be case insensitive`() {
        val mockLauncher = MockAppLauncher()
        val handler = AppHandler(mockLauncher)

        val result1 = handler.handleCommand("OPEN MAPS")
        val result2 = handler.handleCommand("Open Maps")
        val result3 = handler.handleCommand("open MAPS")

        assertTrue(result1.success, "Should handle uppercase")
        assertTrue(result2.success, "Should handle mixed case")
        assertTrue(result3.success, "Should handle partial uppercase")
    }

    @Test
    fun `handleCommand with unknown command should fail`() {
        val handler = AppHandler()

        val result = handler.handleCommand("run maps")

        assertFalse(result.success, "Should fail for unknown command prefix")
        assertNotNull(result.error, "Should have error message")
        assertTrue(result.error!!.contains("Unknown app command"), "Error should mention unknown command")
    }

    @Test
    fun `handleCommand with unknown app should fail`() {
        val handler = AppHandler()

        val result = handler.handleCommand("open nonexistent")

        assertFalse(result.success, "Should fail for unknown app")
        assertNotNull(result.error, "Should have error message")
        assertTrue(result.error!!.contains("not found"), "Error should mention app not found")
    }

    // ==================== openApp Tests ====================

    @Test
    fun `openApp by display name should succeed`() {
        val mockLauncher = MockAppLauncher()
        val handler = AppHandler(mockLauncher)

        val result = handler.openApp("Google Maps")

        assertTrue(result.success, "Should succeed")
        assertEquals("Google Maps", result.app?.displayName)
        assertTrue(mockLauncher.launchedPackages.contains("com.google.android.apps.maps"))
    }

    @Test
    fun `openApp by alias should succeed`() {
        val mockLauncher = MockAppLauncher()
        val handler = AppHandler(mockLauncher)

        val result = handler.openApp("browser")

        assertTrue(result.success, "Should succeed with alias")
        assertEquals("Chrome", result.app?.displayName)
        assertTrue(mockLauncher.launchedPackages.contains("com.android.chrome"))
    }

    @Test
    fun `openApp by navigation alias should find Maps`() {
        val mockLauncher = MockAppLauncher()
        val handler = AppHandler(mockLauncher)

        val result = handler.openApp("navigation")

        assertTrue(result.success, "Should succeed with navigation alias")
        assertEquals("Google Maps", result.app?.displayName)
    }

    @Test
    fun `openApp with unknown app should fail`() {
        val handler = AppHandler()

        val result = handler.openApp("unknownapp")

        assertFalse(result.success, "Should fail")
        assertNull(result.app, "Should not return app")
        assertNotNull(result.error, "Should have error message")
        assertTrue(result.error!!.contains("not found"), "Error should mention not found")
    }

    @Test
    fun `openApp should handle launcher failure`() {
        val mockLauncher = MockAppLauncher(shouldSucceed = false)
        val handler = AppHandler(mockLauncher)

        val result = handler.openApp("maps")

        assertFalse(result.success, "Should fail when launcher fails")
        assertNotNull(result.app, "Should still return app info")
        assertNotNull(result.error, "Should have error message")
        assertTrue(result.error!!.contains("Failed to launch"), "Error should mention launch failure")
    }

    @Test
    fun `openApp without launcher should succeed`() {
        // When no launcher is provided, openApp should succeed (simulation mode)
        val handler = AppHandler(null)

        val result = handler.openApp("maps")

        assertTrue(result.success, "Should succeed without launcher (simulation mode)")
        assertNotNull(result.app, "Should return app info")
    }

    // ==================== findApp Tests ====================

    @Test
    fun `findApp by exact display name should work`() {
        val handler = AppHandler()

        val app = handler.findApp("Google Maps")

        assertNotNull(app, "Should find by exact name")
        assertEquals("Google Maps", app.displayName)
    }

    @Test
    fun `findApp by lowercase display name should work`() {
        val handler = AppHandler()

        val app = handler.findApp("google maps")

        assertNotNull(app, "Should find by lowercase name")
        assertEquals("Google Maps", app.displayName)
    }

    @Test
    fun `findApp by package name should work`() {
        val handler = AppHandler()

        val app = handler.findApp("com.google.android.youtube")

        assertNotNull(app, "Should find by package name")
        assertEquals("YouTube", app.displayName)
    }

    @Test
    fun `findApp by alias should work`() {
        val handler = AppHandler()

        val app = handler.findApp("videos")

        assertNotNull(app, "Should find by alias")
        assertEquals("YouTube", app.displayName)
    }

    @Test
    fun `findApp with internet alias should find Chrome`() {
        val handler = AppHandler()

        val app = handler.findApp("internet")

        assertNotNull(app, "Should find Chrome by internet alias")
        assertEquals("Chrome", app.displayName)
    }

    @Test
    fun `findApp with call alias should find Phone`() {
        val handler = AppHandler()

        val app = handler.findApp("call")

        assertNotNull(app, "Should find Phone by call alias")
        assertEquals("Phone", app.displayName)
    }

    @Test
    fun `findApp with texting alias should find Messages`() {
        val handler = AppHandler()

        val app = handler.findApp("texting")

        assertNotNull(app, "Should find Messages by texting alias")
        assertEquals("Messages", app.displayName)
    }

    @Test
    fun `findApp with photo alias should find Camera`() {
        val handler = AppHandler()

        val app = handler.findApp("photo")

        assertNotNull(app, "Should find Camera by photo alias")
        assertEquals("Camera", app.displayName)
    }

    @Test
    fun `findApp with unknown query should return null`() {
        val handler = AppHandler()

        val app = handler.findApp("nonexistent")

        assertNull(app, "Should return null for unknown query")
    }

    @Test
    fun `findApp should trim whitespace`() {
        val handler = AppHandler()

        val app = handler.findApp("  maps  ")

        assertNotNull(app, "Should find app with trimmed input")
        assertEquals("Google Maps", app.displayName)
    }

    // ==================== registerApp Tests ====================

    @Test
    fun `registerApp should add new app`() {
        val handler = AppHandler()
        val initialCount = handler.getAppCount()

        val newApp = AppInfo(
            packageName = "com.example.test",
            displayName = "Test App",
            aliases = listOf("testapp", "test")
        )
        handler.registerApp(newApp)

        assertEquals(initialCount + 1, handler.getAppCount())
    }

    @Test
    fun `registerApp should allow finding by new display name`() {
        val handler = AppHandler()

        val newApp = AppInfo(
            packageName = "com.example.test",
            displayName = "Test App",
            aliases = listOf("testapp")
        )
        handler.registerApp(newApp)

        val found = handler.findApp("Test App")
        assertNotNull(found, "Should find by display name")
        assertEquals("com.example.test", found.packageName)
    }

    @Test
    fun `registerApp should allow finding by new alias`() {
        val handler = AppHandler()

        val newApp = AppInfo(
            packageName = "com.example.test",
            displayName = "Test App",
            aliases = listOf("testapp", "mytest")
        )
        handler.registerApp(newApp)

        val found = handler.findApp("mytest")
        assertNotNull(found, "Should find by alias")
        assertEquals("Test App", found.displayName)
    }

    @Test
    fun `registerApp with same package should update existing`() {
        val handler = AppHandler()

        // Register initial app
        val app1 = AppInfo("com.test", "App V1", listOf("v1"))
        handler.registerApp(app1)
        val countAfterFirst = handler.getAppCount()

        // Register updated app with same package
        val app2 = AppInfo("com.test", "App V2", listOf("v2"))
        handler.registerApp(app2)

        assertEquals(countAfterFirst, handler.getAppCount(), "Should not increase count")

        val found = handler.findApp("com.test")
        assertEquals("App V2", found?.displayName, "Should have updated name")
    }

    // ==================== unregisterApp Tests ====================

    @Test
    fun `unregisterApp should remove existing app`() {
        val handler = AppHandler()
        val initialCount = handler.getAppCount()

        val removed = handler.unregisterApp("com.google.android.apps.maps")

        assertTrue(removed, "Should return true for removed app")
        assertEquals(initialCount - 1, handler.getAppCount())
    }

    @Test
    fun `unregisterApp should prevent finding by removed package`() {
        val handler = AppHandler()

        handler.unregisterApp("com.google.android.youtube")

        val found = handler.findApp("youtube")
        assertNull(found, "Should not find removed app by alias")
    }

    @Test
    fun `unregisterApp with unknown package should return false`() {
        val handler = AppHandler()
        val initialCount = handler.getAppCount()

        val removed = handler.unregisterApp("com.unknown.package")

        assertFalse(removed, "Should return false for unknown package")
        assertEquals(initialCount, handler.getAppCount(), "Count should not change")
    }

    // ==================== getRegisteredApps Tests ====================

    @Test
    fun `getRegisteredApps should return all apps`() {
        val handler = AppHandler()

        val apps = handler.getRegisteredApps()

        assertTrue(apps.isNotEmpty(), "Should have apps")
        assertEquals(handler.getAppCount(), apps.size, "Size should match count")
    }

    @Test
    fun `getRegisteredApps should include custom registered apps`() {
        val handler = AppHandler()

        val customApp = AppInfo("com.custom", "Custom App", emptyList())
        handler.registerApp(customApp)

        val apps = handler.getRegisteredApps()

        assertTrue(apps.any { it.packageName == "com.custom" }, "Should include custom app")
    }

    // ==================== syncInstalledApps Tests ====================

    @Test
    fun `syncInstalledApps should register apps from launcher`() {
        val installedApps = listOf(
            AppInfo("com.new.app1", "New App 1", listOf("app1")),
            AppInfo("com.new.app2", "New App 2", listOf("app2"))
        )
        val mockLauncher = MockAppLauncher(installedApps = installedApps)
        val handler = AppHandler(mockLauncher)
        val initialCount = handler.getAppCount()

        handler.syncInstalledApps()

        assertEquals(initialCount + 2, handler.getAppCount(), "Should add new apps")
        assertNotNull(handler.findApp("app1"), "Should find synced app by alias")
        assertNotNull(handler.findApp("app2"), "Should find synced app by alias")
    }

    @Test
    fun `syncInstalledApps without launcher should do nothing`() {
        val handler = AppHandler(null)
        val initialCount = handler.getAppCount()

        handler.syncInstalledApps()

        assertEquals(initialCount, handler.getAppCount(), "Count should not change")
    }

    // ==================== AppInfo Data Class Tests ====================

    @Test
    fun `AppInfo should store all properties correctly`() {
        val app = AppInfo(
            packageName = "com.test.app",
            displayName = "Test App",
            aliases = listOf("test", "tester")
        )

        assertEquals("com.test.app", app.packageName)
        assertEquals("Test App", app.displayName)
        assertEquals(2, app.aliases.size)
        assertTrue(app.aliases.contains("test"))
        assertTrue(app.aliases.contains("tester"))
    }

    @Test
    fun `AppInfo should have default empty aliases`() {
        val app = AppInfo(
            packageName = "com.test.app",
            displayName = "Test App"
        )

        assertTrue(app.aliases.isEmpty(), "Default aliases should be empty list")
    }

    @Test
    fun `AppInfo equals should work correctly`() {
        val app1 = AppInfo("com.test", "Test", listOf("t"))
        val app2 = AppInfo("com.test", "Test", listOf("t"))
        val app3 = AppInfo("com.other", "Test", listOf("t"))

        assertEquals(app1, app2, "Same properties should be equal")
        assertFalse(app1 == app3, "Different package should not be equal")
    }

    // ==================== AppLaunchResult Data Class Tests ====================

    @Test
    fun `AppLaunchResult success should have correct properties`() {
        val app = AppInfo("com.test", "Test", emptyList())
        val result = AppLaunchResult(success = true, app = app)

        assertTrue(result.success)
        assertEquals(app, result.app)
        assertNull(result.error)
    }

    @Test
    fun `AppLaunchResult failure should have correct properties`() {
        val result = AppLaunchResult(success = false, error = "App not found")

        assertFalse(result.success)
        assertNull(result.app)
        assertEquals("App not found", result.error)
    }

    @Test
    fun `AppLaunchResult failure with app should have all properties`() {
        val app = AppInfo("com.test", "Test", emptyList())
        val result = AppLaunchResult(success = false, app = app, error = "Launch failed")

        assertFalse(result.success)
        assertEquals(app, result.app)
        assertEquals("Launch failed", result.error)
    }

    // ==================== Integration Tests ====================

    @Test
    fun `full workflow should work correctly`() {
        val mockLauncher = MockAppLauncher()
        val handler = AppHandler(mockLauncher)

        // 1. Add custom app
        val customApp = AppInfo("com.custom", "Custom", listOf("mycustom"))
        handler.registerApp(customApp)

        // 2. Find by alias
        val found = handler.findApp("mycustom")
        assertNotNull(found)
        assertEquals("Custom", found.displayName)

        // 3. Launch via command
        val result = handler.handleCommand("open mycustom")
        assertTrue(result.success)
        assertEquals("com.custom", result.app?.packageName)
        assertTrue(mockLauncher.launchedPackages.contains("com.custom"))

        // 4. Unregister
        assertTrue(handler.unregisterApp("com.custom"))
        assertNull(handler.findApp("mycustom"))
    }

    @Test
    fun `multiple launches should track all packages`() {
        val mockLauncher = MockAppLauncher()
        val handler = AppHandler(mockLauncher)

        handler.handleCommand("open maps")
        handler.handleCommand("open youtube")
        handler.handleCommand("open chrome")

        assertEquals(3, mockLauncher.launchedPackages.size)
        assertTrue(mockLauncher.launchedPackages.contains("com.google.android.apps.maps"))
        assertTrue(mockLauncher.launchedPackages.contains("com.google.android.youtube"))
        assertTrue(mockLauncher.launchedPackages.contains("com.android.chrome"))
    }
}
