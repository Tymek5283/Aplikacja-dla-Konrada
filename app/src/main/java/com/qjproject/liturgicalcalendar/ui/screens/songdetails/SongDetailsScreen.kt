package com.qjproject.liturgicalcalendar.ui.screens.songdetails

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.components.PdfViewerDialog
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.TagChipBackground
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsScreen(
    viewModel: SongDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToContent: (song: com.qjproject.liturgicalcalendar.data.Song, startInEditMode: Boolean) -> Unit,
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToTagSearch: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val song = uiState.song
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Launcher do wyboru pliku PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addPdfForSong(it) }
    }

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
                        // Używamy Box z fillMaxWidth aby tytuł był wycentrowany względem całego ekranu
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoResizingText(
                                text = uiState.song?.tytul ?: "Ładowanie...",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                        }
                    },
                    actions = {
                        // Pusty spacer aby zachować symetrię
                        Spacer(modifier = Modifier.width(48.dp))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
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
                        InfoRow(label = "ŚAK 2020:", value = song.numerSAK2020.ifBlank { "-" })
                        InfoRow(label = "DN:", value = song.numerDN.ifBlank { "-" })
                        InfoRow(label = "Siedlecki:", value = song.numerSiedl.ifBlank { "-" })
                        InfoRow(label = "ŚAK:", value = song.numerSAK.ifBlank { "-" })
                        InfoRow(label = "Kategoria:", value = song.kategoria)

                        // Sekcja tagów
                        Spacer(modifier = Modifier.height(16.dp))
                        TagsSection(
                            tags = song.tagi.sorted(),
                            onAddTag = { viewModel.showAddTagDialog() },
                            onRemoveTag = { tag -> viewModel.showRemoveTagConfirmation(tag) },
                            onNavigateToTagManagement = onNavigateToTagManagement,
                            onTagClick = { tag -> onNavigateToTagSearch(tag) }
                        )

                        // Sekcja PDF po informacjach o pieśni
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Przyciski PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Neumy:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            
                            PdfActionButtons(
                                hasPdf = uiState.hasPdf,
                                pdfOperationInProgress = uiState.pdfOperationInProgress,
                                onViewPdf = { viewModel.showPdfViewer() },
                                onAddPdf = { pdfPickerLauncher.launch("application/pdf") },
                                onDeletePdf = { showDeleteConfirmation = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
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
            
            // Komunikaty o operacjach PDF
            uiState.pdfOperationMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.contains("błąd", ignoreCase = true) || message.contains("Nie udało się")) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.contains("błąd", ignoreCase = true) || message.contains("Nie udało się")) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
        }
        
        // Dialog potwierdzenia usunięcia PDF
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Usuń neumy") },
                text = { Text("Czy na pewno chcesz usunąć plik PDF z neumami dla tej pieśni?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePdfForSong()
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Usuń")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }
        
        // Dialog przeglądarki PDF
        if (uiState.showPdfViewer && !uiState.pdfPath.isNullOrEmpty()) {
            PdfViewerDialog(
                pdfPath = uiState.pdfPath!!,
                onDismiss = { viewModel.hidePdfViewer() }
            )
        }
        
        // Dialog dodawania tagów
        if (uiState.showAddTagDialog && song != null) {
            AssignTagsToSongDialog(
                song = song,
                onDismiss = { viewModel.hideAddTagDialog() },
                onTagsUpdated = { updatedSong ->
                    viewModel.updateSong(updatedSong)
                }
            )
        }
        
        // Dialog potwierdzenia usunięcia tagu
        uiState.showRemoveTagConfirmation?.let { tagToRemove ->
            Dialog(onDismissRequest = { viewModel.hideRemoveTagConfirmation() }) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Usunąć tag?",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            color = SaturatedNavy
                        )
                        Spacer(Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            buildAnnotatedString {
                                append("Czy na pewno chcesz usunąć tag ")
                                withStyle(style = SpanStyle(color = SaturatedNavy, fontWeight = FontWeight.Bold)) {
                                    append(tagToRemove)
                                }
                                append("?")
                            }
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.hideRemoveTagConfirmation() }) { Text("Anuluj") }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.removeTagFromSong(tagToRemove)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Usuń")
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

@Composable
private fun PdfActionButtons(
    hasPdf: Boolean,
    pdfOperationInProgress: Boolean,
    onViewPdf: () -> Unit,
    onAddPdf: () -> Unit,
    onDeletePdf: () -> Unit
) {
    // Używamy Box z szerokością 96dp (2 ikony po 48dp) dla symetrycznego układu
    Box(
        modifier = Modifier.width(96.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (pdfOperationInProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            if (hasPdf) {
                // Dwie ikony wyrównane do prawej
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ikona przeglądania PDF
                    IconButton(onClick = onViewPdf) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Pokaż neumy",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Ikona usuwania PDF
                    IconButton(onClick = onDeletePdf) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Usuń neumy",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                // Ikona dodawania PDF wyrównana do prawej
                IconButton(onClick = onAddPdf) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Dodaj neumy",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagsSection(
    tags: List<String>,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onNavigateToTagManagement: () -> Unit,
    onTagClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Label,
            contentDescription = "Tagi",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onNavigateToTagManagement() }
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tags) { tag ->
                TagChip(
                    tag = tag,
                    onLongClick = { onRemoveTag(tag) },
                    onTagIconClick = onNavigateToTagManagement,
                    onTagClick = { onTagClick(tag) }
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = onAddTag,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Dodaj tag",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagChip(
    tag: String,
    onLongClick: () -> Unit,
    onTagIconClick: () -> Unit = {},
    onTagClick: () -> Unit = {}
) {
    val chipShape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier
            .clip(chipShape)
            .combinedClickable(
                onClick = onTagClick,
                onLongClick = onLongClick
            ),
        shape = chipShape,
        colors = CardDefaults.cardColors(
            containerColor = TagChipBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}