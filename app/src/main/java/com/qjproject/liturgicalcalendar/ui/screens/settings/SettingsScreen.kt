package com.qjproject.liturgicalcalendar.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Efekt do wyświetlania komunikatu, gdy tylko pojawi się w stanie
    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage() // Czyścimy wiadomość po wyświetleniu
        }
    }

    // --- POCZĄTEK ZMIANY: Niestandardowy, elegancki Snackbar ---
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                // Używamy kolorów z motywu, aby Snackbar pasował do reszty aplikacji
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    ) { padding ->
        // --- KONIEC ZMIANY ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { viewModel.exportData() },
                enabled = !uiState.isExporting
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary // Dopasowanie koloru
                    )
                } else {
                    Text(text = "Eksportuj dane")
                }
            }
        }
    }
}