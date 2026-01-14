# AVA JARVIS Vision - Part 4: System Integration & Automotive Features

**Date:** 2025-11-05
**Status:** Planning Phase
**Priority:** HIGH

---

## 1. Android System Control

### Overview
Deep integration with Android OS for seamless task execution.

### Implementation
```kotlin
class SystemController(
    private val context: Context
) {
    suspend fun executeCommand(command: NaturalCommand): Result<Unit> {
        return try {
            when (command.intent) {
                Intent.SET_REMINDER -> setReminder(command)
                Intent.MAKE_CALL -> makePhoneCall(command)
                Intent.SEND_MESSAGE -> sendMessage(command)
                Intent.NAVIGATE -> openNavigation(command)
                Intent.TAKE_PHOTO -> openCamera(command)
                Intent.SHARE_CONTENT -> shareContent(command)
                Intent.PLAY_MEDIA -> playMedia(command)
                Intent.SET_ALARM -> setAlarm(command)
                else -> Result.failure(UnknownIntentException(command.intent))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun setReminder(command: NaturalCommand) {
        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, command.parameters["title"])
            putExtra(CalendarContract.Events.DESCRIPTION, command.parameters["description"])
            putExtra(
                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                command.parameters["timestamp"]?.toLong()
            )
        }
        context.startActivity(calendarIntent)
    }

    private suspend fun makePhoneCall(command: NaturalCommand) {
        val phoneNumber = command.parameters["phone_number"]
        val callIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(callIntent)
    }

    private suspend fun openNavigation(command: NaturalCommand) {
        val destination = command.parameters["destination"]
        val navIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=$destination")
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(navIntent)
    }

    private suspend fun shareContent(command: NaturalCommand) {
        val content = command.parameters["content"]
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
}
```

### Natural Language to Intent
```kotlin
class IntentParser(
    private val llm: MLCEngine
) {
    suspend fun parseIntent(userInput: String): NaturalCommand {
        val prompt = """
        Parse this user command into a structured intent:
        "$userInput"

        Output JSON format:
        {
            "intent": "SET_REMINDER|MAKE_CALL|NAVIGATE|etc",
            "parameters": {
                "key": "value"
            },
            "confidence": 0.95
        }
        """

        val response = llm.generate(prompt)
        return Json.decodeFromString(response)
    }
}

// Examples:
// "Remind me to change oil in 3000 miles"
// → Intent.SET_REMINDER, {title: "Change oil", distance: 3000}

// "Call my mechanic"
// → Intent.MAKE_CALL, {contact: "mechanic"}

// "Navigate to nearest auto parts store"
// → Intent.NAVIGATE, {query: "auto parts store", mode: "nearest"}
```

**Priority:** 🟡 HIGH

---

## 2. OBD-II Diagnostic Integration

### Overview
Connect to vehicle's OBD-II port via Bluetooth adapter to read diagnostic codes.

### Implementation
```kotlin
class OBDIIManager(
    private val bluetoothAdapter: BluetoothAdapter
) {
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(OBD_UUID)
            socket?.connect()

            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            // Initialize OBD-II connection
            sendCommand("ATZ") // Reset
            sendCommand("ATE0") // Echo off
            sendCommand("ATL0") // Linefeeds off
            sendCommand("ATSP0") // Auto protocol

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readDiagnosticCodes(): Result<List<DTCCode>> = withContext(Dispatchers.IO) {
        try {
            val response = sendCommand("03") // Request DTCs
            val codes = parseDTCResponse(response)
            Result.success(codes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearDiagnosticCodes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sendCommand("04") // Clear DTCs
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readRealTimeData(pid: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = sendCommand("01$pid")
            Result.success(parseOBDResponse(response))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendCommand(command: String): String {
        outputStream?.write("$command\r".toByteArray())
        return readResponse()
    }

    private suspend fun readResponse(): String {
        val buffer = ByteArray(1024)
        val bytesRead = inputStream?.read(buffer) ?: 0
        return String(buffer, 0, bytesRead).trim()
    }

    private fun parseDTCResponse(response: String): List<DTCCode> {
        // Parse hex response into DTC codes
        // Example: "43 01 33" → "P0133"
        val codes = mutableListOf<DTCCode>()

        val parts = response.split(" ")
        for (i in 1 until parts.size step 2) {
            if (i + 1 < parts.size) {
                val byte1 = parts[i].toInt(16)
                val byte2 = parts[i + 1].toInt(16)

                val prefix = when (byte1 shr 6) {
                    0 -> "P0" // Powertrain
                    1 -> "P1"
                    2 -> "C" // Chassis
                    3 -> "B" // Body
                    else -> "U" // Network
                }

                val code = "$prefix${"%02X".format(byte2)}"
                codes.add(DTCCode(code))
            }
        }

        return codes
    }

    companion object {
        private val OBD_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
```

### Diagnostic Code Database
```kotlin
@Entity(tableName = "dtc_codes")
data class DTCCodeEntity(
    @PrimaryKey val code: String,     // "P0420"
    val description: String,           // "Catalyst System Efficiency Below Threshold"
    val severity: Severity,            // WARNING, CRITICAL, INFO
    val common_causes: String,         // JSON array
    val repair_steps: String,          // JSON array
    val average_repair_cost: Int?
)

class DTCDatabase {
    // Pre-populated database of ~5000 common codes
    suspend fun lookupCode(code: String): DTCCodeInfo? {
        return dtcDao.getCode(code)
    }

    suspend fun searchBySymptom(symptom: String): List<DTCCodeInfo> {
        return dtcDao.searchByDescription("%$symptom%")
    }
}
```

### Integration with RAG
```kotlin
class DiagnosticAssistant(
    private val obdManager: OBDIIManager,
    private val dtcDatabase: DTCDatabase,
    private val ragRepository: RAGRepository
) {
    suspend fun diagnoseIssue(): DiagnosticReport {
        // 1. Read codes from vehicle
        val codes = obdManager.readDiagnosticCodes().getOrThrow()

        // 2. Look up code descriptions
        val codeInfos = codes.mapNotNull { dtcDatabase.lookupCode(it.code) }

        // 3. Search manual for relevant information
        val manualSections = codeInfos.flatMap { info ->
            ragRepository.search(
                SearchQuery(
                    query = "${info.code} ${info.description}",
                    maxResults = 3
                )
            ).getOrNull()?.results ?: emptyList()
        }

        // 4. Generate comprehensive report
        return DiagnosticReport(
            codes = codeInfos,
            manualReferences = manualSections,
            recommendedActions = generateRecommendations(codeInfos),
            estimatedCost = codeInfos.sumOf { it.average_repair_cost ?: 0 }
        )
    }

    suspend fun explainToUser(report: DiagnosticReport): String {
        return llm.generate("""
        The vehicle has these diagnostic codes: ${report.codes.joinToString { it.code }}

        Descriptions:
        ${report.codes.joinToString("\n") { "${it.code}: ${it.description}" }}

        Manual says:
        ${report.manualReferences.joinToString("\n\n") { it.chunk.content }}

        Explain this to the user in plain English, include:
        - What's wrong
        - How serious it is
        - What they should do
        - Estimated cost: $${report.estimatedCost}
        """)
    }
}
```

### Voice-Activated Diagnostics
```
User: "Hey AVA, check my engine"
AVA: "Connecting to your vehicle's diagnostic port..."
     [Connects via Bluetooth OBD-II]
AVA: "I found one code: P0420 - Catalyst System Efficiency Below Threshold.
     This means your catalytic converter isn't working as efficiently as it should.
     It's not urgent, but you should get it checked within the next few weeks.
     According to your manual, this is covered on page 342.
     Average repair cost is around $1,200.
     Want me to find nearby shops?"
```

**Priority:** 🔴 CRITICAL for automotive use case

---

## 3. Android Auto Integration

### Overview
Safe, driver-friendly interface for use while driving.

### Implementation
```kotlin
class AVACarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return AVACarSession()
    }
}

class AVACarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return AVACarScreen(carContext)
    }
}

class AVACarScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return GridTemplate.Builder()
            .setTitle("AVA Assistant")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(
                ItemList.Builder()
                    .addItem(
                        GridItem.Builder()
                            .setTitle("Ask AVA")
                            .setImage(CarIcon.ALERT)
                            .setOnClickListener {
                                startVoiceInput()
                            }
                            .build()
                    )
                    .addItem(
                        GridItem.Builder()
                            .setTitle("Check Codes")
                            .setImage(CarIcon.ERROR)
                            .setOnClickListener {
                                checkDiagnosticCodes()
                            }
                            .build()
                    )
                    .addItem(
                        GridItem.Builder()
                            .setTitle("Maintenance")
                            .setImage(CarIcon.ALERT)
                            .setOnClickListener {
                                showMaintenanceSchedule()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun startVoiceInput() {
        // Trigger voice interaction
        screenManager.push(VoiceInteractionScreen(carContext))
    }

    private fun checkDiagnosticCodes() {
        // Show diagnostic codes
        screenManager.push(DiagnosticScreen(carContext))
    }
}

class VoiceInteractionScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("Listening...")
            .setIcon(CarIcon.ALERT)
            .setHeaderAction(Action.BACK)
            .addAction(
                Action.Builder()
                    .setTitle("Cancel")
                    .setOnClickListener { screenManager.pop() }
                    .build()
            )
            .build()
    }
}
```

### Safety Features
```kotlin
class DrivingSafetyManager(
    private val carContext: CarContext
) {
    fun isParked(): Boolean {
        return carContext.getCarService(CarSensors::class.java)
            .getCarSpeed()
            .value == 0f
    }

    fun shouldAllowAction(action: Action): Boolean {
        return when (action) {
            Action.VOICE_INPUT -> true // Always safe
            Action.READ_DOCUMENT -> isParked() // Only when parked
            Action.VISUAL_SEARCH -> isParked()
            Action.COMPLEX_INTERACTION -> isParked()
            else -> true
        }
    }

    fun simplifyResponse(fullResponse: String): String {
        // Shorten responses for driving
        // "According to page 234..." → "Oil change: every 5000 miles"
        return fullResponse.split(".").first() + "."
    }
}
```

**Priority:** 🟡 HIGH

---

## 4. Visual Understanding (Camera Integration)

### Overview
Use camera to identify parts, read codes, understand warning lights.

### Implementation
```kotlin
class VisualAssistant(
    private val clipModel: CLIPModel,
    private val ocrEngine: MLKit.TextRecognition
) {
    suspend fun analyzeImage(bitmap: Bitmap): VisualAnalysis {
        // 1. Run OCR for text
        val text = ocrEngine.process(bitmap).getOrNull()

        // 2. Run CLIP for semantic understanding
        val semanticTags = clipModel.classify(bitmap, AUTOMOTIVE_LABELS)

        // 3. Search manual for relevant info
        val manualResults = if (text != null) {
            ragRepository.search(SearchQuery(query = text.joinToString(" ")))
        } else null

        return VisualAnalysis(
            text = text,
            semanticTags = semanticTags,
            manualReferences = manualResults
        )
    }

    companion object {
        val AUTOMOTIVE_LABELS = listOf(
            "engine",
            "brake pad",
            "oil filter",
            "air filter",
            "battery",
            "spark plug",
            "alternator",
            "serpentine belt",
            "dashboard warning light",
            "check engine light",
            // ... hundreds more
        )
    }
}
```

### Use Cases
```kotlin
// Use Case 1: Identify Part
// User: *Takes photo of engine component*
// AVA: "That's your air filter. According to your manual, it should be
//       replaced every 30,000 miles. You're at 42,000 now."

// Use Case 2: Read Warning Light
// User: *Takes photo of dashboard*
// AVA: "I see your check engine light is on. Let me read the code..."
//      [Connects to OBD-II]
//      "Code P0420 - catalytic converter issue."

// Use Case 3: Scan VIN
// User: *Takes photo of VIN plate*
// AVA: "VIN detected: 1HGBH41JXMN109186
//       This is a 2015 Honda Civic EX. Want me to download the manual?"

// Use Case 4: Read Part Number
// User: *Takes photo of part label*
// AVA: "Part number: 15400-RTA-003
//       That's the oil filter for your Civic. It's $8 on Amazon. Want the link?"
```

### Camera UI
```kotlin
@Composable
fun CameraAnalysisScreen() {
    val cameraController = rememberCameraController()
    val viewModel = viewModel<VisualAssistantViewModel>()

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            controller = cameraController,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay: Capture button
        FloatingActionButton(
            onClick = {
                cameraController.takePicture { bitmap ->
                    viewModel.analyzeImage(bitmap)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            Icon(Icons.Default.Camera, "Capture")
        }

        // Overlay: Analysis result
        val analysis by viewModel.analysis.collectAsState()
        if (analysis != null) {
            AnalysisOverlay(analysis = analysis!!)
        }
    }
}
```

**Priority:** 🟡 HIGH

---

## 5. Wear OS Companion

### Overview
Quick access to AVA from smartwatch.

### Implementation
```kotlin
// Wear OS Module
class AVAWearService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            "/ava/voice_input" -> {
                // Received voice input from watch
                val transcript = String(event.data)
                processQuery(transcript)
            }
        }
    }

    private fun processQuery(query: String) {
        // Process on phone, send result back to watch
        lifecycleScope.launch {
            val response = avaEngine.processQuery(query)
            sendToWatch("/ava/response", response.toByteArray())
        }
    }

    private fun sendToWatch(path: String, data: ByteArray) {
        Wearable.getMessageClient(this)
            .sendMessage(getConnectedNodeId(), path, data)
    }
}

@Composable
fun WearOSUI() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { startVoiceInput() },
            modifier = Modifier.size(80.dp),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Mic, "Ask AVA")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Tap to ask AVA")
    }
}
```

### Watch Face Complication
```kotlin
class AVAComplicationService : ComplicationProviderService() {
    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        // Show next maintenance task
        val nextTask = getNextMaintenanceTask()

        val complicationData = ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(nextTask.title).build(),
            contentDescription = PlainComplicationText.Builder(nextTask.description).build()
        )
        .setTapAction(openAVAIntent())
        .build()

        listener.onComplicationData(complicationData)
    }
}
```

**Priority:** 🟢 MEDIUM

---

## 6. Cloud Sync (Optional)

### Overview
Optional cloud backup and multi-device sync.

### Implementation
```kotlin
class CloudSyncManager(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun syncConversations() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        // Upload new conversations
        val localConversations = database.conversationDao().getUnsynced()
        localConversations.forEach { conversation ->
            firestore.collection("users/$userId/conversations")
                .document(conversation.id)
                .set(conversation)
        }

        // Download remote conversations
        val remoteConversations = firestore.collection("users/$userId/conversations")
            .get()
            .await()

        remoteConversations.documents.forEach { doc ->
            val conversation = doc.toObject(ConversationEntity::class.java)
            if (conversation != null) {
                database.conversationDao().insert(conversation)
            }
        }
    }

    suspend fun backupDocuments() {
        // Backup document library to cloud storage
        val documents = database.documentDao().getAll()
        documents.forEach { doc ->
            val file = File(doc.file_path)
            if (file.exists()) {
                uploadToCloudStorage(file, doc.id)
            }
        }
    }
}
```

**Privacy Controls:**
```kotlin
data class CloudSyncSettings(
    val enabled: Boolean = false,           // Opt-in only
    val syncConversations: Boolean = true,
    val syncDocuments: Boolean = false,     // Large files
    val syncFacts: Boolean = true,
    val encryptData: Boolean = true         // E2E encryption
)
```

**Priority:** 🟢 LOW (Nice to have, but conflicts with privacy-first approach)

---

## Implementation Timeline

### Week 1: System Integration
- [ ] Intent parsing
- [ ] Calendar/reminder integration
- [ ] Phone/contacts integration
- [ ] Navigation integration
- [ ] Share functionality

### Week 2-3: OBD-II Integration
- [ ] Bluetooth OBD-II library
- [ ] DTC code database
- [ ] Real-time data reading
- [ ] Integration with RAG
- [ ] Voice interface

### Week 4: Android Auto
- [ ] Car app service
- [ ] Voice interface
- [ ] Safety features
- [ ] Diagnostic display
- [ ] Testing in vehicle

### Week 5: Visual Understanding
- [ ] Camera integration
- [ ] OCR implementation
- [ ] CLIP model integration
- [ ] Part identification
- [ ] Warning light recognition

### Week 6: Wear OS (Optional)
- [ ] Wear module
- [ ] Voice input
- [ ] Watch face complication
- [ ] Testing

---

**Next:** Part 5 - Implementation Roadmap & Summary
