package com.qjproject.liturgicalcalendar.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    val rootListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val nestedListState = rememberSaveable(uiState.selectedCategory?.nazwa, uiState.selectedTag, saver = LazyListState.Saver) { LazyListState() }

    LaunchedEffect(uiState.resetToTopEventId) {
        if (uiState.resetToTopEventId > 0) {
            rootListState.scrollToItem(0)
        }
    }

    if (uiState.showAddSongDialog) {
        AddSongDialog(
            categories = uiState.allCategories,
            error = uiState.addSongError,
            initialCategoryName = uiState.selectedCategory?.nazwa,
            preselectedTag = uiState.selectedTag,
            suffixes = viewModel.getAllNumberSuffixes(),
            onDismiss = { viewModel.onDismissAddSongDialog() },
            onConfirm = { title, siedl, sak, dn, sak2020, extras, text, category ->
                viewModel.saveNewSong(title, siedl, sak, dn, sak2020, extras, text, category, uiState.selectedTag)
            },
            onValidate = { title, siedl, sak, dn, sak2020, extras ->
                viewModel.validateSongInput(title, siedl, sak, dn, sak2020, extras)
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
                placeholder = when {
                    uiState.selectedCategory != null -> "Wyszukaj wewnątrz kategorii..."
                    uiState.selectedTag != null -> "Wyszukaj wewnątrz tagu..."
                    else -> "Szukaj pieśni, kategorii lub tagów..."
                }
            )
            Divider()

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.songResults.isEmpty() && uiState.categoryResults.isEmpty() && uiState.tagResults.isEmpty() && uiState.query.isNotBlank()) {
                NoResults()
            } else if (uiState.selectedTag != null && uiState.songResults.isEmpty()) {
                NoSongsForTag(
                    tagName = uiState.selectedTag!!,
                    onAddSong = { viewModel.onAddSongClicked() }
                )
            } else {
                SearchResultsContent(
                    categories = uiState.categoryResults,
                    tags = uiState.tagResults,
                    songs = uiState.songResults,
                    isGlobalSearch = uiState.selectedCategory == null && uiState.selectedTag == null,
                    onCategoryClick = { viewModel.onCategorySelected(it) },
                    onTagClick = { viewModel.onTagSelected(it) },
                    onNoCategoryClick = { viewModel.onNoCategorySelected() },
                    onSongClick = { song ->
                        viewModel.onSongOpened(song)
                        onNavigateToSong(song)
                    },
                    onSongLongClick = { viewModel.onSongLongPress(it) },
                    hasMore = uiState.hasMore,
                    isLoadingMore = uiState.isLoadingMore,
                    onLoadMore = { viewModel.loadMoreResults() },
                    searchQuery = uiState.query,
                    state = if (uiState.selectedCategory == null && uiState.selectedTag == null) rootListState else nestedListState
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