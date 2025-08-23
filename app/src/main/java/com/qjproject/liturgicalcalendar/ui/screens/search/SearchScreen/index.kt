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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.data.Song

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToSong: (Song) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showAddSongDialog) {
        AddSongDialog(
            categories = uiState.allCategories,
            error = uiState.addSongError,
            initialCategoryName = uiState.selectedCategory?.nazwa,
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
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = if (uiState.selectedCategory != null) "Wyszukaj wewnątrz kategorii..." else "Szukaj pieśni lub kategorii..."
            )
            Divider()

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.songResults.isEmpty() && uiState.categoryResults.isEmpty() && uiState.query.isNotBlank()) {
                NoResults()
            } else {
                SearchResultsContent(
                    categories = uiState.categoryResults,
                    songs = uiState.songResults,
                    isGlobalSearch = uiState.selectedCategory == null,
                    onCategoryClick = { viewModel.onCategorySelected(it) },
                    onNoCategoryClick = { viewModel.onNoCategorySelected() },
                    onSongClick = onNavigateToSong,
                    onSongLongClick = { viewModel.onSongLongPress(it) }
                )
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