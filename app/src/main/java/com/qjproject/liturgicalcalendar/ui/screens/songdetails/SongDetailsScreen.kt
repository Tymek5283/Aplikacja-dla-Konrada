package com.qjproject.liturgicalcalendar.ui.screens.songdetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsScreen(
    viewModel: SongDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToContent: (song: com.qjproject.liturgicalcalendar.data.Song, startInEditMode: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val song = uiState.song
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(onBack = onNavigateBack)

    // Ten efekt obserwuje cykl życia ekranu.
    // Gdy ekran jest wznawiany (ON_RESUME), np. po powrocie z edycji,
    // wywołuje funkcję reloadData(), aby pobrać najświeższe dane.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        AutoResizingText(
                            text = uiState.song?.tytul ?: "Ładowanie...",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                        }
                    },
                    actions = { Spacer(Modifier.width(48.dp)) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = "Błąd: ${uiState.error}",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                song != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        InfoRow(label = "Siedlecki:", value = song.numerSiedl)
                        InfoRow(label = "ŚAK:", value = song.numerSAK)
                        // ZMIANA: Dodano wyświetlanie numeru DN
                        InfoRow(label = "DN:", value = song.numerDN)
                        InfoRow(label = "Kategoria:", value = song.kategoria)

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val startInEdit = song.tekst.isNullOrBlank()
                                    onNavigateToContent(song, startInEdit)
                                },
                            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = viewModel.getSongTextPreview(song.tekst),
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                                )
                                if ((song.tekst?.lines()?.size ?: 0) > 6) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "...kliknij, aby zobaczyć więcej",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.End)
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
private fun InfoRow(label: String, value: String) {
    val displayValue = value.ifBlank { "-" }
    if (label == "Kategoria:" && displayValue == "-") {
        return
    }
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}