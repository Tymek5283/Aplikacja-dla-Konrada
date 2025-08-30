// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/notes/NoteDetailsScreen.kt
// Opis: Ekran szczegółów i edycji notatki z automatycznym zapisem i formatowaniem tekstu
package com.qjproject.liturgicalcalendar.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.components.ColorPickerDialog
import com.qjproject.liturgicalcalendar.ui.components.CustomRichTextEditor
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.qjproject.liturgicalcalendar.ui.theme.SectionHeaderBlue
import com.qjproject.liturgicalcalendar.ui.theme.TileBackground
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailsScreen(
    viewModel: NoteDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val richTextState = rememberRichTextState()
    
    // Inicjalizuj RichTextState w ViewModelu
    LaunchedEffect(Unit) {
        viewModel.richTextState.value = richTextState
    }
    
    // Obsługa systemowego przycisku "wstecz" - zapisz przed wyjściem
    BackHandler {
        if (uiState.hasUnsavedChanges) {
            viewModel.saveImmediately()
        }
        onNavigateBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeryDarkNavy)
    ) {
        // Nagłówek z dynamicznie skalowanym tytułem - przypięty do góry
        Column {
            CenterAlignedTopAppBar(
                title = { 
                    AutoResizingText(
                        text = uiState.note?.title ?: "Notatka",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasUnsavedChanges) {
                            viewModel.saveImmediately()
                        }
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Wróć",
                            tint = SectionHeaderBlue
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showEditTitleDescriptionDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edytuj tytuł i opis",
                            tint = SectionHeaderBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = VeryDarkNavy,
                    titleContentColor = SectionHeaderBlue
                )
            )
            
            // Linia oddzielająca
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Panel formatowania - przypięty do nagłówka
            FormattingToolbar(
                onBoldClick = { viewModel.toggleBold() },
                onItalicClick = { viewModel.toggleItalic() },
                onUnderlineClick = { viewModel.toggleUnderline() },
                onColorClick = { viewModel.toggleColor() },
                onListClick = { viewModel.toggleList() },
                onAddClick = { 
                    // Przyszła funkcjonalność: dodawanie obrazów, linków, tabel itp.
                    // Na razie wyłączone
                },
                isBoldActive = richTextState.currentSpanStyle.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold,
                isItalicActive = richTextState.currentSpanStyle.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic,
                isUnderlineActive = richTextState.currentSpanStyle.textDecoration?.contains(androidx.compose.ui.text.style.TextDecoration.Underline) == true,
                isColorActive = uiState.activeTextColor != null,
                isListActive = richTextState.isUnorderedList,
                selectedColor = uiState.activeTextColor
            )
        }

        // Edytor tekstu - przewijany obszar
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            CustomRichTextEditor(
                htmlContent = uiState.htmlContent,
                onContentChange = { newHtmlContent ->
                    viewModel.updateHtmlContent(newHtmlContent)
                },
                richTextState = richTextState,
                onSelectionChange = { selection ->
                    viewModel.onSelectionChange(selection)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .verticalScroll(scrollState)
                    .imePadding()
            )
        }
    }

    // Dialog edycji tytułu i opisu
    if (uiState.showEditTitleDescriptionDialog) {
        EditTitleDescriptionDialog(
            currentTitle = uiState.note?.title ?: "",
            currentDescription = uiState.note?.description ?: "",
            onDismiss = { viewModel.hideEditTitleDescriptionDialog() },
            onConfirm = { title, description ->
                viewModel.updateTitleAndDescription(title, description)
            }
        )
    }

    // Dialog wyboru koloru
    if (uiState.showColorPickerDialog) {
        ColorPickerDialog(
            currentColor = uiState.selectedColor,
            onDismiss = { viewModel.hideColorPickerDialog() },
            onColorSelected = { color ->
                if (color == Color.White) {
                    viewModel.removeColor()
                } else {
                    viewModel.applyColor(color)
                }
            }
        )
    }

    // Obsługa błędów
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // TODO: Pokazanie snackbar z błędem
            viewModel.clearError()
        }
    }
}

@Composable
private fun FormattingToolbar(
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderlineClick: () -> Unit,
    onColorClick: () -> Unit,
    onListClick: () -> Unit,
    onAddClick: () -> Unit,
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    isUnderlineActive: Boolean,
    isColorActive: Boolean,
    isListActive: Boolean,
    selectedColor: Color? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = TileBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onBoldClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isBoldActive) SectionHeaderBlue else Color.Transparent,
                    contentColor = if (isBoldActive) Color.White else SectionHeaderBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FormatBold,
                    contentDescription = "Pogrubienie"
                )
            }

            IconButton(
                onClick = onItalicClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isItalicActive) SectionHeaderBlue else Color.Transparent,
                    contentColor = if (isItalicActive) Color.White else SectionHeaderBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FormatItalic,
                    contentDescription = "Kursywa"
                )
            }

            IconButton(
                onClick = onUnderlineClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isUnderlineActive) SectionHeaderBlue else Color.Transparent,
                    contentColor = if (isUnderlineActive) Color.White else SectionHeaderBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FormatUnderlined,
                    contentDescription = "Podkreślenie"
                )
            }

            IconButton(
                onClick = onColorClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isColorActive) {
                        selectedColor?.copy(alpha = 0.3f) ?: SectionHeaderBlue
                    } else Color.Transparent,
                    contentColor = selectedColor ?: SectionHeaderBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FormatColorText,
                    contentDescription = "Kolor tekstu",
                    tint = selectedColor ?: (if (isColorActive) Color.White else SectionHeaderBlue)
                )
            }

            IconButton(
                onClick = onListClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isListActive) SectionHeaderBlue else Color.Transparent,
                    contentColor = if (isListActive) Color.White else SectionHeaderBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = "Lista"
                )
            }
        }
    }
}

// Stary edytor - zastąpiony przez RichTextEditor
// Funkcja została usunięta, ponieważ została zastąpiona przez RichTextEditor
