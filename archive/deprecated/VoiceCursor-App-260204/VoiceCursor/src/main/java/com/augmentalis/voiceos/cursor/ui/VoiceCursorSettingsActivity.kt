/**
 * SettingsActivity.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/ui/SettingsActivity.kt
 *
 * Created: 2025-01-26 02:15 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: ARVision-themed settings activity for VoiceCursor configuration
 * Module: VoiceCursor System
 *
 * Changelog:
 * - v1.0.0 (2025-01-26 02:15 PST): Initial creation with Compose UI and ARVision theming
 */

package com.augmentalis.voiceos.cursor.ui

// import com.augmentalis.voiceuielements.themes.arvision.glassMorphism
// import com.augmentalis.voiceuielements.themes.arvision.GlassMorphismConfig
// import com.augmentalis.voiceuielements.themes.arvision.DepthLevel

// Import theme utils for validation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.augmentalis.devicemanager.DeviceManager
/*import com.augmentalis.licensemanager.ui.DepthLevel
import com.augmentalis.licensemanager.ui.GlassMorphismConfig
import com.augmentalis.licensemanager.ui.glassMorphism*/
import com.augmentalis.voiceos.cursor.R
import com.augmentalis.voiceos.cursor.VoiceCursor
import com.augmentalis.voiceos.cursor.core.CursorConfig
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.core.CursorType
import com.augmentalis.voiceos.cursor.core.FilterStrength
import com.augmentalis.voiceos.cursor.filter.CursorFilter
import com.augmentalis.voiceos.cursor.VoiceCursorAPI
import kotlinx.coroutines.launch

/**
 * VoiceCursor settings activity with ARVision theming
 * Provides comprehensive cursor configuration with real-time preview
 */
class VoiceCursorSettingsActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "voice_cursor_prefs"
        private const val TAG = "VoiceCursorSettings"
    }

    private lateinit var preferences: SharedPreferences
    private lateinit var voiceCursor: VoiceCursor

    private val viewModel: VoiceCursorViewModel by viewModels()

    // Activity result launchers
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.refresh(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize preferences and VoiceCursor
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        voiceCursor = VoiceCursor.getInstance(this)

        setContent {
            val uiState by viewModel.ui.collectAsStateWithLifecycle()
            VoiceCursorSettingsScreen(
                preferences = preferences,
                voiceCursor = voiceCursor,
                overlayGranted = uiState.overlayGranted,
                accessibilityGranted = uiState.accessibilityGranted,
                onRequestOverlayPermission = { requestOverlayPermission() },
                onRequestAccessibilityPermission = { requestAccessibilityPermission() }
            )
        }
        val deviceManager = DeviceManager.getInstance(this)
        deviceManager.initialize()
        Log.i(TAG, "initializeCoreModules: ${deviceManager.imu.getSensorCapabilities()}")
    }

    /**
     * Broadcast receiver for accessibility service state changes
     */
    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.augmentalis.voiceos.cursor.ACCESSIBILITY_READY" -> {
                    Log.d(TAG, "Accessibility service ready, refreshing UI")
                    viewModel.refresh(this@VoiceCursorSettingsActivity)
                }

                "android.settings.action.MANAGE_OVERLAY_PERMISSION" -> {
                    Log.d(TAG, "Overlay permission changed, refreshing UI")
                    viewModel.refresh(this@VoiceCursorSettingsActivity)
                }
            }
        }
    }

    /**
     * Handle activity resume to refresh permission states
     */
    override fun onResume() {
        super.onResume()
        // Register broadcast receiver for permission changes
        val filter = IntentFilter().apply {
            addAction("com.augmentalis.voiceos.cursor.ACCESSIBILITY_READY")
            addAction("android.settings.action.MANAGE_OVERLAY_PERMISSION")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(permissionReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(permissionReceiver, filter)
        }
        // Force UI refresh when returning from permission settings
        viewModel.refresh(this)
    }

    /**
     * Handle activity pause to unregister receiver
     */
    override fun onPause() {
        super.onPause()
        // Unregister broadcast receiver
        try {
            unregisterReceiver(permissionReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
            Log.w(TAG, "Could not unregister receiver", e)
        }
    }

    /**
     * Request overlay permission
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }
    }

    /**
     * Request accessibility permission
     */
    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)

        Toast.makeText(
            this,
            getString(R.string.permission_accessibility_message),
            Toast.LENGTH_LONG
        ).show()
    }

}

/**
 * Main settings screen with ARVision theming
 */
@Composable
fun VoiceCursorSettingsScreen(
    preferences: SharedPreferences,
    voiceCursor: VoiceCursor,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    overlayGranted: Boolean,
    accessibilityGranted: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load current configuration
    var cursorConfig by remember { mutableStateOf(loadCursorConfig(preferences)) }
    var isCursorEnabled by remember { mutableStateOf(preferences.getBoolean("cursor_enabled", false)) }

    // Glass morphism configuration for panels
    val glassMorphismConfig = remember {
        GlassMorphismConfig(
            cornerRadius = 16.dp,
            backgroundOpacity = 0.9f,
            borderOpacity = 0.6f,
            borderWidth = 1.dp,
            tintColor = Color(0xFF007AFF),
            tintOpacity = 0.05f
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF2F2F7),
                        Color(0xFFFFFFFF)
                    )
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = stringResource(R.string.settings_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1D1D1F),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Enable/Disable Section
        SettingsPanel(
            title = "Cursor Control",
            glassMorphismConfig = glassMorphismConfig
        ) {
            // Enable cursor switch
            SettingsSwitchItem(
                title = stringResource(R.string.settings_enable),
                subtitle = if (isCursorEnabled) "Cursor is active" else "Cursor is disabled",
                isChecked = isCursorEnabled,
                isEnabled = overlayGranted && accessibilityGranted,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        scope.launch {
                            voiceCursor.initialize(cursorConfig)
                            if (VoiceCursorAPI.showCursor(cursorConfig)) {
                                isCursorEnabled = true
                                saveBooleanPreference(preferences, "cursor_enabled", true)
                            }
                        }
                    } else {
                        VoiceCursorAPI.hideCursor()
                        isCursorEnabled = false
                        saveBooleanPreference(preferences, "cursor_enabled", false)
                    }
                }
            )
        }

        // Permissions Section
        SettingsPanel(
            title = "Permissions",
            glassMorphismConfig = glassMorphismConfig
        ) {
            // Overlay permission
            PermissionItem(
                title = "Display Overlay",
                subtitle = if (overlayGranted) "Granted" else "Required for cursor display",
                isGranted = overlayGranted,
                onClick = onRequestOverlayPermission
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Accessibility permission
            PermissionItem(
                title = "Accessibility Service",
                subtitle = if (accessibilityGranted) "Granted" else "Required for touch gestures",
                isGranted = accessibilityGranted,
                onClick = onRequestAccessibilityPermission
            )
        }

        // Cursor Appearance Section
        SettingsPanel(
            title = "Appearance",
            glassMorphismConfig = glassMorphismConfig
        ) {
            // Cursor type
            SettingsDropdownItem(
                title = stringResource(R.string.settings_cursor_type),
                selectedValue = cursorConfig.type.name,
                options = listOf("Normal", "Hand", "Custom"),
                onValueChanged = { typeString ->
                    val newType = when (typeString) {
                        "Hand" -> CursorType.Hand
                        "Custom" -> CursorType.Custom
                        else -> CursorType.Normal
                    }
                    cursorConfig = cursorConfig.copy(type = newType)
                    saveCursorConfig(preferences, cursorConfig)
                    VoiceCursorAPI.updateConfiguration(cursorConfig)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cursor size
            SettingsSliderItem(
                title = stringResource(R.string.settings_cursor_size),
                value = cursorConfig.size.toFloat(),
                valueRange = 32f..80f,
                steps = 11,
                valueText = "${cursorConfig.size}dp",
                onValueChange = { size ->
                    cursorConfig = cursorConfig.copy(size = size.toInt())
                    saveCursorConfig(preferences, cursorConfig)
                    VoiceCursorAPI.updateConfiguration(cursorConfig)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cursor color
            ColorPickerItem(
                title = stringResource(R.string.settings_cursor_color),
                selectedColor = cursorConfig.color,
                onColorSelected = { color ->
                    cursorConfig = cursorConfig.copy(color = color)
                    saveCursorConfig(preferences, cursorConfig)
                    VoiceCursorAPI.updateConfiguration(cursorConfig)
                }
            )
        }

        // Movement Section
        SettingsPanel(
            title = "Movement",
            glassMorphismConfig = glassMorphismConfig
        ) {
            // Cursor speed
            SettingsSliderItem(
                title = stringResource(R.string.settings_cursor_speed),
                value = cursorConfig.speed.toFloat(),
                valueRange = 1f..20f,
                steps = 18,
                valueText = "${cursorConfig.speed}",
                onValueChange = { speed ->
                    cursorConfig = cursorConfig.copy(speed = speed.toInt())
                    saveCursorConfig(preferences, cursorConfig)
                    VoiceCursorAPI.updateConfiguration(cursorConfig)
                }
            )
        }

        // CursorFilter Section
        SettingsPanel(
            title = "Jitter Filtering",
            glassMorphismConfig = glassMorphismConfig
        ) {
            // Enable/Disable jitter filtering
            SettingsSwitchItem(
                title = "Enable Jitter Filtering",
                subtitle = "Reduce cursor shakiness during movement",
                isChecked = cursorConfig.jitterFilterEnabled,
                isEnabled = true,
                onCheckedChange = { enabled ->
                    cursorConfig = cursorConfig.copy(jitterFilterEnabled = enabled)
                    saveCursorConfig(preferences, cursorConfig)
                    VoiceCursorAPI.updateConfiguration(cursorConfig)
                }
            )

            if (cursorConfig.jitterFilterEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                // Filter strength
                SettingsDropdownItem(
                    title = "Filter Strength",
                    selectedValue = cursorConfig.filterStrength.name,
                    options = listOf("Low", "Medium", "High"),
                    onValueChanged = { strengthString ->
                        val newStrength = when (strengthString) {
                            "Low" -> FilterStrength.Low
                            "Medium" -> FilterStrength.Medium
                            "High" -> FilterStrength.High
                            else -> FilterStrength.Medium
                        }
                        cursorConfig = cursorConfig.copy(filterStrength = newStrength)
                        saveCursorConfig(preferences, cursorConfig)
                        VoiceCursorAPI.updateConfiguration(cursorConfig)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Motion sensitivity
                SettingsSliderItem(
                    title = "Motion Sensitivity",
                    value = cursorConfig.motionSensitivity,
                    valueRange = 0.1f..1.0f,
                    steps = 8,
                    valueText = "${(cursorConfig.motionSensitivity * 100).toInt()}%",
                    onValueChange = { sensitivity ->
                        cursorConfig = cursorConfig.copy(motionSensitivity = sensitivity)
                        saveCursorConfig(preferences, cursorConfig)
                        VoiceCursorAPI.updateConfiguration(cursorConfig)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filter test area
                CursorFilterTestArea(
                    cursorConfig = cursorConfig
                )
            }
        }

        // Gaze Click Section
        SettingsPanel(
            title = "Gaze Click",
            glassMorphismConfig = glassMorphismConfig
        ) {
            // Gaze click toggle
            var isGazeEnabled by remember {
                mutableStateOf(preferences.getBoolean("gaze_enabled", false))
            }

            SettingsSwitchItem(
                title = "Enable Gaze Click",
                subtitle = "Auto-click when looking at the same position for 1.5 seconds",
                isChecked = isGazeEnabled,
                isEnabled = isCursorEnabled,
                onCheckedChange = { enabled ->
                    isGazeEnabled = enabled
                    saveBooleanPreference(preferences, "gaze_enabled", enabled)

                    // Update gaze state in cursor view
                    if (isCursorEnabled) {
                        // Update cursor configuration with new gaze setting
                        val currentConfig = VoiceCursor.getInstance(context).getConfig()
                        val newConfig = currentConfig.copy(
                            gazeClickDelay = if (enabled) {
                                preferences.getFloat("gaze_delay", 1.5f).toLong() * 1000
                            } else {
                                0L
                            }
                        )
                        VoiceCursorAPI.updateConfiguration(newConfig)
                    }
                }
            )

            // Gaze delay slider (only show when gaze is enabled)
            if (isGazeEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                var gazeDelay by remember {
                    mutableStateOf(preferences.getFloat("gaze_delay", 1.5f))
                }

                SettingsSliderItem(
                    title = "Gaze Delay",
                    value = gazeDelay,
                    valueRange = 0.5f..3.0f,
                    steps = 4,
                    valueText = "%.1f seconds".format(gazeDelay),
                    onValueChange = { delay ->
                        gazeDelay = delay
                        preferences.edit().putFloat("gaze_delay", delay).apply()

                        // Update gaze delay in cursor configuration
                        if (isCursorEnabled) {
                            val currentConfig = VoiceCursor.getInstance(context).getConfig()
                            val newConfig = currentConfig.copy(
                                gazeClickDelay = (delay * 1000).toLong()
                            )
                            VoiceCursorAPI.updateConfiguration(newConfig)
                        }
                    }
                )
            }
        }

        // Coordinate Display Section
        SettingsPanel(
            title = "Coordinate Display",
            glassMorphismConfig = glassMorphismConfig
        ) {
            var showCoordinates by remember {
                mutableStateOf(preferences.getBoolean("show_coordinates", false))
            }

            SettingsSwitchItem(
                title = "Show Coordinates",
                subtitle = "Display cursor position coordinates on screen",
                isChecked = showCoordinates,
                isEnabled = isCursorEnabled,
                onCheckedChange = { enabled ->
                    showCoordinates = enabled
                    saveBooleanPreference(preferences, "show_coordinates", enabled)

                    // Update coordinate display in cursor configuration
                    if (isCursorEnabled) {
                        val currentConfig = VoiceCursor.getInstance(context).getConfig()
                        val newConfig = currentConfig.copy(showCoordinates = enabled)
                        VoiceCursorAPI.updateConfiguration(newConfig)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Settings panel with glass morphism background
 */
@Composable
fun SettingsPanel(
    title: String,
    glassMorphismConfig: GlassMorphismConfig,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = glassMorphismConfig,
                /*depth = DepthLevel(1.0f)*/
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D1D1F)
        )

        content()
    }
}

/**
 * Switch setting item
 */
@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    isEnabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isEnabled) Color(0xFF1D1D1F) else Color(0xFF8E8E93)
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color(0xFF8E8E93)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = isEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF007AFF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E5EA)
            )
        )
    }
}

/**
 * Permission status item
 */
@Composable
fun PermissionItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1D1D1F)
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = if (isGranted) Color(0xFF34C759) else Color(0xFFFF3B30)
            )
        }

        // Status indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (isGranted) Color(0xFF34C759) else Color(0xFFFF3B30),
                    shape = RoundedCornerShape(6.dp)
                )
        )
    }
}

/**
 * Dropdown setting item
 */
@Composable
fun SettingsDropdownItem(
    title: String,
    selectedValue: String,
    options: List<String>,
    onValueChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1D1D1F)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x1A007AFF)
                )
            ) {
                Text(
                    text = selectedValue,
                    color = Color(0xFF007AFF),
                    modifier = Modifier.weight(1f)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChanged(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Slider setting item
 */
@Composable
fun SettingsSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1D1D1F)
            )

            Text(
                text = valueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF007AFF)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF007AFF),
                activeTrackColor = Color(0xFF007AFF),
                inactiveTrackColor = Color(0xFFE5E5EA)
            )
        )
    }
}

/**
 * Color picker item
 */
@Composable
fun ColorPickerItem(
    title: String,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    val colors = listOf(
        0xFF007AFF to "Blue",
        0xFF30B0C7 to "Teal",
        0xFFAF52DE to "Purple",
        0xFF34C759 to "Green",
        0xFFFF3B30 to "Red",
        0xFFFF9500 to "Orange"
    )

    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1D1D1F)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.forEach { (color, _) ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(color),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onColorSelected(color.toInt()) }
                ) {
                    if (selectedColor == color.toInt()) {
                        // Selection indicator
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Test area for cursor filter preview
 */
@Composable
fun CursorFilterTestArea(
    cursorConfig: CursorConfig
) {
    val filter = remember { CursorFilter() }
    var rawPosition by remember { mutableStateOf(CursorOffset(0f, 0f)) }
    var filteredPosition by remember { mutableStateOf(CursorOffset(0f, 0f)) }
    var motionLevel by remember { mutableStateOf(0f) }
    var filterStrength by remember { mutableStateOf(0) }

    // Update filter configuration when cursor config changes
    LaunchedEffect(cursorConfig) {
        filter.setEnabled(cursorConfig.jitterFilterEnabled)
        filter.updateConfig(
            stationaryStrength = when (cursorConfig.filterStrength) {
                FilterStrength.Low -> 30
                FilterStrength.Medium -> 60
                FilterStrength.High -> 90
            },
            slowStrength = (cursorConfig.filterStrength.numericValue * 0.7f).toInt(),
            fastStrength = (cursorConfig.filterStrength.numericValue * 0.3f).toInt(),
            motionSensitivity = cursorConfig.motionSensitivity
        )
    }

    Column {
        Text(
            text = "Filter Test Area",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1D1D1F)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Touch and drag in the area below to test filter effectiveness",
            fontSize = 14.sp,
            color = Color(0xFF8E8E93)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Test area with visual feedback
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    color = Color(0xFFF2F2F7),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Motion level indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Motion Level:",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = "${motionLevel.toInt()} px/s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF007AFF)
                    )
                }

                // Filter strength indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Filter Strength:",
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = "$filterStrength%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF34C759)
                    )
                }

                // Position comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Raw: ${rawPosition.x.toInt()}, ${rawPosition.y.toInt()}",
                        fontSize = 12.sp,
                        color = Color(0xFFFF3B30)
                    )
                    Text(
                        text = "Filtered: ${filteredPosition.x.toInt()}, ${filteredPosition.y.toInt()}",
                        fontSize = 12.sp,
                        color = Color(0xFF007AFF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Red = Raw position, Blue = Filtered position. Lower filter strength means more responsiveness but less smoothing.",
            fontSize = 12.sp,
            color = Color(0xFF8E8E93)
        )
    }
}

/**
 * Load cursor configuration from preferences
 */
private fun loadCursorConfig(preferences: SharedPreferences): CursorConfig {
    return CursorConfig(
        type = when (preferences.getString("cursor_type", "Normal")) {
            "Hand" -> CursorType.Hand
            "Custom" -> CursorType.Custom
            else -> CursorType.Normal
        },
        color = preferences.getInt("cursor_color", 0xFF007AFF.toInt()),
        size = preferences.getInt("cursor_size", 48),
        speed = preferences.getInt("cursor_speed", 8),
        showCoordinates = preferences.getBoolean("show_coordinates", false),
        jitterFilterEnabled = preferences.getBoolean("jitter_filter_enabled", true),
        filterStrength = when (preferences.getString("filter_strength", "Medium")) {
            "Low" -> FilterStrength.Low
            "High" -> FilterStrength.High
            else -> FilterStrength.Medium
        },
        motionSensitivity = preferences.getFloat("motion_sensitivity", 0.7f)
    )
}

/**
 * Save cursor configuration to preferences
 */
private fun saveCursorConfig(preferences: SharedPreferences, config: CursorConfig) {
    preferences.edit().apply {
        putString("cursor_type", config.type.name)
        putInt("cursor_color", config.color)
        putInt("cursor_size", config.size)
        putInt("cursor_speed", config.speed)
        putBoolean("show_coordinates", config.showCoordinates)
        putBoolean("jitter_filter_enabled", config.jitterFilterEnabled)
        putString("filter_strength", config.filterStrength.name)
        putFloat("motion_sensitivity", config.motionSensitivity)
        apply()
    }
}

/**
 * Save boolean preference
 */
private fun saveBooleanPreference(preferences: SharedPreferences, key: String, value: Boolean) {
    preferences.edit().putBoolean(key, value).apply()
}