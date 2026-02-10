# NewAvanues Repository Rules

## MANDATORY RULE #1: SCRAPING SYSTEM PROTECTION (ZERO TOLERANCE)

**DO NOT remove, delete, replace, disable, comment out, refactor away, or otherwise eliminate ANY part of the working scraping system without FIRST:**

1. **Asking the user for explicit permission** via AskUserQuestion
2. **Stating the COMPLETE reason** why you believe the change is necessary
3. **Describing EXACTLY what you plan to do** - which files, functions, tables, or components will be affected
4. **Waiting for user approval** before making ANY changes

### What "scraping system" means (protected scope):

- **Android scraping**: `Modules/VoiceOSCore/` - `VoiceOSAccessibilityService`, `AndroidScreenExtractor`, and all accessibility-based UI traversal code
- **Web scraping**: `Modules/WebAvanue/` - `DOMScraperBridge` and all JavaScript DOM traversal/injection code
- **Database schema**: `Modules/Database/` - `ScrapedApp.sq`, `ScrapedElement.sq`, `ScrapedHierarchy.sq`, `ScrapedWebElement.sq`, `ScreenContext.sq`, `GeneratedCommand.sq`, `ScreenTransition.sq`, `UserInteraction.sq` and their repository interfaces/implementations
- **RPC layer**: `VoiceOSService.kt` scrapeScreen methods and related DTOs
- **Command generation**: Any code that generates voice commands from scraped elements
- **Supporting infrastructure**: Repositories, data models, element hashing, deduplication logic, form grouping, screen context tracking

### This rule applies to ALL of the following actions:

- Deleting files or functions
- Removing database tables or columns
- Replacing the implementation with a "new" or "better" version
- Refactoring that changes the scraping behavior or data flow
- Migrating to a different scraping approach
- Commenting out or disabling scraping functionality
- Removing dependencies that the scraping system relies on

### Why this rule exists:

The scraping system is a complex, production-grade, dual-platform (Android + Web) UI element discovery and voice command generation pipeline. It has been built, tested, and refined over many sessions. Accidental removal or replacement causes catastrophic loss of working functionality that takes significant effort to rebuild.

**Violation of this rule is a session-ending error. No exceptions. No "I'll add it back later." No stubs.**
