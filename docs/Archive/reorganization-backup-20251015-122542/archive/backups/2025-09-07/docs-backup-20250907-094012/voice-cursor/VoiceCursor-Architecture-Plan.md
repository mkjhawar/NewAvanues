# VoiceCursor Architectural Fix Plan
**Date:** 2025-01-26  
**Status:** PENDING REVIEW  
**Estimated Time:** 2-3 hours total

## Executive Summary

This document outlines a comprehensive plan to fix critical architectural issues in the VoiceCursor module and its integration with IMUManager. The main issues include:
1. CursorAdapter.kt incorrectly placed under IMUManager
2. Single Responsibility Principle (SRP) violations
3. IMUManager receiving information instead of being a one-way data provider
4. Three cursor functionality bugs (toggle stuck, cursor not moving, blocking input)

## Current Architecture Problems

### 1. CursorAdapter Location Issue
**Current:** `/libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/imu/CursorAdapter.kt`
**Problem:** CursorAdapter is a cursor-specific component living in the generic IMU library
**Impact:** Violates module boundaries and creates inappropriate coupling

### 2. SRP Violations in CursorAdapter
The CursorAdapter currently has 6 distinct responsibilities:
- **Cursor Position Management** (lines 24-26, 143-160)
- **IMU Data Processing** (lines 135-141)
- **Screen Configuration** (lines 78-92)
- **Flow Management** (lines 162-169)
- **Calibration Interface** (lines 94-106)
- **Lifecycle Management** (lines 53-72)

### 3. IMUManager as Two-Way Communication
**Current Flow:** `App → CursorAdapter → IMUManager → CursorAdapter → App`
**Problem:** IMUManager manages consumer lifecycle, tracks activeConsumers, and receives control commands
**Evidence:** 
- `imuManager.startIMUTracking(consumerId)` - IMUManager receives consumer IDs
- `activeConsumers = mutableSetOf<String>()` - IMUManager tracks consumers

### 4. Cursor Functionality Issues
1. **Toggle Stuck:** Settings shows cursor always ON due to missing state synchronization
2. **Cursor Not Moving:** IMU data not properly flowing to CursorView
3. **Blocking Input:** Missing FLAG_NOT_TOUCHABLE in WindowManager.LayoutParams

## Proposed Architecture

### Clean Architecture Pattern
```
┌──────────────────────────────────────────────┐
│                VoiceCursor App                │
├──────────────────────────────────────────────┤
│  VoiceCursor  │  CursorPositionManager  │     │
│               │  (New Component)          │     │
├──────────────────────────────────────────────┤
│           IMUDataStream (New)                 │
├──────────────────────────────────────────────┤
│              IMUManager                       │
│         (Pure Data Provider)                  │
└──────────────────────────────────────────────┘
```

## Implementation Plan

### Phase 1: Relocate and Refactor CursorAdapter (30 mins)

#### Step 1.1: Create New CursorPositionManager
**Location:** `/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/core/CursorPositionManager.kt`

```kotlin
class CursorPositionManager(private val context: Context) {
    // Single Responsibility: Convert IMU data to cursor positions
    private val imuDataStream = IMUDataStream.getInstance(context)
    private val positionFlow = MutableSharedFlow<CursorOffset>()
    
    fun startTracking() {
        imuDataStream.orientationFlow
            .map { convertToCursorPosition(it) }
            .onEach { positionFlow.emit(it) }
            .launchIn(scope)
    }
}
```

#### Step 1.2: Remove CursorAdapter from IMUManager
- Delete `/libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/imu/CursorAdapter.kt`
- Remove all references from IMUManager

### Phase 2: Create IMUDataStream Interface (45 mins)

#### Step 2.1: New Data Stream Component
**Location:** `/libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/imu/IMUDataStream.kt`

```kotlin
class IMUDataStream(context: Context) {
    // Pure data provider - no consumer management
    val orientationFlow: StateFlow<OrientationData>
    val accelerationFlow: StateFlow<AccelerationData>
    val rotationFlow: StateFlow<RotationData>
    
    fun start() // Start sensors
    fun stop()  // Stop sensors
    // No consumer tracking, no IDs, no lifecycle management
}
```

#### Step 2.2: Refactor IMUManager
- Remove `activeConsumers` tracking
- Remove `startIMUTracking(consumerId)` method
- Remove `stopIMUTracking(consumerId)` method
- Keep only sensor data provision methods

### Phase 3: Fix Cursor Issues (45 mins)

#### Fix 3.1: Toggle Stuck Issue
**File:** `VoiceCursorSettingsActivity.kt`
```kotlin
// Line 151 - Add proper state initialization
var isCursorEnabled by remember { 
    mutableStateOf(
        VoiceCursorOverlayService.isRunning(context) // Check actual service state
    )
}

// Add service state checking method
fun isServiceRunning(): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == VoiceCursorOverlayService::class.java.name }
}
```

#### Fix 3.2: Cursor Not Moving
**File:** `VoiceCursorIMUIntegration.kt`
```kotlin
// Ensure proper data flow connection
class VoiceCursorIMUIntegration {
    fun connectToPositionManager() {
        val positionManager = CursorPositionManager(context)
        positionManager.positionFlow
            .onEach { position ->
                // Direct update to CursorView
                onPositionUpdate?.invoke(position)
            }
            .launchIn(scope)
    }
}
```

#### Fix 3.3: Cursor Blocking Input
**File:** `VoiceCursorOverlayService.kt` (Line 332-334)
```kotlin
// Add FLAG_NOT_TOUCHABLE to make cursor non-blocking
WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or  // ADD THIS
WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
```

### Phase 4: Settings Connection Review (30 mins)

#### Fix 4.1: Gaze Control Connection
**File:** `VoiceCursorSettingsActivity.kt` (Lines 344-355)
- ✅ Already properly connected with Intent actions

#### Fix 4.2: Config Updates
**File:** `VoiceCursor.kt` (Lines 142-159)
- ✅ Already properly sending config updates

#### Fix 4.3: Add Missing Gaze Property
**File:** `CursorTypes.kt`
```kotlin
data class CursorConfig(
    // ... existing properties
    val gazeClickDelay: Long = 1500L // Already added ✅
)
```

## Migration Strategy

### Step-by-Step Migration (Recommended Order)

1. **Create new components first** (non-breaking)
   - CursorPositionManager
   - IMUDataStream

2. **Update VoiceCursor to use new components**
   - Update VoiceCursorIMUIntegration
   - Test with both old and new paths

3. **Fix immediate bugs**
   - Add FLAG_NOT_TOUCHABLE
   - Fix service state checking

4. **Deprecate old components**
   - Mark CursorAdapter as @Deprecated
   - Add migration warnings

5. **Remove old components** (in next release)
   - Delete CursorAdapter
   - Clean up IMUManager

## Testing Plan

### Unit Tests
```kotlin
@Test fun testCursorPositionManager_SingleResponsibility()
@Test fun testIMUDataStream_OneWayFlow()
@Test fun testServiceState_ProperlyReflected()
@Test fun testCursorInput_NotBlocked()
```

### Integration Tests
```kotlin
@Test fun testIMUToCursor_DataFlow()
@Test fun testSettingsToService_ConfigSync()
@Test fun testGazeControl_ProperActivation()
```

## Risk Assessment

### Low Risk
- Adding FLAG_NOT_TOUCHABLE (simple flag addition)
- Service state checking (read-only operation)

### Medium Risk  
- Creating new components (parallel to existing)
- Updating data flow paths (can be tested alongside old path)

### High Risk
- Removing CursorAdapter (many dependencies)
- Refactoring IMUManager (core system component)

## Rollback Plan

If issues occur:
1. **Immediate:** Revert FLAG changes
2. **Components:** Keep old CursorAdapter, use feature flag
3. **Full:** Git revert to previous commit

## Success Metrics

### Immediate (After Implementation)
- [ ] Cursor toggle reflects actual state
- [ ] Cursor moves with head movement
- [ ] User can click through cursor
- [ ] All settings properly connected

### Architecture (Long-term)
- [ ] Each class has single responsibility
- [ ] IMUManager only provides data
- [ ] No circular dependencies
- [ ] Clean module boundaries

## Recommended Approach

### Option A: Quick Fixes First (1 hour)
1. Fix FLAG_NOT_TOUCHABLE (5 mins)
2. Fix service state checking (15 mins)
3. Verify IMU data flow (20 mins)
4. Test all fixes (20 mins)

**Pros:** Immediate user relief, low risk
**Cons:** Technical debt remains

### Option B: Full Refactor (3 hours)
1. Implement complete architecture change
2. Full testing suite
3. Documentation update

**Pros:** Clean architecture, future-proof
**Cons:** Higher risk, longer timeline

### Option C: Hybrid Approach (Recommended)
1. **Now:** Quick fixes (Option A) - 1 hour
2. **Next Sprint:** Architecture refactor - planned and tested
3. **Future:** Remove deprecated components

**Pros:** User gets immediate fixes, architecture debt addressed systematically
**Cons:** Requires two phases

## Next Steps

1. **Review this plan** and provide feedback
2. **Choose approach** (A, B, or C)
3. **Approve specific changes** before implementation
4. **Begin implementation** based on your decision

---

**Note:** All line numbers and file paths are based on current codebase analysis as of 2025-01-26.