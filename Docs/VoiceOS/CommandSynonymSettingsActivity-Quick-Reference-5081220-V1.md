# Command Synonym Settings - Quick Reference

**Document**: CommandSynonymSettingsActivity-Quick-Reference-5081220-V1.md
**Created**: 2025-12-08
**Version**: 1.0

---

## Launch

```kotlin
val intent = CommandSynonymSettingsActivity.createIntent(context)
startActivity(intent)
```

---

## Files

```
/Modules/VoiceOS/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/settings/
├── CommandSynonymSettingsActivity.kt   (23KB, ~700 lines)
└── CommandSynonymViewModel.kt          (8.1KB, ~240 lines)
```

---

## Dependencies

```gradle
// Already in project
implementation "androidx.compose.material3:material3:1.1.2"
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2"

// NEW - Add if missing
implementation "com.google.accompanist:accompanist-flowlayout:0.32.0"
```

---

## Key Classes

```kotlin
// ViewModel
class CommandSynonymViewModel(
    database: VoiceOSDatabaseManager,
    packageManager: PackageManager
)

// Data
data class AppInfo(
    packageName: String,
    name: String,
    icon: Drawable,
    commandCount: Int
)

// State
sealed class SynonymEditorState {
    object Hidden
    data class Editing(command: GeneratedCommandDTO)
}
```

---

## UI Flow

```
App List → Command List → Synonym Editor
   ↓           ↓              ↓
  Tap        Tap            Edit
  App      Command        Synonyms
```

---

## Database Operations

```kotlin
// Read
database.generatedCommands.getByPackage(packageName)

// Update
database.generatedCommands.update(command.copy(synonyms = "save,submit,send"))
```

---

## Material Design 3 Colors

```kotlin
primaryContainer     → Top bar, info cards
secondaryContainer   → Synonym chips, help
surface              → Command cards
surfaceVariant       → Dialog command display
```

---

## Preview Composables

```kotlin
PreviewInfoCard()        // Info banner
PreviewEmptyAppsState()  // Empty state
PreviewHelpFooter()      // Help text
```

---

## Testing Checklist

- [ ] Launch activity
- [ ] View app list
- [ ] Select app
- [ ] View commands
- [ ] Search commands
- [ ] Edit synonyms
- [ ] Save changes
- [ ] Verify database update
- [ ] Test empty states
- [ ] Test back navigation

---

## Common Issues

**No apps showing:**
→ Learn an app first

**Changes not saving:**
→ Check database permissions

**FlowRow not found:**
→ Add accompanist-flowlayout dependency

---

## Related Files

```
RenameCommandHandler.kt              → Voice-based renaming
CommandListActivity.kt               → Similar UI pattern
VoiceOSDatabaseManager.kt           → Database access
GeneratedCommandDTO.kt              → Data structure
```

---

## Documentation

- **User Guide**: CommandSynonymSettingsActivity-User-Guide-5081220-V1.md
- **Implementation**: CommandSynonymSettingsActivity-Implementation-Summary-5081220-V1.md
- **Design Spec**: LearnApp-On-Demand-Command-Renaming-5081220-V2.md

---

**Status**: ✅ READY FOR INTEGRATION
