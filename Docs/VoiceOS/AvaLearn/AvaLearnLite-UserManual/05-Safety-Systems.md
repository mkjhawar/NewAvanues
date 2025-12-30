# Chapter 5: Safety Systems

**Document:** VoiceOS-AvaLearnLite-UserManual-Ch05
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 5.1 Safety Overview

AvaLearnLite includes comprehensive safety systems to protect your device, data, and accounts during app exploration. These systems operate automatically without requiring user intervention.

### Safety Architecture

```
+------------------------------------------------------------------+
|                     AvaLearnLite Safety Layer                     |
+------------------------------------------------------------------+
|                                                                   |
|  +------------------+  +------------------+  +------------------+ |
|  | DoNotClick       |  | Login Detection  |  | Loop Prevention  | |
|  | Manager          |  | System           |  | System           | |
|  +------------------+  +------------------+  +------------------+ |
|                                                                   |
|  +------------------+  +------------------+  +------------------+ |
|  | Dynamic Region   |  | Menu Discovery   |  | Scroll Safety    | |
|  | Detector         |  | System           |  | Limits           | |
|  +------------------+  +------------------+  +------------------+ |
|                                                                   |
+------------------------------------------------------------------+
```

---

## 5.2 DoNotClick (DNC) Protection

### 5.2.1 Purpose

The DoNotClick system prevents AvaLearnLite from interacting with potentially dangerous UI elements that could:
- Delete important data
- Make unwanted purchases
- Log you out of accounts
- Modify system settings
- Perform irreversible actions

### 5.2.2 Protected Categories

| Category | Keywords Detected | Risk Level |
|----------|-------------------|------------|
| **Destructive** | delete, remove, erase, clear, wipe | Critical |
| **Financial** | pay, purchase, buy, subscribe, checkout | Critical |
| **Account** | logout, sign out, deactivate, close account | High |
| **System** | uninstall, factory reset, format | Critical |
| **Confirmation** | confirm delete, yes delete, permanently | Critical |

### 5.2.3 How DNC Works

```
Element Discovered
       |
       v
+------------------+
| Text Analysis    |
| Check keywords   |
+------------------+
       |
       v
+------------------+
| Pattern Matching |
| Check categories |
+------------------+
       |
       v
+------------------+    Yes    +------------------+
| Is Dangerous?    +---------->| Skip Element     |
+------------------+           | Increment Counter|
       |                       +------------------+
       | No
       v
+------------------+
| Allow Interaction|
+------------------+
```

### 5.2.4 DNC Detection Rules

| Rule | Pattern | Example Match |
|------|---------|---------------|
| Text contains keyword | text.contains("delete") | "Delete message" |
| Content description | desc.contains("remove") | "Remove item" |
| Resource ID | id.contains("btn_pay") | "btn_pay_now" |
| Combined signals | multiple matches | Higher confidence |

### 5.2.5 Viewing DNC Activity

The Safety Status card displays:
- **DNC Skipped**: Count of dangerous elements avoided

| Color | Meaning |
|-------|---------|
| Green (0) | No dangerous elements found yet |
| Orange (> 0) | Protection active, elements skipped |

### 5.2.6 DNC Keyword Lists

**Destructive Actions:**
```
delete, remove, erase, clear, wipe, destroy, discard,
trash, eliminate, purge, empty, reset
```

**Financial Actions:**
```
pay, purchase, buy, subscribe, checkout, order, confirm payment,
add to cart, proceed to payment, complete purchase
```

**Account Actions:**
```
logout, log out, sign out, signout, deactivate, disable account,
close account, delete account, remove account
```

**System Actions:**
```
uninstall, factory reset, format, wipe device, reset all,
clear all data, restore defaults
```

---

## 5.3 Login Screen Detection

### 5.3.1 Purpose

Login detection protects your credentials by:
- Identifying authentication screens
- Pausing exploration automatically
- Warning you before proceeding
- Preventing password field interaction

### 5.3.2 Detection Signals

| Signal | Weight | Example |
|--------|--------|---------|
| Password field | High | Input type = password |
| Login button | Medium | Text = "Login", "Sign in" |
| Username field | Medium | Hint = "username", "email" |
| Biometric prompt | High | Fingerprint dialog |
| Screen title | Low | "Sign In", "Authentication" |

### 5.3.3 Login Types

| Type | Description | UI Indicator |
|------|-------------|--------------|
| **PASSWORD** | Traditional password entry | Orange warning |
| **BIOMETRIC** | Fingerprint/Face recognition | Orange warning |
| **PIN** | Numeric PIN entry | Orange warning |
| **PATTERN** | Pattern lock | Orange warning |
| **TWO_FACTOR** | 2FA code entry | Orange warning |

### 5.3.4 Warning Banner

When login is detected:

```
+----------------------------------------------------+
| ! Login Screen Detected                            |
|   Type: PASSWORD                                   |
+----------------------------------------------------+
```

| Element | Description |
|---------|-------------|
| Icon | Warning symbol (!) |
| Title | "Login Screen Detected" |
| Type | The detected authentication type |
| Background | Orange with 15% opacity |

### 5.3.5 User Actions at Login

When a login screen is detected:

| Action | Result |
|--------|--------|
| **Continue exploring** | Skip login elements, continue with safe elements |
| **Stop exploration** | End session, preserve data collected so far |
| **Log in manually** | Enter credentials yourself, then continue |

### 5.3.6 What Gets Protected

| Protected | Not Protected |
|-----------|---------------|
| Password fields | Navigation buttons |
| PIN inputs | Back button |
| Biometric prompts | Cancel button |
| Security questions | App logo/images |

---

## 5.4 Dynamic Region Detection

### 5.4.1 Purpose

Dynamic regions are areas of the screen where content changes frequently. Detecting these prevents:
- Learning constantly changing elements
- Creating commands that become invalid
- Wasting exploration on non-actionable content

### 5.4.2 Types of Dynamic Content

| Type | Examples | Detection Method |
|------|----------|------------------|
| **Time-based** | Clocks, timers, countdowns | Content changes every second |
| **Live feeds** | Chat messages, notifications | Rapid content updates |
| **Animations** | Loading spinners, progress bars | Continuous state changes |
| **Counters** | Badge counts, unread indicators | Frequent value changes |

### 5.4.3 Detection Algorithm

```
1. Capture screen state at T0
2. Wait 500ms
3. Capture screen state at T1
4. Compare element trees
5. Mark changed regions as dynamic
6. Exclude from command generation
```

### 5.4.4 Dynamic Region Indicators

The Safety Status card shows:
- **Dynamic Regions**: Count of detected areas

| Count | Meaning |
|-------|---------|
| 0 | Static screen, all elements learnable |
| 1-3 | Some dynamic content detected |
| 4+ | Highly dynamic screen (news feed, chat) |

### 5.4.5 How Dynamic Regions Affect Learning

| Element Type | In Dynamic Region | Action |
|--------------|-------------------|--------|
| Button | No | Learn normally |
| Button | Yes | Skip or mark uncertain |
| Text | No | Use for command names |
| Text | Yes | Ignore for naming |

---

## 5.5 Loop Prevention

### 5.5.1 Purpose

Loop prevention stops the exploration from:
- Visiting the same screen repeatedly
- Getting stuck in navigation cycles
- Wasting time on redundant exploration

### 5.5.2 How Loops Are Detected

Each screen has a unique "hash" (fingerprint). The system tracks:

| Metric | Threshold | Action |
|--------|-----------|--------|
| Visit count per screen | > 3 | Warning issued |
| Visit count per screen | > 5 | Exploration paused |
| Rapid screen switches | > 10 in 30 sec | Cooldown triggered |

### 5.5.3 Loop Types

| Type | Pattern | Cause |
|------|---------|-------|
| **Direct loop** | A → A | Element navigates to same screen |
| **Cycle loop** | A → B → A | Back navigation detected |
| **Long cycle** | A → B → C → A | Complex navigation path |

### 5.5.4 Loop Resolution

When a loop is detected:

1. Current screen is marked "visited"
2. Exploration continues with unvisited elements
3. If all elements visited, moves to next screen
4. If no new screens available, exploration completes

---

## 5.6 Menu Discovery System

### 5.6.1 Purpose

Menu discovery identifies:
- Dropdown menus
- Overflow menus (⋮)
- Context menus
- Navigation drawers

### 5.6.2 Menu Types Detected

| Type | Trigger | Example |
|------|---------|---------|
| **Overflow** | Three dots icon | Android action bar menu |
| **Dropdown** | Click on element | Select/spinner |
| **Context** | Long press | Right-click menu |
| **Navigation** | Swipe or hamburger | Side drawer |

### 5.6.3 Menu Statistics

The Safety Status card shows:
- **Menus Found**: Count of discovered menus

### 5.6.4 How Menus Are Explored

```
1. Menu trigger identified
2. Menu opened
3. Visible items captured
4. Total items estimated
5. Menu closed
6. Items converted to commands
```

---

## 5.7 Scroll Safety

### 5.7.1 Purpose

Scroll safety limits prevent:
- Infinite scrolling (social media feeds)
- Missing content due to rapid scrolling
- Performance issues from too much data

### 5.7.2 Scroll Limits

| Setting | Value | Purpose |
|---------|-------|---------|
| Max scroll depth | 10 scrolls per screen | Prevents infinite scroll |
| Scroll delay | 500ms between scrolls | Allows content to load |
| New element threshold | Must find > 0 new | Stops if no new content |

### 5.7.3 Scroll Direction Support

| Direction | Supported | Common Use |
|-----------|-----------|------------|
| Down | Yes | Lists, feeds |
| Up | Yes | Return to top |
| Left | Yes | Horizontal carousels |
| Right | Yes | Horizontal carousels |

---

## 5.8 Safety Configuration

### 5.8.1 Default Settings

AvaLearnLite uses sensible defaults that cannot be changed in the user edition:

| Setting | Value | Reason |
|---------|-------|--------|
| DNC enabled | Always | Protect against data loss |
| Login detection | Always | Protect credentials |
| Loop prevention | Always | Prevent stuck states |
| Dynamic detection | Always | Improve accuracy |

### 5.8.2 Safety Cannot Be Disabled

Unlike the developer edition (AvaLearnPro), the user edition does not allow:
- Disabling DoNotClick
- Ignoring login warnings
- Bypassing loop prevention

This ensures user safety at all times.

---

## 5.9 Safety Event Log

### 5.9.1 What Gets Logged

| Event | Logged | Visible in UI |
|-------|--------|---------------|
| DNC element skipped | Yes | Counter only |
| Login detected | Yes | Warning banner |
| Loop detected | Yes | No (internal) |
| Dynamic region found | Yes | Counter only |

### 5.9.2 Log Retention

Safety logs are:
- Stored in memory during session
- Cleared when app closes
- Not persisted to storage
- Not exported in AVU files

---

## 5.10 Safety Best Practices

### 5.10.1 For Safe Exploration

| Do | Don't |
|----|-------|
| Let safety systems work | Try to bypass warnings |
| Pause at login screens | Enter credentials during exploration |
| Review skipped elements | Assume all elements are safe |
| Export after completion | Export during active exploration |

### 5.10.2 Understanding Safety Counts

| High DNC Count | Meaning |
|----------------|---------|
| Many skipped | App has many dangerous actions |
| Protection working | System keeping you safe |
| Normal for settings apps | Expected behavior |

| High Dynamic Count | Meaning |
|--------------------|---------|
| Content heavy app | News, social, chat apps |
| Fewer learnable elements | Commands may be limited |
| Consider static screens | Navigate to settings/menus |

---

## 5.11 Next Steps

Learn about exporting your data in [Chapter 6: Export & Data Management](./06-Export-Data.md).

---

**End of Chapter 5**
