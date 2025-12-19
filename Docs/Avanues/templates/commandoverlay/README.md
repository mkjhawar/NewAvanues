# Voice Command Overlay Pattern - Shared Template

**Created:** 2025-01-07
**Source:** Browser module (NewAvanue implementation)
**Used By:** NewAvanue, Avanues, VOS4 (any module needing voice commands)

---

## 📁 Files in This Directory

| File | Purpose | Lines | Usage |
|------|---------|-------|-------|
| **Guide-Voice-Command-Overlay-Pattern.md** | Comprehensive implementation guide | ~800 | Read first - complete documentation |
| **ModuleCommandOverlay-Template.kt** | Copy-paste template for new modules | ~350 | Copy to your module, customize |
| **Example-NotepadCommandOverlay.kt** | Real working example (Notepad) | ~400 | Reference implementation |

---

## 🚀 Quick Start

### For NewAvanue Modules:
```bash
# 1. Copy template to your module
cp /Volumes/M-Drive/Coding/template/commandoverlay/ModuleCommandOverlay-Template.kt \
   /Volumes/M-Drive/Coding/Warp/newAvanue/modules/[yourmodule]/src/main/java/.../presentation/components/

# 2. Rename and customize
cd /Volumes/M-Drive/Coding/Warp/newAvanue/modules/[yourmodule]/.../presentation/components/
mv ModuleCommandOverlay-Template.kt [YourModule]CommandOverlay.kt

# 3. Find/Replace in file:
#    "YourModule" → "Notepad" (your actual module name)
#    "yourmodule" → "notepad" (lowercase package)
```

### For Avanues/VOS4 Modules:
```bash
# 1. Copy template
cp /Volumes/M-Drive/Coding/template/commandoverlay/ModuleCommandOverlay-Template.kt \
   /Volumes/M-Drive/Coding/voiceavanue/modules/[yourmodule]/src/main/java/.../presentation/components/

# 2. Follow same customization steps as above
```

---

## 📖 Implementation Steps

1. **Read the Guide** - `/template/commandoverlay/Guide-Voice-Command-Overlay-Pattern.md`
2. **Study the Example** - `/template/commandoverlay/Example-NotepadCommandOverlay.kt`
3. **Copy the Template** - `/template/commandoverlay/ModuleCommandOverlay-Template.kt`
4. **Customize** - Replace placeholders, define categories
5. **Integrate** - Add FAB trigger to main screen
6. **Test** - Portrait/landscape, cascading navigation

**Time Estimate:** 2-4 hours per module

---

## 🎯 Pattern Benefits

| Benefit | Description |
|---------|-------------|
| **Zero Space** | 0dp when hidden (vs 180dp permanent in original) |
| **Voice-First** | All commands visible, no scrolling |
| **Cascading Nav** | Master → Categories → Commands |
| **Uniform Grid** | Same-size buttons, professional look |
| **Responsive** | Auto-adapts to portrait/landscape |
| **Reusable** | Works across all modules |

---

## 📊 Used In

**NewAvanue:**
- ✅ Browser (reference implementation)
- 🔜 Notepad (example created)
- 🔜 FileManager
- 🔜 Task
- 🔜 Accessibility

**Avanues/VOS4:**
- 🔜 Available for all 19 modules

---

## 🔗 Related Documentation

**NewAvanue:**
- Original Implementation: `/newAvanue/modules/browser/src/main/java/.../presentation/components/BrowserCommandOverlay.kt`
- Project Docs: `/newAvanue/docs/modules/Browser/`

**Avanues:**
- Copied to: `/voiceavanue/docs/templates/commandoverlay/`

---

## 📝 Customization Options

### Colors:
```kotlin
// Overlay background
Color.Black.copy(alpha = 0.95f)

// Button background
Color.White.copy(alpha = 0.15f)

// Header
MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
```

### Sizes:
```kotlin
// Portrait
minSize = 110.dp, height = 70.dp

// Landscape
minSize = 90.dp, height = 60.dp
```

### Icons:
- Use emojis (default) or Material Icons
- See example: `Command("📝", "New Note") { ... }`

---

## 💡 Example Module Categories

### Browser:
Tabs, Navigate, URL, Scroll, Zoom, Features, Downloads

### Notepad:
Notes, Folders, Edit, Search, Settings

### FileManager:
Files, Folders, Transfer, Storage, Settings

### Accessibility:
Voice, Navigation, Display, Controls, Help

---

## 🐛 Troubleshooting

**Issue:** Commands scrollable / not all visible
**Fix:** Ensure `userScrollEnabled = false` in `LazyVerticalGrid`

**Issue:** Overlay doesn't slide smoothly
**Fix:** Check `.offset(y = (slideOffset * 1000).dp)`

**Issue:** Back button doesn't appear
**Fix:** Check `currentLevel != "master"` condition

---

## 📜 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-01-07 | Initial creation from Browser module |

---

## 📞 Support

For questions or issues:
1. Read the comprehensive guide (`Guide-Voice-Command-Overlay-Pattern.md`)
2. Study the working example (`Example-NotepadCommandOverlay.kt`)
3. Check the template comments (`ModuleCommandOverlay-Template.kt`)

---

**This pattern is production-ready and battle-tested in the Browser module!** 🚀
