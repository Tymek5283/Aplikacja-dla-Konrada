package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.theme.DividerColor
import com.qjproject.liturgicalcalendar.ui.theme.SongItemBackground
import com.qjproject.liturgicalcalendar.ui.theme.SubtleGrayBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsScreen(
    dayId: String?,
    onNavigateBack: () -> Unit
) {
    if (dayId.isNullOrBlank()) {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Błąd krytyczny: Brak identyfikatora dnia do załadowania.")
            }
        }
        return
    }
    DayDetailsScreenContent(dayId = dayId, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailsScreenContent(
    dayId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DayDetailsViewModel = viewModel(factory = DayDetailsViewModelFactory(context, dayId))
    val uiState by viewModel.uiState.collectAsState()
    val groupedSongs by viewModel.groupedSongs.collectAsState()

    var showUrlModal by remember { mutableStateOf(false) }
    var showSongModal by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<SuggestedSong?>(null) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val readingLayouts = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    val interactionSources = remember { mutableStateMapOf<Int, MutableInteractionSource>() }
    var topBarHeight by remember { mutableStateOf(0) }

    BackHandler(enabled = showSongModal) {
        showSongModal = false
    }

    Scaffold(
        topBar = {
            DayDetailsTopAppBar(
                title = uiState.dayData?.tytulDnia ?: "Ładowanie...",
                onNavigateBack = onNavigateBack,
                onMoreClick = { showUrlModal = true },
                modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                    topBarHeight = layoutCoordinates.size.height
                }
            )
        }
    ) { innerPadding ->
        if (showUrlModal && uiState.dayData?.urlCzytania != null) {
            UrlModal(
                url = uiState.dayData!!.urlCzytania!!,
                onDismiss = { showUrlModal = false }
            )
        }

        if (showSongModal && selectedSong != null) {
            SongDetailsModal(
                song = selectedSong!!,
                onDismiss = { showSongModal = false }
            )
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { Text("Błąd: ${uiState.error}") }
            uiState.dayData != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    HierarchicalCollapsibleSection(
                        title = "Czytania",
                        isExpanded = uiState.isReadingsSectionExpanded,
                        onToggle = { viewModel.toggleReadingsSection() },
                        level = 0
                    ) {
                        uiState.dayData?.czytania?.forEachIndexed { index, reading ->
                            ReadingItemView(
                                reading = reading,
                                isExpanded = uiState.expandedReadings.contains(index),
                                onToggle = { viewModel.toggleReading(index) },
                                onContentDoubleTap = {
                                    handleReadingCollapse(
                                        index = index,
                                        readingLayouts = readingLayouts,
                                        viewModel = viewModel,
                                        coroutineScope = coroutineScope,
                                        scrollState = scrollState,
                                        topBarHeight = topBarHeight,
                                        interactionSources = interactionSources
                                    )
                                },
                                onGloballyPositioned = { coordinates ->
                                    readingLayouts[index] = coordinates
                                },
                                interactionSource = interactionSources.getOrPut(index) { MutableInteractionSource() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HierarchicalCollapsibleSection(
                        title = "Sugerowane pieśni",
                        isExpanded = uiState.isSongsSectionExpanded,
                        onToggle = { viewModel.toggleSongsSection() },
                        level = 0
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
        }
    }
}

private fun handleReadingCollapse(
    index: Int,
    readingLayouts: Map<Int, LayoutCoordinates>,
    viewModel: DayDetailsViewModel,
    coroutineScope: CoroutineScope,
    scrollState: ScrollState,
    topBarHeight: Int,
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


@Composable
private fun HierarchicalCollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    level: Int,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    val sectionModifier = if (level > 0) {
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(SubtleGrayBackground)
    } else {
        modifier
    }

    Column(modifier = sectionModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(),
                    onClick = onToggle
                )
                .padding(
                    horizontal = if (level > 0) 12.dp else 0.dp,
                    vertical = 8.dp
                ),
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

        if (level == 0) {
            Divider(color = DividerColor)
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (level > 0) 12.dp else 0.dp,
                        end = if (level > 0) 12.dp else 0.dp,
                        bottom = if (level > 0) 8.dp else 0.dp
                    )
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ReadingItemView(
    reading: Reading,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onContentDoubleTap: () -> Unit,
    onGloballyPositioned: (LayoutCoordinates) -> Unit,
    interactionSource: MutableInteractionSource
) {
    HierarchicalCollapsibleSection(
        title = reading.typ,
        isExpanded = isExpanded,
        onToggle = onToggle,
        level = 1,
        modifier = Modifier.onGloballyPositioned { coordinates ->
            onGloballyPositioned(coordinates)
        },
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onContentDoubleTap() }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!reading.sigla.isNullOrBlank()) {
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Sigla: ") }
                    append(reading.sigla)
                })
            }
            if (!reading.opis.isNullOrBlank()) {
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Opis: ") }
                    append(reading.opis)
                })
            }
            Text(
                text = reading.tekst,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 5,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SongGroupView(
    momentName: String,
    songs: List<SuggestedSong>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSongClick: (SuggestedSong) -> Unit
) {
    HierarchicalCollapsibleSection(title = momentName, isExpanded = isExpanded, onToggle = onToggle, level = 1) {
        if (songs.isEmpty()) {
            Text(
                "Brak sugerowanych pieśni.",
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        } else {
            songs.forEach { song ->
                SongItemView(song = song, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
private fun SongItemView(song: SuggestedSong, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = SongItemBackground)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = song.piesn,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun UrlModal(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(32.dp), shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Dodatkowe informacje", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Zamknij") }
                }
                Spacer(Modifier.height(16.dp))
                Text("Link do strony z czytaniami:", style = MaterialTheme.typography.titleSmall)
                Text(url, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("URL", url)
                        clipboard.setPrimaryClip(clip)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Kopiuj link")
                }
            }
        }
    }
}

@Composable
private fun SongDetailsModal(song: SuggestedSong, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.piesn,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // --- POCZĄTEK ZMIANY: Poprawiona logika budowania AnnotatedString ---
                    if (song.numer.isNotBlank()) {
                        Text(
                            buildAnnotatedString {
                                append(
                                    AnnotatedString(
                                        "Numer w Siedlecki: ",
                                        spanStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold).toSpanStyle()
                                    )
                                )
                                append(
                                    AnnotatedString(
                                        song.numer,
                                        spanStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold).toSpanStyle()
                                    )
                                )
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    if (song.opis.isNotBlank()) {
                        Text(buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Opis:\n")
                            }
                            append(song.opis)
                        }, style = MaterialTheme.typography.bodyMedium)
                    }
                    // --- KONIEC ZMIANY ---
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailsTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        CenterAlignedTopAppBar(
            title = { AutoResizingText(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć") } },
            actions = { IconButton(onClick = onMoreClick) { Icon(Icons.Default.MoreVert, "Więcej opcji") } },
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