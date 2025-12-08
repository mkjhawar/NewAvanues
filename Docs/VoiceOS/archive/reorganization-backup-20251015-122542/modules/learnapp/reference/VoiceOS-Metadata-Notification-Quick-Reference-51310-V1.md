# Metadata Notification System - Quick Reference

**Last Updated:** 2025-10-13 01:40:38 PDT
**Module:** LearnApp
**Version:** 1.0.0

---

## 30-Second Integration

```kotlin
// 1. Initialize
val queue = MetadataNotificationQueue(context)
val notification = InsufficientMetadataNotification(context, queue)

// 2. Assess during exploration
val quality = MetadataQuality.assess(element)
if (MetadataQuality.requiresNotification(quality)) {
    queue.queueNotification(element, quality, screenHash)
}

// 3. Show when ready
if (queue.isReadyToShow()) {
    notification.showNextNotification(
        onLabelProvided = { item, label -> saveLabel(item, label) },
        onSkip = { continueExploration() },
        onSkipAll = { queue.skipAllForSession() }
    )
}
```

---

## Quality Levels

| Level | Identifiers | Action |
|-------|-------------|--------|
| EXCELLENT | 3 (text + desc + id) | ✅ None |
| GOOD | 2 | ✅ None |
| ACCEPTABLE | 1 | ⚠️ Optional |
| POOR | 0 | 🔴 Notify |

---

## Key Classes

### MetadataQuality
```kotlin
// Assess quality
val quality = MetadataQuality.assess(element)

// Check if notification needed
if (MetadataQuality.requiresNotification(quality)) { }

// Get quality score (0-100)
val score = MetadataQuality.getScore(quality)
```

### MetadataNotificationQueue
```kotlin
// Configure
queue.setBatchSize(5)           // Show after 5 items
queue.setMaxQueueSize(50)       // Max 50 in queue

// Queue item
queue.queueNotification(element, quality, screenHash)

// Check status
queue.isReadyToShow()           // Batch ready?
queue.getCurrentSize()          // Items in queue
queue.skipAllForSession.value   // Skip all active?

// Management
queue.clearQueue()              // Clear all
queue.resetSkipAll()            // Re-enable notifications
```

### InsufficientMetadataNotification
```kotlin
// Show notification
notification.showNextNotification(
    onLabelProvided = { item, label -> },
    onSkip = { },
    onSkipAll = { }
)

// Status
notification.isNotificationVisible()
notification.getCurrentItem()

// Cleanup
notification.hideNotification()
notification.cleanup()
```

---

## UI Components

### Notification Overlay
- Position: Bottom of screen
- Style: Material Design 3 CardView
- Actions: Skip All, Skip, Provide Label
- Dismissible: Yes (close button or skip)

### Manual Label Dialog
- Element preview (type, details, bounds)
- Text input (2-50 chars)
- Quick select chips (suggestions)
- Validation: Real-time
- Actions: Cancel, Save

---

## File Locations

### Kotlin (6 files)
```
metadata/
├── MetadataQuality.kt (197 lines)
└── MetadataNotificationQueue.kt (266 lines)

ui/metadata/
├── InsufficientMetadataNotification.kt (252 lines)
├── MetadataNotificationView.kt (274 lines)
└── ManualLabelDialog.kt (217 lines)

examples/
└── MetadataNotificationExample.kt (202 lines)
```

### Resources (8 files)
```
res/
├── layout/
│   ├── insufficient_metadata_notification.xml
│   ├── metadata_suggestion_item.xml
│   └── manual_label_dialog.xml
├── values/
│   ├── metadata_notification_strings.xml
│   └── metadata_notification_colors.xml
└── drawable/
    ├── bg_element_info.xml
    ├── ic_warning.xml
    └── ic_close.xml
```

---

## Configuration Options

### Batch Size
```kotlin
queue.setBatchSize(1)   // Show immediately
queue.setBatchSize(5)   // Default (balanced)
queue.setBatchSize(10)  // Less intrusive
```

### Queue Size
```kotlin
queue.setMaxQueueSize(25)   // Small sessions
queue.setMaxQueueSize(50)   // Default
queue.setMaxQueueSize(100)  // Large explorations
```

### Session Control
```kotlin
// Disable notifications
queue.skipAllForSession()

// Re-enable
queue.resetSkipAll()

// Check status
if (queue.skipAllForSession.value) { }
```

---

## Common Patterns

### Pattern 1: Standard Integration
```kotlin
fun exploreScreen(elements: List<ElementInfo>) {
    elements.forEach { element ->
        val quality = MetadataQuality.assess(element)
        if (MetadataQuality.requiresNotification(quality)) {
            queue.queueNotification(element, quality, screenHash)
        }
    }

    if (queue.isReadyToShow()) {
        showNotification()
    }
}
```

### Pattern 2: With Database Persistence
```kotlin
notification.showNextNotification(
    onLabelProvided = { item, label ->
        // Save to database
        learnAppDao.insertManualLabel(
            ManualLabelEntity(
                elementUuid = item.element.uuid!!,
                manualLabel = label,
                screenHash = item.screenHash,
                timestamp = System.currentTimeMillis(),
                appPackage = currentAppPackage
            )
        )
    },
    onSkip = { resumeExploration() },
    onSkipAll = { queue.skipAllForSession() }
)
```

### Pattern 3: App Change Handling
```kotlin
fun onAppChanged(newPackage: String) {
    // Clear queue for new app
    queue.clearQueue()

    // Reset skip state
    queue.resetSkipAll()

    // Hide any visible notification
    notification.hideNotification()
}
```

---

## Error Handling

### Overlay Permission
```kotlin
if (!Settings.canDrawOverlays(context)) {
    // Request permission
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}
```

### Database Errors
```kotlin
try {
    learnAppDao.insertManualLabel(label)
} catch (e: Exception) {
    Log.e(TAG, "Failed to save label", e)
    Toast.makeText(context, "Failed to save label", Toast.LENGTH_SHORT).show()
}
```

### Empty Queue
```kotlin
if (queue.getCurrentSize() == 0) {
    // No notifications to show
    return
}
```

---

## Testing

### Unit Test
```kotlin
@Test
fun testQualityAssessment() {
    val poorElement = ElementInfo(className = "Button")
    assertEquals(MetadataQuality.POOR, MetadataQuality.assess(poorElement))
}
```

### UI Test
```kotlin
@Test
fun testNotificationDisplay() {
    queue.queueNotification(element, MetadataQuality.POOR, "hash")
    notification.showNextNotification(onLabel, onSkip, onSkipAll)

    onView(withId(R.id.text_title)).check(matches(isDisplayed()))
}
```

---

## Performance

- Memory: < 100 KB total
- CPU: < 0.1ms per assessment
- UI Inflation: < 50ms
- Animation: 60fps target

---

## Dependencies

```gradle
implementation "com.google.android.material:material:1.11.0"
implementation "androidx.cardview:cardview:1.0.0"
implementation "androidx.recyclerview:recyclerview:1.3.2"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Overlay not showing | Check `Settings.canDrawOverlays()` |
| Empty suggestions | Check element has resourceId or text |
| Queue not batching | Verify `setBatchSize()` called |
| Notification shows immediately | Batch size = 1, increase it |
| No notifications ever | Check "skip all" state |

---

## Resources

- [Full UI/UX Guide](../user-manual/Metadata-Notification-UI-Guide-251013-0140.md)
- [Implementation Guide](../implementation/Metadata-Notification-Implementation-251013-0140.md)
- [Integration Example](../../../modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/examples/MetadataNotificationExample.kt)

---

**Version:** 1.0.0 | **Status:** ✅ Production Ready
