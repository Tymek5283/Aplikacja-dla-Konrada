package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qjproject.liturgicalcalendar.data.Reading
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.theme.DividerColor
import com.qjproject.liturgicalcalendar.ui.theme.SongItemBackground
import com.qjproject.liturgicalcalendar.ui.theme.SubtleGrayBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*

@Composable
fun DayDetailsScreen(
    dayId: String?,
    onNavigateBack: () -> Unit
) {
    if (dayId.isNullOrBlank()) {
        Scaffold { padding -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Błąd krytyczny: Brak identyfikatora dnia.") } }
        return
    }

    val context = LocalContext.current
    val viewModel: DayDetailsViewModel = viewModel(factory = DayDetailsViewModelFactory(context, dayId))
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.isEditMode) { viewModel.onTryExitEditMode() }

    if (uiState.showConfirmExitDialog) {
        ConfirmExitDialog(
            onDismiss = { viewModel.dismissConfirmExitDialog() },
            onDiscard = { viewModel.onExitEditMode(save = false) },
            onSave = { viewModel.onExitEditMode(save = true) }
        )
    }

    when (val dialog = uiState.activeDialog) {
        is DialogState.ConfirmDelete -> ConfirmDeleteDialog(
            description = dialog.description,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { viewModel.deleteItem(dialog.item) }
        )
        is DialogState.AddEditSong -> AddEditSongDialog(
            moment = dialog.moment,
            existingSong = dialog.existingSong,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { song, moment, originalSong -> viewModel.addOrUpdateSong(song, moment, originalSong) }
        )
        else -> {}
    }

    Scaffold(
        topBar = {
            if (uiState.isEditMode) {
                EditModeTopAppBar(
                    title = uiState.dayData?.tytulDnia ?: "Edycja",
                    onCancelClick = { viewModel.onTryExitEditMode() },
                    onSaveClick = { viewModel.onExitEditMode(save = true) },
                    isSaveEnabled = uiState.hasChanges
                )
            } else {
                ViewModeTopAppBar(
                    title = uiState.dayData?.tytulDnia ?: "Ładowanie...",
                    onNavigateBack = onNavigateBack,
                    onEditClick = { viewModel.onEnterEditMode() }
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { Text("Błąd: ${uiState.error}") }
            uiState.isEditMode -> DayDetailsEditModeContent(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
            else -> DayDetailsViewModeContent(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
        }
    }
}

// =================================================================================
// WIDOK STANDARDOWY (VIEW MODE)
// =================================================================================
@Composable
private fun DayDetailsViewModeContent(modifier: Modifier = Modifier, viewModel: DayDetailsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val groupedSongs by viewModel.groupedSongs.collectAsState()
    var showSongModal by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<SuggestedSong?>(null) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val readingLayouts = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    val interactionSources = remember { mutableStateMapOf<Int, MutableInteractionSource>() }
    var topBarHeight by remember { mutableStateOf(0) }

    if (showSongModal && selectedSong != null) {
        SongDetailsModal(song = selectedSong!!, onDismiss = { showSongModal = false })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .onGloballyPositioned { topBarHeight = it.positionInRoot().y.toInt() }
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        HierarchicalCollapsibleSection(
            title = "Czytania",
            isExpanded = uiState.isReadingsSectionExpanded,
            onToggle = { viewModel.toggleReadingsSection() }
        ) {
            uiState.dayData?.czytania?.forEachIndexed { index, reading ->
                ReadingItemView(
                    reading = reading,
                    isExpanded = uiState.expandedReadings.contains(index),
                    onToggle = { viewModel.toggleReading(index) },
                    onContentDoubleTap = {
                        handleReadingCollapse(index, readingLayouts, viewModel, coroutineScope, scrollState, topBarHeight, interactionSources)
                    },
                    onGloballyPositioned = { coordinates -> readingLayouts[index] = coordinates },
                    interactionSource = interactionSources.getOrPut(index) { MutableInteractionSource() }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HierarchicalCollapsibleSection(
            title = "Sugerowane pieśni",
            isExpanded = uiState.isSongsSectionExpanded,
            onToggle = { viewModel.toggleSongsSection() }
        ) {
            songMomentOrderMap.forEach { (momentKey, momentName) ->
                SongGroupView(
                    momentName = momentName,
                    songs = groupedSongs[momentKey].orEmpty(),
                    isExpanded = uiState.expandedSongMoments.contains(momentKey),
                    onToggle = { viewModel.toggleSongMoment(momentKey) },
                    onSongClick = { song ->
                        selectedSong = song
                        showSongModal = true
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun handleReadingCollapse(
    index: Int, readingLayouts: Map<Int, LayoutCoordinates>, viewModel: DayDetailsViewModel,
    coroutineScope: CoroutineScope, scrollState: ScrollState, topBarHeight: Int,
    interactionSources: Map<Int, MutableInteractionSource>
) {
    val layoutCoordinates = readingLayouts[index] ?: return
    val isHeaderVisible = layoutCoordinates.positionInRoot().y >= topBarHeight

    viewModel.collapseReading(index)

    coroutineScope.launch {
        val triggerRipple = suspend {
            interactionSources[index]?.let { source ->
                val press = PressInteraction.Press(Offset.Zero)
                source.emit(press)
                delay(150)
                source.emit(PressInteraction.Release(press))
            }
        }
        if (!isHeaderVisible) {
            val currentScroll = scrollState.value
            val elementYPosInRoot = layoutCoordinates.positionInRoot().y
            val targetScrollPosition = currentScroll + elementYPosInRoot.toInt() - topBarHeight
            scrollState.animateScrollTo(targetScrollPosition)
            triggerRipple()
        } else {
            triggerRipple()
        }
    }
}

// =================================================================================
// WIDOK EDYCJI (EDIT MODE)
// =================================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayDetailsEditModeContent(modifier: Modifier = Modifier, viewModel: DayDetailsViewModel) {
    val editableData by viewModel.editableDayData
    var readingToEdit by remember { mutableStateOf<Pair<Reading, Int>?>(null) }
    var showAddReadingDialog by remember { mutableStateOf(false) }

    val reorderableState = rememberReorderableLazyListState(onMove = { from, to ->
        viewModel.reorderReadings(from.index, to.index)
    })

    if (showAddReadingDialog) {
        AddEditReadingDialog(
            onDismiss = { showAddReadingDialog = false },
            onConfirm = { reading -> viewModel.addOrUpdateReading(reading, null) }
        )
    }
    readingToEdit?.let { (reading, index) ->
        AddEditReadingDialog(
            existingReading = reading,
            onDismiss = { readingToEdit = null },
            onConfirm = { updatedReading -> viewModel.addOrUpdateReading(updatedReading, index) }
        )
    }

    LazyColumn(
        state = reorderableState.listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .reorderable(reorderableState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { SectionHeaderEditable("Czytania") }

        itemsIndexed(editableData?.czytania ?: emptyList(), key = { _, item -> item.hashCode() }) { index, reading ->
            ReorderableItem(reorderableState, key = reading.hashCode()) { isDragging ->
                EditableReadingItem(
                    reading = reading,
                    isDragging = isDragging,
                    onEditClick = { readingToEdit = reading to index },
                    onDeleteClick = { viewModel.showDialog(DialogState.ConfirmDelete(reading, "czytanie: ${reading.typ}")) },
                    modifier = Modifier.detectReorderAfterLongPress(reorderableState)
                )
            }
        }
        item { AddItemButton(text = "Dodaj czytanie", onClick = { showAddReadingDialog = true }) }
        item { Spacer(Modifier.height(16.dp)); SectionHeaderEditable("Sugerowane pieśni") }

        val songsByMoment = editableData?.piesniSugerowane.orEmpty().filterNotNull().groupBy { it.moment }
        songMomentOrderMap.forEach { (momentKey, momentName) ->
            item { EditableSongCategoryHeader(categoryName = momentName) }

            val songs = songsByMoment[momentKey].orEmpty()
            itemsIndexed(songs, key = { _, item -> item.hashCode() }) { index, song ->
                EditableSongItem(
                    song = song,
                    onEditClick = { viewModel.showDialog(DialogState.AddEditSong(momentKey, song)) },
                    onDeleteClick = { viewModel.showDialog(DialogState.ConfirmDelete(song, "pieśń: ${song.piesn}")) }
                )
            }
            item {
                AddItemButton(
                    text = "Dodaj pieśń do '${momentName}'",
                    onClick = { viewModel.showDialog(DialogState.AddEditSong(momentKey)) }
                )
            }
            if (momentKey != songMomentOrderMap.keys.last()) {
                item { Divider(Modifier.padding(vertical = 8.dp)) }
            }
        }
    }
}


// =================================================================================
// Komponenty reużywalne i pomocnicze
// =================================================================================
@Composable
private fun HierarchicalCollapsibleSection(
    title: String, isExpanded: Boolean, onToggle: () -> Unit, level: Int = 0,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.then(
            if (level > 0) Modifier.clip(MaterialTheme.shapes.medium).background(SubtleGrayBackground) else Modifier
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interactionSource, indication = rememberRipple(), onClick = onToggle)
                .padding(horizontal = if (level > 0) 12.dp else 0.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = if (level == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Zwiń" else "Rozwiń"
            )
        }
        if (level == 0) Divider(color = DividerColor)
        AnimatedVisibility(visible = isExpanded, enter = expandVertically(tween(300)), exit = shrinkVertically(tween(300))) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = if (level > 0) 12.dp else 0.dp, end = if (level > 0) 12.dp else 0.dp, bottom = if (level > 0) 8.dp else 0.dp).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) { content() }
        }
    }
}

@Composable
private fun ReadingItemView(
    reading: Reading, isExpanded: Boolean, onToggle: () -> Unit, onContentDoubleTap: () -> Unit,
    onGloballyPositioned: (LayoutCoordinates) -> Unit, interactionSource: MutableInteractionSource
) {
    HierarchicalCollapsibleSection(
        title = reading.typ, isExpanded = isExpanded, onToggle = onToggle, level = 1,
        modifier = Modifier.onGloballyPositioned(onGloballyPositioned), interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp).pointerInput(Unit) { detectTapGestures(onDoubleTap = { onContentDoubleTap() }) },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!reading.sigla.isNullOrBlank()) Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Sigla: ") }; append(reading.sigla) })
            if (!reading.opis.isNullOrBlank()) Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Opis: ") }; append(reading.opis) })
            Text(reading.tekst, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SongGroupView(momentName: String, songs: List<SuggestedSong>, isExpanded: Boolean, onToggle: () -> Unit, onSongClick: (SuggestedSong) -> Unit) {
    HierarchicalCollapsibleSection(title = momentName, isExpanded = isExpanded, onToggle = onToggle, level = 1) {
        if (songs.isEmpty()) {
            Text("Brak sugerowanych pieśni.", Modifier.padding(vertical = 8.dp, horizontal = 12.dp), style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
        } else {
            songs.forEach { song -> SongItemView(song = song, onClick = { onSongClick(song) }) }
        }
    }
}

@Composable
private fun SongItemView(song: SuggestedSong, onClick: () -> Unit) {
    // --- POCZĄTEK ZMIANY: Poprawka wywołania Card ---
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = SongItemBackground)
    ) {
        // --- KONIEC ZMIANY ---
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(song.piesn, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// =================================================================================
// Komponenty trybu edycji
// =================================================================================
@Composable fun SectionHeaderEditable(title: String) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
@Composable fun EditableSongCategoryHeader(categoryName: String) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } }

@Composable
fun EditableReadingItem(reading: Reading, isDragging: Boolean, onEditClick: () -> Unit, onDeleteClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().shadow(if (isDragging) 4.dp else 0.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DragHandle, "Zmień kolejność"); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(reading.typ, fontWeight = FontWeight.Bold)
                Text(reading.sigla ?: "", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
            IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Edytuj") }
            IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, "Usuń") }
        }
    }
}

@Composable
fun EditableSongItem(song: SuggestedSong, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DragHandle, "Zmień kolejność"); Spacer(Modifier.width(8.dp))
            Text(song.piesn, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Edytuj") }
            IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, "Usuń") }
        }
    }
}

@Composable fun AddItemButton(text: String, onClick: () -> Unit) { Button(onClick, Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = MaterialTheme.shapes.medium) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(text) } }

// =================================================================================
// Okna Dialogowe
// =================================================================================
@Composable private fun ConfirmExitDialog(onDismiss: () -> Unit, onDiscard: () -> Unit, onSave: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Niezapisane zmiany") }, text = { Text("Czy chcesz zapisać zmiany przed wyjściem?") }, dismissButton = { TextButton(onClick = onDiscard) { Text("Odrzuć") } }, confirmButton = { Button(onClick = onSave) { Text("Zapisz") } }) }
@Composable private fun ConfirmDeleteDialog(description: String, onDismiss: () -> Unit, onConfirm: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Potwierdź usunięcie") }, text = { Text("Czy na pewno chcesz usunąć $description?") }, dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Usuń") } }) }

@Composable
private fun AddEditReadingDialog(existingReading: Reading? = null, onDismiss: () -> Unit, onConfirm: (Reading) -> Unit) {
    var typ by remember { mutableStateOf(existingReading?.typ ?: "") }
    var sigla by remember { mutableStateOf(existingReading?.sigla ?: "") }
    var opis by remember { mutableStateOf(existingReading?.opis ?: "") }
    var tekst by remember { mutableStateOf(existingReading?.tekst ?: "") }
    val isTypValid by remember { derivedStateOf { typ.isNotBlank() } }

    Dialog(onDismissRequest = onDismiss) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Text(if (existingReading == null) "Dodaj nowe czytanie" else "Edytuj czytanie", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(16.dp))
        OutlinedTextField(typ, { typ = it }, label = { Text("Typ*") }, isError = !isTypValid)
        OutlinedTextField(sigla, { sigla = it }, label = { Text("Sigla") })
        OutlinedTextField(opis, { opis = it }, label = { Text("Opis") })
        OutlinedTextField(tekst, { tekst = it }, label = { Text("Tekst") }, modifier = Modifier.height(200.dp)); Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Anuluj") }; Spacer(Modifier.width(8.dp))
            Button(onClick = { onConfirm(existingReading?.copy(typ = typ, sigla = sigla, opis = opis, tekst = tekst) ?: Reading(typ, sigla, opis, tekst)); onDismiss() }, enabled = isTypValid) { Text("Zapisz") }
        }
    }}}
}

@Composable
private fun AddEditSongDialog(
    moment: String, existingSong: SuggestedSong? = null, viewModel: DayDetailsViewModel, onDismiss: () -> Unit,
    onConfirm: (SuggestedSong, String, SuggestedSong?) -> Unit
) {
    var piesn by remember { mutableStateOf(existingSong?.piesn ?: "") }
    var numer by remember { mutableStateOf(existingSong?.numer ?: "") }
    var opis by remember { mutableStateOf(existingSong?.opis ?: "") }
    val isPiesnValid by remember { derivedStateOf { piesn.isNotBlank() } }
    val searchResults by viewModel.songSearchResults.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Text(if (existingSong == null) "Dodaj nową pieśń" else "Edytuj pieśń", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(16.dp))
        OutlinedTextField(piesn, { piesn = it; viewModel.searchSongs(it) }, label = { Text("Tytuł pieśni*") }, isError = !isPiesnValid, modifier = Modifier.focusRequester(focusRequester))
        if (searchResults.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp).border(1.dp, Color.Gray)) {
                items(searchResults) { song ->
                    Text(song.tytul, Modifier.fillMaxWidth().clickable { piesn = song.tytul; numer = song.numer; viewModel.searchSongs("") }.padding(8.dp))
                }
            }
        }
        OutlinedTextField(numer, { numer = it }, label = { Text("Numer") })
        OutlinedTextField(opis, { opis = it }, label = { Text("Opis") }, modifier = Modifier.height(150.dp)); Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Anuluj") }; Spacer(Modifier.width(8.dp))
            Button(onClick = { onConfirm(SuggestedSong(numer, piesn, opis, moment), moment, existingSong); onDismiss() }, enabled = isPiesnValid) { Text("Zapisz") }
        }
    }}}
}

@Composable
private fun SongDetailsModal(song: SuggestedSong, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) { Card(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp)) { Column(Modifier.padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(song.piesn, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Zamknij") }
        }
        Spacer(Modifier.height(16.dp)); Divider(); Spacer(Modifier.height(16.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (song.numer.isNotBlank()) {
                Text(buildAnnotatedString {
                    append(AnnotatedString("Numer w Siedlecki: ", spanStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold).toSpanStyle()))
                    append(AnnotatedString(song.numer, spanStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold).toSpanStyle()))
                }); Spacer(Modifier.height(16.dp))
            }
            if (song.opis.isNotBlank()) {
                Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Opis:\n") }; append(song.opis) }, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }}}
}


// =================================================================================
// Paski nawigacyjne (Top App Bars)
// =================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeTopAppBar(title: String, onNavigateBack: () -> Unit, onEditClick: () -> Unit) {
    Column {
        CenterAlignedTopAppBar(
            title = { AutoResizingText(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć") } },
            actions = { IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Edytuj dzień") } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary, navigationIconContentColor = MaterialTheme.colorScheme.primary)
        ); Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditModeTopAppBar(title: String, onCancelClick: () -> Unit, onSaveClick: () -> Unit, isSaveEnabled: Boolean) {
    Column {
        CenterAlignedTopAppBar(
            title = { AutoResizingText(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) },
            navigationIcon = { IconButton(onClick = onCancelClick) { Icon(Icons.Default.Close, "Anuluj edycję") } },
            actions = { IconButton(onClick = onSaveClick, enabled = isSaveEnabled) { Icon(Icons.Default.Check, "Zapisz zmiany", tint = if (isSaveEnabled) MaterialTheme.colorScheme.primary else Color.Gray) } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary, navigationIconContentColor = MaterialTheme.colorScheme.primary)
        ); Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    }
}