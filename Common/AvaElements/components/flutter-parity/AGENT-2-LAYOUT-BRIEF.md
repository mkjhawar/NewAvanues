# Agent 2: Layout Specialist - Implementation Brief

**Agent ID:** AGENT-2-LAYOUT
**Responsibility:** Layout & Scrolling Components (17 total)
**Timeline:** 2 weeks (85-102 hours)
**Priority:** P0 (Critical)

---

## MISSION

Implement all 17 layout and scrolling components to achieve Flutter layout parity on Android platform. Focus on performance (lazy loading, smooth scrolling) and layout algorithm accuracy.

---

## YOUR COMPONENTS

### Week 1: Flex & Positioning (10 components)

**Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonMain/kotlin/com/augmentalis/avaelements/flutter/layout/`

1. **Wrap.kt** (P0)
   - Flow layout that wraps children to next line
   - Use: `FlowRow`, `FlowColumn`
   - Tests: Wrapping behavior, spacing, alignment, RTL support

2. **Expanded.kt** (P0)
   - Fills available space in flex container (weight = 1f, fill = true)
   - Use: `Modifier.weight(1f)`
   - Tests: Space distribution, multiple expanded children, flex interaction

3. **Flexible.kt** (P0)
   - Flexible sizing in flex container (weight with fill = false)
   - Use: `Modifier.weight(flex, fill = false)`
   - Tests: Flex factor application, min/max sizing, shrink behavior

4. **Flex.kt** (P1)
   - Generic flex container (Row/Column abstraction)
   - Use: `Row`/`Column` with direction parameter
   - Tests: Direction switching, main/cross axis alignment, spacing

5. **Padding.kt** (P0)
   - Explicit padding widget (wrapper around Modifier.padding)
   - Use: `Box(Modifier.padding())`
   - Tests: All-sides padding, per-side, symmetric, insets

6. **Align.kt** (P0)
   - Aligns child within parent
   - Use: `Box(contentAlignment = ...)`
   - Tests: All 9 alignment positions, RTL support, size constraints

7. **Center.kt** (P0)
   - Centers child (shorthand for Align with center)
   - Use: `Box(contentAlignment = Alignment.Center)`
   - Tests: Centering behavior, size constraints, nested centering

8. **SizedBox.kt** (P0)
   - Fixed-size container or spacer
   - Use: `Box(Modifier.size())` or `Spacer()`
   - Tests: Width-only, height-only, both, zero-size edge case

9. **ConstrainedBox.kt** (P1)
   - Applies additional constraints to child
   - Use: `Box(Modifier.constraints())`
   - Tests: Min/max width/height, constraint composition, conflict resolution

10. **FittedBox.kt** (P2)
    - Scales and positions child to fit parent
    - Use: Custom layout with scale calculation
    - Tests: BoxFit modes (contain, cover, fill, fitWidth, fitHeight, none), aspect ratio

---

### Week 2: Advanced Scrolling (7 components)

**Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonMain/kotlin/com/augmentalis/avaelements/flutter/layout/scrolling/`

11. **ListViewBuilder.kt** (P0 - CRITICAL)
    - Lazy list with item builder function
    - Use: `LazyColumn` with `itemsIndexed()`
    - Tests: 10K items, scroll performance, item recycling, lazy loading

12. **ListViewSeparated.kt** (P0)
    - List with separators between items
    - Use: `LazyColumn` with separator items
    - Tests: Separator rendering, spacing, last item handling

13. **GridViewBuilder.kt** (P0 - CRITICAL)
    - Lazy grid with item builder function
    - Use: `LazyVerticalGrid`
    - Tests: Column count, spacing, scroll performance, dynamic sizing

14. **PageView.kt** (P0)
    - Swipeable pages (horizontal/vertical)
    - Use: `HorizontalPager`/`VerticalPager` from accompanist or Compose Foundation
    - Tests: Page switching, scroll physics, page indicators, infinite scroll

15. **ReorderableListView.kt** (P1)
    - List with drag-to-reorder functionality
    - Use: `LazyColumn` with `ReorderableItem` modifier
    - Tests: Drag detection, reorder animation, callback, long-press threshold

16. **CustomScrollView.kt** (P1)
    - Custom scrollable with slivers
    - Use: `LazyColumn` with mixed content types
    - Tests: Multiple sliver types, scroll coordination, sticky headers

17. **SliverList / SliverGrid.kt** (P1)
    - Sliver versions of list/grid for CustomScrollView
    - Use: Integrated with CustomScrollView implementation
    - Tests: Sliver protocol, scroll effects, app bar collapse

---

## ANDROID RENDERER IMPLEMENTATION

**Path:** `Universal/Libraries/AvaElements/Renderers/Android/src/androidMain/kotlin/com/augmentalis/avaelements/renderer/android/mappers/flutterparity/FlutterParityLayoutMappers.kt`

Create mapper functions for all 17 components:

```kotlin
@Composable
fun WrapMapper(component: WrapComponent) {
    FlowRow(
        horizontalArrangement = component.spacing.toArrangement(),
        verticalAlignment = component.alignment.toAlignment()
    ) {
        component.children.forEach { child ->
            RenderChild(child)
        }
    }
}

@Composable
fun ListViewBuilderMapper(component: ListViewBuilderComponent) {
    LazyColumn(
        state = component.scrollController.toScrollState()
    ) {
        itemsIndexed(component.itemCount) { index, _ ->
            component.itemBuilder(index)
        }
    }
}

// ... 17 total mappers
```

---

## TECHNICAL REQUIREMENTS

### Performance (CRITICAL)
- Lists/grids with 10K+ items MUST maintain 60 FPS scrolling
- Item recycling MUST be efficient (memory ≤100 MB for large lists)
- Lazy loading MUST work correctly (only render visible items + buffer)
- Profile with Android Studio Profiler for memory and frame rate

### Layout Accuracy
- Layout calculations MUST match Flutter's algorithm
- Constraints MUST propagate correctly through layout tree
- RTL (right-to-left) MUST be supported for all components
- Intrinsic measurements MUST be accurate

### Testing
- Minimum 90% code coverage
- At least 3 tests per component:
  1. Basic layout behavior
  2. Performance test (if scrollable)
  3. Edge case (empty, single item, constraints)
- Integration tests with complex nested layouts

### Documentation
- KDoc for all public APIs
- Code sample for each component
- Flutter equivalent comparison
- Performance characteristics

---

## TESTING STRATEGY

**Test Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonTest/kotlin/layout/`

### Example Test Structure
```kotlin
class ListViewBuilderTest {
    @Test
    fun `renders 10000 items with lazy loading`() {
        // Test large list rendering
    }

    @Test
    fun `maintains 60 FPS during scroll`() {
        // Performance test
    }

    @Test
    fun `recycles items efficiently`() {
        // Memory test
    }

    @Test
    fun `handles empty list gracefully`() {
        // Edge case
    }
}

class WrapTest {
    @Test
    fun `wraps children to next row when overflow`() {
        // Wrapping behavior
    }

    @Test
    fun `respects spacing parameter`() {
        // Spacing test
    }

    @Test
    fun `supports RTL layout`() {
        // RTL test
    }
}
```

---

## DELIVERABLES CHECKLIST

- [ ] 10 flex/positioning components implemented
- [ ] 7 advanced scrolling components implemented
- [ ] Android renderer mappers (17 functions)
- [ ] 85+ unit tests (≥90% coverage)
- [ ] All tests passing
- [ ] Performance benchmarks (60 FPS scrolling, memory efficiency)
- [ ] RTL support validated
- [ ] KDoc documentation for all APIs
- [ ] Code samples for each component
- [ ] Layout utilities helper class
- [ ] Migration guide section (Flutter → AVAMagic layouts)

---

## INTEGRATION POINTS

### With Agent 1 (Animation Specialist)
- Your layout components will be used in animations (AnimatedSize, AnimatedPositioned)
- Hero transitions may use your layout calculations
- AnimatedList needs your ListView.builder implementation

### With Agent 3 (Material Design Specialist)
- Material components will use your layout widgets
- Lists and grids will contain Material components
- Ensure smooth integration

---

## QUALITY GATES

Before marking work complete:
1. ✅ All 17 components implemented and tested
2. ✅ Test coverage ≥90%
3. ✅ Performance benchmarks met (60 FPS, memory efficiency)
4. ✅ RTL support verified
5. ✅ Zero compiler warnings
6. ✅ KDoc documentation 100%
7. ✅ Code review approved

---

## RESOURCES

### Jetpack Compose Layout APIs
- `FlowRow` / `FlowColumn` - Flow layouts
- `Modifier.weight()` - Flex weights
- `Box` - Basic positioning
- `LazyColumn` / `LazyRow` - Lazy lists
- `LazyVerticalGrid` - Lazy grids
- `HorizontalPager` / `VerticalPager` - Paging

### Flutter Layout Reference
- Flutter Layout Widgets: https://docs.flutter.dev/development/ui/widgets/layout
- Wrap: https://api.flutter.dev/flutter/widgets/Wrap-class.html
- Expanded: https://api.flutter.dev/flutter/widgets/Expanded-class.html
- ListView.builder: https://api.flutter.dev/flutter/widgets/ListView-class.html
- GridView.builder: https://api.flutter.dev/flutter/widgets/GridView-class.html

---

## PERFORMANCE TARGETS

| Component | Target | Measurement |
|-----------|--------|-------------|
| ListView.builder (10K items) | 60 FPS | Frame rate during scroll |
| GridView.builder (10K items) | 60 FPS | Frame rate during scroll |
| Memory (ListView 100K items) | <100 MB | Memory profiler |
| Item recycling | Reuse >90% | Object allocation tracking |

---

## NEXT STEPS

1. Review this brief thoroughly
2. Set up your development environment
3. Start with Wrap, Expanded, Flexible (most critical)
4. Then implement ListView.builder and GridView.builder (P0)
5. Implement in priority order (P0 → P1 → P2)
6. Commit regularly with descriptive messages
7. Sync with other agents at end of Week 1

---

**Agent Status:** 🟢 READY TO START
**Start Date:** 2025-11-22
**Target Completion:** 2025-12-06 (2 weeks)
**Priority:** P0 (Critical Path)

Good luck! 🚀
