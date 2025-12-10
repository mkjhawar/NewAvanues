# VoiceUI Visual Examples & Screen Layouts

## 🎨 How Simplified VoiceUI Screens Actually Look

This document shows the visual appearance of VoiceUI simplified screens with ASCII mockups and layout descriptions.

## 📱 Example 1: Login Screen

### Code (5 lines):
```kotlin
VoiceScreen("login") {
    text("Welcome to VoiceOS")
    input("email")
    password()
    button("login")
    button("forgot_password")
}
```

### Visual Result:
```
┌─────────────────────────────────────┐
│ ●●●         VoiceOS         ●●●     │ ← HUD Status Bar
├─────────────────────────────────────┤
│                                     │
│          Welcome to VoiceOS         │ ← Auto-centers, announces
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Email                    🎤 │   │ ← Voice input icon
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Password               ●●●● │   │ ← No voice (secure)
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────── LOGIN ──────────┐   │ ← Primary action
│  │         [Tap or Say]        │   │
│  └─────────────────────────────┘   │
│                                     │
│         Forgot Password?            │ ← Link style
│                                     │
├─────────────────────────────────────┤
│ 🎙️ Say: "enter email", "login"    │ ← Voice hints
└─────────────────────────────────────┘
```

### Voice Commands Available:
- "Enter email" → Focus email field + start dictation
- "Enter password" → Focus password field
- "Login" → Submit form
- "Forgot password" → Open reset dialog

---

## 📱 Example 2: Settings Screen

### Code (8 lines):
```kotlin
VoiceScreen("settings") {
    text("App Settings")
    toggle("dark_mode")
    dropdown("language", listOf("English", "Spanish", "French"))
    slider("volume", 0..100)
    button("save_settings")
    button("reset_defaults")
}
```

### Visual Result:
```
┌─────────────────────────────────────┐
│ ●●●      App Settings       ●●●     │
├─────────────────────────────────────┤
│                                     │
│           App Settings              │
│                                     │
│  Dark Mode              ◯ ●        │ ← Toggle switch
│  Say: "toggle dark mode"            │
│                                     │
│  Language               English ▼   │ ← Dropdown
│  Say: "change language to Spanish"  │
│                                     │
│  Volume        ●────────○────       │ ← Slider at 75%
│  Say: "set volume to 50"            │
│                                     │
│  ┌──────── SAVE SETTINGS ────────┐ │
│  │        [Tap or Say]           │ │
│  └───────────────────────────────┘ │
│                                     │
│  ┌────── RESET DEFAULTS ────────┐  │ ← Secondary action
│  │        [Tap or Say]          │  │
│  └──────────────────────────────┘  │
│                                     │
├─────────────────────────────────────┤
│ 🎙️ Voice commands available above  │
└─────────────────────────────────────┘
```

### Voice Commands:
- "Toggle dark mode" → Switch theme
- "Change language to Spanish" → Update language
- "Set volume to 75" → Adjust slider
- "Save settings" → Apply changes

---

## 📱 Example 3: Task Manager

### Code (12 lines):
```kotlin
VoiceScreen("tasks") {
    text("My Tasks (${tasks.size})")
    input("new_task") { tasks.add(it) }
    list(tasks) { task ->
        taskItem(task) { tasks.remove(task) }
    }
    button("clear_completed")
    button("sort_by_priority")
}
```

### Visual Result:
```
┌─────────────────────────────────────┐
│ ●●●       My Tasks (3)      ●●●     │
├─────────────────────────────────────┤
│                                     │
│            My Tasks (3)             │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Add new task...          🎤 │   │ ← Quick add
│  └─────────────────────────────┘   │
│                                     │
│  ✓ Buy groceries            [✓][✗] │ ← Completed
│    Say: "complete buy groceries"    │
│                                     │
│  ○ Call dentist             [✓][✗] │ ← Pending
│    Say: "complete call dentist"     │
│                                     │
│  ○ Fix bike tire            [✓][✗] │ ← Pending
│    Say: "complete fix bike tire"    │
│                                     │
│  ┌──── CLEAR COMPLETED ────────┐   │
│  │       [Tap or Say]         │   │
│  └────────────────────────────┘   │
│                                     │
│  ┌──── SORT BY PRIORITY ─────┐    │
│  │       [Tap or Say]        │    │
│  └───────────────────────────┘    │
│                                     │
├─────────────────────────────────────┤
│ 🎙️ "Add task: buy milk", "complete X" │
└─────────────────────────────────────┘
```

### Voice Commands:
- "Add task: buy milk" → Adds new task
- "Complete buy groceries" → Marks done
- "Delete call dentist" → Removes task
- "Sort by priority" → Reorders list

---

## 📱 Example 4: E-commerce Product

### Code (15 lines):
```kotlin
VoiceScreen("product_details") {
    text("iPhone 15 Pro - $999")
    text("Available in Space Black, Natural Titanium")
    stepper("quantity", quantity) { quantity = it }
    radioGroup("color", listOf("Space Black", "Natural Titanium"))
    chipGroup("storage", listOf("128GB", "256GB", "512GB"))
    button("add_to_cart")
    button("buy_now")
}
```

### Visual Result:
```
┌─────────────────────────────────────┐
│ ●●●     iPhone 15 Pro       ●●●     │
├─────────────────────────────────────┤
│                                     │
│        iPhone 15 Pro - $999         │ ← Product title
│                                     │
│   Available in Space Black,         │ ← Description
│      Natural Titanium               │
│                                     │
│  Quantity:    [−] 2 [+]            │ ← Stepper
│  Say: "set quantity to 3"           │
│                                     │
│  Color:                             │
│  ◉ Space Black  ○ Natural Titanium  │ ← Radio buttons
│  Say: "select Natural Titanium"     │
│                                     │
│  Storage:                           │
│  [128GB] [256GB] [●512GB●] [1TB]   │ ← Chips
│  Say: "select 1 terabyte"           │
│                                     │
│  ┌────── ADD TO CART ──────────┐   │ ← Primary CTA
│  │      $999 • [Tap or Say]    │   │
│  └──────────────────────────────┘   │
│                                     │
│  ┌────────── BUY NOW ────────────┐ │ ← Secondary CTA
│  │       [Tap or Say]           │ │
│  └──────────────────────────────┘ │
│                                     │
├─────────────────────────────────────┤
│ 🎙️ "Add to cart", "buy now", etc.  │
└─────────────────────────────────────┘
```

---

## 🏠 Example 5: Smart Home Control

### Code (6 lines):
```kotlin
VoiceScreen("smart_home") {
    text("Home Control")
    toggle("living_room_lights")
    slider("thermostat", 60..85)
    button("lock_doors")
    button("check_cameras")
}
```

### Visual Result:
```
┌─────────────────────────────────────┐
│ ●●●     Home Control        ●●●     │
├─────────────────────────────────────┤
│                                     │
│           Home Control              │
│                                     │
│  Living Room Lights     ● ○        │ ← ON (green)
│  Say: "turn off living room lights" │
│                                     │
│  Thermostat (72°F)                 │
│  ●────────○────     60°    85°     │ ← Slider
│  Say: "set thermostat to 75"       │
│                                     │
│  ┌──────── LOCK DOORS ────────┐   │
│  │    🔒 [Tap or Say]         │   │ ← Icon + text
│  └────────────────────────────┘   │
│                                     │
│  ┌───── CHECK CAMERAS ────────┐   │
│  │    📹 [Tap or Say]         │   │
│  └────────────────────────────┘   │
│                                     │
│                                     │
├─────────────────────────────────────┤
│ 🎙️ Natural home automation commands │
└─────────────────────────────────────┘
```

---

## 🎵 Example 6: Music Player

### Code (8 lines):
```kotlin
VoiceScreen("music_player") {
    text("Now Playing: ${currentSong}")
    button("play_pause")
    button("next_track")
    button("previous_track")
    slider("volume")
    button("shuffle")
    button("repeat")
}
```

### Visual Result:
```
┌─────────────────────────────────────┐
│ ●●●      Music Player       ●●●     │
├─────────────────────────────────────┤
│                                     │
│        🎵 Album Cover 🎵            │ ← Large artwork
│                                     │
│      "Bohemian Rhapsody"            │ ← Song title
│           Queen                     │ ← Artist
│                                     │
│  ●──────○─────────  2:15 / 5:55    │ ← Progress
│                                     │
│     [◀◀]   [⏸️]   [▶▶]           │ ← Transport controls
│   Previous  Pause   Next            │
│                                     │
│  Volume  ●────────○────             │ ← Volume slider
│                                     │
│     [🔀]         [🔁]              │ ← Shuffle & repeat
│   Shuffle        Repeat             │
│                                     │
├─────────────────────────────────────┤
│ 🎙️ "play", "next song", "volume up" │
└─────────────────────────────────────┘
```

### Voice Commands:
- "Play" / "Pause" → Control playback
- "Next song" / "Previous song" → Navigate
- "Volume up" / "Volume down" → Adjust
- "Shuffle on" / "Repeat off" → Toggle modes

---

## 💡 Visual Design Principles

### 1. **HUD Integration**
Every screen includes the VoiceUI HUD showing:
- System status (battery, network, time)
- Active voice commands
- Visual feedback for voice actions

### 2. **Voice Indicators**
- 🎤 icon shows voice input available
- Dotted borders indicate voice-focusable elements
- Text hints show available commands

### 3. **Gesture Visual Cues**
- Swipe arrows for navigation
- Long-press hints on buttons
- Pinch/zoom indicators on media

### 4. **Accessibility First**
- High contrast options
- Large touch targets (48dp minimum)
- Clear focus indicators
- Voice descriptions for all elements

### 5. **Progressive Enhancement**
```
Base Touch UI → + Voice Commands → + Gestures → + Spatial Features
```

### 6. **Smart Layouts**
- Auto-responsive to screen size
- Adapts to landscape/portrait
- Scales for different devices
- Works in AR/VR environments

---

## 🎯 Layout Templates

### Standard Form Layout:
```
[Title]
[Input Field] 🎤
[Input Field] 🎤
[Dropdown] ▼
[Toggle] ● ○
[Primary Button]
[Secondary Button]
[Voice Hints]
```

### List Layout:
```
[Title + Count]
[Search/Filter] 🎤
┌─ [Item 1] ─ [Actions]
├─ [Item 2] ─ [Actions]
├─ [Item 3] ─ [Actions]
└─ [Add New] ─ [🎤]
[Bulk Actions]
[Voice Navigation Hints]
```

### Media Layout:
```
[Large Preview/Artwork]
[Title/Description]
[Progress/Timestamp]
[Transport Controls]
[Volume/Settings]
[Voice Command Guide]
```

---

**Visual Status:** Complete mockups showing actual screen appearance  
**Key Feature:** Every element is voice-accessible and gesture-enabled  
**Design Goal:** Beautiful, functional, and completely accessible through multiple input methods