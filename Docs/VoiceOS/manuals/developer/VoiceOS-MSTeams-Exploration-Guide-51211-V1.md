# VoiceOS LearnAppPro - MS Teams Exploration Guide

**Version:** 1.0
**Date:** 2025-12-11
**Author:** Manoj Jhawar (with Claude AI assistance)
**Audience:** Developers, QA Testers
**Status:** For Review
**App Version:** Microsoft Teams (com.microsoft.teams)

---

# Table of Contents

1. [Overview](#1-overview)
2. [Exploration Architecture](#2-exploration-architecture)
3. [Step-by-Step Walkthrough](#3-step-by-step-walkthrough)
4. [MS Teams UI Patterns](#4-ms-teams-ui-patterns)
5. [Expected Results](#5-expected-results)
6. [Neo4j Graph Analysis](#6-neo4j-graph-analysis)
7. [Voice Commands Reference](#7-voice-commands-reference)
8. [Testing Checklist](#8-testing-checklist)

---

# 1. Overview

## 1.1 Why MS Teams?

Microsoft Teams is an excellent test case for LearnAppPro exploration because it contains:

| UI Pattern | Teams Implementation | Exploration Challenge |
|------------|---------------------|----------------------|
| Tab Navigation | 5 main tabs (Activity, Chat, Teams, Calendar, Calls) | Must handle ViewPager swipes |
| Nested Lists | Teams > Channels > Threads | Deep hierarchy (5+ levels) |
| Dynamic Content | Chat messages, activity feed | Content changes frequently |
| Overlays | Bottom sheets, dialogs | Overlay vs new screen detection |
| Login Flows | Microsoft OAuth | Login screen detection |
| Scrollable Lists | Chat history, member lists | Scroll-to-discover pattern |
| Context Menus | Long-press actions | Menu capture and dismiss |

## 1.2 Exploration Goals

After successful exploration, VoiceOS should enable:

```
User says: "click chat"           → Navigate to Chat tab
User says: "click sarah"          → Open Sarah's conversation
User says: "click send"           → Send message
User says: "click attach"         → Open attachment picker
User says: "click teams"          → Navigate to Teams tab
User says: "click engineering"    → Open Engineering team
User says: "click general"        → Open General channel
User says: "click meeting"        → Join or view meeting
```

---

# 2. Exploration Architecture

## 2.1 System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        LEARNAPP PRO EXPLORATION FLOW                         │
│                        (Microsoft Teams Example)                             │
└─────────────────────────────────────────────────────────────────────────────┘

User taps "Explore MS Teams"
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PHASE 1: INITIALIZATION                                                      │
│ ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│ │ Bind to JIT │───►│ Pause JIT   │───►│ Launch App  │───►│ Wait for    │   │
│ │ Service     │    │ Capture     │    │ via Intent  │    │ Window      │   │
│ └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PHASE 2: SCREEN CAPTURE                                                      │
│ ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│ │ Get Root    │───►│ Calculate   │───►│ Check if    │───►│ Extract     │   │
│ │ Node Info   │    │ Screen Hash │    │ Already     │    │ All         │   │
│ │             │    │ (MD5)       │    │ Learned     │    │ Elements    │   │
│ └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PHASE 3: ELEMENT ANALYSIS                                                    │
│ ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│ │ Find        │───►│ Generate    │───►│ Detect      │───►│ Store in    │   │
│ │ Clickable   │    │ Voice       │    │ Navigation  │    │ Database    │   │
│ │ Elements    │    │ Commands    │    │ Targets     │    │             │   │
│ └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PHASE 4: NAVIGATION (DFS)                                                    │
│ ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐   │
│ │ Click Next  │───►│ Wait for    │───►│ Capture     │───►│ Backtrack   │   │
│ │ Unexplored  │    │ Screen      │    │ New Screen  │    │ When Done   │   │
│ │ Element     │    │ Change      │    │             │    │             │   │
│ └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2.2 IPC Communication

```
LearnAppPro                          VoiceOSCore
     │                                    │
     │  bindService(JITLearningService)   │
     │───────────────────────────────────►│
     │                                    │
     │  IElementCaptureService.Stub       │
     │◄───────────────────────────────────│
     │                                    │
     │  pauseCapture()                    │
     │───────────────────────────────────►│ ← Prevents JIT from interfering
     │                                    │
     │  getLearnedScreenHashes("teams")   │
     │───────────────────────────────────►│ ← Skip already-learned screens
     │                                    │
     │  registerExplorationListener()     │
     │───────────────────────────────────►│ ← For progress updates
     │                                    │
     │  startExploration("teams")         │
     │───────────────────────────────────►│ ← Begin exploration
     │                                    │
```

---

# 3. Step-by-Step Walkthrough

## Step 1: Service Connection (0-2 seconds)

**What happens:**
- LearnAppPro binds to JITLearningService running in VoiceOSCore
- Gets AIDL proxy for IPC communication
- Pauses JIT capture (avoids duplicate learning)
- Registers for exploration progress callbacks

**Code path:**
```
LearnAppActivity.onCreate()
  └► ServiceConnection.onServiceConnected()
      └► IElementCaptureService.Stub.asInterface(binder)
          └► service.pauseCapture()
          └► service.registerExplorationListener(listener)
```

## Step 2: Check Already Learned Screens (0.5 seconds)

```kotlin
// LearnAppPro queries existing learned data
val learnedHashes = service.getLearnedScreenHashes("com.microsoft.teams")

// Result for MS Teams (example):
learnedHashes = [
    "a1b2c3d4e5f6",  // MainActivity (tabs)
    "f6e5d4c3b2a1",  // ChatListFragment
    "1a2b3c4d5e6f",  // SettingsActivity
    // ... 47 more screens previously learned via JIT
]
```

**Purpose:** Skip screens already learned passively by JIT to avoid redundancy.

## Step 3: Launch MS Teams (1-3 seconds)

```kotlin
val intent = packageManager.getLaunchIntentForPackage("com.microsoft.teams")
startActivity(intent)
```

**Wait condition:** Accessibility event with `packageName == "com.microsoft.teams"`

## Step 4: Capture Initial Screen - Activity Tab

```
┌─────────────────────────────────────────────────────────────────┐
│ MS TEAMS - INITIAL SCREEN                                        │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Search                                                  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  ACTIVITY    CHAT    TEAMS    CALENDAR    CALLS         │    │
│  │     ▲                                                   │    │
│  │   (active)                                              │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  John mentioned you in General                           │◄──┼── Clickable
│  │     "Hey @you, check this out..."                        │    │
│  │     2 minutes ago                                        │    │
│  ├─────────────────────────────────────────────────────────┤    │
│  │  Meeting starting soon                                   │◄──┼── Clickable
│  │     Product Review - 10:00 AM                            │    │
│  ├─────────────────────────────────────────────────────────┤    │
│  │  Sarah sent a message                                    │◄──┼── Clickable
│  │     "Can you review the doc?"                            │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────┐                                                        │
│  │  ≡  │ ← Hamburger menu                                       │
│  └─────┘                                                        │
└─────────────────────────────────────────────────────────────────┘
```

**Accessibility Tree:**
```
Node[0]: FrameLayout (root)
├── Node[1]: LinearLayout
│   ├── Node[2]: EditText "Search" [clickable, focusable]
│   └── Node[3]: TabLayout
│       ├── Node[4]: Tab "Activity" [clickable, selected]
│       ├── Node[5]: Tab "Chat" [clickable]
│       ├── Node[6]: Tab "Teams" [clickable]
│       ├── Node[7]: Tab "Calendar" [clickable]
│       └── Node[8]: Tab "Calls" [clickable]
├── Node[9]: RecyclerView [scrollable]
│   ├── Node[10]: ActivityItem [clickable] "John mentioned you"
│   ├── Node[11]: ActivityItem [clickable] "Meeting starting"
│   └── Node[12]: ActivityItem [clickable] "Sarah sent message"
└── Node[13]: ImageButton "Menu" [clickable]
```

**Screen Hash:** `7f8a9b0c1d2e`

## Step 5: Extract Elements & Generate Commands

| StableID | Class | Text/Desc | Voice Command |
|----------|-------|-----------|---------------|
| teams:id/search | EditText | "Search" | "click search" |
| teams:id/tab_0 | Tab | "Activity" | "click activity" |
| teams:id/tab_1 | Tab | "Chat" | "click chat" |
| teams:id/tab_2 | Tab | "Teams" | "click teams" |
| teams:id/tab_3 | Tab | "Calendar" | "click calendar" |
| teams:id/tab_4 | Tab | "Calls" | "click calls" |
| teams:id/item_0 | ViewGroup | "John mentioned" | "click john mentioned" |
| teams:id/item_1 | ViewGroup | "Meeting..." | "click meeting" |
| teams:id/item_2 | ViewGroup | "Sarah sent..." | "click sarah" |
| teams:id/menu | ImageButton | "Menu" | "click menu" |

## Step 6: DFS Navigation - Click "Chat" Tab

**Before click:**
```
Stack: [MainActivity/Activity (current)]
Unexplored: [Chat, Teams, Calendar, Calls, Search, Menu, item_0, item_1, item_2]
```

**Action:** Click "Chat" tab

**After click:**
```
Stack: [MainActivity/Activity, ChatListFragment (current)]
Explored: [Chat ✓]
Unexplored: [Teams, Calendar, Calls, ...]
```

## Step 7: Capture Chat List Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ MS TEAMS - CHAT LIST                                             │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Search chats                                            │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  New chat                                                │◄──┼── Clickable
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Sarah Johnson                                           │◄──┼── Clickable
│  │     Can you review the doc?                              │    │
│  │     10:30 AM                                             │    │
│  ├─────────────────────────────────────────────────────────┤    │
│  │  Project Alpha Team                                      │◄──┼── Clickable
│  │     Mike: Let's sync up tomorrow                         │    │
│  │     Yesterday                                            │    │
│  ├─────────────────────────────────────────────────────────┤    │
│  │  John Smith                                              │◄──┼── Clickable
│  │     Thanks for the update!                               │    │
│  │     Monday                                               │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

**New Screen Hash:** `b2c3d4e5f6a7`

## Step 8: Deep Dive - Open Chat Conversation

**Action:** Click "Sarah Johnson"

```
┌─────────────────────────────────────────────────────────────────┐
│ MS TEAMS - CHAT CONVERSATION                                     │
├─────────────────────────────────────────────────────────────────┤
│  ◀ Back    Sarah Johnson    📞 📹 ⋮                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Sarah: Can you review the doc?               10:30 AM  │    │
│  │  ┌──────────────────────────────────────────────────┐   │    │
│  │  │ Project_Spec_v2.docx                             │◄──┼── File attachment
│  │  └──────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  You: Sure, I'll take a look                  10:32 AM  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Type a message...                    📎  😊  📷  🎤   │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

**Elements discovered:**
- Back button → "click back"
- Call button → "click call"
- Video button → "click video"
- More options → "click more options"
- File attachment → "click project spec"
- Message input → "click message input"
- Attach button → "click attach"
- Emoji button → "click emoji"
- Camera button → "click camera"
- Voice button → "click voice message"

---

# 4. MS Teams UI Patterns

## 4.1 Pattern: Tab Navigation

```
┌─────────────────────────────────────────────────────────────────┐
│  ACTIVITY    CHAT    TEAMS    CALENDAR    CALLS                 │
└─────────────────────────────────────────────────────────────────┘

Exploration Strategy:
1. Identify TabLayout as navigation container
2. Click each tab sequentially
3. Capture content for each tab
4. Each tab = different screen hash
```

## 4.2 Pattern: Overflow Menu

```
Click "⋮" (more options) → Opens popup menu

┌──────────────────────────────────────────────────┐
│                                   ┌──────────┐   │
│                                   │ Mute     │   │
│                                   │ Pin      │   │
│                                   │ Hide     │   │
│                                   │ Mark read│   │
│                                   │ Delete   │   │
│                                   └──────────┘   │
└──────────────────────────────────────────────────┘

Exploration Strategy:
1. Detect TYPE_WINDOW_STATE_CHANGED
2. Check if isPopupWindow = true
3. Capture menu items (do NOT record as new screen)
4. Press back to dismiss
5. Continue with other elements
```

## 4.3 Pattern: Bottom Sheet

```
Long-press message → Action sheet

┌─────────────────────────────────────────────────────────────────┐
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │    │
│  │                                                         │    │
│  │  👍 😂 ❤️ 😮 😢 😡    + Add reaction                   │    │
│  │                                                         │    │
│  │  ───────────────────────────────────────────────────── │    │
│  │  Copy text                                              │    │
│  │  Pin message                                            │    │
│  │  Save message                                           │    │
│  │  Reply                                                  │    │
│  │  Forward                                                │    │
│  │  Delete                                                 │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

Detection: android:id/design_bottom_sheet
Strategy: Mark as overlay, capture items, dismiss
```

## 4.4 Pattern: Expandable Lists

```
┌─────────────────────────────────────────────────────────────────┐
│  Your teams                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Engineering                                 ▼ Expanded  │    │
│  │      └─ General                                          │    │
│  │      └─ Random                                           │    │
│  │      └─ Project Updates                                  │    │
│  ├─────────────────────────────────────────────────────────┤    │
│  │  Marketing                                   ▶ Collapsed │    │
│  ├─────────────────────────────────────────────────────────┤    │
│  │  Sales                                       ▶ Collapsed │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

Strategy:
1. Detect collapsed (▶) indicators
2. Click to expand
3. Capture revealed children
4. Record parent-child relationship in database
5. Continue DFS into channels
```

## 4.5 Pattern: Login Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ MICROSOFT SIGN IN                                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                    Microsoft                                     │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Email, phone, or Skype                                  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  [ No account? Create one! ]                                    │
│                                                                  │
│  [          Next          ]                                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

Detection Heuristics:
- Password field present
- "sign in", "log in", "email" text
- OAuth buttons
- Activity name contains "login"

Action: Pause exploration, notify user
```

---

# 5. Expected Results

## 5.1 Statistics Summary

| Metric | Expected Value |
|--------|---------------|
| Total Time | 15-20 minutes |
| Screens Explored | 100-150 |
| Elements Discovered | 2,000-3,000 |
| Navigation Paths | 80-120 |
| Max Depth | 5-6 levels |
| Login Pauses | 0-1 |

## 5.2 Screen Breakdown

| Activity/Fragment | Est. Screens | Est. Elements |
|-------------------|--------------|---------------|
| MainActivity (tabs) | 5 | 50 |
| ChatListFragment | 1 | 50 |
| ChatConversationActivity | 30-40 | 700-900 |
| TeamChannelActivity | 25-35 | 500-600 |
| MeetingActivity | 10-15 | 250-350 |
| SettingsActivity | 8-10 | 100-150 |
| ProfileActivity | 3-5 | 80-100 |
| SearchActivity | 5-8 | 120-160 |
| CalendarActivity | 12-18 | 220-280 |
| CallsActivity | 6-10 | 80-120 |
| Dialogs/Menus | 5-10 | 40-60 |

## 5.3 Navigation Graph

```
MainActivity (Activity Tab)
├── Chat Tab ─────────────► ChatListFragment
│                               ├── Chat 1 ─► ConversationActivity
│                               │               ├── Message Actions (overlay)
│                               │               └── Attachment Picker
│                               ├── Chat 2 ─► ConversationActivity
│                               └── New Chat ─► NewChatActivity
│
├── Teams Tab ────────────► TeamsListFragment
│                               ├── Team 1 (expand)
│                               │   ├── General ─► ChannelActivity
│                               │   │               ├── Thread 1 ─► ThreadActivity
│                               │   │               └── Thread 2 ─► ThreadActivity
│                               │   └── Random ─► ChannelActivity
│                               └── Team 2 (expand)
│
├── Calendar Tab ─────────► CalendarFragment
│                               ├── Meeting 1 ─► MeetingDetailActivity
│                               └── Meeting 2 ─► MeetingDetailActivity
│
├── Calls Tab ────────────► CallsFragment
│                               ├── Contact 1 ─► CallActivity
│                               └── Contact 2 ─► CallActivity
│
└── Menu ─────────────────► Navigation Drawer
                                ├── Settings ─► SettingsActivity
                                └── Profile ─► ProfileActivity
```

---

# 6. Neo4j Graph Analysis

## 6.1 Export to Neo4j

After exploration, export to Neo4j via Graph Viewer:

```cypher
// Verify data import
MATCH (s:Screen) WHERE s.packageName = "com.microsoft.teams"
RETURN count(s) as screenCount

MATCH (e:Element) WHERE e.packageName = "com.microsoft.teams"
RETURN count(e) as elementCount

MATCH ()-[r:NAVIGATES_TO]->()
RETURN count(r) as navigationCount
```

## 6.2 Useful Queries

**Find all navigation paths from Activity tab:**
```cypher
MATCH path = (start:Screen {activityName: "MainActivity"})
             -[:NAVIGATES_TO*1..4]->
             (end:Screen)
RETURN path
LIMIT 50
```

**Find screens with most elements (complexity):**
```cypher
MATCH (s:Screen)-[:HAS_ELEMENT]->(e:Element)
WHERE s.packageName = "com.microsoft.teams"
RETURN s.activityName, count(e) as elementCount
ORDER BY elementCount DESC
LIMIT 10
```

**Find navigation hubs (most connections):**
```cypher
MATCH (s:Screen)
WHERE s.packageName = "com.microsoft.teams"
RETURN s.activityName,
       size((s)-[:NAVIGATES_TO]->()) as outgoing,
       size((s)<-[:NAVIGATES_TO]-()) as incoming,
       size((s)-[:NAVIGATES_TO]->()) + size((s)<-[:NAVIGATES_TO]-()) as total
ORDER BY total DESC
LIMIT 10
```

**Find elements with specific voice commands:**
```cypher
MATCH (e:Element)
WHERE e.voiceCommand CONTAINS "chat"
RETURN e.voiceCommand, e.className, e.screenHash
ORDER BY e.voiceCommand
```

**Shortest path between two screens:**
```cypher
MATCH path = shortestPath(
    (start:Screen {activityName: "MainActivity"})-[:NAVIGATES_TO*]->(end:Screen {activityName: "SettingsActivity"})
)
RETURN path
```

---

# 7. Voice Commands Reference

## 7.1 Main Navigation

| Command | Action |
|---------|--------|
| "click activity" | Switch to Activity tab |
| "click chat" | Switch to Chat tab |
| "click teams" | Switch to Teams tab |
| "click calendar" | Switch to Calendar tab |
| "click calls" | Switch to Calls tab |
| "click search" | Focus search field |
| "click menu" | Open navigation drawer |

## 7.2 Chat Commands

| Command | Action |
|---------|--------|
| "click new chat" | Start new chat |
| "click {contact name}" | Open chat with contact |
| "click send" | Send message |
| "click attach" | Open attachment picker |
| "click emoji" | Open emoji picker |
| "click camera" | Open camera |
| "click voice message" | Start voice recording |
| "click call" | Start audio call |
| "click video" | Start video call |
| "click back" | Return to chat list |

## 7.3 Teams Commands

| Command | Action |
|---------|--------|
| "click {team name}" | Expand/open team |
| "click {channel name}" | Open channel |
| "click join team" | Join or create team |
| "click new post" | Create new post |
| "click reply" | Reply to thread |

## 7.4 Calendar Commands

| Command | Action |
|---------|--------|
| "click {meeting name}" | Open meeting details |
| "click join" | Join meeting |
| "click new meeting" | Create new meeting |
| "click today" | Go to today |
| "click next" | Next day/week |
| "click previous" | Previous day/week |

---

# 8. Testing Checklist

## 8.1 Pre-Exploration

- [ ] VoiceOS accessibility service enabled
- [ ] LearnAppPro installed
- [ ] MS Teams installed and logged in
- [ ] Neo4j running (for graph export)

## 8.2 During Exploration

- [ ] Progress bar advances
- [ ] Screen count increases
- [ ] Element count increases
- [ ] No crashes or ANRs
- [ ] Login screen detected (if applicable)
- [ ] Menus properly captured and dismissed
- [ ] Scroll content discovered

## 8.3 Post-Exploration

- [ ] Statistics match expected ranges
- [ ] All 5 main tabs explored
- [ ] At least 3 chat conversations explored
- [ ] At least 2 team channels explored
- [ ] Settings screen explored
- [ ] Calendar view explored

## 8.4 Voice Command Testing

- [ ] "click chat" works
- [ ] "click teams" works
- [ ] "click {contact}" opens correct conversation
- [ ] "click send" triggers send action
- [ ] "click back" navigates back

## 8.5 Neo4j Verification

- [ ] Export completes without error
- [ ] Screen nodes visible in Neo4j
- [ ] Element nodes with voice commands
- [ ] Navigation relationships present
- [ ] Graph queries return expected results

---

# Document History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-11 | Initial release for review |

---

**End of MS Teams Exploration Guide**
