# Chapter 4: Features & Functions

**Document:** VoiceOS-AvaLearnLite-UserManual-Ch04
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 4.1 Feature Overview

AvaLearnLite provides three main feature areas:

| Feature Area | Purpose |
|--------------|---------|
| **JIT Monitoring** | View and control background learning |
| **App Exploration** | Actively explore and learn apps |
| **Data Export** | Save learned data for VoiceOS |

---

## 4.2 JIT Learning Control

### 4.2.1 What is JIT Learning?

JIT (Just-In-Time) Learning is VoiceOS's background learning system. It continuously observes apps you use and learns their interfaces without requiring any special action from you.

### 4.2.2 Viewing JIT Status

The JIT Learning Status card shows:

| Statistic | Description | Example |
|-----------|-------------|---------|
| **Status** | Active or Paused | Active (green) |
| **Screens Learned** | Total unique screens observed | 42 |
| **Elements Discovered** | Total interactive elements found | 187 |
| **Current Package** | App currently being monitored | com.example.app |

### 4.2.3 Pausing JIT

**When to pause:**
- Before starting manual exploration
- When using sensitive apps
- To improve device performance

**How to pause:**
1. Locate the JIT Learning Status card
2. Press the **Pause** button
3. Status badge changes to orange "Paused"

### 4.2.4 Resuming JIT

**How to resume:**
1. Press the **Resume** button
2. Status badge changes to green "Active"
3. JIT begins learning again

### 4.2.5 Refreshing Statistics

Press the **Refresh** button (circular arrow icon) to:
- Update displayed statistics
- Verify service connection
- See real-time counts

---

## 4.3 App Exploration

### 4.3.1 What is App Exploration?

App Exploration is the active learning mode where you systematically explore an app to teach VoiceOS all its screens and interactive elements.

### 4.3.2 Starting Exploration

**Prerequisites:**
1. VoiceOS service is running (JIT shows "Active")
2. Target app is installed
3. Target app is open or ready to open

**Steps:**
1. Open AvaLearnLite
2. Open the app you want to explore
3. Return to AvaLearnLite
4. Press **Start Exploration**
5. Phase changes to "EXPLORING" (blue)

### 4.3.3 During Exploration

While exploring, AvaLearnLite tracks:

| Metric | What It Means |
|--------|---------------|
| **Screens** | Unique screens you've visited |
| **Elements** | Interactive items found |
| **Clicked** | Elements that were tested |
| **Coverage** | Percentage of elements tested |

**Best practices during exploration:**
- Navigate to all screens in the app
- Tap buttons and interactive elements
- Open menus and dropdowns
- Scroll to reveal hidden content
- Use all main features of the app

### 4.3.4 Exploration Phases

| Phase | Description | What to Do |
|-------|-------------|------------|
| **IDLE** | Ready to start | Press Start Exploration |
| **EXPLORING** | Actively learning | Use the target app normally |
| **WAITING_USER** | Paused for your action | Complete required action, then continue |
| **COMPLETED** | Exploration finished | Export your data |
| **ERROR** | Something went wrong | Check troubleshooting guide |

### 4.3.5 Stopping Exploration

**When exploration stops automatically:**
- Login screen detected
- Safety limit reached
- Service disconnection

**To stop manually:**
1. Press **Stop Exploration**
2. Phase changes to "IDLE"
3. Statistics are preserved for export

### 4.3.6 Exploration Tips

**Do:**
- Explore one app at a time
- Visit all major screens
- Interact with all visible elements
- Let the system pause at login screens
- Export data when finished

**Don't:**
- Navigate away from the target app rapidly
- Close the target app during exploration
- Force stop VoiceOS service
- Ignore safety warnings

---

## 4.4 Safety Systems

### 4.4.1 Overview

AvaLearnLite includes multiple safety systems to protect your device and accounts:

| System | Protection |
|--------|------------|
| **DoNotClick** | Avoids dangerous buttons |
| **Login Detection** | Stops at authentication screens |
| **Dynamic Region Detection** | Identifies changing content |
| **Loop Prevention** | Avoids repetitive exploration |

### 4.4.2 DoNotClick Protection

The DoNotClick system automatically avoids:

| Category | Examples |
|----------|----------|
| **Destructive** | Delete, Remove, Clear all |
| **Financial** | Pay, Purchase, Buy now |
| **Account** | Logout, Sign out, Deactivate |
| **System** | Factory reset, Uninstall |
| **Dangerous** | Format, Wipe, Erase |

**The Safety Status card shows:**
- **DNC Skipped**: Number of dangerous elements avoided

### 4.4.3 Login Screen Detection

When a login screen is detected:

1. Warning banner appears (orange background)
2. Exploration may pause automatically
3. Login type is displayed (PASSWORD, BIOMETRIC, etc.)

**Why this matters:**
- Protects your credentials
- Prevents accidental password exposure
- Ensures privacy during learning

### 4.4.4 Dynamic Region Detection

Dynamic regions are areas that change frequently:

| Type | Example |
|------|---------|
| **Time displays** | Clocks, timers |
| **Counters** | Notification badges |
| **Live content** | News feeds, chat messages |
| **Animations** | Loading spinners |

AvaLearnLite identifies these to avoid:
- Learning content that changes
- Creating invalid commands
- Wasting exploration time

### 4.4.5 Menu Discovery

The system automatically detects:
- Dropdown menus
- Overflow menus (three dots)
- Context menus
- Navigation drawers

**Menus Found** counter shows discovered menus.

---

## 4.5 Data Export

### 4.5.1 What is AVU Export?

AVU (Avanues Universal) is VoiceOS's data format for storing learned app information. The `.vos` file extension is used for VoiceOS command files.

### 4.5.2 What Gets Exported

| Data Type | Description |
|-----------|-------------|
| **App info** | Package name, app name |
| **Screens** | All discovered screens |
| **Elements** | Interactive elements per screen |
| **Commands** | Generated voice commands |
| **Synonyms** | Alternative command phrases |

### 4.5.3 How to Export

**Prerequisites:**
- At least one exploration completed
- Elements discovered > 0
- Storage permission granted

**Steps:**
1. Complete an exploration session
2. Locate the Export card
3. Press **Export to AVU (.vos)**
4. Wait for export to complete
5. Success message appears

### 4.5.4 Export Location

Files are saved to:
```
/storage/emulated/0/Android/data/com.augmentalis.avalearnlite/files/learned_apps/
```

**File naming:**
```
{package_name}.vos
```

Example: `com.example.email.vos`

### 4.5.5 What Happens After Export

1. File is written to learned_apps folder
2. VoiceOS CommandManager watches this folder
3. File is automatically detected
4. Commands are imported into VoiceOS
5. Voice control is enabled for that app

### 4.5.6 Re-exporting

You can export again to:
- Update commands after more exploration
- Fix issues with previous export
- Add newly discovered elements

The new export overwrites the previous file for the same app.

---

## 4.6 Command Generation

### 4.6.1 How Commands Are Created

AvaLearnLite generates voice commands from discovered elements:

| Element | Generated Command | Example |
|---------|-------------------|---------|
| Button with text | "Click {text}" | "Click Login" |
| Text field | "Enter {label}" | "Enter username" |
| Checkbox | "Toggle {text}" | "Toggle remember me" |
| Menu item | "Select {text}" | "Select Settings" |

### 4.6.2 Synonym Generation

Each command gets synonyms for natural language:

| Primary | Synonyms |
|---------|----------|
| login | sign in, log in |
| settings | preferences, options |
| search | find, look for |
| close | dismiss, exit |

### 4.6.3 Command Confidence

Each command has a confidence score (0-1):

| Score | Meaning |
|-------|---------|
| 0.90+ | High confidence, element clearly labeled |
| 0.70-0.89 | Medium confidence, label inferred |
| 0.50-0.69 | Low confidence, may need manual verification |

---

## 4.7 Statistics Explained

### 4.7.1 JIT Statistics

| Stat | Description | Updates |
|------|-------------|---------|
| Screens Learned | Total unique screens across all apps | Continuously |
| Elements Discovered | Total elements from JIT capture | Continuously |
| Current Package | Currently active app package | On app switch |

### 4.7.2 Exploration Statistics

| Stat | Description | Updates |
|------|-------------|---------|
| Screens | Screens visited this session | On navigation |
| Elements | Elements found this session | On screen load |
| Clicked | Elements tested | On interaction |
| Coverage | Clicked/Elements * 100 | On interaction |

### 4.7.3 Safety Statistics

| Stat | Description | Ideal Value |
|------|-------------|-------------|
| DNC Skipped | Dangerous elements avoided | > 0 means protection active |
| Dynamic Regions | Areas with changing content | Any number |
| Menus Found | Discovered menu structures | Any number |

---

## 4.8 Workflow Examples

### 4.8.1 Learning a New App

```
1. Install the app you want to learn
2. Open AvaLearnLite
3. Verify JIT Status is "Active"
4. Open the target app
5. Press "Start Exploration" in AvaLearnLite
6. Use the app - visit all screens
7. Open all menus and tap elements
8. Return to AvaLearnLite when done
9. Press "Stop Exploration"
10. Press "Export to AVU"
11. Done! VoiceOS can now control the app
```

### 4.8.2 Updating a Previously Learned App

```
1. Open the app that was updated
2. Start exploration in AvaLearnLite
3. Navigate to new/changed screens
4. Interact with new elements
5. Stop and export
6. New commands are merged with existing
```

### 4.8.3 Troubleshooting a Learned App

```
1. If voice commands don't work:
2. Open AvaLearnLite
3. Start new exploration
4. Focus on the problem screen/element
5. Re-export data
6. Test voice commands again
```

---

## 4.9 Limitations

### 4.9.1 What AvaLearnLite Cannot Learn

| Limitation | Reason |
|------------|--------|
| Games with custom rendering | No accessibility tree |
| Apps with WebViews | Limited accessibility |
| Heavily animated UIs | Elements move too fast |
| Canvas-based UIs | No individual elements |

### 4.9.2 Performance Considerations

| Factor | Impact |
|--------|--------|
| Large apps | More screens = longer exploration |
| Complex UIs | More elements = more commands |
| System resources | JIT uses background processing |

---

## 4.10 Next Steps

Learn about the safety systems in detail in [Chapter 5: Safety Systems](./05-Safety-Systems.md).

---

**End of Chapter 4**
