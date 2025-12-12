# Chapter 8: Glossary & Reference

**Document:** VoiceOS-AvaLearnLite-UserManual-Ch08
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 8.1 Glossary

### A

**Accessibility Service**
An Android system service that allows apps to observe and interact with the user interface of other apps. VoiceOS uses this to learn and control apps.

**Action**
An operation that can be performed on an element, such as click, long click, edit text, or scroll.

**AIDL (Android Interface Definition Language)**
The inter-process communication mechanism used for AvaLearnLite to communicate with VoiceOS Core.

**AVU (Avanues Universal)**
The data format used by VoiceOS for storing and exchanging learned app data.

### B

**Bounds**
The rectangular coordinates (left, top, right, bottom) that define an element's position on screen.

### C

**Command**
A voice instruction that triggers an action on an element. Generated from discovered elements.

**CommandManager**
The VoiceOS component that manages imported commands and watches for new AVU files.

**Confidence Score**
A value between 0 and 1 indicating how reliable a generated command is.

**Coverage**
The percentage of discovered elements that have been tested/clicked during exploration.

### D

**DNC (DoNotClick)**
The safety system that prevents interaction with dangerous UI elements.

**Dynamic Region**
An area of the screen where content changes frequently (timers, counters, live feeds).

### E

**Element**
Any interactive component in an app's user interface (button, text field, checkbox, etc.).

**Exploration**
The process of actively navigating through an app to discover and learn its interface.

### H

**Hash (Screen Hash)**
A unique fingerprint that identifies a specific screen, used to track visited screens.

### J

**JIT (Just-In-Time) Learning**
The background learning system that passively observes apps you use without special action.

### L

**Login Detection**
The safety feature that identifies authentication screens and warns/pauses exploration.

**Loop Prevention**
The system that detects when exploration is visiting the same screens repeatedly.

### M

**Menu Discovery**
The process of identifying and capturing menu structures (dropdowns, overflow menus, etc.).

### N

**Node**
A single element in the accessibility tree hierarchy.

### O

**Ocean Blue Theme**
The visual design language used by AvaLearnLite, with blue as the primary color.

### P

**Package Name**
The unique identifier for an Android app (e.g., com.example.app).

**Parcelable**
Android's mechanism for passing complex data between processes.

### S

**Screen**
A unique view or page within an app, identified by its hash.

**Synonym**
An alternative phrase that maps to the same command (e.g., "log in" = "sign in").

### V

**VoiceOS**
The main accessibility system that enables voice control of Android devices.

**.vos File**
The file extension for VoiceOS command files exported by AvaLearnLite.

---

## 8.2 Abbreviations

| Abbreviation | Full Form |
|--------------|-----------|
| AVU | Avanues Universal |
| DNC | DoNotClick |
| JIT | Just-In-Time |
| IPC | Inter-Process Communication |
| UI | User Interface |
| API | Application Programming Interface |
| AIDL | Android Interface Definition Language |
| CMD | Command |
| ELM | Element |
| SCR | Screen |
| STA | Statistics |
| NAV | Navigation |
| APP | Application |

---

## 8.3 Quick Reference Cards

### 8.3.1 JIT Controls Quick Reference

| Button | Action | When to Use |
|--------|--------|-------------|
| **Pause** | Stops JIT capture | Before manual exploration |
| **Resume** | Starts JIT capture | After exploration or pause |
| **Refresh** | Updates statistics | To see latest counts |

### 8.3.2 Exploration Phases Quick Reference

| Phase | Color | Meaning | Action |
|-------|-------|---------|--------|
| IDLE | Gray | Ready | Start exploration |
| EXPLORING | Blue | Active | Use target app |
| WAITING_USER | Orange | Paused | Complete action |
| COMPLETED | Green | Done | Export data |
| ERROR | Red | Failed | Check troubleshooting |

### 8.3.3 Status Colors Quick Reference

| Color | Hex | Meaning |
|-------|-----|---------|
| Ocean Blue | #3B82F6 | Primary, active exploration |
| Green | #10B981 | Active, success, completed |
| Orange | #F59E0B | Warning, paused, waiting |
| Red | #EF4444 | Error, failed |
| Gray | #6B7280 | Idle, disabled |
| Cyan | #06B6D4 | Secondary accent |

### 8.3.4 AVU Record Types Quick Reference

| Prefix | Name | Fields |
|--------|------|--------|
| APP | Application | packageName:appName:timestamp |
| STA | Statistics | screens:elements:commands:avgTime:loops:coverage |
| SCR | Screen | hash:activity:timestamp:elementCount |
| ELM | Element | uuid:name:type:actions:bounds:state |
| NAV | Navigation | fromScreen:toScreen:elementId:timestamp |
| CMD | Command | id:phrase:action:targetUuid:confidence |

### 8.3.5 Element Actions Quick Reference

| Code | Action | Description |
|------|--------|-------------|
| C | Click | Tap the element |
| L | Long Click | Press and hold |
| E | Edit | Enter text |
| S | Scroll | Swipe in direction |

### 8.3.6 DNC Categories Quick Reference

| Category | Keywords |
|----------|----------|
| Destructive | delete, remove, erase, clear, wipe |
| Financial | pay, purchase, buy, subscribe |
| Account | logout, sign out, deactivate |
| System | uninstall, factory reset |

---

## 8.4 Keyboard Shortcuts

AvaLearnLite is primarily touch-based, but external keyboards are supported:

| Key | Action |
|-----|--------|
| Space | Start/Stop Exploration |
| R | Refresh Statistics |
| E | Export (when available) |
| Esc | Cancel current operation |

---

## 8.5 File Paths Reference

### 8.5.1 App Storage

| Path | Purpose |
|------|---------|
| `/data/data/com.augmentalis.avalearnlite/` | App internal storage |
| `/Android/data/com.augmentalis.avalearnlite/files/` | App external storage |
| `/Android/data/com.augmentalis.avalearnlite/files/learned_apps/` | Export location |

### 8.5.2 VoiceOS Paths

| Path | Purpose |
|------|---------|
| `/data/data/com.augmentalis.voiceoscore/databases/` | VoiceOS database |
| `/Android/data/com.augmentalis.voiceoscore/files/` | VoiceOS files |

---

## 8.6 Version Compatibility Matrix

### 8.6.1 AvaLearnLite + VoiceOS

| AvaLearnLite | VoiceOS Core | Compatible |
|--------------|--------------|------------|
| 2.0.x | 2.0.x | Yes |
| 2.0.x | 1.x | No |
| 1.x | 2.0.x | No |
| 1.x | 1.x | Yes |

### 8.6.2 Android Compatibility

| Android Version | API Level | Support |
|-----------------|-----------|---------|
| Android 14 | API 34 | Full |
| Android 15 | API 35 | Full (Recommended) |
| Android 13 | API 33 | Not supported |

---

## 8.7 Technical Specifications

### 8.7.1 App Specifications

| Specification | Value |
|---------------|-------|
| Package Name | com.augmentalis.avalearnlite |
| Minimum SDK | 34 (Android 14) |
| Target SDK | 35 (Android 15) |
| Build Type | Release |
| Theme | Ocean Blue Light |

### 8.7.2 Resource Usage

| Resource | Typical Usage |
|----------|---------------|
| RAM | 50-100 MB |
| Storage (App) | ~20 MB |
| Storage (Data) | Varies by exports |
| CPU | Low (idle), Medium (exploring) |
| Battery | Moderate during exploration |

### 8.7.3 AIDL Interface

| Interface | Method Count |
|-----------|--------------|
| IElementCaptureService | 15+ methods |
| IAccessibilityEventListener | 7 callbacks |

---

## 8.8 Related Documentation

### 8.8.1 User Documentation

| Document | Description |
|----------|-------------|
| AvaLearnLite User Manual | This document |
| VoiceOS User Guide | Main VoiceOS documentation |
| Quick Start Guide | Getting started in 5 minutes |

### 8.8.2 Developer Documentation

| Document | Description |
|----------|-------------|
| AvaLearnPro Developer Manual | Developer edition documentation |
| VoiceOS Architecture Guide | System architecture |
| AVU Format Specification | Export format details |
| AIDL Interface Reference | Service communication |

---

## 8.9 Document Information

### 8.9.1 Manual Chapters

| Chapter | Title |
|---------|-------|
| 01 | Introduction & Overview |
| 02 | Installation & Setup |
| 03 | User Interface Guide |
| 04 | Features & Functions |
| 05 | Safety Systems |
| 06 | Export & Data Management |
| 07 | Troubleshooting & FAQ |
| 08 | Glossary & Reference |

### 8.9.2 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-11 | Initial release |

### 8.9.3 Document Metadata

| Property | Value |
|----------|-------|
| Document Set | VoiceOS-AvaLearnLite-UserManual |
| Total Chapters | 8 |
| Last Updated | 2025-12-11 |
| Author | Augmentalis |
| Copyright | 2025 Augmentalis |

---

## 8.10 Contact Information

| Type | Contact |
|------|---------|
| Support Email | support@augmentalis.com |
| Feature Requests | features@augmentalis.com |
| Bug Reports | bugs@augmentalis.com |
| Community Forum | forums.augmentalis.com |
| GitHub | github.com/augmentalis |

---

**End of Chapter 8**

---

**End of AvaLearnLite User Manual**

**Copyright 2025 Augmentalis. All rights reserved.**
