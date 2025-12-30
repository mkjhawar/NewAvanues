# User Manual - Chapter 15: Ocean Glass User Interface

**Version:** 2.0.0
**Date:** 2025-12-03
**Author:** AVA AI Team

---

## Overview

AVA features the Ocean Glass design - a modern, elegant interface inspired by glass and water. This design provides a calm, focused experience while you interact with your AI assistant.

---

## What You'll See

When you open AVA, you'll see the Ocean Glass interface:

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  AVA  AI                                         [RAG]  │  ← App name in red + blue
│                                                         │
│  ┌─────────────────────────────────────┐               │
│  │ Hello! I'm AVA, your AI assistant  │               │  ← AVA's message (glass)
│  └─────────────────────────────────────┘               │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Type or say something...                    [>] │   │  ← Your input area
│  └─────────────────────────────────────────────────┘   │
│                                                         │
├─────────────────────────────────────────────────────────┤
│   [Chat]          [Teach]          [Settings]     🎤   │  ← Navigation + Voice
└─────────────────────────────────────────────────────────┘
```

---

## Design Philosophy

The Ocean Glass interface is designed to:

- **Reduce visual clutter** - Clean, minimal elements
- **Be easy on your eyes** - Soft colors and gentle transitions
- **Stay out of your way** - Overlay mode lets you see other apps
- **Feel natural** - Smooth animations and intuitive interactions

---

## Visual Elements

### Color Theme

| Element | Color | Description |
|---------|-------|-------------|
| Background | Ocean Blue gradient | Deep ocean tones (#0A1929 → #1E293B) |
| Accent | CoralBlue | Highlights and buttons (#3B82F6) |
| Your messages | CoralBlue | Easy to identify |
| AVA's messages | Frosted glass | Subtle, non-intrusive |

### Message Bubbles

**Your Messages:**
- Appear on the right side
- CoralBlue color with rounded corners
- Tail points to the right

**AVA's Responses:**
- Appear on the left side
- Frosted glass effect (semi-transparent)
- Subtle border for definition
- Tail points to the left

### Confidence Indicators

AVA shows how confident she is in her responses:

| Badge Color | Meaning |
|-------------|---------|
| Green | High confidence - AVA understood clearly |
| Yellow | Medium confidence - You can confirm if correct |
| Red | Low confidence - You can teach AVA |

---

## Chat Screen Features

### Empty State

When you first open AVA, you'll see:
- A large chat icon
- "Start a conversation" message
- Example commands to try

### Sending Messages

1. Type in the text field at the bottom
2. The field shows helpful placeholder text:
   - "Type or say something..." (normal mode)
   - "Ask about your documents..." (RAG mode)
3. Tap the send button (turns blue when ready)
4. A loading spinner shows while AVA thinks

### While AVA is Thinking

- The send button shows a spinning indicator
- A "AVA is thinking..." message appears
- The glass-styled indicator stays at the bottom of messages

### RAG Mode Indicator

When searching your documents:
- A glass card appears above the input
- Shows "Searching Your Documents"
- Displays how many documents are selected

---

## Navigation

AVA adapts its navigation based on your device orientation:

### Portrait Mode (Bottom Bar)

At the bottom of the screen, you'll find three tabs:

| Tab | Icon | Purpose |
|-----|------|---------|
| **Chat** | 💬 | Your conversations with AVA |
| **Teach** | 🎓 | Train AVA with new commands |
| **Settings** | ⚙️ | Configure AVA preferences |

The selected tab is highlighted in CoralBlue. No background shapes - just clean icons.

### Landscape Mode (Side Rail)

When you rotate to landscape, navigation moves to the left side:

```
┌────┬────────────────────────────────────┐
│ 💬 │                                    │
│Chat│    [Your content area]             │
├────┤                                    │
│ 🎓 │                                    │
│Teach│                                   │
├────┤                                    │
│ ⚙️ │                                    │
│ Set │                                   │
└────┴────────────────────────────────────┘
```

This preserves vertical space for better reading and typing.

---

## Voice Button

The CoralBlue microphone button floats in the bottom-right corner with a subtle shadow:

**Design:**
- Floating at a higher level than the navigation
- Visible shadow shows it's interactive
- 56dp size for easy tapping

**How to Use:**
1. **Tap once** to start voice input
2. **Speak your command** - AVA listens
3. **Wait for response** - AVA processes your request

The button moves responsively:
- **Portrait:** Above the bottom navigation
- **Landscape:** Near the bottom-right, not overlapping the side rail

The button glows when AVA is listening to you.

---

## App Header

At the top of the screen:

- **AVA** in red - the app name
- **AI** in CoralBlue - indicates AI assistant mode
- **RAG icon** (right side) - shows document search status

---

## Overlay Mode (Coming Soon)

The overlay lets you use AVA while using other apps:

### Voice Orb

- Small floating circle in the corner
- Tap to activate voice input
- Glows when listening

### Quick Responses

- AVA's responses appear as floating bubbles
- Auto-dismiss after 5 seconds
- Tap to expand for more details

### Action Chips

- Quick action buttons appear for common tasks
- Tap to execute immediately
- Maximum 3 visible to avoid clutter

---

## Accessibility Features

### Touch Targets

- All buttons are at least 48dp (easy to tap)
- Generous spacing between interactive elements

### Color Contrast

- Text is always readable on glass backgrounds
- Important information uses high-contrast colors

### Screen Readers

- All elements have descriptions for screen readers
- Meaningful labels for all buttons and indicators

---

## Tips for Best Experience

### Chat Interface

1. **Look for the confidence badge** - It tells you how certain AVA is
2. **Use the examples** - The empty state shows good starting commands
3. **Watch the send button** - It indicates when AVA is processing

### Visual Comfort

1. **Use dark mode** - The Ocean Glass theme works best in dark mode
2. **Adjust brightness** - The glass effects look best at moderate brightness
3. **Enable reduced motion** - If animations feel distracting

---

## Customization (Future)

Planned customization options:

| Feature | Status |
|---------|--------|
| Theme color selection | Planned |
| Glass intensity adjustment | Planned |
| Animation speed control | Planned |
| Overlay position choice | Planned |

---

## Troubleshooting

### Glass Effects Look Wrong

- Ensure you're using Android 12 or later for blur effects
- Try toggling dark mode on and off
- Restart the app

### Text Hard to Read

- Check your system font size settings
- Enable high contrast mode in accessibility settings
- Report the issue - we take readability seriously

### Animations Choppy

- Close background apps to free up resources
- Enable battery saver mode for reduced animations
- Update to the latest app version

---

## Feedback

We'd love to hear your thoughts on the Ocean Glass design:

- Is it easy to use?
- Are the colors comfortable?
- Any elements hard to see or tap?

Your feedback helps us improve AVA for everyone.

---

## Quick Reference

| Element | Location | Color |
|---------|----------|-------|
| App name "AVA" | Top left | Red |
| App name "AI" | Top left | CoralBlue (#3B82F6) |
| Your messages | Right side | CoralBlue |
| AVA messages | Left side | Frosted glass |
| Send button | Input area, right | CoralBlue (when active) |
| Voice button | Bottom right (floating) | CoralBlue with shadow |
| Navigation (Portrait) | Bottom 56dp | Ocean depth with CoralBlue icons |
| Navigation (Landscape) | Left side 56dp | Ocean depth with CoralBlue icons |
| Background | Full screen | Ocean Blue gradient |

---

## What's New in v2.0

| Feature | Description |
|---------|-------------|
| Adaptive Navigation | Side rail in landscape, bottom bar in portrait |
| Compact Design | 56dp navigation (was ~80dp) |
| No Icon Backgrounds | Clean icon-only selection |
| Floating Voice Button | Elevated with shadow, positioned above navigation |
| Space Efficiency | More content visible on screen |

---

**Next Chapter:** [Chapter 16 - TBD]
**Previous Chapter:** [Chapter 14 - Privacy & Security](User-Manual-Chapter14-Privacy-Security.md)
