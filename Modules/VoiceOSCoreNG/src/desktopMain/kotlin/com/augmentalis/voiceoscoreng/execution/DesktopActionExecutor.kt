/**
 * DesktopActionExecutor.kt - Desktop (JVM) implementation of action executor
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Desktop-specific action execution using AWT Robot and system utilities.
 * Implements IActionExecutor for voice command execution on macOS/Windows/Linux.
 */
package com.augmentalis.voiceoscoreng.execution

import com.augmentalis.voiceoscoreng.avu.CommandActionType
import com.augmentalis.voiceoscoreng.avu.QuantizedCommand
import java.awt.Desktop
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.net.URI

/**
 * Desktop implementation of IActionExecutor.
 *
 * Uses Java AWT for automation:
 * - Robot for keyboard/mouse input simulation
 * - Desktop for opening apps/URLs
 * - Toolkit for clipboard operations
 *
 * Platform differences handled:
 * - macOS: Command key for shortcuts
 * - Windows/Linux: Control key for shortcuts
 */
class DesktopActionExecutor : IActionExecutor {

    private val robot: Robot by lazy { Robot() }
    private val isMac: Boolean = System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("windows")
    private val modifierKey: Int = if (isMac) KeyEvent.VK_META else KeyEvent.VK_CONTROL

    // ═══════════════════════════════════════════════════════════════════
    // Element Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun tap(vuid: String): ActionResult {
        // Desktop doesn't have VUID-based element targeting
        // Would require integration with accessibility APIs (MSAA, AT-SPI, AXUIElement)
        return ActionResult.NotSupported(
            actionType = CommandActionType.TAP,
            message = "Desktop: VUID element targeting requires accessibility API integration"
        )
    }

    override suspend fun longPress(vuid: String, durationMs: Long): ActionResult {
        return ActionResult.NotSupported(
            actionType = CommandActionType.LONG_PRESS,
            message = "Desktop: VUID element targeting requires accessibility API integration"
        )
    }

    override suspend fun focus(vuid: String): ActionResult {
        return ActionResult.NotSupported(
            actionType = CommandActionType.FOCUS,
            message = "Desktop: VUID element targeting requires accessibility API integration"
        )
    }

    override suspend fun enterText(text: String, vuid: String?): ActionResult {
        return try {
            // Use clipboard to enter text (more reliable than simulating key presses)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val selection = StringSelection(text)
            clipboard.setContents(selection, selection)

            // Paste from clipboard
            robot.keyPress(modifierKey)
            robot.keyPress(KeyEvent.VK_V)
            robot.keyRelease(KeyEvent.VK_V)
            robot.keyRelease(modifierKey)

            ActionResult.Success("Text entered via clipboard")
        } catch (e: Exception) {
            ActionResult.Error("Failed to enter text: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Scroll Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun scroll(
        direction: ScrollDirection,
        amount: Float,
        vuid: String?
    ): ActionResult {
        return try {
            val scrollAmount = (amount * 10).toInt().coerceAtLeast(1)
            when (direction) {
                ScrollDirection.UP -> robot.mouseWheel(-scrollAmount)
                ScrollDirection.DOWN -> robot.mouseWheel(scrollAmount)
                ScrollDirection.LEFT, ScrollDirection.RIGHT -> {
                    // Horizontal scroll requires Shift+scroll on most platforms
                    robot.keyPress(KeyEvent.VK_SHIFT)
                    robot.mouseWheel(if (direction == ScrollDirection.LEFT) -scrollAmount else scrollAmount)
                    robot.keyRelease(KeyEvent.VK_SHIFT)
                }
            }
            ActionResult.Success("Scrolled ${direction.name.lowercase()}")
        } catch (e: Exception) {
            ActionResult.Error("Failed to scroll: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Navigation Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun back(): ActionResult {
        return try {
            // Alt+Left on Windows/Linux, Cmd+[ on macOS
            if (isMac) {
                robot.keyPress(KeyEvent.VK_META)
                robot.keyPress(KeyEvent.VK_OPEN_BRACKET)
                robot.keyRelease(KeyEvent.VK_OPEN_BRACKET)
                robot.keyRelease(KeyEvent.VK_META)
            } else {
                robot.keyPress(KeyEvent.VK_ALT)
                robot.keyPress(KeyEvent.VK_LEFT)
                robot.keyRelease(KeyEvent.VK_LEFT)
                robot.keyRelease(KeyEvent.VK_ALT)
            }
            ActionResult.Success("Navigate back")
        } catch (e: Exception) {
            ActionResult.Error("Failed to navigate back: ${e.message}", e)
        }
    }

    override suspend fun home(): ActionResult {
        return try {
            if (isMac) {
                // Mission Control on macOS (F3 or Ctrl+Up)
                robot.keyPress(KeyEvent.VK_F3)
                robot.keyRelease(KeyEvent.VK_F3)
            } else if (isWindows) {
                // Win+D to show desktop
                robot.keyPress(KeyEvent.VK_WINDOWS)
                robot.keyPress(KeyEvent.VK_D)
                robot.keyRelease(KeyEvent.VK_D)
                robot.keyRelease(KeyEvent.VK_WINDOWS)
            } else {
                // Linux: Super key
                robot.keyPress(KeyEvent.VK_WINDOWS)
                robot.keyRelease(KeyEvent.VK_WINDOWS)
            }
            ActionResult.Success("Show desktop/home")
        } catch (e: Exception) {
            ActionResult.Error("Failed to go home: ${e.message}", e)
        }
    }

    override suspend fun recentApps(): ActionResult {
        return try {
            if (isMac) {
                // Cmd+Tab for app switcher
                robot.keyPress(KeyEvent.VK_META)
                robot.keyPress(KeyEvent.VK_TAB)
                robot.keyRelease(KeyEvent.VK_TAB)
                robot.keyRelease(KeyEvent.VK_META)
            } else if (isWindows) {
                // Alt+Tab
                robot.keyPress(KeyEvent.VK_ALT)
                robot.keyPress(KeyEvent.VK_TAB)
                robot.keyRelease(KeyEvent.VK_TAB)
                robot.keyRelease(KeyEvent.VK_ALT)
            } else {
                // Linux: Alt+Tab
                robot.keyPress(KeyEvent.VK_ALT)
                robot.keyPress(KeyEvent.VK_TAB)
                robot.keyRelease(KeyEvent.VK_TAB)
                robot.keyRelease(KeyEvent.VK_ALT)
            }
            ActionResult.Success("Show recent apps")
        } catch (e: Exception) {
            ActionResult.Error("Failed to show recent apps: ${e.message}", e)
        }
    }

    override suspend fun appDrawer(): ActionResult {
        return try {
            if (isMac) {
                // Launchpad: F4 or pinch with 4 fingers
                robot.keyPress(KeyEvent.VK_F4)
                robot.keyRelease(KeyEvent.VK_F4)
            } else if (isWindows) {
                // Start menu
                robot.keyPress(KeyEvent.VK_WINDOWS)
                robot.keyRelease(KeyEvent.VK_WINDOWS)
            } else {
                // Linux app menu
                robot.keyPress(KeyEvent.VK_WINDOWS)
                robot.keyRelease(KeyEvent.VK_WINDOWS)
            }
            ActionResult.Success("Show app drawer/launcher")
        } catch (e: Exception) {
            ActionResult.Error("Failed to open app drawer: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // System Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun openSettings(): ActionResult {
        return try {
            if (isMac) {
                // Open System Preferences/Settings
                Runtime.getRuntime().exec(arrayOf("open", "-a", "System Preferences"))
            } else if (isWindows) {
                Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", "ms-settings:"))
            } else {
                // GNOME settings
                Runtime.getRuntime().exec(arrayOf("gnome-control-center"))
            }
            ActionResult.Success("Opening settings")
        } catch (e: Exception) {
            ActionResult.Error("Failed to open settings: ${e.message}", e)
        }
    }

    override suspend fun showNotifications(): ActionResult {
        return try {
            if (isMac) {
                // Click notification center icon (no direct keyboard shortcut)
                // Alternative: use AppleScript
                Runtime.getRuntime().exec(arrayOf(
                    "osascript", "-e",
                    "tell application \"System Events\" to click menu bar item \"Notification Center\" of menu bar 1 of application process \"ControlCenter\""
                ))
            } else if (isWindows) {
                // Win+A for Action Center
                robot.keyPress(KeyEvent.VK_WINDOWS)
                robot.keyPress(KeyEvent.VK_A)
                robot.keyRelease(KeyEvent.VK_A)
                robot.keyRelease(KeyEvent.VK_WINDOWS)
            }
            ActionResult.Success("Show notifications")
        } catch (e: Exception) {
            ActionResult.Error("Failed to show notifications: ${e.message}", e)
        }
    }

    override suspend fun clearNotifications(): ActionResult {
        // Desktop platforms don't have a universal clear notifications API
        return ActionResult.NotSupported(
            actionType = CommandActionType.NOTIFICATIONS,
            message = "Desktop: clear notifications not universally supported"
        )
    }

    override suspend fun screenshot(): ActionResult {
        return try {
            if (isMac) {
                // Cmd+Shift+3 for full screenshot, Cmd+Shift+4 for selection
                robot.keyPress(KeyEvent.VK_META)
                robot.keyPress(KeyEvent.VK_SHIFT)
                robot.keyPress(KeyEvent.VK_3)
                robot.keyRelease(KeyEvent.VK_3)
                robot.keyRelease(KeyEvent.VK_SHIFT)
                robot.keyRelease(KeyEvent.VK_META)
            } else if (isWindows) {
                // Win+PrintScreen
                robot.keyPress(KeyEvent.VK_WINDOWS)
                robot.keyPress(KeyEvent.VK_PRINTSCREEN)
                robot.keyRelease(KeyEvent.VK_PRINTSCREEN)
                robot.keyRelease(KeyEvent.VK_WINDOWS)
            } else {
                // gnome-screenshot or PrintScreen
                robot.keyPress(KeyEvent.VK_PRINTSCREEN)
                robot.keyRelease(KeyEvent.VK_PRINTSCREEN)
            }
            ActionResult.Success("Screenshot captured")
        } catch (e: Exception) {
            ActionResult.Error("Failed to take screenshot: ${e.message}", e)
        }
    }

    override suspend fun flashlight(on: Boolean): ActionResult {
        // Desktop doesn't have flashlight (no camera flash)
        return ActionResult.NotSupported(
            actionType = if (on) CommandActionType.FLASHLIGHT_ON else CommandActionType.FLASHLIGHT_OFF,
            message = "Desktop: no flashlight available"
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // Media Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun mediaPlayPause(): ActionResult {
        return try {
            robot.keyPress(KeyEvent.VK_SPACE)
            robot.keyRelease(KeyEvent.VK_SPACE)
            // Note: This only works if a media app has focus
            // For global media control, platform-specific APIs needed
            ActionResult.Success("Media play/pause (requires media app focus)")
        } catch (e: Exception) {
            ActionResult.Error("Failed to play/pause: ${e.message}", e)
        }
    }

    override suspend fun mediaNext(): ActionResult {
        return try {
            // F9 or media key on some keyboards
            if (isMac) {
                Runtime.getRuntime().exec(arrayOf(
                    "osascript", "-e",
                    "tell application \"System Events\" to key code 124 using {command down}" // Cmd+Right
                ))
            }
            ActionResult.Success("Media next (platform-dependent)")
        } catch (e: Exception) {
            ActionResult.Error("Failed to skip next: ${e.message}", e)
        }
    }

    override suspend fun mediaPrevious(): ActionResult {
        return try {
            if (isMac) {
                Runtime.getRuntime().exec(arrayOf(
                    "osascript", "-e",
                    "tell application \"System Events\" to key code 123 using {command down}" // Cmd+Left
                ))
            }
            ActionResult.Success("Media previous (platform-dependent)")
        } catch (e: Exception) {
            ActionResult.Error("Failed to skip previous: ${e.message}", e)
        }
    }

    override suspend fun volume(direction: VolumeDirection): ActionResult {
        return try {
            when (direction) {
                VolumeDirection.UP -> {
                    if (isMac) {
                        Runtime.getRuntime().exec(arrayOf(
                            "osascript", "-e",
                            "set volume output volume ((output volume of (get volume settings)) + 10)"
                        ))
                    } else {
                        // Use media keys if available
                        robot.keyPress(KeyEvent.VK_F12) // Often volume up
                        robot.keyRelease(KeyEvent.VK_F12)
                    }
                }
                VolumeDirection.DOWN -> {
                    if (isMac) {
                        Runtime.getRuntime().exec(arrayOf(
                            "osascript", "-e",
                            "set volume output volume ((output volume of (get volume settings)) - 10)"
                        ))
                    } else {
                        robot.keyPress(KeyEvent.VK_F11) // Often volume down
                        robot.keyRelease(KeyEvent.VK_F11)
                    }
                }
                VolumeDirection.MUTE, VolumeDirection.UNMUTE -> {
                    if (isMac) {
                        Runtime.getRuntime().exec(arrayOf(
                            "osascript", "-e",
                            "set volume output muted not (output muted of (get volume settings))"
                        ))
                    } else {
                        robot.keyPress(KeyEvent.VK_F10) // Often mute
                        robot.keyRelease(KeyEvent.VK_F10)
                    }
                }
            }
            ActionResult.Success("Volume ${direction.name.lowercase()}")
        } catch (e: Exception) {
            ActionResult.Error("Failed to adjust volume: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // App Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun openApp(appType: String): ActionResult {
        return try {
            val appName = when (appType.lowercase()) {
                "browser" -> if (isMac) "Safari" else if (isWindows) "msedge" else "firefox"
                "mail", "email" -> if (isMac) "Mail" else if (isWindows) "outlook" else "thunderbird"
                "calendar" -> if (isMac) "Calendar" else if (isWindows) "outlookcal:" else "gnome-calendar"
                "calculator" -> if (isMac) "Calculator" else if (isWindows) "calc" else "gnome-calculator"
                "music" -> if (isMac) "Music" else if (isWindows) "spotify" else "rhythmbox"
                "notes" -> if (isMac) "Notes" else "notepad"
                "terminal" -> if (isMac) "Terminal" else if (isWindows) "cmd" else "gnome-terminal"
                "files", "finder" -> if (isMac) "Finder" else if (isWindows) "explorer" else "nautilus"
                else -> appType
            }

            if (isMac) {
                Runtime.getRuntime().exec(arrayOf("open", "-a", appName))
            } else if (isWindows) {
                Runtime.getRuntime().exec(arrayOf("cmd", "/c", "start", appName))
            } else {
                Runtime.getRuntime().exec(arrayOf(appName))
            }
            ActionResult.Success("Opening $appType")
        } catch (e: Exception) {
            ActionResult.Error("Failed to open $appType: ${e.message}", e)
        }
    }

    override suspend fun openAppByPackage(packageName: String): ActionResult {
        // Desktop uses app names, not package names
        return openApp(packageName)
    }

    override suspend fun closeApp(): ActionResult {
        return try {
            // Cmd+Q on macOS, Alt+F4 on Windows/Linux
            if (isMac) {
                robot.keyPress(KeyEvent.VK_META)
                robot.keyPress(KeyEvent.VK_Q)
                robot.keyRelease(KeyEvent.VK_Q)
                robot.keyRelease(KeyEvent.VK_META)
            } else {
                robot.keyPress(KeyEvent.VK_ALT)
                robot.keyPress(KeyEvent.VK_F4)
                robot.keyRelease(KeyEvent.VK_F4)
                robot.keyRelease(KeyEvent.VK_ALT)
            }
            ActionResult.Success("Close app")
        } catch (e: Exception) {
            ActionResult.Error("Failed to close app: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Generic Execution
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun executeCommand(command: QuantizedCommand): ActionResult {
        return executeAction(command.actionType, command.metadata)
    }

    override suspend fun executeAction(
        actionType: CommandActionType,
        params: Map<String, Any>
    ): ActionResult {
        return when (actionType) {
            CommandActionType.TAP -> {
                val vuid = params["vuid"] as? String
                if (vuid != null) tap(vuid) else ActionResult.Error("Missing vuid parameter")
            }
            CommandActionType.LONG_PRESS -> {
                val vuid = params["vuid"] as? String
                val duration = (params["duration"] as? Number)?.toLong() ?: 500L
                if (vuid != null) longPress(vuid, duration) else ActionResult.Error("Missing vuid parameter")
            }
            CommandActionType.FOCUS -> {
                val vuid = params["vuid"] as? String
                if (vuid != null) focus(vuid) else ActionResult.Error("Missing vuid parameter")
            }
            CommandActionType.TEXT_INPUT -> {
                val text = params["text"] as? String ?: ""
                val vuid = params["vuid"] as? String
                enterText(text, vuid)
            }
            CommandActionType.SCROLL_UP -> scroll(ScrollDirection.UP)
            CommandActionType.SCROLL_DOWN -> scroll(ScrollDirection.DOWN)
            CommandActionType.SCROLL_LEFT -> scroll(ScrollDirection.LEFT)
            CommandActionType.SCROLL_RIGHT -> scroll(ScrollDirection.RIGHT)
            CommandActionType.BACK -> back()
            CommandActionType.HOME -> home()
            CommandActionType.RECENT_APPS -> recentApps()
            CommandActionType.APP_DRAWER -> appDrawer()
            CommandActionType.MEDIA_PLAY, CommandActionType.MEDIA_PAUSE -> mediaPlayPause()
            CommandActionType.MEDIA_NEXT -> mediaNext()
            CommandActionType.MEDIA_PREVIOUS -> mediaPrevious()
            CommandActionType.VOLUME_UP -> volume(VolumeDirection.UP)
            CommandActionType.VOLUME_DOWN -> volume(VolumeDirection.DOWN)
            CommandActionType.VOLUME_MUTE -> volume(VolumeDirection.MUTE)
            CommandActionType.OPEN_SETTINGS -> openSettings()
            CommandActionType.NOTIFICATIONS -> showNotifications()
            CommandActionType.SCREENSHOT -> screenshot()
            CommandActionType.FLASHLIGHT_ON -> flashlight(true)
            CommandActionType.FLASHLIGHT_OFF -> flashlight(false)
            CommandActionType.OPEN_APP -> {
                val appType = params["appType"] as? String
                if (appType != null) openApp(appType) else ActionResult.Error("Missing appType parameter")
            }
            CommandActionType.CLOSE_APP -> closeApp()
            else -> ActionResult.NotSupported(actionType)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Element Lookup
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun elementExists(vuid: String): Boolean {
        // Desktop doesn't have VUID system
        return false
    }

    override suspend fun getElementBounds(vuid: String): ElementBounds? {
        // Desktop doesn't have VUID system
        return null
    }
}
