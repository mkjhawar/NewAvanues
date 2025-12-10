# Manual Command Assignment - User Guide

**Feature:** Manual Command Assignment (VOS-META-001)
**Version:** 1.0 (Phase 2 Complete)
**Platform:** Android
**Last Updated:** 2025-12-03
**Status:** Production Ready

---

## What is Manual Command Assignment?

Manual Command Assignment allows you to teach VoiceOS custom voice commands for any button or element on your screen, even if the app doesn't have proper accessibility support. When VoiceOS can't automatically create voice commands for poorly-designed apps, you can create your own!

---

## When to Use Manual Command Assignment

Use manual command assignment when:

- VoiceOS learned an app but some elements can't be voice-controlled
- You see generic names like "button_1" or "framelayout_2" after learning
- An app has buttons with no text or labels
- You want custom voice commands instead of auto-generated ones
- Third-party apps have poor accessibility implementation

---

## How to Use Manual Command Assignment

### Quick Start

1. **Learn an app** with VoiceOS (exploration or JIT learning)
2. **See overlay** showing elements that need voice commands
3. **Tap an element** on the overlay to start assignment
4. **Record your voice command** (e.g., "Submit form")
5. **Test it** - Say the command to activate the element!

### Step-by-Step Guide

#### Step 1: Trigger Learning

**Option A: Exploration Mode**
- Open VoiceOS
- Say "Explore this app"
- Navigate through the app
- Say "Finish learning"

**Option B: JIT (Just-In-Time) Learning**
- Use any app normally
- VoiceOS automatically learns elements as you tap them
- No voice command needed

#### Step 2: View Elements Needing Commands

After learning completes, VoiceOS automatically shows an overlay:

```
┌──────────────────────────────────────┐
│ 🎤 Voice Commands Needed             │
│ 5 elements have generic commands     │
│                                      │
│ [Highlighted Button]                 │
│   • Generic: button_1                │
│   • Type: android.widget.Button      │
│   • Tap to assign voice command      │
│                                      │
│ [Done] [Assign Commands]             │
└──────────────────────────────────────┘
```

**What you'll see:**
- Count of elements needing commands
- Each element highlighted on screen
- Generic name (e.g., "button_1")
- Element type (e.g., "Button")

#### Step 3: Tap Element to Assign Command

**To assign a voice command:**
1. Tap the highlighted element
2. Command Assignment Dialog appears
3. You'll see:
   - Element preview (screenshot)
   - Current generic alias
   - Record button

#### Step 4: Record Your Voice Command

**Recording process:**
1. Tap "🎤 Record Command" button
2. Recording starts automatically (3-second countdown)
3. Say your command clearly (e.g., "Submit form")
4. Recording stops after 3 seconds
5. Review your recording (playback available)

**Tips for good commands:**
- **Clear pronunciation:** "Next page" not "nex paj"
- **Unique commands:** Don't use existing commands
- **Short phrases:** 1-4 words work best
- **Natural language:** What you'd actually say

#### Step 5: Save and Test

**After recording:**
1. Tap "Save Command"
2. Command is saved as synonym for the element
3. Dialog shows "Command saved successfully!"
4. Try it: Say your new command!

**Testing:**
- Say your command aloud
- VoiceOS should activate the element
- If it doesn't work, re-record with clearer pronunciation

---

## Advanced Features

### Multiple Commands Per Element

**You can assign multiple voice commands to the same element:**

Example for a "Submit" button:
- "Submit"
- "Submit form"
- "Send"
- "Submit this"

**How to add multiple:**
1. Assign first command (e.g., "Submit")
2. Tap the element again
3. Assign second command (e.g., "Send")
4. Repeat as needed

All commands will activate the same element!

### Replacing Auto-Generated Commands

**Sometimes VoiceOS generates commands you don't like:**

Example: Auto-generated "Unnamed button 3"
You want: "Settings button"

**How to replace:**
1. Tap the element in the overlay
2. Record your preferred command
3. Old generic command is replaced
4. Your custom command becomes primary

### Developer Mode: Element Quality Indicator

**For advanced users and developers:**

Enable Developer Mode in VoiceOS Settings to see:
- **Green highlight**: Element has good metadata
- **Yellow highlight**: Element has partial metadata
- **Red highlight**: Element has no metadata (needs manual command)

**How to enable:**
1. Open VoiceOS Settings
2. Tap "Developer Options"
3. Enable "Show Element Quality Overlay"

---

## Settings

### Voice Command Language

**Default:** English (US)
**Supported:** English (US), English (UK), Spanish, French, German

**To change language:**
1. Open VoiceOS Settings
2. Tap "Voice Recognition"
3. Select "Recognition Language"
4. Choose your preferred language

### Recording Quality

**Default:** 16kHz, 16-bit mono (high quality)

**To adjust quality:**
1. Open VoiceOS Settings
2. Tap "Voice Training"
3. Select "Recording Quality"
4. Choose: Low, Medium, High, or Auto

**Recommended:** Auto (adapts to your device)

### Auto-Show Overlay

**Default:** Enabled (overlay shows automatically after learning)

**To disable auto-show:**
1. Open VoiceOS Settings
2. Tap "Learning Options"
3. Toggle "Show Command Overlay" off

**Manual trigger:** Say "Show command overlay" or tap "Review Commands" in VoiceOS

---

## Tips and Tricks

### Tip 1: Record in Quiet Environment
Background noise can reduce command accuracy. Record commands in a quiet room for best results.

### Tip 2: Use Consistent Pronunciation
If you say "Submit form" during recording, say exactly that when using the command. Variations like "Submit the form" might not match.

### Tip 3: Avoid Ambiguous Commands
Don't use "OK" or "Next" for specific buttons - these are too generic. Use "Submit form" or "Next page" instead.

### Tip 4: Test Immediately After Assignment
Test your new command right away while still in the app. This confirms it works before you rely on it.

### Tip 5: Group Related Commands
For forms, use consistent patterns:
- "Submit form"
- "Cancel form"
- "Reset form"

This makes commands easier to remember!

### Tip 6: Re-record If Not Working
If a command doesn't trigger reliably:
1. Tap the element again
2. Re-record with clearer pronunciation
3. Test in quiet environment

---

## Troubleshooting

### Q: Overlay doesn't appear after learning
**A:** Check these:
1. Make sure "Show Command Overlay" is enabled in Settings
2. Verify learning actually completed (check status in VoiceOS)
3. Try manual trigger: Say "Show command overlay"
4. Check if accessibility service is running

### Q: Recording doesn't start
**A:** Try these steps:
1. Check microphone permission (Settings → Apps → VoiceOS → Permissions)
2. Make sure another app isn't using microphone
3. Restart VoiceOS accessibility service
4. Restart the device if problem persists

### Q: My command doesn't trigger the element
**A:** Common causes:
1. **Pronunciation mismatch:** Re-record with exact pronunciation you'll use
2. **Background noise:** Re-record in quiet environment
3. **Ambiguous command:** Use more specific phrase
4. **Language mismatch:** Verify recognition language matches recording language

### Q: Element isn't highlighted in overlay
**A:** Reasons and solutions:
1. **Already has voice command:** Element might have auto-generated command
2. **Not visible:** Element might be off-screen (scroll to find it)
3. **Learning incomplete:** Re-learn the app
4. **Element changed:** App updated - re-learn to refresh

### Q: Can I delete a manually assigned command?
**A:** Yes, two ways:
1. **Individual:** Long-press element → "Delete command" → Confirm
2. **Bulk:** VoiceOS Settings → "Manage Commands" → Select commands → Delete

### Q: How many commands can I assign?
**A:** No hard limit, but practical limits:
- Per element: 10 commands (more gets confusing)
- Total: Depends on device storage (typically 10,000+)
- Performance: No degradation up to 1,000 commands

### Q: Do commands work offline?
**A:** Partially:
- **Voice recognition:** Requires internet for accurate speech-to-text
- **Command matching:** Works offline once commands are assigned
- **Element activation:** Works offline

**Recommendation:** Assign commands while online for best recognition accuracy.

---

## Related Features

- **Exploration Mode** - Learn entire apps systematically
- **JIT Learning** - Learn elements as you tap them
- **Voice Training** - Improve recognition accuracy for your voice
- **Command Synonyms** - Multiple ways to trigger same action
- **Accessibility Service** - Powers VoiceOS screen reading

---

## Accessibility Features

### For Users with Motor Impairments
- Hands-free voice command recording
- Large touch targets for element selection
- Voice-only workflow (no tapping required with RealWear)

### For Users with Visual Impairments
- TalkBack announces overlay elements
- Voice feedback confirms command saved
- Audio playback to verify recording

### For Users with Cognitive Disabilities
- Visual overlay shows exactly what needs commands
- Step-by-step recording dialog
- Confirmation dialogs prevent accidental deletion
- Clear success/error messages

---

## Privacy & Security

### What Data is Recorded?

**Voice recordings:**
- Stored locally on device (not uploaded to cloud)
- Encrypted with Android keystore
- Associated only with element UUID
- No personal information included

**Element data:**
- Only UI element metadata (bounds, type, package)
- No sensitive content (passwords, credit cards, etc.)
- No screenshots (except for manual assignment preview)

### How to Delete Your Data?

**Individual commands:**
1. Long-press element in overlay
2. Tap "Delete command"
3. Confirm deletion
4. Voice recording permanently deleted

**All commands:**
1. VoiceOS Settings → "Privacy"
2. Tap "Delete All Voice Commands"
3. Enter confirmation code
4. All recordings permanently deleted

**App uninstall:**
- Uninstalling VoiceOS deletes all data
- No cloud backups (fully local)

---

## Version History

### v1.0 (2025-12-03) - Phase 2 Complete
- ✅ Speech recognition integration
- ✅ Command assignment dialog UI
- ✅ Voice recording with 3-second capture
- ✅ Playback and review functionality
- ✅ Command synonym creation
- ✅ Success/error feedback

### v0.1 (2025-12-02) - Phase 1 Complete
- Database schema for command synonyms
- Element metadata quality scoring
- Generic command detection

### Planned for v1.1
- Multi-language support (Spanish, French, German)
- Voice command editing (without re-recording)
- Batch command assignment
- Export/import command sets
- Cloud sync (optional)

---

## Need More Help?

- **Developer Manual:** [Manual Command Assignment Implementation](/docs/manuals/developer/features/manual-command-assignment-implementation-251203.md)
- **Voice Training Guide:** [Voice Training](/docs/manuals/user/features/voice-training.md)
- **Exploration Mode Guide:** [Exploration Mode](/docs/manuals/user/features/exploration-mode.md)
- **Support:** help@voiceos.com
- **Community Forum:** https://community.voiceos.com

---

**Enjoy voice-controlling every app, even the poorly-designed ones!** 🎤
