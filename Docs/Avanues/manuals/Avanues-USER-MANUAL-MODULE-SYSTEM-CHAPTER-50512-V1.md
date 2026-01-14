# AVAMagic User Manual - Module System Chapter

**Version:** 1.0.0
**Last Updated:** 2025-12-05
**Chapter:** Using Platform Modules in Plugins
**Target Audience:** Plugin Designers, Non-Technical Users, Beginners

---

## What are Modules?

Modules let your AVAMagic plugins access your phone's features like **voice recognition**, **device sensors**, **browser control**, and more. Think of them as "bridges" that connect your plugin to the real world.

```
┌─────────────────────────────────────────────────────────────────┐
│                    WHAT MODULES CAN DO                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  @voice - Talk to your phone                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ "Hey, what's the weather?" → @voice.listen()            │   │
│  │ Plugin speaks back         → @voice.speak("It's sunny") │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  @device - Know about the device                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ What phone is this?        → @device.info()             │   │
│  │ Screen size?               → @device.screen.width()     │   │
│  │ Battery level?             → @device.battery.level()    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  @browser - Control web browsing                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Open a website             → @browser.open("google.com")│   │
│  │ Search the web             → @browser.search("recipes") │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  @data - Remember things                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Save a setting             → @data.set("theme", "dark") │   │
│  │ Recall a setting           → @data.get("theme")         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Table of Contents

1. [Quick Start: Your First Module Call](#1-quick-start-your-first-module-call)
2. [Available Modules](#2-available-modules)
3. [Voice Module (@voice)](#3-voice-module-voice)
4. [Device Module (@device)](#4-device-module-device)
5. [Browser Module (@browser)](#5-browser-module-browser)
6. [Data Module (@data)](#6-data-module-data)
7. [Localization Module (@localization)](#7-localization-module-localization)
8. [App Module (@app)](#8-app-module-app)
9. [Common Examples](#9-common-examples)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Quick Start: Your First Module Call

Using a module is as simple as typing `@module.method()`:

```yaml
# Show the device platform
platform: @device.platform()

# Result: "android" or "ios"
```

### The @ Symbol

The `@` symbol tells AVAMagic "this is a module call, not regular text":

| Syntax | Meaning |
|--------|---------|
| `@voice` | The voice module |
| `@device` | The device module |
| `@browser` | The browser module |
| `@data` | The data storage module |
| `@localization` | The translation module |
| `@app` | The app integration module |

### Calling Methods

After the module name, add a `.` and the method name:

```yaml
@voice.listen()           # Listen for speech
@device.info()            # Get device info
@browser.open("url")      # Open a website
```

### With Arguments

Some methods need extra information (arguments):

```yaml
# No arguments needed
@device.platform()

# One argument
@voice.speak("Hello!")
@browser.open("https://google.com")

# Multiple arguments
@data.cache.set("key", "value", 60000)
```

---

## 2. Available Modules

Here's what each module does:

| Module | Symbol | Purpose | Example |
|--------|--------|---------|---------|
| Voice | `@voice` | Speech recognition & TTS | `@voice.listen()` |
| Device | `@device` | Device info & sensors | `@device.battery.level()` |
| Browser | `@browser` | Web browsing control | `@browser.open(url)` |
| Data | `@data` | Save and load data | `@data.get("key")` |
| Localization | `@localization` | Translations & formatting | `@localization.t("hello")` |
| App | `@app` | Share, clipboard, notifications | `@app.share(data)` |
| Command | `@command` | Voice command execution | `@command.execute("...")` |

---

## 3. Voice Module (@voice)

Let your plugin listen and speak!

### Listen for Speech

```yaml
# Start listening and get what the user says
transcript: @voice.listen()

# Use the result
Text("You said: " + $state.transcript)
```

### Speak Text

```yaml
# Make the phone speak
@voice.speak("Hello! How can I help you?")

# Speak dynamic content
@voice.speak("The temperature is " + $state.temperature + " degrees")
```

### Check Status

```yaml
# Is the phone currently listening?
isActive: @voice.isListening()

# Show different UI based on status
Btn(
  text: $if($state.isActive, "Listening...", "Start Listening"),
  @tap->toggleListening
)
```

### Available Voice Methods

| Method | What it does | Example |
|--------|--------------|---------|
| `listen()` | Listen for speech, returns text | `@voice.listen()` |
| `speak(text)` | Speak text out loud | `@voice.speak("Hello")` |
| `isListening()` | Check if currently listening | `@voice.isListening()` |
| `stop()` | Stop listening | `@voice.stop()` |
| `engines()` | List available speech engines | `@voice.engines()` |

---

## 4. Device Module (@device)

Get information about the user's device.

### Basic Device Info

```yaml
# Get device information
deviceInfo: @device.info()

# Access specific properties
Text("Model: " + $state.deviceInfo.model)
Text("Manufacturer: " + $state.deviceInfo.manufacturer)
```

### Platform Detection

```yaml
# Which platform is this?
platform: @device.platform()

# Use for platform-specific UI
$if($state.platform == "ios") {
  # iOS-specific component
} else {
  # Android/other component
}
```

### Screen Size

```yaml
# Get screen dimensions
screenWidth: @device.screen.width()
screenHeight: @device.screen.height()

# Use for responsive layouts
$if($state.screenWidth > 600) {
  # Tablet layout
} else {
  # Phone layout
}
```

### Battery Status

```yaml
# Get battery level (0-100)
batteryLevel: @device.battery.level()

# Check if charging
isCharging: @device.battery.isCharging()

# Show warning if low
$if($state.batteryLevel < 20) {
  Alert(title:"Low Battery", message:"Please charge your device")
}
```

### Network Status

```yaml
# Check internet connection
hasInternet: @device.network.isConnected()

# Get network type
networkType: @device.network.type()

# Show offline message
$if(!$state.hasInternet) {
  Banner(text:"You're offline", type:warning)
}
```

### Available Device Methods

| Method | What it does | Returns |
|--------|--------------|---------|
| `info()` | Device details | Model, manufacturer, OS |
| `platform()` | Platform name | "android", "ios", "web" |
| `isTablet()` | Is it a tablet? | true/false |
| `screen.width()` | Screen width | Number (dp) |
| `screen.height()` | Screen height | Number (dp) |
| `screen.density()` | Pixel density | Number |
| `battery.level()` | Battery percentage | 0-100 |
| `battery.isCharging()` | Charging status | true/false |
| `network.isConnected()` | Has internet? | true/false |
| `network.type()` | Connection type | "wifi", "cellular", "none" |

---

## 5. Browser Module (@browser)

Control web browsing from your plugin.

### Open Websites

```yaml
# Open a URL
@browser.open("https://google.com")

# Open with a variable
@browser.open($state.articleUrl)
```

### Search the Web

```yaml
# Search for something
@browser.search("best pizza near me")

# Search with user input
@browser.search($state.searchQuery)
```

### Get Page Info

```yaml
# Get current page title
pageTitle: @browser.page.title()

# Get current URL
currentUrl: @browser.page.url()
```

### Tab Management

```yaml
# List open tabs
tabs: @browser.tab.list()

# Open new tab
@browser.tab.new("https://docs.example.com")

# Get current tab
currentTab: @browser.tab.current()
```

### Bookmarks

```yaml
# List bookmarks
bookmarks: @browser.bookmark.list()

# Add bookmark
@browser.bookmark.add($state.currentUrl, "My Favorite Page")
```

### Available Browser Methods

| Method | What it does | Example |
|--------|--------------|---------|
| `open(url)` | Open URL | `@browser.open("https://...")` |
| `search(query)` | Search web | `@browser.search("recipes")` |
| `back()` | Go back | `@browser.back()` |
| `forward()` | Go forward | `@browser.forward()` |
| `reload()` | Refresh page | `@browser.reload()` |
| `tab.list()` | List tabs | `@browser.tab.list()` |
| `tab.new(url)` | New tab | `@browser.tab.new(url)` |
| `bookmark.list()` | Get bookmarks | `@browser.bookmark.list()` |
| `page.title()` | Page title | `@browser.page.title()` |
| `page.url()` | Page URL | `@browser.page.url()` |

---

## 6. Data Module (@data)

Save and retrieve data that persists between sessions.

### Basic Storage

```yaml
# Save a value
@data.set("username", "John")
@data.set("darkMode", true)
@data.set("fontSize", 16)

# Get a value
savedName: @data.get("username")
isDarkMode: @data.get("darkMode")
```

### Check and Remove

```yaml
# Check if key exists
hasUsername: @data.has("username")

# Remove a value
@data.remove("username")

# Get all keys
allKeys: @data.keys()
```

### Secure Storage (for passwords, tokens)

```yaml
# Store sensitive data securely
@data.secure.set("authToken", $loginResponse.token)

# Retrieve secure data
token: @data.secure.get("authToken")

# Remove secure data
@data.secure.remove("authToken")
```

### Caching (temporary storage)

```yaml
# Cache data for 5 minutes (300000 ms)
@data.cache.set("weather", $weatherData, 300000)

# Get cached data (returns null if expired)
cachedWeather: @data.cache.get("weather")

# Clear all cache
@data.cache.clear()
```

### Available Data Methods

| Method | What it does | Example |
|--------|--------------|---------|
| `get(key)` | Get stored value | `@data.get("theme")` |
| `set(key, value)` | Store value | `@data.set("theme", "dark")` |
| `remove(key)` | Delete value | `@data.remove("theme")` |
| `has(key)` | Check if exists | `@data.has("theme")` |
| `keys()` | List all keys | `@data.keys()` |
| `clear()` | Delete everything | `@data.clear()` |
| `secure.get(key)` | Get secure value | `@data.secure.get("token")` |
| `secure.set(key, val)` | Store securely | `@data.secure.set("token", t)` |
| `cache.get(key)` | Get cached value | `@data.cache.get("weather")` |
| `cache.set(k, v, ttl)` | Cache with timeout | `@data.cache.set(k, v, 60000)` |

---

## 7. Localization Module (@localization)

Support multiple languages in your plugin.

### Translate Text

```yaml
# Basic translation
greeting: @localization.t("hello_world")

# With parameters
welcome: @localization.t("welcome_user", { name: "John" })
```

### Get/Set Language

```yaml
# Get current language
currentLang: @localization.locale()

# Change language
@localization.setLocale("es")  # Spanish
@localization.setLocale("fr")  # French
@localization.setLocale("ja")  # Japanese

# List available languages
languages: @localization.availableLocales()
```

### Format Numbers and Dates

```yaml
# Format a number (adds commas, etc.)
formattedNumber: @localization.format.number(1234567)
# Result: "1,234,567" (en-US) or "1.234.567" (de-DE)

# Format currency
price: @localization.format.currency(99.99, "USD")
# Result: "$99.99" (en-US) or "99,99 $" (fr-FR)

# Format date
dateStr: @localization.format.date($state.createdAt)
# Result: "12/5/2025" (en-US) or "5/12/2025" (en-GB)

# Relative time
relativeTime: @localization.format.relative($state.lastUpdate)
# Result: "2 hours ago" or "in 3 days"
```

### Right-to-Left Languages

```yaml
# Check if current language is RTL (Arabic, Hebrew, etc.)
isRTL: @localization.isRTL()

# Get text direction
direction: @localization.direction()
# Returns: "ltr" or "rtl"

# Use in layout
Row(direction: @localization.direction()) {
  # Content adapts to language direction
}
```

### Available Localization Methods

| Method | What it does | Example |
|--------|--------------|---------|
| `t(key)` | Translate text | `@localization.t("hello")` |
| `t(key, params)` | Translate with variables | `@localization.t("hi", {name: "Jo"})` |
| `locale()` | Get current language | `@localization.locale()` |
| `setLocale(code)` | Change language | `@localization.setLocale("es")` |
| `availableLocales()` | List languages | `@localization.availableLocales()` |
| `isRTL()` | Is right-to-left? | `@localization.isRTL()` |
| `direction()` | Text direction | `@localization.direction()` |
| `format.number(n)` | Format number | `@localization.format.number(1000)` |
| `format.currency(n, c)` | Format money | `@localization.format.currency(10, "EUR")` |
| `format.date(d)` | Format date | `@localization.format.date(date)` |
| `format.relative(d)` | Relative time | `@localization.format.relative(d)` |

---

## 8. App Module (@app)

Integrate with other apps and system features.

### Share Content

```yaml
# Share text
@app.share({ text: "Check out this app!" })

# Share with title and URL
@app.share({
  title: "Great Article",
  text: "You should read this",
  url: "https://example.com/article"
})

# Share to specific app
@app.share.to("twitter", { text: "Hello Twitter!" })
```

### Clipboard

```yaml
# Copy to clipboard
@app.clipboard.set("Hello, copied!")

# Get clipboard content
clipboardText: @app.clipboard.get()
```

### Notifications

```yaml
# Show notification
@app.notification.show("Download Complete", "Your file is ready")

# Cancel notification by ID
@app.notification.cancel(notificationId)
```

### Open Settings

```yaml
# Open WiFi settings
@app.openSettings("wifi")

# Open Bluetooth settings
@app.openSettings("bluetooth")

# Open app settings
@app.openSettings("app")
```

### Available App Methods

| Method | What it does | Example |
|--------|--------------|---------|
| `share(data)` | Share content | `@app.share({text: "Hi"})` |
| `share.to(app, data)` | Share to app | `@app.share.to("email", d)` |
| `clipboard.get()` | Get clipboard | `@app.clipboard.get()` |
| `clipboard.set(text)` | Copy to clipboard | `@app.clipboard.set("Hi")` |
| `notification.show(t, b)` | Show notification | `@app.notification.show("Title", "Body")` |
| `openSettings(panel)` | Open settings | `@app.openSettings("wifi")` |
| `open(package)` | Open app | `@app.open("com.app.id")` |

---

## 9. Common Examples

### Voice Note App

```yaml
@mel/1.0
id: voice-notes
name: Voice Notes

state:
  notes: []
  isRecording: false

reducers:
  record:
    - isRecording = true
    - transcript = @voice.listen()
    - notes = $array.push($state.notes, $state.transcript)
    - isRecording = false

ui:
  Col(p:16) {
    Btn(
      text: $if($state.isRecording, "Recording...", "Record Note"),
      @tap->record
    )

    $for(note in $state.notes) {
      Card { Text($note) }
    }
  }
```

### Device Info Screen

```yaml
@mel/1.0
id: device-info

state:
  info: {}

reducers:
  loadInfo:
    - info = @device.info()

ui:
  Col(p:16) {
    Text("Device Information", style:h1)

    Card {
      Row { Text("Model:"); Text(@device.info().model) }
      Row { Text("Platform:"); Text(@device.platform()) }
      Row { Text("Battery:"); Text(@device.battery.level() + "%") }
      Row { Text("Network:"); Text(@device.network.type()) }
    }
  }
```

### Multi-Language App

```yaml
@mel/1.0
id: multilang-app

state:
  locale: "en"

reducers:
  changeLanguage:
    params: [code]
    - @localization.setLocale($code)
    - locale = $code

ui:
  Col(p:16) {
    Text(@localization.t("welcome"), style:h1)
    Text(@localization.t("description"))

    Row(gap:8) {
      Btn(text:"English", @tap->changeLanguage("en"))
      Btn(text:"Spanish", @tap->changeLanguage("es"))
      Btn(text:"French", @tap->changeLanguage("fr"))
    }
  }
```

---

## 10. Troubleshooting

### "Module not available"

**Problem:** You see an error like "Module 'voice' is not available on this platform"

**Solution:** Some modules aren't available on all platforms. Check the platform support:

| Module | Android | iOS | Web | Desktop |
|--------|---------|-----|-----|---------|
| @voice | Yes | Limited | No | No |
| @device | Yes | Yes | Limited | Yes |
| @browser | Yes | Yes | Yes | Yes |
| @data | Yes | Yes | Yes | Yes |
| @localization | Yes | Yes | Yes | Yes |
| @app | Yes | Limited | Limited | Limited |
| @command | Yes | No | No | No |

### "Method not available in tier DATA"

**Problem:** You see "Method '@command.execute' is not available in tier DATA"

**Solution:** Some methods only work in "Logic" tier plugins. Change your plugin tier:

```yaml
@mel/1.0
id: my-plugin
tier: logic  # Change from 'data' to 'logic'
```

Note: Logic tier plugins may not be allowed on iOS App Store.

### "Arguments missing"

**Problem:** You see "Missing required argument: url"

**Solution:** Check the method's required arguments:

```yaml
# Wrong - missing URL
@browser.open()

# Correct - URL provided
@browser.open("https://example.com")
```

### Module returns null

**Problem:** A module call returns null or empty

**Solution:**
1. Make sure the device has the capability (e.g., microphone for voice)
2. Check if permissions are granted
3. Verify the module is initialized

```yaml
# Check before using
$if(@device.network.isConnected()) {
  # Safe to make network calls
}
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────┐
│                    MODULE QUICK REFERENCE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  VOICE (@voice)                                                 │
│  @voice.listen()          Listen for speech                     │
│  @voice.speak("text")     Speak text                            │
│  @voice.isListening()     Check status                          │
│                                                                 │
│  DEVICE (@device)                                               │
│  @device.platform()       Get platform name                     │
│  @device.screen.width()   Get screen width                      │
│  @device.battery.level()  Get battery %                         │
│  @device.network.type()   Get connection type                   │
│                                                                 │
│  BROWSER (@browser)                                             │
│  @browser.open(url)       Open website                          │
│  @browser.search(query)   Web search                            │
│  @browser.page.title()    Current page title                    │
│                                                                 │
│  DATA (@data)                                                   │
│  @data.get(key)           Get stored value                      │
│  @data.set(key, val)      Store value                           │
│  @data.secure.get(key)    Get secure value                      │
│                                                                 │
│  LOCALIZATION (@localization)                                   │
│  @localization.t(key)     Translate text                        │
│  @localization.locale()   Current language                      │
│  @localization.format.currency(n, c)  Format money              │
│                                                                 │
│  APP (@app)                                                     │
│  @app.share({text: "..."}) Share content                        │
│  @app.clipboard.set(t)    Copy to clipboard                     │
│  @app.notification.show() Show notification                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Need More Help?

- **Developer Manual:** See the [Module System Developer Chapter](DEVELOPER-MANUAL-MODULE-SYSTEM-CHAPTER.md) for technical details
- **Examples:** Check the `/examples/plugins/` folder for complete examples
- **Community:** Ask questions in our community forum
