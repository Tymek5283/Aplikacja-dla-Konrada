package com.qjproject.liturgicalcalendar.ui.screens.songcontent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText

@Composable
fun SongContentScreen(
    songNumber: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SongContentViewModel = viewModel(
        factory = SongContentViewModelFactory(context, songNumber)
    )
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.isEditMode) {
        viewModel.onTryExitEditMode()
    }

    if (uiState.showConfirmExitDialog) {
        ConfirmExitDialog(
            onDismiss = { viewModel.dismissConfirmExitDialog() },
            onDiscard = { viewModel.onDiscardChanges() }
        )
    }

    Scaffold(
        topBar = {
            if (uiState.isEditMode) {
                EditModeTopAppBar(
                    title = uiState.song?.tytul ?: "Edycja",
                    onCancelClick = { viewModel.onTryExitEditMode() },
                    onSaveClick = { viewModel.onSaveChanges() },
                    isSaveEnabled = uiState.hasChanges
                )
            } else {
                ViewModeTopAppBar(
                    title = uiState.song?.tytul ?: "Ładowanie...",
                    onNavigateBack = onNavigateBack,
                    onEditClick = { viewModel.onEnterEditMode() }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding() // Pozwala uniknąć zasłaniania pola tekstowego przez klawiaturę
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = "Błąd: ${uiState.error}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.isEditMode -> {
                    val editableText by viewModel.editableText
                    OutlinedTextField(
                        value = editableText,
                        onValueChange = { viewModel.onTextChange(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5
                        )
                    )
                }
                uiState.song != null -> {
                    val song = uiState.song!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(PaddingValues(horizontal = 16.dp, vertical = 24.dp))
                    ) {
                        val formattedText = song.tekst
                            ?.replace("*", "\n")
                            ?.replace(Regex("(?<!^)(\\d+\\.)"), "\n\n$1")
                            ?.replace(Regex("\\b(ref(en)?\\b[\\s.:;]*)", RegexOption.IGNORE_CASE), "\n\n$1\n")
                            ?.trim()
                            ?: "Brak dostępnego tekstu dla tej pieśni."

                        Text(
                            text = formattedText,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeTopAppBar(title: String, onNavigateBack: () -> Unit, onEditClick: () -> Unit) {
    Column {
        CenterAlignedTopAppBar(
            title = { AutoResizingText(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center) },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć") } },
            actions = { IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Edytuj treść") } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.primary, navigationIconContentColor = MaterialTheme.colorScheme.primary)
        )
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
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
        )
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    }
}

@Composable
private fun ConfirmExitDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Odrzucić zmiany?", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)) },
        text = { Text("Czy na pewno chcesz wyjść bez zapisywania zmian?") },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Anuluj") }
        },
        confirmButton = {
            TextButton(onClick = onDiscard) { Text("Odrzuć") }
        }
    )
}