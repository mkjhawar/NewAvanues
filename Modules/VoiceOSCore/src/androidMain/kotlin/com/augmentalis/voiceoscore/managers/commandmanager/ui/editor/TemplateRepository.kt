/**
 * TemplateRepository.kt - Pre-built command template repository
 *
 * Provides 15+ pre-built templates organized by category
 */

package com.augmentalis.voiceoscore.managers.commandmanager.ui.editor

import com.augmentalis.voiceoscore.managers.commandmanager.registry.ActionType

/**
 * Repository for command templates
 */
object TemplateRepository {

    /**
     * Get all available templates
     */
    fun getAllTemplates(): List<CommandTemplate> {
        return navigationTemplates() +
                textEditingTemplates() +
                systemTemplates() +
                appSpecificTemplates() +
                accessibilityTemplates() +
                mediaTemplates() +
                productivityTemplates()
    }

    /**
     * Get templates by category
     */
    fun getTemplatesByCategory(category: TemplateCategory): List<CommandTemplate> {
        return getAllTemplates().filter { it.category == category }
    }

    /**
     * Get template by ID
     */
    fun getTemplateById(id: String): CommandTemplate? {
        return getAllTemplates().find { it.id == id }
    }

    /**
     * Search templates
     */
    fun searchTemplates(query: String): List<CommandTemplate> {
        val normalizedQuery = query.lowercase().trim()
        if (normalizedQuery.isEmpty()) return getAllTemplates()

        return getAllTemplates().filter { template ->
            template.name.lowercase().contains(normalizedQuery) ||
                    template.description.lowercase().contains(normalizedQuery) ||
                    template.phrases.any { it.lowercase().contains(normalizedQuery) } ||
                    template.tags.any { it.lowercase().contains(normalizedQuery) }
        }
    }

    /**
     * Filter templates
     */
    fun filterTemplates(filter: TemplateFilter): List<CommandTemplate> {
        var templates = getAllTemplates()

        filter.category?.let { category ->
            templates = templates.filter { it.category == category }
        }

        filter.actionType?.let { type ->
            templates = templates.filter { it.actionType == type }
        }

        filter.tags.forEach { tag ->
            templates = templates.filter { it.tags.contains(tag) }
        }

        filter.searchQuery?.let { query ->
            templates = searchTemplates(query)
        }

        return templates
    }

    // Navigation Templates (5 templates)

    private fun navigationTemplates(): List<CommandTemplate> = listOf(
        commandTemplate {
            id("nav_back")
            name("Navigate Back")
            category(TemplateCategory.NAVIGATION)
            phrases("go back", "back", "previous", "return")
            actionType(ActionType.NAVIGATE)
            description("Navigate to the previous screen or page")
            defaultParam("action", "back")
            priority(70)
            namespace("navigation")
            exampleUsage("Say 'go back' to return to the previous screen")
            addTag("navigation")
            addTag("basic")
        },

        commandTemplate {
            id("nav_home")
            name("Go Home")
            category(TemplateCategory.NAVIGATION)
            phrases("go home", "home", "main screen", "home screen")
            actionType(ActionType.NAVIGATE)
            description("Navigate to the home screen")
            defaultParam("action", "home")
            priority(70)
            namespace("navigation")
            exampleUsage("Say 'go home' to return to the main screen")
            addTag("navigation")
            addTag("basic")
        },

        commandTemplate {
            id("nav_scroll_down")
            name("Scroll Down")
            category(TemplateCategory.NAVIGATION)
            phrases("scroll down", "page down", "down")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Scroll down the current view")
            defaultParam("direction", "down")
            defaultParam("amount", "page")
            priority(60)
            namespace("navigation")
            exampleUsage("Say 'scroll down' to move down the page")
            addTag("navigation")
            addTag("scrolling")
        },

        commandTemplate {
            id("nav_scroll_up")
            name("Scroll Up")
            category(TemplateCategory.NAVIGATION)
            phrases("scroll up", "page up", "up")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Scroll up the current view")
            defaultParam("direction", "up")
            defaultParam("amount", "page")
            priority(60)
            namespace("navigation")
            exampleUsage("Say 'scroll up' to move up the page")
            addTag("navigation")
            addTag("scrolling")
        },

        commandTemplate {
            id("nav_next")
            name("Next Item")
            category(TemplateCategory.NAVIGATION)
            phrases("next", "next item", "forward")
            actionType(ActionType.NAVIGATE)
            description("Move to the next item or screen")
            defaultParam("action", "next")
            priority(65)
            namespace("navigation")
            exampleUsage("Say 'next' to go to the next item")
            addTag("navigation")
        }
    )

    // Text Editing Templates (4 templates)

    private fun textEditingTemplates(): List<CommandTemplate> = listOf(
        commandTemplate {
            id("text_copy")
            name("Copy Text")
            category(TemplateCategory.TEXT_EDITING)
            phrases("copy", "copy text", "duplicate")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Copy selected text to clipboard")
            defaultParam("action", "copy")
            priority(75)
            namespace("text")
            exampleUsage("Say 'copy' to copy selected text")
            addTag("text")
            addTag("clipboard")
        },

        commandTemplate {
            id("text_paste")
            name("Paste Text")
            category(TemplateCategory.TEXT_EDITING)
            phrases("paste", "paste text", "insert")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Paste clipboard content")
            defaultParam("action", "paste")
            priority(75)
            namespace("text")
            exampleUsage("Say 'paste' to insert clipboard content")
            addTag("text")
            addTag("clipboard")
        },

        commandTemplate {
            id("text_select_all")
            name("Select All")
            category(TemplateCategory.TEXT_EDITING)
            phrases("select all", "select everything", "highlight all")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Select all text in the current field")
            defaultParam("action", "select_all")
            priority(70)
            namespace("text")
            exampleUsage("Say 'select all' to highlight all text")
            addTag("text")
            addTag("selection")
        },

        commandTemplate {
            id("text_delete")
            name("Delete Text")
            category(TemplateCategory.TEXT_EDITING)
            phrases("delete", "remove", "clear text")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Delete selected text or character")
            defaultParam("action", "delete")
            priority(70)
            namespace("text")
            exampleUsage("Say 'delete' to remove text")
            addTag("text")
        }
    )

    // System Templates (3 templates)

    private fun systemTemplates(): List<CommandTemplate> = listOf(
        commandTemplate {
            id("sys_volume_up")
            name("Volume Up")
            category(TemplateCategory.SYSTEM)
            phrases("volume up", "louder", "increase volume")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Increase system volume")
            defaultParam("action", "volume_up")
            defaultParam("amount", "1")
            priority(80)
            namespace("system")
            exampleUsage("Say 'volume up' to increase volume")
            addTag("system")
            addTag("audio")
        },

        commandTemplate {
            id("sys_volume_down")
            name("Volume Down")
            category(TemplateCategory.SYSTEM)
            phrases("volume down", "quieter", "decrease volume")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Decrease system volume")
            defaultParam("action", "volume_down")
            defaultParam("amount", "1")
            priority(80)
            namespace("system")
            exampleUsage("Say 'volume down' to decrease volume")
            addTag("system")
            addTag("audio")
        },

        commandTemplate {
            id("sys_brightness")
            name("Adjust Brightness")
            category(TemplateCategory.SYSTEM)
            phrases("brightness up", "brightness down", "adjust brightness")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Adjust screen brightness")
            defaultParam("action", "brightness")
            priority(70)
            namespace("system")
            exampleUsage("Say 'brightness up' to increase screen brightness")
            addTag("system")
            addTag("display")
        }
    )

    // App-Specific Templates (3 templates)

    private fun appSpecificTemplates(): List<CommandTemplate> = listOf(
        commandTemplate {
            id("app_open_generic")
            name("Open App")
            category(TemplateCategory.APP_SPECIFIC)
            phrases("open {app}", "launch {app}", "start {app}")
            actionType(ActionType.LAUNCH_APP)
            description("Open a specified application")
            defaultParam("package", "")
            priority(75)
            namespace("apps")
            exampleUsage("Say 'open calculator' to launch the calculator app")
            addTag("apps")
            addTag("launcher")
        },

        commandTemplate {
            id("app_close")
            name("Close App")
            category(TemplateCategory.APP_SPECIFIC)
            phrases("close app", "exit app", "quit")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Close the current application")
            defaultParam("action", "close_app")
            priority(70)
            namespace("apps")
            exampleUsage("Say 'close app' to exit current application")
            addTag("apps")
        },

        commandTemplate {
            id("app_switch")
            name("Switch App")
            category(TemplateCategory.APP_SPECIFIC)
            phrases("switch app", "recent apps", "app switcher")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Open the app switcher")
            defaultParam("action", "recent_apps")
            priority(75)
            namespace("apps")
            exampleUsage("Say 'switch app' to see recent applications")
            addTag("apps")
            addTag("multitasking")
        }
    )

    // Accessibility Templates (3 templates)

    private fun accessibilityTemplates(): List<CommandTemplate> = listOf(
        commandTemplate {
            id("acc_read_screen")
            name("Read Screen")
            category(TemplateCategory.ACCESSIBILITY)
            phrases("read screen", "read page", "read aloud")
            actionType(ActionType.ACCESSIBILITY)
            description("Read the current screen content aloud")
            defaultParam("action", "read_screen")
            priority(80)
            namespace("accessibility")
            exampleUsage("Say 'read screen' to hear the page content")
            addTag("accessibility")
            addTag("tts")
        },

        commandTemplate {
            id("acc_zoom_in")
            name("Zoom In")
            category(TemplateCategory.ACCESSIBILITY)
            phrases("zoom in", "magnify", "enlarge")
            actionType(ActionType.ACCESSIBILITY)
            description("Zoom in on screen content")
            defaultParam("action", "zoom_in")
            defaultParam("level", "1")
            priority(75)
            namespace("accessibility")
            exampleUsage("Say 'zoom in' to magnify screen content")
            addTag("accessibility")
            addTag("vision")
        },

        commandTemplate {
            id("acc_zoom_out")
            name("Zoom Out")
            category(TemplateCategory.ACCESSIBILITY)
            phrases("zoom out", "reduce", "shrink")
            actionType(ActionType.ACCESSIBILITY)
            description("Zoom out from screen content")
            defaultParam("action", "zoom_out")
            defaultParam("level", "1")
            priority(75)
            namespace("accessibility")
            exampleUsage("Say 'zoom out' to reduce magnification")
            addTag("accessibility")
            addTag("vision")
        }
    )

    // Media Templates (2 templates)

    private fun mediaTemplates(): List<CommandTemplate> = listOf(
        commandTemplate {
            id("media_play_pause")
            name("Play/Pause")
            category(TemplateCategory.MEDIA)
            phrases("play", "pause", "play pause")
            actionType(ActionType.MEDIA_CONTROL)
            description("Toggle media playback")
            defaultParam("action", "play_pause")
            priority(85)
            namespace("media")
            exampleUsage("Say 'play' or 'pause' to control media")
            addTag("media")
            addTag("playback")
        },

        commandTemplate {
            id("media_next_track")
            name("Next Track")
            category(TemplateCategory.MEDIA)
            phrases("next track", "next song", "skip")
            actionType(ActionType.MEDIA_CONTROL)
            description("Skip to next media track")
            defaultParam("action", "next_track")
            priority(80)
            namespace("media")
            exampleUsage("Say 'next track' to skip to next song")
            addTag("media")
            addTag("playback")
        }
    )

    // Productivity Templates (3 templates)

    private fun productivityTemplates(): List<CommandTemplate> = listOf(
        commandTemplate {
            id("prod_search")
            name("Search")
            category(TemplateCategory.PRODUCTIVITY)
            phrases("search for {query}", "find {query}", "look for {query}")
            actionType(ActionType.CUSTOM_ACTION)
            description("Search for specified content")
            defaultParam("action", "search")
            defaultParam("query", "")
            priority(70)
            namespace("productivity")
            exampleUsage("Say 'search for recipes' to find content")
            addTag("productivity")
            addTag("search")
        },

        commandTemplate {
            id("prod_screenshot")
            name("Take Screenshot")
            category(TemplateCategory.PRODUCTIVITY)
            phrases("screenshot", "take screenshot", "capture screen")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Capture current screen")
            defaultParam("action", "screenshot")
            priority(75)
            namespace("productivity")
            exampleUsage("Say 'screenshot' to capture the screen")
            addTag("productivity")
            addTag("capture")
        },

        commandTemplate {
            id("prod_share")
            name("Share")
            category(TemplateCategory.PRODUCTIVITY)
            phrases("share", "send", "share this")
            actionType(ActionType.SYSTEM_COMMAND)
            description("Open share dialog for current content")
            defaultParam("action", "share")
            priority(70)
            namespace("productivity")
            exampleUsage("Say 'share' to send content to others")
            addTag("productivity")
            addTag("sharing")
        }
    )

    /**
     * Get template collections (grouped by category)
     */
    fun getTemplateCollections(): List<TemplateCollection> {
        return TemplateCategory.values().map { category ->
            TemplateCollection(
                name = category.displayName,
                description = getCategoryDescription(category),
                templates = getTemplatesByCategory(category),
                category = category
            )
        }.filter { it.templates.isNotEmpty() }
    }

    private fun getCategoryDescription(category: TemplateCategory): String {
        return when (category) {
            TemplateCategory.NAVIGATION -> "Commands for navigating through apps and screens"
            TemplateCategory.TEXT_EDITING -> "Commands for text manipulation and editing"
            TemplateCategory.SYSTEM -> "System-level commands for device control"
            TemplateCategory.APP_SPECIFIC -> "Commands for launching and managing apps"
            TemplateCategory.ACCESSIBILITY -> "Accessibility-focused commands"
            TemplateCategory.MEDIA -> "Media playback and control commands"
            TemplateCategory.PRODUCTIVITY -> "Productivity and workflow commands"
            TemplateCategory.COMMUNICATION -> "Communication and messaging commands"
            TemplateCategory.CUSTOM -> "Custom user-defined commands"
        }
    }
}
