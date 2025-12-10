# VoiceOS 4 Developer Testing Guide - Real-Time Monitoring & Validation

**Created:** 2025-10-13 22:32 PDT
**Version:** 3.0.0
**Status:** Production Development
**Note:** VoiceOS 4 (shortform: vos4) is a voice-controlled Android accessibility system

---

## Table of Contents

1. [Overview](#overview)
2. [Development Environment Setup](#development-environment-setup)
3. [Real-Time Log Monitoring](#real-time-log-monitoring)
4. [Database Inspection](#database-inspection)
5. [Command Flow Tracing](#command-flow-tracing)
6. [Performance Monitoring](#performance-monitoring)
7. [Memory & CPU Profiling](#memory--cpu-profiling)
8. [Accessibility Event Monitoring](#accessibility-event-monitoring)
9. [Speech Recognition Validation](#speech-recognition-validation)
10. [Network Traffic Analysis](#network-traffic-analysis)
11. [Debugging Tools & Techniques](#debugging-tools--techniques)
12. [Automated Validation Scripts](#automated-validation-scripts)
13. [Common Issues & Solutions](#common-issues--solutions)

---

## Overview

### Purpose

This guide helps developers **monitor VoiceOS 4 in real-time** during development and testing. Learn how to:

- ✅ Track command execution through 3 tiers
- ✅ Monitor database operations live
- ✅ Validate speech recognition accuracy
- ✅ Profile performance and resource usage
- ✅ Debug issues as they occur
- ✅ Verify correctness of app behavior

### Target Audience

- Developers actively working on VoiceOS 4
- QA engineers doing technical validation
- DevOps engineers monitoring production
- Performance engineers optimizing code

### Prerequisites

**Required Tools:**
- Android Studio (latest version)
- ADB (Android Debug Bridge)
- Terminal/Command Prompt
- Device/Emulator with VoiceOS 4 installed

**Optional but Recommended:**
- Android Studio Profiler
- SQLite browser (DB Browser for SQLite)
- Logcat Color (for colored logs)
- Charles Proxy (for network monitoring)

---

## Development Environment Setup

### 1. Enable Developer Logging

**In VoiceOS 4 code, ensure debug logging is enabled:**

```kotlin
// VoiceOSService.kt
companion object {
    private const val TAG = "VoiceOSService"
    private const val DEBUG = BuildConfig.DEBUG  // true in debug builds
}

private fun log(message: String) {
    if (DEBUG) {
        Log.d(TAG, message)
    }
}
```

**Enable verbose logging for all components:**

```kotlin
// CommandManager.kt
private const val VERBOSE_LOGGING = true

fun findCommands(input: String): List<Command> {
    if (VERBOSE_LOGGING) {
        Log.v(TAG, "=== Finding commands for: '$input' ===")
    }

    // ... command matching logic

    if (VERBOSE_LOGGING) {
        Log.v(TAG, "Found ${results.size} matches")
        results.forEach { cmd ->
            Log.v(TAG, "  - ${cmd.phrase} (confidence: ${cmd.confidence})")
        }
    }

    return results
}
```

### 2. Connect Device via ADB

**Check device connection:**
```bash
# List connected devices
adb devices

# Output should show:
# List of devices attached
# ABCD1234    device

# If multiple devices, select one:
export ANDROID_SERIAL=ABCD1234
```

**Enable verbose ADB logging:**
```bash
# Set log level to verbose
adb shell setprop log.tag.VoiceOS VERBOSE
adb shell setprop log.tag.CommandManager VERBOSE
adb shell setprop log.tag.VoiceCommandProcessor VERBOSE
adb shell setprop log.tag.WebCommandCoordinator VERBOSE
```

### 3. Setup Log Monitoring Workspace

**Option A: Multiple Terminal Windows**

```bash
# Terminal 1: Full logcat
adb logcat -v time

# Terminal 2: VoiceOS only
adb logcat -v time | grep -E "VoiceOS|CommandManager|VoiceCommand|WebCommand"

# Terminal 3: Database operations
adb logcat -v time | grep -i "database\|dao\|query"

# Terminal 4: Performance metrics
adb logcat -v time | grep -E "performance|timing|duration|took"
```

**Option B: Single Terminal with tmux/screen**

```bash
# Install tmux (if not installed)
brew install tmux  # macOS
sudo apt install tmux  # Linux

# Start tmux session
tmux new -s voiceos-dev

# Split into panes:
# Ctrl+B then " (horizontal split)
# Ctrl+B then % (vertical split)
# Ctrl+B then arrow keys (navigate between panes)

# In each pane, run different log filters
```

**Option C: Android Studio Logcat**

```
1. Open Android Studio
2. View → Tool Windows → Logcat
3. Select device
4. Create filters:
   - Name: VoiceOS All
     Tag: VoiceOS.*

   - Name: Command Execution
     Tag: CommandManager|VoiceCommandProcessor|ActionCoordinator

   - Name: Database
     Tag: .*Dao|.*Database

   - Name: Performance
     Log Message: (took|duration|performance|timing)
```

---

## Real-Time Log Monitoring

### Basic Log Monitoring

**Monitor all VoiceOS 4 logs:**
```bash
adb logcat -v time | grep -E "VoiceOS|CommandManager|VoiceCommand|WebCommand|AccessibilityScrapingIntegration"
```

**Colored output (easier to read):**
```bash
# Install logcat-color
pip install logcat-color

# Use colored logcat
adb logcat -v time | logcat-color | grep -E "VoiceOS|CommandManager"
```

### Structured Logging Format

**Implement structured logging in code:**

```kotlin
// Use consistent log format for easy parsing
private fun logCommandExecution(tier: Int, command: String, success: Boolean, duration: Long) {
    Log.d(TAG, "COMMAND_EXEC | tier=$tier | command=\"$command\" | success=$success | duration=${duration}ms")
}

// Example output:
// COMMAND_EXEC | tier=1 | command="go home" | success=true | duration=25ms
// COMMAND_EXEC | tier=2 | command="click one" | success=true | duration=245ms
```

**Parse structured logs:**
```bash
# Filter command executions
adb logcat | grep "COMMAND_EXEC"

# Extract successful Tier 1 commands
adb logcat | grep "COMMAND_EXEC" | grep "tier=1" | grep "success=true"

# Calculate average execution time
adb logcat | grep "COMMAND_EXEC" | grep "duration=" | \
  sed 's/.*duration=\([0-9]*\)ms.*/\1/' | \
  awk '{sum+=$1; count++} END {print "Average:", sum/count "ms"}'
```

### Component-Specific Monitoring

#### 1. Monitor VoiceOSService

**What to watch:**
- Service lifecycle (connected, interrupted, destroyed)
- Voice command reception
- Tier routing decisions
- Web vs regular command detection

**Log filter:**
```bash
adb logcat -v time VoiceOSService:V *:S
```

**Key log messages to look for:**
```
✅ "AccessibilityService connected" - Service started
✅ "Recognized: 'go home' (confidence: 0.95)" - Speech recognized
✅ "Browser detected: com.android.chrome" - Web mode activated
✅ "✓ Tier 1 executed: go home" - Command succeeded at Tier 1
✅ "Command failed: unknown command" - All tiers failed
```

**Real-time validation script:**
```bash
#!/bin/bash
# voiceos-monitor.sh

echo "Monitoring VoiceOSService..."
adb logcat -c  # Clear log buffer

adb logcat -v time | while read line; do
    # Highlight successful commands in green
    if echo "$line" | grep -q "✓ Tier"; then
        echo -e "\033[32m$line\033[0m"

    # Highlight failures in red
    elif echo "$line" | grep -q "Command failed"; then
        echo -e "\033[31m$line\033[0m"

    # Highlight speech recognition in blue
    elif echo "$line" | grep -q "Recognized:"; then
        echo -e "\033[34m$line\033[0m"

    # Normal output
    else
        echo "$line"
    fi
done
```

#### 2. Monitor CommandManager (Tier 1)

**What to watch:**
- Command registration
- Command matching (exact and fuzzy)
- Confidence filtering
- Execution timing

**Log filter:**
```bash
adb logcat -v time CommandManager:V *:S
```

**Add timing logs:**
```kotlin
// In CommandManager.kt
suspend fun findCommands(input: String): List<Command> {
    val startTime = System.currentTimeMillis()

    Log.v(TAG, "=== CommandManager.findCommands('$input') ===")

    // Matching logic...
    val results = performMatching(input)

    val duration = System.currentTimeMillis() - startTime
    Log.v(TAG, "Found ${results.size} matches in ${duration}ms")

    return results
}
```

**Real-time performance validation:**
```bash
# Watch for slow queries (> 100ms)
adb logcat | grep "CommandManager" | grep "took" | \
  awk '{if ($NF > 100) print "\033[31mSLOW:\033[0m", $0}'
```

#### 3. Monitor VoiceCommandProcessor (Tier 2)

**What to watch:**
- Element hash lookups
- Database queries
- Element action execution
- Accessibility node operations

**Log filter:**
```bash
adb logcat -v time VoiceCommandProcessor:V *:S
```

**Add detailed logging:**
```kotlin
// In VoiceCommandProcessor.kt
suspend fun processCommand(command: String, packageName: String?): Boolean {
    Log.d(TAG, "=== VoiceCommandProcessor.processCommand('$command', '$packageName') ===")

    val startTime = System.currentTimeMillis()

    // Find element
    val element = findElementByCommand(command, packageName)
    val findTime = System.currentTimeMillis() - startTime
    Log.d(TAG, "Element lookup took ${findTime}ms")

    if (element == null) {
        Log.d(TAG, "No element found for command: $command")
        return false
    }

    Log.d(TAG, "Found element: hash=${element.elementHash}, text=${element.text}")

    // Execute action
    val execStart = System.currentTimeMillis()
    val success = executeElementAction(element)
    val execTime = System.currentTimeMillis() - execStart

    Log.d(TAG, "Action execution: success=$success, took ${execTime}ms")
    Log.d(TAG, "Total Tier 2 duration: ${System.currentTimeMillis() - startTime}ms")

    return success
}
```

#### 4. Monitor WebCommandCoordinator

**What to watch:**
- Browser detection
- URL extraction
- Web command matching
- Element finding via accessibility

**Log filter:**
```bash
adb logcat -v time WebCommandCoordinator:V *:S
```

**Monitor web command flow:**
```bash
adb logcat -v time | grep -E "WebCommandCoordinator|isCurrentAppBrowser|getCurrentURL|processWebCommand"
```

**Expected log sequence:**
```
1. isCurrentAppBrowser: true (com.android.chrome)
2. getCurrentURL: https://www.google.com
3. findMatchingWebCommand: Searching for 'click images'
4. Found web command: elementHash=abc123...
5. executeWebAction: CLICK on element
6. Web action completed: success=true, duration=150ms
```

### Log Pattern Recognition

**Create grep patterns for common scenarios:**

```bash
# Successful command execution (any tier)
PATTERN_SUCCESS='✓ Tier [123] executed'

# Failed command execution
PATTERN_FAIL='Command failed'

# Slow execution (> 500ms)
PATTERN_SLOW='duration=[5-9][0-9][0-9]ms|duration=[0-9]{4,}ms'

# Database operations
PATTERN_DB='INSERT|UPDATE|DELETE|SELECT.*FROM'

# Memory warnings
PATTERN_MEMORY='OutOfMemory|GC_FOR_ALLOC|Growing heap'

# Use patterns
adb logcat | grep -E "$PATTERN_SUCCESS|$PATTERN_FAIL"
```

---

## Database Inspection

### Real-Time Database Monitoring

#### 1. Watch Database Changes

**Monitor database operations:**
```bash
# Watch all database queries
adb logcat | grep -E "INSERT|UPDATE|DELETE|SELECT"

# Watch specific table
adb logcat | grep "generated_commands"

# Count queries per minute
adb logcat | grep "SELECT" | pv -l -i 60 > /dev/null
```

#### 2. Query Database While App Running

**CommandDatabase inspection:**
```bash
# Count total commands
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/command_database \
  'SELECT COUNT(*) FROM vos_commands;'"

# List all commands
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/command_database \
  'SELECT commandPhrase, action, confidence FROM vos_commands;'"

# Find command by phrase
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/command_database \
  'SELECT * FROM vos_commands WHERE commandPhrase LIKE \"%home%\";'"
```

**AppScrapingDatabase inspection:**
```bash
# Count scraped elements
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/app_scraping_database \
  'SELECT COUNT(*) FROM scraped_elements;'"

# Show recently scraped elements
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/app_scraping_database \
  'SELECT text, className, lastScraped FROM scraped_elements ORDER BY lastScraped DESC LIMIT 10;'"

# Count commands per app
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/app_scraping_database \
  'SELECT packageName, COUNT(*) FROM scraped_elements GROUP BY packageName;'"
```

**WebScrapingDatabase inspection:**
```bash
# List learned websites
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/web_scraping_database \
  'SELECT url, scrapedCount FROM scraped_websites;'"

# Count web commands per site
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/web_scraping_database \
  'SELECT sw.url, COUNT(gwc.id) as cmd_count \
   FROM scraped_websites sw \
   LEFT JOIN generated_web_commands gwc ON sw.urlHash = gwc.websiteUrlHash \
   GROUP BY sw.url;'"
```

#### 3. Continuous Database Polling

**Create a monitoring script:**
```bash
#!/bin/bash
# db-monitor.sh - Monitor database changes in real-time

DB_PATH="databases/app_scraping_database"
PACKAGE="com.augmentalis.voiceaccessibility"

echo "Monitoring database changes..."
echo "Press Ctrl+C to stop"
echo ""

# Store initial counts
prev_elements=$(adb shell "run-as $PACKAGE sqlite3 $DB_PATH 'SELECT COUNT(*) FROM scraped_elements;'" | tr -d '\r')
prev_commands=$(adb shell "run-as $PACKAGE sqlite3 $DB_PATH 'SELECT COUNT(*) FROM generated_commands;'" | tr -d '\r')

while true; do
    sleep 2

    # Get current counts
    curr_elements=$(adb shell "run-as $PACKAGE sqlite3 $DB_PATH 'SELECT COUNT(*) FROM scraped_elements;'" | tr -d '\r')
    curr_commands=$(adb shell "run-as $PACKAGE sqlite3 $DB_PATH 'SELECT COUNT(*) FROM generated_commands;'" | tr -d '\r')

    # Check for changes
    if [ "$curr_elements" != "$prev_elements" ]; then
        diff=$((curr_elements - prev_elements))
        echo "$(date '+%H:%M:%S') - Elements: $prev_elements → $curr_elements (+$diff)"

        # Show new elements
        adb shell "run-as $PACKAGE sqlite3 $DB_PATH \
          'SELECT text, className FROM scraped_elements ORDER BY id DESC LIMIT $diff;'"

        prev_elements=$curr_elements
    fi

    if [ "$curr_commands" != "$prev_commands" ]; then
        diff=$((curr_commands - prev_commands))
        echo "$(date '+%H:%M:%S') - Commands: $prev_commands → $curr_commands (+$diff)"

        # Show new commands
        adb shell "run-as $PACKAGE sqlite3 $DB_PATH \
          'SELECT commandPhrase FROM generated_commands ORDER BY id DESC LIMIT $diff;'"

        prev_commands=$curr_commands
    fi
done
```

**Run the script:**
```bash
chmod +x db-monitor.sh
./db-monitor.sh
```

#### 4. Export Database for Analysis

**Pull database to local machine:**
```bash
# Pull database file
adb shell "run-as com.augmentalis.voiceaccessibility \
  cp databases/app_scraping_database /sdcard/app_scraping_database"
adb pull /sdcard/app_scraping_database ./

# Open with SQLite browser
# Now you can use DB Browser for SQLite to explore
```

**Automated export script:**
```bash
#!/bin/bash
# export-databases.sh

PACKAGE="com.augmentalis.voiceaccessibility"
OUTPUT_DIR="./db_exports/$(date +%Y%m%d_%H%M%S)"

mkdir -p "$OUTPUT_DIR"

echo "Exporting databases to: $OUTPUT_DIR"

# Export all three databases
for db in command_database app_scraping_database web_scraping_database; do
    echo "Exporting $db..."
    adb shell "run-as $PACKAGE cp databases/$db /sdcard/$db"
    adb pull /sdcard/$db "$OUTPUT_DIR/"
    adb shell "rm /sdcard/$db"
    echo "✓ $db exported"
done

echo "All databases exported to: $OUTPUT_DIR"
```

### Validate Database Integrity

**Check for corruption:**
```bash
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/app_scraping_database \
  'PRAGMA integrity_check;'"

# Should output: ok
```

**Check for orphaned records:**
```bash
# Find commands without corresponding elements
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/app_scraping_database \
  'SELECT gc.commandPhrase FROM generated_commands gc \
   LEFT JOIN scraped_elements se ON gc.elementHash = se.elementHash \
   WHERE se.elementHash IS NULL;'"

# Should return no results (or empty)
```

**Verify hash uniqueness:**
```bash
# Check for duplicate hashes (should be unique)
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/app_scraping_database \
  'SELECT elementHash, COUNT(*) as count FROM scraped_elements \
   GROUP BY elementHash HAVING count > 1;'"

# Should return no results
```

---

## Command Flow Tracing

### End-to-End Command Tracing

**Add trace logging for complete command flow:**

```kotlin
// VoiceOSService.kt
private fun handleVoiceCommand(command: String, confidence: Float) {
    val traceId = UUID.randomUUID().toString().substring(0, 8)

    Log.d(TAG, "[$traceId] === Command Flow Start ===")
    Log.d(TAG, "[$traceId] Input: '$command' (confidence: $confidence)")

    if (confidence < 0.5f) {
        Log.d(TAG, "[$traceId] REJECTED: Low confidence")
        return
    }

    val normalized = command.lowercase().trim()
    Log.d(TAG, "[$traceId] Normalized: '$normalized'")

    // Check browser
    val currentPackage = rootInActiveWindow?.packageName?.toString()
    Log.d(TAG, "[$traceId] Current package: $currentPackage")

    if (currentPackage != null && webCommandCoordinator.isCurrentAppBrowser(currentPackage)) {
        Log.d(TAG, "[$traceId] Route: WEB TIER")
        serviceScope.launch {
            val handled = webCommandCoordinator.processWebCommand(normalized, currentPackage)
            if (handled) {
                Log.d(TAG, "[$traceId] ✓ WEB TIER succeeded")
            } else {
                Log.d(TAG, "[$traceId] WEB TIER failed, trying regular tiers")
                handleRegularCommand(normalized, confidence, traceId)
            }
        }
        return
    }

    Log.d(TAG, "[$traceId] Route: REGULAR TIERS")
    handleRegularCommand(normalized, confidence, traceId)
}

private fun handleRegularCommand(command: String, confidence: Float, traceId: String) {
    serviceScope.launch {
        val context = createCommandContext()

        // Tier 1
        Log.d(TAG, "[$traceId] Trying TIER 1 (CommandManager)")
        val tier1Commands = commandManager.findCommands(command)
        Log.d(TAG, "[$traceId] Tier 1 found ${tier1Commands.size} matches")

        if (tier1Commands.isNotEmpty()) {
            for (cmd in tier1Commands) {
                val success = commandManager.executeCommand(cmd, context)
                if (success) {
                    Log.d(TAG, "[$traceId] ✓ TIER 1 succeeded: ${cmd.phrase}")
                    return@launch
                }
            }
        }

        // Tier 2
        Log.d(TAG, "[$traceId] Trying TIER 2 (VoiceCommandProcessor)")
        val tier2Success = executeTier2Command(command, context)
        if (tier2Success) {
            Log.d(TAG, "[$traceId] ✓ TIER 2 succeeded")
            return@launch
        }

        // Tier 3
        Log.d(TAG, "[$traceId] Trying TIER 3 (ActionCoordinator)")
        val tier3Success = executeTier3Command(command)
        if (tier3Success) {
            Log.d(TAG, "[$traceId] ✓ TIER 3 succeeded")
            return@launch
        }

        Log.d(TAG, "[$traceId] ✗ ALL TIERS FAILED")
    }
}
```

**Monitor complete trace:**
```bash
# Watch specific trace
adb logcat | grep "\[abc12345\]"

# Watch all traces
adb logcat | grep -E "\[[a-f0-9]{8}\]"

# Colorize by tier
adb logcat | grep -E "\[[a-f0-9]{8}\]" | \
  sed -e 's/TIER 1/\o033[32mTIER 1\o033[0m/' \
      -e 's/TIER 2/\o033[33mTIER 2\o033[0m/' \
      -e 's/TIER 3/\o033[31mTIER 3\o033[0m/'
```

### Visualize Command Flow

**Create a trace analyzer script:**

```python
#!/usr/bin/env python3
# trace_analyzer.py

import re
import sys
from collections import defaultdict

traces = defaultdict(list)
trace_pattern = re.compile(r'\[([a-f0-9]{8})\] (.*)')

# Read from stdin or file
for line in sys.stdin:
    match = trace_pattern.search(line)
    if match:
        trace_id, message = match.groups()
        traces[trace_id].append(message)

# Print traces
for trace_id, messages in traces.items():
    print(f"\n=== Trace {trace_id} ===")
    for msg in messages:
        # Indent based on tier
        if 'TIER 1' in msg:
            print(f"  → {msg}")
        elif 'TIER 2' in msg:
            print(f"    → {msg}")
        elif 'TIER 3' in msg:
            print(f"      → {msg}")
        else:
            print(f"{msg}")

    # Summary
    if any('succeeded' in m for m in messages):
        success_tier = next((m for m in messages if 'succeeded' in m), None)
        print(f"✓ Result: {success_tier}")
    else:
        print(f"✗ Result: ALL TIERS FAILED")
```

**Use the analyzer:**
```bash
# Capture traces
adb logcat | grep -E "\[[a-f0-9]{8}\]" | python3 trace_analyzer.py

# Or analyze from log file
adb logcat > voiceos.log
cat voiceos.log | python3 trace_analyzer.py
```

### Tier-Specific Validation

**Validate Tier 1 routing:**
```bash
# Count Tier 1 successes
adb logcat | grep "✓ TIER 1 succeeded" | wc -l

# Show which commands succeeded at Tier 1
adb logcat | grep "✓ TIER 1 succeeded" | \
  sed 's/.*succeeded: \(.*\)$/\1/' | sort | uniq -c
```

**Validate Tier 2 routing:**
```bash
# Show Tier 2 activations
adb logcat | grep "Trying TIER 2"

# Verify Tier 1 failed before Tier 2
adb logcat | grep -B 3 "Trying TIER 2" | grep "Tier 1 found 0 matches"
```

**Validate Tier 3 fallback:**
```bash
# Show commands that reached Tier 3
adb logcat | grep "Trying TIER 3" | wc -l

# Show what failed before reaching Tier 3
adb logcat | grep -B 10 "Trying TIER 3" | grep "TIER [12]"
```

---

## Performance Monitoring

### Real-Time Performance Metrics

#### 1. Command Execution Time

**Add performance instrumentation:**

```kotlin
// Create a performance tracker
object PerformanceTracker {
    private data class Metric(
        val name: String,
        val startTime: Long,
        var endTime: Long? = null
    )

    private val metrics = mutableMapOf<String, Metric>()

    fun start(name: String) {
        metrics[name] = Metric(name, System.currentTimeMillis())
    }

    fun end(name: String) {
        metrics[name]?.let {
            it.endTime = System.currentTimeMillis()
            val duration = it.endTime!! - it.startTime
            Log.d("PERF", "$name took ${duration}ms")

            // Warn if slow
            if (duration > 500) {
                Log.w("PERF", "⚠️ SLOW: $name took ${duration}ms")
            }
        }
    }

    fun measure(name: String, block: () -> Unit) {
        start(name)
        try {
            block()
        } finally {
            end(name)
        }
    }
}

// Use in code
PerformanceTracker.measure("CommandManager.findCommands") {
    commandManager.findCommands(input)
}
```

**Monitor performance in real-time:**
```bash
# Watch all performance logs
adb logcat | grep "PERF"

# Show only slow operations (> 500ms)
adb logcat | grep "PERF" | grep "⚠️ SLOW"

# Calculate average performance
adb logcat | grep "PERF" | grep "took" | \
  sed 's/.*took \([0-9]*\)ms/\1/' | \
  awk '{sum+=$1; count++} END {print "Average:", sum/count "ms"}'
```

#### 2. Database Query Performance

**Monitor query execution:**
```bash
# Watch database queries
adb logcat | grep -E "SELECT|INSERT|UPDATE|DELETE"

# Count queries per second
adb logcat | grep "SELECT" | pv -l -i 1

# Show slow queries (if timing added)
adb logcat | grep "Query took" | awk '$NF > 50 {print}'
```

**Add query timing to DAOs:**
```kotlin
@Dao
interface GeneratedCommandDao {

    @Query("SELECT * FROM generated_commands WHERE commandPhrase = :phrase")
    suspend fun getByCommandPhrase(phrase: String): GeneratedCommandEntity? {
        val start = System.currentTimeMillis()
        val result = getByCommandPhraseInternal(phrase)
        val duration = System.currentTimeMillis() - start
        Log.v("DAO", "getByCommandPhrase took ${duration}ms")
        return result
    }

    @Query("SELECT * FROM generated_commands WHERE commandPhrase = :phrase")
    suspend fun getByCommandPhraseInternal(phrase: String): GeneratedCommandEntity?
}
```

#### 3. System Resource Monitoring

**CPU Usage:**
```bash
# Monitor CPU usage
adb shell top -n 1 | grep voiceaccessibility

# Continuous monitoring
watch -n 1 "adb shell top -n 1 | grep voiceaccessibility"
```

**Memory Usage:**
```bash
# Current memory usage
adb shell dumpsys meminfo com.augmentalis.voiceaccessibility | grep "TOTAL"

# Monitor memory over time
while true; do
    adb shell dumpsys meminfo com.augmentalis.voiceaccessibility | \
      grep "TOTAL" | \
      awk '{print strftime("%H:%M:%S"), $2/1024 "MB"}'
    sleep 5
done
```

**Battery Impact:**
```bash
# Battery usage by VoiceOS 4
adb shell dumpsys batterystats | grep voiceaccessibility

# Continuous battery monitoring
watch -n 60 "adb shell dumpsys battery | grep level"
```

### Performance Benchmarking Script

```bash
#!/bin/bash
# performance-benchmark.sh

PACKAGE="com.augmentalis.voiceaccessibility"

echo "=== VoiceOS 4 Performance Benchmark ==="
echo "Started at: $(date)"
echo ""

# Test 1: Tier 1 command speed
echo "Test 1: Tier 1 Command Speed (go home)"
for i in {1..10}; do
    echo "Executing command $i..."
    # Trigger command and measure
    # (This would need actual command triggering mechanism)
    sleep 1
done

# Analyze logs
echo ""
echo "Tier 1 Performance:"
adb logcat -d | grep "✓ TIER 1 succeeded" | grep "duration=" | \
  sed 's/.*duration=\([0-9]*\)ms.*/\1/' | \
  awk '{
    sum+=$1;
    count++;
    if(NR==1) min=$1;
    if($1<min) min=$1;
    if($1>max) max=$1
  }
  END {
    print "  Average:", sum/count "ms"
    print "  Min:", min "ms"
    print "  Max:", max "ms"
  }'

# Test 2: Database query speed
echo ""
echo "Test 2: Database Query Speed"
for i in {1..100}; do
    adb shell "run-as $PACKAGE sqlite3 databases/app_scraping_database \
      'SELECT * FROM generated_commands LIMIT 1;'" > /dev/null 2>&1
done

# Test 3: Memory usage
echo ""
echo "Test 3: Memory Usage"
MEMORY=$(adb shell dumpsys meminfo $PACKAGE | grep "TOTAL" | awk '{print $2}')
MEMORY_MB=$((MEMORY / 1024))
echo "  Current Memory: ${MEMORY_MB}MB"

if [ $MEMORY_MB -lt 100 ]; then
    echo "  Status: ✓ Good (< 100MB)"
elif [ $MEMORY_MB -lt 150 ]; then
    echo "  Status: ⚠ Acceptable (100-150MB)"
else
    echo "  Status: ✗ High (> 150MB)"
fi

# Test 4: CPU usage
echo ""
echo "Test 4: CPU Usage"
CPU=$(adb shell top -n 1 | grep $PACKAGE | awk '{print $9}')
echo "  Current CPU: ${CPU}%"

echo ""
echo "=== Benchmark Complete ==="
echo "Finished at: $(date)"
```

---

## Memory & CPU Profiling

### Android Studio Profiler

**Use Android Studio's built-in profiler:**

1. **Open Profiler:**
   ```
   View → Tool Windows → Profiler
   ```

2. **Select Process:**
   - Click "+" to add session
   - Select device
   - Choose "com.augmentalis.voiceaccessibility"

3. **Profile Options:**
   - **CPU:** Click CPU timeline → Record → Perform actions → Stop
   - **Memory:** Click Memory timeline → Record → Observe allocations
   - **Network:** Monitor network requests (for online speech engines)
   - **Energy:** Track battery impact

### Memory Leak Detection

**Using LeakCanary (add to debug build):**

```kotlin
// build.gradle.kts
dependencies {
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
```

**Manual memory leak detection:**
```bash
# Dump memory
adb shell am dumpheap com.augmentalis.voiceaccessibility /sdcard/memory.hprof

# Pull to local
adb pull /sdcard/memory.hprof ./

# Analyze with Android Studio:
# File → Open → Select memory.hprof
```

### Monitor AccessibilityNodeInfo Leaks

**Critical: AccessibilityNodeInfo must be recycled**

```kotlin
// Add leak detection in debug builds
private fun findNodeWithLeakDetection(condition: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
    val rootNode = rootInActiveWindow
    val allocatedNodes = mutableListOf<AccessibilityNodeInfo>()

    try {
        val result = traverseAndFind(rootNode, condition, allocatedNodes)
        return result
    } finally {
        // Recycle all allocated nodes
        allocatedNodes.forEach { node ->
            try {
                node.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to recycle node", e)
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Recycled ${allocatedNodes.size} AccessibilityNodeInfo objects")
        }
    }
}
```

**Monitor for un-recycled nodes:**
```bash
# Watch for accessibility node leaks
adb logcat | grep -E "AccessibilityNodeInfo|recycle"

# Count recycle calls
adb logcat | grep "recycle" | wc -l
```

---

## Accessibility Event Monitoring

### Monitor All Accessibility Events

**Filter accessibility events:**
```bash
# All accessibility events
adb logcat | grep "AccessibilityEvent"

# Window state changes
adb logcat | grep "TYPE_WINDOW_STATE_CHANGED"

# Window content changes
adb logcat | grep "TYPE_WINDOW_CONTENT_CHANGED"

# View clicks
adb logcat | grep "TYPE_VIEW_CLICKED"
```

### Add Detailed Event Logging

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (BuildConfig.DEBUG) {
        Log.d(TAG, """
            === Accessibility Event ===
            Type: ${AccessibilityEvent.eventTypeToString(event.eventType)}
            Package: ${event.packageName}
            Class: ${event.className}
            Text: ${event.text}
            Time: ${event.eventTime}
            Source: ${event.source?.className}
        """.trimIndent())
    }

    // Process event...
}
```

### Monitor Scraping Activity

**Watch UI scraping in real-time:**
```bash
# Monitor scraping events
adb logcat | grep -E "Scraping|AccessibilityScrapingIntegration"

# Count elements scraped
adb logcat | grep "Scraped.*elements" | \
  sed 's/.*Scraped \([0-9]*\) elements.*/\1/' | \
  awk '{sum+=$1; count++} END {print "Total elements scraped:", sum}'
```

**Validate scraping frequency:**
```bash
# Check scraping isn't too frequent (should be throttled)
adb logcat -v time | grep "Scraping started" | \
  awk '{print $2}' | \
  awk 'NR>1 {diff=($1-prev)*1; if(diff<0.5) print "⚠️ Too frequent:", diff"s"; prev=$1} NR==1{prev=$1}'
```

---

## Speech Recognition Validation

### Monitor Recognition Results

**Track recognition accuracy:**
```bash
# All recognition results
adb logcat | grep "onRecognitionResult"

# High confidence results (> 0.8)
adb logcat | grep "onRecognitionResult" | grep -E "confidence: 0\.[89]|confidence: 1\."

# Low confidence results (< 0.5)
adb logcat | grep "onRecognitionResult" | grep "confidence: 0\.[0-4]"
```

### Calculate Recognition Statistics

```bash
#!/bin/bash
# recognition-stats.sh

echo "=== Speech Recognition Statistics ==="

# Total recognitions
TOTAL=$(adb logcat -d | grep "onRecognitionResult" | wc -l)
echo "Total recognitions: $TOTAL"

# High confidence (>= 0.8)
HIGH=$(adb logcat -d | grep "onRecognitionResult" | grep -E "confidence: 0\.[89]|confidence: 1\." | wc -l)
echo "High confidence (≥0.8): $HIGH ($((HIGH*100/TOTAL))%)"

# Medium confidence (0.5-0.79)
MEDIUM=$(adb logcat -d | grep "onRecognitionResult" | grep "confidence: 0\.[567]" | wc -l)
echo "Medium confidence (0.5-0.79): $MEDIUM ($((MEDIUM*100/TOTAL))%)"

# Low confidence (< 0.5) - rejected
LOW=$(adb logcat -d | grep "onRecognitionResult" | grep "confidence: 0\.[0-4]" | wc -l)
echo "Low confidence (<0.5): $LOW ($((LOW*100/TOTAL))%)"

# Average confidence
AVG=$(adb logcat -d | grep "onRecognitionResult" | \
  sed 's/.*confidence: \([0-9.]*\).*/\1/' | \
  awk '{sum+=$1; count++} END {printf "%.2f", sum/count}')
echo "Average confidence: $AVG"
```

### Validate Speech Engine Performance

**Compare engines:**
```bash
# Test with Google engine
echo "Testing Google Speech Engine..."
# (trigger commands)

# Capture results
adb logcat -d | grep "onRecognitionResult" > google_results.txt

# Clear log
adb logcat -c

# Switch to Vosk
echo "Testing Vosk Speech Engine..."
# (trigger same commands)

# Capture results
adb logcat -d | grep "onRecognitionResult" > vosk_results.txt

# Compare
echo "Google accuracy: $(cat google_results.txt | wc -l)"
echo "Vosk accuracy: $(cat vosk_results.txt | wc -l)"
```

---

## Network Traffic Analysis

### Monitor Network Requests (Online Engines)

**Using Charles Proxy:**

1. **Setup Charles:**
   ```
   - Install Charles Proxy on computer
   - Get computer's IP address
   - Configure Android device proxy to point to computer
   ```

2. **Configure Device:**
   ```
   Settings → Network → WiFi → Long press → Modify network
   Advanced options → Proxy → Manual
   Proxy hostname: [Computer IP]
   Proxy port: 8888
   ```

3. **Install Charles Certificate:**
   ```
   Help → SSL Proxying → Install Charles Root Certificate on Mobile Device
   Follow instructions to install on Android
   ```

4. **Monitor Traffic:**
   - Start Charles
   - Enable SSL Proxying for relevant domains
   - Use VoiceOS 4 with Google Speech
   - Watch requests in Charles

**Using ADB:**
```bash
# Monitor network stats
adb shell dumpsys netstats | grep voiceaccessibility

# Watch network activity
watch -n 1 "adb shell cat /proc/net/xt_qtaguid/stats | grep voiceaccessibility"
```

### Validate API Calls

**For Google Speech API:**
```bash
# Monitor Google Speech API requests
adb logcat | grep -E "google.*speech|speech.*google"

# Check for errors
adb logcat | grep -E "ERROR.*speech|speech.*ERROR"
```

---

## Debugging Tools & Techniques

### 1. Breakpoint Debugging (Android Studio)

**Set strategic breakpoints:**

```kotlin
// VoiceOSService.kt
private fun handleVoiceCommand(command: String, confidence: Float) {
    // BREAKPOINT HERE - inspect command and confidence
    if (confidence < 0.5f) return

    val normalized = command.lowercase().trim()
    // BREAKPOINT HERE - check normalized value

    // ...rest of method
}
```

**Conditional breakpoints:**
```
Right-click breakpoint → Condition
Enter: confidence < 0.7 && command.contains("home")
```

**Logpoint (non-breaking):**
```
Right-click breakpoint → Disable → Check "Log message to console"
Message: "Command: {command}, Confidence: {confidence}"
```

### 2. Remote Debugging

**Enable remote debugging:**
```bash
# Forward debugging port
adb forward tcp:5005 tcp:5005

# Start app with debug flags
adb shell am start -D -n com.augmentalis.voiceaccessibility/.MainActivity
```

**Attach debugger in Android Studio:**
```
Run → Attach Debugger to Android Process
Select: com.augmentalis.voiceaccessibility
```

### 3. Layout Inspector (Accessibility Tree)

**View accessibility hierarchy:**
```
1. Tools → Layout Inspector
2. Select device and com.augmentalis.voiceaccessibility
3. Click "Capture Layout"
4. Explore accessibility tree structure
```

**Validate element properties:**
- Check text, contentDescription, className
- Verify clickable, focusable flags
- Inspect hierarchy paths
- Compare with scraped data

### 4. Enable StrictMode (Debug Builds)

**Detect performance issues:**
```kotlin
// In Application.onCreate() for debug builds
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
    )

    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .build()
    )
}
```

**Monitor StrictMode violations:**
```bash
adb logcat | grep "StrictMode"
```

---

## Automated Validation Scripts

### All-in-One Monitoring Script

```bash
#!/bin/bash
# voiceos-monitor-all.sh

echo "=== VoiceOS 4 Real-Time Monitor ==="
echo "Started at: $(date)"
echo ""

# Check device connection
if ! adb devices | grep -q "device$"; then
    echo "❌ No device connected"
    exit 1
fi

echo "✓ Device connected"

# Check VoiceOS 4 is running
if ! adb shell pidof com.augmentalis.voiceaccessibility > /dev/null; then
    echo "❌ VoiceOS 4 not running"
    exit 1
fi

echo "✓ VoiceOS 4 is running"
echo ""

# Clear log buffer
adb logcat -c

# Start monitoring in parallel
echo "Starting monitors..."
echo ""

# Monitor 1: Command execution
{
    echo "=== Command Execution Monitor ==="
    adb logcat | grep -E "✓ Tier|✗ ALL TIERS" | while read line; do
        if echo "$line" | grep -q "✓ Tier"; then
            echo -e "\033[32m$line\033[0m"
        else
            echo -e "\033[31m$line\033[0m"
        fi
    done
} &
MON1=$!

# Monitor 2: Performance
{
    echo "=== Performance Monitor ==="
    adb logcat | grep "PERF" | grep "took" | while read line; do
        DURATION=$(echo "$line" | sed 's/.*took \([0-9]*\)ms/\1/')
        if [ "$DURATION" -gt 500 ]; then
            echo -e "\033[31m⚠️ SLOW: $line\033[0m"
        elif [ "$DURATION" -gt 200 ]; then
            echo -e "\033[33m$line\033[0m"
        else
            echo -e "\033[32m$line\033[0m"
        fi
    done
} &
MON2=$!

# Monitor 3: Errors
{
    echo "=== Error Monitor ==="
    adb logcat *:E | grep "voiceaccessibility\|CommandManager\|VoiceCommand" | while read line; do
        echo -e "\033[31m❌ ERROR: $line\033[0m"
    done
} &
MON3=$!

# Monitor 4: Memory
{
    echo "=== Memory Monitor ==="
    while true; do
        MEM=$(adb shell dumpsys meminfo com.augmentalis.voiceaccessibility 2>/dev/null | grep "TOTAL" | awk '{print $2}')
        if [ ! -z "$MEM" ]; then
            MEM_MB=$((MEM / 1024))
            if [ $MEM_MB -gt 150 ]; then
                echo -e "\033[31m⚠️ High memory: ${MEM_MB}MB\033[0m"
            else
                echo "Memory: ${MEM_MB}MB"
            fi
        fi
        sleep 10
    done
} &
MON4=$!

# Wait for interrupt
echo "Monitoring... (Press Ctrl+C to stop)"
trap "kill $MON1 $MON2 $MON3 $MON4 2>/dev/null; exit" INT

wait
```

### Automated Test Runner

```bash
#!/bin/bash
# automated-tests.sh

PACKAGE="com.augmentalis.voiceaccessibility"

echo "=== Automated VoiceOS 4 Tests ==="

# Test 1: Service Running
echo -n "Test 1: Service Running... "
if adb shell pidof $PACKAGE > /dev/null; then
    echo "✓ PASS"
else
    echo "✗ FAIL"
    exit 1
fi

# Test 2: Database Exists
echo -n "Test 2: Database Exists... "
DB_COUNT=$(adb shell "run-as $PACKAGE ls databases/ 2>/dev/null | wc -l")
if [ "$DB_COUNT" -ge 3 ]; then
    echo "✓ PASS ($DB_COUNT databases)"
else
    echo "✗ FAIL (only $DB_COUNT databases)"
fi

# Test 3: Commands Registered
echo -n "Test 3: Commands Registered... "
CMD_COUNT=$(adb shell "run-as $PACKAGE sqlite3 databases/app_scraping_database 'SELECT COUNT(*) FROM generated_commands;' 2>/dev/null" | tr -d '\r')
if [ "$CMD_COUNT" -gt 0 ]; then
    echo "✓ PASS ($CMD_COUNT commands)"
else
    echo "✗ FAIL (no commands)"
fi

# Test 4: Memory Usage
echo -n "Test 4: Memory Usage... "
MEM=$(adb shell dumpsys meminfo $PACKAGE | grep "TOTAL" | awk '{print $2}')
MEM_MB=$((MEM / 1024))
if [ $MEM_MB -lt 200 ]; then
    echo "✓ PASS (${MEM_MB}MB)"
else
    echo "⚠️ WARNING (${MEM_MB}MB - high)"
fi

# Test 5: Recent Activity
echo -n "Test 5: Recent Activity... "
RECENT=$(adb logcat -d | grep "VoiceOS\|CommandManager" | tail -1)
if [ ! -z "$RECENT" ]; then
    echo "✓ PASS"
else
    echo "✗ FAIL (no recent logs)"
fi

echo ""
echo "=== Tests Complete ==="
```

---

## Common Issues & Solutions

### Issue 1: Commands Not Executing

**Symptoms:**
- Speech recognized but command doesn't execute
- Logs show "ALL TIERS FAILED"

**Debugging:**
```bash
# Check if command exists in database
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/app_scraping_database \
  'SELECT * FROM generated_commands WHERE commandPhrase LIKE \"%home%\";'"

# Check confidence threshold
adb logcat | grep "Rejected: Low confidence"

# Verify tier execution
adb logcat | grep -E "Tier [123]"
```

**Solutions:**
1. Check confidence is >= 0.5
2. Verify command exists in database
3. Check accessibility service is enabled
4. Verify current package is correct

### Issue 2: High Memory Usage

**Symptoms:**
- Memory > 200MB
- OutOfMemoryError in logs
- App becomes slow

**Debugging:**
```bash
# Memory breakdown
adb shell dumpsys meminfo com.augmentalis.voiceaccessibility

# Check for leaks
adb logcat | grep "GC_FOR_ALLOC"

# Heap dump
adb shell am dumpheap com.augmentalis.voiceaccessibility /sdcard/heap.hprof
```

**Solutions:**
1. Check AccessibilityNodeInfo are recycled
2. Verify database cursors are closed
3. Check for bitmap leaks
4. Review static references

### Issue 3: Slow Command Execution

**Symptoms:**
- Commands take > 1 second
- UI feels sluggish
- Performance warnings in logs

**Debugging:**
```bash
# Check execution times
adb logcat | grep "took.*ms" | awk '$NF > 500 {print}'

# Profile database queries
adb logcat | grep "Query took"

# CPU usage
adb shell top -n 1 | grep voiceaccessibility
```

**Solutions:**
1. Add database indices
2. Cache frequently used queries
3. Optimize command matching algorithm
4. Reduce accessibility event frequency

### Issue 4: Database Corruption

**Symptoms:**
- SQLite error in logs
- Commands disappear
- App crashes on startup

**Debugging:**
```bash
# Check integrity
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/app_scraping_database \
  'PRAGMA integrity_check;'"

# Check file exists
adb shell "run-as com.augmentalis.voiceaccessibility \
  ls -lh databases/"
```

**Solutions:**
1. Add WAL mode: `PRAGMA journal_mode=WAL;`
2. Add error handling for database operations
3. Implement database backup/restore
4. Clear data and rescan if corrupted

---

## Summary

This developer testing guide provides comprehensive tools and techniques for:

✅ **Real-time monitoring** of all VoiceOS 4 components
✅ **Database inspection** while app is running
✅ **Command flow tracing** through all 3 tiers
✅ **Performance profiling** with timing and metrics
✅ **Memory and CPU monitoring** to prevent resource issues
✅ **Accessibility event tracking** for UI scraping validation
✅ **Speech recognition validation** for accuracy measurement
✅ **Network traffic analysis** for online engines
✅ **Debugging tools** integration with Android Studio
✅ **Automated validation scripts** for continuous testing

### Quick Reference Commands

```bash
# Monitor everything
./voiceos-monitor-all.sh

# Watch command execution
adb logcat | grep -E "✓ Tier|✗ ALL"

# Check database
adb shell "run-as com.augmentalis.voiceaccessibility \
  sqlite3 databases/app_scraping_database 'SELECT COUNT(*) FROM generated_commands;'"

# Monitor performance
adb logcat | grep "PERF" | grep "took"

# Check memory
adb shell dumpsys meminfo com.augmentalis.voiceaccessibility | grep "TOTAL"

# Export logs
adb logcat > voiceos_$(date +%Y%m%d_%H%M%S).log
```

---

**Document Version:** 3.0.0
**Last Updated:** 2025-10-13 22:32 PDT
**Status:** Ready for Development Use
**Related Documents:**
- VoiceOS4-System-Architecture-Complete-251013-2144.md
- VoiceOS4-Developer-Guide-Complete-251013-2144.md
- VoiceOS4-Manual-Testing-Guide-251013-2147.md
