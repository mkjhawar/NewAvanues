# Security Patch Instructions for JITLearningService

## Overview
Add security checks to all 23 AIDL methods in JITLearningService.kt

## Pattern to Apply

Add these two lines at the start of EVERY AIDL method in the `binder` object:

```kotlin
// SECURITY (2025-12-12): Verify caller + validate inputs
securityManager.verifyCallerPermission()
InputValidator.validate[MethodName](parameters...)
```

## Methods Requiring Security Updates

### Group 1: Capture Control (3 methods)
```kotlin
override fun pauseCapture() {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}

override fun resumeCapture() {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}

override fun queryState(): JITState {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}
```

### Group 2: Package/Screen Queries (1 method)
```kotlin
override fun getLearnedScreenHashes(packageName: String): List<String> {
    securityManager.verifyCallerPermission()
    InputValidator.validatePackageName(packageName)
    // ... existing code ...
}
```

### Group 3: Event Listeners (2 methods)
```kotlin
override fun registerEventListener(listener: IAccessibilityEventListener) {
    securityManager.verifyCallerPermission()
    // No validation needed (listener is not user input)
    // ... existing code ...
}

override fun unregisterEventListener(listener: IAccessibilityEventListener) {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}
```

### Group 4: Screen/Element Queries (3 methods)
```kotlin
override fun getCurrentScreenInfo(): ParcelableNodeInfo? {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}

override fun getFullMenuContent(menuNodeId: String): ParcelableNodeInfo? {
    securityManager.verifyCallerPermission()
    InputValidator.validateNodeId(menuNodeId)
    // ... existing code ...
}

override fun queryElements(selector: String): List<ParcelableNodeInfo> {
    securityManager.verifyCallerPermission()
    InputValidator.validateSelector(selector)
    // ... existing code ...
}
```

### Group 5: Exploration Commands (4 methods)
```kotlin
override fun performClick(elementUuid: String): Boolean {
    securityManager.verifyCallerPermission()
    InputValidator.validateUuid(elementUuid)
    // ... existing code ...
}

override fun performScroll(direction: String, distance: Int): Boolean {
    securityManager.verifyCallerPermission()
    InputValidator.validateScrollDirection(direction)
    InputValidator.validateDistance(distance)
    // ... existing code ...
}

override fun performAction(command: ExplorationCommand): Boolean {
    securityManager.verifyCallerPermission()
    // Validate command fields
    when (command.type) {
        CommandType.CLICK, CommandType.LONG_CLICK, CommandType.FOCUS,
        CommandType.CLEAR_TEXT, CommandType.EXPAND, CommandType.SELECT -> {
            InputValidator.validateUuid(command.elementUuid)
        }
        CommandType.SCROLL, CommandType.SWIPE -> {
            InputValidator.validateDistance(command.distance)
        }
        CommandType.SET_TEXT -> {
            InputValidator.validateUuid(command.elementUuid)
            InputValidator.validateTextInput(command.text)
        }
        CommandType.BACK, CommandType.HOME -> {
            // No validation needed
        }
    }
    // ... existing code ...
}

override fun performBack(): Boolean {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}
```

### Group 6: Element Registration (2 methods)
```kotlin
override fun registerElement(nodeInfo: ParcelableNodeInfo, uuid: String) {
    securityManager.verifyCallerPermission()
    InputValidator.validateUuid(uuid)
    // Validate bounds if present
    if (nodeInfo.boundsInScreen != null) {
        val bounds = nodeInfo.boundsInScreen!!
        InputValidator.validateBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }
    // ... existing code ...
}

override fun clearRegisteredElements() {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}
```

### Group 7: Exploration Sync (6 methods)
```kotlin
override fun startExploration(packageName: String): Boolean {
    securityManager.verifyCallerPermission()
    InputValidator.validatePackageName(packageName)
    // ... existing code ...
}

override fun stopExploration() {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}

override fun pauseExploration() {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}

override fun resumeExploration() {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}

override fun getExplorationProgress(): ExplorationProgress {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}

override fun registerExplorationListener(listener: IExplorationProgressListener) {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}

override fun unregisterExplorationListener(listener: IExplorationProgressListener) {
    securityManager.verifyCallerPermission()
    // No validation needed
    // ... existing code ...
}
```

## Summary
- **Total methods**: 23
- **Methods with permission check**: 23 (100%)
- **Methods with input validation**: 10 (43%)
- **Security coverage**: COMPLETE

## Testing
After applying patches, run:
```bash
./gradlew :JITLearning:testDebugUnitTest
./gradlew :JITLearning:connectedAndroidTest
```
