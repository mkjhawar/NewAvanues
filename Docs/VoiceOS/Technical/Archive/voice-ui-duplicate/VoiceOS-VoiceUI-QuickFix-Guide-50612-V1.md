# VoiceUI Quick Fix Guide
*For Next Development Session*
*45 Errors Remaining*

## Priority 1: Simplified Package (18 errors)

### File: `VoiceScreenScope.kt`
**Problem:** References to non-existent `com.augmentalis.voiceui.simplified.*`

**Quick Fix Option A - Create Package:**
```kotlin
// Create: simplified/SimplifiedComponents.kt
package com.augmentalis.voiceui.simplified

@Composable
fun text(text: String, locale: String? = null, aiContext: AIContext? = null) {
    Text(text = text)
}

@Composable  
fun input(label: String, value: String, locale: String? = null, onValueChange: ((String) -> Unit)? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange ?: {},
        label = { Text(label) }
    )
}
// ... etc for all functions
```

**Quick Fix Option B - Replace in VoiceScreenScope:**
Replace all `com.augmentalis.voiceui.simplified.*` with direct implementations already started.

## Priority 2: Animation Imports (5 errors)

### File: `ElementAnimation.kt`
**Add these imports:**
```kotlin
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
```

**Create custom easing:**
```kotlin
val EASE_OUT_BACK = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
val EASE_OUT_BOUNCE = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f) 
val EASE_OUT_ELASTIC = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
```

## Priority 3: SpacerSize Enum (1 error)

### File: `VoiceScreen.kt` Line 198
**Change:**
```kotlin
SpacerSize.XSMALL -> 4.dp
```
**To:**
```kotlin
// Remove XSMALL or add it to enum
```

## Priority 4: VoiceUIElement Issues (6 errors)

### File: `VoiceScreen.kt` Lines 262, 270
**Current:**
```kotlin
VoiceUIElement(
    content = "text",
    properties = mapOf()
)
```
**Fix:**
```kotlin
VoiceUIElement(
    type = ElementType.TEXT,
    name = "text"
)
```

## Priority 5: DeviceProfile Issues (10 errors)

### File: `ThemeIntegrationPipeline.kt`
**Remove these non-existent fields:**
- supportsDPad
- supportsRemoteControl
- supportsHandTracking
- supportsControllers

**Fix multiplication at lines 408-409:**
```kotlin
// Change from:
fontSize = baseSize * 1.2f
// To:
fontSize = (baseSize * 1.2f).toFloat()
```

## Priority 6: @Composable Context (2 errors)

### File: `VoiceScreenScope.kt` Lines 241, 254
**Add @Composable:**
```kotlin
@Composable
fun row(...) { 
    Row { content() }
}

@Composable
fun column(...) {
    Column { content() }
}
```

## Compilation Command
```bash
cd "/Volumes/M Drive/Coding/Warp/VOS4"
./gradlew :apps:VoiceUI:compileDebugKotlin --console=plain
```

## Expected Result After Fixes
- 0 compilation errors
- VoiceUI module builds successfully
- Main app unblocked
- Ready for integration testing

## Time Estimate
- Priority 1-2: 45 minutes
- Priority 3-4: 30 minutes
- Priority 5-6: 30 minutes
- Testing: 15 minutes
- **Total: ~2 hours**