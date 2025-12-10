/**
 * VoiceTrainingActivity.kt - Voice Command Training Interface
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-22
 */

package com.augmentalis.voiceos.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService

/**
 * Voice Training Activity - Interactive command training and testing
 */
class VoiceTrainingActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                VoiceTrainingScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTrainingScreen() {
    var selectedLanguage by remember { mutableStateOf("en") }
    var currentTrainingStep by remember { mutableIntStateOf(0) }
    var isTraining by remember { mutableStateOf(false) }
    var trainingResults by remember { mutableStateOf<List<TrainingResult>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.RecordVoiceOver,
                    contentDescription = "Voice Training",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Voice Command Training",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Improve recognition accuracy and learn new commands",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Language Selection
        LanguageSelectionCard(
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { selectedLanguage = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Training Session
        TrainingSessionCard(
            currentStep = currentTrainingStep,
            isTraining = isTraining,
            language = selectedLanguage,
            onStartTraining = {
                isTraining = true
                currentTrainingStep = 0
                trainingResults = emptyList()
                // Start training simulation
                scope.launch {
                    val results = runTrainingSession(selectedLanguage)
                    trainingResults = results
                    isTraining = false
                }
            },
            trainingResults = trainingResults
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Test
        QuickTestCard(
            isEnabled = !isTraining,
            language = selectedLanguage
        )
    }
}

@Composable
fun LanguageSelectionCard(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Training Language",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val languages = mapOf(
                "en" to "English",
                "es" to "Spanish", 
                "fr" to "French",
                "de" to "German",
                "ja" to "Japanese",
                "zh" to "Chinese"
            )
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(languages.toList()) { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLanguage == code,
                            onClick = { onLanguageSelected(code) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrainingSessionCard(
    currentStep: Int,
    isTraining: Boolean,
    language: String,
    onStartTraining: () -> Unit,
    trainingResults: List<TrainingResult>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Training Session",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!isTraining && trainingResults.isEmpty()) {
                Text(
                    "Start a training session to improve voice recognition accuracy for $language commands.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartTraining,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Training Session")
                }
            } else if (isTraining) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { (currentStep + 1) / 5f }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Training step ${currentStep + 1} of 5",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        getTrainingCommand(currentStep, language),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Show results
                Text(
                    "Training Results",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                trainingResults.forEach { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (result.accuracy >= 0.8f) Icons.Default.CheckCircle 
                            else if (result.accuracy >= 0.6f) Icons.Default.Warning
                            else Icons.Default.Error,
                            contentDescription = "Accuracy",
                            tint = if (result.accuracy >= 0.8f) MaterialTheme.colorScheme.primary
                            else if (result.accuracy >= 0.6f) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                result.command,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Accuracy: ${(result.accuracy * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartTraining,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Train Again")
                }
            }
        }
    }
}

@Composable
fun QuickTestCard(
    isEnabled: Boolean,
    @Suppress("UNUSED_PARAMETER")
    language: String
) {
    var testCommand by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Quick Command Test",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = testCommand,
                onValueChange = { testCommand = it },
                label = { Text("Voice Command") },
                placeholder = { Text("e.g., \"go back\", \"volume up\"") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled && !isTesting
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    if (testCommand.isNotBlank()) {
                        isTesting = true
                        scope.launch {
                            delay(1000)
                            val success = VoiceAccessibilityService.executeCommand(testCommand)
                            testResult = if (success) "✓ Command executed successfully" 
                                        else "✗ Command failed to execute"
                            isTesting = false
                        }
                    }
                },
                enabled = isEnabled && !isTesting && testCommand.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTesting) "Testing..." else "Test Command")
            }
            
            testResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    result,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result.startsWith("✓")) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private suspend fun runTrainingSession(language: String): List<TrainingResult> {
    val commands = getTrainingCommands(language)
    val results = mutableListOf<TrainingResult>()
    
    commands.forEachIndexed { _, command ->
        delay(2000) // Simulate training time
        
        // Simulate varying accuracy based on command complexity
        val accuracy = when {
            command.contains("back") || command.contains("home") -> 0.95f
            command.contains("volume") -> 0.88f
            command.contains("click") -> 0.75f
            command.contains("scroll") -> 0.82f
            else -> 0.70f
        } + (Math.random() * 0.1 - 0.05).toFloat() // Add some randomness
        
        results.add(
            TrainingResult(
                command = command,
                accuracy = accuracy.coerceIn(0f, 1f)
            )
        )
    }
    
    return results
}

private fun getTrainingCommand(step: Int, language: String): String {
    val commands = getTrainingCommands(language)
    return if (step < commands.size) "Say: \"${commands[step]}\"" else "Training Complete!"
}

private fun getTrainingCommands(language: String): List<String> = when (language) {
    "es" -> listOf("atrás", "inicio", "subir volumen", "bajar volumen", "clic botón")
    "fr" -> listOf("retour", "accueil", "augmenter le volume", "diminuer le volume", "cliquer bouton")
    "de" -> listOf("zurück", "startseite", "lauter", "leiser", "klicken")
    "ja" -> listOf("戻る", "ホーム", "音量を上げる", "音量を下げる", "クリック")
    "zh" -> listOf("返回", "主页", "增加音量", "降低音量", "点击")
    else -> listOf("back", "home", "volume up", "volume down", "click button")
}

data class TrainingResult(
    val command: String,
    val accuracy: Float
)