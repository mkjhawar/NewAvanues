# Agent 1: Animation Specialist - Implementation Brief

**Agent ID:** AGENT-1-ANIMATION
**Responsibility:** Animation & Transition Components (23 total)
**Timeline:** 2 weeks (115-138 hours)
**Priority:** P0 (Critical)

---

## MISSION

Implement all 23 animation and transition components to achieve Flutter animation parity on Android platform. Focus on performance (60 FPS minimum) and API compatibility with Flutter's animation system.

---

## YOUR COMPONENTS

### Week 1: Implicit Animations (8 components)

**Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonMain/kotlin/com/augmentalis/avaelements/flutter/animation/`

1. **AnimatedContainer.kt** (P0)
   - Animates size, padding, color, decoration changes
   - Use: `animateDpAsState()`, `animateColorAsState()`
   - Tests: Size animation, color transition, curve application, completion callback

2. **AnimatedOpacity.kt** (P0)
   - Animates opacity changes (fade in/out)
   - Use: `animateFloatAsState()` for alpha channel
   - Tests: Fade in, fade out, custom duration, curve control

3. **AnimatedPositioned.kt** (P0)
   - Animates position changes within Stack/Box
   - Use: `animateDpAsState()` for offset values
   - Tests: Position interpolation, constraint handling, smooth movement

4. **AnimatedDefaultTextStyle.kt** (P1)
   - Animates text style changes (color, size, weight)
   - Use: Compose `TextStyle` with animate*AsState
   - Tests: Font size animation, color transition, style blending

5. **AnimatedPadding.kt** (P1)
   - Animates padding changes
   - Use: `animateDpAsState()` for padding values
   - Tests: All-sides padding, per-side animation, smooth layout

6. **AnimatedSize.kt** (P1)
   - Animates size changes with automatic layout
   - Use: `animateContentSize()` modifier
   - Tests: Size expansion/contraction, constraint satisfaction

7. **AnimatedAlign.kt** (P1)
   - Animates alignment changes
   - Use: `animateAlignmentAsState()`
   - Tests: All alignment positions, smooth interpolation

8. **AnimatedScale.kt** (P1)
   - Animates scale transformations
   - Use: `animateFloatAsState()` for scale factor
   - Tests: Scale up/down, anchor point, layout impact

---

### Week 2: Transitions & Hero (15 components)

**Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonMain/kotlin/com/augmentalis/avaelements/flutter/animation/transitions/`

9. **FadeTransition.kt** (P0)
   - Explicit fade animation (enter/exit)
   - Use: `AnimatedVisibility` with fadeIn/fadeOut
   - Tests: Enter animation, exit animation, custom duration

10. **SlideTransition.kt** (P0)
    - Explicit slide animation (from edges)
    - Use: `slideIntoContainer()`, `slideOutOfContainer()`
    - Tests: All slide directions, offset calculation

11. **ScaleTransition.kt** (P0)
    - Explicit scale animation
    - Use: `scaleIn()`, `scaleOut()` with AnimatedVisibility
    - Tests: Scale from center, custom pivot, combined with other transitions

12. **RotationTransition.kt** (P0)
    - Explicit rotation animation
    - Use: `Modifier.graphicsLayer { rotationZ = ... }` with animation
    - Tests: Full rotation, partial rotation, rotation direction

13. **Hero.kt** (P0 - CRITICAL)
    - Shared element transition across screens
    - Use: Navigation Compose shared element API
    - Tests: Cross-screen animation, tag matching, performance, nested navigation

14. **PositionedTransition.kt** (P1)
    - Animates Positioned widget bounds
    - Use: Animated offset with Box positioning
    - Tests: Smooth position interpolation, constraint changes

15. **SizeTransition.kt** (P1)
    - Animates size with axis alignment
    - Use: `animateContentSize()` with axis control
    - Tests: Horizontal/vertical expansion, alignment during resize

16. **AnimatedCrossFade.kt** (P1)
    - Cross-fades between two children
    - Use: `Crossfade()` composable
    - Tests: Smooth transition, child replacement, duration control

17. **AnimatedSwitcher.kt** (P1)
    - Switches between children with animation
    - Use: `AnimatedContent()` with custom transitions
    - Tests: Child switching, transition customization, key-based switching

18. **AnimatedList.kt** (P2)
    - List with animated insertions/removals
    - Use: `LazyColumn` with `animateItemPlacement()`
    - Tests: Item addition animation, removal animation, reordering

19. **AnimatedModalBarrier.kt** (P2)
    - Animated barrier for modals/dialogs
    - Use: `Box` with animated background + click interception
    - Tests: Fade in barrier, dismiss animation, click handling

20. **DecoratedBoxTransition.kt** (P2)
    - Animates BoxDecoration changes
    - Use: Animated border, background, shadow
    - Tests: Border animation, background color, shadow transition

21. **AlignTransition.kt** (P2)
    - Animates alignment as transition
    - Use: `Alignment` with animation state
    - Tests: Alignment interpolation, smooth movement

22. **DefaultTextStyleTransition.kt** (P2)
    - Animates text style as transition
    - Use: Animated `TextStyle` properties
    - Tests: Style blending, smooth transitions

23. **RelativePositionedTransition.kt** (P2)
    - Animates relative position
    - Use: Animated offset relative to parent
    - Tests: Relative positioning, constraint handling

---

## ANDROID RENDERER IMPLEMENTATION

**Path:** `Universal/Libraries/AvaElements/Renderers/Android/src/androidMain/kotlin/com/augmentalis/avaelements/renderer/android/mappers/flutterparity/FlutterParityAnimationMappers.kt`

Create mapper functions that convert common component definitions to Jetpack Compose implementations:

```kotlin
@Composable
fun AnimatedContainerMapper(component: AnimatedContainerComponent) {
    // Convert to Compose AnimatedContainer implementation
}

@Composable
fun FadeTransitionMapper(component: FadeTransitionComponent) {
    // Convert to Compose FadeTransition implementation
}

// ... 23 total mappers
```

---

## TECHNICAL REQUIREMENTS

### Performance (CRITICAL)
- All animations MUST maintain 60 FPS minimum
- Janky frames (>16ms) MUST be <5% of total
- Memory usage MUST NOT exceed Flutter equivalent by >10%
- Profile all animations with Android Studio Profiler

### API Design
- Match Flutter's animation API where possible
- Use Compose idioms when Flutter API doesn't translate well
- Support all standard curves (linear, easeIn, easeOut, easeInOut, etc.)
- Completion callbacks for all animations

### Testing
- Minimum 90% code coverage
- At least 3 tests per component:
  1. Basic functionality test
  2. Performance/timing test
  3. Edge case test (null, extreme values, etc.)
- Integration tests with navigation (for Hero)

### Documentation
- KDoc for all public APIs
- Code sample for each component
- Flutter equivalent comparison in docs
- Performance characteristics documented

---

## TESTING STRATEGY

**Test Path:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonTest/kotlin/animation/`

### Example Test Structure
```kotlin
class AnimatedContainerTest {
    @Test
    fun `animates size change smoothly`() {
        // Test size animation
    }

    @Test
    fun `respects animation curve`() {
        // Test curve application
    }

    @Test
    fun `completion callback fires correctly`() {
        // Test callback
    }

    @Test
    fun `maintains 60 FPS during animation`() {
        // Performance test
    }
}
```

---

## DELIVERABLES CHECKLIST

- [ ] 8 implicit animation components implemented
- [ ] 15 transition components implemented
- [ ] Android renderer mappers (23 functions)
- [ ] 90+ unit tests (≥90% coverage)
- [ ] All tests passing
- [ ] Performance benchmarks (60 FPS validation)
- [ ] KDoc documentation for all APIs
- [ ] Code samples for each component
- [ ] Animation utilities helper class
- [ ] Migration guide section (Flutter → AVAMagic animations)

---

## INTEGRATION POINTS

### With Agent 2 (Layout Specialist)
- Hero transitions may need layout components
- AnimatedSize interacts with layout system
- AnimatedPositioned works within layout containers

### With Agent 3 (Material Design Specialist)
- Material components will use your animations
- Ensure smooth integration with Material3 theming

---

## QUALITY GATES

Before marking work complete:
1. ✅ All 23 components implemented and tested
2. ✅ Test coverage ≥90%
3. ✅ Performance benchmarks met (60 FPS)
4. ✅ Zero compiler warnings
5. ✅ KDoc documentation 100%
6. ✅ Code review approved

---

## RESOURCES

### Jetpack Compose Animation APIs
- `animate*AsState()` - State-based animations
- `AnimatedVisibility` - Enter/exit animations
- `Crossfade` - Content crossfade
- `AnimatedContent` - Content switching
- `animateContentSize()` - Size animations
- Shared Element Transitions (Navigation Compose)

### Flutter Animation Reference
- Flutter Animation Docs: https://docs.flutter.dev/development/ui/animations
- AnimatedContainer: https://api.flutter.dev/flutter/widgets/AnimatedContainer-class.html
- Transitions: https://api.flutter.dev/flutter/widgets/FadeTransition-class.html
- Hero: https://api.flutter.dev/flutter/widgets/Hero-class.html

---

## NEXT STEPS

1. Review this brief thoroughly
2. Set up your development environment
3. Start with AnimatedContainer (most critical)
4. Implement in priority order (P0 → P1 → P2)
5. Commit regularly with descriptive messages
6. Sync with other agents at end of Week 1

---

**Agent Status:** 🟢 READY TO START
**Start Date:** 2025-11-22
**Target Completion:** 2025-12-06 (2 weeks)
**Priority:** P0 (Critical Path)

Good luck! 🚀
