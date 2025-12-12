# AvaLearnPro Testing Protocol

**Document:** VoiceOS-AvaLearnPro-TestingProtocol
**Version:** 1.0
**Last Updated:** 2025-12-11
**Product:** AvaLearnPro (Developer Edition)

---

## 1. Testing Overview

### 1.1 Scope

This protocol covers testing for AvaLearnPro, the developer edition with full debugging capabilities.

### 1.2 Test Categories

| Category | Coverage | Priority |
|----------|----------|----------|
| Functional | All user features + dev features | High |
| Developer Tools | Logs, Inspector, Events | Critical |
| AIDL Integration | Service communication | Critical |
| Safety | All safety systems | High |
| Export | Plain text AVU | High |
| Performance | Memory, CPU, Event processing | Medium |
| Theme | Dark mode, Cyan accent | Medium |

---

## 2. Environment Setup

### 2.1 Required Devices

| Device Type | Specification | Purpose |
|-------------|---------------|---------|
| Development | Android 14+, 8GB RAM | Primary testing |
| Debug | Android 14+, USB debugging | ADB testing |
| Secondary | Android 14, 4GB RAM | Minimum spec |

### 2.2 Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| VoiceOS Core | 2.0.0+ (Debug) | Service provider |
| AvaLearnPro | 2.0.0 | Test subject |
| ADB | Latest | Debugging |
| Android Studio | Latest | Logcat |
| Sample Apps | Various | Test targets |

### 2.3 Debug Configuration

- Developer options enabled
- USB debugging enabled
- Stay awake while charging
- Show layout bounds (optional)

---

## 3. Developer Edition Feature Tests

### 3.1 Theme & Visual Identification

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| D-001 | Dark Theme | Launch app | Dark blue background (#0C1929) | |
| D-002 | DEV Badge | View title bar | Cyan "DEV" badge visible | |
| D-003 | Cyan Accent | View UI elements | Cyan highlights (#22D3EE) | |
| D-004 | Tab Interface | View main screen | Three tabs: Status, Logs, Elements | |
| D-005 | Console Colors | View Logs tab | Monospace, dark console (#0D0D0D) | |

### 3.2 Tabs Navigation

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| D-010 | Status Tab | Tap Status | Shows status cards | |
| D-011 | Logs Tab | Tap Logs | Shows log console | |
| D-012 | Elements Tab | Tap Elements | Shows element list | |
| D-013 | Tab Persistence | Switch tabs | State preserved | |
| D-014 | Tab Indicator | Select tab | Indicator highlights correctly | |

---

## 4. Log Console Tests

### 4.1 Log Display

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| L-001 | Log Entry Format | View log | [HH:mm:ss.SSS] LEVEL TAG: message | |
| L-002 | DEBUG Color | Add DEBUG log | Gray (#9E9E9E) | |
| L-003 | INFO Color | Add INFO log | Blue (#60A5FA) | |
| L-004 | WARN Color | Add WARN log | Yellow (#FBBF24) | |
| L-005 | ERROR Color | Add ERROR log | Red (#F87171) | |
| L-006 | EVENT Color | Receive event | Purple (#A78BFA) | |

### 4.2 Log Management

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| L-010 | Entry Count | View header | Shows entry count | |
| L-011 | Clear Button | Press Clear | All logs removed | |
| L-012 | Max Entries | Add 600 logs | Limited to 500 | |
| L-013 | Auto-scroll | New log added | Scrolls to show new | |
| L-014 | Monospace Font | View log text | Fixed-width font | |

### 4.3 Event Logging

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| L-020 | Screen Event | Navigate target app | SCREEN log appears | |
| L-021 | Action Event | Click element | ACTION log appears | |
| L-022 | Scroll Event | Scroll in app | SCROLL log appears | |
| L-023 | Login Event | Navigate to login | LOGIN log (WARN) | |
| L-024 | Error Event | Cause error | ERROR log appears | |

---

## 5. Element Inspector Tests

### 5.1 Element Display

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| E-001 | Element Count | View header | Shows element count | |
| E-002 | Query Button | Press Query | Elements populate | |
| E-003 | Element Card | View element | Shows name, type, ID, actions, bounds | |
| E-004 | Display Name | View element | contentDescription or text | |
| E-005 | Class Name | View element | Short class name (e.g., "Button") | |

### 5.2 Element Properties

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| E-010 | Resource ID | Query element with ID | ID displayed in monospace | |
| E-011 | Action Chips | View clickable element | [click] chip shown | |
| E-012 | Long Click | View long-clickable | [long] chip shown | |
| E-013 | Editable | View text field | [edit] chip shown | |
| E-014 | Scrollable | View scrollable | [scroll] chip shown | |
| E-015 | Bounds Display | View element | [left,top,right,bottom] | |

### 5.3 Element Tree

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| E-020 | Root Node | Query screen | Root element present | |
| E-021 | Child Elements | Query screen | Children listed | |
| E-022 | Deep Tree | Complex screen | All levels captured | |
| E-023 | Refresh | Query after navigation | New elements shown | |

---

## 6. AIDL Integration Tests

### 6.1 Service Binding

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| A-001 | Initial Bind | Launch app | Service binds successfully | |
| A-002 | Bind Logging | Observe logs | "Service connected" log | |
| A-003 | Connection State | Check Status tab | Shows bound state | |
| A-004 | Unbind on Destroy | Close app | Clean unbind (no leak) | |

### 6.2 Event Listener

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| A-010 | Listener Registration | On bind | "Event listener registered" log | |
| A-011 | Event Delivery | Navigate target | Events appear in logs | |
| A-012 | Event Timing | Cause event | < 100ms latency | |
| A-013 | Unregister | Close app | Listener unregistered | |

### 6.3 Service Methods

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| A-020 | queryState() | Press Refresh | State returned | |
| A-021 | pauseCapture() | Press Pause | JIT pauses | |
| A-022 | resumeCapture() | Press Resume | JIT resumes | |
| A-023 | getCurrentScreenInfo() | Press Query | Elements returned | |
| A-024 | isLoginScreen() | Navigate to login | Returns true | |
| A-025 | getDynamicRegionCount() | After detection | Returns count | |

### 6.4 Error Handling

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| A-030 | RemoteException | Kill VoiceOS | Caught, logged | |
| A-031 | Null Service | Call before bind | No crash | |
| A-032 | Service Restart | Restart VoiceOS | Reconnects | |

---

## 7. Safety System Tests

### 7.1 DoNotClick

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| S-001 | Block Detection | Explore with delete | Log shows blocked element | |
| S-002 | Counter Update | After blocking | DNC counter increases | |
| S-003 | Reason Logged | View log | Shows DESTRUCTIVE/FINANCIAL/etc | |
| S-004 | Keyword Match | Various keywords | All categories detected | |

### 7.2 Login Detection

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| S-010 | Password Detection | Navigate to login | LOGIN event logged | |
| S-011 | Type Identification | Different login types | Correct type in log | |
| S-012 | Score Logging | Detection occurs | Confidence score logged | |
| S-013 | Signal Details | Check log | Detection signals listed | |

### 7.3 Loop Prevention

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| S-020 | Visit Tracking | Navigate screens | Visit counts in logs | |
| S-021 | Loop Warning | Visit same 3x | Warning log appears | |
| S-022 | Critical Loop | Visit same 5x | Critical log appears | |
| S-023 | Rapid Detection | Navigate quickly | Rapid loop detected | |

---

## 8. Export Tests

### 8.1 Plain Text Export

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| X-001 | Export Success | Press Export | File created | |
| X-002 | Plain Text Format | Read .vos file | Human-readable content | |
| X-003 | Not Encrypted | Inspect file | No Base64 encoding | |
| X-004 | AVU Structure | Read file | Correct record format | |

### 8.2 AVU Content

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| X-010 | Header | Read file | Contains version comment | |
| X-011 | Metadata | Read file | schema, version, locale | |
| X-012 | APP Record | Read file | APP:pkg:name:timestamp | |
| X-013 | STA Record | Read file | Statistics present | |
| X-014 | SCR Records | Read file | Screen records present | |
| X-015 | ELM Records | Read file | Element records present | |
| X-016 | CMD Records | Read file | Command records present | |
| X-017 | Synonyms | Read file | Synonyms section present | |

### 8.3 Export Logging

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| X-020 | Export Start | Press Export | Log shows export starting | |
| X-021 | Export Progress | During export | Progress logged | |
| X-022 | Export Complete | After export | Success path logged | |
| X-023 | Export Error | Cause failure | Error logged with reason | |

---

## 9. Performance Tests

### 9.1 Memory

| ID | Test Case | Metric | Target | Pass/Fail |
|----|-----------|--------|--------|-----------|
| P-001 | Idle Memory | RAM usage | < 60MB | |
| P-002 | Active Memory | During exploration | < 120MB | |
| P-003 | Log Buffer | At 500 entries | < 5MB | |
| P-004 | Element Cache | After query | < 10MB | |

### 9.2 Event Processing

| ID | Test Case | Metric | Target | Pass/Fail |
|----|-----------|--------|--------|-----------|
| P-010 | Event Latency | Event to log | < 50ms | |
| P-011 | High Volume | 100 events/sec | No drops | |
| P-012 | Log Rendering | 500 entries | < 100ms | |
| P-013 | Element Query | Full tree | < 500ms | |

### 9.3 UI Responsiveness

| ID | Test Case | Metric | Target | Pass/Fail |
|----|-----------|--------|--------|-----------|
| P-020 | Tab Switch | Tap to render | < 200ms | |
| P-021 | Log Scroll | Scroll logs | 60 FPS | |
| P-022 | Element Scroll | Scroll elements | 60 FPS | |

---

## 10. Logcat Integration Tests

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| C-001 | Tag Filtering | Filter LearnAppDevActivity | Logs appear | |
| C-002 | Level Filtering | Filter ERROR only | Only errors | |
| C-003 | Combined Filter | Multiple tags | All relevant logs | |
| C-004 | Log Correlation | Compare in-app and Logcat | Match | |

---

## 11. Build Flag Tests

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| B-001 | IS_DEVELOPER_EDITION | Check BuildConfig | true | |
| B-002 | ENABLE_NEO4J | Check BuildConfig | true | |
| B-003 | ENABLE_LOGGING | Check BuildConfig | true | |
| B-004 | PLAIN_TEXT_EXPORT | Export file | Not encrypted | |

---

## 12. Regression Tests

### 12.1 User Edition Features

All tests from AvaLearnLite-TestingProtocol must pass in AvaLearnPro:

| ID | Reference | Pass/Fail |
|----|-----------|-----------|
| R-001 | F-001 to F-035 | |
| R-002 | S-001 to S-032 | |
| R-003 | I-001 to I-022 | |

---

## 13. Test Execution Checklist

### Pre-Test
- [ ] Device in developer mode
- [ ] USB debugging enabled
- [ ] VoiceOS debug build installed
- [ ] AvaLearnPro installed
- [ ] Android Studio connected
- [ ] Logcat configured

### During Test
- [ ] Logcat recording
- [ ] Screenshots on failures
- [ ] Memory profiler (for P tests)

### Post-Test
- [ ] All results recorded
- [ ] Logs exported
- [ ] Memory dumps saved (if issues)
- [ ] Bug reports filed

---

## 14. Sign-Off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | | | |
| QA Lead | | | |
| Tech Lead | | | |

---

**End of Testing Protocol**
