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

---

## MANDATORY RULE #2: FILE PLACEMENT (ZERO TOLERANCE)

**Every new file MUST be placed in the correct location. NEVER create files in ad-hoc or incorrect paths. If unsure, ASK the user before creating the file.**

### Modules: `Modules/ModuleName/`

Shared/reusable library code lives in modules. These follow KMP source set conventions:

```
Modules/
  ModuleName/
    src/
      commonMain/kotlin/...    # Cross-platform shared code
      androidMain/kotlin/...   # Android-specific implementations
      iosMain/kotlin/...       # iOS-specific implementations
      desktopMain/kotlin/...   # Desktop-specific implementations
      jvmMain/kotlin/...       # JVM-specific implementations
```

- Platform-specific code goes in the correct source set (`androidMain`, `iosMain`, etc.), NOT in a separate folder
- NEVER create platform folders outside the KMP `src/` structure for module code
- If a module doesn't exist yet, ask the user before creating a new `Modules/NewName/` directory

### Apps: `Apps/Platform/AppName/`

All applications (runnable targets with UI) live under `Apps/`, organized by platform first, then app name:

```
Apps/
  Android/
    VoiceOS/
    VoiceCursor/
    WebAvanue/
    ...
  iOS/
    ...
  Web/
    ...
  Desktop/
    ...
```

**Correct placement rules:**
- Android apps go in `Apps/Android/AppName/`
- iOS apps go in `Apps/iOS/AppName/`
- Web apps go in `Apps/Web/AppName/`
- Desktop apps go in `Apps/Desktop/AppName/`
- KMP/multiplatform apps go in `Apps/KMP/AppName/` (if the app target is multiplatform itself)

**NEVER place apps in:**
- `android/Apps/` (legacy location - DO NOT ADD new apps here)
- `Apps/` root without a platform subfolder
- `Modules/` (modules are libraries, not apps)
- Any other invented path

### How to decide: Module or App?

| It is a... | Place it in... |
|------------|---------------|
| Reusable library/SDK with no UI entry point | `Modules/ModuleName/` |
| Runnable application with a UI/launcher | `Apps/Platform/AppName/` |
| Platform-specific implementation of a module | `Modules/ModuleName/src/{platform}Main/` |

### Why this rule exists:

The repo has accumulated apps in inconsistent locations (`/Apps/`, `/android/Apps/`, mixed paths) causing confusion about where things live. This rule establishes a single, unambiguous convention going forward.

**If you are about to create a file and are not 100% certain it's in the right place, STOP and ask the user.**
