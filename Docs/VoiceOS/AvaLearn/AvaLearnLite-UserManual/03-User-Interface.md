# Chapter 3: User Interface Guide

**Document:** VoiceOS-AvaLearnLite-UserManual-Ch03
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 3.1 Interface Overview

AvaLearnLite uses the Ocean Blue XR theme - a clean, accessible design optimized for clarity and ease of use.

### Main Screen Layout

```
+--------------------------------------------------+
|                                                  |
|  ============ AvaLearnLite Explorer =========== |  <- Top App Bar
|                                                  |
+--------------------------------------------------+
|                                                  |
|  +--------------------------------------------+  |
|  |         JIT Learning Status                |  |  <- Card 1
|  +--------------------------------------------+  |
|                                                  |
|  +--------------------------------------------+  |
|  |           App Exploration                  |  |  <- Card 2
|  +--------------------------------------------+  |
|                                                  |
|  +--------------------------------------------+  |
|  |           Safety Status                    |  |  <- Card 3
|  +--------------------------------------------+  |
|                                                  |
|  +--------------------------------------------+  |
|  |              Export                        |  |  <- Card 4
|  +--------------------------------------------+  |
|                                                  |
+--------------------------------------------------+
```

---

## 3.2 Top App Bar

### Description

The top bar displays the app name and uses the Ocean Blue primary color.

```
+--------------------------------------------------+
|  ============ AvaLearnLite Explorer =========== |
+--------------------------------------------------+
```

### Elements

| Element | Description |
|---------|-------------|
| Title | "AvaLearnLite Explorer" |
| Background | Ocean Blue primary container (#DBEAFE) |

---

## 3.3 JIT Learning Status Card

### Purpose

Shows the background learning system status and statistics.

### Visual Layout

```
+----------------------------------------------------+
| JIT Learning Status                    [*] Active  |
|----------------------------------------------------|
| Screens Learned                              42    |
| Elements Discovered                         187    |
| Current Package                  com.example.app   |
|----------------------------------------------------|
|  [ Pause ]        [ Resume ]            [Refresh]  |
+----------------------------------------------------+
```

### Elements Explained

| Element | Type | Description |
|---------|------|-------------|
| **Title** | Text | "JIT Learning Status" |
| **Status Badge** | Badge | "Active" (green) or "Paused" (orange) |
| **Screens Learned** | Stat | Total screens JIT has learned |
| **Elements Discovered** | Stat | Total elements found by JIT |
| **Current Package** | Text | Package name of monitored app |
| **Pause Button** | Button | Pauses JIT capture |
| **Resume Button** | Button | Resumes JIT capture |
| **Refresh Button** | Icon | Updates displayed statistics |

### Status Badge Colors

| Status | Color | Hex Code | Meaning |
|--------|-------|----------|---------|
| Active | Green | #10B981 | JIT is running normally |
| Paused | Orange | #F59E0B | JIT is paused |

### Button States

| Button | Enabled When | Action |
|--------|--------------|--------|
| Pause | JIT is Active | Stops JIT capture |
| Resume | JIT is Paused | Starts JIT capture |
| Refresh | Always | Updates UI from service |

---

## 3.4 App Exploration Card

### Purpose

Controls manual app exploration and shows exploration progress.

### Visual Layout

```
+----------------------------------------------------+
| App Exploration                            [IDLE]  |
|----------------------------------------------------|
|                                                    |
|    +------+   +------+   +------+   +------+      |
|    |  5   |   |  23  |   |  12  |   | 50%  |      |
|    |Screens|  |Elem  |   |Click |   |Cover |      |
|    +------+   +------+   +------+   +------+      |
|                                                    |
|----------------------------------------------------|
|           [ Start Exploration ]                    |
+----------------------------------------------------+
```

### Elements Explained

| Element | Type | Description |
|---------|------|-------------|
| **Title** | Text | "App Exploration" |
| **Phase Badge** | Badge | Current exploration state |
| **Screens** | Stat Box | Screens explored this session |
| **Elements** | Stat Box | Elements discovered this session |
| **Clicked** | Stat Box | Elements that were clicked |
| **Coverage** | Stat Box | Percentage of screen explored |
| **Action Button** | Button | Start or Stop exploration |

### Phase States

| Phase | Badge Color | Description |
|-------|-------------|-------------|
| IDLE | Gray (#6B7280) | Ready to start, not exploring |
| EXPLORING | Blue (#3B82F6) | Currently exploring |
| WAITING_USER | Orange (#F59E0B) | Paused for user action |
| COMPLETED | Green (#10B981) | Exploration finished |
| ERROR | Red (#EF4444) | Something went wrong |

### Card Background

| State | Background |
|-------|------------|
| Not exploring | Surface variant |
| Exploring | Primary container (blue tint) |

### Statistics Explained

| Stat | Description | Example |
|------|-------------|---------|
| **Screens** | Unique screens visited | 5 |
| **Elements** | Total interactive elements found | 23 |
| **Clicked** | Elements tested via click | 12 |
| **Coverage** | (Clicked / Elements) * 100 | 52% |

---

## 3.5 Safety Status Card

### Purpose

Shows activity of safety protection systems.

### Visual Layout

```
+----------------------------------------------------+
| Safety Status                                      |
|----------------------------------------------------|
|                                                    |
|  +----------+   +----------+   +----------+        |
|  |    3     |   |    2     |   |    4     |        |
|  |   DNC    |   | Dynamic  |   |  Menus   |        |
|  | Skipped  |   | Regions  |   |  Found   |        |
|  +----------+   +----------+   +----------+        |
|                                                    |
|----------------------------------------------------|
| ! Login Screen Detected                            |
|   Type: PASSWORD                                   |
+----------------------------------------------------+
```

### Elements Explained

| Element | Type | Description |
|---------|------|-------------|
| **Title** | Text | "Safety Status" |
| **DNC Skipped** | Stat Box | Dangerous elements avoided |
| **Dynamic Regions** | Stat Box | Areas with changing content |
| **Menus Found** | Stat Box | Dropdown/overflow menus |
| **Warning Banner** | Alert | Login screen warning (when detected) |

### Stat Box Colors

| Stat | Color When > 0 | Color When 0 |
|------|----------------|--------------|
| DNC Skipped | Orange (#F59E0B) | Green (#10B981) |
| Dynamic Regions | Blue (#3B82F6) | Gray (#6B7280) |
| Menus Found | Cyan (#06B6D4) | Gray (#6B7280) |

### Warning Banner

Appears when a login screen is detected:

```
+----------------------------------------------------+
| ! Login Screen Detected                            |
|   Type: PASSWORD                                   |
+----------------------------------------------------+
```

| Banner Element | Value |
|----------------|-------|
| Background | Orange 15% opacity |
| Icon | "!" |
| Title | "Login Screen Detected" |
| Subtitle | Login type (PASSWORD, BIOMETRIC, etc.) |

---

## 3.6 Export Card

### Purpose

Allows saving exploration data to a file.

### Visual Layout

```
+----------------------------------------------------+
| Export                                             |
|----------------------------------------------------|
| Last export: com.example.app.vos                   |
|                                                    |
|           [ Export to AVU (.vos) ]                 |
|                                                    |
| Explore an app first to enable export              |
+----------------------------------------------------+
```

### Elements Explained

| Element | Type | Description |
|---------|------|-------------|
| **Title** | Text | "Export" |
| **Last Export** | Text | Filename of last exported file |
| **Export Button** | Button | Triggers AVU export |
| **Hint Text** | Text | Shows when export disabled |

### Button States

| State | Enabled | Button Color | Reason |
|-------|---------|--------------|--------|
| Elements discovered | Yes | Primary blue | Can export |
| No elements | No | Gray | Nothing to export |

---

## 3.7 Theme Colors Reference

### Ocean Blue Light Theme (AvaLearnLite)

| Token | Color | Hex | Usage |
|-------|-------|-----|-------|
| Primary | Ocean Blue | #3B82F6 | Buttons, accents |
| Primary Container | Light Blue | #DBEAFE | Card backgrounds |
| Secondary | Cyan | #06B6D4 | Secondary actions |
| Surface | Off-white | #F0F9FF | Main background |
| Surface Variant | Light blue | #E0F2FE | Card backgrounds |
| Success | Green | #10B981 | Active status |
| Warning | Amber | #F59E0B | Warnings, paused |
| Error | Red | #EF4444 | Errors |

### Status Colors

| Status | Color | Hex |
|--------|-------|-----|
| Active | Green | #10B981 |
| Paused | Orange | #F59E0B |
| Idle | Gray | #6B7280 |
| Exploring | Blue | #3B82F6 |
| Completed | Green | #10B981 |
| Error | Red | #EF4444 |

---

## 3.8 Responsive Behavior

### Portrait Mode

Default layout as shown above - single column, scrollable.

### Landscape Mode

Same layout, adjusted spacing for wider screens.

### Scroll Behavior

The entire content area is scrollable when content exceeds screen height.

---

## 3.9 Accessibility Features

| Feature | Implementation |
|---------|----------------|
| Touch targets | Minimum 48dp for all buttons |
| Color contrast | WCAG AAA (7:1 minimum) |
| Screen reader | All elements labeled |
| Font scaling | Respects system font size |

---

## 3.10 UI State Transitions

### Exploration State Flow

```
      +--------+
      |  IDLE  |  <- Initial state
      +---+----+
          |
          | Start Exploration
          v
  +-------+--------+
  |   EXPLORING    |  <- Active exploration
  +-------+--------+
          |
          +---------------+---------------+
          |               |               |
          | Complete      | Login         | Error
          v               v               v
  +-------+----+  +-------+--------+  +---+---+
  | COMPLETED  |  | WAITING_USER   |  | ERROR |
  +------------+  +----------------+  +-------+
                          |
                          | User action
                          v
                  +-------+--------+
                  |   EXPLORING    |
                  +----------------+
```

---

## 3.11 Next Steps

Now that you understand the interface, continue to [Chapter 4: Features & Functions](./04-Features-Functions.md) to learn how to use each feature.

---

**End of Chapter 3**
