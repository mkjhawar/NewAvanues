# Database Module

**Version:** 1.0.0
**Status:** Production
**Updated:** 2026-01-14

Unified cross-platform database module for all Avanues applications using SQLDelight.

---

## Module Structure

```
Modules/Database/
├── build.gradle.kts                    # KMP configuration (Android, iOS, Desktop)
└── src/
    └── commonMain/
        ├── kotlin/com/augmentalis/database/
        │   ├── dto/
        │   │   ├── ScrapedWebCommandDTO.kt     # Web voice command DTO
        │   │   └── WebAppWhitelistDTO.kt       # Whitelisted web app DTO
        │   └── repositories/
        │       ├── IScrapedWebCommandRepository.kt  # Web command CRUD
        │       └── IWebAppWhitelistRepository.kt    # Whitelist management
        └── sqldelight/com/augmentalis/database/
            ├── avid/
            │   ├── AvidElement.sq          # AVID element persistence
            │   ├── AvidHierarchy.sq        # Parent-child relationships
            │   ├── AvidAlias.sq            # Alternative names/synonyms
            │   └── AvidAnalytics.sq        # Usage analytics
            ├── web/
            │   ├── ScrapedWebCommand.sq    # Voice commands from web pages
            │   └── WebAppWhitelist.sq      # User-designated web apps
            └── browser/
                └── BrowserTables.sq        # Tabs, history, favorites, etc.
```

---

## Folder Structure Flowchart

```mermaid
graph TD
    subgraph "Modules/Database"
        BG[build.gradle.kts]

        subgraph "src/commonMain"
            subgraph "kotlin/.../database"
                subgraph "dto"
                    DTO1[ScrapedWebCommandDTO.kt]
                    DTO2[WebAppWhitelistDTO.kt]
                end
                subgraph "repositories"
                    REPO1[IScrapedWebCommandRepository.kt]
                    REPO2[IWebAppWhitelistRepository.kt]
                end
            end

            subgraph "sqldelight/.../database"
                subgraph "avid"
                    SQ1[AvidElement.sq]
                    SQ2[AvidHierarchy.sq]
                    SQ3[AvidAlias.sq]
                    SQ4[AvidAnalytics.sq]
                end
                subgraph "web"
                    SQ5[ScrapedWebCommand.sq]
                    SQ6[WebAppWhitelist.sq]
                end
                subgraph "browser"
                    SQ7[BrowserTables.sq]
                end
            end
        end
    end

    BG --> dto
    BG --> repositories
    BG --> avid
    BG --> web
    BG --> browser
```

---

## App Interactions Flowchart

```mermaid
flowchart TB
    subgraph "Applications"
        WEB[WebAvanue Browser]
        VOS[VoiceOSCoreNG]
        AVA[AVA Assistant]
    end

    subgraph "Database Module"
        subgraph "DTOs"
            SWCDTO[ScrapedWebCommandDTO]
            WAWDTO[WebAppWhitelistDTO]
        end

        subgraph "Repository Interfaces"
            ISWC[IScrapedWebCommandRepository]
            IWAW[IWebAppWhitelistRepository]
        end

        subgraph "SQLDelight Tables"
            subgraph "AVID Tables"
                AVID_E[avid_element]
                AVID_H[avid_hierarchy]
                AVID_A[avid_alias]
                AVID_AN[avid_analytics]
            end

            subgraph "Web Tables"
                SWC[scraped_web_command]
                WAW[web_app_whitelist]
            end

            subgraph "Browser Tables"
                TAB[tab]
                HIST[history_entry]
                FAV[favorite]
                DL[download]
            end
        end
    end

    subgraph "Platform Drivers"
        AND[Android Driver]
        IOS[Native Driver]
        DSK[SQLite Driver]
    end

    WEB -->|"Uses"| ISWC
    WEB -->|"Uses"| IWAW
    WEB -->|"Browser data"| TAB
    WEB -->|"Browser data"| HIST

    VOS -->|"AVID lookup"| AVID_E
    VOS -->|"Element hierarchy"| AVID_H

    AVA -->|"Analytics"| AVID_AN
    AVA -->|"Aliases"| AVID_A

    ISWC --> SWCDTO
    IWAW --> WAWDTO

    SWCDTO --> SWC
    WAWDTO --> WAW

    SWC --> AND
    SWC --> IOS
    SWC --> DSK
```

---

## Data Flow: Web Command Persistence

```mermaid
sequenceDiagram
    participant User
    participant WebAvanue
    participant DOMScraperBridge
    participant IScrapedWebCommandRepository
    participant Database

    User->>WebAvanue: Visits whitelisted site (e.g., Gmail)
    WebAvanue->>DOMScraperBridge: scrapeDOM()
    DOMScraperBridge-->>WebAvanue: List<DOMElement>

    WebAvanue->>IScrapedWebCommandRepository: isWhitelisted(domainId)
    IScrapedWebCommandRepository->>Database: SELECT from web_app_whitelist
    Database-->>IScrapedWebCommandRepository: true

    WebAvanue->>IScrapedWebCommandRepository: insertBatch(commands)
    IScrapedWebCommandRepository->>Database: INSERT INTO scraped_web_command
    Database-->>IScrapedWebCommandRepository: Success

    Note over WebAvanue: Commands persisted for future visits

    User->>WebAvanue: Returns to site later
    WebAvanue->>IScrapedWebCommandRepository: getByDomain(domainId)
    IScrapedWebCommandRepository->>Database: SELECT from scraped_web_command
    Database-->>IScrapedWebCommandRepository: List<ScrapedWebCommandDTO>
    IScrapedWebCommandRepository-->>WebAvanue: Cached commands available

    User->>WebAvanue: Says "Click Compose"
    WebAvanue->>IScrapedWebCommandRepository: incrementUsage(id)
    IScrapedWebCommandRepository->>Database: UPDATE usage_count
```

---

## Database Tables Overview

### AVID Tables (Voice ID System)

| Table | Purpose | Key Fields |
|-------|---------|------------|
| `avid_element` | Element identification | avid, module, type, element_hash, bounds |
| `avid_hierarchy` | Parent-child relationships | parent_avid, child_avid, relationship_type |
| `avid_alias` | Alternative names | avid_id, alias, source, confidence |
| `avid_analytics` | Usage tracking | avid_id, event_type, count, last_used |

### Web Tables (Voice Commands)

| Table | Purpose | Key Fields |
|-------|---------|------------|
| `scraped_web_command` | Web element voice commands | element_hash, domain_id, command_text, css_selector |
| `web_app_whitelist` | User-designated web apps | domain_id, display_name, save_commands, auto_scan |

### Browser Tables

| Table | Purpose | Key Fields |
|-------|---------|------------|
| `tab` | Open browser tabs | url, title, favicon_url, position |
| `tab_group` | Tab grouping | name, color, collapsed |
| `favorite` | Bookmarks | url, title, folder_path |
| `history_entry` | Browsing history | url, title, visit_count, last_visited |
| `download` | Downloaded files | url, filename, status, progress |
| `browser_settings` | Browser config | setting_key, setting_value |
| `site_permission` | Per-site permissions | domain, permission_type, status |
| `session` | Saved sessions | name, tab_count |

---

## Dependencies

```mermaid
graph LR
    subgraph "Database Module"
        DB[Modules/Database]
    end

    subgraph "Dependencies"
        AVID[Modules/AVID]
        SQL[SQLDelight 2.0.1]
        KX[kotlinx-coroutines]
        SER[kotlinx-serialization]
        DT[kotlinx-datetime]
    end

    subgraph "Consumers"
        WEB[WebAvanue/universal]
        VOS[VoiceOSCoreNG]
        AVA[AVA]
    end

    DB --> AVID
    DB --> SQL
    DB --> KX
    DB --> SER
    DB --> DT

    WEB --> DB
    VOS --> DB
    AVA --> DB
```

---

## Platform Support

| Platform | Driver | Status |
|----------|--------|--------|
| Android | `sqldelight-android-driver` | Production |
| iOS | `sqldelight-native-driver` | Production |
| Desktop (JVM) | `sqldelight-sqlite-driver` | Production |

---

## Usage Example

```kotlin
// Check if domain is whitelisted
val isWhitelisted = whitelistRepository.isWhitelisted("mail.google.com")

if (isWhitelisted) {
    // Get cached commands for this domain
    val commands = commandRepository.getByDomain("mail.google.com")

    // Use cached commands for voice matching
    commands.forEach { cmd ->
        println("Command: ${cmd.commandText} -> ${cmd.cssSelector}")
    }
}

// Save new commands after scraping
val newCommands = domScraperBridge.scrapeDOM().map { element ->
    ScrapedWebCommandDTO(
        elementHash = element.hash,
        domainId = "mail.google.com",
        commandText = element.text ?: element.ariaLabel ?: "",
        cssSelector = element.selector,
        elementTag = element.tagName,
        elementType = "button",
        createdAt = System.currentTimeMillis()
    )
}
commandRepository.insertBatch(newCommands)
```

---

## Related Documentation

- [PLATFORM-INDEX.ai.md](../AI/PLATFORM-INDEX.ai.md) - Module registry
- [CLASS-INDEX.ai.md](../AI/CLASS-INDEX.ai.md) - Class reference
- [AVID Module](../AVID/README.md) - Voice ID system

---

*Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC*
