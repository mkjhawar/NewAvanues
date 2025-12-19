# SOLID Refactoring Quick Reference

**Quick guide for using the newly refactored SOLID-compliant components.**

---

## AndroidVoiceCursor - Adapter Pattern

### Standard Usage (Unchanged)

```kotlin
// In Application.onCreate() or Activity.onCreate()
initializeVoiceCursor(context)

// Use normally
val manager = getVoiceCursorManager()
if (manager.isAvailable) {
    manager.start()
}
```

### Testing with Mocks

```kotlin
// Create mock adapter
class MockVoiceOSAdapter : VoiceOSAdapter {
    override val isAvailable = true
    val registeredTargets = mutableListOf<String>()

    override fun registerClickTarget(...) {
        registeredTargets.add(targetId)
    }
    // ... implement other methods
}

// Use in test
val mockAdapter = MockVoiceOSAdapter()
initializeVoiceCursor(context, mockAdapter)

// Verify
assert(mockAdapter.registeredTargets.contains("button1"))
```

### Custom Adapter (Logging)

```kotlin
class LoggingVoiceOSAdapter(
    private val delegate: VoiceOSAdapter
) : VoiceOSAdapter by delegate {
    override fun registerClickTarget(...) {
        println("LOG: Registering $targetId")
        delegate.registerClickTarget(...)
    }
}

// Initialize
val baseAdapter = ReflectionVoiceOSAdapter.create(context)
val loggingAdapter = LoggingVoiceOSAdapter(baseAdapter)
initializeVoiceCursor(context, loggingAdapter)
```

---

## SpacingScale - Provider Pattern

### Standard Usage (Unchanged)

```kotlin
// Use Material spacing (default)
val padding = SpacingScale.MD      // 12dp
val margin = SpacingScale.LG       // 16dp

// EdgeInsets
val insets = EdgeInsets.all(SpacingScale.MD)

// Get by name
val spacing = SpacingScale.byName("xl")  // 24dp

// Get by multiplier
val custom = SpacingScale.get(5f)  // 20dp
```

### Custom Spacing Scale

```kotlin
// Define custom spacing
object LargeSpacing : SpacingScaleProvider {
    override val base = 8f
    override val none = 0f
    override val xxs = 2f
    override val xs = 4f
    override val sm = 8f
    override val md = 16f
    override val lg = 24f
    override val xl = 32f
    override val xxl = 48f
    override val xxxl = 64f
}

// Apply it
SpacingScale.setProvider(LargeSpacing)

// Now all spacing is doubled
val padding = SpacingScale.MD  // 16dp instead of 12dp

// Reset to default
SpacingScale.resetToDefault()
```

### Platform-Specific Spacing

```kotlin
// In platform initialization code
when (Platform.current) {
    Platform.Android -> {
        SpacingScale.setProvider(MaterialSpacingScale)
    }
    Platform.iOS -> {
        SpacingScale.setProvider(AppleSpacingScale)
    }
    Platform.Desktop -> {
        SpacingScale.setProvider(LargeSpacingScale)
    }
}
```

### Accessibility Scaling

```kotlin
class AccessibilitySpacing(
    private val scaleFactor: Float = 1.5f
) : SpacingScaleProvider {
    override val base = MaterialSpacingScale.base * scaleFactor
    override val md = MaterialSpacingScale.md * scaleFactor
    // ... scale all values
}

// Enable accessibility
fun enableAccessibilityMode(enabled: Boolean) {
    if (enabled) {
        SpacingScale.setProvider(AccessibilitySpacing(1.5f))
    } else {
        SpacingScale.resetToDefault()
    }
}
```

---

## File Locations

### VoiceOS Adapter Files
```
Universal/Libraries/AvaElements/Core/src/androidMain/kotlin/
  com/augmentalis/avaelements/input/
    ├── VoiceOSAdapter.kt              (Interface)
    ├── ReflectionVoiceOSAdapter.kt    (Production impl)
    ├── NoOpVoiceOSAdapter.kt          (Null-object impl)
    ├── AndroidVoiceCursor.kt          (Updated)
    └── VoiceOSAdapterExample.kt       (Examples)
```

### Spacing Scale Files
```
Universal/Libraries/AvaElements/Core/src/commonMain/kotlin/
  com/augmentalis/avaelements/common/spacing/
    ├── SpacingUtils.kt               (Updated)
    └── SpacingScaleExample.kt        (Examples)
```

---

## Migration Checklist

### For Existing Code
- ✅ No changes required
- ✅ All existing APIs work unchanged

### For New Features

#### VoiceOS Integration
- [ ] Import `VoiceOSAdapter` if creating custom adapter
- [ ] Call `initializeVoiceCursor(context, adapter)` with custom adapter
- [ ] Use standard `getVoiceCursorManager()` API

#### Custom Spacing
- [ ] Create `SpacingScaleProvider` implementation
- [ ] Call `SpacingScale.setProvider(customProvider)`
- [ ] All existing `SpacingScale.MD` etc. calls work with new values

---

## Common Patterns

### 1. Testing VoiceCursor

```kotlin
@Test
fun testTargetRegistration() {
    // Arrange
    val mockAdapter = MockVoiceOSAdapter()
    initializeVoiceCursor(context, mockAdapter)
    val manager = getVoiceCursorManager()

    // Act
    manager.registerTarget(myTarget)

    // Assert
    assert(mockAdapter.registeredTargets.contains("myTarget"))
}
```

### 2. Conditional Features

```kotlin
class FeatureGatedAdapter(
    private val delegate: VoiceOSAdapter,
    private val hasFeature: Boolean
) : VoiceOSAdapter {
    override val isAvailable = hasFeature && delegate.isAvailable
    // ... delegate or no-op based on hasFeature
}
```

### 3. Dynamic Spacing

```kotlin
// Update spacing at runtime based on preferences
fun updateSpacing(size: String) {
    when (size) {
        "compact" -> SpacingScale.setProvider(CompactSpacing)
        "normal" -> SpacingScale.setProvider(MaterialSpacingScale)
        "large" -> SpacingScale.setProvider(LargeSpacing)
    }
}
```

### 4. Theme-Based Spacing

```kotlin
class ThemeSpacing(
    private val theme: Theme
) : SpacingScaleProvider {
    override val base = if (theme.isDark) 4f else 6f
    // ... adjust spacing based on theme
}

fun applyTheme(theme: Theme) {
    SpacingScale.setProvider(ThemeSpacing(theme))
}
```

---

## SOLID Benefits Achieved

| Principle | AndroidVoiceCursor | SpacingScale |
|-----------|-------------------|--------------|
| **SRP** | ✅ Separated reflection from management | ✅ Separated values from system |
| **OCP** | ✅ Add adapters without changes | ✅ Add providers without changes |
| **LSP** | ✅ All adapters substitutable | ✅ All providers substitutable |
| **ISP** | ✅ Focused VoiceOS interface | ✅ Focused spacing interface |
| **DIP** | ✅ Depends on abstraction | ✅ Depends on abstraction |

---

## Performance Notes

- **Zero overhead**: All delegation is inline-optimized
- **Single instance**: Singleton pattern maintains efficiency
- **Backward compatible**: Existing code runs identically

---

## Troubleshooting

### VoiceOS not working
```kotlin
// Check adapter availability
val manager = getVoiceCursorManager()
println("Available: ${manager.isAvailable}")

// Verify initialization
initializeVoiceCursor(context)
```

### Custom spacing not applying
```kotlin
// Verify provider is set
println("Current provider: ${SpacingScale.getProvider()}")

// Check values
println("MD spacing: ${SpacingScale.MD}")

// Reset if needed
SpacingScale.resetToDefault()
```

---

**Last Updated:** November 26, 2025
**See also:** SOLID-REFACTORING-SUMMARY.md
