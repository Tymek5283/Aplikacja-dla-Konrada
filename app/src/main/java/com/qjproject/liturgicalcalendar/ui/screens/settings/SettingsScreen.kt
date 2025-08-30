package com.qjproject.liturgicalcalendar.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onRestartApp: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToTagManagement: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.startImport(it) }
        }
    )

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.showRestartPrompt) {
        RestartPromptDialog(
            onDismiss = { viewModel.dismissRestartPrompt() },
            onConfirm = onRestartApp
        )
    }

    if (uiState.showExportConfigDialog) {
        ExportConfigurationDialog(
            onDismiss = { viewModel.hideExportConfigDialog() },
            onConfirm = { configuration -> viewModel.exportData(configuration) }
        )
    }

    uiState.importPreviewState?.let { previewState ->
        if (uiState.showImportConfigDialog) {
            ImportConfigurationDialog(
                previewState = previewState,
                onConfigurationChange = { configuration -> viewModel.updateImportConfiguration(configuration) },
                onConfirm = { 
                    viewModel.confirmImport()
                },
                onDismiss = { viewModel.hideImportConfigDialog() }
            )
        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsTile(
                title = "Eksportuj dane",
                subtitle = "Zapisz kopię zapasową swoich danych",
                icon = Icons.Default.Upload,
                isLoading = uiState.isExporting,
                onClick = { viewModel.showExportConfigDialog() },
                enabled = !uiState.isImporting && !uiState.isExporting
            )
            SettingsTile(
                title = "Importuj dane",
                subtitle = "Przywróć dane z kopii zapasowej",
                icon = Icons.Default.Download,
                isLoading = uiState.isImporting,
                onClick = { importLauncher.launch("application/zip") },
                enabled = !uiState.isImporting && !uiState.isExporting
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsTile(
                title = "Zarządzaj kategoriami",
                subtitle = "Dodawaj, edytuj i usuwaj kategorie pieśni",
                icon = Icons.Default.Category,
                onClick = onNavigateToCategoryManagement,
                enabled = !uiState.isImporting && !uiState.isExporting
            )
            SettingsTile(
                title = "Zarządzaj tagami",
                subtitle = "Dodawaj, edytuj i usuwaj tagi pieśni",
                icon = Icons.Default.Label,
                onClick = onNavigateToTagManagement,
                enabled = !uiState.isImporting && !uiState.isExporting
            )
        }
    }
}

@Composable
fun SettingsTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, enabled = enabled),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // --- POCZĄTEK ZMIANY ---
        colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        // --- KONIEC ZMIANY ---
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
            if (isLoading) {
                Spacer(Modifier.width(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun RestartPromptDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Import zakończony",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text("Dane zostały pomyślnie zaimportowane. Aby zmiany były widoczne, aplikacja musi zostać uruchomiona ponownie.")
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Później")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("Uruchom ponownie")
                    }
                }
            }
        }
    }
}