package com.qjproject.liturgicalcalendar.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
fun ImportConfigurationDialog(
    previewState: ImportPreviewState,
    onConfigurationChange: (ImportConfiguration) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Konfiguracja importu",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = SaturatedNavy
                )
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                
                if (previewState.isAnalyzing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analizowanie zawartości pliku...")
                    }
                } else if (previewState.analysisError != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = previewState.analysisError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        text = "Wybierz dane do importu:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Przewijalna sekcja z opcjami importu
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Przełączniki w kolejności określonej w wymaganiach
                        ImportOptionSwitch(
                            title = "Pieśni",
                            subtitle = "Wszystkie pieśni z tekstami i metadanymi",
                            checked = previewState.configuration.includeSongs && previewState.availableData.hasSongs,
                            enabled = previewState.availableData.hasSongs,
                            onCheckedChange = { 
                                onConfigurationChange(previewState.configuration.copy(includeSongs = it))
                            }
                        )

                        ImportOptionSwitch(
                            title = "Dni",
                            subtitle = "Czytania i sugestie pieśni dla okresów liturgicznych",
                            checked = previewState.configuration.includeDays && previewState.availableData.hasDays,
                            enabled = previewState.availableData.hasDays,
                            onCheckedChange = { 
                                onConfigurationChange(previewState.configuration.copy(includeDays = it))
                            }
                        )

                        ImportOptionSwitch(
                            title = "Kategorie",
                            subtitle = "Definicje kategorii i ich skróty",
                            checked = previewState.configuration.includeCategories && previewState.availableData.hasCategories,
                            enabled = previewState.availableData.hasCategories,
                            onCheckedChange = { 
                                onConfigurationChange(previewState.configuration.copy(includeCategories = it))
                            }
                        )

                        ImportOptionSwitch(
                            title = "Tagi",
                            subtitle = "Wszystkie tagi przypisane do pieśni",
                            checked = previewState.configuration.includeTags && previewState.availableData.hasTags,
                            enabled = previewState.availableData.hasTags,
                            onCheckedChange = { 
                                onConfigurationChange(previewState.configuration.copy(includeTags = it))
                            }
                        )

                        ImportOptionSwitch(
                            title = "Neumy",
                            subtitle = "Pliki PDF z notacją muzyczną",
                            checked = previewState.configuration.includeNeumy && previewState.availableData.hasNeumy,
                            enabled = previewState.availableData.hasNeumy,
                            onCheckedChange = { 
                                onConfigurationChange(previewState.configuration.copy(includeNeumy = it))
                            }
                        )

                        ImportOptionSwitch(
                            title = "Notatki",
                            subtitle = "Wszystkie notatki użytkownika (dopisywane do istniejących)",
                            checked = previewState.configuration.includeNotes && previewState.availableData.hasNotes,
                            enabled = previewState.availableData.hasNotes,
                            onCheckedChange = { 
                                onConfigurationChange(previewState.configuration.copy(includeNotes = it))
                            }
                        )

                        ImportOptionSwitch(
                            title = "Lata",
                            subtitle = "Dane specyficzne dla poszczególnych lat",
                            checked = previewState.configuration.includeYears && previewState.availableData.hasYears,
                            enabled = previewState.availableData.hasYears,
                            onCheckedChange = { 
                                onConfigurationChange(previewState.configuration.copy(includeYears = it))
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = !previewState.isAnalyzing && previewState.analysisError == null && hasAnyDataToImport(previewState.availableData),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaturatedNavy
                        )
                    ) {
                        Text("Importuj")
                    }
                }
            }
        }
    }
}

private fun hasAnyDataToImport(availableData: AvailableImportData): Boolean {
    return availableData.hasDays || 
           availableData.hasSongs || 
           availableData.hasCategories || 
           availableData.hasTags || 
           availableData.hasNeumy || 
           availableData.hasNotes || 
           availableData.hasYears
}

@Composable
private fun ImportOptionSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else { {} },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SaturatedNavy,
                checkedTrackColor = SaturatedNavy.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
    }
}
