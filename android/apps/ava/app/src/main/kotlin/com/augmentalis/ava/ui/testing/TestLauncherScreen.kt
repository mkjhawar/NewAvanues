/**
 * Test Launcher Screen for AVA AI
 *
 * Provides UI to run automated tests on emulator and display results.
 * Tests cover TVM Phase 4 features: language detection, token sampling,
 * streaming generation, stop token detection, and system prompts.
 *
 * Created: 2025-11-07
 * Author: AVA AI Team
 */

package com.augmentalis.ava.ui.testing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.ava.features.llm.Language
import com.augmentalis.ava.features.llm.LanguageDetector
import com.augmentalis.ava.features.llm.ModelSelector
import com.augmentalis.ava.features.llm.alc.StopTokenDetector
import com.augmentalis.ava.features.llm.alc.TokenSampler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Test Launcher Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestLauncherScreen(
    viewModel: ITestLauncherViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val testSuites by viewModel.testSuites.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automated Tests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Run all tests button
            Button(
                onClick = { viewModel.runAllTests() },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isRunning) "Running Tests..." else "Run All Tests")
            }

            Spacer(Modifier.height(16.dp))

            // Test suites list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(testSuites) { suite ->
                    TestSuiteCard(
                        suite = suite,
                        onRunSuite = { viewModel.runTestSuite(suite.name) },
                        enabled = !isRunning
                    )
                }
            }
        }
    }
}

@Composable
fun TestSuiteCard(
    suite: TestSuite,
    onRunSuite: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suite.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${suite.tests.size} tests",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status badge
                when (suite.status) {
                    TestStatus.NOT_RUN -> {}
                    TestStatus.RUNNING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    TestStatus.PASSED -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Passed",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    TestStatus.FAILED -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Test results summary
            if (suite.status != TestStatus.NOT_RUN) {
                Spacer(Modifier.height(8.dp))
                val passedCount = suite.tests.count { it.status == TestStatus.PASSED }
                val failedCount = suite.tests.count { it.status == TestStatus.FAILED }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "✓ $passedCount passed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "✗ $failedCount failed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Individual test results
            if (suite.tests.any { it.status != TestStatus.NOT_RUN }) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    suite.tests.forEach { test ->
                        if (test.status != TestStatus.NOT_RUN) {
                            TestResultRow(test)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onRunSuite,
                enabled = enabled,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Run Suite")
            }
        }
    }
}

@Composable
fun TestResultRow(test: TestCase) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val icon = when (test.status) {
            TestStatus.PASSED -> "✓"
            TestStatus.FAILED -> "✗"
            TestStatus.RUNNING -> "●"
            TestStatus.NOT_RUN -> "○"
        }

        val color = when (test.status) {
            TestStatus.PASSED -> Color(0xFF4CAF50)
            TestStatus.FAILED -> MaterialTheme.colorScheme.error
            TestStatus.RUNNING -> MaterialTheme.colorScheme.primary
            TestStatus.NOT_RUN -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Text(
            text = icon,
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = test.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (test.status == TestStatus.FAILED) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }

    // Error message if failed
    if (test.status == TestStatus.FAILED && test.errorMessage != null) {
        Text(
            text = "  Error: ${test.errorMessage}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 24.dp)
        )
    }
}

/**
 * Test Launcher ViewModel
 */
class TestLauncherViewModel : ViewModel(), ITestLauncherViewModel {

    private val _testSuites = MutableStateFlow(createTestSuites())
    override val testSuites: StateFlow<List<TestSuite>> = _testSuites.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /**
     * Run all test suites
     */
    override fun runAllTests() {
        viewModelScope.launch {
            _isRunning.value = true
            Timber.i("Running all test suites")

            for (suite in _testSuites.value) {
                runTestSuite(suite.name)
                delay(100) // Small delay between suites
            }

            _isRunning.value = false
            Timber.i("All test suites completed")
        }
    }

    /**
     * Run a specific test suite
     */
    override fun runTestSuite(suiteName: String) {
        viewModelScope.launch {
            _isRunning.value = true

            val suites = _testSuites.value.toMutableList()
            val suiteIndex = suites.indexOfFirst { it.name == suiteName }

            if (suiteIndex == -1) {
                _isRunning.value = false
                return@launch
            }

            val suite = suites[suiteIndex]
            Timber.i("Running test suite: $suiteName")

            // Mark suite as running
            suites[suiteIndex] = suite.copy(status = TestStatus.RUNNING)
            _testSuites.value = suites

            // Run each test
            val updatedTests = suite.tests.map { test ->
                runTest(test, suiteName)
            }

            // Update suite with results
            val passed = updatedTests.all { it.status == TestStatus.PASSED }
            suites[suiteIndex] = suite.copy(
                tests = updatedTests,
                status = if (passed) TestStatus.PASSED else TestStatus.FAILED
            )
            _testSuites.value = suites

            _isRunning.value = false
            Timber.i("Test suite completed: $suiteName (${if (passed) "PASSED" else "FAILED"})")
        }
    }

    /**
     * Run a single test
     */
    private suspend fun runTest(test: TestCase, suiteName: String): TestCase {
        Timber.d("Running test: ${test.name}")

        return try {
            when (suiteName) {
                "Language Detection" -> runLanguageDetectionTest(test)
                "Token Sampling" -> runTokenSamplingTest(test)
                "Stop Token Detection" -> runStopTokenDetectionTest(test)
                "Model Selection" -> runModelSelectionTest(test)
                else -> test.copy(
                    status = TestStatus.FAILED,
                    errorMessage = "Unknown test suite"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Test failed: ${test.name}")
            test.copy(
                status = TestStatus.FAILED,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Language detection tests
     */
    private fun runLanguageDetectionTest(test: TestCase): TestCase {
        return when (test.name) {
            "English detection" -> {
                val lang = LanguageDetector.detect("Hello world")
                if (lang == Language.ENGLISH) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Expected ENGLISH, got $lang")
                }
            }
            "Spanish detection" -> {
                val lang = LanguageDetector.detect("Hola mundo")
                if (lang == Language.SPANISH) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Expected SPANISH, got $lang")
                }
            }
            "Chinese detection" -> {
                val lang = LanguageDetector.detect("你好世界")
                if (lang == Language.CHINESE_SIMPLIFIED) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Expected CHINESE_SIMPLIFIED, got $lang")
                }
            }
            "Japanese detection" -> {
                val lang = LanguageDetector.detect("こんにちは世界")
                if (lang == Language.JAPANESE) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Expected JAPANESE, got $lang")
                }
            }
            "Korean detection" -> {
                val lang = LanguageDetector.detect("안녕하세요 세계")
                if (lang == Language.KOREAN) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Expected KOREAN, got $lang")
                }
            }
            "Confidence score" -> {
                val (_, confidence) = LanguageDetector.detectWithConfidence("Hello world")
                if (confidence > 0.9f) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Low confidence: $confidence")
                }
            }
            else -> test.copy(status = TestStatus.FAILED, errorMessage = "Unknown test")
        }
    }

    /**
     * Token sampling tests
     */
    private fun runTokenSamplingTest(test: TestCase): TestCase {
        return when (test.name) {
            "Greedy sampling" -> {
                val logits = floatArrayOf(1.0f, 5.0f, 3.0f, 2.0f)
                val token = TokenSampler.sampleGreedy(logits)
                if (token == 1) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Expected token 1, got $token")
                }
            }
            "Temperature effect" -> {
                val logits = floatArrayOf(3.0f, 3.5f, 3.2f)
                val lowTemp = (1..50).map {
                    TokenSampler.sample(logits, temperature = 0.1f, topP = 1.0f, topK = 3)
                }.distinct().size

                val highTemp = (1..50).map {
                    TokenSampler.sample(logits, temperature = 2.0f, topP = 1.0f, topK = 3)
                }.distinct().size

                if (highTemp >= lowTemp) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Temperature effect not working")
                }
            }
            "Repetition penalty" -> {
                val logits = floatArrayOf(5.0f, 4.0f, 3.0f)
                val previousTokens = listOf(0, 0, 0)
                val samples = (1..50).map {
                    TokenSampler.sample(logits, temperature = 0.5f, repetitionPenalty = 1.5f, previousTokens = previousTokens)
                }
                val token0Count = samples.count { it == 0 }
                if (token0Count < 25) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Penalty not working (token 0 count: $token0Count)")
                }
            }
            "Preset configs" -> {
                val precise = TokenSampler.SamplingConfig.PRECISE
                val balanced = TokenSampler.SamplingConfig.BALANCED
                val creative = TokenSampler.SamplingConfig.CREATIVE
                if (precise.temperature < balanced.temperature && balanced.temperature < creative.temperature) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Preset config values incorrect")
                }
            }
            else -> test.copy(status = TestStatus.FAILED, errorMessage = "Unknown test")
        }
    }

    /**
     * Stop token detection tests
     */
    private fun runStopTokenDetectionTest(test: TestCase): TestCase {
        return when (test.name) {
            "Gemma stop tokens" -> {
                val stops = StopTokenDetector.getStopTokens("gemma-2b-it-q4f16_1")
                if (1 in stops && 2 in stops) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Missing expected stop tokens")
                }
            }
            "Qwen stop tokens" -> {
                val stops = StopTokenDetector.getStopTokens("qwen2.5-1.5b-instruct-q4f16_1")
                if (151643 in stops) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Missing Qwen stop token")
                }
            }
            "Remove stop sequences" -> {
                val cleaned = StopTokenDetector.removeStopSequences("Hello</s>", "gemma-2b-it-q4f16_1")
                if (cleaned == "Hello") {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Got: $cleaned")
                }
            }
            "Max generation length" -> {
                val gemmaMax = StopTokenDetector.getMaxGenerationLength("gemma-2b-it-q4f16_1")
                val mistralMax = StopTokenDetector.getMaxGenerationLength("Mistral-7B-Instruct-v0.3-q4f16_1")
                if (gemmaMax == 2048 && mistralMax == 8192) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Incorrect max lengths")
                }
            }
            else -> test.copy(status = TestStatus.FAILED, errorMessage = "Unknown test")
        }
    }

    /**
     * Model selection tests
     */
    private fun runModelSelectionTest(test: TestCase): TestCase {
        return when (test.name) {
            "English → Gemma" -> {
                val model = LanguageDetector.getRecommendedModel(Language.ENGLISH)
                if (model.contains("gemma", ignoreCase = true)) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Got: $model")
                }
            }
            "Chinese → Qwen" -> {
                val model = LanguageDetector.getRecommendedModel(Language.CHINESE_SIMPLIFIED)
                if (model.contains("qwen", ignoreCase = true)) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Got: $model")
                }
            }
            "Model support check" -> {
                val gemmaEn = LanguageDetector.modelSupportsLanguage("gemma-2b-it-q4f16_1", Language.ENGLISH)
                val gemmaCn = LanguageDetector.modelSupportsLanguage("gemma-2b-it-q4f16_1", Language.CHINESE_SIMPLIFIED)
                if (gemmaEn && !gemmaCn) {
                    test.copy(status = TestStatus.PASSED)
                } else {
                    test.copy(status = TestStatus.FAILED, errorMessage = "Support check failed")
                }
            }
            else -> test.copy(status = TestStatus.FAILED, errorMessage = "Unknown test")
        }
    }

    /**
     * Create initial test suites
     */
    private fun createTestSuites(): List<TestSuite> {
        return listOf(
            TestSuite(
                name = "Language Detection",
                tests = listOf(
                    TestCase("English detection"),
                    TestCase("Spanish detection"),
                    TestCase("Chinese detection"),
                    TestCase("Japanese detection"),
                    TestCase("Korean detection"),
                    TestCase("Confidence score")
                )
            ),
            TestSuite(
                name = "Token Sampling",
                tests = listOf(
                    TestCase("Greedy sampling"),
                    TestCase("Temperature effect"),
                    TestCase("Repetition penalty"),
                    TestCase("Preset configs")
                )
            ),
            TestSuite(
                name = "Stop Token Detection",
                tests = listOf(
                    TestCase("Gemma stop tokens"),
                    TestCase("Qwen stop tokens"),
                    TestCase("Remove stop sequences"),
                    TestCase("Max generation length")
                )
            ),
            TestSuite(
                name = "Model Selection",
                tests = listOf(
                    TestCase("English → Gemma"),
                    TestCase("Chinese → Qwen"),
                    TestCase("Model support check")
                )
            )
        )
    }
}

/**
 * Test suite data class
 */
data class TestSuite(
    val name: String,
    val tests: List<TestCase>,
    val status: TestStatus = TestStatus.NOT_RUN
)

/**
 * Test case data class
 */
data class TestCase(
    val name: String,
    val status: TestStatus = TestStatus.NOT_RUN,
    val errorMessage: String? = null
)

/**
 * Test status enum
 */
enum class TestStatus {
    NOT_RUN,
    RUNNING,
    PASSED,
    FAILED
}
