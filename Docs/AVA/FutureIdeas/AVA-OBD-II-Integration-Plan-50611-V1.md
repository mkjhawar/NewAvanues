# OBD-II Integration Implementation Plan

**Status:** Future Feature - On Hold
**Priority:** HIGH (for automotive use case)
**Estimated Time:** 3-4 weeks
**Estimated Cost:** $24,000 development + $50 hardware

---

## Overview

Integration with vehicle OBD-II (On-Board Diagnostics) port via Bluetooth adapter to:
- Read diagnostic trouble codes (DTCs)
- Explain codes in plain English
- Cross-reference with vehicle manual
- Monitor real-time vehicle data
- Clear codes after repair
- Provide repair guidance

---

## Business Value

### Target Users
- DIY car enthusiasts
- Professional mechanics
- Fleet managers
- Auto repair shops

### Key Benefits
- **Instant diagnostics:** No need for separate code reader
- **Context-aware:** Links codes to manual sections
- **Cost savings:** Understand issues before shop visit
- **Preventive maintenance:** Monitor trends over time
- **Competitive advantage:** Unique feature vs competitors

### Market Differentiation
- Google Assistant: ❌ No OBD-II support
- ChatGPT: ❌ No hardware integration
- Torque Pro: ⚠️ OBD-II only, no AI assistance
- AVA: ✅ OBD-II + AI + Manual integration

---

## Technical Architecture

### Component Overview

```
┌─────────────────────────────────────────────┐
│           AVA Application                    │
├─────────────────────────────────────────────┤
│  Voice Interface                             │
│  "Hey AVA, check my engine"                  │
├─────────────────────────────────────────────┤
│  OBDIIManager                                │
│  - Bluetooth connection                      │
│  - Command protocol (ELM327)                 │
│  - Response parsing                          │
├─────────────────────────────────────────────┤
│  DiagnosticDatabase                          │
│  - 5,000+ DTC codes                          │
│  - Descriptions & severity                   │
│  - Common causes                             │
│  - Repair steps                              │
├─────────────────────────────────────────────┤
│  RAG Integration                             │
│  - Search manual for code                    │
│  - Find repair procedures                    │
│  - Extract relevant sections                 │
├─────────────────────────────────────────────┤
│  LLM Integration                             │
│  - Explain in plain English                  │
│  - Provide context & urgency                 │
│  - Suggest next steps                        │
└─────────────────────────────────────────────┘
         ↕ Bluetooth
┌─────────────────────────────────────────────┐
│     OBD-II Bluetooth Adapter                 │
│     (ELM327 compatible)                      │
└─────────────────────────────────────────────┘
         ↕ OBD-II Protocol
┌─────────────────────────────────────────────┐
│     Vehicle ECU                              │
└─────────────────────────────────────────────┘
```

---

## Implementation Phases

### Phase 1: Bluetooth OBD-II Communication (Week 1-2)

#### 1.1 OBDIIManager Class
```kotlin
class OBDIIManager(
    private val bluetoothAdapter: BluetoothAdapter
) {
    suspend fun connect(deviceAddress: String): Result<Unit>
    suspend fun disconnect()
    suspend fun readDiagnosticCodes(): Result<List<DTCCode>>
    suspend fun clearDiagnosticCodes(): Result<Unit>
    suspend fun readRealTimeData(pid: String): Result<String>
    suspend fun readVIN(): Result<String>
    suspend fun readFreezeFrame(): Result<Map<String, String>>
}
```

**Dependencies:**
```kotlin
dependencies {
    implementation("androidx.bluetooth:bluetooth:1.0.0-alpha02")
    // No external OBD library needed - implement ELM327 protocol directly
}
```

**Permissions:**
```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

#### 1.2 ELM327 Protocol Implementation
- AT commands for initialization
- Mode 01: Live data
- Mode 03: Read DTCs
- Mode 04: Clear DTCs
- Mode 09: Vehicle info (VIN)

**Commands:**
```
ATZ          - Reset
ATE0         - Echo off
ATL0         - Linefeeds off
ATSP0        - Auto protocol
03           - Request DTCs
01 0D        - Read vehicle speed
01 05        - Read coolant temp
09 02        - Read VIN
04           - Clear DTCs
```

#### 1.3 Testing Hardware
**Compatible Adapters:**
- BAFX Products OBD Reader ($23)
- BlueDriver LSB2 ($100)
- OBDLink MX+ ($100)
- Generic ELM327 v1.5 ($15-25)

**Test Vehicles:**
- 2015 Honda Civic (primary test vehicle)
- 2018 Toyota Camry
- 2020 Ford F-150

---

### Phase 2: DTC Database (Week 2)

#### 2.1 Database Schema
```kotlin
@Entity(tableName = "dtc_codes")
data class DTCCodeEntity(
    @PrimaryKey val code: String,              // "P0420"
    val description: String,                    // Long description
    val short_description: String,              // Brief summary
    val severity: Severity,                     // CRITICAL, WARNING, INFO
    val system: String,                         // "Emission Control"
    val common_causes: String,                  // JSON array
    val symptoms: String,                       // JSON array
    val repair_steps: String,                   // JSON array
    val required_tools: String,                 // JSON array
    val difficulty: RepairDifficulty,           // EASY, MEDIUM, HARD, EXPERT
    val estimated_time_minutes: Int?,           // Repair time
    val estimated_cost_min: Int?,               // Min cost in dollars
    val estimated_cost_max: Int?,               // Max cost in dollars
    val should_drive: Boolean,                  // Safe to drive?
    val urgency_days: Int?                      // How soon to fix
)

enum class Severity {
    CRITICAL,      // Stop driving immediately
    WARNING,       // Fix soon (days/weeks)
    INFO,          // Informational, monitor
    MAINTENANCE    // Scheduled maintenance
}

enum class RepairDifficulty {
    DIY_EASY,      // Anyone can do it
    DIY_MEDIUM,    // Some mechanical skill
    DIY_HARD,      // Advanced DIY
    PROFESSIONAL   // Requires shop/special tools
}
```

#### 2.2 DTC Code Categories
**P-codes (Powertrain):** ~4,000 codes
- P0xxx: Generic (SAE standard)
- P1xxx: Manufacturer-specific

**C-codes (Chassis):** ~500 codes
**B-codes (Body):** ~300 codes
**U-codes (Network):** ~200 codes

**Total:** ~5,000 codes

#### 2.3 Data Sources
1. **SAE J2012 Standard:** Official DTC definitions
2. **EOBD/OBD-II Standards:** European/US codes
3. **Manufacturer TSBs:** Technical Service Bulletins
4. **Community databases:** OBD-Codes.com, OBD2-Codes.com
5. **Manual curation:** AVA team additions

#### 2.4 Database Population
```kotlin
class DTCDatabaseSeeder {
    suspend fun seedDatabase() {
        // Load from JSON asset
        val json = context.assets.open("dtc_codes.json").bufferedReader().use { it.readText() }
        val codes = Json.decodeFromString<List<DTCCodeEntity>>(json)

        database.dtcDao().insertAll(codes)
    }
}
```

**File:** `assets/dtc_codes.json` (~2-3 MB)

---

### Phase 3: Diagnostic Assistant (Week 3)

#### 3.1 DiagnosticAssistant Class
```kotlin
class DiagnosticAssistant(
    private val obdManager: OBDIIManager,
    private val dtcDatabase: DTCDatabase,
    private val ragRepository: RAGRepository,
    private val llm: MLCEngine
) {
    suspend fun performFullDiagnostic(): DiagnosticReport {
        // 1. Connect to vehicle
        obdManager.connect(savedDeviceAddress)

        // 2. Read codes
        val dtcCodes = obdManager.readDiagnosticCodes().getOrThrow()

        // 3. Look up code info
        val codeInfos = dtcCodes.mapNotNull {
            dtcDatabase.lookupCode(it.code)
        }

        // 4. Search manual
        val manualSections = searchManualForCodes(codeInfos)

        // 5. Read real-time data
        val realtimeData = readRelevantPIDs(codeInfos)

        // 6. Generate report
        return DiagnosticReport(
            codes = codeInfos,
            manualReferences = manualSections,
            realtimeData = realtimeData,
            overallSeverity = calculateSeverity(codeInfos),
            recommendedActions = generateRecommendations(codeInfos),
            estimatedCost = calculateCostRange(codeInfos),
            safetyAssessment = assessDrivingSafety(codeInfos)
        )
    }

    suspend fun explainToUser(report: DiagnosticReport): String {
        val prompt = buildPrompt(report)
        return llm.generate(prompt)
    }
}
```

#### 3.2 Manual Integration
```kotlin
private suspend fun searchManualForCodes(
    codes: List<DTCCodeInfo>
): List<SearchResult> {
    return codes.flatMap { codeInfo ->
        // Search for code number
        val codeResults = ragRepository.search(
            SearchQuery(query = codeInfo.code, maxResults = 2)
        ).getOrNull()?.results ?: emptyList()

        // Search for description/system
        val descResults = ragRepository.search(
            SearchQuery(query = codeInfo.description, maxResults = 3)
        ).getOrNull()?.results ?: emptyList()

        codeResults + descResults
    }.distinctBy { it.chunk.id }
}
```

#### 3.3 LLM Explanation Prompt
```kotlin
private fun buildPrompt(report: DiagnosticReport): String {
    return """
You are AVA, an automotive diagnostic assistant. Explain these diagnostic codes to the user in plain English.

Codes found:
${report.codes.joinToString("\n") {
    "${it.code} - ${it.description} (${it.severity})"
}}

Manual information:
${report.manualReferences.joinToString("\n\n") {
    "Page ${it.chunk.metadata["page_number"]}: ${it.chunk.content.take(200)}"
}}

Real-time data:
${report.realtimeData.entries.joinToString("\n") { "${it.key}: ${it.value}" }}

Provide:
1. What's wrong (in simple terms)
2. How serious it is
3. Can they drive safely?
4. What they should do
5. Estimated cost: ${report.estimatedCost}
6. Timeline to fix: ${report.codes.minOfOrNull { it.urgency_days ?: 30 }} days

Be professional but reassuring. Cite manual page numbers.
""".trimIndent()
}
```

---

### Phase 4: UI & Voice Integration (Week 4)

#### 4.1 Diagnostic Screen
```kotlin
@Composable
fun DiagnosticScreen(viewModel: DiagnosticViewModel) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Connection status
        ConnectionStatusCard(
            connected = state.connected,
            deviceName = state.deviceName,
            onConnect = { viewModel.connectToDevice() }
        )

        // Active codes
        if (state.codes.isNotEmpty()) {
            LazyColumn {
                items(state.codes) { code ->
                    DTCCodeCard(
                        code = code,
                        onExplain = { viewModel.explainCode(code) },
                        onViewManual = { viewModel.openManualSection(code) }
                    )
                }
            }

            // Action buttons
            Row {
                Button(onClick = { viewModel.clearCodes() }) {
                    Text("Clear Codes")
                }
                Button(onClick = { viewModel.exportReport() }) {
                    Text("Export Report")
                }
            }
        } else {
            NoCodesFoundCard()
        }

        // Real-time data
        RealTimeDataSection(data = state.realtimeData)
    }
}
```

#### 4.2 Voice Commands
```
User: "Hey AVA, check my engine"
AVA: "Connecting to your vehicle... Found 2 diagnostic codes."

User: "What's wrong?"
AVA: "You have code P0420 - Catalyst System Efficiency Below Threshold,
      and P0171 - System Too Lean. The catalytic converter issue is
      not urgent but should be fixed within a few weeks. The lean
      condition could be a vacuum leak - I'd get that checked soon.
      Your manual has details on page 342."

User: "Can I still drive?"
AVA: "Yes, it's safe to drive, but you might notice slightly reduced
      fuel economy. I recommend scheduling a diagnostic within
      the next 2 weeks."

User: "How much will it cost?"
AVA: "The catalytic converter replacement typically costs $800-1200.
      The vacuum leak repair is usually $100-300. Total estimated:
      $900-1500."

User: "Clear the codes"
AVA: "Are you sure? I recommend fixing the issues first, otherwise
      the codes will come back."

User: "Yes, clear them"
AVA: "Codes cleared. The check engine light should turn off within
      a few drive cycles."
```

#### 4.3 Android Auto Integration
```kotlin
class DiagnosticCarScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return ListTemplate.Builder()
            .setTitle("Vehicle Diagnostics")
            .setSingleList(
                ItemList.Builder()
                    .addItem(
                        Row.Builder()
                            .setTitle("Check Engine Light: ON")
                            .addText("2 codes found")
                            .setOnClickListener { showDiagnosticDetails() }
                            .build()
                    )
                    .addItem(
                        Row.Builder()
                            .setTitle("Read Codes")
                            .addText("Connect to OBD-II adapter")
                            .setOnClickListener { startDiagnostic() }
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
```

---

## Database Schema

### DTC Codes Table
```sql
CREATE TABLE dtc_codes (
    code TEXT PRIMARY KEY,
    description TEXT NOT NULL,
    short_description TEXT NOT NULL,
    severity TEXT NOT NULL,
    system TEXT NOT NULL,
    common_causes TEXT,
    symptoms TEXT,
    repair_steps TEXT,
    required_tools TEXT,
    difficulty TEXT,
    estimated_time_minutes INTEGER,
    estimated_cost_min INTEGER,
    estimated_cost_max INTEGER,
    should_drive BOOLEAN,
    urgency_days INTEGER
);

CREATE INDEX idx_severity ON dtc_codes(severity);
CREATE INDEX idx_system ON dtc_codes(system);
```

### Diagnostic History Table
```sql
CREATE TABLE diagnostic_history (
    id TEXT PRIMARY KEY,
    timestamp TEXT NOT NULL,
    vehicle_vin TEXT,
    mileage INTEGER,
    codes_json TEXT,
    realtime_data_json TEXT,
    notes TEXT
);

CREATE INDEX idx_timestamp ON diagnostic_history(timestamp);
CREATE INDEX idx_vin ON diagnostic_history(vehicle_vin);
```

### Vehicle Profile Table
```sql
CREATE TABLE vehicle_profiles (
    vin TEXT PRIMARY KEY,
    make TEXT,
    model TEXT,
    year INTEGER,
    obd_device_address TEXT,
    current_mileage INTEGER,
    last_diagnostic_timestamp TEXT,
    manual_document_id TEXT,
    FOREIGN KEY(manual_document_id) REFERENCES documents(id)
);
```

---

## Testing Strategy

### Unit Tests
- [ ] OBD-II command formatting
- [ ] Response parsing (various formats)
- [ ] DTC code lookup
- [ ] Severity calculation
- [ ] Cost estimation

### Integration Tests
- [ ] Bluetooth connection flow
- [ ] Read codes from simulator
- [ ] Clear codes
- [ ] Manual search integration
- [ ] LLM explanation generation

### Hardware Tests
- [ ] Test with 5+ OBD-II adapters
- [ ] Test on 10+ vehicle models
- [ ] Test various DTC combinations
- [ ] Test in different scenarios (parked, driving, etc.)

### Edge Cases
- [ ] No Bluetooth available
- [ ] Adapter not found
- [ ] Connection lost mid-read
- [ ] No codes present
- [ ] Unknown/rare codes
- [ ] Multiple critical codes
- [ ] Non-ELM327 adapters

---

## Security & Safety

### Permissions
- Request location only when needed (OBD scanning)
- Explain why Bluetooth permission is needed
- Don't auto-connect without user approval

### Safety Warnings
```kotlin
fun assessDrivingSafety(codes: List<DTCCodeInfo>): SafetyAssessment {
    val criticalCodes = codes.filter { it.severity == Severity.CRITICAL }

    return when {
        criticalCodes.isNotEmpty() -> SafetyAssessment(
            safeLevel = SafetyLevel.UNSAFE,
            message = "STOP DRIVING. Critical issue detected: ${criticalCodes.first().description}",
            color = Color.Red
        )
        codes.any { !it.should_drive } -> SafetyAssessment(
            safeLevel = SafetyLevel.CAUTION,
            message = "Drive cautiously to a repair shop. Do not take long trips.",
            color = Color.Orange
        )
        else -> SafetyAssessment(
            safeLevel = SafetyLevel.SAFE,
            message = "Safe to drive. Schedule repair within ${codes.minOf { it.urgency_days ?: 30 }} days.",
            color = Color.Green
        )
    }
}
```

### Disclaimers
```
⚠️ Disclaimer:
AVA provides diagnostic information for educational purposes only.
Always consult a qualified mechanic for proper diagnosis and repair.
Do not rely solely on this app for safety-critical decisions.
```

---

## Estimated Costs

### Development
| Task | Time | Cost @ $150/hr |
|------|------|----------------|
| OBD-II Bluetooth communication | 2 weeks | $12,000 |
| DTC database curation | 1 week | $6,000 |
| Diagnostic assistant | 1 week | $6,000 |
| UI & voice integration | 4 days | $4,800 |
| Testing & QA | 3 days | $3,600 |
| **Total** | **~4 weeks** | **$32,400** |

### Hardware
| Item | Quantity | Unit Cost | Total |
|------|----------|-----------|-------|
| OBD-II adapters (testing) | 5 | $50 | $250 |
| Test vehicles (rental/access) | 3 | $100/day × 10 days | $3,000 |
| **Total** | - | - | **$3,250** |

### Data
| Item | Cost |
|------|------|
| DTC code database license | Free (open standards) |
| Manual curation (contractor) | $2,000 |
| **Total** | **$2,000** |

**Grand Total:** ~$37,650

---

## Success Metrics

### Technical
- [ ] Connection success rate: >90%
- [ ] Code reading accuracy: 100%
- [ ] Average read time: <5 seconds
- [ ] Supported vehicles: >95% (1996+)
- [ ] DTC database coverage: >99% common codes

### User
- [ ] User satisfaction: 4.5+ stars
- [ ] Feature usage: >40% of users try it
- [ ] Repeat usage: >60% use weekly
- [ ] Support tickets: <5% encounter issues

### Business
- [ ] Conversion driver: 20% cite as reason for download
- [ ] Premium tier: 30% upgrade for advanced features
- [ ] Reviews mention: Featured in 50%+ positive reviews

---

## Risks & Mitigations

### Risk 1: Adapter Compatibility
**Risk:** Hundreds of OBD-II adapter models, not all compatible
**Mitigation:**
- Test with top 10 most popular adapters
- Maintain compatibility database
- Allow manual protocol selection
- Clear error messages

### Risk 2: Vehicle Compatibility
**Risk:** Some vehicles use proprietary protocols
**Mitigation:**
- Focus on 1996+ vehicles (OBD-II standard)
- Graceful degradation for unsupported vehicles
- Community-driven vehicle database

### Risk 3: Liability
**Risk:** User makes wrong decision based on diagnosis
**Mitigation:**
- Clear disclaimers
- Always recommend professional verification
- Conservative safety assessments
- Don't provide code clearing without warning

### Risk 4: Technical Complexity
**Risk:** OBD-II protocol can be complex
**Mitigation:**
- Start with basic Mode 03/04 (read/clear codes)
- Add advanced features incrementally
- Extensive testing before release

---

## Future Enhancements (Post-Launch)

### Phase 2 Features
- [ ] Live data monitoring (RPM, speed, temp, etc.)
- [ ] Data logging & trend analysis
- [ ] Custom dashboards
- [ ] Freeze frame analysis
- [ ] Multiple vehicle profiles

### Phase 3 Features
- [ ] Predictive maintenance
- [ ] Fuel economy tracking
- [ ] Performance monitoring
- [ ] Emissions testing prep

### Phase 4 Features
- [ ] Cloud sync (multi-device)
- [ ] Fleet management (B2B)
- [ ] Mechanic integration (send reports)
- [ ] Parts lookup & ordering

---

## Launch Checklist

### Pre-Development
- [ ] Acquire OBD-II test hardware
- [ ] Get access to test vehicles
- [ ] Review SAE J2012 standard
- [ ] Create DTC database structure

### Development
- [ ] Implement OBDIIManager
- [ ] Create DTC database
- [ ] Build DiagnosticAssistant
- [ ] Design UI screens
- [ ] Add voice commands

### Testing
- [ ] Unit tests (100% coverage)
- [ ] Integration tests
- [ ] Hardware tests (5+ adapters)
- [ ] Vehicle tests (10+ models)
- [ ] User acceptance testing

### Launch
- [ ] Update app documentation
- [ ] Create user guide
- [ ] Add adapter compatibility list
- [ ] Marketing materials
- [ ] Play Store assets

---

## Documentation

### User Documentation
- [ ] OBD-II adapter setup guide
- [ ] Pairing instructions
- [ ] Troubleshooting guide
- [ ] Supported vehicles list
- [ ] FAQ

### Developer Documentation
- [ ] OBD-II protocol reference
- [ ] DTC database schema
- [ ] API documentation
- [ ] Testing guide

---

## Related Documents

- [JARVIS Vision Part 4: System Integration](../JARVIS-Vision-Part4-Integration.md)
- [RAG Quick Start Guide](../RAG-Quick-Start-Guide.md)
- [Developer Manual Chapter 28](../Developer-Manual-Chapter28-RAG.md)

---

**Status:** READY FOR IMPLEMENTATION (when prioritized)
**Last Updated:** 2025-11-05
**Estimated Start:** TBD
**Estimated Completion:** Start + 4 weeks
