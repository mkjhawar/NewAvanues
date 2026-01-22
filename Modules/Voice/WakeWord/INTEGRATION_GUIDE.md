# Wake Word Integration Quick Start

**5-Minute Setup Guide for AVA AI Wake Word Detection**

---

## Step 1: Get Porcupine API Key (2 minutes)

1. Go to https://picovoice.ai/platform/porcupine/
2. Sign up for free account
3. Create new access key
4. Copy the access key

---

## Step 2: Set Environment Variable (1 minute)

### macOS/Linux

```bash
export AVA_PORCUPINE_API_KEY="your_access_key_here"

# Make it permanent
echo 'export AVA_PORCUPINE_API_KEY="your_access_key_here"' >> ~/.zshrc
source ~/.zshrc
```

### Windows

```cmd
setx AVA_PORCUPINE_API_KEY "your_access_key_here"
```

---

## Step 3: Add Permissions to MainActivity (1 minute)

```kotlin
// In onCreate()
if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.RECORD_AUDIO),
        1001
    )
}
```

---

## Step 4: Add Wake Word Detection (1 minute)

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val wakeWordViewModel: WakeWordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize wake word
        wakeWordViewModel.initialize { keyword ->
            Timber.i("Wake word detected: ${keyword.displayName}")
            // TODO: Start voice input
        }

        // Start listening
        wakeWordViewModel.start()
    }
}
```

---

## Step 5: Test It!

1. Build and run app: `./gradlew installDebug`
2. Grant microphone permission when prompted
3. Say **"Jarvis"** (built-in keyword for testing)
4. Check logs: `adb logcat | grep WakeWord`

You should see:
```
WakeWordDetector: Wake word detected: Jarvis (index: 0, count: 1)
```

---

## What Wake Word to Use?

For testing, use built-in keywords:
- **"Jarvis"** (recommended for testing "Hey AVA")
- **"Alexa"** (alternative)
- **"Computer"** (alternative)

Custom "Hey AVA" requires training through Porcupine Console (paid feature).

---

## Troubleshooting

### "Porcupine access key not found"
- Verify environment variable: `echo $AVA_PORCUPINE_API_KEY`
- Restart terminal/IDE after setting variable

### Wake word not detected
- Check microphone permission is granted
- Verify service is running (notification should appear)
- Try saying keyword louder/clearer
- Check logs for errors

### Build errors
- Sync Gradle: `./gradlew sync`
- Clean build: `./gradlew clean build`

---

## Full Documentation

See [`README.md`](./README.md) for complete API reference and advanced usage.

---

**You're ready!** 🎉

Say "Jarvis" to activate AVA AI hands-free.
