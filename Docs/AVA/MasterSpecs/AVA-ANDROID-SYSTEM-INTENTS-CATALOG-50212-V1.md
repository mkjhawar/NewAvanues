# Android System Intents Catalog for AVA/VoiceOS

**Created:** 2025-12-02
**Updated:** 2025-12-02
**Purpose:** Comprehensive catalog of Android system settings and Google app intents for voice command integration
**Status:** PENDING IMPLEMENTATION
**Total Coverage:**
- 102 Settings.ACTION_* constants (open settings screens)
- 60+ Intent.ACTION_* constants (standard actions)
- 106 Settings.System constants (user-modifiable values)
- 55 Settings.Secure constants (system-protected values)
- 45 Settings.Global constants (device-wide values)
- Google Maps, YouTube, Gmail, Chrome, Play Store intents
- Settings.Panel quick settings (API 29+)

---

## Implementation Workflow

```
1. Add to .ava files     →  2. Generate embeddings  →  3. Update .aot backup
   (intent definitions)      (run Python tool)          (bundle in APK)
```

**Tools:**
- Intent definitions: `android/ava/src/main/assets/intents/*.ava`
- Embedding generator: `tools/embedding-generator/generate_embeddings.py`
- Output: `android/ava/src/main/assets/embeddings/bundled_embeddings.aot`

---

## 1. Complete Settings ACTION Constants (102 Total)

**Source:** [Microsoft Learn - Android.Provider.Settings](https://learn.microsoft.com/en-us/dotnet/api/android.provider.settings?view=net-android-34.0)

### 1.0 COMPLETE LIST - All Settings.ACTION_* Constants

| # | Constant | Action String | API | Category |
|---|----------|---------------|-----|----------|
| 1 | `ACTION_ACCESSIBILITY_SETTINGS` | `android.settings.ACCESSIBILITY_SETTINGS` | 5 | Accessibility |
| 2 | `ACTION_ADD_ACCOUNT` | `android.settings.ADD_ACCOUNT_SETTINGS` | 5 | Accounts |
| 3 | `ACTION_ADVANCED_MEMORY_PROTECTION_SETTINGS` | `android.settings.ADVANCED_MEMORY_PROTECTION_SETTINGS` | 34 | Security |
| 4 | `ACTION_AIRPLANE_MODE_SETTINGS` | `android.settings.AIRPLANE_MODE_SETTINGS` | 3 | Network |
| 5 | `ACTION_ALL_APPS_NOTIFICATION_SETTINGS` | `android.settings.ALL_APPS_NOTIFICATION_SETTINGS` | 33 | Notifications |
| 6 | `ACTION_APN_SETTINGS` | `android.settings.APN_SETTINGS` | 1 | Network |
| 7 | `ACTION_APPLICATION_DETAILS_SETTINGS` | `android.settings.APPLICATION_DETAILS_SETTINGS` | 9 | Apps |
| 8 | `ACTION_APPLICATION_DEVELOPMENT_SETTINGS` | `android.settings.APPLICATION_DEVELOPMENT_SETTINGS` | 3 | Developer |
| 9 | `ACTION_APPLICATION_SETTINGS` | `android.settings.APPLICATION_SETTINGS` | 1 | Apps |
| 10 | `ACTION_APP_LOCALE_SETTINGS` | `android.settings.APP_LOCALE_SETTINGS` | 33 | Language |
| 11 | `ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS` | `android.settings.APP_NOTIFICATION_BUBBLE_SETTINGS` | 29 | Notifications |
| 12 | `ACTION_APP_NOTIFICATION_SETTINGS` | `android.settings.APP_NOTIFICATION_SETTINGS` | 26 | Notifications |
| 13 | `ACTION_APP_OPEN_BY_DEFAULT_SETTINGS` | `android.settings.APP_OPEN_BY_DEFAULT_SETTINGS` | 31 | Apps |
| 14 | `ACTION_APP_SEARCH_SETTINGS` | `android.settings.APP_SEARCH_SETTINGS` | 32 | Search |
| 15 | `ACTION_APP_USAGE_SETTINGS` | `android.settings.APP_USAGE_SETTINGS` | 29 | Apps |
| 16 | `ACTION_AUTOMATIC_ZEN_RULE_SETTINGS` | `android.settings.AUTOMATIC_ZEN_RULE_SETTINGS` | 35 | DND |
| 17 | `ACTION_AUTO_ROTATE_SETTINGS` | `android.settings.AUTO_ROTATE_SETTINGS` | 32 | Display |
| 18 | `ACTION_BATTERY_SAVER_SETTINGS` | `android.settings.BATTERY_SAVER_SETTINGS` | 22 | Battery |
| 19 | `ACTION_BIOMETRIC_ENROLL` | `android.settings.BIOMETRIC_ENROLL` | 30 | Security |
| 20 | `ACTION_BLUETOOTH_SETTINGS` | `android.settings.BLUETOOTH_SETTINGS` | 1 | Network |
| 21 | `ACTION_CAPTIONING_SETTINGS` | `android.settings.CAPTIONING_SETTINGS` | 19 | Accessibility |
| 22 | `ACTION_CAST_SETTINGS` | `android.settings.CAST_SETTINGS` | 21 | Display |
| 23 | `ACTION_CHANNEL_NOTIFICATION_SETTINGS` | `android.settings.CHANNEL_NOTIFICATION_SETTINGS` | 26 | Notifications |
| 24 | `ACTION_CONDITION_PROVIDER_SETTINGS` | `android.settings.ACTION_CONDITION_PROVIDER_SETTINGS` | 24 | DND |
| 25 | `ACTION_CREDENTIAL_PROVIDER` | `android.settings.CREDENTIAL_PROVIDER_SETTINGS` | 34 | Security |
| 26 | `ACTION_DATA_ROAMING_SETTINGS` | `android.settings.DATA_ROAMING_SETTINGS` | 3 | Network |
| 27 | `ACTION_DATA_USAGE_SETTINGS` | `android.settings.DATA_USAGE_SETTINGS` | 28 | Network |
| 28 | `ACTION_DATE_SETTINGS` | `android.settings.DATE_SETTINGS` | 1 | System |
| 29 | `ACTION_DEVICE_INFO_SETTINGS` | `android.settings.DEVICE_INFO_SETTINGS` | 8 | System |
| 30 | `ACTION_DISPLAY_SETTINGS` | `android.settings.DISPLAY_SETTINGS` | 1 | Display |
| 31 | `ACTION_DREAM_SETTINGS` | `android.settings.DREAM_SETTINGS` | 18 | Display |
| 32 | `ACTION_FINGERPRINT_ENROLL` | `android.settings.FINGERPRINT_ENROLL` | 28 | Security |
| 33 | `ACTION_FIRST_DAY_OF_WEEK_SETTINGS` | `android.settings.FIRST_DAY_OF_WEEK_SETTINGS` | 35 | Regional |
| 34 | `ACTION_HARD_KEYBOARD_SETTINGS` | `android.settings.HARD_KEYBOARD_SETTINGS` | 11 | Input |
| 35 | `ACTION_HOME_SETTINGS` | `android.settings.HOME_SETTINGS` | 21 | Apps |
| 36 | `ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS` | `android.settings.IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS` | 24 | Network |
| 37 | `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` | `android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS` | 23 | Battery |
| 38 | `ACTION_INPUT_METHOD_SETTINGS` | `android.settings.INPUT_METHOD_SETTINGS` | 3 | Input |
| 39 | `ACTION_INPUT_METHOD_SUBTYPE_SETTINGS` | `android.settings.INPUT_METHOD_SUBTYPE_SETTINGS` | 11 | Input |
| 40 | `ACTION_INTERNAL_STORAGE_SETTINGS` | `android.settings.INTERNAL_STORAGE_SETTINGS` | 3 | Storage |
| 41 | `ACTION_LOCALE_SETTINGS` | `android.settings.LOCALE_SETTINGS` | 1 | Language |
| 42 | `ACTION_LOCATION_SOURCE_SETTINGS` | `android.settings.LOCATION_SOURCE_SETTINGS` | 1 | Location |
| 43 | `ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS` | `android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS` | 9 | Apps |
| 44 | `ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION` | `android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION` | 30 | Storage |
| 45 | `ACTION_MANAGE_ALL_SIM_PROFILES_SETTINGS` | `android.settings.MANAGE_ALL_SIM_PROFILES_SETTINGS` | 33 | Network |
| 46 | `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` | `android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` | 30 | Storage |
| 47 | `ACTION_MANAGE_APPLICATIONS_SETTINGS` | `android.settings.MANAGE_APPLICATIONS_SETTINGS` | 3 | Apps |
| 48 | `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` | `android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT` | 34 | Notifications |
| 49 | `ACTION_MANAGE_DEFAULT_APPS_SETTINGS` | `android.settings.MANAGE_DEFAULT_APPS_SETTINGS` | 24 | Apps |
| 50 | `ACTION_MANAGE_OVERLAY_PERMISSION` | `android.settings.action.MANAGE_OVERLAY_PERMISSION` | 23 | Permissions |
| 51 | `ACTION_MANAGE_SUPERVISOR_RESTRICTED_SETTING` | `android.settings.MANAGE_SUPERVISOR_RESTRICTED_SETTING` | 33 | Parental |
| 52 | `ACTION_MANAGE_UNKNOWN_APP_SOURCES` | `android.settings.MANAGE_UNKNOWN_APP_SOURCES` | 26 | Security |
| 53 | `ACTION_MANAGE_WRITE_SETTINGS` | `android.settings.action.MANAGE_WRITE_SETTINGS` | 23 | Permissions |
| 54 | `ACTION_MEASUREMENT_SYSTEM_SETTINGS` | `android.settings.MEASUREMENT_SYSTEM_SETTINGS` | 35 | Regional |
| 55 | `ACTION_MEMORY_CARD_SETTINGS` | `android.settings.MEMORY_CARD_SETTINGS` | 1 | Storage |
| 56 | `ACTION_NETWORK_OPERATOR_SETTINGS` | `android.settings.NETWORK_OPERATOR_SETTINGS` | 3 | Network |
| 57 | `ACTION_NFC_PAYMENT_SETTINGS` | `android.settings.NFC_PAYMENT_SETTINGS` | 19 | NFC |
| 58 | `ACTION_NFC_SETTINGS` | `android.settings.NFC_SETTINGS` | 16 | NFC |
| 59 | `ACTION_NFCSHARING_SETTINGS` | `android.settings.NFCSHARING_SETTINGS` | 14 | NFC |
| 60 | `ACTION_NIGHT_DISPLAY_SETTINGS` | `android.settings.NIGHT_DISPLAY_SETTINGS` | 26 | Display |
| 61 | `ACTION_NOTIFICATION_ASSISTANT_SETTINGS` | `android.settings.NOTIFICATION_ASSISTANT_SETTINGS` | 29 | Notifications |
| 62 | `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` | `android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS` | 34 | Notifications |
| 63 | `ACTION_NOTIFICATION_LISTENER_SETTINGS` | `android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` | 22 | Notifications |
| 64 | `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` | `android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS` | 23 | DND |
| 65 | `ACTION_PRINT_SETTINGS` | `android.settings.ACTION_PRINT_SETTINGS` | 19 | System |
| 66 | `ACTION_PRIVACY_SETTINGS` | `android.settings.PRIVACY_SETTINGS` | 5 | Privacy |
| 67 | `ACTION_PROCESS_WIFI_EASY_CONNECT_URI` | `android.settings.PROCESS_WIFI_EASY_CONNECT_URI` | 29 | WiFi |
| 68 | `ACTION_QUICK_ACCESS_WALLET_SETTINGS` | `android.settings.QUICK_ACCESS_WALLET_SETTINGS` | 31 | System |
| 69 | `ACTION_QUICK_LAUNCH_SETTINGS` | `android.settings.QUICK_LAUNCH_SETTINGS` | 3 | System |
| 70 | `ACTION_REGIONAL_PREFERENCES_SETTINGS` | `android.settings.REGIONAL_PREFERENCES_SETTINGS` | 35 | Regional |
| 71 | `ACTION_REGION_SETTINGS` | `android.settings.REGION_SETTINGS` | 35 | Regional |
| 72 | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | `android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 23 | Battery |
| 73 | `ACTION_REQUEST_MANAGE_MEDIA` | `android.settings.REQUEST_MANAGE_MEDIA` | 31 | Storage |
| 74 | `ACTION_REQUEST_MEDIA_ROUTING_CONTROL` | `android.settings.REQUEST_MEDIA_ROUTING_CONTROL` | 34 | Media |
| 75 | `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` | `android.settings.REQUEST_SCHEDULE_EXACT_ALARM` | 31 | Alarms |
| 76 | `ACTION_REQUEST_SET_AUTOFILL_SERVICE` | `android.settings.REQUEST_SET_AUTOFILL_SERVICE` | 26 | Autofill |
| 77 | `ACTION_SATELLITE_SETTING` | `android.settings.SATELLITE_SETTING` | 34 | Network |
| 78 | `ACTION_SEARCH_SETTINGS` | `android.search.action.SEARCH_SETTINGS` | 1 | Search |
| 79 | `ACTION_SECURITY_SETTINGS` | `android.settings.SECURITY_SETTINGS` | 1 | Security |
| 80 | `ACTION_SETTINGS` | `android.settings.SETTINGS` | 1 | System |
| 81 | `ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY` | `android.settings.SETTINGS_EMBED_DEEP_LINK_ACTIVITY` | 32 | System |
| 82 | `ACTION_SHOW_REGULATORY_INFO` | `android.settings.SHOW_REGULATORY_INFO` | 21 | System |
| 83 | `ACTION_SHOW_WORK_POLICY_INFO` | `android.settings.SHOW_WORK_POLICY_INFO` | 30 | Work |
| 84 | `ACTION_SOUND_SETTINGS` | `android.settings.SOUND_SETTINGS` | 1 | Sound |
| 85 | `ACTION_STORAGE_VOLUME_ACCESS_SETTINGS` | `android.settings.STORAGE_VOLUME_ACCESS_SETTINGS` | 30 | Storage |
| 86 | `ACTION_SYNC_SETTINGS` | `android.settings.SYNC_SETTINGS` | 1 | Accounts |
| 87 | `ACTION_TEMPERATURE_UNIT_SETTINGS` | `android.settings.TEMPERATURE_UNIT_SETTINGS` | 35 | Regional |
| 88 | `ACTION_USAGE_ACCESS_SETTINGS` | `android.settings.USAGE_ACCESS_SETTINGS` | 21 | Permissions |
| 89 | `ACTION_USER_DICTIONARY_SETTINGS` | `android.settings.USER_DICTIONARY_SETTINGS` | 3 | Input |
| 90 | `ACTION_VOICE_CONTROL_AIRPLANE_MODE` | `android.settings.VOICE_CONTROL_AIRPLANE_MODE` | 23 | Voice |
| 91 | `ACTION_VOICE_CONTROL_BATTERY_SAVER_MODE` | `android.settings.VOICE_CONTROL_BATTERY_SAVER_MODE` | 23 | Voice |
| 92 | `ACTION_VOICE_CONTROL_DO_NOT_DISTURB_MODE` | `android.settings.VOICE_CONTROL_DO_NOT_DISTURB_MODE` | 23 | Voice |
| 93 | `ACTION_VOICE_INPUT_SETTINGS` | `android.settings.VOICE_INPUT_SETTINGS` | 21 | Input |
| 94 | `ACTION_VPN_SETTINGS` | `android.settings.VPN_SETTINGS` | 24 | Network |
| 95 | `ACTION_VR_LISTENER_SETTINGS` | `android.settings.VR_LISTENER_SETTINGS` | 24 | VR |
| 96 | `ACTION_WEBVIEW_SETTINGS` | `android.settings.WEBVIEW_SETTINGS` | 24 | Apps |
| 97 | `ACTION_WIFI_ADD_NETWORKS` | `android.settings.WIFI_ADD_NETWORKS` | 30 | WiFi |
| 98 | `ACTION_WIFI_IP_SETTINGS` | `android.settings.WIFI_IP_SETTINGS` | 3 | WiFi |
| 99 | `ACTION_WIFI_SETTINGS` | `android.settings.WIFI_SETTINGS` | 1 | WiFi |
| 100 | `ACTION_WIRELESS_SETTINGS` | `android.settings.WIRELESS_SETTINGS` | 1 | Network |
| 101 | `ACTION_ZEN_MODE_PRIORITY_SETTINGS` | `android.settings.ZEN_MODE_PRIORITY_SETTINGS` | 28 | DND |
| 102 | `ACTION_TETHER_SETTINGS` | `android.settings.TETHER_PROVISIONING_UI` | 24 | Network |

---

## 2. Settings Intents by Category

### 2.1 Network & Connectivity

| Intent ID | Action String | Example Phrases | Status |
|-----------|---------------|-----------------|--------|
| `open_wifi_settings` | `android.settings.WIFI_SETTINGS` | "open wifi settings", "wifi configuration", "connect to wifi" | PENDING |
| `open_bluetooth_settings` | `android.settings.BLUETOOTH_SETTINGS` | "bluetooth settings", "pair device", "connect bluetooth" | PENDING |
| `open_airplane_mode` | `android.settings.AIRPLANE_MODE_SETTINGS` | "airplane mode", "flight mode settings" | PENDING |
| `open_mobile_data` | `android.settings.DATA_ROAMING_SETTINGS` | "mobile data settings", "cellular settings", "data roaming" | PENDING |
| `open_nfc_settings` | `android.settings.NFC_SETTINGS` | "nfc settings", "tap to pay settings" | PENDING |
| `open_hotspot_settings` | `android.settings.TETHER_SETTINGS` | "hotspot settings", "mobile hotspot", "tethering" | PENDING |
| `open_vpn_settings` | `android.settings.VPN_SETTINGS` | "vpn settings", "connect vpn" | PENDING |
| `open_wireless_settings` | `android.settings.WIRELESS_SETTINGS` | "wireless settings", "network settings" | PENDING |

### 1.2 Display & Sound

| Intent ID | Action String | Example Phrases | Status |
|-----------|---------------|-----------------|--------|
| `open_display_settings` | `android.settings.DISPLAY_SETTINGS` | "display settings", "screen brightness", "dark mode" | PENDING |
| `open_sound_settings` | `android.settings.SOUND_SETTINGS` | "sound settings", "volume settings", "ringtone" | PENDING |
| `open_notification_settings` | `android.settings.NOTIFICATION_SETTINGS` | "notification settings", "manage notifications" | PENDING |
| `open_do_not_disturb` | `android.settings.ZEN_MODE_SETTINGS` | "do not disturb", "dnd settings", "quiet mode" | PENDING |

### 1.3 Device & Storage

| Intent ID | Action String | Example Phrases | Status |
|-----------|---------------|-----------------|--------|
| `open_storage_settings` | `android.settings.INTERNAL_STORAGE_SETTINGS` | "storage settings", "free up space", "memory" | PENDING |
| `open_battery_settings` | `android.settings.BATTERY_SAVER_SETTINGS` | "battery settings", "battery saver", "power settings" | PENDING |
| `open_device_info` | `android.settings.DEVICE_INFO_SETTINGS` | "about phone", "device info", "phone information" | PENDING |
| `open_memory_settings` | `android.settings.MEMORY_CARD_SETTINGS` | "sd card settings", "memory card" | PENDING |

### 1.4 Security & Privacy

| Intent ID | Action String | Example Phrases | Status |
|-----------|---------------|-----------------|--------|
| `open_security_settings` | `android.settings.SECURITY_SETTINGS` | "security settings", "lock screen", "fingerprint" | PENDING |
| `open_privacy_settings` | `android.settings.PRIVACY_SETTINGS` | "privacy settings", "permissions" | PENDING |
| `open_location_settings` | `android.settings.LOCATION_SOURCE_SETTINGS` | "location settings", "gps settings", "location services" | PENDING |
| `open_biometric_settings` | `android.settings.BIOMETRIC_ENROLL` | "fingerprint settings", "face unlock", "biometrics" | PENDING |

### 1.5 Apps & Accounts

| Intent ID | Action String | Example Phrases | Status |
|-----------|---------------|-----------------|--------|
| `open_app_settings` | `android.settings.APPLICATION_SETTINGS` | "app settings", "manage apps", "installed apps" | PENDING |
| `open_default_apps` | `android.settings.MANAGE_DEFAULT_APPS_SETTINGS` | "default apps", "default browser", "default launcher" | PENDING |
| `open_account_settings` | `android.settings.SYNC_SETTINGS` | "accounts", "sync settings", "google account" | PENDING |
| `open_add_account` | `android.settings.ADD_ACCOUNT_SETTINGS` | "add account", "new account" | PENDING |

### 1.6 Accessibility & Input

| Intent ID | Action String | Example Phrases | Status |
|-----------|---------------|-----------------|--------|
| `open_accessibility_settings` | `android.settings.ACCESSIBILITY_SETTINGS` | "accessibility settings", "talkback", "screen reader" | PENDING |
| `open_keyboard_settings` | `android.settings.INPUT_METHOD_SETTINGS` | "keyboard settings", "input method", "change keyboard" | PENDING |
| `open_language_settings` | `android.settings.LOCALE_SETTINGS` | "language settings", "change language", "region" | PENDING |

### 1.7 Date & Time

| Intent ID | Action String | Example Phrases | Status |
|-----------|---------------|-----------------|--------|
| `open_date_settings` | `android.settings.DATE_SETTINGS` | "date and time", "set time", "timezone" | PENDING |

### 1.8 Developer Options

| Intent ID | Action String | Example Phrases | Status |
|-----------|---------------|-----------------|--------|
| `open_developer_options` | `android.settings.APPLICATION_DEVELOPMENT_SETTINGS` | "developer options", "developer settings", "usb debugging" | PENDING |

---

## 2. Google Maps Intents

### 2.1 Navigation

| Intent ID | URI Scheme | Example Phrases | Parameters |
|-----------|-----------|-----------------|------------|
| `navigate_to_address` | `google.navigation:q={address}` | "navigate to {address}", "directions to {place}" | `mode=d/b/w/l`, `avoid=t/h/f` |
| `navigate_driving` | `google.navigation:q={dest}&mode=d` | "drive to {place}", "driving directions" | |
| `navigate_walking` | `google.navigation:q={dest}&mode=w` | "walk to {place}", "walking directions" | |
| `navigate_biking` | `google.navigation:q={dest}&mode=b` | "bike to {place}", "cycling directions" | |

### 2.2 Search & Display

| Intent ID | URI Scheme | Example Phrases | Parameters |
|-----------|-----------|-----------------|------------|
| `show_on_map` | `geo:0,0?q={query}` | "show {place} on map", "find {location}" | |
| `show_location` | `geo:{lat},{lng}?z={zoom}` | "show this location", "map of here" | `z`=zoom 0-21 |
| `search_nearby` | `geo:0,0?q={type}+near+me` | "find {restaurants} nearby", "{gas stations} near me" | |

### 2.3 Street View

| Intent ID | URI Scheme | Example Phrases | Parameters |
|-----------|-----------|-----------------|------------|
| `street_view` | `google.streetview:cbll={lat},{lng}` | "street view of {place}", "show street view" | `cbp` for camera |

---

## 3. Communication Intents

### 3.1 Phone

| Intent ID | Action/URI | Example Phrases | Status |
|-----------|-----------|-----------------|--------|
| `make_call` | `Intent.ACTION_DIAL` + `tel:{number}` | "call {contact}", "dial {number}" | PENDING |
| `open_dialer` | `Intent.ACTION_DIAL` | "open phone", "open dialer" | PENDING |
| `open_contacts` | `Intent.ACTION_VIEW` + contacts URI | "open contacts", "show contacts" | PENDING |

### 3.2 Messaging

| Intent ID | Action/URI | Example Phrases | Status |
|-----------|-----------|-----------------|--------|
| `send_sms` | `Intent.ACTION_SENDTO` + `smsto:{number}` | "text {contact}", "send message to {name}" | PENDING |
| `open_messages` | Package: `com.google.android.apps.messaging` | "open messages", "open sms" | PENDING |

### 3.3 Email (Gmail)

| Intent ID | Action | Example Phrases | Extras |
|-----------|--------|-----------------|--------|
| `compose_email` | `Intent.ACTION_SENDTO` + `mailto:` | "compose email", "new email", "send email to {address}" | `EXTRA_EMAIL`, `EXTRA_SUBJECT`, `EXTRA_TEXT` |
| `open_gmail` | Package: `com.google.android.gm` | "open gmail", "check email" | |

---

## 4. Media & Entertainment

### 4.1 YouTube

| Intent ID | URI Scheme | Example Phrases | Status |
|-----------|-----------|-----------------|--------|
| `play_youtube_video` | `vnd.youtube:{video_id}` | "play {video} on youtube" | PENDING |
| `search_youtube` | `https://www.youtube.com/results?search_query={q}` | "search youtube for {query}" | PENDING |
| `open_youtube` | Package: `com.google.android.youtube` | "open youtube" | PENDING |

### 4.2 Music

| Intent ID | Action | Example Phrases | Status |
|-----------|--------|-----------------|--------|
| `play_music` | `INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH` | "play {song}", "play music by {artist}" | PENDING |
| `open_music` | Package varies | "open music app", "open spotify" | PENDING |

### 4.3 Camera

| Intent ID | Action | Example Phrases | Status |
|-----------|--------|-----------------|--------|
| `take_photo` | `MediaStore.ACTION_IMAGE_CAPTURE` | "take a photo", "open camera" | PENDING |
| `record_video` | `MediaStore.ACTION_VIDEO_CAPTURE` | "record video", "take a video" | PENDING |

---

## 5. Productivity

### 5.1 Calendar

| Intent ID | Action | Example Phrases | Extras |
|-----------|--------|-----------------|--------|
| `create_event` | `Intent.ACTION_INSERT` + `Events.CONTENT_URI` | "create event", "add to calendar", "schedule meeting" | `EXTRA_EVENT_BEGIN_TIME`, `title`, `description` |
| `open_calendar` | Package: `com.google.android.calendar` | "open calendar", "show my schedule" | |

### 5.2 Alarm & Timer

| Intent ID | Action | Example Phrases | Extras |
|-----------|--------|-----------------|--------|
| `set_alarm` | `AlarmClock.ACTION_SET_ALARM` | "set alarm for {time}", "wake me up at {time}" | `EXTRA_HOUR`, `EXTRA_MINUTES`, `EXTRA_MESSAGE` |
| `set_timer` | `AlarmClock.ACTION_SET_TIMER` | "set timer for {duration}", "countdown {minutes}" | `EXTRA_LENGTH`, `EXTRA_MESSAGE` |
| `show_alarms` | `AlarmClock.ACTION_SHOW_ALARMS` | "show alarms", "my alarms" | |

### 5.3 Notes & Reminders

| Intent ID | Action | Example Phrases | Status |
|-----------|--------|-----------------|--------|
| `create_note` | `Intent.ACTION_CREATE_NOTE` | "create note", "take a note" | PENDING |
| `set_reminder` | Various | "remind me to {task}", "set reminder" | EXISTING |

---

## 6. Google Play Store

| Intent ID | URI Scheme | Example Phrases | Status |
|-----------|-----------|-----------------|--------|
| `open_play_store` | `market://` | "open play store", "app store" | PENDING |
| `search_apps` | `market://search?q={query}` | "search for {app} in play store" | PENDING |
| `app_details` | `market://details?id={package}` | "show {app} in play store" | PENDING |
| `rate_app` | `market://details?id={package}` | "rate this app", "review app" | PENDING |

---

## 7. Browser (Chrome)

| Intent ID | Action | Example Phrases | Status |
|-----------|--------|-----------------|--------|
| `open_url` | `Intent.ACTION_VIEW` + URL | "open {website}", "go to {url}" | PENDING |
| `web_search` | `Intent.ACTION_WEB_SEARCH` | "search for {query}", "google {query}" | EXISTING |
| `open_chrome` | Package: `com.android.chrome` | "open chrome", "open browser" | PENDING |

---

## 8. System Actions (Quick Settings)

| Intent ID | Action | Example Phrases | Notes |
|-----------|--------|-----------------|-------|
| `toggle_wifi` | Requires system permission | "turn on wifi", "disable wifi" | VoiceOS accessibility |
| `toggle_bluetooth` | Requires system permission | "turn on bluetooth", "disable bluetooth" | VoiceOS accessibility |
| `toggle_flashlight` | Camera2 API | "turn on flashlight", "torch on" | EXISTING |
| `toggle_airplane` | Requires system permission | "airplane mode on/off" | VoiceOS accessibility |
| `adjust_brightness` | Settings.System | "increase brightness", "dim screen" | VoiceOS accessibility |
| `adjust_volume` | AudioManager | "volume up", "mute", "set volume to 50%" | EXISTING |

---

## Implementation Priority

### Phase 1: High Priority (Core Settings)
1. `open_wifi_settings`
2. `open_bluetooth_settings`
3. `open_display_settings`
4. `open_sound_settings`
5. `open_battery_settings`
6. `open_location_settings`

### Phase 2: Navigation & Communication
1. `navigate_to_address`
2. `make_call`
3. `send_sms`
4. `compose_email`
5. `web_search` (enhance existing)

### Phase 3: Google Apps
1. `open_youtube` / `play_youtube_video`
2. `open_gmail`
3. `open_play_store`
4. `open_calendar` / `create_event`

### Phase 4: Extended Settings
1. All remaining settings intents
2. Developer options
3. Accessibility settings

---

## .ava File Format Example

```
# open_wifi_settings.ava
intent: open_wifi_settings
action: android.settings.WIFI_SETTINGS
category: settings
requires_voiceos: false

examples:
- open wifi settings
- wifi settings
- go to wifi
- configure wifi
- connect to wifi
- show wifi networks
- wifi configuration
- wireless settings
- change wifi network
- wifi options

entities: []

response_template: "Opening WiFi settings"
```

---

## Notes for VoiceOS Integration

1. **System toggles** (wifi on/off, bluetooth on/off) require VoiceOS accessibility service
2. **Settings screens** can be opened with standard intents (no special permissions)
3. **Google app intents** require the app to be installed
4. **Navigation intents** should include fallback to web URLs

---

## References

- [Settings API Reference](https://developer.android.com/reference/android/provider/Settings)
- [Common Intents](https://developer.android.com/guide/components/intents-common)
- [Google Maps Intents](https://developer.android.com/guide/components/google-maps-intents)
- [Chrome Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs)

---

## 9. Complete Intent.ACTION_* Constants (60+ Actions)

**Source:** [GitHub Gist - Android Actions](https://gist.github.com/zr0n/dfa1afadf7e785e25d53fc2af7c4eee2)

### 9.1 Core Actions

| Action | Description | Example Use |
|--------|-------------|-------------|
| `ACTION_VIEW` | Display data | Open URL, show contact |
| `ACTION_EDIT` | Edit data | Modify contact |
| `ACTION_PICK` | Select item | Choose contact/file |
| `ACTION_DIAL` | Open dialer | Phone number |
| `ACTION_CALL` | Place call (requires permission) | Direct call |
| `ACTION_SEND` | Send data | Share content |
| `ACTION_SENDTO` | Send to specific | Email/SMS |
| `ACTION_SEND_MULTIPLE` | Send multiple items | Share photos |
| `ACTION_SEARCH` | Perform search | Query |
| `ACTION_WEB_SEARCH` | Web search | Google search |

### 9.2 App Management Actions

| Action | Description |
|--------|-------------|
| `ACTION_MAIN` | Main entry point |
| `ACTION_ALL_APPS` | Show all apps |
| `ACTION_APPLICATION_PREFERENCES` | App preferences |
| `ACTION_INSTALL_PACKAGE` | Install APK |
| `ACTION_UNINSTALL_PACKAGE` | Uninstall app |
| `ACTION_MANAGE_APP_PERMISSIONS` | App permissions |
| `ACTION_MANAGE_PERMISSIONS` | System permissions |
| `ACTION_MANAGE_NETWORK_USAGE` | Network usage |
| `ACTION_SHOW_APP_INFO` | App info screen |

### 9.3 Alarm & Timer Actions

| Action | Description | Extras |
|--------|-------------|--------|
| `ACTION_SET_ALARM` | Create alarm | `EXTRA_HOUR`, `EXTRA_MINUTES`, `EXTRA_MESSAGE` |
| `ACTION_SET_TIMER` | Start timer | `EXTRA_LENGTH`, `EXTRA_MESSAGE` |
| `ACTION_SHOW_ALARMS` | Display alarms | - |
| `ACTION_SHOW_TIMERS` | Display timers | - |
| `ACTION_DISMISS_ALARM` | Dismiss alarm | - |
| `ACTION_DISMISS_TIMER` | Dismiss timer | - |
| `ACTION_SNOOZE_ALARM` | Snooze alarm | - |

### 9.4 Media Actions

| Action | Description |
|--------|-------------|
| `ACTION_MUSIC_PLAYER` | Open music player |
| `ACTION_MEDIA_SEARCH` | Search media |
| `ACTION_RINGTONE_PICKER` | Select ringtone |

### 9.5 Document Actions

| Action | Description | API |
|--------|-------------|-----|
| `ACTION_GET_CONTENT` | Get file content | 1 |
| `ACTION_OPEN_DOCUMENT` | Open document (SAF) | 19 |
| `ACTION_OPEN_DOCUMENT_TREE` | Open folder (SAF) | 21 |
| `ACTION_CREATE_DOCUMENT` | Create new file (SAF) | 19 |
| `ACTION_VIEW_DOWNLOADS` | View downloads | - |

### 9.6 System Actions

| Action | Description |
|--------|-------------|
| `ACTION_POWER_USAGE_SUMMARY` | Battery usage |
| `ACTION_BUG_REPORT` | Submit bug report |
| `ACTION_CHOOSER` | App chooser dialog |
| `ACTION_CREATE_SHORTCUT` | Create shortcut |
| `ACTION_QUICK_VIEW` | Quick preview |
| `ACTION_PROCESS_TEXT` | Text processing |
| `ACTION_VOICE_COMMAND` | Voice command |
| `ACTION_VOICE_ASSIST` | Voice assistant |
| `ACTION_ASSIST` | Assistant |

### 9.7 Communication Actions

| Action | Description |
|--------|-------------|
| `ACTION_CALL_BUTTON` | Call button pressed |
| `ACTION_CALL_EMERGENCY` | Emergency call |
| `ACTION_ANSWER` | Answer call |

---

## 10. Settings.Panel (Quick Settings Panels - API 29+)

| Panel | Action | Description |
|-------|--------|-------------|
| WiFi | `Settings.Panel.ACTION_WIFI` | Quick WiFi panel |
| Internet | `Settings.Panel.ACTION_INTERNET_CONNECTIVITY` | Internet options |
| NFC | `Settings.Panel.ACTION_NFC` | NFC toggle |
| Volume | `Settings.Panel.ACTION_VOLUME` | Volume slider |

**Usage:**
```kotlin
val intent = Intent(Settings.Panel.ACTION_WIFI)
startActivity(intent)
```

---

## 11. VoiceOS Accessibility Requirements

These actions require VoiceOS accessibility service (cannot use standard intents):

| Action | Why Accessibility Required |
|--------|---------------------------|
| Toggle WiFi on/off | System permission |
| Toggle Bluetooth on/off | System permission |
| Toggle Airplane Mode | System permission |
| Toggle Flashlight | Hardware access |
| Adjust System Brightness | Write settings |
| Adjust System Volume | Audio focus |
| Lock Screen | Device admin |
| Take Screenshot | System permission |

---

## References

- [Settings API Reference](https://developer.android.com/reference/android/provider/Settings)
- [Microsoft Learn - Android.Provider.Settings](https://learn.microsoft.com/en-us/dotnet/api/android.provider.settings?view=net-android-34.0)
- [Common Intents](https://developer.android.com/guide/components/intents-common)
- [Google Maps Intents](https://developer.android.com/guide/components/google-maps-intents)
- [Chrome Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs)
- [GitHub Gist - Android Actions](https://gist.github.com/zr0n/dfa1afadf7e785e25d53fc2af7c4eee2)
- [Stack Overflow - Opening Settings](https://stackoverflow.com/questions/19517417/opening-android-settings-programmatically)
- [AOSP Settings.java Source](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/provider/Settings.java)

---

---

## 12. Settings Value Constants (Read/Write System Settings)

**Note:** These are actual setting values, NOT intents. Use `Settings.System.putInt()` / `Settings.System.getInt()` etc.

### 12.1 Settings.System Constants (User-Modifiable, 106 Constants)

**Permission Required:** `WRITE_SETTINGS` (special permission via `ACTION_MANAGE_WRITE_SETTINGS`)

**Source:** [Microsoft Learn - Settings.System](https://learn.microsoft.com/en-us/dotnet/api/android.provider.settings.system?view=net-android-35.0)

| Constant | Value | Description | Type | VoiceOS Use |
|----------|-------|-------------|------|-------------|
| `SCREEN_BRIGHTNESS` | `screen_brightness` | Screen brightness (0-255) | int | Adjust brightness |
| `SCREEN_BRIGHTNESS_MODE` | `screen_brightness_mode` | Auto brightness (0=manual, 1=auto) | int | Toggle auto brightness |
| `SCREEN_OFF_TIMEOUT` | `screen_off_timeout` | Screen timeout in ms | int | Set screen timeout |
| `ACCELEROMETER_ROTATION` | `accelerometer_rotation` | Auto-rotate (0=off, 1=on) | int | Toggle rotation |
| `USER_ROTATION` | `user_rotation` | Manual rotation (0=0°, 1=90°, 2=180°, 3=270°) | int | Set rotation |
| `FONT_SCALE` | `font_scale` | Font scaling factor (1.0 = normal) | float | Adjust font size |
| `VOLUME_MUSIC` | `volume_music` | Music volume (0-15) | int | Set music volume |
| `VOLUME_RING` | `volume_ring` | Ring volume (0-7) | int | Set ringer volume |
| `VOLUME_NOTIFICATION` | `volume_notification` | Notification volume | int | Set notification volume |
| `VOLUME_ALARM` | `volume_alarm` | Alarm volume | int | Set alarm volume |
| `VOLUME_VOICE` | `volume_voice` | Voice call volume | int | Set call volume |
| `VOLUME_SYSTEM` | `volume_system` | System sounds volume | int | Set system volume |
| `SOUND_EFFECTS_ENABLED` | `sound_effects_enabled` | UI sounds (0=off, 1=on) | int | Toggle touch sounds |
| `HAPTIC_FEEDBACK_ENABLED` | `haptic_feedback_enabled` | Vibration feedback (0=off, 1=on) | int | Toggle haptics |
| `DTMF_TONE_WHEN_DIALING` | `dtmf_tone` | Dialpad tones (0=off, 1=on) | int | Toggle dial tones |
| `VIBRATE_ON` | `vibrate_on` | Vibrate mode enabled | int | Toggle vibration |
| `NOTIFICATION_SOUND` | `notification_sound` | Notification ringtone URI | String | Set notification sound |
| `RINGTONE` | `ringtone` | Phone ringtone URI | String | Set ringtone |
| `ALARM_ALERT` | `alarm_alert` | Alarm sound URI | String | Set alarm sound |
| `TEXT_AUTO_REPLACE` | `auto_replace` | Auto-correct (0=off, 1=on) | int | Toggle autocorrect |
| `TEXT_AUTO_CAPS` | `auto_caps` | Auto-capitalize (0=off, 1=on) | int | Toggle auto-caps |
| `TEXT_AUTO_PUNCTUATE` | `auto_punctuate` | Auto-punctuate (0=off, 1=on) | int | Toggle auto-punctuate |
| `TEXT_SHOW_PASSWORD` | `show_password` | Show password chars briefly | int | Toggle show password |
| `TIME_12_24` | `time_12_24` | 12h or 24h format (12/24) | String | Set time format |
| `DATE_FORMAT` | `date_format` | Date format string | String | Set date format |
| `END_BUTTON_BEHAVIOR` | `end_button_behavior` | Power button behavior | int | - |
| `SETUP_WIZARD_HAS_RUN` | `setup_wizard_has_run` | First boot complete | int | - |
| `APPEND_FOR_LAST_AUDIBLE` | `_last_audible` | Suffix for last audible volume | String | - |

**Deprecated Settings (moved to Settings.Global in API 17):**
- `WIFI_ON` → `Settings.Global.WIFI_ON`
- `BLUETOOTH_ON` → `Settings.Global.BLUETOOTH_ON`
- `AIRPLANE_MODE_ON` → `Settings.Global.AIRPLANE_MODE_ON`
- `AUTO_TIME` → `Settings.Global.AUTO_TIME`
- `DATA_ROAMING` → `Settings.Global.DATA_ROAMING`
- `MODE_RINGER` → `Settings.Global.MODE_RINGER`
- `STAY_ON_WHILE_PLUGGED_IN` → `Settings.Global.STAY_ON_WHILE_PLUGGED_IN`

---

### 12.2 Settings.Secure Constants (System-Protected, ~55 Constants)

**Permission Required:** Cannot write from apps (system-only). Read with no special permission.

**Source:** [MIT Android SDK - Settings.Secure](https://stuff.mit.edu/afs/sipb/project/android/docs/reference/android/provider/Settings.Secure.html)

| Constant | Value | Description | API | VoiceOS Use |
|----------|-------|-------------|-----|-------------|
| `ACCESSIBILITY_ENABLED` | `accessibility_enabled` | Accessibility services enabled | 4 | Check accessibility |
| `ENABLED_ACCESSIBILITY_SERVICES` | `enabled_accessibility_services` | List of enabled services | 4 | Check VoiceOS enabled |
| `TOUCH_EXPLORATION_ENABLED` | `touch_exploration_enabled` | TalkBack explore by touch | 14 | Check TalkBack |
| `ACCESSIBILITY_SPEAK_PASSWORD` | `speak_password` | Speak passwords | 15 | - |
| `ANDROID_ID` | `android_id` | Unique device ID (64-bit hex) | 3 | Device identification |
| `DEFAULT_INPUT_METHOD` | `default_input_method` | Active keyboard ID | 3 | Get current keyboard |
| `ENABLED_INPUT_METHODS` | `enabled_input_methods` | Enabled keyboards | 3 | List keyboards |
| `INPUT_METHOD_SELECTOR_VISIBILITY` | `input_method_selector_visibility` | Keyboard picker display | 11 | - |
| `SELECTED_INPUT_METHOD_SUBTYPE` | `selected_input_method_subtype` | Active keyboard subtype | 11 | - |
| `LOCATION_PROVIDERS_ALLOWED` | `location_providers_allowed` | Enabled location providers | 3 | Check location |
| `LOCATION_MODE` | `location_mode` | Location mode (off/sensors/battery/high) | 19 | Check location mode |
| `LOCK_PATTERN_ENABLED` | `lock_pattern_autolock` | Pattern lock enabled | 8 | Check lock status |
| `LOCK_PATTERN_VISIBLE` | `lock_pattern_visible_pattern` | Show pattern while drawing | 8 | - |
| `TTS_DEFAULT_RATE` | `tts_default_rate` | TTS speech rate (100=1x) | 4 | Get TTS rate |
| `TTS_DEFAULT_PITCH` | `tts_default_pitch` | TTS pitch (100=1x) | 4 | Get TTS pitch |
| `TTS_DEFAULT_SYNTH` | `tts_default_synth` | Default TTS engine | 4 | Get TTS engine |
| `DEVELOPMENT_SETTINGS_ENABLED` | `development_settings_enabled` | Dev options enabled | 16 | Check dev mode |
| `ADB_ENABLED` | `adb_enabled` | USB debugging enabled | 3 | Check debugging |
| `ALLOW_MOCK_LOCATION` | `mock_location` | Mock locations allowed | 3 | - |
| `INSTALL_NON_MARKET_APPS` | `install_non_market_apps` | Unknown sources | 3 | Check sideload |
| `USB_MASS_STORAGE_ENABLED` | `usb_mass_storage_enabled` | USB storage mode | 3 | - |

**Read Example:**
```kotlin
val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
val accessibilityEnabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
```

---

### 12.3 Settings.Global Constants (Device-Wide, ~45 Constants)

**Permission Required:** Cannot write from apps (system-only). Read with no special permission.

**API Level:** Introduced in API 17 (Android 4.2)

**Source:** [MIT Android SDK - Settings.Global](https://stuff.mit.edu/afs/sipb/project/android/docs/reference/android/provider/Settings.Global.html)

| Constant | Value | Description | VoiceOS Use |
|----------|-------|-------------|-------------|
| `AIRPLANE_MODE_ON` | `airplane_mode_on` | Airplane mode (0=off, 1=on) | Check/toggle airplane |
| `AIRPLANE_MODE_RADIOS` | `airplane_mode_radios` | Radios disabled in airplane mode | - |
| `WIFI_ON` | `wifi_on` | WiFi enabled (0=off, 1=on) | Check WiFi |
| `WIFI_SLEEP_POLICY` | `wifi_sleep_policy` | WiFi sleep behavior | - |
| `WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON` | `wifi_networks_available_notification_on` | WiFi notification | - |
| `BLUETOOTH_ON` | `bluetooth_on` | Bluetooth enabled (0=off, 1=on) | Check Bluetooth |
| `DATA_ROAMING` | `data_roaming` | Data roaming enabled | Check roaming |
| `AUTO_TIME` | `auto_time` | Network time sync | Check auto-time |
| `AUTO_TIME_ZONE` | `auto_time_zone` | Network timezone sync | Check auto-zone |
| `ADB_ENABLED` | `adb_enabled` | ADB debugging enabled | Check ADB |
| `DEVELOPMENT_SETTINGS_ENABLED` | `development_settings_enabled` | Developer options | Check dev mode |
| `DEVICE_PROVISIONED` | `device_provisioned` | Device setup complete | - |
| `HTTP_PROXY` | `http_proxy` | Global HTTP proxy | - |
| `INSTALL_NON_MARKET_APPS` | `install_non_market_apps` | Unknown sources | Check sideload |
| `MODE_RINGER` | `mode_ringer` | Ringer mode (0=silent, 1=vibrate, 2=normal) | Check ringer |
| `NETWORK_PREFERENCE` | `network_preference` | Preferred network type | - |
| `STAY_ON_WHILE_PLUGGED_IN` | `stay_on_while_plugged_in` | Stay awake while charging | - |
| `USB_MASS_STORAGE_ENABLED` | `usb_mass_storage_enabled` | USB storage mode | - |
| `WAIT_FOR_DEBUGGER` | `wait_for_debugger` | Wait for debugger | - |
| `ANIMATOR_DURATION_SCALE` | `animator_duration_scale` | Animation speed scale | - |
| `TRANSITION_ANIMATION_SCALE` | `transition_animation_scale` | Transition animation scale | - |
| `WINDOW_ANIMATION_SCALE` | `window_animation_scale` | Window animation scale | - |
| `ALWAYS_FINISH_ACTIVITIES` | `always_finish_activities` | Don't keep activities | - |
| `DEBUG_APP` | `debug_app` | Debug target app | - |
| `SHOW_PROCESSES` | `show_processes` | Show running processes | - |
| `RADIO_BLUETOOTH` | `bluetooth` | Bluetooth radio constant | - |
| `RADIO_WIFI` | `wifi` | WiFi radio constant | - |
| `RADIO_CELL` | `cell` | Cell radio constant | - |
| `RADIO_NFC` | `nfc` | NFC radio constant | - |

**Read Example:**
```kotlin
val airplaneMode = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
val wifiOn = Settings.Global.getInt(contentResolver, Settings.Global.WIFI_ON, 0)
```

---

### 12.4 Settings Namespace Comparison

| Namespace | Write Permission | Use Case | API |
|-----------|-----------------|----------|-----|
| `Settings.System` | `WRITE_SETTINGS` (special) | User preferences (brightness, sounds, rotation) | 1+ |
| `Settings.Secure` | System-only | Security settings, device ID, accessibility | 3+ |
| `Settings.Global` | System-only | Device-wide settings (wifi, bluetooth, airplane) | 17+ |

**VoiceOS Implementation Strategy:**
1. **Read any setting:** No special permission needed for reads
2. **Write Settings.System:** Request `WRITE_SETTINGS` permission via `ACTION_MANAGE_WRITE_SETTINGS`
3. **Write Settings.Secure/Global:** Requires VoiceOS accessibility service with system-level access

---

## 13. VoiceOS Settings Control Matrix

| Setting | Read | Write | Method |
|---------|------|-------|--------|
| Screen Brightness | ✅ Any app | ⚠️ WRITE_SETTINGS | `Settings.System.SCREEN_BRIGHTNESS` |
| Auto Brightness | ✅ Any app | ⚠️ WRITE_SETTINGS | `Settings.System.SCREEN_BRIGHTNESS_MODE` |
| Screen Timeout | ✅ Any app | ⚠️ WRITE_SETTINGS | `Settings.System.SCREEN_OFF_TIMEOUT` |
| Auto Rotate | ✅ Any app | ⚠️ WRITE_SETTINGS | `Settings.System.ACCELEROMETER_ROTATION` |
| Font Scale | ✅ Any app | ⚠️ WRITE_SETTINGS | `Settings.System.FONT_SCALE` |
| Volumes | ✅ Any app | ✅ AudioManager | Use `AudioManager` API |
| WiFi On/Off | ✅ Any app | ❌ VoiceOS | `WifiManager` (deprecated) or Accessibility |
| Bluetooth On/Off | ✅ Any app | ❌ VoiceOS | `BluetoothAdapter` requires user confirmation |
| Airplane Mode | ✅ Any app | ❌ VoiceOS | System-only |
| Location Mode | ✅ Any app | ❌ Intent only | `ACTION_LOCATION_SOURCE_SETTINGS` |
| Ringer Mode | ✅ Any app | ✅ AudioManager | Use `AudioManager.setRingerMode()` |

---

**Next Steps:**
1. [ ] Create .ava files for Phase 1 intents (high priority settings)
2. [ ] Add VoiceOS accessibility handlers for system toggles
3. [ ] Run embedding generator: `python tools/embedding-generator/generate_embeddings.py`
4. [ ] Update bundled_embeddings.aot
5. [ ] Test on physical device
6. [ ] Add to existing IntentRouter in Actions module
