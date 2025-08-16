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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToSong: (Song) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    // --- POCZĄTEK ZMIANY ---
    val lifecycleOwner = LocalLifecycleOwner.current

    // Odświeża wyniki wyszukiwania po powrocie na ekran, aby odzwierciedlić zmiany
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
    // --- KONIEC ZMIANY ---

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

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
    results: List<Song>,
    onNavigateToSong: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results, key = { song ->
            "song_${song.numerSiedl}_${song.tytul.hashCode()}"
        }) { song ->
            SongResultItem(
                song = song,
                onClick = { onNavigateToSong(song) },
                onLongClick = { onSongLongClick(song) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongResultItem(
    song: Song,
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
            Text(song.tytul, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Text(song.kategoriaSkr, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongDialog(
    categories: List<Category>,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, siedl: String, sak: String, dn: String, text: String, category: String) -> Unit,
    onValidate: (title: String, siedl: String, sak: String, dn: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var numerSiedl by remember { mutableStateOf("") }
    var numerSak by remember { mutableStateOf("") }
    var numerDn by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    val isAnyNumberPresent by remember { derivedStateOf { numerSiedl.isNotBlank() || numerSak.isNotBlank() || numerDn.isNotBlank() } }


    LaunchedEffect(title, numerSiedl, numerSak, numerDn) {
        onValidate(title, numerSiedl, numerSak, numerDn)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Dodaj nową pieśń",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tytuł") },
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = numerSiedl,
                        onValueChange = { numerSiedl = it },
                        label = { Text("Numer Siedlecki") },
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = numerSak,
                        onValueChange = { numerSak = it },
                        label = { Text("Numer ŚAK") },
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = numerDn,
                        onValueChange = { numerDn = it },
                        label = { Text("Numer DN") },
                        isError = error != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = isCategoryExpanded,
                        onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            label = { Text("Kategoria") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryExpanded,
                            onDismissRequest = { isCategoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Brak") },
                                onClick = {
                                    category = ""
                                    isCategoryExpanded = false
                                }
                            )
                            categories.forEach { categoryItem ->
                                DropdownMenuItem(
                                    text = { Text(categoryItem.nazwa) },
                                    onClick = {
                                        category = categoryItem.nazwa
                                        isCategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
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
                        onClick = { onConfirm(title, numerSiedl, numerSak, numerDn, text, category) },
                        enabled = title.isNotBlank() && isAnyNumberPresent && error == null
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
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Usunąć pieśń?",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text(
                    buildAnnotatedString {
                        append("Czy na pewno chcesz usunąć pieśń ")
                        withStyle(style = SpanStyle(color = SaturatedNavy, fontWeight = FontWeight.Bold)) {
                            append(song.tytul)
                        }
                        append("?")
                    }
                )
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
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Usunąć powiązania?",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text(
                    buildAnnotatedString {
                        append("Czy chcesz również usunąć pieśń ")
                        withStyle(style = SpanStyle(color = SaturatedNavy, fontWeight = FontWeight.Bold)) {
                            append(song.tytul)
                        }
                        append(" ze wszystkich list sugerowanych pieśni w dniach liturgicznych?")
                    }
                )
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