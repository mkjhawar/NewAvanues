# UI Overlays Porting Plan - Legacy Avenue to VOS4

**Date:** 2025-09-03
**Status:** Analysis Complete, Implementation Pending

## Executive Summary

Comprehensive plan to port 6 missing UI overlay components from Legacy Avenue to VOS4's modern Compose-based architecture.

## Missing Components (Priority Order)

### HIGH Priority
1. **VoiceCommandOverlayView** → **CommandLabelOverlay**
   - Displays voice command labels over UI elements
   - Dynamic positioning with collision detection
   - Object pooling for performance

2. **DuplicateCommandView** → **CommandDisambiguationOverlay**
   - Shows numbered selection for duplicate commands
   - Custom canvas drawing with transparency
   - Multi-language number conversion

### MEDIUM Priority
3. **VoiceInitializeView** → **ServiceStatusOverlay**
   - Service initialization status display
   - Auto-dismissal after state change
   - Localized status messages

4. **Click Animation Overlays** → **ClickFeedbackOverlay**
   - GazeClickView: Multi-stage animation for gaze clicks
   - VoiceCommandClickView: Lottie animation for voice clicks
   - Position-based overlay with auto-cleanup

### LOW Priority
5. **StartupVoiceView** → **OnboardingOverlay**
   - App startup guidance
   - Navigation instructions
   - Full-width notification style

## Implementation Strategy

### Phase 1: Infrastructure (Week 1)
- [ ] Port 29 drawable resources to vector format
- [ ] Migrate string resources to VOS4 localization
- [ ] Add Lottie-Compose dependency
- [ ] Create model classes (VoiceCommand, DuplicateCommandModel)

### Phase 2: Core Overlays (Weeks 2-3)
- [ ] Implement CommandLabelOverlay with Compose LazyColumn
- [ ] Implement CommandDisambiguationOverlay with Compose Canvas
- [ ] Add collision detection utilities
- [ ] Port NumberHelper for disambiguation

### Phase 3: Feedback Overlays (Week 4)
- [ ] Implement ServiceStatusOverlay
- [ ] Implement ClickFeedbackOverlay with animations
- [ ] Integration with VoiceAccessibility service

### Phase 4: Enhancement (Week 5)
- [ ] Implement OnboardingOverlay
- [ ] Performance optimization
- [ ] Comprehensive testing

## Technical Approach

### Base Architecture
```kotlin
// All overlays extend VOS4's BaseOverlay
class CommandLabelOverlay : BaseOverlay(context, OverlayType.POSITIONED) {
    @Composable
    override fun OverlayContent() {
        // Compose UI implementation
    }
}
```

### Key Conversions
| Legacy | VOS4 | 
|--------|------|
| Android Views | Compose UI |
| WindowManager | BaseOverlay management |
| Object pooling | LazyColumn optimization |
| Canvas drawing | Compose Canvas API |
| LottieAnimationView | Lottie-Compose |

## Performance Requirements
- Startup: <100ms per overlay
- Memory: <5MB per overlay instance
- Rendering: 60fps smooth animations
- Cleanup: Proper resource disposal

## Testing Strategy
1. Unit tests for each overlay component
2. Integration tests with accessibility service
3. Performance benchmarks
4. Memory leak detection
5. Cross-device compatibility

## Success Metrics
- [ ] 100% functional parity with legacy
- [ ] Performance targets met
- [ ] Zero memory leaks
- [ ] Accessibility compliance
- [ ] VOS4 design system consistency

## Risk Mitigation
- **High Risk:** Complex positioning algorithms
  - Mitigation: Incremental porting with extensive testing
- **Medium Risk:** Performance impact
  - Mitigation: Profiling and optimization
- **Low Risk:** Resource migration
  - Mitigation: Automated conversion tools

## Estimated Timeline
**Total: 5 weeks** for complete implementation and testing

## Dependencies
- Lottie-Compose library
- VOS4 BaseOverlay infrastructure (existing)
- VoiceAccessibility service integration
- Resource migration tools