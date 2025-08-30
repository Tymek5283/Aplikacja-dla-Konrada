package com.qjproject.liturgicalcalendar.ui.screens.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.data.Note
import com.qjproject.liturgicalcalendar.ui.theme.SectionHeaderBlue
import com.qjproject.liturgicalcalendar.ui.theme.TileBackground
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNoteDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Odświeżanie danych po powrocie do ekranu
    LaunchedEffect(Unit) {
        viewModel.refreshNotes()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeryDarkNavy)
    ) {
        // Nagłówek
        Column {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Notatki",
                        color = SectionHeaderBlue,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Wróć",
                            tint = SectionHeaderBlue
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = if (uiState.notes.isNotEmpty()) {
                            { viewModel.showAddDialog() }
                        } else {
                            { /* Brak akcji gdy nie ma notatek */ }
                        },
                        enabled = uiState.notes.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (uiState.notes.isNotEmpty()) "Dodaj notatkę" else null,
                            tint = if (uiState.notes.isNotEmpty()) SectionHeaderBlue else Color.Transparent
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = VeryDarkNavy
                )
            )
            
            // Linia oddzielająca
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Zawartość
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (uiState.notes.isEmpty()) {
            // Ekran startowy gdy brak notatek
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { viewModel.showAddDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TileBackground,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Dodaj notatkę",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            // Siatka notatek
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.notes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNavigateToNoteDetails(note.id) },
                        onLongClick = { viewModel.showDeleteDialog(note) }
                    )
                }
            }
        }
    }

    // Dialog dodawania notatki
    if (uiState.showAddDialog) {
        AddNoteDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { title, description ->
                viewModel.addNote(title, description)
            }
        )
    }

    // Dialog usuwania notatki
    uiState.noteToDelete?.let { noteToDelete ->
        if (uiState.showDeleteDialog) {
            DeleteNoteDialog(
                note = noteToDelete,
                onDismiss = { viewModel.hideDeleteDialog() },
                onConfirm = { viewModel.confirmDeleteNote() }
            )
        }
    }

    // Obsługa błędów
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // TODO: Pokazanie snackbar z błędem
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TileBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = note.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (note.description.isNotEmpty()) {
                Text(
                    text = note.description,
                    color = SectionHeaderBlue,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
