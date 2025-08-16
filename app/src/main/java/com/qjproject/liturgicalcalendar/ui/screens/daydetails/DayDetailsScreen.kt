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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qjproject.liturgicalcalendar.data.Reading
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*

@Composable
fun DayDetailsScreen(
    dayId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToSongContent: (song: Song, startInEdit: Boolean) -> Unit
) {
    if (dayId.isNullOrBlank()) {
        Scaffold { padding -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Błąd krytyczny: Brak identyfikatora dnia.") } }
        return
    }

    val context = LocalContext.current
    val viewModel: DayDetailsViewModel = viewModel(factory = DayDetailsViewModelFactory(context, dayId))
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.isEditMode) { viewModel.onTryExitEditMode() }
    BackHandler(enabled = !uiState.isEditMode, onBack = onNavigateBack)

    if (uiState.showConfirmExitDialog) {
        ConfirmExitDialog(
            onDismiss = { viewModel.dismissConfirmExitDialog() },
            onDiscard = { viewModel.onExitEditMode(save = false) }
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
            else -> DayDetailsViewModeContent(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel,
                onNavigateToSongContent = onNavigateToSongContent
            )
        }
    }
}

// =================================================================================
// WIDOK STANDARDOWY (VIEW MODE)
// =================================================================================
@Composable
private fun DayDetailsViewModeContent(
    modifier: Modifier = Modifier,
    viewModel: DayDetailsViewModel,
    onNavigateToSongContent: (song: Song, startInEdit: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSongModal by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<SuggestedSong?>(null) }
    var fullSelectedSong by remember { mutableStateOf<Song?>(null) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val readingOffsetsY = remember { mutableStateMapOf<Int, Int>() }
    val interactionSources = remember { mutableStateMapOf<Int, MutableInteractionSource>() }
    var contentStartY by remember { mutableStateOf(0f) }
    var scrollAnchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    if (showSongModal && selectedSong != null && fullSelectedSong != null) {
        SongDetailsModal(
            suggestedSong = selectedSong!!,
            fullSong = fullSelectedSong!!,
            onDismiss = { showSongModal = false },
            onShowContent = onNavigateToSongContent
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .onGloballyPositioned { contentStartY = it.positionInRoot().y }
    ) {
        Spacer(
            modifier = Modifier
                .height(0.dp)
                .onGloballyPositioned { scrollAnchorCoordinates = it }
        )

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
                        handleReadingCollapse(index, readingOffsetsY, viewModel, coroutineScope, scrollState, interactionSources)
                    },
                    onGloballyPositioned = { itemCoordinates ->
                        scrollAnchorCoordinates?.let { anchor ->
                            val offsetY = (itemCoordinates.positionInRoot().y - anchor.positionInRoot().y).toInt()
                            readingOffsetsY[index] = offsetY
                        }
                    },
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
            val groupedSongs by viewModel.groupedSongs.collectAsState()
            songMomentOrderMap.forEach { (momentKey, momentName) ->
                SongGroupView(
                    momentName = momentName,
                    songs = groupedSongs[momentKey].orEmpty(),
                    isExpanded = uiState.expandedSongMoments.contains(momentKey),
                    onToggle = { viewModel.toggleSongMoment(momentKey) },
                    onSongClick = { suggested ->
                        viewModel.getFullSong(suggested) { fullSong ->
                            if (fullSong != null) {
                                selectedSong = suggested
                                fullSelectedSong = fullSong
                                showSongModal = true
                            }
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun handleReadingCollapse(
    index: Int,
    readingOffsets: Map<Int, Int>,
    viewModel: DayDetailsViewModel,
    coroutineScope: CoroutineScope,
    scrollState: ScrollState,
    interactionSources: Map<Int, MutableInteractionSource>
) {
    val targetScrollPosition = readingOffsets[index] ?: return

    viewModel.collapseReading(index)

    coroutineScope.launch {
        scrollState.animateScrollTo(targetScrollPosition)

        delay(50)
        interactionSources[index]?.let { source ->
            val press = PressInteraction.Press(Offset.Zero)
            source.emit(press)
            delay(150)
            source.emit(PressInteraction.Release(press))
        }
    }
}

// =================================================================================
// WIDOK EDYCJI (EDIT MODE)
// =================================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayDetailsEditModeContent(modifier: Modifier = Modifier, viewModel: DayDetailsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val editableData by viewModel.editableDayData
    var readingToEdit by remember { mutableStateOf<Pair<Reading, Int>?>(null) }
    var showAddReadingDialog by remember { mutableStateOf(false) }

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
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            HierarchicalCollapsibleSection(
                title = "Czytania",
                isExpanded = uiState.isReadingsSectionExpanded,
                onToggle = { viewModel.toggleReadingsSection() }
            ) {
                val reorderState = rememberReorderableLazyListState(
                    onMove = { from, to -> viewModel.reorderReadings(from.index, to.index) }
                )
                LazyColumn(
                    state = reorderState.listState,
                    modifier = Modifier
                        .reorderable(reorderState)
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = editableData?.czytania ?: emptyList(),
                        key = { index, item -> "${item.hashCode()}-$index" }
                    ) { index, reading ->
                        ReorderableItem(reorderState, key = "${reading.hashCode()}-$index") { isDragging ->
                            EditableReadingItem(
                                reading = reading,
                                isDragging = isDragging,
                                onEditClick = { readingToEdit = reading to index },
                                onDeleteClick = { viewModel.showDialog(DialogState.ConfirmDelete(reading, "czytanie: ${reading.typ}")) },
                                reorderModifier = Modifier.detectReorder(reorderState)
                            )
                        }
                    }
                }
                AddItemButton(text = "Dodaj czytanie", onClick = { showAddReadingDialog = true })
            }
        }

        item {
            HierarchicalCollapsibleSection(
                title = "Sugerowane pieśni",
                isExpanded = uiState.isSongsSectionExpanded,
                onToggle = { viewModel.toggleSongsSection() }
            ) {
                val songsByMoment = editableData?.piesniSugerowane.orEmpty().filterNotNull().groupBy { it.moment }
                songMomentOrderMap.forEach { (momentKey, momentName) ->
                    EditableSongCategoryHeader(categoryName = momentName)
                    val songsInMoment = songsByMoment[momentKey].orEmpty()
                    val isReorderEnabled = songsInMoment.size > 1

                    val songReorderState = rememberReorderableLazyListState(
                        onMove = { from, to -> viewModel.reorderSongs(momentKey, from.index, to.index) }
                    )
                    LazyColumn(
                        state = songReorderState.listState,
                        modifier = Modifier
                            .then(if (isReorderEnabled) Modifier.reorderable(songReorderState) else Modifier)
                            .heightIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = songsInMoment,
                            key = { index, song -> "${song.hashCode()}-$index" }
                        ) { index, song ->
                            ReorderableItem(songReorderState, key = "${song.hashCode()}-$index") { isDragging ->
                                EditableSongItem(
                                    song = song,
                                    isDragging = isDragging,
                                    onEditClick = { viewModel.showDialog(DialogState.AddEditSong(momentKey, song)) },
                                    onDeleteClick = { viewModel.showDialog(DialogState.ConfirmDelete(song, "pieśń: ${song.piesn}")) },
                                    reorderModifier = if (isReorderEnabled) Modifier.detectReorder(songReorderState) else Modifier,
                                    isReorderEnabled = isReorderEnabled
                                )
                            }
                        }
                    }
                    AddItemButton(text = "Dodaj pieśń do '$momentName'", onClick = { viewModel.showDialog(DialogState.AddEditSong(momentKey)) })
                    if (momentKey != songMomentOrderMap.keys.last()) {
                        Divider(Modifier.padding(vertical = 8.dp))
                    }
                }
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
            if (level > 0) Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(SubtleGrayBackground) else Modifier
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
        if (level == 0 && isExpanded) Divider(color = DividerColor)
        AnimatedVisibility(visible = isExpanded, enter = expandVertically(tween(300)), exit = shrinkVertically(tween(300))) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (level > 0) 12.dp else 0.dp, end = if (level > 0) 12.dp else 0.dp, bottom = if (level > 0) 8.dp else 0.dp)
                    .padding(top = 4.dp),
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
            modifier = Modifier
                .padding(vertical = 8.dp)
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onContentDoubleTap() }) },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!reading.sigla.isNullOrBlank()) Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Sigla: ") }; append(
                reading.sigla
            )
            })
            if (!reading.opis.isNullOrBlank()) Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Opis: ") }; append(
                reading.opis
            )
            })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongItemView(song: SuggestedSong, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = SongItemBackground)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(song.piesn, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic))
        }
    }
}

// =================================================================================
// Komponenty trybu edycji
// =================================================================================
@Composable fun SectionHeaderEditable(title: String) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
@Composable fun EditableSongCategoryHeader(categoryName: String) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } }

@Composable
fun EditableReadingItem(reading: Reading, isDragging: Boolean, onEditClick: () -> Unit, onDeleteClick: () -> Unit, reorderModifier: Modifier) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isDragging) 4.dp else 0.dp)
            .clickable(onClick = onEditClick),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DragHandle, "Zmień kolejność", modifier = reorderModifier)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(reading.typ, fontWeight = FontWeight.Bold)
                Text(reading.sigla ?: "", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
            IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, "Usuń", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun EditableSongItem(song: SuggestedSong, isDragging: Boolean, onEditClick: () -> Unit, onDeleteClick: () -> Unit, reorderModifier: Modifier, isReorderEnabled: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isDragging) 4.dp else 0.dp)
            .clickable(onClick = onEditClick),
        colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
    ) {
        Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isReorderEnabled) {
                Icon(Icons.Default.DragHandle, "Zmień kolejność", modifier = reorderModifier)
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.width(32.dp)) // Placeholder to keep alignment
            }
            Text(song.piesn, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, "Usuń", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable fun AddItemButton(text: String, onClick: () -> Unit) { Button(onClick, Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = MaterialTheme.shapes.medium) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(text) } }

// =================================================================================
// Okna Dialogowe
// =================================================================================
@Composable
private fun ConfirmExitDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Odrzucić zmiany?",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text("Czy na pewno chcesz wyjść bez zapisywania zmian?", color = Color.White)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDiscard) {
                        Text("Odrzuć", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onDismiss) { Text("Anuluj") }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(description: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Potwierdź usunięcie",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                val (prefix, itemName) = remember(description) {
                    val parts: List<String> = description.split(":", limit = 2)
                    if (parts.size == 2) {
                        // Poprawka: Wywołaj .trim() na elementach listy (parts[0] i parts[1]), a nie na samej liście.
                        Pair(parts[0].trim() + ":", parts[1].trim())
                    } else {
                        Pair("", description.trim())
                    }
                }

                Text(
                    buildAnnotatedString {
                        append("Czy na pewno chcesz usunąć ")
                        append(prefix)
                        if (prefix.isNotEmpty()) {
                            append(" ")
                        }
                        withStyle(style = SpanStyle(color = SaturatedNavy, fontWeight = FontWeight.Bold)) {
                            append(itemName)
                        }
                        append("?")
                    },
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Usuń") }
                }
            }
        }
    }
}

@Composable
private fun AddEditReadingDialog(existingReading: Reading? = null, onDismiss: () -> Unit, onConfirm: (Reading) -> Unit) {
    var typ by remember { mutableStateOf(existingReading?.typ ?: "") }
    var sigla by remember { mutableStateOf(existingReading?.sigla ?: "") }
    var opis by remember { mutableStateOf(existingReading?.opis ?: "") }
    var tekst by remember { mutableStateOf(existingReading?.tekst ?: "") }
    val isTypValid by remember { derivedStateOf { typ.isNotBlank() } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                    if (existingReading == null) "Dodaj nowe czytanie" else "Edytuj czytanie",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = SaturatedNavy
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(typ, { typ = it }, label = { Text("Typ*") }, isError = !isTypValid, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(sigla, { sigla = it }, label = { Text("Sigla") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(opis, { opis = it }, label = { Text("Opis") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(tekst, { tekst = it }, label = { Text("Tekst") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }; Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(existingReading?.copy(typ = typ, sigla = sigla, opis = opis, tekst = tekst) ?: Reading(typ, sigla, opis, tekst)); onDismiss() }, enabled = isTypValid) { Text("Zapisz") }
                }
            }
        }
    }
}

@Composable
private fun AddEditSongDialog(
    moment: String,
    existingSong: SuggestedSong? = null,
    viewModel: DayDetailsViewModel,
    onDismiss: () -> Unit,
    onConfirm: (SuggestedSong, String, SuggestedSong?) -> Unit
) {
    var piesn by remember { mutableStateOf(existingSong?.piesn ?: "") }
    var numer by remember { mutableStateOf(existingSong?.numer ?: "") }
    var opis by remember { mutableStateOf(existingSong?.opis ?: "") }
    val isPiesnValid by remember { derivedStateOf { piesn.isNotBlank() } }

    val titleSearchResults by viewModel.songTitleSearchResults.collectAsState()
    val numberSearchResults by viewModel.songNumberSearchResults.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.clearAllSearchResults()
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (existingSong == null) "Dodaj nową pieśń" else "Edytuj pieśń",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = SaturatedNavy
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                Column {
                    OutlinedTextField(
                        value = piesn,
                        onValueChange = {
                            piesn = it
                            viewModel.searchSongsByTitle(it)
                        },
                        label = { Text("Tytuł pieśni*") },
                        isError = !isPiesnValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    AnimatedVisibility(visible = titleSearchResults.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 240.dp)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            items(
                                items = titleSearchResults,
                                key = { song -> "${song.tytul}-${song.numerSiedl}" }
                            ) { song ->
                                Text(
                                    text = viewModel.formatSongSuggestion(song),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            piesn = song.tytul
                                            numer = song.numerSiedl
                                            viewModel.clearAllSearchResults()
                                        }
                                        .padding(12.dp),
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column {
                    OutlinedTextField(
                        value = numer,
                        onValueChange = {
                            numer = it
                            viewModel.searchSongsByNumber(it)
                        },
                        label = { Text("Numer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(visible = numberSearchResults.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 240.dp)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            items(
                                items = numberSearchResults,
                                key = { song -> "${song.tytul}-${song.numerSiedl}" }
                            ) { song ->
                                Text(
                                    text = viewModel.formatSongSuggestion(song),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            piesn = song.tytul
                                            numer = song.numerSiedl
                                            viewModel.clearAllSearchResults()
                                        }
                                        .padding(12.dp),
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(opis, { opis = it }, label = { Text("Opis") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        viewModel.clearAllSearchResults()
                        onDismiss()
                    }) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(SuggestedSong(numer, piesn, opis, moment), moment, existingSong)
                            onDismiss()
                        },
                        enabled = isPiesnValid
                    ) { Text("Zapisz") }
                }
            }
        }
    }
}


@Composable
private fun SongDetailsModal(
    suggestedSong: SuggestedSong,
    fullSong: Song,
    onDismiss: () -> Unit,
    onShowContent: (song: Song, startInEdit: Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(
                        suggestedSong.piesn,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        color = SaturatedNavy
                    )
                    IconButton(onClick = onDismiss, Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Zamknij") }
                }
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    SongNumberInfo("Siedlecki:", fullSong.numerSiedl)
                    SongNumberInfo("ŚAK:", fullSong.numerSAK)
                    SongNumberInfo("DN:", fullSong.numerDN)

                    if (suggestedSong.opis.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Opis:\n") }
                                append(suggestedSong.opis)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        onShowContent(fullSong, false)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Treść")
                }
            }
        }
    }
}

@Composable
private fun SongNumberInfo(label: String, number: String) {
    val displayValue = number.ifBlank { "-" }
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
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