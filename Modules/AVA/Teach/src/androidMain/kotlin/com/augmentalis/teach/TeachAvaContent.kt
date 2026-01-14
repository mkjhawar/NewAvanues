package com.augmentalis.teach
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.core.domain.model.TrainExample

/**
 * Content component for Teach-Ava screen
 * Displays training examples with filtering and sorting
 */
@Composable
fun TeachAvaContentList(
    uiState: TeachAvaUiState,
    onEditExample: (TrainExample) -> Unit,
    onDeleteExample: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is TeachAvaUiState.Loading -> {
            LoadingView(modifier = modifier)
        }
        is TeachAvaUiState.Empty -> {
            EmptyStateView(modifier = modifier)
        }
        is TeachAvaUiState.Success -> {
            SuccessView(
                examples = uiState.examples,
                intents = uiState.intents,
                onEditExample = onEditExample,
                onDeleteExample = onDeleteExample,
                modifier = modifier
            )
        }
        is TeachAvaUiState.Error -> {
            ErrorView(
                message = uiState.message,
                onRetry = onRetry,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading training examples...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No training examples yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap the + button to add your first example and start teaching AVA",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccessView(
    examples: List<TrainExample>,
    intents: List<String>,
    onEditExample: (TrainExample) -> Unit,
    onDeleteExample: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedIntentFilter by remember { mutableStateOf<String?>(null) }

    if (showFilterSheet) {
        IntentFilterBottomSheet(
            intents = intents,
            selectedIntent = selectedIntentFilter,
            onIntentSelected = { intent ->
                selectedIntentFilter = intent
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    Column(modifier = modifier) {
        // Filter header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${examples.size} training example${if (examples.size != 1) "s" else ""}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            FilledTonalButton(
                onClick = { showFilterSheet = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Filter by intent",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = selectedIntentFilter ?: "All intents"
                )
            }
        }

        // Training examples list
        val filteredExamples = if (selectedIntentFilter != null) {
            examples.filter { it.intent == selectedIntentFilter }
        } else {
            examples
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                items = filteredExamples,
                key = { it.id }
            ) { example ->
                TrainingExampleCard(
                    example = example,
                    onEdit = { onEditExample(example) },
                    onDelete = { onDeleteExample(example.id) }
                )
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntentFilterBottomSheet(
    intents: List<String>,
    selectedIntent: String?,
    onIntentSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Filter by Intent",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            // "All intents" option
            FilterChip(
                selected = selectedIntent == null,
                onClick = { onIntentSelected(null) },
                label = { Text("All intents (${intents.size})") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            )

            // Individual intent filters
            intents.forEach { intent ->
                FilterChip(
                    selected = selectedIntent == intent,
                    onClick = { onIntentSelected(intent) },
                    label = { Text(intent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
        }
    }
}
