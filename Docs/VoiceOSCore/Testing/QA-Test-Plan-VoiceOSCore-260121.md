# VoiceOSCore & VoiceOSCoreNG - QA Test Plan

**Date:** 2026-01-21
**Version:** 1.0
**Author:** Claude (AI Assistant)
**Target App:** `android/apps/voiceoscoreng/`
**Module:** `Modules/VoiceOSCore/`

---

## Table of Contents

1. [Overview](#1-overview)
2. [Test Environment Setup](#2-test-environment-setup)
3. [Database Population Tests](#3-database-population-tests)
4. [Command Generation Tests](#4-command-generation-tests)
5. [Hash Assignment Tests](#5-hash-assignment-tests)
6. [Deduplication Tests](#6-deduplication-tests)
7. [Numbered Command Tests](#7-numbered-command-tests)
8. [Operating Mode Tests (Off/On/Auto)](#8-operating-mode-tests-offonauto)
9. [App-Specific Tests](#9-app-specific-tests)
10. [Dynamic Lists Tests](#10-dynamic-lists-tests)
11. [Memory & Cleanup Tests](#11-memory--cleanup-tests)
12. [Performance Benchmarks](#12-performance-benchmarks)
13. [Edge Cases & Error Handling](#13-edge-cases--error-handling)
14. [Appendix: Database Schema](#appendix-database-schema)

---

## 1. Overview

### 1.1 Purpose

This document provides comprehensive test scenarios for QA testers to validate:
- Database population and persistence
- Voice command generation and execution
- Hash-based screen recognition and caching
- Element deduplication
- Numbered overlay badges for voice commands
- Dynamic list lifecycle management
- Memory management and cleanup

### 1.2 Architecture Summary

```
┌─────────────────────────────────────────────────────────────┐
│                    VoiceOSCoreNG App                        │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │ Accessibility   │  │ Overlay Service │                   │
│  │ Service         │  │ (Numbered       │                   │
│  └────────┬────────┘  │  Badges)        │                   │
│           │           └────────┬────────┘                   │
│           ▼                    │                            │
│  ┌─────────────────────────────▼────────────────────────┐   │
│  │            DynamicCommandGenerator                    │   │
│  │  - ElementExtractor (scrape UI)                       │   │
│  │  - OverlayItemGenerator (numbered badges)             │   │
│  │  - CommandPersistenceManager (save to DB)             │   │
│  └────────────────────────┬─────────────────────────────┘   │
└───────────────────────────┼─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                    VoiceOSCore Module (KMP)                 │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐   │
│  │ CommandGenerator│  │CommandOrchest- │  │ CommandReg-  │   │
│  │ (create cmds)  │  │rator (manage)  │  │ istry (lookup│   │
│  └────────────────┘  └────────────────┘  └──────────────┘   │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐   │
│  │ HashUtils      │  │ScreenCache-    │  │ ActionCoor-  │   │
│  │ (SHA-256)      │  │ Manager        │  │ dinator      │   │
│  └────────────────┘  └────────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│               VoiceOS Database (SQLDelight)                 │
│  ┌─────────────────┐  ┌─────────────────┐                   │
│  │ voice_commands  │  │ scraped_apps    │                   │
│  │ scraped_elements│  │ screen_contexts │                   │
│  └─────────────────┘  └─────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 Key Files for Reference

| Component | File Path |
|-----------|-----------|
| Main Service | `android/apps/voiceoscoreng/.../VoiceOSAccessibilityService.kt` |
| Command Generator | `Modules/VoiceOSCore/src/commonMain/.../CommandGenerator.kt` |
| Command Orchestrator | `Modules/VoiceOSCore/src/commonMain/.../CommandOrchestrator.kt` |
| Screen Cache | `Modules/VoiceOSCore/src/androidMain/.../ScreenCacheManager.kt` |
| Element Extractor | `android/apps/voiceoscoreng/.../ElementExtractor.kt` |
| Overlay Generator | `android/apps/voiceoscoreng/.../OverlayItemGenerator.kt` |
| Hash Utils | `Modules/VoiceOSCore/src/commonMain/.../HashUtils.kt` |

---

## 2. Test Environment Setup

### 2.1 Prerequisites

- Android device/emulator (API 29+, Android 10+)
- VoiceOSCoreNG app installed
- Accessibility service enabled
- Overlay permission granted
- Test apps installed: Gmail, Teams, Calculator, Settings, Contacts

### 2.2 Enabling Developer Mode

1. Open VoiceOSCoreNG app
2. Navigate to Settings > Developer Settings
3. Enable "Debug Overlay"
4. Enable "Element Inspector"
5. Enable "Performance Profiler"

### 2.3 Database Inspection Tools

The app provides debug screens to inspect:
- Screen cache entries
- Command registry
- Element deduplication stats
- Performance metrics

Access via: Settings > Developer Settings > Database Inspector

---

## 3. Database Population Tests

### 3.1 Test: First Launch - Database Initialization

**Objective:** Verify database is created and schema is correct on first app launch.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Clear app data (Settings > Apps > VoiceOSCoreNG > Clear Data) | App data cleared |
| 2 | Launch VoiceOSCoreNG | App opens, accessibility setup prompt shown |
| 3 | Enable accessibility service | Service starts |
| 4 | Open Developer Settings > Database Inspector | Database tables shown |
| 5 | Verify tables exist | Tables: `voice_commands`, `scraped_apps`, `scraped_elements`, `screen_contexts`, `generated_commands` |
| 6 | Check row counts | Initially 0 rows in all tables |

### 3.2 Test: App Scraping Populates Database

**Objective:** Verify opening an app populates the database with scraped elements.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator app | Calculator UI visible |
| 2 | Wait 2 seconds | Screen scraping completes |
| 3 | Open VoiceOSCoreNG > Database Inspector | Database stats shown |
| 4 | Check `scraped_apps` table | 1 row: `com.google.android.calculator` |
| 5 | Check `scraped_elements` table | 15-25 rows (buttons, display) |
| 6 | Check `voice_commands` table | Commands for each button: "0", "1", "2"..., "plus", "minus", etc. |
| 7 | Verify app version stored | `versionCode` matches Calculator app |

### 3.3 Test: Screen Context Persistence

**Objective:** Verify screen context (activity, hash) is persisted.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Settings app | Settings main screen |
| 2 | Wait for scrape | Scrape completes |
| 3 | Navigate to Settings > Display | Display settings screen |
| 4 | Wait for scrape | New screen scraped |
| 5 | Check `screen_contexts` table | 2 entries for Settings app |
| 6 | Verify screen hashes are different | Hash1 != Hash2 |
| 7 | Return to main Settings | Same hash as step 2 |

### 3.4 Test: Command Persistence Across Sessions

**Objective:** Verify commands persist after app restart.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Gmail app | Gmail inbox visible |
| 2 | Wait for scrape | Commands generated |
| 3 | Note command count in Database Inspector | e.g., 45 commands |
| 4 | Force stop VoiceOSCoreNG | Service stops |
| 5 | Restart VoiceOSCoreNG | Service restarts |
| 6 | Check Database Inspector | Same command count (45) |
| 7 | Open Gmail again | Commands load from cache (fast) |

---

## 4. Command Generation Tests

### 4.1 Test: Label-Based Commands

**Objective:** Verify commands are generated from element labels.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator | Calculator visible |
| 2 | Check command registry (Developer Settings) | Commands include: "zero", "one", "two"..."nine", "plus", "minus", "equals", "clear" |
| 3 | Say "seven" | Button "7" highlights/taps |
| 4 | Say "plus" | Plus operator activates |
| 5 | Say "three" | Button "3" highlights/taps |
| 6 | Say "equals" | Result "10" shown |

### 4.2 Test: Content Description Commands

**Objective:** Verify commands use accessibility content descriptions.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Settings app | Settings visible |
| 2 | Check commands for icons without visible text | Commands like "search settings", "profile picture" |
| 3 | Say "search settings" | Search field activates |

### 4.3 Test: Index Commands for Lists

**Objective:** Verify ordinal commands work for list items.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Settings app (list of items) | Settings categories visible |
| 2 | Check command registry | Commands include: "first", "second", "third"... |
| 3 | Say "first" | First settings item selected |
| 4 | Navigate back, say "third" | Third settings item selected |

### 4.4 Test: Numeric Commands (Overlay Badges)

**Objective:** Verify numbered badges and numeric voice commands.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open any app with multiple clickable items | App visible |
| 2 | Verify overlay shows numbered badges (1, 2, 3...) | Numbers visible on clickable elements |
| 3 | Say "one" | Item #1 activated |
| 4 | Say "five" | Item #5 activated |
| 5 | Verify badge numbers are consistent after scroll | Same element keeps same number |

### 4.5 Test: Compound Commands

**Objective:** Verify multi-word commands work.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Gmail | Inbox visible |
| 2 | Say "compose email" or "new message" | Compose screen opens |
| 3 | Say "show menu" or "open menu" | Navigation drawer opens |

---

## 5. Hash Assignment Tests

### 5.1 Test: Element Hash Consistency

**Objective:** Same element produces same hash every time.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator | Calculator visible |
| 2 | Enable "Element Inspector" in Developer Settings | Inspector overlay shown |
| 3 | Tap on "7" button to inspect | Hash shown (e.g., `a1b2c3d4e5f6g7h8`) |
| 4 | Navigate away and return | Same "7" button |
| 5 | Inspect "7" button again | SAME hash as step 3 |
| 6 | Repeat 5 times | Hash remains constant |

### 5.2 Test: Screen Hash Includes Dimensions

**Objective:** Same screen in different orientations gets different hashes.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator in portrait mode | Portrait layout |
| 2 | Check screen hash in Developer Settings | Hash A (e.g., includes "1080x2340") |
| 3 | Rotate to landscape | Landscape layout |
| 4 | Check screen hash | Hash B (different, includes "2340x1080") |
| 5 | Rotate back to portrait | Portrait layout |
| 6 | Check screen hash | SAME as Hash A |

### 5.3 Test: Hash Excludes Dynamic Content

**Objective:** Dynamic text changes don't affect screen hash.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator | Display shows "0" |
| 2 | Note screen hash | Hash X |
| 3 | Type "12345" | Display shows "12345" |
| 4 | Check screen hash | STILL Hash X (unchanged) |
| 5 | Clear calculator | Display shows "0" |
| 6 | Check screen hash | STILL Hash X |

### 5.4 Test: App Version Hash

**Objective:** App update triggers rescan.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator | Scraped |
| 2 | Check `scraped_apps.app_version` | e.g., "8.5" |
| 3 | Update Calculator app (if possible) | New version installed |
| 4 | Open Calculator | Automatic rescan triggered |
| 5 | Check `scraped_apps.app_version` | New version (e.g., "8.6") |
| 6 | Check `scraped_elements` | Fresh elements |

---

## 6. Deduplication Tests

### 6.1 Test: Duplicate Element Detection

**Objective:** Identical elements are deduplicated.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Contacts app with multiple contacts | Contact list visible |
| 2 | Enable Element Inspector | Inspection mode |
| 3 | Check for duplicate elements | Stats show "Duplicates found: X" |
| 4 | Verify only one command per unique element | Command registry doesn't have duplicates |
| 5 | Check deduplication is by structure, not content | Two contacts with same icon = deduplicated |

### 6.2 Test: List Item Deduplication

**Objective:** Repeated list templates don't create duplicate commands.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Gmail inbox with 50+ emails | Email list visible |
| 2 | Each email row has: avatar, sender, subject, date | Similar structure |
| 3 | Check element count | NOT 50 * 4 = 200 elements |
| 4 | Verify deduplication reduced elements | ~10-20 unique element patterns |
| 5 | Commands are by index ("first", "second") not by duplicate | No duplicate "avatar" commands |

### 6.3 Test: Deduplication Stats

**Objective:** View deduplication metrics.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Developer Settings > Deduplication Stats | Stats panel |
| 2 | Check "Total elements scraped" | e.g., 150 |
| 3 | Check "Unique elements" | e.g., 45 |
| 4 | Check "Duplicates removed" | e.g., 105 |
| 5 | Verify ratio | 30% unique is typical for lists |

---

## 7. Numbered Command Tests

### 7.1 Test: Overlay Badge Rendering

**Objective:** Numbered badges appear on clickable elements.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Settings app | Settings visible |
| 2 | Verify overlay badges visible | Numbers 1, 2, 3... on each item |
| 3 | Badges are positioned at element bounds | Not overlapping, readable |
| 4 | Colors are accessible | Contrast ratio acceptable |
| 5 | Scroll down | New badges appear (4, 5, 6...) |

### 7.2 Test: Badge Number Persistence on Scroll

**Objective:** Same element keeps same number after scroll.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open long list (Settings or Gmail) | List with 20+ items |
| 2 | Note badge #3 is on "Display" | Badge assignment recorded |
| 3 | Scroll down | New items visible |
| 4 | Scroll back up | Original items visible |
| 5 | Check badge #3 | STILL on "Display" |
| 6 | Say "three" | "Display" activates |

### 7.3 Test: Badge Reset on App Change

**Objective:** Badge numbers reset when switching apps.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator | Badges 1-15 on buttons |
| 2 | Note badge #5 is on "4" button | Badge assignment |
| 3 | Switch to Settings | New app |
| 4 | Badges reset to 1, 2, 3... | Fresh numbering |
| 5 | Return to Calculator | Same badges as step 2 (cached) |
| 6 | Badge #5 still on "4" button | Persistence |

### 7.4 Test: Voice Commands Match Badges

**Objective:** Saying number triggers corresponding badge.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open any app with badges | Badges visible |
| 2 | Identify badge #7 visually | e.g., on "Notifications" |
| 3 | Say "seven" | Badge #7 element activates |
| 4 | Say "twelve" | Badge #12 element activates |
| 5 | Say "twenty" | Badge #20 element activates (if exists) |

---

## 8. Operating Mode Tests (Off/On/Auto)

### 8.1 Test: Processing Mode - IMMEDIATE

**Objective:** IMMEDIATE mode provides real-time processing.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enable IMMEDIATE mode (default for voice) | Mode set |
| 2 | Open new screen | Screen scraped |
| 3 | Measure time from screen visible to commands ready | < 500ms |
| 4 | Say a command | Response < 200ms |
| 5 | Verify no queuing | Commands process one-by-one |

### 8.2 Test: Processing Mode - BATCH

**Objective:** BATCH mode processes elements efficiently.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enable BATCH mode (exploration/learning) | Mode set |
| 2 | Open screen with 100+ elements | Complex screen |
| 3 | Batch processes up to 100 elements at once | Batch processing indicator |
| 4 | Total time < 5000ms | Within timeout |
| 5 | All elements processed correctly | No skipped elements |

### 8.3 Test: Feature Toggle - Off State

**Objective:** Disabled features don't execute.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Developer Settings > disable "ELEMENT_SCRAPING" | Feature off |
| 2 | Open new app | App visible |
| 3 | No scraping occurs | No commands generated |
| 4 | Database not updated | No new elements |
| 5 | Voice commands don't work for new screens | Expected behavior |
| 6 | Re-enable "ELEMENT_SCRAPING" | Feature on |
| 7 | Scraping resumes | Commands generated |

### 8.4 Test: Auto Mode (Adaptive)

**Objective:** Auto mode adapts to context.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enable AUTO mode | Adaptive processing |
| 2 | Interact actively (voice commands) | IMMEDIATE processing |
| 3 | Stop interacting for 30 seconds | Switch to BATCH mode |
| 4 | Resume voice commands | Back to IMMEDIATE |

---

## 9. App-Specific Tests

### 9.1 Test: Microsoft Teams

**Objective:** Teams app works with voice commands.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Microsoft Teams | Teams home screen |
| 2 | Verify scraping completes | Commands generated |
| 3 | Say "chat" or "activity" | Tab navigation works |
| 4 | Say "search" | Search activates |
| 5 | In chat list, say "first" | First chat opens |
| 6 | Say "video call" | Video call initiates |
| 7 | Navigate to Teams list | Team list visible |
| 8 | Use numbered badges | Correct team selected |

### 9.2 Test: Gmail

**Objective:** Gmail app works with voice commands.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Gmail | Inbox visible |
| 2 | Verify scraping completes | Commands generated |
| 3 | Say "compose" or "new email" | Compose screen |
| 4 | Say "menu" or "hamburger" | Navigation drawer |
| 5 | In inbox, say "first" | First email opens |
| 6 | Say "reply" | Reply mode |
| 7 | Say "archive" | Email archived |
| 8 | In labels, say "starred" | Starred folder |
| 9 | Use numbered badges for emails | Correct email selected |

### 9.3 Test: Calculator

**Objective:** Calculator buttons work perfectly.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator | Calculator visible |
| 2 | Say "seven" | 7 appears |
| 3 | Say "times" or "multiply" | × operator |
| 4 | Say "eight" | 8 appears |
| 5 | Say "equals" | Result: 56 |
| 6 | Say "clear" | Display cleared |
| 7 | Test all digits 0-9 | All work |
| 8 | Test: plus, minus, divide, percent, decimal | All operators work |
| 9 | Say "backspace" or "delete" | Last digit removed |

### 9.4 Test: Settings App

**Objective:** System Settings navigable by voice.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Settings | Settings main screen |
| 2 | Say "network" or "wifi" | Network settings |
| 3 | Say "back" | Previous screen |
| 4 | Say "display" | Display settings |
| 5 | Say "sound" | Sound settings |
| 6 | Use index commands in lists | Correct item selected |
| 7 | Toggle switches by voice | Switches toggle |

### 9.5 Test: Contacts App

**Objective:** Contacts navigable by voice.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Contacts | Contact list visible |
| 2 | Say "search" | Search field |
| 3 | Use index commands | Correct contact selected |
| 4 | Say "call" | Dialer opens |
| 5 | Say "message" or "text" | Messaging opens |
| 6 | Say "edit" | Edit contact |

---

## 10. Dynamic Lists Tests

### 10.1 Test: List Detection

**Objective:** Dynamic containers are detected.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Gmail inbox | Email list (RecyclerView) |
| 2 | Check Element Inspector | Container flagged as "dynamic" |
| 3 | Children have `listIndex` values | 0, 1, 2, 3... |
| 4 | Container type identified | "RecyclerView" |

### 10.2 Test: List Index Commands

**Objective:** Position-based commands work.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Settings (list) | Settings items 1-10 visible |
| 2 | Say "first" | Item at index 0 selected |
| 3 | Say "second" | Item at index 1 selected |
| 4 | Say "tenth" | Item at index 9 selected |
| 5 | Scroll to reveal more | Items 11-20 visible |
| 6 | Say "first" | NOW selects item visible at top (re-indexed) |

### 10.3 Test: List Numeric Commands

**Objective:** Badge numbers work for list items.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open list app | List visible |
| 2 | Badges 1, 2, 3... on items | Visible |
| 3 | Say "one" | First item |
| 4 | Say "five" | Fifth item |
| 5 | Scroll down | Badges persist (incremental) |
| 6 | Item that was #5 keeps #5 | Stable numbering |

### 10.4 Test: List Cleanup on Navigation

**Objective:** List commands cleared on screen change.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Gmail inbox | List commands active |
| 2 | Check command registry | 30+ list commands |
| 3 | Open email detail | Different screen |
| 4 | Check command registry | Inbox list commands cleared |
| 5 | New commands for detail screen | Fresh commands |
| 6 | Return to inbox | List commands regenerated |

---

## 11. Memory & Cleanup Tests

### 11.1 Test: Screen Cache Cleanup

**Objective:** Unused cache entries are cleaned.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open 10 different apps | 10 screen caches created |
| 2 | Check cache size in Developer Settings | ~10 entries |
| 3 | Trigger "Clear Cache" | Cache cleared |
| 4 | Verify cache empty | 0 entries |
| 5 | Re-open app | Fresh scrape, cache rebuilt |

### 11.2 Test: Per-App Cache Clear ("Rescan Current App")

**Objective:** Single app can be rescanned without affecting others.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator, Gmail, Settings | 3 apps cached |
| 2 | Check cache: 3 app entries | Verified |
| 3 | In Calculator, trigger "Rescan Current App" | Calculator cache cleared |
| 4 | Check cache | Gmail + Settings still cached |
| 5 | Calculator rescanned fresh | New cache entry |

### 11.3 Test: Memory Footprint

**Objective:** Memory usage stays reasonable.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Note initial memory (Developer Settings) | e.g., 50MB |
| 2 | Use app for 30 minutes, open 20+ apps | Heavy usage |
| 3 | Check memory | < 150MB (reasonable growth) |
| 4 | Trigger garbage collection | Memory reduced |
| 5 | Memory < 100MB after GC | Cleanup works |

### 11.4 Test: Command Registry Cleanup

**Objective:** Old commands are cleaned when app changes.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator | ~20 commands in registry |
| 2 | Switch to Gmail | Calculator commands cleared |
| 3 | Gmail commands active | ~50 commands |
| 4 | Switch to Settings | Gmail commands cleared |
| 5 | Only current app's commands active | Memory efficient |

### 11.5 Test: Dynamic List Memory

**Objective:** Dynamic lists don't leak memory.

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Gmail inbox (100+ emails) | List visible |
| 2 | Scroll through entire list | All items rendered |
| 3 | Check memory | Stable (not growing unbounded) |
| 4 | Navigate away | List commands freed |
| 5 | Memory decreases | No leak |

---

## 12. Performance Benchmarks

### 12.1 Target Metrics

| Operation | Target | Acceptable | Unacceptable |
|-----------|--------|------------|--------------|
| Service initialization | < 2000ms | < 3000ms | > 5000ms |
| Screen scrape | < 500ms | < 1000ms | > 2000ms |
| Command generation | < 100ms | < 200ms | > 500ms |
| Voice command execution | < 100ms | < 200ms | > 500ms |
| Database query | < 50ms | < 100ms | > 200ms |
| Screen hash calculation | < 50ms | < 100ms | > 200ms |
| Cache lookup | < 10ms | < 50ms | > 100ms |

### 12.2 Test: Service Startup Time

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Force stop VoiceOSCoreNG | Service stopped |
| 2 | Start stopwatch | Timer running |
| 3 | Enable accessibility service | Service starting |
| 4 | Stop when overlay appears | Time recorded |
| 5 | Time < 2000ms | Pass |

### 12.3 Test: Scrape Latency

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Clear cache for test app | Fresh start |
| 2 | Open test app, start timer | Timer running |
| 3 | Stop when badges appear | Time recorded |
| 4 | Time < 500ms | Pass |

### 12.4 Test: Cache Hit Performance

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Calculator (first time) | Fresh scrape |
| 2 | Note time: ~500ms | Baseline |
| 3 | Navigate away, return | Cache hit |
| 4 | Time < 100ms (cached) | 5x faster |

---

## 13. Edge Cases & Error Handling

### 13.1 Test: Empty Screen

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open app with minimal UI (e.g., blank screen) | Empty screen |
| 2 | Service handles gracefully | No crash |
| 3 | No badges shown | Expected |
| 4 | Log message: "No clickable elements" | Informative |

### 13.2 Test: Extremely Long List

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open app with 1000+ items (e.g., large contact list) | Long list |
| 2 | Service doesn't freeze | Responsive |
| 3 | Only visible items are indexed | Efficient |
| 4 | Scroll is smooth | No lag |

### 13.3 Test: Rapid Screen Changes

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate quickly between screens | Fast transitions |
| 2 | Debouncing prevents thrashing | 1000ms debounce |
| 3 | Final screen is correctly scraped | Accurate commands |
| 4 | No duplicate scrapes | Efficient |

### 13.4 Test: App Crash Recovery

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open target app | App visible |
| 2 | Force crash target app | App closes |
| 3 | VoiceOSCoreNG handles gracefully | No crash |
| 4 | Switch to different app | Service continues |

### 13.5 Test: Low Memory Conditions

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open many apps to exhaust memory | Low memory |
| 2 | VoiceOSCoreNG degrades gracefully | Reduced functionality OK |
| 3 | No crash | Stable |
| 4 | When memory freed, full function resumes | Recovery |

### 13.6 Test: Accessibility Service Restart

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Disable accessibility service | Service stops |
| 2 | Re-enable accessibility service | Service restarts |
| 3 | All functionality works | Full recovery |
| 4 | Cache preserved | Quick startup |

---

## Appendix: Database Schema

### voice_commands Table

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| avid | TEXT | Unique voice ID |
| phrase | TEXT | Voice command phrase |
| action_type | TEXT | CLICK, SCROLL, TEXT_ENTRY, etc. |
| target_vuid | TEXT | Element hash for targeting |
| confidence | REAL | 0.0-1.0 confidence score |
| app_package | TEXT | Package name |
| app_version | TEXT | App version when scraped |
| created_at | INTEGER | Timestamp |

### scraped_apps Table

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| package_name | TEXT | App package |
| version_code | INTEGER | App version code |
| version_name | TEXT | App version name |
| last_scraped | INTEGER | Last scrape timestamp |
| element_count | INTEGER | Total elements scraped |

### scraped_elements Table

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| element_hash | TEXT | SHA-256 hash (16 chars) |
| class_name | TEXT | Widget class |
| resource_id | TEXT | Resource ID |
| text | TEXT | Element text |
| content_desc | TEXT | Content description |
| bounds | TEXT | JSON bounds object |
| is_clickable | INTEGER | 0 or 1 |
| is_scrollable | INTEGER | 0 or 1 |
| list_index | INTEGER | Position in list (-1 if not list) |
| app_package | TEXT | Parent app |
| screen_hash | TEXT | Parent screen hash |

### screen_contexts Table

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| screen_hash | TEXT | SHA-256 screen hash |
| package_name | TEXT | App package |
| activity_name | TEXT | Activity class name |
| element_count | INTEGER | Elements on screen |
| dimension_width | INTEGER | Screen width |
| dimension_height | INTEGER | Screen height |
| created_at | INTEGER | First seen timestamp |
| last_accessed | INTEGER | Last cache hit |

---

## Test Execution Checklist

Use this checklist to track test completion:

- [ ] 3.1 First Launch - Database Initialization
- [ ] 3.2 App Scraping Populates Database
- [ ] 3.3 Screen Context Persistence
- [ ] 3.4 Command Persistence Across Sessions
- [ ] 4.1 Label-Based Commands
- [ ] 4.2 Content Description Commands
- [ ] 4.3 Index Commands for Lists
- [ ] 4.4 Numeric Commands (Overlay Badges)
- [ ] 4.5 Compound Commands
- [ ] 5.1 Element Hash Consistency
- [ ] 5.2 Screen Hash Includes Dimensions
- [ ] 5.3 Hash Excludes Dynamic Content
- [ ] 5.4 App Version Hash
- [ ] 6.1 Duplicate Element Detection
- [ ] 6.2 List Item Deduplication
- [ ] 6.3 Deduplication Stats
- [ ] 7.1 Overlay Badge Rendering
- [ ] 7.2 Badge Number Persistence on Scroll
- [ ] 7.3 Badge Reset on App Change
- [ ] 7.4 Voice Commands Match Badges
- [ ] 8.1 Processing Mode - IMMEDIATE
- [ ] 8.2 Processing Mode - BATCH
- [ ] 8.3 Feature Toggle - Off State
- [ ] 8.4 Auto Mode (Adaptive)
- [ ] 9.1 Microsoft Teams
- [ ] 9.2 Gmail
- [ ] 9.3 Calculator
- [ ] 9.4 Settings App
- [ ] 9.5 Contacts App
- [ ] 10.1 List Detection
- [ ] 10.2 List Index Commands
- [ ] 10.3 List Numeric Commands
- [ ] 10.4 List Cleanup on Navigation
- [ ] 11.1 Screen Cache Cleanup
- [ ] 11.2 Per-App Cache Clear
- [ ] 11.3 Memory Footprint
- [ ] 11.4 Command Registry Cleanup
- [ ] 11.5 Dynamic List Memory
- [ ] 12.2 Service Startup Time
- [ ] 12.3 Scrape Latency
- [ ] 12.4 Cache Hit Performance
- [ ] 13.1 Empty Screen
- [ ] 13.2 Extremely Long List
- [ ] 13.3 Rapid Screen Changes
- [ ] 13.4 App Crash Recovery
- [ ] 13.5 Low Memory Conditions
- [ ] 13.6 Accessibility Service Restart

---

**Document End**
