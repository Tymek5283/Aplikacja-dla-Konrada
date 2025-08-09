package com.qjproject.liturgicalcalendar.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onRestartApp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.importData(it) }
        }
    )

    // Efekt do pokazywania Snackbarów
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Efekt do pokazywania dialogu restartu
    if (uiState.showRestartPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestartPrompt() },
            title = { Text("Import zakończony") },
            text = { Text("Dane zostały pomyślnie zaimportowane. Aby zmiany były widoczne, aplikacja musi zostać uruchomiona ponownie.") },
            confirmButton = {
                TextButton(onClick = onRestartApp) {
                    Text("Uruchom ponownie")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestartPrompt() }) {
                    Text("Później")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Przycisk Eksportu
            Button(
                onClick = { viewModel.exportData() },
                enabled = !uiState.isExporting && !uiState.isImporting,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "Eksportuj dane")
                }
            }

            // Przycisk Importu
            Button(
                onClick = { importLauncher.launch("application/zip") },
                enabled = !uiState.isImporting && !uiState.isExporting,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                if (uiState.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "Importuj dane")
                }
            }
        }
    }
}