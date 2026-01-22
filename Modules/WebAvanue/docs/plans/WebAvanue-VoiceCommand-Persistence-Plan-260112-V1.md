# WebAvanue Voice Command Persistence Implementation Plan

**Date:** 2026-01-12
**Version:** 1.0
**Status:** Ready for Implementation
**Analysis Method:** Chain of Thought (.cot) + Swarm Analysis

---

## Executive Summary

WebAvanue successfully scrapes interactive elements from web pages and generates voice commands, but these commands are **stored in-memory only** (ArrayList in VoiceCommandGenerator.kt) and lost on navigation. This plan addresses:

1. **Persistent storage** for web voice commands
2. **Webapp whitelist** for user-designated sites to save to database
3. **Action verb support** (click, tap, long press, right-click)
4. **Platform-aware database schema** distinguishing web vs native elements

---

## Chain of Thought Analysis

### Problem Statement

```
User navigates to gmail.com
    → DOM scraped successfully (26 elements)
    → 9 voice commands generated
    → Commands stored in ArrayList (memory only)
    → User navigates away
    → ❌ ALL COMMANDS LOST
    → User returns to gmail.com
    → Must rescan entirely (no learning)
```

### Root Cause Analysis

1. **No persistence hook**: `BrowserVoiceOSCallback.onDOMScraped()` only stores in memory
2. **No domain tracking**: Commands have no `appId` equivalent for web
3. **No whitelist system**: No way to mark sites for persistent storage
4. **Missing action verbs**: Database stores `actionType` but web commands don't capture available actions per element

### Proposed Solution Flow

```
User navigates to gmail.com
    → Check if domain in whitelist
    → If YES (whitelisted):
        → Load existing commands from database
        → Merge with fresh DOM scan
        → Persist new/updated commands
        → Track usage statistics
    → If NO (not whitelisted):
        → Generate commands in memory only
        → Discard on navigation (current behavior)
        → No database storage (saves space)
```

---

## Implementation Tasks

### Phase 1: Database Schema Updates

#### Task 1.1: Add Web Commands Table
**File:** `Common/VoiceOS/database/src/commonMain/sqldelight/com/augmentalis/database/ScrapedWebCommand.sq`

```sql
CREATE TABLE scraped_web_command (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Element identification
    element_hash TEXT NOT NULL,
    domain_id TEXT NOT NULL,              -- e.g., "mail.google.com"
    url_pattern TEXT,                     -- e.g., "/inbox*" (optional, for page-specific)

    -- Selectors for execution
    css_selector TEXT NOT NULL,
    xpath TEXT,

    -- Command data
    command_text TEXT NOT NULL,
    element_text TEXT,                    -- Visible text snapshot
    element_tag TEXT NOT NULL,            -- button, a, input, etc.
    element_type TEXT NOT NULL,           -- button, link, input, dropdown, etc.

    -- Actions allowed (JSON array: ["click", "long_press", "right_click"])
    allowed_actions TEXT NOT NULL DEFAULT '["click"]',
    primary_action TEXT NOT NULL DEFAULT 'click',

    -- Confidence and approval
    confidence REAL NOT NULL DEFAULT 0.5,
    is_user_approved INTEGER NOT NULL DEFAULT 0,
    user_approved_at INTEGER,

    -- Synonyms (JSON array)
    synonyms TEXT,

    -- Usage tracking
    usage_count INTEGER NOT NULL DEFAULT 0,
    last_used INTEGER,

    -- Lifecycle
    created_at INTEGER NOT NULL,
    last_verified INTEGER,
    is_deprecated INTEGER NOT NULL DEFAULT 0,

    -- Position for overlay
    bound_left INTEGER,
    bound_top INTEGER,
    bound_width INTEGER,
    bound_height INTEGER,

    UNIQUE(element_hash, domain_id, primary_action)
);

-- Indexes for performance
CREATE INDEX idx_swc_domain ON scraped_web_command(domain_id);
CREATE INDEX idx_swc_selector ON scraped_web_command(css_selector);
CREATE INDEX idx_swc_confidence ON scraped_web_command(confidence);
CREATE INDEX idx_swc_usage ON scraped_web_command(usage_count DESC);
CREATE INDEX idx_swc_deprecated ON scraped_web_command(is_deprecated, last_verified);

-- Queries
selectByDomain:
SELECT * FROM scraped_web_command WHERE domain_id = ? AND is_deprecated = 0 ORDER BY confidence DESC;

selectByDomainAndUrl:
SELECT * FROM scraped_web_command WHERE domain_id = ? AND (url_pattern IS NULL OR ? LIKE url_pattern) AND is_deprecated = 0;

selectHighConfidence:
SELECT * FROM scraped_web_command WHERE domain_id = ? AND confidence >= ? AND is_deprecated = 0;

insertCommand:
INSERT OR REPLACE INTO scraped_web_command (
    element_hash, domain_id, url_pattern, css_selector, xpath,
    command_text, element_text, element_tag, element_type,
    allowed_actions, primary_action, confidence,
    created_at, last_verified
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

incrementUsage:
UPDATE scraped_web_command SET usage_count = usage_count + 1, last_used = ? WHERE id = ?;

markDeprecated:
UPDATE scraped_web_command SET is_deprecated = 1 WHERE domain_id = ? AND last_verified < ?;

deleteDeprecated:
DELETE FROM scraped_web_command WHERE is_deprecated = 1 AND last_verified < ?;
```

#### Task 1.2: Add Webapp Whitelist Table
**File:** `Common/VoiceOS/database/src/commonMain/sqldelight/com/augmentalis/database/WebAppWhitelist.sq`

```sql
CREATE TABLE web_app_whitelist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    domain_id TEXT NOT NULL UNIQUE,       -- e.g., "mail.google.com"
    display_name TEXT NOT NULL,           -- e.g., "Gmail"
    base_url TEXT,                        -- e.g., "https://mail.google.com"
    category TEXT,                        -- email, social, productivity, etc.

    -- Settings
    is_enabled INTEGER NOT NULL DEFAULT 1,
    auto_scan INTEGER NOT NULL DEFAULT 1,
    save_commands INTEGER NOT NULL DEFAULT 1,

    -- Statistics
    command_count INTEGER NOT NULL DEFAULT 0,
    last_visited INTEGER,
    visit_count INTEGER NOT NULL DEFAULT 0,

    -- Metadata
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX idx_whitelist_enabled ON web_app_whitelist(is_enabled);

-- Queries
selectAll:
SELECT * FROM web_app_whitelist ORDER BY last_visited DESC;

selectEnabled:
SELECT * FROM web_app_whitelist WHERE is_enabled = 1;

selectByDomain:
SELECT * FROM web_app_whitelist WHERE domain_id = ?;

isWhitelisted:
SELECT COUNT(*) > 0 FROM web_app_whitelist WHERE domain_id = ? AND is_enabled = 1 AND save_commands = 1;

insertOrUpdate:
INSERT OR REPLACE INTO web_app_whitelist (
    domain_id, display_name, base_url, category,
    is_enabled, auto_scan, save_commands,
    command_count, last_visited, visit_count,
    created_at, updated_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

updateVisit:
UPDATE web_app_whitelist SET last_visited = ?, visit_count = visit_count + 1, updated_at = ? WHERE domain_id = ?;

updateCommandCount:
UPDATE web_app_whitelist SET command_count = ?, updated_at = ? WHERE domain_id = ?;

delete:
DELETE FROM web_app_whitelist WHERE domain_id = ?;
```

#### Task 1.3: Add Platform Field to Existing Tables
**File:** `Common/VoiceOS/database/src/commonMain/sqldelight/com/augmentalis/database/ScrapedElement.sq`

Add migration:
```sql
ALTER TABLE scraped_element ADD COLUMN platform TEXT NOT NULL DEFAULT 'ANDROID';
ALTER TABLE scraped_element ADD COLUMN css_selector TEXT;
ALTER TABLE scraped_element ADD COLUMN xpath TEXT;
ALTER TABLE scraped_element ADD COLUMN html_tag TEXT;
ALTER TABLE scraped_element ADD COLUMN allowed_actions TEXT DEFAULT '["click"]';
```

---

### Phase 2: DTOs and Repositories

#### Task 2.1: Create Web Command DTO
**File:** `Common/VoiceOS/database/src/commonMain/kotlin/com/augmentalis/database/dto/ScrapedWebCommandDTO.kt`

```kotlin
package com.augmentalis.database.dto

data class ScrapedWebCommandDTO(
    val id: Long = 0,
    val elementHash: String,
    val domainId: String,
    val urlPattern: String? = null,
    val cssSelector: String,
    val xpath: String? = null,
    val commandText: String,
    val elementText: String? = null,
    val elementTag: String,
    val elementType: String,
    val allowedActions: List<String> = listOf("click"),
    val primaryAction: String = "click",
    val confidence: Float = 0.5f,
    val isUserApproved: Boolean = false,
    val userApprovedAt: Long? = null,
    val synonyms: List<String>? = null,
    val usageCount: Int = 0,
    val lastUsed: Long? = null,
    val createdAt: Long,
    val lastVerified: Long? = null,
    val isDeprecated: Boolean = false,
    val boundLeft: Int? = null,
    val boundTop: Int? = null,
    val boundWidth: Int? = null,
    val boundHeight: Int? = null
)
```

#### Task 2.2: Create Whitelist DTO
**File:** `Common/VoiceOS/database/src/commonMain/kotlin/com/augmentalis/database/dto/WebAppWhitelistDTO.kt`

```kotlin
package com.augmentalis.database.dto

data class WebAppWhitelistDTO(
    val id: Long = 0,
    val domainId: String,
    val displayName: String,
    val baseUrl: String? = null,
    val category: String? = null,
    val isEnabled: Boolean = true,
    val autoScan: Boolean = true,
    val saveCommands: Boolean = true,
    val commandCount: Int = 0,
    val lastVisited: Long? = null,
    val visitCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### Task 2.3: Create Repository Interfaces
**File:** `Common/VoiceOS/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IScrapedWebCommandRepository.kt`

```kotlin
package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ScrapedWebCommandDTO

interface IScrapedWebCommandRepository {
    suspend fun getByDomain(domainId: String): List<ScrapedWebCommandDTO>
    suspend fun getByDomainAndUrl(domainId: String, url: String): List<ScrapedWebCommandDTO>
    suspend fun getHighConfidence(domainId: String, minConfidence: Float): List<ScrapedWebCommandDTO>
    suspend fun insert(command: ScrapedWebCommandDTO): Long
    suspend fun insertBatch(commands: List<ScrapedWebCommandDTO>)
    suspend fun incrementUsage(id: Long)
    suspend fun markDeprecated(domainId: String, olderThan: Long)
    suspend fun deleteDeprecated(olderThan: Long): Int
    suspend fun getCommandCount(domainId: String): Int
}
```

**File:** `Common/VoiceOS/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IWebAppWhitelistRepository.kt`

```kotlin
package com.augmentalis.database.repositories

import com.augmentalis.database.dto.WebAppWhitelistDTO

interface IWebAppWhitelistRepository {
    suspend fun getAll(): List<WebAppWhitelistDTO>
    suspend fun getEnabled(): List<WebAppWhitelistDTO>
    suspend fun getByDomain(domainId: String): WebAppWhitelistDTO?
    suspend fun isWhitelisted(domainId: String): Boolean
    suspend fun insertOrUpdate(entry: WebAppWhitelistDTO)
    suspend fun updateVisit(domainId: String)
    suspend fun updateCommandCount(domainId: String, count: Int)
    suspend fun delete(domainId: String)
}
```

---

### Phase 3: Action Verb Support

#### Task 3.1: Define Allowed Actions per Element Type
**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/webavanue/voiceos/WebElementActions.kt`

```kotlin
package com.augmentalis.webavanue.voiceos

/**
 * Defines allowed voice actions for web elements.
 * Maps element types to their supported actions.
 */
object WebElementActions {

    /**
     * Action verbs that users can speak.
     */
    enum class ActionVerb(val aliases: List<String>) {
        CLICK(listOf("click", "tap", "press", "select", "activate")),
        LONG_PRESS(listOf("long press", "hold", "long click", "press and hold")),
        RIGHT_CLICK(listOf("right click", "context menu", "secondary click")),
        DOUBLE_CLICK(listOf("double click", "double tap")),
        FOCUS(listOf("focus", "go to", "move to")),
        TYPE(listOf("type", "enter", "input", "write")),
        CLEAR(listOf("clear", "delete", "erase", "remove text")),
        SCROLL_TO(listOf("scroll to", "show", "reveal")),
        CHECK(listOf("check", "enable", "turn on", "select")),
        UNCHECK(listOf("uncheck", "disable", "turn off", "deselect")),
        EXPAND(listOf("expand", "open", "show more")),
        COLLAPSE(listOf("collapse", "close", "show less"));

        companion object {
            fun fromPhrase(phrase: String): ActionVerb? {
                val normalized = phrase.lowercase().trim()
                return entries.find { action ->
                    action.aliases.any { alias -> normalized.startsWith(alias) }
                }
            }
        }
    }

    /**
     * Get allowed actions for an element based on its type and attributes.
     */
    fun getAllowedActions(
        elementType: String,
        tag: String,
        isClickable: Boolean = true,
        isEditable: Boolean = false,
        isCheckable: Boolean = false,
        isExpandable: Boolean = false
    ): List<ActionVerb> {
        val actions = mutableListOf<ActionVerb>()

        // Base actions for all interactive elements
        if (isClickable) {
            actions.add(ActionVerb.CLICK)
            actions.add(ActionVerb.LONG_PRESS)
            actions.add(ActionVerb.RIGHT_CLICK)
        }

        // Type-specific actions
        when (elementType.lowercase()) {
            "button" -> {
                actions.add(ActionVerb.DOUBLE_CLICK)
            }
            "link" -> {
                actions.add(ActionVerb.DOUBLE_CLICK)
            }
            "input" -> {
                actions.add(ActionVerb.FOCUS)
                if (isEditable) {
                    actions.add(ActionVerb.TYPE)
                    actions.add(ActionVerb.CLEAR)
                }
            }
            "checkbox", "radio" -> {
                if (isCheckable) {
                    actions.add(ActionVerb.CHECK)
                    actions.add(ActionVerb.UNCHECK)
                }
            }
            "dropdown", "select" -> {
                actions.add(ActionVerb.EXPAND)
                actions.add(ActionVerb.FOCUS)
            }
            "tab" -> {
                actions.add(ActionVerb.FOCUS)
            }
            "menuitem" -> {
                actions.add(ActionVerb.FOCUS)
            }
        }

        // Scroll-to for any element
        actions.add(ActionVerb.SCROLL_TO)

        // Expandable elements
        if (isExpandable) {
            actions.add(ActionVerb.EXPAND)
            actions.add(ActionVerb.COLLAPSE)
        }

        return actions.distinct()
    }

    /**
     * Get the primary (default) action for an element type.
     */
    fun getPrimaryAction(elementType: String): ActionVerb {
        return when (elementType.lowercase()) {
            "input", "textarea" -> ActionVerb.FOCUS
            "checkbox", "radio" -> ActionVerb.CLICK  // Toggle state
            else -> ActionVerb.CLICK
        }
    }

    /**
     * Convert actions list to JSON string for database storage.
     */
    fun toJson(actions: List<ActionVerb>): String {
        return actions.joinToString(prefix = "[", postfix = "]") { "\"${it.name.lowercase()}\"" }
    }

    /**
     * Parse actions from JSON string.
     */
    fun fromJson(json: String): List<ActionVerb> {
        return json
            .removeSurrounding("[", "]")
            .split(",")
            .mapNotNull { it.trim().removeSurrounding("\"").uppercase().let { name ->
                try { ActionVerb.valueOf(name) } catch (e: Exception) { null }
            }}
    }
}
```

#### Task 3.2: Update VoiceCommandGenerator to Include Actions
**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/webavanue/voiceos/VoiceCommandGenerator.kt`

Add to `WebVoiceCommand` data class:
```kotlin
data class WebVoiceCommand(
    val vosId: String,
    val elementType: String,
    val fullText: String,
    val words: List<String>,
    val selector: String,
    val xpath: String,
    val bounds: ElementBounds,
    val action: CommandAction,
    val metadata: Map<String, String>,
    // NEW FIELDS
    val allowedActions: List<WebElementActions.ActionVerb> = listOf(WebElementActions.ActionVerb.CLICK),
    val primaryAction: WebElementActions.ActionVerb = WebElementActions.ActionVerb.CLICK,
    val elementHash: String = "",
    val domainId: String = ""
)
```

---

### Phase 4: BrowserVoiceOSCallback Integration

#### Task 4.1: Add Persistence to BrowserVoiceOSCallback
**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/webavanue/voiceos/BrowserVoiceOSCallback.kt`

```kotlin
package com.augmentalis.webavanue.voiceos

import com.augmentalis.database.dto.ScrapedWebCommandDTO
import com.augmentalis.database.repositories.IScrapedWebCommandRepository
import com.augmentalis.database.repositories.IWebAppWhitelistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowserVoiceOSCallback(
    private val webCommandRepository: IScrapedWebCommandRepository? = null,
    private val whitelistRepository: IWebAppWhitelistRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : VoiceOSWebCallback {

    private val _currentScrapeResult = MutableStateFlow<DOMScrapeResult?>(null)
    val currentScrapeResult: StateFlow<DOMScrapeResult?> = _currentScrapeResult.asStateFlow()

    private val _isPageLoading = MutableStateFlow(false)
    val isPageLoading: StateFlow<Boolean> = _isPageLoading.asStateFlow()

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val commandGenerator = VoiceCommandGenerator()

    override fun onDOMScraped(result: DOMScrapeResult) {
        _currentScrapeResult.value = result

        val domainId = extractDomain(result.url)

        // Clear and regenerate commands
        commandGenerator.clear()
        commandGenerator.addElements(result.elements, domainId)

        println("VoiceOS: DOM scraped - ${result.elementCount} elements, ${commandGenerator.getCommandCount()} voice commands generated")

        // Check if domain is whitelisted for persistence
        scope.launch {
            try {
                val isWhitelisted = whitelistRepository?.isWhitelisted(domainId) ?: false

                if (isWhitelisted && webCommandRepository != null) {
                    // Persist commands to database
                    val commands = commandGenerator.getAllCommands()
                    val dtos = commands.map { cmd ->
                        ScrapedWebCommandDTO(
                            elementHash = cmd.elementHash,
                            domainId = domainId,
                            urlPattern = extractUrlPattern(result.url),
                            cssSelector = cmd.selector,
                            xpath = cmd.xpath,
                            commandText = cmd.fullText,
                            elementText = cmd.metadata["text"],
                            elementTag = cmd.metadata["tag"] ?: "unknown",
                            elementType = cmd.elementType,
                            allowedActions = cmd.allowedActions.map { it.name.lowercase() },
                            primaryAction = cmd.primaryAction.name.lowercase(),
                            confidence = 0.7f,  // Default confidence
                            createdAt = System.currentTimeMillis(),
                            lastVerified = System.currentTimeMillis(),
                            boundLeft = cmd.bounds.left,
                            boundTop = cmd.bounds.top,
                            boundWidth = cmd.bounds.width,
                            boundHeight = cmd.bounds.height
                        )
                    }
                    webCommandRepository.insertBatch(dtos)

                    // Update whitelist stats
                    whitelistRepository?.updateVisit(domainId)
                    whitelistRepository?.updateCommandCount(domainId, dtos.size)

                    println("VoiceOS: Persisted ${dtos.size} commands for whitelisted domain: $domainId")
                } else {
                    println("VoiceOS: Domain not whitelisted, commands in memory only: $domainId")
                }
            } catch (e: Exception) {
                println("VoiceOS: Error persisting commands: ${e.message}")
            }
        }
    }

    override fun onPageLoadStarted(url: String) {
        _isPageLoading.value = true
        _currentUrl.value = url
        _currentScrapeResult.value = null
        commandGenerator.clear()
        println("VoiceOS: Page load started - $url")
    }

    override fun onPageLoadFinished(url: String, title: String) {
        _isPageLoading.value = false
        _currentUrl.value = url
        _currentTitle.value = title

        val domainId = extractDomain(url)

        // Try to load cached commands for whitelisted domains
        scope.launch {
            try {
                val isWhitelisted = whitelistRepository?.isWhitelisted(domainId) ?: false
                if (isWhitelisted && webCommandRepository != null) {
                    val cachedCommands = webCommandRepository.getByDomainAndUrl(domainId, url)
                    if (cachedCommands.isNotEmpty()) {
                        // Preload cached commands while waiting for fresh scan
                        println("VoiceOS: Loaded ${cachedCommands.size} cached commands for $domainId")
                    }
                }
            } catch (e: Exception) {
                println("VoiceOS: Error loading cached commands: ${e.message}")
            }
        }

        println("VoiceOS: Page load finished - $title ($url)")
    }

    override fun onDOMContentChanged() {
        println("VoiceOS: DOM content changed")
    }

    override suspend fun executeCommand(command: VoiceCommandGenerator.WebVoiceCommand): Boolean {
        println("VoiceOS: Executing command - ${command.action} on ${command.fullText}")

        // Track usage if persisted
        if (webCommandRepository != null && command.elementHash.isNotEmpty()) {
            try {
                // Find command in database and increment usage
                val domainId = extractDomain(_currentUrl.value)
                val commands = webCommandRepository.getByDomain(domainId)
                commands.find { it.elementHash == command.elementHash }?.let {
                    webCommandRepository.incrementUsage(it.id)
                }
            } catch (e: Exception) {
                println("VoiceOS: Error tracking usage: ${e.message}")
            }
        }

        return true  // TODO: Implement actual execution via JavaScript
    }

    // ... existing helper methods ...

    private fun extractDomain(url: String): String {
        return try {
            val cleanUrl = url.removePrefix("https://").removePrefix("http://")
            cleanUrl.substringBefore("/").substringBefore("?")
        } catch (e: Exception) {
            url
        }
    }

    private fun extractUrlPattern(url: String): String? {
        return try {
            val path = url.substringAfter("://").substringAfter("/", "")
            if (path.isNotEmpty()) "/$path".substringBefore("?") else null
        } catch (e: Exception) {
            null
        }
    }
}
```

---

### Phase 5: Webapp Whitelist Settings UI

#### Task 5.1: Create Whitelist Settings Screen
**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/webavanue/presentation/ui/settings/WebAppWhitelistScreen.kt`

```kotlin
@Composable
fun WebAppWhitelistScreen(
    whitelistRepository: IWebAppWhitelistRepository,
    onDismiss: () -> Unit
) {
    var entries by remember { mutableStateOf<List<WebAppWhitelistDTO>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entries = whitelistRepository.getAll()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Saved Web Apps",
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add webapp")
            }
        }

        Text(
            "Voice commands for these websites will be saved to the database for faster access.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Whitelist entries
        LazyColumn {
            items(entries) { entry ->
                WhitelistEntryCard(
                    entry = entry,
                    onToggle = { enabled ->
                        scope.launch {
                            whitelistRepository.insertOrUpdate(entry.copy(isEnabled = enabled))
                            entries = whitelistRepository.getAll()
                        }
                    },
                    onDelete = {
                        scope.launch {
                            whitelistRepository.delete(entry.domainId)
                            entries = whitelistRepository.getAll()
                        }
                    }
                )
            }
        }
    }

    // Add webapp dialog
    if (showAddDialog) {
        AddWebAppDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { domain, name, category ->
                scope.launch {
                    whitelistRepository.insertOrUpdate(
                        WebAppWhitelistDTO(
                            domainId = domain,
                            displayName = name,
                            category = category,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    entries = whitelistRepository.getAll()
                    showAddDialog = false
                }
            }
        )
    }
}
```

---

### Phase 6: Element Hash Strategy

#### Task 6.1: Implement Stable Element Hashing
**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/webavanue/voiceos/WebElementHasher.kt`

```kotlin
package com.augmentalis.webavanue.voiceos

import com.augmentalis.voiceoscoreng.functions.HashUtils

/**
 * Generates stable hashes for web elements that persist across page loads.
 *
 * Strategy: Combine multiple stable attributes to create a unique fingerprint.
 * Avoid: Position-based selectors (:nth-of-type) which change with DOM mutations.
 */
object WebElementHasher {

    /**
     * Generate a stable hash for a DOM element.
     *
     * Components (in order of stability):
     * 1. Element ID (most stable if present)
     * 2. ARIA label (semantic, usually stable)
     * 3. Role + tag combination
     * 4. Visible text (first 50 chars, normalized)
     * 5. Class names (first 2, sorted)
     */
    fun computeHash(element: DOMElement): String {
        val components = mutableListOf<String>()

        // ID is the most stable identifier
        if (element.id.isNotBlank()) {
            components.add("id:${element.id}")
        }

        // ARIA label is semantic and usually stable
        if (element.ariaLabel.isNotBlank()) {
            components.add("aria:${normalize(element.ariaLabel)}")
        }

        // Role + tag
        val role = element.role.ifBlank { inferRole(element) }
        components.add("role:${element.tag}/$role")

        // Visible text (normalized, truncated)
        if (element.name.isNotBlank()) {
            components.add("text:${normalize(element.name).take(50)}")
        }

        // href for links (normalized)
        if (element.href.isNotBlank()) {
            components.add("href:${normalizeHref(element.href)}")
        }

        // Input type for form fields
        if (element.inputType.isNotBlank()) {
            components.add("input:${element.inputType}")
        }

        // Placeholder for inputs
        if (element.placeholder.isNotBlank()) {
            components.add("ph:${normalize(element.placeholder)}")
        }

        // Combine and hash
        val fingerprint = components.joinToString("|")
        return HashUtils.generateHash(fingerprint, 12)  // 12-char hash
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeHref(href: String): String {
        // Remove query params and fragments for stability
        return href.substringBefore("?").substringBefore("#")
    }

    private fun inferRole(element: DOMElement): String {
        return when (element.tag.lowercase()) {
            "a" -> "link"
            "button" -> "button"
            "input" -> when (element.inputType.lowercase()) {
                "submit", "button" -> "button"
                "checkbox" -> "checkbox"
                "radio" -> "radio"
                else -> "textbox"
            }
            "select" -> "listbox"
            "textarea" -> "textbox"
            else -> element.type.ifBlank { "generic" }
        }
    }
}
```

---

## Implementation Checklist

### Database Layer
- [ ] Create `ScrapedWebCommand.sq` schema file
- [ ] Create `WebAppWhitelist.sq` schema file
- [ ] Create `ScrapedWebCommandDTO.kt`
- [ ] Create `WebAppWhitelistDTO.kt`
- [ ] Create `IScrapedWebCommandRepository.kt` interface
- [ ] Create `IWebAppWhitelistRepository.kt` interface
- [ ] Implement repositories with SQLDelight

### Voice Command Layer
- [ ] Create `WebElementActions.kt` with action verbs
- [ ] Create `WebElementHasher.kt` for stable hashing
- [ ] Update `VoiceCommandGenerator.kt` to include actions and hashing
- [ ] Update `BrowserVoiceOSCallback.kt` with persistence integration

### UI Layer
- [ ] Create `WebAppWhitelistScreen.kt` settings UI
- [ ] Add whitelist settings to browser settings menu
- [ ] Add "Save this site" option in browser overflow menu

### Testing
- [ ] Test command persistence for whitelisted domains
- [ ] Test command cleanup for non-whitelisted domains
- [ ] Test action verb recognition
- [ ] Test element hash stability across page reloads

---

## Summary

This implementation plan addresses:

1. **Persistent web command storage** via new `scraped_web_command` table
2. **Webapp whitelist** allowing users to mark sites for database storage
3. **Action verb support** with element-specific allowed actions
4. **Stable element hashing** to match commands across page loads
5. **Memory-only mode** for non-whitelisted sites (no database bloat)

The design mirrors the native Android command storage system while accounting for web-specific challenges (no version codes, dynamic DOM, URL patterns).
