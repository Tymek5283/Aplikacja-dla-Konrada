// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/DayDetailsScreen/DayDetailsEditMode.kt
// Opis: Ten plik zawiera wszystkie komponenty odpowiedzialne za wyświetlanie i obsługę zawartości ekranu DayDetailsScreen w trybie edycji.

package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.data.Reading
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VividRed
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DayDetailsEditModeContent(modifier: Modifier = Modifier, viewModel: DayDetailsViewModel) {
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            HierarchicalCollapsibleSection(
                title = "Czytania",
                isExpanded = uiState.isReadingsSectionExpanded,
                onToggle = { viewModel.toggleReadingsSection() }
            ) {
                val readings = editableData?.czytania ?: emptyList()
                val readingsReorderState = rememberReorderableLazyListState(
                    onMove = { from, to -> viewModel.reorderReadings(from.index, to.index) }
                )

                val readingItemHeight = 70.dp
                val itemSpacing = 8.dp
                val readingsTotalHeight = (readingItemHeight * readings.size) + (itemSpacing * (readings.size - 1).coerceAtLeast(0))

                LazyColumn(
                    state = readingsReorderState.listState,
                    modifier = Modifier
                        .reorderable(readingsReorderState)
                        .height(readingsTotalHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    itemsIndexed(
                        items = readings,
                        key = { index, item -> "${item.hashCode()}-$index" }
                    ) { index, reading ->
                        ReorderableItem(readingsReorderState, key = "${reading.hashCode()}-$index") { isDragging ->
                            EditableReadingItem(
                                reading = reading,
                                isDragging = isDragging,
                                onEditClick = { readingToEdit = reading to index },
                                onDeleteClick = { viewModel.showDialog(DialogState.ConfirmDelete(reading, "czytanie: ${reading.typ}")) },
                                reorderModifier = Modifier.detectReorder(readingsReorderState)
                            )
                        }
                    }
                }
                AddItemButton(text = "Dodaj czytanie", onClick = { showAddReadingDialog = true })
            }

            Spacer(modifier = Modifier.height(16.dp))

            HierarchicalCollapsibleSection(
                title = "Sugerowane pieśni",
                isExpanded = uiState.isSongsSectionExpanded,
                onToggle = { viewModel.toggleSongsSection() }
            ) {
                val reorderableSongList by viewModel.reorderableSongList.collectAsState()
                val songReorderState = rememberReorderableLazyListState(
                    onMove = { from, to -> viewModel.reorderSongs(from, to) },
                    canDragOver = { draggedOver, _ ->
                        reorderableSongList.getOrNull(draggedOver.index) is ReorderableListItem.SongItem
                    }
                )

                LazyColumn(
                    state = songReorderState.listState,
                    modifier = Modifier
                        .reorderable(songReorderState)
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = true
                ) {
                    items(
                        items = reorderableSongList,
                        key = { item ->
                            when (item) {
                                is ReorderableListItem.HeaderItem -> "header_${item.momentKey}"
                                is ReorderableListItem.SongItem -> "song_${item.suggestedSong.piesn}_${item.suggestedSong.numer}_${item.suggestedSong.moment}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is ReorderableListItem.HeaderItem -> {
                                Column(modifier = Modifier.animateItemPlacement(tween(250))) {
                                    EditableSongCategoryHeader(categoryName = item.momentName)
                                    AddItemButton(
                                        text = "Dodaj pieśń do '${item.momentName}'",
                                        onClick = { viewModel.showDialog(DialogState.AddEditSong(item.momentKey)) })
                                }
                            }
                            is ReorderableListItem.SongItem -> {
                                ReorderableItem(
                                    reorderableState = songReorderState,
                                    key = "song_${item.suggestedSong.piesn}_${item.suggestedSong.numer}_${item.suggestedSong.moment}"
                                ) { isDragging ->
                                    EditableSongItem(
                                        song = item.suggestedSong,
                                        isDragging = isDragging,
                                        onEditClick = { viewModel.showDialog(DialogState.AddEditSong(item.suggestedSong.moment, item.suggestedSong)) },
                                        onDeleteClick = { viewModel.showDialog(DialogState.ConfirmDelete(item.suggestedSong, "pieśń: ${item.suggestedSong.piesn}")) },
                                        reorderModifier = Modifier.detectReorder(songReorderState),
                                        isReorderEnabled = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HierarchicalCollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 0f else -90f, label = "arrowRotation")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = SaturatedNavy)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                modifier = Modifier.rotate(rotationAngle),
                tint = SaturatedNavy
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun AddItemButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun EditableReadingItem(
    reading: Reading,
    isDragging: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    reorderModifier: Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isDragging) 4.dp else 0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Zmień kolejność",
                modifier = reorderModifier.padding(8.dp)
            )
            Icon(
                imageVector = Icons.Outlined.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = reading.typ,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, "Edytuj")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Usuń", tint = VividRed)
            }
        }
    }
}

@Composable
fun EditableSongCategoryHeader(categoryName: String) {
    Text(
        text = categoryName,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun EditableSongItem(
    song: SuggestedSong,
    isDragging: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    reorderModifier: Modifier,
    isReorderEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isDragging) 4.dp else 0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isReorderEnabled) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Zmień kolejność",
                    modifier = reorderModifier.padding(8.dp)
                )
            } else {
                Spacer(Modifier.width(40.dp))
            }
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = song.piesn,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, "Edytuj")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Usuń", tint = VividRed)
            }
        }
    }
}