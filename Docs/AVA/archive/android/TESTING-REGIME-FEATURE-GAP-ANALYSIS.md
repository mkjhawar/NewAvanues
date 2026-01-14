# AVA AI - Testing Regime & Feature Gap Analysis

**Created:** 2025-11-27
**Version:** 1.0
**Purpose:** Identify implemented vs. missing features and create systematic testing plan

---

## Executive Summary

This document analyzes AVA's current capabilities against the AON 3.0 semantic ontology to identify feature gaps and create a comprehensive testing regime.

### Current State
- **28 intents defined** across 5 categories in AON 3.0 ontology (.aot files)
- **120+ action handlers registered** (mostly VoiceOS routing and system controls)
- **Critical gap**: Many AON 3.0 intents lack corresponding action handlers

---

## Browser & Internet Access Analysis

### Current Browser Capabilities

#### ✅ Implemented
- **OpenBrowserActionHandler** (line 246-272, NavigationActionHandlers.kt)
  - Intent: `open_browser`
  - Action: Opens default browser to google.com
  - Uses: `Intent.ACTION_VIEW` with hardcoded URL
  - Status: **BASIC IMPLEMENTATION**

#### ❌ Missing
- **Web Search Handler**
  - Intent defined: `search_web` (productivity.aot)
  - Expected action: Extract query from utterance, perform web search
  - Status: **NOT IMPLEMENTED**
  - Required for: "search for X", "google Y", "look up Z"

- **URL Navigation Handler**
  - Intent: Not defined in ontology
  - Expected action: Navigate to specific URLs
  - Status: **NOT DEFINED**
  - Required for: "go to website.com", "open youtube.com"

### WebAvanue Integration

**Location:** `/Volumes/M-Drive/Coding/mainavanues/modules/webavanue`

**Current Status:** ❌ NOT INTEGRATED

**Recommendation:**
- **Option 1 (Quick)**: Use Android's default browser via `Intent.ACTION_VIEW` with extracted query
- **Option 2 (Advanced)**: Integrate WebAvanue as embedded browser component
  - Provides custom UI/UX control
  - Allows JavaScript injection for voice control
  - Enables browser state management
  - **Effort:** 2-3 days integration work

---

## Feature Gap Analysis by Category

### 1. Communication (communication.aot)

| Intent ID | Defined | Handler | Entity Extraction | Status |
|-----------|---------|---------|-------------------|--------|
| `send_email` | ✅ | ❌ | ❌ | **MISSING** |
| `send_text` | ✅ | ❌ | ❌ | **MISSING** |
| `make_call` | ✅ | ❌ | ❌ | **MISSING** |

**What's Missing:**
- Email composer intent handler (ACTION_SENDTO)
- SMS composer intent handler (ACTION_SENDTO)
- Phone dialer intent handler (ACTION_DIAL)
- Entity extraction: recipient, subject, message, phone number

**Required Handlers:**
```kotlin
SendEmailActionHandler() // communication.aot:send_email
SendTextActionHandler()  // communication.aot:send_text
MakeCallActionHandler()  // communication.aot:make_call
```

---

### 2. Device Control (device_control.aot)

| Intent ID | Defined | Handler | Entity Extraction | Status |
|-----------|---------|---------|-------------------|--------|
| `control_lights` | ✅ | ❌ | ❌ | **MISSING** |
| `set_volume` | ✅ | ✅ | ❌ | **PARTIAL** |
| `set_brightness` | ✅ | ✅ | ❌ | **PARTIAL** |
| `toggle_wifi` | ✅ | ✅ | ✅ | **COMPLETE** |
| `toggle_bluetooth` | ✅ | ✅ | ✅ | **COMPLETE** |
| `toggle_flashlight` | ✅ | ✅ | ✅ | **COMPLETE** |
| `set_alarm` | ✅ | ✅ | ❌ | **PARTIAL** |
| `set_timer` | ✅ | ❌ | ❌ | **MISSING** |

**What's Missing:**
- Smart home integration (lights control via Google Home/Alexa)
- Entity extraction for volume level, brightness percentage
- Timer creation handler (ACTION_SET_TIMER)
- Alarm entity extraction (time, label, recurrence)

**Required Handlers:**
```kotlin
ControlLightsActionHandler()  // device_control.aot:control_lights (requires smart home API)
SetTimerActionHandler()        // device_control.aot:set_timer
```

**Required Entity Extractors:**
- `VolumeEntityExtractor` - Extract volume level (0-15, percentage, "max", "min")
- `BrightnessEntityExtractor` - Extract brightness (0-100%, "dim", "bright")
- `AlarmEntityExtractor` - Extract time, label, recurrence from utterance

---

### 3. Media Control (media.aot)

| Intent ID | Defined | Handler | Entity Extraction | Status |
|-----------|---------|---------|-------------------|--------|
| `play_music` | ✅ | ✅ | ❌ | **PARTIAL** |
| `pause_media` | ✅ | ✅ | ✅ | **COMPLETE** |
| `resume_media` | ✅ | ❌ | ✅ | **MISSING** |
| `skip_track` | ✅ | ✅ | ✅ | **COMPLETE** |
| `previous_track` | ✅ | ✅ | ✅ | **COMPLETE** |
| `play_video` | ✅ | ❌ | ❌ | **MISSING** |

**What's Missing:**
- Resume media handler (currently only pause exists)
- Video playback handler (YouTube, Netflix integration)
- Music entity extraction (song, artist, album, playlist, genre)

**Required Handlers:**
```kotlin
ResumeMediaActionHandler()  // media.aot:resume_media
PlayVideoActionHandler()    // media.aot:play_video
```

**Required Entity Extractors:**
- `MusicEntityExtractor` - Extract song, artist, album, playlist from utterance
- `VideoEntityExtractor` - Extract title, platform (YouTube, Netflix) from utterance

---

### 4. Navigation (navigation.aot)

| Intent ID | Defined | Handler | Entity Extraction | Status |
|-----------|---------|---------|-------------------|--------|
| `get_directions` | ✅ | ❌ | ❌ | **MISSING** |
| `find_nearby` | ✅ | ❌ | ❌ | **MISSING** |
| `show_traffic` | ✅ | ❌ | ❌ | **MISSING** |
| `share_location` | ✅ | ❌ | ❌ | **MISSING** |
| `save_location` | ✅ | ❌ | ❌ | **MISSING** |

**What's Missing:**
- **ALL navigation intents lack handlers**
- Maps integration (Google Maps, Waze)
- Location services integration
- Entity extraction for destinations, place types, locations

**Required Handlers:**
```kotlin
GetDirectionsActionHandler()   // navigation.aot:get_directions
FindNearbyActionHandler()       // navigation.aot:find_nearby
ShowTrafficActionHandler()      // navigation.aot:show_traffic
ShareLocationActionHandler()    // navigation.aot:share_location
SaveLocationActionHandler()     // navigation.aot:save_location
```

**Required Entity Extractors:**
- `LocationEntityExtractor` - Extract destination, current location
- `PlaceTypeEntityExtractor` - Extract place categories (restaurant, gas station, etc.)
- `TravelModeEntityExtractor` - Extract travel mode (driving, walking, transit)

---

### 5. Productivity (productivity.aot)

| Intent ID | Defined | Handler | Entity Extraction | Status |
|-----------|---------|---------|-------------------|--------|
| `create_reminder` | ✅ | ❌ | ❌ | **MISSING** |
| `create_calendar_event` | ✅ | ❌ | ❌ | **MISSING** |
| `check_calendar` | ✅ | ❌ | ❌ | **MISSING** |
| `create_note` | ✅ | ❌ | ❌ | **MISSING** |
| `add_todo` | ✅ | ❌ | ❌ | **MISSING** |
| `search_web` | ✅ | ❌ | ❌ | **MISSING** |

**What's Missing:**
- **ALL productivity intents lack handlers**
- Calendar integration (Google Calendar, Outlook)
- Notes integration (Google Keep, Evernote)
- Tasks integration (Google Tasks, Todoist)
- Web search functionality

**Required Handlers:**
```kotlin
CreateReminderActionHandler()       // productivity.aot:create_reminder
CreateCalendarEventActionHandler()  // productivity.aot:create_calendar_event
CheckCalendarActionHandler()        // productivity.aot:check_calendar
CreateNoteActionHandler()           // productivity.aot:create_note
AddTodoActionHandler()              // productivity.aot:add_todo
SearchWebActionHandler()            // productivity.aot:search_web
```

**Required Entity Extractors:**
- `ReminderEntityExtractor` - Extract task, time, location
- `EventEntityExtractor` - Extract title, date, time, duration, location, attendees
- `NoteEntityExtractor` - Extract content, title
- `TodoEntityExtractor` - Extract task, due date, priority, list
- `WebQueryEntityExtractor` - Extract search query from utterance

---

## Priority Matrix

### P0 (Critical - Week 1)
**Goal:** Basic internet access and most-requested features

1. **Web Search Handler** (productivity.aot:search_web)
   - Effort: 2 hours
   - Impact: HIGH - enables "search for X", "google Y"
   - Implementation: Extract query → Intent.ACTION_WEB_SEARCH

2. **URL Navigation Handler** (NEW intent)
   - Effort: 1 hour
   - Impact: HIGH - enables "go to youtube.com"
   - Implementation: Parse URL → Intent.ACTION_VIEW

3. **Send Text Handler** (communication.aot:send_text)
   - Effort: 3 hours
   - Impact: HIGH - common use case
   - Implementation: Extract recipient + message → ACTION_SENDTO

4. **Make Call Handler** (communication.aot:make_call)
   - Effort: 2 hours
   - Impact: HIGH - safety critical
   - Implementation: Extract phone number → ACTION_DIAL

### P1 (High - Week 2)
**Goal:** Complete communication and basic navigation

5. **Send Email Handler** (communication.aot:send_email)
   - Effort: 4 hours
   - Impact: MEDIUM - less frequent than SMS

6. **Get Directions Handler** (navigation.aot:get_directions)
   - Effort: 5 hours
   - Impact: HIGH - common use case
   - Implementation: Extract destination → geo: URI

7. **Find Nearby Handler** (navigation.aot:find_nearby)
   - Effort: 4 hours
   - Impact: MEDIUM - "find coffee near me"

8. **Play Video Handler** (media.aot:play_video)
   - Effort: 3 hours
   - Impact: MEDIUM - YouTube integration

### P2 (Medium - Week 3)
**Goal:** Productivity features

9. **Create Reminder Handler** (productivity.aot:create_reminder)
10. **Create Calendar Event Handler** (productivity.aot:create_calendar_event)
11. **Add Todo Handler** (productivity.aot:add_todo)
12. **Create Note Handler** (productivity.aot:create_note)

### P3 (Low - Week 4+)
**Goal:** Advanced features

13. **Smart Home Integration** (device_control.aot:control_lights)
14. **Advanced Navigation** (traffic, save location, share location)
15. **WebAvanue Integration** (custom browser)

---

## Testing Regime

### Phase 1: Feature Discovery Testing (Week 1)
**Goal:** Document what works vs. what doesn't

#### Test Matrix Template
```markdown
| Intent | Utterance | Expected | Actual | Status |
|--------|-----------|----------|--------|--------|
| search_web | "search for cats" | Google search for "cats" | ❌ No handler | FAIL |
| open_browser | "open browser" | Browser opens | ✅ Google.com opens | PASS |
| toggle_wifi | "turn on wifi" | WiFi enables | ✅ WiFi enabled | PASS |
```

#### Test Categories
1. **Communication** (3 intents × 5 variations = 15 tests)
2. **Device Control** (8 intents × 5 variations = 40 tests)
3. **Media** (6 intents × 5 variations = 30 tests)
4. **Navigation** (5 intents × 5 variations = 25 tests)
5. **Productivity** (6 intents × 5 variations = 30 tests)

**Total:** 140 baseline tests

#### Test Execution Plan
```bash
# Day 1: Communication
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.category=communication

# Day 2: Device Control
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.category=device_control

# Day 3: Media
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.category=media

# Day 4: Navigation
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.category=navigation

# Day 5: Productivity
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.category=productivity
```

### Phase 2: Handler Implementation Testing (Weeks 2-4)
**Goal:** Test each new handler as it's implemented

#### Test Template per Handler
```kotlin
@Test
fun `test search_web with valid query`() = runTest {
    val utterance = "search for kotlin tutorials"
    val result = handler.execute(context, utterance)

    assertThat(result).isInstanceOf<ActionResult.Success>()
    // Verify Intent.ACTION_WEB_SEARCH was triggered
    verify(context).startActivity(argThat { intent ->
        intent.action == Intent.ACTION_WEB_SEARCH &&
        intent.getStringExtra(SearchManager.QUERY) == "kotlin tutorials"
    })
}

@Test
fun `test search_web extracts query correctly`() = runTest {
    val testCases = mapOf(
        "search for cats" to "cats",
        "google kotlin" to "kotlin",
        "look up pizza near me" to "pizza near me",
        "what is quantum computing" to "quantum computing"
    )

    testCases.forEach { (utterance, expectedQuery) ->
        val extracted = extractor.extractQuery(utterance)
        assertThat(extracted).isEqualTo(expectedQuery)
    }
}
```

### Phase 3: Entity Extraction Testing (Ongoing)
**Goal:** Validate entity extraction accuracy

#### Entity Extraction Test Matrix
```kotlin
// Example: Phone number extraction
@Test
fun `extract phone number from various formats`() {
    val testCases = mapOf(
        "call 555-1234" to "555-1234",
        "call john at 555 1234" to "555 1234",
        "dial 1-800-FLOWERS" to "1-800-FLOWERS",
        "phone 555.123.4567" to "555.123.4567"
    )

    testCases.forEach { (utterance, expected) ->
        val result = PhoneEntityExtractor.extract(utterance)
        assertThat(result.phoneNumber).isEqualTo(expected)
    }
}
```

### Phase 4: End-to-End Integration Testing (Week 5)
**Goal:** Test complete user flows

#### User Flow Tests
1. **Morning Routine**
   - "What's the weather?" → Weather info
   - "Check my calendar" → Today's events
   - "Set alarm for 7 AM" → Alarm created
   - "Remind me to take medicine at 8 AM" → Reminder created

2. **Commute Flow**
   - "Directions to work" → Navigation starts
   - "What's traffic like?" → Traffic info displayed
   - "Play my morning playlist" → Music starts
   - "Turn on bluetooth" → BT enabled for car

3. **Work Flow**
   - "Send email to John" → Email composer
   - "Schedule meeting tomorrow at 2 PM" → Calendar event
   - "Add to my todo list" → Task added
   - "Search for project documentation" → Web search

### Phase 5: Regression Testing (Continuous)
**Goal:** Ensure new features don't break existing ones

#### Automated Regression Suite
```bash
# Run all tests nightly
./gradlew test connectedAndroidTest

# Performance benchmarks
./gradlew benchmark

# Coverage report
./gradlew jacocoTestReport
```

---

## Test Automation Infrastructure

### Required Test Files

#### 1. Feature Gap Test Suite
**Location:** `Universal/AVA/Features/Actions/src/androidTest/kotlin/`

```
FeatureGapAnalysisTest.kt           # Documents missing handlers
CommunicationHandlersTest.kt       # 3 handlers × 10 tests = 30 tests
DeviceControlHandlersTest.kt       # 8 handlers × 10 tests = 80 tests
MediaControlHandlersTest.kt        # 6 handlers × 10 tests = 60 tests
NavigationHandlersTest.kt          # 5 handlers × 10 tests = 50 tests
ProductivityHandlersTest.kt        # 6 handlers × 10 tests = 60 tests
```

**Total:** 280 automated tests

#### 2. Entity Extraction Test Suite
**Location:** `Universal/AVA/Features/NLU/src/test/kotlin/`

```
PhoneNumberEntityExtractorTest.kt
EmailEntityExtractorTest.kt
LocationEntityExtractorTest.kt
TimeEntityExtractorTest.kt
DateEntityExtractorTest.kt
QueryEntityExtractorTest.kt
```

#### 3. End-to-End Flow Tests
**Location:** `apps/ava-app-android/src/androidTest/kotlin/flows/`

```
MorningRoutineFlowTest.kt
CommuteFlowTest.kt
WorkFlowTest.kt
EveningRoutineFlowTest.kt
EmergencyFlowTest.kt
```

---

## Immediate Next Steps

### Step 1: Run Feature Discovery (Today)
1. Create `FeatureGapAnalysisTest.kt`
2. Iterate through all 28 AON 3.0 intents
3. Document which have handlers vs. which don't
4. Generate test report markdown

### Step 2: Implement P0 Handlers (This Week)
1. **WebSearchActionHandler** (2 hours)
2. **URLNavigationActionHandler** (1 hour)
3. **SendTextActionHandler** (3 hours)
4. **MakeCallActionHandler** (2 hours)

**Total:** 8 hours of implementation

### Step 3: WebAvanue Decision (By Week End)
**Decision Required:** Use default browser or integrate WebAvanue?

**Comparison:**

| Feature | Default Browser | WebAvanue |
|---------|----------------|-----------|
| Implementation | 1-2 hours | 2-3 days |
| Maintenance | Zero | Ongoing |
| Control | Limited | Full |
| Voice Integration | Basic | Advanced |
| Custom UI | No | Yes |
| JavaScript Access | No | Yes |

**Recommendation for Now:**
- ✅ Use default browser for P0 (web search, URL navigation)
- ⏸️  Evaluate WebAvanue integration in Phase 2 (Week 3-4)
- 📋 Add WebAvanue as P3 feature if advanced browser control is needed

---

## Success Metrics

### Coverage Goals
- **Week 1:** 50% of AON 3.0 intents have handlers (14/28)
- **Week 2:** 75% of AON 3.0 intents have handlers (21/28)
- **Week 4:** 95% of AON 3.0 intents have handlers (27/28)

### Quality Goals
- **Test Coverage:** 90%+ for all handlers
- **Entity Extraction Accuracy:** 85%+ for all extractors
- **End-to-End Success Rate:** 95%+ for common flows
- **Response Time:** <500ms for all intent → action flows

### User Satisfaction Goals
- **Feature Completeness:** Users can accomplish 95% of tasks via voice
- **Reliability:** <5% action failure rate
- **Usability:** 90%+ user satisfaction score

---

## Appendix A: Missing Handler Template

```kotlin
package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler

/**
 * Action handler for web search.
 *
 * Extracts search query from utterance and launches web search.
 *
 * Intent: search_web (productivity.aot)
 * Utterances: "search for X", "google Y", "look up Z"
 * Entities: query (required)
 */
class SearchWebActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "SearchWebHandler"
    }

    override val intent = "search_web"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Searching web for utterance: '$utterance'")

            // Extract query using simple pattern matching
            // TODO: Replace with proper entity extractor
            val query = extractQuery(utterance)

            if (query.isEmpty()) {
                return ActionResult.Failure("Could not extract search query from: $utterance")
            }

            // Launch web search
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)

            Log.i(TAG, "Launched web search for: $query")
            ActionResult.Success(message = "Searching for $query")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search web", e)
            ActionResult.Failure(
                message = "Failed to search: ${e.message}",
                exception = e
            )
        }
    }

    private fun extractQuery(utterance: String): String {
        // Simple pattern matching (to be replaced with entity extractor)
        val patterns = listOf(
            Regex("search for (.+)", RegexOption.IGNORE_CASE),
            Regex("google (.+)", RegexOption.IGNORE_CASE),
            Regex("look up (.+)", RegexOption.IGNORE_CASE),
            Regex("what is (.+)", RegexOption.IGNORE_CASE),
            Regex("who is (.+)", RegexOption.IGNORE_CASE),
            Regex("how to (.+)", RegexOption.IGNORE_CASE)
        )

        patterns.forEach { pattern ->
            pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                return match.trim()
            }
        }

        return ""
    }
}
```

---

## Appendix B: Test Report Template

```markdown
# AVA Feature Gap Test Report

**Date:** 2025-11-27
**Tested By:** Automated Test Suite
**Test Coverage:** 28 intents across 5 categories

## Summary
- ✅ Passing: 12/28 (43%)
- ❌ Failing: 16/28 (57%)
- ⚠️  Partial: 0/28 (0%)

## Category Breakdown

### Communication (0/3 passing)
| Intent | Status | Notes |
|--------|--------|-------|
| send_email | ❌ FAIL | No handler registered |
| send_text | ❌ FAIL | No handler registered |
| make_call | ❌ FAIL | No handler registered |

### Device Control (5/8 passing)
| Intent | Status | Notes |
|--------|--------|-------|
| control_lights | ❌ FAIL | No handler registered |
| set_volume | ✅ PASS | VolumeUpActionHandler exists |
| set_brightness | ✅ PASS | BrightnessUpActionHandler exists |
| toggle_wifi | ✅ PASS | WifiOnActionHandler exists |
| toggle_bluetooth | ✅ PASS | BluetoothOnActionHandler exists |
| toggle_flashlight | ✅ PASS | FlashlightOnActionHandler exists |
| set_alarm | ⚠️  PARTIAL | AlarmActionHandler exists, no entity extraction |
| set_timer | ❌ FAIL | No handler registered |

[... continue for all categories ...]
```

---

**End of Document**
