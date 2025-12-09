# Command Synonym Settings - User Guide

**Document**: CommandSynonymSettingsActivity-User-Guide-5081220-V1.md
**Author**: VoiceOS Development Team
**Code-Reviewed-By**: Claude Code (IDEACODE v10.3)
**Created**: 2025-12-08
**Version**: 1.0

---

## Overview

The Command Synonym Settings UI allows users to view and manage voice command synonyms for all apps. This provides a manual alternative to voice-based renaming, offering full control over command naming.

---

## Launch the Settings UI

### From Code:
```kotlin
val intent = CommandSynonymSettingsActivity.createIntent(context)
startActivity(intent)
```

### From Voice:
```
User: "Open voice command settings"
```

---

## User Interface

### 1. App List Screen

**What you see:**
- List of all installed apps that have voice commands
- Each app shows:
  - App icon
  - App name
  - Number of commands

**What you can do:**
- Tap an app to view its commands
- Pull to refresh the app list
- Close the settings

**Example:**
```
┌─────────────────────────────────────┐
│ Voice Command Synonyms              │
├─────────────────────────────────────┤
│ ℹ️ Manage Command Synonyms          │
│ Select an app to view and edit...  │
├─────────────────────────────────────┤
│ [📱] DeviceInfo                     │
│      117 commands              →    │
├─────────────────────────────────────┤
│ [📱] Chrome                         │
│      28 commands               →    │
└─────────────────────────────────────┘
```

---

### 2. Command List Screen

**What you see:**
- Search bar at the top
- List of all commands for the selected app
- Each command shows:
  - Command text (e.g., "click button 1")
  - Synonyms (if any)

**What you can do:**
- Search/filter commands
- Tap a command to edit its synonyms
- Navigate back to app list

**Example:**
```
┌─────────────────────────────────────┐
│ ← Edit Synonyms                     │
├─────────────────────────────────────┤
│ 🔍 Search commands...                │
├─────────────────────────────────────┤
│ click button 1                      │
│ [save] [submit]                     │
├─────────────────────────────────────┤
│ click button 2                      │
│ No synonyms yet                     │
├─────────────────────────────────────┤
│ click tab 1                         │
│ [settings] [preferences]            │
└─────────────────────────────────────┘
```

---

### 3. Synonym Editor Dialog

**What you see:**
- Command being edited (read-only)
- Text field with current synonyms
- Save and Cancel buttons

**What you can do:**
- Add new synonyms (comma-separated)
- Edit existing synonyms
- Remove synonyms
- Save changes to database

**Example:**
```
┌─────────────────────────────────────┐
│ Edit Synonyms                       │
├─────────────────────────────────────┤
│ Command: click button 1             │
├─────────────────────────────────────┤
│ Synonyms (comma-separated)          │
│ ┌─────────────────────────────────┐ │
│ │ save, submit, send              │ │
│ │                                 │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│ ℹ️ Enter alternative names...       │
├─────────────────────────────────────┤
│            [Cancel]  [Save]         │
└─────────────────────────────────────┘
```

---

## How to Use

### Add Synonyms to a Command

1. **Launch Settings:**
   - Say "Open voice command settings" OR
   - Launch from app menu

2. **Select App:**
   - Scroll through app list
   - Tap the app you want to edit

3. **Find Command:**
   - Use search bar (optional)
   - Tap the command you want to edit

4. **Edit Synonyms:**
   - Type synonyms separated by commas
   - Example: `save, submit, send`

5. **Save Changes:**
   - Tap "Save" button
   - Synonyms are immediately active

---

## Search Functionality

**Search filters commands by:**
- Command text (e.g., "button")
- Existing synonyms (e.g., "save")

**Example:**
```
Search: "button"
Results:
- click button 1
- click button 2
- click top button

Search: "save"
Results:
- click button 1 (synonyms: save, submit)
```

---

## Synonym Usage

After adding synonyms, you can use ANY of these names:

**Example:**
```
Command: "click button 1"
Synonyms: "save, submit, send"

✅ User says: "Button 1"     → Executes
✅ User says: "Save"         → Executes
✅ User says: "Submit"       → Executes
✅ User says: "Send"         → Executes
```

**All names work equally!** The original command text is always preserved as a fallback.

---

## Material Design 3 Features

### Color Scheme
- **Dynamic colors** on Android 12+ (adapts to wallpaper)
- **Light theme** by default
- **Consistent theming** across all screens

### Components
- **Cards** for app/command items
- **Chips** for synonym display
- **Outlined text fields** for input
- **Rounded corners** throughout
- **Elevation** for depth perception

### Accessibility
- **Large tap targets** (48dp minimum)
- **High contrast** text
- **Clear labels** and descriptions
- **Icon + text** for all actions

---

## Empty States

### No Apps with Commands
**When:** No apps have been learned yet
**Shows:** Empty state with icon and message
**Action:** "Learn an app first to create voice commands"

### No Commands Found
**When:** Search returns no results
**Shows:** Empty state with search icon
**Action:** "Try a different search query"

---

## Tips & Tricks

### 💡 Tip 1: Multiple Synonyms
You can add as many synonyms as you want:
```
Synonyms: save, submit, send, confirm, apply, ok
```

### 💡 Tip 2: Short Names
Shorter synonyms are easier to say:
```
✅ Good: save, ok, done
❌ Avoid: save and close the document
```

### 💡 Tip 3: Natural Language
Use words you naturally say:
```
Command: "click settings button"
Synonyms: settings, preferences, options, config
```

### 💡 Tip 4: Search First
Use search to quickly find commands:
```
Search: "button 1" → Find all commands with "button 1"
```

### 💡 Tip 5: Voice Alternative
You can also rename via voice:
```
User: "Rename Button 1 to Save"
System: "Renamed to Save"
```

---

## Technical Details

### Database Updates
- Changes are saved immediately to SQLite database
- Uses `VoiceOSDatabaseManager.generatedCommands.update()`
- Thread-safe with coroutines

### State Management
- ViewModel handles all business logic
- StateFlow for reactive UI updates
- Automatic reload after changes

### Performance
- Lazy loading of app list
- Efficient filtering/searching
- Minimal memory footprint

---

## Troubleshooting

### Problem: No apps showing in list
**Solution:** Learn at least one app first using LearnApp

### Problem: Changes not saving
**Solution:** Check logcat for database errors, ensure app has storage permissions

### Problem: App crashes on launch
**Solution:** Verify VoiceOSDatabaseManager is initialized properly

### Problem: Synonyms not working in voice commands
**Solution:** Restart VoiceOS service or check RenameCommandHandler integration

---

## Related Features

### Voice-Based Renaming
Alternative to manual editing:
```
User: "Rename Button 1 to Save"
System: Adds "Save" as synonym
```
See: `RenameCommandHandler.kt`

### Contextual Hints
Automatic hints when screen has generated labels:
```
Overlay: "You can rename buttons by saying: Rename Button 1 to Save"
```
See: `RenameHintOverlay.kt`

### Command Discovery
View all commands with tutorial:
```
User: "Show commands on screen"
System: Opens CommandListActivity
```
See: `CommandListActivity.kt`

---

## Files Created

### Main Implementation
```
/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/settings/
├── CommandSynonymSettingsActivity.kt
└── CommandSynonymViewModel.kt
```

### Key Dependencies
- `VoiceOSDatabaseManager` - Database access
- `GeneratedCommandDTO` - Command data structure
- `IGeneratedCommandRepository` - Database operations

---

## Future Enhancements

### Phase 1 (Current)
- ✅ Manual synonym editing
- ✅ Search/filter
- ✅ Material Design 3

### Phase 2 (Planned)
- [ ] Bulk editing
- [ ] Export/import synonyms
- [ ] Synonym suggestions (AI-powered)
- [ ] Usage statistics per synonym

### Phase 3 (Future)
- [ ] Cloud sync
- [ ] Shared synonym libraries
- [ ] Voice-based synonym management
- [ ] Advanced filtering (by confidence, usage, etc.)

---

## Support

**Documentation:**
- LearnApp-On-Demand-Command-Renaming-5081220-V2.md
- RenameCommandHandler-Integration-Guide.md

**Code Examples:**
- See `CommandListActivity.kt` for similar UI patterns
- See `RenameCommandHandler.kt` for synonym logic

**Contact:**
- Developer: VoiceOS Development Team
- Code Review: Claude Code (IDEACODE v10.3)

---

**End of User Guide**
