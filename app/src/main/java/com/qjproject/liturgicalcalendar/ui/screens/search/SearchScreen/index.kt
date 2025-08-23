// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/search/index.kt
// Opis: Główny plik komponentu SearchScreen. Pełni rolę "indexu", składając widok z mniejszych, wyspecjalizowanych komponentów, zarządzając stanem i logiką nawigacji.

package com.qjproject.liturgicalcalendar.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.qjproject.liturgicalcalendar.data.Song

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToSong: (Song) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.triggerSearch()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (uiState.showAddSongDialog) {
        AddSongDialog(
            categories = uiState.allCategories,
            error = uiState.addSongError,
            onDismiss = { viewModel.onDismissAddSongDialog() },
            onConfirm = { title, siedl, sak, dn, text, category ->
                viewModel.saveNewSong(title, siedl, sak, dn, text, category)
            },
            onValidate = { title, siedl, sak, dn ->
                viewModel.validateSongInput(title, siedl, sak, dn)
            }
        )
    }

    when(val dialogState = uiState.deleteDialogState) {
        is DeleteDialogState.ConfirmInitial -> {
            ConfirmDeleteDialog(
                song = dialogState.song,
                onConfirm = { viewModel.onConfirmInitialDelete() },
                onDismiss = { viewModel.onDismissDeleteDialog() }
            )
        }
        is DeleteDialogState.ConfirmOccurrences -> {
            ConfirmDeleteOccurrencesDialog(
                song = dialogState.song,
                onConfirmDeleteAll = { viewModel.onFinalDelete(true) },
                onConfirmDeleteOne = { viewModel.onFinalDelete(false) },
                onDismiss = { viewModel.onDismissDeleteDialog() }
            )
        }
        is DeleteDialogState.None -> {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange
            )

            Divider()

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.searchPerformed && uiState.results.isEmpty() -> {
                    NoResults()
                }

                else -> {
                    ResultsList(
                        results = uiState.results,
                        onNavigateToSong = onNavigateToSong,
                        onSongLongClick = { song -> viewModel.onSongLongPress(song) }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { viewModel.onAddSongClicked() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj pieśń")
        }
    }
}