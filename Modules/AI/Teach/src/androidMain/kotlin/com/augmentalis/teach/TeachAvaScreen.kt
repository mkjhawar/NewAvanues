package com.augmentalis.teach
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.core.domain.model.TrainExample

/**
 * Main Teach-Ava screen for training intent classification
 * Implements IDEACODE Teach-Ava pattern for continuous learning
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachAvaScreen(
    viewModel: TeachAvaViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddExample by remember { mutableStateOf(false) }
    var editingExample by remember { mutableStateOf<TrainExample?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teach AVA") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AvanueTheme.colors.primaryContainer,
                    titleContentColor = AvanueTheme.colors.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExample = true },
                containerColor = AvanueTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add training example"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (showAddExample) {
            AddExampleDialog(
                onDismiss = { showAddExample = false },
                onExampleAdded = { example ->
                    viewModel.addExample(example)
                    showAddExample = false
                }
            )
        }

        editingExample?.let { example ->
            EditExampleDialog(
                example = example,
                onDismiss = { editingExample = null },
                onExampleUpdated = { updatedExample ->
                    viewModel.updateExample(updatedExample)
                    editingExample = null
                }
            )
        }

        TeachAvaContentList(
            uiState = uiState,
            onEditExample = { example ->
                editingExample = example
            },
            onDeleteExample = { exampleId ->
                viewModel.deleteExample(exampleId)
            },
            onRetry = {
                viewModel.clearError()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
