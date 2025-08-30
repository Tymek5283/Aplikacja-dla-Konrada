package com.qjproject.liturgicalcalendar.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
fun ExportConfigurationDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExportConfiguration) -> Unit
) {
    var configuration by remember { 
        mutableStateOf(ExportConfiguration()) 
    }

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
                    text = "Konfiguracja eksportu",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = SaturatedNavy
                )
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                
                Text(
                    text = "Wybierz dane do eksportu:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Przewijalna sekcja z opcjami eksportu
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Przełączniki w kolejności określonej w wymaganiach
                    ExportOptionSwitch(
                        title = "Pieśni",
                        subtitle = "Wszystkie pieśni z tekstami i metadanymi",
                        checked = configuration.includeSongs,
                        onCheckedChange = { configuration = configuration.copy(includeSongs = it) }
                    )

                    ExportOptionSwitch(
                        title = "Dni",
                        subtitle = "Czytania i sugestie pieśni dla okresów liturgicznych",
                        checked = configuration.includeDays,
                        onCheckedChange = { configuration = configuration.copy(includeDays = it) }
                    )

                    ExportOptionSwitch(
                        title = "Kategorie",
                        subtitle = "Definicje kategorii i ich skróty",
                        checked = configuration.includeCategories,
                        onCheckedChange = { configuration = configuration.copy(includeCategories = it) }
                    )

                    ExportOptionSwitch(
                        title = "Tagi",
                        subtitle = "Wszystkie tagi przypisane do pieśni",
                        checked = configuration.includeTags,
                        onCheckedChange = { configuration = configuration.copy(includeTags = it) }
                    )

                    ExportOptionSwitch(
                        title = "Neumy",
                        subtitle = "Pliki PDF z notacją muzyczną",
                        checked = configuration.includeNeumy,
                        onCheckedChange = { configuration = configuration.copy(includeNeumy = it) }
                    )

                    ExportOptionSwitch(
                        title = "Notatki",
                        subtitle = "Wszystkie notatki użytkownika",
                        checked = configuration.includeNotes,
                        onCheckedChange = { configuration = configuration.copy(includeNotes = it) }
                    )

                    ExportOptionSwitch(
                        title = "Lata",
                        subtitle = "Dane specyficzne dla poszczególnych lat",
                        checked = configuration.includeYears,
                        onCheckedChange = { configuration = configuration.copy(includeYears = it) }
                    )
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
                        onClick = { onConfirm(configuration) },
                        enabled = configuration.hasAnyOptionSelected(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaturatedNavy
                        )
                    ) {
                        Text("Eksportuj")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportOptionSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SaturatedNavy,
                checkedTrackColor = SaturatedNavy.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
    }
}
