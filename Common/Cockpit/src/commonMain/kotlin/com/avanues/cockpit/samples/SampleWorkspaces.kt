package com.avanues.cockpit.samples

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowType
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.core.workspace.Workspace
import java.util.UUID

/**
 * Sample Workspaces for MVP Testing
 *
 * Pre-configured workspaces demonstrating various use cases.
 * Used for demos, screenshots, and initial testing.
 *
 * **Voice Commands:**
 * - "Load work setup" → Productivity workspace
 * - "Load media center" → Entertainment workspace
 * - "Load development" → Coding workspace
 */
object SampleWorkspaces {

    /**
     * Productivity workspace
     *
     * **Scenario:** Working professional managing email, docs, and communication
     *
     * **Windows:**
     * 1. Gmail (email)
     * 2. Google Docs (document editing)
     * 3. Slack (team chat)
     * 4. Google Calendar (scheduling)
     * 5. Calculator (quick math)
     *
     * **Layout:** LINEAR_HORIZONTAL (default, macOS style)
     *
     * **Voice Commands:**
     * - "Open work setup"
     * - "Focus email"
     * - "Switch to docs"
     * - "Check calendar"
     */
    val WORK_SETUP = Workspace(
        id = UUID.randomUUID().toString(),
        name = "Work Setup",
        voiceName = "work",
        layoutPresetId = "LINEAR_HORIZONTAL",
        centerPoint = Vector3D(0f, 0f, -2f),
        windows = listOf(
            // Gmail - Email management
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Gmail",
                url = "https://mail.google.com",
                position = Vector3D(-0.8f, 0f, -2f),
                voiceName = "email"
            ),

            // Google Docs - Document editing
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Google Docs",
                url = "https://docs.google.com",
                position = Vector3D(-0.4f, 0f, -2f),
                voiceName = "docs"
            ),

            // Slack - Team communication
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Slack",
                url = "https://app.slack.com",
                position = Vector3D(0f, 0f, -1.9f), // Slightly forward (center)
                voiceName = "slack"
            ),

            // Google Calendar - Scheduling
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Google Calendar",
                url = "https://calendar.google.com",
                position = Vector3D(0.4f, 0f, -2f),
                voiceName = "calendar"
            ),

            // Calculator widget
            AppWindow.widget(
                id = UUID.randomUUID().toString(),
                title = "Calculator",
                widgetType = "calculator",
                position = Vector3D(0.8f, 0f, -2f),
                widthMeters = 0.4f,
                heightMeters = 0.5f,
                voiceName = "calculator"
            )
        )
    )

    /**
     * Entertainment workspace
     *
     * **Scenario:** Relaxing with media, music, and social content
     *
     * **Windows:**
     * 1. YouTube (video streaming)
     * 2. Spotify Web Player (music)
     * 3. Twitter/X (social media)
     *
     * **Layout:** THEATER (large center window + side panels)
     *
     * **Voice Commands:**
     * - "Open media center"
     * - "Play video"
     * - "Next song"
     * - "Check Twitter"
     */
    val MEDIA_CENTER = Workspace(
        id = UUID.randomUUID().toString(),
        name = "Media Center",
        voiceName = "media",
        layoutPresetId = "THEATER", // Will be implemented in Phase 4
        centerPoint = Vector3D(0f, 0f, -2.5f), // Slightly further back
        windows = listOf(
            // YouTube - Main video window (large)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "YouTube",
                url = "https://www.youtube.com",
                position = Vector3D(0f, 0f, -2.5f),
                voiceName = "youtube"
            ).copy(
                widthMeters = 1.6f,  // Double width for theater mode
                heightMeters = 0.9f
            ),

            // Spotify - Music player (side panel)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Spotify",
                url = "https://open.spotify.com",
                position = Vector3D(1.2f, 0f, -2.5f),
                voiceName = "music"
            ).copy(
                widthMeters = 0.5f,
                heightMeters = 0.7f
            ),

            // Twitter/X - Social feed (side panel)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Twitter",
                url = "https://twitter.com",
                position = Vector3D(-1.2f, 0f, -2.5f),
                voiceName = "twitter"
            ).copy(
                widthMeters = 0.5f,
                heightMeters = 0.7f
            )
        )
    )

    /**
     * Development workspace
     *
     * **Scenario:** Software developer with code editor, docs, and terminal
     *
     * **Windows:**
     * 1. GitHub (code repository)
     * 2. Stack Overflow (Q&A reference)
     * 3. MDN Docs (web documentation)
     * 4. CodePen (live code testing)
     *
     * **Layout:** GRID_2x2 (4 evenly sized windows)
     *
     * **Voice Commands:**
     * - "Open development"
     * - "Focus GitHub"
     * - "Search Stack Overflow"
     * - "Open docs"
     */
    val DEVELOPMENT = Workspace(
        id = UUID.randomUUID().toString(),
        name = "Development",
        voiceName = "development",
        layoutPresetId = "GRID_2x2", // Will be implemented in Phase 4
        centerPoint = Vector3D(0f, 0f, -2f),
        windows = listOf(
            // GitHub - Code repository (top-left)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "GitHub",
                url = "https://github.com",
                position = Vector3D(-0.5f, 0.4f, -2f),
                voiceName = "github"
            ),

            // Stack Overflow - Q&A (top-right)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Stack Overflow",
                url = "https://stackoverflow.com",
                position = Vector3D(0.5f, 0.4f, -2f),
                voiceName = "stackoverflow"
            ),

            // MDN Docs - Web documentation (bottom-left)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "MDN Web Docs",
                url = "https://developer.mozilla.org",
                position = Vector3D(-0.5f, -0.4f, -2f),
                voiceName = "docs"
            ),

            // CodePen - Live code testing (bottom-right)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "CodePen",
                url = "https://codepen.io",
                position = Vector3D(0.5f, -0.4f, -2f),
                voiceName = "codepen"
            )
        )
    )

    /**
     * Reading workspace
     *
     * **Scenario:** Focused reading with news, articles, and notes
     *
     * **Windows:**
     * 1. Medium (article reading)
     * 2. Pocket (saved articles)
     * 3. Google Keep (notes)
     *
     * **Layout:** STACK_CENTER (main window + background stack)
     *
     * **Voice Commands:**
     * - "Open reading"
     * - "Next article"
     * - "Take note"
     */
    val READING = Workspace(
        id = UUID.randomUUID().toString(),
        name = "Reading",
        voiceName = "reading",
        layoutPresetId = "STACK_CENTER", // Will be implemented in Phase 4
        centerPoint = Vector3D(0f, 0f, -2f),
        windows = listOf(
            // Medium - Main article (foreground)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Medium",
                url = "https://medium.com",
                position = Vector3D(0f, 0f, -1.8f), // Closer (active)
                voiceName = "medium"
            ),

            // Pocket - Saved articles (background)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Pocket",
                url = "https://getpocket.com",
                position = Vector3D(0.1f, 0.1f, -2.2f), // Further back
                voiceName = "pocket"
            ),

            // Google Keep - Notes (background)
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Google Keep",
                url = "https://keep.google.com",
                position = Vector3D(-0.1f, -0.1f, -2.3f), // Further back
                voiceName = "notes"
            )
        )
    )

    /**
     * Minimal browser workspace (MVP testing)
     *
     * **Scenario:** Simple 2-3 window setup for MVP testing
     *
     * **Windows:**
     * 1. Google (search)
     * 2. GitHub (code)
     * 3. Calculator (widget)
     *
     * **Layout:** LINEAR_HORIZONTAL
     *
     * **Purpose:** Minimal setup for testing basic window management
     */
    val MINIMAL_BROWSER = Workspace(
        id = UUID.randomUUID().toString(),
        name = "Minimal Browser",
        voiceName = "browser",
        layoutPresetId = "LINEAR_HORIZONTAL",
        centerPoint = Vector3D(0f, 0f, -2f),
        windows = listOf(
            // Google Search
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "Google",
                url = "https://www.google.com",
                position = Vector3D(-0.4f, 0f, -2f),
                voiceName = "google"
            ),

            // GitHub
            AppWindow.webApp(
                id = UUID.randomUUID().toString(),
                title = "GitHub",
                url = "https://github.com",
                position = Vector3D(0f, 0f, -1.9f), // Center, slightly forward
                voiceName = "github"
            ),

            // Calculator widget
            AppWindow.widget(
                id = UUID.randomUUID().toString(),
                title = "Calculator",
                widgetType = "calculator",
                position = Vector3D(0.4f, 0f, -2f),
                widthMeters = 0.4f,
                heightMeters = 0.5f,
                voiceName = "calculator"
            )
        )
    )

    /**
     * All sample workspaces
     */
    val ALL = listOf(
        WORK_SETUP,
        MEDIA_CENTER,
        DEVELOPMENT,
        READING,
        MINIMAL_BROWSER
    )

    /**
     * Gets a workspace by voice name
     *
     * Voice command: "Load [voiceName]"
     *
     * @param voiceName Voice-friendly name (e.g., "work", "media")
     * @return Workspace or null if not found
     */
    fun getByVoiceName(voiceName: String): Workspace? {
        return ALL.find { it.voiceName.equals(voiceName, ignoreCase = true) }
    }

    /**
     * Gets a workspace by ID
     *
     * @param id Workspace ID
     * @return Workspace or null if not found
     */
    fun getById(id: String): Workspace? {
        return ALL.find { it.id == id }
    }
}
