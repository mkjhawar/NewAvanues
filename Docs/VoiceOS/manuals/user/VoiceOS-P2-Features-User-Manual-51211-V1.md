# VoiceOS P2 Features - User Manual

**Version:** 1.0
**Date:** 2025-12-11
**Audience:** End Users, Testers
**Status:** Published

---

# Table of Contents

1. [Introduction](#1-introduction)
2. [Getting Started](#2-getting-started)
3. [Feature: Exploration from LearnApp](#3-feature-exploration-from-learnapp)
4. [Feature: Graph Viewer (LearnAppPro)](#4-feature-graph-viewer-learnappro)
5. [Feature: Learned Screen Status](#5-feature-learned-screen-status)
6. [Performance Improvements](#6-performance-improvements)
7. [Troubleshooting](#7-troubleshooting)
8. [FAQ](#8-faq)
9. [New in December 2025 Update](#9-new-in-december-2025-update)

---

# 1. Introduction

## 1.1 What's New in P2

The P2 update brings three major features to VoiceOS LearnApp:

| Feature | Description | Available In |
|---------|-------------|--------------|
| **Remote Exploration** | Start, stop, pause exploration from LearnApp | LearnApp, LearnAppPro |
| **Graph Viewer** | Visualize learned apps in Neo4j | LearnAppPro only |
| **Screen Status** | See which screens are already learned | LearnApp, LearnAppPro |

## 1.2 Requirements

- VoiceOS Core installed and accessibility service enabled
- LearnApp or LearnAppPro installed
- For Graph Viewer: Neo4j database server (local or remote)

---

# 2. Getting Started

## 2.1 Enabling VoiceOS Accessibility Service

1. Open **Settings** > **Accessibility**
2. Find **VoiceOS** in the list
3. Tap to enable the service
4. Grant all requested permissions

## 2.2 Installing LearnApp

### Standard LearnApp
- Install `learnapp-release.apk`
- Features: Exploration control, screen status

### LearnAppPro (Developer Edition)
- Install `learnappdev-release.apk`
- Features: All standard + Graph Viewer, debug tools

## 2.3 Connecting LearnApp to VoiceOS

LearnApp automatically connects to VoiceOS when:
1. VoiceOS accessibility service is running
2. LearnApp is opened

**Connection Status Indicators:**
```
● Green dot = Connected to VoiceOS
○ Gray dot = Disconnected
◐ Blue spinner = Connecting...
```

---

# 3. Feature: Exploration from LearnApp

## 3.1 What is Exploration?

Exploration is the process of automatically learning an app's interface:
- VoiceOS navigates through the app
- Discovers all screens and buttons
- Generates voice commands for each element
- Saves the learned data for future use

## 3.2 Starting Exploration

### Step 1: Open LearnApp
Launch LearnApp from your app drawer.

### Step 2: Select an App
```
┌────────────────────────────────────┐
│  SELECT APP TO EXPLORE             │
│                                    │
│  📱 Google Photos                  │
│     Not yet explored               │
│                                    │
│  📱 Gmail                          │
│     42 screens learned             │
│                                    │
│  📱 Chrome                         │
│     128 screens learned            │
└────────────────────────────────────┘
```

Tap on the app you want to explore.

### Step 3: Start Exploration
Tap **"Start Exploration"** button.

```
┌────────────────────────────────────┐
│  GOOGLE PHOTOS                     │
│                                    │
│  [    Start Exploration    ]       │
│                                    │
│  Estimated time: 5-10 minutes      │
│  Will discover: 50+ screens        │
└────────────────────────────────────┘
```

### Step 4: Monitor Progress

```
┌────────────────────────────────────┐
│  EXPLORATION RUNNING               │
│                                    │
│  [====================     ] 75%   │
│                                    │
│  Screens: 38                       │
│  Elements: 892                     │
│  Depth: 4                          │
│  Time: 3m 42s                      │
│                                    │
│  [  Pause  ]  [  Stop  ]           │
└────────────────────────────────────┘
```

## 3.3 Controlling Exploration

### Pause Exploration
- Tap **"Pause"** to temporarily stop
- The app won't navigate further
- Useful if you need to use your phone

### Resume Exploration
- Tap **"Resume"** to continue from where you left off
- All progress is preserved

### Stop Exploration
- Tap **"Stop"** to end exploration
- Learned data is saved
- You can restart later to learn more

## 3.4 Understanding Exploration Status

| Status | What It Means |
|--------|---------------|
| **Idle** | Ready to start new exploration |
| **Running** | Actively exploring the app |
| **Paused** | User paused, can resume |
| **Paused (Login)** | Login screen detected, please log in |
| **Completed** | Finished exploring all accessible screens |
| **Failed** | Error occurred, can retry |

## 3.5 Login Screen Detection

When exploration encounters a login screen:

```
┌────────────────────────────────────┐
│  🔐 LOGIN SCREEN DETECTED          │
│                                    │
│  Please log into the app to        │
│  continue exploring.               │
│                                    │
│  After logging in, tap Resume.     │
│                                    │
│  [  Resume  ]  [  Skip Login  ]    │
└────────────────────────────────────┘
```

**Options:**
1. **Log in manually** → Open the app, log in, then tap Resume
2. **Skip Login** → Continue without exploring logged-in content

---

# 4. Feature: Graph Viewer (LearnAppPro)

## 4.1 What is Graph Viewer?

Graph Viewer lets you visualize learned app data in Neo4j:
- See screens as nodes
- See navigation paths as relationships
- Query the data with Cypher

**Note:** This feature is only available in LearnAppPro (developer edition).

## 4.2 Setting Up Neo4j

### Option A: Neo4j Desktop (Recommended)
1. Download Neo4j Desktop from https://neo4j.com/download/
2. Install and create a new database
3. Start the database
4. Note the Bolt URI (usually `bolt://localhost:7687`)

### Option B: Neo4j Docker
```bash
docker run -d --name neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password \
  neo4j:latest
```

## 4.3 Connecting to Neo4j

1. Open LearnAppPro
2. Go to **Menu** > **Graph Viewer**
3. Enter connection details:

```
┌────────────────────────────────────┐
│  NEO4J CONNECTION                  │
│                                    │
│  URI:  bolt://localhost:7687       │
│  User: neo4j                       │
│  Pass: ••••••••                    │
│                                    │
│  [      Connect      ]             │
└────────────────────────────────────┘
```

4. Tap **Connect**

### Connection States
```
○ Disconnected - Not connected to Neo4j
◐ Connecting... - Connection in progress
● Connected - Successfully connected
⚠ Error - Connection failed (check settings)
```

## 4.4 Exporting Data to Neo4j

Once connected, export your learned data:

1. Tap **"Export All"**
2. Wait for the export to complete:

```
┌────────────────────────────────────┐
│  EXPORTING TO NEO4J                │
│                                    │
│  [====================     ] 66%   │
│  Exporting elements...             │
│                                    │
│  Progress:                         │
│  ✓ 127 screens exported            │
│  ● 1,642 / 2,458 elements          │
│  ○ 0 / 89 navigations              │
│                                    │
│  [      Cancel      ]              │
└────────────────────────────────────┘
```

3. When complete, you'll see stats:

```
┌────────────────────────────────────┐
│  EXPORT COMPLETE!                  │
│                                    │
│  Exported to Neo4j:                │
│  • 127 screens                     │
│  • 2,458 elements                  │
│  • 89 navigation paths             │
│                                    │
│  [        OK        ]              │
└────────────────────────────────────┘
```

## 4.5 Viewing the Graph

### In Neo4j Browser
1. Open http://localhost:7474 in your browser
2. Log in with your credentials
3. Run queries to explore:

**See all screens:**
```cypher
MATCH (s:Screen) RETURN s LIMIT 25
```

**See navigation paths:**
```cypher
MATCH path = (s1:Screen)-[:NAVIGATES_TO]->(s2:Screen)
RETURN path LIMIT 50
```

**See elements with voice commands:**
```cypher
MATCH (e:Element)
WHERE e.voiceCommand IS NOT NULL
RETURN e.voiceCommand, e.className
LIMIT 20
```

### In LearnAppPro Query Panel

```
┌────────────────────────────────────┐
│  CYPHER QUERY                      │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ MATCH (s:Screen)             │  │
│  │ RETURN s.activityName, count │  │
│  │ ORDER BY count DESC          │  │
│  └──────────────────────────────┘  │
│                                    │
│  [  Execute Query  ]               │
│                                    │
│  RESULTS:                          │
│  MainActivity - 24 elements        │
│  ListActivity - 18 elements        │
│  DetailActivity - 12 elements      │
└────────────────────────────────────┘
```

## 4.6 Example Queries

| Query | Description |
|-------|-------------|
| `MATCH (s:Screen) RETURN s` | All screens |
| `MATCH (s:Screen {packageName:"com.google.photos"}) RETURN s` | Screens for specific app |
| `MATCH (s)-[:HAS_ELEMENT]->(e) RETURN s,e LIMIT 50` | Screens with elements |
| `MATCH path=(s1)-[:NAVIGATES_TO*1..3]->(s2) RETURN path` | Navigation paths (depth 1-3) |
| `MATCH (e:Element) WHERE e.voiceCommand CONTAINS "click" RETURN e` | Elements with "click" command |

---

# 5. Feature: Learned Screen Status

## 5.1 What is Screen Status?

Screen status shows which screens have already been learned:
- Helps avoid redundant exploration
- Shows learning progress per app
- Identifies areas that need more exploration

## 5.2 Viewing Learned Screens

1. Open LearnApp
2. Select an app from the list
3. View the learned screens:

```
┌────────────────────────────────────────┐
│  GOOGLE PHOTOS - LEARNED SCREENS       │
│                                        │
│  Total: 52 screens                     │
│  Elements: 1,248                       │
│  Last explored: 2 hours ago            │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │ MainActivity        │ 24 elem   │  │
│  │ PhotoGridActivity   │ 18 elem   │  │
│  │ AlbumsActivity      │ 12 elem   │  │
│  │ PhotoDetailActivity │ 8 elem    │  │
│  │ SettingsActivity    │ 32 elem   │  │
│  │ ...                              │  │
│  └──────────────────────────────────┘  │
│                                        │
│  [  Explore More  ]  [  Clear All  ]   │
└────────────────────────────────────────┘
```

## 5.3 Understanding Screen Hashes

Each screen is identified by a unique hash (e.g., `abc123def456`).

- **Same hash** = Same screen layout
- **Different hash** = Different screen (or layout changed)

This helps VoiceOS:
- Recognize screens instantly
- Skip re-learning unchanged screens
- Detect when app updates change layouts

---

# 6. Performance Improvements

## 6.1 Faster Command Lists

VoiceOS now loads command lists much faster, especially for apps with many learned commands:

### What's Improved

| Before | After |
|--------|-------|
| All commands loaded at once | Commands loaded page-by-page |
| Slow for apps with 1000+ commands | Fast regardless of command count |
| May freeze on large lists | Smooth scrolling always |

### How It Works

When you view commands for an app like Gmail or Chrome that has hundreds of learned commands:

1. **First 50 commands load instantly** (< 50ms)
2. **Scroll down to load more** - VoiceOS automatically loads the next batch
3. **Smooth performance** - no lag even with 10,000+ commands

### Benefits You'll Notice

**✅ Instant app switching** - No delay when switching between apps in command list

**✅ Smooth scrolling** - Infinite scroll works smoothly even for apps with thousands of commands

**✅ Lower memory usage** - Only loads what you're viewing, saving phone resources

**✅ Faster search** - Finding commands in large lists is now instant

### Example: Before vs After

**Before (App with 5,000 commands):**
```
Opening command list... ⏳ 3 seconds
Scrolling... ⏳ Laggy
Switching apps... ⏳ 2 seconds
```

**After (Same app):**
```
Opening command list... ✅ Instant
Scrolling... ✅ Smooth
Switching apps... ✅ Instant
```

## 6.2 App-Specific Command Filtering

Commands are now organized by app package, making it easier to:

- **Browse commands per app** without seeing commands from other apps
- **Clear commands for specific apps** without affecting others
- **View app-specific statistics** (total commands, most used, etc.)

### Viewing Commands for an App

```
┌─────────────────────────────────────┐
│  GMAIL - VOICE COMMANDS             │
│                                     │
│  Total: 1,247 commands              │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ "Click compose"               │  │
│  │ "Open inbox"                  │  │
│  │ "Click send button"           │  │
│  │ "Click attachment"            │  │
│  │ "Show settings"               │  │
│  │ ...                           │  │
│  │ ↓ Scroll for more             │  │
│  └───────────────────────────────┘  │
│                                     │
│  [  Back  ]  [  Clear All  ]        │
└─────────────────────────────────────┘
```

As you scroll, more commands load automatically - no waiting!

---

# 7. Troubleshooting

## 7.1 LearnApp Can't Connect to VoiceOS

**Symptoms:**
- Gray disconnected indicator
- "Service not available" message

**Solutions:**
1. Check VoiceOS accessibility service is enabled
2. Restart VoiceOS service:
   - Settings > Accessibility > VoiceOS > Turn off then on
3. Restart LearnApp
4. Restart your phone

## 7.2 Exploration Stops Unexpectedly

**Symptoms:**
- Progress bar freezes
- "Failed" status

**Solutions:**
1. Check if the app crashed
2. Check if login is required
3. Tap "Retry" to continue
4. Stop and restart exploration

## 7.3 Neo4j Connection Fails

**Symptoms:**
- "Connection Failed" error
- Timeout errors

**Solutions:**
1. Verify Neo4j is running
2. Check URI is correct (`bolt://` not `http://`)
3. Verify username and password
4. Check network connectivity
5. For remote server, check firewall rules

## 7.4 Export to Neo4j Slow

**Symptoms:**
- Export takes very long
- Progress bar moves slowly

**Solutions:**
1. Reduce data before export (clear old data)
2. Use local Neo4j instance (not remote)
3. Increase Neo4j memory allocation
4. Export one app at a time

---

# 8. FAQ

## General Questions

**Q: Do I need Neo4j to use LearnApp?**
A: No. Neo4j is optional and only available in LearnAppPro. Standard LearnApp works without it.

**Q: How long does exploration take?**
A: Depends on app complexity:
- Simple app: 2-5 minutes
- Medium app: 5-15 minutes
- Complex app: 15-30 minutes

**Q: Can I use my phone during exploration?**
A: Yes, but exploration will pause. Tap Resume when ready.

**Q: Will exploration affect my app data?**
A: Exploration only reads the interface, but may trigger navigation. Log out of sensitive apps if concerned.

## Technical Questions

**Q: What's the difference between JIT learning and exploration?**
A:
- **JIT (Just-In-Time)**: Learns passively as you use apps normally
- **Exploration**: Actively navigates apps to learn comprehensively

**Q: Why do some screens have different hashes?**
A: Screens get new hashes when their layout changes (app updates, different content, etc.)

**Q: Can I export to other graph databases?**
A: Currently only Neo4j is supported. Other databases may be added in future.

**Q: How much storage does learned data use?**
A: Approximately:
- 1 app: 1-5 MB
- 10 apps: 10-50 MB
- 100 apps: 100-500 MB

---

# 9. New in December 2025 Update

## 9.1 Enhanced Exploration Progress Tracking

The December 2025 update brings improved exploration progress tracking. You'll now see more detailed progress information during app exploration.

### What's New

| Feature | Description |
|---------|-------------|
| **Per-Screen Progress** | See progress for each individual screen |
| **Element Click Tracking** | Know exactly which buttons have been explored |
| **Completion Indicators** | Clear visual indicators when screens are fully explored |
| **Export Checklists** | Export exploration progress for your records |

### Progress Display

During exploration, you'll see enhanced progress information:

```
┌────────────────────────────────────────┐
│  EXPLORATION PROGRESS                  │
│                                        │
│  Overall: [================    ] 80%   │
│                                        │
│  Screens explored: 24/30               │
│  Elements clicked: 412/515             │
│  Completed screens: 18                 │
│                                        │
│  Current: SettingsActivity             │
│  Screen progress: 75%                  │
└────────────────────────────────────────┘
```

### Screen-by-Screen Tracking

For each screen, VoiceOS now tracks:
- Total clickable elements found
- How many elements have been clicked
- Whether the screen is fully explored

This helps you understand:
- Which parts of an app are well-learned
- Which screens need more exploration
- Overall exploration quality

## 9.2 Improved Window Detection

VoiceOS now better recognizes different types of windows:

| Window Type | What It Is | How VoiceOS Handles It |
|-------------|------------|------------------------|
| **Main App** | Regular app screens | Full exploration |
| **Dialog** | Pop-up windows | Detects and explores |
| **Overlay** | Floating elements | Skips system overlays |
| **Keyboard** | Input keyboard | Ignores during exploration |
| **System** | System UI | Skips to avoid interference |

### Benefits

- **More accurate exploration** - VoiceOS focuses on actual app content
- **Fewer false positives** - System elements aren't mistakenly learned
- **Better dialog handling** - Pop-ups and modals are properly explored

## 9.3 Just-In-Time Learning Improvements

JIT (Just-In-Time) learning now captures more element information:

### Enhanced Element Recognition

VoiceOS now recognizes:
- **Editable fields** - Text inputs, search boxes
- **Checkboxes** - Toggle options
- **Long-clickable items** - Elements with context menus
- **Focusable elements** - Navigation targets

### Better Voice Commands

With enhanced recognition, VoiceOS generates more accurate voice commands:

| Element Type | Example Commands |
|--------------|------------------|
| Text Input | "Edit search field", "Type in email" |
| Checkbox | "Check remember me", "Uncheck notifications" |
| Long-click | "Long press message", "Hold attachment" |

## 9.4 AI-Powered Context

VoiceOS can now provide intelligent context for apps:

### How It Works

When you use an app, VoiceOS understands:
1. **What screen you're on** - Current activity and layout
2. **What actions are available** - Clickable elements
3. **How to navigate** - Paths to other screens

### Practical Benefits

- **Smarter suggestions** - VoiceOS can suggest relevant commands
- **Better error handling** - If a command fails, VoiceOS knows alternatives
- **Faster learning** - New screens are understood in context

## 9.5 Navigation Graph Visualization

For advanced users, VoiceOS now builds navigation graphs:

### What's a Navigation Graph?

A navigation graph shows:
- All screens in an app (nodes)
- How to get from one screen to another (edges)
- Which buttons lead where

### Why It Matters

- **Complete app maps** - Visualize entire app structure
- **Find missing paths** - Identify unexplored navigation
- **Debug issues** - Understand why certain commands work

### Viewing Navigation (LearnAppPro)

In LearnAppPro, you can export and view navigation graphs:

1. Complete an exploration
2. Go to **Menu** > **Export Navigation Graph**
3. View in Neo4j or export as JSON

---

## 9.6 Reliability and Stability Improvements

The December 18, 2025 update includes important stability improvements that make VoiceOS more reliable.

### Better Error Messages

When something goes wrong, VoiceOS now tells you what happened:

| Situation | What You'll See |
|-----------|-----------------|
| LearnApp not installed | "LearnApp is not installed. Opening Play Store..." |
| Permission denied | "Permission denied to launch LearnApp" |
| Launch failed | "Failed to launch LearnApp: [reason]" |

### Exploration Completion Feedback

When exploration finishes, you'll now see and hear the results:

```
┌────────────────────────────────────────┐
│     COMMANDS DISCOVERED                 │
│                                         │
│           Gmail                         │
│                                         │
│  Screens explored: 24                   │
│  Elements found: 412                    │
│  Navigation paths: 18                   │
│  Duration: 3m 45s                       │
│                                         │
│  Tap to dismiss or wait 10 seconds      │
└────────────────────────────────────────┘
```

VoiceOS also speaks: *"Finished learning Gmail. Found 412 elements across 24 screens."*

### What's Improved

| Before | After |
|--------|-------|
| Silent failures | Clear error messages |
| No completion feedback | Visual overlay + voice summary |
| App may freeze on errors | Graceful error handling |

### Auto-Dismissing Notifications

- Exploration results appear for **10 seconds** then auto-hide
- Tap anywhere on the overlay to dismiss immediately
- Error notifications also auto-dismiss

### Smoother App Switching

When you go to Settings and come back:
- VoiceOS immediately detects if accessibility service is now enabled
- No need to restart the app
- Setup wizard automatically advances

---

# Document History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-11 | Initial release |
| 1.1 | 2025-12-13 | Added Section 6: Performance Improvements (pagination, app-specific filtering) |
| 1.2 | 2025-12-17 | Added Section 9: December 2025 Update (exploration tracking, window detection, JIT improvements, AI context, navigation graphs) |
| 1.3 | 2025-12-18 | Added Section 9.6: Reliability and Stability Improvements (error messages, exploration feedback, auto-dismiss) |

---

**End of User Manual**
