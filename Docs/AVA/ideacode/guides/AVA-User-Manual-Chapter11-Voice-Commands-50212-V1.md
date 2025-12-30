# AVA AI - User Manual: Chapter 11 - Voice Commands

**Version**: 1.5.0
**Last Updated**: December 2, 2025
**For AVA AI App Version**: 1.5+

---

## Welcome to AVA Voice Commands!

AVA AI can understand and execute over 125 different types of voice commands across ten categories. This chapter will teach you how to use each command with natural language examples.

**What You'll Learn:**
- How to control your device with voice
- Commands for communication (calls, texts, emails)
- Media playback control
- Navigation and location features
- Productivity tools (reminders, calendar, notes)
- Accessibility and cursor control
- Keyboard and text input commands
- Screen gestures and scrolling

**Important:** All voice commands work by typing them in the chat interface. Future updates will add hands-free voice input.

---

## Table of Contents

1. [Communication Commands](#communication-commands)
2. [Device Control Commands](#device-control-commands)
3. [Media Commands](#media-commands)
4. [Navigation Commands](#navigation-commands)
5. [Productivity Commands](#productivity-commands)
6. [Web & General Commands](#web--general-commands)
7. [Accessibility Commands](#accessibility-commands)
8. [Keyboard Commands](#keyboard-commands)
9. [Gesture Commands](#gesture-commands)
10. [Tips for Natural Language](#tips-for-natural-language)
11. [Troubleshooting](#troubleshooting)

---

## Communication Commands

AVA can help you communicate with others through calls, texts, and emails.

### 📞 Making Phone Calls

**What it does:** Opens your phone dialer with the contact or number ready to call.

**How to say it:**
```
"Call Mom"
"Call John Smith"
"Dial 555-1234"
"Phone 415-555-0199"
"Ring Sarah"
```

**What happens:**
- AVA opens your phone app
- The contact or number is pre-filled
- You tap to confirm the call

**Examples in conversation:**
```
You: Call Dad
AVA: Calling Dad
     🟢 95%

You: Dial 911
AVA: Calling 911
     🟢 98%
```

**Tips:**
- Use contact names exactly as saved in your phone
- Include area code for unknown numbers
- Say "call" or "dial" first for best results

---

### 💬 Sending Text Messages

**What it does:** Opens your messaging app with the recipient and message ready to send.

**How to say it:**
```
"Text Mom saying I'm on my way"
"Send a text to John that I'll be late"
"Message Sarah saying thanks for lunch"
"SMS Dad I love you"
"Text 555-1234 saying the order is ready"
```

**Message patterns AVA understands:**
- "saying [message]"
- "that [message]"
- "to [contact] saying [message]"
- "to [contact] that [message]"

**Examples in conversation:**
```
You: Text Mom saying I'm running late
AVA: Sending text to Mom
     🟢 92%

You: Message John that the meeting is at 3pm
AVA: Sending text to John
     🟢 88%
```

**Tips:**
- Always include "saying" or "that" before your message
- Use full sentences for clarity
- AVA extracts both recipient and message automatically

---

### 📧 Sending Emails

**What it does:** Opens your email app with recipient, subject, and message pre-filled.

**How to say it:**
```
"Email alice@example.com about project updates"
"Send email to bob@company.com regarding tomorrow's meeting"
"Email john about the quarterly report"
"Mail sarah@work.com the presentation draft"
```

**Subject patterns AVA understands:**
- "about [subject]"
- "regarding [subject]"
- "subject [subject]"

**Examples in conversation:**
```
You: Email alice@example.com about the new feature
AVA: Opening email to alice@example.com
     🟢 85%

You: Send email to team@company.com regarding Monday's meeting
AVA: Opening email to team@company.com
     🟢 89%
```

**Tips:**
- Use full email addresses for best results
- Keep subjects short and clear
- You can add the body text in the email app

---

## Device Control Commands

Control your Android device settings with voice commands.

### 🔊 Volume Control

**What it does:** Adjusts your device volume up, down, or mutes it.

**How to say it:**
```
"Volume up"
"Increase volume"
"Louder"
"Turn up the sound"

"Volume down"
"Decrease volume"
"Quieter"
"Lower the volume"

"Mute"
"Volume mute"
"Silence"
```

**Examples in conversation:**
```
You: Volume up
AVA: Increasing volume
     🟢 98%

You: Mute
AVA: Muting volume
     🟢 95%
```

---

### 💡 Screen Brightness

**What it does:** Increases or decreases your screen brightness.

**How to say it:**
```
"Brightness up"
"Increase brightness"
"Brighter screen"
"Turn up brightness"

"Brightness down"
"Decrease brightness"
"Dimmer screen"
"Lower brightness"
```

**Examples in conversation:**
```
You: Brightness up
AVA: Increasing brightness
     🟢 96%

You: Dim the screen
AVA: Decreasing brightness
     🟢 92%
```

---

### 📶 WiFi Control

**What it does:** Turns WiFi on or off.

**How to say it:**
```
"WiFi on"
"Turn on WiFi"
"Enable WiFi"
"Connect to WiFi"

"WiFi off"
"Turn off WiFi"
"Disable WiFi"
"Disconnect WiFi"
```

**Examples in conversation:**
```
You: Turn on WiFi
AVA: Turning on WiFi
     🟢 99%

You: WiFi off
AVA: Turning off WiFi
     🟢 99%
```

---

### 📱 Bluetooth Control

**What it does:** Turns Bluetooth on or off.

**How to say it:**
```
"Bluetooth on"
"Turn on Bluetooth"
"Enable Bluetooth"
"Activate Bluetooth"

"Bluetooth off"
"Turn off Bluetooth"
"Disable Bluetooth"
"Deactivate Bluetooth"
```

**Examples in conversation:**
```
You: Turn on Bluetooth
AVA: Turning on Bluetooth
     🟢 98%

You: Bluetooth off
AVA: Turning off Bluetooth
     🟢 98%
```

---

### 🔦 Flashlight Control

**What it does:** Turns your device flashlight on or off.

**How to say it:**
```
"Flashlight on"
"Turn on flashlight"
"Enable flashlight"
"Light on"

"Flashlight off"
"Turn off flashlight"
"Disable flashlight"
"Light off"
```

**Examples in conversation:**
```
You: Flashlight on
AVA: Turning on flashlight
     🟢 97%

You: Turn off the light
AVA: Turning off flashlight
     🟢 94%
```

---

### ⏰ Setting Alarms

**What it does:** Opens your alarm app to set a new alarm.

**How to say it:**
```
"Set alarm for 7am"
"Alarm at 6:30"
"Wake me at 8:00"
"Set morning alarm"
```

**Examples in conversation:**
```
You: Set alarm for 7am
AVA: Setting alarm
     🟢 89%

You: Wake me at 6:30
AVA: Setting alarm
     🟢 86%
```

**Note:** Currently opens the alarm app. Entity extraction for specific times coming in a future update.

---

### ⏲️ Setting Timers

**What it does:** Sets a countdown timer with the specified duration.

**How to say it:**
```
"Set timer for 10 minutes"
"Timer for 5 minutes"
"Countdown 30 seconds"
"Set timer 1 hour"
"Timer 2 hours 30 minutes"
```

**Duration formats AVA understands:**
- Minutes: "10 minutes", "5 mins", "3m"
- Hours: "2 hours", "1 hour", "1h"
- Seconds: "30 seconds", "45 secs", "10s"

**Examples in conversation:**
```
You: Set timer for 10 minutes
AVA: Setting timer for 10 minutes
     🟢 94%

You: Timer for 30 seconds
AVA: Setting timer for 30 seconds
     🟢 96%

You: Set timer 1 hour
AVA: Setting timer for 1 hour
     🟢 93%
```

**Tips:**
- Be specific with units (minutes, hours, seconds)
- AVA automatically converts to the right format
- The timer starts silently in the background

---

## Media Commands

Control music and video playback on your device.

### 🎵 Playing Music

**What it does:** Opens your default music app and starts playback.

**How to say it:**
```
"Play music"
"Start music"
"Play song"
"Music on"
```

**Examples in conversation:**
```
You: Play music
AVA: Playing music
     🟢 97%

You: Start music
AVA: Playing music
     🟢 96%
```

**Note:** Entity extraction for specific songs/artists coming in a future update.

---

### ⏸️ Pausing Music

**What it does:** Pauses currently playing media.

**How to say it:**
```
"Pause music"
"Pause"
"Stop music"
"Pause playback"
```

**Examples in conversation:**
```
You: Pause music
AVA: Pausing music
     🟢 98%

You: Pause
AVA: Pausing music
     🟢 95%
```

---

### ▶️ Resuming Music

**What it does:** Resumes paused media playback.

**How to say it:**
```
"Resume music"
"Resume"
"Continue playing"
"Unpause"
"Keep playing"
```

**Examples in conversation:**
```
You: Resume music
AVA: Resuming playback
     🟢 97%

You: Continue playing
AVA: Resuming playback
     🟢 94%
```

---

### ⏭️ Skipping Tracks

**What it does:** Skips to the next track in your playlist.

**How to say it:**
```
"Next track"
"Skip song"
"Next"
"Skip"
"Next song"
```

**Examples in conversation:**
```
You: Next track
AVA: Skipping to next track
     🟢 98%

You: Skip
AVA: Skipping to next track
     🟢 96%
```

---

### ⏮️ Previous Track

**What it does:** Goes back to the previous track.

**How to say it:**
```
"Previous track"
"Last song"
"Go back"
"Previous"
"Back"
```

**Examples in conversation:**
```
You: Previous track
AVA: Going to previous track
     🟢 97%

You: Last song
AVA: Going to previous track
     🟢 94%
```

---

### 🎥 Playing Videos

**What it does:** Opens YouTube or your video app to play videos.

**How to say it:**
```
"Play video cats"
"Watch video funny dogs"
"Play cats on YouTube"
"Show video cooking tutorial"
```

**Platforms AVA understands:**
- YouTube (default)
- "on YouTube" explicitly specifies YouTube
- "video [query]" searches your default video app

**Examples in conversation:**
```
You: Play video cats
AVA: Playing video: cats
     🟢 91%

You: Watch funny dogs on YouTube
AVA: Playing video: funny dogs
     🟢 89%
```

**Tips:**
- Be specific with your search terms
- Add "on YouTube" for YouTube-specific searches
- AVA extracts the video title automatically

---

## Navigation Commands

Use maps and location features with voice commands.

### 🗺️ Getting Directions

**What it does:** Opens Google Maps with directions to your destination.

**How to say it:**
```
"Directions to work"
"Navigate to home"
"Get directions to 123 Main Street"
"Directions to Central Park"
"How do I get to the airport"
```

**Destination formats AVA understands:**
- Saved locations: "work", "home", "gym"
- Addresses: "123 Main St, City"
- Places: "Starbucks", "Central Park", "LAX"

**Examples in conversation:**
```
You: Directions to work
AVA: Getting directions to work
     🟢 94%

You: Navigate to 123 Main Street
AVA: Getting directions to 123 Main Street
     🟢 91%
```

**Tips:**
- Save common locations in Google Maps first
- Use full addresses for accuracy
- AVA opens Maps with the route ready

---

### 📍 Finding Nearby Places

**What it does:** Opens Google Maps to search for nearby locations.

**How to say it:**
```
"Find coffee near me"
"Find nearby restaurants"
"Where is the nearest gas station"
"Find ATM nearby"
"Search for pharmacy near me"
```

**Place types AVA understands:**
- Food: coffee, restaurant, cafe, pizza
- Services: gas station, ATM, pharmacy, bank
- Shopping: grocery store, mall, store
- General: [any place type] + "near me" or "nearby"

**Examples in conversation:**
```
You: Find coffee near me
AVA: Finding coffee nearby
     🟢 93%

You: Where is the nearest gas station
AVA: Finding gas station nearby
     🟢 89%
```

**Tips:**
- Use "near me" or "nearby" for best results
- Be specific with place types
- Results open in Google Maps

---

### 🚦 Checking Traffic

**What it does:** Opens Google Maps with the traffic layer enabled.

**How to say it:**
```
"Show traffic"
"Check traffic"
"How is traffic"
"Traffic conditions"
"What's the traffic like"
```

**Examples in conversation:**
```
You: Show traffic
AVA: Showing traffic conditions
     🟢 96%

You: How is traffic
AVA: Showing traffic conditions
     🟢 94%
```

**What you'll see:**
- Google Maps opens with traffic overlay
- Real-time traffic conditions
- Red/yellow/green route indicators

---

### 📤 Sharing Your Location

**What it does:** Opens Google Maps to share your current location with others.

**How to say it:**
```
"Share my location"
"Send my location"
"Share location"
"Where am I"
"Show my location"
```

**Examples in conversation:**
```
You: Share my location
AVA: Opening location sharing
     🟢 97%

You: Where am I
AVA: Opening location sharing
     🟢 93%
```

**What happens:**
- Maps opens with your current location
- Share options appear (text, email, etc.)
- Choose how to send your location

---

### 🔖 Saving Locations

**What it does:** Opens Google Maps to bookmark/save a location.

**How to say it:**
```
"Save location"
"Bookmark this place"
"Save this location"
"Remember this location"
"Add to saved places"
```

**Examples in conversation:**
```
You: Save location
AVA: Opening Maps to save location
     🟢 95%

You: Bookmark this place
AVA: Opening Maps to save location
     🟢 92%
```

**Tips:**
- Use this at places you want to remember
- Saved locations appear in Maps favorites
- Access saved places anytime in Maps

---

## Productivity Commands

Manage your tasks, calendar, and notes with voice commands.

### 📝 Creating Reminders

**What it does:** Opens Google Tasks or Google Keep to create a reminder.

**How to say it:**
```
"Remind me to buy milk"
"Reminder to call John"
"Don't forget to send the email"
"Remind me about the meeting"
```

**Task patterns AVA understands:**
- "remind me to [task]"
- "reminder to [task]"
- "don't forget to [task]"
- "remind me about [task]"

**Examples in conversation:**
```
You: Remind me to buy milk
AVA: Creating reminder
     🟢 89%

You: Don't forget to call mom
AVA: Creating reminder
     🟢 86%
```

**What happens:**
- Google Tasks opens (primary)
- Falls back to Google Keep if Tasks isn't available
- Task title is pre-filled with your reminder
- You can add time/date in the app

---

### 📅 Creating Calendar Events

**What it does:** Opens your calendar app with a new event pre-filled.

**How to say it:**
```
"Schedule meeting with John"
"Add to calendar dentist appointment"
"Create calendar event team standup"
"Calendar event lunch with Sarah"
```

**Event patterns AVA understands:**
- "schedule [event]"
- "add to calendar [event]"
- "create calendar event [event]"
- "calendar event [event]"

**Examples in conversation:**
```
You: Schedule meeting with John
AVA: Creating calendar event
     🟢 88%

You: Add to calendar dentist appointment tomorrow at 2pm
AVA: Creating calendar event
     🟢 85%
```

**Tips:**
- Include time/date details if known
- Event title is extracted automatically
- Complete the details in the calendar app

---

### 📆 Checking Your Calendar

**What it does:** Opens your calendar app to view upcoming events.

**How to say it:**
```
"Check calendar"
"What's on my calendar"
"Show my schedule"
"View calendar"
"Open calendar"
```

**Examples in conversation:**
```
You: Check calendar
AVA: Opening calendar
     🟢 97%

You: What's on my calendar today
AVA: Opening calendar
     🟢 94%
```

**What you'll see:**
- Calendar app opens to today's view
- All your scheduled events
- Quick access to add new events

---

### ✅ Adding To-Do Items

**What it does:** Opens Google Tasks or your to-do app with a new task.

**How to say it:**
```
"Add to do buy groceries"
"I need to call the dentist"
"Todo clean the garage"
"Add task finish report"
```

**Task patterns AVA understands:**
- "add to do [task]"
- "I need to [task]"
- "todo [task]"
- "add task [task]"

**Examples in conversation:**
```
You: Add to do buy groceries
AVA: Adding to-do
     🟢 91%

You: I need to finish the presentation
AVA: Adding to-do
     🟢 87%
```

**Tips:**
- Tasks sync with Google Tasks
- Access from any device
- Set priorities and due dates in the app

---

### 📓 Creating Notes

**What it does:** Opens Google Keep or your notes app with a new note.

**How to say it:**
```
"Take a note meeting summary"
"Note this buy milk eggs bread"
"Create note project ideas"
"Note remember password is 12345"
```

**Note patterns AVA understands:**
- "take a note [content]"
- "note this [content]"
- "create note [content]"
- "note [content]"

**Examples in conversation:**
```
You: Take a note meeting summary discussed Q4 goals
AVA: Creating note
     🟢 89%

You: Note this buy milk eggs and bread
AVA: Creating note
     🟢 92%
```

**What happens:**
- Google Keep opens (primary)
- Falls back to generic notes app if Keep isn't available
- Note content is pre-filled
- You can edit and add more details

---

## Web & General Commands

Search the web and open URLs with voice commands.

### 🔍 Web Search

**What it does:** Opens your browser with a web search for your query.

**How to say it:**
```
"Search for cats"
"Google kotlin tutorials"
"Search how to bake a cake"
"Look up weather forecast"
```

**Search patterns AVA understands:**
- "search for [query]"
- "google [query]"
- "search [query]"
- "look up [query]"

**Examples in conversation:**
```
You: Search for cats
AVA: Searching for: cats
     🟢 96%

You: Google kotlin tutorials
AVA: Searching for: kotlin tutorials
     🟢 94%
```

**Tips:**
- Use natural phrases
- Ask questions directly: "how to cook pasta"
- AVA opens your default browser

---

### 🌐 Opening URLs

**What it does:** Opens a specific website in your browser.

**How to say it:**
```
"Go to youtube.com"
"Open github.com"
"Navigate to google.com"
"Visit reddit.com"
```

**URL patterns AVA understands:**
- "go to [url]"
- "open [url]"
- "navigate to [url]"
- "visit [url]"

**Examples in conversation:**
```
You: Go to youtube.com
AVA: Opening youtube.com
     🟢 98%

You: Open github.com
AVA: Opening github.com
     🟢 97%
```

**Tips:**
- Include the full domain (.com, .org, etc.)
- AVA automatically adds "https://" if needed
- Works with any valid URL

---

## Accessibility Commands

AVA includes comprehensive accessibility features for hands-free device control.

### Cursor Control

**What it does:** Control an on-screen cursor for precise interactions.

**How to say it:**
```
"Show cursor"
"Hide cursor"
"Center cursor"
"Hand cursor"
"Normal cursor"
```

**Examples in conversation:**
```
You: Show cursor
AVA: Showing cursor
     🟢 97%

You: Center cursor
AVA: Centering cursor
     🟢 95%
```

---

### Selection and Confirmation

**What it does:** Select items and confirm actions on screen.

**How to say it:**
```
"Select"
"Confirm"
"Cancel"
"Submit"
"Close"
```

**Examples in conversation:**
```
You: Select
AVA: Selecting item
     🟢 98%

You: Confirm
AVA: Confirming action
     🟢 97%
```

---

### Click Actions

**What it does:** Perform click actions at cursor position.

**How to say it:**
```
"Single click"
"Double click"
"Long press"
```

**Examples in conversation:**
```
You: Double click
AVA: Double clicking
     🟢 96%

You: Long press
AVA: Long pressing
     🟢 95%
```

---

### Dictation Mode

**What it does:** Enter text by speaking.

**How to say it:**
```
"Start dictation"
"Dictation"
"End dictation"
"Stop dictation"
```

**Examples in conversation:**
```
You: Start dictation
AVA: Starting dictation mode
     🟢 94%

You: End dictation
AVA: Ending dictation mode
     🟢 96%
```

---

### Gaze Control

**What it does:** Enable or disable gaze-based cursor control.

**How to say it:**
```
"Gaze on"
"Enable gaze"
"Gaze off"
"Disable gaze"
```

**Examples in conversation:**
```
You: Gaze on
AVA: Enabling gaze control
     🟢 93%

You: Gaze off
AVA: Disabling gaze control
     🟢 94%
```

---

## Keyboard Commands

Control keyboard and text input with voice.

### Keyboard Visibility

**What it does:** Show, hide, or switch keyboards.

**How to say it:**
```
"Open keyboard"
"Show keyboard"
"Hide keyboard"
"Close keyboard"
"Change keyboard"
"Switch keyboard"
```

**Examples in conversation:**
```
You: Open keyboard
AVA: Opening keyboard
     🟢 97%

You: Hide keyboard
AVA: Hiding keyboard
     🟢 98%
```

---

### Text Editing

**What it does:** Edit text in input fields.

**How to say it:**
```
"Backspace"
"Delete"
"Clear text"
"Enter"
"Press enter"
```

**Examples in conversation:**
```
You: Backspace
AVA: Deleting character
     🟢 96%

You: Clear text
AVA: Clearing text
     🟢 95%
```

---

## Gesture Commands

Perform screen gestures with voice commands.

### Scrolling

**What it does:** Scroll the screen in any direction.

**How to say it:**
```
"Scroll up"
"Scroll down"
"Page up"
"Page down"
```

**Examples in conversation:**
```
You: Scroll down
AVA: Scrolling down
     🟢 98%

You: Scroll up
AVA: Scrolling up
     🟢 97%
```

---

### Swiping

**What it does:** Swipe the screen in any direction.

**How to say it:**
```
"Swipe up"
"Swipe down"
"Swipe left"
"Swipe right"
```

**Examples in conversation:**
```
You: Swipe left
AVA: Swiping left
     🟢 96%

You: Swipe right
AVA: Swiping right
     🟢 95%
```

---

### Pinch Gestures

**What it does:** Zoom in or out with pinch gestures.

**How to say it:**
```
"Pinch open"
"Zoom in"
"Pinch close"
"Zoom out"
```

**Examples in conversation:**
```
You: Pinch open
AVA: Zooming in
     🟢 94%

You: Zoom out
AVA: Zooming out
     🟢 93%
```

---

### Drag Operations

**What it does:** Drag items on screen.

**How to say it:**
```
"Drag start"
"Start dragging"
"Drag stop"
"Stop dragging"
```

**Examples in conversation:**
```
You: Drag start
AVA: Starting drag
     🟢 92%

You: Drag stop
AVA: Stopping drag
     🟢 93%
```

---

## Tips for Natural Language

### Be Clear and Specific

**Good Examples:**
```
✅ "Call Mom"
✅ "Set timer for 10 minutes"
✅ "Directions to work"
✅ "Text John saying I'm on my way"
```

**Less Clear Examples:**
```
❌ "Call" (who?)
❌ "Timer" (how long?)
❌ "Go somewhere" (where?)
❌ "Send message" (to whom? what message?)
```

### Use Natural Variations

AVA understands many ways to say the same thing:

**Volume:**
- "Volume up" = "Increase volume" = "Louder"

**Music:**
- "Play music" = "Start music" = "Music on"

**Directions:**
- "Directions to work" = "Navigate to work" = "How do I get to work"

### Include Key Information

For best results, include all important details:

**Texting:**
- Include: recipient + message
- "Text Mom saying I'm running late"

**Calendar:**
- Include: event name
- "Schedule meeting with John"

**Timers:**
- Include: duration + unit
- "Set timer for 10 minutes"

### Common Phrase Patterns

Learn these patterns for quick commands:

**Communication:**
- "Call [contact]"
- "Text [contact] saying [message]"
- "Email [address] about [subject]"

**Media:**
- "Play [content]"
- "Next/Previous track"
- "Pause/Resume"

**Navigation:**
- "Directions to [destination]"
- "Find [place type] near me"

**Productivity:**
- "Remind me to [task]"
- "Schedule [event]"
- "Add to do [task]"

---

## Troubleshooting

### AVA Doesn't Understand My Command

**Issue:** Low confidence or wrong action

**Solutions:**

1. **Check your phrasing**
   - Compare to examples in this chapter
   - Use key words: "call", "text", "play", "directions"

2. **Be more specific**
   ```
   ❌ "Music" → 🔴 35% (Too vague)
   ✅ "Play music" → 🟢 97% (Clear intent)
   ```

3. **Include all required information**
   ```
   ❌ "Text saying hello" → 🟡 62% (Missing recipient)
   ✅ "Text Mom saying hello" → 🟢 92% (Complete)
   ```

4. **Teach AVA**
   - If she consistently misunderstands
   - Long-press her response
   - Select "Teach AVA this"
   - Choose the correct intent

---

### Command Opens Wrong App

**Issue:** AVA opens an unexpected app

**Cause:** Your device has multiple apps for that function

**Solution:**

1. Set default apps:
   - Android Settings → Apps → Default Apps
   - Choose your preferred app for each category

2. Grant permissions:
   - Some commands need specific permissions
   - Settings → Apps → AVA → Permissions

---

### Timer Doesn't Work

**Issue:** "Set timer" command fails

**Possible causes:**

1. **No alarm/clock app**
   - Install Google Clock from Play Store
   - It's the standard Android timer app

2. **Permission denied**
   - Grant AVA permission to set alarms
   - Settings → Apps → AVA → Permissions → Alarms

3. **Unclear duration**
   ```
   ❌ "Timer for a bit" → 🔴 38% (Vague)
   ✅ "Timer for 5 minutes" → 🟢 94% (Specific)
   ```

---

### Maps Commands Not Working

**Issue:** Navigation/location commands fail

**Solutions:**

1. **Install Google Maps**
   - Required for most navigation features
   - Download from Play Store

2. **Grant location permission**
   - Settings → Apps → AVA → Permissions → Location
   - Also grant to Google Maps

3. **Enable location services**
   - Android Settings → Location → On

---

### Music Controls Don't Work

**Issue:** Play/pause/next/previous fail

**Possible causes:**

1. **No media playing**
   - Start music in your music app first
   - Then use AVA to control it

2. **Music app doesn't support controls**
   - Try with YouTube Music, Spotify, or Google Play Music
   - These apps support standard Android media controls

3. **Background playback disabled**
   - Check your music app settings
   - Enable background playback

---

### Text/Call Commands Open Wrong Contact

**Issue:** AVA calls/texts the wrong person

**Solutions:**

1. **Use exact contact names**
   ```
   ❌ "Call John" (if you have 3 Johns)
   ✅ "Call John Smith" (specific)
   ```

2. **Check your contacts**
   - Open Contacts app
   - Verify names are correct
   - Remove duplicates

3. **Teach AVA nicknames**
   - If you want to use "Mom" instead of "Mary Johnson"
   - Make sure "Mom" is how she's saved in Contacts

---

## Best Practices

### 1. Start with Common Commands

Learn the basics first:
- "Call [contact]"
- "Play music"
- "Set timer [duration]"
- "Directions to [place]"

### 2. Build Muscle Memory

Use the same phrases repeatedly:
- Day 1-7: Stick to example phrases
- Week 2: Add small variations
- Week 3+: Use natural language

### 3. Teach Variations

Once AVA learns a command, teach her your variations:

```
Day 1: "Play music" → teach_intent: play_music
Day 2: "Start music" → teach_intent: play_music
Day 3: "Turn on music" → teach_intent: play_music
```

### 4. Use Complete Sentences

Better results with full phrases:

```
✅ "Set timer for 10 minutes"
   vs
❌ "Timer 10"
```

### 5. Test in Quiet Environment

When teaching AVA:
- Use clear text (no typos)
- Try command 2-3 times
- Teach if confidence stays low

---

## Quick Reference

### Most Common Commands

**Daily Use:**
```
"Call [contact]"
"Text [contact] saying [message]"
"Set timer [duration]"
"Play music"
"Directions to [place]"
"Search for [query]"
```

**Device Control:**
```
"Volume up/down"
"Brightness up/down"
"WiFi on/off"
"Bluetooth on/off"
"Flashlight on/off"
```

**Productivity:**
```
"Remind me to [task]"
"Schedule [event]"
"Add to do [task]"
"Take a note [content]"
"Check calendar"
```

**Media:**
```
"Play/Pause/Resume"
"Next/Previous track"
"Play video [query]"
```

**Navigation:**
```
"Directions to [place]"
"Find [place type] near me"
"Show traffic"
"Share my location"
```

---

## Coverage Summary

**126 Voice Commands Available:**

| Category | Commands | Description |
|----------|----------|-------------|
| Communication | 3 | Calls, texts, emails |
| Device Control | 12 | WiFi, Bluetooth, brightness, flashlight |
| Media | 10 | Play, pause, skip, volume, shuffle |
| Navigation | 8 | Directions, nearby places, traffic |
| Productivity | 6 | Reminders, calendar, notes, to-dos |
| Web & General | 3 | Search, open URLs |
| Accessibility | 25 | Cursor, selection, clicks, dictation, gaze |
| Keyboard | 12 | Show/hide, switch, text editing |
| Gestures | 15 | Scroll, swipe, pinch, drag |
| System | 32 | Volume levels, reboot, notifications |

**Coming Soon:**
- Smart home control (lights, thermostats)
- Advanced entity extraction (specific songs, times)
- Voice input (hands-free commands)

---

## Feedback & Support

**Found a bug?**
- Email: bugs@augmentalis.com

**Have suggestions?**
- Email: feedback@augmentalis.com

**Need help?**
- Check the main User Manual: `/docs/active/User-Manual.md`
- Email: support@augmentalis.com

---

**Thank you for using AVA AI!**

Voice commands make AVA more powerful every day. Keep teaching her, and she'll keep getting smarter!

---

*Chapter Version: 1.5*
*Last Updated: December 2, 2025*
*For questions: docs@augmentalis.com*
