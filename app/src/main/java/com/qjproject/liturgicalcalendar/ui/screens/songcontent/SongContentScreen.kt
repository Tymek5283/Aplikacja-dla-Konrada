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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongContentScreen(
    viewModel: SongContentViewModel,
    onNavigateBack: () -> Unit,
    startInEditMode: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(startInEditMode) {
        if (startInEditMode) {
            viewModel.onEnterEditMode()
        }
    }

    BackHandler(enabled = uiState.isEditMode) {
        viewModel.onTryExitEditMode()
    }
    BackHandler(enabled = !uiState.isEditMode, onBack = onNavigateBack)

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
                .imePadding()
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.editableTitle.value,
                            onValueChange = { viewModel.onEditableFieldChange(title = it) },
                            label = { Text("Tytuł") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = viewModel.editableNumerSiedl.value,
                            onValueChange = { viewModel.onEditableFieldChange(siedl = it) },
                            label = { Text("Numer Siedlecki") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = viewModel.editableNumerSak.value,
                            onValueChange = { viewModel.onEditableFieldChange(sak = it) },
                            label = { Text("Numer ŚAK") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // ZMIANA: Dodano pole do edycji numeru DN
                        OutlinedTextField(
                            value = viewModel.editableNumerDn.value,
                            onValueChange = { viewModel.onEditableFieldChange(dn = it) },
                            label = { Text("Numer DN") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = viewModel.editableCategory.value,
                                onValueChange = { viewModel.onEditableFieldChange(category = it) },
                                label = { Text("Kategoria") },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                uiState.allCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.nazwa) },
                                        onClick = {
                                            viewModel.onEditableFieldChange(category = category.nazwa)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = viewModel.editableText.value,
                            onValueChange = { viewModel.onEditableFieldChange(text = it) },
                            label = { Text("Tekst pieśni") },
                            placeholder = { Text("Brak tekstu") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5
                            )
                        )
                    }
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
                            ?.takeIf { it.isNotBlank() }
                            ?: "Brak tekstu"

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
                        Text("Tak", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onDismiss) {
                        Text("Nie")
                    }
                }
            }
        }
    }
}