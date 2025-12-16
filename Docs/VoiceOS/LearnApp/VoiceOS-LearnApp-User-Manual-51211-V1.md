# LearnApp User Manual

**Document ID:** VoiceOS-LearnApp-User-Manual-51211-V1
**Version:** 1.0
**Created:** 2025-12-11
**Audience:** End Users

---

## Introduction

LearnApp is a tool that helps VoiceOS learn how to control third-party apps on your device. By exploring apps with LearnApp, you teach VoiceOS what elements exist on each screen and how to interact with them.

---

## Getting Started

### Installation

LearnApp is installed alongside VoiceOS. You can find it in your app drawer as "LearnApp Explorer."

### Requirements

- VoiceOS Core service must be running
- Accessibility service must be enabled for VoiceOS
- Target app to explore must be installed

---

## User Interface

### Main Screen

The LearnApp interface consists of four main sections:

```
+--------------------------------------------------+
|               LearnApp Explorer                  |
+--------------------------------------------------+
|                                                  |
|  +--------------------------------------------+  |
|  |         JIT Learning Status                |  |  <- Section 1
|  +--------------------------------------------+  |
|                                                  |
|  +--------------------------------------------+  |
|  |           App Exploration                  |  |  <- Section 2
|  +--------------------------------------------+  |
|                                                  |
|  +--------------------------------------------+  |
|  |           Safety Status                    |  |  <- Section 3
|  +--------------------------------------------+  |
|                                                  |
|  +--------------------------------------------+  |
|  |              Export                        |  |  <- Section 4
|  +--------------------------------------------+  |
|                                                  |
+--------------------------------------------------+
```

---

## Section Details

### 1. JIT Learning Status

Shows the background learning status:

| Field | Description |
|-------|-------------|
| **Status** | Active (green) or Paused (orange) |
| **Screens Learned** | Number of screens VoiceOS has learned |
| **Elements Discovered** | Total interactive elements found |
| **Current Package** | App currently being monitored |

**Controls:**
- **Pause** - Stop background learning temporarily
- **Resume** - Continue background learning
- **Refresh** - Update the status display

### 2. App Exploration

Manual exploration controls:

| Field | Description |
|-------|-------------|
| **Phase** | Current exploration state |
| **Screens** | Number of screens explored this session |
| **Elements** | Elements discovered this session |
| **Clicked** | Elements tested by clicking |
| **Coverage** | Percentage of screen explored |

**Phases:**
- **IDLE** - Ready to start
- **EXPLORING** - Actively exploring
- **WAITING_USER** - Needs user input (e.g., login screen)
- **COMPLETED** - Exploration finished
- **ERROR** - Something went wrong

**Button:**
- **Start Exploration** - Begin exploring the current app
- **Stop Exploration** - End the current session

### 3. Safety Status

Shows safety system activity:

| Indicator | Meaning |
|-----------|---------|
| **DNC Skipped** | Dangerous elements avoided (logout, delete, etc.) |
| **Dynamic Regions** | Areas with changing content detected |
| **Menus Found** | Dropdown/overflow menus discovered |

**Warning Banner:**
If a login screen is detected, you'll see an orange warning. This means LearnApp has paused to avoid clicking login buttons.

### 4. Export

Save learned data:

| Field | Description |
|-------|-------------|
| **Last Export** | Filename of the last exported file |

**Button:**
- **Export to AVU** - Save the exploration data

---

## How to Explore an App

### Step-by-Step Guide

1. **Open the target app** you want to teach VoiceOS about
2. **Open LearnApp** from your app drawer
3. **Check JIT Status** - should show "Active"
4. **Press "Start Exploration"**
5. **Switch to the target app** and use it normally
6. LearnApp will learn from your interactions
7. **Return to LearnApp** and press "Stop Exploration"
8. **Press "Export to AVU"** to save the data

### Tips for Better Results

| Tip | Why |
|-----|-----|
| Explore all screens | VoiceOS needs to see each screen |
| Open all menus | Dropdown items need to be visible |
| Scroll through lists | Hidden elements won't be learned |
| Wait for content to load | Dynamic content needs time |
| Avoid login screens | Let LearnApp pause when it detects them |

---

## Safety Features

LearnApp includes automatic safety features to prevent problems:

### Do-Not-Click (DNC) Protection

LearnApp automatically avoids clicking dangerous elements:

| Protected Elements | Why |
|--------------------|-----|
| Logout buttons | Prevents accidental sign-out |
| Delete buttons | Prevents data loss |
| Cancel buttons | Preserves your work |
| Payment buttons | Prevents accidental purchases |
| Uninstall options | Protects apps |

### Login Screen Detection

When LearnApp detects a login screen, it:
1. Pauses exploration
2. Shows a warning banner
3. Waits for you to log in manually
4. Resumes after you navigate away

### Loop Prevention

If you visit the same screen multiple times, LearnApp detects this and may pause to prevent infinite loops.

---

## Troubleshooting

### Common Issues

| Problem | Solution |
|---------|----------|
| JIT Status shows "Paused" | Press "Resume" button |
| "Not connected" error | Restart VoiceOS service |
| Export fails | Check storage permissions |
| Exploration stuck | Press "Stop" and start again |

### Error Messages

| Error | Meaning | Action |
|-------|---------|--------|
| "Service not bound" | VoiceOS not running | Restart VoiceOS |
| "No elements found" | Empty screen | Navigate to a screen with content |
| "Export failed" | Storage issue | Free up space or check permissions |

---

## Data Privacy

### What LearnApp Collects

- Screen layout information (element positions, types)
- Element labels and IDs
- Navigation patterns between screens

### What LearnApp Does NOT Collect

- Personal data entered into apps
- Passwords or credentials
- Photos, messages, or files
- Location data

### Where Data is Stored

Exported files are saved to:
```
/storage/emulated/0/Android/data/com.augmentalis.learnapp/files/learned_apps/
```

---

## Glossary

| Term | Definition |
|------|------------|
| **JIT** | Just-In-Time learning - background learning system |
| **AVU** | Avanues Universal format - export file format |
| **DNC** | Do-Not-Click - safety system for dangerous elements |
| **Element** | A button, text field, or other interactive item |
| **Screen** | A unique view in an app |
| **Exploration** | The process of discovering elements |

---

## Support

For help with LearnApp:
- Check the VoiceOS help documentation
- Visit the VoiceOS community forum
- Contact support@augmentalis.com

---

**End of User Manual**
