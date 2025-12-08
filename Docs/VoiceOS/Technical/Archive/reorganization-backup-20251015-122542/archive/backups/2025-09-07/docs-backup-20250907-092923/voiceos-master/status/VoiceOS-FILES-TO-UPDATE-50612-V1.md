# Files That Need Header Path Updates

## Files to Update:

### Already Updated:
1. ✅ VoiceAccessibilityService.kt
2. ✅ MemoryManager.kt  
3. ✅ IMemoryMonitor.kt

### Need Updates:
4. ⏳ ICommandProcessor.kt
5. ⏳ IOverlayManager.kt
6. ⏳ IRecognitionManager.kt
7. ⏳ CommandProcessor.kt
8. ⏳ OverlayManager.kt
9. ⏳ CompactOverlayView.kt

## Update Pattern:

Replace:
```kotlin
/**
 * [FileName].kt - [Description]
 * 
 * Created: [Date]
```

With:
```kotlin
/**
 * [FileName].kt - [Description]
 * Path: app/src/main/java/com/augmentalis/voiceos/[package]/[FileName].kt
 * 
 * Created: [Date]
```