// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/DayDetailsTopAppBar.kt
// Opis: Plik ten zawiera komponenty górnego paska nawigacyjnego (TopAppBar) dla ekranu DayDetailsScreen, oddzielając logikę paska dla trybu podglądu i edycji.

package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewModeTopAppBar(title: String, onNavigateBack: () -> Unit, onEditClick: () -> Unit) {
    Column {
        CenterAlignedTopAppBar(
            title = { AutoResizingText(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć") } },
            actions = { IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Edytuj dzień") } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.primary,
                navigationIconContentColor = MaterialTheme.colorScheme.primary
            )
        )
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditModeTopAppBar(title: String, onCancelClick: () -> Unit, onSaveClick: () -> Unit, isSaveEnabled: Boolean) {
    Column {
        CenterAlignedTopAppBar(
            title = { AutoResizingText(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) },
            navigationIcon = { IconButton(onClick = onCancelClick) { Icon(Icons.Default.Close, "Anuluj edycję") } },
            actions = { IconButton(onClick = onSaveClick, enabled = isSaveEnabled) { Icon(Icons.Default.Check, "Zapisz zmiany", tint = if (isSaveEnabled) MaterialTheme.colorScheme.primary else Color.Gray) } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.primary,
                navigationIconContentColor = MaterialTheme.colorScheme.primary
            )
        )
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    }
}