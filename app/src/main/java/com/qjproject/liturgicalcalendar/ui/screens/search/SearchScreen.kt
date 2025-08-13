package com.qjproject.liturgicalcalendar.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.models.SearchResult

@Composable
fun SearchScreen(
    onNavigateToDay: (String) -> Unit,
    onNavigateToSong: (String) -> Unit,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory(context))
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showAddSongDialog) {
        AddSongDialog(
            error = uiState.addSongError,
            onDismiss = { viewModel.onDismissAddSongDialog() },
            onConfirm = { title, number, text ->
                viewModel.saveNewSong(title, number, text)
            },
            onValidate = { title, number ->
                viewModel.validateSongInput(title, number)
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

    Scaffold(
        floatingActionButton = {
            if (uiState.searchMode == SearchMode.Pieśni) {
                FloatingActionButton(onClick = { viewModel.onAddSongClicked() }) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj pieśń")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchModeTabs(
                    selectedMode = uiState.searchMode,
                    onModeSelected = viewModel::onSearchModeChange,
                    modifier = Modifier.weight(1f)
                )

                AnimatedVisibility(visible = uiState.searchMode == SearchMode.Pieśni) {
                    SongOptions(
                        showMenu = showMenu,
                        onShowMenuChange = onShowMenuChange,
                        searchInTitle = uiState.searchInTitle,
                        searchInContent = uiState.searchInContent,
                        sortMode = uiState.sortMode,
                        onSearchInTitleChange = viewModel::onSearchInTitleChange,
                        onSearchInContentChange = viewModel::onSearchInContentChange,
                        onSortModeChange = viewModel::onSortModeChange,
                        showSortOption = uiState.results.isNotEmpty()
                    )
                }
            }
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
                        onNavigateToDay = onNavigateToDay,
                        onNavigateToSong = onNavigateToSong,
                        onSongLongClick = { song -> viewModel.onSongLongPress(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { Text("Wyszukaj...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ikona wyszukiwania") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}


@Composable
fun SearchModeTabs(selectedMode: SearchMode, onModeSelected: (SearchMode) -> Unit, modifier: Modifier = Modifier) {
    TabRow(
        selectedTabIndex = selectedMode.ordinal,
        modifier = modifier
    ) {
        SearchMode.values().forEach { mode ->
            Tab(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                text = { Text(mode.name) }
            )
        }
    }
}

@Composable
fun SongOptions(
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    searchInTitle: Boolean,
    searchInContent: Boolean,
    sortMode: SongSortMode,
    onSearchInTitleChange: (Boolean) -> Unit,
    onSearchInContentChange: (Boolean) -> Unit,
    onSortModeChange: (SongSortMode) -> Unit,
    showSortOption: Boolean
) {
    Box(contentAlignment = Alignment.Center) {
        IconButton(onClick = { onShowMenuChange(true) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Więcej opcji")
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onShowMenuChange(false) }
        ) {
            Text("Szukaj w:", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
            DropdownMenuItem(
                text = { Text("Tytuł") },
                onClick = { onSearchInTitleChange(!searchInTitle) },
                leadingIcon = { Checkbox(checked = searchInTitle, onCheckedChange = null) }
            )
            DropdownMenuItem(
                text = { Text("Treść") },
                onClick = { onSearchInContentChange(!searchInContent) },
                leadingIcon = { Checkbox(checked = searchInContent, onCheckedChange = null) }
            )
            if (showSortOption) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Sortuj:", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                Column(Modifier.selectableGroup()) {
                    SongSortMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.name) },
                            onClick = { onSortModeChange(mode) },
                            leadingIcon = {
                                RadioButton(
                                    selected = (sortMode == mode),
                                    onClick = null
                                )
                            },
                            modifier = Modifier.selectable(
                                selected = (sortMode == mode),
                                onClick = { onSortModeChange(mode) },
                                role = Role.RadioButton
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoResults() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Dla tej frazy nie znaleziono żadnego dopasowania.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ResultsList(
    results: List<SearchResult>,
    onNavigateToDay: (String) -> Unit,
    onNavigateToSong: (String) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results, key = { result ->
            when (result) {
                is SearchResult.DayResult -> "day_${result.path}"
                is SearchResult.SongResult -> "song_${result.song.numer}_${result.song.tytul.hashCode()}"
            }
        }) { result ->
            when (result) {
                is SearchResult.DayResult -> DayResultItem(result = result, onClick = { onNavigateToDay(result.path) })
                is SearchResult.SongResult -> SongResultItem(
                    result = result,
                    onClick = { onNavigateToSong(result.song.numer) },
                    onLongClick = { onSongLongClick(result.song) }
                )
            }
        }
    }
}

@Composable
fun DayResultItem(result: SearchResult.DayResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Article, contentDescription = "Dzień", tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(result.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongResultItem(
    result: SearchResult.SongResult,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.MusicNote, contentDescription = "Pieśń", tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(result.song.tytul, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Text(result.song.numer, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddSongDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, number: String, text: String) -> Unit,
    onValidate: (title: String, number: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    LaunchedEffect(title, number) {
        onValidate(title, number)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Dodaj nową pieśń", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp))
                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tytuł*") },
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Numer*") },
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Tekst") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                    if (error != null) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(title, number, text) },
                        enabled = title.isNotBlank() && number.isNotBlank() && error == null
                    ) {
                        Text("Zapisz")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    song: Song,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Usunąć pieśń?", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp))
                Spacer(Modifier.height(16.dp))
                Text("Czy na pewno chcesz usunąć pieśń '${song.tytul}'?")
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Usuń")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteOccurrencesDialog(
    song: Song,
    onConfirmDeleteAll: () -> Unit,
    onConfirmDeleteOne: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Usunąć powiązania?", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp))
                Spacer(Modifier.height(16.dp))
                Text("Czy chcesz również usunąć pieśń '${song.tytul}' ze wszystkich list sugerowanych pieśni w dniach liturgicznych?")
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onConfirmDeleteOne,
                    ) {
                        Text("Nie, dziękuję")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirmDeleteAll,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Tak, usuń")
                    }
                }
            }
        }
    }
}