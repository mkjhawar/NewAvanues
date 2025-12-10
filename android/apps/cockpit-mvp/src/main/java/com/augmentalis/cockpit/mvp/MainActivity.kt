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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CockpitMVPScreen()
            }
        }
    }
}

@Composable
fun CockpitMVPScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                    Text("Cockpit Library Loaded Successfully",
                         style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("✓ Common/Cockpit KMP library")
                    Text("✓ AppWindow model")
                    Text("✓ Vector3D spatial positioning")
                    Text("✓ LayoutPreset system")
                    Text("✓ LinearHorizontalLayout preset")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Next steps:", style = MaterialTheme.typography.titleSmall)
                    Text("• Implement UI components (ControlRail, UtilityBelt, WindowDock)")
                    Text("• Add workspace management")
                    Text("• Integrate VoiceOS")
                }
            }
        }
    }
}
