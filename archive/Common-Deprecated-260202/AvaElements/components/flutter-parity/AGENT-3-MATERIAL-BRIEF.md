# Agent 3: Material Design Specialist - Implementation Brief

**Agent ID:** AGENT-3-MATERIAL
**Responsibility:** Material Design Variant Components (18 total)
**Timeline:** 2 weeks (90-108 hours)
**Priority:** P1 (High)

---

## MISSION

Implement all 18 Material Design 3 variant components to achieve Flutter Material library parity on Android platform. Focus on theming consistency, accessibility (TalkBack), and dark mode support.

---

## YOUR COMPONENTS

### Week 1: Chips & List Tiles (8 components)

#### Chip Variants (5 components)
**Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonMain/kotlin/com/augmentalis/avaelements/flutter/material/chips/`

1. **ActionChip.kt** (P1)
   - Chip that triggers an action (like a compact button)
   - Use: Material3 `SuggestionChip` with leading icon
   - Tests: Click handling, enabled/disabled states, theming, accessibility

2. **FilterChip.kt** (P1 - IMPORTANT)
   - Selectable chip for filtering content
   - Use: Material3 `FilterChip`
   - Tests: Selection state, checkmark appearance, selected/unselected styling, callbacks

3. **ChoiceChip.kt** (P1)
   - Single-selection chip from a set (radio button style)
   - Use: Material3 `FilterChip` with single-select logic wrapper
   - Tests: Selection exclusivity, visual states, group behavior

4. **InputChip.kt** (P1)
   - Chip with avatar and delete action
   - Use: Material3 `InputChip`
   - Tests: Avatar rendering, delete callback, keyboard input, accessibility

5. **Chip.kt** (enhancements)
   - Enhance base chip with additional features
   - Use: Material3 `AssistChip` improvements
   - Tests: All visual states, elevation, ripple, Material3 theming

#### List Tile Variants (3 components)
**Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonMain/kotlin/com/augmentalis/avaelements/flutter/material/lists/`

6. **CheckboxListTile.kt** (P0)
   - List tile with integrated checkbox
   - Use: `ListItem` with `Checkbox` leading or trailing
   - Tests: Checkbox states (checked, unchecked, indeterminate), three-line support, callbacks

7. **SwitchListTile.kt** (P0)
   - List tile with integrated switch
   - Use: `ListItem` with `Switch` trailing
   - Tests: Switch states, secondary text, theming, accessibility

8. **ExpansionTile.kt** (P0 - IMPORTANT)
   - Expandable list tile with children
   - Use: `AnimatedVisibility` with state management
   - Tests: Expand/collapse animation (200ms), trailing icon rotation, nested tiles, callbacks

---

### Week 2: Advanced Material Components (10 components)

**Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonMain/kotlin/com/augmentalis/avaelements/flutter/material/advanced/`

#### Buttons & Menus (3 components)

9. **FilledButton.kt** (P0)
   - Material3 filled button variant with all states
   - Use: Material3 `FilledButton`
   - Tests: All button states (enabled, disabled, pressed, hovered, focused), theming

10. **PopupMenuButton.kt** (P0)
    - Button that shows popup menu when clicked
    - Use: `DropdownMenu` with button trigger
    - Tests: Menu positioning, item selection, dismiss behavior, keyboard navigation

11. **RefreshIndicator.kt** (P0)
    - Pull-to-refresh indicator
    - Use: Compose `SwipeRefresh` or custom implementation
    - Tests: Pull gesture detection, refresh callback, indicator animation, threshold

#### Layout & Display (3 components)

12. **IndexedStack.kt** (P1)
    - Shows only one child at a given index
    - Use: `Box` with index-based visibility
    - Tests: Index switching, layout stability, child lifecycle

13. **VerticalDivider.kt** (P1)
    - Vertical divider line (complement to horizontal Divider)
    - Use: `Divider` with vertical orientation
    - Tests: Height behavior, thickness, color, indentation

14. **FadeInImage.kt** (P1)
    - Image with fade-in from placeholder
    - Use: `AsyncImage` with crossfade transition
    - Tests: Placeholder → image transition, error handling, fade duration

#### Avatar & Text (4 components)

15. **CircleAvatar.kt** (P1)
    - Circular avatar container (explicit implementation)
    - Use: Enhanced `Avatar` with circular clip and border
    - Tests: Image avatars, text avatars (initials), sizing, border

16. **RichText.kt** (P1)
    - Styled text with multiple spans (bold, italic, colors, links)
    - Use: `Text` with `AnnotatedString` and `SpanStyle`
    - Tests: Multiple spans, click handling, text selection, accessibility

17. **SelectableText.kt** (P2)
    - Text that can be selected and copied
    - Use: `BasicTextField` in read-only mode with selection
    - Tests: Selection behavior, copy action, theming, long-press

18. **EndDrawer.kt** (P2)
    - Drawer that opens from trailing edge (end)
    - Use: `ModalDrawer` with end alignment
    - Tests: Swipe from end, RTL support (becomes start in RTL), backdrop behavior

---

## ANDROID RENDERER IMPLEMENTATION

**Path:** `Universal/Libraries/AvaElements/Renderers/Android/src/androidMain/kotlin/com/augmentalis/avaelements/renderer/android/mappers/flutterparity/FlutterParityMaterialMappers.kt`

Create mapper functions for all 18 components:

```kotlin
@Composable
fun FilterChipMapper(component: FilterChipComponent) {
    FilterChip(
        selected = component.selected,
        onClick = component.onSelected,
        label = { Text(component.label) },
        leadingIcon = if (component.selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else null
    )
}

@Composable
fun ExpansionTileMapper(component: ExpansionTileComponent) {
    var expanded by remember { mutableStateOf(component.initiallyExpanded) }

    Column {
        ListItem(
            headlineContent = { Text(component.title) },
            supportingContent = component.subtitle?.let { { Text(it) } },
            trailingContent = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            },
            modifier = Modifier.clickable { expanded = !expanded }
        )

        AnimatedVisibility(visible = expanded) {
            Column {
                component.children.forEach { child ->
                    RenderChild(child)
                }
            }
        }
    }
}

// ... 18 total mappers
```

---

## TECHNICAL REQUIREMENTS

### Material Design 3 Compliance
- All components MUST follow Material Design 3 specifications
- Color tokens MUST use Material3 color system
- Typography MUST use Material3 type scale
- Spacing MUST follow 8dp grid system

### Theming
- Dark mode MUST be supported for all components
- Theme switching MUST be smooth (no flashing)
- Custom color schemes MUST be supported
- Text contrast MUST meet WCAG AA standards

### Accessibility (CRITICAL)
- All interactive components MUST have content descriptions
- TalkBack MUST announce component states correctly
- Focus order MUST be logical
- Minimum touch target: 48dp × 48dp
- WCAG 2.1 Level AA compliance required

### Testing
- Minimum 90% code coverage
- At least 4 tests per component:
  1. Basic functionality
  2. All visual states
  3. Dark mode rendering
  4. Accessibility (TalkBack)
- Integration tests with theming

### Documentation
- KDoc for all public APIs
- Code sample for each component
- Flutter equivalent comparison
- Theming guidelines

---

## TESTING STRATEGY

**Test Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonTest/kotlin/material/`

### Example Test Structure
```kotlin
class FilterChipTest {
    @Test
    fun `displays checkmark when selected`() {
        // Test selected state visual
    }

    @Test
    fun `fires callback on selection change`() {
        // Test onSelected callback
    }

    @Test
    fun `supports dark mode`() {
        // Test dark theme rendering
    }

    @Test
    fun `announces selection state to TalkBack`() {
        // Accessibility test
    }
}

class ExpansionTileTest {
    @Test
    fun `expands children on click`() {
        // Test expand behavior
    }

    @Test
    fun `animates expansion over 200ms`() {
        // Test animation timing
    }

    @Test
    fun `rotates trailing icon 180 degrees`() {
        // Test icon animation
    }

    @Test
    fun `supports nested expansion tiles`() {
        // Test nesting
    }
}
```

---

## DELIVERABLES CHECKLIST

- [ ] 5 chip variant components implemented
- [ ] 3 list tile variant components implemented
- [ ] 10 advanced Material components implemented
- [ ] Android renderer mappers (18 functions)
- [ ] 90+ unit tests (≥90% coverage)
- [ ] All tests passing
- [ ] Dark mode validated for all components
- [ ] Accessibility audit passed (TalkBack, WCAG AA)
- [ ] Material theming utilities created
- [ ] KDoc documentation for all APIs
- [ ] Code samples for each component
- [ ] Migration guide section (Flutter → AVAMagic Material)

---

## INTEGRATION POINTS

### With Agent 1 (Animation Specialist)
- ExpansionTile needs smooth expand/collapse animation
- Use Agent 1's animation utilities
- Ensure 200ms animation timing

### With Agent 2 (Layout Specialist)
- List tiles will use Agent 2's layout components
- FilterChip may be used in Wrap layouts
- Ensure smooth integration

---

## QUALITY GATES

Before marking work complete:
1. ✅ All 18 components implemented and tested
2. ✅ Test coverage ≥90%
3. ✅ Dark mode validated (no visual bugs)
4. ✅ Accessibility audit passed (TalkBack + WCAG AA)
5. ✅ Material3 theming verified
6. ✅ Zero compiler warnings
7. ✅ KDoc documentation 100%
8. ✅ Code review approved

---

## RESOURCES

### Material Design 3 Specifications
- Material Design 3: https://m3.material.io/
- Chips: https://m3.material.io/components/chips/overview
- List: https://m3.material.io/components/lists/overview
- Buttons: https://m3.material.io/components/buttons/overview

### Jetpack Compose Material3
- Material3 Components: https://developer.android.com/jetpack/compose/designsystems/material3
- Theming: https://developer.android.com/jetpack/compose/designsystems/material3#theming
- Accessibility: https://developer.android.com/jetpack/compose/accessibility

### Flutter Material Reference
- Flutter Material Widgets: https://docs.flutter.dev/development/ui/widgets/material
- FilterChip: https://api.flutter.dev/flutter/material/FilterChip-class.html
- ExpansionTile: https://api.flutter.dev/flutter/material/ExpansionTile-class.html

---

## ACCESSIBILITY CHECKLIST

For each interactive component:
- [ ] Content description provided
- [ ] State changes announced to TalkBack
- [ ] Minimum 48dp touch target
- [ ] Focus order is logical
- [ ] Keyboard navigation supported (where applicable)
- [ ] Color contrast meets WCAG AA (4.5:1 for text, 3:1 for UI elements)
- [ ] Works correctly in TalkBack mode

---

## THEMING VALIDATION

For each component:
- [ ] Light mode rendering verified
- [ ] Dark mode rendering verified
- [ ] Theme switching smooth (no flashing)
- [ ] Custom color scheme supported
- [ ] Material3 color tokens used correctly
- [ ] Typography from Material3 type scale

---

## NEXT STEPS

1. Review this brief thoroughly
2. Set up your development environment
3. Start with FilterChip and ExpansionTile (most important)
4. Then CheckboxListTile, SwitchListTile, FilledButton
5. Implement in priority order (P0 → P1 → P2)
6. Validate accessibility early and often (TalkBack)
7. Test dark mode for every component
8. Commit regularly with descriptive messages
9. Sync with other agents at end of Week 1

---

**Agent Status:** 🟢 READY TO START
**Start Date:** 2025-11-22
**Target Completion:** 2025-12-06 (2 weeks)
**Priority:** P1 (High)

Good luck! 🚀
