// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/main/MainTopAppBar.kt
// Opis: Ten plik zawiera komponent górnego paska nawigacyjnego (TopAppBar), który dynamicznie dostosowuje swój wygląd i akcje w zależności od aktywnego ekranu w MainTabsScreen.
package com.qjproject.liturgicalcalendar.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    showCategoryIcon: Boolean = false,
    showTagIcon: Boolean = false,
    searchActions: @Composable RowScope.() -> Unit = {},
    calendarActions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (showCategoryIcon) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Kategoria",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else if (showTagIcon) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = "Tag",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    AutoResizingText(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
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
                        Icon(
                            Icons.Default.Check,
                            "Zapisz zmiany",
                            tint = if (isSaveEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                } else if (showEditButton) {
                    // Przycisk edycji wyłączony - niewidzialny i nieinteraktywny
                    Spacer(Modifier.width(48.dp))
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