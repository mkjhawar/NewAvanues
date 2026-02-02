# Deprecated CommandManager Files

**Moved:** 2025-12-15
**Reason:** Refactored out of CommandManager in VoiceOS-Development

---

## Files

### NluIntegration.kt
**Original Path:** `src/main/java/com/augmentalis/commandmanager/nlu/NluIntegration.kt`

**Purpose:**
- Bridged UnifiedNluService with CommandManager
- Provided intent-to-command conversion
- Handled NLU service initialization

**Why Removed:**
- CommandManager refactored to not use NLU directly
- Deleted in VoiceOS-Development branch
- Reintroduced accidentally during NLU-Development merge

**Replacement:**
- Modules/Shared/NLU - UnifiedNluService for shared intent classification
- Modules/AVA/NLU - UnifiedNluBridge for AVA-specific integration
- VoiceOS apps should integrate with Shared NLU directly, not through CommandManager

---

### CommandContextAdapter.kt
**Original Path:** `src/main/java/com/augmentalis/commandmanager/context/CommandContextAdapter.kt`

**Purpose:**
- Migration adapter between legacy sealed class CommandContext and unified data class
- Converted context types (ActivityTypes, TimeOfDay, AppCategories, LocationTypes)
- Supported composite context merging

**Why Removed:**
- Part of the NLU integration that was refactored out
- Required types (ActivityTypes, etc.) don't exist in current CommandContext model
- Deleted in VoiceOS-Development branch

**Replacement:**
- CommandContext in command-models module is now simpler
- Context conversion handled at app level if needed
- No nested type constants in current architecture

---

## Restoration

If these files are needed:

```bash
# Copy back from deprecated folder
cp Modules/VoiceOS/managers/CommandManager/deprecated/NluIntegration.kt \
   Modules/VoiceOS/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/nlu/

# Add Shared NLU dependency to build.gradle.kts
implementation(project(":Modules:Shared:NLU"))

# Extend CommandContext in command-models to include missing fields
```

---

## Notes

- These files were part of an earlier NLU integration architecture
- VoiceOS-Development removed them as part of a larger refactor
- Preserved here for reference and potential future use
- See git history for full implementation details

---

**Related Branches:**
- VoiceOS-Development: Files deleted
- NLU-Development: Had old versions of these files
- Avanues-Development: Received stale files during merge (now moved here)
