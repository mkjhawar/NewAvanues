# AVA JARVIS Vision - Part 3: UI/UX Excellence

**Date:** 2025-11-05
**Status:** Planning Phase
**Priority:** CRITICAL

---

## 1. Floating Assistant Bubble

### Overview
Persistent overlay bubble accessible from any app, inspired by Facebook Messenger chat heads.

### Implementation
```kotlin
class FloatingAssistantService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View

    override fun onCreate() {
        super.onCreate()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        floatingView = createFloatingView()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        setupDragBehavior(floatingView, params)
    }

    private fun createFloatingView(): View {
        return FrameLayout(this).apply {
            addView(ComposeView(this@FloatingAssistantService).apply {
                setContent {
                    FloatingBubbleUI()
                }
            })
        }
    }
}
```

### Bubble UI States
```kotlin
@Composable
fun FloatingBubbleUI() {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Cyan.copy(alpha = 0.3f),
                        Color.Blue.copy(alpha = 0.8f)
                    )
                )
            )
            .border(2.dp, Color.Cyan.copy(alpha = 0.5f), CircleShape)
            .clickable { viewModel.onBubbleTapped() }
    ) {
        when (state) {
            BubbleState.IDLE -> IdleAnimation()
            BubbleState.LISTENING -> ListeningAnimation()
            BubbleState.PROCESSING -> ProcessingAnimation()
            BubbleState.SPEAKING -> SpeakingAnimation()
            BubbleState.NOTIFICATION -> NotificationBadge()
        }
    }
}

@Composable
fun ListeningAnimation() {
    // Pulsing circular waveform
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            drawCircle(
                color = Color.Cyan,
                radius = size.minDimension / 2,
                alpha = 0.5f
            )
        }
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Listening",
            tint = Color.White
        )
    }
}

@Composable
fun ProcessingAnimation() {
    // Spinning particles
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(8) { i ->
                val angle = (rotation + i * 45f) * (Math.PI / 180f).toFloat()
                val x = center.x + cos(angle) * size.minDimension / 3
                val y = center.y + sin(angle) * size.minDimension / 3

                drawCircle(
                    color = Color.Cyan,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = "Thinking",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SpeakingAnimation(audioLevel: Float) {
    // Waveform bars matching speech amplitude
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val barCount = 5
            val barWidth = size.width / (barCount * 2)
            val centerX = size.width / 2

            repeat(barCount) { i ->
                val x = centerX + (i - barCount / 2) * barWidth * 2
                val amplitude = sin((audioLevel + i) * 0.5f).absoluteValue
                val height = size.height * amplitude * 0.8f

                drawRect(
                    color = Color.Cyan,
                    topLeft = Offset(x, (size.height - height) / 2),
                    size = Size(barWidth, height)
                )
            }
        }
    }
}
```

### Drag Behavior
```kotlin
private fun setupDragBehavior(view: View, params: WindowManager.LayoutParams) {
    var initialX = 0
    var initialY = 0
    var initialTouchX = 0f
    var initialTouchY = 0f

    view.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                true
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = initialX + (event.rawX - initialTouchX).toInt()
                params.y = initialY + (event.rawY - initialTouchY).toInt()
                windowManager.updateViewLayout(view, params)
                true
            }
            MotionEvent.ACTION_UP -> {
                // Snap to edge
                snapToEdge(params)
                v.performClick()
                true
            }
            else -> false
        }
    }
}

private fun snapToEdge(params: WindowManager.LayoutParams) {
    val displayMetrics = resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels

    // Snap to nearest edge
    params.x = if (params.x < screenWidth / 2) {
        0 // Left edge
    } else {
        screenWidth - floatingView.width // Right edge
    }

    windowManager.updateViewLayout(floatingView, params)
}
```

### Permissions
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>

<!-- Request in activity -->
if (!Settings.canDrawOverlays(this)) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName")
    )
    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
}
```

**Priority:** 🔴 CRITICAL

---

## 2. Cinematic JARVIS-Style Interface

### Overview
Iron Man inspired visual effects and animations.

### Full-Screen Interface
```kotlin
@Composable
fun JARVISInterface() {
    val viewModel = viewModel<AVAViewModel>()
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Background: Dark with subtle grid
        Background()

        // Center: Circular visualization
        CircularVisualization(
            state = state,
            audioLevel = viewModel.audioLevel.collectAsState().value
        )

        // Top: Status bar
        StatusBar(state = state)

        // Bottom: Transcript / Response
        TranscriptDisplay(
            transcript = state.transcript,
            response = state.response
        )

        // Floating: Quick actions
        QuickActionBar(
            suggestions = state.suggestions,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun Background() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color.Black)

        // Draw subtle grid
        val gridSize = 50.dp.toPx()
        for (x in 0 until size.width.toInt() step gridSize.toInt()) {
            drawLine(
                color = Color.Cyan.copy(alpha = 0.05f),
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
        for (y in 0 until size.height.toInt() step gridSize.toInt()) {
            drawLine(
                color = Color.Cyan.copy(alpha = 0.05f),
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun CircularVisualization(state: AVAState, audioLevel: Float) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            // Outer ring (always rotating)
            drawArc(
                color = Color.Cyan.copy(alpha = 0.3f),
                startAngle = rotation,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx()),
                topLeft = Offset(0f, 0f),
                size = size
            )

            // Middle ring (counter-rotating)
            drawArc(
                color = Color.Blue.copy(alpha = 0.3f),
                startAngle = -rotation * 0.7f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx()),
                topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
                size = Size(size.width * 0.8f, size.height * 0.8f)
            )

            // Inner core (pulsing based on audio)
            if (state is AVAState.Listening || state is AVAState.Speaking) {
                val pulseRadius = radius * 0.3f * (1f + audioLevel * 0.5f)
                drawCircle(
                    color = Color.Cyan.copy(alpha = 0.6f),
                    radius = pulseRadius,
                    center = center
                )
            }

            // Particles (only when processing)
            if (state is AVAState.Processing) {
                repeat(20) { i ->
                    val angle = (rotation + i * 18f) * (Math.PI / 180f).toFloat()
                    val distance = radius * 0.7f + sin(rotation * 0.1f + i) * 20f
                    val x = center.x + cos(angle) * distance
                    val y = center.y + sin(angle) * distance

                    drawCircle(
                        color = Color.Cyan.copy(alpha = 0.8f),
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Center icon
        Icon(
            imageVector = when (state) {
                is AVAState.Idle -> Icons.Default.Assistant
                is AVAState.Listening -> Icons.Default.Mic
                is AVAState.Processing -> Icons.Default.Psychology
                is AVAState.Speaking -> Icons.Default.RecordVoiceOver
            },
            contentDescription = null,
            tint = Color.Cyan,
            modifier = Modifier.size(48.dp)
        )
    }
}
```

### Particle System
```kotlin
class ParticleSystem {
    private val particles = mutableListOf<Particle>()

    data class Particle(
        var position: Offset,
        var velocity: Offset,
        var life: Float,
        var size: Float,
        val color: Color
    )

    fun emit(origin: Offset, count: Int = 10) {
        repeat(count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 5f + 2f

            particles.add(
                Particle(
                    position = origin,
                    velocity = Offset(
                        cos(angle) * speed,
                        sin(angle) * speed
                    ),
                    life = 1f,
                    size = Random.nextFloat() * 4f + 2f,
                    color = Color.Cyan
                )
            )
        }
    }

    fun update(deltaTime: Float) {
        particles.forEach { particle ->
            particle.position += particle.velocity * deltaTime
            particle.life -= deltaTime * 0.5f
            particle.velocity *= 0.98f // Friction
        }

        particles.removeAll { it.life <= 0f }
    }

    fun draw(drawScope: DrawScope) {
        particles.forEach { particle ->
            drawScope.drawCircle(
                color = particle.color.copy(alpha = particle.life),
                radius = particle.size,
                center = particle.position
            )
        }
    }
}
```

**Priority:** 🟢 MEDIUM

---

## 3. Contextual Quick Actions

### Overview
Show relevant action chips based on conversation context.

### Implementation
```kotlin
@Composable
fun QuickActionBar(
    suggestions: List<QuickAction>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { action ->
            QuickActionChip(action = action)
        }
    }
}

@Composable
fun QuickActionChip(action: QuickAction) {
    SuggestionChip(
        onClick = { action.execute() },
        label = { Text(action.title) },
        icon = {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color.Cyan.copy(alpha = 0.1f),
            labelColor = Color.Cyan,
            iconContentColor = Color.Cyan
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            borderColor = Color.Cyan.copy(alpha = 0.3f)
        )
    )
}
```

### Context-Aware Suggestions
```kotlin
class QuickActionGenerator(
    private val conversationState: ConversationState
) {
    fun generateActions(): List<QuickAction> {
        val actions = mutableListOf<QuickAction>()

        when (conversationState.lastTopic) {
            Topic.DOCUMENT_REFERENCE -> {
                actions.add(
                    QuickAction(
                        title = "Open PDF",
                        icon = Icons.Default.PictureAsPdf,
                        action = { openDocument(conversationState.lastDocument) }
                    )
                )
                actions.add(
                    QuickAction(
                        title = "Read Aloud",
                        icon = Icons.Default.RecordVoiceOver,
                        action = { readAloud(conversationState.lastDocument) }
                    )
                )
                actions.add(
                    QuickAction(
                        title = "Bookmark",
                        icon = Icons.Default.Bookmark,
                        action = { bookmarkPage(conversationState.lastPage) }
                    )
                )
            }

            Topic.MAINTENANCE_SCHEDULE -> {
                actions.add(
                    QuickAction(
                        title = "Set Reminder",
                        icon = Icons.Default.Alarm,
                        action = { setReminder() }
                    )
                )
                actions.add(
                    QuickAction(
                        title = "Find Shop",
                        icon = Icons.Default.Place,
                        action = { findNearbyShops() }
                    )
                )
            }

            Topic.DIAGNOSTIC_CODE -> {
                actions.add(
                    QuickAction(
                        title = "Read Code",
                        icon = Icons.Default.Cable,
                        action = { connectOBD() }
                    )
                )
                actions.add(
                    QuickAction(
                        title = "Clear Code",
                        icon = Icons.Default.Clear,
                        action = { clearDiagnosticCode() }
                    )
                )
            }

            Topic.PART_IDENTIFICATION -> {
                actions.add(
                    QuickAction(
                        title = "Search Part",
                        icon = Icons.Default.Search,
                        action = { searchOnlineForPart() }
                    )
                )
                actions.add(
                    QuickAction(
                        title = "Take Photo",
                        icon = Icons.Default.Camera,
                        action = { openCamera() }
                    )
                )
            }
        }

        // Always available actions
        actions.add(
            QuickAction(
                title = "Share",
                icon = Icons.Default.Share,
                action = { shareConversation() }
            )
        )

        return actions
    }
}
```

**Priority:** 🟡 HIGH

---

## 4. Picture-in-Picture Mode

### Overview
Continue conversation while using other apps.

### Implementation
```kotlin
class AVAActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): PictureInPictureParams {
        val aspectRatio = Rational(16, 9)

        return PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(getPipActions())
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getPipActions(): List<RemoteAction> {
        return listOf(
            RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_mic),
                "Listen",
                "Start listening",
                PendingIntent.getBroadcast(
                    this,
                    REQUEST_START_LISTENING,
                    Intent(ACTION_START_LISTENING),
                    PendingIntent.FLAG_IMMUTABLE
                )
            ),
            RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_stop),
                "Stop",
                "Stop speaking",
                PendingIntent.getBroadcast(
                    this,
                    REQUEST_STOP_SPEAKING,
                    Intent(ACTION_STOP_SPEAKING),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        )
    }

    override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (viewModel.isConversationActive) {
                enterPictureInPictureMode(buildPipParams())
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        if (isInPictureInPictureMode) {
            // Hide unnecessary UI elements
            viewModel.setPipMode(true)
        } else {
            // Restore full UI
            viewModel.setPipMode(false)
        }
    }
}

@Composable
fun PipUI(state: AVAState) {
    // Minimal UI for PiP mode
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is AVAState.Listening -> {
                CircularWaveform(audioLevel = state.audioLevel)
                Text(
                    text = "Listening...",
                    color = Color.Cyan,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            is AVAState.Speaking -> {
                AnimatedWaveformBars(audioLevel = state.audioLevel)
                Text(
                    text = state.currentSentence,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    maxLines = 2
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Assistant,
                    contentDescription = null,
                    tint = Color.Cyan,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
```

**Priority:** 🟡 HIGH

---

## 5. Haptic Feedback

### Overview
Tactile feedback for key interactions.

### Implementation
```kotlin
class HapticFeedbackManager(private val context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun trigger(effect: HapticEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = when (effect) {
                HapticEffect.LIGHT_TAP ->
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticEffect.MEDIUM_TAP ->
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticEffect.HEAVY_TAP ->
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                HapticEffect.WAKE_WORD_DETECTED ->
                    VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1)
                HapticEffect.ERROR ->
                    VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1)
            }
            vibrator.vibrate(vibrationEffect)
        }
    }
}

enum class HapticEffect {
    LIGHT_TAP,
    MEDIUM_TAP,
    HEAVY_TAP,
    WAKE_WORD_DETECTED,
    ERROR
}

// Usage
modifier.clickable {
    haptics.trigger(HapticEffect.LIGHT_TAP)
    onClick()
}
```

**Priority:** 🟢 MEDIUM

---

## Implementation Timeline

### Week 1: Floating Bubble
- [ ] Implement overlay service
- [ ] Create bubble UI
- [ ] Add drag behavior
- [ ] Animate states
- [ ] Test permissions

### Week 2: JARVIS Interface
- [ ] Design circular visualization
- [ ] Implement particle system
- [ ] Add background effects
- [ ] Polish animations
- [ ] Performance optimization

### Week 3: Quick Actions & PiP
- [ ] Context-aware action generation
- [ ] Quick action UI
- [ ] Picture-in-picture mode
- [ ] PiP controls
- [ ] Testing

### Week 4: Polish
- [ ] Haptic feedback
- [ ] Sound effects
- [ ] Transitions
- [ ] Accessibility
- [ ] User testing

---

**Next:** Part 4 - System Integration
