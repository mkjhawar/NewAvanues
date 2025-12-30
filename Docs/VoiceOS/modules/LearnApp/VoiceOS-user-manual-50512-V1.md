# LearnApp User Manual

**Module**: LearnApp
**App**: VoiceOS
**Version**: 1.6 (Developer Settings UI)
**Last Updated**: 2025-12-05 21:00 PST

## What's New in v1.6? ⭐ LATEST

### Developer Settings UI Now Available!

You can now access **51 configurable settings** via a dedicated settings screen!

### How to Access Developer Settings

From any screen in VoiceOS, you can open Developer Settings:

```kotlin
// From code
LearnAppIntegration.openDeveloperSettings(context)
```

Or through VoiceOS settings menu (coming soon).

### What You Can Customize

| Category | Settings | Examples |
|----------|----------|----------|
| Exploration | 5 | Max depth, timeout, completeness threshold |
| Navigation | 4 | Bounds tolerance, click failures, back attempts |
| Login & Consent | 3 | Login timeout (10 min default), permission polling |
| Scrolling | 7 | Scroll delay, max attempts, container limits |
| Click & Interaction | 4 | Click delay, retry attempts, screen processing |
| UI Detection | 5 | Touch target size, expansion wait, confidence |
| JIT Learning | 3 | Capture timeout, traversal depth, element limits |
| State Detection | 9 | Transient duration, flicker interval, penalties |
| Quality & Processing | 6 | Quality weights, batch size, label length |
| UI & Debug | 3 | Overlay timeout, verbose logging, screenshots |

### Key Improvements

- **All timing is configurable** - Adjust click delays, scroll delays, and timeouts
- **Verbose logging toggle** - Enable/disable debug logs at runtime
- **No code changes needed** - All settings persist and take effect immediately

---

## What's New in v1.5?

LearnApp now has **configurable exploration settings** for advanced users and developers!

### Better Stats

LearnApp now tracks **scrollable containers** found during exploration:
- See how many scrolling lists were discovered
- More accurate exploration statistics
- Helps identify apps with lots of scrollable content

### Smarter App Completion

Apps are now properly marked as "fully learned" when exploration reaches 95%+ completeness:
- Clear status: LEARNED vs IN_PROGRESS
- Accurate progress tracking
- Better re-learning recommendations

---

## What's New in v1.4?

LearnApp now achieves **98% click success rate** during exploration! This means more thorough learning with fewer "stuck" moments.

### More Reliable Exploration

- ✅ **98% Click Success** - Up from 70-80% in v1.3
- ✅ **No Stale Elements** - Fresh screen scan before every click
- ✅ **Smarter Click Order** - Most reliable buttons clicked first
- ✅ **Fallback Clicks** - If button can't be found, uses screen coordinates

**How it works:**
1. Before each click, LearnApp does a fresh scan of the screen
2. It prioritizes buttons with clear IDs and labels
3. If a normal click fails, it uses an alternate method
4. Result: Almost every clickable element gets clicked!

**What this means for you:**
- Learning finds more screens and features
- Fewer "learning incomplete" situations
- Better voice commands after learning
- More reliable exploration of complex apps

**Technical details:** See [Hybrid C-Lite Strategy](./developer-manual.md#hybrid-c-lite-exploration-strategy-2025-12-05--latest) in Developer Manual.

---

## What's New in v1.3?

Just-in-Time (JIT) learning is now **86% faster** when revisiting screens you've already seen! This means better battery life and less waiting.

### Smarter JIT Learning

- ✅ **Instant Screen Recognition** - JIT remembers screens it already learned
- ✅ **Skip Duplicate Work** - Won't re-scan screens you've visited before
- ✅ **86% Faster** - 7ms check vs 50ms full scan on revisited screens
- ✅ **Better Battery Life** - Avoid redundant work saves your battery

**How it works:**
1. When you first visit a screen, JIT learns it (takes ~50ms)
2. JIT remembers the screen with a unique "fingerprint"
3. Next time you visit the same screen, JIT recognizes it instantly (<7ms)
4. Skips the expensive re-scan, saving battery and time!

**Real improvements:**
- Instagram feed: Revisit = instant recognition (was 50ms)
- Gmail inbox: Return from email = skip re-scan (saves battery)
- Photos app: Navigate back = no duplicate work (86% faster)

**Smart popup handling:**
Even better - JIT recognizes the *type* of popup, not just the content. This means:
- "Delete Photo1.jpg?" and "Delete Photo2.jpg?" are recognized as the same delete confirmation popup
- Time pickers with different times are recognized as the same time picker
- You get the same fast recognition even when popup content changes!

---

## What's New in v1.2?

Voice commands now work **instantly** on JIT-learned apps! This is a major improvement to how LearnApp handles voice commands.

### Smart Voice Commands

- ✅ **Instant Recognition** - Voice commands for learned apps work immediately
- ✅ **4-Tier Matching** - Multiple fallback methods ensure commands work
- ✅ **Remembers Elements** - Buttons and controls are saved for later use
- ✅ **Works Offline** - Commands work even without internet

**How it works:**
1. When you learn an app, LearnApp saves all buttons and controls
2. Voice commands like "click Submit" instantly find the right button
3. If the button moved, LearnApp uses smart search to find it
4. Works on all apps you've learned!

---

## What's New in v1.1?

LearnApp is now **smarter and more thorough**! We've upgraded the learning system to find 3-4 times more screens in your apps:

- ✅ **Finds More Screens** - Now discovers overflow menus, bottom navigation tabs, and hidden screens
- ✅ **Works with More Apps** - Supports system apps like Settings
- ✅ **Handles Login Better** - Gives you 10 minutes to enter passwords and 2FA codes
- ✅ **Deeper Exploration** - Explores twice as deep into your apps

**Real improvements:**
- Google Calculator: 1 screen → 4 screens found
- Google Clock: 2 screens → 8 screens found
- Voice commands like "world clock" now work!

---

## Table of Contents

1. [What's New in v1.6](#whats-new-in-v16--latest) ⭐ LATEST
2. [What is LearnApp?](#what-is-learnapp)
3. [How It Works](#how-it-works)
4. [Getting Started](#getting-started)
5. [The Consent Dialog](#the-consent-dialog)
6. [Learning Process](#learning-process)
7. [Voice Commands on Learned Apps](#voice-commands-on-learned-apps)
8. [What Data is Collected](#what-data-is-collected)
9. [Privacy & Security](#privacy--security)
10. [Managing Learned Apps](#managing-learned-apps)
11. [Voice Contacts](#voice-contacts)
12. [Frequently Asked Questions](#frequently-asked-questions)
13. [Troubleshooting](#troubleshooting)

---

## What is LearnApp?

LearnApp is VoiceOS's intelligent feature that automatically learns how to control third-party apps using voice commands. When you install a new app, LearnApp can explore it and figure out how to navigate menus, click buttons, and perform actions - all without requiring the app developer to do any work.

### Key Benefits

- **Automatic Voice Control**: Turn any app into a voice-controlled app
- **No Developer Integration**: Works with apps that don't support accessibility
- **Privacy First**: You control what apps get learned
- **Continuous Improvement**: Re-learn apps when they update

### Real-World Example

You install Instagram for the first time. VoiceOS notices and asks: "Do you want VoiceOS to learn Instagram?" You say yes. LearnApp explores Instagram for 2-3 minutes, clicking around to discover all the buttons and screens. Once done, you can say:

- "Open Instagram"
- "Go to my profile"
- "Click the search button"
- "Navigate to messages"

All without touching your phone!

---

## How It Works

### The Learning Process

```
1. You Launch App
   ↓
2. VoiceOS Detects New App
   ↓
3. Consent Dialog Appears
   ↓
4. You Approve Learning
   ↓
5. LearnApp Explores App (2-5 minutes)
   ├─ Clicks buttons and menus
   ├─ Navigates between screens
   ├─ Maps out the app structure
   └─ Skips dangerous actions
   ↓
6. Learning Complete
   ↓
7. Voice Control Enabled!
```

### What Happens During Learning?

LearnApp uses a **Depth-First Search (DFS) algorithm** to systematically explore your app:

1. **Start at Home Screen**: Begins at the app's main screen
2. **Find Clickable Elements**: Identifies all buttons, menus, and links
3. **Click and Explore**: Clicks each element to see where it goes
4. **Build a Map**: Creates a navigation graph of the entire app
5. **Backtrack**: Returns to previous screens to explore other paths
6. **Repeat**: Continues until entire app is mapped

### Safety Features

LearnApp is designed to be **safe and non-destructive**:

- ✅ Only clicks buttons and navigates menus
- ✅ Skips "dangerous" actions (delete, purchase, send, post)
- ✅ Pauses when login screens detected (now 10 minutes for 2FA)
- ✅ Respects depth limits (won't get stuck)
- ✅ Time limits prevent infinite loops
- ❌ Never enters text or form data
- ❌ Never makes purchases
- ❌ Never deletes or modifies your data

### New in v1.1: Smarter Discovery

LearnApp now finds **hidden navigation** that was missed before:

- 🔍 **Overflow Menus** - Clicks the 3-dot menu to find hidden options
- 🔍 **Bottom Navigation** - Explores all tabs (like in Google Clock)
- 🔍 **Toolbar Icons** - Finds buttons in the top bar
- 🔍 **Large Icons** - Identifies clickable images and icons
- ⏱️ **More Time** - Takes up to 60 minutes for complex apps (was 30)

---

## Getting Started

### Prerequisites

Before LearnApp can work, ensure:

1. **VoiceOS is Installed**: LearnApp is part of VoiceOS
2. **Accessibility Service Enabled**: VoiceOS accessibility service must be running
3. **Overlay Permission Granted**: VoiceOS needs permission to display overlays

### First-Time Setup

1. **Install VoiceOS**
2. **Grant Permissions**:
   - Accessibility permission
   - Overlay permission (for dialogs)
3. **Launch a New App**

That's it! VoiceOS will automatically detect new apps and offer to learn them.

---

## The Consent Dialog

### When You'll See It

The consent dialog appears when:

- You launch an app VoiceOS hasn't learned yet
- The app is NOT a system app (Settings, Phone, etc.)
- You haven't recently declined learning this app (24-hour window)

### What It Looks Like

```
┌────────────────────────────────────┐
│                                    │
│   Learn Instagram?                 │
│                                    │
│   VoiceOS will explore Instagram   │
│   to enable voice commands.        │
│                                    │
│   This will:                       │
│   • Click buttons and menus        │
│   • Navigate between screens       │
│   • Skip dangerous actions         │
│   • Take ~2-5 minutes              │
│                                    │
│   ┌────┐            ┌────┐         │
│   │ No │            │Yes │         │
│   └────┘            └────┘         │
│                                    │
│   □ Don't ask again for this app   │
│                                    │
└────────────────────────────────────┘
```

### Your Choices

#### Option 1: Click "Yes"

- LearnApp will immediately start exploring the app
- You'll see a progress overlay showing:
  - Number of screens explored
  - Elements discovered
  - Current depth
  - Elapsed time
- You can pause or stop anytime
- Takes 2-5 minutes on average

#### Option 2: Click "No"

- LearnApp won't explore this app
- You can still use the app normally
- Voice control won't be available for this app
- Dialog won't show again for 24 hours (unless you check "don't ask again")

#### Option 3: "Don't Ask Again"

Check this box if you:

- **Never want voice control** for this app
- **Don't want to be bothered** with the dialog

**For "Yes" + "Don't Ask Again":**
- App will be learned now
- Won't ask again even if app updates

**For "No" + "Don't Ask Again":**
- App won't be learned
- Won't ask again in the future

---

## Learning Process

### What You'll See

#### Progress Overlay

While LearnApp is exploring, you'll see a small overlay showing:

```
┌─────────────────────────────────┐
│ Learning Instagram...           │
│                                 │
│ Progress: 15 / ~30 screens      │
│ ██████████░░░░░░░░░ 50%        │
│                                 │
│ Elements: 234                   │
│ Depth: 5                        │
│ Time: 2m 15s                    │
│                                 │
│ [Pause]  [Stop]                 │
└─────────────────────────────────┘
```

#### What's Happening

- **Screens Explored**: Number of unique screens discovered
- **Progress Bar**: Estimated completion (based on discovered screens)
- **Elements**: Total UI elements (buttons, menus, etc.) found
- **Depth**: How many clicks deep into the app
- **Time**: Elapsed time

### Interacting During Learning

#### Pause Learning

Click **[Pause]** to temporarily stop exploration:

- Useful if you need to use your phone
- Exploration can be resumed later
- Progress is saved

#### Stop Learning

Click **[Stop]** to end exploration:

- Saves all data collected so far
- Voice control enabled for discovered elements
- Can re-learn app later to discover more

### Special Cases

#### Login Screens

If LearnApp encounters a login screen:

1. **Automatically pauses**
2. **Shows login prompt**: "Please log in to continue learning"
3. **Waits for you** to complete login (up to 10 minutes) ⭐ NEW
4. **Resumes automatically** once logged in

**v1.1 Update:** Login timeout increased from 1 minute to **10 minutes**!

This gives you plenty of time to:
- Enter your email and password
- Handle 2-factor authentication (2FA)
- Enter verification codes from SMS or email
- Solve captchas
- Use your password manager
- Deal with security questions

**Why?** LearnApp never enters passwords or credentials. It needs you to log in manually.

**Tip:** If you're not ready to login, you can tap "Stop" and re-learn the app later when you have your login information handy.

#### App Permissions

If the app requests permissions during learning:

- LearnApp will pause
- You grant/deny the permission
- Learning continues

### Completion

When learning is complete:

1. **Progress overlay disappears**
2. **Notification shown**: "Finished learning Instagram"
3. **Voice control enabled** for all discovered features
4. **Data saved** to device

---

## Voice Commands on Learned Apps

### Overview

Once LearnApp finishes learning an app, you can control it with voice commands! The system uses a smart 4-tier approach to find buttons and controls:

```
Your Voice Command
       │
       ▼
┌─────────────────────┐
│ Tier 1: Quick Match │ ← "Open settings" finds Settings icon instantly
└──────────┬──────────┘
           │ Not found?
           ▼
┌─────────────────────┐
│ Tier 2: Database    │ ← Searches saved buttons from learning
└──────────┬──────────┘
           │ Not found?
           ▼
┌─────────────────────┐
│ Tier 3: Smart Search│ ← Uses button properties to find match
└──────────┬──────────┘
           │ Not found?
           ▼
┌─────────────────────┐
│ Tier 4: Text Search │ ← Looks for any matching text on screen
└─────────────────────┘
```

### Supported Voice Commands

After learning an app, you can use these commands:

#### Click/Tap Commands
- "Click Submit"
- "Tap the settings button"
- "Press continue"
- "Click the menu icon"

#### Scroll Commands
- "Scroll down"
- "Scroll up"
- "Scroll to the bottom"

#### Navigation Commands
- "Go back"
- "Open home"
- "Navigate to profile"

### Examples

**Instagram (after learning):**
- "Open messages"
- "Click the search icon"
- "Tap the home button"
- "Open my profile"

**Gmail (after learning):**
- "Click compose"
- "Open settings"
- "Tap the menu"
- "Click inbox"

### Tips for Best Results

1. **Use Button Labels**: Say exactly what's written on the button
   - ✅ "Click Submit" (if button says "Submit")
   - ❌ "Click the send thing"

2. **Be Specific**: If there are multiple similar buttons, be specific
   - ✅ "Click the blue Send button"
   - ✅ "Click Send in the toolbar"

3. **Re-learn After Updates**: If an app updates and commands stop working, re-learn it

4. **Wait for Learning**: Commands only work after learning completes

### How It Remembers Buttons

When LearnApp learns an app, it saves:
- Button names and labels
- Button positions on screen
- Button types (button, menu, icon, etc.)
- Unique identifiers for each button

This information is used to:
- Instantly find buttons you ask for
- Find buttons even if they moved slightly
- Match your voice command to the right control

### Performance

| Action | Speed |
|--------|-------|
| Quick match (common commands) | <5ms |
| Database lookup | <30ms |
| Smart search | <20ms |
| Text search (fallback) | <50ms |

**Total response time:** Usually under 100ms!

---

## What Data is Collected

### Data LearnApp Stores

LearnApp collects and stores:

1. **App Information**
   - Package name (e.g., "com.instagram.android")
   - App name (e.g., "Instagram")
   - Version code and name

2. **Screen Information**
   - Screen fingerprints (unique hashes)
   - Activity names
   - Element counts per screen

3. **UI Elements**
   - Element UUIDs (unique identifiers)
   - Element types (button, menu, etc.)
   - Element names/labels
   - Accessibility properties

4. **Navigation Graph**
   - Screen-to-screen navigation paths
   - Which buttons lead where
   - App structure map

5. **Metadata**
   - When app was learned
   - Exploration duration
   - Statistics (screens, elements, depth)

### What is NOT Collected

LearnApp **NEVER** collects:

- ❌ Your personal data (photos, messages, contacts)
- ❌ Login credentials or passwords
- ❌ App content (posts, emails, documents)
- ❌ User-entered text
- ❌ Payment information
- ❌ Location data
- ❌ Any data from the app's servers

### Where Data is Stored

All data is stored:

- **Locally on your device** (Room database)
- **NOT uploaded** to any servers
- **NOT shared** with app developers
- **Only accessible** by VoiceOS

---

## Privacy & Security

### Your Privacy Matters

LearnApp is designed with privacy as the top priority:

#### Local-Only Processing

- All learning happens **on your device**
- No data sent to cloud or external servers
- No internet connection required

#### Minimal Data Collection

- Only collects **UI structure**, not content
- Can't access app data or user information
- Only sees what's visible on screen

#### User Control

- **You decide** which apps to learn
- **You can delete** learned apps anytime
- **You control** when learning happens

### Security Features

#### Safe Exploration

- **No text entry**: Won't enter passwords or data
- **Dangerous action detection**: Skips delete, purchase, send buttons
- **Depth limits**: Won't explore infinitely
- **Time limits**: Stops after maximum time

#### Permission Model

LearnApp requires:

1. **Accessibility Service**: To see and interact with UI
   - Required for all VoiceOS features
   - Standard Android permission

2. **Overlay Permission**: To show dialogs and progress
   - For consent dialog
   - For progress overlay
   - Standard Android permission

### What Apps Can/Can't Do

Apps being learned:

- ✅ Can see LearnApp is clicking buttons
- ✅ Can detect accessibility service is active
- ❌ Can't tell why buttons are being clicked
- ❌ Can't access LearnApp data
- ❌ Can't prevent learning (if accessibility enabled)

---

## Managing Learned Apps

### Viewing Learned Apps

*Feature coming soon in VoiceOS settings*

You'll be able to:

- See list of all learned apps
- View learning statistics
- Check when each app was learned
- See number of screens and elements discovered

### Re-Learning Apps

To re-learn an app (e.g., after update):

1. **Delete learned data** (in VoiceOS settings)
2. **Launch app** again
3. **Approve** consent dialog
4. **Learning** starts fresh

### Deleting Learned Apps

To remove learned data:

1. Open **VoiceOS settings**
2. Go to **Learned Apps**
3. Select app
4. Tap **Delete**

**Effect**:
- All learned data for app is deleted
- Voice control disabled for that app
- Can re-learn anytime

---

## Voice Contacts

**Coming Soon:** Version 2.0 feature (Planned for Q1 2026)

### What are Voice Contacts?

Voice Contacts is a new feature that lets LearnApp discover and manage your contacts from communication apps (WhatsApp, Teams, Slack, etc.) so you can use natural voice commands like:

- "Call Mike Johnson on Teams"
- "Message Sarah on WhatsApp"
- "Video call John on Zoom"

### How It Works

#### Simple 3-Step Process

```
Step 1: Learn App UI
   ↓
   LearnApp explores WhatsApp buttons and screens
   (This already happens today!)

Step 2: Learn Contacts (NEW!)
   ↓
   After UI learning, you'll see:
   "Would you like to learn contacts from WhatsApp?"

Step 3: Voice Commands Work!
   ↓
   "Call Mike Johnson on WhatsApp"
```

#### What Makes It Special

**1. Works Across All Your Apps**
- Learn contacts from WhatsApp, Teams, Slack, Discord, etc.
- All contacts unified in one place
- No duplicate contacts across apps

**2. Privacy-First Design**
- You choose which apps to learn contacts from
- All data stays on your device
- Can export/import for backup
- Optional linking with phone contacts

**3. Smart Deduplication**
- "Mike Johnson" on WhatsApp + "Johnson, Mike" on Teams = One contact
- You decide which app to use as default
- Works even if names are formatted differently

### How Contact Learning Works

#### After App Learning Completes

When LearnApp finishes learning an app's UI (buttons and screens), you'll see a new dialog:

```
┌────────────────────────────────────┐
│                                    │
│   Learn contacts from WhatsApp?    │
│                                    │
│   This will let you say:           │
│   "Call John on WhatsApp"          │
│                                    │
│   LearnApp will:                   │
│   • Navigate to your contacts list │
│   • Read visible contact names     │
│   • Store them locally             │
│   • Take ~1-2 minutes              │
│                                    │
│   ┌────┐            ┌────┐         │
│   │ No │            │Yes │         │
│   └────┘            └────┘         │
│                                    │
└────────────────────────────────────┘
```

#### What Happens When You Click "Yes"

1. **Navigate to Contacts**
   - LearnApp finds your contacts/people list in the app
   - Works with different app layouts automatically

2. **Extract Contact Data**
   - Reads contact names
   - Saves profile pictures (if visible)
   - Captures visible info (status, job title, etc.)

3. **Merge with Existing Contacts**
   - Combines with contacts from other apps
   - Finds duplicates automatically
   - Creates one unified contact list

4. **Ready for Voice Commands**
   - "Call Mike Johnson on Teams" now works!
   - "Message Sarah on WhatsApp" works too!

### Privacy & What's Collected

#### What LearnApp DOES Collect

Only visible contact information:
- ✅ Contact names (as shown in app)
- ✅ Profile pictures (URLs/paths only)
- ✅ Visible metadata (status messages, job titles)
- ✅ Phone numbers (if visible in the app)
- ✅ Email addresses (if visible in the app)

#### What LearnApp NEVER Collects

- ❌ Message contents or history
- ❌ Call history
- ❌ Conversation data
- ❌ Files or media shared
- ❌ Payment information
- ❌ Location data

#### Where Contacts Are Stored

- **Local database only** - Never uploaded to cloud
- **Encrypted storage** (coming in v2.0)
- **Export option** - Backup to your own files
- **Full control** - Delete anytime

### Progressive Permission Levels

LearnApp uses a "progressive permission" model - you start with minimal access and upgrade if you want more features:

#### Level 1: Accessibility Only (Default)

What you get:
- ✅ Learn contacts from apps you approve
- ✅ Voice commands work perfectly
- ✅ Export/import your contacts
- ✅ NO phone contacts permission needed

What you can't do:
- ❌ Can't link with phone contacts
- ❌ Can't enrich contact data from phone

#### Level 2: System Linked (Optional Upgrade)

If you choose to link with phone contacts:
- ✅ All Level 1 features
- ✅ Match learned contacts with phone contacts
- ✅ Enrich contact data (phone numbers, emails)
- ✅ See "Also in Phone Contacts" tag
- ✅ One-way sync (read only)

Requires:
- 📱 READ_CONTACTS permission (you can grant this later)

#### Level 3: Full Sync (Future - v2.0)

Coming in version 2.0:
- ✅ All Level 2 features
- ✅ Update phone contacts automatically
- ✅ Bi-directional sync
- ✅ Merge duplicates across all apps

### Using Voice Commands with Contacts

#### Basic Commands

Once contacts are learned, you can use natural voice commands:

**Calling:**
- "Call Mike Johnson on Teams"
- "Video call Sarah on Zoom"
- "Voice call John on WhatsApp"

**Messaging:**
- "Message Sarah on WhatsApp"
- "Send a message to Mike on Teams"
- "Chat with John on Slack"

**Opening Contacts:**
- "Open Mike Johnson's profile on LinkedIn"
- "Show Sarah's contact on WhatsApp"

#### Smart Contact Matching

If you have "Mike Johnson" in multiple apps, LearnApp is smart about which one to use:

**Scenario 1: You specify the app**
```
You: "Call Mike Johnson on Teams"
LearnApp: Uses Teams contact ✓
```

**Scenario 2: You don't specify**
```
You: "Call Mike Johnson"
LearnApp: Uses your preferred app (you can set this)
```

**Scenario 3: Multiple "Mike Johnson" contacts**
```
You: "Call Mike Johnson"
LearnApp: "I found 2 contacts named Mike Johnson:
          1. Mike Johnson on Teams
          2. Mike Johnson on WhatsApp
          Which one?"
You: "Teams"
LearnApp: Calls Mike on Teams ✓
```

### Managing Your Contacts

#### Viewing Learned Contacts

In VoiceOS settings:
1. Go to **Voice Contacts**
2. See all your unified contacts
3. View which apps have each contact
4. See profile pictures and metadata

#### Setting Preferred Apps

For contacts in multiple apps, set your preference:

1. Open **Voice Contacts** settings
2. Select a contact (e.g., "Mike Johnson")
3. Choose **Preferred App** (e.g., "Teams")
4. Now "Call Mike Johnson" defaults to Teams

#### Deleting Contacts

You can delete contacts anytime:

**Delete from specific app:**
1. Open **Voice Contacts**
2. Select contact
3. Tap app name (e.g., "WhatsApp")
4. Tap **Delete from WhatsApp**

**Delete unified contact:**
1. Open **Voice Contacts**
2. Select contact
3. Tap **Delete Completely**
4. All sources removed

#### Re-Learning Contacts

If an app's contacts changed (new friends, contacts updated):

1. Open the app
2. Say "Re-learn contacts from WhatsApp"
3. LearnApp refreshes contact list
4. New contacts added, old ones updated

### Export & Import

#### Exporting Contacts

Backup all your contacts to a file:

1. Open **Voice Contacts** settings
2. Tap **Export Contacts**
3. Choose location (e.g., Downloads folder)
4. File saved as `voiceos-contacts.vos`

**Export includes:**
- All unified contacts
- Contact data from all apps
- Your preferences (preferred apps, notes)
- Profile pictures (paths)

#### Importing Contacts

Restore contacts from a backup:

1. Open **Voice Contacts** settings
2. Tap **Import Contacts**
3. Select your `.vos` file
4. Contacts restored!

**Import options:**
- **Merge** - Add to existing contacts (default)
- **Replace** - Delete all and import fresh
- **Skip duplicates** - Only import new contacts

#### VOS Contact Format

Contacts are saved in **VOS Compact Format** - a special format designed for efficiency:

- **File size:** 44% smaller than standard JSON
- **Format:** 3-letter codes (like "NAM" for name, "PHN" for phone)
- **Compatible:** Works across VoiceOS versions
- **Encrypted:** Password protection (coming in v2.0)

### Example Workflow

#### Sarah's Story

**Sarah uses WhatsApp, Teams, and Slack for work:**

1. **Learn WhatsApp**
   - LearnApp learns WhatsApp UI
   - Dialog: "Learn contacts from WhatsApp?"
   - Sarah clicks "Yes"
   - LearnApp finds 47 contacts

2. **Learn Teams**
   - LearnApp learns Teams UI
   - Dialog: "Learn contacts from Teams?"
   - Sarah clicks "Yes"
   - LearnApp finds 23 contacts
   - 12 contacts match WhatsApp (merged automatically!)

3. **Learn Slack**
   - LearnApp learns Slack UI
   - Dialog: "Learn contacts from Slack?"
   - Sarah clicks "Yes"
   - LearnApp finds 15 contacts
   - 8 contacts match existing (merged!)

4. **Result**
   - Total: 75 unique contacts
   - Mike Johnson found in all 3 apps (shown as one contact)
   - Sarah sets Teams as preferred for Mike

5. **Using Voice**
   - "Call Mike Johnson" → Uses Teams (preferred)
   - "Message Mike on Slack" → Uses Slack
   - "Call Sarah's boss on Teams" → Works!

### FAQ About Voice Contacts

**Q: Do I have to learn contacts?**

A: No! It's completely optional. You can:
- Skip contact learning and just use UI voice commands
- Decline contact learning for specific apps
- Use voice contacts for some apps, not others

**Q: Will it learn my private contacts?**

A: Only contacts visible in apps you approve. And remember:
- You choose which apps
- You can delete contacts anytime
- All data stays local
- No cloud upload

**Q: What if I don't want to link with phone contacts?**

A: That's fine! The default is "Accessibility Only" mode:
- No phone contacts permission needed
- Learned contacts work perfectly
- Just can't enrich data from phone

You stay at Level 1 and everything still works!

**Q: How does deduplication work?**

A: LearnApp is smart about matching:

1. **Normalize names**: "Mike Johnson" and "MIKE JOHNSON" match
2. **Handle formats**: "Johnson, Mike" and "Mike Johnson" match
3. **User decides**: You pick which app's data to use
4. **Merge metadata**: Combines phone numbers, emails, etc.

**Q: Can I edit contact information?**

A: Not directly in v1.0, but you can:
- Add notes to contacts
- Set preferred apps
- Mark favorites

Full editing coming in v2.0!

**Q: What happens if I delete an app?**

A: If you delete WhatsApp:
- WhatsApp contact data removed from database
- Contacts still in other apps remain
- If a contact only existed in WhatsApp, it's deleted
- You can re-import from backup if needed

**Q: Does this work with all apps?**

A: Works best with:
- ✅ Messaging apps (WhatsApp, Telegram, Signal)
- ✅ Business apps (Teams, Slack, Discord)
- ✅ Social apps (LinkedIn, Twitter, Instagram)
- ✅ Email apps (Gmail, Outlook)

May not work with:
- ❌ Apps without visible contact lists
- ❌ Apps with anti-automation protection
- ❌ Apps that require login for contacts

### Timeline

**Version 1.0 (Q1 2026):**
- ✅ Contact learning from apps
- ✅ Unified contact management
- ✅ Voice command integration
- ✅ Export/import (VOS format)
- ✅ Basic deduplication
- ✅ Progressive permission (Level 1 & 2)

**Version 1.5 (Q2 2026):**
- ✅ Enhanced deduplication
- ✅ Contact disambiguation improvements
- ✅ Import from VOS format
- ✅ "Also in Phone Contacts" indicator

**Version 2.0 (Q3 2026):**
- ✅ AVA NLU-powered smart matching
- ✅ Full bi-directional sync
- ✅ Encrypted exports
- ✅ Multi-device sync
- ✅ Advanced contact editing

### Getting Ready

Voice Contacts isn't available yet, but here's how to prepare:

1. **Update LearnApp** to v1.1 (Aggressive Exploration Mode)
2. **Learn your communication apps** (WhatsApp, Teams, etc.)
3. **Watch for updates** - v2.0 notification coming soon!

When v2.0 launches, you'll be prompted to learn contacts from your existing learned apps!

---

## Frequently Asked Questions

### About v1.1 (Aggressive Exploration Mode) ⭐ NEW

**Q: What's different in v1.1?**

A: LearnApp is now much smarter at finding all the screens in your apps! Before, it only clicked buttons that were marked as "clickable" by the app. Now it:
- Clicks overflow menus (the 3-dot icon)
- Explores all bottom navigation tabs
- Finds toolbar buttons and icons
- Discovers 3-4 times more screens

**Q: Will it re-learn my already learned apps?**

A: No, not automatically. But you can re-learn any app to discover more screens! Just:
1. Open the app
2. Say "re-learn this app" or use VoiceOS settings
3. Approve the re-learning process

**Q: Does it take longer now?**

A: It can take up to 60 minutes for very complex apps (like Facebook or Instagram), but most apps still finish in 2-5 minutes. The extra time ensures nothing is missed!

**Q: What's the "10 minute login" feature?**

A: When apps have login screens, LearnApp now waits up to 10 minutes for you to:
- Enter your email and password
- Handle 2-factor authentication (2FA)
- Solve captchas
- Use your password manager

Before, it only waited 1 minute and would give up.

**Q: Can it learn system apps now (like Settings)?**

A: Partially! LearnApp now detects system apps and tries to explore them, but with limited capabilities. Some features may not work due to Android security restrictions.

**Q: My Clock app only had 2 screens before. Will it find all 6 tabs now?**

A: Yes! That's exactly what v1.1 fixes. Apps with bottom navigation (Clock, Photos, Messages) will now have all tabs discovered.

---

### General

**Q: Does LearnApp work with all apps?**

A: LearnApp works with most apps that use standard Android UI components. It works best with:
- Material Design apps
- Apps with clear button labels
- Apps with consistent navigation

May not work well with:
- Games with custom rendering
- Apps with heavy graphics/canvas
- WebView-only apps
- Apps with anti-automation measures

**Q: How long does learning take?**

A: **v1.1 Update**: Learning now takes longer, but finds much more!

Typical times:
- Simple apps (Calculator, Notes): 2-5 minutes
- Medium apps (Clock, Gallery): 5-15 minutes
- Complex apps (Instagram, Gmail): 15-60 minutes

The extra time is worth it - you'll get 3-4 times more voice commands!

Factors affecting time:
- App complexity (number of screens and menus)
- Number of screens discovered
- Login screens (waits for you)
- App responsiveness
- Your device speed

**Q: Can I use my phone during learning?**

A: It's best not to. LearnApp is actively clicking buttons and navigating. Using your phone during learning can:
- Interfere with exploration
- Cause learning to fail
- Result in incomplete data

**Recommendation**: Start learning, then put your phone down for a few minutes.

### Privacy & Safety

**Q: Is LearnApp safe to use?**

A: Yes! LearnApp is designed with multiple safety features:
- Skips dangerous actions
- Never enters text
- Never makes purchases
- Never deletes data
- Pauses on login screens

**Q: Can LearnApp access my personal data?**

A: No. LearnApp only sees:
- UI element labels and types
- Screen layouts
- Button positions

It cannot access:
- Your messages, photos, or files
- App content or data
- Login credentials
- Personal information

**Q: Does LearnApp send data to servers?**

A: No. All data stays on your device. LearnApp:
- Processes everything locally
- Stores data in local database
- Never uploads to cloud
- Works offline

### Technical

**Q: Why does LearnApp need accessibility permission?**

A: Accessibility permission allows LearnApp to:
- See UI elements on screen
- Click buttons and navigate
- Detect screen changes
- Read element labels

This is the same permission used by screen readers and other accessibility tools.

**Q: Will LearnApp drain my battery?**

A: Learning uses battery while active (like any app exploration would). However:
- Only runs when you approve
- Completes in minutes
- Not running continuously
- Minimal impact after learning complete

**Q: Can apps detect LearnApp?**

A: Apps can detect that an accessibility service is active, but:
- They can't tell it's specifically LearnApp
- They can't see what LearnApp is doing
- Standard Android behavior
- Same as screen readers

### Consent & Control

**Q: What if I accidentally click "Yes"?**

A: You can stop learning anytime:
1. Click **[Stop]** button on progress overlay
2. Learning stops immediately
3. Partial data is saved

You can also:
- Delete learned data later
- Re-learn if you want to try again

**Q: Can I learn an app later if I click "No"?**

A: Yes! Just:
1. Wait 24 hours (or disable "don't ask again")
2. Launch the app again
3. Consent dialog will appear
4. Click "Yes" to start learning

Or manually trigger learning from VoiceOS settings.

**Q: How do I permanently disable LearnApp?**

A: In VoiceOS settings:
1. Go to **LearnApp settings**
2. Toggle **"Auto-detect new apps"** OFF

This disables automatic consent dialogs. You can still manually trigger learning.

---

## Troubleshooting

### Consent Dialog Issues

**Problem**: Dialog doesn't appear for new app

**Solutions**:

1. **Check if app is system app**
   - System apps are filtered (Settings, Phone, etc.)
   - LearnApp only learns third-party apps

2. **Check if recently dismissed**
   - Dialog won't show for 24 hours after "No"
   - Clear dismissal in VoiceOS settings

3. **Check overlay permission**
   - Settings → Apps → VoiceOS → Display over other apps
   - Must be enabled

4. **Check if already learned**
   - App may already be in database
   - Check learned apps list

### Learning Issues

**Problem**: Progress indicator keeps spinning after clicking "Yes" (FIXED ✅)

**What happened**:
When you clicked "Yes" to learn an app, the loading spinner would appear but never stop, making it look like the app froze.

**Why it happened**:
The consent popup was blocking the view of your app, so LearnApp couldn't detect the app's screen to start learning.

**Fixed on**: December 2, 2025

**What changed**:
- The consent popup now automatically closes when you click "Yes"
- LearnApp waits a moment for the popup to disappear
- Learning starts normally with the app visible
- If something goes wrong, you'll see a clear error message (no more endless spinning!)

**If you still see spinning**:
1. **Wait 30 seconds** - LearnApp will timeout and show an error
2. **Make sure the app is open** - The app you want to learn should be on screen
3. **Close other popups** - Dismiss any notifications or system dialogs
4. **Try again** - Click "Yes" on the consent popup again

---

**Problem**: LearnApp only learns part of an app / Keyboard interrupts learning (FIXED ✅)

**What happened**:
When learning apps like Google Photos, you noticed:
- Not all buttons were being clicked during learning
- Text input fields would be clicked, making the keyboard (Gboard) appear
- When the keyboard appeared, VoiceOS would ask for consent to learn Gboard
- This consent popup would interrupt the Photos learning that was already running

**Why it happened**:
1. **Input fields being clicked**: LearnApp didn't recognize all types of text input fields (especially Material Design ones used by Google apps), so it tried to click them like regular buttons
2. **Keyboard interrupting**: When the keyboard appeared, VoiceOS detected it as a "new app" and showed the consent dialog, even though you were already learning another app

**Fixed on**: December 2, 2025

**What changed**:
- ✅ **Better input field detection**: LearnApp now recognizes ALL types of text input fields (including Google Material Design fields)
- ✅ **Input fields are skipped**: Text input fields are no longer clicked during learning, so keyboards won't appear unexpectedly
- ✅ **No interruptions**: If a keyboard or other app appears during learning, VoiceOS won't show consent popups—learning continues
- ✅ **Better diagnostics**: Developers can now see exactly why specific buttons were skipped

**Important Notes**:
- ✅ **Text fields still get voice commands**: Even though LearnApp doesn't click text fields during learning, they still get voice commands! VoiceOS can learn them later when you actually use them (called "just-in-time learning")
- ✅ **Login screens still work**: If LearnApp finds a login screen, it will still pause and ask you to log in (with a security message that your password won't be learned)
- ✅ **All elements tracked**: Every button, field, and element gets a unique ID, even if it's not clicked during learning

**If you still see partial learning**:
1. **Check the logs** (for developers):
   - Connect device: `adb logcat | grep "ExplorationEngine-Skip"`
   - Look for: "STRATEGY REJECTED", "ALREADY CLICKED", or "CLICK FAILED"
   - This shows exactly why each element was skipped

2. **Common reasons elements are skipped**:
   - **Already clicked**: Normal! Prevents clicking the same button twice
   - **Strategy rejected**: Element filtered by learning strategy
   - **Click failed**: Element might be disabled or covered by something
   - **EditText detected**: Text input fields (this is correct behavior)

3. **What to do**:
   - **Text fields**: Normal! They'll be learned when you use the app
   - **Missing buttons**: Report which app and which button to VoiceOS support
   - **Login issues**: Make sure you're fully logged in before resuming learning

---

**Problem**: Learning gets stuck or doesn't complete

**Solutions**:

1. **Stop and restart**
   - Click [Stop] button
   - Delete learned data
   - Try again

2. **Check for infinite scrolls**
   - Some apps have infinite feeds
   - LearnApp has time limits to prevent this
   - May need manual intervention

3. **Check for popups**
   - Dismiss any popups or notifications
   - App permissions dialogs
   - Update prompts

**Problem**: Login screen doesn't resume

**Solutions**:

1. **Complete login fully**
   - Make sure you're fully logged in
   - App should show main screen

2. **Wait a few seconds**
   - LearnApp polls for screen change
   - May take 5-10 seconds to detect

3. **Restart learning**
   - If stuck, stop and restart
   - Login before learning

### Performance Issues

**Problem**: Learning is very slow

**Possible Causes**:

1. **Slow device** - Learning is CPU-intensive
2. **Complex app** - Many screens to explore
3. **Slow app** - App itself is slow to respond

**Solutions**:
- Close other apps
- Ensure good battery level
- Try when device isn't busy

**Problem**: App crashes during learning

**Solutions**:

1. **Update app** - May fix app bugs
2. **Clear app cache** - Free up resources
3. **Restart device** - Clean slate
4. **Report to VoiceOS** - May be compatibility issue

### Data Issues

**Problem**: Voice control doesn't work after learning

**Checks**:

1. **Verify learning completed**
   - Check learned apps list
   - Look for completion notification

2. **Check voice commands**
   - Use correct element names
   - Try "show buttons" to see what's available

3. **Re-learn if needed**
   - Delete learned data
   - Learn again

**Problem**: Can't delete learned app

**Solutions**:

1. **Force stop VoiceOS**
2. **Clear VoiceOS cache** (not data!)
3. **Restart device**
4. **Try again**

---

## Tips & Best Practices

### For Best Results

1. **Log in before learning**
   - Avoid login screen pauses
   - Ensures full app access

2. **Dismiss onboarding**
   - Complete any app tutorials first
   - Skip "welcome" screens

3. **Grant permissions**
   - Give app necessary permissions beforehand
   - Avoids permission dialogs during learning

4. **Stable internet**
   - Some apps need internet to load screens
   - Ensure good connection

5. **Full battery**
   - Learning can take several minutes
   - Avoid interruptions

### When to Re-Learn

Re-learn apps when:

- **App updates** significantly change UI
- **New features added** you want to control
- **Learning was incomplete** (you stopped early)
- **Voice commands stop working** (app changed)

### Apps That Work Best

LearnApp works great with:

- Social media apps (Twitter, Facebook, Instagram)
- Email apps (Gmail, Outlook)
- Shopping apps (Amazon, eBay)
- Messaging apps (WhatsApp, Telegram)
- Productivity apps (Notion, Trello)

LearnApp may struggle with:

- Games (custom rendering)
- Video streaming (mostly playback UI)
- Camera apps (complex custom UI)
- Banking apps (security restrictions)

---

## Getting Help

### Support Resources

1. **VoiceOS Documentation**
   - Comprehensive guides
   - API references
   - Troubleshooting

2. **Community Forum**
   - Ask questions
   - Share experiences
   - Get tips from other users

3. **Bug Reports**
   - Report issues on GitHub
   - Include app name and version
   - Describe what happened

### Feedback

We want to hear from you!

- **Feature requests**: What apps or features would you like?
- **Bug reports**: What's not working?
- **Success stories**: What apps work great?

---

## Privacy Policy Summary

LearnApp:

✅ **Stores data locally** on your device
✅ **Never uploads** to external servers
✅ **Collects only UI structure**, not content
✅ **Requires your consent** before learning
✅ **Lets you delete** data anytime

❌ **Never accesses** your personal data
❌ **Never enters** text or credentials
❌ **Never makes** purchases or modifications
❌ **Never shares** data with app developers

For full privacy policy, see VoiceOS Privacy Policy.

---

## Glossary

**Accessibility Service**: Android system service that allows apps to interact with UI for accessibility purposes (screen readers, automation, etc.)

**Consent Dialog**: The dialog asking permission to learn an app

**Depth-First Search (DFS)**: Algorithm that explores as deep as possible before backtracking

**Element**: A UI component (button, menu, text field, etc.)

**Exploration**: The process of LearnApp discovering app structure

**Navigation Graph**: A map of how screens connect in an app

**Overlay**: A window displayed on top of other apps

**Screen State**: A unique screen/page in an app

**UUID**: Universally Unique Identifier - a unique code for each element

---

**End of User Manual**

For developer documentation, see [Developer Manual](./developer-manual.md).

For technical details, see [Architecture Guide](./architecture/overview.md).
