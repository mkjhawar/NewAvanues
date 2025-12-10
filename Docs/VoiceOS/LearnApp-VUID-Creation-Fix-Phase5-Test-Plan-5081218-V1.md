# LearnApp VUID Creation Fix - Phase 5 Test Plan

**Version**: 1.0
**Date**: 2025-12-08
**Status**: ACTIVE
**Related Documents**:
- Spec: [LearnApp-VUID-Creation-Fix-Spec-5081218-V1.md](./LearnApp-VUID-Creation-Fix-Spec-5081218-V1.md)
- Plan: [LearnApp-VUID-Creation-Fix-Plan-5081218-V1.md](./LearnApp-VUID-Creation-Fix-Plan-5081218-V1.md)

---

## Executive Summary

**Goal**: Validate VUID creation fix works across 7 diverse apps with 95%+ creation rate.

**Test Apps**:
1. DeviceInfo (com.ytheekshana.deviceinfo) - Original failure case
2. Microsoft Teams - Baseline (should maintain 95%+)
3. Google News - Tab navigation test
4. Amazon - Product cards test
5. Android Settings - System preferences test
6. Facebook - Social media UI test
7. Custom synthetic test app - Edge cases

**Success Criteria**:
- All 7 apps: 95%+ VUID creation rate
- Voice commands work correctly
- Performance overhead <10%
- No regressions in existing functionality
- Memory usage acceptable

---

## Test Configuration

### Test Device
- **Device**: RealWear Navigator 500 (or emulator)
- **Android Version**: 10+ (API 29+)
- **VoiceOS Build**: VOS4 Development branch

### Test Environment
```bash
# Environment setup
DEVICE_SERIAL=$(adb devices | awk 'NR==2 {print $1}')
TEST_DATE=$(date "+%Y%m%d-%H%M%S")
RESULTS_DIR="/Volumes/M-Drive/Coding/NewAvanues/Docs/VoiceOS/Testing/VUID-Phase5-${TEST_DATE}"
```

---

## Test 1: DeviceInfo (Original Failure Case)

### Pre-Test Setup
```bash
# Install app
adb install -r DeviceInfo.apk

# Clear existing data
adb shell pm clear com.ytheekshana.deviceinfo
adb shell am broadcast -a com.augmentalis.voiceos.DELETE_VUIDS --es package com.ytheekshana.deviceinfo
```

### Test Execution
```bash
# Launch exploration
adb shell am start -n com.augmentalis.voiceos/.learnapp.LearnAppActivity \
    --es target_package com.ytheekshana.deviceinfo \
    --ei timeout 1080000

# Monitor progress (logcat filter)
adb logcat -s LearnApp:* ExplorationEngine:* UUIDCreator:* | tee ${RESULTS_DIR}/deviceinfo-logcat.txt
```

### Metrics Collection
```bash
# After exploration completes (18 min timeout)

# 1. Total elements detected
ELEMENTS_DETECTED=$(adb shell "run-as com.augmentalis.voiceos cat databases/learnapp.db" | \
    sqlite3 ":memory:" "SELECT COUNT(*) FROM elements WHERE package='com.ytheekshana.deviceinfo'")

# 2. VUIDs created
VUIDS_CREATED=$(adb shell "run-as com.augmentalis.voiceos cat databases/voiceos.db" | \
    sqlite3 ":memory:" "SELECT COUNT(*) FROM vuids WHERE packageName='com.ytheekshana.deviceinfo'")

# 3. Creation rate
CREATION_RATE=$(echo "scale=2; $VUIDS_CREATED / $ELEMENTS_DETECTED * 100" | bc)

# 4. Breakdown by type
adb shell "run-as com.augmentalis.voiceos sqlite3 databases/learnapp.db \
    \"SELECT className, COUNT(*) as count \
     FROM elements \
     WHERE package='com.ytheekshana.deviceinfo' \
     GROUP BY className\""
```

### Expected Results
| Metric | Before Fix | Target | Status |
|--------|-----------|--------|--------|
| Elements detected | 117 | 117 | TBD |
| VUIDs created | 1 | 117 | TBD |
| Creation rate | 0.85% | 95%+ | TBD |
| LinearLayout VUIDs | 0/78 | 78/78 | TBD |
| CardView VUIDs | 0/22 | 22/22 | TBD |
| Button VUIDs | 0/5 | 5/5 | TBD |

### Voice Command Tests
```bash
# Test 1: Tab navigation
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Select CPU tab"

# Expected: Navigate to CPU tab
# Verify: Activity name changes, CPU content visible

# Test 2: Card interaction
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Open tests card"

# Expected: Open Tests section
# Verify: Tests detail screen appears

# Test 3: Button action
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Rate this app"

# Expected: Play Store opens
# Verify: Package changed to com.android.vending
```

---

## Test 2: Microsoft Teams (Baseline)

### Pre-Test Setup
```bash
# Ensure Teams installed (or download)
adb shell pm list packages | grep teams

# Clear existing data
adb shell pm clear com.microsoft.teams
adb shell am broadcast -a com.augmentalis.voiceos.DELETE_VUIDS --es package com.microsoft.teams
```

### Test Execution
```bash
# Launch exploration
adb shell am start -n com.augmentalis.voiceos/.learnapp.LearnAppActivity \
    --es target_package com.microsoft.teams \
    --ei timeout 1080000
```

### Expected Results
| Metric | Baseline | Target | Status |
|--------|----------|--------|--------|
| Elements detected | ~150 | ~150 | TBD |
| VUIDs created | ~140 | ~140 | TBD |
| Creation rate | 95%+ | 95%+ | TBD |

### Voice Command Tests
```bash
# Test navigation
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Open chat"

# Test interaction
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Select new message"
```

---

## Test 3: Google News (Tab Navigation)

### Pre-Test Setup
```bash
# Install Google News
adb install -r GoogleNews.apk

# Clear data
adb shell pm clear com.google.android.apps.magazines
adb shell am broadcast -a com.augmentalis.voiceos.DELETE_VUIDS --es package com.google.android.apps.magazines
```

### Test Focus
- Tab navigation (For You, Following, Newsstand)
- News card interactions
- Material Design components

### Voice Command Tests
```bash
# Tab navigation
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Select Following tab"

# Card interaction
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Open first news card"
```

### Expected Results
| Metric | Target | Status |
|--------|--------|--------|
| Tab VUIDs | 100% | TBD |
| Card VUIDs | 95%+ | TBD |
| Overall creation rate | 95%+ | TBD |

---

## Test 4: Amazon (Product Cards)

### Pre-Test Setup
```bash
# Install Amazon
adb install -r Amazon.apk

# Clear data
adb shell pm clear com.amazon.mShop.android.shopping
adb shell am broadcast -a com.augmentalis.voiceos.DELETE_VUIDS --es package com.amazon.mShop.android.shopping
```

### Test Focus
- Product card grids
- Category navigation
- Complex layouts with nested containers

### Voice Command Tests
```bash
# Product interaction
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Select first product"

# Add to cart
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Add to cart"
```

### Expected Results
| Metric | Target | Status |
|--------|--------|--------|
| Product card VUIDs | 95%+ | TBD |
| Button VUIDs | 100% | TBD |
| Overall creation rate | 95%+ | TBD |

---

## Test 5: Android Settings (System Preferences)

### Pre-Test Setup
```bash
# Settings is system app (no install needed)

# Clear data
adb shell am broadcast -a com.augmentalis.voiceos.DELETE_VUIDS --es package com.android.settings
```

### Test Focus
- Preference cards
- Switch controls
- System UI components

### Voice Command Tests
```bash
# Navigation
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Open Wi-Fi settings"

# Toggle
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Toggle Bluetooth"
```

### Expected Results
| Metric | Target | Status |
|--------|--------|--------|
| Preference VUIDs | 95%+ | TBD |
| Switch VUIDs | 100% | TBD |
| Overall creation rate | 95%+ | TBD |

---

## Test 6: Facebook (Social Media UI)

### Pre-Test Setup
```bash
# Install Facebook
adb install -r Facebook.apk

# Clear data
adb shell pm clear com.facebook.katana
adb shell am broadcast -a com.augmentalis.voiceos.DELETE_VUIDS --es package com.facebook.katana
```

### Test Focus
- Tab bar navigation
- Feed cards
- Dynamic content
- Custom views

### Voice Command Tests
```bash
# Tab navigation
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Select notifications tab"

# Feed interaction
adb shell am broadcast -a com.augmentalis.voiceos.VOICE_COMMAND \
    --es command "Like first post"
```

### Expected Results
| Metric | Target | Status |
|--------|--------|--------|
| Tab VUIDs | 100% | TBD |
| Feed card VUIDs | 95%+ | TBD |
| Overall creation rate | 95%+ | TBD |

---

## Test 7: Custom Synthetic Test App (Edge Cases)

### Test App Code
```kotlin
// File: TestClickabilityActivity.kt
// Location: /android/testapp/src/main/java/com/augmentalis/testapp/

package com.augmentalis.testapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView

class TestClickabilityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)

            // CASE 1: LinearLayout tab (isClickable=false, but should create VUID)
            addView(LinearLayout(this@TestClickabilityActivity).apply {
                id = View.generateViewId()
                isFocusable = true
                setOnClickListener { showToast("CPU Tab Clicked") }
                setPadding(16, 16, 16, 16)
                setBackgroundColor(Color.LTGRAY)

                addView(TextView(this@TestClickabilityActivity).apply {
                    text = "CPU Tab"
                    textSize = 16f
                })
            })

            addView(View(this@TestClickabilityActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    8
                )
            })

            // CASE 2: CardView (isClickable=false, but should create VUID)
            addView(CardView(this@TestClickabilityActivity).apply {
                id = View.generateViewId()
                isFocusable = true
                setOnClickListener { showToast("Tests Card Clicked") }
                radius = 8f
                cardElevation = 4f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }

                addView(LinearLayout(this@TestClickabilityActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)

                    addView(TextView(this@TestClickabilityActivity).apply {
                        text = "Tests"
                        textSize = 18f
                        setTextColor(Color.BLACK)
                    })

                    addView(TextView(this@TestClickabilityActivity).apply {
                        text = "View test results"
                        textSize = 14f
                        setTextColor(Color.GRAY)
                    })
                })
            })

            // CASE 3: FrameLayout wrapper (should detect child clickability)
            addView(FrameLayout(this@TestClickabilityActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }

                addView(Button(this@TestClickabilityActivity).apply {
                    text = "Submit"
                    setOnClickListener { showToast("Submit Clicked") }
                })
            })

            // CASE 4: Decorative ImageView (should be filtered)
            addView(ImageView(this@TestClickabilityActivity).apply {
                layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                    setMargins(0, 0, 0, 16)
                }
                setImageResource(android.R.drawable.ic_dialog_info)
                // No click listener, no text, no description
            })

            // CASE 5: Divider (should be filtered)
            addView(View(this@TestClickabilityActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(Color.GRAY)
            })

            // CASE 6: Explicit button (isClickable=true, should create VUID)
            addView(Button(this@TestClickabilityActivity).apply {
                text = "Rate This App"
                setOnClickListener { showToast("Rate App Clicked") }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 0)
                }
            })

            // CASE 7: MaterialCardView (Material Design)
            addView(MaterialCardView(this@TestClickabilityActivity).apply {
                id = View.generateViewId()
                isFocusable = true
                setOnClickListener { showToast("Material Card Clicked") }
                radius = 12f
                cardElevation = 6f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 0)
                }

                addView(TextView(this@TestClickabilityActivity).apply {
                    text = "Material Card"
                    textSize = 16f
                    setPadding(16, 16, 16, 16)
                })
            })
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
```

### Expected Results
| Case | Element Type | isClickable | Should Create VUID | Status |
|------|-------------|-------------|-------------------|--------|
| 1 | LinearLayout tab | false | YES (isFocusable + click listener) | TBD |
| 2 | CardView | false | YES (isFocusable + click listener) | TBD |
| 3 | FrameLayout wrapper | false | YES (clickable child) | TBD |
| 4 | Decorative ImageView | false | NO (no text/description) | TBD |
| 5 | Divider View | false | NO (empty, no children) | TBD |
| 6 | Button | true | YES (explicit clickable) | TBD |
| 7 | MaterialCardView | false | YES (isFocusable + click listener) | TBD |

**Target**: 5/7 VUIDs created (cases 1,2,3,6,7)

---

## Performance Profiling

### Metrics to Measure

#### 1. VUID Creation Overhead
```bash
# Enable performance logging
adb shell setprop log.tag.UUIDCreator VERBOSE
adb shell setprop log.tag.ClickabilityDetector VERBOSE

# Extract timing data from logcat
adb logcat -d | grep -E "VUID creation time|Clickability scoring time" > ${RESULTS_DIR}/performance-timings.txt

# Calculate averages
awk '/VUID creation time/ {sum+=$NF; count++} END {print "Avg VUID creation:", sum/count, "ms"}' ${RESULTS_DIR}/performance-timings.txt
```

**Target**: <50ms per element

#### 2. Total Exploration Time
```bash
# Before fix (baseline)
BASELINE_TIME="18 minutes"  # DeviceInfo exploration time

# After fix
START_TIME=$(date +%s)
# ... run exploration ...
END_TIME=$(date +%s)
EXPLORATION_TIME=$((END_TIME - START_TIME))

# Calculate increase
INCREASE_PERCENT=$(echo "scale=2; ($EXPLORATION_TIME - 1080) / 1080 * 100" | bc)
```

**Target**: <10% increase (max 19.8 minutes)

#### 3. Memory Usage
```bash
# Before exploration
adb shell dumpsys meminfo com.augmentalis.voiceos > ${RESULTS_DIR}/memory-before.txt

# During exploration (peak)
watch -n 5 "adb shell dumpsys meminfo com.augmentalis.voiceos | grep 'TOTAL PSS'" > ${RESULTS_DIR}/memory-during.txt

# After exploration
adb shell dumpsys meminfo com.augmentalis.voiceos > ${RESULTS_DIR}/memory-after.txt

# Calculate delta
BEFORE_PSS=$(grep "TOTAL PSS:" ${RESULTS_DIR}/memory-before.txt | awk '{print $3}')
AFTER_PSS=$(grep "TOTAL PSS:" ${RESULTS_DIR}/memory-after.txt | awk '{print $3}')
MEMORY_INCREASE=$((AFTER_PSS - BEFORE_PSS))
```

**Target**: <5MB additional memory

#### 4. CPU Usage
```bash
# Profile during exploration
adb shell top -m 1 -n 60 > ${RESULTS_DIR}/cpu-usage.txt

# Extract VoiceOS CPU %
grep "com.augmentalis.voiceos" ${RESULTS_DIR}/cpu-usage.txt | awk '{sum+=$9; count++} END {print "Avg CPU:", sum/count, "%"}'
```

**Target**: CPU usage remains acceptable (no thermal throttling)

---

## Regression Testing

### Unit Tests
```bash
# Run existing test suite
cd /Volumes/M-Drive/Coding/NewAvanues
./gradlew :VoiceOS:apps:VoiceOSCore:testDebugUnitTest --tests "*.learnapp.*"

# Expected: All tests pass
```

### Integration Tests
```bash
# Run LearnApp integration tests
./gradlew :VoiceOS:apps:VoiceOSCore:connectedDebugAndroidTest --tests "*.learnapp.integration.*"

# Expected: All tests pass
```

### Smoke Tests
```bash
# 1. Basic VUID registration
adb shell am broadcast -a com.augmentalis.voiceos.TEST_VUID_REGISTRATION

# 2. Voice command processing
adb shell am broadcast -a com.augmentalis.voiceos.TEST_VOICE_COMMAND \
    --es command "Click first button"

# 3. Database operations
adb shell am broadcast -a com.augmentalis.voiceos.TEST_DATABASE_OPS

# Expected: All smoke tests pass
```

---

## Test Execution Checklist

### Pre-Execution
- [ ] VoiceOS built with Phase 1-4 implementation
- [ ] Test device/emulator ready
- [ ] All test apps installed or available
- [ ] Test environment variables set
- [ ] Results directory created
- [ ] Baseline metrics recorded

### Execution Order
1. [ ] Test 7: Custom synthetic test app (fastest, validates core logic)
2. [ ] Test 1: DeviceInfo (original failure case)
3. [ ] Test 2: Microsoft Teams (baseline regression)
4. [ ] Test 3: Google News (tab navigation)
5. [ ] Test 4: Amazon (product cards)
6. [ ] Test 5: Android Settings (system UI)
7. [ ] Test 6: Facebook (social media)

### Per-App Testing
- [ ] Pre-test setup complete
- [ ] Exploration launched
- [ ] Metrics collected
- [ ] Voice commands tested
- [ ] Screenshots captured
- [ ] Logs saved
- [ ] Results documented

### Performance Testing
- [ ] Timing measurements collected
- [ ] Memory profiling complete
- [ ] CPU usage monitored
- [ ] Results analyzed

### Regression Testing
- [ ] Unit tests run
- [ ] Integration tests run
- [ ] Smoke tests run
- [ ] All tests passing

---

## Results Collection

### Data Points Per App
```bash
# Template for results
cat > ${RESULTS_DIR}/app-results-template.txt << 'EOF'
App: [APP_NAME]
Package: [PACKAGE_NAME]
Test Date: [DATE_TIME]

VUID Creation Metrics:
- Elements Detected: [COUNT]
- VUIDs Created: [COUNT]
- Creation Rate: [PERCENTAGE]%

By Element Type:
- LinearLayout: [CREATED]/[TOTAL] ([PERCENTAGE]%)
- CardView: [CREATED]/[TOTAL] ([PERCENTAGE]%)
- Button: [CREATED]/[TOTAL] ([PERCENTAGE]%)
- ImageButton: [CREATED]/[TOTAL] ([PERCENTAGE]%)
- [OTHER_TYPES...]

Voice Command Tests:
- Command 1: [COMMAND] -> [PASS/FAIL] ([DETAILS])
- Command 2: [COMMAND] -> [PASS/FAIL] ([DETAILS])
- Command 3: [COMMAND] -> [PASS/FAIL] ([DETAILS])

Performance:
- Exploration Time: [MINUTES]
- VUID Creation Overhead: [MS] per element
- Memory Usage: [MB]
- CPU Usage: [PERCENTAGE]%

Status: [PASS/FAIL]
Issues: [LIST_ISSUES_IF_ANY]
EOF
```

### Aggregate Report
```bash
# Summary across all apps
cat > ${RESULTS_DIR}/validation-summary.txt << 'EOF'
LearnApp VUID Creation Fix - Phase 5 Validation Summary
========================================================

Test Date: [DATE]
Duration: [HOURS]
Apps Tested: 7

Overall Results:
- Apps Passing (95%+ creation rate): [COUNT]/7
- Average Creation Rate: [PERCENTAGE]%
- Voice Commands Working: [COUNT]/[TOTAL]

Performance:
- Average Overhead: [MS] per element
- Average Exploration Time Increase: [PERCENTAGE]%
- Memory Impact: [MB]

Regression Tests:
- Unit Tests: [PASS/FAIL] ([PASSED]/[TOTAL])
- Integration Tests: [PASS/FAIL] ([PASSED]/[TOTAL])
- Smoke Tests: [PASS/FAIL] ([PASSED]/[TOTAL])

Status: [PASS/FAIL]
Blockers: [LIST_IF_ANY]
Notes: [ADDITIONAL_NOTES]
EOF
```

---

## Acceptance Criteria

### Must-Pass Criteria (Blocking)
- [ ] **All 7 apps achieve 95%+ VUID creation rate**
- [ ] **DeviceInfo: 117/117 VUIDs created (100%)**
- [ ] **No regressions: Microsoft Teams maintains 95%+ rate**
- [ ] **All voice commands work as expected**
- [ ] **Performance overhead <10%**
- [ ] **All existing tests pass**

### Should-Pass Criteria (Non-Blocking)
- [ ] Memory usage <5MB additional
- [ ] CPU usage remains acceptable
- [ ] No ANRs during exploration
- [ ] Logs clean (no ERROR level messages related to VUID creation)

---

## Failure Handling

### If App Fails to Achieve 95%+ Rate

1. **Capture Diagnostics**
```bash
# Full element dump
adb shell dumpsys accessibility > ${RESULTS_DIR}/accessibility-dump-${APP}.txt

# Filter logs
adb logcat -d | grep -E "ElementFilterLogger|ClickabilityDetector" > ${RESULTS_DIR}/filter-logs-${APP}.txt

# Database export
adb exec-out run-as com.augmentalis.voiceos sqlite3 databases/learnapp.db .dump > ${RESULTS_DIR}/db-dump-${APP}.sql
```

2. **Analyze Filtered Elements**
```bash
# Find ERROR severity filters (isClickable=true but filtered)
grep "ERROR.*Misfiltered" ${RESULTS_DIR}/filter-logs-${APP}.txt

# Find WARNING severity filters (suspicious)
grep "WARNING.*Suspicious" ${RESULTS_DIR}/filter-logs-${APP}.txt
```

3. **Root Cause Analysis**
- Check if element types are new/unexpected
- Verify clickability scoring signals
- Review resource ID patterns
- Check for custom view types

4. **Document and Escalate**
- Create bug report in `/Docs/VoiceOS/Testing/Phase5-Failures/`
- Include full diagnostics
- Propose fix or threshold adjustment

---

## Test Report Template

See: `LearnApp-VUID-Creation-Fix-Phase5-Test-Report-5081218-V1.md`

---

**Document Version**: 1.0
**Last Updated**: 2025-12-08
**Author**: Claude Code (IDEACODE v10.3)
**Status**: READY FOR EXECUTION
**Next Step**: Execute Test 7 (Custom Synthetic Test App)
