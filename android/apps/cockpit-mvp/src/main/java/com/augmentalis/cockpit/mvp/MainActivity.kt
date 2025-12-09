package com.augmentalis.cockpit.mvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.ui.*
import com.augmentalis.cockpit.ui.compose.*
import com.augmentalis.cockpit.ui.theme.CockpitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CockpitTheme {
                CockpitMVPScreen()
            }
        }
    }
}

@Composable
fun CockpitMVPScreen() {
    var selectedAction by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Cockpit MVP Demo",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(32.dp))
                Card(modifier = Modifier.fillMaxWidth(0.8f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("UI Components Active", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✓ Control Rail")
                        Text("✓ Utility Belt")
                        Text("✓ Window Dock")
                        selectedAction?.let {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Last Action: $it")
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                ControlRailCompose(
                    rail = ControlRail(visible = true, buttons = defaultButtons()),
                    onButtonClick = { action -> selectedAction = action.name }
                )
            }

            UtilityBeltCompose(
                belt = UtilityBelt(visible = true, widgets = defaultWidgets()),
                onWidgetClick = { selectedAction = "Widget: $it" }
            )

            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                WindowDockCompose(
                    dock = WindowDock(visible = true, activeWindowId = "main", windowCount = 3),
                    onWindowClick = { selectedAction = "Window: $it" }
                )
            }
        }
    }
}
