# Voice-First Accessibility Standard

**Version:** 1.0.0
**Date:** 2025-11-10
**Status:** MANDATORY - All AVA/VoiceOS applications
**Applies To:** AVA, VoiceOS, VoiceAvanue, and all voice-enabled applications

---

## Core Principle

**All functionality MUST be accessible via voice commands first, with visual/touch interfaces as secondary access methods.**

Voice-first applications are designed for users with:
- Visual impairments
- Motor disabilities
- Hands-free operation requirements
- Multitasking scenarios
- Accessibility needs

---

## Mandatory Requirements

### 1. **Primary Access Method: Voice Command**

Every feature MUST have a voice command as its primary access method.

**✅ CORRECT:**
```kotlin
// Feature: Teach AVA
// Voice commands: "teach ava", "train ava", "teach this", "add intent"
// Visual fallback: Button in UI
```

**❌ INCORRECT:**
```kotlin
// Feature: Teach AVA
// Only accessible via: Long-press gesture
// No voice command available
```

### 2. **Secondary Access Method: Visible Button**

Every feature MUST also have a visible, clickable button/control.

**Why:**
- Voice recognition may fail in noisy environments
- Users may have speech disabilities
- Provides visual feedback and discoverability
- Enables traditional touch interaction

**✅ CORRECT:**
```kotlin
// Top bar or bottom bar button
Button(onClick = { viewModel.activateTeachMode() }) {
    Icon(Icons.Filled.School, contentDescription = "Teach AVA")
    Text("Teach")
}
```

**❌ INCORRECT:**
```kotlin
// Hidden behind long-press gesture
Box(modifier = Modifier.combinedClickable(
    onLongClick = { viewModel.activateTeachMode() }
))
```

### 3. **Gesture Access: Optional Enhancement**

Gestures (long-press, swipe, etc.) MAY be provided as additional convenience, but MUST NOT be the primary or only access method.

**✅ CORRECT:**
- Voice command: Primary
- Button: Always visible
- Long-press: Bonus convenience

**❌ INCORRECT:**
- Voice command: None
- Button: Hidden
- Long-press: Only access method

---

## Implementation Checklist

For EVERY feature, ensure:

- [ ] **Voice Command Defined**
  - [ ] Command is intuitive and natural
  - [ ] Multiple synonyms provided (at least 3)
  - [ ] Intent registered in NLU system
  - [ ] Action handler implemented

- [ ] **Visible Button/Control**
  - [ ] Always visible in UI (not hidden in menu unless menu also voice-accessible)
  - [ ] Minimum 48dp touch target (WCAG AA)
  - [ ] Clear label and icon
  - [ ] Proper contrast ratio (4.5:1 minimum)

- [ ] **Documentation**
  - [ ] Voice commands documented in user guide
  - [ ] Button location shown in screenshots/tutorial
  - [ ] Intent examples added to training data

- [ ] **Testing**
  - [ ] Voice command works in quiet environment
  - [ ] Voice command works with background noise
  - [ ] Button works with touch
  - [ ] Button works with screen reader (TalkBack)

---

## Example: Teach AVA Feature

### Compliant Implementation

**Voice Commands:**
```kotlin
// Intent: teach_ava
// Examples:
- "teach ava"
- "teach this"
- "train ava"
- "add intent"
- "teach ava this utterance"
- "I want to teach you something"
```

**Visible Button:**
```kotlin
// ChatScreen.kt - Top App Bar
TopAppBar(
    title = { Text("AVA AI") },
    actions = {
        IconButton(onClick = { viewModel.activateTeachMode() }) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = "Teach AVA",
                modifier = Modifier.size(24.dp)
            )
        }
    }
)
```

**Optional Gesture:**
```kotlin
// MessageBubble.kt - Long-press as bonus
Box(modifier = Modifier.combinedClickable(
    onLongClick = { onTeachAva() }
))
```

---

## Anti-Patterns to Avoid

### ❌ Anti-Pattern 1: Hidden Features

```kotlin
// BAD: Feature only accessible via obscure gesture
DropdownMenu(expanded = showMenu) {
    DropdownMenuItem(onClick = { /* teach ava */ }) {
        Text("Teach AVA")
    }
}
// No voice command, no visible button
```

**Fix:** Add voice command + always-visible button

### ❌ Anti-Pattern 2: Voice Command Without Visual Fallback

```kotlin
// BAD: Only voice command, no button
// Users can't discover feature visually
// Fails in noisy environments
```

**Fix:** Add visible button with icon and label

### ❌ Anti-Pattern 3: Complex Multi-Step Voice Commands

```kotlin
// BAD: "open menu, then select teach, then confirm"
// Too many steps, high cognitive load
```

**Fix:** Single direct command: "teach ava"

---

## Accessibility Testing Requirements

### Voice Command Testing

1. **Quiet Environment Test**
   - Speak command in quiet room
   - Should work 95%+ of attempts

2. **Noisy Environment Test**
   - Background conversation
   - Music playing
   - Should work 80%+ of attempts

3. **Accent/Dialect Test**
   - Test with different English accents
   - Test with non-native speakers
   - Should work 75%+ of attempts

4. **Multiple Synonym Test**
   - Try at least 5 different phrasings
   - All should trigger same action

### Button/Visual Testing

1. **TalkBack Test** (Screen Reader)
   - Enable TalkBack
   - Navigate to button
   - Should announce clear description
   - Should be activatable

2. **Touch Target Test**
   - Measure touch target size
   - Must be minimum 48dp x 48dp
   - Should have adequate spacing (8dp minimum)

3. **Contrast Test**
   - Check color contrast ratio
   - Text: 4.5:1 minimum (WCAG AA)
   - Icons: 3:1 minimum

4. **Discovery Test**
   - Show app to new user
   - Can they find feature within 10 seconds?
   - No hints allowed

---

## Voice Command Design Guidelines

### Good Command Characteristics

1. **Natural Language**
   - ✅ "teach ava"
   - ✅ "teach this"
   - ❌ "ava.teach.mode.activate"

2. **Short (1-4 words)**
   - ✅ "teach ava"
   - ✅ "set alarm"
   - ❌ "please activate the teaching mode for ava so I can train it"

3. **Memorable**
   - ✅ "teach ava" (obvious purpose)
   - ❌ "mode seven" (obscure)

4. **Distinct**
   - ✅ "teach ava" vs "set alarm" (clearly different)
   - ❌ "teach ava" vs "reach ava" (confusable)

### Multiple Synonyms Required

Every command MUST have at least 3 synonyms:

```kotlin
// teach_ava intent examples:
- "teach ava"          // Primary
- "train ava"          // Synonym 1
- "teach this"         // Synonym 2
- "add intent"         // Synonym 3
- "show me how"        // Synonym 4
- "I want to teach you" // Synonym 5
```

**Why:** Users don't all think the same way. Multiple phrasings increase discoverability and success rate.

---

## Button Design Guidelines

### Always-Visible Pattern

**Preferred locations (in order):**

1. **Top App Bar** - For primary features
   ```kotlin
   TopAppBar(actions = {
       IconButton(onClick = { /* action */ }) {
           Icon(Icons.Filled.Feature)
       }
   })
   ```

2. **Bottom Bar** - For frequently used features
   ```kotlin
   BottomAppBar {
       IconButton(onClick = { /* action */ }) {
           Icon(Icons.Filled.Feature)
       }
   }
   ```

3. **Floating Action Button** - For primary action
   ```kotlin
   FloatingActionButton(onClick = { /* action */ }) {
       Icon(Icons.Filled.Add)
   }
   ```

4. **Inline Button** - For contextual features
   ```kotlin
   Button(onClick = { /* action */ }) {
       Text("Action Name")
   }
   ```

### Button Requirements

- **Size**: Minimum 48dp x 48dp touch target
- **Spacing**: Minimum 8dp between buttons
- **Label**: Text label OR contentDescription (screen readers)
- **Icon**: Recognizable icon that matches function
- **Contrast**: 4.5:1 for text, 3:1 for icons
- **State**: Clear visual feedback (pressed, disabled)

---

## Integration with VoiceOS Commands

AVA can leverage VoiceOS's comprehensive command database:

**VoiceOS Commands Available:**
- 87 commands across 19 categories
- Cursor control, gestures, navigation, etc.
- All voice-activatable

**Integration Strategy:**
```kotlin
// AVA can call VoiceOS commands
when (intent) {
    "teach_ava" -> openTeachDialog()
    "select" -> voiceOSCommandManager.execute("SELECT")
    "navigate_back" -> voiceOSCommandManager.execute("NAVIGATE_BACK")
}
```

---

## Compliance Checklist

Before shipping ANY feature:

### Design Phase
- [ ] Voice command defined
- [ ] Button mockup created
- [ ] User flow documented
- [ ] Accessibility reviewed

### Implementation Phase
- [ ] Voice command intent registered
- [ ] Action handler implemented
- [ ] Button added to UI
- [ ] TalkBack contentDescription added
- [ ] Touch target size verified (48dp)

### Testing Phase
- [ ] Voice command tested (quiet)
- [ ] Voice command tested (noisy)
- [ ] Button tested (touch)
- [ ] Button tested (TalkBack)
- [ ] Multiple synonyms tested
- [ ] Discovery tested (new user)

### Documentation Phase
- [ ] Voice commands listed in user guide
- [ ] Button location shown in screenshots
- [ ] Tutorial video created (if applicable)

---

## Enforcement

**This standard is MANDATORY.**

**Code Review Requirements:**
- All PRs MUST include voice command documentation
- All new features MUST have visible button
- Accessibility checklist MUST be completed

**Automatic Checks:**
- CI/CD pipeline checks for contentDescription
- Lint rules enforce minimum touch target size
- Integration tests verify voice commands work

**Manual Review:**
- Design review checks for visible buttons
- Accessibility review checks TalkBack support
- UX review checks command discoverability

---

## Related Standards

- **WCAG 2.1 AA** - Web Content Accessibility Guidelines
- **Material Design 3** - Accessibility guidelines
- **Android Accessibility** - TalkBack, Switch Access
- **VoiceOS Command Patterns** - Voice command design

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-10 | Initial standard created |

---

## Contact

**Questions about this standard?**
- Email: manoj@ideahq.net
- Project: AVA Voice Assistant
- Repository: github.com/mkjhawar/AVA

---

**REMEMBER: Voice first, visual second, gestures as bonus. Always provide multiple access methods.**
