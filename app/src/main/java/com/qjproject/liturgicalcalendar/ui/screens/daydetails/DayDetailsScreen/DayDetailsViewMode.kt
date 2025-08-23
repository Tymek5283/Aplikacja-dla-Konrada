// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/DayDetailsScreen/DayDetailsViewMode.kt
// Opis: Ten plik zawiera wszystkie komponenty odpowiedzialne za wyświetlanie zawartości ekranu DayDetailsScreen w trybie tylko do odczytu (View Mode).

package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.data.Reading
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun DayDetailsViewModeContent(
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
    readingOffsetsY: Map<Int, Int>,
    viewModel: DayDetailsViewModel,
    coroutineScope: CoroutineScope,
    scrollState: ScrollState,
    interactionSources: Map<Int, MutableInteractionSource>
) {
    val currentOffset = readingOffsetsY[index] ?: 0
    val isExpanded = viewModel.uiState.value.expandedReadings.contains(index)
    if (isExpanded) {
        coroutineScope.launch {
            scrollState.animateScrollTo(currentOffset)
        }
    }
    viewModel.collapseReading(index)
}

@Composable
fun ReadingItemView(
    reading: Reading,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onContentDoubleTap: () -> Unit,
    onGloballyPositioned: (LayoutCoordinates) -> Unit,
    interactionSource: MutableInteractionSource
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned(onGloballyPositioned)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                ),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = reading.typ,
                    style = MaterialTheme.typography.titleMedium,
                    color = SaturatedNavy
                )
                if (reading.sigla?.isNotBlank() == true) {
                    Text(
                        text = reading.sigla,
                        style = MaterialTheme.typography.bodySmall,
                        color = SaturatedNavy.copy(alpha = 0.8f)
                    )
                }
                if (reading.opis?.isNotBlank() == true) {
                    Text(
                        text = reading.opis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = reading.tekst,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onContentDoubleTap() })
                    },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun SongGroupView(
    momentName: String,
    songs: List<SuggestedSong>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSongClick: (SuggestedSong) -> Unit
) {
    if (songs.isNotEmpty()) {
        Column {
            val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 0f else -90f, label = "songArrowRotation")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(momentName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    songs.forEach { song ->
                        SongItemView(song = song, onClick = { onSongClick(song) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SongItemView(song: SuggestedSong, onClick: () -> Unit) {
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
            Icon(Icons.Outlined.MusicNote, contentDescription = "Pieśń", tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(song.piesn, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Text(song.numer, fontWeight = FontWeight.Bold)
        }
    }
}