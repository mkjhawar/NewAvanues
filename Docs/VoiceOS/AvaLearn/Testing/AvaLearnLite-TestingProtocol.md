# AvaLearnLite Testing Protocol

**Document:** VoiceOS-AvaLearnLite-TestingProtocol
**Version:** 1.0
**Last Updated:** 2025-12-11
**Product:** AvaLearnLite (User Edition)

---

## 1. Testing Overview

### 1.1 Scope

This protocol covers testing for AvaLearnLite, the user-facing edition of the app learning tool.

### 1.2 Test Categories

| Category | Coverage | Priority |
|----------|----------|----------|
| Functional | UI, Features, Integration | High |
| Safety | DNC, Login, Loop | Critical |
| Export | AVU generation, File I/O | High |
| Performance | Memory, Battery, Speed | Medium |
| Accessibility | Screen reader, Touch targets | High |
| Compatibility | Android versions, Devices | Medium |

---

## 2. Environment Setup

### 2.1 Required Devices

| Device Type | Specification | Purpose |
|-------------|---------------|---------|
| Primary | Android 14+, 6GB RAM | Main testing |
| Secondary | Android 14, 4GB RAM | Minimum spec |
| Tablet | Android 14+, 10" | Layout testing |

### 2.2 Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| VoiceOS Core | 2.0.0+ | Service provider |
| AvaLearnLite | 2.0.0 | Test subject |
| ADB | Latest | Debugging |
| Sample Apps | Various | Test targets |

### 2.3 Test Accounts

- Non-privileged Android user account
- No special developer options enabled
- Standard permissions workflow

---

## 3. Functional Test Cases

### 3.1 Installation & Setup

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| F-001 | Fresh Install | Install APK on clean device | App installs successfully | |
| F-002 | Launch App | Open AvaLearnLite from drawer | App launches with Ocean Blue theme | |
| F-003 | Service Connection | Launch with VoiceOS running | JIT Status shows "Active" (green) | |
| F-004 | No Service | Launch without VoiceOS | Shows "Service not bound" | |
| F-005 | Permission Request | First launch | Storage permission requested | |

### 3.2 JIT Learning Status

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| F-010 | Status Display | View JIT card | Shows Active/Paused badge | |
| F-011 | Screens Count | After exploring | Count updates correctly | |
| F-012 | Elements Count | After exploring | Count updates correctly | |
| F-013 | Package Display | Focus different app | Current package updates | |
| F-014 | Pause Button | Press Pause | Status changes to Paused (orange) | |
| F-015 | Resume Button | Press Resume | Status changes to Active (green) | |
| F-016 | Refresh Button | Press Refresh | Stats update from service | |

### 3.3 App Exploration

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| F-020 | Start Exploration | Press Start | Phase changes to EXPLORING (blue) | |
| F-021 | Stop Exploration | Press Stop | Phase changes to IDLE | |
| F-022 | Screen Discovery | Navigate target app | Screens count increases | |
| F-023 | Element Discovery | View target screens | Elements count increases | |
| F-024 | Coverage Calculation | Click elements | Coverage percentage updates | |
| F-025 | Phase Indicator | During exploration | Badge shows correct phase | |
| F-026 | Statistics Grid | View exploration card | All 4 stats display correctly | |

### 3.4 Export Functionality

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| F-030 | Export Enabled | After exploration | Export button active | |
| F-031 | Export Disabled | No exploration | Export button grayed out | |
| F-032 | Export Success | Press Export | Success message, filename shown | |
| F-033 | Export Location | Check filesystem | File in learned_apps folder | |
| F-034 | File Format | Inspect .vos file | Encrypted content | |
| F-035 | Re-export | Export same app twice | Previous file overwritten | |

---

## 4. Safety Test Cases

### 4.1 DoNotClick Protection

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| S-001 | Delete Button | Explore app with Delete button | DNC count increases | |
| S-002 | Pay Button | Explore app with Pay button | DNC count increases | |
| S-003 | Logout Button | Explore app with Logout | DNC count increases | |
| S-004 | Uninstall Button | Explore Settings app | DNC count increases | |
| S-005 | Normal Button | Explore app with normal buttons | Button not skipped | |
| S-006 | DNC Counter | After skipping elements | Counter displays correctly | |

### 4.2 Login Detection

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| S-010 | Password Screen | Navigate to login screen | Warning banner appears | |
| S-011 | PIN Screen | Navigate to PIN entry | Warning banner appears | |
| S-012 | Biometric Prompt | Trigger fingerprint dialog | Warning shows BIOMETRIC | |
| S-013 | Non-Login Screen | Navigate to normal screen | No warning | |
| S-014 | Warning Display | On login detection | Orange banner with type | |

### 4.3 Dynamic Region Detection

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| S-020 | Clock Display | Explore screen with clock | Dynamic region detected | |
| S-021 | Counter Badge | Explore screen with badge | Dynamic region detected | |
| S-022 | Static Content | Explore static screen | No dynamic detection | |
| S-023 | Counter Display | After detection | Counter updates correctly | |

### 4.4 Loop Prevention

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| S-030 | Multiple Visits | Visit same screen 5+ times | Loop detected | |
| S-031 | Normal Navigation | Visit screens once | No loop warning | |
| S-032 | Rapid Navigation | Navigate very quickly | Cooldown triggered | |

---

## 5. UI Test Cases

### 5.1 Theme & Styling

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| U-001 | Ocean Blue Theme | Launch app | Blue primary colors | |
| U-002 | Light Background | View all screens | Light blue background | |
| U-003 | Card Styling | View all cards | Consistent card design | |
| U-004 | Status Colors | Observe status badges | Correct color per state | |
| U-005 | Button Colors | View all buttons | Theme-consistent colors | |

### 5.2 Layout & Responsiveness

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| U-010 | Portrait Layout | Standard portrait | All cards visible, scrollable | |
| U-011 | Landscape Layout | Rotate device | Layout adjusts correctly | |
| U-012 | Scroll Behavior | Scroll content | Smooth scrolling | |
| U-013 | Card Order | View main screen | JIT, Exploration, Safety, Export | |
| U-014 | Touch Targets | Tap buttons | 48dp minimum | |

### 5.3 Accessibility

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| U-020 | TalkBack Support | Enable TalkBack | All elements readable | |
| U-021 | Content Descriptions | Inspect elements | All buttons labeled | |
| U-022 | Color Contrast | Check WCAG | 7:1 minimum ratio | |
| U-023 | Font Scaling | Increase system font | Text scales correctly | |

---

## 6. Integration Test Cases

### 6.1 VoiceOS Integration

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| I-001 | Service Binding | Launch AvaLearnLite | Binds to VoiceOS service | |
| I-002 | Service Disconnect | Kill VoiceOS | Status updates to not bound | |
| I-003 | Service Reconnect | Restart VoiceOS | Auto-reconnects | |
| I-004 | Event Reception | Explore app | Events received from JIT | |
| I-005 | State Query | Press Refresh | Gets state from service | |

### 6.2 File System Integration

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| I-010 | Export Directory | Export file | Creates learned_apps folder | |
| I-011 | File Writing | Export file | File created successfully | |
| I-012 | File Overwrite | Export same app | Previous file replaced | |
| I-013 | Storage Full | Fill storage, export | Graceful error message | |

### 6.3 Command Import (VoiceOS)

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| I-020 | Auto Import | Export, wait 30s | VoiceOS imports commands | |
| I-021 | Command Availability | Say voice command | Command recognized | |
| I-022 | Synonym Support | Use synonym phrase | Command recognized | |

---

## 7. Performance Test Cases

### 7.1 Memory Usage

| ID | Test Case | Metric | Target | Pass/Fail |
|----|-----------|--------|--------|-----------|
| P-001 | Idle Memory | RAM usage | < 50MB | |
| P-002 | Active Memory | During exploration | < 100MB | |
| P-003 | Memory Leak | 1 hour session | No growth | |

### 7.2 Battery Impact

| ID | Test Case | Metric | Target | Pass/Fail |
|----|-----------|--------|--------|-----------|
| P-010 | Idle Drain | 1 hour idle | < 1% | |
| P-011 | Active Drain | 30 min exploration | < 5% | |

### 7.3 Responsiveness

| ID | Test Case | Metric | Target | Pass/Fail |
|----|-----------|--------|--------|-----------|
| P-020 | Launch Time | App open | < 2 seconds | |
| P-021 | UI Response | Button tap | < 100ms | |
| P-022 | Export Time | Medium app | < 5 seconds | |

---

## 8. Compatibility Test Cases

### 8.1 Android Versions

| ID | Test Case | Version | Expected Result | Pass/Fail |
|----|-----------|---------|-----------------|-----------|
| C-001 | Android 14 | API 34 | Full functionality | |
| C-002 | Android 15 | API 35 | Full functionality | |
| C-003 | Android 13 | API 33 | Install fails gracefully | |

### 8.2 Device Types

| ID | Test Case | Device | Expected Result | Pass/Fail |
|----|-----------|--------|-----------------|-----------|
| C-010 | Phone Small | 5.5" | Layout OK | |
| C-011 | Phone Large | 6.7" | Layout OK | |
| C-012 | Tablet | 10" | Layout OK | |

---

## 9. Error Handling Test Cases

| ID | Test Case | Steps | Expected Result | Pass/Fail |
|----|-----------|-------|-----------------|-----------|
| E-001 | No Permission | Deny storage | Helpful error message | |
| E-002 | Service Crash | Kill VoiceOS process | Graceful disconnect | |
| E-003 | Low Storage | < 10MB free | Export fails gracefully | |
| E-004 | Target App Crash | App crashes during explore | Exploration pauses | |

---

## 10. Test Execution Checklist

### Pre-Test
- [ ] Device charged > 80%
- [ ] VoiceOS installed and enabled
- [ ] AvaLearnLite installed
- [ ] Storage permission granted
- [ ] Test apps installed

### Post-Test
- [ ] All test results recorded
- [ ] Screenshots captured for failures
- [ ] Logs exported
- [ ] Device cleaned up

---

## 11. Sign-Off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Tester | | | |
| QA Lead | | | |
| Dev Lead | | | |

---

**End of Testing Protocol**
