// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\MainActivity\MainTopAppBar.kt
// Opis: Ten plik zawiera komponent górnego paska nawigacyjnego (TopAppBar), który dynamicznie dostosowuje swój wygląd i akcje w zależności od aktywnego ekranu w MainTabsScreen.
package com.qjproject.liturgicalcalendar.MainActivity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainTopAppBar(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    isBrowseScreenInEditMode: Boolean = false,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    isSaveEnabled: Boolean = false,
    isCalendarScreenActive: Boolean = false,
    isSearchScreenActive: Boolean = false,
    searchActions: @Composable RowScope.() -> Unit = {},
    calendarActions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                AutoResizingText(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            navigationIcon = {
                if (isBrowseScreenInEditMode) {
                    IconButton(onClick = onCancelClick) {
                        Icon(Icons.Default.Close, "Anuluj edycję")
                    }
                } else if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            },
            actions = {
                if (isSearchScreenActive) {
                    searchActions()
                } else if (isBrowseScreenInEditMode) {
                    IconButton(onClick = onSaveClick, enabled = isSaveEnabled) {
                        Icon(Icons.Default.Check, "Zapisz zmiany", tint = if (isSaveEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                } else if (showEditButton) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, "Edytuj")
                    }
                } else if (isCalendarScreenActive) {
                    calendarActions()
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.primary,
                navigationIconContentColor = MaterialTheme.colorScheme.primary
            ),
            windowInsets = TopAppBarDefaults.windowInsets
        )
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    }
}