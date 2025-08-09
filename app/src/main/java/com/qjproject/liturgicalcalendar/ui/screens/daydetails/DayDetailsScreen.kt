package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.qjproject.liturgicalcalendar.ui.components.CollapsibleSection

// --- POCZĄTEK POPRAWKI: Zmieniono strukturę funkcji, aby najpierw walidować dayId ---
@Composable
fun DayDetailsScreen(
    dayId: String?,
    onNavigateBack: () -> Unit
) {
    if (dayId.isNullOrBlank()) {
        // Jeśli dayId jest pusty lub null, od razu wyświetl błąd i zakończ.
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Błąd krytyczny: Brak identyfikatora dnia do załadowania.")
            }
        }
    } else {
        // Dopiero gdy mamy pewność, że dayId istnieje, tworzymy ViewModel i resztę ekranu.
        DayDetailsScreenContent(dayId = dayId, onNavigateBack = onNavigateBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailsScreenContent(
    dayId: String,
    onNavigateBack: () -> Unit
) {
    // --- KONIEC POPRAWKI ---
    val context = LocalContext.current
    val viewModel: DayDetailsViewModel = viewModel(factory = DayDetailsViewModelFactory(context, dayId))
    val uiState by viewModel.uiState.collectAsState()
    val groupedSongs by viewModel.groupedSongs.collectAsState()

    var showUrlModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DayDetailsTopAppBar(
                title = uiState.dayData?.tytulDnia ?: "Ładowanie...",
                onNavigateBack = onNavigateBack,
                onMoreClick = { showUrlModal = true }
            )
        }
    ) { innerPadding ->
        if (showUrlModal && uiState.dayData?.urlCzytania != null) {
            UrlModal(
                url = uiState.dayData!!.urlCzytania!!,
                onDismiss = { showUrlModal = false }
            )
        }

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    // --- POCZĄTEK POPRAWKI: Wyświetlanie konkretnego błędu z ViewModelu ---
                    Text(
                        text = "Nie udało się załadować danych.\nBłąd: ${uiState.error}",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    // --- KONIEC POPRAWKI ---
                }
            }
            uiState.dayData != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    CollapsibleSection(
                        title = "Czytania",
                        isExpanded = uiState.isReadingsSectionExpanded,
                        onToggle = { viewModel.toggleReadingsSection() }
                    ) {
                        uiState.dayData?.czytania?.forEachIndexed { index, reading ->
                            ReadingItemView(
                                reading = reading,
                                isExpanded = uiState.expandedReadings.contains(index),
                                onToggle = { viewModel.toggleReading(index) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CollapsibleSection(
                        title = "Pieśni Sugerowane",
                        isExpanded = uiState.isSongsSectionExpanded,
                        onToggle = { viewModel.toggleSongsSection() }
                    ) {
                        songMomentOrderMap.forEach { (momentKey, momentName) ->
                            SongGroupView(
                                momentName = momentName,
                                songs = groupedSongs[momentKey].orEmpty(),
                                isExpanded = uiState.expandedSongMoments.contains(momentKey),
                                onToggle = { viewModel.toggleSongMoment(momentKey) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ReadingItemView(reading: Reading, isExpanded: Boolean, onToggle: () -> Unit) {
    CollapsibleSection(
        title = reading.typ,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
private fun SongGroupView(momentName: String, songs: List<SuggestedSong>, isExpanded: Boolean, onToggle: () -> Unit) {
    CollapsibleSection(title = momentName, isExpanded = isExpanded, onToggle = onToggle) {
        if (songs.isEmpty()) {
            Text(
                "Brak sugerowanych pieśni.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        } else {
            songs.forEach { song ->
                SongItemView(song = song)
            }
        }
    }
}

@Composable
private fun SongItemView(song: SuggestedSong) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = song.piesn, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun UrlModal(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dodatkowe informacje", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij")
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailsTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                AutoResizingText(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                }
            },
            actions = {
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, "Więcej opcji")
                }
            },
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