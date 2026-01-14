# Agent 7: Advanced Navigation Components - Quick Reference

**Date:** 2025-11-24
**Status:** ✅ COMPLETE

---

## Components Implemented

| Component | Purpose | Package | LOC |
|-----------|---------|---------|-----|
| **MenuBar** | Desktop-style menu bar | `flutter.material.navigation` | 230 |
| **SubMenu** | Cascading nested menus | `flutter.material.navigation` | 330 |
| **VerticalTabs** | Vertical tab navigation | `flutter.material.navigation` | 340 |

**Total LOC (Components):** 900 lines
**Total LOC (Mappers):** 540 lines
**Total LOC (Tests):** 680 lines
**Grand Total:** 2,120 lines

---

## Quick Start

### 1. MenuBar - Desktop Application Menu
```kotlin
import com.augmentalis.avaelements.flutter.material.navigation.*

MenuBar.standard(
    fileItems = listOf(
        Menu.MenuItem(id = "new", label = "New", accelerator = "n"),
        Menu.MenuItem(id = "open", label = "Open", accelerator = "o")
    ),
    editItems = listOf(
        Menu.MenuItem(id = "cut", label = "Cut"),
        Menu.MenuItem(id = "copy", label = "Copy")
    ),
    viewItems = listOf(
        Menu.MenuItem(id = "zoom", label = "Zoom")
    ),
    onItemClick = { itemId -> handleMenuClick(itemId) }
)
```

### 2. SubMenu - Export Options
```kotlin
SubMenu.cascading(
    label = "Export",
    icon = "save_alt",
    items = listOf(
        SubMenu.SubMenuItem(
            id = "pdf",
            label = "Export as PDF",
            icon = "picture_as_pdf"
        ),
        SubMenu.SubMenuItem(
            id = "advanced",
            label = "Advanced",
            children = listOf(
                SubMenu.SubMenuItem(id = "custom", label = "Custom Format")
            )
        )
    ),
    onItemClick = { itemId -> exportData(itemId) }
)
```

### 3. VerticalTabs - Settings Panel
```kotlin
VerticalTabs.standard(
    tabs = listOf(
        VerticalTabs.Tab(
            id = "general",
            label = "General",
            icon = "settings",
            selected = true
        ),
        VerticalTabs.Tab(
            id = "notifications",
            label = "Notifications",
            icon = "notifications",
            badge = "3"
        )
    ),
    onTabSelected = { tabId -> navigateToSettings(tabId) }
)
```

---

## Factory Methods Cheat Sheet

### MenuBar
- `MenuBar.desktop()` - With keyboard accelerators
- `MenuBar.simple()` - Without accelerators
- `MenuBar.standard()` - File/Edit/View layout

### SubMenu
- `SubMenu.hover()` - Hover to open
- `SubMenu.click()` - Click to open
- `SubMenu.contextMenu()` - Right-click menu
- `SubMenu.cascading()` - Multi-level menu

### VerticalTabs
- `VerticalTabs.standard()` - Icons + labels
- `VerticalTabs.iconOnly()` - Icons only (72dp)
- `VerticalTabs.scrollable()` - For many tabs
- `VerticalTabs.grouped()` - With sections
- `VerticalTabs.dense()` - Compact spacing

---

## Common Patterns

### Desktop Menu Bar
```kotlin
MenuBar(
    items = listOf(
        MenuBarItem(
            id = "file",
            label = "File",
            accelerator = "f",
            children = listOf(
                Menu.MenuItem(id = "new", label = "New", accelerator = "n"),
                Menu.MenuItem(id = "divider", label = "", divider = true),
                Menu.MenuItem(id = "exit", label = "Exit", accelerator = "x")
            )
        )
    ),
    showAccelerators = true,
    height = 48f
)
```

### Context Menu (Right-Click)
```kotlin
SubMenu.contextMenu(
    items = listOf(
        SubMenuItem(id = "copy", label = "Copy", icon = "content_copy"),
        SubMenuItem(id = "paste", label = "Paste", icon = "content_paste"),
        SubMenuItem(id = "divider", label = "", divider = true),
        SubMenuItem(id = "delete", label = "Delete", icon = "delete", destructive = true)
    )
)
```

### Settings Navigation
```kotlin
VerticalTabs(
    tabs = listOf(
        Tab(id = "general", label = "General", icon = "settings", group = "App"),
        Tab(id = "appearance", label = "Appearance", icon = "palette", group = "App"),
        Tab(id = "divider1", label = "", divider = true),
        Tab(id = "privacy", label = "Privacy", icon = "lock", group = "Security"),
        Tab(id = "security", label = "Security", icon = "security", group = "Security")
    ),
    width = 240f,
    labelPosition = VerticalTabs.LabelPosition.Right
)
```

---

## Rendering on Android

```kotlin
import com.augmentalis.avaelements.renderers.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*

// Create renderer
val renderer = ComposeRenderer()

// Render component
@Composable
fun MyScreen() {
    val menuBar = MenuBar(...)
    val composableFunction = renderer.render(menuBar) as @Composable () -> Unit
    composableFunction()
}
```

---

## Testing

### Run All Tests
```bash
./gradlew :Universal:Libraries:AvaElements:Renderers:Android:connectedAndroidTest
```

### Run Specific Test
```bash
./gradlew :Universal:Libraries:AvaElements:Renderers:Android:connectedAndroidTest \
  --tests "NavigationComponentsTest.menuBar_rendersCorrectly"
```

### Test Coverage
```bash
./gradlew :Universal:Libraries:AvaElements:Renderers:Android:jacocoTestReport
```

---

## Accessibility

All components support:
- ✅ TalkBack screen reader
- ✅ WCAG 2.1 Level AA contrast
- ✅ 48dp minimum touch targets
- ✅ Keyboard navigation
- ✅ Semantic descriptions

### Example Accessibility Description
```kotlin
val menuBar = MenuBar(
    items = listOf(...),
    contentDescription = "Main application menu bar"
)

// Automatic description:
// "Main application menu bar with 3 menus"
```

---

## Material Design 3 Components Used

| Component | Material3 Composables |
|-----------|----------------------|
| MenuBar | Surface, Row, TextButton, DropdownMenu |
| SubMenu | DropdownMenu, DropdownMenuItem, Icon |
| VerticalTabs | Surface, Column/LazyColumn, Badge |

---

## File Locations

### Component Data Classes
```
Universal/Libraries/AvaElements/components/flutter-parity/
  src/commonMain/kotlin/com/augmentalis/avaelements/flutter/material/navigation/
    ├── MenuBar.kt
    ├── SubMenu.kt
    └── VerticalTabs.kt
```

### Android Mappers
```
Universal/Libraries/AvaElements/Renderers/Android/
  src/androidMain/kotlin/com/augmentalis/avaelements/renderer/android/mappers/flutterparity/
    └── FlutterParityMaterialMappers.kt (contains MenuBarMapper, SubMenuMapper, VerticalTabsMapper)
```

### Tests
```
Universal/Libraries/AvaElements/Renderers/Android/
  src/androidTest/kotlin/com/augmentalis/avaelements/renderers/android/
    └── NavigationComponentsTest.kt (32 tests)
```

### Completion Marker
```
.ideacode/swarm-state/android-parity/
  └── navigation-components-complete.json
```

---

## Performance Tips

1. **MenuBar**
   - Keep dropdown items under 20 for smooth rendering
   - Use icons sparingly (increases memory)

2. **SubMenu**
   - Limit nesting to 3-4 levels max
   - Use dividers to organize long menus
   - Enable `closeOnItemClick` for better UX

3. **VerticalTabs**
   - Use `scrollable = true` for 10+ tabs
   - Enable `dense = true` for compact layouts
   - Use `iconOnly()` for narrow sidebars (72dp)

---

## Common Issues & Solutions

### Issue: Dropdown not showing
**Solution:** Ensure parent has enough space. Add `.fillMaxSize()` modifier.

### Issue: Click not working
**Solution:** Check `enabled = true` on items. Verify callback is set.

### Issue: Accessibility warnings
**Solution:** Add `contentDescription` to all components and icons.

### Issue: Badge not visible
**Solution:** Badge text must be non-empty. Use "0" instead of empty string.

---

## API Overview

### MenuBar Properties
```kotlin
MenuBar(
    items: List<MenuBarItem>,           // Required
    backgroundColor: String? = null,
    elevation: Float? = null,
    height: Float = 48f,
    showAccelerators: Boolean = false,
    onItemClick: ((String) -> Unit)? = null
)
```

### SubMenu Properties
```kotlin
SubMenu(
    label: String,                      // Required
    items: List<SubMenuItem>,           // Required
    icon: String? = null,
    trigger: TriggerMode = Both,
    placement: Placement = Auto,
    onItemClick: ((String) -> Unit)? = null
)
```

### VerticalTabs Properties
```kotlin
VerticalTabs(
    tabs: List<Tab>,                    // Required
    width: Float = 200f,
    scrollable: Boolean = false,
    dense: Boolean = false,
    showLabels: Boolean = true,
    showIcons: Boolean = true,
    labelPosition: LabelPosition = Right,
    onTabSelected: ((String) -> Unit)? = null
)
```

---

## Integration with Other Components

### MenuBar + Menu
MenuBar can embed Menu.MenuItem in dropdowns:
```kotlin
MenuBarItem(
    id = "file",
    children = listOf(
        Menu.MenuItem(id = "new", label = "New")  // Reuse Menu.MenuItem
    )
)
```

### VerticalTabs + NavLink
Use VerticalTabs for navigation structure, NavLink for individual links:
```kotlin
VerticalTabs(
    tabs = listOf(
        Tab(id = "settings", label = "Settings")
    ),
    onTabSelected = { tabId ->
        // Navigate using NavLink
    }
)
```

### SubMenu + PopupMenuButton
SubMenu complements PopupMenuButton for desktop-style cascading menus.

---

## Testing Checklist

- ✅ Component renders without crash
- ✅ Click triggers callback
- ✅ Disabled items not clickable
- ✅ Accessibility description correct
- ✅ Edge cases (empty, null) handled
- ✅ Selection state updates
- ✅ Badge displays correctly
- ✅ Keyboard navigation works

---

## Documentation

- **Full Report:** `/docs/AGENT-7-NAVIGATION-COMPLETE.md`
- **Component KDocs:** In source files (MenuBar.kt, SubMenu.kt, VerticalTabs.kt)
- **Test Suite:** `NavigationComponentsTest.kt`

---

## Support

For issues or questions:
1. Check KDoc in source files
2. Review test cases for examples
3. See Material Design 3 specs: https://m3.material.io/components

---

**Agent 7 - Advanced Navigation Components**
**Status:** ✅ Production Ready
**Last Updated:** 2025-11-24
