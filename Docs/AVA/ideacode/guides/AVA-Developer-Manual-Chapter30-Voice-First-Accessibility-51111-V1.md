# Developer Manual - Chapter 30: Voice-First Accessibility

**Date:** 2025-11-11
**Author:** AVA Development Team
**Status:** Production Standard
**Related Standard:** `/globalprogrammingstandards/VOICE-FIRST-ACCESSIBILITY.md`

---

## Table of Contents

1. [Overview](#overview)
2. [Core Principles](#core-principles)
3. [Mandatory Requirements](#mandatory-requirements)
4. [Implementation Guidelines](#implementation-guidelines)
5. [Teach AVA Feature Implementation](#teach-ava-feature-implementation)
6. [Testing Requirements](#testing-requirements)
7. [Compliance Checklist](#compliance-checklist)
8. [Common Anti-Patterns](#common-anti-patterns)
9. [Integration with VoiceOS](#integration-with-voiceos)
10. [References](#references)

---

## 1. Overview

### Purpose

AVA is designed as a **voice-first application** where all functionality must be accessible primarily through voice commands, with visual/touch interfaces serving as secondary access methods. This chapter documents the voice-first accessibility standard and its implementation across AVA.

### Design Philosophy

> **"Voice first, visual second, gestures as bonus. Always provide multiple access methods."**

This approach ensures AVA is accessible to:
- Users with visual impairments
- Users with motor disabilities
- Users in hands-free scenarios
- Users multitasking
- All users requiring accessibility features

### Scope

This chapter applies to:
- All AVA features and UI components
- All VoiceOS-integrated applications
- All voice-enabled Augmentalis products

---

## 2. Core Principles

### Principle 1: Voice Commands Are Primary

Every feature MUST have a voice command as its primary access method.

**Example:**
```kotlin
// Feature: Teach AVA
// ✅ PRIMARY: Voice commands
val voiceCommands = listOf(
    "teach ava",
    "train ava",
    "teach this",
    "add intent",
    // ... minimum 5-6 synonyms
)

// ✅ SECONDARY: Visible button
IconButton(onClick = { viewModel.activateTeachMode() }) {
    Icon(Icons.Filled.School, contentDescription = "Teach AVA")
}

// ✅ OPTIONAL: Gesture enhancement
Box(modifier = Modifier.combinedClickable(
    onLongClick = { viewModel.activateTeachMode() }
))
```

### Principle 2: Visual Buttons Are Mandatory

Every feature MUST also have a visible, clickable button or control.

**Why:**
- Voice recognition may fail in noisy environments
- Users may have speech disabilities
- Provides visual feedback and discoverability
- Enables traditional touch interaction

### Principle 3: Gestures Are Optional Enhancements

Gestures (long-press, swipe, etc.) MAY be provided as additional convenience, but MUST NOT be the primary or only access method.

---

## 3. Mandatory Requirements

### Requirement 1: Multiple Access Methods

**EVERY feature MUST provide:**

1. **Voice Command** (Primary)
   - Minimum 5-6 synonyms for discoverability
   - Natural language phrasing
   - Intent registered in NLU system
   - Action handler implemented

2. **Visible Button/Control** (Secondary)
   - Always visible in UI (not hidden in menu unless menu also voice-accessible)
   - Minimum 48dp touch target (WCAG AA)
   - Clear label and icon
   - Proper contrast ratio (4.5:1 minimum)
   - TalkBack support with contentDescription

3. **Optional Gesture** (Tertiary)
   - Bonus convenience only
   - Not required for compliance

### Requirement 2: Intent Registration

All voice commands MUST be registered as intents in the NLU system.

**Files to Update:**
```
1. BuiltInIntents.kt - Add intent constant
2. IntentTemplates.kt - Add response template
3. intent_examples.json - Add training examples (5-9 variants)
4. IntentActionHandler (if applicable) - Implement action
```

### Requirement 3: Accessibility Support

All buttons MUST have proper accessibility support:

```kotlin
IconButton(
    onClick = { /* action */ },
    enabled = isEnabled,
    modifier = Modifier.semantics {
        contentDescription = "Button name - What it does"
    }
) {
    Icon(
        imageVector = Icons.Filled.Icon,
        contentDescription = "Button name",
        tint = if (isEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }
    )
}
```

---

## 4. Implementation Guidelines

### Step 1: Define Voice Commands

**Minimum 5-6 Synonyms Required**

```kotlin
// BuiltInIntents.kt
object BuiltInIntents {
    const val TEACH_AVA = "teach_ava"

    fun getExampleUtterances(intent: String): List<String> {
        return when (intent) {
            TEACH_AVA -> listOf(
                "Teach AVA",              // Primary
                "Train AVA",              // Synonym 1
                "Teach this",             // Synonym 2
                "Add intent",             // Synonym 3
                "I want to train you",    // Synonym 4
                "Learn this command",     // Synonym 5
                "Teach you a new intent", // Synonym 6
                "Show me how",            // Synonym 7
                "I want to teach you something" // Synonym 8
            )
            // ...
        }
    }
}
```

### Step 2: Add Intent Examples JSON

```json
// intent_examples.json
{
  "teach_ava": [
    "Teach AVA",
    "Train AVA",
    "Teach this",
    "Add intent",
    "I want to train you",
    "Learn this command",
    "Teach you a new intent",
    "Show me how",
    "I want to teach you something"
  ]
}
```

### Step 3: Add Response Template

```kotlin
// IntentTemplates.kt
object IntentTemplates {
    private val templates = mapOf(
        "teach_ava" to "I'm ready to learn! What would you like to teach me?",
        // ...
    )
}
```

### Step 4: Add Visible Button

**Preferred Locations (in order):**

1. **Top App Bar** - For primary features
```kotlin
TopAppBar(
    title = { Text("AVA AI") },
    actions = {
        IconButton(
            onClick = { viewModel.activateTeachMode() },
            enabled = isEnabled,
            modifier = Modifier.semantics {
                contentDescription = "Teach AVA - Train AVA with new intents"
            }
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = "Teach AVA"
            )
        }
    }
)
```

2. **Bottom Bar** - For frequently used features
3. **Floating Action Button** - For primary action
4. **Inline Button** - For contextual features

### Step 5: Add Optional Gesture (If Desired)

```kotlin
// MessageBubble.kt
Box(
    modifier = Modifier.combinedClickable(
        onClick = { /* normal action */ },
        onLongClick = { onTeachAva() } // Bonus convenience
    )
) {
    // Message content
}
```

---

## 5. Teach AVA Feature Implementation

### Overview

The "Teach AVA" feature is a perfect example of voice-first accessibility compliance. It demonstrates all three access methods working together.

### Architecture

```
User Intent
    ├─ Voice Command (Primary)
    │   ├─ "teach ava"
    │   ├─ "train ava"
    │   ├─ "teach this"
    │   └─ ... (9 total synonyms)
    │
    ├─ Button Click (Secondary)
    │   └─ School icon in TopAppBar
    │
    └─ Long Press (Optional)
        └─ Any message bubble
```

### Implementation Details

**File:** `ChatScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AVA AI") },
                actions = {
                    // Teach AVA button - Voice-first accessibility compliant
                    // See: globalprogrammingstandards/VOICE-FIRST-ACCESSIBILITY.md
                    IconButton(
                        onClick = {
                            val lastMessage = messages.lastOrNull()
                            if (lastMessage != null) {
                                viewModel.activateTeachMode(lastMessage.id)
                            }
                        },
                        enabled = messages.isNotEmpty(),
                        modifier = Modifier.semantics {
                            contentDescription = "Teach AVA - Train AVA with new intents. Send a message first to enable."
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "Teach AVA",
                            tint = if (messages.isNotEmpty()) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Chat content
    }
}
```

### Voice Command Flow

```
User: "teach ava"
    ↓
IntentClassifier
    ↓
Intent: teach_ava (confidence: 85%)
    ↓
IntentTemplates
    ↓
Response: "I'm ready to learn! What would you like to teach me?"
    ↓
ChatViewModel.activateTeachMode()
    ↓
TeachAvaBottomSheet opens
```

### Button Click Flow

```
User: Clicks School icon
    ↓
IconButton onClick
    ↓
viewModel.activateTeachMode(lastMessage.id)
    ↓
TeachAvaBottomSheet opens with context
```

### Long Press Flow

```
User: Long-presses message bubble
    ↓
combinedClickable.onLongPress
    ↓
onTeachAva() callback
    ↓
viewModel.activateTeachMode(message.id)
    ↓
TeachAvaBottomSheet opens with message context
```

---

## 6. Testing Requirements

### Voice Command Testing

#### Test 1: Quiet Environment
- **Setup:** Test in quiet room
- **Action:** Speak each voice command synonym
- **Expected:** Works 95%+ of attempts
- **Test Cases:** All 9 synonyms for teach_ava

#### Test 2: Noisy Environment
- **Setup:** Background conversation or music
- **Action:** Speak voice commands
- **Expected:** Works 80%+ of attempts
- **Notes:** May require multiple attempts

#### Test 3: Accent/Dialect Variations
- **Setup:** Test with different English accents
- **Action:** Speak commands with various accents
- **Expected:** Works 75%+ of attempts
- **Accents:** US, UK, Australian, Indian, non-native speakers

#### Test 4: Multiple Synonyms
- **Setup:** Try at least 5 different phrasings
- **Action:** Speak various synonyms
- **Expected:** All trigger same action
- **Example:** "teach ava", "train ava", "teach this", etc.

### Button/Visual Testing

#### Test 1: TalkBack (Screen Reader)
```bash
# Enable TalkBack on device
adb shell settings put secure enabled_accessibility_services \
  com.google.android.marvin.talkback/.TalkBackService

# Navigate to button
# Expected: Announces "Teach AVA - Train AVA with new intents"
# Expected: Button is activatable
```

#### Test 2: Touch Target Size
- **Measure:** Button touch target
- **Expected:** Minimum 48dp x 48dp
- **Spacing:** Minimum 8dp between adjacent buttons

#### Test 3: Contrast Ratio
- **Tool:** Use color contrast analyzer
- **Expected:** Text 4.5:1 minimum (WCAG AA)
- **Expected:** Icons 3:1 minimum

#### Test 4: Discoverability
- **Setup:** Show app to new user
- **Action:** Ask them to find "Teach AVA" feature
- **Expected:** Found within 10 seconds
- **No hints allowed**

---

## 7. Compliance Checklist

### Design Phase
- [ ] Voice command defined
- [ ] Minimum 5-6 synonyms identified
- [ ] Button mockup created
- [ ] Button location determined
- [ ] User flow documented
- [ ] Accessibility reviewed

### Implementation Phase
- [ ] Voice command intent registered in BuiltInIntents.kt
- [ ] Intent examples added to intent_examples.json
- [ ] Response template added to IntentTemplates.kt
- [ ] Action handler implemented (if applicable)
- [ ] Button added to UI
- [ ] TalkBack contentDescription added
- [ ] Touch target size verified (48dp minimum)
- [ ] Color contrast verified (4.5:1 minimum)

### Testing Phase
- [ ] Voice command tested (quiet environment)
- [ ] Voice command tested (noisy environment)
- [ ] Multiple synonyms tested
- [ ] Accent/dialect testing completed
- [ ] Button tested (touch)
- [ ] Button tested (TalkBack)
- [ ] Discovery test passed (new user finds feature <10s)

### Documentation Phase
- [ ] Voice commands listed in user guide
- [ ] Button location shown in screenshots
- [ ] Tutorial created (if applicable)
- [ ] Developer manual chapter updated

---

## 8. Common Anti-Patterns

### Anti-Pattern 1: Hidden Features ❌

**BAD:**
```kotlin
// Feature only accessible via obscure dropdown menu
DropdownMenu(expanded = showMenu) {
    DropdownMenuItem(onClick = { /* teach ava */ }) {
        Text("Teach AVA")
    }
}
// No voice command, no visible button
```

**FIX:**
- Add voice command with 5+ synonyms
- Add always-visible button in TopAppBar
- Menu can remain as tertiary access method

### Anti-Pattern 2: Voice Command Without Visual Fallback ❌

**BAD:**
```kotlin
// Only voice command, no button
// Users can't discover feature visually
// Fails in noisy environments
```

**FIX:**
- Add visible button with icon and label
- Ensure button is always visible (not in submenu)

### Anti-Pattern 3: Complex Multi-Step Voice Commands ❌

**BAD:**
```kotlin
// User must say: "open menu, then select teach, then confirm"
// Too many steps, high cognitive load
```

**FIX:**
- Single direct command: "teach ava"
- Maximum 1-4 words per command

### Anti-Pattern 4: Insufficient Synonyms ❌

**BAD:**
```kotlin
val teachCommands = listOf(
    "teach ava"  // Only 1 command
)
```

**FIX:**
```kotlin
val teachCommands = listOf(
    "teach ava",
    "train ava",
    "teach this",
    "add intent",
    "i want to train you",
    "learn this command"
    // Minimum 5-6 synonyms
)
```

### Anti-Pattern 5: Gesture as Primary Access ❌

**BAD:**
```kotlin
// Long-press is ONLY way to access feature
Box(modifier = Modifier.combinedClickable(
    onLongClick = { viewModel.activateTeachMode() }
    // No onClick, no button, no voice command
))
```

**FIX:**
- Voice command as primary
- Button as secondary
- Long-press as optional bonus

---

## 9. Integration with VoiceOS

### VoiceOS Command Database

VoiceOS provides 87 pre-built voice commands across 19 categories:

**Categories:**
- Cursor control
- Gestures
- Navigation
- Text editing
- System control
- Media control
- Accessibility
- ... and more

### Integration Strategy

AVA can leverage VoiceOS commands:

```kotlin
class HybridCommandProcessor(
    private val avaIntentClassifier: IntentClassifier,
    private val voiceOSCommandManager: VoiceCommandManager
) {
    suspend fun process(utterance: String): CommandResult {
        // Try AVA's conversational intents first
        val avaIntent = avaIntentClassifier.classify(utterance)

        if (avaIntent.confidence > 0.7) {
            return executeAvaIntent(avaIntent)
        }

        // Fallback to VoiceOS UI/accessibility commands
        val voiceOSCommand = voiceOSCommandManager.findCommand(utterance)
        if (voiceOSCommand != null) {
            return executeVoiceOSCommand(voiceOSCommand)
        }

        // Unknown - prompt to teach
        return CommandResult.Unknown
    }
}
```

### Command Sharing

Users can teach commands from either system:
- **AVA commands:** Conversational, context-aware (e.g., "remind me to call mom")
- **VoiceOS commands:** UI/accessibility control (e.g., "select", "scroll down")

Both are learned through the same "Teach AVA" interface.

---

## 10. References

### Internal Documentation
- `/globalprogrammingstandards/VOICE-FIRST-ACCESSIBILITY.md` - Full standard specification
- `Developer-Manual-Chapter31-TRM-Integration.md` - Advanced reasoning capabilities
- `Developer-Manual-Chapter28-RAG.md` - Retrieval augmented generation

### External Standards
- **WCAG 2.1 AA** - Web Content Accessibility Guidelines
- **Material Design 3** - Accessibility guidelines
- **Android Accessibility** - TalkBack, Switch Access documentation
- **VoiceOS Command Patterns** - Voice command design best practices

### Code Files
- `ChatScreen.kt` - Teach AVA button implementation (lines 88-119)
- `BuiltInIntents.kt` - Intent definitions and examples (lines 229-239)
- `IntentTemplates.kt` - Response templates (line 47)
- `intent_examples.json` - Training data (lines 58-68)
- `TeachAvaBottomSheet.kt` - Teaching UI component

### Testing Resources
- TalkBack documentation: https://support.google.com/accessibility/android/answer/6283677
- Color contrast analyzer: https://www.tpgi.com/color-contrast-checker/
- WCAG guidelines: https://www.w3.org/WAI/WCAG21/quickref/

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2025-11-11 | AVA Team | Initial chapter creation |
| | | | Voice-first standard documented |
| | | | Teach AVA implementation example |
| | | | Testing requirements defined |

---

## Contact

**Questions or feedback?**
- Email: manoj@ideahq.net
- Project: AVA Voice Assistant
- Repository: github.com/mkjhawar/AVA

---

**Remember: Voice first, visual second, gestures as bonus. Always provide multiple access methods.**

---

**END OF CHAPTER 30**
