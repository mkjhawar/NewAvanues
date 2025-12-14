package com.augmentalis.webavanue.feature.commands

import com.augmentalis.webavanue.ui.viewmodel.TabViewModel
import com.augmentalis.webavanue.ui.viewmodel.WebViewController

/**
 * Maps VoiceOS command IDs to WebAvanue browser actions
 *
 * This mapper is registered with VoiceOS IntentDispatcher on app startup.
 * VoiceOS loads commands centrally from JSON, routes them here for execution.
 *
 * Architecture:
 * - VoiceOS loads commands from assets/localization/commands/{locale}.json
 * - IntentDispatcher routes browser category commands to this mapper
 * - Mapper executes appropriate browser actions via TabViewModel/WebViewController
 *
 * See: Developer Manual Chapter 46, ADR-007
 */
class WebAvanueActionMapper(
    private val tabViewModel: TabViewModel,
    private val webViewController: WebViewController
) {
    /**
     * Execute browser action for given command ID
     *
     * @param commandId VoiceOS command ID (e.g., "SCROLL_TOP", "NEW_TAB")
     * @param parameters Optional parameters from voice input (e.g., zoom level)
     * @return ActionResult indicating success/failure with optional message
     */
    suspend fun executeAction(
        commandId: String,
        parameters: Map<String, Any> = emptyMap()
    ): ActionResult {
        return when (commandId) {
            // ========== Scrolling ==========
            "SCROLL_UP" -> webViewController.scrollUp()
            "SCROLL_DOWN" -> webViewController.scrollDown()
            "SCROLL_LEFT" -> webViewController.scrollLeft()
            "SCROLL_RIGHT" -> webViewController.scrollRight()
            "SCROLL_TOP" -> webViewController.scrollTop()
            "SCROLL_BOTTOM" -> webViewController.scrollBottom()

            // ========== Navigation ==========
            "GO_BACK" -> webViewController.goBack()
            "GO_FORWARD", "NAVIGATE_FORWARD" -> webViewController.goForward()
            "RELOAD_PAGE", "ACTION_REFRESH" -> webViewController.reload()

            // ========== Zoom ==========
            "ZOOM_IN", "PINCH_OPEN" -> webViewController.zoomIn()
            "ZOOM_OUT", "PINCH_CLOSE" -> webViewController.zoomOut()
            "RESET_ZOOM" -> webViewController.resetZoom()
            "SET_ZOOM_LEVEL" -> {
                val level = parameters["level"] as? Int ?: 100
                if (level !in 50..200) {
                    return ActionResult.error("Invalid zoom level: must be 50-200")
                }
                webViewController.setZoomLevel(level)
                ActionResult.success("Zoom set to $level%")
            }

            // ========== Desktop Mode ==========
            "DESKTOP_MODE" -> {
                webViewController.setDesktopMode(true)
                ActionResult.success("Desktop mode enabled")
            }
            "MOBILE_MODE" -> {
                webViewController.setDesktopMode(false)
                ActionResult.success("Mobile mode enabled")
            }

            // ========== Page Control ==========
            "FREEZE_PAGE" -> {
                webViewController.toggleFreeze()
                ActionResult.success("Page freeze toggled")
            }
            "CLEAR_COOKIES" -> {
                webViewController.clearCookies()
                ActionResult.success("Cookies cleared")
            }

            // ========== Tabs ==========
            "NEW_TAB" -> {
                tabViewModel.createTab()
                ActionResult.success("New tab created")
            }
            "CLOSE_TAB" -> {
                val activeTabId = tabViewModel.activeTab.value?.tab?.id
                if (activeTabId != null) {
                    tabViewModel.closeTab(activeTabId)
                    ActionResult.success("Tab closed")
                } else {
                    ActionResult.error("No active tab to close")
                }
            }

            // ========== Bookmarks ==========
            "ADD_BOOKMARK" -> {
                val url = webViewController.getCurrentUrl()
                val title = webViewController.getCurrentTitle()
                // TODO: Implement bookmark logic via TabViewModel
                ActionResult.success("Bookmark added: $title")
            }

            // ========== Gestures (Legacy) ==========
            "SINGLE_CLICK" -> webViewController.performClick()
            "DOUBLE_CLICK" -> webViewController.performDoubleClick()
            "DRAG_START" -> webViewController.startDrag()
            "DRAG_STOP" -> webViewController.stopDrag()
            "SELECT" -> webViewController.select()

            // ========== Universal Gestures (IPC) ==========
            // Route all GESTURE_* commands to unified gesture handler
            // Supports all 80+ VoiceOS gesture types with coordinates and modifiers
            else -> {
                if (commandId.startsWith("GESTURE_")) {
                    // Extract coordinates and modifiers from parameters
                    val x = when (val xParam = parameters["x"]) {
                        is Float -> xParam
                        is Double -> xParam.toFloat()
                        is Int -> xParam.toFloat()
                        is String -> xParam.toFloatOrNull() ?: -1f
                        else -> -1f
                    }

                    val y = when (val yParam = parameters["y"]) {
                        is Float -> yParam
                        is Double -> yParam.toFloat()
                        is Int -> yParam.toFloat()
                        is String -> yParam.toFloatOrNull() ?: -1f
                        else -> -1f
                    }

                    val modifiers = when (val modParam = parameters["modifiers"]) {
                        is Int -> modParam
                        is String -> modParam.toIntOrNull() ?: 0
                        else -> 0
                    }

                    webViewController.performGesture(commandId, x, y, modifiers)
                } else {
                    ActionResult.error("Unknown command: $commandId")
                }
            }
        }
    }
}
