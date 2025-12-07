# LearnApp v2.0 - Contact Learning System Specification

**Project:** VoiceOS
**Module:** LearnApp
**Version:** 2.0
**Status:** 📋 SPECIFICATION (Post v1.1 Testing)
**Created:** 2025-11-23 17:39 PST
**Author:** VoiceOS Development Team

---

## Executive Summary

LearnApp v2.0 adds **Contact Learning & Management** to enable natural voice commands for calling and messaging contacts across all communication apps (WhatsApp, Teams, Phone, Messenger, etc.) without requiring manual contact entry or system permissions upfront.

**Key Features:**
- ✅ Automatic contact discovery during app learning
- ✅ Cross-app contact deduplication & merging
- ✅ User preference learning (which app per contact)
- ✅ Progressive system contact integration (optional)
- ✅ Voice commands: "Call Mike Johnson on Teams"
- ✅ Export/import for device migration

**Implementation Timeline:** Post v1.1 testing (Month 3-6)

---

## Problem Statement

### Current Limitations (v1.1)

**Without Contact Learning:**
```
User: "Call Mike Johnson"
VoiceOS: ❌ "I don't know who Mike Johnson is"
```

**Manual Contact Entry:**
- User must manually add each contact
- Must specify which app for each contact
- Tedious for 100+ contacts across 5+ apps
- Doesn't scale

### Proposed Solution (v2.0)

**Automatic Contact Learning:**
```
[User learns WhatsApp with LearnApp]
   ↓
LearnApp: "Found 127 contacts. Learn them for voice commands?"
   ↓
[User approves]
   ↓
LearnApp automatically scrapes all contact names
   ↓
User: "Call Mike Johnson on WhatsApp"
VoiceOS: ✅ *Opens WhatsApp and calls Mike Johnson*
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│              VoiceOSService                              │
│          (AccessibilityService)                          │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│           LearnApp v2.0 - Contact System                 │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ 1. Contact Discovery                            │    │
│  │    - Detects contact lists during exploration   │    │
│  │    - Shows consent dialog after app learned     │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ 2. Contact Scraper                              │    │
│  │    - Scrolls through contact list               │    │
│  │    - Reads names, phone, email (visible only)   │    │
│  │    - Maps position in list                      │    │
│  │    - Links to UI flows (call/message/video)     │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ 3. Contact Deduplicator                         │    │
│  │    - Merges same contact across apps            │    │
│  │    - Fuzzy name matching                        │    │
│  │    - Combines metadata (phone, email, etc.)     │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ 4. Preference Manager                           │    │
│  │    - Asks user first time per contact/action    │    │
│  │    - Remembers choice                           │    │
│  │    - Future commands use preference             │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ 5. System Contact Integration (Optional)        │    │
│  │    - Progressive upgrade offer (7+ days)        │    │
│  │    - READ_CONTACTS permission                   │    │
│  │    - Merges with learned contacts               │    │
│  │    - Enables: "Call my mom", relationship tags  │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ 6. Export/Import Manager                        │    │
│  │    - Export: voiceos-contacts.vos               │    │
│  │    - 3-letter compact format                    │    │
│  │    - Validates schema on import                 │    │
│  │    - Restores preferences                       │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
└─────────────────────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│              CommandManager                              │
│          (Voice Command Routing)                         │
│  - Parses: "call {name}" / "message {name}"             │
│  - Resolves contact                                     │
│  - Gets preferred app or asks                           │
│  - Executes UI flow                                     │
└─────────────────────────────────────────────────────────┘
```

---

## User Flow

### Phase 1: Contact Discovery (After App Learning)

**1. App Exploration Completes**
```
[User approves WhatsApp learning]
   ↓
LearnApp explores UI (buttons, screens, navigation)
   ↓
Discovers contact list screen
   ↓
Exploration complete! ✅
```

**2. Contact Learning Consent Dialog**
```
┌──────────────────────────────────────────────┐
│ WhatsApp learning complete! ✅               │
├──────────────────────────────────────────────┤
│ 📱 Found 127 contacts                        │
│                                              │
│ Learn contacts for voice commands like:     │
│ • "Call Mike Johnson on WhatsApp"           │
│ • "Message Sarah on WhatsApp"               │
│ • "Video call John on WhatsApp"             │
│                                              │
│ ⏱️ Takes ~1 minute                          │
│                                              │
│   [No Thanks]  [Yes, Learn Contacts]        │
└──────────────────────────────────────────────┘
```

**3. Contact Scraping Process**
```
[User clicks "Yes, Learn Contacts"]
   ↓
Shows progress overlay:
   "Learning WhatsApp contacts... 47/127"
   ↓
LearnApp (via Accessibility):
   1. Scrolls through contact list (ACTION_SCROLL_FORWARD)
   2. Reads each contact name (getText, getContentDescription)
   3. Captures visible metadata:
      - Phone numbers (if visible)
      - Email addresses (if visible)
      - Status (if visible: "Available", "Busy", etc.)
   4. Maps position in list (for future scrolling)
   5. Links available actions (call, message, video based on learned UI)
   ↓
Stores in database (contact_sources table)
   ↓
Notification: "✅ Learned 127 WhatsApp contacts!"
```

### Phase 2: Cross-App Deduplication

**Scenario:** User has learned WhatsApp, Teams, and Phone app

**Database State:**
```sql
-- contact_sources table (individual app contacts)
| id | unified_id | app_package       | name           | position | actions         |
|----|------------|-------------------|----------------|----------|-----------------|
| 1  | NULL       | com.whatsapp      | Mike Johnson   | 15       | [1,2,3]         |
| 2  | NULL       | com.microsoft.teams| Mike Johnson   | 8        | [1,2,3,6]       |
| 3  | NULL       | com.android.phone | Mike Johnson   | 42       | [1,2]           |
| 4  | NULL       | com.whatsapp      | Sarah Davis    | 16       | [1,2,3]         |
```

**Deduplication Process:**
```
Contact Deduplicator runs after each app learning:
   ↓
1. Normalize names:
   "Mike Johnson" → "mikejohnson"
   "Mike  Johnson" → "mikejohnson"
   "mike johnson" → "mikejohnson"
   ↓
2. Find matches across apps:
   "Mike Johnson" appears in:
   - WhatsApp (position 15)
   - Teams (position 8)
   - Phone (position 42)
   ↓
3. Create unified contact:
   INSERT INTO unified_contacts (
       contact_id: "u1",
       primary_name: "Mike Johnson",
       normalized_name: "mikejohnson",
       merged_actions: [1,2,3,6]  // call, message, video, screen_share
   )
   ↓
4. Link sources:
   UPDATE contact_sources SET unified_contact_id = 1 WHERE name = "Mike Johnson"
   ↓
5. Merge metadata:
   - Phone: +1-555-123-4567 (from Phone app)
   - Email: mike.j@company.com (from Teams)
   - Apps: WhatsApp, Teams, Phone
```

**Result:**
```sql
-- unified_contacts table (deduplicated)
| id | contact_id | primary_name | normalized_name | merged_actions | preferences |
|----|------------|--------------|-----------------|----------------|-------------|
| 1  | u1         | Mike Johnson | mikejohnson     | [1,2,3,6]      | NULL        |
| 2  | u2         | Sarah Davis  | sarahdavis      | [1,2,3]        | NULL        |
```

### Phase 3: User Preference Learning

**First Voice Command:**
```
User: "Call Mike Johnson"
   ↓
VoiceCommandProcessor:
   1. Finds contact: Mike Johnson (u1)
   2. Available in: WhatsApp, Teams, Phone
   3. No preference saved yet
   ↓
Shows preference dialog:
┌──────────────────────────────────────────────┐
│ Call Mike Johnson using:                     │
├──────────────────────────────────────────────┤
│ ○ Microsoft Teams                            │
│   Work contact • mike.j@company.com          │
│                                              │
│ ○ WhatsApp                                   │
│   Mobile                                     │
│                                              │
│ ○ Phone                                      │
│   +1-555-123-4567                           │
│                                              │
│ ☑ Remember this choice for calls            │
│                                              │
│           [Cancel]  [Select]                 │
└──────────────────────────────────────────────┘
   ↓
User selects: "Microsoft Teams"
   ↓
Saves preference:
UPDATE unified_contacts
SET preferences = '{"CAL":"com.microsoft.teams"}'
WHERE contact_id = "u1"
   ↓
Executes: Opens Teams → Calls Mike Johnson
```

**Future Voice Commands:**
```
User: "Call Mike Johnson"
   ↓
Finds contact: u1
   ↓
Checks preference: {"CAL":"com.microsoft.teams"}
   ↓
Executes immediately using Teams (no dialog)
   ↓
Success! ✅
```

**Different Action:**
```
User: "Message Mike Johnson"
   ↓
Finds contact: u1
   ↓
Checks preference for "message" action: NULL
   ↓
Shows dialog (first time for message action):
   "Message Mike Johnson using: Teams / WhatsApp / Phone?"
   ↓
User selects WhatsApp
   ↓
Saves: {"CAL":"com.microsoft.teams", "MSG":"com.whatsapp"}
   ↓
Future: "Message Mike" → WhatsApp automatically
```

### Phase 4: Progressive System Contact Integration

**Trigger Conditions:**
```
After 7+ days of usage AND:
- 3+ apps with contacts learned
- 100+ total contacts
- User hasn't dismissed upgrade offer before
```

**Upgrade Offer Dialog:**
```
┌──────────────────────────────────────────────┐
│ 💡 Make Voice Commands Easier                │
├──────────────────────────────────────────────┤
│ You've learned 157 contacts across 4 apps.   │
│                                              │
│ Enable system contacts for:                 │
│ • Simpler: "Call Mike" (no app name needed) │
│ • Labels: "Call my manager"                  │
│ • Nicknames: "Call mom"                      │
│ • Better matching across all apps            │
│                                              │
│ This requires READ_CONTACTS permission.      │
│ You can disable this anytime in settings.    │
│                                              │
│      [Maybe Later]  [Enable]                 │
└──────────────────────────────────────────────┘
   ↓
[User clicks "Enable"]
   ↓
Requests Android READ_CONTACTS permission
   ↓
[User grants permission]
   ↓
Background process:
1. Reads system contacts
2. Merges with learned contacts (fuzzy matching)
3. Adds relationship labels ("Mom", "Manager", etc.)
4. Updates unified_contacts table
   ↓
Notification: "✅ System contacts enabled!"
```

**Enhanced Commands Now Work:**
```
✅ "Call my mom"          // Uses relationship label
✅ "Email Mike at work"   // Uses email from system contacts
✅ "Message Sarah"        // No app name needed (uses preference)
```

### Phase 5: Export & Import

**Export Flow:**
```
Settings → Export → Export Contacts
   ↓
Privacy Warning Dialog:
┌──────────────────────────────────────────────┐
│ Export All Contacts                          │
├──────────────────────────────────────────────┤
│ ⚠️ Privacy Warning                           │
│                                              │
│ This file contains personal information:    │
│ • 324 contact names                          │
│ • Phone numbers and emails                   │
│ • Your app preferences                       │
│                                              │
│ Total: 167 unique contacts                   │
│ Apps: WhatsApp, Teams, Phone, Messenger      │
│                                              │
│ Keep this file private and secure.           │
│ Encryption: Coming soon (v2.1)               │
│                                              │
│        [Cancel]  [Export]                    │
└──────────────────────────────────────────────┘
   ↓
Exports to: /Download/voiceos-contacts-20251123.vos
   ↓
Success notification with file path
```

**VOS Contact Format (3-Letter Compact):**
```json
{
  "SCH": "vos-cnt-v1",
  "VER": "1.0.0",
  "EXP": {
    "TSP": 1732406400,
    "DEV": "Pixel 7 Pro",
    "USR": "user@example.com"
  },
  "CNT": [
    {
      "ID": "u1",
      "NAM": "Mike Johnson",
      "NRM": "mikejohnson",
      "ACT": [1,2,3,6],
      "PH": ["+15551234567"],
      "EM": ["mike.j@company.com"],
      "PRF": {"CAL":"com.microsoft.teams","MSG":"com.whatsapp"},
      "SRC": [
        {"PKG":"com.whatsapp","POS":15,"ACT":[1,2,3]},
        {"PKG":"com.microsoft.teams","POS":8,"ACT":[1,2,3,6]},
        {"PKG":"com.android.phone","POS":42,"ACT":[1,2]}
      ]
    }
  ],
  "CHK": {
    "SHA": "a7f3c2e1..."
  }
}
```

**Import Flow:**
```
New Device → Settings → Import → Select File
   ↓
Validates schema:
   "vos-cnt-v1" ✅ Compatible
   ↓
Checks installed apps:
   ✅ WhatsApp (installed)
   ✅ Teams (installed)
   ✅ Phone (system app)
   ⚠️ Messenger (not installed)
   ↓
Shows import summary:
┌──────────────────────────────────────────────┐
│ Import 167 Contacts?                         │
├──────────────────────────────────────────────┤
│ ✅ 104 contacts ready to import              │
│    (WhatsApp, Teams, Phone)                  │
│                                              │
│ ⚠️ 63 contacts skipped                       │
│    (Messenger not installed)                 │
│                                              │
│ Preferences will be restored:                │
│ • Call preferences: 87 contacts              │
│ • Message preferences: 52 contacts           │
│                                              │
│        [Cancel]  [Import]                    │
└──────────────────────────────────────────────┘
   ↓
Imports contacts + preferences
   ↓
Success! ✅
```

---

## Database Schema

### Table 1: `unified_contacts`

**Purpose:** Deduplicated contacts merged across all apps

```sql
CREATE TABLE unified_contacts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    contact_id TEXT UNIQUE NOT NULL,         -- "u1", "u2", etc. (short identifier)
    primary_name TEXT NOT NULL,              -- "Mike Johnson"
    normalized_name TEXT NOT NULL,           -- "mikejohnson" (for matching)

    -- Merged data (JSON - adaptive based on what apps expose)
    merged_phone_numbers TEXT,               -- [{"TYP":1,"NUM":"+15551234567"}]
    merged_email_addresses TEXT,             -- [{"TYP":2,"EML":"mike@co.com"}]
    merged_actions TEXT,                     -- [1,2,3,6] (call,message,video,screen_share)
    merged_labels TEXT,                      -- ["Work","Manager"] (from system contacts)

    -- User preferences (JSON)
    -- {"CAL":"com.microsoft.teams","MSG":"com.whatsapp","VID":"com.whatsapp"}
    preferences TEXT,

    -- System integration (optional)
    system_contact_id INTEGER,               -- Android contact ID (if READ_CONTACTS granted)

    -- Metadata
    times_used INTEGER DEFAULT 0,
    last_used_timestamp TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Indexes
    UNIQUE(normalized_name)
);

CREATE INDEX idx_unified_normalized ON unified_contacts(normalized_name);
CREATE INDEX idx_unified_system ON unified_contacts(system_contact_id);
```

**Action Codes:**
```
1 = Call
2 = Message
3 = Video
4 = Email
5 = Chat
6 = Screen Share
```

### Table 2: `contact_sources`

**Purpose:** Individual contact entries from each app (before deduplication)

```sql
CREATE TABLE contact_sources (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    unified_contact_id INTEGER,              -- FK to unified_contacts (NULL until merged)

    -- App identification
    app_package TEXT NOT NULL,               -- "com.whatsapp"
    app_name TEXT NOT NULL,                  -- "WhatsApp"
    position_in_list INTEGER,                -- 15 (for scrolling to contact)

    -- Contact name as seen in app
    display_name TEXT NOT NULL,              -- "Mike Johnson"
    normalized_name TEXT NOT NULL,           -- "mikejohnson"

    -- Actions available in this app (JSON array of action codes)
    actions TEXT NOT NULL,                   -- [1,2,3] = call, message, video

    -- App-specific metadata (JSON - adaptive, only what's visible)
    metadata TEXT,                           -- {"STS":1,"SUB":"Available","LAS":"2 min ago"}

    -- Contact data visible in this app (JSON - adaptive)
    phone_numbers TEXT,                      -- [{"TYP":1,"NUM":"+1555..."}]
    email_addresses TEXT,                    -- [{"TYP":2,"EML":"..."}]

    -- Timestamps
    learned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    FOREIGN KEY (unified_contact_id) REFERENCES unified_contacts(id) ON DELETE CASCADE
);

CREATE INDEX idx_sources_app ON contact_sources(app_package);
CREATE INDEX idx_sources_unified ON contact_sources(unified_contact_id);
CREATE INDEX idx_sources_normalized ON contact_sources(normalized_name);
```

### Table 3: `system_integration_settings`

**Purpose:** Track system contact integration status and upgrade offer timing

```sql
CREATE TABLE system_integration_settings (
    id INTEGER PRIMARY KEY DEFAULT 1,

    -- System contacts enabled?
    system_contacts_enabled BOOLEAN DEFAULT 0,
    system_contacts_permission_granted BOOLEAN DEFAULT 0,

    -- Tracking for upgrade offer (show after 7+ days with 100+ contacts)
    days_since_first_app_learned INTEGER DEFAULT 0,
    total_apps_with_contacts INTEGER DEFAULT 0,
    total_contacts_learned INTEGER DEFAULT 0,

    -- Upgrade offer status
    upgrade_offered BOOLEAN DEFAULT 0,
    upgrade_offered_timestamp TIMESTAMP,
    upgrade_accepted_timestamp TIMESTAMP,
    upgrade_declined_timestamp TIMESTAMP,

    -- Timestamps
    first_enabled_timestamp TIMESTAMP,

    -- Ensure single row
    CHECK (id = 1)
);
```

### Table 4: `contact_export_history`

**Purpose:** Track contact exports for user reference

```sql
CREATE TABLE contact_export_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    export_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    file_path TEXT NOT NULL,
    file_size_bytes INTEGER,

    -- Export stats
    total_contacts INTEGER NOT NULL,
    unique_contacts INTEGER NOT NULL,
    apps_included TEXT,                      -- JSON array ["com.whatsapp","com.microsoft.teams"]

    -- Format
    schema_version TEXT NOT NULL,            -- "vos-cnt-v1"
    format_version TEXT NOT NULL,            -- "1.0.0"

    -- Security (v2.1 - encryption planned)
    encryption_enabled BOOLEAN DEFAULT 0,
    checksum TEXT NOT NULL                   -- SHA-256 of file content
);
```

---

## Voice Command Implementation

### Command Patterns (v1.0)

**Supported patterns:**
```
"call {full_name}"                    // Uses preference or asks
"call {full_name} on {app}"          // Explicit app override
"message {full_name}"                 // Uses preference or asks
"message {full_name} on {app}"       // Explicit app
"video call {full_name}"             // Uses preference or asks
"video call {full_name} on {app}"    // Explicit app
"email {full_name}"                  // Uses preference or asks
```

**Examples:**
```
✅ "Call Mike Johnson"                 → Uses saved preference (Teams)
✅ "Call Mike Johnson on WhatsApp"     → Overrides preference
✅ "Message Sarah"                     → Uses saved preference (WhatsApp)
✅ "Video call John"                   → First time - asks which app
✅ "Email Mike at work"                → Uses Teams (has email)
```

### CommandManager Integration

**File:** `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/handlers/ContactCommandHandler.kt` (NEW)

```kotlin
class ContactCommandHandler(
    private val contactRepository: ContactRepository,
    private val uiFlowExecutor: UIFlowExecutor
) : CommandHandler {

    override fun canHandle(input: String): Boolean {
        // Patterns: "call|message|video|email {name} [on {app}]"
        val contactPattern = Regex(
            "(call|message|video call|email)\\s+(.+?)(?:\\s+on\\s+(.+))?\$",
            RegexOption.IGNORE_CASE
        )
        return contactPattern.matches(input)
    }

    override suspend fun handle(input: String): CommandResult {
        // 1. Parse command
        val intent = parseContactCommand(input)
        if (intent == null) {
            return CommandResult.NotRecognized
        }

        // 2. Find contact
        val contact = contactRepository.findContact(
            name = intent.contactName,
            appFilter = intent.appName
        )

        if (contact == null) {
            speak("I don't have ${intent.contactName} in my contacts")
            return CommandResult.Failed("Contact not found")
        }

        // 3. Get preferred app (or ask user)
        val appPackage = intent.appName?.let { resolveAppPackage(it) }
            ?: getPreferredApp(contact, intent.action)
            ?: return showAppSelectionDialog(contact, intent.action)

        // 4. Execute action
        return executeContactAction(contact, intent.action, appPackage)
    }

    private suspend fun executeContactAction(
        contact: UnifiedContact,
        action: ContactAction,
        appPackage: String
    ): CommandResult {
        // Get contact source for this app
        val source = contact.sources.find { it.appPackage == appPackage }
        if (source == null) {
            speak("${contact.primaryName} is not available in this app")
            return CommandResult.Failed("Contact not in app")
        }

        // Get learned UI flow from LearnApp database
        val uiFlow = uiFlowExecutor.getLearnedFlow(
            packageName = appPackage,
            flowType = action.flowType
        )

        if (uiFlow == null) {
            speak("I don't know how to ${action.verb} in this app yet")
            return CommandResult.Failed("UI flow not learned")
        }

        // Execute the flow:
        // 1. Open app
        // 2. Navigate to contacts list
        // 3. Scroll to position: source.positionInList
        // 4. Click contact
        // 5. Click action button (call/message/video)

        return try {
            uiFlowExecutor.execute(uiFlow, mapOf(
                "contact_position" to source.positionInList,
                "contact_name" to contact.primaryName
            ))

            // Update usage stats
            contactRepository.recordUsage(contact.id, action, appPackage)

            CommandResult.Success
        } catch (e: Exception) {
            speak("Sorry, I couldn't ${action.verb} ${contact.primaryName}")
            CommandResult.Failed(e.message ?: "Unknown error")
        }
    }
}
```

---

## Implementation Phases

### Phase 1: Contact Discovery & Scraping (Month 3)

**Tasks:**
1. ✅ Add contact list detection during exploration
2. ✅ Create contact consent dialog UI
3. ✅ Implement contact scraper (scroll + read + store)
4. ✅ Create `contact_sources` table
5. ✅ Test with WhatsApp, Teams, Phone app

**Deliverable:** Can learn contacts from any app

### Phase 2: Deduplication & Preferences (Month 4)

**Tasks:**
1. ✅ Create `unified_contacts` table
2. ✅ Implement fuzzy name matching
3. ✅ Build deduplication algorithm
4. ✅ Create preference dialog UI
5. ✅ Store and retrieve preferences
6. ✅ Test cross-app contact merging

**Deliverable:** "Call Mike" works with learned preferences

### Phase 3: Voice Command Integration (Month 5)

**Tasks:**
1. ✅ Create `ContactCommandHandler` in CommandManager
2. ✅ Implement command parsing
3. ✅ Build UI flow executor for contact actions
4. ✅ Test voice commands end-to-end
5. ✅ Handle edge cases (contact not found, app not installed, etc.)

**Deliverable:** Full voice command support

### Phase 4: System Integration & Export (Month 6)

**Tasks:**
1. ✅ Create `system_integration_settings` table
2. ✅ Implement upgrade offer logic (7 days + 100 contacts)
3. ✅ Add READ_CONTACTS integration
4. ✅ Implement VOS contact export format
5. ✅ Build import validator
6. ✅ Test device migration flow

**Deliverable:** v2.0 Release

---

## Success Metrics

### v2.0 Launch Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| Contact Learning Adoption | 60%+ | % of users who learn contacts after app exploration |
| Command Success Rate | 90%+ | % of contact commands that execute successfully |
| Preference Learning | 50%+ | % of users who set at least one preference |
| Average Contacts Learned | 100+ | Mean contacts per user across all apps |
| Cross-App Deduplication Accuracy | 95%+ | % of correctly matched contacts |
| Export/Import Success | 98%+ | % of successful migrations |

---

## Privacy & Security

### Data Storage

**What We Store:**
- ✅ Contact names (as visible in apps)
- ✅ Phone numbers (ONLY if visible in app UI)
- ✅ Email addresses (ONLY if visible in app UI)
- ✅ App preferences (which app per contact)
- ✅ Position in contact list (for scrolling)

**What We DON'T Store:**
- ❌ Full system contact database (unless user enables)
- ❌ Messages or call history
- ❌ Conversation content
- ❌ Social graph or relationships (unless user enables system contacts)

### Permissions

**Required:**
- ✅ Accessibility Service (already granted for LearnApp v1.x)

**Optional:**
- ⚠️ READ_CONTACTS (only for system integration in Phase 3)
  - Progressive upgrade offer
  - User can decline and still use learned contacts
  - Can be disabled anytime in settings

### Export Security

**v2.0:**
- ✅ Unencrypted `.vos` file
- ✅ SHA-256 checksum validation
- ⚠️ User warned about privacy

**v2.1 (Future):**
- 🔒 AES-256 encryption
- 🔒 Password protection
- 🔒 Secure cloud backup option

---

## Testing Plan

### Unit Tests

**Contact Scraper:**
- ✅ Scroll through 100+ contact list
- ✅ Read contact names correctly
- ✅ Handle empty lists
- ✅ Handle permission denials

**Deduplicator:**
- ✅ Match "Mike Johnson" = "mike johnson" = "Mike  Johnson"
- ✅ Don't match "Mike Johnson" ≠ "Michael Johnson"
- ✅ Merge metadata correctly
- ✅ Preserve app-specific data

**Preference Manager:**
- ✅ Save preference correctly
- ✅ Retrieve preference correctly
- ✅ Handle multiple actions per contact
- ✅ Override with explicit app name

### Integration Tests

**End-to-End:**
1. Learn WhatsApp contacts
2. Learn Teams contacts
3. Verify deduplication
4. Test voice command: "Call Mike Johnson"
5. Verify preference dialog appears
6. Select Teams
7. Verify Teams opens and calls Mike
8. Test again: "Call Mike Johnson"
9. Verify Teams used automatically (no dialog)

### Manual Testing

**Apps to Test:**
- ✅ WhatsApp
- ✅ Microsoft Teams
- ✅ Phone (system app)
- ✅ Messenger
- ✅ Signal
- ✅ Telegram

---

## Dependencies

### Required Modules

**Existing:**
- ✅ `modules/apps/LearnApp` (v1.1 - app exploration)
- ✅ `modules/apps/VoiceOSCore` (accessibility service)
- ✅ `modules/managers/CommandManager` (voice routing)
- ✅ `modules/libraries/UUIDCreator` (element registry)

**New:**
- 🆕 `ContactCommandHandler` (in CommandManager)
- 🆕 `ContactRepository` (database access)
- 🆕 `ContactDeduplicator` (fuzzy matching)
- 🆕 `UIFlowExecutor` (execute learned flows)

---

## Future Enhancements (v2.1+)

### v2.1 (Minor Update - Month 9)

**Smart Suggestions:**
- Partial name matching: "Call Mike" → "Did you mean Mike Johnson or Mike Smith?"
- Recent contact suggestions: "Call the person I talked to yesterday"

### v2.2 (Minor Update - Month 12)

**Group Commands:**
- "Message everyone in the engineering team"
- "Call the project team on Teams"

**Contact Labels:**
- "Call my manager"
- "Message my mom"
- Uses system contact relationships

### v3.0 (Major Update - Month 18)

**Context Awareness:**
- "Call who messaged me this morning"
- "Reply to Sarah" (from notification)

**Natural Language:**
- "Start a video call with the design team on Teams"

---

## Migration Guide (v1.1 → v2.0)

### Database Migration

**New Tables:**
```sql
-- Create new tables for v2.0
CREATE TABLE unified_contacts (...);
CREATE TABLE contact_sources (...);
CREATE TABLE system_integration_settings (...);
CREATE TABLE contact_export_history (...);
```

**No Breaking Changes:**
- ✅ v1.1 tables unchanged (`learned_apps`, `screen_states`, etc.)
- ✅ Existing functionality continues to work
- ✅ v2.0 is purely additive

### User Experience

**What Changes:**
- ✅ New dialog after app learning: "Learn contacts?"
- ✅ New voice commands work: "Call {name}"
- ✅ New settings section: "Contacts & Preferences"

**What Stays Same:**
- ✅ App learning flow unchanged
- ✅ Element-based voice commands still work
- ✅ Navigation graph still built

---

## Open Questions

### For User Decision

**Q1: System Contact Timing**
- Option A: Offer immediately during first app learning
- Option B: Progressive offer after 7 days (SPEC CHOICE)
- Option C: Never offer automatically

**Q2: Duplicate Name Handling**
- Option A: Always ask user
- Option B: Use most recently used (SPEC CHOICE)
- Option C: Use alphabetically first

**Q3: Export Encryption (v2.1)**
- Option A: Always encrypt by default
- Option B: Optional encryption (user choice)
- Option C: No encryption (user manages security)

---

## Appendix

### VOS Contact Export Format Specification

**Schema:** `vos-cnt-v1`
**Version:** `1.0.0`
**File Extension:** `.vos`
**MIME Type:** `application/x-voiceos-contacts`

**3-Letter Field Mapping:**
```
SCH = Schema
VER = Version
EXP = Export Metadata
TSP = Timestamp
DEV = Device
USR = User
CNT = Contacts Array
ID  = Contact ID
NAM = Name
NRM = Normalized Name
ACT = Actions
PH  = Phone Numbers
EM  = Emails
PRF = Preferences
SRC = Sources
PKG = Package
POS = Position
CHK = Checksum
SHA = SHA-256 Hash
```

**Why 3-Letter Format?**
- Reduces file size by ~40% vs full field names
- Faster parsing
- Easier compression
- Industry practice (see VCard, vCal, etc.)

---

**END OF SPECIFICATION**

**Status:** ✅ Ready for Review
**Next Step:** Review with team → Implement after v1.1 testing complete
**Estimated Implementation:** 4 months (Month 3-6)
**Priority:** High (Monetization enabler)

